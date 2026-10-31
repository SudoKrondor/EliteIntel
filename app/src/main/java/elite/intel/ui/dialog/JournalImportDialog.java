package elite.intel.ui.dialog;

import elite.intel.gameapi.journalimport.ImportAlreadyRunningException;
import elite.intel.gameapi.journalimport.ImportCancelledException;
import elite.intel.gameapi.journalimport.ImportReport;
import elite.intel.gameapi.journalimport.JournalImporter;
import elite.intel.gameapi.journalimport.JournalImporter.Phase;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.HUD_GAP;

/**
 * Imports journals the commander backed up - folders, zip, 7z or tar archives - so the app learns from them.
 * <p>
 * The commander adds as many folders and archives as they like, in any mix, and presses Import. Three bars follow
 * the import through unpacking, checking and learning, and the dialog ends on a summary of what was learned and
 * what was passed over. Closing or cancelling stops the import at the next step and keeps everything learned so far.
 * Use {@link #open} as the entry point.
 */
public final class JournalImportDialog extends JDialog {

    private static final Logger log = LogManager.getLogger(JournalImportDialog.class);

    /**
     * What the file chooser offers besides folders. The import decides by content, not name - this only keeps the
     * chooser from listing every screenshot; "All files" is still there for a backup with an odd extension.
     */
    private static final Set<String> CHOOSER_EXTENSIONS = Set.of(
            "zip", "7z", "tar", "tgz", "gz", "bz2", "tbz", "tbz2", "xz", "txz", "log");

    private final DefaultListModel<Path> sources = new DefaultListModel<>();
    private final AtomicBoolean cancelRequested = new AtomicBoolean();

    private JList<Path> sourceList;
    private JButton addButton;
    private JButton removeButton;
    private JButton importButton;
    private JButton closeButton;
    private JPanel progressSection;
    private HudStatCell collectingCell;
    private HudStatCell checkingCell;
    private HudStatCell readingCell;
    private JPanel resultHolder;

    private boolean running;
    private boolean closeWhenStopped;

    private JournalImportDialog(Component parent) {
        super(SwingUtilities.getWindowAncestor(parent), getText("journalImport.title"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        buildUi();
        pack();
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(parent);
    }

    public static void open(Component parent) {
        JournalImportDialog dialog = new JournalImportDialog(parent);
        AppTheme.runWithModalScrim(SwingUtilities.getWindowAncestor(parent), () -> dialog.setVisible(true));
    }

    private void buildUi() {
        JPanel page = AppTheme.transparentPanel(null);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        HudBanner intro = HudBanner.multiline(getText("journalImport.intro"), StatusBadge.State.INFO);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(intro);
        page.add(Box.createVerticalStrut(HUD_GAP));

        page.add(buildSourcesSection());
        page.add(Box.createVerticalStrut(HUD_GAP));

        collectingCell = new HudStatCell(getText("journalImport.progress.collecting"), "/images/file-recycle.png",
                HudPalette.HUD_COLOR_ROLE_INFORMATION, null);
        checkingCell = new HudStatCell(getText("journalImport.progress.checking"), "/images/stats.png",
                HudPalette.HUD_COLOR_ROLE_INFORMATION, null);
        readingCell = new HudStatCell(getText("journalImport.progress.reading"), "/images/manual.png",
                HudPalette.HUD_COLOR_ROLE_INFORMATION, null);
        progressSection = section(getText("journalImport.section.progress"),
                List.of(collectingCell, checkingCell, readingCell));
        progressSection.setVisible(false);
        page.add(progressSection);

        resultHolder = AppTheme.transparentPanel(new BorderLayout());
        resultHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(resultHolder);

        importButton = AppTheme.makeButton(getText("journalImport.start"));
        importButton.setEnabled(false);
        importButton.addActionListener(e -> startImport());
        closeButton = AppTheme.makeButtonSubtle(getText("journalImport.close"));
        closeButton.addActionListener(e -> cancelOrClose(false));

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("journalImport.title"))
                .onClose(() -> cancelOrClose(true))
                .body(page)
                .scrollBody(true)
                .primary(importButton)
                .dismiss(closeButton)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    private JComponent buildSourcesSection() {
        sourceList = new JList<>(sources);
        sourceList.setVisibleRowCount(5);
        sourceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sourceList.addListSelectionListener(e -> updateButtons());
        JScrollPane scroll = new JScrollPane(sourceList);
        scroll.setPreferredSize(new Dimension(600, 120));

        addButton = AppTheme.makeButton(getText("journalImport.add"));
        addButton.addActionListener(e -> chooseSources());
        removeButton = AppTheme.makeButtonSubtle(getText("journalImport.remove"));
        removeButton.setEnabled(false);
        removeButton.addActionListener(e -> removeSelected());
        JPanel buttons = AppTheme.transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 0));
        buttons.add(addButton);
        buttons.add(removeButton);

        HudSection section = HudSection.flat(getText("journalImport.section.sources"), new BorderLayout(0, HUD_GAP));
        section.body().add(scroll, BorderLayout.CENTER);
        section.body().add(buttons, BorderLayout.SOUTH);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    private static JPanel section(String title, List<HudStatCell> cells) {
        JPanel column = AppTheme.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        for (HudStatCell cell : cells) {
            cell.setAlignmentX(Component.LEFT_ALIGNMENT);
            column.add(cell);
        }
        HudSection section = HudSection.flat(title, new BorderLayout());
        section.body().add(column, BorderLayout.CENTER);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    // ---------------------------------------------------------------- choosing

    private void chooseSources() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("journalImport.chooser.title"));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(true);
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) return true;
                String name = file.getName().toLowerCase(Locale.ROOT);
                int dot = name.lastIndexOf('.');
                return dot >= 0 && CHOOSER_EXTENSIONS.contains(name.substring(dot + 1));
            }

            @Override
            public String getDescription() {
                return getText("journalImport.chooser.filter");
            }
        };
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File[] chosen = chooser.getSelectedFiles();
        if (chosen.length == 0 && chooser.getSelectedFile() != null) chosen = new File[]{chooser.getSelectedFile()};
        for (File file : chosen) {
            Path path = file.toPath().toAbsolutePath().normalize();
            if (!sources.contains(path)) sources.addElement(path);
        }
        resultHolder.removeAll();
        updateButtons();
        revalidate();
    }

    private void removeSelected() {
        for (Path path : sourceList.getSelectedValuesList()) {
            sources.removeElement(path);
        }
        updateButtons();
    }

    private void updateButtons() {
        addButton.setEnabled(!running);
        removeButton.setEnabled(!running && !sourceList.isSelectionEmpty());
        importButton.setEnabled(!running && !sources.isEmpty());
        closeButton.setText(getText(running ? "journalImport.cancel" : "journalImport.close"));
        closeButton.setEnabled(!running || !cancelRequested.get());
    }

    // ---------------------------------------------------------------- importing

    private void startImport() {
        List<Path> chosen = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) chosen.add(sources.get(i));

        running = true;
        cancelRequested.set(false);
        updateButtons();
        resultHolder.removeAll();
        for (HudStatCell cell : List.of(collectingCell, checkingCell, readingCell)) cell.setValue("0%", 0, "");
        progressSection.setVisible(true);
        revalidate();

        new SwingWorker<ImportReport, Runnable>() {
            private final long[] lastShown = new long[Phase.values().length];

            @Override
            protected ImportReport doInBackground() throws Exception {
                return JournalImporter.run(chosen, this::progress, cancelRequested::get);
            }

            /**
             * Called for every chunk the import reads, which is far more often than a bar can show. A phase's bar
             * moves when its whole percentage changes, and only then is a repaint queued.
             */
            private void progress(Phase phase, long done, long total) {
                int percent = total <= 0 ? 100 : (int) Math.min(100, done * 100 / total);
                if (lastShown[phase.ordinal()] == percent + 1) return;
                lastShown[phase.ordinal()] = percent + 1;
                HudStatCell cell = switch (phase) {
                    case COLLECTING -> collectingCell;
                    case CHECKING -> checkingCell;
                    case READING -> readingCell;
                };
                publish(() -> cell.setValue(percent + "%", percent / 100.0, ""));
            }

            @Override
            protected void process(List<Runnable> updates) {
                updates.forEach(Runnable::run);
            }

            @Override
            protected void done() {
                running = false;
                updateButtons();
                try {
                    ImportReport report = get();
                    for (HudStatCell cell : List.of(collectingCell, checkingCell, readingCell))
                        cell.setValue("100%", 1, "");
                    showResult(summary(report), report.journalsRead() == 0 ? StatusBadge.State.STANDBY : StatusBadge.State.OK);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    if (cause instanceof ImportCancelledException) {
                        showResult(getText("journalImport.status.cancelled"), StatusBadge.State.STANDBY);
                    } else if (cause instanceof ImportAlreadyRunningException) {
                        showResult(getText("journalImport.status.alreadyRunning"), StatusBadge.State.STANDBY);
                    } else {
                        log.error("Journal import failed", cause);
                        showResult(getText("journalImport.status.failed", String.valueOf(cause.getMessage())),
                                StatusBadge.State.OFFLINE);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (closeWhenStopped) dispose();
            }
        }.execute();
    }

    private void showResult(String text, StatusBadge.State state) {
        resultHolder.removeAll();
        resultHolder.add(Box.createVerticalStrut(HUD_GAP), BorderLayout.NORTH);
        resultHolder.add(HudBanner.multiline(text, state), BorderLayout.CENTER);
        resultHolder.revalidate();
        resultHolder.repaint();
    }

    /**
     * The summary, one line per thing worth saying: what was learned first, then what was passed over, each line
     * only when it is not zero.
     */
    static String summary(ImportReport report) {
        if (report.journalsRead() == 0) {
            List<String> lines = new ArrayList<>();
            lines.add(getText("journalImport.status.noJournals"));
            addSkipped(report, lines);
            return String.join("\n", lines);
        }
        List<String> lines = new ArrayList<>();
        lines.add(getText("journalImport.summary.read", report.journalsRead()));
        if (!report.commanders().isEmpty()) {
            lines.add(getText("journalImport.summary.commanders", String.join(", ", report.commanders())));
        }
        lines.add(getText("journalImport.summary.huntingGrounds", report.huntingGroundsLearned(), report.contractsLearned()));
        if (report.bioSamples() > 0) lines.add(getText("journalImport.summary.bioSamples", report.bioSamples()));
        if (report.bodiesScanned() > 0) lines.add(getText("journalImport.summary.bodies", report.bodiesScanned()));
        if (report.unownedJournals() > 0) lines.add(getText("journalImport.summary.unowned", report.unownedJournals()));
        addSkipped(report, lines);
        return String.join("\n", lines);
    }

    private static void addSkipped(ImportReport report, List<String> lines) {
        if (report.duplicates() > 0) lines.add(getText("journalImport.summary.duplicates", report.duplicates()));
        if (report.notJournals() > 0) lines.add(getText("journalImport.summary.notJournals", report.notJournals()));
        if (report.damaged() > 0) lines.add(getText("journalImport.summary.damaged", report.damaged()));
        if (report.unreadable() > 0) lines.add(getText("journalImport.summary.unreadable", report.unreadable()));
        if (report.unsupported() > 0) lines.add(getText("journalImport.summary.unsupported", report.unsupported()));
    }

    /**
     * Cancel stops the import and leaves the dialog up to say so; closing the window while the import runs stops it
     * and closes once it has stopped - its thread must not be left writing behind a dialog that is gone.
     */
    private void cancelOrClose(boolean closing) {
        if (!running) {
            dispose();
            return;
        }
        if (closing) closeWhenStopped = true;
        cancelRequested.set(true);
        updateButtons();
        showResult(getText("journalImport.status.cancelling"), StatusBadge.State.INFO);
    }
}
