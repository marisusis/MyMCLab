package me.megamichiel.mymclab.bukkit.network;

import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.io.ProtocolInputStream;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.IPermission;
import me.megamichiel.mymclab.server.ClientImpl;
import me.megamichiel.mymclab.server.ClientProcessor;
import me.megamichiel.mymclab.server.NetworkHandler;
import me.megamichiel.mymclab.server.ServerHandler;
import me.megamichiel.mymclab.server.util.ChannelWrapper;
import net.minecraft.util.io.netty.buffer.ByteBuf;
import net.minecraft.util.io.netty.buffer.ByteBufInputStream;
import net.minecraft.util.io.netty.buffer.Unpooled;
import net.minecraft.util.io.netty.channel.*;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.util.List;
import java.util.function.Consumer;

public class Netty_1_7 extends NetworkHandler {

    private CustomChild child;

    public Netty_1_7(ServerHandler server) {
        super(server);
    }

    @Override
    public void onEnable() {
        try {
            Object server = Bukkit.getServer();
            String nms = "net.minecraft.server." + server.getClass().getPackage().getName().split("\\.")[3];
            Class<?> clazz = Class.forName(nms + ".MinecraftServer");
            server = server.getClass().getDeclaredMethod("getServer").invoke(server);
            Object conn = null;
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getReturnType().getSimpleName().equals("ServerConnection")) {
                    method.setAccessible(true);
                    conn = method.invoke(server);
                    break;
                }
            }
            if (conn == null) {
                this.server.warning("Couldn't find server's connection handler!");
                return;
            }
            for (Field f : conn.getClass().getDeclaredFields()) {
                if (f.getType() == List.class) {
                    f.setAccessible(true);
                    List<?> l = (List<?>) f.get(conn);
                    if (l.size() == 1 && l.get(0) instanceof ChannelFuture) {
                        inject((ChannelFuture) l.get(0));
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void inject(ChannelFuture future) {
        for (Object o : future.channel().pipeline().toMap().values()) {
            if (o.getClass().getSimpleName().equals("ServerBootstrapAcceptor")) {
                try {
                    Field childField = o.getClass().getDeclaredField("childHandler");
                    childField.setAccessible(true);
                    Field modifiers = Field.class.getDeclaredField("modifiers");
                    modifiers.setAccessible(true);
                    modifiers.setInt(childField, childField.getModifiers() & ~Modifier.FINAL);
                    ChannelInitializer<?> serverChild = (ChannelInitializer<?>) childField.get(o);
                    child = new CustomChild(o, childField, serverChild);
                    childField.set(o, child);
                    break;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private final Consumer<ClientImpl> clientClose = ClientImpl::close; // Avoid static usage when reload on plugin update

    @Override
    public void onDisable() {
        if (child != null) {
            child.revert();
            child = null;
        }
        clients.forEach(clientClose);
    }

    private class CustomChild extends ChannelInitializer<Channel> {

        private final Method initChannel;
        private final Object bootstrap;
        private final Field childField;
        private final ChannelInitializer<?> serverChild;

        CustomChild(Object bootstrap, Field childField, ChannelInitializer<?> serverChild) {
            this.bootstrap = bootstrap;
            this.childField = childField;
            this.serverChild = serverChild;
            Method m = null;
            try {
                m = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
                m.setAccessible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            initChannel = m;
        }

        @Override
        protected void initChannel(Channel channel) throws Exception {
            try {
                initChannel.invoke(serverChild, channel);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            channel.pipeline().addFirst("mymclab", new MyMCLabHandler(channel));
        }

        void revert() {
            try {
                childField.set(bootstrap, serverChild);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private class MyMCLabHandler extends ChannelDuplexHandler {

            private final ClientProcessor processor;

            MyMCLabHandler(final Channel channel) {
                processor = new ClientProcessor(server, Netty_1_7.this, new ChannelWrapper() {
                    @Override
                    public void inEventLoop(Runnable runnable) {
                        if (channel.eventLoop().inEventLoop()) runnable.run();
                        else channel.eventLoop().execute(runnable);
                    }

                    @Override
                    public void writeAndFlush(Object object) {
                        channel.writeAndFlush(object);
                    }

                    @Override
                    public void writeAndClose(Object object) {
                        channel.writeAndFlush(object).addListener(ChannelFutureListener.CLOSE);
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
                        while (channel.pipeline().last() != MyMCLabHandler.this)
                            channel.pipeline().removeLast();
                    }
                });
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
                ByteBuf buf = (ByteBuf) o;
                ProtocolInputStream stream = new ProtocolInputStream(new ByteBufInputStream(buf));
                if (processor.isValidated()) {
                    while (stream.available() > 0) processor.handlePacket(stream);
                } else {
                    buf.markReaderIndex();
                    ClientProcessor.State state = processor.handleLogin(stream);
                    switch (state) {
                        case BAD_PROTOCOL:
                            buf.resetReaderIndex();
                            super.channelRead(ctx, buf);
                            ctx.channel().pipeline().remove(this);
                        case LOGIN:
                            ctx.channel().closeFuture().addListener((ChannelFutureListener) future -> {
                                if (processor.getClient() != null && clients.remove(processor.getClient())) {
                                    processor.handleClose();
                                    for (ClientListener listener : clientListeners)
                                        listener.clientDisconnected(processor.getClient());
                                }
                            });
                            break;
                    }
                    buf.release();
                }
            }

            @Override
            public void write(ChannelHandlerContext channelHandlerContext, Object o, ChannelPromise channelPromise) throws Exception {
                if (o instanceof Packet) {
                    Packet packet = (Packet) o;
                    IPermission perm = packet.getPermission();
                    if (perm != null && !processor.getClient().hasPermission(perm))
                        return;
                    super.write(channelHandlerContext,
                            Unpooled.wrappedBuffer(processor.encodePacket(packet)),
                            channelPromise);
                } else super.write(channelHandlerContext, o instanceof byte[] ?
                        Unpooled.wrappedBuffer((byte[]) o) : o, channelPromise);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {
                if (processor.isValidated()) server.warning(throwable.toString());
                else super.exceptionCaught(channelHandlerContext, throwable);
            }
        }
    }
}
