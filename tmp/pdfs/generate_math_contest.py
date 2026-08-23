from pathlib import Path
import html
import math
import os
import re

import pdfplumber
import pypdfium2 as pdfium
from PIL import Image, ImageDraw
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import BaseDocTemplate, Flowable, Frame, KeepTogether, PageBreak, PageTemplate, Paragraph, Spacer


OUTPUT = Path(r"D:\trythese.pdf")
BUILD_OUTPUT = Path(r"D:\trythese.direct-entry.pdf")
RENDER_DIR = Path(r"C:\Indian Servers\AIExplorer\tmp\pdfs\rendered_math")
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
RENDER_DIR.mkdir(parents=True, exist_ok=True)

fonts = Path(r"C:\Windows\Fonts")
pdfmetrics.registerFont(TTFont("Contest", str(fonts / "NotoSans-Regular.ttf")))
pdfmetrics.registerFont(TTFont("Contest-Bold", str(fonts / "NotoSans-Bold.ttf")))

NAVY = colors.HexColor("#112743")
BLUE = colors.HexColor("#1769AA")
CYAN = colors.HexColor("#0D9DBA")
GOLD = colors.HexColor("#E9A23B")
INK = colors.HexColor("#172332")
MUTED = colors.HexColor("#5D6E7D")


sections = [
    ("SCHOOL MATHEMATICS", "1. Number Systems, Arithmetic, and Commercial Mathematics", "Calculator / Solver / Number Theory", [
        "Evaluate \\(\\frac{7}{12}+\\frac{5}{18}-\\frac{1}{9}\\) and express the result in lowest terms.",
        "Simplify \\(3\\sqrt{48}-2\\sqrt{75}+\\sqrt{27}\\) using exact radical form.",
        "Find the HCF and LCM of \\(756\\) and \\(1,134\\) using prime factorization and the Euclidean algorithm.",
        "Use Number Theory to find the least positive \\(n\\) satisfying \\(n\\,mod\\,12=5\\) and \\(n\\,mod\\,15=7\\).",
        "Evaluate \\(2^{-3}+4^{-1}-8^{-2/3}\\) exactly.",
        "Find the percentage increase when a quantity changes from \\(480\\) to \\(612\\).",
        "Enter \\(2400(1-0.15)(1-0.10)\\) to find the selling price; then calculate the equivalent single discount percentage.",
        "Enter \\(50000[(1+0.08)^{3}-1]\\) to calculate the compound interest exactly.",
        "Evaluate \\(84000(3/12)\\), \\(84000(4/12)\\), and \\(84000(5/12)\\); then calculate each percentage share.",
        "Evaluate \\(t=180/60+120/80\\), then evaluate \\(v=300/t\\) to find total time and average speed.",
    ]),
    ("SCHOOL MATHEMATICS", "2. Linear Equations, Inequalities, and Word Problems", "Solver / 2D Graph", [
        "Solve \\(5x-17=3x+29\\) and verify the solution by substitution.",
        "Solve \\(\\frac{2x-3}{5}-\\frac{x+4}{3}=1\\).",
        "Solve the system \\(2x+3y=17\\) and \\(4x-y=5\\) algebraically and graphically.",
        "Solve \\(3x-5\\leq 2x+7\\) and display the solution on a number line.",
        "Solve the compound inequality \\(-4<2x+6\\leq 14\\).",
        "Enter and solve the system \\(x+y=64;\\ x-y=18\\).",
        "Enter and solve \\(f=3s;\\ f+12=2(s+12)\\) for the present ages \\(f\\) and \\(s\\).",
        "Enter and solve \\(b+s=30/2;\\ b-s=30/3\\), where \\(b\\) is boat speed and \\(s\\) is stream speed, in km/h.",
        "Enter and solve \\(28/(12+w)=7/5\\) for the litres of water \\(w\\) to add.",
        "Enter and solve \\(2(l+w)=94;\\ l=w+9\\), then evaluate \\(lw\\) for the rectangle's area.",
    ]),
    ("SCHOOL MATHEMATICS", "3. Polynomials, Factorization, and Quadratic Equations", "CAS / Solver / 2D Graph", [
        "Factor completely: \\(6x^{3}-15x^{2}-6x+15\\).",
        "Find the remainder when \\(2x^{4}-3x^{3}+5x-7\\) is divided by \\(x-2\\).",
        "If \\(x-3\\) is a factor of \\(x^{3}+ax^{2}-5x-15\\), find \\(a\\) and factor the polynomial completely.",
        "Solve \\(x^{2}-11x+24=0\\) by factorization and show the roots on a graph.",
        "Solve \\(3x^{2}+2x-7=0\\) using the quadratic formula, giving exact and decimal answers.",
        "Determine the nature of the roots of \\(5x^{2}-6x+2=0\\) using the discriminant.",
        "Form the quadratic equation whose roots are \\(2+\\sqrt{3}\\) and \\(2-\\sqrt{3}\\).",
        "Enter and solve \\(n(n+1)=306\\) for the positive integer \\(n\\); report \\(n\\) and \\(n+1\\).",
        "Enter and solve \\(w(w+3)=180\\); report the positive width \\(w\\) and length \\(w+3\\).",
        "Plot \\(y=x^{2}-6x+5\\), identify its vertex, axis of symmetry, roots, and y-intercept, and write its vertex form.",
    ]),
    ("SCHOOL MATHEMATICS", "4. Coordinate Geometry and 2D Graphs", "2D Graph / Geometry / Solver", [
        "Find the distance and midpoint between \\(A(-3,5)\\) and \\(B(7,-1)\\).",
        "Find the point dividing the segment joining \\(A(2,-3)\\) and \\(B(8,9)\\) internally in the ratio \\(2:1\\).",
        "Find the equation of the line through \\(P(4,-2)\\) with slope \\(3/5\\), and plot it.",
        "Find the equation of the line passing through \\((-2,7)\\) and \\(6,-1\\), then calculate both intercepts.",
        "Determine whether the lines \\(3x-2y+5=0\\) and \\(4x+6y-7=0\\) are perpendicular.",
        "Find the area of the triangle with vertices \\(A(1,2)\\), \\(B(7,5)\\), and \\(C(4,10)\\).",
        "Find the equation of the circle with center \\((3,-2)\\) and radius \\(5\\), and plot it as an implicit graph.",
        "Find the center and radius of \\(x^{2}+y^{2}-8x+6y-11=0\\).",
        "Find all intersection points of \\(y=x^{2}-4\\) and \\(y=2x+5\\) using graph and Solver.",
        "Plot the feasible region satisfying \\(x\\geq 0\\), \\(y\\geq 0\\), \\(x+y\\leq 8\\), and \\(2x+y\\leq 10\\), then list its vertices.",
    ]),
    ("SCHOOL MATHEMATICS", "5. Euclidean Geometry and Mensuration", "2D Geometry / 3D Geometry / Solver", [
        "Construct a triangle with sides \\(5\\,\\mathrm{cm}\\), \\(6\\,\\mathrm{cm}\\), and \\(7\\,\\mathrm{cm}\\), then calculate its area using Heron's formula.",
        "In a right triangle with legs \\(9\\,\\mathrm{cm}\\) and \\(12\\,\\mathrm{cm}\\), find the hypotenuse, inradius, and circumradius.",
        "Two parallel lines are cut by a transversal. If one interior angle is \\(68^{\\circ}\\), determine all remaining angle measures and illustrate them.",
        "Construct the circumcenter and orthocenter of an acute triangle and measure the distance between them.",
        "A chord of a circle of radius \\(13\\,\\mathrm{cm}\\) is \\(10\\,\\mathrm{cm}\\) long. Find its distance from the center.",
        "A sector has radius \\(14\\,\\mathrm{cm}\\) and central angle \\(120^{\\circ}\\). Find its arc length and area.",
        "A cylinder has radius \\(4\\,\\mathrm{cm}\\) and height \\(15\\,\\mathrm{cm}\\). Find its curved surface area, total surface area, and volume.",
        "A cone has radius \\(6\\,\\mathrm{cm}\\) and height \\(8\\,\\mathrm{cm}\\). Find its slant height, total surface area, and volume.",
        "A solid consists of a cylinder of radius \\(3\\,\\mathrm{cm}\\) and height \\(10\\,\\mathrm{cm}\\) topped by a hemisphere. Find its total volume and exposed surface area.",
        "A sphere of radius \\(3\\,\\mathrm{cm}\\) is melted into smaller spheres of radius \\(1\\,\\mathrm{cm}\\). Find the number of smaller spheres and verify using 3D models.",
    ]),
    ("SCHOOL MATHEMATICS", "6. Trigonometry and Applications", "Trigonometry / Solver / 2D Graph", [
        "Evaluate exactly: \\(\\sin 30^{\\circ}+\\cos 60^{\\circ}+\\tan 45^{\\circ}\\).",
        "If \\(\\sin\\theta=3/5\\) and \\(\\theta\\) is acute, find \\(\\cos\\theta\\), \\(\\tan\\theta\\), and \\(\\sec\\theta\\).",
        "Verify \\(\\frac{1-\\cos 2\\theta}{2}=\\sin^{2}\\theta\\) symbolically and with the identity visualizer.",
        "Solve \\(2\\sin x=1\\) for \\(0^{\\circ}\\leq x\\leq 360^{\\circ}\\) and mark the solutions on a graph.",
        "Solve \\(\\cos 2x=\\sin x\\) for \\(0^{\\circ}\\leq x\\leq 360^{\\circ}\\).",
        "Plot \\(y=2\\sin(3x-\\pi/2)+1\\) and identify amplitude, period, phase shift, and vertical shift.",
        "Evaluate \\(h=50\\tan 38^{\\circ}\\) to find the tower height in metres.",
        "Solve \\(\\tan 32^{\\circ}=24/d\\) for the horizontal distance \\(d\\) in metres.",
        "Evaluate \\(c=\\sqrt{8^{2}+11^{2}-2(8)(11)\\cos47^{\\circ}}\\) and \\(K=\\frac{1}{2}(8)(11)\\sin47^{\\circ}\\).",
        "Given \\(a=7\\), \\(A=35^{\\circ}\\), and \\(B=68^{\\circ}\\), evaluate \\(C=180^{\\circ}-A-B\\), \\(b=a\\sin B/\\sin A\\), and \\(c=a\\sin C/\\sin A\\).",
    ]),
    ("SCHOOL MATHEMATICS", "7. Sequences, Series, Counting, and Probability", "Solver / Spreadsheet / Probability", [
        "Find the \\(25\\)th term and sum of the first \\(25\\) terms of the arithmetic progression \\(7,12,17,\\ldots\\).",
        "The \\(8\\)th term of an arithmetic progression is \\(31\\), and the \\(15\\)th term is \\(59\\). Find its first term and common difference.",
        "Find the \\(10\\)th term and sum of the first \\(10\\) terms of the geometric progression \\(3,6,12,\\ldots\\).",
        "Find the sum to infinity of \\(12+6+3+\\cdots\\), and display partial sums in a spreadsheet.",
        "How many four-digit numbers can be formed from \\(1,2,3,4,5,6\\) without repetition? How many are even?",
        "From \\(10\\) students, how many committees of \\(4\\) can be formed if two specified students cannot serve together?",
        "A bag contains \\(5\\) red, \\(4\\) blue, and \\(3\\) green balls. Find the probability of drawing two red balls without replacement.",
        "Two fair dice are thrown. Find the probability that the sum is prime and compare it with a simulation.",
        "A card is drawn from a standard deck. Find \\(P(\\text{king or heart})\\) and \\(P(\\text{king}\\mid\\text{face card})\\).",
        "Evaluate \\(P(D|+)=0.95(0.02)/[0.95(0.02)+0.10(0.98)]\\).",
    ]),
    ("SCHOOL MATHEMATICS", "8. Statistics and Data Interpretation", "Statistics / Spreadsheet / 2D Graph", [
        "For \\(4,6,7,7,8,10,13,15\\), find mean, median, mode, range, variance, and standard deviation.",
        "Find the quartiles, interquartile range, and outliers of \\(5,7,8,9,10,12,13,15,18,30\\), then draw a box plot.",
        "The values \\(10,20,30,40,50\\) have frequencies \\(3,5,8,6,2\\). Find the weighted mean and standard deviation.",
        "Create a histogram for class intervals \\(0-10,10-20,20-30,30-40,40-50\\) with frequencies \\(4,9,15,10,2\\).",
        "For points \\((1,2),(2,3),(3,5),(4,7),(5,11)\\), calculate the correlation coefficient and fit a linear regression model.",
        "Fit a quadratic regression to \\((0,1),(1,4),(2,9),(3,16),(4,25)\\) and identify the exact generating function.",
        "Evaluate the z-score directly using \\(z=(86-68)/12\\).",
        "For \\(X\\sim\\mathrm{Binomial}(20,0.3)\\), find \\(P(X=5)\\), \\(P(X\\leq 5)\\), mean, and variance.",
        "For \\(X\\sim N(70,10^{2})\\), find \\(P(60<X<85)\\) and the \\(90\\)th percentile.",
        "Evaluate the interval \\(52\\pm1.96(9/\\sqrt{36})\\) for the population mean.",
    ]),
    ("ENGINEERING MATHEMATICS", "9. Limits, Continuity, and Differential Calculus", "Solver / CAS / 2D Graph", [
        "Evaluate \\(\\lim_{x\\to 0}\\frac{\\sin 5x}{x}\\).",
        "Evaluate \\(\\lim_{x\\to 2}\\frac{x^{2}-4}{x-2}\\) and show the removable discontinuity graphically.",
        "Determine whether \\(f(x)=\\frac{x^{2}-1}{x-1}\\) can be made continuous at \\(x=1\\), and find the required value.",
        "Differentiate \\(y=x^{3}e^{2x}\\) and simplify the result.",
        "Find \\(\\frac{dy}{dx}\\) if \\(x^{2}+xy+y^{2}=7\\), then calculate the slope at \\((1,2)\\).",
        "Find the first and second derivatives of \\(f(x)=\\ln(x^{2}+1)\\sin x\\).",
        "Find the tangent and normal to \\(y=x^{3}-3x+1\\) at \\(x=2\\), and plot all three lines.",
        "Find and classify the stationary points of \\(f(x)=x^{4}-4x^{2}+3\\).",
        "Use Newton's method with \\(x_{0}=1\\) to approximate a root of \\(x^{3}-x-1=0\\) to six decimal places.",
        "Minimize \\(S(r)=2\\pi r^{2}+1000/r\\) for \\(r>0\\), then evaluate \\(h=500/(\\pi r^{2})\\).",
    ]),
    ("ENGINEERING MATHEMATICS", "10. Integral Calculus and Applications", "Solver / CAS / 2D Graph", [
        "Evaluate \\(\\int(3x^{4}-2x+5)\\,dx\\).",
        "Evaluate \\(\\int x e^{x}\\,dx\\) using integration by parts and verify by differentiation.",
        "Evaluate \\(\\int\\frac{2x+3}{x^{2}+3x+5}\\,dx\\).",
        "Evaluate \\(\\int\\frac{1}{x^{2}+4}\\,dx\\) exactly.",
        "Evaluate \\(\\int_{0}^{\\pi/2}\\sin^{3}x\\,dx\\).",
        "Find the area enclosed by \\(y=x^{2}\\) and \\(y=2x\\).",
        "Find the volume generated when the region under \\(y=\\sqrt{x}\\), \\(0\\leq x\\leq 4\\), is revolved about the x-axis.",
        "Find the arc length of \\(y=\\frac{2}{3}x^{3/2}\\) from \\(x=0\\) to \\(x=3\\).",
        "Evaluate the improper integral \\(\\int_{1}^{\\infty}\\frac{1}{x^{2}}\\,dx\\) and state whether it converges.",
        "Use numerical integration to approximate \\(\\int_{0}^{1}e^{-x^{2}}\\,dx\\), then compare with the app's higher-precision result.",
    ]),
    ("ENGINEERING MATHEMATICS", "11. Multivariable and Vector Calculus", "CAS / Solver / 3D Graph", [
        "For \\(f(x,y)=x^{2}y+e^{xy}\\), find \\(f_{x}\\), \\(f_{y}\\), and the gradient at \\((1,0)\\).",
        "Find all second-order partial derivatives of \\(f(x,y)=x^{3}y^{2}+\\sin(xy)\\).",
        "Find and classify the critical points of \\(f(x,y)=x^{2}+y^{2}-4x+6y+5\\), and display the surface.",
        "Find the directional derivative of \\(f=x^{2}y+yz^{2}\\) at \\((1,2,-1)\\) in the direction of \\(\\mathbf{v}=\\langle2,-1,2\\rangle\\).",
        "Find the tangent plane and normal line to \\(z=x^{2}+y^{2}\\) at \\((1,2,5)\\).",
        "For \\(\\mathbf{F}=\\langle x^{2}y,yz^{2},xz\\rangle\\), calculate divergence and curl.",
        "Determine whether \\(\\mathbf{F}=\\langle2xy+x,x^{2}+2y\\rangle\\) is conservative, and find a potential function.",
        "Evaluate \\(\\int_{0}^{1}\\int_{0}^{2}(x+2y)\\,dy\\,dx\\).",
        "Evaluate \\(\\iint_{R}(x^{2}+y^{2})\\,dA\\) over the disk \\(x^{2}+y^{2}\\leq 4\\) using polar coordinates.",
        "Find the maximum and minimum of \\(f(x,y)=xy\\) subject to \\(x^{2}+y^{2}=1\\) using Lagrange multipliers.",
    ]),
    ("ENGINEERING MATHEMATICS", "12. Matrices and Linear Algebra", "Matrices / Solver / CAS", [
        "For \\(A=\\begin{bmatrix}2&1\\\\3&4\\end{bmatrix}\\) and \\(B=\\begin{bmatrix}1&-1\\\\0&2\\end{bmatrix}\\), calculate \\(A+B\\), \\(AB\\), and \\(BA\\).",
        "Find the determinant and inverse of \\(A=\\begin{bmatrix}4&1\\\\2&3\\end{bmatrix}\\), then verify \\(AA^{-1}=I\\).",
        "Solve \\(2x+y-z=8\\), \\(-3x-y+2z=-11\\), and \\(-2x+y+2z=-3\\) by row reduction.",
        "Find the rank and reduced row-echelon form of \\(\\begin{bmatrix}1&2&3\\\\2&4&7\\\\1&1&2\\end{bmatrix}\\).",
        "Find the eigenvalues and eigenvectors of \\(A=\\begin{bmatrix}3&1\\\\0&2\\end{bmatrix}\\).",
        "Diagonalize \\(A=\\begin{bmatrix}4&1\\\\2&3\\end{bmatrix}\\), if possible, and use the result to calculate \\(A^{5}\\).",
        "Determine whether the vectors \\(\\langle1,2,3\\rangle\\), \\(\\langle2,4,6\\rangle\\), and \\(\\langle0,1,1\\rangle\\) are linearly independent.",
        "Apply the Gram-Schmidt process to \\(\\mathbf{v}_{1}=\\langle1,1,0\\rangle\\) and \\(\\mathbf{v}_{2}=\\langle1,0,1\\rangle\\).",
        "Find the least-squares line \\(y=mx+c\\) for \\((0,1),(1,2),(2,2),(3,4)\\) using matrix normal equations.",
        "Find the matrix of a \\(45^{\\circ}\\) rotation followed by reflection in the x-axis, and apply it to \\(\\langle2,1\\rangle\\).",
    ]),
    ("ENGINEERING MATHEMATICS", "13. Ordinary Differential Equations", "Solver / CAS / 2D Graph", [
        "Solve \\(\\frac{dy}{dx}=3x^{2}\\) with \\(y(0)=4\\).",
        "Solve the separable equation \\(\\frac{dy}{dx}=xy\\) with \\(y(0)=2\\).",
        "Solve \\(\\frac{dy}{dx}+2y=e^{-x}\\) with \\(y(0)=1\\).",
        "Solve the Bernoulli equation \\(\\frac{dy}{dx}+y=xy^{2}\\).",
        "Solve \\(y^{\\prime\\prime}-5y^{\\prime}+6y=0\\) with \\(y(0)=1\\), \\(y^{\\prime}(0)=0\\).",
        "Solve \\(y^{\\prime\\prime}+4y=8\\cos 2x\\) and identify the resonance behavior.",
        "Solve the Cauchy-Euler equation \\(x^{2}y^{\\prime\\prime}-3xy^{\\prime}+4y=0\\).",
        "Solve the system \\(x^{\\prime}=3x+y\\), \\(y^{\\prime}=x+3y\\) with \\(x(0)=1\\), \\(y(0)=0\\).",
        "Model Newton's cooling law \\(T^{\\prime}=-k(T-25)\\) when \\(T(0)=90\\) and \\(T(10)=60\\), then find \\(T(20)\\).",
        "A mass-spring system satisfies \\(x^{\\prime\\prime}+4x=0\\), \\(x(0)=0.1\\), \\(x^{\\prime}(0)=0\\). Find and graph the displacement.",
    ]),
    ("ENGINEERING MATHEMATICS", "14. Complex Numbers, Series, and Transforms", "Calculator / CAS / 2D Graph", [
        "Simplify \\((3+4i)(2-i)\\) and express the result in rectangular and polar forms.",
        "Find all fourth roots of \\(16\\) and display them on the complex plane.",
        "Use De Moivre's theorem to calculate \\((1+i)^{10}\\) exactly.",
        "Solve \\(z^{3}=8i\\) and plot all roots on an Argand diagram.",
        "Find the Taylor polynomial of degree \\(5\\) for \\(e^{x}\\) about \\(x=0\\), and compare it with \\(e^{x}\\) on \\([-2,2]\\).",
        "Find the Maclaurin series of \\(\\sin x\\) through the term in \\(x^{7}\\), and graph the successive approximations.",
        "Determine the radius and interval of convergence of \\(\\sum_{n=1}^{\\infty}\\frac{x^{n}}{n}\\).",
        "Find the Fourier series coefficients for \\(f(x)=x\\) on \\((-\\pi,\\pi)\\), and display the first three harmonics.",
        "Find \\(\\mathcal{L}\\{t^{2}e^{3t}\\}\\) and state its region of convergence.",
        "Use Laplace transforms to solve \\(y^{\\prime\\prime}+y=1\\), \\(y(0)=0\\), \\(y^{\\prime}(0)=1\\), then graph the solution.",
    ]),
    ("ENGINEERING MATHEMATICS", "15. 3D Graphs, Analytic Geometry, and Engineering Applications", "3D Graph / 3D Geometry / Solver", [
        "Plot \\(z=x^{2}+y^{2}\\), identify its vertex and symmetry, and display contour curves for \\(z=1,4,9\\).",
        "Plot the saddle surface \\(z=x^{2}-y^{2}\\), find the traces in the planes \\(x=0\\), \\(y=0\\), and \\(z=0\\), and classify the origin.",
        "Plot the sphere \\(x^{2}+y^{2}+z^{2}=16\\) and calculate its surface area and volume.",
        "Plot the ellipsoid \\(x^{2}/9+y^{2}/4+z^{2}=1\\) and identify its semi-axes and intercepts.",
        "Plot the cone \\(z^{2}=x^{2}+y^{2}\\) and intersect it with the plane \\(z=3\\). Find the resulting curve and its area.",
        "Plot the parametric helix \\(x=2\\cos t\\), \\(y=2\\sin t\\), \\(z=t\\) for \\(0\\leq t\\leq 4\\pi\\), and find its arc length.",
        "Plot the parametric torus \\(x=(3+\\cos v)\\cos u\\), \\(y=(3+\\cos v)\\sin u\\), \\(z=\\sin v\\), and identify its major and minor radii.",
        "Find the angle between the planes \\(2x-y+2z=5\\) and \\(x+2y-2z=3\\).",
        "Find the shortest distance from \\(P(2,-1,4)\\) to the plane \\(3x-4y+12z-7=0\\), and illustrate the normal segment.",
        "For \\(z=\\sin x+\\cos y\\), find the gradient and tangent plane at \\((0,0,1)\\), then use the 3D trace and gradient controls to visualize them.",
    ]),
]


assert len(sections) == 15
assert all(len(qs) == 10 for _, _, _, qs in sections)
assert sum(len(qs) for _, _, _, qs in sections) == 150


def balanced(text, start):
    if start >= len(text) or text[start] != "{":
        return None
    depth = 0
    for index in range(start, len(text)):
        if text[index] == "{": depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0: return text[start + 1:index], index + 1
    return None


def one_arg(text, command, formatter):
    cursor = 0
    while True:
        start = text.find(command, cursor)
        if start < 0: return text
        group = balanced(text, start + len(command))
        if group is None:
            cursor = start + len(command)
            continue
        replacement = formatter(group[0])
        text = text[:start] + replacement + text[group[1]:]
        cursor = start + len(replacement)


def two_arg(text, command, formatter):
    cursor = 0
    while True:
        start = text.find(command, cursor)
        if start < 0: return text
        first = balanced(text, start + len(command))
        if first is None:
            cursor = start + len(command)
            continue
        second = balanced(text, first[1])
        if second is None:
            cursor = first[1]
            continue
        replacement = formatter(first[0], second[0])
        text = text[:start] + replacement + text[second[1]:]
        cursor = start + len(replacement)


def math_markup(source):
    s = source.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    matrix = re.compile(r"\\begin\{bmatrix\}(.+?)\\end\{bmatrix\}")
    s = matrix.sub(lambda m: "[" + "; ".join("[" + ", ".join(c.strip() for c in row.split("&amp;")) + "]" for row in m.group(1).split(r"\\")) + "]", s)
    for _ in range(8):
        old = s
        s = two_arg(s, r"\frac", lambda a, b: f"({a})/({b})")
        s = one_arg(s, r"\sqrt", lambda a: f"√({a})")
        if s == old: break
    for cmd, fmt in [(r"\mathrm", lambda a: a), (r"\mathbf", lambda a: a), (r"\vec", lambda a: a + "⃗"), (r"\text", lambda a: a), (r"\mathcal", lambda a: a)]:
        s = one_arg(s, cmd, fmt)
    replacements = {
        r"\pi":"π", r"\theta":"θ", r"\alpha":"α", r"\lambda":"λ", r"\sigma":"σ", r"\circ":"°",
        r"\infty":"∞", r"\leq":"≤", r"\geq":"≥", r"\neq":"≠", r"\pm":"±",
        r"\times":"×", r"\cdot":"·", r"\to":"→", r"\prime":"′", r"\ldots":"…", r"\sim":"∼", r"\lim":"lim", r"\mid":" | ",
        r"\sin":"sin", r"\cos":"cos", r"\tan":"tan", r"\sec":"sec", r"\ln":"ln", r"\log":"log",
        r"\int":"∫", r"\iint":"∬", r"\sum":"Σ", r"\operatorname":"", r"\langle":"(", r"\rangle":")",
        r"\le":"≤", r"\ge":"≥", r"\,":" ", r"\;":" ", r"\\":"; ",
    }
    for old, new in replacements.items(): s = s.replace(old, new)
    s = re.sub(r"\^\{([^{}]+)\}", r"<super>\1</super>", s)
    s = re.sub(r"_\{([^{}]+)\}", r"<sub>\1</sub>", s)
    s = re.sub(r"\^([A-Za-z0-9+\-′]+)", r"<super>\1</super>", s)
    s = re.sub(r"_([A-Za-z0-9+\-]+)", r"<sub>\1</sub>", s)
    s = s.replace(r"\{", "{").replace(r"\}", "}").replace("{", "").replace("}", "")
    return f'<font name="Contest">{s}</font>'


def rich(text):
    parts = re.split(r"(\\\(.+?\\\))", text)
    return "".join(math_markup(p[2:-2]) if p.startswith(r"\(") and p.endswith(r"\)") else html.escape(p) for p in parts)


class Rule(Flowable):
    def __init__(self, width, color=GOLD, thickness=1.3):
        super().__init__(); self.width=width; self.height=3; self.color=color; self.thickness=thickness
    def draw(self):
        self.canv.setStrokeColor(self.color); self.canv.setLineWidth(self.thickness); self.canv.line(0,1.5,self.width,1.5)


PAGE_W, PAGE_H = A4
MX, MT, MB = 17*mm, 18*mm, 17*mm
title = ParagraphStyle("title", fontName="Contest-Bold", fontSize=27, leading=33, textColor=NAVY, alignment=TA_CENTER, spaceAfter=10)
subtitle = ParagraphStyle("subtitle", fontName="Contest", fontSize=11, leading=16, textColor=MUTED, alignment=TA_CENTER, spaceAfter=12)
band = ParagraphStyle("band", fontName="Contest-Bold", fontSize=8.5, leading=14, textColor=colors.white, backColor=NAVY, leftIndent=7, rightIndent=7, spaceBefore=5, spaceAfter=0, keepWithNext=True)
section = ParagraphStyle("section", fontName="Contest-Bold", fontSize=14, leading=25, textColor=colors.white, backColor=BLUE, leftIndent=8, rightIndent=8, spaceBefore=0, spaceAfter=4, keepWithNext=True)
tools = ParagraphStyle("tools", fontName="Contest", fontSize=7.5, leading=10, textColor=CYAN, spaceAfter=5, keepWithNext=True)
question = ParagraphStyle("question", fontName="Contest", fontSize=9.2, leading=13.2, textColor=INK, leftIndent=17, firstLineIndent=-17, rightIndent=2, spaceAfter=6.5)
small = ParagraphStyle("small", fontName="Contest", fontSize=9, leading=13, textColor=MUTED, spaceAfter=5)
toc = ParagraphStyle("toc", fontName="Contest", fontSize=9.1, leading=13.5, textColor=INK, leftIndent=7, spaceAfter=2)


def page_decor(canvas, doc):
    canvas.saveState(); canvas.setFillColor(NAVY); canvas.rect(0,PAGE_H-11*mm,PAGE_W,11*mm,fill=1,stroke=0)
    canvas.setFont("Contest-Bold",8); canvas.setFillColor(colors.white); canvas.drawString(MX,PAGE_H-7.2*mm,"AIEXPLORER MATHEMATICS CONTEST - TRY THESE")
    canvas.setFont("Contest",8); canvas.drawRightString(PAGE_W-MX,PAGE_H-7.2*mm,"150 mathematics questions - Questions only")
    canvas.setStrokeColor(colors.HexColor("#C8DDE8")); canvas.line(MX,12*mm,PAGE_W-MX,12*mm)
    canvas.setFillColor(MUTED); canvas.setFont("Contest",7.4); canvas.drawString(MX,7.5*mm,"School Syllabus and Engineering Mathematics")
    canvas.drawRightString(PAGE_W-MX,7.5*mm,f"Page {doc.page}"); canvas.restoreState()


frame = Frame(MX,MB,PAGE_W-2*MX,PAGE_H-MT-MB,leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)
doc = BaseDocTemplate(str(BUILD_OUTPUT), pagesize=A4, title="Try These - 150 Direct-Entry Mathematics Questions", author="AIExplorer Contest Team", subject="Direct-entry mathematics contest questions solvable with AIExplorer")
doc.addPageTemplates(PageTemplate(id="math",frames=[frame],onPageEnd=page_decor))

story = [Spacer(1,22*mm), Paragraph("TRY THESE",title), Paragraph("150 Direct-Entry School and Engineering Mathematics Questions",subtitle), Spacer(1,2*mm), Rule(PAGE_W-2*MX), Spacer(1,8*mm),
         Paragraph("Every problem supplies the mathematical expression, equation, dataset, construction, or graph directly for use in Solver, CAS, Calculator, 2D and 3D Graphs, Geometry, Trigonometry, Matrices, Probability, Statistics, Spreadsheet, or Number Theory.", ParagraphStyle("intro",parent=small,fontSize=11,leading=17,alignment=TA_CENTER,textColor=INK)), Spacer(1,7*mm),
         Paragraph("Contestant details", ParagraphStyle("details",parent=section,alignment=TA_CENTER)), Paragraph("Name: ____________________________________________",subtitle), Paragraph("School / College: _________________________________",subtitle), Paragraph("Team: __________________  Category: _______________",subtitle), Spacer(1,5*mm),
         Paragraph("Questions only. Answers and marking schemes will be issued separately.", ParagraphStyle("only",parent=small,fontName="Contest-Bold",alignment=TA_CENTER,textColor=BLUE)), PageBreak(),
         Paragraph("Coverage",section)]

for i, (level, heading, workspace, qs) in enumerate(sections,1):
    start=(i-1)*10+1; end=i*10
    story.append(Paragraph(f"{i:02d}. {html.escape(heading.split('. ',1)[1])} <font color='#5D6E7D'>Questions {start}-{end}</font>",toc))
story += [Spacer(1,6*mm), Paragraph("Contest directions",section), Paragraph("Type the displayed mathematical input - not the complete instruction sentence - in the suggested workspace. No natural-language interpretation is required. Show exact values where possible, use graphs or constructions when requested, and retain equations, steps, and visual evidence for judging.",small), PageBreak()]

number=1
for section_index,(level,heading,workspace,qs) in enumerate(sections):
    story.append(Paragraph(level,band)); story.append(Paragraph(html.escape(heading),section)); story.append(Paragraph(f"Suggested app workspaces: {html.escape(workspace)}",tools))
    for text in qs:
        tag = "School" if number <= 80 else "Engineering"
        color = "#16856B" if tag == "School" else "#A65A12"
        story.append(KeepTogether([Paragraph(f"<b>{number}.</b> {rich(text)} <font name='Contest-Bold' color='{color}' size='7.3'>[{tag}]</font>",question)]))
        number += 1
story += [Spacer(1,7*mm),Rule(PAGE_W-2*MX),Spacer(1,4*mm),Paragraph("End of question bank - 150 of 150",ParagraphStyle("end",parent=subtitle,fontName="Contest-Bold",textColor=NAVY))]
assert number == 151
doc.build(story)
active_output = BUILD_OUTPUT
try:
    os.replace(BUILD_OUTPUT, OUTPUT)
    active_output = OUTPUT
except PermissionError:
    # Keep the fully built replacement available for validation until the open viewer releases the target.
    pass

# Structural and content validation.
with pdfplumber.open(active_output) as pdf:
    extracted="\n".join(page.extract_text() or "" for page in pdf.pages)
    page_count=len(pdf.pages)
assert extracted.count("[School]")==80
assert extracted.count("[Engineering]")==70
assert "instrumentation" not in extracted.lower() and "actual crash" not in extracted.lower()
assert "End of question bank - 150 of 150" in extracted
assert not re.findall(r"\\[A-Za-z]+",extracted)

# Render every page and create a contact sheet for visual inspection.
document=pdfium.PdfDocument(str(active_output)); thumbs=[]
for index in range(len(document)):
    image=document[index].render(scale=1.2).to_pil().convert("RGB")
    image.save(RENDER_DIR/f"page-{index+1:02d}.png",quality=92)
    thumb=image.copy(); thumb.thumbnail((310,438)); tile=Image.new("RGB",(330,478),"white"); tile.paste(thumb,((330-thumb.width)//2,22)); ImageDraw.Draw(tile).text((10,452),f"Page {index+1}",fill="#172332"); thumbs.append(tile)
sheet=Image.new("RGB",(1320,math.ceil(len(thumbs)/4)*478),"#DDE8EE")
for index,thumb in enumerate(thumbs): sheet.paste(thumb,((index%4)*330,(index//4)*478))
sheet.save(RENDER_DIR/"contact-sheet.png",quality=92)
print(f"{active_output} | questions=150 | pages={page_count} | school=80 | engineering=70")
