
package baritone.utils.schematic.schematica;

public class MathUtils
{
    /**
     * Returns the average value of the elements in the given array
     * @param arr
     * @return
     */
    public static double average(int[] arr)
    {
        final int size = arr.length;

        if (size == 0)
        {
            return 0;
        }

        long sum = 0;

        for (int i = 0; i < size; ++i)
        {
            sum += arr[i];
        }

        return (double) sum / (double) size;
    }

    /**
     * Returns the average value of the elements in the given array
     * @param arr
     * @return
     */
    public static double average(long[] arr)
    {
        final int size = arr.length;

        if (size == 0)
        {
            return 0;
        }

        long sum = 0;

        for (int i = 0; i < size; ++i)
        {
            sum += arr[i];
        }

        return (double) sum / (double) size;
    }

    /**
     * Returns the average value of the elements in the given array
     * @param arr
     * @return
     */
    public static double average(double[] arr)
    {
        final int size = arr.length;

        if (size == 0)
        {
            return 0;
        }

        double sum = 0;

        for (int i = 0; i < size; ++i)
        {
            sum += arr[i];
        }

        return sum / (double) size;
    }

    public static double roundUp(double value, double interval)
    {
        if (interval == 0.0)
        {
            return 0.0;
        }
        else if (value == 0.0)
        {
            return interval;
        }
        else
        {
            if (value < 0.0)
            {
                interval *= -1.0;
            }

            double remainder = value % interval;

            return remainder == 0.0 ? value : value + interval - remainder;
        }
    }

    public static long roundUp(long number, long interval)
    {
        if (interval == 0)
        {
            return 0;
        }
        else if (number == 0)
        {
            return interval;
        }
        else
        {
            if (number < 0)
            {
                interval *= -1;
            }

            long i = number % interval;
            return i == 0 ? number : number + interval - i;
        }
    }

    /**
     * Returns the minimum value from the given array
     * @param arr
     * @return
     */
    public static int getMinValue(int[] arr)
    {
        if (arr.length == 0)
        {
            throw new IllegalArgumentException("Empty array");
        }

        final int size = arr.length;
        int minValue = arr[0];

        for (int i = 1; i < size; ++i)
        {
            if (arr[i] < minValue)
            {
                minValue = arr[i];
            }
        }

        return minValue;
    }

    /**
     * Returns the maximum value from the given array
     * @param arr
     * @return
     */
    public static int getMaxValue(int[] arr)
    {
        if (arr.length == 0)
        {
            throw new IllegalArgumentException("Empty array");
        }

        final int size = arr.length;
        int maxValue = arr[0];

        for (int i = 1; i < size; ++i)
        {
            if (arr[i] > maxValue)
            {
                maxValue = arr[i];
            }
        }

        return maxValue;
    }

    /**
     * Returns the minimum value from the given array
     * @param arr
     * @return
     */
    public static long getMinValue(long[] arr)
    {
        if (arr.length == 0)
        {
            throw new IllegalArgumentException("Empty array");
        }

        final int size = arr.length;
        long minValue = arr[0];

        for (int i = 1; i < size; ++i)
        {
            if (arr[i] < minValue)
            {
                minValue = arr[i];
            }
        }

        return minValue;
    }

    /**
     * Returns the maximum value from the given array
     * @param arr
     * @return
     */
    public static long getMaxValue(long[] arr)
    {
        if (arr.length == 0)
        {
            throw new IllegalArgumentException("Empty array");
        }

        final int size = arr.length;
        long maxValue = arr[0];

        for (int i = 1; i < size; ++i)
        {
            if (arr[i] > maxValue)
            {
                maxValue = arr[i];
            }
        }

        return maxValue;
    }
}