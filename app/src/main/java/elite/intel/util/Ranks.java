package elite.intel.util;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.List;
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

    /**
     * One rank tier: the English name the journal reports and its i18n key.
     */
    private record RankTier(String englishName, String i18nKey) {
    }

    /**
     * Federation navy ranks ordered by journal rank number: the list index equals the number the
     * journal reports for that rank (index 0 = unranked). Single source of truth for both the
     * index-to-name lookup used by promotion announcements and the name-to-key lookup used by
     * {@link #getLocalizedRankName}.
     */
    private static final List<RankTier> FEDERATION_RANKS = List.of(
            new RankTier("none", "ranks.federation.none"),
            new RankTier("Recruit", "ranks.federation.recruit"),
            new RankTier("Cadet", "ranks.federation.cadet"),
            new RankTier("Midshipman", "ranks.federation.midshipman"),
            new RankTier("Petty Officer", "ranks.federation.pettyOfficer"),
            new RankTier("Chief Petty Officer", "ranks.federation.chiefPettyOfficer"),
            new RankTier("Warrant Officer", "ranks.federation.warrantOfficer"),
            new RankTier("Ensign", "ranks.federation.ensign"),
            new RankTier("Lieutenant", "ranks.federation.lieutenant"),
            new RankTier("Lieutenant Commander", "ranks.federation.lieutenantCommander"),
            new RankTier("Post Commander", "ranks.federation.postCommander"),
            new RankTier("Post Captain", "ranks.federation.postCaptain"),
            new RankTier("Rear Admiral", "ranks.federation.rearAdmiral"),
            new RankTier("Vice Admiral", "ranks.federation.viceAdmiral"),
            new RankTier("Admiral", "ranks.federation.admiral")
    );

    /**
     * Imperial navy ranks ordered by journal rank number: the list index equals the number the
     * journal reports for that rank (index 0 = unranked).
     */
    private static final List<RankTier> IMPERIAL_RANKS = List.of(
            new RankTier("none", "ranks.imperial.none"),
            new RankTier("Outsider", "ranks.imperial.outsider"),
            new RankTier("Serf", "ranks.imperial.serf"),
            new RankTier("Master", "ranks.imperial.master"),
            new RankTier("Squire", "ranks.imperial.squire"),
            new RankTier("Knight", "ranks.imperial.knight"),
            new RankTier("Lord", "ranks.imperial.lord"),
            new RankTier("Baron", "ranks.imperial.baron"),
            new RankTier("Viscount", "ranks.imperial.viscount"),
            new RankTier("Count", "ranks.imperial.count"),
            new RankTier("Earl", "ranks.imperial.earl"),
            new RankTier("Marquis", "ranks.imperial.marquis"),
            new RankTier("Duke", "ranks.imperial.duke"),
            new RankTier("Prince", "ranks.imperial.prince"),
            new RankTier("King", "ranks.imperial.king")
    );

    /**
     * English rank name to i18n key, derived from the ordered rank lists (the "none" tier is excluded).
     */
    private static final Map<String, String> RANK_I18N_KEY_MAP = buildNameToKeyMap();

    /**
     * Legal states, keyed by the journal's English value reduced to letters only (see {@link #legalStatusKey}).
     * The same vocabulary reaches us from two journal sources - a scanned ship (ShipTargeted.LegalStatus) and
     * the commander's own standing (Status.json LegalState) - which are not guaranteed to agree on case or
     * spacing (e.g. {@code IllegalCargo} versus {@code Illegal cargo}), so the lookup normalizes rather than
     * relying on an exact match.
     */
    private static final Map<String, String> LEGAL_STATUS_I18N_KEY_MAP = Map.ofEntries(
            Map.entry("clean", "event.target.legal.clean"),
            Map.entry("wanted", "event.target.legal.wanted"),
            Map.entry("hostile", "event.target.legal.hostile"),
            Map.entry("lawless", "event.target.legal.lawless"),
            Map.entry("allied", "event.target.legal.allied"),
            Map.entry("illegalcargo", "event.target.legal.illegalCargo")
    );

    /**
     * Returns the localized display name for the given English legal status sourced from the game journal
     * (e.g. {@code Clean}, {@code Wanted}, {@code Hostile}, {@code Allied}, {@code IllegalCargo}).
     * Falls back to the original value (with underscores replaced by spaces) if no translation key is registered.
     * Returns {@code null} for {@code null} or blank input so callers can filter it out.
     */
    public static String getLocalizedLegalStatus(String englishLegalStatus) {
        if (englishLegalStatus == null || englishLegalStatus.isBlank()) {
            return null;
        }
        String key = LEGAL_STATUS_I18N_KEY_MAP.get(legalStatusKey(englishLegalStatus));
        return key != null ? getText(key) : englishLegalStatus.replace("_", " ");
    }

    /**
     * Reduces a journal legal-status value to letters only, so case, spaces and underscores cannot miss.
     */
    private static String legalStatusKey(String englishLegalStatus) {
        return englishLegalStatus.replaceAll("[^A-Za-z]", "").toLowerCase(java.util.Locale.ROOT);
    }

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
        return buildIndexToLocalizedMap(IMPERIAL_RANKS);
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
        return buildIndexToLocalizedMap(FEDERATION_RANKS);
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
            return getFederationHonorificMap().get(getFederationRankMap().get(federation));
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

    public static String getPlayerHonorific(Integer imperial, Integer federation) {
        if (imperial >= federation) {
            return getImperialHonorificMap().get(getImperialRankMap().get(imperial));
        } else {
            return getFederationHonorificMap().get(getFederationRankMap().get(federation));
        }
    }

    /**
     * Builds an index-to-localized-name map from an ordered rank list, resolving each key against
     * the active UI language at call time so the result tracks a mid-session language switch.
     */
    private static HashMap<Integer, String> buildIndexToLocalizedMap(List<RankTier> ranks) {
        HashMap<Integer, String> rankMap = new HashMap<>();
        for (int index = 0; index < ranks.size(); index++) {
            rankMap.put(index, getText(ranks.get(index).i18nKey()));
        }
        return rankMap;
    }

    private static Map<String, String> buildNameToKeyMap() {
        Map<String, String> map = new HashMap<>();
        addNames(map, IMPERIAL_RANKS);
        addNames(map, FEDERATION_RANKS);
        return Map.copyOf(map);
    }

    private static void addNames(Map<String, String> map, List<RankTier> ranks) {
        for (RankTier tier : ranks) {
            if ("none".equals(tier.englishName())) continue; // the unranked tier has no journal rank name to localize
            map.put(tier.englishName(), tier.i18nKey());
        }
    }

}
