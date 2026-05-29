import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';

const C = {
    bg: '#0e0e0e',
    surface: '#161514',
    surfaceHi: '#1f1e1d',
    surfaceHover: '#252320',
    border: '#2e2b28',
    borderHi: '#50453b',
    primary: '#f1bc8b',
    secondary: '#e9c176',
    muted: '#9d8e83',
    outline: '#6b5f57',
    onBg: '#e5e2e1',
    success: '#4ade80',
    error: '#f87171',
    warning: '#fbbf24',
    info: '#60a5fa',
};

const DIFF = {
    EASY:   { color: '#4ade80', bg: 'rgba(74,222,128,0.08)'  },
    MEDIUM: { color: '#fbbf24', bg: 'rgba(251,191,36,0.08)'  },
    HARD:   { color: '#f87171', bg: 'rgba(248,113,113,0.08)' },
};

const OUTCOME = {
    USER_A_WIN: { label: 'A Wins',    color: '#4ade80', bg: 'rgba(74,222,128,0.1)'   },
    USER_B_WIN: { label: 'B Wins',    color: '#60a5fa', bg: 'rgba(96,165,250,0.1)'   },
    DRAW:       { label: 'Draw',      color: '#fbbf24', bg: 'rgba(251,191,36,0.1)'   },
    ABANDONED:  { label: 'Abandoned', color: '#6b5f57', bg: 'rgba(107,95,87,0.15)'   },
};

const PAGE_SIZE = 8;

/* ── tiny helpers ── */
const DiffBadge = ({ diff }) => {
    const d = DIFF[diff] || { color: C.muted, bg: 'rgba(157,142,131,0.1)' };
    return (
        <span style={{
            padding: '2px 9px', fontSize: '10px', fontWeight: 700,
            letterSpacing: '0.12em', textTransform: 'uppercase',
            color: d.color, background: d.bg,
            border: `1px solid ${d.color}30`,
            fontFamily: "'JetBrains Mono', monospace",
        }}>{diff || '—'}</span>
    );
};

const OutcomePill = ({ outcome }) => {
    const o = OUTCOME[outcome] || { label: outcome || 'FINISHED', color: C.success, bg: 'rgba(74,222,128,0.1)' };
    return (
        <span style={{
            padding: '3px 11px', fontSize: '10px', fontWeight: 700,
            letterSpacing: '0.1em', textTransform: 'uppercase',
            color: o.color, background: o.bg,
            border: `1px solid ${o.color}40`,
            fontFamily: "'JetBrains Mono', monospace",
        }}>{o.label}</span>
    );
};

const Avatar = ({ name, size = 36, winner }) => {
    const initials = (name || '?').slice(0, 2).toUpperCase();
    const hue = [...(name || '')].reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
    return (
        <div style={{
            width: size, height: size, borderRadius: '50%',
            background: `hsl(${hue},35%,22%)`,
            border: `2px solid ${winner ? C.success : `hsl(${hue},30%,35%)`}`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: size * 0.36, fontWeight: 700, color: `hsl(${hue},60%,75%)`,
            flexShrink: 0, boxShadow: winner ? `0 0 10px ${C.success}40` : 'none',
            fontFamily: "'JetBrains Mono', monospace",
        }}>{initials}</div>
    );
};

/* ── Match History Card ── */
const HistoryCard = ({ match }) => {
    const aWon = match.outcome === 'USER_A_WIN';
    const bWon = match.outcome === 'USER_B_WIN';
    const isDraw = match.outcome === 'DRAW';
    const isAbandoned = match.outcome === 'ABANDONED';

    const accentColor = isAbandoned ? C.outline : isDraw ? C.warning : aWon ? C.success : C.info;

    const formatDate = (d) => d ? new Date(d).toLocaleString('en-IN', {
        day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
    }) : '—';

    return (
        <div className="duel-card" style={{
            background: C.surface,
            border: `1px solid ${C.border}`,
            borderTop: `2px solid ${accentColor}`,
            padding: '0',
            overflow: 'hidden',
            transition: 'border-color 0.2s, background 0.2s',
        }}>
            {/* top bar */}
            <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '10px 16px',
                borderBottom: `1px solid ${C.border}`,
                background: C.surfaceHi,
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.muted }}>swords</span>
                    <span style={{ fontSize: '11px', color: C.muted, fontFamily: "'JetBrains Mono', monospace", letterSpacing: '0.08em' }}>
                        {formatDate(match.startedAt)}
                    </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <DiffBadge diff={match.difficulty} />
                    <OutcomePill outcome={match.outcome} />
                </div>
            </div>

            {/* battle row */}
            <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 56px 1fr',
                alignItems: 'center',
                padding: '16px 20px',
                gap: '8px',
            }}>
                {/* Player A */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <Avatar name={match.userAUsername} size={40} winner={aWon} />
                    <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            {aWon && (
                                <span className="material-symbols-outlined" style={{
                                    fontSize: '14px', color: C.success,
                                    fontVariationSettings: "'FILL' 1",
                                }}>military_tech</span>
                            )}
                            <span style={{
                                fontSize: '15px', fontWeight: aWon ? 700 : 500,
                                color: aWon ? C.success : isDraw ? C.warning : isAbandoned ? C.muted : C.onBg,
                            }}>
                                {match.userAUsername || `User ${match.userAId}`}
                            </span>
                        </div>
                        <span style={{ fontSize: '11px', color: C.outline, fontFamily: "'JetBrains Mono', monospace" }}>
                            Player A
                        </span>
                    </div>
                </div>

                {/* VS center */}
                <div style={{ textAlign: 'center' }}>
                    <div style={{
                        width: '40px', height: '40px', margin: '0 auto',
                        border: `1px solid ${C.borderHi}`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        background: C.surfaceHi,
                        transform: 'rotate(45deg)',
                    }}>
                        <span style={{
                            transform: 'rotate(-45deg)',
                            fontSize: '10px', fontWeight: 800,
                            color: C.outline, letterSpacing: '0.05em',
                            fontFamily: "'JetBrains Mono', monospace",
                        }}>VS</span>
                    </div>
                </div>

                {/* Player B */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', justifyContent: 'flex-end', flexDirection: 'row-reverse' }}>
                    <Avatar name={match.userBUsername} size={40} winner={bWon} />
                    <div style={{ textAlign: 'right' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', justifyContent: 'flex-end' }}>
                            <span style={{
                                fontSize: '15px', fontWeight: bWon ? 700 : 500,
                                color: bWon ? C.info : isDraw ? C.warning : isAbandoned ? C.muted : C.onBg,
                            }}>
                                {match.userBUsername || `User ${match.userBId}`}
                            </span>
                            {bWon && (
                                <span className="material-symbols-outlined" style={{
                                    fontSize: '14px', color: C.info,
                                    fontVariationSettings: "'FILL' 1",
                                }}>military_tech</span>
                            )}
                        </div>
                        <span style={{ fontSize: '11px', color: C.outline, fontFamily: "'JetBrains Mono', monospace" }}>
                            Player B
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};

/* ── Live Match Card ── */
const LiveCard = ({ match, onCancel }) => {
    const formatTime = (s) => {
        if (!s || s < 0) return '0:00';
        return `${Math.floor(s / 60)}:${(s % 60).toString().padStart(2, '0')}`;
    };
    const urgent = match.remainingSeconds < 300;

    return (
        <div style={{
            background: C.surface,
            border: `1px solid ${C.success}40`,
            borderTop: `2px solid ${C.success}`,
            overflow: 'hidden',
        }}>
            {/* top bar */}
            <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '8px 16px', background: `${C.success}08`,
                borderBottom: `1px solid ${C.success}20`,
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: C.success, display: 'inline-block', animation: 'pulse 1.5s infinite' }} />
                    <span style={{ fontSize: '11px', color: C.success, fontFamily: "'JetBrains Mono', monospace", letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        Live
                    </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <DiffBadge diff={match.difficulty} />
                    <button onClick={() => onCancel(match.matchId)} style={{
                        padding: '3px 10px', background: 'transparent',
                        border: `1px solid ${C.error}60`, color: C.error,
                        cursor: 'pointer', fontSize: '10px',
                        letterSpacing: '0.1em', textTransform: 'uppercase',
                        fontFamily: "'JetBrains Mono', monospace",
                    }}>Cancel</button>
                </div>
            </div>

            {/* battle row */}
            <div style={{
                display: 'grid', gridTemplateColumns: '1fr 80px 1fr',
                alignItems: 'center', padding: '16px 20px', gap: '8px',
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <Avatar name={match.userAUsername} size={40} />
                    <div>
                        <div style={{ fontSize: '15px', fontWeight: 600, color: C.primary }}>
                            {match.userAUsername || `User ${match.userAId}`}
                        </div>
                        <span style={{ fontSize: '11px', color: C.outline, fontFamily: "'JetBrains Mono', monospace" }}>
                            {match.runsUsed ?? 0}/5 runs
                        </span>
                    </div>
                </div>

                <div style={{ textAlign: 'center' }}>
                    <div style={{
                        fontSize: '22px', fontWeight: 700,
                        fontFamily: "'JetBrains Mono', monospace",
                        color: urgent ? C.error : C.onBg,
                        animation: urgent ? 'blink 1s infinite' : 'none',
                    }}>
                        {formatTime(match.remainingSeconds)}
                    </div>
                    <div style={{ fontSize: '9px', color: C.outline, letterSpacing: '0.1em', textTransform: 'uppercase', marginTop: '2px' }}>
                        remaining
                    </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', justifyContent: 'flex-end', flexDirection: 'row-reverse' }}>
                    <Avatar name={match.userBUsername} size={40} />
                    <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '15px', fontWeight: 600, color: C.primary }}>
                            {match.userBUsername || `User ${match.userBId}`}
                        </div>
                        <span style={{ fontSize: '11px', color: C.outline, fontFamily: "'JetBrains Mono', monospace" }}>
                            {match.runsUsed ?? 0}/5 runs
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};

/* ── Main Page ── */
const AdminDuelMonitor = () => {
    const [liveMatches, setLiveMatches]     = useState([]);
    const [historyMatches, setHistoryMatches] = useState([]);
    const [loading, setLoading]             = useState(true);
    const [error, setError]                 = useState(null);
    const [historyTotal, setHistoryTotal]   = useState(0);
    const [page, setPage]                   = useState(0);

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
        } catch (err) {
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

    if (loading) return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh', color: C.muted }}>
            Loading duel monitor...
        </div>
    );
    if (error) return (
        <div style={{ padding: '2rem', color: C.error }}>
            <h2>Access Error</h2><p>{error}</p>
        </div>
    );

    return (
        <div style={{ padding: '1.5rem 2rem', color: C.onBg, maxWidth: '900px' }}>

            {/* ── Header ── */}
            <div style={{ marginBottom: '2.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '6px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '28px', color: C.secondary, fontVariationSettings: "'FILL' 1" }}>swords</span>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '30px', color: C.primary, margin: 0 }}>
                        Duel Monitor
                    </h1>
                </div>
                <p style={{ color: C.muted, fontSize: '13px', marginLeft: '40px' }}>
                    Live matches auto-refresh every 5 seconds
                </p>
            </div>

            {/* ── Live Matches ── */}
            <section style={{ marginBottom: '3rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem' }}>
                    <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: C.success, display: 'inline-block', animation: 'pulse 1.5s infinite' }} />
                    <h2 style={{ margin: 0, fontSize: '13px', color: C.success, fontFamily: "'JetBrains Mono', monospace", letterSpacing: '0.12em', textTransform: 'uppercase' }}>
                        Live Matches
                    </h2>
                    <span style={{
                        padding: '1px 8px', fontSize: '11px', fontWeight: 700,
                        background: `${C.success}15`, color: C.success,
                        border: `1px solid ${C.success}30`,
                        fontFamily: "'JetBrains Mono', monospace",
                    }}>{liveMatches.length}</span>
                </div>

                {liveMatches.length === 0 ? (
                    <div style={{
                        padding: '2.5rem', background: C.surface,
                        border: `1px dashed ${C.border}`,
                        textAlign: 'center', color: C.outline,
                        fontSize: '14px',
                    }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '32px', display: 'block', marginBottom: '8px', color: C.border }}>sports_kabaddi</span>
                        No matches in progress right now
                    </div>
                ) : (
                    <div style={{ display: 'grid', gap: '10px' }}>
                        {liveMatches.map(m => <LiveCard key={m.matchId} match={m} onCancel={cancelMatch} />)}
                    </div>
                )}
            </section>

            {/* ── Match History ── */}
            <section>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.muted }}>history</span>
                        <h2 style={{ margin: 0, fontSize: '13px', color: C.muted, fontFamily: "'JetBrains Mono', monospace", letterSpacing: '0.12em', textTransform: 'uppercase' }}>
                            Match History
                        </h2>
                    </div>
                    {historyTotal > 0 && (
                        <span style={{ fontSize: '12px', color: C.outline, fontFamily: "'JetBrains Mono', monospace" }}>
                            {historyTotal} matches · page {page + 1}/{totalPages}
                        </span>
                    )}
                </div>

                {historyMatches.length === 0 ? (
                    <div style={{ padding: '2.5rem', background: C.surface, border: `1px dashed ${C.border}`, textAlign: 'center', color: C.outline, fontSize: '14px' }}>
                        No completed matches yet
                    </div>
                ) : (
                    <div style={{ display: 'grid', gap: '8px' }}>
                        {historyMatches.map(m => <HistoryCard key={m.matchId} match={m} />)}
                    </div>
                )}

                {/* ── Pagination ── */}
                {totalPages > 1 && (
                    <div style={{ marginTop: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }}>
                        <button
                            onClick={() => setPage(p => Math.max(0, p - 1))}
                            disabled={page === 0}
                            style={{
                                width: '34px', height: '34px', background: 'transparent',
                                border: `1px solid ${page === 0 ? C.border : C.borderHi}`,
                                color: page === 0 ? C.border : C.muted,
                                cursor: page === 0 ? 'not-allowed' : 'pointer',
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                            }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>chevron_left</span>
                        </button>

                        {Array.from({ length: totalPages }, (_, i) => {
                            const near = Math.abs(i - page) <= 1;
                            const edge = i === 0 || i === totalPages - 1;
                            if (!near && !edge) {
                                if (i === 1 || i === totalPages - 2) return <span key={i} style={{ color: C.outline, fontSize: '13px', padding: '0 2px' }}>…</span>;
                                return null;
                            }
                            const active = i === page;
                            return (
                                <button key={i} onClick={() => setPage(i)} style={{
                                    width: '34px', height: '34px',
                                    background: active ? `${C.secondary}18` : 'transparent',
                                    border: `1px solid ${active ? C.secondary : C.border}`,
                                    color: active ? C.secondary : C.muted,
                                    cursor: 'pointer', fontSize: '12px',
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontWeight: active ? 700 : 400,
                                }}>{i + 1}</button>
                            );
                        })}

                        <button
                            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                            disabled={page >= totalPages - 1}
                            style={{
                                width: '34px', height: '34px', background: 'transparent',
                                border: `1px solid ${page >= totalPages - 1 ? C.border : C.borderHi}`,
                                color: page >= totalPages - 1 ? C.border : C.muted,
                                cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                            }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>chevron_right</span>
                        </button>
                    </div>
                )}
            </section>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                @keyframes pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.5;transform:scale(1.3)} }
                @keyframes blink  { 0%,100%{opacity:1} 50%{opacity:0.3} }
                .duel-card:hover { background: ${C.surfaceHover} !important; border-color: ${C.borderHi} !important; }
            `}</style>
        </div>
    );
};

export default AdminDuelMonitor;
