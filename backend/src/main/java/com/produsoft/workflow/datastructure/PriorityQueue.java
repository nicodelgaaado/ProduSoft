package com.produsoft.workflow.datastructure;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

public class PriorityQueue<T extends Comparable<? super T>> {
    private static final int DEFAULT_CAPACITY = 16;
    private Object[] heap;
    private int size;

    public PriorityQueue() {
        this.heap = new Object[DEFAULT_CAPACITY];
    }

    public void offer(T value) {
        Objects.requireNonNull(value, "PriorityQueue does not support null elements");
        ensureCapacity(size + 1);
        heap[size] = value;
        siftUp(size);
        size++;
    }

    public T poll() {
        if (isEmpty()) {
            throw new NoSuchElementException("PriorityQueue is empty");
        }
        T root = elementAt(0);
        size--;
        heap[0] = heap[size];
        heap[size] = null;
        if (size > 0) {
            siftDown(0);
        }
        return root;
    }

    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("PriorityQueue is empty");
        }
        return elementAt(0);
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void clear() {
        Arrays.fill(heap, 0, size, null);
        size = 0;
    }

    @SuppressWarnings("unchecked")
    private T elementAt(int index) {
        return (T) heap[index];
    }

    private void ensureCapacity(int capacity) {
        if (capacity <= heap.length) {
            return;
        }
        heap = Arrays.copyOf(heap, heap.length + (heap.length >> 1));
    }

    private void siftUp(int index) {
        int current = index;
        while (current > 0) {
            int parent = (current - 1) >>> 1;
            if (compare(current, parent) >= 0) {
                break;
            }
            swap(current, parent);
            current = parent;
        }
    }

    private void siftDown(int index) {
        int current = index;
        int half = size >>> 1;
        while (current < half) {
            int left = (current << 1) + 1;
            int right = left + 1;
            int smallest = left;
            if (right < size && compare(right, left) < 0) {
                smallest = right;
            }
            if (compare(current, smallest) <= 0) {
                break;
            }
            swap(current, smallest);
            current = smallest;
        }
    }

    private int compare(int firstIndex, int secondIndex) {
        T first = elementAt(firstIndex);
        T second = elementAt(secondIndex);
        return first.compareTo(second);
    }

    private void swap(int firstIndex, int secondIndex) {
        Object tmp = heap[firstIndex];
        heap[firstIndex] = heap[secondIndex];
        heap[secondIndex] = tmp;
    }
}
