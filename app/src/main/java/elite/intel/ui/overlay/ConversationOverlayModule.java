package elite.intel.ui.overlay;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.ui.event.AiResponseLogEvent;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudLogArea;
import elite.intel.ui.widget.HudSection;

import javax.swing.*;
import java.awt.*;

/**
 * Displays exactly the current commander/Vega exchange. A new commander phrase clears the preceding pair, while
 * Vega's response keeps the established chat typewriter animation and right-hand visual lane.
 */
final class ConversationOverlayModule implements OverlayModule {

    private static final int TYPEWRITER_DELAY_MS = 25;

    private final HudLogArea conversation = HudLogArea.overlayChat(TYPEWRITER_DELAY_MS);
    private final HudSection section;
    private volatile boolean running;

    /** Creates the headerless conversation card and its fixed-height transparent viewport. */
    ConversationOverlayModule() {
        JScrollPane scrollPane = AppTheme.hudApplicationScrollPane(conversation);
        configureTransparentViewport(scrollPane);
        scrollPane.setPreferredSize(new Dimension(
                HudPalette.HUD_OVERLAY_DEFAULT_WIDTH,
                HudPalette.HUD_OVERLAY_CONVERSATION_HEIGHT));

        section = HudSection.compactCard(null, new BorderLayout());
        section.setHeaderVisible(false);
        section.setSurfaceBackground(HudPalette.HUD_COLOR_ROLE_OVERLAY_CARD_BACKGROUND);
        section.body().add(scrollPane, BorderLayout.CENTER);
    }

    /** Keeps the overlay card's alpha surface visible through its scroll viewport. */
    private static void configureTransparentViewport(JScrollPane scrollPane) {
        Color transparent = HudPalette.HUD_COLOR_ROLE_OVERLAY_TRANSPARENT_BACKGROUND;
        scrollPane.setOpaque(false);
        scrollPane.setBackground(transparent);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(transparent);
    }

    @Override
    public JComponent component() {
        return section;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        GameEventBus.register(this);
        UiBus.register(this);
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        GameEventBus.unregister(this);
        UiBus.unregister(this);
    }

    /** Replaces the displayed pair with the newly accepted commander phrase. */
    @Subscribe
    public void onCommanderPhrase(NormalizedUserInputEvent event) {
        if (event.getText() == null || event.getText().isBlank()) {
            return;
        }
        updateOnEdt(() -> {
            conversation.clear();
            conversation.addMessage(event.getText(), HudLogArea.Style.USER_INPUT, HudLogArea.Align.LEFT);
        });
    }

    /** Adds Vega's completed response to the current commander phrase. */
    @Subscribe
    public void onVegaResponse(AiResponseLogEvent event) {
        if (event.getData() == null || event.getData().isBlank()) {
            return;
        }
        updateOnEdt(() -> conversation.addMessage(
                event.getData(), HudLogArea.Style.AI_RESPONSE, HudLogArea.Align.RIGHT));
    }

    private void updateOnEdt(Runnable update) {
        SwingUtilities.invokeLater(() -> {
            if (running) {
                update.run();
            }
        });
    }
}
