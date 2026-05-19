package adris.altoclef.tasks.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.container.DoStuffInContainerTask;
import adris.altoclef.tasks.container.LootContainerTask;
import adris.altoclef.tasks.container.SmeltInFurnaceTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.misc.LootDesertTempleTask;
import adris.altoclef.tasks.misc.PlaceBedAndSetSpawnTask;
import adris.altoclef.tasks.misc.SleepThroughNightTask;
import adris.altoclef.tasks.movement.*;
import adris.altoclef.tasks.resources.*;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.ConfigHelper;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("ALL")
public class BeatMinecraft2Task extends Task {

    private static final Block[] TRACK_BLOCKS = new Block[]{
            Blocks.END_PORTAL_FRAME,
            Blocks.END_PORTAL,
            Blocks.CRAFTING_TABLE, // For pearl trading + gold crafting
            Blocks.CHEST, // For ruined portals
            Blocks.SPAWNER, // For silverfish,
            Blocks.STONE_PRESSURE_PLATE // For desert temples
    };
    private static final Item[] COLLECT_EYE_ARMOR = new Item[]{
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS,
            Items.GOLDEN_BOOTS
    };
    private static final Item[] COLLECT_EYE_ARMOR_END = ItemHelper.DIAMOND_ARMORS;
    private static final ItemTarget[] COLLECT_EYE_GEAR = combine(
            toItemTargets(Items.DIAMOND_SWORD),
            toItemTargets(Items.DIAMOND_PICKAXE, 3),
            toItemTargets(Items.CRAFTING_TABLE)
    );
    private static final ItemTarget[] COLLECT_EYE_GEAR_MIN = combine(
            toItemTargets(Items.DIAMOND_SWORD),
            toItemTargets(Items.DIAMOND_PICKAXE, 1)
    );
    private static final ItemTarget[] IRON_GEAR = combine(
            toItemTargets(Items.IRON_SWORD),
            toItemTargets(Items.IRON_PICKAXE, 2)
    );
    private static final ItemTarget[] IRON_GEAR_MIN = combine(
            toItemTargets(Items.IRON_SWORD)
    );
    private static final int END_PORTAL_FRAME_COUNT = 12;
    private static final double END_PORTAL_BED_SPAWN_RANGE = 8;
    // We don't want curse of binding
    private static final Predicate<ItemStack> _noCurseOfBinding = stack -> {
        if (stack.getEnchantments().getEnchantments().contains(EnchantmentTags.CURSE)) {
            return false;
        }
        return true;
    };
    private static BeatMinecraftConfig _config;

    static {
        ConfigHelper.loadConfig("configs/beat_minecraft.json", BeatMinecraftConfig::new, BeatMinecraftConfig.class, newConfig -> _config = newConfig);
    }

    private final HashMap<Item, Integer> _cachedEndItemDrops = new HashMap<>();
    // For some reason, after death there's a frame where the game thinks there are NO items in the end.
    private final TimerGame _cachedEndItemNothingWaitTime = new TimerGame(2);
    private final Task _buildMaterialsTask;
    private final PlaceBedAndSetSpawnTask _setBedSpawnTask = new PlaceBedAndSetSpawnTask();
    private final GoToStrongholdPortalTask _locateStrongholdTask;
    private final Task _goToNetherTask = new DefaultGoToDimensionTask(Dimension.NETHER); // To keep the portal build cache.
    private final Task _getOneBedTask = TaskCatalogue.getItemTask("bed", 1);
    private final Task _sleepThroughNightTask = new SleepThroughNightTask();
    private final Task _killDragonBedStratsTask = new KillEnderDragonWithBedsTask(new WaitForDragonAndPearlTask());
    // End specific dragon breath avoidance
    private final DragonBreathTracker _dragonBreathTracker = new DragonBreathTracker();
    private BlockPos _endPortalCenterLocation;
    private boolean _ranStrongholdLocator;
    private boolean _endPortalOpened;
    private BlockPos _bedSpawnLocation;
    private List<BlockPos> _notRuinedPortalChests = new ArrayList<>();
    private int _cachedFilledPortalFrames = 0;
    // Controls whether we CAN walk on the end portal.
    private boolean _enterindEndPortal = false;
    private Task _foodTask;
    private Task _gearTask;
    private Task _lootTask;
    private boolean _collectingEyes;
    private boolean _escapingDragonsBreath;

    public BeatMinecraft2Task() {
        _locateStrongholdTask = new GoToStrongholdPortalTask(_config.targetEyes);
        _buildMaterialsTask = new GetBuildingMaterialsTask(_config.buildMaterialCount);
    }

    public static BeatMinecraftConfig getConfig() {
        return _config;
    }

    private static List<BlockPos> getFrameBlocks(BlockPos endPortalCenter) {
        Vec3i[] frameOffsets = new Vec3i[]{
                new Vec3i(2, 0, 1),
                new Vec3i(2, 0, 0),
                new Vec3i(2, 0, -1),
                new Vec3i(-2, 0, 1),
                new Vec3i(-2, 0, 0),
                new Vec3i(-2, 0, -1),
                new Vec3i(1, 0, 2),
                new Vec3i(0, 0, 2),
                new Vec3i(-1, 0, 2),
                new Vec3i(1, 0, -2),
                new Vec3i(0, 0, -2),
                new Vec3i(-1, 0, -2)
        };
        return Arrays.stream(frameOffsets).map(endPortalCenter::add).toList();
    }

    private static boolean isEndPortalFrameFilled(AltoClef mod, BlockPos pos) {
        if (!mod.getChunkTracker().isChunkLoaded(pos))
            return false;
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.getBlock() != Blocks.END_PORTAL_FRAME) {
            Debug.logWarning("BLOCK POS " + pos + " DOES NOT CONTAIN END PORTAL FRAME! This is probably due to a bug/incorrect assumption.");
            return false;
        }
        return state.get(EndPortalFrameBlock.EYE);
    }

    // Just a helpful utility to reduce reuse recycle.
    private static boolean shouldForce(AltoClef mod, Task task) {
        return task != null && task.isActive() && !task.isFinished(mod);
    }

    private static ItemTarget[] toItemTargets(Item... items) {
        return Arrays.stream(items).map(item -> new ItemTarget(item, 1)).toArray(ItemTarget[]::new);
    }

    private static ItemTarget[] toItemTargets(Item item, int count) {
        return new ItemTarget[]{new ItemTarget(item, count)};
    }

    private static ItemTarget[] combine(ItemTarget[]... targets) {
        List<ItemTarget> result = new ArrayList<>();
        for (ItemTarget[] ts : targets) {
            result.addAll(Arrays.asList(ts));
        }
        return result.toArray(ItemTarget[]::new);
    }

    @Override
    protected void onStart(AltoClef mod) {

        // Add a warning to make sure the user at least knows to change the settings.
        String settingsWarningTail = "in \".minecraft/altoclef_settings.json\". @gamer may break if you don't add this! (sorry!)";
        if (!ArrayUtils.contains(mod.getModSettings().getThrowawayItems(mod), Items.END_STONE)) {
            Debug.logWarning("\"end_stone\" is not part of your \"throwawayItems\" list " + settingsWarningTail);
        }
        if (!mod.getModSettings().shouldThrowawayUnusedItems()) {
            Debug.logWarning("\"throwawayUnusedItems\" is not set to true " + settingsWarningTail);
        }

        mod.getBlockTracker().trackBlock(TRACK_BLOCKS);
        mod.getBlockTracker().trackBlock(ItemHelper.itemsToBlocks(ItemHelper.BED));
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(Items.ENDER_EYE, Items.BLAZE_ROD, Items.ENDER_PEARL, Items.CRAFTING_TABLE);
        mod.getBehaviour().addProtectedItems(ItemHelper.BED);
        // Allow walking on end portal
        mod.getBehaviour().allowWalkingOn(blockPos -> _enterindEndPortal && mod.getChunkTracker().isChunkLoaded(blockPos) && mod.getWorld().getBlockState(blockPos).getBlock() == Blocks.END_PORTAL);

        // Avoid dragon breath
        mod.getBehaviour().avoidWalkingThrough(blockPos -> {
            return WorldHelper.getCurrentDimension() == Dimension.END && !_escapingDragonsBreath && _dragonBreathTracker.isTouchingDragonBreath(blockPos);
        });

        // Don't break the bed we placed near the end portal
        mod.getBehaviour().avoidBlockBreaking(blockPos -> {
            if (_bedSpawnLocation != null) {
                return blockPos.equals(WorldHelper.getBedHead(mod, _bedSpawnLocation)) || blockPos.equals(WorldHelper.getBedFoot(mod, _bedSpawnLocation));
            }
            return false;
        });
    }

    @Override
    protected Task onTick(AltoClef mod) {
        /*
        if in the overworld:
          if end portal found:
            if end portal opened:
              @make sure we have iron gear and enough beds to kill the dragon first, considering whether that gear was dropped in the end
              @enter end portal
            else if we have enough eyes of ender:
              @fill in the end portal
          else if we have enough eyes of ender:
            @locate the end portal
          else:
            if we don't have diamond gear:
              if we have no food:
                @get a little bit of food
              @get diamond gear
            @go to the nether
        if in the nether:
          if we don't have enough blaze rods:
            @kill blazes till we do
          else if we don't have enough pearls:
            @kill enderman till we do
          else:
            @leave the nether
        if in the end:
          if we have a bed:
            @do bed strats
          else:
            @just hit the dragon normally
         */

        // By default, don't walk over end portals.
        _enterindEndPortal = false;

        Predicate<Task> isCraftingTableTask = task -> {
            if (task instanceof DoStuffInContainerTask cont) {
                return cont.getContainerTarget().matches(Items.CRAFTING_TABLE);
            }
            return false;
        };

        // Portable crafting table.
        // If we're NOT using our crafting table right now and there's one nearby, grab it.
        if (!_endPortalOpened && WorldHelper.getCurrentDimension() != Dimension.END && _config.rePickupCraftingTable && !mod.getItemStorage().hasItem(Items.CRAFTING_TABLE) && !thisOrChildSatisfies(isCraftingTableTask)
                && (mod.getBlockTracker().anyFound(blockPos -> WorldHelper.canBreak(mod, blockPos), Blocks.CRAFTING_TABLE)
                || mod.getEntityTracker().itemDropped(Items.CRAFTING_TABLE))) {
            setDebugState("Pick up crafting table while we're at it");
            return new MineAndCollectTask(Items.CRAFTING_TABLE, 1, new Block[]{Blocks.CRAFTING_TABLE}, MiningRequirement.HAND);
        }

        // End stuff.
        if (WorldHelper.getCurrentDimension() == Dimension.END) {

            // Dragons breath avoidance
            _dragonBreathTracker.updateBreath(mod);
            for (BlockPos playerIn : WorldHelper.getBlocksTouchingPlayer(mod)) {
                if (_dragonBreathTracker.isTouchingDragonBreath(playerIn)) {
                    setDebugState("ESCAPE dragons breath");
                    _escapingDragonsBreath = true;
                    return _dragonBreathTracker.getRunAwayTask();
                }
            }
            _escapingDragonsBreath = false;

            // If we find an ender portal, just GO to it!!!
            if (mod.getBlockTracker().anyFound(Blocks.END_PORTAL)) {
                setDebugState("WOOHOO");
                _enterindEndPortal = true;
                return new DoToClosestBlockTask(
                        blockPos -> new GetToBlockTask(blockPos.up()),
                        Blocks.END_PORTAL
                );
            }

            // If we have bed, do bed strats, otherwise punk normally.
            updateCachedEndItems(mod);
            // Grab beds
            if (mod.getEntityTracker().itemDropped(ItemHelper.BED) && mod.getItemStorage().getItemCount(ItemHelper.BED) < _config.requiredBeds)
                return new PickupDroppedItemTask(new ItemTarget(ItemHelper.BED), true);
            // Grab tools
            if (!mod.getItemStorage().hasItem(Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE)) {
                if (mod.getEntityTracker().itemDropped(Items.IRON_PICKAXE))
                    return new PickupDroppedItemTask(Items.IRON_PICKAXE, 1);
                if (mod.getEntityTracker().itemDropped(Items.DIAMOND_PICKAXE))
                    return new PickupDroppedItemTask(Items.DIAMOND_PICKAXE, 1);
            }
            if (!mod.getItemStorage().hasItem(Items.WATER_BUCKET) && mod.getEntityTracker().itemDropped(Items.WATER_BUCKET))
                return new PickupDroppedItemTask(Items.WATER_BUCKET, 1);
            // Grab armor
            for (Item armorCheck : COLLECT_EYE_ARMOR_END) {
                if (!StorageHelper.isArmorEquipped(mod, armorCheck)) {
                    if (mod.getItemStorage().hasItem(armorCheck)) {
                        return new EquipArmorTask(armorCheck);
                    }
                    if (mod.getEntityTracker().itemDropped(armorCheck)) {
                        return new PickupDroppedItemTask(armorCheck, 1);
                    }
                }
            }
            if (mod.getItemStorage().hasItem(ItemHelper.BED) || (_killDragonBedStratsTask.isActive() && !_killDragonBedStratsTask.isFinished(mod))) {
                setDebugState("Bed strats");
                return _killDragonBedStratsTask;
            }
            setDebugState("No beds, regular strats.");
            return new KillEnderDragonTask();
        } else {
            // We're not in the end so reset our "end cache" timer
            _cachedEndItemNothingWaitTime.reset();
        }

        // Check for end portals. Always.
        if (!endPortalOpened(mod, _endPortalCenterLocation) && WorldHelper.getCurrentDimension() == Dimension.OVERWORLD) {
            Optional<BlockPos> endPortal = mod.getBlockTracker().getNearestTracking(Blocks.END_PORTAL);
            if (endPortal.isPresent()) {
                _endPortalCenterLocation = endPortal.get();
                _endPortalOpened = true;
            } else {
                // TODO: Test that this works, for some reason the bot gets stuck near the stronghold and it keeps "Searching" for the portal
                _endPortalCenterLocation = doSimpleSearchForEndPortal(mod);
            }
        }

        // Sleep through night.
        if (_config.sleepThroughNight && !_endPortalOpened && WorldHelper.getCurrentDimension() == Dimension.OVERWORLD) {
            if (WorldHelper.canSleep()) {
                setDebugState("Sleeping through night");
                return _sleepThroughNightTask;
            }
            if (!mod.getItemStorage().hasItem(ItemHelper.BED)) {
                if (mod.getBlockTracker().anyFound(blockPos -> WorldHelper.canBreak(mod, blockPos), ItemHelper.itemsToBlocks(ItemHelper.BED))
                        || shouldForce(mod, _getOneBedTask)) {
                    setDebugState("Grabbing a bed we found to sleep through the night.");
                    return _getOneBedTask;
                }
            }
        }

        // Do we need more eyes?
        boolean noEyesPlease = (endPortalOpened(mod, _endPortalCenterLocation) || WorldHelper.getCurrentDimension() == Dimension.END);
        int filledPortalFrames = getFilledPortalFrames(mod, _endPortalCenterLocation);
        int eyesNeededMin = noEyesPlease ? 0 : _config.minimumEyes - filledPortalFrames;
        int eyesNeeded = noEyesPlease ? 0 : _config.targetEyes - filledPortalFrames;
        int eyes = mod.getItemStorage().getItemCount(Items.ENDER_EYE);
        if (eyes < eyesNeededMin || (!_ranStrongholdLocator && _collectingEyes && eyes < eyesNeeded)) {
            _collectingEyes = true;
            return getEyesOfEnderTask(mod, eyesNeeded);
        } else {
            _collectingEyes = false;
        }

        // We have eyes. Locate our portal + enter.
        switch (WorldHelper.getCurrentDimension()) {
            case OVERWORLD -> {
                // If we found our end portal...
                if (endPortalFound(mod, _endPortalCenterLocation)) {

                    // Destroy silverfish spawner
                    if (StorageHelper.miningRequirementMetInventory(mod, MiningRequirement.WOOD)) {
                        Optional<BlockPos> silverfish = mod.getBlockTracker().getNearestTracking(blockPos -> {
                            return WorldHelper.getSpawnerEntity(mod, blockPos) instanceof SilverfishEntity;
                        }, Blocks.SPAWNER);
                        if (silverfish.isPresent()) {
                            return new DestroyBlockTask(silverfish.get());
                        }
                    }

                    // Get remaining beds.
                    if (needsBeds(mod)) {
                        setDebugState("Collecting beds.");
                        return getBedTask(mod);
                    }
                    if (_config.placeSpawnNearEndPortal) {
                        if (!spawnSetNearPortal(mod, _endPortalCenterLocation)) {
                            setDebugState("Setting spawn near end portal");
                            return setSpawnNearPortalTask(mod);
                        }
                    }
                    if (endPortalOpened(mod, _endPortalCenterLocation)) {
                        // Does our (current inventory) + (end dropped items inventory) satisfy (base requirements)?
                        //      If not, obtain (base requirements) - (end dropped items).
                        setDebugState("Getting equipment for End");
                        if (!hasItemOrDroppedInEnd(mod, Items.IRON_SWORD) && !hasItemOrDroppedInEnd(mod, Items.DIAMOND_SWORD)) {
                            return TaskCatalogue.getItemTask(Items.IRON_SWORD, 1);
                        }
                        if (!hasItemOrDroppedInEnd(mod, Items.WATER_BUCKET)) {
                            return TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1);
                        }
                        if (!hasItemOrDroppedInEnd(mod, Items.IRON_PICKAXE) && !hasItemOrDroppedInEnd(mod, Items.DIAMOND_PICKAXE)) {
                            return TaskCatalogue.getItemTask(Items.DIAMOND_PICKAXE, 1);
                        }
                        if (needsBuildingMaterials(mod)) {
                            return _buildMaterialsTask;
                        }

                        // We're as ready as we'll ever be, hop into the portal!
                        setDebugState("Entering End");
                        _enterindEndPortal = true;
                        return new DoToClosestBlockTask(
                                blockPos -> new GetToBlockTask(blockPos.up()),
                                Blocks.END_PORTAL
                        );
                    } else {

                        // Open the portal! (we have enough eyes, do it)
                        setDebugState("Opening End Portal");
                        return new DoToClosestBlockTask(
                                blockPos -> new InteractWithBlockTask(Items.ENDER_EYE, blockPos),
                                blockPos -> !isEndPortalFrameFilled(mod, blockPos),
                                Blocks.END_PORTAL_FRAME
                        );
                    }
                } else {
                    // Get beds before starting our portal location.
                    if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD && needsBeds(mod)) {
                        setDebugState("Getting beds before stronghold search.");
                        return getBedTask(mod);
                    }
                    // Portal Location
                    setDebugState("Locating End Portal...");
                    _ranStrongholdLocator = true;
                    return _locateStrongholdTask;
                }
            }
            case NETHER -> {
                // Portal Location
                setDebugState("Locating End Portal...");
                if (needsBuildingMaterials(mod)) {
                    return _buildMaterialsTask;
                }
                return _locateStrongholdTask;
            }
        }

        return null;
    }

    private boolean needsBuildingMaterials(AltoClef mod) {
        return StorageHelper.getBuildingMaterialCount(mod) < _config.minBuildMaterialCount || shouldForce(mod, _buildMaterialsTask);
    }

    private void updateCachedEndItems(AltoClef mod) {
        List<ItemEntity> droppedItems = mod.getEntityTracker().getDroppedItems();
        // If we have no items, it COULD be because we're dead. Wait a little.
        if (droppedItems.isEmpty()) {
            if (!_cachedEndItemNothingWaitTime.elapsed()) {
                return;
            }
        } else {
            _cachedEndItemNothingWaitTime.reset();
        }
        _cachedEndItemDrops.clear();
        for (ItemEntity entity : droppedItems) {
            Item item = entity.getStack().getItem();
            int count = entity.getStack().getCount();
            _cachedEndItemDrops.put(item, _cachedEndItemDrops.getOrDefault(item, 0) + count);
        }
    }

    private int getEndCachedCount(Item item) {
        return _cachedEndItemDrops.getOrDefault(item, 0);
    }

    private boolean droppedInEnd(Item item) {
        return getEndCachedCount(item) > 0;
    }

    private boolean hasItemOrDroppedInEnd(AltoClef mod, Item item) {
        return mod.getItemStorage().hasItem(item) || droppedInEnd(item);
    }

    private List<Item> lootableItems(AltoClef mod) {
        List<Item> lootable = new ArrayList<>();
        lootable.add(Items.GOLDEN_APPLE);
        lootable.add(Items.ENCHANTED_GOLDEN_APPLE);
        lootable.add(Items.GLISTERING_MELON_SLICE);
        lootable.add(Items.GOLDEN_CARROT);
        lootable.add(Items.OBSIDIAN);
        if (!StorageHelper.isArmorEquipped(mod, Items.GOLDEN_BOOTS) && !mod.getItemStorage().hasItemInventoryOnly(Items.GOLDEN_BOOTS)) {
            lootable.add(Items.GOLDEN_BOOTS);
        }
        if ((mod.getItemStorage().getItemCountInventoryOnly(Items.GOLD_INGOT) < 4 && !StorageHelper.isArmorEquipped(mod, Items.GOLDEN_BOOTS) && !mod.getItemStorage().hasItemInventoryOnly(Items.GOLDEN_BOOTS)) || _config.barterPearlsInsteadOfEndermanHunt) {
            lootable.add(Items.GOLD_INGOT);
        }
        if (!mod.getItemStorage().hasItemInventoryOnly(Items.FLINT_AND_STEEL)) {
            lootable.add(Items.FLINT_AND_STEEL);
            if (!mod.getItemStorage().hasItemInventoryOnly(Items.FIRE_CHARGE)) {
                lootable.add(Items.FIRE_CHARGE);
            }
        }
        if (!mod.getItemStorage().hasItemInventoryOnly(Items.BUCKET) && !mod.getItemStorage().hasItemInventoryOnly(Items.WATER_BUCKET)) {
            lootable.add(Items.IRON_INGOT);
        }
        if (!StorageHelper.itemTargetsMetInventory(mod, COLLECT_EYE_GEAR_MIN)) {
            lootable.add(Items.DIAMOND);
        }
        if (!mod.getItemStorage().hasItemInventoryOnly(Items.FLINT)) {
            lootable.add(Items.FLINT);
        }
        return lootable;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(TRACK_BLOCKS);
        mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(ItemHelper.BED));
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof BeatMinecraft2Task;
    }

    @Override
    protected String toDebugString() {
        return "Beating the Game.";
    }

    private boolean endPortalFound(AltoClef mod, BlockPos endPortalCenter) {
        if (endPortalCenter == null) {
            return false;
        }
        if (endPortalOpened(mod, endPortalCenter)) {
            return true;
        }
        return getFrameBlocks(endPortalCenter).stream().allMatch(frame -> mod.getBlockTracker().blockIsValid(frame, Blocks.END_PORTAL_FRAME));
    }

    private boolean endPortalOpened(AltoClef mod, BlockPos endPortalCenter) {
        return _endPortalOpened && endPortalCenter != null && mod.getBlockTracker().blockIsValid(endPortalCenter, Blocks.END_PORTAL);
    }

    private boolean spawnSetNearPortal(AltoClef mod, BlockPos endPortalCenter) {
        return _bedSpawnLocation != null && mod.getBlockTracker().blockIsValid(_bedSpawnLocation, ItemHelper.itemsToBlocks(ItemHelper.BED));
    }

    private int getFilledPortalFrames(AltoClef mod, BlockPos endPortalCenter) {
        // If we have our end portal, this doesn't matter.
        if (endPortalFound(mod, endPortalCenter)) {
            return END_PORTAL_FRAME_COUNT;
        }
        if (endPortalFound(mod, endPortalCenter)) {
            List<BlockPos> frameBlocks = getFrameBlocks(endPortalCenter);
            // If EVERY portal frame is loaded, consider updating our cached filled portal count.
            if (frameBlocks.stream().allMatch(blockPos -> mod.getChunkTracker().isChunkLoaded(blockPos))) {
                _cachedFilledPortalFrames = frameBlocks.stream().reduce(0, (count, blockPos) ->
                                count + (isEndPortalFrameFilled(mod, blockPos) ? 1 : 0),
                        Integer::sum);
            }
            return _cachedFilledPortalFrames;
        }
        return 0;
    }

    private boolean canBeLootablePortalChest(AltoClef mod, BlockPos blockPos) {
        if (mod.getWorld().getBlockState(blockPos.up(1)).getBlock() == Blocks.WATER || blockPos.getY() < 50) {
            return false;
        }
        for (BlockPos check : WorldHelper.scanRegion(mod, blockPos.add(-4, -2, -4), blockPos.add(4, 2, 4))) {
            if (mod.getWorld().getBlockState(check).getBlock() == Blocks.NETHERRACK) {
                return true;
            }
        }
        _notRuinedPortalChests.add(blockPos);
        return false;
    }

    private Optional<BlockPos> locateClosestUnopenedRuinedPortalChest(AltoClef mod) {
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            return Optional.empty();
        }
        return mod.getBlockTracker().getNearestTracking(blockPos -> !_notRuinedPortalChests.contains(blockPos) && WorldHelper.isUnopenedChest(mod, blockPos) && mod.getPlayer().getBlockPos().isWithinDistance(blockPos, 150) && canBeLootablePortalChest(mod, blockPos), Blocks.CHEST);
    }

    private Task getEyesOfEnderTask(AltoClef mod, int targetEyes) {
        if (mod.getEntityTracker().itemDropped(Items.ENDER_EYE)) {
            setDebugState("Picking up Dropped Eyes");
            return new PickupDroppedItemTask(Items.ENDER_EYE, targetEyes);
        }

        int eyeCount = mod.getItemStorage().getItemCount(Items.ENDER_EYE);

        int blazePowderCount = mod.getItemStorage().getItemCount(Items.BLAZE_POWDER);
        int blazeRodCount = mod.getItemStorage().getItemCount(Items.BLAZE_ROD);
        int blazeRodTarget = (int) Math.ceil(((double) targetEyes - eyeCount - blazePowderCount) / 2.0);
        int enderPearlTarget = targetEyes - eyeCount;
        boolean needsBlazeRods = blazeRodCount < blazeRodTarget;
        boolean needsBlazePowder = eyeCount + blazePowderCount < targetEyes;
        boolean needsEnderPearls = mod.getItemStorage().getItemCount(Items.ENDER_PEARL) < enderPearlTarget;

        if (needsBlazePowder && !needsBlazeRods) {
            // We have enough blaze rods.
            setDebugState("Crafting blaze powder");
            return TaskCatalogue.getItemTask(Items.BLAZE_POWDER, targetEyes - eyeCount);
        }

        if (!needsBlazePowder && !needsEnderPearls) {
            // Craft ender eyes
            setDebugState("Crafting Ender Eyes");
            return TaskCatalogue.getItemTask(Items.ENDER_EYE, targetEyes);
        }

        // Get blaze rods + pearls...
        switch (WorldHelper.getCurrentDimension()) {
            case OVERWORLD -> {
                // Make sure we have gear, then food.
                for (Item diamond : COLLECT_EYE_ARMOR) {
                    if (mod.getItemStorage().hasItem(diamond) && !StorageHelper.isArmorEquipped(mod, diamond)) {
                        return new EquipArmorTask(COLLECT_EYE_ARMOR);
                    }
                }
                if (shouldForce(mod, _lootTask)) {
                    return _lootTask;
                }
                if (_config.searchRuinedPortals) {
                    // Check for ruined portals
                    Optional<BlockPos> chest = locateClosestUnopenedRuinedPortalChest(mod);
                    if (chest.isPresent()) {
                        setDebugState("Looting ruined portal chest for goodies");
                        _lootTask = new LootContainerTask(chest.get(), lootableItems(mod), _noCurseOfBinding);
                        return _lootTask;
                    }
                }
                if (_config.searchDesertTemples && StorageHelper.miningRequirementMetInventory(mod, MiningRequirement.WOOD)) {
                    // Check for desert temples
                    BlockPos temple = WorldHelper.getADesertTemple(mod);
                    if (temple != null) {
                        setDebugState("Looting desert temple for goodies");
                        _lootTask = new LootDesertTempleTask(temple, lootableItems(mod));
                        return _lootTask;
                    }
                }
                if (shouldForce(mod, _gearTask) && !StorageHelper.isArmorEquippedAll(mod, COLLECT_EYE_ARMOR)) {
                    setDebugState("Getting gear for Ender Eye journey");
                    return _gearTask;
                }
                if (shouldForce(mod, _foodTask)) {
                    setDebugState("Getting Food for Ender Eye journey");
                    return _foodTask;
                }
                // Smelt remaining raw food
                if (_config.alwaysCookRawFood) {
                    for (Item raw : ItemHelper.RAW_FOODS) {
                        if (mod.getItemStorage().hasItem(raw)) {
                            Optional<Item> cooked = ItemHelper.getCookedFood(raw);
                            if (cooked.isPresent()) {
                                int targetCount = mod.getItemStorage().getItemCount(cooked.get()) + mod.getItemStorage().getItemCount(raw);
                                setDebugState("Smelting raw food: " + ItemHelper.stripItemName(raw));
                                return new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(cooked.get(), targetCount), new ItemTarget(raw, targetCount)));
                            }
                        }
                    }
                }

                boolean eyeGearSatisfied = StorageHelper.itemTargetsMet(mod, COLLECT_EYE_GEAR_MIN) && StorageHelper.isArmorEquippedAll(mod, COLLECT_EYE_ARMOR);
                // Start with iron
                if (!StorageHelper.itemTargetsMet(mod, IRON_GEAR_MIN) && !eyeGearSatisfied) {
                    _gearTask = TaskCatalogue.getSquashedItemTask(IRON_GEAR);
                    return _gearTask;
                }

                // If we happen to find beds...
                if (needsBeds(mod) && anyBedsFound(mod)) {
                    setDebugState("A bed was found, grabbing that first.");
                    return getBedTask(mod);
                }

                // Then get food
                if (StorageHelper.calculateInventoryFoodScore(mod) < _config.minFoodUnits) {
                    _foodTask = new CollectFoodTask(_config.foodUnits);
                    return _foodTask;
                }

                // Then get diamond
                if (!eyeGearSatisfied) {
                    _gearTask = TaskCatalogue.getSquashedItemTask(Stream.concat(Arrays.stream(COLLECT_EYE_ARMOR).filter(item -> !mod.getItemStorage().hasItem(item) && !StorageHelper.isArmorEquipped(mod, item)).map(item -> new ItemTarget(item, 1)), Arrays.stream(COLLECT_EYE_GEAR)).toArray(ItemTarget[]::new));
                    return _gearTask;
                }
                // Then go to the nether.
                setDebugState("Going to Nether");
                return _goToNetherTask;
            }
            case NETHER -> {
                if (needsBlazeRods) {
                    setDebugState("Getting Blaze Rods");
                    return getBlazeRodsTask(mod, blazeRodTarget);
                }
                setDebugState("Getting Ender Pearls");
                return getEnderPearlTask(mod, enderPearlTarget);
            }
            case END -> throw new UnsupportedOperationException("You're in the end. Don't collect eyes here.");
        }
        return null;
    }

    private Task setSpawnNearPortalTask(AltoClef mod) {
        if (_setBedSpawnTask.isSpawnSet()) {
            _bedSpawnLocation = _setBedSpawnTask.getBedSleptPos();
        } else {
            _bedSpawnLocation = null;
        }
        if (shouldForce(mod, _setBedSpawnTask)) {
            // Set spawnpoint and set our bed spawn when it happens.
            setDebugState("Setting spawnpoint now.");
            return _setBedSpawnTask;
        }
        // Get close to portal. If we're close enough, set our bed spawn somewhere nearby.
        if (WorldHelper.inRangeXZ(mod.getPlayer(), WorldHelper.toVec3d(_endPortalCenterLocation), END_PORTAL_BED_SPAWN_RANGE)) {
            return _setBedSpawnTask;
        } else {
            setDebugState("Approaching portal (to set spawnpoint)");
            return new GetToXZTask(_endPortalCenterLocation.getX(), _endPortalCenterLocation.getZ());
        }
    }

    private Task getBlazeRodsTask(AltoClef mod, int count) {
        if (mod.getEntityTracker().itemDropped(Items.BLAZE_POWDER)) {
            return new PickupDroppedItemTask(Items.BLAZE_POWDER, 1);
        }
        return new CollectBlazeRodsTask(count);
    }

    private Task getEnderPearlTask(AltoClef mod, int count) {
        if (_config.barterPearlsInsteadOfEndermanHunt) {
            // Equip golden boots before trading...
            if (!StorageHelper.isArmorEquipped(mod, Items.GOLDEN_BOOTS)) {
                return new EquipArmorTask(Items.GOLDEN_BOOTS);
            }
            int goldBuffer = 32;
            return new TradeWithPiglinsTask(32, Items.ENDER_PEARL, count);
        } else {
            if (mod.getEntityTracker().entityFound(EndermanEntity.class) || mod.getEntityTracker().itemDropped(Items.ENDER_PEARL)) {
                return new KillAndLootTask(EndermanEntity.class, new ItemTarget(Items.ENDER_PEARL, count));
            }
            // Search for warped forests this way...
            return new SearchChunkForBlockTask(Blocks.WARPED_NYLIUM);
        }
    }

    private int getTargetBeds(AltoClef mod) {
        boolean needsToSetSpawn = _config.placeSpawnNearEndPortal &&
                (
                        !spawnSetNearPortal(mod, _endPortalCenterLocation)
                                && !shouldForce(mod, _setBedSpawnTask)
                );
        int bedsInEnd = 0;
        for (Item bed : ItemHelper.BED) {
            bedsInEnd += _cachedEndItemDrops.getOrDefault(bed, 0);
        }

        return _config.requiredBeds + (needsToSetSpawn ? 1 : 0) - bedsInEnd;
    }

    private boolean needsBeds(AltoClef mod) {
        int inEnd = 0;
        for (Item item : ItemHelper.BED) {
            inEnd += _cachedEndItemDrops.getOrDefault(item, 0);
        }
        return (mod.getItemStorage().getItemCount(ItemHelper.BED) + inEnd) < getTargetBeds(mod);
    }

    private Task getBedTask(AltoClef mod) {
        int targetBeds = getTargetBeds(mod);
        // Collect beds. If we want to set our spawn, collect 1 more.
        setDebugState("Collecting " + targetBeds + " beds");
        if (!mod.getItemStorage().hasItem(Items.SHEARS) && !anyBedsFound(mod)) {
            return TaskCatalogue.getItemTask(Items.SHEARS, 1);
        }
        return TaskCatalogue.getItemTask("bed", targetBeds);
    }

    private boolean anyBedsFound(AltoClef mod) {
        return mod.getBlockTracker().anyFound(ItemHelper.itemsToBlocks(ItemHelper.BED));
    }

    private BlockPos doSimpleSearchForEndPortal(AltoClef mod) {
        List<BlockPos> frames = mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME);
        if (frames.size() >= END_PORTAL_FRAME_COUNT) {
            // Get the center of the frames.
            Vec3d average = frames.stream()
                    .reduce(Vec3d.ZERO, (accum, bpos) -> accum.add(bpos.getX() + 0.5, bpos.getY() + 0.5, bpos.getZ() + 0.5), Vec3d::add)
                    .multiply(1.0f / frames.size());
            return new BlockPos((int) average.x, (int) average.y, (int) average.z);
        }
        return null;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return MinecraftClient.getInstance().currentScreen instanceof CreditsScreen;
    }
}