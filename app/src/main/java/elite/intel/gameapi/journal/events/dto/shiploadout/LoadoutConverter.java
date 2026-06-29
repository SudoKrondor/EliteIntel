package elite.intel.gameapi.journal.events.dto.shiploadout;

import elite.intel.db.dao.ShipTypeDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.LoadoutEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static elite.intel.util.StringUtls.toReadableModuleName;

public class LoadoutConverter {

    private static final Logger log = LogManager.getLogger(LoadoutConverter.class);

    private static final ConcurrentHashMap<String, String> SHIP_DISPLAY_NAMES =
            new ConcurrentHashMap<>(loadShipDisplayNames());

    private static Map<String, String> loadShipDisplayNames() {
        return Database.withDao(ShipTypeDao.class, dao -> dao.findAll());
    }

    public static void upsertDisplayName(String internalName, String displayName) {
        if (internalName == null) return;
        String key = internalName.toLowerCase(Locale.ROOT);
        try {
            Database.withDao(ShipTypeDao.class, dao -> {
                dao.upsert(key, displayName);
                return null;
            });
        } catch (RuntimeException e) {
            log.warn("Could not persist ship display name {} -> {}: {}", key, displayName, e.getMessage());
            return; // WHY: DB write failure is non-fatal; skip in-memory update to avoid divergence on restart
        }
        SHIP_DISPLAY_NAMES.put(key, displayName);
    }

    public static ShipLoadOutDto toShipLoadOutDto(LoadoutEvent event) {
        ShipLoadOutDto dto = new ShipLoadOutDto();
        dto.setCargoCapacity(event.getCargoCapacity());
        FuelCapacityDto fuelCapacity = new FuelCapacityDto();
        fuelCapacity.setMainTank(event.getFuelCapacity().getMain());
        fuelCapacity.setReserveTank(event.getFuelCapacity().getReserve());
        dto.setShipId(event.getShipId());
        dto.setFuelCapacity(fuelCapacity);
        dto.setHullHealth(event.getHullHealth() * 100f);
        dto.setMaxJumpRange(Math.round(event.getMaxJumpRange() * 100.0) / 100.0);
        dto.setModules(toModules(event.getModules()));
        dto.setModulesValue(event.getModulesValue());
        dto.setInsurance(event.getRebuy());
        dto.setShipId(event.getShipId());
        dto.setShipIdent(event.getShipIdent());
        dto.setShipMake(event.getShip());
        dto.setShipName(toDisplayShipName(event));
        dto.setUnladenMass(event.getUnladenMass());
        return dto;
    }

    /**
     * Returns the player-defined ship name when present, otherwise resolves the
     * journal ship type to a human-readable display name. Returns null when
     * neither value is usable so existing Unknown fallback text can still apply.
     */
    public static String toDisplayShipName(LoadoutEvent event) {
        return toDisplayShipName(event.getShipName(), event.getShip());
    }

    /**
     * Normalizes a stored or journal ship name into the display name used by the UI.
     * Prefers the player-assigned name when non-blank. Otherwise looks up the internal
     * ship type in the in-memory ship_type map (loaded once at startup); if not found,
     * title-cases the raw value (correct for ship types the game itself considers
     * self-explanatory, e.g. "vulture" → "Vulture").
     */
    public static String toDisplayShipName(String shipName, String ship) {
        String normalizedShipName = normalizeBlank(shipName);
        if (normalizedShipName != null) {
            return normalizedShipName;
        }

        String normalizedShip = normalizeBlank(ship);
        if (normalizedShip == null) {
            return null;
        }

        String displayName = SHIP_DISPLAY_NAMES.get(normalizedShip.toLowerCase(Locale.ROOT));
        if (displayName != null) {
            return displayName;
        }

        return normalizedShip.substring(0, 1).toUpperCase(Locale.ROOT) + normalizedShip.substring(1);
    }

    private static String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<ModuleDto> toModules(List<LoadoutEvent.Module> modules) {
        ArrayList<ModuleDto> result = new ArrayList<>();
        for (LoadoutEvent.Module module : modules) {
            result.add(toModule(module));
        }
        return result;
    }

    private static ModuleDto toModule(LoadoutEvent.Module module) {
        ModuleDto dto = new ModuleDto();
        dto.setSlot(toReadableModuleName(module.getSlot()));
        dto.setItem(toReadableModuleName(module.getItem()));
        dto.setOn(module.isOn());
        dto.setPriority(module.getPriority());
        dto.setHealthPercentage(module.getHealth() * 100f);
        dto.setValue(module.getValue());
        dto.setAmmoInClip(module.getAmmoInClip());
        dto.setAmmoInHopper(module.getAmmoInHopper());
        dto.setEngineering(toEngineering(module.getEngineering()));
        return dto;
    }

    private static EngineeringDto toEngineering(LoadoutEvent.Engineering engineering) {
        if (engineering == null) {
            return null;
        }
        EngineeringDto dto = new EngineeringDto();
        dto.setEngineer(engineering.getEngineer());
        dto.setBlueprintName(toReadableModuleName(engineering.getBlueprintName()));
        dto.setQuality(engineering.getQuality());
        dto.setExperimentalEffectLocalised(engineering.getExperimentalEffectLocalised());
        dto.setModifiers(toModifiers(engineering.getModifiers()));
        return dto;
    }

    private static List<ModifierDto> toModifiers(List<LoadoutEvent.Modifier> modifiers) {
        if (modifiers == null) {
            return null;
        }
        ArrayList<ModifierDto> result = new ArrayList<>();
        for (LoadoutEvent.Modifier modifier : modifiers) {
            result.add(toModifier(modifier));
        }
        return result;
    }

    private static ModifierDto toModifier(LoadoutEvent.Modifier modifier) {
        ModifierDto dto = new ModifierDto();
        dto.setLabel(toReadableModuleName(modifier.getLabel()));
        dto.setValue((float) Math.round(modifier.getValue() * 100.0) / 100.0f);
        return dto;
    }
}
