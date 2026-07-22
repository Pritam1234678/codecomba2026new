import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';

import SkeletonLoader from '../components/SkeletonLoader';
// ── Same theme tokens as Practice.jsx ─────────────────────────────────────────
const C = {
    bg:         '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceHi:  '#2a2a2a',
    border:     '#50453b',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
    success:    '#4ade80',
};

const DIFF_CFG = {
    EASY:   { color: C.success,   label: 'Easy'   },
    MEDIUM: { color: C.secondary, label: 'Medium' },
    HARD:   { color: C.error,     label: 'Hard'   },
};

// outcome → use only the existing palette, no new neon colors
const OUTCOME_CFG = {
    USER_A_WIN: { color: C.primary,   label: 'A Wins'    },
    USER_B_WIN: { color: C.muted,     label: 'B Wins'    },
    DRAW:       { color: C.secondary, label: 'Draw'      },
    ABANDONED:  { color: C.outline,   label: 'Abandoned' },
};

const PAGE_SIZE = 8;

const formatDate = (d) => d
    ? new Date(d).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })
    : '—';

// ── Diff badge — identical style to Practice.jsx ──────────────────────────────
const DiffBadge = ({ diff }) => {
    const cfg = DIFF_CFG[diff] || { color: C.outline, label: diff || '—' };
    return (
        <span style={{
            padding: '3px 10px',
            border: `1px solid ${cfg.color}`,
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '10px', letterSpacing: '0.12em',
            color: cfg.color, textTransform: 'uppercase',
        }}>{cfg.label}</span>
    );
};

// ── Outcome badge ─────────────────────────────────────────────────────────────
const OutcomeBadge = ({ outcome }) => {
    const cfg = OUTCOME_CFG[outcome] || { color: C.outline, label: outcome || 'Finished' };
    return (
        <span style={{
            padding: '3px 10px',
            border: `1px solid ${cfg.color}40`,
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '10px', letterSpacing: '0.12em',
            color: cfg.color, textTransform: 'uppercase',
        }}>{cfg.label}</span>
    );
};

// ── History Match Card — same card anatomy as ProblemCard in Practice.jsx ─────
const HistoryCard = ({ match }) => {
    const [hovered, setHovered] = useState(false);
    const aWon = match.outcome === 'USER_A_WIN';
    const bWon = match.outcome === 'USER_B_WIN';
    const diff = DIFF_CFG[match.difficulty] || { color: C.outline };

    return (
        <div
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                border: `1px solid ${hovered ? diff.color : C.border}`,
                backgroundColor: hovered ? C.surfaceCon : C.surfaceLow,
                padding: '1.25rem 1.5rem',
                display: 'flex', flexDirection: 'column', gap: '10px',
                transition: 'all 0.2s',
                position: 'relative', overflow: 'hidden',
            }}
        >
            {/* Top accent bar — same as ProblemCard */}
            <div style={{
                position: 'absolute', top: 0, left: 0, right: 0, height: '2px',
                backgroundColor: diff.color,
                opacity: hovered ? 1 : 0.3, transition: 'opacity 0.2s',
            }} />

            {/* Row 1: date + badges */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                <span style={{
                    fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                    letterSpacing: '0.08em', color: C.outline,
                }}>{formatDate(match.startedAt)}</span>
                <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                    <DiffBadge diff={match.difficulty} />
                    <OutcomeBadge outcome={match.outcome} />
                </div>
            </div>

            {/* Row 2: players */}
            <div style={{
                display: 'grid', gridTemplateColumns: '1fr auto 1fr',
                alignItems: 'center', gap: '12px',
            }}>
                {/* Player A */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <span style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: '17px', fontWeight: 600,
                        color: aWon ? C.primary : hovered ? C.muted : C.onBg,
                        transition: 'color 0.2s',
                    }}>
                        {aWon && (
                            <span className="material-symbols-outlined" style={{
                                fontSize: '14px', marginRight: '5px',
                                color: C.secondary, fontVariationSettings: "'FILL' 1",
                                verticalAlign: 'middle',
                            }}>military_tech</span>
                        )}
                        {match.userAUsername || `User ${match.userAId}`}
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase' }}>
                        Player A
                    </span>
                </div>

                {/* VS divider */}
                <span style={{
                    fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                    letterSpacing: '0.15em', color: C.outline,
                    padding: '0 4px', textTransform: 'uppercase',
                }}>vs</span>

                {/* Player B */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', alignItems: 'flex-end' }}>
                    <span style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: '17px', fontWeight: 600,
                        color: bWon ? C.primary : hovered ? C.muted : C.onBg,
                        transition: 'color 0.2s',
                    }}>
                        {match.userBUsername || `User ${match.userBId}`}
                        {bWon && (
                            <span className="material-symbols-outlined" style={{
                                fontSize: '14px', marginLeft: '5px',
                                color: C.secondary, fontVariationSettings: "'FILL' 1",
                                verticalAlign: 'middle',
                            }}>military_tech</span>
                        )}
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase' }}>
                        Player B
                    </span>
                </div>
            </div>
        </div>
    );
};

// ── Live Match Card ───────────────────────────────────────────────────────────
const LiveCard = ({ match, onCancel }) => {
    const [hovered, setHovered] = useState(false);
    const urgent = match.remainingSeconds < 300;
    const diff = DIFF_CFG[match.difficulty] || { color: C.outline };

    const formatTime = (s) => {
        if (!s || s < 0) return '0:00';
        return `${Math.floor(s / 60)}:${(s % 60).toString().padStart(2, '0')}`;
    };

    return (
        <div
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                border: `1px solid ${hovered ? diff.color : C.border}`,
                backgroundColor: hovered ? C.surfaceCon : C.surfaceLow,
                padding: '1.25rem 1.5rem',
                display: 'flex', flexDirection: 'column', gap: '10px',
                transition: 'all 0.2s',
                position: 'relative', overflow: 'hidden',
            }}
        >
            {/* Top accent bar */}
            <div style={{
                position: 'absolute', top: 0, left: 0, right: 0, height: '2px',
                backgroundColor: diff.color, opacity: hovered ? 1 : 0.3, transition: 'opacity 0.2s',
            }} />

            {/* Row 1: live indicator + diff + cancel */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{
                        width: '6px', height: '6px', borderRadius: '50%',
                        backgroundColor: C.success, display: 'inline-block',
                        animation: 'dmPulse 1.5s infinite',
                    }} />
                    <span style={{
                        fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                        letterSpacing: '0.15em', color: C.success, textTransform: 'uppercase',
                    }}>Live</span>
                    <DiffBadge diff={match.difficulty} />
                </div>
                <button onClick={() => onCancel(match.matchId)} style={{
                    padding: '3px 12px',
                    border: `1px solid ${C.border}`,
                    backgroundColor: 'transparent',
                    color: C.outline,
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '10px', letterSpacing: '0.1em', textTransform: 'uppercase',
                    cursor: 'pointer', transition: 'all 0.2s',
                }}
                    onMouseEnter={e => { e.currentTarget.style.borderColor = C.error; e.currentTarget.style.color = C.error; }}
                    onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.outline; }}
                >Cancel</button>
            </div>

            {/* Row 2: players + timer */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr auto 1fr', alignItems: 'center', gap: '12px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '17px', fontWeight: 600, color: C.primary }}>
                        {match.userAUsername || `User ${match.userAId}`}
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        {match.runsUsed ?? 0}/5 runs
                    </span>
                </div>

                <div style={{ textAlign: 'center', padding: '0 8px' }}>
                    <div style={{
                        fontFamily: "'JetBrains Mono', monospace", fontSize: '20px', fontWeight: 700,
                        color: urgent ? C.error : C.onBg,
                        animation: urgent ? 'dmBlink 1s infinite' : 'none',
                    }}>{formatTime(match.remainingSeconds)}</div>
                    <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        left
                    </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', alignItems: 'flex-end' }}>
                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '17px', fontWeight: 600, color: C.primary }}>
                        {match.userBUsername || `User ${match.userBId}`}
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        {match.runsUsed ?? 0}/5 runs
                    </span>
                </div>
            </div>
        </div>
    );
};

// ── Main Page ─────────────────────────────────────────────────────────────────
const AdminDuelMonitor = () => {
    const [liveMatches, setLiveMatches]       = useState([]);
    const [historyMatches, setHistoryMatches] = useState([]);
    const [loading, setLoading]               = useState(true);
    const [error, setError]                   = useState(null);
    const [historyTotal, setHistoryTotal]     = useState(0);
    const [page, setPage]                     = useState(0);

    const fetchMatches = useCallback(async () => {
        try {
            const [liveRes, histRes] = await Promise.all([
                api.get('/admin/duels', { params: { status: 'IN_PROGRESS', limit: 100, offset: 0 } }),
                api.get('/admin/duels', { params: { status: 'FINISHED', limit: PAGE_SIZE, offset: page * PAGE_SIZE } }),
            ]);
            setLiveMatches(liveRes.data.content || []);
            setHistoryMatches(histRes.data.content || []);
            setHistoryTotal(histRes.data.totalElements || 0);
            setError(null);
        } catch {
            setError('Failed to load matches. You may not have admin access.');
        } finally {
            setLoading(false);
        }
    }, [page]);

    useEffect(() => {
        fetchMatches();
        const iv = setInterval(fetchMatches, 5000);
        return () => clearInterval(iv);
    }, [fetchMatches]);

    const cancelMatch = async (matchId) => {
        if (!window.confirm('Cancel this match? It will be marked as ABANDONED.')) return;
        try {
            await api.post(`/admin/duels/${matchId}/cancel`);
            fetchMatches();
        } catch (err) {
            alert('Failed to cancel: ' + (err.response?.data?.message || err.message));
        }
    };

    const totalPages = Math.ceil(historyTotal / PAGE_SIZE);

    if (loading) return <SkeletonLoader />;
    if (error) return (
        <div style={{ padding: '2rem', color: C.error, fontFamily: "'JetBrains Mono', monospace" }}>
            <p>{error}</p>
        </div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh', padding: '48px 64px' }}>

            {/* ── Header — same style as Practice hero ── */}
            <section style={{ marginBottom: '3rem', borderBottom: `1px solid ${C.border}`, paddingBottom: '2rem' }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase' }}>
                    Admin Panel
                </span>
                <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(32px, 4vw, 48px)', fontWeight: 700, lineHeight: 1.1, color: C.primary, margin: '0.5rem 0 0.75rem' }}>
                    Duel Monitor
                </h1>
                <p style={{ fontSize: '14px', color: C.muted, margin: 0 }}>
                    Live matches auto-refresh every 5 seconds
                </p>
            </section>

            {/* ── Live Matches ── */}
            <section style={{ marginBottom: '3rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1.25rem' }}>
                    <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: C.success, display: 'inline-block', animation: 'dmPulse 1.5s infinite' }} />
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>
                        Live Matches
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                        ({liveMatches.length})
                    </span>
                </div>

                {liveMatches.length === 0 ? (
                    <div style={{ border: `1px solid ${C.border}`, padding: '3rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, letterSpacing: '0.08em' }}>
                        No matches in progress right now
                    </div>
                ) : (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(380px, 1fr))', gap: '16px' }}>
                        {liveMatches.map(m => <LiveCard key={m.matchId} match={m} onCancel={cancelMatch} />)}
                    </div>
                )}
            </section>

            {/* ── Match History ── */}
            <section>
                <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>
                        Match History
                    </span>
                    {historyTotal > 0 && (
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                            {historyTotal} total · page {page + 1}/{totalPages}
                        </span>
                    )}
                </div>

                {historyMatches.length === 0 ? (
                    <div style={{ border: `1px solid ${C.border}`, padding: '3rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, letterSpacing: '0.08em' }}>
                        No completed matches yet
                    </div>
                ) : (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(380px, 1fr))', gap: '16px' }}>
                        {historyMatches.map(m => <HistoryCard key={m.matchId} match={m} />)}
                    </div>
                )}

                {/* ── Pagination — same style as AdminProblemManagement ── */}
                {totalPages > 1 && (
                    <div style={{ marginTop: '2rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
                        <button
                            onClick={() => setPage(p => Math.max(0, p - 1))}
                            disabled={page === 0}
                            style={{
                                padding: '6px 10px', backgroundColor: 'transparent',
                                border: `1px solid ${page === 0 ? C.border : C.outline}`,
                                color: page === 0 ? C.border : C.outline,
                                cursor: page === 0 ? 'not-allowed' : 'pointer',
                            }}
                            onMouseEnter={e => { if (page > 0) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                            onMouseLeave={e => { e.currentTarget.style.borderColor = page === 0 ? C.border : C.outline; e.currentTarget.style.color = page === 0 ? C.border : C.outline; }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '18px', display: 'block' }}>chevron_left</span>
                        </button>

                        {Array.from({ length: totalPages }, (_, i) => {
                            const near = Math.abs(i - page) <= 1;
                            const edge = i === 0 || i === totalPages - 1;
                            if (!near && !edge) {
                                if (i === 1 || i === totalPages - 2) return <span key={i} style={{ color: C.outline, fontSize: '12px', padding: '0 2px', fontFamily: "'JetBrains Mono', monospace" }}>…</span>;
                                return null;
                            }
                            const active = i === page;
                            return (
                                <button key={i} onClick={() => setPage(i)}
                                    style={{
                                        width: '34px', height: '34px',
                                        backgroundColor: active ? C.secondary : 'transparent',
                                        border: `1px solid ${active ? C.secondary : C.border}`,
                                        color: active ? C.bg : C.outline,
                                        cursor: 'pointer',
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '12px', fontWeight: active ? 700 : 400,
                                        transition: 'all 0.15s',
                                    }}
                                    onMouseEnter={e => { if (!active) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                                    onMouseLeave={e => { if (!active) { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.outline; } }}
                                >{i + 1}</button>
                            );
                        })}

                        <button
                            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                            disabled={page >= totalPages - 1}
                            style={{
                                padding: '6px 10px', backgroundColor: 'transparent',
                                border: `1px solid ${page >= totalPages - 1 ? C.border : C.outline}`,
                                color: page >= totalPages - 1 ? C.border : C.outline,
                                cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
                            }}
                            onMouseEnter={e => { if (page < totalPages - 1) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                            onMouseLeave={e => { e.currentTarget.style.borderColor = page >= totalPages - 1 ? C.border : C.outline; e.currentTarget.style.color = page >= totalPages - 1 ? C.border : C.outline; }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '18px', display: 'block' }}>chevron_right</span>
                        </button>
                    </div>
                )}
            </section>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                @keyframes dmPulse { 0%,100%{opacity:1} 50%{opacity:0.3} }
                @keyframes dmBlink { 0%,100%{opacity:1} 50%{opacity:0.25} }
                @media (max-width: 768px) {
                    div[style*="padding: '48px 64px'"] { padding: 24px 16px !important; }
                }
            `}</style>
        </div>
    );
};

export default AdminDuelMonitor;
