/*
 * Decompiled with CFR.
 */
package mage.client.cards;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import mage.abilities.icon.CardIconRenderSettings;
import mage.abilities.icon.CardIconType;
import mage.cards.MageCard;
import mage.cards.MageCardSpace;
import mage.client.cards.BigCard;
import mage.client.cards.CardEventProducer;
import mage.client.cards.CardEventSource;
import mage.client.dialog.PreferencesDialog;
import mage.client.plugins.adapters.MageActionCallback;
import mage.client.plugins.impl.Plugins;
import mage.client.util.ClientDefaultSettings;
import mage.client.util.Event;
import mage.client.util.GUISizeHelper;
import mage.client.util.Listener;
import mage.constants.Zone;
import mage.util.DebugUtil;
import mage.view.CardView;
import mage.view.CardsView;
import mage.view.PermanentView;
import mage.view.StackAbilityView;
import org.apache.log4j.Logger;

public class Cards
extends JPanel
implements CardEventProducer {
    private static final Logger logger = Logger.getLogger(Cards.class);
    private static final Border EMPTY_BORDER = new EmptyBorder(0, 0, 0, 0);
    private final Map<UUID, MageCard> cards = new LinkedHashMap<UUID, MageCard>();
    private final CardEventSource cardEventSource = new CardEventSource();
    private boolean dontDisplayTapped = false;
    private Zone zone;
    private int lastLoadedCardsCount = 0;
    private final JScrollPane parentScrollPane;
    private boolean isVisibleIfEmpty = true;
    private Dimension cardDimension;
    private boolean verticalStackLayout = false;
    private JPanel cardArea;
    private JScrollPane jScrollPane1;

    public void setVerticalStackLayout(boolean enabled) {
        this.verticalStackLayout = enabled;
        for (MageCard mageCard : this.cards.values()) {
            mageCard.setCardContainerRef((Container)(enabled ? this : this.cardArea));
        }
        for (MageCard mageCard : this.cards.values()) {
            mageCard.setCardContainerRef((Container)(enabled ? this : this.cardArea));
        }
        for (MageCard mageCard : this.cards.values()) {
            mageCard.setCardContainerRef((Container)(enabled ? this : this.cardArea));
        }
        if (enabled) {
            this.cardArea.setLayout(null);
        }
        if (this.jScrollPane1 != null) {
            this.jScrollPane1.setVerticalScrollBarPolicy(enabled ? 20 : 21);
            this.jScrollPane1.setHorizontalScrollBarPolicy(enabled ? 31 : 30);
        }
        this.layoutCards();
        this.sizeCards(this.getCardDimension());
        this.revalidate();
        this.repaint();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (this.verticalStackLayout) {
            this.layoutCards();
        }
    }

    public boolean isVerticalStackLayout() {
        return this.verticalStackLayout;
    }

    private int getVerticalStackStep() {
        return Math.max(34, Math.min(44, this.getCardDimension().height / 8));
    }

    public Cards() {
        this(false, null);
    }

    public Cards(boolean skipAddingScrollPane, JScrollPane parentScrollPane) {
        this.initComponents(skipAddingScrollPane);
        this.setOpaque(false);
        this.setBackgroundColor(new Color(0, 0, 0, 100));
        this.parentScrollPane = parentScrollPane;
        if (!skipAddingScrollPane) {
            this.jScrollPane1.setOpaque(false);
            this.jScrollPane1.getViewport().setOpaque(false);
            this.jScrollPane1.setBorder(EMPTY_BORDER);
        }
        if (Plugins.instance.isCardPluginLoaded()) {
            this.cardArea.setLayout(null);
        }
        this.cardArea.setBorder(EMPTY_BORDER);
        if (DebugUtil.GUI_GAME_DRAW_HAND_AND_STACK_BORDER) {
            this.setBorder(BorderFactory.createLineBorder(Color.green));
            this.cardArea.setBorder(BorderFactory.createLineBorder(Color.yellow));
        }
        this.setGUISize();
    }

    public void cleanUp() {
    }

    public void changeGUISize() {
        this.setGUISize();
        for (MageCard mageCard : this.cards.values()) {
            mageCard.setCardBounds(0, 0, this.getCardDimension().width, this.getCardDimension().height);
            mageCard.updateArtImage();
            mageCard.doLayout();
        }
        this.layoutCards();
        this.sizeCards(this.getCardDimension());
    }

    private void setGUISize() {
        if (this.jScrollPane1 != null) {
            this.jScrollPane1.getVerticalScrollBar().setPreferredSize(new Dimension(GUISizeHelper.scrollBarSize, 0));
            this.jScrollPane1.getHorizontalScrollBar().setPreferredSize(new Dimension(0, GUISizeHelper.scrollBarSize));
            this.jScrollPane1.getHorizontalScrollBar().setUnitIncrement(GUISizeHelper.getCardsScrollbarUnitInc(this.getCardDimension().width));
        }
    }

    public void setBackgroundColor(Color color) {
        this.setBackground(color);
        this.cardArea.setOpaque(true);
        this.cardArea.setBackground(color);
    }

    public void setVisibleIfEmpty(boolean isVisibleIfEmpty) {
        this.isVisibleIfEmpty = isVisibleIfEmpty;
    }

    @Override
    public void setBorder(Border border) {
        super.setBorder(border);
        if (this.jScrollPane1 != null) {
            this.jScrollPane1.setViewportBorder(border);
            this.jScrollPane1.setBorder(border);
        }
    }

    public boolean loadCards(CardsView cardsView, BigCard bigCard, UUID gameId, boolean revertOrder) {
        ArrayList<UUID> verticalStackOrder;
        boolean changed = false;
        boolean moveScrollbar = this.zone == Zone.HAND ? this.lastLoadedCardsCount != 0 && cardsView.size() > this.lastLoadedCardsCount : cardsView.size() != this.lastLoadedCardsCount;
        this.lastLoadedCardsCount = cardsView.size();
        changed = this.removeOutdatedCards(cardsView);
        if (cardsView.isEmpty() && this.countCards() > 0) {
            logger.fatal((Object)"Card object on the cards panel was not removed");
            for (Component comp : this.cardArea.getComponents()) {
                if (comp instanceof MageCard) {
                    MageCard mageCard = (MageCard)comp;
                    logger.fatal((Object)("MageCard name:" + mageCard.getName() + " toolTiptext:" + mageCard.getToolTipText()));
                } else {
                    logger.fatal((Object)("Unknown object:" + comp.getName() + " className:" + comp.getClass().getName()));
                }
                this.cardArea.remove(comp);
            }
        }
        ArrayList<CardView> orderedList = new ArrayList<CardView>();
        ArrayList<UUID> arrayList = verticalStackOrder = this.verticalStackLayout ? new ArrayList<UUID>() : null;
        if (revertOrder && !this.verticalStackLayout) {
            for (CardView card : cardsView.values()) {
                orderedList.add(0, card);
            }
        } else {
            orderedList.addAll(cardsView.values());
        }
        for (CardView card : orderedList) {
            if (this.verticalStackLayout && card.getCardIcons() != null) {
                card.getCardIcons().removeIf(icon -> icon.getIconType() == CardIconType.OTHER_HAS_TARGETS);
            }
            if (this.dontDisplayTapped && card instanceof PermanentView) {
                ((PermanentView)card).overrideTapped(false);
            }
            if (card instanceof StackAbilityView) {
                CardView tmp = ((StackAbilityView)card).getSourceCard();
                tmp.overrideRules(card.getRules());
                tmp.setChoosable(card.isChoosable());
                tmp.setPlayableStats(card.getPlayableStats().copy());
                tmp.setSelected(card.isSelected());
                tmp.setIsAbility(true);
                tmp.overrideTargets(card.getTargets());
                tmp.overrideId(card.getId());
                tmp.setAbilityType(card.getAbilityType());
                tmp.getCardIcons().clear();
                tmp.getCardIcons().addAll(card.getCardIcons());
                card = tmp;
            } else {
                card.setAbilityType(null);
            }
            if (verticalStackOrder != null) {
                verticalStackOrder.add(card.getId());
            }
            if (!this.cards.containsKey(card.getId())) {
                this.addCard(card, bigCard, gameId);
                changed = true;
            }
            this.cards.get(card.getId()).update(card);
        }
        if (verticalStackOrder != null) {
            LinkedHashMap<UUID, MageCard> reorderedCards = new LinkedHashMap<UUID, MageCard>();
            for (UUID id : verticalStackOrder) {
                MageCard mageCard = this.cards.get(id);
                if (mageCard == null) continue;
                reorderedCards.put(id, mageCard);
            }
            if (!new ArrayList<UUID>(this.cards.keySet()).equals(new ArrayList(reorderedCards.keySet()))) {
                changed = true;
            }
            this.cards.clear();
            this.cards.putAll(reorderedCards);
        }
        if (changed) {
            this.layoutCards();
        }
        if (!this.isVisibleIfEmpty) {
            this.cardArea.setVisible(!this.cards.isEmpty());
        }
        this.sizeCards(this.getCardDimension());
        this.revalidate();
        this.repaint();
        if (changed && moveScrollbar) {
            SwingUtilities.invokeLater(() -> {
                if (this.jScrollPane1 != null) {
                    if (this.verticalStackLayout) {
                        this.jScrollPane1.getVerticalScrollBar().setValue(this.jScrollPane1.getVerticalScrollBar().getMinimum());
                    } else {
                        this.jScrollPane1.getHorizontalScrollBar().setValue(this.jScrollPane1.getHorizontalScrollBar().getMaximum());
                    }
                }
                if (this.parentScrollPane != null) {
                    this.parentScrollPane.getHorizontalScrollBar().setValue(this.parentScrollPane.getHorizontalScrollBar().getMaximum());
                }
            });
        }
        return changed;
    }

    public void sizeCards(Dimension cardDimension) {
        if (this.verticalStackLayout) {
            int count = this.cards.size();
            int height = MageActionCallback.getHandOrStackMargins(this.zone).getHeight();
            int width = (int)cardDimension.getWidth() + MageActionCallback.getHandOrStackMargins(this.zone).getWidth();
            if (this.jScrollPane1 != null && this.jScrollPane1.getViewport() != null) {
                width = Math.max(width, this.jScrollPane1.getViewport().getWidth());
            }
            if (count > 0) {
                height += cardDimension.height;
                height += (count - 1) * this.getVerticalStackStep();
            }
            this.cardArea.setPreferredSize(new Dimension(width, Math.max(height, 1)));
            this.layoutCards();
        } else {
            this.cardArea.setPreferredSize(new Dimension((int)((double)this.cards.size() * (cardDimension.getWidth() + (double)MageActionCallback.getHandOrStackBetweenGapX(this.zone))) + MageActionCallback.getHandOrStackMargins(this.zone).getWidth(), (int)cardDimension.getHeight() + MageActionCallback.getHandOrStackMargins(this.zone).getHeight()));
        }
        this.cardArea.revalidate();
        this.cardArea.repaint();
    }

    public int getNumberOfCards() {
        return this.cards.size();
    }

    private Dimension getCardDimension() {
        if (this.cardDimension == null) {
            this.cardDimension = new Dimension(ClientDefaultSettings.dimensions.getFrameWidth(), ClientDefaultSettings.dimensions.getFrameHeight());
        }
        return this.cardDimension;
    }

    public void setCardDimension(Dimension dimension) {
        this.cardDimension = dimension;
        for (Component component : this.cardArea.getComponents()) {
            if (!(component instanceof MageCard)) continue;
            component.setBounds(0, 0, dimension.width, dimension.height);
        }
        this.layoutCards();
    }

    private void addCard(CardView card, BigCard bigCard, UUID gameId) {
        MageCard mageCard = Plugins.instance.getMageCard(card, bigCard, new CardIconRenderSettings(), this.getCardDimension(), gameId, true, true, PreferencesDialog.getRenderMode(), true);
        mageCard.setCardContainerRef((Container)(this.verticalStackLayout ? this : this.cardArea));
        if (this.zone != null) {
            mageCard.setZone(this.zone);
        }
        mageCard.update(card);
        this.cards.put(card.getId(), mageCard);
        this.cardArea.add((Component)mageCard);
        this.definePosition(mageCard);
    }

    private void definePosition(MageCard newCard) {
        int dx = MageActionCallback.getHandOrStackMargins(this.zone).getLeft();
        for (Component currentComp : this.cardArea.getComponents()) {
            if (currentComp.equals(newCard) || !(currentComp instanceof MageCard)) continue;
            MageCard currentCard = (MageCard)currentComp;
            dx = Math.max(dx, currentCard.getCardLocation().getCardX());
        }
        newCard.setCardLocation(dx += newCard.getCardLocation().getCardWidth() + MageActionCallback.getHandOrStackBetweenGapX(newCard.getZone()), MageActionCallback.getHandOrStackMargins(newCard.getZone()).getTop());
    }

    private boolean removeOutdatedCards(CardsView cardsView) {
        boolean changed = false;
        this.cards.keySet().removeIf(id -> !cardsView.containsKey(id));
        for (Component comp : this.cardArea.getComponents()) {
            if (comp instanceof MageCard) {
                if (this.cards.containsValue(comp)) continue;
                this.cardArea.remove(comp);
                changed = true;
                continue;
            }
            logger.error((Object)("Unknown card conponent in cards panel to remove: " + comp));
        }
        return changed;
    }

    private int countCards() {
        return this.cardArea.getComponentCount();
    }

    private void initComponents(boolean skipAddingScrollPane) {
        this.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 0)));
        this.setLayout(new BorderLayout());
        this.cardArea = new JPanel();
        this.cardArea.setLayout(new FlowLayout(0, 0, 0));
        if (skipAddingScrollPane) {
            this.add((Component)this.cardArea, "Center");
        } else {
            this.jScrollPane1 = new JScrollPane();
            this.jScrollPane1.setVerticalScrollBarPolicy(21);
            this.jScrollPane1.setViewportView(this.cardArea);
            this.add((Component)this.jScrollPane1, "Center");
        }
    }

    public void setHScrollSpeed(int unitIncrement) {
        if (this.jScrollPane1 != null) {
            this.jScrollPane1.getHorizontalScrollBar().setUnitIncrement(unitIncrement);
        }
    }

    public void setVScrollSpeed(int unitIncrement) {
        if (this.jScrollPane1 != null) {
            this.jScrollPane1.getVerticalScrollBar().setUnitIncrement(unitIncrement);
        }
    }

    private void layoutCards() {
        if (this.verticalStackLayout) {
            ArrayList<MageCard> cardsToLayout = new ArrayList<MageCard>(this.cards.values());
            MageCardSpace margins = MageActionCallback.getHandOrStackMargins(this.zone);
            int dx = margins.getLeft();
            int dy = margins.getTop();
            int step = this.getVerticalStackStep();
            int areaWidth = Math.max(this.cardArea.getWidth(), this.cardArea.getPreferredSize().width);
            if (this.jScrollPane1 != null && this.jScrollPane1.getViewport() != null) {
                areaWidth = Math.max(areaWidth, this.jScrollPane1.getViewport().getWidth());
            }
            int usableWidth = Math.max(this.getCardDimension().width, areaWidth - margins.getLeft() - margins.getRight());
            dx += Math.max(0, (usableWidth - this.getCardDimension().width) / 2);
            int baseY = dy + Math.max(0, cardsToLayout.size() - 1) * step;
            for (int i = 0; i < cardsToLayout.size(); ++i) {
                MageCard component = (MageCard)cardsToLayout.get(i);
                component.setCardLocation(dx, baseY - i * step);
                this.cardArea.setComponentZOrder((Component)component, i);
            }
            return;
        }
        ArrayList<MageCard> cardsToLayout = new ArrayList<MageCard>();
        for (Component component : this.cardArea.getComponents()) {
            if (!(component instanceof MageCard)) continue;
            cardsToLayout.add((MageCard)component);
        }
        cardsToLayout.sort(Comparator.comparingInt(cp -> cp.getCardLocation().getCardX()));
        int dx = MageActionCallback.getHandOrStackBetweenGapX(this.zone);
        for (MageCard component : cardsToLayout) {
            component.setCardLocation(dx, component.getCardLocation().getCardY());
            dx += component.getCardLocation().getCardWidth() + MageActionCallback.getHandOrStackBetweenGapX(this.zone);
        }
    }

    public void setZone(Zone zone) {
        this.zone = zone;
        for (MageCard mageCard : this.cards.values()) {
            mageCard.setZone(zone);
        }
    }

    public Map<UUID, MageCard> getMageCardsForUpdate() {
        return this.cards;
    }

    public void addCardEventListener(Listener<Event> listener) {
        this.cardEventSource.addListener(listener);
    }

    @Override
    public CardEventSource getCardEventSource() {
        return this.cardEventSource;
    }
}
