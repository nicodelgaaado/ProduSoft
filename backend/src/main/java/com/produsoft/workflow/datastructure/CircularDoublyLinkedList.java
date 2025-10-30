package com.produsoft.workflow.datastructure;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class CircularDoublyLinkedList<T> implements Iterable<T> {
    private final Node<T> sentinel = new Node<>(null);
    private int size;

    public CircularDoublyLinkedList() {
        sentinel.next = sentinel;
        sentinel.prev = sentinel;
    }

    public void addFirst(T value) {
        insertBetween(value, sentinel, sentinel.next);
    }

    public void addLast(T value) {
        insertBetween(value, sentinel.prev, sentinel);
    }

    public T removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException("List is empty");
        }
        return unlink(sentinel.next);
    }

    public T removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException("List is empty");
        }
        return unlink(sentinel.prev);
    }

    public boolean remove(T value) {
        Node<T> current = sentinel.next;
        while (current != sentinel) {
            if (Objects.equals(current.value, value)) {
                unlink(current);
                return true;
            }
            current = current.next;
        }
        return false;
    }

    public boolean contains(T value) {
        for (T element : this) {
            if (Objects.equals(element, value)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        sentinel.next = sentinel;
        sentinel.prev = sentinel;
        size = 0;
    }

    private void insertBetween(T value, Node<T> prev, Node<T> next) {
        Node<T> newNode = new Node<>(value);
        newNode.prev = prev;
        newNode.next = next;
        prev.next = newNode;
        next.prev = newNode;
        size++;
    }

    private T unlink(Node<T> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        size--;
        return node.value;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Node<T> current = sentinel.next;

            @Override
            public boolean hasNext() {
                return current != sentinel;
            }

            @Override
            public T next() {
                if (current == sentinel) {
                    throw new NoSuchElementException();
                }
                T value = current.value;
                current = current.next;
                return value;
            }
        };
    }

    private static final class Node<T> {
        private final T value;
        private Node<T> prev;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }
}
