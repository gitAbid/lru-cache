package org.example;

/**
 * This class represents a node in a Doubly Linked List.
 * The next-variable is a pointer to the next node,
 * and the prev-variable is a pointer to the previous node.
 * <p>
 *
 * @author Anders Engen Olsen
 * @see DoublyLinkedList
 */
public class Node<K, V> {
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