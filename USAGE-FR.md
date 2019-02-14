(En assumant que vous avez déja installé baritone [set up](SETUP.md))

# Préfix

Les commande de baritone peuvent être par défaut être écrite dans le chat. Cependent si vous faite une érreur d'écriture, par exemple écrire "gola 10000 10000" a la place de "goal 10000 10000" ce qui va être envoyer dans le chat publique, ce qui est pas bon.

Cependent vous pouvez utilisé un préfixe avant vos commande.

Baritone v1.1.0 et plus réçent: Le préfixe est `#` par défault. Tout commencent par `#` n'est pas envoyé, et est seulement interprété par baritone.
Pour les version inférieur a v1.1.0, `#` doit être activé dans les paramètre du préfixe.

**Seulement** dans Impact 4.4 `.b` est un préfixe valide. Dans la 4.4, `#` **ne fonctionne pas**, tout comme entrer les commande dirrectement dans le chat.

Les autre client comme Kami ou Asuna ont leur propre commande (comme `-path`), et ils peuvent désactivé totalement le contrôle de baritone dans le chat.



# Commande

**Toutes** les commande ont besoin d'un préfixe, comme indiqué là haut.

`help` pour une liste des commande disponible. Vous pouver savoir ce que cette commande retourene [ici](https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/utils/ExampleBaritoneControl.java#L53).

Pour activé un paramètre booléan, dite le nom de la fonction booléenne parexemple, quand `allowBreak` est activé, quand Baritone considère le beroin de brisé des blocks. Pour un paramètre numérique, dite le nom du paramètre et ensuite dite la valeur (par exemple: `primaryTimeoutMS 250`). Ces paramètres sont sensible à la case.



Quelque exemple :
- `thisway 1000` puis ensuite `path` pour allé vers la dirrection que vous êtes en face `goal x y z` ou `goal x z` ou `goal y`, pour ensuite faire `path` pour allé vers une certaine position 
- `goal` pour faire un objectif basé sur votre position
- `goal clear` pour effacé l'objectif courant; 
- `cancel` ou `stop` pour tout arrêté
- `goto portal` ou `goto ender_chest` ou `goto block_type` pour aller vers un block. (dans Impact, `.goto` est un racourcis pour `.b goto`)
- `mine diamond_ore` pour miner du diamant, activé le paramètre`legitMine` pour miner les minerais que baritone voit. `legitmine` va exploré aléatoirement jusqu'a environ Y=11 pour trouvé le diamant.
- `follow playerName` pour suivre un joueur. `follow` pour suivre l'entité que vous regardé(Cela fonctionne que lorsce que vous êtes proche de la hitbox de l'entité. `followplayers` pour suivre tout les joueur proche de votre personnage(Combiné avec Kill Aura pour avoir un moment de plaisir).
- `save waypointName` pour sauvegardé un marqueur. Pour allée vers la dirrection du marqueur`goto waypointName`.
- `goto axis` permet d'allé vers un axe ou vers un axe dialgonale à y=120 (`axisHeight` permet de regler la hauteur auquelle votre personnage se déplacera).
- `invert` permet d'inversé le but actuelle. This gets as far away from it as possible, instead of as close as possible. For example, do `goal` then `invert` to run as far as possible from where you're standing at the start.
- `render` permet de rechargé le système de chunk. `renderCachedChunks` est un paramètre qui permet de chargé les chunk mis en cache.
- `damn` daniel

Pour le reste des commande, vous pouvez voir le code [ici](https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/utils/ExampleBaritoneControl.java).

Toute les paramètre de la documentation sont <a href="https://github.com/cabaletta/baritone/blob/master/src/api/java/baritone/api/Settings.java">ici</a>. Si vous trouver le HTML plus facile à lire que le Javadoc, il y a une documentation web <a href="https://baritone.leijurv.com/">ici</a>. À partir du site web allé dans la barre gauche et cliquer sur settings. 

Il y a une centaine de paramètre, voici quelque paramètre. Certaint sont juste distrayant, d'autre utile et finalement certain peut être une quelque chose de bien a conaitre pour changé le fonctionement de baritone. La documentation des 
commande sur chacun des paramètre est indiqué en haut.
- `allowBreak`
- `allowSprint`
- `allowPlace`
- `allowParkour`
- `allowParkourPlace`
- `renderCachedChunks` (et `cachedChunksOpacity`) <-- vraiment amusant mais vous avez besoin d'un ordi puissant.)
- `avoidance`
- `legitMine`
- `followRadius`



# Résolution des problème /problème fréquant

## Baritone met en évidence un block en vert mais reste bloqué? J'utilise aussi baritone avec future client.
Baritone esssaie de faire un clique droit pour placer un bloque, il ne peut pas puisqu'il y a un conflit entre Baritone et Future. Baritone ne peut pas forcer le clique gauche lorse que Future est installé. Le clique gauche **Ne fonctione pas** sur les version récente de baritone avec Future. Pour le moment désactiver `allowPlace` cependent vous pouvez activé `allowBreak`.

## Pourquois Baritone ne répond pas à aucun de mes commandes?
Cette érreur peut être la source de plusieurs problème.


Premièrement, regarder si barotone est installer. Regardé si il y a un dossier nommer `baritone` dans votre .minecraft.

Second, make sure that you're using the prefix properly, and that chat control is enabled in the way you expect.
For example, Impact disables direct chat control. (i.e. anything typed in chat without a prefix will be ignored and sent publicly). **This is a saved setting**, so if you run Impact once, `chatControl` will be off from then on, **even in other clients**.  So you'll need to use the `#` prefix or edit `baritone/settings.txt` in your Minecraft folder to undo that (specifically, remove the line `chatControl false` then restart your client).


## Why can I do `.goto x z` in Impact but nowhere else? Why can I do `-path to x z` in KAMI but nowhere else?
These are custom commands that they added; those aren't from Baritone.
The equivalent you're looking for is `goal x z` then `path`.
