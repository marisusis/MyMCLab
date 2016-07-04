package me.megamichiel.mymclab.packet.modal;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;

import java.io.IOException;

public class ModalClosePacket extends Packet {

    private static final byte ID = getId(ModalClosePacket.class);

    public ModalClosePacket() {
        super(ID);
    }

    public ModalClosePacket(ProtocolInput data) {
        super(ID);
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {}
}
