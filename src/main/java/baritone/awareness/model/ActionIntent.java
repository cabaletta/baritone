package baritone.awareness.model;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/** The chosen action mode for this tick, plus optional target data. */
public final class ActionIntent {

    public static final ActionIntent IDLE = new ActionIntent(ActionMode.IDLE, 0f, null, null);

    public final ActionMode mode;
    /** Utility score that produced this intent (0-1). */
    public final float utility;
    /** Relevant entity target, or null. */
    public final Entity targetEntity;
    /** Relevant position target, or null. */
    public final BlockPos targetPos;

    public ActionIntent(ActionMode mode, float utility) {
        this(mode, utility, null, null);
    }

    public ActionIntent(ActionMode mode, float utility, Entity targetEntity, BlockPos targetPos) {
        this.mode = mode;
        this.utility = utility;
        this.targetEntity = targetEntity;
        this.targetPos = targetPos;
    }
}
