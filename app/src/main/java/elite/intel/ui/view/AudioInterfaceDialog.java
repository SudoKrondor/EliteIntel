package elite.intel.ui.view;

import elite.intel.session.SystemSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

public class AudioInterfaceDialog extends JDialog {

    public AudioInterfaceDialog(Component parent) {
        super(SwingUtilities.getWindowAncestor(parent), getText("audio.devices.title"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        SystemSession session = SystemSession.getInstance();

        String savedInput = session.getAudioInputDevice();
        String savedOutput = session.getAudioOutputDevice();

        HudComboBox<String> inputCombo = AudioDeviceCombo.input(savedInput);
        HudComboBox<String> outputCombo = AudioDeviceCombo.output(savedOutput);
        // Persist on change — no Save button (listeners added after the initial selection is set).
        inputCombo.addActionListener(e ->
                session.setAudioInputDevice(AudioDeviceCombo.normalize((String) inputCombo.getSelectedItem())));
        outputCombo.addActionListener(e ->
                session.setAudioOutputDevice(AudioDeviceCombo.normalize((String) outputCombo.getSelectedItem())));

        JPanel form = transparentPanel(new GridBagLayout());

        GridBagConstraints gbc = baseGbc();
        gbc.insets = new Insets(6, 4, 6, 4);

        // Input row
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel inLabel = hudReadoutLabel(getText("audio.devices.input"));
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
        JLabel outLabel = hudReadoutLabel(getText("audio.devices.output"));
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

        JButton back = makeButtonSubtle(getText("button.back"));      // dismiss = subtle
        back.addActionListener(e -> dispose());

        HudSection section = HudSection.flat(getText("audio.devices.section.devices"), new BorderLayout());
        section.body().add(form, BorderLayout.CENTER);

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("audio.devices.title"))
                .onClose(this::dispose)
                .body(section)
                .scrollBody(false)
                .dismiss(back)                // left side
                .build();

        setContentPane(hudModalScaffold(spec));

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        getRootPane().setDefaultButton(back);
        pack();
        setMinimumSize(new Dimension(500, getHeight()));
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

}
