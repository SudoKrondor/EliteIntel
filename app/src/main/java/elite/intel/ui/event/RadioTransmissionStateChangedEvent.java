package elite.intel.ui.event;

/**
 * Published whenever the radio-transmissions switch moves, by whichever hand moved it - the Commander tab's
 * checkbox or the spoken toggle command - so a view that depends on the state (the radio volume slider,
 * which is dead while the radio is off) is told by one place instead of polling.
 */
public record RadioTransmissionStateChangedEvent(boolean on) {
}
