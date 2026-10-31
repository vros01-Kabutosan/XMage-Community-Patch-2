package mage.client.cards;

import mage.cards.repository.CardInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

public class DragCardGridTest {

    @Test
    void testPrintingSelectorNumberSorting() throws Exception {
        DragCardGrid grid = new DragCardGrid();
        Method sortMethod = DragCardGrid.class.getDeclaredMethod("xcpPrintingSelectorNumberSort", CardInfo.class);
        sortMethod.setAccessible(true);

        Method fastSortMethod = DragCardGrid.class.getDeclaredMethod("xcpPrintingSelectorFastNumberSort", CardInfo.class);
        fastSortMethod.setAccessible(true);

        Assertions.assertEquals(Integer.MAX_VALUE, sortMethod.invoke(grid, (CardInfo) null));
        Assertions.assertEquals(Integer.MAX_VALUE, fastSortMethod.invoke(grid, (CardInfo) null));
    }

    @Test
    void testQuickLandsConfiguration() throws Exception {
        java.lang.reflect.Field field = DragCardGrid.class.getDeclaredField("QUICK_LANDS");
        field.setAccessible(true);
        String[][] lands = (String[][]) field.get(null);
        Assertions.assertNotNull(lands);
        Assertions.assertEquals(6, lands.length);
        Assertions.assertEquals("Plains", lands[0][1]);
        Assertions.assertEquals("Island", lands[1][1]);
        Assertions.assertEquals("Swamp", lands[2][1]);
        Assertions.assertEquals("Mountain", lands[3][1]);
        Assertions.assertEquals("Forest", lands[4][1]);
        Assertions.assertEquals("Wastes", lands[5][1]);
    }

    @Test
    void testDragCardGridSelectionAndParentArea() throws Exception {
        DragCardGrid grid = new DragCardGrid();
        grid.selectAll();
        Assertions.assertTrue(grid.dragCardList().isEmpty());
        grid.deselectAll();
        Assertions.assertTrue(grid.dragCardList().isEmpty());

        Method findAreaMethod = DragCardGrid.class.getDeclaredMethod("findParentDeckArea");
        findAreaMethod.setAccessible(true);
        Assertions.assertNull(findAreaMethod.invoke(grid));
    }
}
