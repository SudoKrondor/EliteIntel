package elite.intel.ai.ears;

import elite.intel.util.WavHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class DumpAudioForTesting {

    private static final Logger log = LogManager.getLogger(DumpAudioForTesting.class);

    private static final DumpAudioForTesting INSTANCE;

    static {
        try {
            INSTANCE = new DumpAudioForTesting();
        } catch (Exception e) {
            throw new RuntimeException("Singleton instance failed to initialize", e);
        }
    }

    private DumpAudioForTesting() {
    }

    private static final int MAX_FAILED_CAPTURE_DUMPS = 20;
    private final AtomicInteger failedCaptureDumps = new AtomicInteger();

    public static DumpAudioForTesting getInstance() {
        return INSTANCE;
    }

    /**
     * Writes the audio a capture failed on next to the log, so the commander can hear what the recogniser
     * was actually given. A transcript that comes back empty says nothing about why: speech the decoder
     * could not read, a room the microphone never picked up, and a button click in an otherwise silent
     * buffer are indistinguishable in a log line and obvious in two seconds of listening.
     * <p>
     * Capped per session: a systematic failure must not quietly fill the disk with the evidence of itself.
     */
    public void dumpFailedCapture(byte[] audio, int sampleRateHertz, String reason) {
        if (failedCaptureDumps.incrementAndGet() > MAX_FAILED_CAPTURE_DUMPS) {
            return;
        }
        Path file = Path.of("logs", "stt-" + reason + "_" + System.currentTimeMillis() + ".wav");
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                out.write(new WavHeader(sampleRateHertz, (short) 16, audio.length).toByteArray());
                out.write(audio);
            }
            log.info("Wrote the failed capture to {} ({}ms of audio)",
                    file, audio.length * 1000 / (sampleRateHertz * 2));
        } catch (IOException e) {
            log.warn("Could not write the failed capture to {}: {}", file, e.getMessage());
        }
    }

    public void dumpAudioAsWav(byte[] audio, int sampleRateHertz) {
        if (true) return; // dbug
        String filename = "MicOutputForTesting_" + System.currentTimeMillis() + ".wav";
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            WavHeader header = new WavHeader(sampleRateHertz, (short) 16, audio.length);
            fos.write(header.toByteArray());
            fos.write(audio);
            log.info("Dumped {} bytes of audio to {}", audio.length, filename);
        } catch (IOException e) {
            log.error("Failed to dump audio: ", e);
        }
    }
}