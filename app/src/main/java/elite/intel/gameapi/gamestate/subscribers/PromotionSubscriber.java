package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.gameapi.journal.events.PromotionEvent;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.Ranks;

import static elite.intel.util.StringUtls.localizedEvent;

public class PromotionSubscriber {

    @Subscribe
    public void onPromotionEvent(PromotionEvent event) {
        Integer imperialNavyRank = event.getEmpire();
        Integer combatRank = event.getCombat();
        Integer tradeRank = event.getTrade();
        Integer exploreRank = event.getExplore();
        Integer soldier = event.getSoldier();
        Integer federalNavyRank = event.getFederation();
        Integer exobiologyRank = event.getExobiologist();

        PlayerSession session = PlayerSession.getInstance();
        RankAndProgressDto rankAndProgressDto = session.getRankAndProgressDto();

        if (imperialNavyRank != null) {
            String string = Ranks.getImperialRankMap().get(imperialNavyRank);
            VegaRuntime.narrator().narrate(localizedEvent("event.promotion.imperialNavy", string), "Congratulate the commander on their promotion and state the new rank clearly.");
            rankAndProgressDto.setCombatRankEmpire(event.getEmpire());
            session.setRankAndProgressDto(rankAndProgressDto);
        }
        if (federalNavyRank != null) {
            String string = Ranks.getFederationRankMap().get(federalNavyRank);
            VegaRuntime.narrator().narrate(localizedEvent("event.promotion.federalNavy", string), "Congratulate the commander on their promotion and state the new rank clearly.");
            rankAndProgressDto.setCombatRankFederation(event.getFederation());
            session.setRankAndProgressDto(rankAndProgressDto);
        }
        if (combatRank != null) {
            String string = Ranks.getCombatRankMap().get(combatRank);
            VegaRuntime.narrator().narrate(localizedEvent("event.promotion.combat", string), "Congratulate the commander on their promotion and state the new rank clearly.");
            rankAndProgressDto.setCombatRank(string);
            session.setRankAndProgressDto(rankAndProgressDto);
        }

        //other ranks are not used for honorifics etc. the data will update on next game session load.
        if (tradeRank != null) {
            String string = Ranks.getTradeRankMap().get(tradeRank);
            VegaRuntime.narrator().narrate(localizedEvent("event.promotion.trade", string), "Congratulate the commander on their promotion and state the new rank clearly.");
        }
        if (exploreRank != null) {
            String string = Ranks.getExplorationRankMap().get(exploreRank);
            VegaRuntime.narrator().narrate(localizedEvent("event.promotion.exploration", string), "Congratulate the commander on their promotion and state the new rank clearly.");
        }
        if (exobiologyRank != null) {
            String string = Ranks.getExobiologyRankMap().get(exobiologyRank);
            VegaRuntime.narrator().narrate(localizedEvent("event.promotion.exobiology", string), "Congratulate the commander on their promotion and state the new rank clearly.");
        }
        if (soldier != null) {
            String string = Ranks.getMercenaryRankMap().get(soldier);
            VegaRuntime.narrator().narrate(localizedEvent("event.promotion.soldier", string), "Congratulate the commander on their promotion and state the new rank clearly.");
        }
    }
}
