package elite.intel.ai.mouth.supertonic.stress;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.tokenizers.jni.CharSpan;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * RUAccent's ё judge for words spelled with е that take ё only in some sentences ({@code все} / {@code всё}):
 * labels each word of the sentence, and names the ones that need their ё.
 */
final class YoHomographModel implements AutoCloseable {

    private static final String SUBWORD_PREFIX = "##";

    private final HuggingFaceTokenizer tokenizer;
    private final OrtSession session;
    private final int yoLabel;

    YoHomographModel(Path modelDir) throws IOException, OrtException {
        this.tokenizer = HuggingFaceTokenizer.newInstance(modelDir.resolve("tokenizer.json"),
                Map.of("truncation", "true", "maxLength", "512", "padding", "false"));
        this.yoLabel = OnnxModels.labelIndex(modelDir, "YO");
        this.session = OnnxModels.open(modelDir);
    }

    /**
     * Where the words that need their ё begin, as offsets into {@code lowerCaseSentence}.
     * <p>
     * RUAccent pairs the model's words with its own by position in the list, which slips by one after any run of
     * punctuation the two split differently ("?!"). Matching by where a word starts cannot slip.
     */
    Set<Integer> yoWordStarts(String lowerCaseSentence) throws OrtException {
        Encoding encoding = tokenizer.encode(lowerCaseSentence);
        float[][] logits = ((float[][][]) OnnxModels.run(session, Map.of(
                "input_ids", encoding.getIds(), "attention_mask", encoding.getAttentionMask())))[0];
        String[] tokens = encoding.getTokens();
        long[] special = encoding.getSpecialTokenMask();
        CharSpan[] spans = encoding.getCharTokenSpans();

        Set<Integer> starts = new HashSet<>();
        int wordStart = -1;
        double[] summed = null;
        for (int t = 0; t < tokens.length; t++) {
            if (special[t] == 1 || spans[t] == null) continue;
            double[] probabilities = OnnxModels.softmax(logits[t]);
            if (summed != null && tokens[t].startsWith(SUBWORD_PREFIX)) {
                add(summed, probabilities);
                continue;
            }
            if (summed != null && isYo(summed)) starts.add(wordStart);
            wordStart = spans[t].getStart();
            summed = probabilities;
        }
        if (summed != null && isYo(summed)) starts.add(wordStart);
        return starts;
    }

    /**
     * RUAccent's "average" aggregation: the word's label is the best of its pieces' mean probabilities. The sum
     * ranks the labels the same way the mean does.
     */
    private boolean isYo(double[] summed) {
        int best = 0;
        for (int label = 1; label < summed.length; label++) {
            if (summed[label] > summed[best]) best = label;
        }
        return best == yoLabel;
    }

    private static void add(double[] into, double[] probabilities) {
        for (int i = 0; i < into.length; i++) into[i] += probabilities[i];
    }

    @Override
    public void close() throws OrtException {
        session.close();
        tokenizer.close();
    }
}
