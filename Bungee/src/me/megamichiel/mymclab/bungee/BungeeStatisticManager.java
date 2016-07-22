package me.megamichiel.mymclab.bungee;

import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.mymclab.server.ServerHandler;
import me.megamichiel.mymclab.server.StatisticManager;
import me.megamichiel.mymclab.server.util.DynamicString;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class BungeeStatisticManager extends StatisticManager {

    private final MyMCLabPlugin plugin;

    public BungeeStatisticManager(MyMCLabPlugin plugin, ServerHandler server) {
        super(server);
        this.plugin = plugin;
    }

    @Override
    public Object createStringContext() {
        return PlaceholderContext.create(plugin);
    }

    @Override
    protected DynamicString parseString(String str) {
        return new BungeeString(str);
    }

    @Override
    protected Object getPlayer(String name) {
        return plugin.getProxy().getPlayer(name);
    }

    @Override
    protected String getName(Object player) {
        return ((ProxiedPlayer) player).getName();
    }

    @Override
    protected Collection<?> getPlayers() {
        return plugin.getProxy().getPlayers();
    }

    @Override
    protected <V> Map<Object, V> createPlayerMap() {
        return new PlayerMap<>(plugin);
    }

    private class BungeeString implements DynamicString {

        private final StringBundle bundle;

        private BungeeString(String val) {
            this.bundle = StringBundle.parse(plugin, val);
        }

        @Override
        public String toString(Object player, Object context) {
            if (context != null)
                return bundle.toString(player, (PlaceholderContext) context);
            else return bundle.toString(player);
        }

        @Override
        public void colorAmpersands() {
            bundle.colorAmpersands();
        }

        @Override
        public void replacePrompts(Pattern pattern, Supplier<Map<String, String>> promptValues) {
            bundle.replace(pattern, matcher -> {
                String name = matcher.group(1);
                return (nagger, who) -> {
                    StringBundle bundle = StringBundle.parse(plugin, promptValues.get().get(name));
                    return bundle == null ? null : bundle.toString(who);
                };
            });
        }
    }

    private class PlayerMap<V> extends HashMap<Object, V> implements Listener {

        PlayerMap(Plugin plugin) {
            plugin.getProxy().getPluginManager().registerListener(plugin, this);
        }

        @EventHandler
        void playerQuit(PlayerDisconnectEvent event) {
            remove(event.getPlayer());
        }
    }
}
