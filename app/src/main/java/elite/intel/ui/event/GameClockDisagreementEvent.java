package elite.intel.ui.event;

import java.time.Duration;

/**
 * The game's clock and the PC's disagree by a minute or more ({@code GameClock.WARNING_THRESHOLD}), published once
 * per app run so the commander is told to fix it.
 *
 * @param offset     how far the game's stamps run behind the PC clock (negative: ahead of it)
 * @param wholeHours the offset as a whole number of hours, or 0 when it is not one - the time-zone case
 */
public record GameClockDisagreementEvent(Duration offset, long wholeHours) {
}
