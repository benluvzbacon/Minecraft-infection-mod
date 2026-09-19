#!/usr/bin/env python3
"""Render an isometric block preview from the mod's real textures.

This is NOT a gameplay screenshot - nothing here touches Minecraft. It decodes
the actual PNGs that ship in the jar and draws them onto isometric cube faces
with per-face shading, so the README shows what the candy blocks genuinely look
like rather than a fabricated in-game capture.

Run from the repository root:

    python3 tools/render_preview.py
"""

import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "candyinfection",
                   "textures", "block")
OUT = os.path.join(ROOT, "docs", "preview.png")


# ------------------------------------------------------------------ png decode

def read_png(path):
    with open(path, "rb") as handle:
        data = handle.read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    pos = 8
    idat = b""
    width = height = None
    bitdepth = colortype = None
    while pos < len(data):
        length = int.from_bytes(data[pos:pos + 4], "big")
        kind = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            width, height, bitdepth, colortype = struct.unpack(">IIBB", chunk[:10])
        elif kind == b"IDAT":
            idat += chunk
        elif kind == b"IEND":
            break
        pos += 12 + length
    assert bitdepth == 8 and colortype == 6, "expected 8-bit RGBA in %s" % path
    raw = zlib.decompress(idat)
    bpp = 4
    stride = width * bpp
    pixels = bytearray(height * stride)
    prev = bytearray(stride)
    i = 0
    for y in range(height):
        filt = raw[i]
        i += 1
        line = bytearray(raw[i:i + stride])
        i += stride
        if filt == 1:
            for x in range(bpp, stride):
                line[x] = (line[x] + line[x - bpp]) & 0xFF
        elif filt == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 0xFF
        elif filt == 3:
            for x in range(stride):
                left = line[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + ((left + prev[x]) >> 1)) & 0xFF
        elif filt == 4:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x - bpp] if x >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pred = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pred) & 0xFF
        elif filt != 0:
            raise ValueError("unknown filter %d in %s" % (filt, path))
        pixels[y * stride:(y + 1) * stride] = line
        prev = line
    return width, height, pixels


class Texture(object):
    def __init__(self, name):
        self.width, self.height, self.data = read_png(os.path.join(TEX, name + ".png"))

    def sample(self, u, v):
        x = int(u * self.width) % self.width
        y = int(v * self.height) % self.height
        i = (y * self.width + x) * 4
        return (self.data[i], self.data[i + 1], self.data[i + 2], self.data[i + 3])


# ------------------------------------------------------------------- png encode

def write_png(path, width, height, rows):
    def chunk(tag, payload):
        body = tag + payload
        return (struct.pack(">I", len(payload)) + body
                + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF))

    raw = b"".join(b"\x00" + row for row in rows)
    out = b"\x89PNG\r\n\x1a\n"
    out += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    out += chunk(b"IDAT", zlib.compress(raw, 9))
    out += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(out)


# ------------------------------------------------------------------- iso render

SCALE = 6          # pixels per texture pixel along a cube edge
TOP, LEFT, RIGHT = 1.0, 0.62, 0.78


class Canvas(object):
    def __init__(self, width, height, background):
        self.width = width
        self.height = height
        self.rows = [bytearray(background) * width for _ in range(height)]

    def put(self, x, y, rgba):
        if 0 <= x < self.width and 0 <= y < self.height:
            i = x * 4
            row = self.rows[y]
            row[i] = rgba[0]
            row[i + 1] = rgba[1]
            row[i + 2] = rgba[2]
            row[i + 3] = 255


def draw_cube(canvas, cx, cy, texture, alpha_skip=True):
    """Draw an isometric cube centred on (cx, cy) using one texture.

    Each face is an affine map of the texture, so the real pixel art is what you
    see - just projected and shaded the way Minecraft shades a cube.
    """
    w = texture.width * SCALE
    h = w // 2
    faces = [
        # (origin, u vector, v vector, shade)
        ((cx, cy - h), (w, h), (-w, h), TOP),      # top
        ((cx - w, cy), (w, h), (0, w), LEFT),      # left
        ((cx, cy + h), (w, -h), (0, w), RIGHT),    # right
    ]
    for origin, uvec, vvec, shade in faces:
        det = uvec[0] * vvec[1] - uvec[1] * vvec[0]
        if det == 0:
            continue
        xs = [origin[0], origin[0] + uvec[0], origin[0] + vvec[0], origin[0] + uvec[0] + vvec[0]]
        ys = [origin[1], origin[1] + uvec[1], origin[1] + vvec[1], origin[1] + uvec[1] + vvec[1]]
        for py in range(int(min(ys)) - 1, int(max(ys)) + 2):
            for px in range(int(min(xs)) - 1, int(max(xs)) + 2):
                dx = px - origin[0]
                dy = py - origin[1]
                u = (dx * vvec[1] - dy * vvec[0]) / det
                v = (uvec[0] * dy - uvec[1] * dx) / det
                if not (0.0 <= u < 1.0 and 0.0 <= v < 1.0):
                    continue
                r, g, b, a = texture.sample(u, v)
                if alpha_skip and a < 200:
                    continue
                canvas.put(px, py, (int(r * shade), int(g * shade), int(b * shade), 255))


def iso_origin(gx, gy, gz, texture_w, origin_x, origin_y):
    """Screen position for a cube at grid (gx, gy, gz); y is up."""
    step = texture_w * SCALE
    cx = origin_x + (gx - gz) * step
    cy = origin_y + (gx + gz) * (step // 2) - gy * step
    return cx, cy


def main():
    # The scene: an infected grass field with candy stone, a gummy tree, a
    # crystal cluster, hard candy and a lollipop. Every block here is a real
    # registered block from CandyBlocks.java, drawn from its shipped texture.
    scene = []

    ground = [
        ["infected_grass_block", "infected_grass_block", "candy_stone", "infected_grass_block"],
        ["infected_grass_block", "infected_grass_block", "infected_grass_block", "candy_sand"],
        ["candy_stone", "infected_grass_block", "infected_grass_block", "infected_grass_block"],
        ["infected_grass_block", "candy_sand", "infected_grass_block", "infected_grass_block"],
    ]
    for gx, row in enumerate(ground):
        for gz, name in enumerate(row):
            scene.append((gx, 0, gz, name))

    # gummy tree: three logs and a leaf cap
    for gy in range(1, 4):
        scene.append((1, gy, 1, "gummy_log"))
    for dx in (0, 1, 2):
        for dz in (0, 1, 2):
            if (dx, dz) != (1, 1):
                scene.append((dx, 4, dz, "gummy_leaves"))
    scene.append((1, 5, 1, "gummy_leaves"))

    # a sugar crystal spire and some decoration
    scene.append((3, 1, 0, "sugar_crystal_block"))
    scene.append((3, 2, 0, "sugar_crystal_block"))
    scene.append((3, 3, 0, "sugar_crystal_cluster"))
    scene.append((0, 1, 3, "hard_candy_pink"))
    scene.append((0, 1, 2, "hard_candy_cyan"))
    scene.append((2, 1, 3, "lollipop_red"))
    scene.append((3, 1, 2, "caramel_growth"))

    cache = {}

    def tex(name):
        if name not in cache:
            cache[name] = Texture(name)
        return cache[name]

    texw = tex("infected_grass_block").width
    step = texw * SCALE
    width = step * 9
    height = step * 8
    sky = []
    for y in range(height):
        t = y / float(height)
        sky.append((int(255 - 40 * t), int(150 + 60 * t), int(210 + 30 * t), 255))

    canvas = Canvas(width, height, (0, 0, 0, 0))
    for y in range(height):
        row = canvas.rows[y]
        for x in range(width):
            i = x * 4
            row[i] = sky[y][0]
            row[i + 1] = sky[y][1]
            row[i + 2] = sky[y][2]
            row[i + 3] = 255

    origin_x = width // 2
    origin_y = int(height * 0.30)

    # painter's algorithm: far to near, bottom to top
    for gx, gy, gz, name in sorted(scene, key=lambda b: (b[0] + b[2], b[1])):
        cx, cy = iso_origin(gx, gy, gz, texw, origin_x, origin_y)
        draw_cube(canvas, cx, cy, tex(name))

    write_png(OUT, width, height, [bytes(r) for r in canvas.rows])
    print("wrote %s (%dx%d, %d blocks drawn from %d real textures)"
          % (os.path.relpath(OUT, ROOT), width, height, len(scene), len(cache)))


if __name__ == "__main__":
    main()
