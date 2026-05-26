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

const STATUS_COLORS = {
    WAITING: C.warning,
    IN_PROGRESS: C.primary,
    FINISHED: C.success,
};

const AdminDuelMonitor = () => {
    const [liveMatches, setLiveMatches] = useState([]);
    const [historyMatches, setHistoryMatches] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [historyLimit, setHistoryLimit] = useState(50);
    const [historyOffset, setHistoryOffset] = useState(0);
    const [historyTotal, setHistoryTotal] = useState(0);

    const fetchMatches = useCallback(async () => {
        try {
            const [liveRes, historyRes] = await Promise.all([
                api.get('/admin/duels', {
                    params: { status: 'IN_PROGRESS', limit: 100, offset: 0 }
                }),
                api.get('/admin/duels', {
                    params: { status: 'FINISHED', limit: historyLimit, offset: historyOffset }
                }),
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
    }, [historyLimit, historyOffset]);

    useEffect(() => {
        fetchMatches();
        const interval = setInterval(fetchMatches, 5000); // refresh every 5s
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
            day: '2-digit',
            month: 'short',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    if (loading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh', color: C.muted }}>
                Loading duel monitor...
            </div>
        );
    }

    if (error) {
        return (
            <div style={{ padding: '2rem', color: C.error }}>
                <h2>Access Error</h2>
                <p>{error}</p>
            </div>
        );
    }

    return (
        <div style={{ padding: '1.5rem', color: C.onBg }}>
            {/* Header */}
            <div style={{ marginBottom: '2rem' }}>
                <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', color: C.primary, marginBottom: '0.5rem' }}>
                    Duel Monitor
                </h1>
                <p style={{ color: C.muted, fontSize: '14px' }}>
                    Live matches refresh every 5 seconds
                </p>
            </div>

            {/* Live Matches */}
            <section style={{ marginBottom: '3rem' }}>
                <h2 style={{ fontSize: '18px', color: C.secondary, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '20px', color: C.success }}>circle</span>
                    Live Matches ({liveMatches.length})
                </h2>

                {liveMatches.length === 0 ? (
                    <div style={{ padding: '2rem', backgroundColor: C.surface, borderRadius: '8px', border: `1px solid ${C.border}`, textAlign: 'center', color: C.muted }}>
                        No matches in progress right now
                    </div>
                ) : (
                    <div style={{ display: 'grid', gap: '12px' }}>
                        {liveMatches.map((match) => (
                            <div key={match.matchId} style={{
                                backgroundColor: C.surface,
                                border: `1px solid ${C.border}`,
                                borderRadius: '8px',
                                padding: '1rem 1.25rem',
                                display: 'grid',
                                gridTemplateColumns: '1fr auto 1fr auto',
                                alignItems: 'center',
                                gap: '1rem',
                            }}>
                                {/* Player A */}
                                <div>
                                    <span style={{ color: C.primary, fontWeight: 600 }}>{match.userAUsername || `User ${match.userAId}`}</span>
                                    <span style={{ color: C.muted, marginLeft: '8px', fontSize: '12px' }}>
                                        {match.runsUsed ?? 0}/5 runs
                                    </span>
                                </div>

                                {/* VS + Timer */}
                                <div style={{ textAlign: 'center', minWidth: '100px' }}>
                                    <div style={{ fontSize: '12px', color: C.muted, marginBottom: '2px' }}>VS</div>
                                    <div style={{
                                        fontSize: '20px',
                                        fontFamily: "'JetBrains Mono', monospace",
                                        color: match.remainingSeconds < 300 ? C.error : C.onBg,
                                    }}>
                                        {formatTime(match.remainingSeconds)}
                                    </div>
                                    <div style={{
                                        display: 'inline-block',
                                        padding: '2px 8px',
                                        borderRadius: '4px',
                                        fontSize: '10px',
                                        letterSpacing: '0.08em',
                                        textTransform: 'uppercase',
                                        backgroundColor: DIFFICULTY_COLORS[match.difficulty] + '20',
                                        color: DIFFICULTY_COLORS[match.difficulty],
                                        marginTop: '4px',
                                    }}>
                                        {match.difficulty}
                                    </div>
                                </div>

                                {/* Player B */}
                                <div style={{ textAlign: 'right' }}>
                                    <span style={{ color: C.primary, fontWeight: 600 }}>{match.userBUsername || `User ${match.userBId}`}</span>
                                    <span style={{ color: C.muted, marginLeft: '8px', fontSize: '12px' }}>
                                        {match.runsUsed ?? 0}/5 runs
                                    </span>
                                </div>

                                {/* Cancel Button */}
                                <button
                                    onClick={() => cancelMatch(match.matchId)}
                                    style={{
                                        padding: '6px 12px',
                                        backgroundColor: 'transparent',
                                        border: `1px solid ${C.error}`,
                                        color: C.error,
                                        borderRadius: '4px',
                                        cursor: 'pointer',
                                        fontSize: '11px',
                                        textTransform: 'uppercase',
                                        letterSpacing: '0.08em',
                                    }}
                                >
                                    Cancel
                                </button>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {/* Match History */}
            <section>
                <h2 style={{ fontSize: '18px', color: C.secondary, marginBottom: '1rem' }}>
                    Match History
                </h2>

                {historyMatches.length === 0 ? (
                    <div style={{ padding: '2rem', backgroundColor: C.surface, borderRadius: '8px', border: `1px solid ${C.border}`, textAlign: 'center', color: C.muted }}>
                        No completed matches yet
                    </div>
                ) : (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                            <thead>
                                <tr style={{ borderBottom: `1px solid ${C.border}` }}>
                                    <th style={{ textAlign: 'left', padding: '12px 8px', color: C.muted, fontWeight: 500 }}>Started</th>
                                    <th style={{ textAlign: 'left', padding: '12px 8px', color: C.muted, fontWeight: 500 }}>Player A</th>
                                    <th style={{ textAlign: 'center', padding: '12px 8px', color: C.muted, fontWeight: 500 }}>VS</th>
                                    <th style={{ textAlign: 'left', padding: '12px 8px', color: C.muted, fontWeight: 500 }}>Player B</th>
                                    <th style={{ textAlign: 'center', padding: '12px 8px', color: C.muted, fontWeight: 500 }}>Level</th>
                                    <th style={{ textAlign: 'center', padding: '12px 8px', color: C.muted, fontWeight: 500 }}>Winner</th>
                                    <th style={{ textAlign: 'center', padding: '12px 8px', color: C.muted, fontWeight: 500 }}>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {historyMatches.map((match) => (
                                    <tr key={match.matchId} style={{ borderBottom: `1px solid ${C.border}` }}>
                                        <td style={{ padding: '12px 8px', color: C.muted, whiteSpace: 'nowrap' }}>
                                            {formatDate(match.startedAt)}
                                        </td>
                                        <td style={{ padding: '12px 8px', color: match.winnerUserId === match.userAId ? C.success : C.onBg, fontWeight: match.winnerUserId === match.userAId ? 600 : 400 }}>
                                            {match.userAUsername || `User ${match.userAId}`}
                                        </td>
                                        <td style={{ padding: '12px 8px', textAlign: 'center', color: C.outline }}>vs</td>
                                        <td style={{ padding: '12px 8px', color: match.winnerUserId === match.userBId ? C.success : C.onBg, fontWeight: match.winnerUserId === match.userBId ? 600 : 400 }}>
                                            {match.userBUsername || `User ${match.userBId}`}
                                        </td>
                                        <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                                            <span style={{
                                                padding: '2px 8px',
                                                borderRadius: '4px',
                                                fontSize: '10px',
                                                letterSpacing: '0.08em',
                                                textTransform: 'uppercase',
                                                backgroundColor: DIFFICULTY_COLORS[match.difficulty] + '20',
                                                color: DIFFICULTY_COLORS[match.difficulty],
                                            }}>
                                                {match.difficulty || '-'}
                                            </span>
                                        </td>
                                        <td style={{ padding: '12px 8px', textAlign: 'center', color: C.success, fontWeight: 600 }}>
                                            {match.winnerUsername || (match.outcome === 'DRAW' ? 'Draw' : '-')}
                                        </td>
                                        <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                                            {match.outcome === 'DRAW' ? (
                                                <span style={{
                                                    padding: '2px 8px',
                                                    borderRadius: '4px',
                                                    fontSize: '10px',
                                                    letterSpacing: '0.08em',
                                                    textTransform: 'uppercase',
                                                    backgroundColor: C.warning + '20',
                                                    color: C.warning,
                                                }}>
                                                    DRAW
                                                </span>
                                            ) : match.outcome === 'ABANDONED' ? (
                                                <span style={{
                                                    padding: '2px 8px',
                                                    borderRadius: '4px',
                                                    fontSize: '10px',
                                                    letterSpacing: '0.08em',
                                                    textTransform: 'uppercase',
                                                    backgroundColor: C.outline + '20',
                                                    color: C.outline,
                                                }}>
                                                    ABANDONED
                                                </span>
                                            ) : (
                                                <span style={{
                                                    padding: '2px 8px',
                                                    borderRadius: '4px',
                                                    fontSize: '10px',
                                                    letterSpacing: '0.08em',
                                                    textTransform: 'uppercase',
                                                    backgroundColor: C.success + '20',
                                                    color: C.success,
                                                }}>
                                                    {match.outcome || 'FINISHED'}
                                                </span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* Pagination for history */}
                {historyTotal > historyLimit && (
                    <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'center', gap: '8px' }}>
                        <button
                            onClick={() => setHistoryOffset(Math.max(0, historyOffset - historyLimit))}
                            disabled={historyOffset === 0}
                            style={{
                                padding: '8px 16px',
                                backgroundColor: historyOffset === 0 ? C.surface : C.surfaceHi,
                                border: `1px solid ${C.border}`,
                                color: historyOffset === 0 ? C.outline : C.onBg,
                                borderRadius: '4px',
                                cursor: historyOffset === 0 ? 'not-allowed' : 'pointer',
                            }}
                        >
                            Previous
                        </button>
                        <span style={{ padding: '8px 16px', color: C.muted }}>
                            {Math.floor(historyOffset / historyLimit) + 1} of {Math.ceil(historyTotal / historyLimit)}
                        </span>
                        <button
                            onClick={() => setHistoryOffset(historyOffset + historyLimit)}
                            disabled={historyOffset + historyLimit >= historyTotal}
                            style={{
                                padding: '8px 16px',
                                backgroundColor: historyOffset + historyLimit >= historyTotal ? C.surface : C.surfaceHi,
                                border: `1px solid ${C.border}`,
                                color: historyOffset + historyLimit >= historyTotal ? C.outline : C.onBg,
                                borderRadius: '4px',
                                cursor: historyOffset + historyLimit >= historyTotal ? 'not-allowed' : 'pointer',
                            }}
                        >
                            Next
                        </button>
                    </div>
                )}
            </section>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                @media (max-width: 768px) {
                    section > div > div {
                        grid-template-columns: 1fr !important;
                    }
                    table {
                        font-size: 11px;
                    }
                    th, td {
                        padding: 8px 4px !important;
                    }
                }
            `}</style>
        </div>
    );
};

export default AdminDuelMonitor;
