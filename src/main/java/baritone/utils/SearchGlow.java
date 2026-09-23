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

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.calc.IPathFinder;
import baritone.api.utils.BetterBlockPos;
import com.mojang.blaze3d.vertex.PoseStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

// what the search looks like while it's thinking
//
// the search does a few hundred thousand nodes a second and we get to look at it sixty times a second,
// so drawing "the best path right now" and "the path to the newest node" honestly means drawing two unrelated paths every frame. that's the snapping
// this remembers what it drew last frame so that it can move on from it gradually:
// the best path grows toward its new tip, and a branch that got abandoned fades out from where it split off
// the newest node gets the same treatment but thinner and lighter: whatever we catch the search looking at grows out as a twig and fades away again
// every twig starts on something that's already on screen, the best path or an older twig, so it all hangs together as one tree
final class SearchGlow implements IRenderer {

    private static final int MAX_GHOSTS = 4;
    private static final long GHOST_LIFE = 450_000_000L;
    // in nodes. the tip of the best path is see through for this long, so that it has something to glide with
    private static final float TIP = 4;
    // how much of the gap to the real tip gets closed per second, more or less. 1 - e^(-10 / 60) is about 15% a frame
    private static final double GROWTH = 10;

    private static final int MAX_TWIGS = 48;
    // the search pokes at places that are nowhere near anything we've drawn, and a twig all the way out there is a long line across the screen
    // those get skipped. if the search really is headed that way then closer samples will turn up, and the tree gets there a twig at a time
    private static final int TWIG_NODES = 20;
    private static final float TWIG_TIP = 3;
    private static final float TWIG_WIDTH = 0.5F;
    private static final float TWIG_OPACITY = 0.65F;
    private static final long TWIG_GROW = 200_000_000L;
    private static final long TWIG_LIFE = 800_000_000L;
    // a new one every frame is more than anyone can look at, and every twig is a draw call
    private static final long TWIG_GAP = 30_000_000L;

    private final List<Ghost> ghosts = new ArrayList<>();
    private List<BetterBlockPos> best;
    // how many nodes of best are on screen so far
    private float shown;
    private long lastFrame;

    private final List<Twig> twigs = new ArrayList<>();
    private BetterBlockPos lastTwig;
    private long lastTwigTime;

    void render(PoseStack stack, Optional<? extends IPathFinder> search) {
        long now = System.nanoTime();
        double dt = Math.min((now - lastFrame) / 1.0E9, 0.1);
        lastFrame = now;

        List<BetterBlockPos> newBest = search.flatMap(IPathFinder::bestPathSoFar).map(IPath::positions).orElse(null);
        follow(newBest, now);
        if (best != null) {
            shown += (best.size() - shown) * (1 - Math.exp(-GROWTH * dt));
        }
        if (now - lastTwigTime >= TWIG_GAP) {
            search.flatMap(IPathFinder::pathToMostRecentNodeConsidered).map(IPath::positions).ifPresent(positions -> sprout(positions, now));
        }

        // twigs first, so that the best path is drawn over them and not the other way around
        drawTwigs(stack, now);
        Color color = settings.colorBestPathSoFar.value;
        for (Iterator<Ghost> it = ghosts.iterator(); it.hasNext(); ) {
            Ghost ghost = it.next();
            float age = (float) (now - ghost.died) / GHOST_LIFE;
            if (age >= 1) {
                it.remove();
                continue;
            }
            drawGrowing(stack, ghost.positions, ghost.from, ghost.shown, TIP, color, 1 - age, 1);
        }
        if (best != null) {
            drawGrowing(stack, best, 0, shown, TIP, color, 1, 1);
        }
    }

    private static void drawGrowing(PoseStack stack, List<BetterBlockPos> positions, int from, float shown, float tip, Color color, float opacity, float width) {
        // fadePath, except the fade sits on the tip of what's shown so far
        PathRibbon.draw(stack, positions, from, color, true, shown - tip - from, shown - from, 0.5D, 0, opacity, width, false, true, false);
    }

    private void follow(List<BetterBlockPos> newBest, long now) {
        if (best != null) {
            int split = newBest == null ? 0 : split(best, newBest);
            // if the old one is all still in there then the path only got longer, and growing takes care of that
            if (split < best.size() && split < shown) {
                if (ghosts.size() >= MAX_GHOSTS) {
                    ghosts.remove(0);
                }
                // the trunk is still being drawn by the new best. only the abandoned branch needs a ghost, from a node before the fork so it stays attached
                ghosts.add(new Ghost(best, Math.max(split - 1, 0), shown, now));
            }
            if (split < best.size()) {
                abandon(split, now);
            }
            // and the new branch grows out of the fork, it doesn't just appear
            shown = Math.min(shown, split);
        } else {
            shown = 0;
        }
        best = newBest;
    }

    // the twigs that were growing on the branch that just got dropped go down with it, on the ghost's schedule
    // otherwise they hang in the air for most of a second after the thing they were attached to is gone
    private void abandon(int split, long now) {
        for (Twig twig : twigs) {
            Twig root = twig;
            while (root.parent != null) {
                root = root.parent;
            }
            // a root's from is an index into the best path. split - 1 is the fork itself, which the new best still goes through
            if (root.abandoned || root.from >= split) {
                twig.abandoned = true;
                twig.keepUntil = Math.min(twig.keepUntil, now + GHOST_LIFE);
            }
        }
    }

    private static int split(List<BetterBlockPos> a, List<BetterBlockPos> b) {
        int max = Math.min(a.size(), b.size());
        int i = 0;
        while (i < max && a.get(i).equals(b.get(i))) {
            i++;
        }
        return i;
    }

    private void sprout(List<BetterBlockPos> positions, long now) {
        BetterBlockPos tip = positions.get(positions.size() - 1);
        if (tip.equals(lastTwig)) {
            // the search is between nodes (or done), don't stack twigs on the same spot
            return;
        }
        lastTwig = tip;
        // every one of these paths starts at the same node and they all come out of the same search tree, so any two of them are the same up to some fork
        // draw this one from the latest fork it has with anything on screen: no tracing over what's already there, and no loose ends
        int from = best == null ? 0 : split(best, positions) - 1;
        Twig parent = null;
        for (Twig twig : twigs) {
            if (twig.abandoned) {
                // it's on its way out, and anything that grabs on would keep it alive
                continue;
            }
            int fork = split(twig.positions, positions) - 1;
            // a twig is only on screen from its own fork onwards. sharing the part of it that was never drawn is no use
            if (fork >= twig.from && fork > from) {
                from = fork;
                parent = twig;
            }
        }
        from = Math.max(from, 0);
        if (parent == null && best != null && from > shown) {
            // it forks off a bit of the best path that hasn't grown in yet. it'd be hanging in the air until that catches up
            return;
        }
        int length = positions.size() - from;
        if (length < 2 || length > TWIG_NODES) {
            return;
        }
        lastTwigTime = now;
        if (twigs.size() >= MAX_TWIGS) {
            // whichever is closest to gone anyway. the oldest one might be holding half the tree up
            Twig dying = twigs.get(0);
            for (Twig twig : twigs) {
                if (twig.keepUntil < dying.keepUntil) {
                    dying = twig;
                }
            }
            twigs.remove(dying);
        }
        twigs.add(new Twig(positions, from, now, parent));
        // and whatever it's hanging off has to outlive it, all the way down to the trunk
        for (Twig twig = parent; twig != null; twig = twig.parent) {
            twig.keepUntil = now + TWIG_LIFE;
        }
    }

    private void drawTwigs(PoseStack stack, long now) {
        Color color = settings.colorMostRecentConsidered.value;
        for (Iterator<Twig> it = twigs.iterator(); it.hasNext(); ) {
            Twig twig = it.next();
            long age = now - twig.born;
            if (now >= twig.keepUntil) {
                it.remove();
                continue;
            }
            // grows out quick (easing off as it gets there), hangs around, and spends the second half of its life fading
            float grown = Math.min((float) age / TWIG_GROW, 1);
            grown = 1 - (1 - grown) * (1 - grown);
            float length = twig.positions.size() - twig.from + TWIG_TIP;
            float fade = Math.min(2 * (float) (twig.keepUntil - now) / TWIG_LIFE, 1);
            drawGrowing(stack, twig.positions, twig.from, twig.from + length * grown, TWIG_TIP, color, TWIG_OPACITY * fade, TWIG_WIDTH);
        }
    }

    private record Ghost(List<BetterBlockPos> positions, int from, float shown, long died) {}

    private static final class Twig {

        private final List<BetterBlockPos> positions;
        private final int from;
        private final long born;
        private final Twig parent;
        private long keepUntil;
        private boolean abandoned;

        private Twig(List<BetterBlockPos> positions, int from, long born, Twig parent) {
            this.positions = positions;
            this.from = from;
            this.born = born;
            this.parent = parent;
            this.keepUntil = born + TWIG_LIFE;
        }
    }
}
