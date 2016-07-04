package me.megamichiel.mymclab.api;

public interface PluginMessageListener {

    /**
     * Called when a plugin message was received
     *
     * @param client the client the plugin message was sent from
     * @param tag the tag of the plugin message
     * @param data the data of the plugin message
     */
    void onPluginMessageReceived(Client client, String tag, byte[] data);
}
