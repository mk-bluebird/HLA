import { useEffect, useReducer, useRef, useState } from "react";
import { ALL_DESTS, ENTRANCE, FLOORS, PRIVACY_POINTS, ROUTES } from "../lib/data";
import type { Destination } from "../lib/data";
import {
  buildSteps,
  clamp,
  dist,
  distanceToRssi,
  gauss,
  nearestOnSegments,
  pathLength,
  pointAt,
  progressNear,
} from "../lib/geo";
import type { Pt, Step } from "../lib/geo";
import { useReducedMotion, useSpeech } from "../lib/hooks";
import FloorMap from "./FloorMap";
import {
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  Beacon as BeaconIcon,
  Flag,
  Play,
  Pulse,
  Speaker,
  SpeakerOff,
  Stairs,
  Wave,
} from "./icons";

/* ------------------------------ types ------------------------------ */

interface Leg {
  level: 1 | 2;
  pts: Pt[];
  len: number;
  steps: Step[];
  startAt: number;
}

interface Nav {
  destId: string;
  dest: Destination;
  legs: Leg[];
  legIdx: number;
  progress: number;
  phase: "walk" | "stairs" | "done";
}

interface BeaconReading {
  id: string;
  d: number;
  rssi: number;
  inRange: boolean;
}

interface SimS {
  level: 1 | 2;
  user: Pt;
  heading: number;
  prevHeading: number;
  error: number;
  walking: boolean;
  nav: Nav | null;
  interference: boolean;
  corrections: boolean;
  speed: number;
  steps: number;
  stepTimer: number;
  accelMag: number;
  accelArr: number[];
  ax: number;
  ay: number;
  az: number;
  yawRate: number;
  rawPdr: number;
  rawDriftArr: number[];
  fusedDriftArr: number[];
  beaconReadings: BeaconReading[];
  beaconsInView: number;
  spokenKey: string;
  tickCount: number;
}

const makeSim = (): SimS => ({
  level: 1,
  user: { ...ENTRANCE },
  heading: 0,
  prevHeading: 0,
  error: 0.62,
  walking: false,
  nav: null,
  interference: false,
  corrections: true,
  speed: 1.3,
  steps: 0,
  stepTimer: 0,
  accelMag: 9.81,
  accelArr: Array.from({ length: 44 }, () => 9.81 + (Math.random() - 0.5) * 0.06),
  ax: 0.02,
  ay: 0.01,
  az: -9.79,
  yawRate: 0,
  rawPdr: 0.4,
  rawDriftArr: Array.from({ length: 30 }, () => 0.4),
  fusedDriftArr: Array.from({ length: 30 }, () => 0.62),
  beaconReadings: [],
  beaconsInView: 4,
  spokenKey: "",
  tickCount: 0,
});

/* ---------------------------- helpers ------------------------------ */

function pathFrom(pts: Pt[], d: number): Pt[] {
  let acc = 0;
  for (let i = 1; i < pts.length; i++) {
    const seg = dist(pts[i - 1], pts[i]);
    if (acc + seg >= d) {
      const { p } = pointAt(pts, d);
      return [p, ...pts.slice(i)];
    }
    acc += seg;
  }
  return [pts[pts.length - 1]];
}

const StepGlyph = ({ step, className }: { step: Step; className?: string }) => {
  if (step.kind === "arrive") return <Flag className={className} />;
  if (step.kind === "stairs") return <Stairs className={className} />;
  if (step.kind === "turn") return step.turn === "left" ? <ArrowLeft className={className} /> : <ArrowRight className={className} />;
  return <ArrowUp className={className} />;
};

function Spark({ data, max, color }: { data: number[]; max: number; color: string }) {
  if (data.length < 2) return null;
  const w = 100;
  const h = 26;
  const stepX = w / (data.length - 1);
  const pts = data.map((v, i) => `${(i * stepX).toFixed(1)},${(h - clamp(v / max, 0, 1) * (h - 2) - 1).toFixed(1)}`).join(" ");
  return (
    <svg viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none" className="w-full h-7 block">
      <polyline points={pts} fill="none" stroke={color} strokeWidth="1.7" />
    </svg>
  );
}

function DualSpark({ a, b, max }: { a: number[]; b: number[]; max: number }) {
  const w = 100;
  const h = 30;
  const toPts = (data: number[]) => {
    if (data.length < 2) return "";
    const stepX = w / (data.length - 1);
    return data.map((v, i) => `${(i * stepX).toFixed(1)},${(h - clamp(v / max, 0, 1) * (h - 2) - 1).toFixed(1)}`).join(" ");
  };
  return (
    <svg viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none" className="w-full h-9 block">
      <line x1="0" x2={w} y1={h - clamp(1 / max, 0, 1) * (h - 2) - 1} y2={h - clamp(1 / max, 0, 1) * (h - 2) - 1} stroke="var(--line)" strokeWidth="0.7" strokeDasharray="2 2" />
      <polyline points={toPts(a)} fill="none" stroke="var(--coral)" strokeWidth="1.6" />
      <polyline points={toPts(b)} fill="none" stroke="var(--teal)" strokeWidth="1.6" />
    </svg>
  );
}

const Card = ({ title, icon, children, className = "" }: { title: string; icon: React.ReactNode; children: React.ReactNode; className?: string }) => (
  <div className={`border p-3.5 ${className}`} style={{ borderColor: "var(--bd)", background: "var(--ink2)" }}>
    <div className="flex items-center gap-2 kicker mb-3" style={{ color: "var(--mut)" }}>
      <span style={{ color: "var(--teal)" }}>{icon}</span>
      {title}
    </div>
    {children}
  </div>
);

const Toggle = ({ on, label, onClick, hint }: { on: boolean; label: string; onClick: () => void; hint?: string }) => (
  <button
    onClick={onClick}
    aria-pressed={on}
    title={hint}
    className="flex items-center gap-2.5 kicker px-3 py-2 border transition-colors duration-200 cursor-pointer"
    style={{
      borderColor: on ? "var(--yellow)" : "var(--bd)",
      color: on ? "var(--ink)" : "var(--mut)",
      background: on ? "var(--yellow)" : "transparent",
    }}
  >
    <span className="w-2 h-2" style={{ background: on ? "var(--ink)" : "var(--mut)", opacity: on ? 1 : 0.5 }} />
    {label}
  </button>
);

/* ---------------------------- component ---------------------------- */

export default function Simulator({ onTelemetry }: { onTelemetry?: (err: number, inView: number) => void }) {
  const sim = useRef<SimS>(makeSim());
  const [, force] = useReducer((x: number) => x + 1, 0);
  const [destId, setDestId] = useState<string>("d-permits");
  const { enabled, setEnabled, speak, stop } = useSpeech();
  const reduced = useReducedMotion();

  const onTelRef = useRef(onTelemetry);
  onTelRef.current = onTelemetry;

  /* ----- simulation tick @ 10 Hz ----- */
  const tickRef = useRef<() => void>(() => {});
  tickRef.current = () => {
    const s = sim.current;
    const dt = 0.1;
    const floor = FLOORS[s.level];
    const navWalking = !!(s.nav && s.nav.phase === "walk" && s.walking);

    if (navWalking && s.nav) {
      const leg = s.nav.legs[s.nav.legIdx];
      s.nav.progress = Math.min(s.nav.progress + s.speed * dt, leg.len);
      const { p, heading } = pointAt(leg.pts, s.nav.progress);
      s.user = p;
      s.heading = heading;
      s.stepTimer += dt;
      if (s.stepTimer >= 0.55) {
        s.stepTimer -= 0.55;
        s.steps += 1;
        s.accelMag = 11.3 + Math.random() * 0.9;
      } else {
        s.accelMag = Math.max(9.81 + gauss() * 0.05, s.accelMag - 1.4);
      }
      const idx = leg.steps.reduce((acc, st, i) => (st.at <= s.nav!.progress + 0.12 ? i : acc), 0);
      const key = `${s.nav.legIdx}-${idx}`;
      if (key !== s.spokenKey) {
        s.spokenKey = key;
        speak(leg.steps[idx].text);
      }
      if (s.nav.progress >= leg.len) {
        if (s.nav.legIdx < s.nav.legs.length - 1) {
          s.nav.phase = "stairs";
          s.walking = false;
        } else {
          s.nav.phase = "done";
          s.walking = false;
          speak(`You have arrived at ${s.nav.dest.name}, ${s.nav.dest.code}.`);
        }
      }
    } else {
      s.accelMag = 9.81 + gauss() * 0.04;
      if (!s.nav || s.nav.phase === "done") s.heading += gauss() * 0.35;
    }

    s.ax = gauss() * (navWalking ? 0.32 : 0.03);
    s.ay = gauss() * (navWalking ? 0.28 : 0.03);
    s.az = -9.79 + gauss() * 0.05;
    const dh = ((s.heading - s.prevHeading + 540) % 360) - 180;
    s.yawRate = s.yawRate * 0.72 + (dh / dt) * 0.28;
    s.prevHeading = s.heading;
    s.accelArr.push(s.accelMag);
    if (s.accelArr.length > 44) s.accelArr.shift();

    /* beacon ranging */
    const cap = s.interference ? 7 : 14;
    s.beaconReadings = floor.beacons.map((b) => {
      const dTrue = dist(b, s.user);
      const d = Math.max(0.2, dTrue * (1 + gauss() * (s.interference ? 0.17 : 0.05)) + 0.08);
      const rssi = distanceToRssi(dTrue) + gauss() * (s.interference ? 6.5 : 2.2);
      return { id: b.id, d, rssi, inRange: dTrue < cap };
    });
    const n = s.beaconReadings.filter((r) => r.inRange).length;
    s.beaconsInView = n;

    /* fusion error model */
    let err: number;
    if (!s.corrections) {
      s.rawPdr = Math.min(6.5, s.rawPdr + 0.035 + Math.abs(gauss()) * 0.02);
      err = s.rawPdr;
    } else {
      s.rawPdr = navWalking
        ? Math.min(6.5, s.rawPdr + 0.02 + Math.abs(gauss()) * 0.014)
        : Math.max(0.4, s.rawPdr * 0.985);
      const base = (2.1 / Math.sqrt(Math.max(n, 1))) * (0.5 + (s.interference ? 0.85 : 0.28));
      err = clamp(base + Math.abs(gauss()) * 0.06 + (n < 3 ? 0.35 : 0), 0.26, 2.2);
    }
    s.error = err;
    s.rawDriftArr.push(s.rawPdr);
    s.fusedDriftArr.push(s.corrections ? err : s.rawPdr);
    if (s.rawDriftArr.length > 90) s.rawDriftArr.shift();
    if (s.fusedDriftArr.length > 90) s.fusedDriftArr.shift();

    s.tickCount += 1;
    if (s.tickCount % 5 === 0 && onTelRef.current) onTelRef.current(err, n);
  };

  useEffect(() => {
    const id = setInterval(() => {
      tickRef.current();
      force();
    }, 100);
    return () => {
      clearInterval(id);
      stop();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /* ----- actions ----- */
  const switchFloor = (l: 1 | 2) => {
    const s = sim.current;
    s.nav = null;
    s.walking = false;
    s.level = l;
    s.user = l === 1 ? { ...ENTRANCE } : { x: 30, y: 19 };
    s.spokenKey = "";
    force();
  };

  const resetNav = () => {
    const s = sim.current;
    s.nav = null;
    s.walking = false;
    s.rawPdr = 0.4;
    s.spokenKey = "";
    stop();
    force();
  };

  const startNav = () => {
    const s = sim.current;
    const route = ROUTES[destId];
    const dest = ALL_DESTS.find((d) => d.id === destId);
    if (!route || !dest) return;
    const legs: Leg[] = [];
    let startAt = 0;
    route.forEach((r, i) => {
      let pts = r.pts;
      if (i === 0) {
        const np = progressNear(pts, s.user);
        if (np.progress > 0.15) {
          let acc = 0;
          for (let k = 1; k < pts.length; k++) {
            const seg = dist(pts[k - 1], pts[k]);
            if (acc + seg >= np.progress) {
              pts = [pointAt(pts, np.progress).p, ...pts.slice(k)];
              break;
            }
            acc += seg;
          }
        }
      }
      const isLast = i === route.length - 1;
      const finalText = isLast
        ? `Arrive — ${dest.name}, ${dest.code}`
        : r.level === 1
          ? "Take stairs S1 up to Level 2"
          : "Take stairs S2 down to Level 1";
      const steps = buildSteps(pts, finalText, isLast ? "arrive" : "stairs");
      const len = pathLength(pts);
      legs.push({ level: r.level, pts, len, steps, startAt });
      startAt += len;
    });
    s.nav = { destId, dest, legs, legIdx: 0, progress: 0, phase: "walk" };
    s.walking = true;
    s.level = legs[0].level;
    if (legs[0].len > 0.001) {
      const { p, heading } = pointAt(legs[0].pts, 0);
      s.user = p;
      s.heading = heading;
    }
    s.spokenKey = "";
    speak(legs[0].steps[0]?.text ?? "");
    force();
  };

  const continueStairs = () => {
    const s = sim.current;
    if (!s.nav || s.nav.phase !== "stairs") return;
    s.nav.legIdx += 1;
    const leg = s.nav.legs[s.nav.legIdx];
    s.level = leg.level;
    s.nav.progress = 0;
    s.nav.phase = "walk";
    s.user = { ...leg.pts[0] };
    s.spokenKey = "";
    speak(leg.steps[0]?.text ?? "");
    force();
  };

  const placeUser = (p: Pt) => {
    const s = sim.current;
    const q = nearestOnSegments(FLOORS[s.level].walk, p);
    s.user = q;
    if (s.nav) {
      s.nav = null;
      s.walking = false;
      s.spokenKey = "";
    }
    force();
  };

  /* ----- derived ----- */
  const s = sim.current;
  const nav = s.nav;
  const flat = (nav?.legs ?? []).flatMap((leg, li) =>
    leg.steps.map((st) => ({ ...st, legIdx: li, globalAt: leg.startAt + st.at })),
  );
  const totalLen = (nav?.legs ?? []).reduce((a, l) => a + l.len, 0);
  const globalProgress = nav ? nav.legs[nav.legIdx].startAt + nav.progress : 0;
  let activeIdx = -1;
  flat.forEach((f, i) => {
    if (f.globalAt <= globalProgress + 0.12) activeIdx = i;
  });
  const routePts =
    nav && nav.phase !== "done" && nav.legs[nav.legIdx].level === s.level
      ? pathFrom(nav.legs[nav.legIdx].pts, nav.progress)
      : null;
  const dest = nav?.dest ?? null;
  const showDest = nav && nav.phase !== "done" && dest && dest.floor === s.level ? dest : null;
  const nextLeg = nav && nav.phase === "stairs" ? nav.legs[nav.legIdx + 1] : null;
  const driftMax = Math.max(2, ...s.rawDriftArr, ...s.fusedDriftArr);
  const topRanges = [...s.beaconReadings].sort((a, b) => a.d - b.d).slice(0, 4);
  const pass = s.error < 1;

  const dests1 = ALL_DESTS.filter((d) => d.floor === 1);
  const dests2 = ALL_DESTS.filter((d) => d.floor === 2);

  return (
    <section id="simulator" className="scroll-mt-24 mx-auto max-w-7xl px-4 sm:px-6 pb-20">
      {/* section header */}
      <div className="flex flex-wrap items-end justify-between gap-4 border-b pb-5 mb-6" style={{ borderColor: "var(--bd)" }}>
        <div>
          <p className="kicker" style={{ color: "var(--teal)" }}>
            live simulation · interactive
          </p>
          <h2 className="display mt-2" style={{ fontSize: "clamp(1.7rem, 3.4vw, 2.6rem)" }}>
            Meridian City Hall — Level {s.level}
          </h2>
        </div>
        <p className="kicker max-w-xs text-right" style={{ color: "var(--mut)" }}>
          pick a destination, start the route, and watch the fusion stack keep error &lt; 1 m
        </p>
      </div>

      <div className="grid lg:grid-cols-12 gap-5">
        {/* ---------------- left: map + controls ---------------- */}
        <div className="lg:col-span-7 flex flex-col gap-5">
          <div className="border" style={{ borderColor: "var(--bd)", background: "var(--ink)" }}>
            <div className="flex items-center gap-3 px-4 py-3 border-b" style={{ borderColor: "var(--bd)" }}>
              <div className="flex">
                {[1, 2].map((l) => (
                  <button
                    key={l}
                    onClick={() => switchFloor(l as 1 | 2)}
                    className="display px-4 py-1.5 text-sm border transition-colors duration-200 cursor-pointer"
                    style={{
                      borderColor: "var(--bd)",
                      background: s.level === l ? "var(--yellow)" : "transparent",
                      color: s.level === l ? "var(--ink)" : "var(--mut)",
                    }}
                  >
                    L{l}
                  </button>
                ))}
              </div>
              <span className="kicker ml-auto flex items-center gap-2" style={{ color: "var(--mut)" }}>
                <span className="w-1.5 h-1.5 rounded-full blinkdot" style={{ background: s.beaconsInView >= 3 ? "var(--teal)" : "var(--coral)" }} />
                {s.beaconsInView}/12 beacons in view
              </span>
            </div>

            <FloorMap
              floor={FLOORS[s.level]}
              user={{ x: s.user.x, y: s.user.y, headingDeg: s.heading, error: s.error }}
              routePts={routePts}
              destination={showDest}
              walking={s.walking}
              reduced={reduced}
              stairsHot={nav?.phase === "stairs"}
              onPick={placeUser}
            />

            <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 px-4 py-3 border-t kicker" style={{ borderColor: "var(--bd)", color: "var(--mut)" }}>
              <span className="flex items-center gap-2">
                <span className="inline-block w-6 border-t-2 border-dashed" style={{ borderColor: "var(--yellow)" }} /> route
              </span>
              <span className="flex items-center gap-2">
                <span className="inline-block w-2 h-2 rotate-45" style={{ background: "var(--teal)" }} /> beacon
              </span>
              <span className="flex items-center gap-2">
                <span className="inline-block w-2.5 h-2.5 rounded-full" style={{ background: "var(--yellow)" }} /> you
              </span>
              <span className="flex items-center gap-2">
                <Flag className="w-3.5 h-3.5" /> destination
              </span>
              <span className="ml-auto hidden sm:inline" style={{ color: "var(--mut)" }}>
                tap the plan to reposition
              </span>
            </div>
          </div>

          {/* route controls */}
          <div className="border p-4" style={{ borderColor: "var(--bd)", background: "var(--ink2)" }}>
            <div className="grid sm:grid-cols-2 gap-4">
              {[
                { label: "Floor 1", list: dests1 },
                { label: "Floor 2", list: dests2 },
              ].map((g) => (
                <div key={g.label}>
                  <p className="kicker mb-2" style={{ color: "var(--mut)" }}>
                    {g.label}
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {g.list.map((d) => (
                      <button
                        key={d.id}
                        onClick={() => setDestId(d.id)}
                        className="kicker px-2.5 py-1.5 border transition-all duration-200 cursor-pointer hover:-translate-y-0.5"
                        style={{
                          borderColor: destId === d.id ? "var(--yellow)" : "var(--bd)",
                          color: destId === d.id ? "var(--ink)" : "var(--mut)",
                          background: destId === d.id ? "var(--yellow)" : "transparent",
                        }}
                      >
                        {d.code} · {d.name}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>

            <div className="flex flex-wrap items-center gap-3 mt-4 pt-4 border-t" style={{ borderColor: "var(--bd)" }}>
              <button
                onClick={startNav}
                disabled={!destId}
                className="display px-6 py-2.5 text-lg tracking-wide transition-all duration-200 cursor-pointer hover:-translate-y-0.5 disabled:opacity-40"
                style={{ background: "var(--yellow)", color: "var(--ink)" }}
              >
                {nav ? "Re-route" : "Start route"} →
              </button>
              <button
                onClick={resetNav}
                className="kicker px-4 py-2.5 border transition-colors duration-200 cursor-pointer"
                style={{ borderColor: "var(--bd)", color: "var(--mut)" }}
              >
                reset
              </button>
              <label className="flex items-center gap-2.5 ml-auto kicker" style={{ color: "var(--mut)" }}>
                walk {s.speed.toFixed(1)} m/s
                <input
                  type="range"
                  min={0.6}
                  max={2.2}
                  step={0.1}
                  value={s.speed}
                  onChange={(e) => {
                    s.speed = parseFloat(e.target.value);
                    force();
                  }}
                  className="w-24 accent-[var(--yellow)]"
                  aria-label="Walking speed"
                />
              </label>
            </div>

            <div className="flex flex-wrap gap-2 mt-3.5">
              <Toggle on={enabled} label={enabled ? "narration on" : "narration off"} onClick={() => setEnabled(!enabled)} hint="Read guidance steps aloud (Web Speech)" />
              <span className="w-px self-stretch" style={{ background: "var(--bd)" }} />
              <Toggle on={s.interference} label="RF interference" onClick={() => { s.interference = !s.interference; force(); }} hint="Simulate multipath — shrinks usable beacon range" />
              <Toggle on={s.corrections} label="beacon corrections" onClick={() => { s.corrections = !s.corrections; force(); }} hint="Toggle the trilateration fix — watch PDR drift" />
            </div>
          </div>

          {/* privacy strip */}
          <div className="grid sm:grid-cols-3 border" style={{ borderColor: "var(--bd)", background: "var(--ink2)" }}>
            {PRIVACY_POINTS.map((p, i) => (
              <div
                key={p.u}
                className="p-4 group transition-colors duration-300 hover:bg-[var(--ink3)]"
                style={{ borderLeft: i === 0 ? "none" : "1px solid var(--bd)" }}
              >
                <div className="flex items-baseline gap-2">
                  <span className="display text-3xl" style={{ color: "var(--yellow)" }}>
                    {p.k}
                  </span>
                  <span className="kicker" style={{ color: "var(--teal)" }}>
                    {p.u}
                  </span>
                </div>
                <p className="text-[0.8rem] leading-relaxed mt-2" style={{ color: "var(--mut)" }}>
                  {p.d}
                </p>
              </div>
            ))}
          </div>
        </div>

        {/* ---------------- right: telemetry ---------------- */}
        <div className="lg:col-span-5 flex flex-col gap-5">
          {/* status banner */}
          <div
            className="border px-4 py-4"
            style={{
              borderColor: nav?.phase === "stairs" ? "var(--yellow)" : "var(--bd)",
              background: nav?.phase === "stairs" ? "var(--yellow)" : nav?.phase === "done" ? "var(--teal)" : "var(--ink2)",
              color: nav?.phase === "stairs" || nav?.phase === "done" ? "var(--ink)" : "var(--tx)",
            }}
          >
            <p className="kicker opacity-70">guidance status</p>
            {!nav && <p className="display text-2xl mt-1.5">Standby</p>}
            {!nav && (
              <p className="text-[0.82rem] mt-1 opacity-80">Choose a destination and start the route — or tap the plan to place yourself.</p>
            )}
            {nav && nav.phase === "walk" && (
              <>
                <p className="display text-2xl mt-1.5">Guiding → {nav.dest.name}</p>
                <p className="kicker mt-1.5 opacity-80">
                  {Math.max(0, totalLen - globalProgress).toFixed(0)} m remaining · level {nav.legs[nav.legIdx].level}
                </p>
              </>
            )}
            {nav && nav.phase === "stairs" && (
              <>
                <p className="display text-2xl mt-1.5 flex items-center gap-3">
                  <Stairs className="w-6 h-6" /> Stairs {nav.legs[nav.legIdx].level === 1 ? "S1" : "S2"} — go to level {nextLeg?.level}
                </p>
                <button
                  onClick={continueStairs}
                  className="display mt-3 px-5 py-2 text-base transition-transform duration-200 hover:-translate-y-0.5 cursor-pointer"
                  style={{ background: "var(--ink)", color: "var(--yellow)" }}
                >
                  I'm on level {nextLeg?.level} →
                </button>
              </>
            )}
            {nav && nav.phase === "done" && (
              <>
                <p className="display text-2xl mt-1.5 flex items-center gap-3">
                  <Flag className="w-6 h-6" /> Arrived — {nav.dest.name}
                </p>
                <p className="kicker mt-1.5 opacity-80">{nav.dest.code} · {nav.dest.desc}</p>
              </>
            )}
            {nav && nav.phase !== "stairs" && (
              <div className="mt-3 h-1.5 w-full" style={{ background: "rgba(0,0,0,0.25)" }}>
                <div
                  className="h-full transition-[width] duration-150"
                  style={{ width: `${totalLen > 0 ? (globalProgress / totalLen) * 100 : 0}%`, background: nav.phase === "done" ? "var(--ink)" : "var(--yellow)" }}
                />
              </div>
            )}
          </div>

          {/* guidance list */}
          <Card title="turn-by-turn · audio described" icon={<Speaker className="w-4 h-4" />} className="flex-1">
            {flat.length === 0 ? (
              <p className="text-[0.82rem]" style={{ color: "var(--mut)" }}>
                No active route. Steps will appear here with distance, turn direction and floor changes — each one readable aloud.
              </p>
            ) : (
              <ol className="max-h-64 overflow-y-auto code-scroll -mr-1 pr-1">
                {flat.map((st, i) => {
                  const state = i < activeIdx ? "done" : i === activeIdx ? "now" : "todo";
                  return (
                    <li
                      key={`${st.legIdx}-${i}`}
                      className="flex items-center gap-3 px-3 py-2.5 border-b transition-colors duration-300"
                      style={{
                        borderColor: "var(--bd)",
                        background: state === "now" ? "var(--ink3)" : "transparent",
                        borderLeft: state === "now" ? "3px solid var(--yellow)" : "3px solid transparent",
                        opacity: state === "done" ? 0.45 : 1,
                      }}
                    >
                      <span style={{ color: state === "now" ? "var(--yellow)" : "var(--mut)" }}>
                        <StepGlyph step={st} className="w-4.5 h-4.5" />
                      </span>
                      <span className="text-[0.84rem] font-medium flex-1">{st.text}</span>
                      {st.legIdx > 0 && i === flat.findIndex((f) => f.legIdx === st.legIdx) && (
                        <span className="kicker" style={{ color: "var(--teal)" }}>
                          L{st.legIdx === 1 ? 2 : 1}
                        </span>
                      )}
                      <button
                        onClick={() => speak(st.text, true)}
                        aria-label={`Read aloud: ${st.text}`}
                        className="p-1 transition-colors duration-200 cursor-pointer hover:text-[var(--yellow)]"
                        style={{ color: "var(--mut)" }}
                      >
                        <Play className="w-3.5 h-3.5" />
                      </button>
                    </li>
                  );
                })}
              </ol>
            )}
            <div className="flex items-center gap-2 mt-3">
              <button
                onClick={() => setEnabled(!enabled)}
                className="flex items-center gap-2 kicker px-3 py-1.5 border transition-colors duration-200 cursor-pointer"
                style={{ borderColor: enabled ? "var(--teal)" : "var(--bd)", color: enabled ? "var(--teal)" : "var(--mut)" }}
              >
                {enabled ? <Speaker className="w-3.5 h-3.5" /> : <SpeakerOff className="w-3.5 h-3.5" />}
                auto-narrate steps {enabled ? "on" : "off"}
              </button>
            </div>
          </Card>

          <div className="grid grid-cols-2 gap-5">
            {/* motion */}
            <Card title="inertial · 50 Hz" icon={<Pulse className="w-4 h-4" />}>
              <div className="flex items-baseline gap-1.5">
                <span className="font-mono text-2xl font-semibold" style={{ color: "var(--tx)" }}>
                  {s.accelMag.toFixed(2)}
                </span>
                <span className="kicker" style={{ color: "var(--mut)" }}>
                  m/s² |a|
                </span>
              </div>
              <div className="mt-1.5">
                <Spark data={s.accelArr} max={13} color="var(--blue)" />
              </div>
              <dl className="grid grid-cols-2 gap-x-3 gap-y-1 mt-2 font-mono text-[0.7rem]" style={{ color: "var(--mut)" }}>
                <div className="flex justify-between">
                  <dt>ax</dt>
                  <dd>{s.ax.toFixed(2)}</dd>
                </div>
                <div className="flex justify-between">
                  <dt>ay</dt>
                  <dd>{s.ay.toFixed(2)}</dd>
                </div>
                <div className="flex justify-between">
                  <dt>az</dt>
                  <dd>{s.az.toFixed(2)}</dd>
                </div>
                <div className="flex justify-between">
                  <dt>yaw°/s</dt>
                  <dd>{s.yawRate.toFixed(0)}</dd>
                </div>
              </dl>
              <p className="kicker mt-2.5 pt-2 border-t" style={{ borderColor: "var(--bd)", color: "var(--teal)" }}>
                {s.steps} steps counted
              </p>
            </Card>

            {/* ranging */}
            <Card title="BLE ranging" icon={<BeaconIcon className="w-4 h-4" />}>
              <ul className="space-y-2">
                {topRanges.map((r) => {
                  const lvl = clamp(Math.round((r.rssi + 92) / 10.5), 0, 4);
                  return (
                    <li key={r.id} className="flex items-center gap-2.5 font-mono text-[0.72rem]">
                      <span style={{ color: r.inRange ? "var(--teal)" : "var(--mut)" }}>{r.id}</span>
                      <span className="flex-1 text-right" style={{ color: "var(--tx)" }}>
                        {r.d.toFixed(1)} m
                      </span>
                      <span className="flex items-end gap-0.5 h-3.5" aria-hidden>
                        {[0, 1, 2, 3].map((k) => (
                          <span
                            key={k}
                            className="w-1"
                            style={{
                              height: `${28 + k * 24}%`,
                              background: k < lvl ? "var(--teal)" : "var(--ink3)",
                              opacity: r.inRange ? 1 : 0.4,
                            }}
                          />
                        ))}
                      </span>
                    </li>
                  );
                })}
              </ul>
              <p className="kicker mt-2.5 pt-2 border-t" style={{ borderColor: "var(--bd)", color: s.beaconsInView >= 3 ? "var(--teal)" : "var(--coral)" }}>
                {s.beaconsInView} in view · need ≥ 3 for a fix
              </p>
            </Card>
          </div>

          {/* fusion */}
          <Card title="fused position error" icon={<Wave className="w-4 h-4" />}>
            <div className="flex items-center gap-4">
              <span
                className="font-mono font-semibold leading-none"
                style={{ fontSize: "2.6rem", color: pass ? "var(--teal)" : "var(--coral)" }}
              >
                {s.error.toFixed(2)}
              </span>
              <div>
                <p className="kicker" style={{ color: "var(--mut)" }}>
                  metres · target &lt; 1.00
                </p>
                <p
                  className="display text-sm mt-1 px-2 py-0.5 inline-block"
                  style={{ background: pass ? "var(--teal)" : "var(--coral)", color: "var(--ink)" }}
                >
                  {pass ? "PASS" : "OUT OF SPEC"}
                </p>
              </div>
              <p className="kicker ml-auto text-right max-w-[8.5rem]" style={{ color: "var(--mut)" }}>
                {!s.corrections ? "PDR only — drifting" : s.interference ? "multipath — WLS degraded" : "WLS + particle rail"}
              </p>
            </div>
            <div className="mt-3">
              <DualSpark a={s.rawDriftArr} b={s.fusedDriftArr} max={driftMax} />
            </div>
            <div className="flex items-center gap-5 mt-2 kicker" style={{ color: "var(--mut)" }}>
              <span className="flex items-center gap-2">
                <span className="w-4 border-t-2" style={{ borderColor: "var(--coral)" }} /> raw PDR drift
              </span>
              <span className="flex items-center gap-2">
                <span className="w-4 border-t-2" style={{ borderColor: "var(--teal)" }} /> fused error
              </span>
            </div>
          </Card>
        </div>
      </div>
    </section>
  );
}
