package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.data.PowerDetails;
import elite.intel.gameapi.data.PowerPlayData;
import elite.intel.gameapi.journal.events.*;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Keeps the power the commander is pledged to level with the journal.
 * <p>
 * WHY the game load is the source of truth and not the day they joined: the game writes a
 * {@code Powerplay} event at every load for a pledged commander and nothing for one who is not. So
 * the newest load settles the pledge however long ago it was made, without the journal that recorded
 * the joining - which a commander may well have deleted or never kept. The {@code Commander} event
 * opens every load and comes before {@code Powerplay}, so it clears the pledge and {@code Powerplay},
 * if it follows, puts it back. Joining, leaving and defecting mid-session are the changes between loads.
 * <p>
 * Registered on the pre-scan bus as well, so a commander who starts the app with the game already
 * running has the pledge from that load before anything asks for it.
 * <p>
 * WHY on the bus thread: a load's clear and its pledge are two writes that must land in journal order.
 * On a thread each, the clear could land last and leave a pledged commander with no power.
 */
@SuppressWarnings("unused")
public class PowerPlaySubscriber {

    private static final Logger log = LogManager.getLogger(PowerPlaySubscriber.class);

    private final PlayerSession session = PlayerSession.getInstance();

    @Subscribe
    public void onCommander(CommanderEvent event) {
        if (event == null) return;
        pledge(RankAndProgressDto.NOT_PLEDGED, 0, 0, 0);
    }

    @Subscribe
    public void onPowerPlayEvent(PowerplayEvent event) {
        if (event == null || isBlank(event.getPower())) return;
        pledge(event.getPower(), event.getRank(), event.getMerits(), event.getTimePledged());
    }

    @Subscribe
    public void onPowerplayJoin(PowerplayJoinEvent event) {
        if (event == null || isBlank(event.getPower())) return;
        pledge(event.getPower(), 0, 0, 0);
    }

    @Subscribe
    public void onPowerplayLeave(PowerplayLeaveEvent event) {
        if (event == null) return;
        pledge(RankAndProgressDto.NOT_PLEDGED, 0, 0, 0);
    }

    /**
     * A defector keeps nothing of the old power's standing, so rank and merits start over with the new one.
     */
    @Subscribe
    public void onPowerplayDefect(PowerplayDefectEvent event) {
        if (event == null || isBlank(event.getToPower())) return;
        pledge(event.getToPower(), 0, 0, 0);
    }

    private void pledge(String power, int rank, int merits, long timePledged) {
        RankAndProgressDto rp = session.getRankAndProgressDto();
        rp.setPledgedToPower(power);
        rp.setAllegiance(allegianceOf(power));
        rp.setPowerRank(rank);
        rp.setMerrits(merits);
        rp.setTimePledged(timePledged);
        session.setRankAndProgressDto(rp);
    }

    private static String allegianceOf(String power) {
        if (RankAndProgressDto.NOT_PLEDGED.equals(power)) return RankAndProgressDto.NOT_PLEDGED;
        PowerDetails powerDetails = PowerPlayData.getPowerDetails(power);
        if (powerDetails != null) return powerDetails.allegiance();
        // WHY error: the elite.intel loggers run at ERROR, so a warning would never reach a support bundle - and a power
        // the game names that the app does not know (a new one Frontier added) is a gap to fix in PowerPlayData.
        log.error("Power [{}] is not in PowerPlayData; its allegiance is unknown until it is added", power);
        return "unknown";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
