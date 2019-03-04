package com.github.btrekkie.red_black_node;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A node in a red-black tree ( https://en.wikipedia.org/wiki/Red%E2%80%93black_tree ). Compared to a class like Java's
 * TreeMap, RedBlackNode is a low-level data structure. The internals of a node are exposed as public fields, allowing
 * clients to directly observe and manipulate the structure of the tree. This gives clients flexibility, although it
 * also enables them to violate the red-black or BST properties. The RedBlackNode class provides methods for performing
 * various standard operations, such as insertion and removal.
 *
 * Unlike most implementations of binary search trees, RedBlackNode supports arbitrary augmentation. By subclassing
 * RedBlackNode, clients can add arbitrary data and augmentation information to each node. For example, if we were to
 * use a RedBlackNode subclass to implement a sorted set, the subclass would have a field storing an element in the set.
 * If we wanted to keep track of the number of non-leaf nodes in each subtree, we would store this as a "size" field and
 * override augment() to update this field. All RedBlackNode methods (such as "insert" and remove()) call augment() as
 * necessary to correctly maintain the augmentation information, unless otherwise indicated.
 *
 * The values of the tree are stored in the non-leaf nodes. RedBlackNode does not support use cases where values must be
 * stored in the leaf nodes. It is recommended that all of the leaf nodes in a given tree be the same (black)
 * RedBlackNode instance, to save space. The root of an empty tree is a leaf node, as opposed to null.
 *
 * For reference, a red-black tree is a binary search tree satisfying the following properties:
 *
 * - Every node is colored red or black.
 * - The leaf nodes, which are dummy nodes that do not store any values, are colored black.
 * - The root is black.
 * - Both children of each red node are black.
 * - Every path from the root to a leaf contains the same number of black nodes.
 *
 * @param <N> The type of node in the tree. For example, we might have
 *     "class FooNode<T> extends RedBlackNode<FooNode<T>>".
 * @author Bill Jacobs
 */
public abstract class RedBlackNode<N extends RedBlackNode<N>> implements Comparable<N> {
    /** A Comparator that compares Comparable elements using their natural order. */
    private static final Comparator<Comparable<Object>> NATURAL_ORDER = new Comparator<Comparable<Object>>() {
        @Override
        public int compare(Comparable<Object> value1, Comparable<Object> value2) {
            return value1.compareTo(value2);
        }
    };

    /** The parent of this node, if any.  "parent" is null if this is a leaf node. */
    public N parent;

    /** The left child of this node.  "left" is null if this is a leaf node. */
    public N left;

    /** The right child of this node.  "right" is null if this is a leaf node. */
    public N right;

    /** Whether the node is colored red, as opposed to black. */
    public boolean isRed;

    /**
     * Sets any augmentation information about the subtree rooted at this node that is stored in this node.  For
     * example, if we augment each node by subtree size (the number of non-leaf nodes in the subtree), this method would
     * set the size field of this node to be equal to the size field of the left child plus the size field of the right
     * child plus one.
     *
     * "Augmentation information" is information that we can compute about a subtree rooted at some node, preferably
     * based only on the augmentation information in the node's two children and the information in the node.  Examples
     * of augmentation information are the sum of the values in a subtree and the number of non-leaf nodes in a subtree.
     * Augmentation information may not depend on the colors of the nodes.
     *
     * This method returns whether the augmentation information in any of the ancestors of this node might have been
     * affected by changes in this subtree since the last call to augment().  In the usual case, where the augmentation
     * information depends only on the information in this node and the augmentation information in its immediate
     * children, this is equivalent to whether the augmentation information changed as a result of this call to
     * augment().  For example, in the case of subtree size, this returns whether the value of the size field prior to
     * calling augment() differed from the size field of the left child plus the size field of the right child plus one.
     * False positives are permitted.  The return value is unspecified if we have not called augment() on this node
     * before.
     *
     * This method may assume that this is not a leaf node.  It may not assume that the augmentation information stored
     * in any of the tree's nodes is correct.  However, if the augmentation information stored in all of the node's
     * descendants is correct, then the augmentation information stored in this node must be correct after calling
     * augment().
     */
    public boolean augment() {
        return false;
    }

    /**
     * Throws a RuntimeException if we detect that this node locally violates any invariants specific to this subclass
     * of RedBlackNode.  For example, if this stores the size of the subtree rooted at this node, this should throw a
     * RuntimeException if the size field of this is not equal to the size field of the left child plus the size field
     * of the right child plus one.  Note that we may call this on a leaf node.
     *
     * assertSubtreeIsValid() calls assertNodeIsValid() on each node, or at least starts to do so until it detects a
     * problem.  assertNodeIsValid() should assume the node is in a tree that satisfies all properties common to all
     * red-black trees, as assertSubtreeIsValid() is responsible for such checks.  assertNodeIsValid() should be
     * "downward-looking", i.e. it should ignore any information in "parent", and it should be "local", i.e. it should
     * only check a constant number of descendants.  To include "global" checks, such as verifying the BST property
     * concerning ordering, override assertSubtreeIsValid().  assertOrderIsValid is useful for checking the BST
     * property.
     */
    public void assertNodeIsValid() {

    }

    /** Returns whether this is a leaf node. */
    public boolean isLeaf() {
        return left == null;
    }

    /** Returns the root of the tree that contains this node. */
    public N root() {
        @SuppressWarnings("unchecked")
        N node = (N)this;
        while (node.parent != null) {
            node = node.parent;
        }
        return node;
    }

    /** Returns the first node in the subtree rooted at this node, if any. */
    public N min() {
        if (isLeaf()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        N node = (N)this;
        while (!node.left.isLeaf()) {
            node = node.left;
        }
        return node;
    }

    /** Returns the last node in the subtree rooted at this node, if any. */
    public N max() {
        if (isLeaf()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        N node = (N)this;
        while (!node.right.isLeaf()) {
            node = node.right;
        }
        return node;
    }

    /** Returns the node immediately before this in the tree that contains this node, if any. */
    public N predecessor() {
        if (!left.isLeaf()) {
            N node;
            for (node = left; !node.right.isLeaf(); node = node.right);
            return node;
        } else if (parent == null) {
            return null;
        } else {
            @SuppressWarnings("unchecked")
            N node = (N)this;
            while (node.parent != null && node.parent.left == node) {
                node = node.parent;
            }
            return node.parent;
        }
    }

    /** Returns the node immediately after this in the tree that contains this node, if any. */
    public N successor() {
        if (!right.isLeaf()) {
            N node;
            for (node = right; !node.left.isLeaf(); node = node.left);
            return node;
        } else if (parent == null) {
            return null;
        } else {
            @SuppressWarnings("unchecked")
            N node = (N)this;
            while (node.parent != null && node.parent.right == node) {
                node = node.parent;
            }
            return node.parent;
        }
    }

    /**
     * Performs a left rotation about this node. This method assumes that !isLeaf() && !right.isLeaf(). It calls
     * augment() on this node and on its resulting parent. However, it does not call augment() on any of the resulting
     * parent's ancestors, because that is normally the responsibility of the caller.
     * @return The return value from calling augment() on the resulting parent.
     */
    public boolean rotateLeft() {
        if (isLeaf() || right.isLeaf()) {
            throw new IllegalArgumentException("The node or its right child is a leaf");
        }
        N newParent = right;
        right = newParent.left;
        @SuppressWarnings("unchecked")
        N nThis = (N)this;
        if (!right.isLeaf()) {
            right.parent = nThis;
        }
        newParent.parent = parent;
        parent = newParent;
        newParent.left = nThis;
        if (newParent.parent != null) {
            if (newParent.parent.left == this) {
                newParent.parent.left = newParent;
            } else {
                newParent.parent.right = newParent;
            }
        }
        augment();
        return newParent.augment();
    }

    /**
     * Performs a right rotation about this node. This method assumes that !isLeaf() && !left.isLeaf(). It calls
     * augment() on this node and on its resulting parent. However, it does not call augment() on any of the resulting
     * parent's ancestors, because that is normally the responsibility of the caller.
     * @return The return value from calling augment() on the resulting parent.
     */
    public boolean rotateRight() {
        if (isLeaf() || left.isLeaf()) {
            throw new IllegalArgumentException("The node or its left child is a leaf");
        }
        N newParent = left;
        left = newParent.right;
        @SuppressWarnings("unchecked")
        N nThis = (N)this;
        if (!left.isLeaf()) {
            left.parent = nThis;
        }
        newParent.parent = parent;
        parent = newParent;
        newParent.right = nThis;
        if (newParent.parent != null) {
            if (newParent.parent.left == this) {
                newParent.parent.left = newParent;
            } else {
                newParent.parent.right = newParent;
            }
        }
        augment();
        return newParent.augment();
    }

    /**
     * Performs red-black insertion fixup.  To be more precise, this fixes a tree that satisfies all of the requirements
     * of red-black trees, except that this may be a red child of a red node, and if this is the root, the root may be
     * red.  node.isRed must initially be true.  This method assumes that this is not a leaf node.  The method performs
     * any rotations by calling rotateLeft() and rotateRight().  This method is more efficient than fixInsertion if
     * "augment" is false or augment() might return false.
     * @param augment Whether to set the augmentation information for "node" and its ancestors, by calling augment().
     */
    public void fixInsertionWithoutGettingRoot(boolean augment) {
        if (!isRed) {
            throw new IllegalArgumentException("The node must be red");
        }
        boolean changed = augment;
        if (augment) {
            augment();
        }

        RedBlackNode<N> node = this;
        while (node.parent != null && node.parent.isRed) {
            N parent = node.parent;
            N grandparent = parent.parent;
            if (grandparent.left.isRed && grandparent.right.isRed) {
                grandparent.left.isRed = false;
                grandparent.right.isRed = false;
                grandparent.isRed = true;
                if (changed) {
                    changed = parent.augment();
                    if (changed) {
                        changed = grandparent.augment();
                    }
                }
                node = grandparent;
            } else {
                if (parent.left == node) {
                    if (grandparent.right == parent) {
                        parent.rotateRight();
                        node = parent;
                        parent = node.parent;
                    }
                } else if (grandparent.left == parent) {
                    parent.rotateLeft();
                    node = parent;
                    parent = node.parent;
                }
                if (parent.left == node) {
                    boolean grandparentChanged = grandparent.rotateRight();
                    if (augment) {
                        changed = grandparentChanged;
                    }
                } else {
                    boolean grandparentChanged = grandparent.rotateLeft();
                    if (augment) {
                        changed = grandparentChanged;
                    }
                }
                parent.isRed = false;
                grandparent.isRed = true;
                node = parent;
                break;
            }
        }

        if (node.parent == null) {
            node.isRed = false;
        }
        if (changed) {
            for (node = node.parent; node != null; node = node.parent) {
                if (!node.augment()) {
                    break;
                }
            }
        }
    }

    /**
     * Performs red-black insertion fixup.  To be more precise, this fixes a tree that satisfies all of the requirements
     * of red-black trees, except that this may be a red child of a red node, and if this is the root, the root may be
     * red.  node.isRed must initially be true.  This method assumes that this is not a leaf node.  The method performs
     * any rotations by calling rotateLeft() and rotateRight().  This method is more efficient than fixInsertion() if
     * augment() might return false.
     */
    public void fixInsertionWithoutGettingRoot() {
        fixInsertionWithoutGettingRoot(true);
    }

    /**
     * Performs red-black insertion fixup.  To be more precise, this fixes a tree that satisfies all of the requirements
     * of red-black trees, except that this may be a red child of a red node, and if this is the root, the root may be
     * red.  node.isRed must initially be true.  This method assumes that this is not a leaf node.  The method performs
     * any rotations by calling rotateLeft() and rotateRight().
     * @param augment Whether to set the augmentation information for "node" and its ancestors, by calling augment().
     * @return The root of the resulting tree.
     */
    public N fixInsertion(boolean augment) {
        fixInsertionWithoutGettingRoot(augment);
        return root();
    }

    /**
     * Performs red-black insertion fixup.  To be more precise, this fixes a tree that satisfies all of the requirements
     * of red-black trees, except that this may be a red child of a red node, and if this is the root, the root may be
     * red.  node.isRed must initially be true.  This method assumes that this is not a leaf node.  The method performs
     * any rotations by calling rotateLeft() and rotateRight().
     * @return The root of the resulting tree.
     */
    public N fixInsertion() {
        fixInsertionWithoutGettingRoot(true);
        return root();
    }

    /** Returns a Comparator that compares instances of N using their natural order, as in N.compareTo. */
    private Comparator<N> naturalOrder() {
        @SuppressWarnings("unchecked")
        Comparator<N> comparator = (Comparator<N>)NATURAL_ORDER;
        return comparator;
    }

    /**
     * Inserts the specified node into the tree rooted at this node. Assumes this is the root. We treat newNode as a
     * solitary node that does not belong to any tree, and we ignore its initial "parent", "left", "right", and isRed
     * fields.
     *
     * If it is not efficient or convenient for a subclass to find the location for a node using a Comparator, then it
     * should manually add the node to the appropriate location, color it red, and call fixInsertion().
     *
     * @param newNode The node to insert.
     * @param allowDuplicates Whether to insert newNode if there is an equal node in the tree. To check whether we
     *     inserted newNode, check whether newNode.parent is null and the return value differs from newNode.
     * @param comparator A comparator indicating where to put the node. If this is null, we use the nodes' natural
     *     order, as in N.compareTo. If you are passing null, then you must override the compareTo method, because the
     *     default implementation requires the nodes to already be in the same tree.
     * @return The root of the resulting tree.
     */
    public N insert(N newNode, boolean allowDuplicates, Comparator<? super N> comparator) {
        if (parent != null) {
            throw new IllegalArgumentException("This is not the root of a tree");
        }
        @SuppressWarnings("unchecked")
        N nThis = (N)this;
        if (isLeaf()) {
            newNode.isRed = false;
            newNode.left = nThis;
            newNode.right = nThis;
            newNode.parent = null;
            newNode.augment();
            return newNode;
        }
        if (comparator == null) {
            comparator = naturalOrder();
        }

        N node = nThis;
        int comparison;
        while (true) {
            comparison = comparator.compare(newNode, node);
            if (comparison < 0) {
                if (!node.left.isLeaf()) {
                    node = node.left;
                } else {
                    newNode.left = node.left;
                    newNode.right = node.left;
                    node.left = newNode;
                    newNode.parent = node;
                    break;
                }
            } else if (comparison > 0 || allowDuplicates) {
                if (!node.right.isLeaf()) {
                    node = node.right;
                } else {
                    newNode.left = node.right;
                    newNode.right = node.right;
                    node.right = newNode;
                    newNode.parent = node;
                    break;
                }
            } else {
                newNode.parent = null;
                return nThis;
            }
        }
        newNode.isRed = true;
        return newNode.fixInsertion();
    }

    /**
     * Moves this node to its successor's former position in the tree and vice versa, i.e. sets the "left", "right",
     * "parent", and isRed fields of each.  This method assumes that this is not a leaf node.
     * @return The node with which we swapped.
     */
    private N swapWithSuccessor() {
        N replacement = successor();
        boolean oldReplacementIsRed = replacement.isRed;
        N oldReplacementLeft = replacement.left;
        N oldReplacementRight = replacement.right;
        N oldReplacementParent = replacement.parent;

        replacement.isRed = isRed;
        replacement.left = left;
        replacement.right = right;
        replacement.parent = parent;
        if (parent != null) {
            if (parent.left == this) {
                parent.left = replacement;
            } else {
                parent.right = replacement;
            }
        }

        @SuppressWarnings("unchecked")
        N nThis = (N)this;
        isRed = oldReplacementIsRed;
        left = oldReplacementLeft;
        right = oldReplacementRight;
        if (oldReplacementParent == this) {
            parent = replacement;
            parent.right = nThis;
        } else {
            parent = oldReplacementParent;
            parent.left = nThis;
        }

        replacement.right.parent = replacement;
        if (!replacement.left.isLeaf()) {
            replacement.left.parent = replacement;
        }
        if (!right.isLeaf()) {
            right.parent = nThis;
        }
        return replacement;
    }

    /**
     * Performs red-black deletion fixup.  To be more precise, this fixes a tree that satisfies all of the requirements
     * of red-black trees, except that all paths from the root to a leaf that pass through the sibling of this node have
     * one fewer black node than all other root-to-leaf paths.  This method assumes that this is not a leaf node.
     */
    private void fixSiblingDeletion() {
        RedBlackNode<N> sibling = this;
        boolean changed = true;
        boolean haveAugmentedParent = false;
        boolean haveAugmentedGrandparent = false;
        while (true) {
            N parent = sibling.parent;
            if (sibling.isRed) {
                parent.isRed = true;
                sibling.isRed = false;
                if (parent.left == sibling) {
                    changed = parent.rotateRight();
                    sibling = parent.left;
                } else {
                    changed = parent.rotateLeft();
                    sibling = parent.right;
                }
                haveAugmentedParent = true;
                haveAugmentedGrandparent = true;
            } else if (!sibling.left.isRed && !sibling.right.isRed) {
                sibling.isRed = true;
                if (parent.isRed) {
                    parent.isRed = false;
                    break;
                } else {
                    if (changed && !haveAugmentedParent) {
                        changed = parent.augment();
                    }
                    N grandparent = parent.parent;
                    if (grandparent == null) {
                        break;
                    } else if (grandparent.left == parent) {
                        sibling = grandparent.right;
                    } else {
                        sibling = grandparent.left;
                    }
                    haveAugmentedParent = haveAugmentedGrandparent;
                    haveAugmentedGrandparent = false;
                }
            } else {
                if (sibling == parent.left) {
                    if (!sibling.left.isRed) {
                        sibling.rotateLeft();
                        sibling = sibling.parent;
                    }
                } else if (!sibling.right.isRed) {
                    sibling.rotateRight();
                    sibling = sibling.parent;
                }
                sibling.isRed = parent.isRed;
                parent.isRed = false;
                if (sibling == parent.left) {
                    sibling.left.isRed = false;
                    changed = parent.rotateRight();
                } else {
                    sibling.right.isRed = false;
                    changed = parent.rotateLeft();
                }
                haveAugmentedParent = haveAugmentedGrandparent;
                haveAugmentedGrandparent = false;
                break;
            }
        }

        N parent = sibling.parent;
        if (changed && parent != null) {
            if (!haveAugmentedParent) {
                changed = parent.augment();
            }
            if (changed && parent.parent != null) {
                parent = parent.parent;
                if (!haveAugmentedGrandparent) {
                    changed = parent.augment();
                }
                if (changed) {
                    for (parent = parent.parent; parent != null; parent = parent.parent) {
                        if (!parent.augment()) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes this node from the tree that contains it.  The effect of this method on the fields of this node is
     * unspecified.  This method assumes that this is not a leaf node.  This method is more efficient than remove() if
     * augment() might return false.
     *
     * If the node has two children, we begin by moving the node's successor to its former position, by changing the
     * successor's "left", "right", "parent", and isRed fields.
     */
    public void removeWithoutGettingRoot() {
        if (isLeaf()) {
            throw new IllegalArgumentException("Attempted to remove a leaf node");
        }
        N replacement;
        if (left.isLeaf() || right.isLeaf()) {
            replacement = null;
        } else {
            replacement = swapWithSuccessor();
        }

        N child;
        if (!left.isLeaf()) {
            child = left;
        } else if (!right.isLeaf()) {
            child = right;
        } else {
            child = null;
        }

        if (child != null) {
            child.parent = parent;
            if (parent != null) {
                if (parent.left == this) {
                    parent.left = child;
                } else {
                    parent.right = child;
                }
            }
            child.isRed = false;
            if (child.parent != null) {
                N parent;
                for (parent = child.parent; parent != null; parent = parent.parent) {
                    if (!parent.augment()) {
                        break;
                    }
                }
            }
        } else if (parent != null) {
            N leaf = left;
            N parent = this.parent;
            N sibling;
            if (parent.left == this) {
                parent.left = leaf;
                sibling = parent.right;
            } else {
                parent.right = leaf;
                sibling = parent.left;
            }
            if (!isRed) {
                RedBlackNode<N> siblingNode = sibling;
                siblingNode.fixSiblingDeletion();
            } else {
                while (parent != null) {
                    if (!parent.augment()) {
                        break;
                    }
                    parent = parent.parent;
                }
            }
        }

        if (replacement != null) {
            replacement.augment();
            for (N parent = replacement.parent; parent != null; parent = parent.parent) {
                if (!parent.augment()) {
                    break;
                }
            }
        }

        // Clear any previously existing links, so that we're more likely to encounter an exception if we attempt to
        // access the removed node
        parent = null;
        left = null;
        right = null;
        isRed = true;
    }

    /**
     * Removes this node from the tree that contains it.  The effect of this method on the fields of this node is
     * unspecified.  This method assumes that this is not a leaf node.
     *
     * If the node has two children, we begin by moving the node's successor to its former position, by changing the
     * successor's "left", "right", "parent", and isRed fields.
     *
     * @return The root of the resulting tree.
     */
    public N remove() {
        if (isLeaf()) {
            throw new IllegalArgumentException("Attempted to remove a leaf node");
        }

        // Find an arbitrary non-leaf node in the tree other than this node
        N node;
        if (parent != null) {
            node = parent;
        } else if (!left.isLeaf()) {
            node = left;
        } else if (!right.isLeaf()) {
            node = right;
        } else {
            return left;
        }

        removeWithoutGettingRoot();
        return node.root();
    }

    /**
     * Returns the root of a perfectly height-balanced subtree containing the next "size" (non-leaf) nodes from
     * "iterator", in iteration order.  This method is responsible for setting the "left", "right", "parent", and isRed
     * fields of the nodes, and calling augment() as appropriate.  It ignores the initial values of the "left", "right",
     * "parent", and isRed fields.
     * @param iterator The nodes.
     * @param size The number of nodes.
     * @param height The "height" of the subtree's root node above the deepest leaf in the tree that contains it.  Since
     *     insertion fixup is slow if there are too many red nodes and deleteion fixup is slow if there are too few red
     *     nodes, we compromise and have red nodes at every fourth level.  We color a node red iff its "height" is equal
     *     to 1 mod 4.
     * @param leaf The leaf node.
     * @return The root of the subtree.
     */
    private static <N extends RedBlackNode<N>> N createTree(
            Iterator<? extends N> iterator, int size, int height, N leaf) {
        if (size == 0) {
            return leaf;
        } else {
            N left = createTree(iterator, (size - 1) / 2, height - 1, leaf);
            N node = iterator.next();
            N right = createTree(iterator, size / 2, height - 1, leaf);
            node.isRed = height % 4 == 1;
            node.left = left;
            node.right = right;
            if (!left.isLeaf()) {
                left.parent = node;
            }
            if (!right.isLeaf()) {
                right.parent = node;
            }
            node.augment();
            return node;
        }
    }

    /**
     * Returns the root of a perfectly height-balanced tree containing the specified nodes, in iteration order. This
     * method is responsible for setting the "left", "right", "parent", and isRed fields of the nodes (excluding
     * "leaf"), and calling augment() as appropriate. It ignores the initial values of the "left", "right", "parent",
     * and isRed fields.
     * @param nodes The nodes.
     * @param leaf The leaf node.
     * @return The root of the tree.
     */
    public static <N extends RedBlackNode<N>> N createTree(Collection<? extends N> nodes, N leaf) {
        int size = nodes.size();
        if (size == 0) {
            return leaf;
        }
        int height = 0;
        for (int subtreeSize = size; subtreeSize > 0; subtreeSize /= 2) {
            height++;
        }
        N node = createTree(nodes.iterator(), size, height, leaf);
        node.parent = null;
        node.isRed = false;
        return node;
    }

    /**
     * Concatenates to the end of the tree rooted at this node.  To be precise, given that all of the nodes in this
     * precede the node "pivot", which precedes all of the nodes in "last", this returns the root of a tree containing
     * all of these nodes.  This method destroys the trees rooted at "this" and "last".  We treat "pivot" as a solitary
     * node that does not belong to any tree, and we ignore its initial "parent", "left", "right", and isRed fields.
     * This method assumes that this node and "last" are the roots of their respective trees.
     *
     * This method takes O(log N) time.  It is more efficient than inserting "pivot" and then calling concatenate(last).
     * It is considerably more efficient than inserting "pivot" and all of the nodes in "last".
     */
    public N concatenate(N last, N pivot) {
        // If the black height of "first", where first = this, is less than or equal to that of "last", starting at the
        // root of "last", we keep going left until we reach a black node whose black height is equal to that of
        // "first".  Then, we make "pivot" the parent of that node and of "first", coloring it red, and perform
        // insertion fixup on the pivot.  If the black height of "first" is greater than that of "last", we do the
        // mirror image of the above.

        if (parent != null) {
            throw new IllegalArgumentException("This is not the root of a tree");
        }
        if (last.parent != null) {
            throw new IllegalArgumentException("\"last\" is not the root of a tree");
        }

        // Compute the black height of the trees
        int firstBlackHeight = 0;
        @SuppressWarnings("unchecked")
        N first = (N)this;
        for (N node = first; node != null; node = node.right) {
            if (!node.isRed) {
                firstBlackHeight++;
            }
        }
        int lastBlackHeight = 0;
        for (N node = last; node != null; node = node.right) {
            if (!node.isRed) {
                lastBlackHeight++;
            }
        }

        // Identify the children and parent of pivot
        N firstChild = first;
        N lastChild = last;
        N parent;
        if (firstBlackHeight <= lastBlackHeight) {
            parent = null;
            int blackHeight = lastBlackHeight;
            while (blackHeight > firstBlackHeight) {
                if (!lastChild.isRed) {
                    blackHeight--;
                }
                parent = lastChild;
                lastChild = lastChild.left;
            }
            if (lastChild.isRed) {
                parent = lastChild;
                lastChild = lastChild.left;
            }
        } else {
            parent = null;
            int blackHeight = firstBlackHeight;
            while (blackHeight > lastBlackHeight) {
                if (!firstChild.isRed) {
                    blackHeight--;
                }
                parent = firstChild;
                firstChild = firstChild.right;
            }
            if (firstChild.isRed) {
                parent = firstChild;
                firstChild = firstChild.right;
            }
        }

        // Add "pivot" to the tree
        pivot.isRed = true;
        pivot.parent = parent;
        if (parent != null) {
            if (firstBlackHeight < lastBlackHeight) {
                parent.left = pivot;
            } else {
                parent.right = pivot;
            }
        }
        pivot.left = firstChild;
        if (!firstChild.isLeaf()) {
            firstChild.parent = pivot;
        }
        pivot.right = lastChild;
        if (!lastChild.isLeaf()) {
            lastChild.parent = pivot;
        }

        // Perform insertion fixup
        return pivot.fixInsertion();
    }

    /**
     * Concatenates the tree rooted at "last" to the end of the tree rooted at this node.  To be precise, given that all
     * of the nodes in this precede all of the nodes in "last", this returns the root of a tree containing all of these
     * nodes.  This method destroys the trees rooted at "this" and "last".  It assumes that this node and "last" are the
     * roots of their respective trees.  This method takes O(log N) time.  It is considerably more efficient than
     * inserting all of the nodes in "last".
     */
    public N concatenate(N last) {
        if (parent != null || last.parent != null) {
            throw new IllegalArgumentException("The node is not the root of a tree");
        }
        if (isLeaf()) {
            return last;
        } else if (last.isLeaf()) {
            @SuppressWarnings("unchecked")
            N nThis = (N)this;
            return nThis;
        } else {
            N node = last.min();
            last = node.remove();
            return concatenate(last, node);
        }
    }

    /**
     * Splits the tree rooted at this node into two trees, so that the first element of the return value is the root of
     * a tree consisting of the nodes that were before the specified node, and the second element of the return value is
     * the root of a tree consisting of the nodes that were equal to or after the specified node.  This method assumes
     * that this node is the root.  It assumes that this is in the same tree as splitNode.  It takes O(log N) time.  It
     * is considerably more efficient than removing all of the elements after splitNode and then creating a new tree
     * from those nodes.
     * @param The node at which to split the tree.
     * @return An array consisting of the resulting trees.
     */
    public N[] split(N splitNode) {
        // To split the tree, we accumulate a pre-split tree and a post-split tree.  We walk down the tree toward the
        // position where we are splitting.  Whenever we go left, we concatenate the right subtree with the post-split
        // tree, and whenever we go right, we concatenate the pre-split tree with the left subtree.  We use the
        // concatenation algorithm described in concatenate(Object, Object).  For the pivot, we use the last node where
        // we went left in the case of a left move, and the last node where we went right in the case of a right move.
        //
        // The method uses the following variables:
        //
        // node: The current node in our walk down the tree.
        // first: A node on the right spine of the pre-split tree.  At the beginning of each iteration, it is the black
        //     node with the same black height as "node".  If the pre-split tree is empty, this is null instead.
        // firstParent: The parent of "first".  If the pre-split tree is empty, this is null.  Otherwise, this is the
        //     same as first.parent, unless first.isLeaf().
        // firstPivot: The node where we last went right, i.e. the next node to use as a pivot when concatenating with
        //     the pre-split tree.
        // advanceFirst: Whether to set "first" to be its next black descendant at the end of the loop.
        // last, lastParent, lastPivot, advanceLast: Analogous to "first", firstParent, firstPivot, and advanceFirst,
        //     but for the post-split tree.
        if (parent != null) {
            throw new IllegalArgumentException("This is not the root of a tree");
        }

        // Create an array containing the path from the root to splitNode
        int depth = 1;
        N parent;
        for (parent = splitNode; parent.parent != null; parent = parent.parent) {
            depth++;
        }
        if (parent != this) {
            throw new IllegalArgumentException("The split node does not belong to this tree");
        }
        @SuppressWarnings("unchecked")
        N[] path = (N[])Array.newInstance(getClass(), depth);
        for (parent = splitNode; parent != null; parent = parent.parent) {
            depth--;
            path[depth] = parent;
        }

        @SuppressWarnings("unchecked")
        N node = (N)this;
        N first = null;
        N firstParent = null;
        N last = null;
        N lastParent = null;
        N firstPivot = null;
        N lastPivot = null;
        while (!node.isLeaf()) {
            boolean advanceFirst = !node.isRed && firstPivot != null;
            boolean advanceLast = !node.isRed && lastPivot != null;
            if ((depth + 1 < path.length && path[depth + 1] == node.left) || depth + 1 == path.length) {
                // Left move
                if (lastPivot == null) {
                    // The post-split tree is empty
                    last = node.right;
                    last.parent = null;
                    if (last.isRed) {
                        last.isRed = false;
                        lastParent = last;
                        last = last.left;
                    }
                } else {
                    // Concatenate node.right and the post-split tree
                    if (node.right.isRed) {
                        node.right.isRed = false;
                    } else if (!node.isRed) {
                        lastParent = last;
                        last = last.left;
                        if (last.isRed) {
                            lastParent = last;
                            last = last.left;
                        }
                        advanceLast = false;
                    }
                    lastPivot.isRed = true;
                    lastPivot.parent = lastParent;
                    if (lastParent != null) {
                        lastParent.left = lastPivot;
                    }
                    lastPivot.left = node.right;
                    if (!lastPivot.left.isLeaf()) {
                        lastPivot.left.parent = lastPivot;
                    }
                    lastPivot.right = last;
                    if (!last.isLeaf()) {
                        last.parent = lastPivot;
                    }
                    last = lastPivot.left;
                    lastParent = lastPivot;
                    lastPivot.fixInsertionWithoutGettingRoot(false);
                }
                lastPivot = node;
                node = node.left;
            } else {
                // Right move
                if (firstPivot == null) {
                    // The pre-split tree is empty
                    first = node.left;
                    first.parent = null;
                    if (first.isRed) {
                        first.isRed = false;
                        firstParent = first;
                        first = first.right;
                    }
                } else {
                    // Concatenate the post-split tree and node.left
                    if (node.left.isRed) {
                        node.left.isRed = false;
                    } else if (!node.isRed) {
                        firstParent = first;
                        first = first.right;
                        if (first.isRed) {
                            firstParent = first;
                            first = first.right;
                        }
                        advanceFirst = false;
                    }
                    firstPivot.isRed = true;
                    firstPivot.parent = firstParent;
                    if (firstParent != null) {
                        firstParent.right = firstPivot;
                    }
                    firstPivot.right = node.left;
                    if (!firstPivot.right.isLeaf()) {
                        firstPivot.right.parent = firstPivot;
                    }
                    firstPivot.left = first;
                    if (!first.isLeaf()) {
                        first.parent = firstPivot;
                    }
                    first = firstPivot.right;
                    firstParent = firstPivot;
                    firstPivot.fixInsertionWithoutGettingRoot(false);
                }
                firstPivot = node;
                node = node.right;
            }

            depth++;

            // Update "first" and "last" to be the nodes at the proper black height
            if (advanceFirst) {
                firstParent = first;
                first = first.right;
                if (first.isRed) {
                    firstParent = first;
                    first = first.right;
                }
            }
            if (advanceLast) {
                lastParent = last;
                last = last.left;
                if (last.isRed) {
                    lastParent = last;
                    last = last.left;
                }
            }
        }

        // Add firstPivot to the pre-split tree
        N leaf = node;
        if (first == null) {
            first = leaf;
        } else {
            firstPivot.isRed = true;
            firstPivot.parent = firstParent;
            if (firstParent != null) {
                firstParent.right = firstPivot;
            }
            firstPivot.left = leaf;
            firstPivot.right = leaf;
            firstPivot.fixInsertionWithoutGettingRoot(false);
            for (first = firstPivot; first.parent != null; first = first.parent) {
                first.augment();
            }
            first.augment();
        }

        // Add lastPivot to the post-split tree
        if (last == null) {
            last = leaf;
        } else {
            lastPivot.isRed = true;
            lastPivot.parent = lastParent;
            if (lastParent != null) {
                lastParent.left = lastPivot;
            }
            lastPivot.left = leaf;
            lastPivot.right = leaf;
            lastPivot.fixInsertionWithoutGettingRoot(false);
            for (last = lastPivot; last.parent != null; last = last.parent) {
                last.augment();
            }
            last.augment();
        }

        @SuppressWarnings("unchecked")
        N[] result = (N[])Array.newInstance(getClass(), 2);
        result[0] = first;
        result[1] = last;
        return result;
    }

    /**
     * Returns the lowest common ancestor of this node and "other" - the node that is an ancestor of both and is not the
     * parent of a node that is an ancestor of both. Assumes that this is in the same tree as "other". Assumes that
     * neither "this" nor "other" is a leaf node. This method may return "this" or "other".
     *
     * Note that while it is possible to compute the lowest common ancestor in O(P) time, where P is the length of the
     * path from this node to "other", the "lca" method is not guaranteed to take O(P) time. If your application
     * requires this, then you should write your own lowest common ancestor method.
     */
    public N lca(N other) {
        if (isLeaf() || other.isLeaf()) {
            throw new IllegalArgumentException("One of the nodes is a leaf node");
        }

        // Compute the depth of each node
        int depth = 0;
        for (N parent = this.parent; parent != null; parent = parent.parent) {
            depth++;
        }
        int otherDepth = 0;
        for (N parent = other.parent; parent != null; parent = parent.parent) {
            otherDepth++;
        }

        // Go up to nodes of the same depth
        @SuppressWarnings("unchecked")
        N parent = (N)this;
        N otherParent = other;
        if (depth <= otherDepth) {
            for (int i = otherDepth; i > depth; i--) {
                otherParent = otherParent.parent;
            }
        } else {
            for (int i = depth; i > otherDepth; i--) {
                parent = parent.parent;
            }
        }

        // Find the LCA
        while (parent != otherParent) {
            parent = parent.parent;
            otherParent = otherParent.parent;
        }
        if (parent != null) {
            return parent;
        } else {
            throw new IllegalArgumentException("The nodes do not belong to the same tree");
        }
    }

    /**
     * Returns an integer comparing the position of this node in the tree that contains it with that of "other". Returns
     * a negative number if this is earlier, a positive number if this is later, and 0 if this is at the same position.
     * Assumes that this is in the same tree as "other". Assumes that neither "this" nor "other" is a leaf node.
     *
     * The base class's implementation takes O(log N) time. If a RedBlackNode subclass stores a value used to order the
     * nodes, then it could override compareTo to compare the nodes' values, which would take O(1) time.
     *
     * Note that while it is possible to compare the positions of two nodes in O(P) time, where P is the length of the
     * path from this node to "other", the default implementation of compareTo is not guaranteed to take O(P) time. If
     * your application requires this, then you should write your own comparison method.
     */
    @Override
    public int compareTo(N other) {
        if (isLeaf() || other.isLeaf()) {
            throw new IllegalArgumentException("One of the nodes is a leaf node");
        }

        // The algorithm operates as follows: compare the depth of this node to that of "other".  If the depth of
        // "other" is greater, keep moving up from "other" until we find the ancestor at the same depth.  Then, keep
        // moving up from "this" and from that node until we reach the lowest common ancestor.  The node that arrived
        // from the left child of the common ancestor is earlier.  The algorithm is analogous if the depth of "other" is
        // not greater.
        if (this == other) {
            return 0;
        }

        // Compute the depth of each node
        int depth = 0;
        RedBlackNode<N> parent;
        for (parent = this; parent.parent != null; parent = parent.parent) {
            depth++;
        }
        int otherDepth = 0;
        N otherParent;
        for (otherParent = other; otherParent.parent != null; otherParent = otherParent.parent) {
            otherDepth++;
        }

        // Go up to nodes of the same depth
        if (depth < otherDepth) {
            otherParent = other;
            for (int i = otherDepth - 1; i > depth; i--) {
                otherParent = otherParent.parent;
            }
            if (otherParent.parent != this) {
                otherParent = otherParent.parent;
            } else if (left == otherParent) {
                return 1;
            } else {
                return -1;
            }
            parent = this;
        } else if (depth > otherDepth) {
            parent = this;
            for (int i = depth - 1; i > otherDepth; i--) {
                parent = parent.parent;
            }
            if (parent.parent != other) {
                parent = parent.parent;
            } else if (other.left == parent) {
                return -1;
            } else {
                return 1;
            }
            otherParent = other;
        } else {
            parent = this;
            otherParent = other;
        }

        // Keep going up until we reach the lowest common ancestor
        while (parent.parent != otherParent.parent) {
            parent = parent.parent;
            otherParent = otherParent.parent;
        }
        if (parent.parent == null) {
            throw new IllegalArgumentException("The nodes do not belong to the same tree");
        }
        if (parent.parent.left == parent) {
            return -1;
        } else {
            return 1;
        }
    }

    /** Throws a RuntimeException if the RedBlackNode fields of this are not correct for a leaf node. */
    private void assertIsValidLeaf() {
        if (left != null || right != null || parent != null || isRed) {
            throw new RuntimeException("A leaf node's \"left\", \"right\", \"parent\", or isRed field is incorrect");
        }
    }

    /**
     * Throws a RuntimeException if the subtree rooted at this node does not satisfy the red-black properties, excluding
     * the requirement that the root be black, or it contains a repeated node other than a leaf node.
     * @param blackHeight The required number of black nodes in each path from this to a leaf node, including this and
     *     the leaf node.
     * @param visited The nodes we have reached thus far, other than leaf nodes. This method adds the non-leaf nodes in
     *     the subtree rooted at this node to "visited".
     */
    private void assertSubtreeIsValidRedBlack(int blackHeight, Set<Reference<N>> visited) {
        @SuppressWarnings("unchecked")
        N nThis = (N)this;
        if (left == null || right == null) {
            assertIsValidLeaf();
            if (blackHeight != 1) {
                throw new RuntimeException("Not all root-to-leaf paths have the same number of black nodes");
            }
            return;
        } else if (!visited.add(new Reference<N>(nThis))) {
            throw new RuntimeException("The tree contains a repeated non-leaf node");
        } else {
            int childBlackHeight;
            if (isRed) {
                if ((!left.isLeaf() && left.isRed) || (!right.isLeaf() && right.isRed)) {
                    throw new RuntimeException("A red node has a red child");
                }
                childBlackHeight = blackHeight;
            } else if (blackHeight == 0) {
                throw new RuntimeException("Not all root-to-leaf paths have the same number of black nodes");
            } else {
                childBlackHeight = blackHeight - 1;
            }

            if (!left.isLeaf() && left.parent != this) {
                throw new RuntimeException("left.parent != this");
            }
            if (!right.isLeaf() && right.parent != this) {
                throw new RuntimeException("right.parent != this");
            }
            RedBlackNode<N> leftNode = left;
            RedBlackNode<N> rightNode = right;
            leftNode.assertSubtreeIsValidRedBlack(childBlackHeight, visited);
            rightNode.assertSubtreeIsValidRedBlack(childBlackHeight, visited);
        }
    }

    /** Calls assertNodeIsValid() on every node in the subtree rooted at this node. */
    private void assertNodesAreValid() {
        assertNodeIsValid();
        if (left != null) {
            RedBlackNode<N> leftNode = left;
            RedBlackNode<N> rightNode = right;
            leftNode.assertNodesAreValid();
            rightNode.assertNodesAreValid();
        }
    }

    /**
     * Throws a RuntimeException if the subtree rooted at this node is not a valid red-black tree, e.g. if a red node
     * has a red child or it contains a non-leaf node "node" for which node.left.parent != node. (If parent != null,
     * it's okay if isRed is true.) This method is useful for debugging. See also assertSubtreeIsValid().
     */
    public void assertSubtreeIsValidRedBlack() {
        if (isLeaf()) {
            assertIsValidLeaf();
        } else {
            if (parent == null && isRed) {
                throw new RuntimeException("The root is red");
            }

            // Compute the black height of the tree
            Set<Reference<N>> nodes = new HashSet<Reference<N>>();
            int blackHeight = 0;
            @SuppressWarnings("unchecked")
            N node = (N)this;
            while (node != null) {
                if (!nodes.add(new Reference<N>(node))) {
                    throw new RuntimeException("The tree contains a repeated non-leaf node");
                }
                if (!node.isRed) {
                    blackHeight++;
                }
                node = node.left;
            }

            assertSubtreeIsValidRedBlack(blackHeight, new HashSet<Reference<N>>());
        }
    }

    /**
     * Throws a RuntimeException if we detect a problem with the subtree rooted at this node, such as a red child of a
     * red node or a non-leaf descendant "node" for which node.left.parent != node.  This method is useful for
     * debugging.  RedBlackNode subclasses may want to override assertSubtreeIsValid() to call assertOrderIsValid.
     */
    public void assertSubtreeIsValid() {
        assertSubtreeIsValidRedBlack();
        assertNodesAreValid();
    }

    /**
     * Throws a RuntimeException if the nodes in the subtree rooted at this node are not in the specified order or they
     * do not lie in the specified range.  Assumes that the subtree rooted at this node is a valid binary tree, i.e. it
     * has no repeated nodes other than leaf nodes.
     * @param comparator A comparator indicating how the nodes should be ordered.
     * @param start The lower limit for nodes in the subtree, if any.
     * @param end The upper limit for nodes in the subtree, if any.
     */
    private void assertOrderIsValid(Comparator<? super N> comparator, N start, N end) {
        if (!isLeaf()) {
            @SuppressWarnings("unchecked")
            N nThis = (N)this;
            if (start != null && comparator.compare(nThis, start) < 0) {
                throw new RuntimeException("The nodes are not ordered correctly");
            }
            if (end != null && comparator.compare(nThis, end) > 0) {
                throw new RuntimeException("The nodes are not ordered correctly");
            }
            RedBlackNode<N> leftNode = left;
            RedBlackNode<N> rightNode = right;
            leftNode.assertOrderIsValid(comparator, start, nThis);
            rightNode.assertOrderIsValid(comparator, nThis, end);
        }
    }

    /**
     * Throws a RuntimeException if the nodes in the subtree rooted at this node are not in the specified order.
     * Assumes that this is a valid binary tree, i.e. there are no repeated nodes other than leaf nodes.  This method is
     * useful for debugging.  RedBlackNode subclasses may want to override assertSubtreeIsValid() to call
     * assertOrderIsValid.
     * @param comparator A comparator indicating how the nodes should be ordered.  If this is null, we use the nodes'
     *     natural order, as in N.compareTo.
     */
    public void assertOrderIsValid(Comparator<? super N> comparator) {
        if (comparator == null) {
            comparator = naturalOrder();
        }
        assertOrderIsValid(comparator, null, null);
    }
}
