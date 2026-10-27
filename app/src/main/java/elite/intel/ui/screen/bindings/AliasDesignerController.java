package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.devices.*;
import elite.intel.bindforge.storage.DeviceEdits;
import elite.intel.bindforge.storage.Edits;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeIdentityAnswerManager;
import elite.intel.devices.events.DeviceConnectedEvent;
import elite.intel.devices.events.DeviceDisconnectedEvent;
import elite.intel.eventbus.DeviceBus;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
import elite.intel.ui.screen.bindings.AliasDesignerReader.OnboardingReading;
import elite.intel.ui.screen.bindings.AliasDesignerReader.Snapshot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.util.StringUtls.messageOf;

/**
 * The Alias Designer screen's flows: the refresh, the identity question (A12), onboarding, controllers plugged in or out
 * (A7), and opening the Device Editor. It decides when each runs, runs the disk work off the EDT, asks and tells the
 * user, and hands what it read to {@link AliasDesignerPanel}, which only draws.
 * <p>
 * <strong>Nothing here writes a game file</strong> (storage.md rule 6; Storage S4a). Onboarding's names and the Device
 * Editor's changes are edits not applied ({@link Edits}); the one APPLY above BindForge's tabs writes them. The screen
 * reads again whenever the edits change - an edit, DISCARD, APPLY or RESTORE.
 * <p>
 * State is kept on the EDT, but for the flags background threads touch, which are atomic.
 */
final class AliasDesignerController {

    private static final Logger log = LogManager.getLogger(AliasDesignerController.class);

    /**
     * What the screen shows before its first refresh has finished.
     */
    private static final Snapshot NOTHING_YET = new Snapshot(List.of(), List.of(), false, false, false,
            IdentityQuestions.Reading.NONE);

    private final AliasDesignerPanel view;
    private final AliasDesignerReader reader = new AliasDesignerReader();

    private final RefreshCoalescer refreshes = new RefreshCoalescer();
    /**
     * Set when the screen is shown while this run's missing-controller warning is still to come, and taken by the next
     * refresh to start. A refresh already running when the screen appeared may have read the hardware before the
     * Device Service had listed it, so it is never the one that judges.
     */
    private final AtomicBoolean missingWarningWanted = new AtomicBoolean();
    /**
     * Set from the start and on CHECK AGAIN, and taken by the first refresh after it that finds first-time startup
     * complete and the hardware listed: onboarding opens then, for the controllers attached (Alan, 2026-10-14, A6b).
     */
    private final AtomicBoolean onboardingWanted = new AtomicBoolean(true);
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
     * Whether a change the user started - onboarding's names, an identity answer - is being made. EDT only.
     */
    private boolean actionRunning;

    AliasDesignerController(AliasDesignerPanel view) {
        this.view = view;
        UiBus.register(this);
        DeviceBus.register(this);
    }

    void dispose() {
        UiBus.unregister(this);
        DeviceBus.unregister(this);
    }

    /**
     * The edits changed - an edit, DISCARD, APPLY, RESTORE or first-time startup - so the names and labels shown may
     * have. Read again while the user is looking; otherwise the next showing does.
     */
    @Subscribe
    public void onEditsChanged(BindForgeEditsChangedEvent event) {
        SwingUtilities.invokeLater(this::hardwareChanged);
    }

    /**
     * A controller was plugged in (A7). It waits for onboarding's offer - which leaves it out if it already has a name -
     * and the screen reads the hardware again, so it appears ATTACHED. On the device bus's thread, which also carries
     * push-to-talk: nothing here reads a file or the database.
     * <p>
     * A connect published before this registered - the controllers present at startup - is never seen here. Those
     * are the automatic offer's after first-time startup, which asks about every attached controller.
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
     * missing-controller warning judges that one. Onboarding's offer, still waiting, asks the same.
     */
    void onShown() {
        boolean warning = MissingControllerWarning.getInstance().pending();
        if (!warning && !onboardingWanted.get() && !hardwareStale.get() && !hotPlug.anyWaiting()) return;
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
     * Reads the edits and the hardware off the EDT - parsing every binds file would otherwise freeze the window.
     * <p>
     * A refresh already in flight is left to finish rather than a second one being started beside it, and one more
     * follows it, however many requests came in meanwhile - see {@link RefreshCoalescer} for why none is dropped.
     */
    void refresh() {
        hardwareStale.set(false);
        if (!refreshes.tryStart()) return;
        new Thread(() -> {
            Snapshot read = reader.read(missingWarningWanted.getAndSet(false));
            SwingUtilities.invokeLater(() -> {
                shown = keepingLastAnswers(read);
                view.render(shown);
                // WHY: the identity question before onboarding - naming a candidate would end it unasked, and a
                // retarget is what saves the bindings (Alan, 2026-10-15, A12).
                boolean identityOffered = offerIdentity(read);
                Set<HardwareId> heldBack = read.identity().withheld();
                // WHY: only once the hardware is listed, and only while the user is looking. Before the listing every
                // controller reads absent, so nobody would need a name and the offer would be spent on nothing; a
                // refresh the user did not start would put the dialog over another screen. Not over another dialog
                // either - a refresh can land while the identity questions are showing.
                if (!identityOffered && activated() && read.listed() && view.isShowing()
                        && !dialogOpen() && onboardingWanted.compareAndSet(true, false)) {
                    // WHY: the automatic offer asks about every attached controller, those plugged in meanwhile too -
                    // but a candidate an identity question still holds keeps waiting.
                    hotPlug.take(heldBack);
                    openOnboarding(null, false, heldBack);
                } else if (!identityOffered && hotPlugOfferMayOpen(read)) {
                    openOnboarding(hotPlug.take(heldBack), false, heldBack);
                }
                // WHY: something asked for a refresh while this one ran - the screen shown, an edit made, a controller
                // plugged in - and this one may have read what it changed too early.
                if (refreshes.finish()) refresh();
            });
        }, "BindForge-AliasDesigner").start();
    }

    /**
     * A refresh that failed before reading first-time startup's flag or the hardware keeps the last answers for them.
     */
    private Snapshot keepingLastAnswers(Snapshot read) {
        return new Snapshot(read.myDevices(), read.builtIn(),
                read.activated() != null ? read.activated() : shown.activated(),
                read.running() != null ? read.running() : shown.running(),
                read.listed(), read.identity());
    }

    private boolean activated() {
        return Boolean.TRUE.equals(shown.activated());
    }

    // ------------------------------------------------------------------ the identity question

    /**
     * Puts the identity questions this refresh found, one missing controller at a time (Alan, 2026-10-15, A12): before
     * onboarding, and under onboarding's conditions - first-time startup complete, the hardware listed, the user on the
     * screen, no change being made and no other dialog open. Asked after this refresh has finished drawing.
     *
     * @return whether it is asking - onboarding's offer then waits for the refresh after the answers
     */
    private boolean offerIdentity(Snapshot read) {
        if (!activated() || !read.listed() || !view.isShowing() || actionRunning || dialogOpen()) return false;
        List<IdentityQuestions.Question> questions = (identityDeferredWanted.getAndSet(false)
                ? read.identity().askingDeferred()
                : read.identity().toAsk()).stream()
                .filter(question -> !identityAsked.contains(question.answerKey()))
                .toList();
        if (questions.isEmpty()) return false;
        questions.forEach(question -> identityAsked.add(question.answerKey()));
        SwingUtilities.invokeLater(() -> askIdentity(questions));
        return true;
    }

    /**
     * One dialog per missing controller. A controller picked for one is left out of the next, since one controller
     * cannot be two; a question left with no candidate is not put. Every answer is written together once the last is
     * given: Not now and None of these are remembered; a pick for a named controller is a retarget, still written to the
     * draft until Storage S4b moves it onto the edits.
     * <p>
     * <strong>A pick for a controller the bindings know only by its hardware id</strong> names the picked controller
     * (Alan, 2026-10-17, A17): onboarding's page opens for it, and its FINISH moves the bindings from the old id to the
     * name in the same edit. BACK gives the yes up, and the question comes back next run.
     */
    private void askIdentity(List<IdentityQuestions.Question> questions) {
        // WHY: a click queued before this may have started a change since the offer - the question goes back, for the
        // refresh that ends it to put again.
        if (actionRunning) {
            questions.forEach(question -> identityAsked.remove(question.answerKey()));
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
        Map<HardwareId, HardwareId> replacing = new LinkedHashMap<>();
        decisions.stream()
                .filter(d -> !d.question().named() && d.choice().picked() != null)
                .forEach(d -> replacing.put(d.choice().picked().hardware(), d.question().was()));
        String title = getText("bindings.aliasDesigner.identity.title");
        offEdt(title, () -> writeIdentity(decisions), result -> {
            showIdentityResult(title, result);
            if (replacing.isEmpty()) {
                refresh();
            } else {
                // WHY: the user chose to name these, so nothing is held back - as NAME holds nothing back. Refreshed on
                // every way out, since this batch may have written a Not now (review, 2026-10-17).
                openOnboarding(replacing.keySet(), true, Set.of(), replacing, true);
            }
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
                answers.put(question.answerKey(), question.candidatesKey(), decision.choice().answer().name());
                log.info("Identity question for {} answered {}", question.answerKey(), decision.choice().answer());
            } else if (!question.named()) {
                // WHY: named by onboarding's page, which opens next - nothing changes until its FINISH (A17).
                log.info("Identity question for {} answered: {} ({}), to be named", question.answerKey(), picked.label(),
                        picked.hardware());
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
     * automatic offer, once first-time startup is complete, the hardware listed and the user on the screen - and not
     * while a change is being made, nor over another dialog such as the Device Editor. Held back, the controllers keep
     * waiting for a later refresh.
     */
    private boolean hotPlugOfferMayOpen(Snapshot read) {
        return hotPlug.anyWaitingBesides(read.identity().withheld()) && activated() && read.listed() && view.isShowing()
                && !actionRunning && !dialogOpen();
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
     * Onboarding: reads who needs a name off the EDT, opens the dialog, and puts the names in the edits when the user
     * finishes (Alan, 2026-10-14, A6b; as an edit since Storage S4a, Alan, 2026-10-26).
     * <p>
     * Every controller it asks about counts as offered in this run, so plugging it in again does not ask a second time
     * (A7).
     *
     * @param only      the controllers to ask about, or {@code null} for every controller that needs a name
     * @param sayNobody whether to say so when none of them needs a name - only when the user asked, from NAME
     * @param heldBack  controllers left out: candidates of an identity question still open
     */
    private void openOnboarding(Set<HardwareId> only, boolean sayNobody, Set<HardwareId> heldBack) {
        openOnboarding(only, sayNobody, heldBack, Map.of(), false);
    }

    /**
     * @param replacing    the old hardware id each controller is, by the controller's hardware - the identity
     *                     question's yes for a controller the bindings know only by its id (A17)
     * @param refreshAfter whether to read the screen again however onboarding ends - BACK, a problem, nobody to name -
     *                     not only after FINISH, whose edit always does
     */
    private void openOnboarding(Set<HardwareId> only, boolean sayNobody, Set<HardwareId> heldBack,
                                Map<HardwareId, HardwareId> replacing, boolean refreshAfter) {
        String title = getText("bindings.aliasDesigner.onboarding.title");
        offEdt(title, () -> reader.readOnboarding(only, heldBack, replacing), (OnboardingReading reading) -> {
            hotPlug.offered(reading.pages().stream().map(page -> page.device().hardware()).toList());
            if (reading.problem() != null) {
                HudConfirmDialog.info(view, title, reading.problem(),
                        getText("bindings.aliasDesigner.firstSetup.close"));
                if (refreshAfter) refresh();
                return;
            }
            if (reading.pages().isEmpty()) {
                if (sayNobody) {
                    HudConfirmDialog.info(view, title, getText("bindings.aliasDesigner.onboarding.nobody"),
                            getText("bindings.aliasDesigner.firstSetup.close"));
                }
                if (refreshAfter) refresh();
                return;
            }
            boolean[] finished = {false};
            new OnboardingDialog(view, reading.pages(), reading.taken(), names -> {
                finished[0] = true;
                offEdt(title, () -> nameAll(reading.pages(), names), message ->
                        HudConfirmDialog.info(view, title, message, getText("bindings.aliasDesigner.firstSetup.close")));
            }).showDialog();
            // WHY: the dialog is modal, so this runs once it is closed - FINISH's edit has the screen read again.
            if (refreshAfter && !finished[0]) refresh();
        });
    }

    /**
     * Every name the user gave, in one edit: each controller's element, its default labels and the bindings that named
     * it by hardware id - an old id it replaces included (A17). Written at the next APPLY (Alan, 2026-10-23).
     *
     * @return what to tell the user
     */
    private static String nameAll(List<Onboarding.Page> pages, List<String> names) throws IOException {
        List<String> lines = new ArrayList<>();
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        Edits.stored().change(current -> {
            lines.clear();
            for (int i = 0; i < pages.size(); i++) {
                Onboarding.Page page = pages.get(i);
                String name = names.get(i);
                DeviceEdits.Changed changed = DeviceEdits.name(current, name, page.device().vid(),
                        page.device().pid(), page.labels(), page.hardwareIds(), stock);
                current = changed.files();
                lines.add(changed.bindingsMoved() > 0
                        ? getText("bindings.aliasDesigner.onboarding.result.namedMoves", name, changed.bindingsMoved())
                        : getText("bindings.aliasDesigner.onboarding.result.named", name));
            }
            return current;
        });
        log.info("Onboarding named {} controller(s) in the edits", pages.size());
        return String.join("\n", lines);
    }

    /**
     * Opens the Device Editor for one My Devices row. Before first-time startup there is no working copy for its edits
     * to change, so it says first-time startup comes first, from a click on the BindForge tab (Storage S2b).
     */
    void openDeviceEditor(MyDevice device) {
        if (!activated()) {
            HudConfirmDialog.info(view, getText("bindings.aliasDesigner.editor.heading"),
                    getText("bindings.aliasDesigner.editor.beforeSetUp.message"),
                    getText("bindings.aliasDesigner.firstSetup.close"));
            return;
        }
        new Thread(() -> {
            DeviceEditorDialog.Opening opening = null;
            String failure = null;
            try {
                opening = reader.openingFor(device);
            } catch (IOException | RuntimeException e) {
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
                new DeviceEditorDialog(view, loaded, () -> openOnboarding(device)).showDialog();
            });
        }, "BindForge-DeviceEditorRead").start();
    }

    // ------------------------------------------------------------------ running a change

    /**
     * Reads or changes the edits off the EDT, with no second change started meanwhile, then hands the result back on
     * it. A failure is shown under {@code title}, not only logged - the user pressed a button and must be told.
     */
    private <T> void offEdt(String title, Callable<T> work, Consumer<T> onEdt) {
        actionRunning = true;
        new Thread(() -> {
            T result = null;
            String failure = null;
            try {
                result = work.call();
            } catch (Edits.WriteRunningException e) {
                // WHY: a write is running - said in the user's language, not the exception's.
                log.info("{} not done: {}", title, e.getMessage());
                failure = getText("bindings.writeRunning");
            } catch (Exception e) {
                // WHY: broad on purpose - a thread boundary, and anything escaping it would leave the screen waiting
                // with no word of why.
                log.warn("{} failed", title, e);
                failure = messageOf(e);
            }
            T loaded = result;
            String loadedFailure = failure;
            SwingUtilities.invokeLater(() -> {
                actionRunning = false;
                if (loadedFailure != null) {
                    HudConfirmDialog.info(view, title, getText("bindings.aliasDesigner.editor.actionFailed",
                            loadedFailure), getText("bindings.aliasDesigner.firstSetup.close"));
                    refresh();
                    return;
                }
                onEdt.accept(loaded);
            });
        }, "BindForge-AliasDesignerChange").start();
    }
}
