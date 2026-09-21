package elite.intel.gameapi.search;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.gameapi.search.spansh.station.StationSearchHit;
import elite.intel.session.PlayerSession;
import elite.intel.util.Ranks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Drops every search hit in a permit-locked system the commander cannot jump to.
 *
 * <p>The one filter every search that ends in a plotted route runs its candidates through - commodity,
 * module, fuel, trader, broker, factors, Vista, ring, brain tree, hunting ground, war zone, carrier -
 * so the rule about who may enter which system lives here once. The trade-route search is the exception
 * on purpose: Spansh's router takes "allow permit systems" as a parameter, and that is the commander's
 * own setting.
 *
 * <p>WHY it is applied to the candidates and not to the winner: a route search ranks its hits and
 * plots the first. If the first is Sol and the commander has no Federation rank, vetoing the plot would
 * report "nothing found" while the second-best hit sat there unused. Filtered before the ranking, the
 * next station out simply wins.
 *
 * <p>Four kinds of lock, told apart by what the app can know (source: the Elite Dangerous wiki's Permits
 * tables, read 2026-09-20):
 * <ul>
 *   <li><b>Navy rank.</b> Sol needs a Federation Petty Officer, Achenar an Imperial Squire, and so on.
 *   The journal reports both navy ranks, so these are checked.</li>
 *   <li><b>Elite rank.</b> Shinrarta Dezhra, the Founders' World, opens at Elite in any career. Checked
 *   against the highest career rank the journal has reported.</li>
 *   <li><b>Something the journal does not say.</b> Alioth, Sirius and the like are granted by a minor
 *   faction to its allies through a permit mission; CD-43 11917 by a CQC standing the journal does not
 *   report; a handful of inhabited-bubble systems by nobody at all. These are taboo - unless the commander
 *   has demonstrably been there, which is the one proof there is.</li>
 *   <li><b>Closed regions.</b> Whole sectors - Col 70, the Praei and Bleia clusters, the Horsehead Dark
 *   Region - that no permit opens. Uninhabited, so no station search finds them, but a ring or a brain
 *   tree can sit in one. Matched by the sector prefix of the procedural system name.</li>
 * </ul>
 *
 * <p>A visit settles every kind: a system the location ledger holds a row for is one the commander has
 * jumped into, and a permit, once granted, is not taken back. The ledger is journal-fed only, so the
 * evidence is first-hand.
 */
public final class PermitLockedSystems {

    private static final Logger log = LogManager.getLogger(PermitLockedSystems.class);

    /**
     * The Pilots Federation's own system: Jameson Memorial stocks every ship and module in the game, so it
     * is the answer to most outfitting searches - for the commanders allowed in.
     */
    static final String FOUNDERS_WORLD = "shinrarta dezhra";

    enum Navy {
        FEDERATION, EMPIRE
    }

    /**
     * The navy rank a system's permit is issued at.
     */
    record NavyLock(Navy navy, int rank) {
    }

    /**
     * Systems whose permit comes with a navy rank, keyed by lower-case name. The rank is looked up by
     * its English name so the table reads as the game's own wording and fails at class load if a name
     * is misspelled.
     */
    private static final Map<String, NavyLock> NAVY_LOCKED = Map.ofEntries(
            Map.entry("sol", federation("Petty Officer")),
            Map.entry("beta hydri", federation("Chief Petty Officer")),
            Map.entry("vega", federation("Chief Petty Officer")),
            Map.entry("plx 695", federation("Warrant Officer")),
            Map.entry("ross 128", federation("Ensign")),
            Map.entry("exbeur", federation("Lieutenant")),
            Map.entry("hors", federation("Post Commander")),
            Map.entry("achenar", empire("Squire")),
            Map.entry("summerland", empire("Baron")),
            Map.entry("facece", empire("Earl"))
    );

    /**
     * Systems whose permit turns on something the journal never reports - a minor faction's permit
     * mission, a CQC standing, or nothing anyone has found - so they are withheld outright. Lower-case.
     */
    private static final Set<String> UNPROVABLE_LOCKED = Set.of(
            // Faction permit missions, by allegiance of the holder.
            "4 sextantis", "cd-44 1695", "hip 54530", "lft 509", "mingfu", "witch's reach",
            "ltt 198",
            "alioth",
            "crom", "hodack", "isinor", "jotun", "luyten 347-14", "mbooni", "nastrond", "peregrina",
            "pi mensae", "sirius", "terra mater", "tiliala", "van maanen's star",
            // Locked by a CQC standing the journal does not carry.
            "cd-43 11917",
            // Held by no faction and opened by nothing.
            "alpha hydri", "bellica", "dryio flyuae ic-b c1-377", "hip 10332", "hip 104941", "hip 22182",
            "hip 39425", "hip 51073", "hip 87621", "hr 4413", "lhs 2894", "lhs 2921", "lhs 3091",
            "plaa ain ha-z d46", "polaris", "ross 354", "scheau bli nb-o d6-1409", "wolf 262",
            // Listed as permit-locked in older community lists but absent from the wiki's current tables.
            "vanayequi", "jaroua"
    );

    /**
     * Sectors closed in their entirety, as the prefix of every system name inside them ("Col 70 Sector
     * FY-N c21-3", "Praei3 AB-C d1-4"). Matched on the prefix followed by a space so "Dryman" does not
     * swallow "Drymanni".
     */
    private static final List<String> CLOSED_REGIONS = List.of(
            "bleia1", "bleia2", "bleia3", "bleia4", "bleia5",
            "bovomit",
            "col 70 sector", "col 97 sector", "col 121 sector",
            "cone sector",
            "dryman",
            "formorian frontier",
            "froadik",
            "horsehead dark region",
            "hyponia",
            "ic 4673 sector",
            "m41 sector",
            "ngc 1647 sector", "ngc 2264 sector", "ngc 2286 sector", "ngc 3603 sector",
            "praei1", "praei2", "praei3", "praei4", "praei5", "praei6",
            "regor sector",
            "sidgoir"
    );

    private PermitLockedSystems() {
    }

    private static NavyLock federation(String rankName) {
        return new NavyLock(Navy.FEDERATION, Ranks.federationRankNumber(rankName));
    }

    private static NavyLock empire(String rankName) {
        return new NavyLock(Navy.EMPIRE, Ranks.imperialRankNumber(rankName));
    }

    /**
     * The given hits minus every one in a system the commander cannot enter. Never null; may be empty,
     * which callers treat as "nothing found" like any other empty page.
     *
     * @param systemOf how to read a hit's system name; a hit whose name is null is kept, since it is not
     *                 known to be locked and the caller's own null checks will judge it
     */
    public static <T> List<T> reachable(List<T> hits, Function<T, String> systemOf) {
        if (hits == null) return List.of();
        return hits.stream().filter(hit -> isReachable(systemOf.apply(hit))).toList();
    }

    /**
     * {@link #reachable(List, Function)} for the station searches that share {@link StationSearchHit}.
     */
    public static <T extends StationSearchHit> List<T> reachable(List<T> hits) {
        return reachable(hits, StationSearchHit::getSystemName);
    }

    /**
     * True when the commander may jump to {@code systemName}: it is not permit-locked, or the lock is
     * one they meet, or they have been there before.
     */
    public static boolean isReachable(String systemName) {
        if (systemName == null || !isLocked(systemName)) return true;
        RankAndProgressDto ranks = PlayerSession.getInstance().getRankAndProgressDto();
        boolean reachable = isReachable(systemName, ranks.getCombatRankFederation(), ranks.getCombatRankEmpire(),
                ranks.hasEliteRank(), PermitLockedSystems::visited);
        if (!reachable) log.debug("Dropping {}: permit-locked and the commander does not hold it", systemName);
        return reachable;
    }

    /**
     * The verdict with every input in hand, so the rule can be tested without a session.
     *
     * @param federationRank the Federation navy rank number, -1 when unknown
     * @param empireRank     the Imperial navy rank number, -1 when unknown
     * @param elite          whether the commander is Elite in any career
     * @param visited        whether the commander has been to a system, asked only for a locked one the
     *                       ranks do not open
     */
    static boolean isReachable(String systemName, Integer federationRank, Integer empireRank, boolean elite,
                               Predicate<String> visited) {
        String key = key(systemName);
        if (!isLocked(key)) return true;
        NavyLock lock = NAVY_LOCKED.get(key);
        if (lock != null && rankOf(lock.navy(), federationRank, empireRank) >= lock.rank()) return true;
        if (FOUNDERS_WORLD.equals(key) && elite) return true;
        return visited.test(systemName);
    }

    /**
     * True when the system is on any of the lists or inside a closed region - the cheap test that spares
     * every ordinary hit the session and ledger lookups.
     */
    static boolean isLocked(String systemName) {
        String key = key(systemName);
        if (NAVY_LOCKED.containsKey(key) || FOUNDERS_WORLD.equals(key) || UNPROVABLE_LOCKED.contains(key)) return true;
        for (String region : CLOSED_REGIONS) {
            if (key.startsWith(region + " ")) return true;
        }
        return false;
    }

    private static int rankOf(Navy navy, Integer federationRank, Integer empireRank) {
        Integer rank = navy == Navy.FEDERATION ? federationRank : empireRank;
        // -1 stands for "not reported yet" and must sit below every real tier rather than blow up on unboxing.
        return rank == null ? -1 : rank;
    }

    /**
     * A location row filed under the system is proof of a visit: every writer of that ledger is a journal
     * subscriber reporting where the ship actually is.
     */
    private static boolean visited(String systemName) {
        return !LocationManager.getInstance().findByPrimaryStar(systemName).isEmpty();
    }

    private static String key(String systemName) {
        return systemName.strip().toLowerCase(Locale.ROOT);
    }
}
