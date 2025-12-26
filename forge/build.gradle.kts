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

plugins {
    id("baritone.loader-conventions")
    id("baritone.r8-conventions")
    id("baritone.distribution-conventions")
}

// Set the archive name for the Forge module
base.archivesName.set("${rootProject.base.archivesName.get()}-forge")

// Configure Minecraft with Forge loader
unimined.minecraft {
    version(libs.versions.minecraft.get())

    mappings {
        intermediary()
        mojmap()
        parchment(version = libs.versions.parchment.get())
    }

    minecraftForge {
        loader(libs.versions.forge.get())
        mixinConfig("mixins.baritone.json")
    }
}

// Add nether-pathfinder to Minecraft runtime libraries for development
dependencies {
    "minecraftLibraries"(libs.nether.pathfinder)
}

tasks {
    processResources {
        inputs.property("version", version)

        filesMatching("META-INF/mods.toml") {
            expand("version" to version)
        }
    }

    jar {
        manifest {
            attributes(
                "MixinConfigs" to "mixins.baritone.json",
                "MixinConnector" to "baritone.launch.BaritoneMixinConnector",
                "Implementation-Title" to "Baritone",
                "Implementation-Version" to version
            )
        }
    }

    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveClassifier.set("dev-shadow")
    }

    // remapJar configuration is handled by baritone.r8-conventions
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "${rootProject.base.archivesName.get()}-${project.name}"
        }
    }
}