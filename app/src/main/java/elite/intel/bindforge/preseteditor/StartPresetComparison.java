package elite.intel.bindforge.preseteditor;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;

import java.util.List;
import java.util.Objects;

/**
 * The game's {@code StartPreset.4.start} and the draft, each against the master, line by line.
 * <p>
 * The four lines are four independent entities (preset-editor.md): a game change to one line and a draft change to
 * another merge with nothing to ask, and only the same line changed differently on both sides is a conflict.
 * Values are compared, never bytes, so the single-line form equals four identical lines.
 *
 * @param lines General, Ship, SRV and On Foot, in file order
 */
public record StartPresetComparison(List<Line> lines) {

    /**
     * What happened to one line since the master.
     */
    public enum Change {
        /** The master, the draft and the game agree. */
        UNCHANGED,
        /** Only the draft moved; the game still holds the master's value. */
        DRAFT_ONLY,
        /** Only the game moved; the draft, if any, still holds the master's value. */
        GAME_ONLY,
        /** Both moved, to the same value. Nothing to ask; the master is the one behind. */
        BOTH_SAME,
        /** Both moved, to different values. */
        CONFLICT
    }

    /**
     * One line's three values. What they add up to is worked out from them, never stored, so a line cannot claim
     * a {@link Change} its values do not support.
     */
    public record Line(BindingSection section, String master, String draft, String game) {

        public Line {
            Objects.requireNonNull(section, "section");
            Objects.requireNonNull(master, "master");
            Objects.requireNonNull(draft, "draft");
            Objects.requireNonNull(game, "game");
        }

        /**
         * What happened to this line since the master.
         */
        public Change change() {
            if (draftMoved() && gameMoved()) {
                return draft.equals(game) ? Change.BOTH_SAME : Change.CONFLICT;
            }
            if (draftMoved()) {
                return Change.DRAFT_ONLY;
            }
            if (gameMoved()) {
                return Change.GAME_ONLY;
            }
            return Change.UNCHANGED;
        }

        /**
         * Whether the game's line differs from the master's.
         */
        public boolean gameMoved() {
            return !game.equals(master);
        }

        private boolean draftMoved() {
            return !draft.equals(master);
        }
    }

    /**
     * @throws IllegalArgumentException unless the lines are exactly General, Ship, SRV and On Foot, in that order
     */
    public StartPresetComparison {
        lines = List.copyOf(lines);
        List<BindingSection> sections = lines.stream().map(Line::section).toList();
        if (!sections.equals(StartPresetLines.SECTIONS)) {
            throw new IllegalArgumentException("A comparison needs one line per section, in file order, not " + sections);
        }
    }

    /**
     * Compares the three.
     *
     * @param draft the draft, or the master when there is none - {@link StartPresetStore#draftOrMaster()}
     */
    public static StartPresetComparison of(StartPresetSections master, StartPresetSections draft,
                                           StartPresetSections game) {
        Objects.requireNonNull(master, "master");
        Objects.requireNonNull(draft, "draft");
        Objects.requireNonNull(game, "game");
        List<Line> lines = StartPresetLines.SECTIONS.stream()
                .map(section -> new Line(section, master.forSection(section), draft.forSection(section),
                        game.forSection(section)))
                .toList();
        return new StartPresetComparison(lines);
    }

    /**
     * The line for one section.
     *
     * @throws IllegalArgumentException for {@link BindingSection#OTHER}, which has no line
     */
    public Line line(BindingSection section) {
        return lines.stream().filter(line -> line.section() == section).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("StartPreset.4.start has no line for section " + section));
    }

    /**
     * Whether every line is {@link Change#UNCHANGED}.
     */
    public boolean allUnchanged() {
        return lines.stream().allMatch(line -> line.change() == Change.UNCHANGED);
    }

    /**
     * Whether the game's file differs from the master on any line.
     */
    public boolean gameChanged() {
        return lines.stream().anyMatch(Line::gameMoved);
    }

    /**
     * The lines both sides changed to different values.
     */
    public List<Line> conflicts() {
        return lines.stream().filter(line -> line.change() == Change.CONFLICT).toList();
    }
}
