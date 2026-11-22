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
 * @param otherBindings each other installation's own bindings folder, where it has one apart from the bindings folder -
 *                      on Linux, where each installation keeps its own (linux-several-installations.md, rule 5)
 */
public record WriteReport(Place bindings, List<Place> installations, List<Place> otherBindings) {

    public WriteReport {
        installations = List.copyOf(installations);
        otherBindings = List.copyOf(otherBindings);
    }

    /** A write with no other installation's bindings folder - every installation shares the one. */
    public WriteReport(Place bindings, List<Place> installations) {
        this(bindings, installations, List.of());
    }

    /** Which kind of place - for the screen to name in the user's language, never as text from here. */
    public enum Kind {
        /** The bindings folder: every {@code .binds} and {@code StartPreset.N.start}. */
        BINDINGS_FOLDER,
        /** One installation: its {@code DeviceMappings.xml} and {@code .buttonMap}s. */
        INSTALLATION,
        /**
         * Another installation's own bindings folder - its {@code .binds} and {@code StartPreset.N.start} - apart from
         * the bindings folder Elite-Intel is set to.
         */
        INSTALLATION_BINDINGS;

        /** Whether this kind of place is an installation's, and so named by its storefront. */
        boolean ofAnInstallation() {
            return this != BINDINGS_FOLDER;
        }
    }

    /**
     * Why another installation's own bindings folder was left alone without being found changed outside BindForge
     * (linux-several-installations.md, rule 5) - each told to the user in their own words.
     */
    public enum NotWritten {
        /**
         * Nothing to compare it with - first-time startup, which has no working copy before it, or a working copy that
         * could not be read. It is taken in the first time its installation is played.
         */
        NOT_COMPARED,
        /**
         * Its installation did not take its device files, and a binds file naming a controller the installation does
         * not know is rejected by the game.
         */
        DEVICE_FILES_NOT_TAKEN
    }

    /**
     * What was done in one place.
     *
     * @param kind       the bindings folder or an installation
     * @param storefront an installation's storefront, as the stored list records it; {@code null} for the bindings
     *                   folder
     * @param folder     the bindings folder, the installation's root, or the installation's own bindings folder
     * @param written    the files written, by name - a file that already held the same bytes is not
     * @param removed      the files removed, by name - {@code .buttonMap}s the set lacks; in the bindings folder a
     *                     live-only binds file the user kept the working copy's side of, or a renamed one's old name
     * @param keptReadOnly the files that were kept because the user made them read-only, by name - a {@code .buttonMap}
     *                     or binds file that would have been removed. The screen tells the user how to let them go, in
     *                     their language (review, 2026-10-25)
     * @param failures     what could not be done, one line each - English, for the log and for support, built from the
     *                     system's own messages. A screen says that a place was not updated in its own words, and may
     *                     show these lines as the detail
     * @param differs      whether the installation was left alone because its device files differ from the working
     *                     copy - nothing was written to it, until it is reconciled (Alan, 2026-10-26); for its own
     *                     bindings folder, because its files were changed outside BindForge, until it is played
     *                     (linux-several-installations.md, rule 5)
     * @param moved        the files moved from the bindings folder to Leftover User Files, by name (Storage S13d)
     * @param movedTo      the folder they were moved to - {@code null} when none was
     * @param notWritten   why another installation's own bindings folder was left alone when it was not found changed
     *                     ({@link NotWritten}) - {@code null} when it was not, and for every other kind of place
     */
    public record Place(Kind kind, String storefront, Path folder, List<String> written, List<String> removed,
                        List<String> keptReadOnly, List<String> failures, boolean differs, List<String> moved,
                        Path movedTo, NotWritten notWritten) {
        public Place {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(folder, "folder");
            boolean installation = kind.ofAnInstallation();
            boolean hasStorefront = storefront != null;
            if (installation != hasStorefront) {
                throw new IllegalArgumentException("an installation has a storefront, and the bindings folder none");
            }
            if (differs && !installation) throw new IllegalArgumentException("only an installation can differ");
            if (notWritten != null && (kind != Kind.INSTALLATION_BINDINGS || differs)) {
                throw new IllegalArgumentException("only another installation's bindings folder left alone unchanged "
                        + "has a reason it was not written");
            }
            if (moved.isEmpty() != (movedTo == null)) {
                throw new IllegalArgumentException("files moved, and only then, name where they went");
            }
            written = List.copyOf(written);
            removed = List.copyOf(removed);
            keptReadOnly = List.copyOf(keptReadOnly);
            failures = List.copyOf(failures);
            moved = List.copyOf(moved);
        }

        /** A place written to, with what was moved, or that could not be. */
        public Place(Kind kind, String storefront, Path folder, List<String> written, List<String> removed,
                     List<String> keptReadOnly, List<String> failures, boolean differs, List<String> moved,
                     Path movedTo) {
            this(kind, storefront, folder, written, removed, keptReadOnly, failures, differs, moved, movedTo, null);
        }

        /** Another installation's own bindings folder, left alone for {@code reason} - nothing written to it. */
        public static Place unwritten(String storefront, Path folder, NotWritten reason) {
            return new Place(Kind.INSTALLATION_BINDINGS, storefront, folder, List.of(), List.of(), List.of(), List.of(),
                    false, List.of(), null, Objects.requireNonNull(reason, "reason"));
        }

        /** A place that was written to, or left alone, with nothing moved. */
        public Place(Kind kind, String storefront, Path folder, List<String> written, List<String> removed,
                     List<String> keptReadOnly, List<String> failures, boolean differs) {
            this(kind, storefront, folder, written, removed, keptReadOnly, failures, differs, List.of(), null);
        }

        /** A place that was written to, or could not be, with nothing moved. */
        public Place(Kind kind, String storefront, Path folder, List<String> written, List<String> removed,
                     List<String> keptReadOnly, List<String> failures) {
            this(kind, storefront, folder, written, removed, keptReadOnly, failures, false);
        }

        /**
         * Whether everything for this place was done - a kept read-only file, an installation left alone because it
         * differs, or another installation's bindings folder not written, means it does not match the set.
         */
        public boolean updated() {
            return failures.isEmpty() && keptReadOnly.isEmpty() && !differs && notWritten == null;
        }
    }

    /** How many installations took everything - the 2 of <em>"updated 2 of 3"</em>. */
    public long installationsUpdated() {
        return installations.stream().filter(Place::updated).count();
    }

    /** Whether every place took everything. */
    public boolean complete() {
        return bindings.updated() && installationsUpdated() == installations.size()
                && otherBindings.stream().allMatch(Place::updated);
    }
}
