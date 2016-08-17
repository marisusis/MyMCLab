package me.megamichiel.mymclab.server;

import me.megamichiel.mymclab.api.Client;
import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.api.MyMCLabServer;
import me.megamichiel.mymclab.packet.BatchPacket;
import me.megamichiel.mymclab.packet.KeepAlivePacket;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.packet.messaging.ErrorPacket;
import me.megamichiel.mymclab.packet.messaging.MessagePacket;
import me.megamichiel.mymclab.packet.player.StatisticPacket;
import me.megamichiel.mymclab.perm.DefaultPermission;
import me.megamichiel.mymclab.perm.GroupManager;
import me.megamichiel.mymclab.server.util.AnimatedString;
import me.megamichiel.mymclab.server.util.DynamicString;
import me.megamichiel.mymclab.server.util.IConfig;
import me.megamichiel.mymclab.server.util.LockArrayList;
import me.megamichiel.mymclab.util.ColoredText;
import me.megamichiel.mymclab.util.Encryption;
import me.megamichiel.mymclab.util.Reporter;

import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class ServerHandler implements MyMCLabServer, Runnable, Reporter {

    private static final MessagePacket.Message[] MESSAGE_BUFFER = new MessagePacket.Message[100];
    private static final ErrorPacket.Error[] ERROR_BUFFER = new ErrorPacket.Error[100];

    private final Logger logger;
    private final NetworkHandler networkHandler;
    private final GroupsManager groupManager;

    private final StatisticManager statisticManager;
    private final AnimatedString<Supplier<ColoredText>> motd;

    private final KeyPair keyPair = Encryption.generateAsymmetricKey();

    private final LockArrayList<MessagePacket.Message> consoleMessages = new LockArrayList<>(),
                                                       chatMessages = new LockArrayList<>();
    private final LockArrayList<ErrorPacket.Error>     errors = new LockArrayList<>();

    private final ConsoleHandler consoleHandler;

    public ServerHandler(Logger logger,
                         Function<ServerHandler, NetworkHandler> networkHandler,
                         Function<ServerHandler, StatisticManager> statisticManager,
                         BiFunction<List<MessagePacket.Message>, List<ErrorPacket.Error>,
                                 ConsoleHandler> consoleHandler) {
        this.logger = logger;
        this.networkHandler = networkHandler.apply(this);
        groupManager = new GroupsManager(this);
        this.statisticManager = statisticManager.apply(this);
        this.consoleHandler = consoleHandler.apply(consoleMessages, errors);
        ColoredText def = new ColoredText()
                .color(ColoredText.EnumColor.DARK_GRAY)
                .text("A MyMCLab server");
        motd = new AnimatedString<>(s -> {
            DynamicString ds = this.statisticManager.parseString(s);
            ds.colorAmpersands();
            if (ds.isDynamic())
                return () -> ColoredText.parse(ds.toString(null, null), false);
            ColoredText txt = ColoredText.parse(ds.toString(null, null), false);
            return () -> txt;
        }, () -> def);
    }

    public ServerHandler(Logger logger, Function<ServerHandler, NetworkHandler> networkHandler,
                         Function<ServerHandler, StatisticManager> statisticManager) {
        this(logger, networkHandler, statisticManager, (a, b) -> new ConsoleHandler(a::add, b::add));
    }

    public boolean enable(IConfig config) {
        if (groupManager.enable(config)) {
            networkHandler.onEnable();
            statsRefreshDelay = config.getInt("stats-refresh-delay", 2);
            if (statsRefreshDelay < 1) statsRefreshDelay = 1;
            consoleHandler.enable();
            statisticManager.load(config);
            motd.load(logger, config, "motd");
            return true;
        }
        return false;
    }

    public void disable() {
        networkHandler.onDisable();
        consoleHandler.disable();
    }

    private final Packet[] batchBuffer = new Packet[4];
    private int keepAliveTimer = 0, statsRefreshTimer = 0, statsRefreshDelay;

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
                networkHandler.getClients().forEach(c -> c.sendPacket(packet));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (++statsRefreshTimer == statsRefreshDelay) {
            for (Client client : networkHandler.getClients())
                client.sendPacket(statisticManager.createStatisticPacket(client, StatisticPacket.StatisticItemAction.UPDATE));
            statsRefreshTimer = 0;
        }
    }

    @Override
    public List<Client> getClients() {
        return new ArrayList<>(networkHandler.getClients());
    }

    @Override
    public void addClientListener(ClientListener listener) {
        if (listener != null) networkHandler.clientListeners.add(listener);
    }

    @Override
    public GroupManager getGroupManager() {
        return groupManager;
    }

    public StatisticManager getStatisticManager() {
        return statisticManager;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public void addChatMessage(MessagePacket.Message message) {
        chatMessages.add(message);
    }

    public void disableFeatures(DefaultPermission... permissions) {
        groupManager.disableFeatures(permissions);
    }

    public abstract void handleCommand(String command, boolean chat);

    public abstract void runOnMainThread(Runnable task);

    public abstract boolean isMainThread();

    public NetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    public ColoredText getMotd() {
        return motd.next().get();
    }
}
