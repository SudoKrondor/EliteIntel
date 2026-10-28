package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.BindsDeviceReferences;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.db.dao.BindForgeIdentityAnswerDao.AnswerRow;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The identity question: a controller bindings use is missing and an unnamed one is attached - is it the same controller
 * (spec, alias-designer.md, <em>What Changes When Hardware Changes</em>; Alan, 2026-10-15, A12). Pure - the caller
 * reads the devices, the bindings and the remembered answers.
 * <p>
 * <strong>One question per missing controller</strong>, each listing every candidate. Two sticks flashed together are
 * two missing and two new, all from one vendor, and pairing them by order would swap a user's hands; so BindForge
 * never pairs, the user picks. A candidate picked for one question drops out of the next ({@link Question#without}).
 * <p>
 * <strong>Asked only when bindings use the missing controller</strong> (Alan), as the missing-controller warning is:
 * the question exists to save bindings, and an entry for a stick sold years ago would otherwise come up whenever any
 * new controller is plugged in.
 * <p>
 * <strong>A controller the bindings know only by its hardware id is asked about too</strong> (Alan, 2026-10-17, A17):
 * an id in {@code .binds} that no controller listed has, no device of the user's has or is named, and Frontier does not
 * cover. A yes
 * there opens onboarding for the picked controller, whose FINISH names it and moves the bindings from the old id to the
 * name in one edit (Storage S4a, 2026-10-26).
 * <p>
 * <strong>An answer holds until the hardware changes</strong> (Alan): {@code NOT_NOW} and {@code NONE_OF_THESE} are
 * stored with the candidates the question listed, and stop matching when that set changes. A {@code NOT_NOW}
 * question is deferred - its candidates are held back from onboarding's automatic offer, so naming one cannot end the
 * question unasked, and CHECK AGAIN asks it again. {@code NONE_OF_THESE} frees them to be named.
 */
public final class IdentityQuestions {

    private IdentityQuestions() {
    }

    /** The two answers that are remembered. Picking a controller is not one: it retargets or names a device. */
    public enum Answer {
        NOT_NOW, NONE_OF_THESE
    }

    /**
     * One of the user's named controllers, as the edits hold it - what APPLY will write (Storage S4b, 2026-10-28).
     *
     * @param name     its name now
     * @param names    every name bindings may use for it. Since the edits rewrite the binds files with a rename, that is
     *                 its name now (Storage S4b); {@code null}s are ignored
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
     * @param deviceName the missing controller's name, or {@code null} when the bindings know it only by its hardware
     *                   id (A17)
     * @param was        the VID/PID it had - for a controller known by id, read from the id as the game writes it, VID
     *                   then PID
     * @param bindings   how many bindings use it - the number that makes the question worth answering
     * @param candidates the unnamed controllers attached, in the order My Devices lists them
     * @param foundAs    for a controller known by id, the ids {@code .binds} names it by, exactly as found - one, or the
     *                   same id in both orders; empty for a named controller
     */
    public record Question(String deviceName, HardwareId was, int bindings, List<MyDevice> candidates,
                           Set<String> foundAs) {
        public Question {
            Objects.requireNonNull(was, "was");
            candidates = List.copyOf(candidates);
            foundAs = Set.copyOf(foundAs);
            if ((deviceName == null) == foundAs.isEmpty()) {
                throw new IllegalArgumentException("A question names the controller or gives its ids, never both");
            }
        }

        /** A named controller's question (A12). */
        public Question(String deviceName, HardwareId was, int bindings, List<MyDevice> candidates) {
            this(Objects.requireNonNull(deviceName, "deviceName"), was, bindings, candidates, Set.of());
        }

        /** Whether the bindings name the missing controller - rather than knowing it only by its id. */
        public boolean named() {
            return deviceName != null;
        }

        /**
         * What an answer to the question is stored under, and what it is put once per run under. The name, or the old
         * id as {@code VID:PID} (Alan, 2026-10-17, A17). No name BindForge accepts holds a colon. A name first setup or
         * adopt took from an installation is not checked, and XML allows a colon in an element name - but it would have
         * to be exactly {@code VID:PID} to meet one. Eight hex characters alone could be a device name -
         * {@code ABCDEF12}.
         */
        public String answerKey() {
            return named() ? deviceName : (was.vid() + ":" + was.pid()).toUpperCase(Locale.ROOT);
        }

        /**
         * The old id as {@code .binds} writes it, for a controller known by id - what the user finds if they look. With
         * the same id in both orders, the one in the game's order, VID then PID.
         */
        public String idAsWritten() {
            return asWritten(was, foundAs);
        }

        /** Whether a candidate shares the missing controller's vendor - evidence, never a verdict. */
        public boolean sameVendor(MyDevice candidate) {
            return candidate.vid().equalsIgnoreCase(was.vid());
        }

        /** The question with a controller another question took left out. */
        public Question without(HardwareId taken) {
            return new Question(deviceName, was, bindings,
                    candidates.stream().filter(candidate -> !candidate.hardware().equals(taken)).toList(), foundAs);
        }

        /** The candidates as an answer is stored against them. */
        public String candidatesKey() {
            return hardwareSetKey(candidates.stream().map(MyDevice::hardware).toList());
        }
    }

    /**
     * What the questions about controllers known only by id are read from (A17).
     *
     * @param hardwareIds    {@code .binds}' hardware-id references, as {@link BindsDeviceReferences#hardwareIdCounts}
     *                       reads them
     * @param heldByWorkingCopy hardware the working copy's devices have - an id one of them has is no missing controller
     * @param frontierCovers whether Frontier's file covers some hardware - the game names it by Frontier's element
     * @param namesInUse     every name a device has in the edits or the working copy. A name of eight hex characters -
     *                       {@code DEADBEEF} - is read from {@code .binds} as an id, and is no missing controller: its
     *                       bindings work (review, 2026-10-17)
     */
    public record ById(Map<String, Integer> hardwareIds, Collection<HardwareId> heldByWorkingCopy,
                       Predicate<HardwareId> frontierCovers, Collection<String> namesInUse) {
        public static final ById NONE = new ById(Map.of(), List.of(), hardware -> false, List.of());

        public ById {
            hardwareIds = Map.copyOf(hardwareIds);
            heldByWorkingCopy = List.copyOf(heldByWorkingCopy);
            Objects.requireNonNull(frontierCovers, "frontierCovers");
            namesInUse = namesInUse.stream().filter(Objects::nonNull).toList();
        }
    }

    /**
     * What one refresh found.
     *
     * @param toAsk       the questions to put now, in the order of the user's devices, then those known by id
     * @param deferred    the questions answered {@code NOT_NOW} for the hardware attached now - CHECK AGAIN asks them
     * @param withheld    the candidates of both, which onboarding's automatic offer leaves alone
     * @param forget      keys of remembered answers that no longer apply: attached again, or no longer the user's
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

    /** The named controllers' questions alone (A12). */
    public static Reading read(List<Known> known, List<MyDevice> devices, Collection<HardwareId> heldByEdits,
                               Map<String, Integer> counts, Predicate<String> frontier, List<AnswerRow> answers) {
        return read(known, devices, heldByEdits, counts, frontier, answers, ById.NONE);
    }

    /**
     * @param known       the user's named controllers, in the order of the edits' {@code DeviceMappings.xml}
     * @param devices     My Devices, as the screen built it
     * @param heldByEdits hardware the edits already name - a controller named for APPLY, or one a retarget points at,
     *                    is spoken for and no candidate
     * @param counts      {@code .binds}' references by name, as {@link BindsDeviceReferences#referenceCounts} reads them
     * @param frontier    whether a name is one of Frontier's - never asked about, as the warning never names one
     * @param answers     the remembered answers
     * @param byId        what the questions about controllers known only by id are read from
     */
    public static Reading read(List<Known> known, List<MyDevice> devices, Collection<HardwareId> heldByEdits,
                               Map<String, Integer> counts, Predicate<String> frontier, List<AnswerRow> answers,
                               ById byId) {
        Set<HardwareId> attached = devices.stream()
                .filter(MyDevice::attached)
                .map(MyDevice::hardware)
                .collect(Collectors.toSet());
        Set<HardwareId> held = Set.copyOf(heldByEdits);
        List<MyDevice> candidates = DeviceNames.needingName(devices).stream()
                .filter(device -> !held.contains(device.hardware()))
                .toList();
        Map<String, AnswerRow> answered = new HashMap<>();
        answers.forEach(row -> answered.put(folded(row.deviceName()), row));

        List<Question> asked = new ArrayList<>();
        Set<String> missing = new HashSet<>();
        for (Known device : known) {
            if (frontier.test(device.name()) || attached.contains(device.hardware())) continue;
            missing.add(folded(device.name()));
            int bindings = BindsDeviceReferences.bindingsNaming(device.names(), counts);
            if (bindings == 0 || candidates.isEmpty()) continue;
            asked.add(new Question(device.name(), device.hardware(), bindings, candidates));
        }
        for (IdOnly missingId : knownById(known, devices, held, byId)) {
            if (candidates.isEmpty()) {
                // WHY: still missing, so an answer stored for it holds - only nothing to ask about it now.
                missing.add(folded((missingId.was().vid() + ":" + missingId.was().pid())));
                continue;
            }
            Question question = new Question(null, missingId.was(), missingId.bindings(), candidates,
                    missingId.foundAs());
            missing.add(folded(question.answerKey()));
            asked.add(question);
        }

        List<Question> toAsk = new ArrayList<>();
        List<Question> deferred = new ArrayList<>();
        for (Question question : asked) {
            AnswerRow answer = answered.get(folded(question.answerKey()));
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
     * A controller {@code .binds} knows only by hardware id that is missing (A17).
     *
     * @param foundAs the ids {@code .binds} names it by, exactly as found
     */
    private record IdOnly(HardwareId was, int bindings, Set<String> foundAs) {
    }

    /**
     * The controllers {@code .binds} knows only by hardware id and that are missing (A17): an id no controller My
     * Devices lists has - attached, or named in an installation - no device of the user's has <strong>or is
     * named</strong>, Frontier does not cover and no earlier yes already claims. The same id written in both orders is
     * one controller.
     * <p>
     * <strong>A name is checked as well as hardware</strong> (review, 2026-10-17): a device may be called
     * {@code DEADBEEF}, eight hex characters, which {@code .binds} is read as giving an id. Asked about, a yes would move
     * a working controller's bindings away.
     */
    private static List<IdOnly> knownById(List<Known> known, List<MyDevice> devices, Set<HardwareId> heldByEdits,
                                          ById byId) {
        List<HardwareId> owned = new ArrayList<>();
        devices.forEach(device -> owned.add(device.hardware()));
        known.forEach(device -> owned.add(device.hardware()));
        owned.addAll(heldByEdits);
        owned.addAll(byId.heldByWorkingCopy());
        Set<String> names = new HashSet<>();
        byId.namesInUse().forEach(name -> names.add(folded(name)));
        known.forEach(device -> device.names().forEach(name -> names.add(folded(name))));
        devices.forEach(device -> {
            if (device.entryName() != null) names.add(folded(device.entryName()));
            if (device.alias() != null) names.add(folded(device.alias()));
        });

        List<IdOnly> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : new TreeSet<>(byId.hardwareIds().keySet())) {
            if (!seen.add(id.toUpperCase(Locale.ROOT))) continue;
            HardwareId was = new HardwareId(id.substring(0, 4), id.substring(4));
            Map<String, Integer> group = BindsDeviceReferences.hardwareIdsFor(was.vid(), was.pid(), byId.hardwareIds());
            group.keySet().forEach(other -> seen.add(other.toUpperCase(Locale.ROOT)));
            if (owned.stream().anyMatch(hardware -> sameController(hardware, was))) continue;
            if (group.keySet().stream().anyMatch(value -> names.contains(folded(value)))) continue;
            if (byId.frontierCovers().test(was) || byId.frontierCovers().test(reversed(was))) continue;
            int bindings = group.values().stream().mapToInt(Integer::intValue).sum();
            if (bindings == 0) continue;
            found.add(new IdOnly(was, bindings, group.keySet()));
        }
        return found;
    }


    /**
     * An old id as {@code .binds} writes it (review, 2026-10-17): the value found in the game's order, VID then PID,
     * when the file holds one - else the first found, else the id itself. The same id is shown the same way by the
     * question and by the name page after it.
     *
     * @param found the values {@code .binds} names the controller by, exactly as found; may include other ids
     */
    public static String asWritten(HardwareId id, Collection<String> found) {
        String gameOrder = id.vid() + id.pid();
        List<String> sorted = new ArrayList<>(new TreeSet<>(found));
        return sorted.stream().filter(value -> value.equalsIgnoreCase(gameOrder)).findFirst()
                .or(() -> sorted.stream().filter(value -> value.equalsIgnoreCase(id.pid() + id.vid())).findFirst())
                .orElse(gameOrder);
    }

    /**
     * A set of controllers as one string: sorted upper-case {@code VID:PID} pairs joined by commas, so the same set
     * is the same key however it was gathered.
     */
    public static String hardwareSetKey(Collection<HardwareId> hardware) {
        return hardware.stream()
                .map(id -> (id.vid() + ":" + id.pid()).toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    // WHY: as BindsDeviceReferences.hardwareIdsFor reads an id - VID then PID, or the reverse a hand-edited file holds.
    private static boolean sameController(HardwareId hardware, HardwareId id) {
        return hardware.equals(id) || hardware.equals(reversed(id));
    }

    private static HardwareId reversed(HardwareId id) {
        return new HardwareId(id.pid(), id.vid());
    }

    // WHY: a device name is compared ignoring case everywhere it is matched, as the game matches it.
    private static String folded(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
