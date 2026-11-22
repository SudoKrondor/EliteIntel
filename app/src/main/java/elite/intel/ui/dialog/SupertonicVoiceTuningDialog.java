package elite.intel.ui.dialog;

import elite.intel.ai.mouth.VoiceEqualizer;
import elite.intel.ai.mouth.VoiceTuning;
import elite.intel.ai.mouth.subscribers.events.LocalVoiceAuditionEvent;
import elite.intel.ai.mouth.supertonic.SupertonicVoices;
import elite.intel.db.managers.SupertonicVoiceTuningManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudSlider;
import elite.intel.ui.widget.StatusBadge;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Tunes each Supertonic voice on its own: speed, a volume trim and a six-band EQ, because the voices differ a
 * little in loudness and pace. The voices are listed on the left, grouped by gender, and the selected one's
 * settings on the right.
 * <p>
 * There is no Save: a slider is written when the commander lets go of it, so Test always plays what is set.
 * Opened from Settings / Speech whichever engine speaks for VEGA, because Supertonic voices the radio either way.
 */
public final class SupertonicVoiceTuningDialog extends JDialog {

    private static final int LIST_WIDTH = 220;
    private static final int LABEL_WIDTH = 170;

    private final SupertonicVoiceTuningManager tunings = SupertonicVoiceTuningManager.getInstance();

    private final DefaultListModel<Object> listModel = new DefaultListModel<>();
    private final JList<Object> voiceList = new JList<>(listModel);
    private final JLabel voiceTitle = AppTheme.hudSectionLabel("");
    private HudSlider speedSlider;
    private HudSlider volumeSlider;
    private final HudSlider[] eqSliders = new HudSlider[VoiceEqualizer.BAND_COUNT];

    /**
     * Suppresses writes while the sliders are being filled in for a newly selected voice.
     */
    private boolean loading;

    private SupertonicVoiceTuningDialog(Component parent) {
        super(SwingUtilities.getWindowAncestor(parent), getText("voiceTuning.title"), ModalityType.APPLICATION_MODAL);
        buildUi();
    }

    public static void open(Component parent) {
        SupertonicVoiceTuningDialog dialog = new SupertonicVoiceTuningDialog(parent);
        AppTheme.runWithModalScrim(SwingUtilities.getWindowAncestor(parent), () -> dialog.setVisible(true));
    }

    private void buildUi() {
        setUndecorated(true);

        JPanel body = AppTheme.transparentPanel(new BorderLayout(HudPalette.HUD_GAP * 2, 0));
        body.add(buildVoiceList(), BorderLayout.WEST);
        body.add(buildSettings(), BorderLayout.CENTER);

        JButton close = AppTheme.makeButton(getText("button.close"));
        close.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("voiceTuning.title"))
                .onClose(this::dispose)
                .body(body)
                .primary(close)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(close);

        voiceList.setSelectedValue(SupertonicVoices.DEFAULT_VOICE, true);
        pack();
        setMinimumSize(new Dimension(820, getPreferredSize().height));
        setLocationRelativeTo(getOwner());
    }

    private JComponent buildVoiceList() {
        addGroup(getText("voiceTuning.female"), false);
        addGroup(getText("voiceTuning.male"), true);

        voiceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        voiceList.setSelectionModel(new VoicesOnlySelection());
        voiceList.setCellRenderer(new VoiceRenderer());
        voiceList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedVoice();
        });

        JScrollPane scroll = AppTheme.hudScrollPane(voiceList);
        scroll.setPreferredSize(new Dimension(LIST_WIDTH, 0));
        return scroll;
    }

    private void addGroup(String header, boolean male) {
        listModel.addElement(header);
        Arrays.stream(SupertonicVoices.values())
                .filter(voice -> voice.isMale() == male)
                .sorted(Comparator.comparing(SupertonicVoices::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .forEach(listModel::addElement);
    }

    private JComponent buildSettings() {
        JPanel panel = AppTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, HudPalette.HUD_GAP, 0);
        panel.add(voiceTitle, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        speedSlider = slider(VoiceTuning.MIN_SPEED, VoiceTuning.MAX_SPEED, String::valueOf,
                () -> current().withSpeed(speedSlider.getValue()));
        addSliderRow(panel, gbc, getText("voiceTuning.speed"), speedSlider);

        volumeSlider = slider(VoiceTuning.MIN_VOLUME, VoiceTuning.MAX_VOLUME, value -> value + "%",
                () -> current().withVolume(volumeSlider.getValue()));
        addSliderRow(panel, gbc, getText("voiceTuning.volume"), volumeSlider);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(HudPalette.HUD_GAP * 2, 0, HudPalette.HUD_GAP, 0);
        panel.add(AppTheme.hudGroupLabel(getText("voiceTuning.eq")), gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        for (int band = 0; band < VoiceEqualizer.BAND_COUNT; band++) {
            int thisBand = band;
            eqSliders[band] = slider(VoiceTuning.MIN_EQ_DB, VoiceTuning.MAX_EQ_DB,
                    SupertonicVoiceTuningDialog::decibels,
                    () -> current().withEqBand(thisBand, eqSliders[thisBand].getValue()));
            addSliderRow(panel, gbc, bandLabel(band), eqSliders[band]);
        }

        JButton test = AppTheme.makeButton(getText("voiceTuning.test"));
        test.addActionListener(e -> selectedVoice().ifPresent(voice -> GameEventBus.publish(
                new LocalVoiceAuditionEvent(getText("voiceTuning.test.sentence"), voice.name()))));
        JButton reset = AppTheme.makeButtonSubtle(getText("voiceTuning.reset"));
        reset.addActionListener(e -> selectedVoice().ifPresent(voice -> {
            tunings.reset(voice);
            showSelectedVoice();
        }));
        JPanel actions = AppTheme.transparentPanel(new FlowLayout(FlowLayout.LEFT, HudPalette.HUD_GAP, 0));
        actions.add(test);
        actions.add(reset);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(HudPalette.HUD_GAP * 2, 0, 0, 0);
        panel.add(HudBanner.multiline(getText("voiceTuning.hint"), StatusBadge.State.INFO), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(HudPalette.HUD_GAP, -HudPalette.HUD_GAP, 0, 0);
        panel.add(actions, gbc);
        gbc.gridy++;

        // Takes up the slack, so the rows stay at the top when the voice list is the taller of the two.
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);
        return panel;
    }

    /**
     * A slider that writes the voice's tuning when the commander lets go of it, not on every step of a drag.
     */
    private HudSlider slider(int min, int max, IntFunction<String> format, Supplier<VoiceTuning> edited) {
        HudSlider slider = new HudSlider(min, max, 1, min);
        slider.setValueFormatter(format);
        slider.addChangeListener(e -> {
            if (loading || slider.isAdjusting()) return;
            selectedVoice().ifPresent(voice -> {
                VoiceTuning next = edited.get();
                if (!next.equals(tunings.tuningOf(voice))) tunings.save(voice, next);
            });
        });
        return slider;
    }

    private static void addSliderRow(JPanel panel, GridBagConstraints gbc, String text, HudSlider slider) {
        // The top inset drops the label from the row top to the track, the slider's own layout being value above,
        // track below.
        JLabel label = AppTheme.hudReadoutLabel(text);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, HudPalette.HUD_SLIDER_HEIGHT));
        label.setVerticalAlignment(SwingConstants.TOP);
        label.setBorder(BorderFactory.createEmptyBorder(HudPalette.HUD_SLIDER_VALUE_AREA, 0, 0, 0));
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 2, HudPalette.HUD_GAP);
        panel.add(label, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(slider, gbc);
        gbc.gridy++;
    }

    private void showSelectedVoice() {
        selectedVoice().ifPresent(voice -> {
            VoiceTuning tuning = tunings.tuningOf(voice);
            loading = true;
            try {
                voiceTitle.setText(voice.getDisplayName());
                speedSlider.setValue(tuning.speed());
                volumeSlider.setValue(tuning.volume());
                int[] eq = tuning.eq();
                for (int band = 0; band < eq.length; band++) {
                    eqSliders[band].setValue(eq[band]);
                }
            } finally {
                loading = false;
            }
        });
    }

    /**
     * What the sliders say now, for the one being moved to edit.
     */
    private VoiceTuning current() {
        int[] eq = new int[VoiceEqualizer.BAND_COUNT];
        for (int band = 0; band < eq.length; band++) {
            eq[band] = eqSliders[band].getValue();
        }
        return new VoiceTuning(speedSlider.getValue(), volumeSlider.getValue(), eq);
    }

    private Optional<SupertonicVoices> selectedVoice() {
        return voiceList.getSelectedValue() instanceof SupertonicVoices voice
                ? Optional.of(voice)
                : Optional.empty();
    }

    private static String bandLabel(int band) {
        int hz = VoiceEqualizer.BAND_HZ[band];
        String frequency = hz < 1000 ? hz + " Hz"
                : (hz % 1000 == 0 ? String.valueOf(hz / 1000) : String.valueOf(hz / 1000.0)) + " kHz";
        if (band == 0) return frequency + " · " + getText("voiceTuning.eq.lowShelf");
        if (band == VoiceEqualizer.BAND_COUNT - 1) return frequency + " · " + getText("voiceTuning.eq.highShelf");
        return frequency;
    }

    private static String decibels(int value) {
        return (value > 0 ? "+" : "") + value + " dB";
    }

    /**
     * Group headings sit in the list but cannot be selected: a selection that lands on one moves past it in the
     * direction it was heading, so the arrow keys walk from the last female voice straight to the first male one.
     */
    private final class VoicesOnlySelection extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int anchor, int lead) {
            int target = lead;
            if (!isVoice(target)) {
                target += lead >= getLeadSelectionIndex() ? 1 : -1;
                if (!isVoice(target)) return;
            }
            super.setSelectionInterval(target, target);
        }

        private boolean isVoice(int index) {
            return index >= 0 && index < listModel.size() && listModel.get(index) instanceof SupertonicVoices;
        }
    }

    private static final class VoiceRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            if (value instanceof SupertonicVoices voice) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, voice.getDisplayName(), index, selected, focused);
                label.setBorder(new EmptyBorder(3, 16, 3, 6));
                // The field-value size the HUD combo popups use, not the larger UI default.
                label.setFont(label.getFont().deriveFont(HudPalette.HUD_FONT_FIELD_VALUE));
                return label;
            }
            JLabel header = AppTheme.hudGroupLabel(String.valueOf(value));
            header.setBorder(new EmptyBorder(index == 0 ? 2 : 10, 6, 4, 6));
            return header;
        }
    }
}
