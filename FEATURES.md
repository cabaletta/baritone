# Pathing features
- **Long distance pathing and splicing** Baritone calculates paths in segments, and precalculates the next segment when the current one is about to end, so that it's moving towards the goal at all times.
- **Chunk caching** Baritone simplifies chunks to a compacted internal 2-bit representation (AIR, SOLID, WATER, AVOID) and stores them in RAM for better very-long-distance pathing. There is also an option to save these cached chunks to disk. <a href="https://www.youtube.com/watch?v=dyfYKSubhdc">Example</a>
- **Block breaking** Baritone considers breaking blocks as part of its path. It also takes into account your current tool set and hot bar. For example, if you have a Eff V diamond pick, it may choose to mine through a stone barrier, while if you only had a wood pick it might be faster to climb over it.
- **Block placing** Baritone considers placing blocks as part of its path. This includes sneak-back-placing, pillaring, etc. It has a configurable penalty of placing a block (set to 1 second by default), to conserve its resources. The list of acceptable throwaway blocks is also configurable, and is cobble, dirt, or netherrack by default.
- **Falling** Baritone will fall up to 3 blocks onto solid ground (configurable, if you have Feather Falling and/or don't mind taking a little damage). If you have a water bucket on your hotbar, it will fall up to 23 blocks and place the bucket beneath it. It will fall an unlimited distance into existing still water.
- **Vines and ladders** Baritone understands how to climb and descend vines and ladders. There is experimental support for more advanced maneuvers, like strafing to a different ladder / vine column in midair (off by default, setting named `allowVines`).
- **Fence gates and doors**
- **Falling blocks** Baritone understands the costs of breaking blocks with falling blocks on top, and includes all of their break costs. Additionally, since it avoids breaking any blocks touching a liquid, it won't break the bottom of a gravel stack below a lava lake (anymore).
- **Avoiding dangerous blocks** Obviously, it knows not to walk through fire or on magma, not to corner over lava (that deals some damage), not to break any blocks touching a liquid (it might drown), etc.

# Pathing method
Baritone uses a modified version of A*. 

- **Incremental cost backoff** Since most of the time Baritone only knows the terrain up to the render distance, it can't calculate a full path to the goal. Therefore it needs to select a segment to execute first (assuming it will calculate the next segment at the end of this one). It uses incremental cost backoff to select the best node by varying metrics, then paths to that node. This is unchanged from MineBot and I made a <a href="https://docs.google.com/document/d/1WVHHXKXFdCR1Oz__KtK8sFqyvSwJN_H4lftkHFgmzlc/edit">write-up</a> that still applies. In essence, it keeps track of the best node by various increasing coefficients, then picks the node with the least coefficient that goes at least 5 blocks from the starting position.
- **Minimum improvement repropagation** The pathfinder ignores alternate routes that provide minimal improvements (less than 0.01 ticks of improvement), because the calculation cost of repropagating this to all connected nodes is much higher than the half-millisecond path time improvement it would get.
- **Backtrack cost favoring** While calculating the next segment, Baritone favors backtracking its current segment slightly, as a tiebreaker. This allows it to splice and jump onto the next segment as early as possible, if the next segment begins with a backtrack of the current one. <a href="https://www.youtube.com/watch?v=CGiMcb8-99Y">Example</a>

# Configuring Baritone
All the settings and documentation are <a href="https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/Settings.java">here</a>.
To change a boolean setting, just say its name in chat (for example saying `allowBreak` toggles whether Baritone will consider breaking blocks). For a numeric setting, say its name then the new value (like `pathTimeoutMS 250`). It's case insensitive.

# Goals
The pathing goal can be set to any of these options:
- **GoalBlock** one specific block that the player should stand inside at foot level
- **GoalXZ** an X and a Z coordinate, used for long distance pathing
- **GoalYLevel** a Y coordinate
- **GoalTwoBlocks** a block position that the player should stand in, either at foot or eye level
- **GoalGetToBlock** a block position that the player should stand adjacent to, below, or on top of
- **GoalNear** a block position that the player should get within a certain radius of, used for following entities

And finally `GoalComposite`. `GoalComposite` is a list of other goals, any one of which satisfies the goal. For example, `mine diamond_ore` creates a `GoalComposite` of `GoalTwoBlocks`s for every diamond ore location it knows of.


# Future features
(things it doesn't have yet, and things it may not ever have)
- Trapdoors
- Boats
- Horses / pigs
- Sprint jumping in a 1x2 corridor
- Parkour (jumping over gaps of any length)