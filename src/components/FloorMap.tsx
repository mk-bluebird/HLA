import { useRef } from "react";
import type { Destination, FloorData } from "../lib/data";
import { clamp, dist } from "../lib/geo";
import type { Pt } from "../lib/geo";

interface Props {
  floor: FloorData;
  user: { x: number; y: number; headingDeg: number; error: number };
  routePts: Pt[] | null;
  destination: Destination | null;
  walking: boolean;
  reduced: boolean;
  stairsHot: boolean;
  onPick: (p: Pt) => void;
}

const VW = 42.4;
const VH = 26.4;

export default function FloorMap({ floor, user, routePts, destination, walking, reduced, stairsHot, onPick }: Props) {
  const svgRef = useRef<SVGSVGElement | null>(null);

  const handleClick = (e: React.MouseEvent) => {
    const svg = svgRef.current;
    if (!svg) return;
    const r = svg.getBoundingClientRect();
    const x = ((e.clientX - r.left) / r.width) * VW - 1.2;
    const y = ((e.clientY - r.top) / r.height) * VH - 1.2;
    onPick({ x, y });
  };

  const accR = clamp(user.error * 1.6, 0.55, 4.2);

  return (
    <div className="relative">
      <svg
        ref={svgRef}
        viewBox={`-1.2 -1.2 ${VW} ${VH}`}
        className="w-full h-auto block cursor-crosshair select-none"
        onClick={handleClick}
        role="img"
        aria-label={`Floor plan, level ${floor.level}. Tap to set your position on a corridor.`}
      >
        {/* corridors */}
        {floor.walk.map((seg, i) => (
          <polyline
            key={`walk-${i}`}
            points={seg.map((p) => `${p.x},${p.y}`).join(" ")}
            fill="none"
            stroke="var(--corridor)"
            strokeWidth={2.5}
            strokeLinecap="butt"
          />
        ))}

        {/* outer wall */}
        <rect x={0.75} y={0.75} width={38.5} height={22.5} fill="none" stroke="var(--bd)" strokeWidth={0.22} />

        {/* entrance gap */}
        <line x1={18.7} y1={23.25} x2={21.3} y2={23.25} stroke="var(--corridor)" strokeWidth={0.6} />
        <line x1={18.7} y1={23.0} x2={18.7} y2={23.5} stroke="var(--yellow)" strokeWidth={0.14} />
        <line x1={21.3} y1={23.0} x2={21.3} y2={23.5} stroke="var(--yellow)" strokeWidth={0.14} />
        <text x={20} y={24.35} textAnchor="middle" className="room-name" fontSize={0.52}>
          MAIN ENTRY
        </text>

        {/* rooms */}
        {floor.rooms.map((rm) => {
          const isStairs = rm.id.startsWith("st");
          const hot = isStairs && stairsHot;
          const cy = rm.labelY ?? rm.y + rm.h / 2 - 0.1;
          return (
            <g key={rm.id} className="room-g">
              <rect
                className="room-box"
                x={rm.x}
                y={rm.y}
                width={rm.w}
                height={rm.h}
                fill="var(--room)"
                stroke={hot ? "var(--yellow)" : "var(--line)"}
                strokeWidth={hot ? 0.14 : 0.07}
              />
              {isStairs &&
                [0, 1, 2, 3, 4].map((k) => (
                  <line
                    key={k}
                    x1={rm.x + 0.5}
                    x2={rm.x + rm.w - 0.5}
                    y1={rm.y + 0.7 + k * 0.75}
                    y2={rm.y + 0.7 + k * 0.75}
                    stroke="var(--line)"
                    strokeWidth={0.07}
                  />
                ))}
              <text x={rm.x + rm.w / 2} y={cy} textAnchor="middle" className="room-code" fontSize={0.88}>
                {rm.code}
              </text>
              <text x={rm.x + rm.w / 2} y={cy + 0.8} textAnchor="middle" className="room-name" fontSize={0.44}>
                {rm.name}
              </text>
            </g>
          );
        })}

        {/* route */}
        {routePts && routePts.length > 1 && (
          <>
            <polyline
              points={routePts.map((p) => `${p.x},${p.y}`).join(" ")}
              fill="none"
              stroke="var(--yellow)"
              strokeOpacity={0.14}
              strokeWidth={1.25}
              strokeLinejoin="round"
              strokeLinecap="round"
            />
            <polyline
              className="march"
              points={routePts.map((p) => `${p.x},${p.y}`).join(" ")}
              fill="none"
              stroke="var(--yellow)"
              strokeWidth={0.3}
              strokeLinejoin="round"
              strokeLinecap="round"
              strokeDasharray="1.1 0.75"
            />
          </>
        )}

        {/* destination pin */}
        {destination && (
          <g transform={`translate(${destination.door.x} ${destination.door.y})`}>
            {!reduced && <circle r={0.55} fill="none" stroke="var(--yellow)" strokeWidth={0.1} className="bpulse" />}
            <line x1={0} y1={0} x2={0} y2={-1.35} stroke="var(--yellow)" strokeWidth={0.12} />
            <path d="M0 -1.35 L1.15 -0.98 L0 -0.6 Z" fill="var(--yellow)" />
            <circle r={0.2} fill="var(--yellow)" />
          </g>
        )}

        {/* beacons */}
        {floor.beacons.map((b) => {
          const d = dist(b, user);
          const inRange = d < 12;
          return (
            <g key={b.id} transform={`translate(${b.x} ${b.y})`}>
              {inRange && !reduced && (
                <circle r={0.45} fill="none" stroke="var(--teal)" strokeWidth={0.08} className="bpulse" />
              )}
              <rect
                x={-0.22}
                y={-0.22}
                width={0.44}
                height={0.44}
                transform="rotate(45)"
                fill={inRange ? "var(--teal)" : "var(--ink3)"}
                stroke={inRange ? "var(--teal)" : "var(--mut)"}
                strokeWidth={0.06}
                opacity={inRange ? 1 : 0.55}
              />
              {inRange && (
                <text y={0.85} textAnchor="middle" fontFamily="var(--font-mono)" fontSize={0.36} fill="var(--teal)">
                  {b.id}
                </text>
              )}
            </g>
          );
        })}

        {/* user */}
        <g transform={`translate(${user.x} ${user.y})`}>
          <circle r={accR} fill="var(--teal)" fillOpacity={0.09} stroke="var(--teal)" strokeOpacity={0.55} strokeWidth={0.07} strokeDasharray="0.4 0.25" />
          {walking && !reduced && (
            <g>
              <animateTransform attributeName="transform" type="rotate" from="0" to="360" dur="3.4s" repeatCount="indefinite" />
              <path d="M0 0 L2.3 0 A2.3 2.3 0 0 1 1.76 1.48 Z" fill="var(--teal)" fillOpacity={0.12} />
              <line x1={0} y1={0} x2={2.3} y2={0} stroke="var(--teal)" strokeWidth={0.08} strokeOpacity={0.7} />
            </g>
          )}
          <g transform={`rotate(${user.headingDeg})`}>
            <path d="M0 -0.85 L0.52 0.5 L0 0.18 L-0.52 0.5 Z" fill="var(--yellow)" stroke="var(--ink)" strokeWidth={0.06} />
          </g>
          <circle r={0.14} fill="var(--yellow)" />
        </g>
      </svg>

      {/* html overlays: compass + scale */}
      <div className="absolute top-2.5 right-3 kicker flex items-center gap-1.5" style={{ color: "var(--mut)" }}>
        <svg viewBox="0 0 24 24" className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7v5l3.5 3.5" strokeLinecap="round" />
          <path d="M12 3.2V6M12 3.2L10.4 5h3.2L12 3.2z" fill="currentColor" stroke="none" />
        </svg>
        grid 1 m · N up
      </div>
      <div className="absolute bottom-2.5 left-3 flex items-center gap-2" style={{ color: "var(--mut)" }}>
        <span className="block h-[3px] w-16 border-x border-b" style={{ borderColor: "var(--mut)" }} />
        <span className="kicker">5 m</span>
      </div>
    </div>
  );
}
