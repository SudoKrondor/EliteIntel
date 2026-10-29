package elite.intel.ai.mouth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoiceEqualizerTest {

    private static final int RATE = 24000;

    @Test
    void aFlatEqLeavesTheVoiceUntouched() {
        byte[] pcm = sine(800, 0.3);
        byte[] before = pcm.clone();

        VoiceEqualizer.apply(pcm, RATE, new int[VoiceEqualizer.BAND_COUNT]);

        assertArrayEquals(before, pcm);
    }

    @Test
    void aPeakingBandRaisesItsOwnFrequencyByItsGain() {
        byte[] pcm = sine(800, 0.1);
        double before = rms(pcm);

        VoiceEqualizer.apply(pcm, RATE, bands(2, 12));

        assertEquals(Math.pow(10, 12 / 20.0), rms(pcm) / before, 0.25);
    }

    @Test
    void aPeakingBandLeavesFrequenciesFarFromItAlone() {
        byte[] pcm = sine(9000, 0.3);
        double before = rms(pcm);

        VoiceEqualizer.apply(pcm, RATE, bands(1, -12));

        assertEquals(1.0, rms(pcm) / before, 0.05);
    }

    @Test
    void theLowShelfLiftsTheBassButNotTheTreble() {
        byte[] bass = sine(40, 0.1);
        byte[] treble = sine(5000, 0.1);
        double bassBefore = rms(bass);
        double trebleBefore = rms(treble);

        VoiceEqualizer.apply(bass, RATE, bands(0, 6));
        VoiceEqualizer.apply(treble, RATE, bands(0, 6));

        assertEquals(Math.pow(10, 6 / 20.0), rms(bass) / bassBefore, 0.15);
        assertEquals(1.0, rms(treble) / trebleBefore, 0.05);
    }

    @Test
    void aBoostPastFullScaleIsClampedRatherThanWrapped() {
        byte[] pcm = sine(800, 0.9);

        VoiceEqualizer.apply(pcm, RATE, bands(2, 12));

        // A wrapped sample would flip negative at the crest and never reach the rail; a clamped one sits on it.
        assertEquals(Short.MAX_VALUE, peak(pcm));
    }

    @Test
    void theWrongNumberOfBandsIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> VoiceEqualizer.apply(new byte[4], RATE, new int[3]));
    }

    private static int[] bands(int band, int gainDb) {
        int[] gains = new int[VoiceEqualizer.BAND_COUNT];
        gains[band] = gainDb;
        return gains;
    }

    /**
     * One second of a sine at {@code amplitude} of full scale.
     */
    private static byte[] sine(double hz, double amplitude) {
        byte[] pcm = new byte[RATE * 2];
        for (int i = 0; i < RATE; i++) {
            int sample = (int) Math.round(Math.sin(2 * Math.PI * hz * i / RATE) * amplitude * Short.MAX_VALUE);
            pcm[2 * i] = (byte) sample;
            pcm[2 * i + 1] = (byte) (sample >>> 8);
        }
        return pcm;
    }

    /**
     * Level over the second half, past the filter's settling.
     */
    private static double rms(byte[] pcm) {
        int samples = pcm.length / 2;
        double sum = 0;
        for (int i = samples / 2; i < samples; i++) {
            double sample = (short) ((pcm[2 * i + 1] << 8) | (pcm[2 * i] & 0xFF));
            sum += sample * sample;
        }
        return Math.sqrt(sum / (samples - samples / 2));
    }

    private static int peak(byte[] pcm) {
        int peak = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            peak = Math.max(peak, (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF)));
        }
        return peak;
    }
}
