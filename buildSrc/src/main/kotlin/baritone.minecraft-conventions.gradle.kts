/*
 * Minecraft-specific conventions using UniMined
 * Provides Minecraft mappings and common mod dependencies
 */

plugins {
    id("baritone.base-conventions")
    id("xyz.wagyourtail.unimined")
}

// Access the version catalog
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Add required repositories for dependencies
repositories {
    // These are needed for the dependencies we're adding
    maven("https://babbaj.github.io/maven/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    compileOnly(libs.findLibrary("mixin").get())
    compileOnly(libs.findLibrary("asm").get())
    implementation(libs.findLibrary("nether-pathfinder").get())
}

// Configure Minecraft for the main source set with mappings
sourceSets.main {
    unimined.minecraft(this) {
        version(libs.findVersion("minecraft").get().toString())

        mappings {
            intermediary()
            mojmap()
            parchment(version = libs.findVersion("parchment").get().toString())
        }
    }
}