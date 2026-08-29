/*
 * Decompiled with CFR.
 */
package org.mage.card.arcane;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import mage.MageInt;
import mage.cards.MageCardLocation;
import mage.cards.action.ActionCallback;
import mage.client.dialog.PreferencesDialog;
import mage.client.util.GUISizeHelper;
import mage.client.util.ImageCaches;
import mage.client.util.ImageHelper;
import mage.client.util.SoftValuesLoadingCache;
import mage.components.ImagePanel;
import mage.components.ImagePanelStyle;
import mage.constants.AbilityType;
import mage.constants.MageObjectType;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.view.CardView;
import mage.view.CounterView;
import org.jdesktop.swingx.graphics.GraphicsUtilities;
import org.mage.card.arcane.CardPanel;
import org.mage.card.arcane.CardRendererUtils;
import org.mage.card.arcane.GlowText;
import org.mage.card.arcane.ManaSymbols;
import org.mage.card.arcane.ModernCardRenderer;
import org.mage.card.arcane.ScaledImagePanel;
import org.mage.card.arcane.UI;
import org.mage.card.arcane.Util;
import org.mage.plugins.card.images.ImageCache;
import org.mage.plugins.card.images.ImageCacheData;
import org.mage.plugins.card.utils.impl.ImageManagerImpl;

public class CardPanelRenderModeImage
extends CardPanel {
    private static final long serialVersionUID = -3272134219262184411L;
    private static final SoftValuesLoadingCache<Key, BufferedImage> IMAGE_MODE_RENDERED_CACHE = ImageCaches.register(SoftValuesLoadingCache.from(CardPanelRenderModeImage::createImage));
    private static final int WIDTH_LIMIT = 90;
    private static final float ROUNDED_CORNER_SIZE = 0.1f;
    private static final float BLACK_BORDER_SIZE = 0.03f;
    private static final float SELECTION_BORDER_SIZE = 0.03f;
    private static final int TEXT_GLOW_SIZE = 6;
    private static final float TEXT_GLOW_INTENSITY = 3.0f;
    private static final int CARD_PT_FONT_MIN_SIZE = 17;
    public final ScaledImagePanel imagePanel;
    private ImagePanel overlayPanel;
    private JPanel typeIconPanel;
    private JButton typeIconButton;
    private final JPanel ptPanel;
    private JPanel counterPanel;
    private JLabel loyaltyCounterLabel;
    private JLabel plusCounterLabel;
    private JLabel otherCounterLabel;
    private JLabel minusCounterLabel;
    private int loyaltyCounter;
    private int plusCounter;
    private int otherCounter;
    private int minusCounter;
    private int lastCardWidth;
    private final GlowText titleText;
    private final GlowText ptText1;
    private final GlowText ptText2;
    private final GlowText ptText3;
    private final JLabel fullImageText;
    private String fullImagePath = null;
    private boolean hasImage = false;
    private boolean displayTitleAnyway;
    private final boolean displayFullImagePath;
    private int updateArtImageStamp;
    private static final String STACK_TYPE_BADGE_MARKER = "XCP_STACK_TYPE_BADGE_V1_2_1_8";
    private static final String STACK_NO_IMAGE_PATH_MARKER = "XCP_STACK_NO_IMAGE_PATH_V1_2_1_8";
    private static final String STACK_BADGE_VISIBILITY_MARKER = "XCP_STACK_BADGE_VISIBILITY_V1_2_1_9";
    private static final String STACK_CARD_CLIP_MARKER = "XCP_STACK_CARD_CLIP_V1_2_1_9";
    private static final String STACK_BADGE_MOVED_TO_HEADER_MARKER = "XCP_STACK_BADGE_MOVED_TO_HEADER_V1_2_1_9";
    private static final String STACK_NO_WHITE_CORNER_BORDER_MARKER = "XCP_STACK_NO_WHITE_CORNER_BORDER_V1_2_1_9_10_2";
    private static final String STACK_OUTER_ROUNDED_CLIP_MARKER = "XCP_STACK_OUTER_ROUNDED_CLIP_V1_2_1_9_10_4";
    private static final String STACK_CARD_SELF_CLIP_MARKER = "XCP_STACK_CARD_SELF_CLIP_V1_2_1_9_10_6";
    private static final Color STACK_TYPE_BADGE_BACKGROUND = new Color(18, 22, 28, 238);
    private static final Color STACK_TYPE_BADGE_BORDER = new Color(185, 151, 72, 235);
    private static final Color STACK_TYPE_BADGE_TEXT = new Color(250, 235, 185);

    private static boolean canShowCardIcons(int cardFullWidth, boolean cardHasImage) {
        return cardFullWidth > 60 && cardFullWidth < 200 || !cardHasImage;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static BufferedImage createImage(Key key) {
        Insets componentBorder = key.border;
        int renderWidth = key.cardWidth;
        int renderHeight = key.cardHeight;
        int cardXOffset = key.cardXOffset;
        int cardYOffset = key.cardYOffset;
        BufferedImage image = GraphicsUtilities.createCompatibleTranslucentImage((int)renderWidth, (int)renderHeight);
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            CardSizes sizes = new CardSizes(componentBorder, cardXOffset, cardYOffset, renderWidth, renderHeight);
            int cornerSizeSelection = Math.max(4, Math.round((float)sizes.rectSelection.width * 0.1f));
            int cornerSizeBorder = Math.max(4, Math.round((float)sizes.rectBorder.width * 0.1f));
            if (key.isSelected) {
                g2.setColor(Color.green);
                g2.fillRoundRect(sizes.rectSelection.x + 1, sizes.rectSelection.y + 1, sizes.rectSelection.width - 2, sizes.rectSelection.height - 2, cornerSizeSelection, cornerSizeSelection);
            } else if (key.isChoosable) {
                g2.setColor(new Color(250, 250, 0, 230));
                g2.fillRoundRect(sizes.rectSelection.x + 1, sizes.rectSelection.y + 1, sizes.rectSelection.width - 2, sizes.rectSelection.height - 2, cornerSizeSelection, cornerSizeSelection);
            } else if (key.isPlayable) {
                g2.setColor(new Color(153, 102, 204, 200));
                g2.fillRoundRect(sizes.rectSelection.x, sizes.rectSelection.y, sizes.rectSelection.width, sizes.rectSelection.height, cornerSizeSelection, cornerSizeSelection);
            }
            if (key.canAttack || key.canBlock) {
                g2.setColor(new Color(255, 50, 50, 230));
                g2.fillRoundRect(sizes.rectSelection.x + 1, sizes.rectSelection.y + 1, sizes.rectSelection.width - 2, sizes.rectSelection.height - 2, cornerSizeSelection, cornerSizeSelection);
            }
            if (!key.hasImage) {
                g2.setColor(new Color(125, 125, 125, 255));
                g2.fillRoundRect(sizes.rectBorder.x, sizes.rectBorder.y, sizes.rectBorder.width, sizes.rectBorder.height, cornerSizeBorder, cornerSizeBorder);
                g2.setColor(new Color(30, 200, 200, 200));
                g2.fillRoundRect(sizes.rectBorder.x + 1, sizes.rectBorder.y + 1, sizes.rectBorder.width - 2, sizes.rectBorder.height - 2, cornerSizeBorder, cornerSizeBorder);
            }
        }
        finally {
            g2.dispose();
        }
        return image;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static ImageIcon getCounterImageWithAmount(int amount, BufferedImage image, int cardWidth) {
        int xOffset;
        int factor = cardWidth > 90 ? 2 : 1;
        int n = xOffset = amount > 9 ? 2 : 5;
        int fontSize = factor == 1 ? (amount < 10 ? 12 : (amount < 100 ? 10 : (amount < 1000 ? 7 : 6))) : (amount < 10 ? 19 : (amount < 100 ? 15 : (amount < 1000 ? 12 : (amount < 10000 ? 9 : 8))));
        BufferedImage newImage = cardWidth > 90 ? ImageManagerImpl.deepCopy(image) : ImageHelper.getResizedImage(image, 20, 20);
        Graphics2D g2 = newImage.createGraphics();
        try {
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial Black", amount > 100 ? 0 : 1, fontSize));
            ((Graphics)g2).drawString(Integer.toString(amount), xOffset * factor, 11 * factor);
        }
        finally {
            g2.dispose();
        }
        return new ImageIcon(newImage);
    }

    public CardPanelRenderModeImage(CardView newGameCard, UUID gameId, boolean loadImage, ActionCallback callback, boolean foil, Dimension dimension, boolean needFullPermanentRender) {
        super(newGameCard, gameId, loadImage, callback, foil, dimension, needFullPermanentRender);
        if (!newGameCard.isAbility()) {
            this.counterPanel = new JPanel();
            this.counterPanel.setLayout(null);
            this.counterPanel.setOpaque(false);
            this.add(this.counterPanel);
            this.plusCounterLabel = new JLabel("");
            this.plusCounterLabel.setToolTipText("+1/+1");
            this.counterPanel.add(this.plusCounterLabel);
            this.minusCounterLabel = new JLabel("");
            this.minusCounterLabel.setToolTipText("-1/-1");
            this.counterPanel.add(this.minusCounterLabel);
            this.loyaltyCounterLabel = new JLabel("");
            this.loyaltyCounterLabel.setToolTipText("loyalty");
            this.counterPanel.add(this.loyaltyCounterLabel);
            this.otherCounterLabel = new JLabel("");
            this.counterPanel.add(this.otherCounterLabel);
            this.counterPanel.setVisible(false);
        }
        if (newGameCard.isAbility()) {
            if (newGameCard.getAbilityType() == AbilityType.TRIGGERED_NONMANA) {
                this.setTypeIcon(ImageManagerImpl.instance.getTriggeredAbilityImage(), "Triggered Ability");
            } else if (newGameCard.getAbilityType() == AbilityType.ACTIVATED_NONMANA) {
                this.setTypeIcon(ImageManagerImpl.instance.getActivatedAbilityImage(), "Activated Ability");
            }
        }
        if (this.getGameCard().isToken()) {
            this.setTypeIcon(ImageManagerImpl.instance.getTokenIconImage(), "Token Permanent");
        }
        this.displayTitleAnyway = PreferencesDialog.getCachedValue("showCardNames", "true").equals("true");
        this.displayFullImagePath = PreferencesDialog.getCachedValue("showFullImagePath", "false").equals("true");
        this.titleText = new GlowText();
        this.setTitle(this.getGameCard());
        this.titleText.setForeground(Color.white);
        this.titleText.setGlow(Color.black, 6, 3.0f);
        this.titleText.setWrap(true);
        this.add(this.titleText);
        this.fullImageText = new JLabel();
        this.fullImageText.setText(this.fullImagePath);
        this.fullImageText.setForeground(Color.BLACK);
        this.add(this.fullImageText);
        this.ptPanel = new JPanel();
        this.ptPanel.setOpaque(false);
        this.ptPanel.setLayout(new BoxLayout(this.ptPanel, 0));
        this.ptPanel.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(Integer.MAX_VALUE, 0)));
        this.ptText1 = new GlowText();
        this.ptText2 = new GlowText();
        this.ptText3 = new GlowText();
        this.updatePTTexts(this.getGameCard());
        this.ptPanel.add(this.ptText1);
        this.ptPanel.add(this.ptText2);
        this.ptPanel.add(this.ptText3);
        this.add(this.ptPanel);
        BufferedImage sickness = ImageManagerImpl.instance.getSicknessImage();
        this.overlayPanel = new ImagePanel(sickness, ImagePanelStyle.SCALED);
        this.overlayPanel.setOpaque(false);
        this.add((Component)this.overlayPanel);
        this.imagePanel = new ScaledImagePanel(){
            private static final long serialVersionUID = 1L;

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void paint(Graphics g) {
                if (!CardPanelRenderModeImage.this.isStackCard()) {
                    super.paint(g);
                    return;
                }
                Graphics2D clipped = (Graphics2D)g.create();
                try {
                    int width = Math.max(1, this.getWidth());
                    int height = Math.max(1, this.getHeight());
                    int arc = Math.max(20, Math.round((float)Math.min(width, height) * 0.14f));
                    clipped.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    clipped.clip(new RoundRectangle2D.Double(0.0, 0.0, width, height, arc, arc));
                    super.paint(clipped);
                }
                finally {
                    clipped.dispose();
                }
            }
        };
        this.imagePanel.setBorder(BorderFactory.createEmptyBorder());
        this.add(this.imagePanel);
        if (loadImage) {
            this.initialDraw();
        }
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        this.counterPanel = null;
    }

    @Override
    public void setZone(Zone zone) {
        super.setZone(zone);
        if (this.fullImageText != null) {
            this.fullImageText.setVisible(this.fullImagePath != null && this.displayFullImagePath && zone != Zone.STACK);
        }
        this.repaint();
    }

    private boolean isStackCard() {
        return this.getZone() == Zone.STACK && !this.isAnimationPanel();
    }

    private String getStackTypeLabel() {
        CardView card = this.getGameCard();
        if (card == null) {
            return null;
        }
        AbilityType abilityType = card.getAbilityType();
        if (abilityType == AbilityType.SPELL || card.getMageObjectType() == MageObjectType.SPELL) {
            return "Spell";
        }
        if (!card.isAbility() && abilityType == null) {
            return null;
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
        return abilityType.toString() + " Ability";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void paintStackTypeBadge(Graphics g) {
        if (!this.isStackCard()) {
            return;
        }
        String type = this.getStackTypeLabel();
        if (type == null || type.isEmpty()) {
            return;
        }
        Rectangle cardBounds = this.imagePanel.getBounds();
        if (cardBounds.width < 40 || cardBounds.height < 80) {
            return;
        }
        Graphics2D g2 = (Graphics2D)g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int horizontalPadding = Math.max(8, cardBounds.width / 30);
            int badgeHeight = Math.max(24, Math.min(38, cardBounds.height / 12));
            float fontSize = Math.max(12.0f, Math.min(18.0f, (float)cardBounds.height / 32.0f));
            Font badgeFont = this.getFont().deriveFont(1, fontSize);
            FontMetrics metrics = g2.getFontMetrics(badgeFont);
            int badgeWidth = Math.max(metrics.stringWidth(type) + horizontalPadding * 2, cardBounds.width - 16);
            badgeWidth = Math.min(cardBounds.width - 8, badgeWidth);
            int badgeX = cardBounds.x + (cardBounds.width - badgeWidth) / 2;
            int badgeY = cardBounds.y + Math.max(28, cardBounds.height / 9);
            g2.setFont(badgeFont);
            g2.setColor(STACK_TYPE_BADGE_BACKGROUND);
            g2.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 12, 12);
            g2.setColor(STACK_TYPE_BADGE_BORDER);
            g2.drawRoundRect(badgeX, badgeY, badgeWidth - 1, badgeHeight - 1, 12, 12);
            g2.setColor(STACK_TYPE_BADGE_TEXT);
            int textX = badgeX + Math.max(4, (badgeWidth - metrics.stringWidth(type)) / 2);
            int textY = badgeY + (badgeHeight - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(type, textX, textY);
        }
        finally {
            g2.dispose();
        }
    }

    @Override
    public void doLayout() {
        int cardHeight;
        int cardWidth;
        super.doLayout();
        int cardXOffset = 0;
        int cardYOffset = 0;
        CardView cardView = this.getGameCard();
        if (this.getTopPanelRef() == null) {
            cardWidth = this.getWidth();
            cardHeight = this.getHeight();
        } else {
            cardWidth = this.getCardLocation().getCardWidth();
            cardHeight = this.getCardLocation().getCardHeight();
        }
        CardSizes sizes = new CardSizes(this.getInsets(), cardXOffset, cardYOffset, cardWidth, cardHeight);
        Rectangle realCardSize = sizes.rectBorder;
        this.imagePanel.setLocation(realCardSize.x, realCardSize.y);
        this.imagePanel.setSize(realCardSize.width, realCardSize.height);
        if (this.hasSickness() && cardView.isCreature() && this.isPermanent()) {
            this.overlayPanel.setLocation(realCardSize.x, realCardSize.y);
            this.overlayPanel.setSize(realCardSize.width, realCardSize.height);
        } else {
            this.overlayPanel.setVisible(false);
        }
        if (this.typeIconPanel != null) {
            this.typeIconPanel.setLocation(realCardSize.x, realCardSize.y);
            this.typeIconPanel.setSize(realCardSize.width, realCardSize.height);
        }
        if (this.counterPanel != null) {
            this.counterPanel.setLocation(realCardSize.x, realCardSize.y);
            this.counterPanel.setSize(realCardSize.width, realCardSize.height);
            int size = cardWidth > 90 ? 40 : 20;
            this.minusCounterLabel.setLocation(this.counterPanel.getWidth() - size, this.counterPanel.getHeight() - size * 2);
            this.minusCounterLabel.setSize(size, size);
            this.plusCounterLabel.setLocation(5, this.counterPanel.getHeight() - size * 2);
            this.plusCounterLabel.setSize(size, size);
            this.loyaltyCounterLabel.setLocation(this.counterPanel.getWidth() - size, this.counterPanel.getHeight() - size);
            this.loyaltyCounterLabel.setSize(size, size);
            this.otherCounterLabel.setLocation(5, this.counterPanel.getHeight() - size);
            this.otherCounterLabel.setSize(size, size);
        }
        boolean showText = !this.isAnimationPanel() && CardPanelRenderModeImage.canShowCardIcons(cardWidth, this.hasImage);
        this.titleText.setVisible(showText);
        this.ptText1.setVisible(showText && !this.ptText1.getText().isEmpty());
        this.ptText2.setVisible(showText && !this.ptText2.getText().isEmpty());
        this.ptText3.setVisible(showText && !this.ptText3.getText().isEmpty());
        this.fullImageText.setVisible(this.fullImagePath != null && this.displayFullImagePath && this.getZone() != Zone.STACK);
        if (showText) {
            int mainFontSize = GUISizeHelper.getImageRendererMainFontSize(cardHeight);
            int titleFontSize = GUISizeHelper.getImageRendererTitleFontSize(cardHeight);
            this.titleText.setFont(this.getFont().deriveFont(1, titleFontSize));
            int titleMarginLeft = 0;
            int titleMarginRight = 0;
            int titleMarginTop = 0;
            int titleMarginBottom = 0;
            this.titleText.setBounds(this.imagePanel.getX() + titleMarginLeft, this.imagePanel.getY() + titleMarginTop, this.imagePanel.getBounds().width - titleMarginLeft - titleMarginRight, this.imagePanel.getBounds().height - titleMarginTop - titleMarginBottom);
            int missImageFontSize = Math.max(10, Math.round(0.5f * (float)mainFontSize));
            this.fullImageText.setFont(this.getFont().deriveFont(0, missImageFontSize));
            this.fullImageText.setBounds(this.titleText.getX(), this.titleText.getY(), this.titleText.getBounds().width, this.titleText.getBounds().height);
            if (cardView.showPT()) {
                MageInt currentPower = cardView.getOriginalPower();
                MageInt currentToughness = cardView.getOriginalToughness();
                this.prepareGlowFont(this.ptText1, Math.max(17, mainFontSize), currentPower, false);
                this.prepareGlowFont(this.ptText2, Math.max(17, mainFontSize), null, false);
                this.prepareGlowFont(this.ptText3, Math.max(17, mainFontSize), currentToughness, CardRendererUtils.isCardWithDamage(cardView));
                int ptMarginRight = Math.round(0.0952381f * (float)cardWidth);
                int ptMarginBottom = Math.round(0.06623932f * (float)cardHeight);
                int ptWidth = cardWidth - ptMarginRight * 2;
                int ptHeight = this.ptText2.getHeight();
                int ptX = cardXOffset + ptMarginRight;
                int ptY = cardYOffset + cardHeight - ptMarginBottom - ptHeight;
                this.ptPanel.setBounds(ptX, ptY, ptWidth, ptHeight);
            }
        }
    }

    public Image getImage() {
        if (this.hasImage) {
            return ImageCache.getCardImageOriginal(this.getGameCard()).getImage();
        }
        return null;
    }

    @Override
    protected void paintCard(Graphics2D g2d) {
        float alpha = this.getAlpha();
        if (alpha != 1.0f) {
            AlphaComposite composite = AlphaComposite.getInstance(10, alpha);
            g2d.setComposite(composite);
        }
        MageCardLocation cardLocation = this.getCardLocation();
        g2d.drawImage(IMAGE_MODE_RENDERED_CACHE.getOrThrow(new Key(this.getInsets(), cardLocation.getCardWidth(), cardLocation.getCardHeight(), cardLocation.getCardWidth(), cardLocation.getCardHeight(), 0, 0, this.hasImage, this.isSelected(), this.isChoosable(), this.getGameCard().isPlayable(), this.getGameCard().isCanAttack(), this.getGameCard().isCanBlock())), 0, 0, cardLocation.getCardWidth(), cardLocation.getCardHeight(), null);
        g2d.dispose();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D clipped = (Graphics2D)g.create();
        try {
            int width = Math.max(1, this.getWidth());
            int height = Math.max(1, this.getHeight());
            int arc = Math.max(22, Math.round((float)Math.min(width, height) * 0.12f));
            clipped.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            clipped.clip(new RoundRectangle2D.Double(0.0, 0.0, width, height, arc, arc));
            super.paintComponent(clipped);
        }
        finally {
            clipped.dispose();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void paintChildren(Graphics g) {
        Graphics2D clipped = (Graphics2D)g.create();
        try {
            if (this.imagePanel != null && this.imagePanel.getWidth() > 0 && this.imagePanel.getHeight() > 0) {
                Rectangle bounds = this.imagePanel.getBounds();
                int arc = Math.max(20, Math.round((float)bounds.width * 0.14f));
                clipped.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                clipped.clip(new RoundRectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc));
            }
            super.paintChildren((Graphics)clipped);
        }
        finally {
            clipped.dispose();
        }
        if (this.getShowCastingCost() && !this.isAnimationPanel() && CardPanelRenderModeImage.canShowCardIcons(this.getCardWidth(), this.hasImage)) {
            int symbolMarginX = 2;
            String manaCost = ManaSymbols.getClearManaCost(this.getGameCard().getManaCostStr());
            int manaWidth = this.getManaWidth(manaCost, symbolMarginX);
            int manaMarginRight = Math.round(0.032738097f * (float)this.getCardWidth());
            int manaMarginTop = Math.round(0.025641026f * (float)this.getCardHeight());
            int cardOffsetX = 0;
            int cardOffsetY = 0;
            int manaX = cardOffsetX + this.getCardWidth() - manaMarginRight - manaWidth;
            int manaY = cardOffsetY + manaMarginTop;
            ManaSymbols.draw(g, manaCost, manaX, manaY, this.getSymbolWidth(), ModernCardRenderer.MANA_ICONS_TEXT_COLOR, symbolMarginX);
        }
    }

    @Override
    public void setCardBounds(int x, int y, int cardWidth, int cardHeight) {
        super.setCardBounds(x, y, cardWidth, cardHeight);
        if (this.imagePanel != null && this.imagePanel.getSrcImage() != null) {
            this.updateArtImage();
        }
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (alpha == 0.0f) {
            this.ptText1.setVisible(false);
            this.ptText2.setVisible(false);
            this.ptText3.setVisible(false);
            this.titleText.setVisible(false);
        } else if (alpha == 1.0f) {
            this.ptText1.setVisible(true);
            this.ptText2.setVisible(true);
            this.ptText3.setVisible(true);
            this.titleText.setVisible(true);
        }
    }

    @Override
    public void setSelected(boolean isSelected) {
        super.setSelected(isSelected);
        if (isSelected) {
            this.titleText.setGlowColor(Color.green);
        } else {
            this.titleText.setGlowColor(Color.black);
        }
    }

    public void showCardTitle() {
        this.displayTitleAnyway = true;
        this.setTitle(this.getGameCard());
    }

    @Override
    public String toString() {
        return this.getGameCard().toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void transferResources(CardPanel panelAbstract) {
        if (panelAbstract instanceof CardPanelRenderModeImage) {
            CardPanelRenderModeImage panel = (CardPanelRenderModeImage)panelAbstract;
            ScaledImagePanel scaledImagePanel = panel.imagePanel;
            synchronized (scaledImagePanel) {
                if (panel.imagePanel.hasImage()) {
                    this.setImage(panel.imagePanel.getSrcImage());
                }
            }
        }
    }

    @Override
    public void update(CardView card) {
        super.update(card);
        this.updatePTTexts(this.getGameCard());
        this.setTitle(this.getGameCard());
        this.overlayPanel.setVisible(this.hasSickness() && this.getGameCard().isCreature() && this.isPermanent());
        if (this.counterPanel != null) {
            this.updateCounters(card);
        }
        this.repaint();
    }

    public void updateArtImage() {
        this.setTappedAngle(this.isTapped() ? 1.5707963267948966 : 0.0);
        this.setFlippedAngle(this.isFlipped() ? Math.PI : 0.0);
        int stamp = ++this.updateArtImageStamp;
        Util.threadPool.submit(() -> {
            try {
                ImageCacheData data = ImageCache.getCardImage(this.getGameCard(), this.getCardWidth(), this.getCardHeight());
                if (data.getImage() == null) {
                    this.setFullPath(data.getPath());
                }
                UI.invokeLater(() -> {
                    if (stamp == this.updateArtImageStamp) {
                        this.hasImage = data.getImage() != null;
                        this.setTitle(this.getGameCard());
                        if (this.fullImageText != null) {
                            this.fullImageText.setVisible(this.fullImagePath != null && this.displayFullImagePath && this.getZone() != Zone.STACK);
                        }
                        this.setImage(data.getImage());
                    }
                });
            }
            catch (Error | Exception e) {
                e.printStackTrace();
            }
        });
    }

    private int getManaWidth(String manaCost, int symbolMarginX) {
        int width = 0;
        manaCost = manaCost.replace("\\", "");
        StringTokenizer tok = new StringTokenizer(manaCost, " ");
        while (tok.hasMoreTokens()) {
            tok.nextToken();
            if (width != 0) {
                width += symbolMarginX;
            }
            width += this.getSymbolWidth();
        }
        return width;
    }

    private void prepareGlowFont(GlowText label, int fontSize, MageInt value, boolean drawAsDamaged) {
        label.setFont(this.getFont().deriveFont(1, fontSize));
        label.setForeground(CardRendererUtils.getCardTextColor(value, drawAsDamaged, this.titleText.getForeground(), true));
        Dimension ptSize = label.getPreferredSize();
        label.setSize(ptSize.width, ptSize.height);
    }

    private void setFullPath(String fullImagePath) {
        this.fullImagePath = fullImagePath;
        this.fullImagePath = this.fullImagePath.replaceAll("\\\\", "\\\\<br>");
        this.fullImagePath = this.fullImagePath.replaceAll("/", "/<br>");
        this.fullImagePath = "<html>" + this.fullImagePath + "</html>";
        this.fullImageText.setText(!this.displayFullImagePath ? "" : this.fullImagePath);
        this.fullImageText.setVisible(this.displayFullImagePath && this.getZone() != Zone.STACK);
        this.doLayout();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void setImage(BufferedImage srcImage) {
        ScaledImagePanel scaledImagePanel = this.imagePanel;
        synchronized (scaledImagePanel) {
            if (srcImage != null) {
                this.imagePanel.setImage(srcImage);
            } else {
                this.imagePanel.clearImage();
            }
            this.repaint();
        }
        this.doLayout();
    }

    private void setTitle(CardView card) {
        this.titleText.setText(!this.displayTitleAnyway && this.hasImage ? "" : card.getDisplayName());
    }

    private void setTypeIcon(BufferedImage bufferedImage, String toolTipText) {
        this.typeIconPanel = new JPanel();
        this.typeIconPanel.setLayout(null);
        this.typeIconPanel.setOpaque(false);
        this.add(this.typeIconPanel);
        this.typeIconButton = new JButton("");
        this.typeIconButton.setLocation(2, 2);
        this.typeIconButton.setSize(25, 25);
        this.typeIconPanel.setVisible(true);
        this.typeIconButton.setIcon(new ImageIcon(bufferedImage));
        if (toolTipText != null) {
            this.typeIconButton.setToolTipText(toolTipText);
        }
        this.typeIconPanel.add(this.typeIconButton);
    }

    private void updateCounters(CardView card) {
        if (card.getCounters() != null && !card.getCounters().isEmpty()) {
            String name = "";
            if (this.lastCardWidth != this.getCardWidth()) {
                this.lastCardWidth = this.getCardWidth();
                this.plusCounter = 0;
                this.minusCounter = 0;
                this.otherCounter = 0;
                this.loyaltyCounter = 0;
            }
            this.plusCounterLabel.setVisible(false);
            this.minusCounterLabel.setVisible(false);
            this.loyaltyCounterLabel.setVisible(false);
            this.otherCounterLabel.setVisible(false);
            block10: for (CounterView counterView : card.getCounters()) {
                if (counterView.getCount() == 0) continue;
                switch (counterView.getName()) {
                    case "+1/+1": {
                        if (counterView.getCount() != this.plusCounter) {
                            this.plusCounter = counterView.getCount();
                            this.plusCounterLabel.setIcon(CardPanelRenderModeImage.getCounterImageWithAmount(this.plusCounter, ImageManagerImpl.instance.getCounterImageGreen(), this.getCardWidth()));
                        }
                        this.plusCounterLabel.setVisible(true);
                        continue block10;
                    }
                    case "-1/-1": {
                        if (counterView.getCount() != this.minusCounter) {
                            this.minusCounter = counterView.getCount();
                            this.minusCounterLabel.setIcon(CardPanelRenderModeImage.getCounterImageWithAmount(this.minusCounter, ImageManagerImpl.instance.getCounterImageRed(), this.getCardWidth()));
                        }
                        this.minusCounterLabel.setVisible(true);
                        continue block10;
                    }
                    case "loyalty": {
                        if (counterView.getCount() != this.loyaltyCounter) {
                            this.loyaltyCounter = counterView.getCount();
                            this.loyaltyCounterLabel.setIcon(CardPanelRenderModeImage.getCounterImageWithAmount(this.loyaltyCounter, ImageManagerImpl.instance.getCounterImageViolet(), this.getCardWidth()));
                        }
                        this.loyaltyCounterLabel.setVisible(true);
                        continue block10;
                    }
                }
                if (!name.isEmpty()) continue;
                name = counterView.getName();
                this.otherCounter = counterView.getCount();
                this.otherCounterLabel.setToolTipText(name);
                this.otherCounterLabel.setIcon(CardPanelRenderModeImage.getCounterImageWithAmount(this.otherCounter, ImageManagerImpl.instance.getCounterImageGrey(), this.getCardWidth()));
                this.otherCounterLabel.setVisible(true);
            }
            this.counterPanel.setVisible(true);
        } else {
            this.plusCounterLabel.setVisible(false);
            this.minusCounterLabel.setVisible(false);
            this.loyaltyCounterLabel.setVisible(false);
            this.otherCounterLabel.setVisible(false);
            this.counterPanel.setVisible(false);
        }
    }

    private void updatePTTexts(CardView card) {
        if (card.isCreature() || card.getSubTypes().contains((Object)SubType.VEHICLE)) {
            this.ptText1.setText(this.getGameCard().getPower());
            this.ptText2.setText("/");
            this.ptText3.setText(CardRendererUtils.getCardLifeWithDamage(this.getGameCard()));
        } else if (card.isPlaneswalker()) {
            this.ptText1.setText("");
            this.ptText2.setText("");
            this.ptText3.setText(this.getGameCard().getLoyalty());
        } else if (card.isBattle()) {
            this.ptText1.setText("");
            this.ptText2.setText("");
            this.ptText3.setText(this.getGameCard().getDefense());
        } else {
            this.ptText1.setText("");
            this.ptText2.setText("");
            this.ptText3.setText("");
        }
        this.ptText1.setForeground(Color.white);
        this.ptText1.setGlow(Color.black, 6, 3.0f);
        this.ptText2.setForeground(Color.white);
        this.ptText2.setGlow(Color.black, 6, 3.0f);
        this.ptText3.setForeground(Color.white);
        this.ptText3.setGlow(Color.black, 6, 3.0f);
    }

    private static class Key {
        final Insets border;
        final int width;
        final int height;
        final int cardWidth;
        final int cardHeight;
        final int cardXOffset;
        final int cardYOffset;
        final boolean hasImage;
        final boolean isSelected;
        final boolean isChoosable;
        final boolean isPlayable;
        final boolean canAttack;
        final boolean canBlock;

        public Key(Insets border, int width, int height, int cardWidth, int cardHeight, int cardXOffset, int cardYOffset, boolean hasImage, boolean isSelected, boolean isChoosable, boolean isPlayable, boolean canAttack, boolean canBlock) {
            this.border = border;
            this.width = width;
            this.height = height;
            this.cardWidth = cardWidth;
            this.cardHeight = cardHeight;
            this.cardXOffset = cardXOffset;
            this.cardYOffset = cardYOffset;
            this.hasImage = hasImage;
            this.isSelected = isSelected;
            this.isChoosable = isChoosable;
            this.isPlayable = isPlayable;
            this.canAttack = canAttack;
            this.canBlock = canBlock;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            Key that = (Key)obj;
            return Objects.equals(this.border, that.border) && this.width == that.width && this.height == that.height && this.cardWidth == that.cardWidth && this.cardHeight == that.cardHeight && this.cardXOffset == that.cardXOffset && this.cardYOffset == that.cardYOffset && this.hasImage == that.hasImage && this.isSelected == that.isSelected && this.isChoosable == that.isChoosable && this.isPlayable == that.isPlayable && this.canAttack == that.canAttack && this.canBlock == that.canBlock;
        }

        public int hashCode() {
            return Objects.hash(this.border, this.width, this.height, this.cardWidth, this.cardHeight, this.cardXOffset, this.cardYOffset, this.hasImage, this.isSelected, this.isChoosable, this.isPlayable, this.canAttack, this.canBlock);
        }
    }

    private static class CardSizes {
        Rectangle rectFull;
        Rectangle rectSelection;
        Rectangle rectBorder;
        Rectangle rectCard;

        CardSizes(Insets border, int offsetX, int offsetY, int fullWidth, int fullHeight) {
            int realBorderSizeX = Math.max(1, Math.round((float)fullWidth * 0.03f));
            int realBorderSizeY = Math.max(1, Math.round((float)fullHeight * 0.03f));
            int realSelectionSizeX = Math.max(1, Math.round((float)fullWidth * 0.03f));
            int realSelectionSizeY = Math.max(1, Math.round((float)fullHeight * 0.03f));
            this.rectFull = new Rectangle(offsetX + border.left, offsetY + border.top, fullWidth - border.left - border.right, fullHeight - border.top - border.bottom);
            this.rectSelection = new Rectangle(this.rectFull.x, this.rectFull.y, this.rectFull.width, this.rectFull.height);
            this.rectBorder = new Rectangle(this.rectSelection.x + realSelectionSizeX, this.rectSelection.y + realSelectionSizeY, this.rectSelection.width - 2 * realSelectionSizeX, this.rectSelection.height - 2 * realSelectionSizeY);
            this.rectCard = new Rectangle(this.rectBorder.x + realBorderSizeX, this.rectBorder.y + realBorderSizeY, this.rectBorder.width - 2 * realBorderSizeX, this.rectBorder.height - 2 * realBorderSizeY);
        }
    }
}
