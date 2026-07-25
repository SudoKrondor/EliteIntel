package elite.intel.db;

import elite.intel.db.dao.CommodityDao;
import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.dao.SubSystemDao;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FuzzySearch {

    /**
     * Spoken stand-in for a material the catalogue does not know. See {@link #localizedMaterialName}.
     */
    private static final String UNKNOWN_MATERIAL = "query.materials.unknownName";

    public static final SystemSession systemSession = SystemSession.getInstance();

    public static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1] + costOfSubstitution(s1.charAt(i - 1), s2.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private static int min(int... numbers) {
        return java.util.Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }


    public static String fuzzyCommodityMatch(String input, int similarity) {
        Language lang = SystemSession.getInstance().getLanguage();
        if (lang == Language.EN) {
            return fuzzyMatch(input, similarity, CommodityDao.class, CommodityDao::getAllNamesLowerCase, CommodityDao::getOriginalCase);
        }
        String col = commodityColumn(lang);
        return fuzzyMatch(input, similarity, CommodityDao.class,
                dao -> dao.getAllLocalizedNamesLowerCase(col),
                (dao, name) -> dao.getEnglishByLocalizedName(col, name));
    }

    /**
     * Resolves the localized display name for an English commodity name (e.g. the
     * lowercase {@code Type} field from a journal event). Returns the localized name
     * for the current language, or the original {@code englishName} when the game is
     * in English or no localized version exists in the DB.
     */
    public static String localizedCommodityName(String englishName) {
        if (englishName == null || englishName.isBlank()) return englishName;
        Language lang = systemSession.getLanguage();
        if (lang == Language.EN) return englishName;
        String col = commodityColumn(lang);
        String localized = Database.withDao(CommodityDao.class, dao -> dao.getLocalizedByEnglishName(col, englishName));
        return (localized == null || localized.isBlank()) ? englishName : localized;
    }

    /**
     * Resolves the non-localized game symbol (FDevIDs {@code symbol}) for an English
     * commodity name (typically the result of {@link #fuzzyCommodityMatch}). This is the
     * value to compare — case-insensitively — against a Cargo event's {@code Name} field,
     * which the journal writes lower-cased (e.g. "atmosphericextractors" for the symbol
     * "AtmosphericExtractors"). Returns {@code null} for legacy goods with no known symbol.
     */
    public static String commoditySymbol(String englishName) {
        if (englishName == null || englishName.isBlank()) return null;
        return Database.withDao(CommodityDao.class, dao -> dao.getSymbolByEnglishName(englishName));
    }

    /**
     * Resolves a spoken material name to its journal symbol (e.g. {@code focuscrystals}), matching
     * against the commander's own language plus any aliases. Returns {@code null} when nothing clears
     * the threshold.
     * <p>
     * The symbol, not the display name, is the useful result: it is what every inventory row is keyed
     * by and what the journal reports.
     */
    public static String fuzzyMaterialSymbol(String input, int similarity) {
        Language lang = systemSession.getLanguage();
        String col = materialNameColumn(lang);
        String tag = languageTag(lang);
        return fuzzyMatch(input, similarity, MaterialNameDao.class,
                dao -> dao.getAllSpokenFormsLowerCase(col, tag),
                (dao, spoken) -> dao.getSymbolBySpokenForm(col, tag, spoken));
    }

    /**
     * Resolves a spoken material name to its canonical English name. Used where the material name is a
     * lookup key into English reference data — Spansh brain-tree records, for instance — rather than
     * something spoken back to the commander.
     */
    public static String fuzzyMaterialNameSearch(String input, int similarity) {
        String symbol = fuzzyMaterialSymbol(input, similarity);
        if (symbol == null) return null;
        MaterialNameDao.Material material = Database.withDao(MaterialNameDao.class, dao -> dao.findBySymbol(symbol));
        return material == null ? null : material.getName();
    }

    /**
     * The material name to speak, given its journal symbol.
     * <p>
     * Frontier localizes only English, German, Spanish, French, Russian and Brazilian Portuguese. A
     * commander on any other language is necessarily running an English client, so naming the material
     * in their spoken language would name something their HUD does not show — they get English instead.
     * See {@link Language#isGameLocalized()}.
     */
    public static String localizedMaterialName(String symbol) {
        if (symbol == null || symbol.isBlank()) return StringUtls.localizedLlm(UNKNOWN_MATERIAL);
        Language lang = systemSession.getLanguage();
        String col = lang.isGameLocalized() ? materialNameColumn(lang) : "name";
        String name = Database.withDao(MaterialNameDao.class, dao -> dao.getLocalizedNameBySymbol(col, symbol));
        // WHY: the result is spoken aloud, so an unregistered symbol must not leak the raw journal
        // token ("guardian_powercell") into speech. Degrading to a localized "unknown material" keeps
        // the reply intelligible; the amount and capacity around it are still accurate.
        return (name == null || name.isBlank()) ? StringUtls.localizedLlm(UNKNOWN_MATERIAL) : name;
    }

    public static String fuzzySubSystemSearch(String input, int similarity) {
        return fuzzyMatch(input, similarity, SubSystemDao.class, SubSystemDao::getAllNamesLowerCase, SubSystemDao::getOriginalCase);
    }

    private static String materialNameColumn(Language lang) {
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
     * The {@code material_aliases.lang} value for a language, matching migration 01017.
     */
    private static String languageTag(Language lang) {
        return lang.name().toLowerCase();
    }

    private static String commodityColumn(Language lang) {
        return switch (lang) {
            case DE -> "commodity_de";
            case FR -> "commodity_fr";
            case ES -> "commodity_es";
            case RU -> "commodity_ru";
            case UK -> "commodity_uk";
            case IT -> "commodity_it";
            default -> "commodity";
        };
    }



    /// re-use for other fuzzy search
    private static <T> String fuzzyMatch(String input, int similarity,
                                         Class<T> daoClass,
                                         Function<T, List<String>> candidatesProvider,
                                         BiFunction<T, String, String> originalCaseProvider) {
        if (input == null || input.isBlank()) return null;

        final String lowerInput = input.trim().toLowerCase();
        List<String> candidates = Database.withDao(daoClass, candidatesProvider);

        // Pass 1: prefix match.
        // Pure Levenshtein cannot match a short input like "cmm" to a long candidate
        // like "cmm composites" (distance=11) within any useful threshold, while short
        // unrelated words like "tea" (distance=3) sneak in. If the input is an unambiguous
        // prefix of a candidate name, return the shortest (most specific) prefix match
        // directly, bypassing Levenshtein.
        String bestPrefix = null;
        int bestPrefixLen = Integer.MAX_VALUE;
        for (String c : candidates) {
            if (c.toLowerCase().startsWith(lowerInput)) {
                if (c.length() < bestPrefixLen) {
                    bestPrefixLen = c.length();
                    bestPrefix = c;
                }
            }
        }
        if (bestPrefix != null) {
            final String finalBestPrefix = bestPrefix;
            return Database.withDao(daoClass, dao -> originalCaseProvider.apply(dao, finalBestPrefix));
        }

        ///NOTE
        // Pass 2: Levenshtein fallback for typos/near-matches.
        // Threshold is fully dynamic so it scales correctly for both short single words
        // ("бор" = 3 chars -> tight budget) and long multi-word names
        // ("Специальные микропрограммы..." = 47 chars -> generous budget).
        // Lower bound: max(caller's similarity, len/3) – ensures long names get enough room.
        // Upper bound: max(2, len/2) – prevents short words from accepting unrelated matches
        // e.g. "хрома"(5) gets cap=2, so dist=4 to "бор" is rejected.
        int effectiveSimilarity = Math.min(
                Math.max(similarity, lowerInput.length() / 3),
                Math.max(2, lowerInput.length() / 2)
        );
        String bestLower = null;
        int bestDist = Integer.MAX_VALUE;
        for (String c : candidates) {
            int dist = levenshteinDistance(lowerInput, c.toLowerCase());
            if (dist < bestDist) {
                bestDist = dist;
                bestLower = c;
            }
        }
        if (bestDist <= effectiveSimilarity && bestLower != null) {
            final String finalBestLower = bestLower;
            return Database.withDao(daoClass, dao -> originalCaseProvider.apply(dao, finalBestLower));
        }

        return null;
    }
}
