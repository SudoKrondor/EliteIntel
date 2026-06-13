package elite.intel.ui.view;

import elite.intel.ai.ears.AudioDeviceEnumerator;
import elite.intel.session.SystemSession;

import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

public class AudioInterfaceDialog extends JDialog {

    private static final String SYSTEM_DEFAULT_LABEL = getText("audio.devices.systemDefault");

    public AudioInterfaceDialog(Component parent) {
        super(SwingUtilities.getWindowAncestor(parent), getText("audio.devices.title"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        SystemSession session = SystemSession.getInstance();

        String savedInput = session.getAudioInputDevice();
        String savedOutput = session.getAudioOutputDevice();

        HudComboBox<String> inputCombo = buildCombo(AudioDeviceEnumerator.getInputDevices(), savedInput);
        HudComboBox<String> outputCombo = buildCombo(AudioDeviceEnumerator.getOutputDevices(), savedOutput);

        JPanel form = transparentPanel(new GridBagLayout());

        GridBagConstraints gbc = baseGbc();
        gbc.insets = new Insets(6, 4, 6, 4);

        // Input row
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel inLabel = new JLabel(getText("audio.devices.input"));
        inLabel.setForeground(FG);
        inLabel.setPreferredSize(new Dimension(170, 28));
        form.add(inLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(inputCombo, gbc);

        // Output row
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel outLabel = new JLabel(getText("audio.devices.output"));
        outLabel.setForeground(FG);
        outLabel.setPreferredSize(new Dimension(170, 28));
        form.add(outLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(outputCombo, gbc);

        // Note
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel note = new JLabel(getText("audio.devices.note"));
        note.setForeground(FG_MUTED);
        note.setFont(note.getFont().deriveFont(note.getFont().getSize() * 0.9f));
        form.add(note, gbc);

        JButton saveBtn = makeButton(getText("button.save"));         // primary
        JButton back = makeButtonSubtle(getText("button.back"));      // dismiss = subtle
        saveBtn.addActionListener(e -> {
            String inSel = (String) inputCombo.getSelectedItem();
            String outSel = (String) outputCombo.getSelectedItem();
            session.setAudioInputDevice(SYSTEM_DEFAULT_LABEL.equals(inSel) ? null : inSel);
            session.setAudioOutputDevice(SYSTEM_DEFAULT_LABEL.equals(outSel) ? null : outSel);
            dispose();
        });
        back.addActionListener(e -> dispose());

        HudSection section = HudSection.flat(getText("audio.devices.section.devices"), new BorderLayout());
        section.body().add(form, BorderLayout.CENTER);

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("audio.devices.title"))
                .onClose(this::dispose)
                .body(section)
                .scrollBody(false)
                .primary(saveBtn)             // right side
                .dismiss(back)                // left side
                .build();

        setContentPane(hudModalScaffold(spec));

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        getRootPane().setDefaultButton(saveBtn);
        pack();
        setMinimumSize(new Dimension(500, getHeight()));
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private static HudComboBox<String> buildCombo(List<Mixer.Info> devices, String savedName) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement(SYSTEM_DEFAULT_LABEL);
        for (Mixer.Info info : devices) {
            model.addElement(info.getName());
        }
        HudComboBox<String> combo = new HudComboBox<>(model);
        if (savedName != null && !savedName.isBlank()) {
            combo.setSelectedItem(savedName);
        }
        return combo;
    }
}
