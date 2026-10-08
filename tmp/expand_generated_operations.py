from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/GeneratedMathRounds.kt')
s=p.read_text(encoding='utf-8')
a=s.index('        "decimal-harbor" ->')
b=s.index('        "function-machine" ->',a)
s=s[:a]+'''        "decimal-harbor" -> {
            val x=a*10+c; val y=b*10+c
            when(skill) {
                0 -> answer("Decimal cargo", "Add ${roundNumber(x/10.0)} + ${roundNumber(y/10.0)}.", (x+y)/10.0,
                    "Align place values. $x tenths + $y tenths = ${x+y} tenths, or ${roundNumber((x+y)/10.0)}.", "Align decimal points before combining place values.", listOf(x/10.0,y/10.0), .1)
                1 -> answer("Cargo difference", "Subtract ${roundNumber((x+y)/10.0)} − ${roundNumber(y/10.0)}.", x/10.0,
                    "Subtract aligned tenths: ${x+y} − $y = $x tenths, or ${roundNumber(x/10.0)}.", "Align decimal points before subtracting.", listOf((x+y)/10.0,y/10.0), .1)
                2 -> answer("Decimal product", "Multiply ${roundNumber(a/10.0)} × ${roundNumber(b/10.0)}.", a*b/100.0,
                    "Multiplying tenths by tenths produces hundredths. The product is ${a*b} hundredths, or ${roundNumber(a*b/100.0)}.", "Multiply the whole-number digits, then restore both decimal places.", listOf(a/10.0,b/10.0), .01)
                3 -> integer("Channel division", "Divide ${roundNumber(a*c/10.0)} by ${roundNumber(c/10.0)}.", a,
                    "Scale dividend and divisor by ten. The equivalent quotient ${a*c} ÷ $c equals $a.", "Multiply both numbers by the same power of ten.", listOf(a*c,c))
                4 -> answer("Harbor rounding", "Round ${roundNumber((a*100+b)/100.0)} to ONE decimal place.", (a*10+(b+5)/10)/10.0,
                    "Inspect the hundredths digit. Five or more raises the tenths digit; otherwise retain it. Keep one decimal place.", "Use the hundredths digit to decide how the tenths should round.", listOf((a*100+b)/100.0), .1)
                else -> answer("Place-value shift", "Multiply ${roundNumber(x/100.0)} by 100.", x.toDouble(),
                    "Multiplying by 100 moves each digit two places to the left: ${roundNumber(x/100.0)} × 100 = $x.", "Track the place value of each digit.", listOf(x/100.0))
            }
        }
        "math-market" -> {
            val price=a*100+b; val budget=(a+c)*100; val change=budget-price
            when(skill%3) {
                0 -> answer("Checkout change", "Pay ₹${roundNumber(budget/100.0)} for an item costing ₹${roundNumber(price/100.0)}. Enter change in rupees.", change/100.0,
                    "Work in paise to avoid rounding: $budget − $price = $change paise. Convert to ₹${roundNumber(change/100.0)}.", "Subtract the cost from the payment; keep decimal places aligned.", listOf(budget/100.0,price/100.0), .1)
                1 -> answer("Basket total", "Buy $c notebooks at ₹${roundNumber(price/100.0)} each. Enter the total in rupees.", price*c/100.0,
                    "Multiply the unit price by quantity: $price paise × $c = ${price*c} paise. Convert this to rupees.", "Multiply price per item by the number of items.", listOf(price/100.0,c.toDouble()), .1)
                else -> { val prices=List(6+tier){n(2,limit)}; val items=prices.shuffled(random).take(2+tier)
                    build("Exact-budget basket", "Spend exactly ₹${items.sum()} on ${items.size} items. Each price tile represents one available item.", items.sum(), prices, items.size) }
            }
        }
        "integer-expedition" -> {
            val x=n(-limit,limit); val y=n(-limit,limit)
            when(skill%3) {
                0 -> integer("Signed journey", "Start at $x on a number line and move by $y. Where do you finish?", x+y,
                    "Combine signed displacements: $x + ($y) = ${x+y}. Negative moves travel left and positive moves travel right.", "Track the direction of each signed move.", listOf(x,y))
                1 -> integer("Integer difference", "Calculate $x − ($y).", x-y,
                    "Subtracting a signed number means adding its opposite: $x − ($y) = ${x-y}.", "Change subtraction into addition of the opposite.", listOf(x,y))
                else -> integer("Signed product", "Calculate $x × ($y).", x*y,
                    "Multiply the magnitudes, then use the signs. Matching signs make a nonnegative product, unlike signs make a negative product: ${x*y}.", "Determine the product's sign before multiplying magnitudes.", listOf(x,y))
            }
        }
        "percent-studio" -> {
            val p=n(1,19)*5; val base=n(2,limit)*20; val part=p*base/100
            when(skill%3) {
                0 -> integer("Percent studio", "Find $p% of $base.", part,
                    "A percent is a fraction of one hundred: ($p ÷ 100) × $base = $part.", "Convert the percentage to a fraction of one hundred.", listOf(p,base))
                1 -> integer("Discount studio", "An item costs ₹$base before a $p% discount. Enter its sale price in rupees.", base-part,
                    "The discount is $p% of $base, or ₹$part. Subtract it from the original price to obtain ₹${base-part}.", "Find the discount first, then subtract it from the original price.", listOf(p,base))
                else -> integer("Reverse percentage", "$p% of an original amount equals $part. Find the original amount.", base,
                    "Undo the percentage scale: $part × 100 ÷ $p = $base. This checks by applying the original percentage.", "Divide by the percentage expressed as a fraction of one hundred.", listOf(p,part))
            }
        }
'''+s[b:]
p.write_text(s,encoding='utf-8')
