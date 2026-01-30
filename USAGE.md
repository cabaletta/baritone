(assuming you already have Baritone [set up](SETUP.md))

# Prefix

Baritone's chat control prefix is `#` by default. Some clients add alternative aliases.

Baritone commands can be typed in the chatbox by default. However if you make a typo, like typing "gola 10000 10000" instead of "goal" it goes into public chat, which is bad, so using `#` is suggested.

To disable direct chat control (with no prefix), turn off the `chatControl` setting. To disable chat control with the `#` prefix, turn off the `prefixControl` setting. Be careful that you don't leave yourself with all control methods disabled (if you do, open the settings file in `<Minecraft directory>/baritone/settings.txt`, change the settings back, and relaunch the game).

# Getting Help

Try `help` - it has clickable commands and tab completion! Different help sections are available:

- `help` - Show all commands with pagination
- `help <command>` - Show detailed help for a specific command (e.g., `help sel`)

Watch this [showcase video](https://youtu.be/CZkLXWo4Fg4)!

# Settings

To toggle a boolean setting, just say its name in chat (for example saying `allowBreak` toggles whether Baritone will consider breaking blocks). For a numeric setting, say its name then the new value (like `primaryTimeoutMS 250`). It's case insensitive. To reset a setting to its default value, say `acceptableThrowawayItems reset`. To reset all settings, say `reset all`. To see all settings that have been modified from their default values, say `modified`.

# Core Commands

Commands may have a prefix before them (# by default).

## Basic Movement & Goals
- `goal x y z` or `goal x z` or `goal y` - Set a goal to coordinates, then use `path` to path to it
- `goal` - Set the goal to your current position
- `goal clear` - Clear the current goal
- `goto x y z` or `goto x z` or `goto y` - Go to coordinates immediately (combines goal + path)
- `goto <block_type>` - Go to the nearest block of that type (e.g., `goto diamond_ore`, `goto portal`)
- `thisway <distance>` - Set goal in the direction you're facing, then `path` to start
- `path` - Start pathfinding to the current goal
- `cancel` or `stop` - Stop all current processes
- `forcecancel` - Force stop everything (more aggressive)
- `pause` - Pause the current process
- `resume` - Resume a paused process
- `come` - Head towards your camera position (useful with freecam)

## Advanced Movement
- `surface` or `top` - Go to the highest accessible block (surface or air pocket)
- `axis` - Go to world axis at configurable height
- `invert` - Invert current goal (go as far away as possible instead of close)
- `tunnel [width] [height] [length]` - Dig a tunnel (default 1x2, or specify dimensions)
- `blacklist` - While using `goto <block>`, blacklist the closest target block to skip unreachable instances

## Mining & Building
- `mine <block_types>` - Mine specified blocks (e.g., `mine diamond_ore iron_ore`)
- `mine <quantity> <block_types>` - Mine a specific amount (e.g., `mine 64 diamond_ore`)
- `build <schematic>` - Build a schematic from the schematics folder
- `build <schematic> <x> <y> <z>` - Build at specific coordinates (can use relative: `~ ~1 ~-5`)
- `litematica [index]` - Build the currently loaded litematica schematic
- `farm [range] [waypoint]` - Automatically farm crops within range

## Selection & WorldEdit-like Commands
The `sel` command provides WorldEdit-like functionality for area manipulation:
- `sel pos1` or `sel p1` - Set position 1 to current location
- `sel pos2` or `sel p2` - Set position 2 to current location  
- `sel pos1 <x> <y> <z>` - Set position 1 to specific coordinates
- `sel clear` or `sel c` - Clear all selections
- `sel undo` or `sel u` - Undo last selection action
- `sel set <block>` or `sel fill <block>` - Fill selection with blocks
- `sel cleararea` or `sel ca` - Clear area (set to air)
- `sel walls <block>` - Fill walls of selection
- `sel shell <block>` - Fill walls, floor, and ceiling
- `sel replace <from_blocks...> <to_block>` - Replace specific blocks
- `sel sphere <block>` - Fill with sphere shape
- `sel hsphere <block>` - Fill with hollow sphere
- `sel cylinder <block> [axis]` - Fill with cylinder (default Y axis)
- `sel hcylinder <block> [axis]` - Fill with hollow cylinder
- `sel copy [position]` - Copy selection to clipboard
- `sel paste [position]` - Paste from clipboard
- `sel expand <target> <direction> <blocks>` - Expand selection
- `sel contract <target> <direction> <blocks>` - Contract selection
- `sel shift <target> <direction> <blocks>` - Shift selection

## Elytra Flying
**Important:** Elytra requires accepting terms and configuring seed settings. See detailed help with `help elytra`.

For automated nether travel using elytra and fireworks:
- `elytra` - Fly to current goal using elytra (nether only)
- `elytra reset` - Reset elytra state but keep same goal
- `elytra repack` - Queue chunks for repacking  
- `elytra supported` - Check if your system supports elytra features

## Following & Item Management
- `follow player <name>` - Follow a specific player
- `follow players` - Follow any nearby players
- `follow entities` - Follow any nearby entities
- `follow entity <type>` - Follow specific entity type (e.g., `follow entity pig`)
- `pickup` - Pick up all nearby items
- `pickup <items...>` - Pick up specific items only (e.g., `pickup diamond emerald`)

## World Exploration
- `explore [x] [z]` - Explore world from origin point (default: current position)
- `explorefilter <json_file> [invert]` - Apply exploration filter from JSON file
- `find <block_type>` - Search cache for block locations
- `click` - Click to set destinations (left/right click, drag for selections)

## Waypoints
Full waypoint system for saving and navigating to locations:
- `wp` or `waypoints` - List all waypoints
- `wp save <tag> <name>` - Save current location as waypoint
- `wp goal <tag> <name>` - Set goal to waypoint
- `wp goto <tag> <name>` - Go to waypoint immediately
- `wp list [tag]` - List waypoints (optionally filtered by tag)
- `wp delete <tag> <name>` - Delete a waypoint
- `sethome` - Quick save home waypoint
- `home` - Quick go to home waypoint

Common tags: `home` (bed locations), `death` (death locations), `user` (manual waypoints)

## Information & Debugging
- `version` - Show Baritone version
- `proc` - Show information about current process
- `eta` - Show estimated time to goal

## System Commands
- `repack` - Re-cache surrounding chunks
- `reloadall` - Reload world cache from disk
- `saveall` - Save world cache to disk
- `render` - Fix glitched chunk rendering
- `gc` - Call garbage collection

# Settings

All the settings and documentation are available in the [Settings.java file](https://github.com/cabaletta/baritone/blob/master/src/api/java/baritone/api/Settings.java) and [online documentation](https://baritone.leijurv.com/baritone/api/Settings.html#field.detail).

There are over 100 settings available. Here are the most important ones for general usage:

## Core Behavior Settings
- `allowBreak` - Allow breaking blocks
- `allowBreakAnyway` - Break blocks even if they're in the avoid list
- `allowSprint` - Allow sprinting
- `allowPlace` - Allow placing blocks
- `allowInventory` - Allow moving items in inventory
- `allowParkour` - Allow parkour movements
- `allowParkourPlace` - Allow placing blocks for parkour
- `allowDiagonalAscend` - Allow diagonal ascending movements
- `allowDiagonalDescend` - Allow diagonal descending movements

## Pathing & Performance
- `primaryTimeoutMS` - Max time for pathfinding
- `planAheadTimeoutMS` - Max time for planning ahead
- `blockPlacementPenalty` - Cost penalty for placing blocks
- `blockBreakAdditionalPenalty` - Additional cost for breaking blocks

## Visual & Cache Settings  
- `renderCachedChunks` - Render cached chunks (performance intensive)
- `cachedChunksOpacity` - Opacity of rendered cached chunks
- `chunkCaching` - Enable chunk caching for long-distance pathing
- `renderPath` - Render the current path
- `renderGoal` - Render the current goal

## Mining Settings
- `legitMine` - Only mine blocks you can see
- `avoidance` - Avoid mobs and spawners (impacts performance)
- `mobSpawnerAvoidanceRadius` - Distance to avoid spawners
- `mineScanDroppedItems` - Scan for dropped items while mining

## Building Settings
- `buildInLayers` - Build schematics layer by layer
- `buildRepeatDistance` - Distance for repeating builds
- `buildRepeatDirection` - Direction for repeating builds
- `buildSkipBlocks` - Blocks to consider already correct
- `buildIgnoreBlocks` - Blocks to treat as air in schematics

## Miscellaneous Settings
- `backfill` - Fill in tunnels behind you
- `followRadius` - Radius for following entities
- `acceptableThrowawayItems` - Items Baritone can use for bridging/pillaring
- `blocksToAvoidBreaking` - Blocks to avoid breaking (chests, crafting tables, etc.)
- `chatControl` - Enable direct chat control
- `prefixControl` - Enable prefix-based chat control

## Elytra Settings
- `elytraAutoJump` - Automatically jump to take off
- `elytraFireworkSpeed` - Speed multiplier with fireworks
- `elytraPredictTerrain` - Use seed for terrain prediction
- `elytraNetherSeed` - World seed for terrain prediction
- `elytraConserveFireworks` - Use fireworks more conservatively
- `elytraTermsAccepted` - Accept elytra usage terms (required for elytra commands)



# Troubleshooting / Common Issues

## Why doesn't Baritone respond to any of my chat commands?
This could be several things:

1. **Check Installation**: Make sure Baritone is installed. It should create a `baritone` folder in your Minecraft directory.

2. **Check Settings File**: Some clients disable direct chat control by default. This is a **saved setting** that persists across different clients. Check `minecraft/baritone/settings.txt` for lines containing `chatControl false` or `prefixControl false` and delete those lines, then restart your client.

3. **Use Prefix**: Try using the `#` prefix (e.g., `#help`) if direct chat control is disabled.

## Common Command Issues

**Q: Why can I do certain commands in some clients but not others?**  
A: Some clients add custom commands beyond Baritone's built-in commands.

## Performance Issues

**Q: Baritone is running slowly or causing lag**  
A: Try these settings:
- Disable `renderCachedChunks` if enabled
- Disable `avoidance` (it impacts performance significantly)
- If mining frequently, check that `mineScanUpdateInterval` isn't set too low - if lower than the duration of a single scan, it can freeze the game

## Building & Mining Issues

**Q: Builder is not placing blocks correctly**  
A: Check these settings:
- Ensure `allowPlace` is enabled
- Verify you have the correct blocks in inventory
- Check `buildSkipBlocks` and `buildIgnoreBlocks` settings
- Try `buildInLayers` for complex builds

**Q: Mining is not working efficiently**  
A: Consider these settings:
- Adjust `mineScanDroppedItems` to collect drops
- Set appropriate `blocksToAvoidBreaking` list

## Elytra Issues

**Q: Elytra commands not working**  
A: Elytra requires specific setup:
- Must be in the Nether dimension
- Set `elytraTermsAccepted` to true
- Configure `elytraNetherSeed` if using terrain prediction
- Check `elytra supported` to verify system compatibility

**Q: Elytra flying is unreliable**  
A: Try these settings:
- Set `elytraPredictTerrain` to false if you don't know the seed
- Increase `elytraMinFireworksBeforeLanding` for safety
- Enable `elytraAllowEmergencyLand` to prevent crashes

## General Tips

1. **Use `help <command>`** for detailed information about specific commands
2. **Check `modified`** to see what settings you've changed from defaults  
3. **Use `reset all`** to restore all settings to defaults if things go wrong
4. **Enable `chatDebug`** temporarily to see what Baritone is doing
5. **Use `proc`** to understand what process is currently running
6. **Try `cancel` first and `forcecancel` if that didn't work** when Baritone seems stuck

## Getting More Help

- Use `version` to check your Baritone version when reporting issues
- Check the [GitHub repository](https://github.com/cabaletta/baritone) for documentation and issue reports
- Join the community Discord for real-time help and discussion
- Browse the [Settings documentation](https://baritone.leijurv.com/baritone/api/Settings.html) for detailed configuration options
