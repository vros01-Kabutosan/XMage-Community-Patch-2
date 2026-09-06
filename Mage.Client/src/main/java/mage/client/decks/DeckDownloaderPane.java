/*
 * Decompiled with CFR.
 */
package mage.client.decks;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import mage.client.MageFrame;
import mage.client.MagePane;

public final class DeckDownloaderPane
extends MagePane {
    private static final String DOWNLOADER_VERSION = "Decks V2.2 - 2026-08-11";
    private final JTextArea output = new JTextArea();
    private final JComboBox<String> source = new JComboBox<String>(new String[]{"Todas las fuentes", "MTGO", "MTGGoldfish", "MTGTop8"});
    private final JButton update = new JButton("Actualizar decks");
    private final JButton continueButton = new JButton("Continuar / verificaci\u00f3n completada");
    private final JButton cancelButton = new JButton("Cancelar actualizaci\u00f3n");
    private final JButton openFolder = new JButton("Abrir carpeta de decks");
    private volatile Process process;

    public DeckDownloaderPane() {
        this.setTitle("Descargar decks");
        this.setLayout(new BorderLayout(8, 8));
        JPanel header = new JPanel(new FlowLayout(0, 10, 8));
        header.add(new JLabel(DOWNLOADER_VERSION));
        header.add(new JLabel("Fuente:"));
        header.add(this.source);
        header.add(this.update);
        header.add(this.continueButton);
        header.add(this.cancelButton);
        header.add(this.openFolder);
        this.output.setEditable(false);
        this.output.setFont(new Font("Monospaced", 0, 13));
        this.output.setLineWrap(true);
        this.output.setWrapStyleWord(true);
        this.output.setText("Decks V2 - 2026-08-11\nBiblioteca autom\u00e1tica de metajuego\n\nDescarga Standard, Pioneer y Modern, conserva los mazos antiguos y a\u00f1ade \u00fanicamente composiciones nuevas.\nSi Chrome solicita una verificaci\u00f3n, resu\u00e9lvela y pulsa Continuar.\n");
        this.continueButton.setEnabled(false);
        this.cancelButton.setEnabled(false);
        this.update.addActionListener(this::startUpdate);
        this.continueButton.addActionListener(this::sendContinue);
        this.cancelButton.addActionListener(this::cancelUpdate);
        this.openFolder.addActionListener(this::openDeckFolder);
        this.add((Component)header, "North");
        this.add((Component)new JScrollPane(this.output), "Center");
    }

    private Path engineDirectory() {
        Path current = Paths.get(System.getProperty("user.dir"), new String[0]).toAbsolutePath();
        Path direct = current.resolve("config").resolve("deck-downloader");
        if (Files.isDirectory(direct, new LinkOption[0])) {
            return direct;
        }
        return current.resolve("xmage").resolve("mage-client").resolve("config").resolve("deck-downloader");
    }

    private String pythonCommand() {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "py" : "python3";
    }

    private void startUpdate(ActionEvent event) {
        if (this.process != null && this.process.isAlive()) {
            JOptionPane.showMessageDialog(this, "Ya hay una actualizaci\u00f3n en marcha.");
            return;
        }
        Path engine = this.engineDirectory();
        Path script = engine.resolve("deck_library_updater.py");
        if (!Files.isRegularFile(script, new LinkOption[0])) {
            JOptionPane.showMessageDialog(this, "No se encuentra el motor de descarga:\n" + script, "Descargar decks", 0);
            return;
        }
        String selected = String.valueOf(this.source.getSelectedItem());
        String selectedSource = "all";
        if ("MTGO".equals(selected) || "MTGGoldfish".equals(selected) || "MTGTop8".equals(selected)) {
            selectedSource = selected;
        }
        String requestedSource = selectedSource;
        this.output.setText("Iniciando actualizaci\u00f3n...\n");
        this.update.setEnabled(false);
        this.continueButton.setEnabled(true);
        this.cancelButton.setEnabled(true);
        Thread worker = new Thread(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(this.pythonCommand(), "-u", script.toString(), "--source", requestedSource);
                builder.directory(engine.toFile());
                builder.redirectErrorStream(true);
                this.process = builder.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.process.getInputStream(), StandardCharsets.UTF_8));){
                    String line;
                    while ((line = reader.readLine()) != null) {
                        this.append(line + "\n");
                    }
                }
                int result = this.process.waitFor();
                this.append("\nProceso terminado con c\u00f3digo " + result + ".\n");
                if (result == 0) {
                    this.runDeckOrganizer();
                }
            }
            catch (Exception error) {
                this.append("\nERROR: " + error.getMessage() + "\n");
            }
            finally {
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
        }
        catch (IOException error) {
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
            this.append("[Cancelaci\u00f3n solicitada; cerrando procesos...]\n");
        }
        catch (IOException error) {
            this.append("No se pudo solicitar la cancelaci\u00f3n: " + error.getMessage() + "\n");
        }
    }

    private void openDeckFolder(ActionEvent event) {
        Path folder = this.engineDirectory().getParent().getParent().resolve("sample-decks").resolve("Descargados");
        try {
            Files.createDirectories(folder, new FileAttribute[0]);
            Desktop.getDesktop().open(folder.toFile());
        }
        catch (Exception error) {
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
}

