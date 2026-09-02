export interface Pt {
  x: number;
  y: number;
}

export const dist = (a: Pt, b: Pt): number => Math.hypot(a.x - b.x, a.y - b.y);

export function pathLength(pts: Pt[]): number {
  let l = 0;
  for (let i = 1; i < pts.length; i++) l += dist(pts[i - 1], pts[i]);
  return l;
}

/** Point at distance d along a polyline, plus travel heading in degrees (0 = north/up). */
export function pointAt(pts: Pt[], d: number): { p: Pt; heading: number } {
  if (pts.length === 0) return { p: { x: 0, y: 0 }, heading: 0 };
  if (pts.length === 1) return { p: pts[0], heading: 0 };
  let rem = Math.max(0, d);
  for (let i = 1; i < pts.length; i++) {
    const seg = dist(pts[i - 1], pts[i]);
    if (rem <= seg || i === pts.length - 1) {
      const t = seg === 0 ? 0 : Math.min(1, rem / seg);
      const p = {
        x: pts[i - 1].x + (pts[i].x - pts[i - 1].x) * t,
        y: pts[i - 1].y + (pts[i].y - pts[i - 1].y) * t,
      };
      const heading = headingDeg(pts[i - 1], pts[i]);
      return { p, heading };
    }
    rem -= seg;
  }
  return { p: pts[pts.length - 1], heading: 0 };
}

/** Heading from a to b in screen degrees: 0 = north (up), 90 = east. */
export function headingDeg(a: Pt, b: Pt): number {
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  if (dx === 0 && dy === 0) return 0;
  return (Math.atan2(dx, -dy) * 180) / Math.PI < 0
    ? (Math.atan2(dx, -dy) * 180) / Math.PI + 360
    : (Math.atan2(dx, -dy) * 180) / Math.PI;
}

/** Nearest point on a single segment, returns the point and param t. */
function nearestOnSeg(a: Pt, b: Pt, p: Pt): { q: Pt; t: number } {
  const abx = b.x - a.x;
  const aby = b.y - a.y;
  const len2 = abx * abx + aby * aby;
  const t = len2 === 0 ? 0 : Math.max(0, Math.min(1, ((p.x - a.x) * abx + (p.y - a.y) * aby) / len2));
  return { q: { x: a.x + abx * t, y: a.y + aby * t }, t };
}

/** Nearest point among a set of independent segments (corridor network). */
export function nearestOnSegments(segs: Pt[][], p: Pt): Pt {
  let best: Pt = p;
  let bd = Infinity;
  for (const seg of segs) {
    for (let i = 1; i < seg.length; i++) {
      const { q } = nearestOnSeg(seg[i - 1], seg[i], p);
      const d = dist(q, p);
      if (d < bd) {
        bd = d;
        best = q;
      }
    }
  }
  return best;
}

/** Distance-along-path of the nearest point on a polyline to p. */
export function progressNear(pts: Pt[], p: Pt): { progress: number; point: Pt } {
  let best = 0;
  let bd = Infinity;
  let bq: Pt = pts[0];
  let acc = 0;
  for (let i = 1; i < pts.length; i++) {
    const { q } = nearestOnSeg(pts[i - 1], pts[i], p);
    const d = dist(q, p);
    if (d < bd) {
      bd = d;
      best = acc + dist(pts[i - 1], q);
      bq = q;
    }
    acc += dist(pts[i - 1], pts[i]);
  }
  return { progress: best, point: bq };
}

/** Compass name from a movement delta (screen coords, y down). */
export function dirName(a: Pt, b: Pt): string {
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  if (Math.abs(dx) > Math.abs(dy)) return dx > 0 ? "east" : "west";
  return dy > 0 ? "south" : "north";
}

export interface Step {
  kind: "depart" | "turn" | "stairs" | "arrive";
  turn?: "left" | "right";
  dist?: number;
  at: number; // distance along path where this step starts
  text: string;
}

/** Build turn-by-turn instructions from a polyline. finalKind marks the terminal cue. */
export function buildSteps(pts: Pt[], finalText: string, finalKind: "arrive" | "stairs" = "arrive"): Step[] {
  const steps: Step[] = [];
  let acc = 0;
  for (let i = 1; i < pts.length; i++) {
    const a = pts[i - 1];
    const b = pts[i];
    const d = dist(a, b);
    if (i === 1) {
      steps.push({ kind: "depart", dist: d, at: 0, text: `Head ${dirName(a, b)} for ${d.toFixed(1)} m` });
    } else {
      const p = pts[i - 2];
      const ax = a.x - p.x;
      const ay = a.y - p.y;
      const bx = b.x - a.x;
      const by = b.y - a.y;
      const cross = ax * by - ay * bx; // y-down: positive = clockwise = turn right
      const dot = ax * bx + ay * by;
      const last = steps[steps.length - 1];
      if (Math.abs(cross) < 1e-6 && dot > 0) {
        // collinear — merge into previous step
        last.dist = (last.dist ?? 0) + d;
        const base = last.kind === "turn" ? `Turn ${last.turn} — continue` : `Head ${dirName(p, a)} for`;
        last.text = `${base} ${(last.dist ?? d).toFixed(1)} m`;
      } else {
        const turn: "left" | "right" = cross > 0 ? "right" : "left";
        steps.push({ kind: "turn", turn, dist: d, at: acc, text: `Turn ${turn} — continue ${d.toFixed(1)} m` });
      }
    }
    acc += d;
  }
  steps.push({ kind: finalKind, at: acc, text: finalText });
  return steps;
}

/** Index of the active step for a given progress. */
export function activeStepIndex(steps: Step[], progress: number): number {
  let idx = 0;
  for (let i = 0; i < steps.length; i++) {
    if (steps[i].at <= progress + 0.12) idx = i;
  }
  return Math.min(idx, steps.length - 1);
}

/** Log-normal path-loss model: RSSI -> estimated distance. */
export function rssiToDistance(rssi: number, txPower = -59, n = 2.4): number {
  return Math.pow(10, (txPower - rssi) / (10 * n));
}

export function distanceToRssi(d: number, txPower = -59, n = 2.4): number {
  return txPower - 10 * n * Math.log10(Math.max(d, 0.3));
}

export const clamp = (v: number, lo: number, hi: number): number => Math.min(hi, Math.max(lo, v));

/** Deterministic-ish gaussian from Math.random (Box–Muller, one sample). */
export function gauss(): number {
  let u = 0;
  let v = 0;
  while (u === 0) u = Math.random();
  while (v === 0) v = Math.random();
  return Math.sqrt(-2 * Math.log(u)) * Math.cos(2 * Math.PI * v);
}
