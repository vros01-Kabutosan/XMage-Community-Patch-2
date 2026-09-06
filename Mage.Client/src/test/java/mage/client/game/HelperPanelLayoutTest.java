package mage.client.game;

import mage.client.util.GUISizeHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

class HelperPanelLayoutTest {

    @Test
    void scaledDecisionContentStaysInsideRoundedSurfaceWithoutScrollBars() throws Exception {
        Font oldFont = GUISizeHelper.gameFeedbackPanelFont;
        int oldMain = GUISizeHelper.gameFeedbackPanelMainMessageFontSize;
        int oldExtra = GUISizeHelper.gameFeedbackPanelExtraMessageFontSize;
        int oldButtonHeight = GUISizeHelper.gameFeedbackPanelButtonHeight;

        try {
            GUISizeHelper.gameFeedbackPanelFont = new Font("Arial", Font.PLAIN, 32);
            GUISizeHelper.gameFeedbackPanelMainMessageFontSize = 32;
            GUISizeHelper.gameFeedbackPanelExtraMessageFontSize = 18;
            GUISizeHelper.gameFeedbackPanelButtonHeight = 38;

            AtomicReference<HelperPanel> reference = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                HelperPanel panel = new HelperPanel();
                panel.setMessages("Play instants and activated abilities", "Your turn / Upkeep");
                panel.setLeft("Mulligan", true);
                panel.setRight("Keep", true);
                panel.setSpecial("Special action", true);
                panel.setUndoEnabled(true);
                panel.setTurnInfo("A player with a long name", 123, true);
                panel.changeGUISize();
                panel.setSize(1600, panel.getRequiredHeight());
                layoutTree(panel);
                reference.set(panel);
            });
            SwingUtilities.invokeAndWait(() -> layoutTree(reference.get()));

            HelperPanel panel = reference.get();
            Assertions.assertFalse(containsScrollPane(panel), "decision UI must never create a scroll bar");

            JPanel surface = getField(panel, "decisionSurface", JPanel.class);
            JPanel footer = panel.buttonContainer;
            JLabel badge = getField(panel, "turnBadge", JLabel.class);

            assertVisibleChildrenInside(surface);
            assertVisibleChildrenInside(footer);
            assertVisibleChildrenInside(panel.buttonGrid);
            Assertions.assertTrue(surface.getBounds().height > 0, "rounded surface must be visible");
            Assertions.assertTrue(footer.getHeight() >= panel.buttonGrid.getHeight(), "footer must contain full buttons");
            Assertions.assertTrue(footer.getHeight() >= badge.getHeight(), "footer must contain full turn badge");
            Assertions.assertFalse(panel.buttonGrid.getBounds().intersects(badge.getBounds()), "turn badge must not overlap actions");

            int footerCentre = footer.getWidth() / 2;
            int buttonsCentre = panel.buttonGrid.getX() + panel.buttonGrid.getWidth() / 2;
            Assertions.assertTrue(Math.abs(footerCentre - buttonsCentre) <= 1, "action buttons must remain centred");
        } finally {
            GUISizeHelper.gameFeedbackPanelFont = oldFont;
            GUISizeHelper.gameFeedbackPanelMainMessageFontSize = oldMain;
            GUISizeHelper.gameFeedbackPanelExtraMessageFontSize = oldExtra;
            GUISizeHelper.gameFeedbackPanelButtonHeight = oldButtonHeight;
        }
    }

    @Test
    void outerFeedbackRowGrowsWhenHtmlWrapsAfterResize() throws Exception {
        AtomicReference<FeedbackPanel> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            FeedbackPanel feedback = new FeedbackPanel();
            HelperPanel helper = new HelperPanel();
            feedback.setHelperPanel(helper);
            feedback.setLayout(new BorderLayout());
            feedback.add(helper, BorderLayout.CENTER);
            helper.setMessages("A deliberately long decision message that must wrap onto a second line without clipping", "Additional context");
            helper.setLeft("Yes", true);
            helper.setRight("No", true);
            helper.setTurnInfo("Opponent", 4, false);
            helper.changeGUISize();
            helper.setSize(1000, helper.getRequiredHeight());
            feedback.setSize(1000, helper.getRequiredHeight());
            layoutTree(feedback);
            reference.set(feedback);
        });
        SwingUtilities.invokeAndWait(() -> layoutTree(reference.get()));

        FeedbackPanel feedback = reference.get();
        HelperPanel helper = (HelperPanel) feedback.getComponent(0);
        Assertions.assertTrue(feedback.getHeight() >= helper.getRequiredHeight(), "outer feedback row must contain helper");
        Assertions.assertTrue(feedback.getPreferredSize().height >= helper.getRequiredHeight(), "outer preferred height must follow helper");
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                layoutTree((Container) component);
            }
        }
    }

    private static boolean containsScrollPane(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JScrollPane) {
                return true;
            }
            if (component instanceof Container && containsScrollPane((Container) component)) {
                return true;
            }
        }
        return false;
    }

    private static void assertVisibleChildrenInside(Container parent) {
        for (Component child : parent.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            Rectangle bounds = child.getBounds();
            Assertions.assertTrue(bounds.x >= 0 && bounds.y >= 0, child.getClass().getSimpleName() + " starts outside parent");
            Assertions.assertTrue(bounds.x + bounds.width <= parent.getWidth(), child.getClass().getSimpleName() + " exceeds parent width");
            Assertions.assertTrue(bounds.y + bounds.height <= parent.getHeight(), child.getClass().getSimpleName() + " exceeds parent height");
        }
    }

    private static <T> T getField(Object target, String name, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
