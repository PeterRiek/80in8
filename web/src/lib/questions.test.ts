import { test } from "node:test";
import assert from "node:assert/strict";
import { generateQuestions, checkAnswer, QUESTION_COUNT } from "./questions.ts";

const operand = (s: string): number =>
  s.includes("/") ? Number(s.split("/")[0]) / Number(s.split("/")[1]) : Number(s);

const evalText = (text: string): number => {
  const [a, op, b] = text.split(" ");
  const x = operand(a), y = operand(b);
  switch (op) {
    case "+": return x + y;
    case "−": return x - y;
    case "×": return x * y;
    case "÷": return x / y;
    default: throw new Error(`unknown op '${op}' in ${text}`);
  }
};

test("generated questions match their printed equation and round-trip", () => {
  for (let i = 0; i < 500; i++) {
    const qs = generateQuestions();
    assert.equal(qs.length, QUESTION_COUNT);
    for (const q of qs) {
      assert.ok(Math.abs(evalText(q.text) - operand(q.answer)) < 1e-4, `equation ${q.text} != ${q.answer}`);
      assert.ok(checkAnswer(q.answer, q.answer), `round-trip failed for ${q.answer}`);
    }
  }
});

test("answer checker behaves like the site", () => {
  assert.ok(checkAnswer(" 42 ", "42"));
  assert.ok(checkAnswer("5/6", "5/6"));
  assert.ok(checkAnswer("12.50", "12.5"));
  assert.ok(!checkAnswer("", "7"));
  assert.ok(!checkAnswer("0.83", "5/6"));
});
