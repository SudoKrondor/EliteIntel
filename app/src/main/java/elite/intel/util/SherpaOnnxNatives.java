package elite.intel.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SherpaOnnxNatives {

    private static final Logger log = LogManager.getLogger(SherpaOnnxNatives.class);
    private static boolean loaded = false;

    private SherpaOnnxNatives() {
    }

    public static synchronized void load() throws IOException {
        if (loaded) return;

        String platform = detectPlatform();
        Path nativeDir = AppPaths.getNativeLibDir().resolve("sherpa-onnx");
        Files.createDirectories(nativeDir);

        for (String lib : nativeLibsInOrder(platform)) {
            extractAndLoad(platform, nativeDir, lib);
        }

        System.setProperty("sherpa_onnx.native.path", nativeDir.toAbsolutePath().toString());
        loaded = true;
        log.info("sherpa_onnx.native.path = {}", nativeDir.toAbsolutePath());
    }

    private static String[] nativeLibsInOrder(String platform) {
        if (platform.startsWith("win")) {
            // No onnxruntime_providers_shared.dll: it is the plug-in loader for the GPU execution providers,
            // which ONNX Runtime opens lazily and only when one is requested. Both engines run on "cpu", so
            // Windows ships the same two-library set Linux always has.
            return new String[]{
                    "onnxruntime.dll",
                    "sherpa-onnx-jni.dll"
            };
        }
        return new String[]{
                "libonnxruntime.so",
                "libsherpa-onnx-jni.so"
        };
    }

    /**
     * Puts the library the jar carries on disk and loads it. The copy on disk is replaced whenever its size
     * differs from the jar's, not only when it is missing: the JNI library and the sherpa-onnx classes in
     * the jar are one matched pair, and a jar dropped into an install tree by hand (see the install notes)
     * would otherwise keep loading the previous release's library next to the new classes - an
     * {@code UnsatisfiedLinkError} at the first native call, with nothing in the log to say why.
     */
    private static void extractAndLoad(String platform, Path dir, String lib) throws IOException {
        Path target = dir.resolve(lib);
        String resource = "/native/" + platform + "/" + lib;
        byte[] shipped;
        try (InputStream in = SherpaOnnxNatives.class.getResourceAsStream(resource)) {
            if (in == null) throw new IOException("Resource not in JAR: " + resource);
            shipped = in.readAllBytes();
        }
        if (!Files.exists(target)) {
            Files.write(target, shipped);
        } else if (Files.size(target) != shipped.length) {
            try {
                Files.write(target, shipped);
                log.info("Refreshed {} from the jar ({} bytes)", lib, shipped.length);
            } catch (IOException e) {
                // A read-only install tree: say which library is stale rather than fail here, since the
                // load below may still succeed and the message is what the support bundle needs.
                log.warn("{} on disk is {} bytes but the jar carries {}; could not refresh it: {}",
                        lib, Files.size(target), shipped.length, e.getMessage());
            }
        }
        System.load(target.toAbsolutePath().toString());
        log.info("Loaded {}", lib);
    }

    private static String detectPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "win-x86-64";
        if (os.contains("linux")) return "linux-x86-64";
        if (os.contains("mac")) return "osx-x86-64";
        throw new UnsupportedOperationException("Unsupported OS: " + os);
    }
}
