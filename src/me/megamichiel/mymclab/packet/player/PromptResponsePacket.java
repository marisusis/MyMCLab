package me.megamichiel.mymclab.packet.player;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.DefaultPermission;

import java.io.IOException;

public class PromptResponsePacket extends Packet {

    private static final byte ID = getId(PromptResponsePacket.class);

    private final String player;
    private final int itemIndex;
    private final PromptResponse[] responses;

    public PromptResponsePacket(String player, int itemIndex, PromptResponse[] responses) {
        super(ID);
        this.player = player;
        this.itemIndex = itemIndex;
        this.responses = responses;
    }

    public PromptResponsePacket(ProtocolInput data) throws IOException {
        super(ID);
        player = data.readString();
        itemIndex = data.readUnsignedShort();
        int size = data.readUnsignedByte();
        responses = new PromptResponse[size];
        for (int i = 0; i < size; i++)
            responses[i] = new PromptResponse(data.readUnsignedByte(), data.readString());
    }

    public String getPlayer() {
        return player;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    public PromptResponse[] getResponses() {
        return responses;
    }

    @Override
    public DefaultPermission getPermission() {
        return player == null ? DefaultPermission.VIEW_SERVER_INFO : DefaultPermission.VIEW_PLAYERS;
    }

    public static class PromptResponse {

        private final int index;
        private final String value;

        public PromptResponse(int index, String value) {
            this.index = index;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public String getValue() {
            return value;
        }
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeString(player);
        data.writeShort(itemIndex);
        data.writeByte(responses.length);
        for (PromptResponse response : responses) {
            data.writeByte(response.index);
            data.writeString(response.value);
        }
    }
}
