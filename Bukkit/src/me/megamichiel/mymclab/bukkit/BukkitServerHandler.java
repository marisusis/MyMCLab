package me.megamichiel.mymclab.bukkit;

import me.megamichiel.mymclab.bukkit.util.BukkitStatisticManager;
import me.megamichiel.mymclab.server.ServerHandler;
import org.bukkit.event.Cancellable;
import org.bukkit.event.server.ServerCommandEvent;

public class BukkitServerHandler extends ServerHandler {

    private final MyMCLabPlugin plugin;

    public BukkitServerHandler(MyMCLabPlugin plugin) {
        super(MyMCLabPlugin::determineNetworkHandler,
                serverHandler -> new BukkitStatisticManager(plugin, serverHandler));
        this.plugin = plugin;
    }

    @Override
    public void handleCommand(String command, boolean chat) {
        if (chat) command = "say " + command;
        ServerCommandEvent event = new ServerCommandEvent(plugin.getServer().getConsoleSender(), command);
        plugin.getServer().getPluginManager().callEvent(event);
        // Earlier versions didn't have cancellable ServerCommandEvent
        if ((!(event instanceof Cancellable) || !((Cancellable) event).isCancelled()) && event.getCommand() != null)
            plugin.getServer().dispatchCommand(event.getSender(), event.getCommand());
    }

    @Override
    public void runOnMainThread(Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public boolean isMainThread() {
        return plugin.getServer().isPrimaryThread();
    }

    @Override
    public void info(String msg) {
        plugin.getLogger().info(msg);
    }

    @Override
    public void warning(String msg) {
        plugin.getLogger().warning(msg);
    }

    @Override
    public void error(String msg) {
        plugin.getLogger().severe(msg);
    }
}
