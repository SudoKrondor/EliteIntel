package elite.intel.session;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.RadioTransmissionStateChangedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The radio switch has two hands on it - the Commander tab's checkbox and the spoken toggle command - and
 * the radio volume slider must follow both. Rather than have each hand announce the flip, the session's one
 * write path does, so a new caller cannot forget to.
 */
class RadioTransmissionStateEventTest {

    private final List<RadioTransmissionStateChangedEvent> seen = new ArrayList<>();
    private Boolean before;

    @Subscribe
    public void onState(RadioTransmissionStateChangedEvent event) {
        seen.add(event);
    }

    @BeforeEach
    void listen() {
        before = PlayerSession.getInstance().isRadioTransmissionOn();
        UiBus.register(this);
    }

    @AfterEach
    void restore() {
        UiBus.unregister(this);
        PlayerSession.getInstance().setRadioTransmissionOn(before);
    }

    @Test
    void everyFlipOfTheSwitchIsAnnouncedWithTheNewState() {
        PlayerSession session = PlayerSession.getInstance();

        session.setRadioTransmissionOn(true);
        session.setRadioTransmissionOn(false);

        assertEquals(List.of(true, false), seen.stream().map(RadioTransmissionStateChangedEvent::on).toList());
        assertEquals(false, session.isRadioTransmissionOn(), "the flip was also persisted");
    }

    @Test
    void anUnsetSwitchIsAnnouncedAsOff() {
        PlayerSession.getInstance().setRadioTransmissionOn(null);

        assertEquals(List.of(false), seen.stream().map(RadioTransmissionStateChangedEvent::on).toList());
    }
}
