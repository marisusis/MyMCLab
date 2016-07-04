package me.megamichiel.mymclab.packet.messaging;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;

import java.io.IOException;

public class PluginMessagePacket extends Packet {

    private static final byte ID = getId(PluginMessagePacket.class);

    private final String tag;
    private final byte[] data;

    public PluginMessagePacket(String tag, byte[] data) {
        super(ID);
        this.tag = tag;
        this.data = data;
    }

    public PluginMessagePacket(ProtocolInput data) throws IOException {
        super(ID);
        tag = data.readString();
        this.data = new byte[data.readVarInt()];
        data.readFully(this.data);
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeString(tag);
        data.writeVarInt(this.data.length);
        data.write(this.data);
    }

    public String getTag() {
        return tag;
    }

    public byte[] getData() {
        return data;
    }
}
