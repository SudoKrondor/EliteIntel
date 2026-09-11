package elite.intel.ui.widget;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * One stored on/off setting shown as a checkbox, together with the settings that are moot while it is off.
 * <p>
 * A dependent is not switched off with its parent - its stored value is left alone - it is shown disabled,
 * so the commander can see both that it has no effect right now and what it will do once the parent is back
 * on. Listing a toggle as a dependent is the caller's promise that the code honours the dependency: the
 * subscriber reading it must really ignore it while the parent is off, or the grid tells a lie.
 *
 * @param labelKey   gui bundle key of the checkbox label
 * @param read       the stored value, re-read whenever the panel refreshes (a voice command may have flipped it)
 * @param write      where a click goes
 * @param dependents settings that only take effect while this one is on, in display order
 */
public record SettingToggle(String labelKey, BooleanSupplier read, Consumer<Boolean> write,
                            List<SettingToggle> dependents) {

    public SettingToggle {
        dependents = List.copyOf(dependents);
    }

    public static SettingToggle of(String labelKey, BooleanSupplier read, Consumer<Boolean> write,
                                   SettingToggle... dependents) {
        return new SettingToggle(labelKey, read, write, List.of(dependents));
    }

    public boolean hasDependents() {
        return !dependents.isEmpty();
    }
}
