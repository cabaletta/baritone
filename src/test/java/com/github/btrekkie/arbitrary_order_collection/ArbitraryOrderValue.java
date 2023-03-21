package com.github.btrekkie.arbitrary_order_collection;

/**
 * A value in an ArbitraryOrderCollection.  To determine the relative order of two values in the same collection, call
 * compareTo.
 */
public class ArbitraryOrderValue implements Comparable<ArbitraryOrderValue> {
    /**
     * The node that establishes this value's relative position.
     */
    final ArbitraryOrderNode node;

    ArbitraryOrderValue(ArbitraryOrderNode node) {
        this.node = node;
    }

    @Override
    public int compareTo(ArbitraryOrderValue other) {
        return node.compareTo(other.node);
    }
}
