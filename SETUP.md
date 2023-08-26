# Installation

The easiest way to install Baritone is to install [Impact](https://impactclient.net/), which comes with Baritone.

You can also use a custom version json for Minecraft, with the [1.14.4](https://www.dropbox.com/s/rkml3hjokd3qv0m/1.14.4-Baritone.zip?dl=1) version or the [1.15.2](https://www.dropbox.com/s/8rx6f0kts9hvd4f/1.15.2-Baritone.zip?dl=1) version or the [1.16.5](https://www.dropbox.com/s/i6f292o2i7o9acp/1.16.5-Baritone.zip?dl=1) version.

Once Baritone is installed, look [here](USAGE.md) for instructions on how to use it.

## Prebuilt official releases
These releases are not always completely up to date with latest features, and are only released from `master`. (so if you want `backfill-2` branch for example, you'll have to build it yourself)

Link to the releases page: [Releases](https://github.com/cabaletta/baritone/releases)

v1.2.* is for 1.12.2, v1.3.* is for 1.13.2, v1.4.* is for 1.14.4, v1.5.* is for 1.15.2, v1.6.* is for 1.16.5, v1.7.* is for 1.17.1, v1.8.* is for 1.18.1

Any official release will be GPG signed by leijurv (44A3EA646EADAC6A). Please verify that the hash of the file you download is in `checksums.txt` and that `checksums_signed.asc` is a valid signature by that public keys of `checksums.txt`. 

The build is fully deterministic and reproducible, and you can verify Travis did it properly by running `docker build --no-cache -t cabaletta/baritone .` yourself and comparing the shasum. This works identically on Travis, Mac, and Linux (if you have docker on Windows, I'd be grateful if you could let me know if it works there too).


## Artifacts

Building Baritone will result in 5 artifacts created in the ``dist`` directory. These are the same as the artifacts created in the [releases](https://github.com/cabaletta/baritone/releases).

**The Forge and Fabric releases can simply be added as a Forge/Fabric mods.**

If another one of your Forge mods has a Baritone integration, you want `baritone-api-forge-VERSION.jar`. Otherwise, you want `baritone-standalone-forge-VERSION.jar`

- **API**: Only the non-api packages are obfuscated. This should be used in environments where other mods would like to use Baritone's features.
- **Forge/Fabric API**: Same as API, but packaged for Forge/Fabric. This should be used where another mod has a Baritone integration.
- **Standalone**: Everything is obfuscated. This should be used in environments where there are no other mods present that would like to use Baritone's features.
- **Forge/Fabric Standalone**: Same as Standalone, but packaged for Forge/Fabric. This should be used when Baritone is your only Forge/Fabric mod, or none of your other Forge/Fabric mods integrate with Baritone.
- **Unoptimized**: Nothing is obfuscated. This shouldn't be used ever in production.
- **Forge/Fabric Unoptimized**: Same as Unoptimized, but packaged for Forge/Fabric.

## Build it yourself
- Clone or download Baritone

  ![Image](https://i.imgur.com/kbqBtoN.png)
  - If you choose to download, make sure you extract the ZIP archive.
- Follow one of the instruction sets below, based on your preference

## Command Line
On Mac OSX and Linux, use `./gradlew` instead of `gradlew`.

If you have errors with a package missing please make sure you have setup your environment, and are using Oracle JDK 8 for 1.12.2-1.16.5, JDK 16+ for 1.17.1, and JDK 17+ for 1.18.1.

To check which java you are using do 
`java -version` in a command prompt or terminal.
If you are using anything above OpenJDK 8 for 1.12.2-1.16.5, it might not work because the Java distributions above JDK 8 using may not have the needed javax classes.

Download java: https://adoptium.net/
#### macOS guide
In order to get JDK 8, Try running the following command:
`% /usr/libexec/java_home -V`
If it doesn't work try this guide: https://stackoverflow.com/questions/46513639/how-to-downgrade-java-from-9-to-8-on-a-macos-eclipse-is-not-running-with-java-9

If you see something like

`% 1.8.0_VERSION, x86_64:	"Java SE 8"	/Library/Java/JavaVirtualMachines/jdk1.8.0_VERSION.jdk/Contents/Home`

in the list then you've got JDK 8 installed. 
In order to get JDK 8 running in the **current terminal window** you will have to run this command: 

`% export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)`

To add OpenJDK 8 to your PATH add the export line to the end of your `.zshrc / .bashrc` if you want it to apply to each new terminal. If you're using bash change the .bashrc and if you're using zsh change the .zshrc

### Building Baritone

These tasks depend on the minecraft version, but are (for the most part) standard for building mods.

for more details, see [the build ci action](/.github/workflows/gradle_build.yml)

## IntelliJ
- Open the project in IntelliJ as a Gradle project
- Refresh the Gradle project (or, to be safe, just restart IntelliJ)
- depending on the minecraft version, you may need to run `setupDecompWorkspace` or `genIntellijRuns` in order to get everything working