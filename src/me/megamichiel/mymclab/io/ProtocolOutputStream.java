package me.megamichiel.mymclab.io;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;

public class ProtocolOutputStream extends DataOutputStream implements ProtocolOutput {

    public ProtocolOutputStream(OutputStream out) {
        super(out);
    }
}
