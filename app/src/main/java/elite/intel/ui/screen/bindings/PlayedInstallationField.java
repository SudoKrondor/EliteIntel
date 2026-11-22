package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.install.PlayedInstallation;
import elite.intel.bindforge.install.PlayedInstallation.Choice;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.GameFoldersChangedEvent;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.theme.HudPalette;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.hudReadoutLabel;

/**
 * <strong>Playing:</strong> the installation being played, left of the User Configuration Folder on the APPLY bar
 * (linux-several-installations.md, rule 4). It shows what the follower chose and changes when it switches; picking
 * another installation is the user's choice of it. Hidden with fewer than two installations to play - always on
 * Windows, and on a Linux machine with one.
 */
public final class PlayedInstallationField extends JPanel {

    private static final Logger log = LogManager.getLogger(PlayedInstallationField.class);

    /** How long the selection must rest before it is taken as the user's pick. */
    private static final int SETTLE_MILLIS = 600;

    /** How each choice is named, worked out with the list. On the EDT. */
    private Map<Choice, String> labels = Map.of();
    private final JComboBox<Choice> combo = new HudComboBox<>(new Choice[0],
            choice -> choice == null ? " " : labels.getOrDefault(choice, choice.storefront()));
    private final RefreshCoalescer refreshes = new RefreshCoalescer();
    /** Set while the list is filled from the disk, so filling it is not taken for the user's pick. On the EDT. */
    private boolean filling;
    /** The installation shown as played, or last picked - picking it again changes nothing. On the EDT. */
    private Choice played;
    private final Timer pick = new Timer(SETTLE_MILLIS, e -> picked());

    public PlayedInstallationField() {
        super(new FlowLayout(FlowLayout.LEFT, HudPalette.HUD_GAP, 0));
        setOpaque(false);
        setVisible(false);
        add(hudReadoutLabel(getText("bindings.played.label")));
        add(combo);
        combo.setToolTipText(getText("bindings.played.tooltip"));
        // WHY: settled first - each arrow key on the closed list is a selection of its own, and each pick restarts the
        // journal parser, so stepping past an installation must not choose it.
        pick.setRepeats(false);
        combo.addActionListener(e -> {
            if (!filling) pick.restart();
        });
        UiBus.register(this);
    }

    /**
     * Works out the installations to play and the one played, off the EDT - it runs detection. Also when the list of
     * installations changed: one added with its Wine prefix is offered, one removed is not.
     */
    public void refresh() {
        if (!refreshes.tryStart()) return;
        Thread.ofPlatform().name("BindForge-PlayedInstallation").daemon().start(() -> {
            try {
                List<Choice> choices = PlayedInstallation.choices();
                Optional<Choice> current = PlayedInstallation.current(choices);
                SwingUtilities.invokeLater(() -> show(choices, current));
            } catch (RuntimeException e) {
                // WHY: broad on purpose - a thread boundary. The field stays as it was, and the log says why.
                log.error("Could not work out the installations to play", e);
            }
            if (refreshes.finish()) refresh();
        });
    }

    @Subscribe
    public void onGameFoldersChanged(GameFoldersChangedEvent event) {
        refresh();
    }

    public void dispose() {
        pick.stop();
        UiBus.unregister(this);
    }

    private void show(List<Choice> choices, Optional<Choice> current) {
        filling = true;
        // WHY: a pick still settling is overtaken by what the disk says now.
        pick.stop();
        try {
            labels = labels(choices);
            combo.removeAllItems();
            choices.forEach(combo::addItem);
            played = current.orElse(null);
            combo.setSelectedItem(played);
            combo.setToolTipText(current
                    .map(choice -> getText("bindings.played.tooltip.folders", choice.bindingsFolder().toString(),
                            choice.journalFolder().toString()))
                    .orElse(getText("bindings.played.tooltip")));
        } finally {
            filling = false;
        }
        setVisible(choices.size() >= 2);
        // WHY: the bar is a FlowLayout - showing the field changes what fits beside it.
        revalidate();
    }

    /** The selection, once it rested: the user's pick, unless it is the installation already played. On the EDT. */
    private void picked() {
        if (!(combo.getSelectedItem() instanceof Choice choice) || choice.equals(played)) return;
        played = choice;
        choose(choice);
    }

    /** The user's pick, stored off the EDT; the event it publishes refreshes this field and the folder beside it. */
    private void choose(Choice choice) {
        Thread.ofPlatform().name("BindForge-ChoosePlayed").daemon().start(() -> {
            if (PlayedInstallation.choose(choice)) return;
            SwingUtilities.invokeLater(() -> {
                HudConfirmDialog.info(this, getText("bindings.played.label"),
                        getText("bindings.played.failed", choice.storefront(), choice.root().toString()),
                        getText("button.ok"));
                refresh();
            });
        });
    }

    /**
     * The storefront alone where it names one installation, with its folder where two share it - as the reconcile names
     * them ({@link FirstTimeStartupFlow#labelsFor}).
     */
    private static Map<Choice, String> labels(List<Choice> choices) {
        Map<String, Long> countByStorefront = choices.stream()
                .collect(Collectors.groupingBy(Choice::storefront, Collectors.counting()));
        return choices.stream().collect(Collectors.toMap(Function.identity(),
                choice -> countByStorefront.get(choice.storefront()) == 1
                        ? choice.storefront()
                        : choice.storefront() + " (" + choice.root() + ")"));
    }
}
