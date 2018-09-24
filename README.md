# Baritone
[![Build Status](https://travis-ci.com/cabaletta/baritone.svg?branch=master)](https://travis-ci.com/cabaletta/baritone)
[![License](https://img.shields.io/github/license/cabaletta/baritone.svg)](LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7150d8ccf6094057b1782aa7a8f92d7d)](https://www.codacy.com/app/leijurv/baritone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cabaletta/baritone&amp;utm_campaign=Badge_Grade)

A Minecraft pathfinder bot. This project is an updated version of [MineBot](https://github.com/leijurv/MineBot/),
the original version of the bot for Minecraft 1.8, rebuilt for 1.12.2. Baritone focuses on reliability and particularly performance (it's over [29x faster](https://github.com/cabaletta/baritone/pull/180#issuecomment-423822928) than MineBot at calculating paths).

<a href="https://github.com/cabaletta/baritone/blob/master/FEATURES.md">Features</a>

<a href="https://github.com/cabaletta/baritone/blob/master/IMPACT.md">Baritone + Impact</a>

Unofficial Jenkins: [![Build Status](https://plutiejenkins.leijurv.com/job/baritone/badge/icon)](https://plutiejenkins.leijurv.com/job/baritone/lastSuccessfulBuild/)

# Setup

## IntelliJ's Gradle UI
- Open the project in IntelliJ as a Gradle project
- Run the Gradle task `setupDecompWorkspace`
- Run the Gradle task `genIntellijRuns`
- Refresh the Gradle project (or just restart IntelliJ)
- Select the "Minecraft Client" launch config and run

## Command Line
On Mac OSX and Linux, use `./gradlew` instead of `gradlew`.

Running Baritone:

```
$ gradlew run
```

Setting up for IntelliJ:
```
$ gradlew setupDecompWorkspace
$ gradlew --refresh-dependencies
$ gradlew genIntellijRuns
```

# Chat control
<a href="https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/utils/ExampleBaritoneControl.java">Defined here</a>

Quick start example: `thisway 1000` or `goal 70` to set the goal, `path` to actually start pathing. Also try `mine diamond_ore`. `cancel` to cancel.

# API example

```
BaritoneAPI.getSettings().allowSprint.value = true;
BaritoneAPI.getSettings().pathTimeoutMS.value = 2000L;

// Note that at this moment in time the Goal implementations are not exposed in the API
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