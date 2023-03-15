package com.github.btrekkie.connectivity;

import java.util.*;

/**
 * Implements an undirected graph with dynamic connectivity. It supports adding and removing edges and determining
 * whether two vertices are connected - whether there is a path between them. Adding and removing edges take O(log^2 N)
 * amortized time with high probability, while checking whether two vertices are connected takes O(log N) time with high
 * probability. It uses O(V log V + E) space, where V is the number of vertices and E is the number of edges. Note that
 * a ConnVertex may appear in multiple ConnGraphs, with a different set of adjacent vertices in each graph.
 * <p>
 * ConnGraph optionally supports arbitrary augmentation. Each vertex may have an associated augmentation, or value.
 * Given a vertex V, ConnGraph can quickly report the result of combining the augmentations of all of the vertices in
 * the connected component containing V, using a combining function provided to the constructor. For example, if a
 * ConnGraph represents a game map, then given the location of the player, we can quickly determine the amount of gold
 * the player can access, or the strongest monster that can reach him. Augmentation does not affect the running time or
 * space of ConnGraph in terms of big O notation, assuming the augmentation function takes a constant amount of time and
 * the augmentation takes a constant amount of space. Retrieving the combined augmentation for a connected component
 * takes O(log N) time with high probability. (Although ConnGraph does not directly support augmenting edges, this can
 * also be accomplished, by imputing each edge's augmentation to an adjacent vertex.)
 * <p>
 * When a vertex no longer has any adjacent edges, and it has no augmentation information, ConnGraph stops keeping track
 * of the vertex. This reduces the time and space bounds of the ConnGraph, and it enables the ConnVertex to be garbage
 * collected. If you know you are finished with a vertex, and that vertex has an augmentation, then you should call
 * removeVertexAugmentation on the vertex, so that the graph can release it.
 * <p>
 * As a side note, it would be more proper if ConnGraph had a generic type parameter indicating the type of the
 * augmentation values. However, it is expected that it is more common not to use augmentation, so by not using a type
 * parameter, we make usage of the ConnGraph class more convenient and less confusing in the common case.
 */
/* ConnGraph is implemented using a data structure described in
 * http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.89.919&rep=rep1&type=pdf (Holm, de Lichtenberg, and Thorup
 * (1998): Poly-Logarithmic Deterministic Fully-Dynamic Algorithms for Connectivity, Minimum Spanning Tree, 2-Edge, and
 * Biconnectivity). However, ConnGraph does not include the optimization of using a B-tree in the top level, so queries
 * take O(log N) time rather than O(log N / log log N) time.
 *
 * This implementation is actually based on a slightly modified description of the data structure given in
 * https://ocw.mit.edu/courses/6-851-advanced-data-structures-spring-2012/resources/session-20-dynamic-graphs-ii/ . The
 * description in the video differs from the data structure in the paper in that the levels are numbered in reverse
 * order, the constraint on tree sizes is different, and the augmentation uses booleans in place of edges. In addition,
 * the video defines subgraphs G_i. The change in the constraint on tree sizes is beneficial because it makes it easier
 * to delete vertices.
 *
 * Note that the data structure described in the video is faulty. In the procedure for deleting an edge, it directs us
 * to push down some edges. When we push an edge from level i to level i - 1, we would need to add the edge to
 * F_{i - 1}, if the endpoints were not already connected in G_{i - 1}. However, this would violate the invariant that
 * F_i be a superset of F_{i - 1}. To fix this problem, before searching for a replacement edge in level i, ConnGraph
 * first pushes all level-i edges in the relevant tree down to level i - 1 and adds them to F_{i - 1}, as in the
 * original paper. That way, when we subsequently push down edges, we can safely add them to G_{i - 1} without also
 * adding them to F_{i - 1}. In order to do this efficiently, each vertex stores a second adjacency list, consisting of
 * the level-i edges that are in F_i. In addition, we augment each Euler tour tree node with an a second boolean,
 * indicating whether the subtree rooted at the node contains a canonical visit to a vertex with at least one level-i
 * edge that is in F_i.
 *
 * The implementation of rerooting an Euler tour tree described in the video lecture appears to be incorrect as well. It
 * breaks the references to the vertices' first and last visits. To fix this, we do not store references to the
 * vertices' first and last visits. Instead, we have each vertex store a reference to an arbitrary visit to that vertex.
 * We also maintain edge objects for each of the edges in the Euler tours. Each such edge stores a pointer to the two
 * visits that precede the traversal of the edge in the Euler tour. These do not change when we perform a reroot. The
 * remove edge operation then requires a pointer to the edge object, rather than pointers to the vertices. Given the
 * edge object, we can splice out the range of nodes between the two visits that precede the edge.
 *
 * Rather than explicitly giving each edge a level number, the level numbers are implicit through links from each level
 * to the level below it. For purposes of analysis, the level number of the top level is equal to
 * maxLogVertexCountSinceRebuild, the ceiling of log base 2 of the maximum number of vertices in the graph since the
 * last rebuild operation. Once the ratio between the maximum number of vertices since the last rebuild and the current
 * number of vertices becomes large enough, we rebuild the data structure. This ensures that the level number of the top
 * level is O(log V).
 *
 * Most methods' time bounds are probabilistic. For example, "connected" takes O(log N) time with high probability. The
 * reason they are probabilistic is that they involve hash lookups, using the vertexInfo and VertexInfo.edges hash maps.
 * Given that each ConnVertex has a random hash code, it is easy to demonstrate that lookups take O(1) expected time.
 * Furthermore, I claim that they take O(log N / log log N) time with high probability. This claim is sufficient to
 * establish that all time bounds that are at least O(log N / log log N) if we exclude hash lookups can be sustained if
 * we add the qualifier "with high probability."
 *
 * This claim is based on information presented in
 * https://ocw.mit.edu/courses/electrical-engineering-and-computer-science/6-851-advanced-data-structures-spring-2012/lecture-videos/session-10-dictionaries/ .
 * According to that video, in a hash map with chaining, if the hash function is totally random, then the longest chain
 * length is O(log N / log log N) with high probability. A totally random hash function is a slightly different concept
 * than having ConnVertex.hashCode() return a random value, due to the particular definition of "hash function" used in
 * the video. Nevertheless, the analysis is the same. A random hashCode() implementation ultimately results in
 * independently hashing each entry to a random bucket, which is equivalent to a totally random hash function.
 *
 * However, the claim depends on certain features of the implementation of HashMap, gleaned from reading the source
 * code. In particular, it assumes that HashMap resolves collisions using chaining. (Newer versions of Java sometimes
 * store the entries that hash to the same bucket in binary search trees rather than linked lists, but this can't hurt
 * the asymptotic performance.) Note that the implementation of HashMap transforms the return value of hashCode(), by
 * "spreading" the higher-order bits to lower-order positions. However, this transform is a permutation of the integers.
 * If the input to a transform is selected uniformly at random, and the transform is a permutation, than the output also
 * has a uniform random distribution.
 */
public class ConnGraph {
    /**
     * The difference between ceiling of log base 2 of the maximum number of vertices in the graph since the last call
     * to rebuild() or clear() and ceiling of log base 2 of the current number of vertices, at or above which we call
     * rebuild(). (There is special handling for 0 vertices.)
     */
    private static final int REBUILD_CHANGE = 2;

    /**
     * The maximum number of vertices we can store in a ConnGraph. This is limited by the fact that EulerTourNode.size
     * is an int. Since the size of an Euler tour tree is one less than twice the number of vertices in the tree, the
     * number of vertices may be at most (int)((((long)Integer.MAX_VALUE) + 1) / 2).
     * <p>
     * Of course, we could simply change the "size" field to be a long. But more fundamentally, the number of vertices
     * is limited by the fact that vertexInfo and VertexInfo.edges use HashMaps. Using a HashMap becomes problematic at
     * around Integer.MAX_VALUE entries. HashMap buckets entries based on 32-bit hash codes, so in principle, it can
     * only hash the entries to at most 2^32 buckets. In order to support a significantly greater limit on the number of
     * vertices, we would need to use a more elaborate mapping approach.
     */
    private static final int MAX_VERTEX_COUNT = 1 << 30;

    /**
     * The augmentation function for the graph, if any.
     */
    private final Augmentation augmentation;

    /**
     * A map from each vertex in this graph to information about the vertex in this graph. If a vertex has no adjacent
     * edges and no associated augmentation, we remove it from vertexInfo, to save time and space. Lookups take O(1)
     * expected time and O(log N / log log N) time with high probability, because vertexInfo is a HashMap, and
     * ConnVertex.hashCode() returns a random integer.
     */
    private Map<ConnVertex, VertexInfo> vertexInfo = new HashMap<ConnVertex, VertexInfo>();

    /**
     * Ceiling of log base 2 of the maximum number of vertices in this graph since the last rebuild. This is 0 if that
     * number is 0.
     */
    private int maxLogVertexCountSinceRebuild;

    /**
     * Constructs a new ConnGraph with no augmentation.
     */
    public ConnGraph() {
        augmentation = null;
    }

    /**
     * Constructs an augmented ConnGraph, using the specified function to combine augmentation values.
     */
    public ConnGraph(Augmentation augmentation) {
        this.augmentation = augmentation;
    }

    /**
     * Equivalent implementation is contractual.
     */
    private void assertIsAugmented() {
        if (augmentation == null) {
            throw new RuntimeException(
                    "You may only call augmentation-related methods on ConnGraph if the graph is augmented, i.e. if an " +
                            "Augmentation was passed to the constructor");
        }
    }

    /**
     * Returns the VertexInfo containing information about the specified vertex in this graph. If the vertex is not in
     * this graph (i.e. it does not have an entry in vertexInfo), this method adds it to the graph, and creates a
     * VertexInfo object for it.
     */
    private VertexInfo ensureInfo(ConnVertex vertex) {
        VertexInfo info = vertexInfo.get(vertex);
        if (info != null) {
            return info;
        }

        if (vertexInfo.size() == MAX_VERTEX_COUNT) {
            throw new RuntimeException(
                    "Sorry, ConnGraph has too many vertices to perform this operation. ConnGraph does not support " +
                            "storing more than ~2^30 vertices at a time.");
        }

        EulerTourVertex eulerTourVertex = new EulerTourVertex();
        EulerTourNode node = new EulerTourNode(eulerTourVertex, augmentation);
        eulerTourVertex.arbitraryVisit = node;
        node.left = EulerTourNode.LEAF;
        node.right = EulerTourNode.LEAF;
        node.augment();

        info = new VertexInfo(eulerTourVertex);
        vertexInfo.put(vertex, info);
        if (vertexInfo.size() > 1 << maxLogVertexCountSinceRebuild) {
            maxLogVertexCountSinceRebuild++;
        }
        return info;
    }

    /**
     * Takes the specified vertex out of this graph. We should call this method as soon as a vertex does not have any
     * adjacent edges and does not have any augmentation information. This method assumes that the vertex is currently
     * in the graph.
     */
    private void remove(ConnVertex vertex) {
        vertexInfo.remove(vertex);
        if (vertexInfo.size() << REBUILD_CHANGE <= 1 << maxLogVertexCountSinceRebuild) {
            rebuild();
        }
    }

    /**
     * Collapses an adjacency list (either graphListHead or forestListHead) for an EulerTourVertex into the adjacency
     * list for an EulerTourVertex that represents the same underlying ConnVertex, but at a higher level. This has the
     * effect of prepending the list for the lower level to the beginning of the list for the higher level, and
     * replacing all links to the lower-level vertex in the ConnEdges with links to the higher-level vertex.
     *
     * @param head        The first node in the list for the higher-level vertex.
     * @param lowerHead   The first node in the list for the lower-level vertex.
     * @param vertex      The higher-level vertex.
     * @param lowerVertex The lower-level vertex.
     * @return The head of the combined linked list.
     */
    private ConnEdge collapseEdgeList(
            ConnEdge head, ConnEdge lowerHead, EulerTourVertex vertex, EulerTourVertex lowerVertex) {
        if (lowerHead == null) {
            return head;
        }

        ConnEdge prevLowerEdge = null;
        ConnEdge lowerEdge = lowerHead;
        while (lowerEdge != null) {
            prevLowerEdge = lowerEdge;
            if (lowerEdge.vertex1 == lowerVertex) {
                lowerEdge.vertex1 = vertex;
                lowerEdge = lowerEdge.next1;
            } else {
                lowerEdge.vertex2 = vertex;
                lowerEdge = lowerEdge.next2;
            }
        }

        if (prevLowerEdge.vertex1 == vertex) {
            prevLowerEdge.next1 = head;
        } else {
            prevLowerEdge.next2 = head;
        }
        if (head != null) {
            if (head.vertex1 == vertex) {
                head.prev1 = prevLowerEdge;
            } else {
                head.prev2 = prevLowerEdge;
            }
        }
        return lowerHead;
    }

    /**
     * Equivalent implementation is contractual.
     * <p>
     * This method is useful for when an EulerTourVertex's lists (graphListHead or forestListHead) or arbitrary visit
     * change, as these affect the hasGraphEdge and hasForestEdge augmentations.
     */
    private void augmentAncestorFlags(EulerTourNode node) {
        for (EulerTourNode parent = node; parent != null; parent = parent.parent) {
            if (!parent.augmentFlags()) {
                break;
            }
        }
    }

    /**
     * Rebuilds the data structure so that the number of levels is at most the ceiling of log base 2 of the number of
     * vertices in the graph (or zero in the case of zero vertices). The current implementation of rebuild() takes
     * O(V + E) time, assuming a constant difference between maxLogVertexCountSinceRebuild and the result of the
     * logarithm.
     */
    private void rebuild() {
        // Rebuild the graph by collapsing the top deleteCount + 1 levels into the top level

        if (vertexInfo.isEmpty()) {
            maxLogVertexCountSinceRebuild = 0;
            return;
        }
        int deleteCount = 0;
        while (2 * vertexInfo.size() <= 1 << maxLogVertexCountSinceRebuild) {
            maxLogVertexCountSinceRebuild--;
            deleteCount++;
        }
        if (deleteCount == 0) {
            return;
        }

        for (VertexInfo info : vertexInfo.values()) {
            EulerTourVertex vertex = info.vertex;
            EulerTourVertex lowerVertex = vertex;
            for (int i = 0; i < deleteCount; i++) {
                lowerVertex = lowerVertex.lowerVertex;
                if (lowerVertex == null) {
                    break;
                }

                vertex.graphListHead =
                        collapseEdgeList(vertex.graphListHead, lowerVertex.graphListHead, vertex, lowerVertex);
                if (lowerVertex.forestListHead != null) {
                    // Change the eulerTourEdge links
                    ConnEdge lowerEdge = lowerVertex.forestListHead;
                    while (lowerEdge != null) {
                        if (lowerEdge.vertex1 == lowerVertex) {
                            // We'll address this edge when we visit lowerEdge.vertex2
                            lowerEdge = lowerEdge.next1;
                        } else {
                            EulerTourEdge edge = lowerEdge.eulerTourEdge.higherEdge;
                            for (int j = 0; j < i; j++) {
                                edge = edge.higherEdge;
                            }
                            lowerEdge.eulerTourEdge = edge;
                            lowerEdge = lowerEdge.next2;
                        }
                    }

                    vertex.forestListHead =
                            collapseEdgeList(vertex.forestListHead, lowerVertex.forestListHead, vertex, lowerVertex);
                }
            }

            if (lowerVertex != null) {
                lowerVertex = lowerVertex.lowerVertex;
            }
            vertex.lowerVertex = lowerVertex;
            if (lowerVertex != null) {
                lowerVertex.higherVertex = vertex;
            }
            augmentAncestorFlags(vertex.arbitraryVisit);
        }
    }

    /**
     * Adds the specified edge to the graph adjacency list of edge.vertex1, as in EulerTourVertex.graphListHead.
     * Assumes it is not currently in any lists, except possibly the graph adjacency list of edge.vertex2.
     */
    private void addToGraphLinkedList1(ConnEdge edge) {
        edge.prev1 = null;
        edge.next1 = edge.vertex1.graphListHead;
        if (edge.next1 != null) {
            if (edge.next1.vertex1 == edge.vertex1) {
                edge.next1.prev1 = edge;
            } else {
                edge.next1.prev2 = edge;
            }
        }
        edge.vertex1.graphListHead = edge;
    }

    /**
     * Adds the specified edge to the graph adjacency list of edge.vertex2, as in EulerTourVertex.graphListHead.
     * Assumes it is not currently in any lists, except possibly the graph adjacency list of edge.vertex1.
     */
    private void addToGraphLinkedList2(ConnEdge edge) {
        edge.prev2 = null;
        edge.next2 = edge.vertex2.graphListHead;
        if (edge.next2 != null) {
            if (edge.next2.vertex1 == edge.vertex2) {
                edge.next2.prev1 = edge;
            } else {
                edge.next2.prev2 = edge;
            }
        }
        edge.vertex2.graphListHead = edge;
    }

    /**
     * Adds the specified edge to the graph adjacency lists of edge.vertex1 and edge.vertex2, as in
     * EulerTourVertex.graphListHead. Assumes it is not currently in any lists.
     */
    private void addToGraphLinkedLists(ConnEdge edge) {
        addToGraphLinkedList1(edge);
        addToGraphLinkedList2(edge);
    }

    /**
     * Adds the specified edge to the forest adjacency lists of edge.vertex1 and edge.vertex2, as in
     * EulerTourVertex.forestListHead. Assumes it is not currently in any lists.
     */
    private void addToForestLinkedLists(ConnEdge edge) {
        edge.prev1 = null;
        edge.next1 = edge.vertex1.forestListHead;
        if (edge.next1 != null) {
            if (edge.next1.vertex1 == edge.vertex1) {
                edge.next1.prev1 = edge;
            } else {
                edge.next1.prev2 = edge;
            }
        }
        edge.vertex1.forestListHead = edge;

        edge.prev2 = null;
        edge.next2 = edge.vertex2.forestListHead;
        if (edge.next2 != null) {
            if (edge.next2.vertex1 == edge.vertex2) {
                edge.next2.prev1 = edge;
            } else {
                edge.next2.prev2 = edge;
            }
        }
        edge.vertex2.forestListHead = edge;
    }

    /**
     * Removes the specified edge from an adjacency list of edge.vertex1, as in graphListHead and forestListHead.
     * Assumes it is initially in exactly one of the lists for edge.vertex1.
     */
    private void removeFromLinkedList1(ConnEdge edge) {
        if (edge.prev1 != null) {
            if (edge.prev1.vertex1 == edge.vertex1) {
                edge.prev1.next1 = edge.next1;
            } else {
                edge.prev1.next2 = edge.next1;
            }
        } else if (edge == edge.vertex1.graphListHead) {
            edge.vertex1.graphListHead = edge.next1;
        } else {
            edge.vertex1.forestListHead = edge.next1;
        }
        if (edge.next1 != null) {
            if (edge.next1.vertex1 == edge.vertex1) {
                edge.next1.prev1 = edge.prev1;
            } else {
                edge.next1.prev2 = edge.prev1;
            }
        }
    }

    /**
     * Removes the specified edge from an adjacency list of edge.vertex2, as in graphListHead and forestListHead.
     * Assumes it is initially in exactly one of the lists for edge.vertex2.
     */
    private void removeFromLinkedList2(ConnEdge edge) {
        if (edge.prev2 != null) {
            if (edge.prev2.vertex1 == edge.vertex2) {
                edge.prev2.next1 = edge.next2;
            } else {
                edge.prev2.next2 = edge.next2;
            }
        } else if (edge == edge.vertex2.graphListHead) {
            edge.vertex2.graphListHead = edge.next2;
        } else {
            edge.vertex2.forestListHead = edge.next2;
        }
        if (edge.next2 != null) {
            if (edge.next2.vertex1 == edge.vertex2) {
                edge.next2.prev1 = edge.prev2;
            } else {
                edge.next2.prev2 = edge.prev2;
            }
        }
    }

    /**
     * Removes the specified edge from the adjacency lists of edge.vertex1 and edge.vertex2, as in graphListHead and
     * forestListHead. Assumes it is initially in exactly one of the lists for edge.vertex1 and exactly one of the lists
     * for edge.vertex2.
     */
    private void removeFromLinkedLists(ConnEdge edge) {
        removeFromLinkedList1(edge);
        removeFromLinkedList2(edge);
    }

    /**
     * Add an edge between the specified vertices to the Euler tour forest F_i. Assumes that the edge's endpoints are
     * initially in separate trees. Returns the created edge.
     */
    private EulerTourEdge addForestEdge(EulerTourVertex vertex1, EulerTourVertex vertex2) {
        // We need to be careful about where we split and where we add and remove nodes, so as to avoid breaking any
        // EulerTourEdge.visit* fields
        EulerTourNode root = vertex2.arbitraryVisit.root();
        EulerTourNode max = root.max();
        if (max.vertex != vertex2) {
            // Reroot
            EulerTourNode min = root.min();
            if (max.vertex.arbitraryVisit == max) {
                max.vertex.arbitraryVisit = min;
                augmentAncestorFlags(min);
                augmentAncestorFlags(max);
            }
            root = max.remove();
            EulerTourNode[] splitRoots = root.split(vertex2.arbitraryVisit);
            root = splitRoots[1].concatenate(splitRoots[0]);
            EulerTourNode newNode = new EulerTourNode(vertex2, root.augmentationFunc);
            newNode.left = EulerTourNode.LEAF;
            newNode.right = EulerTourNode.LEAF;
            newNode.isRed = true;
            EulerTourNode parent = root.max();
            parent.right = newNode;
            newNode.parent = parent;
            root = newNode.fixInsertion();
            max = newNode;
        }

        EulerTourNode[] splitRoots = vertex1.arbitraryVisit.root().split(vertex1.arbitraryVisit);
        EulerTourNode before = splitRoots[0];
        EulerTourNode after = splitRoots[1];
        EulerTourNode newNode = new EulerTourNode(vertex1, root.augmentationFunc);
        before.concatenate(root, newNode).concatenate(after);
        return new EulerTourEdge(newNode, max);
    }

    /**
     * Removes the specified edge from the Euler tour forest F_i.
     */
    private void removeForestEdge(EulerTourEdge edge) {
        EulerTourNode firstNode;
        EulerTourNode secondNode;
        if (edge.visit1.compareTo(edge.visit2) < 0) {
            firstNode = edge.visit1;
            secondNode = edge.visit2;
        } else {
            firstNode = edge.visit2;
            secondNode = edge.visit1;
        }

        if (firstNode.vertex.arbitraryVisit == firstNode) {
            EulerTourNode successor = secondNode.successor();
            firstNode.vertex.arbitraryVisit = successor;
            augmentAncestorFlags(firstNode);
            augmentAncestorFlags(successor);
        }

        EulerTourNode root = firstNode.root();
        EulerTourNode[] firstSplitRoots = root.split(firstNode);
        EulerTourNode before = firstSplitRoots[0];
        EulerTourNode[] secondSplitRoots = firstSplitRoots[1].split(secondNode.successor());
        before.concatenate(secondSplitRoots[1]);
        firstNode.removeWithoutGettingRoot();
    }

    /**
     * Adds the specified edge to the edge map for srcInfo (srcInfo.edges). Assumes that the edge is not currently in
     * the map.
     *
     * @param edge       The edge.
     * @param srcInfo    The source vertex's info.
     * @param destVertex The destination vertex, i.e. the edge's key in srcInfo.edges.
     */
    private void addToEdgeMap(ConnEdge edge, VertexInfo srcInfo, ConnVertex destVertex) {
        srcInfo.edges.put(destVertex, edge);
    }

    /**
     * Adds an edge between the specified vertices, if such an edge is not already present. Taken together with
     * removeEdge, this method takes O(log^2 N) amortized time with high probability.
     *
     * @return Whether there was no edge between the vertices.
     */
    public boolean addEdge(ConnVertex connVertex1, ConnVertex connVertex2) {
        if (connVertex1 == connVertex2) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }
        if (vertexInfo.size() >= MAX_VERTEX_COUNT - 1) {
            throw new RuntimeException(
                    "Sorry, ConnGraph has too many vertices to perform this operation. ConnGraph does not support " +
                            "storing more than ~2^30 vertices at a time.");
        }
        VertexInfo info1 = ensureInfo(connVertex1);
        if (info1.edges.containsKey(connVertex2)) {
            return false;
        }
        VertexInfo info2 = ensureInfo(connVertex2);

        EulerTourVertex vertex1 = info1.vertex;
        EulerTourVertex vertex2 = info2.vertex;
        ConnEdge edge = new ConnEdge(vertex1, vertex2);

        if (vertex1.arbitraryVisit.root() == vertex2.arbitraryVisit.root()) {
            addToGraphLinkedLists(edge);
        } else {
            addToForestLinkedLists(edge);
            edge.eulerTourEdge = addForestEdge(vertex1, vertex2);
        }
        augmentAncestorFlags(vertex1.arbitraryVisit);
        augmentAncestorFlags(vertex2.arbitraryVisit);

        addToEdgeMap(edge, info1, connVertex2);
        addToEdgeMap(edge, info2, connVertex1);
        return true;
    }

    /**
     * Returns vertex.lowerVertex. If this is null, ensureLowerVertex sets vertex.lowerVertex to a new vertex and
     * returns it.
     */
    private EulerTourVertex ensureLowerVertex(EulerTourVertex vertex) {
        EulerTourVertex lowerVertex = vertex.lowerVertex;
        if (lowerVertex == null) {
            lowerVertex = new EulerTourVertex();
            EulerTourNode lowerNode = new EulerTourNode(lowerVertex, null);
            lowerVertex.arbitraryVisit = lowerNode;
            vertex.lowerVertex = lowerVertex;
            lowerVertex.higherVertex = vertex;

            lowerNode.left = EulerTourNode.LEAF;
            lowerNode.right = EulerTourNode.LEAF;
            lowerNode.augment();
        }
        return lowerVertex;
    }

    /**
     * Pushes all level-i forest edges in the tree rooted at the specified node down to level i - 1, and adds them to
     * F_{i - 1}, where i is the level of the tree.
     */
    private void pushForestEdges(EulerTourNode root) {
        // Iterate over all of the nodes that have hasForestEdge == true
        if (!root.hasForestEdge || root.size == 1) {
            return;
        }
        EulerTourNode node;
        for (node = root; node.left.hasForestEdge; node = node.left) ;
        while (node != null) {
            EulerTourVertex vertex = node.vertex;
            ConnEdge edge = vertex.forestListHead;
            if (edge != null) {
                EulerTourVertex lowerVertex = ensureLowerVertex(vertex);
                ConnEdge prevEdge = null;
                while (edge != null) {
                    if (edge.vertex2 == vertex || edge.vertex2 == lowerVertex) {
                        // We address this edge when we visit edge.vertex1
                        prevEdge = edge;
                        edge = edge.next2;
                    } else {
                        edge.vertex1 = lowerVertex;
                        edge.vertex2 = ensureLowerVertex(edge.vertex2);
                        EulerTourEdge lowerEdge = addForestEdge(edge.vertex1, edge.vertex2);
                        lowerEdge.higherEdge = edge.eulerTourEdge;
                        edge.eulerTourEdge = lowerEdge;
                        prevEdge = edge;
                        edge = edge.next1;
                    }
                }

                // Prepend vertex.forestListHead to the beginning of lowerVertex.forestListHead
                if (prevEdge.vertex1 == lowerVertex) {
                    prevEdge.next1 = lowerVertex.forestListHead;
                } else {
                    prevEdge.next2 = lowerVertex.forestListHead;
                }
                if (lowerVertex.forestListHead != null) {
                    if (lowerVertex.forestListHead.vertex1 == lowerVertex) {
                        lowerVertex.forestListHead.prev1 = prevEdge;
                    } else {
                        lowerVertex.forestListHead.prev2 = prevEdge;
                    }
                }
                lowerVertex.forestListHead = vertex.forestListHead;
                vertex.forestListHead = null;
                augmentAncestorFlags(lowerVertex.arbitraryVisit);
            }

            // Iterate to the next node with hasForestEdge == true, clearing hasForestEdge as we go
            if (node.right.hasForestEdge) {
                for (node = node.right; node.left.hasForestEdge; node = node.left) ;
            } else {
                node.hasForestEdge = false;
                while (node.parent != null && node.parent.right == node) {
                    node = node.parent;
                    node.hasForestEdge = false;
                }
                node = node.parent;
            }
        }
    }

    /**
     * Searches for a level-i edge connecting a vertex in the tree rooted at the specified node to a vertex in another
     * tree, where i is the level of the tree. This is a "replacement" edge because it replaces the edge that was
     * previously connecting the two trees. We push any level-i edges we encounter that do not connect to another tree
     * down to level i - 1, adding them to G_{i - 1}. This method assumes that root.hasForestEdge is false.
     *
     * @param root The root of the tree.
     * @return The replacement edge, or null if there is no replacement edge.
     */
    private ConnEdge findReplacementEdge(EulerTourNode root) {
        // Iterate over all of the nodes that have hasGraphEdge == true
        if (!root.hasGraphEdge) {
            return null;
        }
        EulerTourNode node;
        for (node = root; node.left.hasGraphEdge; node = node.left) ;
        while (node != null) {
            EulerTourVertex vertex = node.vertex;
            ConnEdge edge = vertex.graphListHead;
            if (edge != null) {
                ConnEdge replacementEdge = null;
                ConnEdge prevEdge = null;
                while (edge != null) {
                    EulerTourVertex adjVertex;
                    ConnEdge nextEdge;
                    if (edge.vertex1 == vertex) {
                        adjVertex = edge.vertex2;
                        nextEdge = edge.next1;
                    } else {
                        adjVertex = edge.vertex1;
                        nextEdge = edge.next2;
                    }

                    if (adjVertex.arbitraryVisit.root() != root) {
                        replacementEdge = edge;
                        break;
                    }

                    // Remove the edge from the adjacency list of adjVertex. We will remove it from the adjacency list
                    // of "vertex" later.
                    if (edge.vertex1 == adjVertex) {
                        removeFromLinkedList1(edge);
                    } else {
                        removeFromLinkedList2(edge);
                    }
                    augmentAncestorFlags(adjVertex.arbitraryVisit);

                    // Push the edge down to level i - 1
                    edge.vertex1 = ensureLowerVertex(edge.vertex1);
                    edge.vertex2 = ensureLowerVertex(edge.vertex2);

                    // Add the edge to the adjacency list of adjVertex.lowerVertex. We will add it to the adjacency list
                    // of lowerVertex later.
                    if (edge.vertex1 != vertex.lowerVertex) {
                        addToGraphLinkedList1(edge);
                    } else {
                        addToGraphLinkedList2(edge);
                    }
                    augmentAncestorFlags(adjVertex.lowerVertex.arbitraryVisit);

                    prevEdge = edge;
                    edge = nextEdge;
                }

                // Prepend the linked list up to prevEdge to the beginning of vertex.lowerVertex.graphListHead
                if (prevEdge != null) {
                    EulerTourVertex lowerVertex = vertex.lowerVertex;
                    if (prevEdge.vertex1 == lowerVertex) {
                        prevEdge.next1 = lowerVertex.graphListHead;
                    } else {
                        prevEdge.next2 = lowerVertex.graphListHead;
                    }
                    if (lowerVertex.graphListHead != null) {
                        if (lowerVertex.graphListHead.vertex1 == lowerVertex) {
                            lowerVertex.graphListHead.prev1 = prevEdge;
                        } else {
                            lowerVertex.graphListHead.prev2 = prevEdge;
                        }
                    }
                    lowerVertex.graphListHead = vertex.graphListHead;
                    augmentAncestorFlags(lowerVertex.arbitraryVisit);
                }
                vertex.graphListHead = edge;
                if (edge == null) {
                    augmentAncestorFlags(vertex.arbitraryVisit);
                } else if (edge.vertex1 == vertex) {
                    edge.prev1 = null;
                } else {
                    edge.prev2 = null;
                }

                if (replacementEdge != null) {
                    return replacementEdge;
                }
            }

            // Iterate to the next node with hasGraphEdge == true. Note that nodes' hasGraphEdge fields can change as we
            // push down edges.
            if (node.right.hasGraphEdge) {
                for (node = node.right; node.left.hasGraphEdge; node = node.left) ;
            } else {
                while (node.parent != null && (node.parent.right == node || !node.parent.hasGraphEdge)) {
                    node = node.parent;
                }
                node = node.parent;
            }
        }
        return null;
    }

    /**
     * Removes the edge from srcInfo to destVertex from the edge map for srcInfo (srcInfo.edges), if it is present.
     * Returns the edge that we removed, if any.
     */
    private ConnEdge removeFromEdgeMap(VertexInfo srcInfo, ConnVertex destVertex) {
        ConnEdge edge = srcInfo.edges.remove(destVertex);
        return edge;
    }

    /**
     * Removes the edge between the specified vertices, if there is such an edge. Taken together with addEdge, this
     * method takes O(log^2 N) amortized time with high probability.
     *
     * @return Whether there was an edge between the vertices.
     */
    public boolean removeEdge(ConnVertex vertex1, ConnVertex vertex2) {
        if (vertex1 == vertex2) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }

        VertexInfo info1 = vertexInfo.get(vertex1);
        if (info1 == null) {
            return false;
        }
        ConnEdge edge = removeFromEdgeMap(info1, vertex2);
        if (edge == null) {
            return false;
        }
        VertexInfo info2 = vertexInfo.get(vertex2);
        removeFromEdgeMap(info2, vertex1);

        removeFromLinkedLists(edge);
        augmentAncestorFlags(edge.vertex1.arbitraryVisit);
        augmentAncestorFlags(edge.vertex2.arbitraryVisit);

        if (edge.eulerTourEdge != null) {
            for (EulerTourEdge levelEdge = edge.eulerTourEdge; levelEdge != null; levelEdge = levelEdge.higherEdge) {
                removeForestEdge(levelEdge);
            }
            edge.eulerTourEdge = null;

            // Search for a replacement edge
            ConnEdge replacementEdge = null;
            EulerTourVertex levelVertex1 = edge.vertex1;
            EulerTourVertex levelVertex2 = edge.vertex2;
            while (levelVertex1 != null) {
                EulerTourNode root1 = levelVertex1.arbitraryVisit.root();
                EulerTourNode root2 = levelVertex2.arbitraryVisit.root();

                // Optimization: if hasGraphEdge is false for one of the roots, then there definitely isn't a
                // replacement edge at this level
                if (root1.hasGraphEdge && root2.hasGraphEdge) {
                    EulerTourNode root;
                    if (root1.size < root2.size) {
                        root = root1;
                    } else {
                        root = root2;
                    }

                    pushForestEdges(root);
                    replacementEdge = findReplacementEdge(root);
                    if (replacementEdge != null) {
                        break;
                    }
                }

                // To save space, get rid of trees with one node
                if (root1.size == 1 && levelVertex1.higherVertex != null) {
                    levelVertex1.higherVertex.lowerVertex = null;
                }
                if (root2.size == 1 && levelVertex2.higherVertex != null) {
                    levelVertex2.higherVertex.lowerVertex = null;
                }

                levelVertex1 = levelVertex1.higherVertex;
                levelVertex2 = levelVertex2.higherVertex;
            }

            if (replacementEdge != null) {
                // Add the replacement edge to all of the forests at or above the current level
                removeFromLinkedLists(replacementEdge);
                addToForestLinkedLists(replacementEdge);
                EulerTourVertex replacementVertex1 = replacementEdge.vertex1;
                EulerTourVertex replacementVertex2 = replacementEdge.vertex2;
                augmentAncestorFlags(replacementVertex1.arbitraryVisit);
                augmentAncestorFlags(replacementVertex2.arbitraryVisit);
                EulerTourEdge lowerEdge = null;
                while (replacementVertex1 != null) {
                    EulerTourEdge levelEdge = addForestEdge(replacementVertex1, replacementVertex2);
                    if (lowerEdge == null) {
                        replacementEdge.eulerTourEdge = levelEdge;
                    } else {
                        lowerEdge.higherEdge = levelEdge;
                    }

                    lowerEdge = levelEdge;
                    replacementVertex1 = replacementVertex1.higherVertex;
                    replacementVertex2 = replacementVertex2.higherVertex;
                }
            }
        }

        if (info1.edges.isEmpty() && !info1.vertex.hasAugmentation) {
            remove(vertex1);
        }
        if (info2.edges.isEmpty() && !info2.vertex.hasAugmentation) {
            remove(vertex2);
        }
        return true;
    }

    /**
     * Returns whether the specified vertices are connected - whether there is a path between them. Returns true if
     * vertex1 == vertex2. This method takes O(log N) time with high probability.
     */
    public boolean connected(ConnVertex vertex1, ConnVertex vertex2) {
        if (vertex1 == vertex2) {
            return true;
        }
        VertexInfo info1 = vertexInfo.get(vertex1);
        if (info1 == null) {
            return false;
        }
        VertexInfo info2 = vertexInfo.get(vertex2);
        return info2 != null && info1.vertex.arbitraryVisit.root() == info2.vertex.arbitraryVisit.root();
    }

    /**
     * Returns the vertices that are directly adjacent to the specified vertex.
     */
    public Collection<ConnVertex> adjacentVertices(ConnVertex vertex) {
        VertexInfo info = vertexInfo.get(vertex);
        if (info != null) {
            return new ArrayList<ConnVertex>(info.edges.keySet());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Sets the augmentation associated with the specified vertex. This method takes O(log N) time with high
     * probability.
     * <p>
     * Note that passing a null value for the second argument is not the same as removing the augmentation. For that,
     * you need to call removeVertexAugmentation.
     *
     * @return The augmentation that was previously associated with the vertex. Returns null if it did not have any
     * associated augmentation.
     */
    public Object setVertexAugmentation(ConnVertex connVertex, Object vertexAugmentation) {
        assertIsAugmented();
        EulerTourVertex vertex = ensureInfo(connVertex).vertex;
        Object oldAugmentation = vertex.augmentation;
        if (!vertex.hasAugmentation ||
                (vertexAugmentation != null ? !vertexAugmentation.equals(oldAugmentation) : oldAugmentation != null)) {
            vertex.augmentation = vertexAugmentation;
            vertex.hasAugmentation = true;
            for (EulerTourNode node = vertex.arbitraryVisit; node != null; node = node.parent) {
                if (!node.augment()) {
                    break;
                }
            }
        }
        return oldAugmentation;
    }

    /**
     * Removes any augmentation associated with the specified vertex. This method takes O(log N) time with high
     * probability.
     *
     * @return The augmentation that was previously associated with the vertex. Returns null if it did not have any
     * associated augmentation.
     */
    public Object removeVertexAugmentation(ConnVertex connVertex) {
        assertIsAugmented();
        VertexInfo info = vertexInfo.get(connVertex);
        if (info == null) {
            return null;
        }

        EulerTourVertex vertex = info.vertex;
        Object oldAugmentation = vertex.augmentation;
        if (info.edges.isEmpty()) {
            remove(connVertex);
        } else if (vertex.hasAugmentation) {
            vertex.augmentation = null;
            vertex.hasAugmentation = false;
            for (EulerTourNode node = vertex.arbitraryVisit; node != null; node = node.parent) {
                if (!node.augment()) {
                    break;
                }
            }
        }
        return oldAugmentation;
    }

    /**
     * Returns the augmentation associated with the specified vertex. Returns null if it does not have any associated
     * augmentation. At present, this method takes constant expected time. Contrast with getComponentAugmentation.
     */
    public Object getVertexAugmentation(ConnVertex vertex) {
        assertIsAugmented();
        VertexInfo info = vertexInfo.get(vertex);
        if (info != null) {
            return info.vertex.augmentation;
        } else {
            return null;
        }
    }

    /**
     * Returns the result of combining the augmentations associated with all of the vertices in the connected component
     * containing the specified vertex. Returns null if none of those vertices has any associated augmentation. This
     * method takes O(log N) time with high probability.
     */
    public Object getComponentAugmentation(ConnVertex vertex) {
        assertIsAugmented();
        VertexInfo info = vertexInfo.get(vertex);
        if (info != null) {
            return info.vertex.arbitraryVisit.root().augmentation;
        } else {
            return null;
        }
    }

    /**
     * Returns whether the specified vertex has any associated augmentation. At present, this method takes constant
     * expected time. Contrast with componentHasAugmentation.
     */
    public boolean vertexHasAugmentation(ConnVertex vertex) {
        assertIsAugmented();
        VertexInfo info = vertexInfo.get(vertex);
        if (info != null) {
            return info.vertex.hasAugmentation;
        } else {
            return false;
        }
    }

    /**
     * Returns whether any of the vertices in the connected component containing the specified vertex has any associated
     * augmentation. This method takes O(log N) time with high probability.
     */
    public boolean componentHasAugmentation(ConnVertex vertex) {
        assertIsAugmented();
        VertexInfo info = vertexInfo.get(vertex);
        if (info != null) {
            return info.vertex.arbitraryVisit.root().hasAugmentation;
        } else {
            return false;
        }
    }

    /**
     * Clears this graph, by removing all edges and vertices, and removing all augmentation information from the
     * vertices.
     */
    public void clear() {
        // Note that we construct a new HashMap rather than calling vertexInfo.clear() in order to ensure a reduction in
        // space
        vertexInfo = new HashMap<ConnVertex, VertexInfo>();
        maxLogVertexCountSinceRebuild = 0;
    }

    /**
     * Pushes all forest edges as far down as possible, so that any further pushes would violate the constraint on the
     * size of connected components. The current implementation of this method takes O(V log^2 V) time.
     */
    private void optimizeForestEdges() {
        for (VertexInfo info : vertexInfo.values()) {
            int level = maxLogVertexCountSinceRebuild;
            EulerTourVertex vertex;
            for (vertex = info.vertex; vertex.lowerVertex != null; vertex = vertex.lowerVertex) {
                level--;
            }

            while (vertex != null) {
                EulerTourNode node = vertex.arbitraryVisit;
                ConnEdge edge = vertex.forestListHead;
                while (edge != null) {
                    if (vertex == edge.vertex2) {
                        // We'll address this edge when we visit edge.vertex1
                        edge = edge.next2;
                        continue;
                    }
                    ConnEdge nextEdge = edge.next1;

                    EulerTourVertex lowerVertex1 = vertex;
                    EulerTourVertex lowerVertex2 = edge.vertex2;
                    for (int lowerLevel = level - 1; lowerLevel > 0; lowerLevel--) {
                        // Compute the total size if we combine the Euler tour trees
                        int combinedSize = 1;
                        if (lowerVertex1.lowerVertex != null) {
                            combinedSize += lowerVertex1.lowerVertex.arbitraryVisit.root().size;
                        } else {
                            combinedSize++;
                        }
                        if (lowerVertex2.lowerVertex != null) {
                            combinedSize += lowerVertex2.lowerVertex.arbitraryVisit.root().size;
                        } else {
                            combinedSize++;
                        }

                        // X EulerTourVertices = (2 * X - 1) EulerTourNodes
                        if (combinedSize > 2 * (1 << lowerLevel) - 1) {
                            break;
                        }

                        lowerVertex1 = ensureLowerVertex(lowerVertex1);
                        lowerVertex2 = ensureLowerVertex(lowerVertex2);
                        EulerTourEdge lowerEdge = addForestEdge(lowerVertex1, lowerVertex2);
                        lowerEdge.higherEdge = edge.eulerTourEdge;
                        edge.eulerTourEdge = lowerEdge;
                    }

                    if (lowerVertex1 != vertex) {
                        // We pushed the edge down at least one level
                        removeFromLinkedLists(edge);
                        augmentAncestorFlags(node);
                        augmentAncestorFlags(edge.vertex2.arbitraryVisit);

                        edge.vertex1 = lowerVertex1;
                        edge.vertex2 = lowerVertex2;
                        addToForestLinkedLists(edge);
                        augmentAncestorFlags(lowerVertex1.arbitraryVisit);
                        augmentAncestorFlags(lowerVertex2.arbitraryVisit);
                    }

                    edge = nextEdge;
                }

                vertex = vertex.higherVertex;
                level++;
            }
        }
    }

    /**
     * Pushes each non-forest edge down to the lowest level where the endpoints are in the same connected component. The
     * current implementation of this method takes O(V log V + E log V log log V) time.
     */
    private void optimizeGraphEdges() {
        for (VertexInfo info : vertexInfo.values()) {
            EulerTourVertex vertex;
            for (vertex = info.vertex; vertex.lowerVertex != null; vertex = vertex.lowerVertex) ;
            while (vertex != null) {
                EulerTourNode node = vertex.arbitraryVisit;
                ConnEdge edge = vertex.graphListHead;
                while (edge != null) {
                    if (vertex == edge.vertex2) {
                        // We'll address this edge when we visit edge.vertex1
                        edge = edge.next2;
                        continue;
                    }
                    ConnEdge nextEdge = edge.next1;

                    // Use binary search to identify the lowest level where the two vertices are in the same connected
                    // component
                    int maxLevelsDown = 0;
                    EulerTourVertex lowerVertex1 = vertex.lowerVertex;
                    EulerTourVertex lowerVertex2 = edge.vertex2.lowerVertex;
                    while (lowerVertex1 != null && lowerVertex2 != null) {
                        maxLevelsDown++;
                        lowerVertex1 = lowerVertex1.lowerVertex;
                        lowerVertex2 = lowerVertex2.lowerVertex;
                    }
                    EulerTourVertex levelVertex1 = vertex;
                    EulerTourVertex levelVertex2 = edge.vertex2;
                    while (maxLevelsDown > 0) {
                        int levelsDown = (maxLevelsDown + 1) / 2;
                        lowerVertex1 = levelVertex1;
                        lowerVertex2 = levelVertex2;
                        for (int i = 0; i < levelsDown; i++) {
                            lowerVertex1 = lowerVertex1.lowerVertex;
                            lowerVertex2 = lowerVertex2.lowerVertex;
                        }

                        if (lowerVertex1.arbitraryVisit.root() != lowerVertex2.arbitraryVisit.root()) {
                            maxLevelsDown = levelsDown - 1;
                        } else {
                            levelVertex1 = lowerVertex1;
                            levelVertex2 = lowerVertex2;
                            maxLevelsDown -= levelsDown;
                        }
                    }

                    if (levelVertex1 != vertex) {
                        removeFromLinkedLists(edge);
                        augmentAncestorFlags(node);
                        augmentAncestorFlags(edge.vertex2.arbitraryVisit);

                        edge.vertex1 = levelVertex1;
                        edge.vertex2 = levelVertex2;
                        addToGraphLinkedLists(edge);
                        augmentAncestorFlags(levelVertex1.arbitraryVisit);
                        augmentAncestorFlags(levelVertex2.arbitraryVisit);
                    }

                    edge = nextEdge;
                }
                vertex = vertex.higherVertex;
            }
        }
    }

    /**
     * Attempts to optimize the internal representation of the graph so that future updates will take less time. This
     * method does not affect how long queries such as "connected" will take. You may find it beneficial to call
     * optimize() when there is some downtime. Note that this method generally increases the amount of space the
     * ConnGraph uses, but not beyond the bound of O(V log V + E).
     */
    public void optimize() {
        // The current implementation of optimize() takes O(V log^2 V + E log V log log V) time
        rebuild();
        optimizeForestEdges();
        optimizeGraphEdges();
    }
}
