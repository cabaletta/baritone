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

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

repositories {
    maven("https://babbaj.github.io/maven/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    compileOnly(libs.findLibrary("mixin").get())
    compileOnly(libs.findLibrary("asm").get())
    implementation(libs.findLibrary("nether-pathfinder").get())
    testImplementation(libs.findLibrary("junit").get())
}

sourceSets.main {
    unimined.minecraft(this) {
        version(libs.findVersion("minecraft").get().toString())

        mappings {
            intermediary()
            mojmap()
            parchment(version = libs.findVersion("parchment").get().toString())
        }

        runs.off = true
        defaultRemapJar = false
    }
}

sourceSets {
    val mainSourceSet = getByName("main")

    // Use .apply {} to configure after creation. This avoids type inference ambiguity.
    create("api").apply {
        compileClasspath += mainSourceSet.compileClasspath
        runtimeClasspath += mainSourceSet.runtimeClasspath
    }

    create("launch").apply {
        compileClasspath += mainSourceSet.compileClasspath + mainSourceSet.runtimeClasspath + mainSourceSet.output + sourceSets["api"].output
        runtimeClasspath += mainSourceSet.compileClasspath + mainSourceSet.runtimeClasspath + mainSourceSet.output + sourceSets["api"].output
    }

    create("schematica_api").apply {
        compileClasspath += mainSourceSet.compileClasspath
        runtimeClasspath += mainSourceSet.runtimeClasspath
    }

    getByName("main") {
        compileClasspath += sourceSets["api"].output
        runtimeClasspath += sourceSets["api"].output
        compileClasspath += sourceSets["schematica_api"].output
        runtimeClasspath += sourceSets["schematica_api"].output
    }

    getByName("test") {
        compileClasspath += mainSourceSet.compileClasspath + mainSourceSet.runtimeClasspath + mainSourceSet.output
        runtimeClasspath += mainSourceSet.compileClasspath + mainSourceSet.runtimeClasspath + mainSourceSet.output
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
        options {
            // Make the build fail on javadoc errors
            (this as StandardJavadocDocletOptions).apply {
                addStringOption("Xwerror", "-quiet")
                isLinkSource = true
                encoding = "UTF-8" // Allow emoji in comments
            }
        }
        source = sourceSets["api"].allJava
        classpath += sourceSets["api"].compileClasspath
    }
}