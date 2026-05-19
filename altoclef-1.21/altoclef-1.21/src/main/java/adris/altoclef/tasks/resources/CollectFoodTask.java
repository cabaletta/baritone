package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.container.CraftInTableTask;
import adris.altoclef.tasks.container.SmeltInSmokerTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.speedrun.MarvionBeatMinecraftTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.SmokerSlot;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmokerScreenHandler;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class CollectFoodTask extends Task {

    // Represents order of preferred mobs to least preferred
    private static final CookableFoodTarget[] COOKABLE_FOODS = new CookableFoodTarget[]{
            new CookableFoodTarget("porkchop", PigEntity.class),
            new CookableFoodTarget("beef", CowEntity.class),
            new CookableFoodTarget("chicken", ChickenEntity.class),
            new CookableFoodTarget("mutton", SheepEntity.class),
            new CookableFoodTargetFish("cod", CodEntity.class),
            new CookableFoodTargetFish("salmon", SalmonEntity.class)
    };

    private static final Item[] ITEMS_TO_PICK_UP = new Item[]{
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_APPLE,
            Items.GOLDEN_CARROT,
            Items.BREAD,
            Items.BAKED_POTATO
    };

    private static final CropTarget[] CROPS = new CropTarget[]{
            new CropTarget(Items.WHEAT, Blocks.WHEAT),
            new CropTarget(Items.CARROT, Blocks.CARROTS)
    };

    private final double _unitsNeeded;
    private final TimerGame _checkNewOptionsTimer = new TimerGame(10);
    private SmeltInSmokerTask _smeltTask = null;
    private Task _currentResourceTask = null;

    public CollectFoodTask(double unitsNeeded) {
        _unitsNeeded = unitsNeeded;
    }

    private static double getFoodPotential(ItemStack food) {
        if (food == null) return 0;
        int count = food.getCount();
        if (count <= 0) return 0;
        for (CookableFoodTarget cookable : COOKABLE_FOODS) {
            if (food.getItem() == cookable.getRaw()) {
                assert cookable.getCooked().getComponents().get(DataComponentTypes.FOOD) != null;
                return count * Objects.requireNonNull(cookable.getCooked().getComponents().get(DataComponentTypes.FOOD)).nutrition();
            }
        }
        // We're just an ordinary item.
        if (food.getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            assert food.getItem().getComponents().get(DataComponentTypes.FOOD) != null;
            return count * Objects.requireNonNull(food.getItem().getComponents().get(DataComponentTypes.FOOD)).nutrition();
        }
        return 0;
    }

    // Gets the units of food if we were to convert all of our raw resources to food.
    @SuppressWarnings("RedundantCast")
    private static double calculateFoodPotential(AltoClef mod) {
        double potentialFood = 0;
        for (ItemStack food : mod.getItemStorage().getItemStacksPlayerInventory(true)) {
            potentialFood += getFoodPotential(food);
        }
        int potentialBread = (int) (mod.getItemStorage().getItemCount(Items.WHEAT) / 3) + mod.getItemStorage().getItemCount(Items.HAY_BLOCK) * 3;
        potentialFood += Objects.requireNonNull(Items.BREAD.getComponents().get(DataComponentTypes.FOOD)).nutrition() * potentialBread;
        // Check smelting
        ScreenHandler screen = mod.getPlayer().currentScreenHandler;
        if (screen instanceof SmokerScreenHandler) {
            potentialFood += getFoodPotential(StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_MATERIALS));
            potentialFood += getFoodPotential(StorageHelper.getItemStackInSlot(SmokerSlot.OUTPUT_SLOT));
        }
        return potentialFood;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        // Protect ALL food
        mod.getBehaviour().addProtectedItems(ITEMS_TO_PICK_UP);
        for (CropTarget crop : CROPS) {
            mod.getBlockTracker().trackBlock(crop.cropBlock);
        }

        // Allow us to consume food.
        /*
        for (CookableFoodTarget food : COOKABLE_FOODS)
            mod.getBehaviour().addProtectedItems(food.getRaw(), food.getCooked());
            mod.getBehaviour().addProtectedItems(crop.cropItem);
        }
         */
        mod.getBehaviour().addProtectedItems(Items.HAY_BLOCK, Items.SWEET_BERRIES);

        mod.getBlockTracker().trackBlock(Blocks.HAY_BLOCK);
        mod.getBlockTracker().trackBlock(Blocks.SWEET_BERRY_BUSH);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getEntityTracker().entityFound(ChickenEntity.class)) {
            Optional<Entity> chickens = mod.getEntityTracker().getClosestEntity(ChickenEntity.class);
            if (chickens.isPresent()) {
                Iterable<Entity> entities = mod.getWorld().getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof HostileEntity || entity instanceof SlimeEntity) {
                        if (chickens.get().hasPassenger(entity)) {
                            if (mod.getEntityTracker().isEntityReachable(entity)) {
                                Debug.logMessage("Blacklisting chicken jockey.");
                                mod.getEntityTracker().requestEntityUnreachable(chickens.get());
                            }
                        }
                    }
                }
            }
        }
        if (mod.getBlockTracker().isTracking(Blocks.HAY_BLOCK)) {
            Optional<BlockPos> hay = mod.getBlockTracker().getNearestTracking(Blocks.HAY_BLOCK);
            if (hay.isPresent()) {
                BlockPos haysUpPos = hay.get().up();
                if (mod.getWorld().getBlockState(haysUpPos).getBlock() == Blocks.CARVED_PUMPKIN) {
                    Debug.logMessage("Blacklisting pillage hay bales.");
                    mod.getBlockTracker().requestBlockUnreachable(hay.get(), 0);
                }
            }
        }
        // If we were previously smelting, keep on smelting.
        if (_smeltTask != null && _smeltTask.isActive() && !_smeltTask.isFinished(mod)) {
            // TODO: If we don't have cooking materials, cancel.
            setDebugState("Cooking...");
            if (MarvionBeatMinecraftTask.getConfig().renderDistanceManipulation) {
                MinecraftClient.getInstance().options.getViewDistance().setValue(2);
                MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(0.5);
            }
            return _smeltTask;
        }

        if (_checkNewOptionsTimer.elapsed()) {
            // Try a new resource task
            _checkNewOptionsTimer.reset();
            _currentResourceTask = null;
        }

        if (_currentResourceTask != null && _currentResourceTask.isActive() && !_currentResourceTask.isFinished(mod) && !_currentResourceTask.thisOrChildAreTimedOut()) {
            return _currentResourceTask;
        }

        // Calculate potential
        double potentialFood = calculateFoodPotential(mod);
        if (potentialFood >= _unitsNeeded) {
            // Convert our raw foods
            // PLAN:
            // - If we have hay/wheat, make it into bread
            // - If we have raw foods, smelt all of them

            // Convert Hay+Wheat -> Bread
            if (mod.getItemStorage().getItemCount(Items.WHEAT) >= 3) {
                setDebugState("Crafting Bread");
                Item[] w = new Item[]{Items.WHEAT};
                Item[] o = null;
                // jank
                _currentResourceTask = new CraftInTableTask(new RecipeTarget(Items.BREAD, 99999999, CraftingRecipe.newShapedRecipe("bread", new Item[][]{w, w, w, o, o, o, o, o, o}, 1)), false, false);
                return _currentResourceTask;
            }
            if (mod.getItemStorage().getItemCount(Items.HAY_BLOCK) >= 1) {
                setDebugState("Crafting Wheat");
                Item[] o = null;
                _currentResourceTask = new CraftInInventoryTask(new RecipeTarget(Items.WHEAT, 99999999, CraftingRecipe.newShapedRecipe("wheat", new Item[][]{new Item[]{Items.HAY_BLOCK}, o, o, o}, 9)), false, false);
                return _currentResourceTask;
            }
            // Convert raw foods -> cooked foods

            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                int rawCount = mod.getItemStorage().getItemCount(cookable.getRaw());
                if (rawCount > 0) {
                    //Debug.logMessage("STARTING COOK OF " + cookable.getRaw().getTranslationKey());
                    int toSmelt = rawCount + mod.getItemStorage().getItemCount(cookable.getCooked());
                    _smeltTask = new SmeltInSmokerTask(new SmeltTarget(new ItemTarget(cookable.cookedFood, toSmelt), new ItemTarget(cookable.rawFood, rawCount)));
                    _smeltTask.ignoreMaterials();
                    return _smeltTask;
                }
            }
        } else {
            // Pick up food items from ground
            for (Item item : ITEMS_TO_PICK_UP) {
                Task t = this.pickupTaskOrNull(mod, item);
                if (t != null) {
                    setDebugState("Picking up Food: " + item.getTranslationKey());
                    _currentResourceTask = t;
                    return _currentResourceTask;
                }
            }
            // Pick up raw/cooked foods on ground
            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                Task t = this.pickupTaskOrNull(mod, cookable.getRaw(), 20);
                if (t == null) t = this.pickupTaskOrNull(mod, cookable.getCooked(), 40);
                if (t != null) {
                    setDebugState("Picking up Cookable food");
                    _currentResourceTask = t;
                    return _currentResourceTask;
                }
            }
            // Hay blocks
            Task hayTaskBlock = this.pickupBlockTaskOrNull(mod, Blocks.HAY_BLOCK, Items.HAY_BLOCK, 300);
            if (hayTaskBlock != null) {
                setDebugState("Collecting Hay");
                _currentResourceTask = hayTaskBlock;
                return _currentResourceTask;
            }
            // Crops
            for (CropTarget target : CROPS) {
                // If crops are nearby. Do not replant cause we don't care.
                Task t = pickupBlockTaskOrNull(mod, target.cropBlock, target.cropItem, (blockPos -> {
                    BlockState s = mod.getWorld().getBlockState(blockPos);
                    Block b = s.getBlock();
                    if (b instanceof CropBlock) {
                        boolean isWheat = !(b instanceof PotatoesBlock || b instanceof CarrotsBlock || b instanceof BeetrootsBlock);
                        if (isWheat) {
                            // Chunk needs to be loaded for wheat maturity to be checked.
                            if (!mod.getChunkTracker().isChunkLoaded(blockPos)) {
                                return false;
                            }
                            // Prune if we're not mature/fully grown wheat.
                            CropBlock crop = (CropBlock) b;
                            return crop.isMature(s);
                        }
                    }
                    // Unbreakable.
                    return WorldHelper.canBreak(mod, blockPos);
                    // We're not wheat so do NOT reject.
                }), 96);
                if (t != null) {
                    setDebugState("Harvesting " + target.cropItem.getTranslationKey());
                    _currentResourceTask = t;
                    return _currentResourceTask;
                }
            }
            // Cooked foods
            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                Predicate<Entity> notBaby = entity -> entity instanceof LivingEntity livingEntity && !livingEntity.isBaby();
                Optional<Entity> nearest = mod.getEntityTracker().getClosestEntity(notBaby, cookable.mobToKill);
                if (nearest.isPresent()) {
                    setDebugState("Killing " + nearest.get().getType().getTranslationKey());
                    _currentResourceTask = killTaskOrNull(nearest.get(), notBaby, cookable.getRaw());
                    return _currentResourceTask;
                }
//                if (nearest.isEmpty()) continue; // ?? This crashed once?
//                int hungerPerformance = cookable.getCookedUnits();
//                double sqDistance = nearest.get().squaredDistanceTo(mod.getPlayer());
//                double score = (double) 100 * hungerPerformance / (sqDistance);
//                if (cookable.isFish()) {
//                    score *= FISH_PENALTY;
//                }
//                if (score > bestScore) {
//                    bestScore = score;
//                    bestEntity = nearest.get();
//                    bestRawFood = cookable.getRaw();
//                }
            }

            // Sweet berries (separate from crops because they should have a lower priority than everything else cause they suck)
            Task berryPickup = pickupBlockTaskOrNull(mod, Blocks.SWEET_BERRY_BUSH, Items.SWEET_BERRIES, 96);
            if (berryPickup != null) {
                setDebugState("Getting sweet berries (no better foods are present)");
                _currentResourceTask = berryPickup;
                return _currentResourceTask;
            }
        }

        // Look for food.
        setDebugState("Searching...");
        return new TimeoutWanderTask();
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
        mod.getBlockTracker().stopTracking(Blocks.HAY_BLOCK);
        mod.getBlockTracker().stopTracking(Blocks.SWEET_BERRY_BUSH);
        for (CropTarget crop : CROPS) {
            mod.getBlockTracker().stopTracking(crop.cropBlock);
        }
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return StorageHelper.calculateInventoryFoodScore(mod) >= _unitsNeeded;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CollectFoodTask task) {
            return task._unitsNeeded == _unitsNeeded;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Collect " + _unitsNeeded + " units of food.";
    }

    /**
     * Returns a task that mines a block and picks up its output.
     * Returns null if task cannot reasonably run.
     */
    private Task pickupBlockTaskOrNull(AltoClef mod, Block blockToCheck, Item itemToGrab, Predicate<BlockPos> accept, double maxRange) {
        Predicate<BlockPos> acceptPlus = (blockPos) -> {
            if (!WorldHelper.canBreak(mod, blockPos)) return false;
            return accept.test(blockPos);
        };
        Optional<BlockPos> nearestBlock = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), acceptPlus, blockToCheck);

        if (nearestBlock.isPresent() && !nearestBlock.get().isWithinDistance(mod.getPlayer().getPos(), maxRange)) {
            nearestBlock = Optional.empty();
        }

        Optional<ItemEntity> nearestDrop = Optional.empty();
        if (mod.getEntityTracker().itemDropped(itemToGrab)) {
            nearestDrop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemToGrab);
        }
        boolean spotted = nearestBlock.isPresent() || nearestDrop.isPresent();
        // Collect hay until we have enough.
        if (spotted) {
            if (nearestDrop.isPresent()) {
                return new PickupDroppedItemTask(itemToGrab, Integer.MAX_VALUE);
            } else {
                return new DoToClosestBlockTask(DestroyBlockTask::new, acceptPlus, blockToCheck);
            }
        }
        return null;
    }

    private Task pickupBlockTaskOrNull(AltoClef mod, Block blockToCheck, Item itemToGrab, double maxRange) {
        return pickupBlockTaskOrNull(mod, blockToCheck, itemToGrab, toAccept -> true, maxRange);
    }

    private Task killTaskOrNull(Entity entity, Predicate<Entity> entityPredicate, Item itemToGrab) {
        return new KillAndLootTask(entity.getClass(), entityPredicate, new ItemTarget(itemToGrab, 1));
    }

    /**
     * Returns a task that picks up a dropped item.
     * Returns null if task cannot reasonably run.
     */
    private Task pickupTaskOrNull(AltoClef mod, Item itemToGrab, double maxRange) {
        Optional<ItemEntity> nearestDrop = Optional.empty();
        if (mod.getEntityTracker().itemDropped(itemToGrab)) {
            nearestDrop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemToGrab);
        }
        if (nearestDrop.isPresent()) {
            if (nearestDrop.get().isInRange(mod.getPlayer(), maxRange)) {
                return new PickupDroppedItemTask(new ItemTarget(itemToGrab), true);
            }
            //return new GetToBlockTask(nearestDrop.getBlockPos(), false);
        }
        return null;
    }

    private Task pickupTaskOrNull(AltoClef mod, Item itemToGrab) {
        return pickupTaskOrNull(mod, itemToGrab, Double.POSITIVE_INFINITY);
    }

    @SuppressWarnings("rawtypes")
    private static class CookableFoodTarget {
        public String rawFood;
        public String cookedFood;
        public Class mobToKill;

        public CookableFoodTarget(String rawFood, String cookedFood, Class mobToKill) {
            this.rawFood = rawFood;
            this.cookedFood = cookedFood;
            this.mobToKill = mobToKill;
        }

        public CookableFoodTarget(String rawFood, Class mobToKill) {
            this(rawFood, "cooked_" + rawFood, mobToKill);
        }

        private Item getRaw() {
            return Objects.requireNonNull(TaskCatalogue.getItemMatches(rawFood))[0];
        }

        private Item getCooked() {
            return Objects.requireNonNull(TaskCatalogue.getItemMatches(cookedFood))[0];
        }

        public int getCookedUnits() {
            assert getCooked().getComponents().get(DataComponentTypes.FOOD) != null;
            return Objects.requireNonNull(getCooked().getComponents().get(DataComponentTypes.FOOD)).nutrition();
        }

        public boolean isFish() {
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    private static class CookableFoodTargetFish extends CookableFoodTarget {

        public CookableFoodTargetFish(String rawFood, Class mobToKill) {
            super(rawFood, mobToKill);
        }

        @Override
        public boolean isFish() {
            return true;
        }
    }

    private static class CropTarget {
        public Item cropItem;
        public Block cropBlock;

        public CropTarget(Item cropItem, Block cropBlock) {
            this.cropItem = cropItem;
            this.cropBlock = cropBlock;
        }
    }
}
