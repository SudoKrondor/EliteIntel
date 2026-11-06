package elite.intel.ui.screen.services;

import com.formdev.flatlaf.FlatLightLaf;
import elite.intel.session.SystemSession;
import elite.intel.ui.widget.HudLockCheckBox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A slip on the AI Services page has to be undoable from the page itself: a cleared key can leave SAVE greyed out,
 * and the only other way off the edit is a prompt that appears on switching section tabs.
 * <p>
 * No test here stores a key, because {@code Cypher} would read or create the real {@code secret.key}. Cancel reloads
 * every field from the session the same way, so a typed value over an empty stored key exercises it fully.
 */
class AiServicesPanelCancelTest {

    private static LookAndFeel previousLookAndFeel;

    /**
     * The look and feel {@code App} starts with: the page's combo boxes measure themselves while it is built.
     */
    @BeforeAll
    static void useTheAppsLookAndFeel() {
        previousLookAndFeel = UIManager.getLookAndFeel();
        FlatLightLaf.setup();
    }

    @AfterAll
    static void restoreLookAndFeel() throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(previousLookAndFeel);
    }

    private static <T extends Component> List<T> find(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) found.add(type.cast(c));
            if (c instanceof Container child) found.addAll(find(child, type));
        }
        return found;
    }

    private static JButton button(Container root, String i18nKey) {
        String text = getText(i18nKey);
        return find(root, JButton.class).stream()
                .filter(b -> text.equalsIgnoreCase(b.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no button " + i18nKey));
    }

    /**
     * The speech section is built after the language model one, so its key field comes second.
     */
    private static JPasswordField ttsKeyField(AiServicesPanel panel) {
        return find(panel, JPasswordField.class).get(1);
    }

    @Test
    void cancelIsOnlyOfferedWhileThereIsAnEditToCancel() {
        AiServicesPanel panel = new AiServicesPanel();
        panel.initData();
        JButton cancel = button(panel, "button.cancel");
        assertFalse(cancel.isEnabled(), "a clean page has nothing to cancel");

        ttsKeyField(panel).setText("slip");
        assertTrue(cancel.isEnabled(), "an edit can be cancelled");

        ttsKeyField(panel).setText(new String(new char[0]));
        assertFalse(cancel.isEnabled(), "typing the saved value back leaves nothing to cancel");
    }

    @Test
    void cancelGetsOffAnEditWhileSaveIsGreyedOut() {
        SystemSession session = SystemSession.getInstance();
        boolean command = session.useLocalCommandLlm();
        boolean query = session.useLocalQueryLlm();
        try {
            session.setUseLocalCommandLlm(false); // a cloud setup with no key: the page cannot save
            session.setUseLocalQueryLlm(false);

            AiServicesPanel panel = new AiServicesPanel();
            panel.initData();
            JPasswordField ttsKey = ttsKeyField(panel);
            JButton cancel = button(panel, "button.cancel");

            ttsKey.setText("slip");
            assertFalse(button(panel, "button.save").isEnabled(), "this edit cannot be saved");
            assertTrue(cancel.isEnabled(), "and so it must be possible to cancel it");

            cancel.doClick();

            assertEquals(0, ttsKey.getPassword().length, "the saved value is back");
            assertFalse(panel.isDirty());
            assertFalse(cancel.isEnabled());
            assertEquals("", session.getTtsApiKey(), "cancel never writes to the session");
        } finally {
            session.setUseLocalCommandLlm(command);
            session.setUseLocalQueryLlm(query);
        }
    }

    @Test
    void cancelLocksTheKeyFieldsAgain() {
        AiServicesPanel panel = new AiServicesPanel();
        panel.initData();
        List<HudLockCheckBox> locks = find(panel, HudLockCheckBox.class);
        assertEquals(2, locks.size());
        locks.forEach(lock -> lock.setSelected(false));

        ttsKeyField(panel).setText("slip");
        button(panel, "button.cancel").doClick();

        locks.forEach(lock -> assertTrue(lock.isSelected(), "a restored key is locked again"));
    }
}
