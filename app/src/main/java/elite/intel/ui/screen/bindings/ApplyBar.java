package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.storage.*;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
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
 * one Elite-Intel uses; then checks the preset file, naming the sections it refuses for; then backs up, saves the
 * edits into the working copy and writes it out ({@link Apply}), and reports per place. DISCARD asks, then throws
 * every edit away; no file is touched.
 */
public final class ApplyBar extends JPanel {

    private static final Logger log = LogManager.getLogger(ApplyBar.class);

    private final JButton apply = makeButton(getText("bindings.applyBar.apply"));
    private final JButton discard = makeButtonSubtle(getText("bindings.applyBar.discard"));
    private final JLabel status = AppTheme.hudReadoutValue(" ", HudPalette.HUD_COLOR_ROLE_WARNING);

    public ApplyBar() {
        super(new BorderLayout());
        setOpaque(false);
        apply.addActionListener(e -> apply());
        discard.addActionListener(e -> discard());
        add(HudFooter.build(false, null, status, List.of(discard, apply)), BorderLayout.CENTER);
        refresh();
        UiBus.register(this);
    }

    public void dispose() {
        UiBus.unregister(this);
    }

    @Subscribe
    public void onEditsChanged(BindForgeEditsChangedEvent event) {
        SwingUtilities.invokeLater(this::refresh);
    }

    /** Lit when there are edits not applied and no write is running. On the EDT; reads nothing from disk. */
    private void refresh() {
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

    private void apply() {
        if (!Edits.stored().hasEdits()) return;
        String question = getText("bindings.applyBar.confirm");
        // WHY: told, not fixed (Alan, 2026-10-14, A11). On Linux each installation has its own bindings folder, and
        // BindForge moves bindings in the one Elite-Intel uses.
        if (OsDetector.getOs() == OsDetector.OS.LINUX
                && BindForgeInstallationsManager.getInstance().findAll().size() > 1) {
            question += "\n\n" + getText("bindings.applyBar.linuxNote");
        }
        String title = getText("bindings.applyBar.title");
        if (!HudConfirmDialog.confirm(this, title, question, getText("bindings.applyBar.apply"),
                getText("bindings.aliasDesigner.firstSetup.cancel"))) {
            return;
        }
        apply.setEnabled(false);
        discard.setEnabled(false);
        new Thread(() -> {
            String message;
            try {
                message = reportText(Apply.stored(AliasDesignerReader::defaultLabels).apply());
            } catch (Backups.NothingToBackUpException e) {
                log.warn("APPLY wrote nothing: there was nothing to back up");
                message = getText(Backups.NothingToBackUpException.ADVICE_KEY);
            } catch (Edits.WriteRunningException e) {
                // WHY: RESTORE or first-time startup is writing - nothing was written, and the edits are still on screen.
                log.info("APPLY not run: {}", e.getMessage());
                message = getText("bindings.writeRunning");
            } catch (PresetRefusedException e) {
                // WHY: refused before anything was backed up or written - a line naming nothing real fails silently in
                // the game (preset-editor.md, What APPLY checks and writes for the preset file).
                log.warn("APPLY refused: {}", e.getMessage());
                message = presetRefusedText(e);
            } catch (IOException e) {
                // WHY: each of these stops before any live copy is written - the working copy unreadable, the backup
                // or the working copy's save failed - so the edits are still on screen.
                log.error("APPLY stopped before writing the game files", e);
                message = getText("bindings.applyBar.failed", StringUtls.messageOf(e));
            } catch (RuntimeException e) {
                // WHY: broad on purpose - a thread boundary. A bug can strike after the working copy was saved and the
                // edits handed over, with some live copies written, so nothing is promised either way: the log says
                // what was written (review, 2026-10-27).
                log.error("APPLY stopped part way", e);
                message = getText("bindings.applyBar.stopped", StringUtls.messageOf(e));
            }
            String told = message;
            SwingUtilities.invokeLater(() -> {
                refresh();
                HudConfirmDialog.info(this, title, told, getText("bindings.aliasDesigner.firstSetup.close"));
            });
        }, "BindForge-Apply").start();
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
                report.installations().size()) + FirstTimeStartupFlow.placeFailures(report);
    }

    private void discard() {
        if (!Edits.stored().hasEdits()) return;
        if (!HudConfirmDialog.confirm(this, getText("bindings.applyBar.discard.title"),
                getText("bindings.applyBar.discard.confirm"), getText("bindings.applyBar.discard"),
                getText("bindings.aliasDesigner.firstSetup.cancel"))) {
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
