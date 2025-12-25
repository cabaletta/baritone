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
 * Common conventions for mod loader modules (Forge, Fabric, Tweaker)
 * Provides shadow jar configuration and common dependencies
 */
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("baritone.base-conventions")
    id("xyz.wagyourtail.unimined")
    id("com.gradleup.shadow")
}

// Access the version catalog
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Repositories needed for dependencies (using PREFER_PROJECT mode)
repositories {
    // Repository for nether-pathfinder dependency
    maven("https://babbaj.github.io/maven/")
    // Repository for Mixin
    maven("https://repo.spongepowered.org/repository/maven-public/")
    // Repository for SimpleTweaker
    maven("https://impactdevelopment.github.io/maven/")
    // Repository for launchwrapper
    maven("https://files.multimc.org/maven/") {
        metadataSources {
            artifact()
        }
    }
}

// Access root project for shared outputs
val rootSourceSets = rootProject.the<SourceSetContainer>()

configurations {
    create("common")
    create("shadowCommon")

    compileClasspath.get().extendsFrom(named("common").get())
    runtimeClasspath.get().extendsFrom(named("common").get())
}

dependencies {
    compileOnly(libs.findLibrary("mixin").get())
    compileOnly(libs.findLibrary("asm").get())

    // Include nether-pathfinder as a transitive dependency
    implementation(libs.findLibrary("nether-pathfinder").get())
    "shadowCommon"(libs.findLibrary("nether-pathfinder").get())

    // Add all root project source sets except test and schematica_api
    rootSourceSets.forEach { sourceSet ->
        if (sourceSet.name != "test" && sourceSet.name != "schematica_api") {
            "common"(sourceSet.output)
            "shadowCommon"(sourceSet.output)
        }
    }
}

// Note: Each module must configure its own minecraft settings with the loader

tasks {
    named<ShadowJar>("shadowJar") {
        configurations = listOf(project.configurations["shadowCommon"])
        // Classifier is set per module if needed
    }

    named<Jar>("jar") {
        archiveClassifier.set("dev")
    }

    // Note: remapJar configuration is handled in each module

    // Build depends on remapJar when it exists
    afterEvaluate {
        if (tasks.findByName("remapJar") != null) {
            build {
                dependsOn("remapJar")
            }
        }
    }
}

// Disable shadow jar from being included in publications
afterEvaluate {
    components.named<AdhocComponentWithVariants>("java") {
        withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
            skip()
        }
    }
}