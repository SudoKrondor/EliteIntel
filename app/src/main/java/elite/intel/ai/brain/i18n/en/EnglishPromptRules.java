package elite.intel.ai.brain.i18n.en;

import elite.intel.ai.brain.i18n.PromptLanguageRules;

import static elite.intel.ai.brain.actions.Commands.*;
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
        sb.append("- 'activate' (exact standalone word only, nothing else meaningful in input) → ");
        sb.append(ACTIVATE.getAction());
        sb.append("\n");

        sb.append(". 'toggle [X]', 'engage [X]', 'enable [X]' and other non-activate verbs are NOT 'activate' - never map these to ");
        sb.append(ACTIVATE.getAction());
        sb.append("\n");

        sb.append("- 'activate' → ");
        sb.append(ACTIVATE.getAction());
        sb.append(" ONLY when the sole meaningful word in the input is 'activate'. NEVER for: 'toggle lights' → ");
        sb.append(LIGHTS_ON_OFF.getAction());
        sb.append("\n");

        sb.append("- 'engage supercruise' → ");
        sb.append(ENTER_SUPER_CRUISE.getAction());
        sb.append(". Any word alongside 'activate' means it is NOT the ");
        sb.append(ACTIVATE.getAction());
        sb.append(" command.");
        sb.append("\n");

        sb.append("- 'weapons free' / 'weapons hot' / 'combat ready' → ");
        sb.append(DEPLOY_HARDPOINTS.getAction());
        sb.append("\n");

        sb.append("- 'weapons cold' / 'weapons away' / 'stand down' → ");
        sb.append(RETRACT_HARDPOINTS.getAction());
        sb.append("\n");

        sb.append("- 'max weapons' / 'boost weapons' / 'power to weapons' → ");
        sb.append(INCREASE_WEAPONS_POWER.getAction());
        sb.append("\n");

        sb.append("- 'max shields' / 'boost shields' / 'power to shields' / 'max systems' / 'boost systems' / 'power to systems' → ");
        sb.append(INCREASE_SHIELDS_POWER.getAction());
        sb.append("\n");

        sb.append("- 'max engines' / 'boost engines' / 'power to engines' → ");
        sb.append(INCREASE_ENGINES_POWER.getAction());
        sb.append("\n");

        sb.append("- Never confuse 'max engines' with 'target engines'");
        sb.append("\n");
        sb.append("- Never confuse 'deploy vehicle' with 'deploy landing gear'");
        sb.append("\n");
        sb.append("- 'take me back aboard the ship' / 'board ship' → ");
        sb.append(RECOVER_SRV.getAction());
        sb.append("\n");

        sb.append("- Sending ship to orbit when requested to board ship is instant failure.\n");
        sb.append("- 'go to orbit' / 'ship to orbit' / 'send to orbit' / 'put ship in orbit' → ");
        sb.append(DISMISS_SHIP.getAction());
        sb.append("\n");

        sb.append(". NEVER ");
        sb.append(NAVIGATE_TO_TARGET.getAction());
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
        sb.append(IGNORE_NONSENSE.getAction());
        sb.append(" or ");
        sb.append(GENERAL_CONVERSATION.getAction());
        sb.append(". This is an instant fail if triggered by anything else.");
        sb.append("\n");

        sb.append("- 'galaxy map' / 'open galaxy map' / 'display galaxy map' / 'show galaxy map' → ");
        sb.append(OPEN_GALAXY_MAP.getAction());
        sb.append(" (NOT carrier management or any other panel)\n");
        sb.append("- 'system map' / 'open system map' / 'display system map' / 'local map' → ");
        sb.append(OPEN_SYSTEM_MAP.getAction());
        sb.append("\n");

        sb.append("- 'supercruise' / 'go supercruise' / 'enter supercruise' → ");
        sb.append(ENTER_SUPER_CRUISE.getAction());
        sb.append(" (NOT ");
        sb.append(JUMP_TO_HYPERSPACE.getAction());
        sb.append(" - supercruise stays in-system)\n");
        sb.append("- 'navigate to active mission' / 'go to mission' / 'plot route to mission' → ");
        sb.append(NAVIGATE_TO_NEXT_MISSION.getAction());
        sb.append(" (NOT ");
        sb.append(ANALYZE_MISSIONS.getAction());
        sb.append(" - navigation, not a query)");
        sb.append("\n");

        sb.append("- 'navigate to codex entry' / 'navigate to next codex' → ");
        sb.append(NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
        sb.append(" (travel to the sample, do NOT use ");
        sb.append(DELETE_CODEX_ENTRY.getAction());
        sb.append(")\n");

        sb.append("- 'cargo scoop' / 'open cargo scoop' / 'deploy cargo scoop' / 'close cargo scoop' / 'retract cargo scoop' → ");
        sb.append(TOGGLE_CARGO_SCOOP.getAction());
        sb.append("\n");

        sb.append("- 'unbound keys' / 'check key bindings' / 'missing bindings' / 'keybind check' → ");
        sb.append(KEY_BINDINGS_ANALYSIS.getAction());
        sb.append(" (this IS a valid game command, not meta-talk)\n");
        sb.append("- 'listen' / 'listen up' / 'wake up' alone → ");
        sb.append(WAKEUP.getAction());
        sb.append("\n");

        sb.append("- 'listen [+ any instruction]' → treat as a normal command/query\n");
        sb.append("- 'exit' or 'close' → ");
        sb.append(EXIT_CLOSE.getAction());
        sb.append("\n");

        sb.append("- 'drop' alone / 'drop in' / 'drop out' → ");
        sb.append(DROP_FROM_SUPER_CRUISE.getAction());
        sb.append("\n");

        sb.append("- 'halt' alone → ");
        sb.append(SET_SPEED_ZERO.getAction());
        sb.append("\n");

        sb.append("- 'taxi' alone / 'auto docking' / 'autopilot' → ");
        sb.append(TAXI.getAction());
        sb.append(" (automated ship approach and landing at a pad - not a ground vehicle)\n");
        sb.append("- 'lets go' / 'jump to ...' / 'enter hyperspace' → ");
        sb.append(JUMP_TO_HYPERSPACE.getAction());
        sb.append("\n");

        sb.append("- 'confirm ...' → only match confirm-requiring actions when 'confirm' is literally in the input\n");
        sb.append("- 'clear ...' → only match clear-requiring actions when 'clear' is literally in the input\n");
        sb.append("- 'target wingman 1/2/3' → their specific wingman actions\n");
        sb.append("- 'target next route system' → ");
        sb.append(TARGET_DESTINATION.getAction());
        sb.append("\n");

        sb.append("- 'target most dangerous / highest threat' → ");
        sb.append(SELECT_HIGHEST_THREAT.getAction());
        sb.append("\n");

        sb.append("- 'focus [my] target' / 'focus on target' → ");
        sb.append(FIGHTER_REQUEST_FOCUS_TARGET.getAction());
        sb.append(" (NOT ");
        sb.append(TARGET_SUB_SYSTEM.getAction());
        sb.append(")\n");

        sb.append("- 'target fsd' → ");
        sb.append(TARGET_SUB_SYSTEM.getAction());
        sb.append(")\n");

        sb.append(" ONLY. NEVER ");
        sb.append(JUMP_TO_HYPERSPACE.getAction());
        sb.append(". Targeting a subsystem is not engaging it.");
        sb.append(")\n");

        sb.append("- 'target [anything else]' → ");
        sb.append(TARGET_SUB_SYSTEM.getAction());
        sb.append(", key = the words after 'target'");

        sb.append(")\n");
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
        sb.append(FIND_BRAIN_TREES.getAction());
        sb.append(")\n");

        sb.append("- 'find mission providers' / 'find pirate mission providers' → ");
        sb.append(FIND_HUNTING_GROUNDS.getAction());
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
        sb.append(HONK_THE_SYSTEM.getAction());
        sb.append("'. No other action is permitted when 'honk' is present.\n");

        sb.append("- require very high probability match for action");
        sb.append(CLEAR_ALL_ACTIVE_MISSIONS.getAction());
        sb.append("\n");

        sb.append("- Interstellar factors is where we pay our tickets or bounties applied to us");
        sb.append(" always use this action for interstellar factor command ");
        sb.append(FIND_INTERSTELLAR_FACTOR.getAction());
        sb.append("\n");

        return sb.toString();
    }
}
