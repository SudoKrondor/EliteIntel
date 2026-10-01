package elite.intel.ai.mouth.supertonic;

import java.util.Arrays;

/**
 * Speeds mono speech up without changing its pitch (WSOLA: waveform-similarity overlap-add).
 * <p>
 * Supertonic drops the end of a sentence when it is asked to speak faster than its own pace - the clip keeps
 * its length, but the last word comes out as silence (see {@link SupertonicTTS#generate}). So the model speaks
 * at a pace it renders whole, and this brings the result up to the pace the commander asked for.
 */
final class TimeStretch {

    private static final double FRAME_SECONDS = 0.025;

    private TimeStretch() {
    }

    /**
     * @param samples    mono samples, left untouched
     * @param sampleRate their rate, which sets the frame size
     * @param rate       how much faster: 1.25 plays in 80% of the time
     * @return the sped-up samples; the input itself when {@code rate} is 1 or there is nothing to stretch
     */
    static float[] speedUp(float[] samples, int sampleRate, double rate) {
        if (!(rate > 0) || Double.isInfinite(rate)) throw new IllegalArgumentException("Stretch rate must be positive");
        if (sampleRate <= 0) throw new IllegalArgumentException("Sample rate must be positive");
        if (rate == 1 || samples.length == 0) return samples;

        int hop = Math.max(1, (int) Math.round(sampleRate * FRAME_SECONDS / 2));
        int frame = hop * 2;
        // One hop of search covers a full pitch period of the deepest voice (84 Hz is ~12 ms, the hop 12.5 ms),
        // so a frame can always be lined up with the waveform it continues.
        int tolerance = hop;
        float[] window = hann(frame);
        int outLength = (int) Math.ceil(samples.length / rate);
        float[] out = new float[outLength + frame];

        int previous = 0;
        for (int k = 0; (long) k * hop < outLength; k++) {
            int position = k == 0 ? 0 : bestMatch(samples, (int) Math.round(k * hop * rate), previous + hop, hop, tolerance);
            int outStart = k * hop;
            for (int i = 0; i < frame; i++) {
                // The first frame has nothing to overlap its rising half, which would fade the opening in.
                float gain = k == 0 && i < hop ? 1f : window[i];
                out[outStart + i] += sampleAt(samples, position + i) * gain;
            }
            previous = position;
        }
        return Arrays.copyOf(out, outLength);
    }

    /**
     * The start near {@code nominal} whose opening best continues the waveform at {@code natural}, by
     * normalised cross-correlation over one hop.
     */
    private static int bestMatch(float[] samples, int nominal, int natural, int hop, int tolerance) {
        int best = nominal;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int offset = -tolerance; offset <= tolerance; offset++) {
            int candidate = nominal + offset;
            if (candidate < 0) continue;
            double dot = 0;
            double energy = 0;
            for (int i = 0; i < hop; i++) {
                float value = sampleAt(samples, candidate + i);
                dot += value * sampleAt(samples, natural + i);
                energy += value * value;
            }
            double score = energy > 0 ? dot / Math.sqrt(energy) : 0;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Periodic Hann: at half-frame overlap the windows sum to exactly 1, so the level is unchanged.
     */
    private static float[] hann(int length) {
        float[] window = new float[length];
        for (int i = 0; i < length; i++) {
            window[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / length));
        }
        return window;
    }

    private static float sampleAt(float[] samples, int index) {
        return index >= 0 && index < samples.length ? samples[index] : 0f;
    }
}
