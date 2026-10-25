package elite.intel.gameapi;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.GameClockDisagreementEvent;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * How far the game's UTC stamps run behind (or ahead of) the PC clock Java reads, measured from the stamps
 * themselves.
 * <p>
 * WHY it exists: commanders have reported, after a Windows Update, a game that stamps the journal an hour away from
 * the UTC Java gets - fixed by changing the Windows time zone. Every freshness rule compares a game stamp with
 * {@link Instant#now()} (the 10-second "outdated" cut in {@code EventRegistry}, the app-start replay cut in
 * {@code BaseEvent}), so on such a PC every live event is silently thrown away and the app looks dead.
 * <p>
 * The measurement: a stamp read moments after the game wrote it is {@code now - stamp = offset + delay}, and the
 * delay is never negative. The smallest age over recent fresh stamps is therefore the offset, with at most about a
 * second of noise (stamps are whole seconds). Only fresh stamps may be sampled - a backlog only ever reads older and
 * would overstate it - so callers offer a changed {@code Status.json} stamp and journal lines read a few at a time.
 * <p>
 * Below {@link #CORRECTION_THRESHOLD} the offset is noise and nothing is corrected, so a healthy PC behaves exactly as
 * before. The commander is only warned from {@link #WARNING_THRESHOLD}: a Windows clock drifts by seconds between its
 * weekly syncs, and every comparison in the app is either corrected or made in game time, so a drift of seconds
 * breaks nothing they could fix.
 * <p>
 * The correction is only good to about a second. Anything that needs better - a stamp compared with a moment the app
 * itself chose, like a key press - compares game time with game time instead, through {@link #newestStamp()}.
 */
public final class GameClock {

    private static final Logger log = LogManager.getLogger(GameClock.class);

    /**
     * An offset this large is a clock disagreement, not stamp rounding plus a poll interval.
     */
    static final Duration CORRECTION_THRESHOLD = Duration.ofSeconds(5);
    /**
     * An offset this large is worth the commander's attention: over 59 seconds is a clock that is plainly wrong, not
     * a Windows clock between its weekly syncs, and other Elite Dangerous tools on the PC will not correct it.
     */
    static final Duration WARNING_THRESHOLD = Duration.ofSeconds(60);
    /**
     * Fresh stamps needed before an offset is believed.
     */
    static final int MIN_SAMPLES = 5;
    /**
     * How many recent fresh stamps the minimum is taken over.
     */
    static final int WINDOW = 50;
    /**
     * A measured offset that moves by this much is reported again.
     */
    private static final Duration REPORT_STEP = Duration.ofSeconds(2);
    /**
     * A whole number of hours, give or take this, reads as a time-zone disagreement.
     */
    private static final Duration WHOLE_HOUR_TOLERANCE = Duration.ofSeconds(90);

    private static final GameClock INSTANCE = new GameClock(Clock.systemUTC());

    private final Clock pcClock;
    private final Deque<Duration> ages = new ArrayDeque<>();
    private Duration measured;
    private Duration reported;
    private Instant newestStamp;
    /**
     * The commander has been shown (and told) the disagreement this run. Once is enough: the correction keeps the app going,
     * and a warning on every drift of the measurement would be nagging.
     */
    private boolean warned;
    private volatile Duration correction = Duration.ZERO;

    GameClock(Clock pcClock) {
        this.pcClock = pcClock;
    }

    public static GameClock get() {
        return INSTANCE;
    }

    /**
     * When the PC clock says a game stamp happened: the stamp shifted by the measured offset once it is large enough
     * to be real, the stamp itself otherwise.
     */
    public Instant toPcTime(Instant gameStamp) {
        return gameStamp.plus(correction);
    }

    /**
     * The latest stamp the game has written that the app has read: a moment the game's own clock has certainly
     * reached. A key pressed now gets a reply stamped no earlier than this, whatever the PC clock says. Empty until
     * the first fresh stamp.
     */
    public synchronized Optional<Instant> newestStamp() {
        return Optional.ofNullable(newestStamp);
    }

    /**
     * Offers a stamp the game wrote moments ago. An unparseable one is not a sample.
     */
    public void observe(String gameStamp) {
        if (gameStamp == null) return;
        try {
            observe(Instant.parse(gameStamp));
        } catch (DateTimeParseException e) {
            // WHY: not a failure of the clock check - the journal reader already warns about a bad stamp on the
            // event that carries it. One sample fewer changes nothing.
            log.debug("Not a clock sample: {}", gameStamp);
        }
    }

    void observe(Instant gameStamp) {
        String report = null;
        GameClockDisagreementEvent warning = null;
        boolean correcting;
        synchronized (this) {
            if (newestStamp == null || gameStamp.isAfter(newestStamp)) newestStamp = gameStamp;
            ages.addLast(Duration.between(gameStamp, pcClock.instant()));
            if (ages.size() > WINDOW) ages.removeFirst();
            if (ages.size() < MIN_SAMPLES) return;
            measured = ages.stream().min(Duration::compareTo).orElseThrow();
            correction = measured.abs().compareTo(CORRECTION_THRESHOLD) >= 0 ? measured : Duration.ZERO;
            correcting = !correction.isZero();
            if (!warned && measured.abs().compareTo(WARNING_THRESHOLD) >= 0) {
                warned = true;
                warning = new GameClockDisagreementEvent(measured, wholeHours(measured));
            }
            if (reported == null || measured.minus(reported).abs().compareTo(REPORT_STEP) >= 0) {
                reported = measured;
                report = describeLocked();
            }
        }
        if (report != null) {
            log.info("Game clock check: {}", report);
            if (correcting) UiBus.publish(new AppLogEvent("SYSTEM: Game clock check: " + report));
        }
        if (warning != null) {
            // WHY spoken too: the game is usually fullscreen, so the dialog waits unseen behind it while every
            // command seems broken. One line sends the commander to it. Straight to TTS, like GameEditionCheck.
            GameEventBus.publish(new VocalisationRequestEvent(
                    StringUtls.localizedSpeech("speech.gameClockDisagreement"),
                    MissionCriticalAnnouncementEvent.class, false));
            UiBus.publish(warning);
        }
    }

    /**
     * One line for the support bundle and the log: what was measured, and whether it is being corrected.
     */
    public synchronized String describe() {
        return describeLocked();
    }

    private String describeLocked() {
        if (measured == null) {
            return "not measured yet (" + ages.size() + " of " + MIN_SAMPLES + " fresh samples)";
        }
        long seconds = measured.getSeconds();
        String direction = seconds == 0 ? "level with" : Math.abs(seconds) + " s " + (seconds > 0 ? "behind" : "ahead of");
        String text = "game stamps run " + direction + " the PC clock (smallest of " + ages.size() + " fresh samples)";
        if (correction.isZero()) return text + ", no correction";
        return text + ", correcting for it" + wholeHoursHint();
    }

    private String wholeHoursHint() {
        long hours = wholeHours(measured);
        if (hours == 0) return "";
        return " - " + hours + " whole hour" + (hours == 1 ? "" : "s")
                + ": the game and Windows disagree about the time zone";
    }

    /**
     * The offset's size as a whole number of hours, give or take {@link #WHOLE_HOUR_TOLERANCE}, or 0 when it is not
     * one. A whole-hour gap is a time-zone disagreement, anything else a clock that has drifted.
     */
    static long wholeHours(Duration offset) {
        long hours = Math.round(offset.abs().getSeconds() / 3600.0);
        if (hours == 0) return 0;
        Duration fromWholeHours = offset.abs().minus(Duration.ofHours(hours)).abs();
        return fromWholeHours.compareTo(WHOLE_HOUR_TOLERANCE) > 0 ? 0 : hours;
    }
}
