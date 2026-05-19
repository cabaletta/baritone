package adris.altoclef.tasks.stupid;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.container.SmeltInFurnaceTask;
import adris.altoclef.tasks.entity.DoToClosestEntityTask;
import adris.altoclef.tasks.entity.KillPlayerTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.movement.RunAwayFromEntitiesTask;
import adris.altoclef.tasks.movement.SearchChunksExploreTask;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.ui.MessagePriority;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Roams around the world to terminate Sarah Khaannah
 */
public class TerminatorTask extends Task {

    private static final int FEAR_SEE_DISTANCE = 30;
    private static final int FEAR_DISTANCE = 20;
    private static final int RUN_AWAY_DISTANCE = 80;

    private static final int MIN_BUILDING_BLOCKS = 10;
    private static final int PREFERRED_BUILDING_BLOCKS = 60;

    private static Item[] GEAR_TO_COLLECT = new Item[]{
            Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_SWORD, Items.WATER_BUCKET
    };
    private final Task _prepareDiamondMiningEquipmentTask = TaskCatalogue.getSquashedItemTask(
            new ItemTarget(Items.IRON_PICKAXE, 3), new ItemTarget(Items.IRON_SWORD, 1)
    );
    private final Task _foodTask = new CollectFoodTask(80);
    private final TimerGame _runAwayExtraTime = new TimerGame(10);
    private final Predicate<PlayerEntity> _canTerminate;
    private final ScanChunksInRadius _scanTask;
    private final TimerGame _funnyMessageTimer = new TimerGame(10);
    private Vec3d _closestPlayerLastPos;
    private Vec3d _closestPlayerLastObservePos;
    private Task _runAwayTask;
    private String _currentVisibleTarget;

    private Task _armorTask;

    public TerminatorTask(BlockPos center, double scanRadius, Predicate<PlayerEntity> canTerminate) {
        _canTerminate = canTerminate;
        _scanTask = new ScanChunksInRadius(center, scanRadius);
    }

    public TerminatorTask(BlockPos center, double scanRadius) {
        this(center, scanRadius, accept -> true);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().setForceFieldPlayers(true);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        Optional<Entity> closest = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), toPunk -> shouldPunk(mod, (PlayerEntity) toPunk), PlayerEntity.class);

        if (closest.isPresent()) {
            _closestPlayerLastPos = closest.get().getPos();
            _closestPlayerLastObservePos = mod.getPlayer().getPos();
        }

        if (!isReadyToPunk(mod)) {

            if (_runAwayTask != null && _runAwayTask.isActive() && !_runAwayTask.isFinished(mod)) {
                // If our last "scare" was too long ago or there are no more nearby players...
                boolean noneRemote = (closest.isEmpty() || !closest.get().isInRange(mod.getPlayer(), FEAR_DISTANCE));
                if (_runAwayExtraTime.elapsed() && noneRemote) {
                    Debug.logMessage("Stop running away, we're good.");
                    // Stop running away.
                    _runAwayTask = null;
                } else {
                    return _runAwayTask;
                }
            }

            // See if there's anyone nearby.
            if (mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), entityAccept -> {
                if (!shouldPunk(mod, (PlayerEntity) entityAccept)) {
                    return false;
                }
                if (entityAccept.isInRange(mod.getPlayer(), 15)) {
                    // We're close, count us.
                    return true;
                } else {
                    // Too far away.
                    if (!entityAccept.isInRange(mod.getPlayer(), FEAR_DISTANCE)) return false;
                    // We may be far and obstructed, check.
                    return LookHelper.seesPlayer(entityAccept, mod.getPlayer(), FEAR_SEE_DISTANCE);
                }
            }, PlayerEntity.class).isPresent()) {
                // RUN!

                _runAwayExtraTime.reset();
                try {
                    Optional<Entity> player = mod.getEntityTracker().getClosestEntity(PlayerEntity.class);
                    if (player.isPresent() && player.get() instanceof PlayerEntity playerEntity
                            && shouldPunk(mod, playerEntity)) {
                        _runAwayTask = new RunAwayFromPlayersTask(player.get(), RUN_AWAY_DISTANCE);
                    }
                } catch (ConcurrentModificationException e) {
                    // oof
                    Debug.logWarning("Duct tape over ConcurrentModificationException (see log)");
                    e.printStackTrace();
                }
                setDebugState("Running away from players.");
                return _runAwayTask;
            }
        } else {
            // We can totally punk
            if (_runAwayTask != null) {
                _runAwayTask = null;
                Debug.logMessage("Stopped running away because we can now punk.");
            }
            // Get building materials if we don't have them.
            if (PlaceStructureBlockTask.getMaterialCount(mod) < MIN_BUILDING_BLOCKS) {
                setDebugState("Collecting building materials");
                return PlaceBlockTask.getMaterialTask(PREFERRED_BUILDING_BLOCKS);
            }

            // Get some food so we can last a little longer.
            if ((mod.getPlayer().getHungerManager().getFoodLevel() < (20 - 3 * 2) || mod.getPlayer().getHealth() < 10) && StorageHelper.calculateInventoryFoodScore(mod) <= 0) {
                return _foodTask;
            }

            if (mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), toPunk -> shouldPunk(mod, (PlayerEntity) toPunk), PlayerEntity.class).isPresent()) {
                setDebugState("Punking.");
                return new DoToClosestEntityTask(
                        entity -> {
                            if (entity instanceof PlayerEntity) {
                                tryDoFunnyMessageTo(mod, (PlayerEntity) entity);
                                return new KillPlayerTask(entity.getName().getString());
                            }
                            // Should never happen.
                            Debug.logWarning("This should never happen.");
                            return _scanTask;
                        },
                        interact -> shouldPunk(mod, (PlayerEntity) interact),
                        PlayerEntity.class
                );
            }
        }

        // Get stacked first
        // Equip diamond armor asap
        if (_armorTask != null && _armorTask.isActive() && !_armorTask.isFinished(mod)) {
            setDebugState("Collecting Diamond Armor");
            return _armorTask;
        }

        // Get iron pickaxes first
        if (!mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE) && mod.getItemStorage().getItemCount(Items.DIAMOND) < 3) {
            if (!mod.getItemStorage().hasItem(Items.IRON_PICKAXE) || (_prepareDiamondMiningEquipmentTask.isActive() && !_prepareDiamondMiningEquipmentTask.isFinished(mod))) {
                setDebugState("Getting iron pickaxes to mine diamonds");
                return _prepareDiamondMiningEquipmentTask;
            }
        }

        // Collect food
        if (StorageHelper.calculateInventoryFoodScore(mod) <= 0 || (_foodTask.isActive() && !_foodTask.isFinished(mod))) {
            setDebugState("Collecting food");
            return _foodTask;
        }
        // Raw food
        for (Item raw : ItemHelper.RAW_FOODS) {
            if (mod.getItemStorage().hasItem(raw)) {
                Optional<Item> cooked = ItemHelper.getCookedFood(raw);
                if (cooked.isPresent()) {
                    int targetCount = mod.getItemStorage().getItemCount(cooked.get()) + mod.getItemStorage().getItemCount(raw);
                    setDebugState("Smelting raw food: " + ItemHelper.stripItemName(raw));
                    return new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(cooked.get(), targetCount), new ItemTarget(raw, targetCount)));
                }
            }
        }

        // If we're not all equip, do equip
        if (!StorageHelper.isArmorEquippedAll(mod, ItemHelper.DIAMOND_ARMORS)) {
            _armorTask = new EquipArmorTask(ItemHelper.DIAMOND_ARMORS);
            return _armorTask;
        }

        // Get gear one by one...
        for (Item gear : GEAR_TO_COLLECT) {
            if (!mod.getItemStorage().hasItem(gear)) {
                setDebugState("Collecting gear");
                return TaskCatalogue.getItemTask(gear, 1);
            }
        }


        setDebugState("Scanning for players...");
        _currentVisibleTarget = null;
        if (_scanTask.failedSearch()) {
            Debug.logMessage("Re-searching missed places.");
            _scanTask.resetSearch(mod);
        }

        return _scanTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof TerminatorTask;
    }

    @Override
    protected String toDebugString() {
        return "Terminator Task";
    }

    private boolean isReadyToPunk(AltoClef mod) {
        if (mod.getPlayer().getHealth() <= 5) return false; // We need to heal.
        return StorageHelper.isArmorEquippedAll(mod, ItemHelper.DIAMOND_ARMORS) && mod.getItemStorage().hasItem(Items.DIAMOND_SWORD);
    }

    private boolean shouldPunk(AltoClef mod, PlayerEntity player) {
        if (player == null || player.isDead()) return false;
        if (player.isCreative() || player.isSpectator()) return false;
        return !mod.getButler().isUserAuthorized(player.getName().getString()) && _canTerminate.test(player);
    }

    private void tryDoFunnyMessageTo(AltoClef mod, PlayerEntity player) {
        if (_funnyMessageTimer.elapsed()) {
            if (LookHelper.seesPlayer(player, mod.getPlayer(), 80)) {
                String name = player.getName().getString();
                if (_currentVisibleTarget == null || !_currentVisibleTarget.equals(name)) {
                    _currentVisibleTarget = name;
                    _funnyMessageTimer.reset();
                    String funnyMessage = getRandomFunnyMessage();
                    mod.getMessageSender().enqueueWhisper(name, funnyMessage, MessagePriority.ASAP);
                }
            }
        }
    }

    private String getRandomFunnyMessage() {
        return "Prepare to get punked, kid";
    }

    private static class RunAwayFromPlayersTask extends RunAwayFromEntitiesTask {

        public RunAwayFromPlayersTask(Entity toRunAwayFrom, double distanceToRun) {
            super(toRunAwayFrom, distanceToRun, true, 0.1);
            // More lenient progress checker
            _checker = new MovementProgressChecker();
        }

        @Override
        protected boolean isEqual(Task other) {
            return other instanceof RunAwayFromPlayersTask;
        }

        @Override
        protected String toDebugString() {
            return "Running away from players";
        }
    }

    private class ScanChunksInRadius extends SearchChunksExploreTask {

        private final BlockPos _center;
        private final double _radius;

        public ScanChunksInRadius(BlockPos center, double radius) {
            _center = center;
            _radius = radius;
        }

        @Override
        protected boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos) {
            double cx = (pos.getStartX() + pos.getEndX()) / 2.0;
            double cz = (pos.getStartZ() + pos.getEndZ()) / 2.0;
            double dx = _center.getX() - cx,
                    dz = _center.getZ() - cz;
            return dx * dx + dz * dz < _radius * _radius;
        }

        @Override
        protected ChunkPos getBestChunkOverride(AltoClef mod, List<ChunkPos> chunks) {
            // Prioritise the chunk we last saw a player in.
            if (_closestPlayerLastPos != null) {
                double lowestScore = Double.POSITIVE_INFINITY;
                ChunkPos bestChunk = null;
                if (!chunks.isEmpty()) {
                    for (ChunkPos toSearch : chunks) {
                        double cx = (toSearch.getStartX() + toSearch.getEndX() + 1) / 2.0, cz = (toSearch.getStartZ() + toSearch.getEndZ() + 1) / 2.0;
                        double px = mod.getPlayer().getX(), pz = mod.getPlayer().getZ();
                        double distanceSq = (cx - px) * (cx - px) + (cz - pz) * (cz - pz);
                        double pdx = _closestPlayerLastPos.getX() - cx, pdz = _closestPlayerLastPos.getZ() - cz;
                        double distanceToLastPlayerPos = pdx * pdx + pdz * pdz;
                        Vec3d direction = _closestPlayerLastPos.subtract(_closestPlayerLastObservePos).multiply(1, 0, 1).normalize();
                        double dirx = direction.x, dirz = direction.z;
                        double correctDistance = pdx * dirx + pdz * dirz;
                        double tempX = dirx * correctDistance,
                                tempZ = dirz * correctDistance;
                        double perpendicularDistance = ((pdx - tempX) * (pdx - tempX)) + ((pdz - tempZ) * (pdz - tempZ));
                        double score = distanceSq + distanceToLastPlayerPos * 0.6 - correctDistance * 2 + perpendicularDistance * 0.5;
                        if (score < lowestScore) {
                            lowestScore = score;
                            bestChunk = toSearch;
                        }
                    }
                }
                return bestChunk;
            }
            return super.getBestChunkOverride(mod, chunks);
        }

        @Override
        protected boolean isEqual(Task other) {
            if (other instanceof ScanChunksInRadius scan) {
                return scan._center.equals(_center) && Math.abs(scan._radius - _radius) <= 1;
            }
            return false;
        }

        @Override
        protected String toDebugString() {
            return "Scanning around a radius";
        }
    }
}
