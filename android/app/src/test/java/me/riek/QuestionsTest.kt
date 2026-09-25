package me.riek

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class QuestionsTest {

    private fun operand(s: String): Double =
        if ("/" in s) { val (n, d) = s.split("/"); n.toDouble() / d.toDouble() } else s.toDouble()

    private fun evalText(text: String): Double {
        val (a, op, b) = text.split(" ")
        val x = operand(a); val y = operand(b)
        return when (op) {
            "+" -> x + y
            "−" -> x - y
            "×" -> x * y
            "÷" -> x / y
            else -> error("unknown op '$op' in $text")
        }
    }

    @Test fun generatedQuestionsMatchTheSite() {
        repeat(500) { seed ->
            val qs = generateQuestions(Random(seed))
            assertEquals(QUESTION_COUNT, qs.size)
            qs.forEach { q ->
                // the printed equation must actually evaluate to the answer
                assertTrue(
                    "equation ${q.text} != ${q.answer}",
                    abs(evalText(q.text) - operand(q.answer)) < 1e-4,
                )
                // typing the exact answer must score correct (site's checker)
                assertTrue("round-trip failed for ${q.answer}", checkAnswer(q.answer, q.answer))
            }
        }
    }

    @Test fun answerCheckerBehavesLikeTheSite() {
        assertTrue(checkAnswer(" 42 ", "42"))          // trim + exact
        assertTrue(checkAnswer("5/6", "5/6"))          // fraction exact
        assertTrue(checkAnswer("12.50", "12.5"))       // numeric within 1e-4
        assertTrue(!checkAnswer("", "7"))              // blank is never correct
        assertTrue(!checkAnswer("0.83", "5/6"))        // decimal != fraction string
    }
}
