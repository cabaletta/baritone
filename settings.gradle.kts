/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

rootProject.name = "baritone"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
// Configuration cache is incompatible with UniMined plugin
// enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

// Build cache configuration for improved performance
buildCache {
    local {
        isEnabled = true
        directory = file("${rootDir}/.gradle/build-cache")
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        maven("https://maven.wagyourtail.xyz/snapshots") {
            name = "WagYourMaven"
            content {
                includeGroupByRegex("xyz\\.wagyourtail.*")
            }
        }
        maven("https://maven.minecraftforge.net/") {
            name = "ForgeMaven"
            content {
                includeGroup("net.minecraftforge")
            }
        }
        maven("https://maven.fabricmc.net/") {
            name = "FabricMaven"
            content {
                includeGroup("net.fabricmc")
                includeGroup("fabric-loom")
            }
        }
        mavenCentral()
        gradlePluginPortal {
            content {
                excludeGroup("org.apache.logging.log4j")
            }
        }
    }
}

// Centralized dependency resolution management
dependencyResolutionManagement {
    // Allow project repositories (required for UniMined plugin to add its own repositories)
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)

    repositories {
        // Core repositories
        mavenCentral()
        mavenLocal()

        // Minecraft and mapping repositories
        maven("https://libraries.minecraft.net/") {
            name = "minecraft"
            content {
                includeGroup("net.minecraft")
                includeGroup("com.mojang")
            }
        }
        maven("https://maven.parchmentmc.net/") {
            name = "parchment"
            content {
                includeGroup("org.parchmentmc")
                includeGroup("net.parchmentmc")
            }
        }

        // Mod loader repositories
        maven("https://maven.fabricmc.net/") {
            name = "fabric-maven"
            content {
                includeGroupByRegex("net\\.fabricmc.*")
            }
        }
        maven("https://maven.minecraftforge.net/") {
            name = "forge-maven"
            content {
                includeGroupByRegex("net\\.minecraftforge.*")
                includeGroup("de.oceanlabs.mcp")
                includeGroup("cpw.mods")
            }
        }

        // Mixin and related
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "spongepowered-repo"
            content {
                includeGroupByRegex("org\\.spongepowered.*")
            }
        }

        // Baritone dependencies
        maven("https://babbaj.github.io/maven/") {
            name = "babbaj-repo"
            content {
                includeModule("dev.babbaj", "nether-pathfinder")
            }
        }
        maven("https://impactdevelopment.github.io/maven/") {
            name = "impactdevelopment-repo"
            content {
                includeModule("com.github.ImpactDevelopment", "SimpleTweaker")
                includeModule("io.github.impactdevelopment", "simpletweaker")
            }
        }

        // LaunchWrapper for tweaker
        maven("https://files.multimc.org/maven/") {
            name = "multimc-maven"
            content {
                includeModule("net.minecraft", "launchwrapper")
            }
            metadataSources {
                artifact()
            }
        }

        // Alternative for GitHub-based dependencies
        maven("https://jitpack.io") {
            name = "jitpack"
            content {
                includeGroupByRegex("com\\.github.*")
            }
        }
    }
}

// Include subprojects
include("tweaker")
include("forge")
include("fabric")