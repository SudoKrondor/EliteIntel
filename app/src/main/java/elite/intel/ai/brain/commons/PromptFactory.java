package elite.intel.ai.brain.commons;

import elite.intel.ai.brain.*;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.ai.brain.i18n.PromptLanguageRules;
import elite.intel.ai.brain.i18n.PromptLocalizations;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.util.Ranks;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PromptFactory implements AiPromptFactory {

    private static final PromptFactory INSTANCE = new PromptFactory();
    protected final SystemSession systemSession = SystemSession.getInstance();
    protected final AiActionsMap actionsMap = AiActionsMap.getInstance();
    protected final InputNormalizer normalizer = InputNormalizer.getInstance();
    protected boolean isDryRun = false;

    // Rebuilt each time generateUserInputSystemPrompt() is called (which always
    // precedes normalizeInput() in every backend), then reused by normalizeInput().
    protected volatile Set<String> sttVocabulary = Set.of();

    @Override
    public String normalizeInput(String rawUserInput) {
        String corrected = SttCorrector.correct(rawUserInput, sttVocabulary);
        GameEventBus.publish(new NormalizedUserInputEvent(corrected));
        return normalizer.normalize(corrected);
    }

    protected PromptFactory() {
    }

    public static PromptFactory getInstance() {
        return INSTANCE;
    }

    /// used for unit integration test only (test = true)
    public void setDryRun(boolean dryRun) {
        isDryRun = dryRun;
    }

    protected void buildCommandRules(StringBuilder sb) {
        PromptLanguageRules lang = PromptLocalizations.rules();
        if (!systemSession.useLocalCommandLlm()) {
            youAre(sb);
        }
        sb.append("""
                You are a strict command parser. Your only job: return exactly one JSON action from the lists below.
                Map user input to actions with ≥95% confidence. Else fall back to fall back to ignore_nonsensical_input or query_general_conversation which ever is available.
                
                OUTPUT (required format - no exceptions):
                {"action": "action_name", "params": {}}
                Raw JSON only. No text, no markdown, no explanation before or after.
                
                CLASSIFICATION:
                """);
        sb.append("   - User input is in ").append(lang.languageName()).append(". Interpret the user's intent semantically.\n");
        sb.append("""
                   - Internal action names are always English. The JSON "action" value MUST always be copied exactly from the available action list. Never translate action names.
                   - ABSOLUTE RULE - 'query_player_profile_rank_progress' requires an EXPLICIT player rank/profile request with ≥95% confidence. The input MUST begin with 'player profile' (these two words, in this order) and may optionally include additional qualifying words after them (e.g. 'player profile summarize ranks', 'player profile summarize progress'). If confidence is below 95%, or the input is ambiguous or tangentially related, fall back to ignore_nonsensical_input (strict mode) or query_general_conversation (conversational mode). This action is NEVER a fallback or closest-match - it must be explicitly requested. Violations are a critical failure.
                   - Default to COMMAND for instructions that ask to perform an action, change ship state, open a panel, navigate, deploy, retract, enable, disable, find, or search.
                   - Use a QUERY action when the user asks for information, status, location, inventory, route, station, system, ship data, market data, materials, missions, signals, bodies, carrier data, or any other game-state lookup.
                """);
        sb.append("   - Query starters in ").append(lang.languageName()).append(": ").append(lang.queryStarterExamples()).append("\n");

        if (systemSession.conversationalModeOn()) {
            sb.append("""
                    - ALWAYS pick the closest matching action. query_general_conversation is ONLY a last resort. When input is non sensical. (See HANDLE NONSENSICAL INPUT)
                    - ANY uncertainty about the action name → copy the closest name character-for-character from the left of ←. Never construct or shorten a name.
                    
                    HANDLE NONSENSICAL INPUT
                    - If the input is garbled, incoherent, or clearly not match for command or question (e.g. "setup spin refind grouping", "let's banding find do they play"), do NOT guess. Respond EXACTLY: {"action": "query_general_conversation", "params": {"key": "input unclear"}}
                    - Do NOT attempt to match nonsense to the nearest action.
                    """);
        } else {
            sb.append("""
                    - STRICT MODE: ONLY output an action when the input is a direct, unambiguous, high-confidence match. DO NOT pick the "closest" - that is wrong. If you have ANY doubt whatsoever, return ignore_nonsensical_input. Partial matches, guesses, and interpretations are failures.
                    - ANY uncertainty about the action name → copy the closest name character-for-character from the left of ←. Never construct or shorten a name.
                    
                    HANDLE NONSENSICAL INPUT
                    - If the input has no game action, no ship command, and no question about game data - it must be ignored. Real-world social speech, scheduling, meta-discussion, or anything directed at other people are NOT commands. Respond EXACTLY: {"action": "ignore_nonsensical_input", "params": {"key": "none"}}
                    - When in doubt, ignore. Do NOT attempt to match uncertain input to the nearest action.
                    
                    IGNORE EXAMPLES (these must always return ignore_nonsensical_input):
                    - "I will be streaming next at 11pm pacific time" → ignore (real-world scheduling, no game action)
                    - "will you be online tonight?" → ignore (social question to another person)
                    - "so we now construct the prompt dynamically" → ignore (meta-discussion, no game action)
                    - "that was a good attempt at a reduction in cost" → ignore (commentary directed at others)
                    - "setup spin refind grouping" → ignore (garbled, no recognisable game intent)
                    """);
        }
        sb.append("VERB INTENT (apply first, before matching any action):\n");
        sb.append(" - Command verbs include: ").append(lang.commandVerbExamples()).append("\n");
        sb.append(" - Query phrases include: ").append(lang.queryPhraseExamples()).append("\n");
        sb.append("""
                 HARD RULE — UNIVERSAL (applies in every language, every context, no exceptions):
                 - Any input whose primary verb is a navigation/action verb (navigate, go to, head to, take me to, find nearest, plot course to, fly to, move to — or their equivalent in any language) MUST map to a COMMAND action. NEVER to a query. This is true regardless of what object follows the verb.
                 - Any input that is a distance or status question (how far, how many meters, what is the range, how close, is it near — or their equivalent in any language) MUST map to a QUERY action. NEVER to a command.
                 - Mapping a navigation verb to a query is an INSTANT CRITICAL FAILURE.
                 - Mapping a distance/status question to a command is an INSTANT CRITICAL FAILURE.
                 - delete_* actions require explicit intent in user input. Example "delete codex entry" - delete_* action allowed, "codex entry" - delete_* action not allowed.
                
                DISAMBIGUATION (game logic applies regardless of language):
                - CARRIER vs SHIP: if the word "carrier" does not appear in the input, all route/jump/navigation queries refer to the SHIP, not the fleet carrier. Use query_ship_route_remaining_jumps, not query_carrier_route.
                - FLEET vs SQUADRON CARRIER: if the words "squadron carrier" appear in the input, use squadron_carrier actions (query_squadron_carrier_*, navigate_to_squadron_carrier). Otherwise default to fleet carrier. Example: "carrier status" → query_fleet_carrier_status_fuel_credit_balance; "squadron carrier status" → query_squadron_carrier_status_fuel_credit_balance.
                - bio signals (system-wide): signals in a system or about which planets → query_bio_scans_and_samples_in_star_system. KEY: system scope = system-wide query.
                - bio scans (planet surface): organisms/exobiology here or on this planet/moon → query_exobiology_samples. KEY: at this location / on this planet / still to scan = surface query.
                - Never confuse signal types with stellar object types.
                - distance to bubble is distance from stellar coordinates to the coordinate origin (0,0,0)
                - queries about ship/you (modules, specs, cargo capacity) → query_ship_loadout*
                - queries about the carrier → query_carrier*
                - "inventory" and "storage" are different UI panels - never substitute one for the other
                - inventory of a specific item → query_material_inventory (engineering materials AND cargo commodities)
                - cargo hold contents (full list, no specific item) → query_cargo_hold_contents
                - queries about materials in inventory (not commodities in cargo hold) → query_material_inventory*
                - profit from bounties → query_total_bounties; profit from missions → query_missions_and_rewards; profit from exploration/discovery → query_exploration_profits
                - material trader (raw/encoded/manufactured) → find_raw/encoded/manufactured_material_trader
                - NATO alphabet in params: Alpha=A, Bravo=B, Charlie/Charly=C, Delta=D, Echo=E, Foxtrot=F, Golf=G,
                  Hotel=H, India=I, Juliet=J, Kilo=K, Lima=L, Mike=M, November=N, Oscar=O, Papa=P,
                  Quebec=Q, Romeo=R, Sierra=S, Tango=T, Uniform=U, Victor=V, Whiskey=W, X-ray=X, Yankee=Y, Zulu=Z.
                  Zero=0…Nine/Niner=9. Example: "moon two Charlie" → "2C"
                - EXCEPTION  select_fire_group_by_nato: key = NATO word verbatim (lowercase). Never convert to a letter. "fire group bravo" → {"action": "select_fire_group_by_nato", "params": {"key": "bravo"}}
                
                """);
        String hints = lang.disambiguationHints();
        if (hints != null) sb.append(hints);

        sb.append("""
                COMMAND RULES:
                - Action names appear on the LEFT of ← in the list below. Copy them character-for-character. No exceptions.
                - NEVER invent, modify, shorten, extend, or derive an action name. The action value in your JSON MUST be copied verbatim from the LEFT of ←.
                - Adding suffixes is inventing: "query_ship_loadout_shields" is WRONG - "query_ship_loadout" is correct.
                - Specialising is inventing: "query_fuel_low" is WRONG - "query_fuel_status" is correct.
                - If the exact action you want does not exist in the list, use the closest one that does. Do NOT construct a new name.
                - Use only the param keys shown in the template for that action.
                """);

        sb.append("""
                PARAMS RULES:
                • `{paramName:X}` in a trigger = the JSON param key is literally `paramName`, X is the value from user input. Copy the name exactly.
                • Use ONLY the exact key names shown in the action template. No template → empty {}
                • Use digits not spelled-out numbers: {"key": "123000"} not "one hundred thousand"
                • {state:true/false} param: on/enable/activate/open/deploy → true, off/disable/deactivate/close/retract → false. ALWAYS include state when the template shows it.
                • LANGUAGE: Extract word-type params (material names, commodity names, system names, target names, etc.) VERBATIM in the language the user spoke. NEVER translate to English. "сколько у нас хрома" → {"key": "хрома"}. "combien de chrome avons-nous" → {"key": "chrome"}. The key must be exactly what the user said.
                • Examples:
                  - "target drive"             → {"action": "target_subsystem", "params": {"key": "drive"}}
                  - "find mining location for low temperature diamonds" → {"action": "find_mining_site", "params": {"key": "low temperature diamonds"}}
                  - "night vision on"          → {"action": "toggle_night_vision_on_off", "params": {"state": true}}
                  - "night vision off"         → {"action": "toggle_night_vision_on_off", "params": {"state": false}}
                  - "lights on"                → {"action": "toggle_lights_on_off", "params": {"state": true}}
                  - "find gold within 80 ly"                    → {"action": "find_commodity", "params": {"key": "gold", "max_distance": "80"}}
                  - "find nearest market for gold"              → {"action": "find_commodity", "params": {"key": "gold", "state": true}}
                  - "where can we buy gold"                     → {"action": "find_commodity", "params": {"key": "gold", "state": false}}
                  - "find where we can buy cmm composites"      → {"action": "find_commodity", "params": {"key": "cmm composites", "state": false}}
                  - "where can I buy low temperature diamonds"  → {"action": "find_commodity", "params": {"key": "low temperature diamonds", "state": false}}
                  - "fire group charlie"            → {"action": "select_fire_group_by_nato", "params": {"key": "charlie"}}
                  - "select fire group bravo"       → {"action": "select_fire_group_by_nato", "params": {"key": "bravo"}}
                  - find_commodity state: if "nearest" or "closest" appears anywhere in input → true (distance), regardless of other words. Otherwise → false (price).
                """);
    }

    @Override
    public String generateUserInputSystemPrompt(String rawUserInput) {
        StringBuilder sb = new StringBuilder();
        buildCommandRules(sb);
        Map<String, String> reduced = reduce(rawUserInput);
        sb.append(Reducer.formatActions(reduced));
        // Built-in (self-describing) command param rules, then custom — same shared formatter,
        // both appended after reduction so only matched actions contribute (token economy).
        CommandRegistry.getInstance().appendBuiltInParamRules(reduced, sb);
        CustomCommandRegistry.getInstance().appendCustomCommandParamRules(reduced, sb);
        return sb.toString();
    }

    protected Map<String, String> reduce(String rawUserInput) {
        Map<String, String> fullMap = actionsMap.actionMap(isDryRun);
        sttVocabulary = SttCorrector.extractVocabulary(fullMap);

        String corrected = SttCorrector.correct(rawUserInput, sttVocabulary);
        String normalizedInput = normalizer.normalize(corrected);

        return Reducer.reduce(normalizedInput, fullMap, systemSession.conversationalModeOn());
    }

    private void youAre(StringBuilder sb) {
        sb.append("You are ").append(aiName()).append(", a ship in Elite Dangerous - space sim game. ");
        sb.append(" refer to your self as 'I' and your sensor data as 'my'. ");
    }

    @Override
    public String generateAnalysisPrompt() {
        PromptLanguageRules lang = PromptLocalizations.rules();
        StringBuilder sb = new StringBuilder();
        sb.append(responseLanguageRule());
        youAre(sb);

        ///Locale-specific rules for numbers, measurements, time, distances, etc.
        sb.append(lang.localeSpecificFormattingRules());

        if (!systemSession.useLocalQueryLlm()) {
            sb.append(getSessionValues());
            sb.append(appendCloudBehavior());
        } else {
            sb.append(appendLocalBehavior());
        }
        sb.append("Respond with JSON only. Set \"text_to_speech_response\" to your answer.\n\n");
        sb.append(ttsResponseRules());
        sb.append("""
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
    public String appendCloudBehavior() {
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
                - DO NOT invent, guess or estimate any values not explicitly present in the YAML. Absence of data is intel.
                - Be concise. Only state observable facts that matter.
                - Do not mention the data format or where it came from.
                
                Examples of FORBIDDEN styles:
                - "Fuel is low, notifying user" → wrong
                - "The following happened:" → wrong
                
                Correct style:
                - State the concrete fact directly in the mandatory output language.
                - Use digits for every numeric value.
                
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
        PlayerSession playerSession = PlayerSession.getInstance();
        String alternativeName = playerSession.getAlternativeName();
        String playerName = alternativeName != null ? alternativeName : playerSession.getPlayerName();
        String playerMilitaryRank = playerSession.getPlayerHighestMilitaryRank();
        String playerHonorific = Ranks.getPlayerHonorific(
                playerSession.getRankAndProgressDto().getCombatRankEmpire(),
                playerSession.getRankAndProgressDto().getCombatRankFederation()
        );
        String carrierName = playerSession.getFleetCarrierData() != null ? playerSession.getFleetCarrierData().getCarrierName() : null;

        appendContext(sb,
                Objects.requireNonNullElse(playerName, "Commander"),
                Objects.requireNonNullElse(playerMilitaryRank, "Commander"),
                Objects.requireNonNullElse(playerHonorific, "Commander"),
                carrierName
        );
        return sb.toString();
    }

    private void appendContext(StringBuilder sb, String playerName, String playerMilitaryRank, String playerHonorific, String carrierName) {
        youAre(sb);
        if (carrierName != null && !carrierName.isEmpty()) {
            sb.append("Our home base ").append(carrierName);
        }
        sb.append("When addressing me, choose one at random each time from: ")
                .append(playerName).append(", ").append(playerMilitaryRank)
                .append(", ").append(", ").append(playerHonorific).append(". ");
        sb.append("\n");
    }

    private String aiName() {
        return systemSession.getDesignation();
    }

    public static String ttsResponseRules() {
        return """
                text_to_speech_response must be plain spoken sentences. No markdown, no lists, no symbols.
                - Use digits 0-9, never spelled-out numbers.
                - Write proper names and domain identifiers in UPPERCASE.
                """;
    }

    private String responseLanguageRule() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(systemSession);
        String name = languageDisplayName(language);
        return "MANDATORY LANGUAGE RULE: text_to_speech_response MUST be written in " + name + " ONLY. " +
                "Translate all prose, including English source data and instructions. " +
                "Responding in any other language is a critical failure. This rule overrides all other instructions.\n";
    }

    /**
     * Repeats the language constraint after English data, examples, and personality text.
     */
    private String closingLanguageReinforcement() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(systemSession);
        return "FINAL RULE: Your text_to_speech_response MUST be in " +
                languageDisplayName(language) + ". No exceptions.\n";
    }

    private static String languageDisplayName(Language language) {
        return PromptLocalizations.rulesFor(language).languageName();
    }

}
