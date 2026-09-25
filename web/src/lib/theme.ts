"use client";
import { createTheme } from "@mui/material/styles";

// palette ported 1:1 from the Android app (ui/theme/Theme.kt)
export const C = {
  cyan: "#00E5FF",
  dimCyan: "#0A4A52",
  black: "#000000",
  white: "#FFFFFF",
  surface: "#121212",
  faded: "#4A4A4A",
  wrong: "#FF5252",
  emerald: "#10B981",
  emeraldText: "#34D399",
  emeraldBg: "rgba(16,185,129,0.10)",
  redC: "#EF4444",
  redText: "#F87171",
  redBg: "rgba(239,68,68,0.10)",
  lime: "#A3E635",
  amber: "#F59E0B",
  zinc200: "#E4E4E7",
  zinc400: "#A1A1AA",
  zinc500: "#71717A",
  zinc800: "#27272A",
  skippedBg: "rgba(39,39,42,0.40)",
} as const;

export const MONO = "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace";

export const theme = createTheme({
  palette: {
    mode: "dark",
    primary: { main: C.cyan, contrastText: C.black },
    background: { default: C.black, paper: C.surface },
    text: { primary: C.white, secondary: C.zinc400 },
  },
  shape: { borderRadius: 12 },
});
