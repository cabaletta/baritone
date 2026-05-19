package adris.altoclef.util;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Item;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines an item and a count.
 * <p>
 * Multiple Minecraft Items can meet the criteria of an "item" (ex. "wooden planks" can be satisfied by oak, acacia, spruce, jungle, etc.)
 */
public class ItemTarget {

    private static final int BASICALLY_INFINITY = 99999999;

    public static ItemTarget EMPTY = new ItemTarget(new Item[0], 0);
    private Item[] _itemMatches;
    private int _targetCount;
    private String _catalogueName = null;
    private boolean _infinite = false;

    public ItemTarget(Item[] items, int targetCount) {
        _itemMatches = items;
        _targetCount = targetCount;
        _infinite = false;
    }

    public ItemTarget(String catalogueName, int targetCount) {
        _catalogueName = catalogueName;
        _itemMatches = TaskCatalogue.getItemMatches(catalogueName);
        _targetCount = targetCount;
    }

    public ItemTarget(String catalogueName) {
        this(catalogueName, 1);
    }

    public ItemTarget(Item item, int targetCount) {
        this(new Item[]{item}, targetCount);
    }

    public ItemTarget(Item[] items) {
        this(items, 1);
    }

    public ItemTarget(Item item) {
        this(item, 1);
    }

    public ItemTarget(ItemTarget toCopy, int newCount) {
        if (toCopy._itemMatches != null) {
            _itemMatches = new Item[toCopy._itemMatches.length];
            System.arraycopy(toCopy._itemMatches, 0, _itemMatches, 0, toCopy._itemMatches.length);
        }
        _catalogueName = toCopy._catalogueName;
        _targetCount = newCount;
        _infinite = toCopy._infinite;
    }

    public static boolean nullOrEmpty(ItemTarget target) {
        return target == null || target == EMPTY;
    }

    public static Item[] getMatches(ItemTarget... targets) {
        Set<Item> result = new HashSet<>();
        for (ItemTarget target : targets) {
            result.addAll(Arrays.asList(target.getMatches()));
        }
        return result.toArray(Item[]::new);
    }

    public ItemTarget infinite() {
        _infinite = true;
        return this;
    }

    public Item[] getMatches() {
        return _itemMatches != null ? _itemMatches : new Item[0];
    }

    public int getTargetCount() {
        if (_infinite) {
            return BASICALLY_INFINITY;
        }
        return _targetCount;
    }

    public boolean matches(Item item) {
        if (_itemMatches != null) {
            for (Item match : _itemMatches) {
                if (match == null) continue;
                if (match.equals(item)) return true;
            }
        }
        return false;
    }

    public boolean isCatalogueItem() {
        return _catalogueName != null;
    }

    public String getCatalogueName() {
        return _catalogueName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemTarget other) {
            if (_infinite) {
                if (!other._infinite) return false;
            } else {
                // Neither are infinite
                if (_targetCount != other._targetCount) return false;
            }
            if ((other._itemMatches == null) != (_itemMatches == null)) return false;
            if (_itemMatches != null) {
                if (_itemMatches.length != other._itemMatches.length) return false;
                for (int i = 0; i < _itemMatches.length; ++i) {
                    if (other._itemMatches[i] == null) {
                        if ((other._itemMatches[i] == null) != (_itemMatches[i] == null)) return false;
                    } else {
                        if (!other._itemMatches[i].equals(_itemMatches[i])) return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return _itemMatches == null || _itemMatches.length == 0;
    }

    @Override
    public String toString() {

        StringBuilder result = new StringBuilder();
        if (isEmpty()) {
            result.append("(empty)");
        } else if (isCatalogueItem()) {
            result.append(_catalogueName);
        } else {
            result.append("[");
            int counter = 0;
            if (_itemMatches != null) {
                for (Item item : _itemMatches) {
                    if (item == null) {
                        result.append("(null??)");
                    } else {
                        result.append(ItemHelper.trimItemName(item.getTranslationKey()));
                    }
                    if (++counter != _itemMatches.length) {
                        result.append(",");
                    }
                }
            }
            result.append("]");
        }
        if (!_infinite && !isEmpty()) {
            result.append(" x ").append(_targetCount);
        } else if (_infinite) {
            result.append(" x infinity");
        }

        return result.toString();
    }


}
