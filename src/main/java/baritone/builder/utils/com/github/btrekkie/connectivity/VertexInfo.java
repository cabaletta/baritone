/*
 * This file was originally written by btrekkie under the MIT license, which is compatible with the LGPL license for this usage within Baritone
 * https://github.com/btrekkie/dynamic-connectivity/
 */

package baritone.builder.utils.com.github.btrekkie.connectivity;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

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
    public Long2ObjectOpenHashMap<ConnEdge> edges = new Long2ObjectOpenHashMap<>();

    public VertexInfo(EulerTourVertex vertex) {
        this.vertex = vertex;
    }
}
