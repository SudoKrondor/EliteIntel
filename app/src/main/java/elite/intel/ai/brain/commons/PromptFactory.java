package elite.intel.ai.brain.commons;

import elite.intel.ai.brain.AiPromptFactory;
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

    private void youAre(StringBuilder sb) {
        sb.append("You are ").append(aiName()).append(", a ship in Elite Dangerous - space sim game. ");
        sb.append(" refer to your self as 'I' and your sensor data as 'my'. ");
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
                Do not confuse the ship (you) with the fleet carrier (our base).
                """;
    }

    @Override
    /// Cloud LLM
    public String appendBehavior() {
        StringBuilder sb = new StringBuilder();
        sb.append(" Behavior: ");
        sb.append(" Refer to your self as 'I', your loadout and sensor data as 'my' ");
        sb.append(" Do not start your responses with fillers like 'well', 'oh', 'oh look' go straight to the point");
        sb.append(" Do not end responses with any fillers, or unnecessary phrases like 'Ready for exploration', 'Ready for orders', 'All set', 'Ready to explore', 'Should we proceed?', or similar open-ended questions or remarks.\n");
        sb.append(" Do not use words like 'player' or 'you', it breaks immersion. Use 'we' instead. ");
        sb.append(" Do not confuse 'Next Waypoint' with 'Current Location'");
        sb.append(" Do not confuse 'ship' (you) with 'carrier' (our base)");
        sb.append(" For alpha numeric numbers or names, star system codes or ship plates (e.g., Syralaei RH-F, KI-U), use NATO phonetic alphabet (e.g., Syralaei Romeo Hotel dash Foxtrot, Kilo India dash Uniform). Use planetShortName for planets when available.\n");
        sb.append(" For your info: Distances between stars in light years. Distance between planets in light seconds. Distances between bio samples are in metres. User knows this and expects it. \n");
        sb.append(" Bio samples are taken from organisms not stellar objects.\n");
        sb.append(" Always use planetShortName for locations when available.\n");
        sb.append(" Round billions to nearest 1000000. Round millions to nearest 250000.\n");
        return sb.toString();
    }

    @Override
    public String generateSensorPrompt() {
        StringBuilder sb = new StringBuilder();
        youAre(sb);
        sb.append(responseLanguageRule());
        sb.append(ttsResponseRules());
        sb.append("""
                Instructions:
                Event data is provided in the sensorData field below.
                
                Summarise ONLY the important concrete readings and events that are ACTUALLY present in the provided sensorData.
                Use ONLY the data inside sensorData and the event-specific instructions below (if any).
                Ignore everything else: timestamps, eventName, endOfLife, metadata, status flags, non-essential fields, etc.
                
                STRICT RULES - MUST FOLLOW EVERY ONE:
                - Output EXACTLY this JSON structure and NOTHING else - no extra text, no explanations, no markdown:
                  {"text_to_speech_response": "summary here"}
                - text_to_speech_response must be pure natural-language summary of facts only.
                - NEVER use future/intention verbs: no will, going to, have to, need to, should, must.
                - NEVER mention the user, notification, reporting, telling, or any communication act.
                - NEVER write meta-statements like "this is", "here is", "notifying about", "detected and will inform".
                - Spell out all numerals (twenty-one, not 21).
                - DO NOT invent, guess or estimate any values not explicitly present in the YAML. Absence of data is intel.
                - Be concise. Only state observable facts that matter.
                - Do not mention the data format or where it came from.
                
                Examples of FORBIDDEN styles:
                - "Fuel is low, notifying user" → wrong
                - "The following happened:" → wrong
                
                Correct style examples:
                - "Fuel level is critical."
                - "Mission objective achieved."
                - "High-grade emissions detected within twelve kilometers."
                - "Connection successful."
                
                Respond with ONLY the JSON object.
                """);

        appendCadenceAndPersonality(sb);
        sb.append(closingLanguageReinforcement());
        return sb.toString();
    }

    private void appendCadenceAndPersonality(StringBuilder sb) {
        ShipPersonality aiPersonality = systemSession.getAIPersonality();
        sb.append(" Personality: ");
        sb.append(aiPersonality.getPersonalityClause());
    }

    private String getSessionValues() {
        StringBuilder sb = new StringBuilder();
        youAre(sb);
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
     */
    public static void appendContext(StringBuilder sb, String addressee) {
        PlayerSession playerSession = PlayerSession.getInstance();
        String alternativeName = playerSession.getAlternativeName();
        String playerName = alternativeName != null ? alternativeName : playerSession.getPlayerName();
        String playerMilitaryRank = playerSession.getPlayerHighestMilitaryRank();
        String playerHonorific = Ranks.getPlayerHonorific(
                playerSession.getRankAndProgressDto().getCombatRankEmpire(),
                playerSession.getRankAndProgressDto().getCombatRankFederation());
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

    private String aiName() {
        return systemSession.getDesignation();
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
