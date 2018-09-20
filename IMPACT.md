# Integration between Baritone and Impact

## An Introduction
There are some basic steps to getting Baritone setup with Impact.
- Acquiring a build of Baritone
- Placing Baritone in the libraries directory
- Modifying the Impact Profile JSON to run baritone
- How to use Baritone

## Acquiring a build of Baritone
There are 3 methods of acquiring a build of Baritone (While it is still in development)

### Official Build (Not always up to date)
Download the "official" jar (as of commit <a href="https://github.com/cabaletta/baritone/commit/2e63ac41d9b22e4ee0a62f2bd29974e43e2071a1">2e63ac4</a>,
built on September 19) from <a href="https://www.dropbox.com/s/imc6xwwpwsh3i0y/baritone-1.0.0.jar?dl=0">here</a>.

### Building Baritone yourself
There are a few steps to this
- Clone this repository
- Setup the project as instructed in the README
- Run the ``build`` gradle task. You can either do this using IntelliJ's gradle UI or through a
command line
  - Windows: ``gradlew build``
  - Mac/Linux: ``./gradlew build``
- The build should be exported into ``/build/libs/baritone-1.0.0.jar``

### Cutting Edge Release
If you want to trust @Plutie#9079, you can download an automatically generated build of the latest commit
from his Jenkins server, found <a href="http://24.202.239.85:8080/job/baritone/lastSuccessfulBuild/">here</a>.

## Placing Baritone in the libraries directory
``/libraries`` is a neat directory in your <a href="https://minecraft.gamepedia.com/.minecraft">Minecraft Installation Directory</a>
that contains all of the dependencies that are required from the game and some mods. This is where we will be
putting baritone.
- Locate the ``libraries`` folder, it should be in the Minecraft Installation Directory
- Create 3 new subdirectories starting from ``libraries``
  - ``cabaletta``
    - ``baritone``
      - ``1.0.0``
 - Copy the build of Baritone that was acquired earlier, and place it into the ``1.0.0`` folder
   - The full path should look like ``<Minecraft>/libraries/cabaletta/baritone/1.0.0/baritone-1.0.0.jar``

## Modifying the Impact Profile JSON to run baritone
The final step is "registering" the Baritone library with Impact, so that it loads on launch.
- Ensure your Minecraft launcher is closed
- Navigate back to the Minecraft Installation Directory
- Find the ``profiles`` directory, and open in
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
- Create a new object in the array, between the ``Impact`` and ``ClientAPI`` dependencies preferably.
    ```
    {
      "name": "cabaletta:baritone:1.0.0"
    },
    ```
- Now find the ``"minecraftArguments": "..."`` text near the top.
- At the very end of the quotes where it says ``--tweakClass clientapi.load.ClientTweaker"``, add on the following so it looks like:
  - ``--tweakClass clientapi.load.ClientTweaker --tweakClass baritone.launch.BaritoneTweakerOptifine"``
  - It should now read something like 
- If you didn't close your launcher for this step, restart it now.
- You can now launch Impact 4.3 as normal, and Baritone should start up
 
 ## How to use Baritone
 Instructions on how to use Baritone are limited, and you may have to read a little bit of code (Really nothing much
  just plain English), you can view that <a href="https://github.com/cabaletta/baritone#chat-control">here</a>.
