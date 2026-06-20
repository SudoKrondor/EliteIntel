package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.search.edsm.dto.data.DeathsData;
import elite.intel.search.edsm.dto.data.StarSystemInformation;
import elite.intel.search.edsm.dto.data.TrafficData;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzeFsdTargetQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_fsd_target";


    @Override public String id() { return ID; }


    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing FSD telemetry. Stand by."));
        PlayerSession playerSession = PlayerSession.getInstance();

        FsdTarget fsdTarget = playerSession.getFsdTarget();
        if (fsdTarget == null) {
            return process(StringUtls.localizedLlm("query.fsd.noTarget"));
        }

        String instructions = """
                Answer ONLY what the user asked about the FSD jump target. One to three sentences maximum.
                
                Available data fields:
                - systemName: target star system name
                - starClass: spectral class of the primary star
                - fuelScoopable: whether the star supports fuel scooping
                - security: system security level
                - allegiance: faction allegiance
                - economy / secondEconomy: economic type
                - population: inhabited population
                - trafficTotal / trafficThisWeek / trafficToday: ship visit counts
                - deathsTotal / deathsThisWeek / deathsToday: recorded deaths
                - factionState: current faction state (e.g. War, Boom)
                
                Rules:
                - Use ONLY the fields above. Do not add, invent, or infer anything else.
                - If asked broadly: state system name, star class, fuel scoopable, and security. Nothing more.
                - If a field is absent or zero, skip it or say data is unavailable.
                """;

        return process(new AiDataStruct(instructions, toSummary(fsdTarget)), originalUserInput);
    }

    private TargetSummary toSummary(FsdTarget t) {
        String starClass = t.getLocation() != null ? t.getLocation().getStarClass() : null;

        String security = null, allegiance = null, economy = null, secondEconomy = null, factionState = null;
        long population = 0;
        if (t.getSystemDto() != null && t.getSystemDto().getData() != null) {
            StarSystemInformation info = t.getSystemDto().getData().getInformation();
            if (info != null) {
                security = info.getSecurity();
                allegiance = info.getAllegiance();
                economy = info.getEconomy();
                secondEconomy = info.getSecondEconomy();
                factionState = info.getFactionState();
                population = info.getPopulation();
            }
        }

        int trafficTotal = 0, trafficWeek = 0, trafficToday = 0;
        if (t.getTrafficDto() != null && t.getTrafficDto().getData() != null) {
            TrafficData td = t.getTrafficDto().getData();
            if (td.getTraffic() != null) {
                trafficTotal = td.getTraffic().getTotal();
                trafficWeek = td.getTraffic().getThisWeek();
                trafficToday = td.getTraffic().getDay();
            }
        }

        int deathsTotal = 0, deathsWeek = 0, deathsToday = 0;
        if (t.getDeathsDto() != null && t.getDeathsDto().getData() != null) {
            DeathsData dd = t.getDeathsDto().getData();
            if (dd.getDeaths() != null) {
                deathsTotal = dd.getDeaths().getTotal();
                deathsWeek = dd.getDeaths().getThisWeek();
                deathsToday = dd.getDeaths().getToday();
            }
        }

        return new TargetSummary(t.getName(), starClass, t.getFuelStarStatus(),
                security, allegiance, economy, secondEconomy, factionState, population,
                trafficTotal, trafficWeek, trafficToday,
                deathsTotal, deathsWeek, deathsToday);
    }

    record TargetSummary(
            String systemName,
            String starClass,
            String fuelScoopable,
            String security,
            String allegiance,
            String economy,
            String secondEconomy,
            String factionState,
            long population,
            int trafficTotal, int trafficThisWeek, int trafficToday,
            int deathsTotal, int deathsThisWeek, int deathsToday
    ) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
