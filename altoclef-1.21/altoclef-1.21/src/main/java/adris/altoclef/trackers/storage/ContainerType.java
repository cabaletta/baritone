package adris.altoclef.trackers.storage;

import adris.altoclef.util.slots.ChestSlot;
import adris.altoclef.util.slots.FurnaceSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.*;
import org.apache.commons.lang3.NotImplementedException;

public enum ContainerType {
    CHEST, ENDER_CHEST, SHULKER, FURNACE, BREWING, MISC, EMPTY;

    public static ContainerType getFromBlock(Block block) {
        if (block instanceof ChestBlock) {
            return CHEST;
        }
        if (block instanceof AbstractFurnaceBlock) {
            return FURNACE;
        }
        if (block.equals(Blocks.ENDER_CHEST)) {
            return ENDER_CHEST;
        }
        if (block instanceof ShulkerBoxBlock) {
            return SHULKER;
        }
        if (block instanceof BrewingStandBlock) {
            return BREWING;
        }
        if (block instanceof BarrelBlock || block instanceof DispenserBlock || block instanceof HopperBlock) {
            return MISC;
        }
        return EMPTY;
    }

    public static boolean screenHandlerMatches(ContainerType type, ScreenHandler handler) {
        switch (type) {
            case CHEST, ENDER_CHEST -> {
                return handler instanceof GenericContainerScreenHandler;
            }
            case SHULKER -> {
                return handler instanceof ShulkerBoxScreenHandler;
            }
            case FURNACE -> {
                return handler instanceof AbstractFurnaceScreenHandler;
            }
            case BREWING -> {
                return handler instanceof BrewingStandScreenHandler;
            }
            case MISC -> {
                return handler instanceof Generic3x3ContainerScreenHandler || handler instanceof GenericContainerScreenHandler;
            }
            case EMPTY -> {
                return false;
            }
            default -> throw new NotImplementedException("Missed this chest type: " + type);
        }
    }

    public static boolean screenHandlerMatches(ContainerType type) {
        if (MinecraftClient.getInstance().player != null) {
            ScreenHandler h = MinecraftClient.getInstance().player.currentScreenHandler;
            if (h != null)
                return screenHandlerMatches(type, h);
        }
        return false;
    }

    public static boolean screenHandlerMatchesAny() {
        return screenHandlerMatches(CHEST) ||
                screenHandlerMatches(SHULKER) ||
                screenHandlerMatches(FURNACE);
    }

    public static boolean slotTypeMatches(ContainerType type, Slot slot) {
        switch (type) {
            case CHEST, ENDER_CHEST, SHULKER -> {
                return slot instanceof ChestSlot;
            }
            case FURNACE -> {
                return slot instanceof FurnaceSlot;
            }
            case BREWING -> throw new NotImplementedException("Brewing slots not implemented yet.");
            case MISC -> {
                return true;
            }
            default -> throw new NotImplementedException("Missed this chest type: " + type);
        }
    }
}
