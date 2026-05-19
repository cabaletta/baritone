package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.PutOutFireTask;
import adris.altoclef.tasks.entity.KillEntitiesTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.RunAwayFromHostilesTask;
import adris.altoclef.tasks.movement.SearchChunkForBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Optional;
import java.util.function.Predicate;

public class CollectBlazeRodsTask extends ResourceTask {

    private static final double SPAWNER_BLAZE_RADIUS = 32;
    private static final double TOO_LITTLE_HEALTH_BLAZE = 10;
    private static final int TOO_MANY_BLAZES = 5;
    private final int _count;
    private final Task _searcher = new SearchChunkForBlockTask(Blocks.NETHER_BRICKS);

    // Why was this here???
    //private Entity _toKill;
    private BlockPos _foundBlazeSpawner = null;

    public CollectBlazeRodsTask(int count) {
        super(Items.BLAZE_ROD, count);
        _count = count;
    }

    private static boolean isHoveringAboveLavaOrTooHigh(AltoClef mod, Entity entity) {
        int MAX_HEIGHT = 11;
        for (BlockPos check = entity.getBlockPos(); entity.getBlockPos().getY() - check.getY() < MAX_HEIGHT; check = check.down()) {
            if (mod.getWorld().getBlockState(check).getBlock() == Blocks.LAVA) return true;
            if (WorldHelper.isSolid(mod, check)) return false;
        }
        return true;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.SPAWNER);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // We must go to the nether.
        if (WorldHelper.getCurrentDimension() != Dimension.NETHER) {
            setDebugState("Going to nether");
            return new DefaultGoToDimensionTask(Dimension.NETHER);
        }
        // If there is a blaze, kill it.
        Predicate<Entity> safeToPursue = entity -> !isHoveringAboveLavaOrTooHigh(mod, entity);
        Optional<Entity> toKill;
        toKill = mod.getEntityTracker().getClosestEntity(safeToPursue, BlazeEntity.class);
        if (toKill.isPresent()) {
            if (mod.getPlayer().getHealth() <= TOO_LITTLE_HEALTH_BLAZE &&
                    mod.getEntityTracker().getTrackedEntities(BlazeEntity.class).size() >= TOO_MANY_BLAZES) {
                setDebugState("Running away as there are too many blazes nearby.");
                return new RunAwayFromHostilesTask(15 * 2, true);
            }
            if (_foundBlazeSpawner != null) {
                Entity kill = toKill.get();
                Vec3d nearest = kill.getPos();
                double sqDistanceToPlayer = nearest.squaredDistanceTo(mod.getPlayer().getPos());//_foundBlazeSpawner.getX(), _foundBlazeSpawner.getY(), _foundBlazeSpawner.getZ());
                // Ignore if the blaze is too far away.
                if (sqDistanceToPlayer > SPAWNER_BLAZE_RADIUS * SPAWNER_BLAZE_RADIUS) {
                    // If the blaze can see us it needs to go lol
                    BlockHitResult hit = mod.getWorld().raycast(new RaycastContext(mod.getPlayer().getCameraPosVec(1.0F), kill.getCameraPosVec(1.0F), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mod.getPlayer()));
                    if (hit != null && hit.getBlockPos().getSquaredDistance(mod.getPlayer().getPos()) < sqDistanceToPlayer) {
                        toKill = Optional.empty();
                    }
                }
            }
        }
        if (toKill.isPresent()) {
            setDebugState("Killing blaze");
            return new KillEntitiesTask(safeToPursue, toKill.get().getClass());
        }


        // If the blaze spawner somehow isn't valid
        if (_foundBlazeSpawner != null && mod.getChunkTracker().isChunkLoaded(_foundBlazeSpawner) && !isValidBlazeSpawner(mod, _foundBlazeSpawner)) {
            Debug.logMessage("Blaze spawner at " + _foundBlazeSpawner + " too far away or invalid. Re-searching.");
            _foundBlazeSpawner = null;
        }

        // If we have a blaze spawner, go near it.
        if (_foundBlazeSpawner != null) {
            if (!_foundBlazeSpawner.isWithinDistance(mod.getPlayer().getPos(), 4)) {
                setDebugState("Going to blaze spawner");
                return new GetToBlockTask(_foundBlazeSpawner.up(), false);
            }
            // Put out fire that might mess with us.
            Optional<BlockPos> nearestFire = mod.getBlockTracker().getNearestWithinRange(_foundBlazeSpawner, 5, Blocks.FIRE);
            if (nearestFire.isPresent()) {
                setDebugState("Clearing fire around spawner to prevent loss of blaze rods.");
                return new PutOutFireTask(nearestFire.get());
            }
            setDebugState("Waiting near blaze spawner for blazes to spawn");
            return null;
        }
        // Search for blaze
        if (mod.getBlockTracker().isTracking(Blocks.SPAWNER)) {
            Optional<BlockPos> spawner = mod.getBlockTracker().getNearestTracking(Blocks.SPAWNER);
            if (spawner.isPresent() && isValidBlazeSpawner(mod, spawner.get())) {
                _foundBlazeSpawner = spawner.get();
            }
        }
        // We need to find our fortress.
        setDebugState("Searching for fortress/Traveling around fortress");
        return _searcher;
    }

    private boolean isValidBlazeSpawner(AltoClef mod, BlockPos pos) {
        if (!mod.getChunkTracker().isChunkLoaded(pos)) {
            // If unloaded, go to it. Unless it's super far away.
            return false;
            //return pos.isWithinDistance(mod.getPlayer().getPos(),3000);
        }
        return WorldHelper.getSpawnerEntity(mod, pos) instanceof BlazeEntity;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.SPAWNER);
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectBlazeRodsTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect " + _count + " blaze rods";
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }
}
