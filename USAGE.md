# Usage

This repository currently provides a **buildable 1.7.10 Forge mod scaffold** for the Baritone port.

## What You Can Do Right Now

1. Build the 1.7.10 jar in an isolated Podman environment.
2. Drop the built jar into a Forge 1.7.10 mods setup for smoke testing.
3. Iterate on port code under `baritone-1.7.10/src/main/java/baritone/port`.

## Build Command

```bash
./scripts/podman-build-1.7.10.sh
```

## Artifact

`baritone-1.7.10/build/libs/baritone-1.7.10-1.0.jar`

## Runtime Behavior (Current)

At this stage, the 1.7.10 module includes mod bootstrap/proxy classes only. Full Baritone command/pathing behavior is not yet restored in this module.

## Next Porting Work

- wire core Baritone subsystems into the 1.7.10 module
- add command surface parity for required workflows
- add integration tests/smoke tests for expected LOTR-mod scenarios
