package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LmStudioDefaults;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStart;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadState;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStatus;
import elite.intel.db.managers.LmStudioInstallManager;
import elite.intel.db.managers.LmStudioInstallManager.Install;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which steps run, which are skipped, and what is recorded, against a {@link FakeLmStudioMachine} that changes nothing on
 * the PC.
 */
class LmStudioInstallerTest {

    private static final String MODEL = LmStudioDefaults.MODEL;

    @TempDir
    Path chosen;

    private final LmStudioInstallManager installs = LmStudioInstallManager.getInstance();
    private final RecordingProgress progress = new RecordingProgress();
    private final List<String> saved = new ArrayList<>();
    private final List<Path> stoppedOnExit = new ArrayList<>();
    private FakeLmStudioMachine machine;
    private Path home;

    // WHY: the suite shares one in-memory database, so a record left behind would leak into other tests.
    @BeforeEach
    void freshPc() {
        installs.forget();
        home = chosen.resolve("LMStudio");
        machine = new FakeLmStudioMachine(home);
    }

    @AfterEach
    void noRecordLeft() {
        installs.forget();
    }

    // -------------------------------------------------------------------------
    // A fresh install
    // -------------------------------------------------------------------------

    @Test
    void aFreshInstallRunsEveryStepInOrderAndEndsSetUp() {
        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertEquals(new InstallOutcome.Completed(), outcome);
        assertEquals(List.of("start CHECK", "done CHECK", "start INSTALL", "done INSTALL", "start START", "done START",
                "start DOWNLOAD", "done DOWNLOAD", "start LOAD", "done LOAD", "start GPU_CHECK", "done GPU_CHECK",
                "start TEST", "done TEST", "start SAVE", "done SAVE"), progress.steps);
        assertEquals(List.of(LmStudioDefaults.ADDRESS + " " + MODEL), saved);
    }

    @Test
    void aFreshInstallIsRecordedWithItsMarkerAndPointer() {
        installer(0).install(InstallFolder.inside(chosen));

        Install recorded = installs.find().orElseThrow();
        assertEquals(home, recorded.homeDir());
        assertTrue(recorded.pointerWritten());
        assertTrue(recorded.setupComplete());
        assertTrue(InstallMarker.matches(home, recorded.installId()));
        assertEquals(List.of(home), machine.pointers);
    }

    @Test
    void theDefaultFolderWritesNoPointer() {
        installer(0).install(new InstallFolder(home, false));

        assertFalse(installs.find().orElseThrow().pointerWritten());
        assertTrue(machine.pointers.isEmpty());
    }

    @Test
    void tooLittleSpaceStopsBeforeAnythingIsInstalled() {
        InstallOutcome outcome = installer(Long.MAX_VALUE).install(InstallFolder.inside(chosen));

        assertEquals(InstallStep.CHECK, ((InstallOutcome.Failed) outcome).step());
        assertTrue(installs.find().isEmpty());
        assertFalse(machine.calls.contains("script"));
    }

    @Test
    void aScriptThatLeavesNoLmsFailsTheInstallButKeepsTheRecord() {
        machine.scriptInstallsLms = false;

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertEquals(InstallStep.INSTALL, ((InstallOutcome.Failed) outcome).step());
        assertFalse(installs.find().orElseThrow().setupComplete(), "kept unfinished, so the button offers Continue");
    }

    @Test
    void aRecordWhoseMarkerNoLongerMatchesIsReplaced() {
        installs.recordStarted(chosen.resolve("gone"), false);

        assertEquals(new InstallOutcome.Completed(), installer(0).install(InstallFolder.inside(chosen)));
        assertEquals(home, installs.find().orElseThrow().homeDir());
    }

    @Test
    void theAppsOwnInstallIsStoppedAtExitEvenWhenItStopsPartWay() {
        machine.listedAfter = -1;

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertInstanceOf(InstallOutcome.Failed.class, outcome);
        assertEquals(List.of(lmsIn(home)), stoppedOnExit, "no service runs an unfinished install, so the exit stops it");
    }

    @Test
    void anInstallThatFailsBeforeLmStudioIsThereLeavesNothingToStop() {
        machine.scriptInstallsLms = false;

        installer(0).install(InstallFolder.inside(chosen));

        assertTrue(stoppedOnExit.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Continuing
    // -------------------------------------------------------------------------

    @Test
    void continuingSkipsWhatIsAlreadyDone() throws IOException {
        Install owned = installs.recordStarted(home, false);
        machine.installLms();
        machine.serverAnswering = true;
        machine.downloadStart = new DownloadStart.AlreadyDownloaded();

        InstallOutcome outcome = installer(0).continueSetup(owned);

        assertEquals(new InstallOutcome.Completed(), outcome);
        assertTrue(progress.steps.containsAll(List.of("skip CHECK", "skip INSTALL", "skip START", "skip DOWNLOAD")));
        assertFalse(machine.calls.contains("script"));
        assertTrue(installs.find().orElseThrow().setupComplete());
    }

    @Test
    void continuingWhereOnlyLmsIsLeftRunsTheScriptAgain() throws IOException {
        Install owned = installs.recordStarted(home, false);
        machine.leaveOnlyLms();

        assertEquals(new InstallOutcome.Completed(), installer(0).continueSetup(owned));
        assertTrue(machine.calls.contains("script"), "an lms with nothing to start is no install");
    }

    @Test
    void continuingStopsAtExitTheLmStudioAnEarlierRunLeftRunning() throws IOException {
        Install owned = installs.recordStarted(home, false);
        machine.installLms();
        machine.serverAnswering = true;

        installer(0).continueSetup(owned);

        assertEquals(List.of(lmsIn(home)), stoppedOnExit);
    }

    @Test
    void continuingWithoutLmsRunsTheScriptAgainAndRestoresThePointer() {
        Install owned = installs.recordStarted(home, true);

        assertEquals(new InstallOutcome.Completed(), installer(0).continueSetup(owned));
        assertTrue(machine.calls.contains("script"));
        assertEquals(List.of(home), machine.pointers);
    }

    // -------------------------------------------------------------------------
    // An LM Studio the app did not install
    // -------------------------------------------------------------------------

    @Test
    void settingUpAnExistingLmStudioRecordsNothingAndInstallsNothing() throws IOException {
        machine.installLms();
        machine.serverAnswering = true;

        InstallOutcome outcome = installer(0).setUpExisting(new LmStudioHome(home));

        assertEquals(new InstallOutcome.Completed(), outcome);
        assertTrue(installs.find().isEmpty(), "never started, stopped or uninstalled by the app");
        assertFalse(machine.calls.contains("script"));
        assertFalse(progress.steps.contains("start INSTALL"));
        assertTrue(progress.steps.contains("skip START"));
        assertEquals(List.of(LmStudioDefaults.ADDRESS + " " + MODEL), saved);
        assertTrue(stoppedOnExit.isEmpty(), "the commander's own LM Studio keeps running however they set it up");
    }

    @Test
    void anExistingLmStudioWhoseServerIsDownIsStartedOnce() throws IOException {
        machine.installLms();

        installer(0).setUpExisting(new LmStudioHome(home));

        assertEquals(1, machine.calls.stream().filter("daemon up"::equals).count());
        assertTrue(machine.calls.contains("server start"));
    }

    // -------------------------------------------------------------------------
    // The model
    // -------------------------------------------------------------------------

    @Test
    void theLoadWaitsUntilLmStudioListsTheDownloadedModel() {
        machine.listedAfter = 3;

        InstallOutcome outcome = installerWaitingForListing(0, Duration.ofMinutes(1)).install(InstallFolder.inside(chosen));

        assertEquals(new InstallOutcome.Completed(), outcome);
        assertEquals(4, machine.listingAsks);
        assertTrue(machine.calls.contains("load"));
    }

    @Test
    void aDownloadedModelLmStudioNeverListsFailsTheDownloadBeforeAnyLoad() {
        machine.listedAfter = -1;

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertEquals(InstallStep.DOWNLOAD, ((InstallOutcome.Failed) outcome).step());
        assertTrue(((InstallOutcome.Failed) outcome).reason().contains("did not list"));
        assertFalse(machine.calls.contains("load"));
    }

    @Test
    void aModelAlreadyDownloadedIsNotWaitedFor() {
        machine.downloadStart = new DownloadStart.AlreadyDownloaded();
        machine.listedAfter = -1;

        assertEquals(new InstallOutcome.Completed(), installer(0).install(InstallFolder.inside(chosen)));
        assertEquals(0, machine.listingAsks);
    }

    @Test
    void aModelLoadedWithAnotherContextIsReloadedWithElitesSettings() {
        machine.loaded = true;
        machine.loadedContext = 4096;

        installer(0).install(InstallFolder.inside(chosen));

        assertTrue(machine.calls.contains("unload"));
        assertTrue(machine.calls.indexOf("unload") < machine.calls.indexOf("load"));
    }

    @Test
    void aModelLoadedWithoutAReportedContextIsReloaded() {
        machine.loaded = true;
        machine.loadedContext = 0;

        installer(0).install(InstallFolder.inside(chosen));

        assertTrue(machine.calls.indexOf("unload") < machine.calls.indexOf("load"));
    }

    @Test
    void aModelLoadedWithVegasContextIsKeptAndTheGpuCheckSkipped() {
        // The Windows tester's Continue (2026-11-10): the reload cut off a commander turn that was using the model.
        machine.loaded = true;
        machine.loadedContext = LocalLmStudioMachine.CONTEXT_LENGTH;

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertEquals(new InstallOutcome.Completed(), outcome);
        assertFalse(machine.calls.contains("unload"), machine.calls.toString());
        assertFalse(machine.calls.contains("load"), machine.calls.toString());
        assertTrue(progress.steps.containsAll(List.of("skip LOAD", "skip GPU_CHECK")), progress.steps.toString());
        assertTrue(machine.calls.contains("test"), "the test still proves the kept model answers");
    }

    @Test
    void aModelThatDidNotGoOnTheGraphicsCardFailsTheGpuCheck() {
        machine.gpuReadings = new ArrayDeque<>(List.of(OptionalLong.of(9_000), OptionalLong.of(9_300)));

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertEquals(InstallStep.GPU_CHECK, ((InstallOutcome.Failed) outcome).step());
        assertTrue(saved.isEmpty(), "the fields are not filled for a model running on the CPU");
        assertFalse(installs.find().orElseThrow().setupComplete());
    }

    @Test
    void aReplyWithoutAToolCallFailsTheTest() {
        machine.toolCall = false;

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertEquals(InstallStep.TEST, ((InstallOutcome.Failed) outcome).step());
    }

    @Test
    void aModelThatIsOnlySlowToAnswerIsAskedAgain() {
        // The Windows tester's case (2026-11-10): the first test got no answer in 2 minutes, the next one passed.
        machine.slowTestAnswers = 1;

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertEquals(new InstallOutcome.Completed(), outcome);
        assertEquals(2, machine.calls.stream().filter("test"::equals).count());
        assertTrue(progress.lines.stream().anyMatch(line -> line.contains("Asking again (2 of 4)")), progress.lines.toString());
    }

    @Test
    void aModelThatNeverAnswersFailsTheTestAfterEveryTry() {
        machine.slowTestAnswers = LmStudioInstaller.TEST_ATTEMPTS;

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertEquals(InstallStep.TEST, ((InstallOutcome.Failed) outcome).step());
        assertEquals(LmStudioInstaller.TEST_ATTEMPTS, machine.calls.stream().filter("test"::equals).count());
        assertTrue(saved.isEmpty());
    }

    @Test
    void aReplyWithoutAToolCallIsNotAskedAgain() {
        machine.toolCall = false;

        installer(0).install(InstallFolder.inside(chosen));

        assertEquals(1, machine.calls.stream().filter("test"::equals).count());
    }

    @Test
    void aFailedDownloadSaysHowFarItGotAndLogsLmStudiosReply() {
        // The Windows tester's first try (2026-11-10) said only that the download failed.
        String reply = "{\"status\": \"failed\", \"downloaded_bytes\": 1500, \"total_size_bytes\": 6000}";
        long gb = 1024L * 1024 * 1024;
        machine.statuses = new ArrayDeque<>(List.of(
                new DownloadStatus(DownloadState.FAILED, (long) (0.7 * gb), (long) (5.9 * gb), 0, Optional.empty(), reply)));

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        InstallOutcome.Failed failed = assertInstanceOf(InstallOutcome.Failed.class, outcome);
        assertEquals(InstallStep.DOWNLOAD, failed.step());
        assertTrue(failed.reason().endsWith("failed at " + String.format("%.1f of %.1f GB", 0.7, 5.9)), failed.reason());
        assertTrue(progress.lines.contains("LM Studio's download status: " + reply), progress.lines.toString());
    }

    @Test
    void theDownloadReportsItsProgressUntilItCompletes() {
        machine.statuses = new ArrayDeque<>(List.of(
                status(DownloadState.DOWNLOADING, 1_000), status(DownloadState.DOWNLOADING, 3_000),
                status(DownloadState.COMPLETED, 6_000)));

        installer(0).install(InstallFolder.inside(chosen));

        assertEquals(List.of(1_000L, 3_000L, 6_000L), progress.downloaded);
    }

    @Test
    void aDownloadThatFailsEveryAttemptFailsTheDownloadStep() {
        machine.statuses = new ArrayDeque<>(List.of(status(DownloadState.FAILED, 1_000)));

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertEquals(InstallStep.DOWNLOAD, ((InstallOutcome.Failed) outcome).step());
        assertEquals(LmStudioInstaller.DOWNLOAD_ATTEMPTS, machine.calls.stream().filter("download"::equals).count());
    }

    @Test
    void aDownloadThatFailsOnceIsStartedAgainAndTheInstallGoesOn() {
        // The Windows tester's first download after each fresh install failed a minute in; the next one went through.
        machine.statuses = new ArrayDeque<>(List.of(status(DownloadState.FAILED, 1_000),
                status(DownloadState.DOWNLOADING, 3_000), status(DownloadState.COMPLETED, 6_000)));

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertInstanceOf(InstallOutcome.Completed.class, outcome);
        assertEquals(2, machine.calls.stream().filter("download"::equals).count());
        assertTrue(progress.lines.stream().anyMatch(line -> line.endsWith("Starting it again.")), progress.lines.toString());
    }

    @Test
    void aRetriedDownloadLmStudioCallsDoneAlreadyGoesOnToTheLoad() {
        machine.statuses = new ArrayDeque<>(List.of(status(DownloadState.FAILED, 1_000)));
        machine.downloadStartsAfterFirst = new DownloadStart.AlreadyDownloaded();

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertInstanceOf(InstallOutcome.Completed.class, outcome);
    }

    @Test
    void aDownloadThatStopsMovingIsTreatedAsStuck() {
        machine.statuses = new ArrayDeque<>(List.of(status(DownloadState.DOWNLOADING, 1_000)));

        InstallOutcome outcome = installer(0).install(InstallFolder.inside(chosen));

        assertEquals(InstallStep.DOWNLOAD, ((InstallOutcome.Failed) outcome).step());
        assertTrue(((InstallOutcome.Failed) outcome).reason().contains("no progress"));
    }

    @Test
    void cancellingTheAppsOwnDownloadPausesItByStoppingTheDaemon() {
        LmStudioInstaller installer = new LmStudioInstaller(machine, progress, this::save, installs, 0,
                Duration.ofMillis(1), Duration.ofMinutes(5), Duration.ZERO, stoppedOnExit::add);
        machine.statuses = new ArrayDeque<>(List.of(status(DownloadState.DOWNLOADING, 1_000)));
        machine.onStatus = installer::cancel;

        InstallOutcome outcome = installer.install(InstallFolder.inside(chosen));

        assertEquals(new InstallOutcome.Cancelled(), outcome);
        assertEquals("daemon down", machine.calls.getLast());
        assertFalse(machine.cancelled, "the daemon must still be stoppable after the cancel");
        assertFalse(installs.find().orElseThrow().setupComplete());
    }

    // -------------------------------------------------------------------------

    private LmStudioInstaller installer(long requiredBytes) {
        return installerWaitingForListing(requiredBytes, Duration.ZERO);
    }

    private LmStudioInstaller installerWaitingForListing(long requiredBytes, Duration listingWait) {
        return new LmStudioInstaller(machine, progress, this::save, installs, requiredBytes, Duration.ZERO,
                Duration.ZERO, listingWait, stoppedOnExit::add);
    }

    private static Path lmsIn(Path home) {
        return new LmStudioHome(home).lmsCommand().orElseThrow();
    }

    private void save(String address, String model) {
        saved.add(address + " " + model);
    }

    private static DownloadStatus status(DownloadState state, long downloaded) {
        return new DownloadStatus(state, downloaded, 6_000, 0, Optional.empty());
    }

    private static final class RecordingProgress implements InstallProgress {
        final List<String> steps = new ArrayList<>();
        final List<Long> downloaded = new ArrayList<>();
        final List<String> lines = new ArrayList<>();

        @Override
        public void stepStarted(InstallStep step) {
            steps.add("start " + step);
        }

        @Override
        public void stepSkipped(InstallStep step) {
            steps.add("skip " + step);
        }

        @Override
        public void stepFinished(InstallStep step) {
            steps.add("done " + step);
        }

        @Override
        public void log(String line) {
            lines.add(line);
        }

        @Override
        public void download(long downloadedBytes, long totalBytes, double bytesPerSecond) {
            downloaded.add(downloadedBytes);
        }
    }
}
