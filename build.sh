# Build and stage a loadable mod folder.

set -euo pipefail
cd "$(dirname "$0")"

export DOTNET_CLI_TELEMETRY_OPTOUT=1
export DOTNET_NOLOGO=1
export DOTNET_SKIP_FIRST_TIME_EXPERIENCE=1
export PATH="$PWD/.tools/.dotnet:$PATH"

# 1. Regenerate the fillet mesh from the shared Bezier profile.
dotnet run --project tools/FilletMeshGen/FilletMeshGen.csproj -- \
  assets/smoothterrain/shapes/block/terrain-fillet.obj

# 2. Regenerate the block types, then validate the content (JSON, variant coverage, mesh).
if command -v python >/dev/null 2>&1; then
  python tools/gen_fillet_blocks.py
  python tools/verify_content.py
else
  echo "python not found, skipping content generation and validation"
fi

# 3. Build the mod against the local game assemblies.
dotnet build SmoothTerrain.csproj -c Release

# 4. Stage a folder mod that can be dropped into the Mods directory.
rm -rf dist/SmoothTerrain
mkdir -p dist/SmoothTerrain
cp modinfo.json dist/SmoothTerrain/
cp -r assets dist/SmoothTerrain/assets
cp bin/Release/SmoothTerrain.dll dist/SmoothTerrain/
cp bin/Release/SmoothTerrain.pdb dist/SmoothTerrain/ 2>/dev/null || true

echo
echo "Staged mod folder: dist/SmoothTerrain"
echo "Copy it into your Vintage Story Mods folder (or zip its contents for distribution)."
