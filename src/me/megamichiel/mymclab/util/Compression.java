package me.megamichiel.mymclab.util;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Compression {

    private final Deflater deflater = new Deflater();
    private final byte[] deflateBuffer = new byte[8192];

    public byte[] compress(byte[] input) {
        deflater.setInput(input);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        while (!deflater.finished())
            out.write(deflateBuffer, 0, deflater.deflate(deflateBuffer));
        deflater.reset();
        return out.toByteArray();
    }

    private final Inflater inflater = new Inflater();

    public byte[] decompress(byte[] input, int length) {
        inflater.setInput(input);
        byte[] output = new byte[length];
        try {
             inflater.inflate(output);
        } catch (DataFormatException ex) {
            ex.printStackTrace();
        }
        inflater.reset();
        return output;
    }
}
