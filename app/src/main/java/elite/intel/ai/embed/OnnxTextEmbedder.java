package elite.intel.ai.embed;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * In-process {@link TextEmbedder} backed by the multilingual-e5-small int8 ONNX model shipped in
 * {@code distribution/embed/} (same packaging path as Parakeet/Kokoro). Runs entirely on the CPU out of
 * system RAM (~130 MB resident) — it never touches the GPU the game is using — and turns a short phrase
 * into a unit-length 384-d vector in a few milliseconds.
 * <p>
 * Pipeline: the DJL HuggingFace tokenizer converts text into the model's input ids; ONNX Runtime runs the
 * transformer to per-token vectors; we mean-pool those (attention-mask weighted) and L2-normalise. e5
 * models are trained with an instruction prefix, so {@value #E5_PREFIX} is prepended to every input — the
 * same on both sides of a comparison, which is what e5 expects for symmetric short-text matching.
 * <p>
 * Not thread-safe for concurrent {@link #embed} on one instance (one ONNX session); create one per worker
 * or guard externally. {@link #close()} releases the native session and tokenizer; the shared
 * {@link OrtEnvironment} is a process singleton and is deliberately left open.
 */
public final class OnnxTextEmbedder implements TextEmbedder, AutoCloseable {

    private static final Logger log = LogManager.getLogger(OnnxTextEmbedder.class);

    /**
     * e5 models expect this instruction prefix; applied uniformly so both sides of a match are comparable.
     */
    static final String E5_PREFIX = "query: ";

    private final HuggingFaceTokenizer tokenizer;
    private final OrtEnvironment env;
    private final OrtSession session;
    private final Set<String> inputNames;
    private final int dimensions;

    /**
     * Production: load the model bundled in {@code distribution/embed/} on a single CPU thread.
     */
    public OnnxTextEmbedder() {
        this(AppPaths.getEmbedModelDir(), 1);
    }

    /**
     * Test/advanced: load from an explicit model directory.
     *
     * @param modelDir directory containing {@code model_quantized.onnx} and {@code tokenizer.json}
     * @param threads  intra-op CPU threads (keep low so the embedder does not starve the game)
     */
    public OnnxTextEmbedder(Path modelDir, int threads) {
        Path modelFile = modelDir.resolve("model_quantized.onnx");
        Path tokenizerFile = modelDir.resolve("tokenizer.json");
        if (!Files.exists(modelFile)) {
            throw new IllegalStateException("Embedding model missing at: " + modelFile);
        }
        if (!Files.exists(tokenizerFile)) {
            throw new IllegalStateException("Embedding tokenizer missing at: " + tokenizerFile);
        }
        try {
            // Must run before the first tokenizer load so DJL uses the jar-bundled CPU native, not a download.
            DjlTokenizerNatives.configure();
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile);
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setIntraOpNumThreads(Math.max(1, threads));
            this.session = env.createSession(modelFile.toAbsolutePath().toString(), options);
            this.inputNames = session.getInputNames();
            // Warm-up: pays the first-call cost up front and discovers the output dimensionality.
            this.dimensions = embedRaw(E5_PREFIX).length;
            log.info("Embedding model loaded from {} ({}-d, inputs={})", modelDir, dimensions, inputNames);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise ONNX text embedder from " + modelDir, e);
        }
    }

    @Override
    public float[] embed(String text) {
        return embedRaw(E5_PREFIX + (text == null ? "" : text));
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    /**
     * Tokenise -> run -> mean-pool -> normalise, for already-prefixed text.
     */
    private float[] embedRaw(String text) {
        Encoding encoding = tokenizer.encode(text);
        long[] ids = encoding.getIds();
        long[] mask = encoding.getAttentionMask();
        long[] typeIds = encoding.getTypeIds();
        int seqLen = ids.length;

        Map<String, OnnxTensor> inputs = new HashMap<>();
        try {
            inputs.put("input_ids", OnnxTensor.createTensor(env, new long[][]{ids}));
            inputs.put("attention_mask", OnnxTensor.createTensor(env, new long[][]{mask}));
            // BERT-architecture models (e5-small) declare token_type_ids; RoBERTa-architecture ones do not.
            if (inputNames.contains("token_type_ids")) {
                inputs.put("token_type_ids", OnnxTensor.createTensor(env, new long[][]{typeIds}));
            }
            try (OrtSession.Result result = session.run(inputs)) {
                // last_hidden_state: [batch=1][seq][hidden]
                float[][][] hidden = (float[][][]) result.get(0).getValue();
                return VectorMath.normalizeInPlace(meanPool(hidden[0], mask, seqLen));
            }
        } catch (OrtException e) {
            throw new IllegalStateException("Embedding inference failed", e);
        } finally {
            inputs.values().forEach(OnnxTensor::close);
        }
    }

    /**
     * Attention-mask-weighted average of per-token vectors (e5's pooling), ignoring padding positions.
     */
    private static float[] meanPool(float[][] tokenVectors, long[] mask, int seqLen) {
        int hidden = tokenVectors[0].length;
        float[] pooled = new float[hidden];
        long counted = 0;
        for (int t = 0; t < seqLen; t++) {
            if (mask[t] == 0) {
                continue;
            }
            counted++;
            float[] vec = tokenVectors[t];
            for (int h = 0; h < hidden; h++) {
                pooled[h] += vec[h];
            }
        }
        if (counted > 0) {
            for (int h = 0; h < hidden; h++) {
                pooled[h] /= counted;
            }
        }
        return pooled;
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            log.warn("Error closing ONNX session: {}", e.getMessage());
        }
        tokenizer.close();
    }
}
