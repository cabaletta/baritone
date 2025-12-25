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

dependencies {
    // Use version catalog for consistency
    implementation("com.google.code.gson:gson:2.9.1")
    implementation("commons-io:commons-io:2.20.0")

    // UniMined plugin and mapping library
    implementation("xyz.wagyourtail.unimined:xyz.wagyourtail.unimined.gradle.plugin:1.4.1")
    implementation("xyz.wagyourtail.unimined.mapping:unimined-mapping-library-jvm:1.2.1")

    // Shadow plugin
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.5")
}