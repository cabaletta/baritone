package com.github.btrekkie.connectivity;

/**
 * A combining function for taking the augmentations associated with a set of ConnVertices and reducing them to a single
 * result. The function is a binary operation for combining two values into one. For example, given vertices with
 * augmentations A1, A2, A3, and A4, the combined result may be obtained by computing
 * combine(combine(combine(A1, A2), A3), A4). In order for an augmentation result to be meaningful, the combining
 * function must be commutative, meaning combine(x, y) is equivalent to combine(y, x), and associative, meaning
 * combine(x, combine(y, z)) is equivalent to combine(combine(x, y), z).
 *
 * If a ConnGraph represents a game map, then one example of an augmentation would be the amount of gold accessible from
 * a certain location. Each vertex would be augmented with the amount of gold in that location, and the combining
 * function would add the two amounts of gold passed in as arguments. Another example would be the strongest monster
 * that can reach a particular location. Each vertex with at least one monster would be augmented with a pointer to the
 * strongest monster at that location, and the combining function would return the stronger of the two monsters passed
 * in as arguments. A third example would be the number of locations accessible from a given vertex. Each vertex would
 * be augmented with the number 1, and the combining function would add the two numbers of vertices passed in as
 * arguments.
 *
 * ConnGraph treats two augmentation values X and Y as interchangeable if they are equal, as in
 * X != null ? X.equals(Y) : Y == null. The same holds for two combined augmentation values, and for one combined
 * augmentation value and one augmentation value.
 *
 * See the comments for ConnGraph.
 */
public interface Augmentation {
    /**
     * Returns the result of combining the specified values into one. Each argument is either the augmentation
     * information associated with a vertex, or the result of a previous call to "combine".
     *
     * Note that a value of null is never passed in to indicate the absence of augmentation information. The fact that
     * ConnGraph.getVertexAugmentation, for example, may return null when there is no associated augmentation might lead
     * you to believe that a null argument indicates the absence of augmentation information, but again, it does not. A
     * null argument can only mean that a vertex is explicitly associated with null augmentation information, due to a
     * prior call to ConnGraph.setVertexAugmentation(vertex, null), or that the "combine" method previously returned
     * null.
     */
    public Object combine(Object value1, Object value2);
}
