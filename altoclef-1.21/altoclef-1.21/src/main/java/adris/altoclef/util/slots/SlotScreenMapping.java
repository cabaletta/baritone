package adris.altoclef.util.slots;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

@SuppressWarnings("rawtypes")
public class SlotScreenMapping {

    // Order here matters as whoever returns "true" in the predicate first is picked.
    private static final List<SlotScreenMappingEntry> _classList = List.of(
            e(CraftingTableSlot.class, screen -> screen instanceof CraftingScreen, CraftingTableSlot::new),
            e(FurnaceSlot.class, screen -> screen instanceof AbstractFurnaceScreen, FurnaceSlot::new),
            e(SmokerSlot.class, screen -> screen instanceof AbstractFurnaceScreen, SmokerSlot::new),
            e(BlastFurnaceSlot.class, screen -> screen instanceof AbstractFurnaceScreen, BlastFurnaceSlot::new),
            e(SmithingTableSlot.class, screen -> screen instanceof SmithingScreen, SmithingTableSlot::new),
            e(BrewingStandSlot.class, screen -> screen instanceof BrewingStandScreen, BrewingStandSlot::new),
            e(ChestSlot.class, screen -> screen instanceof GenericContainerScreen, ChestSlot::new),
            e(PlayerSlot.class, screen -> true, PlayerSlot::new), // Order matters, leave this BEFORE the BACK!
            e(CursorSlot.class, screen -> true, (slot, inv) -> CursorSlot.SLOT) // Order matters, leave this in the BACK!
    );

    @SuppressWarnings("unchecked")
    public static boolean isScreenOpen(Class slotType) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (!_classList.isEmpty()) {
            for (SlotScreenMappingEntry entry : _classList) {
                if (slotType == entry.type || slotType.isAssignableFrom(entry.type)) {
                    return entry.inScreen.test(screen);
                }
            }
        }
        throw new NotImplementedException("Slot type class not registered in SlotScreenMapping: " + slotType + ". Please register! (current screen = " + screen + ")");
    }

    public static Slot getFromScreen(int slot, boolean inventory) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (!_classList.isEmpty()) {
            for (SlotScreenMappingEntry entry : _classList) {
                if (entry.inScreen.test(screen)) {
                    return entry.getSlot.apply(slot, inventory);
                }
            }
        }
        throw new NotImplementedException("We should never get here, _classList should be filled with a predicate that always returns true at the bottom (for PlayerSlot & CursorSlot)");
    }


    private static SlotScreenMappingEntry e(Class type, Predicate<Screen> inScreen, BiFunction<Integer, Boolean, Slot> getSlot) {
        return new SlotScreenMappingEntry(type, inScreen, getSlot);
    }

    static class SlotScreenMappingEntry {
        public Class type;
        public Predicate<Screen> inScreen;
        public BiFunction<Integer, Boolean, Slot> getSlot;

        public SlotScreenMappingEntry(Class type, Predicate<Screen> inScreen, BiFunction<Integer, Boolean, Slot> getSlot) {
            this.type = type;
            this.inScreen = inScreen;
            this.getSlot = getSlot;
        }
    }
}
