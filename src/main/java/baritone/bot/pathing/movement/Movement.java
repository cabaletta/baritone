package baritone.bot.pathing.movement;

import baritone.bot.Baritone;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Helper;
import baritone.bot.utils.ToolSet;
import baritone.bot.utils.Utils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public abstract class Movement implements Helper, MovementHelper {

    private MovementState currentState = new MovementState().setStatus(MovementStatus.PREPPING);
    protected final BlockPos src;

    protected final BlockPos dest;

    /**
     * The positions that need to be broken before this movement can ensue
     */
    public final BlockPos[] positionsToBreak;

    /**
     * The positions where we need to place a block before this movement can ensue
     */
    public final BlockPos[] positionsToPlace;

    private Double cost;

    protected Movement(BlockPos src, BlockPos dest, BlockPos[] toBreak, BlockPos[] toPlace) {
        this.src = src;
        this.dest = dest;
        this.positionsToBreak = toBreak;
        this.positionsToPlace = toPlace;
    }

    protected Movement(BlockPos src, BlockPos dest, BlockPos[] toBreak, BlockPos[] toPlace, Vec3d rotationTarget) {
        this(src, dest, toBreak, toPlace);
//        currentState = new MovementState()
//                .setGoal(new )
//                .setStatus(MovementStatus.WAITING);
    }

    public double getCost(ToolSet ts) {
        if (cost == null) {
            if (ts == null) {
                ts = new ToolSet();
            }
            cost = calculateCost(ts);
        }
        return cost;
    }

    public double getTotalHardnessOfBlocksToBreak(ToolSet ts) {
        /*
        double sum = 0;
        HashSet<BlockPos> toBreak = new HashSet();
        for (BlockPos positionsToBreak1 : positionsToBreak) {
            toBreak.add(positionsToBreak1);
            if (this instanceof ActionFall) {//if we are digging straight down, assume we have already broken the sand above us
                continue;
            }
            BlockPos tmp = positionsToBreak1.up();
            while (canFall(tmp)) {
                toBreak.add(tmp);
                tmp = tmp.up();
            }
        }
        for (BlockPos pos : toBreak) {
            sum += getHardness(ts, Baritone.get(pos), pos);
            if (sum >= COST_INF) {
                return COST_INF;
            }
        }
        if (!Baritone.allowBreakOrPlace || !Baritone.hasThrowaway) {
            for (int i = 0; i < blocksToPlace.length; i++) {
                if (!canWalkOn(positionsToPlace[i])) {
                    return COST_INF;
                }
            }
        }*/
        //^ the above implementation properly deals with falling blocks, TODO integrate
        double sum = 0;
        for (BlockPos pos : positionsToBreak) {
            sum += MovementHelper.getMiningDurationTicks(ts, BlockStateInterface.get(pos), pos);
            if (sum >= COST_INF) {
                return COST_INF;
            }
        }
        return sum;
    }

    protected abstract double calculateCost(ToolSet ts); // TODO pass in information like whether it's allowed to place throwaway blocks

    public double recalculateCost() {
        cost = null;
        return getCost(null);
    }

    /**
     * Handles the execution of the latest Movement
     * State, and offers a Status to the calling class.
     *
     * @return Status
     */
    public MovementStatus update() {
//        if(isPrepared(state)) {
//            if (!currentState.isPresent()) {
//                currentState = Optional.of(new MovementState()
//                        .setStatus(MovementStatus.WAITING)
//                        .setGoal());
//            }
//        }
        if (isFinished()) {

        }
        MovementState latestState = updateState(currentState);
        Tuple<Float, Float> rotation = Utils.calcRotationFromVec3d(player().getPositionEyes(1.0F),
                latestState.getGoal().rotation);
        player().setPositionAndRotation(player().posX, player().posY, player().posZ,
                rotation.getFirst(), rotation.getSecond());
        //TODO calculate movement inputs from latestState.getGoal().position
        latestState.inputState.forEach((input, forced) -> {
            Baritone.INSTANCE.getInputOverrideHandler().setInputForceState(input, forced);
        });
        currentState = latestState;

        if (isFinished())
            onFinish();

        return currentState.getStatus();
    }

    private boolean prepare(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        Optional<BlockPos> cruftPos;
        for (BlockPos blockPos : positionsToBreak) {
            if (MovementHelper.canWalkThrough(blockPos, BlockStateInterface.get(blockPos))) {

            }
        }
        return true;
    }

    public boolean isFinished() {
        return (currentState.getStatus() != MovementStatus.RUNNING
                && currentState.getStatus() != MovementStatus.WAITING);
    }

    public BlockPos getSrc() {
        return src;
    }

    public BlockPos getDest() {
        return dest;
    }

    /**
     * Run cleanup on state finish
     */
    public abstract void onFinish();

    /**
     * Calculate latest movement state.
     * Gets called once a tick.
     *
     * @return
     */
    public MovementState updateState(MovementState state) {
        if (!prepare(state))
            return state.setStatus(MovementStatus.PREPPING);
        return state;
    }
}
