package me.megamichiel.mymclab.bukkit.util;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;

public class PlayerMap<V> extends HashMap<Player, V> implements Listener {

    public PlayerMap(Plugin owningPlugin) {
        owningPlugin.getServer().getPluginManager().registerEvents(this, owningPlugin);
    }

    @EventHandler
    private void playerQuit(PlayerQuitEvent e) {
        remove(e.getPlayer());
    }
}
