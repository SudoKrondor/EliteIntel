package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.Backup;
import elite.intel.bindforge.storage.BackupDestination;
import elite.intel.bindforge.storage.Backups;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Player Backups' <em>Taken</em> column and the refused-folder notice: each case gets its own string, and every string is
 * in every bundle.
 * <p>
 * WHY a test: nothing fails when a key is missing - {@code MultiLingualTextProvider} shows the key itself - and the
 * before-changes fallback, a zip whose {@code backup-info.txt} does not say what it came before, is the case most
 * likely to break unnoticed (review, 2026-10-24).
 */
class BindingManagementPanelTest {

    private static final Path ZIP = Path.of("backups", "2026-10-24_18-30-00.zip");

    @Test
    void eachKindAndWhatItCameBeforeHasItsOwnTakenLabel() {
        assertEquals("bindings.bindingManagement.taken.daily", takenKey(Backups.Kind.DAILY, null));
        assertEquals("bindings.bindingManagement.taken.user", takenKey(Backups.Kind.USER, null));
        assertEquals("bindings.bindingManagement.taken.firstSetup", takenKey(Backups.Kind.FIRST_SETUP, null));
        assertEquals("bindings.bindingManagement.taken.beforeApply",
                takenKey(Backups.Kind.BEFORE_CHANGES, Backups.Before.APPLY));
        assertEquals("bindings.bindingManagement.taken.beforeRestore",
                takenKey(Backups.Kind.BEFORE_CHANGES, Backups.Before.RESTORE));
        assertEquals("bindings.bindingManagement.taken.beforeCheck",
                takenKey(Backups.Kind.BEFORE_CHANGES, Backups.Before.CHECK));
        assertEquals("bindings.bindingManagement.taken.beforeChanges", takenKey(Backups.Kind.BEFORE_CHANGES, null),
                "a before-changes zip that does not say what it came before is still labelled");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "_de", "_es", "_fr", "_it", "_pt", "_ptbz"})
    void everyTakenLabelAndRefusalIsInEveryBundle(String suffix) {
        Properties props = bundle(suffix);
        List<String> keys = new ArrayList<>();
        for (Backups.Kind kind : Backups.Kind.values()) keys.add(takenKey(kind, null));
        for (Backups.Before before : Backups.Before.values()) keys.add(takenKey(Backups.Kind.BEFORE_CHANGES, before));
        for (BackupDestination.Refusal refusal : BackupDestination.Refusal.values()) {
            keys.add(BindingManagementPanel.refusalKey(refusal));
        }

        List<String> missing = keys.stream()
                .filter(key -> !props.containsKey(key) || props.getProperty(key).isBlank()).toList();

        assertEquals(List.of(), missing, "missing from gui" + suffix + ".properties");
    }

    @Test
    void eachRefusalHasItsOwnReason() {
        assertEquals(BackupDestination.Refusal.values().length, Arrays.stream(BackupDestination.Refusal.values())
                .map(BindingManagementPanel::refusalKey).distinct().count());
    }

    private static String takenKey(Backups.Kind kind, Backups.Before before) {
        return BindingManagementPanel.takenKey(
                new Backup(ZIP, LocalDateTime.parse("2026-10-24T18:30:00"), kind, before, true, List.of()));
    }

    private static Properties bundle(String suffix) {
        Properties props = new Properties();
        String path = "/i18n/gui" + suffix + ".properties";
        try (InputStream in = BindingManagementPanelTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing bundle " + path);
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new AssertionError("could not read " + path, e);
        }
        return props;
    }
}
