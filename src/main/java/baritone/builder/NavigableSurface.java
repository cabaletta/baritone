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
import baritone.builder.utils.com.github.btrekkie.connectivity.Augmentation;
import baritone.builder.utils.com.github.btrekkie.connectivity.ConnGraph;

import java.util.Arrays;
import java.util.function.Function;

public class NavigableSurface {

    private final CuboidBounds bounds;

    private final BlockStateCachedData[] blocks; // TODO switch to xzy ordering so columnFrom is faster

    private final ConnGraph connGraph;

    private final Function<BetterBlockPos, Object> genVertexAugmentation;

    private final Column col1 = new Column();
    private final Column col2 = new Column();

    public NavigableSurface(int x, int y, int z, Augmentation augmentation, Function<BetterBlockPos, Object> genVertexAugmentation) {
        this.bounds = new CuboidBounds(x, y, z);
        this.blocks = new BlockStateCachedData[bounds.volume()];
        Arrays.fill(blocks, FakeStates.AIR);
        this.genVertexAugmentation = genVertexAugmentation;
        this.connGraph = new ConnGraph(augmentation);

        if (!genVertexAugmentation.apply(new BetterBlockPos(0, 0, 0)).equals(genVertexAugmentation.apply(new BetterBlockPos(0, 0, 0)))) {
            throw new IllegalStateException("RedBlackNode optimization requires correct impl of .equals on the attachment, to avoid percolating up spurious augmentation non-updates");
        }
    }

    private void columnFrom(Column column, long pos) {
        column.underneath = getBlockOrAir((pos + Column.DOWN_1) & BetterBlockPos.POST_ADDITION_MASK);
        column.feet = getBlockOrAir(pos);
        column.head = getBlockOrAir((pos + Column.UP_1) & BetterBlockPos.POST_ADDITION_MASK);
        column.above = getBlockOrAir((pos + Column.UP_2) & BetterBlockPos.POST_ADDITION_MASK);
        column.aboveAbove = getBlockOrAir((pos + Column.UP_3) & BetterBlockPos.POST_ADDITION_MASK);
        column.init();
    }

    protected void setBlock(long pos, BlockStateCachedData data) {
        blocks[bounds.toIndex(pos)] = data;
        for (int dy = -2; dy <= 1; dy++) {
            long couldHaveChanged = BetterBlockPos.offsetBy(pos, 0, dy, 0);
            columnFrom(col1, couldHaveChanged);
            boolean currentlyAllowed = col1.standing();
            if (currentlyAllowed) {
                // TODO skip the next line if it already has an augmentation?
                connGraph.setVertexAugmentation(couldHaveChanged, genVertexAugmentation.apply(BetterBlockPos.fromLong(couldHaveChanged)));

                for (Face dir : Face.HORIZONTALS) {
                    long adj = dir.offset(couldHaveChanged);
                    columnFrom(col2, adj);
                    Integer connDy = PlayerPhysics.bidirectionalPlayerTravel(col1, col2, getBlockOrAir((adj + Column.DOWN_2) & BetterBlockPos.POST_ADDITION_MASK), getBlockOrAir((adj + Column.DOWN_3) & BetterBlockPos.POST_ADDITION_MASK));
                    for (int fakeDy = -2; fakeDy <= 2; fakeDy++) {
                        long neighbor = BetterBlockPos.offsetBy(adj, 0, fakeDy, 0);
                        if (((Integer) fakeDy).equals(connDy)) {
                            connGraph.addEdge(couldHaveChanged, neighbor);
                        } else {
                            connGraph.removeEdge(couldHaveChanged, neighbor);
                        }
                    }
                }
            } else {
                connGraph.removeVertexAugmentation(couldHaveChanged);
                for (Face dir : Face.HORIZONTALS) {
                    long adj = dir.offset(couldHaveChanged);
                    for (int fakeDy = -2; fakeDy <= 2; fakeDy++) {
                        connGraph.removeEdge(couldHaveChanged, BetterBlockPos.offsetBy(adj, 0, fakeDy, 0));
                    }
                }
            }
        }
    }

    protected void setBlock(int x, int y, int z, BlockStateCachedData data) {
        setBlock(BetterBlockPos.toLong(x, y, z), data);
    }

    public CuboidBounds bounds() {
        return bounds;
    }

    public BlockStateCachedData getBlock(long pos) {
        return blocks[bounds.toIndex(pos)];
    }

    public BlockStateCachedData getBlockOrAir(long pos) {
        if (!bounds.inRangePos(pos)) {
            return FakeStates.AIR;
        }
        return getBlock(pos);
    }

    public boolean hasBlock(BetterBlockPos pos) {
        return getBlockOrAir(pos.toLong()).collidesWithPlayer;
    }

    public boolean connected(BetterBlockPos a, BetterBlockPos b) {
        return connGraph.connected(a.toLong(), b.toLong());
    }

    public Object getComponentAugmentation(BetterBlockPos pos) { // maybe should be protected? subclass defines it anyway
        return connGraph.getComponentAugmentation(pos.toLong());
    }
}
