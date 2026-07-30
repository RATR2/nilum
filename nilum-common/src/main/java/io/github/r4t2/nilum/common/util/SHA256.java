package io.github.r4t2.nilum.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for generating hex-encoded SHA-256 hashes.
 */
public final class SHA256 {

    private SHA256() {
    }

    /**
     * Computes the SHA-256 cryptographic hash of the provided byte array and
     * returns it as a hexadecimal string.
     *
     * @param data the input byte array to be hashed
     * @return a 64-character lowercase hexadecimal string representing the SHA-256 digest
     * @throws IllegalStateException if the SHA-256 algorithm is not supported by the JDK environment
     * @throws NullPointerException if the provided {@code data} array is null
     */
    public static String of(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is a required JDK algorithm", e);
        }
    }
}
