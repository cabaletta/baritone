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