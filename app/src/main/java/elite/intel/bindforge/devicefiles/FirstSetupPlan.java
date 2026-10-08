package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceDivergence.Severity;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;

import java.util.ArrayList;
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
 * First setup, as a decision: what the installations hold, where they disagree, and the master that results once
 * the user has answered. Pure - reading the files is {@link FirstSetupReader}'s, writing is {@link FirstSetup}'s.
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
 *     <li><strong>A name conflict is answered only where no binding is lost.</strong> The rename transaction and
 *     its {@code .binds} rewrite are not built, so a name whose loser {@code .binds} references cannot be chosen.
 *     The loser's element and {@code .buttonMap} stay on disk, below the winner, as a pending removal.</li>
 *     <li><strong>Colliding labels are answered per device</strong>, by an installation: that installation's label
 *     wins every collision, and labels the others hold alone are still taken. The per-input merge view is A2.</li>
 *     <li><strong>"Use one installation as the master"</strong> answers every question in its favour, and still
 *     takes the elements only the others hold - first setup never drops a custom entry.</li>
 * </ul>
 */
public final class FirstSetupPlan {

    /**
     * One installation as first setup reads it.
     *
     * @param elements the user's elements - every element whose name Frontier does not ship - by name, with its
     *                 primary VID/PID
     * @param builtIns Frontier-named elements this installation holds that carry labels of the user's or of
     *                 BindForge's, with the primary pair the file gives them
     * @param labels   labels by device name, for elements and built-ins alike. Frontier's own files are not here
     */
    public record Installation(long installId, String storefront, Map<String, HardwareId> elements,
                               Map<String, HardwareId> builtIns, Map<String, Map<String, String>> labels) {
        public Installation {
            Objects.requireNonNull(storefront, "storefront");
            elements = Map.copyOf(elements);
            builtIns = Map.copyOf(builtIns);
            Map<String, Map<String, String>> copied = new LinkedHashMap<>();
            labels.forEach((device, map) -> copied.put(device, Map.copyOf(map)));
            labels = Map.copyOf(copied);
        }
    }

    public enum Kind {
        /** The user's element, the same in every installation. */
        IDENTICAL,
        /** The user's element, in some installations only. The master takes it. */
        ONLY_IN_SOME,
        /** One name, different VID/PID. A question. */
        HARDWARE_DIFFERS,
        /** One VID/PID under different names. A question - and a rename, see the class comment. */
        NAME_DIFFERS,
        /** Two installations label one input differently. A question. */
        LABELS_DIFFER,
        /** Labels on one of Frontier's devices, the same everywhere they appear. Taken. */
        BUILT_IN_LABELS
    }

    /**
     * One possible answer.
     *
     * @param key       what an answer names
     * @param text      what the user is shown: a VID/PID, a name, or an installation's storefront
     * @param installs  the installations holding this value
     * @param available whether it may be chosen. Only a name whose choice would orphan bindings is not
     */
    public record Option(String key, String text, Set<Long> installs, boolean available) {
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
     * @param devices         the master as it is to be
     * @param pendingRemovals names that lost a name conflict. Their entries stay in the installations, below the
     *                        winner, until something removes them
     */
    public record Resolution(List<StoredDevice> devices, List<String> pendingRemovals) {
        public Resolution {
            devices = List.copyOf(devices);
            pendingRemovals = List.copyOf(pendingRemovals);
        }

        public long buttonMapCount() {
            return devices.stream().filter(device -> !device.labels().isEmpty()).count();
        }
    }

    private static final String HARDWARE_ROW = "hardware:";
    private static final String NAME_ROW = "name:";
    private static final String LABELS_ROW = "labels:";

    private final List<Installation> installations;
    private final Set<String> referenced;
    private final List<Row> rows;
    /** Every name the installations hold for one VID/PID, where there is more than one. */
    private final Map<HardwareId, Set<String>> namesByHardware;

    /**
     * @param referenced the device names the shared {@code .binds} uses - what ranks a row red, and what makes a
     *                   name impossible to give up
     */
    public FirstSetupPlan(List<Installation> installations, Set<String> referenced) {
        this.installations = List.copyOf(installations);
        this.referenced = Set.copyOf(referenced);
        this.namesByHardware = conflictingNames(this.installations);
        this.rows = buildRows();
    }

    /** Questions first, then red, yellow and green, then by name. */
    public List<Row> rows() {
        return rows;
    }

    public List<Installation> installations() {
        return installations;
    }

    /** Whether there is anything of the user's to take into a master. */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * The questions still needing an answer: unanswered, answered with something not on offer, or answered with
     * an option that is not available. A question about a name that has just lost a name conflict needs none.
     */
    public List<String> unanswered(Map<String, String> answers) {
        Set<String> losers = losers(answers);
        List<String> missing = new ArrayList<>();
        for (Row row : rows) {
            if (!row.isQuestion()) continue;
            if (row.kind() != Kind.NAME_DIFFERS && losers.contains(row.deviceName())) continue;
            Option chosen = chosen(row, answers);
            if (chosen == null || !chosen.available()) missing.add(row.id());
        }
        return missing;
    }

    /**
     * The answers that take one installation's side wherever it has one. A question where it has none, or where
     * its side is not available, is left unanswered.
     */
    public Map<String, String> answersFavouring(long installId) {
        Map<String, String> answers = new LinkedHashMap<>();
        for (Row row : rows) {
            for (Option option : row.options()) {
                if (option.available() && option.installs().contains(installId)) {
                    answers.put(row.id(), option.key());
                    break;
                }
            }
        }
        return answers;
    }

    /**
     * The master the answers produce.
     *
     * @throws IllegalStateException if a question is still unanswered - see {@link #unanswered(Map)}
     */
    public Resolution resolve(Map<String, String> answers) {
        List<String> missing = unanswered(answers);
        if (!missing.isEmpty()) throw new IllegalStateException("first setup is not answered: " + missing);
        Set<String> losers = losers(answers);

        List<StoredDevice> devices = new ArrayList<>();
        for (String name : elementNames()) {
            if (losers.contains(name)) continue;
            HardwareId hardware = hardwareFor(name, answers);
            devices.add(new StoredDevice(null, name, hardware.vid(), hardware.pid(), true, null,
                    labelsFor(name, answers)));
        }
        for (String name : builtInNames()) {
            HardwareId hardware = firstBuiltInHardware(name);
            // WHY: confirmed, because a built-in has no name to confirm - its labels unlock straight away.
            devices.add(new StoredDevice(null, name, hardware.vid(), hardware.pid(), true, null,
                    labelsFor(name, answers)));
        }
        return new Resolution(devices, List.copyOf(new TreeSet<>(losers)));
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
            installations.forEach(install -> {
                if (hardware.equals(install.elements().get(name))) holding.add(install.installId());
            });
            present.addAll(holding);
            // WHY: giving up a name that .binds uses would orphan those bindings until the rename transaction
            // rewrites them, and that is not built - so the choice is shown but cannot be made.
            boolean losesNothing = names.stream()
                    .filter(other -> !other.equals(name))
                    .noneMatch(referenced::contains);
            options.add(new Option(name, name, holding, losesNothing));
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
                options.add(new Option(key(hardware), hardware.toString(), installs, true)));
        return new Row(HARDWARE_ROW + name, Kind.HARDWARE_DIFFERS, Severity.RED, name, byInstall.keySet(), options);
    }

    private Optional<Row> labelsRow(String name) {
        Map<Long, Map<String, String>> holders = labelHolders(name);
        if (collisions(holders.values()).isEmpty()) return Optional.empty();
        List<Option> options = new ArrayList<>();
        for (Installation install : installations) {
            if (!holders.containsKey(install.installId())) continue;
            options.add(new Option(String.valueOf(install.installId()), install.storefront(),
                    Set.of(install.installId()), true));
        }
        return Optional.of(new Row(LABELS_ROW + name, Kind.LABELS_DIFFER, Severity.YELLOW, name,
                holders.keySet(), options));
    }

    /**
     * The union of every installation's labels for the device. Where they collide, the answered installation's
     * label wins.
     */
    private Map<String, String> labelsFor(String name, Map<String, String> answers) {
        Map<Long, Map<String, String>> holders = labelHolders(name);
        Map<String, String> merged = new TreeMap<>();
        holders.values().forEach(labels -> labels.forEach(merged::putIfAbsent));
        String answer = answers.get(LABELS_ROW + name);
        if (answer != null) {
            Map<String, String> winner = holders.get(Long.valueOf(answer));
            if (winner != null) merged.putAll(winner);
        }
        return merged;
    }

    private Set<String> losers(Map<String, String> answers) {
        Set<String> losers = new LinkedHashSet<>();
        for (Row row : rows) {
            if (row.kind() != Kind.NAME_DIFFERS) continue;
            Option chosen = chosen(row, answers);
            if (chosen == null || !chosen.available()) continue;
            row.options().stream().map(Option::key).filter(name -> !name.equals(chosen.key())).forEach(losers::add);
        }
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

    private HardwareId firstBuiltInHardware(String name) {
        for (Installation install : installations) {
            HardwareId hardware = install.builtIns().get(name);
            if (hardware != null) return hardware;
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
        installations.forEach(install -> {
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

    private static Set<String> collisions(Iterable<Map<String, String>> labelSets) {
        Map<String, String> seen = new LinkedHashMap<>();
        Set<String> colliding = new TreeSet<>();
        for (Map<String, String> labels : labelSets) {
            labels.forEach((token, label) -> {
                String earlier = seen.putIfAbsent(token, label);
                if (earlier != null && !earlier.equals(label)) colliding.add(token);
            });
        }
        return colliding;
    }

    private Set<String> elementNames() {
        Set<String> names = new TreeSet<>();
        installations.forEach(install -> names.addAll(install.elements().keySet()));
        return names;
    }

    /** Built-ins carrying labels in at least one installation. */
    private Set<String> builtInNames() {
        Set<String> names = new TreeSet<>();
        installations.forEach(install -> install.builtIns().keySet().forEach(name -> {
            if (!labelHolders(name).isEmpty()) names.add(name);
        }));
        return names;
    }

    private static Map<HardwareId, Set<String>> conflictingNames(List<Installation> installations) {
        Map<HardwareId, Set<String>> byHardware = new LinkedHashMap<>();
        // WHY: an element with no VID/PID claims no hardware, so two of them are not one device under two names.
        installations.forEach(install -> install.elements().forEach((name, hardware) -> {
            if (hardware.vid().isEmpty() && hardware.pid().isEmpty()) return;
            byHardware.computeIfAbsent(hardware, h -> new TreeSet<>()).add(name);
        }));
        byHardware.values().removeIf(names -> names.size() < 2);
        return byHardware;
    }

    private static String key(HardwareId hardware) {
        return hardware.vid() + ":" + hardware.pid();
    }
}
