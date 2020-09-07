package org.example;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LoadingCache<K, V> {
    public static final Logger logger = Logger.getLogger(LoadingCache.class);
    private final HashMap<K, Node<K, V>> map;
    private final ReentrantLock lock = new ReentrantLock();
    private final int maxCapacity;
    private final long expiration;
    private final CacheLoader<K, V> cacheLoader;
    private final DoublyLinkedList<K, V> doublyLinkedList;


    private LoadingCache(Builder<K, V> builder) {
        BasicConfigurator.configure();
        maxCapacity = builder.maxCapacity;
        expiration = builder.expiration;
        cacheLoader = builder.cacheLoader;
        map = new HashMap<>(maxCapacity);
        doublyLinkedList = new DoublyLinkedList<>();
        logger.info("Loading cache initialized with [maxCapacity: -> " + maxCapacity + ", expiration: -> " + expiration + ", cacheLoader: -> " + cacheLoader + "]");
    }

    public static IMaxCapacity builder() {
        return new Builder();
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
                doublyLinkedList.addNode(node);
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
            doublyLinkedList.deleteFirstNode();
        }
    }

    private void refreshNodes(Node<K, V> node) {
        if (node == null) {
            return;
        }
        doublyLinkedList.delete(node);
        node.lastAccessTime = System.currentTimeMillis();
        doublyLinkedList.addNode( node);
    }

    public V get(K key) {
        logger.debug("GET operation [" + key + "]");
        lock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) {
                logger.info("No cache found with [" + key + "]. Loading cache using loader");
                if (cacheLoader != null) {
                    V value = cacheLoader.loadCache(key);
                    put(key, value);
                    node = map.get(key);
                } else {
                    logger.info("No loader found to load cache [" + key + "]");
                }
            } else if (isNodeExpired(node)) {
                logger.info("Cache expired reloading cache [" + key + "]");
                if (cacheLoader != null) {
                    node.value = cacheLoader.loadCache(key);
                    refreshNodes(node);
                } else {
                    logger.info("No loader found removing cache [" + key + "]");
                    evictNode(node);
                    return null;
                }
            } else {
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
        doublyLinkedList.delete(node);
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

    static public class DoublyLinkedList<K, V> {
        public Node<K, V> head;
        public Node<K, V> tail;
        private int size;

        public DoublyLinkedList() {
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

        /**
         * Inserting new node at the end of the linked list
         * If there is node node present we will make the first node as out main node
         *
         * @param node - represent the DListNode value to be added to the linked list
         */
        public void addNode(Node<K, V> node) {
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


        /**
         * Deleting the first Node from the list
         */
        public void deleteFirstNode() {
            if (head != null) {
                this.head = this.head.next;
                if (head != null) this.head.prev = null;
                this.size--;
            }
        }

        /**
         * Deleting the last DListNode from the list
         */
        public void deleteLastNode() {
            if (tail != null) {
                this.tail = this.tail.prev;
                if (tail != null) this.tail.next = null;
                this.size--;
            }
        }

        public void delete(Node<K, V> node) {
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


        /**
         * Get linked list size
         *
         * @return
         */
        public int getSize() {
            return size;
        }
    }

    static public class Node<K, V> {
        // The actual data
        V value;
        K key;
        long lastAccessTime;
        // Reference to the next node
        Node<K, V> next;
        // Reference to the prev node
        Node<K, V> prev;

        /**
         * Constructor.
         * Note that the next and prev variables are set to null, thus this is the "root-node"
         *
         * @param value node data
         */
        Node(K key, V value) {
            this(null, key, value, null, System.currentTimeMillis());
        }

        /**
         * Constructor.
         *
         * @param value node data
         * @param next  reference to next node
         * @param prev  reference to the previous node
         */
        Node(Node<K, V> prev, K key, V value, Node<K, V> next, long lastAccessTime) {
            this.value = value;
            this.key = key;
            this.next = next;
            this.prev = prev;
            this.lastAccessTime = lastAccessTime;
        }

        @Override
        public String toString() {
            return String.valueOf(this.value);
        }
    }
}