package me.megamichiel.mymclab.packet;

import me.megamichiel.mymclab.io.ByteArrayProtocolOutput;
import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.messaging.ErrorPacket;
import me.megamichiel.mymclab.packet.messaging.MessagePacket;
import me.megamichiel.mymclab.packet.messaging.PluginMessagePacket;
import me.megamichiel.mymclab.packet.messaging.RawMessagePacket;
import me.megamichiel.mymclab.packet.modal.ModalClickPacket;
import me.megamichiel.mymclab.packet.modal.ModalClosePacket;
import me.megamichiel.mymclab.packet.modal.ModalOpenPacket;
import me.megamichiel.mymclab.packet.player.PromptRequestPacket;
import me.megamichiel.mymclab.packet.player.PromptResponsePacket;
import me.megamichiel.mymclab.packet.player.StatisticClickPacket;
import me.megamichiel.mymclab.packet.player.StatisticPacket;
import me.megamichiel.mymclab.perm.DefaultPermission;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

public abstract class Packet {

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final Class<?>[] VALUES = {
            BatchPacket.class,
            KeepAlivePacket.class,
            PermissionPacket.class,

            ErrorPacket.class,
            MessagePacket.class,
            PluginMessagePacket.class,
            RawMessagePacket.class,

            ModalClickPacket.class,
            ModalClosePacket.class,
            ModalOpenPacket.class,

            PromptRequestPacket.class,
            PromptResponsePacket.class,
            StatisticClickPacket.class,
            StatisticPacket.class,

            ServerStatusPacket.class
    };
    private static final Constructor<?>[] CONSTRUCTORS = new Constructor<?>[VALUES.length];

    static {
        try {
            for (int i = 0; i < VALUES.length; i++)
                (CONSTRUCTORS[i] = VALUES[i].getDeclaredConstructor(ProtocolInput.class)).setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Packet createPacket(ProtocolInput stream) throws IOException {
        try {
            return (Packet) CONSTRUCTORS[stream.readUnsignedByte()].newInstance(stream);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof IOException)
                throw (IOException) ex.getCause();
            else throw new IOException(ex.getCause());
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    protected static byte getId(Class<? extends Packet> clazz) {
        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i] == clazz)
                return (byte) i;
        return -1;
    }

    private final byte id;

    private byte[] encoded;

    protected Packet(byte id) {
        this.id = id;
    }

    public final byte[] encode() throws IOException {
        if (encoded != null) return encoded;
        ByteArrayProtocolOutput data = new ByteArrayProtocolOutput();
        data.writeByte(id);
        encode(data);
        return encoded = data.toByteArray();
    }

    protected abstract void encode(ProtocolOutput data) throws IOException;

    public DefaultPermission getPermission() {
        return null;
    }

    public static int varIntLength(int i) {
        for (int j = 1; j < 5; j++)
            if ((i & (-1 << (j * 7))) == 0)
                return j;
        return 5;
    }
}
