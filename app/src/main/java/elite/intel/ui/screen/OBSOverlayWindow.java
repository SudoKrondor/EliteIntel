package elite.intel.ui.screen;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.AiResponseLogEvent;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class OBSOverlayWindow extends JFrame {

    private static final Color OVERLAY_BACKGROUND = HudPalette.HUD_COLOR_ROLE_STREAM_OVERLAY_BACKGROUND;
    // Match the conversation view (AI tab) chat colors: commander in green, AI reply in blue.
    private static final Color COMMANDER_TEXT = HudPalette.HUD_COLOR_ROLE_USER_INPUT_LOG_TEXT;
    private static final Color AI_TEXT = HudPalette.HUD_COLOR_ROLE_ASSISTANT_RESPONSE_LOG_TEXT;
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final ShipManager shipManager = ShipManager.getInstance();
    // Match the conversation view's typewriter cadence (AiTabPanel uses HudLogArea.chat(25)).
    private static final int TYPEWRITER_DELAY_MS = 25;
    private static final int MAX_MESSAGES = 7;

    /// cached buffered image
    private BufferedImage frame;

    // -- Inner model -----------------------------------------------------------

    private static final class OverlayMessage {
        final String prefix;   // "User: " or "AI: "
        final String fullText;
        final Color color;     // commander green / AI blue, matching the conversation view
        final boolean ai;      // AI replies get the sliding highlight panel while typing
        String visibleText = "";
        boolean complete = false;


        OverlayMessage(String prefix, String fullText, Color color, boolean ai) {
            this.prefix = prefix;
            this.fullText = fullText;
            this.color = color;
            this.ai = ai;
        }
    }

    // -- State -----------------------------------------------------------------

    private final List<OverlayMessage> messages = new ArrayList<>();
    private final OverlayPanel overlayPanel;

    // -- Construction ----------------------------------------------------------

    public OBSOverlayWindow() {
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
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            messages.clear();
            GameEventBus.register(this);
            UiBus.register(this);
        } else {
            GameEventBus.unregister(this);
            UiBus.unregister(this);
        }
        super.setVisible(visible);
    }

    // -- Event bus -------------------------------------------------------------

    @Subscribe
    public void onUserInputEvent(NormalizedUserInputEvent event) {
        if (event.getText() == null || event.getText().isBlank()) return;
        SwingUtilities.invokeLater(() -> addMessage(playerSession.getPlayerName() + ": ", event.getText(), COMMANDER_TEXT, false));
    }

    @Subscribe
    public void onAiResponseLogEvent(AiResponseLogEvent event) {
        if (event.getData() == null || event.getData().isBlank()) return;
        String shipName = shipManager.getShip() == null ? "AI" : shipManager.getShip().getShipName();
        SwingUtilities.invokeLater(() -> addMessage(shipName + ": ", event.getData(), AI_TEXT, true));
    }

    // -- Message lifecycle -----------------------------------------------------

    private void addMessage(String prefix, String text, Color color, boolean ai) {
        // Fast-forward any still-typing message
        for (OverlayMessage m : messages) {
            m.complete = true;
            m.visibleText = m.fullText;
        }

        OverlayMessage msg = new OverlayMessage(prefix, text, color, ai);
        messages.add(msg);

        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }

        overlayPanel.repaint();
        startTypewriter(msg);
    }

    private void startTypewriter(OverlayMessage target) {
        Timer[] holder = new Timer[1];
        holder[0] = new Timer(TYPEWRITER_DELAY_MS, null);
        holder[0].addActionListener(e -> {
            if (target.complete) {
                target.visibleText = target.fullText;
                holder[0].stop();
                overlayPanel.paintImmediately(0, 0, overlayPanel.getWidth(), overlayPanel.getHeight());
                return;
            }
            int len = target.visibleText.length();
            if (len >= target.fullText.length()) {
                target.complete = true;
                target.visibleText = target.fullText;
                holder[0].stop();
            } else {
                target.visibleText = target.fullText.substring(0, len + 1);
            }
            overlayPanel.paintImmediately(0, 0, overlayPanel.getWidth(), overlayPanel.getHeight());
        });
        holder[0].start();
    }

    // -- Custom panel ----------------------------------------------------------

    private final class OverlayPanel extends JPanel {

        private static final int PAD_X = 12;
        private static final int PAD_Y = 8;
        private static final int LINE_GAP = 6;
        /**
         * Alpha of the AI reply's sliding highlight wash at its anchored (left) edge.
         */
        private static final int AI_PANEL_ALPHA = 40;
        /**
         * Arm length of the L-brackets drawn at the AI panel's advancing (right) edge.
         */
        private static final int AI_BRACKET_ARM = 8;

        private final Font font;

        OverlayPanel() {
            setOpaque(true);
            setBackground(OVERLAY_BACKGROUND);
            font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) HudPalette.HUD_FONT_UI_DEFAULT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (messages.isEmpty()) return;
            int w = getWidth();
            int h = getHeight();

            //BufferedImage frame = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

            // reallocate only if size changed
            if (frame == null || frame.getWidth() != w || frame.getHeight() != h) {
                frame = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            }

            Graphics2D g2 = frame.createGraphics();
            g2.setColor(OVERLAY_BACKGROUND);
            g2.fillRect(0, 0, w, h);


            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int maxW = getWidth() - PAD_X * 2;

            // Measure all message blocks bottom-up
            List<List<String>> wrappedAll = new ArrayList<>();
            for (OverlayMessage m : messages) {
                String display = m.prefix + (m.complete ? m.fullText : m.visibleText);
                wrappedAll.add(wrapText(display, fm, maxW));
            }

            // Draw from the bottom up, newest last (bottom of panel)
            int y = getHeight() - PAD_Y;
            for (int i = messages.size() - 1; i >= 0; i--) {
                OverlayMessage msg = messages.get(i);
                List<String> lines = wrappedAll.get(i);
                int blockH = lines.size() * fm.getHeight();
                y -= blockH;
                if (y + blockH < 0) break; // scrolled off top

                boolean active = !msg.complete && i == messages.size() - 1;

                // AI reply: a cyan highlight panel whose advancing (right) edge tracks the typed text, so the
                // panel slides left-to-right in sync with the typewriter - matching the conversation view's
                // active AI card. Anchored (strongest) at the left; L-brackets mark the typing frontier.
                if (msg.ai && active) {
                    int widest = 0;
                    for (String ln : lines) widest = Math.max(widest, fm.stringWidth(ln));
                    drawAiPanel(g2, PAD_X - 6, y - 2, PAD_X + widest + 8, blockH + 4);
                }

                g2.setColor(msg.color);
                for (int li = 0; li < lines.size(); li++) {
                    g2.drawString(lines.get(li), PAD_X, y + li * fm.getHeight() + fm.getAscent());
                }

                // Thin blinking typewriter caret on the active (newest, incomplete) message, matching the
                // conversation view's caret (a 2px vertical bar in the message's own colour).
                if (active) {
                    String lastLine = lines.get(lines.size() - 1);
                    int cx = PAD_X + fm.stringWidth(lastLine);
                    int cy = y + (lines.size() - 1) * fm.getHeight();
                    if ((System.currentTimeMillis() / 500) % 2 == 0) {
                        HudGlyphs.paintHudTextCaret(g2, cx + 1, cy + fm.getAscent(), fm, msg.color);
                    }
                }

                y -= LINE_GAP;
            }

            g2.dispose();
            g.drawImage(frame, 0, 0, null); // single blit to screen
        }

        /**
         * Draws the AI reply's sliding highlight panel: a cyan wash strongest at the left (anchor) fading to
         * transparent at the advancing right edge, plus L-brackets on that right edge marking the typing
         * frontier. Called with a right edge that tracks the typed-text width, so it slides left-to-right as
         * the typewriter runs.
         */
        private void drawAiPanel(Graphics2D g2, int left, int top, int right, int height) {
            Color c = AI_TEXT;
            Paint old = g2.getPaint();
            g2.setPaint(new GradientPaint(left, top, withAlpha(c, AI_PANEL_ALPHA), right, top, withAlpha(c, 0)));
            g2.fillRect(left, top, right - left, height);
            g2.setPaint(old);
            g2.setColor(c);
            int arm = AI_BRACKET_ARM, t = 2, bottom = top + height;
            g2.fillRect(right - arm, top, arm, t);          // top-right horizontal
            g2.fillRect(right - t, top, t, arm);            // top-right vertical
            g2.fillRect(right - arm, bottom - t, arm, t);   // bottom-right horizontal
            g2.fillRect(right - t, bottom - arm, t, arm);   // bottom-right vertical
        }

        private static Color withAlpha(Color base, int alpha) {
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
        }

        private List<String> wrapText(String text, FontMetrics fm, int maxW) {
            List<String> result = new ArrayList<>();
            if (text == null || text.isEmpty()) {
                result.add("");
                return result;
            }
            String[] words = text.split(" ", -1);
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (fm.stringWidth(candidate) <= maxW) {
                    line = new StringBuilder(candidate);
                } else {
                    if (!line.isEmpty()) result.add(line.toString());
                    if (fm.stringWidth(word) > maxW) {
                        StringBuilder part = new StringBuilder();
                        for (char c : word.toCharArray()) {
                            if (fm.stringWidth(part + String.valueOf(c)) > maxW) {
                                result.add(part.toString());
                                part = new StringBuilder();
                            }
                            part.append(c);
                        }
                        line = part;
                    } else {
                        line = new StringBuilder(word);
                    }
                }
            }
            if (!line.isEmpty()) result.add(line.toString());
            return result;
        }
    }
}
