# Usage Guide

Alto Clef has a variety of commands, settings and modes. This will give users an overview on how to use the bot.

Keep in mind this project is still in rapid development. A lot of features are placeholders and a work in progress.

## Commands

Commands are prefixed with `@`. Here's a list along with their functions:

| command                                                         | description                                                                                                                                                                                                                                                                                        | examples                                                                    |
|-----------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| `help`                                                          | Lists all commands                                                                                                                                                                                                                                                                                 |                                                                             |
| `coords`                                                        | Prints the bot's current coordinates. This is here in cases where the `F3` menu gets too crowded.                                                                                                                                                                                                  |
| `equip {material}` or `equip [items...]`                        | Equips a `material` armor set or `[items...]`. If not in the bot's inventory, bot will obtain it.                                                                                                                                                                                                  | `@equip [diamond_chestplate, gold_leggings]` `@equip netherite`             |
| `follow {player = <you>}`                                       | Follow a player. If sent via `/msg`, will follow the player who sent the command.                                                                                                                                                                                                                  | `@follow TacoTechnica` `/msg Bot follow`                                    |
| `hero`                                                          | Kills all hostile mobs                                                                                                                                                                                                                                                                             | `@hero`                                                                     |
| `food {amount}`                                                 | Collects `amount` units of food (1 unit = 1/2 drumstick). Collects from various sources (animals, crops)                                                                                                                                                                                           | `@food 20`                                                                  |
| `gamer`                                                         | Beats the game epic style                                                                                                                                                                                                                                                                          | `@gamer`                                                                    |
| `get [items...]`                                                | Gets all items in `[items...]`. Can either do one item or pass a comma-separated list to get multiple items. Not every resource is get-able.                                                                                                                                                       | `@get diamond 3` `@get [cobblestone 40, wooden_door, glass 4]`              |
| `list`                                                          | Prints a list of all get-able items                                                                                                                                                                                                                                                                |                                                                             |
| `give {player = <you>} {item} {quantity=1}`                     | Gives `player` `quantity` units of `item`, getting said items if the bot doesn't have them. If sent via `/msg`, will follow the player who sent the command.                                                                                                                                       | `/msg Bot give iron_pickaxe`                                                |
| `deposit [items... = <Everything>]`                             | Deposit a list of items in the nearest container, making a chest if we can't find one. Will only deposit items present in the bot's inventory (at the time of running this command). Leave out the list to deposit every non-tool/armor item in the bot's inventory. Useful with command chaining. | `@deposit diamond 3` `@deposit [cobblestone 1000, raw_iron 100]` `@deposit` |
| `stash {x0} {y0} {z0} {x1} {y0} {z1} [items... = <Everything>]` | Same as `@deposit`, but you specify an area from `(x0, y0, z0)` to `(x1, y1, z1)` where the bot stores the item list (these coordinates being a chest stash). Just like `@deposit`, providing no items simply deposits everything in the bots inventory.                                           | `@stash 100 64 100 200 70 100 diamond 3`                                    |
| `goto {x} {y} {z} {dimension=<current>}`                        | Goes to (`x`,`y`, `z`) in a given `dimension`. Travels to `dimension` if not there already. Can also omit coordinates to just go to a dimension. Passing 2 values as coordinates goes to X Z coordinates instead.                                                                                  | `@goto 100 64 100 overworld` `@goto nether` `@goto 100 100`                 |
| `inventory {item=<Entire Inventory>}`                           | Prints the bots inventory, OR how many items of a specific type the bot has. Mostly useful when running through `/msg`.                                                                                                                                                                            | `/msg Bot inventory` `/msg Bot inventory cobblestone`                       |
| `locate_structure {structure_type}`                             | Attempts to locate a `structure_type` structure. Can find strongholds or desert temples.                                                                                                                                                                                                           | `@locate_structure stronghold`, `@locate_structure desert_temple`           |
| `punk {player}`                                                 | Attacks `player`.                                                                                                                                                                                                                                                                                  |                                                                             |
| `reload_settings`                                               | Reloads the local settings file. Run this every time you want your settings to be updated.                                                                                                                                                                                                         |                                                                             |
| `gamma {brightness=1}`                                          | Sets the game's gamma. Useful for testing. 0 is "Moody" and 1 is "Bright", and you can go beyond to enable fullbright.                                                                                                                                                                             | `@gamma 1000`                                                               |
| `status`                                                        | Prints the status of the currently executing command. Mostly useful when running through `/msg`.                                                                                                                                                                                                   |                                                                             |
| `stop`                                                          | Forcefully stops the currently running command. The shortcut `CTRL+K` also achieves this.                                                                                                                                                                                                          |                                                                             |
| `test {testname}`                                               | Runs a "test" command. These vary, and will be described below.                                                                                                                                                                                                                                    |                                                                             |

### Command chaining

Execute consecutve commands by separating them with `;`

`@get iron_axe;get log 100;goto 0 0;give Player log 100`

will get an iron axe, then get 100 logs, then go to `x=0 z=0` and then give 100 logs to a player with the name `Player`

### Notable test commands

Test commands are temporary/only exist as an experiment, but some of these might be interesting.
For example, `@test terminate` runs the terminator.
Here's a list of some highlights.

| test name   | what it does                                                                                                                                                                                                                                                                                         |
|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `terminate` | Runs the terminator task. When without diamond gear, the bot flees players and obtains diamond gear + food. When diamond gear is equipped, the bot hunts any nearby players down and searches for any chunks that were last seen to have players in them.                                            |
| `deadmeme`  | Expects a file to exist in your `.minecraft` directory called `test.txt`. Reads from this file, then displays the text contents of the file by placing signs in a line. Dubbed the "Bee Movie Task" for stupid reasons. Will automatically collect signs and building materials if the bot runs out. |
| `173`       | Attacks any player that doesn't have direct line of sight to the bot and stands still otherwise. Like a weeping angel.                                                                                                                                                                               | 
| `replace`   | Replace grass block within around 100 blocks with crafting tables. Frequently fails when replacing grass blocks next to water.                                                                                                                                                                       |
| `piglin`    | Collects 12 ender pearls via piglin bartering.                                                                                                                                                                                                                                                       |
| `stacked`   | Collects diamond armor, a sword and a crafting table.                                                                                                                                                                                                                                                |
| `netherite` | Same as `stacked` but for netherite gear.                                                                                                                                                                                                                                                            |
| `sign`      | Place a sign nearby that says "Hello there!"                                                                                                                                                                                                                                                         |
| `bed`       | Right clicks a nearby bed to set the bot's spawnpoint, placing one if it does not exist.                                                                                                                                                                                                             |

## Bot Settings/Configuration

After running the game with the mod once, a folder called `altoclef` should appear in your `.minecraft` directory. This
contains `altoclef` related settings and configurations.

With regards to the `altoclef_settings.json` file
Check [Settings.java](https://github.com/gaucho-matrero/altoclef/blob/main/src/main/java/adris/altoclef/Settings.java)

After modifying your local settings, run `@reload_settings` to apply your changes in-game.

## /msg: The Butler System

This bot lets other players run commands on the bot via server messages. When sending a command via `/msg` you may omit
the `@`.

You'll have to append each player's name to a file called `altoclef_butler_whitelist.txt`
located in your `.minecraft/altoclef` directory. Make sure your name is not found in `altoclef_butler_blacklist.txt`
too.

### Disabling/Enabling Blacklist/Whitelist

To toggle whitelist/blacklist, check the `useButlerBlacklist` and `useButlerWhitelist` settings.

**WARNING:** If you set `useButlerWhitelist` to false, ALL PLAYERS will be able to send commands to the bot. Be extra
careful with this one.

### Expect delayed messages

Servers auto-kick players that rapidly send messages. For this reason, the butler will send messages at a delayed rate.
When sending messages, messages to authorized
users are prioritized and sent first.

### Dealing with custom /msg outputs on servers

If the server's whisper /msg system doesn't send players a message that looks like `X whispers to you: Y` and it's not
being
picked up by altoclef, you can append the server's custom whisper format in the `whisperFormats` setting.

**NOTE:** At least for now, you must escape brackets and paranthesis. Example: To receive messages that look
like `[Player -> me] message` the format string is `\[{from} -> {to}\] {message}`

**WARNING:** Be careful with this one as well, as the bot will trust these formats for ALL non-player chat messages.
Messing up the format's ordering can let unauthorized people execute bot commands.
