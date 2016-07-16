package me.megamichiel.mymclab.packet.messaging;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.DefaultPermission;

import java.io.IOException;

public class RawMessagePacket extends Packet {

    private static final byte ID = getId(RawMessagePacket.class);

    private final RawMessageType type;
    private final String message;

    public RawMessagePacket(RawMessageType type, String message) {
        super(ID);
        this.type = type;
        this.message = message;
    }

    public RawMessagePacket(ProtocolInput data) throws IOException {
        super(ID);
        type = data.readEnum(RawMessageType.class);
        message = data.readString();
    }

    public RawMessageType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeEnum(type);
        data.writeString(message);
    }

    @Override
    public DefaultPermission getPermission() {
        switch (type) {
            case CHAT: return DefaultPermission.INPUT_CHAT;
            case COMMAND: return DefaultPermission.INPUT_COMMANDS;
            default: return null;
        }
    }

    public enum RawMessageType {
        DISCONNECT, TOAST, COMMAND, CHAT
    }
}
