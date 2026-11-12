package com.school.calc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.math.*

class Sec(val name: String, val fields: List<Pair<String, String>>, val ops: List<String> = emptyList())

val sections = listOf(
    Sec("Обычные", listOf("Первое число" to "25", "Второе число" to "5"), listOf("+", "−", "×", "÷")),
    Sec("Корни", listOf("Число" to "25", "Степень корня" to "2")),
    Sec("Степени", listOf("Основание" to "2", "Показатель" to "10")),
    Sec("Факториал", listOf("n" to "5")),
    Sec("Логарифмы", listOf("Аргумент логарифма" to "25", "Основание логарифма" to "5")),
    Sec("Тригонометрия", listOf("Угол (в градусах)" to "30"), listOf("sin", "cos", "tg", "ctg")),
    Sec("Дроби", listOf("Числитель 1" to "1", "Знаменатель 1" to "2", "Числитель 2" to "1", "Знаменатель 2" to "3"), listOf("+", "−", "×", "÷"))
)

fun fmt(d: Double): String {
    require(!d.isNaN() && !d.isInfinite()) { "результат не определён" }
    val r = d.roundToLong()
    if (abs(d - r) < 1e-9 && abs(d) < 1e15) return r.toString()
    return BigDecimal(d).round(MathContext(12)).stripTrailingZeros().toPlainString()
}

fun gcd(a: Long, b: Long): Long = if (b == 0L) abs(a) else gcd(b, a % b)

fun calc(i: Int, v: List<Double>, op: String): String = when (i) {
    0 -> {
        val (a, b) = v
        require(!(op == "÷" && b == 0.0)) { "деление на ноль" }
        val r = when (op) { "+" -> a + b; "−" -> a - b; "×" -> a * b; else -> a / b }
        "${fmt(a)}$op${fmt(b)} = ${fmt(r)}"
    }
    1 -> {
        val (x, n) = v
        require(n >= 1 && n == floor(n)) { "степень корня — целое ≥ 1" }
        val odd = n.toLong() % 2 == 1L
        require(x >= 0 || odd) { "чётный корень из отрицательного" }
        val r = if (x < 0) -(-x).pow(1 / n) else x.pow(1 / n)
        val rr = r.roundToLong().toDouble()
        val res = if (abs(rr.pow(n) - x) < 1e-9) rr else r
        "корень степени ${fmt(n)} из ${fmt(x)} = ${fmt(res)}"
    }
    2 -> "${fmt(v[0])}^${fmt(v[1])} = ${fmt(v[0].pow(v[1]))}"
    3 -> {
        val n = v[0]
        require(n >= 0 && n == floor(n) && n <= 1000) { "n — целое от 0 до 1000" }
        var f = BigInteger.ONE
        for (k in 2..n.toInt()) f = f.multiply(BigInteger.valueOf(k.toLong()))
        "${n.toInt()}! = $f"
    }
    4 -> {
        val (x, b) = v
        require(x > 0 && b > 0 && b != 1.0) { "аргумент > 0, основание > 0 и ≠ 1" }
        "log по основанию ${fmt(b)} от ${fmt(x)} = ${fmt(ln(x) / ln(b))}"
    }
    5 -> {
        val rad = Math.toRadians(v[0])
        val r = when (op) { "sin" -> sin(rad); "cos" -> cos(rad); "tg" -> tan(rad); else -> 1 / tan(rad) }
        val r2 = if (abs(r) < 1e-12) 0.0 else r
        "$op(${fmt(v[0])}°) = ${fmt(r2)}"
    }
    else -> {
        val (a, b, c, d) = v.map { it.toLong() }
        require(b != 0L && d != 0L) { "знаменатель не может быть 0" }
        var n: Long; var m: Long
        when (op) {
            "+" -> { n = a * d + c * b; m = b * d }
            "−" -> { n = a * d - c * b; m = b * d }
            "×" -> { n = a * c; m = b * d }
            else -> { require(c != 0L) { "деление на ноль" }; n = a * d; m = b * c }
        }
        if (m < 0) { n = -n; m = -m }
        val g = gcd(n, m).coerceAtLeast(1)
        n /= g; m /= g
        "$a/$b $op $c/$d = " + if (m == 1L) "$n" else "$n/$m"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

val BG = Color(0xFF0B1220); val CARD = Color(0xFF131C2F); val BTN = Color(0xFF1B2640)
val ACC = Color(0xFF3A63B8); val GREEN = Color(0xFF6EE7A0); val DIM = Color(0xFF8FA0BF)

@Composable
fun App() {
    var sec by remember { mutableStateOf(4) }
    val s = sections[sec]
    val values = remember(sec) { mutableStateListOf(*s.fields.map { it.second }.toTypedArray()) }
    var op by remember(sec) { mutableStateOf(0) }
    var result by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }

    Column(
        Modifier.fillMaxSize().background(BG).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Школьный калькулятор PRO", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Математика под рукой · работает офлайн", color = DIM, fontSize = 14.sp)

        Card(CARD) {
            Label("РАЗДЕЛ")
            sections.indices.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { i -> Chip(sections[i].name, i == sec, Modifier.weight(1f)) { sec = i; result = ""; desc = "" } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (s.ops.isNotEmpty()) {
                Label("ОПЕРАЦИЯ")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.ops.forEachIndexed { i, o -> Chip(o, i == op, Modifier.weight(1f)) { op = i } }
                }
            }
            s.fields.forEachIndexed { i, f ->
                Label(f.first.uppercase())
                OutlinedTextField(
                    value = values[i], onValueChange = { values[i] = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = ACC, unfocusedBorderColor = BTN, cursorColor = Color.White
                    )
                )
            }
            Button(
                onClick = {
                    try {
                        val nums = values.map { it.replace(',', '.').trim().toDouble() }
                        val text = calc(sec, nums, s.ops.getOrElse(op) { "" })
                        desc = text; result = text.substringAfterLast("= ")
                        history.add(0, text)
                    } catch (e: NumberFormatException) {
                        result = "Ошибка"; desc = "Введите корректные числа"
                    } catch (e: Exception) {
                        result = "Ошибка"; desc = e.message ?: "Не удалось вычислить"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ACC)
            ) { Text("Вычислить", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            Label("РЕЗУЛЬТАТ")
            Text(result, color = GREEN, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = DIM, fontSize = 16.sp)
        }

        Card(CARD) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("История вычислений", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Chip("Очистить", false, Modifier) { history.clear() }
            }
            history.take(30).forEach { Text(it, color = DIM, fontSize = 16.sp, modifier = Modifier.padding(vertical = 6.dp)) }
        }
    }
}

@Composable fun Label(t: String) = Text(t, color = DIM, fontSize = 13.sp, fontWeight = FontWeight.Bold)

@Composable
fun Card(bg: Color, content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(bg).padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp), content = content
)

@Composable
fun Chip(t: String, sel: Boolean, m: Modifier, onClick: () -> Unit) = Button(
    onClick = onClick, modifier = m.height(48.dp),
    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(containerColor = if (sel) ACC else BTN, contentColor = Color.White),
    contentPadding = PaddingValues(horizontal = 8.dp)
) { Text(t, fontSize = 16.sp, maxLines = 1) }
