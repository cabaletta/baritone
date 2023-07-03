/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

/**
 * @author Brady
 */
public final class InventorySlot {

    /**
     * Maps directly to the slot ids of the player's inventory container.
     * <table width="100%">
     *   <tr>
     *       <th width="10%">Index</th>
     *       <th>Description</th>
     *   </tr>
     *   <tr><td>0</td><td>crafting output</td></tr>
     *   <tr><td>1-4</td><td>crafting grid</td></tr>
     *   <tr><td>5-8</td><td>armor</td></tr>
     *   <tr><td>9-35</td><td>inventory (index 9-35)</td></tr>
     *   <tr><td>36-44</td><td>hotbar (index 0-8)</td></tr>
     *   <tr><td>45</td><td>off-hand</td></tr>
     * </table>
     */
    private static final InventorySlot[] SLOTS = new InventorySlot[46];

    static {
        SLOTS[0] = new InventorySlot(0, Type.CRAFTING_OUTPUT);
        for (int i = 0; i < 4; i++) {
            SLOTS[i + 1] = new InventorySlot(i + 1, Type.CRAFTING_GRID);
        }
        for (int i = 0; i < 4; i++) {
            SLOTS[i + 5] = new InventorySlot(i + 5, Type.ARMOR);
        }
        for (int i = 0; i < 27; i++) {
            SLOTS[i + 9] = new InventorySlot(i + 9, Type.INVENTORY);
        }
        for (int i = 0; i < 9; i++) {
            SLOTS[i + 36] = new InventorySlot(i + 36, Type.HOTBAR);
        }
        SLOTS[45] = new InventorySlot(45, Type.OFFHAND);
    }

    private final int slotId;
    private final Type type;

    private InventorySlot(int slotId, Type type) {
        this.slotId = slotId;
        this.type = type;
    }

    /**
     * @return The ID of this slot, as used by {@code ContainerPlayer}
     */
    public int getSlotId() {
        return this.slotId;
    }

    public Type getType() {
        return this.type;
    }

    public int getInventoryIndex() {
        switch (this.getType()) {
            case HOTBAR:
                return this.slotId - 36;
            case INVENTORY:
                return this.slotId;
            default:
                throw new IllegalStateException("Slot type must be either HOTBAR or INVENTORY");
        }
    }

    public static InventorySlot hotbar(final int index) {
        if (index < 0 || index >= 9) {
            throw new IllegalArgumentException();
        }
        return SLOTS[index + 36];
    }

    public static InventorySlot inventory(final int index) {
        if (index < 9 || index >= 36) {
            throw new IllegalArgumentException();
        }
        return SLOTS[index];
    }

    public static InventorySlot offhand() {
        return SLOTS[45];
    }

    public enum Type {
        CRAFTING_OUTPUT,
        CRAFTING_GRID,
        ARMOR,
        INVENTORY,
        HOTBAR,
        OFFHAND
    }
}
