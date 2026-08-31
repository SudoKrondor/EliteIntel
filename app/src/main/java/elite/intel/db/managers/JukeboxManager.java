package elite.intel.db.managers;

import elite.intel.db.dao.JukeboxDao;
import elite.intel.db.util.Database;
import elite.intel.jukebox.PlaybackOrder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The commander's music library: which files are in the playlist, what order they sit in, and where
 * playback had reached when the app last closed.
 * <p>
 * <b>Why the playlist is stored at all.</b> Explorers run for hours and rebuild nothing between sessions.
 * A player that forgets its list, its order and its place on every restart is one the commander stops
 * using, so the list is durable and so is the position in it.
 * <p>
 * <b>What this does not do.</b> No file system and no audio: it never touches the disk to see whether a
 * path still resolves, never reads a tag, and never plays anything. Those belong to the scanner and the
 * playback engine, which report their findings here. Keeping the I/O out means the whole of the
 * playlist's behaviour is testable against an in-memory database.
 */
public final class JukeboxManager {

    /**
     * How many unscanned files one call hands the tag reader. Small enough that the rows the commander is
     * looking at appear quickly, large enough that a big library does not cost a query per file.
     */
    private static final int TAG_SCAN_BATCH = 64;

    private static final int MIN_VOLUME = 0;
    private static final int MAX_VOLUME = 100;

    private static final JukeboxManager INSTANCE = new JukeboxManager();

    private JukeboxManager() {
    }

    public static JukeboxManager getInstance() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------- reading the playlist

    /**
     * The whole playlist in the commander's order. Empty before they have added anything.
     */
    public List<JukeboxDao.Track> playlist() {
        return Database.withDao(JukeboxDao.class, JukeboxDao::playlist);
    }

    public int size() {
        return Database.withDao(JukeboxDao.class, JukeboxDao::count);
    }

    /**
     * One track by id, or empty when it is no longer in the playlist.
     */
    public Optional<JukeboxDao.Track> track(long id) {
        return Optional.ofNullable(Database.withDao(JukeboxDao.class, dao -> dao.track(id)));
    }

    /**
     * The track that was playing when the app last closed, or empty when there was none - or when the
     * commander has since removed it from the list.
     */
    public Optional<JukeboxDao.Track> currentTrack() {
        Long id = state().getCurrentTrackId();
        return id == null ? Optional.empty() : track(id);
    }

    // ---------------------------------------------------------------- changing the playlist

    /**
     * Appends files to the end of the playlist, ignoring any already in it.
     * <p>
     * Adding the same folder twice, or importing a playlist that overlaps one already loaded, is therefore
     * a no-op rather than a way to double every track.
     *
     * @param paths absolute paths, in the order they should appear
     * @return how many were actually new
     */
    public int add(List<String> paths) {
        if (paths == null || paths.isEmpty()) return 0;
        List<String> distinct = new ArrayList<>(new LinkedHashSet<>(paths));
        distinct.removeIf(path -> path == null || path.isBlank());
        if (distinct.isEmpty()) return 0;
        return Database.withDao(JukeboxDao.class, dao -> dao.appendAll(distinct));
    }

    /**
     * Removes one track from the playlist and closes the gap it left. The file on disk is untouched -
     * this is the right-click "remove from list", not a delete.
     *
     * @return true when a row was removed
     */
    public boolean remove(long id) {
        return remove(List.of(id)) > 0;
    }

    /**
     * Removes several tracks and renumbers what is left.
     *
     * @return how many rows were removed
     */
    public int remove(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        Set<Long> doomed = new LinkedHashSet<>(ids);
        return Database.withDao(JukeboxDao.class, dao -> {
            int removed = 0;
            for (Long id : doomed) {
                removed += dao.delete(id);
            }
            if (removed > 0) renumber(dao);
            return removed;
        });
    }

    /**
     * Empties the playlist. Files on disk are untouched.
     */
    public void clear() {
        Database.withDao(JukeboxDao.class, dao -> {
            dao.deleteAll();
            return null;
        });
    }

    /**
     * Drops every track flagged as absent from disk, and renumbers what is left.
     *
     * @return how many rows were removed
     */
    public int removeMissing() {
        return Database.withDao(JukeboxDao.class, dao -> {
            int removed = dao.deleteMissing();
            if (removed > 0) renumber(dao);
            return removed;
        });
    }

    /**
     * Moves the track at {@code fromIndex} to {@code toIndex}, shifting everything between them along -
     * what dragging a row up or down the list does.
     * <p>
     * Indices are positions in the playlist, not track ids, because that is what the table hands us. Out of
     * range indices are ignored rather than throwing: a drag that ends outside the list is a gesture the
     * commander abandoned, not a defect.
     */
    public void move(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return;
        Database.withDao(JukeboxDao.class, dao -> {
            List<JukeboxDao.Track> tracks = dao.playlist();
            if (!isIndexInRange(fromIndex, tracks) || !isIndexInRange(toIndex, tracks)) return null;
            List<Long> ids = idsOf(tracks);
            ids.add(toIndex, ids.remove(fromIndex));
            dao.reorder(ids);
            return null;
        });
    }

    /**
     * Rewrites the playlist order to match {@code comparator} - the one-shot "sort by title / artist /
     * folder" from the right-click menu.
     * <p>
     * WHY this writes ordinals instead of the table sorting its own view: the order is the commander's
     * data. They may drag a row after sorting, and the result has to survive a restart, which a view-only
     * sort would not.
     */
    public void sort(Comparator<JukeboxDao.Track> comparator) {
        if (comparator == null) return;
        Database.withDao(JukeboxDao.class, dao -> {
            List<JukeboxDao.Track> tracks = dao.playlist();
            tracks.sort(comparator);
            dao.reorder(idsOf(tracks));
            return null;
        });
    }

    // ---------------------------------------------------------------- what the scanners report

    /**
     * The next files whose tags have not been read, in playlist order. Empty once the library is scanned.
     */
    public List<JukeboxDao.Track> awaitingTagScan() {
        return Database.withDao(JukeboxDao.class, dao -> dao.awaitingTagScan(TAG_SCAN_BATCH));
    }

    /**
     * Records what the tag reader found for one file. Every field may be null - a file with no tags at all
     * is normal - and the row is still marked scanned so it is not read again on the next start.
     */
    public void recordTags(long id, String title, String artist, String album,
                           Integer trackNumber, Long durationMs) {
        String scannedAt = Instant.now().toString();
        Database.withDao(JukeboxDao.class, dao -> {
            dao.recordTags(id, trimToNull(title), trimToNull(artist), trimToNull(album),
                    trackNumber, durationMs, scannedAt);
            return null;
        });
    }

    /**
     * Flags one track as absent from disk, after playback failed to open it.
     * <p>
     * Separate from {@link #recordMissing(Collection)} because it answers a different question: that one
     * reports a sweep of the whole playlist, this one reports the single file the player just tripped over,
     * and must not clear the flags on any other row while doing so.
     */
    public void markMissing(long id) {
        Database.withDao(JukeboxDao.class, dao -> {
            dao.setMissing(id, true);
            return null;
        });
    }

    /**
     * Records the result of checking the playlist against the disk: exactly these tracks are absent, and
     * every other row is available again.
     * <p>
     * WHY the whole answer at once rather than a flag per file: an unmounted drive makes hundreds of tracks
     * vanish together and reappear together, and passing the complete set means a remount clears every
     * stale flag without the caller having to remember which ones it set.
     */
    public void recordMissing(Collection<Long> missingIds) {
        Collection<Long> missing = missingIds == null ? List.of() : missingIds;
        Database.withDao(JukeboxDao.class, dao -> {
            dao.recordMissing(missing);
            return null;
        });
    }

    // ---------------------------------------------------------------- transport state

    /**
     * The stored settings and playback position. Never null - the migration seeds the single row.
     */
    public JukeboxDao.State state() {
        return Database.withDao(JukeboxDao.class, JukeboxDao::state);
    }

    /**
     * The folder the directory picker should open at, or empty before one has been chosen.
     */
    public Optional<String> musicFolder() {
        return Optional.ofNullable(trimToNull(state().getMusicFolder()));
    }

    public void setMusicFolder(String folder) {
        mutateState(state -> state.setMusicFolder(trimToNull(folder)));
    }

    /**
     * Music volume, 0-100. Separate from the companion's speech volume by design.
     */
    public int volume() {
        return clampVolume(state().getVolume());
    }

    public void setVolume(int volume) {
        mutateState(state -> state.setVolume(clampVolume(volume)));
    }

    public PlaybackOrder playbackOrder() {
        return PlaybackOrder.fromStored(state().getPlaybackOrder());
    }

    public void setPlaybackOrder(PlaybackOrder order) {
        PlaybackOrder resolved = order == null ? PlaybackOrder.SEQUENTIAL : order;
        mutateState(state -> state.setPlaybackOrder(resolved.name()));
    }

    /**
     * Remembers where playback had reached, so closing the app mid-track and reopening it resumes there.
     *
     * @param trackId    the track being played, or null when playback stopped
     * @param positionMs how far into it, clamped at zero
     */
    public void rememberPosition(Long trackId, long positionMs) {
        mutateState(state -> {
            state.setCurrentTrackId(trackId);
            state.setPositionMs(Math.max(0, trackId == null ? 0 : positionMs));
        });
    }

    // ---------------------------------------------------------------- internals

    /**
     * Applies a change to the single state row and writes it back.
     * <p>
     * WHY read-modify-write rather than a column-by-column update: the row is written only when the
     * commander moves a control or a track ends, so the extra read costs nothing, and it keeps one
     * INSERT OR REPLACE as the single place the row's shape is spelled out.
     */
    private void mutateState(java.util.function.Consumer<JukeboxDao.State> change) {
        Database.withDao(JukeboxDao.class, dao -> {
            JukeboxDao.State state = dao.state();
            change.accept(state);
            dao.saveState(state);
            return null;
        });
    }

    /**
     * Closes any gaps left in the ordinals by a removal, leaving them dense from 0 again.
     */
    private static void renumber(JukeboxDao dao) {
        dao.reorder(idsOf(dao.playlist()));
    }

    private static List<Long> idsOf(List<JukeboxDao.Track> tracks) {
        List<Long> ids = new ArrayList<>(tracks.size());
        for (JukeboxDao.Track track : tracks) {
            ids.add(track.getId());
        }
        return ids;
    }

    private static boolean isIndexInRange(int index, List<?> list) {
        return index >= 0 && index < list.size();
    }

    private static int clampVolume(int volume) {
        return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
