package me.megamichiel.mymclab.packet.player;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.DefaultPermission;

import java.io.IOException;

public class PromptRequestPacket extends Packet {

    private static final byte ID = getId(PromptRequestPacket.class);

    private final String player, name;
    private final int itemIndex;
    private final PromptRequest[] requests;

    public PromptRequestPacket(String player, String name,
                               int itemIndex, PromptRequest... requests) {
        super(ID);
        this.player = player;
        this.name = name;
        this.itemIndex = itemIndex;
        this.requests = requests;
    }

    public PromptRequestPacket(ProtocolInput data) throws IOException {
        super(ID);
        player = data.readString();
        name = data.readString();
        itemIndex = data.readUnsignedShort();
        int size = data.readUnsignedByte();
        requests = new PromptRequest[size];
        for (int i = 0; i < size; i++)
            requests[i] = data.readEnum(PromptType.class).read(data);
    }

    public String getPlayer() {
        return player;
    }

    public String getName() {
        return name;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    public PromptRequest[] getRequests() {
        return requests;
    }

    @Override
    public DefaultPermission getPermission() {
        return player == null ? DefaultPermission.VIEW_SERVER_INFO : DefaultPermission.VIEW_PLAYERS;
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeString(player);
        data.writeString(name);
        data.writeShort(itemIndex);
        data.writeByte(requests.length);
        for (PromptRequest request : requests) request.write(data);
    }

    public static class PromptRequest {

        private final String name;
        private final PromptType type;

        public PromptRequest(String name, PromptType type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public PromptType getType() {
            return type;
        }

        public void write(ProtocolOutput data) throws IOException {
            data.writeEnum(type);
            data.writeString(name);
        }
    }

    public static class SelectionPromptRequest extends PromptRequest {

        private final String[] values;

        public SelectionPromptRequest(String name, PromptType type, String[] values) {
            super(name, type);
            this.values = values;
        }

        public String[] getValues() {
            return values;
        }

        @Override
        public void write(ProtocolOutput data) throws IOException {
            super.write(data);
            data.writeByte(values.length);
            for (String str : values) data.writeString(str);
        }
    }

    public enum PromptType {
        TEXT, PASSWORD, CHECKBOX, NUMBER, DECIMAL_NUMBER, SELECTION {
            @Override
            PromptRequest read(ProtocolInput data) throws IOException {
                String name = data.readString();
                String[] values = new String[data.readUnsignedByte()];
                for (int i = 0; i < values.length; i++)
                    values[i] = data.readString();
                return new SelectionPromptRequest(name, this, values);
            }
        };

        PromptRequest read(ProtocolInput data) throws IOException {
            return new PromptRequest(data.readString(), this);
        }
    }
}
