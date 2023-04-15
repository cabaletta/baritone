package baritone.builder.utils.com.github.btrekkie.connectivity;

/**
 * An Augmentation implementation that wraps a MutatingAugmentation. It recycles (or "pools") previously constructed
 * combined augmentation objects in order to improve performance. This should be passed to the ConnGraph constructor as
 * an AugmentationReleaseListener so that it can recycle unused objects.
 */
class AugmentationPool implements Augmentation, AugmentationReleaseListener {
    /**
     * The maximum number of unused objects to store in a pool.
     */
    private static final int CAPACITY = 20;

    /**
     * The MutatingAugmentation we are wrapping.
     */
    private final MutatingAugmentation mutatingAugmentation;

    /**
     * A pool of unused objects we may reuse. The array has length CAPACITY, but only the first "size" elements contain
     * reusable objects. The values in the array after the first "size" are unspecified.
     */
    private Object[] pool = new Object[CAPACITY];

    /**
     * The number of reusable objects in the pool.
     */
    private int size;

    public AugmentationPool(MutatingAugmentation mutatingAugmentation) {
        this.mutatingAugmentation = mutatingAugmentation;
    }

    @Override
    public Object combine(Object value1, Object value2) {
        Object result;
        if (size == 0) {
            result = mutatingAugmentation.newAugmentation();
        } else {
            size--;
            result = pool[size];
        }
        mutatingAugmentation.combine(value1, value2, result);
        return result;
    }

    @Override
    public void combinedAugmentationReleased(Object obj) {
        if (size < CAPACITY) {
            pool[size] = obj;
            size++;
        }
    }

    @Override
    public void vertexAugmentationReleased(Object obj) {

    }
}
