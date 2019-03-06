package com.github.btrekkie.sub_array_min;

/** A list of integers.  SubArrayMin provides the ability to quickly determine the minimum value in a given sublist. */
/* We implement SubArrayMin using a red-black tree augmented by subtree size and minimum value.  Using the subtree size
 * augmentation, we can find the node at a given index.
 */
public class SubArrayMin {
    /** The root node. */
    private SubArrayMinNode root = SubArrayMinNode.LEAF;

    /** Appends the specified value to the end of the list. */
    public void add(int value) {
        SubArrayMinNode newNode = new SubArrayMinNode(value);
        newNode.left = SubArrayMinNode.LEAF;
        newNode.right = SubArrayMinNode.LEAF;
        if (root.isLeaf()) {
            root = newNode;
            newNode.augment();
        } else {
            SubArrayMinNode node = root.max();
            node.right = newNode;
            newNode.parent = node;
            newNode.isRed = true;
            root = newNode.fixInsertion();
        }
    }

    /** Returns the node for the element with the specified index.  Assumes "index" is in the range [0, root.size). */
    private SubArrayMinNode getNode(int index) {
        if (index < 0 || index >= root.size) {
            throw new IndexOutOfBoundsException("Index " + index + " is not in the range [0, " + root.size + ")");
        }
        int rank = index;
        SubArrayMinNode node = root;
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

    /**
     * Returns the minimum value in the subarray starting at index startIndex and ending at index endIndex - 1,
     * inclusive.  Assumes startIndex < endIndex, and assumes this contains indices startIndex and endIndex - 1.
     */
    public int min(int startIndex, int endIndex) {
        if (startIndex >= endIndex) {
            throw new IllegalArgumentException("The start index must be less than the end index");
        }
        SubArrayMinNode start = getNode(startIndex);
        SubArrayMinNode end = getNode(endIndex - 1);
        SubArrayMinNode lca = start.lca(end);

        int min = Math.min(lca.value, Math.min(start.value, end.value));
        if (start != lca) {
            if (start.right.min < min) {
                min = start.right.min;
            }
            for (SubArrayMinNode node = start; node.parent != lca; node = node.parent) {
                if (node.parent.left == node) {
                    if (node.parent.value < min) {
                        min = node.parent.value;
                    }
                    if (node.parent.right.min < min) {
                        min = node.parent.right.min;
                    }
                }
            }
        }
        if (end != lca) {
            if (end.left.min < min) {
                min = end.left.min;
            }
            for (SubArrayMinNode node = end; node.parent != lca; node = node.parent) {
                if (node.parent.right == node) {
                    if (node.parent.value < min) {
                        min = node.parent.value;
                    }
                    if (node.parent.left.min < min) {
                        min = node.parent.left.min;
                    }
                }
            }
        }
        return min;
    }
}
