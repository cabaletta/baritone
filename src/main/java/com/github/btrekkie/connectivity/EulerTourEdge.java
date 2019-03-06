package com.github.btrekkie.connectivity;

/**
 * The representation of a forest edge in some Euler tour forest F_i at some particular level i. Each forest edge has
 * one EulerTourEdge object for each level it appears in. See the comments for the implementation of ConnGraph.
 */
class EulerTourEdge {
    /**
     * One of the two visits preceding the edge in the Euler tour, in addition to visit2. (The node is at the same level
     * as the EulerTourEdge.)
     */
    public final EulerTourNode visit1;

    /**
     * One of the two visits preceding the edge in the Euler tour, in addition to visit1. (The node is at the same level
     * as the EulerTourEdge.)
     */
    public final EulerTourNode visit2;

    /**
     * The representation of this edge in the next-higher level. higherEdge is null if this edge is in the highest
     * level.
     */
    public EulerTourEdge higherEdge;

    public EulerTourEdge(EulerTourNode visit1, EulerTourNode visit2) {
        this.visit1 = visit1;
        this.visit2 = visit2;
    }
}
