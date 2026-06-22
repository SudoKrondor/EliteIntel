package elite.intel.gameapi.journal.events.dto;

import elite.intel.gameapi.journal.events.PlayerProgressStats;
import elite.intel.gameapi.journal.events.PlayerRankStats;
import elite.intel.util.Ranks;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class RankAndProgressDto implements ToJsonConvertible {

    private String combatRank = "unknown";
    private Integer combatRankEmpire = -1;
    private Integer combatRankFederation = -1;
    private String highestMilitaryRank = "unknown";
    private String exobiologyRank = "unknown";
    private String explorationRank = "unknown";
    private String mercenaryRank = "unknown";

    private String pledgedToPower = "unknown";
    private String allegiance = "unknown";
    private int powerRank = 0;
    private int merrits = 0;
    private long timePledged = 0;

    private int combatProgressToNextRankInPercent = 0;
    private int cqcRankProgressToNextRankInPercent = 0;
    private int empireMilitaryRankProgressToNextRankInPercent = 0;
    private int exobiologyProgressToNextRankInPercent = 0;
    private int explorationProgressToNextRankInPercent = 0;
    private int federationMilitaryRankProgressToNextRankInPercent = 0;
    private int mercenaryProgressToNextRankInPercent = 0;

    public String getCombatRank() {
        return combatRank;
    }

    public void setCombatRank(String combatRank) {
        this.combatRank = combatRank;
    }

    public Integer getCombatRankEmpire() {
        return combatRankEmpire;
    }

    public void setCombatRankEmpire(Integer combatRankEmpire) {
        this.combatRankEmpire = combatRankEmpire;
    }

    public Integer getCombatRankFederation() {
        return combatRankFederation;
    }

    public void setCombatRankFederation(Integer combatRankFederation) {
        this.combatRankFederation = combatRankFederation;
    }

    public String getHighestMilitaryRank() {
        return highestMilitaryRank;
    }

    public void setHighestMilitaryRank(String highestMilitaryRank) {
        this.highestMilitaryRank = highestMilitaryRank;
    }

    /**
     * Resolves the honorific dynamically from the stored (language-independent) navy rank
     * indices, so it always reflects the active UI language even after a language switch.
     */
    public String getHonorific() {
        String honorific = Ranks.getHonorific(combatRankEmpire, combatRankFederation);
        return honorific == null ? "Commander" : honorific;
    }

    public String getExobiologyRank() {
        return exobiologyRank;
    }

    public void setExobiologyRank(String exobiologyRank) {
        this.exobiologyRank = exobiologyRank;
    }

    public String getExplorationRank() {
        return explorationRank;
    }

    public void setExplorationRank(String explorationRank) {
        this.explorationRank = explorationRank;
    }

    public String getMercenaryRank() {
        return mercenaryRank;
    }

    public void setMercenaryRank(String mercenaryRank) {
        this.mercenaryRank = mercenaryRank;
    }

    public int getCombatProgressToNextRankInPercent() {
        return combatProgressToNextRankInPercent;
    }

    public void setCombatProgressToNextRankInPercent(int combatProgressToNextRankInPercent) {
        this.combatProgressToNextRankInPercent = combatProgressToNextRankInPercent;
    }

    public int getMercenaryProgressToNextRankInPercent() {
        return mercenaryProgressToNextRankInPercent;
    }

    public void setMercenaryProgressToNextRankInPercent(int mercenaryProgressToNextRankInPercent) {
        this.mercenaryProgressToNextRankInPercent = mercenaryProgressToNextRankInPercent;
    }

    public int getExplorationProgressToNextRankInPercent() {
        return explorationProgressToNextRankInPercent;
    }

    public void setExplorationProgressToNextRankInPercent(int explorationProgressToNextRankInPercent) {
        this.explorationProgressToNextRankInPercent = explorationProgressToNextRankInPercent;
    }

    public int getExobiologyProgressToNextRankInPercent() {
        return exobiologyProgressToNextRankInPercent;
    }

    public void setExobiologyProgressToNextRankInPercent(int exobiologyProgressToNextRankInPercent) {
        this.exobiologyProgressToNextRankInPercent = exobiologyProgressToNextRankInPercent;
    }

    public int getEmpireMilitaryRankProgressToNextRankInPercent() {
        return empireMilitaryRankProgressToNextRankInPercent;
    }

    public void setEmpireMilitaryRankProgressToNextRankInPercent(int empireMilitaryRankProgressToNextRankInPercent) {
        this.empireMilitaryRankProgressToNextRankInPercent = empireMilitaryRankProgressToNextRankInPercent;
    }

    public int getFederationMilitaryRankProgressToNextRankInPercent() {
        return federationMilitaryRankProgressToNextRankInPercent;
    }

    public void setFederationMilitaryRankProgressToNextRankInPercent(int federationMilitaryRankProgressToNextRankInPercent) {
        this.federationMilitaryRankProgressToNextRankInPercent = federationMilitaryRankProgressToNextRankInPercent;
    }

    public int getCqcRankProgressToNextRankInPercent() {
        return cqcRankProgressToNextRankInPercent;
    }

    public void setCqcRankProgressToNextRankInPercent(int cqcRankProgressToNextRankInPercent) {
        this.cqcRankProgressToNextRankInPercent = cqcRankProgressToNextRankInPercent;
    }


    public String getPledgedToPower() {
        return pledgedToPower;
    }

    public void setPledgedToPower(String pledgedToPower) {
        this.pledgedToPower = pledgedToPower;
    }

    public String getAllegiance() {
        return allegiance;
    }

    public void setAllegiance(String allegiance) {
        this.allegiance = allegiance;
    }

    public int getPowerRank() {
        return powerRank;
    }

    public void setPowerRank(int powerRank) {
        this.powerRank = powerRank;
    }

    public int getMerrits() {
        return merrits;
    }

    public void setMerrits(int merrits) {
        this.merrits = merrits;
    }

    public long getTimePledged() {
        return timePledged;
    }

    public void setTimePledged(long timePledged) {
        this.timePledged = timePledged;
    }

    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    public void setRanksData(PlayerRankStats data) {
        this.setCombatRank(Ranks.getCombatRankMap().get(data.getCombat()));
        this.setExobiologyRank(Ranks.getExobiologyRankMap().get(data.getExobiologist()));
        this.setExplorationRank(Ranks.getExplorationRankMap().get(data.getExplore()));
        this.setHighestMilitaryRank(Ranks.getHighestRankAsString(data.getEmpire(), data.getFederation()));
        this.setCombatRankEmpire(data.getEmpire());
        this.setCombatRankFederation(data.getFederation());
        this.setMercenaryRank(Ranks.getMercenaryRankMap().get(data.getSoldier()));
    }

    public void setProgressData(PlayerProgressStats data) {
        this.setCombatProgressToNextRankInPercent(data.getCombat());
        this.setCqcRankProgressToNextRankInPercent(data.getCQC());
        this.setEmpireMilitaryRankProgressToNextRankInPercent(data.getEmpire());
        this.setExobiologyProgressToNextRankInPercent(data.getExobiologist());
        this.setExplorationProgressToNextRankInPercent(data.getExplore());
        this.setFederationMilitaryRankProgressToNextRankInPercent(data.getFederation());
        this.setMercenaryProgressToNextRankInPercent(data.getSoldier());
    }

}
