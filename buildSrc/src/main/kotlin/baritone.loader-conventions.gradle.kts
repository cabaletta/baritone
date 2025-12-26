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
 * Common conventions plugin for mod loader modules.
 *
 * This convention plugin provides:
 * - Shadow JAR configuration for embedding dependencies
 * - Common mod loader dependencies (Mixin, ASM, pathfinder)
 * - Integration with root project source sets
 * - Dev JAR classifier for non-remapped artifacts
 * - Simplified publication configuration
 *
 * Applied to: Forge and Fabric subprojects
 * Dependencies: baritone.base-conventions, xyz.wagyourtail.unimined, com.gradleup.shadow
 *
 * Note: Each module must configure its own minecraft settings with the specific loader
 */
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("baritone.base-conventions")
    id("xyz.wagyourtail.unimined")
    id("com.gradleup.shadow")
}

// Access the version catalog with cleaner syntax
val libs = the<VersionCatalogsExtension>().named("libs")

// Additional repositories needed for UniMined compatibility
// UniMined requires project-level repositories due to its dynamic repository management
repositories {
    maven("https://babbaj.github.io/maven/") {
        name = "babbaj-repo"
        content {
            includeModule("dev.babbaj", "nether-pathfinder")
        }
    }

    // Mixin repository
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "spongepowered-repo"
        content {
            includeGroupByRegex("org\\.spongepowered.*")
        }
    }

}

// Configuration cache compatible - use providers for root project access
val rootSourceSets = providers.provider {
    rootProject.extensions.getByType<SourceSetContainer>()
}

configurations {
    val common = create("common")
    val shadowCommon = create("shadowCommon") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
}

dependencies {
    // Common dependencies for all loader modules
    compileOnly(libs.findLibrary("mixin").get())
    compileOnly(libs.findBundle("asm").get())
    implementation(libs.findLibrary("nether-pathfinder").get())
    "shadowCommon"(libs.findLibrary("nether-pathfinder").get()) {
        isTransitive = false
    }

    // Add all root project source sets except test and schematica_api
    rootSourceSets.get().forEach { sourceSet ->
        if (sourceSet.name != "test" && sourceSet.name != "schematica_api") {
            "common"(sourceSet.output)
        }
    }
}

// Note: Each module must configure its own minecraft settings with the loader

tasks {
    withType<ShadowJar>().configureEach {
        configurations = listOf(project.configurations["shadowCommon"])

        // Lazy evaluation - include root project source sets in the shadow jar
        from(providers.provider {
            rootSourceSets.get().filter {
                it.name !in setOf("test", "schematica_api")
            }.map { it.output }
        })

        // Exclude unnecessary files for smaller JARs
        exclude("META-INF/maven/**")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }

    named<Jar>("jar") {
        archiveClassifier.set("dev")
    }

    // Lazy configuration - build depends on remapJar when it exists
    named("build") {
        dependsOn(provider {
            tasks.findByName("remapJar") ?: tasks.named("shadowJar")
        })
    }
}

// Simplified publication configuration - exclude shadow runtime elements
components.findByName("java")?.let { javaComponent ->
    if (javaComponent is AdhocComponentWithVariants) {
        javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
            skip()
        }
    }
}