/*
 * Decompiled with CFR.
 */
package mage.client;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import mage.MageException;
import mage.cards.action.ActionCallback;
import mage.cards.decks.Deck;
import mage.cards.repository.CardRepository;
import mage.cards.repository.CardScanner;
import mage.cards.repository.RepositoryUtil;
import mage.client.MagePane;
import mage.client.MagePaneMenuItem;
import mage.client.SessionHandler;
import mage.client.cards.BigCard;
import mage.client.chat.ChatPanelBasic;
import mage.client.components.MageComponents;
import mage.client.components.MageDesktopManager;
import mage.client.components.MageJDesktop;
import mage.client.components.MageRoundPane;
import mage.client.components.MageUI;
import mage.client.components.ext.dlg.DialogManager;
import mage.client.components.tray.MageTray;
import mage.client.constants.Constants;
import mage.client.deckeditor.DeckEditorPane;
import mage.client.deckeditor.collection.viewer.CollectionViewerPane;
import mage.client.decks.DeckDownloaderPane;
import mage.client.dialog.AboutDialog;
import mage.client.dialog.ConnectDialog;
import mage.client.dialog.ErrorDialog;
import mage.client.dialog.FeedbackDialog;
import mage.client.dialog.GameEndDialog;
import mage.client.dialog.MageDialog;
import mage.client.dialog.PreferencesDialog;
import mage.client.dialog.TableWaitingDialog;
import mage.client.dialog.TestCardRenderDialog;
import mage.client.dialog.TestModalDialog;
import mage.client.dialog.UserRequestDialog;
import mage.client.dialog.WhatsNewDialog;
import mage.client.draft.DraftPane;
import mage.client.draft.DraftPanel;
import mage.client.game.GamePane;
import mage.client.game.GamePanel;
import mage.client.game.PlayAreaPanel;
import mage.client.plugins.adapters.MageActionCallback;
import mage.client.plugins.impl.Plugins;
import mage.client.preference.MagePreferences;
import mage.client.remote.CallbackClientImpl;
import mage.client.table.TablesPane;
import mage.client.tournament.TournamentPane;
import mage.client.util.AppUtil;
import mage.client.util.EDTExceptionHandler;
import mage.client.util.GUISizeHelper;
import mage.client.util.ImageCaches;
import mage.client.util.MacFullscreenUtil;
import mage.client.util.SettingsManager;
import mage.client.util.audio.MusicPlayer;
import mage.client.util.gui.ArrowBuilder;
import mage.client.util.gui.GuiDisplayUtil;
import mage.client.util.gui.countryBox.CountryUtil;
import mage.client.util.sets.ConstructedFormats;
import mage.client.util.stats.UpdateMemUsageTask;
import mage.components.ImagePanel;
import mage.components.ImagePanelStyle;
import mage.constants.PlayerAction;
import mage.interfaces.MageClient;
import mage.interfaces.callback.CallbackClient;
import mage.interfaces.callback.ClientCallback;
import mage.remote.Connection;
import mage.util.DebugUtil;
import mage.util.JavaUtil;
import mage.util.ThreadUtils;
import mage.util.XmageThreadFactory;
import mage.utils.MageVersion;
import mage.view.GameEndView;
import mage.view.UserRequestMessage;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TConfig;
import net.java.truevfs.kernel.spec.FsAccessOption;
import org.apache.log4j.Logger;
import org.mage.card.arcane.ManaSymbols;
import org.mage.card.arcane.SvgUtils;
import org.mage.plugins.card.images.DownloadPicturesService;
import org.mage.plugins.card.info.CardInfoPaneImpl;
import org.mage.plugins.card.utils.CardImageUtils;
import org.mage.plugins.card.utils.impl.ImageManagerImpl;

public class MageFrame
extends JFrame
implements MageClient {
    private static final String TITLE_NAME = "XMage";
    private static final Logger LOGGER = Logger.getLogger(MageFrame.class);
    private static final String LITE_MODE_ARG = "-lite";
    private static final String GRAY_MODE_ARG = "-gray";
    private static final String FULL_SCREEN_PROP = "xmage.fullScreen";
    private static final String GUI_MODAL_MODE_PROP = "xmage.guiModalMode";
    private static final String SKIP_DONE_SYMBOLS = "-skipDoneSymbols";
    private static final String DEBUG_ARG = "-debug";
    private static final String NOT_CONNECTED_TEXT = "<not connected>";
    private static final String NOT_CONNECTED_BUTTON = "CONNECT TO SERVER";
    private static MageFrame instance;
    private final ConnectDialog connectDialog;
    private final ErrorDialog errorDialog;
    private static CallbackClient callbackClient;
    private static Preferences PREFS;
    private final JPanel fakeTopPanel;
    private WhatsNewDialog whatsNewDialog;
    private JLabel title;
    private Rectangle titleRectangle;
    private static final MageVersion VERSION;
    private Connection currentConnection;
    private static MagePane activeFrame;
    private static boolean liteMode;
    private static boolean grayMode;
    private static boolean macOsFullScreenEnabled;
    private static boolean skipSmallSymbolGenerationForExisting;
    private static boolean debugMode;
    private static boolean guiModalModeEnabled;
    private JToggleButton switchPanelsButton = null;
    private static String SWITCH_PANELS_BUTTON_NAME;
    private static final Map<UUID, ChatPanelBasic> CHATS;
    private static final Map<UUID, GamePanel> GAMES;
    private static final Map<UUID, DraftPanel> DRAFTS;
    private static final MageUI UI;
    private static final ScheduledExecutorService PING_SENDER_EXECUTOR;
    private static UpdateMemUsageTask updateMemUsageTask;
    private static long startTime;
    private JButton btnAbout;
    private JButton btnCollectionViewer;
    private JButton btnConnect;
    private JButton btnDebug;
    private JButton btnDeckEditor;
    private JButton btnDownload;
    private JButton btnPreferences;
    private JButton btnSendFeedback;
    private static JDesktopPane desktopPane;
    private JLabel jMemUsageLabel;
    private JToolBar.Separator jSeparator1;
    private JToolBar.Separator jSeparator2;
    private JToolBar.Separator jSeparator4;
    private JToolBar.Separator jSeparator5;
    private JToolBar.Separator jSeparator6;
    private JToolBar.Separator jSeparator7;
    private JToolBar.Separator jSeparatorSymbols;
    private JToolBar mageToolbar;
    private JPopupMenu.Separator menuDebugSeparator;
    private JMenuItem menuDebugTestCardRenderModesDialog;
    private JMenuItem menuDebugTestCustomCode;
    private JMenuItem menuDebugTestModalDialog;
    private JMenuItem menuDownloadImages;
    private JMenuItem menuDownloadSymbols;
    private JPopupMenu popupDebug;
    private JPopupMenu popupDownload;
    private JToolBar.Separator separatorDebug;
    private static final long serialVersionUID = -9104885239063142218L;
    private ImagePanel backgroundPane;
    private final TablesPane tablesPane;

    public static JDesktopPane getDesktop() {
        return desktopPane;
    }

    public static Preferences getPreferences() {
        if (PREFS == null) {
            PREFS = Preferences.userNodeForPackage(MageFrame.class);
        }
        return PREFS;
    }

    public static boolean isLite() {
        return liteMode;
    }

    public static boolean isGray() {
        return grayMode;
    }

    public static boolean isSkipSmallSymbolGenerationForExisting() {
        return skipSmallSymbolGenerationForExisting;
    }

    public static boolean isGuiModalModeEnabled() {
        return guiModalModeEnabled;
    }

    public MageVersion getVersion() {
        return VERSION;
    }

    public static MageFrame getInstance() {
        return instance;
    }

    private void handleEvent(AWTEvent event) {
        MagePane frame = activeFrame;
        Object source = event.getSource();
        if (source instanceof Component) {
            for (Component component = (Component)source; component != null; component = component.getParent()) {
                if (!(component instanceof MagePane)) continue;
                frame = (MagePane)component;
                break;
            }
        }
        if (frame != null) {
            frame.handleEvent(event);
        }
    }

    public MageFrame() throws MageException {
        this.setWindowTitle();
        if (MacFullscreenUtil.isMacOSX() && macOsFullScreenEnabled) {
            MacFullscreenUtil.enableMacOSFullScreenMode(this);
            MacFullscreenUtil.toggleMacOSFullScreenMode(this);
        }
        EDTExceptionHandler.registerExceptionHandler();
        this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent e) {
                MageFrame.this.exitApp();
            }
        });
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> this.handleEvent(event), 24L);
        TConfig config = TConfig.current();
        config.setArchiveDetector(new TArchiveDetector("zip"));
        config.setAccessPreference(FsAccessOption.STORE, true);
        GUISizeHelper.calculateGUISizes();
        GuiDisplayUtil.refreshThemeSettings();
        Object value = UIManager.get("SplitPane.ancestorInputMap");
        if (value instanceof InputMap) {
            InputMap map = (InputMap)value;
            for (int vk = 113; vk <= 123; ++vk) {
                map.remove(KeyStroke.getKeyStroke(vk, 0));
            }
        }
        if (ClientCallback.SIMULATE_BAD_CONNECTION) {
            LOGGER.info((Object)"Network: bad connection mode enabled");
        }
        RepositoryUtil.bootstrapLocalDb();
        if (RepositoryUtil.isDatabaseEmpty()) {
            LOGGER.info((Object)"DB: creating cards database (it can take few minutes)...");
            CardScanner.scan();
            LOGGER.info((Object)"Done.");
        }
        LOGGER.info((Object)"Images: search broken files...");
        CardImageUtils.checkAndFixImageFiles();
        this.bootstrapSetsAndFormats();
        SvgUtils.checkSvgSupport();
        ManaSymbols.loadImages();
        Plugins.instance.loadPlugins();
        if (!Plugins.instance.isCardPluginLoaded()) {
            throw new MageException("can't load card plugin");
        }
        this.initComponents();
        desktopPane.addContainerListener(new ContainerAdapter(){

            @Override
            public void componentAdded(ContainerEvent e) {
                if (desktopPane.getLayer(e.getComponent()) == JLayeredPane.DEFAULT_LAYER.intValue()) {
                    MageFrame.this.updateSwitchPanelsButton();
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (desktopPane.getLayer(e.getComponent()) == JLayeredPane.DEFAULT_LAYER.intValue()) {
                    MageFrame.this.updateSwitchPanelsButton();
                }
            }
        });
        desktopPane.setDesktopManager(new MageDesktopManager());
        this.setSize(1024, 768);
        SettingsManager.instance.setScreenWidthAndHeight(1024, 768);
        DialogManager.updateParams(768, 1024, false);
        this.setExtendedState(6);
        SessionHandler.startSession(this);
        callbackClient = new CallbackClientImpl(this);
        this.connectDialog = new ConnectDialog();
        desktopPane.add((Component)this.connectDialog, this.connectDialog.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        this.errorDialog = new ErrorDialog();
        this.errorDialog.setLocation(100, 100);
        desktopPane.add((Component)this.errorDialog, this.errorDialog.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        try {
            this.whatsNewDialog = new WhatsNewDialog();
        }
        catch (Throwable e) {
            LOGGER.error((Object)"JavaFX is not supported by your system. What's new page will be disabled.", e);
            this.whatsNewDialog = null;
        }
        PING_SENDER_EXECUTOR.scheduleAtFixedRate(SessionHandler::ping, 20L, 20L, TimeUnit.SECONDS);
        updateMemUsageTask = new UpdateMemUsageTask(this.jMemUsageLabel);
        this.tablesPane = new TablesPane();
        desktopPane.add((Component)this.tablesPane, JLayeredPane.DEFAULT_LAYER);
        SwingUtilities.invokeLater(this::hideServerLobby);
        UI.addComponent(MageComponents.DESKTOP_PANE, desktopPane);
        UI.addComponent(MageComponents.DESKTOP_TOOLBAR, this.mageToolbar);
        this.addTooltipContainer();
        this.setBackground();
        this.addMageLabel();
        this.setAppIcon();
        MageTray.instance.install();
        this.fakeTopPanel = new JPanel();
        this.fakeTopPanel.setVisible(true);
        this.fakeTopPanel.setOpaque(false);
        this.fakeTopPanel.setLayout(null);
        desktopPane.add((Component)this.fakeTopPanel, JLayeredPane.DRAG_LAYER);
        desktopPane.add((Component)ArrowBuilder.getBuilder().getArrowsManagerPanel(), JLayeredPane.PALETTE_LAYER);
        desktopPane.addComponentListener(new ComponentAdapter(){

            @Override
            public void componentResized(ComponentEvent e) {
                int width = ((JComponent)e.getSource()).getWidth();
                int height = ((JComponent)e.getSource()).getHeight();
                SettingsManager.instance.setScreenWidthAndHeight(width, height);
                if (!liteMode && !grayMode) {
                    MageFrame.this.backgroundPane.setSize(width, height);
                }
                MageFrame.this.updateCurrentFrameSize();
                ArrowBuilder.getBuilder().setSize(width, height);
                MageFrame.this.fakeTopPanel.setSize(width, height);
                if (MageFrame.this.title != null) {
                    MageFrame.this.title.setBounds((int)((double)width - MageFrame.this.titleRectangle.getWidth()) / 2, (int)((double)height - MageFrame.this.titleRectangle.getHeight()) / 2, ((MageFrame)MageFrame.this).titleRectangle.width, ((MageFrame)MageFrame.this).titleRectangle.height);
                }
            }
        });
        ToolTipManager.sharedInstance().setDismissDelay(60000);
        this.mageToolbar.add((Component)this.createSwitchPanelsButton(), 0);
        this.mageToolbar.add((Component)new JToolBar.Separator(), 1);
        JButton btnDeckLibrary = new JButton("Descargar decks");
        btnDeckLibrary.setToolTipText("Actualizar la biblioteca hist\u00f3rica de mazos");
        btnDeckLibrary.setFocusable(false);
        btnDeckLibrary.addActionListener(event -> DeckDownloaderPane.showPane());
        int deckButtonIndex = this.mageToolbar.getComponentIndex(this.btnDeckEditor);
        this.mageToolbar.add((Component)btnDeckLibrary, deckButtonIndex + 2);
        this.mageToolbar.add((Component)new JToolBar.Separator(), deckButtonIndex + 3);
        if (Plugins.instance.isCounterPluginLoaded()) {
            int i = Plugins.instance.getGamesPlayed();
            JLabel label = new JLabel("  Games played: " + i);
            desktopPane.add((Component)label, JLayeredPane.DEFAULT_LAYER + 1);
            label.setVisible(true);
            label.setForeground(Color.white);
            label.setBounds(0, 0, 180, 30);
        }
        this.setGUISize();
        this.setConnectButtonText(NOT_CONNECTED_BUTTON);
        SwingUtilities.invokeLater(() -> {
            updateMemUsageTask.execute();
            LOGGER.info((Object)("Client start up time: " + (System.currentTimeMillis() - startTime) / 1000L + " seconds"));
            if (Boolean.parseBoolean(MageFrame.getPreferences().get("autoConnect", "false"))) {
                this.startAutoConnect();
            } else {
                this.connectDialog.showDialog(this::setWindowTitle);
            }
            this.setWindowTitle();
        });
        SwingUtilities.invokeLater(() -> this.showWhatsNewDialog(false));
    }

    @Deprecated
    private void initSSLCertificates() {
        boolean cacertsUsed = false;
        File cacertsFile = new File(System.getProperty("user.dir") + "/release/cacerts").getAbsoluteFile();
        if (cacertsFile.exists()) {
            cacertsUsed = true;
            LOGGER.info((Object)"SSL certificates: used runtime cacerts bundle");
        }
        if (!cacertsUsed && (cacertsFile = new File(System.getProperty("user.dir") + "/cacerts").getAbsoluteFile()).exists()) {
            cacertsUsed = true;
            LOGGER.info((Object)"SSL certificates: used release cacerts bundle");
        }
        if (cacertsUsed && cacertsFile.exists()) {
            String cacertsPath = cacertsFile.getPath();
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
            System.setProperty("javax.net.ssl.trustStore", cacertsPath);
            System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        } else {
            LOGGER.info((Object)("SSL certificates: used default cacerts bundle from " + System.getProperty("java.version")));
        }
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
    }

    private void bootstrapSetsAndFormats() {
        LOGGER.info((Object)"Loading sets and formats...");
        ConstructedFormats.ensureLists();
    }

    private void setWindowTitle() {
        this.setTitle("XMage  Client: " + (VERSION == null ? "<not available>" : VERSION.toString()) + "  Server: " + (SessionHandler.getSession() != null && SessionHandler.isConnected() ? SessionHandler.getVersionInfo() : NOT_CONNECTED_TEXT));
    }

    private void updateTooltipContainerSizes() {
        BigCard bigCardRotated;
        JPanel cardPreviewContainerRotated;
        BigCard bigCard;
        JPanel cardPreviewContainer;
        try {
            cardPreviewContainer = (JPanel)UI.getComponent(MageComponents.CARD_PREVIEW_CONTAINER);
            bigCard = (BigCard)UI.getComponent(MageComponents.CARD_PREVIEW_PANE);
            cardPreviewContainerRotated = (JPanel)UI.getComponent(MageComponents.CARD_PREVIEW_CONTAINER_ROTATED);
            bigCardRotated = (BigCard)UI.getComponent(MageComponents.CARD_PREVIEW_PANE_ROTATED);
        }
        catch (InterruptedException e) {
            LOGGER.fatal((Object)"Can't update tooltip panel sizes");
            Thread.currentThread().interrupt();
            return;
        }
        int height = GUISizeHelper.cardTooltipLargeImageHeight;
        int width = (int)((float)height * 0.64f);
        bigCard.setSize(width, height);
        cardPreviewContainer.setBounds(0, 0, width + 80, height + 30);
        bigCardRotated.setSize(height, width + 30);
        cardPreviewContainerRotated.setBounds(0, 0, height + 80, width + 100 + 30);
    }

    private void addTooltipContainer() {
        JEditorPane cardInfoPane = (JEditorPane)Plugins.instance.getCardInfoPane();
        if (cardInfoPane == null) {
            LOGGER.fatal((Object)"Can't find card tooltip plugin");
            return;
        }
        cardInfoPane.setLocation(40, 40);
        UI.addComponent(MageComponents.CARD_INFO_PANE, cardInfoPane);
        MageRoundPane popupContainer = new MageRoundPane();
        popupContainer.setLayout(null);
        popupContainer.add(cardInfoPane);
        popupContainer.setVisible(false);
        if (DebugUtil.GUI_POPUP_CONTAINER_DRAW_DEBUG_BORDER) {
            popupContainer.setBorder(BorderFactory.createLineBorder(Color.red));
        }
        desktopPane.add((Component)popupContainer, JLayeredPane.POPUP_LAYER);
        UI.addComponent(MageComponents.POPUP_CONTAINER, popupContainer);
        JPanel cardPreviewContainer = new JPanel();
        cardPreviewContainer.setOpaque(false);
        cardPreviewContainer.setLayout(null);
        cardPreviewContainer.setVisible(false);
        desktopPane.add((Component)cardPreviewContainer, JLayeredPane.POPUP_LAYER);
        UI.addComponent(MageComponents.CARD_PREVIEW_CONTAINER, cardPreviewContainer);
        BigCard bigCard = new BigCard();
        bigCard.setLocation(40, 40);
        bigCard.setBackground(new Color(0, 0, 0, 0));
        cardPreviewContainer.add(bigCard);
        UI.addComponent(MageComponents.CARD_PREVIEW_PANE, bigCard);
        JPanel cardPreviewContainerRotated = new JPanel();
        cardPreviewContainerRotated.setOpaque(false);
        cardPreviewContainerRotated.setLayout(null);
        cardPreviewContainerRotated.setVisible(false);
        desktopPane.add((Component)cardPreviewContainerRotated, JLayeredPane.POPUP_LAYER);
        UI.addComponent(MageComponents.CARD_PREVIEW_CONTAINER_ROTATED, cardPreviewContainerRotated);
        BigCard bigCardRotated = new BigCard(true);
        bigCardRotated.setLocation(40, 40);
        bigCardRotated.setBackground(new Color(0, 0, 0, 0));
        cardPreviewContainerRotated.add(bigCardRotated);
        UI.addComponent(MageComponents.CARD_PREVIEW_PANE_ROTATED, bigCardRotated);
        this.updateTooltipContainerSizes();
    }

    private void setGUISizeTooltipContainer() {
        try {
            int height = GUISizeHelper.cardTooltipLargeImageHeight;
            int width = (int)((float)height * 0.64f);
            JPanel cardPreviewContainer = (JPanel)UI.getComponent(MageComponents.CARD_PREVIEW_CONTAINER);
            cardPreviewContainer.setBounds(0, 0, width + 80, height + 30);
            BigCard bigCard = (BigCard)UI.getComponent(MageComponents.CARD_PREVIEW_PANE);
            bigCard.setSize(width, height);
            JPanel cardPreviewContainerRotated = (JPanel)UI.getComponent(MageComponents.CARD_PREVIEW_CONTAINER_ROTATED);
            cardPreviewContainerRotated.setBounds(0, 0, height + 80, width + 100 + 30);
            BigCard bigCardRotated = (BigCard)UI.getComponent(MageComponents.CARD_PREVIEW_PANE_ROTATED);
            bigCardRotated.setSize(height, width + 30);
        }
        catch (Exception e) {
            LOGGER.warn((Object)"Error while changing tooltip container size.", (Throwable)e);
        }
    }

    private void setBackground() {
        if (liteMode || grayMode) {
            return;
        }
        try {
            if (Plugins.instance.isThemePluginLoaded() && !PreferencesDialog.getCachedValue("backgroundImagedDefault", "true").equals("true")) {
                this.backgroundPane = (ImagePanel)Plugins.instance.updateTablePanel(new HashMap<String, JComponent>());
            } else {
                InputStream is = this.getClass().getResourceAsStream(PreferencesDialog.getCurrentTheme().getLoginBackgroundPath());
                BufferedImage background = ImageIO.read(is);
                this.backgroundPane = new ImagePanel(background, ImagePanelStyle.SCALED);
            }
            this.backgroundPane.setSize(1024, 768);
            desktopPane.add((Component)this.backgroundPane, JLayeredPane.DEFAULT_LAYER);
        }
        catch (IOException e) {
            LOGGER.fatal((Object)"Error while setting background.", (Throwable)e);
        }
    }

    public static boolean isChristmasTime(Date currentTime) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(currentTime);
        int currentYear = cal.get(1);
        if (cal.get(2) == 0) {
            --currentYear;
        }
        Date chrisFrom = new GregorianCalendar(currentYear, 11, 15).getTime();
        Date chrisTo = new GregorianCalendar(currentYear + 1, 0, 16).getTime();
        return (currentTime.equals(chrisFrom) || currentTime.after(chrisFrom)) && currentTime.before(chrisTo);
    }

    private void addMageLabel() {
        float ratio;
        String filename;
        if (liteMode || grayMode) {
            return;
        }
        if (MageFrame.isChristmasTime(Calendar.getInstance().getTime())) {
            LOGGER.info((Object)"Ho Ho Ho, Merry Christmas and a Happy New Year");
            filename = "/label-xmage-christmas.png";
            ratio = 1.6949686f;
        } else {
            filename = "/label-xmage.png";
            ratio = 1.7673612f;
        }
        try {
            InputStream is = this.getClass().getResourceAsStream(filename);
            if (is != null) {
                this.titleRectangle = new Rectangle(540, (int)(640.0f / ratio));
                BufferedImage image = ImageIO.read(is);
                this.title = new JLabel();
                this.title.setIcon(new ImageIcon(image));
                this.backgroundPane.setLayout(null);
                this.backgroundPane.add((JComponent)this.title);
            }
        }
        catch (IOException e) {
            LOGGER.fatal((Object)"Error while adding mage label.", (Throwable)e);
        }
    }

    private void setAppIcon() {
        Image image = ImageManagerImpl.instance.getAppImage();
        this.setIconImage(image);
    }

    private AbstractButton createSwitchPanelsButton() {
        this.switchPanelsButton = new JToggleButton(SWITCH_PANELS_BUTTON_NAME);
        this.switchPanelsButton.addItemListener(e -> {
            if (e.getStateChange() == 1) {
                this.createAndShowSwitchPanelsMenu((JComponent)e.getSource(), this.switchPanelsButton);
            }
        });
        this.switchPanelsButton.setFocusable(false);
        this.switchPanelsButton.setHorizontalTextPosition(10);
        return this.switchPanelsButton;
    }

    private void updateSwitchPanelsButton() {
        if (this.switchPanelsButton != null) {
            int totalCount = this.getPanelsCount(false);
            int activeCount = this.getPanelsCount(true);
            this.switchPanelsButton.setText(SWITCH_PANELS_BUTTON_NAME + String.format(" (%d)", totalCount));
            this.switchPanelsButton.setToolTipText(String.format("Click to switch between panels (active panels: %d of %d)", activeCount, totalCount));
        }
    }

    private void createAndShowSwitchPanelsMenu(JComponent component, final AbstractButton windowButton) {
        JPopupMenu menu = new JPopupMenu();
        Component[] windows = desktopPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER);
        List panels = Arrays.stream(windows).filter(Component::isVisible).filter(p -> p instanceof MagePane).map(p -> (MagePane)p).collect(Collectors.toList());
        MagePane activePanel = panels.stream().findFirst().orElse(null);
        panels.sort((p1, p2) -> {
            int ng2;
            int ng1 = p1.getSortTableId() == null ? 0 : 1;
            int n = ng2 = p2.getSortTableId() == null ? 0 : 1;
            if (ng1 != ng2) {
                return Integer.compare(ng1, ng2);
            }
            if (p1.getSortTableId() != null && !p1.getSortTableId().equals(p2.getSortTableId())) {
                return p1.getSortTableId().compareTo(p2.getSortTableId());
            }
            return Integer.compare(p1.getSortOrder(), p2.getSortOrder());
        });
        UUID lastTableId = null;
        for (MagePane panel : panels) {
            if (!Objects.equals(panel.getSortTableId(), lastTableId)) {
                lastTableId = panel.getSortTableId();
                if (menu.getComponentCount() > 0) {
                    menu.addSeparator();
                }
            }
            MagePaneMenuItem menuItem = new MagePaneMenuItem(panel);
            if (activePanel == panel) {
                menuItem.setState(true);
            }
            menuItem.setFont(GUISizeHelper.dialogFont);
            menuItem.addActionListener(ae -> {
                MagePane frame = ((MagePaneMenuItem)ae.getSource()).getFrame();
                MageFrame.setActive(frame);
            });
            menu.add(menuItem);
        }
        menu.addPopupMenuListener(new PopupMenuListener(){

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                windowButton.setSelected(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                windowButton.setSelected(false);
            }
        });
        menu.show(component, 0, component.getHeight());
    }

    public static boolean isGameActive() {
        return activeFrame instanceof GamePane;
    }

    public static void setActive(MagePane frame) {
        try {
            Component container;
            ActionCallback callback = Plugins.instance.getActionCallback();
            if (callback instanceof MageActionCallback) {
                ((MageActionCallback)callback).hideEnlargedCard();
            }
            if ((container = MageFrame.getUI().getComponent(MageComponents.POPUP_CONTAINER)).isVisible()) {
                container.setVisible(false);
                container.repaint();
            }
        }
        catch (InterruptedException e) {
            LOGGER.fatal((Object)"MageFrame error", (Throwable)e);
            Thread.currentThread().interrupt();
        }
        if (activeFrame == frame) {
            return;
        }
        if (activeFrame != null) {
            activeFrame.deactivated();
        }
        activeFrame = null;
        ArrowBuilder.getBuilder().hideAllPanels();
        MusicPlayer.stopBGM();
        if (frame == null) {
            return;
        }
        activeFrame = frame;
        desktopPane.moveToFront(activeFrame);
        activeFrame.setBounds(0, 0, desktopPane.getWidth(), desktopPane.getHeight());
        activeFrame.revalidate();
        activeFrame.activated();
        activeFrame.setVisible(true);
        if (activeFrame instanceof GamePane) {
            ArrowBuilder.getBuilder().showPanel(((GamePane)activeFrame).getGameId());
            MusicPlayer.playBGM();
        }
    }

    private void updateCurrentFrameSize() {
        if (activeFrame != null) {
            activeFrame.setBounds(0, 0, desktopPane.getWidth(), desktopPane.getHeight());
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        this.updateCurrentFrameSize();
    }

    public static void deactivate(MagePane frame) {
        frame.setVisible(false);
        MagePane topPane = MageFrame.getTopMost(frame);
        if (topPane == frame) {
            throw new IllegalArgumentException("Impossible use case - deactivated frame can't ref to itself");
        }
        MageFrame.setActive(topPane);
    }

    public static MagePane getTopMost(MagePane exclude) {
        MagePane topmost = null;
        int best = Integer.MAX_VALUE;
        for (Component frame : desktopPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)) {
            int z;
            if (!frame.isVisible() || (z = desktopPane.getComponentZOrder(frame)) >= best || !(frame instanceof MagePane) || !SessionHandler.isConnected() && frame instanceof TablesPane) continue;
            best = z;
            if (frame.equals(exclude)) continue;
            topmost = (MagePane)frame;
        }
        return topmost;
    }

    public void showGame(UUID currentTableId, UUID parentTableId, UUID gameId, UUID playerId) {
        GamePane gamePane = new GamePane();
        desktopPane.add((Component)gamePane, JLayeredPane.DEFAULT_LAYER);
        gamePane.setVisible(true);
        gamePane.showGame(currentTableId, parentTableId, gameId, playerId);
        MageFrame.setActive(gamePane);
    }

    public void watchGame(UUID currentTableId, UUID parentTableId, UUID gameId) {
        for (Component component : desktopPane.getComponents()) {
            if (!(component instanceof GamePane) || !((GamePane)component).getGameId().equals(gameId)) continue;
            MageFrame.setActive((GamePane)component);
            return;
        }
        GamePane gamePane = new GamePane();
        desktopPane.add((Component)gamePane, JLayeredPane.DEFAULT_LAYER);
        gamePane.setVisible(true);
        gamePane.watchGame(currentTableId, parentTableId, gameId);
        MageFrame.setActive(gamePane);
    }

    public void replayGame(UUID gameId) {
        GamePane gamePane = new GamePane();
        desktopPane.add((Component)gamePane, JLayeredPane.DEFAULT_LAYER);
        gamePane.setVisible(true);
        gamePane.replayGame(gameId);
        MageFrame.setActive(gamePane);
    }

    public void showDraft(UUID tableId, UUID draftId) {
        DraftPane draftPane = new DraftPane();
        desktopPane.add((Component)draftPane, JLayeredPane.DEFAULT_LAYER);
        draftPane.setVisible(true);
        draftPane.showDraft(tableId, draftId);
        MageFrame.setActive(draftPane);
    }

    public void endDraft(UUID draftId) {
        for (Component window : desktopPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)) {
            if (!(window instanceof DraftPane)) continue;
            DraftPane draftPane = (DraftPane)window;
            draftPane.removeDraft();
        }
    }

    public void showTournament(UUID tableId, UUID tournamentId) {
        TournamentPane tournamentPane = null;
        for (Component component : desktopPane.getComponents()) {
            if (!(component instanceof TournamentPane) || !((TournamentPane)component).getTournamentId().equals(tournamentId)) continue;
            tournamentPane = (TournamentPane)component;
        }
        if (tournamentPane == null) {
            tournamentPane = new TournamentPane();
            desktopPane.add((Component)tournamentPane, JLayeredPane.DEFAULT_LAYER);
            tournamentPane.setVisible(true);
            tournamentPane.showTournament(tableId, tournamentId);
        }
        MageFrame.setActive(tournamentPane);
    }

    public void showGameEndDialog(GameEndView gameEndView) {
        GameEndDialog gameEndDialog;
        desktopPane.add((Component)gameEndDialog, (gameEndDialog = new GameEndDialog(gameEndView)).isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        gameEndDialog.showDialog();
    }

    public void showTableWaitingDialog(UUID roomId, UUID tableId, boolean isTournament) {
        TableWaitingDialog tableWaitingDialog;
        desktopPane.add((Component)tableWaitingDialog, (tableWaitingDialog = new TableWaitingDialog()).isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        tableWaitingDialog.showDialog(roomId, tableId, isTournament);
    }

    public static boolean connect(Connection connection) {
        boolean result = SessionHandler.connect(connection);
        MageFrame.getInstance().setWindowTitle();
        return result;
    }

    public static boolean stopConnecting() {
        return SessionHandler.stopConnecting();
    }

    public void startAutoConnect() {
        LOGGER.info((Object)("Auto-connecting to " + MagePreferences.getServerAddress()));
        this.setConnectButtonText("AUTO-CONNECT to " + MagePreferences.getLastServerAddress());
        SwingUtilities.invokeLater(() -> {
            boolean isConnected = false;
            try {
                isConnected = this.performConnect(false);
            }
            finally {
                if (!isConnected) {
                    this.setConnectButtonText(NOT_CONNECTED_BUTTON);
                }
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean performConnect(boolean reconnect) {
        if (this.currentConnection == null || !reconnect) {
            String server = MagePreferences.getLastServerAddress();
            int port = MagePreferences.getLastServerPort();
            String userName = MagePreferences.getLastServerUser();
            String password = MagePreferences.getLastServerPassword();
            String proxyServer = MageFrame.getPreferences().get("proxyAddress", "");
            int proxyPort = Integer.parseInt(MageFrame.getPreferences().get("proxyPort", "0"));
            Connection.ProxyType proxyType = Connection.ProxyType.valueByText((String)MageFrame.getPreferences().get("proxyType", "None"));
            String proxyUsername = MageFrame.getPreferences().get("proxyUsername", "");
            String proxyPassword = MageFrame.getPreferences().get("proxyPassword", "");
            this.currentConnection = new Connection();
            this.currentConnection.setUsername(userName);
            this.currentConnection.setPassword(password);
            this.currentConnection.setHost(server);
            this.currentConnection.setPort(port);
            String allMAC = "";
            try {
                allMAC = Connection.getMAC();
            }
            catch (SocketException socketException) {
                // empty catch block
            }
            this.currentConnection.setUserIdStr(System.getProperty("user.name") + ":" + System.getProperty("os.name") + ":" + MagePreferences.getUserNames() + ":" + allMAC);
            this.currentConnection.setProxyType(Connection.ProxyType.NONE);
            this.setUserPrefsToConnection(this.currentConnection);
        }
        this.setCursor(new Cursor(3));
        try {
            LOGGER.debug((Object)("connecting (auto): " + this.currentConnection.getProxyType().toString() + ' ' + this.currentConnection.getProxyHost() + ' ' + this.currentConnection.getProxyPort() + ' ' + this.currentConnection.getProxyUsername()));
            if (MageFrame.connect(this.currentConnection)) {
                this.prepareAndShowServerLobby();
                boolean bl = true;
                return bl;
            }
            this.showMessage("Unable connect to server: " + SessionHandler.getLastConnectError());
        }
        finally {
            this.setCursor(new Cursor(0));
        }
        return false;
    }

    public void setUserPrefsToConnection(Connection connection) {
        connection.setUserData(PreferencesDialog.getUserData());
    }

    private void initComponents() {
        this.popupDebug = new JPopupMenu();
        this.menuDebugTestModalDialog = new JMenuItem();
        this.menuDebugTestCardRenderModesDialog = new JMenuItem();
        this.menuDebugSeparator = new JPopupMenu.Separator();
        this.menuDebugTestCustomCode = new JMenuItem();
        this.popupDownload = new JPopupMenu();
        this.menuDownloadSymbols = new JMenuItem();
        this.menuDownloadImages = new JMenuItem();
        desktopPane = new MageJDesktop();
        this.mageToolbar = new JToolBar();
        this.btnPreferences = new JButton();
        this.jSeparator4 = new JToolBar.Separator();
        this.btnConnect = new JButton();
        this.jSeparator1 = new JToolBar.Separator();
        this.btnDeckEditor = new JButton();
        this.jSeparator2 = new JToolBar.Separator();
        this.btnCollectionViewer = new JButton();
        this.jSeparator5 = new JToolBar.Separator();
        this.btnSendFeedback = new JButton();
        this.jSeparator6 = new JToolBar.Separator();
        this.btnDownload = new JButton();
        this.jSeparatorSymbols = new JToolBar.Separator();
        this.btnAbout = new JButton();
        this.jSeparator7 = new JToolBar.Separator();
        this.btnDebug = new JButton();
        this.separatorDebug = new JToolBar.Separator();
        this.jMemUsageLabel = new JLabel();
        this.menuDebugTestModalDialog.setText("Test Modal Dialogs");
        this.menuDebugTestModalDialog.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.menuDebugTestModalDialogActionPerformed(evt);
            }
        });
        this.popupDebug.add(this.menuDebugTestModalDialog);
        this.menuDebugTestCardRenderModesDialog.setText("Test Card Render Modes");
        this.menuDebugTestCardRenderModesDialog.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.menuDebugTestCardRenderModesDialogActionPerformed(evt);
            }
        });
        this.popupDebug.add(this.menuDebugTestCardRenderModesDialog);
        this.popupDebug.add(this.menuDebugSeparator);
        this.menuDebugTestCustomCode.setText("Run custom code");
        this.menuDebugTestCustomCode.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.menuDebugTestCustomCodeActionPerformed(evt);
            }
        });
        this.popupDebug.add(this.menuDebugTestCustomCode);
        this.menuDownloadSymbols.setText("Download mana symbols");
        this.menuDownloadSymbols.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.menuDownloadSymbolsActionPerformed(evt);
            }
        });
        this.popupDownload.add(this.menuDownloadSymbols);
        this.menuDownloadImages.setText("Download card images");
        this.menuDownloadImages.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.menuDownloadImagesActionPerformed(evt);
            }
        });
        this.popupDownload.add(this.menuDownloadImages);
        this.setDefaultCloseOperation(0);
        this.setMinimumSize(new Dimension(1000, 500));
        desktopPane.setBackground(new Color(204, 204, 204));
        this.mageToolbar.setFloatable(false);
        this.mageToolbar.setRollover(true);
        this.mageToolbar.setFont(new Font("Segoe UI", 0, 48));
        this.mageToolbar.setMaximumSize(new Dimension(614, 60));
        this.mageToolbar.setMinimumSize(new Dimension(566, 60));
        this.mageToolbar.setPreferredSize(new Dimension(614, 60));
        this.btnPreferences.setIcon(new ImageIcon(this.getClass().getResource("/menu/preferences.png")));
        this.btnPreferences.setText("Preferences");
        this.btnPreferences.setToolTipText("By changing the settings in the preferences window you can adjust the look and behaviour of xmage.");
        this.btnPreferences.setFocusable(false);
        this.btnPreferences.setHorizontalTextPosition(4);
        this.btnPreferences.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.btnPreferencesActionPerformed(evt);
            }
        });
        this.mageToolbar.add(this.btnPreferences);
        this.mageToolbar.add(this.jSeparator4);
        this.btnConnect.setIcon(new ImageIcon(this.getClass().getResource("/menu/connect.png")));
        this.btnConnect.setToolTipText("Connect to or disconnect from a XMage server.");
        this.btnConnect.setFocusable(false);
        this.btnConnect.setHorizontalTextPosition(4);
        this.btnConnect.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.btnConnectActionPerformed(evt);
            }
        });
        this.mageToolbar.add(this.btnConnect);
        this.mageToolbar.add(this.jSeparator1);
        this.btnDeckEditor.setIcon(new ImageIcon(this.getClass().getResource("/menu/deck_editor.png")));
        this.btnDeckEditor.setText("Deck Editor");
        this.btnDeckEditor.setToolTipText("Start the deck editor to create or modify decks.");
        this.btnDeckEditor.setFocusable(false);
        this.btnDeckEditor.setHorizontalTextPosition(4);
        this.btnDeckEditor.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.btnDeckEditorActionPerformed(evt);
            }
        });
        this.mageToolbar.add(this.btnDeckEditor);
        this.mageToolbar.add(this.jSeparator2);
        this.btnCollectionViewer.setIcon(new ImageIcon(this.getClass().getResource("/menu/collection.png")));
        this.btnCollectionViewer.setText("Card Viewer");
        this.btnCollectionViewer.setToolTipText("Card viewer to show the cards of sets. ");
        this.btnCollectionViewer.setFocusable(false);
        this.btnCollectionViewer.setHorizontalTextPosition(4);
        this.btnCollectionViewer.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.btnCollectionViewerActionPerformed(evt);
            }
        });
        this.mageToolbar.add(this.btnCollectionViewer);
        this.mageToolbar.add(this.jSeparator5);
        this.btnSendFeedback.setIcon(new ImageIcon(this.getClass().getResource("/menu/feedback.png")));
        this.btnSendFeedback.setText("Feedback");
        this.btnSendFeedback.setToolTipText("Send some feedback to the developers.");
        this.btnSendFeedback.setFocusable(false);
        this.btnSendFeedback.setHorizontalTextPosition(4);
        this.btnSendFeedback.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.btnSendFeedbackActionPerformed(evt);
            }
        });
        this.mageToolbar.add(this.btnSendFeedback);
        this.mageToolbar.add(this.jSeparator6);
        this.btnDownload.setIcon(new ImageIcon(this.getClass().getResource("/menu/images.png")));
        this.btnDownload.setText("Download");
        this.btnDownload.setToolTipText("Download cards images and mana symbols");
        this.btnDownload.setFocusable(false);
        this.btnDownload.setHorizontalTextPosition(4);
        this.btnDownload.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent evt) {
                MageFrame.this.btnDownloadMouseClicked(evt);
            }
        });
        this.mageToolbar.add(this.btnDownload);
        this.mageToolbar.add(this.jSeparatorSymbols);
        this.btnAbout.setIcon(new ImageIcon(this.getClass().getResource("/menu/about.png")));
        this.btnAbout.setText("About");
        this.btnAbout.setToolTipText("About app");
        this.btnAbout.setFocusable(false);
        this.btnAbout.setHorizontalTextPosition(4);
        this.btnAbout.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                MageFrame.this.btnAboutActionPerformed(evt);
            }
        });
        this.mageToolbar.add(this.btnAbout);
        this.mageToolbar.add(this.jSeparator7);
        this.btnDebug.setIcon(new ImageIcon(this.getClass().getResource("/menu/connect.png")));
        this.btnDebug.setText("Debug");
        this.btnDebug.setToolTipText("Show debug tools");
        this.btnDebug.setFocusable(false);
        this.btnDebug.setVerticalTextPosition(3);
        this.btnDebug.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent evt) {
                MageFrame.this.btnDebugMouseClicked(evt);
            }
        });
        this.mageToolbar.add(this.btnDebug);
        this.mageToolbar.add(this.separatorDebug);
        this.jMemUsageLabel.setHorizontalAlignment(2);
        this.jMemUsageLabel.setIcon(new ImageIcon(this.getClass().getResource("/menu/memory.png")));
        this.jMemUsageLabel.setText("100% Free mem");
        this.jMemUsageLabel.setFocusable(false);
        this.jMemUsageLabel.setHorizontalTextPosition(4);
        this.mageToolbar.add(this.jMemUsageLabel);
        GroupLayout layout = new GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(desktopPane, -1, 838, Short.MAX_VALUE).addComponent(this.mageToolbar, -1, -1, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(this.mageToolbar, -2, -1, -2).addGap(2, 2, 2).addComponent(desktopPane, -1, 145, Short.MAX_VALUE)));
        this.pack();
    }

    private void btnDeckEditorActionPerformed(ActionEvent evt) {
        this.showDeckEditor(Constants.DeckEditorMode.FREE_BUILDING, null, null, null, 0);
    }

    private void btnConnectActionPerformed(ActionEvent evt) {
        if (SessionHandler.isConnected()) {
            this.tryDisconnectOrExit(false);
        } else {
            this.connectDialog.showDialog(this::setWindowTitle);
        }
    }

    public void btnAboutActionPerformed(ActionEvent evt) {
        AboutDialog aboutDialog;
        JInternalFrame[] windows;
        for (JInternalFrame window : windows = desktopPane.getAllFrames()) {
            if (!(window instanceof AboutDialog)) continue;
            return;
        }
        desktopPane.add((Component)aboutDialog, (aboutDialog = new AboutDialog()).isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        aboutDialog.showDialog(VERSION);
    }

    private void btnCollectionViewerActionPerformed(ActionEvent evt) {
        this.showCollectionViewer();
    }

    public void btnPreferencesActionPerformed(ActionEvent evt) {
        PreferencesDialog.main(new String[0]);
    }

    public void btnSendFeedbackActionPerformed(ActionEvent evt) {
        if (!SessionHandler.isConnected()) {
            JOptionPane.showMessageDialog(null, "You may send us feedback only when connected to server.", "Information", 1);
            return;
        }
        FeedbackDialog.main(new String[0]);
    }

    public void downloadAdditionalResources() {
        UserRequestMessage message = new UserRequestMessage("Download additional resources", "Do you want to download game symbols and additional image files?");
        message.setButton1("No", null);
        message.setButton2("Yes", PlayerAction.CLIENT_DOWNLOAD_SYMBOLS);
        this.showUserRequestDialog(message);
    }

    private void menuDebugTestModalDialogActionPerformed(ActionEvent evt) {
        TestModalDialog dialog = new TestModalDialog();
        dialog.showDialog();
    }

    private void btnDebugMouseClicked(MouseEvent evt) {
        if (!SwingUtilities.isLeftMouseButton(evt)) {
            return;
        }
        this.popupDebug.show(evt.getComponent(), 0, evt.getComponent().getHeight());
    }

    private void menuDebugTestCardRenderModesDialogActionPerformed(ActionEvent evt) {
        TestCardRenderDialog dialog = new TestCardRenderDialog();
        dialog.showDialog();
    }

    private void btnDownloadMouseClicked(MouseEvent evt) {
        if (!SwingUtilities.isLeftMouseButton(evt)) {
            return;
        }
        this.popupDownload.show(evt.getComponent(), 0, evt.getComponent().getHeight());
    }

    private void menuDownloadSymbolsActionPerformed(ActionEvent evt) {
        this.downloadAdditionalResources();
    }

    private void menuDownloadImagesActionPerformed(ActionEvent evt) {
        this.downloadImages();
    }

    private void menuDebugTestCustomCodeActionPerformed(ActionEvent evt) {
        LOGGER.info((Object)"debug: insert custom code here or set breakpoint");
    }

    public void downloadImages() {
        DownloadPicturesService.startDownload();
    }

    public void exitApp() {
        this.tryDisconnectOrExit(true);
    }

    private void tryDisconnectOrExit(Boolean needExit) {
        String actionName = needExit != false ? "exit" : "disconnect";
        PlayerAction actionFull = needExit != false ? PlayerAction.CLIENT_EXIT_FULL : PlayerAction.CLIENT_DISCONNECT_FULL;
        PlayerAction actionKeepTables = needExit != false ? PlayerAction.CLIENT_EXIT_KEEP_GAMES : PlayerAction.CLIENT_DISCONNECT_KEEP_GAMES;
        double windowSizeRatio = 1.3;
        if (SessionHandler.isConnected()) {
            int activeTables = MageFrame.getInstance().getPanelsCount(true);
            UserRequestMessage message = new UserRequestMessage("Confirm " + actionName, "You are connected and has " + activeTables + " active table(s). You can quit from all your tables (concede) or ask server to wait a few minutes for reconnect. What to do?");
            String totalInfo = activeTables == 0 ? "" : String.format(" from %d table%s", activeTables, activeTables > 1 ? "s" : "");
            message.setButton1("Cancel", null);
            message.setButton2("Wait for me", actionKeepTables);
            message.setButton3("Quit" + totalInfo, actionFull);
            message.setWindowSizeRatio(windowSizeRatio);
            MageFrame.getInstance().showUserRequestDialog(message);
        } else {
            UserRequestMessage message = new UserRequestMessage("Confirm " + actionName, "Are you sure you want to " + actionName + "?");
            message.setButton1("Cancel", null);
            message.setButton2("Yes", actionFull);
            message.setWindowSizeRatio(windowSizeRatio);
            MageFrame.getInstance().showUserRequestDialog(message);
        }
    }

    public void hideServerLobby() {
        this.tablesPane.hideTables();
        this.updateSwitchPanelsButton();
    }

    public void setServerLobbyTablesFilter() {
        if (this.tablesPane != null) {
            this.tablesPane.setTableFilter();
        }
    }

    public void prepareAndShowServerLobby() {
        this.tablesPane.showTables();
        MagePane topPanebefore = MageFrame.getTopMost(this.tablesPane);
        MageFrame.setActive(this.tablesPane);
        if (topPanebefore != null && topPanebefore != this.tablesPane) {
            MageFrame.setActive(topPanebefore);
        }
        this.updateSwitchPanelsButton();
    }

    public void hideGames() {
        Component[] windows;
        for (Component window : windows = desktopPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)) {
            DeckEditorPane deckEditorPane;
            if (window instanceof GamePane) {
                GamePane gamePane = (GamePane)window;
                gamePane.removeGame();
            }
            if (window instanceof DraftPane) {
                DraftPane draftPane = (DraftPane)window;
                draftPane.removeDraft();
            }
            if (window instanceof TournamentPane) {
                TournamentPane tournamentPane = (TournamentPane)window;
                tournamentPane.removeTournament();
            }
            if (!(window instanceof DeckEditorPane) || (deckEditorPane = (DeckEditorPane)window).getDeckEditorMode() != Constants.DeckEditorMode.LIMITED_BUILDING && deckEditorPane.getDeckEditorMode() != Constants.DeckEditorMode.SIDEBOARDING && deckEditorPane.getDeckEditorMode() != Constants.DeckEditorMode.LIMITED_SIDEBOARD_BUILDING && deckEditorPane.getDeckEditorMode() != Constants.DeckEditorMode.VIEW_LIMITED_DECK) continue;
            deckEditorPane.removeFrame();
        }
    }

    private String prepareDeckEditorName(Constants.DeckEditorMode mode, Deck deck, UUID tableId) {
        String name;
        switch (mode) {
            case FREE_BUILDING: {
                name = "Deck Editor";
                break;
            }
            case LIMITED_BUILDING:
            case LIMITED_SIDEBOARD_BUILDING:
            case SIDEBOARDING:
            case VIEW_LIMITED_DECK: {
                name = "Deck Editor - " + mode.getTitle();
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown deck editor mode: " + (Object)((Object)mode));
            }
        }
        if (deck != null && deck.getName() != null && !deck.getName().isEmpty()) {
            name = name + " - " + deck.getName();
        }
        if (tableId != null) {
            name = name + " - table " + tableId;
        }
        return name;
    }

    public void showDeckEditor(Constants.DeckEditorMode mode, Deck deck, UUID currentTableId, UUID parentTableId, int visibleTimer) {
        Component[] windows;
        String name = this.prepareDeckEditorName(mode, deck, currentTableId);
        for (Component window : windows = desktopPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)) {
            if (!(window instanceof DeckEditorPane) || !((MagePane)window).getTitle().equals(name)) continue;
            MageFrame.setActive((MagePane)window);
            return;
        }
        DeckEditorPane deckEditor = new DeckEditorPane();
        desktopPane.add((Component)deckEditor, JLayeredPane.DEFAULT_LAYER);
        deckEditor.setVisible(false);
        deckEditor.show(mode, deck, name, currentTableId, parentTableId, visibleTimer);
        MageFrame.setActive(deckEditor);
    }

    public void showUserRequestDialog(UserRequestMessage userRequestMessage) {
        if (SwingUtilities.isEventDispatchThread()) {
            this.innerShowUserRequestDialog(userRequestMessage);
        } else {
            SwingUtilities.invokeLater(() -> this.innerShowUserRequestDialog(userRequestMessage));
        }
    }

    private void innerShowUserRequestDialog(UserRequestMessage userRequestMessage) {
        UserRequestDialog userRequestDialog = new UserRequestDialog();
        userRequestDialog.setLocation(100, 100);
        desktopPane.add((Component)userRequestDialog, userRequestDialog.isModal() ? JLayeredPane.MODAL_LAYER : JLayeredPane.PALETTE_LAYER);
        userRequestDialog.showDialog(userRequestMessage);
    }

    public void showErrorDialog(String errorType, Throwable e) {
        String errorMessage = e.getMessage();
        if (errorMessage == null || errorMessage.isEmpty() || errorMessage.equals("Null")) {
            errorMessage = e.getClass().getSimpleName() + " - look at server or client logs for more details";
        }
        int maxLines = 10;
        String newLine = "\n";
        String mainError = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).limit(maxLines).collect(Collectors.joining(newLine));
        if (e.getStackTrace().length > maxLines) {
            mainError = mainError + newLine + "and other " + (e.getStackTrace().length - maxLines) + " lines";
        }
        String rootError = "";
        Throwable root = ThreadUtils.findRootException(e);
        if (root != e) {
            rootError = Arrays.stream(root.getStackTrace()).map(StackTraceElement::toString).limit(maxLines).collect(Collectors.joining(newLine));
            if (root.getStackTrace().length > maxLines) {
                rootError = rootError + newLine + "and other " + (root.getStackTrace().length - maxLines) + " lines";
            }
        }
        String allErrors = mainError;
        if (!rootError.isEmpty()) {
            allErrors = allErrors + newLine + "Root caused by:" + newLine + rootError;
        }
        this.showErrorDialog(errorType, e.getClass().getSimpleName(), errorMessage + newLine + newLine + "Stack trace:" + newLine + allErrors);
    }

    public void showErrorDialog(String errorType, String errorTitle, String errorText) {
        if (SwingUtilities.isEventDispatchThread()) {
            this.errorDialog.showDialog(errorType, errorTitle, errorText);
        } else {
            SwingUtilities.invokeLater(() -> this.errorDialog.showDialog(errorType, errorTitle, errorText));
        }
    }

    public void showCollectionViewer() {
        Component[] windows;
        for (Component window : windows = desktopPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)) {
            if (!(window instanceof CollectionViewerPane)) continue;
            MageFrame.setActive((MagePane)window);
            return;
        }
        CollectionViewerPane collectionViewerPane = new CollectionViewerPane();
        desktopPane.add((Component)collectionViewerPane, JLayeredPane.DEFAULT_LAYER);
        collectionViewerPane.setVisible(true);
        MageFrame.setActive(collectionViewerPane);
    }

    static void renderSplashFrame(Graphics2D g) {
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(120, 140, 200, 40);
        g.setPaintMode();
        g.setColor(Color.white);
        g.drawString("Version 0.6.1", 560, 460);
    }

    public static void main(String[] args) {
        JavaUtil.applyDefaultClientSettings();
        LOGGER.info((Object)("Starting MAGE CLIENT version: " + VERSION));
        LOGGER.info((Object)("Java version: " + System.getProperty("java.version")));
        DebugUtil.printLogsInfo(LOGGER);
        LOGGER.info((Object)("Default charset: " + Charset.defaultCharset()));
        if (!Charset.defaultCharset().toString().equals("UTF-8")) {
            LOGGER.warn((Object)"WARNING, bad charset. Some images will not be downloaded. You must:");
            LOGGER.warn((Object)"* Open launcher -> settings -> java -> client java options");
            LOGGER.warn((Object)"* Insert at the the end: -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8");
        }
        startTime = System.currentTimeMillis();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.fatal(null, e));
        SwingUtilities.invokeLater(() -> {
            int settingsVersion;
            SplashScreen splash;
            for (int i = 0; i < args.length; ++i) {
                String arg = args[i];
                if (arg.startsWith(LITE_MODE_ARG)) {
                    liteMode = true;
                }
                if (arg.startsWith(GRAY_MODE_ARG)) {
                    grayMode = true;
                }
                if (arg.startsWith(SKIP_DONE_SYMBOLS)) {
                    skipSmallSymbolGenerationForExisting = true;
                }
                if (!arg.startsWith(DEBUG_ARG)) continue;
                debugMode = true;
            }
            if (System.getProperty(FULL_SCREEN_PROP) != null) {
                macOsFullScreenEnabled = Boolean.parseBoolean(System.getProperty(FULL_SCREEN_PROP));
            }
            if (System.getProperty(GUI_MODAL_MODE_PROP) != null) {
                guiModalModeEnabled = Boolean.parseBoolean(System.getProperty(GUI_MODAL_MODE_PROP));
            }
            debugMode |= VERSION.isDeveloperBuild();
            if (!liteMode && (splash = SplashScreen.getSplashScreen()) != null) {
                Graphics2D g2 = splash.createGraphics();
                try {
                    MageFrame.renderSplashFrame(g2);
                }
                finally {
                    g2.dispose();
                }
                splash.update();
            }
            if ((settingsVersion = PreferencesDialog.getCachedValue("settingsVersion", 0)) == 0) {
                LOGGER.info((Object)"Settings: it's a first run, trying to apply GUI size settings");
                int screenDPI = Toolkit.getDefaultToolkit().getScreenResolution();
                int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
                LOGGER.info((Object)String.format("Settings: screen DPI - %d, screen height - %d", screenDPI, screenHeight));
                String preset = PreferencesDialog.getDefaultSizeSettings().findBestPreset(screenDPI, screenHeight);
                if (preset != null) {
                    LOGGER.info((Object)("Settings: selected preset " + preset));
                    PreferencesDialog.getDefaultSizeSettings().applyPreset(preset);
                } else {
                    LOGGER.info((Object)"Settings: WARNING, can't find compatible preset, use Preferences - GUI Size to setup your app");
                }
                PreferencesDialog.saveValue("settingsVersion", String.valueOf(1));
            }
            try {
                instance = new MageFrame();
                EDTExceptionHandler.registerMainApp(instance);
            }
            catch (Throwable e) {
                LOGGER.fatal((Object)("Critical error on start up, app will be closed: " + e.getMessage()), e);
                System.exit(1);
            }
            if (debugMode) {
                LOGGER.info((Object)"Settings: debug menu enabled");
            }
            MageFrame.instance.separatorDebug.setVisible(debugMode);
            MageFrame.instance.btnDebug.setVisible(debugMode);
            instance.setVisible(true);
        });
    }

    public void setConnectButtonText(String status) {
        this.btnConnect.setText(status);
        this.btnConnect.invalidate();
    }

    public static MageUI getUI() {
        return UI;
    }

    public static ChatPanelBasic getChat(UUID chatId) {
        return CHATS.get(chatId);
    }

    public static void addChat(UUID chatId, ChatPanelBasic chatPanel) {
        CHATS.put(chatId, chatPanel);
    }

    public static void removeChat(UUID chatId) {
        CHATS.remove(chatId);
    }

    public static Map<UUID, ChatPanelBasic> getChatPanels() {
        return CHATS;
    }

    public static void addGame(UUID gameId, GamePanel gamePanel) {
        GAMES.put(gameId, gamePanel);
    }

    public static GamePanel getGame(UUID gameId) {
        return GAMES.get(gameId);
    }

    public static Map<UUID, PlayAreaPanel> getGamePlayers(UUID gameId) {
        GamePanel p = GAMES.get(gameId);
        return p != null ? p.getPlayers() : new HashMap<UUID, PlayAreaPanel>();
    }

    public static void removeGame(UUID gameId) {
        GAMES.remove(gameId);
    }

    public static DraftPanel getDraft(UUID draftId) {
        return DRAFTS.get(draftId);
    }

    public static void removeDraft(UUID draftId) {
        DraftPanel draftPanel = DRAFTS.get(draftId);
        if (draftPanel != null) {
            DRAFTS.remove(draftId);
            draftPanel.hideDraft();
        }
    }

    public static void addDraft(UUID draftId, DraftPanel draftPanel) {
        DRAFTS.put(draftId, draftPanel);
    }

    public int getPanelsCount(boolean onlyActive) {
        return (int)Arrays.stream(desktopPane.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)).filter(Component::isVisible).filter(p -> p instanceof MagePane).map(p -> (MagePane)p).filter(p -> !onlyActive || p.isActiveTable()).count();
    }

    public void connected(String message) {
        SwingUtilities.invokeLater(() -> this.setConnectButtonText(message));
    }

    public void disconnected(boolean askToReconnect, boolean keepMySessionActive) {
        if (SwingUtilities.isEventDispatchThread()) {
            LOGGER.info((Object)"Disconnected from server side");
        } else {
            LOGGER.info((Object)"Disconnected from client side");
        }
        Runnable runOnExit = () -> {
            SessionHandler.disconnect(false, keepMySessionActive);
            this.setConnectButtonText(NOT_CONNECTED_BUTTON);
            this.hideGames();
            this.hideServerLobby();
            if (askToReconnect) {
                UserRequestMessage message = new UserRequestMessage("Connection lost", "The connection to server was lost. Reconnect to " + MagePreferences.getLastServerAddress() + "?");
                message.setButton1("No", null);
                message.setButton2("Yes", PlayerAction.CLIENT_RECONNECT);
                this.showUserRequestDialog(message);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runOnExit.run();
        } else {
            SwingUtilities.invokeLater(runOnExit);
        }
    }

    public void showMessage(String message) {
        UserRequestMessage requestMessage = new UserRequestMessage("Message", message);
        requestMessage.setButton1("OK", null);
        this.showUserRequestDialog(requestMessage);
    }

    public void showError(String message) {
        UserRequestMessage requestMessage = new UserRequestMessage("Error", message);
        requestMessage.setButton1("OK", null);
        this.showUserRequestDialog(requestMessage);
    }

    public void onCallback(ClientCallback callback) {
        callbackClient.onCallback(callback);
    }

    public void onNewConnection() {
        callbackClient.onNewConnection();
    }

    public void sendUserReplay(PlayerAction playerAction, UserRequestMessage userRequestMessage) {
        switch (playerAction) {
            case CLIENT_DOWNLOAD_SYMBOLS: {
                Plugins.instance.downloadSymbols();
                break;
            }
            case CLIENT_DOWNLOAD_CARD_IMAGES: {
                DownloadPicturesService.startDownload();
                break;
            }
            case CLIENT_DISCONNECT_FULL: {
                this.doClientDisconnect(false, "You have disconnected");
                break;
            }
            case CLIENT_DISCONNECT_KEEP_GAMES: {
                this.doClientDisconnect(true, "You have disconnected and have few minutes to reconnect");
                break;
            }
            case CLIENT_QUIT_TOURNAMENT: {
                SessionHandler.quitTournament(userRequestMessage.getTournamentId());
                break;
            }
            case CLIENT_QUIT_DRAFT_TOURNAMENT: {
                SessionHandler.quitDraft(userRequestMessage.getTournamentId());
                MageFrame.removeDraft(userRequestMessage.getTournamentId());
                break;
            }
            case CLIENT_CONCEDE_GAME: {
                SessionHandler.sendPlayerAction(PlayerAction.CONCEDE, userRequestMessage.getGameId(), null);
                break;
            }
            case CLIENT_CONCEDE_MATCH: {
                SessionHandler.quitMatch(userRequestMessage.getGameId());
                break;
            }
            case CLIENT_STOP_WATCHING: {
                SessionHandler.stopWatching(userRequestMessage.getGameId());
                GamePanel gamePanel = MageFrame.getGame(userRequestMessage.getGameId());
                if (gamePanel != null) {
                    gamePanel.removeGame();
                }
                MageFrame.removeGame(userRequestMessage.getGameId());
                break;
            }
            case CLIENT_EXIT_FULL: {
                this.doClientDisconnect(false, "");
                this.doClientShutdownAndExit();
                break;
            }
            case CLIENT_EXIT_KEEP_GAMES: {
                this.doClientDisconnect(true, "");
                this.doClientShutdownAndExit();
                break;
            }
            case CLIENT_REMOVE_TABLE: {
                SessionHandler.removeTable(userRequestMessage.getRoomId(), userRequestMessage.getTableId());
                break;
            }
            case CLIENT_RECONNECT: {
                this.performConnect(true);
                break;
            }
            case CLIENT_REPLAY_ACTION: {
                SessionHandler.stopReplay(userRequestMessage.getGameId());
                break;
            }
            default: {
                if (SessionHandler.getSession() == null || playerAction == null) break;
                SessionHandler.sendPlayerAction(playerAction, userRequestMessage.getGameId(), userRequestMessage.getRelatedUserId());
            }
        }
    }

    private void doClientDisconnect(boolean keepMySessionActive, String afterMessage) {
        if (SessionHandler.isConnected()) {
            SessionHandler.disconnect(false, keepMySessionActive);
        }
        this.tablesPane.clearChat();
        this.setWindowTitle();
        if (!afterMessage.isEmpty()) {
            this.showMessage(afterMessage);
        }
    }

    private void doClientShutdownAndExit() {
        this.tablesPane.cleanUp();
        CardRepository.instance.closeDB(true);
        Plugins.instance.shutdown();
        this.dispose();
        System.exit(0);
    }

    private void endTables() {
        for (UUID gameId : GAMES.keySet()) {
            SessionHandler.quitMatch(gameId);
        }
        for (UUID draftId : DRAFTS.keySet()) {
            SessionHandler.quitDraft(draftId);
        }
    }

    public void refreshGUIAndCards() {
        ImageCaches.clearAll();
        this.setGUISize();
        this.setGUISizeTooltipContainer();
        Plugins.instance.changeGUISize();
        CountryUtil.changeGUISize();
        for (Component component : desktopPane.getComponents()) {
            if (component instanceof MageDialog) {
                ((MageDialog)component).changeGUISize();
            }
            if (!(component instanceof MagePane)) continue;
            ((MagePane)component).changeGUISize();
        }
        for (ChatPanelBasic chatPanel : CHATS.values()) {
            chatPanel.changeGUISize(GUISizeHelper.chatFont);
        }
        try {
            CardInfoPaneImpl cardInfoPane = (CardInfoPaneImpl)UI.getComponent(MageComponents.CARD_INFO_PANE);
            if (cardInfoPane != null) {
                cardInfoPane.changeGUISize();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        this.revalidate();
        this.repaint();
    }

    private void setGUISize() {
        Font font = GUISizeHelper.dialogFont;
        this.mageToolbar.setFont(font);
        int newHeight = font.getSize() + 6;
        Dimension mageToolbarDimension = this.mageToolbar.getPreferredSize();
        mageToolbarDimension.height = newHeight + 6;
        this.mageToolbar.setMinimumSize(mageToolbarDimension);
        this.mageToolbar.setMaximumSize(mageToolbarDimension);
        this.mageToolbar.setPreferredSize(mageToolbarDimension);
        for (Component component : this.mageToolbar.getComponents()) {
            Dimension d;
            if (component instanceof JButton || component instanceof JLabel || component instanceof JToggleButton) {
                component.setFont(font);
                d = component.getPreferredSize();
                d.height = newHeight;
                component.setMinimumSize(d);
                component.setMaximumSize(d);
            }
            if (!(component instanceof JToolBar.Separator)) continue;
            d = component.getPreferredSize();
            d.height = newHeight;
            component.setMinimumSize(d);
            component.setMaximumSize(d);
        }
        this.connectDialog.changeGUISize();
        this.errorDialog.changeGUISize();
        this.menuDownloadSymbols.setFont(font);
        this.menuDownloadImages.setFont(font);
        this.menuDebugTestModalDialog.setFont(font);
        this.menuDebugTestCardRenderModesDialog.setFont(font);
        this.menuDebugTestCustomCode.setFont(font);
        this.mageToolbar.getParent().setBackground(PreferencesDialog.getCurrentTheme().getMageToolbar());
        this.updateTooltipContainerSizes();
    }

    public void showWhatsNewDialog(boolean forceToShowPage) {
        if (this.whatsNewDialog != null) {
            this.whatsNewDialog.checkUpdatesAndShow(forceToShowPage);
        } else {
            AppUtil.openUrlInSystemBrowser("https://jaydi85.github.io/xmage-web-news/news.html");
        }
    }

    public boolean isGameFrameActive(UUID gameId) {
        if (activeFrame != null && activeFrame instanceof GamePane) {
            return ((GamePane)activeFrame).getGameId().equals(gameId);
        }
        return false;
    }

    static {
        PREFS = null;
        VERSION = new MageVersion(MageFrame.class);
        liteMode = false;
        grayMode = false;
        macOsFullScreenEnabled = true;
        skipSmallSymbolGenerationForExisting = false;
        debugMode = false;
        guiModalModeEnabled = false;
        SWITCH_PANELS_BUTTON_NAME = "Switch panels";
        CHATS = new HashMap<UUID, ChatPanelBasic>();
        GAMES = new HashMap<UUID, GamePanel>();
        DRAFTS = new HashMap<UUID, DraftPanel>();
        UI = new MageUI();
        PING_SENDER_EXECUTOR = Executors.newSingleThreadScheduledExecutor(new XmageThreadFactory("XMAGE ping sender"));
    }
}
