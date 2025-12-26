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

/**
 * Minecraft-specific conventions plugin using UniMined.
 *
 * This convention plugin provides:
 * - Minecraft 1.19.4 configuration via UniMined
 * - Mojang mappings with Parchment documentation
 * - Intermediary mappings for compatibility
 * - Core modding dependencies (Mixin, ASM)
 * - Baritone pathfinder dependency
 *
 * Applied to: Root project and loader modules needing Minecraft
 * Dependencies: baritone.base-conventions
 */

plugins {
    id("baritone.base-conventions")
    id("xyz.wagyourtail.unimined")
}

// Access the version catalog using cleaner syntax
val libs = the<VersionCatalogsExtension>().named("libs")

// Repositories are managed centrally in settings.gradle.kts via dependencyResolutionManagement

dependencies {
    // Core Minecraft modding dependencies
    compileOnly(libs.findLibrary("mixin").get())
    compileOnly(libs.findBundle("asm").get())  // Use bundle for ASM libraries

    // Baritone-specific dependency
    implementation(libs.findLibrary("nether-pathfinder").get())
}

// Configure Minecraft for the main source set with mappings
unimined.minecraft {
    version(libs.findVersion("minecraft").get().toString())

    mappings {
        intermediary()
        mojmap()
        parchment(version = libs.findVersion("parchment").get().toString())
    }
}