package com.github.leijurv;

import baritone.api.utils.BetterBlockPos;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

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

    private NavigableSurface makeFlatSurface(int SZ) {
        NavigableSurface surface = new NavigableSurface(SZ, SZ, SZ);
        for (int x = 0; x < SZ; x++) {
            for (int z = 0; z < SZ; z++) {
                surface.placeBlock(new BetterBlockPos(x, 0, z));
            }
        }
        assertEquals(SZ * SZ, surface.requireSurfaceSize(0, 1, 0));
        return surface;
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
        outer:
        while (true) {
            for (BetterBlockPos candidate : iterationOrder) {
                if (surface.getBlock(candidate)) {
                    continue; // already placed
                }
                // let's try placing
                surface.placeBlock(candidate);
                if (surface.connected(candidate.up(), maintainConnectionTo)) {
                    // success, placed a block while retaining the path down to the ground
                    continue outer;
                }
                // fail :(
                surface.removeBlock(candidate);
            }
            return;
        }
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
                "XX                  |                   |                   |                  \n" +
                "XXX                 |                   |                   |                  \n" +
                "XXXX                |                   |                   |                  \n" +
                "XXXXX               |                   |                   |                  \n" +
                "XXXXXX              |                   |                   |                  \n" +
                "XXXXXXX             |                   |                   |                  \n" +
                "XXXXXXXX            |                   |                   |                  \n" +
                "XXXXXXXXX           |                   |                   |                  \n" +
                "XXXXXXXXXX          |                   |                   |                  \n" +
                "XXXXXXXXXXX         |                   |                   |                  \n" +
                "XXXXXXXXXXXX        |                   |                   |                  \n" +
                "XXXXXXXXXXXXX       |                   |                   |                  \n" +
                "XXXXXXXXXXXXXX      |                   |                   |                  \n" +
                "XXXXXXXXXXXXXXX     |                   |                   |                  \n" +
                "XXXXXXXXXXXXXXXX    |                   |                   |                  \n" +
                "XXXXXXXXXXXXXXXXX   |                   |                   |                  \n" +
                "XXXXXXXXXXXXXXXXXX  |                   |                   |                  \n" +
                "XXXXXXXXXXXXXXXXXXX |                   |                   |                  \n" +
                "XXXXXXXXXXXXXXXXXXXX|                   |                   |                  \n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n"; // double row is because we started with a flat surface, so there is another flat row behind this one to step back into
        assertEquals(shouldBe, reportAllFourWalls(surface));
    }

    @Test
    public void testCastleFourWalls() {
        // build four walls, but, never place a block that disconnects the surface
        // (we expect to see a zigzag cut out)
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
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXX   XXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXX   XXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXX   XXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXX   XXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXX   XXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXX   XXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXX   XXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXX   XXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXX   XXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXX   XXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXX   XXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XX   XXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|X   XXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|   XXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX |  XXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX  | XXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX  |XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXX X|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n" +
                "XXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXX\n";
        assertEquals(shouldBe, reportAllFourWalls(surface));
    }

    private String reportAllFourWalls(NavigableSurface surface) {
        StringBuilder report = new StringBuilder();
        for (int y = surface.sizeY - 1; y >= 0; y--) {
            // make a report of what all four walls look like
            for (int x = 0; x < surface.sizeX; x++) {
                report.append(surface.getBlock(new BetterBlockPos(x, y, 0)) ? 'X' : ' ');
            }
            report.append('|');
            // start at 1 not 0 so that we don't repeat the last iteration of the previous loop (that would make the report look bad because the staircase would repeat one column for no reason)
            for (int z = 1; z < surface.sizeZ; z++) {
                report.append(surface.getBlock(new BetterBlockPos(surface.sizeX - 1, y, z)) ? 'X' : ' ');
            }
            report.append('|');
            // same deal for starting at -2 rather than -1
            for (int x = surface.sizeX - 2; x >= 0; x--) {
                report.append(surface.getBlock(new BetterBlockPos(x, y, surface.sizeZ - 1)) ? 'X' : ' ');
            }
            report.append('|');
            // and same again
            for (int z = surface.sizeZ - 2; z > 0; z--) {
                report.append(surface.getBlock(new BetterBlockPos(0, y, z)) ? 'X' : ' ');
            }
            report.append('\n');
        }
        return report.toString();
    }
}
