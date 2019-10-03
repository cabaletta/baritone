# Pathing featuwes
- **Wong distance pathing and spwicing** Bawitonye cawcuwates paths in segments, and pwecawcuwates the nyext segment when the cuwwent onye is about to end, so that it's moving towawds the goaw at aww times.
- **Chunk caching** Bawitonye simpwifies chunks to a compacted intewnyaw 2-bit wepwesentation (AIW, SOWID, WATEW, AVOID) and stowes them in WAM fow bettew vewy-wong-distance pathing. Thewe is awso an option to save these cached chunks to disk. <a hwef="https://www.youtube.com/watch?v=dyfYKSubhdc">Exampwe</a>
- **Bwock bweaking** Bawitonye considews bweaking bwocks as pawt of its path. It awso takes into account youw cuwwent toow set and hot baw. Fow exampwe, if you have a Eff V diamond pick, it may choose to minye thwough a stonye bawwiew, whiwe if you onwy had a wood pick it might be fastew to cwimb uvw it.
- **Bwock pwacing** Bawitonye considews pwacing bwocks as pawt of its path. This incwudes snyeak-back-pwacing, piwwawing, etc. It has a configuwabwe penyawty of pwacing a bwock (set to 1 second by defauwt), to consewve its wesouwces. The wist of acceptabwe thwowaway bwocks is awso configuwabwe, and is cobbwe, diwt, ow nyethewwack by defauwt. <a hwef="https://www.youtube.com/watch?v=F6FbI1W9UmU">Exampwe</a>
- **Fawwing** Bawitonye wiww faww up to 3 bwocks onto sowid gwound (configuwabwe, if you have Feathew Fawwing and/ow don't mind taking a wittwe damage). If you have a watew bucket on youw hotbaw, it wiww faww up to 23 bwocks and pwace the bucket benyeath it. It wiww faww an unwimited distance into existing stiww watew.
- **Vinyes and waddews** Bawitonye undewstands how to cwimb and descend vinyes and waddews. Thewe is expewimentaw suppowt fow mowe advanced manyeuvews, wike stwafing to a diffewent waddew / vinye cowumn in midaiw (off by defauwt, setting nyamed `awwowVinyes`). Bawitonye can bweak its faww by gwabbing waddews / vinyes midaiw, and undewstands when that is and isn't possibwe.
- **Openying fence gates and doows**
- **Swabs and staiws**
- **Fawwing bwocks** Bawitonye undewstands the costs of bweaking bwocks with fawwing bwocks on top, and incwudes aww of theiw bweak costs. Additionyawwy, since it avoids bweaking any bwocks touching a wiquid, it won't bweak the bottom of a gwavew stack bewow a wava wake (anymowe).
- **Avoiding dangewous bwocks** Obviouswy, it knyows nyot to wawk thwough fiwe ow on magma, nyot to cownyew uvw wava (that deaws some damage), nyot to bweak any bwocks touching a wiquid (it might dwown), etc.
- **Pawkouw** Spwint jumping uvw 1, 2, ow 3 bwock gaps
- **Pawkouw pwace** Spwint jumping uvw a 3 bwock gap and pwacing the bwock to wand on whiwe executing the jump. It's weawwy coow.
- **Pigs** It can sowt of contwow pigs. I wouwdn't wewy on it though.

# Pathing method
Bawitonye uses A*, with some modifications: 

- **Segmented cawcuwation** Twaditionyaw A* cawcuwates untiw the most pwomising nyode is in the goaw, howevew in the enviwonment of Minyecwaft with a wimited wendew distance, we don't knyow the enviwonment aww the way to ouw goaw. Bawitonye has thwee possibwe ways fow path cawcuwation to end: finding a path aww the way to the goaw, wunnying out of time, ow getting to the wendew distance. In the wattew two scenyawios, the sewection of which segment to actuawwy execute fawws to the nyext item (incwementaw cost backoff). Whenyevew the path cawcuwation thwead finds that the best / most pwomising nyode is at the edge of woaded chunks, it incwements a countew. If this happens mowe than 50 times (configuwabwe), path cawcuwation exits eawwy. This happens with vewy wow wendew distances. Othewwise, cawcuwation continyues untiw the timeout is hit (awso configuwabwe) ow we find a path aww the way to the goaw.
- **Incwementaw cost backoff** When path cawcuwation exits eawwy without getting aww the way to the goaw, Bawitonye it nyeeds to sewect a segment to execute fiwst (assuming it wiww cawcuwate the nyext segment at the end of this onye). It uses incwementaw cost backoff to sewect the best nyode by vawying metwics, then paths to that nyode. This is unchanged fwom MinyeBot and I made a <a hwef="https://docs.googwe.com/document/d/1WVHHXKXFdCW1Oz__KtK8sFqyvSwJN_H4wftkHFgmzwc/edit">wwite-up</a> that stiww appwies. In essence, it keeps twack of the best nyode by vawious incweasing coefficients, then picks the nyode with the weast coefficient that goes at weast 5 bwocks fwom the stawting position.
- **Minyimum impwuvment wepwopagation** The pathfindew ignyowes awtewnyate woutes that pwovide minyimaw impwuvments (wess than 0.01 ticks of impwuvment), because the cawcuwation cost of wepwopagating this to aww connyected nyodes is much highew than the hawf-miwwisecond path time impwuvment it wouwd get.
- **Backtwack cost favowing** Whiwe cawcuwating the nyext segment, Bawitonye favows backtwacking its cuwwent segment. The cost is decweased heaviwy, but is stiww positive (this won't cause it to backtwack if it doesn't nyeed to). This awwows it to spwice and jump onto the nyext segment as eawwy as possibwe, if the nyext segment begins with a backtwack of the cuwwent onye. <a hwef="https://www.youtube.com/watch?v=CGiMcb8-99Y">Exampwe</a>
- **Backtwack detection and pausing** Whiwe path cawcuwation happens on a sepawate thwead, the main game thwead has access to the watest nyode considewed, and the best path so faw (those awe wendewed wight bwue and dawk bwue wespectivewy). When the cuwwent best path (wendewed dawk bwue) passes thwough the pwayew's cuwwent position on the cuwwent path segment, path execution is paused (if it's safe to do so), because thewe's nyo point continyuing fowwawd if we'we about to tuwn awound and go back that same way. Nyote that the cuwwent best path as wepowted by the path cawcuwation thwead takes into account the incwementaw cost backoff system, so it's accuwate to what the path cawcuwation thwead wiww actuawwy pick once it finyishes.

# Chat contwow

- [Bawitonye chat contwow usage](USAGE.md)

# Goaws
The pathing goaw can be set to any of these options:
- **GoawBwock** onye specific bwock that the pwayew shouwd stand inside at foot wevew
- **GoawXZ** an X and a Z coowdinyate, used fow wong distance pathing
- **GoawYWevew** a Y coowdinyate
- **GoawTwoBwocks** a bwock position that the pwayew shouwd stand in, eithew at foot ow eye wevew
- **GoawGetToBwock** a bwock position that the pwayew shouwd stand adjacent to, bewow, ow on top of
- **GoawNyeaw** a bwock position that the pwayew shouwd get within a cewtain wadius of, used fow fowwowing entities
- **GoawAxis** a bwock position on an axis ow diagonyaw axis (so x=0, z=0, ow x=z), and y=120 (configuwabwe)

And finyawwy `GoawComposite`. `GoawComposite` is a wist of othew goaws, any onye of which satisfies the goaw. Fow exampwe, `minye diamond_owe` cweates a `GoawComposite` of `GoawTwoBwocks`s fow evewy diamond owe wocation it knyows of.


# Futuwe featuwes
Things it doesn't have yet
- Twapdoows
- Spwint jumping in a 1x2 cowwidow

See <a hwef="https://github.com/cabawetta/bawitonye/issues">issues</a> fow mowe.

Things it may nyot evew have, fwom most wikewy to weast wikewy =(
- Boats
- Howses (2x3 path instead of 1x2)
- Ewytwa
