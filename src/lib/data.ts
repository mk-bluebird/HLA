import type { Pt } from "./geo";

/* ------------------------------------------------------------------ */
/*  Meridian City Hall — indoor dataset (units: metres, y grows south) */
/* ------------------------------------------------------------------ */

export interface Room {
  id: string;
  code: string;
  name: string;
  x: number;
  y: number;
  w: number;
  h: number;
  labelY?: number;
}

export interface Beacon {
  id: string;
  x: number;
  y: number;
}

export interface Destination {
  id: string;
  name: string;
  code: string;
  floor: 1 | 2;
  door: Pt;
  desc: string;
}

export interface FloorData {
  level: 1 | 2;
  rooms: Room[];
  walk: Pt[][]; // walkable centre-line segments
  beacons: Beacon[];
  dests: Destination[];
}

const walk1: Pt[][] = [
  [{ x: 20, y: 22.4 }, { x: 20, y: 5 }], // main spine
  [{ x: 2, y: 19 }, { x: 38, y: 19 }], // corridor C (south)
  [{ x: 2, y: 12 }, { x: 38, y: 12 }], // corridor B
  [{ x: 2, y: 5 }, { x: 38, y: 5 }], // corridor A (north)
  [{ x: 30, y: 19 }, { x: 30, y: 15.5 }], // stair spur
];

const beacons1: Beacon[] = [
  { id: "B1-01", x: 20, y: 21.4 },
  { id: "B1-02", x: 8, y: 19 },
  { id: "B1-03", x: 20, y: 19 },
  { id: "B1-04", x: 33, y: 19 },
  { id: "B1-05", x: 2, y: 12 },
  { id: "B1-06", x: 14, y: 12 },
  { id: "B1-07", x: 26, y: 12 },
  { id: "B1-08", x: 38, y: 12 },
  { id: "B1-09", x: 2, y: 5 },
  { id: "B1-10", x: 14, y: 5 },
  { id: "B1-11", x: 26, y: 5 },
  { id: "B1-12", x: 38, y: 5 },
];

const rooms1: Room[] = [
  { id: "r101", code: "101", name: "RECORDS ANNEX", x: 1.2, y: 1.2, w: 9.3, h: 2.6 },
  { id: "r102", code: "102", name: "IT SERVICES", x: 11.7, y: 1.2, w: 7.1, h: 2.6 },
  { id: "r103", code: "103", name: "COUNCIL CHAMBER", x: 21.2, y: 1.2, w: 17.6, h: 2.6 },
  { id: "r104", code: "104", name: "PERMITS & LICENSING", x: 1.2, y: 6.2, w: 9.3, h: 4.6 },
  { id: "r105", code: "105", name: "TAX & BILLING", x: 21.2, y: 6.2, w: 9.6, h: 4.6 },
  { id: "r106", code: "106", name: "RESTROOMS", x: 32, y: 6.2, w: 6.8, h: 4.6 },
  { id: "r107", code: "107", name: "REGISTRAR", x: 1.2, y: 13.2, w: 9.3, h: 4.6 },
  { id: "r108", code: "108", name: "COMMUNITY ROOM", x: 11.7, y: 13.2, w: 7.1, h: 4.6 },
  { id: "st1", code: "S1", name: "STAIRS", x: 27.4, y: 13.2, w: 5.2, h: 4.6, labelY: 15 },
  { id: "r109", code: "109", name: "LOBBY CAFÉ", x: 33.8, y: 13.2, w: 5, h: 4.6 },
  { id: "lob", code: "—", name: "ENTRANCE LOBBY", x: 1.2, y: 20.2, w: 37.6, h: 2.1, labelY: 21.1 },
];

const dests1: Destination[] = [
  { id: "d-permits", name: "Permits & Licensing", code: "RM 104", floor: 1, door: { x: 6, y: 11.4 }, desc: "Building permits, business licences, inspections." },
  { id: "d-council", name: "Council Chamber", code: "RM 103", floor: 1, door: { x: 26, y: 4.4 }, desc: "Public sessions every Tuesday, 18:00." },
  { id: "d-records", name: "Records Annex", code: "RM 101", floor: 1, door: { x: 6, y: 4.4 }, desc: "Property deeds, archived filings since 1911." },
  { id: "d-registrar", name: "Registrar", code: "RM 107", floor: 1, door: { x: 6, y: 18.4 }, desc: "Vital records, marriage licences, IDs." },
  { id: "d-restroom", name: "Restrooms", code: "RM 106", floor: 1, door: { x: 34.5, y: 11.4 }, desc: "Accessible stalls, changing table." },
];

const rooms2: Room[] = [
  { id: "r201", code: "201", name: "PLANNING DEPT", x: 1.2, y: 1.2, w: 9.3, h: 2.6 },
  { id: "r202", code: "202", name: "PRESS ROOM", x: 11.7, y: 1.2, w: 7.1, h: 2.6 },
  { id: "r210", code: "210", name: "MAYOR'S OFFICE", x: 21.2, y: 1.2, w: 9.6, h: 2.6 },
  { id: "r211", code: "211", name: "CONFERENCE 2B", x: 32, y: 1.2, w: 6.8, h: 2.6 },
  { id: "r203", code: "203", name: "HEARING ROOM 2", x: 1.2, y: 6.2, w: 9.3, h: 4.6 },
  { id: "r204", code: "204", name: "CITY CLERK", x: 21.2, y: 6.2, w: 9.6, h: 4.6 },
  { id: "r205", code: "205", name: "PUBLIC GALLERY", x: 11.7, y: 6.2, w: 7.1, h: 4.6 },
  { id: "st2", code: "S2", name: "STAIRS", x: 27.4, y: 13.2, w: 5.2, h: 4.6, labelY: 15 },
  { id: "r206", code: "206", name: "OPEN WORKSPACE", x: 1.2, y: 13.2, w: 25, h: 4.6 },
  { id: "r207", code: "207", name: "MEZZANINE ARCHIVE", x: 33.8, y: 13.2, w: 5, h: 4.6 },
];

const beacons2: Beacon[] = beacons1.map((b) => ({ ...b, id: b.id.replace("B1", "B2") }));

const dests2: Destination[] = [
  { id: "d-mayor", name: "Mayor's Office", code: "RM 210", floor: 2, door: { x: 26, y: 4.4 }, desc: "Constituent services, appointment desk." },
  { id: "d-planning", name: "Planning Dept", code: "RM 201", floor: 2, door: { x: 6, y: 4.4 }, desc: "Zoning maps, development applications." },
  { id: "d-clerk", name: "City Clerk", code: "RM 204", floor: 2, door: { x: 26, y: 11.4 }, desc: "Agendas, minutes, public records requests." },
  { id: "d-gallery", name: "Public Gallery", code: "RM 205", floor: 2, door: { x: 16, y: 11.4 }, desc: "Overlooks the council chamber below." },
];

export const FLOORS: Record<1 | 2, FloorData> = {
  1: { level: 1, rooms: rooms1, walk: walk1, beacons: beacons1, dests: dests1 },
  2: { level: 2, rooms: rooms2, walk: walk1, beacons: beacons2, dests: dests2 },
};

export const ENTRANCE: Pt = { x: 20, y: 22.4 };
export const STAIR_NODE: Pt = { x: 30, y: 15.5 };

/** Full route legs per destination. Leg level matches the floor it is walked on. */
export const ROUTES: Record<string, { level: 1 | 2; pts: Pt[] }[]> = {
  "d-permits": [{ level: 1, pts: [ENTRANCE, { x: 20, y: 12 }, { x: 6, y: 12 }] }],
  "d-council": [{ level: 1, pts: [ENTRANCE, { x: 20, y: 5 }, { x: 26, y: 5 }] }],
  "d-records": [{ level: 1, pts: [ENTRANCE, { x: 20, y: 5 }, { x: 6, y: 5 }] }],
  "d-registrar": [{ level: 1, pts: [ENTRANCE, { x: 20, y: 19 }, { x: 6, y: 19 }] }],
  "d-restroom": [{ level: 1, pts: [ENTRANCE, { x: 20, y: 12 }, { x: 34.5, y: 12 }] }],
  "d-mayor": [
    { level: 1, pts: [ENTRANCE, { x: 20, y: 19 }, { x: 30, y: 19 }, STAIR_NODE] },
    { level: 2, pts: [STAIR_NODE, { x: 30, y: 19 }, { x: 20, y: 19 }, { x: 20, y: 5 }, { x: 26, y: 5 }] },
  ],
  "d-planning": [
    { level: 1, pts: [ENTRANCE, { x: 20, y: 19 }, { x: 30, y: 19 }, STAIR_NODE] },
    { level: 2, pts: [STAIR_NODE, { x: 30, y: 19 }, { x: 20, y: 19 }, { x: 20, y: 5 }, { x: 6, y: 5 }] },
  ],
  "d-clerk": [
    { level: 1, pts: [ENTRANCE, { x: 20, y: 19 }, { x: 30, y: 19 }, STAIR_NODE] },
    { level: 2, pts: [STAIR_NODE, { x: 30, y: 19 }, { x: 20, y: 19 }, { x: 20, y: 12 }, { x: 26, y: 12 }] },
  ],
  "d-gallery": [
    { level: 1, pts: [ENTRANCE, { x: 20, y: 19 }, { x: 30, y: 19 }, STAIR_NODE] },
    { level: 2, pts: [STAIR_NODE, { x: 30, y: 19 }, { x: 20, y: 19 }, { x: 20, y: 12 }, { x: 16, y: 12 }] },
  ],
};

export const ALL_DESTS: Destination[] = [...dests1, ...dests2];

/* ----------------------------- copy ----------------------------- */

export const TICKER_ITEMS = [
  "NO CAMERA ACCESS — EVER",
  "0 IMAGES CAPTURED",
  "0 IMAGES STORED",
  "SENSOR FUSION ON-DEVICE",
  "FLOOR PLANS PRELOADED",
  "12 BLE 5.1 BEACONS / FLOOR",
  "50 HZ INERTIAL TRACKING",
  "WCAG 2.2 AA TARGET",
  "P95 ERROR < 1.00 M",
];

export interface Stage {
  tag: string;
  title: string;
  body: string;
  io: string;
  formula: string;
  icon: "imu" | "ble" | "fuse" | "overlay";
}

export const STAGES: Stage[] = [
  {
    tag: "STAGE 01 — COREMOTION",
    title: "Inertial dead reckoning",
    body: "The device-motion stream (accelerometer + gyro + magnetometer) runs at 50 Hz. A peak detector on vertical acceleration counts steps; fused yaw integrates them into displacement. Pure PDR is free of any RF infrastructure — and drifts roughly 2% of distance walked, so it is never trusted alone for long.",
    io: "IN raw IMU @ 50 Hz → OUT step events, heading, Δposition",
    formula: "Δp = L · ( sin θ,  cos θ )    L ≈ 0.72 m",
    icon: "imu",
  },
  {
    tag: "STAGE 02 — COREBLUETOOTH",
    title: "Beacon trilateration",
    body: "Eddystone-UID beacons mounted 4–6 m apart broadcast calibrated TX power. The phone converts RSSI to range with a log-normal path-loss model, then solves a weighted least-squares fix. Three ranges give a position; a fourth rejects multipath outliers.",
    io: "IN RSSI frames @ ~5 Hz → OUT weighted position fix",
    formula: "p̂ = argmin Σ wᵢ ( ‖p − bᵢ‖ − d̂ᵢ )²,   wᵢ = d̂ᵢ⁻²",
    icon: "ble",
  },
  {
    tag: "STAGE 03 — MAP MATCHING",
    title: "Particle-filter fusion",
    body: "Particles are propagated by the inertial step model and re-weighted by beacon likelihood — then hard-constrained to the preloaded floor plan. Walls are impassable, corridors act as rails, and PDR drift is continuously pulled back onto walkable geometry. This is where the < 1 m guarantee is actually earned.",
    io: "IN PDR + beacon fixes + floor plan → OUT fused pose",
    formula: "σ²_fused ≈ ( σ⁻²_pdr + σ⁻²_ble )⁻¹",
    icon: "fuse",
  },
  {
    tag: "STAGE 04 — RENDER",
    title: "Cameraless AR overlay",
    body: "Instead of anchoring to a camera feed, the overlay is anchored to the plan: a SceneKit / RealityKit scene rendered from the fused pose alone — glowing route ribbons, floating room tags, and arrow fields that rotate with device yaw. Nothing is captured, nothing is uploaded; the scene is the only visual.",
    io: "IN fused pose → OUT rendered guidance layer",
    formula: "render(pose) → overlay    capture(pixels) → ∅",
    icon: "overlay",
  },
];

export const SWIFT_CODE = `// InertialNavigator.swift — no camera, no frames, ever.
// CoreMotion  +  CoreBluetooth  +  preloaded floor plan
import CoreMotion
import CoreBluetooth
import simd

final class InertialNavigator: NSObject {
    private let motion = CMMotionManager()
    private let queue  = OperationQueue()
    private var yaw: Double = 0                    // fused, magnetic-north referenced
    private var pos  = SIMD2<Double>(20.0, 22.4)   // plan coordinates (m)
    private var lastStep = Date.distantPast
    private let stride = 0.72                      // calibrated stride length (m)

    func start() {
        motion.deviceMotionUpdateInterval = 1.0 / 50.0          // 50 Hz IMU
        motion.startDeviceMotionUpdates(
            using: .xMagneticNorthZVertical,                    // yaw without camera
            to: queue
        ) { [weak self] dm, _ in
            guard let self, let dm else { return }
            self.yaw = dm.attitude.yaw
            let a = dm.userAcceleration
            let mag = sqrt(a.x*a.x + a.y*a.y + a.z*a.z)
            // Step: magnitude spike, 300 ms refractory period
            if mag > 1.8, Date().timeIntervalSince(self.lastStep) > 0.3 {
                self.lastStep = Date()
                self.pos += self.stride * SIMD2(sin(self.yaw), cos(self.yaw))
            }
        }
    }

    /// Weighted trilateration — 2 Gauss–Newton iterations from the PDR prior.
    func fuse(_ ranges: [BeaconRange]) {
        guard ranges.count >= 3 else { return }   // need 3+ for a fix
        var p = pos
        for _ in 0..<2 {
            var num = SIMD2<Double>.zero
            var den = 0.0
            for r in ranges {
                let d  = max(simd_distance(p, r.anchor), 0.3)
                let w  = 1.0 / (d * d)            // w_i = d^-2
                num += w * (r.anchor + (p - r.anchor) / d * r.distance - p)
                den += w
            }
            p += num / den
        }
        pos = MapMatcher.snapToCorridor(p)        // particle-filter rail
    }
}`;

export const CSHARP_CODE = `// BeaconPdrFusion.cs — Unity side of the pipeline.
// Sensor bridge feeds fused yaw + steps; no ARCamera, no frames.
using UnityEngine;

public class BeaconPdrFusion : MonoBehaviour
{
    [SerializeField] Transform playerRoot;      // overlay rig, plan-space
    [SerializeField] float stride = 0.72f;      // calibrated stride (m)
    [SerializeField] float snapWeight = 0.35f;  // beacon trust per fix

    Vector2 pos = new Vector2(20f, 22.4f);      // entrance, plan coords
    float yaw;

    void Update()
    {
        // CoreMotion bridge: magnetic-north yaw, 50 Hz
        yaw = SensorBridge.FusedYawRad;

        // Pedestrian dead reckoning on validated steps
        if (StepDetector.ConsumedStep())
            pos += stride * new Vector2(Mathf.Sin(yaw), Mathf.Cos(yaw));

        // BLE5 trilateration: need 3+ ranges for a fix
        var ranges = BeaconScanner.Ranges;
        if (ranges.Count >= 3)
        {
            Vector2 fix = Trilateration.Weighted(ranges);   // w = d^-2
            pos = Vector2.Lerp(pos, fix, snapWeight);       // sigma < 1 m
        }

        pos = MapMatcher.SnapToCorridor(pos);   // plan-constrained rail
        playerRoot.localPosition = new Vector3(pos.x, 0f, pos.y);
        playerRoot.localRotation = Quaternion.Euler(0f, -yaw * Mathf.Rad2Deg, 0f);
    }
}`;

export const BEACON_SPEC: [string, string][] = [
  ["Protocol", "BLE 5.1 · Eddystone-UID + TLM"],
  ["Spacing", "4–6 m along corridors"],
  ["Mount height", "2.4–2.7 m, door-side"],
  ["TX calibration", "measured @ 1 m, per unit"],
  ["Advert interval", "300 ms"],
  ["Ranging model", "log-normal, n = 2.4"],
  ["Power", "coin cell · 5 yr service"],
  ["Density target", "≥ 3 in range everywhere"],
];

export const SURVEY_ITEMS = [
  "Walk every corridor; log RSSI heatmap at 1 m grid",
  "Mount beacons door-side, above sightline obstructions",
  "Calibrate TX power per unit at exactly 1 m",
  "Verify ≥ 3-beacon overlap in elevators, stairs, restrooms",
  "Record metal / glass multipath zones for the filter prior",
  "Re-scan after any wall, shelving or signage changes",
];

export const PILOT_PHASES = [
  { weeks: "WK 1–2", title: "Site survey", body: "RSSI heatmaps, beacon placement plan, floor-plan vectorisation from CAD." },
  { weeks: "WK 3–4", title: "Install", body: "Mount + register 142 beacons across 3 floors; link to plan anchors." },
  { weeks: "WK 5–6", title: "Calibrate", body: "Per-unit TX trim, path-loss exponent fit, particle-filter priors." },
  { weeks: "WK 7–8", title: "Access testing", body: "Blind & low-vision walkthroughs with audio description; iterate cues." },
];

export const PRIVACY_POINTS = [
  { k: "0", u: "CAMERAS", d: "No camera permission is requested. The app cannot see the room — it reads motion and radio only." },
  { k: "0", u: "IMAGES STORED", d: "No frames, no point clouds, no photos. The visual layer is generated from vectors on-device." },
  { k: "100%", u: "ON-DEVICE", d: "Sensor fusion runs locally. Telemetry that leaves the phone is opt-in, aggregate, and anonymous." },
];
