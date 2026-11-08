#!/usr/bin/env bash
# Fallback shell версия combine-jars.py — если нет python3
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST="$ROOT/dist"
mkdir -p "$DIST"
echo "=== Shell fallback: создаю checksums.txt ==="
if ls "$DIST"/*.jar >/dev/null 2>&1; then
  (cd "$DIST" && sha1sum *.jar *.zip 2>/dev/null > checksums.txt || shasum *.jar *.zip > checksums.txt 2>/dev/null || echo "no shasum" > checksums.txt)
  cat "$DIST/checksums.txt" 2>/dev/null | head -20
fi
echo "Используй python3 scripts/combine-jars.py для полной сборки multi-JAR"
