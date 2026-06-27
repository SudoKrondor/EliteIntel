package elite.intel.ai.brain.i18n.de;

import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;
import elite.intel.ai.brain.i18n.PromptLanguageRules;
import elite.intel.ai.brain.actions.command.builtin.ClearActiveMissionsCommand;
import elite.intel.ai.brain.i18n.PromptLanguageRules;


public class GermanPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "German";
    }

    @Override
    public String queryStarterExamples() {
        return "was, wo, wie, welcher, welche, welches, warum, gibt es, wie viel, wie viele, erzähl mir";
    }

    @Override
    public String commandVerbExamples() {
        return "zeig / öffne / finde / suche / aktiviere / deaktiviere / navigiere / plotte / fahre aus / einziehen / einschalten / ausschalten";
    }

    @Override
    public String queryPhraseExamples() {
        return "wo / was / wie viel / wie viele / gibt es / welcher / welche / auf welcher Station / in welchem System";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- classify commands like unterbrich, sei still, ruhe, halt die klappe to interrupt action → ");
        sb.append(InterruptCommand.ID);
        sb.append("\n");

        sb.append("- classify queries about location like wie lange dauert ein tag hier to action → ");
        sb.append(AnalyzeCurrentLocationQueryCommand.ID);
        sb.append("\n");

        sb.append("- classify bio signals: Welche Planeten / müssen noch biologisch oder organisch gescannt werden? / sind landbar / haben Biosignale usw. → ");
        sb.append(AnalyzeBioScansStarSystemQueryCommand.ID);
        sb.append("\n");

        sb.append("- classify DISTANCE QUERIES about bio sample: entfernung zur letzten bio probe, wie weit zur probe usw. → ");
        sb.append(AnalyzeDistanceFromLastBioSampleQueryCommand.ID);
        sb.append(" ONLY when asking HOW FAR. NEVER when navigating.\n");
        sb.append("- HARD RULE: navigation / fliege zu / gehe zu / zum nächsten bio sample / codex → ");
        sb.append(NavigateToBioSampleCodexEntryCommand.ID);
        sb.append(" ONLY. NEVER query_distance_to_bio_sample for navigation commands.\n");

        sb.append("- classify commands to fire on target such as: feuer frei, feuer, angriff, das feuer eröffnen → ");
        sb.append(FighterFireAtWillCommand.ID);
        sb.append("\n");

        sb.append("- HARD RULE: generic scan-the-system commands erkunde das system, scanne das system, system erkunden, system abtasten, system scannen → ");
        sb.append(HonkCommand.ID);
        sb.append(" ONLY. Use ");
        sb.append(OpenFssScanSystemCommand.ID);
        sb.append(" ONLY for explicit full-spectrum terms: fss, vollständiger spektralscan, spektralscan, vollständiger scan, systemscan.\n");

        sb.append("- classify command analysemodus, erkundungsmodus → ");
        sb.append(SwitchToAnalysisModeCommand.ID);
        sb.append("\n");

        sb.append("- classify command kampfmodus, kampf → ");
        sb.append(SwitchToCombatModeCommand.ID);
        sb.append("\n");

        sb.append("- classify chaff/decoy commands: täuschkörper, düppel, chaff → ");
        sb.append(DeployChaffCommand.ID);
        sb.append(" but heat venting wärmesenke, kühlkörper, hitze abwerfen → ");
        sb.append(DeployHeatSinkCommand.ID);
        sb.append("\n");

        sb.append("- classify navigation abbrechen, route abbrechen, navigation ausschalten → ");
        sb.append(CancelNavigationCommand.ID);
        sb.append(" NEVER ");
        sb.append(ToggleLightsOnOffCommand.ID);
        sb.append("\n");

        sb.append("- classify squadron carrier kurs → ");
        sb.append(AnalyzeSquadronCarrierFinalDestinationQueryCommand.ID);
        sb.append(" with squadron carrier route → ");
        sb.append(AnalyzeSquadronCarrierRouteQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify in supercruise gehen, supercruise, supercruise einschalten, lichtgeschwindigkeit, überlichtflug as ");
        sb.append(EnterSuperCruiseCommand.ID);
        sb.append(" never as ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append(" - classify sprung in den hyperraum, sprung, hypersprung, in den hyperraum, los gehts, auf gehts, nächster wegpunkt as ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append(" - classify questions about treibstoff, tritium des carriers as ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify carrier reichweite / sprungreichweite (the carrier's JUMP RANGE) as ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append(", NOT ");
        sb.append(AnalyzeDistanceFromFleetCarrierQueryCommand.ID);
        sb.append(" (which is only how far the carrier is away)\n");

        sb.append(" - classify questions about Entfernung zur Erde, wie weit zur Erde, wie weit zur Zivilisation as ");
        sb.append(AnalyzeDistanceFromTheBubbleQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify automatisches andocken as ");
        sb.append(TaxiToLandingPadCommand.ID);
        sb.append("\n");

        sb.append("- HARD RULE: a carrier question WITHOUT the word 'squadron' is about the FLEET carrier → ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append("; ONLY when 'squadron' is present use the squadron action → ");
        sb.append(AnalyzeSquadronCarrierDataQueryCommand.ID);
        sb.append("\n");

        sb.append("- require very high probability match for action → ");
        sb.append(ClearActiveMissionsCommand.ID);
        sb.append("\n");

        return sb.toString();
    }

    @Override
    public String localeSpecificFormattingRules() {
        return "";
    }
}
