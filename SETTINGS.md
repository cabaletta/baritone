### allowBreak
Allow Baritone to break blocks

*Default:* `true` (Boolean)

### allowSprint
Allow Baritone to sprint

*Default:* `true` (Boolean)

### allowPlace
Allow Baritone to place blocks

*Default:* `true` (Boolean)

### blockPlacementPenalty
It doesn't actually take twenty ticks to place a block, this cost is so high
because we want to generally conserve blocks which might be limited

*Default:* `20D` (Double)

### blockBreakAdditionalPenalty
This is just a tiebreaker to make it less likely to break blocks if it can avoid it.
For example, fire has a break cost of 0, this makes it nonzero, so all else being equal
it will take an otherwise equivalent route that doesn't require it to put out fire.

*Default:* `2D` (Double)

### allowWaterBucketFall
Allow Baritone to fall arbitrary distances and place a water bucket beneath it.
Reliability: questionable.

*Default:* `true` (Boolean)

### assumeWalkOnWater
Allow Baritone to assume it can walk on still water just like any other block.
This functionality is assumed to be provided by a separate library that might have imported Baritone.

*Default:* `false` (Boolean)

### assumeStep
Assume step functionality; don't jump on an Ascend.

*Default:* `false` (Boolean)

### assumeSafeWalk
Assume safe walk functionality; don't sneak on a backplace traverse.
<p>
Warning: if you do something janky like sneak-backplace from an ender chest, if this is true
it won't sneak right click, it'll just right click, which means it'll open the chest instead of placing
against it. That's why this defaults to off.

*Default:* `false` (Boolean)

### acceptableThrowawayItems
Blocks that Baritone is allowed to place (as throwaway, for sneak bridging, pillaring, etc.)

*Default:* `new ArrayList<>(Arrays.asList(Item.getItemFromBlock(Blocks.DIRT),Item.getItemFromBlock(Blocks.COBBLESTONE),Item.getItemFromBlock(Blocks.NETHERRACK)))` (Array)

### allowVines
Enables some more advanced vine features. They're honestly just gimmicks and won't ever be needed in real
pathing scenarios. And they can cause Baritone to get trapped indefinitely in a strange scenario.

*Default:* `false` (Boolean)

### allowWalkOnBottomSlab
Slab behavior is complicated, disable this for higher path reliability. Leave enabled if you have bottom slabs
everywhere in your base.

*Default:* `true` (Boolean)

### allowParkour
You know what it is
<p>
But it's very unreliable and falls off when cornering like all the time so.

*Default:* `false` (Boolean)

### allowParkourPlace
Like parkour, but even more unreliable!

*Default:* `false` (Boolean)

### considerPotionEffects
For example, if you have Mining Fatigue or Haste, adjust the costs of breaking blocks accordingly.

*Default:* `true` (Boolean)

### costHeuristic
This is the big A* setting.
As long as your cost heuristic is an *underestimate*, it's guaranteed to find you the best path.
3.5 is always an underestimate, even if you are sprinting.
If you're walking only (with allowSprint off) 4.6 is safe.
Any value below 3.5 is never worth it. It's just more computation to find the same path, guaranteed.
(specifically, it needs to be strictly slightly less than ActionCosts.WALK_ONE_BLOCK_COST, which is about 3.56)

Setting it at 3.57 or above with sprinting, or to 4.64 or above without sprinting, will result in
faster computation, at the cost of a suboptimal path. Any value above the walk / sprint cost will result
in it going straight at its goal, and not investigating alternatives, because the combined cost / heuristic
metric gets better and better with each block, instead of slightly worse.

Finding the optimal path is worth it, so it's the default.

This value is an expression instead of a literal so that it's exactly equal to SPRINT_ONE_BLOCK_COST defined in ActionCosts.java

*Default:* `20 / 5.612` (Double)

## a bunch of obscure internal A* settings that you probably don't want to change
### pathingMaxChunkBorderFetch
The maximum number of times it will fetch outside loaded or cached chunks before assuming that
pathing has reached the end of the known area, and should therefore stop.

*Default:* `50` (Integer)

### backtrackCostFavoringCoefficient
Set to 1.0 to effectively disable this feature. See [Issue #18](https://github.com/cabaletta/baritone/issues/18)

*Default:* `0.5` (Double)

### avoidance
Toggle the following 4 settings

They have a noticable performance impact, so they default off

*Default:* `false` (Boolean)

### mobSpawnerAvoidance (~Coefficient and ~Radius)
Set to 1.0 to effectively disable this feature

Set below 1.0 to go out of your way to walk near mob spawners

*Default **mobSpawnerAvoidanceCoefficient**:* `2.0` (Double)

*Default **mobSpawnerAvoidanceRadius**:* `16` (Integer)

### mobAvoidance (~Coefficient and ~Radius)
Set to 1.0 to effectively disable this feature

Set below 1.0 to go out of your way to walk near mobs

*Default **mobAvoidanceCoefficient**:* `1.5` (Double)

*Default **mobAvoidanceRadius**:* `8` (Integer)

### minimumImprovementRepropagation
Don't repropagate cost improvements below 0.01 ticks. They're all just floating point inaccuracies,
and there's no point.

*Default:* `true` (Boolean)

### cutoffAtLoadBoundary
After calculating a path (potentially through cached chunks), artificially cut it off to just the part that is
entirely within currently loaded chunks. Improves path safety because cached chunks are heavily simplified.

see [Issue #144](https://github.com/cabaletta/baritone/issues/144)

*Default:* `false` (Boolean)

### maxCostIncrease
If a movement's cost increases by more than this amount between calculation and execution (due to changes
in the environment / world), cancel and recalculate

*Default:* `10D` (Double)

### costVerificationLookahead
Stop 5 movements before anything that made the path COST_INF.
For example, if lava has spread across the path, don't walk right up to it then recalculate, it might
still be spreading lol

*Default:* `5` (Integer)

### pathCutoffFactor
Static cutoff factor. 0.9 means cut off the last 10% of all paths, regardless of chunk load state

*Default:* `0.9` (Double)

### pathCutoffMinimumLength
Only apply static cutoff for paths of at least this length (in terms of number of movements)

*Default:* `30` (Integer)

### planningTickLookAhead
Start planning the next path once the remaining movements tick estimates sum up to less than this value

*Default:* `150` (Integer)

### pathingMapDefaultSize
Default size of the Long2ObjectOpenHashMap used in pathing

*Default:* `1024` (Integer)

### pathingMapLoadFactor
Load factor coefficient for the Long2ObjectOpenHashMap used in pathing

Decrease for faster map operations, but higher memory usage

*Default:* `0.75f` (Float)

### maxFallHeightNoWater
How far are you allowed to fall onto solid ground (without a water bucket)?
3 won't deal any damage. But if you just want to get down the mountain quickly and you have
Feather Falling IV, you might set it a bit higher, like 4 or 5.

*Default:* `3` (Integer)

### maxFallHeightBucket
How far are you allowed to fall onto solid ground (with a water bucket)?
It's not that reliable, so I've set it below what would kill an unarmored player (23)

*Default:* `20` (Integer)

### allowOvershootDiagonalDescend
Is it okay to sprint through a descend followed by a diagonal?
The player overshoots the landing, but not enough to fall off. And the diagonal ensures that there isn't
lava or anything that's !canWalkInto in that space, so it's technically safe, just a little sketchy.

*Default:* `true` (Boolean)

### simplifyUnloadedYCoord
If your goal is a GoalBlock in an unloaded chunk, assume it's far enough away that the Y coord
doesn't matter yet, and replace it with a GoalXZ to the same place before calculating a path.
Once a segment ends within chunk load range of the GoalBlock, it will go back to normal behavior
of considering the Y coord. The reasoning is that if your X and Z are 10,000 blocks away,
your Y coordinate's accuracy doesn't matter at all until you get much much closer.

*Default:* `true` (Boolean)

### movementTimeoutTicks
If a movement takes this many ticks more than its initial cost estimate, cancel it

*Default:* `100` (Integer)

### primaryTimeoutMS
Pathing ends after this amount of time, but only if a path has been found

If no valid path (length above the minimum) has been found, pathing continues up until the failure timeout

*Default:* `500L` (Long)

### failureTimeoutMS
Pathing can never take longer than this, even if that means failing to find any path at all

*Default:* `2000L` (Long)

### planAheadPrimaryTimeoutMS
Planning ahead while executing a segment ends after this amount of time, but only if a path has been found

If no valid path (length above the minimum) has been found, pathing continues up until the failure timeout

*Default:* `4000L` (Long)

### planAheadFailureTimeoutMS
Planning ahead while executing a segment can never take longer than this, even if that means failing to find any path at all

*Default:* `5000L` (Long)

### slowPath
For debugging, consider nodes much much slower

*Default:* `false` (Boolean)

### slowPathTimeDelayMS
Milliseconds between each node

*Default:* `100L` (Long)

### slowPathTimeoutMS
The alternative timeout number when slowPath is on

*Default:* `40000L` (Long)

### chunkCaching
The big one. Download all chunks in simplified 2-bit format and save them for better very-long-distance pathing.

*Default:* `true` (Boolean)

### pruneRegionsFromRAM
On save, delete from RAM any cached regions that are more than 1024 blocks away from the player
<p>
Temporarily disabled, see issue #248

*Default:* `false` (Boolean)

### chatDebug
Print all the debug messages to chat

*Default:* `true` (Boolean)

### chatControl
Allow chat based control of Baritone. Most likely should be disabled when Baritone is imported for use in
something else

*Default:* `true` (Boolean)

### removePrefix
A second override over chatControl to force it on

*Default:* `false` (Boolean)

### renderPath
Render the path

*Default:* `true` (Boolean)

### renderGoal
Render the goal

*Default:* `true` (Boolean)

### renderGoalIgnoreDepth
Ignore depth when rendering the goal

*Default:* `true` (Boolean)

### renderGoalXZBeacon
Renders X/Z type Goals with the vanilla beacon beam effect. Combining this with
[renderGoalIgnoreDepth](#renderGoalIgnoreDepth) will cause strange render clipping.

*Default:* `false` (Boolean)

### renderSelectionBoxesIgnoreDepth
Ignore depth when rendering the selection boxes (to break, to place, to walk into)

*Default:* `true` (Boolean)

### renderPathIgnoreDepth
Ignore depth when rendering the path

*Default:* `true` (Boolean)

### pathRenderLineWidthPixels
Line width of the path when rendered, in pixels

*Default:* `5F` (Float)

### goalRenderLineWidthPixels
Line width of the goal when rendered, in pixels

*Default:* `3F` (Float)

### fadePath
Start fading out the path at 20 movements ahead, and stop rendering it entirely 30 movements ahead.
Improves FPS.

*Default:* `false` (Boolean)

### freeLook
Move without having to force the client-sided rotations

*Default:* `true` (Boolean)

### antiCheatCompatibility
Will cause some minor behavioral differences to ensure that Baritone works on anticheats.
<p>
At the moment this will silently set the player's rotations when using freeLook so you're not sprinting in
directions other than forward, which is picken up by more "advanced" anticheats like AAC, but not NCP.

*Default:* `true` (Boolean)

### pathThroughCachedOnly
Exclusively use cached chunks for pathing

*Default:* `false` (Boolean)

### prefix
Whether or not to use the "#" command prefix

*Default:* `false` (Boolean)

### leftClickWorkaround
`true`: can mine blocks when in inventory, chat, or tabbed away in ESC menu

`false`: works on cosmic prisons LOL

*Default:* `true` (Boolean)

### walkWhileBreaking
Don't stop walking forward when you need to break blocks in your way

*Default:* `true` (Boolean)

### maxPathHistoryLength
If we are more than 500 movements into the current path, discard the oldest segments, as they are no longer useful

*Default:* `300` (Integer)

### pathHistoryCutoffAmount
If the current path is too long, cut off this many movements from the beginning.

*Default:* `50` (Integer)

### mineGoalUpdateInterval
Rescan for the goal once every 5 ticks.
Set to 0 to disable.

*Default:* `5` (Integer)

### mineScanDroppedItems
While mining, should it also consider dropped items of the correct type as a pathing destination (as well as ore blocks)?

*Default:* `true` (Boolean)

### cancelOnGoalInvalidation
Cancel the current path if the goal has changed, and the path originally ended in the goal but doesn't anymore.

Currently only runs when either MineBehavior or FollowBehavior is active.

For example, if Baritone is doing "mine iron_ore", the instant it breaks the ore (and it becomes air), that location
is no longer a goal. This means that if this setting is true, it will stop there. If this setting were off, it would
continue with its path, and walk into that location. The tradeoff is if this setting is true, it mines ores much faster
since it doesn't waste any time getting into locations that no longer contain ores, but on the other hand, it misses
some drops, and continues on without ever picking them up.

Also on cosmic prisons this should be set to true since you don't actually mine the ore it just gets replaced with stone.

*Default:* `true` (Boolean)

### axisHeight
The "axis" command (aka GoalAxis) will go to a axis, or diagonal axis, at this Y level.

*Default:* `120` (Integer)

### legitMine
Allow MineBehavior to use X-Ray to see where the ores are. Turn this option off to force it to mine "legit"
where it will only mine an ore once it can actually see it, so it won't do or know anything that a normal player
couldn't. If you don't want it to look like you're X-Raying, turn this off

*Default:* `false` (Boolean)

### legitMineYLevel
What Y level to go to for legit strip mining

*Default:* `11` (Integer)

### forceInternalMining
When mining block of a certain type, try to mine two at once instead of one.
If the block above is also a goal block, set GoalBlock instead of GoalTwoBlocks
If the block below is also a goal block, set GoalBlock to the position one down instead of GoalTwoBlocks

*Default:* `true` (Boolean)

### internalMiningAirException
Modification to the previous setting, only has effect if forceInternalMining is true
If true, only apply the previous setting if the block adjacent to the goal isn't air.

*Default:* `true` (Boolean)

### followOffsetDistance
The actual GoalNear is set this distance away from the entity you're following
<p>
For example, set followOffsetDistance to 5 and followRadius to 0 to always stay precisely 5 blocks north of your follow target.

*Default:* `0D` (Double)

### followOffsetDirection
The actual GoalNear is set in this direction from the entity you're following

*Default:* `0F` (Float)

### followRadius
The radius (for the GoalNear) of how close to your target position you actually have to be

*Default:* `3` (Integer)

### cachedChunksExpirySeconds
Cached chunks (regardless of if they're in RAM or saved to disk) expire and are deleted after this number of seconds
-1 to disable

I would highly suggest leaving this setting disabled (-1).

The only valid reason I can think of enable this setting is if you are extremely low on disk space and you play on multiplayer,
and can't take (average) 300kb saved for every 512x512 area. (note that more complicated terrain is less compressible and will take more space)

However, simply discarding old chunks because they are old is inadvisable. Baritone is extremely good at correcting
itself and its paths as it learns new information, as new chunks load. There is no scenario in which having an
incorrect cache can cause Baritone to get stuck, take damage, or perform any action it wouldn't otherwise, everything
is rechecked once the real chunk is in range.

Having a robust cache greatly improves long distance pathfinding, as it's able to go around large scale obstacles
before they're in render distance. In fact, when the chunkCaching setting is disabled and Baritone starts anew
every time, or when you enter a completely new and very complicated area, it backtracks far more often because it
has to build up that cache from scratch. But after it's gone through an area just once, the next time will have zero
backtracking, since the entire area is now known and cached.

*Default:* `-1L` (Long)

### logger

The function that is called when Baritone will log to chat. This function can be added to
via {@link Consumer#andThen(Consumer)} or it can completely be overriden via setting
{@link Setting#value};
*/
public Setting<Consumer<ITextComponent>> logger = new Setting<>(Minecraft.getMinecraft().ingameGUI.getChatGUI()::printChatMessage);

*Default:* `Minecraft.getMinecraft().ingameGUI.getChatGUI()::printChatMessage` (Consumer<ITextComponent>)

### colorCurrentPath
The color of the current path

*Default:* `Color.RED` (Color)

### colorNextPath
The color of the next path

*Default:* `Color.MAGENTA` (Color)

### colorBlocksToBreak
The color of the blocks to break

*Default:* `Color.RED` (Color)

### colorBlocksToPlace
The color of the blocks to place

*Default:* `Color.GREEN` (Color)

### colorBlocksToWalkInto
The color of the blocks to walk into

*Default:* `Color.MAGENTA` (Color)

### colorBestPathSoFar
The color of the best path so far

*Default:* `Color.BLUE` (Color)

### colorMostRecentConsidered
The color of the path to the most recent considered node

*Default:* `Color.CYAN` (Color)

### colorGoalBox
The color of the goal box

*Default:* `Color.GREEN` (Color)
