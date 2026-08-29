/*
 * Decompiled with CFR.
 */
package mage.client.plugins.adapters;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import mage.cards.MageCard;
import mage.cards.MageCardSpace;
import mage.cards.action.ActionCallback;
import mage.cards.action.TransferData;
import mage.client.MageFrame;
import mage.client.MagePane;
import mage.client.SessionHandler;
import mage.client.cards.BigCard;
import mage.client.cards.CardEventProducer;
import mage.client.cards.Cards;
import mage.client.components.MageComponents;
import mage.client.components.MageUI;
import mage.client.dialog.PreferencesDialog;
import mage.client.game.GamePane;
import mage.client.plugins.impl.Plugins;
import mage.client.util.ClientEventType;
import mage.client.util.DefaultActionCallback;
import mage.client.util.Event;
import mage.client.util.gui.ArrowBuilder;
import mage.client.util.gui.ArrowUtil;
import mage.client.util.gui.GuiDisplayUtil;
import mage.components.CardInfoPane;
import mage.constants.EnlargeMode;
import mage.constants.Zone;
import mage.util.DebugUtil;
import mage.util.ThreadUtils;
import mage.view.CardView;
import mage.view.PermanentView;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXPanel;
import org.mage.plugins.card.images.ImageCache;

public class MageActionCallback
implements ActionCallback {
    private static final Logger logger = Logger.getLogger(MageActionCallback.class);
    public static final int HAND_CARDS_BETWEEN_GAP_X = 5;
    public static final int STACK_CARDS_BETWEEN_GAP_X = 5;
    public static final MageCardSpace HAND_CARDS_MARGINS = new MageCardSpace(10, 10, 5, 5);
    public static final MageCardSpace STACK_CARDS_MARGINS = new MageCardSpace(10, 10, 5, 5);
    public static final int HAND_CARDS_COMPARE_GAP_X = 30;
    public static final int HAND_CARDS_MIN_DISTANCE_TO_START_DRAGGING = 20;
    public static final int GO_DOWN_ON_DRAG_Y_OFFSET = 0;
    public static final int GO_UP_ON_DRAG_Y_OFFSET = 0;
    private Popup tooltipPopup;
    private BigCard bigCard;
    private CardView tooltipCard;
    private TransferData popupData;
    private JComponent cardInfoPane;
    private volatile boolean popupTextWindowOpen = false;
    private int tooltipDelay;
    private Date enlargeredViewOpened;
    private volatile EnlargedWindowState enlargedWindowState = EnlargedWindowState.CLOSED;
    private volatile EnlargeMode enlargeMode;
    private static final ScheduledExecutorService hideEnlargedCardWorker = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> hideEnlagedCardTask;
    private static final int HIDE_ENLARGED_CARD_TIMEOUT_MS = 700;
    private MageCard prevCardPanel;
    private boolean startedDragging;
    private boolean isDragging;
    private Point initialCardPos = null;
    private Point initialMousePos = null;
    private final Set<MageCard> draggingCards = new HashSet<MageCard>();

    public MageActionCallback() {
        this.enlargeMode = EnlargeMode.NORMAL;
    }

    public void setCardPreviewComponent(BigCard bigCard) {
        this.bigCard = bigCard;
    }

    public synchronized void refreshSession() {
        if (this.cardInfoPane == null) {
            this.cardInfoPane = Plugins.instance.getCardInfoPane();
        }
    }

    public void mouseClicked(MouseEvent e, TransferData data, boolean doubleClick) {
        if (e.isConsumed()) {
            return;
        }
        if (!(e.isPopupTrigger() || SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e))) {
            return;
        }
        if (data.getComponent().getCardContainer() instanceof CardEventProducer) {
            ClientEventType clickType = doubleClick ? ClientEventType.CARD_DOUBLE_CLICK : ClientEventType.CARD_CLICK;
            CardEventProducer cardContainer = (CardEventProducer)((Object)data.getComponent().getCardContainer());
            Event clientEvent = new Event(data.getComponent().getOriginal(), clickType, 0, e.getX(), e.getY(), (Component)data.getComponent(), e, false);
            cardContainer.getCardEventSource().fireEvent(clientEvent);
        }
    }

    public void mouseEntered(MouseEvent e, TransferData data) {
        this.popupData = data;
        this.handleMouseMoveOverNewCard(data);
    }

    private void startCardHintPopup(TransferData data, Component parentComponent, Point parentPoint) {
        MageCard cardPanel = data.getComponent().getTopPanelRef();
        this.tooltipDelay = data.getTooltipDelay() > 0 ? data.getTooltipDelay() : PreferencesDialog.getCachedValue("showTooltipsDelay", 300);
        if (this.tooltipDelay == 0) {
            return;
        }
        if (this.cardInfoPane == null) {
            if (data.getLocationOnScreen() == null) {
                data.setLocationOnScreen(cardPanel.getCardLocationOnScreen().getCardPoint());
            }
            int newLocationX = (int)data.getLocationOnScreen().getX() + data.getPopupOffsetX();
            int newLocationY = (int)data.getLocationOnScreen().getY() + data.getPopupOffsetY() + 40;
            PopupFactory factory = PopupFactory.getSharedInstance();
            data.getPopupText().updateText();
            this.tooltipPopup = factory.getPopup((Component)cardPanel, (Component)data.getPopupText(), newLocationX, newLocationY);
            this.tooltipPopup.show();
            this.tooltipPopup.hide();
            this.tooltipPopup = factory.getPopup((Component)cardPanel, (Component)data.getPopupText(), newLocationX, newLocationY);
            this.tooltipPopup.show();
        } else {
            this.showCardHintPopup(data, parentComponent, parentPoint);
        }
    }

    private void showCardHintPopup(final TransferData data, final Component parentComponent, final Point parentPoint) {
        final MageCard cardPanel = data.getComponent().getTopPanelRef();
        MageUI.threadPoolPopups.submit(new Runnable(){

            @Override
            public void run() {
                ThreadUtils.sleep(MageActionCallback.this.tooltipDelay);
                if (MageActionCallback.this.tooltipCard == null || !MageActionCallback.this.tooltipCard.equals(data.getCard()) || SessionHandler.getSession() == null || !MageActionCallback.this.popupTextWindowOpen || MageActionCallback.this.enlargedWindowState != EnlargedWindowState.CLOSED) {
                    return;
                }
                try {
                    Component popupContainer = MageFrame.getUI().getComponent(MageComponents.POPUP_CONTAINER);
                    Component popupInfo = MageFrame.getUI().getComponent(MageComponents.CARD_INFO_PANE);
                    ((CardInfoPane)popupInfo).setCard(data.getCard(), popupContainer);
                    this.showPopup(popupContainer, popupInfo);
                }
                catch (InterruptedException e) {
                    logger.error((Object)"Can't show card tooltip", (Throwable)e);
                    Thread.currentThread().interrupt();
                }
            }

            public void showPopup(Component popupContainer, Component infoPane) throws InterruptedException {
                Component c = MageFrame.getUI().getComponent(MageComponents.DESKTOP_PANE);
                SwingUtilities.invokeLater(() -> {
                    if (!MageActionCallback.this.popupTextWindowOpen || MageActionCallback.this.enlargedWindowState != EnlargedWindowState.CLOSED) {
                        return;
                    }
                    if (data.getLocationOnScreen() == null) {
                        data.setLocationOnScreen(cardPanel.getCardLocationOnScreen().getCardPoint());
                    }
                    if (DebugUtil.GUI_POPUP_CONTAINER_DRAW_DEBUG_BORDER) {
                        ((JComponent)infoPane).setBorder(BorderFactory.createLineBorder(Color.green));
                    }
                    Point location = MageActionCallback.this.preparePopupContainerLocation(popupContainer, infoPane, data, parentPoint, parentComponent);
                    popupContainer.setLocation(location);
                    popupContainer.setVisible(true);
                    c.repaint();
                });
            }
        });
    }

    public void mousePressed(MouseEvent e, TransferData data) {
        MageCard cardPanel = data.getComponent().getTopPanelRef();
        cardPanel.requestFocusInWindow();
        this.clearDragging(this.prevCardPanel);
        this.isDragging = false;
        this.startedDragging = false;
        this.prevCardPanel = null;
        this.draggingCards.clear();
        Point mouse = new Point(e.getX(), e.getY());
        SwingUtilities.convertPointToScreen(mouse, (Component)data.getComponent());
        this.initialMousePos = new Point((int)mouse.getX(), (int)mouse.getY());
        this.initialCardPos = cardPanel.getCardLocation().getCardPoint();
        this.hideTooltipPopup();
    }

    public void mouseReleased(MouseEvent e, TransferData data) {
        MageCard cardPanel = data.getComponent().getTopPanelRef();
        if (e.isPopupTrigger()) {
            this.hideTooltipPopup();
        } else if (cardPanel.getZone() == Zone.HAND) {
            boolean needClick = false;
            if (!this.isDragging) {
                needClick = true;
            }
            this.clearDragging(cardPanel);
            this.startedDragging = false;
            if (needClick) {
                this.simulateCardClick(data);
            }
            e.consume();
        } else {
            boolean floatingStackEvent;
            boolean bl = floatingStackEvent = cardPanel.getCardContainer() instanceof Cards && ((Cards)cardPanel.getCardContainer()).isVerticalStackLayout();
            if (!floatingStackEvent) {
                this.simulateCardClick(data);
            }
            e.consume();
        }
    }

    private void simulateCardClick(TransferData data) {
        MageCard cardPanel = data.getComponent().getTopPanelRef();
        cardPanel.requestFocusInWindow();
        DefaultActionCallback.instance.mouseClicked(data.getGameId(), data.getCard());
        this.hideTooltipPopup();
    }

    private void clearDragging(MageCard clearCard) {
        if (this.startedDragging && this.prevCardPanel != null && clearCard != null) {
            for (Component comp : clearCard.getCardContainer().getComponents()) {
                MageCard realCard;
                if (!(comp instanceof MageCard) || !this.draggingCards.contains(realCard = (MageCard)comp)) continue;
                realCard.setCardLocation(realCard.getCardLocation().getCardX(), realCard.getCardLocation().getCardY() - 0);
            }
            clearCard.setCardLocation(clearCard.getCardLocation().getCardX(), clearCard.getCardLocation().getCardY() + 0);
            this.sortHandCards(clearCard, clearCard.getCardContainer(), true);
            this.draggingCards.clear();
        }
        this.prevCardPanel = null;
    }

    public void mouseMoved(MouseEvent e, TransferData data) {
        if (!Plugins.instance.isCardPluginLoaded()) {
            return;
        }
        if (this.popupData == null || !this.popupData.getCard().equals(data.getCard())) {
            this.popupData = data;
            this.handleMouseMoveOverNewCard(data);
        }
        if (this.bigCard == null) {
            return;
        }
        this.updateCardHints(data);
    }

    public void mouseDragged(MouseEvent e, TransferData data) {
        MageCard cardPanel = data.getComponent().getTopPanelRef();
        if (cardPanel.getZone() != Zone.HAND) {
            return;
        }
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        if (this.initialMousePos == null || this.initialCardPos == null) {
            return;
        }
        Point mouse = new Point(e.getX(), e.getY());
        SwingUtilities.convertPointToScreen(mouse, (Component)data.getComponent());
        if (!this.isDragging && Math.abs(mouse.x - this.initialMousePos.x) < 20 && Math.abs(mouse.y - this.initialMousePos.y) < 20) {
            return;
        }
        this.isDragging = true;
        this.prevCardPanel = cardPanel;
        Point cardPanelLocationOld = cardPanel.getCardLocation().getCardPoint();
        int xOffset = 0;
        int newX = Math.max(this.initialCardPos.x + (int)(mouse.getX() - (double)this.initialMousePos.x) - xOffset, 0);
        cardPanel.setCardBounds(newX, cardPanelLocationOld.y, cardPanel.getCardLocation().getCardWidth(), cardPanel.getCardLocation().getCardHeight());
        cardPanel.getCardContainer().setComponentZOrder((Component)cardPanel, 0);
        this.sortHandCards(cardPanel, cardPanel.getCardContainer(), false);
        if (!this.startedDragging) {
            this.startedDragging = true;
        }
    }

    public void mouseExited(MouseEvent e, TransferData data) {
        if (data != null) {
            this.hideAll(data.getGameId());
        } else {
            this.hideAll(null);
        }
    }

    public void popupMenuCard(MouseEvent e, TransferData data) {
        if (e.isConsumed()) {
            return;
        }
        e.consume();
        if (data.getComponent().getCardContainer() instanceof CardEventProducer) {
            CardEventProducer area = (CardEventProducer)((Object)data.getComponent().getCardContainer());
            Event clientEvent = new Event(data.getComponent().getOriginal(), ClientEventType.CARD_POPUP_MENU, 0, e.getX(), e.getY(), (Component)data.getComponent(), e, false);
            area.getCardEventSource().fireEvent(clientEvent);
        }
    }

    public void popupMenuPanel(MouseEvent e, Component sourceComponent) {
        if (e.isConsumed()) {
            return;
        }
        e.consume();
        if (sourceComponent instanceof CardEventProducer) {
            CardEventProducer area = (CardEventProducer)((Object)sourceComponent);
            Event clientEvent = new Event(null, ClientEventType.CARD_POPUP_MENU, 0, e.getX(), e.getY(), e.getComponent(), e, false);
            area.getCardEventSource().fireEvent(clientEvent);
        }
    }

    private void sortHandCards(MageCard card, Container container, boolean sortSource) {
        ArrayList<MageCard> cards = new ArrayList<MageCard>();
        for (Component comp : container.getComponents()) {
            if (!(comp instanceof MageCard)) continue;
            MageCard realCard = (MageCard)comp;
            if (!realCard.equals(card)) {
                if (!this.draggingCards.contains(realCard)) {
                    realCard.setCardLocation(realCard.getCardLocation().getCardX(), realCard.getCardLocation().getCardY() + 0);
                }
                this.draggingCards.add(realCard);
            } else if (!this.startedDragging) {
                realCard.setCardLocation(realCard.getCardLocation().getCardX(), realCard.getCardLocation().getCardY() - 0);
            }
            cards.add(realCard);
        }
        this.sortAndAnimateDraggingHandCards(cards, card, sortSource);
    }

    private void sortAndAnimateDraggingHandCards(List<MageCard> cards, MageCard source, boolean includeSource) {
        int draggingOffsetX = 0;
        source.setCardLocation(source.getCardLocation().getCardX() - draggingOffsetX, source.getCardLocation().getCardY());
        cards.sort(Comparator.comparingInt(cp -> cp.getCardLocation().getCardX()));
        int dx = MageActionCallback.getHandOrStackMargins(source.getZone()).getLeft();
        boolean createdGapForSource = false;
        for (MageCard component : cards) {
            if (!includeSource) {
                if (component.equals(source)) continue;
                component.setCardLocation(dx, component.getCardLocation().getCardY());
                if (createdGapForSource || (dx += component.getCardLocation().getCardWidth() + MageActionCallback.getHandOrStackBetweenGapX(source.getZone())) + 30 <= source.getCardLocation().getCardX()) continue;
                createdGapForSource = true;
                int gapOffset = component.getCardLocation().getCardWidth() + MageActionCallback.getHandOrStackBetweenGapX(source.getZone());
                dx += gapOffset;
                if (!cards.get(0).equals(source) || cards.size() <= 1 || !cards.get(1).equals(component)) continue;
                component.setCardLocation(component.getCardLocation().getCardX() + gapOffset, component.getCardLocation().getCardY());
                continue;
            }
            component.setCardLocation(dx, component.getCardLocation().getCardY());
            dx += component.getCardLocation().getCardWidth() + MageActionCallback.getHandOrStackBetweenGapX(source.getZone());
        }
    }

    private void handleMouseMoveOverNewCard(TransferData data) {
        MageCard cardPanel = data.getComponent().getTopPanelRef();
        MagePane topPane = MageFrame.getTopMost(null);
        if (topPane instanceof GamePane && data.getGameId() != null && !((GamePane)topPane).getGameId().equals(data.getGameId())) {
            return;
        }
        this.hideTooltipPopup();
        this.cancelHidingEnlagedCard();
        Component parentComponent = SwingUtilities.getRoot((Component)cardPanel);
        if (parentComponent == null) {
            parentComponent = MageFrame.getDesktop();
        }
        Point parentPoint = parentComponent.getLocationOnScreen();
        if (data.getLocationOnScreen() == null) {
            data.setLocationOnScreen(cardPanel.getCardLocationOnScreen().getCardPoint());
        }
        ArrowUtil.drawArrowsForTargets(data, parentPoint);
        ArrowUtil.drawArrowsForSource(data, parentPoint);
        ArrowUtil.drawArrowsForPairedCards(data, parentPoint);
        ArrowUtil.drawArrowsForBandedCards(data, parentPoint);
        ArrowUtil.drawArrowsForEnchantPlayers(data, parentPoint);
        this.tooltipCard = data.getCard();
        this.startCardHintPopup(data, parentComponent, parentPoint);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private void updateCardHints(TransferData data) {
        MageCard cardPanel = data.getComponent().getTopPanelRef();
        if (this.popupTextWindowOpen && Objects.equals(cardPanel.getOriginal().getId(), this.bigCard.getCardId())) return;
        if (this.bigCard.getWidth() > 0) {
            Class<MageActionCallback> clazz = MageActionCallback.class;
            synchronized (MageActionCallback.class) {
                if (!this.popupTextWindowOpen || !Objects.equals(cardPanel.getOriginal().getId(), this.bigCard.getCardId())) {
                    if (!this.popupTextWindowOpen) {
                        this.bigCard.resetCardId();
                    }
                    this.popupTextWindowOpen = true;
                    Image image = cardPanel.getImage();
                    this.displayCardInfo(cardPanel.getOriginal(), image, this.bigCard);
                }
                // ** MonitorExit[var3_3] (shouldn't be in output)
            }
        } else {
            this.popupTextWindowOpen = true;
        }
        {
            if (this.enlargedWindowState == EnlargedWindowState.CLOSED) return;
            this.cancelHidingEnlagedCard();
            this.displayEnlargedCard(cardPanel.getOriginal(), data);
            return;
        }
    }

    public void hideOpenComponents() {
        this.hideAll(null);
    }

    public void hideTooltipPopup() {
        this.tooltipCard = null;
        if (this.tooltipPopup != null) {
            this.tooltipPopup.hide();
        }
        try {
            if (SessionHandler.getSession() == null) {
                return;
            }
            Component popupContainer = MageFrame.getUI().getComponent(MageComponents.POPUP_CONTAINER);
            popupContainer.setVisible(false);
        }
        catch (InterruptedException e) {
            logger.error((Object)"Can't hide card tooltip", (Throwable)e);
            Thread.currentThread().interrupt();
        }
    }

    public void hideGameUpdate(UUID gameId) {
        ArrowBuilder.getBuilder().removeArrowsByType(gameId, ArrowBuilder.Type.TARGET);
        ArrowBuilder.getBuilder().removeArrowsByType(gameId, ArrowBuilder.Type.PAIRED);
        ArrowBuilder.getBuilder().removeArrowsByType(gameId, ArrowBuilder.Type.BANDED);
        ArrowBuilder.getBuilder().removeArrowsByType(gameId, ArrowBuilder.Type.SOURCE);
        ArrowBuilder.getBuilder().removeArrowsByType(gameId, ArrowBuilder.Type.ENCHANT_PLAYERS);
    }

    public void hideAll(UUID gameId) {
        this.hideTooltipPopup();
        this.startHidingEnlagedCard();
        this.popupTextWindowOpen = false;
        if (gameId != null) {
            ArrowBuilder.getBuilder().removeArrowsByType(gameId, ArrowBuilder.Type.TARGET);
            ArrowBuilder.getBuilder().removeArrowsByType(gameId, ArrowBuilder.Type.PAIRED);
            ArrowBuilder.getBuilder().removeArrowsByType(gameId, ArrowBuilder.Type.BANDED);
            ArrowBuilder.getBuilder().removeArrowsByType(gameId, ArrowBuilder.Type.SOURCE);
            ArrowBuilder.getBuilder().removeArrowsByType(gameId, ArrowBuilder.Type.ENCHANT_PLAYERS);
        }
    }

    public void mouseWheelMoved(int mouseWheelRotation, TransferData data) {
        if (this.enlargedWindowState != EnlargedWindowState.CLOSED) {
            if (this.enlargeredViewOpened != null && new Date().getTime() - this.enlargeredViewOpened.getTime() > 1000L) {
                this.hideEnlargedCard();
                this.handleMouseMoveOverNewCard(data);
            } else if (this.enlargeMode == EnlargeMode.NORMAL) {
                if (mouseWheelRotation > 0) {
                    this.hideEnlargedCard();
                    this.handleMouseMoveOverNewCard(data);
                }
            } else if (mouseWheelRotation < 0) {
                this.hideEnlargedCard();
                this.handleMouseMoveOverNewCard(data);
            }
            return;
        }
        if (mouseWheelRotation < 0) {
            this.enlargeCard(EnlargeMode.NORMAL);
        } else {
            this.enlargeCard(EnlargeMode.ALTERNATE);
        }
    }

    public void enlargeCard(EnlargeMode showAlternative) {
        if (this.enlargedWindowState == EnlargedWindowState.CLOSED) {
            this.enlargeMode = showAlternative;
            CardView cardView = null;
            if (this.popupData != null) {
                cardView = this.popupData.getCard();
            }
            if (this.popupTextWindowOpen) {
                this.hideTooltipPopup();
            }
            if (cardView != null) {
                this.enlargedWindowState = cardView.isToRotate() ? EnlargedWindowState.ROTATED : EnlargedWindowState.NORMAL;
                this.displayEnlargedCard(cardView, this.popupData);
            }
        }
    }

    public void hideEnlargedCard() {
        this.enlargedWindowState = EnlargedWindowState.CLOSED;
        try {
            Component cardPreviewContainer = MageFrame.getUI().getComponent(MageComponents.CARD_PREVIEW_CONTAINER);
            if (cardPreviewContainer.isVisible()) {
                cardPreviewContainer.setVisible(false);
                cardPreviewContainer.repaint();
            }
            if ((cardPreviewContainer = MageFrame.getUI().getComponent(MageComponents.CARD_PREVIEW_CONTAINER_ROTATED)).isVisible()) {
                cardPreviewContainer.setVisible(false);
                cardPreviewContainer.repaint();
            }
        }
        catch (InterruptedException e) {
            logger.warn((Object)"Can't hide enlarged card", (Throwable)e);
        }
    }

    private void displayEnlargedCard(CardView cardView, TransferData data) {
        MageCard cardPanel = data.getComponent().getTopPanelRef();
        MageUI.threadPoolPopups.submit(() -> {
            if (cardView == null) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                try {
                    MageComponents mageComponentCardPreviewPane;
                    MageComponents mageComponentCardPreviewContainer;
                    if (this.enlargedWindowState == EnlargedWindowState.CLOSED) {
                        return;
                    }
                    if (cardView.isToRotate()) {
                        if (this.enlargedWindowState == EnlargedWindowState.NORMAL) {
                            this.hideEnlargedCard();
                            this.enlargedWindowState = EnlargedWindowState.ROTATED;
                        }
                        mageComponentCardPreviewContainer = MageComponents.CARD_PREVIEW_CONTAINER_ROTATED;
                        mageComponentCardPreviewPane = MageComponents.CARD_PREVIEW_PANE_ROTATED;
                    } else {
                        if (this.enlargedWindowState == EnlargedWindowState.ROTATED) {
                            this.hideEnlargedCard();
                            this.enlargedWindowState = EnlargedWindowState.NORMAL;
                        }
                        mageComponentCardPreviewContainer = MageComponents.CARD_PREVIEW_CONTAINER;
                        mageComponentCardPreviewPane = MageComponents.CARD_PREVIEW_PANE;
                    }
                    Component popupContainer = MageFrame.getUI().getComponent(mageComponentCardPreviewContainer);
                    Component cardPreviewPane = MageFrame.getUI().getComponent(mageComponentCardPreviewPane);
                    Component parentComponent = SwingUtilities.getRoot((Component)cardPanel);
                    if (parentComponent == null) {
                        parentComponent = MageFrame.getDesktop();
                    }
                    if (cardPreviewPane != null && parentComponent != null) {
                        Point parentPoint = parentComponent.getLocationOnScreen();
                        if (DebugUtil.GUI_POPUP_CONTAINER_DRAW_DEBUG_BORDER) {
                            ((JComponent)cardPreviewPane).setBorder(BorderFactory.createLineBorder(Color.green));
                        }
                        if (data.getLocationOnScreen() == null) {
                            data.setLocationOnScreen(cardPanel.getCardLocationOnScreen().getCardPoint());
                        }
                        Point location = this.preparePopupContainerLocation(popupContainer, cardPreviewPane, data, parentPoint, parentComponent);
                        popupContainer.setLocation(location);
                        popupContainer.setVisible(true);
                        Image image = cardPanel.getImage();
                        CardView displayCard = cardPanel.getOriginal();
                        switch (this.enlargeMode) {
                            case COPY: {
                                if (!(cardView instanceof PermanentView)) break;
                                image = ImageCache.getCardImageOriginal(((PermanentView)cardView).getOriginal()).getImage();
                                break;
                            }
                            case ALTERNATE: {
                                if (cardView.getAlternateName() == null) break;
                                if (cardView instanceof PermanentView && !cardView.isFlipCard() && !cardView.canTransform() && ((PermanentView)cardView).isCopy()) {
                                    image = ImageCache.getCardImageOriginal(((PermanentView)cardView).getOriginal()).getImage();
                                    break;
                                }
                                image = ImageCache.getCardImageAlternate(cardView).getImage();
                                if ((displayCard = displayCard.getSecondCardFace()) != null) break;
                                displayCard = cardPanel.getOriginal();
                                break;
                            }
                        }
                        this.displayCardInfo(displayCard, image, (BigCard)cardPreviewPane);
                    } else {
                        logger.warn((Object)("No Card preview Pane in Mage Frame defined. Card: " + cardView.getName()));
                    }
                }
                catch (Exception e) {
                    logger.warn((Object)"Problem dring display of enlarged card", (Throwable)e);
                }
            });
        });
    }

    private Point preparePopupContainerLocation(Component popupContainer, Component popupComponent, TransferData data, Point parentPoint, Component parentComponent) {
        Point location;
        switch (data.getPopupAutoLocationMode()) {
            case PUT_INSIDE_PARENT: {
                location = new Point((int)data.getLocationOnScreen().getX() + data.getPopupOffsetX() - 40, (int)data.getLocationOnScreen().getY() + data.getPopupOffsetY() - 40);
                location = GuiDisplayUtil.keepComponentInsideParent(location, parentPoint, popupComponent, parentComponent);
                break;
            }
            case PUT_NEAR_MOUSE_POSITION: {
                boolean hasBottomSpace;
                location = MouseInfo.getPointerInfo().getLocation();
                boolean hasRightSpace = location.x + popupContainer.getWidth() < parentComponent.getX() + parentComponent.getWidth();
                boolean bl = hasBottomSpace = location.y + popupContainer.getHeight() < parentComponent.getY() + parentComponent.getHeight();
                if (!hasRightSpace) {
                    location.setLocation(location.x - popupContainer.getWidth(), location.y);
                }
                if (hasBottomSpace) break;
                location.setLocation(location.x, Math.max(parentComponent.getY(), location.y - popupContainer.getHeight()));
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupport auto-location " + data.getPopupAutoLocationMode());
            }
        }
        location.translate(-parentPoint.x, -parentPoint.y);
        return location;
    }

    private void displayCardInfo(CardView card, Image image, BigCard bigCard) {
        if (image instanceof BufferedImage) {
            bigCard.setCard(card.getId(), this.enlargeMode, image, card.getRules(), card.isToRotate());
            if (card.isAbility() && this.enlargeMode == EnlargeMode.NORMAL && this.isAbilityTextOverlayEnabled()) {
                bigCard.showTextComponent();
            } else {
                bigCard.hideTextComponent();
            }
        } else {
            JXPanel panel = GuiDisplayUtil.getDescription(card, bigCard.getWidth(), bigCard.getHeight());
            panel.setVisible(true);
            bigCard.hideTextComponent();
            bigCard.addJXPanel(card.getId(), panel);
        }
        this.enlargeredViewOpened = new Date();
    }

    private boolean isAbilityTextOverlayEnabled() {
        return PreferencesDialog.getCachedValue("cardRenderingAbilityTextOverlay", "true").equals("true");
    }

    private synchronized void startHidingEnlagedCard() {
        this.cancelHidingEnlagedCard();
        this.hideEnlagedCardTask = hideEnlargedCardWorker.schedule(() -> SwingUtilities.invokeLater(this::hideEnlargedCard), 700L, TimeUnit.MILLISECONDS);
    }

    private synchronized void cancelHidingEnlagedCard() {
        if (this.hideEnlagedCardTask != null) {
            this.hideEnlagedCardTask.cancel(false);
        }
    }

    public static MageCardSpace getHandOrStackMargins(Zone zone) {
        if (zone == Zone.HAND) {
            return HAND_CARDS_MARGINS;
        }
        return STACK_CARDS_MARGINS;
    }

    public static int getHandOrStackBetweenGapX(Zone zone) {
        if (zone == Zone.HAND) {
            return 5;
        }
        return 5;
    }

    static enum EnlargedWindowState {
        CLOSED,
        NORMAL,
        ROTATED;

    }
}
