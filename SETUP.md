# Setup

## Prerequisites

Required on host:
- Podman
- Git

Not required on host:
- Java
- Gradle

## Build

From repo root:

```bash
./scripts/podman-build-1.7.10.sh
```

This command:
- builds the image from `Containerfile`
- runs `gradle build` in container at `/workspace/baritone-1.7.10`

Output jar:

`baritone-1.7.10/build/libs/baritone-1.7.10-1.0.jar`

## Manual Build Commands

```bash
podman build -f Containerfile -t baritone-1.7.10-build .
podman run --rm --userns=keep-id -v "$PWD:/workspace" -w /workspace/baritone-1.7.10 baritone-1.7.10-build gradle build
```

## Run Tests

```bash
podman build -f Containerfile -t baritone-1.7.10-build .
podman run --rm -v "$PWD:/workspace" -w /workspace/baritone-1.7.10 baritone-1.7.10-build gradle test
```

## Troubleshooting

- If dependency fetches fail, re-run; legacy 1.7.10 dependency infrastructure is less stable than modern mod toolchains.
- If Podman mount permissions fail, confirm rootless Podman is correctly configured in your environment.
