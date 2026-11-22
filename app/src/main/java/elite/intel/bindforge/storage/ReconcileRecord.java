package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * What a reconcile read and what the player chose, written to BindForge's log ({@code bindforge.log}, which the support
 * bundle carries) - first-time startup's, the check's and RESTORE's alike.
 * <p>
 * WHY: "my binds are all wrong" after a reconcile cannot be answered from the result alone. The working copy shows what
 * was made, not which installation held what before, which questions were put, or which side each answer took - and
 * the player who answered them, often over files edited by hand before BindForge, cannot say either. The before-setup
 * backup in the bundle holds the files; this holds the decisions. Names, VID/PIDs, counts and labels only - no file
 * content. Pure apart from the log.
 */
public final class ReconcileRecord {

    private static final Logger log = LogManager.getLogger(ReconcileRecord.class);

    private ReconcileRecord() {
    }

    /** Logs what a reconcile read, before anything is asked - kept even when the player abandons it. */
    public static void logReading(String purpose, FirstSetupReading reading) {
        try {
            log.info("{}", describeReading(purpose, reading));
        } catch (RuntimeException e) {
            // WHY: broad on purpose - a record for a support bundle must never cost the player the reconcile.
            log.warn("Could not record what the reconcile for {} read", purpose, e);
        }
    }

    /** Logs the player's answers and the working copy they made, at the reconcile's last button. */
    public static void logDecision(String purpose, FirstSetupReading reading, Map<String, String> answers,
                                   Long favoured, FirstSetupFiles.Merged merged) {
        try {
            log.info("{}", describeDecision(purpose, reading, answers, favoured, merged));
        } catch (RuntimeException e) {
            // WHY: broad on purpose - the working copy they settled on is made; its record must not undo that.
            log.warn("Could not record the reconcile for {}'s answers", purpose, e);
        }
    }

    static String describeReading(String purpose, FirstSetupReading reading) {
        StringBuilder text = new StringBuilder("Reconcile for ").append(purpose).append(" read ")
                .append(reading.installations().size()).append(" installation(s) and ")
                .append(reading.bindings().size()).append(" binds and preset file(s) from ")
                .append(reading.bindingsFolder())
                .append(reading.identical() ? "; the installations' device files are identical" : "");
        for (InstallationRow row : reading.unreachable()) {
            line(text, "not reachable: " + row.storefront() + " #" + row.id() + " (" + row.rootPath() + ")");
        }
        for (InstallationFiles files : reading.installations()) {
            FirstSetupPlan.Installation plan = planOf(reading, files.installId());
            line(text, name(files.installId(), reading) + " (" + files.root() + "): "
                    + (files.hasDeviceMappings() ? "DeviceMappings.xml" : "no DeviceMappings.xml") + ", "
                    + files.buttonMaps().size() + " button maps " + new TreeMap<>(files.buttonMaps()).keySet());
            if (plan == null) continue;
            line(text, "  elements, in file order: " + hardware(plan.elements()));
            if (!plan.builtIns().isEmpty()) line(text, "  built-ins with labels: " + hardware(plan.builtIns()));
            plan.labels().forEach((device, labels) -> line(text, "  labels on " + device + ": " + labels.size()));
        }
        reading.references().forEach((file, counts) -> {
            if (!counts.isEmpty()) line(text, "bindings in " + file + " name: " + new TreeMap<>(counts));
        });
        for (FirstSetupPlan.Row row : reading.plan().rows()) {
            line(text, row.kind() + " " + row.severity() + " " + row.deviceName() + " in "
                    + names(row.presentIn(), reading)
                    + (row.isQuestion() ? ", asked: " + row.options().stream()
                    .map(option -> option.text() + " " + names(option.installs(), reading))
                    .collect(Collectors.joining(" | ")) : ""));
        }
        return text.toString();
    }

    static String describeDecision(String purpose, FirstSetupReading reading, Map<String, String> answers,
                                   Long favoured, FirstSetupFiles.Merged merged) {
        StringBuilder text = new StringBuilder("Reconcile for ").append(purpose).append(" finished");
        text.append(favoured == null ? "; answered question by question"
                : "; made everything like " + name(favoured, reading) + ", nothing changed after");
        FirstSetupPlan plan = reading.plan();
        for (FirstSetupPlan.Row row : plan.rows()) {
            if (!row.isQuestion()) continue;
            if (row.kind() == FirstSetupPlan.Kind.LABELS_DIFFER) {
                line(text, "labels on " + row.deviceName() + ": " + plan.labelAnswers(row.deviceName(), answers));
                continue;
            }
            String key = answers.get(row.id());
            String chosen = row.options().stream().filter(option -> option.key().equals(key)).findFirst()
                    .map(option -> option.text() + " " + names(option.installs(), reading))
                    .orElse("not answered - its name lost a name question");
            line(text, row.kind() + " " + row.deviceName() + ": " + chosen);
        }
        FirstSetupPlan.Resolution resolution = plan.resolve(answers);
        for (FirstSetupPlan.Kept kept : resolution.devices()) {
            line(text, "kept " + kept.name() + " [" + hardware(kept.hardware()) + "]"
                    + (kept.builtIn() ? " built-in" : " from " + name(kept.source(), reading))
                    + ", " + kept.labels().size() + " labels");
        }
        merged.moves().forEach((loser, winner) -> line(text, "moved " + loser + " -> " + winner));
        line(text, merged.bindingsMoved() + " bindings moved; the working copy holds "
                + merged.files().bindings().size() + " binds and preset files, "
                + (merged.files().hasDeviceMappings() ? "a DeviceMappings.xml" : "no DeviceMappings.xml") + ", "
                + merged.files().buttonMaps().size() + " button maps");
        return text.toString();
    }

    private static void line(StringBuilder text, String content) {
        text.append("\n  ").append(content);
    }

    private static FirstSetupPlan.Installation planOf(FirstSetupReading reading, long installId) {
        return reading.plan().installations().stream().filter(one -> one.installId() == installId).findFirst()
                .orElse(null);
    }

    private static String name(long installId, FirstSetupReading reading) {
        return reading.installations().stream().filter(files -> files.installId() == installId).findFirst()
                .map(files -> files.storefront() + " #" + installId)
                .orElse("#" + installId);
    }

    private static String names(Collection<Long> installIds, FirstSetupReading reading) {
        return installIds.stream().sorted().map(id -> name(id, reading)).collect(Collectors.joining(", ", "[", "]"));
    }

    private static String hardware(Map<String, HardwareId> devices) {
        return devices.entrySet().stream().map(entry -> entry.getKey() + " [" + hardware(entry.getValue()) + "]")
                .collect(Collectors.joining(", "));
    }

    private static String hardware(HardwareId id) {
        return id.vid() + ":" + id.pid();
    }
}
