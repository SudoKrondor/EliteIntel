package elite.intel.ai.embed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forces the DJL HuggingFace tokenizer to load its native library <em>offline</em> from the copy already
 * bundled inside the {@code tokenizers} jar, instead of downloading one from {@code publish.djl.ai} on first
 * use. This keeps the embedder a true slide-in: no internet, no model/native hunting — matching how
 * {@link elite.intel.util.SherpaOnnxNatives} handles the sherpa-onnx natives.
 * <p>
 * Why it is needed: DJL appends a CUDA "flavor" (e.g. {@code cu122}) to the native path when it detects an
 * NVIDIA GPU, and the jar only bundles the {@code cpu} flavor — so on a GPU machine (most Elite players) DJL
 * would otherwise download a CUDA-flavored native. We run the tiny embedding model on the CPU regardless, so
 * we pin {@code RUST_FLAVOR=cpu}; the jar already ships {@code native/lib/<os>/cpu/libtokenizers.*} for
 * linux-x86_64, linux-aarch64, win-x86_64 and osx-aarch64. {@code ai.djl.offline=true} is a belt-and-braces
 * guard: if the bundled native is ever missing, DJL fails fast instead of reaching the network.
 * <p>
 * Both are honoured by DJL only if set before its tokenizer loader first runs, so {@link #configure()} is
 * called at the top of {@link OnnxTextEmbedder}'s construction, before any tokenizer is created. Values the
 * operator has already set (env var or {@code -D}) are left untouched, so a power user can still override.
 */
final class DjlTokenizerNatives {

    private static final Logger log = LogManager.getLogger(DjlTokenizerNatives.class);
    private static boolean configured = false;

    private DjlTokenizerNatives() {
    }

    static synchronized void configure() {
        if (configured) {
            return;
        }
        // DJL reads RUST_FLAVOR via getEnvOrSystemProperty (env first, then -D). Only set the system property
        // if neither is present, so an explicit operator override of either form wins.
        if (System.getenv("RUST_FLAVOR") == null && System.getProperty("RUST_FLAVOR") == null) {
            System.setProperty("RUST_FLAVOR", "cpu");
        }
        // DJL offline flag: env DJL_OFFLINE or -Dai.djl.offline. Same "don't clobber an explicit choice" rule.
        if (System.getenv("DJL_OFFLINE") == null && System.getProperty("ai.djl.offline") == null) {
            System.setProperty("ai.djl.offline", "true");
        }
        configured = true;
        log.info("DJL tokenizer natives pinned offline (RUST_FLAVOR={}, ai.djl.offline={})",
                System.getProperty("RUST_FLAVOR"), System.getProperty("ai.djl.offline"));
    }
}
