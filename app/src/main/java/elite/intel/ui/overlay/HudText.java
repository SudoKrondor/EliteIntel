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
     * Credits, which is what most figures on a card are.
     */
    static String credits(long value) {
        return amount(value, "overlay.card.unit.credits");
    }

    /**
     * A counted string, taking the plural form the count calls for - which is three-way in Russian
     * and Ukrainian, so "2 missions" and "5 missions" are not the same word.
     *
     * @param keyBase key without its category suffix, e.g. {@code overlay.card.value.missionCount}
     */
    static String plural(String keyBase, int count) {
        Language language = SystemSession.getInstance().getLanguage();
        // The count is pre-rendered: MessageFormat would group it by its own locale rules, and these
        // are small counts that no language groups anyway.
        String rendered = String.valueOf(count);
        String key = keyBase + StringUtls.pluralSuffix(language, count);
        String text = MultiLingualTextProvider.getText(language, key, rendered);
        if (!text.equals(key)) return text;

        // WHY this fallback: a category a language declares nowhere resolves to the key itself, and
        // English cannot cover for it because English has no word for ".few" and never declares one. The
        // card would then read "overlay.card.value.missionCount.few" mid-row. A plural in the wrong case
        // is worse text but still text, so degrade to it. HudCardLocalizationTest is what keeps this
        // path unreached: it checks every category each language's own rule can select.
        return MultiLingualTextProvider.getText(language, keyBase + ".many", rendered);
    }
}
