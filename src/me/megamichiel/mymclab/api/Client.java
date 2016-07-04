package me.megamichiel.mymclab.api;

import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.packet.messaging.PluginMessagePacket;
import me.megamichiel.mymclab.packet.messaging.RawMessagePacket;
import me.megamichiel.mymclab.perm.CustomPermission;
import me.megamichiel.mymclab.perm.DefaultPermission;
import me.megamichiel.mymclab.perm.Group;
import me.megamichiel.mymclab.perm.IPermission;

import java.net.SocketAddress;

public interface Client {

    /**
     * Returns the socket address this Client has connected with
     */
    SocketAddress getRemoteAddress();

    /**
     * Closes this client's connection.
     */
    void close();

    /**
     * Returns whether this client is still connected
     */
    boolean isConnected();

    /**
     * Kicks this client
     *
     * @param reason The reason why they were kicked. Will show up as toast.
     */
    void disconnect(String reason);

    /**
     * Shows a toast to the client
     *
     * @param text the text to put in the toast
     */
    default void makeToast(String text) {
        sendPacket(new RawMessagePacket(RawMessagePacket.RawMessageType.TOAST, text));
    }

    /**
     * Returns the group this client is in
     */
    Group getGroup();

    /**
     * Checks if a client has a specific permission<br/>
     * This basically just calls #getGroup().hasPermission(IPermission)
     *
     * @param permission the permission to check
     * @return whether the client has the permission
     * @see DefaultPermission
     * @see CustomPermission#resolvePermission(String)
     */
    default boolean hasPermission(IPermission permission) {
        return getGroup().hasPermission(permission);
    }

    /**
     * Makes the client open a Modal
     *
     * @param modal the Modal to open
     */
    void openModal(Modal modal);

    /**
     * Makes the client close their currently open Modal
     */
    default void closeModal() {
        openModal(null);
    }

    /**
     * Returns the Modal that is currently viewed by the client
     */
    Modal getOpenModal();

    /**
     * Sends a Packet to this client.
     *
     * @param packet the Packet to send
     */
    void sendPacket(Packet packet);

    /**
     * Registers a PluginMessageListener for this Client
     *
     * @param listener the listener to register
     */
    void addPluginMessageListener(PluginMessageListener listener);

    /**
     * Removes a PluginMessageListener from this Client
     *
     * @param listener the listener to remove
     */
    void removePluginMessageListener(PluginMessageListener listener);

    /**
     * Sends a plugin message to this Client
     *
     * @param tag the tag of the plugin message
     * @param data the data of the message
     */
    default void sendPluginMessage(String tag, byte[] data) {
        if (tag != null && data != null)
            sendPacket(new PluginMessagePacket(tag, data));
    }

    /**
     * Returns the time this client joined at.
     */
    long getJoinTime();

    /**
     * Returns the amount of bytes sent to this Client
     */
    long getSentBytes();

    /**
     * Returns the amount of bytes received from this Client
     */
    long getReceivedBytes();
}
