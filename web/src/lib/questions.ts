/**
 * 1:1 port of the Android app's question generator (me.riek.Questions.kt),
 * itself a port of 80in8.com's generator. Verified against the site's source.
 */
export type Question = { text: string; answer: string };

export const QUESTION_COUNT = 80;
export const GAME_SECONDS = 8 * 60; // 480s

// injectable RNG returning [0,1) so tests can be deterministic; defaults to Math.random
type Rng = () => number;

// z(e,t): inclusive random int in [e, t]
const z = (e: number, t: number, r: Rng) => e + Math.floor(r() * (t - e + 1));
// s(arr): random element
const pick = <T>(a: T[], r: Rng): T => a[z(0, a.length - 1, r)];

const gcd = (a: number, b: number): number => {
  let x = Math.abs(a), y = Math.abs(b);
  while (y !== 0) { const t = y; y = x % y; x = t; }
  return x;
};

// $(e): integer -> plain string, else round to 2 decimals and strip trailing zeros
const fmtDec = (e: number): string =>
  Number.isInteger(e) ? String(e) : String(Math.round(e * 100) / 100);

// E(e,t): reduced fraction string (sign on numerator, "n" if denominator is 1)
const fracReduced = (e: number, t: number): string => {
  let num = e, den = t;
  if (den < 0) { num = -num; den = -den; }
  if (num === 0) return "0";
  const g = gcd(num, den);
  const r = num / g, n = den / g;
  return n === 1 ? String(r) : `${r}/${n}`;
};

// R(e,t): display a fraction term unreduced ("e" if denominator is 1)
const fracTerm = (e: number, t: number): string => (t === 1 ? String(e) : `${e}/${t}`);

// _(e): with 25% probability, negate e
const sign = (e: number, r: Rng): number => (r() < 0.25 ? -e : e);

const MUL = [11,12,13,14,15,16,17,18,19,20,21,22,24,25,27,29,30,31,32,33,35,36,39,40,41,44,45,48,49,50,51,55,60,64,72,75,80,81,90,96,99];
const DIV = [12,14,15,16,18,20,22,24,25,27,32,33,35,36,40,44,45,48,50,55,60,64,72,75,80,96];
const DENOM = [2,3,4,5,6,8,10,12];

const addInt = (r: Rng): Question => {
  const e = sign(z(1, 999, r), r), t = z(1, 999, r);
  return { text: `${e} + ${t}`, answer: String(e + t) };
};
const subInt = (r: Rng): Question => {
  const e = sign(z(1, 999, r), r), t = z(1, 999, r);
  return { text: `${e} − ${t}`, answer: String(e - t) };
};
const mulInt = (r: Rng): Question => {
  let e: number; let t: number;
  if (r() < 0.5) { e = z(100, 999, r); t = z(2, 9, r); }
  else { e = pick(MUL, r); t = z(10, 99, r); }
  e = sign(e, r);
  return { text: `${e} × ${t}`, answer: String(e * t) };
};
const divInt = (r: Rng): Question => {
  const e = pick(DIV, r);
  const t = sign(e * z(3, 20, r), r);
  return { text: `${t} ÷ ${e}`, answer: String(t / e) };
};

// D(): random decimal — 75% one-decimal, 25% two-decimal
const decimal = (r: Rng): number =>
  r() < 0.75 ? z(1, 999, r) / 10 : z(1, 9999, r) / 100;

const addDec = (r: Rng): Question => {
  const e = sign(decimal(r), r), t = decimal(r);
  return { text: `${fmtDec(e)} + ${fmtDec(t)}`, answer: fmtDec(Math.round((e + t) * 100) / 100) };
};
const subDec = (r: Rng): Question => {
  const e = sign(decimal(r), r), t = decimal(r);
  return { text: `${fmtDec(e)} − ${fmtDec(t)}`, answer: fmtDec(Math.round((e - t) * 100) / 100) };
};

// F(): two denominators from DENOM whose lcm <= 12
const twoDenoms = (r: Rng): [number, number] => {
  let e: number, t: number;
  do { e = pick(DENOM, r); t = pick(DENOM, r); } while ((e * t) / gcd(e, t) > 12);
  return [e, t];
};
const addFrac = (r: Rng): Question => {
  for (;;) {
    const [t, rr] = twoDenoms(r);
    const n = sign(z(1, t - 1, r), r), i = z(1, rr - 1, r);
    const a = (t * rr) / gcd(t, rr);
    const l = (a / t) * n + (a / rr) * i;
    if (l === 0) continue;
    return { text: `${fracTerm(n, t)} + ${fracTerm(i, rr)}`, answer: fracReduced(l, a) };
  }
};
const subFrac = (r: Rng): Question => {
  for (;;) {
    const [t, rr] = twoDenoms(r);
    const n = sign(z(1, t - 1, r), r), i = z(1, rr - 1, r);
    const a = (t * rr) / gcd(t, rr);
    const l = (a / t) * n - (a / rr) * i;
    if (l === 0) continue;
    return { text: `${fracTerm(n, t)} − ${fracTerm(i, rr)}`, answer: fracReduced(l, a) };
  }
};

// weighted pool — integers appear twice as often as decimals/fractions
const POOL: ((r: Rng) => Question)[] = [
  addInt, addInt, subInt, subInt, mulInt, mulInt, divInt, divInt,
  addDec, subDec, addFrac, subFrac,
];

export const generateQuestions = (r: Rng = Math.random): Question[] =>
  Array.from({ length: QUESTION_COUNT }, () => pick(POOL, r)(r));

/**
 * The site's answer check: trim; exact string match (handles fractions), else
 * both parse as numbers within 1e-4. parseFloat gives JS leading-number semantics.
 */
export const checkAnswer = (user: string, correct: string): boolean => {
  const r = user.trim(), n = correct.trim();
  if (r === "") return false;
  if (r === n) return true;
  const a = parseFloat(r), b = parseFloat(n);
  return !Number.isNaN(a) && !Number.isNaN(b) && Math.abs(a - b) < 1e-4;
};
