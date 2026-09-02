package elite.intel.jukebox;

import org.jflac.FLACDecoder;
import org.jflac.frame.Frame;
import org.jflac.io.RandomFileInputStream;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A FLAC file being played, decoded a frame at a time and converted to the jukebox's canonical format.
 * <p>
 * Lossless files are what a commander who cares about music actually keeps, and they are also the large
 * ones - a FLAC album is several times the size of the same album as MP3 - so the same frame-at-a-time
 * reading the MP3 source uses matters more here, not less.
 * <p>
 * Decoding is pure Java through jFLAC, chosen for the same reason JLayer was: no native library to ship
 * per platform, and no Java Sound codec provider that may or may not be installed on the commander's
 * machine.
 * <p>
 * <b>What FLAC brings that MP3 did not.</b> Two things the rest of the jukebox already handles, but which
 * were theoretical until now: sample rates well above the output line's - 96 kHz is ordinary in a
 * lossless library, where 44.1 kHz was very nearly universal in an MP3 one - and samples deeper than 16
 * bits. The rate goes to {@link PcmResampler} like any other. The depth is dealt with here, because the
 * resampler works in {@code short} and a 24-bit sample does not fit in one.
 */
final class FlacAudioSource implements AudioSource {

    /**
     * Sample depths jFLAC packs into bytes for us. The format permits 4 to 32 bits, but a decoder that
     * silently returns nothing for the depths it does not handle would play a track as silence, which
     * reads as a broken sound card rather than as an unsupported file - so anything else is refused
     * outright and the player moves on to the next track.
     */
    private static final int[] SUPPORTED_DEPTHS = {8, 16, 24};

    private final RandomFileInputStream input;
    private final FLACDecoder decoder;
    private final PcmBuffer pending = new PcmBuffer();
    private final PcmResampler resampler;
    private final int sampleRate;
    private final int channels;
    private final int bytesPerSample;
    private final StreamInfo streamInfo;

    /**
     * Reused between frames: jFLAC grows it when a frame needs more room and hands the larger one back.
     */
    private ByteData decoded;
    private short[] interleaved = new short[0];

    /**
     * Samples per channel still to be thrown away to finish a seek. See {@link #seekTo}.
     */
    private long discardFrames;
    private boolean exhausted;

    /**
     * Opens a file positioned at {@code startMs}, matching {@link JukeboxPlayer.SourceFactory}.
     */
    static AudioSource open(Path file, long startMs) throws IOException {
        FlacAudioSource source = new FlacAudioSource(file);
        try {
            source.seekTo(startMs);
        } catch (IOException | RuntimeException e) {
            source.close();
            throw e;
        }
        return source;
    }

    FlacAudioSource(Path file) throws IOException {
        // NOT wrapped in a BufferedInputStream: jFLAC's seek casts the stream it was given back to a
        // RandomFileInputStream, so a wrapper would turn every resume into an unsupported-stream failure.
        // Buffering is not lost by leaving it off - the decoder's own BitInputStream reads in chunks.
        this.input = new RandomFileInputStream(file.toFile());
        this.decoder = new FLACDecoder(input);
        StreamInfo streamInfo;
        try {
            streamInfo = decoder.readStreamInfo();
        } catch (IOException | RuntimeException e) {
            closeQuietly();
            throw new IOException("Could not read " + describe(), e);
        }
        if (streamInfo == null) {
            closeQuietly();
            throw new IOException("Not a FLAC file: " + file.getFileName());
        }
        this.streamInfo = streamInfo;
        this.sampleRate = streamInfo.getSampleRate();
        this.channels = streamInfo.getChannels();
        int depth = streamInfo.getBitsPerSample();
        if (!isSupportedDepth(depth)) {
            closeQuietly();
            throw new IOException(depth + "-bit FLAC is not supported: " + file.getFileName());
        }
        // A header claiming no rate or no channels would otherwise reach the resampler, which rejects it
        // with an IllegalArgumentException from inside a constructor - too late to close the file, and the
        // wrong kind of failure for a bad file. Refused here instead, as the unreadable file it is.
        if (sampleRate <= 0 || channels <= 0) {
            closeQuietly();
            throw new IOException("FLAC header declares no audio: " + file.getFileName());
        }
        this.bytesPerSample = depth / 8;
        // Unlike MP3, a FLAC stream's rate and channel count are fixed by its header for the whole file,
        // so one resampler serves the track and there is no mid-stream change to watch for.
        this.resampler = new PcmResampler(sampleRate, channels);
    }

    /**
     * Winds forward to {@code targetMs}, for resuming a track where the commander left it.
     * <p>
     * FLAC frames are independent, so unlike MP3 there is no decoder state to rebuild and the file's own
     * seek table can be used directly - a resume an hour into an audiobook is a handful of reads rather
     * than an hour of decoding.
     * <p>
     * <b>Why samples are then thrown away.</b> The seek lands on a frame boundary at or before the target,
     * and a FLAC frame is commonly 4096 samples - about 93 ms, several times an MP3 frame. Left there, a
     * resume would audibly repeat the moment before it. The remainder up to the target is decoded and
     * dropped, which costs one frame and makes the resume exact.
     */
    void seekTo(long targetMs) throws IOException {
        try {
            if (targetMs <= 0) {
                // The frame reader starts where the metadata blocks end, so they have to be consumed
                // even when nothing is being sought.
                decoder.readMetadata(streamInfo);
                return;
            }
            long targetFrame = targetMs * sampleRate / 1000L;
            long totalFrames = streamInfo.getTotalSamples();
            if (totalFrames > 0 && targetFrame >= totalFrames) {
                // Past the end of the file - a stored position from a track that has since been replaced
                // by a shorter one. Running off the end is an ending, not a failure: the track finishes
                // at once and the playlist moves on. Asked to seek there, jFLAC throws instead.
                exhausted = true;
                return;
            }
            long landedAt = decoder.seek(targetFrame);
            if (landedAt < 0) {
                exhausted = true;
                return;
            }
            discardFrames = Math.max(0, targetFrame - landedAt);
        } catch (IOException | RuntimeException e) {
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

    /**
     * <b>Why the samples still owed count as played.</b> A seek lands before its target and leaves the
     * remainder to be decoded and dropped, so between opening a track and reading from it the decoder
     * sits behind where the commander actually is. Reporting the decoder's position alone would write a
     * resume point earlier than the one just resumed from, and a track paused and resumed repeatedly
     * would crawl backwards. The two together are the real position at every moment: as the owed samples
     * are dropped the decoder advances by exactly as much.
     */
    @Override
    public long positionMs() {
        return (decoder.getSamplesDecoded() + discardFrames) * 1000L / sampleRate;
    }

    @Override
    public void close() {
        closeQuietly();
    }

    /**
     * @return false once the file has no more frames
     */
    private boolean decodeOneFrame() throws IOException {
        try {
            Frame frame = decoder.readNextFrame();
            if (frame == null) {
                exhausted = true;
                return false;
            }
            decoded = decoder.decodeFrame(frame, decoded);
            if (decoded != null && decoded.getLen() > 0) {
                convert(decoded);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            // A damaged frame ends the track rather than the application: a corrupt file in a library of
            // thousands must not stop the music, and the player moves on to the next one.
            exhausted = true;
            throw new IOException("Could not decode " + describe(), e);
        }
    }

    /**
     * Turns one decoded frame into the 16-bit interleaved samples the resampler works in, dropping
     * whatever a seek still owes.
     */
    private void convert(ByteData frame) {
        int samples = frame.getLen() / bytesPerSample;
        if (interleaved.length < samples) {
            interleaved = new short[samples];
        }
        byte[] bytes = frame.getData();
        for (int i = 0, at = 0; i < samples; i++, at += bytesPerSample) {
            interleaved[i] = toShort(bytes, at);
        }

        int offset = 0;
        if (discardFrames > 0) {
            long dropped = Math.min(discardFrames, samples / (long) channels);
            discardFrames -= dropped;
            offset = (int) (dropped * channels);
            if (offset >= samples) return;
        }
        resampler.resample(offset == 0 ? interleaved : shiftedCopy(offset, samples),
                samples - offset, pending);
    }

    /**
     * The tail of a frame whose first samples a seek discarded. Only ever allocated on the one frame a
     * resume lands in, so the steady state stays free of copying.
     */
    private short[] shiftedCopy(int offset, int samples) {
        short[] tail = new short[samples - offset];
        System.arraycopy(interleaved, offset, tail, 0, tail.length);
        return tail;
    }

    /**
     * One sample, little-endian at the file's depth, scaled to the 16 bits the rest of the pipeline uses.
     * Deep samples are truncated rather than dithered: this is background music under speech, and the
     * discarded bits are far below anything audible against a game and a voice.
     */
    private short toShort(byte[] bytes, int at) {
        return switch (bytesPerSample) {
            case 1 -> (short) (((bytes[at] & 0xFF) - 128) << 8);
            case 2 -> (short) ((bytes[at] & 0xFF) | (bytes[at + 1] << 8));
            // 24-bit, keeping the top two bytes.
            default -> (short) ((bytes[at + 1] & 0xFF) | (bytes[at + 2] << 8));
        };
    }

    private static boolean isSupportedDepth(int depth) {
        for (int supported : SUPPORTED_DEPTHS) {
            if (supported == depth) return true;
        }
        return false;
    }

    private String describe() {
        return "FLAC audio";
    }

    private void closeQuietly() {
        try {
            input.close();
        } catch (IOException ignored) {
            // Nothing useful can follow a failed close on a file being abandoned.
        }
    }
}
