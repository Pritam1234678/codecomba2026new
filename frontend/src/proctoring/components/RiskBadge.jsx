import { C } from '../constants';

// Risk band → palette token (Req 15.1). LOW = success-tinted, MEDIUM
// = warning, HIGH = error. Strict to the Practice / Contest palette
// per Req 23.8 — no extra neon colors.
const BAND_TONE = {
    LOW:    { color: C.success, label: 'Low'    },
    MEDIUM: { color: C.warning, label: 'Medium' },
    HIGH:   { color: C.error,   label: 'High'   },
};

/**
 * Compact, color-coded risk band pill matching the existing badge
 * style in `ContestList.jsx` / `AdminDuelMonitor.jsx`. Reuses the
 * shared proctoring palette so dashboard, drill-down, and any future
 * surface render the band the same way.
 *
 * @param {{ band?: 'LOW'|'MEDIUM'|'HIGH', score?: number, compact?: boolean }} props
 */
export default function RiskBadge({ band, score, compact = false }) {
    const tone = BAND_TONE[band] || { color: C.outline, label: band || '—' };
    const showScore = typeof score === 'number' && Number.isFinite(score);
    return (
        <span style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            padding: compact ? '2px 8px' : '3px 10px',
            border: `1px solid ${tone.color}`,
            color: tone.color,
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: compact ? '9px' : '10px',
            letterSpacing: '0.12em',
            textTransform: 'uppercase',
            whiteSpace: 'nowrap',
        }}>
            <span>{tone.label}</span>
            {showScore && (
                <span style={{ opacity: 0.8 }}>· {score}</span>
            )}
        </span>
    );
}
