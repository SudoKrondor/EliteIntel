package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindsValues;
import elite.intel.ui.widget.HudSlider;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The slider must never rewrite a value it merely shows, and players' files do go past the game's stops.
 */
class SettingsModePanelTest {

    private static final String THIRD_PERSON = "SAAThirdPersonMouseSensitivity";
    private static final String MOTION = "HeadlookMotionSensitivity";

    private final List<String> edits = new ArrayList<>();
    private final SettingsModePanel panel = new SettingsModePanel(new GameModePanel.EditRunner() {
        @Override
        public boolean running() {
            return false;
        }

        @Override
        public void run(String title, GameModePanel.Change change, Runnable onEdt, Runnable afterFailure) {
            edits.add(title);
        }
    });

    @Test
    void readingAFileMovesTheThumbButKeepsNoEdit() {
        show(THIRD_PERSON, "30.00000000");

        assertEquals(30_000, slider().getValue());
        assertEquals("30.0", field().getText());
        assertTrue(edits.isEmpty());
    }

    @Test
    void theNumberFollowsADragAndLettingGoKeepsOneEdit() {
        show(THIRD_PERSON, "30.00000000");
        HudSlider slider = slider();
        slider.setSize(300, 44);

        mouse(slider, MouseEvent.MOUSE_PRESSED, 150);
        mouse(slider, MouseEvent.MOUSE_DRAGGED, 200);
        assertNotEquals("30.0", field().getText());
        assertTrue(edits.isEmpty(), "kept an edit before the thumb was let go");

        mouse(slider, MouseEvent.MOUSE_RELEASED, 200);
        assertEquals(1, edits.size());
    }

    @Test
    void aValuePastTheStopPinsTheThumbAndStaysUntilMoved() {
        show(THIRD_PERSON, "500.00000000");
        HudSlider slider = slider();
        slider.setSize(300, 44);

        assertEquals(200_000, slider.getValue());
        assertEquals("500.0", field().getText());
        assertTrue(hasLabelContaining("200"), "nothing says the value is past the stop");

        mouse(slider, MouseEvent.MOUSE_PRESSED, 299);
        mouse(slider, MouseEvent.MOUSE_RELEASED, 299);

        assertEquals("500.0", field().getText());
        assertTrue(edits.isEmpty(), "a click on the end stop rewrote the value");
    }

    @Test
    void aValueBetweenStepsIsNotRewrittenByShowingIt() {
        show("MouseSensitivity", "1.52700000");

        assertEquals("1.527", field().getText());
        assertTrue(edits.isEmpty());
    }

    @Test
    void aSettingWithNoMeasuredRangeIsTheNumberAlone() {
        show(MOTION, "1.00000000");

        assertTrue(all(panel, HudSlider.class).isEmpty());
        assertEquals("1.0", field().getText());
    }

    @Test
    void aSettingTheEditorCannotChangeHasItsSliderOff() {
        panel.show("Custom.4.2.binds", new BindsValues(Map.of(),
                Map.of(THIRD_PERSON, new BindsValues.Setting(THIRD_PERSON, "30.00000000", false))));

        assertFalse(slider().isEnabled());
    }

    private void show(String id, String value) {
        panel.show("Custom.4.2.binds", new BindsValues(Map.of(), Map.of(id, new BindsValues.Setting(id, value, true))));
    }

    private HudSlider slider() {
        return all(panel, HudSlider.class).getFirst();
    }

    private JTextField field() {
        return all(panel, JTextField.class).getFirst();
    }

    private boolean hasLabelContaining(String text) {
        return all(panel, JLabel.class).stream().anyMatch(label -> label.getText() != null && label.getText().contains(text));
    }

    private static void mouse(HudSlider slider, int id, int x) {
        slider.dispatchEvent(new MouseEvent(slider, id, System.currentTimeMillis(), 0, x, 30, 1, false, MouseEvent.BUTTON1));
    }

    private static <T> List<T> all(Container parent, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) found.add(type.cast(child));
            if (child instanceof Container container) found.addAll(all(container, type));
        }
        return found;
    }
}
