package elite.intel.appdata;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SealedKeyTest {

    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    @Test
    void theRightPasswordGivesBackTheKey() throws Exception {
        byte[] sealed = SealedKey.seal(KEY, "fly safe".toCharArray());

        assertArrayEquals(KEY, SealedKey.unseal(sealed, "fly safe".toCharArray()));
    }

    @Test
    void aWrongPasswordIsToldApart() {
        byte[] sealed = SealedKey.seal(KEY, "fly safe".toCharArray());

        assertThrows(SealedKey.WrongPasswordException.class, () -> SealedKey.unseal(sealed, "fly dangerous".toCharArray()));
    }

    @Test
    void somethingThatIsNotASealedKeyIsRefused() {
        assertThrows(IOException.class, () -> SealedKey.unseal(KEY, "fly safe".toCharArray()));
    }
}
