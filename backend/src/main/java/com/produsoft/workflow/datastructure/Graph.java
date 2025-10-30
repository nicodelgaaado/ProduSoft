package com.produsoft.workflow.datastructure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Graph<T> {
    private final Map<T, Set<T>> adjacency = new LinkedHashMap<>();
    private int edgeCount;

    public boolean addVertex(T vertex) {
        Objects.requireNonNull(vertex, "Vertex cannot be null");
        if (adjacency.containsKey(vertex)) {
            return false;
        }
        adjacency.put(vertex, new LinkedHashSet<>());
        return true;
    }

    public boolean addEdge(T source, T target) {
        Objects.requireNonNull(source, "Source cannot be null");
        Objects.requireNonNull(target, "Target cannot be null");
        addVertex(source);
        addVertex(target);
        Set<T> neighbors = adjacency.get(source);
        if (neighbors.add(target)) {
            edgeCount++;
            return true;
        }
        return false;
    }

    public boolean addUndirectedEdge(T first, T second) {
        boolean added = false;
        added |= addEdge(first, second);
        added |= addEdge(second, first);
        return added;
    }

    public boolean containsVertex(T vertex) {
        return adjacency.containsKey(vertex);
    }

    public boolean containsEdge(T source, T target) {
        Set<T> neighbors = adjacency.get(source);
        return neighbors != null && neighbors.contains(target);
    }

    public Set<T> getNeighbors(T vertex) {
        Set<T> neighbors = adjacency.get(vertex);
        if (neighbors == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(neighbors);
    }

    public Set<T> getVertices() {
        if (adjacency.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(adjacency.keySet());
    }

    public boolean removeEdge(T source, T target) {
        Set<T> neighbors = adjacency.get(source);
        if (neighbors != null && neighbors.remove(target)) {
            edgeCount--;
            return true;
        }
        return false;
    }

    public boolean removeUndirectedEdge(T first, T second) {
        boolean removed = false;
        removed |= removeEdge(first, second);
        removed |= removeEdge(second, first);
        return removed;
    }

    public boolean removeVertex(T vertex) {
        Set<T> removed = adjacency.remove(vertex);
        if (removed == null) {
            return false;
        }
        edgeCount -= removed.size();
        for (Set<T> neighbors : adjacency.values()) {
            if (neighbors.remove(vertex)) {
                edgeCount--;
            }
        }
        return true;
    }

    public int vertexCount() {
        return adjacency.size();
    }

    public int edgeCount() {
        return edgeCount;
    }

    public boolean isEmpty() {
        return adjacency.isEmpty();
    }

    public void clear() {
        adjacency.clear();
        edgeCount = 0;
    }
}
