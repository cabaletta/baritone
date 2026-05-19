package adris.altoclef.util;

import adris.altoclef.Debug;
import net.minecraft.item.Item;

import java.util.Arrays;

public class CraftingRecipe {

    private ItemTarget[] _slots;

    private int _width, _height;

    private boolean _shapeless;

    private String _shortName;

    private int _outputCount;

    // Every item in this list MUST match.
    // Used for beds where the wood can be anything
    // but the wool MUST be the same color.
    //private final Set<Integer> _mustMatch = new HashSet<>();

    private CraftingRecipe() {
    }

    public static CraftingRecipe newShapedRecipe(Item[][] items, int outputCount) {
        return newShapedRecipe(null, items, outputCount);
    }

    public static CraftingRecipe newShapedRecipe(ItemTarget[] slots, int outputCount) {
        return newShapedRecipe(null, slots, outputCount);
    }

    public static CraftingRecipe newShapedRecipe(String shortName, Item[][] items, int outputCount) {
        return newShapedRecipe(shortName, createSlots(items), outputCount);
    }

    public static CraftingRecipe newShapedRecipe(String shortName, ItemTarget[] slots, int outputCount) {
        if (slots.length != 4 && slots.length != 9) {
            Debug.logError("Invalid shaped crafting recipe, must be either size 4 or 9. Size given: " + slots.length);
            return null;
        }

        CraftingRecipe result = new CraftingRecipe();
        result._shortName = shortName;
        // Remove null
        result._slots = Arrays.stream(slots).map(target -> target == null ? ItemTarget.EMPTY : target).toArray(ItemTarget[]::new);
        result._outputCount = outputCount;
        if (slots.length == 4) {
            result._width = 2;
            result._height = 2;
        } else {
            result._width = 3;
            result._height = 3;
        }
        result._shapeless = false;

        return result;
    }

    private static ItemTarget[] createSlots(ItemTarget[] slots) {
        ItemTarget[] result = new ItemTarget[slots.length];
        System.arraycopy(slots, 0, result, 0, slots.length);
        return result;
    }

    private static ItemTarget[] createSlots(Item[][] slots) {
        ItemTarget[] result = new ItemTarget[slots.length];
        for (int i = 0; i < slots.length; ++i) {
            if (slots[i] == null) {
                result[i] = ItemTarget.EMPTY;
            } else {
                result[i] = new ItemTarget(slots[i]);
            }
        }
        return result;
    }

    public ItemTarget getSlot(int index) {
        ItemTarget result = _slots[index];
        return result != null ? result : ItemTarget.EMPTY;
    }

    public int getSlotCount() {
        return _slots.length;
    }

    public ItemTarget[] getSlots() {
        return _slots;
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public boolean isShapeless() {
        return _shapeless;
    }

    public boolean isBig() {
        return _slots.length > 4;
    }

    public int outputCount() {
        return _outputCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CraftingRecipe other) {
            if (other._shapeless != _shapeless) return false;
            if (other._outputCount != _outputCount) return false;
            if (other._height != _height) return false;
            if (other._width != _width) return false;
            //if (other._mustMatch.size() != _mustMatch.size()) return false;
            if (other._slots.length != _slots.length) return false;
            for (int i = 0; i < _slots.length; ++i) {
                if ((other._slots[i] == null) != (_slots[i] == null)) return false;
                if (other._slots[i] != null && !other._slots[i].equals(_slots[i])) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String name = "CraftingRecipe{";
        if (_shortName != null) {
            name += "craft " + _shortName;
        } else {
            name += "_slots=" + Arrays.toString(_slots) +
                    ", _width=" + _width +
                    ", _height=" + _height +
                    ", _shapeless=" + _shapeless;
        }
        name += "}";
        return name;
    }
}
