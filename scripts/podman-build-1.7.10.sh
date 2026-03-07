#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME="${IMAGE_NAME:-baritone-1.7.10-build}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

podman build -f "${ROOT_DIR}/Containerfile" -t "${IMAGE_NAME}" "${ROOT_DIR}"

podman run --rm \
  --userns=keep-id \
  -v "${ROOT_DIR}:/workspace" \
  -w /workspace/baritone-1.7.10 \
  "${IMAGE_NAME}" \
  gradle build
