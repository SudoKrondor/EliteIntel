package elite.intel.ai.mouth.edge;

import elite.intel.ai.ears.AudioDeviceEnumerator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Persistent Java Sound output line for the application's required PCM format and configured mixer. */
final class JavaSoundEdgeAudioOutput implements EdgeAudioOutput {
    /**
     * Silence written before each clip, the same gap {@code KokoroTTS} leaves between sentences.
     */
    private static final float SILENCE_GAP_SECONDS = 0.03f;
    /**
     * One tenth of a second of audio: the line buffer, and the chunk size interruption is checked between.
     */
    private static final int BUFFER_FRACTION_OF_A_SECOND = 10;

    @FunctionalInterface
    interface LineFactory {
        SourceDataLine create(DataLine.Info info, Mixer.Info mixer) throws Exception;
    }

    private final Supplier<String> outputDevice;
    private final LineFactory lineFactory;
    private final AtomicReference<SourceDataLine> line = new AtomicReference<>();

    JavaSoundEdgeAudioOutput(Supplier<String> outputDevice) {
        this(outputDevice, AudioDeviceEnumerator::openOutputLine);
    }

    JavaSoundEdgeAudioOutput(Supplier<String> outputDevice, LineFactory lineFactory) {
        this.outputDevice = outputDevice;
        this.lineFactory = lineFactory;
    }

    @Override
    public synchronized void open() throws Exception {
        SourceDataLine current = line.get();
        if (current != null && current.isOpen()) {
            return;
        }
        if (current != null) {
            current.close();
            line.compareAndSet(current, null);
        }
        AudioFormat format = new AudioFormat(EdgeProtocolConstants.SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        Mixer.Info mixer = AudioDeviceEnumerator.resolveOutputDevice(outputDevice.get());
        SourceDataLine opened = lineFactory.create(info, mixer);
        try {
            opened.open(format, bufferBytes(format));
            opened.start();
            line.set(opened);
        } catch (Exception | Error failure) {
            try {
                opened.close();
            } catch (RuntimeException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }

    @Override
    public boolean play(byte[] pcm, BooleanSupplier interrupted) {
        SourceDataLine current = line.get();
        if (current == null || !current.isOpen()) {
            throw new IllegalStateException("Edge TTS audio output is unavailable");
        }
        AudioFormat format = current.getFormat();
        int frameSize = format.getFrameSize();
        int bufferBytes = bufferBytes(format);
        // Small silence gap between sentences, as KokoroTTS does.
        byte[] silence = new byte[(int) (format.getSampleRate() * SILENCE_GAP_SECONDS) * frameSize];
        current.write(silence, 0, silence.length);
        for (int offset = 0; offset < pcm.length; offset += bufferBytes) {
            if (interrupted.getAsBoolean()) {
                current.flush();
                return false;
            }
            int length = Math.min(bufferBytes, pcm.length - offset);
            length -= length % frameSize;
            if (length > 0) {
                current.write(pcm, offset, length);
            }
        }
        if (interrupted.getAsBoolean()) {
            current.flush();
            return false;
        }
        current.drain();
        return true;
    }

    @Override
    public void interrupt() {
        SourceDataLine current = line.get();
        if (current == null || !current.isOpen()) {
            return;
        }
        current.stop();
        current.flush();
        current.start();
    }

    @Override
    public synchronized void close() {
        SourceDataLine current = line.getAndSet(null);
        if (current == null) {
            return;
        }
        try {
            current.stop();
            current.flush();
        } finally {
            current.close();
        }
    }

    private static int bufferBytes(AudioFormat format) {
        return (int) (format.getFrameSize() * format.getSampleRate() / BUFFER_FRACTION_OF_A_SECOND);
    }
}
