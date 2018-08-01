package baritone.bot;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Brady
 * @since 7/31/2018 10:29 PM
 */
public final class HookStateManager {

    HookStateManager() {}

    public final boolean shouldCancelDebugRenderRight() {
        return false;
    }

    public final boolean shouldOverrideDebugInfoLeft() {
        return false;
    }

    public final List<String> getDebugInfoLeft() {
        return new ArrayList<>();
    }
}
