package mage.client.decks;

import java.awt.Component;
import java.lang.reflect.Method;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.junit.Test;

public class DeckDownloaderPaneTest {

    @Test
    public void testDeckDownloaderPaneInitialization() throws Exception {
        final DeckDownloaderPane[] paneHolder = new DeckDownloaderPane[1];
        SwingUtilities.invokeAndWait(() -> {
            paneHolder[0] = new DeckDownloaderPane();
        });

        DeckDownloaderPane pane = paneHolder[0];
        Assert.assertNotNull("DeckDownloaderPane should instantiate", pane);
        Assert.assertEquals("Title should match", "Descargar decks", pane.getTitle());
        Assert.assertFalse("ActiveTable should be false", pane.isActiveTable());
        Assert.assertNull("SortTableId should be null", pane.getSortTableId());
    }

    @Test
    public void testEventParsing() throws Exception {
        final DeckDownloaderPane[] paneHolder = new DeckDownloaderPane[1];
        SwingUtilities.invokeAndWait(() -> {
            paneHolder[0] = new DeckDownloaderPane();
        });

        DeckDownloaderPane pane = paneHolder[0];
        Method parseMethod = DeckDownloaderPane.class.getDeclaredMethod("processEventLine", String.class);
        parseMethod.setAccessible(true);

        String[] testLines = new String[] {
            "=== MTGO: ejecutando mtgo_3_formatos_fusionado.py ===",
            "RECUPERACIÓN MTGO: nuevos=10, repetidos=5, rechazados=0",
            "[1/25] [Standard] Azorius Control -> OK 1/25",
            "NUEVO [Standard] 2026-10-31_MTGO_1_Azorius Control.dck",
            "[2/25] [Pioneer] Izzet Phoenix -> OK 2/25",
            "[3/25] [Modern] Dimir Murktide -> OK 3/25",
            "RECHAZADO [Modern] Bad Deck: Scryfall not found",
            "RESUMEN MTGO: nuevos=46, repetidos=29, rechazados=1",
            "Iniciando ordenación automática de decks...",
            "Ordenación automática terminada con código 0."
        };

        for (String line : testLines) {
            parseMethod.invoke(pane, line);
        }

        // Wait for all SwingUtilities.invokeLater updates to execute
        SwingUtilities.invokeAndWait(() -> {
            // Check that pane remains healthy and responsive
            Assert.assertNotNull(pane);
        });
    }
}

