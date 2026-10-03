package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the hand-written action families in {@link BindingConflictRules} against the control name
 * table they are meant to mirror. The rules list actions by name on purpose - the action set is
 * Frontier's, and a control they add later should be opted in by someone who has decided it belongs -
 * but a list nobody checks is a list that silently goes stale.
 */
class BindingConflictRulesTest {

    private static final String INTERFACE_MODE_GROUP = "Interface Mode";

    /**
     * The Interface Mode family decides whether a chord shared with a vehicle control is reported as
     * blocking. A control Frontier adds to that group and nobody adds here would be cleared by the
     * context model instead, which is the bug this family exists to fix - so the two have to agree.
     */
    @Test
    void interfaceModeFamilyMatchesTheGameControlScreen() {
        Set<String> fromControlTable = BindingDisplayNames.all().entrySet().stream()
                .filter(e -> e.getValue().section() == BindingSection.GENERAL)
                .filter(e -> INTERFACE_MODE_GROUP.equals(e.getValue().group()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(fromControlTable, new TreeSet<>(BindingConflictRules.interfaceModeActions()),
                "BindingConflictRules.INTERFACE_MODE_ACTIONS has drifted from the '" + INTERFACE_MODE_GROUP
                        + "' group of /bindings/ed_control_names.properties");
    }
}
