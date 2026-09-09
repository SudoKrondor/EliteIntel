package elite.intel.ui.support;

import elite.intel.ai.hands.BindingDisplayNames;
import elite.intel.ai.hands.BindingSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomCommandStepPickerItemTest {

    @Test
    void resolvesStoredIdFromPickerItem() {
        CustomCommandStepPickerItem item = new CustomCommandStepPickerItem("deploy_landing_gear", "Deploy landing gear", true);

        assertEquals("deploy_landing_gear", CustomCommandStepPickerItem.resolveId(item));
    }

    @Test
    void resolvesStoredIdFromEditableDisplayText() {
        assertEquals("deploy_landing_gear", CustomCommandStepPickerItem.resolveId("Deploy landing gear - deploy_landing_gear"));
    }

    @Test
    void unknownItemKeepsLegacyIdVisibleAndResolvable() {
        CustomCommandStepPickerItem item = CustomCommandStepPickerItem.unknown("legacy_command", "Unknown command");

        assertEquals("legacy_command", CustomCommandStepPickerItem.resolveId(item));
        assertTrue(item.toString().contains("legacy_command"));
    }

    @Test
    void bindingItemsContainKnownEliteDangerousBindingIds() {
        assertTrue(CustomCommandStepPickerItem.bindingItems().stream()
                .anyMatch(item -> item.id().equals("LandingGearToggle")));
    }

    @Test
    void bindingItemsAreDeduplicatedById() {
        List<String> ids = CustomCommandStepPickerItem.bindingItems().stream()
                .map(CustomCommandStepPickerItem::id)
                .toList();

        assertEquals(ids.size(), ids.stream().distinct().count());
    }

    @Test
    void bindingItemsAreLabelledTheWayTheGameLabelsThem() {
        CustomCommandStepPickerItem landingGear = itemFor("LandingGearToggle");

        assertEquals("Ship controls / Miscellaneous / Landing Gear", landingGear.label());
    }

    /**
     * The three vehicle variants of one control must be told apart on sight; the section is in the
     * label precisely because this picker has no section heading to supply it.
     */
    @Test
    void vehicleVariantsOfOneControlAreDistinguishable() {
        List<String> labels = List.of(
                itemFor("GalaxyMapOpen").label(),
                itemFor("GalaxyMapOpen_Buggy").label(),
                itemFor("GalaxyMapOpen_Humanoid").label());

        assertEquals(labels.size(), labels.stream().distinct().count(), labels.toString());
        assertTrue(labels.get(0).startsWith("Ship controls / "), labels.get(0));
        assertTrue(labels.get(1).startsWith("SRV controls / "), labels.get(1));
        assertTrue(labels.get(2).startsWith("On-foot controls / "), labels.get(2));
    }

    /**
     * No two rows may read identically, or the picker cannot say which binding a step targets.
     */
    @Test
    void bindingItemLabelsAreUnique() {
        List<String> labels = CustomCommandStepPickerItem.bindingItems().stream()
                .map(CustomCommandStepPickerItem::label)
                .toList();

        assertEquals(labels.size(), labels.stream().distinct().count(), "duplicate picker labels");
    }

    /**
     * Guards the " - " separator against the labels that now contain one of their own: the id must
     * still round-trip out of the editable combo's text.
     */
    @Test
    void resolvesStoredIdWhenTheLabelItselfContainsTheSeparator() {
        CustomCommandStepPickerItem item = itemFor("VanityCameraOne");

        assertTrue(item.label().contains(" - "), item.label());
        assertEquals("VanityCameraOne", CustomCommandStepPickerItem.resolveId(item.toString()));
    }

    @Test
    void searchMatchesSectionGroupNameAndRawTag() {
        CustomCommandStepPickerItem item = itemFor("LandingGearToggle");

        assertTrue(item.matches("ship"));
        assertTrue(item.matches("miscellaneous"));
        assertTrue(item.matches("landing gear"));
        assertTrue(item.matches("LandingGearToggle"));
        assertFalse(item.matches("hyperspace"));
    }

    /**
     * Pins the controls whose in-game row has not been read off the game screen yet. They still
     * appear in the picker, under their humanized tag in the "Other controls" section, because a
     * step that targets one must stay selectable.
     * <p>
     * This is a ratchet, not an aspiration: it fails if a NEW binding turns up unnamed (the table
     * has drifted behind the game) and it fails once one of these three is finally named, which is
     * the reminder to delete it from this list.
     */
    @Test
    void onlyTheKnownUnnamedControlsFallBackToTheirRawTag() {
        List<String> unnamed = CustomCommandStepPickerItem.bindingItems().stream()
                .filter(item -> BindingDisplayNames.lookup(item.id()).section() == BindingSection.OTHER)
                .map(CustomCommandStepPickerItem::id)
                .sorted()
                .toList();

        assertEquals(List.of("BlockMouseDecay", "HumanoidPing", "MouseReset"), unnamed);
    }

    private static CustomCommandStepPickerItem itemFor(String bindingId) {
        return CustomCommandStepPickerItem.bindingItems().stream()
                .filter(item -> item.id().equals(bindingId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("picker has no item for " + bindingId));
    }
}
