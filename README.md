# Baritone 1.7.10 Port (LOTR)

This repository is focused on a **Minecraft 1.7.10 Forge port** with a **Podman-first, reproducible build**.

## Current Status

The 1.7.10 module currently builds successfully in Podman and produces a jar, but gameplay/pathing integration is still in early scaffold state.

## Quick Start

From repo root:

```bash
./scripts/podman-build-1.7.10.sh
```

Build output:

`baritone-1.7.10/build/libs/baritone-1.7.10-1.0.jar`

## Project Layout

- `Containerfile`: canonical container definition for builds
- `scripts/podman-build-1.7.10.sh`: build entrypoint
- `baritone-1.7.10/`: Forge 1.7.10 module

## CI

GitHub Actions validates builds on:
- pushes to `main`
- pull requests targeting `main`

Workflows run the build/test path in Podman.

## Docs

- Setup: [SETUP.md](SETUP.md)
- Usage: [USAGE.md](USAGE.md)
- Current capabilities: [FEATURES.md](FEATURES.md)
