package me.megamichiel.mymclab.server;

import me.megamichiel.mymclab.MyMCLab;
import me.megamichiel.mymclab.api.ClientListener;
import me.megamichiel.mymclab.io.ByteArrayProtocolInput;
import me.megamichiel.mymclab.io.ByteArrayProtocolOutput;
import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.packet.BatchPacket;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.packet.PermissionPacket;
import me.megamichiel.mymclab.packet.player.StatisticPacket;
import me.megamichiel.mymclab.perm.DefaultPermission;
import me.megamichiel.mymclab.perm.Group;
import me.megamichiel.mymclab.perm.IPermission;
import me.megamichiel.mymclab.server.util.ChannelWrapper;
import me.megamichiel.mymclab.util.Compression;
import me.megamichiel.mymclab.util.Encryption;
import me.megamichiel.mymclab.util.EncryptionHandler;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClientProcessor implements Runnable {

    private final ServerHandler server;
    private final NetworkHandler networkHandler;
    private final ChannelWrapper channel;
    private ClientImpl client;

    private Group group;

    private Compression compression;
    private EncryptionHandler encryption;

    private boolean validated;

    private long lastActive;
    private boolean open = true;

    public ClientProcessor(ServerHandler server, NetworkHandler networkHandler, ChannelWrapper channel) {
        this.server = server;
        this.networkHandler = networkHandler;
        this.channel = channel;
        lastActive = System.currentTimeMillis();
        new Thread(this).start();
    }

    public byte[] encodePacket(Packet packet) throws Exception {
        byte[] encoded = packet.encode();
        int length = 0;
        if (compression != null && encoded.length > MyMCLab.COMPRESSION_THRESHOLD) {
            length = encoded.length;
            encoded = compression.compress(encoded);
        }
        ByteArrayProtocolOutput data = new ByteArrayProtocolOutput(encoded.length + 4);
        data.writeVarInt(length);
        data.write(encoded);
        encoded = encryption.encrypt(data.toByteArray());
        data = new ByteArrayProtocolOutput(encoded.length + 4);
        data.writeVarInt(encoded.length);
        data.write(encoded);
        client.bytesSent += data.size();
        return data.toByteArray();
    }

    public <S extends InputStream & ProtocolInput> void handlePacket(S stream) throws Exception {
        lastActive = System.currentTimeMillis();
        int length = stream.readVarInt();
        client.bytesReceived += Packet.varIntLength(length) + length;
        ByteArrayProtocolInput in = new ByteArrayProtocolInput(
                encryption.decrypt(stream.readFully(length))
        );
        if ((length = in.readVarInt()) != 0) { // Compressed
            if (length <= MyMCLab.COMPRESSION_THRESHOLD)
                throw new IOException("Badly compressed packet, length " + length);
            in = new ByteArrayProtocolInput(compression.decompress(
                    in.readFully(in.available()), length));
        }
        final Packet packet = Packet.createPacket(in);
        IPermission perm = packet.getPermission();
        if (perm != null && !client.hasPermission(perm)) return;

        if (server.isMainThread()) client.handlePacket(packet);
        else server.runOnMainThread(() -> client.handlePacket(packet));
    }

    public <S extends InputStream & ProtocolInput> State handleLogin(S stream) throws Exception {
        lastActive = System.currentTimeMillis();
        byte[] header = me.megamichiel.mymclab.MyMCLab.HEADER;
        if (stream.available() > header.length
                && Arrays.equals(stream.readFully(header.length), header)) {
            int available = stream.available();
            if (available == 2) {
                short version = stream.readShort(),
                        myVersion = MyMCLab.PROTOCOL_VERSION;
                if (version != myVersion) {
                    ByteArrayProtocolOutput out = new ByteArrayProtocolOutput();
                    out.write(MyMCLab.HEADER);
                    out.writeByte(version < myVersion ? -1 : 1);
                    channel.writeAndClose(out.toByteArray());
                    return version < myVersion ? State.OUTDATED_CLIENT : State.OUTDATED_SERVER;
                }
                client = new ClientImpl(channel, server);
                channel.clearHandlers();

                ByteArrayProtocolOutput out = new ByteArrayProtocolOutput();
                out.write(MyMCLab.HEADER);
                out.writeByte(0);
                byte[] encoded = server.getKeyPair().getPublic().getEncoded();
                out.writeVarInt(encoded.length);
                out.write(encoded);
                channel.writeAndFlush(out.toByteArray());
                return State.LOGIN;
            } else if (client != null) {
                byte[] decoded = stream.readFully(available);
                decoded = Encryption.decryptData(server.getKeyPair().getPrivate(), decoded);
                ByteArrayProtocolInput dataInput = new ByteArrayProtocolInput(decoded);
                String groupName = dataInput.readString();
                group = server.getGroupManager().getGroup(groupName);
                if (group == null) group = server.getGroupManager().getDefaultGroup();
                if (group == null) {
                    disconnect("Unknown group!");
                    return State.BAD_GROUP;
                }
                byte[] password = group.getPassword();
                if (dataInput.available() > password.length
                        && Arrays.equals(dataInput.readFully(password.length), password)) {
                    validated = true;
                    compression = new Compression();
                    encryption = new EncryptionHandler(new SecretKeySpec(
                            dataInput.readFully(dataInput.available()), "AES"));
                    loginComplete();
                    return State.AUTHENTICATED;
                }
                disconnect("Incorrect password!");
                return State.BAD_PASSWORD;
            }
        }
        open = false;
        return State.BAD_PROTOCOL;
    }

    public void loginComplete() {
        if (networkHandler.handleClientJoin(client)) {
            client.group = group;
            Set<DefaultPermission> set = new HashSet<>();
            for (DefaultPermission perm : DefaultPermission.values())
                if (group.hasPermission(perm))
                    set.add(perm);
            group = null;
            client.sendPacket(new BatchPacket(new Packet[] {
                    new PermissionPacket(set),
                    server.getStatisticManager().createStatisticPacket(client, StatisticPacket.StatisticItemAction.ADD)
            }, 2));
        }
    }

    @Override
    public void run() {
        while (open) {
            if (System.currentTimeMillis() - lastActive >= 30_000) {
                if (client != null) client.disconnect("Connection timed out");
                else channel.close();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // Don't care if something goes wrong here
            }
        }
    }

    public boolean isValidated() {
        return validated;
    }

    public void handleClose() {
        open = false;
    }

    public void disconnect(String reason) {
        client.disconnect(reason);
    }

    public ClientImpl getClient() {
        return client;
    }

    public enum State {
        BAD_PROTOCOL, OUTDATED_CLIENT, OUTDATED_SERVER, BAD_PASSWORD, BAD_GROUP,
        LOGIN, AUTHENTICATED
    }
}
