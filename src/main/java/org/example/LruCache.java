package org.example;

import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class LruCache<K, V> {
    private final int maxCapacity;
    private final long expiration;
    private final ConcurrentHashMap<K, Node<K, V>> map;
    Timer timer;
    private boolean cacheMaintainerRunning = false;
    private Node<K, V> head, tail;

    public LruCache(int initialCapacity, int maxCapacity, long expiration) {
        this.maxCapacity = maxCapacity;
        if (initialCapacity > maxCapacity) {
            initialCapacity = maxCapacity;
        }
        this.expiration = expiration;
        map = new ConcurrentHashMap<>(initialCapacity);
        timer = new Timer();
        timer.scheduleAtFixedRate(new CacheMaintainer(), 0, 1000);
        cacheMaintainerRunning = true;
    }

    public LruCache(int maxCapacity, int expiration) {
        this(20, maxCapacity, expiration);
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

    private void addLatestNode(Node<K, V> node) {
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
        if (map.contains(key)) {
            Node<K, V> node = map.get(key);
            node.value = value;
            refreshLatestNode(node);
        } else {
            if (map.size() > maxCapacity) {
                System.out.println("Maximum capacity reached removed node " + head);
                removeNode(head);
                map.remove(head.key);
            }
            Node<K, V> node = new Node<>(key, value);
            map.put(key, node);
            addLatestNode(node);
        }
        if (!cacheMaintainerRunning && !map.isEmpty()) {
            this.timer = new Timer();
            timer.scheduleAtFixedRate(new CacheMaintainer(), 0, 1000);
        }
    }

    private void refreshLatestNode(Node<K, V> node) {
        removeNode(node);
        addLatestNode(node);
        node.lastAccessed = System.currentTimeMillis();
    }

    public V get(K key) {
        Node<K, V> node = map.get(key);
        refreshLatestNode(node);
        return node != null ? node.value : null;
    }

    public void printCache() {
        Node<K, V> curr = head;
        while (curr != null) {
            System.out.print(curr.value + " -> ");
            curr = curr.next;
        }
        System.out.println();
    }

    private void updateCacheMaintainerStatus() {
        if (map.isEmpty()) {
            timer.cancel();
            timer.purge();
            System.out.println("No entries to clean up. Stopping maintainer");
            cacheMaintainerRunning = false;
        }
    }

    public void evict(Node<K, V> node) {
        long lastAccessedPeriod = System.currentTimeMillis() - node.lastAccessed;
        if (lastAccessedPeriod > expiration) {
            System.out.println("Maximum expiration reached evicting cache " + node);
            removeNode(node);
            map.remove(node.key);
            if (head != null) {
                evict(head);
            }
        }
    }

    private static class Node<K, V> {
        private long lastAccessed = System.currentTimeMillis();
        private K key;
        private V value;
        private Node<K, V> next, prev;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    class CacheMaintainer extends TimerTask {
        @Override
        public void run() {
            cacheMaintainerRunning = true;
            if (head != null) {
                evict(head);
            }
            updateCacheMaintainerStatus();
            System.out.println("Running " + Instant.now());
        }
    }

}
