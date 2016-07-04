package me.megamichiel.mymclab.api;

/**
 * An interface which can be used to listen for clients connecting and disconnecting
 *
 */
public interface ClientListener {

    /**
     * Called when a client connects
     *
     * @param client the client that connected
     */
    void clientConnected(Client client);

    /**
     * Called when a client disconnects
     * @param client the client that disconnected
     */
    void clientDisconnected(Client client);
}
