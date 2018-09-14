# Integration between Baritone and Impact

Baritone will be in Impact 4.4 with nice integrations with its utility modules, but if you're impatient you can run Baritone on top of Impact 4.3 right now.

You can either build Baritone yourself, or download the "official" jar (as of commit <a href="https://github.com/cabaletta/baritone/commit/7d0914bd43a7cf0536b6b769c1d880a794bf29ef">7d0914b</a>, built on September 13) from <a href="https://www.dropbox.com/s/imc6xwwpwsh3i0y/baritone-1.0.0.jar?dl=0">here</a>. If you really want the cutting edge latest release, and you trust @Plutie#9079, commits are automatically built on their Jenkins <a href="http://24.202.239.85:8080/job/baritone/lastSuccessfulBuild/">here</a>.

To build it yourself, clone and setup Baritone (instructions in main README.md). Then, build the jar. From the command line, it's `./gradlew build` (or `gradlew build` on Windows). In IntelliJ, you can just start the `build` task in the Gradle menu.

Copy the jar into place. If you built it yourself, it should be at `build/libs/baritone-1.0.0.jar` in baritone (otherwise, if you downloaded it, it's in your Downloads). Copy it to your libraries in your Minecraft install. For example, on Mac I do `cp Documents/baritone/build/libs/baritone-1.0.0.jar Library/Application\ Support/minecraft/libraries/cabaletta/baritone/1.0.0/baritone-1.0.0.jar`. The first time you'll need to make the directory `cabaletta/baritone/1.0.0` in libraries first.

Then, we'll need to modify the Impact launch json. Open `minecraft/versions/1.12.2-Impact_4.3/1.12.2-Impact_4.3.json`. Alternatively, copy your existing installation and rename the version folder, json, and id in the json if you want to be able to choose from the Minecraft launcher whether you want Impact or Impact+Baritone.

- Add the Baritone tweak class to line 7 "minecraftArguments" like so: `"minecraftArguments": " ... --tweakClass clientapi.load.ClientTweaker --tweakClass baritone.launch.BaritoneTweakerOptifine",`. You need the Optifine tweaker even though there is no Optifine involved, for reasons I don't quite understand.
- Add the Baritone library. Insert `{ "name": "cabaletta:baritone:1.0.0" },` between Impact and ClientAPI, which should be between lines 15 and 16.

Restart the Minecraft launcher, then load Impact 4.3 as normal, and it should now include Baritone.
