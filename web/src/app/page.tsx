"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  Box, Button, Card, Dialog, DialogActions, DialogContent, DialogTitle,
  IconButton, LinearProgress, Menu, MenuItem, Stack, TextField, Typography,
} from "@mui/material";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CancelIcon from "@mui/icons-material/Cancel";
import RemoveIcon from "@mui/icons-material/Remove";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import EmojiEventsIcon from "@mui/icons-material/EmojiEvents";
import { C, MONO } from "@/lib/theme.ts";
import { BANDS, bandFor } from "@/lib/score.ts";
import {
  GAME_SECONDS, QUESTION_COUNT, checkAnswer, generateQuestions, type Question,
} from "@/lib/questions.ts";
import { addGame, clearGames, loadGames, type Game } from "@/lib/storage.ts";

/** Result of one finished game (net score: right +1, wrong −1, blank 0). */
class PlayedGame {
  right: number; wrong: number; blank: number; score: number;
  constructor(public questions: Question[], public answers: string[], public timeUsed: number) {
    this.right = questions.reduce((a, q, i) => a + (checkAnswer(answers[i], q.answer) ? 1 : 0), 0);
    this.wrong = questions.reduce((a, q, i) => a + (answers[i].trim() !== "" && !checkAnswer(answers[i], q.answer) ? 1 : 0), 0);
    this.blank = QUESTION_COUNT - this.right - this.wrong;
    this.score = this.right - this.wrong;
  }
}

type Screen =
  | { name: "home" }
  | { name: "play" }
  | { name: "result"; game: PlayedGame }
  | { name: "history" };

export default function Page() {
  const [screen, setScreen] = useState<Screen>({ name: "home" });
  return (
    <Box sx={{ minHeight: "100dvh", bgcolor: C.black, color: C.white }}>
      {screen.name === "home" && (
        <HomeScreen onPlay={() => setScreen({ name: "play" })} onHistory={() => setScreen({ name: "history" })} />
      )}
      {screen.name === "play" && (
        <PlayScreen
          onFinish={(g) => setScreen({ name: "result", game: g })}
          onCancel={() => setScreen({ name: "home" })}
        />
      )}
      {screen.name === "result" && (
        <ResultScreen
          game={screen.game}
          onPlayAgain={() => setScreen({ name: "play" })}
          onHome={() => setScreen({ name: "home" })}
        />
      )}
      {screen.name === "history" && <HistoryScreen onBack={() => setScreen({ name: "home" })} />}
    </Box>
  );
}

/* ----------------------------- Home ----------------------------- */

function HomeScreen({ onPlay, onHistory }: { onPlay: () => void; onHistory: () => void }) {
  return (
    <Stack alignItems="center" sx={{ px: 3, py: 4, maxWidth: 420, mx: "auto" }}>
      <Typography sx={{ fontFamily: MONO, fontSize: 56, fontWeight: 700, letterSpacing: "-2px", color: C.cyan }}>
        80in8
      </Typography>
      <Typography sx={{ color: C.cyan, fontSize: 11, fontWeight: 500, letterSpacing: "2px", textAlign: "center", mt: 1 }}>
        PREP FOR CRACKING THE QUANT ASSESSMENT
      </Typography>
      <Typography sx={{ color: C.zinc400, fontSize: 16, mt: 1.5 }}>80 math questions · 8 minutes</Typography>

      <Stack direction="row" spacing={2} sx={{ mt: 2.5, fontFamily: MONO }}>
        <Typography sx={{ color: C.emerald, fontFamily: MONO, fontSize: 13 }}>+1 correct</Typography>
        <Typography sx={{ color: C.redC, fontFamily: MONO, fontSize: 13 }}>−1 wrong</Typography>
        <Typography sx={{ color: C.zinc400, fontFamily: MONO, fontSize: 13 }}>0 skipped</Typography>
      </Stack>

      <Box sx={{ width: "100%", mt: 2.5, border: `1px solid ${C.zinc800}`, borderRadius: "16px", overflow: "hidden" }}>
        <Typography sx={{ color: C.zinc500, fontSize: 11, fontWeight: 600, letterSpacing: "1.5px", px: 2, py: 1.5 }}>
          SCORING BENCHMARKS
        </Typography>
        {BANDS.map((b) => (
          <Stack key={b.range} direction="row" justifyContent="space-between" sx={{ px: 2, py: 1.25 }}>
            <Typography sx={{ color: C.zinc400, fontFamily: MONO, fontSize: 14 }}>{b.range}</Typography>
            <Typography sx={{ color: b.color, fontSize: 14, fontWeight: 600 }}>{b.label}</Typography>
          </Stack>
        ))}
      </Box>

      <Button
        onClick={onPlay}
        variant="contained"
        sx={{ mt: 3, width: "100%", maxWidth: 320, height: 60, bgcolor: C.cyan, color: C.black, borderRadius: "18px", fontSize: 18, fontWeight: 600, "&:hover": { bgcolor: C.cyan } }}
      >
        Start Quiz
      </Button>
      <Typography sx={{ color: C.zinc500, fontSize: 12, mt: 1.25 }}>Press Enter to advance · no going back</Typography>

      <Button
        onClick={onHistory}
        sx={{ mt: 2, width: "100%", maxWidth: 320, height: 52, bgcolor: C.surface, color: C.zinc400, borderRadius: "16px", fontWeight: 700 }}
      >
        PROGRESS
      </Button>
    </Stack>
  );
}

/* ----------------------------- Play ----------------------------- */

function PlayScreen({ onFinish, onCancel }: { onFinish: (g: PlayedGame) => void; onCancel: () => void }) {
  const questions = useMemo(() => generateQuestions(), []);
  const answers = useRef<string[]>(Array(QUESTION_COUNT).fill(""));
  const [index, setIndex] = useState(0);
  const [input, setInput] = useState("");
  const [secondsLeft, setSecondsLeft] = useState(GAME_SECONDS);
  const [showConfirm, setShowConfirm] = useState(false);
  const doneRef = useRef(false);
  const fieldRef = useRef<HTMLInputElement>(null);

  const finish = (used: number) => {
    if (doneRef.current) return;
    doneRef.current = true;
    onFinish(new PlayedGame(questions, [...answers.current], used));
  };

  // countdown
  useEffect(() => {
    const id = setInterval(() => {
      setSecondsLeft((s) => {
        if (s <= 1) {
          clearInterval(id);
          finish(GAME_SECONDS);
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => { fieldRef.current?.focus(); }, [index]);

  const submit = () => {
    answers.current[index] = input.trim();
    setInput("");
    if (index + 1 >= QUESTION_COUNT) finish(GAME_SECONDS - secondsLeft);
    else setIndex((i) => i + 1);
  };

  const low = secondsLeft <= 30;
  const mm = Math.floor(secondsLeft / 60);
  const ss = secondsLeft % 60;

  return (
    <Stack sx={{ minHeight: "100dvh", py: 3 }}>
      <Typography sx={{ px: 3, textAlign: "center", fontSize: 20, fontWeight: 700, color: low ? C.wrong : C.cyan }}>
        {mm}:{String(ss).padStart(2, "0")}
      </Typography>
      <LinearProgress
        variant="determinate"
        value={(secondsLeft / GAME_SECONDS) * 100}
        sx={{
          mt: 1, height: 4,
          bgcolor: C.surface,
          "& .MuiLinearProgress-bar": { bgcolor: low ? "rgba(255,82,82,0.6)" : C.dimCyan },
        }}
      />

      <Stack sx={{ flex: 1, px: 3, justifyContent: "center", alignItems: "center" }}>
        <Typography sx={{ color: C.faded, fontSize: 15, textAlign: "center", width: "100%" }}>
          {index + 1}/{QUESTION_COUNT} Questions
        </Typography>
        <Typography sx={{ mt: 2, fontSize: 52, fontWeight: 900, color: C.white, textAlign: "center", width: "100%" }}>
          {questions[index].text}
        </Typography>
        <TextField
          inputRef={fieldRef}
          value={input}
          onChange={(e) => setInput(e.target.value.replace(/[^0-9.\-/]/g, ""))}
          onKeyDown={(e) => { if (e.key === "Enter") submit(); }}
          autoFocus
          fullWidth
          sx={{ mt: 3.5, maxWidth: 420 }}
          inputProps={{ inputMode: "text", style: { textAlign: "center", fontSize: 28, color: C.white } }}
        />
        <Typography sx={{ mt: 1.25, color: C.zinc500, fontSize: 13, textAlign: "center", width: "100%" }}>
          press enter to submit
        </Typography>
      </Stack>

      <Box sx={{ display: "flex", justifyContent: "center", pb: 2 }}>
        <IconButton
          onClick={() => setShowConfirm(true)}
          sx={{ width: 48, height: 48, border: `1px solid ${C.zinc800}`, color: C.zinc400 }}
        >
          ✕
        </IconButton>
      </Box>

      <Dialog open={showConfirm} onClose={() => setShowConfirm(false)} PaperProps={{ sx: { bgcolor: C.surface } }}>
        <DialogTitle sx={{ color: C.white }}>Cancel this run?</DialogTitle>
        <DialogContent><Typography sx={{ color: C.zinc400 }}>This game won&apos;t be saved to your history.</Typography></DialogContent>
        <DialogActions>
          <Button onClick={onCancel} sx={{ color: C.redC, fontWeight: 700 }}>Quit</Button>
          <Button onClick={() => setShowConfirm(false)} sx={{ color: C.zinc400 }}>Keep playing</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

/* ----------------------------- Result ----------------------------- */

function ResultScreen({ game, onPlayAgain, onHome }: { game: PlayedGame; onPlayAgain: () => void; onHome: () => void }) {
  // save exactly once (ref guard survives StrictMode's double-invoke in dev)
  const saved = useRef(false);
  useEffect(() => {
    if (saved.current) return;
    saved.current = true;
    addGame({ playedAt: Date.now(), score: game.score, right: game.right, wrong: game.wrong, skipped: game.blank });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const band = bandFor(game.score);
  const mm = Math.floor(game.timeUsed / 60);
  const ss = game.timeUsed % 60;
  const timeText =
    game.timeUsed >= GAME_SECONDS ? "Time expired"
    : game.blank === 0 ? `Finished in ${mm}m ${ss}s`
    : `${mm}m ${ss}s elapsed`;

  return (
    <Stack alignItems="center" sx={{ minHeight: "100dvh", px: 2, py: 2 }}>
      <Typography sx={{ color: band.color, fontSize: 18, fontWeight: 500 }}>{band.label}</Typography>
      <Typography sx={{ fontFamily: MONO, fontSize: 64, fontWeight: 700, color: C.white }}>{game.score}</Typography>
      <Stack direction="row" sx={{ fontFamily: MONO, fontSize: 14 }}>
        <Typography sx={{ color: C.emerald, fontFamily: MONO, fontSize: 14 }}>+{game.right}</Typography>
        <Typography sx={{ color: C.zinc500, fontFamily: MONO, fontSize: 14 }}>&nbsp;·&nbsp;</Typography>
        <Typography sx={{ color: C.redC, fontFamily: MONO, fontSize: 14 }}>−{game.wrong}</Typography>
        {game.blank > 0 && (
          <>
            <Typography sx={{ color: C.zinc500, fontFamily: MONO, fontSize: 14 }}>&nbsp;·&nbsp;</Typography>
            <Typography sx={{ color: C.zinc400, fontFamily: MONO, fontSize: 14 }}>{game.blank} skipped</Typography>
          </>
        )}
      </Stack>
      <Typography sx={{ color: C.zinc500, fontSize: 13, mt: 0.5 }}>{timeText}</Typography>
      <Typography sx={{ color: C.zinc400, fontSize: 13, textAlign: "center", maxWidth: 340, mt: 1.25 }}>{band.advice}</Typography>

      <Stack direction="row" spacing={1.5} sx={{ mt: 2.5 }}>
        <Button onClick={onHome} variant="outlined" sx={{ borderColor: C.zinc800, color: C.zinc400, fontWeight: 600 }}>Home</Button>
        <Button onClick={onPlayAgain} variant="contained" sx={{ bgcolor: C.cyan, color: C.black, fontWeight: 600, "&:hover": { bgcolor: C.cyan } }}>Try Again</Button>
      </Stack>

      <Box sx={{ flex: 1, width: "100%", maxWidth: 560, mt: 2, border: `1px solid ${C.zinc800}`, borderRadius: "16px", p: 1.5, overflowY: "auto" }}>
        <Stack spacing={0.75}>
          {game.questions.map((q, i) => (
            <AnswerRow key={i} number={i + 1} question={q.text} given={game.answers[i]} answer={q.answer} />
          ))}
        </Stack>
      </Box>
    </Stack>
  );
}

function AnswerRow({ number, question, given, answer }: { number: number; question: string; given: string; answer: string }) {
  const skipped = given.trim() === "";
  const correct = checkAnswer(given, answer);
  const bg = correct ? C.emeraldBg : skipped ? C.skippedBg : C.redBg;
  const ansColor = correct ? C.emeraldText : skipped ? C.zinc400 : C.redText;
  const Icon = correct ? CheckCircleIcon : skipped ? RemoveIcon : CancelIcon;
  return (
    <Stack direction="row" alignItems="center" sx={{ bgcolor: bg, borderRadius: "8px", px: 1.75, py: 1.5 }}>
      <Typography sx={{ color: C.zinc500, fontFamily: MONO, fontSize: 12, textAlign: "right", width: 22 }}>{number}</Typography>
      <Icon sx={{ ml: 1.25, fontSize: 18, color: ansColor }} />
      <Typography noWrap sx={{ flex: 1, color: C.zinc200, fontFamily: MONO, fontSize: 15, ml: 1.25 }}>{question}</Typography>
      {!correct && !skipped && (
        <Typography sx={{ color: C.zinc500, fontFamily: MONO, fontSize: 14, textDecoration: "line-through", ml: 1 }}>{given}</Typography>
      )}
      <Typography sx={{ color: ansColor, fontFamily: MONO, fontWeight: 600, fontSize: 14, ml: 1.25 }}>{answer}</Typography>
    </Stack>
  );
}

/* ----------------------------- History ----------------------------- */

function fmtDate(ms: number): string {
  const d = new Date(ms);
  const mon = d.toLocaleString("en", { month: "short" });
  return `${d.getDate()} ${mon} · ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function HistoryScreen({ onBack }: { onBack: () => void }) {
  const [games, setGames] = useState<Game[]>([]);
  const [menuEl, setMenuEl] = useState<null | HTMLElement>(null);
  const [confirmReset, setConfirmReset] = useState(false);

  useEffect(() => { setGames(loadGames()); }, []);

  return (
    <Stack sx={{ minHeight: "100dvh", p: 3, maxWidth: 720, mx: "auto" }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography sx={{ fontSize: 32, fontWeight: 900, color: C.cyan }}>Progress</Typography>
        <IconButton onClick={(e) => setMenuEl(e.currentTarget)}><MoreVertIcon sx={{ color: C.zinc400 }} /></IconButton>
        <Menu anchorEl={menuEl} open={!!menuEl} onClose={() => setMenuEl(null)}>
          <MenuItem onClick={() => { setMenuEl(null); setConfirmReset(true); }} sx={{ color: C.redC }}>
            Reset progress
          </MenuItem>
        </Menu>
      </Stack>

      <Box sx={{ mt: 2 }}>
        {games.length < 2 ? (
          <Typography sx={{ color: C.faded }}>Play at least 2 games to see your progress.</Typography>
        ) : (
          <ScoreChart scores={games.map((g) => g.score)} />
        )}
      </Box>

      <Typography sx={{ mt: 3, color: C.white, fontWeight: 600 }}>All games</Typography>
      <Box sx={{ flex: 1, mt: 1, overflowY: "auto" }}>
        {[...games].reverse().map((g) => <GameRow key={g.id} game={g} />)}
      </Box>

      <Button onClick={onBack} sx={{ mt: 2, height: 52, bgcolor: C.surface, color: C.zinc400, borderRadius: "16px", fontWeight: 700 }}>
        BACK
      </Button>

      <Dialog open={confirmReset} onClose={() => setConfirmReset(false)} PaperProps={{ sx: { bgcolor: C.surface } }}>
        <DialogTitle sx={{ color: C.white }}>Reset progress?</DialogTitle>
        <DialogContent><Typography sx={{ color: C.zinc400 }}>This permanently deletes all {games.length} saved games.</Typography></DialogContent>
        <DialogActions>
          <Button onClick={() => { clearGames(); setGames([]); setConfirmReset(false); }} sx={{ color: C.redC, fontWeight: 700 }}>Reset</Button>
          <Button onClick={() => setConfirmReset(false)} sx={{ color: C.zinc400 }}>Cancel</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

function GameRow({ game }: { game: Game }) {
  const stat = (Icon: typeof CheckCircleIcon, count: number, color: string) => (
    <Stack direction="row" alignItems="center" spacing={0.5}>
      <Icon sx={{ fontSize: 15, color }} />
      <Typography sx={{ color, fontFamily: MONO, fontSize: 13, width: 18 }}>{count}</Typography>
    </Stack>
  );
  return (
    <Stack direction="row" alignItems="center" sx={{ py: 1.25 }}>
      <Typography sx={{ flex: 1, color: C.zinc400, fontSize: 13 }}>{fmtDate(game.playedAt)}</Typography>
      <Stack direction="row" spacing={2} alignItems="center">
        {stat(CheckCircleIcon, game.right, C.emeraldText)}
        {stat(CancelIcon, game.wrong, C.redText)}
        {stat(RemoveIcon, game.skipped, C.zinc400)}
      </Stack>
      <Stack direction="row" alignItems="center" spacing={0.5} sx={{ width: 64, pl: 2, color: bandFor(game.score).color }}>
        <EmojiEventsIcon sx={{ fontSize: 15 }} />
        <Typography sx={{ whiteSpace: "nowrap", fontWeight: 700, fontSize: 15, fontFamily: MONO }}>
          {game.score}
        </Typography>
      </Stack>
    </Stack>
  );
}

function ScoreChart({ scores }: { scores: number[] }) {
  const ref = useRef<HTMLDivElement>(null);
  const [w, setW] = useState(600);
  const h = 240, pad = 16;
  useEffect(() => {
    if (!ref.current) return;
    const ro = new ResizeObserver(([e]) => setW(e.contentRect.width));
    ro.observe(ref.current);
    return () => ro.disconnect();
  }, []);

  const hi = Math.max(...scores, 1);
  const lo = Math.min(...scores, 0); // always include zero
  const range = Math.max(hi - lo, 1);
  const n = scores.length;
  const innerW = w - pad * 2, innerH = h - pad * 2;
  const x = (i: number) => pad + (n < 2 ? 0 : (i / (n - 1)) * innerW);
  const y = (v: number) => pad + innerH - ((v - lo) / range) * innerH;
  const colors = scores.map((v) => bandFor(v).color);

  return (
    <Card ref={ref} sx={{ bgcolor: C.surface, borderRadius: "16px", p: 0 }}>
      <svg width="100%" height={h} viewBox={`0 0 ${w} ${h}`}>
        <line x1={pad} y1={y(0)} x2={w - pad} y2={y(0)} stroke={C.faded} strokeWidth={2} />
        {scores.slice(0, -1).map((_, i) => (
          <line key={i} x1={x(i)} y1={y(scores[i])} x2={x(i + 1)} y2={y(scores[i + 1])} stroke={colors[i + 1]} strokeWidth={2.5} />
        ))}
        {scores.map((v, i) => (
          <g key={i}>
            <circle cx={x(i)} cy={y(v)} r={4} fill={colors[i]} />
            <circle cx={x(i)} cy={y(v)} r={1.8} fill={C.white} />
          </g>
        ))}
      </svg>
    </Card>
  );
}
