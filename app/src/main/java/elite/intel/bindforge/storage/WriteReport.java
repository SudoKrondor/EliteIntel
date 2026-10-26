package elite.intel.bindforge.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * What one write of the working copy to the live copies did, place by place - so the user is told what failed and
 * where, never left to guess (principle 9; storage.md rule 8: <em>"updated 2 of 3"</em>).
 *
 * @param bindings      the bindings folder
 * @param installations each installation, in the stored list's order
 */
public record WriteReport(Place bindings, List<Place> installations) {

    public WriteReport {
        installations = List.copyOf(installations);
    }

    /** Which kind of place - for the screen to name in the user's language, never as text from here. */
    public enum Kind {
        /** The bindings folder: every {@code .binds} and {@code StartPreset.N.start}. */
        BINDINGS_FOLDER,
        /** One installation: its {@code DeviceMappings.xml} and {@code .buttonMap}s. */
        INSTALLATION
    }

    /**
     * What was done in one place.
     *
     * @param kind       the bindings folder or an installation
     * @param storefront an installation's storefront, as the stored list records it; {@code null} for the bindings
     *                   folder
     * @param folder     the bindings folder, or the installation's root
     * @param written    the files written, by name - a file that already held the same bytes is not
     * @param removed      the {@code .buttonMap}s removed, by name
     * @param keptReadOnly the {@code .buttonMap}s the set lacks that were kept because the user made them read-only,
     *                     by name - the screen tells the user how to let them go, in their language (review,
     *                     2026-10-25)
     * @param failures     what could not be done, one line each - English, for the log and for support, built from the
     *                     system's own messages. A screen says that a place was not updated in its own words, and may
     *                     show these lines as the detail
     */
    public record Place(Kind kind, String storefront, Path folder, List<String> written, List<String> removed,
                        List<String> keptReadOnly, List<String> failures) {
        public Place {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(folder, "folder");
            if ((kind == Kind.INSTALLATION) != (storefront != null)) {
                throw new IllegalArgumentException("an installation has a storefront, and the bindings folder none");
            }
            written = List.copyOf(written);
            removed = List.copyOf(removed);
            keptReadOnly = List.copyOf(keptReadOnly);
            failures = List.copyOf(failures);
        }

        /** Whether everything for this place was done - a kept read-only file means it does not match the set. */
        public boolean updated() {
            return failures.isEmpty() && keptReadOnly.isEmpty();
        }
    }

    /** How many installations took everything - the 2 of <em>"updated 2 of 3"</em>. */
    public long installationsUpdated() {
        return installations.stream().filter(Place::updated).count();
    }

    /** Whether every place took everything. */
    public boolean complete() {
        return bindings.updated() && installationsUpdated() == installations.size();
    }
}
