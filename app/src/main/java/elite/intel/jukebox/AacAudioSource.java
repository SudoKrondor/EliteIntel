package elite.intel.jukebox;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.MP4InputStream;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Track;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.List;

/**
 * An AAC track inside an MP4 container - {@code .m4a}, and the {@code .m4b} an audiobook comes as -
 * decoded a frame at a time and converted to the jukebox's canonical format.
 * <p>
 * Decoding is pure Java through JAAD, for the same reason JLayer and jFLAC were chosen: no native library
 * to ship per platform.
 * <p>
 * <b>The extension does not name the codec here, and that is the trap.</b> Unlike {@code .mp3} and
 * {@code .flac}, {@code .m4a} is a container that says nothing about what is inside it. An iTunes library
 * holds AAC and Apple Lossless under the same extension, and older purchases are encrypted on top. Only
 * AAC can be decoded, so the track's codec is checked at open and anything else is refused there - see
 * {@link #audioTrack}. Refusing early is the whole point: the alternative is a file that opens, occupies
 * the output line and plays nothing, which reads as broken hardware rather than as an unsupported file.
 */
final class AacAudioSource implements AudioSource {

    private final RandomAccessFile file;
    private final AudioTrack track;
    private final Decoder decoder;
    private final SampleBuffer samples = new SampleBuffer();
    private final PcmBuffer pending = new PcmBuffer();
    private final double durationMs;

    private PcmResampler resampler;
    private int resamplerRate;
    private int resamplerChannels;
    private double positionMs;
    private boolean exhausted;

    /**
     * Opens a file positioned at {@code startMs}, matching {@link JukeboxPlayer.SourceFactory}.
     */
    static AudioSource open(Path path, long startMs) throws IOException {
        AacAudioSource source = new AacAudioSource(path);
        try {
            source.seekTo(startMs);
        } catch (IOException | RuntimeException e) {
            source.close();
            throw e;
        }
        return source;
    }

    AacAudioSource(Path path) throws IOException {
        this.file = new RandomAccessFile(path.toFile(), "r");
        try {
            requireMp4(file, path);
            Movie movie = new MP4Container(MP4InputStream.open(file)).getMovie();
            this.durationMs = movie.getDuration() * 1000.0;
            this.track = audioTrack(movie, path);
            // Little-endian 16-bit, which is what the rest of the pipeline works in. JAAD writes whole
            // samples into this buffer itself, so unlike FLAC there is no sample depth to convert.
            this.samples.setBigEndian(false);
            this.decoder = Decoder.create(track.getDecoderSpecificInfo().getData());
        } catch (IOException | RuntimeException e) {
            closeQuietly();
            throw e instanceof IOException io ? io : new IOException("Could not read " + describe(path), e);
        }
    }

    /**
     * Refuses anything that is not an MP4 before the box parser is given it.
     * <p>
     * <b>WHY this guard exists at all:</b> handed a file that is not an MP4 - an MP3 that someone renamed,
     * which a library of thousands reliably contains - the box parser reads a length from whatever bytes
     * are there and walks forever without ever failing. That is worse than an exception: it hangs the
     * playback thread on one bad file and the music simply stops. Every MP4 opens with an {@code ftyp}
     * box, so four bytes settle it before the parser is involved.
     */
    private static void requireMp4(RandomAccessFile file, Path path) throws IOException {
        byte[] header = new byte[8];
        file.seek(0);
        try {
            file.readFully(header);
        } catch (IOException e) {
            throw new IOException("Not an MP4 file: " + describe(path), e);
        }
        file.seek(0);
        // Bytes 0-3 are the box length, 4-7 its type.
        if (!(header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p')) {
            throw new IOException("Not an MP4 file: " + describe(path));
        }
    }

    /**
     * The one AAC track in the file.
     * <p>
     * Asking for AAC specifically is what rejects Apple Lossless and the encrypted older purchases: both
     * appear as tracks of another codec, so the list simply comes back empty rather than needing the
     * formats enumerated by name.
     */
    private static AudioTrack audioTrack(Movie movie, Path path) throws IOException {
        List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);
        if (tracks.isEmpty()) {
            throw new IOException("No AAC audio in " + describe(path)
                    + " - Apple Lossless and protected files cannot be played");
        }
        return (AudioTrack) tracks.get(0);
    }

    /**
     * Winds forward to {@code targetMs}, for resuming a track where the commander left it.
     * <p>
     * The container carries a sample table, so this is a lookup rather than a decode, however far in the
     * commander had got. It lands on the nearest frame boundary - about 23 ms of audio - which is the
     * same order of accuracy the MP3 source resumes with and finer than anyone notices. Unlike FLAC there
     * is nothing to trim afterwards, because the landing point is reported back and simply becomes the
     * position.
     */
    void seekTo(long targetMs) throws IOException {
        if (targetMs <= 0) return;
        if (durationMs > 0 && targetMs >= durationMs) {
            // Past the end - a stored position from a track since replaced by a shorter one. Asked to
            // seek there, JAAD clamps and quietly plays the file from the beginning, which would replay a
            // whole track the commander had finished. An ending is the honest answer.
            exhausted = true;
            positionMs = durationMs;
            return;
        }
        try {
            positionMs = track.seek(targetMs / 1000.0) * 1000.0;
        } catch (RuntimeException e) {
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
        closeQuietly();
    }

    /**
     * @return false once the track has no more frames
     */
    private boolean decodeOneFrame() throws IOException {
        try {
            if (!track.hasMoreFrames()) {
                exhausted = true;
                return false;
            }
            Frame frame = track.readNextFrame();
            if (frame == null) {
                exhausted = true;
                return false;
            }
            decoder.decodeFrame(frame.getData(), samples);
            // The frame's own timestamp rather than a running total, so a seek needs no correction and a
            // file whose frames are not perfectly contiguous still reports where it actually is.
            positionMs = frame.getTime() * 1000.0;
            convert();
            return true;
        } catch (IOException | RuntimeException e) {
            // A damaged frame ends the track rather than the application: a corrupt file in a library of
            // thousands must not stop the music, and the player moves on to the next one.
            exhausted = true;
            throw new IOException("Could not decode " + describe(), e);
        }
    }

    /**
     * A stream whose rate or channel count changes gets a fresh resampler, because the old one's carried
     * position describes a stream that no longer exists.
     * <p>
     * WHY that is not theoretical here, where it nearly is for MP3: HE-AAC carries a spectral band
     * replication layer that the decoder only discovers part way in, and switching it on doubles the
     * output rate mid-file. The decoder also upmixes mono to stereo on its own, so what comes back is
     * read from the buffer rather than assumed from the track header.
     */
    private void convert() {
        int rate = samples.getSampleRate();
        int channels = samples.getChannels();
        if (rate <= 0 || channels <= 0) return;
        if (resampler == null || rate != resamplerRate || channels != resamplerChannels) {
            resampler = new PcmResampler(rate, channels);
            resamplerRate = rate;
            resamplerChannels = channels;
        }
        byte[] decoded = samples.getData();
        short[] interleaved = new short[decoded.length / 2];
        for (int i = 0; i < interleaved.length; i++) {
            int at = i * 2;
            interleaved[i] = (short) ((decoded[at] & 0xFF) | (decoded[at + 1] << 8));
        }
        resampler.resample(interleaved, interleaved.length, pending);
    }

    private String describe() {
        return "AAC audio";
    }

    private static String describe(Path path) {
        Path name = path.getFileName();
        return name == null ? path.toString() : name.toString();
    }

    private void closeQuietly() {
        try {
            file.close();
        } catch (IOException ignored) {
            // Nothing useful can follow a failed close on a file being abandoned.
        }
    }
}
