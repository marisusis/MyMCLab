package me.megamichiel.mymclab.packet.modal;

import me.megamichiel.mymclab.api.Modal;
import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;

import java.io.IOException;
import java.util.List;

public class ModalOpenPacket extends Packet {

    private static final byte ID = getId(ModalOpenPacket.class);

    private final Modal modal;

    public ModalOpenPacket(Modal modal) {
        super(ID);
        this.modal = modal;
    }

    public ModalOpenPacket(ProtocolInput data) throws IOException {
        super(ID);
        modal = new Modal(data.readVarInt(), data.readString());
        int size = data.readVarInt();
        List<Modal.Item> items = modal.getItems();
        for (int i = 0; i < size; i++) items.add(new Modal.Item(data.readVarInt(), data.readString()));
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeVarInt(modal.getId());
        data.writeString(modal.getTitle());

        List<Modal.Item> items = modal.getItems();
        data.writeVarInt(items.size());
        for (Modal.Item item : items) {
            data.writeVarInt(item.getId());
            data.writeString(item.getHtml());
        }
    }
}
