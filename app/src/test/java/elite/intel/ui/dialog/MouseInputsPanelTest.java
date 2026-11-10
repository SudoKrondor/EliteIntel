package elite.intel.ui.dialog;

import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the mouse buttons as the game numbers them (E4): AWT counts left, middle, right, the game left, right,
 * middle, and nothing caps the count.
 */
class MouseInputsPanelTest {

    @Test
    void theLeftButtonIsMouse1() {
        assertEquals(Optional.of("Mouse_1"), MouseInputsPanel.tokenForAwtButton(MouseEvent.BUTTON1));
    }

    @Test
    void middleAndRightSwapBecauseTheGameCountsRightSecond() {
        assertEquals(Optional.of("Mouse_3"), MouseInputsPanel.tokenForAwtButton(MouseEvent.BUTTON2));
        assertEquals(Optional.of("Mouse_2"), MouseInputsPanel.tokenForAwtButton(MouseEvent.BUTTON3));
    }

    @Test
    void sideButtonsAndAnyPastThemKeepTheirNumber() {
        assertEquals(Optional.of("Mouse_4"), MouseInputsPanel.tokenForAwtButton(4));
        assertEquals(Optional.of("Mouse_5"), MouseInputsPanel.tokenForAwtButton(5));
        assertEquals(Optional.of("Mouse_9"), MouseInputsPanel.tokenForAwtButton(9));
    }

    @Test
    void noButtonIsNoInput() {
        assertEquals(Optional.empty(), MouseInputsPanel.tokenForAwtButton(MouseEvent.NOBUTTON));
    }
}
