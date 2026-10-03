package elite.intel.gameapi;

import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.gameapi.journal.events.CommanderEvent;
import elite.intel.gameapi.journal.events.LoadGameEvent;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Follows the game from one commander to another.
 * <p>
 * The database is opened for the commander of the newest journal at startup. When the game then loads a session
 * for a different commander, their {@code Commander} event is the first thing the journal says about them, and
 * this switches the database to that commander's own file before the event goes anywhere.
 * <p>
 * WHY in the parser, before publishing, rather than in a subscriber: subscribers of one event run in no promised
 * order, and {@code FinanceSubscriber} writes on {@code LoadGame}. A switch made by a sibling subscriber could come
 * after a write had already landed in the previous commander's file. Nothing about the event may be handled until
 * the right file is open.
 */
public final class CommanderSwitch {

    private static final Logger log = LogManager.getLogger(CommanderSwitch.class);

    private CommanderSwitch() {
    }

    /**
     * Opens the database of the commander this event names, if it is not the one already open. Every other event
     * passes straight through.
     * <p>
     * {@code LoadGame} carries the FID too and is checked as well: it is the backstop for a session whose
     * {@code Commander} line was written just before the app started and so never arrived live.
     */
    public static void beforePublishing(BaseEvent event) {
        String fid = switch (event) {
            case CommanderEvent commander -> commander.getFID();
            case LoadGameEvent loadGame -> loadGame.getFID();
            default -> null;
        };
        if (fid != null) {
            ensureOpenFor(fid);
        }
    }

    static void ensureOpenFor(String fid) {
        if (!Database.switchCommander(fid)) return;
        log.info("Commander {} loaded: their own database is open", fid);
        // Seed their file from their own journals, synchronously, on the parser's thread. Run beside the live
        // parser instead, a replayed Loadout from their previous session could land after, and overwrite, the one
        // this session is about to write.
        JournalPreScanner.scan(PlayerSession.getInstance().getJournalPath());
    }
}
