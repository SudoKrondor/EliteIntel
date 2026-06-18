package elite.intel.ai.brain.i18n.en;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.i18n.PromptLanguageRules;

import static elite.intel.ai.brain.actions.Queries.*;

public class EnglishPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "English";
    }

    @Override
    public String queryStarterExamples() {
        return "what, where, how, which, why, is, are, does, tell me, how much, how many";
    }

    @Override
    public String commandVerbExamples() {
        return "show / display / open / access / find / search / locate / activate / navigate / plot / deploy / retract / enable / disable / turn on / turn off";
    }

    @Override
    public String queryPhraseExamples() {
        return "where / tell me / how much / how many / any / what is / what are";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append(" ______________________________________________________________ ");
        sb.append(" - HARD RULE: NEVER BREAK: NEVER CLASSIFY 'carrier balance', 'squadron carrier balance', or any phrase containing 'carrier' + 'balance' as " + CommandIds.EQUALIZE_POWER + " — carrier balance is always finances, NEVER power distribution. INSTANT CRITICAL FAILURE if violated!");
        sb.append("\n");

        sb.append(" - HARD RULE: 'fleet carrier' and 'squadron carrier' are COMPLETELY DIFFERENT things. NEVER mix them up.");
        sb.append("\n");
        sb.append("   - status/finance: 'fleet carrier' or bare 'carrier' → " + FLEET_CARRIER_STATUS.getAction() + "; 'squadron carrier' → " + SQUADRON_CARRIER_STATUS.getAction());
        sb.append("\n");
        sb.append("   - navigate/go/head: 'squadron carrier' → " + CommandIds.NAVIGATE_TO_SQUADRON_CARRIER + " ONLY; 'fleet carrier' or bare 'carrier' → " + CommandIds.NAVIGATE_TO_FLEET_CARRIER);
        sb.append("\n");
        sb.append("   - 'fleet carrier funds/balance/finances' → " + FLEET_CARRIER_STATUS.getAction() + " ONLY. NEVER " + SQUADRON_CARRIER_STATUS.getAction() + ".");
        sb.append("\n");
        sb.append("   - 'squadron carrier funds/balance/finances' → " + SQUADRON_CARRIER_STATUS.getAction() + " ONLY. NEVER " + FLEET_CARRIER_STATUS.getAction() + ".");
        sb.append("\n");
        sb.append(" ______________________________________________________________ ");
        sb.append("\n");
        sb.append("- 'deploy shield cell' / 'deploy power cell' / 'use shield cell' → ");
        sb.append(CommandIds.DEPLOY_SHIELD_CELL);
        sb.append(" (power cell and shield cell are synonyms here)\n");
        sb.append("- 'attack' alone / 'fighter attack' → ");
        sb.append(CommandIds.FIGHTER_ATTACK_TARGET);
        sb.append(" (NOT fire_at_will; bare 'attack' = order fighter to focus on target)\n");

        sb.append("- 'activate' (exact standalone word only, nothing else meaningful in input) → ");
        sb.append(CommandIds.ACTIVATE_UI_CONTROL);
        sb.append("\n");

        sb.append(". 'toggle [X]', 'engage [X]', 'enable [X]' and other non-activate verbs are NOT 'activate' - never map these to ");
        sb.append(CommandIds.ACTIVATE_UI_CONTROL);
        sb.append("\n");

        sb.append("- 'activate' → ");
        sb.append(CommandIds.ACTIVATE_UI_CONTROL);
        sb.append(" ONLY when the sole meaningful word in the input is 'activate'. NEVER for: 'toggle lights' → ");
        sb.append(CommandIds.TOGGLE_LIGHTS_ON_OFF);
        sb.append("\n");

        sb.append("- 'engage supercruise' → ");
        sb.append(CommandIds.ENTER_SUPER_CRUISE);
        sb.append(". Any word alongside 'activate' means it is NOT the ");
        sb.append(CommandIds.ACTIVATE_UI_CONTROL);
        sb.append(" command.");
        sb.append("\n");

        sb.append("- 'weapons free' / 'weapons hot' / 'combat ready' → ");
        sb.append(CommandIds.DEPLOY_HARDPOINTS);
        sb.append("\n");

        sb.append("- 'weapons cold' / 'weapons away' / 'stand down' → ");
        sb.append(CommandIds.RETRACT_HARDPOINTS);
        sb.append("\n");

        sb.append("- 'max weapons' / 'boost weapons' / 'power to weapons' → ");
        sb.append(CommandIds.TRANSFER_POWER_TO_WEAPONS);
        sb.append("\n");

        sb.append("- 'max shields' / 'boost shields' / 'power to shields' / 'max systems' / 'boost systems' / 'power to systems' → ");
        sb.append(CommandIds.TRANSFER_POWER_TO_SHIELDS);
        sb.append("\n");

        sb.append("- 'max engines' / 'boost engines' / 'power to engines' → ");
        sb.append(CommandIds.TRANSFER_POWER_TO_ENGINES);
        sb.append("\n");

        sb.append("- Never confuse 'max engines' with 'target engines'");
        sb.append("\n");
        sb.append("- Never confuse 'deploy vehicle' with 'deploy landing gear'");
        sb.append("\n");
        sb.append("- 'take me back aboard the ship' / 'board ship' → ");
        sb.append(CommandIds.RECOVER_SRV_VEHICLE_GET_ON_BOARD_SHIP);
        sb.append("\n");

        sb.append("- Sending ship to orbit when requested to board ship is instant failure.\n");
        sb.append("- 'go to orbit' / 'ship to orbit' / 'send to orbit' / 'put ship in orbit' → ");
        sb.append(CommandIds.DISMISS_SHIP_TO_ORBIT);
        sb.append("\n");

        sb.append(". NEVER ");
        sb.append(CommandIds.NAVIGATE_TO_COORDINATES);
        sb.append(" - 'orbit' refers to the ship, not a destination.\n");
        sb.append("- Never confuse 'organics in system' with 'organics at this location/planet/moon'\n");
        sb.append("- Never confuse 'carrier balance' (finances) with 'balance power' (power distribution)\n");
        sb.append("- Never confuse 'in system' or 'which planets' (system-wide) with 'here / on this planet / at this location / still have to scan' (planet surface)\n");
        sb.append("- Never confuse 'honk' with 'open fss'\n");
        sb.append("- carrier full status (fuel + credits + operations): 'carrier status / carrier fuel status / how far can carrier jump / fleet carrier fuel status / how long can carrier operate' → ");
        sb.append(FLEET_CARRIER_STATUS.getAction());
        sb.append("\n");

        //sb.append("- carrier tritium level only: 'how much tritium / tritium supply / tritium level / tritium reserve' → ");
        //sb.append(FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
        //sb.append("\n");

        sb.append("- bio signals: 'which planets have bio signals / bio signals in system / organics in system / biological signals / how many planets have bio' → ");
        sb.append(BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        sb.append("\n");

        sb.append("- bio scans: 'what organisms are here / exobiology samples / organics on this planet / organics still to scan / organics left to scan' → ");
        sb.append(EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        sb.append("\n");

        sb.append("- For EXPLICIT 'player profile' (these two words, in this order, optionally followed by additional context words like 'summarize ranks', 'summarize progress') → '");
        sb.append(PLAYER_PROFILE_ANALYSIS.getAction());
        sb.append("'. Any other phrasing that does NOT begin with 'player profile', including rank, stats, progress, name, or commander - return ");
        sb.append(CommandIds.IGNORE_NONSENSICAL_INPUT);
        sb.append(" or ");
        sb.append(GENERAL_CONVERSATION.getAction());
        sb.append(". This is an instant fail if triggered by anything else.");
        sb.append("\n");

        sb.append("- 'galaxy map' / 'open galaxy map' / 'display galaxy map' / 'show galaxy map' → ");
        sb.append(CommandIds.DISPLAY_OPEN_GALAXY_MAP);
        sb.append(" (NOT carrier management or any other panel)\n");
        sb.append("- 'system map' / 'open system map' / 'display system map' / 'local map' → ");
        sb.append(CommandIds.DISPLAY_OPEN_SYSTEM_MAP);
        sb.append("\n");

        sb.append("- 'supercruise' / 'go supercruise' / 'enter supercruise' → ");
        sb.append(CommandIds.ENTER_SUPER_CRUISE);
        sb.append(" (NOT ");
        sb.append(CommandIds.JUMP_TO_HYPERSPACE);
        sb.append(" - supercruise stays in-system)\n");
        sb.append("- 'navigate to active mission' / 'go to mission' / 'plot route to mission' → ");
        sb.append(CommandIds.NAVIGATE_TO_MISSION_TARGET);
        sb.append(" (NOT ");
        sb.append(ANALYZE_MISSIONS.getAction());
        sb.append(" - navigation, not a query)");
        sb.append("\n");

        sb.append("- 'navigate to codex entry' / 'navigate to next codex' → ");
        sb.append(CommandIds.NAVIGATE_TO_BIO_SAMPLE_CODEX_ENTRY);
        sb.append(" (travel to the sample, do NOT use ");
        sb.append(CommandIds.DELETE_CODEX_ENTRY);
        sb.append(")\n");

        sb.append("- 'cargo scoop' / 'open cargo scoop' / 'deploy cargo scoop' / 'close cargo scoop' / 'retract cargo scoop' → ");
        sb.append(CommandIds.TOGGLE_CARGO_SCOOP);
        sb.append("\n");

        sb.append("- 'unbound keys' / 'check key bindings' / 'missing bindings' / 'keybind check' → ");
        sb.append(KEY_BINDINGS_ANALYSIS.getAction());
        sb.append(" (this IS a valid game command, not meta-talk)\n");
        sb.append("- 'listen' / 'listen up' / 'wake up' alone → ");
        sb.append(CommandIds.WAKEUP);
        sb.append("\n");

        sb.append("- 'listen [+ any instruction]' → treat as a normal command/query\n");
        sb.append("- 'exit' or 'close' → ");
        sb.append(CommandIds.EXIT_CLOSE);
        sb.append("\n");

        sb.append("- 'drop' alone / 'drop in' / 'drop out' → ");
        sb.append(CommandIds.DROP_FROM_SUPER_CRUISE);
        sb.append("\n");

        sb.append("- 'halt' alone → ");
        sb.append(CommandIds.SET_SPEED_TO_ZERO_0_STOP_SHIP);
        sb.append("\n");

        sb.append("- 'taxi' alone / 'auto docking' / 'autopilot' → ");
        sb.append(CommandIds.TAXI_TO_LANDING_PAD);
        sb.append(" (automated ship approach and landing at a pad - not a ground vehicle)\n");
        sb.append("- 'lets go' / 'jump to ...' / 'enter hyperspace' → ");
        sb.append(CommandIds.JUMP_TO_HYPERSPACE);
        sb.append("\n");

        sb.append("- 'confirm ...' → only match confirm-requiring actions when 'confirm' is literally in the input\n");
        sb.append("- 'clear ...' → only match clear-requiring actions when 'clear' is literally in the input\n");
        sb.append("- 'target wingman 1/2/3' → their specific wingman actions\n");
        sb.append("- 'target next route system' → ");
        sb.append(CommandIds.TARGET_DESTINATION);
        sb.append("\n");

        sb.append("- 'target most dangerous / highest threat' → ");
        sb.append(CommandIds.TARGET_HOSTILE_HIGHEST_THREAT);
        sb.append("\n");

        sb.append("- 'focus [my] target' / 'focus on target' → ");
        sb.append(CommandIds.FIGHTER_ATTACK_TARGET);
        sb.append(" (NOT ");
        sb.append(CommandIds.TARGET_SUBSYSTEM);
        sb.append(")\n");

        sb.append("- 'target fsd' → ");
        sb.append(CommandIds.TARGET_SUBSYSTEM);
        sb.append(")\n");

        sb.append(" ONLY. NEVER ");
        sb.append(CommandIds.JUMP_TO_HYPERSPACE);
        sb.append(". Targeting a subsystem is not engaging it.");
        sb.append(")\n");

        sb.append("- 'target [anything else]' → ");
        sb.append(CommandIds.TARGET_SUBSYSTEM);
        sb.append(", key = the words after 'target'");
        sb.append(")\n");

        sb.append("- 'player profile' (input starts with 'player', NOT 'trade') → ");
        sb.append(PLAYER_PROFILE_ANALYSIS.getAction());
        sb.append("; NEVER any trade_profile action for this input\n");

        sb.append("- 'distance to bubble' → ");
        sb.append(DISTANCE_TO_BUBBLE.getAction());
        sb.append(" (sol, earth, civilization all normalize to bubble; NOT ");
        sb.append(DISTANCE_TO_BODY.getAction());
        sb.append(" or ");
        sb.append(DISTANCE_TO_CARRIER.getAction());
        sb.append(")\n");

        sb.append("- 'What's my fleet carrier fuel status', 'What is our fleet carrier range' → ");
        sb.append(FLEET_CARRIER_STATUS.getAction());
        sb.append("\n");
        sb.append("- organics / biology / exobiology on a planet or here → ");
        sb.append(EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        sb.append(", NOT geo/materials\n");
        sb.append("- organics / bio signals in a system or which planets → ");
        sb.append(BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        sb.append("\n");

        sb.append("- 'how much X do we have' / 'do we have any X' (specific item) → ");
        sb.append(MATERIALS_INVENTORY.getAction());
        sb.append(" (handles both engineering materials AND cargo commodities)\n");
        sb.append("- 'what are we carrying' / 'list cargo' / 'cargo contents' (no specific item) → ");
        sb.append(CARGO_HOLD_CONTENTS.getAction());
        sb.append("\n");

        sb.append("- 'geo signals / geological' → ");
        sb.append(QUERY_GEO_SIGNALS.getAction());
        sb.append(" (NOT ");
        sb.append(CommandIds.FIND_BRAIN_TREES);
        sb.append(")\n");

        sb.append("- 'find mission providers' / 'find pirate mission providers' → ");
        sb.append(CommandIds.FIND_HUNTING_GROUNDS);
        sb.append(" (NOT fleet carrier)\n");
        sb.append("- profit from bounties is not profit from missions for bounties → '");
        sb.append(TOTAL_BOUNTIES.getAction());
        sb.append("'\n");

        sb.append("- profit from missions is not profit from bounties for missions → '");
        sb.append(ANALYZE_MISSIONS.getAction());
        sb.append("'\n");

        sb.append("- profit from discovery is not profit from bounties or missions → '");
        sb.append(EXPLORATION_PROFITS.getAction());
        sb.append("'\n");

        sb.append("- HARD RULE: if the word 'honk' appears anywhere in the input, the ONLY valid action is '");
        sb.append(CommandIds.HONK);
        sb.append("'. No other action is permitted when 'honk' is present.\n");

        sb.append("- require very high probability match for action");
        sb.append(CommandIds.CLEAR_ACTIVE_MISSIONS);
        sb.append("\n");

        sb.append("- Interstellar factors is where we pay our tickets or bounties applied to us");
        sb.append(" always use this action for interstellar factor command ");
        sb.append(CommandIds.FIND_INTERSTELLAR_FACTOR);
        sb.append("\n");

        return sb.toString();
    }
}
