package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindingSaveResult;
import elite.intel.bindforge.io.BindsValues;
import elite.intel.ui.widget.HudToggleButton;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A toggle shows the file's value and keeps no edit until it is pressed (bind-editor.md, <em>Toggles</em>); a press
 * writes {@code 1} or {@code 0}, and a press that cannot be kept puts the button back (E10R, D4).
 */
class TogglesModePanelTest {

    private final List<String> edits = new ArrayList<>();
    private final List<String> written = new ArrayList<>();
    private final AtomicBoolean readOnly = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    /** What the write answers; a failure instead when {@link #failure} is set. */
    private BindingSaveResult answer = BindingSaveResult.SAVED;
    private IOException failure;
    /**
     * Runs the change at once, then what follows it on the EDT - or, when the change throws, what follows a failure.
     */
    private final TogglesModePanel panel = new TogglesModePanel(new GameModePanel.EditRunner() {
        @Override
        public boolean running() {
            return running.get();
        }

        @Override
        public void run(String title, GameModePanel.Change change, Runnable onEdt, Runnable afterFailure) {
            edits.add(title);
            try {
                change.run();
            } catch (IOException e) {
                afterFailure.run();
                return;
            }
            onEdt.run();
        }
    }, readOnly::get, (file, setting, value) -> {
        if (failure != null) throw failure;
        written.add(setting + "=" + value);
        return answer;
    });

    @Test
    void oneIsOnAndZeroIsOffAndShowingKeepsNoEdit() {
        show(Map.of("MouseGUI", "1", "EnableMenuGroups", "0"));

        List<HudToggleButton> buttons = buttons();
        assertEquals(2, buttons.size());
        assertTrue(buttons.get(0).isSelected(), "MouseGUI, Mouse Controls, comes before Miscellaneous");
        assertFalse(buttons.get(1).isSelected());
        assertTrue(edits.isEmpty());
    }

    @Test
    void aValueTheGameWouldNotWriteReadsOffAndIsLeftUntilPressed() {
        show(Map.of("MouseGUI", "yes"));

        assertFalse(buttons().getFirst().isSelected());
        assertTrue(buttons().getFirst().isEnabled());
        assertTrue(edits.isEmpty());

        buttons().getFirst().doClick();

        assertEquals(1, edits.size());
        assertTrue(buttons().getFirst().isSelected());
    }

    @Test
    void aPressKeepsOneEdit() {
        show(Map.of("MouseGUI", "0"));

        buttons().getFirst().doClick();

        assertEquals(1, edits.size());
    }

    @Test
    void everyToggleTheFileHoldsHasARowAndNothingElseDoes() {
        show(Map.of("MouseGUI", "1", "MouseSensitivity", "1.00000000", "ThrottleRange", ""));

        assertEquals(1, buttons().size());
    }

    @Test
    void aSettingTheEditorCannotChangeIsOffAndSaysWhy() {
        panel.show("Custom.4.2.binds", new BindsValues(Map.of(),
                Map.of("MouseGUI", new BindsValues.Setting("MouseGUI", "1", false))));

        assertFalse(buttons().getFirst().isEnabled());
        assertNotNull(buttons().getFirst().getToolTipText());
    }

    @Test
    void readOnlyGreysEveryToggleAndAPressKeepsNoEdit() {
        readOnly.set(true);
        show(Map.of("MouseGUI", "1"));

        assertFalse(buttons().getFirst().isEnabled());
        assertNotNull(buttons().getFirst().getToolTipText());
        assertTrue(buttons().getFirst().isSelected(), "read only still shows the value");

        readOnly.set(false);
        panel.readOnlyChanged();

        assertTrue(buttons().getFirst().isEnabled());
        assertNull(buttons().getFirst().getToolTipText());
    }

    @Test
    void onWritesOneAndOffWritesZero() {
        show(Map.of("MouseGUI", "0"));
        buttons().getFirst().doClick();

        show(Map.of("MouseGUI", "1"));
        buttons().getFirst().doClick();

        assertEquals(List.of("MouseGUI=1", "MouseGUI=0"), written);
    }

    @Test
    void aRefusedWritePutsTheButtonBackAndSaysWhy() {
        answer = BindingSaveResult.UNSUPPORTED_XML;
        show(Map.of("MouseGUI", "0"));

        buttons().getFirst().doClick();

        HudToggleButton button = buttons().getFirst();
        assertFalse(button.isSelected());
        assertEquals(getText("bindings.toggles.off").toUpperCase(), button.getText());
        assertTrue(labelTexts().contains(getText("bindings.settings.notEditable")));
    }

    @Test
    void aChangeThatFailsPutsTheButtonBack() {
        failure = new IOException("the edits could not be read");
        show(Map.of("MouseGUI", "1"));

        buttons().getFirst().doClick();

        HudToggleButton button = buttons().getFirst();
        assertTrue(button.isSelected());
        assertEquals(getText("bindings.toggles.on").toUpperCase(), button.getText());
    }

    @Test
    void aPressWhileAnEditRunsPutsTheButtonBackAndKeepsNoEdit() {
        running.set(true);
        show(Map.of("MouseGUI", "0"));

        buttons().getFirst().doClick();

        HudToggleButton button = buttons().getFirst();
        assertFalse(button.isSelected());
        assertEquals(getText("bindings.toggles.off").toUpperCase(), button.getText());
        assertTrue(edits.isEmpty());
        assertTrue(written.isEmpty());
    }

    @Test
    void searchNarrowsTheRows() {
        show(Map.of("MouseGUI", "1", "EnableMenuGroups", "0"));

        panel.search("widget");

        assertEquals(1, buttons().size());
    }

    private void show(Map<String, String> held) {
        Map<String, BindsValues.Setting> settings = new LinkedHashMap<>();
        held.forEach((id, value) -> settings.put(id, new BindsValues.Setting(id, value, true)));
        panel.show("Custom.4.2.binds", new BindsValues(Map.of(), settings));
    }

    private List<HudToggleButton> buttons() {
        return all(panel, HudToggleButton.class);
    }

    private List<String> labelTexts() {
        return all(panel, JLabel.class).stream().map(JLabel::getText).toList();
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
