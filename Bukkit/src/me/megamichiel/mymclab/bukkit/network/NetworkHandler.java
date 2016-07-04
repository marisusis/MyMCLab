package me.megamichiel.mymclab.bukkit.network;

import me.megamichiel.mymclab.api.Client;
import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.bukkit.MyMCLabPlugin;
import me.megamichiel.mymclab.perm.Group;
import org.bukkit.Material;

import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class NetworkHandler {

    final MyMCLabPlugin plugin;
    final KeyPair keyPair;
    final List<ClientImpl> clients = new CopyOnWriteArrayList<>();

    final List<ClientListener> clientListeners = new CopyOnWriteArrayList<>();

    NetworkHandler(MyMCLabPlugin plugin, KeyPair keyPair) {
        this.plugin = plugin;
        this.keyPair = keyPair;
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
            client.group = plugin.getGroupManager().getDefaultGroup();
            client.joinTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public static NetworkHandler determineNetworkHandler(MyMCLabPlugin plugin, KeyPair keyPair) {
        try {
            Material.valueOf("SLIME_BLOCK");
            return new Netty_1_8(plugin, keyPair);
        } catch (IllegalArgumentException not1_8) {
            try {
                Material.valueOf("PACKED_ICE");
                return new Netty_1_7(plugin, keyPair);
            } catch (IllegalArgumentException not1_7) {
                return null;
                /*try {
                    Class.forName("io.netty.channel.Channel");
                    return new Legacy_Handler_Netty(plugin, keyPair);
                } catch (ClassNotFoundException noNetty) {
                    plugin.nag("I do not support legacy craftbukkit versions! Either use spigot or update to at least 1.7");
                    return null;
                }*/
            }
        }
    }
}
