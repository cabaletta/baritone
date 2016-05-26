package com.github.btrekkie.tree_list;

import com.github.btrekkie.red_black_node.RedBlackNode;

/** A node in a TreeList.  See the comments for TreeList. */
class TreeListNode<T> extends RedBlackNode<TreeListNode<T>> {
    /** The element stored in the node.  The value is unspecified if this is a leaf node. */
    public T value;

    /** The number of elements in the subtree rooted at this node. */
    public int size;

    public TreeListNode(T value) {
        this.value = value;
    }

    @Override
    public boolean augment() {
        int newSize = left.size + right.size + 1;
        if (newSize == size) {
            return false;
        } else {
            size = newSize;
            return true;
        }
    }

    @Override
    public void assertNodeIsValid() {
        int expectedSize;
        if (isLeaf()) {
            expectedSize = 0;
        } else {
            expectedSize = left.size + right.size + 1;
        }
        if (size != expectedSize) {
            throw new RuntimeException("The node's size does not match that of the children");
        }
    }
}
