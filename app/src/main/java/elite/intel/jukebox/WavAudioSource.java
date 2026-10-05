package elite.intel.jukebox;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A WAV file being played, read a block at a time and converted to the jukebox's canonical format.
 * <p>
 * <b>Why Java Sound here, when every other format avoided it.</b> The other decoders are pure-Java
 * libraries because a Java Sound codec provider for MP3, FLAC or AAC may or may not be installed on the
 * commander's machine. WAV is the exception: the JDK reads it itself, on every platform, with no provider
 * to go missing. The same goes for the depth conversion - the JDK's own converter takes 24-bit, 32-bit and
 * floating-point samples down to 16 bits - so this source only has to resample and keep count.
 * <p>
 * <b>What a WAV from the wild looks like.</b> Not the textbook 16-bit 44.1 kHz file. Rips from a capture
 * tool or an audio workstation are Broadcast WAV: 24-bit, 48 kHz, with {@code JUNK}, {@code bext} and
 * {@code axml} chunks ahead of the audio. The JDK's reader steps over chunks it does not know, the depth
 * goes to the converter, and the rate to {@link PcmResampler} like any other.
 * <p>
 * WAV can also wrap compressed audio - ADPCM, GSM, even MP3. The JDK cannot convert those to PCM, and
 * they are refused at open so the player moves on rather than occupying the line with silence.
 */
final class WavAudioSource implements AudioSource {

    /**
     * Source frames read per block. Uncompressed audio has no natural frame of its own, so this is chosen
     * to match the other decoders' grain: about 23 ms at 44.1 kHz.
     */
    private static final int FRAMES_PER_READ = 1024;

    private final AudioInputStream raw;
    private final AudioInputStream pcm;
    private final PcmBuffer pending = new PcmBuffer();
    private final PcmResampler resampler;
    private final int sampleRate;
    private final int channels;
    private final byte[] bytes;
    private final short[] interleaved;

    /**
     * Source frames behind the reader, counting those a seek stepped over.
     */
    private long framesConsumed;
    private boolean exhausted;

    /**
     * Opens a file positioned at {@code startMs}, matching {@link JukeboxPlayer.SourceFactory}.
     */
    static AudioSource open(Path file, long startMs) throws IOException {
        return new WavAudioSource(file, startMs);
    }

    /**
     * The seek happens here rather than after construction because it has to: the converted stream is
     * built on top of the raw one, and a skip is only a cheap jump through the file while it is taken on
     * the raw stream, before the converter is wrapped around it.
     */
    private WavAudioSource(Path file, long startMs) throws IOException {
        try {
            this.raw = AudioSystem.getAudioInputStream(file.toFile());
        } catch (UnsupportedAudioFileException e) {
            throw new IOException("Not a WAV file we can read: " + file.getFileName(), e);
        }
        try {
            AudioFormat source = raw.getFormat();
            this.sampleRate = Math.round(source.getSampleRate());
            this.channels = source.getChannels();
            // A header claiming no rate or no channels would otherwise reach the resampler, which rejects
            // it with an IllegalArgumentException - the wrong kind of failure for a bad file.
            if (sampleRate <= 0 || channels <= 0) {
                throw new IOException("WAV header declares no audio: " + file.getFileName());
            }
            AudioFormat target = new AudioFormat(source.getSampleRate(), MusicFormat.BITS_PER_SAMPLE,
                    channels, true, false);
            if (!AudioSystem.isConversionSupported(target, source)) {
                throw new IOException(source.getEncoding() + " WAV is not supported: " + file.getFileName());
            }
            skipTo(startMs);
            this.pcm = AudioSystem.getAudioInputStream(target, raw);
            this.resampler = new PcmResampler(sampleRate, channels);
            this.interleaved = new short[FRAMES_PER_READ * channels];
            this.bytes = new byte[interleaved.length * 2];
        } catch (IOException | RuntimeException e) {
            closeQuietly(raw);
            throw e;
        }
    }

    /**
     * Steps over the audio before {@code targetMs}, for resuming a track where the commander left it.
     * <p>
     * Uncompressed audio is the one format where a resume is exact for free: every frame is the same
     * size, so the target is a byte offset and nothing has to be decoded to reach it.
     */
    private void skipTo(long targetMs) throws IOException {
        if (targetMs <= 0) return;
        long targetFrames = targetMs * sampleRate / 1000L;
        long frameSize = raw.getFormat().getFrameSize();
        long wanted = targetFrames * frameSize;
        long skipped = 0;
        while (skipped < wanted) {
            long step = raw.skip(wanted - skipped);
            if (step <= 0) {
                // Past the end of the file - a stored position from a track that has since been replaced
                // by a shorter one. Running off the end is an ending, not a failure.
                exhausted = true;
                break;
            }
            skipped += step;
        }
        framesConsumed = skipped / frameSize;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        while (pending.size() < length && !exhausted) {
            readOneBlock();
        }
        if (pending.size() == 0) return -1;
        return pending.drainInto(buffer, offset, length);
    }

    @Override
    public long positionMs() {
        return framesConsumed * 1000L / sampleRate;
    }

    @Override
    public void close() {
        closeQuietly(pcm);
        closeQuietly(raw);
    }

    private void readOneBlock() throws IOException {
        int filled = 0;
        try {
            // A read may return fewer bytes than asked, and not always a whole frame, so the block is
            // filled until it is complete or the file ends - a half frame handed on would swap channels.
            while (filled < bytes.length) {
                int read = pcm.read(bytes, filled, bytes.length - filled);
                if (read < 0) {
                    exhausted = true;
                    break;
                }
                filled += read;
            }
        } catch (IOException | RuntimeException e) {
            // A damaged file ends the track rather than the application: the player moves on to the next.
            exhausted = true;
            throw new IOException("Could not read WAV audio", e);
        }
        int samples = filled / 2 / channels * channels;
        if (samples == 0) return;
        for (int i = 0; i < samples; i++) {
            interleaved[i] = (short) ((bytes[i * 2] & 0xFF) | (bytes[i * 2 + 1] << 8));
        }
        framesConsumed += samples / channels;
        resampler.resample(interleaved, samples, pending);
    }

    private static void closeQuietly(AudioInputStream stream) {
        if (stream == null) return;
        try {
            stream.close();
        } catch (IOException ignored) {
            // Nothing useful can follow a failed close on a file being abandoned.
        }
    }
}
