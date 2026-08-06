package com.indianservers.aiexplorer.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.indianservers.aiexplorer.AppVisualTreatment
import com.indianservers.aiexplorer.LocalAppVisualEffects

enum class MathKeyboardContext {
    GENERAL,
    GRAPH_2D,
    GRAPH_3D,
    CALCULUS,
    MATRIX,
    SETS,
    STATISTICS,
    SCIENCE,
}

enum class MathKeyboardPage(val label: String) {
    BASIC("123"),
    FUNCTIONS("f(x)"),
    LETTERS("abc"),
    TRIG("trig"),
    ADVANCED("Math+"),
    SYMBOLS("αβ"),
    UNITS("units"),
    COMMANDS("…"),
}

private enum class AdvancedMathGroup(val label: String) {
    NOTATION("√x"),
    CALCULUS("∂"),
    MATRICES("▦"),
}

internal val primaryMathKeyboardPages = listOf(
    MathKeyboardPage.BASIC,
    MathKeyboardPage.FUNCTIONS,
    MathKeyboardPage.LETTERS,
    MathKeyboardPage.TRIG,
    MathKeyboardPage.ADVANCED,
    MathKeyboardPage.SYMBOLS,
    MathKeyboardPage.COMMANDS,
)

enum class MathKeyboardKeySize(val label: String, val mainHeight: Dp, val actionHeight: Dp, val fontScale: Float) {
    COMPACT("Compact", 31.dp, 27.dp, .88f),
    STANDARD("Standard", 40.dp, 32.dp, 1f),
    LARGE("Large", 48.dp, 38.dp, 1.14f),
}

object MathKeyboardPreferences {
    var beginnerMode by mutableStateOf(true)
    var keySize by mutableStateOf(MathKeyboardKeySize.COMPACT)
    var highContrast by mutableStateOf(false)
}

enum class MathKeyAction {
    INSERT,
    TOGGLE_SUPERSCRIPT,
    TOGGLE_SUBSCRIPT,
    TOGGLE_FRACTION,
    TOGGLE_ROOT,
    TOGGLE_CUBE_ROOT,
    TOGGLE_NTH_ROOT,
    TOGGLE_LOG_BASE,
}

data class MathKey(
    val label: String,
    val insertion: String = label,
    val cursorBack: Int = 0,
    val selectionLength: Int = 0,
    val description: String = label,
    val tone: MathKeyTone? = null,
    val action: MathKeyAction = MathKeyAction.INSERT,
)

enum class MathKeyTone { NUMBER, VARIABLE, OPERATOR, FUNCTION, CONSTANT, BRACKET, RELATION, CALCULUS, UNIT, GENERAL }

data class MathCommand(
    val name: String,
    val category: String,
    val template: MathKey,
    val explanation: String,
    val contexts: Set<MathKeyboardContext> = emptySet(),
)

object MathTextEditing {
    fun insert(value: TextFieldValue, key: MathKey): TextFieldValue {
        val selectionStart = value.selection.min
        val selectionEnd = value.selection.max
        val selected = value.text.substring(selectionStart, selectionEnd)
        val insertion = when {
            selected.isNotEmpty() && key.insertion.contains("%s") -> key.insertion.replace("%s", selected)
            else -> key.insertion.replace("%s", "")
        }
        val next = value.text.replaceRange(selectionStart, selectionEnd, insertion)
        val end = (selectionStart + insertion.length - key.cursorBack).coerceIn(0, next.length)
        val start = (end - key.selectionLength).coerceAtLeast(selectionStart)
        return TextFieldValue(next, TextRange(start, end))
    }

    fun backspace(value: TextFieldValue): TextFieldValue {
        val selectionStart = value.selection.min
        val selectionEnd = value.selection.max
        if (selectionStart != selectionEnd) {
            val next = value.text.removeRange(selectionStart, selectionEnd)
            return TextFieldValue(next, TextRange(selectionStart))
        }
        if (selectionStart == 0) return value
        val pairs = listOf("()", "[]", "{}", "||")
        val pair = pairs.firstOrNull {
            selectionStart < value.text.length &&
                value.text.substring(selectionStart - 1, selectionStart + 1) == it
        }
        return if (pair != null) {
            val next = value.text.removeRange(selectionStart - 1, selectionStart + 1)
            TextFieldValue(next, TextRange(selectionStart - 1))
        } else {
            val next = value.text.removeRange(selectionStart - 1, selectionStart)
            TextFieldValue(next, TextRange(selectionStart - 1))
        }
    }

    fun move(value: TextFieldValue, amount: Int): TextFieldValue {
        val position = if (amount < 0) value.selection.min else value.selection.max
        return value.copy(selection = TextRange((position + amount).coerceIn(0, value.text.length)))
    }

    fun selectedOrAll(value: TextFieldValue): String =
        if (value.selection.collapsed) value.text else value.text.substring(value.selection.min, value.selection.max)

    fun replaceSelection(value: TextFieldValue, replacement: String): TextFieldValue {
        val start = value.selection.min
        val end = value.selection.max
        val next = value.text.replaceRange(start, end, replacement)
        return TextFieldValue(next, TextRange(start + replacement.length))
    }

    fun cutSelectionOrAll(value: TextFieldValue): TextFieldValue =
        if (value.selection.collapsed) TextFieldValue("") else replaceSelection(value, "")

    fun selectAll(value: TextFieldValue): TextFieldValue =
        value.copy(selection = TextRange(0, value.text.length))

    fun clear(value: TextFieldValue) = TextFieldValue("")
}

object MathKeyboardHistory {
    val recent = mutableStateListOf<String>()
    val favourites = mutableStateListOf<String>()
    val recentSymbols = mutableStateListOf<MathKey>()

    fun remember(expression: String) {
        val clean = expression.trim()
        if (clean.isBlank()) return
        recent.remove(clean)
        recent.add(0, clean)
        while (recent.size > 8) recent.removeAt(recent.lastIndex)
    }

    fun rememberSymbol(key: MathKey) {
        val isSymbol = key.label.any { !it.isLetterOrDigit() } || key.label in setOf("pi", "theta", "lambda", "mu", "sigma")
        if (!isSymbol || key.insertion.length > 16) return
        recentSymbols.removeAll { it.insertion == key.insertion }
        recentSymbols.add(0, key)
        while (recentSymbols.size > 8) recentSymbols.removeAt(recentSymbols.lastIndex)
    }
}

internal val basicNumberPadRows = listOf(
    listOf(MathKey("7", tone = MathKeyTone.NUMBER), MathKey("8", tone = MathKeyTone.NUMBER), MathKey("9", tone = MathKeyTone.NUMBER), MathKey("÷", "/", description = "Divide operator", tone = MathKeyTone.OPERATOR)),
    listOf(MathKey("4", tone = MathKeyTone.NUMBER), MathKey("5", tone = MathKeyTone.NUMBER), MathKey("6", tone = MathKeyTone.NUMBER), MathKey("×", "*", description = "Multiply operator", tone = MathKeyTone.OPERATOR)),
    listOf(MathKey("1", tone = MathKeyTone.NUMBER), MathKey("2", tone = MathKeyTone.NUMBER), MathKey("3", tone = MathKeyTone.NUMBER), MathKey("−", "-", description = "Subtract operator", tone = MathKeyTone.OPERATOR)),
    listOf(MathKey("0", tone = MathKeyTone.NUMBER), MathKey(".", tone = MathKeyTone.NUMBER), MathKey("=", description = "Equals", tone = MathKeyTone.RELATION), MathKey("+", description = "Add operator", tone = MathKeyTone.OPERATOR)),
    listOf(
        MathKey("𝑥", "x", description = "Variable x", tone = MathKeyTone.VARIABLE),
        MathKey("𝑦", "y", description = "Variable y", tone = MathKeyTone.VARIABLE),
        MathKey("π", "pi", description = "Pi constant", tone = MathKeyTone.CONSTANT),
        MathKey("(", description = "Open parenthesis", tone = MathKeyTone.BRACKET),
        MathKey(")", description = "Close parenthesis", tone = MathKeyTone.BRACKET),
        MathKey(",", description = "Comma", tone = MathKeyTone.GENERAL),
    ),
)

private val basicKeys = basicNumberPadRows.flatten()

internal val inverseFunctionKeys = listOf(
    MathKey("sin⁻¹", "asin()", 1, description = "Inverse sine"),
    MathKey("cos⁻¹", "acos()", 1, description = "Inverse cosine"),
    MathKey("tan⁻¹", "atan()", 1, description = "Inverse tangent"),
    MathKey("sec⁻¹", "asec()", 1, description = "Inverse secant"),
    MathKey("csc⁻¹", "acsc()", 1, description = "Inverse cosecant"),
    MathKey("cot⁻¹", "acot()", 1, description = "Inverse cotangent"),
)

internal val functionKeys = listOf(
    MathKey("ln", "ln()", 1, description = "Natural logarithm"),
    MathKey("log", "log()", 1, description = "Base ten logarithm"),
    MathKey("|x|", "abs(%s)", 1, description = "Absolute value"),
    MathKey("eˣ", "exp()", 1, description = "Exponential"),
    MathKey("floor", "floor()", 1, description = "Floor"),
    MathKey("ceil", "ceil()", 1, description = "Ceiling"),
    MathKey("min", "min(,)", 2, description = "Minimum"),
    MathKey("max", "max(,)", 2, description = "Maximum"),
    MathKey("if", "if(,,)", 3, description = "Piecewise condition"),
)

private val directTrigonometryKeys = listOf(
    MathKey("sin", "sin()", 1, description = "Sine"),
    MathKey("cos", "cos()", 1, description = "Cosine"),
    MathKey("tan", "tan()", 1, description = "Tangent"),
    MathKey("sec", "sec()", 1, description = "Secant"),
    MathKey("csc", "csc()", 1, description = "Cosecant"),
    MathKey("cot", "cot()", 1, description = "Cotangent"),
)

private val directHyperbolicKeys = listOf(
    MathKey("sinh", "sinh()", 1, description = "Hyperbolic sine"),
    MathKey("cosh", "cosh()", 1, description = "Hyperbolic cosine"),
    MathKey("tanh", "tanh()", 1, description = "Hyperbolic tangent"),
    MathKey("sech", "sech()", 1, description = "Hyperbolic secant"),
    MathKey("csch", "csch()", 1, description = "Hyperbolic cosecant"),
    MathKey("coth", "coth()", 1, description = "Hyperbolic cotangent"),
)

private val inverseHyperbolicKeys = listOf(
    MathKey("sinh⁻¹", "asinh()", 1, description = "Inverse hyperbolic sine"),
    MathKey("cosh⁻¹", "acosh()", 1, description = "Inverse hyperbolic cosine"),
    MathKey("tanh⁻¹", "atanh()", 1, description = "Inverse hyperbolic tangent"),
    MathKey("sech⁻¹", "asech()", 1, description = "Inverse hyperbolic secant"),
    MathKey("csch⁻¹", "acsch()", 1, description = "Inverse hyperbolic cosecant"),
    MathKey("coth⁻¹", "acoth()", 1, description = "Inverse hyperbolic cotangent"),
)

private val trigonometryUtilityKeys = listOf(
    MathKey("θ", "theta", description = "Angle variable theta", tone = MathKeyTone.VARIABLE),
    MathKey("π", "pi", description = "Pi constant", tone = MathKeyTone.CONSTANT),
    MathKey("°", "deg", description = "Degrees", tone = MathKeyTone.UNIT),
    MathKey("rad", "rad", description = "Radians", tone = MathKeyTone.UNIT),
)

internal fun trigonometryKeys(inverse: Boolean): List<MathKey> =
    if (inverse) {
        inverseFunctionKeys + inverseHyperbolicKeys + trigonometryUtilityKeys
    } else {
        directTrigonometryKeys + directHyperbolicKeys + trigonometryUtilityKeys
    }

internal val calculusKeys = listOf(
    MathKey("d/dx", "derivative(%s,x)", 2, description = "Derivative"),
    MathKey("d²/dx²", "derivative(derivative(%s,x),x)", 4, description = "Second derivative"),
    MathKey("∂/∂x", "partial(%s,x)", 2, description = "Partial derivative"),
    MathKey("∂/∂y", "partial(%s,y)", 2, description = "Partial derivative with respect to y"),
    MathKey("∂²/∂x²", "partial(partial(%s,x),x)", 4, description = "Second partial derivative"),
    MathKey("∂²/∂x∂y", "partial(partial(%s,x),y)", 4, description = "Mixed partial derivative"),
    MathKey("∫", "integral(%s,x)", 2, description = "Indefinite integral"),
    MathKey("∬", "integral(integral(%s,x),y)", 4, description = "Double integral"),
    MathKey("∭", "integral(integral(integral(%s,x),y),z)", 6, description = "Triple integral"),
    MathKey("∫ᵃᵇ", "integral(%s,x,,)", 3, description = "Definite integral"),
    MathKey("lim", "limit(%s,x,)", 1, description = "Limit"),
    MathKey("Σ", "sum(%s,n,,)", 3, description = "Summation"),
    MathKey("Π", "product(%s,n,,)", 3, description = "Product"),
    MathKey("∮", "contour(%s,z)", 2, description = "Contour integral"),
    MathKey("∇", "gradient(%s)", 1, description = "Gradient"),
    MathKey("θ", "theta()", 1, description = "Heaviside theta"),
    MathKey("δ", "delta()", 1, description = "Dirac delta"),
    MathKey("dx"), MathKey("dy"), MathKey("dt"), MathKey("∞", "infinity"),
)

internal val advancedNotationKeys = listOf(
    MathKey("x²", "(%s)^2", description = "Square"),
    MathKey("1/x", "1/(%s)", 1, description = "Reciprocal"),
    MathKey(
        "ⁿ√",
        "nthroot(,)",
        2,
        description = "Toggle root index and radicand",
        action = MathKeyAction.TOGGLE_NTH_ROOT,
    ),
    MathKey("∞", "infinity", description = "Positive infinity", tone = MathKeyTone.CONSTANT),
    MathKey("−∞", "-infinity", description = "Negative infinity", tone = MathKeyTone.CONSTANT),
    MathKey("e", description = "Euler's number", tone = MathKeyTone.CONSTANT),
    MathKey("π", "pi", description = "Pi", tone = MathKeyTone.CONSTANT),
    MathKey("10ˣ", "10^(%s)", 1, description = "Power of ten"),
    MathKey("log₁₀", "logbase(10,)", 1, description = "Base ten logarithm"),
    MathKey("⌊x⌋", "floor()", 1, description = "Floor"),
    MathKey("⌈x⌉", "ceil()", 1, description = "Ceiling"),
    MathKey("|x|", "abs(%s)", 1, description = "Absolute value"),
    MathKey("≤x≤", "<=x<=", 3, description = "Closed range"),
    MathKey("≠", "!=", description = "Not equal", tone = MathKeyTone.RELATION),
)

internal val matrixStructureKeys = listOf(
    matrixTemplate(1, 2),
    matrixTemplate(1, 3),
    matrixTemplate(1, 4),
    matrixTemplate(2, 1),
    matrixTemplate(3, 1),
    matrixTemplate(4, 1),
    matrixTemplate(2, 2),
    matrixTemplate(2, 3),
    matrixTemplate(3, 2),
    matrixTemplate(3, 3),
    matrixTemplate(4, 4),
    MathKey("det", "det()", 1, description = "Determinant"),
    MathKey("A⁻¹", "inverse()", 1, description = "Inverse matrix"),
    MathKey("Aᵀ", "transpose()", 1, description = "Matrix transpose"),
)

private val symbolKeys = listOf(
    MathKey("α"), MathKey("β"), MathKey("γ"), MathKey("δ"),
    MathKey("θ", "theta"), MathKey("λ", "lambda"), MathKey("μ", "mu"), MathKey("σ", "sigma"),
    MathKey("φ", "phi"), MathKey("ω", "omega"), MathKey("Δ", "delta"), MathKey("π", "pi"),
    MathKey("<"), MathKey(">"), MathKey("≤", "<="), MathKey("≥", ">="),
    MathKey("≠", "!="), MathKey("≈"), MathKey("∈"), MathKey("∉"),
    MathKey("∪"), MathKey("∩"), MathKey("⊂"), MathKey("∅", "{}"),
)

private val letterKeys = ("qwertyuiopasdfghjklzxcvbnm".map { MathKey(it.toString()) } +
    listOf(
        MathKey("Space", " ", description = "Space"),
        MathKey(","), MathKey("_"),
        MathKey("[ ]", "[]", 1, description = "Square brackets", tone = MathKeyTone.BRACKET),
        MathKey("{ }", "{}", 1, description = "Braces", tone = MathKeyTone.BRACKET),
    ))

private val unitAndConstantKeys = listOf(
    MathKey("π", "pi", description = "Pi constant", tone = MathKeyTone.CONSTANT),
    MathKey("e", "e", description = "Euler's number", tone = MathKeyTone.CONSTANT),
    MathKey("∞", "infinity", description = "Infinity", tone = MathKeyTone.CONSTANT),
    MathKey("c", "299792458", description = "Speed of light in metres per second", tone = MathKeyTone.CONSTANT),
    MathKey("g₀", "9.80665", description = "Standard gravity", tone = MathKeyTone.CONSTANT),
    MathKey("G", "6.67430e-11", description = "Gravitational constant", tone = MathKeyTone.CONSTANT),
    MathKey("h", "6.62607015e-34", description = "Planck constant", tone = MathKeyTone.CONSTANT),
    MathKey("kᴮ", "1.380649e-23", description = "Boltzmann constant", tone = MathKeyTone.CONSTANT),
    MathKey("Nᴬ", "6.02214076e23", description = "Avogadro constant", tone = MathKeyTone.CONSTANT),
    MathKey("m", " m", description = "Metre", tone = MathKeyTone.UNIT),
    MathKey("cm", " cm", description = "Centimetre", tone = MathKeyTone.UNIT),
    MathKey("km", " km", description = "Kilometre", tone = MathKeyTone.UNIT),
    MathKey("s", " s", description = "Second", tone = MathKeyTone.UNIT),
    MathKey("kg", " kg", description = "Kilogram", tone = MathKeyTone.UNIT),
    MathKey("mol", " mol", description = "Mole", tone = MathKeyTone.UNIT),
    MathKey("°", " deg", description = "Degrees", tone = MathKeyTone.UNIT),
    MathKey("rad", " rad", description = "Radians", tone = MathKeyTone.UNIT),
    MathKey("μ", "mu", description = "Micro prefix", tone = MathKeyTone.UNIT),
)

private val statisticsKeys = listOf(
    MathKey("x̄", "mean()", 1, description = "Mean"),
    MathKey("med", "median()", 1, description = "Median"),
    MathKey("mode", "mode()", 1, description = "Mode"),
    MathKey("σ", "stdev()", 1, description = "Standard deviation"),
    MathKey("var", "variance()", 1, description = "Variance"),
    MathKey("n!", "factorial()", 1, description = "Factorial"),
    MathKey("nCr", "combination(,)", 2, description = "Combination"),
    MathKey("nPr", "permutation(,)", 2, description = "Permutation"),
)

private val setAndLogicKeys = listOf(
    MathKey("∈"), MathKey("∉"), MathKey("∪"), MathKey("∩"),
    MathKey("⊂"), MathKey("⊆"), MathKey("∅", "{}"),
    MathKey("¬", "not()", 1, description = "Logical not"),
    MathKey("∧", " and ", description = "Logical and"),
    MathKey("∨", " or ", description = "Logical or"),
    MathKey("⇒", " implies ", description = "Implies"),
    MathKey("⇔", " iff ", description = "If and only if"),
)

internal fun matrixTemplate(rows: Int, columns: Int): MathKey {
    val safeRows = rows.coerceIn(1, 6)
    val safeColumns = columns.coerceIn(1, 6)
    val row = List(safeColumns) { "" }.joinToString(",")
    val insertion = List(safeRows) { "[$row]" }.joinToString(prefix = "[", postfix = "]", separator = ",")
    return MathKey(
        label = "${safeRows}×${safeColumns}",
        insertion = insertion,
        cursorBack = (insertion.length - 2).coerceAtLeast(0),
        description = "Insert a $safeRows by $safeColumns matrix",
    )
}

internal fun fractionTemplate(hasSelection: Boolean): MathKey =
    if (hasSelection) {
        MathKey(
            "a⁄b",
            "(%s)/()",
            1,
            description = "Use selection as numerator and enter denominator",
            action = MathKeyAction.TOGGLE_FRACTION,
        )
    } else {
        MathKey(
            "a⁄b",
            "()/()",
            4,
            description = "Toggle fraction numerator and denominator",
            action = MathKeyAction.TOGGLE_FRACTION,
        )
    }

internal val commonMathKeys = listOf(
    MathKey("=", description = "Equals"),
    MathKey("+", description = "Add"),
    MathKey("−", "-", description = "Subtract"),
    MathKey("×", "*", description = "Multiply"),
    MathKey("÷", "/", description = "Divide"),
    MathKey("(", description = "Open parenthesis"),
    MathKey(")", description = "Close parenthesis"),
    MathKey(",", description = "Comma"),
)

internal val mathKeyboardCommands = listOf(
    MathCommand("Explicit graph", "Graph", MathKey("y=f(x)", "y=", description = "Explicit graph"), "Plot y as a function of x.", setOf(MathKeyboardContext.GRAPH_2D)),
    MathCommand("Polar graph", "Graph", MathKey("r=f(θ)", "r=", description = "Polar graph"), "Plot radius against angle.", setOf(MathKeyboardContext.GRAPH_2D)),
    MathCommand("Parametric curve", "Graph", MathKey("x(t), y(t)", "x(t)=; y(t)=", 8, description = "Parametric curve"), "Plot x and y from parameter t.", setOf(MathKeyboardContext.GRAPH_2D)),
    MathCommand("Inequality", "Graph", MathKey("y≤f(x)", "y<=", description = "Graph inequality"), "Shade values satisfying an inequality."),
    MathCommand("Point", "Geometry", MathKey("(x,y)", "(,)", 2, description = "Point"), "Insert a two-dimensional point."),
    MathCommand("Distance", "Geometry", MathKey("distance", "distance((,),(,))", 8, description = "Distance between points"), "Find the distance between two points."),
    MathCommand("Midpoint", "Geometry", MathKey("midpoint", "midpoint((,),(,))", 8, description = "Midpoint"), "Find the midpoint of a segment."),
    MathCommand("Circle", "Conics", MathKey("circle", "(x-)^2+(y-)^2=^2", 14, description = "Circle equation"), "Insert centre-radius form."),
    MathCommand("Parabola", "Conics", MathKey("parabola", "y=()*(x-)^2+", 10, description = "Parabola equation"), "Insert vertex form of a parabola."),
    MathCommand("Ellipse", "Conics", MathKey("ellipse", "(x-)^2/^2+(y-)^2/^2=1", 20, description = "Ellipse equation"), "Insert standard ellipse form."),
    MathCommand("Hyperbola", "Conics", MathKey("hyperbola", "(x-)^2/^2-(y-)^2/^2=1", 20, description = "Hyperbola equation"), "Insert standard hyperbola form."),
    MathCommand("3D point", "3D", MathKey("(x,y,z)", "(,,)", 3, description = "3D point"), "Insert a three-dimensional point.", setOf(MathKeyboardContext.GRAPH_3D)),
    MathCommand("Explicit surface", "3D", MathKey("z=f(x,y)", "z=", description = "Explicit surface"), "Plot height z from x and y.", setOf(MathKeyboardContext.GRAPH_3D)),
    MathCommand("Sphere", "3D", MathKey("Sphere", "x^2+y^2+z^2=", 0, description = "Sphere equation"), "Create a sphere equation.", setOf(MathKeyboardContext.GRAPH_3D)),
    MathCommand("Plane", "3D", MathKey("Plane", "z=()*x+()*y+", 10, description = "Plane equation"), "Insert an explicit plane."),
    MathCommand("Cylinder", "3D", MathKey("Cylinder", "x^2+y^2=^2", 2, description = "Cylinder equation"), "Insert a vertical cylinder."),
    MathCommand("Vector", "Vectors", MathKey("Vector", "vector(,,)", 3, description = "Vector"), "Insert a vector template."),
    MathCommand("Matrix 2×2", "Matrices", MathKey("[2×2]", "[[,],[,]]", 7, description = "Two by two matrix"), "Insert a 2 by 2 matrix.", setOf(MathKeyboardContext.MATRIX)),
    MathCommand("Matrix 3×3", "Matrices", MathKey("[3×3]", "[[,,],[,,],[,,]]", 13, description = "Three by three matrix"), "Insert a 3 by 3 matrix."),
    MathCommand("Determinant", "Matrices", MathKey("det", "det()", 1, description = "Determinant"), "Calculate a matrix determinant."),
    MathCommand("Inverse matrix", "Matrices", MathKey("A⁻¹", "inverse()", 1, description = "Inverse matrix"), "Find a matrix inverse."),
    MathCommand("Transpose", "Matrices", MathKey("Aᵀ", "transpose()", 1, description = "Matrix transpose"), "Transpose a matrix."),
    MathCommand("Dot product", "Vectors", MathKey("a·b", "dot(,)", 2, description = "Dot product"), "Calculate a scalar product."),
    MathCommand("Cross product", "Vectors", MathKey("a×b", "cross(,)", 2, description = "Cross product"), "Calculate a vector product."),
    MathCommand("Mean", "Statistics", MathKey("mean", "mean()", 1, description = "Mean"), "Calculate the arithmetic mean."),
    MathCommand("Median", "Statistics", MathKey("median", "median()", 1, description = "Median"), "Find the middle ordered value."),
    MathCommand("Mode", "Statistics", MathKey("mode", "mode()", 1, description = "Mode"), "Find the most frequent value."),
    MathCommand("Standard deviation", "Statistics", MathKey("stdev", "stdev()", 1, description = "Standard deviation"), "Measure data spread."),
    MathCommand("Variance", "Statistics", MathKey("variance", "variance()", 1, description = "Variance"), "Measure squared data spread."),
    MathCommand("Quartile", "Statistics", MathKey("quartile", "quartile(,)", 2, description = "Quartile"), "Find a dataset quartile."),
    MathCommand("Correlation", "Statistics", MathKey("corr", "correlation(,)", 2, description = "Correlation"), "Measure linear association."),
    MathCommand("Regression", "Statistics", MathKey("regression", "regression(,)", 2, description = "Linear regression"), "Fit a straight line to paired data."),
    MathCommand("Factorial", "Discrete Maths", MathKey("n!", "factorial()", 1, description = "Factorial"), "Count arrangements of distinct items."),
    MathCommand("Combination", "Discrete Maths", MathKey("nCr", "nCr(,)", 2, description = "Combination"), "Choose items when order does not matter."),
    MathCommand("Permutation", "Discrete Maths", MathKey("nPr", "nPr(,)", 2, description = "Permutation"), "Choose and arrange items."),
    MathCommand("Greatest common divisor", "Discrete Maths", MathKey("gcd", "gcd(,)", 2, description = "Greatest common divisor"), "Find the largest common divisor."),
    MathCommand("Least common multiple", "Discrete Maths", MathKey("lcm", "lcm(,)", 2, description = "Least common multiple"), "Find the smallest common multiple."),
    MathCommand("Modulo", "Discrete Maths", MathKey("mod", "mod(,)", 2, description = "Modulo"), "Find a division remainder."),
    MathCommand("Fibonacci", "Discrete Maths", MathKey("fib", "fibonacci()", 1, description = "Fibonacci number"), "Find a term of the Fibonacci sequence."),
    MathCommand("Binomial probability", "Probability", MathKey("Binomial", "binomial(,,,)", 4, description = "Binomial probability"), "Model successes in fixed independent trials."),
    MathCommand("Normal probability", "Probability", MathKey("Normal", "normalcdf(,,,)", 4, description = "Normal probability"), "Find probability under a normal curve."),
    MathCommand("Conditional probability", "Probability", MathKey("P(A|B)", "conditional(,,)", 3, description = "Conditional probability"), "Find probability given another event."),
    MathCommand("Expected value", "Probability", MathKey("E(X)", "expectation()", 1, description = "Expected value"), "Calculate a probability-weighted mean."),
    MathCommand("Simple interest", "Finance", MathKey("I=Prt", "()*()*()", 6, description = "Simple interest"), "Calculate interest without compounding."),
    MathCommand("Compound value", "Finance", MathKey("A=P(1+r)ⁿ", "()*((1+())^())", 12, description = "Compound value"), "Calculate compounded growth."),
    MathCommand("Present value", "Finance", MathKey("PV", "()/((1+())^())", 11, description = "Present value"), "Discount a future amount."),
    MathCommand("List", "Lists", MathKey("{…}", "{,,}", 3, description = "List"), "Insert a list of values."),
    MathCommand("Sequence", "Lists", MathKey("aₙ", "sequence(,n,,)", 4, description = "Sequence"), "Generate terms from a rule."),
    MathCommand("Sort", "Lists", MathKey("sort", "sort()", 1, description = "Sort list"), "Sort values into ascending order."),
    MathCommand("And", "Logic", MathKey("AND", " and ", description = "Logical and"), "Require both conditions."),
    MathCommand("Or", "Logic", MathKey("OR", " or ", description = "Logical or"), "Require either condition."),
    MathCommand("Not", "Logic", MathKey("NOT", "not()", 1, description = "Logical not"), "Negate a condition."),
    MathCommand("Implication", "Logic", MathKey("⇒", " implies ", description = "Logical implication"), "Insert a logical implication."),
    MathCommand("Union", "Sets", MathKey("A∪B", " union ", description = "Set union"), "Combine elements from two sets.", setOf(MathKeyboardContext.SETS)),
    MathCommand("Intersection", "Sets", MathKey("A∩B", " intersection ", description = "Set intersection"), "Keep common elements from two sets.", setOf(MathKeyboardContext.SETS)),
    MathCommand("Set difference", "Sets", MathKey("A\\B", " difference ", description = "Set difference"), "Remove elements of one set from another."),
    MathCommand("Translate", "Transformations", MathKey("translate", "translate(,,,)", 4, description = "Translation"), "Move an object by a vector."),
    MathCommand("Rotate", "Transformations", MathKey("rotate", "rotate(,,)", 3, description = "Rotation"), "Rotate an object through an angle."),
    MathCommand("Reflect", "Transformations", MathKey("reflect", "reflect(,)", 2, description = "Reflection"), "Reflect an object in a line or plane."),
    MathCommand("Minimum", "Optimisation", MathKey("min", "min()", 1, description = "Minimum"), "Find a minimum value."),
    MathCommand("Maximum", "Optimisation", MathKey("max", "max()", 1, description = "Maximum"), "Find a maximum value."),
    MathCommand("Derivative", "Calculus", calculusKeys[0], "Find the rate of change.", setOf(MathKeyboardContext.CALCULUS, MathKeyboardContext.GRAPH_2D)),
    MathCommand("Definite integral", "Calculus", calculusKeys[9], "Find accumulated signed area.", setOf(MathKeyboardContext.CALCULUS, MathKeyboardContext.GRAPH_2D)),
    MathCommand("Limit", "Calculus", calculusKeys[10], "Find the value approached by a function."),
    MathCommand("Summation", "Calculus", calculusKeys[11], "Add terms over an index range."),
    MathCommand("Piecewise function", "Functions", functionKeys.last(), "Choose an expression using a condition."),
)

internal fun filterMathCommands(
    query: String = "",
    category: String? = null,
    context: MathKeyboardContext = MathKeyboardContext.GENERAL,
): List<MathCommand> {
    val cleanQuery = query.trim().lowercase()
    val stopWords = setOf("find", "show", "draw", "plot", "calculate", "compute", "give", "me", "the", "a", "an", "of", "for", "with", "numbers", "values")
    val queryWords = cleanQuery.split(Regex("[^a-z0-9]+")).filter { it.length > 1 && it !in stopWords }.toSet()
    val categorized = mathKeyboardCommands.filter { category == null || it.category == category }
    val exactMatches = categorized.filter { cleanQuery.isNotBlank() && commandSearchText(it).contains(cleanQuery) }
    val matches = when {
        cleanQuery.isBlank() -> categorized
        exactMatches.isNotEmpty() -> exactMatches
        else -> categorized.filter { command -> queryWords.isNotEmpty() && queryWords.all { word -> word in commandSearchText(command) } }
    }
    return matches
        .sortedWith(
            compareByDescending<MathCommand> { it.contexts.isNotEmpty() && context in it.contexts }
                .thenBy { it.category }
                .thenBy { it.name },
        )
}

private val plainEnglishCommandAliases = mapOf(
    "Mean" to "average add values divide by count",
    "Median" to "middle number ordered data",
    "Mode" to "most common frequent value",
    "Standard deviation" to "spread dispersion variation from average",
    "Variance" to "squared spread dispersion",
    "Combination" to "choose select items order does not matter",
    "Permutation" to "arrange order items",
    "Greatest common divisor" to "highest common factor hcf",
    "Least common multiple" to "smallest shared multiple",
    "Explicit graph" to "draw plot function line curve",
    "Explicit surface" to "draw plot 3d function surface",
    "Definite integral" to "area under curve accumulated total between limits",
    "Derivative" to "slope tangent rate of change",
    "Distance" to "length between two points",
    "Midpoint" to "centre center halfway between points",
    "Circle" to "round shape centre center radius equation",
    "Normal probability" to "bell curve gaussian probability area",
    "Binomial probability" to "success failure repeated trials probability",
    "Simple interest" to "loan savings interest without compounding",
    "Compound value" to "growth repeated interest investment",
)

private fun commandSearchText(command: MathCommand): String = listOf(
    command.name,
    command.category,
    command.explanation,
    command.template.label,
    plainEnglishCommandAliases[command.name].orEmpty(),
).joinToString(" ").lowercase()

private fun contextualKeys(context: MathKeyboardContext): List<MathKey> = when (context) {
    MathKeyboardContext.GRAPH_2D -> listOf(
        MathKey("y=", "y="), MathKey("x"), MathKey("x²", "x^2"), MathKey("sin(x)", "sin(x)"),
        MathKey("r=", "r="), MathKey("(x,y)", "(,)", 2), MathKey("if", "if(,,)", 3),
    )
    MathKeyboardContext.GRAPH_3D -> listOf(
        MathKey("z=", "z="), MathKey("x"), MathKey("y"), MathKey("x²+y²", "x^2+y^2"),
        MathKey("sin(x)", "sin(x)"), MathKey("cos(y)", "cos(y)"),
        MathKey("√", "sqrt()", 1, description = "Toggle square-root radicand", action = MathKeyAction.TOGGLE_ROOT),
    )
    MathKeyboardContext.CALCULUS -> calculusKeys.take(7)
    MathKeyboardContext.MATRIX -> listOf(MathKey("[2×2]", "[[,],[,]]", 7), MathKey("det", "det()", 1), MathKey("⁻¹", "inverse()", 1), MathKey("T", "transpose()", 1))
    MathKeyboardContext.SETS -> listOf(MathKey("∈"), MathKey("∉"), MathKey("∪"), MathKey("∩"), MathKey("⊂"), MathKey("∅", "{}"))
    MathKeyboardContext.STATISTICS -> listOf(MathKey("mean", "mean()", 1), MathKey("median", "median()", 1), MathKey("σ", "stdev()", 1), MathKey("nCr", "nCr(,)", 2))
    MathKeyboardContext.SCIENCE -> listOf(MathKey("π", "pi"), MathKey("e"), MathKey("×10ⁿ", "*10^()", 1), MathKey("μ", "mu"), MathKey("Δ", "delta"))
    MathKeyboardContext.GENERAL -> listOf(
        MathKey("x"),
        MathKey("y"),
        MathKey("π", "pi"),
        MathKey("x²", "(%s)^2"),
        MathKey("√", "sqrt(%s)", 1, description = "Toggle square-root radicand", action = MathKeyAction.TOGGLE_ROOT),
    )
}

@Composable
fun AdaptiveMathKeyboardPopup(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    context: MathKeyboardContext,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        popupPositionProvider = MathKeyboardBottomDockPositionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        AdaptiveMathKeyboard(
            value = value,
            onValueChange = onValueChange,
            context = context,
            onDone = onDone,
            onDismiss = onDismiss,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
        )
    }
}

private object MathKeyboardBottomDockPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0),
        y = (windowSize.height - popupContentSize.height).coerceAtLeast(0),
    )
}

@Composable
fun AdaptiveMathKeyboard(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    context: MathKeyboardContext,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf(MathKeyboardPage.BASIC) }
    var showSettings by remember { mutableStateOf(false) }
    var inverseFunctions by remember { mutableStateOf(false) }
    var advancedGroup by remember { mutableStateOf(AdvancedMathGroup.NOTATION) }
    var commandQuery by remember { mutableStateOf("") }
    var commandCategory by remember { mutableStateOf<String?>(null) }
    var workingValue by remember { mutableStateOf(value) }
    var expectedParentEcho by remember { mutableStateOf<TextFieldValue?>(null) }
    val clipboard = LocalClipboardManager.current
    val undo = remember { mutableStateListOf<TextFieldValue>() }
    val redo = remember { mutableStateListOf<TextFieldValue>() }
    LaunchedEffect(value) {
        expectedParentEcho?.let { expected ->
            if (value.text == expected.text) {
                expectedParentEcho = null
                if (value.selection == expected.selection) workingValue = value
                return@LaunchedEffect
            }
        }
        if (value != workingValue) workingValue = value
    }
    val emit: (TextFieldValue) -> Unit = { next ->
        workingValue = next
        expectedParentEcho = next
        onValueChange(next)
    }
    val applyEdit: (TextFieldValue) -> Unit = { next ->
        if (next != workingValue) {
            undo.add(workingValue)
            while (undo.size > 40) undo.removeAt(0)
            redo.clear()
            emit(next)
        }
    }
    val edit: (MathKey) -> Unit = { key ->
        MathKeyboardHistory.rememberSymbol(key)
        applyEdit(
            when (key.action) {
                MathKeyAction.INSERT -> MathTextEditing.insert(workingValue, key)
                MathKeyAction.TOGGLE_SUPERSCRIPT -> StructuredMathEditing.toggleSuperscript(workingValue)
                MathKeyAction.TOGGLE_SUBSCRIPT -> StructuredMathEditing.toggleSubscript(workingValue)
                MathKeyAction.TOGGLE_FRACTION -> StructuredMathEditing.toggleFraction(workingValue, key)
                MathKeyAction.TOGGLE_ROOT -> StructuredMathEditing.toggleRoot(workingValue, key)
                MathKeyAction.TOGGLE_CUBE_ROOT -> StructuredMathEditing.toggleCubeRoot(workingValue, key)
                MathKeyAction.TOGGLE_NTH_ROOT -> StructuredMathEditing.toggleNthRoot(workingValue, key)
                MathKeyAction.TOGGLE_LOG_BASE -> StructuredMathEditing.toggleLogBase(workingValue)
            },
        )
    }
    val activeMode = StructuredMathEditing.modeAt(workingValue.text, workingValue.selection.end)
    val keys = when (page) {
        MathKeyboardPage.BASIC -> basicKeys
        MathKeyboardPage.FUNCTIONS -> if (MathKeyboardPreferences.beginnerMode) functionKeys.take(8) else functionKeys
        MathKeyboardPage.LETTERS -> letterKeys
        MathKeyboardPage.TRIG -> trigonometryKeys(inverseFunctions)
        MathKeyboardPage.ADVANCED -> when (advancedGroup) {
            AdvancedMathGroup.NOTATION -> advancedNotationKeys
            AdvancedMathGroup.CALCULUS -> calculusKeys
            AdvancedMathGroup.MATRICES -> matrixStructureKeys
        }
        MathKeyboardPage.SYMBOLS -> symbolKeys
        MathKeyboardPage.UNITS -> unitAndConstantKeys
        MathKeyboardPage.COMMANDS -> emptyList()
    }
    val visibleKeys = keys
    val visiblePages = primaryMathKeyboardPages
    val visualEffects = LocalAppVisualEffects.current
    val keyboardShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val keyboardShell = modifier.fillMaxWidth().navigationBarsPadding()
    val decoratedKeyboardShell = when {
        MathKeyboardPreferences.highContrast -> keyboardShell
            .background(Color.Black, keyboardShape)
            .border(2.dp, Color.White, keyboardShape)
        !visualEffects.enhanced -> keyboardShell
            .background(Color(0xFF07131F), keyboardShape)
            .border(1.dp, IntentMathPalette.Command.copy(alpha = .62f), keyboardShape)
        else -> {
            val surface = MaterialTheme.colorScheme.surface
            val primary = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary
            val colors = when (visualEffects.treatment) {
                AppVisualTreatment.NeonGlass -> listOf(
                    primary.copy(alpha = .16f),
                    surface,
                    secondary.copy(alpha = .10f),
                )
                AppVisualTreatment.SpectralWireframe -> listOf(
                    secondary.copy(alpha = .15f),
                    surface,
                    primary.copy(alpha = .12f),
                )
                AppVisualTreatment.Standard -> listOf(surface, surface)
            }
            keyboardShell
                .background(Brush.verticalGradient(colors), keyboardShape)
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            primary.copy(alpha = visualEffects.borderGlowAlpha),
                            secondary.copy(alpha = visualEffects.borderGlowAlpha),
                        ),
                    ),
                    keyboardShape,
                )
        }
    }
    Column(
        decoratedKeyboardShell
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f)) {
                Text(caretPreview(workingValue), color = IntentMathPalette.Number, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1)
            }
            Text(
                when (activeMode) {
                    MathInputMode.SUPERSCRIPT -> "Exponent"
                    MathInputMode.SUBSCRIPT -> "Subscript"
                    MathInputMode.NUMERATOR -> "Numerator"
                    MathInputMode.DENOMINATOR -> "Denominator"
                    MathInputMode.RADICAND -> "Inside root"
                    MathInputMode.LOG_BASE -> "Log base"
                    MathInputMode.FUNCTION_ARGUMENT -> "Argument"
                    else -> "${workingValue.selection.end + 1}/${workingValue.text.length + 1}"
                },
                color = if (activeMode == MathInputMode.BASELINE) IntentMathPalette.Muted else IntentMathPalette.Variable,
                fontSize = 8.sp,
            )
            KeyboardActionKey("Aa", "Keyboard display and clipboard tools") { showSettings = !showSettings }
            KeyboardActionKey("⌄", "Collapse math keyboard", onClick = onDismiss)
        }
        if (showSettings) {
            KeyboardAppearancePanel()
            CompactClipboardRow(
                onSelectAll = { emit(MathTextEditing.selectAll(workingValue)) },
                onCopy = {
                    MathTextEditing.selectedOrAll(workingValue).takeIf(String::isNotEmpty)?.let {
                        clipboard.setText(AnnotatedString(it))
                    }
                },
                onCut = {
                    MathTextEditing.selectedOrAll(workingValue).takeIf(String::isNotEmpty)?.let {
                        clipboard.setText(AnnotatedString(it))
                        applyEdit(MathTextEditing.cutSelectionOrAll(workingValue))
                    }
                },
                onPaste = {
                    clipboard.getText()?.text?.takeIf(String::isNotEmpty)?.let {
                        applyEdit(MathTextEditing.replaceSelection(workingValue, it))
                    }
                },
                onClear = { applyEdit(MathTextEditing.clear(workingValue)) },
            )
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            visiblePages.forEach { item ->
                KeyboardTab(item.label, page == item) { page = item }
            }
        }
        StructuredEntryStrip(
            activeMode = activeMode,
            currentSource = workingValue.text,
            currentCursor = workingValue.selection.end,
            onInsert = edit,
        )
        if (page == MathKeyboardPage.COMMANDS) {
            CommandBrowser(
                query = commandQuery,
                onQueryChange = { commandQuery = it },
                category = commandCategory,
                onCategoryChange = { commandCategory = it },
                context = context,
                onInsert = edit,
                hasEditorSelection = !workingValue.selection.collapsed,
                currentSource = workingValue.text,
                currentCursor = workingValue.selection.end,
            )
        } else if (page == MathKeyboardPage.BASIC) {
            BasicNumberPad(onInsert = edit)
        } else {
            if (page == MathKeyboardPage.ADVANCED) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AdvancedMathGroup.entries.forEach { group ->
                        KeyboardActionKey(
                            label = group.label,
                            description = when (group) {
                                AdvancedMathGroup.NOTATION -> "Advanced roots and notation"
                                AdvancedMathGroup.CALCULUS -> "Calculus structures"
                                AdvancedMathGroup.MATRICES -> "Matrix templates"
                            },
                            modifier = Modifier.weight(1f),
                            accent = if (advancedGroup == group) IntentMathPalette.Variable else IntentMathPalette.Command,
                        ) {
                            advancedGroup = group
                        }
                    }
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = when {
                    page == MathKeyboardPage.LETTERS -> 10
                    page == MathKeyboardPage.ADVANCED -> 7
                    maxWidth >= 520.dp -> 10
                    maxWidth >= 350.dp -> 9
                    else -> 7
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val firstFunctionRow = if (page == MathKeyboardPage.TRIG) {
                        visibleKeys.take((columns - 1).coerceAtLeast(1))
                    } else {
                        emptyList()
                    }
                    if (page == MathKeyboardPage.TRIG) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            KeyboardActionKey(
                                label = if (inverseFunctions) "Inv ✓" else "Inv",
                                description = "Switch between direct and inverse trigonometric functions",
                                modifier = Modifier.weight(1f),
                                accent = if (inverseFunctions) IntentMathPalette.Variable else IntentMathPalette.Function,
                            ) {
                                inverseFunctions = !inverseFunctions
                            }
                            firstFunctionRow.forEach { key ->
                                MathKeyboardKey(
                                    key = key,
                                    onClick = edit,
                                    modifier = Modifier.weight(1f),
                                    selected = false,
                                )
                            }
                            repeat(columns - firstFunctionRow.size - 1) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    val remainingKeys = if (page == MathKeyboardPage.TRIG) {
                        visibleKeys.drop(firstFunctionRow.size)
                    } else {
                        visibleKeys
                    }
                    remainingKeys.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { key ->
                                MathKeyboardKey(
                                    key = key,
                                    onClick = edit,
                                    modifier = Modifier.weight(1f),
                                    selected = isStructuralKeyActive(
                                        key = key,
                                        source = workingValue.text,
                                        cursor = workingValue.selection.end,
                                    ),
                                )
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        if (page != MathKeyboardPage.BASIC && page != MathKeyboardPage.COMMANDS) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                commonMathKeys.forEach { key ->
                    KeyboardActionKey(key.label, key.description, Modifier.weight(1f)) { edit(key) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            KeyboardActionKey("↶", "Undo last edit", Modifier.weight(1f)) {
                if (undo.isNotEmpty()) {
                    redo.add(workingValue)
                    emit(undo.removeAt(undo.lastIndex))
                }
            }
            KeyboardActionKey("↷", "Redo last edit", Modifier.weight(1f)) {
                if (redo.isNotEmpty()) {
                    undo.add(workingValue)
                    emit(redo.removeAt(redo.lastIndex))
                }
            }
            KeyboardActionKey("←", "Move cursor left", Modifier.weight(1f)) { emit(StructuredMathEditing.move(workingValue, -1)) }
            KeyboardActionKey("→", "Move cursor right", Modifier.weight(1f)) { emit(StructuredMathEditing.move(workingValue, 1)) }
            KeyboardActionKey("⌫", "Backspace", Modifier.weight(1f), accent = IntentMathPalette.Variable) {
                applyEdit(StructuredMathEditing.backspace(workingValue))
            }
            KeyboardActionKey("↵", "Finish math entry", Modifier.weight(1.15f), accent = IntentMathPalette.Variable) {
                MathKeyboardHistory.remember(workingValue.text)
                onDone()
            }
        }
    }
}

@Composable
private fun KeyboardAppearancePanel() {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Keys", color = IntentMathPalette.Muted, fontSize = 9.sp)
        MathKeyboardKeySize.entries.forEach { size ->
            KeyboardActionKey(
                if (MathKeyboardPreferences.keySize == size) "• ${size.label}" else size.label,
                "Use ${size.label.lowercase()} math keys",
            ) { MathKeyboardPreferences.keySize = size }
        }
        KeyboardActionKey(
            if (MathKeyboardPreferences.highContrast) "Contrast on" else "Contrast",
            "Toggle high contrast keyboard",
            accent = if (MathKeyboardPreferences.highContrast) IntentMathPalette.Constant else IntentMathPalette.Command,
        ) { MathKeyboardPreferences.highContrast = !MathKeyboardPreferences.highContrast }
    }
}

@Composable
private fun StructuredEntryStrip(
    activeMode: MathInputMode,
    currentSource: String,
    currentCursor: Int,
    onInsert: (MathKey) -> Unit,
) {
    val keys = listOf(
        MathKey("xʸ", description = "Toggle superscript", tone = MathKeyTone.FUNCTION, action = MathKeyAction.TOGGLE_SUPERSCRIPT),
        MathKey("xₙ", description = "Toggle subscript", tone = MathKeyTone.VARIABLE, action = MathKeyAction.TOGGLE_SUBSCRIPT),
        fractionTemplate(hasSelection = false).copy(tone = MathKeyTone.FUNCTION),
        MathKey(
            "√",
            "sqrt()",
            1,
            description = "Toggle square-root radicand",
            tone = MathKeyTone.FUNCTION,
            action = MathKeyAction.TOGGLE_ROOT,
        ),
        MathKey(
            "∛",
            "cbrt()",
            1,
            description = "Toggle cube-root radicand",
            tone = MathKeyTone.FUNCTION,
            action = MathKeyAction.TOGGLE_CUBE_ROOT,
        ),
        MathKey(
            "logₐ",
            description = "Toggle logarithm base and argument",
            tone = MathKeyTone.FUNCTION,
            action = MathKeyAction.TOGGLE_LOG_BASE,
        ),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        keys.forEach { key ->
            val selected = isStructuralKeyActive(key, currentSource, currentCursor, activeMode)
            MathKeyboardKey(key, onInsert, Modifier.weight(1f), selected = selected)
        }
    }
}

internal fun isStructuralKeyActive(
    key: MathKey,
    source: String,
    cursor: Int,
    activeMode: MathInputMode = StructuredMathEditing.modeAt(source, cursor),
): Boolean = when (key.action) {
    MathKeyAction.TOGGLE_SUPERSCRIPT -> activeMode == MathInputMode.SUPERSCRIPT
    MathKeyAction.TOGGLE_SUBSCRIPT -> activeMode == MathInputMode.SUBSCRIPT
    MathKeyAction.TOGGLE_FRACTION -> StructuredMathEditing.isFractionActive(source, cursor)
    MathKeyAction.TOGGLE_ROOT -> StructuredMathEditing.isRootActive(source, cursor)
    MathKeyAction.TOGGLE_CUBE_ROOT -> StructuredMathEditing.isCubeRootActive(source, cursor)
    MathKeyAction.TOGGLE_NTH_ROOT -> StructuredMathEditing.isNthRootActive(source, cursor)
    MathKeyAction.TOGGLE_LOG_BASE -> StructuredMathEditing.isLogBaseActive(source, cursor)
    MathKeyAction.INSERT -> false
}

@Composable
private fun CompactClipboardRow(
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        KeyboardActionKey("All", "Select the complete expression", Modifier.weight(1f), onClick = onSelectAll)
        KeyboardActionKey("Copy", "Copy selection or expression", Modifier.weight(1f), onClick = onCopy)
        KeyboardActionKey("Cut", "Cut selection or expression", Modifier.weight(1f), onClick = onCut)
        KeyboardActionKey("Paste", "Paste at cursor", Modifier.weight(1f), accent = IntentMathPalette.Variable, onClick = onPaste)
        KeyboardActionKey("Clear", "Clear entry", Modifier.weight(1f), onClick = onClear)
    }
}

@Composable
private fun BasicNumberPad(onInsert: (MathKey) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        basicNumberPadRows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key -> MathKeyboardKey(key, onInsert, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun VisualFractionKey(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val appearance = MathKeyboardPreferences.keySize
    val accent = IntentMathPalette.Function
    Column(
        modifier
            .height(appearance.actionHeight + 5.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .background(
                if (MathKeyboardPreferences.highContrast) Color.Black else accent.copy(if (selected) .34f else .13f),
                RoundedCornerShape(7.dp),
            )
            .border(
                if (MathKeyboardPreferences.highContrast || selected) 2.dp else 1.dp,
                if (MathKeyboardPreferences.highContrast) Color.White else accent.copy(if (selected) .95f else .55f),
                RoundedCornerShape(7.dp),
            )
            .semantics {
                contentDescription = "Toggle visual fraction numerator and denominator"
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("□", color = IntentMathPalette.Ink, fontSize = 9.sp, lineHeight = 8.sp)
        Box(Modifier.fillMaxWidth(.32f).height(1.dp).background(IntentMathPalette.Function))
        Text("□", color = IntentMathPalette.Ink, fontSize = 9.sp, lineHeight = 8.sp)
    }
}

@Composable
private fun MatrixDimensionPicker(
    rows: Int,
    columns: Int,
    onRowsChange: (Int) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onInsert: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (MathKeyboardPreferences.highContrast) Color.Black else Color(0xFF102235), RoundedCornerShape(8.dp))
            .border(1.dp, IntentMathPalette.Variable.copy(.5f), RoundedCornerShape(8.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Matrix", color = IntentMathPalette.Variable, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        KeyboardActionKey("−", "Remove a matrix row") { onRowsChange((rows - 1).coerceAtLeast(1)) }
        Text("$rows rows", color = IntentMathPalette.Ink, fontSize = 9.sp)
        KeyboardActionKey("+", "Add a matrix row") { onRowsChange((rows + 1).coerceAtMost(6)) }
        KeyboardActionKey("−", "Remove a matrix column") { onColumnsChange((columns - 1).coerceAtLeast(1)) }
        Text("$columns cols", color = IntentMathPalette.Ink, fontSize = 9.sp)
        KeyboardActionKey("+", "Add a matrix column") { onColumnsChange((columns + 1).coerceAtMost(6)) }
        KeyboardActionKey("Insert", "Insert the selected matrix dimensions", Modifier.weight(1f), accent = IntentMathPalette.Variable, onClick = onInsert)
    }
}

private enum class MoreKeyboardTool { MATRIX, STATISTICS, SETS, UNITS }

@Composable
private fun CommandBrowser(
    query: String,
    onQueryChange: (String) -> Unit,
    category: String?,
    onCategoryChange: (String?) -> Unit,
    context: MathKeyboardContext,
    onInsert: (MathKey) -> Unit,
    hasEditorSelection: Boolean,
    currentSource: String,
    currentCursor: Int,
) {
    var activeTool by remember { mutableStateOf<MoreKeyboardTool?>(null) }
    var matrixRows by remember { mutableStateOf(2) }
    var matrixColumns by remember { mutableStateOf(2) }
    val visible = filterMathCommands(query, category, context)
    val categories = mathKeyboardCommands.map(MathCommand::category).distinct().sorted()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            KeyboardActionKey("▦", "Open matrix dimension picker", Modifier.weight(1f)) {
                activeTool = MoreKeyboardTool.MATRIX.takeUnless { activeTool == it }
            }
            KeyboardActionKey("x̄", "Open statistics keys", Modifier.weight(1f)) {
                activeTool = MoreKeyboardTool.STATISTICS.takeUnless { activeTool == it }
            }
            KeyboardActionKey("∈", "Open sets and logic keys", Modifier.weight(1f)) {
                activeTool = MoreKeyboardTool.SETS.takeUnless { activeTool == it }
            }
            KeyboardActionKey("SI", "Open units and scientific constants", Modifier.weight(1f)) {
                activeTool = MoreKeyboardTool.UNITS.takeUnless { activeTool == it }
            }
        }
        when (activeTool) {
            MoreKeyboardTool.MATRIX -> MatrixDimensionPicker(
                rows = matrixRows,
                columns = matrixColumns,
                onRowsChange = { matrixRows = it },
                onColumnsChange = { matrixColumns = it },
                onInsert = {
                    onInsert(matrixTemplate(matrixRows, matrixColumns))
                    activeTool = null
                },
            )
            MoreKeyboardTool.STATISTICS -> MoreKeyGrid(
                keys = statisticsKeys,
                onInsert = onInsert,
                heading = "Statistics",
                currentSource = currentSource,
                currentCursor = currentCursor,
            )
            MoreKeyboardTool.SETS -> MoreKeyGrid(
                keys = setAndLogicKeys,
                onInsert = onInsert,
                heading = "Sets & logic",
                currentSource = currentSource,
                currentCursor = currentCursor,
            )
            MoreKeyboardTool.UNITS -> MoreKeyGrid(
                keys = unitAndConstantKeys,
                onInsert = onInsert,
                heading = "Units & constants",
                currentSource = currentSource,
                currentCursor = currentCursor,
            )
            null -> Unit
        }
        if (activeTool == null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                readOnly = true,
                singleLine = true,
                keyboardOptions = KeyboardOptions(showKeyboardOnFocus = false),
                textStyle = androidx.compose.ui.text.TextStyle(color = IntentMathPalette.Ink, fontSize = 12.sp),
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF101F2E), RoundedCornerShape(7.dp))
                    .border(1.dp, IntentMathPalette.Command.copy(.45f), RoundedCornerShape(7.dp))
                    .padding(9.dp)
                    .semantics { contentDescription = "Search math commands" },
                decorationBox = { inner -> Box { if (query.isBlank()) Text("Describe it: average, area under curve…", color = IntentMathPalette.Muted, fontSize = 11.sp); inner() } },
            )
            KeyboardActionKey("⌫", "Delete search character") { if (query.isNotEmpty()) onQueryChange(query.dropLast(1)) }
            KeyboardActionKey("Clear", "Clear command search") { onQueryChange("") }
        }
        if (query.isBlank()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("average of numbers", "area under curve", "distance between points", "plot a 3d surface").forEach { phrase ->
                    KeyboardActionKey(phrase, "Search for $phrase") { onQueryChange(phrase) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            KeyboardTab("All", category == null) { onCategoryChange(null) }
            categories.forEach { item ->
                KeyboardTab(item, category == item) { onCategoryChange(item) }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (MathKeyboardHistory.favourites + MathKeyboardHistory.recent).distinct().take(4).forEach { expression ->
                KeyboardActionKey(expression.take(16), "Insert saved expression $expression") {
                    onInsert(MathKey(expression.take(16), expression, description = "Saved expression $expression"))
                }
            }
            visible.take(12).forEach { command ->
                Column(
                    Modifier
                        .widthIn(min = 112.dp, max = 165.dp)
                        .clickable(role = Role.Button) { onInsert(command.template) }
                        .background(Color(0xFF102235), RoundedCornerShape(7.dp))
                        .border(1.dp, IntentMathPalette.Function.copy(.38f), RoundedCornerShape(7.dp))
                        .padding(7.dp),
                ) {
                    Text(command.name, color = IntentMathPalette.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(command.explanation, color = IntentMathPalette.Muted, fontSize = 9.sp, maxLines = 2)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            "abcdefghi".forEach { char ->
                KeyboardActionKey(char.toString(), "Type $char in command search", Modifier.weight(1f)) { onQueryChange(query + char) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            "jklmnopqr".forEach { char ->
                KeyboardActionKey(char.toString(), "Type $char in command search", Modifier.weight(1f)) { onQueryChange(query + char) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            "stuvwxyz".forEach { char ->
                KeyboardActionKey(char.toString(), "Type $char in command search", Modifier.weight(1f)) { onQueryChange(query + char) }
            }
            KeyboardActionKey("space", "Type a space in command search", Modifier.weight(2f)) { onQueryChange(query + " ") }
        }
        }
    }
}

@Composable
private fun MoreKeyGrid(
    keys: List<MathKey>,
    onInsert: (MathKey) -> Unit,
    heading: String,
    currentSource: String,
    currentCursor: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(heading, color = IntentMathPalette.Muted, fontSize = 9.sp)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 420.dp) 9 else 7
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                keys.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { key ->
                            MathKeyboardKey(
                                key = key,
                                onClick = onInsert,
                                modifier = Modifier.weight(1f),
                                selected = isStructuralKeyActive(key, currentSource, currentCursor),
                            )
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MathKeyboardKey(
    key: MathKey,
    onClick: (MathKey) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val appearance = MathKeyboardPreferences.keySize
    val tone = resolveMathKeyTone(key)
    val isMultiply = key.insertion == "*"
    val isVariable = tone == MathKeyTone.VARIABLE
    val accent = mathKeyToneColor(tone)
    Box(
        modifier
            .height(appearance.mainHeight)
            .clickable(role = Role.Button) { onClick(key) }
            .background(
                when {
                    MathKeyboardPreferences.highContrast -> Color.Black
                    selected -> accent.copy(alpha = .34f)
                    else -> accent.copy(alpha = if (isMultiply || isVariable) .15f else .08f)
                },
                RoundedCornerShape(7.dp),
            )
            .border(
                if (MathKeyboardPreferences.highContrast || selected) 2.dp else 1.dp,
                if (MathKeyboardPreferences.highContrast) Color.White else accent.copy(alpha = if (selected) .95f else if (isMultiply || isVariable) .65f else .3f),
                RoundedCornerShape(7.dp),
            )
            .semantics {
                contentDescription = key.description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                displayMathKeyLabel(key),
                color = accent,
                fontSize = (14f * appearance.fontScale).sp,
                fontWeight = if (isMultiply) FontWeight.Black else FontWeight.SemiBold,
                fontStyle = if (isVariable) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
            )
            if (isMultiply && appearance != MathKeyboardKeySize.COMPACT) {
                Text("multiply", color = IntentMathPalette.Muted, fontSize = (6.5f * appearance.fontScale).sp, maxLines = 1)
            }
        }
    }
}

internal fun resolveMathKeyTone(key: MathKey): MathKeyTone {
    key.tone?.let { return it }
    val insertion = key.insertion.trim()
    val label = key.label
    return when {
        insertion.matches(Regex("""-?(\d+(\.\d*)?|\.\d+)""")) || label == "." -> MathKeyTone.NUMBER
        insertion in setOf("+", "-", "*", "/", "%", "^") -> MathKeyTone.OPERATOR
        insertion in setOf("=", "<", ">", "<=", ">=", "!=", "≈", "∈", "∉", "⊂") -> MathKeyTone.RELATION
        insertion in setOf("pi", "e", "infinity") || label in setOf("π", "∞") -> MathKeyTone.CONSTANT
        insertion.startsWith("integral") || insertion.startsWith("derivative") || insertion.startsWith("partial") ||
            insertion.startsWith("limit") || insertion.startsWith("sum") || insertion.startsWith("product") ||
            label in setOf("∫", "∫ᵃᵇ", "Σ", "Π", "lim", "d/dx", "∂/∂x", "f′", "f″", "∇") -> MathKeyTone.CALCULUS
        insertion in setOf("()", "[]", "{}", "(,)", "(,,)") || label in setOf("( )", "[ ]", "{ }") -> MathKeyTone.BRACKET
        insertion.startsWith(" ") || insertion in setOf("deg", "rad") -> MathKeyTone.UNIT
        insertion.length == 1 && insertion.single().isLetter() -> MathKeyTone.VARIABLE
        insertion.contains("(") && insertion.endsWith(")") -> MathKeyTone.FUNCTION
        else -> MathKeyTone.GENERAL
    }
}

private fun mathKeyToneColor(tone: MathKeyTone): Color = when (tone) {
    MathKeyTone.NUMBER -> IntentMathPalette.Number
    MathKeyTone.VARIABLE -> IntentMathPalette.Variable
    MathKeyTone.OPERATOR -> IntentMathPalette.Operator
    MathKeyTone.FUNCTION -> IntentMathPalette.Function
    MathKeyTone.CONSTANT -> IntentMathPalette.Constant
    MathKeyTone.BRACKET -> IntentMathPalette.Command
    MathKeyTone.RELATION -> IntentMathPalette.Relation
    MathKeyTone.CALCULUS -> IntentMathPalette.Keyword
    MathKeyTone.UNIT -> IntentMathPalette.Unit
    MathKeyTone.GENERAL -> IntentMathPalette.Ink
}

@Composable
private fun KeyboardTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val appearance = MathKeyboardPreferences.keySize
    Text(
        label,
        color = if (selected) Color(0xFF06121D) else IntentMathPalette.Ink,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clickable(role = Role.Tab, onClick = onClick)
            .background(if (selected) IntentMathPalette.Number else if (MathKeyboardPreferences.highContrast) Color.Black else Color(0xFF122538), RoundedCornerShape(7.dp))
            .border(if (MathKeyboardPreferences.highContrast) 1.dp else 0.dp, Color.White, RoundedCornerShape(7.dp))
            .padding(horizontal = (9f * appearance.fontScale).dp, vertical = (6f * appearance.fontScale).dp),
    )
}

@Composable
private fun KeyboardActionKey(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    accent: Color = IntentMathPalette.Command,
    onClick: () -> Unit,
) {
    val appearance = MathKeyboardPreferences.keySize
    Box(
        modifier
            .height(appearance.actionHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .background(if (MathKeyboardPreferences.highContrast) Color.Black else accent.copy(alpha = .16f), RoundedCornerShape(7.dp))
            .border(if (MathKeyboardPreferences.highContrast) 2.dp else 1.dp, if (MathKeyboardPreferences.highContrast) Color.White else accent.copy(alpha = .42f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (MathKeyboardPreferences.highContrast) Color.White else IntentMathPalette.Ink, fontSize = (11f * appearance.fontScale).sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun displayMathKeyLabel(key: MathKey): String = when {
    key.insertion == "x" && key.label == "x" -> "𝑥"
    key.insertion == "y" && key.label == "y" -> "𝑦"
    key.insertion == "z" && key.label == "z" -> "𝑧"
    else -> key.label
}

private fun prettyMath(source: String): String = source
    .replace("sqrt", "√")
    .replace("*", "×")
    .replace("<=", "≤")
    .replace(">=", "≥")
    .replace("!=", "≠")
    .replace("^2", "²")
    .replace("pi", "π")

private fun caretPreview(value: TextFieldValue, context: Int = 14): String {
    if (value.text.isEmpty()) return "│ Ready"
    val start = value.selection.min
    val end = value.selection.max
    val leftStart = (start - context).coerceAtLeast(0)
    val rightEnd = (end + context).coerceAtMost(value.text.length)
    val left = value.text.substring(leftStart, start)
    val right = value.text.substring(end, rightEnd)
    val marker = if (start == end) "│" else "⟦${value.text.substring(start, end)}⟧"
    return "${if (leftStart > 0) "…" else ""}$left$marker$right${if (rightEnd < value.text.length) "…" else ""}"
}
