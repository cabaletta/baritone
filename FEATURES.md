# Current Features

## Implemented

- Podman-first isolated build flow for `baritone-1.7.10`
- Reproducible container environment via top-level `Containerfile`
- Root build helper script: `scripts/podman-build-1.7.10.sh`
- CI workflows that validate build/test on:
  - push to `main`
  - pull requests targeting `main`
- Legacy ForgeGradle 1.7.10 build patched to use currently reachable dependency/version endpoints

## In Progress / Not Yet Ported

- Full Baritone gameplay features (pathing, mining, build process, command system) in the 1.7.10 module
- Full API/behavior parity with newer upstream branches
- Dedicated automated in-game integration tests for LOTR-specific scenarios

## Scope Note

This documentation reflects the current 1.7.10 port implementation in this repository, not the full feature set of modern upstream Baritone branches.
