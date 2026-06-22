package elite.intel.db;

import elite.intel.db.dao.CommodityDao;
import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.dao.MaterialsDao;
import elite.intel.db.dao.SubSystemDao;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FuzzySearch {


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

    public static String fuzzyMaterialNameSearch(String input, int similarity) {
        Language lang = SystemSession.getInstance().getLanguage();
        if (lang == Language.EN) {
            return fuzzyMatch(input, similarity, MaterialNameDao.class, MaterialNameDao::getAllNamesLowerCase, MaterialNameDao::getOriginalCase);
        }
        String col = materialNameColumn(lang);
        return fuzzyMatch(input, similarity, MaterialNameDao.class,
                dao -> dao.getAllLocalizedNamesLowerCase(col),
                (dao, name) -> dao.getEnglishByLocalizedName(col, name));
    }

    public static String fuzzyInventorySearch(String input, int similarity) {
        Language lang = SystemSession.getInstance().getLanguage();
        if (lang == Language.EN) {
            return fuzzyMatch(input, similarity, MaterialsDao.class, MaterialsDao::getAllNamesLowerCase, MaterialsDao::getOriginalCase);
        }
        // Inventory materialNames are always English (from journal).
        // JOIN with material_names lets us match localized input and return the English canonical name.
        String col = materialNameColumn(lang);
        return fuzzyMatch(input, similarity, MaterialsDao.class,
                dao -> dao.getAllLocalizedNamesLowerCase(col),
                (dao, name) -> dao.getEnglishByLocalizedName(col, name));
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
            default -> "name";
        };
    }

    private static String commodityColumn(Language lang) {
        return switch (lang) {
            case DE -> "commodity_de";
            case FR -> "commodity_fr";
            case ES -> "commodity_es";
            case RU -> "commodity_ru";
            case UK -> "commodity_uk";
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
