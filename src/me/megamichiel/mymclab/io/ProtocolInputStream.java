package me.megamichiel.mymclab.io;

import java.io.DataInputStream;
import java.io.InputStream;

public class ProtocolInputStream extends DataInputStream implements ProtocolInput {

    public ProtocolInputStream(InputStream in) {
        super(in);
    }
}
