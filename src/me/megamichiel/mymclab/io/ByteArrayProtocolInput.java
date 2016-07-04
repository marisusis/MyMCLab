package me.megamichiel.mymclab.io;

import java.io.*;

public class ByteArrayProtocolInput extends ByteArrayInputStream implements ProtocolInput {

    public ByteArrayProtocolInput() {
        super(new byte[0]);
    }

    public ByteArrayProtocolInput(byte[] buf) {
        super(buf);
    }

    public boolean isReadable() {
        return available() > 0;
    }

    public byte readByte() throws IOException {
        int temp = read();
        if (temp < 0) {
            throw new EOFException();
        }
        return (byte) temp;
    }

    @Override
    public String readLine() throws IOException {
        InputStream in = this;
        StringBuilder line = new StringBuilder(80); // Typical line length
        boolean foundTerminator = false;
        while (true) {
            int nextByte = read();
            switch (nextByte) {
                case -1:
                    if (line.length() == 0 && !foundTerminator) {
                        return null;
                    }
                    return line.toString();
                case (byte) '\r':
                    if (foundTerminator && in instanceof PushbackInputStream) {
                        ((PushbackInputStream) in).unread(nextByte);
                        return line.toString();
                    }
                    foundTerminator = true;
                    /* Have to be able to peek ahead one byte */
                    if (!(in.getClass() == PushbackInputStream.class)) {
                        in = new PushbackInputStream(in);
                    }
                    break;
                case (byte) '\n':
                    return line.toString();
                default:
                    if (foundTerminator && in instanceof PushbackInputStream) {
                        ((PushbackInputStream) in).unread(nextByte);
                        return line.toString();
                    }
                    line.append((char) nextByte);
            }
        }
    }

    @Override
    public int skipBytes(int count) throws IOException {
        return (int) skip(count);
    }

    public void setArray(byte[] array) {
        buf = array;
        pos = 0;
        count = array.length;
        mark = 0;
    }
}
