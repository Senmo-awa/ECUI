"""Generate the ECUI mod icon: chunky pixel-font "ECUI", white on saturated blue."""
from PIL import Image, ImageFilter
import sys

GLYPHS = {
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
}

BG = (11, 93, 255, 255)          # saturated blue
INK = (255, 255, 255, 255)       # white letters
SHADOW = (6, 38, 110, 255)       # navy pixel shadow
EDGE = (3, 20, 64, 255)          # darkest edge


def build_mask(size, text="ECUI", spacing=2, margin_ratio=0.06):
    gw, gh = 5, 7
    total_w = len(text) * gw + (len(text) - 1) * spacing
    margin = max(1, int(size * margin_ratio))
    scale = (size - 2 * margin) // total_w
    w, h = total_w * scale, gh * scale
    mask = Image.new("L", (size, size), 0)
    px = mask.load()
    ox, oy = (size - w) // 2, (size - h) // 2
    for i, ch in enumerate(text):
        for gy in range(gh):
            for gx in range(gw):
                if GLYPHS[ch][gy][gx] == "1":
                    X = ox + (gx + i * (gw + spacing)) * scale
                    Y = oy + gy * scale
                    for yy in range(scale):
                        for xx in range(scale):
                            px[X + xx, Y + yy] = 255
    return mask


def draw_icon(size=512, spacing=2, margin_ratio=0.06, outline_px=0,
              shadow_px=0, bg=BG, shadow=SHADOW, edge=EDGE):
    mask = build_mask(size, spacing=spacing, margin_ratio=margin_ratio)
    img = Image.new("RGBA", (size, size), bg)

    if shadow_px:
        sh = Image.new("L", (size, size), 0)
        sh.paste(mask, (shadow_px, shadow_px))
        img.paste(shadow, (0, 0), sh)

    if outline_px:
        grown = mask.filter(ImageFilter.MaxFilter(outline_px * 2 + 1))
        ring = Image.composite(grown, Image.new("L", (size, size), 0), mask.point(lambda v: 255 - v))
        img.paste(edge, (0, 0), ring)

    img.paste(INK, (0, 0), mask)
    return img


VARIANTS = {
    # flat white, generous spacing
    "a": dict(spacing=2, margin_ratio=0.05),
    # larger flat white
    "b": dict(spacing=1, margin_ratio=0.03),
    # thin navy outline
    "c": dict(spacing=2, margin_ratio=0.055, outline_px=6),
    # outline + soft shadow
    "d": dict(spacing=2, margin_ratio=0.05, outline_px=5, shadow_px=10),
}

if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    for name, kw in VARIANTS.items():
        draw_icon(512, **kw).save(f"{out}/icon-{name}.png")
    print("wrote", out)
