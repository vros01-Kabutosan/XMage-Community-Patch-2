/*
 * Decompiled with CFR.
 */
package org.mage.card.arcane;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.util.ArrayList;
import java.util.List;
import mage.client.dialog.PreferencesDialog;
import mage.constants.AbilityType;
import mage.constants.CardType;
import mage.constants.MageObjectType;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.view.CardView;
import mage.view.CounterView;
import mage.view.PermanentView;
import org.mage.card.arcane.CardPanelAttributes;
import org.mage.card.arcane.CardRendererUtils;
import org.mage.card.arcane.ManaSymbols;
import org.mage.card.arcane.TextboxRule;
import org.mage.card.arcane.TextboxRuleParser;
import org.mage.card.arcane.TextboxRuleType;

public abstract class CardRenderer {
    protected final CardView cardView;
    protected BufferedImage artImage;
    private static final Polygon PLUS_COUNTER_POLY = new Polygon(new int[]{0, 5, 10, 10, 5, 0}, new int[]{3, 0, 3, 10, 9, 10}, 6);
    private static final Polygon MINUS_COUNTER_POLY = new Polygon(new int[]{0, 5, 10, 10, 5, 0}, new int[]{0, 1, 0, 7, 10, 7}, 6);
    private static final Polygon TIME_COUNTER_POLY = new Polygon(new int[]{0, 10, 8, 10, 0, 2}, new int[]{0, 0, 5, 10, 10, 5}, 6);
    private static final Polygon OTHER_COUNTER_POLY = new Polygon(new int[]{1, 9, 9, 1}, new int[]{1, 1, 9, 9}, 4);
    public static final Paint BG_TEXTURE_CARDBACK = new Color(153, 102, 51);
    protected int cardWidth;
    protected int cardHeight;
    protected boolean isChoosable;
    protected boolean isSelected;
    protected static final float CORNER_RADIUS_FRAC = 0.1f;
    protected static final int CORNER_RADIUS_MIN = 3;
    protected int cornerRadius;
    protected static final float BORDER_WIDTH_FRAC = 0.03f;
    protected static final float BORDER_WIDTH_MIN = 2.0f;
    protected int borderWidth;
    protected ArrayList<TextboxRule> textboxRules = new ArrayList();
    protected ArrayList<TextboxRule> textboxKeywords = new ArrayList();
    private boolean lessOpaqueRulesTextBox = false;

    public CardRenderer(CardView card) {
        this.cardView = card;
        this.parseRules(card.getRules(), this.textboxKeywords, this.textboxRules);
    }

    protected void parseRules(List<String> stringRules, List<TextboxRule> keywords, List<TextboxRule> rules) {
        for (String rule : stringRules) {
            if (rule.equals("<br/><hintstart/>")) break;
            String realCardName = this.cardView.getDisplayName();
            if (this.cardView.isSplitCard()) {
                for (String partRule : this.cardView.getLeftSplitRules()) {
                    if (!partRule.equals(rule)) continue;
                    realCardName = this.cardView.getLeftSplitName();
                    break;
                }
                for (String partRule : this.cardView.getRightSplitRules()) {
                    if (!partRule.equals(rule)) continue;
                    realCardName = this.cardView.getRightSplitName();
                    break;
                }
            }
            if (PreferencesDialog.getCachedValue("cardRenderingReminderText", "false").equals("false")) {
                rule = CardRendererUtils.killReminderText(rule).trim();
            }
            if (rule.isEmpty()) continue;
            TextboxRule tbRule = TextboxRuleParser.parse(this.cardView, rule, realCardName);
            if (tbRule.type == TextboxRuleType.SIMPLE_KEYWORD) {
                keywords.add(tbRule);
                continue;
            }
            if (tbRule.text.isEmpty()) continue;
            rules.add(tbRule);
        }
    }

    private static int getBorderWidth(int cardWidth) {
        return (int)Math.max(2.0f, 0.03f * (float)cardWidth);
    }

    protected void layout(int cardWidth, int cardHeight) {
        this.cardWidth = cardWidth;
        this.cardHeight = cardHeight;
        this.cornerRadius = (int)Math.max(3.0f, 0.1f * (float)cardWidth);
        this.borderWidth = CardRenderer.getBorderWidth(cardWidth);
    }

    public static int getCardTopHeight(int cardWidth) {
        float BOX_HEIGHT_FRAC = 0.065f;
        int BOX_HEIGHT_MIN = 16;
        int boxHeight = (int)Math.max((float)BOX_HEIGHT_MIN, BOX_HEIGHT_FRAC * (float)cardWidth * 1.4f);
        int borderWidth = CardRenderer.getBorderWidth(cardWidth);
        return 2 * borderWidth + boxHeight;
    }

    public void draw(Graphics2D g, CardPanelAttributes attribs, BufferedImage image) {
        this.layout(attribs.cardWidth, attribs.cardHeight);
        this.isSelected = attribs.isSelected;
        this.isChoosable = attribs.isChoosable;
        this.drawBorder(g);
        this.drawBackground(g);
        this.lessOpaqueRulesTextBox = false;
        this.drawArt(g);
        this.drawFrame(g, attribs, image, this.lessOpaqueRulesTextBox);
        if (!this.cardView.isAbility()) {
            this.drawOverlays(g);
            this.drawCounters(g);
        }
    }

    protected abstract void drawBorder(Graphics2D var1);

    protected abstract void drawBackground(Graphics2D var1);

    protected abstract void drawArt(Graphics2D var1);

    protected abstract void drawFrame(Graphics2D var1, CardPanelAttributes var2, BufferedImage var3, boolean var4);

    protected void drawCardBackTexture(Graphics2D g) {
        g.setPaint(BG_TEXTURE_CARDBACK);
        g.fillRect(this.borderWidth, this.borderWidth, this.cardWidth - 2 * this.borderWidth, this.cardHeight - 2 * this.borderWidth);
    }

    protected void drawOverlays(Graphics2D g) {
        if (this.cardView.isCreature() && this.cardView instanceof PermanentView && ((PermanentView)this.cardView).hasSummoningSickness()) {
            int x1 = (int)(0.2 * (double)this.cardWidth);
            int x2 = (int)(0.8 * (double)this.cardWidth);
            int y1 = (int)(0.2 * (double)this.cardHeight);
            int y2 = (int)(0.8 * (double)this.cardHeight);
            int[] xPoints = new int[]{x1, x2, x1, x2};
            int[] yPoints = new int[]{y1, y1, y2, y2};
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke(7.0f));
            g.drawPolygon(xPoints, yPoints, 4);
            g.setColor(new Color(0, 0, 0, 200));
            g.setStroke(new BasicStroke(5.0f));
            g.drawPolygon(xPoints, yPoints, 4);
            g.setStroke(new BasicStroke(1.0f));
            int[] xPoints2 = new int[]{x1, x2, this.cardWidth / 2};
            int[] yPoints2 = new int[]{y1, y1, this.cardHeight / 2};
            g.setColor(new Color(0, 0, 0, 100));
            g.fillPolygon(xPoints2, yPoints2, 3);
        }
    }

    protected void drawArtIntoRect(Graphics2D g, int x, int y, int w, int h, Rectangle2D artRect, boolean shouldPreserveAspect) {
        double fullCardImgWidth = this.artImage.getWidth();
        double fullCardImgHeight = this.artImage.getHeight();
        double artWidth = artRect.getWidth() * fullCardImgWidth;
        double artHeight = artRect.getHeight() * fullCardImgHeight;
        double targetWidth = w;
        double targetHeight = h;
        double targetAspect = targetWidth / targetHeight;
        if (shouldPreserveAspect) {
            if (targetAspect * artHeight < artWidth) {
                artWidth = targetAspect * artHeight;
            } else {
                artHeight = artWidth / targetAspect;
            }
        }
        try {
            BufferedImage subImg = this.artImage.getSubimage((int)(artRect.getX() * fullCardImgWidth), (int)(artRect.getY() * fullCardImgHeight), (int)artWidth, (int)artHeight);
            g.drawImage(subImg, x, y, (int)targetWidth, (int)targetHeight, null);
        }
        catch (RasterFormatException rasterFormatException) {
            // empty catch block
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void drawCounters(Graphics2D g) {
        int xPos = (int)(0.65 * (double)this.cardWidth);
        int yPos = (int)(0.15 * (double)this.cardHeight);
        if (this.cardView.getCounters() != null) {
            for (CounterView v : this.cardView.getCounters()) {
                Polygon p;
                if (v.getName().equals("loyalty")) continue;
                switch (v.getName()) {
                    case "+1/+1": {
                        p = PLUS_COUNTER_POLY;
                        break;
                    }
                    case "-1/-1": {
                        p = MINUS_COUNTER_POLY;
                        break;
                    }
                    case "time": {
                        p = TIME_COUNTER_POLY;
                        break;
                    }
                    default: {
                        p = OTHER_COUNTER_POLY;
                    }
                }
                double scale = 0.025 * (double)this.cardWidth;
                Graphics2D g2 = (Graphics2D)g.create();
                try {
                    g2.translate(xPos, yPos);
                    g2.scale(scale, scale);
                    g2.setColor(Color.white);
                    g2.fillPolygon(p);
                    g2.setColor(Color.black);
                    g2.drawPolygon(p);
                    g2.setFont(new Font("Arial", 1, 7));
                    String cstr = String.valueOf(v.getCount());
                    int strW = g2.getFontMetrics().stringWidth(cstr);
                    g2.drawString(cstr, 5 - strW / 2, 8);
                }
                finally {
                    g2.dispose();
                }
                yPos += (int)(0.3 * (double)this.cardWidth);
            }
        }
    }

    protected int drawExpansionSymbol(Graphics2D g, int x, int y, int w, int h) {
        // Draw the expansion symbol
        Image setSymbol = ManaSymbols.getSetSymbolImage(cardView.getExpansionSetCode(), cardView.getRarity());
        int setSymbolWidth;
        if (setSymbol == null) {
            // Don't draw anything when we don't have a set symbol
            return 0;
            /*
            // Just draw the as a code
            String code = cardView.getExpansionSetCode();
            code = (code != null) ? code.toUpperCase(Locale.ENGLISH) : "";
            FontMetrics metrics = g.getFontMetrics();
            setSymbolWidth = metrics.stringWidth(code);
            if (cardView.getRarity() == Rarity.COMMON) {
                g.setColor(Color.white);
            } else {
                g.setColor(Color.black);
            }
            g.fillRoundRect(
                    x + w - setSymbolWidth - 1, y + 2,
                    setSymbolWidth+2, h - 5,
                    5, 5);
            g.setColor(getRarityColor());
            g.drawString(code, x + w - setSymbolWidth, y + h - 3);
             */
        } else {
            // Draw the set symbol
            int height = setSymbol.getHeight(null);
            int scale = 1;
            if (height != -1) {
                while (height > h + 2) {
                    scale *= 2;
                    height /= 2;
                }
            }
            setSymbolWidth = setSymbol.getWidth(null) / scale;
            g.drawImage(setSymbol,
                    x + w - setSymbolWidth, y + (h - height) / 2,
                    setSymbolWidth, height,
                    null);
        }
        return setSymbolWidth;
    }

    private Color getRarityColor() {
        if (this.cardView.getRarity() != null) {
            switch (this.cardView.getRarity()) {
                case RARE: {
                    return new Color(255, 191, 0);
                }
                case UNCOMMON: {
                    return new Color(192, 192, 192);
                }
                case MYTHIC: {
                    return new Color(213, 51, 11);
                }
                case SPECIAL: {
                    return new Color(204, 0, 255);
                }
                case BONUS: {
                    return new Color(129, 228, 228);
                }
            }
            return Color.black;
        }
        return Color.black;
    }

    protected String getCardTypeLine() {
        if (this.cardView.isAbility()) {
            AbilityType abilityType = this.cardView.getAbilityType();
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
            return abilityType.toString() + " Ability";
        }
        if (this.cardView.getMageObjectType() == MageObjectType.SPELL) {
            return "Spell";
        }
        StringBuilder sbType = new StringBuilder();
        String spType = this.getCardSuperTypeLine();
        String subType = this.getCardSubTypeLine();
        if (spType.equalsIgnoreCase("")) {
            sbType.append(subType);
        } else {
            sbType.append(spType);
            if (!subType.equalsIgnoreCase("")) {
                sbType.append("- ");
                sbType.append(subType);
            }
        }
        return sbType.toString();
    }

    protected String getCardSuperTypeLine() {
        StringBuilder spType = new StringBuilder();
        if (this.cardView.isToken()) {
            spType.append("Token ");
        }
        for (SuperType superType : this.cardView.getSuperTypes()) {
            spType.append((Object)superType).append(' ');
        }
        for (CardType cardType : this.cardView.getCardTypes()) {
            spType.append(cardType.toString()).append(' ');
        }
        return spType.toString();
    }

    protected String getCardSubTypeLine() {
        StringBuilder subType = new StringBuilder();
        if (!this.cardView.getSubTypes().isEmpty()) {
            for (SubType sType : this.cardView.getSubTypes()) {
                subType.append((Object)sType).append(' ');
            }
        }
        return subType.toString();
    }

    public void setArtImage(Image image) {
        this.artImage = CardRendererUtils.toBufferedImage(image);
    }
}
