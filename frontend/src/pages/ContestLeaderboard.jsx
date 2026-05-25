import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg:         '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceHi:  '#2a2a2a',
    surfaceMin: '#0e0e0e',
    border:     '#50453b',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    tertiary:   '#f4bb92',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
};

const ContestLeaderboard = () => {
    const { isMobile } = useResponsive();
    const { contestId } = useParams();
    const navigate      = useNavigate();
    const [contest,     setContest]     = useState(null);
    const [leaderboard, setLeaderboard] = useState([]);
    const [loading,     setLoading]     = useState(true);
    const [error,       setError]       = useState(null);
    const [search,      setSearch]      = useState('');

    useEffect(() => {
        setLoading(true);
        Promise.all([
            api.get(`/contests/${contestId}`),
            api.get(`/admin/leaderboard/contest/${contestId}`),
        ])
            .then(([cRes, lbRes]) => {
                setContest(cRes.data);
                setLeaderboard(lbRes.data);
            })
            .catch(err => setError(err.response?.status === 403 ? 'Access denied — Admin only' : 'Failed to load leaderboard'))
            .finally(() => setLoading(false));
    }, [contestId]);

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', letterSpacing: '0.1em' }}>
            Loading...
        </div>
    );

    if (error) return (
        <div style={{ padding: '48px 64px' }}>
            <div style={{ padding: '12px 16px', border: `1px solid ${C.error}`, borderLeft: `3px solid ${C.error}`, backgroundColor: 'rgba(255,180,171,0.06)', marginBottom: '1.5rem', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                {error}
            </div>
            <button onClick={() => navigate('/admin/leaderboard')} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.primary, background: 'none', border: 'none', cursor: 'pointer', letterSpacing: '0.08em' }}>
                ← Back to Leaderboard
            </button>
        </div>
    );

    const top3    = leaderboard.slice(0, 3);
    const rest    = leaderboard.slice(3);
    const filtered = rest.filter(e =>
        (e.userName || '').toLowerCase().includes(search.toLowerCase()) ||
        (e.userRoll || '').toLowerCase().includes(search.toLowerCase())
    );

    const totalParticipants = leaderboard.length;
    const avgScore = totalParticipants > 0
        ? (leaderboard.reduce((s, e) => s + (e.totalScore || 0), 0) / totalParticipants).toFixed(0)
        : 0;
    const highScore = top3[0]?.totalScore?.toFixed(0) || 0;

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", padding: isMobile ? '24px 16px' : '48px 64px' }}>

            {/* ── Header ── */}
            <motion.header
                initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '3rem', paddingBottom: '2rem', borderBottom: `1px solid ${C.border}` }}
            >
                <div>
                    <button
                        onClick={() => navigate('/admin/leaderboard')}
                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', textTransform: 'uppercase', marginBottom: '12px', display: 'block', transition: 'color 0.2s' }}
                        onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                        onMouseLeave={e => e.currentTarget.style.color = C.outline}
                    >
                        ← Back to Index
                    </button>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase', display: 'block', marginBottom: '8px' }}>
                        Final Results
                    </span>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: C.primary, marginBottom: '1.5rem' }}>
                        {contest?.name}
                    </h1>
                    {/* Stats strip */}
                    <div style={{ display: 'flex', gap: '0', borderLeft: `1px solid ${C.border}` }}>
                        {[
                            { label: 'Participants', value: totalParticipants },
                            { label: 'Average Score', value: avgScore },
                            { label: 'Highest Score', value: highScore, highlight: true },
                        ].map(({ label, value, highlight }) => (
                            <div key={label} style={{ padding: '0 24px', borderRight: `1px solid ${C.border}` }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>{label}</span>
                                <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 300, color: highlight ? C.secondary : C.onBg }}>{value}</span>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Search */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', width: '260px' }}>
                    <span className="material-symbols-outlined" style={{ color: C.outline, fontSize: '18px' }}>search</span>
                    <input
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        placeholder="SEARCH ARCHITECT ID..."
                        style={{ background: 'transparent', border: 'none', outline: 'none', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.08em', color: C.onBg, width: '100%' }}
                    />
                    <span style={{ color: C.primary, animation: 'blink 1s infinite', fontFamily: "'JetBrains Mono', monospace" }}>█</span>
                </div>
            </motion.header>

            {leaderboard.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '4rem', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline, border: `1px solid ${C.border}` }}>
                    No submissions yet for this contest
                </div>
            ) : (
                <>
                    {/* ── Hall of Excellence (Top 3 Podium) ── */}
                    {top3.length > 0 && (
                        <motion.section
                            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                            style={{ marginBottom: '3rem' }}
                        >
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '1rem' }}>
                                Hall of Excellence
                            </span>
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1px', backgroundColor: C.border }}>
                                {/* Reorder: 2nd, 1st, 3rd */}
                                {[top3[1], top3[0], top3[2]].map((entry, i) => {
                                    if (!entry) return <div key={i} style={{ backgroundColor: C.surfaceCon }} />;
                                    const isFirst = entry.rank === 1;
                                    const rankColors = ['#9d8e83', '#e9c176', '#f4bb92'];
                                    const rankColor  = rankColors[entry.rank - 1] || C.outline;
                                    return (
                                        <div
                                            key={entry.userId}
                                            style={{
                                                backgroundColor: isFirst ? C.surfaceLow : C.surfaceCon,
                                                padding: isFirst ? '3rem 2rem' : '2.5rem 2rem',
                                                display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center',
                                                position: 'relative', overflow: 'hidden',
                                                borderTop: isFirst ? `2px solid ${C.secondary}` : 'none',
                                            }}
                                        >
                                            {isFirst && (
                                                <div style={{ position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)', width: '80px', height: '1px', backgroundColor: C.secondary, boxShadow: `0 0 20px rgba(233,193,118,0.5)` }} />
                                            )}
                                            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: isFirst ? '48px' : '32px', fontWeight: 300, color: rankColor, marginBottom: '1rem', opacity: isFirst ? 1 : 0.6 }}>
                                                {String(entry.rank).padStart(2, '0')}
                                            </span>
                                            {/* Avatar circle */}
                                            <div style={{
                                                width: isFirst ? '80px' : '64px', height: isFirst ? '80px' : '64px',
                                                borderRadius: '50%',
                                                border: `${isFirst ? '2px' : '1px'} solid ${isFirst ? C.secondary : C.border}`,
                                                backgroundColor: C.surfaceMin,
                                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                marginBottom: '1rem',
                                            }}>
                                                <span style={{ fontFamily: "'Playfair Display', serif", fontSize: isFirst ? '28px' : '22px', fontWeight: 700, color: rankColor }}>
                                                    {(entry.userName || 'U').charAt(0).toUpperCase()}
                                                </span>
                                            </div>
                                            <h3 style={{ fontFamily: "'Geist', sans-serif", fontSize: isFirst ? '18px' : '15px', color: C.onBg, marginBottom: '4px' }}>
                                                {entry.userName || 'Unknown'}
                                            </h3>
                                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, marginBottom: '1.5rem' }}>
                                                {entry.userRoll || '—'}
                                            </p>
                                            <div style={{ borderTop: `1px solid ${isFirst ? C.secondary : C.border}`, paddingTop: '1rem', width: '100%' }}>
                                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: isFirst ? '16px' : '13px', color: isFirst ? C.secondary : C.muted, letterSpacing: '0.05em' }}>
                                                    {entry.totalScore?.toFixed(0)} PTS
                                                </span>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </motion.section>
                    )}

                    {/* ── Full Rankings Table ── */}
                    <motion.section
                        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.2 }}
                    >
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '1rem' }}>
                            Global Rankings
                        </span>
                        <div style={{ border: `1px solid ${C.border}`, overflow: 'hidden' }}>
                            {/* Table header */}
                            <div style={{ display: 'grid', gridTemplateColumns: '80px 1fr 140px 100px 120px', gap: '16px', padding: '14px 24px', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceHi }}>
                                {['Rank', 'Architect', 'Roll ID', 'Solved', 'Total Score'].map((h, i) => (
                                    <span key={h} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', textAlign: i >= 3 ? 'center' : 'left' }}>
                                        {h}
                                    </span>
                                ))}
                            </div>

                            {/* Top 3 in table */}
                            {top3.map((entry, i) => (
                                <TableRow key={entry.userId} entry={entry} index={i} isTop />
                            ))}

                            {/* Rest filtered */}
                            {filtered.map((entry, i) => (
                                <TableRow key={entry.userId} entry={entry} index={i + top3.length} />
                            ))}

                            {filtered.length === 0 && search && (
                                <div style={{ padding: '2rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline }}>
                                    No results for "{search}"
                                </div>
                            )}
                        </div>
                    </motion.section>
                </>
            )}

            <style>{`
                @keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
            `}</style>
        </div>
    );
};

/* ── Table Row ── */
const TableRow = ({ entry, index, isTop }) => {
    const [hovered, setHovered] = useState(false);
    const rankColors = { 1: '#e9c176', 2: '#9d8e83', 3: '#f4bb92' };
    const rankColor  = rankColors[entry.rank] || '#d4c4b7';

    return (
        <motion.div
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.03 }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'grid', gridTemplateColumns: '80px 1fr 140px 100px 120px',
                gap: '16px', padding: '18px 24px',
                borderBottom: `1px solid ${C.border}`,
                backgroundColor: hovered ? '#201f1f' : 'transparent',
                transition: 'background-color 0.2s',
            }}
        >
            {/* Rank */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: rankColor, fontWeight: isTop ? 600 : 400 }}>
                    {String(entry.rank).padStart(2, '0')}
                </span>
            </div>
            {/* Name */}
            <div style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.onBg }}>
                {entry.userName || 'Unknown'}
            </div>
            {/* Roll */}
            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline }}>
                {entry.userRoll || '—'}
            </div>
            {/* Solved */}
            <div style={{ textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.secondary }}>
                {entry.problemsSolved ?? '—'}
            </div>
            {/* Score */}
            <div style={{ textAlign: 'center' }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', color: C.primary, fontWeight: 600 }}>
                    {entry.totalScore?.toFixed(0)}
                </span>
            </div>
        </motion.div>
    );
};

export default ContestLeaderboard;
