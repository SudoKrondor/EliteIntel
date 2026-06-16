package elite.intel.ui.screen;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.AiResponseLogEvent;
import elite.intel.ui.event.NormalizedUserInputEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.*;

public class OBSOverlayWindow extends JFrame {

    private static final Color BG = HudPalette.HUD_OVERLAY_BG;
    private static final Color FG = HudPalette.ACCENT;
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final ShipManager shipManager = ShipManager.getInstance();
    private static final int TYPEWRITER_DELAY_MS = 50;
    private static final int MAX_MESSAGES = 7;

    /// cached buffered image
    private BufferedImage frame;

    // -- Inner model -----------------------------------------------------------

    private static final class OverlayMessage {
        final String prefix;   // "User: " or "AI: "
        final String fullText;
        String visibleText = "";
        boolean complete = false;


        OverlayMessage(String prefix, String fullText) {
            this.prefix = prefix;
            this.fullText = fullText;
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
        getContentPane().setBackground(BG);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            messages.clear();
            EventBusManager.register(this);
        } else {
            EventBusManager.unregister(this);
        }
        super.setVisible(visible);
    }

    // -- Event bus -------------------------------------------------------------

    @Subscribe
    public void onUserInputEvent(NormalizedUserInputEvent event) {
        if (event.getText() == null || event.getText().isBlank()) return;
        SwingUtilities.invokeLater(() -> addMessage(playerSession.getPlayerName() + ": ", event.getText()));
    }

    @Subscribe
    public void onAiResponseLogEvent(AiResponseLogEvent event) {
        if (event.getData() == null || event.getData().isBlank()) return;
        String shipName = shipManager.getShip() == null ? "AI" : shipManager.getShip().getShipName();
        SwingUtilities.invokeLater(() -> addMessage(shipName + ": ", event.getData()));
    }

    // -- Message lifecycle -----------------------------------------------------

    private void addMessage(String prefix, String text) {
        // Fast-forward any still-typing message
        for (OverlayMessage m : messages) {
            m.complete = true;
            m.visibleText = m.fullText;
        }

        OverlayMessage msg = new OverlayMessage(prefix, text);
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

        private final Font font;

        OverlayPanel() {
            setOpaque(true);
            setBackground(BG);

            Font loaded;
            try {
                loaded = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(
                        getClass().getResourceAsStream("/fonts/Electrolize-Regular.ttf"))
                ).deriveFont(HudPalette.HUD_FONT_OVERLAY);
            } catch (FontFormatException | IOException e) {
                loaded = new Font(Font.MONOSPACED, Font.PLAIN, (int) HudPalette.HUD_FONT_OVERLAY);
            }
            font = loaded;
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
            g2.setColor(BG);
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
                List<String> lines = wrappedAll.get(i);
                int blockH = lines.size() * fm.getHeight();
                y -= blockH;
                if (y + blockH < 0) break; // scrolled off top

                g2.setColor(FG);
                for (int li = 0; li < lines.size(); li++) {
                    g2.drawString(lines.get(li), PAD_X, y + li * fm.getHeight() + fm.getAscent());
                }

                // Blinking cursor on the active (newest, incomplete) message
                OverlayMessage msg = messages.get(i);
                if (!msg.complete && i == messages.size() - 1) {
                    String lastLine = lines.get(lines.size() - 1);
                    int cx = PAD_X + fm.stringWidth(lastLine);
                    int cy = y + (lines.size() - 1) * fm.getHeight();
                    boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
                    if (blink) {
                        g2.setColor(FG);
                        g2.fillRect(cx + 2, cy + 2, fm.charWidth('M') / 2, fm.getAscent() - 2);
                    }
                }

                y -= LINE_GAP;
            }

            g2.dispose();
            g.drawImage(frame, 0, 0, null); // single blit to screen
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
