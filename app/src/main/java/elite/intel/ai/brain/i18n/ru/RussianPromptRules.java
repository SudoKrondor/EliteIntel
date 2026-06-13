package elite.intel.ai.brain.i18n.ru;

import elite.intel.ai.brain.actions.Commands;
import elite.intel.ai.brain.actions.Queries;
import elite.intel.ai.brain.i18n.PromptLanguageRules;

import static elite.intel.ai.brain.actions.Commands.*;
import static elite.intel.ai.brain.actions.Queries.*;

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
        sb.append(INTERRUPT_TTS.getAction());
        sb.append("\n");

        sb.append("- classify queries about location like как долго длится день здесь to action → ");
        sb.append(CURRENT_LOCATION.getAction());
        sb.append("\n");

        sb.append("- classify bio signals: Какие планеты / ещё нуждаются в биологическом или органическом сканировании? / позволяют приземлится / имеют био сигналы итд. → ");
        sb.append(Queries.BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        sb.append("\n");

        sb.append("- classify queries about bio sample distance: расстояние до последнего биообразца, какие органические объекты ещё нужно сканировать итд. → ");
        sb.append(Queries.DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
        sb.append("\n");

        sb.append("- classify commands to fire on target such as : открыть огонь, атака, атакуй итд. → ");
        sb.append(Commands.FIGHTER_OPEN_ORDERS.getAction());
        sb.append("\n");

        sb.append("- classify command режим анализа → ");
        sb.append(Commands.ACTIVATE_ANALYSIS_MODE.getAction());
        sb.append("\n");

        sb.append("- classify command боевой режим → ");
        sb.append(Commands.ACTIVATE_COMBAT_MODE.getAction());
        sb.append("\n");
        //переключись в боевой режим

        sb.append("- classify курс авианосца эскадрильи → ");
        sb.append(SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        sb.append(" with пункт назначения авианосца эскадрильи → ");
        sb.append(SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
        sb.append("\n");


        sb.append(" - classify войти в суперкруиз, суперкруиз, включи суперкруиз, световая скорость, на форсаж as ");
        sb.append(ENTER_SUPER_CRUISE.getAction());
        sb.append(" never as ");
        sb.append(JUMP_TO_HYPERSPACE.getAction());
        sb.append("\n");
        sb.append(" - classify прыжок в гиперпространство, прыгай, гиперпрыжок, войти в гиперпространство, поехали, следующий маршрутный пункт as ");
        sb.append(JUMP_TO_HYPERSPACE.getAction());
        sb.append("\n");

        sb.append("- require very high probability match for action →");
        sb.append(CLEAR_ALL_ACTIVE_MISSIONS.getAction());
        sb.append("\n");
        return sb.toString();
    }
}
