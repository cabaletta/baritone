package baritone.combat.watchdog;

import baritone.combat.InventoryHelper;
import baritone.combat.InventoryLayout;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/**
 * Zero-delay packet-level evasion maneuvers for each MaceCpvpSituation.
 *
 * All methods execute within the same tick they are called — slot switching
 * uses raw ServerboundSetCarriedItemPacket rather than GUI-side slot selection,
 * rotation uses direct yaw/pitch mutation + ServerboundMovePlayerPacket.Rot,
 * and item use goes through MultiPlayerGameMode.useItem() which handles the
 * sequence number and fires the right-click packet internally.
 */
public final class EvasionController {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ── STATE 1: AIRBORNE_COMBO_LOCK — Snap-Drop Pearl ───────────────────────

    /**
     * While explosion-launched: aim straight down and pearl, teleporting the
     * bot to the ground directly below and breaking the combo trajectory.
     * Fallback: wind charge fired slightly downward to redirect knockback.
     */
    void executeState1(Minecraft mc, WatchdogSnapshot snap) {
        LOGGER.warn("[WATCHDOG OVERRIDE: AIRBORNE_COMBO_LOCK] Snap-drop pearl");
        Player player = mc.player;

        int pearlSlot = InventoryLayout.findPearlSlot(player);
        if (pearlSlot >= 0) {
            switchSlot(mc, pearlSlot);
            aimAndUse(mc, player.getYRot(), 90f);   // pitch 90 = dead down
        } else {
            int windSlot = findItem(player, Items.WIND_CHARGE);
            if (windSlot >= 0) {
                switchSlot(mc, windSlot);
                // 75° downward — explosion pushes bot into ground, absorbing velocity
                aimAndUse(mc, player.getYRot(), 75f);
            } else {
                LOGGER.warn("[WATCHDOG STATE1] No pearl or wind charge — no evasion possible");
            }
        }
    }

    // ── STATE 2: BOXED_CAGE_TRAP — Corner-Seam Pearl Clip ───────────────────

    /**
     * Locate the least-blocked cardinal direction, snap aim to the corner seam
     * between that face and its neighbour (45° offset), and fire an ender pearl
     * to clip through the obsidian geometry.
     * Fallback: switch to Netherite Pickaxe and begin breaking the nearest
     * non-obsidian/non-bedrock adjacent block.
     */
    void executeState2(Minecraft mc, WatchdogSnapshot snap) {
        LOGGER.warn("[WATCHDOG OVERRIDE: BOXED_CAGE_TRAP] Corner-seam pearl clip");
        Player player = mc.player;

        int pearlSlot = InventoryLayout.findPearlSlot(player);
        if (pearlSlot >= 0) {
            Direction escape   = findBestEscapeDirection(mc, player);
            float escapeYaw    = dirToYaw(escape);
            // +45° rotates aim to the corner seam for geometry-clip probability
            float seamYaw      = escapeYaw + 45f;
            switchSlot(mc, pearlSlot);
            aimAndUse(mc, seamYaw, -10f);   // slight upward arc for range
        } else {
            int pickSlot = findItem(player, Items.NETHERITE_PICKAXE);
            if (pickSlot >= 0 && mc.gameMode != null) {
                switchSlot(mc, pickSlot);
                BlockPos breakTarget = findWeakestAdjacentBlock(mc, player);
                if (breakTarget != null) {
                    mc.gameMode.startDestroyBlock(breakTarget, Direction.NORTH);
                    LOGGER.warn("[WATCHDOG STATE2] Pickaxe-breaking block at {}", breakTarget);
                }
            } else {
                LOGGER.warn("[WATCHDOG STATE2] No pearl or pickaxe — standing by");
            }
        }
    }

    // ── STATE 3: CEILING_MACE_DROP — Mace-Escape Lateral Pearl ──────────────

    /**
     * Drop the shield (switch off it) to restore full movement speed, then fire
     * a pearl 180°+45° away from the attacker's descent vector to clear the
     * mace landing zone before D-tap lands.
     * Fallback: wind charge aimed at attacker's torso to deflect mid-air momentum.
     */
    void executeState3(Minecraft mc, WatchdogSnapshot snap) {
        LOGGER.warn("[WATCHDOG OVERRIDE: CEILING_MACE_DROP] Mace-escape lateral pearl");
        Player player = mc.player;

        // Drop shield: switch to sword so the block button no longer holds shield
        if (player.isBlocking()) {
            int swordSlot = InventoryLayout.findSwordSlot(player);
            if (swordSlot >= 0) switchSlot(mc, swordSlot);
        }

        int pearlSlot = InventoryLayout.findPearlSlot(player);
        if (pearlSlot >= 0 && snap.primaryTarget != null) {
            Vec3 toTarget  = snap.primaryTarget.position().subtract(player.position());
            float toYaw    = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
            // Opposite direction + 45° lateral offset to clear the landing zone
            float escapeYaw = toYaw + 225f;
            switchSlot(mc, pearlSlot);
            aimAndUse(mc, escapeYaw, -20f);  // slight upward arc for distance
        } else {
            int windSlot = findItem(player, Items.WIND_CHARGE);
            if (windSlot >= 0 && snap.primaryTarget != null) {
                switchSlot(mc, windSlot);
                aimAtEntity(mc, snap.primaryTarget);
                mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            } else {
                LOGGER.warn("[WATCHDOG STATE3] No pearl or wind charge available");
            }
        }
    }

    // ── STATE 4: LOW_GROUND_EXPOSURE — Sprint-Over + Respawn Anchor Trap ────

    /**
     * Rotate to face the enemy directly, apply sprint velocity and a jump,
     * then place a Respawn Anchor directly behind them at ground level,
     * fuel it with Glowstone, and right-click to detonate (Overworld = instant
     * explosion). Forces them off the low-ground crystal tier.
     *
     * NOTE: The sprint-jump here manipulates velocity and sprinting flag directly.
     * Full input-system integration would require Baritone's InputOverrideHandler.
     */
    void executeState4(Minecraft mc, WatchdogSnapshot snap) {
        LOGGER.warn("[WATCHDOG OVERRIDE: LOW_GROUND_EXPOSURE] Sprint-over + anchor trap");
        Player player = mc.player;
        if (snap.primaryTarget == null || mc.gameMode == null) return;

        // 1. Rotate toward target and sprint-jump
        Vec3 toTarget = snap.primaryTarget.position().subtract(player.position());
        float chargeYaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        player.setYRot(chargeYaw);    player.yRotO = chargeYaw;
        player.setXRot(-15f);         player.xRotO = -15f;
        sendPacket(mc, new ServerboundMovePlayerPacket.Rot(chargeYaw, -15f, player.onGround(), false));

        player.setSprinting(true);
        if (player.onGround()) {
            // Inject jump velocity directly (0.42 = vanilla jump height)
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, 0.42, v.z);
        }

        // 2. Anchor placement behind target
        int anchorSlot = findItem(player, Items.RESPAWN_ANCHOR);
        int glowSlot   = findItem(player, Items.GLOWSTONE);
        if (anchorSlot < 0 || glowSlot < 0) {
            LOGGER.warn("[WATCHDOG STATE4] Missing anchor({}) or glowstone({})", anchorSlot, glowSlot);
            return;
        }

        // Position: 1.5 blocks behind target along the player→target axis
        Vec3 norm       = new Vec3(toTarget.x, 0, toTarget.z).normalize();
        Vec3 behindPos  = snap.primaryTarget.position().subtract(norm.scale(1.5));
        BlockPos anchor = new BlockPos(
            (int) Math.floor(behindPos.x),
            (int) Math.floor(snap.primaryTarget.getY()),
            (int) Math.floor(behindPos.z));
        BlockPos ground = anchor.below();

        if (!mc.level.getBlockState(anchor).isAir()) return;    // already occupied
        if (mc.level.getBlockState(ground).isAir())  return;    // no surface

        Vec3 groundHit = Vec3.atCenterOf(ground).add(0, 0.5, 0);
        Vec3 anchorHit = Vec3.atCenterOf(anchor);

        // Place anchor on top of ground block
        switchSlot(mc, anchorSlot);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(groundHit, Direction.UP, ground, false));

        // Fuel anchor with glowstone
        switchSlot(mc, glowSlot);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(anchorHit, Direction.UP, anchor, false));

        // Detonate (third right-click on a charged anchor in Overworld = explosion)
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(anchorHit, Direction.UP, anchor, false));
    }

    // ── STATE 5: CRYSTAL_CROSS_FIRE — Pop + Pearl Away ───────────────────────

    /**
     * Immediately attack the nearest end crystal to destroy it before the enemy
     * pops it. If the crystal is inside 3 m (nearly certain to one-shot), also
     * fire a pearl in the opposite direction to clear the blast radius.
     */
    void executeState5(Minecraft mc, WatchdogSnapshot snap) {
        LOGGER.warn("[WATCHDOG OVERRIDE: CRYSTAL_CROSS_FIRE] Popping crystal at {:.1f}m",
            snap.nearestCrystalDist);
        Player player = mc.player;
        if (mc.gameMode == null) return;

        // Attack all crystals within 8 m this tick — prioritise nearest
        if (snap.nearestCrystal != null) {
            mc.gameMode.attack(player, snap.nearestCrystal);

            // Also sweep additional crystals in immediate range
            AABB box = new AABB(player.blockPosition()).inflate(6);
            mc.level.getEntities(player, box, e -> e.getType() == net.minecraft.world.entity.EntityType.END_CRYSTAL)
                .stream()
                .filter(e -> e != snap.nearestCrystal)
                .limit(2)
                .forEach(e -> mc.gameMode.attack(player, e));
        }

        // Pearl away when dangerously close
        if (snap.nearestCrystalDist >= 0 && snap.nearestCrystalDist < 3.5f) {
            int pearlSlot = InventoryLayout.findPearlSlot(player);
            if (pearlSlot >= 0 && snap.nearestCrystal != null) {
                Vec3 toCrystal = snap.nearestCrystal.position().subtract(player.position());
                // Flee in the exact opposite direction
                float awayYaw = (float) Math.toDegrees(Math.atan2(toCrystal.x, -toCrystal.z));
                switchSlot(mc, pearlSlot);
                aimAndUse(mc, awayYaw, -5f);
            }
        }
    }

    // ── STATE 6: PROGRESSIVE_SURROUND — Pre-Emptive Pearl Escape ────────────

    /**
     * Obsidian cage is forming (face count increasing). Pearl through the open
     * face before it is sealed — one tick of reaction time is enough.
     * Fallback: sprint toward the open face at full speed.
     */
    void executeState6(Minecraft mc, WatchdogSnapshot snap) {
        LOGGER.warn("[WATCHDOG OVERRIDE: PROGRESSIVE_SURROUND] Pre-emptive surround escape");
        Player player = mc.player;

        int pearlSlot = InventoryLayout.findPearlSlot(player);
        Direction escape = findBestEscapeDirection(mc, player);

        if (pearlSlot >= 0) {
            float yaw = dirToYaw(escape);
            switchSlot(mc, pearlSlot);
            aimAndUse(mc, yaw, -10f);
        } else {
            // No pearl: sprint hard toward the gap
            float yaw = dirToYaw(escape);
            player.setYRot(yaw);  player.yRotO = yaw;
            player.setSprinting(true);
            sendPacket(mc, new ServerboundMovePlayerPacket.Rot(yaw, 0f, player.onGround(), false));
            LOGGER.warn("[WATCHDOG STATE6] No pearl — sprinting toward {}", escape);
        }
    }

    // ── Package-visible helpers (also used by WatchdogEngine) ────────────────

    /**
     * Finds the first hotbar slot (0-8) holding the given item. Returns -1 if
     * not found. Searches at the canonical layout slot first for speed.
     */
    static int findItem(Player player, Item item) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Atomic slot switch: update local Inventory field + send raw packet same tick. */
    private void switchSlot(Minecraft mc, int slot) {
        InventoryHelper.setSelected(mc.player, slot);
        sendPacket(mc, new ServerboundSetCarriedItemPacket(slot));
    }

    /**
     * Set player look angles, send a Rot packet, then right-click the held item —
     * all in the same tick, zero GUI delay.
     */
    private void aimAndUse(Minecraft mc, float yaw, float pitch) {
        if (mc.gameMode == null) return;
        Player p = mc.player;
        p.setYRot(yaw);    p.yRotO = yaw;
        p.setXRot(pitch);  p.xRotO = pitch;
        sendPacket(mc, new ServerboundMovePlayerPacket.Rot(yaw, pitch, p.onGround(), false));
        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
    }

    /** Aim at an entity's eye position. */
    private void aimAtEntity(Minecraft mc, net.minecraft.world.entity.Entity target) {
        Player p  = mc.player;
        Vec3 eye  = p.getEyePosition(1f);
        Vec3 tEye = target.getEyePosition(1f);
        Vec3 dir  = tEye.subtract(eye).normalize();
        float yaw   = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, dir.y))));
        p.setYRot(yaw);    p.yRotO = yaw;
        p.setXRot(pitch);  p.xRotO = pitch;
        sendPacket(mc, new ServerboundMovePlayerPacket.Rot(yaw, pitch, p.onGround(), false));
    }

    /** Null-safe packet send through Connection. */
    private void sendPacket(Minecraft mc, net.minecraft.network.protocol.Packet<?> pkt) {
        var conn = mc.getConnection();
        if (conn != null) conn.getConnection().send(pkt);
    }

    /**
     * Returns the cardinal direction with the most air space at foot and head
     * height — the best direction to pearl toward when escaping a cage.
     */
    Direction findBestEscapeDirection(Minecraft mc, Player player) {
        Direction best    = Direction.NORTH;
        int minBlocked    = Integer.MAX_VALUE;
        BlockPos base     = player.blockPosition();

        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH,
                                           Direction.EAST,  Direction.WEST}) {
            BlockPos adj = base.relative(d);
            int blocked  = 0;
            for (int dy = 0; dy <= 1; dy++) {
                if (!mc.level.getBlockState(adj.above(dy)).isAir()) blocked++;
            }
            if (blocked < minBlocked) { minBlocked = blocked; best = d; }
        }
        return best;
    }

    /**
     * Returns the nearest non-obsidian, non-bedrock adjacent block that can be
     * broken as a cage-escape route, or null if all faces are hardened.
     */
    private BlockPos findWeakestAdjacentBlock(Minecraft mc, Player player) {
        BlockPos base = player.blockPosition();
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH,
                                           Direction.EAST,  Direction.WEST}) {
            BlockPos adj = base.relative(d);
            BlockState bs = mc.level.getBlockState(adj);
            if (!bs.isAir() && !bs.is(Blocks.OBSIDIAN) && !bs.is(Blocks.BEDROCK)
                    && !bs.is(Blocks.ENDER_CHEST)) {
                return adj;
            }
        }
        return null;
    }

    private static float dirToYaw(Direction dir) {
        switch (dir) {
            case SOUTH: return   0f;
            case WEST:  return  90f;
            case NORTH: return 180f;
            case EAST:  return -90f;
            default:    return   0f;
        }
    }
}
