package elite.intel.ui.event;

/**
 * The commander turned push-to-talk on or off, or switched between hold and toggle mode.
 * <p>
 * Carries no payload: the settings themselves live in {@code SystemSession}, and the service that acts on them
 * reads them there, so there is never a second copy of the mapping to keep in step. Published only for the
 * enable flag and the mode, which change the policy; picking a different controller or button changes nothing
 * until the next press, which reads the current selection anyway.
 */
public class PushToTalkSettingsChangedEvent {
}
