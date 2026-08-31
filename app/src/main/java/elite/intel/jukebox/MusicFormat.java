package elite.intel.jukebox;

import javax.sound.sampled.AudioFormat;

/**
 * The one audio format the jukebox plays in, and the block sizes it works in.
 * <p>
 * <b>Why a fixed format rather than the file's own.</b> A library is a mixture of 44.1 kHz, 48 kHz and
 * 32 kHz files, in mono and stereo. Opening the output line at each file's format would renegotiate the
 * sound device between tracks - a gap, a click, and on some devices a format the driver simply refuses -
 * and would reset the duck envelope at every track change. Everything is converted to this format instead,
 * and the line is opened once and kept. 44.1 kHz is the rate almost every MP3 already uses, so the common
 * case converts nothing at all.
 */
public final class MusicFormat {

    public static final float SAMPLE_RATE = 44_100f;
    public static final int BITS_PER_SAMPLE = 16;
    public static final int CHANNELS = 2;

    /**
     * Signed 16-bit little-endian stereo at 44.1 kHz.
     */
    public static final AudioFormat CANONICAL =
            new AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, true, false);

    /**
     * Bytes in one stereo frame.
     */
    public static final int FRAME_BYTES = CHANNELS * BITS_PER_SAMPLE / 8;

    /**
     * Frames the player processes at a time - about 5.8 ms.
     * <p>
     * WHY this small: the duck envelope advances once per block, so the block length is the resolution of
     * a 6 ms attack. A block of the usual size for file playback would be several times the attack itself
     * and would turn the ramp into a step.
     */
    public static final int BLOCK_FRAMES = 256;

    public static final int BLOCK_BYTES = BLOCK_FRAMES * FRAME_BYTES;

    /**
     * How much audio the output line holds - a tenth of a second, deliberately the same depth the speech
     * engines use.
     * <p>
     * WHY it has to match theirs: a write is heard one buffer later, so the music the duck is applied to
     * is heard one music-buffer later and the speech that triggered it one speech-buffer later. Equal
     * buffers put the duck exactly under the word that caused it. A deeper music buffer would let the
     * first word through undercut, which is the failure this whole feature exists to prevent.
     */
    public static int lineBufferBytes() {
        return (int) (FRAME_BYTES * SAMPLE_RATE / 10);
    }

    /**
     * How long one full block of audio lasts, for advancing the duck envelope.
     */
    public static double blockSeconds(int byteCount) {
        return (byteCount / (double) FRAME_BYTES) / SAMPLE_RATE;
    }

    private MusicFormat() {
    }
}
