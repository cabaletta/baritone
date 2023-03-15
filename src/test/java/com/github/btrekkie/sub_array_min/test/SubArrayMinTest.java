package com.github.btrekkie.sub_array_min.test;

import com.github.btrekkie.sub_array_min.SubArrayMin;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SubArrayMinTest {
    /**
     * Tests SubArrayMin.
     */
    @Test
    public void test() {
        SubArrayMin sam = new SubArrayMin();
        sam.add(12);
        sam.add(42);
        sam.add(-3);
        sam.add(16);
        sam.add(5);
        sam.add(8);
        sam.add(4);
        assertEquals(-3, sam.min(0, 7));
        assertEquals(12, sam.min(0, 2));
        assertEquals(-3, sam.min(2, 4));
        assertEquals(12, sam.min(0, 1));
        assertEquals(5, sam.min(3, 6));
        assertEquals(4, sam.min(4, 7));

        sam = new SubArrayMin();
        for (int i = 0; i < 1000; i++) {
            sam.add(-Integer.bitCount(i));
        }
        assertEquals(0, sam.min(0, 1));
        assertEquals(-4, sam.min(0, 30));
        assertEquals(-9, sam.min(0, 1000));
        assertEquals(-9, sam.min(123, 777));
        assertEquals(-8, sam.min(777, 888));
        assertEquals(-6, sam.min(777, 788));
        assertEquals(-9, sam.min(900, 1000));
    }
}
