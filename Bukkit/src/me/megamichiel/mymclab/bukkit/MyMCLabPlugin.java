package me.megamichiel.mymclab.bukkit;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import me.megamichiel.mymclab.api.Client;
import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.api.MyMCLabServer;
import me.megamichiel.mymclab.bukkit.network.Legacy_Handler_Netty;
import me.megamichiel.mymclab.bukkit.network.Netty_1_7;
import me.megamichiel.mymclab.bukkit.network.Netty_1_8;
import me.megamichiel.mymclab.packet.BatchPacket;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.packet.PermissionPacket;
import me.megamichiel.mymclab.packet.messaging.MessagePacket;
import me.megamichiel.mymclab.packet.player.StatisticPacket;
import me.megamichiel.mymclab.perm.DefaultPermission;
import me.megamichiel.mymclab.perm.Group;
import me.megamichiel.mymclab.perm.GroupManager;
import me.megamichiel.mymclab.server.NetworkHandler;
import me.megamichiel.mymclab.server.ServerHandler;
import me.megamichiel.mymclab.server.util.IConfig;
import me.megamichiel.mymclab.util.ColoredText;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MyMCLabPlugin extends JavaPlugin implements Listener, MyMCLabServer, Nagger {

    private static final String DONT_MIND_ME_I_HAVE_NO_USE = "%%__USER__%%";

    private final ServerHandler serverHandler = new BukkitServerHandler(this);
    private final ConfigManager<BukkitConfig> config = ConfigManager.of(BukkitConfig::new);

    @Override
    public void onEnable() {
        if (serverHandler.getNetworkHandler() == null) {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[MyMCLab] Unsupported version: " + version + ". Please report this version on the plugin page so the author can work on it");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        config.file(new File(getDataFolder(), "config.yml"));
        saveDefaultConfig();

        if (!serverHandler.enable(getConfiguration())) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        packetTask = getServer().getScheduler().runTaskTimerAsynchronously(this, serverHandler, 20L, 20L);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("mymclab").setExecutor(new MyMCLabCommand(this));

        checkForUpdate();
    }

    @Override
    public void onDisable() {
        serverHandler.disable();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void on(AsyncPlayerChatEvent event) {
        serverHandler.addChatMessage(new MessagePacket.Message(System.currentTimeMillis(),
                MessagePacket.LogLevel.INFO,
                ColoredText.parse(String.format(event.getFormat(),
                        event.getPlayer().getDisplayName(), event.getMessage()), false)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void on(ServerCommandEvent event) {
        int index = event.getCommand().indexOf(' ');
        if (index > -1) {
            String command = event.getCommand().substring(0, index);
            if ("say".equals(command.toLowerCase(Locale.US))) {
                serverHandler.addChatMessage(new MessagePacket.Message(System.currentTimeMillis(),
                        MessagePacket.LogLevel.INFO,
                        ColoredText.parse('[' + event.getSender().getName() + "] "
                                + event.getCommand().substring(index + 1), false)));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void on(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().substring(1);
        int index = message.indexOf(' ');
        if (index > -1) {
            String command = message.substring(0, index);
            if ("say".equals(command.toLowerCase(Locale.US))) {
                serverHandler.addChatMessage(new MessagePacket.Message(System.currentTimeMillis(),
                        MessagePacket.LogLevel.INFO,
                        ColoredText.parse('[' + event.getPlayer().getName() + "] "
                                + message.substring(index + 1), false)));
            }
        }
    }

    @EventHandler
    void on(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        PlaceholderContext context = PlaceholderContext.create(this);
        NetworkHandler networkHandler = unsafe();
        for (Client client : networkHandler.getClients())
            client.sendPacket(new StatisticPacket(
                    StatisticPacket.StatisticItemAction.ADD,
                    Collections.singletonList(serverHandler.getStatisticManager()
                            .createStatistic(client, player, context, true))
            ));
        if (update != null && player.hasPermission("mymclab.seeupdate")) {
            String msg = ChatColor.GREEN + "[MyMCLab] A new version is available! (Current version: \" + current + \", new version: \" + update + \")";
            player.sendMessage(msg);
        }
    }

    @EventHandler
    void on(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        PlaceholderContext context = PlaceholderContext.create(this);
        NetworkHandler networkHandler = unsafe();
        for (Client client : networkHandler.getClients())
            client.sendPacket(new StatisticPacket(
                    StatisticPacket.StatisticItemAction.REMOVE,
                    Collections.singletonList(serverHandler.getStatisticManager()
                            .createStatistic(client, player, context, false))
            ));
    }

    @Override
    public List<Client> getClients() {
        return new ArrayList<>(unsafe().getClients());
    }

    @Override
    public void addClientListener(ClientListener listener) {
        if (listener == null) throw new NullPointerException("listener is null!");
        unsafe().addClientListener(listener);
    }

    @Override
    public void saveDefaultConfig() {
        config.saveDefaultConfig(() -> getResource("config_bukkit.yml"));
    }

    @Override
    public void reloadConfig() {
        config.reloadConfig();
    }

    public BukkitConfig getConfiguration() {
        return config.getConfig();
    }

    @Override
    public void nag(String s) {
        getLogger().warning(s);
    }

    @Override
    public void nag(Throwable throwable) {
        getLogger().warning(throwable.toString());
    }

    void reload() {
        packetTask.cancel();

        reloadConfig();
        if (!serverHandler.enable(getConfiguration())) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        NetworkHandler networkHandler = unsafe();
        for (Client client : getClients()) { // Gimme a copy
            Group oldGroup = client.getGroup();
            Group group = serverHandler.getGroupManager().getGroup(oldGroup.getName()); // Get new group after reload
            if (group == null) {
                client.disconnect("Your group was removed after a reload");
                continue;
            }
            networkHandler.setGroup(client, group);
            Set<DefaultPermission> set = new HashSet<>();
            for (DefaultPermission perm : DefaultPermission.values())
                if (client.hasPermission(perm)) set.add(perm);
            client.sendPacket(new BatchPacket(new Packet[] {
                    new PermissionPacket(set),
                    serverHandler.getStatisticManager().createStatisticPacket(client, StatisticPacket.StatisticItemAction.ADD)
            }, 2));
        }

        packetTask = getServer().getScheduler().runTaskTimerAsynchronously(this, serverHandler, 20L, 20L);
    }

    private BukkitTask packetTask;

    @Override
    public GroupManager getGroupManager() {
        return serverHandler.getGroupManager();
    }

    private NetworkHandler unsafe() {
        return serverHandler.getNetworkHandler();
    }

    private String update;

    private void checkForUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                String current = getDescription().getVersion();
                try {
                    URLConnection connection = new URL("https://github.com/megamichiel/MyMCLab").openConnection();
                    Scanner scanner = new Scanner(connection.getInputStream());
                    scanner.useDelimiter("\\Z");
                    String content = scanner.next();
                    Matcher matcher = Pattern.compile("Current\\sVersion:\\s(<b>)?([0-9]\\.[0-9]\\.[0-9])(</b>)?").matcher(content);
                    if (!matcher.find()) {
                        nag("Failed to check for updates: Unknown page format!");
                    }
                    String version = matcher.group(2);
                    update = current.equals(version) ? null : version;
                    scanner.close();
                } catch (Exception ex) {
                    nag("Failed to check for updates:");
                    nag(ex);
                }
                if (update != null) {
                    getLogger().info("A new version is available! (Current version: " + current + ", new version: " + update + ")");
                }
            }
        }.runTaskAsynchronously(this);
    }

    public static NetworkHandler determineNetworkHandler(ServerHandler server) {
        try {
            Material.valueOf("SLIME_BLOCK");
            return new Netty_1_8(server);
        } catch (IllegalArgumentException not1_8) {
            try {
                Material.valueOf("PACKED_ICE");
                return new Netty_1_7(server);
            } catch (IllegalArgumentException not1_7) {
                try {
                    Class.forName("io.netty.channel.Channel");
                    String pkg = Bukkit.getServer().getClass().getPackage().getName();
                    if (Class.forName(pkg + ".Spigot").getDeclaredField("netty").getBoolean(null)) {
                        return new Legacy_Handler_Netty(server);
                    }
                } catch (Exception noNetty) {
                    server.error("I do not support legacy craftbukkit versions! Either use spigot or update to at least 1.7");
                }
                return null;
            }
        }
    }

    private static class BukkitConfig extends AbstractConfig implements IConfig {

        private final AbstractConfig parent;

        BukkitConfig(AbstractConfig parent) {
            this.parent = parent;
        }

        BukkitConfig() {
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
        public void setAll(Map<?, ?> map) {
            parent.setAll(map);
        }

        @Override
        public Object get(String path) {
            return parent.get(path);
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
        public BukkitConfig getSection(String path) {
            AbstractConfig sec = parent.getSection(path);
            return sec == null ? null : new BukkitConfig(sec);
        }

        @Override
        public List<BukkitConfig> getSectionList(String path) {
            return parent.getSectionList(path).stream()
                    .map(BukkitConfig::new).collect(Collectors.toList());
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
