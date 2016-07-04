package me.megamichiel.mymclab.bukkit;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.YamlConfig;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import me.megamichiel.mymclab.api.Client;
import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.api.MyMCLabServer;
import me.megamichiel.mymclab.bukkit.network.NetworkHandler;
import me.megamichiel.mymclab.bukkit.util.LockArrayList;
import me.megamichiel.mymclab.packet.BatchPacket;
import me.megamichiel.mymclab.packet.KeepAlivePacket;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.packet.PermissionPacket;
import me.megamichiel.mymclab.packet.messaging.ErrorPacket;
import me.megamichiel.mymclab.packet.messaging.MessagePacket;
import me.megamichiel.mymclab.packet.player.StatisticPacket;
import me.megamichiel.mymclab.perm.DefaultPermission;
import me.megamichiel.mymclab.perm.Group;
import me.megamichiel.mymclab.util.ColoredText;
import me.megamichiel.mymclab.util.Encryption;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyPair;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyMCLabPlugin extends JavaPlugin implements Listener, MyMCLabServer, Nagger, Runnable {

    private static final String DONT_MIND_ME_I_HAVE_NO_USE = "%%__USER__%%";

    private static final MessagePacket.Message[] MESSAGE_BUFFER = new MessagePacket.Message[100];
    private static final ErrorPacket.Error[] ERROR_BUFFER = new ErrorPacket.Error[100];

    private final KeyPair keyPair = Encryption.generateAsymmetricKey();

    private final NetworkHandler networkHandler = NetworkHandler.determineNetworkHandler(this, keyPair);

    private final LockArrayList<MessagePacket.Message> consoleMessages = new LockArrayList<>(),
                                                       chatMessages = new LockArrayList<>();
    private final LockArrayList<ErrorPacket.Error>     errors = new LockArrayList<>();

    private final ConsoleHandler consoleHandler = new ConsoleHandler(consoleMessages, errors);

    private StatisticManager statisticManager;
    private final GroupsManager groupsManager = new GroupsManager(this);

    private int statsRefreshDelay;

    @Override
    public void onEnable() {
        if (networkHandler == null) {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[MyMCLab] Unsupported version: " + version + ". Please report this version on the plugin page so the author can work on it");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        configFile = new File(getDataFolder(), "config.yml");
        saveDefaultConfig();
        if (!groupsManager.onEnable()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        statisticManager = new StatisticManager(this, getConfig());
        statsRefreshDelay = getConfig().getInt("stats-refresh-delay", 2);

        consoleHandler.onEnable();

        networkHandler.onEnable();
        packetTask = getServer().getScheduler().runTaskTimerAsynchronously(this, this, 20L, 20L);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("mymclab").setExecutor(new MyMCLabCommand(this));

        checkForUpdate();
    }

    @Override
    public void onDisable() {
        consoleHandler.onDisable();
        unsafe().onDisable();
    }

    public StatisticManager getStatisticManager() {
        return statisticManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void on(AsyncPlayerChatEvent event) {
        chatMessages.add(new MessagePacket.Message(System.currentTimeMillis(),
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
                chatMessages.add(new MessagePacket.Message(System.currentTimeMillis(),
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
                chatMessages.add(new MessagePacket.Message(System.currentTimeMillis(),
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
                    Collections.singletonList(statisticManager.createPlayerInfo(client, player, context, true))
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
                    Collections.singletonList(statisticManager.createPlayerInfo(client, player, context, false))
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

    private File configFile;
    private YamlConfig config;

    @Override
    public void reloadConfig() {
        this.config = YamlConfig.loadConfig(this.configFile);
        InputStream defConfigStream = this.getResource("config.yml");
        if (defConfigStream != null) {
            this.config.setDefaults(YamlConfig.loadConfig(new InputStreamReader(defConfigStream, Packet.UTF_8)));
        }
    }

    @Override
    public YamlConfig getConfig() {
        if (config == null) reloadConfig();
        return config;
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
        if (!groupsManager.onEnable()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        statisticManager = new StatisticManager(this, getConfig());

        NetworkHandler networkHandler = unsafe();
        for (Client client : getClients()) { // Gimme a copy
            Group oldGroup = client.getGroup();
            Group group = groupsManager.getGroup(oldGroup.getName()); // Get new group after reload
            if (group == null) {
                client.disconnect("Your group was removed after a reload");
                continue;
            }
            networkHandler.setGroup(client, group);
            Set<DefaultPermission> set = new HashSet<>();
            for (DefaultPermission perm : DefaultPermission.values())
                if (client.hasPermission(perm))
                    set.add(perm);
            client.sendPacket(new BatchPacket(new Packet[] {
                    new PermissionPacket(set),
                    statisticManager.createPlayerInfoPacket(client, StatisticPacket.StatisticItemAction.ADD)
            }, 2));
        }

        statsRefreshDelay = getConfig().getInt("stats-refresh-delay", 2);
        if (statsRefreshDelay < 1) statsRefreshDelay = 1;
        statsRefreshTimer = 0;
        packetTask = getServer().getScheduler().runTaskTimerAsynchronously(this, this, 20L, 20L);
    }

    private BukkitTask packetTask;
    private int statsRefreshTimer = 0, keepAliveTimer = 0;

    private final Packet[] batchBuffer = new Packet[4];

    @Override
    public void run() {
        consoleMessages.toggleLock();
        int packetCount = 0, size = consoleMessages.size();
        if (size != 0) {
            if (size < 100) {
                batchBuffer[packetCount++] = new MessagePacket(false, consoleMessages.toArray(new MessagePacket.Message[size]));
            } else {
                consoleMessages.copyTo(size - 100, MESSAGE_BUFFER, 0, 100);
                batchBuffer[packetCount++] = new MessagePacket(false, MESSAGE_BUFFER);
            }
            consoleMessages.clear();
        }
        consoleMessages.toggleLock();
        chatMessages.toggleLock();
        if ((size = chatMessages.size()) != 0) {
            if (size < 100) {
                batchBuffer[packetCount++] = new MessagePacket(true, chatMessages.toArray(new MessagePacket.Message[size]));
            } else {
                chatMessages.copyTo(size - 100, MESSAGE_BUFFER, 0, 100);
                batchBuffer[packetCount++] = new MessagePacket(true, MESSAGE_BUFFER);
            }
            chatMessages.clear();
        }
        chatMessages.toggleLock();
        errors.toggleLock();
        if ((size = errors.size()) != 0) {
            if (size < 100) {
                batchBuffer[packetCount++] = new ErrorPacket(errors.toArray(new ErrorPacket.Error[size]));
            } else {
                errors.copyTo(size - 100, ERROR_BUFFER, 0, 100);
                batchBuffer[packetCount++] = new ErrorPacket(ERROR_BUFFER);
            }
            errors.clear();
        }
        errors.toggleLock();
        if (++keepAliveTimer == 10) {
            keepAliveTimer = 0;
            batchBuffer[packetCount++] = new KeepAlivePacket();
        }
        if (packetCount > 0) {
            Packet packet = packetCount == 1 ? batchBuffer[0] : new BatchPacket(batchBuffer, packetCount);
            try {
                packet.encode(); // Make sure the packet is already encoded to ensure the next use of the batchBuffer doesn't update the packets
                unsafe().getClients().forEach(c -> c.sendPacket(packet));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (++statsRefreshTimer == statsRefreshDelay) {
            for (Client client : unsafe().getClients())
                client.sendPacket(statisticManager.createPlayerInfoPacket(client,
                        StatisticPacket.StatisticItemAction.UPDATE));
            statsRefreshTimer = 0;
        }
    }

    @Override
    public GroupsManager getGroupManager() {
        return groupsManager;
    }

    private NetworkHandler unsafe() {
        return networkHandler;
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
}
