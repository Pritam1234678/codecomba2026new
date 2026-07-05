import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import api from '../services/api';
import AuthService from '../services/auth.service';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg: '#131313', surfaceCon: '#201f1f', surfaceLow: '#1c1b1b', surfaceHi: '#2a2a2a',
    border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7',
    outline: '#9d8e83', onBg: '#e5e2e1', error: '#ffb4ab', success: '#4ade80', warning: '#facc15',
};

const SOCIAL_LINKS = [
    { key: 'githubUrl',    label: 'GitHub',    icon: 'code',           color: '#8b949e' },
    { key: 'linkedinUrl',  label: 'LinkedIn',  icon: 'badge',          color: '#0a66c2' },
    { key: 'instagramUrl', label: 'Instagram', icon: 'photo_camera',   color: '#E1306C' },
    { key: 'twitterUrl',   label: 'Twitter',   icon: 'raven',          color: '#1DA1F2' },
    { key: 'websiteUrl',   label: 'Website',   icon: 'language',       color: C.secondary },
];

const StatCard = ({ label, value, accent, icon }) => (
    <motion.div
        initial={{ opacity: 0, y: 12 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ duration: 0.4 }}
        style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: '22px 20px', display: 'flex', flexDirection: 'column', gap: '8px', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '3px', backgroundColor: accent || C.secondary, opacity: 0.7 }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="material-symbols-outlined" style={{ fontSize: '16px', color: accent || C.secondary }}>{icon}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.14em', color: C.outline, textTransform: 'uppercase' }}>{label}</span>
        </div>
        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: accent || C.onBg, lineHeight: 1 }}>{value ?? '—'}</span>
    </motion.div>
);

const SocialPill = ({ url, icon, label, color }) => {
    if (!url) return null;
    return (
        <a href={url} target="_blank" rel="noopener noreferrer"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '10px 20px', border: `1px solid ${C.border}`, textDecoration: 'none', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', transition: 'all 0.3s', position: 'relative', overflow: 'hidden' }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = color; e.currentTarget.style.color = color; e.currentTarget.style.backgroundColor = `${color}14`; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; e.currentTarget.style.backgroundColor = 'transparent'; }}>
            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{icon}</span>
            {label}
            <span style={{ marginLeft: '2px', fontSize: '14px', opacity: 0.5 }}>↗</span>
        </a>
    );
};

const EmptyField = ({ icon, label }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 0', opacity: 0.35 }}>
        <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.outline }}>{icon}</span>
        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.06em', fontStyle: 'italic' }}>{label} — not set</span>
    </div>
);

export default function Socials() {
    const { isMobile } = useResponsive();
    const [loading, setLoading] = useState(true);
    const [profile, setProfile] = useState(null);
    const [stats, setStats] = useState(null);
    const [currentUser, setCurrentUser] = useState(null);

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) setCurrentUser(user);

        api.get('/user/profile').then(res => {
            setProfile(res.data);
            return api.get(`/user/profile/${res.data.username}`);
        }).then(res => {
            setStats(res.data);
        }).catch(() => {})
          .finally(() => setLoading(false));
    }, []);

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '70vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
            Loading...
        </div>
    );

    const displayName = profile?.displayName || profile?.fullName || currentUser?.username || 'Player';
    const initials = displayName.charAt(0).toUpperCase();
    const hasSocials = SOCIAL_LINKS.some(s => profile?.[s.key]);
    const hasInfo = profile?.title || profile?.company || profile?.location;

    return (
        <div style={{ minHeight: '100vh', backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif" }}>
            <main style={{ maxWidth: '960px', margin: '0 auto', padding: isMobile ? '28px 18px' : '52px 40px' }}>

                {/* ── Header ── */}
                <motion.header initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}
                    style={{ marginBottom: '2.5rem', paddingBottom: '1.8rem', borderBottom: `1px solid ${C.border}` }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.25em', color: C.outline, textTransform: 'uppercase' }}>Social Profile</span>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: C.primary, margin: '8px 0 0' }}>
                        Your Public Identity
                    </h1>
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, marginTop: '6px', maxWidth: '560px', lineHeight: 1.6 }}>
                        This is how other players see you on the platform. Edit your details from the{' '}
                        <span style={{ color: C.secondary }}>/profile/edit</span> page.
                    </p>
                </motion.header>

                {/* ── Hero Card ── */}
                <motion.section initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                    style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: isMobile ? '24px 20px' : '36px', display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? '24px' : '36px', alignItems: isMobile ? 'center' : 'flex-start', marginBottom: '2.5rem', position: 'relative', overflow: 'hidden' }}>
                    {/* Decorative corner */}
                    <div style={{ position: 'absolute', top: -60, right: -60, width: '200px', height: '200px', background: 'radial-gradient(circle, rgba(241,188,139,0.06) 0%, transparent 70%)', borderRadius: '50%', pointerEvents: 'none' }} />

                    {/* Avatar */}
                    <div style={{ width: '130px', height: '130px', borderRadius: '50%', border: `2px solid ${C.secondary}`, overflow: 'hidden', flexShrink: 0, position: 'relative', zIndex: 1 }}>
                        {profile?.photoUrl ? (
                            <img src={profile.photoUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        ) : (
                            <div style={{ width: '100%', height: '100%', backgroundColor: C.surfaceCon, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '52px', fontWeight: 700, color: C.primary }}>{initials}</span>
                            </div>
                        )}
                    </div>

                    {/* Details */}
                    <div style={{ flex: 1, minWidth: 0, position: 'relative', zIndex: 1 }}>
                        <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.primary, margin: '0 0 4px' }}>
                            {displayName}
                        </h2>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, letterSpacing: '0.06em', display: 'inline-block', marginBottom: '12px' }}>
                            @{currentUser?.username || 'player'}
                        </span>

                        {profile?.bio ? (
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.muted, lineHeight: 1.75, margin: '0 0 16px', maxWidth: '600px' }}>
                                {profile.bio}
                            </p>
                        ) : (
                            <EmptyField icon="description" label="Bio" />
                        )}

                        {/* Info pills */}
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginBottom: '12px' }}>
                            {profile?.title ? (
                                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '5px 14px', border: `1px solid ${C.primary}44`, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.primary, letterSpacing: '0.06em' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>work</span> {profile.title}
                                </span>
                            ) : <EmptyField icon="work" label="Title" />}
                            {profile?.company ? (
                                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '5px 14px', border: `1px solid ${C.secondary}44`, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.secondary, letterSpacing: '0.06em' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>apartment</span> {profile.company}
                                </span>
                            ) : <EmptyField icon="apartment" label="Company" />}
                            {profile?.location ? (
                                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '5px 14px', border: `1px solid ${C.muted}44`, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted, letterSpacing: '0.06em' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>location_on</span> {profile.location}
                                </span>
                            ) : <EmptyField icon="location_on" label="Location" />}
                        </div>
                    </div>
                </motion.section>

                {/* ── Social Links Section ── */}
                <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.2 }}
                    style={{ marginBottom: '2.5rem' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '16px' }}>Connected Platforms</span>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px' }}>
                        {SOCIAL_LINKS.map(s => (
                            profile?.[s.key] ? (
                                <SocialPill key={s.key} url={profile[s.key]} icon={s.icon} label={s.label} color={s.color} />
                            ) : (
                                <div key={s.key} style={{ padding: '10px 20px', border: `1px dashed ${C.border}`, opacity: 0.3, display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.outline }}>{s.icon}</span>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.08em', textTransform: 'uppercase' }}>{s.label}</span>
                                </div>
                            )
                        ))}
                    </div>
                </motion.section>

                {/* ── Stats Grid ── */}
                <div>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '16px' }}>Performance Metrics</span>
                    <div style={{ display: 'grid', gridTemplateColumns: `repeat(${isMobile ? 2 : 5}, 1fr)`, gap: '14px' }}>
                        <StatCard label="Problems Solved" value={stats?.problemsSolved ?? 0}     accent={C.success}    icon="check_circle" />
                        <StatCard label="Submissions"     value={stats?.totalSubmissions ?? 0}   accent={C.secondary}  icon="send" />
                        <StatCard label="Accept Rate"     value={stats?.successRate != null ? `${stats.successRate}%` : '0%'} accent={C.primary} icon="trending_up" />
                        <StatCard label="Contests"        value={stats?.contestsJoined ?? 0}      accent={C.warning}    icon="emoji_events" />
                        <StatCard label="Points"          value={stats?.totalPoints ?? profile?.totalPoints ?? 0} accent="#ff8c00" icon="stars" />
                    </div>
                </div>
            </main>
            <style>{`.material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300, 'GRAD' 0; }`}</style>
        </div>
    );
}
