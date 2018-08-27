# Baritone
A Minecraft bot. This project is an updated version of [Minebot](https://github.com/leijurv/MineBot/),
the original version of the bot for Minecraft 1.8, rebuilt for 1.12.2.

# Setup
- Open the project in IntelliJ as a Gradle project
- Run the Gradle task `setupDecompWorkspace`
- Refresh the Gradle project
- Run the Gradle task `genIntellijRuns`
- Select the "Minecraft Client" launch config and run

## Command Line
```
$ gradlew setupDecompWorkspace
$ gradlew --refresh-dependencies
$ gradlew genIntellijRuns
```
