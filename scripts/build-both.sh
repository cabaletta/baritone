#!/usr/bin/env bash
# Baritone Multi-Version Builder
# Собирает JAR для обеих версий Minecraft: 1.16.5 и 1.21.4
# Использование: ./scripts/build-both.sh [--combine-only]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$ROOT/dist"
WORKTREE_1165="/tmp/baritone-1.16.5-build"
WORKTREE_1214="/tmp/baritone-1.21.4-build"

# Цвета
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=== Baritone Multi-Version Build ===${NC}"
echo "Версии: 1.16.5 (Java 8, ForgeGradle 3) + 1.21.4 (Java 21, Unimined)"
echo "Текущая ветка: $(git -C "$ROOT" branch --show-current 2>/dev/null || echo 'unknown')"
echo ""

COMBINE_ONLY=false
if [[ "${1:-}" == "--combine-only" ]]; then
  COMBINE_ONLY=true
  echo -e "${YELLOW}Режим --combine-only: только сборка комбинированного JAR из dist/${NC}"
fi

# === 1. Проверка окружения ===
check_java() {
  if ! command -v java &>/dev/null; then
    echo -e "${YELLOW}⚠ Java не найдена в PATH${NC}"
    echo "  Для сборки 1.21.4 нужна Java 21, для 1.16.5 — Java 8"
    echo "  Установка: https://adoptium.net/"
    return 1
  fi
  echo -e "${GREEN}✓ Java: $(java -version 2>&1 | head -1)${NC}"
}

# === 2. Сборка 1.21.4 (текущая ветка) ===
build_1214() {
  echo ""
  echo -e "${GREEN}=== [1/2] Сборка 1.21.4 (Java 21, Forge/Fabric/NeoForge) ===${NC}"
  cd "$ROOT"
  git status --porcelain
  echo "  Запуск: ./gradlew build -Pmod_version=..."
  if [[ "$COMBINE_ONLY" == false ]]; then
    if command -v java &>/dev/null; then
      chmod +x ./gradlew
      ./gradlew build -Pmod_version="$(git describe --always --tags --first-parent 2>/dev/null | cut -c2- || echo '1.13.1')" || {
        echo -e "${RED}✗ Сборка 1.21.4 не удалась (возможно нет сети/maven)${NC}"
        echo "  Проверь доступ к https://maven.fabricmc.net, https://maven.minecraftforge.net"
        return 1
      }
      echo -e "${GREEN}✓ 1.21.4 собрана, артефакты в dist/${NC}"
      ls -lh dist/*.jar 2>/dev/null | head -20
    else
      echo -e "${YELLOW}  Пропуск: нет Java — будет использован placeholder${NC}"
      return 1
    fi
  else
    echo "  Пропуск (combine-only)"
  fi
}

# === 3. Сборка 1.16.5 (отдельный worktree) ===
build_1165() {
  echo ""
  echo -e "${GREEN}=== [2/2] Сборка 1.16.5 (Java 8, ForgeGradle) ===${NC}"
  if [[ "$COMBINE_ONLY" == false ]]; then
    # Создаём worktree если его нет
    if [[ ! -d "$WORKTREE_1165" ]]; then
      echo "  Создаю worktree для 1.16.5..."
      git -C "$ROOT" fetch origin 1.16.5 2>&1 | tail -5 || true
      # если ветка 1.16.5 уже есть локально, используем её
      if git -C "$ROOT" show-ref --verify --quiet refs/heads/1.16.5; then
        git -C "$ROOT" worktree add "$WORKTREE_1165" 1.16.5 2>&1 | tail -5 || true
      else
        git -C "$ROOT" worktree add "$WORKTREE_1165" origin/1.16.5 2>&1 | tail -5 || true
      fi
    else
      echo "  Worktree уже существует: $WORKTREE_1165"
      git -C "$WORKTREE_1165" pull --ff-only 2>&1 | tail -5 || true
    fi

    if command -v java &>/dev/null; then
      # Для 1.16.5 нужна Java 8 — пытаемся найти её
      JAVA8=""
      for candidate in /usr/lib/jvm/java-8-openjdk-amd64/bin/java /opt/jdk8/bin/java /usr/local/java8/bin/java; do
        if [[ -x "$candidate" ]]; then JAVA8="$candidate"; break; fi
      done
      if [[ -n "$JAVA8" ]]; then
        echo "  Найдена Java 8: $JAVA8"
        export JAVA_HOME="$(dirname $(dirname "$JAVA8"))"
        export PATH="$JAVA_HOME/bin:$PATH"
      else
        echo -e "${YELLOW}  Java 8 не найдена, пробую текущую Java (может не собраться)${NC}"
      fi

      cd "$WORKTREE_1165"
      chmod +x ./gradlew
      echo "  Запуск сборки 1.16.5..."
      # 1.16.5 собирается через forgesetup: build — сначала пробуем общий build
      ./gradlew build -Pbaritone.forge_build 2>&1 | tail -30 || \
      ./gradlew build 2>&1 | tail -30 || {
        echo -e "${RED}✗ Сборка 1.16.5 не удалась${NC}"
        return 1
      }
      echo -e "${GREEN}✓ 1.16.5 собрана${NC}"
      ls -lh "$WORKTREE_1165/dist/" 2>/dev/null | head -20 || ls -lh "$WORKTREE_1165/build/libs/" 2>/dev/null | head -20
      # Копируем артефакты в основной dist
      mkdir -p "$DIST_DIR"
      cp -v "$WORKTREE_1165/dist/"*.jar "$DIST_DIR/" 2>/dev/null || cp -v "$WORKTREE_1165/build/libs/"*.jar "$DIST_DIR/" 2>/dev/null || true
    else
      echo -e "${YELLOW}  Пропуск: нет Java${NC}"
      return 1
    fi
  else
    echo "  Пропуск (combine-only)"
  fi
}

# === 4. Создание комбинированного JAR ===
combine_jars() {
  echo ""
  echo -e "${GREEN}=== [3/3] Создание комбинированного JAR (обе версии) ===${NC}"
  cd "$ROOT"
  if [[ ! -d "$DIST_DIR" ]]; then mkdir -p "$DIST_DIR"; fi

  # Проверяем наличие артефактов
  echo "  Содержимое dist/ перед объединением:"
  ls -lh "$DIST_DIR" 2>/dev/null | head -30 || echo "  (пусто)"

  # Вызываем Python скрипт для сборки multi-version JAR
  if command -v python3 &>/dev/null; then
    python3 "$ROOT/scripts/combine-jars.py" || {
      echo -e "${YELLOW}  combine-jars.py не удался, пробую shell fallback${NC}"
      bash "$ROOT/scripts/combine-jars.sh" 2>/dev/null || true
    }
  else
    echo -e "${YELLOW}  python3 не найден, пробую shell${NC}"
    bash "$ROOT/scripts/combine-jars.sh" 2>/dev/null || true
  fi

  echo ""
  echo -e "${GREEN}=== Итоговые артефакты ===${NC}"
  ls -lh "$DIST_DIR" 2>&1 | head -40
  if [[ -f "$DIST_DIR/checksums.txt" ]]; then
    echo ""
    echo "checksums.txt:"
    cat "$DIST_DIR/checksums.txt"
  fi
}

# === Main ===
check_java || true
build_1214 || echo -e "${YELLOW}  -> будет создан placeholder для 1.21.4${NC}"
build_1165 || echo -e "${YELLOW}  -> будет создан placeholder для 1.16.5${NC}"
combine_jars

echo ""
echo -e "${GREEN}=== Готово! ===${NC}"
echo "Артефакты в dist/:"
echo "  - baritone-*.jar — отдельные версии (forge/fabric/neoforge, api/standalone)"
echo "  - baritone-multi-1.16.5_1.21.4.jar — комбинированный JAR (обе версии внутри)"
echo "  - baritone-both-versions.zip — дистрибутив с обеими версиями + документация"
echo ""
echo "Для ручной сборки без сети:"
echo "  ./scripts/build-both.sh --combine-only   # только склеить уже собранные JARы"
echo ""
