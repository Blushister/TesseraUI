package com.tesseraui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TesseraVirtualListTest {

    @Test
    void mouseScrolled_withCoordinates_onlyConsumesWhenScrollChanges() {
        TesseraVirtualList list = TesseraVirtualList.of(items(10), 10,
                model -> new TesseraLabel(0, 0, 80, 10, model.resolve("name")));
        list.setPosition(0, 0);
        list.setSize(100, 30);

        assertFalse(list.mouseScrolled(150, 5, -1), "outside bounds should not consume");
        assertFalse(list.mouseScrolled(5, 5, 1), "at top, scrolling upward should not consume");

        assertTrue(list.mouseScrolled(5, 5, -1), "scrolling down should move the list");
        assertEquals(10, list.getScrollY());
    }

    private static List<TesseraModel> items(int count) {
        List<TesseraModel> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = i;
            out.add(key -> "name".equals(key) ? "Row " + index : null);
        }
        return out;
    }
}
