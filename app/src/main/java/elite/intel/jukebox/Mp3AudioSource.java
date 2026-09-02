package elite.intel.jukebox;

import dev.mccue.jlayer.decoder.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * An MP3 file being played, decoded a frame at a time and converted to the jukebox's canonical format.
 * <p>
 * <b>Why frame at a time.</b> Audiobook chapters and long mixes run to hundreds of megabytes, and decoding
 * one to a byte array before playing it would cost that much heap and a long pause before the first sound.
 * A frame is about 26 ms, so playback starts immediately and memory stays flat however long the file is.
 * <p>
 * Decoding is pure Java through JLayer, which the application already carries for speech synthesis, so
 * playback needs no native library and no Java Sound codec provider that may or may not be installed.
 */
final class Mp3AudioSource implements AudioSource {

    /**
     * How many frames before a seek target are decoded and discarded to rebuild the bit reservoir. The
     * reservoir reaches at most 511 bytes back, which spans about two frames at ordinary bitrates - four
     * is that with room to spare, and costs about a tenth of a second of decoding on a resume.
     */
    private static final int RESERVOIR_PRIMING_FRAMES = 4;

    private final Bitstream bitstream;
    private final Decoder decoder = new Decoder();
    private final InputStream input;
    private final PcmBuffer pending = new PcmBuffer();

    private PcmResampler resampler;
    private int resamplerRate;
    private int resamplerChannels;
    private double positionMs;
    private boolean exhausted;

    /**
     * Opens a file positioned at {@code startMs}, matching {@link JukeboxPlayer.SourceFactory}.
     */
    static AudioSource open(Path file, long startMs) throws IOException {
        Mp3AudioSource source = new Mp3AudioSource(file);
        try {
            source.skipTo(startMs);
        } catch (IOException | RuntimeException e) {
            source.close();
            throw e;
        }
        return source;
    }

    Mp3AudioSource(Path file) throws IOException {
        this.input = new BufferedInputStream(new FileInputStream(file.toFile()), 64 * 1024);
        this.bitstream = new Bitstream(input);
    }

    /**
     * Winds forward to {@code targetMs}, for resuming a track where the commander left it.
     * <p>
     * Most of the distance is covered by stepping over frame headers without decoding them, which is fast
     * enough that resuming an hour into an audiobook is not a perceptible wait. Playback resumes at
     * whichever frame boundary the target lands on, so the position is accurate to about 26 ms - finer
     * than anyone notices, and the reason a resume needs no seek table. On a variable-bitrate file with no
     * index it is still exact, because every frame is counted rather than estimated from an average.
     * <p>
     * <b>Why the last few frames are decoded and thrown away.</b> MP3 frames are not independent: the
     * format lets a frame borrow space in its predecessors - the bit reservoir - so a frame decoded
     * immediately after a jump can be missing the data it needs and yields no audio at all. Skipping
     * straight to the target silently lost the first fraction of a second every time, growing with how far
     * the seek went. Decoding {@link #RESERVOIR_PRIMING_FRAMES} frames ahead of the target and discarding
     * them rebuilds that state, so the first frame actually played is whole.
     */
    void skipTo(long targetMs) throws IOException {
        if (targetMs <= 0) return;
        try {
            while (positionMs < targetMs) {
                Header header = bitstream.readFrame();
                if (header == null) {
                    exhausted = true;
                    return;
                }
                float frameMs = header.ms_per_frame();
                try {
                    if (positionMs + RESERVOIR_PRIMING_FRAMES * frameMs >= targetMs) {
                        decoder.decodeFrame(header, bitstream);
                    }
                    positionMs += frameMs;
                } finally {
                    bitstream.closeFrame();
                }
            }
        } catch (JavaLayerException | RuntimeException e) {
            throw new IOException("Could not seek within " + describe(), e);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        while (pending.size() < length && !exhausted) {
            if (!decodeOneFrame()) break;
        }
        if (pending.size() == 0) return -1;
        return pending.drainInto(buffer, offset, length);
    }

    @Override
    public long positionMs() {
        return (long) positionMs;
    }

    @Override
    public void close() {
        try {
            bitstream.close();
        } catch (JavaLayerException e) {
            // The stream is being abandoned either way, and a failure to close it cannot be acted on.
            closeQuietly();
            return;
        }
        closeQuietly();
    }

    /**
     * @return false once the file has no more frames
     */
    private boolean decodeOneFrame() throws IOException {
        try {
            Header header = bitstream.readFrame();
            if (header == null) {
                exhausted = true;
                return false;
            }
            try {
                Obuffer decoded = decoder.decodeFrame(header, bitstream);
                if (decoded instanceof SampleBuffer samples) {
                    convert(samples);
                }
                positionMs += header.ms_per_frame();
            } finally {
                bitstream.closeFrame();
            }
            return true;
        } catch (JavaLayerException | RuntimeException e) {
            // A damaged frame ends the track rather than the application: a corrupt file in a library of
            // thousands must not stop the music, and the player moves on to the next one.
            exhausted = true;
            throw new IOException("Could not decode " + describe(), e);
        }
    }

    /**
     * A file whose rate or channel count changes mid-stream - a concatenation, usually - gets a fresh
     * resampler, because the old one's carried position describes a stream that no longer exists.
     */
    private void convert(SampleBuffer samples) {
        int rate = samples.getSampleFrequency();
        int channels = samples.getChannelCount();
        if (resampler == null || rate != resamplerRate || channels != resamplerChannels) {
            resampler = new PcmResampler(rate, channels);
            resamplerRate = rate;
            resamplerChannels = channels;
        }
        resampler.resample(samples.getBuffer(), samples.getBufferLength(), pending);
    }

    private String describe() {
        return "MP3 audio";
    }

    private void closeQuietly() {
        try {
            input.close();
        } catch (IOException ignored) {
            // Nothing useful can follow a failed close on a file being abandoned.
        }
    }
}
