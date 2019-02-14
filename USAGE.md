	(assuming you already have Baritone [set up](SETUP.md))

	#Don't speak english?
		- Traduction de ce guide en français [français](USAGE-FR.md)

	# Prefix

Baritone commands can by default be typed in the chatbox. However if you make a typo, like typing "gola 10000 10000" instead of goal it goes into public chat, which is bad.

Therefore you can use a prefix before your messages.

On Baritone v1.1.0 and newer: The prefix is `#` by default. Anything beginning with `#` isn't sent, and is only interpreted by Baritone.
For older than v1.1.0, `#` must be enabled by toggling on the `prefix` setting.

**Only** in Impact 4.4 is `.b` also a valid prefix. In 4.4, `#` does **not** work, neither does saying the commands directly in chat.

Other clients like Kami and Asuna have their own custom things (like `-path`), and can disable direct chat control entirely.



# Commands

**All** of these commands may need a prefix before them, as above ^.

`help` for (rudimentary) help. You can see what it says [here](https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/utils/ExampleBaritoneControl.java#L53).

To toggle a boolean setting, just say its name in chat (for example saying `allowBreak` toggles whether Baritone will consider breaking blocks). For a numeric setting, say its name then the new value (like `primaryTimeoutMS 250`). It's case insensitive.




Some common examples:
- `thisway 1000` then `path` to go in the direction you're facing for a thousand blocks
- `goal x y z` or `goal x z` or `goal y`, then `path` to go to a certain coordinate
- `goal` to set the goal to your player's feet
- `goal clear` to clear the goal
- `cancel` or `stop` to stop everything
- `goto portal` or `goto ender_chest` or `goto block_type` to go to a block. (in Impact, `.goto` is an alias for `.b goto` for the most part)
- `mine diamond_ore` to mine diamond ore (turn on the setting `legitMine` to only mine ores that it can actually see. It will explore randomly around y=11 until it finds them.)
- `follow playerName` to follow a player. `follow` to follow the entity you're looking at (only works if it hitting range). `followplayers` to follow any players in range (combine with Kill Aura for a fun time).
- `save waypointName` to save a waypoint. `goto waypointName` to go to it.
- `goto axis` to go to an axis or diagonal axis at y=120 (`axisHeight` is a configurable setting, defaults to 120).
- `invert` to invert the current goal and path. This gets as far away from it as possible, instead of as close as possible. For example, do `goal` then `invert` to run as far as possible from where you're standing at the start.
- `render` to rerender the world in case `renderCachedChunks` is being glitchy
- `damn` daniel

For the rest of the commands, you can take a look at the code [here](https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/utils/ExampleBaritoneControl.java).

All the settings and documentation are <a href="https://github.com/cabaletta/baritone/blob/master/src/api/java/baritone/api/Settings.java">here</a>. If you find HTML easier to read than Javadoc, you can look <a href="https://baritone.leijurv.com/">here</a> and navigate to Settings in the left sidebar.

There are about a hundred settings, but here are some fun / interesting / important ones that you might want to look at changing in normal usage of Baritone. The documentation for each can be found at the above links.
- `allowBreak`
- `allowSprint`
- `allowPlace`
- `allowParkour`
- `allowParkourPlace`
- `renderCachedChunks` (and `cachedChunksOpacity`) <-- very fun but you need a beefy computer
- `avoidance`
- `legitMine`
- `followRadius`



# Troubleshooting / common issues

## Baritone highlights a block in green but gets completely stuck? Also I'm using Baritone with Future?
Baritone is trying to right click to place a block there, but it can't since there's a conflicting mixin. Baritone can't force click right click when Future is also installed. Left click **does work** on recent Baritone even with Future, however. For now, turn off `allowPlace` and Baritone will only search for paths that don't require placing blocks to complete. `allowBreak` can remain on.

## Why doesn't Baritone respond to any of my chat commands?
This could be one of many things.

First, make sure it's actually installed. An easy way to check is seeing if it created the folder `baritone` in your Minecraft folder.

Second, make sure that you're using the prefix properly, and that chat control is enabled in the way you expect.

For example, Impact disables direct chat control. (i.e. anything typed in chat without a prefix will be ignored and sent publicly). **This is a saved setting**, so if you run Impact once, `chatControl` will be off from then on, **even in other clients**.
So you'll need to use the `#` prefix or edit `baritone/settings.txt` in your Minecraft folder to undo that (specifically, remove the line `chatControl false` then restart your client).


## Why can I do `.goto x z` in Impact but nowhere else? Why can I do `-path to x z` in KAMI but nowhere else?
These are custom commands that they added; those aren't from Baritone.
The equivalent you're looking for is `goal x z` then `path`.
