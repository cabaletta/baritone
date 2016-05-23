package com.github.btrekkie.tree_list.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.junit.Test;

import com.github.btrekkie.tree_list.TreeList;

public class TreeListTest {
    /** Tests TreeList.add. */
    @Test
    public void testAdd() {
        List<Integer> list = new TreeList<Integer>();
        assertEquals(Collections.emptyList(), list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());

        list.add(4);
        list.add(17);
        list.add(-4);
        list.add(null);
        assertEquals(Arrays.asList(4, 17, -4, null), list);
        assertEquals(4, list.size());
        assertFalse(list.isEmpty());
        assertEquals(-4, list.get(2).intValue());

        boolean threwException;
        try {
            list.get(-3);
            threwException = false;
        } catch (IndexOutOfBoundsException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        try {
            list.get(9);
            threwException = false;
        } catch (IndexOutOfBoundsException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        try {
            list.add(-3, 5);
            threwException = false;
        } catch (IndexOutOfBoundsException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        try {
            list.add(9, 10);
            threwException = false;
        } catch (IndexOutOfBoundsException exception) {
            threwException = true;
        }
        assertTrue(threwException);

        list.add(1, 6);
        list.add(0, -1);
        list.add(6, 42);
        assertEquals(Arrays.asList(-1, 4, 6, 17, -4, null, 42), list);
        assertEquals(7, list.size());
        assertEquals(6, list.get(2).intValue());
        assertEquals(null, list.get(5));

        list = new TreeList<Integer>();
        for (int i = 0; i < 200; i++) {
            list.add(i + 300);
        }
        for (int i = 0; i < 300; i++) {
            list.add(i, i);
        }
        for (int i = 499; i >= 0; i--) {
            list.add(500, i + 500);
        }
        List<Integer> expected = new ArrayList<Integer>(1000);
        for (int i = 0; i < 1000; i++) {
            expected.add(i);
        }
        assertEquals(expected, list);
        assertEquals(1000, list.size());
        assertFalse(list.isEmpty());
        assertEquals(777, list.get(777).intValue());
        assertEquals(123, list.get(123).intValue());
        assertEquals(0, list.get(0).intValue());
        assertEquals(999, list.get(999).intValue());
    }

    /** Tests TreeList.remove. */
    @Test
    public void testRemove() {
        List<Integer> list = new TreeList<Integer>();
        list.add(17);
        list.add(-5);
        list.add(null);
        list.add(0);
        list.add(1, 16);
        assertEquals(null, list.remove(3));
        assertEquals(17, list.remove(0).intValue());
        assertEquals(0, list.remove(2).intValue());
        assertEquals(Arrays.asList(16, -5), list);
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());
        assertEquals(16, list.get(0).intValue());
        assertEquals(-5, list.get(1).intValue());

        boolean threwException;
        try {
            list.remove(-3);
            threwException = false;
        } catch (IndexOutOfBoundsException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        try {
            list.remove(9);
            threwException = false;
        } catch (IndexOutOfBoundsException exception) {
            threwException = true;
        }
        assertTrue(threwException);

        list = new TreeList<Integer>();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        for (int i = 0; i < 250; i++) {
            assertEquals(2 * i + 501, list.remove(501).intValue());
            assertEquals(2 * i + 1, list.remove(i + 1).intValue());
        }
        List<Integer> expected = new ArrayList<Integer>(500);
        for (int i = 0; i < 500; i++) {
            expected.add(2 * i);
        }
        assertEquals(expected, list);
        assertEquals(500, list.size());
        assertFalse(list.isEmpty());
        assertEquals(0, list.get(0).intValue());
        assertEquals(998, list.get(499).intValue());
        assertEquals(84, list.get(42).intValue());
        assertEquals(602, list.get(301).intValue());
    }

    /** Tests TreeList.set.*/
    @Test
    public void testSet() {
        List<Integer> list = new TreeList<Integer>();
        list.addAll(Arrays.asList(5, 17, 42, -6, null, 3, null));
        list.set(3, 12);
        list.set(0, 6);
        list.set(6, 88);
        boolean threwException;
        try {
            list.set(7, 2);
            threwException = false;
        } catch (IndexOutOfBoundsException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        try {
            list.set(-1, 4);
            threwException = false;
        } catch (IndexOutOfBoundsException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        assertEquals(Arrays.asList(6, 17, 42, 12, null, 3, 88), list);
        assertEquals(7, list.size());
        assertFalse(list.isEmpty());
        assertEquals(42, list.get(2).intValue());
        assertEquals(6, list.get(0).intValue());
        assertEquals(88, list.get(6).intValue());

        list = new TreeList<Integer>();
        for (int i = 0; i < 1000; i++) {
            list.add(999 - i);
        }
        for (int i = 0; i < 500; i++) {
            list.set(i, i);
            list.set(i + 500, i + 500);
        }
        List<Integer> expected = new ArrayList<Integer>(1000);
        for (int i = 0; i < 1000; i++) {
            expected.add(i);
        }
        assertEquals(expected, list);
        assertEquals(1000, list.size());
        assertFalse(list.isEmpty());
        assertEquals(123, list.get(123).intValue());
        assertEquals(777, list.get(777).intValue());
        assertEquals(0, list.get(0).intValue());
        assertEquals(999, list.get(999).intValue());
    }

    /** Tests TreeList.addAll. */
    @Test
    public void testAddAll() {
        List<Integer> list = new TreeList<Integer>();
        list.add(3);
        list.add(42);
        list.add(16);
        list.addAll(0, Arrays.asList(15, 4, -1));
        list.addAll(2, Arrays.asList(6, 14));
        list.addAll(1, Collections.<Integer>emptyList());
        list.addAll(8, Arrays.asList(null, 7));
        list.addAll(Arrays.asList(-5, 5));
        assertEquals(Arrays.asList(15, 4, 6, 14, -1, 3, 42, 16, null, 7, -5, 5), list);
        assertEquals(12, list.size());
        assertFalse(list.isEmpty());
        assertEquals(14, list.get(3).intValue());
        assertEquals(15, list.get(0).intValue());
        assertEquals(5, list.get(11).intValue());

        list = new TreeList<Integer>();
        List<Integer> list2 = new ArrayList<Integer>(400);
        for (int i = 0; i < 200; i++) {
            list2.add(i + 100);
        }
        for (int i = 0; i < 200; i++) {
            list2.add(i + 700);
        }
        list.addAll(list2);
        list2.clear();
        for (int i = 0; i < 100; i++) {
            list2.add(i);
        }
        list.addAll(0, list2);
        list2.clear();
        for (int i = 0; i < 400; i++) {
            list2.add(i + 300);
        }
        list.addAll(300, list2);
        list2.clear();
        for (int i = 0; i < 100; i++) {
            list2.add(i + 900);
        }
        list.addAll(900, list2);
        List<Integer> expected = new ArrayList<Integer>(1000);
        for (int i = 0; i < 1000; i++) {
            expected.add(i);
        }
        assertEquals(expected, list);
        assertEquals(1000, list.size());
        assertFalse(list.isEmpty());
        assertEquals(123, list.get(123).intValue());
        assertEquals(777, list.get(777).intValue());
        assertEquals(0, list.get(0).intValue());
        assertEquals(999, list.get(999).intValue());
    }

    /** Tests TreeList.subList.clear(). */
    @Test
    public void testClearSubList() {
        List<Integer> list = new TreeList<Integer>();
        list.addAll(Arrays.asList(6, 42, -3, 15, 7, 99, 6, 12));
        list.subList(2, 4).clear();
        list.subList(0, 1).clear();
        list.subList(4, 4).clear();
        list.subList(3, 5).clear();
        assertEquals(Arrays.asList(42, 7, 99), list);
        assertEquals(3, list.size());
        assertFalse(list.isEmpty());
        assertEquals(42, list.get(0).intValue());
        assertEquals(7, list.get(1).intValue());
        assertEquals(99, list.get(2).intValue());

        list = new TreeList<Integer>();
        for (int i = 0; i < 200; i++) {
            list.add(-1);
        }
        for (int i = 0; i < 500; i++) {
            list.add(i);
        }
        for (int i = 0; i < 400; i++) {
            list.add(-1);
        }
        for (int i = 0; i < 500; i++) {
            list.add(i + 500);
        }
        for (int i = 0; i < 600; i++) {
            list.add(-1);
        }
        list.subList(1600, 2200).clear();
        list.subList(777, 777).clear();
        list.subList(700, 1100).clear();
        list.subList(0, 200).clear();
        List<Integer> expected = new ArrayList<Integer>(1000);
        for (int i = 0; i < 1000; i++) {
            expected.add(i);
        }
        assertEquals(expected, list);
        assertEquals(1000, list.size());
        assertFalse(list.isEmpty());
        assertEquals(123, list.get(123).intValue());
        assertEquals(777, list.get(777).intValue());
        assertEquals(0, list.get(0).intValue());
        assertEquals(999, list.get(999).intValue());

        list = new TreeList<Integer>();
        list.addAll(Arrays.asList(-3, null, 8, 14, 9, 42));
        list.subList(0, 6).clear();
        assertEquals(Collections.emptyList(), list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    /** Tests TreeList(Collection). */
    @Test
    public void testConstructor() {
        List<Integer> list = new TreeList<Integer>(Arrays.asList(1, 5, 42, -6, null, 3));
        assertEquals(Arrays.asList(1, 5, 42, -6, null, 3), list);
        assertEquals(6, list.size());
        assertFalse(list.isEmpty());
        assertEquals(42, list.get(2).intValue());
        assertEquals(1, list.get(0).intValue());
        assertEquals(3, list.get(5).intValue());

        List<Integer> expected = new ArrayList<Integer>(1000);
        for (int i = 0; i < 1000; i++) {
            expected.add(i);
        }
        list = new TreeList<Integer>(expected);
        assertEquals(expected, list);
        assertEquals(1000, list.size());
        assertFalse(list.isEmpty());
        assertEquals(123, list.get(123).intValue());
        assertEquals(777, list.get(777).intValue());
        assertEquals(0, list.get(0).intValue());
        assertEquals(999, list.get(999).intValue());
    }

    /** Tests TreeList.clear(). */
    @Test
    public void testClear() {
        List<Integer> list = new TreeList<Integer>();
        list.addAll(Arrays.asList(7, 16, 5, 42));
        list.clear();
        assertEquals(Collections.emptyList(), list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());

        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        list.clear();
        assertEquals(Collections.emptyList(), list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    /** Tests TreeList.iterator(). */
    @Test
    public void testIterator() {
        List<Integer> list = new TreeList<Integer>();
        Iterator<Integer> iterator = list.iterator();
        assertFalse(iterator.hasNext());
        boolean threwException;
        try {
            iterator.next();
            threwException = false;
        } catch (NoSuchElementException exception) {
            threwException = true;
        }
        assertTrue(threwException);

        list = new TreeList<Integer>(Arrays.asList(42, 16, null, 7, 8, 3, 12));
        iterator = list.iterator();
        try {
            iterator.remove();
            threwException = false;
        } catch (IllegalStateException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        assertTrue(iterator.hasNext());
        assertEquals(42, iterator.next().intValue());
        assertEquals(16, iterator.next().intValue());
        assertEquals(null, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(7, iterator.next().intValue());
        iterator.remove();
        try {
            iterator.remove();
            threwException = false;
        } catch (IllegalStateException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertEquals(8, iterator.next().intValue());
        assertTrue(iterator.hasNext());
        assertEquals(3, iterator.next().intValue());
        assertTrue(iterator.hasNext());
        assertEquals(12, iterator.next().intValue());
        assertFalse(iterator.hasNext());
        iterator.remove();
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            threwException = false;
        } catch (NoSuchElementException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        assertEquals(Arrays.asList(42, 16, null, 8, 3), list);

        list = new TreeList<Integer>();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        iterator = list.iterator();
        for (int i = 0; i < 500; i++) {
            assertTrue(iterator.hasNext());
            assertEquals(2 * i, iterator.next().intValue());
            assertTrue(iterator.hasNext());
            assertEquals(2 * i + 1, iterator.next().intValue());
            iterator.remove();
        }
        assertFalse(iterator.hasNext());
        List<Integer> expected = new ArrayList<Integer>(500);
        for (int i = 0; i < 500; i++) {
            expected.add(2 * i);
        }
        assertEquals(expected, list);
    }

    /** Tests TreeList.listIterator. */
    @Test
    public void testListIterator() {
        List<Integer> list = new TreeList<Integer>();
        list.addAll(Arrays.asList(7, 16, 42, -3, 12, 25, 8, 9));
        boolean threwException;
        try {
            list.listIterator(-1);
            threwException = false;
        } catch (IndexOutOfBoundsException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        try {
            list.listIterator(18);
            threwException = false;
        } catch (IndexOutOfBoundsException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        ListIterator<Integer> iterator = list.listIterator(1);
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasPrevious());
        assertEquals(16, iterator.next().intValue());
        iterator.add(6);
        try {
            iterator.set(-1);
            threwException = false;
        } catch (IllegalStateException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        try {
            iterator.add(9);
            threwException = false;
        } catch (IllegalStateException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        try {
            iterator.remove();
            threwException = false;
        } catch (IllegalStateException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        assertEquals(6, iterator.previous().intValue());
        assertEquals(6, iterator.next().intValue());
        assertEquals(2, iterator.previousIndex());
        assertEquals(3, iterator.nextIndex());
        assertEquals(42, iterator.next().intValue());
        assertEquals(-3, iterator.next().intValue());
        assertEquals(12, iterator.next().intValue());
        iterator.remove();
        assertEquals(-3, iterator.previous().intValue());
        iterator.remove();
        assertEquals(25, iterator.next().intValue());
        iterator.set(14);
        assertEquals(8, iterator.next().intValue());
        assertEquals(9, iterator.next().intValue());
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            threwException = false;
        } catch (NoSuchElementException exception) {
            threwException = true;
        }
        assertTrue(threwException);
        assertEquals(9, iterator.previous().intValue());
        iterator.set(10);
        assertEquals(Arrays.asList(7, 16, 6, 42, 14, 8, 10), list);
        assertEquals(7, list.size());
        assertFalse(list.isEmpty());
        assertEquals(42, list.get(3).intValue());
        assertEquals(7, list.get(0).intValue());
        assertEquals(10, list.get(6).intValue());

        list = new TreeList<Integer>();
        for (int i = 0; i < 1000; i++) {
            list.add(-1);
        }
        iterator = list.listIterator();
        for (int i = 0; i < 500; i++) {
            iterator.add(i);
            iterator.next();
        }
        for (int i = 0; i < 500; i++) {
            iterator.previous();
            iterator.remove();
            iterator.previous();
        }
        iterator = list.listIterator(500);
        for (int i = 0; i < 250; i++) {
            iterator.next();
            iterator.set(2 * i + 500);
            iterator.next();
        }
        for (int i = 0; i < 250; i++) {
            iterator.previous();
            iterator.set(999 - 2 * i);
            iterator.previous();
        }
        List<Integer> expected = new ArrayList<Integer>(1000);
        for (int i = 0; i < 1000; i++) {
            expected.add(i);
        }
        assertEquals(expected, list);
        assertEquals(1000, list.size());
        assertFalse(list.isEmpty());
        assertEquals(123, list.get(123).intValue());
        assertEquals(777, list.get(777).intValue());
        assertEquals(0, list.get(0).intValue());
        assertEquals(999, list.get(999).intValue());
    }
}
