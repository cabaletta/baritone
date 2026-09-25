<h1 align="center">Baritone</h1>

<p align="center"><b>A Minecraft pathfinder bot.</b></p>

<p align="center">
  <a href="#downloads"><img src="https://img.shields.io/badge/MC-1.12.2%20--%201.21.3-yellow.svg" alt="Minecraft 1.12.2 - 1.21.3"/></a>
  <a href="#downloads"><img src="https://img.shields.io/badge/MC-1.21.4%20--%2026.3-brightgreen.svg" alt="Minecraft 1.21.4 - 26.3"/></a>
</p>

<p align="center">
  <a href="https://github.com/cabaletta/baritone/releases/"><img src="https://img.shields.io/github/downloads/cabaletta/baritone/total.svg" alt="GitHub All Releases"/></a>
  <a href="https://github.com/cabaletta/baritone/actions/workflows/gradle_build.yml"><img src="https://github.com/cabaletta/baritone/actions/workflows/gradle_build.yml/badge.svg" alt="Build Status"/></a>
  <a href="https://github.com/cabaletta/baritone/releases/"><img src="https://img.shields.io/github/release/cabaletta/baritone.svg" alt="Release"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-LGPL--3.0%20with%20anime%20exception-green.svg" alt="License"/></a>
  <a href="http://discord.gg/s6fRBAUpmr"><img src="https://img.shields.io/badge/chat-discord-5865F2.svg" alt="Discord"/></a>
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
  <img src="https://img.shields.io/badge/Badges-36-blue.svg" alt="yes"/>
</p>

<p align="center">
  <a href="http://forthebadge.com/"><img src="https://web.archive.org/web/20230604002050/https://forthebadge.com/images/badges/built-with-swag.svg" alt="forthebadge"/></a>
  <a href="http://forthebadge.com/"><img src="https://web.archive.org/web/20230604002050/https://forthebadge.com/images/badges/mom-made-pizza-rolls.svg" alt="forthebadge"/></a>
</p>

<p align="center">
  <a href="#quick-start">Quick start</a> ·
  <a href="#downloads">Downloads</a> ·
  <a href="USAGE.md">Usage</a> ·
  <a href="FEATURES.md">Features</a> ·
  <a href="SETUP.md">Setup</a> ·
  <a href="https://baritone.leijurv.com/">Javadocs</a> ·
  <a href="http://discord.gg/s6fRBAUpmr">Discord</a>
</p>

Baritone walks, mines, builds, farms and flies for you. Give it a goal and it figures out how to get there: breaking and placing blocks, parkour, ladders, water buckets, the lot.

It's the pathfinding system used in [Impact](https://impactclient.net/) since 4.4, and an updated version of [MineBot](https://github.com/leijurv/MineBot/), the original bot for Minecraft 1.8.9. Baritone focuses on reliability and particularly performance (it's over [30x faster](https://github.com/cabaletta/baritone/pull/180#issuecomment-423822928) than MineBot at calculating paths).

[Here's](https://www.youtube.com/watch?v=StquF69-_wI) a (very old!) video I made showing off what it can do, and there's a [showcase video](https://youtu.be/CZkLXWo4Fg4) by @Adovin#6313 which I recommend.

# Quick start

1. Download the jar for your Minecraft version and mod loader from [the table below](#downloads) and drop it in your `mods` folder.
2. In game, type `#goto 1000 500` to go to x=1000 z=500, `#mine diamond_ore` to mine diamond ore, and `#stop` to stop.
3. Try `#elytra` for Elytra flying using fireworks ([trailer](https://youtu.be/4bGGPo8yiHo), [usage](https://youtu.be/NnSlQi-68eQ)).

After that, `#help` lists every command (it's clickable!). There's also [the usage page](USAGE.md) and a [tutorial playlist](https://www.youtube.com/playlist?list=PLnwnJ1qsS7CoQl9Si-RTluuzCo_4Oulpa). Stuck? Ask in the [Discord](http://discord.gg/s6fRBAUpmr).

# Downloads

> [!IMPORTANT]
> Before asking in the Discord for a new version of Baritone, check the [releases page](https://github.com/cabaletta/baritone/releases) for the Minecraft version you want.
> If it's not there yet, please don't ask in the Discord. It'll be released soon enough.

These are the `api` jars, which is what you want unless you know otherwise. See [setup](SETUP.md) for the other flavors and for building it yourself.

| Minecraft | Baritone | Download |
|---|---|---|
| 26.3 | v1.20.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.20.0/baritone-api-forge-1.20.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.20.0/baritone-api-fabric-1.20.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.20.0/baritone-api-neoforge-1.20.0.jar) |
| 26.2 | v1.19.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.19.0/baritone-api-forge-1.19.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.19.0/baritone-api-fabric-1.19.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.19.0/baritone-api-neoforge-1.19.0.jar) |
| 26.1 | v1.18.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.18.0/baritone-api-forge-1.18.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.18.0/baritone-api-fabric-1.18.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.18.0/baritone-api-neoforge-1.18.0.jar) |
| 1.21.11 | v1.17.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.17.0/baritone-api-forge-1.17.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.17.0/baritone-api-fabric-1.17.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.17.0/baritone-api-neoforge-1.17.0.jar) |
| 1.21.10 | v1.16.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.16.0/baritone-api-forge-1.16.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.16.0/baritone-api-fabric-1.16.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.16.0/baritone-api-neoforge-1.16.0.jar) |
| 1.21.9 | v1.16.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.16.0/baritone-api-forge-1.16.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.16.0/baritone-api-fabric-1.16.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.16.0/baritone-api-neoforge-1.16.0.jar) |
| 1.21.8 | v1.15.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-forge-1.15.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-fabric-1.15.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-neoforge-1.15.0.jar) |
| 1.21.7 | v1.15.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-forge-1.15.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-fabric-1.15.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-neoforge-1.15.0.jar) |
| 1.21.6 | v1.15.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-forge-1.15.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-fabric-1.15.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-neoforge-1.15.0.jar) |
| 1.21.5 | v1.14.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.14.0/baritone-api-forge-1.14.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.14.0/baritone-api-fabric-1.14.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.14.0/baritone-api-neoforge-1.14.0.jar) |
| 1.21.4 | v1.13.1 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.13.1/baritone-api-forge-1.13.1.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.13.1/baritone-api-fabric-1.13.1.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.13.1/baritone-api-neoforge-1.13.1.jar) |

<details>
<summary>Older versions</summary>

| Minecraft | Baritone | Download |
|---|---|---|
| 1.21.3 | v1.12.0 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.12.0/baritone-api-forge-1.12.0.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.12.0/baritone-api-fabric-1.12.0.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.12.0/baritone-api-neoforge-1.12.0.jar) |
| 1.21.1 | v1.11.3 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.11.3/baritone-api-forge-1.11.3.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.11.3/baritone-api-fabric-1.11.3.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.11.3/baritone-api-neoforge-1.11.3.jar) |
| 1.20.6 | v1.10.8 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.10.8/baritone-api-forge-1.10.8.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.10.8/baritone-api-fabric-1.10.8.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.10.8/baritone-api-neoforge-1.10.8.jar) |
| 1.20.5 | v1.10.8 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.10.8/baritone-api-forge-1.10.8.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.10.8/baritone-api-fabric-1.10.8.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.10.8/baritone-api-neoforge-1.10.8.jar) |
| 1.20.4 | v1.10.7 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.10.7/baritone-api-forge-1.10.7.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.10.7/baritone-api-fabric-1.10.7.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.10.7/baritone-api-neoforge-1.10.7.jar) |
| 1.20.3 | v1.10.7 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.10.7/baritone-api-forge-1.10.7.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.10.7/baritone-api-fabric-1.10.7.jar) · [NeoForge](https://github.com/cabaletta/baritone/releases/download/v1.10.7/baritone-api-neoforge-1.10.7.jar) |
| 1.20.2 | v1.10.6 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.10.6/baritone-api-forge-1.10.6.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.10.6/baritone-api-fabric-1.10.6.jar) |
| 1.20.1 | v1.10.5 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.10.5/baritone-api-forge-1.10.5.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.10.5/baritone-api-fabric-1.10.5.jar) |
| 1.19.4 | v1.9.6 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.9.6/baritone-api-forge-1.9.6.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.9.6/baritone-api-fabric-1.9.6.jar) |
| 1.19.3 | v1.9.1 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.9.1/baritone-api-forge-1.9.1.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.9.1/baritone-api-fabric-1.9.1.jar) |
| 1.19.2 | v1.9.4 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.9.4/baritone-api-forge-1.9.4.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.9.4/baritone-api-fabric-1.9.4.jar) |
| 1.18.2 | v1.8.6 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.8.6/baritone-api-forge-1.8.6.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.8.6/baritone-api-fabric-1.8.6.jar) |
| 1.17.1 | v1.7.3 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.7.3/baritone-api-forge-1.7.3.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.7.3/baritone-api-fabric-1.7.3.jar) |
| 1.16.5 | v1.6.5 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.6.5/baritone-api-forge-1.6.5.jar) · [Fabric](https://github.com/cabaletta/baritone/releases/download/v1.6.5/baritone-api-fabric-1.6.5.jar) |
| 1.12.2 | v1.2.19 | [Forge](https://github.com/cabaletta/baritone/releases/download/v1.2.19/baritone-api-forge-1.2.19.jar) |


</details>

# API

Baritone is meant to be used as a library too. The API is heavily documented, you can find the Javadocs for the latest release [here](https://baritone.leijurv.com/) and all the settings [here](https://baritone.leijurv.com/baritone/api/Settings.html#field.detail).
Please note that usage of anything outside of the `baritone.api` package is not supported by the API release jar.

Here's how to change some settings and then path to an X/Z goal:

```java
BaritoneAPI.getSettings().allowSprint.value = true;
BaritoneAPI.getSettings().primaryTimeoutMS.value = 2000L;
BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(10000, 20000));
```

# FAQ

**Can I use Baritone as a library in my custom utility client?**
That's what it's for, sure! (As long as usage complies with the LGPL 3.0 License)

**How is it so fast?**
Magic. (Hours of [leijurv](https://github.com/leijurv/) enduring excruciating pain)

**Why is it called Baritone?**
It's named for FitMC's deep sultry voice.

**Where's the version for Minecraft 1.xx?**
See [the big purple box](#downloads).

# Special thanks

<a href="https://www.yourkit.com/"><img src="https://www.yourkit.com/images/yklogo.png" alt="YourKit"/></a>

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications.
YourKit is the creator of the [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).
We thank them for granting Baritone an OSS license so that we can make our software the best it can be.

# Trivia

Have committed at least once a day from Aug 1, 2018, to Aug 1, 2019.

1Leijurv3DWTrGAfmmiTphjhXLvQiHg7K2

<a href="https://www.star-history.com/#cabaletta/baritone&amp;Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=cabaletta/baritone&amp;type=Date&amp;theme=dark"/>
    <img alt="Stars over time" src="https://api.star-history.com/svg?repos=cabaletta/baritone&amp;type=Date"/>
  </picture>
</a>
