package org.example;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        // 1. initiate the cache with capacity 10
        LruCache<String, String> cache = new LruCache<String, String>(10, 5000);
// 2. insert 10 objects to cache
        for (int i = 1; i <= 10; i++) {
            cache.put(String.format("key-%d", i), String.format("value-%d", i));
        }
// 3. print the cache objects
        System.out.println("printing cache:");
        cache.printCache();
// 4. access the first object and print the cache
        cache.get("key-1");
        System.out.println("printing cache after accessing key-1:");
        cache.printCache();


        try {
            Thread.sleep(10000);
            cache.put(String.format("key-%d", 200), String.format("value-%d", 200));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}