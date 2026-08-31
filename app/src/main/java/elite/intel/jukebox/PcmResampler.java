package elite.intel.jukebox;

/**
 * Converts decoded audio to the jukebox's {@link MusicFormat#CANONICAL} format: any sample rate to
 * 44.1 kHz, any channel count to stereo.
 * <p>
 * One instance follows one track from beginning to end, because resampling has memory: the output frames
 * do not line up with the input frames, so each decoded chunk leaves a fractional position and a trailing
 * sample that the next chunk has to continue from. A fresh instance per track, never shared.
 * <p>
 * <b>On quality.</b> This interpolates linearly between neighbouring samples rather than filtering
 * properly, which trades a little high-frequency accuracy on rate changes for a great deal of simplicity.
 * That is a fair trade here: this is background music under speech, and the overwhelmingly common case is
 * a 44.1 kHz file, where every output sample lands exactly on an input sample and nothing is interpolated
 * at all.
 */
final class PcmResampler {

    private final int sourceChannels;
    private final double step;

    /**
     * Where the next output sample falls, in source frames, relative to the current chunk's start.
     */
    private double position;
    private short carriedLeft;
    private short carriedRight;
    private boolean started;

    PcmResampler(int sourceSampleRate, int sourceChannels) {
        if (sourceSampleRate <= 0) throw new IllegalArgumentException("Sample rate must be positive");
        if (sourceChannels <= 0) throw new IllegalArgumentException("Channel count must be positive");
        this.sourceChannels = sourceChannels;
        this.step = sourceSampleRate / (double) MusicFormat.SAMPLE_RATE;
    }

    /**
     * Converts one chunk of interleaved source samples and appends the result to {@code out}.
     *
     * @param interleaved decoded samples, channels interleaved
     * @param length      how many entries of {@code interleaved} are valid
     * @param out         receives canonical stereo 16-bit little-endian bytes
     */
    void resample(short[] interleaved, int length, ByteSink out) {
        int frames = length / sourceChannels;
        if (frames <= 0) return;
        if (!started) {
            carriedLeft = leftOf(interleaved, 0);
            carriedRight = rightOf(interleaved, 0);
            started = true;
        }
        // Interpolation needs a frame on each side, so this stops one short of the end and carries the
        // final frame into the next chunk to be the left-hand side there. Nothing is lost in the middle of
        // a track - the frame is emitted on the next call - and the very last frame of a file, some twenty
        // microseconds, goes unplayed rather than being extrapolated from nothing.
        while (position < frames - 1) {
            int index = (int) Math.floor(position);
            double fraction = position - index;
            short leftA = index < 0 ? carriedLeft : leftOf(interleaved, index);
            short rightA = index < 0 ? carriedRight : rightOf(interleaved, index);
            short leftB = leftOf(interleaved, index + 1);
            short rightB = rightOf(interleaved, index + 1);
            out.putFrame(interpolate(leftA, leftB, fraction), interpolate(rightA, rightB, fraction));
            position += step;
        }
        carriedLeft = leftOf(interleaved, frames - 1);
        carriedRight = rightOf(interleaved, frames - 1);
        position -= frames;
    }

    private short leftOf(short[] interleaved, int frame) {
        return interleaved[frame * sourceChannels];
    }

    /**
     * Mono is duplicated to both channels; anything beyond the first two channels is dropped.
     */
    private short rightOf(short[] interleaved, int frame) {
        int base = frame * sourceChannels;
        return sourceChannels == 1 ? interleaved[base] : interleaved[base + 1];
    }

    private static short interpolate(short a, short b, double fraction) {
        if (fraction == 0.0) return a;
        return (short) Math.round(a + (b - a) * fraction);
    }

    /**
     * Where resampled frames are written. Keeps the resampler free of any buffer's growth policy.
     */
    interface ByteSink {
        void putFrame(short left, short right);
    }
}
