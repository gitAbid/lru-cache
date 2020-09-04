package org.example;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit test for simple App.
 */
public class AppTest {
    private LRUCache<Integer, Integer> cache;

    public AppTest() {
        this.cache = new LRUCache<>(2, 5000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testCacheStartsEmpty() {
        assertNull(cache.get(1));
    }

    @Test
    public void testSetBelowCapacity() {
        cache.put(1, 1);
        assertEquals(cache.get(1), 1);
        assertNull(cache.get(2));
        cache.put(2, 4);
        assertEquals(cache.get(1), 1);
        assertEquals(cache.get(2), 4);
    }

    @Test
    public void testCapacityReachedOldestRemoved() {
        cache.put(1, 1);
        cache.put(2, 4);
        cache.put(3, 9);
        assertNull(cache.get(1));
        assertEquals(cache.get(2), 4);
        assertEquals(cache.get(3), 9);
    }

    @Test
    public void testGetRenewsEntry() {
        cache.put(1, 1);
        cache.put(2, 4);
        assertEquals(cache.get(1), 1);
        cache.put(3, 9);
        assertEquals(cache.get(1), 1);
        assertNull(cache.get(2));
        assertEquals(cache.get(3), 9);
    }

    @Test
    public void testExpiration() {
        cache.put(1, 1);
        cache.put(2, 4);
        cache.put(3, 9);
        assertNull(cache.get(1));
        try {
            Thread.sleep(4000);
            assertEquals(cache.get(2), 4);
            Thread.sleep(2000);
            assertNull(cache.get(3));
            assertEquals(cache.get(2), 4);
            Thread.sleep(6000);
            assertNull(cache.get(2));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    @Test
    public void testMultiThreadedPut() {
        cache = new LRUCache<>(2, 5000, TimeUnit.MILLISECONDS);
        cache.put(1, 1);
        cache.put(2, 4);
        Thread t1 = new Thread(() -> {
            System.out.println("Thread ->[3,9] ->" + Thread.currentThread());
            cache.put(3, 9);
        });

        Thread t2 = new Thread(() -> {
            System.out.println("Thread ->[4,16] ->" + Thread.currentThread());
            cache.put(4, 16);

        });
        t2.start();
        t1.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNull(cache.get(1));
        assertNull(cache.get(2));
        assertEquals(cache.get(4), 16);
        assertEquals(cache.get(3), 9);

    }

    @Test
    public void testMultiThreadedGet() {
        cache.put(1, 1);
        cache.put(2, 4);
        Thread t1 = new Thread(() -> {
            System.out.println("Thread ->[2]" + Thread.currentThread() + "Value -> " + cache.get(2));
        });

        Thread t2 = new Thread(() -> {
            System.out.println("Thread ->[1]" + Thread.currentThread() + "Value -> " + cache.get(1));
        });
        t2.start();
        t1.start();
        try {
            Thread.sleep(100);
            cache.printCache();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


}
