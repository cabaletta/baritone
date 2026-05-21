package baritone.combat;

import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.IPlayerContext;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import baritone.api.utils.input.Input;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CreepeTactics {

    private static final float FUSE_TICKS          = 30f;
    private static final float HIT_WINDOW_START    = 19f / FUSE_TICKS;
    private static final float HIT_WINDOW_END      = 26f / FUSE_TICKS;
    private static final float FAST_FUSE_DELTA     = 2f / FUSE_TICKS;
    private static final int   POST_EXPLOSION_TICKS = 15;

    private final IPlayerContext ctx;

    private final Map<Integer, Float>   fuseProgress  = new HashMap<>();
    private final Map<Integer, Boolean> hitFired      = new HashMap<>();
    private final Map<Integer, Vec3>    lastKnownPos  = new HashMap<>();

    private Vec3  escapeFromPos   = null;
    private int   escapeTicksLeft = 0;
    private int   blockPlaceCooldown = 0;

    public CreepeTactics(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public boolean hasPendingEscape() {
        return escapeTicksLeft > 0;
    }

    public PathingCommand tick(InputOverrideHandler input, AwarenessContext awarenessCtx) {
        if (blockPlaceCooldown > 0) blockPlaceCooldown--;

        if (escapeTicksLeft > 0) {
            escapeTicksLeft--;
            if (escapeFromPos != null) sprintAwayFromPos(input, escapeFromPos);
            return pause();
        }

        Set<Integer> activeFusingIds = new HashSet<>();
        Creeper fusing  = null;
        int     fusingId = -1;
        float   maxProg  = 0f;

        for (ThreatEntry t : awarenessCtx.getThreats()) {
            if (!(t.tracked.entity instanceof Creeper)) continue;
            Creeper c = (Creeper) t.tracked.entity;
            int id = c.getId();

            if (c.getSwellDir() > 0) {
                activeFusingIds.add(id);
                lastKnownPos.put(id, c.position());

                float prev = fuseProgress.getOrDefault(id, 0f);
                float next = prev + (1f / FUSE_TICKS);
                fuseProgress.put(id, next);

                if (next > maxProg) {
                    maxProg  = next;
                    fusing   = c;
                    fusingId = id;
                }
            }
        }

        for (Map.Entry<Integer, Float> entry : new HashMap<>(fuseProgress).entrySet()) {
            int   id       = entry.getKey();
            float progress = entry.getValue();
            if (!activeFusingIds.contains(id) && progress >= HIT_WINDOW_END) {
                Vec3 pos = lastKnownPos.get(id);
                if (pos != null) {
                    escapeFromPos   = pos;
                    escapeTicksLeft = POST_EXPLOSION_TICKS;
                }
            }
        }

        fuseProgress.keySet().retainAll(activeFusingIds);
        hitFired.keySet().retainAll(activeFusingIds);
        lastKnownPos.keySet().retainAll(activeFusingIds);

        if (fusing == null) return null;

        float   prevProg = maxProg - (1f / FUSE_TICKS);
        boolean fastFuse = (maxProg - prevProg) > FAST_FUSE_DELTA && prevProg > 0.05f;

        if (fastFuse || maxProg >= HIT_WINDOW_END) {
            sprintAwayFrom(input, fusing);
            tryPlaceShieldBlock(fusing);
            return pause();
        }

        if (maxProg >= HIT_WINDOW_START) {
            if (!hitFired.getOrDefault(fusingId, false)) {
                aimAt(fusing);
                Minecraft mc = ctx.minecraft();
                if (mc.gameMode != null) {
                    mc.gameMode.attack(ctx.player(), fusing);
                }
                hitFired.put(fusingId, true);
            }
            sprintAwayFrom(input, fusing);
            return pause();
        }

        return null;
    }

    private void sprintAwayFrom(InputOverrideHandler input, Creeper creeper) {
        sprintAwayFromPos(input, creeper.position());
    }

    private void sprintAwayFromPos(InputOverrideHandler input, Vec3 dangerPos) {
        Player player = ctx.player();
        if (player == null) return;
        Vec3 away = player.position().subtract(dangerPos);
        if (away.lengthSqr() < 0.001) away = new Vec3(1, 0, 0);
        away = new Vec3(away.x, 0, away.z).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-away.x, away.z));
        player.setYRot(yaw);  player.yRotO = yaw;
        player.setXRot(10f);  player.xRotO = 10f;
        input.setInputForceState(Input.SPRINT,        true);
        input.setInputForceState(Input.MOVE_FORWARD,  true);
    }

    private void tryPlaceShieldBlock(Creeper creeper) {
        if (blockPlaceCooldown > 0) return;
        Player player = ctx.player();
        if (player == null) return;

        Vec3 toCreeper = creeper.position().subtract(player.position());
        if (toCreeper.lengthSqr() < 0.001) return;
        Vec3 dir = new Vec3(toCreeper.x, 0, toCreeper.z).normalize();

        BlockPos place = new BlockPos(
            (int) Math.floor(player.getX() + dir.x),
            (int) Math.floor(player.getY()),
            (int) Math.floor(player.getZ() + dir.z)
        );

        if (!ctx.world().getBlockState(place).isAir()) return;

        BlockPos support = place.below();
        if (ctx.world().getBlockState(support).isAir()) return;

        int blockSlot = findBlockSlot(player);
        if (blockSlot < 0) return;

        int prevSlot = InventoryHelper.getSelected(player);
        InventoryHelper.setSelected(player, blockSlot);

        Vec3 hitVec = Vec3.atCenterOf(support).add(0, 0.5, 0);
        ctx.playerController().processRightClickBlock(
            ctx.player(),
            ctx.world(),
            InteractionHand.MAIN_HAND,
            new BlockHitResult(hitVec, Direction.UP, support, false)
        );

        InventoryHelper.setSelected(player, prevSlot);
        blockPlaceCooldown = 5;
    }

    private int findBlockSlot(Player player) {
        if (isPlaceable(player.getInventory().getItem(InventoryLayout.SLOT_BLOCKS))) {
            return InventoryLayout.SLOT_BLOCKS;
        }
        for (int i = 0; i < 9; i++) {
            if (isPlaceable(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private boolean isPlaceable(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
    }

    private void aimAt(net.minecraft.world.entity.Entity target) {
        Player player = ctx.player();
        if (player == null) return;
        Vec3 eye  = player.getEyePosition(1f);
        Vec3 tEye = target.getEyePosition(1f);
        Vec3 dir  = tEye.subtract(eye).normalize();
        float yaw   = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, dir.y))));
        player.setYRot(yaw);   player.yRotO = yaw;
        player.setXRot(pitch); player.xRotO = pitch;
    }

    private static PathingCommand pause() {
        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
    }
}
