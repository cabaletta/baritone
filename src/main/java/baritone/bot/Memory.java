package baritone.bot;

import net.minecraft.util.math.BlockPos;

/**
 * @author Brady
 * @since 7/31/2018 10:50 PM
 */
public final class Memory {

    public final void scanBlock(BlockPos pos) {
        checkActive(() -> {
            // We might want to always run this method even if Baritone
            // isn't active, this is just an example of the implementation
            // of checkActive(Runnable).
        });
    }

    private void checkActive(Runnable runnable) {
        if (Baritone.INSTANCE.isActive()) {
            runnable.run();
        }
    }
}
