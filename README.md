# Baritone
[![HitCount](http://hits.dwyl.com/cabaletta/baritone.svg)](http://hits.dwyl.com/cabaletta/baritone/)
[![GitHub All Releases](https://img.shields.io/github/downloads/cabaletta/baritone/total.svg)](https://github.com/cabaletta/baritone/releases/)

[![Build Status](https://travis-ci.com/cabaletta/baritone.svg?branch=master)](https://travis-ci.com/cabaletta/baritone/)
[![Release](https://img.shields.io/github/release/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/releases/)
[![License](https://img.shields.io/badge/license-LGPL--3.0%20with%20anime%20exception-green.svg)](LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a73d037823b64a5faf597a18d71e3400)](https://www.codacy.com/app/leijurv/baritone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cabaletta/baritone&amp;utm_campaign=Badge_Grade)
[![Minecraft](https://img.shields.io/badge/MC-1.12.2-brightgreen.svg)](https://github.com/cabaletta/baritone/tree/master/)
[![Minecraft](https://img.shields.io/badge/MC-1.13.2-brightgreen.svg)](https://github.com/cabaletta/baritone/tree/1.13.2/)
[![Minecraft](https://img.shields.io/badge/MC-1.14.4-brightgreen.svg)](https://github.com/cabaletta/baritone/tree/1.14.4/)
[![Minecraft](https://img.shields.io/badge/MC-1.15.2-brightgreen.svg)](https://github.com/cabaletta/baritone/tree/1.15.2/)
[![Code of Conduct](https://img.shields.io/badge/%E2%9D%A4-code%20of%20conduct-blue.svg?style=flat)](https://github.com/cabaletta/baritone/blob/master/CODE_OF_CONDUCT.md)
[![Known Vulnerabilities](https://snyk.io/test/github/cabaletta/baritone/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/cabaletta/baritone?targetFile=build.gradle)
[![Contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/cabaletta/baritone/issues/)
[![Issues](https://img.shields.io/github/issues/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/issues/)
[![GitHub issues-closed](https://img.shields.io/github/issues-closed/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/issues?q=is%3Aissue+is%3Aclosed)
[![Pull Requests](https://img.shields.io/github/issues-pr/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/pulls/)
![Code size](https://img.shields.io/github/languages/code-size/cabaletta/baritone.svg)
![GitHub repo size](https://img.shields.io/github/repo-size/cabaletta/baritone.svg)
![Lines of Code](https://tokei.rs/b1/github/cabaletta/baritone?category=code)
[![GitHub contributors](https://img.shields.io/github/contributors/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/graphs/contributors/)
[![GitHub commits](https://img.shields.io/github/commits-since/cabaletta/baritone/v1.0.0.svg)](https://github.com/cabaletta/baritone/commit/)
[![Impact integration](https://img.shields.io/badge/Impact%20integration-v1.2.14%20/%20v1.3.8%20/%20v1.4.6%20/%20v1.5.3-brightgreen.svg)](https://impactclient.net/)
[![KAMI Blue integration](https://img.shields.io/badge/KAMI%20Blue%20integration-v1.2.14--master-green)](https://github.com/kami-blue/client)
[![ForgeHax integration](https://img.shields.io/badge/ForgeHax%20%22integration%22-scuffed-yellow.svg)](https://github.com/fr1kin/ForgeHax/)
[![Aristois add-on integration](https://img.shields.io/badge/Aristois%20add--on%20integration-v1.3.4%20/%20v1.4.1-green.svg)](https://gitlab.com/emc-mods-indrit/baritone_api)
[![rootNET integration](https://img.shields.io/badge/rootNET%20integration-v1.2.11-green.svg)](https://rootnet.dev/)
[![WWE integration](https://img.shields.io/badge/WWE%20%22integration%22-master%3F-green.svg)](https://wweclient.com/)
[![Future integration](https://img.shields.io/badge/Future%20integration-v1.2.12%20%2F%20v1.3.6%20%2F%20v1.4.4-red)](https://futureclient.net/)
[![RusherHack integration](https://img.shields.io/badge/RusherHack%20integration-v1.2.14-green)](https://rusherhack.org/)
[![forthebadge](https://forthebadge.com/images/badges/built-with-swag.svg)](http://forthebadge.com/)
[![forthebadge](https://forthebadge.com/images/badges/mom-made-pizza-rolls.svg)](http://forthebadge.com/)

A Minecraft pathfinder bot. 

Baritone is the pathfinding system used in [Impact](https://impactclient.net/) since 4.4. There's a [showcase video](https://youtu.be/CZkLXWo4Fg4) made by @Adovin#0730 on Baritone which I recommend. [Here's](https://www.youtube.com/watch?v=StquF69-_wI) a (very old!) video I made showing off what it can do.

The easiest way to install Baritone is to install [Impact](https://impactclient.net/), which comes with Baritone. The second easiest way (for 1.12.2 only) is to install the v1.2.* `api-forge` jar from [releases](https://github.com/cabaletta/baritone/releases). **For 1.12.2 Forge, just click [here](https://github.com/cabaletta/baritone/releases/download/v1.2.14/baritone-api-forge-1.2.14.jar)**. Otherwise, see [Installation & setup](SETUP.md). Once Baritone is installed, look [here](USAGE.md) for instructions on how to use it.

For 1.15.2, [click here](https://www.youtube.com/watch?v=j1qKtCZFURM) and see description. If you need Forge 1.15.2, look [here](https://github.com/cabaletta/baritone/releases/tag/v1.5.3), follow the instructions, and get the `api-forge` jar.

This project is an updated version of [MineBot](https://github.com/leijurv/MineBot/),
the original version of the bot for Minecraft 1.8.9, rebuilt for 1.12.2 through 1.15.2. Baritone focuses on reliability and particularly performance (it's over [30x faster](https://github.com/cabaletta/baritone/pull/180#issuecomment-423822928) than MineBot at calculating paths).

Have committed at least once a day from Aug 1, 2018, to Aug 1, 2019.

1Leijurv3DWTrGAfmmiTphjhXLvQiHg7K2

# Getting Started

Here are some links to help to get started:

- [Features](FEATURES.md)

- [Installation & setup](SETUP.md)

- [API Javadocs](https://baritone.leijurv.com/)

- [Settings](https://baritone.leijurv.com/baritone/api/Settings.html#field.detail)

- [Usage (chat control)](USAGE.md)

## Stars over time

[![Stargazers over time](https://starchart.cc/cabaletta/baritone.svg)](https://starchart.cc/cabaletta/baritone)

# API

The API is heavily documented, you can find the Javadocs for the latest release [here](https://baritone.leijurv.com/).
Please note that usage of anything located outside of the ``baritone.api`` package is not supported by the API release
jar.

Below is an example of basic usage for changing some settings, and then pathing to an X/Z goal.

```
BaritoneAPI.getSettings().allowSprint.value = true;
BaritoneAPI.getSettings().primaryTimeoutMS.value = 2000L;

BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(10000, 20000));
```

# FAQ

## Can I use Baritone as a library in my custom utility client?

That's what it's for, sure! (As long as usage complies with the LGPL 3.0 License)

## How is it so fast?

Magic. (Hours of [leijurv](https://github.com/leijurv/) enduring excruciating pain)

### Additional Special Thanks To:

![YourKit-Logo](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications.

YourKit is the creator of the [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

We thank them for granting Baritone an OSS license so that we can make our software the best it can be.

## Why is it called Baritone?

It's named for FitMC's deep sultry voice.
