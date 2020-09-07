package org.example;


public class DoublyLinkedList<K, V> {
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
     * @param value - represent the DListNode value to be added to the linked list
     */
    public void addNode(K key, V value) {
        Node<K, V> node = new Node<>(key, value);
        node.value = value;
        node.next = null;

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

    public void delete(K key) {
        Node<K, V> node = this.head;
        if (node != null) {
            for (int i = 0; i < size; i++) {
                if (node.key.equals(key)) {
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
                    return;
                }
                node = node.next;
            }
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
