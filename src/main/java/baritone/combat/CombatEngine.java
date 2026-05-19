package baritone.combat;

import baritone.Baritone;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.SelfState;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Top-level combat orchestrator.  Called every tick by CombatProcess.
 *
 * Tick order:
 *   1. Reset all combat inputs.
 *   2. ShieldController.manageOffHand — totem/shield swap based on HP.
 *   3. PearlController — escape throw when surrounded or shield broken + low HP.
 *   4. HealthGate — disengage and heal when HP < 50%.
 *   5. CreepeTactics — fusing creeper override.
 *   6. TargetSelector — pick best living target.
 *   7a. Target > 4.5 m → Baritone GoalNear(3) to close gap.
 *   7b. Target ≤ 4.5 m → direct input control:
 *       WeaponSelector sets hotbar slot (sword or axe).
 *       SpacingController drives W/A/D/sprint/W-tap.
 *       AttackValidator fires CLICK_LEFT only on legit hits (cooldown,
 *       range, LOS, falling-for-crit).
 *       Player rotation set directly toward target each tick.
 */
public final class CombatEngine {

    private static final float ENGAGE_DISTANCE = 4.5f;

    private final Baritone       baritone;
    private final IPlayerContext ctx;
    private final AwarenessContext awarenessCtx;

    private final TargetSelector    targetSelector;
    private final SpacingController spacingController;
    private final AttackValidator   attackValidator;
    private final CreepeTactics     creepeTactics;
    private final HealthGate        healthGate;
    private final WeaponSelector    weaponSelector;
    private final ShieldController  shieldController;
    private final PearlController   pearlController;

    public CombatEngine(Baritone baritone, AwarenessContext awarenessCtx) {
        this.baritone      = baritone;
        this.ctx           = baritone.getPlayerContext();
        this.awarenessCtx  = awarenessCtx;
        targetSelector    = new TargetSelector();
        spacingController = new SpacingController();
        attackValidator   = new AttackValidator(ctx, spacingController);
        creepeTactics     = new CreepeTactics(ctx);
        healthGate        = new HealthGate(ctx);
        weaponSelector    = new WeaponSelector();
        shieldController  = new ShieldController(ctx);
        pearlController   = new PearlController(ctx);
    }

    public PathingCommand tick() {
        InputOverrideHandler input = baritone.getInputOverrideHandler();
        Player player = ctx.player();
        if (input == null || player == null) return pause();

        resetInputs(input);

        SelfState self = awarenessCtx.getSelf();

        // 1. Off-hand management (totem ↔ shield swap)
        shieldController.manageOffHand(self);

        // 2. Pearl escape (runs before health gate so we can flee even while trying to heal)
        boolean ownShieldBroken = shieldController.isOwnShieldBroken();
        pearlController.tick(input, awarenessCtx, ownShieldBroken);
        if (pearlController.justThrew()) return pause();

        // 3. Health gate
        if (healthGate.shouldHeal(awarenessCtx)) {
            return healthGate.tick(input, awarenessCtx);
        }

        // 4. Creeper override
        PathingCommand creeperCmd = creepeTactics.tick(input, awarenessCtx);
        if (creeperCmd != null) return creeperCmd;

        // 5. Target selection
        ThreatEntry target = targetSelector.select(awarenessCtx);
        if (target == null || !target.tracked.entity.isAlive()) return pause();

        float distance = (float) target.tracked.distance;

        if (distance > ENGAGE_DISTANCE) {
            return new PathingCommand(
                new GoalNear(target.tracked.entity.blockPosition(), 3),
                PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }

        // 6. Direct input control at close range
        aimAt(target.tracked.entity);

        // Weapon selection (sword vs axe for shield breaking)
        int desiredSlot = weaponSelector.select(player, awarenessCtx);
        player.getInventory().selected = desiredSlot;

        attackValidator.tick(input, target);
        spacingController.tick(input, target, awarenessCtx);

        return pause();
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────

    private void aimAt(Entity target) {
        Vec3  eye   = ctx.player().getEyePosition(1f);
        Vec3  tEye  = target.getEyePosition(1f);
        Vec3  dir   = tEye.subtract(eye).normalize();
        float yaw   = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, dir.y))));
        Player p = ctx.player();
        p.setYRot(yaw);   p.yRotO = yaw;
        p.setXRot(pitch); p.xRotO = pitch;
    }

    private void resetInputs(InputOverrideHandler input) {
        input.setInputForceState(Input.MOVE_FORWARD, false);
        input.setInputForceState(Input.MOVE_BACK,    false);
        input.setInputForceState(Input.MOVE_LEFT,    false);
        input.setInputForceState(Input.MOVE_RIGHT,   false);
        input.setInputForceState(Input.SPRINT,       false);
        input.setInputForceState(Input.JUMP,         false);
        input.setInputForceState(Input.CLICK_LEFT,   false);
        input.setInputForceState(Input.CLICK_RIGHT,  false);
    }

    private static PathingCommand pause() {
        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
    }
}
