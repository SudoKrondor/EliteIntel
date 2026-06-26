package elite.intel.ai.brain.i18n.uk;

import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;
import elite.intel.ai.brain.i18n.PromptLanguageRules;


public class UkrainianPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "Ukrainian";
    }

    @Override
    public String queryStarterExamples() {
        return "що, де, як, який, яка, які, чому, чи є, скільки, на якій, в якій, розкажи";
    }

    @Override
    public String commandVerbExamples() {
        return "покажи / відкрий / знайди / шукай / активуй / вимкни / проклади маршрут / випусти / прибери / увімкни / вимкни";
    }

    @Override
    public String queryPhraseExamples() {
        return "де / що / скільки / чи є / який / яка / які / на якій станції / в якій системі";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- classify commands like перерви, замовкни, годі, відставити to interrupt action → ");
        sb.append(InterruptCommand.ID);
        sb.append("\n");

        sb.append("- classify queries about location like скільки триває день тут to action → ");
        sb.append(AnalyzeCurrentLocationQueryCommand.ID);
        sb.append("\n");

        sb.append("- classify bio signals: Які планети / ще потребують біологічного чи органічного сканування? / придатні для посадки / мають біосигнали тощо. → ");
        sb.append(AnalyzeBioScansStarSystemQueryCommand.ID);
        sb.append("\n");

        sb.append("- classify DISTANCE QUERIES about bio sample: відстань до останнього біозразка, як далеко до зразка, скільки метрів до організму тощо. → ");
        sb.append(AnalyzeDistanceFromLastBioSampleQueryCommand.ID);
        sb.append(" ONLY when asking HOW FAR. NEVER when navigating.\n");
        sb.append("- HARD RULE: навігація / лети до / перейди до / знайди найближчий біозразок / кодекс → ");
        sb.append(NavigateToBioSampleCodexEntryCommand.ID);
        sb.append(" ONLY. NEVER query_distance_to_bio_sample for navigation commands.\n");

        sb.append("- classify commands to fire on target such as: відкрити вогонь, атака, атакуй, вогонь тощо. → ");
        sb.append(FighterFireAtWillCommand.ID);
        sb.append("\n");

        sb.append("- classify HONK commands: досліджуй систему, скануй систему, відскануй систему, обстеж систему → ");
        sb.append(HonkCommand.ID);
        sb.append(" but full/filtered spectrum scan phrases повне спектральне сканування, фільтроване спектральне сканування, сканування системи, FSS → ");
        sb.append(OpenFssScanSystemCommand.ID);
        sb.append("\n");

        sb.append("- classify command режим аналізу, режим дослідника → ");
        sb.append(SwitchToAnalysisModeCommand.ID);
        sb.append("\n");

        sb.append("- classify command бойовий режим, бойовий → ");
        sb.append(SwitchToCombatModeCommand.ID);
        sb.append("\n");

        sb.append("- classify chaff/decoy commands: випусти пастки, диполі, дипольні відбивачі, пасивні перешкоди → ");
        sb.append(DeployChaffCommand.ID);
        sb.append(" but heat venting тепловідвід, скинь тепло → ");
        sb.append(DeployHeatSinkCommand.ID);
        sb.append("\n");

        sb.append("- classify скасуй навігацію, перерви навігацію, вимкни навігацію → ");
        sb.append(CancelNavigationCommand.ID);
        sb.append(" NEVER ");
        sb.append(ToggleLightsOnOffCommand.ID);
        sb.append("\n");

        sb.append("- HARD RULE: a carrier question WITHOUT the word 'ескадрильї' is about the FLEET carrier → ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append("; ONLY when 'ескадрильї' is present use the squadron action → ");
        sb.append(AnalyzeSquadronCarrierDataQueryCommand.ID);
        sb.append("\n");

        sb.append("- classify курс авіаносця ескадрильї → ");
        sb.append(AnalyzeSquadronCarrierFinalDestinationQueryCommand.ID);
        sb.append(" with маршрут авіаносця ескадрильї → ");
        sb.append(AnalyzeSquadronCarrierRouteQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify увійти в суперкруїз, суперкруїз, увімкни суперкруїз, світлова швидкість, на форсаж as ");
        sb.append(EnterSuperCruiseCommand.ID);
        sb.append(" never as ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append(" - classify стрибок у гіперпростір, стрибай, гіперстрибок, увійти в гіперпростір, відходимо, поїхали, наступна точка маршруту as ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append(" - classify questions about авіаносець as ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify questions about тритій авіаносця, паливо авіаносця, скільки тритію на авіаносці, рівень тритію, рівень палива авіаносця as ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify questions about Відстань до Землі, Як далеко Земля, як далеко до цивілізації as ");
        sb.append(AnalyzeDistanceFromTheBubbleQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify questions about авіаносець ескадрильї as ");
        sb.append(AnalyzeSquadronCarrierDataQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify автоматичне стикування as ");
        sb.append(TaxiToLandingPadCommand.ID);
        sb.append("\n");

        sb.append("- require very high probability match for action → ");
        sb.append(ClearActiveMissionsCommand.ID);
        sb.append("\n");

        return sb.toString();
    }
}
