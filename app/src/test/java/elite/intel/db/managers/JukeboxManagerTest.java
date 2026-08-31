package elite.intel.db.managers;

import elite.intel.db.dao.JukeboxDao;
import elite.intel.db.util.Database;
import elite.intel.jukebox.PlaybackOrder;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The playlist is the commander's own work - they picked the folder, dropped the tracks they did not
 * want, and dragged the rest into an order. All of that has to survive a restart, which is why it lives
 * in the database rather than in a list in memory.
 *
 * <p>Nothing here touches the disk or plays audio: the manager is deliberately I/O-free so the whole of
 * the playlist's behaviour can be exercised against the in-memory database.
 */
class JukeboxManagerTest {

    private static final String TRACK_A = "/music/aphelion.mp3";
    private static final String TRACK_B = "/music/beacon.mp3";
    private static final String TRACK_C = "/music/coriolis.mp3";

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    /**
     * The playlist and the transport state are single, shared, durable things - which is the point of the
     * feature - so each test starts from a known one rather than from whatever the last test left behind.
     * The shipped defaults themselves are asserted in {@code JukeboxSchemaTest}, against a freshly
     * migrated database, because a default stops being observable the moment anything sets it.
     */
    @BeforeEach
    void resetTheJukebox() {
        jukebox().clear();
        jukebox().rememberPosition(null, 0);
        jukebox().setVolume(70);
        jukebox().setPlaybackOrder(PlaybackOrder.SEQUENTIAL);
        jukebox().setMusicFolder(null);
    }

    // ---------------------------------------------------------------- building the list

    @Test
    void tracksAreAddedInTheOrderTheyAreGiven() {
        jukebox().add(List.of(TRACK_C, TRACK_A, TRACK_B));

        assertEquals(List.of(TRACK_C, TRACK_A, TRACK_B), paths(),
                "the order files arrive in is the playlist order until the commander changes it");
    }

    @Test
    void addingTheSameFolderTwiceDoesNotDoubleThePlaylist() {
        jukebox().add(List.of(TRACK_A, TRACK_B));
        int addedOnSecondPass = jukebox().add(List.of(TRACK_A, TRACK_B, TRACK_C));

        assertEquals(1, addedOnSecondPass, "only the file that was not already in the list is new");
        assertEquals(List.of(TRACK_A, TRACK_B, TRACK_C), paths());
    }

    @Test
    void aBatchContainingTheSameFileTwiceAddsItOnce() {
        assertEquals(1, jukebox().add(List.of(TRACK_A, TRACK_A)));
        assertEquals(List.of(TRACK_A), paths());
    }

    @Test
    void blankAndNullPathsAreNotTracks() {
        assertEquals(0, jukebox().add(java.util.Arrays.asList(null, "", "   ")));
        assertEquals(0, jukebox().size());
    }

    // ---------------------------------------------------------------- removing

    @Test
    void removingATrackClosesTheGapItLeft() {
        jukebox().add(List.of(TRACK_A, TRACK_B, TRACK_C));

        jukebox().remove(idAt(1));

        assertEquals(List.of(TRACK_A, TRACK_C), paths());
        assertEquals(List.of(0, 1), ordinals(),
                "ordinals stay dense from zero, or the next drag computes the wrong position");
    }

    @Test
    void removingIsFromTheListOnlyAndTheFileIsNeverNamedForDeletion() {
        jukebox().add(List.of(TRACK_A));
        assertTrue(jukebox().remove(idAt(0)));

        // The manager has no file-system reach at all, which is what makes "remove from list" safe:
        // there is no code path here that could delete anything from disk.
        assertEquals(0, jukebox().size());
    }

    @Test
    void removingATrackThatIsNotThereChangesNothing() {
        jukebox().add(List.of(TRACK_A));
        assertFalse(jukebox().remove(9999L));
        assertEquals(1, jukebox().size());
    }

    // ---------------------------------------------------------------- reordering

    @Test
    void draggingATrackDownShiftsTheOnesItPassed() {
        jukebox().add(List.of(TRACK_A, TRACK_B, TRACK_C));

        jukebox().move(0, 2);

        assertEquals(List.of(TRACK_B, TRACK_C, TRACK_A), paths());
        assertEquals(List.of(0, 1, 2), ordinals());
    }

    @Test
    void draggingATrackUpShiftsTheOnesItPassed() {
        jukebox().add(List.of(TRACK_A, TRACK_B, TRACK_C));

        jukebox().move(2, 0);

        assertEquals(List.of(TRACK_C, TRACK_A, TRACK_B), paths());
    }

    @Test
    void aDragThatEndsOutsideTheListIsAbandonedRatherThanThrowing() {
        jukebox().add(List.of(TRACK_A, TRACK_B));

        jukebox().move(0, 7);
        jukebox().move(-1, 0);

        assertEquals(List.of(TRACK_A, TRACK_B), paths(), "an abandoned gesture leaves the order alone");
    }

    @Test
    void theNewOrderSurvivesBeingReadBackFresh() {
        jukebox().add(List.of(TRACK_A, TRACK_B, TRACK_C));
        jukebox().move(2, 0);

        // Re-read through a new query rather than trusting the list the move returned.
        assertEquals(List.of(TRACK_C, TRACK_A, TRACK_B), paths(),
                "playlist order is stored, not held in memory, or it would not survive a restart");
    }

    @Test
    void sortingRewritesThePlaylistOrderRatherThanJustTheView() {
        jukebox().add(List.of(TRACK_C, TRACK_A, TRACK_B));

        jukebox().sort(Comparator.comparing(JukeboxDao.Track::displayTitle));

        assertEquals(List.of(TRACK_A, TRACK_B, TRACK_C), paths());
        assertEquals(List.of(0, 1, 2), ordinals(),
                "a sort is a change to the commander's data, so it is stored the same way a drag is");
    }

    // ---------------------------------------------------------------- tags

    @Test
    void everyNewTrackIsWaitingToBeScanned() {
        jukebox().add(List.of(TRACK_A, TRACK_B));

        assertEquals(2, jukebox().awaitingTagScan().size(),
                "rows go in from the path alone so a huge folder can be listed at once");
    }

    @Test
    void aScannedTrackIsNotOfferedForScanningAgain() {
        jukebox().add(List.of(TRACK_A, TRACK_B));
        jukebox().recordTags(idAt(0), "Aphelion", "Some Artist", "An Album", 3, 214_000L);

        assertEquals(List.of(TRACK_B), pathsOf(jukebox().awaitingTagScan()));
    }

    @Test
    void aFileWithNoTagsAtAllIsStillMarkedScanned() {
        jukebox().add(List.of(TRACK_A));
        jukebox().recordTags(idAt(0), null, null, null, null, null);

        assertTrue(jukebox().awaitingTagScan().isEmpty(),
                "an untagged file must not be re-read on every start");
        JukeboxDao.Track track = jukebox().track(idAt(0)).orElseThrow();
        assertTrue(track.isTagsScanned());
        assertNull(track.getTitle());
    }

    @Test
    void blankTagsAreStoredAsAbsentRatherThanAsEmptyText() {
        jukebox().add(List.of(TRACK_A));
        jukebox().recordTags(idAt(0), "   ", "", null, null, null);

        assertNull(jukebox().track(idAt(0)).orElseThrow().getTitle(),
                "a blank tag is the same as no tag, and the title fallback has to fire for both");
    }

    @Test
    void anUntaggedTrackFallsBackToItsFileName() {
        jukebox().add(List.of(TRACK_A));

        assertEquals("aphelion", jukebox().track(idAt(0)).orElseThrow().displayTitle(),
                "a row showing nothing at all is unusable, and the file name is what they named it");
    }

    @Test
    void aTaggedTrackShowsItsTitle() {
        jukebox().add(List.of(TRACK_A));
        jukebox().recordTags(idAt(0), "Aphelion", "Some Artist", null, null, null);

        assertEquals("Aphelion", jukebox().track(idAt(0)).orElseThrow().displayTitle());
    }

    @Test
    void aTagThatGenuinelySaysZeroIsNotConfusedWithAnUnreadTag() {
        jukebox().add(List.of(TRACK_A, TRACK_B));
        jukebox().recordTags(idAt(0), "Intro", null, null, 0, 0L);

        JukeboxDao.Track scanned = jukebox().track(idAt(0)).orElseThrow();
        assertEquals(0, scanned.getTrackNumber(), "zero is a real track number, not a missing one");
        assertEquals(0L, scanned.getDurationMs());
        assertNull(jukebox().track(idAt(1)).orElseThrow().getDurationMs(),
                "an unscanned row reports null, which is how it stays distinguishable from zero");
    }

    // ---------------------------------------------------------------- missing files

    @Test
    void anUnmountedDriveFlagsItsTracksWithoutEmptyingThePlaylist() {
        jukebox().add(List.of(TRACK_A, TRACK_B, TRACK_C));

        jukebox().recordMissing(List.of(idAt(0), idAt(2)));

        assertEquals(3, jukebox().size(), "a drive that is not mounted is not a deleted file");
        assertEquals(List.of(true, false, true), missingFlags());
    }

    @Test
    void remountingTheDriveClearsEveryStaleFlag() {
        jukebox().add(List.of(TRACK_A, TRACK_B));
        jukebox().recordMissing(List.of(idAt(0), idAt(1)));

        jukebox().recordMissing(List.of());

        assertEquals(List.of(false, false), missingFlags(),
                "passing the whole answer means a remount clears flags the caller never has to track");
    }

    @Test
    void droppingDeadEntriesRemovesOnlyTheFlaggedOnesAndRenumbers() {
        jukebox().add(List.of(TRACK_A, TRACK_B, TRACK_C));
        jukebox().recordMissing(List.of(idAt(1)));

        assertEquals(1, jukebox().removeMissing());

        assertEquals(List.of(TRACK_A, TRACK_C), paths());
        assertEquals(List.of(0, 1), ordinals());
    }

    // ---------------------------------------------------------------- transport state

    @Test
    void volumeAndOrderAreRemembered() {
        jukebox().setVolume(35);
        jukebox().setPlaybackOrder(PlaybackOrder.RANDOM);
        jukebox().setMusicFolder("/music");

        assertEquals(35, jukebox().volume());
        assertEquals(PlaybackOrder.RANDOM, jukebox().playbackOrder());
        assertEquals("/music", jukebox().musicFolder().orElseThrow());
    }

    @Test
    void volumeIsClampedRatherThanStoredOutOfRange() {
        jukebox().setVolume(140);
        assertEquals(100, jukebox().volume());

        jukebox().setVolume(-20);
        assertEquals(0, jukebox().volume());
    }

    @Test
    void anUnreadablePlaybackOrderFallsBackToSequential() {
        jukebox().setPlaybackOrder(null);
        assertEquals(PlaybackOrder.SEQUENTIAL, jukebox().playbackOrder());
    }

    @Test
    void thePlaceInTheTrackIsRememberedAcrossARestart() {
        jukebox().add(List.of(TRACK_A, TRACK_B));
        jukebox().rememberPosition(idAt(1), 92_500L);

        assertEquals(TRACK_B, jukebox().currentTrack().orElseThrow().getPath());
        assertEquals(92_500L, jukebox().state().getPositionMs(),
                "an explorer who quits mid-track expects to come back to the same place in it");
    }

    @Test
    void stoppingPlaybackClearsThePositionRatherThanLeavingAStaleOne() {
        jukebox().add(List.of(TRACK_A));
        jukebox().rememberPosition(idAt(0), 30_000L);

        jukebox().rememberPosition(null, 30_000L);

        assertTrue(jukebox().currentTrack().isEmpty());
        assertEquals(0L, jukebox().state().getPositionMs());
    }

    @Test
    void removingTheTrackThatWasPlayingLeavesNoCurrentTrack() {
        jukebox().add(List.of(TRACK_A, TRACK_B));
        long playing = idAt(0);
        jukebox().rememberPosition(playing, 10_000L);

        jukebox().remove(playing);

        // The id survives in jukebox_state, but AUTOINCREMENT guarantees it is never re-issued, so it
        // resolves to nothing instead of resuming inside a different song.
        assertTrue(jukebox().currentTrack().isEmpty());
    }

    @Test
    void anIdIsNeverReusedAfterItsTrackIsRemoved() {
        jukebox().add(List.of(TRACK_A));
        long firstId = idAt(0);
        jukebox().remove(firstId);

        jukebox().add(List.of(TRACK_B));

        assertTrue(idAt(0) > firstId,
                "re-using a rowid would let a saved position resume inside a different song");
    }

    // ---------------------------------------------------------------- helpers

    private static JukeboxManager jukebox() {
        return JukeboxManager.getInstance();
    }

    private static long idAt(int index) {
        return jukebox().playlist().get(index).getId();
    }

    private static List<String> paths() {
        return pathsOf(jukebox().playlist());
    }

    private static List<String> pathsOf(List<JukeboxDao.Track> tracks) {
        return tracks.stream().map(JukeboxDao.Track::getPath).toList();
    }

    private static List<Integer> ordinals() {
        return jukebox().playlist().stream().map(JukeboxDao.Track::getOrdinal).toList();
    }

    private static List<Boolean> missingFlags() {
        return jukebox().playlist().stream().map(JukeboxDao.Track::isMissing).toList();
    }
}
