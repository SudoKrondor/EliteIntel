package elite.intel.ui.screen.settings;

import elite.intel.gameapi.SurfaceVehicle;
import elite.intel.gameapi.SurfaceVehicleDeployment;
import elite.intel.ui.widget.HudComboBox;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.hudReadoutLabel;
import static elite.intel.ui.theme.AppTheme.transparentPanel;
import static elite.intel.ui.theme.HudPalette.HUD_GAP;

/**
 * What the commander has loaded into each planetary vehicle hangar bay.
 * <p>
 * <b>Why the commander has to fill this in.</b> The journal names the hangar module but never its
 * contents, and a multi-bay hangar deploys whichever vehicle the bay list is sitting on. Without knowing
 * what is in each bay the app can only ever open the top one - which is exactly the bug this settles - and
 * cannot know whether the ship should be landed or hovering to open it.
 * <p>
 * All four bays are always shown rather than only as many as the fitted hangar holds. The hangar is part
 * of a loadout that changes at any outfitting station, and a dialog that grew and shrank rows underneath
 * the commander would lose whatever they had already set for the hull they were about to fly.
 */
public class VehicleBaySettingsPanel implements SettingRow {

    /**
     * The entry meaning "nothing configured here", which is the state every bay starts in and a real
     * choice to return to. It is not a vehicle, so it carries its own label rather than an enum name.
     */
    private static final String EMPTY = "";

    private final IntFunction<SurfaceVehicle> getter;
    private final BaySetter setter;

    /**
     * Writes one bay, 1-based, or clears it when the vehicle is null.
     */
    @FunctionalInterface
    public interface BaySetter {
        void set(int bay, SurfaceVehicle vehicle);
    }

    public VehicleBaySettingsPanel(IntFunction<SurfaceVehicle> getter, BaySetter setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public JPanel build() {
        JPanel row = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 4));
        row.add(hudReadoutLabel(getText("automation.vehicleBays")));
        for (int bay = 1; bay <= SurfaceVehicleDeployment.MAX_BAYS; bay++) {
            row.add(hudReadoutLabel(getText("automation.vehicleBay", bay)));
            row.add(bayCombo(bay));
        }
        return row;
    }

    private HudComboBox<String> bayCombo(int bay) {
        String[] options = options();
        HudComboBox<String> combo = new HudComboBox<>(options);
        SurfaceVehicle current = getter.apply(bay);
        combo.setSelectedItem(current == null ? label(null) : label(current));

        Consumer<String> apply = chosen -> setter.set(bay, fromLabel(chosen));
        combo.addActionListener(e -> {
            String chosen = (String) combo.getSelectedItem();
            if (chosen != null) apply.accept(chosen);
        });
        return combo;
    }

    /**
     * The empty choice first, so a bay that has never been set reads as unset at a glance rather than as a
     * Scarab the commander did not choose.
     */
    private static String[] options() {
        SurfaceVehicle[] vehicles = SurfaceVehicle.values();
        String[] options = new String[vehicles.length + 1];
        options[0] = label(null);
        for (int i = 0; i < vehicles.length; i++) {
            options[i + 1] = label(vehicles[i]);
        }
        return options;
    }

    /**
     * Vehicle names are proper nouns from the game and are deliberately NOT translated; only the "empty"
     * entry is, because that word is ours rather than Frontier's.
     */
    private static String label(SurfaceVehicle vehicle) {
        return vehicle == null ? getText("automation.vehicleBay.empty") : vehicle.displayName();
    }

    private static SurfaceVehicle fromLabel(String chosen) {
        if (chosen == null || chosen.equals(getText("automation.vehicleBay.empty")) || chosen.equals(EMPTY)) {
            return null;
        }
        return SurfaceVehicle.fromStored(chosen);
    }
}
