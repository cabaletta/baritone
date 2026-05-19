package adris.altoclef.tasks.construction.compound;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.squashed.CataloguedResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class ConstructIronGolemTask extends Task {
    private BlockPos _position;
    private boolean _canBeFinished = false;

    public ConstructIronGolemTask() {

    }

    public ConstructIronGolemTask(BlockPos pos) {
        _position = pos;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(Items.IRON_BLOCK, Items.CARVED_PUMPKIN);
        mod.getClientBaritoneSettings().blocksToAvoidBreaking.value.add(Blocks.IRON_BLOCK);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!StorageHelper.itemTargetsMetInventory(mod, golemMaterials(mod))) {
            setDebugState("Getting materials for the iron golem");
            return new CataloguedResourceTask(golemMaterials(mod));
        }
        if (_position == null) {
            for (BlockPos pos : WorldHelper.scanRegion(mod,
                    new BlockPos(mod.getPlayer().getBlockX(), 64, mod.getPlayer().getBlockZ()),
                    new BlockPos(mod.getPlayer().getBlockX(), 128, mod.getPlayer().getBlockZ()))) {
                if (mod.getWorld().getBlockState(pos).getBlock() == Blocks.AIR) {
                    _position = pos;
                    break;
                }
            }
            if (_position == null) {
                _position = mod.getPlayer().getBlockPos();
            }
        }
        if (!WorldHelper.isBlock(mod, _position, Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(mod, _position, Blocks.AIR)) {
                setDebugState("Destroying block in way of base iron block");
                return new DestroyBlockTask(_position);
            }
            setDebugState("Placing the base iron block");
            return new PlaceBlockTask(_position, Blocks.IRON_BLOCK);
        }
//        mod.getPlayer().getServer().getPlayerManager().getPlayer("camelCasedSnivy").getAdvancementTracker()
        if (!WorldHelper.isBlock(mod, _position.up(), Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(mod, _position.up(), Blocks.AIR)) {
                setDebugState("Destroying block in way of center iron block");
                return new DestroyBlockTask(_position.up());
            }
            setDebugState("Placing the center iron block");
            return new PlaceBlockTask(_position.up(), Blocks.IRON_BLOCK);
        }
        if (!WorldHelper.isBlock(mod, _position.up().east(), Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(mod, _position.up().east(), Blocks.AIR)) {
                setDebugState("Destroying block in way of east iron block");
                return new DestroyBlockTask(_position.up().east());
            }
            setDebugState("Placing the east iron block");
            return new PlaceBlockTask(_position.up().east(), Blocks.IRON_BLOCK);
        }
        if (!WorldHelper.isBlock(mod, _position.up().west(), Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(mod, _position.up().west(), Blocks.AIR)) {
                setDebugState("Destroying block in way of west iron block");
                return new DestroyBlockTask(_position.up().west());
            }
            setDebugState("Placing the west iron block");
            return new PlaceBlockTask(_position.up().west(), Blocks.IRON_BLOCK);
        }
        if (!WorldHelper.isBlock(mod, _position.east(), Blocks.AIR)) {
            setDebugState("Clearing area on east side...");
            return new DestroyBlockTask(_position.east());
        }
        if (!WorldHelper.isBlock(mod, _position.west(), Blocks.AIR)) {
            setDebugState("Clearing area on west side...");
            return new DestroyBlockTask(_position.west());
        }
        if (!WorldHelper.isBlock(mod, _position.up(2), Blocks.AIR)) {
            setDebugState("Destroying block in way of pumpkin");
            return new DestroyBlockTask(_position.up(2));
        }
        _canBeFinished = true;
        setDebugState("Placing the pumpkin (I think)");
        return new PlaceBlockTask(_position.up(2), Blocks.CARVED_PUMPKIN);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritoneSettings().blocksToAvoidBreaking.value.remove(Blocks.IRON_BLOCK);
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof ConstructIronGolemTask;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (_position == null) return false;
        Optional<Entity> closestIronGolem = mod.getEntityTracker().getClosestEntity(new Vec3d(_position.getX(), _position.getY(), _position.getZ()), IronGolemEntity.class);
        return closestIronGolem.isPresent() && closestIronGolem.get().getBlockPos().isWithinDistance(_position, 2) && _canBeFinished;
    }

    @Override
    protected String toDebugString() {
        return "Construct Iron Golem";
    }

    private int ironBlocksNeeded(AltoClef mod) {
        if (_position == null) {
            return 4;
        }
        int needed = 0;
        if (mod.getWorld().getBlockState(_position).getBlock() != Blocks.IRON_BLOCK)
            needed++;
        if (mod.getWorld().getBlockState(_position.up().west()).getBlock() != Blocks.IRON_BLOCK)
            needed++;
        if (mod.getWorld().getBlockState(_position.up().east()).getBlock() != Blocks.IRON_BLOCK)
            needed++;
        if (mod.getWorld().getBlockState(_position.up()).getBlock() != Blocks.IRON_BLOCK)
            needed++;
        return needed;
    }

    private ItemTarget[] golemMaterials(AltoClef mod) {
        if (_position == null || mod.getWorld().getBlockState(_position.up(2)).getBlock() != Blocks.CARVED_PUMPKIN)
            return new ItemTarget[]{
                    new ItemTarget(Items.IRON_BLOCK, ironBlocksNeeded(mod)),
                    new ItemTarget(Items.CARVED_PUMPKIN, 1)
            };
        else return new ItemTarget[]{
                new ItemTarget(Items.IRON_BLOCK, ironBlocksNeeded(mod))
        };
    }
}
