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
    id("baritone.loader-conventions")
    id("baritone.distribution-conventions")
}

unimined.minecraft {
    version(libs.versions.minecraft.get())

    mappings {
        intermediary()
        mojmap()
        parchment(version = libs.versions.parchment.get())
    }

    runs {
        config("client") {
            mainClass = "net.minecraft.launchwrapper.Launch"
            args("--tweakClass", "baritone.launch.tweaker.BaritoneTweaker")
            jvmArgs("-Dmixin.debug=true")
        }
    }
}

dependencies {
    implementation(project(":"))
    "shadowCommon"(project(":"))

    implementation(libs.mixin)

    // ASM dependencies (Mixin doesn't include these for some reason)
    implementation(libs.bundles.asm)

    // Tweaker-specific dependencies
    implementation(libs.simple.tweaker)
    implementation(libs.launchwrapper) {
        exclude(module = "lwjgl")
        exclude(module = "asm-debug-all")
    }

    compileOnly(libs.jsr305)
}

tasks {
    jar {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true

        manifest {
            attributes(
                "MixinConfigs" to "mixins.baritone.json",
                "MixinConnector" to "baritone.launch.BaritoneMixinConnector",
                "Implementation-Title" to "Baritone",
                "Implementation-Version" to version
            )
        }
    }
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = rootProject.base.archivesName.get()
        }
    }
}