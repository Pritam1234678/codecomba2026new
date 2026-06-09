import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
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
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
};

const Leaderboard = () => {
    const { isMobile } = useResponsive();
    const navigate = useNavigate();
    const [contests, setContests] = useState([]);
    const [loading,  setLoading]  = useState(true);
    const [error,    setError]    = useState(null);
    const [search,   setSearch]   = useState('');

    useEffect(() => {
        api.get('/contests')
            .then(res => setContests(res.data))
            .catch(() => setError('Failed to load contests'))
            .finally(() => setLoading(false));
    }, []);

    const filtered = contests.filter(c =>
        c.name?.toLowerCase().includes(search.toLowerCase())
    );

    const getStatus = (c) => {
        const now = Date.now();
        const start = new Date(c.startTime).getTime();
        const end   = new Date(c.endTime).getTime();
        if (now < start) return 'UPCOMING';
        if (now > end)   return 'ENDED';
        return 'LIVE';
    };

    const statusStyle = (s) => ({
        LIVE:     { color: C.error,    border: `1px solid ${C.error}` },
        ENDED:    { color: C.outline,  border: `1px solid ${C.border}` },
        UPCOMING: { color: C.secondary, border: `1px solid ${C.secondary}` },
    }[s]);

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', letterSpacing: '0.1em' }}>
            Loading...
        </div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", padding: isMobile ? '24px 16px' : '48px 64px' }}>

            {/* ── Header ── */}
            <motion.header
                initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: isMobile ? 'flex-start' : 'flex-end', marginBottom: '3rem', paddingBottom: '2rem', borderBottom: `1px solid ${C.border}`, flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? '1rem' : '0' }}
            >
                <div>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase', display: 'block', marginBottom: '8px' }}>
                        Leaderboard Control
                    </span>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '28px' : '40px', fontWeight: 600, color: C.onBg, marginBottom: '8px' }}>
                        Contest Index
                    </h1>
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                        Index and overview of all contest results.
                    </p>
                </div>

                {/* Search */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', width: '280px', transition: 'border-color 0.2s' }}
                    onFocus={e => e.currentTarget.style.borderBottomColor = C.secondary}
                    onBlur={e => e.currentTarget.style.borderBottomColor = C.border}
                >
                    <span className="material-symbols-outlined" style={{ color: C.outline, fontSize: '18px' }}>search</span>
                    <input
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        placeholder="SEARCH CONTESTS..."
                        style={{
                            background: 'transparent', border: 'none', outline: 'none',
                            fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
                            letterSpacing: '0.08em', color: C.onBg, width: '100%',
                        }}
                    />
                    <span style={{ color: C.primary, animation: 'blink 1s infinite', fontFamily: "'JetBrains Mono', monospace" }}>█</span>
                </div>
            </motion.header>

            {error && (
                <div style={{ padding: '12px 16px', border: `1px solid ${C.error}`, borderLeft: `3px solid ${C.error}`, backgroundColor: 'rgba(255,180,171,0.06)', marginBottom: '2rem', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                    {error}
                </div>
            )}

            {/* ── Contest Table ── */}
            <motion.section
                initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceCon, overflow: 'hidden' }}
                className="responsive-table-wrap"
            >
                {/* Table header */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 120px 120px 120px', gap: '16px', padding: '16px 24px', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceHi }}>
                    {['Contest Name', 'Status', 'Participants', 'Action'].map((h, i) => (
                        <span key={h} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', textAlign: i > 1 ? 'center' : 'left' }}>
                            {h}
                        </span>
                    ))}
                </div>

                {filtered.length === 0 ? (
                    <div style={{ padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                        No contests found
                    </div>
                ) : (
                    filtered.map((contest, i) => {
                        const status = getStatus(contest);
                        const ss = statusStyle(status);
                        const isLive = status === 'LIVE';
                        return (
                            <motion.div
                                key={contest.id}
                                initial={{ opacity: 0, x: -10 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ delay: i * 0.04 }}
                                style={{
                                    display: 'grid', gridTemplateColumns: '1fr 120px 120px 120px',
                                    gap: '16px', padding: '20px 24px',
                                    borderBottom: i < filtered.length - 1 ? `1px solid ${C.border}` : 'none',
                                    borderLeft: isLive ? `2px solid ${C.error}` : '2px solid transparent',
                                    transition: 'background-color 0.2s',
                                    cursor: 'pointer',
                                }}
                                onMouseEnter={e => e.currentTarget.style.backgroundColor = C.surfaceHi}
                                onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                            >
                                <div>
                                    <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.onBg, marginBottom: '4px' }}>{contest.name}</p>
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                        {new Date(contest.startTime).toLocaleDateString()} — {new Date(contest.endTime).toLocaleDateString()}
                                    </p>
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                    <span style={{ ...ss, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', padding: '3px 10px', textTransform: 'uppercase' }}>
                                        {status}
                                    </span>
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.muted }}>
                                    {contest.participantCount ?? '—'}
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                    <button
                                        onClick={() => navigate(`/admin/leaderboard/${contest.id}`)}
                                        style={{
                                            fontFamily: "'JetBrains Mono', monospace",
                                            fontSize: '11px', letterSpacing: '0.1em',
                                            color: C.secondary, background: 'none', border: 'none',
                                            cursor: 'pointer', textTransform: 'uppercase',
                                            display: 'flex', alignItems: 'center', gap: '4px',
                                            transition: 'color 0.2s',
                                        }}
                                        onMouseEnter={e => e.currentTarget.style.color = C.primary}
                                        onMouseLeave={e => e.currentTarget.style.color = C.secondary}
                                    >
                                        View →
                                    </button>
                                </div>
                            </motion.div>
                        );
                    })
                )}
            </motion.section>

            <style>{`
                @keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
            `}</style>
        </div>
    );
};

export default Leaderboard;
