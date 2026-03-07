# Baritone 1.7.10 Port (LOTR)

This repository is set up to build the 1.7.10 port in an isolated Podman container.

## Build (Podman-first)

From repo root:

```bash
./scripts/podman-build-1.7.10.sh
```

This script:
- builds the root `Containerfile`
- runs `gradle build` inside the container
- targets `/workspace/baritone-1.7.10`

Build outputs are written to:

`baritone-1.7.10/build/libs/`

## Manual commands

If you want to run commands manually:

```bash
podman build -f Containerfile -t baritone-1.7.10-build .
podman run --rm --userns=keep-id -v "$PWD:/workspace" -w /workspace/baritone-1.7.10 baritone-1.7.10-build gradle build
```

## Host dependencies

Required on host:
- Podman

Not required on host:
- Java
- Gradle

## Notes

- The legacy ForgeGradle dependency chain for 1.7.10 can still fail if upstream snapshot artifacts are unavailable.
- This setup isolates tooling to the container, but cannot fix missing external artifacts.
