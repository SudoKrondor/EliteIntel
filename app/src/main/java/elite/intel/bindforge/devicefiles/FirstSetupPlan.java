package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceDivergence.Severity;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * First setup, as a decision: what the installations hold, where they disagree, and the devices the working copy
 * holds once the user has answered. Pure - reading the files is {@link FirstSetupReader}'s, making the working copy
 * from the answers is {@code bindforge.storage.FirstSetupFiles}'.
 * <p>
 * Settled 2026-09-23 - alias-designer.md, <em>First setup</em>:
 * <ul>
 *     <li><strong>Per device element, never per file.</strong> Frontier's elements are set aside before anything
 *     is compared; only the user's are reconciled.</li>
 *     <li><strong>Show everything, ask about little.</strong> Every element is a row. Only a name pointing at
 *     different hardware, hardware under different names, and labels that collide are questions.</li>
 *     <li>A label present in one file only is taken; identical labels are not asked about.</li>
 * </ul>
 * Settled while building it - 2026-10-07 (Alan):
 * <ul>
 *     <li><strong>Colliding labels can be answered per device</strong>, by an installation: that installation's
 *     label wins every collision it holds one for, and labels the others hold alone are still taken.</li>
 *     <li>{@link #answersFavouring} answers every question in one installation's favour. <em>Since 2026-10-24 "use one
 *     installation for all" takes that installation's files whole instead, warning when it removes an entry the
 *     bindings use - {@code bindforge.storage.FirstSetupFiles#useOne}.</em></li>
 * </ul>
 * After review - 2026-10-08 (Alan):
 * <ul>
 *     <li><strong>Two names for one VID/PID inside one installation are not a question.</strong> The game uses the
 *     first in the file, so the later ones are dead entries: they are shown, and not taken. A name conflict is asked
 *     only where different installations disagree.</li>
 *     <li><strong>A name that wins any name conflict is kept</strong>, even where it loses another - a stale PID in
 *     one installation must not cost the user the device. Answers that would leave two devices on one VID/PID are
 *     unanswered until changed.</li>
 * </ul>
 * Names that need Rename - 2026-10-17 (Alan, A16), reworked 2026-10-24 (Alan) and built on the working copy in
 * Storage S2b (2026-10-25):
 * <ul>
 *     <li><strong>Every name may win, and the bindings follow it.</strong> Every losing name is a move to the name
 *     that beat it, made in the working copy's binds files - a name differing only in case included, since in
 *     {@code .binds} {@code LVWAP} and {@code lvwap} are not the same name. A losing or shadowed name is not in the
 *     working copy.</li>
 * </ul>
 * The {@code .buttonMap} label merge - 2026-10-08 (Alan):
 * <ul>
 *     <li><strong>Colliding labels are answered per input</strong>, in the merge view ({@link LabelMerge}). The
 *     device keeps one row here; its options are the whole-file route, which answers every input at once.</li>
 * </ul>
 */
public final class FirstSetupPlan {

    /**
     * One installation as first setup reads it.
     *
     * @param elements the user's elements - every element whose name Frontier does not ship - by name, with its
     *                 primary VID/PID, <strong>in file order</strong> - which of two elements on one VID/PID the
     *                 game uses depends on it
     * @param builtIns Frontier-named elements this installation holds that carry labels of the user's or of
     *                 BindForge's, with the primary pair the file gives them. Only those with labels: a built-in
     *                 with none has nothing for the working copy
     * @param labels   labels by device name, for elements and built-ins alike. Frontier's own files are not here
     */
    public record Installation(long installId, String storefront, Map<String, HardwareId> elements,
                               Map<String, HardwareId> builtIns, Map<String, Map<String, String>> labels) {
        public Installation {
            Objects.requireNonNull(storefront, "storefront");
            elements = Collections.unmodifiableMap(new LinkedHashMap<>(elements));
            builtIns = Map.copyOf(builtIns);
            Map<String, Map<String, String>> copied = new LinkedHashMap<>();
            labels.forEach((device, map) -> copied.put(device, Map.copyOf(map)));
            labels = Map.copyOf(copied);
        }
    }

    public enum Kind {
        /** The user's element, the same in every installation. */
        IDENTICAL,
        /** The user's element, in some installations only. The working copy takes it. */
        ONLY_IN_SOME,
        /** One name, different VID/PID. A question. */
        HARDWARE_DIFFERS,
        /** One VID/PID under different names. A question - and a rename, see the class comment. */
        NAME_DIFFERS,
        /**
         * Installations label one or more of the device's inputs differently. A question, answered per input
         * ({@link #labelMerge(String)}); the row's options answer every input in one installation's favour.
         */
        LABELS_DIFFER,
        /** Labels on one of Frontier's devices, the same everywhere they appear. Taken. */
        BUILT_IN_LABELS,
        /**
         * An element below another on the same VID/PID in its installation, and the first nowhere. The game never
         * uses it, so the working copy does not take it.
         */
        SHADOWED
    }

    /**
     * One possible answer. Every answer may be chosen - since A16 a losing name's bindings follow the winner.
     *
     * @param key      what an answer names
     * @param text     what the user is shown: a VID/PID, a name, or an installation's storefront
     * @param installs the installations holding this value
     */
    public record Option(String key, String text, Set<Long> installs) {
        public Option {
            installs = Set.copyOf(installs);
        }
    }

    /**
     * One line of the list.
     *
     * @param deviceName the element name; for {@link Kind#NAME_DIFFERS} the names, joined
     * @param presentIn  the installations this row is about
     * @param options    the answers, for a question; empty otherwise
     */
    public record Row(String id, Kind kind, Severity severity, String deviceName, Set<Long> presentIn,
                      List<Option> options) {
        public Row {
            presentIn = Set.copyOf(presentIn);
            options = List.copyOf(options);
        }

        public boolean isQuestion() {
            return !options.isEmpty();
        }
    }

    /**
     * One device the working copy holds.
     *
     * @param name     its element name - for a built-in, Frontier's
     * @param hardware its VID/PID, as the user chose it
     * @param source   the installation whose element is taken whole - the one holding the chosen hardware
     * @param builtIn  one of Frontier's elements, which the working copy's {@code DeviceMappings.xml} already holds:
     *                 only its labels are the user's
     * @param labels   its labels, every collision answered; empty for a device with none
     */
    public record Kept(String name, HardwareId hardware, long source, boolean builtIn, Map<String, String> labels) {
        public Kept {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(hardware, "hardware");
            labels = Collections.unmodifiableMap(new LinkedHashMap<>(labels));
        }
    }

    /**
     * @param devices the devices the working copy holds: the user's elements by name, then the built-ins carrying
     *                labels
     * @param moves   every losing name, by the name its bindings move to - a case-only difference included
     */
    public record Resolution(List<Kept> devices, Map<String, String> moves) {
        public Resolution {
            devices = List.copyOf(devices);
            moves = Collections.unmodifiableMap(new TreeMap<>(moves));
        }

        public long buttonMapCount() {
            return devices.stream().filter(device -> !device.labels().isEmpty()).count();
        }
    }

    private static final String HARDWARE_ROW = "hardware:";
    private static final String NAME_ROW = "name:";
    private static final String LABELS_ROW = "labels:";
    private static final String SHADOWED_ROW = "shadowed:";

    private final List<Installation> installations;
    /** Each installation as the game sees it: an element below another on its VID/PID left out. */
    private final List<Installation> effective;
    /** Names shadowed in every installation holding them, with those installations. */
    private final Map<String, Set<Long>> shadowed;
    private final Set<String> referenced;
    private final List<Row> rows;
    /** Every name the installations hold for one VID/PID, where there is more than one. */
    private final Map<HardwareId, Set<String>> namesByHardware;

    /**
     * @param referenced the device names the binds files use - what ranks a row red
     */
    public FirstSetupPlan(List<Installation> installations, Set<String> referenced) {
        this.installations = List.copyOf(installations);
        this.referenced = Set.copyOf(referenced);
        this.effective = this.installations.stream().map(FirstSetupPlan::withoutShadowed).toList();
        this.shadowed = shadowedEverywhere(this.installations, this.effective);
        this.namesByHardware = conflictingNames(this.effective);
        this.rows = buildRows();
    }

    /** Questions first, then red, yellow and green, then by name. */
    public List<Row> rows() {
        return rows;
    }

    public List<Installation> installations() {
        return installations;
    }

    /** Whether there is anything of the user's to reconcile. */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * The questions still needing an answer: unanswered, or answered with something not on offer. A question about a
     * name that has just lost a name conflict needs none.
     * Colliding labels are counted per input, by {@link #labelAnswerKey(String, String)}.
     */
    public List<String> unanswered(Map<String, String> answers) {
        Set<String> losers = losers(answers).keySet();
        List<String> missing = new ArrayList<>();
        for (Row row : rows) {
            if (!row.isQuestion()) continue;
            if (row.kind() != Kind.NAME_DIFFERS && losers.contains(row.deviceName())) continue;
            if (row.kind() == Kind.LABELS_DIFFER) {
                String device = row.deviceName();
                labelMerge(device).unanswered(labelAnswers(device, answers))
                        .forEach(input -> missing.add(labelAnswerKey(device, input)));
                continue;
            }
            if (chosen(row, answers) == null) missing.add(row.id());
        }
        if (missing.isEmpty()) missing.addAll(clashing(answers, losers));
        return missing;
    }

    /** The answers that take one installation's side wherever it has one. A question where it has none is left. */
    public Map<String, String> answersFavouring(long installId) {
        Map<String, String> answers = new LinkedHashMap<>();
        for (Row row : rows) {
            if (row.kind() == Kind.LABELS_DIFFER) {
                answers.putAll(labelAnswersFavouring(row.deviceName(), installId));
                continue;
            }
            for (Option option : row.options()) {
                if (option.installs().contains(installId)) {
                    answers.put(row.id(), option.key());
                    break;
                }
            }
        }
        return answers;
    }

    /**
     * One device's labels across the installations, keyed by installation id as text - the merge view's rows.
     * A device no installation labels gives a merge with nothing in it.
     */
    public LabelMerge labelMerge(String device) {
        Map<String, Map<String, String>> sides = new LinkedHashMap<>();
        labelHolders(device).forEach((install, labels) -> sides.put(String.valueOf(install), labels));
        return new LabelMerge(sides);
    }

    /** Where the answer for one of a device's colliding inputs is kept in the answers. */
    public static String labelAnswerKey(String device, String input) {
        return LABELS_ROW + device + ":" + input;
    }

    /** The answers given for a device's colliding inputs, input to label. */
    public Map<String, String> labelAnswers(String device, Map<String, String> answers) {
        Map<String, String> chosen = new LinkedHashMap<>();
        for (LabelMerge.Collision collision : labelMerge(device).collisions()) {
            String label = answers.get(labelAnswerKey(device, collision.input()));
            if (label != null) chosen.put(collision.input(), label);
        }
        return chosen;
    }

    /**
     * The whole-file route for one device - <em>use Steam's labels for this device</em> - as answers. An input the
     * installation holds no label for is left unanswered.
     */
    public Map<String, String> labelAnswersFavouring(String device, long installId) {
        Map<String, String> answers = new LinkedHashMap<>();
        labelMerge(device).answersFavouring(String.valueOf(installId))
                .forEach((input, label) -> answers.put(labelAnswerKey(device, input), label));
        return answers;
    }

    /**
     * The devices the answers produce.
     *
     * @throws IllegalStateException if a question is still unanswered - see {@link #unanswered(Map)}
     */
    public Resolution resolve(Map<String, String> answers) {
        List<String> missing = unanswered(answers);
        if (!missing.isEmpty()) throw new IllegalStateException("first setup is not answered: " + missing);
        Map<String, String> losers = losers(answers);

        List<Kept> devices = new ArrayList<>();
        for (String name : elementNames()) {
            if (losers.containsKey(name)) continue;
            HardwareId hardware = hardwareFor(name, answers);
            devices.add(new Kept(name, hardware, sourceOf(name, hardware), false, labelsFor(name, answers)));
        }
        for (String name : builtInNames()) {
            Installation holder = firstBuiltInHolder(name);
            devices.add(new Kept(name, holder.builtIns().get(name), holder.installId(), true,
                    labelsFor(name, answers)));
        }
        return new Resolution(devices, losers);
    }

    /** The first installation, in list order, whose element under {@code name} has {@code hardware}. */
    private long sourceOf(String name, HardwareId hardware) {
        for (Installation install : effective) {
            if (hardware.equals(install.elements().get(name))) return install.installId();
        }
        throw new IllegalStateException("no installation holds " + name + " on " + hardware);
    }

    /**
     * The hardware questions behind two kept devices ending on one VID/PID. Only answers can do that, when a name
     * keeps the hardware another name won.
     */
    private List<String> clashing(Map<String, String> answers, Set<String> losers) {
        Map<HardwareId, List<String>> namesOn = new LinkedHashMap<>();
        for (String name : elementNames()) {
            if (losers.contains(name)) continue;
            namesOn.computeIfAbsent(hardwareFor(name, answers), h -> new ArrayList<>()).add(name);
        }
        Set<String> rowIds = new LinkedHashSet<>();
        namesOn.forEach((hardware, names) -> {
            if (names.size() < 2 || claimsNothing(hardware)) return;
            for (String name : names) {
                if (new LinkedHashSet<>(hardwareByInstall(name).values()).size() > 1) rowIds.add(HARDWARE_ROW + name);
            }
        });
        return List.copyOf(rowIds);
    }

    private List<Row> buildRows() {
        List<Row> built = new ArrayList<>();
        Set<String> inNameConflict = new LinkedHashSet<>();
        namesByHardware.forEach((hardware, names) -> {
            inNameConflict.addAll(names);
            built.add(nameRow(hardware, names));
        });

        for (String name : elementNames()) {
            Map<Long, HardwareId> byInstall = hardwareByInstall(name);
            Set<HardwareId> distinct = new LinkedHashSet<>(byInstall.values());
            if (distinct.size() > 1) {
                built.add(hardwareRow(name, byInstall));
            } else if (!inNameConflict.contains(name)) {
                boolean everywhere = byInstall.size() == installations.size();
                Severity severity = everywhere ? Severity.GREEN
                        : referenced.contains(name) ? Severity.RED : Severity.YELLOW;
                built.add(new Row(name, everywhere ? Kind.IDENTICAL : Kind.ONLY_IN_SOME, severity, name,
                        byInstall.keySet(), List.of()));
            }
            labelsRow(name).ifPresent(built::add);
        }
        for (String name : builtInNames()) {
            built.add(labelsRow(name).orElse(new Row(LABELS_ROW + name, Kind.BUILT_IN_LABELS, Severity.GREEN, name,
                    labelHolders(name).keySet(), List.of())));
        }
        shadowed.forEach((name, installs) -> built.add(new Row(SHADOWED_ROW + name, Kind.SHADOWED,
                referenced.contains(name) ? Severity.RED : Severity.YELLOW, name, installs, List.of())));
        built.sort(Comparator.comparing((Row row) -> !row.isQuestion())
                .thenComparing(Row::severity, Comparator.reverseOrder())
                .thenComparing(Row::deviceName));
        return List.copyOf(built);
    }

    private Row nameRow(HardwareId hardware, Set<String> names) {
        List<Option> options = new ArrayList<>();
        Set<Long> present = new LinkedHashSet<>();
        for (String name : names) {
            Set<Long> holding = new LinkedHashSet<>();
            effective.forEach(install -> {
                if (hardware.equals(install.elements().get(name))) holding.add(install.installId());
            });
            present.addAll(holding);
            options.add(new Option(name, name, holding));
        }
        boolean anyReferenced = names.stream().anyMatch(referenced::contains);
        return new Row(NAME_ROW + hardware.vid() + ":" + hardware.pid(), Kind.NAME_DIFFERS,
                anyReferenced ? Severity.RED : Severity.YELLOW, String.join(" / ", names), present, options);
    }

    private Row hardwareRow(String name, Map<Long, HardwareId> byInstall) {
        Map<HardwareId, Set<Long>> installsByHardware = new LinkedHashMap<>();
        byInstall.forEach((install, hardware) ->
                installsByHardware.computeIfAbsent(hardware, h -> new LinkedHashSet<>()).add(install));
        List<Option> options = new ArrayList<>();
        installsByHardware.forEach((hardware, installs) ->
                options.add(new Option(key(hardware), hardware.toString(), installs)));
        return new Row(HARDWARE_ROW + name, Kind.HARDWARE_DIFFERS, Severity.RED, name, byInstall.keySet(), options);
    }

    private Optional<Row> labelsRow(String name) {
        Map<Long, Map<String, String>> holders = labelHolders(name);
        if (labelMerge(name).collisions().isEmpty()) return Optional.empty();
        List<Option> options = new ArrayList<>();
        for (Installation install : installations) {
            if (!holders.containsKey(install.installId())) continue;
            options.add(new Option(String.valueOf(install.installId()), install.storefront(),
                    Set.of(install.installId())));
        }
        return Optional.of(new Row(LABELS_ROW + name, Kind.LABELS_DIFFER, Severity.YELLOW, name,
                holders.keySet(), options));
    }

    /** The union of every installation's labels for the device, each colliding input taking its answer. */
    private Map<String, String> labelsFor(String name, Map<String, String> answers) {
        return labelMerge(name).merged(labelAnswers(name, answers));
    }

    /**
     * Names beaten in a name conflict and winning none, each with the name that beat it - in the first conflict it
     * lost, in the list's order, should it lose more than one. A name that wins anywhere is kept.
     */
    private Map<String, String> losers(Map<String, String> answers) {
        Map<String, String> losers = new LinkedHashMap<>();
        Set<String> winners = new LinkedHashSet<>();
        for (Row row : rows) {
            if (row.kind() != Kind.NAME_DIFFERS) continue;
            Option chosen = chosen(row, answers);
            if (chosen == null) continue;
            winners.add(chosen.key());
            row.options().stream().map(Option::key).filter(name -> !name.equals(chosen.key()))
                    .forEach(name -> losers.putIfAbsent(name, chosen.key()));
        }
        losers.keySet().removeAll(winners);
        return losers;
    }

    private HardwareId hardwareFor(String name, Map<String, String> answers) {
        Map<Long, HardwareId> byInstall = hardwareByInstall(name);
        String answer = answers.get(HARDWARE_ROW + name);
        if (answer != null) {
            for (HardwareId hardware : byInstall.values()) {
                if (key(hardware).equals(answer)) return hardware;
            }
        }
        return byInstall.values().iterator().next();
    }

    private Installation firstBuiltInHolder(String name) {
        for (Installation install : installations) {
            if (install.builtIns().containsKey(name)) return install;
        }
        throw new IllegalStateException("no installation holds built-in " + name);
    }

    private static Option chosen(Row row, Map<String, String> answers) {
        String answer = answers.get(row.id());
        if (answer == null) return null;
        return row.options().stream().filter(option -> option.key().equals(answer)).findFirst().orElse(null);
    }

    private Map<Long, HardwareId> hardwareByInstall(String name) {
        Map<Long, HardwareId> byInstall = new LinkedHashMap<>();
        effective.forEach(install -> {
            HardwareId hardware = install.elements().get(name);
            if (hardware != null) byInstall.put(install.installId(), hardware);
        });
        return byInstall;
    }

    private Map<Long, Map<String, String>> labelHolders(String name) {
        Map<Long, Map<String, String>> holders = new LinkedHashMap<>();
        installations.forEach(install -> {
            Map<String, String> labels = install.labels().get(name);
            if (labels != null && !labels.isEmpty()) holders.put(install.installId(), labels);
        });
        return holders;
    }

    private Set<String> elementNames() {
        Set<String> names = new TreeSet<>();
        effective.forEach(install -> names.addAll(install.elements().keySet()));
        return names;
    }

    /** Built-ins carrying labels - every built-in an installation reports carries some. */
    private Set<String> builtInNames() {
        Set<String> names = new TreeSet<>();
        installations.forEach(install -> names.addAll(install.builtIns().keySet()));
        return names;
    }

    /**
     * The installation with every element below another on the same VID/PID left out: the game uses the first in
     * the file (domain doc section 1.2d), so the rest are not a choice the user made.
     */
    private static Installation withoutShadowed(Installation install) {
        Map<String, HardwareId> kept = new LinkedHashMap<>();
        Set<HardwareId> claimed = new LinkedHashSet<>();
        install.elements().forEach((name, hardware) -> {
            if (claimsNothing(hardware) || claimed.add(hardware)) kept.put(name, hardware);
        });
        return new Installation(install.installId(), install.storefront(), kept, install.builtIns(),
                install.labels());
    }

    /** Names left out of every installation holding them. One kept anywhere is in play, not shadowed. */
    private static Map<String, Set<Long>> shadowedEverywhere(List<Installation> installations,
                                                            List<Installation> effective) {
        Set<String> kept = new LinkedHashSet<>();
        effective.forEach(install -> kept.addAll(install.elements().keySet()));
        Map<String, Set<Long>> shadowed = new TreeMap<>();
        installations.forEach(install -> install.elements().keySet().forEach(name -> {
            if (!kept.contains(name)) {
                shadowed.computeIfAbsent(name, n -> new LinkedHashSet<>()).add(install.installId());
            }
        }));
        return shadowed;
    }

    /** An element with no VID/PID claims no hardware, so two of them are not one device under two names. */
    private static boolean claimsNothing(HardwareId hardware) {
        return hardware.vid().isEmpty() && hardware.pid().isEmpty();
    }

    /** Hardware held under different names - by different installations, as each is already unshadowed. */
    private static Map<HardwareId, Set<String>> conflictingNames(List<Installation> installations) {
        Map<HardwareId, Set<String>> byHardware = new LinkedHashMap<>();
        installations.forEach(install -> install.elements().forEach((name, hardware) -> {
            if (claimsNothing(hardware)) return;
            byHardware.computeIfAbsent(hardware, h -> new TreeSet<>()).add(name);
        }));
        byHardware.values().removeIf(names -> names.size() < 2);
        return byHardware;
    }

    private static String key(HardwareId hardware) {
        return hardware.vid() + ":" + hardware.pid();
    }
}
