package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.BindsDeviceReferences;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Kind;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Option;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Row;
import elite.intel.bindforge.devicefiles.LabelMerge;
import elite.intel.bindforge.storage.FirstSetupReading;
import elite.intel.bindforge.storage.InstallationFiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * The reconcile wizard's content, worked out from what was read - which questions to ask and in what order, the
 * counts the screens show, and what the result holds and why (multi-install-reconcile.md). Pure: no Swing, no text,
 * no live hardware - the screens put words to it, and the plugged-in state is passed in.
 * <p>
 * <strong>Questions are asked one at a time:</strong> names first, then which controller a name means, then button
 * names; within each, the one most of the user's controls depend on first. A question an earlier answer made moot - a
 * name that lost, asked which controller it means - is skipped.
 */
final class ReconcileQuestions {

    /** What a question asks, in the user's terms. */
    enum Type {
        /** One controller under different names - {@link Kind#NAME_DIFFERS}. */
        NAME,
        /** One name on different controllers - {@link Kind#HARDWARE_DIFFERS}. */
        CONTROLLER,
        /** A controller's buttons named differently - {@link Kind#LABELS_DIFFER}. */
        BUTTON_NAMES
    }

    /** Why a device is in the result, as the Check step says it. */
    enum Reason {
        /** The user picked it. */
        CHOSEN,
        /** Only some copies had it; it goes to the rest. */
        ADDED,
        /** Every copy already agreed. */
        SAME
    }

    /**
     * One controller any copy knows, for the Plug in step.
     *
     * @param hardware its VID/PID; empty for an entry that claims none
     * @param aliases  every name the copies hold for it, dead ones included
     * @param installs the copies that know it
     * @param controls how many bindings name any of its aliases
     */
    record Controller(HardwareId hardware, List<String> aliases, Set<Long> installs, int controls) {
        Controller {
            aliases = List.copyOf(aliases);
            installs = Set.copyOf(installs);
        }
    }

    /**
     * One device the result holds, for the Check step.
     *
     * @param kept       the device as the working copy will hold it
     * @param reason     why it is there
     * @param onlyIn     for {@link Reason#ADDED}, the copies that had it
     * @param movedHere  how many bindings move to its name from names that lost, or from a dead entry
     * @param labelsChosen whether the user answered its button names
     */
    record Outcome(FirstSetupPlan.Kept kept, Reason reason, Set<Long> onlyIn, int movedHere, boolean labelsChosen) {
        Outcome {
            onlyIn = Set.copyOf(onlyIn);
        }
    }

    /**
     * Something the result drops, for the Check step's <em>Also tidied up</em>.
     *
     * @param name     the alias dropped
     * @param movedTo  the alias its bindings move to; {@code null} when the bindings stay with this name on the
     *                 controller chosen - a controller that lost a name it shared
     * @param dead     whether it was a second name the game never used
     * @param hardware for a controller that lost its name, that controller
     */
    record Tidied(String name, String movedTo, boolean dead, HardwareId hardware) {
    }

    private final FirstSetupReading reading;
    private final FirstSetupPlan plan;
    private final List<Row> questions;

    ReconcileQuestions(FirstSetupReading reading) {
        this.reading = reading;
        this.plan = reading.plan();
        this.questions = ordered(plan.rows());
    }

    FirstSetupPlan plan() {
        return plan;
    }

    FirstSetupReading reading() {
        return reading;
    }

    /** Every question, in the order they are asked. */
    List<Row> questions() {
        return questions;
    }

    static Type typeOf(Row row) {
        return switch (row.kind()) {
            case NAME_DIFFERS -> Type.NAME;
            case HARDWARE_DIFFERS -> Type.CONTROLLER;
            case LABELS_DIFFER -> Type.BUTTON_NAMES;
            default -> throw new IllegalArgumentException(row.kind() + " is not a question");
        };
    }

    /** How many things are settled without asking: devices only some copies have, and dead entries. */
    int settledAlone() {
        return (int) plan.rows().stream()
                .filter(row -> row.kind() == Kind.ONLY_IN_SOME || row.kind() == Kind.SHADOWED).count();
    }

    /** How many devices every copy already agrees on - entry and button names both. */
    int alreadyMatching() {
        Set<String> labelsAsked = new LinkedHashSet<>();
        plan.rows().stream().filter(row -> row.kind() == Kind.LABELS_DIFFER)
                .forEach(row -> labelsAsked.addAll(plan.namesOfLabelDevice(row.deviceName())));
        return (int) plan.rows().stream()
                .filter(row -> row.kind() == Kind.IDENTICAL || row.kind() == Kind.BUILT_IN_LABELS)
                .filter(row -> !labelsAsked.contains(row.deviceName())).count();
    }

    /** How many bindings, across every binds file, name {@code name} - spelled exactly so. */
    int uses(String name) {
        return uses(List.of(name));
    }

    int uses(Collection<String> names) {
        int total = 0;
        for (Map<String, Integer> counts : reading.references().values()) {
            total += BindsDeviceReferences.bindingsNaming(names, counts);
        }
        return total;
    }

    /** How many bindings a question touches: every name it is about. */
    int controlsOf(Row row) {
        return switch (row.kind()) {
            case NAME_DIFFERS -> uses(row.options().stream().map(Option::key).toList());
            case LABELS_DIFFER -> uses(plan.namesOfLabelDevice(row.deviceName()));
            default -> uses(row.deviceName());
        };
    }

    /** The controller a question is about; empty for a name on several controllers, which is the question. */
    Optional<HardwareId> hardwareOf(Row row) {
        return switch (row.kind()) {
            case NAME_DIFFERS -> Optional.of(plan.hardwareOf(row));
            case LABELS_DIFFER -> plan.hardwareOfDevice(row.deviceName());
            default -> Optional.empty();
        };
    }

    /**
     * Whether a question still needs asking with these answers: answered already, or one the plan still waits on. A
     * question no answer is needed for - a name that lost, asked which controller it means - is not.
     */
    boolean needed(Row row, Map<String, String> answers) {
        if (isAnswered(row, answers)) return true;
        List<String> missing = plan.unanswered(answers);
        return keysOf(row).stream().anyMatch(missing::contains);
    }

    /** Whether the question has a full answer: every colliding button, for button names. */
    boolean isAnswered(Row row, Map<String, String> answers) {
        if (row.kind() == Kind.LABELS_DIFFER) {
            LabelMerge merge = plan.labelMerge(row.deviceName());
            return merge.unanswered(plan.labelAnswers(row.deviceName(), answers)).isEmpty();
        }
        String answer = answers.get(row.id());
        return answer != null && row.options().stream().anyMatch(option -> option.key().equals(answer));
    }

    /** The answers' keys a question fills. */
    List<String> keysOf(Row row) {
        if (row.kind() != Kind.LABELS_DIFFER) return List.of(row.id());
        return plan.labelMerge(row.deviceName()).collisions().stream()
                .map(collision -> FirstSetupPlan.labelAnswerKey(row.deviceName(), collision.input())).toList();
    }

    /** How many of a button-names question's buttons are answered. */
    int buttonsAnswered(Row row, Map<String, String> answers) {
        LabelMerge merge = plan.labelMerge(row.deviceName());
        return merge.collisions().size() - merge.unanswered(plan.labelAnswers(row.deviceName(), answers)).size();
    }

    /**
     * The question whose answer clashes with another - two controllers left on one VID/PID - once everything else is
     * answered. Empty when nothing clashes.
     */
    Optional<Row> clash(Map<String, String> answers) {
        List<String> missing = plan.unanswered(answers);
        return questions.stream().filter(row -> row.kind() == Kind.HARDWARE_DIFFERS && missing.contains(row.id()))
                .filter(row -> isAnswered(row, answers))
                .findFirst();
    }

    /** The copies whose files the shortcut can favour - each one holding a {@code DeviceMappings.xml}. */
    List<Long> shortcutSides() {
        return reading.installations().stream().filter(InstallationFiles::hasDeviceMappings)
                .map(InstallationFiles::installId).toList();
    }

    /**
     * Every controller any copy knows - the user's entries and Frontier's carrying the user's labels - one row per
     * controller, the ones the most bindings use first.
     */
    List<Controller> controllers() {
        Map<HardwareId, Set<String>> aliases = new LinkedHashMap<>();
        Map<HardwareId, Set<Long>> installs = new LinkedHashMap<>();
        List<Controller> unclaimed = new ArrayList<>();
        for (FirstSetupPlan.Installation install : plan.installations()) {
            install.elements().forEach((name, hardware) -> {
                if (hardware.vid().isEmpty() && hardware.pid().isEmpty()) {
                    unclaimed.add(new Controller(hardware, List.of(name), Set.of(install.installId()), uses(name)));
                    return;
                }
                aliases.computeIfAbsent(hardware, h -> new TreeSet<>()).add(name);
                installs.computeIfAbsent(hardware, h -> new LinkedHashSet<>()).add(install.installId());
            });
            install.builtIns().forEach((name, hardware) -> {
                aliases.computeIfAbsent(hardware, h -> new TreeSet<>()).add(name);
                installs.computeIfAbsent(hardware, h -> new LinkedHashSet<>()).add(install.installId());
            });
        }
        List<Controller> controllers = new ArrayList<>();
        aliases.forEach((hardware, names) -> controllers.add(new Controller(hardware, List.copyOf(names),
                installs.get(hardware), uses(names))));
        controllers.addAll(unclaimed);
        controllers.sort(Comparator.comparingInt(Controller::controls).reversed()
                .thenComparing(controller -> controller.aliases().getFirst(), String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(controllers);
    }

    /** What the result holds and why, for complete answers. */
    List<Outcome> outcomes(FirstSetupPlan.Resolution resolution, Map<String, String> answers) {
        Set<String> chosen = new LinkedHashSet<>();
        Set<String> labelsChosen = new LinkedHashSet<>();
        Map<String, Row> plainRows = new LinkedHashMap<>();
        for (Row row : plan.rows()) {
            switch (row.kind()) {
                case NAME_DIFFERS -> row.options().forEach(option -> chosen.add(option.key()));
                case HARDWARE_DIFFERS -> chosen.add(row.deviceName());
                case LABELS_DIFFER -> labelsChosen.addAll(plan.namesOfLabelDevice(row.deviceName()));
                case ONLY_IN_SOME, IDENTICAL, BUILT_IN_LABELS -> plainRows.put(row.deviceName(), row);
                default -> {
                }
            }
        }
        List<Outcome> outcomes = new ArrayList<>();
        for (FirstSetupPlan.Kept kept : resolution.devices()) {
            String name = kept.name();
            int movedHere = 0;
            for (Map.Entry<String, String> move : resolution.moves().entrySet()) {
                if (move.getValue().equals(name)) movedHere += uses(move.getKey());
            }
            Row plain = plainRows.get(name);
            Reason reason = chosen.contains(name) ? Reason.CHOSEN
                    : plain != null && plain.kind() == Kind.ONLY_IN_SOME ? Reason.ADDED
                    : labelsChosen.contains(name) ? Reason.CHOSEN
                    : Reason.SAME;
            Set<Long> onlyIn = reason == Reason.ADDED ? plain.presentIn() : Set.of();
            outcomes.add(new Outcome(kept, reason, onlyIn, movedHere, labelsChosen.contains(name)));
        }
        return List.copyOf(outcomes);
    }

    /** What the answers drop: dead entries, and each controller that lost a name it shared with another. */
    List<Tidied> tidied(FirstSetupPlan.Resolution resolution, Map<String, String> answers) {
        List<Tidied> tidied = new ArrayList<>();
        for (Row row : plan.rows()) {
            if (row.kind() == Kind.SHADOWED) {
                tidied.add(new Tidied(row.deviceName(), resolution.moves().get(row.deviceName()), true, null));
            } else if (row.kind() == Kind.HARDWARE_DIFFERS && answers.containsKey(row.id())) {
                boolean keptAtAll = resolution.devices().stream().anyMatch(kept -> kept.name().equals(row.deviceName()));
                if (!keptAtAll) continue;
                for (Option option : row.options()) {
                    if (option.key().equals(answers.get(row.id()))) continue;
                    tidied.add(new Tidied(row.deviceName(), null, false, FirstSetupPlan.hardwareOf(option)));
                }
            }
        }
        return List.copyOf(tidied);
    }

    /**
     * The kept devices the bindings use whose controller is not plugged in: the game falls back to keyboard and mouse
     * when one is missing (domain doc section 1.2b). Entries that claim no hardware are not controllers, and are left
     * out.
     *
     * @param connected the controllers plugged in now
     */
    List<FirstSetupPlan.Kept> unpluggedInUse(FirstSetupPlan.Resolution resolution, Set<HardwareId> connected) {
        return resolution.devices().stream()
                .filter(kept -> !kept.hardware().vid().isEmpty() || !kept.hardware().pid().isEmpty())
                .filter(kept -> !connected.contains(kept.hardware()))
                .filter(kept -> uses(kept.name()) > 0)
                .toList();
    }

    /** Questions in asking order: names, then controllers, then button names; the most controls first in each. */
    private List<Row> ordered(List<Row> rows) {
        List<Row> asked = new ArrayList<>(rows.stream().filter(Row::isQuestion).toList());
        asked.sort(Comparator.comparing(ReconcileQuestions::typeOf)
                .thenComparing(Comparator.comparingInt(this::controlsOf).reversed())
                .thenComparing(Row::deviceName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(asked);
    }
}
