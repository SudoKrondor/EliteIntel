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

    public ShipDao.Ship getShipById(int shipId) {
        return Database.withDao(ShipDao.class, dao -> dao.findShip(shipId));
    }

    public List<ShipDao.Ship> getAllShips() {
        return Database.withDao(ShipDao.class, dao -> dao.allShips());
    }

    public List<ShipDao.Ship> getShipsForCommander(String commanderName) {
        return Database.withDao(ShipDao.class, dao -> dao.allShipsForCommander(commanderName));
    }

    public void resetAllVoicesToDefault(String defaultVoice) {
        List<ShipDao.Ship> ships = getAllShips();
        for (ShipDao.Ship ship : ships) {
            ship.setVoice(defaultVoice);
            saveShip(ship);
        }
    }
}
