package me.megamichiel.mymclab.bukkit.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import me.megamichiel.mymclab.server.ServerHandler;
import me.megamichiel.mymclab.server.util.ChannelWrapper;

import java.lang.reflect.Field;
import java.net.SocketAddress;

public class Legacy_Handler_Netty extends Netty_1_8 {

    public Legacy_Handler_Netty(ServerHandler server) {
        super(server);
    }

    @Override
    void inject(Object conn) throws Exception {
        ChannelFuture future = null;
        for (Field field : conn.getClass().getDeclaredFields()) {
            if (field.getType() == ChannelFuture.class) {
                field.setAccessible(true);
                future = (ChannelFuture) field.get(conn);
            }
        }
        if (future != null) inject(future);
        else server.warning("Failed to inject!");
    }

    @Override
    ChannelWrapper wrap(final Channel channel, MyMCLabHandler handler) {
        return new ChannelWrapper() {
            @Override
            public void inEventLoop(Runnable runnable) {
                if (channel.eventLoop().inEventLoop()) runnable.run();
                else channel.eventLoop().execute(runnable);
            }

            @Override
            public void writeAndFlush(Object object) {
                channel.write(object);
                channel.flush();
            }

            @Override
            public void writeAndClose(Object object) {
                ChannelFuture future = channel.write(object);
                try {
                    CHANNEL_ADD_LISTENER.invoke(future, ChannelFutureListener.CLOSE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    channel.flush();
                    channel.close(); // Manual close
                    return;
                }
                channel.flush();
            }

            @Override
            public void close() {
                channel.close();
            }

            @Override
            public SocketAddress remoteAddress() {
                return channel.remoteAddress();
            }

            @Override
            public boolean isOpen() {
                return channel.isOpen();
            }

            @Override
            public void clearHandlers() {
                while (channel.pipeline().last() != handler)
                    channel.pipeline().removeLast();
            }
        };
    }
}
