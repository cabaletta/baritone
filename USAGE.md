(assuming you already have Baritone [set up](SETUP.md))

# Prefix

Baritone's chat control prefix is `#` by default. In Impact, you can also use `.b` as a prefix. (for example, `.b click` instead of `#click`)

Baritone commands can also by default be typed in the chatbox. However if you make a typo, like typing "gola 10000 10000" instead of "goal" it goes into public chat, which is bad, so using `#` is suggested.

To disable direct chat control (with no prefix), turn off the `chatControl` setting. To disable chat control with the `#` prefix, turn off the `prefixControl` setting. In Impact, `.b` cannot be disabled. Be careful that you don't leave yourself with all control methods disabled (if you do, reset your settings by deleting the file `minecraft/baritone/settings.txt` and relaunching).

# Commands

**All** of these commands may need a prefix before them, as above ^.

`help` for (rudimentary) help. You can see what it says [here](https://github.com/cabaletta/baritone/blob/master/src/api/java/baritone/api/utils/ExampleBaritoneControl.java#L47).

To toggle a boolean setting, just say its name in chat (for example saying `allowBreak` toggles whether Baritone will consider breaking blocks). For a numeric setting, say its name then the new value (like `primaryTimeoutMS 250`). It's case insensitive. To reset a setting to its default value, say `acceptableThrowawayItems reset`. To reset all settings, say `reset`. To see all settings that have been modified from their default values, say `modified`.




Some common examples:
- `thisway 1000` then `path` to go in the direction you're facing for a thousand blocks
- `goal x y z` or `goal x z` or `goal y`, then `path` to go to a certain coordinate
- `goal` to set the goal to your player's feet
- `goal clear` to clear the goal
- `cancel` or `stop` to stop everything
- `goto portal` or `goto ender_chest` or `goto block_type` to go to a block. (in Impact, `.goto` is an alias for `.b goto` for the most part)
- `mine diamond_ore` to mine diamond ore (turn on the setting `legitMine` to only mine ores that it can actually see. It will explore randomly around y=11 until it finds them.)
- `click` to click your destination on the screen. Right click path to on top of the block, left click to path into it (either at foot level or eye level), and left click and drag to clear all blocks from an area.
- `follow playerName` to follow a player. `follow` to follow the entity you're looking at (only works if it hitting range). `followplayers` to follow any players in range (combine with Kill Aura for a fun time).
- `save waypointName` to save a waypoint. `goto waypointName` to go to it.
- `build` to build a schematic. `build blah` will load `schematics/blah.schematic` and build it with the origin being your player feet. `build blah x y z` to set the origin. Any of those can be relative to your player (`~ 69 ~-420` would build at x=player x, y=69, z=player z-420).
- `tunnel` to dig just straight ahead and make a tunnel
- `farm` to automatically harvest, replant, or bone meal crops
- `axis` to go to an axis or diagonal axis at y=120 (`axisHeight` is a configurable setting, defaults to 120).
- `explore x z` to explore the world from the origin of x,z. Leave out x and z to default to player feet. This will continually path towards the closest chunk to the origin that it's never seen before. `explorefilter filter.json` with optional invert can be used to load in a list of chunks to load.
- `invert` to invert the current goal and path. This gets as far away from it as possible, instead of as close as possible. For example, do `goal` then `invert` to run as far as possible from where you're standing at the start.
- `version` to get the version of Baritone you're running
- `damn` daniel


For the rest of the commands, you can take a look at the code [here](https://github.com/cabaletta/baritone/blob/master/src/api/java/baritone/api/utils/ExampleBaritoneControl.java).

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




# Troubleshooting / common issues

## Why doesn't Baritone respond to any of my chat commands?
This could be one of many things.

First, make sure it's actually installed. An easy way to check is seeing if it created the folder `baritone` in your Minecraft folder.

Second, make sure that you're using the prefix properly, and that chat control is enabled in the way you expect.

For example, Impact disables direct chat control. (i.e. anything typed in chat without a prefix will be ignored and sent publicly). **This is a saved setting**, so if you run Impact once, `chatControl` will be off from then on, **even in other clients**.
So you'll need to use the `#` prefix or edit `baritone/settings.txt` in your Minecraft folder to undo that (specifically, remove the line `chatControl false` then restart your client).


## Why can I do `.goto x z` in Impact but nowhere else? Why can I do `-path to x z` in KAMI but nowhere else?
These are custom commands that they added; those aren't from Baritone.
The equivalent you're looking for is `goal x z` then `path`.
