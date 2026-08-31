package elite.intel.jukebox;

import elite.intel.ai.ears.AudioDeviceEnumerator;
import elite.intel.session.SystemSession;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * The jukebox's own output line, on the same speaker the commander chose for the companion's voice.
 * <p>
 * A line of its own rather than a share of the speech engine's: the two are mixed by the operating system,
 * which is what allows music to keep playing underneath speech instead of taking turns with it. The device
 * is read once when the line opens, so changing the speaker takes effect when playback next starts.
 */
final class JavaSoundMusicOutput implements MusicOutput {

    private SourceDataLine line;

    @Override
    public void open() throws Exception {
        if (line != null && line.isOpen()) return;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, MusicFormat.CANONICAL);
        Mixer.Info mixer = AudioDeviceEnumerator.resolveOutputDevice(
                SystemSession.getInstance().getAudioOutputDevice());
        SourceDataLine opened = AudioDeviceEnumerator.openOutputLine(info, mixer);
        try {
            opened.open(MusicFormat.CANONICAL, MusicFormat.lineBufferBytes());
            opened.start();
            line = opened;
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
    public void write(byte[] pcm, int offset, int length) {
        SourceDataLine current = line;
        if (current == null || !current.isOpen()) {
            throw new IllegalStateException("Jukebox audio output is unavailable");
        }
        current.write(pcm, offset, length);
    }

    @Override
    public void flush() {
        SourceDataLine current = line;
        if (current == null || !current.isOpen()) return;
        current.stop();
        current.flush();
        current.start();
    }

    @Override
    public void close() {
        SourceDataLine current = line;
        line = null;
        if (current == null) return;
        try {
            current.stop();
            current.flush();
        } finally {
            current.close();
        }
    }
}
