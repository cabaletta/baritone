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
import java.util.Map;

/**
 * Handles fusing Creepers independently of the main combat loop.
 *
 * Fuse mechanics (standard, 30 ticks):
 *   Tick  0–17  (swell 0.00–0.600)  — fusing, approach and pre-position
 *   Tick 18–25  (swell 0.600–0.833) — HIT WINDOW: aim and knock into any mob cluster,
 *                                       then sprint directly away from the creeper
 *   Tick 26+    (swell > 0.867)     — DANGER: sprint directly away + place a block
 *                                       between player and explosion
 *
 * Hit window moved 2 ticks earlier (~100 ms) vs the original to give more time to
 * escape after the knockback sends the creeper away.
 *
 * Escape strategy in danger phase:
 *   Sprint away from the creeper to maximize distance, and place a solid block
 *   (from SLOT_BLOCKS in the hotbar) between the player and the creeper to absorb
 *   some of the blast.
 */
public final class CreepeTactics {

    private static final float FUSE_TICKS       = 30f;
    private static final float HIT_WINDOW_START = 18f / FUSE_TICKS; // ~100 ms earlier: 0.600
    private static final float HIT_WINDOW_END   = 26f / FUSE_TICKS; // 0.867
    private static final float FAST_FUSE_DELTA  = 2f / FUSE_TICKS;

    private final IPlayerContext ctx;

    private final Map<Integer, Float>   fuseProgress    = new HashMap<>();
    private final Map<Integer, Boolean> hitFired        = new HashMap<>();
    private int blockPlaceCooldown = 0;

    public CreepeTactics(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public PathingCommand tick(InputOverrideHandler input, AwarenessContext awarenessCtx) {
        if (blockPlaceCooldown > 0) blockPlaceCooldown--;

        Creeper fusing  = null;
        int     fusingId = -1;
        float   maxProg  = 0f;

        for (ThreatEntry t : awarenessCtx.getThreats()) {
            if (!(t.tracked.entity instanceof Creeper)) continue;
            Creeper c = (Creeper) t.tracked.entity;
            int id = c.getId();

            if (c.getSwellDir() > 0) {
                float prev = fuseProgress.getOrDefault(id, 0f);
                float next = prev + (1f / FUSE_TICKS);
                fuseProgress.put(id, next);
                if (next > maxProg) {
                    maxProg  = next;
                    fusing   = c;
                    fusingId = id;
                }
            } else {
                fuseProgress.remove(id);
                hitFired.remove(id);
            }
        }

        if (fusing == null) return null;

        float prevProg   = fuseProgress.getOrDefault(fusingId, 0f) - (1f / FUSE_TICKS);
        boolean fastFuse = (maxProg - prevProg) > FAST_FUSE_DELTA && prevProg > 0.05f;

        if (fastFuse || maxProg >= HIT_WINDOW_END) {
            // DANGER — sprint directly away + try to place a block to absorb blast
            sprintAwayFrom(input, fusing);
            tryPlaceShieldBlock(fusing);
            return pause();
        }

        if (maxProg >= HIT_WINDOW_START) {
            // HIT WINDOW — one aimed hit, then sprint away from the (now-knocked-back) creeper
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

        // Pre-window: let main combat handle positioning (CombatEngine keeps 3.5-block distance)
        return null;
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────

    /** Sprint directly away from the creeper to maximise distance before detonation. */
    private void sprintAwayFrom(InputOverrideHandler input, Creeper creeper) {
        Player player = ctx.player();
        if (player == null) return;
        Vec3 away = player.position().subtract(creeper.position());
        if (away.lengthSqr() < 0.001) away = new Vec3(1, 0, 0);
        away = new Vec3(away.x, 0, away.z).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-away.x, away.z));
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setXRot(10f);  // look slightly down — natural running posture
        player.xRotO = 10f;
        input.setInputForceState(Input.SPRINT, true);
        input.setInputForceState(Input.MOVE_FORWARD, true);
    }

    /**
     * Places a solid block between the player and the creeper to absorb blast damage.
     * Targets the top face of the ground block one step toward the creeper from the
     * player's feet. processRightClickBlock uses the explicit BlockHitResult so the
     * player's look direction does not need to match the placement direction.
     */
    private void tryPlaceShieldBlock(Creeper creeper) {
        if (blockPlaceCooldown > 0) return;
        Player player = ctx.player();
        if (player == null) return;

        Vec3 toCreeper = creeper.position().subtract(player.position());
        if (toCreeper.lengthSqr() < 0.001) return;
        Vec3 dir = new Vec3(toCreeper.x, 0, toCreeper.z).normalize();

        // One block toward the creeper at foot level
        BlockPos place = new BlockPos(
            (int) Math.floor(player.getX() + dir.x),
            (int) Math.floor(player.getY()),
            (int) Math.floor(player.getZ() + dir.z)
        );

        if (!ctx.world().getBlockState(place).isAir()) return;

        BlockPos support = place.below();
        if (ctx.world().getBlockState(support).isAir()) return; // nothing to place on

        int blockSlot = findBlockSlot(player);
        if (blockSlot < 0) return;

        int prevSlot = player.getInventory().selected;
        player.getInventory().selected = blockSlot;

        Vec3 hitVec = Vec3.atCenterOf(support).add(0, 0.5, 0);
        ctx.playerController().processRightClickBlock(
            ctx.player(),
            ctx.world(),
            InteractionHand.MAIN_HAND,
            new BlockHitResult(hitVec, Direction.UP, support, false)
        );

        player.getInventory().selected = prevSlot;
        blockPlaceCooldown = 5; // don't spam-place every tick
    }

    private int findBlockSlot(Player player) {
        // Check the designated block slot first (slot 6)
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
