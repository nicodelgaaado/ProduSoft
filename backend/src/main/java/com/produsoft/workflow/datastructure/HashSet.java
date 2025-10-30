package com.produsoft.workflow.datastructure;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class HashSet<T> implements Iterable<T> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    private Node<T>[] buckets;
    private int size;

    @SuppressWarnings("unchecked")
    public HashSet() {
        this.buckets = (Node<T>[]) new Node[DEFAULT_CAPACITY];
    }

    public boolean add(T value) {
        Objects.requireNonNull(value, "HashSet does not support null values");
        ensureCapacity(size + 1);
        int index = indexFor(value);
        Node<T> current = buckets[index];
        while (current != null) {
            if (Objects.equals(current.value, value)) {
                return false;
            }
            current = current.next;
        }
        buckets[index] = new Node<>(value, buckets[index]);
        size++;
        return true;
    }

    public boolean contains(T value) {
        if (value == null) {
            return false;
        }
        int index = indexFor(value);
        Node<T> current = buckets[index];
        while (current != null) {
            if (Objects.equals(current.value, value)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    public boolean remove(T value) {
        if (value == null) {
            return false;
        }
        int index = indexFor(value);
        Node<T> current = buckets[index];
        Node<T> previous = null;
        while (current != null) {
            if (Objects.equals(current.value, value)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return true;
            }
            previous = current;
            current = current.next;
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
        Arrays.fill(buckets, null);
        size = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int bucketIndex;
            private Node<T> currentNode;
            private int visited;

            @Override
            public boolean hasNext() {
                return visited < size;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                while (currentNode == null) {
                    if (bucketIndex >= buckets.length) {
                        throw new IllegalStateException("HashSet iterator exceeded bucket array");
                    }
                    currentNode = buckets[bucketIndex++];
                }
                T value = currentNode.value;
                currentNode = currentNode.next;
                visited++;
                return value;
            }
        };
    }

    private int indexFor(T value) {
        return (value.hashCode() & 0x7fffffff) % buckets.length;
    }

    private void ensureCapacity(int required) {
        if (required <= threshold()) {
            return;
        }
        resize();
    }

    private int threshold() {
        return (int) (buckets.length * LOAD_FACTOR);
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Node<T>[] oldBuckets = buckets;
        buckets = (Node<T>[]) new Node[oldBuckets.length << 1];
        size = 0;
        for (Node<T> node : oldBuckets) {
            while (node != null) {
                add(node.value);
                node = node.next;
            }
        }
    }

    private static final class Node<T> {
        private final T value;
        private Node<T> next;

        private Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }
}
