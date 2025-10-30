package com.produsoft.workflow.datastructure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class Tree<T> implements Iterable<T> {
    private TreeNode<T> root;

    public TreeNode<T> setRoot(T value) {
        root = new TreeNode<>(value, null);
        return root;
    }

    public TreeNode<T> getRoot() {
        return root;
    }

    public boolean isEmpty() {
        return root == null;
    }

    public TreeNode<T> addChild(TreeNode<T> parent, T value) {
        Objects.requireNonNull(parent, "Parent node is required");
        ensureBelongsToTree(parent);
        TreeNode<T> child = new TreeNode<>(value, parent);
        parent.children.add(child);
        return child;
    }

    public void removeSubtree(TreeNode<T> node) {
        Objects.requireNonNull(node, "Node is required");
        ensureBelongsToTree(node);
        if (node == root) {
            root = null;
        } else {
            TreeNode<T> parent = node.parent;
            Objects.requireNonNull(parent).children.remove(node);
        }
    }

    public int size() {
        if (isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Iterator<T> iterator = iterator(); iterator.hasNext(); iterator.next()) {
            count++;
        }
        return count;
    }

    public int height() {
        return height(root);
    }

    public Optional<TreeNode<T>> findFirst(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "Predicate is required");
        for (TreeNode<T> node : nodes()) {
            if (predicate.test(node.value)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    public Iterable<TreeNode<T>> nodes() {
        return () -> new Iterator<>() {
            private final ArrayDeque<TreeNode<T>> queue = initQueue();

            private ArrayDeque<TreeNode<T>> initQueue() {
                ArrayDeque<TreeNode<T>> deque = new ArrayDeque<>();
                if (root != null) {
                    deque.add(root);
                }
                return deque;
            }

            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public TreeNode<T> next() {
                if (queue.isEmpty()) {
                    throw new NoSuchElementException();
                }
                TreeNode<T> next = queue.removeFirst();
                queue.addAll(next.children);
                return next;
            }
        };
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private final Iterator<TreeNode<T>> nodeIterator = nodes().iterator();

            @Override
            public boolean hasNext() {
                return nodeIterator.hasNext();
            }

            @Override
            public T next() {
                return nodeIterator.next().value;
            }
        };
    }

    private int height(TreeNode<T> node) {
        if (node == null) {
            return 0;
        }
        int maxChildHeight = 0;
        for (TreeNode<T> child : node.children) {
            maxChildHeight = Math.max(maxChildHeight, height(child));
        }
        return maxChildHeight + 1;
    }

    private void ensureBelongsToTree(TreeNode<T> node) {
        TreeNode<T> current = node;
        while (current != null && current != root) {
            current = current.parent;
        }
        if (current != root) {
            throw new IllegalArgumentException("Node does not belong to this tree");
        }
    }

    public static final class TreeNode<T> {
        private T value;
        private final TreeNode<T> parent;
        private final List<TreeNode<T>> children = new ArrayList<>();

        private TreeNode(T value, TreeNode<T> parent) {
            this.value = value;
            this.parent = parent;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public TreeNode<T> getParent() {
            return parent;
        }

        public List<TreeNode<T>> getChildren() {
            return Collections.unmodifiableList(children);
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }
    }
}
