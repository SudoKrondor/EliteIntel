package elite.intel.ai.mouth.supertonic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupertonicTTSModelAssetsTest {

    @TempDir
    Path modelDir;

    @Test
    void rejectsMissingModelAssets() throws IOException {
        Path missingIndexer = Files.createDirectory(modelDir.resolve("missing-indexer"));
        Files.createFile(missingIndexer.resolve("voice.bin"));
        Path missingVoice = Files.createDirectory(modelDir.resolve("missing-voice"));
        Files.createFile(missingVoice.resolve("unicode_indexer.bin"));

        IllegalStateException missingIndexerError = assertThrows(IllegalStateException.class,
                () -> SupertonicTTS.validateModelAssets(missingIndexer));
        assertTrue(missingIndexerError.getMessage().contains("unicode_indexer.bin"));
        assertTrue(missingIndexerError.getMessage().contains("missing-indexer"));

        IllegalStateException missingVoiceError = assertThrows(IllegalStateException.class,
                () -> SupertonicTTS.validateModelAssets(missingVoice));
        assertTrue(missingVoiceError.getMessage().contains("voice.bin"));
        assertTrue(missingVoiceError.getMessage().contains("missing-voice"));
    }

    @Test
    void acceptsCompleteModelAssets() throws IOException {
        Files.createFile(modelDir.resolve("voice.bin"));
        Files.createFile(modelDir.resolve("unicode_indexer.bin"));

        assertDoesNotThrow(() -> SupertonicTTS.validateModelAssets(modelDir));
    }
}
