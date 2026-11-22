package elite.intel.ai.mouth.supertonic.stress;

import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RUAccent's stress guess for a word no dictionary knows: reads the word one character at a time and marks the
 * characters it thinks carry the stress.
 */
final class CharAccentModel implements AutoCloseable {

    // RUAccent's cut-off: a primary-stress guess below this is left unmarked.
    private static final double MIN_SCORE = 0.55;

    private final OrtSession session;
    private final Map<Character, Long> vocabulary;
    private final long unknownId;
    private final long beginId;
    private final long endId;
    private final int primaryStress;

    CharAccentModel(Path modelDir) throws IOException, OrtException {
        List<String> tokens = Files.readAllLines(modelDir.resolve("vocab.txt"), StandardCharsets.UTF_8);
        Map<String, Long> ids = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) ids.put(tokens.get(i), (long) i);
        this.vocabulary = new HashMap<>();
        ids.forEach((token, id) -> {
            if (token.length() == 1) vocabulary.put(token.charAt(0), id);
        });
        this.unknownId = required(ids, "[unk]");
        this.beginId = required(ids, "[bos]");
        this.endId = required(ids, "[eos]");
        this.primaryStress = OnnxModels.labelIndex(modelDir, "STRESS_PRIMARY");
        this.session = OnnxModels.open(modelDir);
    }

    /**
     * The word with a {@code +} before each character the model stresses, its own capitals kept.
     */
    String putAccent(String word) throws OrtException {
        String lower = SentenceWords.lowerCaseKeepingLength(word);
        long[] inputIds = new long[lower.length() + 2];
        inputIds[0] = beginId;
        for (int i = 0; i < lower.length(); i++) inputIds[i + 1] = vocabulary.getOrDefault(lower.charAt(i), unknownId);
        inputIds[inputIds.length - 1] = endId;
        long[] mask = new long[inputIds.length];
        Arrays.fill(mask, 1);

        float[][] logits = ((float[][][]) OnnxModels.run(session, Map.of(
                "input_ids", inputIds, "attention_mask", mask, "token_type_ids", new long[inputIds.length])))[0];
        StringBuilder marked = new StringBuilder(word.length() + 2);
        for (int i = 0; i < word.length(); i++) {
            // Position 0 is the begin token, so character i is position i + 1.
            if (isPrimaryStress(logits[i + 1])) marked.append('+');
            marked.append(word.charAt(i));
        }
        return marked.toString();
    }

    private boolean isPrimaryStress(float[] logits) {
        double[] probabilities = OnnxModels.softmax(logits);
        int best = 0;
        for (int label = 1; label < logits.length; label++) {
            if (logits[label] > logits[best]) best = label;
        }
        return best == primaryStress && probabilities[best] >= MIN_SCORE;
    }

    private static long required(Map<String, Long> ids, String token) throws IOException {
        Long id = ids.get(token);
        if (id == null) throw new IOException("Stress accent vocabulary has no " + token);
        return id;
    }

    @Override
    public void close() throws OrtException {
        session.close();
    }
}
