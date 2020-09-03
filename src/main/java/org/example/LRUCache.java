package org.example;

import java.util.concurrent.*;

public class LRUCache<K, V> {
    private final int maxCapacity;
    private final long expiration;
    private final TimeUnit timeUnit;
    private final ConcurrentHashMap<K, Node<K, V>> map;
    private final ConcurrentHashMap<K, ScheduledFuture<?>> cacheMaintainerMap;
    private final ScheduledExecutorService scheduledExecutorService;

    private Node<K, V> head, tail;

    public LRUCache(int initialCapacity, int maxCapacity, long expiration, TimeUnit timeUnit) {
        this.maxCapacity = maxCapacity;
        this.expiration = expiration;
        this.timeUnit = timeUnit;
        if (initialCapacity > maxCapacity) {
            initialCapacity = maxCapacity;
        }
        map = new ConcurrentHashMap<>(initialCapacity);
        cacheMaintainerMap = new ConcurrentHashMap<>(initialCapacity);
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    public LRUCache(int maxCapacity, int expiration, TimeUnit timeUnit) {
        this(20, maxCapacity, expiration, timeUnit);
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
                evict(head);
            }
            Node<K, V> node = new Node<>(key, value);
            map.put(key, node);
            addLatestNode(node);
            ScheduledFuture<?> scheduledFuture = scheduledExecutorService.schedule(new CacheMaintainer(node),
                    expiration, timeUnit);
            cacheMaintainerMap.put(key, scheduledFuture);
        }

    }

    private void updateExpirationTime(K key) {
        if (cacheMaintainerMap.containsKey(key)) {
            ScheduledFuture<?> scheduledFuture = cacheMaintainerMap.get(key);
            scheduledFuture.cancel(false);
        }
    }

    private void refreshLatestNode(Node<K, V> node) {
        if (node == null) {
            return;
        }
        removeNode(node);
        addLatestNode(node);
        updateExpirationTime(node.key);
        ScheduledFuture<?> scheduledFuture = scheduledExecutorService.schedule(new CacheMaintainer(node)
                , expiration, timeUnit);
        cacheMaintainerMap.put(node.key, scheduledFuture);
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

    public void evict(Node<K, V> node) {
        removeNode(node);
        map.remove(node.key);
        if (cacheMaintainerMap.containsKey(node.key)) {
            ScheduledFuture<?> scheduledFuture = cacheMaintainerMap.get(node.key);
            scheduledFuture.cancel(false);
        }
        cacheMaintainerMap.remove(node.key);
    }

    private static class Node<K, V> {
        private final K key;
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

    class CacheMaintainer implements Runnable {
        final Node<K, V> node;

        public CacheMaintainer(Node<K, V> node) {
            this.node = node;
        }

        @Override
        public void run() {
            if (node != null && map.containsKey(node.key)) {
                System.out.println("Maximum expiration reached evicting cache " + node);
                evict(node);
            }
        }
    }

}