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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

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
    CALCULUS("∫∑"),
    SYMBOLS("αβ"),
    COMMANDS("⌕"),
}

data class MathKey(
    val label: String,
    val insertion: String = label,
    val cursorBack: Int = 0,
    val selectionLength: Int = 0,
    val description: String = label,
)

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

    fun clear(value: TextFieldValue) = TextFieldValue("")
}

object MathKeyboardHistory {
    val recent = mutableStateListOf<String>()
    val favourites = mutableStateListOf<String>()

    fun remember(expression: String) {
        val clean = expression.trim()
        if (clean.isBlank()) return
        recent.remove(clean)
        recent.add(0, clean)
        while (recent.size > 8) recent.removeAt(recent.lastIndex)
    }
}

private val basicKeys = listOf(
    MathKey("7"), MathKey("8"), MathKey("9"), MathKey("÷", "/", description = "Divide"),
    MathKey("4"), MathKey("5"), MathKey("6"), MathKey("×", "*", description = "Multiply"),
    MathKey("1"), MathKey("2"), MathKey("3"), MathKey("−", "-", description = "Subtract"),
    MathKey("0"), MathKey("."), MathKey("+"), MathKey("=", description = "Equals"),
    MathKey("x"), MathKey("y"), MathKey("π", "pi"), MathKey("( )", "()", 1, description = "Parentheses"),
)

private val functionKeys = listOf(
    MathKey("sin", "sin()", 1, description = "Sine"),
    MathKey("cos", "cos()", 1, description = "Cosine"),
    MathKey("tan", "tan()", 1, description = "Tangent"),
    MathKey("x²", "(%s)^2", cursorBack = 0, description = "Square"),
    MathKey("√", "sqrt(%s)", cursorBack = 1, description = "Square root"),
    MathKey("ln", "ln()", 1, description = "Natural logarithm"),
    MathKey("log", "log()", 1, description = "Base ten logarithm"),
    MathKey("|x|", "abs(%s)", 1, description = "Absolute value"),
    MathKey("eˣ", "exp()", 1, description = "Exponential"),
    MathKey("aⁿ", "(%s)^()", 1, description = "Power"),
    MathKey("asin", "asin()", 1, description = "Inverse sine"),
    MathKey("acos", "acos()", 1, description = "Inverse cosine"),
    MathKey("atan", "atan()", 1, description = "Inverse tangent"),
    MathKey("min", "min(,)", 2, description = "Minimum"),
    MathKey("max", "max(,)", 2, description = "Maximum"),
    MathKey("if", "if(,,)", 3, description = "Piecewise condition"),
)

private val calculusKeys = listOf(
    MathKey("d/dx", "derivative(%s,x)", 2, description = "Derivative"),
    MathKey("∂/∂x", "partial(%s,x)", 2, description = "Partial derivative"),
    MathKey("∫", "integral(%s,x)", 2, description = "Indefinite integral"),
    MathKey("∫ᵃᵇ", "integral(%s,x,,)", 3, description = "Definite integral"),
    MathKey("lim", "limit(%s,x,)", 1, description = "Limit"),
    MathKey("Σ", "sum(%s,n,,)", 3, description = "Summation"),
    MathKey("Π", "product(%s,n,,)", 3, description = "Product"),
    MathKey("∇", "sqrt(partial(%s,x)^2+partial(%s,y)^2)", 1, description = "Gradient magnitude"),
    MathKey("f′", "derivative(%s,x)", 2, description = "First derivative"),
    MathKey("f″", "derivative(derivative(%s,x),x)", 4, description = "Second derivative"),
    MathKey("dx"), MathKey("dy"), MathKey("dt"), MathKey("∞", "infinity"),
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
    listOf(MathKey(","), MathKey("_"), MathKey("[ ]", "[]", 1), MathKey("{ }", "{}", 1)))

private val commands = listOf(
    MathCommand("Explicit graph", "Graph", MathKey("y=f(x)", "y=", description = "Explicit graph"), "Plot y as a function of x.", setOf(MathKeyboardContext.GRAPH_2D)),
    MathCommand("Polar graph", "Graph", MathKey("r=f(θ)", "r=", description = "Polar graph"), "Plot radius against angle.", setOf(MathKeyboardContext.GRAPH_2D)),
    MathCommand("Parametric curve", "Graph", MathKey("x(t), y(t)", "x(t)=; y(t)=", 8, description = "Parametric curve"), "Plot x and y from parameter t.", setOf(MathKeyboardContext.GRAPH_2D)),
    MathCommand("Point", "Geometry", MathKey("(x,y)", "(,)", 2, description = "Point"), "Insert a two-dimensional point."),
    MathCommand("3D point", "3D", MathKey("(x,y,z)", "(,,)", 3, description = "3D point"), "Insert a three-dimensional point.", setOf(MathKeyboardContext.GRAPH_3D)),
    MathCommand("Explicit surface", "3D", MathKey("z=f(x,y)", "z=", description = "Explicit surface"), "Plot height z from x and y.", setOf(MathKeyboardContext.GRAPH_3D)),
    MathCommand("Sphere", "3D", MathKey("Sphere", "x^2+y^2+z^2=", 0, description = "Sphere equation"), "Create a sphere equation.", setOf(MathKeyboardContext.GRAPH_3D)),
    MathCommand("Vector", "Vectors", MathKey("Vector", "vector(,,)", 3, description = "Vector"), "Insert a vector template."),
    MathCommand("Matrix 2×2", "Matrices", MathKey("[2×2]", "[[,],[,]]", 7, description = "Two by two matrix"), "Insert a 2 by 2 matrix.", setOf(MathKeyboardContext.MATRIX)),
    MathCommand("Mean", "Statistics", MathKey("mean", "mean()", 1, description = "Mean"), "Calculate the arithmetic mean.", setOf(MathKeyboardContext.STATISTICS)),
    MathCommand("Standard deviation", "Statistics", MathKey("stdev", "stdev()", 1, description = "Standard deviation"), "Measure data spread.", setOf(MathKeyboardContext.STATISTICS)),
    MathCommand("Union", "Sets", MathKey("A∪B", " union ", description = "Set union"), "Combine elements from two sets.", setOf(MathKeyboardContext.SETS)),
    MathCommand("Intersection", "Sets", MathKey("A∩B", " intersection ", description = "Set intersection"), "Keep common elements from two sets.", setOf(MathKeyboardContext.SETS)),
    MathCommand("Derivative", "Calculus", calculusKeys[0], "Find the rate of change.", setOf(MathKeyboardContext.CALCULUS, MathKeyboardContext.GRAPH_2D)),
    MathCommand("Definite integral", "Calculus", calculusKeys[3], "Find accumulated signed area.", setOf(MathKeyboardContext.CALCULUS, MathKeyboardContext.GRAPH_2D)),
    MathCommand("Piecewise function", "Functions", functionKeys.last(), "Choose an expression using a condition."),
)

private fun contextualKeys(context: MathKeyboardContext): List<MathKey> = when (context) {
    MathKeyboardContext.GRAPH_2D -> listOf(
        MathKey("y=", "y="), MathKey("x"), MathKey("x²", "x^2"), MathKey("sin(x)", "sin(x)"),
        MathKey("r=", "r="), MathKey("(x,y)", "(,)", 2), MathKey("if", "if(,,)", 3),
    )
    MathKeyboardContext.GRAPH_3D -> listOf(
        MathKey("z=", "z="), MathKey("x"), MathKey("y"), MathKey("x²+y²", "x^2+y^2"),
        MathKey("sin(x)", "sin(x)"), MathKey("cos(y)", "cos(y)"), MathKey("√", "sqrt()", 1),
    )
    MathKeyboardContext.CALCULUS -> calculusKeys.take(7)
    MathKeyboardContext.MATRIX -> listOf(MathKey("[2×2]", "[[,],[,]]", 7), MathKey("det", "det()", 1), MathKey("⁻¹", "inverse()", 1), MathKey("T", "transpose()", 1))
    MathKeyboardContext.SETS -> listOf(MathKey("∈"), MathKey("∉"), MathKey("∪"), MathKey("∩"), MathKey("⊂"), MathKey("∅", "{}"))
    MathKeyboardContext.STATISTICS -> listOf(MathKey("mean", "mean()", 1), MathKey("median", "median()", 1), MathKey("σ", "stdev()", 1), MathKey("nCr", "nCr(,)", 2))
    MathKeyboardContext.SCIENCE -> listOf(MathKey("π", "pi"), MathKey("e"), MathKey("×10ⁿ", "*10^()", 1), MathKey("μ", "mu"), MathKey("Δ", "delta"))
    MathKeyboardContext.GENERAL -> listOf(MathKey("x"), MathKey("y"), MathKey("π", "pi"), MathKey("x²", "(%s)^2"), MathKey("√", "sqrt(%s)", 1))
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
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        AdaptiveMathKeyboard(
            value = value,
            onValueChange = onValueChange,
            context = context,
            onDone = onDone,
            onDismiss = onDismiss,
            modifier = Modifier.widthIn(max = 720.dp),
        )
    }
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
    var expert by remember { mutableStateOf(false) }
    var commandQuery by remember { mutableStateOf("") }
    val undo = remember { mutableStateListOf<TextFieldValue>() }
    val redo = remember { mutableStateListOf<TextFieldValue>() }
    val applyEdit: (TextFieldValue) -> Unit = { next ->
        if (next != value) {
            undo.add(value)
            while (undo.size > 40) undo.removeAt(0)
            redo.clear()
            onValueChange(next)
        }
    }
    val edit: (MathKey) -> Unit = { applyEdit(MathTextEditing.insert(value, it)) }
    val keys = when (page) {
        MathKeyboardPage.BASIC -> basicKeys
        MathKeyboardPage.FUNCTIONS -> functionKeys
        MathKeyboardPage.LETTERS -> letterKeys
        MathKeyboardPage.CALCULUS -> calculusKeys
        MathKeyboardPage.SYMBOLS -> symbolKeys
        MathKeyboardPage.COMMANDS -> emptyList()
    }
    val visibleKeys = remember(page, context, expert) {
        if (page == MathKeyboardPage.BASIC && !expert) {
            (contextualKeys(context) + basicKeys).distinctBy { it.insertion }
        } else {
            keys
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(Color(0xFF07131F), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .border(1.dp, IntentMathPalette.Command.copy(alpha = .62f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Math", color = IntentMathPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(prettyMath(value.text).ifBlank { "Ready" }, color = IntentMathPalette.Number, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
            val favourite = value.text.trim().takeIf(String::isNotBlank)?.let { it in MathKeyboardHistory.favourites } == true
            KeyboardActionKey(if (favourite) "★" else "☆", if (favourite) "Remove favourite" else "Save favourite") {
                val expression = value.text.trim()
                if (expression.isNotBlank()) {
                    if (favourite) MathKeyboardHistory.favourites.remove(expression)
                    else MathKeyboardHistory.favourites.add(0, expression)
                }
            }
            KeyboardActionKey(if (expert) "Pro" else "Simple", "Switch keyboard detail") { expert = !expert }
            KeyboardActionKey("⌄", "Collapse math keyboard", onClick = onDismiss)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MathKeyboardPage.entries.forEach { item ->
                KeyboardTab(item.label, page == item) { page = item }
            }
        }
        if (page == MathKeyboardPage.COMMANDS) {
            CommandBrowser(
                query = commandQuery,
                onQueryChange = { commandQuery = it },
                context = context,
                onInsert = edit,
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = when {
                    page == MathKeyboardPage.LETTERS -> 10
                    maxWidth >= 520.dp -> if (expert) 10 else 9
                    maxWidth >= 350.dp -> if (expert) 9 else 8
                    else -> 7
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    visibleKeys.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { key -> MathKeyboardKey(key, edit, Modifier.weight(1f)) }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            KeyboardActionKey("↶", "Undo last edit", Modifier.weight(1f)) {
                if (undo.isNotEmpty()) {
                    redo.add(value)
                    onValueChange(undo.removeAt(undo.lastIndex))
                }
            }
            KeyboardActionKey("↷", "Redo last edit", Modifier.weight(1f)) {
                if (redo.isNotEmpty()) {
                    undo.add(value)
                    onValueChange(redo.removeAt(redo.lastIndex))
                }
            }
            KeyboardActionKey("←", "Move cursor left", Modifier.weight(1f)) { onValueChange(MathTextEditing.move(value, -1)) }
            KeyboardActionKey("→", "Move cursor right", Modifier.weight(1f)) { onValueChange(MathTextEditing.move(value, 1)) }
            KeyboardActionKey("⌫", "Delete", Modifier.weight(1f)) { applyEdit(MathTextEditing.backspace(value)) }
            KeyboardActionKey("Clear", "Clear entry", Modifier.weight(1.25f)) { applyEdit(MathTextEditing.clear(value)) }
            KeyboardActionKey("↵", "Finish math entry", Modifier.weight(1.15f), accent = IntentMathPalette.Variable) {
                MathKeyboardHistory.remember(value.text)
                onDone()
            }
        }
    }
}

@Composable
private fun CommandBrowser(
    query: String,
    onQueryChange: (String) -> Unit,
    context: MathKeyboardContext,
    onInsert: (MathKey) -> Unit,
) {
    val visible = commands.filter {
        (it.contexts.isEmpty() || context in it.contexts) &&
            (query.isBlank() || listOf(it.name, it.category, it.explanation).any { text -> text.contains(query, ignoreCase = true) })
    }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                readOnly = true,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = IntentMathPalette.Ink, fontSize = 12.sp),
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF101F2E), RoundedCornerShape(7.dp))
                    .border(1.dp, IntentMathPalette.Command.copy(.45f), RoundedCornerShape(7.dp))
                    .padding(9.dp)
                    .semantics { contentDescription = "Search math commands" },
                decorationBox = { inner -> Box { if (query.isBlank()) Text("Search commands with abc keys", color = IntentMathPalette.Muted, fontSize = 11.sp); inner() } },
            )
            KeyboardActionKey("abc", "Enter command search text") { }
            KeyboardActionKey("⌫", "Delete search character") { if (query.isNotEmpty()) onQueryChange(query.dropLast(1)) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (MathKeyboardHistory.favourites + MathKeyboardHistory.recent).distinct().take(4).forEach { expression ->
                KeyboardActionKey(expression.take(16), "Insert saved expression $expression") {
                    onInsert(MathKey(expression.take(16), expression, description = "Saved expression $expression"))
                }
            }
            visible.take(8).forEach { command ->
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
        }
    }
}

@Composable
private fun MathKeyboardKey(key: MathKey, onClick: (MathKey) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(40.dp)
            .clickable(role = Role.Button) { onClick(key) }
            .background(Color(0xFF122538), RoundedCornerShape(7.dp))
            .border(1.dp, IntentMathPalette.Number.copy(alpha = .26f), RoundedCornerShape(7.dp))
            .semantics { contentDescription = key.description },
        contentAlignment = Alignment.Center,
    ) {
        Text(key.label, color = IntentMathPalette.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun KeyboardTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color(0xFF06121D) else IntentMathPalette.Ink,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clickable(role = Role.Tab, onClick = onClick)
            .background(if (selected) IntentMathPalette.Number else Color(0xFF122538), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
    Box(
        modifier
            .height(32.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .background(accent.copy(alpha = .16f), RoundedCornerShape(7.dp))
            .border(1.dp, accent.copy(alpha = .42f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = IntentMathPalette.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun prettyMath(source: String): String = source
    .replace("sqrt", "√")
    .replace("*", "×")
    .replace("<=", "≤")
    .replace(">=", "≥")
    .replace("!=", "≠")
    .replace("^2", "²")
    .replace("pi", "π")
