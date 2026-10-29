package elite.intel.ai.mouth.supertonic.stress;

import ai.onnxruntime.OrtException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import elite.intel.ai.embed.DjlTokenizerNatives;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the port to RUAccent itself. {@code stress/ruaccent-reference.json} is what the Python original (1.5.8.3,
 * tiny mode, tiny2.1, model revision b78ae5e) gives for each sentence, with the one change the port makes on
 * purpose - homographs looked up in lower case, see {@link Accentuator}. Its misses are pinned too
 * ("узн+аю об этом завтра"): this checks the port, and the model's accuracy is the model's.
 */
class AccentuatorTest {

    private static final String MODEL_DIR = "distribution/tts/ruaccent-tiny2.1";

    private static Accentuator accentuator;

    @BeforeAll
    static void loadModels() throws IOException, OrtException {
        Path modelDir = locate();
        assumeTrue(modelDir != null, MODEL_DIR + " not found from " + Path.of("").toAbsolutePath());
        Path model = modelDir.resolve("nn/nn_omograph/tiny2.1/model.onnx");
        try (InputStream in = Files.newInputStream(model)) {
            assumeTrue(in.read() != 'v', model + " is an LFS pointer; run git lfs pull");
        }
        DjlTokenizerNatives.configure();
        accentuator = new Accentuator(modelDir);
    }

    @AfterAll
    static void closeModels() throws OrtException {
        if (accentuator != null) accentuator.close();
    }

    @Test
    void everyReferenceSentenceComesOutAsRuAccentMarksIt() throws IOException, OrtException {
        List<String> mismatches = new ArrayList<>();
        for (Reference reference : references()) {
            String expected = StressMarks.fromPlusNotation(reference.out());
            String actual = accentuator.accentuate(reference.in());
            if (!expected.equals(actual))
                mismatches.add(reference.in() + "\n  expected " + expected + "\n  actual   " + actual);
        }
        assertTrue(mismatches.isEmpty(), String.join("\n", mismatches));
    }

    @Test
    void whatRuAccentWouldDeleteIsStillThere() throws OrtException {
        String marked = accentuator.accentuate("Щиты 50% / +5 к броне…");
        assertEquals("Щиты 50% / +5 к броне…", marked.replace(String.valueOf(StressMarks.COMBINING_ACUTE), ""));
    }

    private record Reference(String in, String out) {
    }

    private static List<Reference> references() throws IOException {
        try (InputStream in = AccentuatorTest.class.getResourceAsStream("/stress/ruaccent-reference.json");
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, new TypeToken<List<Reference>>() {
            }.getType());
        }
    }

    private static Path locate() {
        Path dir = Path.of("").toAbsolutePath();
        for (int up = 0; up < 4 && dir != null; up++, dir = dir.getParent()) {
            Path candidate = dir.resolve(MODEL_DIR);
            if (Files.isDirectory(candidate)) return candidate;
        }
        return null;
    }
}
