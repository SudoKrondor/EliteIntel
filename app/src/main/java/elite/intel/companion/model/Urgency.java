package elite.intel.companion.model;

/**
 * Urgency of a thought/request. Set mechanically at thought birth (urgent phrase matcher for
 * commander input, urgent event-type list for events) and never decided by the LLM.
 * <p>
 * An URGENT thought jumps to the head of its queue and interrupts both live thoughts.
 */
public enum Urgency {
    NORMAL,
    URGENT
}
