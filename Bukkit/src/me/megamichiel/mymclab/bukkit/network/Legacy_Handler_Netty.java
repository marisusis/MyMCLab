package me.megamichiel.mymclab.bukkit.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import me.megamichiel.mymclab.bukkit.MyMCLabPlugin;

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.util.List;

class Legacy_Handler_Netty extends Netty_1_8 {

    private List pendingConnections;

    Legacy_Handler_Netty(MyMCLabPlugin plugin, KeyPair keyPair) {
        super(plugin, keyPair);
    }

    @Override
    void inject(Object conn) throws Exception {
        ChannelFuture future = null;
        for (Field field : conn.getClass().getDeclaredFields()) {
            if (field.getType() == ChannelFuture.class) {
                field.setAccessible(true);
                future = (ChannelFuture) field.get(conn);
            } else if (field.getType() == List.class) {
                field.setAccessible(true);
                pendingConnections = (List) field.get(conn);
            }
        }
        if (future != null) inject(future);
        else plugin.getLogger().warning("Failed to inject!");
    }

    @Override
    ChannelWrapper wrap(final Channel channel) {
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
                    future.channel().close();
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
        };
    }
}
