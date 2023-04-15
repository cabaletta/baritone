package baritone.builder.utils.com.github.btrekkie.connectivity;

/**
 * Responds to when ownership of an augmentation object is released. If the number of times that
 * combinedAugmentationReleased and vertexAugmentationReleased were called on an object is equal to the number of times
 * that Augmentation.combine returned the object plus the number of times the object was passed to
 * setVertexAugmentation, then ConnGraph no longer has a reference to the object. Such an object may be recycled.
 * <p>
 * Note that a graph may have multiple ownership claims to a given augmentation object, meaning the graph needs to
 * release the object multiple times before it can be recycled. This could happen if Augmentation.combine returned the
 * same object multiple times or the object was passed to setVertexAugmentation multiple times.
 * <p>
 * See ConnGraph(Augmentation, AugmentationReleaseListener).
 */
public interface AugmentationReleaseListener {
    /**
     * Responds to one ownership claim to the specified combined augmentation object (or previous return value of
     * Augmentation.combine) being released. "obj" is guaranteed not to be null.
     * <p>
     * This may be called from any ConnGraph method that mutates the graph, as well as from optimize().
     */
    public void combinedAugmentationReleased(Object obj);

    /**
     * Responds to one ownership claim to the specified vertex augmentation object (or previous argument to
     * setVertexAugmentation) being released. "obj" is guaranteed not to be null.
     * <p>
     * This may be called from the following ConnGraph methods: setVertexAugmentation, removeVertexAugmentation, and
     * clear().
     */
    public void vertexAugmentationReleased(Object obj);
}
