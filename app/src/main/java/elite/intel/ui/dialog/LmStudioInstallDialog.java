package elite.intel.ui.dialog;

import elite.intel.ai.brain.inference.lmstudio.install.InstallFolder;
import elite.intel.ai.brain.inference.lmstudio.install.InstallOutcome;
import elite.intel.ai.brain.inference.lmstudio.install.InstallProgress;
import elite.intel.ai.brain.inference.lmstudio.install.InstallStep;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioHome;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioInstaller;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioSetupOffer;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.i18n.LocalizedNumbers;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.HUD_GAP;

/**
 * Installs LM Studio, sets up one already here, or continues the app's own install
 * (docs/LM_STUDIO_INSTALL_ASSISTANCE.md, "The install").
 * <p>
 * The commander sees what will happen, where LM Studio will go when it is a fresh install, and presses the button.
 * Each step then shows its state, the download shows its bytes, speed and time left, and the details log shows
 * everything the steps printed - the installer writes the same lines to the LM Studio log, so a support bundle carries
 * them, also after a restart. Closing or
 * cancelling while it runs asks first, then stops it; what was done is kept. Use {@link #open} as the entry point.
 */
public final class LmStudioInstallDialog extends JDialog {

    private static final Logger log = LogManager.getLogger(LmStudioInstallDialog.class);

    private static final double BYTES_PER_GB = 1024.0 * 1024 * 1024;
    private static final double BYTES_PER_MB = 1024.0 * 1024;

    /**
     * What the button runs. A fresh install that stops after recording its install turns into
     * {@link LmStudioSetupOffer#CONTINUE_SETUP} (see {@link #continueAfterFreshInstall}).
     */
    private LmStudioSetupOffer offer;
    /**
     * Whether the PC passed the GPU check on an AMD card: the dialog then says that is a beta.
     */
    private final boolean onAmd;
    private final LmStudioInstaller.FieldsSaver saver;
    private final Map<InstallStep, StatusBadge> badges = new EnumMap<>(InstallStep.class);

    private InstallFolder folder = InstallFolder.lmStudioDefault();
    private JComponent folderSection;
    private HudBanner intro;
    private JTextField folderField;
    private JLabel spaceLabel;
    private JButton changeFolderButton;
    private HudStatCell downloadCell;
    private JTextArea logArea;
    private JPanel resultHolder;
    private JButton startButton;
    private JButton closeButton;

    private volatile LmStudioInstaller installer;
    private InstallStep currentStep;
    private boolean running;
    private boolean stopping;
    private boolean closeWhenStopped;

    private LmStudioInstallDialog(Component parent, LmStudioSetupOffer offer, boolean onAmd,
                                  LmStudioInstaller.FieldsSaver saver) {
        super(SwingUtilities.getWindowAncestor(parent), getText(captionKey(offer)), ModalityType.APPLICATION_MODAL);
        this.offer = offer;
        this.onAmd = onAmd;
        this.saver = saver;
        setUndecorated(true);
        setMinimumSize(new Dimension(760, 0));
        buildUi();
        refit();
        setLocationRelativeTo(parent);
    }

    /**
     * Sizes the dialog to what it shows now: it grows as the download or a result appears.
     * <p>
     * WHY packed until the size holds: a wrapping banner measures its height against the width it was last laid out
     * at, and a new one, never laid out, measures as one unwrapped line. Each pass lays the banners out at the width
     * the one before settled on, so the size stops changing once every banner's height matches the width it is drawn
     * at - two or three passes. WHY no scrolling body either: inside a scroll pane the banners are measured at one
     * width and drawn at another, which cut the intro short.
     */
    private void refit() {
        for (int pass = 0; pass < 4; pass++) {
            Dimension before = getSize();
            // WHY: a container keeps the preferred size it measured while it is still valid, so without this a
            // second pass would reuse the first one's measure instead of asking the banners again.
            invalidateTree(getContentPane());
            pack();
            if (getSize().equals(before)) return;
        }
    }

    private static void invalidateTree(Container container) {
        for (Component child : container.getComponents()) {
            child.invalidate();
            if (child instanceof Container nested) invalidateTree(nested);
        }
    }

    /**
     * Opens the dialog for {@code offer} and returns once it is closed.
     *
     * @param onAmd whether the PC passed on an AMD card, which the dialog warns is a beta
     * @param saver writes the Address and Model the install ends with
     * @throws IllegalArgumentException for an offer that is not an install or a set-up
     */
    public static void open(Component parent, LmStudioSetupOffer offer, boolean onAmd,
                            LmStudioInstaller.FieldsSaver saver) {
        if (offer != LmStudioSetupOffer.INSTALL && offer != LmStudioSetupOffer.SET_UP_EXISTING
                && offer != LmStudioSetupOffer.CONTINUE_SETUP) {
            throw new IllegalArgumentException("The install dialog does not handle " + offer);
        }
        LmStudioInstallDialog dialog = new LmStudioInstallDialog(parent, offer, onAmd, saver);
        AppTheme.runWithModalScrim(SwingUtilities.getWindowAncestor(parent), () -> dialog.setVisible(true));
    }

    private static String captionKey(LmStudioSetupOffer offer) {
        return offer.captionKey().orElseThrow(() -> new IllegalArgumentException(offer + " has no caption"));
    }

    /**
     * The steps this kind of install runs. Setting up an LM Studio the app did not install never checks space or
     * installs anything.
     */
    static List<InstallStep> stepsFor(LmStudioSetupOffer offer) {
        List<InstallStep> all = Arrays.asList(InstallStep.values());
        return offer == LmStudioSetupOffer.SET_UP_EXISTING ? all.subList(InstallStep.START.ordinal(), all.size()) : all;
    }

    // ---------------------------------------------------------------- layout

    private void buildUi() {
        JPanel page = AppTheme.transparentPanel(null);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        intro = HudBanner.multiline(getText(introKey()), StatusBadge.State.INFO);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(intro);
        page.add(Box.createVerticalStrut(HUD_GAP));

        if (onAmd) {
            HudBanner amdBeta = HudBanner.multiline(getText("lmStudioInstall.amdBeta"), StatusBadge.State.STANDBY);
            amdBeta.setAlignmentX(Component.LEFT_ALIGNMENT);
            page.add(amdBeta);
            page.add(Box.createVerticalStrut(HUD_GAP));
        }

        if (offer == LmStudioSetupOffer.INSTALL) {
            folderSection = buildFolderSection();
            page.add(folderSection);
            page.add(Box.createVerticalStrut(HUD_GAP));
        }
        page.add(buildStepsSection());
        page.add(Box.createVerticalStrut(HUD_GAP));
        page.add(buildDetails());

        resultHolder = AppTheme.transparentPanel(new BorderLayout());
        resultHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(resultHolder);

        startButton = AppTheme.makeButton(getText(captionKey(offer)));
        startButton.addActionListener(e -> start());
        closeButton = AppTheme.makeButtonSubtle(getText("button.close"));
        closeButton.addActionListener(e -> requestStop(false));

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText(captionKey(offer)))
                .onClose(() -> requestStop(true))
                .body(page)
                .scrollBody(false)
                .primary(startButton)
                .dismiss(closeButton)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        if (offer == LmStudioSetupOffer.INSTALL) showFolder();
    }

    private String introKey() {
        return switch (offer) {
            case INSTALL -> "lmStudioInstall.intro.install";
            case SET_UP_EXISTING -> "lmStudioInstall.intro.setUp";
            case CONTINUE_SETUP -> "lmStudioInstall.intro.continue";
            case NONE, UNINSTALL -> throw new IllegalStateException("No install for " + offer);
        };
    }

    private JComponent buildFolderSection() {
        folderField = AppTheme.makeTextField();
        folderField.setEditable(false);
        changeFolderButton = AppTheme.makeButtonSubtle(getText("lmStudioInstall.folder.change"));
        changeFolderButton.addActionListener(e -> chooseFolder());
        JPanel row = AppTheme.transparentPanel(new BorderLayout(HUD_GAP, 0));
        row.add(folderField, BorderLayout.CENTER);
        row.add(changeFolderButton, BorderLayout.EAST);

        spaceLabel = AppTheme.hudReadoutValue("", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        JLabel note = AppTheme.hudReadoutValue(getText("lmStudioInstall.folder.note"),
                HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        JPanel notes = AppTheme.transparentPanel(new GridLayout(2, 1));
        notes.add(spaceLabel);
        notes.add(note);

        HudSection section = HudSection.flat(getText("lmStudioInstall.section.folder"), new BorderLayout(0, HUD_GAP));
        section.body().add(row, BorderLayout.NORTH);
        section.body().add(notes, BorderLayout.CENTER);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    private JComponent buildStepsSection() {
        JPanel rows = AppTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(2, 0, 2, HUD_GAP);
        gc.gridy = 0;
        for (InstallStep step : stepsFor(offer)) {
            StatusBadge badge = new StatusBadge(getText("lmStudioInstall.state.waiting"), StatusBadge.State.IDLE);
            badges.put(step, badge);
            gc.gridx = 0;
            gc.weightx = 0;
            rows.add(badge, gc);
            gc.gridx = 1;
            gc.weightx = 1;
            rows.add(AppTheme.hudReadoutValue(getText("lmStudioInstall.step." + step.name()),
                    HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT), gc);
            gc.gridy++;
        }

        downloadCell = new HudStatCell(getText("lmStudioInstall.download"), "/images/release.png",
                HudPalette.HUD_COLOR_ROLE_INFORMATION, null);
        downloadCell.setVisible(false);

        HudSection section = HudSection.flat(getText("lmStudioInstall.section.steps"), new BorderLayout(0, HUD_GAP));
        section.body().add(rows, BorderLayout.NORTH);
        section.body().add(downloadCell, BorderLayout.SOUTH);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    /**
     * The details log, always shown. WHY not behind a Show button: watching the steps print what they do is what
     * tells the commander a long install is working, and what it is doing to their PC.
     */
    private JComponent buildDetails() {
        logArea = AppTheme.makeTextArea(10, 60);
        logArea.setEditable(false);
        JScrollPane logScroll = AppTheme.hudScrollPane(logArea);
        logScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return logScroll;
    }

    // ---------------------------------------------------------------- the folder

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("lmStudioInstall.folder.chooser"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) return;
        Path chosen = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        folder = InstallFolder.inside(chosen);
        showFolder();
    }

    private void showFolder() {
        folderField.setText(folder.home().toString());
        String needed = gigabytes(InstallFolder.REQUIRED_BYTES);
        try {
            long free = folder.usableBytes();
            boolean enough = free >= InstallFolder.REQUIRED_BYTES;
            spaceLabel.setText(getText("lmStudioInstall.folder.space", gigabytes(free), needed));
            spaceLabel.setForeground(enough ? HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT : HudPalette.HUD_COLOR_ROLE_DANGER);
            startButton.setEnabled(enough);
        } catch (IOException e) {
            log.warn("Could not read the free space for {}", folder.home(), e);
            spaceLabel.setText(getText("lmStudioInstall.folder.space", "?", needed));
            spaceLabel.setForeground(HudPalette.HUD_COLOR_ROLE_DANGER);
            startButton.setEnabled(false);
        }
    }

    // ---------------------------------------------------------------- running

    private void start() {
        running = true;
        stopping = false;
        resultHolder.removeAll();
        logArea.setText("");
        badges.values().forEach(badge -> badge.setStatus(getText("lmStudioInstall.state.waiting"), StatusBadge.State.IDLE));
        updateButtons();
        installer = new LmStudioInstaller(new ToTheDialog(), saver);
        LmStudioInstaller started = installer;

        LmStudioSetupOffer offered = offer;
        new SwingWorker<Ended, Void>() {
            @Override
            protected Ended doInBackground() {
                InstallOutcome outcome = runOffer();
                // WHY read here: the record is in the database, which stays off the event thread.
                boolean recorded = offered == LmStudioSetupOffer.INSTALL
                        && !(outcome instanceof InstallOutcome.Completed)
                        && LmStudioSetupOffer.ownedInstall().isPresent();
                return new Ended(outcome, recorded);
            }

            private InstallOutcome runOffer() {
                return switch (offered) {
                    case INSTALL -> started.install(folder);
                    case SET_UP_EXISTING -> started.setUpExisting(LmStudioHome.locate());
                    case CONTINUE_SETUP -> LmStudioSetupOffer.ownedInstall()
                            .map(started::continueSetup)
                            .orElseGet(() -> new InstallOutcome.Failed(InstallStep.CHECK,
                                    "The install this would continue is no longer on this PC"));
                    case NONE, UNINSTALL -> throw new IllegalStateException("No install for " + offered);
                };
            }

            @Override
            protected void done() {
                running = false;
                try {
                    Ended ended = get();
                    if (ended.recorded()) continueAfterFreshInstall();
                    showOutcome(ended.outcome());
                } catch (ExecutionException e) {
                    log.error("LM Studio install ended unexpectedly", e.getCause());
                    showOutcome(new InstallOutcome.Failed(currentStep == null ? InstallStep.CHECK : currentStep,
                            String.valueOf(e.getCause().getMessage())));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                updateButtons();
                if (closeWhenStopped) dispose();
            }
        }.execute();
    }

    /**
     * How a run ended, and whether a fresh install that did not complete left its install recorded.
     */
    private record Ended(InstallOutcome outcome, boolean recorded) {
    }

    /**
     * Makes the button continue the install the last run recorded rather than start a fresh one. WHY: the fresh
     * install forgets that record and runs LM Studio's install script again - a Windows tester's retry after a failed
     * download re-downloaded LM Studio for two minutes, over the one already running (2026-11-10). The result banner
     * already says pressing the button again picks up from there. The folder is fixed from here on: the install is in
     * it.
     */
    private void continueAfterFreshInstall() {
        offer = LmStudioSetupOffer.CONTINUE_SETUP;
        startButton.setText(getText(captionKey(offer)));
        Container page = intro.getParent();
        int at = page.getComponentZOrder(intro);
        page.remove(intro);
        intro = HudBanner.multiline(getText(introKey()), StatusBadge.State.INFO);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(intro, at);
        if (folderSection != null) folderSection.setVisible(false);
        changeFolderButton = null;
        refit();
    }

    private void showOutcome(InstallOutcome outcome) {
        switch (outcome) {
            case InstallOutcome.Completed() -> {
                showResult(getText("lmStudioInstall.result.done"), StatusBadge.State.OK);
                startButton.setVisible(false);
            }
            case InstallOutcome.Failed(InstallStep step, String reason) -> {
                markStep(step, "lmStudioInstall.state.failed", StatusBadge.State.OFFLINE);
                showResult(getText("lmStudioInstall.result.failed", getText("lmStudioInstall.step." + step.name()), reason)
                        + "\n" + getText("lmStudioInstall.result.tryAgain"), StatusBadge.State.OFFLINE);
            }
            case InstallOutcome.Cancelled() -> {
                if (currentStep != null) markStep(currentStep, "lmStudioInstall.state.stopped", StatusBadge.State.STANDBY);
                showResult(getText("lmStudioInstall.result.cancelled"), StatusBadge.State.STANDBY);
            }
        }
    }

    private void showResult(String text, StatusBadge.State state) {
        resultHolder.removeAll();
        resultHolder.add(Box.createVerticalStrut(HUD_GAP), BorderLayout.NORTH);
        HudBanner banner = HudBanner.multiline(text, state);
        // WHY laid out at the width it will have: otherwise it first measures as one long line and the dialog jumps
        // that wide for a frame before refit settles it.
        banner.setSize(resultHolder.getWidth(), Short.MAX_VALUE);
        banner.doLayout();
        resultHolder.add(banner, BorderLayout.CENTER);
        refit();
    }

    private void updateButtons() {
        startButton.setEnabled(!running);
        if (changeFolderButton != null) changeFolderButton.setEnabled(!running);
        closeButton.setText(getText(running ? "button.cancel" : "button.close"));
        closeButton.setEnabled(!stopping);
    }

    /**
     * Cancel asks, then stops the install and leaves the dialog up to say so. Closing the window asks too, and closes
     * once it has stopped - the install's thread must not be left saving settings behind a dialog that is gone.
     */
    private void requestStop(boolean closing) {
        if (!running) {
            dispose();
            return;
        }
        if (stopping) return;
        boolean confirmed = HudConfirmDialog.confirm(this,
                getText("lmStudioInstall.stop.title"),
                getText("lmStudioInstall.stop.message"),
                getText("lmStudioInstall.stop.confirm"),
                getText("lmStudioInstall.stop.keepGoing"));
        if (!confirmed || !running) return;
        stopping = true;
        if (closing) closeWhenStopped = true;
        updateButtons();
        showResult(getText("lmStudioInstall.result.stopping"), StatusBadge.State.INFO);
        installer.cancel();
    }

    private void markStep(InstallStep step, String stateKey, StatusBadge.State state) {
        StatusBadge badge = badges.get(step);
        if (badge != null) badge.setStatus(getText(stateKey), state);
    }

    private void showDownload(long downloaded, long total, double bytesPerSecond) {
        String speed = String.format(locale(), "%.0f", bytesPerSecond / BYTES_PER_MB);
        String minutesLeft = bytesPerSecond > 0
                ? String.valueOf((long) Math.ceil((total - downloaded) / bytesPerSecond / 60))
                : "-";
        double fraction = total > 0 ? (double) downloaded / total : 0;
        downloadCell.setValue(getText("lmStudioInstall.download.value", gigabytes(downloaded), gigabytes(total),
                speed, minutesLeft), fraction, Math.round(fraction * 100) + "%");
        if (!downloadCell.isVisible()) {
            downloadCell.setVisible(true);
            refit();
        }
    }

    private void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static String gigabytes(long bytes) {
        return String.format(locale(), "%.1f", bytes / BYTES_PER_GB);
    }

    /**
     * WHY not the JVM's default: figures the commander reads follow the language chosen in the app.
     */
    private static Locale locale() {
        return LocalizedNumbers.locale();
    }

    /**
     * The installer's reports, handed to the event thread. Log lines also go to the in-app system log.
     */
    private final class ToTheDialog implements InstallProgress {

        @Override
        public void stepStarted(InstallStep step) {
            SwingUtilities.invokeLater(() -> {
                currentStep = step;
                markStep(step, "lmStudioInstall.state.running", StatusBadge.State.INFO);
            });
        }

        @Override
        public void stepSkipped(InstallStep step) {
            SwingUtilities.invokeLater(() -> markStep(step, "lmStudioInstall.state.alreadyDone", StatusBadge.State.OK));
        }

        @Override
        public void stepFinished(InstallStep step) {
            SwingUtilities.invokeLater(() -> markStep(step, "lmStudioInstall.state.done", StatusBadge.State.OK));
        }

        @Override
        public void log(String line) {
            UiBus.publish(new AppLogEvent("LM Studio setup: " + line));
            SwingUtilities.invokeLater(() -> appendLog(line));
        }

        @Override
        public void download(long downloadedBytes, long totalBytes, double bytesPerSecond) {
            SwingUtilities.invokeLater(() -> showDownload(downloadedBytes, totalBytes, bytesPerSecond));
        }
    }
}
