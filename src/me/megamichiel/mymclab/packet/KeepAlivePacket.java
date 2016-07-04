package me.megamichiel.mymclab.packet;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;

import java.io.IOException;

public class KeepAlivePacket extends Packet {

    private static final byte ID = getId(KeepAlivePacket.class);

    public KeepAlivePacket() {
        super(ID);
    }

    public KeepAlivePacket(ProtocolInput stream) {
        super(ID);
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {}
}
