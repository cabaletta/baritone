***

<center> <h1>Baritone</h1> </center>

***

<p align="center">
  <a href="https://github.com/cabaletta/baritone/releases/"><img src="https://img.shields.io/github/downloads/cabaletta/baritone/total.svg" alt="GitHub All Releases"/></a>
</p>

<p align="center">
<a href="https://github.com/cabaletta/baritone/releases/"><img src="https://img.shields.io/github/release/cabaletta/baritone.svg" alt="Release"/></a>
</p>

#### Baritone is a Minecraft [pathfinding](https://en.wikipedia.org/wiki/Pathfinding) bot.

#### Baritone currently supports Minecraft versions 1.12.2 - 1.16.5

***

Baritone has been used in popular utility clients such as [Impact Client](https://impactclient.net/),
[Meteor Client](https://meteorclient.com/), and [Aristois](https://aristois.net/).

#### You can install Baritone from the following locations:

- [This repository](https://github.com/cabaletta/baritone/releases/)
- [Impact Client](https://impactclient.net/)
- [Meteor Client](https://aristois.net/)
- [Aristois]()
- Note: Many utility clients offer Baritone, but sometimes utility clients can come with **malware**,
use your best judgement and be careful of what you download.

Installing a utility client with Baritone support is an easy way to get started with Baritone
as most utility clients include a GUI to navigate Baritone's commands.

The safest way to install Baritone is to install a release .jar from this repository 
and place it in your .minecraft\mods\ folder.

The most recent version of Minecraft that Baritone currently supports is *1.16.5* ,
Baritone will be updated to 1.17.~ soon.

***

## Getting Started

***

- #### [Features](FEATURES.md)

- #### [Installation & setup](SETUP.md)

- #### [API Javadocs](https://baritone.leijurv.com/)

- #### [Settings](https://baritone.leijurv.com/baritone/api/Settings.html#field.detail)

- #### [Usage (chat control)](USAGE.md)

***

## Baritone API

***

There is extensive documentation for the Baritone API,
[JavaDocs](https://baritone.leijurv.com/).
Please note that usage of anything located outside the ``baritone.api`` package is unsupported by the API release jar.

Below is an example of changing settings, and then pathing to an X/Z goal.

```
BaritoneAPI.getSettings().allowSprint.value = true;
BaritoneAPI.getSettings().primaryTimeoutMS.value = 2000L;

BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(10000, 20000));
```
***

## FAQ

***

*Can I use Baritone in my utility client?*

Absolutely, as long as your usage is compliant
with Baritone's [LGPLv3 license](https://github.com/cabaletta/baritone/blob/master/LICENSE).

More information about the LGPLv3 license can be found [here](https://www.gnu.org/licenses/lgpl-3.0.en.html).

***

*When will support for Minecraft 1.17.~ be released?*

Join the [Baritone Discord Server](https://discord.com/invite/s6fRBAUpmr) to be notified of future releases.

***

*Is using Baritone cheating?*

No, Baritone is a utility mod intended to automate certain game mechanics.

Do **NOT** use Baritone on public servers which explicitly ban utility clients and other mods which may give
you an unfair advantage over other players.

***

*How do I use Baritone?*

Read [Usage.md](USAGE.md) or 
watch [Impact Client's YouTube playlist.](https://www.youtube.com/playlist?list=PLnwnJ1qsS7CoQl9Si-RTluuzCo_4Oulpa)

***

*How does Baritone work and how is it so fast?*

~~Read the source code / documentation~~. 

**Magic** (Over 2 years of [leijurv](https://github.com/leijurv/) enduring excruciating pain)

***

*Why is it called "Baritone?"*

Read: [Baritone](https://en.wikipedia.org/wiki/Baritone)

Named for [FitMC's](https://www.youtube.com/user/SonOfShoop) deep, sultry voice.

***
