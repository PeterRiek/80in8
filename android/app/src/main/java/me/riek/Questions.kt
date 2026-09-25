package me.riek

import kotlin.math.abs
import kotlin.random.Random

/**
 * 1:1 port of 80in8.com's question generator (Optiver 80in8 simulation).
 * [answer] is the canonical answer string — an integer, a 2-decimal value,
 * or a reduced fraction like "5/6". Verified against the site's minified source.
 */
data class Question(val text: String, val answer: String)

const val QUESTION_COUNT = 80
const val GAME_SECONDS = 8 * 60 // 480s, matches the site

/* ---- helpers (named after the site's minified functions) ---- */

// z(e,t): inclusive random int in [e, t]
private fun z(e: Int, t: Int, rng: Random) = rng.nextInt(e, t + 1)

// S(arr): random element
private fun <T> s(a: List<T>, rng: Random): T = a[z(0, a.size - 1, rng)]

// C(a,b): gcd on absolute values
private fun gcd(a: Int, b: Int): Int {
    var x = abs(a); var y = abs(b)
    while (y != 0) { val t = y; y = x % y; x = t }
    return x
}

// $(e): integer -> plain string, else round to 2 decimals and strip trailing zeros
private fun fmtDec(e: Double): String =
    if (e == Math.floor(e) && !e.isInfinite()) e.toLong().toString()
    else (Math.round(e * 100.0) / 100.0).toString().trimEnd('0').trimEnd('.')

// E(e,t): reduced fraction string (sign on numerator, "n" if denominator is 1)
private fun fracReduced(e: Int, t: Int): String {
    var num = e; var den = t
    if (den < 0) { num = -num; den = -den }
    if (num == 0) return "0"
    val g = gcd(num, den)
    val r = num / g; val n = den / g
    return if (n == 1) r.toString() else "$r/$n"
}

// R(e,t): display a fraction term unreduced ("e" if denominator is 1)
private fun fracTerm(e: Int, t: Int): String = if (t == 1) e.toString() else "$e/$t"

// _(e): with 25% probability, negate e
private fun sign(e: Int, rng: Random): Int = if (rng.nextDouble() < 0.25) -e else e
private fun sign(e: Double, rng: Random): Double = if (rng.nextDouble() < 0.25) -e else e

/* ---- operand tables (verbatim from the site) ---- */

private val MUL = listOf(11,12,13,14,15,16,17,18,19,20,21,22,24,25,27,29,30,31,32,33,35,36,39,40,41,44,45,48,49,50,51,55,60,64,72,75,80,81,90,96,99)
private val DIV = listOf(12,14,15,16,18,20,22,24,25,27,32,33,35,36,40,44,45,48,50,55,60,64,72,75,80,96)
private val DENOM = listOf(2,3,4,5,6,8,10,12)

/* ---- question generators ---- */

private fun addInt(rng: Random): Question {
    val e = sign(z(1, 999, rng), rng); val t = z(1, 999, rng)
    return Question("$e + $t", (e + t).toString())
}

private fun subInt(rng: Random): Question {
    val e = sign(z(1, 999, rng), rng); val t = z(1, 999, rng)
    return Question("$e − $t", (e - t).toString())
}

private fun mulInt(rng: Random): Question {
    var e: Int; val t: Int
    if (rng.nextDouble() < 0.5) { e = z(100, 999, rng); t = z(2, 9, rng) }
    else { e = s(MUL, rng); t = z(10, 99, rng) }
    e = sign(e, rng)
    return Question("$e × $t", (e * t).toString())
}

private fun divInt(rng: Random): Question {
    val e = s(DIV, rng)
    val t = sign(e * z(3, 20, rng), rng)
    return Question("$t ÷ $e", (t / e).toString())
}

// D(): random decimal — 75% one-decimal, 25% two-decimal
private fun decimal(rng: Random): Double =
    if (rng.nextDouble() < 0.75) z(1, 999, rng) / 10.0 else z(1, 9999, rng) / 100.0

private fun addDec(rng: Random): Question {
    val e = sign(decimal(rng), rng); val t = decimal(rng)
    val r = Math.round((e + t) * 100.0) / 100.0
    return Question("${fmtDec(e)} + ${fmtDec(t)}", fmtDec(r))
}

private fun subDec(rng: Random): Question {
    val e = sign(decimal(rng), rng); val t = decimal(rng)
    val r = Math.round((e - t) * 100.0) / 100.0
    return Question("${fmtDec(e)} − ${fmtDec(t)}", fmtDec(r))
}

// F(): two denominators from DENOM whose lcm <= 12
private fun twoDenoms(rng: Random): Pair<Int, Int> {
    var e: Int; var t: Int
    do { e = s(DENOM, rng); t = s(DENOM, rng) } while (e * t / gcd(e, t) > 12)
    return e to t
}

private fun addFrac(rng: Random): Question {
    while (true) {
        val (t, r) = twoDenoms(rng)
        val n = sign(z(1, t - 1, rng), rng); val i = z(1, r - 1, rng)
        val a = t * r / gcd(t, r)
        val l = a / t * n + a / r * i
        if (l == 0) continue
        return Question("${fracTerm(n, t)} + ${fracTerm(i, r)}", fracReduced(l, a))
    }
}

private fun subFrac(rng: Random): Question {
    while (true) {
        val (t, r) = twoDenoms(rng)
        val n = sign(z(1, t - 1, rng), rng); val i = z(1, r - 1, rng)
        val a = t * r / gcd(t, r)
        val l = a / t * n - a / r * i
        if (l == 0) continue
        return Question("${fracTerm(n, t)} − ${fracTerm(i, r)}", fracReduced(l, a))
    }
}

// B: the weighted pool — integers appear twice as often as decimals/fractions
private val POOL: List<(Random) -> Question> = listOf(
    ::addInt, ::addInt, ::subInt, ::subInt, ::mulInt, ::mulInt, ::divInt, ::divInt,
    ::addDec, ::subDec, ::addFrac, ::subFrac,
)

/** K(80): 80 independent draws from the weighted pool. */
fun generateQuestions(rng: Random = Random.Default): List<Question> =
    List(QUESTION_COUNT) { s(POOL, rng)(rng) }

/**
 * The site's answer check: trim; exact string match (handles fractions), else
 * both parse as numbers within 1e-4. Uses JS parseFloat semantics (leading number).
 */
fun checkAnswer(user: String, correct: String): Boolean {
    val r = user.trim(); val n = correct.trim()
    if (r.isEmpty()) return false
    if (r == n) return true
    val a = parseFloatJs(r); val b = parseFloatJs(n)
    return a != null && b != null && abs(a - b) < 1e-4
}

private val LEADING_NUMBER = Regex("^[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?")
private fun parseFloatJs(s: String): Double? =
    LEADING_NUMBER.find(s.trim())?.value?.toDoubleOrNull()
