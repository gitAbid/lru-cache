package org.example;

import java.util.concurrent.ConcurrentHashMap;

public class LruCache<K, V> {
    final int maxCapacity;
    ConcurrentHashMap<K, Node<K, V>> map;
    private Node<K, V> head, tail;

    public LruCache(int initialCapacity, int maxCapacity) {
        this.maxCapacity = maxCapacity;
        if (initialCapacity > maxCapacity) {
            initialCapacity = maxCapacity;
        }
        map = new ConcurrentHashMap<>(initialCapacity);
    }

    public LruCache(int maxCapacity) {
        this(20, maxCapacity);
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
    }

    private void refreshLatestNode(Node<K, V> node) {
        removeNode(node);
        addLatestNode(node);
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

    private static class Node<K, V> {
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

}
