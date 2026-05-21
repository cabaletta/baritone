package baritone.combat.watchdog;

import baritone.api.BaritoneAPI;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * CPvP Watchdog — deterministic, zero-delay PvP anomaly detector.
 *
 * Architecture
 * ────────────
 * Every tick (via MixinWatchdog injecting at Minecraft.tick() TAIL):
 *   1. sample()  — capture WatchdogSnapshot from current world state
 *   2. window    — push to a 20-tick rolling deque (= 1 second of history)
 *   3. evaluate()— run all 6 anomaly detectors, return highest-priority hit
 *   4. dispatch()— cancel Baritone pathing + call EvasionController same tick
 *
 * After an evasion fires, a per-state suppression window prevents re-triggering
 * until the maneuver has had time to take effect.
 *
 * Integration
 * ───────────
 * Call WatchdogEngine.init() once from your mod initializer (e.g. ClientModInitializer).
 * The Mixin fires automatically; no additional wiring needed.
 *
 * To track swing misses for DeSyncMissTracker, call:
 *   WatchdogEngine.get().recordSwingMiss()
 * from a Mixin on MultiPlayerGameMode.attack() when the attack produces no
 * EntityHitResult (i.e. the target was not within range on server-side).
 */
public final class WatchdogEngine {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Rolling window size: 20 ticks = 1 second at 20 TPS
    private static final int WINDOW = 20;

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static WatchdogEngine INSTANCE;

    public static void init() {
        if (INSTANCE != null) return;
        INSTANCE = new WatchdogEngine();
        LOGGER.info("[WATCHDOG INITIALIZED] CPvP Watchdog armed — {} anomaly states active",
            MaceCpvpSituation.values().length - 1);
        LOGGER.info("[WATCHDOG INITIALIZED] Meta Kit: Sword/Axe/Pearl/Obsidian/Crystal/Anchor/Mace/gApple/Totem");
    }

    public static WatchdogEngine get() { return INSTANCE; }

    /** Entry point called by MixinWatchdog at the tail of every Minecraft.tick(). */
    public static void onTickEnd(Minecraft mc) {
        if (INSTANCE != null) INSTANCE.tick(mc);
    }

    // ── Instance state ────────────────────────────────────────────────────────
    private final ArrayDeque<WatchdogSnapshot> history  = new ArrayDeque<>(WINDOW + 1);
    private final EvasionController            evasion  = new EvasionController();

    private MaceCpvpSituation active        = MaceCpvpSituation.NONE;
    private int                suppressTicks = 0;

    // DeSyncMissTracker: incremented externally when a swing finds no EntityHitResult
    private int deSyncMissCount = 0;

    // Health from the previous tick for computing damageTaken
    private float prevHealth = 20f;

    private WatchdogEngine() {}

    // ── Tick entry ────────────────────────────────────────────────────────────

    private void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        // 1. Sample
        WatchdogSnapshot snap = sample(mc);
        history.addLast(snap);
        if (history.size() > WINDOW) history.removeFirst();
        prevHealth = snap.health;

        // 2. Suppress cooldown
        if (suppressTicks > 0) {
            suppressTicks--;
            return;
        }

        // 3. Evaluate
        MaceCpvpSituation detected = evaluate(snap);

        if (detected == MaceCpvpSituation.NONE) {
            if (active != MaceCpvpSituation.NONE) {
                LOGGER.debug("[WATCHDOG CLEAR] {} resolved", active);
            }
            active = MaceCpvpSituation.NONE;
            return;
        }

        // 4. Dispatch on first detection or state change
        if (detected != active) {
            active = detected;
            LOGGER.warn("[WATCHDOG OVERRIDE: {}] tick={} hp={} dist={}",
                detected.name(),
                mc.level.getGameTime(),
                String.format("%.1f", snap.health),
                snap.targetDist >= 0 ? String.format("%.1f", snap.targetDist) : "?");
            dispatch(mc, snap, detected);
        }
    }

    // ── State sampler ─────────────────────────────────────────────────────────

    private WatchdogSnapshot sample(Minecraft mc) {
        var p = mc.player;

        float  hp    = p.getHealth();
        float  dmg   = Math.max(0f, prevHealth - hp);
        Vec3   vel   = p.getDeltaMovement();
        double hspd  = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        // ── Primary threat: nearest live non-self player, fall back to mob ──
        LivingEntity target = null;
        float tDist = -1f;

        for (var other : mc.level.players()) {
            if (other == p || !other.isAlive()) continue;
            float d = (float) p.distanceTo(other);
            if (target == null || d < tDist) { target = other; tDist = d; }
        }

        if (target == null) {
            AABB mobBox = new AABB(p.blockPosition()).inflate(20);
            for (Entity e : mc.level.getEntities(p, mobBox,
                    ent -> ent instanceof Monster && ent.isAlive())) {
                float d = (float) p.distanceTo(e);
                if (target == null || d < tDist) {
                    target = (LivingEntity) e;
                    tDist  = d;
                }
            }
        }

        float   tY     = target != null ? (float) target.getY()                   : Float.NaN;
        float   tVY    = target != null ? (float) target.getDeltaMovement().y      : Float.NaN;
        boolean tMace  = target != null && target.getMainHandItem().is(Items.MACE);

        // "deploying obsidian" proxy: holding obsidian block-item OR end crystal
        boolean tObs   = target != null && (
            target.getMainHandItem().is(Blocks.OBSIDIAN.asItem()) ||
            target.getMainHandItem().is(Items.END_CRYSTAL));

        // ── Nearest end crystal ──────────────────────────────────────────────
        Entity nearCrystal     = null;
        float  nearCrystalDist = -1f;
        AABB   crystalBox      = new AABB(p.blockPosition()).inflate(8);

        for (Entity e : mc.level.getEntities(p, crystalBox,
                ent -> ent.getType() == EntityType.END_CRYSTAL)) {
            float d = (float) p.distanceTo(e);
            if (nearCrystal == null || d < nearCrystalDist) {
                nearCrystal     = e;
                nearCrystalDist = d;
            }
        }

        // ── Blocked face count (N/S/E/W at foot + head) ─────────────────────
        int blocked = 0;
        BlockPos base = p.blockPosition();
        for (Direction dir : new Direction[]{
                Direction.NORTH, Direction.SOUTH,
                Direction.EAST,  Direction.WEST}) {
            BlockState bs = mc.level.getBlockState(base.relative(dir));
            if (bs.is(Blocks.OBSIDIAN) ||
                bs.is(Blocks.BEDROCK)  ||
                bs.is(Blocks.ENDER_CHEST)) {
                blocked++;
            }
        }

        return new WatchdogSnapshot(
            mc.level.getGameTime(),
            hp, dmg,
            vel, p.position(),
            p.onGround(), p.horizontalCollision, hspd,
            target, tY, tVY, tMace, tObs, tDist,
            nearCrystal, nearCrystalDist,
            blocked
        );
    }

    // ── Anomaly evaluator ─────────────────────────────────────────────────────

    /** Returns the highest-priority triggered state, or NONE. */
    private MaceCpvpSituation evaluate(WatchdogSnapshot snap) {
        if (detectState1(snap)) return MaceCpvpSituation.AIRBORNE_COMBO_LOCK;
        if (detectState2(snap)) return MaceCpvpSituation.BOXED_CAGE_TRAP;
        if (detectState3(snap)) return MaceCpvpSituation.CEILING_MACE_DROP;
        if (detectState4(snap)) return MaceCpvpSituation.LOW_GROUND_EXPOSURE;
        if (detectState5(snap)) return MaceCpvpSituation.CRYSTAL_CROSS_FIRE;
        if (detectState6(snap)) return MaceCpvpSituation.PROGRESSIVE_SURROUND;
        return MaceCpvpSituation.NONE;
    }

    // ── Individual detectors ──────────────────────────────────────────────────

    /**
     * STATE 1 — AIRBORNE_COMBO_LOCK
     * Metric: VerticalVelocitySpike (vel.y > 0.4, !onGround) in last 3 ticks
     *       + health drop in last 5 ticks
     *       + enemy within 5 m
     */
    private boolean detectState1(WatchdogSnapshot snap) {
        long vSpike = history.stream()
            .skip(Math.max(0L, history.size() - 3L))
            .filter(s -> s.velocity.y > 0.4 && !s.onGround)
            .count();
        if (vSpike == 0) return false;

        boolean recentDamage = history.stream()
            .skip(Math.max(0L, history.size() - 5L))
            .anyMatch(s -> s.damageTaken > 0);

        return recentDamage && snap.targetDist >= 0 && snap.targetDist < 5f;
    }

    /**
     * STATE 2 — BOXED_CAGE_TRAP
     * Metric: HorizontalTrappedState — 5+ consecutive ticks with
     *         horizontalSpeed < 0.05 AND horizontalCollision AND recent damage,
     *         AND current snapshot has 3+ blocked faces.
     */
    private boolean detectState2(WatchdogSnapshot snap) {
        if (snap.blockedFaces < 3) return false;

        int consecutive = 0;
        int maxConsec   = 0;
        for (WatchdogSnapshot s : history) {
            if (s.horizontalSpeed < 0.05 && s.horizontalCollision) {
                consecutive++;
                maxConsec = Math.max(maxConsec, consecutive);
            } else {
                consecutive = 0;
            }
        }
        if (maxConsec < 5) return false;

        return history.stream()
            .skip(Math.max(0L, history.size() - 5L))
            .anyMatch(s -> s.damageTaken > 0);
    }

    /**
     * STATE 3 — CEILING_MACE_DROP
     * Metric: TargetElevationThreat (target.Y > player.Y + 1.5)
     *       + target holds Mace
     *       + target is falling (vel.y < -0.1)
     */
    private boolean detectState3(WatchdogSnapshot snap) {
        if (snap.primaryTarget == null || !snap.targetHasMace) return false;
        if (!snap.targetFalling) return false;
        return snap.targetY > (float) snap.position.y + 1.5f;
    }

    /**
     * STATE 4 — LOW_GROUND_EXPOSURE
     * Metric: enemy Y is 1.5–3.5 blocks BELOW player
     *       + enemy is holding obsidian or end crystals (setting up surround/crystal)
     */
    private boolean detectState4(WatchdogSnapshot snap) {
        if (snap.primaryTarget == null) return false;
        float yDiff = (float) snap.position.y - snap.targetY;
        return yDiff >= 1.5f && yDiff <= 3.5f && snap.targetDeployingObsidian;
    }

    /**
     * STATE 5 — CRYSTAL_CROSS_FIRE
     * Metric: live EndCrystal within 6 m + enemy within 20 m (can pop it).
     */
    private boolean detectState5(WatchdogSnapshot snap) {
        return snap.nearestCrystalDist >= 0 && snap.nearestCrystalDist < 6f
            && snap.primaryTarget != null && snap.targetDist < 20f;
    }

    /**
     * STATE 6 — PROGRESSIVE_SURROUND
     * Metric: blocked face count increasing over the last 5 ticks (someone is
     *         actively placing around the player), current count 1–3
     *         (still an open face to pearl through).
     */
    private boolean detectState6(WatchdogSnapshot snap) {
        if (history.size() < 5)           return false;
        if (snap.blockedFaces < 1)         return false;
        if (snap.blockedFaces >= 4)        return false;  // fully boxed → state 2

        var all    = new ArrayList<>(history);
        int start  = Math.max(0, all.size() - 5);
        int first  = all.get(start).blockedFaces;
        int last   = all.get(all.size() - 1).blockedFaces;

        return last > first;    // face count strictly increasing
    }

    // ── Dispatcher ────────────────────────────────────────────────────────────

    private void dispatch(Minecraft mc, WatchdogSnapshot snap, MaceCpvpSituation state) {
        // Always cancel Baritone pathing first
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone()
                .getPathingBehavior().cancelEverything();
        } catch (Exception ignored) {}

        switch (state) {
            case AIRBORNE_COMBO_LOCK:
                suppressTicks = 30;
                evasion.executeState1(mc, snap);
                break;
            case BOXED_CAGE_TRAP:
                suppressTicks = 20;
                evasion.executeState2(mc, snap);
                break;
            case CEILING_MACE_DROP:
                suppressTicks = 15;
                evasion.executeState3(mc, snap);
                break;
            case LOW_GROUND_EXPOSURE:
                suppressTicks = 40;
                evasion.executeState4(mc, snap);
                break;
            case CRYSTAL_CROSS_FIRE:
                suppressTicks = 5;
                evasion.executeState5(mc, snap);
                break;
            case PROGRESSIVE_SURROUND:
                suppressTicks = 20;
                evasion.executeState6(mc, snap);
                break;
            default:
                break;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Call this from a Mixin on MultiPlayerGameMode.attack() when the attack
     * does not result in an EntityHitResult. Feeds the DeSyncMissTracker metric.
     */
    public void recordSwingMiss() {
        deSyncMissCount++;
        LOGGER.debug("[WATCHDOG DESYNCTRACE] Miss count: {}", deSyncMissCount);
    }

    public MaceCpvpSituation getActiveSituation() { return active; }

    public int getDeSyncMissCount()                { return deSyncMissCount; }

    public int historySize()                       { return history.size(); }
}
