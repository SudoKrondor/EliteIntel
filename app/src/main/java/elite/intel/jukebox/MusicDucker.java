package elite.intel.jukebox;

import elite.intel.ai.ears.PushToTalkHoldTap;
import elite.intel.ai.mouth.VoiceLevelTap;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Pulls the music down while the companion speaks - or while the commander is holding the push-to-talk
 * button - and lets it back up afterwards.
 * <p>
 * A side-chain compressor: the companion's voice is the side-chain input and the music is what gets
 * compressed. The music's own loudness is deliberately not part of the calculation - a quiet passage
 * ducks by exactly as much as a loud one, so the commander hears speech over a steady bed rather than
 * over music that pumps in time with itself.
 * <p>
 * <b>The numbers, and why they are not on a settings panel.</b> Threshold, ratio, attack and release are
 * constants because almost nobody knows what a 12:1 ratio at -24 dBFS sounds like, and a control nobody
 * can reason about is a control that gets set wrong and blamed on the app. They are tuned here instead.
 * <p>
 * <b>Why the duck is limited.</b> At full speech volume the curve alone would pull the music down by the
 * better part of 20 dB, which is silence in all but name. {@link #MAX_GAIN_REDUCTION_DB} caps it so music
 * stays present underneath the voice - the commander asked for a duck, not a mute. Below that ceiling the
 * duck is proportional: set speech volume to a third and the music barely moves, because
 * {@link VoiceLevelTap} measures the voice after the volume control rather than before it.
 * <p>
 * <b>The second reason to duck.</b> A held push-to-talk button ducks the music too, at a fixed depth rather
 * than off a curve - there is no level to follow, only a button that is down or up. It is the same duck the
 * commander already knows from the companion speaking, arriving and leaving on the same attack and release,
 * because a second kind of duck that moved differently would read as a fault - see
 * {@link #PUSH_TO_TALK_REDUCTION_DB}.
 */
public final class MusicDucker {

    /**
     * Voice level above which the music starts to move, in dBFS, measured as RMS.
     * <p>
     * WHY this is not the -24 dBFS the same setting would carry in a streaming mixer: that number belongs
     * to a peak detector reading a live microphone, and neither half of that applies here. Speech runs some
     * 12-15 dB of crest factor, so peak and RMS put the same voice in very different places, and a
     * synthesised sentence arrives normalised near -18 dBFS RMS rather than wherever a preamp left it.
     * Carried over unchanged it produced about 5 dB of duck at full speech volume and none whatsoever below
     * half, which is a feature that looks implemented and does nothing.
     * <p>
     * This is the one number to turn if the duck feels wrong: lower it for a heavier duck that reaches
     * further down the volume range, raise it for a lighter one.
     */
    static final double THRESHOLD_DBFS = -30.0;

    /**
     * How much louder the voice has to get to move the duck another decibel.
     */
    static final double RATIO = 12.0;

    /**
     * Time constant for pulling the music down. Short: the duck must be in place before the first word.
     */
    static final double ATTACK_MS = 6.0;

    /**
     * Time constant for letting the music back up. Long on purpose: it rides straight through the gaps
     * between words and sentences, so a spoken paragraph is one smooth duck rather than a stutter.
     */
    static final double RELEASE_MS = 1000.0;

    /**
     * The most the music is ever pulled down. The commander wants it quieter, not gone.
     */
    static final double MAX_GAIN_REDUCTION_DB = 12.0;

    /**
     * How far the music comes down while the push-to-talk button is held.
     * <p>
     * WHY there is a duck here at all: push-to-talk is the mode for a commander on speakers rather than
     * headphones, so the jukebox is playing into the room the microphone is listening to. An audiobook or a
     * song with words is competing with them for the recogniser the whole time they are speaking.
     * <p>
     * WHY it is a flat figure and not a curve: the button carries no level to follow. It is deliberately set
     * to the same depth the loudest speech reaches, so the two reasons to duck feel like one behaviour -
     * press the button and the music gets out of the way exactly as far, and as smoothly, as it does when she
     * answers.
     */
    static final double PUSH_TO_TALK_REDUCTION_DB = 12.0;

    private final DoubleSupplier voiceLevelDbfs;
    private final BooleanSupplier pushToTalkHeld;

    private double gainReductionDb;

    /**
     * A ducker driven by the live companion voice.
     */
    public MusicDucker() {
        this(VoiceLevelTap::currentLevelDbfs, PushToTalkHoldTap::isHeld);
    }

    /**
     * A ducker driven by any level source, with the push-to-talk button up throughout.
     */
    public MusicDucker(DoubleSupplier voiceLevelDbfs) {
        this(voiceLevelDbfs, () -> false);
    }

    /**
     * A ducker driven by any level source and any button, so the response can be tested without playing
     * audio or holding a controller.
     */
    public MusicDucker(DoubleSupplier voiceLevelDbfs, BooleanSupplier pushToTalkHeld) {
        this.voiceLevelDbfs = voiceLevelDbfs;
        this.pushToTalkHeld = pushToTalkHeld;
    }

    /**
     * Advances the envelope by one block of music and answers what to multiply that block's samples by.
     * <p>
     * Called by the playback engine once per block, which is what makes the attack and release wall-clock
     * accurate no matter how coarsely the voice is being measured: the engine's own block rate sets the
     * resolution of the ramp.
     *
     * @param elapsedSeconds how much audio this block covers
     * @return a linear gain in (0, 1], where 1 is untouched music
     */
    public float advance(double elapsedSeconds) {
        if (elapsedSeconds <= 0 || !Double.isFinite(elapsedSeconds)) return currentGain();
        double target = targetGainReductionDb();
        double timeConstantMs = target > gainReductionDb ? ATTACK_MS : RELEASE_MS;
        double approach = 1.0 - Math.exp(-(elapsedSeconds * 1000.0) / timeConstantMs);
        gainReductionDb += approach * (target - gainReductionDb);
        return currentGain();
    }

    /**
     * The gain currently being applied, without advancing the envelope.
     */
    public float currentGain() {
        return (float) Math.pow(10.0, -gainReductionDb / 20.0);
    }

    /**
     * How far the music is currently pulled down, in decibels. Zero when nothing is speaking.
     */
    public double gainReductionDb() {
        return gainReductionDb;
    }

    /**
     * Lifts the duck immediately. For a stop, a track change, or a test.
     */
    public void reset() {
        gainReductionDb = 0.0;
    }

    /**
     * How far down the music belongs right now, before the envelope decides how quickly to get there.
     * <p>
     * The deeper of the two reasons wins rather than the two adding up: a commander who presses the button
     * while she is still talking asked for the music out of the way, not for it gone.
     */
    private double targetGainReductionDb() {
        double voiceTarget = compressionCurveDb(voiceLevelDbfs.getAsDouble());
        return pushToTalkHeld.getAsBoolean() ? Math.max(voiceTarget, PUSH_TO_TALK_REDUCTION_DB) : voiceTarget;
    }

    /**
     * The compression curve: how far down the music belongs for a given voice level.
     */
    private static double compressionCurveDb(double voiceLevelDbfs) {
        if (!Double.isFinite(voiceLevelDbfs) || voiceLevelDbfs <= THRESHOLD_DBFS) return 0.0;
        double overshoot = voiceLevelDbfs - THRESHOLD_DBFS;
        return Math.min(overshoot * (1.0 - 1.0 / RATIO), MAX_GAIN_REDUCTION_DB);
    }
}
