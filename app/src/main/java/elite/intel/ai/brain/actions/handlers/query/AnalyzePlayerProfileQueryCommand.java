package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzePlayerProfileQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_player_profile_rank_progress";


    @Override public String id() { return ID; }


    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing commander's  data. Stand by."));
        PlayerSession playerSession = PlayerSession.getInstance();

        String highestMilitaryRank = playerSession.getPlayerHighestMilitaryRank();
        long totalBountiesCollected = playerSession.getBountyCollectedLiveTime();
        long highestTransaction = playerSession.getHighestTransaction();
        long tradeProfits = playerSession.getTradeProfits();
        long personalCredits = playerSession.getPersonalCredits();
        double totalDistanceTraveled = playerSession.getTotalDistanceTraveled();
        long totalHyperspaceDistance = playerSession.getTotalHyperspaceDistance();
        long totalProfitsFromExploration = playerSession.getTotalProfitsFromExploration();
        long totalSystemsVisited = playerSession.getTotalSystemsVisited();
        long totalExobiologyProfits = playerSession.getTotalExobiologyProfits();

        RankAndProgressDto data = playerSession.getRankAndProgressDto();
        if (data == null) {
            return GenericResponse.getInstance().genericResponse("No data available");
        }

        String instructions = """
                Answer the user's question about commander progression statistics.
                
                Data fields:
                - data.combatRank / explorationRank / exobiologyRank / mercenaryRank: current rank titles
                - data.militaryRankEmpire / militaryRankFederation: military ranks
                - data.combatProgressToNextRankInPercent and similar: progress toward next rank (0-99 percent)
                - data.pledgedToPower: power the commander is pledged to
                - data.powerRank: numeric power rank
                - data.timePledged: time pledged to power in seconds - convert to hours and minutes when reporting
                - data.merits: powerplay merits
                - highestMilitaryRank: highest military rank achieved across all factions
                - totalBountiesCollected: lifetime bounty credits collected
                - highestTransaction: largest single transaction in credits
                - totalProfits: total trade profits in credits
                - personalCredits: current credit balance
                - totalDistanceTraveled: total distance traveled in light years
                - totalHyperspaceDistanceTraveled: total hyperspace distance in light years
                - totalSystemsVisited: number of star systems visited
                - totalExobiologyProfits: total credits from exobiology
                - totalProfitsFromExploration: total credits from exploration
                
                Rules:
                - Answer only what the user asked. If no specific question, provide a brief progression summary.
                - Convert timePledged from seconds to hours and minutes.
                - Distances are in light years.
                - these stats are not about you, they are about player. Start you reply with "Your progress..."
                Example: Your progress in X rank is Y percent etc.
                """;
        return process(
                new AiDataStruct(
                        instructions,
                        new DataDto(
                                data,
                                highestMilitaryRank,
                                totalBountiesCollected,
                                highestTransaction,
                                tradeProfits,
                                personalCredits,
                                totalDistanceTraveled,
                                totalHyperspaceDistance,
                                totalSystemsVisited,
                                totalExobiologyProfits,
                                totalProfitsFromExploration
                        )
                ),
                originalUserInput
        );
    }

    record DataDto(
            RankAndProgressDto data,
            String highestMilitaryRank,
            long totalBountiesCollected,
            long highestTransaction,
            long totalProfits,
            long personalCredits,
            double totalDistanceTraveled,
            long totalHyperspaceDistanceTraveled,
            long totalSystemsVisited,
            long totalExobiologyProfits,
            long totalProfitsFromExploration
    ) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
