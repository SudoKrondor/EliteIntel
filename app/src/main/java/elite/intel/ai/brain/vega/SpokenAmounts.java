package elite.intel.ai.brain.vega;

import elite.intel.util.TTSFriendlyNumberConverter;

/**
 * Credit amounts as the companion should say them.
 *
 * <p>A ten-digit balance read out digit by digit is noise: the commander wants "about one point zero two
 * billion", not "one billion twenty three million three hundred nine thousand two hundred forty five". The
 * rounding is done here in Java rather than asked of the LLM, because the narration prompt tells the model to
 * copy every number from the payload exactly as written - that rule is what stops it inventing figures, and it
 * stays intact. The model receives an already-rounded, already-spelled-out phrase and only translates it.
 *
 * <p>Payloads carry both forms: the raw numeric field, and a {@code ...Spoken} companion field. {@link #RULE}
 * tells the model to speak the latter and touch the former only when the commander asks for the exact figure,
 * so precision is hedged in speech but never actually lost.
 */
public final class SpokenAmounts {

    /**
     * Appended to the narration/query instructions of any payload carrying a {@code ...Spoken} field. Names the
     * suffix rather than individual fields, so a payload can add amounts without this text changing.
     */
    public static final String RULE = """
            
            Credit amounts: every field whose name ends in "Spoken" is its amount already written the way it
            should be said out loud. Say it as written, translated into your language. Never read the digits of
            its numeric counterpart and never round it differently yourself. Read the exact numeric field only
            if the commander explicitly asked for the precise figure.
            """;

    private SpokenAmounts() {
    }

    /**
     * A {@code ...Spoken} field appended to an event's own YAML, for payloads built from a journal event
     * rather than from a record we control.
     *
     * @param field   name of the numeric field this speaks for, without the {@code Spoken} suffix
     * @param credits the amount
     */
    public static String yamlLine(String field, long credits) {
        return "\n" + field + "Spoken: " + forLlm(credits);
    }

    /**
     * A numeric line plus its spoken sibling, for a computed amount that is <em>not</em> a field of the
     * serialized payload (e.g. a purchase's net cost after trade-in). Emitting both keeps such a figure
     * uniform with real fields: it reads the spoken form and the exact number stays answerable on request.
     *
     * @param field   name to give the amount, without the {@code Spoken} suffix
     * @param credits the amount
     */
    public static String syntheticAmount(String field, long credits) {
        return "\n" + field + ": " + credits + yamlLine(field, credits);
    }

    /**
     * The amount as the LLM should receive it: rounded, spelled out, English.
     */
    public static String forLlm(long credits) {
        return TTSFriendlyNumberConverter.formatCreditsForLlm(credits);
    }
}
