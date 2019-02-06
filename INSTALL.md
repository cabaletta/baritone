# Integration between Baritone and Impact
Impact 4.4 has Baritone included.

These instructions apply to Impact 4.3.

For Forge follow the instructions in [Setup](SETUP.md).

To run Baritone on Vanilla, just follow the instructions in the README (it's `./gradlew runClient`).

## An Introduction
There are some basic steps to getting Baritone setup with Impact.
- Acquiring a build of Baritone
- Placing Baritone in the libraries directory
- Modifying the Impact Profile JSON to run baritone
- How to use Baritone

## Acquiring a build of Baritone
There are two methods of acquiring a build of Baritone

### Official Release (Not always up to date)
https://github.com/cabaletta/baritone/releases

For Impact 4.3, there is no Baritone integration yet, so you will want `baritone-standalone-X.Y.Z.jar`. **For the rest of this guide, replace `X.Y.Z` with the actual numeric version you are using.**

Any official release will be GPG signed by leijurv (44A3EA646EADAC6A) and ZeroMemes (73A788379A197567). Please verify that the hash of the file you download is in `checksums.txt` and that `checksums_signed.asc` is a valid signature by those two public keys of `checksums.txt`. 

The build is fully deterministic and reproducible, and you can verify Travis did it properly by running `docker build --no-cache -t cabaletta/baritone .` yourself and comparing the shasum. This works identically on Travis, Mac, and Linux (if you have docker on Windows, I'd be grateful if you could let me know if it works there too).

### Building Baritone yourself
You can either build Baritone through a command line or through IntelliJ's UI, information on that can be found [here](SETUP.md#building).

## Placing Baritone in the libraries directory
``/libraries`` is a neat directory in your <a href="https://minecraft.gamepedia.com/.minecraft">Minecraft Installation Directory</a>
that contains all of the dependencies that are required from the game and some mods. This is where we will be
putting baritone.
- Locate the ``libraries`` folder, it should be in the Minecraft Installation Directory
- Create 3 new subdirectories starting from ``libraries``
  - ``cabaletta``
    - ``baritone``
      - ``X.Y.Z``
 - Copy the build of Baritone that was acquired earlier, and place it into the ``X.Y.Z`` folder, renamed like so:
   - The full path should look like ``<Minecraft>/libraries/cabaletta/baritone/X.Y.Z/baritone-X.Y.Z.jar``

## Modifying the Impact Profile JSON to run baritone
The final step is "registering" the Baritone library with Impact, so that it loads on launch.
- Ensure your Minecraft launcher is closed
- Navigate back to the Minecraft Installation Directory
- Find the ``versions`` directory, and open in
- In here there should be a ``1.12.2-Impact_4.3`` folder.
  - If you don't have any Impact folder or have a version older than 4.3, you can download Impact <a href="https://impactdevelopment.github.io">here</a>.
- Open the folder and inside there should be a file called ``1.12.2-Impact_4.3.json``
- Open the JSON file with a text editor that supports your system's line endings
  - For example, Notepad on Windows likely will NOT work for this. You should instead use a Text Editor like
  <a href="https://notepad-plus-plus.org/">Notepad++</a> if you're on Windows. (For other systems, I'm not sure
  what would work the best so you may have to do some research.)
- Find the ``libraries`` array in the JSON. It should look something like this.
    ```
    "libraries": [
        {
            "name": "net.minecraft:launchwrapper:1.12"
        },
        {
            "name": "com.github.ImpactDevelopment:Impact:4.3-1.12.2",
            "url": "https://impactdevelopment.github.io/maven/"
        },
        {
            "name": "com.github.ImpactDeveloment:ClientAPI:3.0.2",
            "url": "https://impactdevelopment.github.io/maven/"
        },
        ...
    ```
- Create two new objects in the array, between the ``Impact`` and ``ClientAPI`` dependencies preferably.
    ```
    {
        "name": "cabaletta:baritone:X.Y.Z"
    },
    {
        "name": "com.github.ImpactDevelopment:SimpleTweaker:1.2",
        "url": "https://impactdevelopment.github.io/maven/"
    },
    ```
- Now find the ``"minecraftArguments": "..."`` text near the top.
- At the very end of the quotes where it says ``--tweakClass clientapi.load.ClientTweaker"``, add on the following so it looks like:
  - ``--tweakClass clientapi.load.ClientTweaker --tweakClass baritone.launch.BaritoneTweaker"``
- If you didn't close your launcher for this step, restart it now.
- You can now launch Impact 4.3 as normal, and Baritone should start up
 
 ## How to use Baritone

- [Baritone chat control usage](USAGE.md)
