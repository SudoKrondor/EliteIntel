package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.BindsDeviceReferences;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.db.dao.BindForgeIdentityAnswerDao.AnswerRow;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The identity question: a named controller is missing and an unnamed one is attached - is it the same controller
 * (spec, alias-designer.md, <em>What Changes When Hardware Changes</em>; Alan, 2026-10-15, A12). Pure - the caller
 * reads the devices, the bindings and the remembered answers.
 * <p>
 * <strong>One question per missing controller</strong>, each listing every candidate. Two sticks flashed together are
 * two missing and two new, all from one vendor, and pairing them by order would swap a user's hands; so BindForge
 * never pairs, the user picks. A candidate picked for one question drops out of the next ({@link Question#without}).
 * <p>
 * <strong>Asked only when bindings use the missing controller</strong> (Alan), as the missing-controller warning is:
 * the question exists to save bindings, and an entry for a stick sold years ago would otherwise come up whenever any
 * new controller is plugged in. <strong>Named controllers only</strong> - one the bindings know by hardware id alone is
 * slice A17's.
 * <p>
 * <strong>An answer holds until the hardware changes</strong> (Alan): {@code NOT_NOW} and {@code NONE_OF_THESE} are
 * stored with the candidates the question listed, and stop matching when that set changes. A {@code NOT_NOW}
 * question is deferred - its candidates are held back from onboarding's automatic offer, so naming one cannot end the
 * question unasked, and CHECK AGAIN asks it again. {@code NONE_OF_THESE} frees them to be named.
 */
public final class IdentityQuestions {

    private IdentityQuestions() {
    }

    /** The two answers that are remembered. Picking a controller is not one: it retargets the device. */
    public enum Answer {
        NOT_NOW, NONE_OF_THESE
    }

    /**
     * One of the user's named controllers, as it will be after the next Apply - the draft's row when a draft is
     * started, the master's otherwise.
     *
     * @param name     its name now
     * @param names    every name bindings may use for it: now, in the master, and an old name a rename is still moving
     *                 away from. {@code null}s are ignored
     * @param hardware its VID/PID
     */
    public record Known(String name, Collection<String> names, HardwareId hardware) {
        public Known {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(hardware, "hardware");
            names = names.stream().filter(Objects::nonNull).toList();
        }
    }

    /**
     * One question: which attached controller is the missing one.
     *
     * @param deviceName the missing controller's name
     * @param was        the VID/PID it had
     * @param bindings   how many bindings use it - the number that makes the question worth answering
     * @param candidates the unnamed controllers attached, in the order My Devices lists them
     */
    public record Question(String deviceName, HardwareId was, int bindings, List<MyDevice> candidates) {
        public Question {
            Objects.requireNonNull(deviceName, "deviceName");
            Objects.requireNonNull(was, "was");
            candidates = List.copyOf(candidates);
        }

        /** Whether a candidate shares the missing controller's vendor - evidence, never a verdict. */
        public boolean sameVendor(MyDevice candidate) {
            return candidate.vid().equalsIgnoreCase(was.vid());
        }

        /** The question with a controller another question took left out. */
        public Question without(HardwareId taken) {
            return new Question(deviceName, was, bindings,
                    candidates.stream().filter(candidate -> !candidate.hardware().equals(taken)).toList());
        }

        /** The candidates as an answer is stored against them. */
        public String candidatesKey() {
            return key(candidates.stream().map(MyDevice::hardware).toList());
        }
    }

    /**
     * What one refresh found.
     *
     * @param toAsk    the questions to put now, in the order of the user's devices
     * @param deferred the questions answered {@code NOT_NOW} for the hardware attached now - CHECK AGAIN asks them
     * @param withheld the candidates of both, which onboarding's automatic offer leaves alone
     * @param forget   devices whose remembered answer no longer applies: attached again, or no longer the user's
     */
    public record Reading(List<Question> toAsk, List<Question> deferred, Set<HardwareId> withheld,
                          Set<String> forget) {
        public static final Reading NONE = new Reading(List.of(), List.of(), Set.of(), Set.of());

        public Reading {
            toAsk = List.copyOf(toAsk);
            deferred = List.copyOf(deferred);
            withheld = Set.copyOf(withheld);
            forget = Set.copyOf(forget);
        }

        /** The questions CHECK AGAIN puts: the deferred ones too, since the user pressed it to deal with them. */
        public List<Question> askingDeferred() {
            List<Question> all = new ArrayList<>(toAsk);
            all.addAll(deferred);
            return all;
        }
    }

    /**
     * @param known       the user's named controllers, in the order of the master
     * @param devices     My Devices, as the screen built it
     * @param heldByDraft hardware the draft already names - a controller named for Apply, or one a retarget points at,
     *                    is spoken for and no candidate
     * @param counts      {@code .binds}' references by name, as {@link BindsDeviceReferences#referenceCounts} reads them
     * @param frontier    whether a name is one of Frontier's - never asked about, as the warning never names one
     * @param answers     the remembered answers
     */
    public static Reading read(List<Known> known, List<MyDevice> devices, Collection<HardwareId> heldByDraft,
                               Map<String, Integer> counts, Predicate<String> frontier, List<AnswerRow> answers) {
        Set<HardwareId> attached = devices.stream()
                .filter(MyDevice::attached)
                .map(MyDevice::hardware)
                .collect(Collectors.toSet());
        Set<HardwareId> held = Set.copyOf(heldByDraft);
        List<MyDevice> candidates = DeviceNames.needingName(devices).stream()
                .filter(device -> !held.contains(device.hardware()))
                .toList();
        Map<String, AnswerRow> answered = new HashMap<>();
        answers.forEach(row -> answered.put(folded(row.deviceName()), row));

        List<Question> toAsk = new ArrayList<>();
        List<Question> deferred = new ArrayList<>();
        Set<String> missing = new HashSet<>();
        for (Known device : known) {
            if (frontier.test(device.name()) || attached.contains(device.hardware())) continue;
            missing.add(folded(device.name()));
            int bindings = BindsDeviceReferences.bindingsNaming(device.names(), counts);
            if (bindings == 0 || candidates.isEmpty()) continue;
            Question question = new Question(device.name(), device.hardware(), bindings, candidates);
            AnswerRow answer = answered.get(folded(device.name()));
            if (answer == null || !answer.candidates().equals(question.candidatesKey())) {
                toAsk.add(question);
            } else if (Answer.NOT_NOW.name().equals(answer.answer())) {
                deferred.add(question);
            }
        }

        Set<HardwareId> withheld = new HashSet<>();
        toAsk.forEach(question -> question.candidates().forEach(candidate -> withheld.add(candidate.hardware())));
        deferred.forEach(question -> question.candidates().forEach(candidate -> withheld.add(candidate.hardware())));
        Set<String> forget = answers.stream()
                .map(AnswerRow::deviceName)
                .filter(name -> !missing.contains(folded(name)))
                .collect(Collectors.toSet());
        return new Reading(toAsk, deferred, withheld, forget);
    }

    /**
     * A set of controllers as one string: sorted upper-case {@code VID:PID} pairs joined by commas, so the same set
     * is the same key however it was gathered.
     */
    public static String key(Collection<HardwareId> hardware) {
        return hardware.stream()
                .map(id -> (id.vid() + ":" + id.pid()).toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    // WHY: a device name is compared ignoring case everywhere it is matched, as the game matches it.
    private static String folded(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
