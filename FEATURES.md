# Pathing features
- Long distance pathing and splicing. Baritone calculates paths in segments, and precalculates the next segment when the current one is about to end.
- Chunk caching. Baritone simplifies chunks to an internal 2-bit representation (AIR, SOLID, WATER, AVOID) and stores them in RAM for better very-long-distance pathing. There is also an option to save these cached chunks to disk.
- Block breaking. Baritone considers breaking blocks as part of its path. It also takes into account your current tool set and hot bar. For example, if you have a Eff V diamond pick, it may choose to mine through a stone barrier, while if you only had a wood pick it might be faster to climb over it.
- Block placing. Baritone considers placing blocks as part of its path. This includes sneak-back-placing, pillaring, etc. It has a configurable penalty of placing a block (set to 1 second by default), to conserve its resources.
- Falling. Baritone will fall up to 3 blocks onto solid ground (configurable, if you have Feather Falling and/or don't mind taking a little damage). If you have a water bucket on your hotbar, it will fall up to 23 blocks and place the bucket beneath it. It will fall an unlimited distance into existing still water.
- Vines and ladders. Baritone understands how to climb and descend vines and ladders.
- Fence gates and doors.
- Falling blocks. Baritone understands the costs of breaking blocks with falling blocks on top, and includes all of their break costs. Additionally, since it avoids breaking any blocks touching a liquid, it won't break the bottom of a gravel stack below a lava lake (anymore).
- 

# Goals
The pathing goal can be set to any of these options 
- GoalBlock - one specific block that the player should stand inside at foot level
- GoalXZ - an X and a Z coordinate, used for long distance pathing
- GoalYLevel - a Y coordinate
- GoalTwoBlocks - a block position that the player should stand in, either at foot or eye level
- GoalGetToBlock - a block position that the player should stand adjacent to, below, or on top of
- GoalNear - a block position that the player should get within a certain radius of, used for following entities

And finally GoalComposite. GoalComposite is a list of other goals, any one of which satisfies the goal. For example, `mine diamond_ore` creates a GoalComposite of GoalTwoBlock s for every diamond ore location it knows of.


# Future features
(things it doesn't have yet)