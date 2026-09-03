from __future__ import annotations

import csv
import math
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont, ImageStat


ROOT = Path(__file__).resolve().parents[1]
TARGET_DIR = Path.home() / "Downloads" / "maths_visual_proofs_android_mockups_69" / "visual_proofs_android_mockups"
ARTIFACTS = ROOT / "artifacts"
OUTPUT = ARTIFACTS / "visual_proof_pair_audit"
FINAL_ACTUAL_DIR = ARTIFACTS / "final_visual_proof_ordered"
KNOWN_GOOD_ACTUALS = {
    "002": "002_pythagorean_actual.png",
    "008": "008_similar_actual.png",
    "060": "060_addition_actual.png",
}


def actual_score(path: Path) -> tuple[int, str]:
    name = path.stem.lower()
    score = 0
    if "actual" in name:
        score += 50
    if "actual_v2" in name or "fixed_actual" in name:
        score += 30
    if any(word in name for word in ("filter", "search", "catalog", "list", "nav", "routed")):
        score -= 80
    return score, name


def fit(image: Image.Image, width: int, height: int) -> Image.Image:
    image = image.convert("RGB")
    scale = min(width / image.width, height / image.height)
    resized = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.LANCZOS)
    result = Image.new("RGB", (width, height), "white")
    result.paste(resized, ((width - resized.width) // 2, (height - resized.height) // 2))
    return result


def normalized_rms(left: Image.Image, right: Image.Image) -> float:
    left = fit(left, 180, 320)
    right = fit(right, 180, 320)
    diff = ImageChops.difference(left, right)
    values = ImageStat.Stat(diff).rms
    return math.sqrt(sum(value * value for value in values) / len(values)) / 255.0


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    rows: list[dict[str, str | float]] = []
    cards: list[Image.Image] = []
    font = ImageFont.load_default(size=18)
    for target in sorted(TARGET_DIR.glob("*.png")):
        number = target.stem[:3]
        candidates = sorted(ARTIFACTS.glob(f"{number}*.png"), key=actual_score, reverse=True)
        final_actual = FINAL_ACTUAL_DIR / f"{number}_final.png" if int(number) >= 59 else None
        preferred = final_actual if final_actual and final_actual.exists() else (ARTIFACTS / KNOWN_GOOD_ACTUALS[number] if number in KNOWN_GOOD_ACTUALS else None)
        actual = preferred if preferred and preferred.exists() else (candidates[0] if candidates else None)
        target_image = Image.open(target)
        actual_image = Image.open(actual) if actual else Image.new("RGB", target_image.size, "white")
        target_fit = fit(target_image, 250, 445)
        actual_fit = fit(actual_image, 250, 445)
        card = Image.new("RGB", (520, 485), "white")
        card.paste(target_fit, (5, 35))
        card.paste(actual_fit, (265, 35))
        draw = ImageDraw.Draw(card)
        draw.text((8, 8), f"{number} TARGET", fill="black", font=font)
        draw.text((268, 8), f"ACTUAL {actual.name if actual else 'MISSING'}", fill="black", font=font)
        cards.append(card)
        rows.append({"number": number, "target": target.name, "actual": actual.name if actual else "", "normalized_rms": round(normalized_rms(target_image, actual_image), 4)})

    for sheet_index in range(0, len(cards), 6):
        chunk = cards[sheet_index : sheet_index + 6]
        sheet = Image.new("RGB", (1040, 1455), "#dddddd")
        for index, card in enumerate(chunk):
            sheet.paste(card, ((index % 2) * 520, (index // 2) * 485))
        first = sheet_index + 1
        last = sheet_index + len(chunk)
        sheet.save(OUTPUT / f"pairs_{first:02d}_{last:02d}.jpg", quality=90)

    with (OUTPUT / "pair_metrics.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["number", "target", "actual", "normalized_rms"])
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    main()
