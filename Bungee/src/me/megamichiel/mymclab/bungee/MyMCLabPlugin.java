package me.megamichiel.mymclab.bungee;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.config.YamlConfig;
import me.megamichiel.mymclab.api.Client;
import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.api.MyMCLabServer;
import me.megamichiel.mymclab.packet.player.StatisticPacket;
import me.megamichiel.mymclab.perm.GroupManager;
import me.megamichiel.mymclab.server.ServerHandler;
import me.megamichiel.mymclab.server.util.IConfig;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MyMCLabPlugin extends Plugin implements MyMCLabServer, Listener, Nagger {

    private final ServerHandler serverHandler = new BungeeServerHandler(this);
    private final ConfigManager<BungeeConfig> config = ConfigManager.of(BungeeConfig::new);

    @Override
    public void onEnable() {
        config.file(new File(getDataFolder(), "config.yml"));
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

    public BungeeConfig getConfig() {
        return config.getConfig();
    }

    public void saveDefaultConfig() {
        config.saveDefaultConfig(() -> getResourceAsStream("config_bungee.yml"));
    }

    public void reloadConfig() {
        config.reloadConfig();
    }

    @EventHandler
    public void on(PostLoginEvent e) {
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
    public void on(PlayerDisconnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        Object context = serverHandler.getStatisticManager().createStringContext();
        for (Client client : serverHandler.getNetworkHandler().getClients())
            client.sendPacket(new StatisticPacket(
                    StatisticPacket.StatisticItemAction.REMOVE,
                    Collections.singletonList(serverHandler.getStatisticManager()
                            .createStatistic(client, player, context, false))
            ));
    }

    @Override
    public void nag(String message) {
        getLogger().warning(message);
    }

    @Override
    public void nag(Throwable thr) {
        getLogger().warning(thr.getClass().getName() + ": " + thr.getMessage());
    }

    static class BungeeConfig extends AbstractConfig implements IConfig {

        private final AbstractConfig parent;

        private BungeeConfig(AbstractConfig parent) {
            this.parent = parent;
        }

        BungeeConfig() {
            parent = new YamlConfig();
        }

        @Override
        public void set(String path, Object value) {
            parent.set(path, value);
        }

        @Override
        public void setAll(AbstractConfig config) {
            parent.setAll(config);
        }

        @Override
        public void setAll(Map<String, Object> map) {
            parent.setAll(map);
        }

        @Override
        public Object get(String path) {
            return parent.get(path);
        }

        @Override
        public BungeeConfig getSection(String path) {
            AbstractConfig sec = parent.getSection(path);
            return sec == null ? null : new BungeeConfig(sec);
        }

        @Override
        public List<BungeeConfig> getSectionList(String path) {
            return parent.getSectionList(path).stream()
                    .map(BungeeConfig::new).collect(Collectors.toList());
        }

        @Override
        public Set<String> keys() {
            return parent.keys();
        }

        @Override
        public Map<String, Object> values() {
            return parent.values();
        }

        @Override
        public Set<String> deepKeys() {
            return parent.deepKeys();
        }

        @Override
        public Map<String, Object> deepValues() {
            return parent.deepValues();
        }

        @Override
        public Map<String, Object> toRawMap() {
            return parent.toRawMap();
        }

        @Override
        public AbstractConfig loadFromFile(File file) throws IOException {
            parent.loadFromFile(file);
            return this;
        }

        @Override
        public void save(File file) throws IOException {
            parent.save(file);
        }
    }
}
