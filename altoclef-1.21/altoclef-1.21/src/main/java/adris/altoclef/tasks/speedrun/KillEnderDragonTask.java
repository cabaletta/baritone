package adris.altoclef.tasks.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.entity.AbstractKillEntityTask;
import adris.altoclef.tasks.entity.DoToClosestEntityTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Here we go
 * the final stretch
 * <p>
 * Until something inevitably fucks up and I gotta go back here to fix it
 * in which case this'll be pretty ironic.
 */
public class KillEnderDragonTask extends Task {

    private static final String[] DIAMOND_ARMORS = new String[]{"diamond_chestplate", "diamond_leggings", "diamond_helmet", "diamond_boots"};
    // Don't accidentally anger endermen lol
    private final TimerGame _lookDownTimer = new TimerGame(0.5);
    private final Task _collectBuildMaterialsTask = new MineAndCollectTask(new ItemTarget(Items.END_STONE, 100), new Block[]{Blocks.END_STONE}, MiningRequirement.WOOD);
    private final PunkEnderDragonTask _punkTask = new PunkEnderDragonTask();
    private BlockPos _exitPortalTop;

    private static Task getPickupTaskIfAny(AltoClef mod, Item... itemsToPickup) {
        for (Item check : itemsToPickup) {
            if (mod.getEntityTracker().itemDropped(check)) {
                return new PickupDroppedItemTask(new ItemTarget(check), true);
            }
        }
        return null;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL);
        // Don't forcefield endermen.
        mod.getBehaviour().addForceFieldExclusion(entity -> entity instanceof EndermanEntity || entity instanceof EnderDragonEntity || entity instanceof EnderDragonPart);
        mod.getBehaviour().setPreferredStairs(true);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_exitPortalTop == null) {
            _exitPortalTop = locateExitPortalTop(mod);
        }

        // Collect the following if dropped:
        // - Diamond Sword
        // - Diamond Armor
        // - Food (List)

        List<Item> toPickUp = new ArrayList<>(Arrays.asList(Items.DIAMOND_SWORD, Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_HELMET));
        if (StorageHelper.calculateInventoryFoodScore(mod) < 10) {
            toPickUp.addAll(Arrays.asList(
                    Items.BREAD, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_MUTTON, Items.COOKED_RABBIT, Items.COOKED_PORKCHOP
            ));
        }

        Task pickupDrops = getPickupTaskIfAny(mod, toPickUp.toArray(Item[]::new));
        if (pickupDrops != null) {
            setDebugState("Picking up drops in end.");
            return pickupDrops;
        }

        // If not equipped diamond armor and we have any, equip it.
        for (Item armor : ItemHelper.DIAMOND_ARMORS) {
            try {
                if (mod.getItemStorage().hasItem(armor) && !StorageHelper.isArmorEquipped(mod, armor)) {
                    setDebugState("Equipping " + armor);
                    return new EquipArmorTask(armor);
                }
            } catch (NullPointerException e) {
                // Should never happen.
                Debug.logError("NullpointerException that Should never happen.");
                e.printStackTrace();
            }
        }

        if (!isRailingOnDragon() && _lookDownTimer.elapsed() && !mod.getControllerExtras().isBreakingBlock()) {
            if (mod.getPlayer().isOnGround()) {
                _lookDownTimer.reset();
                mod.getClientBaritone().getLookBehavior().updateTarget(new Rotation(0f, 90f), true);
            }
        }

        // If there is a portal, enter it.
        if (mod.getBlockTracker().anyFound(Blocks.END_PORTAL)) {
            setDebugState("Entering portal to beat the game.");
            return new DoToClosestBlockTask(
                    blockPos -> new GetToBlockTask(blockPos.up(), false),
                    Blocks.END_PORTAL
            );
        }

        // If we have no building materials (stone + cobble + end stone), get end stone
        // If there are crystals, suicide blow em up.
        // If there are no crystals, punk the dragon if it's close.
        int MINIMUM_BUILDING_BLOCKS = 1;
        if (mod.getEntityTracker().entityFound(EndCrystalEntity.class) && mod.getItemStorage().getItemCount(Items.DIRT, Items.COBBLESTONE, Items.NETHERRACK, Items.END_STONE) < MINIMUM_BUILDING_BLOCKS || (_collectBuildMaterialsTask.isActive() && !_collectBuildMaterialsTask.isFinished(mod))) {
            if (StorageHelper.miningRequirementMetInventory(mod, MiningRequirement.WOOD)) {
                mod.getBehaviour().addProtectedItems(Items.END_STONE);
                setDebugState("Collecting building blocks to pillar to crystals");
                return _collectBuildMaterialsTask;
            }
        } else {
            mod.getBehaviour().removeProtectedItems(Items.END_STONE);
        }

        // Blow up the nearest end crystal
        if (mod.getEntityTracker().entityFound(EndCrystalEntity.class)) {
            setDebugState("Kamakazeeing crystals");
            return new DoToClosestEntityTask(
                    (toDestroy) -> {
                        if (toDestroy.isInRange(mod.getPlayer(), 7)) {
                            mod.getControllerExtras().attack(toDestroy);
                        }
                        // Go next to the crystal, arbitrary where we just need to get close.
                        return new GetToBlockTask(toDestroy.getBlockPos().add(1, 0, 0), false);
                    },
                    EndCrystalEntity.class
            );
        }

        // Punk dragon
        if (mod.getEntityTracker().entityFound(EnderDragonEntity.class)) {
            setDebugState("Punking dragon");
            return _punkTask;
        }
        setDebugState("Couldn't find ender dragon... This can be very good or bad news.");
        return null;
        //return new KillEntitiesTask(EnderDragonEntity.class);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof KillEnderDragonTask;
    }

    @Override
    protected String toDebugString() {
        return "Killing Ender Dragon";
    }

    private boolean isRailingOnDragon() {
        return _punkTask.getMode() == Mode.RAILING;
    }

    private BlockPos locateExitPortalTop(AltoClef mod) {
        if (!mod.getChunkTracker().isChunkLoaded(new BlockPos(0, 64, 0))) return null;
        int height = WorldHelper.getGroundHeight(mod, 0, 0, Blocks.BEDROCK);
        if (height != -1) return new BlockPos(0, height, 0);
        return null;
    }

    private enum Mode {
        WAITING_FOR_PERCH,
        RAILING
    }

    private class PunkEnderDragonTask extends Task {

        private final HashMap<BlockPos, Double> _breathCostMap = new HashMap<>();
        private final TimerGame _hitHoldTimer = new TimerGame(0.1);
        private final TimerGame _hitResetTimer = new TimerGame(0.4);
        private final TimerGame _randomWanderChangeTimeout = new TimerGame(20);
        private Mode _mode = Mode.WAITING_FOR_PERCH;

        private BlockPos _randomWanderPos;
        private boolean _wasHitting;
        private boolean _wasReleased;

        private PunkEnderDragonTask() {
        }

        public Mode getMode() {
            return _mode;
        }

        private void hit(AltoClef mod) {
            mod.getExtraBaritoneSettings().setInteractionPaused(true);
            if (!_wasHitting) {
                _wasHitting = true;
                _wasReleased = false;
                _hitHoldTimer.reset();
                _hitResetTimer.reset();
                Debug.logInternal("HIT");
                mod.getInputControls().tryPress(Input.CLICK_LEFT);
                //mod.getPlayer().swingHand(Hand.MAIN_HAND);
            }
            if (_hitHoldTimer.elapsed()) {
                if (!_wasReleased) {
                    Debug.logInternal("    up");
                    //mod.getControllerExtras().mouseClickOverride(0, false);
                    _wasReleased = true;
                }
            }
            if (_wasHitting && _hitResetTimer.elapsed() && mod.getPlayer().getAttackCooldownProgress(0) > 0.99) {
                _wasHitting = false;
                // Code duplication maybe?
                //mod.getControllerExtras().mouseClickOverride(0, false);
                mod.getExtraBaritoneSettings().setInteractionPaused(false);
                _hitResetTimer.reset();
            }
        }

        private void stopHitting(AltoClef mod) {
            if (_wasHitting) {
                //MinecraftClient.getInstance().options.keyAttack.setPressed(false);
                if (!_wasReleased) {
                    //mod.getControllerExtras().mouseClickOverride(0, false);
                    mod.getExtraBaritoneSettings().setInteractionPaused(false);
                    _wasReleased = true;
                }
                _wasHitting = false;
            }
        }


        @Override
        protected void onStart(AltoClef mod) {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        }

        @Override
        protected Task onTick(AltoClef mod) {
            if (!mod.getEntityTracker().entityFound(EnderDragonEntity.class)) {
                setDebugState("No dragon found.");
                return null;
            }
            Optional<Entity> dragon = mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class);
            if (dragon.isPresent()) {
                EnderDragonEntity dragonEntity = (EnderDragonEntity) dragon.get();
                Phase dragonPhase = dragonEntity.getPhaseManager().getCurrent();
                //Debug.logInternal("PHASE: " + dragonPhase);
                boolean perchingOrGettingReady = dragonPhase.getType() == PhaseType.LANDING || dragonPhase.isSittingOrHovering();
                switch (_mode) {
                    case RAILING -> {
                        if (!perchingOrGettingReady) {
                            Debug.logMessage("Dragon no longer perching.");
                            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                            _mode = Mode.WAITING_FOR_PERCH;
                            break;
                        }
                        //DamageSource.DRAGON_BREATH
                        Entity head = dragonEntity.head;
                        // Go for the head
                        if (head.isInRange(mod.getPlayer(), 7.5) && dragonEntity.ticksSinceDeath <= 1) {
                            // Equip weapon
                            AbstractKillEntityTask.equipWeapon(mod);
                            // Look torwards da dragon
                            Vec3d targetLookPos = head.getPos().add(0, 3, 0);
                            Rotation targetRotation = RotationUtils.calcRotationFromVec3d(mod.getClientBaritone().getPlayerContext().playerHead(), targetLookPos, mod.getClientBaritone().getPlayerContext().playerRotations());
                            mod.getClientBaritone().getLookBehavior().updateTarget(targetRotation, true);
                            // Also look towards da dragon
                            MinecraftClient.getInstance().options.getAutoJump().setValue(false);
                            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                            hit(mod);
                        } else {
                            stopHitting(mod);
                        }
                        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                            // Set goal to closest block within the pillar that's by the head.
                            if (_exitPortalTop != null) {
                                int bottomYDelta = -3;
                                BlockPos closest = null;
                                double closestDist = Double.POSITIVE_INFINITY;
                                for (int dx = -2; dx <= 2; ++dx) {
                                    for (int dz = -2; dz <= 2; ++dz) {
                                        // We have sort of a rounded circle here.
                                        if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                                        BlockPos toCheck = _exitPortalTop.add(dx, bottomYDelta, dz);
                                        double distSq = toCheck.getSquaredDistance(head.getPos());
                                        if (distSq < closestDist) {
                                            closest = toCheck;
                                            closestDist = distSq;
                                        }
                                    }
                                }
                                if (closest != null) {
                                    mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(
                                            new GoalGetToBlock(closest)
                                    );
                                }
                            }
                        }
                        setDebugState("Railing on dragon");
                    }
                    case WAITING_FOR_PERCH -> {
                        stopHitting(mod);
                        if (perchingOrGettingReady) {
                            // We're perching!!
                            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                            Debug.logMessage("Dragon perching detected. Dabar duosiu į snuki.");
                            _mode = Mode.RAILING;
                            break;
                        }
                        // Run around aimlessly, dodging dragon fire
                        if (_randomWanderPos != null && WorldHelper.inRangeXZ(mod.getPlayer(), _randomWanderPos, 2)) {
                            _randomWanderPos = null;
                        }
                        if (_randomWanderPos != null && _randomWanderChangeTimeout.elapsed()) {
                            _randomWanderPos = null;
                            Debug.logMessage("Reset wander pos after timeout, oof");
                        }
                        if (_randomWanderPos == null) {
                            _randomWanderPos = getRandomWanderPos(mod);
                            _randomWanderChangeTimeout.reset();
                            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                        }
                        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(
                                    new GoalGetToBlock(_randomWanderPos)
                            );
                        }
                        setDebugState("Waiting for perch");
                    }
                }
            }
            return null;
        }

        @Override
        protected void onStop(AltoClef mod, Task interruptTask) {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, false);
            //mod.getControllerExtras().mouseClickOverride(0, false);
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
        }

        @Override
        protected boolean isEqual(Task other) {
            return other instanceof PunkEnderDragonTask;
        }

        @Override
        protected String toDebugString() {
            return "Punking the dragon";
        }

        private BlockPos getRandomWanderPos(AltoClef mod) {
            double RADIUS_RANGE = 45;
            double MIN_RADIUS = 7;
            BlockPos pos = null;
            int allowed = 5000;

            while (pos == null) {
                if (allowed-- < 0) {
                    Debug.logWarning("Failed to find random solid ground in end, this may lead to problems.");
                    return null;
                }
                double radius = MIN_RADIUS + (RADIUS_RANGE - MIN_RADIUS) * Math.random();
                double angle = Math.PI * 2 * Math.random();
                int x = (int) (radius * Math.cos(angle)),
                        z = (int) (radius * Math.sin(angle));
                int y = WorldHelper.getGroundHeight(mod, x, z);
                if (y == -1) continue;
                BlockPos check = new BlockPos(x, y, z);
                if (mod.getWorld().getBlockState(check).getBlock() == Blocks.END_STONE) {
                    // We found a spot!
                    pos = check.up();
                }
            }
            return pos;
        }
    }
}
