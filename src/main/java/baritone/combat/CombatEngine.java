package baritone.combat;

import baritone.Baritone;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Top-level combat orchestrator.  Called every tick by CombatProcess.
 *
 * Decision order:
 *   1. Reset all combat inputs.
 *   2. HealthGate — if HP < 50%, disengage and heal; skip steps 3-5.
 *   3. CreepeTactics — if any creeper is fusing, override everything.
 *   4. TargetSelector — pick the best living target.
 *   5a. Target > 4.5 m: hand Baritone a GoalNear(3) to close the gap.
 *   5b. Target <= 4.5 m: take direct input control.
 *       SpacingController manages W/A/D/sprint/W-tap.
 *       AttackValidator fires CLICK_LEFT only when hit is legit (cooldown,
 *       range, LOS, and falling-for-crit all satisfied).
 *       Player rotation is set directly toward the target each tick.
 */
public final class CombatEngine {

    private static final float ENGAGE_DISTANCE = 4.5f;

    private final Baritone baritone;
    private final IPlayerContext ctx;
    private final AwarenessContext awarenessCtx;

    private final TargetSelector     targetSelector;
    private final SpacingController  spacingController;
    private final AttackValidator    attackValidator;
    private final CreepeTactics      creepeTactics;
    private final HealthGate         healthGate;

    public CombatEngine(Baritone baritone, AwarenessContext awarenessCtx) {
        this.baritone       = baritone;
        this.ctx            = baritone.getPlayerContext();
        this.awarenessCtx   = awarenessCtx;
        this.targetSelector = new TargetSelector();
        spacingController   = new SpacingController();
        attackValidator     = new AttackValidator(ctx, spacingController);
        creepeTactics       = new CreepeTactics(ctx);
        healthGate          = new HealthGate(ctx);
    }

    public PathingCommand tick() {
        InputOverrideHandler input = baritone.getInputOverrideHandler();
        if (input == null || ctx.player() == null) return pause();

        resetInputs(input);

        // 1. Health gate
        if (healthGate.shouldHeal(awarenessCtx)) {
            return healthGate.tick(input, awarenessCtx);
        }

        // 2. Creeper override
        PathingCommand creeperCmd = creepeTactics.tick(input, awarenessCtx);
        if (creeperCmd != null) return creeperCmd;

        // 3. Target selection
        ThreatEntry target = targetSelector.select(awarenessCtx);
        if (target == null || !target.tracked.entity.isAlive()) return pause();

        float distance = target.tracked.distance;

        if (distance > ENGAGE_DISTANCE) {
            // Let Baritone path to close the gap
            return new PathingCommand(
                new GoalNear(target.tracked.entity.blockPosition(), 3),
                PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }

        // 4. Close-range: direct control
        aimAt(target.tracked.entity);
        spacingController.tick(input, target, awarenessCtx);
        attackValidator.tick(input, target);

        return pause();
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────

    private void aimAt(Entity target) {
        net.minecraft.world.entity.player.Player player = ctx.player();
        if (player == null) return;
        Vec3 eye    = player.getEyePosition(1f);
        Vec3 tEye   = target.getEyePosition(1f);
        Vec3 dir    = tEye.subtract(eye).normalize();
        float yaw   = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, dir.y))));
        player.setYRot(yaw);   player.yRotO = yaw;
        player.setXRot(pitch); player.xRotO = pitch;
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
