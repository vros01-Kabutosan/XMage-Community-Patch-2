/*
 * Decompiled with CFR.
 */
package org.mage.plugins.card;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import mage.cards.MageCard;
import mage.cards.MagePermanent;
import mage.cards.action.ActionCallback;
import mage.client.util.CardRenderMode;
import mage.client.util.GUISizeHelper;
import mage.interfaces.plugin.CardPlugin;
import mage.view.CardView;
import mage.view.PermanentView;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import net.xeoh.plugins.base.annotations.events.PluginLoaded;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.apache.log4j.Logger;
import org.mage.card.arcane.Animation;
import org.mage.card.arcane.CardPanel;
import org.mage.card.arcane.CardPanelRenderModeImage;
import org.mage.card.arcane.CardPanelRenderModeMTGO;
import org.mage.card.arcane.ManaSymbols;
import org.mage.plugins.card.dl.DownloadGui;
import org.mage.plugins.card.dl.DownloadJob;
import org.mage.plugins.card.dl.Downloader;
import org.mage.plugins.card.dl.sources.DirectLinksForDownload;
import org.mage.plugins.card.dl.sources.GathererSets;
import org.mage.plugins.card.dl.sources.GathererSymbols;
import org.mage.plugins.card.dl.sources.ScryfallSymbolsSource;
import org.mage.plugins.card.images.ImageCache;
import org.mage.plugins.card.info.CardInfoPaneImpl;

@PluginImplementation
@Author(name="nantuko")
public class CardPluginImpl
implements CardPlugin {
    private static final Logger LOGGER = Logger.getLogger(CardPluginImpl.class);
    private static final int GUTTER_Y = 15;
    private static final int GUTTER_X = 15;
    static final float EXTRA_CARD_SPACING_X = 0.04f;
    private static final float CARD_SPACING_Y = 0.03f;
    private static final float STACK_SPACING_X = 0.07f;
    private static final float STACK_SPACING_Y = 0.1f;
    private static final float ATTACHMENT_SPACING_Y = 0.13f;
    private static final int cardStackMax = 5;
    private int cardWidthMin = (int)GUISizeHelper.battlefieldCardMinDimension.getWidth();
    private int cardWidthMax = (int)GUISizeHelper.battlefieldCardMaxDimension.getWidth();
    private static final int CARD_WIDTH_AUTO_FIT_INCREMENT = 4;
    private static final boolean stackVertical = false;
    private int playAreaWidth;
    private int playAreaHeight;
    private int cardWidth;
    private int cardHeight;
    private int effectiveCardWidthMin;
    private int effectiveGutterY = 15;
    private int xcpBattlefieldPermanentCount;
    private int extraCardSpacingX;
    private int cardSpacingX;
    private int cardSpacingY;
    private int stackSpacingX;
    private int stackSpacingY;
    private int attachmentSpacingY;
    private List<Row> rows = new ArrayList<Row>();
    private static final int XCP_SCROLL_GUARD_Y = 12;
    private static final int XCP_TOP_PANEL_GUTTER_Y = 8;
    private static final int XCP_SCROLL_START_PERMANENTS = 6;
    private static final int XCP_NON_FLOODED_HARD_FLOOR = 48;

    public CardPluginImpl() {
        this.setGUISize();
    }

    @Init
    public void init() {
    }

    @PluginLoaded
    public void newPlugin(CardPlugin plugin) {
        LOGGER.info((Object)(plugin.toString() + " has been loaded."));
    }

    public String toString() {
        return "[Card plugin, version 0.7]";
    }

    public void changeGUISize() {
        this.setGUISize();
    }

    private void setGUISize() {
        this.cardWidthMin = (int)GUISizeHelper.battlefieldCardMinDimension.getWidth();
        this.cardWidthMax = (int)GUISizeHelper.battlefieldCardMaxDimension.getWidth();
    }

    private CardPanel makeCardPanel(CardView view, UUID gameId, boolean loadImage, ActionCallback callback, boolean isFoil, Dimension dimension, int renderModeId, boolean needFullPermanentRender) {
        CardRenderMode cardRenderMode = CardRenderMode.fromId(renderModeId);
        switch (cardRenderMode) {
            case MTGO:
            case FORCED_M15:
            case FORCED_RETRO: {
                return new CardPanelRenderModeMTGO(view, gameId, loadImage, callback, isFoil, dimension, needFullPermanentRender, renderModeId);
            }
            case IMAGE: {
                return new CardPanelRenderModeImage(view, gameId, loadImage, callback, isFoil, dimension, needFullPermanentRender);
            }
        }
        throw new IllegalStateException("Unknown render mode " + (Object)((Object)cardRenderMode));
    }

    public MageCard getMagePermanent(PermanentView permanent, Dimension dimension, UUID gameId, ActionCallback callback, boolean canBeFoil, boolean loadImage, int renderMode, boolean needFullPermanentRender) {
        CardPanel cardPanel = this.makeCardPanel((CardView)permanent, gameId, loadImage, callback, false, dimension, renderMode, needFullPermanentRender);
        cardPanel.setShowCastingCost(true);
        return cardPanel;
    }

    public MageCard getMageCard(CardView cardView, Dimension dimension, UUID gameId, ActionCallback callback, boolean canBeFoil, boolean loadImage, int renderMode, boolean needFullPermanentRender) {
        CardPanel cardPanel = this.makeCardPanel(cardView, gameId, loadImage, callback, false, dimension, renderMode, needFullPermanentRender);
        cardPanel.setShowCastingCost(true);
        return cardPanel;
    }

    private Row createStacks(Map<UUID, MageCard> cards, Row workingRow, RowType rowType) {
        block0: for (MageCard card : cards.values()) {
            MagePermanent perm = (MagePermanent)card.getMainPanel();
            if (!rowType.isType(perm) || perm.getOriginalPermanent().isAttachedToPermanent() || perm.isCreature() && !rowType.equals((Object)RowType.creature)) continue;
            int insertIndex = -1;
            int cardPower = perm.getOriginal().getOriginalPower() != null ? perm.getOriginal().getOriginalPower().getValue() : 0;
            int cardToughness = perm.getOriginal().getOriginalToughness() != null ? perm.getOriginalPermanent().getOriginalToughness().getValue() : 0;
            List cardCounters = perm.getOriginalPermanent().getCounters() != null ? perm.getOriginalPermanent().getCounters() : Collections.emptyList();
            List cardAbilities = perm.getOriginal().getRules() != null ? perm.getOriginal().getRules() : new ArrayList();
            int n = workingRow.size();
            for (int i = 0; i < n; ++i) {
                List stackAbilities;
                Stack stack = (Stack)workingRow.get(i);
                MagePermanent firstPanelPerm = (MagePermanent)stack.get(0);
                int stackPower = firstPanelPerm.getOriginal().getOriginalPower() != null ? firstPanelPerm.getOriginal().getOriginalPower().getValue() : 0;
                int stackToughness = firstPanelPerm.getOriginal().getOriginalToughness() != null ? firstPanelPerm.getOriginal().getOriginalToughness().getValue() : 0;
                List stackCounters = firstPanelPerm.getOriginalPermanent().getCounters() != null ? firstPanelPerm.getOriginalPermanent().getCounters() : Collections.emptyList();
                List list = stackAbilities = firstPanelPerm.getOriginal().getRules() != null ? firstPanelPerm.getOriginal().getRules() : new ArrayList();
                if (firstPanelPerm.getOriginal().isToken() == perm.getOriginal().isToken() && firstPanelPerm.getOriginal().getName().equals(perm.getOriginal().getName()) && stackPower == cardPower && stackToughness == cardToughness && stackAbilities.equals(cardAbilities) && stackCounters.equals(cardCounters) && (!perm.isCreature() || firstPanelPerm.getOriginalPermanent().hasSummoningSickness() == perm.getOriginalPermanent().hasSummoningSickness())) {
                    if (!this.empty(firstPanelPerm.getOriginalPermanent().getAttachments())) {
                        insertIndex = i;
                        break;
                    }
                    if (!this.empty(perm.getOriginalPermanent().getAttachments()) || stack.size() == cardStackMax) {
                        insertIndex = i + 1;
                        continue;
                    }
                    stack.add(0, perm);
                    continue block0;
                }
                if (insertIndex != -1) break;
            }
            Stack stack = new Stack();
            if (perm.getOriginalPermanent().getAttachments() != null && !perm.getOriginalPermanent().getAttachments().isEmpty() && !perm.getOriginalPermanent().isAttachedTo()) {
                AttachmentLayoutInfos ali = this.calculateNeededNumberOfVerticalColumns(0, cards, card);
                stack.setMaxAttachedCount(ali.getAttachments());
                stack.setAttachmentColumns(ali.getColumns());
            }
            stack.add(perm);
            workingRow.add(insertIndex == -1 ? workingRow.size() : insertIndex, stack);
        }
        return workingRow;
    }

    private int xcpResponsiveTwoRowMinimumWidth(int availableHeight) {
        int configuredMinimum = Math.max(1, this.cardWidthMin);
        int visualFloor = Math.min(configuredMinimum, 80);
        if (availableHeight <= 0) {
            return configuredMinimum;
        }
        int safeHeight = Math.max(1, availableHeight - 12);
        for (int candidate = configuredMinimum; candidate > visualFloor; --candidate) {
            int candidateSpacing;
            int candidateHeight = Math.round((float)candidate * 1.4f);
            int requiredHeight = candidateHeight * 2 + (candidateSpacing = Math.round((float)candidateHeight * 0.03f)) + this.effectiveGutterY * 2;
            if (requiredHeight > safeHeight) continue;
            return candidate;
        }
        return visualFloor;
    }

    /*
     * WARNING - void declaration
     */
    public int sortPermanents(Map<String, JComponent> ui, Map<UUID, MageCard> cards, boolean nonPermanentsOwnRow, boolean topPanel) {
        Stack stack;
        if (ui == null) {
            throw new RuntimeException("No battlefield ui for layout");
        }
        JLayeredPane battlefieldPanel = (JLayeredPane)ui.get("battlefieldPanel");
        JComponent cardsPanel = ui.get("jPanel");
        JScrollPane scrollPane = (JScrollPane)ui.get("scrollPane");
        if (battlefieldPanel == null || cardsPanel == null || scrollPane == null) {
            throw new RuntimeException("No battlefield components for layout");
        }
        Row rowAllLands = new Row();
        this.createStacks(cards, rowAllLands, RowType.land);
        Row rowAllCreatures = new Row();
        this.createStacks(cards, rowAllCreatures, RowType.creature);
        Row rowAllOthers = new Row();
        this.createStacks(cards, rowAllOthers, RowType.other);
        Row rowAllAttached = new Row(cards, RowType.attached);
        boolean othersOnTheRight = true;
        if (nonPermanentsOwnRow) {
            othersOnTheRight = false;
            rowAllCreatures.addAll(rowAllOthers);
            rowAllOthers.clear();
        }
        this.cardWidth = this.cardWidthMax;
        Rectangle rect = battlefieldPanel.getVisibleRect();
        Dimension viewportExtent = scrollPane.getViewport().getExtentSize();
        this.playAreaWidth = Math.max(rect.width, viewportExtent.width);
        this.playAreaHeight = Math.max(rect.height, viewportExtent.height);
        this.xcpBattlefieldPermanentCount = cards.size();
        this.effectiveGutterY = topPanel ? 8 : 15;
        int responsiveMinimum = this.xcpResponsiveTwoRowMinimumWidth(this.playAreaHeight);
        this.effectiveCardWidthMin = this.xcpBattlefieldPermanentCount >= 6 ? Math.min(this.cardWidthMin, responsiveMinimum) : Math.min(this.cardWidthMin, 48);
        while (true) {
            int addOthersIndex;
            this.rows.clear();
            this.cardHeight = Math.round((float)this.cardWidth * 1.4f);
            this.extraCardSpacingX = Math.round((float)this.cardWidth * 0.04f);
            this.cardSpacingX = this.cardHeight - this.cardWidth + this.extraCardSpacingX;
            this.cardSpacingY = Math.round((float)this.cardHeight * 0.03f);
            this.stackSpacingX = Math.round((float)this.cardWidth * 0.07f);
            this.stackSpacingY = Math.round((float)this.cardHeight * 0.1f);
            this.attachmentSpacingY = Math.round((float)this.cardHeight * 0.13f);
            Row creatures = (Row)rowAllCreatures.clone();
            Row lands = (Row)rowAllLands.clone();
            Row others = (Row)rowAllOthers.clone();
            if (topPanel) {
                this.wrap(lands, this.rows, -1);
                this.wrap(others, this.rows, this.rows.size());
                addOthersIndex = this.rows.size();
                this.wrap(creatures, this.rows, addOthersIndex);
            } else {
                this.wrap(creatures, this.rows, -1);
                addOthersIndex = this.rows.size();
                this.wrap(lands, this.rows, this.rows.size());
                this.wrap(others, this.rows, this.rows.size());
            }
            ArrayList<Row> storedRows = new ArrayList<Row>(this.rows.size());
            for (Row row : this.rows) {
                storedRows.add((Row)row.clone());
            }
            Row storedOthers = (Row)others.clone();
            for (Row row3 : this.rows) {
                this.fillRow(others, this.rows, row3);
            }
            if (creatures.isEmpty() && lands.isEmpty() && others.isEmpty()) break;
            this.rows = storedRows;
            others = storedOthers;
            this.wrap(others, this.rows, addOthersIndex);
            for (Row row : this.rows) {
                this.fillRow(others, this.rows, row);
            }
            if (creatures.isEmpty() && lands.isEmpty() && others.isEmpty()) break;
            this.cardWidth -= 4;
        }
        int y = this.effectiveGutterY;
        int maxRowWidth = 0;
        for (Row row : this.rows) {
            int var22_36 = 0;
            int rowBottom = 0;
            int x = 15;
            boolean bl = false;
            int stackCount = row.size();
            while (var22_36 < stackCount) {
                stack = (Stack)row.get((int)var22_36);
                rowBottom = Math.max(rowBottom, y + stack.getHeight());
                x += stack.getWidth();
                ++var22_36;
            }
            y = rowBottom;
            maxRowWidth = Math.max(maxRowWidth, x);
        }
        y = this.effectiveGutterY;
        for (Row row : this.rows) {
            int var22_38 = 0;
            int rowBottom = 0;
            int x = 15;
            boolean bl = false;
            int stackCount = row.size();
            while (var22_38 < stackCount) {
                stack = (Stack)row.get((int)var22_38);
                if (othersOnTheRight && RowType.other.isType((MagePermanent)stack.get(0))) {
                    x = this.playAreaWidth - 15 + this.extraCardSpacingX;
                    int n = row.size();
                    for (int i = var22_38; i < n; ++i) {
                        x -= ((Stack)row.get((int)i)).getWidth();
                    }
                }
                int panelCount = stack.size();
                for (int panelIndex = 0; panelIndex < panelCount; ++panelIndex) {
                    MagePermanent panelPerm = (MagePermanent)stack.get(panelIndex);
                    int stackPosition = panelCount - panelIndex - 1;
                    if (cardsPanel != null) {
                        cardsPanel.setComponentZOrder((Component)panelPerm.getTopPanelRef(), panelIndex);
                    }
                    int panelX = x + stackPosition * this.stackSpacingX;
                    int panelY = y + stackPosition * this.stackSpacingY;
                    try {
                        battlefieldPanel.moveToFront((Component)panelPerm.getTopPanelRef());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    panelPerm.getTopPanelRef().setCardBounds(panelX, panelY, this.cardWidth, this.cardHeight);
                }
                rowBottom = Math.max(rowBottom, y + stack.getHeight());
                x += stack.getWidth();
                ++var22_38;
            }
            y = rowBottom;
        }
        for (Stack stack2 : rowAllAttached) {
            for (MagePermanent magePermanent : stack2) {
                magePermanent.getTopPanelRef().setCardBounds(0, 0, this.cardWidth, this.cardHeight);
            }
        }
        scrollPane.getVerticalScrollBar().setUnitIncrement(GUISizeHelper.getCardsScrollbarUnitInc(this.cardHeight));
        return y;
    }

    private boolean empty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private int wrap(Row sourceRow, List<Row> rows, int insertIndex) {
        boolean allowHeightOverflow = this.xcpBattlefieldPermanentCount >= 6 ? this.cardWidth <= this.effectiveCardWidthMin : this.cardWidth <= 48;
        Row currentRow = new Row();
        int n = sourceRow.size() - 1;
        for (int i = 0; i <= n; ++i) {
            Stack stack = (Stack)sourceRow.get(i);
            int rowWidth = currentRow.getWidth();
            if (!currentRow.isEmpty() && rowWidth + stack.getWidth() > this.playAreaWidth) {
                if (!allowHeightOverflow && rowWidth > this.playAreaWidth || !allowHeightOverflow && this.getRowsHeight(rows) + sourceRow.getHeight() > this.playAreaHeight) break;
                rows.add(insertIndex == -1 ? rows.size() : insertIndex, currentRow);
                currentRow = new Row();
            }
            currentRow.add(stack);
        }
        if (!currentRow.isEmpty()) {
            int rowWidth = currentRow.getWidth();
            if ((allowHeightOverflow || rowWidth <= this.playAreaWidth) && (allowHeightOverflow || this.getRowsHeight(rows) + sourceRow.getHeight() <= this.playAreaHeight)) {
                rows.add(insertIndex == -1 ? rows.size() : insertIndex, currentRow);
            }
        }
        for (Row row : rows) {
            for (Stack stack : row) {
                sourceRow.remove(stack);
            }
        }
        return insertIndex;
    }

    private void fillRow(Row sourceRow, List<Row> rows, Row row) {
        Stack stack;
        int rowWidth = row.getWidth();
        while (!(sourceRow.isEmpty() || (rowWidth += (stack = (Stack)sourceRow.get(0)).getWidth()) > this.playAreaWidth || stack.getHeight() > row.getHeight() && this.getRowsHeight(rows) - row.getHeight() + stack.getHeight() > this.playAreaHeight)) {
            row.add((Stack)sourceRow.remove(0));
        }
    }

    private int getRowsHeight(List<Row> rows) {
        int height = 0;
        for (Row row : rows) {
            height += row.getHeight();
        }
        return height - this.cardSpacingY + this.effectiveGutterY * 2;
    }

    private AttachmentLayoutInfos calculateNeededNumberOfVerticalColumns(int currentCol, Map<UUID, MageCard> cards, MageCard cardWithAttachments) {
        int maxCol = ++currentCol;
        int attachments = 0;
        MagePermanent permWithAttachments = (MagePermanent)cardWithAttachments.getMainPanel();
        for (UUID attachmentId : permWithAttachments.getOriginalPermanent().getAttachments()) {
            AttachmentLayoutInfos attachmentLayoutInfos;
            MageCard attachedCard = cards.get(attachmentId);
            if (attachedCard == null) continue;
            ++attachments;
            MagePermanent attachedPerm = (MagePermanent)attachedCard.getMainPanel();
            if (attachedPerm.getOriginalPermanent().getAttachments() == null || attachedPerm.getOriginalPermanent().getAttachments().isEmpty() || (attachmentLayoutInfos = this.calculateNeededNumberOfVerticalColumns(currentCol, cards, attachedCard)).getColumns() <= maxCol) continue;
            maxCol = attachmentLayoutInfos.getColumns();
            attachments += attachmentLayoutInfos.getAttachments();
        }
        return new AttachmentLayoutInfos(maxCol, attachments);
    }

    private void symbolsOnFinish() {
    }

    public void downloadSymbols(String imagesDir) {
        final Downloader downloader = new Downloader();
        DownloadGui downloadGui = new DownloadGui(downloader);
        LOGGER.info((Object)"Download: prepare symbols to download...");
        Iterable<DownloadJob> jobs = new GathererSymbols();
        for (DownloadJob downloadJob : jobs) {
        }
        jobs = new GathererSets();
        for (DownloadJob job : jobs) {
            downloader.add(job);
        }
        jobs = new ScryfallSymbolsSource();
        for (DownloadJob job : jobs) {
            downloader.add(job);
        }
        jobs = new DirectLinksForDownload();
        for (DownloadJob job : jobs) {
            downloader.add(job);
        }
        LOGGER.info((Object)("Download: app used " + downloader.getJobs().size() + " symbol files"));
        final JDialog dialog = new JDialog((Frame)null, "Download symbols", false);
        dialog.setDefaultCloseOperation(2);
        dialog.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent e) {
                downloader.cleanup();
            }
        });
        dialog.setLayout(new BorderLayout());
        dialog.add(downloadGui);
        dialog.pack();
        dialog.setVisible(true);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){

            @Override
            protected Void doInBackground() throws Exception {
                downloader.publishAllJobs();
                downloader.waitFinished();
                downloader.cleanup();
                return null;
            }
        };
        worker.addPropertyChangeListener(new PropertyChangeListener(){

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("state") && evt.getNewValue() == SwingWorker.StateValue.DONE) {
                    LOGGER.info((Object)"Download: symbols download finished");
                    dialog.dispose();
                    ManaSymbols.loadImages();
                    GUISizeHelper.refreshGUIAndCards(false);
                }
            }
        });
        worker.execute();
    }

    public void onAddCard(MageCard card, int count) {
        if (card != null) {
            Animation.showCard(card, count > 0 ? count : 1);
            try {
                while (card.getAlpha() + 0.05f < 1.0f) {
                    TimeUnit.MILLISECONDS.sleep(30L);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onRemoveCard(MageCard card, int count) {
        if (card != null) {
            Animation.hideCard(card, count > 0 ? count : 1);
            try {
                while (card.getAlpha() - 0.05f > 0.0f) {
                    TimeUnit.MILLISECONDS.sleep(30L);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public JComponent getCardInfoPane() {
        return new CardInfoPaneImpl();
    }

    public BufferedImage getOriginalImage(CardView card) {
        return ImageCache.getCardImageOriginal(card).getImage();
    }

    private final class AttachmentLayoutInfos {
        private int columns;
        private int attachments;

        public AttachmentLayoutInfos(int columns, int attachments) {
            this.columns = columns;
            this.attachments = attachments;
        }

        public int getColumns() {
            return this.columns;
        }

        public int getAttachments() {
            return this.attachments;
        }

        public void increaseAttachments() {
            ++this.attachments;
        }

        public void increaseColumns() {
            ++this.columns;
        }
    }

    private static enum RowType {
        land,
        creature,
        other,
        attached;


        public boolean isType(MagePermanent permanent) {
            switch (this) {
                case land: {
                    return permanent.isLand();
                }
                case creature: {
                    return permanent.isCreature();
                }
                case other: {
                    return !permanent.isLand() && !permanent.isCreature();
                }
                case attached: {
                    return permanent.getOriginalPermanent().isAttachedToPermanent();
                }
            }
            throw new RuntimeException("Unhandled type: " + (Object)((Object)this));
        }
    }

    private class Stack
    extends ArrayList<MagePermanent> {
        private static final long serialVersionUID = 1L;
        private int maxAttachedCount;
        private int attachmentColumns;

        public Stack() {
            super(8);
            this.maxAttachedCount = 0;
            this.attachmentColumns = 0;
        }

        private int getWidth() {
            return CardPluginImpl.this.cardWidth + (this.size() - 1) * CardPluginImpl.this.stackSpacingX + CardPluginImpl.this.cardSpacingX + 12 * this.attachmentColumns;
        }

        private int getHeight() {
            return CardPluginImpl.this.cardHeight + (this.size() - 1) * CardPluginImpl.this.stackSpacingY + CardPluginImpl.this.cardSpacingY + CardPluginImpl.this.attachmentSpacingY * this.maxAttachedCount;
        }

        public int getMaxAttachedCount() {
            return this.maxAttachedCount;
        }

        public void setMaxAttachedCount(int maxAttachedCount) {
            this.maxAttachedCount = maxAttachedCount;
        }

        public void setAttachmentColumns(int attachmentColumns) {
            this.attachmentColumns = attachmentColumns;
        }
    }

    private class Row
    extends ArrayList<Stack> {
        private static final long serialVersionUID = 1L;

        public Row() {
            super(16);
        }

        public Row(Map<UUID, MageCard> cards, RowType type) {
            this();
            this.addAll(cards, type);
        }

        private void addAll(Map<UUID, MageCard> cards, RowType type) {
            for (MageCard card : cards.values()) {
                MagePermanent perm = (MagePermanent)card.getMainPanel();
                if (!type.isType(perm) || type != RowType.attached && RowType.attached.isType(perm)) continue;
                Stack stack = new Stack();
                stack.add(perm);
                if (perm.getOriginalPermanent().getAttachments() != null) {
                    AttachmentLayoutInfos ali = CardPluginImpl.this.calculateNeededNumberOfVerticalColumns(0, cards, card);
                    stack.setMaxAttachedCount(ali.getAttachments());
                    stack.setAttachmentColumns(ali.getColumns());
                }
                this.add(stack);
            }
        }

        @Override
        public boolean addAll(Collection<? extends Stack> c) {
            boolean changed = super.addAll(c);
            c.clear();
            return changed;
        }

        private int getWidth() {
            if (this.isEmpty()) {
                return 0;
            }
            int width = 0;
            for (Stack stack : this) {
                width += stack.getWidth();
            }
            return width + 30 - CardPluginImpl.this.extraCardSpacingX;
        }

        private int getHeight() {
            if (this.isEmpty()) {
                return 0;
            }
            int height = 0;
            for (Stack stack : this) {
                height = Math.max(height, stack.getHeight());
            }
            return height;
        }
    }
}
