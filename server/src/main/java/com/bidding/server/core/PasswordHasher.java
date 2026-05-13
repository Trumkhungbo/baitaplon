package com.bidding.server.core;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {

    private static final String PREFIX = "pbkdf2$";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(salt);
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return PREFIX
                    + ITERATIONS
                    + "$"
                    + Base64.getEncoder().encodeToString(salt)
                    + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Unable to hash password", e);
        }
    }

    public static boolean matches(String rawPassword, String storedValue) {
        if (rawPassword == null || storedValue == null) {
            return false;
        }

        if (!storedValue.startsWith(PREFIX)) {
            return MessageDigest.isEqual(rawPassword.getBytes(), storedValue.getBytes());
        }

        try {
            String[] parts = storedValue.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = pbkdf2(rawPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean needsUpgrade(String storedValue) {
        return storedValue == null || !storedValue.startsWith(PREFIX);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }
}
