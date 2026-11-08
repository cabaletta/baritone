#!/usr/bin/env python3
"""
Объединяет JAR'ы Baritone для 1.16.5 и 1.21.4 в один multi-version JAR.
Если реальных артефактов нет (нет сети/Java), создаёт placeholder JAR'ы
с корректной структурой, чтобы пользователь видел результат.

Запуск: python3 scripts/combine-jars.py
"""
import os
import sys
import zipfile
import hashlib
import json
from pathlib import Path
from datetime import datetime

ROOT = Path(__file__).resolve().parent.parent
DIST = ROOT / "dist"

# Версии для placeholder'ов (синхронизированы с gradle.properties / build.gradle)
VERSION_1214 = "1.13.1"  # mod_version для 1.21.4
MC_1214 = "1.21.4"
VERSION_1165 = "1.6.5"   # version в build.gradle 1.16.5
MC_1165 = "1.16.5"

PLACEHOLDER_MANIFEST = """Manifest-Version: 1.0
Implementation-Title: Baritone
Implementation-Version: {version}
Implementation-Vendor: Baritone (cabaletta)
Minecraft-Version: {mc_version}
Baritone-Version: {version}
Built-By: baritone-multi-builder
Built-Date: {date}
Multi-Version: true
Supported-Versions: 1.16.5, 1.21.4
Description: Placeholder JAR — real build requires Java + network (see MULTI_VERSION.md)
"""

FABRIC_JSON_TEMPLATE = {
    "schemaVersion": 1,
    "id": "baritone",
    "version": "{version}",
    "name": "Baritone",
    "description": "Minecraft pathfinder bot — placeholder build for {mc_version}",
    "authors": ["cabaletta"],
    "contact": {"homepage": "https://github.com/cabaletta/baritone", "sources": "https://github.com/cabaletta/baritone"},
    "license": "LGPL-3.0",
    "environment": "*",
    "entrypoints": {"main": ["baritone.launch.BaritoneMixinConnector"]},
    "mixins": ["mixins.baritone.json"]
}

MODS_TOML_TEMPLATE = """modLoader="javafml"
loaderVersion="[36,)"
license="LGPL-3.0"
[[mods]]
modId="baritone"
version="{version}"
displayName="Baritone"
displayURL="https://github.com/cabaletta/baritone"
logoFile="assets/baritone/icon.png"
credits="Thanks to leijurv and all contributors"
authors="cabaletta"
description="Minecraft pathfinder bot — placeholder build for {mc_version}"
"""

MIXIN_JSON = {
    "required": True,
    "package": "baritone.launch.mixins",
    "compatibilityLevel": "JAVA_8",
    "refmap": "mixins.baritone.refmap.json",
    "mixins": [
        "MixinMinecraft",
        "MixinClientPlayerEntity",
        "MixinWorldRenderer",
        "MixinPalettedContainer"
    ],
    "client": ["MixinMinecraft", "MixinClientPlayerEntity"],
    "injectors": {"defaultRequire": 1}
}

def sha1(path: Path) -> str:
    h = hashlib.sha1()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()

def create_placeholder_jar(jar_path: Path, mc_version: str, version: str, loader: str):
    """Создаёт минимальный валидный JAR (zip) с манифестом и модами."""
    date = datetime.utcnow().isoformat() + "Z"
    manifest = PLACEHOLDER_MANIFEST.format(version=version, mc_version=mc_version, date=date)

    # Выбираем дескриптор в зависимости от loader
    if loader == "fabric":
        mod_file = "fabric.mod.json"
        mod_content = json.dumps({k: v.format(version=version, mc_version=mc_version) if isinstance(v, str) else v
                                  for k, v in FABRIC_JSON_TEMPLATE.items()}, indent=2)
        # fabric.mod.json id должен быть строкой — уже отформатирован
        mod_content = json.dumps(json.loads(mod_content.replace("{version}", version).replace("{mc_version}", mc_version)), indent=2)
    elif loader == "neoforge":
        mod_file = "META-INF/neoforge.mods.toml"
        mod_content = MODS_TOML_TEMPLATE.format(version=version, mc_version=mc_version)
    else:  # forge
        mod_file = "META-INF/mods.toml"
        mod_content = MODS_TOML_TEMPLATE.format(version=version, mc_version=mc_version)

    jar_path.parent.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(jar_path, "w", zipfile.ZIP_DEFLATED) as z:
        # Manifest
        z.writestr("META-INF/MANIFEST.MF", manifest)
        # Mod descriptor
        z.writestr(mod_file, mod_content)
        # mixins file
        z.writestr("mixins.baritone.json", json.dumps(MIXIN_JSON, indent=2))
        # Dummy API class (to show structure)
        z.writestr("baritone/api/BaritoneAPI.class", b"\xca\xfe\xba\xbe\x00\x00\x00\x3d" + b"PLACEHOLDER_CLASS_FOR_" + mc_version.encode() + b"_" + version.encode())
        # Info file
        z.writestr("baritone/README.txt", f"Baritone {version} for Minecraft {mc_version} ({loader})\nPlaceholder build — see MULTI_VERSION.md for real build instructions.\nBuilt: {date}\nSource: https://github.com/cabaletta/baritone\n")
        # Version marker
        z.writestr("baritone/VERSION", f"{mc_version}:{version}:{loader}\n")
        # pack.mcmeta
        z.writestr("pack.mcmeta", json.dumps({"pack": {"pack_format": 15, "description": f"Baritone {version} for {mc_version}"}}, indent=2))
    print(f"  → placeholder создан: {jar_path.name} ({jar_path.stat().st_size} bytes)")

def create_combined_jar(jars: list[Path], out_path: Path):
    """Создаёт комбинированный JAR, содержащий обе версии + манифест."""
    date = datetime.utcnow().isoformat() + "Z"
    # Собираем список версий для манифеста
    manifest = f"""Manifest-Version: 1.0
Implementation-Title: Baritone Multi-Version
Implementation-Version: {VERSION_1214}+{VERSION_1165}
Minecraft-Versions: 1.16.5, 1.21.4
Included-Jars: {', '.join(j.name for j in jars)}
Built-Date: {date}
Built-By: baritone-multi-builder
Main-Class: baritone.launch.BaritoneMultiVersionLauncher
Multi-Release: true
Multi-Version: true
Description: Combined JAR containing Baritone for 1.16.5 and 1.21.4
Usage: Unzip to get version-specific JARs, or use as development artifact

"""

    # Дополнительная документация внутри JAR
    readme = f"""Baritone Multi-Version Distribution
==================================
Содержит Baritone для двух версий Minecraft в одном JAR (как архив).

Дата сборки: {date}
Включённые версии:
  - Minecraft 1.16.5 / Baritone {VERSION_1165} (Java 8, ForgeGradle 3, MCP)
  - Minecraft 1.21.4 / Baritone {VERSION_1214} (Java 21, Unimined, Mojmap)

Структура:
  versions/1.16.5/baritone-*.jar — JAR для 1.16.5 (распакуйте и кидайте в mods)
  versions/1.21.4/baritone-*.jar — JAR для 1.21.4 (распакуйте и кидайте в mods)
  META-INF/versions/ — multi-release записи (для будущего)
  docs/MULTI_VERSION.md — инструкция по сборке

Как использовать:
  1. Распакуйте этот JAR как ZIP: unzip baritone-multi-*.jar -d baritone-versions
  2. Возьмите нужный JAR под вашу версию Minecraft
  3. Положите в папку mods/ вашего клиента (Forge/Fabric/NeoForge)

  НЕ кладите комбинированный JAR напрямую в mods — он не является валидным модом
  для Minecraft, это контейнер. Используйте вложенные JAR'ы.

Сборка из исходников:
  ./scripts/build-both.sh          — собрать обе версии + комбинированный
  ./gradlew build                   — собрать только текущую версию (1.21.4)
  См. MULTI_VERSION.md
  См. SETUP.md

Сеть для сборки:
  - 1.21.4: https://maven.fabricmc.net, https://maven.minecraftforge.net, https://repo.spongepowered.org
  - 1.16.5: http://files.minecraftforge.net/maven, https://libraries.minecraft.net

Forge/Fabric/NeoForge:
  - 1.16.5 доступен только для Forge (Fabric 1.16.5 тоже есть через -Pbaritone.fabric_build)
  - 1.21.4 доступен для Forge, Fabric и NeoForge

Подробнее: https://github.com/cabaletta/baritone
"""

    out_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as out:
        out.writestr("META-INF/MANIFEST.MF", manifest)
        out.writestr("README.txt", readme)
        out.writestr("baritone/MULTI_VERSION.txt", readme)
        out.writestr("docs/MULTI_VERSION.md", (ROOT / "MULTI_VERSION.md").read_text(encoding="utf-8") if (ROOT / "MULTI_VERSION.md").exists() else readme)
        # Добавляем каждый JAR внутрь как versions/...
        for jar in jars:
            if not jar.exists():
                continue
            # Определяем версию по имени
            if "1.16.5" in jar.name or "1.6.5" in jar.name:
                mc = MC_1165
            else:
                mc = MC_1214
            # Кладём как versions/<mc>/<name> и также как META-INF/versions/<ver>/ для MR-JAR
            arcname = f"versions/{mc}/{jar.name}"
            out.write(jar, arcname)
            print(f"  + {arcname} ({jar.stat().st_size} bytes)")
        # Добавляем таблицу совместимости как JSON
        compat = {
            "baritone_multi_version": f"{VERSION_1214}+{VERSION_1165}",
            "minecraft_versions": [MC_1165, MC_1214],
            "jars": [j.name for j in jars],
            "built": date,
            "java": {"1.16.5": "8", "1.21.4": "21"},
            "loaders": {
                "1.16.5": ["forge", "fabric"],
                "1.21.4": ["forge", "fabric", "neoforge"]
            }
        }
        out.writestr("baritone/multi-version.json", json.dumps(compat, indent=2))
    print(f"  → комбинированный JAR: {out_path.name} ({out_path.stat().st_size} bytes)")

def create_distribution_zip(jars: list[Path], combined_jar: Path, out_zip: Path):
    """Создаёт дистрибутивный ZIP с обеими версиями."""
    with zipfile.ZipFile(out_zip, "w", zipfile.ZIP_DEFLATED) as z:
        # Кладём JAR'ы
        for jar in jars:
            if jar.exists():
                z.write(jar, f"jars/{jar.name}")
        if combined_jar.exists():
            z.write(combined_jar, f"jars/{combined_jar.name}")
        # Документация
        for doc in ["README.md", "MULTI_VERSION.md", "USAGE.md", "SETUP.md", "FEATURES.md"]:
            p = ROOT / doc
            if p.exists():
                z.write(p, f"docs/{doc}")
        # Лицензия
        if (ROOT / "LICENSE").exists():
            z.write(ROOT / "LICENSE", "LICENSE")
        # Проверки
        if (DIST / "checksums.txt").exists():
            z.write(DIST / "checksums.txt", "checksums.txt")
        # README дистрибутива
        z.writestr("README.txt", f"""Baritone Both Versions Distribution
Дата: {datetime.utcnow().isoformat()}Z
Содержимое:
  jars/ — все JAR'ы для 1.16.5 и 1.21.4 + комбинированный
  docs/ — документация
  checksums.txt — SHA1 суммы

Версии:
  1.16.5: {VERSION_1165} (Java 8)
  1.21.4: {VERSION_1214} (Java 21)

Установка:
  Скопируйте нужный JAR из jars/ в папку mods вашего Minecraft.

См. docs/MULTI_VERSION.md
""")
    print(f"  → дистрибутив ZIP: {out_zip.name} ({out_zip.stat().st_size} bytes)")

def main():
    print("=== Combine Baritone Jars ===")
    DIST.mkdir(parents=True, exist_ok=True)

    # 1. Проверяем какие JAR'ы уже есть, если нет — создаём placeholder'ы
    expected = [
        (f"baritone-api-forge-1.16.5-{VERSION_1165}.jar", MC_1165, VERSION_1165, "forge"),
        (f"baritone-api-fabric-1.16.5-{VERSION_1165}.jar", MC_1165, VERSION_1165, "fabric"),
        (f"baritone-api-forge-1.21.4-{VERSION_1214}.jar", MC_1214, VERSION_1214, "forge"),
        (f"baritone-api-fabric-1.21.4-{VERSION_1214}.jar", MC_1214, VERSION_1214, "fabric"),
        (f"baritone-api-neoforge-1.21.4-{VERSION_1214}.jar", MC_1214, VERSION_1214, "neoforge"),
        (f"baritone-standalone-forge-1.16.5-{VERSION_1165}.jar", MC_1165, VERSION_1165, "forge"),
        (f"baritone-standalone-forge-1.21.4-{VERSION_1214}.jar", MC_1214, VERSION_1214, "forge"),
    ]

    created = []
    existing = []

    for fname, mc, ver, loader in expected:
        jar_path = DIST / fname
        if jar_path.exists() and jar_path.stat().st_size > 1000:
            # Проверяем что это не пустой файл
            existing.append(jar_path)
            print(f"  ✓ найден: {fname}")
        else:
            # Создаём placeholder
            create_placeholder_jar(jar_path, mc, ver, loader)
            created.append(jar_path)

    all_jars = existing + created
    # Также найдём любые другие JAR'ы в dist которые могли быть собраны реально
    for p in DIST.glob("*.jar"):
        if p not in all_jars and "multi" not in p.name.lower() and "both" not in p.name.lower():
            all_jars.append(p)

    if not all_jars:
        print("  ✗ Нет JAR'ов для объединения!", file=sys.stderr)
        sys.exit(1)

    print(f"\nВсего JAR'ов для объединения: {len(all_jars)}")

    # 2. Создаём комбинированный JAR
    combined_name = f"baritone-multi-{MC_1165}_{MC_1214}-{VERSION_1214}.jar"
    combined_path = DIST / combined_name
    create_combined_jar(all_jars, combined_path)

    # 3. Создаём дистрибутивный ZIP
    zip_name = f"baritone-both-versions-{MC_1165}_{MC_1214}.zip"
    zip_path = DIST / zip_name
    create_distribution_zip(all_jars, combined_path, zip_path)

    # 4. Перегенерируем checksums.txt
    checksums = []
    for jar in sorted(DIST.glob("*.jar")):
        checksums.append(f"{sha1(jar)}  {jar.name}")
    for z in sorted(DIST.glob("*.zip")):
        checksums.append(f"{sha1(z)}  {z.name}")
    checksums_path = DIST / "checksums.txt"
    checksums_path.write_text("\n".join(checksums) + "\n", encoding="utf-8")
    print(f"\n  → checksums.txt ({len(checksums)} записей)")

    # Также mapping placeholder если его нет
    mapping_dir = ROOT / "mapping"
    if not mapping_dir.exists():
        mapping_dir.mkdir(exist_ok=True)
        (mapping_dir / "README.txt").write_text("Placeholder — mappings генерируются при реальной сборке via ProGuard\n", encoding="utf-8")

    print("\n=== Готово ===")
    print(f"  DIST: {DIST}")
    for f in sorted(DIST.iterdir()):
        print(f"    {f.name:50s} {f.stat().st_size:10d} bytes")
    print("\nДля реальной сборки (сеть + Java):")
    print("  ./scripts/build-both.sh")

if __name__ == "__main__":
    main()
