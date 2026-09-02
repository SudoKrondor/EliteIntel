package elite.intel.jukebox;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decoding real M4A files.
 *
 * <p>What is different about this format, and what most of these cover, is that the extension does not
 * name the codec: an iTunes library holds AAC and Apple Lossless both as {@code .m4a}, and only the
 * former can be decoded. A file that opened and then played silence would read as broken hardware, so
 * the codec check at open is tested as carefully as the decode itself.
 *
 * <p>Durations are asserted for self-consistency rather than against the half-second the fixtures were
 * generated as: an AAC encoder adds priming samples, so these decode to nearer 0.53 s. That is the same
 * trap the MP3 fixtures carry, and the reason neither suite pins an exact length.
 */
class AacAudioSourceTest {

    @Test
    void aStereoFileDecodesToAudibleAudio() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.m4a", 0)) {
            byte[] audio = readAll(source);

            assertTrue(audio.length > 0, "the fixture is not silent, so neither should the decode be");
            assertEquals(0, audio.length % MusicFormat.FRAME_BYTES,
                    "output has to be whole stereo frames or the line will drift a byte and swap channels");
            assertTrue(rootMeanSquare(audio) > 1000,
                    "a 440 Hz tone should decode loud - near silence means the decode produced nothing");
        }
    }

    @Test
    void anAudiobookExtensionPlaysAsTheSameFormat() throws Exception {
        // .m4b is .m4a with a different name on it. Audiobooks are the case the resume position exists
        // for, so the extension they are published under has to reach the same decoder.
        try (AudioSource source = open("tone-44100-stereo.m4b", 0)) {
            assertTrue(rootMeanSquare(readAll(source)) > 1000, "an .m4b is AAC in an MP4 container too");
        }
    }

    @Test
    void appleLosslessIsRefusedAtOpenRatherThanPlayingSilence() {
        // The case that makes this format different: same extension, codec this build cannot decode.
        // Refused at open, the player logs it and moves on; accepted, it would hold the line playing nothing.
        IOException refused = assertThrows(IOException.class,
                () -> open("lossless-alac.m4a", 0));
        assertTrue(refused.getMessage().contains("AAC"),
                "the reason has to name the codec or a commander cannot act on it: " + refused.getMessage());
    }

    @Test
    void aMonoFileIsConvertedToTheOutputFormat() throws Exception {
        try (AudioSource source = open("tone-32000-mono.m4a", 0)) {
            byte[] audio = readAll(source);

            assertTrue(rootMeanSquare(audio) > 1000, "the tone survives the rate conversion");
            assertTrue(channelRms(audio, 0) > 1000, "left channel is silent");
            assertTrue(channelRms(audio, 1) > 1000, "right channel is silent - mono was not duplicated");
        }
    }

    @Test
    void aRateBelowTheOutputLineIsResampledRatherThanPlayedSlow() throws Exception {
        double stereo;
        try (AudioSource source = open("tone-44100-stereo.m4a", 0)) {
            stereo = secondsOf(readAll(source));
        }
        try (AudioSource source = open("tone-32000-mono.m4a", 0)) {
            // Both fixtures are the same length of audio at different rates. Played without resampling
            // the 32 kHz one would run out well early, so agreement between them is what proves the
            // conversion happened - without pinning either to an encoder's priming length.
            assertEquals(stereo, secondsOf(readAll(source)), 0.05,
                    "the 32 kHz file should last as long as the 44.1 kHz one, but it did not");
        }
    }

    @Test
    void thePositionAdvancesWithTheAudioRead() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.m4a", 0)) {
            assertEquals(0, source.positionMs());
            byte[] audio = readAll(source);

            assertEquals(secondsOf(audio) * 1000, source.positionMs(), 50,
                    "the reported position disagrees with the audio that was handed over");
        }
    }

    @Test
    void resumingPartWayInSkipsTheAudioAlreadyHeard() throws Exception {
        double whole;
        try (AudioSource source = open("tone-44100-stereo.m4a", 0)) {
            whole = secondsOf(readAll(source));
        }

        try (AudioSource source = open("tone-44100-stereo.m4a", 200)) {
            long resumedAt = source.positionMs();
            assertTrue(resumedAt >= 200,
                    "a resume must not replay what the commander already listened to");
            assertTrue(resumedAt < 400,
                    "the seek landed " + resumedAt + " ms in, far enough past the target to skip audio");

            double remainder = secondsOf(readAll(source));
            // Self-consistency is the invariant, not the requested position: JAAD lands on the container's
            // own boundary, tens of milliseconds after what was asked for, and reports where it actually
            // went. What must hold is that the audio still to come agrees with the position written down -
            // that is what makes a resume land where the last one left off.
            assertEquals(whole - resumedAt / 1000.0, remainder, 0.03,
                    "the audio left to play disagrees with the position reported for the resume");
        }
    }

    @Test
    void seekingBeyondTheEndYieldsNoAudioRatherThanThrowing() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.m4a", 60_000)) {
            assertEquals(-1, source.read(new byte[MusicFormat.BLOCK_BYTES], 0, MusicFormat.BLOCK_BYTES),
                    "running off the end of a track is an ending, not a failure");
        }
    }

    @Test
    void aFileThatIsNotThereFailsAtOpenSoThePlayerCanSkipIt() {
        assertThrows(IOException.class, () -> AacAudioSource.open(Path.of("/no/such/track.m4a"), 0));
    }

    @Test
    void aFileThatIsNotAnMp4FailsAtOpenRatherThanPlayingSilence() {
        assertThrows(IOException.class, () -> AacAudioSource.open(fixturePath("tone-44100-stereo.mp3"), 0));
    }

    private static AudioSource open(String fixture, long startMs) throws IOException, URISyntaxException {
        return AacAudioSource.open(fixturePath(fixture), startMs);
    }

    private static Path fixturePath(String name) throws URISyntaxException {
        var url = AacAudioSourceTest.class.getResource("/jukebox/" + name);
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
