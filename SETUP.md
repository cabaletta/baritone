# Installation

The easiest way to install Baritone is as a Forge, NeoForge or Fabric mod: download the jar for your Minecraft version and put it in your `mods` folder.
If you know what you're doing you can also load it with a custom `version.json`
(examples: [1.14.4](https://www.dropbox.com/s/rkml3hjokd3qv0m/1.14.4-Baritone.zip?dl=1), [1.15.2](https://www.dropbox.com/s/8rx6f0kts9hvd4f/1.15.2-Baritone.zip?dl=1), [1.16.5](https://www.dropbox.com/s/i6f292o2i7o9acp/1.16.5-Baritone.zip?dl=1)).

Once Baritone is installed, see [the usage page](USAGE.md) for how to use it.

## Prebuilt official releases

Releases are made rarely and are not always up to date with the latest features and bug fixes. You can find them on the [releases page](https://github.com/cabaletta/baritone/releases), and the most common ones are linked from the [readme](README.md#downloads).

Which Baritone version goes with which Minecraft version:

| Minecraft version | 1.12 | 1.13 | 1.14 | 1.15 | 1.16 | 1.17 | 1.18 | 1.19 | 1.20  | 1.21 - 1.21.1 | 1.21.3 | 1.21.4 | 1.21.5 | 1.21.6 - 1.21.8 | 1.21.9 - 1.21.10 | 1.21.11 | 26.1  | 26.2  | 26.3  |
|-------------------|------|------|------|------|------|------|------|------|-------|---------------|--------|--------|--------|-----------------|------------------|---------|-------|-------|-------|
| Baritone version  | v1.2 | v1.3 | v1.4 | v1.5 | v1.6 | v1.7 | v1.8 | v1.9 | v1.10 | v1.11         | v1.12  | v1.13  | v1.14  | v1.15           | v1.16            | v1.17   | v1.18 | v1.19 | v1.20 |

### Verifying a release

Any official release will be GPG signed by leijurv (44A3EA646EADAC6A). Please verify that the hash of the file you download is in `checksums.txt` and that `checksums_signed.asc` is a valid signature by that public key of `checksums.txt`.

The build is fully deterministic and reproducible, and you can verify that by running `docker build --no-cache -t cabaletta/baritone .` yourself and comparing the shasum. This works identically on Travis, Mac, and Linux (if you have docker on Windows, I'd be grateful if you could let me know if it works there too).

## Artifacts

Building Baritone will create the final artifacts in the `dist` directory. These are the same as the artifacts created in the [releases](https://github.com/cabaletta/baritone/releases).

**The Forge, NeoForge and Fabric releases can simply be added as mods.**

There's three flavors:

- **API**: Only the non-api packages are obfuscated. Use this one if another one of your mods has a Baritone integration.
- **Standalone**: Everything is obfuscated. Other mods cannot use Baritone, but you get a bit of extra performance. Use this one otherwise.
- **Unoptimized**: Nothing is obfuscated. This shouldn't be used in production, but if you want to report a bug and spare us some effort, use this one.

And each flavor comes for:

- **Forge / NeoForge / Fabric**: Loadable as a standard mod using the respective loader. The Fabric build may or may not work on Quilt.
- **No loader**: Loadable as a launchwrapper tweaker against vanilla Minecraft using a custom `version.json`.

If you build from source you will also find mapping files in the `dist` directory. These contain the renamings done by ProGuard and are useful if you want to read obfuscated stack traces.

# Building it yourself

First, clone or download Baritone.

![Image](https://i.imgur.com/kbqBtoN.png)

If you choose to download, make sure you download the correct branch (one per Minecraft version) and extract the ZIP archive. Then follow one of the sections below.

## Java

You need the right Java version for the Minecraft version you're building. [Download Java here](https://adoptium.net/), and check which one you're using with `java -version`.

| Minecraft version | Java version |
|-------------------|--------------|
| 1.12.2 - 1.16.5   | 8            |
| 1.17.1            | 16           |
| 1.18.2 - 1.20.4   | 17           |
| 1.20.5 - 1.21.11  | 21           |
| 26.1 - 26.3       | 25           |

For anything newer, check the Java version in [the build CI action](/.github/workflows/gradle_build.yml) of the branch you want to build.

## Command line

On Mac and Linux, use `./gradlew` instead of `gradlew`.

For most branches, `gradlew build` builds everything, and the finished jars end up in the `dist` directory at the root of the repo. The exact tasks depend on the Minecraft version, so if that doesn't work, check [the build CI action](/.github/workflows/gradle_build.yml) of the branch you want to build, since this file might be out of date.

On older branches, `gradlew build` only builds the tweaker jar, and you need `gradlew build -Pbaritone.forge_build` or `gradlew build -Pbaritone.fabric_build` for Forge or Fabric instead. You might also have to run `setupDecompWorkspace` first.

## IntelliJ

- Open the project in IntelliJ as a Gradle project
- Refresh the Gradle project (or, to be safe, just restart IntelliJ)
- Depending on the Minecraft version, you may need to run `setupDecompWorkspace` or `genIntellijRuns` to get everything working

## GitHub Actions

Most branches have a CI workflow at `.github/workflows/gradle_build.yml`. If you fork this repository and enable actions on your fork, you can push a dummy commit to trigger it and have GitHub build Baritone for you.

If the commit you want to build is less than 90 days old, you can also find its workflow run in
[this list](https://github.com/cabaletta/baritone/actions/workflows/gradle_build.yml) and download the artifacts from there.
