package elite.intel.util;

import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.session.PlayerSession;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static elite.intel.gameapi.i18n.EventsTextProvider.getText;

/**
 * The Ranks class provides various static methods to manage and retrieve mappings
 * of different ranks, honorific titles, and their respective hierarchical levels.
 * These mappings include data for Imperial, Federation, Combat, Exobiology, Exploration,
 * and Trade ranks.
 */
public class Ranks {

    private static final Map<String, String> RANK_I18N_KEY_MAP = Map.ofEntries(
            // Imperial
            Map.entry("Outsider",             "ranks.imperial.outsider"),
            Map.entry("Serf",                 "ranks.imperial.serf"),
            Map.entry("Master",               "ranks.imperial.master"),
            Map.entry("Squire",               "ranks.imperial.squire"),
            Map.entry("Knight",               "ranks.imperial.knight"),
            Map.entry("Lord",                 "ranks.imperial.lord"),
            Map.entry("Baron",                "ranks.imperial.baron"),
            Map.entry("Viscount",             "ranks.imperial.viscount"),
            Map.entry("Count",                "ranks.imperial.count"),
            Map.entry("Earl",                 "ranks.imperial.earl"),
            Map.entry("Marquis",              "ranks.imperial.marquis"),
            Map.entry("Duke",                 "ranks.imperial.duke"),
            Map.entry("Prince",               "ranks.imperial.prince"),
            Map.entry("King",                 "ranks.imperial.king"),
            // Federation
            Map.entry("Recruit",              "ranks.federation.recruit"),
            Map.entry("Cadet",                "ranks.federation.cadet"),
            Map.entry("Midshipman",           "ranks.federation.midshipman"),
            Map.entry("Petty Officer",        "ranks.federation.pettyOfficer"),
            Map.entry("Chief Petty Officer",  "ranks.federation.chiefPettyOfficer"),
            Map.entry("Warrant Officer",      "ranks.federation.warrantOfficer"),
            Map.entry("Ensign",               "ranks.federation.ensign"),
            Map.entry("Lieutenant",           "ranks.federation.lieutenant"),
            Map.entry("Lieutenant Commander", "ranks.federation.lieutenantCommander"),
            Map.entry("Post Commander",       "ranks.federation.postCommander"),
            Map.entry("Post Captain",         "ranks.federation.postCaptain"),
            Map.entry("Rear Admiral",         "ranks.federation.rearAdmiral"),
            Map.entry("Vice Admiral",         "ranks.federation.viceAdmiral"),
            Map.entry("Admiral",              "ranks.federation.admiral")
    );

    /**
     * Returns the localized display name for the given English rank name sourced from the game journal.
     * Falls back to the original English name if no translation key is registered.
     * Returns {@code null} for {@code null}, blank, or {@code "none"} input so callers can filter it out.
     */
    public static String getLocalizedRankName(String englishRankName) {
        if (englishRankName == null || englishRankName.isBlank() || "none".equalsIgnoreCase(englishRankName)) {
            return null;
        }
        String key = RANK_I18N_KEY_MAP.get(englishRankName);
        return key != null ? getText(key) : englishRankName;
    }

    /**
     * Returns the honorific map. Military rank to Honorific mapping.
     * Values are resolved from the active UI language at call time.
     */
    public static HashMap<String, String> getImperialHonorificMap() {
        HashMap<String, String> rankMap = new HashMap<>();

        //Imperial ranks
        rankMap.put("none",     getText("ranks.honorific.commander"));
        rankMap.put("Outsider", getText("ranks.honorific.outsider"));
        rankMap.put("Serf",     getText("ranks.honorific.serf"));
        rankMap.put("Master",   getText("ranks.honorific.master"));
        rankMap.put("Squire",   getText("ranks.honorific.squire"));
        rankMap.put("Knight",   getText("ranks.honorific.sir"));
        rankMap.put("Lord",     getText("ranks.honorific.myLord"));
        rankMap.put("Baron",    getText("ranks.honorific.myLord"));
        rankMap.put("Viscount", getText("ranks.honorific.myLord"));
        rankMap.put("Count",    getText("ranks.honorific.myLord"));
        rankMap.put("Earl",     getText("ranks.honorific.myLord"));
        rankMap.put("Marquis",  getText("ranks.honorific.myLord"));
        rankMap.put("Duke",     getText("ranks.honorific.yourGrace"));
        rankMap.put("Prince",   getText("ranks.honorific.yourHighness"));
        rankMap.put("King",     getText("ranks.honorific.yourMajesty"));

        return rankMap;
    }


    /**
     * Returns the imperial military rank map int to rank name mapping.
     *
     */
    public static HashMap<Integer, String> getImperialRankMap() {
        HashMap<Integer, String> rankMap = new HashMap<>();
        rankMap.put(0, getText("ranks.imperial.none"));
        rankMap.put(1, getText("ranks.imperial.outsider"));
        rankMap.put(2, getText("ranks.imperial.serf"));
        rankMap.put(3, getText("ranks.imperial.master"));
        rankMap.put(4, getText("ranks.imperial.squire"));
        rankMap.put(5, getText("ranks.imperial.knight"));
        rankMap.put(6, getText("ranks.imperial.lord"));
        rankMap.put(7, getText("ranks.imperial.baron"));
        rankMap.put(8, getText("ranks.imperial.viscount"));
        rankMap.put(9, getText("ranks.imperial.count"));
        rankMap.put(10, getText("ranks.imperial.earl"));
        rankMap.put(11, getText("ranks.imperial.marquis"));
        rankMap.put(12, getText("ranks.imperial.duke"));
        rankMap.put(13, getText("ranks.imperial.prince"));
        rankMap.put(14, getText("ranks.imperial.king"));
        return rankMap;
    }


    /**
     * Returns the federation honorific map rank name to honorific mapping.
     *
     */
    public static HashMap<String, String> getFederationHonorificMap() {
        HashMap<String, String> rankMap = new HashMap<>();
        //Federation ranks
        rankMap.put("Recruit",              getText("ranks.honorific.recruit"));
        rankMap.put("Cadet",                getText("ranks.honorific.cadet"));
        rankMap.put("Midshipman",           getText("ranks.honorific.midshipman"));
        rankMap.put("Petty Officer",        getText("ranks.honorific.po"));
        rankMap.put("Chief Petty Officer",  getText("ranks.honorific.chief"));
        rankMap.put("Warrant Officer",      getText("ranks.honorific.warrant"));
        rankMap.put("Ensign",               getText("ranks.honorific.ensign"));
        rankMap.put("Lieutenant",           getText("ranks.honorific.lieutenant"));
        rankMap.put("Lieutenant Commander", getText("ranks.honorific.commander"));
        rankMap.put("Post Commander",       getText("ranks.honorific.commander"));
        rankMap.put("Post Captain",         getText("ranks.honorific.captain"));
        rankMap.put("Rear Admiral",         getText("ranks.honorific.admiral"));
        rankMap.put("Vice Admiral",         getText("ranks.honorific.admiral"));
        rankMap.put("Admiral",              getText("ranks.honorific.admiral"));
        return rankMap;
    }


    /**
     * Returns the federation military rank map int to rank name mapping.
     *
     */
    public static HashMap<Integer, String> getFederationRankMap() {
        HashMap<Integer, String> rankMap = new HashMap<>();
        rankMap.put(0, getText("ranks.federation.none"));
        rankMap.put(1, getText("ranks.federation.recruit"));
        rankMap.put(2, getText("ranks.federation.cadet"));
        rankMap.put(3, getText("ranks.federation.midshipman"));
        rankMap.put(4, getText("ranks.federation.pettyOfficer"));
        rankMap.put(5, getText("ranks.federation.chiefPettyOfficer"));
        rankMap.put(6, getText("ranks.federation.warrant"));
        rankMap.put(7, getText("ranks.federation.ensign"));
        rankMap.put(8, getText("ranks.federation.lieutenant"));
        rankMap.put(9, getText("ranks.federation.lieutenant"));
        rankMap.put(10, getText("ranks.federation.postCommander"));
        rankMap.put(11, getText("ranks.federation.postcaptain"));
        rankMap.put(12, getText("ranks.federation.rearAdmiral"));
        rankMap.put(13, getText("ranks.federation.viceAdmiral"));
        rankMap.put(14, getText("ranks.federation.admiral"));
        return rankMap;
    }


    /**
     * Returns the combat rank map int to rank name mapping.
     *
     */
    public static HashMap<Integer, String> getCombatRankMap() {
        HashMap<Integer, String> rankMap = new HashMap<>();
        rankMap.put(0, getText("rank.harmless"));
        rankMap.put(1, getText("rank.mostlyHarmless"));
        rankMap.put(2, getText("rank.novice"));
        rankMap.put(3, getText("rank.competent"));
        rankMap.put(4, getText("rank.expert"));
        rankMap.put(5, getText("rank.master"));
        rankMap.put(6, getText("rank.dangerous"));
        rankMap.put(7, getText("rank.deadly"));
        rankMap.put(8, getText("rank.elite"));
        rankMap.put(9, getText("rank.elite1"));
        rankMap.put(10, getText("rank.elite2"));
        rankMap.put(11, getText("rank.elite3"));
        rankMap.put(12, getText("rank.elite4"));
        rankMap.put(13, getText("rank.elite5"));
        return rankMap;
    }


    /**
     * Returns the exobiology rank map int to rank name mapping.
     *
     */
    public static HashMap<Integer, String> getExobiologyRankMap() {
        HashMap<Integer, String> rankMap = new HashMap<>();
        rankMap.put(0, getText("rank.exo.directionless"));
        rankMap.put(1, getText("rank.exo.mostlyDirectionless"));
        rankMap.put(2, getText("rank.exo.compiler"));
        rankMap.put(3, getText("rank.exo.cataloguer"));
        rankMap.put(4, getText("rank.exo.taxonomist"));
        rankMap.put(5, getText("rank.exo.knight"));
        rankMap.put(6, getText("rank.exo.ecologist"));
        rankMap.put(7, getText("rank.exo.geneticist"));
        rankMap.put(8, getText("rank.elite"));
        rankMap.put(9, getText("rank.elite1"));
        rankMap.put(10, getText("rank.elite2"));
        rankMap.put(11, getText("rank.elite3"));
        rankMap.put(12, getText("rank.elite4"));
        rankMap.put(13, getText("rank.elite5"));
        return rankMap;
    }


    /**
     * Returns the exploration rank map int to rank name mapping.
     *
     */
    public static HashMap<Integer, String> getExplorationRankMap() {
        HashMap<Integer, String> rankMap = new HashMap<>();
        rankMap.put(0, getText("rank.exp.aimless"));
        rankMap.put(1, getText("rank.exp.mostlyAimless"));
        rankMap.put(2, getText("rank.exp.scout"));
        rankMap.put(3, getText("rank.exp.surveyor"));
        rankMap.put(4, getText("rank.exp.trailblazer"));
        rankMap.put(5, getText("rank.exp.pathfinder"));
        rankMap.put(6, getText("rank.exp.ranger"));
        rankMap.put(7, getText("rank.exp.pioneer"));
        rankMap.put(8, getText("rank.elite"));
        rankMap.put(9, getText("rank.elite1"));
        rankMap.put(10, getText("rank.elite2"));
        rankMap.put(11, getText("rank.elite3"));
        rankMap.put(12, getText("rank.elite4"));
        rankMap.put(13, getText("rank.elite5"));
        return rankMap;
    }


    /**
     * Returns the trade rank map int to rank name mapping.
     */
    public static HashMap<Integer, String> getTradeRankMap() {
        HashMap<Integer, String> rankMap = new HashMap<>();
        rankMap.put(0, getText("rank.trade.penniless"));
        rankMap.put(1, getText("rank.trade.mostlyPenniless"));
        rankMap.put(2, getText("rank.trade.peddler"));
        rankMap.put(3, getText("rank.trade.dealer"));
        rankMap.put(4, getText("rank.trade.merchant"));
        rankMap.put(5, getText("rank.trade.broker"));
        rankMap.put(6, getText("rank.trade.entrepreneur"));
        rankMap.put(7, getText("rank.trade.tycoon"));
        rankMap.put(8, getText("rank.elite"));
        rankMap.put(9, getText("rank.elite1"));
        rankMap.put(10, getText("rank.elite2"));
        rankMap.put(11, getText("rank.elite3"));
        rankMap.put(12, getText("rank.elite4"));
        rankMap.put(13, getText("rank.elite5"));
        return rankMap;
    }


    public static HashMap<Integer, String> getMercenaryRankMap() {
        HashMap<Integer, String> rankMap = new HashMap<>();
        rankMap.put(0, getText("rank.merc.defenceless"));
        rankMap.put(1, getText("rank.merc.mostlyDefenceless"));
        rankMap.put(2, getText("rank.merc.rookie"));
        rankMap.put(3, getText("rank.merc.soldier"));
        rankMap.put(4, getText("rank.merc.gunslinger"));
        rankMap.put(5, getText("rank.merc.warrior"));
        rankMap.put(6, getText("rank.merc.entrepreneur"));
        rankMap.put(7, getText("rank.merc.gladiator"));
        rankMap.put(8, getText("rank.merc.deadeye"));
        rankMap.put(9, getText("rank.merc.eliteI"));
        rankMap.put(10, getText("rank.merc.eliteII"));
        rankMap.put(11, getText("rank.merc.eliteIII"));
        rankMap.put(12, getText("rank.merc.eliteVI"));
        rankMap.put(13, getText("rank.merc.eliteV"));
        return rankMap;
    }


    public static HashMap<String, String> getLocalizedPilotFederationRankMap() {
        HashMap<String, String> rankMap = new HashMap<>();
        rankMap.put("Harmless", getText("rank.merc.defenceless"));
        rankMap.put("Mostly Harmless", getText("rank.merc.mostlyDefenceless"));
        rankMap.put("Novice", getText("rank.merc.rookie"));
        rankMap.put("Competent", getText("rank.merc.soldier"));
        rankMap.put("Expert", getText("rank.merc.gunslinger"));
        rankMap.put("Master", getText("rank.merc.warrior"));
        rankMap.put("Dangerous", getText("rank.merc.entrepreneur"));
        rankMap.put("Deadly", getText("rank.merc.gladiator"));
        rankMap.put("Elite", getText("rank.merc.deadeye"));
        rankMap.put("Elite I", getText("rank.merc.eliteI"));
        rankMap.put("Elite II", getText("rank.merc.eliteII"));
        rankMap.put("Elite III", getText("rank.merc.eliteIII"));
        rankMap.put("Elite IV", getText("rank.merc.eliteVI"));
        rankMap.put("Elite V", getText("rank.merc.eliteV"));
        return rankMap;
    }




/**
 *
 * Defenceless	0	Dominator Suit Body Suit Livery (Bronze)
 * Mostly Defenceless	10,000,000	Dominator Suit Torso, Arms, and Legs Livery (Bronze)
 * Rookie	30,000,000	Dominator Suit Helmet Livery (Bronze)
 * Soldier	60,000,000	Dominator Suit Body Suit Livery (Silver)
 * Gunslinger	125,000,000	Dominator Suit Torso, Arms, and Legs Livery (Silver)
 * Warrior	350,000,000	Dominator Suit Helmet Livery (Silver)
 * Gladiator	520,000,000	Dominator Suit Body Suit Livery (Gold)
 * Deadeye	888,000,000	Dominator Suit Torso, Arms, and Legs Livery (Gold)
 * Elite
 * */


    /**
     * return honorific for imperial or federation depending on which rank is higher.
     */
    public static String getHonorific(int imperial, int federation) {
        if (imperial > federation) {
            return getImperialHonorificMap().get(getImperialRankMap().get(imperial));

        } else if (federation > imperial) {
            return getImperialHonorificMap().get(getFederationRankMap().get(federation));
        } else {
            return chooseAtRandom(imperial, federation);
        }
    }

    private static @NonNull String chooseAtRandom(int imperial, int federation) {
        Random random = new Random();
        int choice = random.nextInt(2); // Returns 0 or 1
        if (choice == 0) {
            return getImperialHonorificMap().get(getImperialRankMap().get(imperial));
        } else {
            return getFederationHonorificMap().get(getFederationRankMap().get(federation));
        }
    }

    public static String getHighestRankAsString(Integer imperial, Integer federation) {
        if (imperial > federation) {
            return getImperialRankMap().get(imperial);
        } else if (federation > imperial) {
            return getFederationRankMap().get(federation);
        } else {
            return new Random().nextBoolean()
                    ? getImperialRankMap().get(imperial)
                    : getFederationRankMap().get(federation);
        }
    }

    public static String getPlayerHonorific() {
        RankAndProgressDto rankDto = PlayerSession.getInstance().getRankAndProgressDto();
        String honorific = getImperialHonorificMap().get(rankDto.getHighestMilitaryRank());
        return honorific != null ? honorific : getText("ranks.honorific.commander");
    }

}
