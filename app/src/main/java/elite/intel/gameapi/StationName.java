package elite.intel.gameapi;

import java.util.Locale;

/**
 * The name of a station as it should be shown and spoken, from the name the journal wrote.
 * <p>
 * Most stations arrive as plain text and pass through untouched. Colonisation ships do not: the journal
 * names one {@code "$EXT_PANEL_ColonisationShip; Schroter's Progress"} - a UI symbol, a semicolon, and
 * only then the name the commander knows it by. Everything downstream treats {@code StationName} as text
 * to display, so the symbol reached the HUD card, the memory line ("docked at $EXT_PANEL_..."), and the
 * companion's mouth.
 * <p>
 * The symbol is not a translation key we hold, and the game does not send a {@code _Localised} sibling for
 * it, so there is nothing to look up - the readable half is already in the string, sitting after the
 * semicolon. This peels it off. Where the symbol is the whole name - an unnamed depot - the symbol itself
 * is turned into words, because a card with a blank subtitle is worse than one saying COLONISATION SHIP.
 * <p>
 * Sibling of {@link JournalSymbol}, which normalises the other direction: that one produces identifiers to
 * join on, this one produces text for a human.
 */
public final class StationName {

    /**
     * Frontier's prefix for the panel-name symbols; carries no meaning once the symbol becomes words.
     */
    private static final String PANEL_PREFIX = "EXT_PANEL_";

    private StationName() {
    }

    /**
     * The station name with any leading {@code $Symbol;} peeled off, or the symbol spelled out as words
     * when it was the entire name. Null and blank pass through unchanged, as does an ordinary name.
     */
    public static String display(String raw) {
        if (raw == null) return null;
        String name = raw.trim();
        if (!name.startsWith("$")) return raw;

        int end = name.indexOf(';');
        if (end < 0) return raw; // not the decorated shape after all - leave it alone

        String remainder = name.substring(end + 1).trim();
        return remainder.isEmpty() ? spellOut(name.substring(1, end)) : remainder;
    }

    /**
     * {@code EXT_PANEL_ColonisationShip} as "Colonisation Ship": the underscored prefix dropped and the
     * camel case broken into words, which is as close to the game's own wording as the symbol can get us.
     */
    private static String spellOut(String symbol) {
        String bare = symbol.toUpperCase(Locale.ROOT).startsWith(PANEL_PREFIX)
                ? symbol.substring(PANEL_PREFIX.length())
                : symbol;
        String spaced = bare.replace('_', ' ').replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ").trim();
        return spaced.isEmpty() ? symbol : spaced;
    }
}
