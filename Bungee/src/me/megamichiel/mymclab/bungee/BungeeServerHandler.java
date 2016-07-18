package me.megamichiel.mymclab.bungee;

import me.megamichiel.mymclab.perm.DefaultPermission;
import me.megamichiel.mymclab.server.ConsoleHandler;
import me.megamichiel.mymclab.server.ServerHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.concurrent.TimeUnit;

public class BungeeServerHandler extends ServerHandler {

    private final MyMCLabPlugin plugin;
    private final Thread mainThread = Thread.currentThread();

    BungeeServerHandler(MyMCLabPlugin plugin) {
        super(BungeeNetworkHandler::new, server -> new BungeeStatisticManager(plugin, server),
                (a, b) -> new ConsoleHandler(a::add, b::add, ProxyServer.getInstance().getLogger()));
        this.plugin = plugin;
        disableFeatures(DefaultPermission.VIEW_CHAT, DefaultPermission.INPUT_CHAT);
    }

    private final TextComponent commandNotFound = new TextComponent("Command not found");
    { commandNotFound.setColor(ChatColor.RED); }

    @Override
    public void handleCommand(String command, boolean chat) {
        ProxyServer proxy = plugin.getProxy();
        if (!proxy.getPluginManager().dispatchCommand(
                proxy.getConsole(), (chat ? "alert " : "") + command))
            proxy.getConsole().sendMessage(commandNotFound); // Have to manually send this .-.
    }

    @Override
    public void runOnMainThread(Runnable task) {
        plugin.getProxy().getScheduler().schedule(plugin, task, 0, TimeUnit.SECONDS);
    }

    @Override
    public boolean isMainThread() {
        return Thread.currentThread() == mainThread;
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
