# Pathing features
- **Long distance pathing and splicing** Baritone calculates paths in segments, and precalculates the next segment when the current one is about to end, so that it's moving towards the goal at all times.
- **Chunk caching** Baritone simplifies chunks to a compacted internal 2-bit representation (AIR, SOLID, WATER, AVOID) and stores them in RAM for better very-long-distance pathing. There is also an option to save these cached chunks to disk. <a href="https://www.youtube.com/watch?v=dyfYKSubhdc">Example</a>
- **Block breaking** Baritone considers breaking blocks as part of its path. It also takes into account your current tool set and hot bar. For example, if you have a Eff V diamond pick, it may choose to mine through a stone barrier, while if you only had a wood pick it might be faster to climb over it.
- **Block placing** Baritone considers placing blocks as part of its path. This includes sneak-back-placing, pillaring, etc. It has a configurable penalty of placing a block (set to 1 second by default), to conserve its resources. The list of acceptable throwaway blocks is also configurable, and is cobble, dirt, or netherrack by default. <a href="https://www.youtube.com/watch?v=F6FbI1L9UmU">Example</a>
- **Falling** Baritone will fall up to 3 blocks onto solid ground (configurable, if you have Feather Falling and/or don't mind taking a little damage). If you have a water bucket on your hotbar, it will fall up to 23 blocks and place the bucket beneath it. It will fall an unlimited distance into existing still water.
- **Vines and ladders** Baritone understands how to climb and descend vines and ladders. There is experimental support for more advanced maneuvers, like strafing to a different ladder / vine column in midair (off by default, setting named `allowVines`). Baritone can break its fall by grabbing ladders / vines midair, and understands when that is and isn't possible.
- **Opening fence gates and doors**
- **Slabs and stairs**
- **Falling blocks** Baritone understands the costs of breaking blocks with falling blocks on top, and includes all of their break costs. Additionally, since it avoids breaking any blocks touching a liquid, it won't break the bottom of a gravel stack below a lava lake (anymore).
- **Avoiding dangerous blocks** Obviously, it knows not to walk through fire or on magma, not to corner over lava (that deals some damage), not to break any blocks touching a liquid (it might drown), etc.
- **Parkour** Sprint jumping over 1, 2, or 3 block gaps
- **Parkour place** Sprint jumping over a 3 block gap and placing the block to land on while executing the jump. It's really cool.
- **Pigs** It can sort of control pigs. I wouldn't rely on it though.

# Pathing method
Baritone uses A*, with some modifications: 

- **Segmented calculation** Traditional A* calculates until the most promising node is in the goal, however in the environment of Minecraft with a limited render distance, we don't know the environment all the way to our goal. Baritone has three possible ways for path calculation to end: finding a path all the way to the goal, running out of time, or getting to the render distance. In the latter two scenarios, the selection of which segment to actually execute falls to the next item (incremental cost backoff). Whenever the path calculation thread finds that the best / most promising node is at the edge of loaded chunks, it increments a counter. If this happens more than 50 times (configurable), path calculation exits early. This happens with very low render distances. Otherwise, calculation continues until the timeout is hit (also configurable) or we find a path all the way to the goal.
- **Incremental cost backoff** When path calculation exits early without getting all the way to the goal, Baritone it needs to select a segment to execute first (assuming it will calculate the next segment at the end of this one). It uses incremental cost backoff to select the best node by varying metrics, then paths to that node. This is unchanged from MineBot and I made a <a href="https://docs.google.com/document/d/1WVHHXKXFdCR1Oz__KtK8sFqyvSwJN_H4lftkHFgmzlc/edit">write-up</a> that still applies. In essence, it keeps track of the best node by various increasing coefficients, then picks the node with the least coefficient that goes at least 5 blocks from the starting position.
- **Minimum improvement repropagation** The pathfinder ignores alternate routes that provide minimal improvements (less than 0.01 ticks of improvement), because the calculation cost of repropagating this to all connected nodes is much higher than the half-millisecond path time improvement it would get.
- **Backtrack cost favoring** While calculating the next segment, Baritone favors backtracking its current segment. The cost is decreased heavily, but is still positive (this won't cause it to backtrack if it doesn't need to). This allows it to splice and jump onto the next segment as early as possible, if the next segment begins with a backtrack of the current one. <a href="https://www.youtube.com/watch?v=CGiMcb8-99Y">Example</a>
- **Backtrack detection and pausing** While path calculation happens on a separate thread, the main game thread has access to the latest node considered, and the best path so far (those are rendered light blue and dark blue respectively). When the current best path (rendered dark blue) passes through the player's current position on the current path segment, path execution is paused (if it's safe to do so), because there's no point continuing forward if we're about to turn around and go back that same way. Note that the current best path as reported by the path calculation thread takes into account the incremental cost backoff system, so it's accurate to what the path calculation thread will actually pick once it finishes.

# Chat control

- [Baritone chat control usage](USAGE.md)

# Goals
The pathing goal can be set to any of these options:
- **GoalBlock** one specific block that the player should stand inside at foot level
- **GoalXZ** an X and a Z coordinate, used for long distance pathing
- **GoalYLevel** a Y coordinate
- **GoalTwoBlocks** a block position that the player should stand in, either at foot or eye level
- **GoalGetToBlock** a block position that the player should stand adjacent to, below, or on top of
- **GoalNear** a block position that the player should get within a certain radius of, used for following entities
- **GoalAxis** a block position on an axis or diagonal axis (so x=0, z=0, or x=z), and y=120 (configurable)

And finally `GoalComposite`. `GoalComposite` is a list of other goals, any one of which satisfies the goal. For example, `mine diamond_ore` creates a `GoalComposite` of `GoalTwoBlocks`s for every diamond ore location it knows of.


# Future features
Things it doesn't have yet
- Trapdoors
- Sprint jumping in a 1x2 corridor

See <a href="https://github.com/cabaletta/baritone/issues">issues</a> for more.

Things it may not ever have, from most likely to least likely =(
- Boats
- Horses (2x3 path instead of 1x2)
