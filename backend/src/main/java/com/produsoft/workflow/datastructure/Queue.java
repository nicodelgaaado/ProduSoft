package com.produsoft.workflow.datastructure;

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class Queue<T> {
    private final LinkedList<T> elements = new LinkedList<>();

    public void enqueue(T value) {
        elements.addLast(value);
    }

    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return elements.removeFirst();
    }

    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return elements.getFirst();
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public void clear() {
        elements.clear();
    }
}
