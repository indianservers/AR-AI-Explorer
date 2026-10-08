from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/GeneratedMathRounds.kt')
s=p.read_text(encoding='utf-8'); a=s.index('        "geometry-proof" ->'); b=s.index('        "calculus-climber" ->',a)
s=s[:a]+'''        "geometry-proof" -> when(skill) {
            0 -> { val first=n(15,75); val second=n(15,75)
                integer("Triangle proof", "A triangle has angles $first° and $second°. Find the third angle in degrees.", 180-first-second,
                    "Triangle angles sum to 180°. Subtract both known angles: 180 − $first − $second = ${180-first-second}°.", "Use the fixed total of a triangle's interior angles.", listOf(first,second)) }
            1 -> { val sides=n(3,12)
                integer("Polygon proof", "Find the sum of interior angles of a $sides-sided polygon, in degrees.", (sides-2)*180,
                    "Split the polygon into ${sides-2} triangles from one vertex. Each contributes 180°, for a total of ${(sides-2)*180}°.", "Count the triangles formed from one vertex.", listOf(sides)) }
            2 -> integer("Similarity proof", "Similar triangles have corresponding sides $a and ${a*c}. A second side on the small triangle is $b. Find its matching large side.", b*c,
                "The scale factor is ${a*c} ÷ $a = $c. Multiply the second side by the same factor: $b × $c = ${b*c}.", "Corresponding lengths share one scale factor.", listOf(a,c,b))
            3 -> integer("Right-triangle proof", "A right triangle has legs ${3*c} and ${4*c}. Find its hypotenuse.", 5*c,
                "Pythagoras gives hypotenuse² = ${3*c}² + ${4*c}² = ${25*c*c}. The positive square root is ${5*c}.", "Square the perpendicular sides, add, and take the positive square root.", listOf(3*c,4*c))
            4 -> integer("Circle proof", "A circle has diameter ${2*a} cm. Find its radius in centimetres.", a,
                "The diameter crosses the centre and is twice the radius. Divide ${2*a} by two to obtain $a centimetres.", "The radius is half the diameter.", listOf(2*a))
            else -> integer("Straight-line proof", "Two adjacent angles on a straight line are $a° and x°. Find x in degrees.", 180-a,
                "A straight angle totals 180°. Subtract the known angle: 180 − $a = ${180-a}°.", "Adjacent angles on a straight line total 180 degrees.", listOf(a))
        }
'''+s[b:];p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/GeneratedMathGameScreen.kt');s=p.read_text(encoding='utf-8').replace('"forge", "shapes", "optimization-arena" -> 1','"forge", "shapes", "number-bonds", "optimization-arena" -> 1');p.write_text(s,encoding='utf-8')
