package elite.intel.gameapi.gamestate.status_events;

/**
 * A body's biological survey has just been recorded as complete - or, when the commander takes that
 * record back, as not complete after all.
 * <p>
 * Published by {@code LocationManager.updateBody} when the survey-complete latch on a location row
 * flips, whoever flipped it: the third organic scan of the last genus, a detailed surface scan that
 * found nothing left, or the commander saying so. It is the one place every writer of that latch
 * passes through, so anything that needs to know a body is sampled out listens here rather than to
 * each of them.
 *
 * @param systemAddress the journal's SystemAddress
 * @param bodyId        the journal's BodyID within that system
 * @param completed     true when the survey was just marked complete, false when the mark was cleared
 */
public record BioSurveyCompletedEvent(long systemAddress, long bodyId, boolean completed) {
}
