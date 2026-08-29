/*
 * Decompiled with CFR.
 */
package mage.client.cards;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mage.abilities.icon.CardIconRenderSettings;
import mage.cards.Card;
import mage.cards.ExpansionSet;
import mage.cards.MageCard;
import mage.cards.Sets;
import mage.cards.decks.DeckCardInfo;
import mage.cards.decks.DeckCardLayout;
import mage.cards.repository.CardCriteria;
import mage.cards.repository.CardInfo;
import mage.cards.repository.CardRepository;
import mage.client.cards.BigCard;
import mage.client.cards.CardDraggerGlassPane;
import mage.client.cards.CardEventProducer;
import mage.client.cards.CardEventSource;
import mage.client.cards.DragCardSource;
import mage.client.cards.DragCardTarget;
import mage.client.cards.ManaBarChart;
import mage.client.cards.ManaPieChart;
import mage.client.cards.SelectionBox;
import mage.client.constants.Constants;
import mage.client.dialog.PreferencesDialog;
import mage.client.plugins.impl.Plugins;
import mage.client.util.Event;
import mage.client.util.GUISizeHelper;
import mage.client.util.Listener;
import mage.client.util.comparators.CardViewCardTypeComparator;
import mage.client.util.comparators.CardViewColorComparator;
import mage.client.util.comparators.CardViewColorIdentityComparator;
import mage.client.util.comparators.CardViewComparator;
import mage.client.util.comparators.CardViewCostComparator;
import mage.client.util.comparators.CardViewEDHPowerLevelComparator;
import mage.client.util.comparators.CardViewNameComparator;
import mage.client.util.comparators.CardViewNoneComparator;
import mage.client.util.comparators.CardViewRarityComparator;
import mage.constants.CardType;
import mage.constants.Rarity;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.util.DebugUtil;
import mage.util.RandomUtil;
import mage.view.CardView;
import mage.view.CardsView;
import mage.view.SimpleCardView;
import org.apache.log4j.Logger;
import org.mage.card.arcane.CardRenderer;
import org.mage.card.arcane.ManaSymbols;
import org.mage.plugins.card.images.ImageCache;

public class DragCardGrid
extends JPanel
implements DragCardSource,
DragCardTarget,
CardEventProducer {
    private static final Logger logger = Logger.getLogger(DragCardGrid.class);
    private static final String DOUBLE_CLICK_MODE_INFO = "<html>Double click mode: <b>%s</b>";
    private Constants.DeckEditorMode mode;
    Listener<Event> cardListener;
    MouseListener countLabelListener;
    private final CardTypeCounter creatureCounter = new CardTypeCounter(){

        @Override
        protected boolean is(CardView card) {
            return card.isCreature();
        }
    };
    private final CardTypeCounter landCounter = new CardTypeCounter(){

        @Override
        protected boolean is(CardView card) {
            return card.isLand();
        }
    };
    private final CardTypeCounter artifactCounter = new CardTypeCounter(){

        @Override
        protected boolean is(CardView card) {
            return card.isArtifact();
        }
    };
    private final CardTypeCounter enchantmentCounter = new CardTypeCounter(){

        @Override
        protected boolean is(CardView card) {
            return card.isEnchantment();
        }
    };
    private final CardTypeCounter instantCounter = new CardTypeCounter(){

        @Override
        protected boolean is(CardView card) {
            return card.isInstant();
        }
    };
    private final CardTypeCounter sorceryCounter = new CardTypeCounter(){

        @Override
        protected boolean is(CardView card) {
            return card.isSorcery();
        }
    };
    private final CardTypeCounter planeswalkerCounter = new CardTypeCounter(){

        @Override
        protected boolean is(CardView card) {
            return card.isPlaneswalker();
        }
    };
    private final CardTypeCounter battleCounter = new CardTypeCounter(){

        @Override
        protected boolean is(CardView card) {
            return card.isBattle();
        }
    };
    private final CardTypeCounter kindredCounter = new CardTypeCounter(){

        @Override
        protected boolean is(CardView card) {
            return card.isKindred();
        }
    };
    private final CardTypeCounter[] allCounters = new CardTypeCounter[]{this.creatureCounter, this.landCounter, this.artifactCounter, this.enchantmentCounter, this.instantCounter, this.planeswalkerCounter, this.sorceryCounter, this.battleCounter, this.kindredCounter};
    private static final int DEFAULT_COUNT_LABEL_HEIGHT = 40;
    public static final int GRID_PADDING = 12;
    private static final ImageIcon INSERT_ROW_ICON = new ImageIcon(DragCardGrid.class.getClassLoader().getResource("editor_insert_row.png"));
    private static final ImageIcon INSERT_COL_ICON = new ImageIcon(DragCardGrid.class.getClassLoader().getResource("editor_insert_col.png"));
    private final Map<UUID, MageCard> cardViews = new LinkedHashMap<UUID, MageCard>();
    private final List<CardView> allCards = new ArrayList<CardView>();
    private final CardEventSource eventSource = new CardEventSource();
    BigCard lastBigCard = null;
    JButton sortButton;
    JButton filterButton;
    JButton visibilityButton;
    JButton selectByButton;
    JButton analyseButton;
    JButton blingButton;
    JButton oldVersionButton;
    JLabel mouseDoubleClickMode;
    final JPopupMenu filterPopup;
    JPopupMenu selectByTypePopup;
    final JPopupMenu sortPopup;
    final JPopupMenu selectByPopup;
    final JCheckBox separateCreaturesCb;
    final JTextField searchByTextField;
    JToggleButton multiplesButton;
    final JSlider cardSizeSlider;
    final JLabel cardSizeSliderLabel;
    final Map<Sort, AbstractButton> sortButtons = new EnumMap<Sort, AbstractButton>(Sort.class);
    final Map<CardType, AbstractButton> selectByTypeButtons = new EnumMap<CardType, AbstractButton>(CardType.class);
    final JLabel deckNameAndCountLabel;
    final JLabel landCountLabel;
    final JLabel creatureCountLabel;
    final JScrollPane cardScroll;
    JLayeredPane cardContent;
    final JLabel insertArrow;
    final SelectionBox selectionPanel;
    Set<CardView> selectionDragStartCards;
    int selectionDragStartX;
    int selectionDragStartY;
    float cardSizeMod = 1.0f;
    Role role = Role.MAINDECK;
    private final CardDraggerGlassPane dragger = new CardDraggerGlassPane(this);
    private List<List<List<CardView>>> cardGrid;
    private List<Integer> maxStackSize = new ArrayList<Integer>();
    private final List<List<JLabel>> stackCountLabels = new ArrayList<List<JLabel>>();
    private Sort cardSort = Sort.CMC;
    private final List<CardType> selectByTypeSelected = new ArrayList<CardType>();
    private boolean separateCreatures = true;
    private static final Pattern pattern = Pattern.compile(".*Add(.*)(\\{[WUBRGXC]\\})");
    private final ArrayList<DragCardGridListener> listeners = new ArrayList();

    @Override
    public Collection<CardView> dragCardList() {
        return this.allCards.stream().filter(SimpleCardView::isSelected).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void dragCardBegin() {
    }

    @Override
    public void dragCardEnd(DragCardTarget target) {
        if (target != this && target != null) {
            this.removeSelectedCards();
        }
    }

    @Override
    public void dragCardEnter(MouseEvent e) {
        this.insertArrow.setVisible(true);
    }

    @Override
    public void dragCardMove(MouseEvent e) {
        e = SwingUtilities.convertMouseEvent(this, e, this.cardContent);
        this.showDropPosition(e.getX(), e.getY());
    }

    @Override
    public CardEventSource getCardEventSource() {
        return this.eventSource;
    }

    private void showDropPosition(int x, int y) {
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        int cardWidth = this.getCardWidth();
        int cardHeight = this.getCardHeight();
        int cardTopHeight = CardRenderer.getCardTopHeight(cardWidth);
        int dx = x % (cardWidth + this.getGridPadding());
        int col = x / (cardWidth + this.getGridPadding());
        int gridWidth = this.cardGrid.isEmpty() ? 0 : this.cardGrid.get(0).size();
        int countLabelHeight = DragCardGrid.getCountLabelHeight();
        if (dx < this.getGridPadding() && col < gridWidth) {
            int curY = countLabelHeight;
            int rowIndex = 0;
            for (int i = 0; i < this.cardGrid.size(); ++i) {
                int maxStack = this.maxStackSize.get(i);
                int rowHeight = cardTopHeight * (maxStack - 1) + cardHeight;
                int rowBottom = curY + rowHeight + countLabelHeight;
                if (y < rowBottom) {
                    rowIndex = i;
                    break;
                }
                rowIndex = i + 1;
                curY = rowBottom;
            }
            this.insertArrow.setIcon(INSERT_COL_ICON);
            this.insertArrow.setSize(64, 64);
            this.insertArrow.setLocation((cardWidth + this.getGridPadding()) * col + this.getGridPadding() / 2 - 32, curY);
        } else {
            col = Math.min(col, gridWidth);
            int curY = countLabelHeight;
            int rowIndex = 0;
            int offsetIntoStack = 0;
            for (int i = 0; i < this.cardGrid.size(); ++i) {
                int maxStack = this.maxStackSize.get(i);
                int rowHeight = cardTopHeight * (maxStack - 1) + cardHeight;
                int rowBottom = curY + rowHeight + countLabelHeight;
                if (y < rowBottom) {
                    rowIndex = i;
                    offsetIntoStack = y - curY;
                    break;
                }
                rowIndex = i + 1;
                offsetIntoStack = y - rowBottom;
                curY = rowBottom;
            }
            List<CardView> stack = rowIndex < this.cardGrid.size() && col < this.cardGrid.get(0).size() ? this.cardGrid.get(rowIndex).get(col) : new ArrayList<>();
            int stackInsertIndex = (offsetIntoStack + cardTopHeight / 2) / cardTopHeight;
            stackInsertIndex = Math.max(0, Math.min(stackInsertIndex, stack.size()));
            this.insertArrow.setIcon(INSERT_ROW_ICON);
            this.insertArrow.setSize(64, 32);
            this.insertArrow.setLocation((cardWidth + this.getGridPadding()) * col + this.getGridPadding() + cardWidth / 2 - 32, curY + stackInsertIndex * cardTopHeight - 32);
        }
    }

    @Override
    public void dragCardExit(MouseEvent e) {
        this.insertArrow.setVisible(false);
    }

    @Override
    public void dragCardDrop(MouseEvent e, DragCardSource source, Collection<CardView> cards) {
        int rowIndex;
        int curY;
        e = SwingUtilities.convertMouseEvent(this, e, this.cardContent);
        int x = e.getX();
        int y = e.getY();
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        if (source == this) {
            for (List<List<CardView>> gridRow : this.cardGrid) {
                for (List<CardView> stack : gridRow) {
                    for (int i = 0; i < stack.size(); ++i) {
                        if (!cards.contains(stack.get(i))) continue;
                        stack.set(i, null);
                    }
                }
            }
        }
        int cardWidth = this.getCardWidth();
        int cardHeight = this.getCardHeight();
        int cardTopHeight = CardRenderer.getCardTopHeight(cardWidth);
        int dx = x % (cardWidth + this.getGridPadding());
        int col = x / (cardWidth + this.getGridPadding());
        int gridWidth = this.cardGrid.isEmpty() ? 0 : this.cardGrid.get(0).size();
        int countLabelHeight = DragCardGrid.getCountLabelHeight();
        if (dx < this.getGridPadding() && col < gridWidth) {
            int i;
            curY = countLabelHeight;
            rowIndex = 0;
            for (i = 0; i < this.cardGrid.size(); ++i) {
                int maxStack = this.maxStackSize.get(i);
                int rowHeight = cardTopHeight * (maxStack - 1) + cardHeight;
                int rowBottom = curY + rowHeight + countLabelHeight;
                if (y < rowBottom) {
                    rowIndex = i;
                    break;
                }
                rowIndex = i + 1;
                curY = rowBottom;
            }
            if (rowIndex >= this.cardGrid.size()) {
                ArrayList newRow = new ArrayList();
                if (!this.cardGrid.isEmpty()) {
                    for (int colIndex = 0; colIndex < this.cardGrid.get(0).size(); ++colIndex) {
                        newRow.add(new ArrayList());
                    }
                }
                this.cardGrid.add(newRow);
                this.maxStackSize.add(0);
            }
            for (i = 0; i < this.cardGrid.size(); ++i) {
                this.cardGrid.get(i).add(col, new ArrayList());
            }
            this.cardGrid.get(rowIndex).get(col).addAll(cards);
        } else {
            col = Math.min(col, gridWidth);
            curY = countLabelHeight;
            rowIndex = 0;
            int offsetIntoStack = 0;
            for (int i = 0; i < this.cardGrid.size(); ++i) {
                int maxStack = this.maxStackSize.get(i);
                int rowHeight = cardTopHeight * (maxStack - 1) + cardHeight;
                int rowBottom = curY + rowHeight + countLabelHeight;
                if (y < rowBottom) {
                    rowIndex = i;
                    offsetIntoStack = y - curY;
                    break;
                }
                rowIndex = i + 1;
                offsetIntoStack = y - rowBottom;
                curY = rowBottom;
            }
            if (rowIndex >= this.cardGrid.size()) {
                ArrayList newRow = new ArrayList();
                if (!this.cardGrid.isEmpty()) {
                    for (int colIndex = 0; colIndex < this.cardGrid.get(0).size(); ++colIndex) {
                        newRow.add(new ArrayList());
                    }
                }
                this.cardGrid.add(newRow);
                this.maxStackSize.add(0);
            }
            if (col >= this.cardGrid.get(0).size()) {
                for (int i = 0; i < this.cardGrid.size(); ++i) {
                    this.cardGrid.get(i).add(new ArrayList());
                }
            }
            List<CardView> stack = this.cardGrid.get(rowIndex).get(col);
            int stackInsertIndex = (offsetIntoStack + cardTopHeight / 2) / cardTopHeight;
            stackInsertIndex = Math.max(0, Math.min(stackInsertIndex, stack.size()));
            stack.addAll(stackInsertIndex, cards);
        }
        if (source == this) {
            this.trimGrid();
            this.layoutGrid();
            this.repaintGrid();
        } else {
            for (CardView card : cards) {
                card.setSelected(true);
                this.addCardView(card);
                this.eventSource.fireEventDeckCardAdded((SimpleCardView)card, null);
            }
            this.layoutGrid();
            this.repaintGrid();
        }
    }

    public void changeGUISize() {
        Font countLabelFont = DragCardGrid.getCountLabelFont();
        this.stackCountLabels.stream().flatMap(Collection::stream).forEach(label -> label.setFont(countLabelFont));
        this.layoutGrid();
        this.cardScroll.getVerticalScrollBar().setUnitIncrement(CardRenderer.getCardTopHeight(this.getCardWidth()));
        this.repaintGrid();
    }

    public void cleanUp() {
        for (MageCard cardView : this.cardViews.values()) {
            this.cardContent.remove((Component)cardView);
        }
        this.cardGrid.clear();
        this.maxStackSize.clear();
        this.allCards.clear();
        this.lastBigCard = null;
        this.clearCardEventListeners();
    }

    public void addCardEventListener(Listener<Event> listener) {
        this.eventSource.addListener(listener);
    }

    public void clearCardEventListeners() {
        this.eventSource.clearListeners();
    }

    public void setRole(Role role) {
        this.role = role;
        if (role == Role.SIDEBOARD) {
            this.creatureCountLabel.setVisible(false);
            this.landCountLabel.setVisible(false);
            this.cardSizeSliderLabel.setVisible(false);
            this.mouseDoubleClickMode.setVisible(false);
        } else {
            this.creatureCountLabel.setVisible(true);
            this.landCountLabel.setVisible(true);
            this.cardSizeSliderLabel.setVisible(true);
            this.mouseDoubleClickMode.setVisible(true);
        }
        this.updateCounts();
    }

    private void updateMouseDoubleClicksInfo(boolean isHotKeyPressed) {
        boolean gameMode = isHotKeyPressed || this.mode != Constants.DeckEditorMode.FREE_BUILDING;
        String oldText = this.mouseDoubleClickMode.getText();
        String newText = String.format(DOUBLE_CLICK_MODE_INFO, gameMode ? "MOVE" : "DELETE");
        if (!oldText.equals(newText)) {
            this.mouseDoubleClickMode.setText(newText);
        }
    }

    public void removeSelectedCards() {
        List<CardView> cardsToRemove = this.allCards.stream().filter(SimpleCardView::isSelected).collect(Collectors.toList());
        this.removeCards(cardsToRemove);
    }

    private void removeCards(List<CardView> cardsToRemove) {
        cardsToRemove.forEach(cardToRemove -> {
            for (List<List<CardView>> gridRow : this.cardGrid) {
                block1: for (List<CardView> stack : gridRow) {
                    for (int i = 0; i < stack.size(); ++i) {
                        CardView card = stack.get(i);
                        if (card == null || !card.equals(cardToRemove)) continue;
                        this.eventSource.fireEventDeckCardRemoved((SimpleCardView)card);
                        stack.set(i, null);
                        this.removeCardView(card);
                        continue block1;
                    }
                }
            }
        });
        this.trimGrid();
        this.layoutGrid();
        this.repaintGrid();
    }

    public DeckCardLayout getCardLayout() {
        ArrayList<List<List<DeckCardInfo>>> info = new ArrayList<List<List<DeckCardInfo>>>();
        for (List<List<CardView>> gridRow : this.cardGrid) {
            ArrayList row = new ArrayList();
            info.add(row);
            for (List<CardView> stack : gridRow) {
                row.add(stack.stream().map(card -> new DeckCardInfo(card.getName(), card.getCardNumber(), card.getExpansionSetCode())).collect(Collectors.toList()));
            }
        }
        return new DeckCardLayout(info, this.saveSettings().toString());
    }

    public void setDeckEditorMode(Constants.DeckEditorMode mode) {
        this.mode = mode;
        this.updateMouseDoubleClicksInfo(false);
    }

    public Settings saveSettings() {
        Settings s = new Settings();
        s.sort = this.cardSort;
        s.separateCreatures = this.separateCreatures;
        s.cardSize = this.cardSizeSlider.getValue();
        return s;
    }

    public void loadSettings(Settings s) {
        if (s != null) {
            this.setSort(s.sort);
            this.setSeparateCreatures(s.separateCreatures);
            this.setCardSize(s.cardSize);
            this.resort();
        }
    }

    public void setSeparateCreatures(boolean state) {
        this.separateCreatures = state;
        this.separateCreaturesCb.setSelected(state);
    }

    public void setSort(Sort s) {
        this.cardSort = s;
        this.sortButtons.get((Object)s).setSelected(true);
    }

    public void setCardSize(int size) {
        this.cardSizeSlider.setValue(size);
    }

    public DragCardGrid() {
        JToggleButton button;
        this.cardGrid = new ArrayList<List<List<CardView>>>();
        this.setLayout(new BorderLayout());
        this.setOpaque(false);
        this.mode = Constants.DeckEditorMode.LIMITED_BUILDING;
        this.cardContent = new JLayeredPane();
        this.cardContent.setLayout(null);
        this.cardContent.setOpaque(false);
        this.cardListener = event -> {
            switch (event.getEventType()) {
                case CARD_POPUP_MENU: {
                    if (event.getSource() == null) break;
                    CardView card = (CardView)event.getSource();
                    MouseEvent me = event.getMouseEvent();
                    if (!card.isSelected()) {
                        this.selectCard(card);
                    }
                    this.showCardRightClickMenu(card, me);
                    break;
                }
                case CARD_CLICK: {
                    if (event.getSource() == null) break;
                    CardView card = (CardView)event.getSource();
                    MouseEvent me = event.getMouseEvent();
                    this.cardClicked(card, me);
                }
            }
        };
        this.eventSource.addListener(this.cardListener);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher(){

            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getKeyCode() == 18) {
                    DragCardGrid.this.updateMouseDoubleClicksInfo(e.isAltDown());
                }
                return false;
            }
        });
        this.cardContent.addMouseListener(new MouseAdapter(){
            private boolean isDragging = false;

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                this.isDragging = true;
                DragCardGrid.this.beginSelectionDrag(e.getX(), e.getY(), e.isShiftDown());
                DragCardGrid.this.updateSelectionDrag(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (this.isDragging) {
                    this.isDragging = false;
                    DragCardGrid.this.updateSelectionDrag(e.getX(), e.getY());
                    DragCardGrid.this.endSelectionDrag(e.getX(), e.getY());
                }
            }
        });
        this.cardContent.addMouseMotionListener(new MouseAdapter(){

            @Override
            public void mouseDragged(MouseEvent e) {
                DragCardGrid.this.updateSelectionDrag(e.getX(), e.getY());
            }
        });
        this.cardScroll = new JScrollPane(this.cardContent, 20, 30);
        this.cardScroll.setOpaque(false);
        this.cardScroll.getViewport().setOpaque(false);
        this.cardScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        this.cardScroll.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
        this.cardScroll.getVerticalScrollBar().setUnitIncrement(CardRenderer.getCardTopHeight(this.getCardWidth()));
        this.add((Component)this.cardScroll, "Center");
        this.sortButton = new JButton("Sort");
        this.filterButton = new JButton("Filter");
        this.visibilityButton = new JButton("V");
        this.selectByButton = new JButton("Select By");
        this.analyseButton = new JButton("M");
        this.blingButton = new JButton("B");
        this.oldVersionButton = new JButton("O");
        this.mouseDoubleClickMode = new JLabel(DOUBLE_CLICK_MODE_INFO);
        this.mouseDoubleClickMode.setToolTipText("<html>Mouse modes for double clicks:<br> * &lt;Double Click&gt;: <b>DELETE</b> card from mainboard/sideboard (it works as <b>MOVE</b> in games);<br> * &lt;ALT + Double Click&gt;: <b>MOVE</b> card between mainboard/sideboard (default for games);<br> * Deck editor: use &lt;ALT + Double Click&gt; on cards list to add card to the sideboard instead mainboard.");
        this.updateMouseDoubleClicksInfo(false);
        this.deckNameAndCountLabel = new JLabel();
        this.landCountLabel = new JLabel("", new ImageIcon(this.getClass().getResource("/buttons/type_land.png")), 2);
        this.landCountLabel.setToolTipText("Number of lands in deck");
        this.creatureCountLabel = new JLabel("", new ImageIcon(this.getClass().getResource("/buttons/type_creatures.png")), 2);
        this.creatureCountLabel.setToolTipText("Number of creatures in deck");
        JPanel toolbar = new JPanel(new BorderLayout());
        JPanel toolbarInner = new JPanel();
        toolbar.setBackground(PreferencesDialog.getCurrentTheme().getDeckEditorToolbarBackgroundColor());
        toolbar.setOpaque(true);
        toolbarInner.setOpaque(false);
        toolbarInner.add(this.deckNameAndCountLabel);
        toolbarInner.add(this.landCountLabel);
        toolbarInner.add(this.creatureCountLabel);
        toolbarInner.add(this.sortButton);
        toolbarInner.add(this.filterButton);
        toolbarInner.add(this.selectByButton);
        toolbarInner.add(this.visibilityButton);
        toolbarInner.add(this.analyseButton);
        toolbarInner.add(this.blingButton);
        toolbarInner.add(this.oldVersionButton);
        toolbarInner.add(this.mouseDoubleClickMode);
        toolbar.add((Component)toolbarInner, "West");
        JPanel sliderPanel = new JPanel(new GridBagLayout());
        sliderPanel.setOpaque(false);
        this.cardSizeSlider = new JSlider(0, 0, 100, 50);
        this.cardSizeSlider.setOpaque(false);
        this.cardSizeSlider.setPreferredSize(new Dimension(100, (int)this.cardSizeSlider.getPreferredSize().getHeight()));
        this.cardSizeSlider.addChangeListener(e -> {
            if (!this.cardSizeSlider.getValueIsAdjusting()) {
                float sliderFrac = (float)(this.cardSizeSlider.getValue() - 50) / 50.0f;
                this.cardSizeMod = (float)Math.pow(2.0, sliderFrac);
                this.layoutGrid();
                this.repaintGrid();
            }
        });
        this.cardSizeSliderLabel = new JLabel("Card size:");
        sliderPanel.add(this.cardSizeSliderLabel);
        sliderPanel.add(this.cardSizeSlider);
        toolbar.add((Component)sliderPanel, "East");
        this.add((Component)toolbar, "North");
        this.insertArrow = new JLabel();
        this.insertArrow.setSize(20, 20);
        this.insertArrow.setVisible(false);
        this.cardContent.add((Component)this.insertArrow, (Object)1000);
        this.selectionPanel = new SelectionBox();
        this.selectionPanel.setVisible(false);
        this.cardContent.add((Component)this.selectionPanel, (Object)1001);
        this.separateCreatures = PreferencesDialog.getCachedValue("deckEditorLastSeparateCreatures", "false").equals("true");
        try {
            this.cardSort = Sort.valueOf(PreferencesDialog.getCachedValue("deckEditorLastSort", Sort.NONE.toString()));
        }
        catch (IllegalArgumentException ex) {
            this.cardSort = Sort.NONE;
        }
        this.sortPopup = new JPopupMenu();
        this.sortPopup.setLayout(new GridBagLayout());
        JPanel sortMode = new JPanel();
        sortMode.setLayout(new GridLayout(Sort.values().length, 1, 0, 2));
        sortMode.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Sort by..."));
        GridBagConstraints sortModeC = new GridBagConstraints();
        sortModeC.gridx = 0;
        sortModeC.gridy = 0;
        sortModeC.gridwidth = 1;
        sortModeC.gridheight = 1;
        sortModeC.fill = 2;
        this.sortPopup.add((Component)sortMode, sortModeC);
        ButtonGroup sortModeGroup = new ButtonGroup();
        Sort[] sortArray = Sort.values();
        int n = sortArray.length;
        for (int i = 0; i < n; ++i) {
            Sort s = sortArray[i];
            button = new JToggleButton(s.getText());
            if (s == this.cardSort) {
                button.setSelected(true);
            }
            this.sortButtons.put(s, button);
            sortMode.add(button);
            sortModeGroup.add(button);
            button.addActionListener(e -> {
                this.cardSort = s;
                PreferencesDialog.saveValue("deckEditorLastSort", s.toString());
                this.resort();
            });
        }
        JPanel sortOptions = new JPanel();
        sortOptions.setLayout(new BoxLayout((Container)sortOptions, 1));
        sortOptions.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Sort options"));
        GridBagConstraints sortOptionsC = new GridBagConstraints();
        sortOptionsC.gridx = 0;
        sortOptionsC.gridy = 1;
        sortOptionsC.gridwidth = 1;
        sortOptionsC.gridheight = 1;
        this.sortPopup.add((Component)sortOptions, sortOptionsC);
        this.separateCreaturesCb = new JCheckBox();
        this.separateCreaturesCb.setText("Puts creatures in separate first row");
        this.separateCreaturesCb.setSelected(this.separateCreatures);
        this.separateCreaturesCb.addItemListener(e -> {
            this.setSeparateCreatures(this.separateCreaturesCb.isSelected());
            PreferencesDialog.saveValue("deckEditorLastSeparateCreatures", Boolean.toString(this.separateCreatures));
            this.resort();
        });
        sortOptions.add(this.separateCreaturesCb);
        this.sortPopup.pack();
        DragCardGrid.makeButtonPopup(this.sortButton, this.sortPopup);
        final JPopupMenu visPopup = new JPopupMenu();
        JMenuItem hideSelected = new JMenuItem("Hide selected");
        hideSelected.addActionListener(e -> this.hideSelection());
        visPopup.add(hideSelected);
        JMenuItem showAll = new JMenuItem("Show all");
        showAll.addActionListener(e -> this.showAll());
        visPopup.add(showAll);
        this.visibilityButton.setToolTipText("Visibility of cards.  Right click to get the same options this provides");
        this.visibilityButton.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                visPopup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });
        this.selectByPopup = new JPopupMenu();
        this.selectByPopup.setLayout(new GridBagLayout());
        JPanel selectByTypeMode = new JPanel();
        selectByTypeMode.setLayout(new GridLayout(CardType.values().length, 1, 0, 2));
        selectByTypeMode.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Select by Type"));
        GridBagConstraints selectByTypeModeC = new GridBagConstraints();
        selectByTypeModeC.gridx = 0;
        selectByTypeModeC.gridy = 0;
        selectByTypeModeC.gridwidth = 1;
        selectByTypeModeC.gridheight = 1;
        selectByTypeModeC.fill = 2;
        this.selectByPopup.add((Component)selectByTypeMode, selectByTypeModeC);
        ButtonGroup selectByTypeModeGroup = new ButtonGroup();
        for (CardType cardType : CardType.values()) {
            if (cardType == CardType.CONSPIRACY) {
                this.multiplesButton = new JToggleButton("Multiples");
                this.selectByTypeButtons.put(cardType, this.multiplesButton);
                selectByTypeMode.add(this.multiplesButton);
                selectByTypeModeGroup.add(this.multiplesButton);
                this.multiplesButton.addActionListener(e -> {
                    this.multiplesButton.setSelected(!this.multiplesButton.isSelected());
                    this.reselectBy();
                });
                continue;
            }
            button = new JToggleButton(cardType.toString());
            this.selectByTypeButtons.put(cardType, button);
            selectByTypeMode.add(button);
            selectByTypeModeGroup.add(button);
            button.addActionListener(e -> {
                button.setSelected(!button.isSelected());
                this.reselectBy();
            });
        }
        JPanel selectBySearchPanel = new JPanel();
        selectBySearchPanel.setPreferredSize(new Dimension(150, 60));
        selectBySearchPanel.setLayout(new GridLayout(1, 1));
        selectBySearchPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Search:"));
        GridBagConstraints selectBySearchPanelC = new GridBagConstraints();
        selectBySearchPanelC.gridx = 0;
        selectBySearchPanelC.gridy = 1;
        selectBySearchPanelC.gridwidth = 1;
        selectBySearchPanelC.gridheight = 1;
        selectBySearchPanelC.fill = 2;
        selectBySearchPanelC.fill = 3;
        this.searchByTextField = new JTextField();
        this.searchByTextField.setToolTipText("Search cards by any data like name or mana symbols like {W}, {U}, {C}, etc (use quotes for exact search)");
        this.searchByTextField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                DragCardGrid.this.reselectBy();
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
        selectBySearchPanel.add(this.searchByTextField);
        this.selectByPopup.add((Component)selectBySearchPanel, selectBySearchPanelC);
        DragCardGrid.makeButtonPopup(this.selectByButton, this.selectByPopup);
        this.analyseButton.setToolTipText("Mana Analyser! Counts coloured/colourless mana costs. Counts land types.");
        this.analyseButton.addActionListener(evt -> this.analyseDeck());
        this.blingButton.setToolTipText("Bling your deck! Select the original and added cards by selecting 'Multiples' in the selection options");
        this.blingButton.addActionListener(evt -> this.blingDeck());
        this.oldVersionButton.setToolTipText("Switch cards to the oldest non-promo printing");
        this.oldVersionButton.addActionListener(evt -> this.oldVersionDeck());
        this.filterPopup = new JPopupMenu();
        this.filterPopup.setPreferredSize(new Dimension(300, 300));
        DragCardGrid.makeButtonPopup(this.filterButton, this.filterPopup);
        this.filterButton.setVisible(false);
        this.initCardAreaPopup();
        this.updateCounts();
    }

    public void initCardAreaPopup() {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem hideSelected = new JMenuItem("Hide selected");
        hideSelected.addActionListener(e -> this.hideSelection());
        menu.add(hideSelected);
        JMenuItem showAll = new JMenuItem("Show all");
        showAll.addActionListener(e -> this.showAll());
        menu.add(showAll);
        JMenu sortMenu = new JMenu("Sort by...");
        final LinkedHashMap<Sort, JCheckBoxMenuItem> sortMenuItems = new LinkedHashMap<Sort, JCheckBoxMenuItem>();
        for (Sort sort : Sort.values()) {
            JCheckBoxMenuItem subSort = new JCheckBoxMenuItem(sort.getText());
            sortMenuItems.put(sort, subSort);
            subSort.addActionListener(e -> {
                this.cardSort = sort;
                this.resort();
            });
            sortMenu.add(subSort);
        }
        sortMenu.add(new JPopupMenu.Separator());
        final JCheckBoxMenuItem separateButton = new JCheckBoxMenuItem("Separate creatures");
        separateButton.addActionListener(e -> {
            this.setSeparateCreatures(!this.separateCreatures);
            this.resort();
        });
        sortMenu.add(separateButton);
        menu.add(sortMenu);
        this.cardContent.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    for (Sort s : sortMenuItems.keySet()) {
                        ((JMenuItem)sortMenuItems.get((Object)s)).setSelected(DragCardGrid.this.cardSort == s);
                    }
                    hideSelected.setEnabled(!DragCardGrid.this.dragCardList().isEmpty());
                    separateButton.setSelected(DragCardGrid.this.separateCreatures);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    public void deselectAll() {
        for (List<List<CardView>> gridRow : this.cardGrid) {
            for (List<CardView> stack : gridRow) {
                for (CardView card : stack) {
                    if (!card.isSelected()) continue;
                    card.setSelected(false);
                    this.cardViews.get(card.getId()).update(card);
                }
            }
        }
    }

    public void selectByName(List<String> cardNames) {
        for (List<List<CardView>> gridRow : this.cardGrid) {
            for (List<CardView> stack : gridRow) {
                for (CardView card : stack) {
                    if (!cardNames.contains(card.getName())) continue;
                    card.setSelected(true);
                    this.cardViews.get(card.getId()).update(card);
                }
            }
        }
    }

    private void hideSelection() {
        Collection<CardView> toHide = this.dragCardList();
        for (DragCardGridListener l : this.listeners) {
            l.hideCards(toHide);
        }
    }

    private void duplicateSelection() {
        Collection<CardView> toDuplicate = this.dragCardList();
        for (DragCardGridListener l : this.listeners) {
            l.duplicateCards(toDuplicate);
        }
    }

    private void invertSelection() {
        List<CardView> toInvert = this.allCards;
        for (DragCardGridListener l : this.listeners) {
            l.invertCardSelection(toInvert);
            for (CardView card : this.allCards) {
                MageCard view = this.cardViews.get(card.getId());
                view.update(card);
            }
        }
        this.repaint();
    }

    private void chooseMatching() {
        Collection<CardView> toMatch = this.dragCardList();
        for (DragCardGridListener l : this.listeners) {
            for (CardView card : this.allCards) {
                for (CardView aMatch : toMatch) {
                    if (!card.getName().equals(aMatch.getName())) continue;
                    card.setSelected(true);
                    this.cardViews.get(card.getId()).update(card);
                }
            }
        }
        this.repaint();
    }

    private void showAll() {
        for (DragCardGridListener l : this.listeners) {
            l.showAll();
        }
    }

    private void beginSelectionDrag(int x, int y, boolean shiftHeld) {
        this.selectionPanel.setVisible(true);
        this.selectionPanel.setLocation(x, y);
        this.cardScroll.revalidate();
        this.selectionDragStartX = x;
        this.selectionDragStartY = y;
        this.selectionDragStartCards = new HashSet<CardView>();
        if (shiftHeld) {
            this.selectionDragStartCards.addAll(this.dragCardList());
        }
        this.notifyCardsSelected();
    }

    private void updateSelectionDrag(int x, int y) {
        int countLabelHeight;
        int cardWidth = this.getCardWidth();
        int cardHeight = this.getCardHeight();
        int cardTopHeight = CardRenderer.getCardTopHeight(cardWidth);
        int x1 = Math.min(x, this.selectionDragStartX);
        int x2 = Math.max(x, this.selectionDragStartX);
        int y1 = Math.min(y, this.selectionDragStartY);
        int y2 = Math.max(y, this.selectionDragStartY);
        this.selectionPanel.setLocation(x1, y1);
        this.selectionPanel.setSize(x2 - x1, y2 - y1);
        int col1 = x1 / (cardWidth + this.getGridPadding());
        int col2 = x2 / (cardWidth + this.getGridPadding());
        int offsetIntoCol2 = x2 % (cardWidth + this.getGridPadding());
        if (offsetIntoCol2 < this.getGridPadding()) {
            --col2;
        }
        int curY = countLabelHeight = DragCardGrid.getCountLabelHeight();
        for (int rowIndex = 0; rowIndex < this.cardGrid.size(); ++rowIndex) {
            int stackStartIndex = y1 < curY ? 0 : (y1 - curY) / cardTopHeight;
            int stackEndIndex = y2 < curY ? -1 : (y2 - curY) / cardTopHeight;
            List<List<CardView>> gridRow = this.cardGrid.get(rowIndex);
            for (int col = 0; col < gridRow.size(); ++col) {
                List<CardView> stack = gridRow.get(col);
                int stackBottomBegin = curY + cardTopHeight * stack.size();
                int stackBottomEnd = curY + cardTopHeight * (stack.size() - 1) + cardHeight;
                for (int i = 0; i < stack.size(); ++i) {
                    boolean inSeletionDrag;
                    CardView card = stack.get(i);
                    MageCard view = this.cardViews.get(card.getId());
                    boolean inBoundsX = col >= col1 && col <= col2;
                    boolean inBoundsY = i >= stackStartIndex && i <= stackEndIndex;
                    boolean lastCard = i == stack.size() - 1;
                    boolean bl = inSeletionDrag = inBoundsX && (inBoundsY || lastCard && y2 >= stackBottomBegin && y1 <= stackBottomEnd);
                    if (inSeletionDrag || this.selectionDragStartCards != null && this.selectionDragStartCards.contains(card)) {
                        if (card.isSelected()) continue;
                        card.setSelected(true);
                        view.update(card);
                        continue;
                    }
                    if (!card.isSelected()) continue;
                    card.setSelected(false);
                    view.update(card);
                }
            }
            curY += cardTopHeight * (this.maxStackSize.get(rowIndex) - 1) + cardHeight + countLabelHeight;
        }
    }

    private void endSelectionDrag(int x, int y) {
        this.selectionPanel.setVisible(false);
    }

    public void resort() {
        for (List<List<CardView>> gridRow : this.cardGrid) {
            for (List<CardView> stack : gridRow) {
                stack.clear();
            }
        }
        this.trimGrid();
        this.allCards.sort(new CardViewNameComparator());
        for (CardView card : this.allCards) {
            this.sortIntoGrid(card, null);
        }
        this.trimGrid();
        this.deselectAll();
        this.layoutGrid();
        this.repaintGrid();
    }

    public void reselectBy() {
        // Deselect everything
        deselectAll();

        boolean useText = false;
        String searchStr = "";
        if (searchByTextField.getText().length() >= 3) {
            useText = true;
            searchStr = searchByTextField.getText().toLowerCase(Locale.ENGLISH);
        }

        for (CardType cardType : selectByTypeButtons.keySet()) {
            AbstractButton button = selectByTypeButtons.get(cardType);
            if (button != null) {
                if (button.isSelected()) {
                    // Special case - "Multiples"  (CONSPIRACY type)
                    if (cardType == CardType.CONSPIRACY) {
                        Map<String, CardView> cardNames = new HashMap<>();

                        for (List<List<CardView>> gridRow : cardGrid) {
                            for (List<CardView> stack : gridRow) {
                                for (CardView card : stack) {
                                    if (cardNames.get(card.getName()) == null) {
                                        cardNames.put(card.getName(), card);
                                    } else {
                                        card.setSelected(true);
                                        cardViews.get(card.getId()).update(card);

                                        CardView origCard = cardNames.get(card.getName());
                                        origCard.setSelected(true);
                                        cardViews.get(origCard.getId()).update(origCard);
                                    }
                                }
                            }
                        }
                        continue;
                    }

                    for (List<List<CardView>> gridRow : cardGrid) {
                        for (List<CardView> stack : gridRow) {
                            for (CardView card : stack) {
                                boolean s = card.isSelected() || card.getCardTypes().contains(cardType);
                                card.setSelected(s);
                                cardViews.get(card.getId()).update(card);
                            }
                        }
                    }
                }
            }
        }

        if (useText) {
            for (List<List<CardView>> gridRow : cardGrid) {
                for (List<CardView> stack : gridRow) {
                    for (CardView card : stack) {
                        boolean s = card.isSelected();
                        // Name
                        if (!s) {
                            s = card.getName().toLowerCase(Locale.ENGLISH).contains(searchStr);
                        }
                        // Sub & Super Types
                        if (!s) {
                            for (SuperType str : card.getSuperTypes()) {
                                s |= str.toString().toLowerCase(Locale.ENGLISH).contains(searchStr);
                            }
                            for (SubType str : card.getSubTypes()) {
                                s |= str.toString().toLowerCase(Locale.ENGLISH).contains(searchStr);
                            }
                        }
                        // Rarity
                        if (!s) {
                            Rarity r = card.getRarity();
                            if (r != null) {
                                s |= r.toString().toLowerCase(Locale.ENGLISH).contains(searchStr);
                            }
                        }
                        // Type line
                        if (!s) {
                            String t = "";
                            for (CardType type : card.getCardTypes()) {
                                t += ' ' + type.toString();
                            }
                            s |= t.toLowerCase(Locale.ENGLISH).contains(searchStr);
                        }
                        // Casting cost
                        if (!s) {
                            s |= card.getManaCostStr().toLowerCase(Locale.ENGLISH).contains(searchStr);
                        }
                        // Rules
                        if (!s) {
                            for (String str : card.getRules()) {
                                s |= str.toLowerCase(Locale.ENGLISH).contains(searchStr);
                            }
                        }
                        card.setSelected(s);
                        cardViews.get(card.getId()).update(card);
                    }
                }
            }
        }

        // And finally rerender
        layoutGrid();
        repaintGrid();
    }

    public void analyseDeck() {
        HashMap<String, Integer> qtys = new HashMap<String, Integer>();
        HashMap<String, Integer> pips = new HashMap<String, Integer>();
        HashMap<String, Integer> pips_at_cmcs = new HashMap<String, Integer>();
        HashMap sourcePips = new HashMap();
        HashMap<String, Integer> manaCounts = new HashMap<String, Integer>();
        pips.put("#w}", 0);
        pips.put("#u}", 0);
        pips.put("#b}", 0);
        pips.put("#r}", 0);
        pips.put("#g}", 0);
        pips.put("#c}", 0);
        qtys.put("plains", 0);
        qtys.put("island", 0);
        qtys.put("swamp", 0);
        qtys.put("mountain", 0);
        qtys.put("forest", 0);
        qtys.put("basic", 0);
        qtys.put("wastes", 0);
        manaCounts = new HashMap();
        for (List<List<CardView>> gridRow : this.cardGrid) {
            for (List<CardView> stack : gridRow) {
                for (CardView card : stack) {
                    String t = card.getCardTypes().stream().map(CardType::toString).collect(Collectors.joining(" "));
                    t = t + card.getSuperTypes().stream().map(st -> st.toString().toLowerCase(Locale.ENGLISH)).collect(Collectors.joining(" "));
                    t = t + card.getSubTypes().stream().map(st -> st.toString().toLowerCase(Locale.ENGLISH)).collect(Collectors.joining(" "));
                    for (String qty : qtys.keySet()) {
                        int value = (Integer)qtys.get(qty);
                        if (t.toLowerCase(Locale.ENGLISH).contains(qty)) {
                            qtys.put(qty, ++value);
                        }
                        for (String str : card.getRules()) {
                            if (!str.toLowerCase(Locale.ENGLISH).contains(qty)) continue;
                            qtys.put(qty, ++value);
                        }
                    }
                    if (card.getName().equals("Wastes")) {
                        int value = (Integer)qtys.get("wastes");
                        qtys.put("wastes", ++value);
                    }
                    String mc = card.getManaCostStr();
                    mc = mc.replaceAll("\\{([WUBRG]).([WUBRG])\\}", "{$1}{$2}");
                    mc = mc.replaceAll("\\{", "#");
                    mc = mc.replaceAll("#2\\/", "#");
                    mc = mc.replaceAll("p}", "}");
                    mc = mc.toLowerCase(Locale.ENGLISH);
                    int cmc = card.getManaValue();
                    Pattern regex = Pattern.compile("#([0-9]+)}");
                    Matcher regexMatcher = regex.matcher(mc);
                    while (regexMatcher.find()) {
                        String val = regexMatcher.group(1);
                        int colorless_val = Integer.parseInt(val);
                        int total_c_pip = 0;
                        if (pips.get("#c}") != null) {
                            total_c_pip = (Integer)pips.get("#c}");
                        }
                        pips.put("#c}", colorless_val + total_c_pip);
                        int cmc_pip_value = 0;
                        if (pips_at_cmcs.get(cmc + "##c}") != null) {
                            cmc_pip_value = (Integer)pips_at_cmcs.get(cmc + "##c}");
                        }
                        pips_at_cmcs.put(cmc + "##c}", colorless_val + cmc_pip_value);
                    }
                    for (String pip : pips.keySet()) {
                        int value = (Integer)pips.get(pip);
                        while (mc.toLowerCase(Locale.ENGLISH).contains(pip)) {
                            pips.put(pip, ++value);
                            int pip_value = 0;
                            if (pips_at_cmcs.get(cmc + "#" + pip) != null) {
                                pip_value = (Integer)pips_at_cmcs.get(cmc + "#" + pip);
                            }
                            pips_at_cmcs.put(cmc + "#" + pip, ++pip_value);
                            mc = mc.replaceFirst(pip, "");
                        }
                    }
                    for (String str : card.getRules()) {
                        Matcher m = pattern.matcher(str);
                        while (m.find()) {
                            str = "Add" + m.group(1);
                            int num = 1;
                            if (manaCounts.get(m.group(2)) != null) {
                                num = (Integer)manaCounts.get(m.group(2));
                                ++num;
                            }
                            manaCounts.put(m.group(2), num);
                            m = pattern.matcher(str);
                        }
                    }
                }
            }
        }
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 0));
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, 1));
        ManaPieChart chart = new ManaPieChart((Integer)pips.get("#w}"), (Integer)pips.get("#u}"), (Integer)pips.get("#b}"), (Integer)pips.get("#r}"), (Integer)pips.get("#g}"), (Integer)pips.get("#c}"));
        chart.setMinimumSize(new Dimension(200, 200));
        panel2.add(new JLabel("Casting Costs found:"));
        panel2.add(chart);
        JPanel panel3 = new JPanel();
        panel3.setLayout(new BoxLayout(panel3, 1));
        ManaPieChart chart2 = new ManaPieChart((Integer)qtys.get("plains"), (Integer)qtys.get("island"), (Integer)qtys.get("swamp"), (Integer)qtys.get("mountain"), (Integer)qtys.get("forest"), (Integer)qtys.get("wastes"));
        chart2.setMinimumSize(new Dimension(200, 200));
        panel3.add(new JLabel("Basic Land types found:"));
        panel3.add(chart2);
        JPanel panel4 = new JPanel();
        panel4.setLayout(new BoxLayout(panel4, 1));
        ManaPieChart chart3 = new ManaPieChart((Integer)manaCounts.get("{W}"), (Integer)manaCounts.get("{U}"), (Integer)manaCounts.get("{B}"), (Integer)manaCounts.get("{R}"), (Integer)manaCounts.get("{G}"), (Integer)manaCounts.get("{C}"));
        chart3.setMinimumSize(new Dimension(200, 200));
        panel4.add(new JLabel("Mana sources found:"));
        panel4.add(chart3);
        JPanel panel5 = new JPanel();
        panel5.setLayout(new BoxLayout(panel5, 1));
        ManaBarChart chart4 = new ManaBarChart(pips_at_cmcs);
        chart4.setMinimumSize(new Dimension(200, 200));
        panel5.add(new JLabel("Mana distribution:"));
        panel5.add(chart4);
        panel.add(panel2);
        panel.add(panel3);
        panel.add(panel4);
        panel.add(panel5);
        JFrame frame = new JFrame("JOptionPane showMessageDialog component example");
        JOptionPane.showMessageDialog(frame, panel, "This is the distribution of colors found", 1);
    }

    public void blingDeck() {
        if (this.mode != Constants.DeckEditorMode.FREE_BUILDING) {
            return;
        }
        if (JOptionPane.showConfirmDialog(null, "Are you sure you want to bling your deck?  This process will add cards!", "WARNING", 0) == 1) {
            return;
        }
        HashMap<String, Integer> pimpedSets = new HashMap<String, Integer>();
        HashMap<CardView, Integer> pimpedCards = new HashMap<CardView, Integer>();
        pimpedSets.put("PCMP", 1);
        pimpedSets.put("MPS", 1);
        pimpedSets.put("MP2", 1);
        pimpedSets.put("EXP", 1);
        pimpedSets.put("CP1", 1);
        pimpedSets.put("CP2", 1);
        pimpedSets.put("CP3", 1);
        pimpedSets.put("JGP", 1);
        pimpedSets.put("G99", 1);
        pimpedSets.put("G00", 1);
        pimpedSets.put("G01", 1);
        pimpedSets.put("G02", 1);
        pimpedSets.put("G03", 1);
        pimpedSets.put("G04", 1);
        pimpedSets.put("G05", 1);
        pimpedSets.put("G06", 1);
        pimpedSets.put("G07", 1);
        pimpedSets.put("G08", 1);
        pimpedSets.put("G09", 1);
        pimpedSets.put("G10", 1);
        pimpedSets.put("G11", 1);
        pimpedSets.put("J12", 1);
        pimpedSets.put("J13", 1);
        pimpedSets.put("J14", 1);
        pimpedSets.put("J15", 1);
        pimpedSets.put("J16", 1);
        pimpedSets.put("J17", 1);
        pimpedSets.put("J18", 1);
        pimpedSets.put("J19", 1);
        pimpedSets.put("J20", 1);
        pimpedSets.put("PARL", 1);
        pimpedSets.put("PAL99", 1);
        pimpedSets.put("PAL00", 1);
        pimpedSets.put("PAL01", 1);
        pimpedSets.put("PAL02", 1);
        pimpedSets.put("PAL03", 1);
        pimpedSets.put("PAL04", 1);
        pimpedSets.put("PAL05", 1);
        pimpedSets.put("PAL06", 1);
        pimpedSets.put("UGIN", 1);
        pimpedSets.put("PALP", 1);
        pimpedSets.put("PELP", 1);
        pimpedSets.put("FNM", 1);
        pimpedSets.put("F01", 1);
        pimpedSets.put("F02", 1);
        pimpedSets.put("F03", 1);
        pimpedSets.put("F04", 1);
        pimpedSets.put("F05", 1);
        pimpedSets.put("F06", 1);
        pimpedSets.put("F07", 1);
        pimpedSets.put("F08", 1);
        pimpedSets.put("F09", 1);
        pimpedSets.put("F10", 1);
        pimpedSets.put("F11", 1);
        pimpedSets.put("F12", 1);
        pimpedSets.put("F13", 1);
        pimpedSets.put("F14", 1);
        pimpedSets.put("F15", 1);
        pimpedSets.put("F16", 1);
        pimpedSets.put("F17", 1);
        pimpedSets.put("F18", 1);
        pimpedSets.put("MPR", 1);
        pimpedSets.put("P03", 1);
        pimpedSets.put("P04", 1);
        pimpedSets.put("P05", 1);
        pimpedSets.put("P06", 1);
        pimpedSets.put("P07", 1);
        pimpedSets.put("P08", 1);
        pimpedSets.put("P09", 1);
        pimpedSets.put("P10", 1);
        pimpedSets.put("P11", 1);
        pimpedSets.put("OVNT", 1);
        pimpedSets.put("PJSE", 1);
        pimpedSets.put("P2HG", 1);
        pimpedSets.put("PGTW", 1);
        pimpedSets.put("PJAS", 1);
        pimpedSets.put("EXP", 1);
        pimpedSets.put("PGPX", 1);
        pimpedSets.put("PMEI", 1);
        pimpedSets.put("PLS", 1);
        String[] sets = pimpedSets.keySet().toArray(new String[pimpedSets.keySet().size()]);
        Boolean didModify = false;
        for (List<List<CardView>> gridRow : this.cardGrid) {
            for (List<CardView> stack : gridRow) {
                for (CardView card : stack) {
                    Card acard;
                    if (card.getSuperTypes().contains((Object)SuperType.BASIC) || pimpedSets.containsKey(card.getExpansionSetCode())) continue;
                    CardCriteria cardCriteria = new CardCriteria();
                    cardCriteria.setCodes(sets);
                    cardCriteria.name(card.getName());
                    List<CardInfo> cardPool = CardRepository.instance.findCards(cardCriteria);
                    if (cardPool.isEmpty() || !(acard = cardPool.get(RandomUtil.nextInt(cardPool.size())).createMockCard()).getName().equals(card.getName())) continue;
                    CardView pimpedCard = new CardView(acard);
                    this.addCardView(pimpedCard);
                    this.eventSource.fireEventDeckCardAdded((SimpleCardView)pimpedCard, null);
                    pimpedCards.put(pimpedCard, 1);
                    didModify = true;
                }
            }
            if (!didModify.booleanValue()) continue;
            for (CardView c : pimpedCards.keySet()) {
                this.sortIntoGrid(c, null);
            }
            this.trimGrid();
            this.layoutGrid();
            this.repaintGrid();
            JOptionPane.showMessageDialog(null, "Added " + pimpedCards.size() + " cards.  You can select them and the originals by choosing 'Multiples'");
        }
    }

    private void oldVersionDeck() {
        if (this.mode != Constants.DeckEditorMode.FREE_BUILDING) {
            return;
        }
        if (JOptionPane.showConfirmDialog(null, "Are you sure you want to switch your card versions to the oldest ones?", "WARNING", 0) != 0) {
            return;
        }
        this.deselectAll();
        HashMap<CardView, NewCardInfo> cardsToReplace = new HashMap<CardView, NewCardInfo>();
        for (List<List<CardView>> gridRow : this.cardGrid) {
            for (List<CardView> stack : gridRow) {
                for (CardView card : stack) {
                    Card oldestCard;
                    CardView oldestView;
                    CardInfo oldestCardInfo = CardRepository.instance.findOldestNonPromoVersionCard(card.getName());
                    if (oldestCardInfo == null || card.isSameCardVersion(oldestView = new CardView(oldestCard = oldestCardInfo.createMockCard()))) continue;
                    cardsToReplace.put(card, new NewCardInfo(oldestCard, oldestView));
                }
            }
        }
        ArrayList<CardView> cardsToRemove = new ArrayList<CardView>();
        cardsToReplace.forEach((currentCard, oldestInfo) -> {
            this.addCardView(((NewCardInfo)oldestInfo).newView, ((NewCardInfo)oldestInfo).newCard, (CardView)currentCard);
            cardsToRemove.add((CardView)currentCard);
        });
        this.removeCards(cardsToRemove);
        JOptionPane.showMessageDialog(null, "Replaced cards: " + cardsToReplace.size());
    }

    public void setCards(CardsView cardsView, DeckCardLayout layout, BigCard bigCard) {
        if (bigCard != null) {
            lastBigCard = bigCard;
        }

        // Remove all of the cards not in the cardsView
        boolean didModify = false; // Until contested
        for (int i = 0; i < cardGrid.size(); ++i) {
            List<List<CardView>> gridRow = cardGrid.get(i);
            for (int j = 0; j < gridRow.size(); ++j) {
                List<CardView> stack = gridRow.get(j);
                for (int k = 0; k < stack.size(); ++k) {
                    CardView card = stack.get(k);
                    if (!cardsView.containsKey(card.getId())) {
                        // Remove it
                        removeCardView(card);
                        stack.remove(k--);

                        // Mark
                        didModify = true;
                    }
                }
            }
        }

        // clear grid from empty rows
        if (didModify) {
            trimGrid();
        }

        if (layout == null) {
            // No layout -> add any new card views one at a time as par the current sort
            for (CardView newCard : cardsView.values()) {
                if (!cardViews.containsKey(newCard.getId())) {
                    // Is a new card
                    addCardView(newCard);

                    // Put it into the appropirate place in the grid given the current sort
                    sortIntoGrid(newCard, null);

                    // Mark
                    didModify = true;
                }
            }
        } else {
            // Layout given -> Build card grid using layout, and set sort / separate

            // Always modify when given a layout
            didModify = true;

            // Load in settings
            loadSettings(Settings.parse(layout.getSettings()));

            // Traverse the cards once and track them so we can pick ones to insert into the grid
            Map<String, Map<String, List<CardView>>> trackedCards = new HashMap<>();
            for (CardView newCard : cardsView.values()) {
                if (!cardViews.containsKey(newCard.getId())) {
                    // Add the new card
                    addCardView(newCard);

                    // Add the new card to tracking
                    Map<String, List<CardView>> forSetCode;
                    if (trackedCards.containsKey(newCard.getExpansionSetCode())) {
                        forSetCode = trackedCards.get(newCard.getExpansionSetCode());
                    } else {
                        forSetCode = new HashMap<>();
                        trackedCards.put(newCard.getExpansionSetCode(), forSetCode);
                    }
                    List<CardView> list;
                    if (forSetCode.containsKey(newCard.getCardNumber())) {
                        list = forSetCode.get(newCard.getCardNumber());
                    } else {
                        list = new ArrayList<>();
                        forSetCode.put(newCard.getCardNumber(), list);
                    }
                    list.add(newCard);
                }
            }

            // Now go through the layout and use it to build the cardGrid
            cardGrid = new ArrayList<>();
            maxStackSize = new ArrayList<>();
            for (List<List<DeckCardInfo>> row : layout.getCards()) {
                List<List<CardView>> gridRow = new ArrayList<>();
                int thisMaxStackSize = 0;
                cardGrid.add(gridRow);
                for (List<DeckCardInfo> stack : row) {
                    List<CardView> gridStack = new ArrayList<>();
                    gridRow.add(gridStack);
                    for (DeckCardInfo info : stack) {
                        if (trackedCards.containsKey(info.getSetCode()) && trackedCards.get(info.getSetCode()).containsKey(info.getCardNumber())) {
                            List<CardView> candidates
                                    = trackedCards.get(info.getSetCode()).get(info.getCardNumber());
                            if (!candidates.isEmpty()) {
                                gridStack.add(candidates.remove(0));
                                thisMaxStackSize = Math.max(thisMaxStackSize, gridStack.size());
                            }
                        }
                    }
                }
                maxStackSize.add(thisMaxStackSize);
            }

            // Check that there aren't any "orphans" not referenced in the layout. There should
            // never be any under normal operation, but as a failsafe in case the user screwed with
            // the file in an invalid way, sort them into the grid so that they aren't just left hanging.
            for (Map<String, List<CardView>> tracked : trackedCards.values()) {
                for (List<CardView> orphans : tracked.values()) {
                    for (CardView orphan : orphans) {
                        logger.info("Orphan when setting with layout: "
                                + orphan.getExpansionSetCode() + ":"
                                + orphan.getName() + ":"
                                + orphan.getCardNumber()
                        );
                        sortIntoGrid(orphan, null);
                    }
                }
            }
        }

        if (didModify) {
            // clear grid from empty rows
            trimGrid();

            // Update layout
            layoutGrid();
            repaintGrid();
        }
    }

    private int getCount(CardType cardType) {
        if (null != cardType) {
            switch (cardType) {
                case CREATURE: {
                    return this.creatureCounter.get();
                }
                case LAND: {
                    return this.landCounter.get();
                }
                case ARTIFACT: {
                    return this.artifactCounter.get();
                }
                case ENCHANTMENT: {
                    return this.enchantmentCounter.get();
                }
                case INSTANT: {
                    return this.instantCounter.get();
                }
                case PLANESWALKER: {
                    return this.planeswalkerCounter.get();
                }
                case SORCERY: {
                    return this.sorceryCounter.get();
                }
                case KINDRED: {
                    return this.kindredCounter.get();
                }
            }
        }
        return 0;
    }

    private void updateCounts() {
        int extraDeckCount = this.allCards.stream().filter(c -> c.isExtraDeckCard()).collect(Collectors.toSet()).size();
        int maindeckCount = this.allCards.size() - extraDeckCount;
        this.deckNameAndCountLabel.setText(this.role.getName() + " - " + maindeckCount + (extraDeckCount > 0 ? " (" + extraDeckCount + ")" : ""));
        this.creatureCountLabel.setText(String.valueOf(this.creatureCounter.get()));
        this.landCountLabel.setText(String.valueOf(this.landCounter.get()));
        for (CardType cardType : this.selectByTypeButtons.keySet()) {
            AbstractButton button = this.selectByTypeButtons.get((Object)cardType);
            String text = cardType.toString();
            int numCards = this.getCount(cardType);
            if (cardType == CardType.CONSPIRACY) continue;
            if (numCards > 0) {
                button.setForeground(Color.BLACK);
                text = text + " - " + numCards;
            } else {
                button.setForeground(new Color(100, 100, 100));
            }
            button.setText(text);
        }
    }

    private void showCardRightClickMenu(CardView card, MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem hide = new JMenuItem("Hide (hidden in sideboard)");
        hide.addActionListener(e2 -> this.hideSelection());
        menu.add(hide);
        JMenuItem invertSelection = new JMenuItem("Invert selection");
        invertSelection.addActionListener(e2 -> this.invertSelection());
        menu.add(invertSelection);
        JMenuItem chooseMatching = new JMenuItem("Select same cards");
        chooseMatching.addActionListener(e2 -> this.chooseMatching());
        menu.add(chooseMatching);
        if (this.mode == Constants.DeckEditorMode.FREE_BUILDING) {
            JMenuItem chooseEdition = new JMenuItem("Elegir edici\u00f3n...");
            chooseEdition.addActionListener(e2 -> this.chooseEdition(card));
            menu.add(chooseEdition);
            menu.add(new JPopupMenu.Separator());
            JMenuItem duplicateSelection = new JMenuItem("Duplicate selected cards");
            duplicateSelection.addActionListener(e2 -> this.duplicateSelection());
            menu.add(duplicateSelection);
        }
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void chooseEdition(CardView clickedCard) {
        if (this.mode != Constants.DeckEditorMode.FREE_BUILDING || clickedCard == null) {
            return;
        }
        CardCriteria criteria = new CardCriteria();
        criteria.name(clickedCard.getName());
        ArrayList<CardInfo> printings = new ArrayList<CardInfo>(CardRepository.instance.findCards(criteria));
        printings.sort(Comparator.comparing(CardInfo::getSetCode, Comparator.nullsLast(String::compareToIgnoreCase)).thenComparingInt(this::xcpPrintingSelectorNumberSort).thenComparing(CardInfo::getCardNumber, Comparator.nullsLast(String::compareToIgnoreCase)));
        if (printings.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron ediciones para " + clickedCard.getName());
            return;
        }
        DefaultListModel model = new DefaultListModel();
        printings.forEach(model::addElement);
        JList<Object> list = new JList<Object>(model);
        list.setSelectionMode(0);
        list.setVisibleRowCount(Math.min(12, printings.size()));
        list.setFixedCellHeight(42);
        list.setCellRenderer(new DefaultListCellRenderer(){

            @Override
            public Component getListCellRendererComponent(JList<?> listComponent, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel)super.getListCellRendererComponent(listComponent, value, index, isSelected, cellHasFocus);
                CardInfo info = (CardInfo)value;
                ExpansionSet expansion = Sets.findSet(info.getSetCode());
                String setName = expansion == null ? "" : expansion.getName();
                label.setText("<html><b>" + info.getSetCode() + "</b>  #" + info.getCardNumber() + (setName.isEmpty() ? "" : " \u00b7 " + setName) + "<br>" + (Object)((Object)info.getRarity()) + "</html>");
                label.setIcon(null);
                return label;
            }
        });
        JTextField editionSearch = new JTextField();
        editionSearch.setToolTipText("Busca por c\u00f3digo o nombre de edici\u00f3n, por ejemplo LOR o Lorwyn");
        final Runnable filterPrintings = () -> {
            String query = editionSearch.getText().trim().toLowerCase(Locale.ROOT);
            model.clear();
            for (CardInfo info : printings) {
                String setName;
                ExpansionSet expansion = Sets.findSet(info.getSetCode());
                String string = setName = expansion == null ? "" : expansion.getName();
                if (!query.isEmpty() && !String.valueOf(info.getSetCode()).toLowerCase(Locale.ROOT).contains(query) && !setName.toLowerCase(Locale.ROOT).contains(query)) continue;
                model.addElement(info);
            }
            if (!model.isEmpty()) {
                list.setSelectedIndex(0);
                list.ensureIndexIsVisible(0);
            }
        };
        editionSearch.getDocument().addDocumentListener(new DocumentListener(){

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterPrintings.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterPrintings.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterPrintings.run();
            }
        });
        JLabel previewLabel = new JLabel("Selecciona una edici\u00f3n", 0);
        previewLabel.setPreferredSize(new Dimension(230, 330));
        list.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            CardInfo info = (CardInfo)list.getSelectedValue();
            if (info == null) {
                previewLabel.setIcon(null);
                return;
            }
            try {
                CardView preview = new CardView(info.createMockCard());
                BufferedImage image = ImageCache.getCardImage(preview, 220, 310).getImage();
                previewLabel.setText(image == null ? "Imagen no descargada" : "");
                previewLabel.setIcon(image == null ? null : new ImageIcon(image));
            }
            catch (Exception ex) {
                previewLabel.setText("No se pudo cargar la imagen");
                previewLabel.setIcon(null);
            }
        });
        int currentIndex = 0;
        for (int i = 0; i < printings.size(); ++i) {
            CardInfo info = (CardInfo)printings.get(i);
            if (!Objects.equals(info.getSetCode(), clickedCard.getExpansionSetCode()) || !Objects.equals(info.getCardNumber(), clickedCard.getCardNumber())) continue;
            currentIndex = i;
            break;
        }
        list.setSelectedIndex(currentIndex);
        list.ensureIndexIsVisible(currentIndex);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(260, 500));
        JPanel listPanel = new JPanel(new BorderLayout(0, 6));
        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.add((Component)new JLabel("Buscar edici\u00f3n:"), "West");
        searchPanel.add((Component)editionSearch, "Center");
        listPanel.add((Component)searchPanel, "North");
        listPanel.add((Component)scroll, "Center");
        JPanel selectorPanel = new JPanel(new BorderLayout(10, 0));
        selectorPanel.add((Component)listPanel, "Center");
        selectorPanel.add((Component)previewLabel, "East");
        int answer = JOptionPane.showConfirmDialog(this, selectorPanel, "Elegir edici\u00f3n - " + clickedCard.getName(), 2, -1);
        CardInfo selected = (CardInfo)list.getSelectedValue();
        if (answer != 0 || selected == null) {
            return;
        }
        List<CardView> cardsToReplace = this.allCards.stream().filter(card -> Objects.equals(card.getName(), clickedCard.getName())).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<CardView> cardsToRemove = new ArrayList<CardView>();
        for (CardView currentCard : cardsToReplace) {
            Card replacement;
            CardView replacementView;
            if (currentCard.isSameCardVersion(replacementView = new CardView(replacement = selected.createMockCard()))) continue;
            this.addCardView(replacementView, replacement, currentCard);
            cardsToRemove.add(currentCard);
        }
        this.removeCards(cardsToRemove);
    }

    private int xcpPrintingSelectorNumberSort(CardInfo info) {
        String number = info.getCardNumber();
        if (number == null) {
            return Integer.MAX_VALUE;
        }
        Matcher matcher = Pattern.compile("\\d+").matcher(number);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            }
            catch (NumberFormatException ignored) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    public void addCardView(CardView newView) {
        this.addCardView(newView, null, null);
    }

    public void addCardView(final CardView newView, Card newCard, CardView duplicatedFromView) {
        if (newCard == null != (duplicatedFromView == null)) {
            throw new IllegalArgumentException("Wrong code usage: if you duplicate card then must ref real card too");
        }
        this.allCards.add(newView);
        for (CardTypeCounter counter : this.allCounters) {
            counter.add(newView);
        }
        this.updateCounts();
        final MageCard cardPanel = Plugins.instance.getMageCard(newView, this.lastBigCard, new CardIconRenderSettings(), new Dimension(this.getCardWidth(), this.getCardHeight()), null, true, true, PreferencesDialog.getRenderMode(), true);
        cardPanel.setCardContainerRef((Container)this);
        cardPanel.update(newView);
        cardPanel.setCardCaptionTopOffset(0);
        cardPanel.addMouseMotionListener((MouseMotionListener)new MouseAdapter(){

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!DragCardGrid.this.dragger.isDragging()) {
                    if (!newView.isSelected()) {
                        DragCardGrid.this.selectCard(newView);
                    }
                    DragCardGrid.this.dragger.handleDragStart(cardPanel, e);
                }
            }
        });
        this.cardContent.add((Component)cardPanel);
        this.cardViews.put(newView.getId(), cardPanel);
        if (newCard != null || duplicatedFromView != null) {
            this.sortIntoGrid(newView, duplicatedFromView);
            this.eventSource.fireEventDeckCardAdded((SimpleCardView)newView, newCard);
            this.trimGrid();
            this.layoutGrid();
            this.repaintGrid();
        }
    }

    public void addDragCardGridListener(DragCardGridListener l) {
        this.listeners.add(l);
    }

    private void notifyCardsSelected() {
        for (DragCardGridListener listener : this.listeners) {
            listener.cardsSelected();
        }
    }

    private void selectCard(CardView targetCard) {
        for (CardView card : this.allCards) {
            if (card == targetCard) {
                if (card.isSelected()) continue;
                card.setSelected(true);
                this.cardViews.get(card.getId()).update(card);
                continue;
            }
            if (!card.isSelected()) continue;
            card.setSelected(false);
            this.cardViews.get(card.getId()).update(card);
        }
    }

    private void toggleSelected(CardView targetCard) {
        targetCard.setSelected(!targetCard.isSelected());
        this.cardViews.get(targetCard.getId()).update(targetCard);
    }

    private void cardClicked(CardView targetCard, MouseEvent e) {
        if (e.isShiftDown()) {
            this.toggleSelected(targetCard);
        } else {
            this.selectCard(targetCard);
        }
        this.notifyCardsSelected();
    }

    private void removeCardView(CardView card) {
        this.allCards.remove(card);
        for (CardTypeCounter counter : this.allCounters) {
            counter.remove(card);
        }
        this.updateCounts();
        this.cardContent.remove((Component)this.cardViews.get(card.getId()));
        this.cardViews.remove(card.getId());
    }

    private void sortIntoGrid(CardView newCard, CardView duplicatedFromCard) {
        List<List<CardView>> targetRow;
        if (duplicatedFromCard != null) {
            for (List<List<CardView>> gridRow : this.cardGrid) {
                for (List<CardView> gridStack : gridRow) {
                    for (int i = 0; i < gridStack.size(); ++i) {
                        if (!gridStack.get(i).equals(duplicatedFromCard)) continue;
                        gridStack.add(i, newCard);
                        return;
                    }
                }
            }
        }
        if (this.cardGrid.isEmpty()) {
            this.cardGrid.add(0, new ArrayList());
            this.maxStackSize.add(0, 0);
        }
        if (this.separateCreatures) {
            int i;
            if (this.cardGrid.size() < 2) {
                this.cardGrid.add(1, new ArrayList());
                this.maxStackSize.add(1, 0);
                for (int i2 = 0; i2 < this.cardGrid.get(0).size(); ++i2) {
                    this.cardGrid.get(1).add(new ArrayList());
                }
            }
            boolean isFirstRowWithCreatures = true;
            for (i = 0; i < this.cardGrid.get(0).size(); ++i) {
                if (!this.cardGrid.get(0).get(i).stream().anyMatch(card -> !card.isCreature())) continue;
                isFirstRowWithCreatures = false;
                break;
            }
            if (!isFirstRowWithCreatures) {
                for (i = 0; i < this.cardGrid.get(0).size(); ++i) {
                    if (this.cardGrid.get(0).get(i).isEmpty()) continue;
                    this.cardGrid.get(1).add(this.cardGrid.get(0).get(i));
                }
                this.cardGrid.get(0).clear();
                while (this.cardGrid.get(0).size() < this.cardGrid.get(1).size()) {
                    this.cardGrid.get(0).add(new ArrayList());
                }
            }
            targetRow = newCard.isCreature() ? this.cardGrid.get(0) : this.cardGrid.get(1);
        } else {
            targetRow = this.cardGrid.get(0);
        }
        boolean didInsert = false;
        for (int currentColumn = 0; currentColumn < targetRow.size(); ++currentColumn) {
            CardView cardInColumn = null;
            for (List<List<CardView>> gridRow : this.cardGrid) {
                CardView card2;
                Iterator<CardView> iterator = gridRow.get(currentColumn).iterator();
                if (!iterator.hasNext()) continue;
                cardInColumn = card2 = iterator.next();
            }
            if (cardInColumn == null) {
                logger.error((Object)("Empty column! " + currentColumn));
                continue;
            }
            CardViewComparator cardComparator = this.cardSort.getComparator();
            int res = cardComparator.compare(newCard, cardInColumn);
            if (res > 0) continue;
            if (res < 0) {
                for (int rowIndex = 0; rowIndex < this.cardGrid.size(); ++rowIndex) {
                    this.cardGrid.get(rowIndex).add(currentColumn, new ArrayList());
                }
            }
            targetRow.get(currentColumn).add(newCard);
            didInsert = true;
            break;
        }
        if (!didInsert) {
            for (int rowIndex = 0; rowIndex < this.cardGrid.size(); ++rowIndex) {
                this.cardGrid.get(rowIndex).add(new ArrayList());
            }
            targetRow.get(targetRow.size() - 1).add(newCard);
        }
    }

    private void trimGrid() {
        for (int rowIndex = 0; rowIndex < this.cardGrid.size(); ++rowIndex) {
            List<List<CardView>> gridRow = this.cardGrid.get(rowIndex);
            int rowMaxStackSize = 0;
            for (List<CardView> stack : gridRow) {
                for (int i = 0; i < stack.size(); ++i) {
                    if (stack.get(i) != null) continue;
                    stack.remove(i--);
                }
                rowMaxStackSize = Math.max(rowMaxStackSize, stack.size());
            }
            if (rowMaxStackSize == 0) {
                this.cardGrid.remove(rowIndex);
                this.maxStackSize.remove(rowIndex);
                --rowIndex;
                continue;
            }
            this.maxStackSize.set(rowIndex, rowMaxStackSize);
        }
        if (!this.cardGrid.isEmpty()) {
            for (int colIndex = 0; colIndex < this.cardGrid.get(0).size(); ++colIndex) {
                boolean hasContent = false;
                for (List<List<CardView>> aCardGrid : this.cardGrid) {
                    if (aCardGrid.get(colIndex).isEmpty()) continue;
                    hasContent = true;
                    break;
                }
                if (hasContent) continue;
                for (List<List<CardView>> aCardGrid : this.cardGrid) {
                    aCardGrid.remove(colIndex);
                }
                --colIndex;
            }
        }
        while (this.stackCountLabels.size() > this.cardGrid.size()) {
            List<JLabel> labels = this.stackCountLabels.remove(this.cardGrid.size());
            for (JLabel label : labels) {
                this.cardContent.remove(label);
            }
        }
        int colCount = this.cardGrid.isEmpty() ? 0 : this.cardGrid.get(0).size();
        for (List<JLabel> labels : this.stackCountLabels) {
            while (labels.size() > colCount) {
                this.cardContent.remove(labels.remove(colCount));
            }
        }
    }

    private int getCardWidth() {
        if (GUISizeHelper.editorCardDimension == null) {
            return 200;
        }
        return (int)((float)GUISizeHelper.editorCardDimension.width * this.cardSizeMod);
    }

    private int getCardHeight() {
        return (int)(1.4 * (double)this.getCardWidth());
    }

    private void repaintGrid() {
        this.cardScroll.revalidate();
        this.cardScroll.repaint();
        this.repaint();
    }

    private void layoutGrid() {
        int cardWidth = this.getCardWidth();
        int cardHeight = this.getCardHeight();
        int cardTopHeight = CardRenderer.getCardTopHeight(cardWidth);
        int countLabelHeight = DragCardGrid.getCountLabelHeight();
        int layerIndex = 0;
        int currentY = countLabelHeight;
        int maxWidth = 0;
        for (int rowIndex = 0; rowIndex < this.cardGrid.size(); ++rowIndex) {
            int rowMaxStackSize = 0;
            List<List<CardView>> gridRow = this.cardGrid.get(rowIndex);
            for (int colIndex = 0; colIndex < gridRow.size(); ++colIndex) {
                JLabel countLabel;
                List<CardView> stack = gridRow.get(colIndex);
                if (this.stackCountLabels.size() <= rowIndex) {
                    this.stackCountLabels.add(new ArrayList());
                }
                if (this.stackCountLabels.get(rowIndex).size() <= colIndex) {
                    if (this.countLabelListener == null) {
                        this.countLabelListener = new MouseAdapter(){

                            @Override
                            public void mouseClicked(MouseEvent e) {
                                if (!SwingUtilities.isLeftMouseButton(e)) {
                                    return;
                                }
                                JLabel countLabel = (JLabel)e.getComponent();
                                List<CardView> cards = DragCardGrid.this.findCardStackByCountLabel(countLabel);
                                boolean selected = !cards.isEmpty() && ((CardView)cards.get(0)).isSelected();
                                cards.forEach(card -> {
                                    card.setSelected(!selected);
                                    ((MageCard)DragCardGrid.this.cardViews.get(card.getId())).update(card);
                                });
                            }
                        };
                    }
                    countLabel = DragCardGrid.createCountLabel(this.countLabelListener);
                    this.cardContent.add((Component)countLabel, (Object)0);
                    this.stackCountLabels.get(rowIndex).add(countLabel);
                }
                countLabel = this.stackCountLabels.get(rowIndex).get(colIndex);
                if (stack.isEmpty()) {
                    countLabel.setVisible(false);
                } else {
                    String description = this.cardSort.getComparator().getCategoryName(stack.get(0));
                    DragCardGrid.updateCountLabel(countLabel, stack.size(), description);
                    countLabel.setLocation(this.getGridPadding() + (cardWidth + this.getGridPadding()) * colIndex, currentY - countLabelHeight);
                    countLabel.setSize(cardWidth, countLabelHeight);
                    countLabel.setVisible(true);
                }
                rowMaxStackSize = Math.max(rowMaxStackSize, stack.size());
                for (int i = 0; i < stack.size(); ++i) {
                    CardView card = stack.get(i);
                    MageCard view = this.cardViews.get(card.getId());
                    int x = this.getGridPadding() + (cardWidth + this.getGridPadding()) * colIndex;
                    int y = currentY + i * cardTopHeight;
                    view.setCardBounds(x, y, cardWidth, cardHeight);
                    this.cardContent.setLayer((Component)view, layerIndex++);
                }
            }
            maxWidth = Math.max(maxWidth, this.getGridPadding() + (this.getGridPadding() + cardWidth) * gridRow.size());
            this.maxStackSize.set(rowIndex, rowMaxStackSize);
            currentY += cardTopHeight * (rowMaxStackSize - 1) + cardHeight + countLabelHeight;
        }
        this.cardContent.setPreferredSize(new Dimension(maxWidth, currentY - countLabelHeight + this.getGridPadding()));
    }

    private int getGridPadding() {
        return Math.max(12, Math.round(this.cardSizeMod * 12.0f * GUISizeHelper.dialogGuiScale));
    }

    public static int getCountLabelHeight() {
        return Math.round(2.6f * DragCardGrid.getCountLabelFontSize());
    }

    private static float getCountLabelFontSize() {
        return 0.8f * (float)GUISizeHelper.dialogFont.getSize();
    }

    public static Font getCountLabelFont() {
        return GUISizeHelper.dialogFont.deriveFont(1, DragCardGrid.getCountLabelFontSize());
    }

    public static JLabel createCountLabel(MouseListener mouseListener) {
        JLabel countLabel = new JLabel("", 0);
        countLabel.setFont(DragCardGrid.getCountLabelFont());
        countLabel.setForeground(Color.WHITE);
        if (mouseListener != null) {
            countLabel.addMouseListener(mouseListener);
        }
        return countLabel;
    }

    public static void updateCountLabel(JLabel countLabel, int amount, String description) {
        String labelText = ManaSymbols.replaceSymbolsWithHTML(description, Math.round(DragCardGrid.getCountLabelFontSize()));
        String labelHint = ManaSymbols.replaceSymbolsWithHTML(description, ManaSymbols.Type.TOOLTIP);
        boolean smallMode = description.isEmpty();
        boolean supportCustomClicks = countLabel.getMouseListeners().length > 0 && !(countLabel.getMouseListeners()[0] instanceof ToolTipManager);
        countLabel.setText("<html><div style='text-align: center;'>  <div>" + amount + "</div>" + (smallMode ? "" : "  <div style=''>" + labelText + "</div>") + "</div>");
        countLabel.setToolTipText("<html>" + amount + (smallMode ? "" : " - " + labelHint) + (supportCustomClicks ? "<br>Click on the count label to select/unselect cards stack." : ""));
        countLabel.setVerticalAlignment(smallMode ? 0 : 1);
        if (DebugUtil.GUI_DECK_EDITOR_DRAW_COUNT_LABEL_BORDER) {
            countLabel.setBorder(BorderFactory.createLineBorder(Color.green));
        }
    }

    private List<CardView> findCardStackByCountLabel(JLabel countLabel) {
        for (int rowIndex = 0; rowIndex < this.cardGrid.size(); ++rowIndex) {
            List<List<CardView>> gridRow = this.cardGrid.get(rowIndex);
            for (int colIndex = 0; colIndex < gridRow.size(); ++colIndex) {
                if (this.stackCountLabels.size() < rowIndex || this.stackCountLabels.get(rowIndex).size() < colIndex || !countLabel.equals(this.stackCountLabels.get(rowIndex).get(colIndex))) continue;
                return gridRow.get(colIndex);
            }
        }
        return new ArrayList<CardView>();
    }

    private static void makeButtonPopup(AbstractButton button, JPopupMenu popup) {
        button.addActionListener(e -> popup.show(button, 0, button.getHeight()));
    }

    public static enum Sort {
        NONE("No Sort", new CardViewNoneComparator()),
        CARD_TYPE("Card Type", new CardViewCardTypeComparator()),
        CMC("Mana Value", new CardViewCostComparator()),
        COLOR("Color", new CardViewColorComparator()),
        COLOR_IDENTITY("Color Identity", new CardViewColorIdentityComparator()),
        RARITY("Rarity", new CardViewRarityComparator()),
        EDH_POWER_LEVEL("EDH Power Level", new CardViewEDHPowerLevelComparator());

        private final CardViewComparator comparator;
        private final String text;

        private Sort(String text, CardViewComparator comparator) {
            this.comparator = comparator;
            this.text = text;
        }

        public CardViewComparator getComparator() {
            return this.comparator;
        }

        public String getText() {
            return this.text;
        }
    }

    public static enum Role {
        MAINDECK("Main deck"),
        SIDEBOARD("Sideboard/commander");

        private final String name;

        private Role(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public static class Settings {
        public Sort sort;
        public boolean separateCreatures;
        public int cardSize;
        private static final Pattern parser = Pattern.compile("\\(([^,]*),([^,]*),([^)]*)\\)");

        public static Settings parse(String str) {
            Matcher m = parser.matcher(str);
            if (m.find()) {
                Settings s = new Settings();
                if (m.groupCount() > 0) {
                    s.sort = Sort.valueOf(m.group(1));
                }
                if (m.groupCount() > 1) {
                    s.separateCreatures = Boolean.valueOf(m.group(2));
                }
                s.cardSize = m.groupCount() > 2 ? Integer.parseInt(m.group(3)) : 50;
                return s;
            }
            return null;
        }

        public String toString() {
            return '(' + this.sort.toString() + ',' + this.separateCreatures + ',' + this.cardSize + ')';
        }
    }

    private abstract class CardTypeCounter {
        private int count = 0;

        private CardTypeCounter() {
        }

        protected abstract boolean is(CardView var1);

        int get() {
            return this.count;
        }

        void add(CardView card) {
            if (this.is(card)) {
                ++this.count;
            }
        }

        void remove(CardView card) {
            if (this.is(card)) {
                --this.count;
            }
        }
    }

    public static interface DragCardGridListener {
        public void cardsSelected();

        public void hideCards(Collection<CardView> var1);

        public void duplicateCards(Collection<CardView> var1);

        public void invertCardSelection(Collection<CardView> var1);

        public void showAll();
    }

    private class NewCardInfo {
        private final Card newCard;
        private final CardView newView;

        public NewCardInfo(Card newCard, CardView newView) {
            this.newCard = newCard;
            this.newView = newView;
        }
    }
}
