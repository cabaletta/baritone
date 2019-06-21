# Baritone
[![Build Status](https://travis-ci.com/cabaletta/baritone.svg?branch=master)](https://travis-ci.com/cabaletta/baritone)
[![Release](https://img.shields.io/github/release/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/releases)
[![License](https://img.shields.io/badge/license-LGPL--3.0%20with%20anime%20exception-green.svg)](LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a73d037823b64a5faf597a18d71e3400)](https://www.codacy.com/app/leijurv/baritone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cabaletta/baritone&amp;utm_campaign=Badge_Grade)
[![HitCount](http://hits.dwyl.com/cabaletta/baritone.svg)](http://hits.dwyl.com/cabaletta/baritone)
[![GitHub All Releases](https://img.shields.io/github/downloads/cabaletta/baritone/total.svg)](https://github.com/cabaletta/baritone/releases)
[![Minecraft](https://img.shields.io/badge/MC-1.12.2-green.svg)](https://github.com/cabaletta/baritone/tree/master/)
[![Minecraft](https://img.shields.io/badge/MC-1.13.2-brightgreen.svg)](https://github.com/cabaletta/baritone/tree/1.13.2/)
[![Code of Conduct](https://img.shields.io/badge/%E2%9D%A4-code%20of%20conduct-blue.svg?style=flat)](https://github.com/cabaletta/baritone/blob/master/CODE_OF_CONDUCT.md)
[![Known Vulnerabilities](https://snyk.io/test/github/cabaletta/baritone/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/cabaletta/baritone?targetFile=build.gradle)
[![Contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/cabaletta/baritone/issues)
[![Issues](https://img.shields.io/github/issues/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/issues/)
[![GitHub issues-closed](https://img.shields.io/github/issues-closed/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/issues?q=is%3Aissue+is%3Aclosed)
[![Pull Requests](https://img.shields.io/github/issues-pr/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/pulls/)
![Code size](https://img.shields.io/github/languages/code-size/cabaletta/baritone.svg)
![GitHub repo size](https://img.shields.io/github/repo-size/cabaletta/baritone.svg)
![Lines of Code](https://tokei.rs/b1/github/cabaletta/baritone?category=code)
[![GitHub contributors](https://img.shields.io/github/contributors/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/graphs/contributors/)
[![GitHub commits](https://img.shields.io/github/commits-since/cabaletta/baritone/v1.0.0.svg)](https://github.com/cabaletta/baritone/commit/)
[![Impact integration](https://img.shields.io/badge/Impact%20integration-v1.2.6%20/%20v1.3.2-brightgreen.svg)](https://impactdevelopment.github.io/)
[![ForgeHax integration](https://img.shields.io/badge/ForgeHax%20%22integration%22-scuffed-yellow.svg)](https://github.com/fr1kin/ForgeHax/)
[![WWE integration](https://img.shields.io/badge/WWE%20%22integration%22-master%3F-green.svg)](https://wweclient.com/)
[![Future integration](https://img.shields.io/badge/Future%20integration-Soon™%3F%3F%3F-red.svg)](https://futureclient.net/)
[![forthebadge](https://forthebadge.com/images/badges/built-with-swag.svg)](http://forthebadge.com/)
[![forthebadge](https://forthebadge.com/images/badges/mom-made-pizza-rolls.svg)](http://forthebadge.com/)

A Minecraft pathfinder bot. 

Baritone is the pathfinding system used in [Impact](https://impactdevelopment.github.io/) since 4.4. There's a [showcase video](https://www.youtube.com/watch?v=yI8hgW_m6dQ) made by @Adovin#3153 on Baritone's integration into Impact. [Here's](https://www.youtube.com/watch?v=StquF69-_wI) a video I made showing off what it can do.

This project is an updated version of [MineBot](https://github.com/leijurv/MineBot/),
the original version of the bot for Minecraft 1.8.9, rebuilt for 1.12.2 and 1.13.2. Baritone focuses on reliability and particularly performance (it's over [30x faster](https://github.com/cabaletta/baritone/pull/180#issuecomment-423822928) than MineBot at calculating paths).

Have committed at least once a day for the last 10 months =D 🦀

1Leijurv3DWTrGAfmmiTphjhXLvQiHg7K2

# Getting Started

Here are some links to help to get started:

- [Features](FEATURES.md)

- [Installation & setup](SETUP.md)

- [API Javadocs](https://baritone.leijurv.com)

- [Settings](https://baritone.leijurv.com/baritone/api/Settings.html#field.detail)

- [Baritone chat control usage](USAGE.md)

# API

The API is heavily documented, you can find the Javadocs for the latest release [here](https://baritone.leijurv.com/).
Please note that usage of anything located outside of the ``baritone.api`` package is not supported by the API release
jar.

Below is an example of basic usage for changing some settings, and then pathing to a X/Z goal.

```
BaritoneAPI.getSettings().allowSprint.value = true;
BaritoneAPI.getSettings().primaryTimeoutMS.value = 2000L;

BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(10000, 20000));
```

# FAQ

## Can I use Baritone as a library in my custom utility client?

That's what it's for, sure! (As long as usage is in compliance with the LGPL 3.0 License)

## How is it so fast?

Magic. (Hours of [leijurv](https://github.com/leijurv) enduring excruciating pain)

## Why is it called Baritone?

It's named for FitMC's deep sultry voice. 
