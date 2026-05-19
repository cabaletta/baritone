# Development Guide

## [Javadocs](https://gaucho-matrero.github.io/altoclef/)

## Get it Running (IDE)

### James Green's setup guide!

[![James Green's Intellij Setup video on YouTube](https://img.youtube.com/vi/zZ1upxZ43Sg/0.jpg)](https://www.youtube.com/watch?v=zZ1upxZ43Sg)

### Text tutorial

Clone project and import. I'd suggest using JetBrain's IntelliJ to import the project. Make sure you're using jdk 16 in
your Project & Gradle Settings!

1) Open Intellij
2)

Click `File > New > Project from Version Control...` ![image](https://user-images.githubusercontent.com/13367955/146222866-42fa307b-016e-40a6-98bc-6e2428cde2dc.png)

3) Copy + Paste the altoclef clone URL and Clone (find
   here: )![image](https://user-images.githubusercontent.com/13367955/146223264-0cc436c0-4c08-4adc-b948-0ca3da4fbd6f.png)
4) Go to `File > Settings`, search `Gradle` and make sure your Gradle JVM is set to a JDK that's version 16 (IntelliJ
   lets you download open source JDKs, any of those should be
   fine) ![image](https://user-images.githubusercontent.com/13367955/146223463-2cfe8671-5504-430f-93d4-bb5312b2b540.png)
5) Go to `File > Project Structure`, then under `Project Settings/Project` make sure "Project SDK" is set to version
   16 ![image](https://user-images.githubusercontent.com/13367955/146223634-dc4d9eb3-293a-4e70-b5fa-29f44145e02c.png)
7) On the right side of the screen open the gradle tab and navigate to `Tasks/fabric/runClient`.
   Click `runClient` ![image](https://user-images.githubusercontent.com/13367955/146223786-243c63e9-790f-48d7-b627-4e9191a84f22.png)

If the gradle tab doesn't exist, try `View > Tool Windows > Gradle`

## Get it Running (Command line)

1) Git Clone project
2) `cd` into cloned local repo
3) `sudo / doas chmod +x gradlew` (skip this step if you are on windows)
4) `./gradlew build` or `./gradlew runClient`

## Modifying Baritone (dev mode)

Alto Clef uses a custom fork of baritone that gives you more control over how baritone works.
If you wish to make edits to that fork you can do so locally if you follow these steps:

1) Clone [The baritone fork](https://github.com/gaucho-matrero/baritone) into the same directory containing `altoclef`.
   For example, if you cloned `altoclef` into your desktop, `baritone` should be in your desktop as well.
2) Run `gradle build` within the fork you just cloned. You may open the folder in an IDE and run the `build` task.
3) There should now be various `.jar` files starting with `baritone` in the following folder: `baritone/build/libs`
4) Now within `altoclef`, pass `-Paltoclef.development` as a parameter when running `gradle build`
   (In IntelliJ, go to the build dropdown -> `Edit Configurations`, then duplicate the `altoclef [build]` configuration.
   In this duplicate, paste `-Paltoclef.development` into the Arguments text field.)
5) When you build and pass `-Paltoclef.development`, Alto Clef should now use the jar file inside
   of your custom `baritone` fork instead of pulling from online. This lets you rapidly test local changes to baritone.

## Task Development Guides

### Task Programming Tutorial Stream

[![More Recent AltoClef Task Programming VOD](https://img.youtube.com/vi/uROEqwyzn3o/0.jpg)](https://www.youtube.com/watch?v=uROEqwyzn3o)

### Old (Post Stream) Tutorial VOD

[![Rough AltoClef Tutorial VOD](https://img.youtube.com/vi/giBjHDZ7HvY/0.jpg)](https://www.youtube.com/watch?v=giBjHDZ7HvY)


