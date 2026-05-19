package adris.altoclef.control;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controls and applies killaura
 */
public class KillAura {
    // Smart aura data
    private final List<Entity> _targets = new ArrayList<>();
    private final TimerGame _hitDelay = new TimerGame(0.2);
    boolean _shielding = false;
    private double _forceFieldRange = Double.POSITIVE_INFINITY;
    private Entity _forceHit = null;

    public static void equipWeapon(AltoClef mod) {
        List<ItemStack> invStacks = mod.getItemStorage().getItemStacksPlayerInventory(true);
        if (!invStacks.isEmpty()) {
            float handDamage = Float.NEGATIVE_INFINITY;
            for (ItemStack invStack : invStacks) {
                if (invStack.getItem() instanceof SwordItem item) {
                    float itemDamage = item.getMaterial().getAttackDamage();
                    Item handItem = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem();
                    if (handItem instanceof SwordItem handToolItem) {
                        handDamage = handToolItem.getMaterial().getAttackDamage();
                    }
                    if (itemDamage > handDamage) {
                        mod.getSlotHandler().forceEquipItem(item);
                    } else {
                        mod.getSlotHandler().forceEquipItem(handItem);
                    }
                }
            }
        }
    }

    public void tickStart() {
        _targets.clear();
        _forceHit = null;
    }

    public void applyAura(Entity entity) {
        _targets.add(entity);
        // Always hit ghast balls.
        if (entity instanceof FireballEntity) _forceHit = entity;
    }

    public void setRange(double range) {
        _forceFieldRange = range;
    }

    public void tickEnd(AltoClef mod) {
        Optional<Entity> entities = _targets.stream().min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(mod.getPlayer())));
        if (entities.isPresent() && mod.getPlayer().getHealth() >= 10 &&
                !mod.getEntityTracker().entityFound(PotionEntity.class) && !mod.getFoodChain().needsToEat() &&
                (Double.isInfinite(_forceFieldRange) || entities.get().squaredDistanceTo(mod.getPlayer()) < _forceFieldRange * _forceFieldRange ||
                        entities.get().squaredDistanceTo(mod.getPlayer()) < 40) &&
                !mod.getMLGBucketChain().isFallingOhNo(mod) && mod.getMLGBucketChain().doneMLG() &&
                !mod.getMLGBucketChain().isChorusFruiting()) {
            PlayerSlot offhandSlot = PlayerSlot.OFFHAND_SLOT;
            Item offhandItem = StorageHelper.getItemStackInSlot(offhandSlot).getItem();
            if (entities.get().getClass() != CreeperEntity.class && entities.get().getClass() != HoglinEntity.class &&
                    entities.get().getClass() != ZoglinEntity.class && entities.get().getClass() != WardenEntity.class &&
                    entities.get().getClass() != WitherEntity.class
                    && (mod.getItemStorage().hasItem(Items.SHIELD) || mod.getItemStorage().hasItemInOffhand(Items.SHIELD))
                    && !mod.getPlayer().getItemCooldownManager().isCoolingDown(offhandItem)
                    && mod.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
                LookHelper.lookAt(mod, entities.get().getEyePos());
                ItemStack shieldSlot = StorageHelper.getItemStackInSlot(PlayerSlot.OFFHAND_SLOT);
                if (shieldSlot.getItem() != Items.SHIELD) {
                    mod.getSlotHandler().forceEquipItemToOffhand(Items.SHIELD);
                } else {
                    startShielding(mod);
                }
            }
            performDelayedAttack(mod);
        } else {
            stopShielding(mod);
        }
        // Run force field on map
        switch (mod.getModSettings().getForceFieldStrategy()) {
            case FASTEST:
                performFastestAttack(mod);
                break;
            case SMART:
                if (_targets.size() <= 2 || _targets.stream().allMatch(entity -> entity instanceof SkeletonEntity) ||
                        _targets.stream().allMatch(entity -> entity instanceof WitchEntity) ||
                        _targets.stream().allMatch(entity -> entity instanceof PillagerEntity) ||
                        _targets.stream().allMatch(entity -> entity instanceof PiglinEntity) ||
                        _targets.stream().allMatch(entity -> entity instanceof StrayEntity) ||
                        _targets.stream().allMatch(entity -> entity instanceof BlazeEntity)) {
                    performDelayedAttack(mod);
                } else {
                    if (!mod.getFoodChain().needsToEat() && !mod.getMLGBucketChain().isFallingOhNo(mod) &&
                            mod.getMLGBucketChain().doneMLG() && !mod.getMLGBucketChain().isChorusFruiting()) {
                        // Attack force mobs ALWAYS.
                        if (_forceHit != null) {
                            attack(mod, _forceHit, true);
                        }
                        if (_hitDelay.elapsed()) {
                            _hitDelay.reset();

                            Optional<Entity> toHit = _targets.stream().min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(mod.getPlayer())));

                            toHit.ifPresent(entity -> attack(mod, entity, true));
                        }
                    }
                }
                break;
            case DELAY:
                performDelayedAttack(mod);
                break;
            case OFF:
                break;
        }
    }

    private void performDelayedAttack(AltoClef mod) {
        if (!mod.getFoodChain().needsToEat() && !mod.getMLGBucketChain().isFallingOhNo(mod) &&
                mod.getMLGBucketChain().doneMLG() && !mod.getMLGBucketChain().isChorusFruiting()) {
            if (_forceHit != null) {
                attack(mod, _forceHit, true);
            }
            // wait for the attack delay
            if (_targets.isEmpty()) {
                return;
            }

            Optional<Entity> toHit = _targets.stream().min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(mod.getPlayer())));

            if (mod.getPlayer() == null || mod.getPlayer().getAttackCooldownProgress(0) < 1) {
                return;
            }

            toHit.ifPresent(entity -> attack(mod, entity, true));
        }
    }

    private void performFastestAttack(AltoClef mod) {
        if (!mod.getFoodChain().needsToEat() && !mod.getMLGBucketChain().isFallingOhNo(mod) &&
                mod.getMLGBucketChain().doneMLG() && !mod.getMLGBucketChain().isChorusFruiting()) {
            // Just attack whenever you can
            for (Entity entity : _targets) {
                attack(mod, entity);
            }
        }
    }

    private void attack(AltoClef mod, Entity entity) {
        attack(mod, entity, false);
    }

    private void attack(AltoClef mod, Entity entity, boolean equipSword) {
        if (entity == null) return;
        if (!(entity instanceof FireballEntity)) {
            LookHelper.lookAt(mod, entity.getEyePos());
        }
        if (Double.isInfinite(_forceFieldRange) || entity.squaredDistanceTo(mod.getPlayer()) < _forceFieldRange * _forceFieldRange ||
                entity.squaredDistanceTo(mod.getPlayer()) < 40) {
            if (entity instanceof FireballEntity) {
                mod.getControllerExtras().attack(entity);
            }
            boolean canAttack;
            if (equipSword) {
                equipWeapon(mod);
                canAttack = true;
            } else {
                // Equip non-tool
                canAttack = mod.getSlotHandler().forceDeequipHitTool();
            }
            if (canAttack) {
                if (mod.getPlayer().isOnGround() || mod.getPlayer().getVelocity().getY() < 0 || mod.getPlayer().isTouchingWater()) {
                    mod.getControllerExtras().attack(entity);
                }
            }
        }
    }

    public void startShielding(AltoClef mod) {
        _shielding = true;
        mod.getInputControls().hold(Input.SNEAK);
        mod.getInputControls().hold(Input.CLICK_RIGHT);
        mod.getClientBaritone().getPathingBehavior().requestPause();
        mod.getExtraBaritoneSettings().setInteractionPaused(true);
        if (!mod.getPlayer().isBlocking()) {
            ItemStack handItem = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot());
            if (handItem.getItem().getComponents().contains(DataComponentTypes.FOOD)) {
                List<ItemStack> spaceSlots = mod.getItemStorage().getItemStacksPlayerInventory(false);
                if (!spaceSlots.isEmpty()) {
                    for (ItemStack spaceSlot : spaceSlots) {
                        if (spaceSlot.isEmpty()) {
                            mod.getSlotHandler().clickSlot(PlayerSlot.getEquipSlot(), 0, SlotActionType.QUICK_MOVE);
                            return;
                        }
                    }
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
                garbage.ifPresent(slot -> mod.getSlotHandler().forceEquipItem(StorageHelper.getItemStackInSlot(slot).getItem()));
            }
        }
    }

    public void stopShielding(AltoClef mod) {
        if (_shielding) {
            ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
            if (cursor.getItem().getComponents().contains(DataComponentTypes.FOOD)) {
                Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false).or(() -> StorageHelper.getGarbageSlot(mod));
                if (toMoveTo.isPresent()) {
                    Slot garbageSlot = toMoveTo.get();
                    mod.getSlotHandler().clickSlot(garbageSlot, 0, SlotActionType.PICKUP);
                }
            }
            mod.getInputControls().release(Input.SNEAK);
            mod.getInputControls().release(Input.CLICK_RIGHT);
            mod.getInputControls().release(Input.JUMP);
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
            _shielding = false;
        }
    }

    public enum Strategy {
        OFF,
        FASTEST,
        DELAY,
        SMART
    }
}
