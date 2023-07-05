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

package baritone.api.utils;

import java.util.function.ObjIntConsumer;

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
        final ObjIntConsumer<Type> populate = new ObjIntConsumer<Type>() {
            private int index;

            @Override
            public void accept(Type type, int count) {
                for (int i = 0; i < count; i++) {
                    SLOTS[this.index] = new InventorySlot(this.index, type);
                    this.index++;
                }
            }
        };
        populate.accept(Type.CRAFTING_OUTPUT,   1);
        populate.accept(Type.CRAFTING_GRID,     4);
        populate.accept(Type.ARMOR,             4);
        populate.accept(Type.INVENTORY,         27);
        populate.accept(Type.HOTBAR,            9);
        populate.accept(Type.OFFHAND,           1);
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

    /**
     * Returns the index of this slot in {@code mainInventory}. If this slot does not correspond to an index into
     * {@code mainInventory}, then an {@link IllegalArgumentException} is thrown.
     *
     * @return The index of this slot in the player's {@code mainInventory}
     * @throws IllegalArgumentException if type is not {@link Type#HOTBAR} or {@link Type#INVENTORY}
     */
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

    public static InventorySlot inventory(final int index) {
        if (index >= 0 && index < 9) {
            return SLOTS[index + 36]; // HOTBAR
        } else if (index >= 9 && index < 36) {
            return SLOTS[index];      // INVENTORY
        }
        throw new IllegalArgumentException();
    }

    public static InventorySlot armor(final int index) {
        if (index < 0 || index >= 4) {
            throw new IllegalArgumentException();
        }
        return SLOTS[index + 5];
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
