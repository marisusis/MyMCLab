package me.megamichiel.mymclab;

public interface MyMCLab {

    byte[] HEADER = { 77, 121, 77, 67, 76, 97, 98, 32, 82, 111, 99, 107, 115 };

    short PROTOCOL_VERSION = 0;

    int COMPRESSION_THRESHOLD = 255;
}
