# Baritone Build System

## Overview
Baritone uses Gradle with Kotlin DSL for build configuration, supporting multiple Minecraft mod loaders and creating optimized variants through R8 obfuscation.

## Project Structure

```
baritone/
├── src/                 # Common source code and API
├── fabric/              # Fabric mod loader support
├── forge/               # Forge mod loader support
├── tweaker/             # LaunchWrapper tweaker support
├── buildSrc/            # Convention plugins for shared configuration
├── gradle/
│   └── libs.versions.toml  # Centralized version management
└── dist/                # Build output artifacts
```

## Version Management
All versions and dependencies are centralized in `gradle/libs.versions.toml`. This includes:
- Minecraft version (1.19.4)
- Mod loader versions (Fabric, Forge)
- Build tool versions (UniMined, Shadow, R8)
- Project metadata (version, group, archive name)

## Build Variants
The build system creates three variants for each module using R8:

1. **API** - Partial obfuscation
   - Keeps `baritone.api` package untouched
   - Obfuscates implementation code
   - For developers using Baritone as a library

2. **Standalone** - Full obfuscation
   - Maximum code optimization
   - Smallest file size
   - For end users

3. **Unoptimized** - No obfuscation
   - Original code preserved
   - Useful for debugging
   - Larger file size

## Building

### Basic Build
```bash
# Clean and build all modules
./gradlew clean build

# Build specific module
./gradlew :fabric:build
./gradlew :forge:build
./gradlew :tweaker:build
```

### Distribution
Build artifacts are automatically created in the `dist/` directory with SHA-256 and MD5 checksums:
```bash
# Create distribution with all variants
./gradlew build
```

Output structure:
```
dist/
├── baritone-api-{version}.jar
├── baritone-standalone-{version}.jar
├── baritone-unoptimized-{version}.jar
├── baritone-api-fabric-{version}.jar
├── baritone-standalone-fabric-{version}.jar
├── baritone-unoptimized-fabric-{version}.jar
└── ... (similar for forge and tweaker)
```

### Clean Distribution
The build automatically cleans old artifacts before creating new ones:
```bash
# Manually clean dist directory
./gradlew cleanDist
```

## Convention Plugins

The project uses convention plugins in `buildSrc/` for consistent configuration:

- **baritone.base-conventions** - Java toolchain, versioning, publishing
- **baritone.minecraft-conventions** - Minecraft mappings and dependencies
- **baritone.loader-conventions** - Shadow JAR configuration for mod loaders
- **baritone.r8-conventions** - R8 obfuscation for creating variants
- **baritone.distribution-conventions** - Distribution artifact creation

## Development

### Requirements
- Java 17 or higher
- Git for version tracking

### IDE Setup
The project works with IntelliJ IDEA and Eclipse:
```bash
# Generate IDE files
./gradlew idea
./gradlew eclipse
```

### Running Tests
```bash
./gradlew test
```

## Gradle Properties

Performance optimizations are configured in `gradle.properties`:
- 4GB heap allocation
- Parallel execution enabled
- Build caching enabled
- Kotlin incremental compilation

## CI/CD Integration

The build system maintains compatibility with CI/CD pipelines:
- Version can be overridden via `-Pmod_version` property
- Git describe is used for automatic versioning
- Clean builds append no suffix, dirty builds add `-dirty`

Example CI command:
```bash
./gradlew build -Pmod_version="$(git describe --always --tags --first-parent | cut -c2-)"
```

## Troubleshooting

### Out of Memory
Increase heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx6G
```

### Build Cache Issues
Clear the build cache:
```bash
./gradlew clean --no-build-cache
rm -rf ~/.gradle/caches/build-cache-*
```

### Configuration Cache
Currently disabled due to UniMined plugin incompatibility. This may impact build performance but ensures compatibility.