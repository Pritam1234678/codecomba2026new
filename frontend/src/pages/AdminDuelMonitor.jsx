import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';

const C = {
    bg: '#0e0e0e',
    surface: '#1c1b1b',
    surfaceHi: '#2a2a2a',
    border: '#50453b',
    primary: '#f1bc8b',
    secondary: '#e9c176',
    muted: '#d4c4b7',
    outline: '#9d8e83',
    onBg: '#e5e2e1',
    success: '#4ade80',
    error: '#ffb4ab',
    warning: '#fbbf24',
};

const DIFFICULTY_COLORS = {
    EASY: '#4ade80',
    MEDIUM: '#fbbf24',
    HARD: '#f87171',
};

const PAGE_SIZE = 10;

const AdminDuelMonitor = () => {
    const [liveMatches, setLiveMatches] = useState([]);
    const [historyMatches, setHistoryMatches] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [historyTotal, setHistoryTotal] = useState(0);
    const [page, setPage] = useState(0);

    const fetchMatches = useCallback(async () => {
        try {
            const [liveRes, historyRes] = await Promise.all([
                api.get('/admin/duels', { params: { status: 'IN_PROGRESS', limit: 100, offset: 0 } }),
                api.get('/admin/duels', { params: { status: 'FINISHED', limit: PAGE_SIZE, offset: page * PAGE_SIZE } }),
            ]);
            setLiveMatches(liveRes.data.content || []);
            setHistoryMatches(historyRes.data.content || []);
            setHistoryTotal(historyRes.data.totalElements || 0);
            setError(null);
        } catch (err) {
            console.error('Failed to fetch duel matches:', err);
            setError('Failed to load matches. You may not have admin access.');
        } finally {
            setLoading(false);
        }
    }, [page]);

    useEffect(() => {
        fetchMatches();
        const interval = setInterval(fetchMatches, 5000);
        return () => clearInterval(interval);
    }, [fetchMatches]);

    const cancelMatch = async (matchId) => {
        if (!window.confirm('Cancel this match? It will be marked as ABANDONED.')) return;
        try {
            await api.post(`/admin/duels/${matchId}/cancel`);
            fetchMatches();
        } catch (err) {
            alert('Failed to cancel match: ' + (err.response?.data?.message || err.message));
        }
    };

    const formatTime = (seconds) => {
        if (!seconds || seconds < 0) return '0:00';
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${m}:${s.toString().padStart(2, '0')}`;
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '-';
        return new Date(dateStr).toLocaleString('en-IN', {
            day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
        });
    };

    const totalPages = Math.ceil(historyTotal / PAGE_SIZE);

    const OutcomeBadge = ({ outcome }) => {
        const cfg = outcome === 'DRAW'
            ? { bg: C.warning + '22', color: C.warning, label: 'DRAW' }
            : outcome === 'ABANDONED'
            ? { bg: C.outline + '22', color: C.outline, label: 'ABANDONED' }
            : { bg: C.success + '22', color: C.success, label: outcome || 'FINISHED' };
        return (
            <span style={{
                padding: '3px 10px', borderRadius: '4px', fontSize: '10px',
                letterSpacing: '0.1em', textTransform: 'uppercase',
                backgroundColor: cfg.bg, color: cfg.color, fontWeight: 600,
            }}>{cfg.label}</span>
        );
    };

    const DiffBadge = ({ diff }) => (
        <span style={{
            padding: '3px 10px', borderRadius: '4px', fontSize: '10px',
            letterSpacing: '0.1em', textTransform: 'uppercase',
            backgroundColor: (DIFFICULTY_COLORS[diff] || C.outline) + '22',
            color: DIFFICULTY_COLORS[diff] || C.outline, fontWeight: 600,
        }}>{diff || '—'}</span>
    );

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
        <div style={{ padding: '1.5rem', color: C.onBg, maxWidth: '1100px' }}>
            {/* Header */}
            <div style={{ marginBottom: '2rem' }}>
                <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', color: C.primary, marginBottom: '0.5rem' }}>
                    Duel Monitor
                </h1>
                <p style={{ color: C.muted, fontSize: '14px' }}>Live matches refresh every 5 seconds</p>
            </div>

            {/* ── Live Matches ── */}
            <section style={{ marginBottom: '3rem' }}>
                <h2 style={{ fontSize: '16px', color: C.secondary, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '8px', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '18px', color: C.success }}>circle</span>
                    Live Matches ({liveMatches.length})
                </h2>

                {liveMatches.length === 0 ? (
                    <div style={{ padding: '2rem', backgroundColor: C.surface, border: `1px solid ${C.border}`, textAlign: 'center', color: C.muted }}>
                        No matches in progress right now
                    </div>
                ) : (
                    <div style={{ display: 'grid', gap: '12px' }}>
                        {liveMatches.map((match) => (
                            <div key={match.matchId} style={{
                                backgroundColor: C.surface, border: `1px solid ${C.border}`,
                                padding: '1rem 1.25rem',
                                display: 'grid', gridTemplateColumns: '1fr auto 1fr auto',
                                alignItems: 'center', gap: '1rem',
                            }}>
                                <div>
                                    <span style={{ color: C.primary, fontWeight: 600 }}>{match.userAUsername || `User ${match.userAId}`}</span>
                                    <span style={{ color: C.muted, marginLeft: '8px', fontSize: '12px' }}>{match.runsUsed ?? 0}/5 runs</span>
                                </div>
                                <div style={{ textAlign: 'center', minWidth: '100px' }}>
                                    <div style={{ fontSize: '12px', color: C.muted, marginBottom: '2px' }}>VS</div>
                                    <div style={{ fontSize: '20px', fontFamily: "'JetBrains Mono', monospace", color: match.remainingSeconds < 300 ? C.error : C.onBg }}>
                                        {formatTime(match.remainingSeconds)}
                                    </div>
                                    <div style={{ marginTop: '4px' }}><DiffBadge diff={match.difficulty} /></div>
                                </div>
                                <div style={{ textAlign: 'right' }}>
                                    <span style={{ color: C.primary, fontWeight: 600 }}>{match.userBUsername || `User ${match.userBId}`}</span>
                                    <span style={{ color: C.muted, marginLeft: '8px', fontSize: '12px' }}>{match.runsUsed ?? 0}/5 runs</span>
                                </div>
                                <button onClick={() => cancelMatch(match.matchId)} style={{
                                    padding: '6px 12px', backgroundColor: 'transparent',
                                    border: `1px solid ${C.error}`, color: C.error,
                                    cursor: 'pointer', fontSize: '11px',
                                    textTransform: 'uppercase', letterSpacing: '0.08em',
                                }}>Cancel</button>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {/* ── Match History ── */}
            <section>
                <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
                    <h2 style={{ fontSize: '16px', color: C.secondary, letterSpacing: '0.05em', textTransform: 'uppercase', margin: 0 }}>
                        Match History
                    </h2>
                    {historyTotal > 0 && (
                        <span style={{ fontSize: '12px', color: C.outline, fontFamily: "'JetBrains Mono', monospace" }}>
                            {historyTotal} total
                        </span>
                    )}
                </div>

                {historyMatches.length === 0 ? (
                    <div style={{ padding: '2rem', backgroundColor: C.surface, border: `1px solid ${C.border}`, textAlign: 'center', color: C.muted }}>
                        No completed matches yet
                    </div>
                ) : (
                    <div style={{ display: 'grid', gap: '10px' }}>
                        {historyMatches.map((match) => {
                            const aWon = match.winnerUserId === match.userAId;
                            const bWon = match.winnerUserId === match.userBId;
                            const isDraw = match.outcome === 'DRAW';
                            const isAbandoned = match.outcome === 'ABANDONED';

                            return (
                                <div key={match.matchId} style={{
                                    backgroundColor: C.surface,
                                    border: `1px solid ${C.border}`,
                                    borderLeft: `3px solid ${
                                        isAbandoned ? C.outline :
                                        isDraw ? C.warning :
                                        C.success
                                    }`,
                                    padding: '14px 18px',
                                    display: 'grid',
                                    gridTemplateColumns: '1fr 60px 1fr auto',
                                    alignItems: 'center',
                                    gap: '12px',
                                    transition: 'background 0.15s',
                                }}>
                                    {/* Player A */}
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                            {aWon && <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.success, fontVariationSettings: "'FILL' 1" }}>military_tech</span>}
                                            <span style={{
                                                fontWeight: aWon ? 700 : 400,
                                                color: aWon ? C.success : C.onBg,
                                                fontSize: '14px',
                                            }}>{match.userAUsername || `User ${match.userAId}`}</span>
                                        </div>
                                        <span style={{ fontSize: '11px', color: C.outline, fontFamily: "'JetBrains Mono', monospace" }}>Player A</span>
                                    </div>

                                    {/* Center: VS + diff */}
                                    <div style={{ textAlign: 'center' }}>
                                        <div style={{ fontSize: '11px', color: C.outline, letterSpacing: '0.1em', marginBottom: '4px' }}>VS</div>
                                        <DiffBadge diff={match.difficulty} />
                                    </div>

                                    {/* Player B */}
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'flex-end' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                            <span style={{
                                                fontWeight: bWon ? 700 : 400,
                                                color: bWon ? C.success : C.onBg,
                                                fontSize: '14px',
                                            }}>{match.userBUsername || `User ${match.userBId}`}</span>
                                            {bWon && <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.success, fontVariationSettings: "'FILL' 1" }}>military_tech</span>}
                                        </div>
                                        <span style={{ fontSize: '11px', color: C.outline, fontFamily: "'JetBrains Mono', monospace" }}>Player B</span>
                                    </div>

                                    {/* Right: outcome + date */}
                                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '6px', minWidth: '110px' }}>
                                        <OutcomeBadge outcome={match.outcome} />
                                        <span style={{ fontSize: '11px', color: C.outline, fontFamily: "'JetBrains Mono', monospace", whiteSpace: 'nowrap' }}>
                                            {formatDate(match.startedAt)}
                                        </span>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}

                {/* Pagination */}
                {totalPages > 1 && (
                    <div style={{ marginTop: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', flexWrap: 'wrap' }}>
                        <button
                            onClick={() => setPage(p => Math.max(0, p - 1))}
                            disabled={page === 0}
                            style={{
                                padding: '6px 12px', backgroundColor: 'transparent',
                                border: `1px solid ${page === 0 ? C.border : C.outline}`,
                                color: page === 0 ? C.border : C.outline,
                                cursor: page === 0 ? 'not-allowed' : 'pointer', fontSize: '13px',
                            }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '16px', verticalAlign: 'middle' }}>chevron_left</span>
                        </button>

                        {Array.from({ length: totalPages }, (_, i) => {
                            const show = i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1;
                            const showDots = !show && (i === 1 || i === totalPages - 2);
                            if (showDots) return <span key={i} style={{ color: C.outline, padding: '0 2px' }}>…</span>;
                            if (!show) return null;
                            return (
                                <button key={i} onClick={() => setPage(i)} style={{
                                    width: '34px', height: '34px',
                                    backgroundColor: i === page ? C.secondary + '22' : 'transparent',
                                    border: `1px solid ${i === page ? C.secondary : C.border}`,
                                    color: i === page ? C.secondary : C.outline,
                                    cursor: 'pointer', fontSize: '13px',
                                    fontFamily: "'JetBrains Mono', monospace",
                                }}>{i + 1}</button>
                            );
                        })}

                        <button
                            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                            disabled={page >= totalPages - 1}
                            style={{
                                padding: '6px 12px', backgroundColor: 'transparent',
                                border: `1px solid ${page >= totalPages - 1 ? C.border : C.outline}`,
                                color: page >= totalPages - 1 ? C.border : C.outline,
                                cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer', fontSize: '13px',
                            }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '16px', verticalAlign: 'middle' }}>chevron_right</span>
                        </button>
                    </div>
                )}
            </section>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
            `}</style>
        </div>
    );
};

export default AdminDuelMonitor;
