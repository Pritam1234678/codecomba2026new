import { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import { getDuelMetrics } from '../services/duelService';
import useResponsive from '../hooks/useResponsive';

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

const AdminDashboard = () => {
    const { isMobile, isTablet } = useResponsive();
    const location = useLocation();
    const [userStats,    setUserStats]    = useState({ total: 0, enabled: 0, disabled: 0 });
    const [contestStats, setContestStats] = useState({ total: 0, active: 0, inactive: 0 });
    const [adminProfile, setAdminProfile] = useState(null);
    const [loading,      setLoading]      = useState(true);
    const [duelMetrics,    setDuelMetrics]    = useState(null);
    const [duelMetricsErr, setDuelMetricsErr] = useState(false);

    useEffect(() => {
        const user = JSON.parse(localStorage.getItem('user'));
        if (user) setAdminProfile(user);
        api.get('/admin/dashboard')
            .then(res => {
                const { userStats, contestStats, profile } = res.data;
                setUserStats(userStats);
                setContestStats(contestStats);
                if (profile) setAdminProfile(profile);
            })
            .catch(() => {})
            .finally(() => setLoading(false));
    }, []);

    // Poll duel metrics every 5 s. On any failure (e.g. 503), keep prior values
    // and flip the error flag so the panel renders "—" placeholders rather than
    // crashing the dashboard.
    useEffect(() => {
        let cancelled = false;
        const fetchMetrics = () => {
            getDuelMetrics()
                .then((data) => {
                    if (cancelled) return;
                    setDuelMetrics(data);
                    setDuelMetricsErr(false);
                })
                .catch(() => {
                    if (cancelled) return;
                    setDuelMetricsErr(true);
                });
        };
        fetchMetrics();
        const id = setInterval(fetchMetrics, 5000);
        return () => {
            cancelled = true;
            clearInterval(id);
        };
    }, []);

    const stats = [
        { label: 'Total Users',      value: userStats.total,      sub: `${userStats.enabled} enabled`,       icon: 'group' },
        { label: 'Active Contests',  value: contestStats.active,  sub: `${contestStats.total} total`,        icon: 'military_tech' },
        { label: 'Disabled Users',   value: userStats.disabled,   sub: 'accounts blocked',                   icon: 'block' },
        { label: 'Total Contests',   value: contestStats.total,   sub: `${contestStats.inactive} inactive`,  icon: 'emoji_events' },
    ];

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
            {/* ── Main Canvas ── */}
            <main style={{ flex: 1, padding: isMobile ? '16px' : '32px 48px 32px', backgroundColor: C.bg, display: 'flex', flexDirection: 'column' }}>

                {/* Header */}
                <motion.header
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6 }}
                    style={{
                        display: 'flex', justifyContent: 'space-between',
                        marginBottom: '2rem',
                        paddingBottom: '1.5rem',
                        borderBottom: `1px solid ${C.border}`,
                        flexDirection: isMobile ? 'column' : 'row',
                        alignItems: isMobile ? 'flex-start' : 'flex-end',
                        gap: isMobile ? '1rem' : '0',
                    }}
                >
                    <div>
                        <h2 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: isMobile ? '28px' : '40px', fontWeight: 700,
                            lineHeight: 1.1, letterSpacing: '-0.02em',
                            color: C.onBg, marginBottom: '6px',
                        }}>
                            Command Center
                        </h2>
                        <p style={{ fontSize: '15px', color: C.muted, lineHeight: 1.5, maxWidth: '400px' }}>
                            Global system oversight and administrative control. All systems operational.
                        </p>
                    </div>
                    <div style={{ display: 'flex', gap: '16px', flexShrink: 0 }}>
                        <Link
                            to="/admin/contests/create"
                            style={{
                                padding: '12px 24px',
                                border: `1px solid ${C.secondary}`,
                                color: C.secondary,
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', letterSpacing: '0.1em',
                                textTransform: 'uppercase',
                                textDecoration: 'none',
                                display: 'flex', alignItems: 'center', gap: '8px',
                                transition: 'all 0.2s',
                                backgroundColor: 'transparent',
                            }}
                            onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                            onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.secondary; }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>add</span>
                            Create Contest
                        </Link>
                        <Link
                            to="/admin/users"
                            style={{
                                padding: '12px 24px',
                                border: `1px solid ${C.border}`,
                                color: C.onBg,
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', letterSpacing: '0.1em',
                                textTransform: 'uppercase',
                                textDecoration: 'none',
                                transition: 'border-color 0.2s',
                            }}
                            onMouseEnter={e => e.currentTarget.style.borderColor = C.secondary}
                            onMouseLeave={e => e.currentTarget.style.borderColor = C.border}
                        >
                            Manage Users
                        </Link>
                    </div>
                </motion.header>

                {/* Grid */}
                <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(12, 1fr)', gap: '16px', alignItems: 'stretch', flex: 1 }}>

                    {/* System Health — 8 cols */}
                    <motion.section
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.1 }}
                        style={{
                            gridColumn: isMobile ? 'span 1' : 'span 8',
                            backgroundColor: C.surfaceCon,
                            border: `1px solid ${C.border}`,
                            padding: '1.5rem',
                            display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
                        }}
                    >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                            <h3 style={{
                                fontFamily: "'Playfair Display', serif",
                                fontSize: '28px', fontWeight: 600,
                                color: C.onBg,
                                display: 'flex', alignItems: 'center', gap: '12px',
                            }}>
                                <span style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: C.primary, display: 'inline-block', animation: 'pulse 2s infinite' }} />
                                System Health
                            </h3>
                            <span style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '11px', letterSpacing: '0.15em',
                                color: C.primary,
                                border: `1px solid ${C.primary}`,
                                padding: '4px 12px',
                            }}>
                                LIVE METRICS
                            </span>
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(3, 1fr)', gap: isMobile ? '16px' : '32px', marginTop: 'auto' }}>
                            {[
                                { label: 'Total Users',    value: loading ? '—' : userStats.total },
                                { label: 'Total Contests', value: loading ? '—' : contestStats.total },
                                { label: 'Active Contests',value: loading ? '—' : contestStats.active },
                            ].map(({ label, value }) => (
                                <div key={label} style={{ borderTop: `1px solid ${C.border}`, paddingTop: '1rem' }}>
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', marginBottom: '8px' }}>
                                        {label}
                                    </p>
                                    <p style={{ fontFamily: "'Playfair Display', serif", fontSize: '40px', fontWeight: 300, color: C.onBg, margin: 0 }}>
                                        {value}
                                    </p>
                                </div>
                            ))}
                        </div>
                    </motion.section>

                    {/* Quick Actions — 4 cols */}
                    <motion.section
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.2 }}
                        style={{
                            gridColumn: isMobile ? 'span 1' : 'span 4',
                            backgroundColor: C.surfaceCon,
                            border: `1px solid ${C.border}`,
                            borderTop: `2px solid ${C.secondary}`,
                            padding: '1.5rem',
                            display: 'flex', flexDirection: 'column',
                        }}
                    >
                        <h3 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '20px', fontWeight: 600,
                            color: C.secondary,
                            display: 'flex', alignItems: 'center', gap: '10px',
                            marginBottom: '1rem',
                        }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '24px' }}>bolt</span>
                            Quick Actions
                        </h3>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', flexGrow: 1, justifyContent: 'space-between' }}>
                            {[
                                { to: '/admin/proctoring',    label: 'Proctoring Dashboard',    icon: 'group' },
                                { to: '/admin/contests', label: 'Manage Contests', icon: 'military_tech' },
                                { to: '/admin/leaderboard', label: 'Leaderboard', icon: 'leaderboard' },
                                { to: '/admin/problems', label: 'Problem List', icon: 'tune' },
                            ].map(({ to, label, icon }) => (
                                <Link
                                    key={to}
                                    to={to}
                                    style={{
                                        display: 'flex', alignItems: 'center', gap: '12px',
                                        padding: '12px 16px',
                                        border: `1px solid ${C.border}`,
                                        textDecoration: 'none',
                                        color: C.muted,
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '12px', letterSpacing: '0.08em',
                                        transition: 'all 0.2s',
                                        backgroundColor: 'transparent',
                                    }}
                                    onMouseEnter={e => { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; e.currentTarget.style.backgroundColor = 'rgba(96,68,3,0.15)'; }}
                                    onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; e.currentTarget.style.backgroundColor = 'transparent'; }}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{icon}</span>
                                    {label}
                                    <span style={{ marginLeft: 'auto', opacity: 0.4 }}>→</span>
                                </Link>
                            ))}
                        </div>
                    </motion.section>

                    {/* Stat Cards — 12 cols */}
                    <motion.section
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.3 }}
                        style={{
                            gridColumn: isMobile ? 'span 1' : 'span 12',
                            display: 'grid',
                            gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)',
                            gap: '16px',
                        }}
                    >
                        {stats.map(({ label, value, sub, icon }) => (
                            <div
                                key={label}
                                style={{
                                    backgroundColor: C.surface,
                                    border: `1px solid ${C.border}`,
                                    padding: '1rem',
                                    position: 'relative',
                                    overflow: 'hidden',
                                    transition: 'border-color 0.2s',
                                }}
                                onMouseEnter={e => e.currentTarget.style.borderColor = C.secondary}
                                onMouseLeave={e => e.currentTarget.style.borderColor = C.border}
                            >
                                {/* Left accent bar */}
                                <div style={{ position: 'absolute', top: 0, left: 0, width: '3px', height: '100%', backgroundColor: C.secondary }} />
                                <p style={{
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '11px', letterSpacing: '0.1em',
                                    color: C.outline, textTransform: 'uppercase',
                                    marginBottom: '1rem',
                                    display: 'flex', alignItems: 'center', gap: '8px',
                                }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>{icon}</span>
                                    {label}
                                </p>
                                <p style={{
                                    fontFamily: "'Playfair Display', serif",
                                    fontSize: '28px', fontWeight: 600,
                                    color: C.onBg, margin: 0,
                                }}>
                                    {loading ? '—' : value}
                                </p>
                                <p style={{
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '12px', color: C.primary,
                                    marginTop: '8px', margin: '8px 0 0',
                                }}>
                                    {sub}
                                </p>
                            </div>
                        ))}
                    </motion.section>

                    {/* Live Duels — 12 cols */}
                   { /*<motion.section
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.35 }}
                        style={{
                            gridColumn: isMobile ? 'span 1' : 'span 12',
                            backgroundColor: C.surfaceCon,
                            border: `1px solid ${C.border}`,
                            padding: '1.5rem',
                            display: 'flex', flexDirection: 'column',
                        }}
                    >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                            <h3 style={{
                                fontFamily: "'Playfair Display', serif",
                                fontSize: '24px', fontWeight: 600,
                                color: C.onBg,
                                display: 'flex', alignItems: 'center', gap: '12px',
                            }}>
                                <span
                                    className="material-symbols-outlined"
                                    style={{ fontSize: '24px', color: C.primary }}
                                >
                                    swords
                                </span>
                                Live Duels
                            </h3>
                            <span style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '11px', letterSpacing: '0.15em',
                                color: duelMetricsErr ? C.error : C.primary,
                                border: `1px solid ${duelMetricsErr ? C.error : C.primary}`,
                                padding: '4px 12px',
                            }}>
                                {duelMetricsErr ? 'METRICS UNAVAILABLE' : 'POLLING · 5S'}
                            </span>
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)', gap: isMobile ? '16px' : '32px' }}>
                            {[
                                { label: 'Active Matches',   key: 'activeMatchCount',      icon: 'sports_kabaddi' },
                                { label: 'Queue Depth',      key: 'queueDepth',            icon: 'schedule' },
                                { label: 'Finished Today',   key: 'matchesFinishedToday',  icon: 'task_alt' },
                                { label: 'Abandoned Today',  key: 'matchesAbandonedToday', icon: 'cancel' },
                            ].map(({ label, key, icon }) => {
                                const raw = duelMetrics ? duelMetrics[key] : null;
                                const display = duelMetricsErr || raw === null || raw === undefined ? '—' : raw;
                                return (
                                    <div key={key} style={{ borderTop: `1px solid ${C.border}`, paddingTop: '1rem' }}>
                                        <p style={{
                                            fontFamily: "'JetBrains Mono', monospace",
                                            fontSize: '11px', letterSpacing: '0.1em',
                                            color: C.outline, textTransform: 'uppercase',
                                            marginBottom: '8px',
                                            display: 'flex', alignItems: 'center', gap: '8px',
                                        }}>
                                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>{icon}</span>
                                            {label}
                                        </p>
                                        <p style={{
                                            fontFamily: "'Playfair Display', serif",
                                            fontSize: '36px', fontWeight: 300,
                                            color: C.onBg, margin: 0,
                                        }}>
                                            {display}
                                        </p>
                                    </div>
                                );
                            })}
                        </div>
                    </motion.section>*/}

                    {/* Profile Info — 12 cols */}
                    <motion.section
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.4 }}
                        style={{
                            gridColumn: isMobile ? 'span 1' : 'span 12',
                            backgroundColor: C.surfaceCon,
                            border: `1px solid ${C.border}`,
                            padding: '1.5rem',
                        }}
                    >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                            <h3 style={{
                                fontFamily: "'Playfair Display', serif",
                                fontSize: '20px', fontWeight: 600,
                                color: C.onBg,
                            }}>
                                Administrator Profile
                            </h3>
                            <Link
                                to="/profile/edit"
                                style={{
                                    padding: '10px 24px',
                                    border: `1px solid ${C.border}`,
                                    color: C.muted,
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '12px', letterSpacing: '0.1em',
                                    textTransform: 'uppercase',
                                    textDecoration: 'none',
                                    transition: 'all 0.2s',
                                }}
                                onMouseEnter={e => { e.currentTarget.style.borderColor = C.primary; e.currentTarget.style.color = C.primary; }}
                                onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}
                            >
                                Edit Profile
                            </Link>
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)', gap: '16px' }}>
                            {[
                                { label: 'Username',    value: adminProfile?.username   || '—' },
                                { label: 'Full Name',   value: adminProfile?.fullName   || '—' },
                                { label: 'Email',       value: adminProfile?.email      || '—' },
                                
                            ].map(({ label, value }) => (
                                <div key={label} style={{ borderTop: `1px solid ${C.border}`, paddingTop: '1rem' }}>
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', marginBottom: '6px' }}>
                                        {label}
                                    </p>
                                    <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.onBg, margin: 0 }}>
                                        {value}
                                    </p>
                                </div>
                            ))}
                        </div>
                    </motion.section>
                </div>
            </main>

            <style>{`
                @keyframes pulse {
                    0%, 100% { opacity: 1; }
                    50% { opacity: 0.4; }
                }
                .material-symbols-outlined {
                    font-variation-settings: 'FILL' 0, 'wght' 300;
                }
            `}</style>
        </div>
    );
};

export default AdminDashboard;
