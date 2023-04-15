package baritone.builder.utils.com.github.btrekkie.connectivity;

/**
 * A combining function for taking the augmentations associated with a set of ConnVertices and reducing them to a single
 * result. MutatingAugmentation has a binary operation "combine" that takes two values, combines them using a function
 * C, and stores the result in a mutable object. For example:
 * <p>
 * class IntWrapper {
 * public int value;
 * }
 * <p>
 * class Sum implements MutatingAugmentation {
 * public void combine(Object value1, Object value2, Object result) {
 * ((IntWrapper)result).value = ((IntWrapper)value1).value + ((IntWrapper)value2).value;
 * }
 * <p>
 * public Object newAugmentation() {
 * return new IntWrapper();
 * }
 * }
 * <p>
 * Given vertices with augmentations A1, A2, A3, and A4, the combined result may be obtained by computing
 * C(C(C(A1, A2), A3), A4). In order for an augmentation result to be meaningful, the combining function must be
 * commutative, meaning C(x, y) is equivalent to C(y, x), and associative, meaning C(x, C(y, z)) is equivalent to
 * C(C(x, y), z).
 * <p>
 * If a ConnGraph represents a game map, then one example of an augmentation would be the amount of gold accessible from
 * a certain location. Each vertex would be augmented with the amount of gold in that location, and the combining
 * function would add the two amounts of gold passed in as arguments. Another example would be the strongest monster
 * that can reach a particular location. Each vertex with at least one monster would be augmented with a reference to
 * the strongest monster at that location, and the combining function would take the stronger of the two monsters passed
 * in as arguments. A third example would be the number of locations accessible from a given vertex. Each vertex would
 * be augmented with the number 1, and the combining function would add the two numbers of vertices passed in as
 * arguments.
 * <p>
 * ConnGraph treats two augmentation values X and Y as interchangeable if they are equal, as in
 * X != null ? X.equals(Y) : Y == null. The same holds for two combined augmentation values, and for one combined
 * augmentation value and one augmentation value.
 * <p>
 * See the comments for ConnGraph.
 */
public interface MutatingAugmentation {
    /**
     * Computes the result of combining value1 and value2 into one, and stores it in "result".
     */
    public void combine(Object value1, Object value2, Object result);

    /**
     * Constructs and returns a new augmentation object that may subsequently be passed to "combine". The initial
     * contents of the object are ignored.
     */
    public Object newAugmentation();
}
