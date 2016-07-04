package me.megamichiel.mymclab.util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Encryption {

    public static KeyPair generateAsymmetricKey() {
        try {
            KeyPairGenerator localKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
            localKeyPairGenerator.initialize(1024);
            return localKeyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException localNoSuchAlgorithmException) {
            throw new RuntimeException(localNoSuchAlgorithmException);
        }
    }

    public static SecretKey generateSymmetricKey() {
        try {
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(128);
            return gen.generateKey();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static PublicKey decodePublicKey(byte[] array) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(array));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException(ex);
        }
    }

    static Cipher createAESCipher(int paramInt, Key paramKey) {
        try {
            Cipher localCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            localCipher.init(paramInt, paramKey, new IvParameterSpec(paramKey.getEncoded()));
            return localCipher;
        } catch (GeneralSecurityException ex)  {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] encryptDataAndroid(Key key, byte[] data) {
        try {
            return createCipher(Cipher.ENCRYPT_MODE, "RSA/NONE/PKCS1Padding", key).doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] decryptData(Key key, byte[] data) {
        try {
            return createCipher(Cipher.DECRYPT_MODE, key.getAlgorithm(), key).doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Cipher createCipher(int mode, String algorithm, Key paramKey) {
        try {
            Cipher localCipher = Cipher.getInstance(algorithm);
            localCipher.init(mode, paramKey);
            return localCipher;
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
