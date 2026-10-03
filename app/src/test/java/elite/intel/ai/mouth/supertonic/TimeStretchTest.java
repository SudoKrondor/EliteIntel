package elite.intel.ai.mouth.supertonic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeStretchTest {

    private static final int RATE = 44_100;

    @Test
    void shortensByTheRateAndKeepsThePitch() {
        float[] tone = sine(220, RATE, 0, RATE);
        float[] faster = TimeStretch.speedUp(tone, RATE, 1.35);

        assertEquals(Math.ceil(RATE / 1.35), faster.length, 1);
        int from = faster.length / 4;
        int to = faster.length * 3 / 4;
        double seconds = (to - from) / (double) RATE;
        assertEquals(220, zeroCrossings(faster, from, to) / 2.0 / seconds, 5);
    }

    @Test
    void keepsTheLevelOfASteadyTone() {
        float[] tone = sine(220, RATE, 0, RATE);
        float[] faster = TimeStretch.speedUp(tone, RATE, 1.2);
        assertEquals(peak(tone, RATE / 4, RATE / 2), peak(faster, RATE / 4, RATE / 2), 0.05);
    }

    @Test
    void theLastSoundSurvivesTheStretch() {
        // Silence, then a short burst at the very end - the shape of a sentence's final word.
        float[] clip = new float[RATE];
        float[] burst = sine(300, RATE, 0, RATE / 10);
        System.arraycopy(burst, 0, clip, clip.length - burst.length - RATE / 50, burst.length);

        float[] faster = TimeStretch.speedUp(clip, RATE, 1.3);

        int tail = faster.length - faster.length / 6;
        assertTrue(peak(faster, tail, faster.length) > 0.8, "the closing burst is still there");
    }

    @Test
    void theOpeningIsNotFadedIn() {
        float[] dc = new float[RATE / 10];
        java.util.Arrays.fill(dc, 0.5f);
        float[] faster = TimeStretch.speedUp(dc, RATE, 1.2);
        assertEquals(0.5f, faster[0], 1e-6);
    }

    @Test
    void rateOneAndEmptyInputComeBackUntouched() {
        float[] clip = sine(220, RATE, 0, 1000);
        assertSame(clip, TimeStretch.speedUp(clip, RATE, 1));
        float[] empty = new float[0];
        assertSame(empty, TimeStretch.speedUp(empty, RATE, 1.5));
    }

    @Test
    void rejectsANonsenseRate() {
        float[] clip = new float[10];
        assertThrows(IllegalArgumentException.class, () -> TimeStretch.speedUp(clip, RATE, 0));
        assertThrows(IllegalArgumentException.class, () -> TimeStretch.speedUp(clip, RATE, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> TimeStretch.speedUp(clip, 0, 1.2));
    }

    private static float[] sine(double hz, int rate, int from, int length) {
        float[] samples = new float[length];
        for (int i = 0; i < length; i++) {
            samples[i] = (float) Math.sin(2 * Math.PI * hz * (from + i) / rate);
        }
        return samples;
    }

    private static int zeroCrossings(float[] samples, int from, int to) {
        int crossings = 0;
        for (int i = from + 1; i < to; i++) {
            if ((samples[i - 1] < 0) != (samples[i] < 0)) crossings++;
        }
        return crossings;
    }

    private static double peak(float[] samples, int from, int to) {
        double peak = 0;
        for (int i = from; i < to; i++) peak = Math.max(peak, Math.abs(samples[i]));
        return peak;
    }
}
