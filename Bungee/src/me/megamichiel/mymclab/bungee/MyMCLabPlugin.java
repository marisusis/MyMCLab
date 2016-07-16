package me.megamichiel.mymclab.bungee;

import me.megamichiel.mymclab.api.Client;
import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.api.MyMCLabServer;
import me.megamichiel.mymclab.packet.player.StatisticPacket;
import me.megamichiel.mymclab.perm.GroupManager;
import me.megamichiel.mymclab.server.ServerHandler;
import me.megamichiel.mymclab.server.util.MapConfig;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MyMCLabPlugin extends Plugin implements MyMCLabServer, Listener {

    private final ServerHandler serverHandler = new BungeeServerHandler(this);

    @Override
    public void onEnable() {
        configFile = new File(getDataFolder(), "config.yml");
        saveDefaultConfig();
        if (!serverHandler.enable(getConfig())) {
            getLogger().severe("Failed to load!");
            return;
        }
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getScheduler().schedule(this, serverHandler, 1L, 1L, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        serverHandler.disable();
    }

    @Override
    public List<Client> getClients() {
        return serverHandler.getClients();
    }

    @Override
    public void addClientListener(ClientListener listener) {
        serverHandler.addClientListener(listener);
    }

    @Override
    public GroupManager getGroupManager() {
        return serverHandler.getGroupManager();
    }

    private File configFile;
    private MapConfig config;

    public MapConfig getConfig() {
        if (config == null) reloadConfig();
        return config;
    }

    public void saveDefaultConfig() {
        File dataFolder = getDataFolder();
        if (dataFolder.exists() || dataFolder.mkdir()) {
            try {
                if (!configFile.exists() && configFile.createNewFile()) {
                    InputStream in = getResourceAsStream("config.yml");
                    OutputStream out = new FileOutputStream(configFile);
                    int read;
                    while ((read = in.read()) != -1)
                        out.write(read);
                    in.close();
                    out.close();
                }
                if (configFile.exists()) {
                    Configuration config = YamlConfiguration
                            .getProvider(YamlConfiguration.class)
                            .load(configFile);
                    Map<String, Object> map = new HashMap<>();
                    config.getKeys().forEach(k -> map.put(k, config.get(k)));
                    this.config = new MapConfig(map);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void reloadConfig() {
        if (configFile.exists()) {
            try {
                Configuration config = YamlConfiguration
                        .getProvider(YamlConfiguration.class)
                        .load(configFile);
                Map<String, Object> map = new HashMap<>();
                config.getKeys().forEach(k -> map.put(k, config.get(k)));
                this.config = new MapConfig(map);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @EventHandler
    void on(PostLoginEvent e) {
        ProxiedPlayer player = e.getPlayer();
        Object context = serverHandler.getStatisticManager().createStringContext();
        for (Client client : serverHandler.getNetworkHandler().getClients())
            client.sendPacket(new StatisticPacket(
                    StatisticPacket.StatisticItemAction.ADD,
                    Collections.singletonList(serverHandler.getStatisticManager()
                            .createStatistic(client, player, context, true))
            ));
    }

    @EventHandler
    void on(PlayerDisconnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        Object context = serverHandler.getStatisticManager().createStringContext();
        for (Client client : serverHandler.getNetworkHandler().getClients())
            client.sendPacket(new StatisticPacket(
                    StatisticPacket.StatisticItemAction.REMOVE,
                    Collections.singletonList(serverHandler.getStatisticManager()
                            .createStatistic(client, player, context, false))
            ));
    }
}
