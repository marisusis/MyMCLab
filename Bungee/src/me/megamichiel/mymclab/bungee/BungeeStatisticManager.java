package me.megamichiel.mymclab.bungee;

import me.megamichiel.mymclab.server.ServerHandler;
import me.megamichiel.mymclab.server.StatisticManager;
import me.megamichiel.mymclab.server.util.DynamicString;
import me.megamichiel.mymclab.server.util.MapConfig;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class BungeeStatisticManager extends StatisticManager {

    private final MyMCLabPlugin plugin;

    public BungeeStatisticManager(MyMCLabPlugin plugin, ServerHandler server) {
        super(server);
        this.plugin = plugin;
    }

    @Override
    public void load(MapConfig config) {
        super.load(new MapConfig()); // Load empty config, no information.
    }

    @Override
    public Object createStringContext() {
        return null;
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

        private String val;

        private BungeeString(String val) {
            this.val = val;
        }

        @Override
        public String toString(Object player, Object context) {
            return val;
        }

        @Override
        public void colorAmpersands() {
            val = ChatColor.translateAlternateColorCodes('&', val);
        }

        @Override
        public void replacePrompts(Pattern pattern, Map<String, String> promptValues) {
            // TODO ermagerd
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
