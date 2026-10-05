package elite.intel.jukebox;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reading real WAV files, for the same reason the other formats' tests use real files.
 *
 * <p>The fixture that matters most is the Broadcast WAV: 24-bit, 48 kHz, in the extensible format, with
 * {@code JUNK}, {@code bext} and {@code LIST} chunks ahead of the audio. That is what a capture tool or an
 * audio workstation actually writes, and it exercises everything a textbook 16-bit file would not - the
 * unknown chunks, the depth conversion and the rate conversion at once.
 *
 * <p>The fixtures are half-second 440 Hz tones, small enough to live in the repository and loud enough
 * that "did it actually decode audio" is answerable rather than a matter of the file merely opening.
 */
class WavAudioSourceTest {

    @Test
    void aStereoFileAtTheOutputRateDecodesToAudibleAudio() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.wav", 0)) {
            byte[] audio = readAll(source);

            assertEquals(0, audio.length % MusicFormat.FRAME_BYTES,
                    "output has to be whole stereo frames or the line will drift a byte and swap channels");
            assertTrue(rootMeanSquare(audio) > 1000,
                    "a 440 Hz tone should decode loud - near silence means the decode produced nothing");
            assertEquals(0.5, secondsOf(audio), 0.02);
        }
    }

    @Test
    void aBroadcastWavIsReadPastItsExtraChunksAndScaledToSixteenBits() throws Exception {
        double sixteenBit;
        try (AudioSource source = open("tone-44100-stereo.wav", 0)) {
            sixteenBit = rootMeanSquare(readAll(source));
        }

        try (AudioSource source = open("tone-48000-stereo-24bit-bwf.wav", 0)) {
            byte[] audio = readAll(source);

            // The same tone at the same amplitude, so the level has to survive the depth conversion.
            // Reading the metadata chunks as audio, or the wrong bytes of a 24-bit sample, decodes as
            // noise at a wildly different level - a generous band still separates the two.
            assertEquals(sixteenBit, rootMeanSquare(audio), sixteenBit * 0.25,
                    "a 24-bit tone should play at the same level as the 16-bit one, not louder or quieter");
            assertEquals(0.5, secondsOf(audio), 0.02,
                    "48 kHz must be resampled to the output rate, not played slow");
        }
    }

    @Test
    void aMonoFileAtADifferentRateReachesBothChannels() throws Exception {
        try (AudioSource source = open("tone-32000-mono.wav", 0)) {
            byte[] audio = readAll(source);

            assertTrue(channelRms(audio, 0) > 1000, "left channel is silent");
            assertTrue(channelRms(audio, 1) > 1000, "right channel is silent - mono was not duplicated");
            assertEquals(0.5, secondsOf(audio), 0.02, "resampling must preserve duration");
        }
    }

    @Test
    void thePositionAdvancesWithTheAudioRead() throws Exception {
        try (AudioSource source = open("tone-48000-stereo-24bit-bwf.wav", 0)) {
            assertEquals(0, source.positionMs());
            byte[] audio = readAll(source);

            assertEquals(secondsOf(audio) * 1000, source.positionMs(), 30,
                    "the reported position disagrees with the audio that was handed over");
        }
    }

    @Test
    void resumingPartWayInSkipsExactlyTheAudioAlreadyHeard() throws Exception {
        try (AudioSource source = open("tone-48000-stereo-24bit-bwf.wav", 200)) {
            assertEquals(200, source.positionMs(),
                    "every WAV frame is the same size, so a resume lands exactly where it was asked to");

            assertEquals(0.3, secondsOf(readAll(source)), 0.02,
                    "resuming 200 ms in should leave exactly that much less to play");
        }
    }

    @Test
    void seekingBeyondTheEndYieldsNoAudioRatherThanThrowing() throws Exception {
        try (AudioSource source = open("tone-44100-stereo.wav", 60_000)) {
            assertEquals(-1, source.read(new byte[MusicFormat.BLOCK_BYTES], 0, MusicFormat.BLOCK_BYTES),
                    "running off the end of a track is an ending, not a failure");
        }
    }

    @Test
    void aCompressedWavFailsAtOpenRatherThanPlayingSilence() {
        // ADPCM inside a RIFF wrapper: a real .wav, but not one the JDK can turn into PCM. Refused at open
        // so the player logs it and moves on.
        assertThrows(IOException.class, () -> open("adpcm.wav", 0));
    }

    @Test
    void aFileThatIsNotWavFailsAtOpen() {
        assertThrows(IOException.class, () -> open("tone-44100-stereo.mp3", 0));
    }

    @Test
    void aFileThatIsNotThereFailsAtOpenSoThePlayerCanSkipIt() {
        assertThrows(IOException.class, () -> WavAudioSource.open(Path.of("/no/such/track.wav"), 0));
    }

    private static AudioSource open(String fixture, long startMs) throws IOException, URISyntaxException {
        return WavAudioSource.open(fixturePath(fixture), startMs);
    }

    private static Path fixturePath(String name) throws URISyntaxException {
        var url = WavAudioSourceTest.class.getResource("/jukebox/" + name);
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
