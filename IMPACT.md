# Integration between Baritone and Impact

Baritone will be in Impact 4.4 with nice integrations with its hacks, but if you're impatient you can run Baritone on top of Impact 4.3 right now.

You can either build Baritone yourself, or download the jar from Steptember 3 from <a href="https://www.dropbox.com/s/vje9x3xd3eaplxu/baritone-1.0.jar?dl=0">here</a>

To build it yourself, clone and setup Baritone (instructions in main README.md). Then, build the jar. From the command line, it's `./gradlew build` (or `gradlew build` on Windows). In IntelliJ, you can just start the `build` task in the Gradle menu.

Copy the jar into place. It should be `build/libs/baritone-1.0.0.jar` in baritone. Copy it to your libraries in your Minecraft install. For example, on Mac I do `cp Documents/baritone/build/libs/baritone-1.0.0.jar Library/Application\ Support/minecraft/libraries/cabaletta/baritone/1.0.0/baritone-1.0.0.jar`. The first time you'll need to make the directory `cabaletta/baritone/1.0.0` in libraries first.

Then, we'll need to modify the Impact launch json. Open `minecraft/versions/1.12.2-Impact_4.3/1.12.2-Impact_4.3.json` or copy your existing installation and rename the version folder, json, and id in the json.

- Add the Baritone tweak class to line 7 "minecraftArguments" like so: `"minecraftArguments": " ... --tweakClass clientapi.load.ClientTweaker --tweakClass baritone.launch.BaritoneTweakerOptifine",`. You need the Optifine tweaker even though there is no Optifine involved, for reasons I don't quite understand.
- Add the Baritone library. Insert `{ "name": "cabaletta:baritone:1.0.0" },` between Impact and ClientAPI, which should be between lines 15 and 16.

Restart the Minecraft launcher, then load Impact 4.3 as normal, and it should now include Baritone.
