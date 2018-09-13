# Baritone
[![Build Status](https://travis-ci.com/cabaletta/baritone.svg?branch=master)](https://travis-ci.com/cabaletta/baritone)

A Minecraft bot. This project is an updated version of [Minebot](https://github.com/leijurv/MineBot/),
the original version of the bot for Minecraft 1.8, rebuilt for 1.12.2.

<a href="https://github.com/cabaletta/baritone/blob/master/FEATURES.md">Features</a>

<a href="https://github.com/cabaletta/baritone/blob/master/IMPACT.md">Baritone + Impact</a>

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
Baritone.settings().allowSprint.value = true;
Baritone.settings().pathTimeoutMS.value = 2000L;

PathingBehavior.INSTANCE.setGoal(new GoalXZ(10000, 20000));
PathingBehavior.INSTANCE.path();
```

# FAQ

## Can I use Baritone as a library in my hacked client?

Sure! (As long as usage is in compliance with the GPL 3 License)

## How is it so fast?

Magic
