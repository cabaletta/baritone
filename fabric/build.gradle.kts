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
    id("baritone.distribution-conventions")
}

// Set the archive name for the Fabric module
base.archivesName.set("${rootProject.base.archivesName.get()}-fabric")

// Configure Minecraft with Fabric loader
unimined.minecraft {
    version(libs.versions.minecraft.get())

    mappings {
        intermediary()
        mojmap()
        parchment(version = libs.versions.parchment.get())
    }

    fabric {
        loader(libs.versions.fabric.get())
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        archiveClassifier.set("")
    }

    // Configure remapJar to use shadowJar output (using lazy configuration)
    named<xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask>("remapJar") {
        dependsOn("shadowJar")
        inputFile.set(named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").get().archiveFile)
    }
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "${rootProject.base.archivesName.get()}-${project.name}"
        }
    }
}