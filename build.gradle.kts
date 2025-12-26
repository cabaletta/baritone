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
    id("baritone.base-conventions")
    id("xyz.wagyourtail.unimined")
}

base.archivesName.set("${base.archivesName.get()}-common")

// Use the shared repository configuration
repositories {
    configureBaritoneRepositories()
}

dependencies {
    // Add the same dependencies as minecraft-conventions but for the root project
    compileOnly(libs.mixin)
    compileOnly(libs.bundles.asm)
    implementation(libs.nether.pathfinder)
    testImplementation(libs.junit)
}

// Configure Minecraft for root project
unimined.minecraft {
    version(libs.versions.minecraft.get())

    mappings {
        intermediary()
        mojmap()
        parchment(version = libs.versions.parchment.get())
    }

    runs.off = true
    defaultRemapJar = false
}

sourceSets {
    val main by getting

    create("api") {
        compileClasspath += main.compileClasspath
        runtimeClasspath += main.runtimeClasspath
    }

    create("launch") {
        compileClasspath += main.compileClasspath + main.runtimeClasspath + main.output + sourceSets["api"].output
        runtimeClasspath += main.compileClasspath + main.runtimeClasspath + main.output + sourceSets["api"].output
    }

    create("schematica_api") {
        compileClasspath += main.compileClasspath
        runtimeClasspath += main.runtimeClasspath
    }

    main {
        compileClasspath += sourceSets["api"].output
        runtimeClasspath += sourceSets["api"].output
        compileClasspath += sourceSets["schematica_api"].output
        runtimeClasspath += sourceSets["schematica_api"].output
    }

    getByName("test") {
        compileClasspath += main.compileClasspath + main.runtimeClasspath + main.output
        runtimeClasspath += main.compileClasspath + main.runtimeClasspath + main.output
    }
}

tasks {
    jar {
        from(
            sourceSets["main"].output,
            sourceSets["launch"].output,
            sourceSets["api"].output
        )
    }

    javadoc {
        source = sourceSets["api"].allJava
        classpath = sourceSets["api"].compileClasspath

        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xwerror", "-quiet")
            isLinkSource = true
            encoding = "UTF-8"
            addBooleanOption("html5", true)
        }
    }
}

// Add aggregate task for CI
tasks.register("ciBuild") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}