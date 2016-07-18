package me.megamichiel.mymclab.bungee;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import me.megamichiel.mymclab.MyMCLab;
import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.io.ByteArrayProtocolOutput;
import me.megamichiel.mymclab.io.ProtocolInputStream;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.IPermission;
import me.megamichiel.mymclab.server.ClientProcessor;
import me.megamichiel.mymclab.server.NetworkHandler;
import me.megamichiel.mymclab.server.ServerHandler;
import me.megamichiel.mymclab.server.util.ChannelWrapper;
import net.md_5.bungee.netty.PipelineUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;

public class BungeeNetworkHandler extends NetworkHandler {

    private CustomChild child;

    public BungeeNetworkHandler(ServerHandler server) {
        super(server);
    }

    @Override
    public void onEnable() {
        try {
            Field field = PipelineUtils.class.getDeclaredField("SERVER_CHILD");
            Field mod = Field.class.getDeclaredField("modifiers");
            mod.setAccessible(true);
            mod.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, child = new CustomChild(field.get(null)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        try {
            child.revert();
        } catch (Exception ex) {
            // D:
        }
    }

    private class CustomChild extends ChannelInitializer<Channel> {

        private final Object parent;
        private final Method initChannel;

        CustomChild(Object parent) throws Exception {
            this.parent = parent;
            initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
            initChannel.setAccessible(true);
        }

        void revert() throws Exception {
            Field field = PipelineUtils.class.getDeclaredField("SERVER_CHILD");
            if (field.get(null) == this) field.set(null, parent);
        }

        @Override
        protected void initChannel(Channel channel) throws Exception {
            initChannel.invoke(parent, channel);
            channel.pipeline().addFirst(new MyMCLabHandler(channel));
        }
    }

    private class MyMCLabHandler extends ChannelDuplexHandler {

        private final ChannelWrapper wrapper;
        private final ClientProcessor processor;

        MyMCLabHandler(Channel channel) {
            wrapper = new ChannelWrapper() {
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
            };
            processor = new ClientProcessor(server, BungeeNetworkHandler.this, wrapper);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;
            ProtocolInputStream stream = new ProtocolInputStream(new ByteBufInputStream(buf));
            if (processor.isValidated()) {
                while (stream.available() > 0) processor.handlePacket(stream);
            } else {
                buf.markReaderIndex();
                ClientProcessor.State state = processor.handleLogin(stream);
                switch (state) {
                    case BAD_PROTOCOL:
                        buf.resetReaderIndex();
                        super.channelRead(ctx, Unpooled.copiedBuffer(buf));
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
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof Packet) {
                Packet packet = (Packet) msg;
                IPermission perm = packet.getPermission();
                if (perm != null && !processor.getClient().hasPermission(perm))
                    return;
                super.write(ctx,
                        Unpooled.wrappedBuffer(processor.encodePacket(packet)),
                        promise);
            } else super.write(ctx, msg instanceof byte[] ?
                    Unpooled.wrappedBuffer((byte[]) msg) : msg, promise);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {
            if (processor.isValidated()) server.warning(throwable.toString());
            else super.exceptionCaught(channelHandlerContext, throwable);
        }
    }
}
