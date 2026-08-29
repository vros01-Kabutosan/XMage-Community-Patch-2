/*
 * Decompiled with CFR.
 */
package mage.client.game;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyVetoException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import mage.cards.Card;
import mage.cards.MageCard;
import mage.cards.action.ActionCallback;
import mage.choices.Choice;
import mage.client.MageFrame;
import mage.client.SessionHandler;
import mage.client.cards.BigCard;
import mage.client.cards.Cards;
import mage.client.chat.ChatPanelBasic;
import mage.client.combat.CombatManager;
import mage.client.components.HoverButton;
import mage.client.components.KeyboundButton;
import mage.client.components.MageComponents;
import mage.client.components.ability.AbilityPicker;
import mage.client.components.ext.dlg.DialogManager;
import mage.client.components.tray.MageTray;
import mage.client.dialog.CardHintsHelperDialog;
import mage.client.dialog.CardInfoWindowDialog;
import mage.client.dialog.MageDialog;
import mage.client.dialog.PickChoiceDialog;
import mage.client.dialog.PickMultiNumberDialog;
import mage.client.dialog.PickNumberDialog;
import mage.client.dialog.PickPileDialog;
import mage.client.dialog.PreferencesDialog;
import mage.client.dialog.ShowCardsDialog;
import mage.client.game.BattlefieldPanel;
import mage.client.game.FeedbackPanel;
import mage.client.game.FirstButtonMousePressedAction;
import mage.client.game.GamePane;
import mage.client.game.HandPanel;
import mage.client.game.HelperPanel;
import mage.client.game.PlayAreaPanel;
import mage.client.game.PlayAreaPanelOptions;
import mage.client.game.ReplayTask;
import mage.client.plugins.adapters.MageActionCallback;
import mage.client.plugins.impl.Plugins;
import mage.client.util.AppUtil;
import mage.client.util.CardsViewUtil;
import mage.client.util.ClientEventType;
import mage.client.util.DefaultActionCallback;
import mage.client.util.Event;
import mage.client.util.GUISizeHelper;
import mage.client.util.Listener;
import mage.client.util.audio.AudioManager;
import mage.client.util.gui.ArrowBuilder;
import mage.client.util.gui.MageDialogState;
import mage.constants.AbilityType;
import mage.constants.EnlargeMode;
import mage.constants.MageObjectType;
import mage.constants.PhaseStep;
import mage.constants.PlayerAction;
import mage.constants.Zone;
import mage.game.events.PlayerQueryEvent;
import mage.players.PlayableObjectStats;
import mage.players.PlayableObjectsList;
import mage.util.DebugUtil;
import mage.util.MultiAmountMessage;
import mage.view.AbilityPickerView;
import mage.view.CardView;
import mage.view.CardsView;
import mage.view.CommandObjectView;
import mage.view.DungeonView;
import mage.view.ExileView;
import mage.view.GameView;
import mage.view.LookedAtView;
import mage.view.PermanentView;
import mage.view.PlayerView;
import mage.view.RevealedView;
import mage.view.SimpleCardView;
import mage.view.SimpleCardsView;
import mage.view.UserRequestMessage;
import org.apache.log4j.Logger;
import org.mage.plugins.card.utils.impl.ImageManagerImpl;

public final class GamePanel
extends JPanel {
    private static final String XCP_STACK_NO_VISIBLE_DRAG_LABEL_MARKER = "XCP_STACK_NO_VISIBLE_DRAG_LABEL_V1_2_1_8";
    private static final String XCP_STACK_VISIBLE_COUNT_MARKER = "XCP_STACK_VISIBLE_COUNT_V1_2_1_9";
    private static final Logger logger = Logger.getLogger(GamePanel.class);
    private static final String YOUR_HAND = "Your hand";
    private static final int SKIP_BUTTONS_SPACE_H = 3;
    private static final int SKIP_BUTTONS_SPACE_V = 3;
    private static final int PHASE_BUTTONS_SPACE_H = 3;
    private static final int PHASE_BUTTONS_SPACE_V = 3;
    private static final String CMD_AUTO_ORDER_FIRST = "cmdAutoOrderFirst";
    private static final String CMD_AUTO_ORDER_LAST = "cmdAutoOrderLast";
    private static final String CMD_AUTO_ORDER_NAME_FIRST = "cmdAutoOrderNameFirst";
    private static final String CMD_AUTO_ORDER_NAME_LAST = "cmdAutoOrderNameLast";
    private static final String CMD_AUTO_ORDER_RESET_ALL = "cmdAutoOrderResetAll";
    private static final double DIVIDER_KEEP_LEFT_COMPONENT = 0.0;
    private static final double DIVIDER_KEEP_RIGHT_COMPONENT = 1.0;
    private static final double DIVIDER_KEEP_PROPORTION = 0.5;
    private static final int DIVIDER_POSITION_DEFAULT = -1;
    private static final int DIVIDER_POSITION_HIDDEN_LEFT_OR_TOP = -2;
    private static final int DIVIDER_POSITION_HIDDEN_RIGHT_OR_BOTTOM = -3;
    private final Map<UUID, PlayAreaPanel> players = new LinkedHashMap<UUID, PlayAreaPanel>();
    private final Map<UUID, Boolean> playersWhoLeft = new LinkedHashMap<UUID, Boolean>();
    private final Map<UUID, CardInfoWindowDialog> exiles = new HashMap<UUID, CardInfoWindowDialog>();
    private final Map<String, CardInfoWindowDialog> revealed = new HashMap<String, CardInfoWindowDialog>();
    private final Map<String, CardInfoWindowDialog> lookedAt = new HashMap<String, CardInfoWindowDialog>();
    private final Map<String, CardsView> graveyards = new HashMap<String, CardsView>();
    private final Map<String, CardInfoWindowDialog> graveyardWindows = new HashMap<String, CardInfoWindowDialog>();
    private final Map<String, CardInfoWindowDialog> companion = new HashMap<String, CardInfoWindowDialog>();
    private final Map<String, CardsView> sideboards = new HashMap<String, CardsView>();
    private final Map<String, CardInfoWindowDialog> sideboardWindows = new HashMap<String, CardInfoWindowDialog>();
    private final ArrayList<ShowCardsDialog> pickTarget = new ArrayList();
    private final ArrayList<PickPileDialog> pickPile = new ArrayList();
    private final Map<String, CardHintsHelperDialog> cardHintsWindows = new LinkedHashMap<String, CardHintsHelperDialog>();
    private UUID currentTableId;
    private UUID parentTableId;
    private UUID gameId;
    private UUID playerId;
    GamePane gamePane;
    private ReplayTask replayTask;
    private final PickNumberDialog pickNumber;
    private final PickMultiNumberDialog pickMultiNumber;
    private JLayeredPane jLayeredPane;
    private String chosenHandKey = "You";
    private final skipButtonsList skipButtons = new skipButtonsList();
    private boolean menuNameSet = false;
    private boolean handCardsOfOpponentAvailable = false;
    private final Map<String, Card> loadedCards = new HashMap<String, Card>();
    private static final String XCP_UI_RESIZE_V1_3 = "XCP_UI_RESIZE_V1_3";
    private final Map<String, HoverButton> phaseButtons = new LinkedHashMap<String, HoverButton>();
    private final Map<String, MageSplitter> splitters = new LinkedHashMap<String, MageSplitter>();
    private boolean isSplittersFullyRestored = false;
    private MageDialogState choiceWindowState;
    private boolean initComponents = true;
    private Timer resizeTimer;
    private CardView cardViewPopupMenu;
    private JPopupMenu popupMenuTriggerOrder;
    private final LastGameData lastGameData = new LastGameData();
    private static final int BORDER_SIZE = 2;
    private static final Border BORDER_ACTIVE = new LineBorder(Color.orange, 2);
    private static final Border BORDER_NON_ACTIVE = new EmptyBorder(2, 2, 2, 2);
    private static final int holdPriorityMask = System.getProperty("os.name").contains("Mac OS X") ? 256 : 128;
    private boolean holdingPriority;
    private AbilityPicker abilityPicker;
    private BigCard bigCard;
    private KeyboundButton btnToggleMacro;
    private KeyboundButton btnCancelSkip;
    private KeyboundButton btnSkipToNextTurn;
    private KeyboundButton btnSkipToEndTurn;
    private KeyboundButton btnSkipToNextMain;
    private KeyboundButton btnSkipStack;
    private KeyboundButton btnSkipToYourTurn;
    private KeyboundButton btnSkipToEndStepBeforeYourTurn;
    private JButton btnConcede;
    private JButton btnSwitchHands;
    private JButton btnNextPlay;
    private JButton btnPlay;
    private JButton btnPreviousPlay;
    private JButton btnSkipForward;
    private JButton btnStopReplay;
    private JButton btnStopWatching;
    private ChatPanelBasic gameChatPanel;
    private FeedbackPanel feedbackPanel;
    private HelperPanel helper;
    private ChatPanelBasic userChatPanel;
    private JPanel bigCardPanel;
    private JPanel pnlHelperHandButtonsStackArea;
    private JSplitPane splitGameAndBigCard;
    private JSplitPane splitBattlefieldAndChats;
    private JSplitPane splitChatAndLogs;
    private JSplitPane splitHandAndStack;
    private JLabel lblActivePlayer;
    private JLabel lblPhase;
    private JLabel lblPriority;
    private JLabel lblStep;
    private JLabel lblTurn;
    private JPanel pnlBattlefield;
    private JPanel pnlShortCuts;
    private JPanel pnlReplay;
    private JLabel txtActivePlayer;
    private JLabel txtPhase;
    private JLabel txtPriority;
    private JLabel txtStep;
    private JLabel txtTurn;
    private Map<String, CardsView> handCards;
    private Cards stackObjects;
    private JInternalFrame floatingStackFrame;
    private JLabel floatingStackTitleLabel;
    private JLabel floatingStackTypeLabel;
    private JLabel floatingStackOrderLabel;
    private JPanel floatingStackHeader;
    private static final String STACK_HEADER_BADGE_MARKER = "XCP_STACK_HEADER_BADGE_V1_2_1_9";
    private Point floatingStackDragOrigin;
    private Point floatingStackResizeOrigin;
    private Dimension floatingStackResizeStartSize;
    private boolean floatingStackHadObjects = false;
    private boolean floatingStackRestoringBounds = false;
    private static final String XCP_STACK_PREF_NODE = "xcpFloatingStackV5";
    private static final String XCP_STACK_X = "x";
    private static final String XCP_STACK_Y = "y";
    private static final String XCP_STACK_W = "width";
    private static final String XCP_STACK_H = "height";
    private HandPanel handContainer;
    private JPanel jPhases;
    private JPanel phasesContainer;
    private JLabel txtHoldPriority;
    private boolean imagePanelState;

    private static void xcpForceLayoutTree(Component component) {
        if (!(component instanceof Container)) {
            return;
        }
        Container container = (Container)component;
        container.doLayout();
        for (Component child : container.getComponents()) {
            GamePanel.xcpForceLayoutTree(child);
        }
    }

    private void xcpRevalidateAncestors() {
        for (Container current = this; current != null; current = current.getParent()) {
            ((Component)current).revalidate();
            current.doLayout();
        }
    }

    private void xcpRefreshBattlefields() {
        for (PlayAreaPanel playArea : this.players.values()) {
            BattlefieldPanel battlefield = playArea.getBattlefieldPanel();
            battlefield.updateSize();
            this.xcpNormalizeBattlefieldViewport(battlefield);
            battlefield.revalidate();
        }
    }

    private void xcpNormalizeBattlefieldViewport(BattlefieldPanel battlefield) {
        JLayeredPane mainPanel = battlefield.getMainPanel();
        Dimension preferred = mainPanel.getPreferredSize();
        int maxBottom = 0;
        for (Component component : mainPanel.getComponents()) {
            if (!component.isVisible()) continue;
            Rectangle bounds = component.getBounds();
            maxBottom = Math.max(maxBottom, bounds.y + bounds.height);
        }
        int normalizedHeight = Math.max(1, maxBottom);
        if (preferred.height > normalizedHeight) {
            mainPanel.setPreferredSize(new Dimension(preferred.width, normalizedHeight));
        }
    }

    public LastGameData getLastGameData() {
        return this.lastGameData;
    }

    public GamePanel() {
        this.initComponents();
        if (DebugUtil.GUI_GAME_DRAW_COMMANDS_PANEL_BORDER) {
            this.pnlHelperHandButtonsStackArea.setBorder(BorderFactory.createLineBorder(Color.MAGENTA));
        }
        this.pnlHelperHandButtonsStackArea.removeAll();
        this.pnlHelperHandButtonsStackArea.setLayout(new BorderLayout());
        JPanel pnlBattlefieldAndPhases = new JPanel(new BorderLayout());
        pnlBattlefieldAndPhases.setOpaque(false);
        pnlBattlefieldAndPhases.add((Component)this.pnlBattlefield, "Center");
        pnlBattlefieldAndPhases.add((Component)this.phasesContainer, "East");
        this.pnlHelperHandButtonsStackArea.add((Component)pnlBattlefieldAndPhases, "Center");
        JPanel pnlCommandsRoot = new JPanel(new BorderLayout());
        pnlCommandsRoot.setOpaque(false);
        JPanel pnlCommandsFeedbackAndHand = new JPanel(new BorderLayout());
        pnlCommandsFeedbackAndHand.setOpaque(false);
        pnlCommandsFeedbackAndHand.add((Component)this.feedbackPanel, "North");
        pnlCommandsFeedbackAndHand.add((Component)this.handContainer, "Center");
        JPanel pnlCommandsSkipAndStack = new JPanel(new BorderLayout());
        pnlCommandsSkipAndStack.setOpaque(false);
        pnlCommandsSkipAndStack.add((Component)this.pnlShortCuts, "South");
        pnlCommandsFeedbackAndHand.setMinimumSize(new Dimension(0, 0));
        pnlCommandsSkipAndStack.setMinimumSize(new Dimension(0, 0));
        pnlCommandsRoot.add((Component)pnlCommandsFeedbackAndHand, "Center");
        pnlCommandsRoot.add((Component)pnlCommandsSkipAndStack, "South");
        this.pnlHelperHandButtonsStackArea.add((Component)pnlCommandsRoot, "South");
        if (DebugUtil.GUI_GAME_DRAW_SKIP_BUTTONS_PANEL_BORDER) {
            this.pnlShortCuts.setBorder(BorderFactory.createLineBorder(Color.red));
        }
        this.pnlShortCuts.removeAll();
        this.pnlShortCuts.setLayout(null);
        this.pnlShortCuts.add(this.btnSkipToNextTurn);
        this.pnlShortCuts.add(this.btnSkipToEndTurn);
        this.pnlShortCuts.add(this.btnSkipToNextMain);
        this.pnlShortCuts.add(this.btnSkipToYourTurn);
        this.pnlShortCuts.add(this.btnSkipStack);
        this.pnlShortCuts.add(this.btnSkipToEndStepBeforeYourTurn);
        this.pnlShortCuts.add(this.btnCancelSkip);
        this.pnlShortCuts.add(this.txtHoldPriority);
        this.pnlShortCuts.add(this.btnSwitchHands);
        this.pnlShortCuts.add(this.btnConcede);
        this.pnlShortCuts.add(this.btnStopWatching);
        this.pickNumber = new PickNumberDialog();
        MageFrame.getDesktop().add((Component)this.pickNumber, this.pickNumber.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        this.pickMultiNumber = new PickMultiNumberDialog();
        MageFrame.getDesktop().add((Component)this.pickMultiNumber, this.pickMultiNumber.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        this.feedbackPanel.setConnectedChatPanel(this.userChatPanel);
        this.setLayout(new BorderLayout());
        final JLayeredPane jLayeredBackgroundPane = new JLayeredPane();
        jLayeredBackgroundPane.setSize(1024, 768);
        this.add(jLayeredBackgroundPane);
        jLayeredBackgroundPane.add((Component)this.splitGameAndBigCard, JLayeredPane.DEFAULT_LAYER);
        Map<String, JComponent> myUi = this.getUIComponents(jLayeredBackgroundPane);
        Plugins.instance.updateGamePanel(myUi);
        this.initFloatingStackWindow();
        this.addComponentListener(new ComponentAdapter(){

            @Override
            public void componentResized(ComponentEvent e) {
                int width = ((JComponent)e.getSource()).getWidth();
                int height = ((JComponent)e.getSource()).getHeight();
                jLayeredBackgroundPane.setBounds(0, 0, width, height);
                jLayeredBackgroundPane.setPreferredSize(new Dimension(width, height));
                GamePanel.this.splitGameAndBigCard.setBounds(0, 0, width, height);
                GamePanel.this.splitGameAndBigCard.setPreferredSize(new Dimension(width, height));
                GamePanel.this.setMinimumSize(new Dimension(0, 0));
                GamePanel.this.setPreferredSize(new Dimension(width, height));
                GamePanel.this.pnlHelperHandButtonsStackArea.revalidate();
                GamePanel.this.pnlBattlefield.setMinimumSize(new Dimension(0, 0));
                GamePanel.xcpForceLayoutTree(GamePanel.this);
                GamePanel.this.xcpRefreshBattlefields();
                GamePanel.this.xcpRevalidateAncestors();
                SwingUtilities.invokeLater(() -> {
                    if (!GamePanel.this.isDisplayable()) {
                        return;
                    }
                    GamePanel.xcpForceLayoutTree(GamePanel.this);
                    GamePanel.this.xcpRefreshBattlefields();
                    GamePanel.this.xcpRevalidateAncestors();
                    GamePanel.this.repaint();
                });
                SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
                    if (!GamePanel.this.isDisplayable()) {
                        return;
                    }
                    GamePanel.xcpForceLayoutTree(GamePanel.this);
                    GamePanel.this.xcpRefreshBattlefields();
                    GamePanel.this.xcpRevalidateAncestors();
                    GamePanel.this.repaint();
                }));
                GamePanel.this.repaint();
                GamePanel.this.sizeToScreen();
            }
        });
        this.bigCardPanel.addComponentListener(new ComponentAdapter(){

            @Override
            public void componentResized(ComponentEvent e) {
                GamePanel.this.sizeBigCard();
            }
        });
        ComponentAdapter componentAdapterPlayField = new ComponentAdapter(){

            @Override
            public void componentResized(ComponentEvent e) {
                if (GamePanel.this.initComponents) {
                    return;
                }
                if (GamePanel.this.resizeTimer.isRunning()) {
                    GamePanel.this.resizeTimer.restart();
                } else {
                    GamePanel.this.resizeTimer.start();
                }
            }
        };
        this.resizeTimer = new Timer(1000, evt -> SwingUtilities.invokeLater(() -> {
            if (this.initComponents) {
                return;
            }
            this.resizeTimer.stop();
            this.setGUISize(false);
            this.feedbackPanel.changeGUISize();
            GamePanel.xcpForceLayoutTree(this);
            this.xcpRefreshBattlefields();
            this.xcpRevalidateAncestors();
            this.repaint();
            SwingUtilities.invokeLater(() -> {
                if (!this.isDisplayable()) {
                    return;
                }
                GamePanel.xcpForceLayoutTree(this);
                this.xcpRefreshBattlefields();
                this.xcpRevalidateAncestors();
                this.repaint();
            });
        }));
        this.pnlHelperHandButtonsStackArea.addComponentListener(componentAdapterPlayField);
        this.initComponents = false;
        this.setGUISize(true);
    }

    private Map<String, JComponent> getUIComponents(JLayeredPane jLayeredPane) {
        HashMap<String, JComponent> components = new HashMap<String, JComponent>();
        components.put("splitChatAndLogs", this.splitChatAndLogs);
        components.put("splitHandAndStack", this.splitHandAndStack);
        components.put("splitBattlefieldAndChats", this.splitBattlefieldAndChats);
        components.put("splitGameAndBigCard", this.splitGameAndBigCard);
        components.put("pnlBattlefield", this.pnlBattlefield);
        components.put("pnlHelperHandButtonsStackArea", this.pnlHelperHandButtonsStackArea);
        components.put("hand", this.handContainer);
        components.put("gameChatPanel", this.gameChatPanel);
        components.put("userChatPanel", this.userChatPanel);
        components.put("jLayeredPane", jLayeredPane);
        components.put("gamePanel", this);
        return components;
    }

    public void cleanUp() {
        MageFrame.removeGame(this.gameId);
        this.gameChatPanel.cleanUp();
        this.userChatPanel.cleanUp();
        this.removeListener();
        this.handContainer.cleanUp();
        this.disposeFloatingStackWindow();
        this.stackObjects.cleanUp();
        for (Map.Entry<UUID, PlayAreaPanel> entry : this.players.entrySet()) {
            entry.getValue().CleanUp();
        }
        this.players.clear();
        this.playersWhoLeft.clear();
        this.uninstallComponents();
        if (this.pickNumber != null) {
            this.pickNumber.removeDialog();
        }
        if (this.pickMultiNumber != null) {
            this.pickMultiNumber.removeDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.exiles.values()) {
            cardInfoWindowDialog.cleanUp();
            cardInfoWindowDialog.removeDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.graveyardWindows.values()) {
            cardInfoWindowDialog.cleanUp();
            cardInfoWindowDialog.removeDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.sideboardWindows.values()) {
            cardInfoWindowDialog.cleanUp();
            cardInfoWindowDialog.removeDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.revealed.values()) {
            cardInfoWindowDialog.cleanUp();
            cardInfoWindowDialog.removeDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.lookedAt.values()) {
            cardInfoWindowDialog.cleanUp();
            cardInfoWindowDialog.removeDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.companion.values()) {
            cardInfoWindowDialog.cleanUp();
            cardInfoWindowDialog.removeDialog();
        }
        for (CardHintsHelperDialog cardHintsHelperDialog : this.cardHintsWindows.values()) {
            cardHintsHelperDialog.cleanUp();
            cardHintsHelperDialog.removeDialog();
        }
        this.clearPickDialogs();
        Plugins.instance.getActionCallback().hideOpenComponents();
        try {
            Component popupContainer = MageFrame.getUI().getComponent(MageComponents.POPUP_CONTAINER);
            popupContainer.setVisible(false);
        }
        catch (InterruptedException ex) {
            logger.fatal((Object)"popupContainer error:", (Throwable)ex);
        }
    }

    private void hidePickDialogs() {
        for (ShowCardsDialog showCardsDialog : this.pickTarget) {
            showCardsDialog.setVisible(false);
        }
        for (PickPileDialog pickPileDialog : this.pickPile) {
            pickPileDialog.setVisible(false);
        }
    }

    private void clearPickDialogs() {
        this.clearPickTargetDialogs();
        this.clearPickPileDialogs();
    }

    private void clearPickTargetDialogs() {
        for (ShowCardsDialog dialog : this.pickTarget) {
            dialog.cleanUp();
            dialog.removeDialog();
        }
        this.pickTarget.clear();
    }

    private void clearPickPileDialogs() {
        for (PickPileDialog dialog : this.pickPile) {
            dialog.cleanUp();
            dialog.removeDialog();
        }
        this.pickPile.clear();
    }

    public void changeGUISize() {
        this.initComponents = true;
        this.setGUISize(true);
        this.stackObjects.changeGUISize();
        this.feedbackPanel.changeGUISize();
        this.handContainer.changeGUISize();
        for (PlayAreaPanel playAreaPanel : this.players.values()) {
            playAreaPanel.changeGUISize();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.exiles.values()) {
            cardInfoWindowDialog.changeGUISize();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.revealed.values()) {
            cardInfoWindowDialog.changeGUISize();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.lookedAt.values()) {
            cardInfoWindowDialog.changeGUISize();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.companion.values()) {
            cardInfoWindowDialog.changeGUISize();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.graveyardWindows.values()) {
            cardInfoWindowDialog.changeGUISize();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.sideboardWindows.values()) {
            cardInfoWindowDialog.changeGUISize();
        }
        for (CardHintsHelperDialog cardHintsHelperDialog : this.cardHintsWindows.values()) {
            cardHintsHelperDialog.changeGUISize();
        }
        for (ShowCardsDialog showCardsDialog : this.pickTarget) {
            showCardsDialog.changeGUISize();
        }
        for (PickPileDialog pickPileDialog : this.pickPile) {
            pickPileDialog.changeGUISize();
        }
        this.revalidate();
        this.repaint();
        this.initComponents = false;
    }

    private void setGUISize(boolean themeReload) {
        this.splitters.values().forEach(splitter -> splitter.splitPane.setDividerSize(GUISizeHelper.dividerBarSize));
        this.txtHoldPriority.setFont(new Font(GUISizeHelper.gameFeedbackPanelFont.getFontName(), 1, GUISizeHelper.gameFeedbackPanelFont.getSize()));
        GUISizeHelper.changePopupMenuFont(this.popupMenuTriggerOrder);
        int upperPanelsHeight = this.getSkipButtonsPanelDefaultHeight();
        int feedbackButtonRowHeight = Math.round((float)(GUISizeHelper.gameFeedbackPanelButtonHeight * 150) / 100.0f);
        int feedbackTextRowHeight = GUISizeHelper.gameFeedbackPanelMainMessageFontSize + GUISizeHelper.gameFeedbackPanelExtraMessageFontSize + 30;
        int feedbackRowHeight = Math.max(feedbackButtonRowHeight, feedbackTextRowHeight);
        int feedbackPanelHeight = Math.max(upperPanelsHeight, feedbackRowHeight * 2);
        this.feedbackPanel.setPreferredSize(new Dimension(Short.MAX_VALUE, feedbackPanelHeight));
        this.feedbackPanel.setMinimumSize(new Dimension(0, feedbackPanelHeight));
        this.feedbackPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, feedbackPanelHeight));
        this.pnlShortCuts.setPreferredSize(new Dimension(Short.MAX_VALUE, upperPanelsHeight));
        this.pnlShortCuts.setMaximumSize(new Dimension(Short.MAX_VALUE, upperPanelsHeight));
        this.stackObjects.setCardDimension(this.getFloatingStackCardDimension());
        this.stackObjects.changeGUISize();
        this.userChatPanel.changeGUISize(GUISizeHelper.chatFont);
        this.gameChatPanel.changeGUISize(GUISizeHelper.chatFont);
        float guiScale = GUISizeHelper.dialogGuiScale;
        int hGap = GUISizeHelper.guiSizeScale(3, guiScale);
        int vGap = GUISizeHelper.guiSizeScale(3, guiScale);
        this.pnlShortCuts.setLayout(new FlowLayout(1, hGap, vGap));
        Dimension strictSize = new Dimension(2 * GUISizeHelper.gameCommandButtonHeight, GUISizeHelper.gameCommandButtonHeight);
        this.setSkipButtonSize(this.btnCancelSkip, guiScale, strictSize);
        this.setSkipButtonSize(this.btnSkipToNextTurn, guiScale, strictSize);
        this.setSkipButtonSize(this.btnSkipToEndTurn, guiScale, strictSize);
        this.setSkipButtonSize(this.btnSkipToEndStepBeforeYourTurn, guiScale, strictSize);
        this.setSkipButtonSize(this.btnSkipToYourTurn, guiScale, strictSize);
        this.setSkipButtonSize(this.btnSkipToNextMain, guiScale, strictSize);
        this.setSkipButtonSize(this.btnSkipStack, guiScale, strictSize);
        this.setSkipButtonSize(this.btnConcede, guiScale, strictSize);
        this.setSkipButtonSize(this.btnToggleMacro, guiScale, strictSize);
        this.setSkipButtonSize(this.btnSwitchHands, guiScale, strictSize);
        this.setSkipButtonSize(this.btnStopWatching, guiScale, strictSize);
        this.pnlShortCuts.invalidate();
        int buttonSize = GUISizeHelper.gamePhaseButtonSize;
        guiScale = GUISizeHelper.dialogGuiScale;
        hGap = GUISizeHelper.guiSizeScale(3, guiScale);
        vGap = GUISizeHelper.guiSizeScale(3, guiScale);
        BoxLayout layout = new BoxLayout(this.jPhases, 1);
        this.jPhases.setLayout(layout);
        int fullPhaseWidth = Math.round(1.5f * (float)GUISizeHelper.gamePhaseButtonSize);
        this.jPhases.setPreferredSize(new Dimension(fullPhaseWidth, vGap * this.phaseButtons.size() + buttonSize * this.phaseButtons.size()));
        this.jPhases.setMaximumSize(new Dimension(fullPhaseWidth, Short.MAX_VALUE));
        this.phaseButtons.forEach((phaseName, phaseButton) -> phaseButton.setPreferredSize(new Dimension(buttonSize, buttonSize)));
        if (this.lastGameData.game != null) {
            this.updateActivePhase(this.lastGameData.game.getStep());
        }
        if (themeReload) {
            this.reloadThemeRelatedGraphic();
        }
    }

    private int getSkipButtonsPanelDefaultHeight() {
        float guiScale = GUISizeHelper.dialogGuiScale;
        int hGap = GUISizeHelper.guiSizeScale(3, guiScale);
        int vGap = GUISizeHelper.guiSizeScale(3, guiScale);
        int availableWidth = this.pnlShortCuts.getWidth();
        if (availableWidth <= 0) {
            availableWidth = this.pnlHelperHandButtonsStackArea.getWidth();
        }
        int requiredWidth = 0;
        int visibleCount = 0;
        for (Component component : this.pnlShortCuts.getComponents()) {
            if (!component.isVisible()) continue;
            requiredWidth += component.getPreferredSize().width;
            ++visibleCount;
        }
        if (visibleCount > 1) {
            requiredWidth += (visibleCount - 1) * hGap;
        }
        boolean fitsOneRow = availableWidth <= 0 || requiredWidth <= availableWidth;
        int lines = fitsOneRow ? 1 : 2;
        return (lines * 2 - 1) * vGap + lines * GUISizeHelper.gameCommandButtonHeight;
    }

    private void reloadThemeRelatedGraphic() {
        int buttonHeight = GUISizeHelper.gameCommandButtonHeight;
        this.setSkipButtonImage(this.btnCancelSkip, ImageManagerImpl.instance.getCancelSkipButtonImage(buttonHeight));
        this.setSkipButtonImage(this.btnSkipToNextTurn, ImageManagerImpl.instance.getSkipNextTurnButtonImage(buttonHeight));
        this.setSkipButtonImage(this.btnSkipToEndTurn, ImageManagerImpl.instance.getSkipEndTurnButtonImage(buttonHeight));
        this.setSkipButtonImage(this.btnSkipToEndStepBeforeYourTurn, ImageManagerImpl.instance.getSkipEndStepBeforeYourTurnButtonImage(buttonHeight));
        this.setSkipButtonImage(this.btnSkipToYourTurn, ImageManagerImpl.instance.getSkipYourNextTurnButtonImage(buttonHeight));
        this.setSkipButtonImage(this.btnSkipToNextMain, ImageManagerImpl.instance.getSkipMainButtonImage(buttonHeight));
        this.setSkipButtonImage(this.btnSkipStack, ImageManagerImpl.instance.getSkipStackButtonImage(buttonHeight));
        this.setSkipButtonImage(this.btnConcede, ImageManagerImpl.instance.getConcedeButtonImage(buttonHeight));
        this.setSkipButtonImage(this.btnToggleMacro, ImageManagerImpl.instance.getToggleRecordMacroButtonImage(buttonHeight));
        this.setSkipButtonImage(this.btnSwitchHands, ImageManagerImpl.instance.getSwitchHandsButtonImage(buttonHeight));
        this.setSkipButtonImage(this.btnStopWatching, ImageManagerImpl.instance.getStopWatchButtonImage(buttonHeight));
        boolean displayButtonText = PreferencesDialog.getCurrentTheme().isShortcutsVisibleForSkipButtons();
        this.btnCancelSkip.setShowKey(displayButtonText);
        this.btnSkipToNextTurn.setShowKey(displayButtonText);
        this.btnSkipToEndTurn.setShowKey(displayButtonText);
        this.btnSkipToEndStepBeforeYourTurn.setShowKey(displayButtonText);
        this.btnSkipToYourTurn.setShowKey(displayButtonText);
        this.btnSkipToNextMain.setShowKey(displayButtonText);
        this.btnSkipStack.setShowKey(displayButtonText);
        this.btnToggleMacro.setShowKey(displayButtonText);
        this.phaseButtons.forEach((phaseName, phaseButton) -> {
            Image buttonImage = ImageManagerImpl.instance.getPhaseImage((String)phaseName, GUISizeHelper.gamePhaseButtonSize);
            Rectangle buttonRect = new Rectangle(buttonImage.getWidth(null), buttonImage.getHeight(null));
            phaseButton.update(phaseButton.getText(), buttonImage, buttonImage, buttonImage, buttonImage, buttonRect);
        });
        if (this.lastGameData.game != null) {
            this.lastGameData.game.getPlayers().forEach(player -> {
                PlayAreaPanel playPanel = this.players.getOrDefault(player.getPlayerId(), null);
                if (playPanel != null) {
                    playPanel.getPlayerPanel().fullRefresh(GUISizeHelper.playerPanelGuiScale);
                    playPanel.init((PlayerView)player, this.bigCard, this.gameId, player.getPriorityTimeLeftSecs());
                    playPanel.update(this.lastGameData.game, (PlayerView)player, this.lastGameData.targets, this.lastGameData.getChosenTargets());
                    playPanel.getPlayerPanel().sizePlayerPanel(false);
                }
            });
        }
        if (this.abilityPicker != null && !this.abilityPicker.isVisible()) {
            this.abilityPicker.fullRefresh(GUISizeHelper.dialogGuiScale);
            this.abilityPicker.init(this.gameId, this.bigCard);
        }
        if (this.pickMultiNumber != null && !this.pickMultiNumber.isVisible()) {
            this.pickMultiNumber.init(this.gameId, this.bigCard);
        }
    }

    private void setSkipButtonImage(JButton button, Image image) {
        button.setIcon(new ImageIcon(image));
    }

    private void setSkipButtonSize(JComponent button, float guiScale, Dimension size) {
        if (button instanceof KeyboundButton) {
            ((KeyboundButton)button).updateGuiScale(guiScale);
        }
    }

    private Map<String, Integer> loadSplitterLocationsFromSettings(String settingsKey) {
        HashMap res;
        Type type = new TypeToken<Map<String, Integer>>(){}.getType();
        try {
            String savedData = PreferencesDialog.getCachedValue(settingsKey, "");
            res = (HashMap)new Gson().fromJson(savedData, type);
        }
        catch (Exception e) {
            res = null;
            logger.error((Object)("Found broken data for divider locations " + settingsKey), (Throwable)e);
        }
        if (res == null) {
            res = new HashMap();
        }
        return res;
    }

    private void saveSplitterLocationsToSettings(Map<String, Integer> newValues, String settingsKey) {
        PreferencesDialog.saveValue(settingsKey, new Gson().toJson(newValues));
    }

    private void saveSplitters() {
        if (!this.isSplittersFullyRestored) {
            logger.warn((Object)"splitters do not fully restored yet");
            return;
        }
        this.splitters.forEach((settingsKey, splitter) -> this.saveSplitter(splitter.splitPane, (String)settingsKey));
    }

    private void saveSplitter(JSplitPane splitPane, String settingsKey) {
        Map<String, Integer> allLocations = this.loadSplitterLocationsFromSettings(settingsKey);
        Rectangle screenRec = MageFrame.getDesktop().getBounds();
        String screenKey = String.format("%d_x_%d", screenRec.width, screenRec.height);
        int newLocation = splitPane.getDividerLocation();
        if (newLocation == 0 || newLocation < splitPane.getMinimumDividerLocation()) {
            newLocation = -2;
        } else if (newLocation > splitPane.getMaximumDividerLocation()) {
            newLocation = -3;
        }
        allLocations.put(screenKey, newLocation);
        this.saveSplitterLocationsToSettings(allLocations, settingsKey);
    }

    private void restoreSplitter(JSplitPane splitPane, String settingsKey, double defaultProportion) {
        Map<String, Integer> allLocations = this.loadSplitterLocationsFromSettings(settingsKey);
        Rectangle screenRec = MageFrame.getDesktop().getBounds();
        String screenKey = String.format("%d_x_%d", screenRec.width, screenRec.height);
        int newLocation = allLocations.getOrDefault(screenKey, -1);
        if (newLocation == -1) {
            SwingUtilities.invokeLater(() -> {
                splitPane.resetToPreferredSizes();
                splitPane.setDividerLocation(defaultProportion);
            });
        } else if (newLocation == -2) {
            SwingUtilities.invokeLater(() -> {
                splitPane.resetToPreferredSizes();
                splitPane.setDividerLocation(defaultProportion);
                splitPane.getLeftComponent().setMinimumSize(new Dimension());
                splitPane.setDividerLocation(0.0);
            });
        } else if (newLocation == -3) {
            SwingUtilities.invokeLater(() -> {
                splitPane.resetToPreferredSizes();
                splitPane.setDividerLocation(defaultProportion);
                splitPane.getRightComponent().setMinimumSize(new Dimension());
                splitPane.setDividerLocation(1.0);
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                splitPane.resetToPreferredSizes();
                splitPane.setDividerLocation(defaultProportion);
                splitPane.setDividerLocation(newLocation);
            });
        }
    }

    private void restoreSplitters() {
        this.isSplittersFullyRestored = false;
        SwingUtilities.invokeLater(() -> this.restoreSplittersByQueue(new LinkedHashMap<String, MageSplitter>(this.splitters)));
    }

    private void restoreSplittersByQueue(Map<String, MageSplitter> splittersQueue) {
        if (splittersQueue.isEmpty()) {
            this.isSplittersFullyRestored = true;
            return;
        }
        String currentKey = (String)splittersQueue.keySet().stream().findFirst().get();
        MageSplitter currentSplitter = splittersQueue.remove(currentKey);
        this.restoreSplitter(currentSplitter.splitPane, currentKey, currentSplitter.defaultProportion);
        SwingUtilities.invokeLater(() -> this.restoreSplittersByQueue(splittersQueue));
    }

    private void sizeBigCard() {
        int width = this.bigCard.getParent().getWidth();
        int height = Math.round((float)width * 1.426282f);
        this.bigCard.setPreferredSize(new Dimension(width, height));
        this.bigCard.setMaximumSize(new Dimension(Short.MAX_VALUE, height));
    }

    private void sizeToScreen() {
        Rectangle rect = this.getBounds();
        this.pnlShortCuts.revalidate();
        for (PlayAreaPanel p : this.players.values()) {
            p.getPlayerPanel().sizePlayerPanel(false);
        }
        ArrowBuilder.getBuilder().setSize(rect.width, rect.height);
        DialogManager.getManager(this.gameId).setScreenWidth(rect.width);
        DialogManager.getManager(this.gameId).setScreenHeight(rect.height);
        DialogManager.getManager(this.gameId).setBounds(0, 0, rect.width, rect.height);
    }

    public synchronized void showGame(UUID currentTableId, UUID parentTableId, UUID gameId, UUID playerId, GamePane gamePane) {
        this.currentTableId = currentTableId;
        this.parentTableId = parentTableId;
        this.gameId = gameId;
        this.gamePane = gamePane;
        this.playerId = playerId;
        MageFrame.addGame(gameId, this);
        this.feedbackPanel.init(gameId, this.bigCard);
        this.feedbackPanel.clear();
        this.pickMultiNumber.init(gameId, this.bigCard);
        this.abilityPicker.init(gameId, this.bigCard);
        this.btnConcede.setVisible(true);
        this.btnStopWatching.setVisible(false);
        this.btnSwitchHands.setVisible(false);
        this.btnCancelSkip.setVisible(true);
        this.btnToggleMacro.setVisible(true);
        this.gameChatPanel.setGameData(gameId, this.bigCard);
        this.userChatPanel.setGameData(gameId, this.bigCard);
        this.btnSkipToNextTurn.setVisible(true);
        this.btnSkipToEndTurn.setVisible(true);
        this.btnSkipToNextMain.setVisible(true);
        this.btnSkipStack.setVisible(true);
        this.btnSkipToYourTurn.setVisible(true);
        this.btnSkipToEndStepBeforeYourTurn.setVisible(true);
        this.pnlReplay.setVisible(false);
        this.gameChatPanel.clear();
        SessionHandler.getGameChatId(gameId).ifPresent(uuid -> this.gameChatPanel.connect((UUID)uuid));
        if (!SessionHandler.joinGame(gameId)) {
            this.removeGame();
        } else {
            AudioManager.playYourGameStarted();
            if (!AppUtil.isAppActive()) {
                MageTray.instance.displayMessage("Your game has started!");
                MageTray.instance.blink();
            }
        }
    }

    public synchronized void watchGame(UUID currentTableId, UUID parentTableId, UUID gameId, GamePane gamePane) {
        this.currentTableId = currentTableId;
        this.parentTableId = parentTableId;
        this.gameId = gameId;
        this.gamePane = gamePane;
        this.playerId = null;
        MageFrame.addGame(gameId, this);
        this.feedbackPanel.init(gameId, this.bigCard);
        this.feedbackPanel.clear();
        this.btnConcede.setVisible(false);
        this.btnStopWatching.setVisible(true);
        this.btnSwitchHands.setVisible(false);
        this.chosenHandKey = "";
        this.btnCancelSkip.setVisible(false);
        this.btnToggleMacro.setVisible(false);
        this.btnSkipToNextTurn.setVisible(false);
        this.btnSkipToEndTurn.setVisible(false);
        this.btnSkipToNextMain.setVisible(false);
        this.btnSkipStack.setVisible(false);
        this.btnSkipToYourTurn.setVisible(false);
        this.btnSkipToEndStepBeforeYourTurn.setVisible(false);
        this.pnlReplay.setVisible(false);
        this.gameChatPanel.clear();
        SessionHandler.getGameChatId(gameId).ifPresent(uuid -> this.gameChatPanel.connect((UUID)uuid));
        if (!SessionHandler.watchGame(gameId)) {
            this.removeGame();
        }
        for (PlayAreaPanel panel : this.players.values()) {
            panel.setPlayingMode(false);
        }
    }

    public synchronized void replayGame(UUID gameId) {
        this.currentTableId = null;
        this.parentTableId = null;
        this.gameId = gameId;
        this.playerId = null;
        MageFrame.addGame(gameId, this);
        this.feedbackPanel.init(gameId, this.bigCard);
        this.feedbackPanel.clear();
        this.btnConcede.setVisible(false);
        this.btnSkipToNextTurn.setVisible(false);
        this.btnSwitchHands.setVisible(false);
        this.btnStopWatching.setVisible(false);
        this.pnlReplay.setVisible(true);
        this.gameChatPanel.clear();
        if (!SessionHandler.startReplay(gameId)) {
            this.removeGame();
        }
        for (PlayAreaPanel panel : this.players.values()) {
            panel.setPlayingMode(false);
        }
    }

    public void removeGame() {
        Container c;
        for (c = this.getParent(); c != null && !(c instanceof GamePane); c = c.getParent()) {
        }
        if (c != null) {
            ((GamePane)c).removeGame();
        }
    }

    public synchronized void init(int messageId, GameView game, boolean callGameUpdateAfterInit) {
        this.addPlayers(game);
        this.setMenuStates(PreferencesDialog.getCachedValue("gameManaAutopayment", "true").equals("true"), PreferencesDialog.getCachedValue("gameManaAutopaymentOnlyOne", "true").equals("true"), PreferencesDialog.getCachedValue("useFirstManaAbility", "false").equals("true"), this.holdingPriority);
        if (callGameUpdateAfterInit) {
            this.updateGame(messageId, game);
        }
    }

    private void addPlayers(GameView game) {
        int playerNum;
        this.players.clear();
        this.playersWhoLeft.clear();
        this.pnlBattlefield.removeAll();
        int numSeats = game.getPlayers().size();
        int numColumns = (numSeats + 1) / 2;
        boolean oddNumber = numColumns > 1 && numSeats % 2 == 1;
        int col = 0;
        boolean row = true;
        int playerSeat = 0;
        if (this.playerId != null) {
            PlayerView player;
            Iterator iterator = game.getPlayers().iterator();
            while (iterator.hasNext() && !this.playerId.equals((player = (PlayerView)iterator.next()).getPlayerId())) {
                ++playerSeat;
            }
        }
        PlayerView player = (PlayerView)game.getPlayers().get(playerSeat);
        PlayAreaPanel playAreaPanel = new PlayAreaPanel(player, this.bigCard, this.gameId, game.getPriorityTime(), this, new PlayAreaPanelOptions(game.isPlayer(), player.isHuman(), game.isPlayer(), game.isRollbackTurnsAllowed(), !row));
        this.players.put(player.getPlayerId(), playAreaPanel);
        this.playersWhoLeft.put(player.getPlayerId(), false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = 1;
        c.weightx = 0.5;
        c.weighty = 0.5;
        if (oddNumber) {
            c.gridwidth = 2;
        }
        c.gridx = col++;
        c.gridy = 0;
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        topPanel.setLayout(new GridBagLayout());
        bottomPanel.setLayout(new GridBagLayout());
        bottomPanel.add((Component)playAreaPanel, c);
        playAreaPanel.setVisible(true);
        if (oddNumber) {
            // empty if block
        }
        if ((playerNum = playerSeat + 1) >= numSeats) {
            playerNum = 0;
        }
        do {
            col = row ? ++col : --col;
            if (col >= numColumns) {
                row = false;
                col = numColumns - 1;
            }
            player = (PlayerView)game.getPlayers().get(playerNum);
            PlayAreaPanel playerPanel = new PlayAreaPanel(player, this.bigCard, this.gameId, game.getPriorityTime(), this, new PlayAreaPanelOptions(game.isPlayer(), player.isHuman(), false, game.isRollbackTurnsAllowed(), !row));
            this.players.put(player.getPlayerId(), playerPanel);
            this.playersWhoLeft.put(player.getPlayerId(), false);
            c = new GridBagConstraints();
            c.fill = 1;
            c.weightx = 0.5;
            c.weighty = 0.5;
            c.gridx = col;
            c.gridy = 0;
            if (!row) {
                topPanel.add((Component)playerPanel, c);
            } else {
                bottomPanel.add((Component)playerPanel, c);
            }
            playerPanel.setVisible(true);
            if (++playerNum < numSeats) continue;
            playerNum = 0;
        } while (playerNum != playerSeat);
        for (PlayAreaPanel p : this.players.values()) {
            p.getPlayerPanel().sizePlayerPanel(false);
        }
        int sharedBattlefieldMinHeight = Math.max(topPanel.getPreferredSize().height, bottomPanel.getPreferredSize().height);
        topPanel.setMinimumSize(new Dimension(0, sharedBattlefieldMinHeight));
        bottomPanel.setMinimumSize(new Dimension(0, sharedBattlefieldMinHeight));
        GridBagConstraints panelC = new GridBagConstraints();
        panelC.fill = 1;
        panelC.weightx = 0.5;
        panelC.weighty = 0.5;
        panelC.gridwidth = 1;
        panelC.gridy = 0;
        this.pnlBattlefield.add((Component)topPanel, panelC);
        panelC.gridy = 1;
        this.pnlBattlefield.add((Component)bottomPanel, panelC);
    }

    public synchronized void updateGame(int messageId, GameView game) {
        this.updateGame(messageId, game, false, null, null);
    }

    public synchronized void updateGame(int messageId, GameView game, boolean showPlayable, Map<String, Serializable> options, Set<UUID> targets) {
        this.keepLastGameData(messageId, game, showPlayable, options, targets);
        if (this.players.isEmpty() && !game.getPlayers().isEmpty()) {
            logger.warn((Object)"Found empty players list, trying to init game again (possible reason: reconnection)");
            this.init(messageId, game, false);
        }
        this.prepareSelectableView();
        this.updateGame();
    }

    public synchronized void updateGame() {
        if (this.playerId == null && this.lastGameData.game.getWatchedHands().isEmpty()) {
            this.handContainer.setVisible(false);
        } else {
            this.handContainer.setVisible(true);
            this.handCards.clear();
            if (!this.lastGameData.game.getWatchedHands().isEmpty()) {
                for (Map.Entry hand : this.lastGameData.game.getWatchedHands().entrySet()) {
                    this.handCards.put((String)hand.getKey(), CardsViewUtil.convertSimple((SimpleCardsView)hand.getValue(), this.loadedCards));
                }
            }
            if (this.playerId != null) {
                this.handCards.put(YOUR_HAND, this.lastGameData.game.getMyHand());
                if (!this.lastGameData.game.getOpponentHands().isEmpty()) {
                    for (Map.Entry hand : this.lastGameData.game.getOpponentHands().entrySet()) {
                        this.handCards.put((String)hand.getKey(), CardsViewUtil.convertSimple((SimpleCardsView)hand.getValue(), this.loadedCards));
                    }
                }
                if (!this.handCards.containsKey(this.chosenHandKey)) {
                    this.chosenHandKey = YOUR_HAND;
                }
            } else if (this.chosenHandKey.isEmpty() && !this.handCards.isEmpty()) {
                this.chosenHandKey = this.handCards.keySet().iterator().next();
            }
            if (this.chosenHandKey != null && this.handCards.containsKey(this.chosenHandKey)) {
                this.handContainer.loadCards(this.handCards.get(this.chosenHandKey), this.bigCard, this.gameId);
            }
            this.hideAll();
            if (this.playerId != null) {
                boolean change;
                this.btnSwitchHands.setVisible(this.handCards.size() > 1);
                boolean bl = change = this.handCardsOfOpponentAvailable == this.lastGameData.game.getOpponentHands().isEmpty();
                if (change) {
                    boolean bl2 = this.handCardsOfOpponentAvailable = !this.handCardsOfOpponentAvailable;
                    if (this.handCardsOfOpponentAvailable) {
                        MageFrame.getInstance().showMessage("You control other player's turn. \nUse \"Switch Hand\" button to switch between cards in different hands.");
                    } else {
                        MageFrame.getInstance().showMessage("You lost control on other player's turn.");
                    }
                }
            } else {
                this.btnSwitchHands.setVisible(!this.handCards.isEmpty());
            }
        }
        if (this.lastGameData.game.getPhase() != null) {
            this.txtPhase.setText(this.lastGameData.game.getPhase().toString());
        } else {
            this.txtPhase.setText("");
        }
        if (this.lastGameData.game.getStep() != null) {
            this.updateActivePhase(this.lastGameData.game.getStep());
            this.txtStep.setText(this.lastGameData.game.getStep().toString());
        } else {
            logger.debug((Object)"Step is empty");
            this.txtStep.setText("");
        }
        this.txtActivePlayer.setText(this.lastGameData.game.getActivePlayerName());
        this.txtPriority.setText(this.lastGameData.game.getPriorityPlayerName());
        this.txtTurn.setText(Integer.toString(this.lastGameData.game.getTurn()));
        List<UUID> possibleAttackers = new ArrayList<>();
        if (this.lastGameData.options != null && this.lastGameData.options.containsKey("possibleAttackers") && this.lastGameData.options.get("possibleAttackers") instanceof List) {
            possibleAttackers.addAll((List<UUID>) this.lastGameData.options.get("possibleAttackers"));
        }
        List<UUID> possibleBlockers = new ArrayList<>();
        if (this.lastGameData.options != null && this.lastGameData.options.containsKey("possibleBlockers") && this.lastGameData.options.get("possibleBlockers") instanceof List) {
            possibleBlockers.addAll((List<UUID>) this.lastGameData.options.get("possibleBlockers"));
        }
        for (PlayerView player : this.lastGameData.game.getPlayers()) {
            if (this.players.containsKey(player.getPlayerId())) {
                CardInfoWindowDialog windowDialog2;
                if (!possibleAttackers.isEmpty()) {
                    for (UUID permanentId : possibleAttackers) {
                        if (!player.getBattlefield().containsKey(permanentId)) continue;
                        ((PermanentView)player.getBattlefield().get(permanentId)).setCanAttack(true);
                    }
                }
                if (!possibleBlockers.isEmpty()) {
                    for (UUID permanentId : possibleBlockers) {
                        if (!player.getBattlefield().containsKey(permanentId)) continue;
                        ((PermanentView)player.getBattlefield().get(permanentId)).setCanBlock(true);
                    }
                }
                this.players.get(player.getPlayerId()).update(this.lastGameData.game, player, this.lastGameData.targets, this.lastGameData.getChosenTargets());
                if (player.getPlayerId().equals(this.playerId)) {
                    this.skipButtons.updateFromPlayer(player);
                }
                this.graveyards.put(player.getName(), player.getGraveyard());
                if (this.graveyardWindows.containsKey(player.getName())) {
                    windowDialog2 = this.graveyardWindows.get(player.getName());
                    if (windowDialog2.isClosed()) {
                        this.graveyardWindows.remove(player.getName());
                    } else {
                        windowDialog2.loadCardsAndShow(player.getGraveyard(), this.bigCard, this.gameId, false);
                    }
                }
                this.sideboards.put(player.getName(), player.getSideboard());
                if (this.sideboardWindows.containsKey(player.getName())) {
                    windowDialog2 = this.sideboardWindows.get(player.getName());
                    if (windowDialog2.isClosed()) {
                        this.sideboardWindows.remove(player.getName());
                    } else {
                        windowDialog2.loadCardsAndShow(player.getSideboard(), this.bigCard, this.gameId, false);
                    }
                }
                if (player.getTopCard() == null) continue;
                CardsView cardsView = new CardsView();
                cardsView.put(player.getTopCard().getId(), player.getTopCard());
                this.handleGameInfoWindow(this.revealed, CardInfoWindowDialog.ShowType.REVEAL_TOP_LIBRARY, player.getName() + "'s top library card", (LinkedHashMap) cardsView);
                continue;
            }
            if (this.players.isEmpty()) continue;
            logger.warn((Object)"Couldn't find player.");
            logger.warn((Object)("   uuid:" + player.getPlayerId()));
            logger.warn((Object)"   players:");
            for (PlayAreaPanel p : this.players.values()) {
                logger.warn((Object)String.valueOf(p));
            }
        }
        this.updateSkipButtons();
        if (!this.menuNameSet) {
            StringBuilder sb = new StringBuilder();
            if (this.playerId == null) {
                sb.append("Watching: ");
            } else {
                sb.append("Playing: ");
            }
            boolean first = true;
            for (PlayerView player : this.lastGameData.game.getPlayers()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(" - ");
                }
                sb.append(player.getName());
            }
            this.menuNameSet = true;
            this.gamePane.setTitle(sb.toString());
        }
        this.displayStack(this.lastGameData.game, this.bigCard, this.feedbackPanel, this.gameId);
        for (ExileView exile : this.lastGameData.game.getExile()) {
            CardInfoWindowDialog exileWindow = this.exiles.getOrDefault(exile.getId(), null);
            if (exileWindow == null) {
                exileWindow = new CardInfoWindowDialog(CardInfoWindowDialog.ShowType.EXILE, exile.getName());
                this.exiles.put(exile.getId(), exileWindow);
                MageFrame.getDesktop().add((Component)exileWindow, exileWindow.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
                exileWindow.show();
            }
            exileWindow.loadCardsAndShow(exile, this.bigCard, this.gameId);
        }
        this.clearClosedCardHintsWindows();
        this.cardHintsWindows.forEach((s, windowDialog) -> windowDialog.loadHints(this.lastGameData.game));
        this.showRevealed(this.lastGameData.game);
        this.showLookedAt(this.lastGameData.game);
        this.showCompanion(this.lastGameData.game);
        if (!this.lastGameData.game.getCombat().isEmpty()) {
            CombatManager.instance.showCombat(this.lastGameData.game.getCombat(), this.gameId);
        } else {
            CombatManager.instance.hideCombat(this.gameId);
        }
        for (PlayerView player : this.lastGameData.game.getPlayers()) {
            if (!player.hasLeft() || this.playersWhoLeft.get(player.getPlayerId()).booleanValue()) continue;
            PlayAreaPanel playerLeftPanel = this.players.get(player.getPlayerId());
            this.playersWhoLeft.put(player.getPlayerId(), true);
            Container parent = playerLeftPanel.getParent();
            GridBagLayout layout = (GridBagLayout)parent.getLayout();
            for (Component otherPanel : parent.getComponents()) {
                if (!(otherPanel instanceof PlayAreaPanel)) continue;
                GridBagConstraints gbc = layout.getConstraints(otherPanel);
                if (gbc.weightx > 0.1) {
                    gbc.weightx = 0.99;
                }
                gbc.fill = 1;
                gbc.anchor = 17;
                if (gbc.gridx > 0) {
                    gbc.anchor = 13;
                }
                if (otherPanel == playerLeftPanel) {
                    gbc.weightx = 0.01;
                    Dimension d = playerLeftPanel.getPreferredSize();
                    d.width = 95;
                    otherPanel.setPreferredSize(d);
                }
                parent.remove(otherPanel);
                parent.add(otherPanel, gbc);
            }
            parent.validate();
            parent.repaint();
        }
        this.feedbackPanel.disableUndo();
        this.feedbackPanel.updateOptions(this.lastGameData.options);
        this.xcpRefreshBattlefields();
        this.revalidate();
        this.repaint();
    }

    private void updateSkipButtons() {
        this.btnSkipToNextTurn.setToolTipText(this.skipButtons.turn.getTooltip());
        this.btnSkipToEndTurn.setToolTipText(this.skipButtons.untilEndOfTurn.getTooltip());
        this.btnSkipToNextMain.setToolTipText(this.skipButtons.untilNextMain.getTooltip());
        this.btnSkipStack.setToolTipText(this.skipButtons.untilStackResolved.getTooltip());
        this.btnSkipToYourTurn.setToolTipText(this.skipButtons.allTurns.getTooltip());
        this.btnSkipToEndStepBeforeYourTurn.setToolTipText(this.skipButtons.untilUntilEndStepBeforeMyTurn.getTooltip());
        this.btnSkipToNextTurn.setBorder(this.skipButtons.turn.getBorder());
        this.btnSkipToEndTurn.setBorder(this.skipButtons.untilEndOfTurn.getBorder());
        this.btnSkipToNextMain.setBorder(this.skipButtons.untilNextMain.getBorder());
        this.btnSkipStack.setBorder(this.skipButtons.untilStackResolved.getBorder());
        this.btnSkipToYourTurn.setBorder(this.skipButtons.allTurns.getBorder());
        this.btnSkipToEndStepBeforeYourTurn.setBorder(this.skipButtons.untilUntilEndStepBeforeMyTurn.getBorder());
    }

    public void setMenuStates(boolean manaPoolAutomatic, boolean manaPoolAutomaticRestricted, boolean useFirstManaAbility, boolean holdPriority) {
        for (PlayAreaPanel playAreaPanel : this.players.values()) {
            playAreaPanel.setMenuStates(manaPoolAutomatic, manaPoolAutomaticRestricted, useFirstManaAbility, holdPriority);
        }
    }

    private Dimension getFloatingStackCardDimension() {
        Dimension base = GUISizeHelper.handCardDimension;
        double scale = 1.03;
        return new Dimension(Math.max(base.width, (int)Math.round((double)base.width * scale)), Math.max(base.height, (int)Math.round((double)base.height * scale)));
    }

    private Preferences getFloatingStackPreferences() {
        return Preferences.userNodeForPackage(GamePanel.class).node(XCP_STACK_PREF_NODE);
    }

    private void restoreFloatingStackBounds() {
        if (this.floatingStackFrame == null) {
            return;
        }
        JDesktopPane desktop = MageFrame.getDesktop();
        Dimension desktopSize = desktop == null ? new Dimension(1280, 720) : desktop.getSize();
        Dimension cardSize = this.getFloatingStackCardDimension();
        int defaultWidth = Math.max(270, cardSize.width + 70);
        int defaultHeight = Math.max(470, cardSize.height + 190);
        Preferences prefs = this.getFloatingStackPreferences();
        int width = Math.max(this.floatingStackFrame.getMinimumSize().width, prefs.getInt(XCP_STACK_W, defaultWidth));
        int height = Math.max(this.floatingStackFrame.getMinimumSize().height, prefs.getInt(XCP_STACK_H, defaultHeight));
        width = Math.min(width, Math.max(300, desktopSize.width - 20));
        height = Math.min(height, Math.max(360, desktopSize.height - 20));
        int defaultX = Math.max(10, desktopSize.width - width - 28);
        int defaultY = Math.max(10, desktopSize.height - height - GUISizeHelper.guiSizeScale(95, GUISizeHelper.dialogGuiScale));
        int x = prefs.getInt(XCP_STACK_X, defaultX);
        int y = prefs.getInt(XCP_STACK_Y, defaultY);
        x = Math.max(0, Math.min(x, Math.max(0, desktopSize.width - width)));
        y = Math.max(0, Math.min(y, Math.max(0, desktopSize.height - height)));
        this.floatingStackRestoringBounds = true;
        this.floatingStackFrame.setBounds(x, y, width, height);
        this.floatingStackRestoringBounds = false;
    }

    private void saveFloatingStackBounds() {
        if (this.floatingStackFrame == null || this.floatingStackRestoringBounds) {
            return;
        }
        Rectangle bounds = this.floatingStackFrame.getBounds();
        if (bounds.width < 1 || bounds.height < 1) {
            return;
        }
        Preferences prefs = this.getFloatingStackPreferences();
        prefs.putInt(XCP_STACK_X, bounds.x);
        prefs.putInt(XCP_STACK_Y, bounds.y);
        prefs.putInt(XCP_STACK_W, bounds.width);
        prefs.putInt(XCP_STACK_H, bounds.height);
    }

    private JPanel createFloatingStackHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(8, 12, 4, 12));
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        this.floatingStackTitleLabel = new JLabel("The Stack (0)");
        this.floatingStackTitleLabel.setFont(GUISizeHelper.dialogFont.deriveFont(1));
        this.floatingStackTitleLabel.setForeground(new Color(132, 181, 232));
        this.floatingStackTitleLabel.setHorizontalAlignment(0);
        this.floatingStackTitleLabel.setToolTipText("The object marked 1st resolves first");
        titleRow.add((Component)this.floatingStackTitleLabel, "Center");
        header.add((Component)titleRow, "North");
        this.floatingStackTypeLabel = new JLabel("");
        this.floatingStackTypeLabel.setOpaque(true);
        this.floatingStackTypeLabel.setForeground(new Color(250, 235, 185));
        this.floatingStackTypeLabel.setBackground(new Color(18, 22, 28, 238));
        this.floatingStackTypeLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(185, 151, 72, 235)), new EmptyBorder(2, 8, 2, 8)));
        this.floatingStackTypeLabel.setFont(GUISizeHelper.dialogFont.deriveFont(1));
        this.floatingStackTypeLabel.setVisible(false);
        JPanel typeRow = new JPanel(new FlowLayout(1, 0, 1));
        typeRow.setOpaque(false);
        typeRow.add(this.floatingStackTypeLabel);
        this.floatingStackOrderLabel = new JLabel("");
        this.floatingStackOrderLabel.setOpaque(true);
        this.floatingStackOrderLabel.setForeground(new Color(166, 224, 181));
        this.floatingStackOrderLabel.setBackground(new Color(24, 49, 34, 220));
        this.floatingStackOrderLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(78, 137, 95, 190)), new EmptyBorder(1, 5, 1, 5)));
        this.floatingStackOrderLabel.setFont(GUISizeHelper.dialogFont.deriveFont(0, Math.max(9.0f, GUISizeHelper.dialogFont.getSize2D() - 3.0f)));
        this.floatingStackOrderLabel.setHorizontalAlignment(0);
        this.floatingStackOrderLabel.setToolTipText("Next to resolve");
        this.floatingStackOrderLabel.setVisible(false);
        JPanel orderRow = new JPanel(new FlowLayout(1, 0, 1));
        orderRow.setOpaque(false);
        orderRow.add(this.floatingStackOrderLabel);
        JPanel statusRows = new JPanel();
        statusRows.setOpaque(false);
        statusRows.setLayout(new BoxLayout(statusRows, 1));
        statusRows.add(typeRow);
        statusRows.add(orderRow);
        header.add((Component)statusRows, "South");
        MouseAdapter dragAdapter = new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent e) {
                GamePanel.this.floatingStackDragOrigin = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (GamePanel.this.floatingStackFrame == null || GamePanel.this.floatingStackDragOrigin == null) {
                    return;
                }
                Point frameLocation = GamePanel.this.floatingStackFrame.getLocation();
                int nextX = frameLocation.x + e.getX() - ((GamePanel)GamePanel.this).floatingStackDragOrigin.x;
                int nextY = frameLocation.y + e.getY() - ((GamePanel)GamePanel.this).floatingStackDragOrigin.y;
                JDesktopPane desktop = MageFrame.getDesktop();
                if (desktop != null) {
                    nextX = Math.max(0, Math.min(nextX, Math.max(0, desktop.getWidth() - GamePanel.this.floatingStackFrame.getWidth())));
                    nextY = Math.max(0, Math.min(nextY, Math.max(0, desktop.getHeight() - GamePanel.this.floatingStackFrame.getHeight())));
                }
                GamePanel.this.floatingStackFrame.setLocation(nextX, nextY);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                GamePanel.this.saveFloatingStackBounds();
                GamePanel.this.floatingStackDragOrigin = null;
            }
        };
        header.addMouseListener(dragAdapter);
        header.addMouseMotionListener(dragAdapter);
        this.floatingStackTitleLabel.addMouseListener(dragAdapter);
        this.floatingStackTitleLabel.addMouseMotionListener(dragAdapter);
        this.floatingStackTypeLabel.addMouseListener(dragAdapter);
        this.floatingStackTypeLabel.addMouseMotionListener(dragAdapter);
        this.floatingStackOrderLabel.addMouseListener(dragAdapter);
        this.floatingStackOrderLabel.addMouseMotionListener(dragAdapter);
        return header;
    }

    private void initFloatingStackWindow() {
        if (this.floatingStackFrame != null) {
            return;
        }
        this.stackObjects.setVerticalStackLayout(true);
        this.stackObjects.setZone(Zone.STACK);
        this.stackObjects.addCardEventListener(event -> {
            if (event.getSource() instanceof CardView && (event.getEventType() == ClientEventType.CARD_CLICK || event.getEventType() == ClientEventType.CARD_DOUBLE_CLICK)) {
                CardView selected = (CardView)event.getSource();
                logger.info((Object)("Floating stack card click: game=" + this.gameId + " id=" + selected.getId() + " name=" + selected.getName() + " ability=" + selected.isAbility() + " event=" + (Object)((Object)event.getEventType())));
                DefaultActionCallback.instance.mouseClicked(this.gameId, selected);
            }
        });
        this.stackObjects.setCardDimension(this.getFloatingStackCardDimension());
        this.stackObjects.setBackgroundColor(new Color(22, 27, 33));
        this.stackObjects.setBorder(new EmptyBorder(6, 6, 6, 6));
        this.floatingStackFrame = new JInternalFrame("", true, false, false, false);
        this.floatingStackFrame.setDefaultCloseOperation(0);
        this.floatingStackFrame.setFrameIcon(null);
        this.floatingStackFrame.setOpaque(false);
        this.floatingStackFrame.setBackground(new Color(0, 0, 0, 0));
        this.floatingStackFrame.setBorder(new EmptyBorder(4, 4, 4, 4));
        if (this.floatingStackFrame.getUI() instanceof BasicInternalFrameUI) {
            ((BasicInternalFrameUI)this.floatingStackFrame.getUI()).setNorthPane(null);
        }
        JPanel content = new JPanel(new BorderLayout()){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int width = this.getWidth() - 1;
                    int height = this.getHeight() - 1;
                    int arc = Math.min(28, Math.max(18, Math.min(width, height) / 6));
                    g2.setColor(new Color(22, 27, 33, 250));
                    g2.fillRoundRect(0, 0, width, height, arc, arc);
                    g2.setColor(new Color(92, 101, 114, 220));
                    g2.drawRoundRect(0, 0, width, height, arc, arc);
                }
                finally {
                    g2.dispose();
                }
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                try {
                    int width = Math.max(1, this.getWidth() - 1);
                    int height = Math.max(1, this.getHeight() - 1);
                    int arc = Math.min(28, Math.max(18, Math.min(width, height) / 6));
                    g2.clip(new RoundRectangle2D.Double(0.0, 0.0, width, height, arc, arc));
                    super.paintChildren(g2);
                }
                finally {
                    g2.dispose();
                }
            }
        };
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.floatingStackHeader = this.createFloatingStackHeader();
        content.add((Component)this.floatingStackHeader, "North");
        content.add((Component)this.stackObjects, "Center");
        JPanel bottomResizeGrip = new JPanel(new BorderLayout());
        bottomResizeGrip.setOpaque(false);
        bottomResizeGrip.setPreferredSize(new Dimension(1, 12));
        bottomResizeGrip.setCursor(Cursor.getPredefinedCursor(9));
        bottomResizeGrip.setToolTipText(null);
        MouseAdapter bottomResizeAdapter = new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent e) {
                if (GamePanel.this.floatingStackFrame == null) {
                    return;
                }
                GamePanel.this.floatingStackResizeOrigin = e.getLocationOnScreen();
                GamePanel.this.floatingStackResizeStartSize = GamePanel.this.floatingStackFrame.getSize();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (GamePanel.this.floatingStackFrame == null || GamePanel.this.floatingStackResizeOrigin == null || GamePanel.this.floatingStackResizeStartSize == null) {
                    return;
                }
                Point now = e.getLocationOnScreen();
                int dy = now.y - ((GamePanel)GamePanel.this).floatingStackResizeOrigin.y;
                int minHeight = ((GamePanel)GamePanel.this).floatingStackFrame.getMinimumSize().height;
                int newHeight = Math.max(minHeight, ((GamePanel)GamePanel.this).floatingStackResizeStartSize.height + dy);
                JDesktopPane desktop = MageFrame.getDesktop();
                if (desktop != null) {
                    newHeight = Math.min(newHeight, Math.max(minHeight, desktop.getHeight() - GamePanel.this.floatingStackFrame.getY()));
                }
                GamePanel.this.floatingStackFrame.setSize(GamePanel.this.floatingStackFrame.getWidth(), newHeight);
                GamePanel.this.floatingStackFrame.revalidate();
                GamePanel.this.floatingStackFrame.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                GamePanel.this.saveFloatingStackBounds();
                GamePanel.this.floatingStackResizeOrigin = null;
                GamePanel.this.floatingStackResizeStartSize = null;
            }
        };
        bottomResizeGrip.addMouseListener(bottomResizeAdapter);
        bottomResizeGrip.addMouseMotionListener(bottomResizeAdapter);
        content.add((Component)bottomResizeGrip, "South");
        this.floatingStackFrame.setContentPane(content);
        Dimension cardSize = this.getFloatingStackCardDimension();
        this.floatingStackFrame.setMinimumSize(new Dimension(Math.max(235, cardSize.width + 50), Math.max(420, cardSize.height + 190)));
        JDesktopPane desktop = MageFrame.getDesktop();
        if (desktop != null) {
            desktop.add((Component)this.floatingStackFrame, JLayeredPane.PALETTE_LAYER);
        }
        this.restoreFloatingStackBounds();
        this.floatingStackFrame.addComponentListener(new ComponentAdapter(){

            @Override
            public void componentMoved(ComponentEvent e) {
                GamePanel.this.saveFloatingStackBounds();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                GamePanel.this.saveFloatingStackBounds();
            }
        });
        this.floatingStackFrame.setVisible(false);
    }

    private void disposeFloatingStackWindow() {
        if (this.floatingStackFrame == null) {
            return;
        }
        this.saveFloatingStackBounds();
        this.floatingStackFrame.setVisible(false);
        Container parent = this.floatingStackFrame.getParent();
        if (parent != null) {
            parent.remove(this.floatingStackFrame);
            parent.revalidate();
            parent.repaint();
        }
        this.floatingStackFrame.dispose();
        this.floatingStackFrame = null;
        this.floatingStackHeader = null;
        this.floatingStackTitleLabel = null;
        this.floatingStackTypeLabel = null;
        this.floatingStackOrderLabel = null;
        this.floatingStackDragOrigin = null;
        this.floatingStackResizeOrigin = null;
        this.floatingStackResizeStartSize = null;
        this.floatingStackHadObjects = false;
    }

    private String getFloatingStackTypeLabel(CardView card) {
        if (card == null) {
            return "";
        }
        AbilityType abilityType = card.getAbilityType();
        if (abilityType == AbilityType.SPELL || card.getMageObjectType() == MageObjectType.SPELL) {
            return "Spell";
        }
        if (!card.isAbility() && abilityType == null) {
            return "";
        }
        if (abilityType == null || abilityType.isTriggeredAbility()) {
            return "Triggered Ability";
        }
        if (abilityType.isActivatedAbility()) {
            return "Activated Ability";
        }
        if (abilityType == AbilityType.STATIC) {
            return "Static Ability";
        }
        if (abilityType == AbilityType.SPECIAL_ACTION) {
            return "Special Action";
        }
        return (Object)((Object)abilityType) + " Ability";
    }

    private String getFloatingStackOrderLabel(CardView card) {
        if (card == null) {
            return "";
        }
        String name = card.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "unnamed object";
        }
        String safeName = name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\\\"", "&quot;");
        return "<html><div style=\"width:220px;text-align:center\">NEXT \u00b7 " + safeName + "</div></html>";
    }

    private void updateFloatingStackVisibility(GameView game) {
        boolean hasObjects;
        int objectCount = game.getStack().size();
        CardView topStackObject = game.getStack().isEmpty() ? null : (CardView)game.getStack().values().iterator().next();
        String type = this.getFloatingStackTypeLabel(topStackObject);
        if (this.floatingStackFrame == null) {
            this.initFloatingStackWindow();
        }
        boolean bl = hasObjects = objectCount > 0;
        if (this.floatingStackFrame != null) {
            if (this.floatingStackTitleLabel != null) {
                this.floatingStackTitleLabel.setText("The Stack (" + objectCount + ")");
                this.floatingStackTitleLabel.setToolTipText("The object marked 1st resolves first (" + objectCount + " object" + (objectCount == 1 ? "" : "s") + ")");
            }
            if (this.floatingStackTypeLabel != null) {
                this.floatingStackTypeLabel.setText(type);
                this.floatingStackTypeLabel.setVisible(!type.isEmpty());
            }
            if (this.floatingStackOrderLabel != null) {
                this.floatingStackOrderLabel.setText(this.getFloatingStackOrderLabel(topStackObject));
                this.floatingStackOrderLabel.setVisible(hasObjects);
                this.floatingStackHeader.revalidate();
                this.floatingStackHeader.repaint();
                logger.info((Object)("Floating stack guide: text=" + this.floatingStackOrderLabel.getText() + " visible=" + this.floatingStackOrderLabel.isVisible()));
            }
            if (hasObjects && !this.floatingStackHadObjects) {
                this.floatingStackFrame.setVisible(true);
                this.floatingStackFrame.toFront();
            } else if (!hasObjects && this.floatingStackHadObjects) {
                this.saveFloatingStackBounds();
                this.floatingStackFrame.setVisible(false);
            }
        }
        this.floatingStackHadObjects = hasObjects;
    }

    private void displayStack(GameView game, BigCard bigCard, FeedbackPanel feedbackPanel, UUID gameId) {
        this.stackObjects.loadCards(game.getStack(), bigCard, gameId, false);
        String stackAudit = game.getStack().values().stream().map(card -> card.getName() + "[" + card.getId() + "]").collect(Collectors.joining(" -> "));
        logger.info((Object)("Floating stack update: count=" + game.getStack().size() + " resolutionOrder=" + stackAudit));
        this.updateFloatingStackVisibility(game);
    }

    private void updateActivePhase(PhaseStep currentStep) {
        if (currentStep == null) {
            return;
        }
        switch (currentStep) {
            case UNTAP: {
                this.updatePhaseButtons("Untap");
                break;
            }
            case UPKEEP: {
                this.updatePhaseButtons("Upkeep");
                break;
            }
            case DRAW: {
                this.updatePhaseButtons("Draw");
                break;
            }
            case PRECOMBAT_MAIN: {
                this.updatePhaseButtons("Main1");
                break;
            }
            case BEGIN_COMBAT: {
                this.updatePhaseButtons("Combat_Start");
                break;
            }
            case DECLARE_ATTACKERS: {
                this.updatePhaseButtons("Combat_Attack");
                break;
            }
            case DECLARE_BLOCKERS: {
                this.updatePhaseButtons("Combat_Block");
                break;
            }
            case FIRST_COMBAT_DAMAGE:
            case COMBAT_DAMAGE: {
                this.updatePhaseButtons("Combat_Damage");
                break;
            }
            case END_COMBAT: {
                this.updatePhaseButtons("Combat_End");
                break;
            }
            case POSTCOMBAT_MAIN: {
                this.updatePhaseButtons("Main2");
                break;
            }
            case END_TURN:
            case CLEANUP: {
                this.updatePhaseButtons("Cleanup");
                break;
            }
        }
    }

    private void updatePhaseButtons(String currentPhaseName) {
        this.phaseButtons.forEach((phaseName, phaseButton) -> {
            if (phaseName.equals(currentPhaseName)) {
                phaseButton.setAlignmentX(0.5f);
            } else {
                phaseButton.setAlignmentX(0.0f);
            }
        });
        this.jPhases.invalidate();
    }

    public void onDeactivated() {
        this.saveSplitters();
        for (CardInfoWindowDialog cardInfoWindowDialog : this.exiles.values()) {
            cardInfoWindowDialog.hideDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.revealed.values()) {
            cardInfoWindowDialog.hideDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.lookedAt.values()) {
            cardInfoWindowDialog.hideDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.graveyardWindows.values()) {
            cardInfoWindowDialog.hideDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.companion.values()) {
            cardInfoWindowDialog.hideDialog();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.sideboardWindows.values()) {
            cardInfoWindowDialog.hideDialog();
        }
        for (CardHintsHelperDialog cardHintsHelperDialog : this.cardHintsWindows.values()) {
            cardHintsHelperDialog.hideDialog();
        }
    }

    public void onActivated() {
        SwingUtilities.invokeLater(this::restoreSplitters);
        for (CardInfoWindowDialog cardInfoWindowDialog : this.exiles.values()) {
            cardInfoWindowDialog.show();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.revealed.values()) {
            cardInfoWindowDialog.show();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.lookedAt.values()) {
            cardInfoWindowDialog.show();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.graveyardWindows.values()) {
            cardInfoWindowDialog.show();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.companion.values()) {
            cardInfoWindowDialog.show();
        }
        for (CardInfoWindowDialog cardInfoWindowDialog : this.sideboardWindows.values()) {
            cardInfoWindowDialog.show();
        }
        for (CardHintsHelperDialog cardHintsHelperDialog : this.cardHintsWindows.values()) {
            cardHintsHelperDialog.show();
        }
    }

    public void openGraveyardWindow(String playerName) {
        if (this.graveyardWindows.containsKey(playerName)) {
            CardInfoWindowDialog cardInfoWindowDialog = this.graveyardWindows.get(playerName);
            if (cardInfoWindowDialog.isVisible()) {
                cardInfoWindowDialog.hideDialog();
            } else {
                cardInfoWindowDialog.show();
            }
            return;
        }
        CardInfoWindowDialog newGraveyard = new CardInfoWindowDialog(CardInfoWindowDialog.ShowType.GRAVEYARD, playerName);
        this.graveyardWindows.put(playerName, newGraveyard);
        MageFrame.getDesktop().add((Component)newGraveyard, newGraveyard.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        newGraveyard.loadCardsAndShow(this.graveyards.get(playerName), this.bigCard, this.gameId, false);
    }

    private void clearClosedCardHintsWindows() {
        this.cardHintsWindows.entrySet().removeIf(entry -> ((CardHintsHelperDialog)entry.getValue()).isClosed());
    }

    public void openCardHintsWindow(String code) {
        this.clearClosedCardHintsWindows();
        if (this.cardHintsWindows.size() >= 5) {
            this.cardHintsWindows.values().stream().reduce((a, b) -> b).ifPresent(CardHintsHelperDialog::show);
            return;
        }
        CardHintsHelperDialog newDialog = new CardHintsHelperDialog();
        newDialog.setSize(GUISizeHelper.dialogGuiScaleSize(newDialog.getSize()));
        newDialog.setGameData(this.lastGameData.game, this.gameId, this.bigCard);
        this.cardHintsWindows.put(code + UUID.randomUUID(), newDialog);
        MageFrame.getDesktop().add((Component)newDialog, newDialog.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        newDialog.loadHints(this.lastGameData.game);
    }

    public void openSideboardWindow(UUID playerId) {
        if (this.lastGameData == null) {
            return;
        }
        PlayerView playerView = this.lastGameData.game.getPlayers().stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst().orElse(null);
        if (playerView == null) {
            return;
        }
        if (this.sideboardWindows.containsKey(playerView.getName())) {
            CardInfoWindowDialog windowDialog = this.sideboardWindows.get(playerView.getName());
            if (windowDialog.isVisible()) {
                windowDialog.hideDialog();
            } else {
                windowDialog.show();
            }
            return;
        }
        CardInfoWindowDialog windowDialog = new CardInfoWindowDialog(CardInfoWindowDialog.ShowType.SIDEBOARD, playerView.getName());
        this.sideboardWindows.put(playerView.getName(), windowDialog);
        MageFrame.getDesktop().add((Component)windowDialog, windowDialog.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        windowDialog.loadCardsAndShow(this.sideboards.get(playerView.getName()), this.bigCard, this.gameId, false);
    }

    public void openTopLibraryWindow(String playerName) {
        String title = playerName + "'s top library card";
        if (this.revealed.containsKey(title)) {
            CardInfoWindowDialog cardInfoWindowDialog = this.revealed.get(title);
            if (cardInfoWindowDialog.isVisible()) {
                cardInfoWindowDialog.hideDialog();
            } else {
                cardInfoWindowDialog.show();
            }
        }
    }

    private void showRevealed(GameView game) {
        for (RevealedView revealView : game.getRevealed()) {
            this.handleGameInfoWindow(this.revealed, CardInfoWindowDialog.ShowType.REVEAL, revealView.getName(), (LinkedHashMap)revealView.getCards());
        }
        this.removeClosedCardInfoWindows(this.revealed);
    }

    private void showLookedAt(GameView game) {
        for (LookedAtView lookedAtView : game.getLookedAt()) {
            this.handleGameInfoWindow(this.lookedAt, CardInfoWindowDialog.ShowType.LOOKED_AT, lookedAtView.getName(), (LinkedHashMap)lookedAtView.getCards());
        }
        this.removeClosedCardInfoWindows(this.lookedAt);
    }

    private void showCompanion(GameView game) {
        for (RevealedView revealView : game.getCompanion()) {
            this.handleGameInfoWindow(this.companion, CardInfoWindowDialog.ShowType.COMPANION, revealView.getName(), (LinkedHashMap)revealView.getCards());
        }
        this.companion.forEach((name, companionDialog) -> {
            if (game.getCompanion().stream().noneMatch(revealedView -> revealedView.getName().equals(name))) {
                try {
                    companionDialog.setClosed(true);
                }
                catch (PropertyVetoException e) {
                    logger.error((Object)"Couldn't close companion dialog", (Throwable)e);
                }
            }
        });
        this.removeClosedCardInfoWindows(this.companion);
    }

    private void handleGameInfoWindow(Map<String, CardInfoWindowDialog> windowMap, CardInfoWindowDialog.ShowType showType, String name, LinkedHashMap cardsView) {
        CardInfoWindowDialog cardInfoWindowDialog;
        if (!windowMap.containsKey(name)) {
            cardInfoWindowDialog = new CardInfoWindowDialog(showType, name);
            windowMap.put(name, cardInfoWindowDialog);
            MageFrame.getDesktop().add((Component)cardInfoWindowDialog, cardInfoWindowDialog.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        } else {
            cardInfoWindowDialog = windowMap.get(name);
        }
        if (cardInfoWindowDialog != null && !cardInfoWindowDialog.isClosed()) {
            switch (showType) {
                case REVEAL:
                case REVEAL_TOP_LIBRARY:
                case COMPANION: {
                    cardInfoWindowDialog.loadCardsAndShow((CardsView)cardsView, this.bigCard, this.gameId, false);
                    break;
                }
                case LOOKED_AT: {
                    cardInfoWindowDialog.loadCardsAndShow(CardsViewUtil.convertSimple((SimpleCardsView)cardsView), this.bigCard, this.gameId, false);
                    break;
                }
            }
        }
    }

    private void removeClosedCardInfoWindows(Map<String, CardInfoWindowDialog> windowMap) {
        windowMap.entrySet().removeIf(entry -> ((CardInfoWindowDialog)entry.getValue()).isClosed());
    }

    public void ask(int messageId, GameView gameView, String question, Map<String, Serializable> options) {
        this.updateGame(messageId, gameView, false, options, null);
        this.feedbackPanel.prepareFeedback(FeedbackPanel.FeedbackMode.QUESTION, question, "", false, options, true, gameView.getPhase());
    }

    public boolean isMissGameData() {
        return this.lastGameData.game == null || this.lastGameData.game.getPlayers().isEmpty();
    }

    private void keepLastGameData(int messageId, GameView game, boolean showPlayable, Map<String, Serializable> options, Set<UUID> targets) {
        this.lastGameData.messageId = messageId;
        this.lastGameData.setNewGame(game);
        this.lastGameData.showPlayable = showPlayable;
        this.lastGameData.options = options;
        this.lastGameData.targets = targets;
    }

    private void prepareSelectableView() {
        // make cards/perm selectable/chooseable/playable update game data updates
        if (lastGameData.game == null) {
            return;
        }

        Zone needZone = Zone.ALL;
        if (lastGameData.options != null && lastGameData.options.containsKey("targetZone")) {
            needZone = (Zone) lastGameData.options.get("targetZone");
        }

        Set<UUID> needChosen = lastGameData.getChosenTargets();

        Set<UUID> needSelectable;
        if (lastGameData.targets != null) {
            needSelectable = lastGameData.targets;
        } else {
            needSelectable = new HashSet<>();
        }

        PlayableObjectsList needPlayable;
        if (lastGameData.showPlayable && lastGameData.game.getCanPlayObjects() != null) {
            needPlayable = lastGameData.game.getCanPlayObjects();
        } else {
            needPlayable = new PlayableObjectsList();
        }

        if (needChosen.isEmpty() && needSelectable.isEmpty() && needPlayable.isEmpty()) {
            return;
        }

        // hand
        if (needZone == Zone.HAND || needZone == Zone.ALL) {
            // my hand
            for (CardView card : lastGameData.game.getMyHand().values()) {
                if (needSelectable.contains(card.getId())) {
                    card.setChoosable(true);
                }
                if (needChosen.contains(card.getId())) {
                    card.setSelected(true);
                }
                if (needPlayable.containsObject(card.getId())) {
                    card.setPlayableStats(needPlayable.getStats(card.getId()));
                }
            }

            // opponent hands (switching by GUI's button with my hand)
            List<SimpleCardView> list = lastGameData.game.getOpponentHands().values().stream().flatMap(s -> s.values().stream()).collect(Collectors.toList());
            for (SimpleCardView card : list) {
                if (needSelectable.contains(card.getId())) {
                    card.setChoosable(true);
                }
                if (needChosen.contains(card.getId())) {
                    card.setSelected(true);
                }
                if (needPlayable.containsObject(card.getId())) {
                    card.setPlayableStats(needPlayable.getStats(card.getId()));
                }
            }

            // watched hands (switching by GUI's button with my hand)
            list = lastGameData.game.getWatchedHands().values().stream().flatMap(s -> s.values().stream()).collect(Collectors.toList());
            for (SimpleCardView card : list) {
                if (needSelectable.contains(card.getId())) {
                    card.setChoosable(true);
                }
                if (needChosen.contains(card.getId())) {
                    card.setSelected(true);
                }
                if (needPlayable.containsObject(card.getId())) {
                    card.setPlayableStats(needPlayable.getStats(card.getId()));
                }
            }
        }

        // stack
        if (needZone == Zone.STACK || needZone == Zone.ALL) {
            for (Map.Entry<UUID, CardView> card : lastGameData.game.getStack().entrySet()) {
                if (needSelectable.contains(card.getKey())) {
                    card.getValue().setChoosable(true);
                }
                if (needChosen.contains(card.getKey())) {
                    card.getValue().setSelected(true);
                }
                // users can activate abilities of the spell on the stack (example: Lightning Storm);
                if (needPlayable.containsObject(card.getKey())) {
                    card.getValue().setPlayableStats(needPlayable.getStats(card.getKey()));
                }
            }
        }

        // battlefield
        if (needZone == Zone.BATTLEFIELD || needZone == Zone.ALL) {
            for (PlayerView player : lastGameData.game.getPlayers()) {
                for (Map.Entry<UUID, PermanentView> perm : player.getBattlefield().entrySet()) {
                    if (needSelectable.contains(perm.getKey())) {
                        perm.getValue().setChoosable(true);
                    }
                    if (needChosen.contains(perm.getKey())) {
                        perm.getValue().setSelected(true);
                    }
                    if (needPlayable.containsObject(perm.getKey())) {
                        perm.getValue().setPlayableStats(needPlayable.getStats(perm.getKey()));
                    }
                }
            }
        }

        // graveyard
        if (needZone == Zone.GRAVEYARD || needZone == Zone.ALL) {
            for (PlayerView player : lastGameData.game.getPlayers()) {
                for (Map.Entry<UUID, CardView> card : player.getGraveyard().entrySet()) {
                    if (needSelectable.contains(card.getKey())) {
                        card.getValue().setChoosable(true);
                    }
                    if (needChosen.contains(card.getKey())) {
                        card.getValue().setSelected(true);
                    }
                    if (needPlayable.containsObject(card.getKey())) {
                        card.getValue().setPlayableStats(needPlayable.getStats(card.getKey()));
                    }
                }
            }
        }

        // sideboard
        if (needZone == Zone.OUTSIDE || needZone == Zone.ALL) {
            for (PlayerView player : lastGameData.game.getPlayers()) {
                for (Map.Entry<UUID, CardView> card : player.getSideboard().entrySet()) {
                    if (needSelectable.contains(card.getKey())) {
                        card.getValue().setChoosable(true);
                    }
                    if (needChosen.contains(card.getKey())) {
                        card.getValue().setSelected(true);
                    }
                    if (needPlayable.containsObject(card.getKey())) {
                        card.getValue().setPlayableStats(needPlayable.getStats(card.getKey()));
                    }
                }
            }
        }
        // sideboards (old windows all the time, e.g. unattached from game data)
        prepareSelectableWindows(sideboardWindows.values(), needSelectable, needChosen, needPlayable);

        // exile
        if (needZone == Zone.EXILED || needZone == Zone.ALL) {
            // exile from player panel
            for (PlayerView player : lastGameData.game.getPlayers()) {
                for (CardView card : player.getExile().values()) {
                    if (needSelectable.contains(card.getId())) {
                        card.setChoosable(true);
                    }
                    if (needChosen.contains(card.getId())) {
                        card.setSelected(true);
                    }
                    if (needPlayable.containsObject(card.getId())) {
                        card.setPlayableStats(needPlayable.getStats(card.getId()));
                    }
                }
            }

            // exile from windows
            for (ExileView exile : lastGameData.game.getExile()) {
                for (Map.Entry<UUID, CardView> card : exile.entrySet()) {
                    if (needSelectable.contains(card.getKey())) {
                        card.getValue().setChoosable(true);
                    }
                    if (needChosen.contains(card.getKey())) {
                        card.getValue().setSelected(true);
                    }
                    if (needPlayable.containsObject(card.getKey())) {
                        card.getValue().setPlayableStats(needPlayable.getStats(card.getKey()));
                    }
                }
            }
        }

        // command
        if (needZone == Zone.COMMAND || needZone == Zone.ALL) {
            for (PlayerView player : lastGameData.game.getPlayers()) {
                for (CommandObjectView com : player.getCommandObjectList()) {
                    if (needSelectable.contains(com.getId())) {
                        com.setChoosable(true);
                    }
                    if (needChosen.contains(com.getId())) {
                        com.setSelected(true);
                    }
                    if (needPlayable.containsObject(com.getId())) {
                        com.setPlayableStats(needPlayable.getStats(com.getId()));
                    }
                }
            }
        }

        // companion
        for (RevealedView rev : lastGameData.game.getCompanion()) {
            for (Map.Entry<UUID, CardView> card : rev.getCards().entrySet()) {
                if (needSelectable.contains(card.getKey())) {
                    card.getValue().setChoosable(true);
                }
                if (needChosen.contains(card.getKey())) {
                    card.getValue().setSelected(true);
                }
                if (needPlayable.containsObject(card.getKey())) {
                    card.getValue().setPlayableStats(needPlayable.getStats(card.getKey()));
                }
            }
        }

        // revealed (current cards)
        for (RevealedView rev : lastGameData.game.getRevealed()) {
            for (Map.Entry<UUID, CardView> card : rev.getCards().entrySet()) {
                if (needSelectable.contains(card.getKey())) {
                    card.getValue().setChoosable(true);
                }
                if (needChosen.contains(card.getKey())) {
                    card.getValue().setSelected(true);
                }
                if (needPlayable.containsObject(card.getKey())) {
                    card.getValue().setPlayableStats(needPlayable.getStats(card.getKey()));
                }
            }
        }
        // revealed (old windows)
        prepareSelectableWindows(revealed.values(), needSelectable, needChosen, needPlayable);

        // looked at (current cards)
        for (LookedAtView look : lastGameData.game.getLookedAt()) {
            for (Map.Entry<UUID, SimpleCardView> card : look.getCards().entrySet()) {
                if (needPlayable.containsObject(card.getKey())) {
                    card.getValue().setPlayableStats(needPlayable.getStats(card.getKey()));
                }
            }
        }
        // looked at (old windows)
        prepareSelectableWindows(lookedAt.values(), needSelectable, needChosen, needPlayable);
    }

    private void prepareSelectableWindows(
            Collection<CardInfoWindowDialog> windows,
            Set<UUID> needSelectable,
            Set<UUID> needChosen,
            PlayableObjectsList needPlayable
    ) {
        // lookAt or reveals windows clean up on next priority, so users can see dialogs, but xmage can't restore it
        // so it must be updated manually (it's ok to keep outdated cards in dialog, but not ok to show wrong selections)
        for (CardInfoWindowDialog window : windows) {
            for (MageCard mageCard : window.getMageCardsForUpdate().values()) {
                CardView cardView = mageCard.getOriginal();
                cardView.setChoosable(needSelectable.contains(cardView.getId()));
                cardView.setSelected(needChosen.contains(cardView.getId()));
                if (needPlayable.containsObject(cardView.getId())) {
                    cardView.setPlayableStats(needPlayable.getStats(cardView.getId()));
                } else {
                    cardView.setPlayableStats(new PlayableObjectStats());
                }
                // TODO: little bug with toggled night card after update/clicks, but that's ok (can't click on second side)
                mageCard.update(cardView);
            }
        }
    }

    public void pickTarget(int messageId, GameView gameView, Map<String, Serializable> options, String message, CardsView cardsView, Set<UUID> targets, boolean required) {
        this.updateGame(messageId, gameView, false, options, targets);
        this.hideAll();
        DialogManager.getManager(this.gameId).fadeOut();
        this.clearPickTargetDialogs();
        PopUpMenuType popupMenuType = null;
        if (this.lastGameData.options != null && options.containsKey("queryType")) {
            PlayerQueryEvent.QueryType needType = (PlayerQueryEvent.QueryType)((Object)this.lastGameData.options.get("queryType"));
            switch (needType) {
                case PICK_ABILITY: {
                    popupMenuType = PopUpMenuType.TRIGGER_ORDER;
                    break;
                }
                case PICK_TARGET: {
                    break;
                }
                default: {
                    logger.warn((Object)("Unknown query type in pick target: " + (Object)((Object)needType) + " in " + message));
                }
            }
        }
        Map<String, Serializable> options0 = this.lastGameData.options == null ? new HashMap<String, Serializable>() : this.lastGameData.options;
        ShowCardsDialog dialog = null;
        if (cardsView != null && !cardsView.isEmpty()) {
            dialog = this.prepareCardsDialog(message, cardsView, required, options0, popupMenuType);
            options0.put("dialog", dialog);
        }
        this.feedbackPanel.prepareFeedback(required ? FeedbackPanel.FeedbackMode.INFORM : FeedbackPanel.FeedbackMode.CANCEL, message, "", gameView.getSpecial(), options0, true, gameView.getPhase());
        if (dialog != null) {
            this.pickTarget.add(dialog);
        }
    }

    public void inform(int messageId, GameView gameView, String information) {
        this.updateGame(messageId, gameView);
        this.feedbackPanel.prepareFeedback(FeedbackPanel.FeedbackMode.INFORM, information, "", gameView.getSpecial(), null, false, gameView.getPhase());
    }

    public void endMessage(int messageId, GameView gameView, Map<String, Serializable> options, String message) {
        this.updateGame(messageId, gameView, false, options, null);
        this.hideAll();
        DialogManager.getManager(this.gameId).fadeOut();
        this.feedbackPanel.prepareFeedback(FeedbackPanel.FeedbackMode.END, message, "", false, null, true, null);
        ArrowBuilder.getBuilder().removeAllArrows(this.gameId);
    }

    public void select(int messageId, GameView gameView, Map<String, Serializable> options, String message) {
        this.updateGame(messageId, gameView, true, options, null);
        this.hideAll();
        DialogManager.getManager(this.gameId).fadeOut();
        this.abilityPicker.setVisible(false);
        this.holdingPriority = false;
        this.txtHoldPriority.setVisible(false);
        this.setMenuStates(PreferencesDialog.getCachedValue("gameManaAutopayment", "true").equals("true"), PreferencesDialog.getCachedValue("gameManaAutopaymentOnlyOne", "true").equals("true"), PreferencesDialog.getCachedValue("useFirstManaAbility", "false").equals("true"), false);
        boolean controllingPlayer = false;
        for (PlayerView playerView : gameView.getPlayers()) {
            if (!playerView.getPlayerId().equals(this.playerId)) continue;
            boolean bl = controllingPlayer = !gameView.getPriorityPlayerName().equals(playerView.getName());
            if (playerView.getStatesSavedSize() <= 0 || !gameView.getStack().isEmpty()) break;
            this.feedbackPanel.allowUndo(playerView.getStatesSavedSize());
            break;
        }
        HashMap<String, Serializable> panelOptions = new HashMap<String, Serializable>();
        if (this.lastGameData.options != null) {
            panelOptions.putAll(this.lastGameData.options);
        }
        panelOptions.put("your_turn", Boolean.valueOf(true));
        String activePlayerText = gameView.getActivePlayerId().equals(this.playerId) ? "Your turn" : gameView.getActivePlayerName() + "'s turn";
        String priorityPlayerText = "";
        if (controllingPlayer) {
            priorityPlayerText = " / priority " + gameView.getPriorityPlayerName();
        }
        String additionalMessage = activePlayerText + " / " + gameView.getStep().toString() + priorityPlayerText;
        this.feedbackPanel.prepareFeedback(FeedbackPanel.FeedbackMode.SELECT, message, additionalMessage, gameView.getSpecial(), panelOptions, true, gameView.getPhase());
    }

    public void playMana(int messageId, GameView gameView, Map<String, Serializable> options, String message) {
        this.updateGame(messageId, gameView, true, options, null);
        this.hideAll();
        DialogManager.getManager(this.gameId).fadeOut();
        this.feedbackPanel.prepareFeedback(FeedbackPanel.FeedbackMode.CANCEL, message, "", gameView.getSpecial(), options, true, gameView.getPhase());
    }

    public void playXMana(int messageId, GameView gameView, Map<String, Serializable> options, String message) {
        this.updateGame(messageId, gameView, true, options, null);
        this.hideAll();
        DialogManager.getManager(this.gameId).fadeOut();
        this.feedbackPanel.prepareFeedback(FeedbackPanel.FeedbackMode.CONFIRM, message, "", gameView.getSpecial(), null, true, gameView.getPhase());
    }

    public void replayMessage(String message) {
    }

    public void pickAbility(int messageId, GameView gameView, Map<String, Serializable> options, AbilityPickerView choices) {
        this.updateGame(messageId, gameView, false, options, null);
        this.hideAll();
        DialogManager.getManager(this.gameId).fadeOut();
        this.abilityPicker.show(choices, MageFrame.getDesktop().getMousePosition());
    }

    private void hideAll() {
        this.hidePickDialogs();
        this.abilityPicker.setVisible(false);
        ActionCallback callback = Plugins.instance.getActionCallback();
        ((MageActionCallback)callback).hideGameUpdate(this.gameId);
    }

    private ShowCardsDialog prepareCardsDialog(String title, CardsView cards, boolean required, Map<String, Serializable> options, PopUpMenuType popupMenuType) {
        ShowCardsDialog showCards = new ShowCardsDialog();
        JPopupMenu popupMenu = null;
        if (PopUpMenuType.TRIGGER_ORDER == popupMenuType) {
            popupMenu = this.popupMenuTriggerOrder;
        }
        showCards.loadCards(title, cards, this.bigCard, this.gameId, required, options, popupMenu, this.getShowCardsEventListener(showCards));
        return showCards;
    }

    public void getAmount(int messageId, GameView gameView, Map<String, Serializable> options, int min, int max, String message) {
        this.updateGame(messageId, gameView, false, options, null);
        this.hideAll();
        DialogManager.getManager(this.gameId).fadeOut();
        this.pickNumber.showDialog(min, max, message, () -> {
            if (this.pickNumber.isCancel()) {
                SessionHandler.sendPlayerBoolean(this.gameId, false);
            } else {
                SessionHandler.sendPlayerInteger(this.gameId, this.pickNumber.getAmount());
            }
        });
    }

    public void getMultiAmount(int messageId, GameView gameView, List<MultiAmountMessage> messages, Map<String, Serializable> options, int min, int max) {
        this.updateGame(messageId, gameView, false, options, null);
        this.hideAll();
        DialogManager.getManager(this.gameId).fadeOut();
        this.pickMultiNumber.init(this.gameId, this.bigCard);
        this.pickMultiNumber.showDialog(messages, min, max, this.lastGameData.options, () -> {
            if (this.pickMultiNumber.isCancel()) {
                SessionHandler.sendPlayerBoolean(this.gameId, false);
            } else {
                SessionHandler.sendPlayerString(this.gameId, this.pickMultiNumber.getMultiAmount());
            }
        });
    }

    public void getChoice(int messageId, GameView gameView, Map<String, Serializable> options, Choice choice, UUID objectId) {
        this.updateGame(messageId, gameView, false, options, null);
        this.hideAll();
        DialogManager.getManager(this.gameId).fadeOut();
        PickChoiceDialog pickChoice = new PickChoiceDialog();
        pickChoice.showDialog(choice, null, objectId, this.choiceWindowState, this.bigCard, () -> {
            String specialPrefix = choice.isChosenSpecial() ? "#" : "";
            String valueToSend = choice.isKeyChoice() ? choice.getChoiceKey() : choice.getChoice();
            SessionHandler.sendPlayerString(this.gameId, valueToSend == null ? null : specialPrefix + valueToSend);
            this.choiceWindowState = new MageDialogState(pickChoice);
            pickChoice.removeDialog();
        });
    }

    public void pickPile(int messageId, GameView gameView, Map<String, Serializable> options, String message, CardsView pile1, CardsView pile2) {
        this.updateGame(messageId, gameView, false, options, null);
        this.hideAll();
        DialogManager.getManager(this.gameId).fadeOut();
        this.clearPickPileDialogs();
        PickPileDialog pickPileDialog = new PickPileDialog();
        this.pickPile.add(pickPileDialog);
        pickPileDialog.showDialog(message, pile1, pile2, this.bigCard, this.gameId, () -> {
            if (pickPileDialog.isPickedOK()) {
                SessionHandler.sendPlayerBoolean(this.gameId, pickPileDialog.isPickedPile1());
            }
        });
    }

    public Map<UUID, PlayAreaPanel> getPlayers() {
        return this.players;
    }

    private void initComponents() {
        String[] phases;
        this.abilityPicker = new AbilityPicker(GUISizeHelper.dialogGuiScale);
        this.pnlHelperHandButtonsStackArea = new JPanel();
        this.pnlShortCuts = new JPanel();
        this.lblPhase = new JLabel();
        this.txtPhase = new JLabel();
        this.lblStep = new JLabel();
        this.txtStep = new JLabel();
        this.lblTurn = new JLabel();
        this.txtTurn = new JLabel();
        this.txtActivePlayer = new JLabel();
        this.lblActivePlayer = new JLabel();
        this.txtPriority = new JLabel();
        this.lblPriority = new JLabel();
        this.feedbackPanel = new FeedbackPanel();
        this.helper = new HelperPanel();
        this.feedbackPanel.setHelperPanel(this.helper);
        this.feedbackPanel.setLayout(new BorderLayout());
        this.feedbackPanel.add((Component)this.helper, "Center");
        Border paddingBorder = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        Border border = BorderFactory.createLineBorder(Color.DARK_GRAY, 2);
        this.txtHoldPriority = new JLabel();
        this.txtHoldPriority.setText("Hold");
        this.txtHoldPriority.setBorder(BorderFactory.createCompoundBorder(border, paddingBorder));
        this.txtHoldPriority.setBackground(Color.LIGHT_GRAY);
        this.txtHoldPriority.setOpaque(true);
        this.txtHoldPriority.setToolTipText("Holding priority after the next spell cast or ability activation");
        this.txtHoldPriority.setVisible(false);
        boolean displayButtonText = PreferencesDialog.getCurrentTheme().isShortcutsVisibleForSkipButtons();
        this.btnToggleMacro = new KeyboundButton("controlToggleMacro", displayButtonText);
        this.btnCancelSkip = new KeyboundButton("controlCancelSkip", displayButtonText);
        this.btnSkipToNextTurn = new KeyboundButton("controlNextTurn", displayButtonText);
        this.btnSkipToEndTurn = new KeyboundButton("controlEndStep", displayButtonText);
        this.btnSkipToNextMain = new KeyboundButton("controlMainStep", displayButtonText);
        this.btnSkipStack = new KeyboundButton("controlSkipStack", displayButtonText);
        this.btnSkipToYourTurn = new KeyboundButton("controlYourTurn", displayButtonText);
        this.btnSkipToEndStepBeforeYourTurn = new KeyboundButton("controlPriorEnd", displayButtonText);
        this.btnConcede = new JButton();
        this.btnSwitchHands = new JButton();
        this.btnStopWatching = new JButton();
        this.bigCard = new BigCard();
        this.pnlReplay = new JPanel();
        this.btnStopReplay = new JButton();
        this.btnNextPlay = new JButton();
        this.btnPlay = new JButton();
        this.btnSkipForward = new JButton();
        this.btnPreviousPlay = new JButton();
        this.pnlBattlefield = new JPanel();
        this.gameChatPanel = new ChatPanelBasic();
        this.gameChatPanel.useExtendedView(ChatPanelBasic.VIEW_MODE.GAME);
        this.userChatPanel = new ChatPanelBasic();
        this.userChatPanel.setParentChat(this.gameChatPanel);
        this.userChatPanel.useExtendedView(ChatPanelBasic.VIEW_MODE.CHAT);
        this.userChatPanel.setChatType(ChatPanelBasic.ChatType.GAME);
        this.gameChatPanel.setConnectedChat(this.userChatPanel);
        this.gameChatPanel.disableInput();
        this.gameChatPanel.setMinimumSize(new Dimension(100, 48));
        this.handContainer = new HandPanel();
        this.handCards = new HashMap<String, CardsView>();
        this.pnlShortCuts.setOpaque(false);
        this.stackObjects = new Cards();
        this.stackObjects.setZone(Zone.STACK);
        this.splitHandAndStack = new JSplitPane();
        this.splitHandAndStack.setBorder(null);
        this.splitHandAndStack.setResizeWeight(1.0);
        this.splitHandAndStack.setOneTouchExpandable(true);
        this.splitGameAndBigCard = new JSplitPane();
        this.splitGameAndBigCard.setBorder(null);
        this.splitGameAndBigCard.setResizeWeight(1.0);
        this.splitGameAndBigCard.setOneTouchExpandable(true);
        this.splitChatAndLogs = new JSplitPane();
        this.splitChatAndLogs.setOrientation(0);
        this.splitChatAndLogs.setResizeWeight(0.0);
        this.splitChatAndLogs.setTopComponent(this.userChatPanel);
        this.splitChatAndLogs.setBottomComponent(this.gameChatPanel);
        this.splitBattlefieldAndChats = new JSplitPane();
        this.splitBattlefieldAndChats.setBorder(null);
        this.splitBattlefieldAndChats.setResizeWeight(1.0);
        this.splitBattlefieldAndChats.setOneTouchExpandable(true);
        this.splitBattlefieldAndChats.setLeftComponent(this.pnlHelperHandButtonsStackArea);
        this.splitBattlefieldAndChats.setRightComponent(this.splitChatAndLogs);
        this.splitters.put("gamepanelDividerLocationsGameAndBigCard", new MageSplitter(this.splitGameAndBigCard, 0.85));
        this.splitters.put("gamepanelDividerLocationsBattlefieldAndChats", new MageSplitter(this.splitBattlefieldAndChats, 0.8));
        this.splitters.put("gamepanelDividerLocationsChatAndLogs", new MageSplitter(this.splitChatAndLogs, 0.4));
        this.lblPhase.setLabelFor(this.txtPhase);
        this.lblPhase.setText("Phase:");
        this.txtPhase.setText("Phase");
        this.txtPhase.setBorder(new LineBorder(new Color(153, 153, 153), 1, true));
        this.txtPhase.setMinimumSize(new Dimension(0, 16));
        this.lblStep.setLabelFor(this.txtStep);
        this.lblStep.setText("Step:");
        this.txtStep.setText("Step");
        this.txtStep.setBorder(new LineBorder(new Color(153, 153, 153), 1, true));
        this.txtStep.setMinimumSize(new Dimension(0, 16));
        this.lblTurn.setLabelFor(this.txtTurn);
        this.lblTurn.setText("Turn:");
        this.txtTurn.setText("Turn");
        this.txtTurn.setBorder(new LineBorder(new Color(153, 153, 153), 1, true));
        this.txtTurn.setMinimumSize(new Dimension(0, 16));
        this.txtActivePlayer.setText("Active Player");
        this.txtActivePlayer.setBorder(new LineBorder(new Color(153, 153, 153), 1, true));
        this.txtActivePlayer.setMinimumSize(new Dimension(0, 16));
        this.lblActivePlayer.setLabelFor(this.txtActivePlayer);
        this.lblActivePlayer.setText("Active Player:");
        this.txtPriority.setText("Priority Player");
        this.txtPriority.setBorder(new LineBorder(new Color(153, 153, 153), 1, true));
        this.txtPriority.setMinimumSize(new Dimension(0, 16));
        this.lblPriority.setLabelFor(this.txtPriority);
        this.lblPriority.setText("Priority Player:");
        int c = 2;
        this.abilityPicker.injectHotkeys(this, "ABILITY_PICKER");
        this.btnToggleMacro.setContentAreaFilled(false);
        this.btnToggleMacro.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.btnToggleMacro.setToolTipText("Toggle Record Macro (" + PreferencesDialog.getCachedKeyText("controlToggleMacro") + ").");
        this.btnToggleMacro.setFocusable(false);
        this.btnToggleMacro.addMouseListener(new FirstButtonMousePressedAction(e -> this.btnToggleMacroActionPerformed(null)));
        KeyStroke kst = PreferencesDialog.getCachedKeystroke("controlToggleMacro");
        this.getInputMap(c).put(kst, "F8_PRESS");
        this.getActionMap().put("F8_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                GamePanel.this.btnToggleMacroActionPerformed(actionEvent);
            }
        });
        KeyStroke ks3 = PreferencesDialog.getCachedKeystroke("controlCancelSkip");
        this.getInputMap(c).put(ks3, "F3_PRESS");
        this.getActionMap().put("F3_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                GamePanel.this.restorePriorityActionPerformed(actionEvent);
            }
        });
        this.btnCancelSkip.setContentAreaFilled(false);
        this.btnCancelSkip.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.btnCancelSkip.setToolTipText("CANCEL all skips");
        this.btnCancelSkip.setFocusable(false);
        this.btnCancelSkip.addMouseListener(new FirstButtonMousePressedAction(e -> this.restorePriorityActionPerformed(null)));
        this.btnSkipToNextTurn.setContentAreaFilled(false);
        this.btnSkipToNextTurn.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.btnSkipToNextTurn.setToolTipText("dynamic");
        this.btnSkipToNextTurn.setFocusable(false);
        this.btnSkipToNextTurn.addMouseListener(new FirstButtonMousePressedAction(e -> this.btnEndTurnActionPerformed(null)));
        KeyStroke ks = PreferencesDialog.getCachedKeystroke("controlNextTurn");
        this.getInputMap(c).put(ks, "F4_PRESS");
        this.getActionMap().put("F4_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                GamePanel.this.btnEndTurnActionPerformed(actionEvent);
            }
        });
        this.btnSkipToEndTurn.setContentAreaFilled(false);
        this.btnSkipToEndTurn.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.btnSkipToEndTurn.setToolTipText("dynamic");
        this.btnSkipToEndTurn.setFocusable(false);
        this.btnSkipToEndTurn.addMouseListener(new FirstButtonMousePressedAction(e -> this.btnUntilEndOfTurnActionPerformed(null)));
        ks = PreferencesDialog.getCachedKeystroke("controlEndStep");
        this.getInputMap(c).put(ks, "F5_PRESS");
        this.getActionMap().put("F5_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                GamePanel.this.btnUntilEndOfTurnActionPerformed(actionEvent);
            }
        });
        ks = PreferencesDialog.getCachedKeystroke("controlSkipTurn");
        this.getInputMap(c).put(ks, "F6_PRESS");
        this.getActionMap().put("F6_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                GamePanel.this.btnEndTurnSkipStackActionPerformed(actionEvent);
            }
        });
        this.btnSkipToNextMain.setContentAreaFilled(false);
        this.btnSkipToNextMain.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.btnSkipToNextMain.setToolTipText("dynamic");
        this.btnSkipToNextMain.setFocusable(false);
        this.btnSkipToNextMain.addMouseListener(new FirstButtonMousePressedAction(e -> this.btnUntilNextMainPhaseActionPerformed(null)));
        ks = PreferencesDialog.getCachedKeystroke("controlMainStep");
        this.getInputMap(c).put(ks, "F7_PRESS");
        this.getActionMap().put("F7_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                GamePanel.this.btnUntilNextMainPhaseActionPerformed(actionEvent);
            }
        });
        this.btnSkipToYourTurn.setContentAreaFilled(false);
        this.btnSkipToYourTurn.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.btnSkipToYourTurn.setToolTipText("dynamic");
        this.btnSkipToYourTurn.setFocusable(false);
        this.btnSkipToYourTurn.addMouseListener(new FirstButtonMousePressedAction(e -> this.btnPassPriorityUntilNextYourTurnActionPerformed(null)));
        KeyStroke ks9 = PreferencesDialog.getCachedKeystroke("controlYourTurn");
        this.getInputMap(c).put(ks9, "F9_PRESS");
        this.getActionMap().put("F9_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                GamePanel.this.btnPassPriorityUntilNextYourTurnActionPerformed(actionEvent);
            }
        });
        this.btnSkipToEndStepBeforeYourTurn.setContentAreaFilled(false);
        this.btnSkipToEndStepBeforeYourTurn.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.btnSkipToEndStepBeforeYourTurn.setToolTipText("dynamic");
        this.btnSkipToEndStepBeforeYourTurn.setFocusable(false);
        this.btnSkipToEndStepBeforeYourTurn.addMouseListener(new FirstButtonMousePressedAction(e -> this.btnSkipToEndStepBeforeYourTurnActionPerformed(null)));
        KeyStroke ks11 = PreferencesDialog.getCachedKeystroke("controlPriorEnd");
        this.getInputMap(c).put(ks11, "F11_PRESS");
        this.getActionMap().put("F11_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                GamePanel.this.btnSkipToEndStepBeforeYourTurnActionPerformed(actionEvent);
            }
        });
        this.btnSkipStack.setContentAreaFilled(false);
        this.btnSkipStack.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.btnSkipStack.setToolTipText("dynamic");
        this.btnSkipStack.setFocusable(false);
        this.btnSkipStack.addMouseListener(new FirstButtonMousePressedAction(e -> this.btnPassPriorityUntilStackResolvedActionPerformed(null)));
        ks = PreferencesDialog.getCachedKeystroke("controlSkipStack");
        this.getInputMap(c).put(ks, "F10_PRESS");
        this.getActionMap().put("F10_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                GamePanel.this.btnPassPriorityUntilStackResolvedActionPerformed(actionEvent);
            }
        });
        this.btnConcede.setContentAreaFilled(false);
        this.btnConcede.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.btnConcede.setToolTipText("CONCEDE current game");
        this.btnConcede.setFocusable(false);
        this.btnConcede.addMouseListener(new FirstButtonMousePressedAction(e -> this.btnConcedeActionPerformed(null)));
        this.updateSkipButtons();
        KeyStroke ks2 = PreferencesDialog.getCachedKeystroke("controlConfirm");
        this.getInputMap(c).put(ks2, "F2_PRESS");
        this.getActionMap().put("F2_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                if (GamePanel.this.feedbackPanel != null) {
                    GamePanel.this.feedbackPanel.pressOKYesOrDone();
                }
            }
        });
        KeyStroke ks12 = PreferencesDialog.getCachedKeystroke("controlSwitchChat");
        this.getInputMap(c).put(ks12, "F12_PRESS");
        this.getActionMap().put("F12_PRESS", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isChatInputActive()) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
                } else if (!GamePanel.this.isUserImputActive()) {
                    GamePanel.this.userChatPanel.getTxtMessageInputComponent().requestFocusInWindow();
                }
            }
        });
        KeyStroke ksAltE = KeyStroke.getKeyStroke(69, 8);
        this.getInputMap(c).put(ksAltE, "ENLARGE");
        this.getActionMap().put("ENLARGE", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                ActionCallback callback = Plugins.instance.getActionCallback();
                ((MageActionCallback)callback).enlargeCard(EnlargeMode.NORMAL);
            }
        });
        KeyStroke ksAltS = KeyStroke.getKeyStroke(83, 8);
        this.getInputMap(c).put(ksAltS, "ENLARGE_SOURCE");
        this.getActionMap().put("ENLARGE_SOURCE", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                ActionCallback callback = Plugins.instance.getActionCallback();
                ((MageActionCallback)callback).enlargeCard(EnlargeMode.ALTERNATE);
            }
        });
        KeyStroke ksAlt1 = KeyStroke.getKeyStroke(49, 8);
        this.getInputMap(c).put(ksAlt1, "USEFIRSTMANAABILITY");
        this.getActionMap().put("USEFIRSTMANAABILITY", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                SessionHandler.sendPlayerAction(PlayerAction.USE_FIRST_MANA_ABILITY_ON, GamePanel.this.gameId, null);
                GamePanel.this.setMenuStates(PreferencesDialog.getCachedValue("gameManaAutopayment", "true").equals("true"), PreferencesDialog.getCachedValue("gameManaAutopaymentOnlyOne", "true").equals("true"), PreferencesDialog.getCachedValue("useFirstManaAbility", "false").equals("true"), GamePanel.this.holdingPriority);
            }
        });
        KeyStroke ksAltEReleased = KeyStroke.getKeyStroke(69, 8, true);
        this.getInputMap(c).put(ksAltEReleased, "ENLARGE_RELEASE");
        KeyStroke ksAltSReleased = KeyStroke.getKeyStroke(83, 8, true);
        this.getInputMap(c).put(ksAltSReleased, "ENLARGE_RELEASE");
        this.getActionMap().put("ENLARGE_RELEASE", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                ActionCallback callback = Plugins.instance.getActionCallback();
                ((MageActionCallback)callback).hideEnlargedCard();
            }
        });
        KeyStroke ksAlt1Released = KeyStroke.getKeyStroke(49, 8, true);
        this.getInputMap(c).put(ksAlt1Released, "USEFIRSTMANAABILITY_RELEASE");
        this.getActionMap().put("USEFIRSTMANAABILITY_RELEASE", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (GamePanel.this.isUserImputActive()) {
                    return;
                }
                SessionHandler.sendPlayerAction(PlayerAction.USE_FIRST_MANA_ABILITY_OFF, GamePanel.this.gameId, null);
                GamePanel.this.setMenuStates(PreferencesDialog.getCachedValue("gameManaAutopayment", "true").equals("true"), PreferencesDialog.getCachedValue("gameManaAutopaymentOnlyOne", "true").equals("true"), PreferencesDialog.getCachedValue("useFirstManaAbility", "false").equals("true"), GamePanel.this.holdingPriority);
            }
        });
        this.btnSwitchHands.setContentAreaFilled(false);
        this.btnSwitchHands.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.btnSwitchHands.setFocusable(false);
        this.btnSwitchHands.setToolTipText("Switch between your hand cards and hand cards of controlled players.");
        this.btnSwitchHands.addMouseListener(new FirstButtonMousePressedAction(e -> this.btnSwitchHandActionPerformed(null)));
        this.btnStopWatching.setContentAreaFilled(false);
        this.btnStopWatching.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.btnStopWatching.setFocusable(false);
        this.btnStopWatching.setToolTipText("Stop watching this game.");
        this.btnStopWatching.addMouseListener(new FirstButtonMousePressedAction(e -> this.btnStopWatchingActionPerformed(null)));
        this.stackObjects.setBackgroundColor(new Color(0, 0, 0, 40));
        this.btnStopReplay.setIcon(new ImageIcon(this.getClass().getResource("/buttons/control_stop.png")));
        this.btnStopReplay.addActionListener(evt -> this.btnStopReplayActionPerformed(evt));
        this.btnNextPlay.setIcon(new ImageIcon(this.getClass().getResource("/buttons/control_stop_right.png")));
        this.btnNextPlay.addActionListener(evt -> this.btnNextPlayActionPerformed(evt));
        this.btnPlay.setIcon(new ImageIcon(this.getClass().getResource("/buttons/control_right.png")));
        this.btnPlay.addActionListener(evt -> this.btnPlayActionPerformed(evt));
        this.btnSkipForward.setIcon(new ImageIcon(this.getClass().getResource("/buttons/control_double_stop_right.png")));
        this.btnSkipForward.addActionListener(evt -> this.btnSkipForwardActionPerformed(evt));
        this.btnPreviousPlay.setIcon(new ImageIcon(this.getClass().getResource("/buttons/control_stop_left.png")));
        this.btnPreviousPlay.addActionListener(evt -> this.btnPreviousPlayActionPerformed(evt));
        this.initPopupMenuTriggerOrder();
        this.pnlBattlefield.setLayout(new GridBagLayout());
        this.jPhases = new JPanel();
        this.jPhases.setBackground(new Color(0, 0, 0, 0));
        MouseAdapter phasesMouseAdapter = new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent evt) {
                GamePanel.this.mouseClickPhaseBar(evt);
            }
        };
        if (DebugUtil.GUI_GAME_DRAW_PHASE_BUTTONS_PANEL_BORDER) {
            this.jPhases.setBorder(BorderFactory.createLineBorder(Color.red));
        }
        for (String name : phases = new String[]{"Untap", "Upkeep", "Draw", "Main1", "Combat_Start", "Combat_Attack", "Combat_Block", "Combat_Damage", "Combat_End", "Main2", "Cleanup", "Next_Turn"}) {
            this.createPhaseButton(name, phasesMouseAdapter);
        }
        this.pnlReplay.setOpaque(false);
        this.phasesContainer = new JPanel(new BorderLayout());
        this.phasesContainer.setOpaque(false);
        this.phasesContainer.setBorder(new EmptyBorder(0, 2, 8, 2));
        this.phasesContainer.add((Component)this.jPhases, "South");
        this.bigCardPanel = new JPanel();
        this.bigCardPanel.setOpaque(false);
        this.bigCardPanel.setLayout(new BorderLayout());
        this.bigCardPanel.add((Component)this.bigCard, "North");
        this.splitGameAndBigCard.setLeftComponent(this.splitBattlefieldAndChats);
        this.splitGameAndBigCard.setRightComponent(this.bigCardPanel);
    }

    private void removeListener() {
        for (MouseListener mouseListener : this.getMouseListeners()) {
            this.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnToggleMacro.getMouseListeners()) {
            this.btnToggleMacro.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnCancelSkip.getMouseListeners()) {
            this.btnCancelSkip.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnConcede.getMouseListeners()) {
            this.btnConcede.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnSkipToYourTurn.getMouseListeners()) {
            this.btnSkipToYourTurn.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnSkipStack.getMouseListeners()) {
            this.btnSkipStack.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnSkipToEndStepBeforeYourTurn.getMouseListeners()) {
            this.btnSkipToEndStepBeforeYourTurn.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnSkipToEndTurn.getMouseListeners()) {
            this.btnSkipToEndTurn.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnSkipToNextMain.getMouseListeners()) {
            this.btnSkipToNextMain.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnSkipToNextTurn.getMouseListeners()) {
            this.btnSkipToNextTurn.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnSwitchHands.getMouseListeners()) {
            this.btnSwitchHands.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.btnStopWatching.getMouseListeners()) {
            this.btnStopWatching.removeMouseListener(mouseListener);
        }
        for (MouseListener mouseListener : this.jPhases.getMouseListeners()) {
            this.jPhases.removeMouseListener(mouseListener);
        }
        for (String name : this.phaseButtons.keySet()) {
            HoverButton hoverButton = this.phaseButtons.get(name);
            for (MouseListener ml : hoverButton.getMouseListeners()) {
                hoverButton.removeMouseListener(ml);
            }
        }
        for (ActionListener actionListener : this.btnPlay.getActionListeners()) {
            this.btnPlay.removeActionListener(actionListener);
        }
        for (ActionListener actionListener : this.btnStopReplay.getActionListeners()) {
            this.btnStopReplay.removeActionListener(actionListener);
        }
        for (ActionListener actionListener : this.btnNextPlay.getActionListeners()) {
            this.btnNextPlay.removeActionListener(actionListener);
        }
        for (ActionListener actionListener : this.btnNextPlay.getActionListeners()) {
            this.btnNextPlay.removeActionListener(actionListener);
        }
        for (ActionListener actionListener : this.btnPreviousPlay.getActionListeners()) {
            this.btnPreviousPlay.removeActionListener(actionListener);
        }
        for (ActionListener actionListener : this.btnSkipForward.getActionListeners()) {
            this.btnSkipForward.removeActionListener(actionListener);
        }
        BasicSplitPaneUI myUi = (BasicSplitPaneUI)this.splitGameAndBigCard.getUI();
        BasicSplitPaneDivider divider = myUi.getDivider();
        JButton upArrowButton = (JButton)divider.getComponent(0);
        for (ActionListener al : upArrowButton.getActionListeners()) {
            upArrowButton.removeActionListener(al);
        }
        JButton jButton = (JButton)divider.getComponent(1);
        for (ActionListener actionListener : jButton.getActionListeners()) {
            jButton.removeActionListener(actionListener);
        }
        for (EventListener eventListener : this.getComponentListeners()) {
            this.removeComponentListener((ComponentListener)eventListener);
        }
        for (EventListener eventListener : this.getKeyListeners()) {
            this.removeKeyListener((KeyListener)eventListener);
        }
    }

    private void btnConcedeActionPerformed(ActionEvent evt) {
        UserRequestMessage message = new UserRequestMessage("Confirm concede", "Are you sure you want to concede?");
        message.setButton1("No", null);
        message.setButton2("Yes", PlayerAction.CLIENT_CONCEDE_GAME);
        message.setGameId(this.gameId);
        MageFrame.getInstance().showUserRequestDialog(message);
    }

    private void btnToggleMacroActionPerformed(ActionEvent evt) {
        SessionHandler.sendPlayerAction(PlayerAction.TOGGLE_RECORD_MACRO, this.gameId, null);
        this.skipButtons.activateSkipButton("");
        AudioManager.playOnSkipButton();
        if (this.btnToggleMacro.getBorder().equals(BORDER_ACTIVE)) {
            this.btnToggleMacro.setBorder(BORDER_NON_ACTIVE);
        } else {
            this.btnToggleMacro.setBorder(BORDER_ACTIVE);
        }
    }

    private void btnEndTurnActionPerformed(ActionEvent evt) {
        SessionHandler.sendPlayerAction(PlayerAction.PASS_PRIORITY_UNTIL_NEXT_TURN, this.gameId, null);
        this.skipButtons.activateSkipButton("controlNextTurn");
        AudioManager.playOnSkipButton();
        this.updateSkipButtons();
    }

    private boolean isChatInputUnderCursor(Point p) {
        Component c = this.getComponentAt(p);
        return this.gameChatPanel.getTxtMessageInputComponent().equals(c) || this.userChatPanel.getTxtMessageInputComponent().equals(c);
    }

    private boolean isChatInputActive() {
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return this.gameChatPanel.getTxtMessageInputComponent().equals(c) || this.userChatPanel.getTxtMessageInputComponent().equals(c);
    }

    private boolean isUserImputActive() {
        return MageDialog.isModalDialogActivated() || this.isChatInputActive();
    }

    private void btnUntilEndOfTurnActionPerformed(ActionEvent evt) {
        SessionHandler.sendPlayerAction(PlayerAction.PASS_PRIORITY_UNTIL_TURN_END_STEP, this.gameId, null);
        this.skipButtons.activateSkipButton("controlEndStep");
        AudioManager.playOnSkipButton();
        this.updateSkipButtons();
    }

    private void btnEndTurnSkipStackActionPerformed(ActionEvent evt) {
        logger.error((Object)"Skip action don't used", new Throwable());
    }

    private void btnUntilNextMainPhaseActionPerformed(ActionEvent evt) {
        SessionHandler.sendPlayerAction(PlayerAction.PASS_PRIORITY_UNTIL_NEXT_MAIN_PHASE, this.gameId, null);
        this.skipButtons.activateSkipButton("controlMainStep");
        AudioManager.playOnSkipButton();
        this.updateSkipButtons();
    }

    private void btnPassPriorityUntilNextYourTurnActionPerformed(ActionEvent evt) {
        SessionHandler.sendPlayerAction(PlayerAction.PASS_PRIORITY_UNTIL_MY_NEXT_TURN, this.gameId, null);
        this.skipButtons.activateSkipButton("controlYourTurn");
        AudioManager.playOnSkipButton();
        this.updateSkipButtons();
    }

    private void btnPassPriorityUntilStackResolvedActionPerformed(ActionEvent evt) {
        SessionHandler.sendPlayerAction(PlayerAction.PASS_PRIORITY_UNTIL_STACK_RESOLVED, this.gameId, null);
        this.skipButtons.activateSkipButton("controlSkipStack");
        AudioManager.playOnSkipButton();
        this.updateSkipButtons();
    }

    private void btnSkipToEndStepBeforeYourTurnActionPerformed(ActionEvent evt) {
        SessionHandler.sendPlayerAction(PlayerAction.PASS_PRIORITY_UNTIL_END_STEP_BEFORE_MY_NEXT_TURN, this.gameId, null);
        this.skipButtons.activateSkipButton("controlPriorEnd");
        AudioManager.playOnSkipButton();
        this.updateSkipButtons();
    }

    private void restorePriorityActionPerformed(ActionEvent evt) {
        SessionHandler.sendPlayerAction(PlayerAction.PASS_PRIORITY_CANCEL_ALL_ACTIONS, this.gameId, null);
        this.skipButtons.activateSkipButton("");
        AudioManager.playOnSkipButtonCancel();
        this.updateSkipButtons();
    }

    private void mouseClickPhaseBar(MouseEvent evt) {
        if (SwingUtilities.isLeftMouseButton(evt)) {
            PreferencesDialog.main(new String[]{"Open-Phases-Tab"});
        }
    }

    private void btnSwitchHandActionPerformed(ActionEvent evt) {
        Object[] choices = this.handCards.keySet().toArray(new String[0]);
        String newChosenHandKey = (String)JOptionPane.showInputDialog(this, "Choose hand to display:", "Switch between hands", -1, null, choices, this.chosenHandKey);
        if (newChosenHandKey != null && !newChosenHandKey.isEmpty()) {
            this.chosenHandKey = newChosenHandKey;
            CardsView cards = this.handCards.get(this.chosenHandKey);
            this.handContainer.loadCards(cards, this.bigCard, this.gameId);
        }
    }

    private void btnStopWatchingActionPerformed(ActionEvent evt) {
        UserRequestMessage message = new UserRequestMessage("Stop watching", "Are you sure you want to stop watching?");
        message.setButton1("No", null);
        message.setButton2("Yes", PlayerAction.CLIENT_STOP_WATCHING);
        message.setGameId(this.gameId);
        MageFrame.getInstance().showUserRequestDialog(message);
    }

    private void btnStopReplayActionPerformed(ActionEvent evt) {
        if (this.replayTask != null && !this.replayTask.isDone()) {
            this.replayTask.cancel(true);
        } else {
            UserRequestMessage message = new UserRequestMessage("Stop replay", "Are you sure you want to stop replay?");
            message.setButton1("No", null);
            message.setButton2("Yes", PlayerAction.CLIENT_REPLAY_ACTION);
            message.setGameId(this.gameId);
            MageFrame.getInstance().showUserRequestDialog(message);
        }
    }

    private void btnNextPlayActionPerformed(ActionEvent evt) {
        SessionHandler.nextPlay(this.gameId);
    }

    private void btnPreviousPlayActionPerformed(ActionEvent evt) {
        SessionHandler.previousPlay(this.gameId);
    }

    private void btnPlayActionPerformed(ActionEvent evt) {
        if (this.replayTask == null || this.replayTask.isDone()) {
            this.replayTask = new ReplayTask(this.gameId);
            this.replayTask.execute();
        }
    }

    private void btnSkipForwardActionPerformed(ActionEvent evt) {
        SessionHandler.skipForward(this.gameId, 10);
    }

    public void setJLayeredPane(JLayeredPane jLayeredPane) {
        this.jLayeredPane = jLayeredPane;
    }

    public void installComponents() {
        this.jLayeredPane.setOpaque(false);
        this.jLayeredPane.add(DialogManager.getManager(this.gameId), JLayeredPane.MODAL_LAYER, 0);
        this.installAbilityPicker();
    }

    private void installAbilityPicker() {
        this.jLayeredPane.add((Component)((Object)this.abilityPicker), JLayeredPane.MODAL_LAYER);
        this.abilityPicker.setVisible(false);
    }

    private void uninstallComponents() {
        if (this.jLayeredPane != null) {
            this.jLayeredPane.remove(DialogManager.getManager(this.gameId));
        }
        DialogManager.removeGame(this.gameId);
        this.uninstallAbilityPicker();
    }

    private void uninstallAbilityPicker() {
        this.abilityPicker.setVisible(false);
        if (this.jLayeredPane != null) {
            this.jLayeredPane.remove((Component)((Object)this.abilityPicker));
        }
        this.abilityPicker.cleanUp();
    }

    private void createPhaseButton(String name, MouseAdapter mouseAdapter) {
        int buttonSize = GUISizeHelper.gamePhaseButtonSize;
        Rectangle rect = new Rectangle(buttonSize, buttonSize);
        HoverButton button = new HoverButton("", ImageManagerImpl.instance.getPhaseImage(name, buttonSize), rect);
        button.setToolTipText(name.replaceAll("_", " "));
        button.setPreferredSize(new Dimension(buttonSize, buttonSize));
        button.addMouseListener(mouseAdapter);
        this.phaseButtons.put(name, button);
        this.jPhases.add(button);
    }

    private Listener<Event> getShowCardsEventListener(ShowCardsDialog dialog) {
        return event -> {
            JPopupMenu menu;
            if (event.getEventType() == ClientEventType.CARD_POPUP_MENU && event.getComponent() != null && event.getComponent() instanceof MageCard && (menu = ((MageCard)event.getComponent()).getPopupMenu()) != null) {
                this.cardViewPopupMenu = (CardView)event.getSource();
                menu.show(event.getComponent(), event.getxPos(), event.getyPos());
            }
        };
    }

    public void handleTriggerOrderPopupMenuEvent(ActionEvent e) {
        UUID abilityId = null;
        String abilityRuleText = null;
        if (this.cardViewPopupMenu instanceof CardView && this.cardViewPopupMenu.getAbility() != null) {
            abilityId = this.cardViewPopupMenu.getAbility().getId();
            if (!this.cardViewPopupMenu.getAbility().getRules().isEmpty() && !((String)this.cardViewPopupMenu.getAbility().getRules().get(0)).isEmpty()) {
                abilityRuleText = (String)this.cardViewPopupMenu.getAbility().getRules().get(0);
                abilityRuleText = abilityRuleText.replace("{this}", this.cardViewPopupMenu.getName());
            }
        }
        switch (e.getActionCommand()) {
            case "cmdAutoOrderFirst": {
                SessionHandler.sendPlayerAction(PlayerAction.TRIGGER_AUTO_ORDER_ABILITY_FIRST, this.gameId, abilityId);
                SessionHandler.sendPlayerUUID(this.gameId, abilityId);
                break;
            }
            case "cmdAutoOrderLast": {
                SessionHandler.sendPlayerAction(PlayerAction.TRIGGER_AUTO_ORDER_ABILITY_LAST, this.gameId, abilityId);
                SessionHandler.sendPlayerUUID(this.gameId, null);
                break;
            }
            case "cmdAutoOrderNameFirst": {
                if (abilityRuleText == null) break;
                SessionHandler.sendPlayerAction(PlayerAction.TRIGGER_AUTO_ORDER_NAME_FIRST, this.gameId, abilityRuleText);
                SessionHandler.sendPlayerUUID(this.gameId, abilityId);
                break;
            }
            case "cmdAutoOrderNameLast": {
                if (abilityRuleText == null) break;
                SessionHandler.sendPlayerAction(PlayerAction.TRIGGER_AUTO_ORDER_NAME_LAST, this.gameId, abilityRuleText);
                SessionHandler.sendPlayerUUID(this.gameId, null);
                break;
            }
            case "cmdAutoOrderResetAll": {
                SessionHandler.sendPlayerAction(PlayerAction.TRIGGER_AUTO_ORDER_RESET_ALL, this.gameId, null);
                break;
            }
        }
        for (ShowCardsDialog dialog : this.pickTarget) {
            dialog.removeDialog();
        }
        for (PickPileDialog dialog : this.pickPile) {
            dialog.removeDialog();
        }
    }

    private void initPopupMenuTriggerOrder() {
        ActionListener actionListener = e -> this.handleTriggerOrderPopupMenuEvent(e);
        this.popupMenuTriggerOrder = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Put this ability always first on the stack");
        menuItem.setActionCommand(CMD_AUTO_ORDER_FIRST);
        menuItem.addActionListener(actionListener);
        this.popupMenuTriggerOrder.add(menuItem);
        menuItem = new JMenuItem("Put this ability always last on the stack");
        menuItem.setActionCommand(CMD_AUTO_ORDER_LAST);
        menuItem.addActionListener(actionListener);
        this.popupMenuTriggerOrder.add(menuItem);
        menuItem = new JMenuItem("Put all abilities with that rule text always first on the stack");
        menuItem.setActionCommand(CMD_AUTO_ORDER_NAME_FIRST);
        menuItem.addActionListener(actionListener);
        this.popupMenuTriggerOrder.add(menuItem);
        menuItem = new JMenuItem("Put all abilities with that rule text always last on the stack");
        menuItem.setActionCommand(CMD_AUTO_ORDER_NAME_LAST);
        menuItem.addActionListener(actionListener);
        this.popupMenuTriggerOrder.add(menuItem);
        menuItem = new JMenuItem("Reset all order settings for triggered abilities");
        menuItem.setActionCommand(CMD_AUTO_ORDER_RESET_ALL);
        menuItem.addActionListener(actionListener);
        this.popupMenuTriggerOrder.add(menuItem);
    }

    public String getGameLog() {
        return this.gameChatPanel.getText();
    }

    public FeedbackPanel getFeedbackPanel() {
        return this.feedbackPanel;
    }

    public void handleEvent(AWTEvent event) {
        if (event instanceof InputEvent) {
            InputEvent input;
            KeyEvent key;
            int keyCode;
            int id = event.getID();
            boolean isActionEvent = false;
            if (id == 501) {
                isActionEvent = true;
                if (event instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent)event;
                    if (this.isChatInputActive() && !this.isChatInputUnderCursor(me.getPoint())) {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
                    }
                }
            } else if (id == 401 && ((keyCode = (key = (KeyEvent)event).getKeyCode()) == 10 || keyCode == 32)) {
                isActionEvent = true;
            }
            if (isActionEvent && ((input = (InputEvent)event).getModifiersEx() & holdPriorityMask) != 0) {
                this.setMenuStates(PreferencesDialog.getCachedValue("gameManaAutopayment", "true").equals("true"), PreferencesDialog.getCachedValue("gameManaAutopaymentOnlyOne", "true").equals("true"), PreferencesDialog.getCachedValue("useFirstManaAbility", "false").equals("true"), true);
                this.holdPriority(true);
            }
        }
    }

    public void holdPriority(boolean holdPriority) {
        if (this.holdingPriority != holdPriority) {
            this.holdingPriority = holdPriority;
            this.txtHoldPriority.setVisible(holdPriority);
            if (holdPriority) {
                SessionHandler.sendPlayerAction(PlayerAction.HOLD_PRIORITY, this.gameId, null);
            } else {
                SessionHandler.sendPlayerAction(PlayerAction.UNHOLD_PRIORITY, this.gameId, null);
            }
        }
    }

    public static class LastGameData {
        int messageId;
        GameView game;
        boolean showPlayable;
        Map<String, Serializable> options;
        Set<UUID> targets;
        Map<UUID, CardView> allCardsIndex = new HashMap<UUID, CardView>();

        private void setNewGame(GameView game) {
            this.game = game;
            this.prepareAllCardsIndex();
        }

        private void prepareAllCardsIndex() {
            this.allCardsIndex.clear();
            if (this.game == null) {
                return;
            }
            this.game.getMyHand().values().forEach(c -> this.allCardsIndex.put(c.getId(), (CardView)c));
            this.game.getMyHelperEmblems().values().forEach(c -> this.allCardsIndex.put(c.getId(), (CardView)c));
            this.game.getStack().values().forEach(c -> this.allCardsIndex.put(c.getId(), (CardView)c));
            this.game.getExile().stream().flatMap(s -> s.values().stream()).forEach(c -> this.allCardsIndex.put(c.getId(), (CardView)c));
            this.game.getLookedAt().stream().flatMap(s -> s.getCards().values().stream()).filter(c -> c instanceof CardView).map(c -> (CardView)c).forEach(c -> this.allCardsIndex.put(c.getId(), (CardView)c));
            this.game.getRevealed().stream().flatMap(s -> s.getCards().values().stream()).forEach(c -> this.allCardsIndex.put(c.getId(), (CardView)c));
            this.game.getPlayers().forEach(player -> {
                player.getBattlefield().values().forEach(c -> this.allCardsIndex.put(c.getId(), (CardView)c));
                player.getGraveyard().values().forEach(c -> this.allCardsIndex.put(c.getId(), (CardView)c));
                Optional.ofNullable(player.getTopCard()).ifPresent(c -> this.allCardsIndex.put(c.getId(), (CardView)c));
                player.getCommandObjectList().forEach(object -> {
                    if (object instanceof CardView) {
                        this.allCardsIndex.put(object.getId(), (CardView)object);
                    } else if (object instanceof DungeonView) {
                        this.allCardsIndex.put(object.getId(), new CardView((DungeonView)object));
                    }
                });
            });
        }

        public Set<UUID> getChosenTargets() {
            if (this.options != null && this.options.containsKey("chosenTargets")) {
                return (Set)((Object)this.options.get("chosenTargets"));
            }
            return Collections.emptySet();
        }

        public CardView findCard(UUID id) {
            return this.allCardsIndex.getOrDefault(id, null);
        }
    }

    private class skipButtonsList {
        private final skipButton turn;
        private final skipButton untilEndOfTurn;
        private final skipButton untilNextMain;
        private final skipButton allTurns;
        private final skipButton untilStackResolved;
        private final skipButton untilUntilEndStepBeforeMyTurn;

        skipButtonsList() {
            this.turn = new skipButton("Skip to next turn", "", "", "controlNextTurn");
            this.untilEndOfTurn = new skipButton("Skip to [EXTRA_TRUE / EXTRA_FALSE] END OF TURN step", "opponent", "next", "controlEndStep");
            this.untilNextMain = new skipButton("Skip to [EXTRA_TRUE / EXTRA_FALSE] MAIN step", "opponent", "next", "controlMainStep");
            this.allTurns = new skipButton("Skip to YOUR turn", "", "", "controlYourTurn");
            this.untilStackResolved = new skipButton("Skip until stack is resolved [EXTRA_TRUE]", "", "or stop on new objects added", "controlSkipStack");
            this.untilUntilEndStepBeforeMyTurn = new skipButton("Skip to END OF TURN before YOUR", "", "", "controlPriorEnd");
        }

        private void updateExtraMode(PlayerView player) {
            this.turn.setExtraMode(false);
            this.untilEndOfTurn.setExtraMode(player.getUserData().getUserSkipPrioritySteps().isStopOnAllEndPhases());
            this.untilNextMain.setExtraMode(player.getUserData().getUserSkipPrioritySteps().isStopOnAllMainPhases());
            this.allTurns.setExtraMode(false);
            this.untilStackResolved.setExtraMode(player.getUserData().getUserSkipPrioritySteps().isStopOnStackNewObjects());
            this.untilUntilEndStepBeforeMyTurn.setExtraMode(false);
        }

        private void updatePressState(PlayerView player) {
            this.turn.setPressState(player.isPassedTurn());
            this.untilEndOfTurn.setPressState(player.isPassedUntilEndOfTurn());
            this.untilNextMain.setPressState(player.isPassedUntilNextMain());
            this.allTurns.setPressState(player.isPassedAllTurns());
            this.untilStackResolved.setPressState(player.isPassedUntilStackResolved());
            this.untilUntilEndStepBeforeMyTurn.setPressState(player.isPassedUntilEndStepBeforeMyTurn());
        }

        public void updateFromPlayer(PlayerView player) {
            this.updateExtraMode(player);
            this.updatePressState(player);
        }

        private skipButton findButton(String hotkey) {
            switch (hotkey) {
                case "controlNextTurn": {
                    return this.turn;
                }
                case "controlEndStep": {
                    return this.untilEndOfTurn;
                }
                case "controlMainStep": {
                    return this.untilNextMain;
                }
                case "controlYourTurn": {
                    return this.allTurns;
                }
                case "controlSkipStack": {
                    return this.untilStackResolved;
                }
                case "controlPriorEnd": {
                    return this.untilUntilEndStepBeforeMyTurn;
                }
            }
            logger.error((Object)("Unknown hotkey name " + hotkey));
            return null;
        }

        public String getTooltip(String hotkey) {
            skipButton butt = this.findButton(hotkey);
            return butt != null ? butt.getTooltip() : "";
        }

        public Border getBorder(String hotkey) {
            skipButton butt = this.findButton(hotkey);
            return butt != null ? butt.getBorder() : BORDER_NON_ACTIVE;
        }

        public void activateSkipButton(String hotkey) {
            skipButton butt;
            this.turn.setPressState(false);
            this.untilEndOfTurn.setPressState(false);
            this.untilNextMain.setPressState(false);
            this.allTurns.setPressState(false);
            this.untilStackResolved.setPressState(false);
            this.untilUntilEndStepBeforeMyTurn.setPressState(false);
            if (!hotkey.isEmpty() && (butt = this.findButton(hotkey)) != null) {
                butt.setPressState(true);
            }
        }
    }

    public static class MageSplitter {
        JSplitPane splitPane;
        double defaultProportion;

        MageSplitter(JSplitPane splitPane, double defaultProportion) {
            this.splitPane = splitPane;
            this.defaultProportion = defaultProportion;
        }
    }

    private class skipButton {
        private final String text;
        private final String extraFalse;
        private final String extraTrue;
        private final String hotkeyName;
        private boolean extraMode = false;
        private boolean pressState = false;

        skipButton(String text, String extraFalse, String extraTrue, String hotkeyName) {
            this.text = text;
            this.extraFalse = extraFalse;
            this.extraTrue = extraTrue;
            this.hotkeyName = hotkeyName;
        }

        public void setExtraMode(boolean enable) {
            this.extraMode = enable;
        }

        public void setPressState(boolean enable) {
            this.pressState = enable;
        }

        public String getTooltip() {
            String res = "<html><b>" + PreferencesDialog.getCachedKeyText(this.hotkeyName) + "</b> - " + this.text;
            String mesTrue = this.extraTrue;
            String mesFalse = this.extraFalse;
            if (!this.extraTrue.isEmpty() || !this.extraFalse.isEmpty()) {
                if (this.extraMode) {
                    mesTrue = "<b>" + mesTrue + "</b>";
                } else {
                    mesFalse = "<b>" + mesFalse + "</b>";
                }
                res = res.replace("EXTRA_FALSE", mesFalse);
                res = res.replace("EXTRA_TRUE", mesTrue);
                res = res + " - adjust using preferences";
            }
            return res;
        }

        public Border getBorder() {
            return this.pressState ? BORDER_ACTIVE : BORDER_NON_ACTIVE;
        }
    }

    private static enum PopUpMenuType {
        TRIGGER_ORDER;

    }
}
