package elite.intel.ui.widget;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The dependency rule of the toggle page: a setting under a parent that is off is greyed out, never
 * changed, and comes back exactly as it was when the parent is turned on again - at any depth.
 */
class ToggleTreePanelTest {

    /**
     * A stored setting the panel reads and writes, standing in for a manager.
     */
    private static final class Stored {
        final AtomicBoolean value;

        Stored(boolean initial) {
            value = new AtomicBoolean(initial);
        }

        SettingToggle toggle(String key, SettingToggle... dependents) {
            return SettingToggle.of(key, value::get, value::set, dependents);
        }
    }

    private final Stored route = new Stored(true);
    private final Stored arrival = new Stored(true);
    private final Stored remainingJumps = new Stored(true);
    private final Stored fuelStars = new Stored(false);
    private final Stored discovery = new Stored(true);

    private final SettingToggle fuelToggle = fuelStars.toggle("automation.announceFuelAvailable");
    private final SettingToggle remainingToggle = remainingJumps.toggle("automation.announceRemainingJumps", fuelToggle);
    private final SettingToggle arrivalToggle = arrival.toggle("automation.announceArrival");
    private final SettingToggle routeToggle = route.toggle("announcements.route", arrivalToggle, remainingToggle);
    private final SettingToggle discoveryToggle = discovery.toggle("announcements.discovery");

    private final ToggleTreePanel panel = new ToggleTreePanel(List.of(discoveryToggle, routeToggle));

    @Test
    void showsStoredValuesAndEverythingLiveWhileParentsAreOn() {
        assertTrue(panel.checkBox(routeToggle).isSelected());
        assertFalse(panel.checkBox(fuelToggle).isSelected());
        for (SettingToggle t : List.of(discoveryToggle, routeToggle, arrivalToggle, remainingToggle, fuelToggle)) {
            assertTrue(panel.checkBox(t).isEnabled(), t.labelKey());
        }
    }

    @Test
    void turningTheRootOffGreysOutEveryDescendantButChangesNothingStored() {
        click(routeToggle);

        assertFalse(route.value.get());
        assertFalse(panel.checkBox(arrivalToggle).isEnabled());
        assertFalse(panel.checkBox(remainingToggle).isEnabled());
        assertFalse(panel.checkBox(fuelToggle).isEnabled());
        // Greyed, not unticked: the tick still shows what the setting will do once the parent is back.
        assertTrue(panel.checkBox(arrivalToggle).isSelected());
        assertTrue(arrival.value.get());
        assertTrue(remainingJumps.value.get());
        // A standalone toggle is nobody's dependent.
        assertTrue(panel.checkBox(discoveryToggle).isEnabled());

        click(routeToggle);
        assertTrue(route.value.get());
        assertTrue(panel.checkBox(arrivalToggle).isEnabled());
        assertTrue(panel.checkBox(fuelToggle).isEnabled());
    }

    @Test
    void aDependentGreysOutOnlyItsOwnDependents() {
        click(remainingToggle);

        assertFalse(remainingJumps.value.get());
        assertFalse(panel.checkBox(fuelToggle).isEnabled());
        assertTrue(panel.checkBox(arrivalToggle).isEnabled());
        assertTrue(panel.checkBox(routeToggle).isEnabled());
    }

    @Test
    void aClickWritesTheSettingAndNothingElse() {
        click(fuelToggle);
        assertTrue(fuelStars.value.get());
        assertTrue(remainingJumps.value.get());
        assertTrue(route.value.get());
    }

    @Test
    void refreshPicksUpAChangeMadeBehindThePanelsBack() {
        route.value.set(false); // a voice command flipped it while the tab was hidden
        panel.refresh();

        assertFalse(panel.checkBox(routeToggle).isSelected());
        assertFalse(panel.checkBox(arrivalToggle).isEnabled());
        assertFalse(panel.checkBox(fuelToggle).isEnabled());
    }

    @Test
    void refreshDoesNotTouchALiveTogglesEnabledState() {
        // A caller may grey a standalone toggle for reasons of its own (the radio channel); refresh must
        // not switch it back on, since only dependents are the panel's to enable.
        JCheckBox discoveryBox = panel.checkBox(discoveryToggle);
        discoveryBox.setEnabled(false);
        panel.refresh();
        assertFalse(discoveryBox.isEnabled());
    }

    @Test
    void anUnknownToggleIsRefused() {
        SettingToggle stranger = new Stored(true).toggle("announcements.mining");
        try {
            panel.checkBox(stranger);
        } catch (IllegalArgumentException expected) {
            assertEquals("Not on this panel: announcements.mining", expected.getMessage());
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }

    private void click(SettingToggle toggle) {
        panel.checkBox(toggle).doClick();
    }
}
