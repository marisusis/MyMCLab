package me.megamichiel.mymclab.bukkit.network;

import java.net.SocketAddress;

interface ChannelWrapper {

    void inEventLoop(Runnable runnable);
    void writeAndFlush(Object object);
    void writeAndClose(Object object);
    void close();
    SocketAddress remoteAddress();
    boolean isOpen();
}
