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

package baritone.pathing.path;

import baritone.Baritone;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.movements.MovementAscend;
import baritone.pathing.movement.movements.MovementDiagonal;
import baritone.pathing.movement.movements.MovementParkour;
import baritone.pathing.movement.movements.MovementPillar;
import baritone.pathing.movement.movements.MovementTraverse;
import baritone.utils.BlockStateInterface;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

// one crossing of open water in a boat, owned by the PathExecutor. from the last block of land it
// places the boat, climbs in, rows the path's water positions, and at the last one breaks the boat so the
// item drops straight onto us, then hands the tick back to the normal movements. while a trip is running
// the executor doesn't tick movements at all, we just steer the boat between the path nodes ourselves
public final class BoatTrip implements Helper {

    public enum Result {
        CONTINUE,
        DONE,
        ABORT
    }

    private enum Phase {
        PLACE,
        BOARD,
        RIDE,
        SCUTTLE,
        PICKUP,
        EXIT
    }

    // vanilla boat physics, from AbstractBoat.controlBoat and floatBoat: left/right add a degree a tick to
    // the spin, forward adds 0.04 a tick of speed, and both decay by 0.9 a tick on water. so top speed is
    // 0.4 a tick, and after you let go, whatever spin or speed you have carries nine times over
    private static final double WATER_COAST = 9;
    // the boat tracks the middle of the drawn lane (smoothLane). a boat's velocity lags its heading badly,
    // nine tenths of it carries over every tick, so anything that steers on the heading error alone
    // overcorrects and hunts. instead the driver rolls the vanilla physics forward HORIZON ticks for each
    // thing it could do (a tap of each length in TAP_LENGTHS either way, or nothing, with and without
    // forward) and keeps whichever ends up nearest the lane point we'd expect to reach by then, with
    // penalties for wandering out of the lane, clipping a bank, pointing the wrong way and fidgeting with
    // the keys. re-picked every tick. checked against the exact equations offline first: two right angles
    // in 84 ticks with one clean swing wide each and no hunting (scratch boatsim.py)
    private static final int HORIZON = 12;
    private static final double LANE_PENALTY = 4;
    // no penalty this close to the middle of the lane, past it every block costs LANE_PENALTY
    private static final double CENTER_BAND = 0.8;
    private static final double COAST_PENALTY = 0.3;
    // a turn key costs a little and changing keys costs a little more, so on a straight "hold nothing"
    // wins unless a turn actually buys something. offline: key changes on a 40 block straight 8 -> 3,
    // corners unchanged
    private static final double TURN_PENALTY = 0.15;
    private static final double SWITCH_PENALTY = 0.1;
    // and where the bow ends up pointing matters too, or the hull fishtails while the boat slides
    // straight (seen in the telemetry: within a quarter block of the line, yaw swinging 40 degrees).
    // heading error against the lane at the goal, per 30 degrees, and leftover spin, per degree a tick
    private static final double HEADING_PENALTY = 1.0;
    private static final double SPIN_PENALTY = 0.05;
    // a held key is a 47 degree swing over the horizon, useless for an 8 degree fix. so the candidates
    // are taps of these many ticks, in each direction, then let go. offline: presses on a straight 35 -> 5
    private static final int[] TAP_LENGTHS = {1, 2, 4, HORIZON};
    // the hull's collision box is a 1.375 square that doesn't turn with the yaw, so in a tight passage a
    // corner of it clips the bank while the bow's fine. every other rollout step checks the box's four
    // corners against the blocks at water level and one up, and a clip costs this much per step
    private static final double HULL_HALF = 0.6875;
    private static final double CLIP_PENALTY = 2.0;
    // how far down the lane the point we're aiming to reach sits: what we'd cover in the horizon at our
    // speed, less a bit, and never closer than this
    private static final double AHEAD_MIN = 1.5;
    private static final double BOAT_ACCEL = 0.04;
    private static final double TURN_ACCEL = 0.005;
    // a bump into a bank: back off for this long, then carry on. give up after this many in one crossing
    private static final int BUMP_REVERSE_TICKS = 20;
    private static final int MAX_BUMPS = 6;
    // how many ticks the boat gets to make progress toward the next node before we call it run aground
    private static final int STUCK_TICKS = 80;
    private static final int PLACE_TIMEOUT = 60;
    private static final int BOARD_TIMEOUT = 60;
    private static final int SCUTTLE_TIMEOUT = 100;
    private static final int PICKUP_TIMEOUT = 60;
    private static final int EXIT_TIMEOUT = 30;
    private static final int CLICK_SPACING = 8;

    private final Baritone baritone;
    private final IPlayerContext ctx;
    private IPath path;
    private int start; // index of the position we set off from (land when placing, our own water block when resuming)
    private int end;   // index of the last water position, the trip hands back to the movement that starts there
    private Phase phase;
    private int ticksInPhase;
    private int waypoint;
    private double bestWaypointDist;
    private List<Vec3> lane;
    private int laneSeg = -1; // last lane segment we were on, -1 means search the whole thing
    private int reverseTicks;
    private int lastKey;
    private boolean lastFwd;
    private int bumps;
    private int ticksStuck;
    private AbstractBoat boat;
    private Vec3 placeAt;
    private int placeAttempts;
    private int boatsHeld;
    private boolean aborting;
    private boolean fromWater; // launching from a swim rather than the shore
    private String lastClick; // what the last placement click came back with, for the abort message

    private BoatTrip(Baritone baritone, IPath path, int start, int end, Phase phase) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
        this.path = path;
        this.start = start;
        this.end = end;
        this.phase = phase;
        this.waypoint = start + 1;
        this.bestWaypointDist = Double.MAX_VALUE;
        this.placeAt = phase == Phase.PLACE ? placementAim(0) : null;
        // how many we expect back in the inventory after the pickup. resuming means the boat's already out
        this.boatsHeld = countBoats(ctx.player().getInventory()) + (phase == Phase.PLACE ? 0 : 1);
    }

    // whether a trip should start at this path position, and the trip if so. Either we're standing on the
    // shore with enough open water ahead and a boat in the bag, or we're already sitting in a boat (a new
    // segment or a splice replaced the executor mid crossing) and just need to keep rowing
    public static BoatTrip plan(Baritone baritone, IPath path, int pos, int refusedUntil) {
        if (!Baritone.settings().allowBoats.value) {
            return null;
        }
        IPlayerContext ctx = baritone.getPlayerContext();
        if (ctx.player().getVehicle() instanceof AbstractBoat) {
            int end = runEnd(new BlockStateInterface(ctx), path, pos);
            if (end > pos) {
                return new BoatTrip(baritone, path, pos, end, Phase.RIDE);
            }
            // no water ahead, so the boat's job is done. scuttle it if it's floating on the path, but if the
            // user parked it on land and then asked us to go somewhere, just get out of their boat
            return new BoatTrip(baritone, path, pos, pos, MovementHelper.isWater(ctx, path.positions().get(pos)) ? Phase.SCUTTLE : Phase.EXIT);
        }
        if (pos <= refusedUntil || pos + 1 >= path.length()) {
            return null;
        }
        if (!ctx.playerFeet().equals(path.positions().get(pos))) {
            return null; // not on this node yet, the movement before us is still going
        }
        // launching from the shore is the nice case. but the path is allowed to jump or fall straight into
        // the water, and then we're swimming: that's fine too, place the boat right in front of us and swim
        // to it, as long as the head's above water so the item's raytrace can see the surface
        boolean fromWater;
        if (ctx.player().onGround() && !ctx.player().isInWater()) {
            fromWater = false;
        } else if (ctx.player().isInWater() && !ctx.player().isUnderWater() && MovementHelper.isWater(ctx, path.positions().get(pos))) {
            fromWater = true;
        } else {
            return null;
        }
        // from here on we're standing on the shore with water next, or swimming. every no gets a reason
        // in the debug chat, because "it just swam" with no explanation is a pain to chase
        boolean shore = fromWater || MovementHelper.isWater(ctx, path.positions().get(pos + 1));
        if (boatSlot(ctx.player().getInventory()) < 0) {
            if (shore) {
                explain(pos, "no boat in the inventory, swimming this one");
            }
            return null;
        }
        IMovement first = path.movements().get(pos);
        if (!fromWater && (first instanceof MovementAscend || first instanceof MovementParkour || first instanceof MovementPillar)) {
            if (shore) {
                explain(pos, "the hop into the water is a " + first.getClass().getSimpleName() + ", we'll get the boat out once we're in");
            }
            return null;
        }
        if (!fromWater && path.positions().get(pos).y - path.positions().get(pos + 1).y > 3) {
            if (shore) {
                explain(pos, "the water's too far down to reach with the boat item, we'll get the boat out once we're in");
            }
            return null;
        }
        int end = runEnd(new BlockStateInterface(ctx), path, pos);
        if (end - pos < Baritone.settings().boatMinWaterLength.value) {
            if (shore) {
                explain(pos, "only " + (end - pos) + " blocks of boatable water here (need " + Baritone.settings().boatMinWaterLength.value + "), swimming");
            }
            return null;
        }
        BoatTrip trip = new BoatTrip(baritone, path, pos, end, Phase.PLACE);
        trip.fromWater = fromWater;
        trip.placeAt = trip.placementAim(0);
        trip.logDebug("getting the boat out, " + (end - pos) + " blocks of water ahead. hoist the mainsail");
        return trip;
    }

    // every stretch of the path a boat would be used on, as [first land index, last water index] pairs, for
    // the renderer. Same rules as plan minus the "are we standing there right now" ones
    public static List<int[]> runs(Baritone baritone, IPath path) {
        List<int[]> runs = new ArrayList<>();
        if (!Baritone.settings().allowBoats.value || path == null) {
            return runs;
        }
        IPlayerContext ctx = baritone.getPlayerContext();
        if (ctx.player() == null || (boatSlot(ctx.player().getInventory()) < 0 && !(ctx.player().getVehicle() instanceof AbstractBoat))) {
            return runs;
        }
        BlockStateInterface bsi = new BlockStateInterface(ctx);
        int min = Baritone.settings().boatMinWaterLength.value;
        for (int i = 0; i + 1 < path.length(); i++) {
            int end = runEnd(bsi, path, i);
            if (end - i >= min) {
                runs.add(new int[]{i, end});
                i = end;
            }
        }
        return runs;
    }

    // the index of the last position in the row of boatable water that starts after pos, or pos itself if
    // the next position isn't water. boats don't do stairs so the water has to stay level after the first hop
    private static int runEnd(BlockStateInterface bsi, IPath path, int pos) {
        List<BetterBlockPos> positions = path.positions();
        int end = pos;
        for (int i = pos + 1; i < positions.size(); i++) {
            BetterBlockPos p = positions.get(i);
            BetterBlockPos prev = positions.get(i - 1);
            // the first block off the shore gets a pass on its neighbours entirely: the bank is always right
            // behind it and often beside it too (a corner of the beach), and placementAim puts the boat in
            // the second block when that's the case. everything after it has to be properly clear
            boolean ok = i == pos + 1
                    ? MovementHelper.canFloatBoat(bsi, p.x, p.y, p.z, 2, 2)
                    : MovementHelper.canFloatBoat(bsi, p.x, p.y, p.z);
            if (!ok) {
                break;
            }
            if (i > pos + 1) {
                IMovement m = path.movements().get(i - 1);
                if (p.y != positions.get(i - 1).y || !(m instanceof MovementTraverse || m instanceof MovementDiagonal)) {
                    break;
                }
            }
            end = i;
        }
        return end;
    }

    // plan() runs every tick we stand on the shore, the reason only needs saying once per shore
    private static int lastExplained = Integer.MIN_VALUE;

    private static void explain(int pos, String msg) {
        if (pos != lastExplained) {
            lastExplained = pos;
            Helper.HELPER.logDebug(msg);
        }
    }

    // half the width of the lane drawn around a boat run. only a picture: the steering aims at whatever
    // node it can see, this just shows where the boat's going to be
    public static final double LANE_HALF = 1.75;

    // the centerline the boat follows and the lane is drawn around. baritone's water paths are staircases
    // of straight and diagonal hops, and a boat following those literally weaves. so: pull the line taut
    // by relaxing every point toward the middle of its neighbours, forty rounds, but never let a point
    // stray more than LANE_SLACK from its own node. staircase wobble is smaller than that and vanishes,
    // a real bend is bigger and stays a rounded bend through the middle of the water. then two rounds of
    // chaikin corner cutting for density. ends stay put. offline: presses on a wavy lane 31 -> 10
    private static final double LANE_SLACK = 0.6;

    public static List<Vec3> smoothLane(List<BetterBlockPos> positions, int from, int to) {
        int n = to - from + 1;
        Vec3[] center = new Vec3[n];
        Vec3[] pts = new Vec3[n];
        for (int i = 0; i < n; i++) {
            BetterBlockPos p = positions.get(from + i);
            center[i] = new Vec3(p.x + 0.5, p.y, p.z + 0.5);
            pts[i] = center[i];
        }
        for (int round = 0; round < 40; round++) {
            for (int i = 1; i < n - 1; i++) {
                Vec3 mid = pts[i - 1].add(pts[i + 1]).scale(0.5);
                Vec3 pulled = pts[i].add(mid.subtract(pts[i]).scale(0.5));
                double dx = pulled.x - center[i].x;
                double dz = pulled.z - center[i].z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > LANE_SLACK) {
                    pulled = new Vec3(center[i].x + dx / d * LANE_SLACK, center[i].y, center[i].z + dz / d * LANE_SLACK);
                }
                pts[i] = pulled;
            }
        }
        List<Vec3> out = new ArrayList<>(n);
        for (Vec3 p : pts) {
            out.add(p);
        }
        for (int round = 0; round < 2 && out.size() > 2; round++) {
            List<Vec3> cut = new ArrayList<>(out.size() * 2);
            cut.add(out.get(0));
            for (int i = 0; i + 1 < out.size(); i++) {
                Vec3 a = out.get(i);
                Vec3 b = out.get(i + 1);
                cut.add(a.scale(0.75).add(b.scale(0.25)));
                cut.add(a.scale(0.25).add(b.scale(0.75)));
            }
            cut.add(out.get(out.size() - 1));
            out = cut;
        }
        return out;
    }

    private static int boatSlot(Inventory inv) {
        // hotbar first so we don't shuffle the inventory around for nothing
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i).getItem() instanceof BoatItem) {
                return i;
            }
        }
        return -1;
    }

    private static int countBoats(Inventory inv) {
        int n = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i).getItem() instanceof BoatItem) {
                n += inv.items.get(i).getCount();
            }
        }
        return n;
    }

    public int endPosition() {
        return end;
    }

    public int startPosition() {
        return start;
    }

    // the node the boat is at right now, for the executor's own position while we're driving
    public int currentPosition() {
        return Math.max(start, Math.min(end, waypoint));
    }

    // the executor got rebuilt on a new path: a splice keeps our indices and tacks more path on the end,
    // cutting the executed history off the front shifts them all by delta. either way the run might now
    // carry on past where it used to end, and if we didn't notice we'd scuttle the boat in the middle of
    // the lake and swim the rest
    public void rebase(IPath newPath, int delta) {
        path = newPath;
        start -= delta;
        end -= delta;
        waypoint -= delta;
        if (phase == Phase.PLACE || phase == Phase.BOARD || phase == Phase.RIDE) {
            // end - 1 rather than end so the level water check covers the hop onto positions[end + 1]
            end = Math.max(end, runEnd(new BlockStateInterface(ctx), path, end - 1));
        }
        lane = null;
        laneSeg = -1;
    }

    public boolean safeToCancel() {
        // coasting in a boat is fine to pause in, being halfway through placing or breaking one is not
        return phase == Phase.RIDE;
    }

    public Result tick() {
        ticksInPhase++;
        baritone.getInputOverrideHandler().clearAllKeys();
        switch (phase) {
            case PLACE:
                return place();
            case BOARD:
                return board();
            case RIDE:
                return ride();
            case SCUTTLE:
                return scuttle();
            case PICKUP:
                return pickup();
            case EXIT:
                return exit();
            default:
                throw new IllegalStateException();
        }
    }

    private void enter(Phase next) {
        phase = next;
        ticksInPhase = 0;
    }

    private Result abort(String why) {
        logDebug(why);
        if (ctx.player().getVehicle() instanceof AbstractBoat) {
            // get out first, the movements can't do anything from inside a boat
            aborting = true;
            enter(Phase.EXIT);
            return Result.CONTINUE;
        }
        return Result.ABORT;
    }

    private Vec3 placementAim(int attempt) {
        List<BetterBlockPos> positions = path.positions();
        BetterBlockPos land = positions.get(start);
        BetterBlockPos water = positions.get(start + 1);
        // the first water block is the one next to the bank, and its sides are the one place the boat's
        // 1.375 width isn't checked. every node after it is clear all round, so aim out over the lane at
        // the furthest of the next couple of nodes the item can still reach, and swim out to the boat.
        // just under the surface so the ray comes in through the top face, which is where vanilla puts it
        Vec3 eye = ctx.playerHead();
        double reach = ctx.player().blockInteractionRange() - 0.4;
        for (int i = Math.min(end, start + 3); i >= start + 2; i--) {
            BetterBlockPos p = positions.get(i);
            Vec3 aim = new Vec3(p.x + 0.5, p.y + 0.8, p.z + 0.5);
            if (aim.distanceTo(eye) < reach) {
                return nudge(aim, land, p, attempt);
            }
        }
        // the water's too far below us to reach that far out. settle for the first block, past its middle
        double dx = water.x - land.x;
        double dz = water.z - land.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        return nudge(new Vec3(land.x + 0.5 + dx / len * 1.4, water.y + 0.8, land.z + 0.5 + dz / len * 1.4), land, water, attempt);
    }

    // if a spot keeps getting refused, wander the aim along the launch direction between attempts. the
    // boat item won't tell us what it hit, only that it did
    private static Vec3 nudge(Vec3 aim, BetterBlockPos from, BetterBlockPos to, int attempt) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        double along = new double[]{0, 0.4, -0.4, 0.8, -0.8}[attempt % 5];
        return len == 0 ? aim : aim.add(dx / len * along, 0, dz / len * along);
    }

    // how far past the middle of the shore block we are, toward the water. positive means we're leaning
    // over the edge, which is where the boat's box wants to be
    private double edgeLean() {
        List<BetterBlockPos> positions = path.positions();
        BetterBlockPos land = positions.get(start);
        BetterBlockPos water = positions.get(start + 1);
        double dx = water.x - land.x;
        double dz = water.z - land.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        return ((ctx.player().position().x - land.x - 0.5) * dx + (ctx.player().position().z - land.z - 0.5) * dz) / len;
    }

    private Result place() {
        // look for the boat before looking for the item: with a single boat in the bag, the click that
        // places it also empties the slot, and the entity shows up a tick or two after that
        AbstractBoat placed = findBoat(placeAt, 2.5);
        if (placed != null) {
            boat = placed;
            logDebug("ahoy! we can sail the seven seas!");
            enter(Phase.BOARD);
            return Result.CONTINUE;
        }
        if (ticksInPhase > PLACE_TIMEOUT) {
            // one line about why, not one per click. FAIL from the item means the hull's box hit something
            return abort("the boat won't go in the water, guess we're swimming. " + (lastClick == null ? "never got a clear shot at it" : lastClick));
        }
        Inventory inv = ctx.player().getInventory();
        int slot = boatSlot(inv);
        if (slot < 0) {
            if (placeAttempts == 0) {
                return abort("the boat's gone. guess we're swimming");
            }
            return Result.CONTINUE; // that was our last one and it's on its way into the world, hopefully
        }
        if (!Inventory.isHotbarSlot(slot)) {
            baritone.getInventoryBehavior().attemptToPutOnHotbar(slot, i -> false);
            return Result.CONTINUE;
        }
        inv.selected = slot;
        Rotation rot = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), placeAt, ctx.playerRotations());
        baritone.getLookBehavior().updateTarget(rot, true);
        // the item raytraces from the client rotation, and CLIENT mode only sets that once the tick after
        // the target goes in, so give it a tick. with blockFreeLook the client never turns at all, so after
        // a while just try anyway and let the timeout sort it out
        boolean aimed = Math.abs(Mth.wrapDegrees(ctx.player().getYRot() - rot.getYaw())) < 3 && Math.abs(ctx.player().getXRot() - rot.getPitch()) < 3;
        if (!fromWater && edgeLean() > -0.1) {
            // sprint momentum parks us right on the lip of the shore block. shuffle back to the middle so our
            // own hitbox isn't the thing the boat lands on. we're looking at the water, so back is away from it
            baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_BACK, true);
            return Result.CONTINUE;
        }
        if (ticksInPhase > 1 && (aimed || ticksInPhase > 10) && ticksInPhase % CLICK_SPACING == 0) {
            // straight to useItem, not the place helper: that one wants a block under the crosshair and the
            // crosshair raytrace ignores fluids, so over deep water it sees nothing and never clicks
            InteractionResult result = ctx.playerController().processRightClick(ctx.player(), ctx.world(), InteractionHand.MAIN_HAND);
            lastClick = "attempt " + (placeAttempts + 1) + " came back " + result.getClass().getSimpleName().toLowerCase();
            placeAt = placementAim(++placeAttempts);
        }
        return Result.CONTINUE;
    }

    private Result board() {
        if (ctx.player().getVehicle() == boat) {
            logDebug("all aboard. row row row your boat");
            enter(Phase.RIDE);
            return Result.CONTINUE;
        }
        if (boat == null || boat.isRemoved()) {
            return abort("the boat vanished before we got in. suspicious");
        }
        if (ticksInPhase > BOARD_TIMEOUT) {
            return abort("it won't let us in the boat, swimming instead");
        }
        // same deal as breaking a block: put the crosshair on the thing (forced, so the client actually
        // turns) and only click once the view ray really crosses the hull. the middle of the hull is the
        // safest bit to aim at, but from up on a bank the rim can be all that's visible, so swap between
        // the two every second until one of them sticks
        Vec3 hull = boat.position().add(0, (ticksInPhase / 20) % 2 == 0 ? boat.getBbHeight() / 2 : boat.getBbHeight() * 0.9, 0);
        Rotation rot = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), hull, ctx.playerRotations());
        baritone.getLookBehavior().updateTarget(rot, true);
        double dist = ctx.player().distanceTo(boat);
        if (dist > 2.5) {
            baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
            return Result.CONTINUE;
        }
        // baritone's objectMouseOver is a block only raytrace, entities never show up in it, so the entity
        // half of vanilla's pick is done here by hand: clip the view ray against the hull and make sure no
        // block is in the way first
        HitResult over = ctx.objectMouseOver();
        Vec3 eye = ctx.player().getEyePosition();
        Vec3 reachEnd = eye.add(ctx.player().getViewVector(1.0F).scale(ctx.player().entityInteractionRange()));
        Vec3 hullHit = boat.getBoundingBox().inflate(boat.getPickRadius()).clip(eye, reachEnd).orElse(null);
        boolean onBoat = hullHit != null && (over == null || over.getType() == HitResult.Type.MISS || over.getLocation().distanceToSqr(eye) > hullHit.distanceToSqr(eye));
        if (ticksInPhase % 5 == 1 && onBoat) {
            // no sneaking here or the boat just says no (secondary use), and the packet carries our shift
            // state so the server would refuse the ride too
            EntityHitResult hit = new EntityHitResult(boat, hullHit);
            if (!ctx.minecraft().gameMode.interactAt(ctx.player(), boat, hit, InteractionHand.MAIN_HAND).consumesAction()) {
                ctx.minecraft().gameMode.interact(ctx.player(), boat, InteractionHand.MAIN_HAND);
            }
        }
        return Result.CONTINUE;
    }

    private Result ride() {
        Entity vehicle = ctx.player().getVehicle();
        if (!(vehicle instanceof AbstractBoat)) {
            return abort("we fell out of the boat somehow. swimming");
        }
        boat = (AbstractBoat) vehicle;
        List<BetterBlockPos> positions = path.positions();
        Vec3 at = boat.position();
        if (ticksInPhase > (end - start) * 15 + 200) {
            return abort("this crossing is taking forever, abandon ship");
        }
        if (lane == null) {
            lane = smoothLane(positions, start, end);
            laneSeg = -1;
        }
        // where are we on the lane: project onto the segments around the last one (boats don't teleport),
        // all of them the first time. closest one gives how far along and how far off the middle we are
        int lo = laneSeg < 0 ? 0 : Math.max(0, laneSeg - 4);
        int hi = laneSeg < 0 ? lane.size() - 1 : Math.min(lane.size() - 1, laneSeg + 12);
        int bestSeg = lo;
        double bestT = 0;
        double lateral = Double.MAX_VALUE;
        for (int seg = lo; seg < hi; seg++) {
            Vec3 a = lane.get(seg);
            Vec3 b = lane.get(seg + 1);
            double abx = b.x - a.x;
            double abz = b.z - a.z;
            double len2 = abx * abx + abz * abz;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((at.x - a.x) * abx + (at.z - a.z) * abz) / len2));
            double px = a.x + abx * t;
            double pz = a.z + abz * t;
            double d = Math.sqrt((at.x - px) * (at.x - px) + (at.z - pz) * (at.z - pz));
            if (d < lateral) {
                lateral = d;
                bestSeg = seg;
                bestT = t;
            }
        }
        laneSeg = bestSeg;
        // the lane has four points per node after the corner cutting, so this is roughly the node we're at
        waypoint = Math.min(end, start + (bestSeg + 1) / 4);
        double progress = bestSeg + bestT;

        Vec3 motion = boat.getDeltaMovement();
        double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        double coast = WATER_COAST * speed;
        Vec3 last = lane.get(lane.size() - 1);
        double endDist = Math.sqrt((last.x - at.x) * (last.x - at.x) + (last.z - at.z) * (last.z - at.z));

        // ran into something (a bank we clipped, a lily pad, whoever knows): back off the thing for a
        // moment so the bow can swing free, then carry on. only give up after a lot of that
        if (reverseTicks > 0) {
            reverseTicks--;
            baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_BACK, true);
            return Result.CONTINUE;
        }
        if (boat.horizontalCollision && speed < 0.05 && endDist > 1.5) {
            if (++bumps > MAX_BUMPS) {
                return abort("we keep running aground. abandon ship");
            }
            reverseTicks = BUMP_REVERSE_TICKS;
            ticksStuck = 0;
            return Result.CONTINUE;
        }
        if (bestWaypointDist == Double.MAX_VALUE || progress > bestWaypointDist + 0.05) {
            bestWaypointDist = progress;
            ticksStuck = 0;
        } else if (++ticksStuck > STUCK_TICKS) {
            return abort("we've run aground. abandon ship");
        }

        // where we'd like to be after the horizon: down the lane by about what we'll travel, and then
        // try everything we could hold and see which one gets closest without leaving the lane
        double ahead = Math.max(AHEAD_MIN, speed * HORIZON * 0.8 + 1.0);
        Vec3 goal = pointAlong(bestSeg, bestT, ahead);
        int bestKey = 0;
        boolean bestFwd = false;
        double bestScore = Double.MAX_VALUE;
        float goalHeading = headingAlong(bestSeg, bestT, ahead);
        BlockStateInterface bsi = new BlockStateInterface(ctx);
        int waterY = positions.get(end).y;
        for (int dir = -1; dir <= 1; dir++) {
            for (int taps : dir == 0 ? new int[]{0} : TAP_LENGTHS) {
                for (int fwd = 0; fwd <= 1; fwd++) {
                    double[] state = {at.x, at.z, boat.getYRot(), Mth.wrapDegrees(boat.getYRot() - boat.yRotO), motion.x, motion.z};
                    double worstLateral = 0;
                    int clips = 0;
                    // the boat ticks before its passenger, so what we press now reaches it next tick. the
                    // first step of every rollout is last tick's choice, which is already on its way
                    stepBoat(state, lastKey, lastFwd);
                    for (int k = 0; k < HORIZON; k++) {
                        stepBoat(state, k < taps ? dir : 0, fwd == 1);
                        worstLateral = Math.max(worstLateral, lateralAt(state[0], state[1], bestSeg));
                        if ((k & 1) == 1 && hullClips(bsi, state[0], state[1], waterY)) {
                            clips++;
                        }
                    }
                    double score = Math.sqrt((state[0] - goal.x) * (state[0] - goal.x) + (state[1] - goal.z) * (state[1] - goal.z))
                            + LANE_PENALTY * Math.max(0, worstLateral - CENTER_BAND)
                            + CLIP_PENALTY * clips
                            + (fwd == 1 ? 0 : COAST_PENALTY)
                            + (dir != 0 ? TURN_PENALTY : 0)
                            + (dir != lastKey ? SWITCH_PENALTY : 0)
                            + HEADING_PENALTY * Math.abs(Mth.wrapDegrees((float) state[2] - goalHeading)) / 30
                            + SPIN_PENALTY * Math.abs(state[3]);
                    if (score < bestScore) {
                        bestScore = score;
                        bestKey = dir;
                        bestFwd = fwd == 1;
                    }
                }
            }
        }
        if (endDist < coast + 0.4) {
            bestFwd = false; // we'd slide right past the last node and into the shore
        }
        lastKey = bestKey;
        lastFwd = bestFwd;
        if (bestKey != 0) {
            baritone.getInputOverrideHandler().setInputForceState(bestKey > 0 ? Input.MOVE_RIGHT : Input.MOVE_LEFT, true);
        }
        if (bestFwd) {
            baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
        }
        // no look target while riding. the boat steers off its own yaw, not ours, and it clamps the
        // passenger's head to the hull every tick, so any rotation we set just fights it and the camera
        // shakes. the user gets to look wherever they like

        if (endDist < 1.0 && speed < 0.08) {
            return arrive();
        }
        if (endDist < 1.6 && speed < 0.02 && ticksInPhase > 10) {
            return arrive(); // nosed into the shore a little early, close enough
        }
        return Result.CONTINUE;
    }

    // the point dist blocks further along the lane from the projection (seg, t), clamped to the last point
    private Vec3 pointAlong(int seg, double t, double dist) {
        Vec3 a = lane.get(seg);
        Vec3 b = lane.get(seg + 1);
        Vec3 p = a.add(b.subtract(a).scale(t));
        double remaining = dist;
        while (true) {
            b = lane.get(seg + 1);
            double d = Math.sqrt((b.x - p.x) * (b.x - p.x) + (b.z - p.z) * (b.z - p.z));
            if (d >= remaining && d > 0) {
                return p.add(b.subtract(p).scale(remaining / d));
            }
            if (seg + 2 >= lane.size()) {
                return b;
            }
            remaining -= d;
            p = b;
            seg++;
        }
    }

    // the direction the lane runs at the point dist blocks along it from (seg, t), as a yaw
    private float headingAlong(int seg, double t, double dist) {
        Vec3 a = lane.get(seg);
        Vec3 b = lane.get(seg + 1);
        Vec3 p = a.add(b.subtract(a).scale(t));
        double remaining = dist;
        while (seg + 2 < lane.size()) {
            b = lane.get(seg + 1);
            double d = Math.sqrt((b.x - p.x) * (b.x - p.x) + (b.z - p.z) * (b.z - p.z));
            if (d >= remaining) {
                break;
            }
            remaining -= d;
            p = b;
            seg++;
        }
        Vec3 c = lane.get(seg);
        Vec3 n = lane.get(seg + 1);
        return (float) Math.toDegrees(Math.atan2(-(n.x - c.x), n.z - c.z));
    }

    // would the hull's box, centered here, overlap anything solid? the four corners, at water level and
    // the block above. the box is axis aligned in vanilla too, so this is exactly what it hits
    private static boolean hullClips(BlockStateInterface bsi, double x, double z, int y) {
        for (int cx = -1; cx <= 1; cx += 2) {
            for (int cz = -1; cz <= 1; cz += 2) {
                int bx = Mth.floor(x + cx * HULL_HALF);
                int bz = Mth.floor(z + cz * HULL_HALF);
                if (!MovementHelper.boatCanOverlap(bsi.get0(bx, y, bz)) || !MovementHelper.boatCanOverlap(bsi.get0(bx, y + 1, bz))) {
                    return true;
                }
            }
        }
        return false;
    }

    // one tick of vanilla boat physics on water, driven by us: floatBoat's friction, then controlBoat's
    // input, then the move. state is x, z, yaw, spin, vx, vz. key is -1 left, 0, 1 right
    private static void stepBoat(double[] s, int key, boolean forward) {
        s[3] = s[3] * 0.9 + key;
        s[2] += s[3];
        double thrust = (forward ? BOAT_ACCEL : 0) + (key != 0 && !forward ? TURN_ACCEL : 0);
        double yaw = Math.toRadians(s[2]);
        s[4] = s[4] * 0.9 - Math.sin(yaw) * thrust;
        s[5] = s[5] * 0.9 + Math.cos(yaw) * thrust;
        s[0] += s[4];
        s[1] += s[5];
    }

    // how far a point is from the lane's centerline, looking at the segments around the one we're on
    private double lateralAt(double x, double z, int nearSeg) {
        double best = Double.MAX_VALUE;
        for (int seg = Math.max(0, nearSeg - 2); seg < Math.min(lane.size() - 1, nearSeg + 20); seg++) {
            Vec3 a = lane.get(seg);
            Vec3 b = lane.get(seg + 1);
            double abx = b.x - a.x;
            double abz = b.z - a.z;
            double len2 = abx * abx + abz * abz;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - a.x) * abx + (z - a.z) * abz) / len2));
            double px = a.x + abx * t;
            double pz = a.z + abz * t;
            best = Math.min(best, (x - px) * (x - px) + (z - pz) * (z - pz));
        }
        return Math.sqrt(best); // 72 of these a tick, one root each instead of twenty
    }

    private Result arrive() {
        if (end == path.length() - 1) {
            // the path ends on the water, not the shore. either the goal's out here or the next segment
            // hasn't been spliced on yet. stay in the boat, the next executor will find us in it and row on
            logDebug("segment ends on the water, staying aboard");
            return Result.DONE;
        }
        // creative just poofs the boat on the first hit, no drop, which is fine: there's more where that came from
        if (Baritone.settings().boatPickup.value) {
            logDebug("land ho! scuttling the boat so we can take it with us");
            enter(Phase.SCUTTLE);
        } else {
            logDebug("land ho! leaving the boat here");
            enter(Phase.EXIT);
        }
        return Result.CONTINUE;
    }

    private Result scuttle() {
        Entity vehicle = ctx.player().getVehicle();
        if (!(vehicle instanceof AbstractBoat)) {
            // it broke and dumped us in the water, and the item dropped right where we're floating
            enter(Phase.PICKUP);
            return Result.CONTINUE;
        }
        if (ticksInPhase > SCUTTLE_TIMEOUT) {
            logDebug("this boat is unsinkable apparently. leaving it");
            enter(Phase.EXIT);
            return Result.CONTINUE;
        }
        // punching the boat from inside it: the server adds a flat 10 damage a hit regardless of the swing
        // and breaks it past 40, so five hits. spaced out a bit so it doesn't look like an autoclicker
        if (ticksInPhase % 4 == 1) {
            ctx.minecraft().gameMode.attack(ctx.player(), vehicle);
            ctx.player().swing(InteractionHand.MAIN_HAND);
        }
        return Result.CONTINUE;
    }

    private Result pickup() {
        // the wreck drops us on top of where the hull was, half a block above the water, and the movement
        // we hand back to wants our feet in the water node. let ourselves sink in first or it spends the
        // next second complaining about the wrong y coordinate
        boolean settled = ctx.playerFeet().y <= path.positions().get(end).y;
        if (settled && countBoats(ctx.player().getInventory()) >= boatsHeld) {
            logDebug("boat's back in the bag");
            return Result.DONE;
        }
        if (ticksInPhase > PICKUP_TIMEOUT || (settled && ticksInPhase > 12 && findBoatItem(ctx.player().position(), 3.0) == null)) {
            logDebug("the boat item drifted off, so long boat");
            return Result.DONE;
        }
        // items have a ten tick pickup delay after they drop, just float here and wait for it
        return Result.CONTINUE;
    }

    private Result exit() {
        if (!(ctx.player().getVehicle() instanceof AbstractBoat)) {
            // same as pickup: the dismount drops us above the water, let the feet find the water before
            // the movements get us back, or they spend a second going on about the wrong y coordinate
            boolean settled = ctx.player().onGround() || ctx.player().isInWater();
            if (!settled && ticksInPhase <= EXIT_TIMEOUT + 40) {
                return Result.CONTINUE;
            }
            return aborting ? Result.ABORT : Result.DONE;
        }
        if (ticksInPhase > EXIT_TIMEOUT) {
            logDebug("can't get out of the boat?? giving up on the path");
            return Result.ABORT;
        }
        baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
        return Result.CONTINUE;
    }

    private AbstractBoat findBoat(Vec3 near, double radius) {
        AbstractBoat best = null;
        double bestDist = Double.MAX_VALUE;
        for (AbstractBoat candidate : ctx.world().getEntitiesOfClass(AbstractBoat.class, new AABB(near, near).inflate(radius))) {
            if (!candidate.getPassengers().isEmpty()) {
                continue;
            }
            double d = candidate.position().distanceToSqr(near);
            if (d < bestDist) {
                bestDist = d;
                best = candidate;
            }
        }
        return best;
    }

    private ItemEntity findBoatItem(Vec3 near, double radius) {
        for (ItemEntity item : ctx.world().getEntitiesOfClass(ItemEntity.class, new AABB(near, near).inflate(radius))) {
            if (item.getItem().getItem() instanceof BoatItem) {
                return item;
            }
        }
        return null;
    }

    private static double flatDist(Vec3 from, BetterBlockPos to) {
        double dx = to.x + 0.5 - from.x;
        double dz = to.z + 0.5 - from.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
