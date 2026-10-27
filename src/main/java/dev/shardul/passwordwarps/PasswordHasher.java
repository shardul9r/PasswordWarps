package dev.shardul.passwordwarps;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Salted SHA-256 password hashing. Plain-text passwords are never written to disk. */
public final class PasswordHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String saltB64, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(saltB64));
            md.update(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Constant-time comparison so the check does not leak timing information. */
    public static boolean matches(Warp warp, String password) {
        if (warp.salt == null || warp.passwordHash == null) {
            return false;
        }
        byte[] expected = Base64.getDecoder().decode(warp.passwordHash);
        byte[] actual = Base64.getDecoder().decode(hash(warp.salt, password));
        return MessageDigest.isEqual(expected, actual);
    }
}
