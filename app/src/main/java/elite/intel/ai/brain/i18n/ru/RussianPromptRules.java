package elite.intel.ai.brain.i18n.ru;

import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;
import elite.intel.ai.brain.i18n.PromptLanguageRules;


public class RussianPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "Russian";
    }

    @Override
    public String queryStarterExamples() {
        return "что, где, как, какой, какая, какие, почему, есть ли, сколько, на какой, в какой, расскажи";
    }

    @Override
    public String commandVerbExamples() {
        return "покажи / открой / найди / ищи / активируй / отключи / проложи маршрут / выпусти / убери / включи / выключи";
    }

    @Override
    public String queryPhraseExamples() {
        return "где / что / сколько / есть ли / какой / какая / какие / на какой станции / в какой системе";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- classify commands like прервать, заткнись to interrupt action → ");
        sb.append(InterruptCommand.ID);
        sb.append("\n");

        sb.append("- classify queries about location like как долго длится день здесь to action → ");
        sb.append(AnalyzeCurrentLocationQueryCommand.ID);
        sb.append("\n");

        sb.append("- classify bio signals: Какие планеты / ещё нуждаются в биологическом или органическом сканировании? / позволяют приземлится / имеют био сигналы итд. → ");
        sb.append(AnalyzeBioScansStarSystemQueryCommand.ID);
        sb.append("\n");

        sb.append("- classify queries about bio sample distance: расстояние до последнего биообразца, какие органические объекты ещё нужно сканировать итд. → ");
        sb.append(AnalyzeDistanceFromLastBioSampleQueryCommand.ID);
        sb.append("\n");

        sb.append("- classify commands to fire on target such as : открыть огонь, атака, атакуй итд. → ");
        sb.append(FighterFireAtWillCommand.ID);
        sb.append("\n");

        sb.append("- classify command режим анализа → ");
        sb.append(SwitchToAnalysisModeCommand.ID);
        sb.append("\n");

        sb.append("- classify command боевой режим → ");
        sb.append(SwitchToCombatModeCommand.ID);
        sb.append("\n");
        //переключись в боевой режим

        sb.append("- classify курс авианосца эскадрильи → ");
        sb.append(AnalyzeSquadronCarrierRouteQueryCommand.ID);
        sb.append(" with пункт назначения авианосца эскадрильи → ");
        sb.append(AnalyzeSquadronCarrierFinalDestinationQueryCommand.ID);
        sb.append("\n");


        sb.append(" - classify войти в суперкруиз, суперкруиз, включи суперкруиз, световая скорость, на форсаж as ");
        sb.append(EnterSuperCruiseCommand.ID);
        sb.append(" never as ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append(" - classify прыжок в гиперпространство, прыгай, гиперпрыжок, войти в гиперпространство, поехали, следующий маршрутный пункт as ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append(" - classify questions about авианосец as ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify questions about тритий авианосца, топливо авианосца, сколько трития на авианосце, уровень трития, уровень топлива авианосца as ");
        sb.append(AnalyzeFleetCarrierDataQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify questions about Расстояние до Земли, Как далеко Земля ");
        sb.append(AnalyzeDistanceFromTheBubbleQueryCommand.ID);
        sb.append("\n");


        sb.append(" - classify questions about авианосец эскадрона as ");
        sb.append(AnalyzeSquadronCarrierDataQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify questions about курс авианосца эскадрильи as ");
        sb.append(AnalyzeSquadronCarrierRouteQueryCommand.ID);
        sb.append("\n");

        sb.append(" - classify автоматическая стыковка as ");
        sb.append(TaxiToLandingPadCommand.ID);
        sb.append("\n");

        sb.append("- require very high probability match for action →");
        sb.append(ClearActiveMissionsCommand.ID);
        sb.append("\n");


        return sb.toString();
    }

    @Override
    public String localeSpecificFormattingRules() {
        return "";
    }
}
