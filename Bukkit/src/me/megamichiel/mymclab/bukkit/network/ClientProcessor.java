package me.megamichiel.mymclab.bukkit.network;

import me.megamichiel.mymclab.MyMCLab;
import me.megamichiel.mymclab.bukkit.MyMCLabPlugin;
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
import me.megamichiel.mymclab.util.Compression;
import me.megamichiel.mymclab.util.Encryption;
import me.megamichiel.mymclab.util.EncryptionHandler;
import org.bukkit.Bukkit;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class ClientProcessor implements Runnable {

    private final MyMCLabPlugin plugin;
    final NetworkHandler networkHandler;
    private final ChannelWrapper channel;
    ClientImpl client;

    private Group group;

    private Compression compression;
    private EncryptionHandler encryption;

    boolean validated;

    private long lastActive;
    private boolean open = true;

    ClientProcessor(MyMCLabPlugin plugin, NetworkHandler networkHandler, ChannelWrapper channel) {
        this.plugin = plugin;
        this.networkHandler = networkHandler;
        this.channel = channel;
        lastActive = System.currentTimeMillis();
        new Thread(this).start();
    }

    byte[] encodePacket(Packet packet) throws Exception {
        byte[] encoded = packet.encode();
        int length = 0;
        if (compression != null && encoded.length > me.megamichiel.mymclab.MyMCLab.COMPRESSION_THRESHOLD) {
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

    <S extends InputStream & ProtocolInput> void handlePacket(S stream) throws Exception {
        lastActive = System.currentTimeMillis();
        int length = stream.readVarInt();
        client.bytesReceived += Packet.varIntLength(length) + length;
        byte[] decoded = new byte[length];
        stream.readFully(decoded);
        decoded = encryption.decrypt(decoded);
        ByteArrayProtocolInput in = new ByteArrayProtocolInput(decoded);
        length = in.readVarInt();
        if (length != 0) { // Compressed
            if (length <= me.megamichiel.mymclab.MyMCLab.COMPRESSION_THRESHOLD) {
                throw new IOException("Badly compressed packet, length " + length);
            }
            byte[] b = new byte[in.available()];
            in.readFully(b);
            in = new ByteArrayProtocolInput(compression.decompress(b, length));
        }
        final Packet packet = Packet.createPacket(in);
        IPermission perm = packet.getPermission();
        if (perm != null && !client.hasPermission(perm)) return;

        if (Bukkit.isPrimaryThread()) client.handlePacket(packet);
        else Bukkit.getScheduler().runTask(plugin, () -> client.handlePacket(packet));
    }

    <S extends InputStream & ProtocolInput> State handleLogin(S stream) throws Exception {
        lastActive = System.currentTimeMillis();
        byte[] header = me.megamichiel.mymclab.MyMCLab.HEADER;
        if (stream.available() > header.length
                && Arrays.equals(stream.readFully(header.length), header)) {
            int available = stream.available();
            if (available == 2) {
                short version = stream.readShort(),
                        myVersion = MyMCLab.PROTOCOL_VERSION;
                if (version < myVersion) return State.OUTDATED_CLIENT;
                else if (version > myVersion) return State.OUTDATED_SERVER;
                return State.LOGIN;
            } else {
                byte[] decoded = stream.readFully(available);
                decoded = Encryption.decryptData(networkHandler.keyPair.getPrivate(), decoded);
                ByteArrayProtocolInput dataInput = new ByteArrayProtocolInput(decoded);
                String groupName = dataInput.readString();
                group = plugin.getGroupManager().getGroupOrDefault(groupName);
                if (group == null) return State.BAD_GROUP;
                byte[] password = group.getPassword();
                if (dataInput.available() > password.length
                        && Arrays.equals(dataInput.readFully(password.length), password)) {
                    validated = true;
                    compression = new Compression();
                    encryption = new EncryptionHandler(new SecretKeySpec(
                            dataInput.readFully(dataInput.available()), "AES"));
                    return State.AUTHENTICATED;
                }
                return State.BAD_PASSWORD;
            }
        }
        return State.BAD_PROTOCOL;
    }

    void loginComplete() {
        client.group = group;
        Set<DefaultPermission> set = new HashSet<>();
        for (DefaultPermission perm : DefaultPermission.values())
            if (group.hasPermission(perm))
                set.add(perm);
        group = null;
        client.sendPacket(new BatchPacket(new Packet[] {
                new PermissionPacket(set),
                plugin.getStatisticManager().createPlayerInfoPacket(
                        client, StatisticPacket.StatisticItemAction.ADD)
        }, 2));
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

    void handleClose() {
        open = false;
    }

    enum State {
        BAD_PROTOCOL, OUTDATED_CLIENT, OUTDATED_SERVER, BAD_PASSWORD, BAD_GROUP,
        LOGIN, AUTHENTICATED
    }
}
