package com.github.btrekkie.connectivity.test;

import java.util.HashMap;
import java.util.Map;

import com.github.btrekkie.connectivity.Augmentation;
import com.github.btrekkie.connectivity.AugmentationReleaseListener;

/**
 * An Augmentation implementation for SumAndMax that recycles (or "pools") SumAndMax instances and uses caching. This
 * should be passed to the ConnGraph constructor as an AugmentationReleaseListener so that it can recycle unused
 * SumAndMax objects.
 */
public class SumAndMaxPoolAndCache implements Augmentation, AugmentationReleaseListener {
    /** The maximum number of unused SumAndMax objects to store in a pool. */
    private static final int CAPACITY = 3;

    /**
     * The maximum "sum" and "max" values to store in the cache. We cache all possible SumAndMax instances where "sum"
     * and "max" are both in the range [0, CACHE_SIZE).
     */
    private static final int CACHE_SIZE = 10;

    /**
     * A pool of SumAndMax instances we may reuse. The array has length CAPACITY, but only the first "size" elements
     * contain reusable SumAndMax objects. The values in the array after the first "size" are unspecified.
     */
    private SumAndMax[] pool = new SumAndMax[CAPACITY];

    /** The number of reusable SumAndMax instances in the pool. */
    private int size;

    /** A CACHE_SIZE x CACHE_SIZE array of cached SumAndMax instances. cache[m][s] is equal to "new SumAndMax(s, m)". */
    private SumAndMax[][] cache;

    /**
     * A map from each SumAndMax object that is a combined augmentation value that the graph has ownership of to the
     * number of ownership claims the graph has on the object.
     */
    private Map<Reference<SumAndMax>, Integer> ownershipCounts = new HashMap<Reference<SumAndMax>, Integer>();

    public SumAndMaxPoolAndCache() {
        cache = new SumAndMax[CACHE_SIZE][CACHE_SIZE];
        for (int max = 0; max < CACHE_SIZE; max++) {
            for (int sum = 0; sum < CACHE_SIZE; sum++) {
                cache[max][sum] = new SumAndMax(sum, max);
            }
        }
    }

    @Override
    public Object combine(Object value1, Object value2) {
        SumAndMax sumAndMax1 = (SumAndMax)value1;
        SumAndMax sumAndMax2 = (SumAndMax)value2;
        int sum = sumAndMax1.sum + sumAndMax2.sum;
        int max = Math.max(sumAndMax1.max, sumAndMax2.max);

        SumAndMax result;
        if (sum >= 0 && sum < CACHE_SIZE && max >= 0 && max < CACHE_SIZE) {
            result = cache[max][sum];
        } else if (size == 0) {
            result = new SumAndMax(sum, max);
        } else {
            size--;
            result = pool[size];
            result.sum = sum;
            result.max = max;
        }

        Reference<SumAndMax> resultReference = new Reference<SumAndMax>(result);
        Integer count = ownershipCounts.get(resultReference);
        ownershipCounts.put(resultReference, count != null ? count + 1 : 1);
        return result;
    }

    /**
     * Shared implementation of combinedAugmentationReleased and vertexAugmentationReleased.
     * @param obj The object that was released.
     * @param isCombined Whether this is for a call to combinedAugmentationReleased.
     */
    private void augmentationReleased(SumAndMax obj, boolean isCombined) {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot release a null value");
        }

        if (isCombined) {
            Reference<SumAndMax> reference = new Reference<SumAndMax>(obj);
            Integer count = ownershipCounts.get(reference);
            if (count == null) {
                throw new RuntimeException(
                    "ConnGraph attempted to release a combined augmentation object it did not own");
            }
            if (count <= 1) {
                ownershipCounts.remove(reference);
            } else {
                ownershipCounts.put(reference, count - 1);
            }
        }

        if (obj.sum < 0 || obj.sum >= CACHE_SIZE || obj.max < 0 || obj.max >= CACHE_SIZE) {
            if (size < CAPACITY) {
                pool[size] = obj;
                size++;
            } else if (!isCombined) {
                // We want to test reusing vertex augmentation objects, so we add the object to the pool even if the
                // pool is already full
                pool[CAPACITY - 1] = obj;
            }
        }
    }

    @Override
    public void combinedAugmentationReleased(Object obj) {
        augmentationReleased((SumAndMax)obj, true);
    }

    @Override
    public void vertexAugmentationReleased(Object obj) {
        augmentationReleased((SumAndMax)obj, false);
    }
}
