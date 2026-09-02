import { TICKER_ITEMS } from "../lib/data";

export default function PrivacyTicker() {
  return (
    <div className="overflow-hidden border-y" style={{ background: "var(--yellow)", color: "var(--ink)", borderColor: "var(--ink)" }}>
      <div className="marquee-track flex w-max whitespace-nowrap" aria-label="Privacy guarantees">
        {[0, 1].map((copy) => (
          <div key={copy} className="flex items-center" aria-hidden={copy === 1}>
            {TICKER_ITEMS.map((item) => (
              <span key={`${copy}-${item}`} className="display flex items-center gap-5 pl-5 pr-4 py-3 text-[0.95rem] tracking-wider">
                {item}
                <svg viewBox="0 0 24 24" className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2.6" aria-hidden>
                  <path d="M4 12h15M13 6l6 6-6 6" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </span>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
