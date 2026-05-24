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

// ── Difficulty bucket metadata (label + window in seconds) ──────────────────
const DIFFICULTIES = [
    { value: 'EASY',   label: 'Easy',   windowMin: 20 },
    { value: 'MEDIUM', label: 'Medium', windowMin: 40 },
    { value: 'HARD',   label: 'Hard',   windowMin: 65 },
];

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

    const [difficulty, setDifficulty] = useState(null);
    const [history, setHistory]     = useState([]);
    const [historyErr, setHistoryErr] = useState(false);
    const [historyLoading, setHistoryLoading] = useState(true);

    useEffect(() => {
        if (matchId) navigate(`/duel/${matchId}`);
    }, [matchId, navigate]);

    useEffect(() => {
        let cancelled = false;
        const selfUser = (() => {
            try { return JSON.parse(localStorage.getItem('user') || 'null'); }
            catch { return null; }
        })();
        const selfId = selfUser?.id ?? null;
        getDuelHistory(10)
            .then((data) => {
                if (cancelled) return;
                const items = Array.isArray(data) ? data : (data?.items ?? []);
                const decorated = items.map((row) => ({
                    ...row,
                    youWon: row.winnerUserId != null && selfId != null
                        && Number(row.winnerUserId) === Number(selfId),
                }));
                setHistory(decorated);
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

    const isAwaiting  = state === STATES.AWAITING;
    const isCooldown  = state === STATES.COOLDOWN;
    const isError     = state === STATES.ERROR;
    const buttonDisabled = isAwaiting || isCooldown || !difficulty;

    let buttonLabel;
    if (isAwaiting) {
        buttonLabel = 'SEARCHING…';
    } else if (isCooldown) {
        buttonLabel = `COOLDOWN: ${cooldownSeconds}s`;
    } else if (difficulty) {
        buttonLabel = `FIND MATCH (${difficulty})`;
    } else {
        buttonLabel = 'PICK A DIFFICULTY';
    }

    const onFindMatch = () => {
        if (!difficulty) return;
        findMatch(difficulty);
    };

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

                {/* ── Header ───────────────────────────────────────────── */}
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
                        Pick a difficulty, hit Find Match, and prove your speed against another player at your level.
                    </p>
                </motion.header>

                {/* ── Rules Card ──────────────────────────────────────── */}
                <motion.section
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6, delay: 0.05 }}
                    style={{
                        backgroundColor: C.surfaceLow,
                        border: `1px solid ${C.border}`,
                        padding: '1.25rem 1.5rem',
                        marginBottom: '1.5rem',
                    }}
                >
                    <div style={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '11px', letterSpacing: '0.15em',
                        color: C.primary, textTransform: 'uppercase',
                        marginBottom: '10px',
                    }}>
                        Rules
                    </div>
                    <ul style={{
                        margin: 0, paddingLeft: '20px',
                        fontSize: '13.5px', lineHeight: 1.7, color: C.muted,
                    }}>
                        <li>Choose a difficulty — you'll be matched with another player of the same level.</li>
                        <li>EASY: 20 min &nbsp;|&nbsp; MEDIUM: 40 min &nbsp;|&nbsp; HARD: 65 min</li>
                        <li>5 Runs + 2 Submits per match. Run = examples only, doesn't count toward win.</li>
                        <li>First to pass all test cases wins. On timeout, highest test-cases-passed wins.</li>
                        <li>Match runs in fullscreen. Refresh / close tab = auto-forfeit.</li>
                    </ul>
                </motion.section>

                {/* ── Difficulty selector + action ─────────────────────── */}
                <motion.section
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6, delay: 0.1 }}
                    style={{
                        backgroundColor: C.surfaceCon,
                        border: `1px solid ${C.border}`,
                        borderTop: `2px solid ${C.secondary}`,
                        padding: '2.5rem 2rem',
                        marginBottom: '2rem',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '1.25rem',
                    }}
                >
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
                        fontSize: '28px', fontWeight: 600,
                        color: C.onBg, margin: 0, textAlign: 'center',
                    }}>
                        Pick a difficulty
                    </h3>

                    {/* Difficulty button group */}
                    <div style={{
                        display: 'flex', gap: '12px', flexWrap: 'wrap', justifyContent: 'center',
                    }}>
                        {DIFFICULTIES.map((d) => {
                            const selected = difficulty === d.value;
                            return (
                                <button
                                    key={d.value}
                                    type="button"
                                    onClick={() => setDifficulty(d.value)}
                                    disabled={isAwaiting || isCooldown}
                                    style={{
                                        padding: '14px 28px',
                                        minWidth: '140px',
                                        border: `2px solid ${selected ? C.primary : C.border}`,
                                        backgroundColor: selected ? 'rgba(241,188,139,0.10)' : 'transparent',
                                        color: selected ? C.primary : C.muted,
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '13px', letterSpacing: '0.15em',
                                        textTransform: 'uppercase',
                                        cursor: (isAwaiting || isCooldown) ? 'not-allowed' : 'pointer',
                                        transition: 'all 0.15s',
                                        display: 'flex',
                                        flexDirection: 'column',
                                        alignItems: 'center',
                                        gap: '4px',
                                        opacity: (isAwaiting || isCooldown) ? 0.5 : 1,
                                    }}
                                >
                                    <span style={{ fontWeight: 700 }}>{d.label}</span>
                                    <span style={{
                                        fontSize: '10px', letterSpacing: '0.05em',
                                        color: selected ? C.primary : C.outline,
                                        fontWeight: 400,
                                    }}>
                                        {d.windowMin} min
                                    </span>
                                </button>
                            );
                        })}
                    </div>

                    {/* Error banner */}
                    {(isError || error) && (
                        <div style={{
                            border: `1px solid ${C.error}`,
                            backgroundColor: 'rgba(255,180,171,0.08)',
                            color: C.error,
                            padding: '10px 14px',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '12px', letterSpacing: '0.05em',
                            maxWidth: '460px', width: '100%',
                            display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px',
                        }}>
                            <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>error</span>
                                {error || 'Something went wrong'}
                            </span>
                            <button
                                onClick={onFindMatch}
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
                        onClick={onFindMatch}
                        disabled={buttonDisabled}
                        className={isAwaiting ? 'duel-btn-pulse' : ''}
                        style={{
                            padding: '18px 56px',
                            border: `2px solid ${buttonDisabled ? C.border : C.secondary}`,
                            color: buttonDisabled ? C.outline : C.secondary,
                            backgroundColor: 'transparent',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '15px', letterSpacing: '0.2em',
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
                            display: 'flex', flexDirection: 'column',
                            alignItems: 'center', gap: '12px',
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
                                Searching {difficulty} bucket…
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

                    {!isAwaiting && !isCooldown && difficulty && (
                        <p style={{ fontSize: '12.5px', color: C.outline, margin: 0, textAlign: 'center' }}>
                            You'll be matched with another player who picked {difficulty}.
                        </p>
                    )}
                </motion.section>

                {/* ── Recent Duels ─────────────────────────────────────── */}
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
