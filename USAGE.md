(assuming you already have Baritone [set up](SETUP.md))

# Prefix

Baritone's chat control prefix is `#` by default. In Impact, you can also use `.b` as a prefix. (for example, `.b click` instead of `#click`)

Baritone commands can also by default be typed in the chatbox. However if you make a typo, like typing "gola 10000 10000" instead of "goal" it goes into public chat, which is bad, so using `#` is suggested.

To disable direct chat control (with no prefix), turn off the `chatControl` setting. To disable chat control with the `#` prefix, turn off the `prefixControl` setting. In Impact, `.b` cannot be disabled. Be careful that you don't leave yourself with all control methods disabled (if you do, reset your settings by deleting the file `minecraft/baritone/settings.txt` and relaunching).

# For Baritone 1.2.10+, 1.3.5+, 1.4.2+

Lots of the commands have changed, BUT `#help` is improved vastly (its clickable! commands have tab completion! oh my!).

Try `#help` I promise it won't just send you back here =)

"wtf where is cleararea" -> look at `#help sel`

"wtf where is goto death, goto waypoint" -> look at `#help wp`

just look at `#help` lmao

Watch this [showcase video](https://youtu.be/CZkLXWo4Fg4)!

# Commands

[Tutorial playlist](https://www.youtube.com/playlist?list=PLnwnJ1qsS7CoQl9Si-RTluuzCo_4Oulpa)

**All** of these commands may need a prefix before them, as above ^.

`help`

To toggle a boolean setting, just say its name in chat (for example saying `allowBreak` toggles whether Baritone will consider breaking blocks). For a numeric setting, say its name then the new value (like `primaryTimeoutMS 250`). It's case insensitive. To reset a setting to its default value, say `acceptableThrowawayItems reset`. To reset all settings, say `reset`. To see all settings that have been modified from their default values, say `modified`.

Commands in Baritone:

- `thisway 1000` then `path` to go in the direction you're facing for a thousand blocks
- `goal x y z` or `goal x z` or `goal y`, then `path` to set a goal to a certain coordinate then path to it
- `goto x y z` or `goto x z` or `goto y` to go to a certain coordinate (in a single step, starts going immediately)
- `locate biome cherry_grove` to find a biome from world generation and automatically travel to it. See [Biome locating](#biome-locating).
- `locate structure end city` or `locate structure end_city` to predict a structure and travel to its area. See [Structure locating](#structure-locating).
- `goal` to set the goal to your player's feet
- `goal clear` to clear the goal
- `cancel` or `stop` to stop everything, `forcecancel` is also an option
- `goto portal` or `goto ender_chest` or `goto block_type` to go to a block. (in Impact, `.goto` is an alias for `.b goto` for the most part)
- `mine diamond_ore iron_ore` to mine diamond ore or iron ore (turn on the setting `legitMine` to only mine ores that it can actually see. It will explore randomly around y=11 until it finds them.) An amount of blocks can also be specified, for example, `mine 64 diamond_ore`.
- `click` to click your destination on the screen. Right click path to on top of the block, left click to path into it (either at foot level or eye level), and left click and drag to select an area (`#help sel` to see what you can do with that selection).
- `follow player playerName` to follow a player. `follow players` to follow any players in range (combine with Kill Aura for a fun time). `follow entities` to follow any entities. `follow entity pig` to follow entities of a specific type.
- `wp` for waypoints. A "tag" is like "home" (created automatically on right clicking a bed) or "death" (created automatically on death) or "user" (has to be created manually). So you might want `#wp save user coolbiome`, then to set the goal `#wp goal coolbiome` then `#path` to path to it. For death, `#wp goal death` will list waypoints under the "death" tag (remember stuff is clickable!)
- `build` to build a schematic. `build blah.schematic` will load `schematics/blah.schematic` and build it with the origin being your player feet. `build blah.schematic x y z` to set the origin. Any of those can be relative to your player (`~ 69 ~-420` would build at x=player x, y=69, z=player z-420).
- `schematica` to build the schematic that is currently open in schematica
- `tunnel` to dig and make a tunnel, 1x2. It will only deviate from the straight line if necessary such as to avoid lava. For a dumber tunnel that is really just cleararea, you can `tunnel 3 2 100`, to clear an area 3 high, 2 wide, and 100 deep.
- `farm` to automatically harvest, replant, or bone meal crops. Use `farm <range>` or `farm <range> <waypoint>` to limit the max distance from the starting point or a waypoint.
- `axis` to go to an axis or diagonal axis at y=120 (`axisHeight` is a configurable setting, defaults to 120).
- `explore x z` to explore the world from the origin of x,z. Leave out x and z to default to player feet. This will continually path towards the closest chunk to the origin that it's never seen before. `explorefilter filter.json` with optional invert can be used to load in a list of chunks to load.
- `invert` to invert the current goal and path. This gets as far away from it as possible, instead of as close as possible. For example, do `goal` then `invert` to run as far as possible from where you're standing at the start.
- `come` tells Baritone to head towards your camera, useful when freecam doesn't move your player position.
- `blacklist` will stop baritone from going to the closest block so it won't attempt to get to it.
- `eta` to get information about the estimated time until the next segment and the goal, be aware that the ETA to your goal is really unprecise.
- `proc` to view miscellaneous information about the process currently controlling Baritone.
- `repack` to re-cache the chunks around you.
- `gc` to call `System.gc()` which may free up some memory.
- `render` to fix glitched chunk rendering without having to reload all of them.
- `reloadall` to reload Baritone's world cache or `saveall` to save Baritone's world cache.
- `find` to search through Baritone's cache and attempt to find the location of the block.
- `surface` or `top` to tell Baritone to head towards the closest surface-like area, this can be the surface or highest available air space.
- `version` to get the version of Baritone you're running
- `damn` daniel

All the settings and documentation are <a href="https://github.com/cabaletta/baritone/blob/master/src/api/java/baritone/api/Settings.java">here</a>. If you find HTML easier to read than Javadoc, you can look <a href="https://baritone.leijurv.com/baritone/api/Settings.html#field.detail">here</a>.

There are about a hundred settings, but here are some fun / interesting / important ones that you might want to look at changing in normal usage of Baritone. The documentation for each can be found at the above links.

- `allowBreak`
- `allowSprint`
- `allowPlace`
- `allowParkour`
- `allowParkourPlace`
- `blockPlacementPenalty`
- `renderCachedChunks` (and `cachedChunksOpacity`) <-- very fun but you need a beefy computer
- `avoidance` (avoidance of mobs / mob spawners)
- `legitMine`
- `followRadius`
- `backfill` (fill in tunnels behind you)
- `buildInLayers`
- `buildRepeatDistance` and `buildRepeatDirection`
- `worldExploringChunkOffset`
- `acceptableThrowawayItems`
- `blocksToAvoidBreaking`
- `mineScanDroppedItems`
- `allowDiagonalAscend`

# Biome locating

For Minecraft **26.2 Java Edition**, use `#locate biome <biome> [radius]`, for example:

```text
#locate biome cherry_grove
#locate biome minecraft:desert 10000
#locate biome lush_caves
```

Biome names support tab completion. The search uses generation data beyond loaded chunks, in your current dimension. In singleplayer, the seed and actual world generator are obtained automatically, including custom generation and world presets; cheats are not required.

In multiplayer, first supply the world's numeric seed or original text seed in the dimension you want to search:

```text
#locate seed -1234567890123456789
#locate biome badlands
```

Text seeds also work, for example `#locate seed oogabooga` or `#locate seed North Carolina`. The entire remaining text is used without quotes, preserving case and internal spaces, and converted using Minecraft's own seed parser; the resulting numeric seed is displayed. Use the seed entered during world creation, not the world's display name. `clear` alone is reserved for removing a saved seed; use its numeric equivalent if that was your original seed text.

`#locate seed` displays the saved seed (or the actual singleplayer seed); `#locate seed clear` removes the saved seed. Zero is a valid seed. Seeds are saved in `locate.properties` alongside the current world's dimension cache, so another server or dimension does not silently inherit one. Re-enter the seed after switching dimensions. A server reset requires updating or clearing its saved seed.

For a multiplayer Overworld using a non-default vanilla preset, use `#locate preset large_biomes` or `#locate preset amplified`; `#locate preset default` restores the normal preset. Multiplayer prediction supports the vanilla Overworld, Nether and End. It assumes **26.2 generation** with the supplied seed and preset. Custom server generators, data packs and chunks generated in older versions may differ. This command does not discover unknown server seeds or send `/seed` or `/locate` commands to a server.

The default search radius is 6400 blocks, with an allowed range of 32–12800. The search visits expanding square rings on a 32-block horizontal and vertical grid, checking heights nearest your current Y first. Within the first matching ring, it chooses the sample closest horizontally, then refines the surrounding area at 4-block resolution. This is not a guaranteed nearest biome boundary; small biomes can still fall between the initial samples. A failed search reports that no sample matched within the requested range.

Baritone prints the predicted X/Y/Z coordinates and begins travelling. Once nearby terrain is loaded, it checks positions within 16 blocks horizontally across the world's height. These checks are spread across ticks. It gives the pathfinder up to 16 nearby goals with existing standing space or a breathing position at the water surface, all in the actual target biome. It does not select solid terrain or an unsupported Y coordinate as a final destination. Normal pathfinding settings still govern travel, including whether it can break or place blocks on the route.

Arrival is only reported after your actual biome matches. If no suitable position exists near the prediction, chunks fail to load, or pathfinding fails, it stops with an explanation. It checks destination positions again during the final approach to account for terrain changes. A suitable position is not a guarantee of a reachable route; this is especially relevant to caves and End islands.

`#stop`, `#cancel` and `#locate cancel` cancel searching and travel. `#pause`/`#resume` pause and resume movement (the background search may finish while paused). Changing worlds, losing process control, starting a replacement locate search or changing saved locate settings discards the old search result.

# Structure locating

```text
#locate structure end city
#locate structure end_city
#locate structure village
#locate structure stronghold 10000
#locate structure bastion
#locate structure #minecraft:ruined_portal
```

Structures use the same current-dimension seed and preset settings as biomes. Singleplayer obtains these from the active generator automatically; multiplayer requires `#locate seed <seed>`. Enter the End before searching for an End City, or the Nether before searching for a fortress or bastion. The locator does not build or use portals to switch dimensions.

Both vanilla registry identifiers and readable names are supported, including `end city`, `ancient city`, `woodland mansion`, `desert temple`, `jungle temple`, `ocean monument`, and `nether fortress`. `village`, `mineshaft`, `shipwreck`, `ruined portal`, and `ocean ruin` search their vanilla structure tags, including variants. An explicit identifier such as `minecraft:shipwreck` restricts the search to that variant. Names and common tags support tab completion.

The optional radius is measured in **blocks**, with the same default 6400 and maximum 12800 as biome searches. Structure searches enumerate Minecraft's random-spread candidates and stronghold rings, apply placement frequency and exclusion rules, and use Minecraft's full structure-start generation. Weighted competition between structures in a shared set is preserved, so a bastion candidate is not incorrectly reported as a fortress. Candidates inside the requested square radius and world border are checked in horizontal-distance order.

No live chunks are generated by this search and no server commands are sent. Multiplayer predictions load the bundled vanilla server data and templates into private generation state. A temporary directory needed by Minecraft's template manager is cleaned up after each search. Singleplayer uses its existing registry and template data with private noise state where supported.

The command prints the predicted coordinates and starts travelling. It checks loaded terrain for standing or swimming positions near the generated structure bounds, allowing a small margin for an entrance or a breathing position above water. It reports reaching the **predicted structure area**, not confirmed structure presence: the client does not receive authoritative structure starts. Existing chunks from older versions, disabled structure generation, custom server generation or changed placement salts can invalidate a prediction. Pathfinding can also fail if no reachable destination exists. Strongholds and End Cities are not guaranteed to have a usable route, and End City prediction does not guarantee a ship.

`#stop`, `#cancel`, `#locate cancel`, pause/resume, replacement searches and world changes behave the same for biome and structure searches.

# Troubleshooting / common issues

## Why doesn't Baritone respond to any of my chat commands?

This could be one of many things.

First, make sure it's actually installed. An easy way to check is seeing if it created the folder `baritone` in your Minecraft folder.

Second, make sure that you're using the prefix properly, and that chat control is enabled in the way you expect.

For example, Impact disables direct chat control. (i.e. anything typed in chat without a prefix will be ignored and sent publicly). **This is a saved setting**, so if you run Impact once, `chatControl` will be off from then on, **even in other clients**.
So you'll need to use the `#` prefix or edit `baritone/settings.txt` in your Minecraft folder to undo that (specifically, remove the line `chatControl false` then restart your client).

## Why can I do `.goto x z` in Impact but nowhere else? Why can I do `-path to x z` in KAMI but nowhere else?

These are custom commands that they added; those aren't from Baritone.
The equivalent you're looking for is `goto x z`.
