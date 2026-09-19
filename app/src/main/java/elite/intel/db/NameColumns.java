package elite.intel.db;

import elite.intel.db.dao.CommodityDao;
import elite.intel.db.dao.MaterialNameDao;
import elite.intel.gameapi.GameLanguage;
import elite.intel.i18n.Language;

import java.util.Optional;

/**
 * Which column of the material and commodity catalogues holds a display name in a given language.
 * <p>
 * The catalogues are keyed by the journal symbol and carry one name column per language. Two callers
 * need the same mapping and must never disagree: the fuzzy readers, which look a spoken word up in the
 * commander's language, and the learners, which file a name the journal just supplied under the
 * <em>game client's</em> language - the one it was written in. The client language is a de-facto anchor:
 * the app language is a click to change, the game's is a reinstall.
 * <p>
 * Writers take a column ({@link #material}, {@link #commodity}); readers take a spoken-form expression
 * ({@link #materialSpoken}, {@link #commoditySpoken}), which differs for English only: the English column
 * of a learned row may hold the bare symbol as a stand-in (see migration 01054), and the expression hides it.
 */
public final class NameColumns {

    private NameColumns() {
    }

    /**
     * The {@code material_names} column for a language. English is the {@code name} column.
     */
    public static String material(Language lang) {
        return switch (lang) {
            case DE -> "name_de";
            case FR -> "name_fr";
            case ES -> "name_es";
            case RU -> "name_ru";
            case UK -> "name_uk";
            case IT -> "name_it";
            case PT -> "name_pt";
            case PTBZ -> "name_ptbz";
            default -> "name";
        };
    }

    /**
     * The {@code commodities} column for a language. English is the {@code commodity} column.
     */
    public static String commodity(Language lang) {
        return switch (lang) {
            case DE -> "commodity_de";
            case FR -> "commodity_fr";
            case ES -> "commodity_es";
            case RU -> "commodity_ru";
            case UK -> "commodity_uk";
            case IT -> "commodity_it";
            case PT -> "commodity_pt";
            case PTBZ -> "commodity_ptbz";
            default -> "commodity";
        };
    }

    /**
     * The material column the game client writes its {@code Name_Localised} in, or empty when the client
     * language is not known. Empty means a learner must not guess a column.
     */
    public static Optional<String> gameMaterial() {
        return GameLanguage.getInstance().language().map(NameColumns::material);
    }

    /**
     * The commodity column the game client writes its {@code Name_Localised} in - see {@link #gameMaterial()}.
     */
    public static Optional<String> gameCommodity() {
        return GameLanguage.getInstance().language().map(NameColumns::commodity);
    }

    /**
     * The SQL expression that reads a material's spoken name in a language - the column, or for English
     * {@link MaterialNameDao#ENGLISH_NAME}, so a learned row's symbol stand-in reads as no name at all.
     */
    public static String materialSpoken(Language lang) {
        return lang == Language.EN ? MaterialNameDao.ENGLISH_NAME : material(lang);
    }

    /**
     * The SQL expression that reads a commodity's spoken name in a language - see {@link #materialSpoken}.
     */
    public static String commoditySpoken(Language lang) {
        return lang == Language.EN ? CommodityDao.ENGLISH_NAME : commodity(lang);
    }

    /**
     * The expression that reads the word the game client shows for a material. An unknown client language
     * reads as English: for a reader, a wrong guess costs nothing worse than an English word.
     */
    public static String gameMaterialSpoken() {
        return materialSpoken(GameLanguage.getInstance().language().orElse(Language.EN));
    }

    public static String gameCommoditySpoken() {
        return commoditySpoken(GameLanguage.getInstance().language().orElse(Language.EN));
    }
}
