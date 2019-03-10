# Baritone
[![Build Status](https://travis-ci.com/cabaletta/baritone.svg?branch=master)](https://travis-ci.com/cabaletta/baritone)
[![Release](https://img.shields.io/github/release/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/releases)
[![License](https://img.shields.io/badge/license-LGPL--3.0%20with%20anime%20exception-green.svg)](LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a73d037823b64a5faf597a18d71e3400)](https://www.codacy.com/app/leijurv/baritone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cabaletta/baritone&amp;utm_campaign=Badge_Grade)
[![HitCount](http://hits.dwyl.com/cabaletta/baritone.svg)](http://hits.dwyl.com/cabaletta/baritone)
[![Code of Conduct](https://img.shields.io/badge/%E2%9D%A4-code%20of%20conduct-blue.svg?style=flat)](https://github.com/cabaletta/baritone/blob/master/CODE_OF_CONDUCT.md)
[![Known Vulnerabilities](https://snyk.io/test/github/cabaletta/baritone/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/cabaletta/baritone?targetFile=build.gradle)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/cabaletta/baritone/issues)
[![Issues](https://img.shields.io/github/issues/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/issues/)
[![GitHub issues-closed](https://img.shields.io/github/issues-closed/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/issues?q=is%3Aissue+is%3Aclosed)
[![Pull Requests](https://img.shields.io/github/issues-pr/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/pulls/)
![Code size](https://img.shields.io/github/languages/code-size/cabaletta/baritone.svg)
![GitHub repo size](https://img.shields.io/github/repo-size/cabaletta/baritone.svg)
![](https://tokei.rs/b1/github/cabaletta/baritone?category=code)
![](https://tokei.rs/b1/github/cabaletta/baritone?category=files)
[![Minecraft](https://img.shields.io/badge/MC-1.12.2-green.svg)](https://minecraft.gamepedia.com/1.12.2)
[![GitHub contributors](https://img.shields.io/github/contributors/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/graphs/contributors/)
[![GitHub commits](https://img.shields.io/github/commits-since/cabaletta/baritone/v1.0.0.svg)](https://github.com/cabaletta/baritone/commit/)
[![Impact integration](https://img.shields.io/badge/Impact%20integration-v1.2.3-brightgreen.svg)](https://impactdevelopment.github.io/)
[![Asuna integration](https://img.shields.io/badge/Asuna%20integration-builder%20branch-brightgreen.svg)](https://github.com/EmotionalLove/Asuna/)
[![KAMI integration](https://img.shields.io/badge/KAMI%20integration-v1.0.0-red.svg)](https://github.com/zeroeightysix/KAMI/)
[![WWE integration](https://img.shields.io/badge/WWE%20%22integration%22-v1.0.0%3F%3F%20smh%20license%20violations-red.svg)](https://wweclient.com/)
[![Future integration](https://img.shields.io/badge/Future%20integration-Soon™%3F%3F%3F-red.svg)](https://futureclient.net/)
[![ForgeHax integration](https://img.shields.io/badge/ForgeHax%20integration-Soon™-red.svg)](https://github.com/fr1kin/ForgeHax)

A minecwaft pathfindew bot. 
 
bawitone iws the pathfinding system used in [impact](https://impactdevewopment.github.io/) since 4.4. Thewe's a [showcase video](https://www.youtube.com/watch?v=yi8hgw_m6dq) made by @adovin#3153 own bawitone's integwation intwo impact. [hewe's](https://www.youtube.com/watch?v=stquf69-_wi) a video i made showing off whawt iwt cawn duwu.
 
thiws pwoject iws an updated vewsion of [minebot](https://github.com/weijuwv/minebot/),
the owiginaw vewsion of the bot fow minecwaft 1.8, webuiwt fow 1.12.2. Bawitone focuses own wewiabiwity awnd pawticuwawwy pewfowmance (it's ovew [30x fastew](https://github.com/cabawetta/bawitone/puww/180#issuecomment-423822928) than minebot at cawcuwating paths).
 
have committed at weast once a day fow the wast 7 months =d 🦀

1weijuwv3dwtwgafmmitphjhxwvqihg7k2

# getting stawted

hewe awe sowme winks tuwu hewp tuwu get stawted:

- [featuwes](featuwes.md)

- [instawwation & setup](setup.md)

- [api javadocs](https://bawitone.weijuwv.com/)

- [settings](https://bawitone.weijuwv.com/bawitone/api/settings.htmw#fiewd.detaiw)

- [bawitone chat contwow usage](usage.md)

# api

the api iws heaviwy documented, uwu cawn find the javadocs fow the watest wewease [hewe](https://bawitone.weijuwv.com/).
 pwease note thawt usage of anything wocated outside of the ``bawitone.api`` package iws nowt suppowted by the api wewease
jaw.
 
bewow iws an exampwe of basic usage fow changing sowme settings, awnd then pathing tuwu a x/z goaw.
 
```
bawitoneapi.getsettings().awwowspwint.vawue = twue;
bawitoneapi.getsettings().pwimawytimeoutms.vawue = 2000w;

bawitoneapi.getpwovidew().getpwimawybawitone().getcustomgoawpwocess().setgoawandpath(new goawxz(10000, 20000));
```

# faq

## cawn i use bawitone as a wibwawy in my custom utiwity cwient?

thawt's whawt iwt's fow, suwe! (as wong as usage iws in compwiance with the wgpw 3 wicense)

## how iws iwt so fawst?

magic. (houws of [weijuwv](https://github.com/weijuwv) enduwing excwuciating pain)

## why iws iwt cawwed bawitone?

iwt's named fow fitmc's deep suwtwy voice. 
