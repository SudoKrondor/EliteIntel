package elite.intel.jukebox;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decoding real MP3 files, because the format is where the surprises are: two of the fixtures differ from
 * the output line in sample rate and channel count, which is the ordinary state of a music library and the
 * case a decoder tested only against its own output would never exercise.
 *
 * <p>The fixtures are half-second 440 Hz tones, small enough to live in the repository and loud enough
 * that "did it actually decode audio" is answerable rather than a matter of the file merely opening.
 */
class Mp3AudioSourceTest {

    @Test
    void aStereoFileAtTheOutputRateDecodesToAudibleAudio() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.mp3", 0)) {
            byte[] audio = readAll(source);

            assertTrue(audio.length > 0, "the fixture is not silent, so neither should the decode be");
            assertEquals(0, audio.length % MusicFormat.FRAME_BYTES,
                    "output has to be whole stereo frames or the line will drift a byte and swap channels");
            assertTrue(rootMeanSquare(audio) > 1000,
                    "a 440 Hz tone should decode loud - near silence means the decode produced nothing");
        }
    }

    @Test
    void aMonoFileAtADifferentRateIsConvertedToTheOutputFormat() throws Exception {
        try (AudioSource source = open("tone-32000-mono.mp3", 0)) {
            byte[] audio = readAll(source);

            assertTrue(rootMeanSquare(audio) > 1000, "the tone survives the rate conversion");
            // The fixture is a ~0.47 s tone once the encoder's padding is decoded with it. Played without
            // resampling, its 32 kHz frames would run out in about 0.36 s at 44.1 kHz - so a band this wide
            // still separates "converted" from "not converted", without pinning the test to a padding
            // length that belongs to whichever encoder built the fixture.
            double seconds = secondsOf(audio);
            assertTrue(seconds > 0.42 && seconds < 0.55,
                    "resampling must preserve duration, but this played back over " + seconds + "s");
        }
    }

    @Test
    void bothChannelsCarryAudioWhenTheSourceIsMono() throws Exception {
        try (AudioSource source = open("tone-32000-mono.mp3", 0)) {
            byte[] audio = readAll(source);

            assertTrue(channelRms(audio, 0) > 1000, "left channel is silent");
            assertTrue(channelRms(audio, 1) > 1000, "right channel is silent - mono was not duplicated");
        }
    }

    @Test
    void thePositionAdvancesWithTheAudioRead() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.mp3", 0)) {
            assertEquals(0, source.positionMs());
            byte[] audio = readAll(source);

            // The invariant that matters is self-consistency: the position written down for a resume has
            // to describe the audio actually delivered, whatever the fixture's true length turns out to be.
            assertEquals(secondsOf(audio) * 1000, source.positionMs(), 30,
                    "the reported position disagrees with the audio that was handed over");
        }
    }

    @Test
    void resumingPartWayInSkipsTheAudioAlreadyHeard() throws Exception {
        double whole;
        try (AudioSource source = open("tone-44100-stereo.mp3", 0)) {
            whole = secondsOf(readAll(source));
        }

        try (AudioSource source = open("tone-44100-stereo.mp3", 200)) {
            assertTrue(source.positionMs() >= 200,
                    "a resume must not replay what the commander already listened to");

            double remainder = secondsOf(readAll(source));
            assertEquals(whole - 0.2, remainder, 0.05,
                    "resuming 200 ms in should leave exactly that much less to play");
        }
    }

    @Test
    void seekingBeyondTheEndYieldsNoAudioRatherThanThrowing() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.mp3", 60_000)) {
            assertEquals(-1, source.read(new byte[MusicFormat.BLOCK_BYTES], 0, MusicFormat.BLOCK_BYTES),
                    "running off the end of a track is an ending, not a failure");
        }
    }

    @Test
    void aFileThatIsNotThereFailsAtOpenSoThePlayerCanSkipIt() {
        assertThrows(IOException.class, () -> Mp3AudioSource.open(Path.of("/no/such/track.mp3"), 0));
    }

    private static AudioSource open(String fixture, long startMs) throws IOException, URISyntaxException {
        return Mp3AudioSource.open(fixturePath(fixture), startMs);
    }

    private static Path fixturePath(String name) throws URISyntaxException {
        var url = Mp3AudioSourceTest.class.getResource("/jukebox/" + name);
        assertNotNull(url, "missing test fixture: " + name);
        return Path.of(url.toURI());
    }

    private static byte[] readAll(AudioSource source) throws IOException {
        java.io.ByteArrayOutputStream collected = new java.io.ByteArrayOutputStream();
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
