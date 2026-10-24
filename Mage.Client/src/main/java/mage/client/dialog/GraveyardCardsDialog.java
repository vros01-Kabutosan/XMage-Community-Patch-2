package mage.client.dialog;

import mage.client.MageFrame;
import mage.client.cards.BigCard;
import mage.client.cards.CardArea;
import mage.client.components.MageDesktopIconifySupport;
import mage.client.util.ImageHelper;
import mage.client.util.SettingsManager;
import mage.client.util.gui.GuiDisplayUtil;
import mage.constants.CardType;
import mage.util.RandomUtil;
import mage.view.CardView;
import mage.view.CardsView;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Game GUI: graveyard window.
 * <p>
 * Visual clone of the in-game card chooser / library search window
 * ({@link ShowCardsDialog}): it uses the same component base
 * ({@link CardArea} inside a non-modal {@link MageDialog} internal frame),
 * so it inherits exactly the same grid layout, sorting, scrollbars and
 * container sizing behavior. The card amount does not change the look:
 * few cards render as a single row, many cards render as stacked
 * multi-columns (same as the card chooser window), from 1 to 30+ cards.
 * <p>
 * Unlike {@link ShowCardsDialog} (one-shot target pickers) this window is
 * persistent per player: it hides on close (HIDE_ON_CLOSE) and refreshes
 * its content on every game update without forcing itself visible again.
 *
 * @author AutoClaw (UI work requested by vros0)
 */
public class GraveyardCardsDialog extends MageDialog implements MageDesktopIconifySupport {

    private static final Logger LOGGER = Logger.getLogger(GraveyardCardsDialog.class);

    // remember if this dialog was already auto positioned, so don't do it after the first time
    private boolean positioned;
    private final String playerName;
    private CardArea cardArea;

    public GraveyardCardsDialog(String playerName) {
        this.playerName = playerName;
        this.positioned = false;
        this.title = playerName + "'s graveyard";
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        initComponents();
        this.setModal(false);
        this.setFrameIcon(new ImageIcon(ImageHelper.getImageFromResources("/info/grave.png")));
        this.setTitelBarToolTip(this.title);
    }

    public void cleanUp() {
        cardArea.cleanUp();
    }

    @Override
    public void changeGUISize() {
        cardArea.changeGUISize();
        this.validate();
        this.repaint();
    }

    @Override
    public void show() {
        super.show();

        // auto-position on first usage
        if (!positioned) {
            showAndPositionWindow();
        }
    }

    /**
     * Loads the current graveyard content and refreshes the window.
     * Same card renderer and container sizing as the card chooser /
     * library search window (CardArea + pack).
     */
    public void loadCardsAndShow(CardsView showCards, BigCard bigCard, UUID gameId, boolean revertOrder) {
        // revertOrder is ignored on purpose: the card grid of the search
        // window always sorts by card name, so the graveyard uses the same order
        cardArea.loadCards(showCards, bigCard, gameId);

        int qty = qtyCardTypes(showCards);
        String newTitle = playerName + "'s graveyard (" + showCards.size() + ")  -  " + qty
                + ((qty == 1) ? " card type" : " card types");
        setTitle(newTitle);
        this.setTitelBarToolTip(newTitle);

        pack();
        this.revalidate();
        this.repaint();

        // window settings (same layer policy as ShowCardsDialog)
        MageFrame.getDesktop().remove(this);
        MageFrame.getDesktop().add(this, this.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);

        // same as the card chooser window: showAndPositionWindow() makes the
        // window visible only on first usage, so a window closed by the user
        // stays hidden until it is opened again from the player panel
        showAndPositionWindow();
    }

    private void showAndPositionWindow() {
        SwingUtilities.invokeLater(() -> {
            int width = GraveyardCardsDialog.this.getWidth();
            int height = GraveyardCardsDialog.this.getHeight();
            if (width > 0 && height > 0) {
                Point centered = SettingsManager.instance.getComponentPosition(width, height);
                if (!positioned) {
                    // starting position
                    // little randomize to see multiple opened windows
                    int xPos = centered.x / 2 + RandomUtil.nextInt(50);
                    int yPos = centered.y / 2 + RandomUtil.nextInt(50);
                    GraveyardCardsDialog.this.setLocation(xPos, yPos);
                    show();
                    positioned = true;
                }
                GuiDisplayUtil.keepComponentInsideFrame(centered.x, centered.y, GraveyardCardsDialog.this);
            }
        });
    }

    private int qtyCardTypes(CardsView cardsView) {
        Set<String> cardTypesPresent = new LinkedHashSet<>();
        for (CardView card : cardsView.values()) {
            Set<CardType> cardTypes = EnumSet.noneOf(CardType.class);
            cardTypes.addAll(card.getCardTypes());
            for (CardType cardType : cardTypes) {
                cardTypesPresent.add(cardType.toString());
            }
        }
        return cardTypesPresent.size();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * Same structure as the ShowCardsDialog form (CardArea in BorderLayout.CENTER).
     */
    private void initComponents() {
        cardArea = new CardArea();

        setClosable(true);
        setResizable(true);
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(cardArea, java.awt.BorderLayout.CENTER);
        pack();
    }
}
