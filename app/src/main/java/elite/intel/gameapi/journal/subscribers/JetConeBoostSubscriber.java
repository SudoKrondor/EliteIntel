package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.db.dao.NeutronStarRouteDao;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.db.managers.NeutronStarRouteManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.JetConeBoostEvent;
import elite.intel.session.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * Plots the next leg of a neutron highway the moment the commander supercharges at one.
 * <p>
 * Flying such a route is the same three moves at every waypoint - arrive, fly the jet cone, ask for a
 * course to the next waypoint - and commanders asked for the third to stop being their job. The boost is
 * the signal to hang it off: it is written exactly once per waypoint, and only when the commander has
 * actually taken the charge they came for.
 * <p>
 * <b>Why it warns first and waits.</b> Plotting opens the galaxy map, which takes the commander's view
 * away for several seconds. Inside a jet cone that is a hull-temperature problem, so the callout goes out
 * first and the map is not opened until {@link #WARNING_LEAD_MS} later, which is time enough to fly clear.
 * <p>
 * <b>Why it is opt-in and off by default.</b> A galaxy map that opens on its own is alarming to a
 * commander who did not ask for it, and there is no way to tell from the journal whether this boost was
 * taken to follow the route or for a jump of their own. The checkbox on the Ship Options tab is the
 * statement of intent that the journal cannot give us.
 */
@SuppressWarnings("unused")//registered in SubscriberRegistration
public class JetConeBoostSubscriber {

    private static final Logger log = LogManager.getLogger(JetConeBoostSubscriber.class);

    /**
     * How long the commander gets between the callout and the galaxy map opening.
     * <p>
     * The spoken warning names this figure in words, so the two move together: change one and
     * {@code event.neutron.autoPlot} has to change in all nine bundles.
     */
    static final long WARNING_LEAD_MS = 10_000;

    private final NeutronStarRouteManager neutronRoute = NeutronStarRouteManager.getInstance();
    private final GlobalSettingsManager globalSettings = GlobalSettingsManager.getInstance();
    private final Status status = Status.getInstance();

    /**
     * Guards against a second boost arriving while a plot is still being announced or typed. Nothing good
     * comes of two galaxy-map sequences racing each other through the same keyboard.
     */
    private final AtomicBoolean plotInFlight = new AtomicBoolean();

    private final long warningLeadMs;
    private final RoutePlotting plotter;
    private final Executor executor;

    public JetConeBoostSubscriber() {
        this(WARNING_LEAD_MS,
                destination -> new RoutePlotter().plotRoute(destination),
                task -> Thread.ofVirtual().name("Neutron-Auto-Plot").start(task));
    }

    /**
     * Test seam: a lead the suite does not have to sit out, a plotter that types nothing, and an executor
     * that runs the work on the calling thread.
     */
    JetConeBoostSubscriber(long warningLeadMs, RoutePlotting plotter, Executor executor) {
        this.warningLeadMs = warningLeadMs;
        this.plotter = plotter;
        this.executor = executor;
    }

    @Subscribe
    public void onJetConeBoost(JetConeBoostEvent event) {
        if (!globalSettings.getAutoPlotNextNeutronJump()) return;
        // Plotting taps the ship-only galaxy map binding, exactly as the spoken command does. A boost
        // taken in a fighter or with the commander on foot has no route for us to lay in.
        if (!status.isInMainShip()) return;
        String waypoint = nextWaypoint();
        if (waypoint == null) return;
        if (!plotInFlight.compareAndSet(false, true)) return;

        executor.execute(() -> {
            try {
                EventNarrator.say(localizedEvent("event.neutron.autoPlot", waypoint));
                Thread.sleep(warningLeadMs);
                plotWhatIsStillTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                plotInFlight.set(false);
            }
        });
    }

    /**
     * Plots the next waypoint, re-reading everything the wait could have changed.
     * <p>
     * Ten seconds is long enough for the commander to switch the option off, jump away - which retires the
     * leg this was going to plot - or leave the ship. Acting on what was true when the boost landed would
     * open the galaxy map on a commander who has since done something else entirely.
     */
    private void plotWhatIsStillTrue() {
        if (!globalSettings.getAutoPlotNextNeutronJump()) return;
        if (!status.isInMainShip()) return;
        String waypoint = nextWaypoint();
        if (waypoint == null) return;
        log.debug("Jet cone boost: plotting neutron waypoint {}", waypoint);
        String note = plotter.plotRoute(waypoint);
        // plotRoute answers only when it did NOT plot - already on course, or nowhere to go. Dropping that
        // leaves a commander who heard the warning watching for a galaxy map that never opens.
        if (note != null && !note.isBlank()) {
            EventNarrator.say(note);
        }
    }

    /**
     * The first leg still to fly, or null when there is no neutron route on. Arrival retires a leg, so the
     * first one left is always the waypoint after the star being boosted at.
     */
    private String nextWaypoint() {
        NeutronStarRouteDao.Route route = neutronRoute.getNeutronStarRoute();
        if (route == null) return null;
        List<NeutronStarRouteDao.Route.Leg> legs = route.getLegs();
        if (legs == null || legs.isEmpty() || legs.getFirst() == null) return null;
        String systemName = legs.getFirst().getSystemName();
        return systemName == null || systemName.isBlank() ? null : systemName;
    }

    /**
     * What {@link RoutePlotter#plotRoute(String)} does, as a seam a test can stand in for.
     */
    @FunctionalInterface
    public interface RoutePlotting {
        String plotRoute(String destination);
    }
}
