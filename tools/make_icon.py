#!/usr/bin/env python3
"""Genera el icono de MGGX Laberinto: espiral de laberinto ambar sobre roca de cueva."""
from PIL import Image, ImageDraw, ImageFilter
import math, os, random

OUT = "app/src/main/res"
DENS = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
SS = 8  # supersampling

ROCK_DARK = (18, 15, 13)
ROCK_MID  = (46, 38, 31)
ROCK_HI   = (74, 60, 47)
AMBER     = (255, 168, 64)
AMBER_HI  = (255, 226, 158)
GLOW      = (255, 122, 30)


def spiral_segments():
    """Segmentos (x0,y0,x1,y1) de un laberinto en espiral, en coords 0..1."""
    segs = []
    # Espiral cuadrada de 4 vueltas con una boca de entrada abajo.
    steps = [
        (0.500, 0.500, 0.500, 0.365),
        (0.500, 0.365, 0.365, 0.365),
        (0.365, 0.365, 0.365, 0.635),
        (0.365, 0.635, 0.635, 0.635),
        (0.635, 0.635, 0.635, 0.250),
        (0.635, 0.250, 0.250, 0.250),
        (0.250, 0.250, 0.250, 0.750),
        (0.250, 0.750, 0.750, 0.750),
        (0.750, 0.750, 0.750, 0.135),
        (0.750, 0.135, 0.135, 0.135),
        (0.135, 0.135, 0.135, 0.865),
        (0.135, 0.865, 0.560, 0.865),
    ]
    segs.extend(steps)
    return segs


def draw_rock(d, S, rnd):
    d.rectangle([0, 0, S, S], fill=ROCK_DARK)
    # Facetas de roca
    for _ in range(26):
        cx, cy = rnd.uniform(0, S), rnd.uniform(0, S)
        r = rnd.uniform(S * 0.12, S * 0.34)
        pts = []
        n = rnd.randint(5, 7)
        for i in range(n):
            a = i * 2 * math.pi / n + rnd.uniform(-0.25, 0.25)
            rr = r * rnd.uniform(0.65, 1.0)
            pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
        t = rnd.random()
        col = tuple(int(ROCK_DARK[i] + (ROCK_MID[i] - ROCK_DARK[i]) * t) for i in range(3))
        d.polygon(pts, fill=col)
    # Brillos de arista
    for _ in range(18):
        x0, y0 = rnd.uniform(0, S), rnd.uniform(0, S)
        x1 = x0 + rnd.uniform(-S * 0.2, S * 0.2)
        y1 = y0 + rnd.uniform(-S * 0.2, S * 0.2)
        d.line([x0, y0, x1, y1], fill=ROCK_HI, width=max(1, int(S * 0.006)))


def build(size, rounded, adaptive_pad=0.0):
    S = size * SS
    rnd = random.Random(20260904)
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_rock(d, S, rnd)

    # Vineta
    vig = Image.new("L", (S, S), 0)
    vd = ImageDraw.Draw(vig)
    vd.ellipse([-S * 0.15, -S * 0.15, S * 1.15, S * 1.15], fill=255)
    vig = vig.filter(ImageFilter.GaussianBlur(S * 0.10))
    dark = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    img = Image.composite(img, dark, vig)

    # Resplandor detras del laberinto
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    segs = spiral_segments()
    w_glow = int(S * 0.085)
    for (x0, y0, x1, y1) in segs:
        gd.line([x0 * S, y0 * S, x1 * S, y1 * S], fill=GLOW + (170,), width=w_glow)
        gd.ellipse([x0 * S - w_glow / 2, y0 * S - w_glow / 2, x0 * S + w_glow / 2, y0 * S + w_glow / 2], fill=GLOW + (170,))
    glow = glow.filter(ImageFilter.GaussianBlur(S * 0.035))
    img = Image.alpha_composite(img, glow)

    # Trazo del laberinto
    lay = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lay)
    w = int(S * 0.042)
    for (x0, y0, x1, y1) in segs:
        ld.line([x0 * S, y0 * S, x1 * S, y1 * S], fill=AMBER + (255,), width=w)
        ld.ellipse([x0 * S - w / 2, y0 * S - w / 2, x0 * S + w / 2, y0 * S + w / 2], fill=AMBER + (255,))
        ld.ellipse([x1 * S - w / 2, y1 * S - w / 2, x1 * S + w / 2, y1 * S + w / 2], fill=AMBER + (255,))
    # Nucleo brillante
    core = int(S * 0.020)
    ld.ellipse([0.5 * S - core * 2.4, 0.5 * S - core * 2.4, 0.5 * S + core * 2.4, 0.5 * S + core * 2.4], fill=AMBER_HI + (255,))
    img = Image.alpha_composite(img, lay)

    if rounded:
        mask = Image.new("L", (S, S), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, S, S], fill=255)
        img.putalpha(mask)
    else:
        mask = Image.new("L", (S, S), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, S, S], radius=int(S * 0.19), fill=255)
        img.putalpha(mask)

    return img.resize((size, size), Image.LANCZOS)


def build_adaptive_layer(size, foreground):
    """Capas 108x108dp; el contenido seguro es el 66% central."""
    S = size * SS
    rnd = random.Random(20260904)
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if not foreground:
        draw_rock(d, S, rnd)
        return img.resize((size, size), Image.LANCZOS)

    segs = spiral_segments()
    # El glifo ocupa el 62% central para respetar el recorte adaptativo.
    def mp(v):
        return (0.5 + (v - 0.5) * 0.62) * S

    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    wg = int(S * 0.055)
    for (x0, y0, x1, y1) in segs:
        gd.line([mp(x0), mp(y0), mp(x1), mp(y1)], fill=GLOW + (180,), width=wg)
    glow = glow.filter(ImageFilter.GaussianBlur(S * 0.022))
    img = Image.alpha_composite(img, glow)

    lay = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lay)
    w = int(S * 0.027)
    for (x0, y0, x1, y1) in segs:
        ld.line([mp(x0), mp(y0), mp(x1), mp(y1)], fill=AMBER + (255,), width=w)
        ld.ellipse([mp(x0) - w / 2, mp(y0) - w / 2, mp(x0) + w / 2, mp(y0) + w / 2], fill=AMBER + (255,))
        ld.ellipse([mp(x1) - w / 2, mp(y1) - w / 2, mp(x1) + w / 2, mp(y1) + w / 2], fill=AMBER + (255,))
    c = int(S * 0.014)
    ld.ellipse([mp(0.5) - c * 2.2, mp(0.5) - c * 2.2, mp(0.5) + c * 2.2, mp(0.5) + c * 2.2], fill=AMBER_HI + (255,))
    img = Image.alpha_composite(img, lay)
    return img.resize((size, size), Image.LANCZOS)


for dens, px in DENS.items():
    p = os.path.join(OUT, "mipmap-" + dens)
    os.makedirs(p, exist_ok=True)
    build(px, False).save(os.path.join(p, "ic_launcher.png"))
    build(px, True).save(os.path.join(p, "ic_launcher_round.png"))
    ap = int(px * 108 / 48)
    build_adaptive_layer(ap, False).save(os.path.join(p, "ic_launcher_back.png"))
    build_adaptive_layer(ap, True).save(os.path.join(p, "ic_launcher_fore.png"))
    print(dens, px, "ok")

# Icono grande para la ficha de la release
build(512, False).save("tools/icon-512.png")
print("listo")
