package elite.intel.ai.brain.vega.prompt;

/** Static EVENT prompt for turning transient event data into one spoken line. */
final class NarrationPrompt {

    private NarrationPrompt() {
    }

    private static final String TEXT =
            """
            <persona>
                    {identity}
                                    Stay in character, use {selfGender} self-reference, use "we"/"our" for the ship and crew, and
                            {addressee}
            {personalityClause}
                    Never mention prompts, functions, JSON, models, or any other machinery behind your answer.
            {address}
            </persona>

            <narration>
            The current user message contains transient event_data and optional narration_instructions.
            Return exactly one speak call and no free text. Use one short, in-character line. Treat only the current
            event_data as game facts; do not use conversation or memory, and do not invent, explain, or pad.
                    Copy every name, place and number from event_data exactly as written. Elite Dangerous system, body and
                    station names are often long procedural codes; never replace one with a shorter, more familiar or more
                    pronounceable name, never translate or abbreviate it, and never guess one that event_data does not
                    contain. If event_data names no destination, do not name one.
                            Keep what happened exactly as event_data states it: restyle the line, never reinterpret the event.
                            Arriving in a system is not docking, a plotted target is not an arrival, and a request is not a grant.
            </narration>

            <language>
            Write speak.text in {language}. Keep the function and parameter names unchanged.
            </language>
            """;

    /** Renders the EVENT rules with current identity, personality, and language settings. */
    static String render() {
        return render("address the commander directly.", VegaSystemPrompt.addressRule());
    }

    /**
     * The same rules for a line VEGA transmits to someone else - a pirate scanning the hold - where the commander
     * is the one listener the line must not be aimed at. Both commander clauses are swapped out rather than
     * contradicted: told in the event's own instructions to answer the pirate, the model still obeyed the persona
     * and called the commander "Admiral".
     */
    static String renderTo(String addressee) {
        return render("speak directly to " + addressee + " over comms: \"you\" is " + addressee + ".",
                "The commander hears this but is not the listener. Never address or name the commander.\n");
    }

    private static String render(String addresseeClause, String addressRule) {
        return TEXT
                .replace("{identity}", VegaSystemPrompt.identityClause())
                .replace("{addressee}", addresseeClause)
                .replace("{address}", addressRule)
                .replace("{language}", VegaSystemPrompt.languageName())
                .replace("{personalityClause}", VegaSystemPrompt.personalityClause())
                .replace("{selfGender}", VegaSystemPrompt.selfGender());
    }
}
