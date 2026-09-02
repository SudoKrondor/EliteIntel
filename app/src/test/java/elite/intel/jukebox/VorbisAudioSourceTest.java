package elite.intel.jukebox;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decoding real Ogg Vorbis files.
 *
 * <p>Two things are worth the fixtures here. Ogg is a container like MP4, so {@code .ogg} says nothing
 * about the codec inside and an Opus file wears the same extension - that has to be refused at open
 * rather than played as silence. And Ogg has no seek table, so resuming is built on stepping over pages
 * by their granule positions; the three-second fixture exists because half a second is barely two pages
 * and would not exercise that at all.
 */
class VorbisAudioSourceTest {

    @Test
    void aStereoFileDecodesToAudibleAudio() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.ogg", 0)) {
            byte[] audio = readAll(source);

            assertTrue(audio.length > 0, "the fixture is not silent, so neither should the decode be");
            assertEquals(0, audio.length % MusicFormat.FRAME_BYTES,
                    "output has to be whole stereo frames or the line will drift a byte and swap channels");
            assertTrue(rootMeanSquare(audio) > 1000,
                    "a 440 Hz tone should decode loud - near silence means the decode produced nothing");
        }
    }

    @Test
    void xiphsOwnAudioExtensionPlaysAsTheSameFormat() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.oga", 0)) {
            assertTrue(rootMeanSquare(readAll(source)) > 1000, "an .oga is the same Vorbis stream");
        }
    }

    @Test
    void opusInAnOggContainerIsRefusedAtOpenRatherThanPlayingSilence() {
        // The case that makes this format different: same extension, codec this build cannot decode.
        IOException refused = assertThrows(IOException.class, () -> open("opus-in-ogg.ogg", 0));
        assertTrue(refused.getMessage().contains("Vorbis"),
                "the reason has to name the codec or a commander cannot act on it: " + refused.getMessage());
    }

    @Test
    void aMonoFileAtADifferentRateIsConvertedToTheOutputFormat() throws Exception {
        try (AudioSource source = open("tone-32000-mono.ogg", 0)) {
            byte[] audio = readAll(source);

            assertTrue(rootMeanSquare(audio) > 1000, "the tone survives the rate conversion");
            assertTrue(channelRms(audio, 0) > 1000, "left channel is silent");
            assertTrue(channelRms(audio, 1) > 1000, "right channel is silent - mono was not duplicated");
            assertEquals(0.5, secondsOf(audio), 0.05, "resampling must preserve duration");
        }
    }

    @Test
    void thePositionAdvancesWithTheAudioRead() throws Exception {
        try (AudioSource source = open("tone-3s-stereo.ogg", 0)) {
            assertEquals(0, source.positionMs());
            byte[] audio = readAll(source);

            assertEquals(secondsOf(audio) * 1000, source.positionMs(), 40,
                    "the reported position disagrees with the audio that was handed over");
        }
    }

    @Test
    void resumingPartWayInSkipsTheAudioAlreadyHeard() throws Exception {
        double whole;
        try (AudioSource source = open("tone-3s-stereo.ogg", 0)) {
            whole = secondsOf(readAll(source));
        }

        try (AudioSource source = open("tone-3s-stereo.ogg", 2000)) {
            assertTrue(source.positionMs() >= 2000,
                    "a resume must not replay what the commander already listened to");

            double remainder = secondsOf(readAll(source));
            // Tight on purpose: this is what proves the page skipping landed on the target rather than
            // merely somewhere plausible, and that the priming frames were discarded and not played.
            assertEquals(whole - 2.0, remainder, 0.03,
                    "resuming 2 s in should leave exactly that much less to play");
        }
    }

    @Test
    void theAudioAfterAResumeIsWholeRatherThanStartingSilent() throws Exception {
        // The reason the decoder is primed before the target: a Vorbis packet is overlapped with its
        // predecessor, so decoding cold from the target's own page yields nothing for the first packet.
        // Without priming the resume opens with a gap of silence.
        try (AudioSource source = open("tone-3s-stereo.ogg", 2000)) {
            byte[] first = new byte[MusicFormat.BLOCK_BYTES];
            int read = source.read(first, 0, first.length);

            assertTrue(read > 0, "a resume produced no audio at all");
            assertTrue(rootMeanSquare(java.util.Arrays.copyOf(first, read)) > 1000,
                    "the first block after a resume is silent - the decoder was not primed");
        }
    }

    @Test
    void seekingBeyondTheEndYieldsNoAudioRatherThanThrowing() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.ogg", 60_000)) {
            assertEquals(-1, source.read(new byte[MusicFormat.BLOCK_BYTES], 0, MusicFormat.BLOCK_BYTES),
                    "running off the end of a track is an ending, not a failure");
        }
    }

    @Test
    void aFileThatIsNotThereFailsAtOpenSoThePlayerCanSkipIt() {
        assertThrows(IOException.class, () -> VorbisAudioSource.open(Path.of("/no/such/track.ogg"), 0));
    }

    @Test
    void aFileThatIsNotOggFailsAtOpenRatherThanPlayingSilence() {
        assertThrows(IOException.class, () -> VorbisAudioSource.open(fixturePath("tone-44100-stereo.mp3"), 0));
    }

    private static AudioSource open(String fixture, long startMs) throws IOException, URISyntaxException {
        return VorbisAudioSource.open(fixturePath(fixture), startMs);
    }

    private static Path fixturePath(String name) throws URISyntaxException {
        var url = VorbisAudioSourceTest.class.getResource("/jukebox/" + name);
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
