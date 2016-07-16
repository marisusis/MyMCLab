package me.megamichiel.mymclab.server;

import me.megamichiel.mymclab.api.Client;
import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.perm.Group;

import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class NetworkHandler {

    protected final ServerHandler server;
    protected final List<ClientImpl> clients = new CopyOnWriteArrayList<>();

    protected final List<ClientListener> clientListeners = new CopyOnWriteArrayList<>();

    public NetworkHandler(ServerHandler server) {
        this.server = server;
    }

    public void addClientListener(ClientListener listener) {
        clientListeners.add(listener);
    }

    public List<? extends Client> getClients() {
        return clients;
    }

    public abstract void onEnable();
    public abstract void onDisable();

    public void setGroup(Client client, Group group) {
        ((ClientImpl) client).group = group;
    }

    boolean handleClientJoin(ClientImpl client) {
        clients.add(client);
        for (ClientListener listener : clientListeners)
            listener.clientConnected(client);
        if (client.isConnected()) { // No listener has closed it
            client.group = server.getGroupManager().getDefaultGroup();
            client.joinTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }
}
