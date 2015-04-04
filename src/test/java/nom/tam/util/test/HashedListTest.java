package nom.tam.util.test;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 2004 - 2015 nom-tam-fits
 * %%
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * #L%
 */

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import nom.tam.util.Cursor;
import nom.tam.util.HashedList;

import org.junit.Test;

/**
 * This class tests and illustrates the use of the HashedList class. Tests are
 * in three parts.
 * <p>
 * The first section (in testCollection) tests the methods that are present in
 * the Collection interface. All of the optional methods of that interface are
 * supported. This involves tests of the HashedClass interface directly.
 * <p>
 * The second set of tests uses the Iterator (in testIterator) returned by the
 * iterator() method and tests the standard Iterator methods to display and
 * remove rows from the HashedList.
 * <p>
 * The third set of tests (in testCursor) tests the extended capabilities of the
 * HashedListIterator to add rows to the table, and to work as a cursor to move
 * in a non-linear fashion through the list.
 */
public class HashedListTest {

    @Test
    public void testCollection() {

        HashedList<Object, Object> h1 = new HashedList<Object, Object>() {

            @Override
            protected Object intToKey(int i) {
                return Integer.valueOf(i);
            }

            @Override
            protected Object valueToKey(Object value) {
                return value;
            }
        };
        HashedList<Object, Object> h2 = new HashedList<Object, Object>() {

            @Override
            protected Object intToKey(int i) {
                return Integer.valueOf(i);
            }

            @Override
            protected Object valueToKey(Object value) {
                return value;
            }

        };
        Cursor i = h1.iterator(0);
        Iterator j;

        // Add a few unkeyed rows.

        h1.add("Row 1");
        h1.add("Row 2");
        h1.add("Row 3");

        assertEquals("Adding unkeyed rows", 3, h1.size());

        assertEquals("Has row 1", true, h1.contains("Row 1"));
        assertEquals("Has row 2", true, h1.contains("Row 2"));
        h1.remove("Row 2");
        assertEquals("Has row 1", true, h1.contains("Row 1"));
        assertEquals("Has row 2", false, h1.contains("Row 2"));

        assertEquals("Delete unkeyed rows", 2, h1.size());

        h1.clear();
        assertEquals("Cleared unkeyed rows", 0, h1.size());

        h1.add("key 1", "Row 1");
        h1.add("key 2", "Row 2");
        h1.add("key 3", "Row 3");

        assertEquals("Adding keyed rows", 3, h1.size());

        assertEquals("Has Row 1", true, h1.contains("Row 1"));
        assertEquals("Has key 1", true, h1.containsKey("key 1"));
        assertEquals("Has Row 2", true, h1.contains("Row 2"));
        assertEquals("Has key 2", true, h1.containsKey("key 2"));
        assertEquals("Has Row 3", true, h1.contains("Row 3"));
        assertEquals("Has key 3", true, h1.containsKey("key 3"));

        h1.removeKey("key 2");
        assertEquals("Delete keyed row", 2, h1.size());
        assertEquals("Has Row 1", true, h1.contains("Row 1"));
        assertEquals("Has key 1", true, h1.containsKey("key 1"));
        assertEquals("Has Row 2", false, h1.contains("Row 2"));
        assertEquals("Has key 2", false, h1.containsKey("key 2"));
        assertEquals("Has Row 3", true, h1.contains("Row 3"));
        assertEquals("Has key 3", true, h1.containsKey("key 3"));

        h1.clear();
        assertEquals("Clear keyed rows", 0, h1.size());

        h1.add("key 1", "Row 1");
        h1.add("key 2", "Row 2");
        h1.add("key 3", "Row 3");
        assertEquals("Re-Adding keyed rows", 3, h1.size());
        assertEquals("Has Row 2", true, h1.contains("Row 2"));
        assertEquals("Has key 2", true, h1.containsKey("key 2"));

        h2.add("key 4", "Row 4");
        h2.add("key 5", "Row 5");

        assertEquals("containsAll(beforeAdd)", false, h1.containsAll(h2));

        h1.addAll(h2);

        assertEquals("addAll()", 5, h1.size());
        assertEquals("containsAll(afterAdd)", true, h1.containsAll(h2));
        assertEquals("has row 4", true, h1.contains("Row 4"));
        h1.remove("Row 4");
        assertEquals("dropped row 4", false, h1.contains("Row 4"));
        assertEquals("containsAll(afterDrop)", false, h1.containsAll(h2));

        assertEquals("isEmpty(false)", false, h1.isEmpty());
        h1.remove("Row 1");
        h1.remove("Row 2");
        h1.remove("Row 3");
        h1.remove("Row 5");
        assertEquals("isEmpty(true)", true, h1.isEmpty());
        h1.add("Row 1");
        h1.add("Row 2");
        h1.add("Row 3");
        h1.addAll(h2);
        assertEquals("Adding back", 5, h1.size());
        h1.removeAll(h2);

        assertEquals("removeAll()", 3, h1.size());
        h1.addAll(h2);

        assertEquals("Adding back again", 5, h1.size());
        h1.retainAll(h2);
        assertEquals("retainAll()", 2, h1.size());

    }

    @Test
    public void testIterator() {

        HashedList<Object, Object> h1 = new HashedList<Object, Object>() {

            @Override
            protected Object intToKey(int i) {
                return Integer.valueOf(i);
            }

            @Override
            protected Object valueToKey(Object value) {
                return value;
            }
        };

        h1.add("key 4", "Row 4");
        h1.add("key 5", "Row 5");

        Iterator j = h1.iterator();
        assertEquals("next1", true, j.hasNext());
        assertEquals("TestIter1", "Row 4", (String) j.next());
        assertEquals("next2", true, j.hasNext());
        assertEquals("TestIter2", "Row 5", (String) j.next());
        assertEquals("next3", false, j.hasNext());

        h1.clear();

        h1.add("key 1", "Row 1");
        h1.add("key 2", "Row 2");
        h1.add("Row 3");
        h1.add("key 4", "Row 4");
        h1.add("Row 5");

        assertEquals("Before remove", true, h1.contains("Row 2"));
        j = h1.iterator();
        j.next();
        j.next();
        j.remove(); // Should get rid of second row
        assertEquals("After remove", false, h1.contains("Row 2"));
        assertEquals("n3", true, j.hasNext());
        assertEquals("n3v", "Row 3", (String) j.next());
        assertEquals("n4", true, j.hasNext());
        assertEquals("n4v", "Row 4", (String) j.next());
        assertEquals("n5", true, j.hasNext());
        assertEquals("n5v", "Row 5", (String) j.next());
        assertEquals("n6", false, j.hasNext());
    }

    @Test
    public void TestCursor() {
        HashedList<Object, Object> h1 = new HashedList<Object, Object>() {

            @Override
            protected Object intToKey(int i) {
                return Integer.valueOf(i);
            }

            @Override
            protected Object valueToKey(Object value) {
                return value;
            }
        };

        h1.add("key 1", "Row 1");
        h1.add("Row 3");
        h1.add("key 4", "Row 4");
        h1.add("Row 5");

        Cursor j = (Cursor) h1.iterator(0);
        assertEquals("n1x", true, j.hasNext());
        assertEquals("n1xv", "Row 1", (String) j.next());
        assertEquals("n1xv", "Row 3", (String) j.next());

        assertEquals("No Row 2", false, h1.containsKey("key 2"));
        assertEquals("No Row 2", false, h1.contains("Row 2"));
        j.setKey("key 1");
        assertEquals("setKey()", "Row 1", (String) j.next());
        j.add("key 2", "Row 2");
        assertEquals("has Row 2", true, h1.contains("Row 2"));
        assertEquals("after add", "Row 3", (String) j.next());

        j.setKey("key 4");
        assertEquals("setKey(1)", "Row 4", (String) j.next());
        assertEquals("setKey(2)", "Row 5", (String) j.next());
        assertEquals("setKey(3)", false, j.hasNext());

        j.setKey("key 2");
        assertEquals("setKey(4)", "Row 2", (String) j.next());
        assertEquals("setKey(5)", "Row 3", (String) j.next());
        j.add("Row 3.5");
        j.add("Row 3.6");
        assertEquals("After add", 7, h1.size());

        j = h1.iterator("key 2");
        j.add("Row 1.5");
        j.add("key 1.7", "Row 1.7");
        j.add("Row 1.9");
        assertEquals("next() after adds", "Row 2", (String) j.next());
        j.setKey("key 1.7");
        assertEquals("next() after adds", "Row 1.7", (String) j.next());
        assertEquals("prev(1)", "Row 1.7", (String) j.prev());
        assertEquals("prev(2)", "Row 1.5", (String) j.prev());
        assertEquals("prev(3)", true, j.hasPrev());
        assertEquals("prev(4)", "Row 1", (String) j.prev());
        assertEquals("prev(5)", false, j.hasPrev());
    }

    void show(HashedList h, String msg) {
        Iterator t = h.iterator();
        System.out.println("\n Looking at list:" + msg);
        while (t.hasNext()) {
            System.out.println("Has element:" + t.next());
        }
    }
}
