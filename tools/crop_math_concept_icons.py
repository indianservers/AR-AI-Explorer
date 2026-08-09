from pathlib import Path

from PIL import Image


SOURCE = Path(r"C:\Users\saisa\Downloads\ChatGPT Image Aug 9, 2026, 08_57_01 AM.png")
OUTPUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "drawable-nodpi"

NAMES = [
    "abstract_advanced_algebra",
    "algebra",
    "applied_interdisciplinary_mathematics",
    "arithmetic",
    "calculus",
    "complex_analysis",
    "coordinate_geometry",
    "data_handling_patterns",
    "differential_equations_pde",
    "differential_geometry",
    "discrete_mathematics",
    "dynamical_systems_control",
    "financial_commercial_mathematics",
    "fractions_decimals_ratio_percent",
    "geometry",
    "linear_algebra",
    "logic_foundations",
    "measurement",
    "mensuration",
    "multivariable_vector_calculus",
    "number_theory",
    "numbers_number_systems",
    "numerical_analysis",
    "optimization_operations_research",
    "probability_statistics",
    "real_functional_analysis",
    "sets_relations_functions",
    "topology_geometry",
    "trigonometry",
]


def main():
    image = Image.open(SOURCE).convert("RGBA")
    OUTPUT.mkdir(parents=True, exist_ok=True)

    cols = [120, 291, 462, 633, 804, 975, 1146]
    rows = [154, 392, 635, 865]
    centers = [(x, rows[0]) for x in cols]
    centers += [(x, rows[1]) for x in cols]
    centers += [(x, rows[2]) for x in cols]
    centers += [(x, rows[3]) for x in cols]
    centers += [(620, 1083)]

    for name, (cx, cy) in zip(NAMES, centers):
        crop_size = 176 if name == "trigonometry" else 166
        half = crop_size // 2
        crop = image.crop((cx - half, cy - half, cx + half, cy + half))
        crop.resize((192, 192), Image.Resampling.LANCZOS).save(OUTPUT / f"math_concept_{name}.png")

    print(f"cropped={len(NAMES)}")


if __name__ == "__main__":
    main()
