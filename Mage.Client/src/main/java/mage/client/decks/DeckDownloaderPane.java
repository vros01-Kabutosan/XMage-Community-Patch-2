package mage.client.decks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import mage.client.MageFrame;
import mage.client.MagePane;

/**
 * Modern Dashboard UI for XMage Deck Downloader.
 * Real-time event parsing, status cards for MTGO, MTGGoldfish, MTGTop8,
 * format progress bars and collapsible dark console.
 */
public final class DeckDownloaderPane extends MagePane {

    private static final String DOWNLOADER_VERSION = "Decks V2.2 - 2026-08-11";

    // Theme Palette (Modern Dark Slate)
    private static final Color BG_DARK = new Color(22, 24, 30);
    private static final Color CARD_BG = new Color(30, 33, 42);
    private static final Color CARD_BORDER = new Color(46, 51, 64);
    private static final Color TEXT_MAIN = new Color(241, 245, 249);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);
    private static final Color ACCENT_BLUE = new Color(59, 130, 246);
    private static final Color ACCENT_GREEN = new Color(34, 197, 94);
    private static final Color ACCENT_ORANGE = new Color(245, 158, 11);
    private static final Color ACCENT_RED = new Color(239, 68, 68);
    private static final Color ACCENT_PURPLE = new Color(139, 92, 246);

    // Controls
    private final JComboBox<String> source = new JComboBox<>(new String[]{"Todas las fuentes", "MTGO", "MTGGoldfish", "MTGTop8"});
    private final JButton update = createStyledButton("Actualizar decks", ACCENT_BLUE, Color.WHITE);
    private final JButton continueButton = createStyledButton("Continuar / Verificación", ACCENT_ORANGE, Color.BLACK);
    private final JButton cancelButton = createStyledButton("Cancelar", new Color(71, 85, 105), TEXT_MAIN);
    private final JButton openFolder = createStyledButton("Abrir carpeta", new Color(51, 65, 85), TEXT_MAIN);

    // Cards
    private final SourceCard mtgoCard = new SourceCard("MTGO", "Magic Online");
    private final SourceCard goldfishCard = new SourceCard("MTGGoldfish", "Torneos y Metajuego");
    private final SourceCard top8Card = new SourceCard("MTGTop8", "Resultados Competitivos");

    // Formats Progress
    private final FormatRow standardRow = new FormatRow("Standard", ACCENT_BLUE);
    private final FormatRow pioneerRow = new FormatRow("Pioneer", ACCENT_PURPLE);
    private final FormatRow modernRow = new FormatRow("Modern", ACCENT_GREEN);

    // Global Status
    private final JLabel globalStatusLabel = new JLabel("Listo para sincronizar");
    private final StatusBadge globalStateBadge = new StatusBadge("EN ESPERA", new Color(100, 116, 139));
    private final JProgressBar globalProgressBar = new JProgressBar(0, 100);

    // Console Drawer
    private final JTextArea output = new JTextArea();
    private final JScrollPane consoleScroll;
    private final JSplitPane splitPane;
    private final JButton toggleConsoleBtn = createSmallButton("▼ Ocultar consola");

    private volatile Process process;

    // Regex patterns for parsing python output in real time
    private static final Pattern PATTERN_CONNECTOR = Pattern.compile("^===\\s*(MTGO|MTGGoldfish|MTGTop8):\\s*(.*?)\\s*===", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_RECOVERY = Pattern.compile("^RECUPERACI[ÓO]N\\s+(MTGO|MTGGoldfish|MTGTop8):\\s*nuevos=(\\d+),\\s*repetidos=(\\d+),\\s*rechazados=(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUMMARY = Pattern.compile("^RESUMEN\\s+(MTGO|MTGGoldfish|MTGTop8):\\s*nuevos=(\\d+),\\s*repetidos=(\\d+),\\s*rechazados=(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_PROGRESS = Pattern.compile("^\\[(\\d+)/(\\d+)\\]\\s*\\[(Standard|Pioneer|Modern)\\]\\s*(.*?)\\s*->\\s*(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_NEW_DECK = Pattern.compile("^NUEVO\\s*\\[(Standard|Pioneer|Modern)\\]\\s*(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_REJECTED = Pattern.compile("^RECHAZADO\\s*\\[(Standard|Pioneer|Modern)\\]\\s*(.*)", Pattern.CASE_INSENSITIVE);

    public DeckDownloaderPane() {
        this.setTitle("Descargar decks");
        this.setLayout(new BorderLayout());
        this.setBackground(BG_DARK);

        // 1. Header & Controls Panel
        JPanel headerPanel = buildHeaderPanel();

        // 2. Dashboard Panel (Cards + Progress)
        JPanel dashboardPanel = buildDashboardPanel();

        // 3. Console Panel
        JPanel consolePanel = buildConsolePanel();
        consoleScroll = new JScrollPane(output);
        consoleScroll.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
        consolePanel.add(consoleScroll, BorderLayout.CENTER);

        // SplitPane separating Dashboard and Console Drawer
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dashboardPanel, consolePanel);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);
        splitPane.setBackground(BG_DARK);

        this.add(headerPanel, BorderLayout.NORTH);
        this.add(splitPane, BorderLayout.CENTER);

        // Initial component state
        this.continueButton.setEnabled(false);
        this.cancelButton.setEnabled(false);
        this.update.addActionListener(this::startUpdate);
        this.continueButton.addActionListener(this::sendContinue);
        this.cancelButton.addActionListener(this::cancelUpdate);
        this.openFolder.addActionListener(this::openDeckFolder);

        this.output.setEditable(false);
        this.output.setFont(new Font("Consolas", Font.PLAIN, 12));
        this.output.setBackground(new Color(15, 17, 22));
        this.output.setForeground(new Color(226, 232, 240));
        this.output.setCaretColor(ACCENT_BLUE);
        this.output.setLineWrap(true);
        this.output.setWrapStyleWord(true);
        this.output.append(DOWNLOADER_VERSION + " - Biblioteca automática de metajuego\n");
        this.output.append("Descarga Standard, Pioneer y Modern. Conserva mazos antiguos y añade nuevas composiciones.\n");
        this.output.append("Pulsa 'Actualizar decks' para iniciar la sincronización.\n\n");
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new RoundedPanel(12, CARD_BG, CARD_BORDER);
        header.setLayout(new BorderLayout(16, 12));
        header.setBorder(new EmptyBorder(14, 18, 14, 18));

        // Left info
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel("Biblioteca Automática de Metajuego");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(TEXT_MAIN);
        titleRow.add(titleLabel);
        titleRow.add(globalStateBadge);

        JLabel subtitle = new JLabel(DOWNLOADER_VERSION + " • Descarga y catalogación de mazos competitivos");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_MUTED);

        titleBox.add(titleRow);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);

        // Right actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JLabel srcLabel = new JLabel("Fuente:");
        srcLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        srcLabel.setForeground(TEXT_MAIN);

        source.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        source.setPreferredSize(new Dimension(150, 32));

        actions.add(srcLabel);
        actions.add(source);
        actions.add(update);
        actions.add(continueButton);
        actions.add(cancelButton);
        actions.add(openFolder);

        header.add(titleBox, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(10, 12, 6, 12));
        outer.add(header, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildDashboardPanel() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(4, 12, 6, 12));

        // 1. Grid of 3 Source Cards
        JPanel cardsGrid = new JPanel(new GridLayout(1, 3, 12, 0));
        cardsGrid.setOpaque(false);
        cardsGrid.add(mtgoCard);
        cardsGrid.add(goldfishCard);
        cardsGrid.add(top8Card);
        cardsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 135));
        cardsGrid.setPreferredSize(new Dimension(800, 130));

        // 2. Progress and Formats Card
        JPanel progressCard = new RoundedPanel(12, CARD_BG, CARD_BORDER);
        progressCard.setLayout(new BorderLayout(14, 10));
        progressCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Global Progress Header
        JPanel globalBox = new JPanel(new BorderLayout(8, 6));
        globalBox.setOpaque(false);

        JPanel globalTextRow = new JPanel(new BorderLayout());
        globalTextRow.setOpaque(false);
        JLabel progTitle = new JLabel("Progreso General de Sincronización");
        progTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        progTitle.setForeground(TEXT_MAIN);

        globalStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        globalStatusLabel.setForeground(TEXT_MUTED);

        globalTextRow.add(progTitle, BorderLayout.WEST);
        globalTextRow.add(globalStatusLabel, BorderLayout.EAST);

        globalProgressBar.setValue(0);
        globalProgressBar.setStringPainted(true);
        globalProgressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        globalProgressBar.setForeground(ACCENT_BLUE);
        globalProgressBar.setBackground(new Color(20, 22, 28));
        globalProgressBar.setPreferredSize(new Dimension(100, 16));

        globalBox.add(globalTextRow, BorderLayout.NORTH);
        globalBox.add(globalProgressBar, BorderLayout.CENTER);

        // Formats Breakdown
        JPanel formatsBox = new JPanel(new GridLayout(3, 1, 0, 8));
        formatsBox.setOpaque(false);
        formatsBox.add(standardRow);
        formatsBox.add(pioneerRow);
        formatsBox.add(modernRow);

        progressCard.add(globalBox, BorderLayout.NORTH);
        progressCard.add(formatsBox, BorderLayout.CENTER);

        container.add(cardsGrid);
        container.add(Box.createVerticalStrut(10));
        container.add(progressCard);

        return container;
    }

    private JPanel buildConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 12, 10, 12));

        JPanel bar = new RoundedPanel(8, new Color(28, 31, 39), CARD_BORDER);
        bar.setLayout(new BorderLayout());
        bar.setBorder(new EmptyBorder(6, 12, 6, 12));

        JLabel title = new JLabel("Consola de Registro Técnico");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(TEXT_MUTED);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow.setOpaque(false);

        JButton copyBtn = createSmallButton("Copiar registro");
        copyBtn.addActionListener(e -> copyLogToClipboard());

        JButton clearBtn = createSmallButton("Limpiar");
        clearBtn.addActionListener(e -> output.setText(""));

        toggleConsoleBtn.addActionListener(e -> toggleConsole());

        btnRow.add(copyBtn);
        btnRow.add(clearBtn);
        btnRow.add(toggleConsoleBtn);

        bar.add(title, BorderLayout.WEST);
        bar.add(btnRow, BorderLayout.EAST);

        panel.add(bar, BorderLayout.NORTH);
        return panel;
    }

    private void toggleConsole() {
        boolean visible = consoleScroll.isVisible();
        consoleScroll.setVisible(!visible);
        toggleConsoleBtn.setText(!visible ? "▼ Ocultar consola" : "▲ Mostrar consola");
        splitPane.resetToPreferredSizes();
        revalidate();
        repaint();
    }

    private void copyLogToClipboard() {
        String text = output.getText();
        if (text != null && !text.isEmpty()) {
            StringSelection selection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            JOptionPane.showMessageDialog(this, "Registro copiado al portapapeles.", "Descargar decks", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        return btn;
    }

    private static JButton createSmallButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setBackground(new Color(40, 44, 56));
        btn.setForeground(TEXT_MAIN);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        return btn;
    }

    private Path engineDirectory() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path direct = current.resolve("config").resolve("deck-downloader");
        if (Files.isDirectory(direct, new LinkOption[0])) {
            return direct;
        }
        return current.resolve("xmage").resolve("mage-client").resolve("config").resolve("deck-downloader");
    }

    private String pythonCommand() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return "python3";
        }
        Path engine = this.engineDirectory();
        if (engine != null) {
            Path localPy = engine.resolve("py.exe");
            if (Files.isRegularFile(localPy, new LinkOption[0])) {
                return localPy.toString();
            }
            Path localPython = engine.resolve("python.exe");
            if (Files.isRegularFile(localPython, new LinkOption[0])) {
                return localPython.toString();
            }
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        String[] candidates = new String[] {
            "py",
            "python",
            localAppData != null ? localAppData + "\\Programs\\Python\\Launcher\\py.exe" : null,
            localAppData != null ? localAppData + "\\Programs\\Python\\Python312\\python.exe" : null,
            localAppData != null ? localAppData + "\\Programs\\Python\\Python313\\python.exe" : null,
            localAppData != null ? localAppData + "\\Programs\\Python\\Python314\\python.exe" : null
        };
        for (String candidate : candidates) {
            if (candidate == null) continue;
            try {
                Process p = new ProcessBuilder(candidate, "--version").start();
                if (p.waitFor() == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {
            }
        }
        return "py";
    }

    private void resetDashboardUI() {
        globalStateBadge.update("SINCRONIZANDO...", ACCENT_BLUE);
        globalStatusLabel.setText("Iniciando conectores de descarga...");
        globalProgressBar.setValue(5);

        mtgoCard.reset();
        goldfishCard.reset();
        top8Card.reset();

        standardRow.reset();
        pioneerRow.reset();
        modernRow.reset();
    }

    private void startUpdate(ActionEvent event) {
        if (this.process != null && this.process.isAlive()) {
            JOptionPane.showMessageDialog(this, "Ya hay una actualización en marcha.");
            return;
        }
        Path engine = this.engineDirectory();
        Path script = engine.resolve("deck_library_updater.py");
        if (!Files.isRegularFile(script, new LinkOption[0])) {
            JOptionPane.showMessageDialog(this, "No se encuentra el motor de descarga:\n" + script, "Descargar decks", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String selected = String.valueOf(this.source.getSelectedItem());
        String selectedSource = "all";
        if ("MTGO".equals(selected) || "MTGGoldfish".equals(selected) || "MTGTop8".equals(selected)) {
            selectedSource = selected;
        }
        String requestedSource = selectedSource;

        resetDashboardUI();
        this.append("=== Iniciando sincronización de metajuego [" + requestedSource + "] ===\n");
        this.update.setEnabled(false);
        this.continueButton.setEnabled(true);
        this.cancelButton.setEnabled(true);

        Thread worker = new Thread(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(this.pythonCommand(), "-u", script.toString(), "--source", requestedSource);
                builder.directory(engine.toFile());
                builder.redirectErrorStream(true);
                this.process = builder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        this.processEventLine(line);
                        this.append(line + "\n");
                    }
                }
                int result = this.process.waitFor();
                this.append("\nProceso terminado con código " + result + ".\n");

                if (result == 0) {
                    this.runDeckOrganizer();
                    SwingUtilities.invokeLater(() -> {
                        globalStateBadge.update("AL DÍA", ACCENT_GREEN);
                        globalStatusLabel.setText("Sincronización y ordenación completadas con éxito.");
                        globalProgressBar.setValue(100);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        globalStateBadge.update("FINALIZADO (" + result + ")", ACCENT_ORANGE);
                        globalStatusLabel.setText("Proceso finalizado con advertencias o cancelado.");
                    });
                }
            } catch (Exception error) {
                this.append("\nERROR: " + error.getMessage() + "\n");
                SwingUtilities.invokeLater(() -> {
                    globalStateBadge.update("ERROR", ACCENT_RED);
                    globalStatusLabel.setText("Error en la descarga: " + error.getMessage());
                });
            } finally {
                this.process = null;
                SwingUtilities.invokeLater(() -> {
                    this.update.setEnabled(true);
                    this.continueButton.setEnabled(false);
                    this.cancelButton.setEnabled(false);
                });
            }
        }, "xmage-deck-downloader");
        worker.setDaemon(true);
        worker.start();
    }

    private void processEventLine(String line) {
        if (line == null || line.trim().isEmpty()) return;

        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Connector execution banner
                Matcher mConn = PATTERN_CONNECTOR.matcher(line);
                if (mConn.find()) {
                    String src = mConn.group(1);
                    String action = mConn.group(2);
                    SourceCard card = getSourceCard(src);
                    if (card != null) {
                        card.setStatus("PROCESANDO...", ACCENT_BLUE);
                        card.setActiveActivity(action);
                    }
                    globalStatusLabel.setText("Ejecutando " + src + ": " + action);
                    return;
                }

                // 2. Summary banner
                Matcher mSumm = PATTERN_SUMMARY.matcher(line);
                if (mSumm.find()) {
                    String src = mSumm.group(1);
                    int n = Integer.parseInt(mSumm.group(2));
                    int r = Integer.parseInt(mSumm.group(3));
                    int x = Integer.parseInt(mSumm.group(4));
                    SourceCard card = getSourceCard(src);
                    if (card != null) {
                        card.setStats(n, r, x);
                        card.setStatus("COMPLETADO", ACCENT_GREEN);
                    }
                    return;
                }

                // 3. Recovery banner
                Matcher mRec = PATTERN_RECOVERY.matcher(line);
                if (mRec.find()) {
                    String src = mRec.group(1);
                    int n = Integer.parseInt(mRec.group(2));
                    int r = Integer.parseInt(mRec.group(3));
                    int x = Integer.parseInt(mRec.group(4));
                    SourceCard card = getSourceCard(src);
                    if (card != null) {
                        card.setStats(n, r, x);
                        card.setActiveActivity("Recuperados de caché anterior");
                    }
                    return;
                }

                // 4. Download progress: [1/25] [Standard] Azorius Control -> OK 1/25
                Matcher mProg = PATTERN_PROGRESS.matcher(line);
                if (mProg.find()) {
                    int cur = Integer.parseInt(mProg.group(1));
                    int tot = Integer.parseInt(mProg.group(2));
                    String fmt = mProg.group(3);
                    String deck = mProg.group(4);

                    FormatRow row = getFormatRow(fmt);
                    if (row != null) {
                        row.setProgress(cur, tot, deck);
                    }
                    globalStatusLabel.setText("[" + fmt + "] Descargando: " + deck + " (" + cur + "/" + tot + ")");
                    updateGlobalProgress();
                    return;
                }

                // 5. New deck imported
                Matcher mNew = PATTERN_NEW_DECK.matcher(line);
                if (mNew.find()) {
                    String fmt = mNew.group(1);
                    String deck = mNew.group(2);
                    FormatRow row = getFormatRow(fmt);
                    if (row != null) {
                        row.incrementNew();
                    }
                    globalStatusLabel.setText("Nuevo mazo añadido: " + deck);
                    return;
                }

                // 6. Deck rejected
                Matcher mRej = PATTERN_REJECTED.matcher(line);
                if (mRej.find()) {
                    String fmt = mRej.group(1);
                    String msg = mRej.group(2);
                    globalStatusLabel.setText("Descartado [" + fmt + "]: " + msg);
                    return;
                }

                // 7. Auto-organizer
                if (line.contains("Iniciando ordenación automática")) {
                    globalStatusLabel.setText("Organizando biblioteca por carpetas y fechas...");
                    globalStateBadge.update("ORGANIZANDO", ACCENT_PURPLE);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void updateGlobalProgress() {
        int stdVal = standardRow.progressBar.getValue();
        int pioVal = pioneerRow.progressBar.getValue();
        int modVal = modernRow.progressBar.getValue();
        int total = (stdVal + pioVal + modVal); // Max 75
        int percent = Math.min(95, Math.max(5, (total * 100) / 75));
        globalProgressBar.setValue(percent);
    }

    private SourceCard getSourceCard(String name) {
        if ("MTGO".equalsIgnoreCase(name)) return mtgoCard;
        if ("MTGGoldfish".equalsIgnoreCase(name)) return goldfishCard;
        if ("MTGTop8".equalsIgnoreCase(name)) return top8Card;
        return null;
    }

    private FormatRow getFormatRow(String name) {
        if ("Standard".equalsIgnoreCase(name)) return standardRow;
        if ("Pioneer".equalsIgnoreCase(name)) return pioneerRow;
        if ("Modern".equalsIgnoreCase(name)) return modernRow;
        return null;
    }

    private void runDeckOrganizer() {
        Path script = this.engineDirectory().getParent().getParent().resolve("sample-decks").resolve("Descargados").resolve("ordenar_decks.py");
        if (!Files.isRegularFile(script, new LinkOption[0])) {
            this.append("No se encuentra el ordenador de decks: " + script + "\n");
            return;
        }
        this.append("Iniciando ordenación automática de decks...\n");
        try {
            ProcessBuilder organizer = new ProcessBuilder(this.pythonCommand(), "-u", script.toString(), "--no-pausa", "--extensiones", ".dck");
            organizer.directory(script.getParent().toFile());
            organizer.redirectErrorStream(true);
            Process organizerProcess = organizer.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(organizerProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    this.append("[Ordenador] " + line + "\n");
                }
            }
            int organizerResult = organizerProcess.waitFor();
            this.append("Ordenación automática terminada con código " + organizerResult + ".\n");
        } catch (Exception error) {
            this.append("ERROR al ordenar decks: " + error.getMessage() + "\n");
        }
    }

    private void sendContinue(ActionEvent event) {
        Process active = this.process;
        if (active == null || !active.isAlive()) {
            return;
        }
        try {
            active.getOutputStream().write(10);
            active.getOutputStream().flush();
            this.append("[Continuar enviado]\n");
        } catch (IOException error) {
            this.append("No se pudo enviar Continuar: " + error.getMessage() + "\n");
        }
    }

    private void cancelUpdate(ActionEvent event) {
        Process active = this.process;
        if (active == null || !active.isAlive()) {
            return;
        }
        try {
            Files.write(this.engineDirectory().resolve(".cancel-update"), new byte[]{49}, new OpenOption[0]);
            this.cancelButton.setEnabled(false);
            this.append("[Cancelación solicitada; cerrando procesos...]\n");
            globalStatusLabel.setText("Cancelando actualización...");
            globalStateBadge.update("CANCELANDO", ACCENT_RED);
        } catch (IOException error) {
            this.append("No se pudo solicitar la cancelación: " + error.getMessage() + "\n");
        }
    }

    private void openDeckFolder(ActionEvent event) {
        Path folder = this.engineDirectory().getParent().getParent().resolve("sample-decks").resolve("Descargados");
        try {
            Files.createDirectories(folder, new FileAttribute[0]);
            Desktop.getDesktop().open(folder.toFile());
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir:\n" + folder);
        }
    }

    private void append(String text) {
        SwingUtilities.invokeLater(() -> {
            this.output.append(text);
            this.output.setCaretPosition(this.output.getDocument().getLength());
        });
    }

    @Override
    public UUID getSortTableId() {
        return null;
    }

    @Override
    public boolean isActiveTable() {
        return false;
    }

    public static void showPane() {
        for (Component component : MageFrame.getDesktop().getComponents()) {
            if (!(component instanceof DeckDownloaderPane)) continue;
            MageFrame.setActive((DeckDownloaderPane)component);
            return;
        }
        DeckDownloaderPane pane = new DeckDownloaderPane();
        MageFrame.getDesktop().add((Component)pane, JLayeredPane.DEFAULT_LAYER);
        pane.setVisible(true);
        MageFrame.setActive(pane);
    }

    // =========================================================================
    // INNER CLASSES: Custom Modern UI Components
    // =========================================================================

    /**
     * Card component representing one data source (MTGO, MTGGoldfish, MTGTop8).
     */
    private static class SourceCard extends RoundedPanel {
        private final JLabel nameLabel;
        private final StatusBadge badge;
        private final JLabel newBadge = new JLabel("+0 Nuevos");
        private final JLabel dupBadge = new JLabel("=0 Repetidos");
        private final JLabel rejBadge = new JLabel("!0 Errores");
        private final JLabel activityLabel = new JLabel("En espera");

        public SourceCard(String title, String subtitle) {
            super(12, CARD_BG, CARD_BORDER);
            this.setLayout(new BorderLayout(8, 6));
            this.setBorder(new EmptyBorder(10, 14, 10, 14));

            // Header Row
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);

            JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            titleBox.setOpaque(false);
            nameLabel = new JLabel(title);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            nameLabel.setForeground(TEXT_MAIN);
            titleBox.add(nameLabel);

            JLabel sub = new JLabel("(" + subtitle + ")");
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            sub.setForeground(TEXT_MUTED);
            titleBox.add(sub);

            badge = new StatusBadge("INACTIVO", new Color(71, 85, 105));
            top.add(titleBox, BorderLayout.WEST);
            top.add(badge, BorderLayout.EAST);

            // Metrics Row
            JPanel metrics = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            metrics.setOpaque(false);

            styleMetricLabel(newBadge, ACCENT_GREEN);
            styleMetricLabel(dupBadge, TEXT_MUTED);
            styleMetricLabel(rejBadge, ACCENT_RED);

            metrics.add(newBadge);
            metrics.add(dupBadge);
            metrics.add(rejBadge);

            // Activity Row
            activityLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            activityLabel.setForeground(TEXT_MUTED);

            this.add(top, BorderLayout.NORTH);
            this.add(metrics, BorderLayout.CENTER);
            this.add(activityLabel, BorderLayout.SOUTH);
        }

        private static void styleMetricLabel(JLabel label, Color color) {
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setForeground(color);
        }

        public void reset() {
            badge.update("EN ESPERA", new Color(71, 85, 105));
            newBadge.setText("+0 Nuevos");
            dupBadge.setText("=0 Repetidos");
            rejBadge.setText("!0 Errores");
            activityLabel.setText("En espera...");
        }

        public void setStatus(String status, Color color) {
            badge.update(status, color);
        }

        public void setStats(int n, int d, int r) {
            newBadge.setText("+" + n + " Nuevos");
            dupBadge.setText("=" + d + " Repetidos");
            rejBadge.setText("!" + r + " Errores");
        }

        public void setActiveActivity(String text) {
            activityLabel.setText(text);
        }
    }

    /**
     * Row representing a format progress bar (Standard, Pioneer, Modern).
     */
    private static class FormatRow extends JPanel {
        private final JLabel titleLabel;
        private final JProgressBar progressBar;
        private final JLabel statusLabel;
        private int newCount = 0;

        public FormatRow(String formatName, Color accentColor) {
            this.setLayout(new BorderLayout(10, 4));
            this.setOpaque(false);

            titleLabel = new JLabel(formatName);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleLabel.setForeground(TEXT_MAIN);
            titleLabel.setPreferredSize(new Dimension(80, 20));

            progressBar = new JProgressBar(0, 25);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
            progressBar.setString("0 / 25");
            progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));
            progressBar.setForeground(accentColor);
            progressBar.setBackground(new Color(20, 22, 28));
            progressBar.setPreferredSize(new Dimension(240, 16));

            statusLabel = new JLabel("En cola");
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            statusLabel.setForeground(TEXT_MUTED);

            this.add(titleLabel, BorderLayout.WEST);
            this.add(progressBar, BorderLayout.CENTER);
            this.add(statusLabel, BorderLayout.EAST);
        }

        public void reset() {
            newCount = 0;
            progressBar.setValue(0);
            progressBar.setString("0 / 25");
            statusLabel.setText("En cola");
        }

        public void setProgress(int current, int max, String deckName) {
            progressBar.setMaximum(max);
            progressBar.setValue(current);
            progressBar.setString(current + " / " + max + (newCount > 0 ? " (+" + newCount + " nuevos)" : ""));
            statusLabel.setText(deckName != null && !deckName.isEmpty() ? deckName : "Procesando...");
        }

        public void incrementNew() {
            newCount++;
            progressBar.setString(progressBar.getValue() + " / " + progressBar.getMaximum() + " (+" + newCount + " nuevos)");
        }
    }

    /**
     * Minimalist Pill badge for visual status tags.
     */
    private static class StatusBadge extends JLabel {
        private Color bgColor;

        public StatusBadge(String text, Color bg) {
            super(text, SwingConstants.CENTER);
            this.bgColor = bg;
            this.setFont(new Font("Segoe UI", Font.BOLD, 10));
            this.setForeground(Color.WHITE);
            this.setBorder(new EmptyBorder(3, 8, 3, 8));
        }

        public void update(String text, Color bg) {
            this.setText(text);
            this.bgColor = bg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Smooth rounded card panel.
     */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color background;
        private final Color border;

        public RoundedPanel(int radius, Color bg, Color border) {
            this.radius = radius;
            this.background = bg;
            this.border = border;
            this.setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (border != null) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
