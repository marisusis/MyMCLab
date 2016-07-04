package me.megamichiel.mymclab.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import java.util.Arrays;

public class EncryptionHandler {

    private final Cipher encryptCipher, decryptCipher;
    private byte[] encryptBuffer = new byte[0];

    public EncryptionHandler(SecretKey key) {
        encryptCipher = Encryption.createAESCipher(Cipher.ENCRYPT_MODE, key);
        decryptCipher = Encryption.createAESCipher(Cipher.DECRYPT_MODE, key);
    }

    public byte[] decrypt(byte[] input) throws ShortBufferException {
        byte[] out = new byte[decryptCipher.getOutputSize(input.length)];
        decryptCipher.update(input, 0, input.length, out, 0);
        return out;
    }

    public byte[] encrypt(byte[] input) throws ShortBufferException {
        int length = input.length;

        int j = encryptCipher.getOutputSize(length);
        if (encryptBuffer.length < j) encryptBuffer = new byte[j];

        return Arrays.copyOf(encryptBuffer, encryptCipher.update(input, 0, length, encryptBuffer));
    }
}
