package elite.intel.ai.mouth.edge;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSoundEdgeAudioOutputTest {
    @Test
    void opensRequiredPcmFormatAndHonoursPlaybackInterruption() throws Exception {
        FakeLine fake = new FakeLine(false);
        AtomicReference<AudioFormat> requestedFormat = new AtomicReference<>();
        JavaSoundEdgeAudioOutput output = new JavaSoundEdgeAudioOutput(
                () -> null,
                (info, mixer) -> {
                    requestedFormat.set((AudioFormat) info.getFormats()[0]);
                    return fake.proxy;
                });

        output.open();

        AudioFormat format = requestedFormat.get();
        assertEquals(24_000f, format.getSampleRate());
        assertEquals(16, format.getSampleSizeInBits());
        assertEquals(1, format.getChannels());
        assertTrue(format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED);
        assertFalse(format.isBigEndian());

        byte[] pcm = new byte[960];
        assertTrue(output.play(pcm, () -> false));
        assertTrue(fake.bytesWritten >= pcm.length);
        assertTrue(fake.calls.contains("drain"));

        int writesBeforeInterrupt = fake.writeLengths.size();
        assertFalse(output.play(pcm, () -> true));
        assertEquals(writesBeforeInterrupt + 1, fake.writeLengths.size(),
                "an interrupted task may write the sentence gap but no PCM");
        assertTrue(fake.calls.contains("flush"));

        output.interrupt();
        output.close();
        assertFalse(fake.open.get());
        assertTrue(fake.calls.containsAll(List.of("stop", "flush", "start", "close")));
    }

    @Test
    void closesLineWhenOpeningFails() {
        FakeLine fake = new FakeLine(true);
        JavaSoundEdgeAudioOutput output = new JavaSoundEdgeAudioOutput(
                () -> null, (info, mixer) -> fake.proxy);

        assertThrows(LineUnavailableException.class, output::open);

        assertTrue(fake.calls.contains("close"));
    }

    private static final class FakeLine {
        private final boolean failOnOpen;
        private final AtomicBoolean open = new AtomicBoolean();
        private final List<String> calls = new ArrayList<>();
        private final List<Integer> writeLengths = new ArrayList<>();
        private AudioFormat format;
        private int bytesWritten;
        private final SourceDataLine proxy;

        private FakeLine(boolean failOnOpen) {
            this.failOnOpen = failOnOpen;
            proxy = (SourceDataLine) Proxy.newProxyInstance(
                    SourceDataLine.class.getClassLoader(),
                    new Class<?>[]{SourceDataLine.class},
                    (ignored, method, args) -> invoke(method.getName(), args));
        }

        private Object invoke(String name, Object[] args) throws LineUnavailableException {
            return switch (name) {
                case "open" -> {
                    open.set(true);
                    if (args != null && args.length > 0 && args[0] instanceof AudioFormat audioFormat) {
                        format = audioFormat;
                    }
                    calls.add("open");
                    if (failOnOpen) {
                        throw new LineUnavailableException("test open failure");
                    }
                    yield null;
                }
                case "isOpen" -> open.get();
                case "getFormat" -> format;
                case "write" -> {
                    int length = (int) args[2];
                    bytesWritten += length;
                    writeLengths.add(length);
                    yield length;
                }
                case "close" -> {
                    open.set(false);
                    calls.add("close");
                    yield null;
                }
                case "start", "stop", "flush", "drain" -> {
                    calls.add(name);
                    yield null;
                }
                case "isRunning", "isActive", "isControlSupported" -> false;
                case "available", "getBufferSize", "getFramePosition" -> 0;
                case "getLongFramePosition", "getMicrosecondPosition" -> 0L;
                case "getLevel" -> 0f;
                case "getControls" -> new javax.sound.sampled.Control[0];
                case "getLineInfo", "addLineListener", "removeLineListener" -> null;
                case "toString" -> "FakeSourceDataLine";
                default -> throw new UnsupportedOperationException(name);
            };
        }
    }
}
