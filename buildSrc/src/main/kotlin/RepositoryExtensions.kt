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

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

/**
 * Extension function to configure Baritone-specific repositories.
 * This centralizes repository configuration to avoid duplication across build scripts.
 */
fun RepositoryHandler.configureBaritoneRepositories() {
    // Repository for nether-pathfinder dependency
    maven("https://babbaj.github.io/maven/") {
        name = "babbaj-repo"
        content {
            includeModule("dev.babbaj", "nether-pathfinder")
        }
    }

    // Repository for Mixin dependencies
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "spongepowered-repo"
        content {
            includeGroupByRegex("org\\.spongepowered.*")
        }
    }

    // Repository for SimpleTweaker
    maven("https://impactdevelopment.github.io/maven/") {
        name = "impactdevelopment-repo"
        content {
            includeModule("com.github.ImpactDevelopment", "SimpleTweaker")
            includeModule("io.github.impactdevelopment", "simpletweaker")
        }
    }

    // Repository for launchwrapper (used by tweaker module)
    maven("https://files.multimc.org/maven/") {
        name = "multimc-maven"
        content {
            includeModule("net.minecraft", "launchwrapper")
        }
        metadataSources {
            artifact()
        }
    }
}