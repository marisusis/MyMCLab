package me.megamichiel.mymclab.io;

import java.io.ByteArrayOutputStream;

public class ByteArrayProtocolOutput extends ByteArrayOutputStream implements ProtocolOutput {

    public ByteArrayProtocolOutput() {
        super();
    }

    public ByteArrayProtocolOutput(int size) {
        super(size);
    }

    public ByteArrayProtocolOutput(byte[] buffer, int start) {
        buf = buffer;
        count = start;
    }
}
