package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.AiResponseLogEvent;
import elite.intel.ui.overlay.OverlayWindow;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Original single-window conversation overlay retained for Linux desktops. It deliberately preserves the previous
 * fixed-size, opaque presentation instead of introducing the Windows companion cards or route module.
 */
public final class OBSOverlayWindow extends JFrame implements OverlayWindow {

    private static final Color OVERLAY_BACKGROUND = HudPalette.HUD_COLOR_ROLE_LEGACY_OVERLAY_BACKGROUND;
    private static final Color COMMANDER_TEXT = HudPalette.HUD_COLOR_ROLE_USER_INPUT_LOG_TEXT;
    private static final Color AI_TEXT = HudPalette.HUD_COLOR_ROLE_ASSISTANT_RESPONSE_LOG_TEXT;
    private static final int TYPEWRITER_DELAY_MS = 25;
    private static final int MAX_MESSAGES = 7;

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final ShipManager shipManager = ShipManager.getInstance();
    private final Runnable onHidden;
    private final List<OverlayMessage> messages = new ArrayList<>();
    private final OverlayPanel overlayPanel;
    private BufferedImage frame;
    private volatile boolean subscribed;

    /** Creates the legacy Linux overlay and notifies the controlling shortcut when it is hidden. */
    OBSOverlayWindow(Runnable onHidden) {
        this.onHidden = Objects.requireNonNull(onHidden, "onHidden");
        setTitle(getText("obs.title"));
        setUndecorated(true);
        setAlwaysOnTop(false);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(900, 124);
        setLocationRelativeTo(null);
        setLocation(0, 0);
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/ai.png")));

        overlayPanel = new OverlayPanel();
        setContentPane(overlayPanel);
        getContentPane().setBackground(OVERLAY_BACKGROUND);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                hideOverlay();
            }
        });
    }

    @Override
    public void showOverlay() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::showOverlay);
            return;
        }
        if (isVisible()) {
            return;
        }
        messages.clear();
        subscribe();
        setVisible(true);
        overlayPanel.repaint();
    }

    @Override
    public void hideOverlay() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::hideOverlay);
            return;
        }
        if (!isVisible()) {
            return;
        }
        unsubscribe();
        setVisible(false);
        onHidden.run();
    }

    @Override
    public void dispose() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::dispose);
            return;
        }
        unsubscribe();
        super.dispose();
    }

    @Subscribe
    public void onUserInputEvent(NormalizedUserInputEvent event) {
        if (event.getText() == null || event.getText().isBlank()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (subscribed) {
                addMessage(playerSession.getPlayerName() + ": ", event.getText(), COMMANDER_TEXT, false);
            }
        });
    }

    @Subscribe
    public void onAiResponseLogEvent(AiResponseLogEvent event) {
        if (event.getData() == null || event.getData().isBlank()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (subscribed) {
                var ship = shipManager.getShip();
                String shipName = ship == null || ship.getShipName() == null || ship.getShipName().isBlank()
                        ? getText("tab.ai") : ship.getShipName();
                addMessage(shipName + ": ", event.getData(), AI_TEXT, true);
            }
        });
    }

    private void subscribe() {
        if (subscribed) {
            return;
        }
        subscribed = true;
        GameEventBus.register(this);
        UiBus.register(this);
    }

    private void unsubscribe() {
        if (!subscribed) {
            return;
        }
        subscribed = false;
        GameEventBus.unregister(this);
        UiBus.unregister(this);
    }

    private void addMessage(String prefix, String text, Color color, boolean ai) {
        for (OverlayMessage message : messages) {
            message.complete = true;
            message.visibleText = message.fullText;
        }

        OverlayMessage message = new OverlayMessage(prefix, text, color, ai);
        messages.add(message);
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }

        overlayPanel.repaint();
        startTypewriter(message);
    }

    private void startTypewriter(OverlayMessage target) {
        Timer[] holder = new Timer[1];
        holder[0] = new Timer(TYPEWRITER_DELAY_MS, null);
        holder[0].addActionListener(event -> {
            if (target.complete) {
                target.visibleText = target.fullText;
                holder[0].stop();
                overlayPanel.paintImmediately(0, 0, overlayPanel.getWidth(), overlayPanel.getHeight());
                return;
            }
            int length = target.visibleText.length();
            if (length >= target.fullText.length()) {
                target.complete = true;
                target.visibleText = target.fullText;
                holder[0].stop();
            } else {
                target.visibleText = target.fullText.substring(0, length + 1);
            }
            overlayPanel.paintImmediately(0, 0, overlayPanel.getWidth(), overlayPanel.getHeight());
        });
        holder[0].start();
    }

    private static final class OverlayMessage {
        private final String prefix;
        private final String fullText;
        private final Color color;
        private final boolean ai;
        private String visibleText = "";
        private boolean complete;

        private OverlayMessage(String prefix, String fullText, Color color, boolean ai) {
            this.prefix = prefix;
            this.fullText = fullText;
            this.color = color;
            this.ai = ai;
        }
    }

    /** Draws the original message stack directly into the fixed-size legacy overlay surface. */
    private final class OverlayPanel extends JPanel {

        private static final int PAD_X = 12;
        private static final int PAD_Y = 8;
        private static final int LINE_GAP = 6;
        private static final int AI_PANEL_ALPHA = 40;
        private static final int AI_BRACKET_ARM = 8;

        private final Font font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) HudPalette.HUD_FONT_UI_DEFAULT);

        private OverlayPanel() {
            setOpaque(true);
            setBackground(OVERLAY_BACKGROUND);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (messages.isEmpty()) {
                return;
            }
            int width = getWidth();
            int height = getHeight();
            if (frame == null || frame.getWidth() != width || frame.getHeight() != height) {
                frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }

            Graphics2D bufferGraphics = frame.createGraphics();
            try {
                bufferGraphics.setColor(OVERLAY_BACKGROUND);
                bufferGraphics.fillRect(0, 0, width, height);
                bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                bufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                bufferGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                bufferGraphics.setFont(font);
                FontMetrics metrics = bufferGraphics.getFontMetrics();
                int maxWidth = width - PAD_X * 2;

                List<List<String>> wrappedMessages = new ArrayList<>();
                for (OverlayMessage message : messages) {
                    String display = message.prefix + (message.complete ? message.fullText : message.visibleText);
                    wrappedMessages.add(wrapText(display, metrics, maxWidth));
                }

                int y = height - PAD_Y;
                for (int index = messages.size() - 1; index >= 0; index--) {
                    OverlayMessage message = messages.get(index);
                    List<String> lines = wrappedMessages.get(index);
                    int blockHeight = lines.size() * metrics.getHeight();
                    y -= blockHeight;
                    if (y + blockHeight < 0) {
                        break;
                    }

                    boolean active = !message.complete && index == messages.size() - 1;
                    if (message.ai && active) {
                        int widestLine = 0;
                        for (String line : lines) {
                            widestLine = Math.max(widestLine, metrics.stringWidth(line));
                        }
                        drawAiPanel(bufferGraphics, PAD_X - 6, y - 2, PAD_X + widestLine + 8, blockHeight + 4);
                    }

                    bufferGraphics.setColor(message.color);
                    for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                        bufferGraphics.drawString(lines.get(lineIndex), PAD_X,
                                y + lineIndex * metrics.getHeight() + metrics.getAscent());
                    }

                    if (active) {
                        String lastLine = lines.get(lines.size() - 1);
                        int caretX = PAD_X + metrics.stringWidth(lastLine);
                        int caretY = y + (lines.size() - 1) * metrics.getHeight();
                        if ((System.currentTimeMillis() / 500) % 2 == 0) {
                            HudGlyphs.paintHudTextCaret(bufferGraphics, caretX + 1,
                                    caretY + metrics.getAscent(), metrics, message.color);
                        }
                    }
                    y -= LINE_GAP;
                }
            } finally {
                bufferGraphics.dispose();
            }
            graphics.drawImage(frame, 0, 0, null);
        }

        private void drawAiPanel(Graphics2D graphics, int left, int top, int right, int height) {
            Paint oldPaint = graphics.getPaint();
            graphics.setPaint(new GradientPaint(left, top, withAlpha(AI_TEXT, AI_PANEL_ALPHA), right, top,
                    withAlpha(AI_TEXT, 0)));
            graphics.fillRect(left, top, right - left, height);
            graphics.setPaint(oldPaint);
            graphics.setColor(AI_TEXT);
            int arm = AI_BRACKET_ARM;
            int thickness = 2;
            int bottom = top + height;
            graphics.fillRect(right - arm, top, arm, thickness);
            graphics.fillRect(right - thickness, top, thickness, arm);
            graphics.fillRect(right - arm, bottom - thickness, arm, thickness);
            graphics.fillRect(right - thickness, bottom - arm, thickness, arm);
        }

        private static Color withAlpha(Color base, int alpha) {
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
        }

        private static List<String> wrapText(String text, FontMetrics metrics, int maxWidth) {
            List<String> result = new ArrayList<>();
            if (text == null || text.isEmpty()) {
                result.add("");
                return result;
            }
            String[] words = text.split(" ", -1);
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (metrics.stringWidth(candidate) <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    if (!line.isEmpty()) {
                        result.add(line.toString());
                    }
                    if (metrics.stringWidth(word) > maxWidth) {
                        StringBuilder part = new StringBuilder();
                        for (char character : word.toCharArray()) {
                            if (metrics.stringWidth(part + String.valueOf(character)) > maxWidth) {
                                result.add(part.toString());
                                part = new StringBuilder();
                            }
                            part.append(character);
                        }
                        line = part;
                    } else {
                        line = new StringBuilder(word);
                    }
                }
            }
            if (!line.isEmpty()) {
                result.add(line.toString());
            }
            return result;
        }
    }
}
