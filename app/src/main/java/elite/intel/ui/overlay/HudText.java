package elite.intel.ui.overlay;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ui.i18n.LocalizedNumbers;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.util.StringUtls;

/**
 * The words on an objective card - titles, row labels, units - in the commander's own language.
 * <p>
 * Everything else on a card is data: system names, commodities, factions and genus names come from
 * the journal already in whatever language the game client is running, and are passed through
 * untouched. Only the words this app writes itself are looked up here.
 * <p>
 * WHY the length of a translation matters: the renderer draws the label from the left edge and the
 * value from the right, and on a progress row the bar starts at a fixed offset (150px at scale 1.0,
 * see {@code hud_render.c}). Nothing clips or wraps - a label that outgrows its column runs into the
 * bar. Keep these terse, and abbreviate rather than let a label read as a sentence.
 */
final class HudText {

    private HudText() {
    }

    /**
     * A card string by key, e.g. {@code overlay.card.row.reward}.
     */
    static String get(String key, Object... args) {
        return MultiLingualTextProvider.getText(key, args);
    }

    /**
     * A figure with its unit, e.g. {@code 1.500.000 cr} in Italian.
     * <p>
     * One place decides how the two are joined, because the join is a display decision rather than an
     * arithmetic one: a value and its unit should never be separated, and if that ever calls for a
     * non-breaking space it has to change everywhere at once or the cards stop matching each other.
     */
    static String amount(long value, String unitKey) {
        return LocalizedNumbers.grouped(value) + " " + get(unitKey);
    }

    /**
     * A bare figure - a count of something the row's label already names, so it carries no unit.
     * Grouped all the same: a stack can run to four digits, and a commander reading 1,000 in one row
     * and 1.000 in the next would be reading two different numbers.
     */
    static String count(long value) {
        return LocalizedNumbers.grouped(value);
    }

    /**
     * Credits, which is what most figures on a card are.
     */
    static String credits(long value) {
        return amount(value, "overlay.card.unit.credits");
    }

    /**
     * A counted string, taking the plural form the count calls for.
     *
     * @param keyBase key without its category suffix, e.g. {@code overlay.card.value.missionCount}
     */
    static String plural(String keyBase, int count) {
        Language language = SystemSession.getInstance().getLanguage();
        // The count is pre-rendered: MessageFormat would group it by its own locale rules, and these
        // are small counts that no language groups anyway.
        String rendered = String.valueOf(count);
        return MultiLingualTextProvider.getText(language, keyBase + StringUtls.pluralSuffix(count), rendered);
    }
}
