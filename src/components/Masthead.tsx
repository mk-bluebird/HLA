import { useReveal, useScramble } from "../lib/hooks";
import { Beacon, CameraOff, Chip, Compass } from "./icons";

const CHIPS = [
  { icon: CameraOff, k: "0", v: "cameras / images" },
  { icon: Beacon, k: "BLE 5.1", v: "beacon ranging" },
  { icon: Chip, k: "50 Hz", v: "inertial fusion" },
  { icon: Compass, k: "< 1.00 m", v: "p95 position error" },
];

export default function Masthead() {
  const { ref, on } = useReveal<HTMLDivElement>(0.1);
  const line = useScramble("WAYFIND WITHOUT SEEING.", on, 24);

  return (
    <div ref={ref} className="mx-auto max-w-7xl px-4 sm:px-6 pt-12 sm:pt-16 pb-8">
      <div className="grid lg:grid-cols-[1fr_20rem] gap-8 lg:gap-12 items-end">
        <div>
          <p className={`kicker flex items-center gap-3 rv ${on ? "on" : ""}`} style={{ color: "var(--teal)" }}>
            <span className="inline-block w-8 h-px" style={{ background: "var(--teal)" }} />
            system brief — indoor positioning · public buildings
          </p>
          <h1 className="display leading-[0.95] mt-5" style={{ fontSize: "clamp(2.6rem, 7vw, 5.2rem)" }}>
            <span className="line-mask">
              <span>Augmented reality</span>
            </span>
            <span
              className="block font-mono tracking-tight"
              style={{ fontFamily: "var(--font-mono)", fontWeight: 600, color: "var(--yellow)", fontSize: "0.62em", letterSpacing: "-0.01em" }}
              aria-label="Wayfind without seeing."
            >
              {line}
            </span>
          </h1>
          <p className={`mt-6 max-w-xl text-[0.98rem] leading-relaxed rv ${on ? "on" : ""}`} style={{ color: "var(--mut)" }}>
            A guidance layer for city halls and libraries that guides with{" "}
            <strong style={{ color: "var(--tx)" }}>light, not lenses</strong>. Inertial sensors and Bluetooth beacons
            locate you on a preloaded floor plan; the overlay is pure vector — nothing is filmed, nothing is stored,
            and the positioning error stays under one metre.
          </p>
        </div>

        {/* spec chips — right rail */}
        <div className={`grid grid-cols-2 gap-2.5 rv ${on ? "on" : ""}`}>
          {CHIPS.map(({ icon: Icon, k, v }, i) => (
            <div
              key={v}
              className="border p-3.5 transition-all duration-300 hover:-translate-y-1"
              style={{ borderColor: "var(--bd)", background: "var(--ink2)", transitionDelay: `${i * 40}ms` }}
            >
              <Icon className="w-5 h-5 mb-2.5" />
              <div className="display text-xl leading-none" style={{ color: "var(--yellow)" }}>
                {k}
              </div>
              <div className="kicker mt-1.5" style={{ color: "var(--mut)" }}>
                {v}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
