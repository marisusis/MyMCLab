package me.megamichiel.mymclab.server.util;

import java.net.SocketAddress;

public interface ChannelWrapper {

    void inEventLoop(Runnable runnable);
    void writeAndFlush(Object object);
    void writeAndClose(Object object);
    void close();
    SocketAddress remoteAddress();
    boolean isOpen();
    void clearHandlers();
}
