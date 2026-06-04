import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import cache from '../services/cache';
import useResponsive from '../hooks/useResponsive';

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
};

/* ── Countdown timer ── */
const Countdown = ({ endTime, startTime }) => {
    const [display, setDisplay] = useState('');
    const [isLive, setIsLive]   = useState(false);

    useEffect(() => {
        const tick = () => {
            const now   = Date.now();
            const start = new Date(startTime).getTime();
            const end   = new Date(endTime).getTime();
            const live  = now >= start && now < end;
            setIsLive(live);
            const target = live ? end : start;
            const diff   = target - now;
            if (diff <= 0) { setDisplay(live ? 'Ended' : 'Started'); return; }
            const d = Math.floor(diff / 86400000);
            const h = Math.floor((diff % 86400000) / 3600000);
            const m = Math.floor((diff % 3600000) / 60000);
            const s = Math.floor((diff % 60000) / 1000);
            if (d > 0) setDisplay(`${d}d ${h}h ${m}m`);
            else if (h > 0) setDisplay(`${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`);
            else setDisplay(`${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`);
        };
        tick();
        const id = setInterval(tick, 1000);
        return () => clearInterval(id);
    }, [endTime, startTime]);

    return (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginBottom: '4px' }}>
                {isLive ? 'Ends In' : 'Starts In'}
            </span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '20px', color: isLive ? C.secondary : C.primary, letterSpacing: '0.05em' }}>
                {display}
            </span>
        </div>
    );
};

const ContestList = () => {
    const { isMobile } = useResponsive();
    const [contests, setContests] = useState([]);
    const [loading,  setLoading]  = useState(true);
    const [error,    setError]    = useState('');
    const [search,   setSearch]   = useState('');
    const [filter,   setFilter]   = useState('ALL');

    useEffect(() => {
        cache.get('contests', () => api.get('/contests').then(r => r.data))
            .then(data => setContests(data))
            .catch(() => setError('Failed to load contests.'))
            .finally(() => setLoading(false));
    }, []);

    const getStatus = (c) => {
        const now   = Date.now();
        const start = new Date(c.startTime).getTime();
        const end   = new Date(c.endTime).getTime();
        if (now < start) return 'UPCOMING';
        if (now > end)   return 'ENDED';
        return 'LIVE';
    };

    const filtered = contests.filter(c => {
        const matchSearch = c.name?.toLowerCase().includes(search.toLowerCase());
        const status = getStatus(c);
        const matchFilter = filter === 'ALL' || filter === status;
        return matchSearch && matchFilter;
    });

    const liveContests     = contests.filter(c => getStatus(c) === 'LIVE');
    const upcomingContests = contests.filter(c => getStatus(c) === 'UPCOMING');

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', letterSpacing: '0.1em' }}>
            Loading...
        </div>
    );

    if (error) return (
        <div style={{ padding: isMobile ? '24px 16px' : '48px 64px', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.error }}>
            {error}
        </div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", padding: isMobile ? '24px 16px' : '48px 64px' }}>

            {/* ── Featured Live Hero ── */}
            {liveContests.length > 0 && (
                <motion.section
                    initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                    style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, borderTop: `2px solid ${C.secondary}`, padding: '2.5rem', marginBottom: '3rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '2rem', position: 'relative', overflow: 'hidden' }}
                >
                    {/* Grid bg */}
                    <div style={{ position: 'absolute', inset: 0, backgroundImage: `linear-gradient(${C.border} 1px, transparent 1px), linear-gradient(90deg, ${C.border} 1px, transparent 1px)`, backgroundSize: '32px 32px', opacity: 0.06, pointerEvents: 'none' }} />

                    <div style={{ position: 'relative', zIndex: 1, maxWidth: '640px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '1rem' }}>
                            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: C.secondary, animation: 'pulse 2s infinite', display: 'inline-block' }} />
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase' }}>Live Event</span>
                        </div>
                        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '48px', fontWeight: 700, lineHeight: 1.1, color: C.onBg, marginBottom: '1rem' }}>
                            Contest Arena
                        </h1>
                        <p style={{ fontSize: '16px', color: C.muted, lineHeight: 1.6, marginBottom: '2rem', maxWidth: '480px' }}>
                            Compete, solve, and rise through the ranks. Multiple live contests are running — enter the arena and prove your skills.
                        </p>
                        
                    </div>

                    {/* Right panel */}
                    <div style={{ position: 'relative', zIndex: 1, border: `1px solid ${C.border}`, backgroundColor: C.surfaceCon, padding: '1.5rem 2rem', flexShrink: 0, minWidth: '200px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 600, color: C.secondary, textAlign: 'center', lineHeight: 1.4 }}>
                            Welcome to the Arena
                        </span>
                    </div>
                </motion.section>
            )}

            {/* ── Search + Filter ── */}
            <motion.div
                initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.05 }}
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: isMobile ? 'flex-start' : 'center', marginBottom: '2rem', paddingBottom: '1.5rem', borderBottom: `1px solid ${C.border}`, flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? '1rem' : '0' }}
            >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', width: '280px' }}>
                    <span className="material-symbols-outlined" style={{ color: C.outline, fontSize: '18px' }}>search</span>
                    <input
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        placeholder="SEARCH CONTESTS..."
                        style={{ background: 'transparent', border: 'none', outline: 'none', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.08em', color: C.onBg, width: '100%' }}
                    />
                    <span style={{ color: C.primary, animation: 'blink 1s infinite', fontFamily: "'JetBrains Mono', monospace" }}>█</span>
                </div>
                <div style={{ display: 'flex', gap: '0', border: `1px solid ${C.border}` }}>
                    {['ALL', 'LIVE', 'UPCOMING', 'ENDED'].map(f => (
                        <button key={f} onClick={() => setFilter(f)}
                            style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', padding: '6px 16px', background: filter === f ? C.secondary : 'transparent', color: filter === f ? C.bg : C.outline, border: 'none', cursor: 'pointer', borderRight: f !== 'ENDED' ? `1px solid ${C.border}` : 'none', transition: 'all 0.2s' }}
                        >
                            {f}
                        </button>
                    ))}
                </div>
            </motion.div>

            {/* ── Contest List ── */}
            {filtered.length === 0 ? (
                <div style={{ border: `1px solid ${C.border}`, padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                    No contests found
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1px', backgroundColor: C.border }}>
                    {filtered.map((contest, i) => {
                        const status = getStatus(contest);
                        const isLive = status === 'LIVE';
                        const isEnded = status === 'ENDED';
                        return (
                            <ContestRow key={contest.id} contest={contest} status={status} isLive={isLive} isEnded={isEnded} index={i} />
                        );
                    })}
                </div>
            )}

            <style>{`
                @keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
                @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
            `}</style>
        </div>
    );
};

const ContestRow = ({ contest, status, isLive, isEnded, index }) => {
    const [hovered, setHovered] = useState(false);
    const accentColor = isLive ? '#e9c176' : status === 'UPCOMING' ? '#f1bc8b' : '#50453b';
    const statusColors = {
        LIVE:     { color: '#e9c176', border: '#e9c176', bg: 'rgba(96,68,3,0.3)' },
        UPCOMING: { color: '#f1bc8b', border: '#f1bc8b', bg: 'rgba(80,69,59,0.3)' },
        ENDED:    { color: '#9d8e83', border: '#50453b', bg: 'transparent' },
    };
    const sc = statusColors[status];

    return (
        <motion.div
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.04 }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'grid', gridTemplateColumns: window.innerWidth < 768 ? '1fr' : '1fr 200px 160px',
                gap: window.innerWidth < 768 ? '12px' : '24px', padding: window.innerWidth < 768 ? '16px' : '24px 32px',
                backgroundColor: hovered ? '#201f1f' : '#131313',
                borderLeft: `2px solid ${hovered ? accentColor : 'transparent'}`,
                transition: 'all 0.2s',
                opacity: isEnded ? 0.65 : 1,
                position: 'relative',
            }}
        >
            {/* Contest info */}
            <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px', flexWrap: 'wrap' }}>
                    <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 600, color: '#e5e2e1' }}>
                        {contest.name}
                    </h3>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: sc.color, border: `1px solid ${sc.border}`, backgroundColor: sc.bg, padding: '2px 8px', textTransform: 'uppercase' }}>
                        {status}
                    </span>
                    {contest.proctored === true && (
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.secondary, border: `1px solid ${C.secondary}`, padding: '2px 8px', textTransform: 'uppercase' }}>
                            Proctored
                        </span>
                    )}
                </div>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: '#9d8e83', lineHeight: 1.5, marginBottom: '8px' }}>
                    {contest.description || 'No description provided.'}
                </p>
                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: '#50453b' }}>
                    {new Date(contest.startTime).toLocaleDateString()} — {new Date(contest.endTime).toLocaleDateString()}
                </p>
            </div>

            {/* Timer */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', borderLeft: `1px solid #50453b`, paddingLeft: '24px' }}>
                {!isEnded ? (
                    <Countdown endTime={contest.endTime} startTime={contest.startTime} />
                ) : (
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: '#9d8e83' }}>Ended</span>
                )}
            </div>

            {/* Action */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', borderLeft: `1px solid #50453b`, paddingLeft: '24px' }}>
                {!isEnded ? (
                    <Link
                        to={`/contests/${contest.id}`}
                        style={{ border: `1px solid ${isLive ? '#e9c176' : '#50453b'}`, color: isLive ? '#e9c176' : '#d4c4b7', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', padding: '10px 20px', textDecoration: 'none', transition: 'all 0.2s' }}
                        onMouseEnter={e => { e.currentTarget.style.backgroundColor = isLive ? '#e9c176' : '#f1bc8b'; e.currentTarget.style.color = '#131313'; e.currentTarget.style.borderColor = isLive ? '#e9c176' : '#f1bc8b'; }}
                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = isLive ? '#e9c176' : '#d4c4b7'; e.currentTarget.style.borderColor = isLive ? '#e9c176' : '#50453b'; }}
                    >
                        {isLive ? 'Enter Arena' : 'View Contest'}
                    </Link>
                ) : (
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: '#50453b', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        Archived
                    </span>
                )}
            </div>
        </motion.div>
    );
};

export default ContestList;
