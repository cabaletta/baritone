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
    public Long2ObjectOpenHashMap<ConnEdge> edges = new Long2ObjectOpenHashMap<>(); // TODO in my use case, each vertex will have at most 4 edges (at most one each to north, south, east, west). there's probably a MASSIVE performance improvement to be had by simply switching this to such an array. it might require some bitwise twiddling because the key is a long so we'd have to subtract xyz, but, it would work pretty easily just by copying the Face.horizontalIndex approach (x & 1 | (x | z) & 2)

    public VertexInfo(EulerTourVertex vertex) {
        this.vertex = vertex;
    }
}
