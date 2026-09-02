package elite.intel.jukebox;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decoding real FLAC files, for the same reason the MP3 tests use real files: the format is where the
 * surprises are, and a decoder tested only against its own output would never meet them.
 *
 * <p>The two that matter here are new with lossless audio and could not arise from an MP3 library. A
 * 96 kHz file plays back more than twice as fast as the output line unless it is resampled, and a 24-bit
 * file's samples do not fit the 16-bit pipeline at all - read without scaling, the top bits are lost and
 * a loud tone decodes to noise or near-silence. Both fixtures exist to catch exactly that.
 *
 * <p>The fixtures are half-second 440 Hz tones, small enough to live in the repository and loud enough
 * that "did it actually decode audio" is answerable rather than a matter of the file merely opening.
 */
class FlacAudioSourceTest {

    @Test
    void aStereoFileAtTheOutputRateDecodesToAudibleAudio() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.flac", 0)) {
            byte[] audio = readAll(source);

            assertTrue(audio.length > 0, "the fixture is not silent, so neither should the decode be");
            assertEquals(0, audio.length % MusicFormat.FRAME_BYTES,
                    "output has to be whole stereo frames or the line will drift a byte and swap channels");
            assertTrue(rootMeanSquare(audio) > 1000,
                    "a 440 Hz tone should decode loud - near silence means the decode produced nothing");
        }
    }

    @Test
    void aTwentyFourBitFileIsScaledRatherThanTruncatedToNoise() throws Exception {
        double sixteenBit;
        try (AudioSource source = open("tone-44100-stereo.flac", 0)) {
            sixteenBit = rootMeanSquare(readAll(source));
        }

        try (AudioSource source = open("tone-96000-stereo-24bit.flac", 0)) {
            double twentyFourBit = rootMeanSquare(readAll(source));

            // The same tone at the same amplitude, so the level has to survive the depth conversion.
            // Taking the wrong bytes of a 24-bit sample does not merely quieten it, it decodes as noise
            // at a wildly different level - a generous band still separates "scaled" from "mangled".
            assertEquals(sixteenBit, twentyFourBit, sixteenBit * 0.25,
                    "a 24-bit tone should play at the same level as the 16-bit one, not louder or quieter");
        }
    }

    @Test
    void aRateAboveTheOutputLineIsConvertedRatherThanPlayedFast() throws Exception {
        try (AudioSource source = open("tone-96000-stereo-24bit.flac", 0)) {
            byte[] audio = readAll(source);

            // Handed to a 44.1 kHz line unresampled, half a second of 96 kHz audio would last a little
            // over a second. The fixture is exactly 0.5 s, and resampling has to preserve that.
            double seconds = secondsOf(audio);
            assertEquals(0.5, seconds, 0.02,
                    "96 kHz must be resampled down, but this played back over " + seconds + "s");
        }
    }

    @Test
    void aMonoFileAtADifferentRateIsConvertedToTheOutputFormat() throws Exception {
        try (AudioSource source = open("tone-32000-mono.flac", 0)) {
            byte[] audio = readAll(source);

            assertTrue(rootMeanSquare(audio) > 1000, "the tone survives the rate conversion");
            assertEquals(0.5, secondsOf(audio), 0.02, "resampling must preserve duration");
        }
    }

    @Test
    void bothChannelsCarryAudioWhenTheSourceIsMono() throws Exception {
        try (AudioSource source = open("tone-32000-mono.flac", 0)) {
            byte[] audio = readAll(source);

            assertTrue(channelRms(audio, 0) > 1000, "left channel is silent");
            assertTrue(channelRms(audio, 1) > 1000, "right channel is silent - mono was not duplicated");
        }
    }

    @Test
    void thePositionAdvancesWithTheAudioRead() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.flac", 0)) {
            assertEquals(0, source.positionMs());
            byte[] audio = readAll(source);

            // The invariant that matters is self-consistency: the position written down for a resume has
            // to describe the audio actually delivered.
            assertEquals(secondsOf(audio) * 1000, source.positionMs(), 30,
                    "the reported position disagrees with the audio that was handed over");
        }
    }

    @Test
    void resumingPartWayInSkipsTheAudioAlreadyHeard() throws Exception {
        double whole;
        try (AudioSource source = open("tone-44100-stereo.flac", 0)) {
            whole = secondsOf(readAll(source));
        }

        try (AudioSource source = open("tone-44100-stereo.flac", 200)) {
            assertTrue(source.positionMs() >= 200,
                    "a resume must not replay what the commander already listened to");

            double remainder = secondsOf(readAll(source));
            // Tight on purpose: a FLAC frame is about 93 ms, so a seek that stopped at the frame
            // boundary instead of trimming to the target would miss this by more than the tolerance.
            assertEquals(whole - 0.2, remainder, 0.03,
                    "resuming 200 ms in should leave exactly that much less to play");
        }
    }

    @Test
    void seekingBeyondTheEndYieldsNoAudioRatherThanThrowing() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.flac", 60_000)) {
            assertEquals(-1, source.read(new byte[MusicFormat.BLOCK_BYTES], 0, MusicFormat.BLOCK_BYTES),
                    "running off the end of a track is an ending, not a failure");
        }
    }

    @Test
    void aFileThatIsNotThereFailsAtOpenSoThePlayerCanSkipIt() {
        assertThrows(IOException.class, () -> FlacAudioSource.open(Path.of("/no/such/track.flac"), 0));
    }

    @Test
    void aFileThatIsNotFlacFailsAtOpenRatherThanPlayingSilence() throws Exception {
        // An MP3 renamed .flac, which is what a mis-tagged library actually contains. Refusing it at open
        // lets the player log it and move on; accepting it would occupy the line playing nothing.
        assertThrows(IOException.class, () -> FlacAudioSource.open(fixturePath("tone-44100-stereo.mp3"), 0));
    }

    private static AudioSource open(String fixture, long startMs) throws IOException, URISyntaxException {
        return FlacAudioSource.open(fixturePath(fixture), startMs);
    }

    private static Path fixturePath(String name) throws URISyntaxException {
        var url = FlacAudioSourceTest.class.getResource("/jukebox/" + name);
        assertNotNull(url, "missing test fixture: " + name);
        return Path.of(url.toURI());
    }

    private static byte[] readAll(AudioSource source) throws IOException {
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] block = new byte[MusicFormat.BLOCK_BYTES];
        int read;
        while ((read = source.read(block, 0, block.length)) > 0) {
            collected.write(block, 0, read);
        }
        return collected.toByteArray();
    }

    private static double secondsOf(byte[] pcm) {
        return pcm.length / (double) MusicFormat.FRAME_BYTES / MusicFormat.SAMPLE_RATE;
    }

    private static double rootMeanSquare(byte[] pcm) {
        double sum = 0;
        int samples = pcm.length / 2;
        for (int i = 0; i < samples; i++) {
            short sample = (short) ((pcm[i * 2 + 1] << 8) | (pcm[i * 2] & 0xFF));
            sum += (double) sample * sample;
        }
        return samples == 0 ? 0 : Math.sqrt(sum / samples);
    }

    private static double channelRms(byte[] pcm, int channel) {
        double sum = 0;
        int frames = pcm.length / MusicFormat.FRAME_BYTES;
        for (int frame = 0; frame < frames; frame++) {
            int at = frame * MusicFormat.FRAME_BYTES + channel * 2;
            short sample = (short) ((pcm[at + 1] << 8) | (pcm[at] & 0xFF));
            sum += (double) sample * sample;
        }
        return frames == 0 ? 0 : Math.sqrt(sum / frames);
    }
}
