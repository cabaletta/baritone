package baritone.util;

/**
 *
 * @author avecowa
 */
public abstract class ManagerTick extends Manager {
    public static boolean tickPath = false;
    @Override
    protected final void onTick() {
        if (tickPath) {
            if (onTick0()) {
                tickPath = false;
            }
        }
    }
    protected abstract boolean onTick0();
}
