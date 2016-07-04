package me.megamichiel.mymclab.packet.modal;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;

import java.io.IOException;

public class ModalClickPacket extends Packet {

    private static final byte ID = getId(ModalClickPacket.class);

    private final int modal, item;

    public ModalClickPacket(int modal, int item) {
        super(ID);
        this.modal = modal;
        this.item = item;
    }

    public ModalClickPacket(ProtocolInput data) throws IOException {
        super(ID);
        modal = data.readVarInt();
        item = data.readVarInt();
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeVarInt(modal);
        data.writeVarInt(item);
    }

    public int getModal() {
        return modal;
    }

    public int getItem() {
        return item;
    }
}
