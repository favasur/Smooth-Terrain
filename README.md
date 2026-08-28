# Smooth Terrain

A standalone port of [NoCubes](https://github.com/Cadiboo/NoCubes)' smooth-terrain rendering and
collision to modern Minecraft. Meshes the world's surface into smooth, camera-friendly terrain
with custom collisions that match the mesh, per-block smoothness toggles, and a smoothness slider
in the Video Settings screen.

Spin-off of **Minecraft Builder Tools** (`favasur/Minecraft-Builder-Tools`), containing only the
Smooth Terrain functionality.

## Modules

Six loader/version combinations, each fully self-contained:

| Module           | Loader    | Minecraft | Notes                                              |
|------------------|-----------|-----------|----------------------------------------------------|
| (root)           | NeoForge  | 1.21.1    | Canonical build, full network sync                 |
| `forge-1211`     | Forge     | 1.21.1    | No Smooth Terrain network channel (local toggles)  |
| `fabric-1211`    | Fabric    | 1.21.1    | Full network sync via Fabric API                   |
| `neoforge-262`   | NeoForge  | 26.2      | 26.2 API rewrite                                   |
| `forge-262`      | Forge     | 26.2      | 26.2 API rewrite, no network channel               |
| `fabric-262`     | Fabric    | 26.2      | 26.2 API rewrite                                   |

The canonical (root) module holds the shared mesh/collision core; the other modules reference it
through their `build.gradle` source sets and supply loader-specific adapters.

## Building

Each module builds independently with its own Gradle wrapper (JDK 21 for the 1.21.1 modules,
JDK 25 for the 26.2 modules):

```bash
# NeoForge 1.21.1 (root)
./gradlew.bat jar

# any other module
cd forge-1211 && ./gradlew.bat jar
```

Jars are produced in `build/libs/` (or `build/libs/*-dev.jar` for the Fabric remap) and are also
collected into `dist/` via the `collectJars` task.

## Features

- **Smooth terrain rendering** – Surface-Nets mesh drawn into the vanilla chunk pipeline
  (NeoForge `AddSectionGeometryEvent`, Forge/Fabric via a `SectionCompiler` mixin).
- **Custom collisions** – collision shapes generated from the mesh, scoped to entity movement so
  camera, suffocation and pathfinding queries keep the vanilla shapes.
- **Per-block smoothness** – press **N** while looking at a block to toggle its smoothness
  (with an unbindable variant for a single block state).
- **Video Settings** – "Smooth Terrain" toggle and "Terrain Smoothness" slider (right = smoother).
- **Server sync (NeoForge/Fabric 1.21.1)** – server config and smoothable changes are synced over
  the network; the Forge 1.21.1 and 26.2 builds apply changes locally.

## License

MIT
