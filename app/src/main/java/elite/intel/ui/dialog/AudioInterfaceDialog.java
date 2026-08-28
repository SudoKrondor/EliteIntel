package elite.intel.ui.dialog;

import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.RestartEarsEvent;
import elite.intel.ui.event.RestartMouthEvent;
import elite.intel.ui.support.AudioDeviceCombo;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudSection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.baseGbc;
import static elite.intel.ui.theme.HudForms.sizeFieldLabel;

public class AudioInterfaceDialog extends JDialog {

    public AudioInterfaceDialog(Component parent) {
        super(SwingUtilities.getWindowAncestor(parent), getText("audio.devices.title"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        SystemSession session = SystemSession.getInstance();

        String savedInput = session.getAudioInputDevice();
        String savedOutput = session.getAudioOutputDevice();

        HudComboBox<String> inputCombo = AudioDeviceCombo.input(savedInput);
        HudComboBox<String> outputCombo = AudioDeviceCombo.output(savedOutput);
        // Persist on change - no Save button (listeners added after the initial selection is set) - and
        // restart the one service that reads the device, exactly as the AUDIO settings panel does: this is
        // the second door to the same two settings, so it must not leave the commander waiting for a
        // restart the other door no longer needs. The stored-value comparison is what makes re-picking the
        // device already in use a no-op, since a JComboBox fires on every pick.
        inputCombo.addActionListener(e -> {
            String selected = AudioDeviceCombo.normalize((String) inputCombo.getSelectedItem());
            if (Objects.equals(selected, session.getAudioInputDevice())) return;
            session.setAudioInputDevice(selected);
            UiBus.publish(new RestartEarsEvent());
        });
        outputCombo.addActionListener(e -> {
            String selected = AudioDeviceCombo.normalize((String) outputCombo.getSelectedItem());
            if (Objects.equals(selected, session.getAudioOutputDevice())) return;
            session.setAudioOutputDevice(selected);
            UiBus.publish(new RestartMouthEvent());
        });

        JPanel form = transparentPanel(new GridBagLayout());

        GridBagConstraints gbc = baseGbc();
        gbc.insets = new Insets(6, 4, 6, 4);

        // Input row
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel inLabel = hudReadoutLabel(getText("audio.devices.input"));
        sizeFieldLabel(inLabel, 170);
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
        sizeFieldLabel(outLabel, 170);
        form.add(outLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(outputCombo, gbc);

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
