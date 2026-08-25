package elite.intel.db.managers;

import elite.intel.db.dao.StationMarketDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.JournalSymbol;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.util.json.GsonFactory;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class StationMarketsManager {
    private static final StationMarketsManager INSTANCE = new StationMarketsManager();

    /**
     * The last answer {@link #stockedAt} gave, kept because the HUD asks the same question of the same port
     * once a second and a carrier's {@code Market.json} runs to a hundred kilobytes - parsing that on a
     * timer to learn something that changes only when the commander opens a market screen is work for
     * nothing. Dropped wholesale on any write, which is exactly when it can have gone wrong.
     */
    private volatile Stocked memo;

    /**
     * @param key what was asked for - a MarketID or a station name, told apart by prefix, because the two
     *            lookups share one memo and a station id must never answer for a station name
     */
    private record Stocked(String key, MarketSnapshot snapshot) {
    }

    private StationMarketsManager() {
    }

    public static StationMarketsManager getInstance() {
        return INSTANCE;
    }

    public void save(GameEvents.MarketEvent market) {
        memo = null;
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

    /**
     * Everything a market was holding the last time the commander opened it, keyed by journal symbol.
     * <p>
     * The whole snapshot rather than {@link #lastSeen}'s one commodity, for a caller weighing a shopping
     * list against a hold - most of all the commander's own fleet carrier, whose stock is only ever visible
     * to a third-party tool because the game wrote {@code Market.json} while they stood in it.
     * <p>
     * Only what is actually there: a carrier's market keeps listing a good at zero long after the last
     * tonne of it was sold, and "we have none" is not stock.
     *
     * @param stationName the port, which for a carrier is its callsign
     */
    public Optional<MarketSnapshot> stockedAt(String stationName) {
        if (stationName == null || stationName.isBlank()) return Optional.empty();
        return memoized("name:" + stationName.toLowerCase(), () -> readStockedAt(stationName));
    }

    /**
     * The same, for a port we know the MarketID of - which is every port the ship has docked at.
     * <p>
     * Preferred over the name wherever both are to hand. Station names repeat across the galaxy, so the
     * name lookup has to read every row that bears the name and then tell them apart by the system inside
     * the JSON; the id is the row. It is also the handle that survives a restart on the pad, where the name
     * can be missing entirely.
     */
    public Optional<MarketSnapshot> stockedAt(long marketId) {
        if (marketId == 0) return Optional.empty();
        return memoized("id:" + marketId, () -> readStockedAt(marketId));
    }

    /**
     * One market read at a time, remembered until the next write - see {@link #memo}. Remembered even when
     * it is empty: a station we have no market for is asked about just as often, and the scan that proves
     * it costs the same either way.
     */
    private Optional<MarketSnapshot> memoized(String key, Supplier<Optional<MarketSnapshot>> read) {
        Stocked cached = memo;
        if (cached != null && cached.key().equals(key)) {
            return Optional.ofNullable(cached.snapshot());
        }
        Optional<MarketSnapshot> snapshot = read.get();
        memo = new Stocked(key, snapshot.orElse(null));
        return snapshot;
    }

    private Optional<MarketSnapshot> readStockedAt(long marketId) {
        StationMarketDao.StationMarket row = Database.withDao(StationMarketDao.class,
                dao -> dao.findByMarketId(marketId));
        return row == null ? Optional.empty() : snapshotOf(row);
    }

    private Optional<MarketSnapshot> readStockedAt(String stationName) {
        StationMarketDao.StationMarket[] rows = Database.withDao(StationMarketDao.class,
                dao -> dao.findAllForStation(stationName));
        if (rows == null || rows.length == 0) return Optional.empty();

        for (StationMarketDao.StationMarket row : rows) {
            Optional<MarketSnapshot> snapshot = snapshotOf(row);
            if (snapshot.isPresent()) return snapshot;
        }
        return Optional.empty();
    }

    /**
     * One stored {@code Market.json} as what it had in stock. Only what is actually there: a carrier's
     * market keeps listing a good at zero long after the last tonne of it was sold, and "we have none" is
     * not stock.
     */
    private static Optional<MarketSnapshot> snapshotOf(StationMarketDao.StationMarket row) {
        GameEvents.MarketEvent market = GsonFactory.getGson().fromJson(row.getJson(), GameEvents.MarketEvent.class);
        if (market == null || market.getItems() == null) return Optional.empty();

        Map<String, Integer> stock = new HashMap<>();
        for (GameEvents.MarketEvent.MarketItem item : market.getItems()) {
            if (item.getStock() <= 0) continue;
            String symbol = JournalSymbol.normalize(item.getName());
            if (symbol == null) continue;
            stock.merge(symbol, item.getStock(), Integer::sum);
        }
        return Optional.of(new MarketSnapshot(market.getStarSystem(), market.getStationName(),
                parseInstant(market.getTimestamp()), stock));
    }

    /**
     * What one market held, and when we looked.
     *
     * @param stockBySymbol units on sale per journal symbol; only goods actually in stock appear
     */
    public record MarketSnapshot(String starSystem, String stationName, Instant seenAt,
                                 Map<String, Integer> stockBySymbol) {
        public MarketSnapshot {
            stockBySymbol = stockBySymbol == null ? Map.of() : Map.copyOf(stockBySymbol);
        }
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
        memo = null;
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
