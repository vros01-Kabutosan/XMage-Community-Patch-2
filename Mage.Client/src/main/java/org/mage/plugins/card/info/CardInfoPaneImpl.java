/*
 * Decompiled with CFR.
 */
package org.mage.plugins.card.info;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import mage.client.util.GUISizeHelper;
import mage.client.util.gui.GuiDisplayUtil;
import mage.components.CardInfoPane;
import mage.view.CardView;
import org.mage.card.arcane.UI;

public class CardInfoPaneImpl
extends JEditorPane
implements CardInfoPane {
    public static final int TOOLTIP_WIDTH_MIN = 260;
    public static final int TOOLTIP_HEIGHT_MIN = 118;
    public static final int TOOLTIP_HEIGHT_MAX = 480;
    public static final int TOOLTIP_BORDER_WIDTH = 28;
    private static final int CARD_CORNER_RADIUS = 12;
    private static final int CARD_HORIZONTAL_PADDING = 14;
    private static final int CARD_VERTICAL_PADDING = 10;
    private static final Color CARD_BACKGROUND = new Color(248, 250, 252);
    private static final Color CARD_BORDER = new Color(205, 212, 221);
    private int type;
    private int addWidth;
    private int addHeight;
    private boolean setSize = false;
    private CardView pendingCard;
    private Component pendingContainer;
    private boolean cardRenderQueued;

    public CardInfoPaneImpl() {
        UI.setHTMLEditorKit(this);
        this.setEditable(false);
        this.setOpaque(false);
        this.setBackground(CARD_BACKGROUND);
        this.setBorder(BorderFactory.createEmptyBorder(CARD_VERTICAL_PADDING, CARD_HORIZONTAL_PADDING, CARD_VERTICAL_PADDING, CARD_HORIZONTAL_PADDING));
        this.setGUISize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth() - 1;
            int height = getHeight() - 1;
            if (width > 0 && height > 0) {
                g.setColor(getBackground());
                g.fillRoundRect(0, 0, width, height, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS);
                g.setColor(CARD_BORDER);
                g.drawRoundRect(0, 0, width, height, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS);
            }
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }

    public void changeGUISize() {
        this.setGUISize();
        this.revalidate();
        this.repaint();
    }

    private void setGUISize() {
        this.addWidth = GUISizeHelper.cardTooltipLargeTextWidth;
        this.addHeight = GUISizeHelper.cardTooltipLargeTextHeight;
        this.setSize = true;
    }

    public void setCard(CardView card, Component container) {
        if (card == null) {
            return;
        }
        synchronized (this) {
            this.pendingCard = card;
            this.pendingContainer = container;
            if (this.cardRenderQueued) {
                return;
            }
            this.cardRenderQueued = true;
        }
        SwingUtilities.invokeLater(() -> {
            CardView cardToRender;
            Component containerToResize;
            synchronized (this) {
                cardToRender = this.pendingCard;
                containerToResize = this.pendingContainer;
                this.pendingCard = null;
                this.pendingContainer = null;
                this.cardRenderQueued = false;
            }
            if (cardToRender == null) {
                return;
            }
            GuiDisplayUtil.TextLines textLines = GuiDisplayUtil.getTextLinesfromCardView(cardToRender);
            StringBuilder buffer = GuiDisplayUtil.getRulesFromCardView(cardToRender, textLines);
            this.setText(buffer.toString());
            this.setCaretPosition(0);
            this.resizeTooltipIfNeeded(containerToResize, textLines.getBasicTextLength(), textLines.getLines().size());
        });
    }

    private void resizeTooltipIfNeeded(Component container, int ruleLength, int rules) {
        if (container == null) {
            return;
        }
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int contentWidth = 475;
        if (ruleLength > 180 || rules > 3) {
            contentWidth += 38;
        }
        if (ruleLength > 380 || rules > 6) {
            contentWidth += 48;
        }
        if (ruleLength > 650 || rules > 9) {
            contentWidth += 52;
        }
        int maxWidth = Math.max(475, Math.min(660, screen.width - 160));
        contentWidth = Math.max(475, Math.min(contentWidth, maxWidth));
        this.setPreferredSize(null);
        this.setMinimumSize(null);
        this.setSize(contentWidth, Math.max(2200, screen.height * 2));
        Dimension preferred = this.getPreferredSize();
        int naturalHeight = preferred == null ? 145 : preferred.height;
        int minHeight = 128;
        if (rules > 3 || ruleLength > 210) {
            minHeight = 148;
        }
        if (rules > 6 || ruleLength > 420) {
            minHeight = 178;
        }
        int contentHeight = Math.max(minHeight, naturalHeight + 18);
        int maxContentHeight = Math.max(260, Math.min(480, screen.height - 175 - 32));
        contentHeight = Math.max(minHeight, Math.min(contentHeight, maxContentHeight));
        this.setPreferredSize(new Dimension(contentWidth, contentHeight));
        this.setMinimumSize(new Dimension(contentWidth, Math.min(contentHeight, minHeight)));
        this.setSize(contentWidth, contentHeight);
        // Keep the outer frame symmetrical with the 16px content inset.
        int outerWidth = contentWidth + 32;
        int outerHeight = contentHeight + 32;
        container.setPreferredSize(new Dimension(outerWidth, outerHeight));
        container.setSize(outerWidth, outerHeight);
        GuiDisplayUtil.keepComponentInsideScreen(container.getX(), container.getY(), container);
        Container parent = container.getParent();
        if (parent != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
            int margin = 16;
            int x = container.getX();
            int y = container.getY();
            int safeRight = parent.getWidth() - margin;
            int safeBottom = parent.getHeight() - margin;
            if (x + container.getWidth() > safeRight) {
                x = Math.max(margin, safeRight - container.getWidth());
            }
            if (y + container.getHeight() > safeBottom) {
                y = Math.max(margin, safeBottom - container.getHeight());
            }
            if (x < margin) {
                x = margin;
            }
            if (y < margin) {
                y = margin;
            }
            container.setLocation(x, y);
        }
        this.type = ruleLength > 180 || rules > 3 ? 1 : 0;
        this.setSize = false;
        container.validate();
        container.repaint();
    }
}
