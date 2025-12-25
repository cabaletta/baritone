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
 * Distribution conventions for creating final artifacts
 * Simplified version without ProGuard (marked as optional)
 */

import org.gradle.kotlin.dsl.*

tasks {
    register<Copy>("createDist") {
        description = "Creates distribution artifacts"
        group = "distribution"

        from(tasks.named<Jar>("remapJar"))
        into(layout.buildDirectory.dir("dist"))

        doLast {
            println("Distribution created in: ${layout.buildDirectory.dir("dist").get()}")
        }
    }

    named("build") {
        finalizedBy("createDist")
    }
}