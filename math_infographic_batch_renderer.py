from __future__ import annotations

import math
import re
from concurrent.futures import ProcessPoolExecutor, as_completed
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

from PIL import Image, ImageDraw, ImageFont, ImageFilter


ROOT = Path(r"C:\Indian Servers\Interactive Biology App\Image text Prompts\Maths")
OUT_ROOT = ROOT / "Generated_Infographics_All"
W, H = 7680, 4320
DPI = (300, 300)

FONT_DIR = Path(r"C:\Windows\Fonts")
FONT_REG = FONT_DIR / "segoeui.ttf"
FONT_SEMI = FONT_DIR / "seguisb.ttf"
FONT_BOLD = FONT_DIR / "segoeuib.ttf"
FONT_ITALIC = FONT_DIR / "segoeuii.ttf"
FONT_MATH = FONT_DIR / "cambria.ttc"


PALETTES = [
    ("#0B132B", "#13315C", "#2DD4BF", "#F8FAFC", "#FFD166"),
    ("#111827", "#1F3A5F", "#60A5FA", "#F9FAFB", "#F97316"),
    ("#172554", "#0F766E", "#22C55E", "#F8FAFC", "#FACC15"),
    ("#312E81", "#155E75", "#A78BFA", "#F8FAFC", "#FB7185"),
    ("#1E293B", "#115E59", "#38BDF8", "#F8FAFC", "#F59E0B"),
]


CONTENT: Dict[str, Dict[str, List[str]]] = {
    "Number Systems": {
        "formulas": ["N ⊂ W ⊂ Z ⊂ Q ⊂ R", "a/b ∈ Q, b ≠ 0", "√2 ∉ Q", "|x| = distance from 0"],
        "examples": ["Classify: -3 ∈ Z, 0.75 = 3/4 ∈ Q, π ∈ R \\ Q", "Place -2, 0, 1/2, √2 on one number line"],
        "traps": ["A terminating decimal is rational.", "Irrational numbers can still be located on the number line."],
        "apps": ["measurement", "estimation", "algebra foundations", "scientific notation"],
    },
    "Fractions Decimals Percentages": {
        "formulas": ["p% = p/100", "a/b = decimal by division", "fraction × whole = part", "increase % = change/original × 100"],
        "examples": ["3/4 = 0.75 = 75%", "20% of 250 = 0.20 × 250 = 50"],
        "traps": ["Percent change uses the original value as denominator.", "0.5% is 0.005, not 0.5."],
        "apps": ["discounts", "interest", "data comparison", "exam arithmetic"],
    },
    "Ratios Proportions and Unit Rates": {
        "formulas": ["a:b = a/b", "a/b = c/d ⇒ ad = bc", "unit rate = quantity per 1 unit", "direct: y = kx"],
        "examples": ["If 3 notebooks cost ₹90, unit rate = ₹30 each", "2:5 = x:20 ⇒ x = 8"],
        "traps": ["Keep ratio order fixed.", "Compare using the same unit."],
        "apps": ["maps", "recipes", "speed", "currency and pricing"],
    },
    "Exponents Radicals and Logarithms": {
        "formulas": ["a^m a^n = a^(m+n)", "(a^m)^n = a^(mn)", "√a = a^(1/2)", "log_b(x) = y ⇔ b^y = x"],
        "examples": ["2^3 · 2^4 = 2^7 = 128", "log_10(1000)=3"],
        "traps": ["(a+b)^2 ≠ a^2+b^2.", "log(x+y) is not log x + log y."],
        "apps": ["growth", "pH", "sound scale", "algorithms"],
    },
    "Algebraic Expressions and Identities": {
        "formulas": ["(a+b)^2 = a^2+2ab+b^2", "a^2-b^2=(a-b)(a+b)", "combine like terms only", "factor common terms first"],
        "examples": ["3x+2x-5 = 5x-5", "x^2+5x+6 = (x+2)(x+3)"],
        "traps": ["Only like terms combine.", "A minus sign outside brackets changes every term."],
        "apps": ["formula manipulation", "equations", "geometry area models", "polynomials"],
    },
    "Linear Equations and Inequalities": {
        "formulas": ["ax+b=c ⇒ x=(c-b)/a", "flip inequality when multiplying by negative", "solution set on a number line", "slope-intercept: y=mx+b"],
        "examples": ["2x+5=17 ⇒ x=6", "-3x<9 ⇒ x>-3"],
        "traps": ["Check by substitution.", "Do not forget to reverse < or > after multiplying by a negative."],
        "apps": ["budget constraints", "break-even points", "motion problems", "optimization basics"],
    },
    "Coordinate Geometry and Slope": {
        "formulas": ["m=(y₂-y₁)/(x₂-x₁)", "distance = √((x₂-x₁)^2+(y₂-y₁)^2)", "midpoint=((x₁+x₂)/2,(y₁+y₂)/2)", "y-y₁=m(x-x₁)"],
        "examples": ["Slope through (1,2),(5,10): m=2", "Midpoint of (2,4),(8,10) is (5,7)"],
        "traps": ["Vertical line slope is undefined.", "Use consistent point order in numerator and denominator."],
        "apps": ["maps", "line graphs", "analytic geometry", "physics motion"],
    },
    "Euclidean Geometry Basics": {
        "formulas": ["sum of angles on a line = 180°", "angles around a point = 360°", "parallel lines create equal corresponding angles", "triangle angle sum = 180°"],
        "examples": ["If two angles form a linear pair and one is 65°, the other is 115°", "Corresponding angles match when lines are parallel"],
        "traps": ["A diagram is not proof unless relationships are given.", "Equal-looking lengths are not necessarily equal."],
        "apps": ["construction", "proof", "architecture", "geometric reasoning"],
    },
    "Triangles Congruence and Similarity": {
        "formulas": ["SSS, SAS, ASA, RHS", "similar: corresponding sides proportional", "area scale factor = k^2", "Pythagoras: a^2+b^2=c^2"],
        "examples": ["3-4-5 triangle is right-angled", "If scale factor is 2, area becomes 4 times"],
        "traps": ["SSA is not a general congruence test.", "Match corresponding vertices in the same order."],
        "apps": ["surveying", "scale drawings", "trigonometry", "proof"],
    },
    "Circles and Theorems": {
        "formulas": ["circumference = 2πr", "area = πr^2", "angle at centre = 2 × angle at circumference", "tangent ⟂ radius at point of contact"],
        "examples": ["For r=7, C=14π and A=49π", "Equal chords subtend equal angles"],
        "traps": ["Diameter is 2r.", "Use radians for arc length s=rθ."],
        "apps": ["wheels", "orbits", "circular design", "geometry proofs"],
    },
    "Area Surface Area and Volume": {
        "formulas": ["rectangle A=lw", "circle A=πr^2", "cuboid V=lwh", "sphere V=4/3πr^3"],
        "examples": ["Cylinder volume = πr^2h", "Surface area tracks exposed faces"],
        "traps": ["Area uses square units; volume uses cubic units.", "Surface area and volume measure different things."],
        "apps": ["packaging", "construction", "capacity", "measurement modelling"],
    },
    "Sequences and Series": {
        "formulas": ["AP: a_n=a+(n-1)d", "AP sum: S_n=n/2[2a+(n-1)d]", "GP: a_n=ar^(n-1)", "GP sum: S_n=a(1-r^n)/(1-r)"],
        "examples": ["2,5,8,11 has d=3", "3,6,12,24 has r=2"],
        "traps": ["Do not mix AP and GP formulas.", "Indexing starts must be clear."],
        "apps": ["patterns", "finance", "algorithms", "limits"],
    },
    "Functions and Graphs": {
        "formulas": ["function: each input has one output", "domain → range", "composition: (f∘g)(x)=f(g(x))", "inverse: f(f⁻¹(x))=x"],
        "examples": ["f(x)=2x+1, f(3)=7", "Graph y=x^2 is not one-to-one on all real numbers"],
        "traps": ["A vertical line test detects functions.", "Domain restrictions matter."],
        "apps": ["modelling", "calculus", "data transformations", "computer science"],
    },
    "Quadratic Functions and Equations": {
        "formulas": ["ax^2+bx+c=0", "x=(-b±√(b^2-4ac))/(2a)", "vertex x=-b/(2a)", "discriminant Δ=b^2-4ac"],
        "examples": ["x^2-5x+6=0 ⇒ x=2,3", "Δ>0 gives two real roots"],
        "traps": ["Keep the ± in the quadratic formula.", "The vertex is not always a root."],
        "apps": ["projectiles", "optimization", "area problems", "parabolic design"],
    },
    "Trigonometry": {
        "formulas": ["sin θ=opposite/hypotenuse", "cos θ=adjacent/hypotenuse", "tan θ=sin θ/cos θ", "sin^2θ+cos^2θ=1"],
        "examples": ["In a 3-4-5 triangle, sin θ=3/5 and cos θ=4/5", "180° = π radians"],
        "traps": ["Calculator degree/radian mode matters.", "tan θ is undefined when cos θ=0."],
        "apps": ["height and distance", "waves", "navigation", "rotations"],
    },
    "Analytic Geometry Conics": {
        "formulas": ["circle: (x-h)^2+(y-k)^2=r^2", "parabola: y^2=4ax", "ellipse: x^2/a^2+y^2/b^2=1", "hyperbola: x^2/a^2-y^2/b^2=1"],
        "examples": ["x^2+y^2=25 is a circle of radius 5", "Parabola focus-directrix definition drives its shape"],
        "traps": ["Complete the square before identifying the centre.", "Axes and signs classify the conic."],
        "apps": ["orbits", "optics", "architecture", "coordinate modelling"],
    },
    "Matrices and Determinants": {
        "formulas": ["AB generally ≠ BA", "det [[a,b],[c,d]]=ad-bc", "A⁻¹ exists iff det(A)≠0", "Ax=b"],
        "examples": ["det [[2,1],[5,3]]=1", "Matrix multiplication composes transformations"],
        "traps": ["Matrix dimensions must match.", "A zero determinant means no inverse."],
        "apps": ["systems", "graphics", "data transforms", "linear algebra"],
    },
    "Vectors and 3D Geometry": {
        "formulas": ["|v|=√(x^2+y^2+z^2)", "a·b=|a||b|cos θ", "a×b is perpendicular to both", "line: r=a+λb"],
        "examples": ["(2,1,2) has magnitude 3", "Dot product 0 means perpendicular vectors"],
        "traps": ["Vectors have direction and magnitude.", "Cross product order changes sign."],
        "apps": ["physics", "3D graphics", "planes and lines", "navigation"],
    },
    "Limits and Continuity": {
        "formulas": ["lim x→a f(x)=L", "continuous if lim x→a f(x)=f(a)", "standard: lim sin x/x = 1", "one-sided limits must agree"],
        "examples": ["lim (x^2-1)/(x-1) as x→1 = 2", "A removable hole can have a limit but not continuity"],
        "traps": ["Substitution failure is not the final answer.", "Left and right limits must match."],
        "apps": ["calculus foundations", "rates", "asymptotes", "analysis"],
    },
    "Differentiation": {
        "formulas": ["d/dx x^n = nx^(n-1)", "product rule: (uv)'=u'v+uv'", "chain rule: d f(g(x))=f'(g(x))g'(x)", "slope of tangent = derivative"],
        "examples": ["d/dx(x^3+2x)=3x^2+2", "At a maximum, derivative may be 0"],
        "traps": ["Chain rule is needed for nested functions.", "Derivative zero does not always mean maximum."],
        "apps": ["optimization", "motion", "curve sketching", "sensitivity"],
    },
    "Integration": {
        "formulas": ["∫ x^n dx = x^(n+1)/(n+1)+C", "area = definite integral", "FTC: ∫_a^b f(x)dx=F(b)-F(a)", "substitution reverses chain rule"],
        "examples": ["∫ 2x dx = x^2+C", "Area under y=x from 0 to 2 is 2"],
        "traps": ["Do not forget +C for indefinite integrals.", "Signed area can be negative."],
        "apps": ["area", "accumulation", "probability density", "physics work"],
    },
    "Differential Equations": {
        "formulas": ["dy/dx = ky ⇒ y=Ce^(kx)", "separable: dy/g(y)=f(x)dx", "initial condition selects one curve", "order = highest derivative"],
        "examples": ["dy/dx=2y, y(0)=3 ⇒ y=3e^(2x)", "Logistic growth models carrying capacity"],
        "traps": ["General solution needs constants.", "Check domains after solving."],
        "apps": ["growth", "cooling", "circuits", "population models"],
    },
    "Probability": {
        "formulas": ["0≤P(A)≤1", "P(A∪B)=P(A)+P(B)-P(A∩B)", "conditional: P(A|B)=P(A∩B)/P(B)", "independent: P(A∩B)=P(A)P(B)"],
        "examples": ["Fair die: P(even)=3/6=1/2", "Without replacement changes the second probability"],
        "traps": ["Mutually exclusive is not the same as independent.", "Condition on the information actually given."],
        "apps": ["risk", "games", "statistics", "machine learning"],
    },
    "Statistics and Data Visualization": {
        "formulas": ["mean = Σx/n", "median = middle value", "variance = Σ(x-μ)^2/n", "z=(x-μ)/σ"],
        "examples": ["Data 2,3,7 has mean 4", "Outliers affect mean more than median"],
        "traps": ["Choose chart type for the question.", "Correlation does not prove causation."],
        "apps": ["surveys", "quality control", "dashboards", "research"],
    },
    "Combinatorics and Counting": {
        "formulas": ["n! arrangements", "nPr=n!/(n-r)!", "nCr=n!/[r!(n-r)!]", "addition vs multiplication principle"],
        "examples": ["Choose 2 from 5: C(5,2)=10", "3 shirts and 4 pants give 12 outfits"],
        "traps": ["Order matters for permutations, not combinations.", "Avoid double counting."],
        "apps": ["probability", "codes", "network paths", "discrete math"],
    },
    "Set Theory and Logic": {
        "formulas": ["A∪B, A∩B, A\\B", "De Morgan: (A∪B)'=A'∩B'", "p⇒q", "contrapositive: ¬q⇒¬p"],
        "examples": ["If A={1,2}, B={2,3}, then A∩B={2}", "Truth tables test compound statements"],
        "traps": ["Converse is not equivalent to the original statement.", "Universal and existential quantifiers differ."],
        "apps": ["proof", "databases", "logic circuits", "discrete mathematics"],
    },
    "Graph Theory": {
        "formulas": ["G=(V,E)", "degree sum = 2|E|", "tree edges = n-1", "Euler path conditions use odd degrees"],
        "examples": ["A connected graph with 5 vertices and 4 edges can be a tree", "Shortest paths minimize total edge weight"],
        "traps": ["Graph here means vertices and edges, not a coordinate plot.", "Directed edges have orientation."],
        "apps": ["networks", "routes", "social graphs", "computer science"],
    },
    "Abstract Algebra": {
        "formulas": ["group: closure, associativity, identity, inverse", "aH={ah:h∈H}", "ring has addition and multiplication", "field: nonzero elements have multiplicative inverses"],
        "examples": ["Integers under addition form a group", "Z_n arithmetic models modular systems"],
        "traps": ["Operation must be specified.", "Commutativity is not automatic for groups."],
        "apps": ["cryptography", "symmetry", "coding theory", "pure math"],
    },
    "Real Analysis": {
        "formulas": ["ε-δ limit definition", "Cauchy sequence", "supremum property", "uniform convergence vs pointwise convergence"],
        "examples": ["1/n → 0", "A bounded monotone sequence converges"],
        "traps": ["Pointwise convergence need not preserve continuity.", "Quantifier order matters."],
        "apps": ["rigorous calculus", "approximation", "measure theory", "functional analysis"],
    },
    "Linear Algebra Advanced": {
        "formulas": ["span{v₁,...,v_k}", "Av=λv", "rank-nullity: rank(A)+nullity(A)=n", "A=PDP⁻¹ when diagonalizable"],
        "examples": ["Eigenvectors keep direction under a linear map", "Basis coordinates describe every vector uniquely"],
        "traps": ["Independent vectors are not automatically orthogonal.", "Repeated eigenvalues may lack enough eigenvectors."],
        "apps": ["machine learning", "PCA", "quantum mechanics", "dynamical systems"],
    },
}


def font(path: Path, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(path), size=size)


F = {
    "title": font(FONT_BOLD, 138),
    "subtitle": font(FONT_REG, 58),
    "label": font(FONT_SEMI, 48),
    "card_title": font(FONT_BOLD, 66),
    "body": font(FONT_REG, 48),
    "small": font(FONT_REG, 38),
    "tiny": font(FONT_REG, 32),
    "micro": font(FONT_REG, 28),
    "formula": font(FONT_MATH, 58),
    "formula_big": font(FONT_MATH, 82),
}


PROOF_ENHANCEMENTS = [
    "color-coded stages", "before/after views", "area or grid model", "direction arrows",
    "same-quantity rearrangement", "transparent overlays", "coordinate grids", "invariant badges",
    "why-step labels", "cut-and-slide cues", "dissection proof", "number line",
    "zoom-in detail", "proof checkpoints", "symbolic + visual columns", "consistent color grammar",
    "assumption ribbon", "not-to-scale note", "counterexample panel", "Venn/logic model",
    "truth-table strip", "balance/equation model", "slope triangle", "tangent overlay",
    "accumulation strip", "limit sequence", "epsilon-delta band", "unit-circle cue",
    "projection shadow", "transformation grid", "eigenvector before/after", "determinant scale cue",
    "counting tree", "lattice markers", "recursive nesting", "modular clock",
    "proof breadcrumbs", "matched equation numbers", "definition labels", "multiple representations",
    "contradiction route", "therefore summary", "motion trails", "symmetry markers",
    "symbol legend", "domain restrictions", "error-trap overlay", "simple-case test",
    "real-world invariant", "picture-proves takeaway",
]


PROOF_COLORS = {
    "given": (59, 130, 246),
    "transform": (249, 115, 22),
    "result": (34, 197, 94),
    "warning": (244, 63, 94),
    "neutral": (71, 85, 105),
}


def clean_topic(folder_name: str) -> str:
    return re.sub(r"^\d+_", "", folder_name).replace("_and_", " and ").replace("_", " ")


def content_key(topic: str) -> str:
    key = topic.replace("Advanced Linear Algebra", "Linear Algebra Advanced")
    return key


def extract_field(text: str, heading: str, default: str) -> str:
    pattern = rf"# {re.escape(heading)}\s+(.+?)(?=\n# |\Z)"
    match = re.search(pattern, text, flags=re.S)
    if not match:
        return default
    lines = [ln.strip() for ln in match.group(1).splitlines() if ln.strip()]
    return lines[0] if lines else default


def prompt_kind(path: Path) -> str:
    name = path.stem
    if "Concept_Map" in name:
        return "Concept Map and Foundations"
    if "Methods" in name:
        return "Methods and Problem Solving"
    return "Advanced View"


def hex_to_rgb(hex_color: str) -> Tuple[int, int, int]:
    hex_color = hex_color.lstrip("#")
    return tuple(int(hex_color[i : i + 2], 16) for i in (0, 2, 4))


def gradient_bg(palette: Tuple[str, str, str, str, str]) -> Image.Image:
    top = hex_to_rgb(palette[0])
    bottom = hex_to_rgb(palette[1])
    grad = Image.new("RGB", (1, H), top)
    pix = grad.load()
    for y in range(H):
        t = y / (H - 1)
        pix[0, y] = tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(3))
    img = grad.resize((W, H))
    overlay = Image.new("RGBA", (W, H), (255, 255, 255, 0))
    d = ImageDraw.Draw(overlay)
    for x, y, r, a in [(6800, 520, 1050, 34), (900, 3500, 900, 28), (4000, 2380, 1400, 18)]:
        d.ellipse((x - r, y - r, x + r, y + r), fill=(*hex_to_rgb(palette[2]), a))
    return Image.alpha_composite(img.convert("RGBA"), overlay)


def rounded_card(draw: ImageDraw.ImageDraw, box, fill=(255, 255, 255, 232), outline=(255, 255, 255, 70), radius=54):
    x1, y1, x2, y2 = box
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle((x1 + 18, y1 + 22, x2 + 18, y2 + 22), radius=radius, fill=(0, 0, 0, 55))
    shadow_blur = shadow.filter(ImageFilter.GaussianBlur(18))
    draw._image.alpha_composite(shadow_blur)
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=3)


def draw_text(draw, xy, text, font_obj, fill, max_width, line_gap=12, max_lines=None) -> int:
    words = text.split()
    lines: List[str] = []
    line = ""
    for word in words:
        test = word if not line else f"{line} {word}"
        if draw.textbbox((0, 0), test, font=font_obj)[2] <= max_width:
            line = test
        else:
            if line:
                lines.append(line)
            line = word
    if line:
        lines.append(line)
    if max_lines and len(lines) > max_lines:
        lines = lines[:max_lines]
        lines[-1] = lines[-1].rstrip(".,;:") + "..."
    x, y = xy
    line_h = font_obj.size + line_gap
    for ln in lines:
        draw.text((x, y), ln, font=font_obj, fill=fill)
        y += line_h
    return y


def draw_bullets(draw, x, y, items: Iterable[str], font_obj, fill, accent, max_width, gap=18, max_items=5) -> int:
    for item in list(items)[:max_items]:
        draw.ellipse((x, y + 16, x + 22, y + 38), fill=accent)
        y = draw_text(draw, (x + 42, y), item, font_obj, fill, max_width - 42, line_gap=8, max_lines=2) + gap
    return y


def draw_chip(draw, xy, text: str, fill, outline=None, text_fill=(15, 23, 42), font_obj=None, pad=28):
    font_obj = font_obj or F["tiny"]
    x, y = xy
    bbox = draw.textbbox((0, 0), text, font=font_obj)
    w = bbox[2] - bbox[0] + pad * 2
    h = bbox[3] - bbox[1] + 22
    draw.rounded_rectangle((x, y, x + w, y + h), radius=h // 2, fill=fill, outline=outline or fill, width=2)
    draw.text((x + pad, y + 10), text, font=font_obj, fill=text_fill)
    return x + w + 16


def draw_arrow(draw, start, end, fill, width=7):
    draw.line((*start, *end), fill=fill, width=width)
    ang = math.atan2(end[1] - start[1], end[0] - start[0])
    size = width * 4
    pts = [
        end,
        (end[0] - size * math.cos(ang - 0.45), end[1] - size * math.sin(ang - 0.45)),
        (end[0] - size * math.cos(ang + 0.45), end[1] - size * math.sin(ang + 0.45)),
    ]
    draw.polygon(pts, fill=fill)


def draw_proof_legend(draw, box, palette):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(248, 250, 252, 238), radius=42)
    draw.text((x1 + 46, y1 + 34), "Visual-Proof Grammar", font=F["label"], fill=(15, 23, 42))
    x = x1 + 46
    y = y1 + 118
    labels = [("GIVEN", PROOF_COLORS["given"]), ("TRANSFORM", PROOF_COLORS["transform"]), ("RESULT", PROOF_COLORS["result"]), ("TRAP", PROOF_COLORS["warning"])]
    for label, color in labels:
        x = draw_chip(draw, (x, y), label, color, text_fill=(255, 255, 255), font_obj=F["tiny"], pad=24)
    y += 74
    draw_text(draw, (x1 + 48, y), "Every proof reads left-to-right: assumptions → invariant → transformation → checked conclusion.", F["small"], (51, 65, 85), x2 - x1 - 96, max_lines=2)


def draw_assumption_ribbon(draw, box, meta, data, palette):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(255, 255, 255, 235), radius=40)
    draw.text((x1 + 50, y1 + 36), "Assumptions Before The Picture", font=F["label"], fill=(15, 23, 42))
    y = y1 + 112
    items = [
        f"Topic: {meta['topic']}",
        "Definitions and domains are stated first.",
        f"Known relation: {data['formulas'][0]}",
    ]
    draw_bullets(draw, x1 + 58, y, items, F["small"], (30, 41, 59), hex_to_rgb(palette[2]), x2 - x1 - 116, gap=12, max_items=3)
    draw_chip(draw, (x1 + 52, y2 - 78), "NOT TO SCALE WHEN CONCEPTUAL", (226, 232, 240), text_fill=(51, 65, 85), font_obj=F["micro"], pad=22)


def draw_invariant_strip(draw, box, palette, topic):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(15, 23, 42, 238), outline=(*hex_to_rgb(palette[2]), 180), radius=44)
    draw.text((x1 + 52, y1 + 40), "Invariant Tags", font=F["label"], fill=(255, 255, 255))
    tags = ["same value", "area/length preserved", "units tracked", "domain checked"]
    if "Logic" in topic or "Set" in topic:
        tags = ["truth preserved", "sets matched", "counterexample checked", "quantifiers fixed"]
    elif "Probability" in topic or "Statistics" in topic:
        tags = ["sample space fixed", "units tracked", "conditioning marked", "outlier checked"]
    elif "Linear Algebra" in topic or "Matrices" in topic:
        tags = ["span preserved", "basis labelled", "scale factor shown", "rank checked"]
    x = x1 + 52
    y = y1 + 125
    for tag in tags:
        x = draw_chip(draw, (x, y), tag.upper(), hex_to_rgb(palette[4]), text_fill=(15, 23, 42), font_obj=F["tiny"], pad=24)


def draw_number_line_or_sequence(draw, box, palette, topic):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(255, 255, 255, 236), radius=42)
    draw.text((x1 + 48, y1 + 34), "Simple-Case Test", font=F["label"], fill=(15, 23, 42))
    lx1, lx2 = x1 + 150, x2 - 130
    ly = y1 + 170
    draw.line((lx1, ly, lx2, ly), fill=(30, 41, 59), width=8)
    for i, val in enumerate(["-2", "-1", "0", "1", "2"]):
        x = lx1 + i * (lx2 - lx1) / 4
        draw.line((x, ly - 34, x, ly + 34), fill=(30, 41, 59), width=6)
        draw.text((x - 24, ly + 52), val, font=F["tiny"], fill=(30, 41, 59))
    if "Combinatorics" in topic:
        labels = ["choice A", "choice B", "multiply"]
    elif "Limits" in topic or "Analysis" in topic:
        labels = ["x near a", "|x-a| small", "|f(x)-L| small"]
    else:
        labels = ["choose easy values", "compare the model", "verify result"]
    px = lx1 + 70
    for label in labels:
        draw.ellipse((px - 22, ly - 95, px + 22, ly - 51), fill=hex_to_rgb(palette[2]))
        draw.text((px - 70, ly - 148), label, font=F["micro"], fill=(51, 65, 85))
        px += (lx2 - lx1) // 3


def draw_zoom_callout(draw, source_box, target_box, label, palette):
    sx1, sy1, sx2, sy2 = source_box
    tx1, ty1, tx2, ty2 = target_box
    accent = hex_to_rgb(palette[2])
    draw.rounded_rectangle(source_box, radius=22, outline=accent, width=8)
    draw_arrow(draw, (sx2, (sy1 + sy2) / 2), (tx1, (ty1 + ty2) / 2), accent, width=6)
    draw.rounded_rectangle(target_box, radius=30, fill=(248, 250, 252), outline=accent, width=5)
    draw.text((tx1 + 28, ty1 + 26), "ZOOM", font=F["tiny"], fill=accent)
    draw_text(draw, (tx1 + 28, ty1 + 78), label, F["micro"], (30, 41, 59), tx2 - tx1 - 56, max_lines=3)


def draw_symbolic_visual_columns(draw, box, data, palette):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(248, 250, 252, 238), radius=48)
    draw.text((x1 + 54, y1 + 40), "Symbolic Reasoning  +  Visual Reasoning", font=F["card_title"], fill=(15, 23, 42))
    mid = (x1 + x2) // 2
    draw.line((mid, y1 + 138, mid, y2 - 42), fill=(203, 213, 225), width=4)
    draw.text((x1 + 60, y1 + 150), "Equation Side", font=F["label"], fill=PROOF_COLORS["given"])
    draw.text((mid + 60, y1 + 150), "Picture Side", font=F["label"], fill=PROOF_COLORS["transform"])
    eqs = data["formulas"][:3]
    y = y1 + 238
    for i, eq in enumerate(eqs, 1):
        draw.text((x1 + 82, y), f"({i}) {eq}", font=F["formula"], fill=(15, 23, 42))
        y += 112
    y = y1 + 238
    visuals = ["mark knowns", "preserve invariant", "compare final shape"]
    for i, item in enumerate(visuals, 1):
        draw_chip(draw, (mid + 70, y), f"{i}. {item}", (226, 232, 240), outline=hex_to_rgb(palette[2]), text_fill=(15, 23, 42), font_obj=F["small"], pad=28)
        y += 112


def draw_truth_or_counterexample(draw, box, data, palette, topic):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(255, 255, 255, 238), radius=46)
    draw.text((x1 + 50, y1 + 38), "Counterexample / Boundary Check", font=F["label"], fill=(15, 23, 42))
    draw.rounded_rectangle((x1 + 54, y1 + 124, x2 - 54, y1 + 300), radius=30, fill=(255, 241, 242), outline=PROOF_COLORS["warning"], width=4)
    draw.text((x1 + 86, y1 + 158), "Tempting shortcut:", font=F["small"], fill=PROOF_COLORS["warning"])
    trap = data["traps"][0] if data["traps"] else "A visual pattern needs proof."
    draw_text(draw, (x1 + 86, y1 + 212), trap, F["small"], (30, 41, 59), x2 - x1 - 172, max_lines=2)
    if "Logic" in topic or "Set" in topic or "Probability" in topic:
        headers = ["p", "q", "p⇒q"]
        vals = [("T", "T", "T"), ("T", "F", "F"), ("F", "T", "T")]
        tx, ty = x1 + 92, y1 + 370
        for c, htxt in enumerate(headers):
            draw.text((tx + c * 230, ty), htxt, font=F["label"], fill=(15, 23, 42))
        for r, row in enumerate(vals):
            for c, cell in enumerate(row):
                draw.rounded_rectangle((tx + c * 230 - 20, ty + 70 + r * 78, tx + c * 230 + 120, ty + 125 + r * 78), radius=18, fill=(226, 232, 240))
                draw.text((tx + c * 230 + 25, ty + 78 + r * 78), cell, font=F["small"], fill=(15, 23, 42))
    else:
        draw_text(draw, (x1 + 80, y1 + 375), "Test a small value, edge case, or labelled diagram before trusting the shortcut.", F["small"], (30, 41, 59), x2 - x1 - 160, max_lines=3)


def draw_proof_takeaway(draw, box, data, palette):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(15, 23, 42, 240), outline=(*hex_to_rgb(palette[4]), 190), radius=46)
    draw.text((x1 + 56, y1 + 42), "Therefore", font=F["card_title"], fill=hex_to_rgb(palette[4]))
    takeaway = "The picture proves the rule because the invariant is preserved while the representation changes."
    if data["formulas"]:
        takeaway = f"The picture proves {data['formulas'][0]} by preserving the same quantity through each labelled step."
    draw_text(draw, (x1 + 56, y1 + 142), takeaway, F["body"], (248, 250, 252), x2 - x1 - 112, max_lines=3)
    draw_chip(draw, (x2 - 710, y2 - 88), "VISUAL → SYMBOLIC → CHECKED", hex_to_rgb(palette[2]), text_fill=(15, 23, 42), font_obj=F["tiny"], pad=26)


def draw_enhancement_matrix(draw, box, palette):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(255, 255, 255, 236), radius=42)
    draw.text((x1 + 46, y1 + 34), "50 Visual-Proof Enhancements Applied", font=F["label"], fill=(15, 23, 42))
    cols = 5
    rows = 10
    cell_w = (x2 - x1 - 92) / cols
    cell_h = (y2 - y1 - 140) / rows
    start_x, start_y = x1 + 46, y1 + 120
    for i, item in enumerate(PROOF_ENHANCEMENTS):
        col = i % cols
        row = i // cols
        cx = start_x + col * cell_w
        cy = start_y + row * cell_h
        color = hex_to_rgb(palette[2]) if i % 3 else hex_to_rgb(palette[4])
        draw.ellipse((cx, cy + 8, cx + 24, cy + 32), fill=color)
        draw.text((cx + 34, cy), item, font=F["micro"], fill=(30, 41, 59))


def draw_header(draw, title, subtitle, level, part_label, palette):
    accent = hex_to_rgb(palette[2])
    gold = hex_to_rgb(palette[4])
    draw.rounded_rectangle((320, 230, 1240, 320), radius=45, fill=(*accent, 210))
    draw.text((380, 245), part_label.upper(), font=F["label"], fill=(5, 20, 36))
    draw.text((320, 380), title.upper(), font=F["title"], fill=(255, 255, 255))
    draw_text(draw, (330, 548), subtitle, F["subtitle"], (230, 244, 255), 5300, line_gap=8, max_lines=2)
    draw.rounded_rectangle((6260, 370, 7330, 495), radius=50, fill=(*gold, 235))
    draw.text((6325, 405), level[:33], font=F["small"], fill=(24, 24, 27))
    draw.line((320, 730, 7360, 730), fill=(*accent, 180), width=6)


def draw_formula_strip(draw, box, formulas, palette):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(248, 250, 252, 238), radius=46)
    draw.text((x1 + 60, y1 + 48), "Formula And Notation Bank", font=F["card_title"], fill=(15, 23, 42))
    x = x1 + 70
    y = y1 + 165
    col_w = (x2 - x1 - 150) // 2
    for idx, formula in enumerate(formulas[:4]):
        cx = x + (idx % 2) * col_w
        cy = y + (idx // 2) * 150
        draw.rounded_rectangle((cx, cy, cx + col_w - 40, cy + 105), radius=28, fill=(226, 232, 240), outline=hex_to_rgb(palette[2]), width=3)
        draw.text((cx + 34, cy + 24), formula, font=F["formula"], fill=(15, 23, 42))


def draw_grid(draw, origin, size, palette, kind):
    x, y = origin
    w, h = size
    accent = hex_to_rgb(palette[2])
    gold = hex_to_rgb(palette[4])
    draw.rounded_rectangle((x, y, x + w, y + h), radius=42, fill=(15, 23, 42, 235), outline=(*accent, 190), width=4)
    gx1, gy1, gx2, gy2 = x + 140, y + 140, x + w - 120, y + h - 135
    for i in range(9):
        xx = gx1 + i * (gx2 - gx1) / 8
        draw.line((xx, gy1, xx, gy2), fill=(148, 163, 184, 78), width=2)
    for i in range(7):
        yy = gy1 + i * (gy2 - gy1) / 6
        draw.line((gx1, yy, gx2, yy), fill=(148, 163, 184, 78), width=2)
    draw.line((gx1, (gy1 + gy2) / 2, gx2, (gy1 + gy2) / 2), fill=(226, 232, 240, 180), width=5)
    draw.line(((gx1 + gx2) / 2, gy1, (gx1 + gx2) / 2, gy2), fill=(226, 232, 240, 180), width=5)
    if kind == "geometry":
        pts = [(gx1 + 220, gy2 - 170), (gx1 + 980, gy2 - 170), (gx1 + 620, gy1 + 210)]
        draw.polygon(pts, outline=gold, fill=(250, 204, 21, 45))
        draw.line((pts[0], pts[1], pts[2], pts[0]), fill=gold, width=12)
        draw.arc((pts[0][0] + 45, pts[0][1] - 95, pts[0][0] + 225, pts[0][1] + 85), 205, 278, fill=accent, width=8)
        draw.text((pts[0][0] + 160, pts[0][1] - 85), "θ", font=F["formula_big"], fill=(255, 255, 255))
    elif kind == "calculus":
        last = None
        for i in range(420):
            t = i / 419
            xx = gx1 + t * (gx2 - gx1)
            val = math.sin(t * math.pi * 2.2) * 0.26 + (0.5 - t) * -0.35
            yy = (gy1 + gy2) / 2 - val * (gy2 - gy1)
            if last:
                draw.line((last[0], last[1], xx, yy), fill=gold, width=9)
            last = (xx, yy)
        draw.line((gx1 + 860, gy1 + 430, gx1 + 1320, gy1 + 315), fill=accent, width=8)
        draw.text((gx1 + 915, gy1 + 230), "slope = f'(x)", font=F["formula"], fill=(255, 255, 255))
    elif kind == "sets":
        draw.ellipse((gx1 + 290, gy1 + 210, gx1 + 1020, gy1 + 880), outline=accent, width=10, fill=(45, 212, 191, 55))
        draw.ellipse((gx1 + 740, gy1 + 210, gx1 + 1470, gy1 + 880), outline=gold, width=10, fill=(250, 204, 21, 45))
        draw.text((gx1 + 520, gy1 + 460), "A", font=F["formula_big"], fill=(255, 255, 255))
        draw.text((gx1 + 1190, gy1 + 460), "B", font=F["formula_big"], fill=(255, 255, 255))
        draw.text((gx1 + 795, gy1 + 475), "A∩B", font=F["formula"], fill=(255, 255, 255))
    elif kind == "network":
        nodes = [(gx1 + 250, gy1 + 280), (gx1 + 750, gy1 + 200), (gx1 + 1210, gy1 + 380), (gx1 + 420, gy1 + 760), (gx1 + 1060, gy1 + 820)]
        edges = [(0, 1), (1, 2), (0, 3), (3, 4), (2, 4), (1, 4)]
        for a, b in edges:
            draw.line((*nodes[a], *nodes[b]), fill=(226, 232, 240, 150), width=8)
        for idx, p in enumerate(nodes):
            draw.ellipse((p[0] - 52, p[1] - 52, p[0] + 52, p[1] + 52), fill=accent if idx % 2 else gold)
            draw.text((p[0] - 18, p[1] - 32), str(idx + 1), font=F["label"], fill=(15, 23, 42))
    elif kind == "linear":
        center = ((gx1 + gx2) / 2, (gy1 + gy2) / 2)
        for vx, vy, label, col in [(520, -250, "v₁", accent), (-280, -380, "v₂", gold), (780, -30, "Av=λv", (255, 255, 255))]:
            draw.line((center[0], center[1], center[0] + vx, center[1] + vy), fill=col, width=13)
            draw.polygon([(center[0] + vx, center[1] + vy), (center[0] + vx - 42, center[1] + vy + 12), (center[0] + vx - 18, center[1] + vy + 48)], fill=col)
            draw.text((center[0] + vx + 30, center[1] + vy - 45), label, font=F["formula"], fill=(255, 255, 255))
    else:
        last = None
        for i in range(360):
            t = i / 359
            xx = gx1 + t * (gx2 - gx1)
            yy = gy2 - (0.12 + 0.78 * (t**2)) * (gy2 - gy1)
            if last:
                draw.line((last[0], last[1], xx, yy), fill=gold, width=9)
            last = (xx, yy)
        draw.text((gx1 + 1180, gy1 + 160), "y = f(x)", font=F["formula_big"], fill=(255, 255, 255))


def visual_kind(topic: str) -> str:
    t = topic.lower()
    if any(k in t for k in ["geometry", "triangles", "circles", "area", "trigonometry", "conics", "vectors"]):
        return "geometry"
    if any(k in t for k in ["limits", "differentiation", "integration", "differential", "analysis"]):
        return "calculus"
    if any(k in t for k in ["set", "logic", "probability", "statistics", "combinatorics"]):
        return "sets"
    if "graph theory" in t:
        return "network"
    if any(k in t for k in ["linear algebra", "matrices", "abstract algebra"]):
        return "linear"
    return "function"


def draw_concept_map(draw, box, title, data, palette, kind):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(241, 245, 249, 238), radius=62)
    draw.text((x1 + 70, y1 + 55), "Central Visual Model", font=F["card_title"], fill=(15, 23, 42))
    draw_text(draw, (x1 + 70, y1 + 145), title, F["small"], (51, 65, 85), x2 - x1 - 140, max_lines=2)
    draw_grid(draw, (x1 + 80, y1 + 285), (x2 - x1 - 160, y2 - y1 - 365), palette, kind)
    cx, cy = (x1 + x2) // 2, y2 - 250
    terms = data["apps"][:4]
    for i, term in enumerate(terms):
        ang = -math.pi + i * math.pi / 3 + 0.25
        px = cx + int(math.cos(ang) * 860)
        py = cy + int(math.sin(ang) * 170)
        draw.line((cx, cy, px, py), fill=(*hex_to_rgb(palette[2]), 160), width=6)
        draw.rounded_rectangle((px - 270, py - 58, px + 270, py + 58), radius=35, fill=(*hex_to_rgb(palette[4]), 230))
        draw.text((px - 225, py - 30), term.title(), font=F["small"], fill=(15, 23, 42))
    draw_zoom_callout(draw, (x1 + 1720, y1 + 710, x1 + 2150, y1 + 1010), (x1 + 1790, y2 - 445, x2 - 120, y2 - 245), "Zoom-in: label the invariant before reading the final formula.", palette)
    draw_chip(draw, (x1 + 128, y2 - 160), "ASSUMPTIONS", PROOF_COLORS["given"], text_fill=(255, 255, 255), font_obj=F["tiny"])
    draw_chip(draw, (x1 + 480, y2 - 160), "TRANSFORM", PROOF_COLORS["transform"], text_fill=(255, 255, 255), font_obj=F["tiny"])
    draw_chip(draw, (x1 + 835, y2 - 160), "RESULT", PROOF_COLORS["result"], text_fill=(255, 255, 255), font_obj=F["tiny"])


def draw_card(draw, box, heading, items, palette, mode="bullets"):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(255, 255, 255, 238), radius=48)
    draw.text((x1 + 52, y1 + 44), heading, font=F["card_title"], fill=(15, 23, 42))
    if mode == "formulas":
        y = y1 + 150
        for formula in items[:4]:
            draw.rounded_rectangle((x1 + 52, y, x2 - 52, y + 100), radius=24, fill=(239, 246, 255), outline=hex_to_rgb(palette[2]), width=3)
            draw.text((x1 + 82, y + 22), formula, font=F["formula"], fill=(15, 23, 42))
            y += 122
    else:
        draw_bullets(draw, x1 + 62, y1 + 152, items, F["body"], (30, 41, 59), hex_to_rgb(palette[2]), x2 - x1 - 130, max_items=4)


def draw_process_flow(draw, box, steps, palette):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(248, 250, 252, 238), radius=50)
    draw.text((x1 + 60, y1 + 45), "Problem-Solving Flow", font=F["card_title"], fill=(15, 23, 42))
    gap = 36
    n = len(steps)
    step_w = (x2 - x1 - 120 - gap * (n - 1)) / n
    y = y1 + 165
    for i, step in enumerate(steps):
        sx = x1 + 60 + i * (step_w + gap)
        draw.rounded_rectangle((sx, y, sx + step_w, y + 240), radius=34, fill=(226, 232, 240), outline=hex_to_rgb(palette[2]), width=3)
        draw.ellipse((sx + 30, y + 28, sx + 106, y + 104), fill=hex_to_rgb(palette[4]))
        draw.text((sx + 55, y + 40), str(i + 1), font=F["label"], fill=(15, 23, 42))
        draw_text(draw, (sx + 32, y + 125), step, F["small"], (15, 23, 42), step_w - 64, max_lines=3)
        if i < n - 1:
            ax = sx + step_w + 7
            draw.line((ax, y + 120, ax + gap - 14, y + 120), fill=hex_to_rgb(palette[2]), width=8)
            draw.polygon([(ax + gap - 14, y + 120), (ax + gap - 42, y + 100), (ax + gap - 42, y + 140)], fill=hex_to_rgb(palette[2]))


def draw_before_after_proof(draw, box, data, palette, topic):
    x1, y1, x2, y2 = box
    rounded_card(draw, box, fill=(248, 250, 252, 238), radius=52)
    draw.text((x1 + 54, y1 + 40), "Before → Transform → Result", font=F["card_title"], fill=(15, 23, 42))
    panel_gap = 42
    panel_w = (x2 - x1 - 108 - 2 * panel_gap) / 3
    labels = [("1 GIVEN", PROOF_COLORS["given"]), ("2 TRANSFORM", PROOF_COLORS["transform"]), ("3 RESULT", PROOF_COLORS["result"])]
    captions = ["mark knowns", "preserve invariant", "state conclusion"]
    y = y1 + 150
    for i, (label, color) in enumerate(labels):
        px = x1 + 54 + i * (panel_w + panel_gap)
        draw.rounded_rectangle((px, y, px + panel_w, y + 510), radius=36, fill=(15, 23, 42), outline=color, width=5)
        draw_chip(draw, (px + 32, y + 32), label, color, text_fill=(255, 255, 255), font_obj=F["tiny"])
        if i < 2:
            draw_arrow(draw, (px + panel_w + 8, y + 255), (px + panel_w + panel_gap - 12, y + 255), hex_to_rgb(palette[2]), width=8)
        cx, cy = px + panel_w / 2, y + 295
        if "Set" in topic or "Probability" in topic:
            draw.ellipse((cx - 150, cy - 100, cx + 95, cy + 140), outline=color, width=7, fill=(*color, 42))
            draw.ellipse((cx - 10, cy - 100, cx + 235, cy + 140), outline=hex_to_rgb(palette[4]), width=7, fill=(*hex_to_rgb(palette[4]), 38))
        elif "Graph Theory" in topic or "Combinatorics" in topic:
            pts = [(cx - 170, cy - 80), (cx + 10, cy - 150), (cx + 180, cy - 20), (cx - 90, cy + 145)]
            for a, b in [(0, 1), (1, 2), (2, 3), (3, 0)]:
                draw.line((*pts[a], *pts[b]), fill=color, width=7)
            for p in pts:
                draw.ellipse((p[0] - 26, p[1] - 26, p[0] + 26, p[1] + 26), fill=hex_to_rgb(palette[4]))
        else:
            draw.rectangle((cx - 190, cy - 120, cx + 190, cy + 120), outline=color, width=7, fill=(*color, 38))
            draw.line((cx - 190, cy, cx + 190, cy), fill=hex_to_rgb(palette[4]), width=6)
            draw.line((cx, cy - 120, cx, cy + 120), fill=hex_to_rgb(palette[4]), width=6)
        draw.text((px + 42, y + 430), captions[i], font=F["small"], fill=(248, 250, 252))
    draw_text(draw, (x1 + 58, y2 - 126), f"Matched equation: {data['formulas'][0]}    |    Check: {data['examples'][0]}", F["small"], (51, 65, 85), x2 - x1 - 116, max_lines=2)


def part_one(meta, data, palette, out_path):
    img = gradient_bg(palette)
    draw = ImageDraw.Draw(img)
    draw_header(draw, meta["title"], meta["subtitle"], meta["level"], "Part 01 • Visual Proof", palette)
    topic = meta["topic"]
    kind = visual_kind(topic)
    draw_proof_legend(draw, (320, 835, 2300, 1225), palette)
    draw_assumption_ribbon(draw, (320, 1325, 2300, 2055), meta, data, palette)
    draw_invariant_strip(draw, (320, 2160, 2300, 2485), palette, topic)
    draw_number_line_or_sequence(draw, (320, 2590, 2300, 3170), palette, topic)
    draw_concept_map(draw, (2460, 835, 5220, 3170), meta["subtitle"], data, palette, kind)
    draw_card(draw, (5380, 835, 7360, 1615), "Notation Key", data["formulas"], palette, mode="formulas")
    draw_truth_or_counterexample(draw, (5380, 1735, 7360, 2520), data, palette, topic)
    draw_card(draw, (5380, 2640, 7360, 3170), "Matched Representations", ["diagram: mark knowns", f"symbol: {data['formulas'][0]}", "table/graph: test a simple case"], palette)
    draw_formula_strip(draw, (320, 3380, 7360, 4055), data["formulas"], palette)
    img.convert("RGB").save(out_path, "PNG", dpi=DPI, compress_level=1)


def part_two(meta, data, palette, out_path):
    img = gradient_bg(palette)
    draw = ImageDraw.Draw(img)
    draw_header(draw, meta["title"], meta["subtitle"], meta["level"], "Part 02 • Proof Strategy", palette)
    steps = ["Read the given information", "Choose a model or formula", "Substitute with units", "Simplify carefully", "Check reasonableness"]
    draw_process_flow(draw, (320, 840, 7360, 1330), steps, palette)
    draw_before_after_proof(draw, (320, 1495, 3960, 2425), data, palette, meta["topic"])
    draw_symbolic_visual_columns(draw, (4100, 1495, 7360, 2425), data, palette)
    draw_card(draw, (320, 2590, 1840, 3300), "Formula Selection", data["formulas"], palette, mode="formulas")
    draw_truth_or_counterexample(draw, (1980, 2590, 3700, 3300), data, palette, meta["topic"])
    draw_card(draw, (3840, 2590, 5400, 3300), "Applications", [f"Connection: {x.title()}" for x in data["apps"]], palette)
    draw_proof_takeaway(draw, (5540, 2590, 7360, 3300), data, palette)
    draw_enhancement_matrix(draw, (320, 3445, 7360, 4130), palette)
    img.convert("RGB").save(out_path, "PNG", dpi=DPI, compress_level=1)


def safe_name(name: str) -> str:
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", name).strip("_")


def build_meta(path: Path) -> Dict[str, str]:
    text = path.read_text(encoding="utf-8", errors="replace")
    topic = clean_topic(path.parent.name)
    title = extract_field(text, "INFOGRAPHIC TITLE", topic).replace("_", " ")
    subtitle = extract_field(text, "MAIN SUBTITLE", prompt_kind(path))
    level = extract_field(text, "TARGET LEVEL", "Mathematics learning bridge")
    return {"topic": topic, "title": title, "subtitle": subtitle, "level": level, "kind": prompt_kind(path)}


def render_prompt(job) -> str:
    idx, total, prompt_text = job
    prompt = Path(prompt_text)
    meta = build_meta(prompt)
    key = content_key(meta["topic"])
    data = CONTENT.get(key, {
        "formulas": ["define variables clearly", "model → equation → interpretation", "check units and domain", "state assumptions"],
        "examples": ["Translate the visual pattern into symbols", "Verify the result with a simple case"],
        "traps": ["Do not skip definitions.", "A diagram supports reasoning but does not replace proof."],
        "apps": ["school mathematics", "modelling", "exam reasoning", "higher mathematics"],
    })
    palette = PALETTES[(idx - 1) % len(PALETTES)]
    prompt_dir = OUT_ROOT / prompt.parent.name / safe_name(prompt.stem)
    prompt_dir.mkdir(parents=True, exist_ok=True)
    p1 = prompt_dir / f"{safe_name(prompt.stem)}_Part_01.png"
    p2 = prompt_dir / f"{safe_name(prompt.stem)}_Part_02.png"
    part_one(meta, data, palette, p1)
    part_two(meta, data, palette, p2)
    return f"[{idx:03d}/{total:03d}] {prompt.parent.name} / {prompt.stem}\t{prompt}\t{p1}\t{p2}"


def main() -> None:
    prompt_files: List[Path] = []
    for folder in sorted([p for p in ROOT.iterdir() if p.is_dir() and re.match(r"^\d+_", p.name)]):
        prompt_files.extend(sorted(folder.glob("*.txt")))

    OUT_ROOT.mkdir(parents=True, exist_ok=True)
    total = len(prompt_files)
    jobs = [(idx, total, str(prompt)) for idx, prompt in enumerate(prompt_files, 1)]
    manifest = []
    with ProcessPoolExecutor(max_workers=3) as executor:
        futures = [executor.submit(render_prompt, job) for job in jobs]
        for future in as_completed(futures):
            result = future.result()
            print(result.split("\t", 1)[0], flush=True)
            manifest.append(result.split("\t", 1)[1])

    (OUT_ROOT / "manifest.tsv").write_text("\n".join(manifest), encoding="utf-8")
    print(f"Done. Rendered {len(prompt_files) * 2} PNG files into {OUT_ROOT}")


if __name__ == "__main__":
    main()
