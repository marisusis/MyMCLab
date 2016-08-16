package me.megamichiel.mymclab.bukkit.util;

import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.mymclab.bukkit.MyMCLabPlugin;
import me.megamichiel.mymclab.server.ServerHandler;
import me.megamichiel.mymclab.server.StatisticManager;
import me.megamichiel.mymclab.server.util.DynamicString;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class BukkitStatisticManager extends StatisticManager {

    private final MyMCLabPlugin plugin;

    public BukkitStatisticManager(MyMCLabPlugin plugin, ServerHandler server) {
        super(server);
        this.plugin = plugin;
    }

    @Override
    public Object createStringContext() {
        return PlaceholderContext.create(plugin);
    }

    @Override
    protected DynamicString parseString(String str) {
        return str == null ? null : new BukkitDynamicString(StringBundle.parse(plugin, str));
    }

    @Override
    protected Object getPlayer(String name) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getName().equals(name))
                .findAny().orElse(null);
    }

    @Override
    protected String getName(Object player) {
        return ((Player) player).getName();
    }

    @Override
    protected Collection<?> getPlayers() {
        return Bukkit.getOnlinePlayers();
    }

    @Override
    protected <V> Map<Object, V> createPlayerMap() {
        return new PlayerMap<V>(plugin);
    }

    private class BukkitDynamicString implements DynamicString {

        private final StringBundle bundle;

        private BukkitDynamicString(StringBundle bundle) {
            this.bundle = bundle;
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
                final String name = matcher.group(1);
                return (nagger, who) -> {
                    StringBundle bundle = StringBundle.parse(plugin, promptValues.get().get(name));
                    return bundle == null ? null : bundle.toString(who);
                };
            });
        }

        @Override
        public boolean isDynamic() {
            return bundle.containsPlaceholders();
        }
    }
}
