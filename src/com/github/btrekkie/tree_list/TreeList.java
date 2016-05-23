package com.github.btrekkie.tree_list;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import com.github.btrekkie.red_black_node.RedBlackNode;

/**
 * Implements a list using a self-balancing binary search tree augmented by subtree size.  The benefit of this compared
 * to ArrayList or LinkedList is that it supports both decent random access and quickly adding to or removing from the
 * middle of the list.  Operations have the following running times:
 *
 * size(): O(1)
 * get, set, add, remove: O(log N)
 * addAll: O(log N + P), where P is the number of elements we add
 * iterator(): O(log N + P + M log N), where P is the number of elements over which we iterate and M is the number of
 *     elements we remove
 * listIterator: O(log N + P + M log N + R log N), where P is the number of times we iterate over or set an element, M
 *     is the number of elements we add or remove, and R is the number of times we change the direction of iteration
 * clear(): O(1), excluding garbage collection
 * subList.clear(): O(log N), excluding garbage collection
 * Constructor: O(N)
 *
 * This class is similar to an Apache Commons Collections class by the same name.  I speculate that the Apache class is
 * faster than this (by a constant factor) for most operations.  However, this class's implementations of addAll and
 * subList.clear() are asymptotically faster than the Apache class's implementations.
 */
public class TreeList<T> extends AbstractList<T> {
    /** The dummy leaf node. */
    private final TreeListNode<T> leaf = new TreeListNode<T>(null);

    /** The root node of the tree. */
    private TreeListNode<T> root;

    /** Constructs a new empty TreeList. */
    public TreeList() {
        root = leaf;
    }

    /** Constructs a new TreeList containing the specified values, in iteration order. */
    public TreeList(Collection<? extends T> values) {
        root = createTree(values);
    }

    /** Returns the root of a perfectly height-balanced tree containing the specified values, in iteration order. */
    private TreeListNode<T> createTree(Collection<? extends T> values) {
        List<TreeListNode<T>> nodes = new ArrayList<TreeListNode<T>>(values.size());
        for (T value : values) {
            nodes.add(new TreeListNode<T>(value));
        }
        return RedBlackNode.<TreeListNode<T>>createTree(nodes, leaf);
    }

    /**
     * Returns the node for get(index).  Raises an IndexOutOfBoundsException if "index" is not in the range [0, size()).
     */
    private TreeListNode<T> getNode(int index) {
        if (index < 0 || index >= root.size) {
            throw new IndexOutOfBoundsException("Index " + index + " is not in the range [0, " + root.size + ")");
        }
        int rank = index;
        TreeListNode<T> node = root;
        while (rank != node.left.size) {
            if (rank < node.left.size) {
                node = node.left;
            } else {
                rank -= node.left.size + 1;
                node = node.right;
            }
        }
        return node;
    }

    @Override
    public T get(int index) {
        return getNode(index).value;
    }

    @Override
    public int size() {
        return root.size;
    }

    @Override
    public boolean isEmpty() {
        return root == leaf;
    }

    @Override
    public T set(int index, T value) {
        modCount++;
        TreeListNode<T> node = getNode(index);
        T oldValue = node.value;
        node.value = value;
        return oldValue;
    }

    @Override
    public void add(int index, T value) {
        if (index < 0 || index > root.size) {
            throw new IndexOutOfBoundsException("Index " + index + " is not in the range [0, " + root.size + "]");
        }
        modCount++;

        TreeListNode<T> newNode = new TreeListNode<T>(value);
        newNode.left = leaf;
        newNode.right = leaf;
        if (root.isLeaf()) {
            root = newNode;
            newNode.isRed = false;
            return;
        }
        newNode.isRed = true;
        if (index < root.size) {
            TreeListNode<T> node = getNode(index);
            if (node.left.isLeaf()) {
                node.left = newNode;
                newNode.parent = node;
            } else {
                node = node.predecessor();
                node.right = newNode;
                newNode.parent = node;
            }
        } else {
            TreeListNode<T> node;
            node = root.max();
            node.right = newNode;
            newNode.parent = node;
        }

        newNode.fixInsertion();
        while (root.parent != null) {
            root = root.parent;
        }
    }

    @Override
    public T remove(int index) {
        TreeListNode<T> node = getNode(index);
        modCount++;
        T value = node.value;
        root = node.remove();
        return value;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> values) {
        if (index < 0 || index > root.size) {
            throw new IndexOutOfBoundsException("Index " + index + " is not in the range [0, " + root.size + "]");
        }
        modCount++;
        if (values.isEmpty()) {
            return false;
        } else {
            if (index >= root.size) {
                root = root.concatenate(createTree(values));
            } else {
                TreeListNode<T>[] split = root.split(getNode(index));
                root = split[0].concatenate(createTree(values)).concatenate(split[1]);
            }
            return true;
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> values) {
        modCount++;
        if (values.isEmpty()) {
            return false;
        } else {
            root = root.concatenate(createTree(values));
            return true;
        }
    }

    @Override
    protected void removeRange(int startIndex, int endIndex) {
        if (startIndex != endIndex) {
            modCount++;
            TreeListNode<T> last;
            if (endIndex == root.size) {
                last = leaf;
            } else {
                TreeListNode<T>[] split = root.split(getNode(endIndex));
                root = split[0];
                last = split[1];
            }
            TreeListNode<T> first = root.split(getNode(startIndex))[0];
            root = first.concatenate(last);
        }
    }

    @Override
    public void clear() {
        modCount++;
        root = leaf;
    }

    /** The class for TreeList.iterator(). */
    private class TreeListIterator implements Iterator<T> {
        /** The value of TreeList.this.modCount we require to continue iteration without concurrent modification. */
        private int modCount = TreeList.this.modCount;

        /**
         * The node containing the last element next() returned.  This is null if we have yet to call next() or we have
         * called remove() since the last call to next().
         */
        private TreeListNode<T> node;

        /** The node containing next().  This is null if we have reached the end of the list. */
        private TreeListNode<T> nextNode;

        /** Whether we have (successfully) called next(). */
        private boolean haveCalledNext;

        private TreeListIterator() {
            if (root.isLeaf()) {
                nextNode = null;
            } else {
                nextNode = root.min();
            }
        }

        @Override
        public boolean hasNext() {
            return nextNode != null;
        }

        @Override
        public T next() {
            if (nextNode == null) {
                throw new NoSuchElementException("Reached the end of the list");
            } else if (TreeList.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            haveCalledNext = true;
            node = nextNode;
            nextNode = nextNode.successor();
            return node.value;
        }

        @Override
        public void remove() {
            if (node == null) {
                if (!haveCalledNext) {
                    throw new IllegalStateException("Must call next() before calling remove()");
                } else {
                    throw new IllegalStateException("Already removed this element");
                }
            } else if (TreeList.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            root = node.remove();
            node = null;
            TreeList.this.modCount++;
            modCount = TreeList.this.modCount;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new TreeListIterator();
    }

    /** The class for TreeList.listIterator. */
    private class TreeListListIterator implements ListIterator<T> {
        /** The value of TreeList.this.modCount we require to continue iteration without concurrent modification. */
        private int modCount = TreeList.this.modCount;

        /** The current return value for nextIndex(). */
        private int nextIndex;

        /** The node for next(), or null if hasNext() is false. */
        private TreeListNode<T> nextNode;

        /** The node for previous(), or null if hasPrevious() is false. */
        private TreeListNode<T> prevNode;

        /** Whether we have called next() or previous(). */
        private boolean haveCalledNextOrPrevious;

        /** Whether we (successfully) called next() more recently than previous(). */
        private boolean justCalledNext;

        /**
         * Whether we have (successfully) called remove() or "add" since the last (successful) call to next() or
         * previous().
         */
        private boolean haveModified;

        /**
         * Constructs a new TreeListListIterator.
         * @param index The starting index, as in the "index" argument to listIterator.  This method assumes that
         *     0 <= index < root.size.
         */
        private TreeListListIterator(int index) {
            nextIndex = index;
            if (index > 0) {
                prevNode = getNode(index - 1);
                nextNode = prevNode.successor();
            } else {
                prevNode = null;
                if (root.size > 0) {
                    nextNode = root.min();
                } else {
                    nextNode = null;
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (modCount != TreeList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return nextNode != null;
        }

        @Override
        public T next() {
            if (nextNode == null) {
                throw new NoSuchElementException("Reached the end of the list");
            } else if (modCount != TreeList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            haveCalledNextOrPrevious = true;
            justCalledNext = true;
            haveModified = false;
            nextIndex++;
            prevNode = nextNode;
            nextNode = nextNode.successor();
            return prevNode.value;
        }

        @Override
        public int nextIndex() {
            if (modCount != TreeList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return nextIndex;
        }

        @Override
        public boolean hasPrevious() {
            if (modCount != TreeList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return prevNode != null;
        }

        @Override
        public T previous() {
            if (prevNode == null) {
                throw new NoSuchElementException("Reached the beginning of the list");
            } else if (modCount != TreeList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            haveCalledNextOrPrevious = true;
            justCalledNext = false;
            haveModified = false;
            nextIndex--;
            nextNode = prevNode;
            prevNode = prevNode.predecessor();
            return nextNode.value;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public void set(T value) {
            if (!haveCalledNextOrPrevious) {
                throw new IllegalStateException("Must call next() or previous() before calling \"set\"");
            } else if (haveModified) {
                throw new IllegalStateException("Already modified the list at this position");
            } else if (modCount != TreeList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (justCalledNext) {
                prevNode.value = value;
            } else {
                nextNode.value = value;
            }
            TreeList.this.modCount++;
            modCount = TreeList.this.modCount;
        }

        @Override
        public void add(T value) {
            if (haveModified) {
                throw new IllegalStateException("Already modified the list at this position");
            } else if (modCount != TreeList.this.modCount) {
                throw new ConcurrentModificationException();
            }

            // Create the new node
            TreeListNode<T> newNode = new TreeListNode<T>(value);;
            newNode.left = leaf;
            newNode.right = leaf;
            newNode.isRed = true;

            // Insert newNode.  There is guaranteed to be a leaf child of prevNode or nextNode where we can insert it.
            if (nextNode != null && nextNode.left.isLeaf()) {
                nextNode.left = newNode;
                newNode.parent = nextNode;
            } else if (prevNode != null) {
                prevNode.right = newNode;
                newNode.parent = prevNode;
            } else {
                root = newNode;
            }

            prevNode = newNode;
            newNode.fixInsertion();
            nextIndex++;
            haveModified = true;
            TreeList.this.modCount++;
            modCount = TreeList.this.modCount;
        }

        @Override
        public void remove() {
            if (!haveCalledNextOrPrevious) {
                throw new IllegalStateException("Must call next() or previous() before calling remove()");
            } else if (haveModified) {
                throw new IllegalStateException("Already modified the list at this position");
            } else if (modCount != TreeList.this.modCount) {
                throw new ConcurrentModificationException();
            }

            if (justCalledNext) {
                TreeListNode<T> predecessor = prevNode.predecessor();
                root = prevNode.remove();
                prevNode = predecessor;
            } else {
                TreeListNode<T> successor = nextNode.successor();
                root = nextNode.remove();
                nextNode = successor;
            }

            haveModified = true;
            TreeList.this.modCount++;
            modCount = TreeList.this.modCount;
        }
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        if (index < 0 || index > root.size) {
            throw new IndexOutOfBoundsException("Index " + index + " is not in the range [0, " + root.size + "]");
        }
        return new TreeListListIterator(index);
    }
}
