(Assumant que vous avez déja installé baritone [set up](SETUP.md))

# Prefix

Les commande de baritone peuvent être par défaut être écrite dans le chat. Cependent si vous faite une érreur d'écriture, par exemple écrire "gola 10000 10000" a la place de "goal 10000 10000" ce qui va être envoyer dans le chat publique, ce qui est pas bon.

Cependent vous pouvez utilisé un préfixe avant vos commande.

Baritone v1.1.0 et plus réçent: Le préfixe est `#` par défault. Tout commencent par `#` n'est pas envoyé, et est seulement interprété par baritone.
Pour les version inférieur a v1.1.0, `#` doit être activé dans les paramètre du préfixe.

**Seulement** dans Impact 4.4 `.b` est un préfixe valide. Dans la 4.4, `#` **ne fonctionne pas**, tout comme entrer les commande dirrectement dans le chat.

Les autre client comme Kami ou Asuna ont leur propre commande (comme `-path`), et ils peuvent désactivé totalement le contrôle de baritone dans le chat.



# Commands

**Tout** les commande ont besoin d'un préfixe, comme indiqué là haut.

`help` pour une liste des commande disponible. Vous pouver savoir ce que cette commande retourene [ici](https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/utils/ExampleBaritoneControl.java#L53).

Pour activé un paramètre booléan, dite le nom de la fonction booléenne parexemple, quand `allowBreak` est activé, quand Baritone considère le beroin de brisé des blocks. Pour un paramètre numérique, dite le nom du paramètre et ensuite dite la valeur (par exemple: `primaryTimeoutMS 250`). Ces paramètres sont sensible à la case.




Quelque exemple :
- `thisway 1000` puis ensuite `path` pour allé vers la dirrection que vous êtes en face
- `goal x y z` ou `goal x z` ou `goal y`, pour ensuite faire `path` pour allé vers une certaine position 
- `goal` pour faire un objectif basé sur votre position
- `goal clear` pour effacé l'objectif courant; 
- `cancel` ou `stop` pour tout arrêté
- `goto portal` ou `goto ender_chest` ou `goto block_type` pour aller vers un block. (dans Impact, `.goto` est un racourcis pour `.b goto`)
- `mine diamond_ore` to mine diamond ore(turn on the setting `legitMine` to only mine ores that it can actually see. It will explore randomly around y=11 until it finds them.)
- `follow playerName` to follow a player. `follow` to follow the entity you're looking at (only works if it hitting range). `followplayers` to follow any players in range (combine with Kill Aura for a fun time).
- `save waypointName` to save a waypoint. `goto waypointName` to go to it.
- `goto axis` to go to an axis or diagonal axis at y=120 (`axisHeight` is a configurable setting, defaults to 120).
- `invert` to invert the current goal and path. This gets as far away from it as possible, instead of as close as possible. For example, do `goal` then `invert` to run as far as possible from where you're standing at the start.
- `render` to rerender the world in case `renderCachedChunks` is being glitchy
- `damn` daniel

For the rest of the commands, you can take a look at the code [here](https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/utils/ExampleBaritoneControl.java).

All the settings and documentation are <a href="https://github.com/cabaletta/baritone/blob/master/src/api/java/baritone/api/Settings.java">here</a>. If you find HTML easier to read than Javadoc, you can look <a href="https://baritone.leijurv.com/">here</a> and navigate to Settings in the left sidebar.

There are about a hundred settings, but here are some fun / interesting / important ones that you might want to look at changing in normal usage of Baritone. The documentation for each can be found at the above links.
- `allowBreak`
- `allowSprint`
- `allowPlace`
- `allowParkour`
- `allowParkourPlace`
- `renderCachedChunks` (and `cachedChunksOpacity`) <-- very fun but you need a beefy computer
- `avoidance`
- `legitMine`
- `followRadius`



# Troubleshooting / common issues

## Baritone highlights a block in green but gets completely stuck? Also I'm using Baritone with Future?
Baritone is trying to right click to place a block there, but it can't since there's a conflicting mixin. Baritone can't force click right click when Future is also installed. Left click **does work** on recent Baritone even with Future, however. For now, turn off `allowPlace` and Baritone will only search for paths that don't require placing blocks to complete. `allowBreak` can remain on.

## Why doesn't Baritone respond to any of my chat commands?
This could be one of many things.

First, make sure it's actually installed. An easy way to check is seeing if it created the folder `baritone` in your Minecraft folder.

Second, make sure that you're using the prefix properly, and that chat control is enabled in the way you expect.

For example, Impact disables direct chat control. (i.e. anything typed in chat without a prefix will be ignored and sent publicly). **This is a saved setting**, so if you run Impact once, `chatControl` will be off from then on, **even in other clients**.
So you'll need to use the `#` prefix or edit `baritone/settings.txt` in your Minecraft folder to undo that (specifically, remove the line `chatControl false` then restart your client).


## Why can I do `.goto x z` in Impact but nowhere else? Why can I do `-path to x z` in KAMI but nowhere else?
These are custom commands that they added; those aren't from Baritone.
The equivalent you're looking for is `goal x z` then `path`.
