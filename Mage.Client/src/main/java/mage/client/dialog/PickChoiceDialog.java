package mage.client.dialog;

import mage.cards.action.TransferData;
import mage.choices.Choice;
import mage.choices.ChoiceHintType;
import mage.client.MageFrame;
import mage.client.cards.BigCard;
import mage.client.cards.VirtualCardInfo;
import mage.client.components.MageEditorPane;
import mage.client.game.GamePanel;
import mage.client.util.GUISizeHelper;
import mage.client.util.SettingsManager;
import mage.client.util.gui.MageDialogState;
import mage.game.command.Dungeon;
import mage.view.CardView;
import mage.view.DungeonView;
import org.mage.card.arcane.ManaSymbols;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * GUI: choosing one of the list's item. Uses in game's and non game's GUI like fast search
 *
 * @author JayDi85
 */
public class PickChoiceDialog extends MageDialog {

    Choice choice;

    // popup card info
    int lastModelIndex = -1;
    VirtualCardInfo cardInfo = new VirtualCardInfo();
    BigCard bigCard;
    UUID gameId;

    java.util.List<KeyValueItem> allItems = new ArrayList<>();
    KeyValueItem biggestItem = null; // for render optimization
    PickChoiceCallback callback = null;

    final private static String HTML_HEADERS_TEMPLATE = "<html><div style='text-align: center;'>%s</div></html>";

    public PickChoiceDialog() {
        initComponents();

        this.textMessage.enableHyperlinksAndCardPopups();
        this.textMessage.enableTextLabelMode();
        this.textSubMessage.enableHyperlinksAndCardPopups();
        this.textSubMessage.enableTextLabelMode();

        // pick choice shared in multiple dialogs, so modify window size only one time
        this.setSize(GUISizeHelper.dialogGuiScaleSize(this.getSize()));
        Dimension currentSize = this.getSize();
        this.setSize(Math.max(760, currentSize.width), Math.max(480, currentSize.height));
        this.setMinimumSize(new Dimension(640, 360));

        this.listChoices.setModel(new DefaultListModel<KeyValueItem>());
        this.listChoices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.listChoices.setFixedCellHeight(-1);
        this.listChoices.setCellRenderer(new ChoiceCellRenderer());
        this.scrollList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        installInteractionListeners();
        this.setModal(true);
    }

    /**
     * Install the dialog interaction handlers once. This dialog is reused for
     * many choices; registering them from showDialog() made every reuse add
     * another set of listeners and progressively slowed filtering and hover
     * handling.
     */
    private void installInteractionListeners() {
        editSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshSearch();
            }
        });

        editSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    doPrevSelect();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    doNextSelect();
                }
            }
        });

        listChoices.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    doChoose();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                choiceHintHide();
            }
        });

        listChoices.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = -1;
                Rectangle visibleCells = listChoices.getCellBounds(0, listChoices.getLastVisibleIndex());
                if (visibleCells != null && visibleCells.contains(e.getPoint())) {
                    index = listChoices.locationToIndex(e.getPoint());
                }

                if (index >= 0) {
                    choiceHintShow(index);
                } else {
                    choiceHintHide();
                }
            }
        });

        String cancelName = "cancel";
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
        getRootPane().getActionMap().put(cancelName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (choice != null && !choice.isRequired()) {
                    doCancel();
                }
            }
        });
    }

    public interface PickChoiceCallback {
        void onChoiceDone();
    }

    public void showDialog(Choice choice, String startSelectionValue, PickChoiceCallback callback) {
        showDialog(choice, startSelectionValue, null, null, null, callback);
    }

    public void showDialog(Choice choice, String startSelectionValue, UUID objectId, MageDialogState mageDialogState, BigCard bigCard, PickChoiceCallback callback) {
        this.choice = choice;
        this.bigCard = bigCard;
        this.gameId = objectId;
        this.callback = callback;

        changeGUISize();

        setMessageText(this.textMessage, choice.getMessage());
        setMessageText(this.textSubMessage, choice.getSubMessage());

        btCancel.setEnabled(!choice.isRequired());

        // popup support in headers

        // special choice (example: auto-choose answer next time)
        cbSpecial.setVisible(choice.isSpecialEnabled());
        cbSpecial.setText(choice.getSpecialText());
        cbSpecial.setToolTipText(choice.getSpecialHint());

        // 2 modes: string or key-values
        // store data in allItems for inremental filtering
        // http://logicbig.com/tutorials/core-java-tutorial/swing/list-filter/
        this.allItems.clear();
        this.biggestItem = null;
        if (choice.isKeyChoice()) {
            for (Map.Entry<String, String> entry : choice.getKeyChoices().entrySet()) {
                // with default hints
                KeyValueItem newItem = new KeyValueItem(entry.getKey(), entry.getValue(), entry.getValue(), choice.getHintType());
                this.allItems.add(newItem);
                if (this.biggestItem == null || newItem.valueAsHtml.length() > this.biggestItem.valueAsHtml.length()) {
                    biggestItem = newItem;
                }
            }
        } else {
            for (String value : choice.getChoices()) {
                // with default hints
                KeyValueItem newItem =new KeyValueItem(value, value, value, choice.getHintType());
                this.allItems.add(newItem);
                if (this.biggestItem == null || newItem.valueAsHtml.length() > this.biggestItem.valueAsHtml.length()) {
                    biggestItem = newItem;
                }
            }
        }

        // custom sorting
        if (choice.isSortEnabled()) {
            this.allItems.sort((o1, o2) -> {
                Integer n1 = choice.getSortData().get(o1.getKey());
                Integer n2 = choice.getSortData().get(o2.getKey());
                if (n1.equals(n2)) {
                    // default sorting by value
                    return o1.value.compareTo(o2.value);
                } else {
                    return Integer.compare(n1, n2);
                }
            });
        }

        // custom hints (per item)
        if (!choice.getHintData().isEmpty()) {
            this.allItems.forEach(item -> {
                List<String> info = choice.getHintData().getOrDefault(item.key, null);
                if (info != null) {
                    item.hintType = ChoiceHintType.valueOf(info.get(0));
                    item.hint = info.get(1);
                }
            });
        }

        // Keep variable row heights. A prototype cell makes JList reuse one
        // fixed height, which clips long alternative-cost descriptions.
        this.listChoices.setPrototypeCellValue(null);

        // search
        if (choice.isSearchEnabled()) {
            panelSearch.setVisible(true);
            this.editSearch.setText(choice.getSearchText());
        } else {
            panelSearch.setVisible(false);
            this.editSearch.setText("");
        }

        // window settings
        MageFrame.getDesktop().remove(this);
        MageFrame.getDesktop().add(this, this.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        if (mageDialogState != null) {
            mageDialogState.setStateToDialog(this);
        } else {
            this.makeWindowCentered();
        }

        // final load
        loadData();

        // Recompute variable-height rows before the dialog is displayed.
        this.listChoices.revalidate();
        this.scrollList.revalidate();
        this.revalidate();
        this.pack();
        Dimension packedSize = this.getSize();
        int maxWidth = Math.max(760, SettingsManager.instance.getScreenWidth() - 32);
        int maxHeight = Math.max(480, SettingsManager.instance.getScreenHeight() - 96);
        this.setSize(
                Math.min(maxWidth, Math.max(760, packedSize.width)),
                Math.min(maxHeight, Math.max(480, packedSize.height))
        );

        // start selection
        if (startSelectionValue != null) {
            int selectIndex = -1;
            for (int i = 0; i < this.listChoices.getModel().getSize(); i++) {
                KeyValueItem listItem = (KeyValueItem) this.listChoices.getModel().getElementAt(i);
                if (listItem.getKey().equals(startSelectionValue)) {
                    selectIndex = i;
                    break;
                }
            }

            if (selectIndex >= 0) {
                this.listChoices.setSelectedIndex(selectIndex);
                this.listChoices.ensureIndexIsVisible(selectIndex);
            }
        }

        this.setVisible(true);
    }

    private void refreshSearch() {
        if (choice != null) {
            choice.setSearchText(editSearch.getText());
            loadData();
        }
    }
    @Override
    public void changeGUISize() {
        super.changeGUISize();

        this.textMessage.setFont(GUISizeHelper.gameFeedbackPanelFont);
        this.textSubMessage.setFont(GUISizeHelper.gameFeedbackPanelFont);
        this.labelSearch.setFont(GUISizeHelper.gameFeedbackPanelFont);
        this.editSearch.setFont(GUISizeHelper.gameFeedbackPanelFont);
        this.cbSpecial.setFont(GUISizeHelper.gameFeedbackPanelFont);
        this.listChoices.setFont(GUISizeHelper.tableFont);
        this.btOK.setFont(GUISizeHelper.gameFeedbackPanelFont);
        this.btCancel.setFont(GUISizeHelper.gameFeedbackPanelFont);
    }

    private void choiceHintShow(int modelIndex) {
        // close old hint
        if (lastModelIndex != modelIndex) {
            cardInfo.onMouseExited();
            listChoices.setToolTipText(null);
        }

        KeyValueItem item = (KeyValueItem) listChoices.getModel().getElementAt(modelIndex);
        switch (item.getHintType()) {
            case CARD:
            case CARD_DUNGEON:
            case GAME_OBJECT: {
                // as popup card
                if (lastModelIndex != modelIndex) {
                    // NEW
                    if (item.getHintType() == ChoiceHintType.CARD) {
                        // as card name
                        cardInfo.init(item.getHint(), this.bigCard, this.gameId);
                    } else if (item.getHintType() == ChoiceHintType.CARD_DUNGEON) {
                        // as dungeon name
                        CardView cardView = new CardView(new DungeonView(Dungeon.createDungeon(item.getHint(), true)));
                        cardInfo.init(cardView, this.bigCard, this.gameId);
                    } else if (item.getHintType() == ChoiceHintType.GAME_OBJECT) {
                        // as object
                        GamePanel game = MageFrame.getGame(this.gameId);
                        if (game != null) {
                            UUID objectId = UUID.fromString(item.getHint());
                            CardView cardView = game.getLastGameData().findCard(objectId);
                            if (cardView != null) {
                                cardInfo.init(cardView, this.bigCard, this.gameId);
                            }
                        }
                    }
                    cardInfo.setPopupAutoLocationMode(TransferData.PopupAutoLocationMode.PUT_NEAR_MOUSE_POSITION);
                    cardInfo.onMouseEntered(MouseInfo.getPointerInfo().getLocation());
                } else {
                    // OLD - keep it opened
                    cardInfo.onMouseMoved(MouseInfo.getPointerInfo().getLocation());
                }
                lastModelIndex = modelIndex;
                break;
            }

            case TEXT: {
                // as popup text
                if (lastModelIndex != modelIndex) {
                    String hint = item.getHint();
                    hint = ManaSymbols.replaceSymbolsWithHTML(hint, ManaSymbols.Type.DIALOG);
                    hint = GUISizeHelper.textToHtmlWithSize(hint, listChoices.getFont());
                    listChoices.setToolTipText(hint);
                }
                lastModelIndex = modelIndex;
                break;
            }

            default: {
                throw new IllegalArgumentException("Unsupported hint type " + item.getHintType());
            }
        }
    }

    private void choiceHintHide() {
        cardInfo.onMouseExited();
        listChoices.setToolTipText(null);
        lastModelIndex = -1;
    }

    public void setWindowSize(int width, int height) {
        this.setSize(new Dimension(width, height));
    }

    @Override
    public void hideDialog() {
        choiceHintHide();
        super.hideDialog();
    }

    private void loadData() {
        // load data to datamodel after filter or on startup
        String filter = choice.getSearchText();
        if (filter == null) {
            filter = "";
        }
        filter = filter.toLowerCase(Locale.ENGLISH);

        // render fix: make changes to new model instead current, so it can help with GUI freeze on fast text deleting
        // https://github.com/magefree/mage/issues/8671
        DefaultListModel<KeyValueItem> newModel = new DefaultListModel<>();
        for (KeyValueItem item : this.allItems) {
            if (!choice.isSearchEnabled() || item.getValue().toLowerCase(Locale.ENGLISH).contains(filter)) {
                newModel.addElement(item);
            }
        }

        this.listChoices.setModel(newModel);
    }

    private void setMessageText(MageEditorPane editor, String text) {
        editor.setGameData(this.gameId, this.bigCard);

        if ((text != null) && !text.equals("")) {
            String realText = ManaSymbols.replaceSymbolsWithHTML(text, ManaSymbols.Type.DIALOG);
            editor.setText(String.format(HTML_HEADERS_TEMPLATE, realText));
            editor.setVisible(true);
        } else {
            editor.setText("");
            editor.setVisible(false);
        }
    }

    private void doNextSelect() {
        int newSel = this.listChoices.getSelectedIndex() + 1;
        int maxSel = this.listChoices.getModel().getSize() - 1;
        if (newSel <= maxSel) {
            this.listChoices.setSelectedIndex(newSel);
            this.listChoices.ensureIndexIsVisible(newSel);
        }
    }

    private void doPrevSelect() {
        int newSel = this.listChoices.getSelectedIndex() - 1;
        if (newSel >= 0) {
            this.listChoices.setSelectedIndex(newSel);
            this.listChoices.ensureIndexIsVisible(newSel);
        }
    }

    private void doChoose() {
        if (setChoice()) {
            doClose();
        }
    }

    private void doCancel() {
        this.listChoices.clearSelection();
        this.choice.clearChoice();
        doClose();
    }

    private void doClose() {
        this.hideDialog();
        if (this.callback != null) {
            this.callback.onChoiceDone();
        }
    }

    public boolean setChoice() {
        KeyValueItem item = (KeyValueItem) this.listChoices.getSelectedValue();
        boolean isSpecial = choice.isSpecialEnabled() && cbSpecial.isSelected();

        // auto select one item (after incremental filtering)
        if ((item == null) && (this.listChoices.getModel().getSize() == 1)) {
            this.listChoices.setSelectedIndex(0);
            item = (KeyValueItem) this.listChoices.getSelectedValue();
        }

        if (item != null) {
            if (choice.isKeyChoice()) {
                choice.setChoiceByKey(item.getKey(), isSpecial);
            } else {
                choice.setChoice(item.getKey(), isSpecial);
            }
            return true;
        } else {
            // special choice can be empty
            if (choice.isSpecialEnabled() && choice.isSpecialCanBeEmpty()) {
                if (choice.isKeyChoice()) {
                    choice.setChoiceByKey(null, isSpecial);
                } else {
                    choice.setChoice(null, isSpecial);
                }
                return true;
            }

            // nothing to choose
            choice.clearChoice();
            return false;
        }
    }

    private static final class ChoiceCellRenderer extends JPanel implements ListCellRenderer<KeyValueItem> {

        private final JLabel label = new JLabel();

        ChoiceCellRenderer() {
            super(new BorderLayout());
            setOpaque(true);
            label.setOpaque(false);
            label.setVerticalAlignment(SwingConstants.CENTER);
            add(label, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends KeyValueItem> list,
                                                       KeyValueItem item, int index,
                                                       boolean selected, boolean focused) {
            int textWidth = Math.max(420, list.getWidth() - 44);
            String value = item == null || item.getValue() == null ? "" : item.getValue();
            String html = ManaSymbols.replaceSymbolsWithHTML(value, ManaSymbols.Type.TABLE);
            label.setText("<html><div style='width:" + textWidth + "px;'>" + html + "</div></html>");
            label.setFont(list.getFont());
            label.setForeground(selected ? new Color(235, 255, 240) : new Color(35, 40, 48));
            setBackground(selected ? new Color(42, 150, 82) : new Color(244, 246, 249));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected ? new Color(25, 105, 55) : new Color(205, 211, 220), 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            Insets insets = getInsets();
            Dimension labelSize = label.getPreferredSize();
            setPreferredSize(new Dimension(
                    Math.max(textWidth + insets.left + insets.right, list.getWidth()),
                    labelSize.height + insets.top + insets.bottom
            ));
            return this;
        }
    }
    static class KeyValueItem {

        protected final String key;
        protected String value;
        protected String valueAsHtml; // final html view
        protected String hint;
        protected ChoiceHintType hintType;

        public KeyValueItem(String key, String value, String hint, ChoiceHintType hintType) {
            this.key = key;
            this.value = value;
            this.valueAsHtml = "<html>" + ManaSymbols.replaceSymbolsWithHTML(value, ManaSymbols.Type.TABLE);
            this.hint = hint;
            this.hintType = hintType;
        }

        public String getKey() {
            return this.key;
        }

        public String getValue() {
            return this.value;
        }

        public String getValueAsHtml() {
            return this.valueAsHtml;
        }

        public String getHint() {
            return this.hint;
        }

        public ChoiceHintType getHintType() {
            return this.hintType;
        }

        @Override
        public String toString() {
            return valueAsHtml;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelHeader = new javax.swing.JPanel();
        textMessage = new mage.client.components.MageEditorPane();
        textSubMessage = new mage.client.components.MageEditorPane();
        panelSearch = new javax.swing.JPanel();
        labelSearch = new javax.swing.JLabel();
        editSearch = new javax.swing.JTextField();
        scrollList = new javax.swing.JScrollPane();
        listChoices = new javax.swing.JList();
        panelCommands = new javax.swing.JPanel();
        btOK = new javax.swing.JButton();
        btCancel = new javax.swing.JButton();
        cbSpecial = new javax.swing.JCheckBox();

        setResizable(true);

        panelHeader.setLayout(new java.awt.BorderLayout());

        textMessage.setEditable(false);
        textMessage.setBorder(null);
        textMessage.setText("<html><div style='text-align: center;'>example long message example long message example long message example long message example long message</div></html>");
        textMessage.setAutoscrolls(false);
        textMessage.setFocusable(false);
        textMessage.setOpaque(false);
        panelHeader.add(textMessage, java.awt.BorderLayout.CENTER);

        textSubMessage.setEditable(false);
        textSubMessage.setBorder(null);
        textSubMessage.setText("<html><div style='text-align: center;'>example long message example long</div></html>");
        textSubMessage.setAutoscrolls(false);
        textSubMessage.setFocusable(false);
        textSubMessage.setOpaque(false);
        panelHeader.add(textSubMessage, java.awt.BorderLayout.SOUTH);

        labelSearch.setText("Search:");

        editSearch.setText("sample search text");

        javax.swing.GroupLayout panelSearchLayout = new javax.swing.GroupLayout(panelSearch);
        panelSearch.setLayout(panelSearchLayout);
        panelSearchLayout.setHorizontalGroup(
                panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelSearchLayout.createSequentialGroup()
                                .addGap(0, 0, 0)
                                .addComponent(labelSearch)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(editSearch)
                                .addGap(0, 0, 0))
        );
        panelSearchLayout.setVerticalGroup(
                panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelSearchLayout.createSequentialGroup()
                                .addGap(3, 3, 3)
                                .addGroup(panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(labelSearch)
                                        .addComponent(editSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(3, 3, 3))
        );

        listChoices.setModel(new javax.swing.AbstractListModel() {
            String[] strings = {"item1", "item2", "item3"};

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        scrollList.setViewportView(listChoices);

        btOK.setText("Choose");
        btOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btOKActionPerformed(evt);
            }
        });

        btCancel.setText("Cancel");
        btCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btCancelActionPerformed(evt);
            }
        });

        cbSpecial.setText("Remember choose");

        javax.swing.GroupLayout panelCommandsLayout = new javax.swing.GroupLayout(panelCommands);
        panelCommands.setLayout(panelCommandsLayout);
        panelCommandsLayout.setHorizontalGroup(
                panelCommandsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelCommandsLayout.createSequentialGroup()
                                .addComponent(cbSpecial)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btOK)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        panelCommandsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[]{btCancel, btOK});

        panelCommandsLayout.setVerticalGroup(
                panelCommandsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelCommandsLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panelCommandsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btCancel)
                                        .addComponent(btOK)
                                        .addComponent(cbSpecial))
                                .addContainerGap())
        );

        getRootPane().setDefaultButton(btOK);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(scrollList, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(panelCommands, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelHeader, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE)
                                        .addComponent(panelSearch, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollList, javax.swing.GroupLayout.DEFAULT_SIZE, 268, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelCommands, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btOKActionPerformed
        doChoose();
    }//GEN-LAST:event_btOKActionPerformed

    private void btCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btCancelActionPerformed
        doCancel();
    }//GEN-LAST:event_btCancelActionPerformed

    /**
     * Closes the dialog
     */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        doCancel();
    }//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btCancel;
    private javax.swing.JButton btOK;
    private javax.swing.JCheckBox cbSpecial;
    private javax.swing.JTextField editSearch;
    private javax.swing.JLabel labelSearch;
    private javax.swing.JList listChoices;
    private javax.swing.JPanel panelCommands;
    private javax.swing.JPanel panelHeader;
    private javax.swing.JPanel panelSearch;
    private javax.swing.JScrollPane scrollList;
    private mage.client.components.MageEditorPane textMessage;
    private mage.client.components.MageEditorPane textSubMessage;
    // End of variables declaration//GEN-END:variables
}
