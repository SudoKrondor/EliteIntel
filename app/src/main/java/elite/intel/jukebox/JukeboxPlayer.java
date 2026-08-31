package elite.intel.jukebox;

import elite.intel.db.dao.JukeboxDao;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.JukeboxStateChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Plays the commander's music, underneath the companion rather than in competition with it.
 * <p>
 * <b>How the ducking is timed.</b> One block of audio is read, scaled, and written at a time, and the
 * write blocks until the sound card has room - so the loop runs at the speed of the music itself, and
 * advancing {@link MusicDucker} once per block gives the duck a wall-clock accurate attack and release
 * without a timer anywhere. The gain is ramped across each block rather than applied as a step, which is
 * what keeps a moving duck, and a dragged volume slider, from clicking.
 * <p>
 * <b>Why it does not need the companion running.</b> A commander may want music with the companion shut
 * down, so the player is started on demand rather than as one of the application's services. With nothing
 * speaking, {@code VoiceLevelTap} simply reports silence and the music plays at full volume.
 */
public final class JukeboxPlayer {

    private static final Logger log = LogManager.getLogger(JukeboxPlayer.class);

    /**
     * How often the position is written down while playing. Often enough to be useful, rarely enough not to churn the database.
     */
    private static final long PERSIST_EVERY_MS = 5_000;

    /**
     * How many tracks "previous" can walk back through.
     */
    private static final int HISTORY_LIMIT = 64;

    private static final JukeboxPlayer INSTANCE = new JukeboxPlayer(
            JukeboxManager.getInstance(), JavaSoundMusicOutput::new, Mp3AudioSource::open, new MusicDucker());

    static {
        // WHY a JVM hook rather than a call in the window's close handler: the position is only worth
        // storing if it survives EVERY way out, and the application exits from more than one place. A hook
        // cannot be forgotten by a later exit path the way a call site can. It is a no-op when the jukebox
        // was never started, which is the usual case.
        Runtime.getRuntime().addShutdownHook(new Thread(INSTANCE::shutdown, "Jukebox-Shutdown"));
    }

    private final JukeboxManager library;
    private final Supplier<MusicOutput> outputFactory;
    private final SourceFactory sourceFactory;
    private final MusicDucker ducker;

    private final Object lock = new Object();
    private final Deque<Long> history = new ArrayDeque<>();
    private final byte[] block = new byte[MusicFormat.BLOCK_BYTES];

    private PlaybackState state = PlaybackState.STOPPED;
    private Long currentTrackId;
    private long resumeFromMs;
    private boolean trackSwitchPending;
    private boolean running;
    private Thread thread;

    private volatile int volume = 70;
    private volatile PlaybackOrder order = PlaybackOrder.SEQUENTIAL;
    private volatile long positionMs;

    // Playback thread only.
    private AudioSource source;
    private MusicOutput output;
    private float lastGain = 1f;
    private long lastPersistedAtMs;
    private int consecutiveFailures;

    JukeboxPlayer(JukeboxManager library,
                  Supplier<MusicOutput> outputFactory,
                  SourceFactory sourceFactory,
                  MusicDucker ducker) {
        this.library = library;
        this.outputFactory = outputFactory;
        this.sourceFactory = sourceFactory;
        this.ducker = ducker;
    }

    public static JukeboxPlayer getInstance() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------- lifecycle

    /**
     * Starts the playback thread and restores where the commander left off - without playing.
     * <p>
     * WHY it does not resume automatically: an application that starts talking, or playing music, the
     * moment it launches is one people learn to launch with the speakers off.
     */
    public void start() {
        synchronized (lock) {
            if (running) return;
            running = true;
            volume = library.volume();
            order = library.playbackOrder();
            JukeboxDao.State stored = library.state();
            currentTrackId = library.currentTrack().map(JukeboxDao.Track::getId).orElse(null);
            resumeFromMs = currentTrackId == null ? 0 : stored.getPositionMs();
            positionMs = resumeFromMs;
            state = PlaybackState.STOPPED;
            thread = new Thread(this::playbackLoop, "Jukebox-Playback");
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Stops playback and shuts the thread down.
     */
    public void shutdown() {
        Thread playback;
        synchronized (lock) {
            if (!running) return;
            rememberPosition();
            running = false;
            state = PlaybackState.STOPPED;
            playback = thread;
            lock.notifyAll();
        }
        if (playback != null) {
            playback.interrupt();
            try {
                playback.join(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ---------------------------------------------------------------- transport

    /**
     * Starts or resumes playback, picking the first track when nothing is selected.
     */
    public void play() {
        synchronized (lock) {
            if (currentTrackId == null) {
                Long first = firstTrackId();
                if (first == null) return;
                selectTrack(first, 0);
            }
            state = PlaybackState.PLAYING;
            lock.notifyAll();
        }
        publishState();
    }

    public void pause() {
        synchronized (lock) {
            if (state != PlaybackState.PLAYING) return;
            state = PlaybackState.PAUSED;
            rememberPosition();
        }
        flushOutput();
        publishState();
    }

    public void togglePlayPause() {
        if (state() == PlaybackState.PLAYING) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Stops playback and forgets the position within the track, but keeps the track selected.
     */
    public void stop() {
        synchronized (lock) {
            state = PlaybackState.STOPPED;
            resumeFromMs = 0;
            positionMs = 0;
            trackSwitchPending = true;
            library.rememberPosition(currentTrackId, 0);
            lock.notifyAll();
        }
        flushOutput();
        publishState();
    }

    /**
     * Moves the play-head to a point in the track the commander picked off the seek bar.
     * <p>
     * WHY it reopens the file rather than scrubbing: the decoder can only run forwards, so a jump is a
     * fresh read positioned at the target, and the sound card's buffer is thrown away so the move is heard
     * at once instead of a fifth of a second later. Nothing is played while the thumb is dragged - the
     * commander lands where they let go, which is what a seek bar is for.
     * <p>
     * Seeking a paused or stopped track only moves the play-head; playback resumes there when they ask
     * for it.
     */
    public void seekTo(long ms) {
        synchronized (lock) {
            if (currentTrackId == null) return;
            long target = Math.max(0, ms);
            resumeFromMs = target;
            positionMs = target;
            trackSwitchPending = true;
            library.rememberPosition(currentTrackId, target);
            lock.notifyAll();
        }
        flushOutput();
    }

    /**
     * Plays one track from its beginning - what double-clicking the playlist does.
     */
    public void playTrack(long trackId) {
        synchronized (lock) {
            selectTrack(trackId, 0);
            state = PlaybackState.PLAYING;
            lock.notifyAll();
        }
        publishState();
    }

    /**
     * Moves to the next track, by playlist order or at random depending on the setting.
     */
    public void next() {
        Long id = nextTrackId();
        if (id == null) {
            stop();
            return;
        }
        playTrack(id);
    }

    /**
     * Goes back to the previously played track.
     * <p>
     * WHY a history rather than the row above: in random order there is no row above - the track before
     * this one is whichever one the shuffle happened to pick, and only a history knows that.
     */
    public void previous() {
        Long id;
        synchronized (lock) {
            id = history.pollLast();
        }
        if (id == null) {
            id = neighbourTrackId(-1);
        }
        if (id == null) return;
        synchronized (lock) {
            currentTrackId = id;
            resumeFromMs = 0;
            trackSwitchPending = true;
            state = PlaybackState.PLAYING;
            lock.notifyAll();
        }
        publishState();
    }

    // ---------------------------------------------------------------- settings

    public void setVolume(int newVolume) {
        library.setVolume(newVolume);
        volume = library.volume();
    }

    public void setPlaybackOrder(PlaybackOrder newOrder) {
        library.setPlaybackOrder(newOrder);
        order = library.playbackOrder();
    }

    public PlaybackState state() {
        synchronized (lock) {
            return state;
        }
    }

    public Optional<Long> currentTrackId() {
        synchronized (lock) {
            return Optional.ofNullable(currentTrackId);
        }
    }

    public long positionMs() {
        return positionMs;
    }

    public boolean isPlaying() {
        return state() == PlaybackState.PLAYING;
    }

    // ---------------------------------------------------------------- the playback thread

    private void playbackLoop() {
        try {
            while (awaitPlaying()) {
                if (!ensureTrackOpen()) continue;
                pumpOneBlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            // Broad on purpose: this is a thread's top level, and an unhandled fault here would kill
            // playback silently for the rest of the session.
            log.error("Jukebox playback stopped unexpectedly", e);
        } finally {
            closeSource();
            closeOutput();
        }
    }

    /**
     * @return false once the player is shutting down
     */
    private boolean awaitPlaying() throws InterruptedException {
        synchronized (lock) {
            while (running && state != PlaybackState.PLAYING) {
                lock.wait();
            }
            return running;
        }
    }

    /**
     * @return true when a track is open and ready to read
     */
    private boolean ensureTrackOpen() {
        Long wanted;
        long from;
        boolean changed;
        synchronized (lock) {
            wanted = currentTrackId;
            from = resumeFromMs;
            changed = trackSwitchPending;
            trackSwitchPending = false;
        }
        if (source != null && !changed) return true;
        closeSource();
        if (wanted == null) return haltPlayback();

        JukeboxDao.Track track = library.track(wanted).orElse(null);
        if (track == null) return skipToNextAfterFailure();
        try {
            openOutput();
            source = sourceFactory.open(Path.of(track.getPath()), from);
            consecutiveFailures = 0;
            lastPersistedAtMs = System.currentTimeMillis();
            return true;
        } catch (Exception e) {
            log.warn("Jukebox cannot play {}: {}", track.getPath(), e.getMessage());
            library.markMissing(track.getId());
            return skipToNextAfterFailure();
        }
    }

    private void pumpOneBlock() {
        int read;
        try {
            read = source.read(block, 0, MusicFormat.BLOCK_BYTES);
        } catch (IOException e) {
            log.warn("Jukebox stopped reading a track early: {}", e.getMessage());
            read = -1;
        }
        if (read <= 0) {
            onTrackFinished();
            return;
        }
        float gain = ducker.advance(MusicFormat.blockSeconds(read)) * (volume / 100f);
        applyGainRamp(block, read, lastGain, gain);
        lastGain = gain;
        try {
            output.write(block, 0, read);
        } catch (RuntimeException e) {
            log.warn("Jukebox lost its audio device: {}", e.getMessage());
            closeOutput();
            haltPlayback();
            return;
        }
        reportPosition(source.positionMs());
        persistPeriodically();
    }

    /**
     * Publishes how far into the track playback has reached, unless the transport has moved on underneath.
     * <p>
     * WHY the guard: a block already in flight finishes after a stop or a track change, and writing its
     * position afterwards would undo the rewind - leaving a stopped player reporting a position it no
     * longer has, and writing that stale figure to the database on shutdown.
     */
    private void reportPosition(long ms) {
        synchronized (lock) {
            if (state != PlaybackState.PLAYING || trackSwitchPending) return;
            positionMs = ms;
        }
    }

    /**
     * Scales a block, sliding from the previous block's gain to this one's across it.
     * <p>
     * WHY a ramp and not a single multiplier: gain applied in steps, once per block, is a discontinuity in
     * the waveform at every boundary - audible as a click while the duck moves and as a buzz while the
     * volume slider is dragged. Sliding between the two removes both for the cost of one add per sample.
     */
    static void applyGainRamp(byte[] pcm, int length, float startGain, float endGain) {
        int samples = length / 2;
        if (samples == 0) return;
        float stepPerSample = (endGain - startGain) / samples;
        for (int i = 0; i < samples; i++) {
            int at = i * 2;
            short sample = (short) ((pcm[at + 1] << 8) | (pcm[at] & 0xFF));
            int scaled = Math.round(sample * (startGain + stepPerSample * i));
            if (scaled > Short.MAX_VALUE) scaled = Short.MAX_VALUE;
            else if (scaled < Short.MIN_VALUE) scaled = Short.MIN_VALUE;
            pcm[at] = (byte) (scaled & 0xFF);
            pcm[at + 1] = (byte) ((scaled >>> 8) & 0xFF);
        }
    }

    private void onTrackFinished() {
        closeSource();
        Long id = nextTrackId();
        if (id == null) {
            synchronized (lock) {
                library.rememberPosition(currentTrackId, 0);
            }
            haltPlayback();
            publishState();
            return;
        }
        synchronized (lock) {
            pushHistory(currentTrackId);
            currentTrackId = id;
            resumeFromMs = 0;
            positionMs = 0;
            trackSwitchPending = true;
        }
        publishState();
    }

    private boolean skipToNextAfterFailure() {
        consecutiveFailures++;
        int playlistSize = Math.max(1, library.size());
        if (consecutiveFailures >= playlistSize) {
            // Every remaining track failed to open - an unmounted drive, most likely. Stop rather than
            // spin through the whole playlist forever.
            log.warn("Jukebox stopping: no playable tracks in the playlist");
            consecutiveFailures = 0;
            haltPlayback();
            publishState();
            return false;
        }
        Long id = nextTrackId();
        if (id == null) {
            haltPlayback();
            publishState();
            return false;
        }
        synchronized (lock) {
            currentTrackId = id;
            resumeFromMs = 0;
            trackSwitchPending = true;
        }
        return false;
    }

    private boolean haltPlayback() {
        synchronized (lock) {
            state = PlaybackState.STOPPED;
        }
        return false;
    }

    // ---------------------------------------------------------------- picking tracks

    private Long nextTrackId() {
        return order == PlaybackOrder.RANDOM ? randomTrackId() : neighbourTrackId(1);
    }

    private Long neighbourTrackId(int offset) {
        List<JukeboxDao.Track> playlist = library.playlist();
        if (playlist.isEmpty()) return null;
        Long current = currentTrackId().orElse(null);
        int index = indexOf(playlist, current);
        if (index < 0) return playlist.get(0).getId();
        int wanted = index + offset;
        if (wanted < 0 || wanted >= playlist.size()) return null;
        return playlist.get(wanted).getId();
    }

    private Long randomTrackId() {
        List<JukeboxDao.Track> playlist = library.playlist();
        if (playlist.isEmpty()) return null;
        if (playlist.size() == 1) return playlist.get(0).getId();
        Long current = currentTrackId().orElse(null);
        int index = indexOf(playlist, current);
        int pick = ThreadLocalRandom.current().nextInt(playlist.size() - (index < 0 ? 0 : 1));
        // Skip over the current track rather than re-rolling, so the same track never repeats immediately.
        if (index >= 0 && pick >= index) pick++;
        return playlist.get(pick).getId();
    }

    private static int indexOf(List<JukeboxDao.Track> playlist, Long trackId) {
        if (trackId == null) return -1;
        for (int i = 0; i < playlist.size(); i++) {
            if (playlist.get(i).getId() == trackId) return i;
        }
        return -1;
    }

    private Long firstTrackId() {
        List<JukeboxDao.Track> playlist = library.playlist();
        return playlist.isEmpty() ? null : playlist.get(0).getId();
    }

    // ---------------------------------------------------------------- plumbing

    private void selectTrack(long trackId, long fromMs) {
        pushHistory(currentTrackId);
        currentTrackId = trackId;
        resumeFromMs = fromMs;
        positionMs = fromMs;
        trackSwitchPending = true;
    }

    private void pushHistory(Long trackId) {
        if (trackId == null) return;
        history.addLast(trackId);
        while (history.size() > HISTORY_LIMIT) {
            history.pollFirst();
        }
    }

    private void openOutput() throws Exception {
        if (output != null) return;
        MusicOutput opened = outputFactory.get();
        opened.open();
        output = opened;
    }

    private void flushOutput() {
        MusicOutput current = output;
        if (current != null) current.flush();
    }

    private void closeOutput() {
        MusicOutput current = output;
        output = null;
        if (current != null) current.close();
    }

    private void closeSource() {
        AudioSource current = source;
        source = null;
        if (current != null) current.close();
    }

    private void persistPeriodically() {
        long now = System.currentTimeMillis();
        if (now - lastPersistedAtMs < PERSIST_EVERY_MS) return;
        lastPersistedAtMs = now;
        rememberPosition();
    }

    private void rememberPosition() {
        library.rememberPosition(currentTrackId, positionMs);
    }

    private void publishState() {
        PlaybackState snapshot;
        Long trackId;
        synchronized (lock) {
            snapshot = state;
            trackId = currentTrackId;
        }
        UiBus.publish(new JukeboxStateChangedEvent(snapshot, trackId));
    }

    /**
     * Opens a track for reading, positioned where playback should resume. A seam for testing.
     */
    @FunctionalInterface
    public interface SourceFactory {
        AudioSource open(Path file, long startMs) throws IOException;
    }
}
