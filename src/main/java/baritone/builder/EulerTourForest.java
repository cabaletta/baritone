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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class EulerTourForest {

    static long parentWalks;
    static long parentCalls;
    // https://web.stanford.edu/class/archive/cs/cs166/cs166.1166/lectures/17/Small17.pdf
    // https://u.cs.biu.ac.il/~rodittl/p723-holm.pdf
    // https://web.archive.org/web/20180725100607/https://infoscience.epfl.ch/record/99353/files/HenzingerK99.pdf
    // https://en.wikipedia.org/wiki/Dynamic_connectivity#The_Level_structure

    public final BSTNode[] loopbacks; // a (v,v) fake edge is created per vertex and maintained at the appropriate location in the tree, to allow fast lookups of where "v" is, without having to rely on the presence or absence of tree edges connected to v

    public EulerTourForest(int n) {
        this.loopbacks = IntStream.range(0, n).mapToObj(SplayNode::new).toArray(BSTNode[]::new);
    }

    public TreeEdge link(int vertA, int vertB) {
        if (connected(vertA, vertB)) {
            throw new IllegalStateException();
        }
        BSTNode outgoing = new SplayNode(vertA, vertB);
        BSTNode incoming = new SplayNode(vertB, vertA);
        BSTNode.barrelRollToLowest(loopbacks[vertA]);
        BSTNode.barrelRollToLowest(loopbacks[vertB]);
        BSTNode.concatenate(loopbacks[vertA], outgoing); // (a,a) ... (a,b)
        BSTNode.concatenate(outgoing, loopbacks[vertB]); // (a,a) ... (a,b) (b,b) ...
        BSTNode.concatenate(loopbacks[vertB], incoming); // (a,a) ... (a,b) (b,b) ... (b,a)
        return new TreeEdge(incoming, outgoing);
    }

    public void cut(TreeEdge edge) {
        if (edge.owner() != this) {
            throw new IllegalArgumentException();
        }
        if (edge.cut) {
            return;
        }
        edge.cut = true;
        BSTNode outgoing = edge.left;
        BSTNode incoming = edge.right;
        if (incoming.src != outgoing.dst || incoming.dst != outgoing.src || outgoing == incoming || incoming.src == incoming.dst) {
            throw new IllegalStateException();
        }
        if (!connected(incoming.src, incoming.dst)) {
            throw new IllegalStateException();
        }
        BSTNode.barrelRollToLowest(outgoing);
        BSTNodePair disconnected = incoming.disconnect(Direction.RIGHT);
        if (disconnected.left.walkDescendant(Direction.LEFT) != outgoing || disconnected.right.walkDescendant(Direction.LEFT) != incoming) {
            throw new IllegalStateException();
        }
        if (loopbacks[incoming.src].walkAncestor() != disconnected.left || loopbacks[outgoing.src].walkAncestor() != disconnected.right) {
            throw new IllegalStateException();
        }
        outgoing.remove();
        incoming.remove();
    }

    public boolean connected(int vertA, int vertB) {
        return loopbacks[vertA].walkAncestor() == loopbacks[vertB].walkAncestor();
    }

    public int size(int vert) {
        return loopbacks[vert].walkAncestor().loopbackSize;
    }

    public int[] walk(int vert) {
        BSTNode root = loopbacks[vert].walkAncestor();
        int[] ret = new int[root.loopbackSize];
        int[] idx = {0};
        root.walk(node -> {
            if (node.isLoopback()) {
                ret[idx[0]++] = node.src;
            }
        });
        return ret;
    }

    // redblacknode impl deleted here

    public static class SplayNode extends BSTNode {

        private SplayNode() {
            super();
        }

        private SplayNode(int same) {
            super(same);
        }

        private SplayNode(int src, int dst) {
            super(src, dst);
        }

        public void splay() {
            while (parent != null) {
                BSTNode grandparent = parent.parent;
                Direction myDir = whichChildAmI();
                if (grandparent != null) {
                    Direction parentDir = parent.whichChildAmI();
                    if (myDir == parentDir) {
                        // see "Zig-zig step" of https://en.wikipedia.org/wiki/Splay_tree
                        grandparent.splayRotate(parentDir);
                        parent.splayRotate(myDir);
                    } else {
                        // see "Zig-zag step" of https://en.wikipedia.org/wiki/Splay_tree
                        parent.splayRotate(myDir);
                        grandparent.splayRotate(parentDir);
                    }
                } else {
                    parent.splayRotate(myDir);
                    if (parent != null) {
                        throw new IllegalStateException();
                    }
                }
            }
        }

        @Override
        protected BSTNode concatenateRoots(BSTNode right) {
            if (this.parent != null || right.parent != null || this == right) {
                throw new IllegalStateException();
            }
            SplayNode newRoot = (SplayNode) right.walkDescendant(Direction.LEFT);
            newRoot.splay();
            // newRoot is now the root of a splay tree containing all of right
            // and, it is the LOWEST value of right, and left is assumed to be less than it, so now we can just attach left as its left child
            // (since it is the lowest value of right, it cannot possibly have any left child already)
            if (newRoot.getChild(Direction.LEFT) != null) {
                throw new IllegalStateException();
            }
            newRoot.setChild(Direction.LEFT, this);
            newRoot.childUpdated();
            return newRoot;
        }

        @Override
        protected BSTNodePair disconnect(Direction remainOnSide) {
            splay(); // SIGNIFICANTLY easier to split the tree in half, if we are the root node
            BSTNode left = this;
            BSTNode right = this;
            // simple detach of one side
            if (remainOnSide == Direction.LEFT) {
                right = rightChild;
                rightChild = null;
                childUpdated();
                if (right != null) {
                    right.sizeMustBeAccurate();
                    right.parent = null;
                }
            } else {
                left = leftChild;
                leftChild = null;
                childUpdated();
                if (left != null) {
                    left.sizeMustBeAccurate();
                    left.parent = null;
                }
            }
            return new BSTNodePair(left, right);
        }

        public void remove() {
            splay();
            if (leftChild != null) {
                leftChild.parent = null;
            }
            if (rightChild != null) {
                rightChild.parent = null;
            }
            if (leftChild != null && rightChild != null) {
                leftChild.concatenateRoots(rightChild);
            }
        }
    }

    public abstract static class BSTNode {

        protected final int src;
        protected final int dst;

        protected BSTNode leftChild;
        protected BSTNode rightChild;
        protected BSTNode parent;

        protected int loopbackSize;

        private BSTNode() {
            this(-1);
        }

        private BSTNode(int same) {
            this.src = same;
            this.dst = same;
            this.loopbackSize = 1;
        }

        private BSTNode(int src, int dst) {
            if (src == dst) {
                throw new IllegalArgumentException();
            }
            this.src = src;
            this.dst = dst;
            this.loopbackSize = 0;
        }

        protected void splayRotate(Direction dir) {
            rotateSecret(dir);
        }

        protected void redBlackRotate(Direction dir) {
            rotateSecret(dir.opposite()); // i guess they just use different conventions?
        }

        private void rotateSecret(Direction dir) {
            // promote my "dir" child to my level, swap myself down to that level

            // see "Zig step" of https://en.wikipedia.org/wiki/Splay_tree
            BSTNode child = this.getChild(dir);
            BSTNode replacementChild = child.getChild(dir.opposite()); // stays at the same level, is just rotated to the other side of the tree
            this.setChild(dir, replacementChild);
            if (parent == null) {
                child.parent = null;
            } else {
                parent.setChild(whichChildAmI(), child);
            }
            child.setChild(dir.opposite(), this); // e.g. my left child now has me as their right child
            childUpdated();
            parent.childUpdated();
        }

        public static BSTNode concatenate(BSTNode left, BSTNode right) {
            if (left == null) {
                throw new IllegalStateException();
            }
            if (right == null) {
                return left;
            }
            return left.walkAncestor().concatenateRoots(right.walkAncestor());
        }

        protected void afterSwap(BSTNode other) {

        }

        protected void swapLocationWith(BSTNode other) {
            if (other == this) {
                throw new IllegalStateException();
            }
            if (other.parent == this) {
                other.swapLocationWith(this);
                return;
            }
            if (parent == other) {
                //            grandpa
                //         other
                //    this   otherChild
                // left  right
                // and we want that to become
                //            grandpa
                //         this
                //    other   otherChild
                // left  right
                Direction dir = whichChildAmI(); // LEFT, in the above example
                BSTNode otherChild = other.getChild(dir.opposite());
                BSTNode left = leftChild;
                BSTNode right = rightChild;
                if (other.parent == null) { // grandpa
                    parent = null;
                } else {
                    other.parent.setChild(other.whichChildAmI(), this);
                }
                setChild(dir, other);
                setChild(dir.opposite(), otherChild);
                other.setChild(Direction.LEFT, left);
                other.setChild(Direction.RIGHT, right);
                other.childUpdated();
                childUpdated();
            } else {
                Direction myDir = parent == null ? null : whichChildAmI();
                Direction otherDir = other.parent == null ? null : other.whichChildAmI();
                BSTNode tmpLeft = leftChild;
                BSTNode tmpRight = rightChild;

                BSTNode tmpParent = parent;
                if (other.parent == null) { // grandpa
                    parent = null;
                } else {
                    other.parent.setChild(otherDir, this);
                }
                if (tmpParent == null) {
                    other.parent = null;
                } else {
                    tmpParent.setChild(myDir, other);
                }

                setChild(Direction.LEFT, other.leftChild);
                setChild(Direction.RIGHT, other.rightChild);
                other.setChild(Direction.LEFT, tmpLeft);
                other.setChild(Direction.RIGHT, tmpRight);
                calcSize();
                if (parent != null) {
                    parent.bubbleUpSize();
                }
                other.calcSize();
                if (other.parent != null) {
                    other.parent.bubbleUpSize();
                }
            }
            afterSwap(other);
        }

        protected abstract BSTNode concatenateRoots(BSTNode right);

        public static BSTNode barrelRollToLowest(BSTNode target) {
            // 1. chop the tree in half, centered at target
            // 2. reattach them in the opposite order
            // in other words, "cut the deck but don't riffle" - leave "target" as the first node (NOT necessarily the root node)
            BSTNodePair pair = target.disconnect(Direction.RIGHT);
            if ((target instanceof SplayNode && pair.right != target) || pair.right.parent != null) { // splay to root only happens with a splay tree, obviously
                throw new IllegalStateException();
            }
            // target is now the lowest (leftmost) element of pair.right
            BSTNode ret = BSTNode.concatenate(pair.right, pair.left); // target is now first, and everything else is still in order :D
            // use concatenate and not concatenateRoots because pair.left could be null
            if (ret == target && pair.left != null) {
                throw new IllegalStateException();
            }
            return ret;
        }

        protected abstract BSTNodePair disconnect(Direction remainOnSide); // chops the tree in half, with "this" remaining on the side "remainOnSide"

        public abstract void remove();

        protected void bubbleUpSize() {
            int ns = calcSize();
            if (loopbackSize != ns) {
                loopbackSize = ns;
                if (parent != null) {
                    parent.bubbleUpSize();
                }
            }
        }

        protected void childUpdated() {
            loopbackSize = calcSize();
        }

        protected void sizeMustBeAccurate() {
            if (loopbackSize != calcSize()) {
                throw new IllegalStateException();
            }
        }

        protected int calcSize() {
            int size = 0;
            if (isLoopback()) {
                size++;
            }
            if (rightChild != null) {
                size += rightChild.loopbackSize;
            }
            if (leftChild != null) {
                size += leftChild.loopbackSize;
            }
            return size;
        }

        protected BSTNode getChild(Direction dir) {
            return dir == Direction.LEFT ? leftChild : rightChild;
        }

        protected void setChild(Direction dir, BSTNode newChild) {
            if (newChild == this) {
                throw new IllegalStateException();
            }
            if (dir == Direction.LEFT) {
                leftChild = newChild;
            } else {
                rightChild = newChild;
            }
            if (newChild != null) {
                newChild.parent = this;
            }
        }

        protected Direction whichChildAmI() {
            if (parent.leftChild == this) {
                return Direction.LEFT;
            }
            if (parent.rightChild == this) {
                return Direction.RIGHT;
            }
            throw new IllegalStateException();
        }

        protected BSTNode walkDescendant(Direction side) {
            BSTNode child = getChild(side);
            if (child == null) {
                return this;
            } else {
                return child.walkDescendant(side);
            }
        }

        protected BSTNode walkAncestor() {
            BSTNode walk = this;
            while (walk.parent != null) {
                parentWalks++;
                walk = walk.parent;
            }
            parentCalls++;
            return walk;
            /*if (parent == null) {
                return this;
            } else {
                return parent.walkAncestor();
            }*/
        }

        protected void walk(Consumer<BSTNode> consumer) {
            if (leftChild != null) {
                leftChild.walk(consumer);
            }
            consumer.accept(this);
            if (rightChild != null) {
                rightChild.walk(consumer);
            }
        }

        protected BSTNode walkNext() {
            if (rightChild != null) {
                return rightChild.walkDescendant(Direction.LEFT);
            }
            BSTNode itr = this;
            while (itr.parent != null && itr.whichChildAmI() == Direction.RIGHT) {
                itr = itr.parent;
            }
            return itr.parent;
        }

        protected boolean isAlone() {
            return parent == null && leftChild == null && rightChild == null;
        }

        public boolean isLoopback() {
            return src == dst;
        }
    }

    public enum Direction { // TODO check if proguard converts this to an int
        LEFT, RIGHT;

        public Direction opposite() {
            return this == LEFT ? RIGHT : LEFT;
        }
    }

    private static class BSTNodePair {

        final BSTNode left;
        final BSTNode right;

        private BSTNodePair(BSTNode left, BSTNode right) {
            this.left = left;
            this.right = right;
            if ((left != null && left.parent != null) || (right != null && right.parent != null)) {
                throw new IllegalStateException();
            }
        }
    }

    public class TreeEdge {

        private boolean cut;
        final BSTNode left;
        final BSTNode right;

        private TreeEdge(BSTNode left, BSTNode right) {
            this.left = left;
            this.right = right;
        }

        private EulerTourForest owner() {
            return EulerTourForest.this;
        }
    }

    private static void mustEq(BSTNode a, BSTNode b) {
        if (a != b) {
            throw new IllegalStateException(a + " " + b);
        }
    }

    public static void sanityCheck2() {
        for (int i = 0; i < 9; i++) {
            int mode = i % 3;
            int SZ = 700;
            TreeEdge[] up = new TreeEdge[SZ * SZ];
            TreeEdge[] right = new TreeEdge[SZ * SZ];
            EulerTourForest forest = new EulerTourForest(SZ * SZ);
            for (int y = 0; y < SZ; y++) {
                for (int x = 0; x < SZ; x++) {
                    if (y != SZ - 1) {
                        try {
                            up[x * SZ + y] = forest.link(x * SZ + y, x * SZ + (y + 1));
                        } catch (IllegalStateException ex) {} // ignore if already linked
                    }
                    if (x != SZ - 1) {
                        try {
                            right[x * SZ + y] = forest.link(x * SZ + y, (x + 1) * SZ + y);
                        } catch (IllegalStateException ex) {} // ignore if already linked
                    }
                }
            }
            Random rand = new Random(5021);
            for (int x = 0; x < SZ; x++) {
                int y = SZ / 2;
                forest.cut(up[x * SZ + y]);
                //System.out.println("Sz " + forest.size(x * SZ + y));
            }
            if (mode == 1) {
                for (int j = 0; j < SZ * SZ * 2; j++) { // *2 for a fair comparison to during connection, since that one splays both sides of each test
                    ((EulerTourForest.SplayNode) forest.loopbacks[rand.nextInt(SZ * SZ)]).splay();
                }
            }
            long a = System.currentTimeMillis();
            parentCalls = 0; // reset metrics
            parentWalks = 0;
            for (int checks = 0; checks < SZ * SZ; checks++) {
                int v1 = rand.nextInt(SZ * SZ);
                int v2 = rand.nextInt(SZ * SZ);
                forest.connected(v1, v2);
                if (mode == 2) {
                    ((SplayNode) forest.loopbacks[v1]).splay();
                    ((SplayNode) forest.loopbacks[v2]).splay();
                }
            }
            forest.checkForest(false);
            if (mode == 0) {
                System.out.println("WITHOUT random accesses");
            } else if (mode == 1) {
                System.out.println("WITH pre-connection random accesses");
            } else {
                System.out.println("WITH random accesses during connection");
            }
            System.out.println("Walk ancestor was called " + parentCalls + " times, and it traversed " + parentWalks + " in total, implying an average height of " + (parentWalks / (float) parentCalls));
            System.out.println("Time: " + (System.currentTimeMillis() - a));
        }
    }

    public static void sanityCheck() {
        for (Direction dir : Direction.values()) {
            System.out.println("Testing zig " + dir);
            // see "Zig step" of https://en.wikipedia.org/wiki/Splay_tree
            SplayNode p = new SplayNode();
            SplayNode x = new SplayNode();
            SplayNode A = new SplayNode();
            SplayNode B = new SplayNode();
            SplayNode C = new SplayNode();
            p.setChild(dir, x);
            p.setChild(dir.opposite(), C);
            x.setChild(dir, A);
            x.setChild(dir.opposite(), B);

            x.splay();

            mustEq(p.parent, x);
            mustEq(p.getChild(dir), B);
            mustEq(p.getChild(dir.opposite()), C);
            mustEq(x.parent, null);
            mustEq(x.getChild(dir), A);
            mustEq(x.getChild(dir.opposite()), p);
            mustEq(A.parent, x);
            mustEq(A.getChild(dir), null);
            mustEq(A.getChild(dir.opposite()), null);
            mustEq(B.parent, p);
            mustEq(B.getChild(dir), null);
            mustEq(B.getChild(dir.opposite()), null);
            mustEq(C.parent, p);
            mustEq(C.getChild(dir), null);
            mustEq(C.getChild(dir.opposite()), null);
        }
        for (Direction dir : Direction.values()) {
            System.out.println("Testing zig-zig " + dir);
            // see "Zig-zig step" of https://en.wikipedia.org/wiki/Splay_tree
            SplayNode g = new SplayNode();
            SplayNode p = new SplayNode();
            SplayNode x = new SplayNode();
            SplayNode A = new SplayNode();
            SplayNode B = new SplayNode();
            SplayNode C = new SplayNode();
            SplayNode D = new SplayNode();
            g.setChild(dir, p);
            g.setChild(dir.opposite(), D);
            p.setChild(dir, x);
            p.setChild(dir.opposite(), C);
            x.setChild(dir, A);
            x.setChild(dir.opposite(), B);

            x.splay();

            mustEq(g.parent, p);
            mustEq(g.getChild(dir), C);
            mustEq(g.getChild(dir.opposite()), D);
            mustEq(p.parent, x);
            mustEq(p.getChild(dir), B);
            mustEq(p.getChild(dir.opposite()), g);
            mustEq(x.parent, null);
            mustEq(x.getChild(dir), A);
            mustEq(x.getChild(dir.opposite()), p);
            mustEq(A.parent, x);
            mustEq(A.getChild(dir), null);
            mustEq(A.getChild(dir.opposite()), null);
            mustEq(B.parent, p);
            mustEq(B.getChild(dir), null);
            mustEq(B.getChild(dir.opposite()), null);
            mustEq(C.parent, g);
            mustEq(C.getChild(dir), null);
            mustEq(C.getChild(dir.opposite()), null);
            mustEq(D.parent, g);
            mustEq(D.getChild(dir), null);
            mustEq(D.getChild(dir.opposite()), null);
        }
        for (Direction dir : Direction.values()) {
            System.out.println("Testing zig-zag " + dir);
            // see "Zig-zag step" of https://en.wikipedia.org/wiki/Splay_tree
            SplayNode g = new SplayNode();
            SplayNode p = new SplayNode();
            SplayNode x = new SplayNode();
            SplayNode A = new SplayNode();
            SplayNode B = new SplayNode();
            SplayNode C = new SplayNode();
            SplayNode D = new SplayNode();
            g.setChild(dir, p);
            g.setChild(dir.opposite(), D);
            p.setChild(dir, A);
            p.setChild(dir.opposite(), x);
            x.setChild(dir, B);
            x.setChild(dir.opposite(), C);

            x.splay();

            mustEq(g.parent, x);
            mustEq(g.getChild(dir), C);
            mustEq(g.getChild(dir.opposite()), D);
            mustEq(p.parent, x);
            mustEq(p.getChild(dir), A);
            mustEq(p.getChild(dir.opposite()), B);
            mustEq(x.parent, null);
            mustEq(x.getChild(dir), p);
            mustEq(x.getChild(dir.opposite()), g);
            mustEq(A.parent, p);
            mustEq(A.getChild(dir), null);
            mustEq(A.getChild(dir.opposite()), null);
            mustEq(B.parent, p);
            mustEq(B.getChild(dir), null);
            mustEq(B.getChild(dir.opposite()), null);
            mustEq(C.parent, g);
            mustEq(C.getChild(dir), null);
            mustEq(C.getChild(dir.opposite()), null);
            mustEq(D.parent, g);
            mustEq(D.getChild(dir), null);
            mustEq(D.getChild(dir.opposite()), null);
        }
        for (Direction GtoP : Direction.values()) {
            for (Direction PtoX : Direction.values()) {
                System.out.println("Testing connected swap " + GtoP + " " + PtoX);
                SplayNode g = new SplayNode();
                SplayNode p = new SplayNode();
                SplayNode x = new SplayNode();
                SplayNode A = new SplayNode();
                SplayNode B = new SplayNode();
                SplayNode C = new SplayNode();
                SplayNode D = new SplayNode();
                g.setChild(GtoP, p);
                g.setChild(GtoP.opposite(), D);
                p.setChild(PtoX, x);
                p.setChild(PtoX.opposite(), A);
                x.setChild(Direction.LEFT, B);
                x.setChild(Direction.RIGHT, C);

                /*x.black = true;
                p.black = false;*/
                p.swapLocationWith(x);

                /*if (x.black || !p.black) {
                    throw new IllegalStateException();
                }*/
                mustEq(g.parent, null);
                mustEq(g.getChild(GtoP), x);
                mustEq(g.getChild(GtoP.opposite()), D);
                mustEq(p.parent, x);
                mustEq(p.getChild(Direction.LEFT), B);
                mustEq(p.getChild(Direction.RIGHT), C);
                mustEq(x.parent, g);
                mustEq(x.getChild(PtoX), p);
                mustEq(x.getChild(PtoX.opposite()), A);
                mustEq(A.parent, x);
                mustEq(A.getChild(Direction.LEFT), null);
                mustEq(A.getChild(Direction.RIGHT), null);
                mustEq(B.parent, p);
                mustEq(B.getChild(Direction.LEFT), null);
                mustEq(B.getChild(Direction.RIGHT), null);
                mustEq(C.parent, p);
                mustEq(C.getChild(Direction.LEFT), null);
                mustEq(C.getChild(Direction.RIGHT), null);
                mustEq(D.parent, g);
                mustEq(D.getChild(Direction.LEFT), null);
                mustEq(D.getChild(Direction.RIGHT), null);
            }
        }
        for (Direction APtoA : Direction.values()) {
            for (Direction BPtoB : Direction.values()) {
                System.out.println("Testing disconnected swap " + APtoA + " " + BPtoB);
                SplayNode ap = new SplayNode();
                SplayNode apoc = new SplayNode();
                SplayNode a = new SplayNode();
                SplayNode alc = new SplayNode();
                SplayNode arc = new SplayNode();
                SplayNode bp = new SplayNode();
                SplayNode bpoc = new SplayNode();
                SplayNode b = new SplayNode();
                SplayNode blc = new SplayNode();
                SplayNode brc = new SplayNode();
                ap.setChild(APtoA, a);
                ap.setChild(APtoA.opposite(), apoc);
                a.setChild(Direction.LEFT, alc);
                a.setChild(Direction.RIGHT, arc);
                bp.setChild(BPtoB, b);
                bp.setChild(BPtoB.opposite(), bpoc);
                b.setChild(Direction.LEFT, blc);
                b.setChild(Direction.RIGHT, brc);

                /*a.black = true;
                b.black = false;*/
                a.swapLocationWith(b);

                /*if (a.black || !b.black) {
                    throw new IllegalStateException();
                }*/
                mustEq(ap.parent, null);
                mustEq(ap.getChild(APtoA), b);
                mustEq(ap.getChild(APtoA.opposite()), apoc);
                mustEq(apoc.parent, ap);
                mustEq(apoc.getChild(Direction.LEFT), null);
                mustEq(apoc.getChild(Direction.RIGHT), null);
                mustEq(a.parent, bp);
                mustEq(a.getChild(Direction.LEFT), blc);
                mustEq(a.getChild(Direction.RIGHT), brc);
                mustEq(alc.parent, b);
                mustEq(alc.getChild(Direction.LEFT), null);
                mustEq(alc.getChild(Direction.RIGHT), null);
                mustEq(arc.parent, b);
                mustEq(arc.getChild(Direction.LEFT), null);
                mustEq(arc.getChild(Direction.RIGHT), null);
                mustEq(bp.parent, null);
                mustEq(bp.getChild(BPtoB), a);
                mustEq(bp.getChild(BPtoB.opposite()), bpoc);
                mustEq(bpoc.parent, bp);
                mustEq(bpoc.getChild(Direction.LEFT), null);
                mustEq(bpoc.getChild(Direction.RIGHT), null);
                mustEq(b.parent, ap);
                mustEq(b.getChild(Direction.LEFT), alc);
                mustEq(b.getChild(Direction.RIGHT), arc);
                mustEq(blc.parent, a);
                mustEq(blc.getChild(Direction.LEFT), null);
                mustEq(blc.getChild(Direction.RIGHT), null);
                mustEq(brc.parent, a);
                mustEq(brc.getChild(Direction.LEFT), null);
                mustEq(brc.getChild(Direction.RIGHT), null);
            }
        }
        {
            Random rand = new Random(5021);
            List<Supplier<BSTNode>> constructors = Arrays.asList(SplayNode::new/*, SplayNode::new*/);
            for (int run = 0; run < 10; run++) {
                int NODES = 10000;
                Supplier<BSTNode> toUse = constructors.get(run % constructors.size());
                List<BSTNode> nodes = new ArrayList<>();
                {
                    BSTNode root = toUse.get();
                    nodes.add(root);
                    for (int i = 1; i < NODES; i++) {
                        nodes.add(toUse.get());
                        root = BSTNode.concatenate(root, nodes.get(i));
                    }
                }
                int shuffledBy = 0;
                for (int ii = 0; ii < 10000; ii++) {
                    if (rand.nextBoolean()) {
                        BSTNode root = nodes.get(rand.nextInt(NODES));
                        if (root instanceof SplayNode) {
                            ((SplayNode) root).splay();
                            if (root != nodes.get(rand.nextInt(NODES)).walkAncestor() || root.loopbackSize != NODES) {
                                throw new IllegalStateException();
                            }
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                    if (rand.nextBoolean()) {
                        shuffledBy = rand.nextInt(NODES);
                        BSTNode root = BSTNode.barrelRollToLowest(nodes.get(shuffledBy));
                        if (root != nodes.get(rand.nextInt(NODES)).walkAncestor() || root.loopbackSize != NODES) {
                            throw new IllegalStateException();
                        }
                    }
                    if (rand.nextBoolean()) {
                        int pos = rand.nextBoolean() ? (shuffledBy + NODES + rand.nextInt(10) - 5) % NODES : rand.nextInt(NODES);
                        BSTNode remove = nodes.remove(pos);
                        NODES--;
                        remove.remove();
                        if (shuffledBy > pos) {
                            shuffledBy--;
                        }
                    }
                    List<BSTNode> order = new ArrayList<>(NODES);
                    nodes.get(rand.nextInt(NODES)).walkAncestor().walk(order::add);
                    for (int n = 0; n < NODES; n++) {
                        if (order.get(n) != nodes.get((n + shuffledBy) % NODES)) {
                            throw new IllegalStateException();
                        }
                        order.get(n).sizeMustBeAccurate();
                        if (order.get(n).walkNext() != (n < NODES - 1 ? order.get(n + 1) : null)) {
                            throw new IllegalStateException();
                        }
                    }
                }
            }
        }
        {
            // slide 22 of https://web.stanford.edu/class/archive/cs/cs166/cs166.1166/lectures/17/Small17.pdf
            EulerTourForest forest = new EulerTourForest(11);
            forest.link(0, 1);
            forest.link(1, 3);
            forest.link(2, 4);
            forest.link(1, 2);
            TreeEdge toCut = forest.link(0, 5);
            forest.link(5, 6);
            forest.link(6, 9);
            forest.link(9, 10);
            forest.link(9, 8);
            forest.link(6, 7);
            BSTNode.barrelRollToLowest(forest.loopbacks[0]);
            if (!forest.checkForest(true).equals("abdbcecbafgjkjijghgf")) {
                throw new IllegalStateException();
            }
            forest.cut(toCut);
            if (!forest.checkForest(true).equals("abdbcecb fgjkjijghg")) {
                throw new IllegalStateException();
            }
        }
        {
            // slide 26 of https://web.stanford.edu/class/archive/cs/cs166/cs166.1166/lectures/17/Small17.pdf
            EulerTourForest forest = new EulerTourForest(11);
            forest.link(2, 4);
            TreeEdge toCut = forest.link(2, 1);
            forest.link(1, 0);
            forest.link(1, 3);
            forest.link(2, 6);
            forest.link(6, 9);
            forest.link(9, 10);
            forest.link(9, 8);
            forest.link(6, 7);
            forest.link(5, 6);
            BSTNode.barrelRollToLowest(forest.loopbacks[2]);
            if (!forest.checkForest(true).equals("cecbabdbcgjkjijghgfg")) {
                throw new IllegalStateException();
            }
            forest.cut(toCut);
            if (!forest.checkForest(true).equals("babd cgjkjijghgfgce")) {
                throw new IllegalStateException();
            }
        }
    }

    public String checkForest(boolean verbose) {
        boolean[] seen = new boolean[loopbacks.length];
        StringBuilder ret = new StringBuilder();
        for (int vert = 0; vert < loopbacks.length; vert++) {
            if (seen[vert]) {
                continue;
            }
            List<BSTNode> order = new ArrayList<>();
            loopbacks[vert].walkAncestor().walk(order::add);
            for (int i = 0; i < order.size(); i++) {
                if (verbose) {
                    System.out.print("(" + (char) ('a' + order.get(i).src) + "," + (char) ('a' + order.get(i).dst) + ") ");
                }
                if (order.get(i).dst != order.get((i + 1) % order.size()).src) {
                    throw new IllegalStateException();
                }
                if (order.get(i).isLoopback()) {
                    seen[order.get(i).src] = true;
                } else {
                    ret.append((char) ('a' + order.get(i).src));
                }
            }
            if (verbose) {
                System.out.println();
            }
            ret.append(" ");
            if (!seen[vert]) {
                throw new IllegalStateException();
            }
        }
        if (verbose) {
            System.out.println(ret);
        }
        return ret.toString().trim();
    }
}
