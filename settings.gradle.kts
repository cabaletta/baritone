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

pluginManagement {
    repositories {
        mavenLocal()
        maven("https://maven.wagyourtail.xyz/snapshots") {
            name = "WagYourMaven"
        }
        maven("https://maven.minecraftforge.net/") {
            name = "ForgeMaven"
        }
        maven("https://maven.fabricmc.net/") {
            name = "FabricMaven"
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
        }
        maven("https://maven.parchmentmc.net/") {
            name = "parchment"
        }

        // Mod loader repositories
        maven("https://maven.fabricmc.net/") {
            name = "fabric-maven"
        }
        maven("https://maven.minecraftforge.net/") {
            name = "forge-maven"
        }
        maven("https://maven.neoforged.net/") {
            name = "neoforged-maven"
        }

        // Mixin and related
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "spongepowered-repo"
        }

        // Baritone dependencies
        maven("https://babbaj.github.io/maven/") {
            name = "babbaj-repo"
        }
        maven("https://impactdevelopment.github.io/maven/") {
            name = "impactdevelopment-repo"
        }

        // LaunchWrapper for tweaker
        maven("https://files.multimc.org/maven/") {
            name = "multimc-maven"
            metadataSources {
                artifact()
            }
        }

        // Alternative for GitHub-based dependencies
        maven("https://jitpack.io") {
            name = "jitpack"
        }
    }

    // Version catalog is automatically loaded from gradle/libs.versions.toml
}

// Include subprojects
include("tweaker")
include("forge")
include("fabric")