/*
 * This file was originally written by btrekkie under the MIT license, which is compatible with the LGPL license for this usage within Baritone
 * https://github.com/btrekkie/RedBlackNode/
 */

package com.github.btrekkie.arbitrary_order_collection.test;

import com.github.btrekkie.arbitrary_order_collection.ArbitraryOrderCollection;
import com.github.btrekkie.arbitrary_order_collection.ArbitraryOrderValue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArbitraryOrderCollectionTest {
    /**
     * Tests ArbitraryOrderCollection.
     */
    @Test
    public void test() {
        ArbitraryOrderCollection collection = new ArbitraryOrderCollection();
        List<ArbitraryOrderValue> values1 = new ArrayList<ArbitraryOrderValue>(5);
        ArbitraryOrderValue value = collection.createValue();
        assertEquals(0, value.compareTo(value));
        values1.add(value);
        for (int i = 0; i < 4; i++) {
            values1.add(collection.createValue());
        }
        Collections.sort(values1);
        List<ArbitraryOrderValue> values2 = new ArrayList<ArbitraryOrderValue>(10);
        for (int i = 0; i < 10; i++) {
            value = collection.createValue();
            values2.add(value);
        }
        for (int i = 0; i < 5; i++) {
            collection.remove(values2.get(2 * i));
        }
        assertEquals(0, values1.get(0).compareTo(values1.get(0)));
        assertTrue(values1.get(0).compareTo(values1.get(1)) < 0);
        assertTrue(values1.get(1).compareTo(values1.get(0)) > 0);
        assertTrue(values1.get(4).compareTo(values1.get(2)) > 0);
        assertTrue(values1.get(0).compareTo(values1.get(4)) < 0);

        collection = new ArbitraryOrderCollection();
        values1 = new ArrayList<ArbitraryOrderValue>(1000);
        for (int i = 0; i < 1000; i++) {
            value = collection.createValue();
            values1.add(value);
        }
        for (int i = 0; i < 500; i++) {
            collection.remove(values1.get(2 * i));
        }
        values2 = new ArrayList<ArbitraryOrderValue>(500);
        for (int i = 0; i < 500; i++) {
            values2.add(values1.get(2 * i + 1));
        }
        for (int i = 0; i < 500; i++) {
            values2.get(0).compareTo(values2.get(i));
        }
        Collections.sort(values2);
        for (int i = 0; i < 500; i++) {
            collection.createValue();
        }
        for (int i = 0; i < 499; i++) {
            assertTrue(values2.get(i).compareTo(values2.get(i + 1)) < 0);
            assertTrue(values2.get(i + 1).compareTo(values2.get(i)) > 0);
        }
        for (int i = 1; i < 500; i++) {
            assertEquals(0, values2.get(i).compareTo(values2.get(i)));
            assertTrue(values2.get(0).compareTo(values2.get(i)) < 0);
            assertTrue(values2.get(i).compareTo(values2.get(0)) > 0);
        }
    }
}
