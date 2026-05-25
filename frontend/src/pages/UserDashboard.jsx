import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import AuthService from '../services/auth.service';
import useResponsive from '../hooks/useResponsive';

// ── Design tokens ─────────────────────────────────────────────────────────────
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
    success:    '#4ade80',
};

// ── Status helpers ────────────────────────────────────────────────────────────
const STATUS_CFG = {
    AC:      { color: C.success,   label: 'Accepted',             bar: C.success },
    WA:      { color: C.error,     label: 'Wrong Answer',         bar: C.error },
    TLE:     { color: '#facc15',   label: 'Time Limit Exceeded',  bar: '#facc15' },
    RE:      { color: '#fb923c',   label: 'Runtime Error',        bar: '#fb923c' },
    CE:      { color: '#fb923c',   label: 'Compilation Error',    bar: '#fb923c' },
    MLE:     { color: '#a78bfa',   label: 'Memory Limit Exceeded',bar: '#a78bfa' },
    PENDING: { color: C.outline,   label: 'Pending',              bar: C.outline },
    JUDGING: { color: '#7ab3e0',   label: 'Judging',              bar: '#7ab3e0' },
};
const getStatus = (s) => STATUS_CFG[s] || { color: C.outline, label: s, bar: C.outline };

const timeAgo = (dateStr) => {
    if (!dateStr) return '—';
    const diff = Date.now() - new Date(dateStr).getTime();
    const m = Math.floor(diff / 60000);
    if (m < 1)  return 'just now';
    if (m < 60) return `${m}m ago`;
    const h = Math.floor(m / 60);
    if (h < 24) return `${h}h ago`;
    return new Date(dateStr).toLocaleDateString();
};

export default function UserDashboard() {
    const { isMobile } = useResponsive();
    const navigate = useNavigate();
    const [user,        setUser]        = useState(null);
    const [submissions, setSubmissions] = useState([]);
    const [contests,    setContests]    = useState([]);
    const [stats,       setStats]       = useState({ total: 0, accepted: 0, successRate: 0, problemsSolved: 0 });
    const [loading,     setLoading]     = useState(true);

    useEffect(() => {
        // Fetch dashboard data + contests in parallel
        Promise.all([
            api.get('/user/dashboard'),
            api.get('/contests').catch(() => ({ data: [] })),
        ]).then(([dashRes, contestsRes]) => {
            const { profile, submissions: subs, totalSubmissions } = dashRes.data;
            setUser(profile);
            setSubmissions(subs || []);
            setContests(contestsRes.data || []);
            calcStats(subs || [], totalSubmissions);
        }).catch(() => {
            const u = AuthService.getCurrentUser();
            setUser(u);
        }).finally(() => setLoading(false));
    }, []);

    const calcStats = (subs, totalCount) => {
        const total    = totalCount !== undefined ? totalCount : subs.length;
        const accepted = subs.filter(s => s.status === 'AC').length;
        const solved   = new Set(subs.filter(s => s.status === 'AC').map(s => s.problemId)).size;
        const rate     = total > 0 ? ((accepted / total) * 100).toFixed(1) : 0;
        setStats({ total, accepted, successRate: rate, problemsSolved: solved });
    };

    // Upcoming / live contests
    const upcomingContests = contests.filter(c => {
        const now = Date.now();
        return new Date(c.endTime).getTime() > now;
    }).slice(0, 5);

    const initials = (user?.fullName || user?.username || 'U').charAt(0).toUpperCase();

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
            Loading...
        </div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh' }}>
            <div style={{ maxWidth: '1440px', margin: '0 auto', padding: isMobile ? '24px 16px' : '48px 64px', display: 'flex', flexDirection: 'column', gap: '32px' }}>

                {/* ── Page Header ── */}
                <motion.header
                    initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                    style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '24px' }}
                >
                    <div>
                        <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.primary, textTransform: 'uppercase', marginBottom: '8px' }}>
                            Command Center
                        </p>
                        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(48px, 6vw, 72px)', fontWeight: 700, lineHeight: 1.1, letterSpacing: '-0.02em', color: C.onBg, margin: 0 }}>
                            Dashboard
                        </h1>
                    </div>
                    <div style={{ display: 'flex', gap: '12px' }}>
                        <button
                            onClick={() => navigate('/profile/edit')}
                            style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '12px 24px', border: `1px solid ${C.border}`, color: C.onBg, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s' }}
                            onMouseEnter={e => { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; }}
                            onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.onBg; }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>settings</span>
                            Manage Profile
                        </button>
                        <button
                            onClick={() => navigate('/contests')}
                            style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '12px 24px', border: `1px solid ${C.secondary}`, color: C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s' }}
                            onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                            onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.secondary; }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>play_arrow</span>
                            Start Battle
                        </button>
                    </div>
                </motion.header>

                {/* ── Main Bento Grid ── */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                    style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(12, 1fr)', gap: '1px', backgroundColor: C.border, border: `1px solid ${C.border}` }}
                >
                    {/* ── Profile Panel (4 cols) ── */}
                    <div style={{ gridColumn: isMobile ? 'span 1' : 'span 4', backgroundColor: C.bg, padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem', position: 'relative', overflow: 'hidden' }}>
                        {/* Amber top accent */}
                        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.secondary }} />

                        {/* Avatar + name */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', zIndex: 1 }}>
                            <div style={{ width: '80px', height: '80px', borderRadius: '50%', border: `1px solid ${C.secondary}`, padding: '3px', flexShrink: 0 }}>
                                {user?.photoUrl ? (
                                    <img
                                        src={user.photoUrl}
                                        alt={user?.fullName || user?.username}
                                        style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%', filter: 'grayscale(1)', transition: 'filter 0.5s' }}
                                        onMouseEnter={e => e.target.style.filter = 'grayscale(0)'}
                                        onMouseLeave={e => e.target.style.filter = 'grayscale(1)'}
                                    />
                                ) : (
                                    <div style={{ width: '100%', height: '100%', borderRadius: '50%', backgroundColor: C.surfaceLow, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 700, color: C.primary }}>{initials}</span>
                                    </div>
                                )}
                            </div>
                            <div>
                                <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 600, color: C.onBg, marginBottom: '4px' }}>
                                    {user?.fullName || user?.username || '—'}
                                </h3>
                                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.outline, display: 'flex', alignItems: 'center', gap: '6px' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.secondary, fontVariationSettings: "'FILL' 1" }}>verified</span>
                                    {user?.branch || 'Architect'}
                                </p>
                            </div>
                        </div>

                        {/* Profile fields */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            {[
                                { label: 'Username',    value: user?.username    || '—' },
                                { label: 'Email',       value: user?.email       || '—', mono: true },
                                { label: 'Roll Number', value: user?.rollNumber  || '—', mono: true },
                                { label: 'Branch',      value: user?.branch      || '—' },
                            ].map(({ label, value, mono }) => (
                                <div key={label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: `1px solid rgba(80,69,59,0.3)`, paddingBottom: '10px' }}>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>{label}</span>
                                    <span style={{ fontFamily: mono ? "'JetBrains Mono', monospace" : "'Geist', sans-serif", fontSize: '13px', color: C.muted, maxWidth: '160px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{value}</span>
                                </div>
                            ))}
                        </div>

                        {/* Mini stats */}
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1px', backgroundColor: C.border, border: `1px solid ${C.border}`, marginTop: 'auto' }}>
                            <div style={{ backgroundColor: C.surfaceLow, padding: '1rem', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>Problems</span>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '20px', color: C.onBg }}>{stats.problemsSolved}</span>
                            </div>
                            <div style={{ backgroundColor: C.surfaceLow, padding: '1rem', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>Success</span>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '20px', color: C.secondary }}>{stats.successRate}%</span>
                            </div>
                        </div>
                    </div>

                    {/* ── Stats Grid (8 cols, 4 cards) ── */}
                    <div style={{ gridColumn: isMobile ? 'span 1' : 'span 8', display: 'grid', gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)', gap: '1px', backgroundColor: C.border }}>
                        {[
                            { icon: 'history_edu',  value: stats.total,          label: 'Submissions',    hoverColor: C.primary },
                            { icon: 'check_circle', value: stats.accepted,        label: 'Accepted',       hoverColor: C.success },
                            { icon: 'percent',      value: `${stats.successRate}`, label: 'Success Rate',  hoverColor: C.secondary, highlight: true },
                            { icon: 'extension',    value: stats.problemsSolved,  label: 'Problems Solved',hoverColor: C.primary },
                        ].map(({ icon, value, label, hoverColor, highlight }) => (
                            <StatCard key={label} icon={icon} value={value} label={label} hoverColor={hoverColor} highlight={highlight} />
                        ))}
                    </div>
                </motion.div>

                {/* ── Submissions + Skill Vector ── */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.2 }}
                    style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(12, 1fr)', gap: '1px', backgroundColor: C.border, border: `1px solid ${C.border}` }}
                >
                    {/* Recent Submissions (8 cols) */}
                    <div style={{ gridColumn: isMobile ? 'span 1' : 'span 8', backgroundColor: C.bg, display: 'flex', flexDirection: 'column' }}>
                        <div style={{ padding: '1.5rem 2rem', borderBottom: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 600, color: C.onBg, margin: 0 }}>
                                Recent Submissions
                            </h3>
                            <Link
                                to="/contests"
                                style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.secondary, textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '4px', transition: 'opacity 0.2s' }}
                                onMouseEnter={e => e.currentTarget.style.opacity = '0.7'}
                                onMouseLeave={e => e.currentTarget.style.opacity = '1'}
                            >
                                View All
                                <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>arrow_forward</span>
                            </Link>
                        </div>

                        {submissions.length === 0 ? (
                            <div style={{ padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                                No submissions yet.{' '}
                                <Link to="/contests" style={{ color: C.primary, textDecoration: 'underline' }}>Start solving →</Link>
                            </div>
                        ) : (
                            <div style={{ overflowX: 'auto' }}>
                                <table style={{ width: '100%', borderCollapse: 'collapse', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', whiteSpace: 'nowrap' }}>
                                    <thead>
                                        <tr style={{ backgroundColor: C.surfaceLow, borderBottom: `1px solid ${C.border}` }}>
                                            {['Problem', 'Language', 'Result', 'Score', 'Time'].map(h => (
                                                <th key={h} style={{ padding: '14px 24px', textAlign: 'left', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', fontWeight: 500 }}>
                                                    {h}
                                                </th>
                                            ))}
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {submissions.slice(0, 10).map((sub, i) => {
                                            const sc = getStatus(sub.status);
                                            return (
                                                <SubRow key={sub.id || i} sub={sub} sc={sc} />
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>

                    {/* Skill Vector (4 cols) */}
                    <div style={{ gridColumn: isMobile ? 'span 1' : 'span 4', backgroundColor: C.bg, padding: '2rem', borderLeft: isMobile ? 'none' : `1px solid ${C.border}`, display: 'flex', flexDirection: 'column' }}>
                        <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 600, color: C.onBg, marginBottom: '1.5rem' }}>
                            Skill Vector
                        </h3>
                        <div style={{ flex: 1, position: 'relative', minHeight: '280px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            {/* Concentric circles */}
                            {[80, 60, 40, 20].map(size => (
                                <div key={size} style={{ position: 'absolute', width: `${size}%`, height: `${size}%`, border: `1px solid ${C.border}`, borderRadius: '50%', opacity: 0.4 }} />
                            ))}
                            {/* Cross lines */}
                            {[0, 45, 90, 135].map(deg => (
                                <div key={deg} style={{ position: 'absolute', width: '100%', height: '1px', backgroundColor: C.border, opacity: 0.3, transform: `rotate(${deg}deg)` }} />
                            ))}
                            {/* Polygon */}
                            <svg style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', pointerEvents: 'none' }} viewBox="0 0 100 100" preserveAspectRatio="none">
                                <polygon
                                    points="50,15 85,35 75,80 30,85 15,45"
                                    fill="rgba(233,193,118,0.1)"
                                    stroke={C.secondary}
                                    strokeWidth="1.5"
                                />
                                {[[50,15],[85,35],[75,80],[30,85],[15,45]].map(([cx,cy], i) => (
                                    <circle key={i} cx={cx} cy={cy} r="2.5" fill={C.secondary} />
                                ))}
                            </svg>
                            {/* Labels */}
                            {[
                                { label: 'Graphs',       style: { top: '4%',    left: '50%', transform: 'translateX(-50%)' } },
                                { label: 'DP',           style: { top: '28%',   right: '4%' } },
                                { label: 'Math',         style: { bottom: '12%',right: '12%' } },
                                { label: 'Strings',      style: { bottom: '8%', left: '12%' } },
                                { label: 'Data Structs', style: { top: '38%',   left: '2%' } },
                            ].map(({ label, style }) => (
                                <span key={label} style={{ position: 'absolute', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: C.onBg, ...style }}>
                                    {label}
                                </span>
                            ))}
                        </div>

                        {/* Skill bars */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '1.5rem' }}>
                            {[
                                { label: 'Graphs',       pct: 72 },
                                { label: 'DP',           pct: 85 },
                                { label: 'Math',         pct: 60 },
                                { label: 'Strings',      pct: 78 },
                                { label: 'Data Structs', pct: 68 },
                            ].map(({ label, pct }) => (
                                <div key={label}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase' }}>{label}</span>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.secondary }}>{pct}%</span>
                                    </div>
                                    <div style={{ height: '2px', backgroundColor: C.surfaceHi, borderRadius: '1px' }}>
                                        <div style={{ height: '100%', width: `${pct}%`, backgroundColor: C.secondary, borderRadius: '1px', transition: 'width 1s ease' }} />
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </motion.div>

                {/* ── Upcoming Contests ── */}
                <motion.section
                    initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.3 }}
                >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '1.5rem' }}>
                        <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.onBg, margin: 0 }}>
                            Upcoming Engagements
                        </h3>
                        <Link to="/contests" style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.secondary, textDecoration: 'none' }}>
                            View All →
                        </Link>
                    </div>

                    {upcomingContests.length === 0 ? (
                        <div style={{ border: `1px solid ${C.border}`, padding: '3rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                            No upcoming contests.{' '}
                            <Link to="/contests" style={{ color: C.primary, textDecoration: 'underline' }}>Browse all →</Link>
                        </div>
                    ) : (
                        <div style={{ display: 'flex', gap: '1px', backgroundColor: C.border, border: `1px solid ${C.border}`, overflowX: 'auto' }}>
                            {upcomingContests.map(c => <ContestCard key={c.id} contest={c} navigate={navigate} />)}
                        </div>
                    )}
                </motion.section>

                {/* ── Activity Feed ── */}
                <motion.section
                    initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.4 }}
                >
                    <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.onBg, marginBottom: '1.5rem' }}>
                        Recent Activity
                    </h3>
                    <div style={{ borderLeft: `1px solid ${C.border}`, marginLeft: '16px', paddingLeft: '32px', display: 'flex', flexDirection: 'column', gap: '2rem', paddingTop: '8px', paddingBottom: '8px' }}>
                        {submissions.slice(0, 5).map((sub, i) => {
                            const sc = getStatus(sub.status);
                            return (
                                <div key={sub.id || i} style={{ position: 'relative' }}>
                                    {/* Diamond bullet */}
                                    <div style={{ position: 'absolute', left: '-41px', top: '4px', width: '10px', height: '10px', backgroundColor: C.bg, border: `1px solid ${sub.status === 'AC' ? C.secondary : C.border}`, transform: 'rotate(45deg)' }} />
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: C.outline, marginBottom: '4px', textTransform: 'uppercase' }}>
                                        {timeAgo(sub.submittedAt)}
                                    </p>
                                    <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.onBg, lineHeight: 1.5 }}>
                                        Submitted{' '}
                                        <Link to={`/problems/${sub.problemId}`} style={{ color: C.primary, textDecoration: 'none' }}>
                                            {sub.problemName || `Problem #${sub.problemId}`}
                                        </Link>
                                        {' — '}
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: sc.color }}>
                                            {sc.label}
                                        </span>
                                        {sub.score != null && (
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline }}> ({sub.score}/100)</span>
                                        )}
                                    </p>
                                </div>
                            );
                        })}
                        {submissions.length === 0 && (
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                                No activity yet. <Link to="/contests" style={{ color: C.primary, textDecoration: 'underline' }}>Start competing →</Link>
                            </p>
                        )}
                    </div>
                </motion.section>
            </div>

            <style>{`.material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }`}</style>
        </div>
    );
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
function StatCard({ icon, value, label, hoverColor, highlight }) {
    const [hovered, setHovered] = useState(false);
    return (
        <div
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                backgroundColor: hovered ? C.surfaceLow : C.bg,
                padding: '1.5rem',
                display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
                transition: 'background-color 0.3s',
                position: 'relative', overflow: 'hidden',
            }}
        >
            {/* Hover top line */}
            {highlight && (
                <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '1px', backgroundColor: C.secondary, opacity: hovered ? 1 : 0, transition: 'opacity 0.3s' }} />
            )}
            <div style={{ marginBottom: '2rem' }}>
                <span
                    className="material-symbols-outlined"
                    style={{ fontSize: '22px', color: hovered ? hoverColor : C.outline, transition: 'color 0.3s' }}
                >
                    {icon}
                </span>
            </div>
            <div>
                <p style={{ fontFamily: "'Playfair Display', serif", fontSize: '40px', fontWeight: 300, lineHeight: 1, color: highlight ? C.secondary : C.onBg, margin: 0 }}>
                    {value}
                </p>
                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginTop: '8px' }}>
                    {label}
                </p>
            </div>
        </div>
    );
}

// ── Submission Row ─────────────────────────────────────────────────────────────
function SubRow({ sub, sc }) {
    const [hovered, setHovered] = useState(false);
    return (
        <tr
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{ borderBottom: `1px solid ${C.border}`, backgroundColor: hovered ? C.surfaceMin : 'transparent', transition: 'background-color 0.2s' }}
        >
            <td style={{ padding: '14px 24px', color: C.onBg, display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div style={{ width: '3px', height: '16px', backgroundColor: sc.bar, flexShrink: 0 }} />
                <Link
                    to={`/problems/${sub.problemId}`}
                    style={{ color: C.onBg, textDecoration: 'none', transition: 'color 0.2s' }}
                    onMouseEnter={e => e.currentTarget.style.color = C.primary}
                    onMouseLeave={e => e.currentTarget.style.color = C.onBg}
                >
                    {sub.problemName || `Problem #${sub.problemId}`}
                </Link>
            </td>
            <td style={{ padding: '14px 24px', color: C.outline }}>{sub.language || '—'}</td>
            <td style={{ padding: '14px 24px', color: sc.color }}>{sc.label}</td>
            <td style={{ padding: '14px 24px', color: C.muted }}>
                {sub.score != null ? `${sub.score}/100` : '—'}
            </td>
            <td style={{ padding: '14px 24px', color: C.outline, textAlign: 'right' }}>
                {timeAgo(sub.submittedAt)}
            </td>
        </tr>
    );
}

// ── Contest Card ──────────────────────────────────────────────────────────────
function ContestCard({ contest, navigate }) {
    const [hovered, setHovered] = useState(false);
    const now   = Date.now();
    const start = new Date(contest.startTime).getTime();
    const end   = new Date(contest.endTime).getTime();
    const isLive = now >= start && now < end;

    const timeLabel = isLive
        ? 'Live Now'
        : now < start
        ? `Starts ${timeAgo(contest.startTime).replace(' ago', '')}`
        : 'Ended';

    const durationMs = end - start;
    const durationH  = Math.floor(durationMs / 3600000);
    const durationM  = Math.floor((durationMs % 3600000) / 60000);
    const durationStr = durationH > 0 ? `${durationH}h ${durationM > 0 ? durationM + 'm' : ''}` : `${durationM}m`;

    return (
        <div
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                minWidth: '320px',
                backgroundColor: C.bg,
                padding: '1.5rem',
                display: 'flex', flexDirection: 'column', gap: '1rem',
                borderTop: `2px solid ${hovered ? C.secondary : 'transparent'}`,
                transition: 'border-color 0.3s',
                cursor: 'pointer',
                flexShrink: 0,
            }}
            onClick={() => navigate(`/contests/${contest.id}`)}
        >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <span style={{ padding: '3px 10px', backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: isLive ? C.secondary : C.primary, textTransform: 'uppercase' }}>
                    {isLive ? 'Live' : 'Upcoming'}
                </span>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.secondary, display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>timer</span>
                    {durationStr}
                </span>
            </div>
            <h4 style={{ fontFamily: "'Playfair Display', serif", fontSize: '18px', fontWeight: 600, color: C.onBg, lineHeight: 1.3, margin: 0 }}>
                {contest.name}
            </h4>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 'auto', paddingTop: '1rem', borderTop: `1px solid ${C.border}` }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                    {timeLabel}
                </span>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.onBg, letterSpacing: '0.1em', textTransform: 'uppercase', borderBottom: `1px solid ${hovered ? C.secondary : 'transparent'}`, transition: 'border-color 0.3s' }}>
                    {isLive ? 'Enter Arena' : 'View'}
                </span>
            </div>
        </div>
    );
}
