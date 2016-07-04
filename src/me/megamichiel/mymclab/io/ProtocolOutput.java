package me.megamichiel.mymclab.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.UTFDataFormatException;

public interface ProtocolOutput extends DataOutput {

    @Override
    default void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    default void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        for (int i = 0 ; i < len ; i++) {
            write(b[off + i]);
        }
    }

    @Override
    default void writeByte(int v) throws IOException {
        write(v);
    }

    @Override
    default void writeShort(int v) throws IOException {
        write((v >>> 8) & 0xFF);
        write(v & 0xFF);
    }

    @Override
    default void writeChar(int v) throws IOException {
        writeShort(v);
    }

    @Override
    default void writeInt(int v) throws IOException {
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>> 8) & 0xFF);
        write(v & 0xFF);
    }

    @Override
    default void writeLong(long v) throws IOException {
        write((int) ((v >>> 56) & 0xFF));
        write((int) ((v >>> 48) & 0xFF));
        write((int) ((v >>> 40) & 0xFF));
        write((int) ((v >>> 32) & 0xFF));
        write((int) ((v >>> 24) & 0xFF));
        write((int) ((v >>> 16) & 0xFF));
        write((int) ((v >>> 8) & 0xFF));
        write((int) (v & 0xFF));
    }

    @Override
    default void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    @Override
    default void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    default void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    default void writeBytes(String s) throws IOException {
        if (s.length() == 0) {
            return;
        }
        for (char c : s.toCharArray()) write(c);
    }

    @Override
    default void writeChars(String s) throws IOException {
        write(s.getBytes("UTF-16BE"));
    }

    @Override
    default void writeUTF(String s) throws IOException {
        int strlen = s.length();
        int utflen = 0;
        int c;

        /* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            c = s.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) utflen++;
            else if (c > 0x07FF) utflen += 3;
            else utflen += 2;
        }

        if (utflen > 65535)
            throw new UTFDataFormatException(
                    "encoded string too long: " + utflen + " bytes");

        writeShort(utflen);

        int i = 0;
        for (; i < strlen; i++) {
            c = s.charAt(i);
            if (!((c >= 0x0001) && (c <= 0x007F))) break;
            write(c);
        }

        for (;i < strlen; i++){
            c = s.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                write(c);
            } else if (c > 0x07FF) {
                write((byte) (0xE0 | ((c >> 12) & 0x0F)));
                write((byte) (0x80 | ((c >>  6) & 0x3F)));
                write((byte) (0x80 | (c & 0x3F)));
            } else {
                write((byte) (0xC0 | ((c >>  6) & 0x1F)));
                write((byte) (0x80 | (c & 0x3F)));
            }
        }
    }

    default void writeVarInt(int i) throws IOException {
        while ((i & 0xFFFFFF80) != 0) {
            write((i & 0x7F) | 0x80);
            i >>>= 7;
        }
        write(i);
    }

    default void writeVarLong(long l) throws IOException {
        while ((l & 0xFFFFFFFFFFFFFF00L) != 0) {
            write((int) (l & 0x7F) | 0x80);
            l >>>= 7;
        }
        write((byte) l);
    }

    default void writeString(String str) throws IOException {
        if (str == null) writeVarInt(1);
        else {
            byte[] b = str.getBytes(ProtocolInput.UTF_8);
            writeVarInt(b.length << 1);
            write(b);
        }
    }

    default <E extends Enum<E>> void writeEnum(E val) throws IOException {
        write(val.ordinal());
    }
}
