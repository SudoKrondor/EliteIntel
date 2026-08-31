package elite.intel.jukebox;

import elite.intel.db.dao.JukeboxDao;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.JukeboxPlaylistChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;

/**
 * Fills in the title, artist, album and duration of everything in the playlist, in the background.
 * <p>
 * <b>Why this is not done as files are added.</b> Reading a tag means opening a file, and a library runs
 * to thousands of them on a disk that may be spinning or across a network. Doing that before the playlist
 * could be shown would leave the commander watching a frozen window for a minute over a column of text.
 * Rows go in from their path alone and this fills them in afterwards, a batch at a time, so the list is
 * usable immediately and completes itself while they use it.
 * <p>
 * A file that cannot be read is still marked as read. It has nothing to tell us this time and will have
 * nothing to tell us next time either, so retrying it on every launch would be a permanent cost for a
 * permanently empty answer.
 */
public final class TagScanner {

    private static final Logger log = LogManager.getLogger(TagScanner.class);

    private static final TagScanner INSTANCE = new TagScanner(JukeboxManager.getInstance(), new JAudioTaggerTagReader());

    private final JukeboxManager library;
    private final TrackTagReader reader;

    private final Object lock = new Object();
    private boolean running;
    private boolean workPending;
    private Thread thread;

    TagScanner(JukeboxManager library, TrackTagReader reader) {
        this.library = library;
        this.reader = reader;
    }

    public static TagScanner getInstance() {
        return INSTANCE;
    }

    /**
     * Starts the scanner and looks for anything left unread by an earlier session.
     */
    public void start() {
        synchronized (lock) {
            if (running) return;
            running = true;
            workPending = true;
            thread = new Thread(this::scanLoop, "Jukebox-TagScanner");
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Tells the scanner there are new files to read. Returns immediately.
     */
    public void requestScan() {
        synchronized (lock) {
            workPending = true;
            lock.notifyAll();
        }
    }

    public void shutdown() {
        Thread scanning;
        synchronized (lock) {
            if (!running) return;
            running = false;
            scanning = thread;
            lock.notifyAll();
        }
        if (scanning != null) scanning.interrupt();
    }

    private void scanLoop() {
        try {
            while (awaitWork()) {
                scanOneBatch();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            // A thread's top level: an unhandled fault here would leave every future track untagged for
            // the rest of the session with nothing said about it.
            log.error("Jukebox tag scanning stopped unexpectedly", e);
        }
    }

    /**
     * @return false once the scanner is shutting down
     */
    private boolean awaitWork() throws InterruptedException {
        synchronized (lock) {
            while (running && !workPending) {
                lock.wait();
            }
            return running;
        }
    }

    private void scanOneBatch() {
        List<JukeboxDao.Track> batch = library.awaitingTagScan();
        if (batch.isEmpty()) {
            synchronized (lock) {
                workPending = false;
            }
            return;
        }
        for (JukeboxDao.Track track : batch) {
            TrackTags tags = readOrGiveUp(track);
            library.recordTags(track.getId(), tags.title(), tags.artist(), tags.album(),
                    tags.trackNumber(), tags.durationMs());
        }
        UiBus.publish(new JukeboxPlaylistChangedEvent());
    }

    private TrackTags readOrGiveUp(JukeboxDao.Track track) {
        try {
            return reader.read(Path.of(track.getPath()));
        } catch (Exception e) {
            log.warn("No tags readable from {}: {}", track.getPath(), e.getMessage());
            return TrackTags.UNKNOWN;
        }
    }
}
