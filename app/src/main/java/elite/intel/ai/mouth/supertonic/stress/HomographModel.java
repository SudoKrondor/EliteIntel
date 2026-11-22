package elite.intel.ai.mouth.supertonic.stress;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * RUAccent's homograph judge: given the sentence with the homograph tagged and one stressed reading of it, how
 * likely that reading is the one the sentence means. The reading it rates highest wins.
 */
final class HomographModel implements AutoCloseable {

    private final HuggingFaceTokenizer tokenizer;
    private final OrtSession session;

    HomographModel(Path modelDir) throws IOException, OrtException {
        this.tokenizer = HuggingFaceTokenizer.newInstance(modelDir.resolve("tokenizer.json"),
                Map.of("truncation", "true", "maxLength", "512", "padding", "false"));
        this.session = OnnxModels.open(modelDir);
    }

    /**
     * The reading the sentence means, as RUAccent decides it: each one scored on its own, the first best kept.
     *
     * @param context  the sentence from {@link SentenceWords#homographContext}
     * @param readings the homograph's stressed forms, in {@code +} notation
     */
    String choose(String context, List<String> readings) throws OrtException {
        // RUAccent's last tidy-up: no space before closing punctuation.
        String text = context.replaceAll("\\s+(?=[,.?!:;…])", "");
        String best = readings.getFirst();
        double bestScore = Double.NEGATIVE_INFINITY;
        for (String reading : readings) {
            double score = probabilityMeant(text, reading);
            if (score > bestScore) {
                best = reading;
                bestScore = score;
            }
        }
        return best;
    }

    private double probabilityMeant(String text, String reading) throws OrtException {
        Encoding encoding = tokenizer.encode(text, reading);
        float[][] logits = (float[][]) OnnxModels.run(session, Map.of(
                "input_ids", encoding.getIds(), "attention_mask", encoding.getAttentionMask()));
        return OnnxModels.softmax(logits[0])[1];
    }

    @Override
    public void close() throws OrtException {
        session.close();
        tokenizer.close();
    }
}
