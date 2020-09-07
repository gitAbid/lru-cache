package org.example;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {

        // 1. initiate the cache with capacity 10
        LoadingCache<String, String> cache = LoadingCache.builder()
                .withMaxCapacity(10)
                .withExpiration(5000L)
                .withCacheLoader(key -> "val-" + key)
                .withRemovalListener(key -> System.out.println("Removed Key " + key))
                .build();
// 2. insert 10 objects to cache
        for (int i = 1; i <= 10; i++) {
            cache.put(String.format("key-%d", i), String.format("value-%d", i));
        }
// 3. print the cache objects
        System.out.println("printing cache:");
        cache.printCache();
        System.out.println("Stats: " + cache.getCacheStatistics());
// 4. access the first object and print the cache
        cache.get("key-1");
        System.out.println("printing cache after accessing key-1:");
        cache.printCache();
        System.out.println("Stats: " + cache.getCacheStatistics());
        for (int i = 11; i <= 20; i++) {
            cache.put(String.format("key-%d", i), String.format("value-%d", i));
        }
        cache.printCache();
        System.out.println("Stats: " + cache.getCacheStatistics());


        try {
            System.out.println("printing cache after getting new element:" + cache.get("key-1000"));
            cache.printCache();
            System.out.println("Stats: " + cache.getCacheStatistics());
            System.out.println("Sleeping 4000");
            Thread.sleep(4000);
            cache.get("key-12");
            System.out.println("printing cache after accessing key-12:");
            cache.printCache();
            System.out.println("Stats: " + cache.getCacheStatistics());
            cache.get("key-1");
            System.out.println("printing cache after accessing key-1:");
            cache.printCache();
            System.out.println("Stats: " + cache.getCacheStatistics());
            cache.get("key-13");
            System.out.println("printing cache after accessing key-13:");
            cache.printCache();
            System.out.println("Stats: " + cache.getCacheStatistics());
            System.out.println("Sleeping 3000");
            Thread.sleep(3000);
            cache.put("key-100", "value-100");
            System.out.println("printing cache after accessing key-100:");
            cache.printCache();
            System.out.println("Stats: " + cache.getCacheStatistics());
            cache.put("key-200", "value-200");
            System.out.println("printing cache after accessing key-200:");
            cache.printCache();
            System.out.println("Stats: " + cache.getCacheStatistics());
            System.out.println("Sleeping 6000");
            Thread.sleep(6000);
            System.out.println("printing cache after final access:" + cache.get("key-100"));
            cache.printCache();
            System.out.println("Stats: " + cache.getCacheStatistics());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("printing cache after resetting cache:");
        cache.resetCache();
        cache.printCache();

    }
}
