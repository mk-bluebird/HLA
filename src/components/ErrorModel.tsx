import { useState } from "react";
import { clamp } from "../lib/geo";
import { useReveal } from "../lib/hooks";

interface Live {
  error: number;
  inView: number;
}

/** Predicted fused error from beacon geometry + environment. */
const sigma = (spacing: number, n: number, interf: number) =>
  (0.32 + 0.085 * spacing) * (n / 2.4) * (1 + 0.9 * interf);

const W = 560;
const H = 300;
const PAD = { l: 46, r: 16, t: 18, b: 36 };
const X0 = 3;
const X1 = 8;
const Y1 = 2.2;

const px = (s: number) => PAD.l + ((s - X0) / (X1 - X0)) * (W - PAD.l - PAD.r);
const py = (v: number) => PAD.t + (1 - clamp(v, 0, Y1) / Y1) * (H - PAD.t - PAD.b);

function Slider({
  label,
  value,
  min,
  max,
  step,
  unit,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  step: number;
  unit: string;
  onChange: (v: number) => void;
}) {
  return (
    <label className="block">
      <span className="flex justify-between kicker mb-2" style={{ color: "var(--mut)" }}>
        {label}
        <span className="font-mono text-[0.82rem]" style={{ color: "var(--yellow)" }}>
          {value.toFixed(step < 1 ? (step < 0.1 ? 2 : 1) : 0)} {unit}
        </span>
      </span>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(parseFloat(e.target.value))}
        className="w-full accent-[var(--yellow)] cursor-pointer"
      />
    </label>
  );
}

export default function ErrorModel({ live }: { live: Live | null }) {
  const [spacing, setSpacing] = useState(5);
  const [n, setN] = useState(2.4);
  const [interf, setInterf] = useState(0);
  const { ref, on } = useReveal<HTMLDivElement>(0.1);

  const current = sigma(spacing, n, interf);
  const pass = current < 1;

  const curve = (i: number) => {
    const pts: string[] = [];
    for (let s = X0; s <= X1 + 1e-6; s += 0.2) pts.push(`${px(s).toFixed(1)},${py(sigma(s, n, i)).toFixed(1)}`);
    return pts.join(" ");
  };

  return (
    <section id="accuracy" className="scroll-mt-24 bg-blueprint">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 py-20 sm:py-24">
        <div ref={ref}>
          <p className={`kicker rv ${on ? "on" : ""}`} style={{ color: "var(--teal)" }}>
            the accuracy budget · σ &lt; 1 m
          </p>
          <h2 className={`display mt-3 leading-[0.98] rv ${on ? "on" : ""}`} style={{ fontSize: "clamp(2rem, 4.2vw, 3.2rem)" }}>
            One metre is a <span style={{ color: "var(--yellow)" }}>design constraint,</span>
            <br /> not a hope
          </h2>
          <p className={`mt-4 max-w-2xl text-[0.95rem] leading-relaxed rv ${on ? "on" : ""}`} style={{ color: "var(--mut)" }}>
            The beacon lattice is dimensioned so the weighted least-squares fix stays inside one metre at the 95th
            percentile. Drag the environment and watch the model respond — then compare with the live simulator
            reading above.
          </p>
        </div>

        <div className="grid lg:grid-cols-12 gap-6 mt-10 items-start">
          {/* controls */}
          <div className="lg:col-span-5 flex flex-col gap-6">
            <div className="border p-5 flex flex-col gap-5" style={{ borderColor: "var(--bd)", background: "var(--ink2)" }}>
              <Slider label="beacon spacing" value={spacing} min={3} max={8} step={0.25} unit="m" onChange={setSpacing} />
              <Slider label="path-loss exponent n" value={n} min={2} max={4} step={0.1} unit="" onChange={setN} />
              <Slider label="RF interference" value={interf * 100} min={0} max={100} step={5} unit="%" onChange={(v) => setInterf(v / 100)} />
            </div>

            <div className="border p-5" style={{ borderColor: pass ? "var(--teal)" : "var(--coral)", background: "var(--ink2)" }}>
              <div className="flex items-end gap-4">
                <span className="font-mono font-semibold leading-none" style={{ fontSize: "3.4rem", color: pass ? "var(--teal)" : "var(--coral)" }}>
                  {current.toFixed(2)}
                </span>
                <div className="pb-1.5">
                  <p className="kicker" style={{ color: "var(--mut)" }}>
                    predicted σ_fused · metres
                  </p>
                  <p className="display text-sm mt-1.5 px-2 py-0.5 inline-block" style={{ background: pass ? "var(--teal)" : "var(--coral)", color: "var(--ink)" }}>
                    {pass ? "WITHIN SPEC" : "VIOLATES 1.00 m"}
                  </p>
                </div>
              </div>
              <p className="font-mono text-[0.8rem] mt-4 overflow-x-auto code-scroll whitespace-nowrap" style={{ color: "var(--mut)" }}>
                σ ≈ (0.32 + 0.085·s) · (n / 2.4) · (1 + 0.9·interference)
              </p>
              <p className="kicker mt-3 pt-3 border-t leading-relaxed" style={{ borderColor: "var(--bd)", color: "var(--mut)" }}>
                s = spacing · fit from 6 weeks of site surveys · p95 ≈ 1.7σ
              </p>
            </div>

            <div className="border px-5 py-4 flex items-center gap-4" style={{ borderColor: "var(--bd)", background: "var(--ink2)" }}>
              <span className="w-2 h-2 rounded-full blinkdot shrink-0" style={{ background: "var(--teal)" }} />
              <p className="kicker leading-relaxed" style={{ color: "var(--mut)" }}>
                live from simulator —{" "}
                <span className="font-mono text-[0.95rem]" style={{ color: live && live.error < 1 ? "var(--teal)" : "var(--coral)" }}>
                  {live ? `${live.error.toFixed(2)} m · ${live.inView} beacons` : "starting…"}
                </span>
              </p>
            </div>
          </div>

          {/* chart */}
          <div className="lg:col-span-7 border p-5 sm:p-6" style={{ borderColor: "var(--bd)", background: "var(--ink2)" }}>
            <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 kicker mb-4" style={{ color: "var(--mut)" }}>
              <span className="flex items-center gap-2">
                <span className="w-4 border-t-2" style={{ borderColor: "var(--teal)" }} /> calm RF
              </span>
              <span className="flex items-center gap-2">
                <span className="w-4 border-t-2" style={{ borderColor: "var(--yellow)" }} /> current setting
              </span>
              <span className="flex items-center gap-2">
                <span className="w-4 border-t-2 border-dashed" style={{ borderColor: "var(--coral)" }} /> spec limit 1.00 m
              </span>
            </div>
            <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-auto" role="img" aria-label="Chart of predicted positioning error versus beacon spacing">
              {/* grid */}
              {[0, 0.5, 1, 1.5, 2].map((v) => (
                <g key={v}>
                  <line x1={PAD.l} x2={W - PAD.r} y1={py(v)} y2={py(v)} stroke="var(--line)" strokeWidth="1" />
                  <text x={PAD.l - 8} y={py(v) + 3.5} textAnchor="end" fontFamily="var(--font-mono)" fontSize="10" fill="var(--mut)">
                    {v.toFixed(1)}
                  </text>
                </g>
              ))}
              {[3, 4, 5, 6, 7, 8].map((s) => (
                <text key={s} x={px(s)} y={H - 12} textAnchor="middle" fontFamily="var(--font-mono)" fontSize="10" fill="var(--mut)">
                  {s} m
                </text>
              ))}
              {/* pass zone */}
              <rect x={PAD.l} y={py(1)} width={W - PAD.l - PAD.r} height={py(0) - py(1)} fill="var(--teal)" opacity="0.06" />
              {/* limit */}
              <line x1={PAD.l} x2={W - PAD.r} y1={py(1)} y2={py(1)} stroke="var(--coral)" strokeWidth="1.6" strokeDasharray="6 5" />
              <text x={W - PAD.r} y={py(1) - 6} textAnchor="end" fontFamily="var(--font-mono)" fontSize="10" fill="var(--coral)">
                1.00 m
              </text>
              {/* reference curves @ current n */}
              {[0, 0.5, 1].filter((i) => Math.abs(i - interf) > 0.01).map((i) => (
                <polyline key={i} points={curve(i)} fill="none" stroke="var(--teal)" strokeOpacity={i === 0 ? 0.5 : 0.22} strokeWidth="1.6" />
              ))}
              {/* current curve */}
              <polyline points={curve(interf)} fill="none" stroke="var(--yellow)" strokeWidth="2.6" />
              <circle cx={px(spacing)} cy={py(current)} r="5.5" fill="var(--yellow)" stroke="var(--ink)" strokeWidth="2" />
              {/* axis captions */}
              <text x={(PAD.l + W - PAD.r) / 2} y={H - 0.5} textAnchor="middle" fontFamily="var(--font-mono)" fontSize="9" letterSpacing="2" fill="var(--mut)">
                BEACON SPACING →
              </text>
              <text x={12} y={(PAD.t + H - PAD.b) / 2} textAnchor="middle" fontFamily="var(--font-mono)" fontSize="9" letterSpacing="2" fill="var(--mut)" transform={`rotate(-90 12 ${(PAD.t + H - PAD.b) / 2})`}>
                σ FUSED (m) →
              </text>
            </svg>
            <p className="kicker mt-3 leading-relaxed" style={{ color: "var(--mut)" }}>
              read it backwards: the 1 m line sets the maximum legal spacing for a given environment. at n = 2.4 and
              calm RF, that's ~7.4 m — we install at 4–6 m for headroom.
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
