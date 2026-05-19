package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.*;
import adris.altoclef.util.serialization.ItemDeserializer;
import adris.altoclef.util.serialization.ItemSerializer;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MLGBucketTask extends Task {

    private static MLGClutchConfig _config;

    static {
        ConfigHelper.loadConfig("configs/mlg_clutch_settings.json", MLGClutchConfig::new, MLGClutchConfig.class, newConfig -> _config = newConfig);
    }

    private BlockPos _placedPos;
    private BlockPos _movingTorwards;

    private static boolean isLava(BlockPos pos) {
        assert MinecraftClient.getInstance().world != null;
        return MinecraftClient.getInstance().world.getBlockState(pos).getBlock() == Blocks.LAVA;
    }

    private static boolean lavaWillProtect(BlockPos pos) {
        assert MinecraftClient.getInstance().world != null;
        BlockState state = MinecraftClient.getInstance().world.getBlockState(pos);
        if (state.getBlock() == Blocks.LAVA) {
            int level = state.getFluidState().getLevel();
            return level == 0 || level >= _config.lavaLevelOrGreaterWillCancelFallDamage;
        }
        return false;
    }

    private static boolean isWater(BlockPos pos) {
        assert MinecraftClient.getInstance().world != null;
        return MinecraftClient.getInstance().world.getBlockState(pos).getBlock() == Blocks.WATER;
    }

    /**
     * Can we reach this block while falling, or will gravity pull us too far?
     */
    private static boolean canTravelToInAir(BlockPos pos) {
        Entity player = MinecraftClient.getInstance().player;
        assert player != null;
        double verticalDist = player.getPos().getY() - pos.getY() - 1;
        double verticalVelocity = -1 * player.getVelocity().y;
        double grav = EntityHelper.ENTITY_GRAVITY;
        double movementSpeedPerTick = _config.averageHorizontalMovementSpeedPerTick; // Calculated, but also somewhat conservative
        // 1d projectile motion
        double ticksToTravelSq = (-verticalVelocity + Math.sqrt(verticalVelocity * verticalVelocity + 2 * grav * verticalDist)) / grav;
        double maxMoveDistanceSq = movementSpeedPerTick * movementSpeedPerTick * ticksToTravelSq * ticksToTravelSq;
        // We need to get within 1 block, so subtract a "radius" or something idk
        double horizontalDistance = WorldHelper.distanceXZ(player.getPos(), WorldHelper.toVec3d(pos)) - 0.8;
        if (horizontalDistance < 0)
            horizontalDistance = 0;
        return maxMoveDistanceSq > horizontalDistance * horizontalDistance;
    }

    private static boolean isFallDeadly(BlockPos pos) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        double damage = calculateFallDamageToLandOn(pos);
        assert MinecraftClient.getInstance().world != null;
        Block b = MinecraftClient.getInstance().world.getBlockState(pos).getBlock();
        if (b == Blocks.HAY_BLOCK) {
            damage *= 0.2f;
        }
        assert player != null;
        double resultingHealth = player.getHealth() - (float) damage;
        return resultingHealth < _config.preferLavaWhenFallDropsHealthBelowThreshold;
    }

    private static double calculateFallDamageToLandOn(BlockPos pos) {
        ClientWorld world = MinecraftClient.getInstance().world;
        PlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        double totalFallDistance = player.fallDistance + (player.getY() - pos.getY() - 1);
        // Copied from living entity I think, somewhere idk you get the picture.
        double baseFallDamage = MathHelper.ceil(totalFallDistance - 3.0F);
        // Be a bit conservative, assume MORE damage
        assert world != null;
        return EntityHelper.calculateResultingPlayerDamage(player, world.getDamageSources().fall(), baseFallDamage);
    }

    private static void moveLeftRight(AltoClef mod, int delta) {
        if (delta == 0) {
            mod.getInputControls().release(Input.MOVE_LEFT);
            mod.getInputControls().release(Input.MOVE_RIGHT);
        } else if (delta > 0) {
            mod.getInputControls().release(Input.MOVE_LEFT);
            mod.getInputControls().hold(Input.MOVE_RIGHT);
        } else {
            mod.getInputControls().hold(Input.MOVE_LEFT);
            mod.getInputControls().release(Input.MOVE_RIGHT);
        }
    }

    private static void moveForwardBack(AltoClef mod, int delta) {
        if (delta == 0) {
            mod.getInputControls().release(Input.MOVE_FORWARD);
            mod.getInputControls().release(Input.MOVE_BACK);
        } else if (delta > 0) {
            mod.getInputControls().hold(Input.MOVE_FORWARD);
            mod.getInputControls().release(Input.MOVE_BACK);
        } else {
            mod.getInputControls().release(Input.MOVE_FORWARD);
            mod.getInputControls().hold(Input.MOVE_BACK);
        }
    }

    private Task onTickInternal(AltoClef mod, BlockPos oldMovingTorwards) {
        Optional<BlockPos> willLandOn = getBlockWeWillLandOn(mod);
        Optional<BlockPos> bestClutchPos = getBestConeClutchBlock(mod, oldMovingTorwards);
        // Move torwards our best "clutch" position
        if (bestClutchPos.isPresent()) {
            _movingTorwards = bestClutchPos.get().mutableCopy();
            if (!_movingTorwards.equals(oldMovingTorwards)) {
                if (oldMovingTorwards == null)
                    Debug.logMessage("(NEW clutch target: " + _movingTorwards + ")");
                else
                    Debug.logMessage("(changed clutch target: " + _movingTorwards + ")");
            }
        } else if (oldMovingTorwards != null) {
            Debug.logMessage("(LOST clutch position!)");
        }
        if (willLandOn.isPresent()) {
            handleJumpForLand(mod, willLandOn.get());
            return placeMLGBucketTask(mod, willLandOn.get());
        } else {
            setDebugState("Wait for it...");
            // We must trigger jump as soon as we enter a "climbable" object
            mod.getInputControls().release(Input.JUMP);
            return null;
        }
    }

    private Task placeMLGBucketTask(AltoClef mod, BlockPos toPlaceOn) {
        if (!hasClutchItem(mod)) {
            setDebugState("No clutch item");
            return null;
        }
        // If our raycast hit a non-solid block, go DOWN one.
        if (!WorldHelper.isSolid(mod, toPlaceOn)) {
            toPlaceOn = toPlaceOn.down();
        }
        BlockPos willLandIn = toPlaceOn.up();
        // If we're water, we're ok. Do nothing.
        BlockState willLandInState = mod.getWorld().getBlockState(willLandIn);
        if (willLandInState.getBlock() == Blocks.WATER) {
            // We good.
            setDebugState("Waiting to fall into water");
            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
            return null;
        }

        IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
        Optional<Rotation> reachable = RotationUtils.reachable(ctx, toPlaceOn);
        if (reachable.isPresent()) {
            setDebugState("Performing MLG");
            LookHelper.lookAt(mod, reachable.get());
            // Try water by default
            boolean hasClutch = (!mod.getWorld().getDimension().ultrawarm() && mod.getSlotHandler().forceEquipItem(Items.WATER_BUCKET));
            if (!hasClutch) {
                // Go through our "clutch" items and see if any fit
                if (!_config.clutchItems.isEmpty()) {
                    for (Item tryEquip : _config.clutchItems) {
                        if (mod.getSlotHandler().forceEquipItem(tryEquip)) {
                            hasClutch = true;
                            break;
                        }
                    }
                }
            }
            if (hasClutch) {
                Debug.logMessage("HIT: " + willLandIn);
                _placedPos = willLandIn;
                mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
            } else {
                setDebugState("NOT LOOKING CORRECTLY!");
            }
        } else {
            setDebugState("Waiting to reach target block...");
        }
        return null;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // ALWAYS faster
        mod.getInputControls().hold(Input.SPRINT);
        // Check AROUND player instead of directly under.
        // We may crop the edge of a block or wall.
        BlockPos oldMovingTorwards = _movingTorwards != null ? _movingTorwards.mutableCopy() : null;
        _movingTorwards = null;
        Task result = onTickInternal(mod, oldMovingTorwards);

        handleForwardVelocity(mod, !Objects.equals(oldMovingTorwards, _movingTorwards));
        handleCancellingSidewaysVelocity(mod);

        return result;
    }

    private void handleForwardVelocity(AltoClef mod, boolean newForwardTarget) {
        if (mod.getPlayer().isOnGround() || _movingTorwards == null || WorldHelper.inRangeXZ(mod.getPlayer(), _movingTorwards, 0.05f)) {
            moveForwardBack(mod, 0);
            return;
        }
        Rotation look = LookHelper.getLookRotation();
        look = new Rotation(look.getYaw(), 0);
        Vec3d forwardFacing = LookHelper.toVec3d(look).multiply(1, 0, 1).normalize();
        Vec3d delta = WorldHelper.toVec3d(_movingTorwards).subtract(mod.getPlayer().getPos()).multiply(1, 0, 1);
        Vec3d velocity = mod.getPlayer().getVelocity().multiply(1, 0, 1);
        Vec3d pd = delta.subtract(velocity.multiply(3f));
        double forwardStrength = pd.dotProduct(forwardFacing);
        if (newForwardTarget) {
            LookHelper.lookAt(mod, _movingTorwards);
        }
        Debug.logInternal("F:" + forwardStrength);
        moveForwardBack(mod, (int) Math.signum(forwardStrength));
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritone().getPathingBehavior().forceCancel();
        _placedPos = null;
        // hold shift while falling.
        // Look down at first, might help
        mod.getPlayer().setPitch(90);
    }

    /**
     * We will land in this block, handle our jump.
     * <p>
     * Twisted vines require we press space ONLY when we're inside the vines
     */
    private void handleJumpForLand(AltoClef mod, BlockPos willLandOn) {
        BlockPos willLandIn = WorldHelper.isSolid(mod, willLandOn) ? willLandOn.up() : willLandOn;
        BlockState s = mod.getWorld().getBlockState(willLandIn);
        if (s.getBlock() == Blocks.LAVA) {
            // ALWAYS hold jump for lava
            mod.getInputControls().hold(Input.JUMP);
            return;
        }
        Box blockBounds;
        try {
            blockBounds = s.getCollisionShape(mod.getWorld(), willLandIn).getBoundingBox();
        } catch (UnsupportedOperationException ex) {
            blockBounds = Box.of(WorldHelper.toVec3d(willLandIn), 1, 1, 1);
        }
        boolean inside = mod.getPlayer().getBoundingBox().intersects(blockBounds);
        if (inside)
            mod.getInputControls().hold(Input.JUMP);
        else
            mod.getInputControls().release(Input.JUMP);
    }

    private Optional<BlockPos> getBlockWeWillLandOn(AltoClef mod) {
        Vec3d velCheck = mod.getPlayer().getVelocity();
        Box b = mod.getPlayer().getBoundingBox().offset(velCheck);
        Vec3d c = b.getCenter();
        Vec3d[] coords = new Vec3d[]{
                c,
                new Vec3d(b.minX, c.y, b.minZ),
                new Vec3d(b.maxX, c.y, b.minZ),
                new Vec3d(b.minX, c.y, b.maxZ),
                new Vec3d(b.maxX, c.y, b.maxZ),
        };
        BlockHitResult result = null;
        double bestSqDist = Double.POSITIVE_INFINITY;
        for (Vec3d rayOrigin : coords) {
            RaycastContext rctx = castDown(rayOrigin);
            BlockHitResult hit = mod.getWorld().raycast(rctx);
            if (hit.getType() == HitResult.Type.BLOCK) {
                double curDis = hit.getPos().squaredDistanceTo(rayOrigin);
                if (curDis < bestSqDist) {
                    result = hit;
                    bestSqDist = curDis;
                }
            }
        }

        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        return Optional.ofNullable(result.getBlockPos());
    }

    /**
     * While falling to a target, we look towards the center and press forwards.
     * However, if we change our direction we end up moving sideways with respect to our look direction, which
     * often messes us up.
     * <p>
     * This will nudge the bot left/right so we're no longer "slipping" to the side.
     */
    private void handleCancellingSidewaysVelocity(AltoClef mod) {
        if (_movingTorwards == null) {
            moveLeftRight(mod, 0);
            return;
        }
        // Cancel our left/right velocity with respect to block
        Vec3d velocity = mod.getPlayer().getVelocity();
        Vec3d deltaTarget = WorldHelper.toVec3d(_movingTorwards).subtract(mod.getPlayer().getPos());
        // "right" velocity relative to delta
        Rotation look = LookHelper.getLookRotation();
        Vec3d forwardFacing = LookHelper.toVec3d(look).multiply(1, 0, 1).normalize();
        Vec3d rightVelocity = MathsHelper.projectOntoPlane(velocity, forwardFacing).multiply(1, 0, 1); // Flatten
        // Also consider how much further to the right we should move
        Vec3d rightDelta = MathsHelper.projectOntoPlane(deltaTarget, forwardFacing).multiply(1, 0, 1);
        // Do a little PD loop
        Vec3d pd = rightDelta.subtract(rightVelocity.multiply(2));
        // We're traveling too fast sideways
        Vec3d faceRight = forwardFacing.crossProduct(new Vec3d(0, 1, 0));
        boolean moveRight = pd.dotProduct(faceRight) > 0;
        if (moveRight) {
            moveLeftRight(mod, 1);
        } else {
            moveLeftRight(mod, -1);
        }
    }

    private Optional<BlockPos> getBestConeClutchBlock(AltoClef mod, BlockPos oldClutchTarget) {
        double pitchHalfWidth = _config.epicClutchConePitchAngle;
        double dpitchStart = pitchHalfWidth / _config.epicClutchConePitchResolution;

        // Our priority is:
        // - Safe to land (water)
        // - Highest block
        // IF WE HAVE MLG
        // - Closer to player

        ConeClutchContext cctx = new ConeClutchContext(mod);

        // Always check our previous best so we don't lose it
        if (oldClutchTarget != null)
            cctx.checkBlock(mod, oldClutchTarget);

        // Perform cone
        for (double pitch = dpitchStart; pitch <= pitchHalfWidth; pitch += pitchHalfWidth / _config.epicClutchConePitchResolution) {
            double pitchProgress = (pitch - dpitchStart) / (pitchHalfWidth - dpitchStart);
            double yawResolution = _config.epicClutchConeYawDivisionStart + pitchProgress * (_config.epicClutchConeYawDivisionEnd - _config.epicClutchConeYawDivisionStart); // lerp from start to end
            for (double yaw = 0; yaw < 360; yaw += 360.0 / yawResolution) {
                RaycastContext rctx = castCone(yaw, pitch);
                cctx.checkRay(mod, rctx);
            }
        }

        // Perform NEARBY sweep
        //int nearbySweepSize =
        Vec3d center = mod.getPlayer().getPos();
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dz = -2; dz <= 2; ++dz) {
                RaycastContext ctx = castDown(center.add(dx, 0, dz));
                cctx.checkRay(mod, ctx);
            }
        }

        return Optional.ofNullable(cctx.bestBlock);
    }

    private RaycastContext castDown(Vec3d origin) {
        Entity player = MinecraftClient.getInstance().player;
        assert player != null;
        return new RaycastContext(origin, origin.add(0, -1 * _config.castDownDistance, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player);
    }

    private RaycastContext castCone(double yaw, double pitch) {
        Entity player = MinecraftClient.getInstance().player;
        assert player != null;
        Vec3d origin = player.getPos();
        double dy = _config.epicClutchConeCastHeight;
        double dH = dy * Math.sin(Math.toRadians(pitch)); // horizontal distance
        double yawRad = Math.toRadians(yaw);
        double dx = dH * Math.cos(yawRad);
        double dz = dH * Math.sin(yawRad);
        Vec3d end = origin.add(dx, -1 * dy, dz);
        return new RaycastContext(origin, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getPathingBehavior().forceCancel();
        _movingTorwards = null;
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
        moveLeftRight(mod, 0);
        moveForwardBack(mod, 0);
        mod.getInputControls().release(Input.SPRINT);
        mod.getInputControls().release(Input.JUMP);
    }

    private boolean hasClutchItem(AltoClef mod) {
        if (!mod.getWorld().getDimension().ultrawarm() && mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
            return true;
        }
        return _config.clutchItems.stream().anyMatch(item -> mod.getItemStorage().hasItem(item));
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getPlayer().isSwimming() || mod.getPlayer().isTouchingWater() || mod.getPlayer().isOnGround() || mod.getPlayer().isClimbing();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof MLGBucketTask;
    }

    @Override
    protected String toDebugString() {
        String result = "Epic gaemer moment";
        if (_movingTorwards != null) {
            result += " (CLUTCH AT: " + _movingTorwards + ")";
        }
        return result;
    }

    public BlockPos getWaterPlacedPos() {
        return _placedPos;
    }

    private static class MLGClutchConfig {
        public double castDownDistance = 40;
        public double averageHorizontalMovementSpeedPerTick = 0.25; // How "far" the player moves horizontally per tick. Set too low and the bot will ignore viable clutches. Set too high and the bot will go for clutches it can't reach.
        public double epicClutchConeCastHeight = 40; // How high the "epic clutch" ray cone is
        public double epicClutchConePitchAngle = 25; // How wide (degrees) the "epic clutch" ray cone is
        public int epicClutchConePitchResolution = 8; // How many divisions in each direction the cone's pitch has
        public int epicClutchConeYawDivisionStart = 6; // How many divisions to start the cone clutch at in the center
        public int epicClutchConeYawDivisionEnd = 20; // How many divisions to move the cone clutch at torwars the end
        public int preferLavaWhenFallDropsHealthBelowThreshold = 3; // If a fall results in our player's health going below this value, consider it deadly.
        public int lavaLevelOrGreaterWillCancelFallDamage = 5; // Lava at this level will cancel our fall damage if we hold space.
        @JsonSerialize(using = ItemSerializer.class)
        @JsonDeserialize(using = ItemDeserializer.class)
        public List<Item> clutchItems = List.of(Items.HAY_BLOCK, Items.TWISTING_VINES);
    }

    class ConeClutchContext {
        private final boolean hasClutchItem;
        public BlockPos bestBlock = null;
        private double highestY = Double.NEGATIVE_INFINITY;
        private double closestXZ = Double.POSITIVE_INFINITY;
        private boolean bestBlockIsSafe = false;
        private boolean bestBlockIsDeadlyFall = false;
        private boolean bestBlockIsLava = false;

        public ConeClutchContext(AltoClef mod) {
            hasClutchItem = hasClutchItem(mod);
        }

        public void checkBlock(AltoClef mod, BlockPos check) {
            // Already checked
            if (Objects.equals(bestBlock, check))
                return;
            if (WorldHelper.isAir(mod, check)) {
                Debug.logMessage("(MLG Air block checked for landing, the block broke. We'll try another): " + check);
                return;
            }
            boolean lava = isLava(check);
            boolean lavaWillProtect = lava && lavaWillProtect(check);
            boolean water = isWater(check);
            boolean isDeadlyFall = !hasClutchItem && isFallDeadly(check);
            // Prioritize safe blocks ALWAYS
            if (bestBlockIsSafe && !water)
                return;
            double height = check.getY();
            double distSqXZ = WorldHelper.distanceXZSquared(WorldHelper.toVec3d(check), mod.getPlayer().getPos());
            boolean highestSoFar = height > highestY;
            boolean closestSoFar = distSqXZ < closestXZ;
            // We found a new contender
            if (
                    bestBlock == null || // No target was found.
                            (water && !bestBlockIsSafe) || // We ALWAYS land in water if we can
                            (lava && lavaWillProtect && bestBlockIsDeadlyFall && !hasClutchItem) || // Land in lava if our best alternative is death by fall damage
                            (!lava && !isDeadlyFall && ((closestSoFar && hasClutchItem) && highestSoFar || bestBlockIsLava)) // If it's not lava and is not deadly, land on it if it's higher than before OR if our best alternative is lava
            ) {
                if (canTravelToInAir((lava || water) ? check.down() : check)) {
                    if (highestSoFar) {
                        highestY = height;
                    }
                    if (closestSoFar) {
                        closestXZ = distSqXZ;
                    }
                    bestBlockIsSafe = water;
                    bestBlockIsDeadlyFall = isDeadlyFall;
                    bestBlockIsLava = lava;
                    bestBlock = check;
                }
            }
        }

        public void checkRay(AltoClef mod, RaycastContext rctx) {
            BlockHitResult hit = mod.getWorld().raycast(rctx);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos check = hit.getBlockPos();
                // For now, REQUIRE we land on this
                if (hit.getSide().getOffsetY() <= 0)
                    return;
                checkBlock(mod, check);
            }
        }
    }

}
