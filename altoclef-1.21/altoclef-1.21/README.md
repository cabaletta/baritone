# altoclef

Plays block game.

Powered by Baritone.

A client side bot that can accomplish any Minecraft task that is relatively simple and can be split into smaller
tasks. "Relatively Simple" is a vague term, so check the list of current capabilities to see examples.

Became [the first bot to beat Minecraft fully autonomously](https://youtu.be/baAa6s8tahA) on May 24, 2021.

**Join the [Discord Server](https://discord.gg/JdFP4Kqdqc)** for discussions/updates/goofs & gaffs

## How it works

Take a look at this [Guide from the wiki](https://github.com/gaucho-matrero/altoclef/wiki/1:-Documentation:-Big-Picture)
or this [Video explanation](https://youtu.be/q5OmcinQ2ck?t=387)

## Current capabilities, Examples:

- Obtain 400+ Items from a fresh survival world, like diamond armor, cake, and nether brick stairs
- Dodge mob projectiles and force field mobs away while accomplishing arbitrary tasks
- Collect + smelt food from animals, hay, & crops
- Receive commands from chat whispers via /msg. Whitelist + Blacklist configurable (hereby dubbed the Butler System).
  Here's
  a [Butler system demo video](https://drive.google.com/file/d/1axVYYMJ5VjmVHaWlCifFHTwiXlFssOUc/view?usp=sharing)
- Simple config file that can be reloaded via command (check .minecraft directory)
- Beat the entire game on its own (no user input.)
- Print the entire bee movie script with signs in a straight line, automatically collecting signs + bridging materials
  along the way.
- Become the terminator: Run away from players while unarmed, gather diamond gear in secret, then return and wreak
  havoc.

## Download

**Note:** After installing, please move/delete your old baritone configurations if you have any. Preexisting baritone
configurations will interfere with altoclef and introduce bugs. This will be fixed in the future.

### Nightly Release (Recommended) (has the latest bug fixes)

Start by downloading [the Latest Long Term Release](https://github.com/gaucho-matrero/altoclef/releases),
then [Download the Nightly](https://nightly.link/gaucho-matrero/altoclef/workflows/gradle/main/Artifacts.zip) &
replace `altoclef-4.0-SNAPSHOT.jar`.

If the Nightly Link doesn't work, check the latest [Build Action](https://github.com/gaucho-matrero/altoclef/actions)
that succeeded and download `Artifacts.zip` (you must be signed into GitHub). Replace your
existing `altoclef-4.0-SNAPSHOT.jar` with the one found in `Artifacts.zip`

### Long Term Release

[Check releases](https://github.com/gaucho-matrero/altoclef/releases). Note you will need to copy over both jar files
for the mod to work.

### Meloweh's Extra Features Release (Unofficial)

Has some schematic support, command macros and a few utility features. Will eventually be merged, but if you can try it
out now if you'd like:

- [AltoClef jar](https://github.com/Meloweh/altoclef/releases)
- [Baritone jar](https://github.com/Meloweh/baritone/releases)

### Versions

This is a **fabric only** mod, currently only available for **Minecraft 1.19.2-1.19.4**.

For older MC versions, try [multiconnect](https://www.curseforge.com/minecraft/mc-mods/multiconnect) (NOTE: multiconnect
is untested and not affiliated with altoclef, use at your own risk!)

## [Usage Guide](usage.md)

## [TODO's/Future Features](todos.md)

## [Development Guide](develop.md)
