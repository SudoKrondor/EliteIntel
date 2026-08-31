package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.JukeboxDao;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.eventbus.UiBus;
import elite.intel.jukebox.*;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.JukeboxPlaylistChangedEvent;
import elite.intel.ui.event.JukeboxStateChangedEvent;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The Jukebox tab: the commander's own music, played underneath the companion rather than over it.
 * <p>
 * <b>Why the volume lives here and not on the Audio settings tab.</b> That tab governs the companion - its
 * voice, its microphone, its alert beeps. Music is the commander's, and putting its level among the
 * companion's would invite turning the wrong one down.
 * <p>
 * The playlist is a plain list in an order the commander sets by dragging, so the table deliberately does
 * not sort by clicking a column header: a click-sort would silently throw away an order they arranged by
 * hand. Sorting is offered in the context menu instead, where it is an explicit, saved rewrite of that
 * order.
 */
public class JukeboxTabPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(JukeboxTabPanel.class);

    private static final int COLUMN_NUMBER = 0;
    private static final int COLUMN_TITLE = 1;
    private static final int COLUMN_ARTIST = 2;
    private static final int COLUMN_ALBUM = 3;
    private static final int COLUMN_DURATION = 4;

    /**
     * How often the play-head is re-read while a track runs. Twice a second: a second hand that never
     * looks stuck, for two field reads.
     */
    private static final int PLAY_HEAD_REFRESH_MS = 500;

    private final JukeboxManager library = JukeboxManager.getInstance();
    private final JukeboxPlayer player = JukeboxPlayer.getInstance();

    private final PlaylistModel model = new PlaylistModel();
    private JTable playlist;
    private JTextField folderField;
    private JLabel statusLabel;
    private JButton playPauseButton;
    private HudSlider volumeSlider;
    private HudSlider seekBar;
    private HudSegmentedControl orderControl;

    private Long nowPlayingId;
    private long nowPlayingDurationMs;
    private boolean followingThePlayer;

    public JukeboxTabPanel() {
        setLayout(new BorderLayout(0, HudPalette.HUD_GAP));
        setBorder(BorderFactory.createEmptyBorder(HudPalette.HUD_GAP, HudPalette.HUD_GAP,
                HudPalette.HUD_GAP, HudPalette.HUD_GAP));
        setOpaque(false);

        add(buildLibrarySection(), BorderLayout.NORTH);
        add(buildPlaylistSection(), BorderLayout.CENTER);
        add(buildPlaybackSection(), BorderLayout.SOUTH);

        UiBus.register(this);
        player.start();
        TagScanner.getInstance().start();
        refreshPlaylist();
        syncFromPlayer();
    }

    // ---------------------------------------------------------------- layout

    /**
     * Top: where the music comes from.
     */
    private JComponent buildLibrarySection() {
        HudSection section = HudSection.flat(getText("jukebox.section.library"), new BorderLayout(HudPalette.HUD_GAP, 0));
        folderField = AppTheme.makeMetadataField();
        folderField.setText(library.musicFolder().orElse(getText("jukebox.noFolder")));

        JButton browse = AppTheme.makeButton(getText("jukebox.browse"));
        browse.addActionListener(e -> chooseFolder());
        JButton rescan = AppTheme.makeButtonSubtle(getText("jukebox.rescan"));
        rescan.addActionListener(e -> library.musicFolder().ifPresent(folder -> scanInto(Path.of(folder))));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, HudPalette.HUD_GAP, 0));
        buttons.setOpaque(false);
        buttons.add(rescan);
        buttons.add(browse);

        section.body().add(folderField, BorderLayout.CENTER);
        section.body().add(buttons, BorderLayout.EAST);
        return section;
    }

    /**
     * Middle: the playlist, which takes all the room there is.
     */
    private JComponent buildPlaylistSection() {
        HudSection section = HudSection.flat(getText("jukebox.section.playlist"), new BorderLayout(0, HudPalette.HUD_GAP));

        playlist = new JTable(model);
        HudTable.styleCompact(playlist);
        playlist.setAutoCreateRowSorter(false);
        playlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        playlist.setDefaultRenderer(Object.class, new TrackCellRenderer());
        playlist.getColumnModel().getColumn(COLUMN_NUMBER).setMaxWidth(56);
        playlist.getColumnModel().getColumn(COLUMN_DURATION).setMaxWidth(80);
        playlist.addMouseListener(new PlaylistMouse());
        installDragToReorder();

        statusLabel = AppTheme.hudReadoutValue(" ", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);

        section.body().add(new HudScrollPane(playlist), BorderLayout.CENTER);
        section.body().add(statusLabel, BorderLayout.SOUTH);
        return section;
    }

    /**
     * Bottom: the play-head, the transport, the order, and the music's own volume.
     */
    private JComponent buildPlaybackSection() {
        HudSection section = HudSection.flat(getText("jukebox.section.playback"),
                new BorderLayout(HudPalette.HUD_GAP, HudPalette.HUD_GAP));

        JPanel transport = new JPanel(new FlowLayout(FlowLayout.LEFT, HudPalette.HUD_GAP, 0));
        transport.setOpaque(false);
        transport.add(transportButton(getText("jukebox.previous"), player::previous));
        playPauseButton = transportButton(getText("jukebox.play"), player::togglePlayPause);
        setPlayPauseLabel(false);
        transport.add(playPauseButton);
        transport.add(transportButton(getText("jukebox.stop"), player::stop));
        transport.add(transportButton(getText("jukebox.next"), player::next));

        orderControl = new HudSegmentedControl(
                new String[]{getText("jukebox.order.sequential"), getText("jukebox.order.random")},
                library.playbackOrder() == PlaybackOrder.RANDOM ? 1 : 0);
        orderControl.addChangeListener(e -> player.setPlaybackOrder(
                orderControl.getSelectedIndex() == 1 ? PlaybackOrder.RANDOM : PlaybackOrder.SEQUENTIAL));

        volumeSlider = new HudSlider(0, 100, 1, library.volume());
        volumeSlider.addChangeListener(e -> player.setVolume(volumeSlider.getValue()));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, HudPalette.HUD_GAP, 0));
        right.setOpaque(false);
        right.add(AppTheme.hudReadoutLabel(getText("jukebox.order")));
        right.add(orderControl);
        right.add(AppTheme.hudReadoutLabel(getText("jukebox.volume")));
        volumeSlider.setPreferredSize(new Dimension(200, volumeSlider.getPreferredSize().height));
        right.add(volumeSlider);

        section.body().add(buildSeekBar(), BorderLayout.NORTH);
        section.body().add(transport, BorderLayout.WEST);
        section.body().add(right, BorderLayout.EAST);
        return section;
    }

    /**
     * The play-head: how far into the track playback has got, and where the commander can put it.
     * <p>
     * WHY the seek waits for the thumb to be let go: moving the play-head reopens the file and winds it
     * forward to the target, so acting on every step of a drag would ask the decoder for fifty jumps the
     * commander never wanted to hear. Nothing is played while the thumb moves - they land where they let
     * go, which is what a seek bar is for, and cheaper than scrubbing for the same result.
     */
    private JComponent buildSeekBar() {
        seekBar = new HudSlider(0, 0, 1, 0);
        seekBar.setValueFormatter(this::formatPlayHead);
        seekBar.setEnabled(false);
        seekBar.addChangeListener(e -> {
            if (followingThePlayer || seekBar.isAdjusting()) return;
            player.seekTo(seekBar.getValue() * 1000L);
        });
        new javax.swing.Timer(PLAY_HEAD_REFRESH_MS, e -> followPlayHead()).start();
        return seekBar;
    }

    /**
     * Reads the play-head off the player and moves the bar to it.
     * <p>
     * WHY it is polled rather than pushed: the position moves hundreds of times a second, and an event for
     * each would be hundreds of events a second to say something the bar can simply ask for when it is
     * about to repaint. It asks for nothing at all while the tab is out of sight.
     */
    private void followPlayHead() {
        if (!isShowing() || seekBar.isAdjusting()) return;
        moveSeekBar(() -> seekBar.setValue((int) (player.positionMs() / 1000)));
    }

    /**
     * Sizes the bar to the track now playing and points it at the current position.
     * <p>
     * A file the tag reader has not been over yet has no known length, and a bar with a made-up end would
     * drop the play-head somewhere the commander did not choose - so it stays disabled until the length is
     * known, which is the same rule the playlist's Time column follows.
     */
    private void syncSeekBarToTrack() {
        JukeboxDao.Track track = nowPlayingId == null ? null : model.trackAt(model.rowOf(nowPlayingId));
        Long durationMs = track == null ? null : track.getDurationMs();
        nowPlayingDurationMs = durationMs == null ? 0 : durationMs;
        moveSeekBar(() -> {
            seekBar.setMaximum((int) (nowPlayingDurationMs / 1000));
            seekBar.setValue((int) (player.positionMs() / 1000));
        });
        seekBar.setEnabled(nowPlayingDurationMs > 0);
    }

    /**
     * Moves the bar to follow the player rather than the other way round, so the change it fires is not
     * mistaken for the commander asking to seek.
     */
    private void moveSeekBar(Runnable move) {
        followingThePlayer = true;
        try {
            move.run();
        } finally {
            followingThePlayer = false;
        }
    }

    private String formatPlayHead(int seconds) {
        if (nowPlayingDurationMs <= 0) return "--:--";
        return formatClock(seconds) + " / " + formatClock(nowPlayingDurationMs / 1000);
    }

    private static String formatClock(long totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private JButton transportButton(String label, Runnable action) {
        JButton button = AppTheme.makeButton(label);
        button.addActionListener(e -> action.run());
        return button;
    }

    // ---------------------------------------------------------------- the library

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(getText("jukebox.chooseFolder"));
        library.musicFolder().ifPresent(folder -> chooser.setCurrentDirectory(new File(folder)));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path chosen = chooser.getSelectedFile().toPath();
        library.setMusicFolder(chosen.toString());
        folderField.setText(chosen.toString());
        scanInto(chosen);
    }

    /**
     * Walks a folder and adds what it finds.
     * <p>
     * WHY off the event thread: a music library runs to thousands of files across a spinning disk or a
     * network share, and walking it on the event thread freezes the whole window while it does.
     */
    private void scanInto(Path folder) {
        statusLabel.setText(getText("jukebox.scanning"));
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws IOException {
                return library.add(MusicFolderScanner.findTracks(folder));
            }

            @Override
            protected void done() {
                try {
                    int added = get();
                    statusLabel.setText(added == 0
                            ? getText("jukebox.noTracksFound")
                            : getText("jukebox.tracksAdded", added));
                    if (added > 0) TagScanner.getInstance().requestScan();
                } catch (Exception e) {
                    log.warn("Could not scan music folder {}: {}", folder, e.getMessage());
                    statusLabel.setText(getText("jukebox.folderUnreadable"));
                }
                refreshPlaylist();
            }
        }.execute();
    }

    // ---------------------------------------------------------------- the playlist

    @Subscribe
    public void onJukeboxPlaylistChanged(JukeboxPlaylistChangedEvent event) {
        SwingUtilities.invokeLater(this::refreshPlaylist);
    }

    /**
     * Rebuilds the table from the stored playlist, keeping whatever was selected selected.
     * <p>
     * WHY the selection is restored by track rather than by row: the tag scanner refreshes the table
     * underneath the commander every batch, and a plain rebuild would drop their selection several times
     * a second while a big library filled in - taking the right-click menu's target with it.
     */
    private void refreshPlaylist() {
        Set<Long> selected = selectedTrackIds();
        model.replaceWith(library.playlist());
        restoreSelection(selected);
        updateEmptyHint();
        // The tag reader fills the durations in behind the commander, and the bar cannot open until the
        // length of what is playing arrives.
        syncSeekBarToTrack();
    }

    private Set<Long> selectedTrackIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (int row : playlist.getSelectedRows()) {
            JukeboxDao.Track track = model.trackAt(row);
            if (track != null) ids.add(track.getId());
        }
        return ids;
    }

    private void restoreSelection(Set<Long> ids) {
        if (ids.isEmpty()) return;
        playlist.clearSelection();
        for (Long id : ids) {
            int row = model.rowOf(id);
            if (row >= 0) playlist.addRowSelectionInterval(row, row);
        }
    }

    private void updateEmptyHint() {
        if (model.getRowCount() == 0) {
            statusLabel.setText(getText("jukebox.empty"));
        }
    }

    private List<JukeboxDao.Track> selectedTracks() {
        List<JukeboxDao.Track> chosen = new ArrayList<>();
        for (int row : playlist.getSelectedRows()) {
            chosen.add(model.trackAt(row));
        }
        return chosen;
    }

    private void showContextMenu(MouseEvent event, int row) {
        if (row >= 0 && !playlist.isRowSelected(row)) {
            playlist.setRowSelectionInterval(row, row);
        }
        List<JukeboxDao.Track> chosen = selectedTracks();
        JPopupMenu menu = new JPopupMenu();

        if (!chosen.isEmpty()) {
            menu.add(item(getText("jukebox.menu.playNow"), () -> player.playTrack(chosen.get(0).getId())));
            menu.add(item(getText("jukebox.menu.playNext"), () -> queueNext(chosen)));
            menu.addSeparator();
            menu.add(item(getText("jukebox.menu.remove"), () -> removeSelected(chosen)));
            menu.add(item(getText("jukebox.menu.reveal"), () -> revealInFileManager(chosen.get(0))));
            menu.add(item(getText("jukebox.menu.copy"), () -> copyToClipboard(chosen)));
            menu.addSeparator();
        }
        menu.add(item(getText("jukebox.menu.addFolder"), this::chooseFolder));
        menu.add(item(getText("jukebox.menu.importPlaylist"), this::importPlaylist));
        menu.add(item(getText("jukebox.menu.removeDead"), this::removeDeadEntries));
        menu.add(sortMenu());
        menu.addSeparator();
        menu.add(item(getText("jukebox.menu.clear"), this::clearPlaylist));
        menu.show(playlist, event.getX(), event.getY());
    }

    private JMenu sortMenu() {
        JMenu sort = new JMenu(getText("jukebox.menu.sortBy"));
        sort.add(item(getText("jukebox.menu.sort.title"),
                () -> sortBy(Comparator.comparing(track -> track.displayTitle().toLowerCase(Locale.ROOT)))));
        sort.add(item(getText("jukebox.menu.sort.artist"),
                () -> sortBy(Comparator.comparing(track -> text(track.getArtist()).toLowerCase(Locale.ROOT)))));
        sort.add(item(getText("jukebox.menu.sort.folder"),
                () -> sortBy(Comparator.comparing(track -> text(track.getPath()).toLowerCase(Locale.ROOT)))));
        return sort;
    }

    private void sortBy(Comparator<JukeboxDao.Track> comparator) {
        library.sort(comparator);
        refreshPlaylist();
    }

    /**
     * Moves the chosen tracks so they play straight after the one playing now.
     * <p>
     * WHY this and not a separate queue: the playlist IS the queue here, and a second hidden list that
     * overrides it would leave the commander looking at an order the player is not following.
     */
    private void queueNext(List<JukeboxDao.Track> chosen) {
        int target = nowPlayingId == null ? 0 : model.rowOf(nowPlayingId) + 1;
        if (target < 0) target = 0;
        for (JukeboxDao.Track track : chosen) {
            int from = model.rowOf(track.getId());
            if (from < 0) continue;
            library.move(from, target);
            refreshPlaylist();
            if (from >= target) target++;
        }
        refreshPlaylist();
    }

    /**
     * Adds the tracks an M3U playlist names.
     * <p>
     * The playlist's own titles and durations are ignored in favour of reading the files themselves - a
     * playlist exported years ago by another program is a weaker source than the tags on disk today.
     */
    private void importPlaylist() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle(getText("jukebox.choosePlaylist"));
        library.musicFolder().ifPresent(folder -> chooser.setCurrentDirectory(new File(folder)));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path chosen = chooser.getSelectedFile().toPath();
        statusLabel.setText(getText("jukebox.scanning"));
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws IOException {
                return library.add(PlaylistFileImporter.read(chosen));
            }

            @Override
            protected void done() {
                try {
                    int added = get();
                    statusLabel.setText(added == 0
                            ? getText("jukebox.playlistEmpty")
                            : getText("jukebox.tracksAdded", added));
                    if (added > 0) TagScanner.getInstance().requestScan();
                } catch (Exception e) {
                    log.warn("Could not import playlist {}: {}", chosen, e.getMessage());
                    statusLabel.setText(getText("jukebox.playlistUnreadable"));
                }
                refreshPlaylist();
            }
        }.execute();
    }

    private void removeSelected(List<JukeboxDao.Track> chosen) {
        library.remove(chosen.stream().map(JukeboxDao.Track::getId).toList());
        refreshPlaylist();
    }

    private void removeDeadEntries() {
        int removed = library.removeMissing();
        statusLabel.setText(getText("jukebox.tracksRemoved", removed));
        refreshPlaylist();
    }

    private void clearPlaylist() {
        boolean confirmed = HudConfirmDialog.confirm(this,
                getText("jukebox.menu.clear"),
                getText("jukebox.confirm.clear"),
                getText("jukebox.confirm.clearYes"),
                getText("jukebox.confirm.cancel"));
        if (!confirmed) return;
        library.clear();
        refreshPlaylist();
    }

    private void revealInFileManager(JukeboxDao.Track track) {
        File parent = new File(track.getPath()).getParentFile();
        if (parent == null || !parent.isDirectory() || !Desktop.isDesktopSupported()) return;
        try {
            Desktop.getDesktop().open(parent);
        } catch (IOException | UnsupportedOperationException e) {
            log.warn("Could not open {} in the file manager: {}", parent, e.getMessage());
        }
    }

    private void copyToClipboard(List<JukeboxDao.Track> chosen) {
        StringBuilder text = new StringBuilder();
        for (JukeboxDao.Track track : chosen) {
            if (text.length() > 0) text.append(System.lineSeparator());
            String artist = text(track.getArtist());
            text.append(artist.isEmpty() ? track.displayTitle() : artist + " - " + track.displayTitle());
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text.toString()), null);
    }

    // ---------------------------------------------------------------- dragging rows

    /**
     * Lets rows be dragged into a new order.
     * <p>
     * The move is written to the database rather than only to the table, because the order is the
     * commander's data and has to be there again next launch.
     */
    private void installDragToReorder() {
        playlist.setDragEnabled(true);
        playlist.setDropMode(DropMode.INSERT_ROWS);
        playlist.setTransferHandler(new TransferHandler() {
            private final DataFlavor rowFlavor =
                    new DataFlavor(Integer.class, "jukebox/playlist-row");

            @Override
            public int getSourceActions(JComponent component) {
                return MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent component) {
                return new RowTransfer(playlist.getSelectedRow(), rowFlavor);
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDrop() && support.isDataFlavorSupported(rowFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    int from = (Integer) support.getTransferable().getTransferData(rowFlavor);
                    int insertAt = ((JTable.DropLocation) support.getDropLocation()).getRow();
                    // A drop below the dragged row names an insertion point that still counts the row
                    // itself, so removing it first shifts the target up by one.
                    int to = insertAt > from ? insertAt - 1 : insertAt;
                    if (from < 0 || to < 0 || from == to) return false;
                    library.move(from, to);
                    refreshPlaylist();
                    playlist.setRowSelectionInterval(to, to);
                    return true;
                } catch (UnsupportedFlavorException | IOException e) {
                    log.warn("Could not reorder the playlist: {}", e.getMessage());
                    return false;
                }
            }
        });
    }

    private record RowTransfer(int row, DataFlavor flavor) implements Transferable {
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{flavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor candidate) {
            return flavor.equals(candidate);
        }

        @Override
        public Object getTransferData(DataFlavor candidate) throws UnsupportedFlavorException {
            if (!flavor.equals(candidate)) throw new UnsupportedFlavorException(candidate);
            return row;
        }
    }

    // ---------------------------------------------------------------- live state

    @Subscribe
    public void onJukeboxStateChanged(JukeboxStateChangedEvent event) {
        SwingUtilities.invokeLater(() -> {
            nowPlayingId = event.getTrackId();
            setPlayPauseLabel(event.getState() == PlaybackState.PLAYING);
            syncSeekBarToTrack();
            playlist.repaint();
        });
    }

    private void syncFromPlayer() {
        nowPlayingId = player.currentTrackId().orElse(null);
        setPlayPauseLabel(player.isPlaying());
        syncSeekBarToTrack();
    }

    /**
     * Swaps the one button that changes what it says as playback starts and stops.
     * <p>
     * WHY the upper-casing is repeated here: {@code HudButton} capitalises the label it is constructed
     * with, so every other button on the tab is upper case for free. Setting the text afterwards goes
     * straight to Swing and skips that, which left a mixed-case "Play" sitting between PREVIOUS and STOP.
     */
    private void setPlayPauseLabel(boolean playing) {
        playPauseButton.setText(getText(playing ? "jukebox.pause" : "jukebox.play").toUpperCase());
    }

    private static JMenuItem item(String label, Runnable action) {
        JMenuItem menuItem = new JMenuItem(label);
        menuItem.addActionListener(e -> action.run());
        return menuItem;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    // ---------------------------------------------------------------- table plumbing

    private final class PlaylistMouse extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowMenu(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;
            int row = playlist.rowAtPoint(e.getPoint());
            if (row < 0) return;
            player.playTrack(model.trackAt(row).getId());
        }

        private void maybeShowMenu(MouseEvent e) {
            if (!e.isPopupTrigger()) return;
            showContextMenu(e, playlist.rowAtPoint(e.getPoint()));
        }
    }

    /**
     * Paints the track that is playing in the information colour, and unavailable files dimmed.
     */
    private final class TrackCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            JukeboxDao.Track track = model.trackAt(row);
            if (selected) return cell;
            if (track != null && nowPlayingId != null && track.getId() == nowPlayingId) {
                cell.setForeground(HudPalette.HUD_COLOR_ROLE_INFORMATION);
            } else if (track != null && track.isMissing()) {
                cell.setForeground(HudPalette.HUD_COLOR_ROLE_DISABLED);
            } else {
                cell.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
            }
            return cell;
        }
    }

    /**
     * The playlist as the table sees it. Ordinal order is playlist order - never a column sort.
     */
    private final class PlaylistModel extends AbstractTableModel {

        private List<JukeboxDao.Track> tracks = List.of();

        void replaceWith(List<JukeboxDao.Track> rows) {
            tracks = rows;
            fireTableDataChanged();
        }

        JukeboxDao.Track trackAt(int row) {
            return row >= 0 && row < tracks.size() ? tracks.get(row) : null;
        }

        int rowOf(long trackId) {
            for (int i = 0; i < tracks.size(); i++) {
                if (tracks.get(i).getId() == trackId) return i;
            }
            return -1;
        }

        @Override
        public int getRowCount() {
            return tracks.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COLUMN_NUMBER -> getText("jukebox.column.number");
                case COLUMN_TITLE -> getText("jukebox.column.title");
                case COLUMN_ARTIST -> getText("jukebox.column.artist");
                case COLUMN_ALBUM -> getText("jukebox.column.album");
                default -> getText("jukebox.column.duration");
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            JukeboxDao.Track track = trackAt(row);
            if (track == null) return "";
            return switch (column) {
                case COLUMN_NUMBER -> String.valueOf(row + 1);
                case COLUMN_TITLE -> track.isMissing()
                        ? track.displayTitle() + " (" + getText("jukebox.missing") + ")"
                        : track.displayTitle();
                case COLUMN_ARTIST -> text(track.getArtist());
                case COLUMN_ALBUM -> text(track.getAlbum());
                default -> formatDuration(track.getDurationMs());
            };
        }

        /**
         * Blank until the tag reader has been over the file - a made-up duration would be worse.
         */
        private String formatDuration(Long durationMs) {
            if (durationMs == null || durationMs <= 0) return "";
            return formatClock(durationMs / 1000);
        }
    }
}
