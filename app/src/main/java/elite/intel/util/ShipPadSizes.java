package elite.intel.util;

import java.util.Locale;
import java.util.Map;

/**
 * The landing pad a ship needs, and the pads it will fit on.
 * <p>
 * WHY both spellings are in the table: the callers pass {@code ShipDao.Ship#getShipIdentifier}, which is
 * the journal's own internal ship name ({@code asp}, {@code federation_corvette}, {@code krait_mkii}), while
 * the table was originally written in display names ({@code asp explorer}, {@code federal corvette}). A
 * lookup that missed fell through to {@code L}, so an Asp Explorer was quietly treated as a large ship and
 * shown only large-pad stations - fewer answers than it deserves on every station search there is. The
 * internal names are the ones the game actually says; the display names are kept because they cost nothing
 * and a caller holding one still gets a right answer.
 * <p>
 * The default stays {@code L} for a ship neither spelling knows - a newly released hull, most likely. Being
 * offered only large pads costs a commander some nearby options; being sent to a pad the ship cannot land on
 * costs them the trip.
 * <p>
 * Keep this in step with the {@code ship_make} table (migration {@code 01010__schema.sql}), which is the same
 * list of hulls under the same internal names: a ship the game knows and this table does not is a ship whose
 * pad size is being guessed.
 */
public class ShipPadSizes {

    public static final String SMALL = "S";
    public static final String MEDIUM = "M";
    public static final String LARGE = "L";

    public static final Map<String, String> PAD_SIZES = Map.ofEntries(
            // Small pads - journal internal names
            Map.entry("sidewinder", SMALL),
            Map.entry("eagle", SMALL),
            Map.entry("hauler", SMALL),
            Map.entry("adder", SMALL),
            Map.entry("empire_eagle", SMALL),
            Map.entry("viper", SMALL),
            Map.entry("viper_mkiv", SMALL),
            Map.entry("diamondback", SMALL),
            Map.entry("diamondbackxl", SMALL),
            Map.entry("dolphin", SMALL),
            Map.entry("empire_courier", SMALL),
            Map.entry("vulture", SMALL),
            Map.entry("cobramkiii", SMALL),
            Map.entry("cobramkiv", SMALL),
            Map.entry("cobramkv", SMALL),
            Map.entry("smallcombat01_nx", SMALL),

            // Medium pads - journal internal names
            Map.entry("type6", MEDIUM),
            Map.entry("independant_trader", MEDIUM),
            Map.entry("asp_scout", MEDIUM),
            Map.entry("asp", MEDIUM),
            Map.entry("federation_dropship", MEDIUM),
            Map.entry("federation_dropship_mkii", MEDIUM),
            Map.entry("federation_gunship", MEDIUM),
            Map.entry("mandalay", MEDIUM),
            Map.entry("typex", MEDIUM),
            Map.entry("typex_2", MEDIUM),
            Map.entry("typex_3", MEDIUM),
            Map.entry("krait_mkii", MEDIUM),
            Map.entry("krait_light", MEDIUM),
            Map.entry("type8", MEDIUM),
            Map.entry("ferdelance", MEDIUM),
            Map.entry("mamba", MEDIUM),
            Map.entry("python", MEDIUM),
            Map.entry("python_nx", MEDIUM),
            Map.entry("lakonminer", MEDIUM),
            Map.entry("corsair", MEDIUM),

            // Large pads - journal internal names
            Map.entry("type7", LARGE),
            Map.entry("empire_trader", LARGE),
            Map.entry("orca", LARGE),
            Map.entry("type9", LARGE),
            Map.entry("belugaliner", LARGE),
            Map.entry("type9_military", LARGE),
            Map.entry("anaconda", LARGE),
            Map.entry("federation_corvette", LARGE),
            Map.entry("cutter", LARGE),
            Map.entry("panthermkii", LARGE),
            Map.entry("explorer_nx", LARGE),
            Map.entry("mediumtransport01", LARGE),

            // Display names, for a caller holding one of those instead
            Map.entry("imperial eagle", SMALL),
            Map.entry("viper mkiii", SMALL),
            Map.entry("diamondback scout", SMALL),
            Map.entry("diamondback explorer", SMALL),
            Map.entry("imperial courier", SMALL),
            Map.entry("type-6 transporter", MEDIUM),
            Map.entry("keelback", MEDIUM),
            Map.entry("asp scout", MEDIUM),
            Map.entry("asp explorer", MEDIUM),
            Map.entry("federal dropship", MEDIUM),
            Map.entry("alliance chieftain", MEDIUM),
            Map.entry("federal assault ship", MEDIUM),
            Map.entry("alliance crusader", MEDIUM),
            Map.entry("alliance challenger", MEDIUM),
            Map.entry("federal gunship", MEDIUM),
            Map.entry("krait phantom", MEDIUM),
            Map.entry("type-8 transporter", MEDIUM),
            Map.entry("krait mkii", MEDIUM),
            Map.entry("fer-de-lance", MEDIUM),
            Map.entry("python mkii", MEDIUM),
            Map.entry("type-11 prospector", MEDIUM),
            Map.entry("type-7 transporter", LARGE),
            Map.entry("imperial clipper", LARGE),
            Map.entry("type-9 heavy", LARGE),
            Map.entry("beluga liner", LARGE),
            Map.entry("type-10 defender", LARGE),
            Map.entry("federal corvette", LARGE),
            Map.entry("imperial cutter", LARGE),
            Map.entry("panther clipper mkii", LARGE),
            Map.entry("caspian explorer", LARGE),
            Map.entry("kestrel mk ii", SMALL),
            Map.entry("lynx highliner", LARGE)
    );

    /**
     * The smallest pad {@code shipName} can land on, as {@code S}, {@code M} or {@code L}.
     */
    public static String getPadSize(String shipName) {
        if (shipName == null) return LARGE;
        return PAD_SIZES.getOrDefault(shipName.trim().toLowerCase(Locale.ROOT), LARGE);
    }

    /**
     * Whether a ship needing {@code padSize} can put down at a station with these pad counts.
     * <p>
     * A ship fits any pad its own size or bigger and never the other way round, so a small ship is welcome
     * anywhere that has a pad at all and only a large ship is actually constrained. This is a station-by-
     * station test rather than a search filter because Spansh cannot express it as one: its pad counts are
     * three separate range filters that AND together, and "medium OR large" is not something it will answer.
     * The counts are worth trusting individually - measured live, there really are stations with a large pad
     * and no medium one, so "has a large pad" does not imply "has a medium pad".
     */
    public static boolean canDock(String padSize, int smallPads, int mediumPads, int largePads) {
        if (LARGE.equals(padSize)) return largePads > 0;
        if (MEDIUM.equals(padSize)) return mediumPads > 0 || largePads > 0;
        return smallPads > 0 || mediumPads > 0 || largePads > 0;
    }
}
