package elite.intel.ai.mouth.supertonic.stress;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * What the three RUAccent models share: one CPU session per model, and the softmax over their logits.
 */
final class OnnxModels {

    private OnnxModels() {
    }

    static OrtSession open(Path modelDir) throws OrtException {
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        // WHY: one thread - a sentence is a handful of tiny inferences, and the game needs the cores.
        options.setIntraOpNumThreads(1);
        return OrtEnvironment.getEnvironment().createSession(modelDir.resolve("model.onnx").toString(), options);
    }

    /**
     * Runs a batch-of-one model on the given id rows and returns its first output.
     */
    static Object run(OrtSession session, Map<String, long[]> rows) throws OrtException {
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        Map<String, OnnxTensor> inputs = new HashMap<>();
        try {
            for (Map.Entry<String, long[]> row : rows.entrySet()) {
                inputs.put(row.getKey(), OnnxTensor.createTensor(env, new long[][]{row.getValue()}));
            }
            try (OrtSession.Result result = session.run(inputs)) {
                return result.get(0).getValue();
            }
        } finally {
            inputs.values().forEach(OnnxTensor::close);
        }
    }

    static double[] softmax(float[] logits) {
        double max = Double.NEGATIVE_INFINITY;
        for (float logit : logits) max = Math.max(max, logit);
        double[] probabilities = new double[logits.length];
        double sum = 0;
        for (int i = 0; i < logits.length; i++) {
            probabilities[i] = Math.exp(logits[i] - max);
            sum += probabilities[i];
        }
        for (int i = 0; i < probabilities.length; i++) probabilities[i] /= sum;
        return probabilities;
    }

    /**
     * The index a model's {@code config.json} gives a label, so no class number is hard-coded here.
     */
    static int labelIndex(Path modelDir, String label) throws IOException {
        JsonObject id2label = JsonParser.parseString(Files.readString(modelDir.resolve("config.json"), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("id2label");
        for (String id : id2label.keySet()) {
            if (id2label.get(id).getAsString().equals(label)) return Integer.parseInt(id);
        }
        throw new IOException("No label " + label + " in " + modelDir.resolve("config.json"));
    }
}
