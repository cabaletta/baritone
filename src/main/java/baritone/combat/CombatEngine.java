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
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

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

        shieldController.manageOffHand(self);

        boolean ownShieldBroken = shieldController.isOwnShieldBroken();
        pearlController.tick(input, awarenessCtx, ownShieldBroken);
        if (pearlController.justThrew()) return pause();

        if (healthGate.shouldHeal(awarenessCtx)) {
            return healthGate.tick(input, awarenessCtx);
        }

        PathingCommand creeperCmd = creepeTactics.tick(input, awarenessCtx);
        if (creeperCmd != null) return creeperCmd;

        ThreatEntry target = targetSelector.select(awarenessCtx);
        if (target == null || !target.tracked.entity.isAlive()) return pause();

        float distance = (float) target.tracked.distance;

        if (target.tracked.entity instanceof Creeper) {
            if (distance > 3.5f) {
                return new PathingCommand(
                    new GoalNear(target.tracked.entity.blockPosition(), 3),
                    PathingCommandType.REVALIDATE_GOAL_AND_PATH);
            }
            return pause();
        }

        if (distance > ENGAGE_DISTANCE) {
            return new PathingCommand(
                new GoalNear(target.tracked.entity.blockPosition(), 3),
                PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }

        aimAt(target.tracked.entity);

        int desiredSlot = weaponSelector.select(player, awarenessCtx);
        InventoryHelper.setSelected(player, desiredSlot);

        attackValidator.tick(input, target);
        spacingController.tick(input, target, awarenessCtx);

        return pause();
    }

    public boolean hasPendingEscape() {
        return creepeTactics.hasPendingEscape();
    }

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
