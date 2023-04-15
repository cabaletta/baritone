/*
 * This file was originally written by btrekkie under the MIT license, which is compatible with the LGPL license for this usage within Baritone
 * https://github.com/btrekkie/dynamic-connectivity/
 */

package baritone.builder.utils.com.github.btrekkie.connectivity;

import baritone.builder.utils.com.github.btrekkie.red_black_node.RedBlackNode;

/**
 * A node in an Euler tour tree for ConnGraph (at some particular level i). See the comments for the implementation of
 * ConnGraph.
 */
class EulerTourNode extends RedBlackNode<EulerTourNode> {
    /**
     * The dummy leaf node.
     */
    public static final EulerTourNode LEAF = new EulerTourNode(null, null);

    /**
     * The vertex this node visits.
     */
    public final EulerTourVertex vertex;

    /**
     * The number of nodes in the subtree rooted at this node.
     */
    public int size;

    /**
     * Whether the subtree rooted at this node contains a node "node" for which
     * node.vertex.arbitraryNode == node && node.vertex.graphListHead != null.
     */
    public boolean hasGraphEdge;

    /**
     * Whether the subtree rooted at this node contains a node "node" for which
     * node.vertex.arbitraryNode == node && node.vertex.forestListHead != null.
     */
    public boolean hasForestEdge;

    /** The graph this belongs to. "graph" is null instead if this node is not in the highest level. */
    public final ConnGraph graph;

    /**
     * The combined augmentation for the subtree rooted at this node. This is the result of combining the augmentation
     * values node.vertex.augmentation for all nodes "node" in the subtree rooted at this node for which
     * node.vertex.arbitraryVisit == node, using graph.augmentation. This is null if hasAugmentation is false.
     */
    public Object augmentation;

    /**
     * Whether the subtree rooted at this node contains at least one augmentation value. This indicates whether there is
     * some node "node" in the subtree rooted at this node for which node.vertex.hasAugmentation is true and
     * node.vertex.arbitraryVisit == node.
     */
    public boolean hasAugmentation;

    /**
     * Whether this node "owns" "augmentation", with respect to graph.augmentationReleaseListener. A node owns a
     * combined augmentation if the node obtained it by calling Augmentation.combine, as opposed to copying a reference
     * to vertex.augmentation, left.augmentation, or right.augmentation. This is false if
     * graph.augmentationReleaseListener or "augmentation" is null.
     */
    public boolean ownsAugmentation;

    public EulerTourNode(EulerTourVertex vertex, ConnGraph graph) {
        this.vertex = vertex;
        this.graph = graph;
    }

    /**
     * Like augment(), but only updates the augmentation fields hasGraphEdge and hasForestEdge.
     */
    public boolean augmentFlags() {
        boolean newHasGraphEdge =
                left.hasGraphEdge || right.hasGraphEdge || (vertex.arbitraryVisit == this && vertex.graphListHead != null);
        boolean newHasForestEdge =
                left.hasForestEdge || right.hasForestEdge ||
                        (vertex.arbitraryVisit == this && vertex.forestListHead != null);
        if (newHasGraphEdge == hasGraphEdge && newHasForestEdge == hasForestEdge) {
            return false;
        } else {
            hasGraphEdge = newHasGraphEdge;
            hasForestEdge = newHasForestEdge;
            return true;
        }
    }

    @Override
    public boolean augment() {
        int newSize = left.size + right.size + 1;
        boolean augmentedFlags = augmentFlags();
        if (graph == null || graph.augmentation == null) {
            if (newSize == size && !augmentedFlags) {
                return false;
            } else {
                size = newSize;
                return true;
            }
        }

        AugmentationReleaseListener releaseListener = graph.augmentationReleaseListener;
        Object newAugmentation = null;
        int valueCount = 0;
        if (left.hasAugmentation) {
            newAugmentation = left.augmentation;
            valueCount = 1;
        }
        if (vertex.hasAugmentation && vertex.arbitraryVisit == this) {
            if (valueCount == 0) {
                newAugmentation = vertex.augmentation;
            } else {
                newAugmentation = graph.augmentation.combine(newAugmentation, vertex.augmentation);
            }
            valueCount++;
        }
        if (right.hasAugmentation) {
            if (valueCount == 0) {
                newAugmentation = right.augmentation;
            } else {
                Object tempAugmentation = newAugmentation;
                newAugmentation = graph.augmentation.combine(newAugmentation, right.augmentation);
                if (valueCount >= 2 && releaseListener != null && tempAugmentation != null) {
                    releaseListener.combinedAugmentationReleased(tempAugmentation);
                }
            }
            valueCount++;
        }

        boolean newHasAugmentation = valueCount > 0;
        boolean newOwnsAugmentation = valueCount >= 2 && releaseListener != null && newAugmentation != null;
        if (newSize == size && !augmentedFlags && hasAugmentation == newHasAugmentation &&
                (newAugmentation != null ? newAugmentation.equals(augmentation) : augmentation == null)) {
            if (newOwnsAugmentation) {
                releaseListener.combinedAugmentationReleased(newAugmentation);
            }
            return false;
        } else {
            if (ownsAugmentation) {
                releaseListener.combinedAugmentationReleased(augmentation);
            }
            size = newSize;
            augmentation = newAugmentation;
            hasAugmentation = newHasAugmentation;
            ownsAugmentation = newOwnsAugmentation;
            return true;
        }
    }

    @Override
    public void removeWithoutGettingRoot() {
        super.removeWithoutGettingRoot();
        if (ownsAugmentation) {
            graph.augmentationReleaseListener.combinedAugmentationReleased(augmentation);
            augmentation = null;
            hasAugmentation = false;
            ownsAugmentation = false;
        }
    }
}
