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
}
