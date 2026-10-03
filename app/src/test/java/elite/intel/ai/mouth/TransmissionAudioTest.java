package elite.intel.ai.mouth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransmissionAudioTest {
    @Test
    void radioProcessingUsesEnhancedOrBaselineFilter() {
        byte[] original = sine(650);
        byte[] enhanced = original.clone();
        TransmissionAudio.processVoice(enhanced, true, new TransmissionAudio.Options(false, true));
        assertFalse(java.util.Arrays.equals(original, enhanced));

        byte[] legacy = original.clone();
        TransmissionAudio.processVoice(legacy, true, TransmissionAudio.Options.NONE);
        assertFalse(java.util.Arrays.equals(original, legacy));
        assertFalse(java.util.Arrays.equals(enhanced, legacy));

        byte[] ordinary = original.clone();
        TransmissionAudio.processVoice(ordinary, false, TransmissionAudio.Options.NONE);
        assertArrayEquals(original, ordinary);

        byte[] vega = original.clone();
        TransmissionAudio.processVoice(vega, false, new TransmissionAudio.Options(false, true));
        assertArrayEquals(enhanced, vega);
    }

    @Test
    void tonesFrameOnlyTheWholeMessageAndLeaveSpeechUntouched() {
        byte[] first = {1, 2, 3, 4};
        byte[] last = {5, 6, 7, 8};
        byte[] opening = TransmissionAudio.frame(first, true, false, 1);
        byte[] closing = TransmissionAudio.frame(last, false, true, 1);

        assertEquals(2 * (6000 + 840) + first.length, opening.length);
        assertEquals(last.length + 2 * (840 + 6000), closing.length);
        assertArrayEquals(first, java.util.Arrays.copyOfRange(opening, opening.length - first.length, opening.length));
        assertArrayEquals(last, java.util.Arrays.copyOfRange(closing, 0, last.length));
        assertSame(first, TransmissionAudio.frame(first, false, false, 1));
        assertArrayEquals(new byte[opening.length], TransmissionAudio.frame(new byte[first.length], true, false, 0));
    }

    @Test
    void withTonesLeavesTheSentenceAloneWhileTheBeepIsOff() {
        byte[] voice = {1, 2, 3, 4};
        assertSame(voice, TransmissionAudio.withTones(voice, new TransmissionAudio.Options(false, true), true, true, 1));
        assertEquals(voice.length + 2 * 2 * (6000 + 840),
                TransmissionAudio.withTones(voice, new TransmissionAudio.Options(true, false), true, true, 1).length);
    }

    @Test
    void closingToneHasDistinctFrequencyAndFadedEnds() {
        byte[] first = TransmissionAudio.frame(new byte[0], true, false, 1);
        byte[] last = TransmissionAudio.frame(new byte[0], false, true, 1);
        assertEquals(0, sample(first, 0));
        assertEquals(0, sample(last, last.length / 2 - 1));
        assertTrue(zeroCrossings(first, 0, 6000) > zeroCrossings(last, 840, 6840));
        assertTrue(maxAbs(first) < 6000, "the tones must not overpower speech");
    }

    @Test
    void degradationControlsPeaksWithoutAClippingPlateau() {
        byte[] pcm = new byte[4800];
        for (int i = 0; i < pcm.length / 2; i++) {
            int value = (int) (28000 * Math.sin(2 * Math.PI * 800 * i / 24000));
            pcm[i * 2] = (byte) value;
            pcm[i * 2 + 1] = (byte) (value >>> 8);
        }
        TransmissionAudio.degrade(pcm);
        assertTrue(maxAbs(pcm) < 32767);
        assertTrue(maxAbs(pcm) > 10000, "the treatment should preserve intelligible signal level");
    }

    @Test
    void strongerEffectMovesTheBandUpWithoutGeneratingStatic() {
        byte[] bass = sine(120);
        byte[] lowVoice = sine(650);
        byte[] consonants = sine(4000);
        TransmissionAudio.degrade(bass);
        TransmissionAudio.degrade(lowVoice);
        TransmissionAudio.degrade(consonants);
        assertTrue(rms(consonants) > rms(bass) * 5,
                "the passed voice band should sit well above bass frequencies");
        assertTrue(rms(consonants) > rms(lowVoice) * 1.3,
                "presence should be favoured over the low end of speech");
        assertTrue(rms(lowVoice) > 5500,
                "the lower voices should retain enough output after filtering");

        byte[] silence = new byte[24000];
        TransmissionAudio.degrade(silence);
        assertArrayEquals(new byte[silence.length], silence, "the effect must add no noise of its own");
        RadioFilter.apply(silence);
        assertArrayEquals(new byte[silence.length], silence, "the legacy radio path must also stay quiet");
    }

    @Test
    void preFilterSaturationGivesLowVoicesUpperHarmonics() {
        byte[] lowVoice = sine(650);
        TransmissionAudio.degrade(lowVoice);

        double fundamental = toneAmplitude(lowVoice, 650);
        double third = toneAmplitude(lowVoice, 1950);
        assertTrue(third > fundamental * 0.10,
                "harmonics from a low voice should survive inside the passed band");
        assertTrue(third < fundamental * 0.40,
                "pre-filter saturation should stay moderate");
    }

    private static byte[] sine(int frequency) {
        byte[] pcm = new byte[48000];
        for (int i = 0; i < pcm.length / 2; i++) {
            int value = (int) (16000 * Math.sin(2 * Math.PI * frequency * i / 24000));
            pcm[i * 2] = (byte) value;
            pcm[i * 2 + 1] = (byte) (value >>> 8);
        }
        return pcm;
    }

    private static double rms(byte[] pcm) {
        double energy = 0;
        for (int i = 0; i < pcm.length / 2; i++) energy += (double) sample(pcm, i) * sample(pcm, i);
        return Math.sqrt(energy / (pcm.length / 2));
    }

    private static double toneAmplitude(byte[] pcm, int frequency) {
        int start = pcm.length / 4; // Skip the filter and compressor's initial settling period.
        int end = pcm.length / 2;
        double cosine = 0;
        double sine = 0;
        for (int i = start; i < end; i++) {
            double phase = 2 * Math.PI * frequency * i / 24000;
            cosine += sample(pcm, i) * Math.cos(phase);
            sine += sample(pcm, i) * Math.sin(phase);
        }
        return Math.hypot(cosine, sine) / (end - start);
    }

    private static int sample(byte[] pcm, int index) {
        return (short) ((pcm[index * 2 + 1] << 8) | (pcm[index * 2] & 0xff));
    }

    private static int maxAbs(byte[] pcm) {
        int max = 0;
        for (int i = 0; i < pcm.length / 2; i++) max = Math.max(max, Math.abs(sample(pcm, i)));
        return max;
    }

    private static int zeroCrossings(byte[] pcm, int begin, int end) {
        int count = 0;
        for (int i = begin + 1; i < end; i++) {
            if (sample(pcm, i - 1) < 0 && sample(pcm, i) >= 0) count++;
        }
        return count;
    }
}
