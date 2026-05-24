import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import useDuelMatchmaking from '../hooks/useDuelMatchmaking';
import { getDuelHistory } from '../services/duelService';

// ── Color palette — mirrors AdminDashboard.jsx ──────────────────────────────
const C = {
    bg:         '#131313',
    surface:    '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceMin: '#0e0e0e',
    surfaceHi:  '#2a2a2a',
    border:     '#50453b',
    borderThin: 'rgba(80,69,59,0.6)',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
};

// ── Outcome label resolver — keeps the history table readable ───────────────
const OUTCOME_LABEL = {
    USER_A_WIN: 'Win',
    USER_B_WIN: 'Win',
    DRAW:       'Draw',
    ABANDONED:  'Abandoned',
};

const formatEndedAt = (iso) => {
    if (!iso) return '—';
    try {
        const d = new Date(iso);
        return d.toLocaleString(undefined, {
            month: 'short', day: 'numeric',
            hour: '2-digit', minute: '2-digit',
        });
    } catch {
        return '—';
    }
};

const Duel = () => {
    const navigate = useNavigate();
    const {
        state, matchId, cooldownSeconds, error,
        findMatch, cancel, STATES,
    } = useDuelMatchmaking();

    const [history, setHistory]     = useState([]);
    const [historyErr, setHistoryErr] = useState(false);
    const [historyLoading, setHistoryLoading] = useState(true);

    // ── Navigate to the arena once the hook surfaces a matchId ──────────────
    useEffect(() => {
        if (matchId) {
            navigate(`/duel/${matchId}`);
        }
    }, [matchId, navigate]);

    // ── Recent duel history — endpoint is pending on the backend, so a 404 ──
    // is the expected MVP path; render the empty state in that case.
    useEffect(() => {
        let cancelled = false;
        getDuelHistory(10)
            .then((data) => {
                if (cancelled) return;
                // Tolerate either an array or a `{ items: [...] }` envelope.
                const items = Array.isArray(data) ? data : (data?.items ?? []);
                setHistory(items);
                setHistoryErr(false);
            })
            .catch(() => {
                if (cancelled) return;
                setHistoryErr(true);
                setHistory([]);
            })
            .finally(() => { if (!cancelled) setHistoryLoading(false); });
        return () => { cancelled = true; };
    }, []);

    // ── Button label / disabled state derive directly from the state ────────
    const isAwaiting  = state === STATES.AWAITING;
    const isCooldown  = state === STATES.COOLDOWN;
    const isError     = state === STATES.ERROR;
    const isFinding   = isAwaiting; // alias for readability
    const buttonDisabled = isAwaiting || isCooldown;

    let buttonLabel = 'FIND MATCH';
    if (isAwaiting)      buttonLabel = 'SEARCHING…';
    else if (isCooldown) buttonLabel = `COOLDOWN: ${cooldownSeconds}s`;

    return (
        <div style={{
            backgroundColor: C.bg,
            color: C.onBg,
            fontFamily: "'Geist', sans-serif",
            flex: 1,
            minHeight: '100vh',
            display: 'flex',
            flexDirection: 'column',
        }}>
            <main style={{ flex: 1, padding: '32px 48px 32px', backgroundColor: C.bg, display: 'flex', flexDirection: 'column' }}>

                {/* ── Header ───────────────────────────────────────────────── */}
                <motion.header
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6 }}
                    style={{
                        marginBottom: '2rem',
                        paddingBottom: '1.5rem',
                        borderBottom: `1px solid ${C.border}`,
                    }}
                >
                    <h2 style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: '40px', fontWeight: 700,
                        lineHeight: 1.1, letterSpacing: '-0.02em',
                        color: C.onBg, marginBottom: '6px',
                    }}>
                        Duel Mode
                    </h2>
                    <p style={{ fontSize: '15px', color: C.muted, lineHeight: 1.5, maxWidth: '520px' }}>
                        Search for an opponent and prove your speed. Same problem, both clocks running, first to AC takes the round.
                    </p>
                </motion.header>

                {/* ── Hero Card ────────────────────────────────────────────── */}
                <motion.section
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6, delay: 0.1 }}
                    style={{
                        backgroundColor: C.surfaceCon,
                        border: `1px solid ${C.border}`,
                        borderTop: `2px solid ${C.secondary}`,
                        padding: '3rem 2rem',
                        marginBottom: '2rem',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '1.5rem',
                        position: 'relative',
                        overflow: 'hidden',
                    }}
                >
                    {/* Section eyebrow */}
                    <span style={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '11px', letterSpacing: '0.15em',
                        color: C.primary,
                        border: `1px solid ${C.primary}`,
                        padding: '4px 12px',
                        textTransform: 'uppercase',
                    }}>
                        1v1 · Live Arena
                    </span>

                    <h3 style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: '32px', fontWeight: 600,
                        color: C.onBg, margin: 0, textAlign: 'center',
                    }}>
                        Ready when you are
                    </h3>

                    {/* Error banner — sits directly above the action button */}
                    {(isError || error) && (
                        <div style={{
                            border: `1px solid ${C.error}`,
                            backgroundColor: 'rgba(255,180,171,0.08)',
                            color: C.error,
                            padding: '12px 16px',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '12px', letterSpacing: '0.05em',
                            display: 'flex', alignItems: 'center', gap: '12px',
                            maxWidth: '420px', width: '100%',
                            justifyContent: 'space-between',
                        }}>
                            <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>error</span>
                                {error || 'Something went wrong'}
                            </span>
                            <button
                                onClick={findMatch}
                                disabled={buttonDisabled}
                                style={{
                                    background: 'transparent',
                                    border: `1px solid ${C.error}`,
                                    color: C.error,
                                    padding: '4px 12px',
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '11px', letterSpacing: '0.1em',
                                    textTransform: 'uppercase',
                                    cursor: buttonDisabled ? 'not-allowed' : 'pointer',
                                    opacity: buttonDisabled ? 0.5 : 1,
                                }}
                            >
                                Retry
                            </button>
                        </div>
                    )}

                    {/* Find Match button */}
                    <button
                        onClick={findMatch}
                        disabled={buttonDisabled}
                        className={isAwaiting ? 'duel-btn-pulse' : ''}
                        style={{
                            padding: '20px 64px',
                            border: `2px solid ${buttonDisabled ? C.border : C.secondary}`,
                            color: buttonDisabled ? C.outline : C.secondary,
                            backgroundColor: 'transparent',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '16px', letterSpacing: '0.2em',
                            fontWeight: 600,
                            textTransform: 'uppercase',
                            cursor: buttonDisabled ? 'not-allowed' : 'pointer',
                            transition: 'all 0.2s',
                            minWidth: '320px',
                            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px',
                            opacity: isCooldown ? 0.6 : 1,
                        }}
                        onMouseEnter={(e) => {
                            if (buttonDisabled) return;
                            e.currentTarget.style.backgroundColor = C.secondary;
                            e.currentTarget.style.color = C.bg;
                        }}
                        onMouseLeave={(e) => {
                            if (buttonDisabled) return;
                            e.currentTarget.style.backgroundColor = 'transparent';
                            e.currentTarget.style.color = C.secondary;
                        }}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '22px' }}>
                            {isAwaiting ? 'hourglass_top' : isCooldown ? 'schedule' : 'swords'}
                        </span>
                        {buttonLabel}
                    </button>

                    {/* Awaiting spinner + Cancel */}
                    {isAwaiting && (
                        <div style={{
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            gap: '12px',
                            marginTop: '4px',
                        }}>
                            <div style={{
                                display: 'flex', alignItems: 'center', gap: '12px',
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', letterSpacing: '0.1em',
                                color: C.muted, textTransform: 'uppercase',
                            }}>
                                <span className="duel-spinner" style={{
                                    width: '14px', height: '14px',
                                    border: `2px solid ${C.borderThin}`,
                                    borderTopColor: C.primary,
                                    borderRadius: '50%',
                                    display: 'inline-block',
                                }} />
                                Searching for opponent…
                            </div>
                            <button
                                onClick={cancel}
                                style={{
                                    background: 'transparent',
                                    border: `1px solid ${C.border}`,
                                    color: C.muted,
                                    padding: '8px 24px',
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '11px', letterSpacing: '0.1em',
                                    textTransform: 'uppercase',
                                    cursor: 'pointer',
                                    transition: 'all 0.2s',
                                }}
                                onMouseEnter={(e) => { e.currentTarget.style.borderColor = C.error; e.currentTarget.style.color = C.error; }}
                                onMouseLeave={(e) => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}
                            >
                                Cancel
                            </button>
                        </div>
                    )}

                    {/* Helper subtext under the button */}
                    {!isAwaiting && !isCooldown && (
                        <p style={{
                            fontSize: '13px',
                            color: C.outline,
                            margin: 0,
                            textAlign: 'center',
                        }}>
                            Average pairing time is under a few seconds when other players are queued.
                        </p>
                    )}
                </motion.section>

                {/* ── Recent Duels ─────────────────────────────────────────── */}
                <motion.section
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6, delay: 0.2 }}
                    style={{
                        backgroundColor: C.surfaceCon,
                        border: `1px solid ${C.border}`,
                        padding: '1.5rem',
                        display: 'flex',
                        flexDirection: 'column',
                    }}
                >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                        <h3 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '24px', fontWeight: 600,
                            color: C.onBg,
                            display: 'flex', alignItems: 'center', gap: '12px',
                            margin: 0,
                        }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '24px', color: C.primary }}>
                                history
                            </span>
                            Recent Duels
                        </h3>
                        <span style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px', letterSpacing: '0.15em',
                            color: C.outline,
                            border: `1px solid ${C.border}`,
                            padding: '4px 12px',
                        }}>
                            LAST 10
                        </span>
                    </div>

                    {historyLoading ? (
                        <p style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '12px',
                            color: C.outline,
                            padding: '2rem 0',
                            textAlign: 'center',
                        }}>
                            Loading…
                        </p>
                    ) : (history.length === 0) ? (
                        <div style={{
                            border: `1px dashed ${C.border}`,
                            padding: '3rem 1rem',
                            textAlign: 'center',
                            color: C.muted,
                        }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '36px', color: C.outline, marginBottom: '8px', display: 'block' }}>
                                inbox
                            </span>
                            <p style={{ margin: 0, fontSize: '14px' }}>
                                {historyErr
                                    ? 'No duels yet — start your first match above'
                                    : 'No duels yet — start your first match above'}
                            </p>
                        </div>
                    ) : (
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                                <tr style={{ borderBottom: `1px solid ${C.border}` }}>
                                    {['Opponent', 'Problem', 'Outcome', 'Ended'].map((h) => (
                                        <th key={h} style={{
                                            textAlign: 'left',
                                            padding: '12px 8px',
                                            fontFamily: "'JetBrains Mono', monospace",
                                            fontSize: '11px', letterSpacing: '0.1em',
                                            color: C.outline,
                                            textTransform: 'uppercase',
                                            fontWeight: 400,
                                        }}>{h}</th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {history.map((row, idx) => (
                                    <tr key={row.matchId ?? idx} style={{ borderBottom: `1px solid ${C.borderThin}` }}>
                                        <td style={{ padding: '12px 8px', fontSize: '14px', color: C.onBg }}>
                                            {row.opponentUsername ?? '—'}
                                        </td>
                                        <td style={{ padding: '12px 8px', fontSize: '14px', color: C.muted }}>
                                            {row.problemTitle ?? '—'}
                                        </td>
                                        <td style={{ padding: '12px 8px', fontSize: '13px', fontFamily: "'JetBrains Mono', monospace", color: row.youWon ? C.primary : (row.outcome === 'DRAW' ? C.muted : C.outline) }}>
                                            {row.youWon ? 'Win' : (OUTCOME_LABEL[row.outcome] ?? row.outcome ?? '—')}
                                        </td>
                                        <td style={{ padding: '12px 8px', fontSize: '13px', color: C.muted, fontFamily: "'JetBrains Mono', monospace" }}>
                                            {formatEndedAt(row.endedAt)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </motion.section>
            </main>

            <style>{`
                @keyframes duelSpin {
                    to { transform: rotate(360deg); }
                }
                .duel-spinner {
                    animation: duelSpin 0.9s linear infinite;
                }
                @keyframes duelPulseBorder {
                    0%, 100% { box-shadow: 0 0 0 0 rgba(241,188,139,0.45); }
                    50%      { box-shadow: 0 0 0 8px rgba(241,188,139,0); }
                }
                .duel-btn-pulse {
                    animation: duelPulseBorder 1.6s ease-in-out infinite;
                }
                .material-symbols-outlined {
                    font-variation-settings: 'FILL' 0, 'wght' 300;
                }
            `}</style>
        </div>
    );
};

export default Duel;
