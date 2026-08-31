package elite.intel.jukebox;

import elite.intel.ai.mouth.VoiceLevelTap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The side-chain compressor that pulls music down under the companion's voice.
 *
 * <p>Every number here is a coded constant rather than a control on the Jukebox tab, so these tests are
 * where the tuning is pinned down: the curve, the limit that keeps music audible instead of muting it,
 * and the deliberately lopsided attack and release that make a spoken paragraph one smooth duck.
 */
class MusicDuckerTest {

    /**
     * One block of music at the sort of rate the playback engine will call the ducker.
     */
    private static final double BLOCK_SECONDS = 0.005;

    // ---------------------------------------------------------------- the curve

    @Test
    void silenceLeavesTheMusicCompletelyAlone() {
        MusicDucker ducker = duckerAt(VoiceLevelTap.SILENCE_DBFS);

        advance(ducker, 2.0);

        assertEquals(1.0f, ducker.currentGain(), 0.0001f);
        assertEquals(0.0, ducker.gainReductionDb(), 0.0001);
    }

    @Test
    void aVoiceBelowTheThresholdDoesNotMoveTheMusic() {
        MusicDucker ducker = duckerAt(MusicDucker.THRESHOLD_DBFS - 1);

        advance(ducker, 2.0);

        assertEquals(0.0, ducker.gainReductionDb(), 0.0001,
                "below the threshold there is nothing to compress");
    }

    @Test
    void theDuckFollowsTheCompressionCurve() {
        // 12 dB over the threshold at 12:1 leaves 1 dB through, so 11 dB of it is taken off the music.
        MusicDucker ducker = duckerAt(MusicDucker.THRESHOLD_DBFS + 12);

        advance(ducker, 2.0);

        assertEquals(11.0, ducker.gainReductionDb(), 0.05);
    }

    @Test
    void aLoudVoiceIsHeldAtTheDuckLimitRatherThanSilencingTheMusic() {
        MusicDucker ducker = duckerAt(-1.0);

        advance(ducker, 2.0);

        assertEquals(MusicDucker.MAX_GAIN_REDUCTION_DB, ducker.gainReductionDb(), 0.05,
                "the curve alone would take nearly 20 dB, which is a mute in all but name");
        assertTrue(ducker.currentGain() > 0.24f,
                "the commander asked for the music quieter, not gone");
    }

    @Test
    void turningTheCompanionDownDucksTheMusicLess() {
        MusicDucker loud = duckerAt(MusicDucker.THRESHOLD_DBFS + 24);
        MusicDucker quiet = duckerAt(MusicDucker.THRESHOLD_DBFS + 2);

        advance(loud, 2.0);
        advance(quiet, 2.0);

        assertTrue(quiet.gainReductionDb() < loud.gainReductionDb() - 5,
                "the level is measured after the speech-volume control, so the duck tracks it");
    }

    /**
     * The threshold is a tuning constant, and this is what it is tuned FOR: a synthesised sentence lands
     * near -18 dBFS RMS at full speech volume, and the commander's volume slider scales it from there.
     * These are the figures the duck was chosen against - if one moves, the tuning moved with it.
     */
    @Test
    void aTypicalSpokenSentenceDucksTheMusicWellWithoutBuryingIt() {
        MusicDucker atFullVolume = duckerAt(-18.0);
        MusicDucker atHalfVolume = duckerAt(-24.0);
        MusicDucker atAQuarterVolume = duckerAt(-30.0);

        advance(atFullVolume, 2.0);
        advance(atHalfVolume, 2.0);
        advance(atAQuarterVolume, 2.0);

        assertTrue(atFullVolume.gainReductionDb() > 9,
                "a duck this shallow would not make speech intelligible over music");
        assertTrue(atFullVolume.currentGain() > 0.24f, "and never so deep the music disappears");
        assertTrue(atHalfVolume.gainReductionDb() > 4 && atHalfVolume.gainReductionDb() < 9,
                "halfway down the slider the duck should ease off, not switch off");
        assertEquals(0.0, atAQuarterVolume.gainReductionDb(), 0.0001,
                "a companion turned down this far is already quieter than the music it would duck");
    }

    // ---------------------------------------------------------------- the envelope

    @Test
    void theDuckIsAlmostFullyInPlaceWithinTheAttackTime() {
        MusicDucker ducker = duckerAt(-1.0);

        // One time constant of an exponential approach covers about 63% of the distance.
        advance(ducker, MusicDucker.ATTACK_MS / 1000.0);

        double proportionOfTarget = ducker.gainReductionDb() / MusicDucker.MAX_GAIN_REDUCTION_DB;
        assertEquals(0.63, proportionOfTarget, 0.05,
                "the music has to be out of the way before the first word lands");
    }

    @Test
    void theMusicComesBackSlowlyEnoughToRideThroughAGapBetweenWords() {
        Voice voice = speakingAt(-1.0);
        MusicDucker ducker = new MusicDucker(voice);
        advance(ducker, 2.0);
        double ducked = ducker.gainReductionDb();

        // A breath between words: the voice drops out for a fifth of a second.
        voice.fallSilent();
        advance(ducker, 0.2);

        assertTrue(ducker.gainReductionDb() > ducked * 0.75,
                "a 1 second release must not let the music surge back between words");
    }

    @Test
    void theMusicIsFullyBackAfterTheVoiceHasBeenQuietForAWhile() {
        Voice voice = speakingAt(-1.0);
        MusicDucker ducker = new MusicDucker(voice);
        advance(ducker, 2.0);

        voice.fallSilent();
        advance(ducker, 8.0);

        assertEquals(1.0f, ducker.currentGain(), 0.01f);
    }

    @Test
    void theDuckArrivesFarFasterThanItLeaves() {
        Voice voice = speakingAt(-1.0);
        MusicDucker ducker = new MusicDucker(voice);

        advance(ducker, 0.05);
        double afterFiftyMsOfSpeech = ducker.gainReductionDb();

        voice.fallSilent();
        advance(ducker, 0.05);

        assertTrue(afterFiftyMsOfSpeech > MusicDucker.MAX_GAIN_REDUCTION_DB * 0.99,
                "50 ms is many attack time constants, so the duck is fully in");
        assertTrue(ducker.gainReductionDb() > MusicDucker.MAX_GAIN_REDUCTION_DB * 0.9,
                "50 ms is a twentieth of the release, so the music has barely started to return");
    }

    // ---------------------------------------------------------------- the push-to-talk button

    @Test
    void holdingThePushToTalkButtonDucksTheMusicWithNothingSpeaking() {
        Button button = new Button();
        MusicDucker ducker = new MusicDucker(() -> VoiceLevelTap.SILENCE_DBFS, button);

        button.press();
        advance(ducker, 0.5);

        assertEquals(MusicDucker.PUSH_TO_TALK_REDUCTION_DB, ducker.gainReductionDb(), 0.05,
                "a commander on speakers is talking over their own music, whether or not she is");
    }

    @Test
    void theButtonDucksOnTheSameAttackAsTheVoice() {
        Button button = new Button();
        MusicDucker ducker = new MusicDucker(() -> VoiceLevelTap.SILENCE_DBFS, button);

        button.press();
        advance(ducker, MusicDucker.ATTACK_MS / 1000.0);

        double proportionOfTarget = ducker.gainReductionDb() / MusicDucker.PUSH_TO_TALK_REDUCTION_DB;
        assertEquals(0.63, proportionOfTarget, 0.05,
                "the music has to be out of the way before the first word reaches the microphone");
    }

    @Test
    void releasingTheButtonBringsTheMusicBackOnTheSameReleaseAsTheVoice() {
        Button button = new Button();
        MusicDucker ducker = new MusicDucker(() -> VoiceLevelTap.SILENCE_DBFS, button);
        button.press();
        advance(ducker, 0.5);
        double ducked = ducker.gainReductionDb();

        button.release();
        advance(ducker, 0.05);

        assertTrue(ducker.gainReductionDb() > ducked * 0.9,
                "a twentieth of the release has barely started to lift the duck");

        advance(ducker, 8.0);

        assertEquals(1.0f, ducker.currentGain(), 0.01f,
                "the button up is the whole signal: the music comes all the way back");
    }

    @Test
    void speakingWhileTheButtonIsHeldDucksOnceRatherThanTwice() {
        Button button = new Button();
        MusicDucker ducker = new MusicDucker(() -> -1.0, button);

        button.press();
        advance(ducker, 2.0);

        assertEquals(MusicDucker.MAX_GAIN_REDUCTION_DB, ducker.gainReductionDb(), 0.05,
                "the deeper reason wins - two reasons to duck must not add up into a mute");
    }

    @Test
    void aVoiceStillDucksWhileTheButtonIsUp() {
        MusicDucker ducker = new MusicDucker(() -> MusicDucker.THRESHOLD_DBFS + 12, () -> false);

        advance(ducker, 2.0);

        assertEquals(11.0, ducker.gainReductionDb(), 0.05,
                "push-to-talk is an addition to the speech duck, not a replacement for it");
    }

    // ---------------------------------------------------------------- robustness

    @Test
    void aBlockOfNoDurationDoesNotDisturbTheEnvelope() {
        MusicDucker ducker = duckerAt(-1.0);
        advance(ducker, 2.0);
        double before = ducker.gainReductionDb();

        ducker.advance(0);
        ducker.advance(-1);
        ducker.advance(Double.NaN);

        assertEquals(before, ducker.gainReductionDb(), 0.0001);
    }

    @Test
    void anUnreadableVoiceLevelIsTreatedAsSilenceRatherThanAsLoud() {
        MusicDucker ducker = duckerAt(Double.NaN);

        advance(ducker, 1.0);

        assertEquals(0.0, ducker.gainReductionDb(), 0.0001,
                "a detector fault must fail open - a jammed-on duck would look like broken audio");
    }

    @Test
    void resettingLiftsTheDuckImmediatelyForAStopOrTrackChange() {
        MusicDucker ducker = duckerAt(-1.0);
        advance(ducker, 2.0);

        ducker.reset();

        assertEquals(1.0f, ducker.currentGain(), 0.0001f);
    }

    // ---------------------------------------------------------------- helpers

    private static MusicDucker duckerAt(double voiceLevelDbfs) {
        return new MusicDucker(() -> voiceLevelDbfs);
    }

    private static Voice speakingAt(double voiceLevelDbfs) {
        Voice voice = new Voice();
        voice.levelDbfs = voiceLevelDbfs;
        return voice;
    }

    /**
     * Runs the ducker for exactly {@code seconds} of music, in the block sizes the playback engine will
     * use plus whatever partial block is left over.
     * <p>
     * WHY the remainder matters: a loop that only takes whole blocks overshoots, and at a 6 ms attack an
     * extra 4 ms is most of a time constant - enough to make a correct envelope look wrong.
     */
    private static void advance(MusicDucker ducker, double seconds) {
        int wholeBlocks = (int) (seconds / BLOCK_SECONDS);
        for (int block = 0; block < wholeBlocks; block++) {
            ducker.advance(BLOCK_SECONDS);
        }
        double remainder = seconds - wholeBlocks * BLOCK_SECONDS;
        if (remainder > 1e-9) {
            ducker.advance(remainder);
        }
    }

    /**
     * A push-to-talk button that goes up and down while the ducker is running, the way
     * {@code PushToTalkHoldTap} does - the ducker re-reads it on every block.
     */
    private static final class Button implements java.util.function.BooleanSupplier {
        private boolean held;

        void press() {
            held = true;
        }

        void release() {
            held = false;
        }

        @Override
        public boolean getAsBoolean() {
            return held;
        }
    }

    /**
     * A voice whose level changes while the ducker is running, which is what the real one does - the
     * ducker re-reads {@link VoiceLevelTap} on every block rather than being told once.
     */
    private static final class Voice implements java.util.function.DoubleSupplier {
        private double levelDbfs = VoiceLevelTap.SILENCE_DBFS;

        void fallSilent() {
            levelDbfs = VoiceLevelTap.SILENCE_DBFS;
        }

        @Override
        public double getAsDouble() {
            return levelDbfs;
        }
    }
}
