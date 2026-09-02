package mage.client.game;

import mage.client.SessionHandler;
import mage.client.cards.BigCard;
import mage.client.components.MageTextArea;
import mage.client.constants.Constants;
import mage.client.dialog.PreferencesDialog;
import mage.client.game.FeedbackPanel.FeedbackMode;
import mage.client.util.AppUtil;
import mage.client.util.GUISizeHelper;
import mage.client.util.audio.AudioManager;
import mage.constants.TurnPhase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static mage.client.game.FeedbackPanel.FeedbackMode.QUESTION;
import static mage.constants.PlayerAction.*;

/**
 * Game GUI: helper component for feedback panel - implements all feedback logic here
 *
 * @author ayrat, JayDi85
 */
public class HelperPanel extends JPanel {

    private static final Color DECISION_SURFACE_TOP = new Color(28, 32, 40, 248);
    private static final Color DECISION_SURFACE_BOTTOM = new Color(12, 15, 21, 248);
    private static final Color DECISION_BORDER = new Color(91, 103, 122, 210);
    private static final int DECISION_SURFACE_WIDTH = 760;
    private static final int DECISION_SURFACE_HORIZONTAL_PADDING = 16;
    private static final int DECISION_SURFACE_VERTICAL_PADDING = 8;
    private static final int DECISION_FOOTER_HEIGHT = 48;
    private static final int DECISION_MIN_TEXT_HEIGHT = 48;
    private static final int DECISION_SURFACE_OUTER_VERTICAL_GAP = 6;

    private javax.swing.JButton btnLeft;
    private javax.swing.JButton btnRight;
    private javax.swing.JButton btnSpecial;
    private javax.swing.JButton btnUndo;

    private JScrollPane textAreaScrollPane;
    private MageTextArea dialogTextArea;
    JPanel mainPanel;
    JPanel buttonGrid;
    JPanel buttonContainer;
    private JPanel decisionSurface;

    private javax.swing.JButton linkLeft;
    private javax.swing.JButton linkRight;
    private javax.swing.JButton linkSpecial;
    private javax.swing.JButton linkUndo;

    private final Object tooltipBackground = UIManager.get("info");

    private static final String CMD_AUTO_ANSWER_ID_YES = "cmdAutoAnswerIdYes";
    private static final String CMD_AUTO_ANSWER_ID_NO = "cmdAutoAnswerIdNo";
    private static final String CMD_AUTO_ANSWER_NAME_YES = "cmdAutoAnswerNameYes";
    private static final String CMD_AUTO_ANSWER_NAME_NO = "cmdAutoAnswerNameNo";
    private static final String CMD_AUTO_ANSWER_RESET_ALL = "cmdAutoAnswerResetAll";

    // popup menu for keep auto-answer in yes/no dialogs
    private JPopupMenu popupMenuAskYes;
    private JMenuItem popupItemYesAsText;
    private JMenuItem popupItemYesAsTextAndAbility;
    private JPopupMenu popupMenuAskNo;
    private JMenuItem popupItemNoAsText;
    private JMenuItem popupItemNoAsTextAndAbility;

    // originalId of feedback causing ability
    private UUID originalId;
    private String basicMessage; // normal size
    private String secondaryMessage; // smaller size
    private String autoAnswerMessage; // Filtered version of message which is used for remembering answers to text

    private UUID gameId;
    private boolean gameNeedFeedback = false;
    private TurnPhase gameTurnPhase = null;
    private String turnPlayerName;
    private int turnNumber;
    private boolean ownTurn;
    private JLabel turnBadge;

    private Timer needFeedbackTimer;

    {
        // start timer to inform user about needed feedback (example: inform by sound play)
        needFeedbackTimer = new Timer(100, evt -> SwingUtilities.invokeLater(() -> {
            needFeedbackTimer.stop();
            if (!AppUtil.isAppActive() || !AppUtil.isGameActive(this.gameId)) {
                // sound notification
                AudioManager.playFeedbackNeeded();
                // tray notification (baloon + icon blinking)
                //MageTray.instance.displayMessage("Game needs your action.");
                //MageTray.instance.blink();
            }
        }));
    }

    public HelperPanel() {
        initComponents();
    }

    public void init(UUID gameId, BigCard bigCard) {
        this.gameId = gameId;
        this.dialogTextArea.setGameData(gameId, bigCard);
    }

    public void changeGUISize() {
        setGUISize();
    }

    private void setGUISize() {
        //this.setMaximumSize(new Dimension(getParent().getWidth(), Integer.MAX_VALUE));
        int contentWidth = DECISION_SURFACE_WIDTH - 2 * DECISION_SURFACE_HORIZONTAL_PADDING;
        // The fixed decision surface owns the available height. Let the HTML
        // view keep its natural preferred size so its text is laid out before
        // the viewport clips it; the scrollbar itself remains disabled below.
        dialogTextArea.setMinimumSize(new Dimension(0, 0));
        dialogTextArea.setPreferredSize(null);
        textAreaScrollPane.setMaximumSize(new Dimension(contentWidth, GUISizeHelper.gameFeedbackPanelMaxHeight));
        textAreaScrollPane.setPreferredSize(new Dimension(contentWidth, GUISizeHelper.gameFeedbackPanelMaxHeight));

        btnLeft.setFont(GUISizeHelper.gameFeedbackPanelFont);
        btnRight.setFont(GUISizeHelper.gameFeedbackPanelFont);
        btnSpecial.setFont(GUISizeHelper.gameFeedbackPanelFont);
        btnUndo.setFont(GUISizeHelper.gameFeedbackPanelFont);

        this.redrawMessages();

        this.buttonContainer.setPreferredSize(new Dimension(contentWidth, DECISION_FOOTER_HEIGHT));
        this.decisionSurface.setPreferredSize(new Dimension(DECISION_SURFACE_WIDTH, getDecisionSurfaceHeight()));
        autoSizeButtonsAndFeedbackState();

        GUISizeHelper.changePopupMenuFont(popupMenuAskNo);
        GUISizeHelper.changePopupMenuFont(popupMenuAskYes);
        revalidate();
        repaint();
    }

    private void initComponents() {
        initPopupMenuTriggerOrder();

        this.setLayout(new BorderLayout());
        this.setOpaque(false);
        this.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));

        mainPanel = new JPanel(new CenteredDecisionLayout());
        mainPanel.setOpaque(false);
        this.add(mainPanel, BorderLayout.CENTER);

        dialogTextArea = new MageTextArea();
        dialogTextArea.setText("<Empty>");
        dialogTextArea.setOpaque(false);

        textAreaScrollPane = new JScrollPane(dialogTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        textAreaScrollPane.setOpaque(false);
        textAreaScrollPane.setBackground(new Color(0, 0, 0, 0));
        textAreaScrollPane.getViewport().setOpaque(false);
        textAreaScrollPane.setBorder(null);
        textAreaScrollPane.setViewportBorder(null);

        turnBadge = new RoundedTurnBadge();
        turnBadge.setVisible(false);
        turnBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        turnBadge.setForeground(new Color(246, 248, 240));
        turnBadge.setHorizontalAlignment(SwingConstants.CENTER);
        turnBadge.setOpaque(false);
        turnBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        buttonGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonGrid.setOpaque(false);

        buttonContainer = new DecisionFooterPanel();
        buttonContainer.setOpaque(false);
        buttonContainer.setPreferredSize(new Dimension(
                DECISION_SURFACE_WIDTH - 2 * DECISION_SURFACE_HORIZONTAL_PADDING,
                DECISION_FOOTER_HEIGHT
        ));
        buttonContainer.add(buttonGrid);
        buttonContainer.add(turnBadge);

        decisionSurface = new RoundedDecisionSurface();
        decisionSurface.setLayout(new BorderLayout(0, 4));
        decisionSurface.setOpaque(false);
        decisionSurface.setBorder(BorderFactory.createEmptyBorder(
                DECISION_SURFACE_VERTICAL_PADDING,
                DECISION_SURFACE_HORIZONTAL_PADDING,
                DECISION_SURFACE_VERTICAL_PADDING,
                DECISION_SURFACE_HORIZONTAL_PADDING
        ));
        decisionSurface.setPreferredSize(new Dimension(DECISION_SURFACE_WIDTH, getDecisionSurfaceHeight()));
        decisionSurface.add(textAreaScrollPane, BorderLayout.CENTER);
        decisionSurface.add(buttonContainer, BorderLayout.SOUTH);
        mainPanel.add(decisionSurface);

        btnSpecial = new RoundedDecisionButton("Special");
        btnSpecial.setVisible(false);
        buttonGrid.add(btnSpecial);

        btnLeft = new RoundedDecisionButton("OK");
        btnLeft.setVisible(false);
        buttonGrid.add(btnLeft);

        btnRight = new RoundedDecisionButton("Cancel");
        btnRight.setVisible(false);
        buttonGrid.add(btnRight);

        btnUndo = new RoundedDecisionButton("Undo");
        btnUndo.setVisible(false);
        buttonGrid.add(btnUndo);

        styleDecisionButton(btnSpecial);
        styleDecisionButton(btnLeft);
        styleDecisionButton(btnRight);
        styleDecisionButton(btnUndo);

        MouseListener checkPopupAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkPopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopupMenu(e);
            }

        };

        btnLeft.addMouseListener(checkPopupAdapter);
        btnLeft.addActionListener(evt -> {
            if (linkLeft != null) {
                clickButton(linkLeft);
            }
        });

        btnRight.addMouseListener(checkPopupAdapter);
        btnRight.addActionListener(evt -> {
            if (linkRight != null) {
                clickButton(linkRight);
            }
        });

        btnSpecial.addActionListener(evt -> {
            if (linkSpecial != null) {
                clickButton(linkSpecial);
            }
        });

        btnUndo.addActionListener(evt -> {
            if (linkUndo != null) {
                {
                    Thread worker = new Thread(() -> SwingUtilities.invokeLater(() -> linkUndo.doClick()));
                    worker.start();
                }
            }
        });

        // sets a darker background and higher simiss time fur tooltip in the feedback / helper panel
        dialogTextArea.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                ToolTipManager.sharedInstance().setDismissDelay(100 * 1000);
                UIManager.put("info", Color.DARK_GRAY);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ToolTipManager.sharedInstance().setDismissDelay(Constants.TOOLTIPS_DELAY_MS);
                UIManager.put("info", tooltipBackground);
            }
        });
    }

    private void checkPopupMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            // allows any yes/no dialogs
            JButton source = (JButton) e.getSource();
            if (source.getActionCommand().startsWith(QUESTION.toString())) {
                showPopupMenu(e.getComponent(), source.getActionCommand());
                e.consume();
            }
        }
    }

    private void clickButton(final JButton button) {
        Thread worker = new Thread(() -> SwingUtilities.invokeLater(() -> {
            setState("", false, "", false, null);
            setSpecial("", false);
            button.doClick();
        }));
        worker.start();
    }

    public void setState(String txtLeft, boolean leftVisible, String txtRight, boolean rightVisible, FeedbackMode mode) {
        this.btnLeft.setVisible(leftVisible);
        if (!txtLeft.isEmpty()) {
            this.btnLeft.setText(txtLeft);
            if (mode != null) {
                this.btnLeft.setActionCommand(mode + txtLeft);
            }
        }

        this.btnRight.setVisible(rightVisible);
        if (!txtRight.isEmpty()) {
            this.btnRight.setText(txtRight);
            if (mode != null) {
                this.btnRight.setActionCommand(mode + txtRight);
            }
        }

        // auto-answer hints
        String buttonTooltip = null;
        if (mode == QUESTION) {
            buttonTooltip = "Right click on button to make auto-answer.";
        }
        this.btnLeft.setToolTipText(buttonTooltip);
        this.btnRight.setToolTipText(buttonTooltip);

        autoSizeButtonsAndFeedbackState();
    }

    public void setSpecial(String txtSpecial, boolean specialVisible) {
        this.btnSpecial.setVisible(specialVisible);
        this.btnSpecial.setText(txtSpecial);
    }

    public void setUndoEnabled(boolean enabled) {
        this.btnUndo.setVisible(enabled);
        autoSizeButtonsAndFeedbackState();
    }

    public void setLeft(String text, boolean visible) {
        this.btnLeft.setVisible(visible);
        if (!text.isEmpty()) {
            this.btnLeft.setText(text);
        }
        autoSizeButtonsAndFeedbackState();
    }

    public void setRight(String txtRight, boolean rightVisible) {
        this.btnRight.setVisible(rightVisible);
        if (!txtRight.isEmpty()) {
            this.btnRight.setText(txtRight);
        }
        autoSizeButtonsAndFeedbackState();
    }

    public void setGameNeedFeedback(boolean need, TurnPhase gameTurnPhase) {
        this.gameNeedFeedback = need;
        this.gameTurnPhase = gameTurnPhase;

        if (this.gameNeedFeedback) {
            // start notification sound timer
            this.needFeedbackTimer.restart();
        } else {
            // stop notification sound timer
            this.needFeedbackTimer.stop();
        }
    }

    public void autoSizeButtonsAndFeedbackState() {
        // two mode: same size for small texts (flow), different size for long texts (grid)
        // also colorize feedback panel on player's priority and enable sound notification

        int BUTTONS_H_GAP = 15;

        // cleanup current settings to default (flow layout - different sizes)
        this.buttonGrid.setLayout(new FlowLayout(FlowLayout.CENTER, BUTTONS_H_GAP, 0));
        this.buttonGrid.setPreferredSize(null);

        List<JButton> buttons = new ArrayList<>();
        if (this.btnSpecial.isVisible()) {
            buttons.add(this.btnSpecial);
        }
        if (this.btnLeft.isVisible()) {
            buttons.add(this.btnLeft);
        }
        if (this.btnRight.isVisible()) {
            buttons.add(this.btnRight);
        }
        if (this.btnUndo.isVisible()) {
            buttons.add(this.btnUndo);
        }

        // Keep the parent transparent: the decision surface is painted by this panel.
        this.mainPanel.setOpaque(false);
        this.mainPanel.setBackground(new Color(0, 0, 0, 0));

        this.buttonGrid.removeAll();
        if (buttons.isEmpty()) {
            this.buttonGrid.revalidate();
            this.buttonContainer.revalidate();
            this.decisionSurface.revalidate();
            this.mainPanel.revalidate();
            this.repaint();
            return;
        }

        for (JButton button : buttons) {
            this.buttonGrid.add(button);
        }

        // random text test (click to any objects to change)
        /*
        Integer i = 1 + RandomUtil.nextInt(50);
        String longText = i.toString() + "-";
        while (longText.length() < i) {longText += "a";}
        this.btnRight.setText(longText);
        //*/

        // Always use each control's natural width. GridLayout could assign a
        // smaller cell during a refresh and clip the first character of a label.
        this.buttonGrid.setLayout(new FlowLayout(FlowLayout.CENTER, BUTTONS_H_GAP, 0));
        this.buttonGrid.setPreferredSize(null);
        this.buttonGrid.revalidate();
        this.buttonContainer.revalidate();
        this.decisionSurface.revalidate();
        this.mainPanel.revalidate();
        this.revalidate();
        this.repaint();
    }

    private static int getDecisionSurfaceHeightForCurrentSize() {
        // The GamePanel is created before the first GUI-size pass on a fresh
        // client, so the global value can still be zero. Keep a real message
        // row in that case instead of allowing the footer to touch the edge.
        int textHeight = Math.max(DECISION_MIN_TEXT_HEIGHT, GUISizeHelper.gameFeedbackPanelMaxHeight);
        int footerHeight = DECISION_FOOTER_HEIGHT;
        int paddingHeight = 2 * DECISION_SURFACE_VERTICAL_PADDING + 4;
        return Math.max(86, textHeight + footerHeight + paddingHeight);
    }

    private int getDecisionSurfaceHeight() {
        return getDecisionSurfaceHeightForCurrentSize();
    }

    public static int getRequiredHeightForCurrentSize() {
        return getDecisionSurfaceHeightForCurrentSize() + 14 + 2 * DECISION_SURFACE_OUTER_VERTICAL_GAP;
    }

    public int getRequiredHeight() {
        Insets insets = getInsets();
        return getDecisionSurfaceHeight() + insets.top + insets.bottom + 2 * DECISION_SURFACE_OUTER_VERTICAL_GAP;
    }

    private static class CenteredDecisionLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component component) {
        }

        @Override
        public void removeLayoutComponent(Component component) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets insets = parent.getInsets();
            Dimension childSize = parent.getComponentCount() == 0
                    ? new Dimension()
                    : parent.getComponent(0).getPreferredSize();
            return new Dimension(
                    childSize.width + insets.left + insets.right,
                    childSize.height + insets.top + insets.bottom
            );
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Insets insets = parent.getInsets();
            Dimension childSize = parent.getComponentCount() == 0
                    ? new Dimension()
                    : parent.getComponent(0).getMinimumSize();
            return new Dimension(
                    childSize.width + insets.left + insets.right,
                    childSize.height + insets.top + insets.bottom
            );
        }

        @Override
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() == 0) {
                return;
            }
            Insets insets = parent.getInsets();
            Component child = parent.getComponent(0);
            Dimension preferred = child.getPreferredSize();
            int availableWidth = Math.max(0, parent.getWidth() - insets.left - insets.right);
            int availableHeight = Math.max(0, parent.getHeight() - insets.top - insets.bottom);
            int width = Math.min(preferred.width, availableWidth);
            int height = Math.min(preferred.height, availableHeight);
            int x = insets.left + Math.max(0, (availableWidth - width) / 2);
            int y = insets.top + Math.max(0, (availableHeight - height) / 2);
            child.setBounds(x, y, width, height);
        }
    }

    private class DecisionFooterPanel extends JPanel {

        DecisionFooterPanel() {
            setLayout(null);
        }

        @Override
        public void doLayout() {
            int width = getWidth();
            int height = getHeight();
            Dimension buttonsSize = buttonGrid.getPreferredSize();
            int buttonWidth = Math.min(buttonsSize.width, Math.max(0, width - 12));
            int buttonHeight = Math.min(buttonsSize.height, Math.max(0, height - 4));
            int buttonX = Math.max(0, (width - buttonWidth) / 2);
            int buttonY = Math.max(0, (height - buttonHeight) / 2);

            Dimension badgeSize = turnBadge.getPreferredSize();
            int badgeWidth = turnBadge.isVisible() ? Math.min(badgeSize.width, Math.max(0, width - 8)) : 0;
            int badgeHeight = turnBadge.isVisible() ? Math.min(badgeSize.height, Math.max(0, height - 4)) : 0;
            int badgeX = Math.max(0, width - badgeWidth - 6);
            int badgeY = Math.max(0, (height - badgeHeight) / 2);

            // Keep the badge in the corner without ever covering a control.
            if (badgeWidth > 0 && buttonX + buttonWidth + 8 > badgeX) {
                int availableButtonWidth = Math.max(0, badgeX - 8);
                buttonWidth = Math.min(buttonWidth, availableButtonWidth);
                buttonX = Math.max(0, (availableButtonWidth - buttonWidth) / 2);
            }

            buttonGrid.setBounds(buttonX, buttonY, buttonWidth, buttonHeight);
            turnBadge.setBounds(badgeX, badgeY, badgeWidth, badgeHeight);
        }
    }

    private static class RoundedDecisionSurface extends JPanel {

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Shape surface = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g.setPaint(new GradientPaint(0, 0, DECISION_SURFACE_TOP, 0, getHeight(), DECISION_SURFACE_BOTTOM));
                g.fill(surface);
                g.setColor(DECISION_BORDER);
                g.draw(surface);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static class RoundedTurnBadge extends JLabel {

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int width = getWidth() - 1;
                int height = getHeight() - 1;
                if (width > 0 && height > 0) {
                    g.setColor(getBackground());
                    g.fillRoundRect(0, 0, width, height, Math.min(height, 16), Math.min(height, 16));
                    g.setColor(new Color(113, 128, 151, 220));
                    g.drawRoundRect(0, 0, width, height, Math.min(height, 16), Math.min(height, 16));
                }
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static class RoundedDecisionButton extends JButton {

        RoundedDecisionButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setRolloverEnabled(true);
            setMargin(new Insets(5, 16, 5, 16));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            String text = getText();
            Font font = getFont();
            FontMetrics metrics = getFontMetrics(font);
            Insets insets = getInsets();
            int textWidth = text == null || text.isEmpty() ? 0 : metrics.stringWidth(text);
            int horizontalPadding = Math.max(32, insets.left + insets.right + 8);
            int width = Math.max(size.width, textWidth + horizontalPadding);
            int height = Math.max(size.height, Math.max(34, metrics.getHeight() + 12));
            return new Dimension(width, height);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int width = getWidth() - 1;
            int height = getHeight() - 1;
            if (width <= 0 || height <= 0) {
                return;
            }

            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isPressed() ? new Color(74, 91, 112) : new Color(58, 70, 88);
                Color bottom = getModel().isPressed() ? new Color(43, 54, 70) : new Color(35, 43, 56);
                if (getModel().isRollover() && !getModel().isPressed()) {
                    top = new Color(72, 86, 107);
                    bottom = new Color(45, 56, 72);
                }
                if (!isEnabled()) {
                    top = new Color(54, 60, 70);
                    bottom = new Color(39, 44, 53);
                }
                int arc = Math.min(height, 20);
                g.setPaint(new GradientPaint(0, 0, top, 0, height, bottom));
                g.fillRoundRect(0, 0, width, height, arc, arc);
            } finally {
                g.dispose();
            }

            String text = getText();
            if (text == null || text.isEmpty()) {
                return;
            }
            Graphics2D textGraphics = (Graphics2D) graphics.create();
            try {
                textGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Font textFont = getFont();
                Insets insets = getInsets();
                int availableWidth = Math.max(1, getWidth() - insets.left - insets.right - 4);
                FontMetrics metrics = textGraphics.getFontMetrics(textFont);
                float fontSize = textFont.getSize2D();
                while (metrics.stringWidth(text) > availableWidth && fontSize > 8.0f) {
                    fontSize -= 0.5f;
                    textFont = textFont.deriveFont(fontSize);
                    metrics = textGraphics.getFontMetrics(textFont);
                }
                textGraphics.setFont(textFont);
                textGraphics.setColor(isEnabled() ? getForeground() : new Color(145, 151, 163));
                int textWidth = metrics.stringWidth(text);
                int x = Math.max(0, (getWidth() - textWidth) / 2);
                int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                textGraphics.drawString(text, x, y);
            } finally {
                textGraphics.dispose();
            }
        }

        @Override
        protected void paintBorder(Graphics graphics) {
            int width = getWidth() - 1;
            int height = getHeight() - 1;
            if (width <= 0 || height <= 0) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(isFocusOwner() ? new Color(224, 232, 244, 235) : new Color(133, 149, 174, 225));
                int arc = Math.min(height, 20);
                g.drawRoundRect(0, 0, width, height, arc, arc);
            } finally {
                g.dispose();
            }
        }
    }

    private void styleDecisionButton(JButton button) {
        button.setFocusPainted(false);
        button.setFocusable(true);
        button.setForeground(new Color(238, 242, 248));
        button.setBackground(new Color(48, 56, 70));
        button.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
    }



    public void setLinks(JButton left, JButton right, JButton special, JButton undo) {
        this.linkLeft = left;
        this.linkRight = right;
        this.linkSpecial = special;
        this.linkUndo = undo;
    }

    public void setOriginalId(UUID originalId) {
        this.originalId = originalId;
    }

    public void setTurnInfo(String playerName, int turnNumber, boolean ownTurn) {
        this.turnPlayerName = playerName;
        this.turnNumber = turnNumber;
        this.ownTurn = ownTurn;
        boolean validTurnInfo = playerName != null && !playerName.isEmpty() && turnNumber > 0;
        if (turnBadge != null) {
            turnBadge.setText(validTurnInfo ? "Turn " + turnNumber + " · " + playerName : "");
            if (validTurnInfo) {
                turnBadge.setBackground(ownTurn ? new Color(38, 105, 68, 235) : new Color(122, 102, 35, 235));
            }
            turnBadge.setVisible(validTurnInfo);
        }
        revalidate();
        repaint();
    }

    public void setMessages(String basicMessage, String secondaryMessage) {
        this.basicMessage = basicMessage;
        this.secondaryMessage = secondaryMessage;
        redrawMessages();
    }

    private void redrawMessages() {
        String panelText = this.basicMessage;
        if (this.secondaryMessage != null) {
            panelText += "<div style='font-size:" + GUISizeHelper.gameFeedbackPanelExtraMessageFontSize + "pt'>" + secondaryMessage + "</div>";
        }
        this.dialogTextArea.setText(panelText, DECISION_SURFACE_WIDTH - 2 * DECISION_SURFACE_HORIZONTAL_PADDING);
    }

    public void setAutoAnswerMessage(String autoAnswerMessage) {
        this.autoAnswerMessage = autoAnswerMessage;
    }

    @Override
    public void requestFocus() {
        this.btnRight.requestFocus();
    }

    private void initPopupMenuTriggerOrder() {

        ActionListener actionListener = e -> handleAutoAnswerPopupMenuEvent(e);

        popupMenuAskYes = new JPopupMenu();
        popupMenuAskNo = new JPopupMenu();

        // String tooltipText = "";
        popupItemYesAsTextAndAbility = new JMenuItem("Auto-answer YES for the same TEXT and ABILITY");
        popupItemYesAsTextAndAbility.setActionCommand(CMD_AUTO_ANSWER_ID_YES);
        popupItemYesAsTextAndAbility.addActionListener(actionListener);
        popupItemYesAsTextAndAbility.setToolTipText("<HTML>If the same question from the same ability would<br/>be asked again, it's automatically answered with <b>Yes</b>.<br/>You can reset it by battlefield right click menu.");
        popupMenuAskYes.add(popupItemYesAsTextAndAbility);

        popupItemNoAsTextAndAbility = new JMenuItem("Auto-answer NO for the same TEXT and ABILITY");
        popupItemNoAsTextAndAbility.setActionCommand(CMD_AUTO_ANSWER_ID_NO);
        popupItemNoAsTextAndAbility.setToolTipText("<HTML>If the same question from the same ability would<br/>"
                + "be asked again, it's automatically answered with <b>No</b>.<br/>"
                + "You can reset it by battlefield right click menu.");
        popupItemNoAsTextAndAbility.addActionListener(actionListener);
        popupMenuAskNo.add(popupItemNoAsTextAndAbility);

        popupItemYesAsText = new JMenuItem("Auto-answer YES for the same TEXT");
        popupItemYesAsText.setActionCommand(CMD_AUTO_ANSWER_NAME_YES);
        popupItemYesAsText.setToolTipText("<HTML>If the same question would be asked again (regardless from which source),<br/>"
                + "it's automatically answered with <b>Yes</b>.<br/>"
                + "You can reset it by battlefield right click menu.");
        popupItemYesAsText.addActionListener(actionListener);
        popupMenuAskYes.add(popupItemYesAsText);

        popupItemNoAsText = new JMenuItem("Auto-answer NO for the same TEXT");
        popupItemNoAsText.setActionCommand(CMD_AUTO_ANSWER_NAME_NO);
        popupItemNoAsText.setToolTipText("<HTML>If the same question would be asked again (regardless from which source),<br/>"
                + "it's automatically answered with <b>No</b>.<br/>"
                + "You can reset it by battlefield right click menu.");
        popupItemNoAsText.addActionListener(actionListener);
        popupMenuAskNo.add(popupItemNoAsText);

        JMenuItem menuItem = new JMenuItem("Reset all YES/NO auto-answers");
        menuItem.setActionCommand(CMD_AUTO_ANSWER_RESET_ALL);
        menuItem.addActionListener(actionListener);
        popupMenuAskYes.add(menuItem);

        menuItem = new JMenuItem("Reset all YES/NO auto-answers");
        menuItem.setActionCommand(CMD_AUTO_ANSWER_RESET_ALL);
        menuItem.addActionListener(actionListener);
        popupMenuAskNo.add(menuItem);
    }

    public void handleAutoAnswerPopupMenuEvent(ActionEvent e) {
        switch (e.getActionCommand()) {
            case CMD_AUTO_ANSWER_ID_YES:
                SessionHandler.sendPlayerAction(REQUEST_AUTO_ANSWER_ID_YES, gameId,
                        originalId.toString() + '#' + autoAnswerMessage);
                clickButton(btnLeft);
                break;
            case CMD_AUTO_ANSWER_ID_NO:
                SessionHandler.sendPlayerAction(REQUEST_AUTO_ANSWER_ID_NO, gameId,
                        originalId.toString() + '#' + autoAnswerMessage);
                clickButton(btnRight);
                break;
            case CMD_AUTO_ANSWER_NAME_YES:
                SessionHandler.sendPlayerAction(REQUEST_AUTO_ANSWER_TEXT_YES, gameId,
                        autoAnswerMessage);
                clickButton(btnLeft);
                break;
            case CMD_AUTO_ANSWER_NAME_NO:
                SessionHandler.sendPlayerAction(REQUEST_AUTO_ANSWER_TEXT_NO, gameId,
                        autoAnswerMessage);
                clickButton(btnRight);
                break;
            case CMD_AUTO_ANSWER_RESET_ALL:
                SessionHandler.sendPlayerAction(REQUEST_AUTO_ANSWER_RESET_ALL, gameId, null);
                break;
            default:
                break;
        }
    }

    private void showPopupMenu(Component callingComponent, String actionCommand) {
        // keep auto-answer for yes/no

        // two modes:
        // - remember text for all (example: commander zone change);
        // - remember text + ability for source only (example: any optional ability)

        // yes
        popupItemYesAsText.setEnabled(true);
        popupItemYesAsTextAndAbility.setEnabled(originalId != null);
        popupItemYesAsText.setEnabled(true);
        popupItemYesAsTextAndAbility.setEnabled(originalId != null);
        // no
        popupItemNoAsText.setEnabled(true);
        popupItemNoAsTextAndAbility.setEnabled(originalId != null);
        popupItemNoAsText.setEnabled(true);
        popupItemNoAsTextAndAbility.setEnabled(originalId != null);

        Point p = callingComponent.getLocationOnScreen();
        // Show the JPopupMenu via program
        // Parameter desc
        // ----------------
        // this - represents current frame
        // 0,0 is the coordinate where the popup
        // is shown
        JPopupMenu menu;
        if (actionCommand.endsWith("Yes")) {
            menu = popupMenuAskYes;
        } else {
            menu = popupMenuAskNo;
        }
        menu.show(this, 0, 0);

        // Now set the location of the JPopupMenu
        // This location is relative to the screen
        menu.setLocation(p.x, p.y + callingComponent.getHeight());
    }
}
