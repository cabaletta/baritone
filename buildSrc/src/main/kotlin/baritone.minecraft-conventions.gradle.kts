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

/*
 * Minecraft-specific conventions using UniMined
 * Provides Minecraft mappings and common mod dependencies
 */

plugins {
    id("baritone.base-conventions")
    id("xyz.wagyourtail.unimined")
}

// Access the version catalog
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

// UniMined plugin adds its own repositories, so we need these for dependencies
repositories {
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