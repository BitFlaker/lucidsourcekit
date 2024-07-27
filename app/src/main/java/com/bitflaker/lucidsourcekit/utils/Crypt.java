package com.bitflaker.lucidsourcekit.utils;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {
    public static String encryptString(String s, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(s.toCharArray(), salt, 21845, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    public static String encryptStringBlowfish(String s, byte[] secretKey) {
        try {
            SecretKeySpec sks = new SecretKeySpec(secretKey, "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, sks);

            byte[] encrypted = cipher.doFinal(s.getBytes());
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keygenerator;
        keygenerator = KeyGenerator.getInstance("Blowfish");
        SecretKey secretkey = keygenerator.generateKey();
        return secretkey.getEncoded();
    }
}
