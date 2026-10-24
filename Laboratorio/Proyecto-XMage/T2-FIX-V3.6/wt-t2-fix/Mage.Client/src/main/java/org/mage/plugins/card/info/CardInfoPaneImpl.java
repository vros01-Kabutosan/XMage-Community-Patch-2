/*
 * Decompiled with CFR.
 */
package org.mage.plugins.card.info;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
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
    private int type;
    private int addWidth;
    private int addHeight;
    private boolean setSize = false;

    public CardInfoPaneImpl() {
        UI.setHTMLEditorKit(this);
        this.setEditable(false);
        this.setGUISize();
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
        try {
            SwingUtilities.invokeLater(() -> {
                GuiDisplayUtil.TextLines textLines = GuiDisplayUtil.getTextLinesfromCardView(card);
                StringBuilder buffer = GuiDisplayUtil.getRulesFromCardView(card, textLines);
                this.setText(buffer.toString());
                this.setCaretPosition(0);
                this.resizeTooltipIfNeeded(container, textLines.getBasicTextLength(), textLines.getLines().size());
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
        int maxContentHeight = Math.max(260, Math.min(480, screen.height - 175 - 28));
        contentHeight = Math.max(minHeight, Math.min(contentHeight, maxContentHeight));
        this.setPreferredSize(new Dimension(contentWidth, contentHeight));
        this.setMinimumSize(new Dimension(contentWidth, Math.min(contentHeight, minHeight)));
        this.setSize(contentWidth, contentHeight);
        int outerWidth = contentWidth + 28;
        int outerHeight = contentHeight + 28;
        container.setPreferredSize(new Dimension(outerWidth, outerHeight));
        container.setSize(outerWidth, outerHeight);
        GuiDisplayUtil.keepComponentInsideScreen(container.getX(), container.getY(), container);
        Container parent = container.getParent();
        if (parent != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
            int margin = 14;
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
