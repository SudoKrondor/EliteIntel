package elite.intel.ai.mouth;

import java.util.Arrays;

/**
 * One local voice's own settings: its pace, a volume trim and a six-band EQ. The Supertonic voices differ a little
 * in loudness and speed, and this evens them out voice by voice.
 *
 * @param speed  pace on the scale the old global slider used: the voice speaks at {@code 1 + speed/100} times its
 *               own pace, so 0 is the model's pace and {@link #DEFAULT_SPEED} is a touch quicker
 * @param volume percent, multiplied onto the Voice or Radio level: 100 leaves the voice as it is
 * @param eq     gain in dB for each of {@link VoiceEqualizer#BAND_COUNT} bands, low to high
 */
public record VoiceTuning(int speed, int volume, int[] eq) {

    public static final int MIN_SPEED = 0;
    public static final int MAX_SPEED = 100;
    public static final int DEFAULT_SPEED = 20;
    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 150;
    public static final int DEFAULT_VOLUME = 100;
    public static final int MIN_EQ_DB = -12;
    public static final int MAX_EQ_DB = 12;

    public static final VoiceTuning DEFAULT =
            new VoiceTuning(DEFAULT_SPEED, DEFAULT_VOLUME, new int[VoiceEqualizer.BAND_COUNT]);

    public VoiceTuning {
        if (speed < MIN_SPEED || speed > MAX_SPEED) {
            throw new IllegalArgumentException("Voice speed must be between " + MIN_SPEED + " and " + MAX_SPEED);
        }
        if (volume < MIN_VOLUME || volume > MAX_VOLUME) {
            throw new IllegalArgumentException("Voice volume must be between " + MIN_VOLUME + " and " + MAX_VOLUME);
        }
        if (eq == null || eq.length != VoiceEqualizer.BAND_COUNT) {
            throw new IllegalArgumentException("Voice EQ needs " + VoiceEqualizer.BAND_COUNT + " bands");
        }
        for (int gain : eq) {
            if (gain < MIN_EQ_DB || gain > MAX_EQ_DB) {
                throw new IllegalArgumentException("EQ gain must be between " + MIN_EQ_DB + " and " + MAX_EQ_DB + " dB");
            }
        }
        // WHY a copy: the synthesis thread reads this while the settings dialog builds the next one.
        eq = eq.clone();
    }

    @Override
    public int[] eq() {
        return eq.clone();
    }

    /**
     * The pace handed to the engine: 1.0 is the model's own.
     */
    public float pace() {
        return 1f + speed / 100f;
    }

    public float volumeGain() {
        return volume / 100f;
    }

    public VoiceTuning withSpeed(int newSpeed) {
        return new VoiceTuning(newSpeed, volume, eq);
    }

    public VoiceTuning withVolume(int newVolume) {
        return new VoiceTuning(speed, newVolume, eq);
    }

    public VoiceTuning withEqBand(int band, int gainDb) {
        int[] bands = eq.clone();
        bands[band] = gainDb;
        return new VoiceTuning(speed, volume, bands);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VoiceTuning that
                && speed == that.speed && volume == that.volume && Arrays.equals(eq, that.eq);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * speed + volume) + Arrays.hashCode(eq);
    }

    @Override
    public String toString() {
        return "VoiceTuning[speed=" + speed + ", volume=" + volume + ", eq=" + Arrays.toString(eq) + "]";
    }
}
