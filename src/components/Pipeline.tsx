import { STAGES } from "../lib/data";
import { useReveal } from "../lib/hooks";
import { Beacon, Eye, Pulse, Wave } from "./icons";

const ICONS = {
  imu: Pulse,
  ble: Beacon,
  fuse: Wave,
  overlay: Eye,
} as const;

function ChainDiagram() {
  const box = (x: number, y: number, label: string, sub: string, accent = false) => (
    <g key={label}>
      <rect x={x} y={y} width={72} height={36} fill={accent ? "var(--yellow)" : "var(--paper2)"} stroke="var(--line-l)" />
      <text x={x + 36} y={y + 16} textAnchor="middle" fontFamily="var(--font-display)" fontWeight={600} fontSize={11} fill={accent ? "var(--ink)" : "var(--ink)"} letterSpacing="0.05em">
        {label}
      </text>
      <text x={x + 36} y={y + 28} textAnchor="middle" fontFamily="var(--font-mono)" fontSize={7.5} fill="var(--mut-l)">
        {sub}
      </text>
    </g>
  );
  const line = (d: string, key: string) => (
    <path key={key} d={d} fill="none" stroke="var(--mut-l)" strokeWidth={1.4} strokeDasharray="5 4" className="flowdash" />
  );
  return (
    <svg viewBox="0 0 364 132" className="w-full max-w-md" role="img" aria-label="Pipeline diagram: IMU to PDR to fusion, BLE into fusion, fusion to AR overlay">
      {line("M82 38 L100 38", "l1")}
      {line("M172 38 L190 38", "l2")}
      {line("M262 38 L282 38", "l3")}
      {line("M172 96 L190 96 L190 74 L206 74 L206 56", "l4")}
      <path d="M276 34 l6 4 -6 4" fill="none" stroke="var(--mut-l)" strokeWidth={1.4} />
      <path d="M278 92 l6 4 -6 4" fill="none" stroke="var(--mut-l)" strokeWidth={1.4} transform="rotate(90 281 96)" />
      {box(10, 20, "IMU", "50 Hz motion")}
      {box(100, 20, "PDR", "steps + yaw")}
      {box(190, 20, "FUSE", "particle filter", true)}
      {box(282, 20, "OVERLAY", "vector AR")}
      {box(100, 78, "BLE 5.1", "beacon fixes")}
    </svg>
  );
}

function StageCard({ index, stage }: { index: number; stage: (typeof STAGES)[number] }) {
  const { ref, on } = useReveal<HTMLDivElement>(0.15);
  const Icon = ICONS[stage.icon];
  return (
    <article
      ref={ref}
      className={`rv ${on ? "on" : ""} border p-6 sm:p-7 transition-all duration-300 hover:-translate-y-1`}
      style={{ borderColor: "var(--line-l)", background: "var(--paper2)", transitionDelay: `${index * 60}ms` }}
    >
      <div className="flex items-start justify-between gap-4">
        <p className="kicker" style={{ color: "var(--mut-l)" }}>
          {stage.tag}
        </p>
        <span className="display text-4xl leading-none" style={{ color: "var(--ink)", opacity: 0.16 }}>
          {String(index + 1).padStart(2, "0")}
        </span>
      </div>
      <div className="flex items-center gap-3 mt-3">
        <span className="grid place-items-center w-10 h-10 border" style={{ borderColor: "var(--line-l)", color: "var(--ink)", background: "var(--paper)" }}>
          <Icon className="w-5 h-5" />
        </span>
        <h3 className="display text-2xl" style={{ color: "var(--ink)" }}>
          {stage.title}
        </h3>
      </div>
      <p className="mt-3.5 text-[0.92rem] leading-relaxed" style={{ color: "var(--mut-l)" }}>
        {stage.body}
      </p>
      <p className="kicker mt-4 pt-3 border-t" style={{ borderColor: "var(--line-l)", color: "var(--mut-l)" }}>
        {stage.io}
      </p>
      <p
        className="mt-3 font-mono text-[0.82rem] px-3.5 py-2.5 overflow-x-auto code-scroll whitespace-nowrap"
        style={{ background: "var(--ink)", color: "var(--yellow)" }}
      >
        {stage.formula}
      </p>
    </article>
  );
}

export default function Pipeline() {
  const { ref, on } = useReveal<HTMLDivElement>(0.1);
  return (
    <section id="stack" className="scroll-mt-24 grid-light" style={{ background: "var(--paper)", color: "var(--ink)" }}>
      <div className="mx-auto max-w-7xl px-4 sm:px-6 py-20 sm:py-24">
        <div className="grid lg:grid-cols-[minmax(0,26rem)_1fr] gap-10 lg:gap-16 items-start">
          {/* sticky rail */}
          <div ref={ref} className="lg:sticky lg:top-24">
            <p className={`kicker rv ${on ? "on" : ""}`} style={{ color: "var(--mut-l)" }}>
              positioning stack · no visual SLAM
            </p>
            <h2 className="display mt-3 leading-[0.98]" style={{ fontSize: "clamp(2rem, 4.2vw, 3.2rem)", color: "var(--ink)" }}>
              <span className="line-mask">
                <span>How you're located</span>
              </span>
              <span className="line-mask">
                <span style={{ color: "var(--mut-l)" }}>when nothing is seen</span>
              </span>
            </h2>
            <p className={`mt-5 text-[0.95rem] leading-relaxed rv ${on ? "on" : ""}`} style={{ color: "var(--mut-l)" }}>
              Visual SLAM needs a camera — and a camera in a city hall is a privacy liability. The stack replaces
              pixels with physics: inertial dead reckoning supplies motion, beacon trilateration supplies absolute
              fixes, and the preloaded floor plan supplies the rails that bind them together.
            </p>
            <div className={`mt-7 rv ${on ? "on" : ""}`}>
              <ChainDiagram />
            </div>
            <div className={`mt-6 border-l-4 pl-4 rv ${on ? "on" : ""}`} style={{ borderColor: "var(--yellow)" }}>
              <p className="font-mono text-[0.85rem]" style={{ color: "var(--ink)" }}>
                σ²<sub>fused</sub> ≈ ( σ⁻²<sub>pdr</sub> + σ⁻²<sub>ble</sub> )⁻¹
              </p>
              <p className="kicker mt-2" style={{ color: "var(--mut-l)" }}>
                independent errors fuse to something smaller than either
              </p>
            </div>
          </div>

          {/* stage cards */}
          <div className="flex flex-col gap-6">
            {STAGES.map((st, i) => (
              <StageCard key={st.tag} index={i} stage={st} />
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
