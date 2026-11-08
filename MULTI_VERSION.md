# Baritone — Multi-Version Build (1.16.5 + 1.21.4)

Этот репозиторий теперь поддерживает сборку Baritone для **двух версий Minecraft** одновременно:

| Minecraft | Baritone | Java | Система сборки | Загрузчики |
|-----------|----------|------|----------------|------------|
| **1.16.5** | v1.6.5 | 8 | ForgeGradle 3.0.115 + MCP `snapshot_20201028-1.16.3` | Forge, Fabric |
| **1.21.4** | v1.13.1 | 21 | Unimined + Mojmap + Parchment | Forge, Fabric, NeoForge |

Обе версии находятся в **форке `egefefefefef/baritone`** как отдельные ветки:
- `1.16.5` — полная история 1.16.5 (деприкейтнутая в апстриме, но сохранена здесь)
- `1.21.4` — текущая основная ветка (на ней же `arena/52d1d874-baritone`)

---

## Почему отдельные ветки?

Код Baritone сильно отличается между 1.16 и 1.21:
- Разные маппинги (MCP vs Mojmap), разные названия классов/методов
- Разный API Minecraft (например, `BlockState.isPathfindable`, `Fluids.FLOWING_WATER`)
- Разные системы модов (LaunchWrapper vs ModLoader, Mixin версия)
- Разный toolchain (Java 8 vs 21)

**Один JAR не может работать на обеих версиях** — это два разных мода. Поэтому мы собираем **два набора JAR'ов** и упаковываем их в один дистрибутив.

Если нужен *один файл* — используем `baritone-multi-*.jar`: это архив-контейнер, внутри `versions/1.16.5/` и `versions/1.21.4/` с готовыми JAR'ами.

---

## Быстрый старт (пользователь)

### Скачать готовые релизы
- **1.21.4**: [Releases 1.13.1](https://github.com/cabaletta/baritone/releases/tag/v1.13.1) — `baritone-api-forge-1.21.4-*.jar`
- **1.16.5**: [Releases 1.6.5](https://github.com/cabaletta/baritone/releases/tag/v1.6.5) — `baritone-api-forge-1.16.5-*.jar`

Или скачай комбинированный дистрибутив из `dist/` этой сборки:
- `dist/baritone-both-versions-1.16.5_1.21.4.zip` — все JAR'ы + доки + checksums
- `dist/baritone-multi-1.16.5_1.21.4-1.13.1.jar` — один контейнер с обеими версиями

### Установка
1. Распакуй ZIP (или открой multi-JAR как ZIP: `unzip baritone-multi-*.jar -d out`)
2. Выбери JAR под свою версию Minecraft и загрузчик:
   - `baritone-api-forge-1.16.5-*.jar` → `mods/` для Forge 1.16.5 (36.2.0+)
   - `baritone-api-fabric-1.16.5-*.jar` → `mods/` для Fabric 1.16.5 (+ fabric-loader 0.9.1)
   - `baritone-api-forge-1.21.4-*.jar` → `mods/` для Forge 1.21.4 (54.0.5)
   - `baritone-api-fabric-1.21.4-*.jar` → `mods/` для Fabric 1.21.4 (0.16.9)
   - `baritone-api-neoforge-1.21.4-*.jar` → `mods/` для NeoForge (9-beta)
3. **Не клади** `baritone-multi-*.jar` напрямую в `mods/` — это не мод, это контейнер.

---

## Сборка из исходников

### Требования

| Для | Нужно |
|-----|-------|
| 1.21.4 | Java 21 (Temurin), Gradle 8.x, сеть к `maven.fabricmc.net`, `maven.minecraftforge.net`, `repo.spongepowered.org`, `files.multimc.org` |
| 1.16.5 | Java 8 (AdoptOpenJDK 8), Gradle 6.x (через `gradlew`), сеть к `files.minecraftforge.net/maven`, `libraries.minecraft.net`, `maven.fabricmc.net` |

На текущей песочнице (эта CI) сеть ограничена (`github.com`, `registry.npmjs.org`, `pypi.org`) → реальная сборка с доступом к Minecraft Maven не пройдёт. Зато скрипт создаст **placeholder JAR'ы** с валидной структурой для проверки pipeline.

### Вариант A: Собрать всё одним скриптом

```bash
# Клонируй форк с обеими ветками
git clone https://github.com/egefefefefef/baritone.git
cd baritone
git fetch origin 1.16.5:1.16.5
git checkout arena/52d1d874-baritone   # или 1.21.4

# Собери обе версии (требует обе Java + сеть)
./scripts/build-both.sh

# Только склеить уже собранные JAR'ы (без перекомпиляции)
./scripts/build-both.sh --combine-only
# или
python3 scripts/combine-jars.py
```

Результат в `dist/`:
```
dist/
├── baritone-api-forge-1.16.5-1.6.5.jar
├── baritone-api-fabric-1.16.5-1.6.5.jar
├── baritone-standalone-forge-1.16.5-1.6.5.jar
├── baritone-api-forge-1.21.4-1.13.1.jar
├── baritone-api-fabric-1.21.4-1.13.1.jar
├── baritone-api-neoforge-1.21.4-1.13.1.jar
├── baritone-standalone-forge-1.21.4-1.13.1.jar
├── baritone-multi-1.16.5_1.21.4-1.13.1.jar   ← комбинированный контейнер
├── baritone-both-versions-1.16.5_1.21.4.zip ← дистрибутив
├── checksums.txt
└── mapping/ (после ProGuard)
```

### Вариант B: Собрать каждую версию отдельно

```bash
# 1.21.4 (текущая ветка)
git checkout 1.21.4
./gradlew build -Pmod_version="1.13.1"
# артефакты: dist/ + build/libs/

# 1.16.5 (отдельная ветка)
git checkout 1.16.5
# Forge:
./gradlew build -Pbaritone.forge_build
# Fabric:
./gradlew build -Pbaritone.fabric_build
# Артефакты: dist/ или build/libs/

# Склеить вручную:
git checkout arena/52d1d874-baritone
python3 scripts/combine-jars.py
```

### Вариант C: CI (GitHub Actions)

См. `.github/workflows/gradle_build_both.yml`:

```yaml
- name: Set up JDK 21 (for 1.21.4)
  uses: actions/setup-java@v5 with: { java-version: '21' }
- name: Build 1.21.4
  run: ./gradlew build -Pmod_version="..."

- name: Set up JDK 8 (for 1.16.5)
  uses: actions/setup-java@v5 with: { java-version: '8' }
- name: Build 1.16.5
  run: |
    git fetch origin 1.16.5
    git worktree add /tmp/1165 1.16.5
    cd /tmp/1165 && ./gradlew build -Pbaritone.forge_build
    cp dist/*.jar $GITHUB_WORKSPACE/dist/

- name: Combine
  run: python3 scripts/combine-jars.py

- name: Upload Artifacts
  uses: actions/upload-artifact@v7 with: { path: dist/ }
```

При пуше в `arena/52d1d874-baritone` workflow срабатывает автоматически.

---

## Структура комбинированного JAR

`baritone-multi-1.16.5_1.21.4-*.jar` — это обычный ZIP (JAR) с такой структурой:

```
baritone-multi-1.16.5_1.21.4-1.13.1.jar
├── META-INF/MANIFEST.MF              # список версий
├── README.txt                         # инструкция
├── docs/MULTI_VERSION.md              # эта документация
├── baritone/multi-version.json        # машиночитаемая таблица совместимости
├── versions/
│   ├── 1.16.5/
│   │   ├── baritone-api-forge-1.16.5-1.6.5.jar
│   │   ├── baritone-api-fabric-1.16.5-1.6.5.jar
│   │   └── baritone-standalone-forge-1.16.5-1.6.5.jar
│   └── 1.21.4/
│       ├── baritone-api-forge-1.21.4-1.13.1.jar
│       ├── baritone-api-fabric-1.21.4-1.13.1.jar
│       ├── baritone-api-neoforge-1.21.4-1.13.1.jar
│       └── baritone-standalone-forge-1.21.4-1.13.1.jar
└── baritone/MULTI_VERSION.txt
```

Проверка: `unzip -l dist/baritone-multi-*.jar`

---

## Таблица совместимости

| Minecraft | Forge | Fabric | NeoForge | Java | Baritone | Ветка форка |
|-----------|-------|--------|----------|------|----------|-------------|
| 1.16.5 | 36.2.34+ | 0.9.1+ | — | 8 | 1.6.5 | `1.16.5` |
| 1.21.4 | 54.0.5 | 0.16.9 | 9-beta | 21 | 1.13.1 | `1.21.4` / `arena/52d1d874-baritone` |

Также доступны промежуточные версии в апстриме `cabaletta/baritone`: `1.17.1`–`1.20.x`–`1.21.x` — по тому же принципу можно добавить новые ветки.

---

## FAQ

**Q: Можно ли сделать один JAR, который работает и на 1.16.5, и на 1.21.4?**  
A: Нет, без полной переработки Baritone на абстракцию. Minecraft API слишком разный. Единственный рабочий вариант — контейнер с двумя JAR'ами.

**Q: Почему 1.16.5 называется deprecated?**  
A: В `cabaletta/baritone` ветка помечена как deprecated (не получает обновлений). В этом форке мы её сохранили и можем мержить багфиксы вручную.

**Q: Как добавить поддержку 1.12.2 или 1.20.1?**  
A: Аналогично:
```bash
git fetch https://github.com/cabaletta/baritone.git 1.20.1:1.20.1
git push origin 1.20.1
# добавь в scripts/build-both.sh и combine-jars.py
```

**Q: Сборка падает с “Could not resolve …”**  
A: Проверь доступ к Maven: `curl -I https://maven.fabricmc.net` и `https://files.minecraftforge.net/maven`. В песочнице Arena они заблокированы — делай сборку локально.

**Q: Где взять Java 8?**  
A: https://adoptium.net/temurin/releases/?version=8 — выбери JDK 8 (HotSpot). Для 1.21.4 — JDK 21.

---

## Связанные файлы

- `scripts/build-both.sh` — оркестратор сборки обеих версий
- `scripts/combine-jars.py` — создание multi-JAR и ZIP
- `scripts/combine-jars.sh` — fallback
- `.github/workflows/gradle_build_both.yml` — CI для обеих версий
- `dist/` — выходные артефакты (генерируется)
- `SETUP.md`, `USAGE.md`, `FEATURES.md` — основная документация Baritone
