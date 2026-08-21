package elite.intel.gameapi.journal.events.dto;

import com.google.gson.JsonObject;
import elite.intel.gameapi.JournalSymbol;
import elite.intel.gameapi.MissionTargets;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.util.json.GsonFactory;

import java.util.Locale;

public class MissionDto extends BaseJsonDto {

    private long missionId;
    private String faction;
    private String missionDescription;
    private String acceptedAt;
    private MissionType missionType;
    private long reward;
    private boolean influenceIncrease;
    private boolean isReputationIncrease;
    private String missionTargetFaction;
    private boolean isWing;
    private String expiry;
    private String destinationSystem;
    private MissionTargets missionTarget;
    private int killCount;
    private String target;
    private String commoditySymbol;
    private String commodityName;
    private long count;
    private String destinationStation;
    private String destinationSettlement;
    private long passengerCount;
    private boolean passengerVIPs;
    private boolean passengerWanted;
    private String passengerType;
    private String donation;
    private long donated;
    private String redirectedAt;

    public MissionDto(MissionAcceptedEvent event) {
        if (event != null) {
            setAcceptedAt(event.getTimestamp());
            setMissionId(event.getMissionID());
            setMissionProvider(event.getFaction());
            setMissionType(toMissionType(event.getName()));
            setMissionDescription(event.getLocalisedName());
            setReward(event.getReward());
            setReputationIncrease(event.getReputation() != null
                    && "++".equals(event.getReputation())
                    || "+".equals(event.getReputation()));
            setInfluence(event.getInfluence() != null
                    && "++".equals(event.getInfluence())
                    || "+".equals(event.getInfluence()));
            setWing(event.isWing());
            setExpiry(event.getExpiry());
            setDestinationSystem(event.getDestinationSystem());
            setDestinationSettlement(event.getDestinationSettlement());
            setMissionTargetFaction(event.getTargetFaction());
            setKillCount(event.getKillCount());
            setTarget(event.getTarget());
            setCommodityName(event.getCommodityLocalised());
            setCommoditySymbol(JournalSymbol.normalize(event.getCommodity()));
            setCount(event.getCount());
            setDestinationStation(event.getDestinationStation());
        }
    }

    private MissionType toMissionType(String name) {
        for (MissionType type : MissionType.values()) {
            if (name.toLowerCase().contains(type.getMissionType().toLowerCase(Locale.ROOT))) {
                return type;
            }
        }
        return MissionType.getUnknown();
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setCommodityName(String commodityName) {
        this.commodityName = commodityName;
    }

    public String getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(String acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public void setMissionId(long missionID) {
        this.missionId = missionID;
    }

    public void setInfluence(boolean b) {
        this.influenceIncrease = b;
    }

    public void setReputationIncrease(boolean b) {
        this.isReputationIncrease = b;
    }

    public void setDestinationSystem(String destinationSystem) {
        this.destinationSystem = destinationSystem;
    }

    public void setReward(long reward) {
        this.reward = reward;
    }

    public void setKillCount(int killCount) {
        this.killCount = killCount;
    }

    public void setMissionTargetFaction(String targetFaction) {
        this.missionTargetFaction = targetFaction;
    }

    public void setMissionTarget(MissionTargets targetTypeLocalised) {
        this.missionTarget = targetTypeLocalised;
    }

    public void setMissionDescription(String localisedName) {
        this.missionDescription = localisedName;
    }

    public void setMissionType(MissionType name) {
        this.missionType = name;
    }

    public void setMissionProvider(String faction) {
        this.faction = faction;
    }

    public void setWing(boolean wing) {
        this.isWing = wing;
    }

    /**
     * Journal {@code Expiry}, an ISO-8601 instant. Absent on missions that never
     * expire (and on rows written before this was carried across from the
     * accepted event), so every reader has to tolerate null.
     */
    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public void setDestinationSettlement(String destinationSettlement) {
        this.destinationSettlement = destinationSettlement;
    }

    public void setPassengerCount(long passengerCount) {
        this.passengerCount = passengerCount;
    }

    public boolean isPassengerVIPs() {
        return passengerVIPs;
    }

    public void setPassengerVIPs(boolean passengerVIPs) {
        this.passengerVIPs = passengerVIPs;
    }

    public void setPassengerType(String passengerType) {
        this.passengerType = passengerType;
    }

    public void setDonation(String donation) {
        this.donation = donation;
    }

    public void setDonated(long donated) {
        this.donated = donated;
    }

    /**
     * The bare journal symbol of the commodity the mission wants ("hazardousenvironmentsuits").
     * <p>
     * {@link #commodityName} is the game's localised name, written in whatever language the GAME is
     * running in - which need not be the app's, and need not be one of the six the game supports at
     * all. The symbol is language-free, so it is what joins a mission to the cargo hold and to the
     * commodities table, and through that table to the English name Spansh searches by.
     */
    public void setCommoditySymbol(String commoditySymbol) {
        this.commoditySymbol = commoditySymbol;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void setDestinationStation(String destinationStation) {
        this.destinationStation = destinationStation;
    }

    public void setPassengerWanted(boolean passengerWanted) {
        this.passengerWanted = passengerWanted;
    }

    /**
     * Timestamp of the journal {@code MissionRedirected} for this mission, or null if it has not
     * been redirected.
     * <p>
     * This is the game telling us the mission's objectives are met and only the turn-in is left -
     * on a massacre mission that is the ONLY authoritative "the kills are done" signal the journal
     * offers. There is no per-kill mission counter in the journal, and a {@code Bounty} is not proof
     * of mission credit (an assisted kill still pays a voucher), so kill progress inferred from
     * bounties is an upper bound and this field is what confirms it. See {@code MassacreProgress}.
     */
    public void setRedirectedAt(String redirectedAt) {
        this.redirectedAt = redirectedAt;
    }
    /*
            GET Functions
     */

    public String getFaction() {
        return faction;
    }

    public MissionType getMissionType() {
        return missionType;
    }

    public String getMissionDescription() {
        return missionDescription;
    }

    public MissionTargets getMissionTarget() {
        return missionTarget;
    }

    public String getMissionTargetFaction() {
        return missionTargetFaction;
    }

    public int getKillCount() {
        return killCount;
    }

    public long getReward() {
        return reward;
    }

    public String getDestinationSystem() {
        return destinationSystem;
    }

    // Redundant?
    public boolean isReputationIncrease() {
        return isReputationIncrease;
    }

    // Redundant?
    public boolean isInfluenceIncrease() {
        return influenceIncrease;
    }

    public long getMissionId() {
        return missionId;
    }

    public String getTarget() {
        return target;
    }

    public String getCommoditySymbol() {
        return commoditySymbol;
    }

    public long getCount() {
        return count;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public String getDestinationSettlement() {
        return destinationSettlement;
    }

    public long getPassengerCount() {
        return passengerCount;
    }

    public String getPassengerType() {
        return passengerType;
    }

    public String getDonation() {
        return donation;
    }

    public long getDonated() {
        return donated;
    }

    public boolean isPassengerWanted() {
        return passengerWanted;
    }

    public String getCommodityName() {
        return commodityName;
    }

    public String getEventType() {
        return "Mission";
    }

    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public boolean isWing() {
        return isWing;
    }

    public String getExpiry() {
        return expiry;
    }

    /**
     * @see #setRedirectedAt(String)
     */
    public String getRedirectedAt() {
        return redirectedAt;
    }

    /**
     * True once the game has redirected this mission to its turn-in point, i.e. its objectives are
     * complete. Null on rows written before this field existed, which read as "not confirmed".
     */
    public boolean isObjectivesComplete() {
        return redirectedAt != null;
    }
}
