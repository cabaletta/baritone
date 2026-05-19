package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.speedrun.BeatMinecraft2Task;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;

public class ShootArrowSimpleProjectileTask extends Task {

    private final Entity target;
    private boolean shooting = false;
    private boolean shot = false;

    private final TimerGame shotTimer = new TimerGame(1);

    public ShootArrowSimpleProjectileTask(Entity target) {
        this.target = target;
    }

    @Override
    protected void onStart(AltoClef mod) {
        shooting = false;
    }

    private static Rotation calculateThrowLook(AltoClef mod, Entity target) {
        // Velocity based on bow charge.
        float velocity = (mod.getPlayer().getItemUseTime() - mod.getPlayer().getItemUseTimeLeft()) / 20f;
        velocity = (velocity * velocity + velocity * 2) / 3;
        if (velocity > 1) velocity = 1;

        // Find the position of the center
        Vec3d targetCenter = target.getBoundingBox().getCenter();

        double posX = targetCenter.getX();
        double posY = targetCenter.getY();
        double posZ = targetCenter.getZ();

        // Adjusting for hitbox heights
        posY -= 1.9f - target.getHeight();

        double relativeX = posX - mod.getPlayer().getX();
        double relativeY = posY - mod.getPlayer().getY();
        double relativeZ = posZ - mod.getPlayer().getZ();

        // Calculate the pitch
        double hDistance = Math.sqrt(relativeX * relativeX + relativeZ * relativeZ);
        double hDistanceSq = hDistance * hDistance;
        final float g = 0.006f;
        float velocitySq = velocity * velocity;
        float pitch = (float) -Math.toDegrees(Math.atan((velocitySq - Math.sqrt(velocitySq * velocitySq - g * (g * hDistanceSq + 2 * relativeY * velocitySq))) / (g * hDistance)));

        // Set player rotation
        if (Float.isNaN(pitch)) {
            return new Rotation(target.getYaw(), target.getPitch());
        } else {
            return new Rotation(Vec3dToYaw(mod, new Vec3d(posX, posY, posZ)), pitch);
        }
    }

    private static float Vec3dToYaw(AltoClef mod, Vec3d vec) {
        return (mod.getPlayer().getYaw() +
                MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(vec.getZ() - mod.getPlayer().getZ(), vec.getX() - mod.getPlayer().getX())) - 90f - mod.getPlayer().getYaw()));
    }

    @Override
    protected Task onTick(AltoClef mod) {
        setDebugState("Shooting projectile");
        List<Item> requiredArrows = Arrays.asList(Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW);

        if (!(mod.getItemStorage().hasItem(Items.BOW) &&
                requiredArrows.stream().anyMatch(mod.getItemStorage()::hasItem))) {
            Debug.logMessage("Missing items, stopping.");
            return null;
        }

        Rotation lookTarget = calculateThrowLook(mod, target);
        LookHelper.lookAt(mod, lookTarget);

        // check if we are holding a bow
        boolean charged = mod.getPlayer().getItemUseTime() > 20 && mod.getPlayer().getActiveItem().getItem() == Items.BOW;

        mod.getSlotHandler().forceEquipItem(Items.BOW);

        if (LookHelper.isLookingAt(mod, lookTarget) && !shooting) {
            mod.getInputControls().hold(Input.CLICK_RIGHT);
            shooting = true;
            shotTimer.reset();
        }
        if (shooting && charged) {
            List<ArrowEntity> arrows = mod.getEntityTracker().getTrackedEntities(ArrowEntity.class);
            // If any of the arrows belong to us and are moving, do not shoot yet
            // Prevents from shooting multiple arrows to the same target
            for (ArrowEntity arrow : arrows) {
                if (arrow.getOwner() == mod.getPlayer()) {
                    Vec3d velocity = arrow.getVelocity();
                    Vec3d delta = target.getPos().subtract(arrow.getPos());
                    boolean isMovingTowardsTarget = velocity.dotProduct(delta) > 0;
                    if (isMovingTowardsTarget) {
                        return null;
                    }
                }
            }

            if (BeatMinecraft2Task.getConfig().renderDistanceManipulation && MinecraftClient.getInstance().options.getSimulationDistance().getValue() < 32) {
                // For farther entities, the arrow may get stuck in the air, so we need to increase the simulation distance
                MinecraftClient.getInstance().options.getSimulationDistance().setValue(32);
            }
            mod.getInputControls().release(Input.CLICK_RIGHT); // Release the arrow
            shot = true;
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getInputControls().release(Input.CLICK_RIGHT);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return shot;
    }

    @Override
    protected boolean isEqual(Task other) {
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Shooting arrow at " + target.getType().getTranslationKey();
    }
}