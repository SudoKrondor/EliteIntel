package elite.intel.ui.support;


import elite.intel.ai.brain.actions.catalog.CommandCatalog;
import elite.intel.ai.hands.Bindings;
import elite.intel.util.StringUtls;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Display item for custom command step pickers; the UI shows a label, while persistence stores only {@link #id()}.
 */
public record CustomCommandStepPickerItem(String id, String label, boolean known) {

    public CustomCommandStepPickerItem {
        id = Objects.requireNonNull(id, "id");
        label = Objects.requireNonNull(label, "label");
    }

    /**
     * Returns built-in command picker items only; customCommand commands are intentionally excluded for CustomCommand Editor v1.
     */
    public static List<CustomCommandStepPickerItem> builtInCommandItems() {
        return new CommandCatalog().entries().stream()
                .filter(entry -> !entry.isCustomCommand())
                .map(entry -> new CustomCommandStepPickerItem(entry.id(), entry.name(), true))
                .sorted((left, right) -> left.label().compareToIgnoreCase(right.label()))
                .toList();
    }

    /**
     * Returns known Elite Dangerous binding ids exposed by the input binding layer.
     */
    public static List<CustomCommandStepPickerItem> bindingItems() {
        Map<String, CustomCommandStepPickerItem> byId = new LinkedHashMap<>();
        for (Bindings.GameCommand command : Bindings.GameCommand.values()) {
            String id = command.getGameBinding();
            byId.putIfAbsent(id, new CustomCommandStepPickerItem(id, StringUtls.humanizeBindingName(id), true));
        }
        return new ArrayList<>(byId.values()).stream()
                .sorted((left, right) -> left.label().compareToIgnoreCase(right.label()))
                .toList();
    }

    /**
     * Creates a visible fallback item for legacy or unknown ids so editing does not silently drop them.
     */
    public static CustomCommandStepPickerItem unknown(String id, String labelPrefix) {
        return new CustomCommandStepPickerItem(id == null ? "" : id, labelPrefix, false);
    }

    /**
     * Extracts the stable stored id from either a picker item or editable combo-box text.
     */
    public static String resolveId(Object selected) {
        if (selected instanceof CustomCommandStepPickerItem item) {
            return item.id();
        }
        String text = selected == null ? "" : selected.toString().trim();
        int separator = text.lastIndexOf(" - ");
        return separator >= 0 ? text.substring(separator + 3).trim() : text;
    }

    /**
     * Checks whether this item should remain visible for the current picker search query.
     */
    public boolean matches(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        return normalized.isBlank()
                || id.toLowerCase().contains(normalized)
                || label.toLowerCase().contains(normalized);
    }

    @Override
    public String toString() {
        return label + " - " + id;
    }
}
