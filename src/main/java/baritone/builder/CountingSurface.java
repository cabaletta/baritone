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

package baritone.builder;

import baritone.api.utils.BetterBlockPos;

import java.util.OptionalInt;

public class CountingSurface extends NavigableSurface {
    public CountingSurface(int x, int y, int z) {
        super(x, y, z, Attachment::new, $ -> new Attachment());
    }

    private static class Attachment {
        public final int surfaceSize;

        public Attachment(Object a, Object b) {
            this((Attachment) a, (Attachment) b);
        }

        public Attachment(Attachment a, Attachment b) {
            this.surfaceSize = a.surfaceSize + b.surfaceSize;
        }

        public Attachment() {
            this.surfaceSize = 1;
        }

        @Override
        public boolean equals(Object o) { // used as performance optimization in RedBlackNode to avoid augmenting unchanged attachments
            if (this == o) {
                return true;
            }
            if (!(o instanceof Attachment)) {
                return false;
            }
            Attachment that = (Attachment) o;
            return surfaceSize == that.surfaceSize;
        }

        @Override
        public int hashCode() {
            return surfaceSize;
        }
    }

    public OptionalInt surfaceSize(BetterBlockPos pos) { // how big is the navigable surface from here? how many distinct coordinates can i walk to (in the future, the augmentation will probably have a list of those coordinates or something?)
        Object data = getComponentAugmentation(pos);
        if (data != null) { // i disagree with the intellij suggestion here i think it makes it worse
            return OptionalInt.of(((Attachment) data).surfaceSize);
        } else {
            return OptionalInt.empty();
        }
    }

    public int requireSurfaceSize(int x, int y, int z) {
        return surfaceSize(new BetterBlockPos(x, y, z)).getAsInt();
    }

    private void placeOrRemoveBlock(BetterBlockPos where, boolean place) {
        setBlock(where, place ? FakeStates.SOLID : FakeStates.AIR);
    }

    public void placeBlock(BetterBlockPos where) {
        placeOrRemoveBlock(where, true);
    }

    public void placeBlock(int x, int y, int z) {
        placeBlock(new BetterBlockPos(x, y, z));
    }

    public void removeBlock(BetterBlockPos where) {
        placeOrRemoveBlock(where, false);
    }

    public void removeBlock(int x, int y, int z) {
        removeBlock(new BetterBlockPos(x, y, z));
    }
}
