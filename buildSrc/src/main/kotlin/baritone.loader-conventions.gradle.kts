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
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

// UniMined plugin adds its own repositories, so we need these for dependencies
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
            // Note: Source set outputs are added directly in the shadowJar task, not through configuration
        }
    }
}

// Note: Each module must configure its own minecraft settings with the loader

tasks {
    named<ShadowJar>("shadowJar") {
        configurations = listOf(project.configurations["shadowCommon"])

        // Include root project source sets directly in the shadow jar
        rootSourceSets.forEach { sourceSet ->
            if (sourceSet.name != "test" && sourceSet.name != "schematica_api") {
                from(sourceSet.output)
            }
        }

        // Classifier is set per module if needed
    }

    named<Jar>("jar") {
        archiveClassifier.set("dev")
    }

    // Note: remapJar configuration is handled in each module

    // Build depends on remapJar when it exists (using lazy configuration)
    // This will be configured by modules that create the remapJar task
    configureEach {
        if (name == "build") {
            // The remapJar task is created by UniMined when a loader is configured
            // We'll check for it lazily
            dependsOn(provider {
                tasks.findByName("remapJar")
            })
        }
    }
}

// Configure publishing to exclude shadow jar variant
// This is configured immediately as the publication is created during configuration
publishing {
    publications {
        configureEach {
            if (this is MavenPublication) {
                // Remove shadowRuntimeElements variant from the publication
                suppressPomMetadataWarningsFor("shadowRuntimeElements")
            }
        }
    }
}

// Ensure shadow elements are not included in the java component
components.configureEach {
    if (name == "java" && this is AdhocComponentWithVariants) {
        // Skip shadow runtime elements to avoid duplicate artifacts
        withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
            skip()
        }
    }
}