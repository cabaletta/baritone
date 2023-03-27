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
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;

import static org.junit.Assert.assertEquals;


public class NavigableSurfaceTest {
    @Test
    public void testBasic() {
        NavigableSurface surface = new NavigableSurface(10, 10, 10);
        surface.placeBlock(0, 0, 0);
        assertEquals(OptionalInt.empty(), surface.surfaceSize(new BetterBlockPos(0, 0, 0)));
        assertEquals(1, surface.requireSurfaceSize(0, 1, 0));
        surface.placeBlock(1, 0, 0);
        assertEquals(2, surface.requireSurfaceSize(0, 1, 0));
        surface.placeBlock(1, 0, 0);
        surface.placeBlock(2, 0, 0);
        surface.placeBlock(3, 0, 0);
        surface.placeBlock(4, 0, 0);
        surface.placeBlock(5, 0, 0);
        // XXXXXX
        assertEquals(6, surface.requireSurfaceSize(2, 1, 0));

        surface.placeBlock(2, 1, 0);
        assertEquals(OptionalInt.empty(), surface.surfaceSize(new BetterBlockPos(2, 1, 0)));
        assertEquals(6, surface.requireSurfaceSize(2, 2, 0));

        surface.placeBlock(2, 2, 0);
        //   X
        //   X
        // XXXXXX
        assertEquals(2, surface.requireSurfaceSize(0, 1, 0));
        assertEquals(1, surface.requireSurfaceSize(2, 3, 0));
        assertEquals(3, surface.requireSurfaceSize(3, 1, 0));

        surface.placeBlock(1, 1, 0);
        //   X
        //  XX
        // XXXXXX
        assertEquals(3, surface.requireSurfaceSize(0, 1, 0));
        assertEquals(3, surface.requireSurfaceSize(3, 1, 0));

        surface.placeBlock(3, 2, 0);
        //   XX
        //  XX
        // XXXXXX
        assertEquals(4, surface.requireSurfaceSize(0, 1, 0));
        assertEquals(2, surface.requireSurfaceSize(4, 1, 0));

        surface.placeBlock(4, 1, 0);
        //   XX
        //  XX X
        // XXXXXX
        assertEquals(6, surface.requireSurfaceSize(0, 1, 0));
        assertEquals(OptionalInt.empty(), surface.surfaceSize(new BetterBlockPos(3, 1, 0)));

        surface.removeBlock(2, 2, 0);
        //    X
        //  XX X
        // XXXXXX
        assertEquals(6, surface.requireSurfaceSize(2, 2, 0));

        surface.removeBlock(2, 1, 0);
        //    X
        //  X  X
        // XXXXXX
        assertEquals(3, surface.requireSurfaceSize(1, 2, 0));
        assertEquals(3, surface.requireSurfaceSize(3, 3, 0));

        surface.removeBlock(3, 2, 0);
        //  X  X
        // XXXXXX
        assertEquals(6, surface.requireSurfaceSize(0, 1, 0));

        surface.removeBlock(1, 0, 0);
        //  X  X
        // X XXXX
        assertEquals(6, surface.requireSurfaceSize(0, 1, 0));

        surface.removeBlock(1, 1, 0);
        //     X
        // X XXXX
        assertEquals(1, surface.requireSurfaceSize(0, 1, 0));
        assertEquals(4, surface.requireSurfaceSize(2, 1, 0));
    }

    private NavigableSurface makeFlatSurface(int width, int height) {
        NavigableSurface surface = new NavigableSurface(width, height, width);
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                surface.placeBlock(new BetterBlockPos(x, 0, z));
            }
        }
        assertEquals(width * width, surface.requireSurfaceSize(0, 1, 0));
        return surface;
    }

    private NavigableSurface makeFlatSurface(int SZ) {
        return makeFlatSurface(SZ, SZ);
    }

    @Test
    public void testSurfaceSmall() {
        makeFlatSurface(10);
    }

    @Test
    public void testSurfaceMed() {
        makeFlatSurface(100);
    }

    /*@Test
    public void testSurfaceBig() { // 10x more on each side, so 100x more nodes total. youd expect this to be about 100x slower than testSurfaceMed, but it's actually 200x slower. this is ideally just because each graph operation is O(log^2 n) so n operations is O(n log^2 n), and hopefully not because of something that otherwise scales superlinearly
        makeFlatSurface(1000);
    }*/
    // okay but its slow so we dont care

    @Test
    public void testStep() {
        int SZ = 100;
        int lineAt = SZ / 2;
        NavigableSurface surface = makeFlatSurface(SZ);
        for (int x = 0; x < SZ; x++) {
            surface.placeBlock(x, 1, lineAt); // doesn't block the player since you can step over 1 block
        }
        assertEquals(SZ * SZ, surface.requireSurfaceSize(0, 1, 0));
    }

    @Test
    public void testBlocked() {
        int SZ = 100;
        int lineAt = SZ / 2;
        NavigableSurface surface = makeFlatSurface(SZ);
        for (int x = 0; x < SZ; x++) {
            surface.placeBlock(x, 2, lineAt); // does block the player since you can't step over 2 blocks
        }
        assertEquals(SZ * lineAt, surface.requireSurfaceSize(0, 1, 0));
        assertEquals(SZ * (SZ - lineAt - 1), surface.requireSurfaceSize(0, 1, SZ - 1));
        assertEquals(SZ, surface.requireSurfaceSize(0, 3, lineAt));
    }

    private void fillSurfaceInOrderMaintainingConnection(NavigableSurface surface, BetterBlockPos maintainConnectionTo, List<BetterBlockPos> iterationOrder) {
        fillSurfaceInOrderMaintainingConnection(surface, maintainConnectionTo, iterationOrder, false, false);
    }

    private void fillSurfaceInOrderMaintainingConnection(NavigableSurface surface, BetterBlockPos maintainConnectionTo, List<BetterBlockPos> iterationOrder, boolean requirePathToPlacement, boolean allowSideSneakPlace) {
        fillSurfaceInOrderMaintainingConnection(surface, maintainConnectionTo, iterationOrder, requirePathToPlacement, allowSideSneakPlace, Integer.MAX_VALUE);
    }

    private void fillSurfaceInOrderMaintainingConnection(NavigableSurface surface, BetterBlockPos maintainConnectionTo, List<BetterBlockPos> iterationOrder, boolean requirePathToPlacement, boolean allowSideSneakPlace, int stopAfter) {
        int qty = 0;
        outer:
        while (true) {
            for (BetterBlockPos candidate : iterationOrder) {
                if (surface.getBlock(candidate)) {
                    continue; // already placed
                }
                allowed:
                if (requirePathToPlacement) {
                    if (surface.connected(candidate, maintainConnectionTo)) {
                        break allowed;
                    }
                    if (allowSideSneakPlace) {
                        for (Face face : Face.HORIZONTALS) {
                            if (surface.connected(candidate.offset(face.toMC()).up(), maintainConnectionTo)) {
                                break allowed;
                            }
                        }
                    }
                    continue;
                }
                // let's try placing
                surface.placeBlock(candidate);
                if (surface.connected(candidate.up(), maintainConnectionTo)) {
                    qty++;
                    // success, placed a block while retaining the path down to the ground
                    if (qty == stopAfter) {
                        break;
                    }

                    /*if ((qty < 100 && stopAfter != Integer.MAX_VALUE) || qty % 100 == 0) {
                        System.out.println(qty + "\n" + reportAllFourWalls(surface));
                    }*/

                    continue outer;
                }
                // fail :(
                surface.removeBlock(candidate);
            }
            return;
        }
    }

    private String reportBottomToTop(NavigableSurface surface) {
        int len = surface.sizeY * ((surface.sizeX + 1) * surface.sizeZ + 1);
        StringBuilder report = new StringBuilder(len);
        for (int y = 0; y < surface.sizeY; y++) {
            for (int z = 0; z < surface.sizeZ; z++) {
                for (int x = 0; x < surface.sizeX; x++) {
                    report.append(surface.getBlock(new BetterBlockPos(x, y, z)) ? 'X' : ' ');
                }
                report.append('\n');
            }
            report.append('\n');
        }
        if (report.length() != len) {
            throw new IllegalStateException();
        }
        return report.toString();
    }

    private String reportSlice(NavigableSurface surface, int y) {
        int len = (surface.sizeX + 1) * surface.sizeZ;
        StringBuilder report = new StringBuilder(len);
        for (int z = 0; z < surface.sizeZ; z++) {
            for (int x = 0; x < surface.sizeX; x++) {
                report.append(surface.getBlock(new BetterBlockPos(x, y, z)) ? 'X' : ' ');
            }
            report.append('\n');
        }
        if (report.length() != len) {
            throw new IllegalStateException();
        }
        return report.toString();
    }

    private static List<BetterBlockPos> genReportedWallOrder(NavigableSurface surface, int y) {
        List<BetterBlockPos> ret = new ArrayList<>((surface.sizeX + surface.sizeZ) * 2);
        for (int x = 0; x < surface.sizeX; x++) {
            ret.add(new BetterBlockPos(x, y, 0));
        }
        // start at 1 not 0 so that we don't repeat the last iteration of the previous loop (that would make the report look bad because the staircase would repeat one column for no reason)
        for (int z = 1; z < surface.sizeZ; z++) {
            ret.add(new BetterBlockPos(surface.sizeX - 1, y, z));
        }
        // same deal for starting at -2 rather than -1
        for (int x = surface.sizeX - 2; x >= 0; x--) {
            ret.add(new BetterBlockPos(x, y, surface.sizeZ - 1));
        }
        // and same again
        for (int z = surface.sizeZ - 2; z > 0; z--) {
            ret.add(new BetterBlockPos(0, y, z));
        }
        return ret;
    }

    private static <T> List<T> reverse(List<T> list) {
        List<T> ret = new ArrayList<>(list.size());
        for (int i = list.size() - 1; i >= 0; i--) {
            ret.add(list.get(i));
        }
        return ret;
    }

    private String reportAllFourWalls(NavigableSurface surface) {
        int len = surface.sizeY * (surface.sizeX + surface.sizeZ) * 2;
        StringBuilder report = new StringBuilder(len);
        for (int y = surface.sizeY - 1; y >= 0; y--) {
            // make a report of what all four walls look like
            for (int x = 0; x < surface.sizeX - 1; x++) {
                report.append(surface.getBlock(new BetterBlockPos(x, y, 0)) ? 'X' : ' ');
            }
            report.append('|');
            for (int z = 0; z < surface.sizeZ - 1; z++) {
                report.append(surface.getBlock(new BetterBlockPos(surface.sizeX - 1, y, z)) ? 'X' : ' ');
            }
            report.append('|');
            for (int x = surface.sizeX - 1; x > 0; x--) {
                report.append(surface.getBlock(new BetterBlockPos(x, y, surface.sizeZ - 1)) ? 'X' : ' ');
            }
            report.append('|');
            for (int z = surface.sizeZ - 1; z > 0; z--) {
                report.append(surface.getBlock(new BetterBlockPos(0, y, z)) ? 'X' : ' ');
            }
            report.append('\n');
        }
        if (report.length() != len) {
            throw new IllegalStateException();
        }
        return report.toString();
    }

    @Test
    public void testCastleWall() {
        // build a single wall, but, never place a block that disconnects the surface
        // (we expect to see a triangle)
        int SZ = 20;
        NavigableSurface surface = makeFlatSurface(SZ);
        BetterBlockPos someOtherBlock = new BetterBlockPos(0, 1, 1); // won't be involved in the wall (since z=1)
        List<BetterBlockPos> order = new ArrayList<>();
        for (int y = 0; y < SZ; y++) {
            for (int x = 0; x < SZ; x++) {
                order.add(new BetterBlockPos(x, y, 0));
            }
        }

        fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);

        String shouldBe = "" +
                "XX                 |                   |                   |                   \n" +
                "XXX                |                   |                   |                   \n" +
                "XXXX               |                   |                   |                   \n" +
                "XXXXX              |                   |                   |                   \n" +
                "XXXXXX             |                   |                   |                   \n" +
                "XXXXXXX            |                   |                   |                   \n" +
                "XXXXXXXX           |                   |                   |                   \n" +
                "XXXXXXXXX          |                   |                   |                   \n" +
                "XXXXXXXXXX         |                   |                   |                   \n" +
                "XXXXXXXXXXX        |                   |                   |                   \n" +
                "XXXXXXXXXXXX       |                   |                   |                   \n" +
                "XXXXXXXXXXXXX      |                   |                   |                   \n" +
                "XXXXXXXXXXXXXX     |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXX    |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXX   |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXXX  |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXXXX |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXXXXX|                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXXXXX|X                  |                   |                   \n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBe, reportAllFourWalls(surface));

        NavigableSurface surface2 = makeFlatSurface(SZ);
        fillSurfaceInOrderMaintainingConnection(surface2, someOtherBlock, order, true, false); // this one also works in strict jump placement mode
        assertEquals(shouldBe, reportAllFourWalls(surface2));

        NavigableSurface surface3 = makeFlatSurface(SZ);
        fillSurfaceInOrderMaintainingConnection(surface3, someOtherBlock, order, true, true); // sneak doesn't help either
        assertEquals(shouldBe, reportAllFourWalls(surface3));
    }

    @Test
    public void testCastleFourWalls() {
        // build four walls, but, never place a block that disconnects the surface
        // (we expect to see a carved path for the player from bottom to top)
        int SZ = 20;
        NavigableSurface surface = makeFlatSurface(SZ);
        BetterBlockPos someOtherBlock = new BetterBlockPos(SZ / 2, 1, SZ / 2); // center of the courtyard
        List<BetterBlockPos> order = new ArrayList<>();
        for (int y = 0; y < SZ; y++) {
            for (int x = 0; x < SZ; x++) {
                for (int z = 0; z < SZ; z++) {
                    boolean xOnEdge = x == 0 || x == SZ - 1;
                    boolean zOnEdge = z == 0 || z == SZ - 1;
                    if (!xOnEdge && !zOnEdge) {
                        continue; // in the courtyard
                    }
                    order.add(new BetterBlockPos(x, y, z));
                }
            }
        }

        fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);

        String shouldBe = "" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXX   XX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX   XXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX   XXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXX   XXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXX   XXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXX   XXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXX   XXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXX   XXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXX   XXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXX   XXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXX   XXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XX   XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X   XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" + // double row is because we started with a flat surface, so there is another flat row behind this one to step back into
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBe, reportAllFourWalls(surface));

        NavigableSurface surface2 = makeFlatSurface(SZ);
        fillSurfaceInOrderMaintainingConnection(surface2, someOtherBlock, order, true, false);
        String shouldBeWithJump = "" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |                 XX|XXXXXXXXXXXXXXXXXXX\n" + // if we're only allowed to jump place, the overhang is no longer possible (since it requires sneak side place)
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |                XXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |               XXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |              XXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |             XXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |            XXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |           XXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |          XXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |         XXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |        XXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |       XXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |      XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |     XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |    XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBeWithJump, reportAllFourWalls(surface2));

        NavigableSurface surface3 = makeFlatSurface(SZ);
        fillSurfaceInOrderMaintainingConnection(surface3, someOtherBlock, order, true, true);
        assertEquals(shouldBe /* but if side place is allowed, we can make the full shape! */, reportAllFourWalls(surface3));
    }

    @Test
    public void testCastleFourWallsLoopOrder() {
        int SZ = 20;
        NavigableSurface surface = makeFlatSurface(SZ);
        BetterBlockPos someOtherBlock = new BetterBlockPos(SZ / 2, 1, SZ / 2); // center of the courtyard
        List<BetterBlockPos> order = new ArrayList<>();
        for (int y = 0; y < SZ; y++) {
            order.addAll(genReportedWallOrder(surface, y));
        }

        fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);

        String shouldBe = "" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X   XXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XX   XXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXX   XXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXX   XXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXX   XXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXX   XXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXX   XXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXX   XXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXX   XXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXX   XXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX   XXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX   XXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXX   XX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXX   X\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX   \n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX  \n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX \n" + // in loop order it puts off the gap until the last possible moment
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBe, reportAllFourWalls(surface));
    }

    @Test
    public void testCastleFourWallsTopToBottomLoopOrder() {
        // build four walls, but, never place a block that disconnects the surface
        // except, try candidate blocks from top to bottom
        // this creates a cool shape :)
        int SZ = 20;
        for (int heightTest = 0; heightTest < 3; heightTest++) {
            NavigableSurface surface = makeFlatSurface(SZ);
            BetterBlockPos someOtherBlock = new BetterBlockPos(SZ / 2, 1, SZ / 2); // center of the courtyard
            List<BetterBlockPos> order = new ArrayList<>();
            for (int y = SZ - 1 - heightTest; y >= 0; y--) {
                order.addAll(genReportedWallOrder(surface, y));
            }

            fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);

            // you can see the three y variants of how the top row gets squashed down into the bottom, I think it's cool
            String[] shouldBe = {
                    "" +
                            "XXXXXXXXXXXXXXX   X|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "                 X |                   |                   |                   \n" +
                            "                X  |                   |                   |                   \n" +
                            "XXXXXXXXXXXXXXXX   |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "              X   X|                   |                   |                   \n" +
                            "             X   X |                   |                   |                   \n" +
                            "XXXXXXXXXXXXX   X  | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "           X   X   |X                  |                   |                   \n" +
                            "          X   X   X|                   |                   |                   \n" +
                            "XXXXXXXXXX   X   X |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "        X   X   X  | X                 |                   |                   \n" +
                            "       X   X   X   |X                  |                   |                   \n" +
                            "XXXXXXX   X   X   X|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "     X   X   X   X |  X                |                   |                   \n" +
                            "    X   X   X   X  | X                 |                   |                   \n" +
                            "XXXX   X   X   X   |X   XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "  X   X   X   X   X|   X               |                   |                   \n" +
                            " X   X   X   X   X |  X                |                   |                   \n" +
                            "X  XX  XX  XX  XX  |XX  XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n",
                    "" +
                            "                   |                   |                   |                   \n" +
                            "XXXXXXXXXXXXXX   XX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "                X  |                   |                   |                   \n" +
                            "               X   |                   |                   |                   \n" +
                            "XXXXXXXXXXXXXXX   X|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "             X   X |                   |                   |                   \n" +
                            "            X   X  |                   |                   |                   \n" +
                            "XXXXXXXXXXXX   X   |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "          X   X   X|                   |                   |                   \n" +
                            "         X   X   X |                   |                   |                   \n" +
                            "XXXXXXXXX   X   X  | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "       X   X   X   |X                  |                   |                   \n" +
                            "      X   X   X   X|                   |                   |                   \n" +
                            "XXXXXX   X   X   X |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "    X   X   X   X  | X                 |                   |                   \n" +
                            "   X   X   X   X   |X                  |                   |                   \n" +
                            "XXX   X   X   X   X|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            " X   X   X   X   X |  X                |                   |                   \n" +
                            "X  XX  XX  XX  XX  |XX                 |                   |                   \n" +
                            "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n",
                    "" +
                            "                   |                   |                   |                   \n" +
                            "                   |                   |                   |                   \n" +
                            "XXXXXXXXXXXXX   XXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "               X   |                   |                   |                   \n" +
                            "              X    |                   |                   |                   \n" +
                            "XXXXXXXXXXXXXX   XX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "            X   X  |                   |                   |                   \n" +
                            "           X   X   |                   |                   |                   \n" +
                            "XXXXXXXXXXX   X   X|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "         X   X   X |                   |                   |                   \n" +
                            "        X   X   X  |                   |                   |                   \n" +
                            "XXXXXXXX   X   X   |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "      X   X   X   X|                   |                   |                   \n" +
                            "     X   X   X   X |                   |                   |                   \n" +
                            "XXXXX   X   X   X  | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "   X   X   X   X   |X                  |                   |                   \n" +
                            "  X   X   X   X   X|                   |                   |                   \n" +
                            "XX   X   X   X   X |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                            "X  XX  XX  XX  XX  |XX                 |                   |                   \n" +
                            "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n"};
            assertEquals(shouldBe[heightTest], reportAllFourWalls(surface));

            if (heightTest != 0) {
                continue;
            }

            NavigableSurface surface2 = makeFlatSurface(SZ);
            fillSurfaceInOrderMaintainingConnection(surface2, someOtherBlock, order, true, false);
            String shouldBeV = "" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X                  \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XX                 \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX                \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXX               \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXX              \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXX             \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXX            \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXX           \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXX          \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXX         \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXX        \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX       \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX      \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXX     \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXX    \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX   \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX  \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
            assertEquals(shouldBeV, reportAllFourWalls(surface2));


            NavigableSurface surface3 = makeFlatSurface(SZ);
            fillSurfaceInOrderMaintainingConnection(surface3, someOtherBlock, order, true, true, 69);
            // PARTIAL FILL, purely to demonstrate the order
            String shouldBeStaircasePartial69 = "" +
                    "                   |        XXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX                \n" +
                    "                   |                   |                   |  XX               \n" +
                    "                   |                   |                   |   XX              \n" +
                    "                   |                   |                   |    XX             \n" +
                    "                   |                   |                   |     XX            \n" +
                    "                   |                   |                   |      XX           \n" +
                    "                   |                   |                   |       XX          \n" +
                    "                   |                   |                   |        XX         \n" +
                    "                   |                   |                   |         XX        \n" +
                    "                   |                   |                   |          XX       \n" +
                    "                   |                   |                   |           XX      \n" +
                    "                   |                   |                   |            XX     \n" +
                    "                   |                   |                   |             XX    \n" +
                    "                   |                   |                   |              XX   \n" +
                    "                   |                   |                   |               XX  \n" +
                    "                   |                   |                   |                XX \n" +
                    "                   |                   |                   |                 XX\n" +
                    "X                  |                   |                   |                  X\n" +
                    "XX                 |                   |                   |                   \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
            assertEquals(shouldBeStaircasePartial69, reportAllFourWalls(surface3));

            fillSurfaceInOrderMaintainingConnection(surface3, someOtherBlock, order, true, true, 200);
            String shouldBeStaircasePartial200 = "" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX\n" +
                    "                   |                   |                   |  XX               \n" +
                    "                   |                   |                   |   XX              \n" +
                    "                   |                   |                   |    XXXXXXXXXXXX   \n" +
                    "                   |                   |                   |     XXXXXXXXXXXX  \n" +
                    "                   |                   |                   |      XXXXXXXXXXXX \n" +
                    "                   |                   |                   |       XXXXXXXXXXXX\n" +
                    "X                  |                   |                   |        XXXXXXXXXXX\n" +
                    "XX                 |                   |                   |         XXXXXXXXXX\n" +
                    "XXX                |                   |                   |          XXXXXXXXX\n" +
                    "XXXX               |                   |                   |           XXXXXXXX\n" +
                    "XXXXX              |                   |                   |            XXXXXXX\n" +
                    "XXXXXX             |                   |                   |             XXXXXX\n" +
                    "XXXXXXX            |                   |                   |              XXXXX\n" +
                    "XXXXXXXX           |                   |                   |               XXXX\n" +
                    "XXXXXXXXX          |                   |                   |                XXX\n" +
                    "XXXXXXXXXX         |                   |                   |                 XX\n" +
                    "XXXXXXXXXXX        |                   |                   |                  X\n" +
                    "XXXXXXXXXXXX       |                   |                   |                   \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
            assertEquals(shouldBeStaircasePartial200, reportAllFourWalls(surface3));

            // finish partial fill
            fillSurfaceInOrderMaintainingConnection(surface3, someOtherBlock, order, true, true);
            String shouldBeStaircase = "" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX\n" + // this top row is completed first
                    "                   |                   |                   |  XX               \n" +
                    "                   |                   |                   |   XX              \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X   XXXXXXXXXXXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XX   XXXXXXXXXXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXX   XXXXXXXXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXX   XXXXXXXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXX   XXXXXXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXX   XXXXXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXX   XXXXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXX   XXXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXX   XXXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXX   XXXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX   XXXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX   XXX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXX   XX\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXX   X\n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX  \n" +
                    "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
            assertEquals(shouldBeStaircase, reportAllFourWalls(surface3));
        }
    }

    @Test
    public void testCastleFourWallsTopToBottom() {
        // same as previous except in Z X iteration order, this causes some FUNKY shapes to appear and I think it's cool
        int SZ = 20;
        NavigableSurface surface = makeFlatSurface(SZ);
        BetterBlockPos someOtherBlock = new BetterBlockPos(SZ / 2, 1, SZ / 2); // center of the courtyard
        List<BetterBlockPos> order = new ArrayList<>();
        for (int y = SZ - 1; y >= 0; y--) {
            for (int z = 0; z < SZ; z++) { // only reason is that it looks visually better with Z outer loop X inner loop
                for (int x = 0; x < SZ; x++) {
                    boolean xOnEdge = x == 0 || x == SZ - 1;
                    boolean zOnEdge = z == 0 || z == SZ - 1;
                    if (!xOnEdge && !zOnEdge) {
                        continue; // in the courtyard
                    }
                    order.add(new BetterBlockPos(x, y, z));
                }
            }
        }

        fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);

        // waow, so cool!
        String shouldBe = "" +
                "XXXXXXXXXXXXXXX   X|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "                 X |                   |                   |                   \n" +
                "                X  |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXX   |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "              X   X|                   |                   |                   \n" +
                "             X   X |                   |                   |                   \n" +
                "XXXXXXXXXXXXX   X  | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "           X   X   |X                  |                   |                   \n" +
                "          X   X   X|                   |                   |                   \n" +
                "XXXXXXXXXX   X   X |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "        X   X   X  | X                 |                   |                   \n" +
                "       X   X   X   |X                  |                   |                   \n" +
                "XXXXXXX   X   X   X|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "     X   X   X   X |  X                |                   |                   \n" +
                "    X   X   X   X  | X                 |                   |                   \n" +
                "  XX   X   X   X   |X  XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX   \n" +
                " XX   X   X   X   X|                   |                   |                X  \n" +
                "XX   X   X   X   X |  X                |                   |                 X \n" +
                "X  XX  XX  XX  XX  |XX XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX  X\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        // waow, so cool!
        assertEquals(shouldBe, reportAllFourWalls(surface));

        NavigableSurface surface2 = makeFlatSurface(SZ);
        fillSurfaceInOrderMaintainingConnection(surface2, someOtherBlock, order, true, false, 500);
        String shouldBeVPartial500 = "" +
                "XXXXXXXXXXXXX      |                   |                   |                   \n" + // you can see how the iteration order causes it to fill in from both side, eventually causing a V in the middle
                "XXXXXXXXXXXXXX     |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXX    |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXX   |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXXX  |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXXXX |                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXXXXX|                   |                   |                   \n" +
                "XXXXXXXXXXXXXXXXXXX|X                  |                   |                  X\n" + // why is the right triangle much lower than left? because by iteration order, it only starts considering the right quadrant when the left quadrant is full, because the right quadrant is after the left quadrant but before the second to left
                "XXXXXXXXXXXXXXXXXXX|XX                 |                   |                 XX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXX                |                   |                XXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXX               |                   |               XXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXX              |                   |              XXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXX             |                   |             XXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXX            |                   |            XXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXX           |                   |           XXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXX         |                   |          XXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXX        |                   |         XXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX       |                   |        XXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX      |                   |       XXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBeVPartial500, reportAllFourWalls(surface2));

        fillSurfaceInOrderMaintainingConnection(surface2, someOtherBlock, order, true, false, 500);
        String shouldBeVPartial500Again = "" +
                "XXXXXXXXXXXXXXXXXXX|X                  |                   |   XXXXXXXXXXXXXXXX\n" + // it keeps the top right as always blocked off due to the iteration order being top to bottom
                "XXXXXXXXXXXXXXXXXXX|XX                 |                   |   XXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXX                |                   |   XXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXX               |                   |  XXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXX              |                   | XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXX             |                   |XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXX            |                  X|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXX           |                 XX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXX          |                XXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXX         |               XXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXX        |             XXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX       |            XXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX      |           XXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXX     |          XXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXX    |         XXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX   |        XXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX  |       XXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |      XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|     XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBeVPartial500Again, reportAllFourWalls(surface2));

        fillSurfaceInOrderMaintainingConnection(surface2, someOtherBlock, order, true, false);
        String shouldBeV = "" +
                "XXXXXXXXXXXXXXXXXXX|XXX                |                 XX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXX               |                 XX|XXXXXXXXXXXXXXXXXXX\n" + // <-- this one was allowed to place because the top path can loop around and descend on the left
                "XXXXXXXXXXXXXXXXXXX|XXXXX              |                 XX|XXXXXXXXXXXXXXXXXXX\n" + // <-- it can't continue filling it in because the next block would have no path from its top to the center
                "XXXXXXXXXXXXXXXXXXX|XXXXXX             |                XXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXX            |               XXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXX           |              XXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXX          |             XXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXX         |            XXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXX        |           XXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX       |          XXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX      |         XXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXX     |        XXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXX    |       XXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX   |      XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX  |     XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |    XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBeV, reportAllFourWalls(surface2));

        NavigableSurface surface3 = makeFlatSurface(SZ);
        fillSurfaceInOrderMaintainingConnection(surface3, someOtherBlock, order, true, true, 500);
        String shouldBeWeirdPartial500 = "" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX\n" +
                "                   |                   |                   |  XX               \n" +
                "                   |                   |                   |   XX              \n" +
                "XXXXXXXXXXX        |                   |                   |    XXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXX       |                   |                   |     XXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXX      |                   |                   |      XXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXX     |                   |                   |       XXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXX    |                   |                   |        XXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXX   |                   |                   |         XXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXX  |                   |                   |          XXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXX |                   |                   |           XXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|                   |                   |            XXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|X                  |                   |             XXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XX                 |                   |              XXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXX                |                   |               XXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXX               |                   |                XXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXX              |                   |                 XX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXX             |                   |              XX  X\n" + // <-- just as with shouldBeVPartial500, we see that the quadrant iteration order causes the rightmost quadrant to only start being filled from the bottom once the left quadrant is full and spilling into second-to-left
                "XXXXXXXXXXXXXXXXXXX|XXXXXXX            |                   |            XXXXX  \n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBeWeirdPartial500, reportAllFourWalls(surface3));


        fillSurfaceInOrderMaintainingConnection(surface3, someOtherBlock, order, true, true);
        // "look i'll be the first one to admit it: this one is weird and i don't understand it :(" - me, 2.5 hours ago
        // ^^ I WROTE THAT COMMENT BEFORE FIGURING OUT THE PREVIOUS TWO
        // NOW YUO SEE, WITH THE NEW PARTIAL shouldBeWeirdPartial500
        String shouldBeWeird = "" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX\n" +
                "                   |                   |                   |  XX               \n" +
                "                   |                   |                   |   XX              \n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXX             |                XXX|XX  XXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXX            |               XXXX|XXX  XXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXX           |              XXXXX|XXXX  XXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXX          |             XXXXXX|XXXXX  XXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXX         |            XXXXXXX|XXXXXX  XXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXX        |           XXXXXXXX|XXXXXXX  XXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX       |          XXXXXXXXX|XXXXXXXX  XXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX      |         XXXXXXXXXX|XXXXXXXXX  XXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXX     |        XXXXXXXXXXX|XXXXXXXXXX  XXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXX    |       XXXXXXXXXXXX|XXXXXXXXXXX  XXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX   |      XXXXXXXXXXXXX|XXXXXXXXXXXX  XXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX  |     XXXXXXXXXXXXXX|XXXXXXXXXXXXX  XXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |    XXXXXXXXXXXXXXX|XXXXXXXXXXXXXX  XXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXX  XX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX  X\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX  \n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        // basically the story is that i saw THIS ^^^ weird ass shape, then i spent the next few hours figuring out exactly what happened and convincing myself that it actually is accurate and I don't have any bugs causing this incorrectly
        // because i mean what are the odds lol, it looks crazy
        assertEquals(shouldBeWeird, reportAllFourWalls(surface3));
    }

    @Test
    public void testCastleFourWallsNoCornerTopToBottom() {
        // just a small variant with no corner, so there's no wraparound
        // not that interesting
        int SZ = 20;
        NavigableSurface surface = makeFlatSurface(SZ);
        BetterBlockPos someOtherBlock = new BetterBlockPos(SZ / 2, 1, SZ / 2); // center of the courtyard
        List<BetterBlockPos> order = new ArrayList<>();
        for (int y = SZ - 1; y >= 0; y--) {
            for (int z = 0; z < SZ; z++) { // only reason is that it looks visually better with Z outer loop X inner loop
                for (int x = 0; x < SZ; x++) {
                    boolean xOnEdge = x == 0 || x == SZ - 1;
                    boolean zOnEdge = z == 0 || z == SZ - 1;
                    if (!xOnEdge && !zOnEdge) {
                        continue; // in the courtyard
                    }
                    if (x != 0 || z != 0) {
                        order.add(new BetterBlockPos(x, y, z));
                    }
                }
            }
        }

        fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);


        String shouldBe = "" +
                "                   |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "                  X|                   |                   |                   \n" +
                "                 X |                   |                   |                   \n" +
                "                X  | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "               X   |X                  |                   |                   \n" +
                "              X   X|                   |                   |                   \n" +
                "             X   X |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "            X   X  | X                 |                   |                   \n" +
                "           X   X   |X                  |                   |                   \n" +
                "          X   X   X|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "         X   X   X |  X                |                   |                   \n" +
                "        X   X   X  | X                 |                   |                   \n" +
                "       X   X   X   |X   XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "      X   X   X   X|   X               |                   |                   \n" +
                "     X   X   X   X |  X                |                   |                   \n" +
                "    X   X   X   X  | X  XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX   \n" +
                "   X   X   X   X   |X                  |                   |                X  \n" +
                "  X   X   X   X   X|   X               |                   |                 X \n" +
                " X  XX  XX  XX  XX | XX XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX  X\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBe, reportAllFourWalls(surface));

        NavigableSurface surface2 = makeFlatSurface(SZ);
        fillSurfaceInOrderMaintainingConnection(surface2, someOtherBlock, order, true, false);
        String shouldBeV = "" +
                " XXXXXXXXXXXXXXXXXX|XXX                |                   |XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXX               |                  X|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXX              |                 XX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXX             |                XXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXX            |               XXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXX           |              XXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXX          |             XXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXX         |            XXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXX        |           XXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX       |          XXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX      |         XXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXX     |        XXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXX    |       XXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXX   |      XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX  |     XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |    XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                " XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        //       ^  notice how the left column is gone
        assertEquals(shouldBeV, reportAllFourWalls(surface2));

        NavigableSurface surface3 = makeFlatSurface(SZ);
        fillSurfaceInOrderMaintainingConnection(surface3, someOtherBlock, order, true, true);
        assertEquals(shouldBeV /* sneak doesn't help without loop around through {x=0 z=0} corner*/, reportAllFourWalls(surface3));
    }

    @Test
    public void testCastleFourWallsWithInteriorRingAfter() {
        // build four walls, but, never place a block that disconnects the surface
        // (we expect to see a zigzag cut out)
        // but also an interior ring at y=8, filled afterwards
        // because it's filled afterwards, it only allows for one extra block to be placed at y=9
        int SZ = 20;
        NavigableSurface surface = makeFlatSurface(SZ);
        BetterBlockPos someOtherBlock = new BetterBlockPos(SZ / 2, 1, SZ / 2); // center of the courtyard
        List<BetterBlockPos> order = new ArrayList<>();
        for (int y = 0; y < SZ; y++) {
            for (int x = 0; x < SZ; x++) {
                for (int z = 0; z < SZ; z++) {
                    boolean xOnEdge = x == 0 || x == SZ - 1;
                    boolean zOnEdge = z == 0 || z == SZ - 1;
                    if (!xOnEdge && !zOnEdge) {
                        continue; // in the courtyard
                    }
                    order.add(new BetterBlockPos(x, y, z));
                }
            }
        }
        for (int x = 0; x < SZ; x++) {
            for (int z = 0; z < SZ; z++) {
                boolean xOnEdge = (x == 1 || x == SZ - 2) && z != 0 && z != SZ - 1; // the Z condition cuts off the X line beyond the Z line. in other words, we want to make a square and not a # shape
                boolean zOnEdge = (z == 1 || z == SZ - 2) && x != 0 && x != SZ - 1;
                if (!xOnEdge && !zOnEdge) {
                    continue; // in the courtyard or on the wall
                }
                order.add(new BetterBlockPos(x, 8, z));
            }
        }

        fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);

        String shouldBe = "" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXX   XX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX   XXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX   XXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXX   XXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXX   XXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXX   XXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXX   XXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXX   XXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXX   XXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXX   XXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXX  XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" + // <-- the extra block is here
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XX   XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X   XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBe, reportAllFourWalls(surface));
    }

    @Test
    public void testCastleFourWallsWithInteriorRingBefore() {
        // build four walls, but, never place a block that disconnects the surface
        // (we expect to see a zigzag cut out)
        // but also an interior ring at y=8, filled beforehand
        int SZ = 20;
        NavigableSurface surface = makeFlatSurface(SZ);
        BetterBlockPos someOtherBlock = new BetterBlockPos(SZ / 2, 1, SZ / 2); // center of the courtyard
        List<BetterBlockPos> order = new ArrayList<>();
        for (int x = 0; x < SZ; x++) {
            for (int z = 0; z < SZ; z++) {
                boolean xOnEdge = (x == 1 || x == SZ - 2) && z != 0 && z != SZ - 1; // the Z condition cuts off the X line beyond the Z line. in other words, we want to make a square and not a # shape
                boolean zOnEdge = (z == 1 || z == SZ - 2) && x != 0 && x != SZ - 1;
                if (!xOnEdge && !zOnEdge) {
                    continue; // in the courtyard or on the wall
                }
                order.add(new BetterBlockPos(x, 8, z));
            }
        }
        for (int y = 0; y < SZ; y++) {
            for (int x = 0; x < SZ; x++) {
                for (int z = 0; z < SZ; z++) {
                    boolean xOnEdge = x == 0 || x == SZ - 1;
                    boolean zOnEdge = z == 0 || z == SZ - 1;
                    if (!xOnEdge && !zOnEdge) {
                        continue; // in the courtyard
                    }
                    order.add(new BetterBlockPos(x, y, z));
                }
            }
        }

        fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);

        String shouldBe = "" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXX   XXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXX   XXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXX   XXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XX   XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X   XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |XXXXX XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" + // <-- we see both staircases start from the same corner because the iteration order is the same at each y level
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXX  XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XX   XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X   XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBe, reportAllFourWalls(surface));
    }

    @Test
    public void testCastleFourWallsWithInteriorHashtagBefore() {
        // build four walls, but, never place a block that disconnects the surface
        // (we expect to see a zigzag cut out)
        // but also an interior # shape at y=8, filled afterwards
        int SZ = 20;
        NavigableSurface surface = makeFlatSurface(SZ);
        BetterBlockPos someOtherBlock = new BetterBlockPos(SZ / 2, 1, SZ / 2); // center of the courtyard
        List<BetterBlockPos> order = new ArrayList<>();
        for (int x = 0; x < SZ; x++) {
            for (int z = 0; z < SZ; z++) {
                boolean xOnEdge = (x == 1 || x == SZ - 2);
                boolean zOnEdge = (z == 1 || z == SZ - 2);
                if (!xOnEdge && !zOnEdge) {
                    continue; // in the courtyard or on the wall
                }
                // this makes a # shape because it includes {x=1,z=0} {x=1,z=1} {x=0,z=1}
                order.add(new BetterBlockPos(x, 8, z));
            }
        }
        for (int y = 0; y < SZ; y++) {
            for (int x = 0; x < SZ; x++) {
                for (int z = 0; z < SZ; z++) {
                    boolean xOnEdge = x == 0 || x == SZ - 1;
                    boolean zOnEdge = z == 0 || z == SZ - 1;
                    if (!xOnEdge && !zOnEdge) {
                        continue; // in the courtyard
                    }
                    order.add(new BetterBlockPos(x, y, z));
                }
            }
        }

        fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);

        String shouldBe = "" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXX   XXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXX   XXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXX   XXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XX   XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X   XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |XXXXX XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXX  XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" + // <-- the "hashtag prongs" come out here at y=8
                "X XXXXXXXXXXXXXXXX |X XXXXXXXXXXXXXXXX |X    XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" + // <-- preventing four blocks at y=7 from being placed
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X   XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" + // <-- but they CAN be placed at y=6, because the y=6 ring is placed before the staircase connects to y=7, because once the staircase reaches y=7 the entire hashtag gets placed since its ordered before the walls!
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBe, reportAllFourWalls(surface));
        String sliceShouldBe = "" +
                "XXXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XX                XX\n" +
                "XXXXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXX   XXX\n";
        assertEquals(sliceShouldBe, reportSlice(surface, 8));
    }

    @Test
    public void testFullCube() {
        int width = 10;
        int height = 30;
        NavigableSurface surface = makeFlatSurface(width, height);
        BetterBlockPos someOtherBlock = new BetterBlockPos(0, 1, width / 2);
        List<BetterBlockPos> order = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    order.add(new BetterBlockPos(x, y, z));
                }
            }
        }

        fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);

        // it happens to generate a spiral staircase :)
        String shouldBe = "" +
                "XXXXXXXXX|XXXXXXXX |X XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|  XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXX |  XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXX | XXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXX |X XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|  XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXX |  XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXX | XXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXX |X XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|  XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXX |  XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXX | XXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXX |X XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|  XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXX |  XXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXX  | XXXXXXXX|XXXXXXXXX\n" + // once it finds the +x +z corner, it just stays there and makes a spiral staircase
                "XXXXXXXXX|XXXXXX   |XXXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXX   X|XXXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXX  XX|XXXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXX XXX|XXXXXXXXX|XXXXXXXXX\n" + // opposite face, staircase came from vvv down there
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXXXXXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXX XXXX\n" + // this is the opposite face - the staircase starts here and goes straight across up ^ to there
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXX XXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXX XXXX\n" +
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXXXXXXX\n";
        assertEquals(shouldBe, reportAllFourWalls(surface));
    }

    public static <T> void deterministicShuffle(Random rand, List<T> list) {
        int sz = list.size();
        for (int i = 0; i < sz; i++) {
            int j = rand.nextInt(sz);
            T atI = list.get(i);
            T atJ = list.get(j);
            list.set(i, atJ);
            list.set(j, atI);
        }
    }

    @Test
    public void testFullCubeRandom() {
        int SZ = 10;
        NavigableSurface surface = makeFlatSurface(SZ);
        BetterBlockPos someOtherBlock = new BetterBlockPos(0, 1, 0);
        List<BetterBlockPos> order = new ArrayList<>();
        for (int y = 0; y < SZ; y++) {
            for (int x = 0; x < SZ; x++) {
                for (int z = 0; z < SZ; z++) {
                    order.add(new BetterBlockPos(x, y, z));
                }
            }
        }
        deterministicShuffle(new Random(5021), order);

        fillSurfaceInOrderMaintainingConnection(surface, someOtherBlock, order);

        String shouldBe = "" +
                "XX XX  X |XX  X  XX|X  X X   | X X XXXX\n" +
                "   X XX X|  X    X |XX       |X X XXX  \n" +
                " XXXX   X|     XX  |         |XX   XXX \n" +
                "XXX XXXX |X  X X   |  X XXXXX|X    X XX\n" +
                " X     X | XX X X  |X  X     |XX  XX  X\n" +
                "    X   X|XXX  X XX|  X    XX|   X     \n" +
                "  XX XX  |X X    XX|  X X XX |X XX  X  \n" +
                "     XXXX|   XX X  |X  XXX   | X  XX X \n" +
                " XXXXX   |  X     X|   XX  X |    XX  X\n" +
                "XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXXXXXXX\n";
        assertEquals(shouldBe, reportAllFourWalls(surface));
    }
}
