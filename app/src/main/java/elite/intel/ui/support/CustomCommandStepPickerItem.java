package elite.intel.ui.support;


import elite.intel.ai.hands.BindingDisplayNames;
import elite.intel.ai.hands.Bindings;

import java.util.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Display item for custom command step pickers; the UI shows a label, while persistence stores only {@link #id()}.
 */
public record CustomCommandStepPickerItem(String id, String label, boolean known) {

    public CustomCommandStepPickerItem {
        id = Objects.requireNonNull(id, "id");
        label = Objects.requireNonNull(label, "label");
    }

    /**
     * Returns known Elite Dangerous binding ids exposed by the input binding layer, each labelled
     * the way the game's own OPTIONS &gt; CONTROLS screen labels it (see {@link BindingDisplayNames}),
     * in that screen's order.
     * <p>
     * WHY the full "Section / Group / Name" path: unlike the Binding Profile tables, this picker is
     * one flat list with no section heading above it, so nothing else would tell a commander whether
     * "Mode Switches / Comms Panel" is the ship's or the SRV's - and it makes "srv" or "on foot" work
     * as search terms. Several {@code GameCommand} constants share one binding id; {@code putIfAbsent}
     * keeps one item per id.
     */
    public static List<CustomCommandStepPickerItem> bindingItems() {
        Map<String, CustomCommandStepPickerItem> byId = new LinkedHashMap<>();
        for (Bindings.GameCommand command : Bindings.GameCommand.values()) {
            String id = command.getGameBinding();
            byId.putIfAbsent(id, new CustomCommandStepPickerItem(id, inGameLabel(id), true));
        }
        return byId.values().stream()
                .sorted(Comparator
                        .comparingInt((CustomCommandStepPickerItem item) -> BindingDisplayNames.lookup(item.id()).order())
                        .thenComparing(CustomCommandStepPickerItem::label, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * "Ship controls / Weapons / Primary Fire" - the section is localized, the control name is not.
     */
    private static String inGameLabel(String bindingId) {
        BindingDisplayNames.ControlName control = BindingDisplayNames.lookup(bindingId);
        return getText(control.section().getLabelKey()) + " / " + control.label();
    }

    /**
     * Creates a visible fallback item for legacy or unknown ids so editing does not silently drop them.
     */
    public static CustomCommandStepPickerItem unknown(String id, String labelPrefix) {
        return new CustomCommandStepPickerItem(id == null ? "" : id, labelPrefix, false);
    }

    /**
     * Extracts the stable stored id from either a picker item or editable combo-box text.
     * <p>
     * The scan is deliberately from the END: in-game control names contain " - " of their own
     * ("Camera - Cockpit Front"), so only the last separator is the one this class appended before
     * the id. A first-match scan would return a fragment of the label as the id.
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
