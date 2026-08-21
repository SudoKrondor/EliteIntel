package elite.intel.db.managers;

import elite.intel.db.dao.StationMarketDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.JournalSymbol;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.util.json.GsonFactory;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class StationMarketsManager {
    private static final StationMarketsManager INSTANCE = new StationMarketsManager();

    private StationMarketsManager() {
    }

    public static StationMarketsManager getInstance() {
        return INSTANCE;
    }

    public void save(GameEvents.MarketEvent market) {
        Database.withDao(StationMarketDao.class, dao -> {
            StationMarketDao.StationMarket stationMarket = new StationMarketDao.StationMarket();
            stationMarket.setJson(market.toJson());
            stationMarket.setStationName(market.getStationName());
            stationMarket.setMarketId(market.getMarketID());
            dao.upsert(stationMarket);
            return null;
        });
    }

    /**
     * What the game itself told us a market had, the last time the commander stood in it.
     *
     * @param stock units on sale; zero when the market listed the good without stocking it, and when it
     *              did not list it at all - a market that sells none of something is the same answer
     *              either way
     */
    public record Sighting(String starSystem, String stationName, Instant seenAt, int stock) {
    }

    /**
     * Our own last look at a market's stock of one commodity, or empty when the commander has never
     * opened that market.
     * <p>
     * This is first-hand data in a world of second-hand data: Spansh is crowd-sourced, so a market it
     * lists as selling a good may have been emptied - or may never have stocked it - since whoever
     * uploaded that entry was there. {@code Market.json} is the game speaking, and the app has been
     * storing every one it sees all along.
     *
     * @param commoditySymbol the bare journal symbol, as {@link JournalSymbol} normalises it
     */
    public Optional<Sighting> lastSeen(String starSystem, String stationName, String commoditySymbol) {
        if (stationName == null || stationName.isBlank() || commoditySymbol == null) return Optional.empty();

        StationMarketDao.StationMarket[] rows = Database.withDao(StationMarketDao.class,
                dao -> dao.findAllForStation(stationName));
        if (rows == null) return Optional.empty();

        for (StationMarketDao.StationMarket row : rows) {
            GameEvents.MarketEvent market = GsonFactory.getGson().fromJson(row.getJson(), GameEvents.MarketEvent.class);
            if (market == null || market.getItems() == null) continue;
            // Station names repeat across the galaxy; a market in the wrong system says nothing about
            // this one, and guessing would be worse than having no sighting at all.
            if (starSystem != null && market.getStarSystem() != null
                    && !starSystem.equalsIgnoreCase(market.getStarSystem())) {
                continue;
            }
            return Optional.of(new Sighting(market.getStarSystem(), market.getStationName(),
                    parseInstant(market.getTimestamp()), stockOf(market, commoditySymbol)));
        }
        return Optional.empty();
    }

    private static int stockOf(GameEvents.MarketEvent market, String commoditySymbol) {
        return market.getItems().stream()
                .filter(item -> commoditySymbol.equalsIgnoreCase(JournalSymbol.normalize(item.getName())))
                .mapToInt(GameEvents.MarketEvent.MarketItem::getStock)
                .findFirst()
                .orElse(0);
    }

    private static Instant parseInstant(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) return null;
        try {
            return Instant.parse(timestamp);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public void clear() {
        Database.withDao(StationMarketDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    public List<GameEvents.MarketEvent> listAll() {
        return Database.withDao(StationMarketDao.class, dao -> {
            StationMarketDao.StationMarket[] all = dao.listAll();
            List<GameEvents.MarketEvent> result = new java.util.ArrayList<>();
            for (StationMarketDao.StationMarket entity : all) {
                result.add(GsonFactory.getGson().fromJson(entity.getJson(), GameEvents.MarketEvent.class));
            }
            return result;
        });
    }
}
