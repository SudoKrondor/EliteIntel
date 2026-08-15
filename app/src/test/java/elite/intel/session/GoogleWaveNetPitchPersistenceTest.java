package elite.intel.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleWaveNetPitchPersistenceTest {

    @AfterEach
    void restoreNativePitch() {
        SystemSession.getInstance().setGoogleWaveNetPitch(0);
    }

    @Test
    void configuredPitchRoundTripsThroughTheSessionDatabase() {
        SystemSession session = SystemSession.getInstance();

        session.setGoogleWaveNetPitch(-8);

        assertEquals(-8, session.getGoogleWaveNetPitch());
    }

    @Test
    void pitchMustStayWithinTheGoogleApiRange() {
        SystemSession session = SystemSession.getInstance();

        assertThrows(IllegalArgumentException.class, () -> session.setGoogleWaveNetPitch(-21));
        assertThrows(IllegalArgumentException.class, () -> session.setGoogleWaveNetPitch(21));
    }
}
