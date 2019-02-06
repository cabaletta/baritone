# Setup

## Prebuilt
(not always completely up to date with latest features)

Download from the [Releases](https://github.com/cabaletta/baritone/releases)

The Forge releases can simply be added as a Forge mod.

If another one of your Forge mods has a Baritone integration, you want `baritone-api-forge-VERSION.jar`. Otherwise, you want `baritone-standalone-forge-VERSION.jar`

Previously (Baritone v1.1.2 and below), it was not fully compatible with the latest version of Forge. `freeLook` was broken in Forge 14.23.4.2744. Forge 14.23.4.2743 or **older** worked with Baritone v1.1.2 and lower. Newer versions of Forge "worked", sort of, but Baritone's movement became unreliable and `freeLook` must be off.

## Artifacts

Building Baritone will result in 5 artifacts created in the ``dist`` directory. These are the same as the artifacts created in the [releases](https://github.com/cabaletta/baritone/releases).

- **API**: Only the non-api packages are obfuscated. This should be used in environments where other mods would like to use Baritone's features.
- **Forge API**: Same as API, but packaged for Forge. This should be used where another mod has a Baritone integration.
- **Standalone**: Everything is obfuscated. This should be used in environments where there are no other mods present that would like to use Baritone's features.
- **Forge Standalone**: Same as Standalone, but packaged for Forge. This should be used when Baritone is your only Forge mod, or none of your other Forge mods integrate with Baritone.
- **Unoptimized**: Nothing is obfuscated. This shouldn't be used ever in production.

## More Info
To replace out Impact 4.4's Baritone build with a customized one, switch to the `impact4.4-compat` branch, build Baritone as above then copy `dist/baritone-api-$VERSION$.jar` into `minecraft/libraries/cabaletta/baritone-api/1.0.0/baritone-api-1.0.0.jar`, replacing the jar that was previously there. You also need to edit `minecraft/versions/1.12.2-Impact_4.4/1.12.2-Impact_4.4.json`, find the line `"name": "cabaletta:baritone-api:1.0.0"`, remove the comma from the end, and entirely remove the line that's immediately after (starts with `"url"`). 

Impact 4.4 **only** works with builds from the quite outdated `impact4.4-compat` branch. If you must have the latest Baritone features with Impact, and can't wait for 4.5, consider creating a standalone (non forge) build then adding it to Impact 4.**3** via the instructions in [Install](INSTALL.md).

## Build it yourself
- Clone or download Baritone

  ![Image](https://i.imgur.com/kbqBtoN.png)
  - If you choose to download, make sure you extract the ZIP archive.
- Follow one of the instruction sets below, based on your preference

## Command Line
On Mac OSX and Linux, use `./gradlew` instead of `gradlew`.

Setting up the Environment:

```
$ gradlew setupDecompWorkspace
$ gradlew --refresh-dependencies
```

Running Baritone:

```
$ gradlew runClient
```

For information on how to build baritone, see [Building Baritone](#building-baritone)

## IntelliJ
- Open the project in IntelliJ as a Gradle project
  
  ![Image](https://i.imgur.com/jw7Q6vY.png)

- Run the Gradle tasks `setupDecompWorkspace` then `genIntellijRuns`
  
  ![Image](https://i.imgur.com/QEfVvWP.png)

- Refresh the Gradle project (or, to be safe, just restart IntelliJ)
  
  ![Image](https://i.imgur.com/3V7EdWr.png)

- Select the "Minecraft Client" launch config
  
  ![Image](https://i.imgur.com/1qz2QGV.png)

- Click on ``Edit Configurations...`` from the same dropdown and select the "Minecraft Client" config
  
  ![Image](https://i.imgur.com/s4ly0ZF.png)

- In `Edit Configurations...` you need to select `baritone_launch` for `Use classpath of module:`.
  
  ![Image](https://i.imgur.com/hrLhG9u.png)

# Building

Make sure that you have properly [setup](#setup) the environment before trying to build it.

## Command Line

```
$ gradlew build
```

## IntelliJ

- Navigate to the gradle tasks on the right tab as follows

  ![Image](https://i.imgur.com/PE6r9iN.png)

- Double click on **build** to run it