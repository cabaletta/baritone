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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayDeque;

/**
 * Tarjans algorithm destructured into a coroutine-like layout with an explicit "call stack" on the heap
 * <p>
 * This is needed so as to not stack overflow on huge schematics sadly. My test schematic (128x128x128) creates a
 * call stack of depth 2 million, which the JVM cannot handle on its own on the stack, but it has no trouble with an
 * ArrayDeque of 2 million entries on the heap.
 */
public class TarjansAlgorithm {

    private final DependencyGraphScaffoldingOverlay graph;
    private final Long2ObjectOpenHashMap<TarjanVertexInfo> infoMap;
    private ArrayDeque<TarjanVertexInfo> vposStack;
    private ArrayDeque<TarjanVertexInfo> tarjanCallStack;
    private int index;
    private TarjansResult result;

    private TarjansAlgorithm(DependencyGraphScaffoldingOverlay overlayedGraph) {
        this.graph = overlayedGraph;
        this.infoMap = new Long2ObjectOpenHashMap<>();
        this.result = new TarjansResult();
        this.vposStack = new ArrayDeque<>();
        this.tarjanCallStack = new ArrayDeque<>();
    }

    public static TarjansResult run(DependencyGraphScaffoldingOverlay overlayedGraph) {
        TarjansAlgorithm algo = new TarjansAlgorithm(overlayedGraph);
        algo.run();
        return algo.result;
    }

    private void run() {
        if (Main.DEBUG) {
            //System.out.println("Tarjan start");
        }
        long a = System.currentTimeMillis();
        graph.forEachReal(pos -> {
            strongConnect(pos);
            while (!tarjanCallStack.isEmpty()) {
                strongConnectPart2(tarjanCallStack.pop());
            }
        });
        if (Main.DEBUG) {
            //System.out.println("Tarjan end " + (System.currentTimeMillis() - a) + "ms");
        }
    }

    private void strongConnect(long vpos) {
        if (Main.DEBUG && !graph.real(vpos)) {
            throw new IllegalStateException();
        }
        TarjanVertexInfo info = infoMap.get(vpos);
        if (info == null) {
            info = createInfo(vpos);
        } else {
            if (info.doneWithMainLoop) {
                return;
            }
        }
        strongConnectPart2(info);
    }

    private TarjanVertexInfo createInfo(long vpos) {
        TarjanVertexInfo info = new TarjanVertexInfo();
        info.pos = vpos;
        info.index = index++;
        info.lowlink = info.index;
        vposStack.push(info);
        info.onStack = true;
        infoMap.put(vpos, info);
        return info;
    }

    private void strongConnectPart2(TarjanVertexInfo info) {
        if (info.doneWithMainLoop) {
            throw new IllegalStateException();
        }
        long vpos = info.pos;
        for (int fi = info.facesCompleted; fi < Face.NUM_FACES; fi++) {
            Face face = Face.VALUES[fi];
            if (graph.outgoingEdge(vpos, face)) {
                long wpos = face.offset(vpos);
                TarjanVertexInfo winfo = infoMap.get(wpos);

                if (winfo == null) {
                    if (info.recursingInto != -1) {
                        throw new IllegalStateException();
                    }
                    info.recursingInto = wpos;
                    winfo = createInfo(wpos);
                    tarjanCallStack.push(info);
                    tarjanCallStack.push(winfo);
                    return;
                }
                if (info.recursingInto == wpos) {
                    info.lowlink = Math.min(info.lowlink, winfo.lowlink);
                    info.recursingInto = -1;
                } else if (winfo.onStack) {
                    info.lowlink = Math.min(info.lowlink, winfo.index);
                }
            }
            info.facesCompleted = fi + 1;
        }
        if (info.recursingInto != -1) {
            throw new IllegalStateException();
        }
        info.doneWithMainLoop = true;
        if (info.lowlink == info.index) {
            result.startComponent();
            TarjanVertexInfo winfo;
            do {
                winfo = vposStack.pop();
                winfo.onStack = false;
                result.addNode(winfo.pos);
            } while (winfo.pos != vpos);
        }
    }

    public static class TarjansResult {

        private final Long2IntOpenHashMap posToComponent = new Long2IntOpenHashMap();
        private int componentID = -1;

        private TarjansResult() {
            posToComponent.defaultReturnValue(-1);
        }

        private void startComponent() {
            componentID++;
        }

        private void addNode(long nodepos) {
            if (posToComponent.put(nodepos, componentID) != -1) {
                throw new IllegalStateException();
            }
        }

        public int getComponent(long nodepos) {
            int result = posToComponent.get(nodepos);
            if (result == -1) {
                throw new IllegalStateException();
            }
            return result;
        }

        public int numComponents() {
            return componentID + 1;
        }
    }

    private static class TarjanVertexInfo {

        private long pos;
        private int index;
        private int lowlink;
        private boolean onStack;
        private boolean doneWithMainLoop;
        private long recursingInto = -1;
        private int facesCompleted;
    }
}
