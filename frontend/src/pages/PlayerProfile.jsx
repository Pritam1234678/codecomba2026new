import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';

import SkeletonLoader from '../components/SkeletonLoader';
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

const SocialLink = ({ url, icon, label, color }) => {
    if (!url) return null;
    return (
        <a href={url} target="_blank" rel="noopener noreferrer"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '6px 14px', border: `1px solid ${C.border}`, textDecoration: 'none', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', transition: 'all 0.2s' }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = color; e.currentTarget.style.color = color; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}
        >
            <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>{icon}</span>
            {label}
        </a>
    );
};

const SOCIALS = [
    { key: 'githubUrl',    label: 'GitHub',    icon: 'code',           color: '#8b949e' },
    { key: 'linkedinUrl',  label: 'LinkedIn',  icon: 'badge',          color: '#0a66c2' },
    { key: 'instagramUrl', label: 'Instagram', icon: 'photo_camera',   color: '#E1306C' },
    { key: 'twitterUrl',   label: 'Twitter',   icon: 'raven',          color: '#1DA1F2' },
    { key: 'websiteUrl',   label: 'Website',   icon: 'language',       color: C.secondary },
];

const Pill = ({ icon, text, color }) => (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', padding: '3px 10px', border: `1px solid ${color || C.border}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: color || C.muted, letterSpacing: '0.06em' }}>
        <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>{icon}</span>
        {text}
    </span>
);

const StatBlock = ({ label, value, accent, icon }) => (
    <div style={{
        backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`,
        padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '8px',
        position: 'relative', overflow: 'hidden',
    }}>
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: accent || C.secondary, opacity: 0.5 }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="material-symbols-outlined" style={{ fontSize: '16px', color: accent || C.secondary }}>{icon}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>
                {label}
            </span>
        </div>
        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: accent || C.onBg, lineHeight: 1 }}>
            {value ?? '—'}
        </span>
    </div>
);

export default function PlayerProfile() {
    const { username } = useParams();
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [notFound, setNotFound] = useState(false);

    useEffect(() => {
        api.get(`/user/profile/${username}`)
            .then(res => setProfile(res.data))
            .catch(err => {
                if (err.response?.status === 404) setNotFound(true);
            })
            .finally(() => setLoading(false));
    }, [username]);

    if (loading) return <SkeletonLoader />;

    if (notFound || !profile) return (
        <div style={{ maxWidth: '500px', margin: '6rem auto', padding: '0 24px', textAlign: 'center' }}>
            <div style={{ border: `1px solid ${C.border}`, padding: '3rem', backgroundColor: C.surfaceLow }}>
                <span className="material-symbols-outlined" style={{ fontSize: '40px', color: C.border, display: 'block', marginBottom: '16px' }}>person_off</span>
                <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', color: C.muted, marginBottom: '8px' }}>Player Not Found</h2>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.outline, marginBottom: '24px' }}>
                    This player doesn't exist or their profile is private.
                </p>
                <button onClick={() => navigate('/players')} style={{ padding: '10px 24px', border: `1px solid ${C.secondary}`, color: C.secondary, background: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer' }}>
                    ← Back to Search
                </button>
            </div>
            <style>{`.material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }`}</style>
        </div>
    );

    const initials = (profile.fullName || profile.username || '?').charAt(0).toUpperCase();
    const successRateColor = profile.successRate >= 70 ? C.success : profile.successRate >= 40 ? C.secondary : C.error;

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh', padding: '48px 64px' }}>

            {/* Back */}
            <button
                onClick={() => navigate('/players')}
                style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'none', border: 'none', color: C.outline, cursor: 'pointer', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', marginBottom: '2rem', padding: 0, transition: 'color 0.2s' }}
                onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                onMouseLeave={e => e.currentTarget.style.color = C.outline}
            >
                <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>arrow_back</span>
                Players
            </button>

            {/* Hero */}
            <motion.section
                initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                style={{ display: 'flex', alignItems: 'flex-end', gap: '32px', marginBottom: '3rem', borderBottom: `1px solid ${C.border}`, paddingBottom: '2.5rem', flexWrap: 'wrap' }}
            >
                {/* Avatar */}
                <div style={{ position: 'relative', flexShrink: 0 }}>
                    <div style={{ width: '100px', height: '100px', borderRadius: '50%', border: `2px solid ${C.secondary}`, padding: '3px' }}>
                        {profile.photoUrl ? (
                            <img src={profile.photoUrl} alt={profile.username} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                        ) : (
                            <div style={{ width: '100%', height: '100%', borderRadius: '50%', backgroundColor: C.surfaceLow, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 700, color: C.primary }}>{initials}</span>
                            </div>
                        )}
                    </div>
                </div>

                {/* Name + username */}
                <div style={{ flex: 1 }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase' }}>
                        Player Profile
                    </span>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(28px, 4vw, 44px)', fontWeight: 700, lineHeight: 1.1, color: C.primary, margin: '0.4rem 0 0.5rem' }}>
                        {profile.fullName || profile.username}
                    </h1>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.outline }}>alternate_email</span>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline, letterSpacing: '0.08em' }}>
                            {profile.username}
                        </span>
                    </div>
                </div>

                {/* Points badge */}
                <div style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, padding: '1rem 1.5rem', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px', flexShrink: 0 }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '20px', color: C.secondary, fontVariationSettings: "'FILL' 1" }}>stars</span>
                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '32px', fontWeight: 600, color: C.secondary, lineHeight: 1 }}>
                        {profile.totalPoints ?? 0}
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>
                        Points
                    </span>
                </div>
            </motion.section>

            {/* Stats grid */}
            <motion.div
                initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '12px', marginBottom: '3rem' }}
            >
                <StatBlock label="Problems Solved"    value={profile.problemsSolved}     accent={C.primary}   icon="extension" />
                <StatBlock label="Total Submissions"  value={profile.totalSubmissions}   accent={C.muted}     icon="history_edu" />
                <StatBlock label="Accepted"           value={profile.acceptedSubmissions} accent={C.success}  icon="check_circle" />
                <StatBlock label="Success Rate"       value={`${profile.successRate}%`}  accent={successRateColor} icon="percent" />
                <StatBlock label="Contests Joined"    value={profile.contestsJoined}     accent={C.secondary} icon="emoji_events" />
            </motion.div>

            {/* Bio + Info + Socials */}
            {(profile.bio || profile.title || profile.company || profile.location || SOCIALS.some(s => profile[s.key])) && (
                <motion.div
                    initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4, delay: 0.2 }}
                    style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, padding: '1.5rem', marginBottom: '2rem' }}
                >
                    {profile.bio && (
                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.muted, lineHeight: 1.7, margin: '0 0 14px' }}>
                            {profile.bio}
                        </p>
                    )}
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginBottom: profile.bio ? '14px' : '0' }}>
                        {profile.title && <Pill icon="work" text={profile.title} color={C.primary} />}
                        {profile.company && <Pill icon="apartment" text={profile.company} color={C.secondary} />}
                        {profile.location && <Pill icon="location_on" text={profile.location} />}
                    </div>
                    {SOCIALS.some(s => profile[s.key]) && (
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', paddingTop: profile.bio || profile.title || profile.company || profile.location ? '12px' : '0', borderTop: profile.bio || profile.title || profile.company || profile.location ? `1px solid ${C.border}` : 'none' }}>
                            {SOCIALS.map(s => <SocialLink key={s.key} url={profile[s.key]} {...s} />)}
                        </div>
                    )}
                </motion.div>
            )}

            {/* Progress bar — success rate */}
            <motion.div
                initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4, delay: 0.2 }}
                style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, padding: '1.5rem', marginBottom: '2rem' }}
            >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>
                        Acceptance Rate
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: successRateColor, fontWeight: 600 }}>
                        {profile.successRate}%
                    </span>
                </div>
                <div style={{ height: '4px', backgroundColor: C.surfaceHi, borderRadius: '2px' }}>
                    <motion.div
                        initial={{ width: 0 }}
                        animate={{ width: `${Math.min(profile.successRate, 100)}%` }}
                        transition={{ duration: 1, delay: 0.4, ease: 'easeOut' }}
                        style={{ height: '100%', backgroundColor: successRateColor, borderRadius: '2px' }}
                    />
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '6px' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>
                        {profile.acceptedSubmissions} accepted
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>
                        {profile.totalSubmissions} total
                    </span>
                </div>
            </motion.div>

            <style>{`.material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }`}</style>
        </div>
    );
}
