package elite.intel.companion.prompt;

import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.ThoughtSource;

import java.util.EnumSet;
import java.util.Set;

/**
 * Maps a thought {@link ThoughtSource} to the {@code IntelAction} categories it may use. This is the
 * code-level enforcement of the COMMANDER/EVENT split: EVENT thoughts can never receive
 * action/macro tools regardless of any prompt instruction.
 * <p>
 * Governs game tools ({@code IntelCommand}/{@code IntelQuery}/macros) by category; system-function
 * visibility is owned per-function by {@code SystemFunction.availableFor}.
 */
public final class IntelActionAccessPolicy {

    /** Returns the IntelAction categories allowed for the given source. */
    public Set<IntelActionCategory> allowedCategories(ThoughtSource source) {
        return switch (source) {
            case COMMANDER -> EnumSet.of(IntelActionCategory.QUERY, IntelActionCategory.ACTION, IntelActionCategory.MACRO);
            case EVENT -> EnumSet.of(IntelActionCategory.QUERY);
        };
    }
}
