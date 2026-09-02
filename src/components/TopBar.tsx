import { useEffect, useState } from "react";
import { ArrowRight, CameraOff, Contrast } from "./icons";

const NAV = [
  ["#simulator", "SYSTEM"],
  ["#stack", "STACK"],
  ["#accuracy", "ACCURACY"],
  ["#access", "ACCESS"],
  ["#code", "CODE"],
  ["#pilot", "PILOT"],
] as const;

export default function TopBar() {
  const [hc, setHc] = useState(false);
  const [big, setBig] = useState(false);

  useEffect(() => {
    document.documentElement.classList.toggle("hc", hc);
  }, [hc]);

  useEffect(() => {
    document.documentElement.style.fontSize = big ? "112.5%" : "";
  }, [big]);

  return (
    <header className="sticky top-0 z-50 border-b" style={{ background: "var(--ink)", borderColor: "var(--bd)" }}>
      <div className="mx-auto max-w-7xl px-4 sm:px-6 flex items-center gap-4 h-14">
        {/* signage mark */}
        <a href="#simulator" className="flex items-center gap-2.5 shrink-0 group">
          <span
            className="grid place-items-center w-8 h-8 transition-transform duration-300 group-hover:translate-x-0.5"
            style={{ background: "var(--yellow)", color: "var(--ink)" }}
          >
            <ArrowRight className="w-4.5 h-4.5" strokeWidth={2.8} />
          </span>
          <span className="leading-none">
            <span className="display block text-[0.95rem] tracking-wide">Meridian City Hall</span>
            <span className="kicker block mt-0.5" style={{ color: "var(--mut)" }}>
              indoor guidance · beacon/plan
            </span>
          </span>
        </a>

        <nav className="hidden lg:flex items-center gap-1 ml-6" aria-label="Sections">
          {NAV.map(([href, label]) => (
            <a
              key={href}
              href={href}
              className="kicker px-2.5 py-1.5 transition-colors duration-200 hover:text-[var(--ink)]"
              style={{ color: "var(--mut)" }}
              onMouseEnter={(e) => (e.currentTarget.style.background = "var(--yellow)")}
              onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
            >
              {label}
            </a>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          {/* privacy badge */}
          <span
            className="hidden sm:flex items-center gap-2 kicker px-3 py-1.5 border"
            style={{ borderColor: "var(--bd)", color: "var(--teal)" }}
          >
            <CameraOff className="w-4 h-4" />
            0 cameras
            <span className="w-1.5 h-1.5 rounded-full blinkdot" style={{ background: "var(--teal)" }} />
          </span>

          <button
            onClick={() => setBig((v) => !v)}
            aria-pressed={big}
            title="Toggle larger text"
            className="kicker px-3 py-1.5 border transition-colors duration-200 cursor-pointer"
            style={{
              borderColor: big ? "var(--yellow)" : "var(--bd)",
              color: big ? "var(--ink)" : "var(--mut)",
              background: big ? "var(--yellow)" : "transparent",
            }}
          >
            A<span className="text-[1.15em] leading-none">A</span>
          </button>

          <button
            onClick={() => setHc((v) => !v)}
            aria-pressed={hc}
            title="Toggle high-contrast mode"
            className="flex items-center gap-2 kicker px-3 py-1.5 border transition-colors duration-200 cursor-pointer"
            style={{
              borderColor: hc ? "var(--yellow)" : "var(--bd)",
              color: hc ? "var(--ink)" : "var(--mut)",
              background: hc ? "var(--yellow)" : "transparent",
            }}
          >
            <Contrast className="w-4 h-4" />
            <span className="hidden sm:inline">{hc ? "HC on" : "contrast"}</span>
          </button>
        </div>
      </div>
    </header>
  );
}
