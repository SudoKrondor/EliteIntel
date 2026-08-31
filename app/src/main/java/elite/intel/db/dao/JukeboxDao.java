package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * The jukebox playlist and its transport state.
 * <p>
 * Two shapes live here because the feature has two: an ordered list of files the commander assembled, and
 * the single row saying where they were in it. Callers go through {@code JukeboxManager} rather than
 * touching this directly.
 */
@RegisterRowMapper(JukeboxDao.TrackMapper.class)
@RegisterRowMapper(JukeboxDao.StateMapper.class)
public interface JukeboxDao {

    // ---------------------------------------------------------------- playlist reads

    /**
     * The whole playlist in the commander's chosen order.
     */
    @SqlQuery("SELECT * FROM jukebox_track ORDER BY ordinal")
    List<Track> playlist();

    /**
     * One track, or null when the id names a row that is no longer in the list.
     */
    @SqlQuery("SELECT * FROM jukebox_track WHERE id = :id")
    Track track(@Bind("id") long id);

    @SqlQuery("SELECT COUNT(*) FROM jukebox_track")
    int count();

    /**
     * The highest ordinal in use, or -1 when the list is empty, so the next append is always
     * {@code lastOrdinal() + 1} without a special case for the first row.
     */
    @SqlQuery("SELECT COALESCE(MAX(ordinal), -1) FROM jukebox_track")
    int lastOrdinal();

    /**
     * The next tracks whose tags have never been read, in playlist order so the rows the commander is
     * most likely looking at fill in first.
     */
    @SqlQuery("SELECT * FROM jukebox_track WHERE tagsScannedAt IS NULL ORDER BY ordinal LIMIT :limit")
    List<Track> awaitingTagScan(@Bind("limit") int limit);

    // ---------------------------------------------------------------- playlist writes

    /**
     * Adds one file at the given position, or does nothing when the path is already in the list.
     *
     * @return 1 when the row was new, 0 when the path was already present
     */
    @SqlUpdate("INSERT OR IGNORE INTO jukebox_track (path, ordinal) VALUES (:path, :ordinal)")
    int append(@Bind("path") String path, @Bind("ordinal") int ordinal);

    @SqlUpdate("DELETE FROM jukebox_track WHERE id = :id")
    int delete(@Bind("id") long id);

    @SqlUpdate("DELETE FROM jukebox_track")
    void deleteAll();

    @SqlUpdate("DELETE FROM jukebox_track WHERE missing = 1")
    int deleteMissing();

    @SqlUpdate("UPDATE jukebox_track SET ordinal = :ordinal WHERE id = :id")
    void setOrdinal(@Bind("id") long id, @Bind("ordinal") int ordinal);

    @SqlUpdate("UPDATE jukebox_track SET missing = :missing WHERE id = :id")
    void setMissing(@Bind("id") long id, @Bind("missing") boolean missing);

    @SqlUpdate("UPDATE jukebox_track SET missing = 0")
    void clearMissingFlags();

    /**
     * Records what the tag reader found. Every column may be null - plenty of files carry no tags at all -
     * but {@code scannedAt} never is, because it is what stops the file being read again on every start.
     */
    @SqlUpdate("""
            UPDATE jukebox_track
               SET title = :title,
                   artist = :artist,
                   album = :album,
                   trackNumber = :trackNumber,
                   durationMs = :durationMs,
                   tagsScannedAt = :scannedAt
             WHERE id = :id
            """)
    void recordTags(@Bind("id") long id,
                    @Bind("title") String title,
                    @Bind("artist") String artist,
                    @Bind("album") String album,
                    @Bind("trackNumber") Integer trackNumber,
                    @Bind("durationMs") Long durationMs,
                    @Bind("scannedAt") String scannedAt);

    // ---------------------------------------------------------------- multi-row operations

    /**
     * Appends every path not already in the list, keeping the order given.
     *
     * @return how many rows were new
     */
    @Transaction
    default int appendAll(List<String> paths) {
        int ordinal = lastOrdinal();
        int added = 0;
        for (String path : paths) {
            if (append(path, ordinal + 1) == 0) continue;
            ordinal++;
            added++;
        }
        return added;
    }

    /**
     * Rewrites every ordinal to the position of its id in {@code idsInOrder}, which becomes the new
     * playlist order.
     * <p>
     * WHY renumber the whole list rather than shift a range: it is one statement per row inside a single
     * transaction, which SQLite does in a few milliseconds even for a library of thousands, and it leaves
     * the ordinals dense and gap-free afterwards. Sparse ordinals with room to insert between them would
     * save writes on a drag and cost a rebuild the first time the gaps run out.
     */
    @Transaction
    default void reorder(List<Long> idsInOrder) {
        for (int i = 0; i < idsInOrder.size(); i++) {
            setOrdinal(idsInOrder.get(i), i);
        }
    }

    /**
     * Flags exactly the given tracks as absent from disk and clears the flag on every other row.
     */
    @Transaction
    default void recordMissing(Collection<Long> missingIds) {
        clearMissingFlags();
        for (Long id : missingIds) {
            setMissing(id, true);
        }
    }

    // ---------------------------------------------------------------- transport state

    @SqlQuery("SELECT * FROM jukebox_state WHERE id = 1")
    State state();

    @SqlUpdate("""
            INSERT OR REPLACE INTO jukebox_state
                (id, musicFolder, volume, playbackOrder, currentTrackId, positionMs)
            VALUES (1, :musicFolder, :volume, :playbackOrder, :currentTrackId, :positionMs)
            """)
    void saveState(@BindBean State state);

    // ---------------------------------------------------------------- entities

    class TrackMapper implements RowMapper<Track> {
        @Override
        public Track map(ResultSet rs, StatementContext ctx) throws SQLException {
            Track track = new Track();
            track.setId(rs.getLong("id"));
            track.setPath(rs.getString("path"));
            track.setOrdinal(rs.getInt("ordinal"));
            track.setTitle(rs.getString("title"));
            track.setArtist(rs.getString("artist"));
            track.setAlbum(rs.getString("album"));
            track.setTrackNumber(nullableInt(rs, "trackNumber"));
            track.setDurationMs(nullableLong(rs, "durationMs"));
            track.setTagsScannedAt(rs.getString("tagsScannedAt"));
            track.setMissing(rs.getBoolean("missing"));
            return track;
        }

        /**
         * WHY not {@code rs.getInt}: it answers 0 for SQL NULL, and 0 is a real track number and a real
         * duration. The unread-tag case has to stay distinguishable from a tag that genuinely says zero.
         */
        private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        }

        private static Long nullableLong(ResultSet rs, String column) throws SQLException {
            long value = rs.getLong(column);
            return rs.wasNull() ? null : value;
        }
    }

    class StateMapper implements RowMapper<State> {
        @Override
        public State map(ResultSet rs, StatementContext ctx) throws SQLException {
            State state = new State();
            state.setMusicFolder(rs.getString("musicFolder"));
            state.setVolume(rs.getInt("volume"));
            state.setPlaybackOrder(rs.getString("playbackOrder"));
            long trackId = rs.getLong("currentTrackId");
            state.setCurrentTrackId(rs.wasNull() ? null : trackId);
            state.setPositionMs(rs.getLong("positionMs"));
            return state;
        }
    }

    /**
     * One file in the playlist, plus whatever its tags turned out to say.
     */
    class Track {
        private long id;
        private String path;
        private int ordinal;
        private String title;
        private String artist;
        private String album;
        private Integer trackNumber;
        private Long durationMs;
        private String tagsScannedAt;
        private boolean missing;

        /**
         * What to show in the playlist: the title tag when the file has one, and the file name without its
         * extension when it does not.
         * <p>
         * WHY a fallback rather than a blank cell: a great many files carry no tags, and a row showing
         * nothing at all is unusable. The file name is what the commander named it, so it is the next best
         * answer and usually a perfectly good one.
         */
        public String displayTitle() {
            if (title != null && !title.isBlank()) return title;
            if (path == null || path.isBlank()) return "";
            String fileName = Paths.get(path).getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            return dot > 0 ? fileName.substring(0, dot) : fileName;
        }

        /**
         * True once the tag reader has been over this file, whether or not it found anything.
         */
        public boolean isTagsScanned() {
            return tagsScannedAt != null;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        /**
         * Absolute path to the file on disk. Unique across the playlist.
         */
        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        /**
         * Position in the playlist, dense from 0. The commander sets this by dragging rows.
         */
        public int getOrdinal() {
            return ordinal;
        }

        public void setOrdinal(int ordinal) {
            this.ordinal = ordinal;
        }

        /**
         * The title tag, or null when the file has none or has not been scanned yet.
         */
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public Integer getTrackNumber() {
            return trackNumber;
        }

        public void setTrackNumber(Integer trackNumber) {
            this.trackNumber = trackNumber;
        }

        /**
         * Playing time in milliseconds, or null until the file has been scanned.
         */
        public Long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(Long durationMs) {
            this.durationMs = durationMs;
        }

        public String getTagsScannedAt() {
            return tagsScannedAt;
        }

        public void setTagsScannedAt(String tagsScannedAt) {
            this.tagsScannedAt = tagsScannedAt;
        }

        /**
         * True when the file was not on disk the last time the playlist was checked.
         */
        public boolean isMissing() {
            return missing;
        }

        public void setMissing(boolean missing) {
            this.missing = missing;
        }
    }

    /**
     * Where the commander was in the playlist, and how they want it played.
     */
    class State {
        private String musicFolder;
        private int volume;
        private String playbackOrder;
        private Long currentTrackId;
        private long positionMs;

        /**
         * The folder the directory picker last opened at, or null before one has been chosen.
         */
        public String getMusicFolder() {
            return musicFolder;
        }

        public void setMusicFolder(String musicFolder) {
            this.musicFolder = musicFolder;
        }

        /**
         * Music volume, 0-100. Its own setting, deliberately not the AI speech volume: the commander sets
         * this one on the Jukebox tab because the Audio tab governs the companion's voice.
         */
        public int getVolume() {
            return volume;
        }

        public void setVolume(int volume) {
            this.volume = volume;
        }

        /**
         * The {@code PlaybackOrder} constant name. Read through {@code PlaybackOrder.fromStored}.
         */
        public String getPlaybackOrder() {
            return playbackOrder;
        }

        public void setPlaybackOrder(String playbackOrder) {
            this.playbackOrder = playbackOrder;
        }

        /**
         * The track that was playing, or null. May name a row since removed, so resolve it before use.
         */
        public Long getCurrentTrackId() {
            return currentTrackId;
        }

        public void setCurrentTrackId(Long currentTrackId) {
            this.currentTrackId = currentTrackId;
        }

        /**
         * How far into the current track playback had reached, in milliseconds.
         */
        public long getPositionMs() {
            return positionMs;
        }

        public void setPositionMs(long positionMs) {
            this.positionMs = positionMs;
        }
    }
}
