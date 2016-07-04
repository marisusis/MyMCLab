package me.megamichiel.mymclab.bukkit.network;

import me.megamichiel.mymclab.api.Client;
import me.megamichiel.mymclab.api.Modal;
import me.megamichiel.mymclab.api.PluginMessageListener;
import me.megamichiel.mymclab.bukkit.MyMCLabPlugin;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.packet.messaging.PluginMessagePacket;
import me.megamichiel.mymclab.packet.messaging.RawMessagePacket;
import me.megamichiel.mymclab.packet.modal.ModalClickPacket;
import me.megamichiel.mymclab.packet.modal.ModalClosePacket;
import me.megamichiel.mymclab.packet.modal.ModalOpenPacket;
import me.megamichiel.mymclab.packet.player.PromptResponsePacket;
import me.megamichiel.mymclab.packet.player.StatisticClickPacket;
import me.megamichiel.mymclab.perm.DefaultPermission;
import me.megamichiel.mymclab.perm.Group;
import me.megamichiel.mymclab.perm.IPermission;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.server.ServerCommandEvent;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class ClientImpl implements Client {

    private final List<PluginMessageListener> pluginMessageListeners = new CopyOnWriteArrayList<>();

    private final ChannelWrapper channel;
    private final MyMCLabPlugin plugin;

    Group group;
    long joinTime, bytesSent, bytesReceived;
    private Modal modal;

    ClientImpl(ChannelWrapper channel, MyMCLabPlugin plugin) {
        this.channel = channel;
        this.plugin = plugin;
    }

    @Override
    public void sendPacket(final Packet packet) {
        channel.inEventLoop(() -> channel.writeAndFlush(packet));
    }

    @Override
    public void close() {
        channel.close();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public boolean isConnected() {
        return channel.isOpen();
    }

    @Override
    public void disconnect(final String reason) {
        channel.inEventLoop(() -> channel.writeAndClose(new RawMessagePacket(RawMessagePacket.RawMessageType.DISCONNECT, reason)));
    }

    @Override
    public Group getGroup() {
        return group;
    }

    @Override
    public void openModal(Modal modal) {
        this.modal = modal;
        sendPacket(modal == null ? new ModalClosePacket() : new ModalOpenPacket(modal));
    }

    @Override
    public Modal getOpenModal() {
        return modal;
    }

    @Override
    public long getJoinTime() {
        return joinTime;
    }

    @Override
    public long getSentBytes() {
        return bytesSent;
    }

    @Override
    public long getReceivedBytes() {
        return bytesReceived;
    }

    @Override
    public void addPluginMessageListener(PluginMessageListener listener) {
        if (listener != null) pluginMessageListeners.add(listener);
    }

    @Override
    public void removePluginMessageListener(PluginMessageListener listener) {
        if (listener != null) pluginMessageListeners.remove(listener);
    }

    void handlePacket(Packet packet) {
        if (packet instanceof RawMessagePacket) {
            RawMessagePacket message = (RawMessagePacket) packet;
            switch (message.getType()) {
                case DISCONNECT:
                    disconnect(message.getMessage());
                    break;
                case CHAT: case COMMAND:
                    boolean chat = message.getType() == RawMessagePacket.RawMessageType.CHAT;
                    IPermission permission = chat ? DefaultPermission.INPUT_CHAT : DefaultPermission.INPUT_COMMANDS;
                    if (!hasPermission(permission)) return;

                    Server server = Bukkit.getServer();
                    ServerCommandEvent event = new ServerCommandEvent(
                            server.getConsoleSender(), (chat ? "say " : "") + message.getMessage());
                    server.getPluginManager().callEvent(event);
                    if (!event.isCancelled() && event.getCommand() != null)
                        server.dispatchCommand(event.getSender(), event.getCommand());
                    break;
                default:
                    disconnect("Bad raw message: " + message.getType().name());
                    break;
            }
        } else if (packet instanceof StatisticClickPacket) {
            plugin.getStatisticManager().handleClick(this,
                    ((StatisticClickPacket) packet).getName(),
                    ((StatisticClickPacket) packet).getItemIndex());
        } else if (packet instanceof PromptResponsePacket)
            plugin.getStatisticManager().handlePrompt(this, (PromptResponsePacket) packet);
        else if (packet instanceof ModalClickPacket) {
            ModalClickPacket click = (ModalClickPacket) packet;
            Modal current = getOpenModal();
            if (current == null || current.getId() != click.getModal()) return;
            current.getItems().stream().filter(i -> i.getId() == click.getItem())
                    .findAny().ifPresent(i -> i.onClick(this, current));
        } else if (packet instanceof ModalClosePacket) closeModal();
        else if (packet instanceof PluginMessagePacket) {
            PluginMessagePacket message = (PluginMessagePacket) packet;
            String tag = message.getTag();
            byte[] data = message.getData();
            for (PluginMessageListener listener : pluginMessageListeners) {
                listener.onPluginMessageReceived(this, tag, data);
            }
        }
    }
}
