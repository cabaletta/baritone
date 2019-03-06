package com.github.btrekkie.connectivity;

import java.util.Random;

/** A vertex in a ConnGraph. See the comments for ConnGraph. */
public class ConnVertex {
    /**
     * The thread-local random number generator we use by default to set the "hash" field. We could use
     * ThreadLocalRandom instead, but ThreadLocalRandom isn't available in Java 6.0 and below.
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
    private final int hash;

    public ConnVertex() {
        hash = random.get().nextInt();
    }

    /**
     * Constructs a new ConnVertex.
     * @param random The random number generator to use to produce a random hash code. ConnGraph relies on random hash
     *     codes for its performance guarantees.
     */
    public ConnVertex(Random random) {
        hash = random.nextInt();
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
