package elite.intel.bindforge.presets;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** The list the four dropdowns offer: the player's presets, the shipped ones, and the files that are neither. */
class PresetCatalogTest {

    @TempDir
    Path root;

    private final PresetCatalog catalog = new PresetCatalog();

    @Test
    void thePlayersPresetsAreOfferedAndTheRestAreNamed() throws IOException {
        Path bindings = folder("Bindings");
        copySpecimen(bindings, "Custom.4.2.binds");
        copySpecimen(bindings, "DualVirpilDawnTreader.4.1.binds");
        copySpecimen(bindings, "Custom.4.2 (2).binds");
        copySpecimen(bindings, "20250624_003828_DualVirpilDawnTreader.4.2.binds.backup");
        write(bindings, "StartPreset.binds", "KeyboardMouseOnly\r\nCustom\r\nCustom\r\nCustom");
        write(bindings, "StartPreset.4.start", "Custom\r\nCustom\r\nCustom\r\nCustom");

        PresetList list = catalog.list(bindings, List.of());

        assertEquals(List.of(
                        new PresetEntry("Custom", PresetOrigin.USER, List.of(bindings.resolve("Custom.4.2.binds"))),
                        new PresetEntry("DualVirpilDawnTreader", PresetOrigin.USER,
                                List.of(bindings.resolve("DualVirpilDawnTreader.4.1.binds")))),
                list.presets());
        // The backup is not a .binds name at all, so it is neither offered nor explained.
        assertEquals(List.of(
                        new NotOfferedFile(bindings.resolve("Custom.4.2 (2).binds"), PresetOrigin.USER,
                                NotOfferedReason.NAME_MISMATCH, "Custom"),
                        new NotOfferedFile(bindings.resolve("StartPreset.binds"), PresetOrigin.USER,
                                NotOfferedReason.START_PRESET_LIST, "KeyboardMouseOnly\nCustom\nCustom\nCustom")),
                list.notOffered());
    }

    @Test
    void theShippedPresetsAreOfferedOnceWithAFileFromEachInstallation() throws IOException {
        Path bindings = folder("Bindings");
        copySpecimen(bindings, "Custom.4.2.binds");
        Path steam = folder("Steam/ControlSchemes");
        Path epic = folder("Epic/ControlSchemes");
        shipped(steam, "KeyboardMouseOnly");
        shipped(epic, "KeyboardMouseOnly");
        shipped(epic, "SaitekX56");

        PresetList list = catalog.list(bindings, List.of(steam, epic));

        assertEquals(List.of(
                        new PresetEntry("Custom", PresetOrigin.USER, List.of(bindings.resolve("Custom.4.2.binds"))),
                        new PresetEntry("KeyboardMouseOnly", PresetOrigin.FACTORY,
                                List.of(steam.resolve("KeyboardMouseOnly.binds"), epic.resolve("KeyboardMouseOnly.binds"))),
                        new PresetEntry("SaitekX56", PresetOrigin.FACTORY, List.of(epic.resolve("SaitekX56.binds")))),
                list.presets());
    }

    /** The specimen StartPreset.4.start: General on a factory preset, the rest on the player's. */
    @Test
    void everyLineOfTheSplitSpecimenFindsItsPreset() throws IOException {
        Path bindings = folder("Bindings");
        copySpecimen(bindings, "Custom.4.2.binds");
        Path controlSchemes = folder("Steam/ControlSchemes");
        shipped(controlSchemes, "KeyboardMouseOnly");

        PresetList list = catalog.list(bindings, List.of(controlSchemes));

        assertEquals(PresetOrigin.FACTORY, list.find("KeyboardMouseOnly").orElseThrow().origin());
        assertEquals(PresetOrigin.USER, list.find("Custom").orElseThrow().origin());
        assertEquals(Optional.empty(), list.find("custom"), "a near miss is not a match");
    }

    @Test
    void aPlayersPresetShadowsAShippedOneOfTheSameName() throws IOException {
        Path bindings = folder("Bindings");
        write(bindings, "KeyboardMouseOnly.4.2.binds", binds("KeyboardMouseOnly"));
        Path controlSchemes = folder("Steam/ControlSchemes");
        shipped(controlSchemes, "KeyboardMouseOnly");

        PresetList list = catalog.list(bindings, List.of(controlSchemes));

        assertEquals(List.of(new PresetEntry("KeyboardMouseOnly", PresetOrigin.USER,
                List.of(bindings.resolve("KeyboardMouseOnly.4.2.binds")))), list.presets());
    }

    /** Cannot happen in a live folder - the game upgrades the old one in place - but is not silently halved if it does. */
    @Test
    void twoOfThePlayersFilesForOneNameAreBothKept() throws IOException {
        Path bindings = folder("Bindings");
        copySpecimen(bindings, "Custom.3.0.binds");
        copySpecimen(bindings, "Custom.4.2.binds");

        PresetList list = catalog.list(bindings, List.of());

        assertEquals(List.of(new PresetEntry("Custom", PresetOrigin.USER,
                        List.of(bindings.resolve("Custom.3.0.binds"), bindings.resolve("Custom.4.2.binds")))),
                list.presets());
    }

    @Test
    void anInstallationThatCannotBeListedIsSkipped() throws IOException {
        Path bindings = folder("Bindings");
        copySpecimen(bindings, "Custom.4.2.binds");
        Path unplugged = root.resolve("Unplugged/ControlSchemes");

        PresetList list = catalog.list(bindings, List.of(unplugged));

        assertEquals(List.of("Custom"), list.presets().stream().map(PresetEntry::name).toList());
        assertEquals(List.of(), list.notOffered());
    }

    @Test
    void aShippedFileThatIsNotAPresetIsNamedAsFactory() throws IOException {
        Path bindings = folder("Bindings");
        Path controlSchemes = folder("Steam/ControlSchemes");
        write(controlSchemes, "Broken.binds", "");

        PresetList list = catalog.list(bindings, List.of(controlSchemes));

        assertEquals(List.of(new NotOfferedFile(controlSchemes.resolve("Broken.binds"), PresetOrigin.FACTORY,
                NotOfferedReason.EMPTY, "")), list.notOffered());
    }

    @Test
    void aBindingsFolderThatCannotBeListedFails() {
        assertThrows(IOException.class, () -> catalog.list(root.resolve("NoSuchFolder"), List.of()));
    }

    @Test
    void controlSchemesComeOnlyFromInstallationsThatAreThere() {
        InstallationRow present = new InstallationRow(1, "STEAM", root.resolve("Steam").toString(), false, false);
        InstallationRow missing = new InstallationRow(2, "EPIC", root.resolve("Epic").toString(), false, true);

        assertEquals(List.of(root.resolve("Steam/Products/elite-dangerous-odyssey-64/ControlSchemes")),
                PresetCatalog.controlSchemesOf(List.of(present, missing)));
    }

    private Path folder(String relative) throws IOException {
        return Files.createDirectories(root.resolve(relative));
    }

    private static void copySpecimen(Path folder, String name) throws IOException {
        Files.copy(Specimens.bindsSpecimen(name), folder.resolve(name));
    }

    private static void shipped(Path controlSchemes, String name) throws IOException {
        write(controlSchemes, name + ".binds", binds(name).replace(" MajorVersion=\"4\" MinorVersion=\"2\"", " SortOrder=\"0\""));
    }

    private static String binds(String presetName) {
        return "<Root PresetName=\"" + presetName + "\" MajorVersion=\"4\" MinorVersion=\"2\">"
                + "<YawLeftButton><Primary Device=\"Keyboard\" Key=\"Key_A\" /></YawLeftButton></Root>";
    }

    private static void write(Path folder, String name, String content) throws IOException {
        Files.writeString(folder.resolve(name), content, StandardCharsets.UTF_8);
    }
}
