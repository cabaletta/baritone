# Description
`RedBlackNode` is a Java implementation of red-black trees. Compared to a class
like Java's `TreeMap`, `RedBlackNode` is a low-level data structure. The
internals of each node are exposed as public fields, allowing clients to
directly observe and manipulate the structure of the tree. This gives clients
flexibility, although it also enables them to violate the red-black or BST
properties. The `RedBlackNode` class provides methods for performing various
standard operations, such as insertion and removal.

Unlike most implementations of binary search trees, `RedBlackNode` supports
arbitrary augmentation. By subclassing `RedBlackNode`, clients can add arbitrary
data and augmentation information to each node.

# Features
* Supports min, max, root, predecessor, successor, insert, remove, rotate,
  split, concatenate, create balanced tree, LCA, and compare operations. The
  running time of each operation has optimal big O bounds.
* Supports arbitrary augmentation by overriding `augment()`. Examples of
  augmentation are the number of non-leaf nodes in a subtree and the sum of the
  values in a subtree. All `RedBlackNode` methods (such as `insert` and
  `remove()`) call `augment()` as necessary to correctly maintain the
  augmentation information, unless otherwise indicated in their comments.
* The parent and child links and the color are public fields. This gives clients
  flexibility, although it also enables them to violate the red-black or BST
  properties.
* "Assert is valid" methods allow clients to check for errors in the structure
  or contents of a red-black tree. This is useful for debugging.
* As a bonus (a proof of concept and a test case), this includes the `TreeList`
  class, a `List` implementation backed by a red-black tree augmented by subtree
  size.
* Compatible with Java 6.0 and above.

# Limitations
* The values of the tree must be stored in the non-leaf nodes. `RedBlackNode`
  does not support use cases where the values must be stored in the leaf nodes.
  (Note that many data structures can be implemented with either approach.)
* Augmentations that depend on information stored in a node's ancestors are not
  (easily) supported. For example, augmenting each node with the number of nodes
  in the left subtree is not (easily and efficiently) supported, because in
  order to perform a right rotation, we would need to use the parent's
  augmentation information. However, `RedBlackNode` supports augmenting each
  node with the number of nodes in the subtree, which is basically equivalent.
* The running time of each operation has optimal big O bounds. However, beyond
  this, no special effort has been made to optimize performance.

# Example usage
```java
class Node<T> extends RedBlackNode<Node<T>> {
    /** The value we are storing in the node. */
    public final T value;

    /** The number of nodes in this subtree. */
    public int size;

    public Node(T value) {
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
}
```

```java
/** Stores a set of distinct values. */
public class Tree<T extends Comparable<? super T>> {
    /** The dummy leaf node. */
    private final Node<T> leaf = new Node<T>(null);

    private Node<T> root = leaf;

    public void add(T value) {
        // A comparator telling "insert" where to put the new node
        Comparator<Node<T>> comparator = new Comparator<Node<T>>() {
            public int compare(Node<T> node1, Node<T> node2) {
                return node1.value.compareTo(node2.value);
            }
        };

        Node<T> newNode = new Node<T>(value);
        root = root.insert(newNode, false, comparator);
    }

    /** Returns the node containing the specified value, if any. */
    private Node<T> find(T value) {
        Node<T> node = root;
        while (!node.isLeaf()) {
            int c = value.compareTo(node.value);
            if (c == 0) {
                return node;
            } else if (c < 0) {
                node = node.left;
            } else {
                node = node.right;
            }
        }
        return null;
    }

    public boolean contains(T value) {
        return find(value) != null;
    }

    public void remove(T value) {
        Node<T> node = find(value);
        if (node != null) {
            root = node.remove();
        }
    }

    /** Returns the (rank + 1)th node in the subtree rooted at "node". */
    private Node<T> getNodeWithRank(Node<T> node, int rank) {
        if (rank < 0 || rank >= node.size) {
            throw new IndexOutOfBoundsException();
        }
        if (rank == node.left.size) {
            return node;
        } else if (rank < node.left.size) {
            return getNodeWithRank(node.left, rank);
        } else {
            return getNodeWithRank(node.right, rank - node.left.size - 1);
        }
    }

    /** Returns the (rank + 1)th-smallest value in the tree. */
    public T getItemWithRank(int rank) {
        return getNodeWithRank(root, rank).value;
    }
}
```

# Documentation
See <https://btrekkie.github.io/RedBlackNode/index.html> for API documentation.
