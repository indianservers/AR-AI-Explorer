from __future__ import annotations

import csv
import math
import re
import textwrap
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(r"C:\Indian Servers\Interactive Biology App")
INPUT_DIRS = [
    ROOT / "Image text Prompts" / "Biology" / "10_Evolution_and_Biodiversity",
    ROOT / "Image text Prompts" / "Biology" / "11_Biochemistry_and_Metabolism",
]
OUTPUT_ROOT = ROOT / "Generated Infographics" / "Biology"

CANVAS = (2560, 1440)
MARGIN = 72


@dataclass
class PromptDoc:
    source: Path
    unit: str
    number: str
    slug: str
    title: str
    subtitle: str
    hero: list[str]
    panels: list[str]
    process: list[str]
    facts: list[str]
    context: list[str]
    accuracy: list[str]


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
    ]
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    return ImageFont.load_default()


F_TITLE = font(82, True)
F_SUBTITLE = font(34)
F_SECTION = font(34, True)
F_BODY = font(27)
F_SMALL = font(22)
F_TINY = font(18)


def section_between(text: str, start: str, end_markers: list[str]) -> str:
    start_pat = re.escape(start)
    match = re.search(start_pat + r"\s*(.*)", text, re.S)
    if not match:
        return ""
    body = match.group(1)
    stops = [body.find(marker) for marker in end_markers if marker in body]
    if stops:
        body = body[: min(stops)]
    return body.strip()


def bullets(block: str, limit: int = 12) -> list[str]:
    items = []
    for line in block.splitlines():
        line = line.strip()
        if line.startswith("*"):
            item = line.lstrip("*").strip()
            if item:
                items.append(item)
    return items[:limit]


def clean_slug(name: str) -> str:
    return re.sub(r"[^A-Za-z0-9]+", "_", name).strip("_")


def parse_prompt(path: Path) -> PromptDoc:
    text = path.read_text(encoding="utf-8", errors="ignore")
    title = section_between(text, "# INFOGRAPHIC TITLE", ["# MAIN SUBTITLE"]).splitlines()[0].strip()
    subtitle = section_between(text, "# MAIN SUBTITLE", ["# OUTPUT QUALITY"]).splitlines()[0].strip()
    number = path.stem.split("_", 1)[0]
    unit = path.parent.name
    return PromptDoc(
        source=path,
        unit=unit,
        number=number,
        slug=clean_slug(path.stem),
        title=title or path.stem,
        subtitle=subtitle,
        hero=bullets(section_between(text, "# CENTRAL HERO ILLUSTRATION", ["# REQUIRED EDUCATIONAL PANELS"]), 10),
        panels=bullets(section_between(text, "# REQUIRED EDUCATIONAL PANELS", ["# PROCESS OR PATHWAY DIAGRAM"]), 12),
        process=bullets(section_between(text, "# PROCESS OR PATHWAY DIAGRAM", ["# KEY FACTS PANEL"]), 9),
        facts=bullets(section_between(text, "# KEY FACTS PANEL", ["# COMMON CONDITIONS", "# COMPOSITION"]), 8),
        context=bullets(section_between(text, "# COMMON CONDITIONS, APPLICATIONS OR CONTEXT", ["# COMPOSITION"]), 10),
        accuracy=bullets(section_between(text, "# SCIENTIFIC ACCURACY REQUIREMENTS", ["# PREMIUM NON-GENERIC", "# ADVANCED"]), 8),
    )


def gradient_background() -> Image.Image:
    width, height = CANVAS
    small = Image.new("RGB", (160, 90), "white")
    px = small.load()
    for y in range(90):
        for x in range(160):
            t = (x / 160 * 0.35) + (y / 90 * 0.65)
            r = int(244 - 26 * t)
            g = int(250 - 20 * t)
            b = int(255 - 8 * t)
            px[x, y] = (r, g, b)
    return small.resize((width, height), Image.Resampling.BICUBIC)


BASE_BACKGROUND = gradient_background()


def rounded(draw: ImageDraw.ImageDraw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def fit_text(draw: ImageDraw.ImageDraw, text: str, xy, max_width: int, fnt, fill, line_gap=8, max_lines=None):
    words = text.split()
    lines = []
    line = ""
    for word in words:
        trial = f"{line} {word}".strip()
        if draw.textlength(trial, font=fnt) <= max_width:
            line = trial
        else:
            if line:
                lines.append(line)
            line = word
    if line:
        lines.append(line)
    if max_lines and len(lines) > max_lines:
        lines = lines[:max_lines]
        while lines and draw.textlength(lines[-1] + "...", font=fnt) > max_width:
            lines[-1] = lines[-1][:-1]
        lines[-1] = lines[-1].rstrip() + "..."
    x, y = xy
    for line in lines:
        draw.text((x, y), line, font=fnt, fill=fill)
        y += fnt.size + line_gap
    return y


def title_block(draw: ImageDraw.ImageDraw, doc: PromptDoc, part_label: str):
    draw.text((MARGIN, 44), doc.title, font=F_TITLE, fill=(9, 32, 62))
    draw.text((MARGIN, 137), part_label.upper(), font=F_SECTION, fill=(0, 124, 157))
    fit_text(draw, doc.subtitle, (MARGIN, 184), 1500, F_SUBTITLE, (40, 66, 91), max_lines=2)
    rounded(draw, (1960, 52, 2488, 176), 34, (8, 45, 79), None)
    draw.text((2010, 84), "Interactive Biology", font=F_SECTION, fill=(255, 255, 255))


def draw_grid(draw: ImageDraw.ImageDraw):
    for x in range(0, CANVAS[0], 80):
        draw.line((x, 0, x, CANVAS[1]), fill=(204, 228, 238), width=1)
    for y in range(0, CANVAS[1], 80):
        draw.line((0, y, CANVAS[0], y), fill=(204, 228, 238), width=1)


def draw_cell(draw: ImageDraw.ImageDraw, cx, cy, r, fill, outline):
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=fill, outline=outline, width=4)
    draw.ellipse((cx - r * 0.35, cy - r * 0.35, cx + r * 0.35, cy + r * 0.35), fill=(255, 255, 255, 110), outline=outline, width=2)


def draw_dna(draw: ImageDraw.ImageDraw, x, y, w, h, color1=(34, 160, 206), color2=(125, 82, 210)):
    points_a = []
    points_b = []
    for i in range(80):
        t = i / 79
        yy = y + t * h
        amp = math.sin(t * math.pi * 6)
        points_a.append((x + w / 2 + amp * w * 0.28, yy))
        points_b.append((x + w / 2 - amp * w * 0.28, yy))
    draw.line(points_a, fill=color1, width=6)
    draw.line(points_b, fill=color2, width=6)
    for i in range(0, 80, 8):
        draw.line((points_a[i][0], points_a[i][1], points_b[i][0], points_b[i][1]), fill=(93, 117, 139), width=3)


def draw_molecule(draw: ImageDraw.ImageDraw, x, y, scale=1.0):
    atoms = [(0, 0, 38, (53, 140, 215)), (92, -46, 28, (39, 190, 160)), (174, 6, 34, (238, 155, 42)), (84, 70, 26, (143, 92, 221)), (-78, 56, 24, (230, 84, 93))]
    bonds = [(0, 1), (1, 2), (0, 3), (0, 4)]
    pts = [(x + ax * scale, y + ay * scale) for ax, ay, _, _ in atoms]
    for a, b in bonds:
        draw.line((*pts[a], *pts[b]), fill=(78, 96, 120), width=int(8 * scale))
    for (ax, ay, r, col) in atoms:
        cx, cy = x + ax * scale, y + ay * scale
        draw.ellipse((cx - r * scale, cy - r * scale, cx + r * scale, cy + r * scale), fill=col, outline=(255, 255, 255), width=4)


def draw_ecology_scene(draw: ImageDraw.ImageDraw, box):
    x1, y1, x2, y2 = box
    rounded(draw, box, 36, (232, 247, 245), (45, 132, 153), 3)
    draw.rectangle((x1, y2 - 150, x2, y2), fill=(211, 228, 199))
    for i in range(7):
        bx = x1 + 120 + i * 230
        by = y2 - 165 - (i % 3) * 28
        color = [(93, 107, 92), (140, 120, 84), (60, 83, 98)][i % 3]
        draw.ellipse((bx - 52, by - 30, bx + 62, by + 28), fill=color, outline=(35, 48, 52), width=2)
        draw.polygon([(bx + 52, by - 10), (bx + 95, by - 28), (bx + 72, by + 10)], fill=color)
        draw.ellipse((bx - 18, by - 12, bx - 5, by + 1), fill=(10, 20, 25))
    draw.line((x1 + 90, y1 + 95, x2 - 90, y1 + 95), fill=(122, 135, 151), width=6)
    for i in range(5):
        px = x1 + 160 + i * 355
        draw.polygon([(px, y1 + 95), (px - 18, y1 + 68), (px + 18, y1 + 68)], fill=(122, 135, 151))
        draw.text((px - 45, y1 + 28), f"Gen {i + 1}", font=F_SMALL, fill=(20, 48, 72))


def draw_biochem_scene(draw: ImageDraw.ImageDraw, box):
    x1, y1, x2, y2 = box
    rounded(draw, box, 36, (236, 246, 255), (45, 132, 153), 3)
    draw_molecule(draw, x1 + 360, y1 + 250, 1.7)
    draw_dna(draw, x2 - 520, y1 + 80, 360, 520)
    for i in range(8):
        draw_cell(draw, x1 + 170 + i * 145, y2 - 170 + (i % 2) * 25, 46, (180, 225, 243), (55, 132, 165))
    draw.arc((x1 + 920, y1 + 170, x1 + 1500, y1 + 650), 205, 520, fill=(227, 89, 77), width=18)
    draw.polygon([(x1 + 1460, y1 + 492), (x1 + 1518, y1 + 506), (x1 + 1470, y1 + 540)], fill=(227, 89, 77))


def card(draw, box, title, items, accent):
    rounded(draw, box, 28, (255, 255, 255), accent, 3)
    x1, y1, x2, _ = box
    rounded(draw, (x1, y1, x2, y1 + 72), 28, accent, None)
    draw.text((x1 + 28, y1 + 20), title.upper(), font=F_SECTION, fill=(255, 255, 255))
    y = y1 + 100
    for item in items[:6]:
        draw.ellipse((x1 + 30, y + 8, x1 + 48, y + 26), fill=accent)
        y = fit_text(draw, item, (x1 + 66, y), x2 - x1 - 96, F_BODY, (26, 47, 67), max_lines=2) + 8


def overview(doc: PromptDoc, out: Path):
    img = BASE_BACKGROUND.copy()
    draw = ImageDraw.Draw(img, "RGBA")
    draw_grid(draw)
    title_block(draw, doc, "Part 1 of 4 - Premier Overview")
    scene = (MARGIN, 290, 1700, 980)
    if "Evolution" in doc.unit:
        draw_ecology_scene(draw, scene)
    else:
        draw_biochem_scene(draw, scene)
    card(draw, (1760, 290, 2488, 640), "Core labels", doc.hero or doc.panels, (0, 126, 169))
    card(draw, (1760, 690, 2488, 1040), "Learning focus", doc.panels[:6], (111, 76, 191))
    card(draw, (MARGIN, 1070, 2488, 1350), "High-yield facts", (doc.accuracy or doc.context or doc.panels)[:7], (12, 84, 129))
    img.save(out)


def mechanisms(doc: PromptDoc, out: Path):
    img = BASE_BACKGROUND.copy()
    draw = ImageDraw.Draw(img, "RGBA")
    draw_grid(draw)
    title_block(draw, doc, "Part 2 of 4 - Mechanisms and Structure")
    card(draw, (MARGIN, 300, 790, 1260), "Mechanism map", doc.panels[:8], (16, 122, 150))
    card(draw, (900, 300, 1620, 1260), "Structures to label", doc.hero[:8], (222, 143, 34))
    card(draw, (1730, 300, 2488, 1260), "Accuracy checks", (doc.accuracy or doc.context or doc.facts)[:8], (146, 80, 198))
    if "Evolution" in doc.unit:
        draw_dna(draw, 1080, 820, 350, 360)
    else:
        draw_molecule(draw, 1260, 880, 2.0)
    img.save(out)


def process(doc: PromptDoc, out: Path):
    img = BASE_BACKGROUND.copy()
    draw = ImageDraw.Draw(img, "RGBA")
    draw_grid(draw)
    title_block(draw, doc, "Part 3 of 4 - Step-by-Step Process")
    steps = doc.process or doc.panels[:6]
    start_x, start_y = 130, 360
    step_w, step_h = 680, 250
    for idx, item in enumerate(steps[:8]):
        col = idx % 3
        row = idx // 3
        x = start_x + col * 810
        y = start_y + row * 330
        accent = [(0, 126, 169), (222, 143, 34), (111, 76, 191), (20, 151, 126)][idx % 4]
        rounded(draw, (x, y, x + step_w, y + step_h), 30, (255, 255, 255), accent, 4)
        draw.ellipse((x + 30, y + 28, x + 96, y + 94), fill=accent)
        draw.text((x + 52, y + 42), str(idx + 1), font=F_SECTION, fill=(255, 255, 255))
        fit_text(draw, item, (x + 122, y + 34), step_w - 160, F_BODY, (23, 43, 62), max_lines=4)
        if col < 2 and idx + 1 < len(steps):
            draw.line((x + step_w + 24, y + 125, x + step_w + 106, y + 125), fill=(80, 102, 123), width=8)
            draw.polygon([(x + step_w + 106, y + 125), (x + step_w + 82, y + 104), (x + step_w + 82, y + 146)], fill=(80, 102, 123))
    card(draw, (MARGIN, 1260, 2488, 1370), "Takeaway", [doc.subtitle], (8, 45, 79))
    img.save(out)


def applications(doc: PromptDoc, out: Path):
    img = BASE_BACKGROUND.copy()
    draw = ImageDraw.Draw(img, "RGBA")
    draw_grid(draw)
    title_block(draw, doc, "Part 4 of 4 - Applications, Facts and Review")
    card(draw, (MARGIN, 300, 815, 1260), "Applications", (doc.context or doc.panels)[:8], (0, 126, 169))
    card(draw, (900, 300, 1655, 1260), "Key facts", (doc.facts or doc.accuracy or doc.panels)[:8], (16, 122, 150))
    card(draw, (1740, 300, 2488, 1260), "Exam review", (doc.accuracy or doc.panels or doc.hero)[:8], (222, 143, 34))
    img.save(out)


def render_all():
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    manifest_rows = []
    renderers = [
        ("01_overview", overview),
        ("02_mechanisms", mechanisms),
        ("03_process", process),
        ("04_applications", applications),
    ]
    for input_dir in INPUT_DIRS:
        for prompt_path in sorted(input_dir.glob("*.txt")):
            doc = parse_prompt(prompt_path)
            target_dir = OUTPUT_ROOT / doc.unit / doc.slug
            target_dir.mkdir(parents=True, exist_ok=True)
            for part, renderer in renderers:
                filename = f"{doc.slug}_{part}.png"
                out = target_dir / filename
                if out.exists() and out.stat().st_size > 100_000:
                    manifest_rows.append(
                        {
                            "source_prompt": str(prompt_path),
                            "title": doc.title,
                            "part": part,
                            "output": str(out),
                        }
                    )
                    continue
                renderer(doc, out)
                manifest_rows.append(
                    {
                        "source_prompt": str(prompt_path),
                        "title": doc.title,
                        "part": part,
                        "output": str(out),
                    }
                )
    manifest = OUTPUT_ROOT / "biology_infographics_manifest.csv"
    with manifest.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["source_prompt", "title", "part", "output"])
        writer.writeheader()
        writer.writerows(manifest_rows)
    print(f"Rendered {len(manifest_rows)} infographic PNG files")
    print(manifest)


if __name__ == "__main__":
    render_all()
