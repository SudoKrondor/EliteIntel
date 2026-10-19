package elite.intel.ui.screen.settings;

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.ShipPersonality;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.dao.GameSessionDao;
import elite.intel.db.managers.ShipLoadoutManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.db.util.Database;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.SystemSession;
import elite.intel.ui.widget.HudSegmentedControl;
import elite.intel.ui.event.RestartMouthEvent;
import elite.intel.ui.event.ServicesStateEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import javax.swing.text.JTextComponent;

import static org.junit.jupiter.api.Assertions.*;

class AiServicesSettingsPanelVoiceTest {
    private static LookAndFeel previousLookAndFeel;
    private GameSessionDao.GameSession previousVoicePreferences;
    private static final TtsProvider[] LOCAL = {
            TtsProvider.KOKORO, TtsProvider.SUPERTONIC, TtsProvider.PIPER
    };

    @BeforeEach
    void snapshotVoicePreferences() {
        previousVoicePreferences = Database.withDao(GameSessionDao.class, GameSessionDao::get);
    }

    @AfterEach
    void restoreVoicePreferences() {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setKokoroVoice(previousVoicePreferences.getKokoroVoice());
            session.setSupertonicVoice(previousVoicePreferences.getSupertonicVoice());
            session.setPiperVoice(previousVoicePreferences.getPiperVoice());
            dao.save(session);
            return Void.TYPE;
        });
    }

    @BeforeAll
    static void installApplicationLookAndFeel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            previousLookAndFeel = UIManager.getLookAndFeel();
            assertTrue(FlatDarkLaf.setup());
        });
    }

    @AfterAll
    static void restoreLookAndFeel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(previousLookAndFeel);
            } catch (UnsupportedLookAndFeelException e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    void switchingProvidersRendersEveryVoiceAndKeepsEachUnsavedChoice() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SystemSession session = SystemSession.getInstance();
            TtsProvider previous = session.getTtsProvider();
            try {
                session.setTtsProvider(TtsProvider.PIPER);
                AiServicesSettingsPanel panel = new AiServicesSettingsPanel();
                panel.initData();
                HudSegmentedControl engine = field(panel, "ttsLocalEngineControl", HudSegmentedControl.class);
                JComboBox<?> voices = field(panel, "localVoiceCombo", JComboBox.class);
                Object oldCell = voices.getSelectedItem();
                Map<TtsProvider, Object> choices = new EnumMap<>(TtsProvider.class);
                for (int index = 0; index < LOCAL.length; index++) {
                    clickEngine(engine, index);
                    assertEquals(LOCAL[index].voiceRoster().count(), voices.getItemCount());
                    // FlatLaf may repaint an old cell while JComboBox is replacing its model. Its
                    // label must remain valid even though the selected engine has already changed.
                    render(voices, oldCell);
                    for (int voice = 0; voice < voices.getItemCount(); voice++) {
                        render(voices, voices.getItemAt(voice));
                    }
                    voices.setSelectedIndex(voices.getItemCount() - 1);
                    choices.put(LOCAL[index], voices.getSelectedItem());
                    oldCell = voices.getSelectedItem();
                    paint(panel);
                }
                for (int index = 0; index < LOCAL.length; index++) {
                    clickEngine(engine, index);
                    assertEquals(choices.get(LOCAL[index]), voices.getSelectedItem());
                }
            } finally {
                session.setTtsProvider(previous);
            }
        });
    }

    @Test
    void voiceSelectionSavesAndReloadsForAllThreeLocalProviders() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SystemSession session = SystemSession.getInstance();
            TtsProvider previousProvider = session.getTtsProvider();
            boolean previousCommandLocal = session.useLocalCommandLlm();
            boolean previousQueryLocal = session.useLocalQueryLlm();
            ShipLoadoutManager loadouts = ShipLoadoutManager.getInstance();
            ShipLoadOutDto previousLoadout = loadouts.get();
            try {
                session.setUseLocalCommandLlm(true);
                session.setUseLocalQueryLlm(true);
                ShipDao.Ship ship = new ShipDao.Ship();
                ship.setShipId(97531);
                ship.setShipName("Voice settings regression");
                ship.setShipIdentifier("cobramkiii");
                ship.setCommanderName("CMDR Test");
                ship.setCargoCapacity(0);
                ship.setPersonality(ShipPersonality.DEFAULT.name());
                ShipLoadOutDto loadout = new ShipLoadOutDto();
                loadout.setShipId(ship.getShipId());
                loadouts.clear();
                loadouts.save(loadout);
                for (TtsProvider provider : LOCAL) {
                    session.setTtsProvider(provider);
                    ship.setVoice(provider.defaultVoiceName());
                    ShipManager.getInstance().saveShip(ship);
                    AiServicesSettingsPanel panel = new AiServicesSettingsPanel();
                    panel.initData();
                    JComboBox<?> voices = field(panel, "localVoiceCombo", JComboBox.class);
                    voices.setSelectedIndex(voices.getItemCount() - 1);
                    Object selected = voices.getSelectedItem();
                    assertTrue(panel.isDirty());
                    assertTrue(panel.save());
                    String expected = provider.voiceRoster().reduce((left, right) -> right).orElseThrow();
                    assertEquals(expected, ShipManager.getInstance().getShip().getVoice());
                    panel.reload();
                    assertEquals(selected, voices.getSelectedItem());
                    assertFalse(panel.isDirty());
                }
            } finally {
                loadouts.clear();
                if (previousLoadout != null) loadouts.save(previousLoadout);
                session.setTtsProvider(previousProvider);
                session.setUseLocalCommandLlm(previousCommandLocal);
                session.setUseLocalQueryLlm(previousQueryLocal);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void render(JComboBox<?> combo, Object value) {
        Component rendered = ((ListCellRenderer) combo.getRenderer())
                .getListCellRendererComponent(new JList<>(), value, -1, false, false);
        assertFalse(((JLabel) rendered).getText().isBlank());
    }

    @Test
    void voiceSelectionPersistsWithoutAnActiveShipAndSurvivesPanelRecreation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SystemSession session = SystemSession.getInstance();
            TtsProvider previousProvider = session.getTtsProvider();
            boolean previousCommandLocal = session.useLocalCommandLlm();
            boolean previousQueryLocal = session.useLocalQueryLlm();
            ShipLoadoutManager loadouts = ShipLoadoutManager.getInstance();
            ShipLoadOutDto previousLoadout = loadouts.get();
            try {
                loadouts.clear();
                assertNull(ShipManager.getInstance().getShip());
                session.setUseLocalCommandLlm(true);
                session.setUseLocalQueryLlm(true);
                for (TtsProvider provider : LOCAL) {
                    session.setTtsProvider(provider);
                    List<String> roster = provider.voiceRoster().toList();
                    List<String> choices = provider == TtsProvider.PIPER
                            ? roster : List.of(roster.getLast());
                    for (String choice : choices) {
                        AiServicesSettingsPanel panel = new AiServicesSettingsPanel();
                        panel.initData();
                        field(panel, "localVoiceCombo", JComboBox.class).setSelectedIndex(roster.indexOf(choice));
                        assertTrue(panel.save());
                        assertEquals(choice, session.getVoiceName(provider));
                        assertEquals(provider.voiceGender(choice), session.getVoiceGender());
                        panel.reload();
                        assertEquals(choice, selectedVoice(panel));
                        AiServicesSettingsPanel reopened = new AiServicesSettingsPanel();
                        reopened.initData();
                        assertEquals(choice, selectedVoice(reopened), "a new panel must read the persisted preference");
                        assertFalse(reopened.isDirty());
                    }
                }
            } finally {
                loadouts.clear();
                if (previousLoadout != null) loadouts.save(previousLoadout);
                session.setTtsProvider(previousProvider);
                session.setUseLocalCommandLlm(previousCommandLocal);
                session.setUseLocalQueryLlm(previousQueryLocal);
            }
        });
    }

    @Test
    void switchingAwayAndBackRestoresTheSavedProviderPreference() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SystemSession session = SystemSession.getInstance();
            TtsProvider previousProvider = session.getTtsProvider();
            ShipLoadoutManager loadouts = ShipLoadoutManager.getInstance();
            ShipLoadOutDto previousLoadout = loadouts.get();
            try {
                loadouts.clear();
                Map<TtsProvider, String> choices = new EnumMap<>(TtsProvider.class);
                for (TtsProvider provider : LOCAL) {
                    String choice = provider.voiceRoster().toList().getLast();
                    session.setVoiceName(provider, choice);
                    choices.put(provider, choice);
                }
                session.setTtsProvider(TtsProvider.PIPER);
                AiServicesSettingsPanel panel = new AiServicesSettingsPanel();
                panel.initData();
                for (int index = 0; index < LOCAL.length; index++) {
                    clickEngine(field(panel, "ttsLocalEngineControl", HudSegmentedControl.class), index);
                    assertEquals(choices.get(LOCAL[index]), selectedVoice(panel));
                }
            } finally {
                loadouts.clear();
                if (previousLoadout != null) loadouts.save(previousLoadout);
                session.setTtsProvider(previousProvider);
            }
        });
    }

    private static String selectedVoice(AiServicesSettingsPanel panel) {
        try {
            var method = AiServicesSettingsPanel.class.getDeclaredMethod("selectedLocalVoiceName");
            method.setAccessible(true);
            return (String) method.invoke(panel);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void listeningBecomesAvailableAfterAutomaticEngineRestartWithoutReloadingThePanel() throws Exception {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        boolean previousCommandLocal = session.useLocalCommandLlm();
        boolean previousQueryLocal = session.useLocalQueryLlm();
        List<ShipDao.Ship> previousShips = ShipManager.getInstance().getAllShips();
        RestartRecorder recorder = new RestartRecorder();
        UiBus.register(recorder);
        AiServicesSettingsPanel[] panel = new AiServicesSettingsPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                session.setTtsProvider(TtsProvider.PIPER);
                session.setUseLocalCommandLlm(true);
                session.setUseLocalQueryLlm(true);
                panel[0] = new AiServicesSettingsPanel() {
                    @Override protected boolean confirmTtsSwitch() { return true; }
                };
                panel[0].initData();
                JButton listen = field(panel[0], "localVoiceTestButton", JButton.class);
                assertFalse(listen.isEnabled(), "stopped services must be explained instead of a silent audition");
                panel[0].setServicesState(ServicesStateEvent.State.RUNNING);
                assertTrue(listen.isEnabled());
                clickEngine(field(panel[0], "ttsLocalEngineControl", HudSegmentedControl.class), 0);
                assertFalse(listen.isEnabled(), "a different engine must first be saved");
                assertTrue(panel[0].save());
                assertNotNull(recorder.event);
                assertFalse(listen.isEnabled(), "must wait until the new engine is started");
                assertEquals("settings.ai.voice.status.loading", field(panel[0], "localVoiceStatusKey", String.class));
            });
            recorder.event.completion().complete(true);
            SwingUtilities.invokeAndWait(() -> {
                JButton listen = field(panel[0], "localVoiceTestButton", JButton.class);
                assertTrue(listen.isEnabled(), "the same panel must enable listening without reload or app restart");
                assertEquals("settings.ai.voice.status.ready", field(panel[0], "localVoiceStatusKey", String.class));
                clickEngine(field(panel[0], "ttsLocalEngineControl", HudSegmentedControl.class), 1);
                assertTrue(panel[0].save());
            });
            recorder.event.completion().completeExceptionally(new IllegalStateException("test startup failure"));
            SwingUtilities.invokeAndWait(() -> {
                assertFalse(field(panel[0], "localVoiceTestButton", JButton.class).isEnabled());
                assertEquals("settings.ai.voice.status.failed", field(panel[0], "localVoiceStatusKey", String.class));
                assertTrue(hasVisibleText(field(panel[0], "localVoiceStatus", JPanel.class)),
                        "failed startup must show an explanation");
                panel[0].setServicesState(ServicesStateEvent.State.STOPPED);
                panel[0].setServicesState(ServicesStateEvent.State.RUNNING);
                assertTrue(field(panel[0], "localVoiceTestButton", JButton.class).isEnabled(),
                        "starting services again must recover the preview without reopening settings");
            });
        } finally {
            UiBus.unregister(recorder);
            session.setTtsProvider(previousProvider);
            session.setUseLocalCommandLlm(previousCommandLocal);
            session.setUseLocalQueryLlm(previousQueryLocal);
            for (ShipDao.Ship ship : previousShips) ShipManager.getInstance().saveShip(ship);
        }
    }

    private static boolean hasVisibleText(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JTextComponent text && !text.getText().isBlank()) return true;
            if (child instanceof Container nested && hasVisibleText(nested)) return true;
        }
        return false;
    }

    private static class RestartRecorder {
        RestartMouthEvent event;
        @Subscribe public void onRestart(RestartMouthEvent event) { this.event = event; }
    }

    private static void clickEngine(HudSegmentedControl engine, int index) {
        engine.setSize(900, 50);
        engine.dispatchEvent(new MouseEvent(engine, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, index * 300 + 150, 25, 1, false, MouseEvent.BUTTON1));
    }

    private static void paint(JPanel panel) {
        panel.setSize(1400, 1100);
        layout(panel);
        BufferedImage image = new BufferedImage(1400, 1100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layout(nested);
        }
    }

    private static <T> T field(Object panel, String name, Class<T> type) {
        try {
            Field field = AiServicesSettingsPanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(panel));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
