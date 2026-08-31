package elite.intel.ai.mouth;

import javax.sound.sampled.AudioFormat;
import java.util.concurrent.atomic.AtomicReference;

/**
 * How loud the companion is speaking right now, published by whichever Mouth is writing to the speaker.
 * <p>
 * This is the side-chain input for ducking music under speech. It is a detector and nothing more: it
 * measures, it does not decide. What a listener does with the level - how hard to duck, how fast to
 * recover - belongs to that listener, which is why no threshold or ratio appears here and why the mouth
 * package has no idea the jukebox exists.
 * <p>
 * <b>Where the measurement is taken, and why it matters.</b> Every engine applies the commander's speech
 * volume in software ({@link AudioDeClicker#applyVolume}) and then the radio filter, well before the audio
 * reaches the sound card. Observing at the line write therefore sees the signal as it will actually be
 * heard: a commander who sets speech volume to zero and reads the companion off the HUD overlay produces
 * silent PCM here, so nothing ducks, with no special case for it anywhere.
 * <p>
 * <b>Timing.</b> Engines write in chunks of about a tenth of a second, and a write lands in the line
 * buffer slightly before it is audible, so the level leads the voice by roughly one buffer. That makes
 * music dip just *before* speech rather than just after, which is the better of the two errors. A level
 * older than {@link #STALE_AFTER_MS} is reported as silence, so speech ending needs no event and no
 * cooperation from the engines - they simply stop calling and the reading decays on its own.
 */
public final class VoiceLevelTap {

    /**
     * The level reported when nothing is speaking. Far below anything a duck responds to, and finite so
     * arithmetic on it stays well defined.
     */
    public static final double SILENCE_DBFS = -120.0;

    /**
     * How long one observation stands before the voice counts as silent. Comfortably longer than the
     * ~100 ms chunk an engine writes, so ordinary pacing never reads as a gap, and short enough that the
     * end of speech is noticed promptly.
     */
    static final long STALE_AFTER_MS = 300;

    private static final double FULL_SCALE = 32768.0;
    private static final int BITS_PER_SAMPLE = 16;

    private static final AtomicReference<Observation> LATEST =
            new AtomicReference<>(new Observation(SILENCE_DBFS, 0L));

    private VoiceLevelTap() {
    }

    /**
     * Records the loudness of one chunk of audio on its way to the speaker.
     * <p>
     * Called from an engine's playback thread immediately before the write. Cheap by construction - one
     * pass over a tenth of a second of samples - because it runs on the audio path.
     *
     * @param pcm    signed 16-bit PCM, the only sample format the engines produce
     * @param offset first byte of the chunk being written
     * @param length how many bytes are being written
     * @param format the line's format, for channel count and byte order
     */
    public static void observe(byte[] pcm, int offset, int length, AudioFormat format) {
        if (pcm == null || format == null || length <= 0) return;
        if (offset < 0 || offset + length > pcm.length) return;
        if (format.getSampleSizeInBits() != BITS_PER_SAMPLE) return;
        publish(rootMeanSquareDbfs(pcm, offset, length, format.isBigEndian()));
    }

    /**
     * The companion's current speech level in dBFS, or {@link #SILENCE_DBFS} when nothing has been written
     * recently enough to still count.
     */
    public static double currentLevelDbfs() {
        Observation observation = LATEST.get();
        if (observation.atNanos() == 0L) return SILENCE_DBFS;
        long ageMs = (System.nanoTime() - observation.atNanos()) / 1_000_000L;
        return ageMs > STALE_AFTER_MS ? SILENCE_DBFS : observation.levelDbfs();
    }

    /**
     * Forgets the last observation, so the voice reads as silent immediately. For tests and shutdown.
     */
    public static void reset() {
        LATEST.set(new Observation(SILENCE_DBFS, 0L));
    }

    /**
     * WHY the level and its timestamp are replaced together rather than held in two fields: a reader that
     * caught a fresh timestamp beside a stale level would duck to the wrong depth, and one that caught the
     * reverse would hold a duck after speech had stopped.
     */
    private static void publish(double levelDbfs) {
        LATEST.set(new Observation(levelDbfs, System.nanoTime()));
    }

    /**
     * WHY RMS rather than peak: the threshold and ratio describe programme level, and speech has a high
     * crest factor - a peak detector reads a good 12 dB hotter than the voice sounds and would sit pinned
     * against the duck limit whenever the companion spoke, turning a compressor into a gate. RMS also
     * keeps the duck proportional to the commander's speech volume, which is the whole point of measuring
     * after the volume is applied rather than before.
     */
    private static double rootMeanSquareDbfs(byte[] pcm, int offset, int length, boolean bigEndian) {
        int usableBytes = length - (length % 2);
        if (usableBytes <= 0) return SILENCE_DBFS;
        double sumOfSquares = 0.0;
        for (int i = offset; i < offset + usableBytes; i += 2) {
            double normalised = sampleAt(pcm, i, bigEndian) / FULL_SCALE;
            sumOfSquares += normalised * normalised;
        }
        double rms = Math.sqrt(sumOfSquares / (usableBytes / 2));
        if (rms <= 0.0) return SILENCE_DBFS;
        return Math.max(SILENCE_DBFS, 20.0 * Math.log10(rms));
    }

    private static short sampleAt(byte[] pcm, int index, boolean bigEndian) {
        int low = bigEndian ? pcm[index + 1] & 0xFF : pcm[index] & 0xFF;
        int high = bigEndian ? pcm[index] : pcm[index + 1];
        return (short) ((high << 8) | low);
    }

    /**
     * One measurement and the instant it was taken, replaced as a unit.
     */
    private record Observation(double levelDbfs, long atNanos) {
    }
}
