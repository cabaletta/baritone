package baritone.bot.pathfinding.actions;

public final class ActionCosts {
    private ActionCosts() {}

    //These costs are measured roughly in ticks btw
    public static final double WALK_ONE_BLOCK_COST = 20 / 4.317;
    public static final double WALK_ONE_IN_WATER_COST = 20 / 2.2;
    public static final double JUMP_ONE_BLOCK_COST = 5.72854;//see below calculation for fall. 1.25 blocks
    public static final double LADDER_UP_ONE_COST = 20 / 2.35;
    public static final double LADDER_DOWN_ONE_COST = 20 / 3;
    public static final double SNEAK_ONE_BLOCK_COST = 20 / 1.3;
    public static final double SPRINT_ONE_BLOCK_COST = 20 / 5.612;
    /**
     * Doesn't include walking forwards, just the falling
     *
     * Based on a sketchy formula from minecraftwiki
     *
     * d(t) = 3.92 × (99 - 49.50×(0.98^t+1) - t)
     *
     * Solved in mathematica
     */
    public static final double FALL_ONE_BLOCK_COST = 5.11354;
    public static final double FALL_TWO_BLOCK_COST = 7.28283;
    public static final double FALL_THREE_BLOCK_COST = 8.96862;
    /**
     * It doesn't actually take ten ticks to place a block, this cost is so high
     * because we want to generally conserve blocks which might be limited
     */
    public static final double PLACE_ONE_BLOCK_COST = 20;
    /**
     * Add this to the cost of breaking any block. The cost of breaking any
     * block is calculated as the number of ticks that block takes to break with
     * the tools you have. You add this because there's always a little overhead
     * (e.g. looking at the block)
     */
    public static final double BREAK_ONE_BLOCK_ADD = 4;
    public static final double COST_INF = 1000000;
}
