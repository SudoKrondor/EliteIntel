package elite.intel.jukebox;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * An Ogg Vorbis file being played, decoded a packet at a time and converted to the jukebox's canonical
 * format.
 * <p>
 * Decoding is pure Java through JOrbis, for the same reason JLayer, jFLAC and JAAD were chosen: no native
 * library to ship per platform.
 * <p>
 * <b>Like {@code .m4a}, the extension does not name the codec.</b> Ogg is a container, and the same
 * {@code .ogg} holds Opus, Speex or FLAC just as readily as Vorbis. Anything that is not Vorbis fails
 * while the headers are read, which is where it should: the file is refused at open, and the player logs
 * it and moves to the next track rather than holding the output line playing nothing.
 * <p>
 * <b>Why the low-level API and not {@code VorbisFile}.</b> JOrbis ships a port of libvorbisfile that
 * would have given seeking for free, but its {@code pcm_seek} throws a {@link NullPointerException} at
 * every position in this release, and for a seekable stream it never makes the decoder ready, so its
 * read path returns nothing either. The public packet-level API below works, and the seek this class
 * needs is built on Ogg's own granule positions - see {@link #seekTo}.
 */
final class VorbisAudioSource implements AudioSource {

    /**
     * How much of the file is handed to the Ogg framer at a time.
     */
    private static final int READ_CHUNK = 8192;

    /**
     * How much audio before a seek target is decoded and thrown away.
     * <p>
     * WHY any is: a Vorbis packet is not independent. Each one is overlapped with its predecessor to
     * produce audio, so the first packet decoded after a jump yields nothing at all and the second is the
     * first that can be played. Beginning a couple of pages early gives the decoder a predecessor to
     * overlap with, so the audio at the target itself is whole. It is the same trick, for the same
     * reason, as the MP3 source's bit-reservoir priming - about 190 ms here, and a few milliseconds of
     * work.
     */
    private static final long PRIME_FRAMES = 8192;

    private final InputStream input;
    private final SyncState sync = new SyncState();
    private final StreamState stream = new StreamState();
    private final Page page = new Page();
    private final Packet packet = new Packet();
    private final Info info = new Info();
    private final Comment comment = new Comment();
    private final DspState dsp = new DspState();
    private final PcmBuffer pending = new PcmBuffer();
    private final float[][][] decoded = new float[1][][];

    private Block block;
    private PcmResampler resampler;
    private int[] channelOffsets;
    private short[] interleaved = new short[0];

    /**
     * Where the decoder has got to, in source frames from the start of the file.
     */
    private long framesDecoded;

    /**
     * Frames still to be thrown away to finish a seek.
     */
    private long discardFrames;

    /**
     * The seek target, and whether pages are being handed to the decoder yet. Until they are, pages are
     * being counted past without being decoded.
     */
    private long targetFrame;
    private boolean feeding = true;
    private long lastSkippedGranule;

    private boolean exhausted;

    /**
     * Opens a file positioned at {@code startMs}, matching {@link JukeboxPlayer.SourceFactory}.
     */
    static AudioSource open(Path file, long startMs) throws IOException {
        VorbisAudioSource source = new VorbisAudioSource(file);
        try {
            source.seekTo(startMs);
        } catch (RuntimeException e) {
            source.close();
            throw e;
        }
        return source;
    }

    VorbisAudioSource(Path file) throws IOException {
        this.input = new BufferedInputStream(new FileInputStream(file.toFile()), 64 * 1024);
        try {
            readHeaders(file);
        } catch (IOException | RuntimeException e) {
            closeQuietly();
            throw e;
        }
        this.dsp.synthesis_init(info);
        this.block = new Block(dsp);
        this.block.init(dsp);
        this.channelOffsets = new int[info.channels];
        this.resampler = new PcmResampler(info.rate, info.channels);
    }

    /**
     * Reads the three headers every Vorbis stream opens with - identification, comments and codebooks -
     * which is also what establishes that the file is Vorbis at all.
     */
    private void readHeaders(Path file) throws IOException {
        sync.init();
        info.init();
        comment.init();
        boolean streamOpened = false;
        int headers = 0;
        while (headers < 3) {
            if (!readChunk()) {
                throw new IOException("Not a readable Ogg Vorbis file: " + describe(file));
            }
            while (headers < 3 && sync.pageout(page) == 1) {
                if (!streamOpened) {
                    stream.init(page.serialno());
                    stream.reset();
                    streamOpened = true;
                }
                stream.pagein(page);
                while (headers < 3 && stream.packetout(packet) == 1) {
                    if (info.synthesis_headerin(comment, packet) < 0) {
                        // Opus, Speex, FLAC-in-Ogg and Theora all land here: an Ogg container this build
                        // has no decoder for, named with the extension that usually means Vorbis.
                        throw new IOException("Not Vorbis audio: " + describe(file));
                    }
                    headers++;
                }
            }
        }
    }

    /**
     * Winds forward to {@code targetMs}, for resuming a track where the commander left it.
     * <p>
     * <b>Why pages are counted rather than decoded.</b> Ogg has no seek table, and decoding the file up
     * to the target would cost real time - this decoder runs at a couple of hundred times playback, so an
     * hour-long recording would take the better part of half a minute to skip through, with the music
     * stopped. But every Ogg page carries a granule position, which is where its audio ends, so the
     * pages before the target can be stepped over on their headers alone and only the audio around the
     * target is ever decoded. That reads the file but decodes almost none of it.
     */
    void seekTo(long targetMs) {
        if (targetMs <= 0) return;
        targetFrame = targetMs * info.rate / 1000L;
        feeding = false;
        // The whole target is owed until the pages have been walked and it is known how much of it the
        // skipping covered. That keeps the reported position right from the moment the track is opened,
        // rather than reading as zero until the first block is asked for.
        discardFrames = targetFrame;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        while (pending.size() < length && !exhausted) {
            if (!decodeSomething()) break;
        }
        if (pending.size() == 0) return -1;
        return pending.drainInto(buffer, offset, length);
    }

    /**
     * <b>Why the frames still owed count as played:</b> the same reason as the FLAC source. Between
     * opening a track and reading from it the decoder sits behind where the commander actually is, and
     * reporting only where the decoder has got to would write a resume point earlier than the one just
     * resumed from.
     */
    @Override
    public long positionMs() {
        return (framesDecoded + discardFrames) * 1000L / info.rate;
    }

    @Override
    public void close() {
        closeQuietly();
    }

    /**
     * Advances by one page, decoding it or stepping over it.
     *
     * @return false once the file has no more pages
     */
    private boolean decodeSomething() throws IOException {
        int result = sync.pageout(page);
        if (result == 0) {
            if (!readChunk()) {
                exhausted = true;
                return false;
            }
            return true;
        }
        if (result < 0) {
            // A hole in the data. The framer resynchronises on the next page by itself, and a damaged
            // stretch of one file must not stop the music.
            return true;
        }
        if (!feeding && !startsHere()) {
            return true;
        }
        stream.pagein(page);
        while (stream.packetout(packet) == 1) {
            if (block.synthesis(packet) != 0) continue;
            dsp.synthesis_blockin(block);
            drainDecoder();
        }
        return true;
    }

    /**
     * Whether this is the page to start decoding at, while winding forward to a seek target.
     * <p>
     * A page's granule position is where its audio ends, so the page that reaches
     * {@link #PRIME_FRAMES} short of the target is the one to begin with, and the previous page's
     * granule position is where the audio about to be produced begins.
     */
    private boolean startsHere() {
        long granule = page.granulepos();
        if (granule >= 0 && granule < targetFrame - PRIME_FRAMES) {
            lastSkippedGranule = granule;
            return false;
        }
        feeding = true;
        framesDecoded = Math.max(0, lastSkippedGranule);
        discardFrames = Math.max(0, targetFrame - framesDecoded);
        return true;
    }

    /**
     * Takes everything the decoder is holding, drops whatever a seek still owes, and resamples the rest.
     */
    private void drainDecoder() {
        int samples;
        while ((samples = dsp.synthesis_pcmout(decoded, channelOffsets)) > 0) {
            int dropped = 0;
            if (discardFrames > 0) {
                dropped = (int) Math.min(discardFrames, samples);
                discardFrames -= dropped;
            }
            if (dropped < samples) {
                convert(decoded[0], dropped, samples - dropped);
            }
            framesDecoded += samples;
            dsp.synthesis_read(samples);
        }
    }

    /**
     * Interleaves the decoder's per-channel floats into the 16-bit samples the resampler works in.
     */
    private void convert(float[][] channels, int from, int count) {
        int values = count * info.channels;
        if (interleaved.length < values) {
            interleaved = new short[values];
        }
        for (int channel = 0; channel < info.channels; channel++) {
            float[] source = channels[channel];
            int at = channelOffsets[channel] + from;
            int into = channel;
            for (int i = 0; i < count; i++) {
                interleaved[into] = clamp(source[at + i]);
                into += info.channels;
            }
        }
        resampler.resample(interleaved, values, pending);
    }

    /**
     * Vorbis decodes to floats nominally within +/-1, but the format does not guarantee it and a loud
     * master can overshoot. Left unclamped those wrap around to the opposite sign, which is heard as a
     * crack rather than as the clipping it should be.
     */
    private static short clamp(float sample) {
        int value = Math.round(sample * 32768f);
        if (value > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (value < Short.MIN_VALUE) return Short.MIN_VALUE;
        return (short) value;
    }

    /**
     * @return false at end of file
     */
    private boolean readChunk() throws IOException {
        int index = sync.buffer(READ_CHUNK);
        int read = input.read(sync.data, index, READ_CHUNK);
        if (read <= 0) return false;
        sync.wrote(read);
        return true;
    }

    private static String describe(Path file) {
        Path name = file.getFileName();
        return name == null ? file.toString() : name.toString();
    }

    private void closeQuietly() {
        try {
            input.close();
        } catch (IOException ignored) {
            // Nothing useful can follow a failed close on a file being abandoned.
        }
    }
}
