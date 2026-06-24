package elite.intel.ai.ears;


import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.Optional;


public class AudioFormatDetector {

    private static final Logger log = LogManager.getLogger(AudioFormatDetector.class);

    private static final int[] POSSIBLE_RATES = {48000, 44100, 96000, 192000};

    // Tried in order of preference per rate. {sampleSizeBits, channels}
    // 16-bit mono is ideal (no conversion). Others are transcoded to 16-bit mono after capture.
    private static final int[][] CANDIDATE_FORMATS = {
            {16, 1},  // ideal: no conversion needed
            {24, 2},  // e.g. Kraken Chat: natively 24-bit stereo only
            {16, 2},  // stereo headphone: downmix to mono
            {24, 1},  // 24-bit mono
    };

    private static final double BUFFER_DURATION_SECONDS = 0.1;

    public static Format detectSupportedFormat() {
        return detectSupportedFormat(null);
    }

    public static Format detectSupportedFormat(Mixer.Info mixerInfo) {
        AudioFormatDetector detector = new AudioFormatDetector();
        try {
            return detector.detectSupportedFormatInternal(mixerInfo)
                    .orElseThrow(() -> new AudioFormatException("No supported audio format found."));
        } catch (AudioFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Format> detectSupportedFormatInternal(Mixer.Info mixerInfo) throws AudioFormatException {
        for (int rate : POSSIBLE_RATES) {
            for (int[] fmt : CANDIDATE_FORMATS) {
                int bits = fmt[0];
                int channels = fmt[1];
                try {
                    AudioFormat audioFormat = new AudioFormat(rate, bits, channels, true, false);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
                    boolean supported;
                    if (mixerInfo == null) {
                        supported = AudioSystem.isLineSupported(info);
                    } else {
                        try {
                            supported = AudioSystem.getMixer(mixerInfo).isLineSupported(info);
                        } catch (Exception ex) {
                            log.warn("Could not query mixer '{}': {}", mixerInfo.getName(), ex.getMessage());
                            supported = false;
                        }
                    }
                    if (supported) {
                        int bufferSize = calculateBufferSize(audioFormat);
                        if (bufferSize <= 0) {
                            log.warn("Invalid buffer size for {} Hz {}-bit {} ch: {}", rate, bits, channels, bufferSize);
                            continue;
                        }
                        log.info("Detected supported capture format: {} Hz, {}-bit, {} ch, buffer {} bytes",
                                rate, bits, channels, bufferSize);
                        return Optional.of(new Format(rate, bufferSize, audioFormat));
                    }
                } catch (Exception e) {
                    log.error("Error checking support for {} Hz {}-bit {} ch: {}", rate, bits, channels, e.getMessage(), e);
                    throw new AudioFormatException("Failed to detect audio format due to system error: " + e.getMessage());
                }
            }
        }
        log.warn("No supported audio format found for any probed format combination.");
        GameEventBus.publish(new AiVoxResponseEvent("No supported audio format found for mono 16-bit input."));
        return Optional.empty();
    }

    /**
     * Converts captured PCM bytes to 16-bit signed little-endian mono.
     * Only {@code inputLen} bytes of {@code input} are read.
     * If the source is already 16-bit mono, returns {@code input} unchanged (no allocation)
     * when {@code inputLen == input.length}, or a trimmed copy otherwise.
     */
    public static byte[] toPCM16Mono(byte[] input, int inputLen, AudioFormat captureFormat) {
        int bitsPerSample = captureFormat.getSampleSizeInBits();
        int channels = captureFormat.getChannels();
        if (bitsPerSample == 16 && channels == 1) {
            return inputLen == input.length ? input : Arrays.copyOf(input, inputLen);
        }

        boolean bigEndian = captureFormat.isBigEndian();
        int bytesPerSample = bitsPerSample / 8;
        int frameSize = captureFormat.getFrameSize(); // bytesPerSample * channels
        int frames = inputLen / frameSize;
        byte[] output = new byte[frames * 2];
        int shift = bitsPerSample - 16;

        for (int i = 0; i < frames; i++) {
            long sum = 0;
            for (int c = 0; c < channels; c++) {
                int base = i * frameSize + c * bytesPerSample;
                sum += readSignedSample(input, base, bytesPerSample, bigEndian);
            }
            short mono16 = (short) ((sum / channels) >> shift);
            output[i * 2] = (byte) (mono16 & 0xFF);
            output[i * 2 + 1] = (byte) ((mono16 >> 8) & 0xFF);
        }
        return output;
    }

    public static byte[] toPCM16Mono(byte[] input, AudioFormat captureFormat) {
        return toPCM16Mono(input, input.length, captureFormat);
    }

    // Reads a signed PCM sample of bytesPerSample bytes, MSB first (for sign extension).
    private static long readSignedSample(byte[] data, int offset, int bytesPerSample, boolean bigEndian) {
        long val;
        if (bigEndian) {
            val = data[offset]; // sign-extend MSB into long
            for (int b = 1; b < bytesPerSample; b++) val = (val << 8) | (data[offset + b] & 0xFF);
        } else {
            val = data[offset + bytesPerSample - 1]; // MSB is last byte in LE  sign-extend into long
            for (int b = bytesPerSample - 2; b >= 0; b--) val = (val << 8) | (data[offset + b] & 0xFF);
        }
        return val;
    }

    private int calculateBufferSize(AudioFormat format) {
        return (int) (format.getSampleRate() * BUFFER_DURATION_SECONDS * format.getFrameSize());
    }

    public record Format(int sampleRate, int bufferSize, AudioFormat captureFormat) {
        public int getSampleRate() {
            return sampleRate;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public AudioFormat getCaptureFormat() {
            return captureFormat;
        }
    }
}