package elite.intel.ui.support;

import elite.intel.ai.brain.actions.catalog.CommandCatalog;
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
    void builtInCommandItemsDoNotExposeCustomCommandEntries() {
        CommandCatalog catalog = new CommandCatalog();
        //just for test 1
        assertFalse(catalog.entries().stream().anyMatch(entry -> entry.isCustomCommand()));
        assertEquals(catalog.entries().size(), CustomCommandStepPickerItem.builtInCommandItems().size());
    }
}
