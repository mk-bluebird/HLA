import { useReveal } from "../lib/hooks";
import { ArrowRight, CameraOff } from "./icons";

const LINKS = [
  ["#simulator", "Live simulator"],
  ["#stack", "Positioning stack"],
  ["#accuracy", "Accuracy model"],
  ["#access", "Accessibility"],
  ["#code", "Platform notes"],
  ["#pilot", "Pilot plan"],
] as const;

export default function Footer() {
  const { ref, on } = useReveal<HTMLDivElement>(0.15);
  return (
    <footer className="grid-dark" style={{ background: "var(--ink)", borderTop: "1px solid var(--bd)" }}>
      <div className={`mx-auto max-w-7xl px-4 sm:px-6 py-16 sm:py-20 rv ${on ? "on" : ""}`} ref={ref}>
        <div className="grid lg:grid-cols-[1fr_auto] gap-10 items-end">
          <div>
            <p className="display leading-[0.95]" style={{ fontSize: "clamp(2.2rem, 5.5vw, 4rem)" }}>
              <span className="line-mask">
                <span>Follow the light,</span>
              </span>
              <span className="line-mask">
                <span style={{ color: "var(--yellow)" }}>not the lens.</span>
              </span>
            </p>
            <p className={`mt-5 max-w-md text-[0.9rem] leading-relaxed rv ${on ? "on" : ""}`} style={{ color: "var(--mut)" }}>
              Camera-free AR wayfinding for the buildings everyone has to navigate — city halls, libraries, clinics.
              Position from motion and radio; render from vectors; keep the public's privacy absolute.
            </p>
          </div>
          <nav className={`rv ${on ? "on" : ""} grid grid-cols-2 gap-x-10 gap-y-2.5`} aria-label="Footer">
            {LINKS.map(([href, label]) => (
              <a
                key={href}
                href={href}
                className="kicker flex items-center gap-2 py-1 transition-all duration-200 hover:translate-x-1 hover:text-[var(--yellow)]"
                style={{ color: "var(--mut)" }}
              >
                <ArrowRight className="w-3.5 h-3.5" />
                {label}
              </a>
            ))}
          </nav>
        </div>

        <div
          className={`rv ${on ? "on" : ""} mt-12 pt-6 border-t flex flex-col sm:flex-row items-start sm:items-center gap-4`}
          style={{ borderColor: "var(--bd)" }}
        >
          <span className="flex items-center gap-2.5 kicker" style={{ color: "var(--teal)" }}>
            <CameraOff className="w-4.5 h-4.5" />
            this page requests no camera & no permissions — demo telemetry is simulated locally
          </span>
          <span className="kicker sm:ml-auto" style={{ color: "var(--mut)" }}>
            beacon/plan · system brief · swift / coremotion / corebluetooth · unity c#
          </span>
        </div>
      </div>
    </footer>
  );
}
