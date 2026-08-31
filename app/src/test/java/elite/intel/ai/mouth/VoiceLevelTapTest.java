package elite.intel.ai.mouth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The side-chain detector. It answers one question - how loud is the companion speaking right now - and
 * the answer has to be right in dBFS, because the duck's threshold is expressed in the same units.
 *
 * <p>The measurement is taken after the speech-volume control has been applied to the PCM, which is what
 * makes a commander who runs the companion silent and reads it off the overlay get no ducking at all.
 */
class VoiceLevelTapTest {

    private static final AudioFormat MONO_24K = new AudioFormat(24_000f, 16, 1, true, false);
    private static final AudioFormat STEREO_44K = new AudioFormat(44_100f, 16, 2, true, false);
    private static final AudioFormat BIG_ENDIAN_MONO = new AudioFormat(24_000f, 16, 1, true, true);

    @BeforeEach
    void silence() {
        VoiceLevelTap.reset();
    }

    @Test
    void nothingSpokenYetReadsAsSilence() {
        assertEquals(VoiceLevelTap.SILENCE_DBFS, VoiceLevelTap.currentLevelDbfs());
    }

    @Test
    void aFullScaleToneReadsAtRoughlyZeroDbfs() {
        VoiceLevelTap.observe(squareWave(2400, 32767), 0, 4800, MONO_24K);

        assertEquals(0.0, VoiceLevelTap.currentLevelDbfs(), 0.1,
                "a signal at full scale is the 0 dBFS the threshold is measured against");
    }

    @Test
    void halvingTheAmplitudeDropsTheLevelBySixDecibels() {
        VoiceLevelTap.observe(squareWave(2400, 16384), 0, 4800, MONO_24K);

        assertEquals(-6.0, VoiceLevelTap.currentLevelDbfs(), 0.1,
                "the dB scale has to be right or the threshold and ratio mean nothing");
    }

    @Test
    void aCommanderWhoSilencesTheVoiceProducesNoSignalToDuckAgainst() {
        VoiceLevelTap.observe(new byte[4800], 0, 4800, MONO_24K);

        assertEquals(VoiceLevelTap.SILENCE_DBFS, VoiceLevelTap.currentLevelDbfs(),
                "speech volume zero yields silent PCM here, so nothing ducks and no special case is needed");
    }

    @Test
    void turningTheVoiceDownIsVisibleAsALowerLevel() {
        VoiceLevelTap.observe(squareWave(2400, 32767), 0, 4800, MONO_24K);
        double atFullVolume = VoiceLevelTap.currentLevelDbfs();

        // What AudioDeClicker.applyVolume does to the samples at 30% before they ever reach the line.
        VoiceLevelTap.observe(squareWave(2400, (int) (32767 * 0.3)), 0, 4800, MONO_24K);
        double atLowVolume = VoiceLevelTap.currentLevelDbfs();

        assertTrue(atLowVolume < atFullVolume - 9,
                "measuring after the volume control is what makes the duck proportional to it");
    }

    @Test
    void aLevelGoesStaleSoTheEndOfSpeechNeedsNoEvent() throws InterruptedException {
        VoiceLevelTap.observe(squareWave(2400, 32767), 0, 4800, MONO_24K);
        assertEquals(0.0, VoiceLevelTap.currentLevelDbfs(), 0.1);

        Thread.sleep(VoiceLevelTap.STALE_AFTER_MS + 150);

        assertEquals(VoiceLevelTap.SILENCE_DBFS, VoiceLevelTap.currentLevelDbfs(),
                "engines simply stop calling when speech ends - the reading has to decay on its own");
    }

    @Test
    void stereoAudioIsMeasuredAcrossBothChannels() {
        VoiceLevelTap.observe(squareWave(2000, 16384), 0, 4000, STEREO_44K);

        assertEquals(-6.0, VoiceLevelTap.currentLevelDbfs(), 0.1);
    }

    @Test
    void bigEndianSamplesAreNotReadAsNoise() {
        byte[] pcm = new byte[480];
        for (int i = 0; i < pcm.length; i += 2) {
            pcm[i] = (byte) (16384 >>> 8);
            pcm[i + 1] = (byte) (16384 & 0xFF);
        }

        VoiceLevelTap.observe(pcm, 0, pcm.length, BIG_ENDIAN_MONO);

        assertEquals(-6.0, VoiceLevelTap.currentLevelDbfs(), 0.1,
                "byte order comes from the line's format, not an assumption");
    }

    @Test
    void onlyTheChunkBeingWrittenIsMeasured() {
        byte[] pcm = new byte[4800];
        // Loud in the first half, silent in the second - only the silent half is being written.
        byte[] loud = squareWave(1200, 32767);
        System.arraycopy(loud, 0, pcm, 0, loud.length);

        VoiceLevelTap.observe(pcm, 2400, 2400, MONO_24K);

        assertEquals(VoiceLevelTap.SILENCE_DBFS, VoiceLevelTap.currentLevelDbfs(),
                "an engine writes a slice of its buffer, and the level must describe that slice");
    }

    @Test
    void malformedCallsAreIgnoredRatherThanCorruptingTheReading() {
        VoiceLevelTap.observe(squareWave(2400, 32767), 0, 4800, MONO_24K);
        double good = VoiceLevelTap.currentLevelDbfs();

        VoiceLevelTap.observe(null, 0, 100, MONO_24K);
        VoiceLevelTap.observe(new byte[10], 0, 0, MONO_24K);
        VoiceLevelTap.observe(new byte[10], 8, 100, MONO_24K);
        VoiceLevelTap.observe(new byte[10], 0, 10, new AudioFormat(24_000f, 8, 1, true, false));

        assertEquals(good, VoiceLevelTap.currentLevelDbfs(),
                "a bad call must not be able to jam the duck on or off");
    }

    /**
     * {@code samples} frames of a constant-amplitude signal, so RMS and peak coincide and are exact.
     */
    private static byte[] squareWave(int samples, int amplitude) {
        byte[] pcm = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            pcm[i * 2] = (byte) (amplitude & 0xFF);
            pcm[i * 2 + 1] = (byte) ((amplitude >>> 8) & 0xFF);
        }
        return pcm;
    }
}
