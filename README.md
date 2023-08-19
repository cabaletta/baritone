# Warning: Old Branch!!

**This branch (`master`) is Baritone for Minecraft 1.12.2. This is the original version of Minecraft that Baritone was written for, and it was the primary development branch for over 5 years. As such, it's quite mature, and arguably more reliable than Baritone for newer versions of Minecraft. Nevertheless, as of August 2023, with [2b2t's update from 1.12.2 to 1.19.4](https://2b2t.org/update/), I decided to move Baritone's primary development branch accordingly. PRs should now be made against the `1.19.4` branch going forward. This branch might see some fixes going forward, particularly to newer features such as `#elytra`, but it won't be the primary focus anymore.**

The other intermediary branches (`1.13.2`, `1.14.4`, `1.15.2`, `1.16.5`, `1.17.1`, `1.18.2`, `1.19.2`, and `1.19.3`) will probably not receive any updates at all. You can find their last releases in the releases tab, or in the quick download links table.

For `1.16.5` and `1.18.2`, the latest release is fully up to date with the code. ZacSharp merged master into some of those versions even after they were deprecated, if you are for some reason really interested in the latest Baritone bugfixes on these versions of Minecraft, you can build from source as of these commits: [1.13.2](https://github.com/cabaletta/baritone/commit/be54b8ee5b5639f80e3d6809ed1abd52444d8a08), [1.14.4](https://github.com/cabaletta/baritone/commit/be54b8ee5b5639f80e3d6809ed1abd52444d8a08), [1.15.2](https://github.com/cabaletta/baritone/commit/45abbb7fa1062cefc26abbb006a02a4edd6faa32), [1.17.1](https://github.com/cabaletta/baritone/commit/cbf0d79c9c5f7454071dc0a5289261ec9ca4373f), [1.19.2](https://github.com/cabaletta/baritone/commit/217dca53633610edc9483fda7a234e46c839fd99). For `1.19.3`, merging [this](https://github.com/cabaletta/baritone/commit/217dca53633610edc9483fda7a234e46c839fd99) commit into it is trivial and is left as an exercise for the reader. For other versions in between these (for example people always ask in the Discord for 1.16.1), you'll have to figure it out yourself.

# Baritone
<p align="center">
  <a href="https://github.com/cabaletta/baritone/releases/"><img src="https://img.shields.io/github/downloads/cabaletta/baritone/total.svg" alt="GitHub All Releases"/></a>
</p>

<p align="center">
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.12.2-brightgreen.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.13.2-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.14.4-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.15.2-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.16.5-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.17.1-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.18.2-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.19.2-brightgreen.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.19.4-brightgreen.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.20.1-brightgreen.svg" alt="Minecraft"/></a>
</p>

<p align="center">
  <a href="https://travis-ci.com/cabaletta/baritone/"><img src="https://travis-ci.com/cabaletta/baritone.svg?branch=master" alt="Build Status"/></a>
  <a href="https://github.com/cabaletta/baritone/releases/"><img src="https://img.shields.io/github/release/cabaletta/baritone.svg" alt="Release"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-LGPL--3.0%20with%20anime%20exception-green.svg" alt="License"/></a>
  <a href="https://www.codacy.com/gh/cabaletta/baritone/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cabaletta/baritone&amp;utm_campaign=Badge_Grade"><img src="https://app.codacy.com/project/badge/Grade/cadab857dab049438b6e28b3cfc5570e" alt="Codacy Badge"/></a>
  <a href="https://github.com/cabaletta/baritone/blob/master/CODE_OF_CONDUCT.md"><img src="https://img.shields.io/badge/%E2%9D%A4-code%20of%20conduct-blue.svg?style=flat" alt="Code of Conduct"/></a>
  <a href="https://snyk.io/test/github/cabaletta/baritone?targetFile=build.gradle"><img src="https://snyk.io/test/github/cabaletta/baritone/badge.svg?targetFile=build.gradle" alt="Known Vulnerabilities"/></a>
  <a href="https://github.com/cabaletta/baritone/issues/"><img src="https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat" alt="Contributions welcome"/></a>
  <a href="https://github.com/cabaletta/baritone/issues/"><img src="https://img.shields.io/github/issues/cabaletta/baritone.svg" alt="Issues"/></a>
  <a href="https://github.com/cabaletta/baritone/issues?q=is%3Aissue+is%3Aclosed"><img src="https://img.shields.io/github/issues-closed/cabaletta/baritone.svg" alt="GitHub issues-closed"/></a>
  <a href="https://github.com/cabaletta/baritone/pulls/"><img src="https://img.shields.io/github/issues-pr/cabaletta/baritone.svg" alt="Pull Requests"/></a>
  <a href="https://github.com/cabaletta/baritone/graphs/contributors/"><img src="https://img.shields.io/github/contributors/cabaletta/baritone.svg" alt="GitHub contributors"/></a>
  <a href="https://github.com/cabaletta/baritone/commit/"><img src="https://img.shields.io/github/commits-since/cabaletta/baritone/v1.0.0.svg" alt="GitHub commits"/></a>
  <img src="https://img.shields.io/github/languages/code-size/cabaletta/baritone.svg" alt="Code size"/>
  <img src="https://img.shields.io/github/repo-size/cabaletta/baritone.svg" alt="GitHub repo size"/>
  <img src="https://tokei.rs/b1/github/cabaletta/baritone?category=code&style=flat" alt="Lines of Code"/>
  <img src="https://img.shields.io/badge/Badges-36-blue.svg" alt="yes"/>
</p>

<p align="center">
  <a href="https://impactclient.net/"><img src="https://img.shields.io/badge/Impact%20integration-v1.2.14%20/%20v1.3.8%20/%20v1.4.6%20/%20v1.5.3%20/%20v1.6.3-brightgreen.svg" alt="Impact integration"/></a>
  <a href="https://github.com/lambda-client/lambda"><img src="https://img.shields.io/badge/Lambda%20integration-v1.2.17-brightgreen.svg" alt="Lambda integration"/></a>
  <a href="https://github.com/fr1kin/ForgeHax/"><img src="https://img.shields.io/badge/ForgeHax%20%22integration%22-scuffed-yellow.svg" alt="ForgeHax integration"/></a>
  <a href="https://aristois.net/"><img src="https://img.shields.io/badge/Aristois%20add--on%20integration-v1.6.3-green.svg" alt="Aristois add-on integration"/></a>
  <a href="https://rootnet.dev/"><img src="https://img.shields.io/badge/rootNET%20integration-v1.2.14-green.svg" alt="rootNET integration"/></a>
  <a href="https://futureclient.net/"><img src="https://img.shields.io/badge/Future%20integration-v1.2.12%20%2F%20v1.3.6%20%2F%20v1.4.4-red" alt="Future integration"/></a>
  <a href="https://rusherhack.org/"><img src="https://img.shields.io/badge/RusherHack%20integration-v1.2.14-green" alt="RusherHack integration"/></a>
</p>

<p align="center">
  <a href="http://forthebadge.com/"><img src="https://web.archive.org/web/20230604002050/https://forthebadge.com/images/badges/built-with-swag.svg" alt="forthebadge"/></a>
  <a href="http://forthebadge.com/"><img src="https://web.archive.org/web/20230604002050/https://forthebadge.com/images/badges/mom-made-pizza-rolls.svg" alt="forthebadge"/></a>
</p>

A Minecraft pathfinder bot.

Baritone is the pathfinding system used in [Impact](https://impactclient.net/) since 4.4. [Here's](https://www.youtube.com/watch?v=StquF69-_wI) a (very old!) video I made showing off what it can do.

[**Baritone Discord Server**](http://discord.gg/s6fRBAUpmr)

**Quick download links:**

| Forge                                                                                                         | Fabric                                                                                                        |
|---------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| [1.12.2 Forge](https://github.com/cabaletta/baritone/releases/download/v1.2.19/baritone-api-forge-1.2.19.jar) |                                                                                                               |
| [1.16.5 Forge](https://github.com/cabaletta/baritone/releases/download/v1.6.5/baritone-api-forge-1.6.5.jar)   | [1.16.5 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.6.5/baritone-api-fabric-1.6.5.jar) |
| [1.17.1 Forge](https://github.com/cabaletta/baritone/releases/download/v1.7.3/baritone-api-forge-1.7.3.jar)   | [1.17.1 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.7.3/baritone-api-fabric-1.7.3.jar) |
| [1.18.2 Forge](https://github.com/cabaletta/baritone/releases/download/v1.8.5/baritone-api-forge-1.8.5.jar)   | [1.18.2 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.8.5/baritone-api-fabric-1.8.5.jar) |
| [1.19.2 Forge](https://github.com/cabaletta/baritone/releases/download/v1.9.4/baritone-api-forge-1.9.4.jar)   | [1.19.2 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.9.4/baritone-api-fabric-1.9.4.jar) |
| [1.19.3 Forge](https://github.com/cabaletta/baritone/releases/download/v1.9.1/baritone-api-forge-1.9.1.jar)   | [1.19.3 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.9.1/baritone-api-fabric-1.9.1.jar) |
| [1.19.4 Forge](https://github.com/cabaletta/baritone/releases/download/v1.9.3/baritone-api-forge-1.9.3.jar)   | [1.19.4 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.9.3/baritone-api-fabric-1.9.3.jar) |
| [1.20.1 Forge](https://github.com/cabaletta/baritone/releases/download/v1.10.1/baritone-api-forge-1.10.1.jar)   | [1.20.1 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.10.1/baritone-api-fabric-1.10.1.jar) |

**Message for 2b2t players looking for 1.19/1.20 Baritone** Download it from right above ^. But also please check back in a few days for Baritone Elytra ([vid 1](https://youtu.be/4bGGPo8yiHo) [vid 2](https://www.youtube.com/watch?v=pUN9nmINe3I)), which will be ported to 1.19/1.20 soon! It will work on 2b2t with its anticheat, that was the whole point of Baritone Elytra (it's fully vanilla compatible). Also join [**the discord**](http://discord.gg/s6fRBAUpmr). Thanks!

**How to immediately get started:** Type `#goto 1000 500` in chat to go to x=1000 z=500. Type `#mine diamond_ore` to mine diamond ore. Type `#stop` to stop. For more, read [the usage page](USAGE.md) and/or watch this [tutorial playlist](https://www.youtube.com/playlist?list=PLnwnJ1qsS7CoQl9Si-RTluuzCo_4Oulpa). Also try `#elytra` for Elytra flying in the Nether using fireworks.

For other versions of Minecraft or more complicated situations or for development, see [Installation & setup](SETUP.md). Also consider just installing [Impact](https://impactclient.net/), which comes with Baritone and is easier to install than wrangling with version JSONs and zips. For 1.16.5, [click here](https://www.youtube.com/watch?v=_4eVJ9Qz2J8) and see description. Once Baritone is installed, look [here](USAGE.md) for instructions on how to use it. There's a [showcase video](https://youtu.be/CZkLXWo4Fg4) made by @Adovin#6313 on Baritone which I recommend.

This project is an updated version of [MineBot](https://github.com/leijurv/MineBot/),
the original version of the bot for Minecraft 1.8.9, rebuilt for 1.12.2 onwards. Baritone focuses on reliability and particularly performance (it's over [30x faster](https://github.com/cabaletta/baritone/pull/180#issuecomment-423822928) than MineBot at calculating paths).

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

```java
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
