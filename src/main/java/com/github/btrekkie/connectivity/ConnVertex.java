package com.github.btrekkie.connectivity;

import java.util.Random;

/**
 * A vertex in a ConnGraph. See the comments for ConnGraph.
 */
public class ConnVertex {
    /**
     * The thread-local random number generator we use by default to set the "hash" field.
     */
    private static final ThreadLocal<Random> random = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }
    };

    /**
     * A randomly generated integer to use as the return value of hashCode(). ConnGraph relies on random hash codes for
     * its performance guarantees.
     */
    private final long hash;

    public ConnVertex() {
        hash = random.get().nextLong();
    }

    /**
     * Constructs a new ConnVertex.
     *
     * @param random The random number generator to use to produce a random hash code. ConnGraph relies on random hash
     *               codes for its performance guarantees.
     */
    public ConnVertex(Random random) {
        hash = random.nextLong();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    public long getIdentity(){
        return hash;
    }
}
