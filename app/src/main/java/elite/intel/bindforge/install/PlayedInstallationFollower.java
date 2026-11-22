package elite.intel.bindforge.install;

import elite.intel.ui.controller.ManagedService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Follows the game from one installation to another (linux-several-installations.md, rule 3): every
 * {@value #LOOK_EVERY_SECONDS} seconds it finds each installation's newest journal, and when the newest of them all
 * belongs to another installation and was opened after the current choice, that installation becomes the one being
 * played ({@link PlayedInstallation#choose}).
 * <p>
 * The installations are worked out once, when it starts; with fewer than two there is nothing to follow and it stops
 * there - always on Windows, and on a Linux machine with one installation. It runs before the journal parser, whose
 * folder it may change. Reads only folder listings, never a journal's content.
 */
public final class PlayedInstallationFollower implements ManagedService {

    private static final Logger log = LogManager.getLogger(PlayedInstallationFollower.class);

    static final long LOOK_EVERY_SECONDS = 5;

    private Thread thread;

    @Override
    public synchronized void start() {
        if (thread != null && thread.isAlive()) return;
        thread = Thread.ofPlatform().name("PlayedInstallationFollower").daemon().start(this::follow);
    }

    @Override
    public synchronized void stop() {
        if (thread == null) return;
        thread.interrupt();
        try {
            thread.join(TimeUnit.SECONDS.toMillis(LOOK_EVERY_SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        thread = null;
    }

    private void follow() {
        try {
            List<PlayedInstallation.Choice> choices = PlayedInstallation.choices();
            if (choices.size() < 2) {
                log.debug("{} installation(s) to play, so there is nothing to follow", choices.size());
                return;
            }
            log.info("Following the game across {} installations", choices.size());
            while (!Thread.currentThread().isInterrupted()) {
                lookOnce(choices);
                TimeUnit.SECONDS.sleep(LOOK_EVERY_SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            // WHY: broad on purpose - a thread of its own. Elite-Intel is whole without it: the folders stay as they
            // are, the selector still chooses, and the next start tries again.
            log.error("Stopped following the game across installations", e);
        }
    }

    private static void lookOnce(List<PlayedInstallation.Choice> choices) {
        PlayedInstallation.follow(choices).ifPresent(choice -> log.info(
                "The game started in {} ({}), so it is the installation being played now", choice.storefront(),
                choice.root()));
    }
}
