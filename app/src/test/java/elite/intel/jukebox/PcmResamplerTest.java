package elite.intel.jukebox;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Everything reaching the sound card has to be 44.1 kHz stereo, because the output line is opened once and
 * kept for the whole session rather than renegotiated per track. This is what makes that true for a
 * library that is a mixture of rates and channel counts.
 */
class PcmResamplerTest {

    @Test
    void aFileAlreadyAtTheOutputRateIsPassedThroughUntouched() {
        Frames out = new Frames();
        short[] stereo = {100, -100, 200, -200, 300, -300, 400, -400};

        new PcmResampler(44_100, 2).resample(stereo, stereo.length, out);

        // The trailing frame is carried into the next chunk as the left-hand side of the next
        // interpolation, so it appears there rather than here.
        assertEquals(List.of("100/-100", "200/-200", "300/-300"), out.rendered(),
                "the common case must not be quietly degraded by interpolation it does not need");
    }

    @Test
    void monoIsCopiedToBothChannels() {
        Frames out = new Frames();
        short[] mono = {500, 600, 700, 800};

        new PcmResampler(44_100, 1).resample(mono, mono.length, out);

        assertEquals(List.of("500/500", "600/600", "700/700"), out.rendered(),
                "a mono track must come out of both speakers, not just the left one");
    }

    @Test
    void theFrameCarriedOverAppearsAtTheStartOfTheNextChunk() {
        Frames out = new Frames();
        PcmResampler resampler = new PcmResampler(44_100, 1);

        resampler.resample(new short[]{10, 20, 30}, 3, out);
        resampler.resample(new short[]{40, 50, 60}, 3, out);

        assertEquals(List.of("10/10", "20/20", "30/30", "40/40", "50/50"), out.rendered(),
                "nothing may be dropped mid-track - every carried frame is emitted on the next call");
    }

    @Test
    void aHigherRateSourceYieldsFewerFrames() {
        Frames out = new Frames();
        short[] source = ramp(4800, 1);

        // 48 kHz in, 44.1 kHz out: about 0.919 output frames per input frame.
        new PcmResampler(48_000, 1).resample(source, source.length, out);

        assertEquals(4410, out.count(), 2,
                "the point of resampling is that a second of audio stays a second long");
    }

    @Test
    void aLowerRateSourceYieldsMoreFrames() {
        Frames out = new Frames();
        short[] source = ramp(3200, 1);

        new PcmResampler(32_000, 1).resample(source, source.length, out);

        assertEquals(4410, out.count(), 2);
    }

    @Test
    void chunkBoundariesDoNotInterruptTheStream() {
        // The same signal, once whole and once split, must resample to the same audio - the resampler
        // carries a fractional position between calls, and losing it would click at every frame boundary.
        short[] whole = ramp(2000, 1);
        Frames inOneGo = new Frames();
        new PcmResampler(48_000, 1).resample(whole, whole.length, inOneGo);

        Frames inPieces = new Frames();
        PcmResampler split = new PcmResampler(48_000, 1);
        for (int offset = 0; offset < whole.length; offset += 317) {
            int length = Math.min(317, whole.length - offset);
            short[] piece = new short[length];
            System.arraycopy(whole, offset, piece, 0, length);
            split.resample(piece, length, inPieces);
        }

        assertEquals(inOneGo.count(), inPieces.count(), 1,
                "a stream split into odd-sized chunks must produce the same number of frames");
        int compared = Math.min(inOneGo.count(), inPieces.count());
        for (int i = 0; i < compared; i++) {
            assertEquals(inOneGo.left(i), inPieces.left(i), 2,
                    "sample " + i + " diverged, so the carried position was lost between chunks");
        }
    }

    @Test
    void aRisingSignalStaysRisingRatherThanBeingScrambled() {
        Frames out = new Frames();
        short[] source = ramp(1000, 1);

        new PcmResampler(32_000, 1).resample(source, source.length, out);

        for (int i = 1; i < out.count(); i++) {
            assertTrue(out.left(i) >= out.left(i - 1),
                    "interpolation must not reorder or overshoot a monotonic signal");
        }
    }

    @Test
    void channelsBeyondStereoAreDropped() {
        Frames out = new Frames();
        short[] fiveOne = {10, 20, 30, 40, 50, 60, 11, 21, 31, 41, 51, 61};

        new PcmResampler(44_100, 6).resample(fiveOne, fiveOne.length, out);

        assertEquals(List.of("10/20"), out.rendered(),
                "the front pair is the honest stereo answer, and the line only has two channels");
    }

    private static short[] ramp(int frames, int channels) {
        short[] samples = new short[frames * channels];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) (i % 30_000);
        }
        return samples;
    }

    /**
     * Collects resampled frames so a test can read them back as numbers instead of bytes.
     */
    private static final class Frames implements PcmResampler.ByteSink {
        private final List<short[]> frames = new ArrayList<>();

        @Override
        public void putFrame(short left, short right) {
            frames.add(new short[]{left, right});
        }

        int count() {
            return frames.size();
        }

        short left(int index) {
            return frames.get(index)[0];
        }

        List<String> rendered() {
            return frames.stream().map(f -> f[0] + "/" + f[1]).toList();
        }
    }
}
