package elite.intel.gameapi.missions;

import java.util.Locale;

/**
 * Whether an accepted contract is a pirate massacre contract, for the purpose of learning where such
 * work is issued and where it is fought.
 * <p>
 * WHY this is not {@code MissionManager.getPirateMissionTypes()}: that answers a different question -
 * which of the missions currently in the commander's log the kill bar and the progress query should
 * add up - and it matches only the two plain mission names, {@code Mission_Massacre} and
 * {@code Mission_MassacreWing}. The game also issues the same work wrapped in a state or a rank,
 * as {@code Mission_Massacre_Legal_Military} or {@code Mission_Massacre_RankFed}, and those send the
 * commander to exactly the same resource site to kill exactly the same pirates. For learning a
 * provider and its hunting ground they count, and leaving them out drops real pairs.
 * <p>
 * The predicate reads the game's own statement of who the targets are rather than guessing from the
 * name. {@code TargetType} is a symbol, so it says the same thing whatever language the client runs
 * in - a massacre contract against terrorists or against a specific faction's ships carries a
 * different tag and is not this.
 */
public final class PirateMassacreContract {

    private static final String PIRATE_TARGET_TAG = "$MissionUtil_FactionTag_Pirate;";
    private static final String MASSACRE = "massacre";

    private PirateMassacreContract() {
    }

    public static boolean isOne(String missionName, String targetType) {
        if (missionName == null || targetType == null) return false;
        if (!missionName.toLowerCase(Locale.ROOT).contains(MASSACRE)) return false;
        return PIRATE_TARGET_TAG.equals(targetType);
    }
}
