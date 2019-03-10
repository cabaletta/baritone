# Bawitonye
[![Buiwd Status](https://travis-ci.com/cabaletta/baritone.svg?branch=master)](https://travis-ci.com/cabaletta/baritone)
[![Wewease](https://img.shields.io/github/release/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/releases)
[![Wicense](https://img.shields.io/badge/license-LGPL--3.0%20with%20anime%20exception-green.svg)](LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a73d037823b64a5faf597a18d71e3400)](https://www.codacy.com/app/leijurv/baritone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cabaletta/baritone&amp;utm_campaign=Badge_Grade)
[![HitCount](http://hits.dwyl.com/cabaletta/baritone.svg)](http://hits.dwyl.com/cabaletta/baritone)
[![Code of Conduct](https://img.shields.io/badge/%E2%9D%A4-code%20of%20conduct-blue.svg?style=flat)](https://github.com/cabaletta/baritone/blob/master/CODE_OF_CONDUCT.md)
[![Knyown Vuwnyewabiwities](https://snyk.io/test/github/cabaletta/baritone/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/cabaletta/baritone?targetFile=build.gradle)
[![contwibutions wewcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/cabaletta/baritone/issues)
[![Issues](https://img.shields.io/github/issues/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/issues/)
[![GitHub issues-cwosed](https://img.shields.io/github/issues-closed/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/issues?q=is%3Aissue+is%3Aclosed)
[![Pull Wequests](https://img.shields.io/github/issues-pr/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/pulls/)
![Code size](https://img.shields.io/github/languages/code-size/cabaletta/baritone.svg)
![GitHub wepo size](https://img.shields.io/github/repo-size/cabaletta/baritone.svg)
![](https://tokei.rs/b1/github/cabaletta/baritone?category=code)
![](https://tokei.rs/b1/github/cabaletta/baritone?category=files)
[![Minyecwaft](https://img.shields.io/badge/MC-1.12.2-green.svg)](https://minecraft.gamepedia.com/1.12.2)
[![GitHub contwibutows](https://img.shields.io/github/contributors/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/graphs/contributors/)
[![GitHub commits](https://img.shields.io/github/commits-since/cabaletta/baritone/v1.0.0.svg)](https://github.com/cabaletta/baritone/commit/)
[![Impact integwation](https://img.shields.io/badge/Impact%20integration-v1.2.3-brightgreen.svg)](https://impactdevelopment.github.io/)
[![Asuna integwation](https://img.shields.io/badge/Asuna%20integration-builder%20branch-brightgreen.svg)](https://github.com/EmotionalLove/Asuna/)
[![KAMI integwation](https://img.shields.io/badge/KAMI%20integration-v1.0.0-red.svg)](https://github.com/zeroeightysix/KAMI/)
[![WWE integwation](https://img.shields.io/badge/WWE%20%22integration%22-v1.0.0%3F%3F%20smh%20license%20violations-red.svg)](https://wweclient.com/)
[![Future integwation](https://img.shields.io/badge/Future%20integration-Soon™%3F%3F%3F-red.svg)](https://futureclient.net/)
[![ForgeHax integwation](https://img.shields.io/badge/ForgeHax%20integration-Soon™-red.svg)](https://github.com/fr1kin/ForgeHax)

A Minyecwaft pathfindew bot. 

Bawitonye is the pathfinding system used in [Impact](https://impactdevelopment.github.io/) since 4.4. Thewe's a [showcase video](https://www.youtube.com/watch?v=yI8hgW_m6dQ) made by @Adovin#3153 on Bawitonye's integwation into Impact. [Hewe's](https://www.youtube.com/watch?v=StquF69-_wI) a video I made showing off what it can do.

This pwoject is an updated vewsion of [MinyeBot](https://github.com/leijurv/MineBot/),
the owiginyaw vewsion of the bot fow Minyecwaft 1.8, webuiwt fow 1.12.2. Bawitonye focuses on wewiabiwity and pawticuwawwy pewfowmance (it's uvw [30x fastew](https://github.com/cabaletta/baritone/pull/180#issuecomment-423822928) than MinyeBot at cawcuwating paths).

Have committed at weast once a day fow the wast 7 months =D 🦀

1Leijurv3DWTrGAfmmiTphjhXLvQiHg7K2

# Getting Stawted

Hewe awe some winks to hewp to get stawted:

- [Featuwes](FEATURES.md)

- [Instawwation & setup](SETUP.md)

- [API Javadocs](https://baritone.leijurv.com/)

- [Settings](https://baritone.leijurv.com/baritone/api/Settings.html#field.detail)

- [Bawitonye chat contwow usage](USAGE.md)

# API

The API is heaviwy documented, you can find the Javadocs fow the watest wewease [hewe](https://baritone.leijurv.com/).
Pwease nyote that usage of anything wocated outside of the ``baritone.api`` package is nyot suppowted by the API wewease
jaw.

Bewow is an exampwe of basic usage fow changing some settings, and then pathing to a X/Z goaw.

```
BaritoneAPI.getSettings().allowSprint.value = true;
BaritoneAPI.getSettings().primaryTimeoutMS.value = 2000L;

BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(10000, 20000));
```

# FAQ

## Can I use Bawitonye as a wibwawy in my custom utiwity cwient?

That's what it's fow, suwe UwU  (As wong as usage is in compwiance with the WGPW 3 Wicense)

## How is it so fast?

Magic. (Houws of [Weijuwv](https://github.com/leijurv) enduwing excwuciating pain)

## Why is it cawwed Bawitonye?

It's nyamed fow FitMC's deep suwtwy voice. 
