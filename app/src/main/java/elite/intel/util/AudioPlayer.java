package elite.intel.util;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.NotificationVolumeChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class AudioPlayer {

    public static final String BEEP_1 = "/beep1.wav";
    public static final String BEEP_2 = "/beep2.wav";
    public static final String BEEP_3 = "/beep3.wav";

    private static final Logger log = LogManager.getLogger(AudioPlayer.class);
    private static AudioPlayer instance;
    private static float volume = SystemSession.getInstance().getBeepVolume();

    private AudioPlayer() {
        UiBus.register(this);
    }

    public static AudioPlayer getInstance() {
        if (instance == null) {
            instance = new AudioPlayer();
        }
        return instance;
    }


    @Subscribe
    public void onVolumeChangedEvent(NotificationVolumeChangedEvent event) {
        float value = event.getVolume();
        if (volume < 0.0f || volume > 1.0f) {
            throw new IllegalArgumentException("Volume must be between 0.0 and 1.0");
        }
        AudioPlayer.volume = value;
    }


    public void playBeep(String soundFile) {
        try {
            InputStream resourceStream = getClass().getResourceAsStream(soundFile);
            if (resourceStream == null) {
                log.error("Resource 'beep.wav' not found in classpath");
                return;
            }
            InputStream audioStream = new BufferedInputStream(resourceStream);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioStream);

            // Transcode to 16-bit PCM if needed so the output device can always open it.
            AudioFormat src = audioInputStream.getFormat();
            if (src.getSampleSizeInBits() != 16 || src.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                AudioFormat target = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        src.getSampleRate(), 16, src.getChannels(),
                        src.getChannels() * 2, src.getSampleRate(), false);
                if (AudioSystem.isConversionSupported(target, src)) {
                    audioInputStream = AudioSystem.getAudioInputStream(target, audioInputStream);
                }
            }

            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float range = gainControl.getMaximum() - gainControl.getMinimum();
                float gain = (range * volume) + gainControl.getMinimum();
                gainControl.setValue(gain);
            }
            clip.start();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    log.info("Finished playing beep.wav");
                }
            });
        } catch (UnsupportedAudioFileException e) {
            log.error("Unsupported audio file format for beep.wav", e);
        } catch (IOException e) {
            log.error("IO error while loading beep.wav", e);
        } catch (LineUnavailableException e) {
            log.error("Audio line unavailable for beep.wav", e);
        } catch (Exception e) {
            log.error("Unexpected error while playing beep.wav", e);
        }
    }
}