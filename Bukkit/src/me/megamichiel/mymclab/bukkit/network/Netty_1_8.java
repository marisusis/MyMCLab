package me.megamichiel.mymclab.bukkit.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageCodec;
import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.io.ProtocolInputStream;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.IPermission;
import me.megamichiel.mymclab.server.ClientImpl;
import me.megamichiel.mymclab.server.ClientProcessor;
import me.megamichiel.mymclab.server.NetworkHandler;
import me.megamichiel.mymclab.server.ServerHandler;
import me.megamichiel.mymclab.server.util.ChannelWrapper;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Netty_1_8 extends NetworkHandler {

    static final Method CHANNEL_ADD_LISTENER = Arrays.stream(
            ChannelFuture.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("addListener")).findAny().orElse(null);

    private CustomChild child;

    public Netty_1_8(ServerHandler server) {
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
            inject(conn);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void inject(Object conn) throws Exception {
        for (Field f : conn.getClass().getDeclaredFields()) {
            if (f.getType() == List.class) {
                f.setAccessible(true);
                List<?> l = (List<?>) f.get(conn);
                if (l.size() == 1 && l.get(0) instanceof ChannelFuture) {
                    inject((ChannelFuture) l.get(0));
                }
            }
        }
    }

    void inject(ChannelFuture future) {
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

    private final Consumer<ClientImpl> clientClose = ClientImpl::close;

    @Override
    public void onDisable() {
        if (child != null) {
            child.revert();
            child = null;
        }
        clients.forEach(clientClose);
    }

    ChannelWrapper wrap(final Channel channel, MyMCLabHandler handler) {
        return new ChannelWrapper() {
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
                ChannelFuture future = channel.writeAndFlush(object);
                try {
                    CHANNEL_ADD_LISTENER.invoke(future, ChannelFutureListener.CLOSE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.channel().close();
                }
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
            channel.pipeline().addLast("mymclab", new MyMCLabHandler(channel));
            try {
                initChannel.invoke(serverChild, channel);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        void revert() {
            try {
                childField.set(bootstrap, serverChild);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    class MyMCLabHandler extends MessageToMessageCodec<Object, Object> {

        private final ClientProcessor processor;

        MyMCLabHandler(Channel channel) {
            processor = new ClientProcessor(server, Netty_1_8.this, wrap(channel, this));
        }

        protected Object decode(ChannelHandlerContext ctx, Object in) throws Exception {
            System.out.println("Received " + in);
            ByteBuf buf = (ByteBuf) in;
            ProtocolInputStream stream = new ProtocolInputStream(new ByteBufInputStream(buf));
            if (processor.isValidated()) {
                while (stream.available() > 0) processor.handlePacket(stream);
            } else {
                buf.markReaderIndex();
                ClientProcessor.State state = processor.handleLogin(stream);
                switch (state) {
                    case BAD_PROTOCOL:
                        buf.resetReaderIndex();
                        return Unpooled.copiedBuffer(buf);
                    case LOGIN:
                        CHANNEL_ADD_LISTENER.invoke(ctx.channel().closeFuture(), (ChannelFutureListener) future -> {
                            ClientImpl client = processor.getClient();
                            if (client != null && clients.remove(client)) {
                                processor.handleClose();
                                for (ClientListener listener : clientListeners)
                                    listener.clientDisconnected(client);
                            }
                        });
                        break;
                }
            }
            return null;
        }

        protected void decode(ChannelHandlerContext ctx, Object in, List<Object> output) throws Exception {
            Object o = decode(ctx, in);
            if (o != null) output.add(o);
        }

        protected Object encode(ChannelHandlerContext ctx, Object in) throws Exception {
            System.out.println("Encoding " + in);
            if (in instanceof Packet) {
                Packet packet = (Packet) in;
                IPermission perm = packet.getPermission();
                if (perm != null && !processor.getClient().hasPermission(perm))
                    return null;
                return Unpooled.wrappedBuffer(processor.encodePacket(packet));
            }
            if (in instanceof byte[]) return Unpooled.wrappedBuffer((byte[]) in);
            return in instanceof ByteBuf ? Unpooled.copiedBuffer((ByteBuf) in) : null;
        }

        protected void encode(ChannelHandlerContext ctx, Object in, List<Object> out) throws Exception {
            Object o = encode(ctx, in);
            if (o != null) out.add(o);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {
            if (processor.isValidated()) throwable.printStackTrace(); //plugin.getLogger().warning(throwable.toString());
            else super.exceptionCaught(channelHandlerContext, throwable);
        }
    }
}
