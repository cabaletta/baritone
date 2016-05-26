# RedBlackNode
`RedBlackNode` is a Java implementation of red-black trees.  By subclassing
`RedBlackNode`, clients can add arbitrary data and augmentation information to
each node.  (self-balancing binary search tree, self-balancing BST, augment,
augmented)

# Features
* Supports min, max, root, predecessor, successor, insert, remove, rotate,
  split, concatenate, create balanced tree, and compare operations.  The running
  time of each operation has optimal big O bounds.
* Supports arbitrary augmentation by overriding `augment()`.  Examples of
  augmentation are the number of non-leaf nodes in a subtree and the sum of the
  values in a subtree.
* The parent and child links and the color are public fields.  This gives
  clients flexibility.  However, it is possible for a client to violate the
  red-black or BST properties.
* "Assert is valid" methods allow clients to check for errors in the structure
  or contents of a red-black tree.  This is useful for debugging.
* As a bonus (a proof of concept and a test case), this includes the `TreeList`
  class, a `List` implementation backed by a red-black tree augmented by subtree
  size.
* Tested in Java 6.0 and 7.0.  It might also work in Java 5.0.

# Limitations
* Augmentations that depend on information stored in a node's ancestors are not
  (easily) supported.  For example, augmenting each node with the number of
  nodes in the left subtree is not (easily and efficiently) supported, because
  in order to perform a right rotation, we would need to use the parent's
  augmentation information.  However, `RedBlackNode` supports augmenting each
  node with the number of nodes in the subtree, which is basically equivalent.
* The running time of each operation has optimal big O bounds.  However, beyond
  this, no special effort has been made to optimize performance.

# Example
<pre lang="java">
/** Red-black tree augmented by the sum of the values in the subtree. */
public class SumNode extends RedBlackNode&lt;SumNode&gt; {
    public int value;
    public int sum;

    public SumNode(int value) {
        this.value = value;
    }

    @Override
    public boolean augment() {
        int newSum = value + left.sum + right.sum;
        if (newSum == sum) {
            return false;
        } else {
            sum = newSum;
            return true;
        }
    }
}
</pre>

# Documentation
For more detailed instructions, check the source code to see the full API and
Javadoc documentation.
