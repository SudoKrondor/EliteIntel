package elite.intel.bindforge.storage;

import java.nio.file.Path;
import java.util.List;

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

    /**
     * What was done in one place.
     *
     * @param label    for a person: the bindings folder, or an installation's storefront
     * @param folder   the bindings folder, or the installation's root
     * @param written  the files written, by name - a file that already held the same bytes is not
     * @param removed  the {@code .buttonMap}s removed, by name
     * @param failures what could not be done, one line each
     */
    public record Place(String label, Path folder, List<String> written, List<String> removed,
                        List<String> failures) {
        public Place {
            written = List.copyOf(written);
            removed = List.copyOf(removed);
            failures = List.copyOf(failures);
        }

        /** Whether everything for this place was done. */
        public boolean updated() {
            return failures.isEmpty();
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
