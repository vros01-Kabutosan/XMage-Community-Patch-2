/*
 * Decompiled with CFR.
 */
package mage.client.util.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import mage.client.MageFrame;
import mage.client.components.MageComponents;
import mage.client.dialog.PreferencesDialog;
import mage.client.table.PlayersChatPanel;
import mage.client.util.GUISizeHelper;
import mage.constants.CardType;
import mage.constants.MageObjectType;
import mage.constants.Rarity;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.view.CardView;
import mage.view.CounterView;
import mage.view.PermanentView;
import net.java.truevfs.access.TFile;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXPanel;
import org.mage.card.arcane.ManaSymbols;
import org.mage.card.arcane.UI;
import org.mage.plugins.card.utils.CardImageUtils;

public final class GuiDisplayUtil {
    private static final Logger logger = Logger.getLogger(GuiDisplayUtil.class);
    private static final Font cardNameFont = new Font("Calibri", 1, 15);
    private static final Insets DEFAULT_INSETS = new Insets(0, 0, 70, 25);
    private static final Insets COMPONENT_INSETS = new Insets(0, 0, 40, 40);
    static final int OVERLAP_LIMIT = 10;

    public static void restoreDividerLocations(Rectangle bounds, String lastDividerLocation, JComponent component) {
        String currentBounds = Double.toString(bounds.getWidth()) + 'x' + bounds.getHeight();
        String savedBounds = PreferencesDialog.getCachedValue("gamepanelLastSize", null);
        if (savedBounds != null && savedBounds.equals(currentBounds) && lastDividerLocation != null && component != null) {
            if (component instanceof JSplitPane) {
                JSplitPane jSplitPane = (JSplitPane)component;
                jSplitPane.setDividerLocation(Integer.parseInt(lastDividerLocation));
            }
            if (component instanceof PlayersChatPanel) {
                PlayersChatPanel playerChatPanel = (PlayersChatPanel)component;
                playerChatPanel.setSplitDividerLocation(Integer.parseInt(lastDividerLocation));
            }
        }
    }

    public static void saveCurrentBoundsToPrefs() {
        Rectangle rec = MageFrame.getDesktop().getBounds();
        String currentBounds = Double.toString(rec.getWidth()) + 'x' + rec.getHeight();
        PreferencesDialog.saveValue("gamepanelLastSize", currentBounds);
    }

    public static void saveDividerLocationToPrefs(String dividerPrefKey, int position) {
        PreferencesDialog.saveValue(dividerPrefKey, Integer.toString(position));
    }

    public static JXPanel getDescription(CardView card, int width, int height) {
        JXPanel descriptionPanel = new JXPanel();
        descriptionPanel.setBounds(0, 0, width, height);
        descriptionPanel.setVisible(false);
        descriptionPanel.setLayout(null);
        JButton j = new JButton("");
        j.setBounds(0, 0, width, height);
        j.setBackground(Color.black);
        j.setLayout(null);
        JLabel cardText = new JLabel();
        cardText.setBounds(5, 5, width - 10, height - 10);
        cardText.setForeground(Color.white);
        cardText.setFont(cardNameFont);
        cardText.setVerticalAlignment(1);
        j.add(cardText);
        TextLines textLines = GuiDisplayUtil.getTextLinesfromCardView(card);
        cardText.setText(GuiDisplayUtil.getRulesFromCardView(card, textLines).toString());
        descriptionPanel.add((Component)j);
        return descriptionPanel;
    }

    public static String cleanString(String in) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < in.length(); ++i) {
            char c = in.charAt(i);
            if (c == ' ' || c == '-') {
                out.append('_');
                continue;
            }
            if (!Character.isLetterOrDigit(c)) continue;
            out.append(c);
        }
        return out.toString().toLowerCase(Locale.ENGLISH);
    }

    public static void keepComponentInsideScreen(int centerX, int centerY, Component component) {
        Dimension screenDim = component.getToolkit().getScreenSize();
        GraphicsConfiguration g = component.getGraphicsConfiguration();
        if (g != null) {
            Insets insets = component.getToolkit().getScreenInsets(g);
            boolean setLocation = false;
            if (centerX + component.getWidth() > screenDim.width - insets.right) {
                centerX = screenDim.width - insets.right - component.getWidth();
                setLocation = true;
            } else if (centerX < insets.left) {
                centerX = insets.left;
                setLocation = true;
            }
            if (centerY + component.getHeight() > screenDim.height - insets.bottom) {
                centerY = screenDim.height - insets.bottom - component.getHeight();
                setLocation = true;
            } else if (centerY < insets.top) {
                centerY = insets.top;
                setLocation = true;
            }
            if (setLocation) {
                component.setLocation(centerX, centerY);
            }
        } else {
            System.out.println("GuiDisplayUtil::keepComponentInsideScreen -> no GraphicsConfiguration");
        }
    }

    public static void keepComponentInsideFrame(int centerX, int centerY, Component component) {
        Rectangle frameRec = MageFrame.getInstance().getBounds();
        boolean setLocation = false;
        if (component.getX() > frameRec.width - 10) {
            setLocation = true;
        }
        if (component.getY() > frameRec.height - 10) {
            setLocation = true;
        }
        if (setLocation) {
            component.setLocation(centerX, centerY);
        }
    }

    public static Point keepComponentInsideParent(Point l, Point parentPoint, Component c, Component parent) {
        int dx = parentPoint.x + parent.getWidth() - GuiDisplayUtil.DEFAULT_INSETS.right - GuiDisplayUtil.COMPONENT_INSETS.right;
        if (l.x + c.getWidth() > dx) {
            l.x = dx - c.getWidth();
        }
        int dy = parentPoint.y + parent.getHeight() - GuiDisplayUtil.DEFAULT_INSETS.bottom - GuiDisplayUtil.COMPONENT_INSETS.bottom;
        if (l.y + c.getHeight() > dy) {
            l.y = Math.max(10, dy - c.getHeight());
        }
        return l;
    }

    public static TextLines getTextLinesfromCardView(CardView card) {
        int damage;
        TextLines textLines = new TextLines();
        textLines.setLines(new ArrayList<String>(card.getRules()));
        for (String rule : card.getRules()) {
            textLines.setBasicTextLength(textLines.getBasicTextLength() + rule.length());
        }
        if (card.getMageObjectType().canHaveCounters()) {
            ArrayList<CounterView> counters = new ArrayList<>();
            if (card.getCounters() != null) {
                counters.addAll(card.getCounters());
            }
            if (!counters.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int index = 0;
                for (CounterView counter : counters) {
                    if (counter.getCount() <= 0) continue;
                    if (index == 0) {
                        sb.append("<b>Counters:</b> ");
                    } else {
                        sb.append(", ");
                    }
                    sb.append(counter.getCount()).append(" x <i>").append(counter.getName()).append("</i>");
                    ++index;
                }
                textLines.getLines().add(sb.toString());
                textLines.setBasicTextLength(textLines.getBasicTextLength() + 50);
            }
        }
        if (card.getMageObjectType().isPermanent() && card instanceof PermanentView && (damage = ((PermanentView)card).getDamage()) > 0) {
            textLines.getLines().add("<span color='red'><b>Damage dealt:</b> " + damage + "</span>");
            textLines.setBasicTextLength(textLines.getBasicTextLength() + 50);
        }
        return textLines;
    }

    public static String getHintIconHtml(String iconName, int symbolSize) {
        return "<img src='" + GuiDisplayUtil.getResourcePath("hint/" + iconName + ".png") + "' alt='" + iconName + "' width=" + symbolSize + " height=" + symbolSize + ">";
    }

    public static StringBuilder getRulesFromCardView(CardView card, TextLines textLines) {
        String imageFile;
        boolean displayFullImagePath;
        Zone zone;
        String legal;
        String rarity;
        String manaCost = card.getManaCostStr();
        String castingCost = UI.getDisplayManaCost(manaCost);
        String fontFamily = "tahoma";
        int fontSize = Math.max(15, Math.min(16, GUISizeHelper.cardTooltipFont.getSize()));
        int tooltipSymbolSize = Math.max(19, Math.min(22, fontSize + 5));
        int ruleLengthForWidth = textLines == null ? 0 : textLines.getBasicTextLength();
        int ruleLinesForWidth = textLines == null ? 0 : textLines.getLines().size();
        int tooltipTextWidth = 455;
        if (ruleLengthForWidth > 180 || ruleLinesForWidth > 3) {
            tooltipTextWidth += 38;
        }
        if (ruleLengthForWidth > 380 || ruleLinesForWidth > 6) {
            tooltipTextWidth += 48;
        }
        if (ruleLengthForWidth > 650 || ruleLinesForWidth > 9) {
            tooltipTextWidth += 52;
        }
        tooltipTextWidth = Math.max(455, Math.min(620, tooltipTextWidth));
        String tableOpen = "<table cellspacing=0 cellpadding=0 border=0 width='" + tooltipTextWidth + "'>";
        castingCost = ManaSymbols.replaceSymbolsWithHTML(castingCost, tooltipSymbolSize);
        int symbolCount = 0;
        int offset = 0;
        while ((offset = castingCost.indexOf("<img", offset) + 1) != 0) {
            ++symbolCount;
        }
        StringBuilder buffer = new StringBuilder(512);
        buffer.append("<html><body style='font-family:");
        buffer.append(fontFamily);
        buffer.append(";font-size:");
        buffer.append(fontSize);
        buffer.append("px;margin:0px 2px 0px 2px'>");
        buffer.append(tableOpen).append("<tr><td>");
        buffer.append("<table cellspacing=0 cellpadding=0 border=0 width='100%'>");
        buffer.append("<tr><td valign='top'><b>");
        buffer.append(card.getDisplayName());
        if (card.isGameObject()) {
            buffer.append(" [").append(card.getId().toString(), 0, 3).append(']');
        }
        buffer.append("</b></td><td align='right' valign='top' nowrap style='width:");
        buffer.append(Math.max(34, symbolCount * (tooltipSymbolSize + 3)));
        buffer.append("px;white-space:nowrap'>");
        if (!card.isSplitCard()) {
            buffer.append(castingCost);
        }
        buffer.append("</td></tr></table>");
        buffer.append("<table cellspacing=0 cellpadding=0 border=0 width='100%'><tr><td style='margin-left:1px'>");
        String imageSize = " width=" + tooltipSymbolSize + " height=" + tooltipSymbolSize + '>';
        if (card.getColor().isWhite()) {
            buffer.append("<img src='").append(GuiDisplayUtil.getResourcePath("card/color_ind_white.png")).append("' alt='W' ").append(imageSize);
        }
        if (card.getColor().isBlue()) {
            buffer.append("<img src='").append(GuiDisplayUtil.getResourcePath("card/color_ind_blue.png")).append("' alt='U' ").append(imageSize);
        }
        if (card.getColor().isBlack()) {
            buffer.append("<img src='").append(GuiDisplayUtil.getResourcePath("card/color_ind_black.png")).append("' alt='B' ").append(imageSize);
        }
        if (card.getColor().isRed()) {
            buffer.append("<img src='").append(GuiDisplayUtil.getResourcePath("card/color_ind_red.png")).append("' alt='R' ").append(imageSize);
        }
        if (card.getColor().isGreen()) {
            buffer.append("<img src='").append(GuiDisplayUtil.getResourcePath("card/color_ind_green.png")).append("' alt='G' ").append(imageSize);
        }
        if (!card.getColor().isColorless()) {
            buffer.append("&nbsp;&nbsp;");
        }
        buffer.append(GuiDisplayUtil.getTypes(card));
        buffer.append("</td><td align='right' nowrap style='width:188px;white-space:nowrap'>");
        if (card.getRarity() == null) {
            rarity = Rarity.COMMON.getCode();
            buffer.append("<b color='black'>");
        } else {
            switch (card.getRarity()) {
                case RARE: {
                    buffer.append("<b color='#FFBF00'>");
                    break;
                }
                case UNCOMMON: {
                    buffer.append("<b color='silver'>");
                    break;
                }
                case COMMON: {
                    buffer.append("<b color='black'>");
                    break;
                }
                case MYTHIC: {
                    buffer.append("<b color='#D5330B'>");
                }
            }
            rarity = card.getRarity().getCode();
        }
        if (card.getExpansionSetCode() != null) {
            buffer.append(ManaSymbols.replaceSetCodeWithHTML(card.getExpansionSetCode().toUpperCase(Locale.ENGLISH), rarity, tooltipSymbolSize));
        }
        buffer.append("</b></td></tr></table>");
        String pt = card.isCreature() ? card.getPower() + '/' + card.getToughness() : (card.showPT() ? "<span color='gray'>(" + card.getPower() + '/' + card.getToughness() + ")</span>" : (card.isPlaneswalker() ? card.getLoyalty() : (card.isBattle() ? card.getDefense() : "")));
        buffer.append("<p style='margin:2px 2px 1px 1px'>");
        if (!pt.isEmpty()) {
            buffer.append("<b>").append(pt).append("</b>");
        }
        if (!card.isControlledByOwner()) {
            buffer.append("&nbsp;&nbsp;");
            if (card instanceof PermanentView) {
                buffer.append('[').append(((PermanentView)card).getNameOwner()).append("]");
            } else {
                buffer.append("[only controlled]");
            }
        }
        if (card instanceof PermanentView && ((PermanentView)card).isAttachedToDifferentlyControlledPermanent()) {
            buffer.append("&nbsp;").append('(').append(((PermanentView)card).getNameController()).append(")");
        }
        if (card.getMageObjectType() != MageObjectType.NULL) {
            if (!pt.isEmpty()) {
                buffer.append("&nbsp;&nbsp;");
            }
            buffer.append("<font color='#555555'>").append(card.getMageObjectType().toString()).append("</font>");
        }
        buffer.append("</p>");
        HashSet<String> duplicatedRules = new HashSet<String>();
        StringBuilder rule = new StringBuilder("<br/>");
        if (card.isSplitCard()) {
            rule.append("<table cellspacing=0 cellpadding=0 border=0 width='100%'>");
            rule.append("<tr><td valign='top'><b>");
            rule.append(card.getLeftSplitName());
            rule.append("</b></td><td align='right' valign='top' nowrap style='width:");
            rule.append(ManaSymbols.getClearManaSymbolsCount(card.getLeftSplitCostsStr()) * (tooltipSymbolSize + 2) + 1);
            rule.append("px;white-space:nowrap'>");
            rule.append(ManaSymbols.replaceSymbolsWithHTML(card.getLeftSplitCostsStr(), tooltipSymbolSize));
            rule.append("</td></tr></table>");
            for (String ruling : card.getLeftSplitRules()) {
                if (ruling == null || ruling.replace(".", "").trim().isEmpty()) continue;
                duplicatedRules.add(ruling);
                rule.append("<p style='margin:2px 2px 2px 2px'>").append(GuiDisplayUtil.replaceNamesInRule(ruling, card.getLeftSplitName())).append("</p>");
            }
            rule.append("<table cellspacing=0 cellpadding=0 border=0 width='100%'>");
            rule.append("<tr><td valign='top'><b>");
            rule.append(card.getRightSplitName());
            rule.append("</b></td><td align='right' valign='top' nowrap style='width:");
            rule.append(ManaSymbols.getClearManaSymbolsCount(card.getRightSplitCostsStr()) * (tooltipSymbolSize + 2) + 1);
            rule.append("px;white-space:nowrap'>");
            rule.append(ManaSymbols.replaceSymbolsWithHTML(card.getRightSplitCostsStr(), tooltipSymbolSize));
            rule.append("</td></tr></table>");
            for (String ruling : card.getRightSplitRules()) {
                if (ruling == null || ruling.replace(".", "").trim().isEmpty()) continue;
                duplicatedRules.add(ruling);
                rule.append("<p style='margin:2px 2px 2px 2px'>").append(GuiDisplayUtil.replaceNamesInRule(ruling, card.getRightSplitName())).append("</p>");
            }
        }
        if (!textLines.getLines().isEmpty()) {
            for (String textLine : textLines.getLines()) {
                if (textLine == null || textLine.replace(".", "").trim().isEmpty() || duplicatedRules.contains(textLine)) continue;
                rule.append("<p style='margin:2px 2px 2px 2px'>").append(textLine).append("</p>");
            }
        }
        if (!(legal = rule.toString()).isEmpty()) {
            legal = GuiDisplayUtil.replaceNamesInRule(legal, card.getDisplayName());
            buffer.append(ManaSymbols.replaceSymbolsWithHTML(legal, tooltipSymbolSize));
        }
        if ((zone = card.getZone()) != null) {
            buffer.append("<p style='margin:2px 2px 2px 2px'><b>Card Zone:</b> ").append((Object)zone).append("</p>");
        }
        if ((displayFullImagePath = PreferencesDialog.getCachedValue("showFullImagePath", "false").equals("true")) && ((imageFile = CardImageUtils.buildImagePathToCardView(card)).startsWith("ERROR") || !new TFile(imageFile).exists())) {
            buffer.append("<p style='margin:2px 2px 2px 2px'><b>Missing image:</b> ").append(imageFile).append("</p>");
        }
        buffer.append("</td></tr></table></body></html>");
        return buffer;
    }

    private static String replaceNamesInRule(String rule, String cardName) {
        return rule.replaceAll("\\{this\\}", cardName.isEmpty() ? "this" : cardName);
    }

    private static String getResourcePath(String image) {
        return GuiDisplayUtil.class.getClassLoader().getResource(image).toString();
    }

    private static String getTypes(CardView card) {
        String types = "";
        for (SuperType superType : card.getSuperTypes()) {
            types = types + superType.toString() + ' ';
        }
        for (CardType cardType : card.getCardTypes()) {
            types = types + cardType.toString() + ' ';
        }
        if (!card.getSubTypes().isEmpty()) {
            types = types + "- ";
        }
        for (SubType subType : card.getSubTypes()) {
            types = types + (Object)((Object)subType) + " ";
        }
        return types.trim();
    }

    public static void setPanelEnabled(JPanel panel, Boolean isEnabled) {
        Component[] components;
        panel.setEnabled(isEnabled);
        for (Component component : components = panel.getComponents()) {
            if (component instanceof JPanel) {
                GuiDisplayUtil.setPanelEnabled((JPanel)component, isEnabled);
            }
            component.setEnabled(isEnabled);
        }
    }

    public static void refreshThemeSettings() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        }
        catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            logger.error((Object)("Can't apply current theme: " + (Object)((Object)PreferencesDialog.getCurrentTheme()) + " - " + e), (Throwable)e);
        }
        UIManager.put("nimbusBlueGrey", PreferencesDialog.getCurrentTheme().getNimbusBlueGrey());
        UIManager.put("control", PreferencesDialog.getCurrentTheme().getControl());
        UIManager.put("nimbusLightBackground", PreferencesDialog.getCurrentTheme().getNimbusLightBackground());
        UIManager.put("info", PreferencesDialog.getCurrentTheme().getInfo());
        UIManager.put("nimbusBase", PreferencesDialog.getCurrentTheme().getNimbusBase());
        UIManager.put("text", PreferencesDialog.getCurrentTheme().getTextColor());
        for (Frame frame : Frame.getFrames()) {
            GuiDisplayUtil.refreshLookAndFeel(frame);
        }
        Arrays.stream(MageComponents.values()).forEach(compName -> {
            try {
                Component comp = MageFrame.getUI().getComponent((MageComponents)((Object)compName), false);
                if (comp != null) {
                    SwingUtilities.updateComponentTreeUI(comp);
                }
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        });
    }

    private static void refreshLookAndFeel(Window window) {
        for (Window childWindow : window.getOwnedWindows()) {
            GuiDisplayUtil.refreshLookAndFeel(childWindow);
        }
        SwingUtilities.updateComponentTreeUI(window);
    }

    private static /* synthetic */ String lambda$refreshThemeSettings$0(Object key) {
        return key + " = " + UIManager.getLookAndFeel().getDefaults().get(key);
    }

    public static class TextLines {
        private int basicTextLength;
        private List<String> lines;

        public int getBasicTextLength() {
            return this.basicTextLength;
        }

        public void setBasicTextLength(int basicTextLength) {
            this.basicTextLength = basicTextLength;
        }

        public List<String> getLines() {
            return this.lines;
        }

        public void setLines(List<String> lines) {
            this.lines = lines;
        }
    }
}
