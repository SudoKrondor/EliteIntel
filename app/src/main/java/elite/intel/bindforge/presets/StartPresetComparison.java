package elite.intel.bindforge.presets;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;

import java.util.List;
import java.util.Objects;

/**
 * The game's {@code StartPreset} and the draft, each against the master, line by line.
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
     * One line's three values and what they add up to.
     */
    public record Line(BindingSection section, String master, String draft, String game, Change change) {
    }

    public StartPresetComparison {
        lines = List.copyOf(lines);
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
                .map(section -> line(section, master.forSection(section), draft.forSection(section),
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
                .orElseThrow(() -> new IllegalArgumentException("StartPreset has no line for section " + section));
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
        return lines.stream().anyMatch(line -> !line.game().equals(line.master()));
    }

    /**
     * The lines both sides changed to different values.
     */
    public List<Line> conflicts() {
        return lines.stream().filter(line -> line.change() == Change.CONFLICT).toList();
    }

    private static Line line(BindingSection section, String master, String draft, String game) {
        boolean draftMoved = !draft.equals(master);
        boolean gameMoved = !game.equals(master);
        Change change;
        if (!draftMoved && !gameMoved) {
            change = Change.UNCHANGED;
        } else if (!gameMoved) {
            change = Change.DRAFT_ONLY;
        } else if (!draftMoved) {
            change = Change.GAME_ONLY;
        } else if (draft.equals(game)) {
            change = Change.BOTH_SAME;
        } else {
            change = Change.CONFLICT;
        }
        return new Line(section, master, draft, game, change);
    }
}
