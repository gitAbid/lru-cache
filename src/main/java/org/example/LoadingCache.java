package org.example;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LoadingCache<K, V> {
    private final HashMap<K, Node<K, V>> map;
    private final ReentrantLock lock = new ReentrantLock();
    private final int maxCapacity;
    private final long expiration;
    private final CacheLoader<K, V> cacheLoader;
    private Node<K, V> head, tail;


    private LoadingCache(Builder<K, V> builder) {
        maxCapacity = builder.maxCapacity;
        expiration = builder.expiration;
        cacheLoader = builder.cacheLoader;
        map = new HashMap<>(maxCapacity);
    }

    public static IMaxCapacity builder() {
        return new Builder();
    }

    private void removeNode(Node<K, V> node) {
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

    }

    private void addNode(Node<K, V> node) {
        if (node == null) {
            return;
        }
        if (head == null) {
            head = tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            node.next = null;
            tail = node;
        }
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            if (map.containsKey(key)) {
                Node<K, V> node = map.get(key);
                node.value = value;
                refreshNodes(node);
            } else {
                Node<K, V> node = new Node<>(key, value);
                map.put(key, node);
                addNode(node);
                if (map.size() > maxCapacity) {
                    System.out.println("Maximum capacity reached removed cache [" + head.key + "]");
                    evictNode(head);
                }
            }
        } finally {
            lock.unlock();
        }

    }


    private void refreshNodes(Node<K, V> node) {
        if (node == null) {
            return;
        }
        removeNode(node);
        addNode(node);
        node.lastAccessTime = System.currentTimeMillis();
    }

    public V get(K key) {
        lock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) {
                System.out.println("No cache found with [" + key + "]. Loading cache");
                if (cacheLoader != null) {
                    V value = cacheLoader.loadCache(key);
                    put(key, value);
                    node = map.get(key);
                } else {
                    System.out.println("No loader found to load cache [" + key + "]");
                }
            } else if (isNodeExpired(node)) {
                System.out.println("Cache expired reloading cache [" + key + "]");
                if (cacheLoader != null) {
                    node.value = cacheLoader.loadCache(key);
                    refreshNodes(node);
                } else {
                    System.out.println("No loader found removed cache [" + key + "]");
                    evictNode(node);
                    return null;
                }
            } else {
                refreshNodes(node);
            }
            return node != null ? node.value : null;
        } finally {
            lock.unlock();
        }
    }

    public void printCache() {
        Node<K, V> curr = head;
        while (curr != null) {
            System.out.print(curr.value + " -> ");
            curr = curr.next;
        }
        System.out.println();
    }

    public void evictNode(Node<K, V> node) {
        removeNode(node);
        map.remove(node.key);
    }

    private boolean isNodeExpired(Node<K, V> node) {
        return System.currentTimeMillis() - node.lastAccessTime > expiration;
    }

    interface CacheLoader<K, V> {
        V loadCache(K key);
    }

    interface IBuild<K, V> {
        LoadingCache<K, V> build();
    }

    interface ICacheLoader<K, V> {
        IBuild<K, V> withCacheLoader(CacheLoader<K, V> loader);
    }

    interface IExpiration<K, V> {
        ICacheLoader<K, V> withExpiration(Long milliseconds);
    }

    interface IMaxCapacity<K, V> {
        IExpiration<K, V> withMaxCapacity(Integer maxCapacity);
    }

    private static class Node<K, V> {
        private final K key;
        private V value;
        private Node<K, V> next, prev;
        private long lastAccessTime = System.currentTimeMillis();


        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static final class Builder<K, V> implements ICacheLoader<K, V>, IExpiration<K, V>, IMaxCapacity<K, V>, IBuild<K, V> {
        private long expiration;
        private int maxCapacity;
        private CacheLoader<K, V> cacheLoader;

        private Builder() {
        }

        @Override
        public IBuild<K, V> withCacheLoader(CacheLoader<K, V> loader) {
            cacheLoader = loader;
            return this;
        }

        @Override
        public ICacheLoader<K, V> withExpiration(Long milliseconds) {

            if (milliseconds == null) {
                expiration = Long.MAX_VALUE;
            } else {
                expiration = milliseconds;
            }
            return this;
        }

        @Override
        public IExpiration<K, V> withMaxCapacity(Integer maxCapacity) {
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
    }
}