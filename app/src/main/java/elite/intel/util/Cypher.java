package elite.intel.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Set;

/**
 * NOTE: From the author: This is not a proper security solution!!!
 * It can be used to encrypt API keys stored in the local database,
 * but the key is stored on the same computer. Without the secret key
 * the API keys can't be read in database. So database can be backed up.
 * But if your computer is compromised the attacker will find this key
 * and use it to decrypt the API keys.
 * <p>
 * An alternative would be asking user too enter password every time
 * the app starts. However, if someone has full access to your computer
 * (cough Microsoft) you have a bigger problems.
 */
public class Cypher {

    private static SecretKey secretKey;

    public static void initializeKey() {
        try {
            File keyFile = new File(AppPaths.getSecretKeyFile());
            if (keyFile.exists()) {
                loadKeyFromFile();
            } else {
                generateAndSaveKey();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }

    /**
     * Generates a 256-bit AES secret key and saves it to a file.
     *
     * The key is generated using the AES algorithm with a key size of 256 bits.
     * The key is written to the file specified by the {@code getSecretKeyFile}
     * method in the AppPaths class. If the directory for the key file does not
     * exist, it is created. The file's permissions are restricted to allow
     * read and write access only to the owner.
     *
     * @throws Exception if there is an error during key generation, directory creation,
     *                   file writing, or setting file permissions.
     */
    private static void generateAndSaveKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // 256-bit key
        secretKey = keyGen.generateKey();

        File dir = new File(AppPaths.getSecretKeyFile()).getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(AppPaths.getSecretKeyFile())) {
            fos.write(secretKey.getEncoded());
        }

        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(new File(AppPaths.getSecretKeyFile()).toPath(), perms);
        } catch (UnsupportedOperationException e) {
            File file = new File(AppPaths.getSecretKeyFile());
            file.setReadable(true, true); // owner only
            file.setWritable(true, true); // owner only
        }
    }

    private static void loadKeyFromFile() throws Exception {
        byte[] keyBytes = new byte[32]; // 256-bit = 32 bytes
        try (FileInputStream fis = new FileInputStream(AppPaths.getSecretKeyFile())) {
            int bytesRead = fis.read(keyBytes);
            if (bytesRead != 32) {
                throw new Exception("Invalid key file");
            }
        }
        secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts the given plaintext string using AES encryption and encodes the result in Base64.
     * <br><br>
     * This method utilizes a pre-generated secret key to perform encryption. If the secret key
     * is not already initialized, it will be loaded or generated at runtime. The AES encryption
     * algorithm is used in conjunction with a randomly generated 256-bit key.
     *
     * @param data the plaintext string to be encrypted. If null, the method returns null.
     * @return the encrypted string encoded in Base64, or null if the input data is null.
     * @throws Exception if an error occurs during encryption or key initialization.
     */
    public static String encrypt(String data) {
        try {
            if (data == null) {
                return null;
            }
            if(data.isEmpty()){
                return null;
            }
            if (secretKey == null) {
                initializeKey();
            }
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }


    /**
     * Whether {@code encryptedData} was encrypted with {@code keyBytes} - asked of a restored database, whose API
     * keys may have been written under another computer's key. Says nothing about what the data holds.
     */
    public static boolean decryptsWith(byte[] keyBytes, String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"));
            cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return true;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Decrypts the given encrypted data using AES decryption and returns the plaintext string.
     *
     * This method utilizes a pre-initialized secret key to perform decryption. If the secret
     * key is not initialized, it will be automatically loaded or generated at runtime. The
     * input data should be encoded in Base64 format.
     *
     * @param encryptedData the encrypted string in Base64 format to be decrypted. If null, the method returns null.
     * @return the decrypted plaintext string, or null if the input was null.
     * @throws Exception if an error occurs during decryption or key initialization.
     */
    public static String decrypt(String encryptedData) {
        try {
            if (encryptedData == null) {
                return "";
            }
            if(encryptedData.isEmpty()){
                return "";
            }
            if (secretKey == null) {
                initializeKey();
            }
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }
}