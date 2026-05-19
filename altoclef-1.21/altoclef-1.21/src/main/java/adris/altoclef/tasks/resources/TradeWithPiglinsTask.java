package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.entity.AbstractDoToEntityTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.EntityHelper;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.HashSet;
import java.util.Optional;

public class TradeWithPiglinsTask extends ResourceTask {

    // TODO: Settings? Custom parameter?
    private static final boolean AVOID_HOGLINS = true;
    private static final double HOGLIN_AVOID_TRADE_RADIUS = 64;
    // If we're too far away from a trading piglin, we risk deloading them and losing the trade.
    private static final double TRADING_PIGLIN_TOO_FAR_AWAY = 64 + 8;
    private final int _goldBuffer;
    private final Task _tradeTask = new PerformTradeWithPiglin();
    private Task _goldTask = null;

    public TradeWithPiglinsTask(int goldBuffer, ItemTarget[] itemTargets) {
        super(itemTargets);
        _goldBuffer = goldBuffer;
    }

    public TradeWithPiglinsTask(int goldBuffer, ItemTarget target) {
        super(target);
        _goldBuffer = goldBuffer;
    }

    public TradeWithPiglinsTask(int goldBuffer, Item item, int targetCount) {
        super(item, targetCount);
        _goldBuffer = goldBuffer;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // Collect gold if we don't have it.
        if (_goldTask != null && _goldTask.isActive() && !_goldTask.isFinished(mod)) {
            setDebugState("Collecting gold");
            return _goldTask;
        }
        if (!mod.getItemStorage().hasItem(Items.GOLD_INGOT)) {
            if (_goldTask == null) _goldTask = TaskCatalogue.getItemTask(Items.GOLD_INGOT, _goldBuffer);
            return _goldTask;
        }

        // If we have no piglin nearby, explore until we find piglin.
        if (!mod.getEntityTracker().entityFound(PiglinEntity.class)) {
            setDebugState("Wandering");
            return new TimeoutWanderTask(false);
        }

        // If we have a trading piglin that's too far away, get closer to it.

        // Find gold and trade with a piglin
        setDebugState("Trading with Piglin");
        return _tradeTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof TradeWithPiglinsTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Trading with Piglins";
    }

    static class PerformTradeWithPiglin extends AbstractDoToEntityTask {

        private static final double PIGLIN_NEARBY_RADIUS = 10;
        private final TimerGame _barterTimeout = new TimerGame(2);
        private final TimerGame _intervalTimeout = new TimerGame(10);
        private final HashSet<Entity> _blacklisted = new HashSet<>();
        private Entity _currentlyBartering = null;

        public PerformTradeWithPiglin() {
            super(3);
        }

        @Override
        protected void onStart(AltoClef mod) {
            super.onStart(mod);

            mod.getBehaviour().push();

            // Don't throw away our gold lol
            mod.getBehaviour().addProtectedItems(Items.GOLD_INGOT);

            // Don't attack piglins unless we've blacklisted them.
            mod.getBehaviour().addForceFieldExclusion(entity -> {
                if (entity instanceof PiglinEntity) {
                    return !_blacklisted.contains(entity);
                }
                return false;
            });
            //_blacklisted.clear();
        }

        @Override
        protected void onStop(AltoClef mod, Task interruptTask) {
            super.onStop(mod, interruptTask);
            mod.getBehaviour().pop();
        }

        @Override
        protected boolean isSubEqual(AbstractDoToEntityTask other) {
            return other instanceof PerformTradeWithPiglin;
        }

        @Override
        protected Task onEntityInteract(AltoClef mod, Entity entity) {

            // If we didn't run this in a while, we can retry bartering.
            if (_intervalTimeout.elapsed()) {
                // We didn't interact for a while, continue bartering as usual.
                _barterTimeout.reset();
                _intervalTimeout.reset();
            }

            // We're trading so reset the barter timeout
            if (EntityHelper.isTradingPiglin(_currentlyBartering)) {
                _barterTimeout.reset();
            }

            // We're bartering a new entity.
            if (!entity.equals(_currentlyBartering)) {
                _currentlyBartering = entity;
                _barterTimeout.reset();
            }

            if (_barterTimeout.elapsed()) {
                // We failed bartering.
                Debug.logMessage("Failed bartering with current piglin, blacklisting.");
                _blacklisted.add(_currentlyBartering);
                _barterTimeout.reset();
                _currentlyBartering = null;
                return null;
            }

            if (AVOID_HOGLINS && _currentlyBartering != null && !EntityHelper.isTradingPiglin(_currentlyBartering)) {
                Optional<Entity> closestHoglin = mod.getEntityTracker().getClosestEntity(_currentlyBartering.getPos(), HoglinEntity.class);
                if (closestHoglin.isPresent() && closestHoglin.get().isInRange(entity, HOGLIN_AVOID_TRADE_RADIUS)) {
                    Debug.logMessage("Aborting further trading because a hoglin showed up");
                    _blacklisted.add(_currentlyBartering);
                    _barterTimeout.reset();
                    _currentlyBartering = null;
                }
            }

            setDebugState("Trading with piglin");

            if (mod.getSlotHandler().forceEquipItem(Items.GOLD_INGOT)) {
                mod.getController().interactEntity(mod.getPlayer(), entity, Hand.MAIN_HAND);
                _intervalTimeout.reset();
            }
            return null;
        }

        @Override
        protected Optional<Entity> getEntityTarget(AltoClef mod) {
            // Ignore trading piglins
            Optional<Entity> found = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(),
                    entity -> {
                        if (_blacklisted.contains(entity)
                                || EntityHelper.isTradingPiglin(entity)
                                || (entity instanceof LivingEntity && ((LivingEntity) entity).isBaby())
                                || (_currentlyBartering != null && !entity.isInRange(_currentlyBartering, PIGLIN_NEARBY_RADIUS))) {
                            return false;
                        }

                        if (AVOID_HOGLINS) {
                            // Avoid trading if hoglin is anywhere remotely nearby.
                            Optional<Entity> closestHoglin = mod.getEntityTracker().getClosestEntity(entity.getPos(), HoglinEntity.class);
                            return closestHoglin.isEmpty() || !closestHoglin.get().isInRange(entity, HOGLIN_AVOID_TRADE_RADIUS);
                        }
                        return true;
                    }, PiglinEntity.class
            );
            if (found.isEmpty()) {
                if (_currentlyBartering != null && (_blacklisted.contains(_currentlyBartering) || !_currentlyBartering.isAlive())) {
                    _currentlyBartering = null;
                }
                found = Optional.ofNullable(_currentlyBartering);
            }
            return found;
        }

        @Override
        protected String toDebugString() {
            return "Trading with piglin";
        }
    }

}
