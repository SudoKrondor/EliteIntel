package elite.intel.jukebox;

import elite.intel.db.dao.JukeboxDao;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.db.util.Database;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The transport: what the buttons and the voice commands do, and what happens when a track ends, a file
 * has gone missing, or the whole drive has.
 *
 * <p>Runs against a fake decoder and a fake sound card, so it exercises the real threading and the real
 * playlist while needing neither a codec nor an audio device - the build server has no speakers, and a
 * test that needs one only ever runs on a developer's desktop.
 */
class JukeboxPlayerTest {

    private static final long AWAIT_MS = 5_000;

    private FakeOutput output;
    private FakeLibraryOfTracks files;
    private JukeboxPlayer player;

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void freshPlaylist() {
        JukeboxManager library = JukeboxManager.getInstance();
        library.clear();
        library.rememberPosition(null, 0);
        library.setVolume(100);
        library.setPlaybackOrder(PlaybackOrder.SEQUENTIAL);
        library.add(List.of("/music/one.mp3", "/music/two.mp3", "/music/three.mp3"));

        output = new FakeOutput();
        files = new FakeLibraryOfTracks();
        player = new JukeboxPlayer(library, () -> output, files, new MusicDucker(() -> -120.0));
        player.start();
    }

    @AfterEach
    void stopPlaying() {
        player.shutdown();
    }

    // ---------------------------------------------------------------- transport

    @Test
    void startingDoesNotPlayAnythingUntilAsked() {
        assertEquals(PlaybackState.STOPPED, player.state(),
                "an app that starts making noise the moment it launches is one people launch muted");
        assertEquals(0, output.bytesWritten());
    }

    @Test
    void playingSendsAudioToTheSpeaker() {
        player.play();

        await(() -> output.bytesWritten() > 0, "no audio reached the output");
        assertEquals(PlaybackState.PLAYING, player.state());
    }

    @Test
    void pausingStopsTheAudio() {
        player.play();
        await(() -> output.bytesWritten() > 0, "playback never started");

        player.pause();

        assertEquals(PlaybackState.PAUSED, player.state());
        long settled = settledByteCount();
        assertEquals(settled, settledByteCount(), "audio kept flowing after pause");
        assertTrue(output.wasFlushed(), "a pause has to silence the buffer, not play out another tenth of a second");
    }

    @Test
    void pausingAndPlayingAgainResumesRatherThanRestarting() {
        player.play();
        await(() -> player.positionMs() > 0, "playback never advanced");
        player.pause();
        long pausedAt = player.positionMs();

        player.play();

        await(() -> player.positionMs() > pausedAt, "playback did not continue from where it paused");
        assertEquals(PlaybackState.PLAYING, player.state());
    }

    @Test
    void doubleClickingATrackPlaysThatOne() {
        long third = trackId(2);

        player.playTrack(third);

        await(() -> player.currentTrackId().orElse(-1L) == third, "the chosen track did not start");
        await(() -> files.opened().contains("/music/three.mp3"), "a different file was opened");
    }

    @Test
    void nextMovesDownThePlaylist() {
        player.play();
        await(() -> player.currentTrackId().isPresent(), "nothing started");

        player.next();

        await(() -> player.currentTrackId().orElse(-1L) == trackId(1), "next did not reach the second track");
    }

    @Test
    void previousGoesBackToWhatWasPlayingBefore() {
        player.playTrack(trackId(0));
        await(() -> files.opened().contains("/music/one.mp3"), "first track never opened");
        player.playTrack(trackId(2));
        await(() -> player.currentTrackId().orElse(-1L) == trackId(2), "third track never started");

        player.previous();

        await(() -> player.currentTrackId().orElse(-1L) == trackId(0),
                "previous must return to what was actually played, not the row above");
    }

    @Test
    void stoppingRewindsTheTrackRatherThanKeepingThePosition() {
        player.play();
        await(() -> player.positionMs() > 0, "playback never advanced");

        player.stop();

        assertEquals(PlaybackState.STOPPED, player.state());
        assertEquals(0, player.positionMs());
    }

    @Test
    void movingThePlayHeadCarriesOnFromWhereItWasPut() {
        player.play();
        await(() -> player.positionMs() > 0, "playback never advanced");

        player.seekTo(60_000);

        await(() -> player.positionMs() > 60_000, "playback did not carry on from the new play-head");
        assertEquals(PlaybackState.PLAYING, player.state());
        assertTrue(output.wasFlushed(),
                "the audio already queued has to go, or the move is heard a fifth of a second late");
    }

    @Test
    void movingThePlayHeadOfAPausedTrackDoesNotStartItPlaying() {
        player.play();
        await(() -> player.positionMs() > 0, "playback never advanced");
        player.pause();

        player.seekTo(30_000);

        assertEquals(PlaybackState.PAUSED, player.state(), "moving the play-head is not a play button");
        assertEquals(30_000L, player.positionMs());

        player.play();

        await(() -> player.positionMs() > 30_000, "playback resumed somewhere other than the new play-head");
    }

    @Test
    void thePlayHeadCannotBeMovedWhenNothingIsSelected() {
        player.seekTo(10_000);

        assertEquals(PlaybackState.STOPPED, player.state());
        assertEquals(0, player.positionMs(), "there is no track to be ten seconds into");
    }

    // ---------------------------------------------------------------- moving through the playlist

    @Test
    void aFinishedTrackRollsOnToTheNextOne() {
        files.lengthInBlocks(3);

        player.play();

        await(() -> player.currentTrackId().orElse(-1L) == trackId(1),
                "the playlist did not advance when the track ended");
    }

    @Test
    void theEndOfThePlaylistStopsPlaybackInSequentialOrder() {
        files.lengthInBlocks(2);

        player.playTrack(trackId(2));

        await(() -> player.state() == PlaybackState.STOPPED,
                "sequential playback should finish at the end of the list, not wrap round");
    }

    @Test
    void randomOrderKeepsGoingPastTheEndOfTheList() {
        JukeboxManager.getInstance().setPlaybackOrder(PlaybackOrder.RANDOM);
        player.setPlaybackOrder(PlaybackOrder.RANDOM);
        files.lengthInBlocks(2);

        player.playTrack(trackId(2));

        // Counted as opens, not distinct paths: with three tracks the set of paths tops out at three
        // however long it runs, so it could never show that the list wrapped round.
        await(() -> files.openAttempts() >= 5, "random order should have kept picking tracks");
        assertEquals(PlaybackState.PLAYING, player.state());
    }

    // ---------------------------------------------------------------- when files are gone

    @Test
    void aTrackThatWillNotOpenIsFlaggedAndSkipped() {
        files.failToOpen("/music/one.mp3");

        player.play();

        await(() -> player.currentTrackId().orElse(-1L) == trackId(1), "the player got stuck on a dead file");
        assertTrue(missingPaths().contains("/music/one.mp3"),
                "a file that would not open should be flagged so the commander can clear it out");
    }

    @Test
    void anUnmountedDriveStopsPlaybackInsteadOfSpinningThroughTheList() {
        files.failToOpen("/music/one.mp3", "/music/two.mp3", "/music/three.mp3");

        player.play();

        await(() -> player.state() == PlaybackState.STOPPED,
                "with nothing playable the player must stop, not loop over the playlist forever");
        assertTrue(files.openAttempts() < 40,
                "it should give up after one pass, not keep retrying: " + files.openAttempts() + " attempts");
    }

    // ---------------------------------------------------------------- gain

    @Test
    void theVolumeSettingScalesWhatIsPlayed() {
        files.sampleValue((short) 10_000);
        player.setVolume(50);

        player.play();
        await(() -> output.bytesWritten() > MusicFormat.BLOCK_BYTES * 4, "not enough audio to measure");

        assertEquals(5_000, output.steadyStateAmplitude(), 400,
                "half volume should be half amplitude");
    }

    @Test
    void aSpeakingCompanionPullsTheMusicDown() {
        files.sampleValue((short) 10_000);
        JukeboxPlayer ducked = new JukeboxPlayer(JukeboxManager.getInstance(), () -> output, files,
                new MusicDucker(() -> -6.0));
        ducked.start();
        try {
            ducked.setVolume(100);
            ducked.play();
            await(() -> output.bytesWritten() > MusicFormat.BLOCK_BYTES * 60, "not enough audio to measure");

            assertTrue(output.steadyStateAmplitude() < 4_000,
                    "a loud companion should duck the music well below its set level, but the music was at "
                            + output.steadyStateAmplitude());
        } finally {
            ducked.shutdown();
        }
    }

    @Test
    void positionIsWrittenDownSoItSurvivesARestart() {
        player.play();
        await(() -> player.positionMs() > 0, "playback never advanced");

        player.pause();

        JukeboxDao.State stored = JukeboxManager.getInstance().state();
        assertEquals(trackId(0), stored.getCurrentTrackId());
        assertTrue(stored.getPositionMs() > 0, "pausing should record where the commander got to");
    }

    // ---------------------------------------------------------------- helpers

    private static long trackId(int index) {
        return JukeboxManager.getInstance().playlist().get(index).getId();
    }

    private static Set<String> missingPaths() {
        return JukeboxManager.getInstance().playlist().stream()
                .filter(JukeboxDao.Track::isMissing)
                .map(JukeboxDao.Track::getPath)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Lets the playback thread run for a moment and reports how much it wrote, twice, to spot movement.
     */
    private long settledByteCount() {
        sleep(120);
        return output.bytesWritten();
    }

    private static void await(BooleanSupplier condition, String failure) {
        long deadline = System.currentTimeMillis() + AWAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            sleep(5);
        }
        fail(failure);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * A sound card that records what it was given and paces the loop the way a real one would.
     */
    private static final class FakeOutput implements MusicOutput {
        private final AtomicLong bytes = new AtomicLong();
        private volatile boolean flushed;
        private volatile int lastAmplitude;

        @Override
        public void open() {
        }

        @Override
        public void write(byte[] pcm, int offset, int length) {
            int peak = 0;
            for (int i = offset; i + 1 < offset + length; i += 2) {
                short sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
                peak = Math.max(peak, Math.abs(sample));
            }
            lastAmplitude = peak;
            bytes.addAndGet(length);
            // A real line blocks until the device has room; without that the loop spins flat out.
            sleep(1);
        }

        @Override
        public void flush() {
            flushed = true;
        }

        @Override
        public void close() {
        }

        long bytesWritten() {
            return bytes.get();
        }

        boolean wasFlushed() {
            return flushed;
        }

        /**
         * The most recent block's peak, once the gain ramp has settled.
         */
        int steadyStateAmplitude() {
            return lastAmplitude;
        }
    }

    /**
     * Stands in for the MP3 decoder: constant-amplitude audio of a chosen length.
     */
    private static final class FakeLibraryOfTracks implements JukeboxPlayer.SourceFactory {
        private final Set<String> openedPaths = ConcurrentHashMap.newKeySet();
        private final Set<String> unopenable = ConcurrentHashMap.newKeySet();
        private final AtomicLong attempts = new AtomicLong();
        private volatile int blocks = Integer.MAX_VALUE;
        private volatile short sample = 8_000;

        void lengthInBlocks(int count) {
            blocks = count;
        }

        void sampleValue(short value) {
            sample = value;
        }

        void failToOpen(String... paths) {
            unopenable.addAll(Set.of(paths));
        }

        Set<String> opened() {
            return openedPaths;
        }

        long openAttempts() {
            return attempts.get();
        }

        @Override
        public AudioSource open(Path file, long startMs) throws IOException {
            attempts.incrementAndGet();
            String path = file.toString();
            if (unopenable.contains(path)) {
                throw new IOException("no such file: " + path);
            }
            openedPaths.add(path);
            return new ConstantToneSource(blocks, sample, startMs);
        }
    }

    private static final class ConstantToneSource implements AudioSource {
        private final short sample;
        private final int totalBlocks;
        private int blocksRead;
        private long positionMs;

        ConstantToneSource(int totalBlocks, short sample, long startMs) {
            this.totalBlocks = totalBlocks;
            this.sample = sample;
            this.positionMs = startMs;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (blocksRead >= totalBlocks) return -1;
            blocksRead++;
            for (int i = offset; i + 1 < offset + length; i += 2) {
                buffer[i] = (byte) (sample & 0xFF);
                buffer[i + 1] = (byte) ((sample >>> 8) & 0xFF);
            }
            positionMs += (long) (MusicFormat.blockSeconds(length) * 1000);
            return length;
        }

        @Override
        public long positionMs() {
            return positionMs;
        }

        @Override
        public void close() {
        }
    }
}
