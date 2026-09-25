// localStorage-backed game history — the web stand-in for the Android Room DB.
// ponytail: localStorage only, per-device. Swap for a backend API when web gets one.
export type Game = {
  id: number;
  playedAt: number; // epoch millis
  score: number;
  right: number;
  wrong: number;
  skipped: number;
};

const KEY = "80in8.games";

export function loadGames(): Game[] {
  if (typeof window === "undefined") return [];
  try {
    return JSON.parse(localStorage.getItem(KEY) || "[]");
  } catch {
    return [];
  }
}

export function addGame(g: Omit<Game, "id">): Game[] {
  const games = loadGames();
  games.push({ ...g, id: (games[games.length - 1]?.id ?? 0) + 1 });
  localStorage.setItem(KEY, JSON.stringify(games));
  return games;
}

export function clearGames(): void {
  localStorage.removeItem(KEY);
}
