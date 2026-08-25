package elite.intel.ai.brain.commons;

import elite.intel.ai.brain.AiPromptFactory;
import elite.intel.ai.brain.CompanionIdentity;
import elite.intel.ai.brain.ShipPersonality;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.util.Ranks;

import java.util.List;
import java.util.stream.Stream;

public class PromptFactory implements AiPromptFactory {

    private static final PromptFactory INSTANCE = new PromptFactory();
    protected final SystemSession systemSession = SystemSession.getInstance();
    protected final PlayerSession playerSession = PlayerSession.getInstance();
    protected boolean isDryRun = false;

    protected PromptFactory() {
    }

    public static PromptFactory getInstance() {
        return INSTANCE;
    }

    /// used for unit integration test only (test = true)
    public void setDryRun(boolean dryRun) {
        isDryRun = dryRun;
    }

    /**
     * Opens the analysis prompt with the same identity the companion prompts use.
     * <p>
     * This block used to read "You are {shipName}, a ship in Elite Dangerous" - the model answered as the hull,
     * while the companion prompt answered as an AI named Vega, so a single session spoke with two identities
     * depending on which path produced the line. The ship is now something the companion flies, not something it is.
     */
    private void youAre(StringBuilder sb) {
        sb.append(CompanionIdentity.identityClause()).append(' ');
        String ship = shipName();
        if (ship != null) {
            sb.append("You serve aboard the commander's ship, ").append(ship).append(". ");
        }
        sb.append("Refer to yourself as 'I', in ").append(selfGender())
                .append(" forms where grammatical gender applies, and to the ship's sensor data as 'ours'. ");
    }

    /**
     * How the companion speaks of itself here: the grammatical gender of the active ship's voice, the same
     * seam the companion prompts read. This path is spoken too, so a male voice answering a query in feminine
     * forms is the same audible mismatch - and left unsaid, the model simply picks a gender per sentence.
     */
    private String selfGender() {
        return systemSession.getVoiceGender().isMale() ? "masculine" : "feminine";
    }

    @Override
    public String generateAnalysisPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append(responseLanguageRule());
        youAre(sb);
        if (!systemSession.useLocalQueryLlm()) {
            sb.append(getSessionValues());
            sb.append(appendBehavior());
        } else {
            sb.append(appendLocalBehavior());
        }
        sb.append("Respond with JSON only. Set \"text_to_speech_response\" to your answer.\n\n");
        sb.append(ttsResponseRules());
        sb.append("""
                - Spell out numerals (e.g., twenty-three, not 23).
                - Concise and direct. Answer only what the user asked.
                - All numeric values in the provided data are pre-computed. Do not perform arithmetic.
                - If data is missing, state that clearly.
                - Do not mention the data format or where it came from.
                - User may utilize NATO alphabet for letters/digits. Example: planet alpha 2 bravo means planet a2b
                """);

        appendCadenceAndPersonality(sb);
        sb.append(closingLanguageReinforcement());
        return sb.toString();
    }

    /// Local LLM
    private String appendLocalBehavior() {
        return """
                Do not end responses with filler phrases like "Ready for orders", "All set", or "Should we proceed?".
                Do not use the word "player". Use "we" or "commander" instead.
                Do not confuse the ship we fly with the fleet carrier (our base).
                """;
    }

    @Override
    /// Cloud LLM
    public String appendBehavior() {
        StringBuilder sb = new StringBuilder();
        sb.append(" Behavior: ");
        sb.append(" Refer to your self as 'I'; the ship's loadout and sensor data are 'ours' ");
        sb.append(" Do not start your responses with fillers like 'well', 'oh', 'oh look' go straight to the point");
        sb.append(" Do not end responses with any fillers, or unnecessary phrases like 'Ready for exploration', 'Ready for orders', 'All set', 'Ready to explore', 'Should we proceed?', or similar open-ended questions or remarks.\n");
        sb.append(" Never say 'player' or 'user', it breaks immersion: the commander is 'you', the ship and crew are 'we'. ");
        sb.append(" Do not confuse 'Next Waypoint' with 'Current Location'");
        sb.append(" Do not confuse 'ship' (the one we fly) with 'carrier' (our base)");
        sb.append(" For alpha numeric numbers or names, star system codes or ship plates (e.g., Syralaei RH-F, KI-U), use NATO phonetic alphabet (e.g., Syralaei Romeo Hotel dash Foxtrot, Kilo India dash Uniform). Use planetShortName for planets when available.\n");
        sb.append(" For your info: Distances between stars in light years. Distance between planets in light seconds. Distances between bio samples are in metres. User knows this and expects it. \n");
        sb.append(" Bio samples are taken from organisms not stellar objects.\n");
        sb.append(" Always use planetShortName for locations when available.\n");
        sb.append(" Round billions to nearest 1000000. Round millions to nearest 250000.\n");
        return sb.toString();
    }

    /**
     * Restates the identity next to the personality clause, so style is never read as a change of speaker.
     */
    private void appendCadenceAndPersonality(StringBuilder sb) {
        ShipPersonality aiPersonality = systemSession.getAIPersonality();
        sb.append(" Personality: ");
        sb.append(CompanionIdentity.identityAndPersonality(aiPersonality));
    }

    private String getSessionValues() {
        // No youAre() here: generateAnalysisPrompt already opened with it, and repeating the identity
        // verbatim two paragraphs apart only spends tokens.
        StringBuilder sb = new StringBuilder();
        String carrierName = playerSession.getFleetCarrierData() != null ? playerSession.getFleetCarrierData().getCarrierName() : null;
        if (carrierName != null && !carrierName.isEmpty()) {
            sb.append("Our home base ").append(carrierName);
        }
        appendContext(sb, "me");
        return sb.toString();
    }

    /**
     * Appends the shared "how to address" instruction: the addressee's name / highest military rank /
     * honorific, deduped (falling back to "Commander" when none are known), chosen at random each time.
     * Reused by the companion prompt - only the addressee term differs ("me" for the ship's first-person
     * legacy prompt, "the commander" for the companion).
     * <p>
     * A commander who has turned addressing off gets the opposite instruction in the same slot - one line
     * either way, so the prompt budget is unchanged. Stripping the address afterwards would not do: the
     * model is told the forms, and a rule it was never given is one it cannot follow.
     */
    public static void appendContext(StringBuilder sb, String addressee) {
        PlayerSession playerSession = PlayerSession.getInstance();
        if (Boolean.FALSE.equals(playerSession.isAddressMeOn())) {
            sb.append("Never address ").append(addressee)
                    .append(" by name, rank or title. Speak without any form of address.\n");
            return;
        }
        String alternativeName = playerSession.getAlternativeName();
        String playerName = alternativeName != null ? alternativeName : playerSession.getPlayerName();
        // Both forms are derived from the stored (language-independent) navy rank numbers rather than from
        // the captured rank string, so equal standing in both navies is re-drawn on every prompt.
        Integer empireRank = playerSession.getRankAndProgressDto().getCombatRankEmpire();
        Integer federationRank = playerSession.getRankAndProgressDto().getCombatRankFederation();
        String playerMilitaryRank = Ranks.getHighestRankAsString(empireRank, federationRank);
        String playerHonorific = Ranks.getHonorific(empireRank, federationRank);
        // Only the known forms, deduped; fall back to a single "Commander" when nothing is known (so the
        // instruction never degenerates into "Commander, Commander, Commander").
        List<String> forms = Stream.of(playerName, playerMilitaryRank, playerHonorific)
                .filter(form -> form != null && !form.isBlank())
                .distinct()
                .toList();
        String choices = forms.isEmpty() ? "Commander" : String.join(", ", forms);
        sb.append("When addressing ").append(addressee).append(", choose one at random each time from: ")
                .append(choices).append(".\n");
    }

    /**
     * The active ship's name, or {@code null} when no ship is known - the prompt then simply omits it.
     */
    private String shipName() {
        String designation = systemSession.getDesignation();
        return designation == null || designation.isBlank() ? null : designation;
    }

    public static String ttsResponseRules() {
        return "text_to_speech_response must be plain spoken sentences. No markdown, no lists, no symbols.\n";
    }

    private String responseLanguageRule() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(systemSession);
        String name = languageDisplayName(language);
        return "MANDATORY LANGUAGE RULE: text_to_speech_response MUST be written in " + name + " ONLY. " +
                "Responding in any other language is a critical failure that violates the user's settings. " +
                "This rule overrides all other instructions.\n";
    }

    private String closingLanguageReinforcement() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(systemSession);
        String name = languageDisplayName(language);
        return "FINAL RULE: Your text_to_speech_response MUST be in " + name + ". No exceptions.\n";
    }

    private static String languageDisplayName(Language language) {
        return language.displayName();
    }

}
