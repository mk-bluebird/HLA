import { useReveal } from "../lib/hooks";
import { Beacon, Contrast, Eye, Hand, Metronome, Speaker } from "./icons";

interface Row {
  icon: React.ReactNode;
  name: string;
  desc: string;
  status: "SHIPPED" | "SPEC";
  try?: string;
}

const ROWS: Row[] = [
  {
    icon: <Speaker className="w-5 h-5" />,
    name: "Audio description",
    desc: "Every guidance step is spoken aloud via the system speech engine — distances, turn side, floor changes and arrival. Narration fires automatically at each decision point, so the phone can stay in a pocket.",
    status: "SHIPPED",
    try: "Enable narration in the simulator, then start a route",
  },
  {
    icon: <Contrast className="w-5 h-5" />,
    name: "High-contrast mode",
    desc: "A WCAG AAA-leaning palette swap: pure black ground, maximum-luminance yellow route, thickened outlines on the plan. One toggle, applied to the entire interface including the map.",
    status: "SHIPPED",
    try: "Flip “contrast” in the top bar",
  },
  {
    icon: <Eye className="w-5 h-5" />,
    name: "Text scaling",
    desc: "Type respects the system large-text setting and adds an in-app AA step. All labels reflow — nothing clips, nothing overlays the route line.",
    status: "SHIPPED",
    try: "Hit “AA” in the top bar",
  },
  {
    icon: <Metronome className="w-5 h-5" />,
    name: "Reduced motion",
    desc: "The radar sweep, beacon pulses, marching route dashes and ticker all honour the OS reduce-motion preference. Guidance itself stays fully functional — only decoration stands still.",
    status: "SHIPPED",
    try: "Set reduce-motion in your OS to see it here",
  },
  {
    icon: <Hand className="w-5 h-5" />,
    name: "Haptic turn cues",
    desc: "A distinct wrist pattern 3 m before each turn — one pulse for left, two for right, a roll for stairs — so audio can stay quiet in a library.",
    status: "SPEC",
  },
  {
    icon: <Beacon className="w-5 h-5" />,
    name: "Sonar progress tone",
    desc: "An optional proximity tone whose pitch rises as the next decision point approaches, borrowed from parking sensors and loved in user testing.",
    status: "SPEC",
  },
];

export default function AccessSection() {
  const { ref, on } = useReveal<HTMLDivElement>(0.08);
  return (
    <section id="access" className="scroll-mt-24">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 py-20 sm:py-24">
        <div className="grid lg:grid-cols-[minmax(0,24rem)_1fr] gap-10 lg:gap-16 items-start">
          <div ref={ref} className="lg:sticky lg:top-24">
            <p className={`kicker rv ${on ? "on" : ""}`} style={{ color: "var(--teal)" }}>
              accessibility · not an afterthought
            </p>
            <h2 className={`display mt-3 leading-[0.98] rv ${on ? "on" : ""}`} style={{ fontSize: "clamp(2rem, 4.2vw, 3.2rem)" }}>
              <span className="line-mask">
                <span>Designed to be</span>
              </span>
              <span className="line-mask">
                <span style={{ color: "var(--yellow)" }}>followed blind</span>
              </span>
            </h2>
            <p className={`mt-5 text-[0.95rem] leading-relaxed rv ${on ? "on" : ""}`} style={{ color: "var(--mut)" }}>
              A wayfinding app that demands eyesight fails the people who need wayfinding most. The four features
              marked <span style={{ color: "var(--teal)" }}>SHIPPED</span> are live in the simulator on this page —
              the two marked <span style={{ color: "var(--mut)" }}>SPEC</span> ship with the native builds.
            </p>
            <p className={`kicker mt-6 border-l-4 pl-4 leading-relaxed rv ${on ? "on" : ""}`} style={{ borderColor: "var(--yellow)", color: "var(--mut)" }}>
              WCAG 2.2 AA target · validated with blind & low-vision participants in pilot weeks 7–8
            </p>
          </div>

          <div className="border-t" style={{ borderColor: "var(--bd)" }}>
            {ROWS.map((row, i) => {
              const shipped = row.status === "SHIPPED";
              return (
                <div
                  key={row.name}
                  className={`rv ${on ? "on" : ""} group grid sm:grid-cols-[3rem_2.6rem_1fr_auto] items-start gap-x-4 gap-y-2 px-3 sm:px-5 py-5 border-b transition-all duration-300 hover:translate-x-1.5`}
                  style={{ borderColor: "var(--bd)", transitionDelay: `${i * 50}ms`, background: "transparent" }}
                  onMouseEnter={(e) => (e.currentTarget.style.background = "var(--ink2)")}
                  onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
                >
                  <span className="font-mono text-sm pt-1" style={{ color: "var(--mut)" }}>
                    {String(i + 1).padStart(2, "0")}
                  </span>
                  <span className="pt-0.5" style={{ color: shipped ? "var(--yellow)" : "var(--mut)" }}>
                    {row.icon}
                  </span>
                  <div>
                    <h3 className="display text-xl" style={{ color: "var(--tx)" }}>
                      {row.name}
                    </h3>
                    <p className="text-[0.86rem] leading-relaxed mt-1.5 max-w-xl" style={{ color: "var(--mut)" }}>
                      {row.desc}
                    </p>
                    {row.try && (
                      <p className="kicker mt-2.5" style={{ color: "var(--teal)" }}>
                        ↖ try it — {row.try}
                      </p>
                    )}
                  </div>
                  <span
                    className="kicker px-2.5 py-1 border justify-self-start sm:justify-self-end"
                    style={{
                      borderColor: shipped ? "var(--teal)" : "var(--bd)",
                      color: shipped ? "var(--teal)" : "var(--mut)",
                    }}
                  >
                    {row.status}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
}
