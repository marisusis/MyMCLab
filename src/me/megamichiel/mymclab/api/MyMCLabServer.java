package me.megamichiel.mymclab.api;

import me.megamichiel.mymclab.MyMCLab;
import me.megamichiel.mymclab.perm.GroupManager;

import java.util.List;

/**
 * An interface to be used on a server to work with MyMCLab
 */
public interface MyMCLabServer extends MyMCLab {

    /**
     * Returns a List containing all currently connected clients
     */
    List<Client> getClients();

    /**
     * Registers a new client listener
     *
     * @param listener the ClientListener to register
     */
    void addClientListener(ClientListener listener);

    /**
     * Returns the group manager.
     */
    GroupManager getGroupManager();
}
