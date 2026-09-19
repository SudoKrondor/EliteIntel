package elite.intel.db.managers;

import elite.intel.db.NameColumns;
import elite.intel.db.dao.CommodityDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.JournalSymbol;
import elite.intel.i18n.Language;
import elite.intel.util.StringUtls;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Learns commodities the catalogue does not know from the commander's own journal.
 * <p>
 * <b>Why.</b> A game update can add goods faster than a curated migration can name them, and the
 * catalogue is what turns a spoken word into a symbol and a symbol into a spoken word. Without the row,
 * a new ore cannot be a mining target, is not named when it is prospected, and is read out of the hold as
 * nothing at all. The journal names every good it mentions - symbol plus {@code Name_Localised} in the
 * client's language - on every market board, every hold listing, every refinery pop and every sale, so
 * the row can be learned where those already pass through. A commander who never mines still learns
 * everything their local refinery buys the first time they dock there.
 * <p>
 * <b>What it will not do.</b> The name is filed under the game client's language only - the language it
 * was written in - never guessed into another. A non-English client therefore learns no English name,
 * and without one the good cannot be searched on Spansh, which matches the English name exactly; the
 * hold, the prospector and the mining targets work regardless. An English client, or the curated
 * migration that eventually ships the good, supplies the English. Nothing is ever written over a name a
 * migration or an earlier sighting already put there.
 */
public final class CommodityCatalogue {

    private static final CommodityCatalogue INSTANCE = new CommodityCatalogue();

    private CommodityCatalogue() {
    }

    public static CommodityCatalogue getInstance() {
        return INSTANCE;
    }

    /**
     * One mention of a good in the journal: the symbol in any of Frontier's spellings
     * ({@code $gold_name;}, {@code Gold}, {@code gold}) and the {@code Name_Localised} beside it, which
     * may be null where Frontier omitted it because it would equal the symbol.
     */
    public record Sighting(String symbol, String localisedName) {
    }

    public void learn(String symbol, String localisedName) {
        learnAll(List.of(new Sighting(symbol, localisedName)));
    }

    /**
     * Registers every good in {@code sightings} the catalogue does not know, and fills in the game
     * client's display name where the row has none. Cheap enough for a whole market board: one guarded
     * insert and one conditional update per good, both on the symbol index.
     */
    public void learnAll(Collection<Sighting> sightings) {
        if (sightings == null || sightings.isEmpty()) return;
        Optional<String> gameColumn = NameColumns.gameCommodity();
        boolean englishClient = gameColumn.isPresent() && gameColumn.get().equals(NameColumns.commodity(Language.EN));
        Database.withDao(CommodityDao.class, dao -> {
            for (Sighting sighting : sightings) {
                String symbol = JournalSymbol.normalize(sighting.symbol());
                if (symbol == null) continue;
                String display = displayNameOr(symbol, sighting.localisedName());
                if (englishClient) {
                    dao.attachSymbol(display, symbol);
                    dao.insertIfMissing(display, symbol, false);
                    dao.fillEnglishIfPending(symbol, display);
                } else {
                    dao.insertIfMissing(symbol, symbol, true);
                    gameColumn.ifPresent(column -> dao.fillNameIfNull(column, symbol, display));
                }
            }
            return null;
        });
    }

    /**
     * The journal's display name when it gave one, otherwise a readable form of the symbol. Frontier
     * omits {@code Name_Localised} only when it would equal the symbol, so the capitalised symbol IS the
     * word the client shows in that case, whatever its language.
     */
    private static String displayNameOr(String symbol, String localisedName) {
        return (localisedName == null || localisedName.isBlank())
                ? StringUtls.capitalizeWords(symbol)
                : localisedName;
    }
}
