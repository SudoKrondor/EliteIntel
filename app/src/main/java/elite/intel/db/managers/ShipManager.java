package elite.intel.db.managers;

import elite.intel.ai.brain.ShipPersonality;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.util.ShipPadSizes;

import java.util.List;

public class ShipManager {
    private static ShipManager instance;

    private ShipManager() {
    }

    public static synchronized ShipManager getInstance() {
        if (instance == null) {
            instance = new ShipManager();
        }
        return instance;
    }

    public void save(int shipId, String shipName, int cargoCapacity, String shipMake, String voice, String commanderName) {
        Database.withDao(ShipDao.class, dao -> {
            ShipDao.Ship ship = new ShipDao.Ship();
            ship.setShipId(shipId);
            ship.setShipName(shipName);
            ship.setCargoCapacity(cargoCapacity);
            ship.setShipIdentifier(shipMake);
            ship.setVoice(voice);
            ship.setPersonality(ShipPersonality.DEFAULT.name());
            ship.setCommanderName(commanderName);
            dao.save(ship);
            return Void.TYPE;
        });
    }

    public ShipDao.Ship getShip() {
        return Database.withDao(ShipDao.class, dao -> {
                    ShipLoadOutDto dto = ShipLoadoutManager.getInstance().get();
                    if (dto == null) return null;
                    return dao.findShip(dto.getShipId());
                }
        );
    }


    /**
     * The pad the ship currently being flown needs, as {@code S}, {@code M} or {@code L}.
     * <p>
     * WHY an unknown ship reads as large where {@link #requireLargePad} reads as false: that one answers
     * "must I insist on a large pad", and insisting on the strength of no information would hide outposts
     * from every ship. This one answers "what will fit", and a search built on it sends the commander
     * somewhere to land - so the cautious answer is the safe one.
     */
    public String requiredPadSize() {
        ShipDao.Ship ship = getShip();
        return ship == null ? ShipPadSizes.LARGE : ShipPadSizes.getPadSize(ship.getShipIdentifier());
    }

    public boolean requireLargePad() {
        ShipDao.Ship ship = getShip();
        if (ship == null) return false;
        return "L".equals(ShipPadSizes.getPadSize(ship.getShipIdentifier()));
    }

    public void saveShip(ShipDao.Ship ship) {
        Database.withDao(ShipDao.class, dao -> {
            dao.save(ship);
            return Void.TYPE;
        });
    }

    /**
     * Takes a sold ship off the fleet. The row is kept, flagged, with its settings and trade profile: if the game
     * later names the ship again (Loadout or StoredShips), {@link #inFleet} puts it back as the commander left it.
     * The game never reuses a ShipID, so a ship really sold is never brought back by mistake.
     *
     * @param soldAt the sale's journal timestamp
     * @return true when the ship was in the fleet and is now marked sold
     */
    public boolean markSold(int shipId, String soldAt) {
        return Database.withDao(ShipDao.class, dao -> dao.markSold(shipId, soldAt)) > 0;
    }

    /**
     * Records that the game has just named this ship as the commander's, clearing a sale mark if it had one.
     *
     * @return true when the ship had been marked sold and is back in the fleet
     */
    public boolean inFleet(int shipId) {
        return Database.withDao(ShipDao.class, dao -> dao.clearSold(shipId)) > 0;
    }

    /**
     * A ship the game lists as stored: its ShipID, the name the fleet shows and its make.
     */
    public record StoredShip(int shipId, String shipName, String shipMake) {
    }

    /**
     * Adds every stored ship the fleet does not know yet, so the fleet heals from the game's own list instead of
     * waiting for each ship to be flown. A ship already known keeps its row as it is - the commander's voice,
     * personality and settings - and comes back into the fleet if it was marked sold. A Loadout refreshes the
     * rest when the ship is flown. A new row's cargo capacity is unknown until then and stored as 0.
     *
     * @return how many ships were added or brought back
     */
    public int addMissingShips(List<StoredShip> ships, String voice, String commanderName) {
        return Database.withDao(ShipDao.class, dao -> {
            int added = 0;
            for (StoredShip stored : ships) {
                if (dao.findShip(stored.shipId()) != null) {
                    added += dao.clearSold(stored.shipId());
                    continue;
                }
                ShipDao.Ship ship = new ShipDao.Ship();
                ship.setShipId(stored.shipId());
                ship.setShipName(stored.shipName());
                ship.setCargoCapacity(0);
                ship.setShipIdentifier(stored.shipMake());
                ship.setVoice(voice);
                ship.setPersonality(ShipPersonality.DEFAULT.name());
                ship.setCommanderName(commanderName);
                dao.save(ship);
                added++;
            }
            return added;
        });
    }

    public ShipDao.Ship getShipById(int shipId) {
        return Database.withDao(ShipDao.class, dao -> dao.findShip(shipId));
    }

    public List<ShipDao.Ship> getAllShips() {
        return Database.withDao(ShipDao.class, dao -> dao.allShips());
    }

    public List<ShipDao.Ship> getShipsForCommander(String commanderName) {
        return Database.withDao(ShipDao.class, dao -> dao.allShipsForCommander(commanderName));
    }

    /** Sold ships too: one brought back must not carry a voice the new engine lacks. */
    public void resetAllVoicesToDefault(String defaultVoice) {
        List<ShipDao.Ship> ships = Database.withDao(ShipDao.class, ShipDao::allRows);
        for (ShipDao.Ship ship : ships) {
            ship.setVoice(defaultVoice);
            saveShip(ship);
        }
    }
}
