package adris.altoclef.util;

import net.minecraft.item.Item;

import java.util.Objects;

public class RecipeTarget {

    private final CraftingRecipe _recipe;
    private final Item _item;
    private final int _targetCount;

    public RecipeTarget(Item item, int targetCount, CraftingRecipe recipe) {
        _item = item;
        _targetCount = targetCount;
        _recipe = recipe;
    }

    public CraftingRecipe getRecipe() {
        return _recipe;
    }

    public Item getOutputItem() {
        return _item;
    }

    public int getTargetCount() {
        return _targetCount;
    }

    @Override
    public String toString() {
        return "RecipeTarget{" +
                "_recipe=" + _recipe +
                ", _item=" + _item + " x " + _targetCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeTarget that = (RecipeTarget) o;
        return _targetCount == that._targetCount && _recipe.equals(that._recipe) && Objects.equals(_item, that._item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_recipe, _item);
    }
}
