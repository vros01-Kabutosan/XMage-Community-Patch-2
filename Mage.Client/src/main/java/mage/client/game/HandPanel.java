/*
 * Decompiled with CFR.
 */
package mage.client.game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.UUID;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import mage.client.cards.BigCard;
import mage.client.cards.Cards;
import mage.client.util.GUISizeHelper;
import mage.constants.Zone;
import mage.view.CardsView;

public class HandPanel
extends JPanel {
    private static final double XCP_HAND_SCALE = 1.12;
    private JPanel jPanel;
    private JScrollPane jScrollPane1;
    private static final Border EMPTY_BORDER = new EmptyBorder(0, 0, 0, 0);
    private Cards hand;

    private Dimension getCommunityHandCardDimension() {
        Dimension base = GUISizeHelper.handCardDimension;
        return new Dimension(Math.max(base.width, (int)Math.round((double)base.width * 1.12)), Math.max(base.height, (int)Math.round((double)base.height * 1.12)));
    }

    public HandPanel() {
        this.initComponents();
        this.changeGUISize();
    }

    public void initComponents() {
        this.jPanel = new JPanel();
        this.jScrollPane1 = new JScrollPane(this.jPanel);
        this.jScrollPane1.getViewport().setBackground(new Color(0, 0, 0, 0));
        this.hand = new Cards(true, this.jScrollPane1);
        this.hand.setCardDimension(this.getCommunityHandCardDimension());
        this.jPanel.setLayout(new GridBagLayout());
        this.jPanel.setBackground(new Color(0, 0, 0, 0));
        this.jPanel.add(this.hand);
        this.setOpaque(false);
        this.jPanel.setOpaque(false);
        this.jScrollPane1.setOpaque(false);
        this.jPanel.setBorder(EMPTY_BORDER);
        this.jScrollPane1.setBorder(EMPTY_BORDER);
        this.jScrollPane1.setVerticalScrollBarPolicy(21);
        this.jScrollPane1.getHorizontalScrollBar().setUnitIncrement(8);
        this.jScrollPane1.setViewportBorder(EMPTY_BORDER);
        this.setLayout(new BorderLayout());
        this.add((Component)this.jScrollPane1, "Center");
        this.hand.setHScrollSpeed(8);
        this.hand.setBackgroundColor(new Color(0, 0, 0, 0));
        this.hand.setVisibleIfEmpty(false);
        this.hand.setBorder(EMPTY_BORDER);
        this.hand.setZone(Zone.HAND);
    }

    public void cleanUp() {
        this.hand.cleanUp();
    }

    public void changeGUISize() {
        this.setGUISize();
    }

    private void setGUISize() {
        this.jScrollPane1.getVerticalScrollBar().setPreferredSize(new Dimension(GUISizeHelper.scrollBarSize, 0));
        this.jScrollPane1.getHorizontalScrollBar().setPreferredSize(new Dimension(0, GUISizeHelper.scrollBarSize));
        this.jScrollPane1.getHorizontalScrollBar().setUnitIncrement(GUISizeHelper.getCardsScrollbarUnitInc(this.getCommunityHandCardDimension().width));
        this.hand.setCardDimension(this.getCommunityHandCardDimension());
        this.hand.changeGUISize();
    }

    public void loadCards(CardsView cards, BigCard bigCard, UUID gameId) {
        this.hand.loadCards(cards, bigCard, gameId, false);
    }
}
