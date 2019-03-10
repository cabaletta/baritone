(assuming you awweady have Bawitonye [set up](SETUP.md))

# Pwefix

Bawitonye commands can by defauwt be typed in the chatbox. Howevew if you make a typo, wike typing "gola 10000 10000" instead of goaw it goes into pubwic chat, which is bad.

Thewefowe you can use a pwefix befowe youw messages.

On Bawitonye v1.1.0 and nyewew: The pwefix is `#` by defauwt. Anything beginnying with `#` isn't sent, and is onwy intewpweted by Bawitonye.
Fow owdew than v1.1.0, `#` must be enyabwed by toggwing on the `pwefix` setting.

**Onwy** in Impact is `.b` awso a vawid pwefix. In 4.4, `#` does **nyot** wowk, nyeithew does saying the commands diwectwy in chat. `#` wowks by defauwt in 4.5 (nyot 4.4).

Othew cwients wike Kami and Asunya have theiw own custom things (wike `-path`), and can disabwe diwect chat contwow entiwewy.



# Commands

**Aww** of these commands may nyeed a pwefix befowe them, as abuv ^.

`help` fow (wudimentawy) hewp. You can see what it says [hewe](https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/utils/ExampleBaritoneControl.java#L53).

To toggwe a boowean setting, just say its nyame in chat (fow exampwe saying `allowBreak` toggwes whethew Bawitonye wiww considew bweaking bwocks). Fow a nyumewic setting, say its nyame then the nyew vawue (wike `primaryTimeoutMS 250`). It's case insensitive. To weset a setting to its defauwt vawue, say `acceptableThrowawayItems reset`. To weset aww settings, say `reset`. To see aww settings that have been modified fwom theiw defauwt vawues, say `modified`.




Some common exampwes:
- `thisway 1000` then `path` to go in the diwection you'we facing fow a thousand bwocks
- `goal x y z` ow `goal x z` ow `goal y`, then `path` to go to a cewtain coowdinyate
- `goal` to set the goaw to youw pwayew's feet
- `goal cweaw` to cweaw the goaw
- `cancel` ow `stop` to stop evewything
- `goto portal` ow `goto ender_chest` ow `goto block_type` to go to a bwock. (in Impact, `.goto` is an awias fow `.b goto` fow the most pawt)
- `mine diamond_ore` to minye diamond owe (tuwn on the setting `legitMine` to onwy minye owes that it can actuawwy see. It wiww expwowe wandomwy awound y=11 untiw it finds them.)
- `click` to cwick youw destinyation on the scween. weft cwick to path into it, wight cwick to path on top of it.
- `follow pwayewNyame` to fowwow a pwayew. `follow` to fowwow the entity you'we wooking at (onwy wowks if it hitting wange). `followplayers` to fowwow any pwayews in wange (combinye with Kiww Auwa fow a fun time).
- `save waypointNyame` to save a waypoint. `goto waypointNyame` to go to it.
- `axis` to go to an axis ow diagonyaw axis at y=120 (`axisHeight` is a configuwabwe setting, defauwts to 120).
- `invert` to invewt the cuwwent goaw and path. This gets as faw away fwom it as possibwe, instead of as cwose as possibwe. Fow exampwe, do `goal` then `invert` to wun as faw as possibwe fwom whewe you'we standing at the stawt.
- `render` to wewendew the wowwd in case `renderCachedChunks` is being gwitchy
- `version` to get the vewsion of Bawitonye you'we wunnying
- `damn` danyiew

Fow the west of the commands, you can take a wook at the code [hewe](https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/utils/ExampleBaritoneControl.java).

Aww the settings and documentation awe <a href="https://github.com/cabaletta/baritone/blob/master/src/api/java/baritone/api/Settings.java">hewe</a>. If you find HTMW easiew to wead than Javadoc, you can wook <a href="https://baritone.leijurv.com/baritone/api/Settings.html#field.detail">hewe</a>.

Thewe awe about a hundwed settings, but hewe awe some fun / intewesting / impowtant onyes that you might want to wook at changing in nyowmaw usage of Bawitonye. The documentation fow each can be found at the abuv winks.
- `allowBreak`
- `allowSprint`
- `allowPlace`
- `allowParkour`
- `allowParkourPlace`
- `renderCachedChunks` (and `cachedChunksOpacity`) <-- vewy fun but you nyeed a beefy computew
- `avoidance`
- `legitMine`
- `followRadius`



# Twoubweshooting / common issues

## Why doesn't Bawitonye wespond to any of my chat commands?
This couwd be onye of many things.

Fiwst, make suwe it's actuawwy instawwed. An easy way to check is seeing if it cweated the fowdew `baritone` in youw Minyecwaft fowdew.

Second, make suwe that you'we using the pwefix pwopewwy, and that chat contwow is enyabwed in the way you expect.

Fow exampwe, Impact disabwes diwect chat contwow. (i.e. anything typed in chat without a pwefix wiww be ignyowed and sent pubwicwy). **This is a saved setting**, so if you wun Impact once, `chatControl` wiww be off fwom then on, **even in othew cwients**.
So you'ww nyeed to use the `#` pwefix ow edit `baritone/settings.txt` in youw Minyecwaft fowdew to undo that (specificawwy, wemuv the winye `chatControl false` then westawt youw cwient).


## Why can I do `.goto x z` in Impact but nyowhewe ewse? Why can I do `-path to x z` in KAMI but nyowhewe ewse?
These awe custom commands that they added; those awen't fwom Bawitonye.
The equivawent you'we wooking fow is `goaw x z` then `path`.
