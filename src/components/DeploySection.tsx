import { BEACON_SPEC, PILOT_PHASES, SURVEY_ITEMS } from "../lib/data";
import { useCountUp, useReveal } from "../lib/hooks";
import { Beacon, Stairs } from "./icons";

function Stat({ value, decimals = 0, suffix, label, started, delay }: { value: number; decimals?: number; suffix?: string; label: string; started: boolean; delay: number }) {
  const v = useCountUp(value, started);
  return (
    <div className="px-5 py-6 sm:py-8 text-center group" style={{ borderLeft: "1px solid var(--line-l)" }}>
      <div className="display leading-none transition-transform duration-300 group-hover:-translate-y-1" style={{ fontSize: "clamp(1.8rem, 3.4vw, 2.8rem)", color: "var(--ink)" }}>
        {v.toFixed(decimals)}
        {suffix && <span style={{ color: "var(--mut-l)" }}>{suffix}</span>}
      </div>
      <p className="kicker mt-2.5" style={{ color: "var(--mut-l)", transitionDelay: `${delay}ms` }}>
        {label}
      </p>
    </div>
  );
}

export default function DeploySection() {
  const { ref, on } = useReveal<HTMLDivElement>(0.08);
  const { ref: statRef, on: statOn } = useReveal<HTMLDivElement>(0.3);

  return (
    <section id="pilot" className="scroll-mt-24 grid-light" style={{ background: "var(--paper)", color: "var(--ink)" }}>
      <div className="mx-auto max-w-7xl px-4 sm:px-6 py-20 sm:py-24">
        <div ref={ref}>
          <p className={`kicker rv ${on ? "on" : ""}`} style={{ color: "var(--mut-l)" }}>
            deployment · meridian city hall pilot
          </p>
          <h2 className={`display mt-3 leading-[0.98] rv ${on ? "on" : ""}`} style={{ fontSize: "clamp(2rem, 4.2vw, 3.2rem)", color: "var(--ink)" }}>
            <span className="line-mask">
              <span>From survey to</span>
            </span>
            <span className="line-mask">
              <span style={{ color: "var(--mut-l)" }}>sub-metre in eight weeks</span>
            </span>
          </h2>
        </div>

        <div className="grid lg:grid-cols-3 gap-6 mt-10 items-start">
          {/* beacon spec */}
          <div className={`rv ${on ? "on" : ""} border p-6`} style={{ borderColor: "var(--line-l)", background: "var(--paper2)" }}>
            <div className="flex items-center gap-3">
              <span className="grid place-items-center w-10 h-10 border" style={{ borderColor: "var(--line-l)", color: "var(--ink)", background: "var(--paper)" }}>
                <Beacon className="w-5 h-5" />
              </span>
              <h3 className="display text-xl" style={{ color: "var(--ink)" }}>
                Beacon specification
              </h3>
            </div>
            <dl className="mt-5">
              {BEACON_SPEC.map(([k, v]) => (
                <div key={k} className="flex justify-between gap-4 py-2.5 border-b last:border-b-0" style={{ borderColor: "var(--line-l)" }}>
                  <dt className="kicker" style={{ color: "var(--mut-l)" }}>
                    {k}
                  </dt>
                  <dd className="font-mono text-[0.78rem] text-right" style={{ color: "var(--ink)" }}>
                    {v}
                  </dd>
                </div>
              ))}
            </dl>
          </div>

          {/* survey checklist */}
          <div className={`rv ${on ? "on" : ""} border p-6`} style={{ borderColor: "var(--line-l)", background: "var(--paper2)", transitionDelay: "80ms" }}>
            <div className="flex items-center gap-3">
              <span className="grid place-items-center w-10 h-10 border" style={{ borderColor: "var(--line-l)", color: "var(--ink)", background: "var(--paper)" }}>
                <svg viewBox="0 0 24 24" className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M4 6l2 2 4-4.5M4 13l2 2 4-4.5M4 20l2 2 4-4.5" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M14 6h7M14 13h7M14 20h7" strokeLinecap="round" />
                </svg>
              </span>
              <h3 className="display text-xl" style={{ color: "var(--ink)" }}>
                Site-survey checklist
              </h3>
            </div>
            <ul className="mt-5 space-y-3.5">
              {SURVEY_ITEMS.map((item) => (
                <li key={item} className="flex gap-3 text-[0.86rem] leading-relaxed" style={{ color: "var(--mut-l)" }}>
                  <svg viewBox="0 0 24 24" className="w-4.5 h-4.5 shrink-0 mt-0.5" fill="none" stroke="var(--ink)" strokeWidth="2.4">
                    <path d="M4 12.5l5 5L20 6.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  {item}
                </li>
              ))}
            </ul>
          </div>

          {/* timeline */}
          <div className={`rv ${on ? "on" : ""} border p-6`} style={{ borderColor: "var(--line-l)", background: "var(--paper2)", transitionDelay: "160ms" }}>
            <div className="flex items-center gap-3">
              <span className="grid place-items-center w-10 h-10 border" style={{ borderColor: "var(--line-l)", color: "var(--ink)", background: "var(--paper)" }}>
                <Stairs className="w-5 h-5" />
              </span>
              <h3 className="display text-xl" style={{ color: "var(--ink)" }}>
                Pilot timeline
              </h3>
            </div>
            <ol className="mt-5 relative" style={{ borderLeft: "2px solid var(--line-l)" }}>
              {PILOT_PHASES.map((ph, i) => (
                <li key={ph.weeks} className="pl-5 pb-6 last:pb-0 relative group">
                  <span
                    className="absolute w-3 h-3 -left-[7px] top-1 transition-transform duration-300 group-hover:scale-125"
                    style={{ background: i === PILOT_PHASES.length - 1 ? "var(--yellow)" : "var(--ink)" }}
                  />
                  <p className="kicker" style={{ color: "var(--mut-l)" }}>
                    {ph.weeks}
                  </p>
                  <h4 className="display text-lg mt-1" style={{ color: "var(--ink)" }}>
                    {ph.title}
                  </h4>
                  <p className="text-[0.84rem] leading-relaxed mt-1" style={{ color: "var(--mut-l)" }}>
                    {ph.body}
                  </p>
                </li>
              ))}
            </ol>
          </div>
        </div>

        {/* stat band */}
        <div ref={statRef} className={`rv ${statOn ? "on" : ""} grid grid-cols-2 sm:grid-cols-5 border mt-10`} style={{ borderColor: "var(--line-l)", background: "var(--paper2)", borderLeft: "none" }}>
          <Stat value={142} label="beacons installed" started={statOn} delay={0} />
          <Stat value={3} label="floors covered" started={statOn} delay={60} />
          <Stat value={0.62} decimals={2} suffix=" m" label="p95 fused error" started={statOn} delay={120} />
          <Stat value={0} label="cameras, ever" started={statOn} delay={180} />
          <Stat value={50} suffix=" Hz" label="inertial update" started={statOn} delay={240} />
        </div>
      </div>
    </section>
  );
}
