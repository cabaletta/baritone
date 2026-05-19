package adris.altoclef.util;

import net.minecraft.item.Item;

import java.util.Objects;

public class SmeltTarget {

    private final ItemTarget _item;
    private final Item[] _optionalMaterials;
    private ItemTarget _material;

    public SmeltTarget(ItemTarget item, ItemTarget material, Item... optionalMaterials) {
        _item = item;
        _material = material;
        _material = new ItemTarget(material, _item.getTargetCount());
        _optionalMaterials = optionalMaterials;
    }

    public ItemTarget getItem() {
        return _item;
    }

    public ItemTarget getMaterial() {
        return _material;
    }

    public Item[] getOptionalMaterials() {
        return _optionalMaterials;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SmeltTarget that = (SmeltTarget) o;
        return Objects.equals(_material, that._material) && Objects.equals(_item, that._item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_material, _item);
    }
}
