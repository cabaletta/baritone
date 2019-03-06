package com.github.btrekkie.connectivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes a ConnVertex, with respect to a particular ConnGraph. There is exactly one VertexInfo object per vertex in
 * a given graph, regardless of how many levels the vertex is in. See the comments for the implementation of ConnGraph.
 */
class VertexInfo {
    /** The representation of the vertex in the highest level. */
    public EulerTourVertex vertex;

    /**
     * A map from each ConnVertex adjacent to this vertex to the ConnEdge object for the edge connecting it to this
     * vertex. Lookups take O(1) expected time and O(log N / log log N) time with high probability, because "edges" is a
     * HashMap, and ConnVertex.hashCode() returns a random integer.
     */
    public Map<ConnVertex, ConnEdge> edges = new HashMap<ConnVertex, ConnEdge>();

    /**
     * The maximum number of entries in "edges" since the last time we "rebuilt" that field. When the number of edges
     * drops sufficiently, we rebuild "edges" by copying its contents to a new HashMap. We do this to ensure that
     * "edges" uses O(K) space, where K is the number of vertices adjacent to this. (The capacity of a HashMap is not
     * automatically reduced as the number of entries decreases, so we have to limit space usage manually.)
     */
    public int maxEdgeCountSinceRebuild;

    public VertexInfo(EulerTourVertex vertex) {
        this.vertex = vertex;
    }
}
