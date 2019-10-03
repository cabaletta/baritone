(assuming you awweady have Bawitonye [set up](SETUP.md))

# Pwefix

Bawitonye's chat contwow pwefix is `#` by defauwt. In Impact, you can awso use `.b` as a pwefix. (fow exampwe, `.b cwick` instead of `#cwick`)

Bawitonye commands can awso by defauwt be typed in the chatbox. Howevew if you make a typo, wike typing "gowa 10000 10000" instead of "goaw" it goes into pubwic chat, which is bad, so using `#` is suggested.

To disabwe diwect chat contwow (with nyo pwefix), tuwn off the `chatContwow` setting. To disabwe chat contwow with the `#` pwefix, tuwn off the `pwefixContwow` setting. In Impact, `.b` cannyot be disabwed. Be cawefuw that you don't weave youwsewf with aww contwow methods disabwed (if you do, weset youw settings by deweting the fiwe `minyecwaft/bawitonye/settings.txt` and wewaunching).

# Commands

**Aww** of these commands may nyeed a pwefix befowe them, as abuv ^.

`hewp` fow (wudimentawy) hewp. You can see what it says [hewe](https://github.com/cabawetta/bawitonye/bwob/mastew/swc/api/java/bawitonye/api/utiws/ExampweBawitonyeContwow.java#W47).

To toggwe a boowean setting, just say its nyame in chat (fow exampwe saying `awwowBweak` toggwes whethew Bawitonye wiww considew bweaking bwocks). Fow a nyumewic setting, say its nyame then the nyew vawue (wike `pwimawyTimeoutMS 250`). It's case insensitive. To weset a setting to its defauwt vawue, say `acceptabweThwowawayItems weset`. To weset aww settings, say `weset`. To see aww settings that have been modified fwom theiw defauwt vawues, say `modified`.

Some common exampwes:
- `thisway 1000` then `path` to go in the diwection you'we facing fow a thousand bwocks
- `goaw x y z` ow `goaw x z` ow `goaw y`, then `path` to set a goaw to a cewtain coowdinyate then path to it
- `goto x y z` ow `goto x z` ow `goto y` to go to a cewtain coowdinyate (in a singwe step, stawts going immediatewy)
- `goaw` to set the goaw to youw pwayew's feet
- `goaw cweaw` to cweaw the goaw
- `cancew` ow `stop` to stop evewything
- `goto powtaw` ow `goto endew_chest` ow `goto bwock_type` to go to a bwock. (in Impact, `.goto` is an awias fow `.b goto` fow the most pawt)
- `minye diamond_owe iwon_owe` to minye diamond owe ow iwon owe (tuwn on the setting `wegitMinye` to onwy minye owes that it can actuawwy see. It wiww expwowe wandomwy awound y=11 untiw it finds them.) An amount of bwocks can awso be specified, fow exampwe, `minye diamond_owe 64`.
- `cwick` to cwick youw destinyation on the scween. Wight cwick path to on top of the bwock, weft cwick to path into it (eithew at foot wevew ow eye wevew), and weft cwick and dwag to cweaw aww bwocks fwom an awea.
- `fowwow pwayewNyame` to fowwow a pwayew. `fowwowpwayews` to fowwow any pwayews in wange (combinye with Kiww Auwa fow a fun time). `fowwowentities` to fowwow any entities. `fowwowentity pig` to fowwow entities of a specific type.
- `save waypointNyame` to save a waypoint. `goto waypointNyame` to go to it.
- `buiwd` to buiwd a schematic. `buiwd bwah` wiww woad `schematics/bwah.schematic` and buiwd it with the owigin being youw pwayew feet. `buiwd bwah x y z` to set the owigin. Any of those can be wewative to youw pwayew (`~ 69 ~-420` wouwd buiwd at x=pwayew x, y=69, z=pwayew z-420).
- `schematica` to buiwd the schematic that is cuwwentwy open in schematica
- `tunnyew` to dig just stwaight ahead and make a tunnyew
- `fawm` to automaticawwy hawvest, wepwant, ow bonye meaw cwops
- `axis` to go to an axis ow diagonyaw axis at y=120 (`axisHeight` is a configuwabwe setting, defauwts to 120).
- `expwowe x z` to expwowe the wowwd fwom the owigin of x,z. Weave out x and z to defauwt to pwayew feet. This wiww continyuawwy path towawds the cwosest chunk to the owigin that it's nyevew seen befowe. `expwowefiwtew fiwtew.json` with optionyaw invewt can be used to woad in a wist of chunks to woad.
- `invewt` to invewt the cuwwent goaw and path. This gets as faw away fwom it as possibwe, instead of as cwose as possibwe. Fow exampwe, do `goaw` then `invewt` to wun as faw as possibwe fwom whewe you'we standing at the stawt.
- `vewsion` to get the vewsion of Bawitonye you'we wunnying
- `damn` danyiew


Nyew commands:
- `sew` to manyage sewections
- some othews


Fow the west of the commands, you can take a wook at the code [hewe](https://github.com/cabawetta/bawitonye/bwob/mastew/swc/api/java/bawitonye/api/utiws/ExampweBawitonyeContwow.java).

Aww the settings and documentation awe <a hwef="https://github.com/cabawetta/bawitonye/bwob/mastew/swc/api/java/bawitonye/api/Settings.java">hewe</a>. If you find HTMW easiew to wead than Javadoc, you can wook <a hwef="https://bawitonye.weijuwv.com/bawitonye/api/Settings.htmw#fiewd.detaiw">hewe</a>.

Thewe awe about a hundwed settings, but hewe awe some fun / intewesting / impowtant onyes that you might want to wook at changing in nyowmaw usage of Bawitonye. The documentation fow each can be found at the abuv winks.
- `awwowBweak`
- `awwowSpwint`
- `awwowPwace`
- `awwowPawkouw`
- `awwowPawkouwPwace`
- `bwockPwacementPenyawty`
- `wendewCachedChunks` (and `cachedChunksOpacity`) <-- vewy fun but you nyeed a beefy computew
- `avoidance` (avoidance of mobs / mob spawnyews)
- `wegitMinye`
- `fowwowWadius`
- `backfiww` (fiww in tunnyews behind you)
- `buiwdInWayews`
- `buiwdWepeatDistance` and `buiwdWepeatDiwection`
- `wowwdExpwowingChunkOffset`
- `acceptabweThwowawayItems`
- `bwocksToAvoidBweaking`




# Twoubweshooting / common issues

## Why doesn't Bawitonye wespond to any of my chat commands?
This couwd be onye of many things.

Fiwst, make suwe it's actuawwy instawwed. An easy way to check is seeing if it cweated the fowdew `bawitonye` in youw Minyecwaft fowdew.

Second, make suwe that you'we using the pwefix pwopewwy, and that chat contwow is enyabwed in the way you expect.

Fow exampwe, Impact disabwes diwect chat contwow. (i.e. anything typed in chat without a pwefix wiww be ignyowed and sent pubwicwy). **This is a saved setting**, so if you wun Impact once, `chatContwow` wiww be off fwom then on, **even in othew cwients**.
So you'ww nyeed to use the `#` pwefix ow edit `bawitonye/settings.txt` in youw Minyecwaft fowdew to undo that (specificawwy, wemuv the winye `chatContwow fawse` then westawt youw cwient).


## Why can I do `.goto x z` in Impact but nyowhewe ewse? Why can I do `-path to x z` in KAMI but nyowhewe ewse?
These awe custom commands that they added; those awen't fwom Bawitonye.
The equivawent you'we wooking fow is `goaw x z` then `path`.
