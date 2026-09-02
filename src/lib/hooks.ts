import { useCallback, useEffect, useRef, useState } from "react";

/** True when the user prefers reduced motion. */
export function useReducedMotion(): boolean {
  const [reduced, setReduced] = useState(
    () => typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches,
  );
  useEffect(() => {
    const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
    const fn = (e: MediaQueryListEvent) => setReduced(e.matches);
    mq.addEventListener("change", fn);
    return () => mq.removeEventListener("change", fn);
  }, []);
  return reduced;
}

/** IntersectionObserver reveal — returns ref and visibility flag. */
export function useReveal<T extends HTMLElement = HTMLDivElement>(threshold = 0.18) {
  const ref = useRef<T | null>(null);
  const [on, setOn] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const obs = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (e.isIntersecting) {
            setOn(true);
            obs.disconnect();
          }
        }
      },
      { threshold },
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, [threshold]);
  return { ref, on };
}

/** Scramble-decode text effect. Resolves left-to-right. */
export function useScramble(text: string, active: boolean, speed = 28) {
  const [out, setOut] = useState(text);
  const reduced = useReducedMotion();
  useEffect(() => {
    if (!active || reduced) {
      setOut(text);
      return;
    }
    const CHARS = "▮▯/\\|<>#%0123456789";
    let frame = 0;
    let raf = 0;
    let last = 0;
    const tick = (t: number) => {
      if (t - last >= speed) {
        last = t;
        frame += 1;
        const resolved = Math.floor(frame / 2);
        let s = "";
        for (let i = 0; i < text.length; i++) {
          if (i < resolved || text[i] === " ") s += text[i];
          else s += CHARS[Math.floor(Math.random() * CHARS.length)];
        }
        setOut(s);
        if (resolved >= text.length) return;
      }
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [text, active, reduced, speed]);
  return out;
}

/** Web Speech API narration with a global enabled flag. */
export function useSpeech() {
  const [enabled, setEnabled] = useState(false);
  const enabledRef = useRef(enabled);
  enabledRef.current = enabled;

  const speak = useCallback((text: string, force = false) => {
    if (!force && !enabledRef.current) return;
    if (!("speechSynthesis" in window)) return;
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.rate = 1.04;
    u.pitch = 1.0;
    window.speechSynthesis.speak(u);
  }, []);

  const stop = useCallback(() => {
    if ("speechSynthesis" in window) window.speechSynthesis.cancel();
  }, []);

  useEffect(() => () => stop(), [stop]);
  return { enabled, setEnabled, speak, stop };
}

/** Count-up number once `start` flips true. */
export function useCountUp(target: number, start: boolean, duration = 1100) {
  const [val, setVal] = useState(0);
  const reduced = useReducedMotion();
  useEffect(() => {
    if (!start) return;
    if (reduced) {
      setVal(target);
      return;
    }
    let raf = 0;
    const t0 = performance.now();
    const tick = (t: number) => {
      const k = Math.min(1, (t - t0) / duration);
      const eased = 1 - Math.pow(1 - k, 3);
      setVal(target * eased);
      if (k < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [target, start, duration, reduced]);
  return val;
}
