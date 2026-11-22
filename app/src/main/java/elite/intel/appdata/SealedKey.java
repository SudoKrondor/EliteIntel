package elite.intel.appdata;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * The app's {@code secret.key}, locked with a password the commander chooses, so a backup can carry it without
 * carrying the means to read the API keys it protects.
 * <p>
 * WHY a password rather than the bare key: the stored API keys are encrypted with {@code secret.key}, so a zip
 * holding both is a zip holding the keys. Locked, the zip is safe to leave on a USB stick, and the new computer
 * still gets the very key the database was written with - no API key has to be typed again.
 * <p>
 * Layout: {@code EISK1}, a 16-byte salt, a 12-byte nonce, then the AES-256-GCM ciphertext with its tag. The
 * password is stretched with PBKDF2-HMAC-SHA256. GCM's tag is what tells a wrong password from a right one.
 */
final class SealedKey {

    private static final byte[] MAGIC = "EISK1".getBytes(StandardCharsets.US_ASCII);
    private static final int SALT_BYTES = 16;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BITS = 256;
    private static final int ITERATIONS = 310_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SealedKey() {
    }

    /**
     * Locks {@code key} with {@code password}.
     */
    static byte[] seal(byte[] key, char[] password) {
        byte[] salt = randomBytes(SALT_BYTES);
        byte[] nonce = randomBytes(NONCE_BYTES);
        try {
            byte[] ciphertext = cipher(Cipher.ENCRYPT_MODE, password, salt, nonce).doFinal(key);
            return ByteBuffer.allocate(MAGIC.length + SALT_BYTES + NONCE_BYTES + ciphertext.length)
                    .put(MAGIC).put(salt).put(nonce).put(ciphertext)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("This Java runtime cannot seal a key with AES-GCM", e);
        }
    }

    /**
     * The key {@code sealed} holds.
     *
     * @throws WrongPasswordException when {@code password} is not the one it was locked with
     * @throws IOException            when {@code sealed} is not a sealed key at all
     */
    static byte[] unseal(byte[] sealed, char[] password) throws WrongPasswordException, IOException {
        int headerLength = MAGIC.length + SALT_BYTES + NONCE_BYTES;
        if (sealed.length <= headerLength || !Arrays.equals(Arrays.copyOf(sealed, MAGIC.length), MAGIC)) {
            throw new IOException("Not a sealed key");
        }
        byte[] salt = Arrays.copyOfRange(sealed, MAGIC.length, MAGIC.length + SALT_BYTES);
        byte[] nonce = Arrays.copyOfRange(sealed, MAGIC.length + SALT_BYTES, headerLength);
        try {
            return cipher(Cipher.DECRYPT_MODE, password, salt, nonce)
                    .doFinal(sealed, headerLength, sealed.length - headerLength);
        } catch (AEADBadTagException e) {
            throw new WrongPasswordException();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("This Java runtime cannot open a key sealed with AES-GCM", e);
        }
    }

    private static Cipher cipher(int mode, char[] password, byte[] salt, byte[] nonce) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            byte[] stretched = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, new SecretKeySpec(stretched, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            return cipher;
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] randomBytes(int count) {
        byte[] bytes = new byte[count];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * The password does not open the sealed key.
     */
    static final class WrongPasswordException extends Exception {
        WrongPasswordException() {
            super("Wrong password for the sealed key");
        }
    }
}
