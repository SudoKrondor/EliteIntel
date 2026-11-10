package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LMStudioClient;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient.ModelListing;
import elite.intel.db.managers.LmStudioInstallManager;
import elite.intel.db.managers.LmStudioInstallManager.Install;
import elite.intel.session.SystemSession;
import elite.intel.util.NvidiaSmi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * What LM Studio install assistance adds to a support bundle: how it stands on this PC right now. What it did is in
 * the LM Studio log, which the bundle carries beside this.
 * <p>
 * WHY: the questions a report about the setup button starts with - why there is no button, why it offers what it
 * offers, which LM Studio it found - each turn on a state no log line states: what the graphics drivers say about the
 * card against the bar, where LM Studio's home pointer leads, whether the install record's marker still matches.
 * <p>
 * Worked out only when the commander saves a bundle. <strong>One section that fails costs that section</strong>, with
 * what stopped it written in its place. Reads only. Reads the graphics cards, asks LM Studio for its models and reads
 * the database - not for the EDT.
 */
public final class LmStudioSupport {

    /**
     * The report's name in the bundle.
     */
    public static final String REPORT_ENTRY = "lm-studio-report.txt";

    private static final double BYTES_PER_GB = 1024.0 * 1024 * 1024;

    private LmStudioSupport() {
    }

    public static String render() {
        StringBuilder text = new StringBuilder("LM Studio install assistance\n");
        section(text, "Graphics card", LmStudioSupport::graphicsCard);
        section(text, "LM Studio on this PC", LmStudioSupport::thisPc);
        section(text, "The app's own install", LmStudioSupport::ownInstall);
        section(text, "The setup button", LmStudioSupport::setupButton);
        return text.toString();
    }

    private static void section(StringBuilder text, String title, Supplier<String> body) {
        text.append('\n').append(title).append(":\n");
        try {
            text.append(body.get());
        } catch (RuntimeException | LinkageError e) {
            // WHY broad: this runs when something is already wrong, and the fault being reported may be what breaks a
            // section - the rest of the report must still be written.
            text.append("  could not be read: ").append(e).append('\n');
        }
    }

    // -------------------------------------------------------------------------
    // Graphics card
    // -------------------------------------------------------------------------

    private static String graphicsCard() {
        // WHY the same last two fields as GpuRequirement's query: each line is judged by its own reading of them.
        NvidiaSmi.Answer answer = NvidiaSmi.query(
                "--query-gpu=driver_version,name,memory.total,compute_cap", "--format=csv,noheader,nounits");
        return graphicsCard(answer, AmdCards.onThisPc(), GpuMemory.usedMib(), GpuRequirement.forThisPc());
    }

    /**
     * @param usedMib    graphics memory in use now, as the install's GPU check would read it
     * @param decided    what the app decided the first time it asked this run, which is what the button follows
     */
    static String graphicsCard(NvidiaSmi.Answer answer, List<AmdCards.Card> amd, OptionalLong usedMib,
                               GpuRequirement.Verdict decided) {
        StringBuilder text = new StringBuilder(String.format(Locale.ROOT,
                "  The bar: an NVIDIA card with %d MiB and compute capability %d.%d, or (beta) an AMD card with %d MiB%n",
                GpuRequirement.MIN_MEMORY_MIB, GpuRequirement.MIN_COMPUTE_MAJOR, GpuRequirement.MIN_COMPUTE_MINOR,
                GpuRequirement.MIN_MEMORY_MIB));
        switch (answer) {
            case NvidiaSmi.Answer.Lines(List<String> lines) -> {
                for (String line : lines) {
                    text.append("  Card (driver, name, MiB, compute): ").append(line)
                            .append(GpuRequirement.meetsTheBar(line) ? " - meets the bar" : " - below the bar")
                            .append('\n');
                }
            }
            case NvidiaSmi.Answer.Nothing() -> text.append(
                    "  nvidia-smi: nothing - not on the PATH, no NVIDIA driver, or it failed\n");
            case NvidiaSmi.Answer.NoAnswer() -> text.append("  nvidia-smi: did not answer within ")
                    .append(NvidiaSmi.TIMEOUT.toSeconds()).append(" s\n");
            case NvidiaSmi.Answer.Unreadable(IOException cause) -> text.append(
                    "  nvidia-smi: its output could not be read: ").append(cause).append('\n');
        }
        if (amd.isEmpty()) text.append("  AMD: none found\n");
        for (AmdCards.Card card : amd) {
            text.append("  AMD card: ").append(card.name()).append(", ")
                    .append(card.memoryMib().isPresent() ? card.memoryMib().getAsLong() + " MiB" : "memory not reported")
                    .append(GpuRequirement.meetsTheBar(card) ? " - meets the bar" : " - below the bar")
                    .append('\n');
        }
        text.append("  Graphics memory in use now: ")
                .append(usedMib.isPresent() ? usedMib.getAsLong() + " MiB" : "could not be read").append('\n');
        return text.append("  Decided this run, which the button follows: ").append(switch (decided) {
            case NOT_MET -> "does not meet the bar";
            case MET_BY_NVIDIA -> "meets the bar (NVIDIA)";
            case MET_BY_AMD -> "meets the bar (AMD, beta)";
        }).append('\n').toString();
    }

    // -------------------------------------------------------------------------
    // LM Studio on this PC
    // -------------------------------------------------------------------------

    private static String thisPc() {
        Path userHome = Path.of(System.getProperty("user.home"));
        String address = SystemSession.getInstance().getLmStudioAddress();
        LmStudioHome home = LmStudioHome.locate();
        return "  Platform: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + '\n'
                + "  Saved Address names this PC: " + yesNo(LmStudioSetupOffer.isOnThisPc(address))
                + " (the Address is in bundle-info.txt)\n"
                + "  Home pointer ~/" + LmStudioHome.POINTER_FILE + ": " + pointer(userHome) + '\n'
                + "  LM Studio's home: " + home.dir() + '\n'
                + "  lms: " + lms(home) + '\n'
                + "  Server at the saved Address: " + listing(LMStudioClient.getInstance().listModels()) + '\n';
    }

    private static String pointer(Path userHome) {
        Path pointer = userHome.resolve(LmStudioHome.POINTER_FILE);
        if (!Files.isRegularFile(pointer)) return "none";
        try {
            return "names " + Files.readString(pointer, StandardCharsets.UTF_8).strip();
        } catch (IOException unreadable) {
            return "unreadable: " + unreadable;
        }
    }

    private static String lms(LmStudioHome home) {
        return home.lmsCommand().map(Path::toString).orElse("none in " + home.dir().resolve("bin"));
    }

    static String listing(ModelListing listing) {
        return switch (listing) {
            case ModelListing.Listed(var ids) -> ids.isEmpty() ? "answering, serves no models"
                    : "answering, serves " + String.join(", ", ids.stream().sorted().toList());
            case ModelListing.NotAnswering() -> "not answering";
            case ModelListing.Unknown() -> "answered, but what it serves could not be learned"
                    + " (or the Address is not the usual .../v1/chat/completions)";
        };
    }

    // -------------------------------------------------------------------------
    // The app's own install
    // -------------------------------------------------------------------------

    private static String ownInstall() {
        Optional<Install> recorded = LmStudioInstallManager.getInstance().find();
        if (recorded.isEmpty()) return "  none recorded\n";
        Install install = recorded.get();
        StringBuilder text = new StringBuilder()
                .append("  Home: ").append(install.homeDir()).append('\n')
                .append("  Installed at: ").append(install.installedAt()).append('\n')
                .append("  Setup complete: ").append(yesNo(install.setupComplete())).append('\n')
                .append("  Wrote the home pointer: ").append(yesNo(install.pointerWritten())).append('\n')
                .append("  Marker: ").append(marker(install.homeDir(), install.installId())).append('\n')
                .append("  lms: ").append(lms(new LmStudioHome(install.homeDir()))).append('\n');
        List<LmStudioUninstaller.ModelFile> models = LmStudioUninstaller.modelsIn(install);
        if (models.isEmpty()) text.append("  Models: none\n");
        for (LmStudioUninstaller.ModelFile model : models) {
            text.append(String.format(Locale.ROOT, "  Model: %s, %.1f GB%n", model.name(), model.bytes() / BYTES_PER_GB));
        }
        return text.toString();
    }

    /**
     * Whether the install's home still holds its {@link InstallMarker}, and if not, what it holds instead. A record
     * whose marker does not match is ignored by the setup button.
     */
    static String marker(Path homeDir, UUID installId) {
        try {
            Path marker = homeDir.resolve(InstallMarker.FILE);
            if (!Files.isDirectory(homeDir)) return "the home folder is not there";
            if (!Files.isRegularFile(marker)) return "missing, so this record is ignored";
            String held = Files.readString(marker, StandardCharsets.UTF_8).strip();
            return held.equals(installId.toString()) ? "matches" : "holds another install's id, so this record is ignored";
        } catch (IOException | InvalidPathException unreadable) {
            return "unreadable: " + unreadable;
        }
    }

    // -------------------------------------------------------------------------
    // The setup button
    // -------------------------------------------------------------------------

    private static String setupButton() {
        return "  Offers: " + LmStudioSetupOffer.forThisPc() + '\n'
                + "  The app runs its own LM Studio with the services: " + yesNo(LmStudioHost.isWanted()) + '\n';
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
