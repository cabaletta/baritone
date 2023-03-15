package com.github.btrekkie.connectivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes a ConnVertex, with respect to a particular ConnGraph. There is exactly one VertexInfo object per vertex in
 * a given graph, regardless of how many levels the vertex is in. See the comments for the implementation of ConnGraph.
 */
class VertexInfo {
    /**
     * The representation of the vertex in the highest level.
     */
    public EulerTourVertex vertex;

    /**
     * A map from each ConnVertex adjacent to this vertex to the ConnEdge object for the edge connecting it to this
     * vertex. Lookups take O(1) expected time and O(log N / log log N) time with high probability, because "edges" is a
     * HashMap, and ConnVertex.hashCode() returns a random integer.
     */
    public Map<ConnVertex, ConnEdge> edges = new HashMap<ConnVertex, ConnEdge>();

    public VertexInfo(EulerTourVertex vertex) {
        this.vertex = vertex;
    }
}
