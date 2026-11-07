package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.storage.*;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
import elite.intel.ui.event.BindForgeReadOnlyChangedEvent;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudFooter;
import elite.intel.util.OsDetector;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;

/**
 * The one APPLY and DISCARD, above BindForge's tabs and beneath Elite-Intel's main tabs (storage.md rule 7;
 * overview.md, <em>APPLY and DISCARD - one pair</em>; Storage S4a). No tab has its own. <strong>Both are lit only when
 * there are edits not applied</strong>, so their state is itself the hint that there is work to apply; default labels
 * alone do not light them.
 * <p>
 * APPLY asks first, with rule 13's warning and, on Linux with several installations, that bindings move only in the
 * one Elite-Intel uses; then checks the preset file, naming the sections it refuses for; then runs the check, warning
 * when the game's binds and preset files or an installation's device files differ ({@link CheckFlow}, Storage S8b,
 * S9b); then backs up, saves the edits on top of what the check settled into the working copy and writes it out
 * ({@link Apply}), and reports per place. DISCARD asks, then throws every edit away; no file is touched.
 * <p>
 * The leave prompt's Apply ({@link LeavePrompt}, storage.md rule 9) is this APPLY, with its confirmation and report,
 * told when it has finished so it can leave once the edits are gone.
 * <p>
 * <strong>CREATE sits at the bar's left end while the working copy holds no binds file of the user's</strong>
 * (storage.md rule 16; Storage S11): lit until it is pressed, dimmed while its file waits for APPLY, gone once APPLY has
 * written it. DISCARD lights it again. What it shows is worked out off the EDT, from the edits, and handed to
 * {@link #onCreateState} so BindForge's tabs can follow.
 * <p>
 * <strong>While BindForge is read only</strong> - an installation missing ({@link ReadOnlyMode}; storage.md, <em>An
 * installation missing - read only</em>; Storage S14a) - there are no edits, so the bar's left side names every missing
 * installation and the ways out, in warning colour, and <strong>OVERRIDE</strong> takes the place of DISCARD and APPLY.
 * CREATE is not shown. The bar is above the tabs, not an editor, so it may name installations (principle 12).
 */
public final class ApplyBar extends JPanel {

    private static final Logger log = LogManager.getLogger(ApplyBar.class);

    private final JButton apply = makeButton(getText("bindings.applyBar.apply"));
    private final JButton discard = makeButtonSubtle(getText("bindings.applyBar.discard"));
    private final JLabel status = AppTheme.hudReadoutValue(" ", HudPalette.HUD_COLOR_ROLE_WARNING);
    private final JButton create = makeButton(getText("bindings.create.button"));
    private final JButton override = makeButton(getText("bindings.readOnly.override"));
    private final RefreshCoalescer createRefresh = new RefreshCoalescer();
    private Create.State createState = Create.State.NOT_SET_UP;
    private Consumer<Create.State> createListener = state -> {
    };

    public ApplyBar() {
        super(new BorderLayout());
        setOpaque(false);
        apply.addActionListener(e -> apply(() -> {
        }));
        discard.addActionListener(e -> discard());
        create.addActionListener(e -> create());
        create.setVisible(false);
        override.addActionListener(e -> override());
        override.setVisible(false);
        JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, HudPalette.HUD_GAP, 0));
        leading.setOpaque(false);
        leading.add(create);
        leading.add(status);
        add(HudFooter.build(false, null, leading, List.of(discard, apply, override)), BorderLayout.CENTER);
        refresh();
        UiBus.register(this);
    }

    /**
     * Tells {@code listener} what CREATE shows now and whenever it changes - whether the editor tabs are usable. On the
     * EDT.
     */
    public void onCreateState(Consumer<Create.State> listener) {
        createListener = listener;
        listener.accept(createState);
        refreshCreate();
    }

    /**
     * Works out again what CREATE shows, off the EDT - the working copy may be read from disk. Before first-time
     * startup, or while the working copy cannot be read, CREATE is not shown: first-time startup and RESTORE come
     * first.
     */
    public void refreshCreate() {
        if (!createRefresh.tryStart()) return;
        Thread.ofPlatform().name("BindForge-CreateState").start(() -> {
            Create.State state;
            // WHY here: off the EDT, so the first ask in a run - which reads the stored list and the disk - never holds
            // the screen; the bar shows the answer when it comes.
            ReadOnlyMode.stored().workOut();
            try {
                state = Create.State.of(Edits.stored().snapshot());
            } catch (IOException e) {
                // WHY: no working copy yet, or one that cannot be read - the tab click offers first-time startup or
                // RESTORE for those, and CREATE would be an edit on nothing.
                log.debug("CREATE not offered: the working copy could not be read: {}", e.getMessage());
                state = Create.State.NOT_SET_UP;
            } catch (RuntimeException e) {
                // WHY: broad on purpose - a thread boundary; the bar must not stay without an answer.
                log.error("Could not work out whether to offer CREATE", e);
                state = Create.State.NOT_SET_UP;
            }
            Create.State found = state;
            SwingUtilities.invokeLater(() -> showCreate(found));
            if (createRefresh.finish()) refreshCreate();
        });
    }

    private void showCreate(Create.State state) {
        createState = state;
        create.setVisible(state.offered() && !ReadOnlyMode.stored().activeIfKnown());
        create.setEnabled(state.offered() && !state.editors() && !Edits.stored().writing());
        createListener.accept(state);
    }

    public void dispose() {
        UiBus.unregister(this);
    }

    @Subscribe
    public void onEditsChanged(BindForgeEditsChangedEvent event) {
        SwingUtilities.invokeLater(this::refresh);
        refreshCreate();
    }

    @Subscribe
    public void onReadOnlyChanged(BindForgeReadOnlyChangedEvent event) {
        SwingUtilities.invokeLater(() -> {
            refresh();
            showCreate(createState);
        });
    }

    /**
     * Lit when there are edits not applied and no write is running; the read-only message and OVERRIDE instead while
     * BindForge is read only. On the EDT; reads nothing from disk.
     */
    private void refresh() {
        boolean readOnly = ReadOnlyMode.stored().activeIfKnown();
        override.setVisible(readOnly);
        apply.setVisible(!readOnly);
        discard.setVisible(!readOnly);
        if (readOnly) {
            String message = readOnlyText(ReadOnlyMode.stored().missingIfKnown());
            status.setText(message);
            // WHY: the line may be cut short on a narrow window - the tooltip carries all of it.
            status.setToolTipText(message);
            return;
        }
        status.setToolTipText(null);
        Edits edits = Edits.stored();
        boolean writing = edits.writing();
        boolean lit = edits.hasEdits() && !writing;
        apply.setEnabled(lit);
        discard.setEnabled(lit);
        // WHY: a dimmed button that answers nothing says why, rather than leaving the user pressing it again.
        apply.setToolTipText(lit ? null : getText("bindings.applyBar.nothing"));
        status.setText(writing ? getText("bindings.applyBar.writing")
                : lit ? getText("bindings.applyBar.pending") : " ");
    }

    /** The bar's read-only line, naming every missing installation and the ways out. */
    static String readOnlyText(List<InstallationRow> missing) {
        String installations = missing.stream()
                .map(row -> getText("bindings.firstTimeStartup.place.installation", row.storefront(), row.rootPath()))
                .collect(Collectors.joining(", "));
        return getText("bindings.readOnly.bar", installations);
    }

    /**
     * OVERRIDE: asks first, then turns editing on until Elite-Intel closes (storage.md, <em>An installation missing -
     * read only</em>). A missing installation that comes back is compared and reconciled at the next start (rule 1).
     */
    private void override() {
        if (!HudConfirmDialog.confirm(this, getText("bindings.readOnly.override.title"),
                getText("bindings.readOnly.override.confirm"), getText("bindings.readOnly.override"),
                getText("button.cancel"))) {
            return;
        }
        // WHY no follow-up here: the BindForge tab hears read only end, whether by OVERRIDE or Remove, and offers what
        // it held back (Storage S14c).
        ReadOnlyMode.stored().override();
    }

    /** The leave prompt, asking over {@code parent}, whose Apply is this bar's APPLY. */
    public LeavePrompt leavePrompt(Component parent) {
        String title = getText("bindings.leave.title");
        return new LeavePrompt(LeavePrompt.EditsState.of(Edits.stored()),
                () -> switch (HudConfirmDialog.showWithoutDefault(parent, title, getText("bindings.leave.message"),
                        getText("bindings.applyBar.apply"), getText("bindings.applyBar.discard"),
                        getText("bindings.leave.goBack"))) {
                    case PRIMARY -> LeavePrompt.Choice.APPLY;
                    case EXTRA -> LeavePrompt.Choice.DISCARD;
                    case DISMISS -> LeavePrompt.Choice.GO_BACK;
                },
                this::apply,
                () -> HudConfirmDialog.info(parent, title, getText("bindings.writeRunning"), getText("button.close")));
    }

    /**
     * APPLY: the confirmation, then the write on its own thread, then the report.
     *
     * @param finished run on the EDT once it is over - the confirmation cancelled, or the report closed - whether or
     *                 not anything was written
     */
    void apply(Runnable finished) {
        if (!Edits.stored().hasEdits()) {
            finished.run();
            return;
        }
        String question = getText("bindings.applyBar.confirm");
        // WHY: told, not fixed (Alan, 2026-10-14, A11). On Linux each installation has its own bindings folder, and
        // BindForge moves bindings in the one Elite-Intel uses.
        if (OsDetector.getOs() == OsDetector.OS.LINUX
                && BindForgeInstallationsManager.getInstance().findAll().size() > 1) {
            question += "\n\n" + getText("bindings.applyBar.linuxNote");
        }
        String title = getText("bindings.applyBar.title");
        if (!HudConfirmDialog.confirm(this, title, question, getText("bindings.applyBar.apply"),
                getText("button.cancel"))) {
            finished.run();
            return;
        }
        apply.setEnabled(false);
        discard.setEnabled(false);
        Apply applying = Apply.stored(AliasDesignerReader::defaultLabels);
        ApplyWriting writing = new ApplyWriting(applying);
        CheckFlow check = new CheckFlow(this, writing, true);
        Runnable done = () -> {
            refresh();
            finished.run();
        };
        // WHY: the check runs after the confirmation - rule 7's order (Alan, 2026-10-30, Storage S8b) - and before
        // anything is backed up. With nothing to ask it writes straight away; otherwise the warning comes first.
        // A failure here is the write's (the check's own read is caught inside): nothing promised for a bug, which can
        // strike after some live copies were written - the log says what was (review, 2026-10-27).
        FlowSteps.inBackground("BindForge-Apply", log, "APPLY", () -> {
            BindingsCheck.Found found;
            try {
                found = applying.check();
            } catch (PresetRefusedException e) {
                log.warn("APPLY refused: {}", e.getMessage());
                return () -> FlowSteps.tell(this, title, presetRefusedText(e), done);
            } catch (IOException e) {
                // WHY: a file the check cannot read could hold the user's work, so nothing is written over it unseen
                // (Alan, 2026-10-30, Storage S8b). The edits are still on screen.
                log.warn("APPLY stopped: the check could not read the control files", e);
                return () -> FlowSteps.tell(this, title,
                        getText("bindings.applyBar.checkFailed", StringUtls.messageOf(e)), done);
            }
            return check.atApply(found, done);
        }, reason -> () -> FlowSteps.tell(this, title, writing.failed(reason), done),
                reason -> () -> FlowSteps.tell(this, title, writing.stopped(reason), done), done);
    }

    /** APPLY's write, once the check has what the user settled on. Off the EDT. */
    private record ApplyWriting(Apply applying) implements CheckFlow.Writing {
        @Override
        public String title() {
            return getText("bindings.applyBar.title");
        }

        @Override
        public String write(BindingsCheck.Found found, Map<String, BindingsComparison.Side> answers,
                            DeviceCheck.Answer devices) throws IOException {
            try {
                StringBuilder report = new StringBuilder(reportText(applying.apply(found, answers, devices)));
                report.append(CheckFlow.newerReport(found));
                devices.moves().forEach((from, to) -> report.append('\n')
                        .append(getText("bindings.aliasDesigner.firstSetup.summary.moves", from, to)));
                return report.toString();
            } catch (Backups.NothingToBackUpException e) {
                log.warn("APPLY wrote nothing: there was nothing to back up");
                return getText(Backups.NothingToBackUpException.ADVICE_KEY);
            } catch (Edits.RefusedException e) {
                // WHY: refused - another write running, or read only - nothing was written, and the edits are still on
                // screen.
                log.info("APPLY not run: {}", e.getMessage());
                return getText(e.messageKey());
            } catch (PresetRefusedException e) {
                // WHY: refused before anything was backed up or written - a line naming nothing real fails silently in
                // the game (preset-editor.md, What APPLY checks and writes for the preset file). Checked again on the
                // set written, since a merge can take the game's preset line.
                log.warn("APPLY refused: {}", e.getMessage());
                return presetRefusedText(e);
            }
        }

        /**
         * Each of these stops before any live copy is written - the game's files or the working copy unreadable, a
         * merge, the backup or the working copy's save failed - so the edits are still on screen.
         */
        @Override
        public String failed(String reason) {
            return getText("bindings.applyBar.failed", reason);
        }

        @Override
        public String stopped(String reason) {
            return getText("bindings.applyBar.stopped", reason);
        }
    }

    /**
     * Why APPLY refused the preset file. With an installation that could not be reached, that is named instead of
     * blaming the preset file alone - its factory binds files may be what a line names (Storage S6R, 2026-10-29).
     */
    static String presetRefusedText(PresetRefusedException refused) {
        if (refused.unreachable().isEmpty()) {
            return getText("bindings.applyBar.presetRefused", sectionNames(refused.sections()));
        }
        String installations = refused.unreachable().stream()
                .map(row -> getText("bindings.firstTimeStartup.place.installation", row.storefront(), row.rootPath()))
                .collect(Collectors.joining(", "));
        return getText("bindings.applyBar.presetUnreachable", installations, sectionNames(refused.sections()));
    }

    /** The sections in the user's language, in the file's order - <em>"Ship controls, SRV controls"</em>. */
    static String sectionNames(List<BindingSection> sections) {
        return sections.stream().map(section -> getText(section.getLabelKey())).collect(Collectors.joining(", "));
    }

    /** <em>"Updated 2 of 3"</em>, the backup, and each place that did not take everything (storage.md rule 8). */
    static String reportText(Written written) {
        WriteReport report = written.report();
        return getText("bindings.applyBar.result", written.backup().zip().toString(), report.installationsUpdated(),
                report.installations().size()) + FirstTimeStartupFlow.placeFailures(report)
                + CheckFlow.keptReadOnly(report);
    }

    /**
     * CREATE: the factory files offered, read off the EDT; the two questions; then the new file and the four preset lines
     * made one edit not applied (storage.md rule 16). Nothing is written to a file.
     */
    private void create() {
        String title = getText("bindings.create.title");
        Runnable done = () -> showCreate(createState);
        create.setEnabled(false);
        FlowSteps.inBackground("BindForge-Create", log, "CREATE", () -> {
            Create.Choices choices = Create.choices();
            if (choices.factories().isEmpty()) {
                log.warn("CREATE: no factory binds file found; {} installation(s) not reachable",
                        choices.unreachable().size());
                return () -> FlowSteps.tell(this, title, getText("bindings.create.noFactory"), done);
            }
            return () -> new CreateDialog(this, choices).showDialog().ifPresentOrElse(
                    answer -> made(answer, title, done), done);
        }, reason -> () -> FlowSteps.tell(this, title, getText("bindings.create.failed", reason), done),
                reason -> () -> FlowSteps.tell(this, title, getText("bindings.change.stopped", reason), done), done);
    }

    /** The user's answer made an edit: the factory file read and changed off the EDT. */
    private void made(CreateDialog.Answer answer, String title, Runnable done) {
        FlowSteps.inBackground("BindForge-Create", log, "CREATE", () -> {
            byte[] factory = Create.read(answer.factory());
            try {
                Edits.stored().change((current, applied) -> Create.create(current, applied, answer.name(), factory));
            } catch (Edits.RefusedException e) {
                log.info("CREATE not made: {}", e.getMessage());
                return () -> FlowSteps.tell(this, title, getText(e.messageKey()), done);
            }
            log.info("CREATE: {} from {}, an edit not applied", answer.name(), answer.factory().file());
            return null;
        }, reason -> () -> FlowSteps.tell(this, title, getText("bindings.create.failed", reason), done),
                reason -> () -> FlowSteps.tell(this, title, getText("bindings.change.stopped", reason), done), done);
    }

    private void discard() {
        if (!Edits.stored().hasEdits()) return;
        if (!HudConfirmDialog.confirm(this, getText("bindings.applyBar.discard.title"),
                getText("bindings.applyBar.discard.confirm"), getText("bindings.applyBar.discard"),
                getText("button.cancel"))) {
            return;
        }
        try {
            Edits.stored().discard();
        } catch (Edits.WriteRunningException e) {
            // WHY: an APPLY started while the question was open, and is writing these edits - once it has, there is
            // nothing left to throw away. The bar shows it running.
            log.info("DISCARD not done: {}", e.getMessage());
            refresh();
        }
    }
}
