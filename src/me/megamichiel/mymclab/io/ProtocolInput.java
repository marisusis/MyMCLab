package me.megamichiel.mymclab.io;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;

public interface ProtocolInput extends DataInput {

    Charset UTF_8 = Charset.forName("UTF-8");

    int read() throws IOException;

    @Override
    default byte readByte() throws IOException {
        int i = read();
        if (i == -1) throw new EOFException();
        return (byte) i;
    }

    @Override
    default int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    @Override
    default short readShort() throws IOException {
        return (short) ((readByte() << 8) | (readByte() & 0xFF));
    }

    @Override
    default int readUnsignedShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    @Override
    default char readChar() throws IOException {
        return (char) readShort();
    }

    @Override
    default int readInt() throws IOException {
        return (readByte() << 24) | (readByte() << 16) | (readByte() << 8) | (readByte() & 0xFF);
    }

    @Override
    default long readLong() throws IOException {
        return ((long) readByte() << 56)
                | ((long) readByte() << 48)
                | ((long) readByte() << 40)
                | ((long) readByte() << 32)
                | ((long) readByte() << 24)
                | ((long) readByte() << 16)
                | ((long) readByte() << 8)
                | ((long) readByte() & 0xFFL);
    }

    @Override
    default boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    @Override
    default float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    default double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    default String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    default int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    default int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte)c;

        int i = 1;
        try {
            for (; i < len ; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                b[off + i] = (byte)c;
            }
        } catch (IOException ee) {
        }
        return i;
    }

    @Override
    default void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    default void readFully(byte[] dst, int offset, int byteCount) throws IOException {
        if (byteCount < 0) {
            throw new IndexOutOfBoundsException();
        }
        int i = 0;
        while (i < byteCount) {
            int j = read(dst, offset + i, byteCount - i);
            if (j < 0) throw new EOFException();
            i += j;
        }
    }

    default byte[] readFully(int length) throws IOException {
        byte[] b = new byte[length];
        readFully(b, 0, length);
        return b;
    }

    default int readVarInt() throws IOException {
        int result = 0;
        for (int i = 0; i < 5; i++) {
            byte read = readByte();
            result |= (read & 0x7F) << (i * 7);
            if ((read & 0x80) == 0) return result;
        }
        throw new RuntimeException("VarInt too big");
    }

    default long readVarLong() throws IOException {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            byte read = readByte();
            result |= (read & 0x7FL) << (i * 7);
            if ((read & 0x80) == 0) return result;
        }
        return result | ((long) readByte() << 56);
    }

    default String readString() throws IOException {
        int length = readVarInt();
        if (length == 1) return null;
        byte[] b = new byte[length >>> 1];
        readFully(b);
        return new String(b, UTF_8);
    }

    default <E extends Enum<E>> E readEnum(Class<E> type) throws IOException {
        return type.getEnumConstants()[readUnsignedByte()];
    }
}
