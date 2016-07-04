package me.megamichiel.mymclab.packet.player;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.util.ColoredText;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static me.megamichiel.mymclab.packet.player.StatisticPacket.StatisticField.*;

public class StatisticPacket extends Packet {

    private static final byte ID = getId(StatisticPacket.class);

    private final StatisticItemAction action;
    private final List<StatisticInfo> values;

    public StatisticPacket(StatisticItemAction action, List<StatisticInfo> values) {
        super(ID);
        this.action = action;
        this.values = values;
    }

    public StatisticPacket(ProtocolInput data) throws IOException {
        super(ID);
        action = data.readEnum(StatisticItemAction.class);
        int size = data.readUnsignedShort();
        StatisticInfo[] values = new StatisticInfo[size];
        for (int i = 0; i < size; i++) values[i] = deserializePlayerInfo(data);
        this.values = Arrays.asList(values);
    }

    public StatisticItemAction getAction() {
        return action;
    }

    public List<StatisticInfo> getValues() {
        return values;
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeEnum(action);
        data.writeShort(values.size());
        for (StatisticInfo info : values) info.serialize(data);
    }

    public static StatisticInfo deserializePlayerInfo(ProtocolInput data) throws IOException {
        String name = data.readString();
        int size = data.readUnsignedShort();
        StatisticItem[] items = new StatisticItem[size];
        for (int i = 0; i < size; i++) {
            items[i] = new StatisticItem(data);
        }
        return new StatisticInfo(name, Arrays.asList(items));
    }

    public static class StatisticInfo {

        private final String name;
        private final List<StatisticItem> description;

        public StatisticInfo(String name, List<StatisticItem> description) {
            this.name = name;
            this.description = description;
        }

        public void serialize(ProtocolOutput data) throws IOException {
            data.writeString(name);
            data.writeShort(description.size());
            for (StatisticItem item : description) item.write(data);
        }

        public String getName() {
            return name;
        }

        public List<StatisticItem> getDescription() {
            return description;
        }
    }

    public static class StatisticItem {

        private int modified;
        private final StatisticType type;
        private ColoredText text;

        private double value, max;
        private final int progressColor, emptyColor;

        public StatisticItem(int modified, StatisticType type, ColoredText text,
                             double value, double max,
                             int progressColor, int emptyColor) {
            this.modified = modified;
            this.type = type;
            this.text = text;
            this.value = value;
            this.max = max;
            this.progressColor = progressColor;
            this.emptyColor = emptyColor;
        }

        public StatisticItem(ProtocolInput data) throws IOException {
            modified = data.readByte() & 0xFF;
            type = TYPE.test(modified) ? data.readEnum(StatisticType.class) : null;
            text = TEXT.test(modified) ? new ColoredText(data) : null;
            value = VALUE.test(modified) ? data.readDouble() : 0;
            max = MAX.test(modified) ? data.readDouble() : 0;
            progressColor = PROGRESS_COLOR.test(modified) ? data.readInt() : 0;
            emptyColor = EMPTY_COLOR.test(modified) ? data.readInt() : 0;
        }

        public void write(ProtocolOutput data) throws IOException {
            data.writeByte(modified);

            if (TYPE.test(modified)) data.writeEnum(type);
            if (TEXT.test(modified)) text.write(data);
            if (VALUE.test(modified)) data.writeDouble(value);
            if (MAX.test(modified)) data.writeDouble(max);
            if (PROGRESS_COLOR.test(modified)) data.writeInt(progressColor);
            if (EMPTY_COLOR.test(modified)) data.writeInt(emptyColor);
        }

        public int getModified() {
            return modified;
        }

        public void setModified(int modified) {
            this.modified = modified;
        }

        public ColoredText getText() {
            return text;
        }

        public void setText(ColoredText text) {
            this.text = text;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public double getMax() {
            return max;
        }

        public void setMax(double max) {
            this.max = max;
        }
    }

    public enum StatisticType {
        TEXT, PROGRESS_CIRCLE, PROGRESS_BAR
    }

    public enum StatisticItemAction {
        ADD, UPDATE, REMOVE
    }

    public enum StatisticField {

        TYPE, TEXT, VALUE, MAX, PROGRESS_COLOR, EMPTY_COLOR;

        private final int index;

        StatisticField() {
            index = 1 << ordinal();
        }

        public int get() {
            return index;
        }

        public boolean test(int i) {
            return (i & index) == index;
        }
    }
}
