package me.megamichiel.mymclab.packet;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;

import java.io.IOException;

public class BatchPacket extends Packet {

    private static final byte ID = getId(BatchPacket.class);

    private final Packet[] packets;
    private final int size;

    public BatchPacket(Packet[] packets, int size) {
        super(ID);
        this.packets = packets;
        this.size = size;
    }

    public BatchPacket(ProtocolInput data) throws IOException {
        super(ID);
        packets = new Packet[size = data.readVarInt()];
        for (int i = 0; i != size; i++) packets[i] = createPacket(data);
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeVarInt(size);
        for (int i = 0; i != size; i++) data.write(packets[i].encode());
    }

    public Packet[] getPackets() {
        return packets;
    }

    public int getSize() {
        return size;
    }
}
