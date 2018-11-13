# Baritone
[![Build Status](https://travis-ci.com/cabaletta/baritone.svg?branch=master)](https://travis-ci.com/cabaletta/baritone)
[![Release](https://img.shields.io/github/release/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/releases)
[![License](https://img.shields.io/github/license/cabaletta/baritone.svg)](LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a73d037823b64a5faf597a18d71e3400)](https://www.codacy.com/app/leijurv/baritone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cabaletta/baritone&amp;utm_campaign=Badge_Grade)
[![HitCount](http://hits.dwyl.com/cabaletta/baritone.svg)](http://hits.dwyl.com/cabaletta/baritone)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/cabaletta/baritone/issues)
[![Minecraft](https://img.shields.io/badge/MC-1.12.2-green.svg)](https://minecraft.gamepedia.com/1.12.2)

A Minecraft pathfinder bot. 

Baritone is the pathfinding system used in [Impact](https://impactdevelopment.github.io/) since 4.4. There's a [showcase video](https://www.youtube.com/watch?v=yI8hgW_m6dQ) made by @Adovin#3153 on Baritone's integration into Impact. [Here's](https://www.youtube.com/watch?v=StquF69-_wI) a video I made showing off what it can do.

This project is an updated version of [MineBot](https://github.com/leijurv/MineBot/),
the original version of the bot for Minecraft 1.8, rebuilt for 1.12.2. Baritone focuses on reliability and particularly performance (it's over [29x faster](https://github.com/cabaletta/baritone/pull/180#issuecomment-423822928) than MineBot at calculating paths).

Here are some links to help to get started:

- [Features](FEATURES.md)

- [Installation](INSTALL.md)

There's also some useful information down below

# Setup

## Command Line
On Mac OSX and Linux, use `./gradlew` instead of `gradlew`.

Running Baritone:

```
$ gradlew runClient
```

Building Baritone:
```
$ gradlew build
```

For example, to replace out Impact 4.4's Baritone build with a customized one, switch to the `impact4.4-compat` branch, build Baritone as above then copy `dist/baritone-api-$VERSION$.jar` into `minecraft/libraries/cabaletta/baritone-api/1.0.0/baritone-api-1.0.0.jar`, replacing the jar that was previously there. You also need to edit `minecraft/versions/1.12.2-Impact_4.4/1.12.2-Impact_4.4.json`, find the line `"name": "cabaletta:baritone-api:1.0.0"`, remove the comma from the end, and entirely remove the line that's immediately after (starts with `"url"`). 

## IntelliJ's Gradle UI
- Open the project in IntelliJ as a Gradle project
- Run the Gradle tasks `setupDecompWorkspace` then `genIntellijRuns`
- Refresh the Gradle project (or, to be safe, just restart IntelliJ)
- Select the "Minecraft Client" launch config
- In `Edit Configurations...` you may need to select `baritone_launch` for `Use classpath of module:`.

# Chat control
[Defined Here](src/main/java/baritone/utils/ExampleBaritoneControl.java)

Quick start example: `thisway 1000` or `goal 70` to set the goal, `path` to actually start pathing. Also try `mine diamond_ore`. `cancel` to cancel.

# API example

```
BaritoneAPI.getSettings().allowSprint.value = true;
BaritoneAPI.getSettings().pathTimeoutMS.value = 2000L;

BaritoneAPI.getPathingBehavior().setGoal(new GoalXZ(10000, 20000));
BaritoneAPI.getPathingBehavior().path();
```

# FAQ

## Can I use Baritone as a library in my hacked client?

Sure! (As long as usage is in compliance with the LGPL 3 License)

## How is it so fast?

Magic. (Hours of [Leijurv](https://github.com/leijurv) enduring excruciating pain)

## Why is it called Baritone?

It's named for FitMC's deep sultry voice. 
