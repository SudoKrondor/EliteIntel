package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.PresetEdits;
import elite.intel.bindforge.storage.PresetRefusedException;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * What APPLY says when it refuses the preset file (preset-editor.md, <em>What APPLY checks and writes</em>). With an
 * installation that could not be reached, that is named rather than blaming the preset file alone (Storage S6R,
 * 2026-10-29). Compared through the bundles, so it holds in any language.
 */
class PresetRefusedTextTest {

    private static final InstallationRow EPIC = new InstallationRow(2, "EPIC", "E:\\Epic", false, true);

    @Test
    void withEveryInstallationReachableThePresetFileIsNamed() {
        assertEquals(getText("bindings.applyBar.presetRefused", getText(BindingSection.GENERAL.getLabelKey())),
                ApplyBar.presetRefusedText(refused()));
    }

    @Test
    void anInstallationThatCouldNotBeReachedIsNamed() {
        String text = ApplyBar.presetRefusedText(refused().withUnreachable(List.of(EPIC)));

        assertEquals(getText("bindings.applyBar.presetUnreachable",
                getText("bindings.firstTimeStartup.place.installation", "EPIC", "E:\\Epic"),
                getText(BindingSection.GENERAL.getLabelKey())), text);
    }

    /** A refusal of the General line, which names a factory binds file no installation offered. */
    private static PresetRefusedException refused() {
        ConfigurationFiles files = new ConfigurationFiles(Map.of("StartPreset.4.start",
                "KeyboardMouseOnly\r\nCustom\r\nCustom\r\nCustom".getBytes(StandardCharsets.UTF_8)), null, Map.of());
        BindNameList customOnly = new BindNameList(List.of(
                new BindNameEntry("Custom", BindsFileOrigin.USER, List.of(Path.of("Custom.4.2.binds")))), List.of());
        return assertThrows(PresetRefusedException.class, () -> PresetEdits.check(files, customOnly));
    }
}
