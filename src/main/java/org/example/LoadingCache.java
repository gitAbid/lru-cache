package org.example;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LoadingCache<K, V> {
    public static final Logger logger = Logger.getLogger(LoadingCache.class);
    private final HashMap<K, Node<K, V>> map;
    private final ReentrantLock lock = new ReentrantLock();
    private final int maxCapacity;
    private final long expiration;
    private final CacheLoader<K, V> cacheLoader;
    private final DoublyLinkedList<K, V> doublyLinkedList;
    private final CacheStatistics cacheStatistics;
    private final RemovalListener<K> removalListener;

    private LoadingCache(Builder<K, V> builder) {
        BasicConfigurator.configure();
        maxCapacity = builder.maxCapacity;
        expiration = builder.expiration;
        cacheLoader = builder.cacheLoader;
        removalListener = builder.removalListener;
        map = new HashMap<>(maxCapacity);
        doublyLinkedList = new DoublyLinkedList<>();
        cacheStatistics = new CacheStatistics();
        if (builder.periodicCacheStatsResetEnabled) {
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleAtFixedRate(cacheStatistics::resetStatistics, 0L, builder.periodicCacheStatsResetExpiration, TimeUnit.MILLISECONDS);
        }
        logger.info("LoadingCache initialized with [maxCapacity: -> " + maxCapacity + ", expiration: -> " + expiration + ", cacheLoader: -> " + cacheLoader + "]");
    }

    public static MaxCapacity builder() {
        return new Builder();
    }

    public int getCacheSize() {
        return map.size();
    }

    public CacheStatistics getCacheStatistics() {
        return cacheStatistics;
    }

    public void put(K key, V value) {
        logger.debug("PUT operation [" + key + "->" + value + "]");
        lock.lock();
        try {
            if (map.containsKey(key)) {
                logger.debug("Already contains key[" + key + "] updating value[" + value + "]");
                Node<K, V> node = map.get(key);
                node.value = value;
                refreshNodes(node);
            } else {
                logger.debug("Adding new entry [" + key + "->" + value + "]");
                Node<K, V> node = new Node<>(key, value);
                map.put(key, node);
                doublyLinkedList.addTail(node);
                if (map.size() > maxCapacity) {
                    logger.info("Maximum capacity reached removing cache [" + doublyLinkedList.getHead().key + "]");
                    evictNode(doublyLinkedList.getHead());
                }
            }
        } finally {
            lock.unlock();
        }

    }

    public void resetCache() {
        logger.info("Resetting cache");
        while (doublyLinkedList.getSize() > 0) {
            doublyLinkedList.removeHead();
        }
        cacheStatistics.resetStatistics();
    }

    private void refreshNodes(Node<K, V> node) {
        doublyLinkedList.unlink(node);
        node.touch();
        doublyLinkedList.addTail(node);
    }

    public V get(K key) {
        cacheStatistics.increaseRequestCount();
        logger.debug("GET operation [" + key + "]");
        lock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) {
                cacheStatistics.increaseMissCount();
                logger.info("No cache found with [" + key + "]. Loading cache using loader");
                if (cacheLoader != null) {
                    cacheStatistics.increaseLoadCount();
                    V value = null;
                    try {
                        value = cacheLoader.loadCache(key);
                        cacheStatistics.increaseLoadSuccessCount();
                    } catch (Exception e) {
                        cacheStatistics.increaseLoadFailedCount();
                        cacheStatistics.increaseLoadExceptionCount();
                        logger.error("Error occurred while loading cache." + e);
                    }
                    put(key, value);
                    node = map.get(key);
                } else {
                    cacheStatistics.increaseLoadFailedCount();
                    logger.info("No loader found to load cache [" + key + "]");
                }
            } else if (isNodeExpired(node)) {
                cacheStatistics.increaseExpireCount();
                logger.info("Cache expired reloading cache [" + key + "]");
                if (cacheLoader != null) {
                    cacheStatistics.increaseLoadCount();
                    try {
                        node.value = cacheLoader.loadCache(key);
                        cacheStatistics.increaseLoadSuccessCount();
                    } catch (Exception e) {
                        cacheStatistics.increaseLoadFailedCount();
                        cacheStatistics.increaseLoadExceptionCount();
                        logger.error("Error occurred while loading cache." + e);
                    }
                    refreshNodes(node);
                } else {
                    cacheStatistics.increaseLoadFailedCount();
                    logger.info("No loader found removing cache [" + key + "]");
                    evictNode(node);
                    return null;
                }
            } else {
                cacheStatistics.increaseHitCount();
                refreshNodes(node);
            }
            logger.debug("Returning cache value [" + key + "-> " + node + "]");
            return node != null ? node.value : null;
        } finally {
            lock.unlock();
        }
    }

    public void printCache() {
        Node<K, V> current = doublyLinkedList.getHead();
        StringBuilder list = new StringBuilder("[");
        for (int i = 0; i < doublyLinkedList.getSize(); i++) {
            list.append(current);
            if (current != null) {
                current = current.next;
            }
            if (current != null) {
                list.append("->");
            }
        }
        list.append("]");
        logger.info(list.toString());

    }

    public void evictNode(Node<K, V> node) {
        doublyLinkedList.unlink(node);
        map.remove(node.key);
        cacheStatistics.increaseEvictionCount();
        if (removalListener != null) {
            removalListener.onRemoval(node.key);
        }
    }

    private boolean isNodeExpired(Node<K, V> node) {
        return System.currentTimeMillis() - node.getLastAccessTime() > expiration;
    }

    interface CacheLoader<K, V> {
        V loadCache(K key);
    }

    interface RemovalListener<K> {
        void onRemoval(K key);
    }

    interface Build<K, V> {
        LoadingCache<K, V> build();

        Build<K, V> withRemovalListener(RemovalListener<K> removalListener);

        CacheStatisticsResetExpiration<K, V> withPeriodicCacheStatsResetEnabled();
    }

    interface CacheLoaderListener<K, V> {
        Build<K, V> withCacheLoader(CacheLoader<K, V> loader);
    }

    interface CacheStatisticsResetExpiration<K, V> {
        Build<K, V> withPeriodicCacheStatsResetExpiration(Long milliseconds);
    }

    interface Expiration<K, V> {
        CacheLoaderListener<K, V> withExpiration(Long milliseconds);
    }

    interface MaxCapacity<K, V> {
        Expiration<K, V> withMaxCapacity(Integer maxCapacity);
    }


    public static final class Builder<K, V> implements CacheLoaderListener<K, V>,
            CacheStatisticsResetExpiration<K, V>, Expiration<K, V>, MaxCapacity<K, V>, Build<K, V> {
        private long expiration;
        private int maxCapacity;
        private CacheLoader<K, V> cacheLoader;
        private RemovalListener<K> removalListener;
        private boolean periodicCacheStatsResetEnabled = false;
        private long periodicCacheStatsResetExpiration;

        private Builder() {
        }

        @Override
        public Build<K, V> withCacheLoader(CacheLoader<K, V> loader) {
            cacheLoader = loader;
            return this;
        }

        @Override
        public CacheLoaderListener<K, V> withExpiration(Long milliseconds) {

            if (milliseconds == null) {
                expiration = Long.MAX_VALUE;
            } else {
                expiration = milliseconds;
            }
            return this;
        }

        @Override
        public Expiration<K, V> withMaxCapacity(Integer maxCapacity) {
            if (maxCapacity == null) {
                this.maxCapacity = 20;
            } else {
                this.maxCapacity = maxCapacity;
            }
            return this;
        }

        public LoadingCache<K, V> build() {
            return new LoadingCache<>(this);
        }

        @Override
        public Build<K, V> withRemovalListener(RemovalListener<K> removalListener) {
            this.removalListener = removalListener;
            return this;
        }

        @Override
        public Build<K, V> withPeriodicCacheStatsResetExpiration(Long milliseconds) {
            if (milliseconds != null) {
                this.periodicCacheStatsResetExpiration = milliseconds;
            } else {
                this.periodicCacheStatsResetExpiration = Long.MAX_VALUE;
            }
            return this;
        }


        @Override
        public CacheStatisticsResetExpiration<K, V> withPeriodicCacheStatsResetEnabled() {
            this.periodicCacheStatsResetEnabled = true;
            return this;
        }
    }

    private static class DoublyLinkedList<K, V> {
        private Node<K, V> head;
        private Node<K, V> tail;
        private int size;

        private DoublyLinkedList() {
            size = 0;
            this.head = null;
            this.tail = null;
        }

        public Node<K, V> getTail() {
            return tail;
        }

        public Node<K, V> getHead() {
            return head;
        }

        public void addTail(Node<K, V> node) {
            if (this.head == null) {
                this.head = node;
                this.tail = node;
                this.tail.next = null;
                this.tail.prev = null;
                this.size = 1;
            } else {
                Node<K, V> prev = this.tail;
                this.tail.next = node;
                this.tail = node;
                this.tail.prev = prev;
                this.size++;
            }
        }


        public void removeHead() {
            if (head != null) {
                this.head = this.head.next;
                if (head != null) this.head.prev = null;
                this.size--;
            }
        }

        public void removeTail() {
            if (tail != null) {
                this.tail = this.tail.prev;
                if (tail != null) this.tail.next = null;
                this.size--;
            }
        }

        public void unlink(Node<K, V> node) {
            if (node == null) {
                return;
            }

            if (node.prev != null) {
                node.prev.next = node.next;
            } else {
                head = node.next;
            }

            if (node.next != null) {
                node.next.prev = node.prev;
            } else {
                tail = node.prev;
            }
            node.next = null;
            node.prev = null;
            node = null;
            size--;

        }


        public int getSize() {
            return size;
        }
    }

    private static class Node<K, V> {
        V value;
        K key;
        long lastAccessTime;
        Node<K, V> next;
        Node<K, V> prev;

        private Node(K key, V value) {
            this.value = value;
            this.key = key;
            touch();

        }

        public K getKey() {
            return key;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public Node<K, V> getNext() {
            return next;
        }

        public Node<K, V> getPrev() {
            return prev;
        }

        public V getValue() {
            return value;
        }

        public void touch() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.valueOf(this.value);
        }
    }

    public static class CacheStatistics {
        private long hitCount = 0L;
        private long missCount = 0L;
        private long loadCount = 0L;
        private long loadExceptionCount = 0L;
        private long loadSuccessCount = 0L;
        private long loadFailedCount = 0L;
        private long expireCount = 0L;
        private long evictionCount = 0L;
        private long requestCount = 0L;


        public void increaseHitCount() {
            hitCount++;
        }

        public void increaseMissCount() {
            missCount++;
        }

        public void increaseLoadCount() {
            loadCount++;
        }

        public void increaseLoadExceptionCount() {
            loadExceptionCount++;
        }

        public void increaseLoadFailedCount() {
            loadFailedCount++;
        }

        public long getLoadFailedCount() {
            return loadFailedCount;
        }

        public void increaseLoadSuccessCount() {
            loadSuccessCount++;
        }

        public void increaseEvictionCount() {
            evictionCount++;
        }

        public void increaseRequestCount() {
            requestCount++;
        }

        public void increaseExpireCount() {
            expireCount++;
        }

        public long getHitCount() {
            return hitCount;
        }

        public long getMissCount() {
            return missCount;
        }

        public long getExpireCount() {
            return expireCount;
        }

        public double getHitRate() {
            double hitRate = 0.0;
            try {
                hitRate = (double) hitCount / (getRequestCount());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return hitRate;
        }

        public double getMissRate() {
            double missRate = 0.0;
            try {
                missRate = (double) missCount / (getRequestCount());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return missRate;
        }

        public long getLoadCount() {
            return loadCount;
        }

        public long getLoadExceptionCount() {
            return loadExceptionCount;
        }

        public long getLoadSuccessCount() {
            return loadSuccessCount;
        }

        public long getEvictionCount() {
            return evictionCount;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public void resetStatistics() {
            logger.info("Resetting cache statistics");
            hitCount = 0L;
            missCount = 0L;
            loadCount = 0L;
            loadExceptionCount = 0L;
            loadSuccessCount = 0L;
            loadFailedCount = 0L;
            expireCount = 0L;
            evictionCount = 0L;
            requestCount = 0L;
        }

        @Override
        public String toString() {
            return "CacheStatistics{" +
                    "hitCount=" + hitCount +
                    ", hitRate=" + String.format("%.2f", getHitRate()) +
                    ", missCount=" + missCount +
                    ", missRate=" + String.format("%.2f", getMissRate()) +
                    ", loadCount=" + loadCount +
                    ", loadExceptionCount=" + loadExceptionCount +
                    ", loadSuccessCount=" + loadSuccessCount +
                    ", loadFailedCount=" + loadFailedCount +
                    ", expireCount=" + expireCount +
                    ", evictionCount=" + evictionCount +
                    ", requestCount=" + requestCount +
                    '}';
        }
    }
}