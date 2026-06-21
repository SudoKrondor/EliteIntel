package elite.intel.ai.brain.i18n.en;

import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;
import elite.intel.ai.brain.i18n.PromptLanguageRules;

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
        sb.append(" - HARD RULE: NEVER BREAK: NEVER CLASSIFY 'carrier balance', 'squadron carrier balance', or any phrase containing 'carrier' + 'balance' as " + EqualizePowerCommand.ID + " — carrier balance is always finances, NEVER power distribution. INSTANT CRITICAL FAILURE if violated!");
        sb.append("\n");

        sb.append(" - HARD RULE: 'fleet carrier' and 'squadron carrier' are COMPLETELY DIFFERENT things. NEVER mix them up.");
        sb.append("\n");
        sb.append("   - status/finance: 'fleet carrier' or bare 'carrier' → " + AnalyzeFleetCarrierDataQueryCommand.ID + "; 'squadron carrier' → " + AnalyzeSquadronCarrierDataQueryCommand.ID);
        sb.append("\n");
        sb.append("   - navigate/go/head: 'squadron carrier' → " + NavigateToSquadronCarrierCommand.ID + " ONLY; 'fleet carrier' or bare 'carrier' → " + NavigateToFleetCarrierCommand.ID);
        sb.append("\n");
        sb.append("   - 'fleet carrier funds/balance/finances' → " + AnalyzeFleetCarrierDataQueryCommand.ID + " ONLY. NEVER " + AnalyzeSquadronCarrierDataQueryCommand.ID + ".");
        sb.append("\n");
        sb.append("   - 'squadron carrier funds/balance/finances' → " + AnalyzeSquadronCarrierDataQueryCommand.ID + " ONLY. NEVER " + AnalyzeFleetCarrierDataQueryCommand.ID + ".");
        sb.append("\n");
        sb.append(" ______________________________________________________________ ");
        sb.append("\n");
        sb.append("- 'deploy shield cell' / 'deploy power cell' / 'use shield cell' → ");
        sb.append(DeployShieldCellCommand.ID);
        sb.append(" (power cell and shield cell are synonyms here)\n");
        sb.append("- 'attack' alone / 'fighter attack' → ");
        sb.append(FighterAttackTargetCommand.ID);
        sb.append(" (NOT fire_at_will; bare 'attack' = order fighter to focus on target)\n");

        sb.append("- 'activate' (exact standalone word only, nothing else meaningful in input) → ");
        sb.append(ActivateUiControlCommand.ID);
        sb.append("\n");

        sb.append(". 'toggle [X]', 'engage [X]', 'enable [X]' and other non-activate verbs are NOT 'activate' - never map these to ");
        sb.append(ActivateUiControlCommand.ID);
        sb.append("\n");

        sb.append("- 'activate' → ");
        sb.append(ActivateUiControlCommand.ID);
        sb.append(" ONLY when the sole meaningful word in the input is 'activate'. NEVER for: 'toggle lights' → ");
        sb.append(ToggleLightsOnOffCommand.ID);
        sb.append("\n");

        sb.append("- 'engage supercruise' → ");
        sb.append(EnterSuperCruiseCommand.ID);
        sb.append(". Any word alongside 'activate' means it is NOT the ");
        sb.append(ActivateUiControlCommand.ID);
        sb.append(" command.");
        sb.append("\n");

        sb.append("- 'weapons free' / 'weapons hot' / 'combat ready' → ");
        sb.append(DeployHardpointsCommand.ID);
        sb.append("\n");

        sb.append("- 'weapons cold' / 'weapons away' / 'stand down' → ");
        sb.append(RetractHardpointsCommand.ID);
        sb.append("\n");

        sb.append("- 'max weapons' / 'boost weapons' / 'power to weapons' → ");
        sb.append(TransferPowerToWeaponsCommand.ID);
        sb.append("\n");

        sb.append("- 'max shields' / 'boost shields' / 'power to shields' / 'max systems' / 'boost systems' / 'power to systems' → ");
        sb.append(TransferPowerToShieldsCommand.ID);
        sb.append("\n");

        sb.append("- 'max engines' / 'boost engines' / 'power to engines' → ");
        sb.append(TransferPowerToEnginesCommand.ID);
        sb.append("\n");

        sb.append("- Never confuse 'max engines' with 'target engines'");
        sb.append("\n");
        sb.append("- Never confuse 'deploy vehicle' with 'deploy landing gear'");
        sb.append("\n");
        sb.append("- 'take me back aboard the ship' / 'board ship' → ");
        sb.append(RecoverSrvVehicleGetOnBoardShipCommand.ID);
        sb.append("\n");

        sb.append("- Sending ship to orbit when requested to board ship is instant failure.\n");
        sb.append("- 'go to orbit' / 'ship to orbit' / 'send to orbit' / 'put ship in orbit' → ");
        sb.append(DismissShipToOrbitCommand.ID);
        sb.append("\n");

        sb.append(". NEVER ");
        sb.append(NavigateToCoordinatesCommand.ID);
        sb.append(" - 'orbit' refers to the ship, not a destination.\n");
        sb.append("- Never confuse 'organics in system' with 'organics at this location/planet/moon'\n");
        sb.append("- Never confuse 'carrier balance' (finances) with 'balance power' (power distribution)\n");
        sb.append("- Never confuse 'in system' or 'which planets' (system-wide) with 'here / on this planet / at this location / still have to scan' (planet surface)\n");
        sb.append("- Never confuse 'honk' with 'open fss'\n");
        sb.append("- carrier full status (fuel + credits + operations): 'carrier status / carrier fuel status / how far can carrier jump / fleet carrier fuel status / how long can carrier operate' → ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append("\n");

        //sb.append("- carrier tritium level only: 'how much tritium / tritium supply / tritium level / tritium reserve' → ");
        //sb.append(FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
        //sb.append("\n");

        sb.append("- bio signals: 'which planets have bio signals / bio signals in system / organics in system / biological signals / how many planets have bio' → ");
        sb.append(AnalyzeBioScansStarSystemQueryCommand.ID);
        sb.append("\n");

        sb.append("- bio scans: 'what organisms are here / exobiology samples / organics on this planet / organics still to scan / organics left to scan' → ");
        sb.append(AnalyzeBioSamplesPlanetSurfaceQueryCommand.ID);
        sb.append("\n");

        sb.append("- For EXPLICIT 'player profile' (these two words, in this order, optionally followed by additional context words like 'summarize ranks', 'summarize progress') → '");
        sb.append(AnalyzePlayerProfileQueryCommand.ID);
        sb.append("'. Any other phrasing that does NOT begin with 'player profile', including rank, stats, progress, name, or commander - return ");
        sb.append(IgnoreNonsensicalInputCommand.ID);
        sb.append(" or ");
        sb.append(GeneralConversationQueryCommand.ID);
        sb.append(". This is an instant fail if triggered by anything else.");
        sb.append("\n");

        sb.append("- 'galaxy map' / 'open galaxy map' / 'display galaxy map' / 'show galaxy map' → ");
        sb.append(DisplayOpenGalaxyMapCommand.ID);
        sb.append(" (NOT carrier management or any other panel)\n");
        sb.append("- 'system map' / 'open system map' / 'display system map' / 'local map' → ");
        sb.append(DisplayOpenSystemMapCommand.ID);
        sb.append("\n");

        sb.append("- 'supercruise' / 'go supercruise' / 'enter supercruise' → ");
        sb.append(EnterSuperCruiseCommand.ID);
        sb.append(" (NOT ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append(" - supercruise stays in-system)\n");
        sb.append("- 'navigate to active mission' / 'go to mission' / 'plot route to mission' → ");
        sb.append(NavigateToMissionTargetCommand.ID);
        sb.append(" (NOT ");
        sb.append(AnalyzeMissionQueryCommand.ID);
        sb.append(" - navigation, not a query)");
        sb.append("\n");

        sb.append("- 'navigate to codex entry' / 'navigate to next codex' → ");
        sb.append(NavigateToBioSampleCodexEntryCommand.ID);
        sb.append(" (travel to the sample, do NOT use ");
        sb.append(DeleteCodexEntryCommand.ID);
        sb.append(")\n");

        sb.append(" ______________________________________________________________ ");
        sb.append("\n");
        sb.append(" - HARD RULE: NEVER BREAK: Any intent to NAVIGATE, GO TO, HEAD TO, or FIND the nearest/next bio sample or codex entry MUST use ");
        sb.append(NavigateToBioSampleCodexEntryCommand.ID);
        sb.append(". NEVER use query_distance_to_bio_sample for navigation intent.");
        sb.append(" 'navigate' is a command verb (action), NOT a question. 'how far' / 'how far are we from' is a distance question → use query_distance_to_bio_sample.");
        sb.append(" INSTANT CRITICAL FAILURE if you use query_distance_to_bio_sample when the user says navigate/go to/head to/take me to/find nearest bio sample or codex entry.");
        sb.append("\n");
        sb.append(" ______________________________________________________________ ");
        sb.append("\n");

        sb.append("- 'cargo scoop' / 'open cargo scoop' / 'deploy cargo scoop' / 'close cargo scoop' / 'retract cargo scoop' → ");
        sb.append(ToggleCargoScoopCommand.ID);
        sb.append("\n");

        sb.append("- 'unbound keys' / 'check key bindings' / 'missing bindings' / 'keybind check' → ");
        sb.append(AnalyzeMisingKeyBindingQueryCommand.ID);
        sb.append(" (this IS a valid game command, not meta-talk)\n");
        sb.append("- 'listen' / 'listen up' / 'wake up' alone → ");
        sb.append(WakeupCommand.ID);
        sb.append("\n");

        sb.append("- 'listen [+ any instruction]' → treat as a normal command/query\n");
        sb.append("- 'exit' or 'close' → ");
        sb.append(ExitCloseCommand.ID);
        sb.append("\n");

        sb.append("- 'drop' alone / 'drop in' / 'drop out' → ");
        sb.append(DropFromSuperCruiseCommand.ID);
        sb.append("\n");

        sb.append("- 'halt' alone → ");
        sb.append(SetSpeedZeroCommand.ID);
        sb.append("\n");

        sb.append("- 'taxi' alone / 'auto docking' / 'autopilot' → ");
        sb.append(TaxiToLandingPadCommand.ID);
        sb.append(" (automated ship approach and landing at a pad - not a ground vehicle)\n");
        sb.append("- 'lets go' / 'jump to ...' / 'enter hyperspace' → ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append("- 'confirm ...' → only match confirm-requiring actions when 'confirm' is literally in the input\n");
        sb.append("- 'clear ...' → only match clear-requiring actions when 'clear' is literally in the input\n");
        sb.append("- 'target wingman 1/2/3' → their specific wingman actions\n");
        sb.append("- 'target next route system' → ");
        sb.append(TargetDestinationCommand.ID);
        sb.append("\n");

        sb.append("- 'target most dangerous / highest threat' → ");
        sb.append(TargetHostileHighestThreatCommand.ID);
        sb.append("\n");

        sb.append("- 'focus [my] target' / 'focus on target' → ");
        sb.append(FighterAttackTargetCommand.ID);
        sb.append(" (NOT ");
        sb.append(TargetSubsystemCommand.ID);
        sb.append(")\n");

        sb.append("- 'target fsd' → ");
        sb.append(TargetSubsystemCommand.ID);
        sb.append(")\n");

        sb.append(" ONLY. NEVER ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append(". Targeting a subsystem is not engaging it.");
        sb.append(")\n");

        sb.append("- 'target [anything else]' → ");
        sb.append(TargetSubsystemCommand.ID);
        sb.append(", key = the words after 'target'");
        sb.append(")\n");

        sb.append("- 'player profile' (input starts with 'player', NOT 'trade') → ");
        sb.append(AnalyzePlayerProfileQueryCommand.ID);
        sb.append("; NEVER any trade_profile action for this input\n");

        sb.append("- 'distance to bubble' → ");
        sb.append(AnalyzeDistanceFromTheBubbleQueryCommand.ID);
        sb.append(" (sol, earth, civilization all normalize to bubble; NOT ");
        sb.append(AnalyzeDistanceToStellarObjectQueryCommand.ID);
        sb.append(" or ");
        sb.append(AnalyzeDistanceFromFleetCarrierQueryCommand.ID);
        sb.append(")\n");

        sb.append("- 'What's my fleet carrier fuel status', 'What is our fleet carrier range' → ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append("\n");
        sb.append("- organics / biology / exobiology on a planet or here → ");
        sb.append(AnalyzeBioSamplesPlanetSurfaceQueryCommand.ID);
        sb.append(", NOT geo/materials\n");
        sb.append("- organics / bio signals in a system or which planets → ");
        sb.append(AnalyzeBioScansStarSystemQueryCommand.ID);
        sb.append("\n");

        sb.append("- 'how much X do we have' / 'do we have any X' (specific item) → ");
        sb.append(AnalyseMaterialsQueryCommand.ID);
        sb.append(" (handles both engineering materials AND cargo commodities)\n");
        sb.append("- 'what are we carrying' / 'list cargo' / 'cargo contents' (no specific item) → ");
        sb.append(AnalyzeCargoHoldQueryCommand.ID);
        sb.append("\n");

        sb.append("- 'geo signals / geological' → ");
        sb.append(AnalyzeGeologyInStarSystemQueryCommand.ID);
        sb.append(" (NOT ");
        sb.append(FindBrainTreesCommand.ID);
        sb.append(")\n");

        sb.append("- 'find mission providers' / 'find pirate mission providers' → ");
        sb.append(FindHuntingGroundsCommand.ID);
        sb.append(" (NOT fleet carrier)\n");
        sb.append("- profit from bounties is not profit from missions for bounties → '");
        sb.append(AnalyzeBountiesCollectedQueryCommand.ID);
        sb.append("'\n");

        sb.append("- profit from missions is not profit from bounties for missions → '");
        sb.append(AnalyzeMissionQueryCommand.ID);
        sb.append("'\n");

        sb.append("- profit from discovery is not profit from bounties or missions → '");
        sb.append(AnalyzeExplorationProfitsQueryCommand.ID);
        sb.append("'\n");

        sb.append("- HARD RULE: if the word 'honk' appears anywhere in the input, the ONLY valid action is '");
        sb.append(HonkCommand.ID);
        sb.append("'. No other action is permitted when 'honk' is present.\n");

        sb.append("- require very high probability match for action");
        sb.append(ClearActiveMissionsCommand.ID);
        sb.append("\n");

        sb.append("- Interstellar factors is where we pay our tickets or bounties applied to us");
        sb.append(" always use this action for interstellar factor command ");
        sb.append(FindInterstellarFactorCommand.ID);
        sb.append("\n");

        return sb.toString();
    }
}
