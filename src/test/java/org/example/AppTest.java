package org.example;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit test for simple App.
 */
public class AppTest {
    private LRUCache<Integer, Integer> c;

    public AppTest() {
        this.c = new LRUCache<>(2, 5000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testCacheStartsEmpty() {
        assertNull(c.get(1));
    }

    @Test
    public void testSetBelowCapacity() {
        c.put(1, 1);
        assertEquals(c.get(1), 1);
        assertNull(c.get(2));
        c.put(2, 4);
        assertEquals(c.get(1), 1);
        assertEquals(c.get(2), 4);
    }

    @Test
    public void testCapacityReachedOldestRemoved() {
        c.put(1, 1);
        c.put(2, 4);
        c.put(3, 9);
        assertNull(c.get(1));
        assertEquals(c.get(2), 4);
        assertEquals(c.get(3), 9);
    }

    @Test
    public void testGetRenewsEntry() {
        c.put(1, 1);
        c.put(2, 4);
        assertEquals(c.get(1), 1);
        c.put(3, 9);
        assertEquals(c.get(1), 1);
        assertNull(c.get(2));
        assertEquals(c.get(3), 9);
    }

    @Test
    public void testExpiration() {
        c.put(1, 1);
        c.put(2, 4);
        c.put(3, 9);
        assertNull(c.get(1));
        try {
            Thread.sleep(4000);
            assertEquals(c.get(2), 4);
            Thread.sleep(2000);
            assertNull(c.get(3));
            assertEquals(c.get(2), 4);
            Thread.sleep(6000);
            assertNull(c.get(2));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


}
