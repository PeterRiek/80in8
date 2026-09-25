import { C } from "./theme.ts";

// Score bands from 80in8.com (min, range, label, colour, advice) — 1:1 with Android.
export type Band = { minScore: number; range: string; label: string; color: string; advice: string };

export const BANDS: Band[] = [
  { minScore: 75, range: "75–80", label: "Cracked", color: C.cyan, advice: "Maintain and stress-test. You're performing at a top level — train with harder variations to stay sharp." },
  { minScore: 70, range: "70–74", label: "Excellent", color: C.emerald, advice: "Refine precision under pressure. Aim to eliminate the last few mistakes." },
  { minScore: 60, range: "60–69", label: "Strong", color: C.lime, advice: "Push your speed. Accuracy is solid; work on quicker recall and mental shortcuts." },
  { minScore: 50, range: "50–59", label: "Solid", color: C.amber, advice: "Improve consistency. You're close — reduce careless errors and sharpen basic operations." },
  { minScore: -Infinity, range: "< 50", label: "Keep Practicing", color: C.redC, advice: "Focus on fundamentals. Slow down slightly and prioritise accuracy before rebuilding speed." },
];

export const bandFor = (net: number): Band => BANDS.find((b) => net >= b.minScore)!;
