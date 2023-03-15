package com.github.btrekkie.connectivity;

/**
 * The representation of a ConnVertex at some particular level i. Each vertex has one EulerTourVertex object for each
 * level it appears in. Note that different vertices may appear in different numbers of levels, as EulerTourVertex
 * objects are only created for lower levels as needed. See the comments for the implementation of ConnGraph.
 */
class EulerTourVertex {
    /**
     * The representation of this edge in the next-lower level. lowerVertex is null if this is the lowest-level
     * representation of this vertex.
     */
    public EulerTourVertex lowerVertex;

    /**
     * The representation of this edge in the next-higher level. This is null if this vertex is in the highest level.
     */
    public EulerTourVertex higherVertex;

    /**
     * An arbitrarily selected visit to the vertex in the Euler tour tree that contains it (at the same level as this).
     */
    public EulerTourNode arbitraryVisit;

    /**
     * The first edge in the linked list of level-i edges that are adjacent to the vertex in G_i, but are not in the
     * Euler tour forest F_i, where i is the level of the vertex. Note that this list excludes any edges that also
     * appear in lower levels.
     */
    public ConnEdge graphListHead;

    /**
     * The first edge in the linked list of level-i edges adjacent to the vertex that are in F_i, where i is the level
     * of the vertex. Note that this list excludes any edges that also appear in lower levels.
     */
    public ConnEdge forestListHead;

    /**
     * The augmentation associated with this vertex, if any. This is null instead if higherVertex != null.
     */
    public Object augmentation;

    /**
     * Whether there is any augmentation associated with this vertex. This is false instead if higherVertex != null.
     */
    public boolean hasAugmentation;
}
