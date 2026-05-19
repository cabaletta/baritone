package adris.altoclef.util.progresscheck;

/**
 * Used to determine when a task/command is not making any progress over a threshold period of time.
 */

public interface IProgressChecker<T> {
    void setProgress(T progress);

    boolean failed();

    void reset();
}
