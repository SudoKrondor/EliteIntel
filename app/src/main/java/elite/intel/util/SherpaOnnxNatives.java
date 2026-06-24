package elite.intel.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
            return new String[]{
                    "onnxruntime_providers_shared.dll",
                    "onnxruntime.dll",
                    "sherpa-onnx-jni.dll"
            };
        }
        return new String[]{
                "libonnxruntime.so",
                "libsherpa-onnx-jni.so"
        };
    }

    private static void extractAndLoad(String platform, Path dir, String lib) throws IOException {
        Path target = dir.resolve(lib);
        if (!Files.exists(target)) {
            String resource = "/native/" + platform + "/" + lib;
            try (InputStream in = SherpaOnnxNatives.class.getResourceAsStream(resource)) {
                if (in == null) throw new IOException("Resource not in JAR: " + resource);
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
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
