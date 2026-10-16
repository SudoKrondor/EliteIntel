package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.devicefiles.*;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DivergenceWaysOut.WayOut;
import elite.intel.bindforge.devices.DeviceIdentities;
import elite.intel.bindforge.devices.HotPlugOffers;
import elite.intel.bindforge.devices.IdentityQuestions;
import elite.intel.bindforge.devices.MissingControllerWarning;
import elite.intel.bindforge.devices.MyDevice;
import elite.intel.bindforge.devices.Onboarding;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeActivationManager;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeIdentityAnswerManager;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.eventbus.DeviceBus;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.DeviceFilesChangedEvent;
import elite.intel.ui.screen.bindings.AliasDesignerReader.OnboardingReading;
import elite.intel.ui.screen.bindings.AliasDesignerReader.Snapshot;
import elite.intel.util.OsDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.Dialog;
import java.awt.Window;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.util.StringUtls.messageOf;

/**
 * The Alias Designer screen's flows: the refresh, activation, first setup, APPLY and DISCARD, the ways out, catch-up,
 * the identity question (A12), onboarding, controllers plugged in or out (A7), and opening the Device Editor. It decides when each runs, runs the disk work off the EDT, asks and tells
 * the user, and hands what it read to {@link AliasDesignerPanel}, which only draws.
 * <p>
 * State is kept on the EDT, but for the three flags background threads touch, which are atomic.
 */
final class AliasDesignerController {

    private static final Logger log = LogManager.getLogger(AliasDesignerController.class);

    /**
     * What the screen shows before its first refresh has finished.
     */
    private static final Snapshot NOTHING_YET = new Snapshot(Map.of(), Map.of(), null, List.of(), List.of(),
            List.of(), AliasDesignerReader.Markers.NONE, false, List.of(), false, false, false, List.of(),
            IdentityQuestions.Reading.NONE);

    private final AliasDesignerPanel view;
    private final AliasDesignerReader reader;
    private final InstallationRegistry registry;

    private final RefreshCoalescer refreshes = new RefreshCoalescer();
    /**
     * Set when the screen is shown while this run's missing-controller warning is still to come, and taken by the next
     * refresh to start. A refresh already running when the screen appeared may have read the hardware before the
     * Device Service had listed it, so it is never the one that judges.
     */
    private final AtomicBoolean missingWarningWanted = new AtomicBoolean();
    /**
     * Activation is tried once per run - abandoned first setup is then SET UP's, and the next run's to offer again.
     */
    private final AtomicBoolean activationTried = new AtomicBoolean();
    /**
     * Set when activation is tried and on CHECK AGAIN, and taken by the first refresh after it that finds BindForge
     * activated and the hardware listed: onboarding opens then, for the controllers attached (Alan, 2026-10-14, A6b).
     */
    private final AtomicBoolean onboardingWanted = new AtomicBoolean();
    /**
     * The installations catch-up has asked about in this run, answered either way - No is asked again next run, not
     * again this one (Alan, 2026-10-15, A14). EDT only.
     */
    private final Set<Long> catchUpAsked = new HashSet<>();
    /**
     * The controllers plugged in that onboarding still owes an offer, each once per run (Alan, 2026-10-15, A7).
     */
    private final HotPlugOffers hotPlug = new HotPlugOffers();
    /**
     * Set when a controller connects or disconnects while the screen is hidden, and cleared by the next refresh: the
     * screen reads the hardware again when it is next shown, rather than on every plug while nobody is looking.
     */
    private final AtomicBoolean hardwareStale = new AtomicBoolean();
    /**
     * Set by CHECK AGAIN and taken by the next identity offer: the questions answered Not now are asked again too
     * (Alan, 2026-10-15, A12).
     */
    private final AtomicBoolean identityDeferredWanted = new AtomicBoolean();
    /**
     * The missing controllers the identity question has been put about in this run. Every answer is stored, so this
     * only stops a write that failed from putting the same question straight back; CHECK AGAIN clears it. EDT only.
     */
    private final Set<String> identityAsked = new HashSet<>();
    /** What Review in Bind Editor does - set by the BindForge tab. EDT only. */
    private Runnable reviewBindings = () -> {
    };

    /**
     * The last refresh, with its unread answers filled from the one before. EDT only.
     */
    private Snapshot shown = NOTHING_YET;
    /**
     * Whether activation is running. SET UP waits for it, or the two could open first setup twice (review,
     * 2026-10-13). EDT only.
     */
    private boolean activationRunning;
    /**
     * Whether a write the user started - APPLY, DISCARD, a way out, onboarding - is running. EDT only.
     */
    private boolean actionRunning;

    AliasDesignerController(AliasDesignerPanel view, InstallationRegistry registry) {
        this.view = view;
        this.registry = registry;
        this.reader = new AliasDesignerReader(registry);
        UiBus.register(this);
        DeviceBus.register(this);
    }

    void dispose() {
        UiBus.unregister(this);
        DeviceBus.unregister(this);
    }

    /**
     * What the view draws from. EDT only.
     */
    Snapshot shown() {
        return shown;
    }

    /**
     * Whether a write is running, so the view draws the ways out disabled. EDT only.
     */
    boolean actionRunning() {
        return actionRunning;
    }

    /**
     * An installation's device files changed on disk - read again, so the check runs on what is there now. A refresh
     * already running may have read them before the change, so it is followed by another rather than trusted.
     */
    @Subscribe
    public void onDeviceFilesChanged(DeviceFilesChangedEvent event) {
        refresh();
    }

    /**
     * A controller was plugged in (A7). It waits for onboarding's offer - which leaves it out if it already has a name -
     * and the screen reads the hardware again, so it appears ATTACHED. On the device bus's thread, which also carries
     * push-to-talk: nothing here reads a file or the database.
     * <p>
     * A connect published before this registered - the controllers present at startup - is never seen here. Those
     * are the automatic offer's after activation, which asks about every attached controller.
     */
    @Subscribe
    public void onDeviceConnected(DeviceConnectedEvent event) {
        DeviceIdentities.of(event.device()).ifPresent(identity ->
                hotPlug.connected(event.device().id(), new HardwareId(identity.vid(), identity.pid())));
        SwingUtilities.invokeLater(this::hardwareChanged);
    }

    /**
     * A controller was unplugged (A7): it stops waiting for an offer, and the screen reads the hardware again, so it
     * shows MISSING.
     */
    @Subscribe
    public void onDeviceDisconnected(DeviceDisconnectedEvent event) {
        hotPlug.disconnected(event.deviceId());
        SwingUtilities.invokeLater(this::hardwareChanged);
    }

    /**
     * Reads everything again while the user is looking; otherwise the next showing does. EDT.
     */
    private void hardwareChanged() {
        if (view.isShowing()) {
            refresh();
        } else {
            hardwareStale.set(true);
        }
    }

    /**
     * The screen became visible. The first time in a run, not at startup (Alan, 2026-10-11, A13): the refresh at
     * startup runs before the Device Service has looked at any hardware, so this asks for one of its own and the
     * missing-controller warning judges that one. Onboarding's offer, still waiting, asks the same - and so does
     * catch-up, for an installation found again that it has not asked about in this run.
     */
    void onShown() {
        boolean warning = MissingControllerWarning.getInstance().pending();
        if (!warning && !onboardingWanted.get() && !catchUpMayBeWanted() && !hardwareStale.get()
                && !hotPlug.anyWaiting()) return;
        if (warning) missingWarningWanted.set(true);
        refresh();
    }

    /**
     * CHECK AGAIN: reads everything again, and offers onboarding again. Backed out of, onboarding is spent for the
     * run, and a refresh alone changes nothing on screen - so CHECK AGAIN looked dead to a user who still had
     * controllers to name (Krondor, 2026-10-14). It says nothing when nobody needs a name.
     */
    void checkAgain() {
        // WHY: the identity question first, deferred ones included - the user pressed CHECK AGAIN to deal with the
        // unnamed controllers, and naming one first would end its question unasked (Alan, 2026-10-15, A12).
        identityAsked.clear();
        identityDeferredWanted.set(true);
        onboardingWanted.set(true);
        refresh();
    }

    /** What Review in Bind Editor does, after a retarget. EDT only. */
    void setReviewBindings(Runnable showBindings) {
        reviewBindings = Objects.requireNonNull(showBindings, "showBindings");
    }

    // ------------------------------------------------------------------ the refresh

    /**
     * Reads the installations and their device files off the EDT, same as
     * {@code BindingManagementPanel.performBackup()} - parsing every {@code .binds} and every installation's
     * {@code DeviceMappings.xml} would otherwise freeze the window, and an installation on a spun-down or
     * disconnected drive freezes it for as long as the filesystem takes to answer.
     * <p>
     * A refresh already in flight is left to finish rather than a second one being started beside it, and one more
     * follows it, however many requests came in meanwhile - see {@link RefreshCoalescer} for why none is dropped. A
     * ship-profile change mid-refresh therefore costs one extra refresh; nothing here depends on the ship.
     */
    void refresh() {
        hardwareStale.set(false);
        if (!refreshes.tryStart()) return;
        new Thread(() -> {
            Snapshot read = reader.read(missingWarningWanted.getAndSet(false));
            SwingUtilities.invokeLater(() -> {
                shown = keepingLastAnswers(read);
                refreshButtons();
                view.render(shown);
                // WHY: catch-up first - it finishes writes the user already made - and onboarding waits for the next
                // refresh, so the two never write the same installation at once (Alan, 2026-10-15, A14).
                boolean catchUpOffered = offerCatchUp();
                // WHY: the identity question before onboarding - naming a candidate would end it unasked, and a
                // retarget is what saves the bindings (Alan, 2026-10-15, A12).
                boolean identityOffered = !catchUpOffered && offerIdentity(read);
                Set<HardwareId> heldBack = read.identity().withheld();
                // WHY: only once the hardware is listed, and only while the user is looking. Before the listing every
                // controller reads absent, so nobody would need a name and the offer would be spent on nothing; a
                // refresh the user did not start would put the dialog over another screen.
                // WHY: not over another dialog either - a refresh can land while the identity questions are showing.
                if (!catchUpOffered && !identityOffered && activated() && read.listed() && view.isShowing()
                        && !dialogOpen() && onboardingWanted.compareAndSet(true, false)) {
                    // WHY: the automatic offer asks about every attached controller, those plugged in meanwhile too -
                    // but a candidate an identity question still holds keeps waiting.
                    hotPlug.take(heldBack);
                    openOnboarding(null, false, heldBack);
                } else if (!catchUpOffered && !identityOffered && hotPlugOfferMayOpen(read)) {
                    openOnboarding(hotPlug.take(heldBack), false, heldBack);
                }
                // WHY: something asked for a refresh while this one ran - the screen shown, a device file changed, an
                // action finished - and this one may have read what it changed too early.
                if (refreshes.finish()) refresh();
            });
        }, "BindForge-Divergence").start();
    }

    /**
     * A refresh that failed before reading activation or the hardware keeps the last answers for them.
     */
    private Snapshot keepingLastAnswers(Snapshot read) {
        return new Snapshot(read.labels(), read.paths(), read.report(), read.findings(), read.myDevices(),
                read.builtIn(), read.markers(),
                read.activated() != null ? read.activated() : shown.activated(),
                read.changes(),
                read.running() != null ? read.running() : shown.running(),
                read.listed(),
                read.applyWork(),
                read.foundAgain(),
                read.identity());
    }

    private boolean activated() {
        return Boolean.TRUE.equals(shown.activated());
    }

    /**
     * SET UP until BindForge is activated, and not while activation runs, which may open first setup itself. APPLY
     * once activated and only when it has work ({@link ApplyWork}), DISCARD while the draft changes something - and
     * neither while a write runs.
     */
    private void refreshButtons() {
        view.refreshButtons(!activated() && !activationRunning, canApply(),
                !shown.changes().isEmpty() && !actionRunning, shown.changes());
    }

    private boolean canApply() {
        return activated() && shown.applyWork() && !actionRunning;
    }

    // ------------------------------------------------------------------ activation and first setup

    /**
     * Activation, the first time the BindForge tab is shown in a run (Alan, 2026-10-13, A1b): a "before BindForge"
     * backup, then the master filled with no prompt when the installations are identical, or first setup opened when
     * they are not. On the EDT; does nothing once tried in this run.
     */
    void activateOnFirstShowing() {
        if (!activationTried.compareAndSet(false, true)) return;
        onboardingWanted.set(true);
        activationRunning = true;
        refreshButtons();
        new Thread(() -> {
            Activation.Result result = null;
            Map<String, String> names = Map.of();
            try {
                // WHY: asked first, so every run after activation - nearly all of them - pays for no detection and
                // starts none to race the refresh's (review, 2026-10-13).
                if (!BindForgeActivationManager.getInstance().isActivated()) {
                    // WHY: a detection of its own, not currentWithStartupScan(). Activation reads the stored list, and
                    // on a fresh database nothing is stored until detection has run; the startup scan may still be
                    // running on the refresh thread, and the call that loses that race returns the list as it was -
                    // so the user would activate with an empty master. rescan() waits for a detection in progress.
                    List<InstallationRow> rows = registry.rescan();
                    names = AliasDesignerReader.labelsFor(rows);
                    result = Activation.stored().run();
                    reportActivationFailure(result.outcome(), result.reason());
                }
            } catch (RuntimeException e) {
                // WHY: broad on purpose - a thread boundary. Not activated, so SET UP stays offered.
                log.warn("BindForge could not be activated", e);
                reportActivationFailure(Activation.Outcome.FAILED, messageOf(e));
            }
            Activation.Result loadedResult = result;
            Map<String, String> loadedNames = names;
            SwingUtilities.invokeLater(() -> {
                activationRunning = false;
                refreshButtons();
                if (loadedResult != null && loadedResult.outcome() == Activation.Outcome.FIRST_SETUP) {
                    showFirstSetup(loadedResult.setup(), loadedResult.reading(),
                            AliasDesignerReports.problemOf(loadedResult), loadedNames);
                }
                refresh();
            });
        }, "BindForge-Activation").start();
    }

    /**
     * A failed activation is said in the application log, as a successful one's backup is.
     */
    private static void reportActivationFailure(Activation.Outcome outcome, String reason) {
        String line = AliasDesignerReports.activationFailure(outcome, reason);
        if (line != null) UiBus.publish(new AppLogEvent(line));
    }

    /**
     * SET UP: reads every installation for first setup off the EDT, then opens it. Nothing is written until the user
     * confirms the summary inside it.
     */
    void openFirstSetup() {
        // WHY: activation may open first setup itself any moment now - one dialog, not two (review, 2026-10-13).
        if (activationRunning) return;
        view.refreshButtons(false, canApply(), !shown.changes().isEmpty() && !actionRunning, shown.changes());
        Map<String, String> names = shown.labels();
        new Thread(() -> {
            FirstSetup setup = null;
            FirstSetup.Reading reading = null;
            String problem = null;
            try {
                // WHY: SET UP is a way into activation too, so it takes the "before BindForge" backup if activation
                // could not - otherwise finishing first setup would activate without it, for good (review, 2026-10-13).
                BeforeBindForgeBackup.stored().ensureTaken();
            } catch (IOException | RuntimeException e) {
                // WHY: broad on purpose - a thread boundary, and the user pressed a button and must be told.
                log.warn("First setup did not open: the backup before BindForge could not be taken", e);
                problem = getText("bindings.aliasDesigner.firstSetup.result.backupFailed", messageOf(e));
            }
            if (problem == null) {
                try {
                    setup = FirstSetup.stored();
                    reading = setup.read();
                } catch (IOException | RuntimeException e) {
                    // WHY: broad on purpose - a thread boundary, and the user pressed a button and must be told.
                    log.warn("First setup could not read the installations", e);
                    problem = getText("bindings.aliasDesigner.firstSetup.readFailed", messageOf(e));
                }
            }
            FirstSetup loadedSetup = setup;
            FirstSetup.Reading loadedReading = reading;
            String loadedProblem = problem;
            SwingUtilities.invokeLater(() -> {
                refreshButtons();
                showFirstSetup(loadedSetup, loadedReading, loadedProblem, names);
            });
        }, "BindForge-FirstSetupRead").start();
    }

    /**
     * First setup's dialog for a reading, naming the problem when there is one - or, with no reading, the problem
     * alone. On the EDT.
     *
     * @param problem the text to show the user, already worded; {@code null} when nothing went wrong
     */
    private void showFirstSetup(FirstSetup setup, FirstSetup.Reading reading, String problem,
                                Map<String, String> names) {
        // WHY: decided on the reading, not the message. An exception can carry no message, and keyed on that, a
        // failed read would open the dialog with nothing to show.
        if (setup == null || reading == null) {
            HudConfirmDialog.info(view, getText("bindings.aliasDesigner.firstSetup.title"), problem,
                    getText("bindings.aliasDesigner.firstSetup.close"));
            return;
        }
        new FirstSetupDialog(view, setup, reading, problem, names, this::refresh).showDialog();
    }

    // ------------------------------------------------------------------ APPLY and DISCARD

    /**
     * APPLY: the draft into the master, then the master and the pending removals out to every installation, reported
     * per installation. Asked first - it is the one button on the screen that writes a game file.
     */
    void apply() {
        String title = getText("bindings.aliasDesigner.apply.title");
        int installations = shown.labels().size();
        // WHY: said rather than asked. With no installation known there is nothing to write to, and "write to all 0
        // installations?" reads as a fault.
        if (installations == 0) {
            HudConfirmDialog.info(view, title, getText("bindings.aliasDesigner.apply.noInstallations",
                            getText("bindings.tab.bindingManagement"), getText("bindings.tab.gameInstallLocations")),
                    getText("bindings.aliasDesigner.firstSetup.close"));
            return;
        }
        String question = installations == 1
                ? getText("bindings.aliasDesigner.apply.confirm.one")
                : getText("bindings.aliasDesigner.apply.confirm", installations);
        // WHY: told, not fixed (Alan, 2026-10-14, A11). On Linux each installation has its own bindings folder, and
        // BindForge moves bindings in the one Elite-Intel uses - so a rename, or onboarding's hex move, can leave
        // another installation's preset naming a device by a name it no longer has.
        if (installations > 1 && OsDetector.getOs() == OsDetector.OS.LINUX) {
            question += "\n\n" + getText("bindings.aliasDesigner.apply.linuxNote");
        }
        if (!HudConfirmDialog.confirm(view, title, question,
                getText("bindings.aliasDesigner.apply.do"),
                getText("bindings.aliasDesigner.firstSetup.cancel"))) {
            return;
        }
        AliasDesignerReports reports = reports();
        runAndReport(title, () -> reports.applyText(new DeviceApply().apply()));
    }

    /**
     * DISCARD: the whole draft thrown away, after naming what it holds. No installation is touched.
     */
    void discard() {
        List<String> changes = shown.changes();
        if (changes.isEmpty()) return;
        if (!HudConfirmDialog.confirm(view, getText("bindings.aliasDesigner.discard.title"),
                getText("bindings.aliasDesigner.discard.confirm", String.join(", ", changes)),
                getText("bindings.aliasDesigner.discard.do"),
                getText("bindings.aliasDesigner.firstSetup.cancel"))) {
            return;
        }
        runAndReport(getText("bindings.aliasDesigner.discard.title"), () -> {
            BindForgeDeviceDraftManager.getInstance().discard();
            return null;
        });
    }

    // ------------------------------------------------------------------ catch-up

    /**
     * The installations found again - each missed a write and can be reached now - that catch-up has not asked about in
     * this run. EDT only.
     */
    private List<Long> catchUpToAsk() {
        return shown.foundAgain().stream().filter(id -> !catchUpAsked.contains(id)).toList();
    }

    /**
     * Whether some installation missed a write and has not been asked about in this run - from the stored flag, not
     * the last refresh, which may have run before the folder came back (review, 2026-10-15). A held list, so no disk
     * is read here; the refresh this asks for decides whether the installation can be reached.
     */
    private boolean catchUpMayBeWanted() {
        return BindForgeInstallationsManager.getInstance().findAll().stream()
                .anyMatch(row -> row.missedWrite() && !catchUpAsked.contains(row.id()));
    }

    /**
     * Asks about the installations found again, once per run each, while the user is on the screen (Alan, 2026-10-15,
     * A14) - whether the startup announcement said so or the check has just found one. Asked after this refresh has
     * finished drawing, so the dialog never holds a refresh open.
     *
     * @return whether it is asking - onboarding's offer then waits for the refresh after the answer
     */
    private boolean offerCatchUp() {
        if (!activated() || actionRunning || !view.isShowing()) return false;
        List<Long> ids = catchUpToAsk();
        if (ids.isEmpty()) return false;
        catchUpAsked.addAll(ids);
        SwingUtilities.invokeLater(() -> askCatchUp(ids));
        return true;
    }

    private void askCatchUp(List<Long> ids) {
        // WHY: a click queued before this - APPLY, a revert - may have started a write since the offer. Two writes to
        // one installation at once would each keep an Edit History copy of a file the other is replacing, so the
        // question goes back, and the refresh that ends that write offers it again (review, 2026-10-15).
        if (actionRunning) {
            ids.forEach(catchUpAsked::remove);
            return;
        }
        String title = getText("bindings.aliasDesigner.catchUp.title");
        AliasDesignerReports reports = reports();
        String names = reports.namesOf(ids);
        String question = ids.size() == 1
                ? getText("bindings.aliasDesigner.catchUp.confirm.one", names)
                : getText("bindings.aliasDesigner.catchUp.confirm.many", names);
        if (!HudConfirmDialog.confirm(view, title, question,
                getText("bindings.aliasDesigner.catchUp.do"),
                getText("bindings.aliasDesigner.catchUp.notNow"))) {
            log.info("Catch-up declined for {} - asked again next run", names);
            // WHY: a refresh, so onboarding's offer - held back for this question - is made now.
            refresh();
            return;
        }
        runAndReport(title, () -> reports.applyText(new DeviceApply().catchUp(ids)));
    }

    // ------------------------------------------------------------------ the ways out

    /**
     * Revert: pushes the master to each installation the row names. That is the whole installation, not the row, so
     * the confirm says how many rows it reaches.
     */
    void revert(WayOut way) {
        Set<String> keys = new LinkedHashSet<>();
        way.installIds().forEach(id -> keys.add(DeviceDivergence.installKey(id)));
        long rows = shown.findings().stream()
                .filter(finding -> finding.installs().stream().anyMatch(keys::contains))
                .count();
        AliasDesignerReports reports = reports();
        if (!HudConfirmDialog.confirm(view, getText("bindings.aliasDesigner.wayOut.title"),
                getText("bindings.aliasDesigner.wayOut.revert.confirm", reports.namesOf(way.installIds()), rows),
                getText("bindings.aliasDesigner.wayOut.revert.do"),
                getText("bindings.aliasDesigner.firstSetup.cancel"))) {
            return;
        }
        runWayOut(() -> {
            DeviceFilesPush push = new DeviceFilesPush();
            List<DeviceFilesPush.InstallationResult> results = new ArrayList<>();
            for (long installId : way.installIds()) results.addAll(push.push(installId).installations());
            return reports.revertText(new DeviceFilesPush.Report(results));
        });
    }

    /**
     * Takes the row's device, as one installation holds it, into the master - and nothing else of that installation's.
     */
    void adopt(WayOut way, DeviceDivergence.Finding finding) {
        long installId = way.installIds().getFirst();
        AliasDesignerReports reports = reports();
        String installName = reports.namesOf(way.installIds());
        if (!HudConfirmDialog.confirm(view, getText("bindings.aliasDesigner.wayOut.title"),
                getText("bindings.aliasDesigner.wayOut.adopt.confirm", finding.deviceName(), installName,
                        String.valueOf(way.hardware())),
                getText("bindings.aliasDesigner.wayOut.adopt.do"),
                getText("bindings.aliasDesigner.firstSetup.cancel"))) {
            return;
        }
        runWayOut(() -> reports.adoptText(new DeviceFilesAdopt().adopt(installId, finding.deviceName()),
                getText("bindings.aliasDesigner.wayOut.adopt.done", finding.deviceName(), installName)));
    }

    /**
     * The label merge with the master as one side. The labels are read off the EDT, the merge view asks about the
     * inputs that collide - none, when the sides only add to each other - and the answer goes into the master.
     */
    void reMerge(WayOut way, DeviceDivergence.Finding finding) {
        String device = finding.deviceName();
        String title = getText("bindings.aliasDesigner.wayOut.title");
        AliasDesignerReports reports = reports();
        Map<String, String> installLabels = shown.labels();
        offEdt(title, () -> LabelReMerge.stored(device, way.installIds()), merge -> {
            Map<String, String> answers = Map.of();
            if (!merge.collisions().isEmpty()) {
                Map<String, String> sideNames = new LinkedHashMap<>(installLabels);
                sideNames.put(LabelReMerge.MASTER, getText("bindings.aliasDesigner.wayOut.masterSide"));
                Optional<Map<String, String>> chosen =
                        new LabelMergeDialog(view, device, merge, sideNames, Map.of()).showDialog();
                if (chosen.isEmpty()) return;
                answers = chosen.get();
            }
            List<String> unanswered = merge.unanswered(answers);
            if (!unanswered.isEmpty()) {
                HudConfirmDialog.info(view, title,
                        getText("bindings.aliasDesigner.wayOut.remerge.unanswered", unanswered.size()),
                        getText("bindings.aliasDesigner.firstSetup.close"));
                return;
            }
            Map<String, String> merged = merge.merged(answers);
            if (!HudConfirmDialog.confirm(view, title,
                    getText("bindings.aliasDesigner.wayOut.remerge.confirm", device),
                    getText("bindings.aliasDesigner.wayOut.remerge.do"),
                    getText("bindings.aliasDesigner.firstSetup.cancel"))) {
                return;
            }
            runWayOut(() -> reports.adoptText(new DeviceFilesAdopt().adoptLabels(device, merged),
                    getText("bindings.aliasDesigner.wayOut.remerge.done", device)));
        });
    }

    private void runWayOut(Callable<String> work) {
        runAndReport(getText("bindings.aliasDesigner.wayOut.title"), work);
    }

    // ------------------------------------------------------------------ the identity question

    /**
     * Puts the identity questions this refresh found, one missing controller at a time (Alan, 2026-10-15, A12): after
     * catch-up, before onboarding, and under onboarding's conditions - activated, the hardware listed, the user on the
     * screen, no write running and no other dialog open. Asked after this refresh has finished drawing.
     *
     * @return whether it is asking - onboarding's offer then waits for the refresh after the answers
     */
    private boolean offerIdentity(Snapshot read) {
        if (!activated() || !read.listed() || !view.isShowing() || actionRunning || dialogOpen()) return false;
        List<IdentityQuestions.Question> questions = (identityDeferredWanted.getAndSet(false)
                ? read.identity().askingDeferred()
                : read.identity().toAsk()).stream()
                .filter(question -> !identityAsked.contains(question.deviceName()))
                .toList();
        if (questions.isEmpty()) return false;
        questions.forEach(question -> identityAsked.add(question.deviceName()));
        SwingUtilities.invokeLater(() -> askIdentity(questions));
        return true;
    }

    /**
     * One dialog per missing controller. A controller picked for one is left out of the next, since one controller
     * cannot be two; a question left with no candidate is not put. Every answer is written together once the last is
     * given: a pick retargets the device in the draft, Not now and None of these are remembered.
     */
    private void askIdentity(List<IdentityQuestions.Question> questions) {
        // WHY: a click queued before this may have started a write since the offer - the question goes back, for the
        // refresh that ends the write to put again, as catch-up's does.
        if (actionRunning) {
            questions.forEach(question -> identityAsked.remove(question.deviceName()));
            return;
        }
        List<IdentityDecision> decisions = new ArrayList<>();
        Set<HardwareId> picked = new HashSet<>();
        for (IdentityQuestions.Question asked : questions) {
            IdentityQuestions.Question question = asked;
            for (HardwareId taken : picked) question = question.without(taken);
            if (question.candidates().isEmpty()) continue;
            IdentityDialog.Choice choice = new IdentityDialog(view, question).ask();
            if (choice.picked() != null) picked.add(choice.picked().hardware());
            decisions.add(new IdentityDecision(question, choice));
        }
        if (decisions.isEmpty()) return;
        // WHY: None of these routes to create - the controller is a new one, which onboarding names.
        if (decisions.stream().anyMatch(d -> d.choice().answer() == IdentityQuestions.Answer.NONE_OF_THESE)) {
            onboardingWanted.set(true);
        }
        String title = getText("bindings.aliasDesigner.identity.title");
        offEdt(title, () -> writeIdentity(decisions), result -> {
            showIdentityResult(title, result);
            refresh();
        });
    }

    private record IdentityDecision(IdentityQuestions.Question question, IdentityDialog.Choice choice) {
    }

    /**
     * What the identity answers wrote.
     *
     * @param moved controllers retargeted, each with what it now points at
     * @param stale controllers that changed between the question and the answer - nothing was written for them
     */
    private record IdentityResult(List<IdentityDecision> moved, List<String> stale) {
    }

    private static IdentityResult writeIdentity(List<IdentityDecision> decisions) {
        BindForgeDeviceDraftManager draft = BindForgeDeviceDraftManager.getInstance();
        BindForgeIdentityAnswerManager answers = BindForgeIdentityAnswerManager.getInstance();
        List<IdentityDecision> moved = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        for (IdentityDecision decision : decisions) {
            IdentityQuestions.Question question = decision.question();
            MyDevice picked = decision.choice().picked();
            if (picked == null) {
                answers.put(question.deviceName(), question.candidatesKey(), decision.choice().answer().name());
                log.info("Identity question for {} answered {}", question.deviceName(), decision.choice().answer());
            } else if (draft.retargetByName(question.deviceName(), question.was().vid(), question.was().pid(),
                    picked.vid(), picked.pid())) {
                answers.forget(Set.of(question.deviceName()));
                log.info("{} retargeted in the draft from {} to {} ({})", question.deviceName(), question.was(),
                        picked.hardware(), picked.label());
                moved.add(decision);
            } else {
                stale.add(question.deviceName());
            }
        }
        return new IdentityResult(moved, stale);
    }

    /**
     * After a retarget: what moves, and what it cannot promise - bindings keep their button numbers, and on another
     * model some buttons are elsewhere. Review in Bind Editor shows the bindings.
     */
    private void showIdentityResult(String title, IdentityResult result) {
        List<String> lines = new ArrayList<>();
        for (IdentityDecision decision : result.moved()) {
            MyDevice picked = decision.choice().picked();
            lines.add(getText("bindings.aliasDesigner.identity.moved", decision.question().deviceName(),
                    picked.label(), picked.vid(), picked.pid(), decision.question().bindings()));
        }
        for (String name : result.stale()) lines.add(getText("bindings.aliasDesigner.identity.stale", name));
        if (lines.isEmpty()) return;
        if (result.moved().isEmpty()) {
            HudConfirmDialog.info(view, title, String.join("\n\n", lines),
                    getText("bindings.aliasDesigner.firstSetup.close"));
            return;
        }
        lines.add(getText("bindings.aliasDesigner.identity.layoutWarning"));
        if (HudConfirmDialog.confirm(view, title, String.join("\n\n", lines),
                getText("bindings.aliasDesigner.identity.review"),
                getText("bindings.aliasDesigner.identity.dismiss"))) {
            reviewBindings.run();
        }
    }

    // ------------------------------------------------------------------ onboarding and the Device Editor

    /**
     * Whether onboarding may be offered now for the controllers plugged in later (Alan, 2026-10-15, A7): as for the
     * automatic offer, once activated, the hardware listed and the user on the screen - and not while a write runs, nor
     * over another dialog such as the Device Editor. Held back, the controllers keep waiting for a later refresh.
     */
    private boolean hotPlugOfferMayOpen(Snapshot read) {
        return hotPlug.anyWaitingBesides(read.identity().withheld()) && activated() && read.listed() && view.isShowing() && !actionRunning
                && !dialogOpen();
    }

    /**
     * Whether a modal dialog of this window is showing. The screen still counts as showing beneath one, and a second
     * dialog would open over it.
     */
    private boolean dialogOpen() {
        Window window = SwingUtilities.getWindowAncestor(view);
        if (window == null) return false;
        return Arrays.stream(window.getOwnedWindows())
                .anyMatch(owned -> owned.isShowing() && owned instanceof Dialog dialog && dialog.isModal());
    }

    /**
     * NAME, from the Device Editor of an attached controller that has none: onboarding for that one controller, which
     * says so when it turns out to need no name.
     */
    void openOnboarding(MyDevice only) {
        // WHY: nothing held back - choosing to name a candidate is the user's answer, and naming it takes it out of
        // every identity question's candidates (Alan, 2026-10-15, A12).
        openOnboarding(Set.of(only.hardware()), true, Set.of());
    }

    /**
     * Onboarding: reads who needs a name off the EDT, opens the dialog, and writes what the user chose when they
     * finish (Alan, 2026-10-14, A6b).
     * <p>
     * Every controller it asks about counts as offered in this run, so plugging it in again does not ask a second time
     * (A7).
     *
     * @param only      the controllers to ask about, or {@code null} for every controller that needs a name
     * @param sayNobody whether to say so when none of them needs a name - only when the user asked, from NAME
     * @param heldBack  controllers left out: candidates of an identity question still open
     */
    private void openOnboarding(Set<HardwareId> only, boolean sayNobody, Set<HardwareId> heldBack) {
        String title = getText("bindings.aliasDesigner.onboarding.title");
        AliasDesignerReports reports = reports();
        offEdt(title, () -> reader.readOnboarding(only, heldBack), (OnboardingReading reading) -> {
            hotPlug.offered(reading.pages().stream().map(page -> page.device().hardware()).toList());
            if (reading.problem() != null) {
                HudConfirmDialog.info(view, title, reading.problem(),
                        getText("bindings.aliasDesigner.firstSetup.close"));
                return;
            }
            if (reading.pages().isEmpty()) {
                if (sayNobody) {
                    HudConfirmDialog.info(view, title, getText("bindings.aliasDesigner.onboarding.nobody"),
                            getText("bindings.aliasDesigner.firstSetup.close"));
                }
                return;
            }
            new OnboardingDialog(view, reading.pages(), reading.taken(),
                    names -> runAndReport(title, () -> reports.onboardText(nameAll(reading.pages(), names))))
                    .showDialog();
        });
    }

    private static DeviceOnboarding.Result nameAll(List<Onboarding.Page> pages, List<String> names) throws IOException {
        List<DeviceOnboarding.Naming> namings = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            Onboarding.Page page = pages.get(i);
            namings.add(new DeviceOnboarding.Naming(names.get(i), page.device().vid(), page.device().pid(),
                    page.labels(), page.bindings()));
        }
        return DeviceOnboarding.stored().name(namings);
    }

    /**
     * Opens the Device Editor for one My Devices row. Before activation there is no master for its edits to be saved
     * against - first setup throws any draft away - so it says SET UP comes first, and offers it (Alan, 2026-10-10).
     *
     * @param strip the install strip the view drew for the row
     */
    void openDeviceEditor(MyDevice device, List<DeviceEditorDialog.StripLine> strip) {
        // WHY: the stored fact, not the report - a refresh that failed also leaves no report, and once activated that is
        // no reason to send the user to first setup (review, 2026-10-11). Activated, not "the master has rows": a
        // user who owns nothing is activated with an empty master (Alan, 2026-10-13).
        if (!activated()) {
            if (HudConfirmDialog.confirm(view, getText("bindings.aliasDesigner.editor.heading"),
                    getText("bindings.aliasDesigner.editor.beforeSetUp.message"),
                    getText("bindings.aliasDesigner.firstSetup.button"),
                    getText("button.back"))) {
                openFirstSetup();
            }
            return;
        }
        new Thread(() -> {
            DeviceEditorDialog.Opening opening = null;
            String failure = null;
            try {
                opening = reader.openingFor(device, strip);
            } catch (RuntimeException e) {
                // WHY: broad on purpose - a thread boundary, and the user pressed the gear and must be told.
                log.warn("The Device Editor could not read {}", device.label(), e);
                failure = messageOf(e);
            }
            DeviceEditorDialog.Opening loaded = opening;
            String loadedFailure = failure;
            SwingUtilities.invokeLater(() -> {
                if (loaded == null) {
                    HudConfirmDialog.info(view, getText("bindings.aliasDesigner.editor.heading"),
                            getText("bindings.aliasDesigner.editor.loadFailed", loadedFailure),
                            getText("bindings.aliasDesigner.firstSetup.close"));
                    return;
                }
                new DeviceEditorDialog(view, loaded, this::refresh, () -> openOnboarding(device)).showDialog();
            });
        }, "BindForge-DeviceEditorRead").start();
    }

    // ------------------------------------------------------------------ running a write

    /**
     * The words for a report, with the installations named as they are on screen now.
     */
    private AliasDesignerReports reports() {
        return new AliasDesignerReports(shown.labels());
    }

    /**
     * Runs a write off the EDT - a way out, APPLY, DISCARD or onboarding's names - says what happened when there is
     * something to say, and reads everything again.
     */
    private void runAndReport(String title, Callable<String> work) {
        offEdt(title, work, message -> {
            if (message != null) {
                HudConfirmDialog.info(view, title, message, getText("bindings.aliasDesigner.firstSetup.close"));
            }
            refresh();
        });
    }

    /**
     * Reads or writes game files and the master off the EDT, with the buttons disabled meanwhile, then hands the
     * result back on it. A failure is shown under {@code title}, not only logged - the user pressed a button and must
     * be told - and the screen is read again, since a write may have got part of the way.
     */
    private <T> void offEdt(String title, Callable<T> work, Consumer<T> onEdt) {
        // WHY: APPLY and DISCARD wait too - a revert and an Apply writing the same installation at once would each
        // keep an Edit History copy of a file the other was replacing.
        actionRunning = true;
        view.disableWaysOut();
        refreshButtons();
        new Thread(() -> {
            T result = null;
            String failure = null;
            try {
                result = work.call();
            } catch (Exception e) {
                // WHY: broad on purpose - a thread boundary, and anything escaping it would leave the buttons
                // disabled with no word of why.
                log.warn("{} failed", title, e);
                failure = messageOf(e);
            }
            T loaded = result;
            String loadedFailure = failure;
            SwingUtilities.invokeLater(() -> {
                actionRunning = false;
                refreshButtons();
                view.showWaysOut();
                if (loadedFailure != null) {
                    HudConfirmDialog.info(view, title, getText("bindings.aliasDesigner.wayOut.failed", loadedFailure),
                            getText("bindings.aliasDesigner.firstSetup.close"));
                    refresh();
                    return;
                }
                onEdt.accept(loaded);
            });
        }, "BindForge-ScreenWrite").start();
    }
}
