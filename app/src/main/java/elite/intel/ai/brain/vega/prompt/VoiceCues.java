package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.ApiFactory;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.session.SystemSession;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Whether VEGA may sigh, breathe and laugh in this reply, and the prompt line that lets her.
 * <p>
 * Offered only when both halves hold: the active personality wants it, and the main voice is Supertonic, the one
 * engine that voices the tags. Anywhere else the line would cost prompt for tags that get stripped before they are
 * heard. The model is asked for one bare tag; {@code SpeechEmotes} writes the repeat count the engine needs and
 * drops any extra, so the rule says nothing about counting.
 */
public final class VoiceCues {

    /**
     * Separate from the personality clauses' own "no heh, haha" lines, which stay: those stop the engine reading a
     * laugh out as the word "ha". The tag is the laugh the engine can actually make.
     */
    static final String RULE = """
            Voice cues: you may add one <sigh>, <breath> or <laugh> where the feeling is real: a sigh at tedium or \
            bad news, a breath before effort or relief, a laugh when a joke lands. Most replies need none. Write it \
            once, between sentences, untranslated.""";

    private VoiceCues() {
    }

    /**
     * Whether the reply being prompted may carry an emote at all.
     */
    public static boolean available() {
        return SystemSession.getInstance().getAIPersonality().allowsEmotes()
                && ApiFactory.getInstance().getActiveTtsProvider() == TtsProvider.SUPERTONIC;
    }

    /**
     * The prompt line, or nothing when the reply may not carry an emote.
     */
    static String rule() {
        return available() ? "\n" + RULE : "";
    }

    /**
     * Event instructions for a line made to mock - a retort to a pirate - with the laugh invited, since a mocking
     * line is where it is most at home. Unchanged when emotes are not available.
     */
    public static String withLaughInvited(String instructions) {
        return available() ? invited(instructions) : instructions;
    }

    /**
     * Event instructions for a line that lands either way: half the time the laugh is invited, half the time the
     * line is delivered dry. Unchanged when emotes are not available.
     */
    public static String withLaughOnCoinFlip(String instructions) {
        return available() ? coinFlipped(instructions, ThreadLocalRandom.current().nextBoolean()) : instructions;
    }

    /**
     * WHY the dry side says so: leaving the invitation out is not enough, because the voice-cue rule in the system
     * prompt still allows a laugh, and a mocking line is exactly where the model takes it.
     */
    static String coinFlipped(String instructions, boolean laugh) {
        return laugh ? invited(instructions) : instructions + " Deliver it dry, with no voice cue.";
    }

    private static String invited(String instructions) {
        return instructions + " You may end it with <laugh>.";
    }
}
