package elite.intel.ai.mouth.supertonic.stress;

import ai.onnxruntime.OrtException;
import elite.intel.ai.embed.DjlTokenizerNatives;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stress marks on Russian text before Supertonic reads it.
 * <p>
 * Supertonic reads raw letters and guesses the stress, and Russian writes none down, so a word whose meaning
 * turns on the stress came out wrong ("моего голоса" with the plural's голосА). A combining acute after the
 * vowel moves it, so RUAccent's models put one on every word ({@link Accentuator}), and ё back where the writer
 * dropped it. This covers everything Supertonic says in Russian - our strings, VEGA's replies, the number words
 * and a Russian client's radio chatter.
 * <p>
 * The models load once, on first use or at {@link #warmUp()}, from {@code distribution/tts/ruaccent-tiny2.1}.
 */
public final class RussianStress {

    private static final Logger log = LogManager.getLogger(RussianStress.class);
    private static final String MODEL_DIR = "ruaccent-tiny2.1";

    private static Accentuator accentuator;
    private static boolean unavailable;

    private RussianStress() {
    }

    /**
     * The sentence with its stress marked and its ё restored, or the sentence as it came.
     * <p>
     * WHY the fallback: unmarked Russian is what Supertonic spoke before this existed - mostly right, some words
     * stressed wrong. A missing model or a failed inference must cost the commander those words, never the
     * sentence. Both are logged.
     */
    public static String mark(String sentence) {
        if (sentence == null || sentence.isBlank()) return sentence;
        Accentuator loaded = loaded();
        if (loaded == null) return sentence;
        try {
            return loaded.accentuate(sentence);
        } catch (OrtException e) {
            log.error("Russian stress marking failed, speaking the sentence unmarked: {}", e.getMessage());
            return sentence;
        }
    }

    /**
     * Loads the models now, so the first Russian sentence does not wait for them.
     */
    public static void warmUp() {
        loaded();
    }

    private static synchronized Accentuator loaded() {
        if (accentuator != null || unavailable) return accentuator;
        Path modelDir = AppPaths.getTtsModelDir().resolve(MODEL_DIR);
        try {
            if (!Files.isDirectory(modelDir))
                throw new IOException("not found - run the installer to download TTS models");
            DjlTokenizerNatives.configure();
            long started = System.nanoTime();
            accentuator = new Accentuator(modelDir);
            log.info("Russian stress models loaded from {} in {} ms", modelDir, (System.nanoTime() - started) / 1_000_000);
        } catch (IOException | OrtException | RuntimeException e) {
            // WHY: see mark() - Russian is spoken unmarked for the rest of the session rather than not at all.
            // RuntimeException too, because that is how DJL reports a tokenizer native that will not load.
            unavailable = true;
            log.error("Russian stress models unavailable at {}, Russian is spoken without stress marks: {}",
                    modelDir, e.getMessage());
        }
        return accentuator;
    }
}
