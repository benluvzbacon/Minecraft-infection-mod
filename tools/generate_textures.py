#!/usr/bin/env python3
"""Generate every Candy Infection texture as a PNG, with no external libraries.

The mod's whole visual identity is "bright confectionery hiding something
dangerous", so the textures are saturated pinks, magentas, purples, cyans,
yellows, oranges and chocolate browns.  Nothing here is grey or desaturated.

Run from the repository root:

    python3 tools/generate_textures.py
"""

import os
import struct
import zlib
import random

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "candyinfection")

# --------------------------------------------------------------------------- png


def write_png(path, width, height, rows):
    """rows is a list of byte strings, each width*4 bytes long (RGBA)."""
    def chunk(tag, data):
        body = tag + data
        return (struct.pack(">I", len(data)) + body
                + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF))

    raw = b"".join(b"\x00" + row for row in rows)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(png)


def px(r, g, b, a=255):
    return bytes((max(0, min(255, int(r))), max(0, min(255, int(g))),
                  max(0, min(255, int(b))), max(0, min(255, int(a)))))


def canvas(width, height):
    return [[None] * width for _ in range(height)]


def flatten(grid, width):
    rows = []
    for row in grid:
        rows.append(b"".join(row))
    return rows


def save(name, width, height, grid, subfolder="textures/block"):
    path = os.path.join(ASSETS, subfolder, name + ".png")
    write_png(path, width, height, flatten(grid, width))
    return path


# ------------------------------------------------------------------- generators


def noise_texture(size, base, contrast=26, seed=0):
    """Speckled block texture - the default for stone/dirt/sand style blocks."""
    rng = random.Random(seed)
    grid = canvas(size, size)
    for y in range(size):
        for x in range(size):
            d = rng.uniform(-contrast, contrast)
            grid[y][x] = px(base[0] + d, base[1] + d, base[2] + d)
    return grid


def blob_texture(size, base, spots, seed=0, spot_size=2, alpha=255):
    """Base colour with rounded blobs on top - candy/gummy look."""
    rng = random.Random(seed)
    grid = noise_texture(size, base, 14, seed)
    for _ in range(spots):
        cx = rng.randrange(size)
        cy = rng.randrange(size)
        r = rng.uniform(spot_size * 0.5, spot_size)
        tint = tuple(min(255, c + rng.uniform(-30, 50)) for c in base)
        for y in range(size):
            for x in range(size):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    grid[y][x] = px(tint[0], tint[1], tint[2], alpha)
    return grid


def stripe_texture(size, base, stripes=4, seed=0):
    """Diagonal candy stripes - lollipops and hard candy."""
    rng = random.Random(seed)
    grid = canvas(size, size)
    light = tuple(min(255, c + 70) for c in base)
    for y in range(size):
        for x in range(size):
            band = ((x + y) // (max(1, size // stripes))) % 2
            colour = light if band else base
            d = rng.uniform(-12, 12)
            grid[y][x] = px(colour[0] + d, colour[1] + d, colour[2] + d)
    return grid


def crystal_texture(size, base, seed=0):
    """Faceted crystal: bright core, darker edges, a few highlights."""
    rng = random.Random(seed)
    grid = canvas(size, size)
    for y in range(size):
        for x in range(size):
            edge = min(x, y, size - 1 - x, size - 1 - y)
            shade = 1.0 - edge * 0.12
            d = rng.uniform(-18, 18)
            if rng.random() < 0.06:
                grid[y][x] = px(255, 255, 255, 220)
            else:
                grid[y][x] = px(base[0] * shade + d, base[1] * shade + d, base[2] * shade + d)
    return grid


def swirl_texture(size, base, seed=0):
    """Chocolate style: smooth with faint marbling."""
    rng = random.Random(seed)
    grid = canvas(size, size)
    import math
    for y in range(size):
        for x in range(size):
            wave = math.sin((x * 0.9 + math.sin(y * 0.45) * 3.0)) * 22
            d = rng.uniform(-10, 10) + wave
            grid[y][x] = px(base[0] + d, base[1] + d * 0.8, base[2] + d * 0.6)
    return grid


def leaf_texture(size, base, seed=0):
    """Gummy leaves: translucent with a lighter vein."""
    rng = random.Random(seed)
    grid = noise_texture(size, base, 34, seed)
    for y in range(size):
        for x in range(size):
            if x == size // 2 or y == size // 2:
                c = tuple(min(255, v + 60) for v in base)
                grid[y][x] = px(c[0], c[1], c[2], 235)
            else:
                r, g, b, a = grid[y][x]
                grid[y][x] = px(r, g, b, rng.choice((205, 220, 235)))
    return grid


def plant_texture(size, base, seed=0):
    """Candy plant: mostly transparent with a stem and leaves."""
    rng = random.Random(seed)
    grid = [[px(0, 0, 0, 0)] * size for _ in range(size)]
    stem = tuple(max(0, c - 40) for c in base)
    for y in range(size // 2, size):
        grid[y][size // 2] = px(stem[0], stem[1], stem[2])
    for _ in range(7):
        cx = rng.randrange(2, size - 2)
        cy = rng.randrange(1, size // 2 + 3)
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                if 0 <= cx + dx < size and 0 <= cy + dy < size and abs(dx) + abs(dy) <= 1:
                    grid[cy + dy][cx + dx] = px(*base)
    return grid


def item_shape_texture(size, base, kind="round", seed=0):
    """Simple recognisable item silhouettes."""
    rng = random.Random(seed)
    grid = [[px(0, 0, 0, 0)] * size for _ in range(size)]
    light = tuple(min(255, c + 55) for c in base)
    dark = tuple(max(0, c - 45) for c in base)
    cx = cy = size / 2.0
    if kind == "round":
        radius = size * 0.42
        for y in range(size):
            for x in range(size):
                dist = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
                if dist <= radius:
                    shade = light if dist < radius * 0.5 else base
                    grid[y][x] = px(shade[0], shade[1], shade[2])
                elif dist <= radius + 0.9:
                    grid[y][x] = px(*dark)
    elif kind == "shard":
        for y in range(size):
            for x in range(size):
                if abs(x - cy) <= y * 0.34 and y < size - 2:
                    shade = light if x < cy else base
                    grid[y][x] = px(shade[0], shade[1], shade[2])
    elif kind == "bar":
        for y in range(size // 4, size - size // 4):
            for x in range(size // 6, size - size // 6):
                grid[y][x] = px(*(light if (x + y) % 4 < 2 else base))
        for y in range(size // 4, size - size // 4):
            grid[y][size // 6] = px(*dark)
    elif kind == "bottle":
        for y in range(size // 4, size - 2):
            for x in range(size // 3, size - size // 3):
                grid[y][x] = px(*(light if x < size // 2 else base), 235)
        for y in range(2, size // 4):
            grid[y][size // 2] = px(*dark)
    elif kind == "stick":
        for y in range(2, size - 1):
            grid[y][size // 2] = px(230, 230, 210)
        radius = size * 0.28
        for y in range(size):
            for x in range(size):
                dist = ((x + 0.5 - cx) ** 2 + (y + 0.5 - (size * 0.3)) ** 2) ** 0.5
                if dist <= radius:
                    grid[y][x] = px(*(light if (x + y) % 3 == 0 else base))
    elif kind == "gem":
        for y in range(size):
            span = int((1 - abs(y - cy) / cy) * size * 0.42)
            for x in range(int(cx) - span, int(cx) + span + 1):
                if 0 <= x < size:
                    grid[y][x] = px(*(light if y < cy else base), 245)
    return grid


def tool_texture(size, head, handle, seed=0):
    rng = random.Random(seed)
    grid = [[px(0, 0, 0, 0)] * size for _ in range(size)]
    for i in range(size - 3):
        grid[size - 3 - i][2 + i] = px(handle[0], handle[1], handle[2])
        grid[size - 2 - i][2 + i] = px(max(0, handle[0] - 40), max(0, handle[1] - 40), max(0, handle[2] - 40))
    for y in range(1, 7):
        for x in range(7, 15):
            if (x - 11) ** 2 + (y - 4) ** 2 <= 13:
                shade = head if (x + y) % 3 else tuple(min(255, c + 45) for c in head)
                grid[y][x] = px(*shade)
    return grid


def armour_texture(size, base, seed=0):
    rng = random.Random(seed)
    grid = [[px(0, 0, 0, 0)] * size for _ in range(size)]
    light = tuple(min(255, c + 40) for c in base)
    for y in range(3, size - 2):
        for x in range(3, size - 3):
            if (x - 4) * (x - 4) + (y - 4) * (y - 4) > 90:
                continue
            grid[y][x] = px(*(light if y < 7 else base))
    return grid


def entity_texture(size, base, accent, seed=0):
    """Big flat texture the vanilla model UVs sample from."""
    rng = random.Random(seed)
    grid = canvas(size, size)
    for y in range(size):
        for x in range(size):
            d = rng.uniform(-16, 16)
            # a couple of accent bands so limbs read against the body
            band = (y // 8 + x // 16) % 3 == 0
            colour = accent if band else base
            grid[y][x] = px(colour[0] + d, colour[1] + d, colour[2] + d)
    return grid


def particle_texture(size, base):
    grid = canvas(size, size)
    cx = cy = size / 2.0
    for y in range(size):
        for x in range(size):
            dist = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            alpha = int(255 * max(0.0, 1.0 - dist / (size / 2.0)))
            grid[y][x] = px(base[0], base[1], base[2], alpha)
    return grid


def icon_texture(size):
    rng = random.Random(1234)
    grid = canvas(size, size)
    for y in range(size):
        for x in range(size):
            t = y / size
            r = 255
            g = int(105 + 90 * t)
            b = int(180 + 40 * (1 - t))
            d = rng.uniform(-10, 10)
            grid[y][x] = px(r + d, g + d, b + d)
    # a magenta blob in the middle
    cx = cy = size / 2.0
    for y in range(size):
        for x in range(size):
            dist = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if dist < size * 0.30:
                grid[y][x] = px(255, 20, 147)
            elif dist < size * 0.34:
                grid[y][x] = px(255, 105, 180)
    return grid


# ------------------------------------------------------------------------- main

PINK = (255, 105, 180)
MAGENTA = (255, 20, 147)
PURPLE = (155, 93, 229)
CYAN = (0, 187, 249)
YELLOW = (255, 225, 79)
ORANGE = (255, 140, 0)
GREEN = (140, 255, 79)
BROWN = (92, 51, 23)
LIGHT_BROWN = (181, 101, 29)

BLOCKS = [
    ("infected_grass_block", lambda: blob_texture(16, PINK, 14, seed=11, spot_size=3)),
    ("infected_dirt", lambda: noise_texture(16, (214, 84, 148), 30, seed=12)),
    ("candy_stone", lambda: noise_texture(16, (240, 150, 200), 22, seed=13)),
    ("candy_bricks", lambda: brick_texture(16, (232, 120, 180), seed=14)),
    ("sugar_crystal_ore", lambda: blob_texture(16, (240, 150, 200), 6, seed=15, spot_size=2)),
    ("sugar_crystal_block", lambda: crystal_texture(16, CYAN, seed=16)),
    ("sugar_crystal_cluster", lambda: crystal_texture(16, (120, 230, 255), seed=17)),
    ("candy_sand", lambda: noise_texture(16, (255, 214, 170), 26, seed=18)),
    ("candy_gravel", lambda: blob_texture(16, (242, 170, 190), 22, seed=19, spot_size=2)),
    ("sticky_syrup", lambda: swirl_texture(16, (255, 165, 40), seed=20)),
    ("caramel_growth", lambda: swirl_texture(16, (212, 130, 30), seed=21)),
    ("gummy_log", lambda: stripe_texture(16, (214, 40, 140), 8, seed=22)),
    ("gummy_planks", lambda: stripe_texture(16, (255, 130, 190), 6, seed=23)),
    ("gummy_leaves", lambda: leaf_texture(16, (110, 230, 90), seed=24)),
    ("gummy_growth", lambda: plant_texture(16, GREEN, seed=25)),
    ("chocolate_growth", lambda: plant_texture(16, (150, 90, 50), seed=26)),
    ("chocolate_blob", lambda: swirl_texture(16, BROWN, seed=27)),
    ("candy_vines", lambda: plant_texture(16, MAGENTA, seed=28)),
    ("lollipop_pink", lambda: stripe_texture(16, PINK, 5, seed=29)),
    ("lollipop_blue", lambda: stripe_texture(16, (80, 170, 255), 5, seed=30)),
    ("lollipop_red", lambda: stripe_texture(16, (240, 60, 90), 5, seed=31)),
    ("lollipop_yellow", lambda: stripe_texture(16, YELLOW, 5, seed=32)),
    ("hard_candy_pink", lambda: crystal_texture(16, PINK, seed=33)),
    ("hard_candy_red", lambda: crystal_texture(16, (240, 60, 90), seed=34)),
    ("hard_candy_purple", lambda: crystal_texture(16, PURPLE, seed=35)),
    ("hard_candy_cyan", lambda: crystal_texture(16, CYAN, seed=36)),
    ("infection_core", lambda: blob_texture(16, MAGENTA, 20, seed=37, spot_size=3)),
    ("purifier", lambda: blob_texture(16, (60, 200, 200), 12, seed=38, spot_size=2)),
]


def brick_texture(size, base, seed=0):
    rng = random.Random(seed)
    grid = noise_texture(size, base, 12, seed)
    mortar = tuple(min(255, c + 55) for c in base)
    for y in range(size):
        for x in range(size):
            offset = 0 if (y // 4) % 2 == 0 else 4
            if y % 4 == 3 or (x + offset) % 8 == 7:
                grid[y][x] = px(*mortar)
    return grid


MATERIAL_ITEMS = {
    "sugar_shard": (WHITE := (250, 250, 235), "shard"),
    "hardened_sugar": ((255, 250, 220), "gem"),
    "gummy_resin": (GREEN, "round"),
    "caramel_chunk": ((226, 150, 60), "round"),
    "chocolate_core": (BROWN, "gem"),
    "jawbreaker_fragment": (PURPLE, "shard"),
    "infection_crystal": (MAGENTA, "gem"),
    "purification_crystal": (CYAN, "gem"),
    "candy_essence": (PINK, "gem"),
    "holy_sugar": ((255, 255, 200), "gem"),
    "gummy_worm": (GREEN, "round"),
    "candy_apple": ((240, 60, 90), "round"),
    "chocolate_bar": (LIGHT_BROWN, "bar"),
    "giant_lollipop": (PINK, "stick"),
    "caramel_apple": ((226, 150, 60), "round"),
    "sugar_cookie": ((245, 210, 150), "round"),
    "jawbreaker_candy": (PURPLE, "round"),
    "purification_potion": (CYAN, "bottle"),
    "anti_candy_syringe": ((230, 250, 255), "stick"),
    "confectioner_trophy": (YELLOW, "gem"),
}

ENTITIES = [
    ("candy_crawler", PINK, GREEN),
    ("gummy_spawn", GREEN, (210, 255, 170)),
    ("gummy_brute", ORANGE, YELLOW),
    ("sugar_leech", YELLOW, PINK),
    ("candy_mimic", (255, 119, 221), PURPLE),
    ("caramel_beast", LIGHT_BROWN, (255, 169, 77)),
    ("jawbreaker", PURPLE, CYAN),
    ("chocolate_creeper", BROWN, PINK),
    ("lollipop_stalker", (255, 45, 120), (255, 255, 255)),
    ("the_confectioner", MAGENTA, BROWN),
]


def main():
    count = 0
    for name, factory in BLOCKS:
        save(name, 16, 16, factory(), "textures/block")
        count += 1

    for name, (base, kind) in MATERIAL_ITEMS.items():
        save(name, 16, 16, item_shape_texture(16, base, kind, seed=hash(name) % 9973), "textures/item")
        count += 1

    tool_specs = [
        ("candy_pickaxe", PINK), ("candy_axe", PINK), ("candy_shovel", PINK),
        ("candy_hoe", PINK), ("candy_hammer", ORANGE),
        ("sugar_sword", (250, 250, 235)), ("jawbreaker_mace", PURPLE),
        ("candy_bow", MAGENTA), ("gummy_spear", GREEN),
        ("chocolate_hammer", BROWN), ("caramel_blade", (226, 150, 60)),
    ]
    for name, colour in tool_specs:
        save(name, 16, 16, tool_texture(16, colour, (120, 80, 40), seed=hash(name) % 9973), "textures/item")
        count += 1

    armour_specs = [
        ("candy_helmet", PINK), ("candy_chestplate", PINK),
        ("candy_leggings", PINK), ("candy_boots", PINK),
        ("hardened_sugar_helmet", (255, 250, 220)), ("hardened_sugar_chestplate", (255, 250, 220)),
        ("hardened_sugar_leggings", (255, 250, 220)), ("hardened_sugar_boots", (255, 250, 220)),
        ("confectioner_helmet", MAGENTA), ("confectioner_chestplate", MAGENTA),
        ("confectioner_leggings", MAGENTA), ("confectioner_boots", MAGENTA),
    ]
    for name, colour in armour_specs:
        save(name, 16, 16, armour_texture(16, colour, seed=hash(name) % 9973), "textures/item")
        count += 1


    # Spawn eggs reuse the vanilla egg model; give each a coloured overlay texture.
    # Keys are the spawn egg ITEM ids registered in CandyItems.java.
    egg_ids = {
        "candy_crawler": "candy_crawler_spawn_egg",
        "gummy_spawn": "gummy_spawn_spawn_egg",
        "gummy_brute": "gummy_brute_spawn_egg",
        "sugar_leech": "sugar_leech_spawn_egg",
        "candy_mimic": "candy_mimic_spawn_egg",
        "caramel_beast": "caramel_beast_spawn_egg",
        "jawbreaker": "jawbreaker_spawn_egg",
        "chocolate_creeper": "chocolate_creeper_spawn_egg",
        "lollipop_stalker": "lollipop_stalker_spawn_egg",
        "the_confectioner": "confectioner_spawn_egg",
    }
    for name, base, _accent in ENTITIES:
        save(egg_ids[name], 16, 16,
             item_shape_texture(16, base, "round", seed=hash(name) % 9973), "textures/item")
        count += 1

    for name, base, accent in ENTITIES:
        save(name, 64, 64, entity_texture(64, base, accent, seed=hash(name) % 9973), "textures/entity")
        count += 1

    particles = {
        "infection_spark": MAGENTA,
        "purification_spark": CYAN,
        "candy_dust": (255, 180, 220),
        "gummy_droplet": GREEN,
        "chocolate_fragment": LIGHT_BROWN,
        "sugar_sparkle": YELLOW,
        "caramel_bubble": ORANGE,
    }
    for name, colour in particles.items():
        save(name, 8, 8, particle_texture(8, colour), "textures/particle")
        count += 1

    save("candy_item", 16, 16, item_shape_texture(16, PINK, "round"), "textures/item")
    count += 1

    path = os.path.join(ROOT, "src", "main", "resources", "assets", "candyinfection", "icon.png")
    write_png(path, 128, 128, flatten(icon_texture(128), 128))
    count += 1

    print("wrote %d textures" % count)


if __name__ == "__main__":
    main()
