package me.megamichiel.mymclab.packet.messaging;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.DefaultPermission;
import me.megamichiel.mymclab.util.ColoredText;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MessagePacket extends Packet {

    public static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private static final byte ID = getId(MessagePacket.class);

    private final boolean isChat;
    private final Message[] messages;

    public MessagePacket(boolean isChat, Message... msgs) {
        super(ID);
        this.isChat = isChat;
        messages = msgs;
    }

    public MessagePacket(ProtocolInput stream) throws IOException {
        super(ID);
        isChat = stream.readBoolean();
        int length = stream.readUnsignedByte();
        messages = new Message[length];
        for (int i = 0; i < length; i++) {
            long at = stream.readVarLong();
            LogLevel level = stream.readEnum(LogLevel.class);
            messages[i] = new Message(at, level, new ColoredText(stream));
        }
    }

    public boolean isChat() {
        return isChat;
    }

    public Message[] getMessages() {
        return messages;
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeBoolean(isChat);
        data.writeByte(messages.length);
        for (Message msg : messages) {
            data.writeVarLong(msg.at);
            data.writeEnum(msg.level);
            msg.message.write(data);
        }
    }

    @Override
    public DefaultPermission getPermission() {
        return isChat ? DefaultPermission.VIEW_CHAT : DefaultPermission.VIEW_CONSOLE;
    }

    public static class Message {

        private final long at;
        private final LogLevel level;
        private final ColoredText message;

        public Message(long at, LogLevel level, ColoredText message) {
            this.at = at;
            this.level = level;
            this.message = message;
        }

        public long getAt() {
            return at;
        }

        public LogLevel getLevel() {
            return level;
        }

        public ColoredText getMessage() {
            return message;
        }

        public String timeToString() {
            return TIME_FORMAT.format(new Date(at));
        }
    }
    
    public enum LogLevel {
        OFF(0),
        SEVERE(12),
        ERROR(12),
        WARNING(12),
        INFO(0),
        DEBUG(10),
        TRACE(10),
        ALL(0);

        private final int color;

        LogLevel(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }
}
