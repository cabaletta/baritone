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
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    maven("https://maven.wagyourtail.xyz/releases") {
        name = "WagYourMaven"
    }
    maven("https://maven.minecraftforge.net/") {
        name = "ForgeMaven"
    }
    maven("https://maven.fabricmc.net/") {
        name = "FabricMaven"
    }
    maven("https://maven.neoforged.net/") {
        name = "NeoForgedMaven"
    }
    mavenCentral()
    gradlePluginPortal()
}

// Access the version catalog in buildSrc
val libs: VersionCatalog = versionCatalogs.named("libs")

dependencies {
    // Use version catalog for consistency
    implementation(libs.findLibrary("gson").get())
    implementation(libs.findLibrary("commons-io").get())

    // UniMined plugin and mapping library
    implementation(libs.findLibrary("unimined-plugin").get())
    implementation(libs.findLibrary("unimined-mapping").get())

    // Shadow plugin
    implementation(libs.findLibrary("shadow-plugin").get())
}