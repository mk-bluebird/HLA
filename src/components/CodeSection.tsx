import { useState } from "react";
import { CSHARP_CODE, SWIFT_CODE } from "../lib/data";
import { useReveal } from "../lib/hooks";
import { CameraOff, Shield } from "./icons";

const KEYWORDS = new Set([
  "import", "final", "class", "func", "private", "let", "var", "guard", "return", "for", "in", "if",
  "else", "self", "weak", "using", "public", "void", "new", "struct", "enum", "static", "nil", "true",
  "false", "override", "where", "switch", "case", "float", "double", "int",
]);

function findComment(line: string): number {
  let inStr = false;
  for (let i = 0; i < line.length - 1; i++) {
    const c = line[i];
    if (c === '"') inStr = !inStr;
    if (!inStr && c === "/" && line[i + 1] === "/") return i;
  }
  return -1;
}

function Highlight({ code }: { code: string }) {
  return (
    <>
      {code.split("\n").map((line, li) => {
        const ci = findComment(line);
        const codePart = ci >= 0 ? line.slice(0, ci) : line;
        const commentPart = ci >= 0 ? line.slice(ci) : "";
        return (
          <div key={li} className="flex">
            <span className="w-9 shrink-0 text-right pr-4 select-none" style={{ color: "var(--mut)", opacity: 0.45 }}>
              {li + 1}
            </span>
            <span className="flex-1 whitespace-pre">
              {codePart.split(/("[^"]*")/g).map((part, pi) => {
                if (part.startsWith('"') && part.endsWith('"')) {
                  return (
                    <span key={pi} style={{ color: "var(--teal)" }}>
                      {part}
                    </span>
                  );
                }
                return part.split(/\b/).map((w, wi) =>
                  KEYWORDS.has(w) ? (
                    <span key={`${pi}-${wi}`} style={{ color: "var(--yellow)" }}>
                      {w}
                    </span>
                  ) : (
                    <span key={`${pi}-${wi}`}>{w}</span>
                  ),
                );
              })}
              {commentPart && <span style={{ color: "var(--mut)", fontStyle: "italic" }}>{commentPart}</span>}
            </span>
          </div>
        );
      })}
    </>
  );
}

const NOTES = [
  {
    icon: <CameraOff className="w-5 h-5" />,
    title: "Why not ARKit worldTracking?",
    body: "It requires a live camera feed — rejected by the privacy constraint. Instead: CMMotionManager with .xMagneticNorthZVertical gives magnetic-north-referenced yaw with zero pixels involved.",
  },
  {
    icon: <Shield className="w-5 h-5" />,
    title: "Anchors without vision",
    body: "The overlay is anchored to plan coordinates, not real-world features. SceneKit/RealityKit renders the route ribbon and floating room tags from the fused pose alone.",
  },
];

export default function CodeSection() {
  const [tab, setTab] = useState<"swift" | "cs">("swift");
  const { ref, on } = useReveal<HTMLDivElement>(0.08);

  return (
    <section id="code" className="scroll-mt-24" style={{ background: "var(--ink2)", borderTop: "1px solid var(--bd)", borderBottom: "1px solid var(--bd)" }}>
      <div className="mx-auto max-w-7xl px-4 sm:px-6 py-20 sm:py-24">
        <div className="grid lg:grid-cols-[minmax(0,22rem)_1fr] gap-10 lg:gap-14 items-start">
          <div ref={ref} className="lg:sticky lg:top-24">
            <p className={`kicker rv ${on ? "on" : ""}`} style={{ color: "var(--teal)" }}>
              platform notes · native builds
            </p>
            <h2 className={`display mt-3 leading-[0.98] rv ${on ? "on" : ""}`} style={{ fontSize: "clamp(2rem, 4.2vw, 3.2rem)" }}>
              <span className="line-mask">
                <span>Same pipeline,</span>
              </span>
              <span className="line-mask">
                <span style={{ color: "var(--yellow)" }}>two platforms</span>
              </span>
            </h2>
            <div className="mt-6 flex flex-col gap-5">
              {NOTES.map((nt, i) => (
                <div key={nt.title} className={`rv ${on ? "on" : ""} flex gap-3.5`} style={{ transitionDelay: `${i * 70}ms` }}>
                  <span className="shrink-0 mt-0.5" style={{ color: "var(--yellow)" }}>
                    {nt.icon}
                  </span>
                  <div>
                    <h3 className="display text-lg">{nt.title}</h3>
                    <p className="text-[0.86rem] leading-relaxed mt-1" style={{ color: "var(--mut)" }}>
                      {nt.body}
                    </p>
                  </div>
                </div>
              ))}
            </div>
            <p className={`kicker mt-7 leading-relaxed rv ${on ? "on" : ""}`} style={{ color: "var(--mut)" }}>
              both sides share the fusion contract: 50 Hz pose in, plan-space overlay out — the web simulator above
              runs the very same model.
            </p>
          </div>

          <div>
            {/* tabs */}
            <div className="flex items-end gap-1">
              {(
                [
                  ["swift", "Swift — CoreMotion / CoreBluetooth", "InertialNavigator.swift"],
                  ["cs", "C# — Unity", "BeaconPdrFusion.cs"],
                ] as const
              ).map(([key, label, file]) => (
                <button
                  key={key}
                  onClick={() => setTab(key)}
                  className="kicker px-4 py-2.5 border border-b-0 transition-colors duration-200 cursor-pointer"
                  style={{
                    borderColor: "var(--bd)",
                    background: tab === key ? "var(--ink)" : "transparent",
                    color: tab === key ? "var(--yellow)" : "var(--mut)",
                  }}
                  aria-pressed={tab === key}
                >
                  <span className="hidden sm:inline">{label}</span>
                  <span className="sm:hidden">{key === "swift" ? "Swift" : "C#"}</span>
                  <span className="hidden sm:inline opacity-60"> · {file}</span>
                </button>
              ))}
            </div>
            <div className="border" style={{ borderColor: "var(--bd)", background: "var(--ink)" }}>
              <div className="flex items-center gap-2 px-4 py-2.5 border-b" style={{ borderColor: "var(--bd)" }}>
                <span className="w-2.5 h-2.5 rounded-full" style={{ background: "var(--coral)" }} />
                <span className="w-2.5 h-2.5 rounded-full" style={{ background: "var(--yellow)" }} />
                <span className="w-2.5 h-2.5 rounded-full" style={{ background: "var(--teal)" }} />
                <span className="kicker ml-3" style={{ color: "var(--mut)" }}>
                  {tab === "swift" ? "InertialNavigator.swift" : "BeaconPdrFusion.cs"}
                </span>
                <span className="kicker ml-auto" style={{ color: "var(--mut)" }}>
                  {tab === "swift" ? "iOS 17+" : "Unity 2022 LTS"}
                </span>
              </div>
              <pre className="p-4 sm:p-5 overflow-x-auto code-scroll font-mono text-[0.78rem] leading-[1.65]" style={{ color: "var(--tx)" }}>
                <code>{tab === "swift" ? <Highlight code={SWIFT_CODE} /> : <Highlight code={CSHARP_CODE} />}</code>
              </pre>
            </div>
            <p className="kicker mt-3.5 leading-relaxed" style={{ color: "var(--mut)" }}>
              abridged for clarity — production code adds Kalman smoothing, per-device stride calibration and beacon
              outlier rejection (4th range vote).
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
