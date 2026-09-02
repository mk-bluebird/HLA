interface IconProps {
  className?: string;
  strokeWidth?: number;
}

const base = (className?: string) => className ?? "w-5 h-5";

export const ArrowUp = ({ className, strokeWidth = 2.2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M12 20V5" strokeLinecap="round" />
    <path d="M6 11l6-6 6 6" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const ArrowLeft = ({ className, strokeWidth = 2.2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M20 12H5" strokeLinecap="round" />
    <path d="M11 6l-6 6 6 6" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const ArrowRight = ({ className, strokeWidth = 2.2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M4 12h15" strokeLinecap="round" />
    <path d="M13 6l6 6-6 6" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const ArrowDown = ({ className, strokeWidth = 2.2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M12 4v15" strokeLinecap="round" />
    <path d="M6 13l6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const Stairs = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M3 20h4v-4h4v-4h4V8h4V4h2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const Flag = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M5 21V4" strokeLinecap="round" />
    <path d="M5 4h13l-3 4 3 4H5" strokeLinejoin="round" />
  </svg>
);

export const Beacon = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <circle cx="12" cy="12" r="2.2" fill="currentColor" stroke="none" />
    <path d="M8.5 15.5a5 5 0 0 1 0-7" strokeLinecap="round" />
    <path d="M15.5 8.5a5 5 0 0 1 0 7" strokeLinecap="round" />
    <path d="M5.6 18.4a9 9 0 0 1 0-12.8" strokeLinecap="round" />
    <path d="M18.4 5.6a9 9 0 0 1 0 12.8" strokeLinecap="round" />
  </svg>
);

export const Wave = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M2 12c2.5-5 5-5 7.5 0s5 5 7.5 0 3.5-4 5-2" strokeLinecap="round" />
  </svg>
);

export const Shield = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M12 3l7 3v5c0 4.6-3 8.4-7 10-4-1.6-7-5.4-7-10V6l7-3z" strokeLinejoin="round" />
    <path d="M9 12l2 2 4-4.5" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const CameraOff = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M4 8h2.5l1.5-2.5h8L17.5 8H20a1 1 0 0 1 1 1v8" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M21 19v1a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1h1" strokeLinecap="round" />
    <circle cx="12" cy="13" r="3.4" />
    <path d="M3 3l18 18" strokeLinecap="round" />
  </svg>
);

export const Contrast = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 3v18" />
    <path d="M12 3a9 9 0 0 1 0 18z" fill="currentColor" stroke="none" />
  </svg>
);

export const Speaker = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M4 9v6h4l5 4V5L8 9H4z" strokeLinejoin="round" />
    <path d="M16.5 8.5a5 5 0 0 1 0 7" strokeLinecap="round" />
    <path d="M19 6a9 9 0 0 1 0 12" strokeLinecap="round" />
  </svg>
);

export const SpeakerOff = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M4 9v6h4l5 4V5L8 9H4z" strokeLinejoin="round" />
    <path d="M17 9.5l5 5M22 9.5l-5 5" strokeLinecap="round" />
  </svg>
);

export const Play = ({ className }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={base(className)} aria-hidden>
    <path d="M8 5.5v13l11-6.5-11-6.5z" />
  </svg>
);

export const Pulse = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M2 12h4l2.5-7 4 14 2.5-7h7" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const Chip = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <rect x="6" y="6" width="12" height="12" rx="1.5" />
    <rect x="10" y="10" width="4" height="4" />
    <path d="M9 2v3M15 2v3M9 19v3M15 19v3M2 9h3M2 15h3M19 9h3M19 15h3" strokeLinecap="round" />
  </svg>
);

export const Eye = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M2 12s3.5-6.5 10-6.5S22 12 22 12s-3.5 6.5-10 6.5S2 12 2 12z" strokeLinejoin="round" />
    <circle cx="12" cy="12" r="2.8" />
  </svg>
);

export const Compass = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <circle cx="12" cy="12" r="9" />
    <path d="M15.5 8.5l-2.3 4.7-4.7 2.3 2.3-4.7 4.7-2.3z" strokeLinejoin="round" />
  </svg>
);

export const Hand = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M8 12V6.5a1.5 1.5 0 0 1 3 0V11m0-5.5v-1a1.5 1.5 0 0 1 3 0V11m0-4.5a1.5 1.5 0 0 1 3 0V13" strokeLinecap="round" />
    <path d="M17 13v-1.5a1.5 1.5 0 0 1 3 0V15a7 7 0 0 1-7 7h-1.2a7 7 0 0 1-5.6-2.8L4 16.2a1.7 1.7 0 0 1 2.6-2.2L8 15.5V12" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const Metronome = ({ className, strokeWidth = 2 }: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} className={base(className)} aria-hidden>
    <path d="M9 3h6l4 17H5L9 3z" strokeLinejoin="round" />
    <path d="M12 14L17.5 5" strokeLinecap="round" />
    <circle cx="12" cy="14" r="1.4" fill="currentColor" stroke="none" />
    <path d="M7.5 17h9" strokeLinecap="round" />
  </svg>
);
