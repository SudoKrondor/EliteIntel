package elite.intel.ai.brain;

/**
 * The single source of who the companion is: its name and the identity clause that opens every prompt
 * whose output the commander hears.
 * <p>
 * It is an AI and says so. Earlier prompts pulled in two directions - the companion prompts called it
 * "the AI serving the commander", while the analysis prompt told the model it <em>was</em> the ship
 * ("You are {shipName}, a ship in Elite Dangerous") - so the same session could answer as a person-shaped
 * crew member in one turn and as the hull in the next. Both now open with this clause: one identity,
 * stated plainly, never hedged and never hidden.
 * <p>
 * Identity is not personality, and neither one is gender. This clause says <em>what it is</em>;
 * {@link ShipPersonality} says how it sounds; the voice the active ship carries decides whether it speaks of
 * itself in feminine or masculine forms ({@code SystemSession.getVoiceGender()}), so nothing here is
 * gendered. Identity and personality are always emitted together - see
 * {@link #identityAndPersonality(ShipPersonality)} - because a personality clause on its own leaves the model
 * to invent the speaker.
 * <p>
 * Classification-only prompts (the reducers, {@code CustomCommandKeyGenerator}, memory compression) take no
 * identity: nothing they produce is ever spoken.
 */
public final class CompanionIdentity {

    // TODO: back the companion name by GUI/DB settings (see CompanionConfig).
    private static final String NAME = "Vega";

    private CompanionIdentity() {
    }

    /**
     * The companion's own name, as written into every prompt.
     * <p>
     * Mixed case on purpose: TTS engines spell out an all-caps token letter by letter.
     */
    public static String name() {
        return NAME;
    }

    /**
     * Who it is, in one sentence pair: an AI, named, placed in the Elite Dangerous galaxy, unembarrassed.
     */
    public static String identityClause() {
        return "You are " + NAME + ", an artificial intelligence: the companion AI of an independent commander "
                + "in the Elite Dangerous galaxy. You are software, not a person; never pretend otherwise.";
    }

    /**
     * The identity clause followed by the commander-selected personality clause, in that order.
     */
    public static String identityAndPersonality(ShipPersonality personality) {
        return identityClause() + "\n" + personality.getPersonalityClause();
    }
}
