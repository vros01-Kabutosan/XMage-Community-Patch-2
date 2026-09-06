/*
 * Decompiled with CFR.
 */
package org.mage.card.arcane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import mage.cards.repository.ExpansionRepository;
import mage.client.MageFrame;
import mage.client.constants.Constants;
import mage.client.util.GUISizeHelper;
import mage.client.util.ImageHelper;
import mage.client.util.gui.BufferedImageBuilder;
import mage.client.util.gui.GuiDisplayUtil;
import mage.constants.Rarity;
import org.apache.log4j.Logger;
import org.mage.card.arcane.ModernCardRenderer;
import org.mage.card.arcane.SvgUtils;
import org.mage.card.arcane.UI;
import org.mage.plugins.card.utils.CardImageUtils;

public final class ManaSymbols {
    private static final Logger logger = Logger.getLogger(ManaSymbols.class);
    private static final String CSS_FILE_NAME = "mana-svg-settings.css";
    private static final String CSS_ADDITIONAL_SETTINGS = "";
    private static final Map<Integer, Map<String, BufferedImage>> manaImages = new HashMap<Integer, Map<String, BufferedImage>>();
    private static final Map<String, Map<Rarity, Image>> setImages = new ConcurrentHashMap<String, Map<Rarity, Image>>();
    private static final Set<String> onlyMythics = new HashSet<String>();
    private static final Set<String> withoutSymbols = new HashSet<String>();
    private static final Map<String, Dimension> setImagesExist;
    private static final Pattern REPLACE_SYMBOLS_PATTERN;
    private static final String[] symbols;
    private static final JLabel labelRender;

    public static void loadImages() {
        logger.info((Object)"Symbols: loading...");
        SvgUtils.prepareCss(CSS_FILE_NAME, CSS_ADDITIONAL_SETTINGS, true);
        ManaSymbols.loadSymbolImages(15);
        ManaSymbols.loadSymbolImages(25);
        ManaSymbols.loadSymbolImages(50);
        Map<String, BufferedImage> pngImages = manaImages.get(50);
        if (pngImages != null) {
            File pngPath = new File(ManaSymbols.getResourceSymbolsPath(Constants.ResourceSymbolSize.PNG));
            if (!pngPath.exists()) {
                pngPath.mkdirs();
            }
            for (String symbol : symbols) {
                try {
                    BufferedImage image = pngImages.get(symbol);
                    if (image == null) continue;
                    File newFile = new File(pngPath.getPath() + File.separator + symbol + ".png");
                    ImageIO.write((RenderedImage)image, "png", newFile);
                }
                catch (Exception e) {
                    logger.warn((Object)("Symbols: can't generate png image for symbol:" + symbol));
                }
            }
        }
        List<String> setCodes = ExpansionRepository.instance.getSetCodes();
        for (String set : setCodes) {
            if (withoutSymbols.contains(set)) continue;
            EnumSet<Rarity> codes = onlyMythics.contains(set) ? EnumSet.of(Rarity.MYTHIC) : EnumSet.of(Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.MYTHIC);
            EnumMap<Rarity, Image> rarityImages = new EnumMap<Rarity, Image>(Rarity.class);
            setImages.put(set, rarityImages);
            for (Rarity rarityCode : codes) {
                File file = new File(ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.LARGE) + set + '-' + rarityCode.getCode() + ".png");
                try {
                    Image image = UI.getImageIcon(file.getAbsolutePath()).getImage();
                    int width = image.getWidth(null);
                    if (width > 21) {
                        int h = image.getHeight(null);
                        if (h <= 0) continue;
                        Rectangle r = new Rectangle(21, (int)((float)h * 21.0f / (float)width));
                        BufferedImage resized = ImageHelper.getResizedImage(BufferedImageBuilder.bufferImage(image, 2), r);
                        rarityImages.put(rarityCode, resized);
                        continue;
                    }
                    rarityImages.put(rarityCode, image);
                }
                catch (Exception image) {}
            }
            try {
                File file = new File(ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.LARGE));
                if (!file.exists()) {
                    file.mkdirs();
                }
                String pathRoot = ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.SMALL) + set;
                for (Rarity code : codes) {
                    File newFile = new File(pathRoot + '-' + (Object)((Object)code) + ".png");
                    if (MageFrame.isSkipSmallSymbolGenerationForExisting() && newFile.exists() || !(file = new File(ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.LARGE) + set + '-' + (Object)((Object)code) + ".png")).exists()) continue;
                    Image image = UI.getImageIcon(file.getAbsolutePath()).getImage();
                    try {
                        int width = image.getWidth(null);
                        int height = image.getHeight(null);
                        if (height <= 0) continue;
                        int dx = 0;
                        if (set.equals("M10") || set.equals("M11") || set.equals("M12")) {
                            dx = 6;
                        }
                        Rectangle r = new Rectangle(15 + dx, (int)((float)height * (15.0f + (float)dx) / (float)width));
                        BufferedImage resized = ImageHelper.getResizedImage(BufferedImageBuilder.bufferImage(image, 2), r);
                        ImageIO.write((RenderedImage)resized, "png", newFile);
                    }
                    catch (Exception e) {
                        if (!file.exists()) continue;
                        file.delete();
                    }
                }
            }
            catch (Exception file) {
            }
        }
        ManaSymbols.scanAndGenerateMissingSetSymbols();
        for (String set : ExpansionRepository.instance.getSetCodes()) {
            File file = new File(ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.SMALL));
            if (!file.exists()) break;
            file = new File(ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.SMALL) + set + "-C.png");
            try {
                Image image = UI.getImageIcon(file.getAbsolutePath()).getImage();
                int width = image.getWidth(null);
                int height = image.getHeight(null);
                setImagesExist.put(set, new Dimension(width, height));
            }
            catch (Exception exception) {}
        }
        logger.info((Object)"Symbols: done");
    }

    public static File getSymbolFileNameAsSVG(String symbol) {
        return new File(ManaSymbols.getResourceSymbolsPath(Constants.ResourceSymbolSize.SVG) + symbol + ".svg");
    }

    private static BufferedImage loadSymbolAsSVG(InputStream svgFile, String svgInfo, int resizeToWidth, int resizeToHeight) {
        try {
            return SvgUtils.loadSVG(svgFile, svgInfo, CSS_FILE_NAME, CSS_ADDITIONAL_SETTINGS, resizeToWidth, resizeToHeight, true);
        }
        catch (Exception e) {
            logger.error((Object)("Can't load svg symbol: " + svgInfo + " , reason: " + e.getMessage()));
            return null;
        }
    }

    private static File getSymbolFileNameAsPNG(String symbol, int size) {
        Constants.ResourceSymbolSize needSize = null;
        needSize = size <= 15 ? Constants.ResourceSymbolSize.SMALL : (size <= 25 ? Constants.ResourceSymbolSize.MEDIUM : Constants.ResourceSymbolSize.LARGE);
        return new File(ManaSymbols.getResourceSymbolsPath(needSize) + symbol + ".png");
    }

    private static BufferedImage loadSymbolAsPNG(String symbol, int resizeToWidth, int resizeToHeight) {
        File file = ManaSymbols.getSymbolFileNameAsPNG(symbol, resizeToWidth);
        return ManaSymbols.loadSymbolAsPNG(file, resizeToWidth, resizeToHeight);
    }

    private static BufferedImage loadSymbolAsPNG(File sourceFile, int resizeToWidth, int resizeToHeight) {
        BufferedImage image = null;
        try {
            if (resizeToWidth == 15 || resizeToWidth == 25) {
                image = ImageIO.read(sourceFile);
            } else {
                image = ImageIO.read(sourceFile);
                if (image != null) {
                    Rectangle r = new Rectangle(resizeToWidth, resizeToHeight);
                    image = ImageHelper.getResizedImage(image, r);
                }
            }
        }
        catch (IOException e) {
            logger.error((Object)("Can't load gif symbol: " + sourceFile.getPath()));
            return null;
        }
        return image;
    }

    private static boolean loadSymbolImages(int size) {
        ArrayList svgFails = new ArrayList();
        ArrayList otherFails = new ArrayList();
        ConcurrentHashMap sizedSymbols = new ConcurrentHashMap();
        IntStream.range(0, symbols.length).parallel().forEach(i -> {
            List list;
            File file;
            String symbol = symbols[i];
            BufferedImage image = null;
            if (SvgUtils.haveSvgSupport() && (file = ManaSymbols.getSymbolFileNameAsSVG(symbol)).exists()) {
                try (InputStream fileStream = Files.newInputStream(file.toPath(), new OpenOption[0]);){
                    image = ManaSymbols.loadSymbolAsSVG(fileStream, file.getPath(), size, size);
                }
                catch (IOException iOException) {
                    // empty catch block
                }
            }
            if (image == null) {
                list = svgFails;
                synchronized (list) {
                    svgFails.add(symbol);
                }
            }
            if (image == null && (file = ManaSymbols.getSymbolFileNameAsPNG(symbol, size)).exists()) {
                image = ManaSymbols.loadSymbolAsPNG(file, size, size);
            }
            if (image == null) {
                list = otherFails;
                synchronized (list) {
                    otherFails.add(symbol);
                }
            }
            if (image != null) {
                sizedSymbols.put(symbol, image);
            }
        });
        String errorInfo = CSS_ADDITIONAL_SETTINGS;
        if (!svgFails.isEmpty()) {
            errorInfo = errorInfo + String.format("SVG miss - %s and %d others", svgFails.get(0), svgFails.size() - 1);
        }
        if (!otherFails.isEmpty()) {
            if (!errorInfo.isEmpty()) {
                errorInfo = errorInfo + ", ";
            }
            errorInfo = errorInfo + String.format("GIF miss - %s and %d others", otherFails.get(0), otherFails.size() - 1);
        }
        if (!errorInfo.isEmpty()) {
            logger.warn((Object)("Symbols: can't load, make sure you download it by main menu - size " + size + ", " + errorInfo));
        }
        manaImages.put(size, sizedSymbols);
        return errorInfo.isEmpty();
    }

    private static String getResourceSymbolsPath(Constants.ResourceSymbolSize needSize) {
        String path = CardImageUtils.getImagesDir() + Constants.RESOURCE_PATH_SYMBOLS + File.separator;
        switch (needSize) {
            case SMALL: {
                path = path + "small";
                break;
            }
            case MEDIUM: {
                path = path + "medium";
                break;
            }
            case LARGE: {
                path = path + "large";
                break;
            }
            case SVG: {
                path = path + "svg";
                break;
            }
            case PNG: {
                path = path + "png";
                break;
            }
            default: {
                throw new IllegalArgumentException("ResourceSymbolSize value is unknown");
            }
        }
        while (path.endsWith(File.separator)) {
            path = path.substring(0, path.length() - 1);
        }
        return path + File.separator;
    }

    private static String getResourceSetsPath(Constants.ResourceSetSize needSize) {
        String path = CardImageUtils.getImagesDir() + Constants.RESOURCE_PATH_SYMBOLS + File.separator;
        switch (needSize) {
            case SMALL: {
                path = path + "small";
                break;
            }
            case LARGE: {
                path = path + "large";
                break;
            }
            case SVG: {
                path = path + "svg";
                break;
            }
            default: {
                throw new IllegalArgumentException("ResourceSetSize value is unknown");
            }
        }
        while (path.endsWith(File.separator)) {
            path = path.substring(0, path.length() - 1);
        }
        return path + File.separator;
    }

    public static void draw(Graphics g, String manaCost, int x, int y, int symbolWidth) {
        ManaSymbols.draw(g, manaCost, x, y, symbolWidth, ModernCardRenderer.MANA_ICONS_TEXT_COLOR, 0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void draw(Graphics g, String manaCost, int x, int y, int symbolWidth, Color symbolsTextColor, int symbolMarginX) {
        if (!manaImages.containsKey(symbolWidth)) {
            ManaSymbols.loadSymbolImages(symbolWidth);
        }
        Map<String, BufferedImage> sizedSymbols = manaImages.get(symbolWidth);
        if (manaCost.isEmpty()) {
            return;
        }
        manaCost = manaCost.replace("\\", CSS_ADDITIONAL_SETTINGS);
        manaCost = UI.getDisplayManaCost(manaCost);
        StringTokenizer tok = new StringTokenizer(manaCost, " ");
        while (tok.hasMoreTokens()) {
            String symbol = tok.nextToken();
            Image image = sizedSymbols.get(symbol);
            if (image == null && symbol != null && symbol.length() == 2) {
                String symbol2 = CSS_ADDITIONAL_SETTINGS + symbol.charAt(1) + symbol.charAt(0);
                image = sizedSymbols.get(symbol2);
            }
            if (image == null) {
                String sampleAutoFontText = "{W}";
                if (symbol.equals("*")) {
                    labelRender.setText(" / ");
                    sampleAutoFontText = " / ";
                } else {
                    labelRender.setText("{" + symbol + "}");
                }
                labelRender.setSize(symbolWidth, symbolWidth);
                labelRender.setVerticalAlignment(0);
                labelRender.setForeground(symbolsTextColor);
                labelRender.setHorizontalAlignment(0);
                Font labelFont = labelRender.getFont();
                int stringWidth = labelRender.getFontMetrics(labelFont).stringWidth(sampleAutoFontText);
                int componentWidth = labelRender.getWidth();
                double widthRatio = (double)componentWidth / (double)stringWidth;
                int newFontSize = (int)((double)labelFont.getSize() * widthRatio);
                int componentHeight = labelRender.getHeight();
                int fontSizeToUse = Math.min(newFontSize, componentHeight);
                labelRender.setFont(new Font(labelFont.getName(), 1, fontSizeToUse - 1));
                Graphics2D g2 = (Graphics2D)g.create(x, y, symbolWidth, symbolWidth);
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.fillOval(x + 1, y + 1, symbolWidth - 2, symbolWidth - 2);
                    labelRender.paint(g2);
                }
                finally {
                    g2.dispose();
                }
            } else {
                g.drawImage(image, x, y, null);
            }
            x += symbolWidth + symbolMarginX;
        }
    }

    public static String getClearManaCost(List<String> manaCost) {
        StringBuilder sb = new StringBuilder();
        for (String s : manaCost) {
            sb.append(s);
        }
        return ManaSymbols.getClearManaCost(sb.toString());
    }

    public static String getClearManaCost(String manaCost) {
        return manaCost.replace("/", CSS_ADDITIONAL_SETTINGS).replace("{", CSS_ADDITIONAL_SETTINGS).replace("}", " ").trim();
    }

    public static int getClearManaSymbolsCount(String manaCost) {
        if (manaCost.isEmpty()) {
            return 0;
        }
        String clearManaCost = ManaSymbols.getClearManaCost(manaCost);
        String checkManaCost = clearManaCost.replace(" ", CSS_ADDITIONAL_SETTINGS);
        return clearManaCost.length() - checkManaCost.length() + 1;
    }

    private static String filePathToUrl(String path) {
        if (path != null && !path.equals(CSS_ADDITIONAL_SETTINGS)) {
            File file = new File(path);
            return file.toURI().toString();
        }
        return null;
    }

    public static String replaceSymbolsWithHTML(String value, Type destType) {
        int symbolSize;
        switch (destType) {
            case TABLE: {
                symbolSize = GUISizeHelper.symbolTableSize;
                break;
            }
            case CHAT: {
                symbolSize = GUISizeHelper.symbolChatSize;
                break;
            }
            case DIALOG: {
                symbolSize = GUISizeHelper.symbolDialogSize;
                break;
            }
            case TOOLTIP:
            case CARD_ICON_HINT: {
                symbolSize = GUISizeHelper.symbolTooltipSize;
                break;
            }
            default: {
                symbolSize = 11;
            }
        }
        return ManaSymbols.replaceSymbolsWithHTML(value, symbolSize);
    }

    public static String replaceSymbolsWithHTML(String value, int symbolsSize) {
        String replaced = value.replace("{this}", "|this|");
        String htmlImagesPath = ManaSymbols.getResourceSymbolsPath(Constants.ResourceSymbolSize.PNG);
        htmlImagesPath = htmlImagesPath.replace("$", "@S@");
        replaced = replaced.replace("{*}", " / ");
        if ((replaced = REPLACE_SYMBOLS_PATTERN.matcher(replaced).replaceAll("<img src='" + ManaSymbols.filePathToUrl(htmlImagesPath) + "$1$2$3.png' alt='$1$2$3' width=" + symbolsSize + " height=" + symbolsSize + '>')).contains("ICON_GOOD")) {
            replaced = replaced.replace("ICON_GOOD", GuiDisplayUtil.getHintIconHtml("good", symbolsSize) + "&nbsp;");
        }
        if (replaced.contains("ICON_BAD")) {
            replaced = replaced.replace("ICON_BAD", GuiDisplayUtil.getHintIconHtml("bad", symbolsSize) + "&nbsp;");
        }
        if (replaced.contains("ICON_RESTRICT")) {
            replaced = replaced.replace("ICON_RESTRICT", GuiDisplayUtil.getHintIconHtml("restrict", symbolsSize) + "&nbsp;");
        }
        if (replaced.contains("ICON_REQUIRE")) {
            replaced = replaced.replace("ICON_REQUIRE", GuiDisplayUtil.getHintIconHtml("require", symbolsSize) + "&nbsp;");
        }
        if (replaced.contains("ICON_DUNGEON_ROOM_CURRENT")) {
            replaced = replaced.replace("ICON_DUNGEON_ROOM_CURRENT", GuiDisplayUtil.getHintIconHtml("arrow-right-square-fill-green", symbolsSize) + "&nbsp;");
        }
        if (replaced.contains("ICON_DUNGEON_ROOM_NEXT")) {
            replaced = replaced.replace("ICON_DUNGEON_ROOM_NEXT", GuiDisplayUtil.getHintIconHtml("arrow-down-right-square fill-yellow", symbolsSize) + "&nbsp;");
        }
        replaced = replaced.replace("|this|", "{this}").replace("@S@", "$");
        return replaced;
    }

    public static String replaceSetCodeWithHTML(String set, String rarity, int size) {
        if (set == null || set.trim().isEmpty()) {
            return CSS_ADDITIONAL_SETTINGS;
        }
        set = set.trim().toUpperCase(Locale.ENGLISH);
        rarity = ManaSymbols.normalizeSetRarityCode(rarity);
        File largeFile = new File(ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.LARGE) + set + '-' + rarity + ".png");
        File smallFile = new File(ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.SMALL) + set + '-' + rarity + ".png");
        String badge = ManaSymbols.compactSetCodeFallback(set, rarity, size);
        if (!largeFile.exists() && !smallFile.exists()) {
            ManaSymbols.getOrCreateCompactSetCodeIcon(set, rarity, size, true);
            return badge;
        }
        File iconFile = smallFile.exists() ? smallFile : largeFile;
        try {
            BufferedImage image = ImageIO.read(iconFile);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return badge;
            }
            int targetHeight = Math.max(16, Math.min(20, size));
            int targetWidth = Math.max(14, (int)Math.round((double)image.getWidth() * ((double)targetHeight / (double)image.getHeight())));
            targetWidth = Math.min(42, targetWidth);
            return "<img src='" + ManaSymbols.filePathToUrl(iconFile.getAbsolutePath()) + "' alt='" + set + "' height='" + targetHeight + "' width='" + targetWidth + "'>&nbsp;" + badge;
        }
        catch (Exception e) {
            return badge;
        }
    }

    private static void scanAndGenerateMissingSetSymbols() {
        try {
            File smallDir = new File(ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.SMALL));
            if (!smallDir.exists()) {
                smallDir.mkdirs();
            }
            StringBuilder report = new StringBuilder();
            report.append("XMage Community Patch - Missing set symbols scan R5.2i\n");
            report.append("Audited every set/rarity. Missing official symbols get a compact code badge fallback.\n\n");
            int missing = 0;
            int generated = 0;
            List<String> setCodes = ExpansionRepository.instance.getSetCodes();
            for (String set : setCodes) {
                if (set == null || withoutSymbols.contains(set)) continue;
                EnumSet<Rarity> codes = onlyMythics.contains(set) ? EnumSet.of(Rarity.MYTHIC) : EnumSet.of(Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.MYTHIC);
                for (Rarity rarityCode : codes) {
                    String rarity = rarityCode.getCode();
                    File largeFile = new File(ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.LARGE) + set + '-' + rarity + ".png");
                    if (largeFile.exists()) continue;
                    ++missing;
                    File fallback = ManaSymbols.getOrCreateCompactSetCodeIcon(set, rarity, GUISizeHelper.symbolTooltipSize, true);
                    if (fallback != null && fallback.exists()) {
                        ++generated;
                    }
                    report.append(set).append('-').append(rarity).append("  MISSING_OFFICIAL_SYMBOL");
                    if (fallback != null) {
                        report.append("  fallback=").append(fallback.getName());
                    }
                    report.append('\n');
                }
            }
            report.append("\nMissing official set symbols: ").append(missing).append('\n');
            report.append("Generated/updated compact fallbacks: ").append(generated).append('\n');
            File reportFile = new File(smallDir, "xcp-missing-set-symbols-report-r5-2i.txt");
            try (OutputStreamWriter writer = new OutputStreamWriter((OutputStream)new FileOutputStream(reportFile), "UTF-8");){
                writer.write(report.toString());
            }
        }
        catch (Exception e) {
            logger.warn((Object)("Symbols: R5.2i missing set symbol scan failed: " + e.getMessage()));
        }
    }

    private static String normalizeSetRarityCode(String rarity) {
        if (rarity == null || rarity.trim().isEmpty()) {
            return Rarity.COMMON.getCode();
        }
        String code = rarity.trim();
        if (code.equalsIgnoreCase("common")) {
            return Rarity.COMMON.getCode();
        }
        if (code.equalsIgnoreCase("uncommon")) {
            return Rarity.UNCOMMON.getCode();
        }
        if (code.equalsIgnoreCase("rare")) {
            return Rarity.RARE.getCode();
        }
        if (code.equalsIgnoreCase("mythic") || code.equalsIgnoreCase("mythic rare")) {
            return Rarity.MYTHIC.getCode();
        }
        return code.toUpperCase(Locale.ENGLISH);
    }

    private static File getOrCreateCompactSetCodeIcon(String set, String rarity, int size) {
        return ManaSymbols.getOrCreateCompactSetCodeIcon(set, rarity, size, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static File getOrCreateCompactSetCodeIcon(String set, String rarity, int size, boolean forceRewrite) {
        try {
            File dir = new File(ManaSymbols.getResourceSetsPath(Constants.ResourceSetSize.SMALL));
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            File target = new File(dir, set + '-' + rarity + ".png");
            if (target.exists() && !forceRewrite) {
                return target;
            }
            int height = Math.max(15, Math.min(18, size));
            int width = Math.max(32, set.length() * 9 + 12);
            BufferedImage image = new BufferedImage(width, height, 2);
            Graphics2D g = image.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Color color = ManaSymbols.getCompactFallbackColor(rarity);
                g.setColor(new Color(255, 255, 255, 230));
                g.fillRoundRect(0, 0, width - 1, height - 1, 6, 6);
                g.setColor(color);
                g.drawRoundRect(0, 0, width - 1, height - 1, 6, 6);
                Font font = new Font("SansSerif", 1, Math.max(10, height - 6));
                g.setFont(font);
                FontMetrics fm = g.getFontMetrics();
                int x = Math.max(3, (width - fm.stringWidth(set)) / 2);
                int y = Math.max(fm.getAscent(), (height - fm.getHeight()) / 2 + fm.getAscent() - 1);
                g.drawString(set, x, y);
            }
            finally {
                g.dispose();
            }
            ImageIO.write((RenderedImage)image, "png", target);
            return target;
        }
        catch (Exception e) {
            return null;
        }
    }

    private static Color getCompactFallbackColor(String rarity) {
        if ("M".equalsIgnoreCase(rarity)) {
            return new Color(213, 51, 11);
        }
        if ("R".equalsIgnoreCase(rarity)) {
            return new Color(184, 134, 11);
        }
        if ("U".equalsIgnoreCase(rarity)) {
            return new Color(95, 95, 95);
        }
        return new Color(17, 17, 17);
    }

    private static String compactSetCodeFallback(String set, String rarity, int size) {
        String color = "M".equalsIgnoreCase(rarity) ? "#D5330B" : ("R".equalsIgnoreCase(rarity) ? "#B8860B" : ("U".equalsIgnoreCase(rarity) ? "#666666" : "#111111"));
        int badgeSize = Math.max(10, Math.min(12, size - 6));
        return "<b color='" + color + "'><font size='2'>[" + set + "]</font></b>";
    }

    public static Image getSetSymbolImage(String set) {
        return ManaSymbols.getSetSymbolImage(set, Rarity.COMMON);
    }

    public static Image getSetSymbolImage(String set, Rarity rarity) {
        Map<Rarity, Image> rarityImages = setImages.get(set);
        if (rarityImages != null) {
            return rarityImages.get((Object)rarity);
        }
        return null;
    }

    public static BufferedImage getSizedManaSymbol(String symbol) {
        return ManaSymbols.getSizedManaSymbol(symbol, GUISizeHelper.symbolDialogSize);
    }

    public static BufferedImage getSizedManaSymbol(String symbol, int size) {
        if (!manaImages.containsKey(size)) {
            ManaSymbols.loadSymbolImages(size);
        }
        Map<String, BufferedImage> sizedSymbols = manaImages.get(size);
        return sizedSymbols.get(symbol);
    }

    static {
        onlyMythics.add("DRB");
        onlyMythics.add("V09");
        onlyMythics.add("V12");
        onlyMythics.add("V13");
        onlyMythics.add("V14");
        onlyMythics.add("V15");
        onlyMythics.add("V16");
        onlyMythics.add("EXP");
        onlyMythics.add("MPS");
        withoutSymbols.add("MPR");
        withoutSymbols.add("P03");
        withoutSymbols.add("P04");
        withoutSymbols.add("P05");
        withoutSymbols.add("P06");
        withoutSymbols.add("P07");
        withoutSymbols.add("P08");
        withoutSymbols.add("P09");
        withoutSymbols.add("P10");
        withoutSymbols.add("P11");
        setImagesExist = new HashMap<String, Dimension>();
        REPLACE_SYMBOLS_PATTERN = Pattern.compile("\\{([^}/]*)/?([^}/]*)/?([^}/]*)\\}");
        symbols = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "B", "BG", "BR", "BP", "2B", "CB", "G", "GU", "GW", "GP", "2G", "CG", "R", "RG", "RW", "RP", "2R", "CR", "S", "T", "Q", "U", "UB", "UR", "UP", "2U", "CU", "W", "WB", "WU", "WP", "2W", "CW", "X", "C", "E", "P", "H", "TK", "BGP", "BRP", "GUP", "GWP", "RGP", "RWP", "UBP", "URP", "WBP", "WUP"};
        labelRender = new JLabel();
    }

    public static enum Type {
        TABLE,
        CHAT,
        DIALOG,
        TOOLTIP,
        CARD_ICON_HINT;

    }
}
