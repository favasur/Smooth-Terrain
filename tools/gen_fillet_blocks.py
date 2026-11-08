#!/usr/bin/env python3
"""Generate the fillet block types from one terrain family table.

Every family mirrors a vanilla terrain block's variant groups, so a fillet code is the
vanilla code with the family prefix swapped and a side appended:

    soil-medium-normal + north  ->  smoothterrain:fillet-soil-medium-normal-north

Run from the repository root: python tools/gen_fillet_blocks.py
Pass --check to verify the committed block types are up to date.
"""

import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLOCKTYPES = os.path.join(ROOT, "assets", "smoothterrain", "blocktypes")
LANG = os.path.join(ROOT, "assets", "smoothterrain", "lang", "en.json")

SIDES = ["north", "east", "south", "west"]

FERTILITY = ["verylow", "low", "medium", "compost", "high"]
GRASS = ["none", "verysparse", "sparse", "normal"]
ROCK = [
    "andesite", "basalt", "bauxite", "chalk", "chert", "claystone", "conglomerate",
    "granite", "limestone", "peridotite", "phyllite", "sandstone", "shale", "slate",
]
ROCK_WITH_DEPOSIT = ROCK + [
    "halite", "kimberlite", "obsidian", "scoria", "suevite", "travertine", "tuff",
    "whitemarble", "redmarble", "greenmarble",
]
MOISTURE = ["dry", "wet", "wetdark", "wetverydark"]
GRAVEL_TYPE = ["cracked", "plain", "stoney"]

DIRT_SOUNDS = {
    "place": "block/dirt",
    "break": "block/dirt",
    "hit": "block/dirt",
    "walk": "walk/grass",
}
SAND_SOUNDS = {"place": "block/sand", "break": "block/sand", "hit": "block/sand", "walk": "walk/sand"}
GRAVEL_SOUNDS = {
    "place": "block/gravel",
    "break": "block/gravel",
    "hit": "block/gravel",
    "walk": "walk/gravel",
}
ROCK_SOUNDS = {
    "walk": "walk/stone",
    "byTool": {"Pickaxe": {"hit": "block/rock-hit-pickaxe", "break": "block/rock-break-pickaxe"}},
}

# The vanilla landslide behaviour is carried over so curved slopes are part of the
# same collapse mechanic as the cubes they replace: disturbed sand, gravel and snow
# slide as curved blocks instead of turning back into cubes.
SLIDE_PROPERTIES = {"fallSound": "effect/rockslide", "dustIntensity": 1, "fallSidewaysChance": 0.75}
SOFT_SLIDE_PROPERTIES = {"fallSound": "effect/rockslide", "dustIntensity": 0}
SNOW_FALL_PROPERTIES = {"impactDamageMul": 0, "fallSideways": False, "dustIntensity": 0.75}


def slides(properties):
    return [{"name": "UnstableFalling", "properties": properties}]

# The family list is the mod's coverage of natural walkable terrain. Prefixes are matched
# against vanilla block codes; longer, more specific prefixes must come first.
FAMILIES = [
    {
        "code": "fillet-soil",
        "source": "soil",
        "display": "Curved soil",
        "groups": [("fertility", FERTILITY), ("grasscoverage", GRASS)],
        "texture": "survival:block/soil/fert{fertility}",
        "grass_overlay": "survival:block/plant/grasscoverage/{grasscoverage}",
        "material": "Soil",
        "resistance": 1.8,
        "sounds": DIRT_SOUNDS,
    },
    {
        "code": "fillet-rawclay",
        "source": "rawclay",
        "display": "Curved clay",
        "groups": [("type", ["blue", "red", "fire"]), ("grasscoverage", ["none", "verysparse"])],
        "texture": "survival:block/soil/{type}clay",
        "grass_overlay": "survival:block/plant/grasscoverage/{grasscoverage}",
        "material": "Soil",
        "resistance": 4,
        "sounds": DIRT_SOUNDS,
    },
    {
        "code": "fillet-peat",
        "source": "peat",
        "display": "Curved peat",
        "groups": [("grasscoverage", ["none", "verysparse"])],
        "texture": "survival:block/soil/peat",
        "grass_overlay": "survival:block/plant/grasscoverage/{grasscoverage}",
        "material": "Soil",
        "resistance": 1.8,
        "sounds": DIRT_SOUNDS,
    },
    {
        "code": "fillet-cob",
        "source": "cob",
        "display": "Curved cob",
        "groups": [("grasscoverage", GRASS)],
        "texture": "survival:block/soil/cob/normal1",
        "grass_overlay": "survival:block/plant/grasscoverage/{grasscoverage}",
        "material": "Soil",
        "resistance": 2.2,
        "sounds": DIRT_SOUNDS,
    },
    {
        "code": "fillet-bonysoil",
        "source": "bonysoil",
        "display": "Curved bony soil",
        "groups": [],
        "texture": "survival:block/soil/bony/normal1",
        "material": "Soil",
        "resistance": 2.2,
        "sounds": DIRT_SOUNDS,
    },
    {
        "code": "fillet-muddygravel",
        "source": "muddygravel",
        "display": "Curved muddy gravel",
        "groups": [],
        "texture": "survival:block/soil/muddygravel",
        "material": "Soil",
        "resistance": 2.2,
        "sounds": DIRT_SOUNDS,
        "behaviors": slides(SOFT_SLIDE_PROPERTIES),
    },
    {
        "code": "fillet-sludgygravel",
        "source": "sludgygravel",
        "display": "Curved sludgy gravel",
        "groups": [],
        "texture": "survival:block/soil/sludgygravel",
        "material": "Soil",
        "resistance": 2.2,
        "sounds": DIRT_SOUNDS,
        "behaviors": slides(SOFT_SLIDE_PROPERTIES),
    },
    {
        "code": "fillet-sand",
        "source": "sand",
        "display": "Curved sand",
        "groups": [("rock", ROCK)],
        "texture": "survival:block/stone/sand/{rock}",
        "material": "Sand",
        "resistance": 1.8,
        "sounds": SAND_SOUNDS,
        "behaviors": slides(SLIDE_PROPERTIES),
    },
    {
        "code": "fillet-gravel",
        "source": "gravel",
        "display": "Curved gravel",
        "groups": [("rock", ROCK)],
        "texture": "survival:block/stone/gravel/{rock}",
        "material": "Gravel",
        "resistance": 2.4,
        "sounds": GRAVEL_SOUNDS,
        "behaviors": slides(SLIDE_PROPERTIES),
    },
    {
        "code": "fillet-dirtygravel",
        "source": "dirtygravel",
        "display": "Curved dirty gravel",
        "groups": [("moisture", MOISTURE), ("type", GRAVEL_TYPE)],
        "texture": "survival:block/stone/dirtygravel/{moisture}/{type}1",
        "material": "Gravel",
        "resistance": 2.4,
        "sounds": GRAVEL_SOUNDS,
        "behaviors": slides(SLIDE_PROPERTIES),
    },
    {
        "code": "fillet-rock",
        "source": "rock",
        "display": "Curved rock",
        "groups": [("rock", ROCK_WITH_DEPOSIT)],
        "texture": "survival:block/stone/rock/{rock}1",
        "material": "Stone",
        "resistance": 8,
        "requiredMiningTier": 2,
        "sounds": ROCK_SOUNDS,
    },
    {
        "code": "fillet-snowblock",
        "source": "snowblock",
        "display": "Curved snow",
        "groups": [],
        "texture": "survival:block/liquid/snow/normal1",
        "material": "Snow",
        "resistance": 3.5,
        "behaviors": slides(SNOW_FALL_PROPERTIES),
    },
    {
        "code": "fillet-packedglacierice",
        "source": "packedglacierice",
        "display": "Curved packed glacier ice",
        "groups": [],
        "texture": "survival:block/liquid/ice/packedglacier1",
        "material": "Ice",
        "resistance": 0.5,
        "lightAbsorption": 0,
    },
    {
        "code": "fillet-glacierice",
        "source": "glacierice",
        "display": "Curved glacier ice",
        "groups": [],
        "texture": "survival:block/liquid/ice/glacier1",
        "material": "Ice",
        "resistance": 0.5,
        "lightAbsorption": 0,
        "renderpass": "Transparent",
    },
    {
        "code": "fillet-lakeice",
        "source": "lakeice",
        "display": "Curved lake ice",
        "groups": [],
        "texture": "survival:block/liquid/ice/lake1",
        "material": "Ice",
        "resistance": 0.5,
        "lightAbsorption": 0,
        "renderpass": "Transparent",
    },
]

SHAPE = {
    "format": "obj",
    "base": "block/terrain-fillet",
    "rotateYByType": {"*-north": 0, "*-east": 270, "*-south": 180, "*-west": 90},
}


def texture_block(family):
    base = {"base": family["texture"]}
    if family.get("grass_overlay"):
        top = {"base": family["texture"], "overlays": [family["grass_overlay"]]}
        return {
            "texturesByType": {
                "*-none-*": {"obj": base, "all": base, "top": base},
                "*": {"obj": base, "all": base, "top": top},
            }
        }
    return {"textures": {"obj": base, "all": base, "top": base}}


def build_block(family):
    groups = [{"code": name, "states": list(states)} for name, states in family["groups"]]
    groups.append({"code": "side", "states": SIDES})

    block = {
        "code": family["code"],
        "class": "BlockTerrainFillet",
        "variantgroups": groups,
        "shape": SHAPE,
        "drawtype": "json",
        "blockmaterial": family["material"],
    }
    block.update(texture_block(family))
    block["attributes"] = {"handbook": {"exclude": True}}
    block["resistance"] = family["resistance"]
    block["lightAbsorption"] = family.get("lightAbsorption", 0)
    block["sideopaque"] = {"all": False}
    if family.get("behaviors"):
        block["behaviors"] = family["behaviors"]
    if family.get("renderpass"):
        block["renderpass"] = family["renderpass"]
    if family.get("requiredMiningTier"):
        block["requiredMiningTier"] = family["requiredMiningTier"]
    if family.get("sounds"):
        block["sounds"] = family["sounds"]
    return block


def render_files():
    files = {}
    for family in FAMILIES:
        files[family["code"] + ".json"] = json.dumps(build_block(family), indent=2) + "\n"
    return files


def render_lang():
    lang = {}
    for family in FAMILIES:
        lang["block-" + family["code"]] = family["display"]
        lang["block-" + family["code"] + "-*"] = family["display"]
    return json.dumps(lang, indent=2) + "\n"


def main():
    check = "--check" in sys.argv
    files = render_files()
    lang = render_lang()
    stale = []

    os.makedirs(BLOCKTYPES, exist_ok=True)
    for name, content in sorted(files.items()):
        path = os.path.join(BLOCKTYPES, name)
        existing = None
        if os.path.isfile(path):
            with open(path, encoding="utf-8") as handle:
                existing = handle.read()
        if existing != content:
            stale.append(path)
            if not check:
                with open(path, "w", encoding="utf-8", newline="\n") as handle:
                    handle.write(content)

    existing_lang = None
    if os.path.isfile(LANG):
        with open(LANG, encoding="utf-8") as handle:
            existing_lang = handle.read()
    if existing_lang != lang:
        stale.append(LANG)
        if not check:
            with open(LANG, "w", encoding="utf-8", newline="\n") as handle:
                handle.write(lang)

    if check:
        if stale:
            for path in stale:
                print("STALE " + os.path.relpath(path, ROOT))
            return 1
        print("OK: %d fillet block types up to date" % len(files))
        return 0

    print("Generated %d block types%s" % (len(files), "" if not stale else " (updated)"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
