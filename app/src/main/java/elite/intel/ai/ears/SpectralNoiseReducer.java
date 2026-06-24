package elite.intel.ai.ears;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * FFT-based spectral subtraction noise reducer for 16 kHz PCM-16 mono audio.
 * <p>
 * Noise spectrum is estimated in-flight from silence frames fed via
 * {@link #accumulateNoise(byte[], int)} during the VAD-inactive phase of the
 * capture loop. Once enough frames have accumulated, {@link #denoise(byte[], int)}
 * applies overlap-add spectral subtraction before Parakeet inference.
 * <p>
 * Thread-safety: {@code accumulateNoise} is called from the capture thread;
 * {@code denoise} is called from the transcription thread. The noise-spectrum
 * reference is volatile, so a stale but valid spectrum is always observed.
 */
public class SpectralNoiseReducer {

    private static final Logger log = LogManager.getLogger(SpectralNoiseReducer.class);

    private static final int FFT_SIZE = 512;    // ~32 ms at 16 kHz
    private static final int HOP_SIZE = FFT_SIZE / 2;
    private static final int BINS = FFT_SIZE / 2 + 1; // DC … Nyquist

    /**
     * Silence frames required before the first noise estimate is committed (~2 s).
     */
    private static final int WARMUP_FRAMES = 20;
    /**
     * EMA coefficient for the existing estimate once warmed up (0 = full replacement).
     */
    private static final float EMA_WEIGHT_OLD = 0.90f;
    /**
     * Update the running EMA every this many silence frames post-warmup.
     */
    private static final int EMA_UPDATE_INTERVAL = 10;

    // Over-subtraction factor α and spectral floor β for Low / Medium / High.
    private static final float[] ALPHA = {1.5f, 2.0f, 3.0f};
    private static final float[] BETA = {0.10f, 0.05f, 0.02f};

    private static final SpectralNoiseReducer INSTANCE = new SpectralNoiseReducer();

    private final float[] window = new float[FFT_SIZE];

    // Accumulation state — only accessed from the capture thread (accumulateNoise is synchronized).
    private final float[] accumPower = new float[BINS];
    private int accumulatedFrames = 0;

    /**
     * Committed noise power spectrum, visible to the transcription thread.
     */
    private volatile float[] noiseSpectrum = null;

    private SpectralNoiseReducer() {
        for (int i = 0; i < FFT_SIZE; i++) {
            window[i] = (float) (0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / (FFT_SIZE - 1))));
        }
    }

    public static SpectralNoiseReducer getInstance() {
        return INSTANCE;
    }

    /**
     * Resets the noise estimate. Call when STT restarts (device change, service restart)
     * so the reducer re-learns the new environment's noise floor.
     */
    public synchronized void reset() {
        java.util.Arrays.fill(accumPower, 0f);
        accumulatedFrames = 0;
        noiseSpectrum = null;
        log.debug("Spectral noise estimate reset");
    }

    /**
     * Feed one VAD-inactive frame (16 kHz, PCM-16 mono) to update the noise estimate.
     * Only the first {@code len} bytes of {@code pcm16} are used.
     */
    public synchronized void accumulateNoise(byte[] pcm16, int len) {
        if (len < FFT_SIZE * 2) return;

        // Analyse one FFT frame from the start of the buffer.
        float[] frame = new float[FFT_SIZE * 2];
        for (int i = 0; i < FFT_SIZE; i++) {
            short s = (short) ((pcm16[i * 2 + 1] << 8) | (pcm16[i * 2] & 0xFF));
            frame[2 * i] = (s / 32768.0f) * window[i];
            frame[2 * i + 1] = 0f;
        }
        fft(frame, false);

        float[] framePower = new float[BINS];
        for (int k = 0; k < BINS; k++) {
            float re = frame[2 * k], im = frame[2 * k + 1];
            framePower[k] = re * re + im * im;
            accumPower[k] += framePower[k];
        }
        accumulatedFrames++;

        if (accumulatedFrames == WARMUP_FRAMES) {
            // Commit the initial mean-power estimate.
            float[] spectrum = new float[BINS];
            for (int k = 0; k < BINS; k++) spectrum[k] = accumPower[k] / WARMUP_FRAMES;
            noiseSpectrum = spectrum;
            log.debug("Noise spectrum ready after {} silence frames", WARMUP_FRAMES);

        } else if (accumulatedFrames > WARMUP_FRAMES
                && (accumulatedFrames % EMA_UPDATE_INTERVAL == 0)) {
            // Exponential moving average: blend old estimate with the latest frame.
            float[] old = noiseSpectrum;
            if (old != null) {
                float[] updated = new float[BINS];
                for (int k = 0; k < BINS; k++) {
                    updated[k] = EMA_WEIGHT_OLD * old[k] + (1f - EMA_WEIGHT_OLD) * framePower[k];
                }
                noiseSpectrum = updated;
            }
        }
    }

    /**
     * Applies spectral subtraction to a 16 kHz PCM-16 mono utterance.
     * Returns the original array unchanged if no noise spectrum has been estimated yet
     * or if the input is too short to process.
     *
     * @param pcm16Mono   PCM-16 LE mono bytes at 16 kHz
     * @param strengthIdx 0 = Low, 1 = Medium, 2 = High
     */
    public byte[] denoise(byte[] pcm16Mono, int strengthIdx) {
        float[] ns = noiseSpectrum;
        if (ns == null) return pcm16Mono;

        int si = Math.max(0, Math.min(2, strengthIdx));
        float alpha = ALPHA[si];
        float beta = BETA[si];

        int numSamples = pcm16Mono.length / 2;
        if (numSamples < FFT_SIZE) return pcm16Mono;

        float[] samples = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            short s = (short) ((pcm16Mono[i * 2 + 1] << 8) | (pcm16Mono[i * 2] & 0xFF));
            samples[i] = s / 32768.0f;
        }

        float[] output = new float[numSamples + FFT_SIZE];
        float[] norm = new float[numSamples + FFT_SIZE];

        for (int start = 0; start + FFT_SIZE <= numSamples; start += HOP_SIZE) {
            float[] frame = new float[FFT_SIZE * 2];
            for (int i = 0; i < FFT_SIZE; i++) {
                frame[2 * i] = samples[start + i] * window[i];
                frame[2 * i + 1] = 0f;
            }

            fft(frame, false);

            // Spectral subtraction on the positive-frequency half (DC … Nyquist).
            for (int k = 0; k < BINS; k++) {
                float re = frame[2 * k], im = frame[2 * k + 1];
                float power = re * re + im * im;
                float noisePow = ns[k];
                float newPow = Math.max(power - alpha * noisePow, beta * noisePow);
                float scale = (power > 1e-10f) ? (float) Math.sqrt(newPow / power) : 0f;
                frame[2 * k] = re * scale;
                frame[2 * k + 1] = im * scale;
            }
            // Restore conjugate symmetry so the IFFT produces a real signal.
            for (int k = 1; k < FFT_SIZE / 2; k++) {
                frame[2 * (FFT_SIZE - k)] = frame[2 * k];
                frame[2 * (FFT_SIZE - k) + 1] = -frame[2 * k + 1];
            }

            fft(frame, true);

            // Weighted overlap-add (synthesis Hanning window).
            for (int i = 0; i < FFT_SIZE; i++) {
                float w = window[i];
                output[start + i] += frame[2 * i] * w;
                norm[start + i] += w * w;
            }
        }

        byte[] result = new byte[pcm16Mono.length];
        for (int i = 0; i < numSamples; i++) {
            float v = (norm[i] > 1e-6f) ? output[i] / norm[i] : 0f;
            int s = Math.round(v * 32768.0f);
            s = Math.max(-32768, Math.min(32767, s));
            result[2 * i] = (byte) (s & 0xFF);
            result[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return result;
    }

    /**
     * In-place Cooley-Tukey FFT. {@code data} is interleaved real/imag, length = 2 * power-of-2.
     */
    private static void fft(float[] data, boolean inverse) {
        int n = data.length / 2;

        // Bit-reversal permutation.
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                float t;
                t = data[2 * i];
                data[2 * i] = data[2 * j];
                data[2 * j] = t;
                t = data[2 * i + 1];
                data[2 * i + 1] = data[2 * j + 1];
                data[2 * j + 1] = t;
            }
        }

        // Decimation-in-time butterfly stages.
        for (int len = 2; len <= n; len <<= 1) {
            double angle = 2.0 * Math.PI / len * (inverse ? 1 : -1);
            float wRe = (float) Math.cos(angle);
            float wIm = (float) Math.sin(angle);
            for (int i = 0; i < n; i += len) {
                float curRe = 1f, curIm = 0f;
                for (int j = 0; j < len / 2; j++) {
                    int u = i + j, v = i + j + len / 2;
                    float uRe = data[2 * u], uIm = data[2 * u + 1];
                    float vRe = data[2 * v] * curRe - data[2 * v + 1] * curIm;
                    float vIm = data[2 * v] * curIm + data[2 * v + 1] * curRe;
                    data[2 * u] = uRe + vRe;
                    data[2 * u + 1] = uIm + vIm;
                    data[2 * v] = uRe - vRe;
                    data[2 * v + 1] = uIm - vIm;
                    float nr = curRe * wRe - curIm * wIm;
                    curIm = curRe * wIm + curIm * wRe;
                    curRe = nr;
                }
            }
        }

        if (inverse) {
            for (int i = 0; i < data.length; i++) data[i] /= n;
        }
    }
}
