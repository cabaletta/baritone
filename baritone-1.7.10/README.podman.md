# Podman Build Environment for Baritone 1.7.10

## Introduction

The purpose of this setup is to provide reproducible builds for the legacy Minecraft 1.7.10 mod using Podman to isolate the build environment.

## Prerequisites

- Podman installed (e.g., on Linux: `sudo dnf install podman`)

- Git to clone the repo

- Basic CLI knowledge

## Setup

1. Clone the repository: `git clone https://github.com/cabaletta/baritone.git && cd baritone/baritone-1.7.10`

2. Build the image: `podman build -t baritone-build-env -f Dockerfile .`

   - This creates an image with Java 8 and Gradle 1.12.

## Building the Project

From the baritone-1.7.10 directory:

`podman run --rm -v $(pwd):/project -w /project baritone-build-env ./gradlew build`

- Mounts the current directory to /project in the container.

- Runs the Gradle build.

- Output JAR (if successful) in build/libs/.

## Current Status

The build currently fails due to unavailable Maven snapshots for the old ForgeGradle. This is a project dependency issue, not an environment issue. To fix, it may be necessary to update build.gradle or use alternative repositories/mirrors.

## Troubleshooting

- Podman not found: Install Podman.

- Permission issues: Use `--userns=keep-id` or run as rootless.

- Dependency errors: Research ForgeGradle 1.1 setup for MC 1.7.10; possibly add a custom repo.

- View logs: The command outputs build logs directly.

## Why Podman?

Daemonless, rootless containers for secure, reproducible builds without Docker.