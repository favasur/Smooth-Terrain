#!/usr/bin/env python3
"""Validate the generated mod content without launching the game.

Checks:
  * every block json is strict, parseable JSON and matches the family table
  * every vanilla code the smoothing pass can meet maps to a declared fillet block
  * the fillet mesh exists, is inside the unit cube, and has the expected material groups

Usage: python tools/verify_content.py
"""

import itertools
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_fillet_blocks import FAMILIES, SIDES  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLOCKTYPES = os.path.join(ROOT, "assets", "smoothterrain", "blocktypes")
MESH = os.path.join(ROOT, "assets", "smoothterrain", "shapes", "block", "terrain-fillet.obj")

errors = []


def fail(message):
    errors.append(message)


def load_blocks():
    blocks = {}
    for name in sorted(os.listdir(BLOCKTYPES)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(BLOCKTYPES, name)
        try:
            with open(path, encoding="utf-8-sig") as handle:
                blocks[name] = json.load(handle)
        except Exception as exc:  # noqa: BLE001 - report the parse error verbatim
            fail("%s: not strict JSON: %s" % (name, exc))
    return blocks


def expand(groups):
    return list(itertools.product(*groups))


def check_family(family, block, name):
    code = block.get("code")
    if code != family["code"]:
        fail("%s: code is %r, expected %r" % (name, code, family["code"]))
        return
    if block.get("class") != "BlockTerrainFillet":
        fail("%s: class must be BlockTerrainFillet" % name)

    expected_behaviors = [behavior["name"] for behavior in family.get("behaviors", [])]
    actual_behaviors = [behavior.get("name") for behavior in block.get("behaviors", [])]
    if actual_behaviors != expected_behaviors:
        fail("%s: behaviors are %s, expected %s" % (name, actual_behaviors, expected_behaviors))

    groups = block.get("variantgroups", [])
    expected = [(group, list(states)) for group, states in family["groups"]]
    expected.append(("side", SIDES))

    if len(groups) != len(expected):
        fail("%s: expected %d variant groups, found %d" % (name, len(expected), len(groups)))
        return

    for index, (group_code, states) in enumerate(expected):
        actual_code = groups[index].get("code")
        actual_states = list(groups[index].get("states", []))
        if actual_code != group_code:
            fail("%s: variant group %d is %r, expected %r" % (name, index, actual_code, group_code))
        if set(actual_states) != set(states):
            fail("%s: variant group %r states differ from the family table" % (name, group_code))
        if len(actual_states) != len(set(actual_states)):
            fail("%s: variant group %r has duplicate states" % (name, group_code))

    declared = set("-".join([code] + list(values)) for values in expand([g.get("states", []) for g in groups]))

    for values in expand([states for _, states in expected[:-1]]):
        for side in SIDES:
            suffix = "".join("-" + value for value in values)
            target = "%s%s-%s" % (code, suffix, side)
            if target not in declared:
                fail("%s: mapping %s%s-%s is not declared" % (name, family["source"], suffix, side))

    if not block.get("shape"):
        fail("%s: no shape" % name)
    if not (block.get("textures") or block.get("texturesByType")):
        fail("%s: no textures" % name)


def check_mesh():
    if not os.path.isfile(MESH):
        fail("mesh missing: %s" % MESH)
        return

    with open(MESH, encoding="utf-8") as handle:
        lines = handle.read().splitlines()

    verts = [line.split()[1:] for line in lines if line.startswith("v ")]
    if not verts:
        fail("mesh has no vertices")
        return

    coords = [[float(component) for component in vertex] for vertex in verts]
    for axis, label in enumerate("xyz"):
        low = min(vertex[axis] for vertex in coords)
        high = max(vertex[axis] for vertex in coords)
        if low < -0.5001 or high > 0.5001:
            fail("mesh leaves the unit cube on %s: %.3f..%.3f" % (label, low, high))

    materials = sorted({line.split()[1] for line in lines if line.startswith("usemtl ")})
    if materials != ["all", "top"]:
        fail("mesh materials are %s, expected ['all', 'top']" % materials)

    faces = [line for line in lines if line.startswith("f ")]
    if len(faces) != 50:
        fail("mesh has %d faces, expected 50" % len(faces))


def main():
    blocks = load_blocks()

    expected_files = {family["code"] + ".json" for family in FAMILIES}
    if set(blocks) != expected_files:
        fail("block json files are %s, expected %s" % (sorted(blocks), sorted(expected_files)))

    for family in FAMILIES:
        name = family["code"] + ".json"
        if name in blocks:
            check_family(family, blocks[name], name)

    check_mesh()

    if errors:
        for error in errors:
            print("FAIL " + error)
        return 1

    total = 0
    for block in blocks.values():
        total += len(expand([g.get("states", []) for g in block.get("variantgroups", [])]))
    print("OK: %d block json files, %d fillet variants, mesh valid" % (len(blocks), total))
    return 0


if __name__ == "__main__":
    sys.exit(main())
