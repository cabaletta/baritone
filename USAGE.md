(assuming you already have Baritone [set up](SETUP.md))

# Prefix

Baritone's chat control prefix is `#` by default.

By default, Baritone commands can also be typed straight into the chat box without a prefix. However, if you make a typo, like typing "gola 10000 10000" instead of "goal", it goes into public chat, which is bad, so using `#` is recommended.

- To disable direct chat control (with no prefix), turn off the `chatControl` setting.
- To disable chat control with the `#` prefix, turn off the `prefixControl` setting.

Be careful that you don't leave yourself with all control methods disabled. If you do, reset your settings by deleting `minecraft/baritone/settings.txt` and relaunching.

# Help

`#help` is your friend (it's clickable! commands have tab completion! oh my!). Try it, I promise it won't just send you back here =)

There's also a [tutorial playlist](https://www.youtube.com/playlist?list=PLnwnJ1qsS7CoQl9Si-RTluuzCo_4Oulpa) and a [showcase video](https://youtu.be/CZkLXWo4Fg4).

# Commands

**All** of these commands may need a prefix before them, as above ^.

## Going places

- `goto x y z` or `goto x z` or `goto y` to go to a certain coordinate (starts going immediately)
- `goto portal` or `goto ender_chest` or `goto block_type` to go to a block
- `goal x y z` or `goal x z` or `goal y`, then `path`, to set a goal and then path to it
- `goal` to set the goal to your player's feet, `goal clear` to clear it
- `thisway 1000` then `path` to go in the direction you're facing for a thousand blocks
- `click` to click your destination on the screen. Right click to path on top of the block, left click to path into it (either at foot level or eye level), and left click and drag to select an area (`#help sel` to see what you can do with that selection)
- `follow player playerName` to follow a player. `follow players` to follow any players in range (combine with Kill Aura for a fun time). `follow entities` to follow any entities. `follow entity pig` to follow entities of a specific type
- `come` to head towards your camera, useful when freecam doesn't move your player position
- `axis` to go to an axis or diagonal axis at y=120 (`axisHeight` is a configurable setting, defaults to 120)
- `surface` or `top` to head towards the closest surface-like area, this can be the surface or highest available air space
- `invert` to invert the current goal and path. This gets as far away from it as possible, instead of as close as possible. For example, do `goal` then `invert` to run as far as possible from where you're standing
- `explore x z` to explore the world from the origin of x,z. Leave out x and z to default to player feet. This will continually path towards the closest chunk to the origin that it's never seen before. `explorefilter filter.json` with optional invert can be used to load in a list of chunks to load
- `elytra` to fly to the current goal using fireworks ([trailer](https://youtu.be/4bGGPo8yiHo), [usage](https://youtu.be/NnSlQi-68eQ))

## Waypoints

`wp` for waypoints. A "tag" is like "bed" (created automatically on right clicking a bed, once per bed, unless `doBedWaypoints` is off), "death" (created automatically on death), "home" (saved with `sethome`) or "user" (has to be created manually).
So you might want `#wp save user coolbiome`, then `#wp goal coolbiome` to set the goal, then `#path` to path to it.
For death, `#wp list death` will list waypoints under the "death" tag (remember stuff is clickable!). `#wp goal death` immediately sets the goal if there is only one death waypoint, otherwise it lists them so you can pick one.

`sethome` and `home` are shortcuts for saving and going to your home waypoint.

## Mining, building and farming

- `mine diamond_ore iron_ore` to mine diamond ore or iron ore. An amount of blocks can also be specified, for example, `mine 64 diamond_ore`. Turn on the setting `legitMine` to only mine ores that it can actually see, it will explore randomly around y=11 until it finds them
- `tunnel` to dig a 1x2 tunnel. It will only deviate from the straight line if necessary, such as to avoid lava. For a dumber tunnel that is really just cleararea, you can `tunnel 3 2 100` to clear an area 3 high, 2 wide, and 100 deep
- `build` to build a schematic. `build blah.schematic` will load `schematics/blah.schematic` and build it with the origin being your player feet. `build blah.schematic x y z` to set the origin. Any of those can be relative to your player (`~ 69 ~-420` would build at x=player x, y=69, z=player z-420)
- `litematica` to build the schematic that is currently open in Litematica
- `farm` to automatically harvest, replant, or bone meal crops. Use `farm <range>` or `farm <range> <waypoint>` to limit the max distance from the starting point or a waypoint. Set `farmWaitForGrowth` to `true` to keep farming active while nearby crops are immature
- `pickup` to pick up dropped items, or `pickup <item1> <item2>` for only certain items
- `blacklist` to stop Baritone from going to the closest block, so it won't try to get to it again
- `find` to search through Baritone's cache for the location of a block

## Control

- `cancel` or `stop` to stop everything, `forcecancel` is also an option
- `pause` and `resume` to pause Baritone and pick up where it left off, `paused` to check
- `eta` to get the estimated time until the next segment and the goal. Be aware that the ETA to your goal is really imprecise
- `proc` to view miscellaneous information about the process currently controlling Baritone
- `version` to get the version of Baritone you're running

## Maintenance

- `repack` to re-cache the chunks around you
- `reloadall` to reload Baritone's world cache, `saveall` to save it
- `render` to fix glitched chunk rendering without having to reload all of them
- `gc` to call `System.gc()`, which may free up some memory
- `damn` daniel

# Settings

To toggle a boolean setting, just say its name in chat (for example, saying `allowBreak` toggles whether Baritone will consider breaking blocks). For a numeric setting, say its name then the new value (like `primaryTimeoutMS 250`). It's case insensitive.

- `reset acceptableThrowawayItems` resets one setting to its default value
- `reset all` resets all settings
- `modified` lists all settings that have been changed from their defaults

All the settings and their documentation are [here](https://github.com/cabaletta/baritone/blob/master/src/api/java/baritone/api/Settings.java). If you find HTML easier to read than Javadoc, look [here](https://baritone.leijurv.com/baritone/api/Settings.html#field.detail).

There are a couple hundred settings, but here are some fun / interesting / important ones that you might want to look at changing in normal usage of Baritone:

- `allowBreak`
- `allowSprint`
- `allowPlace`
- `allowParkour`
- `allowParkourPlace`
- `allowDiagonalAscend`
- `headHitters` (sprint jump head bonks in 1x2 tunnels, slightly faster than plain sprinting)
- `allowSwimming` (sprint swim along the waterline, head out and body in, instead of bobbing through water)
- `blockPlacementPenalty`
- `acceptableThrowawayItems`
- `blocksToAvoidBreaking`
- `avoidance` (avoidance of mobs / mob spawners)
- `legitMine`
- `mineScanDroppedItems`
- `followRadius`
- `backfill` (fill in tunnels behind you)
- `buildInLayers`
- `buildRepeat` and `buildRepeatCount`
- `worldExploringChunkOffset`
- `renderCachedChunks` (and `cachedChunksOpacity`) <-- very fun but you need a beefy computer

# Troubleshooting / common issues

## Why doesn't Baritone respond to any of my chat commands?

This could be one of many things.

First, make sure it's actually installed. An easy way to check is seeing if it created the folder `baritone` in your Minecraft folder.

Second, make sure that you're using the prefix properly, and that chat control is enabled in the way you expect.

For example, some clients turn off direct chat control (i.e. anything typed in chat without a prefix will be ignored and sent publicly). **This is a saved setting**, so if you run one of those once, `chatControl` will be off from then on, **even in other clients**.
So you'll need to use the `#` prefix, or edit `baritone/settings.txt` in your Minecraft folder to undo that (specifically, remove the line `chatControl false`, then restart your client).

## Why can I do `.goto x z` in some client but nowhere else?

That's a custom command the client added, it isn't from Baritone.
The equivalent you're looking for is `goto x z`.
