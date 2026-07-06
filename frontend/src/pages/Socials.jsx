import { useState, useEffect, useRef } from 'react';
import React from 'react';
import { motion, AnimatePresence, useScroll, useTransform } from 'framer-motion';
import api from '../services/api';
import AuthService from '../services/auth.service';
import useResponsive from '../hooks/useResponsive';
import AchievementPoster, { TIERS } from '../components/AchievementPoster';

const C = {
    bg: '#131313', surface: '#191919', surfaceHi: '#222222',
    border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176',
    muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1',
    error: '#ffb4ab', success: '#4ade80', warning: '#facc15',
};

const SOCIAL_LINKS = [
    { key: 'githubUrl', label: 'GitHub', icon: 'code', color: '#8b949e', placeholder: 'https://github.com/username' },
    { key: 'linkedinUrl', label: 'LinkedIn', icon: 'badge', color: '#0a66c2', placeholder: 'https://linkedin.com/in/username' },
    { key: 'instagramUrl', label: 'Instagram', icon: 'photo_camera', color: '#E1306C', placeholder: 'https://instagram.com/username' },
    { key: 'twitterUrl', label: 'Twitter', icon: 'raven', color: '#1DA1F2', placeholder: 'https://twitter.com/username' },
    { key: 'websiteUrl', label: 'Website', icon: 'language', color: C.secondary, placeholder: 'https://yourwebsite.com' },
];

// ── Decorative elements ───────────────────────────────────────────────────
const GridCanvas = ({ opacity = 0.04, spacing = 20 }) => {
    const ref = useRef(null);
    useEffect(() => {
        const c = ref.current; if (!c) return;
        c.width = c.offsetWidth; c.height = c.offsetHeight;
        const ctx = c.getContext('2d');
        ctx.fillStyle = `rgba(229,226,225,${opacity})`;
        for (let x = 0; x < c.width; x += spacing)
            for (let y = 0; y < c.height; y += spacing)
                ctx.fillRect(x, y, 1, 1);
    }, [opacity, spacing]);
    return <canvas ref={ref} style={{ position: 'absolute', inset: 0, pointerEvents: 'none', zIndex: 0 }} />;
};

const SectionLabel = ({ children }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '28px' }}>
        <svg width="32" height="1"><line x1="0" y1="0" x2="32" y2="0" stroke={C.border} strokeWidth="1" /></svg>
        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.25em', color: C.outline, textTransform: 'uppercase' }}>{children}</span>
        <svg width="32" height="1"><line x1="0" y1="0" x2="32" y2="0" stroke={C.border} strokeWidth="1" /></svg>
    </div>
);

const CornerAccent = ({ pos }) => {
    const positions = {
        tl: { top: 0, left: 0, borderTop: '1px solid', borderLeft: '1px solid' },
        tr: { top: 0, right: 0, borderTop: '1px solid', borderRight: '1px solid' },
        bl: { bottom: 0, left: 0, borderBottom: '1px solid', borderLeft: '1px solid' },
        br: { bottom: 0, right: 0, borderBottom: '1px solid', borderRight: '1px solid' },
    };
    return <div style={{ position: 'absolute', width: 12, height: 12, borderColor: C.border, opacity: 0.5, ...positions[pos] }} />;
};

// ── Stat Blocks ───────────────────────────────────────────────────────────
const StatCard = ({ label, value, accent, icon, delay = 0 }) => (
    <motion.div
        initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true, margin: '-50px' }}
        transition={{ duration: 0.5, delay }}
        style={{ backgroundColor: C.surface, position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: 0, right: 0, width: '60%', height: '1px', background: `linear-gradient(90deg, transparent, ${accent}44)` }} />
        <div style={{ padding: '28px 24px 24px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '16px' }}>
                <span className="material-symbols-outlined" style={{ fontSize: '14px', color: accent }}>{icon}</span>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.14em', color: C.outline, textTransform: 'uppercase' }}>{label}</span>
            </div>
            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '44px', fontWeight: 600, color: C.onBg, lineHeight: 1 }}>{value ?? '—'}</span>
        </div>
    </motion.div>
);

// ── Form fields ───────────────────────────────────────────────────────────
const EditInput = ({ label, name, value, onChange, placeholder, type = 'text', autoFocus }) => {
    const [focused, setFocused] = useState(false);
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>{label}</label>
            <input type={type} name={name} value={value || ''} onChange={onChange} placeholder={placeholder}
                autoFocus={autoFocus} onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
                style={{ backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${focused ? C.primary : `${C.border}55`}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', padding: '8px 0', outline: 'none', transition: 'border-color 0.2s' }} />
        </div>
    );
};

const EditTextArea = ({ label, name, value, onChange, placeholder }) => {
    const [focused, setFocused] = useState(false);
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>{label}</label>
            <textarea name={name} value={value || ''} onChange={onChange} placeholder={placeholder} rows={3}
                onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
                style={{ backgroundColor: C.surface, border: `1px solid ${focused ? C.primary : `${C.border}55`}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', padding: '10px', outline: 'none', resize: 'vertical', transition: 'border-color 0.2s' }} />
        </div>
    );
};

const EMPTY_FORM = { fullName: '', email: '', bio: '', title: '', location: '', company: '', githubUrl: '', linkedinUrl: '', instagramUrl: '', twitterUrl: '', websiteUrl: '' };

// ═══════════════════════════════════════════════════════════════════════════
//  MAIN PAGE
// ═══════════════════════════════════════════════════════════════════════════
export default function Socials() {
    const { isMobile } = useResponsive();
    const containerRef = useRef(null);
    const { scrollYProgress } = useScroll({ target: containerRef, offset: ['start start', 'end end'] });
    const heroScale = useTransform(scrollYProgress, [0, 0.15], [1, 0.97]);
    const heroOpacity = useTransform(scrollYProgress, [0, 0.15], [1, 0.6]);

    const [loading, setLoading] = useState(true);
    const [profile, setProfile] = useState(null);
    const [stats, setStats] = useState(null);
    const [currentUser, setCurrentUser] = useState(null);
    const [editMode, setEditMode] = useState(false);
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState(null);
    const [form, setForm] = useState({ ...EMPTY_FORM });
    const [selectedPhoto, setSelectedPhoto] = useState(null);
    const [photoPreview, setPhotoPreview] = useState(null);

    const fetchData = () => {
        const user = AuthService.getCurrentUser();
        if (user) setCurrentUser(user);
        api.get('/user/profile').then(res => {
            const p = res.data;
            setProfile(p);
            setForm({
                fullName: p.fullName || user?.username || '', email: p.email || user?.email || '',
                bio: p.bio || '', title: p.title || '', location: p.location || '', company: p.company || '',
                githubUrl: p.githubUrl || '', linkedinUrl: p.linkedinUrl || '',
                instagramUrl: p.instagramUrl || '', twitterUrl: p.twitterUrl || '', websiteUrl: p.websiteUrl || '',
            });
            return api.get(`/user/profile/${p.username}`);
        }).then(res => setStats(res.data)).catch(() => {}).finally(() => setLoading(false));
    };

    useEffect(() => { fetchData(); }, []);

    const showToast = (type, msg) => { setToast({ type, msg }); setTimeout(() => setToast(null), 3500); };
    const handlePhotoChange = (e) => {
        const file = e.target.files[0]; if (!file) return;
        if (file.size > 5 * 1024 * 1024) { showToast('error', 'Max 5MB'); return; }
        setSelectedPhoto(file);
        const reader = new FileReader();
        reader.onloadend = () => setPhotoPreview(reader.result);
        reader.readAsDataURL(file);
    };
    const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });
    const handleSave = async () => {
        setSaving(true);
        try {
            if (selectedPhoto) {
                const fd = new FormData(); fd.append('photo', selectedPhoto);
                await api.post('/user/profile/photo', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
            }
            await api.put('/user/profile', form);
            showToast('success', 'Profile updated');
            setEditMode(false); fetchData();
        } catch (err) { showToast('error', err.response?.data?.message || 'Save failed'); }
        finally { setSaving(false); }
    };
    const handleCancel = () => {
        setEditMode(false); setSelectedPhoto(null); setPhotoPreview(null);
        if (profile) {
            setForm({
                fullName: profile.fullName || '', bio: profile.bio || '',
                title: profile.title || '', location: profile.location || '', company: profile.company || '',
                githubUrl: profile.githubUrl || '', linkedinUrl: profile.linkedinUrl || '',
                instagramUrl: profile.instagramUrl || '', twitterUrl: profile.twitterUrl || '', websiteUrl: profile.websiteUrl || '',
            });
        }
    };

    if (loading) return <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px' }}>Loading…</div>;

    const shownName = profile?.fullName || currentUser?.username || 'Player';
    const initials = shownName.charAt(0).toUpperCase();
    const userPoints = stats?.totalPoints ?? profile?.totalPoints ?? 0;

    return (
        <div ref={containerRef} style={{ minHeight: '100vh', backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", position: 'relative', overflow: 'hidden' }}>
            <GridCanvas opacity={0.03} spacing={32} />

            {/* ──────────────────────────────────────────────────────────────────
                HERO — asymmetric, full-width, off-center
               ────────────────────────────────────────────────────────────────── */}
            <motion.section style={{ scale: heroScale, opacity: heroOpacity }}
                className="hero-section">
                <div style={{
                    display: 'flex', flexDirection: isMobile ? 'column' : 'row',
                    minHeight: isMobile ? 'auto' : '85vh',
                    padding: isMobile ? '32px 20px' : '80px 64px 48px',
                    position: 'relative', gap: isMobile ? '32px' : '0',
                }}>
                    {/* Decorative vertical line */}
                    <div style={{ position: 'absolute', left: isMobile ? 20 : 64, top: 0, bottom: 0, width: '1px', background: `linear-gradient(to bottom, ${C.border}, transparent 60%)`, opacity: 0.4 }} />

                    {/* Left: Avatar + quick info — pushed down */}
                    <div style={{
                        flexShrink: 0, display: 'flex', flexDirection: 'column', alignItems: 'flex-start',
                        gap: '16px', paddingLeft: isMobile ? '32px' : '48px', paddingTop: isMobile ? '0' : '40px',
                    }}>
                        <div style={{ width: '120px', height: '120px', borderRadius: '50%', overflow: 'hidden', border: `2px solid ${C.secondary}33`, flexShrink: 0, position: 'relative' }}
                            onClick={() => editMode && document.getElementById('photo-upload').click()}
                            onMouseEnter={e => editMode && (e.currentTarget.style.cursor = 'pointer')}>
                            {photoPreview || profile?.photoUrl ? (
                                <img src={photoPreview || profile?.photoUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                            ) : (
                                <div style={{ width: '100%', height: '100%', backgroundColor: C.surface, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '48px', fontWeight: 700, color: C.primary }}>{initials}</span>
                                </div>
                            )}
                        </div>
                        {editMode && <input type="file" id="photo-upload" accept="image/*" onChange={handlePhotoChange} style={{ display: 'none' }} />}
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, letterSpacing: '0.06em' }}>@{currentUser?.username}</span>
                    </div>

                    {/* Right: Name + Bio — large, staggered */}
                    <div style={{
                        flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center',
                        paddingLeft: isMobile ? '32px' : '64px', position: 'relative',
                    }}>
                        {/* Floating decorative element */}
                        <div style={{ position: 'absolute', top: '-20px', right: '10%', width: '60px', height: '60px', border: `1px solid ${C.primary}15`, borderRadius: '50%', pointerEvents: 'none' }} />

                        <motion.div initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.8, delay: 0.15 }}>
                            <h1 style={{
                                fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '36px' : 'clamp(42px, 6vw, 72px)',
                                fontWeight: 700, color: C.onBg, margin: 0,
                                lineHeight: 1.05, letterSpacing: '-0.02em',
                                textShadow: `0 0 80px ${C.primary}0a`,
                            }}>
                                {shownName.split(' ').map((w, i) => (
                                    <span key={i} style={{ display: 'inline-block', marginRight: '0.3em' }}>
                                        {w}
                                    </span>
                                ))}
                            </h1>
                        </motion.div>

                        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.8, delay: 0.35 }}
                            style={{ marginTop: isMobile ? '16px' : '24px', maxWidth: '560px' }}>
                            {profile?.bio ? (
                                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '16px', color: C.outline, lineHeight: 1.75, margin: 0 }}>{profile.bio}</p>
                            ) : (
                                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, fontStyle: 'italic', opacity: 0.4 }}>No bio yet</p>
                            )}
                        </motion.div>

                        {/* Info pills — offset to the right */}
                        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, delay: 0.5 }}
                            style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', marginTop: '24px', marginLeft: isMobile ? '0' : '-20px' }}>
                            {profile?.title && <Pill icon="work" text={profile.title} color={C.primary} />}
                            {profile?.company && <Pill icon="apartment" text={profile.company} color={C.secondary} />}
                            {profile?.location && <Pill icon="location_on" text={profile.location} color={C.muted} />}
                        </motion.div>

                        {/* Edit button — floating at top right */}
                        <div style={{ position: isMobile ? 'relative' : 'absolute', top: isMobile ? '0' : '0', right: isMobile ? '0' : '0', marginTop: isMobile ? '20px' : '0' }}>
                            {!editMode ? (
                                <button onClick={() => setEditMode(true)}
                                    style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', textTransform: 'uppercase', border: `1px solid ${C.secondary}44`, color: C.secondary, backgroundColor: 'transparent', padding: '8px 24px', cursor: 'pointer', transition: 'all 0.3s' }}
                                    onMouseEnter={e => { e.currentTarget.style.backgroundColor = `${C.secondary}15`; e.currentTarget.style.borderColor = C.secondary; }}
                                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.borderColor = `${C.secondary}44`; }}>
                                    Edit Profile
                                </button>
                            ) : (
                                <div style={{ display: 'flex', gap: '10px' }}>
                                    <button onClick={handleCancel} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', textTransform: 'uppercase', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', padding: '8px 16px' }}>Cancel</button>
                                    <button onClick={handleSave} disabled={saving}
                                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', textTransform: 'uppercase', border: `1px solid ${C.success}55`, color: C.success, backgroundColor: 'transparent', padding: '8px 24px', cursor: saving ? 'not-allowed' : 'pointer', opacity: saving ? 0.5 : 1 }}
                                        onMouseEnter={e => { if (!saving) { e.currentTarget.style.backgroundColor = `${C.success}15`; e.currentTarget.style.borderColor = C.success; } }}
                                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.borderColor = `${C.success}55`; }}>
                                        {saving ? 'Saving…' : 'Save'}
                                    </button>
                                </div>
                            )}
                        </div>

                        {/* Edit form — slides in below */}
                        <AnimatePresence>
                            {editMode && (
                                <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }}
                                    transition={{ duration: 0.3 }}
                                    style={{ overflow: 'hidden', marginTop: '24px', backgroundColor: C.surface, padding: '24px', display: 'grid', gap: '14px', gridTemplateColumns: '1fr 1fr' }}>
                                    <EditInput label="Full Name" name="fullName" value={form.fullName} onChange={handleChange} placeholder="Full name" autoFocus />
                                    <EditInput label="Email" name="email" value={form.email} onChange={handleChange} placeholder="you@example.com" type="email" />
                                    <EditInput label="Title" name="title" value={form.title} onChange={handleChange} placeholder="e.g. Software Engineer" />
                                    <EditInput label="Company" name="company" value={form.company} onChange={handleChange} placeholder="Company / org" />
                                    <EditInput label="Location" name="location" value={form.location} onChange={handleChange} placeholder="e.g. Mumbai, India" />
                                    <div style={{ gridColumn: 'span 2' }}>
                                        <EditTextArea label="Bio" name="bio" value={form.bio} onChange={handleChange} placeholder="Tell others about yourself…" />
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </div>
                </div>
            </motion.section>

            {/* ──────────────────────────────────────────────────────────────────
                SOCIAL LINKS STRIP — horizontal, interactive
               ────────────────────────────────────────────────────────────────── */}
            <motion.section
                initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}
                transition={{ duration: 0.5 }}
                style={{
                    borderTop: `1px solid ${C.border}33`, borderBottom: `1px solid ${C.border}33`,
                    padding: '16px 0', marginBottom: '64px', position: 'relative',
                }}>
                <div style={{
                    display: 'flex', flexWrap: 'wrap', gap: '0', justifyContent: 'center',
                    padding: '0 20px',
                }}>
                    {SOCIAL_LINKS.map((s, i) => (
                        <React.Fragment key={s.key}>
                            <a href={profile?.[s.key] || '#'} target={profile?.[s.key] ? '_blank' : undefined} rel="noopener noreferrer"
                                onClick={e => { if (!profile?.[s.key]) e.preventDefault(); }}
                                style={{
                                    display: 'flex', alignItems: 'center', gap: '8px', padding: '12px 24px',
                                    textDecoration: 'none', color: profile?.[s.key] ? s.color : C.outline,
                                    fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.08em',
                                    opacity: profile?.[s.key] ? 1 : 0.25,
                                    transition: 'all 0.3s', position: 'relative',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.color = s.color;
                                    e.currentTarget.style.opacity = '1';
                                }}
                                onMouseLeave={e => {
                                    if (profile?.[s.key]) e.currentTarget.style.color = s.color;
                                    else e.currentTarget.style.opacity = '0.25';
                                }}>
                                <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>{s.icon}</span>
                                {s.label}
                                {profile?.[s.key] && <span style={{ fontSize: '12px', opacity: 0.4 }}>↗</span>}
                            </a>
                            {i < SOCIAL_LINKS.length - 1 && (
                                <div style={{ width: '1px', alignSelf: 'stretch', background: `${C.border}33`, margin: '4px 0' }} />
                            )}
                        </React.Fragment>
                    ))}
                </div>
            </motion.section>

            {/* ──────────────────────────────────────────────────────────────────
                PERFORMANCE — split contest vs practice
               ────────────────────────────────────────────────────────────────── */}
            <div style={{ padding: isMobile ? '0 20px' : '0 64px', marginBottom: '80px' }}>
                {/* Contest Performance */}
                <div style={{ marginBottom: '64px' }}>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: '24px', marginBottom: '24px' }}>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 600, color: C.primary }}>Contest Performance</span>
                        <div style={{ width: '40px', height: '1px', background: C.border, alignSelf: 'center' }} />
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>arena</span>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: `repeat(${isMobile ? 2 : 5}, 1fr)`, gap: '2px' }}>
                        <StatCard label="Problems Solved" value={stats?.contestProblemsSolved ?? stats?.problemsSolved ?? 0} accent={C.success} icon="check_circle" delay={0.05} />
                        <StatCard label="Submissions" value={stats?.contestTotalSubmissions ?? stats?.totalSubmissions ?? 0} accent={C.secondary} icon="send" delay={0.1} />
                        <StatCard label="Accept Rate" value={stats?.contestSuccessRate != null ? `${stats.contestSuccessRate}%` : (stats?.successRate != null ? `${stats.successRate}%` : '0%')} accent={C.primary} icon="trending_up" delay={0.15} />
                        <StatCard label="Contests Joined" value={stats?.contestsJoined ?? 0} accent={C.warning} icon="emoji_events" delay={0.2} />
                        <StatCard label="Total Points" value={stats?.totalPoints ?? profile?.totalPoints ?? 0} accent="#ff8c00" icon="stars" delay={0.25} />
                    </div>
                </div>

                {/* Practice Progress */}
                <div>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: '24px', marginBottom: '24px' }}>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 600, color: C.success }}>Practice Progress</span>
                        <div style={{ width: '40px', height: '1px', background: C.border, alignSelf: 'center' }} />
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>dojo</span>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: `repeat(${isMobile ? 2 : 3}, 1fr)`, gap: '2px' }}>
                        <StatCard label="Problems Solved" value={stats?.practiceProblemsSolved ?? 0} accent="#4ade80" icon="fitness_center" delay={0.05} />
                        <StatCard label="Points Earned"   value={stats?.practicePointsEarned ?? 0}   accent="#facc15" icon="workspace_premium" delay={0.1} />
                        <StatCard label="Total Points"    value={stats?.totalPoints ?? profile?.totalPoints ?? 0} accent="#60a5fa" icon="stars" delay={0.15} />
                    </div>
                </div>
            </div>

            {/* ──────────────────────────────────────────────────────────────────
                ACHIEVEMENTS — asymmetric masonry grid
               ────────────────────────────────────────────────────────────────── */}
            <div style={{ padding: isMobile ? '0 20px' : '0 64px', marginBottom: '80px' }}>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: '24px', marginBottom: '32px' }}>
                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 600, color: C.secondary }}>Achievements</span>
                    <div style={{ width: '40px', height: '1px', background: C.border, alignSelf: 'center' }} />
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>earned</span>
                </div>
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr 1fr',
                    gap: '2px',
                }}>
                    {TIERS.map((t, i) => {
                        const unlocked = userPoints >= t.min;
                        const span = isMobile ? {} : (
                            i === 1 || i === 4 ? { gridColumn: 'span 2' } : {}
                        );
                        return <div key={t.name} style={span}><AchievementPoster tier={t} unlocked={unlocked} /></div>;
                    })}
                </div>
            </div>

            {/* ── Toast ── */}
            <AnimatePresence>
                {toast && (
                    <motion.div initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 40 }}
                        style={{ position: 'fixed', bottom: '28px', right: '28px', backgroundColor: C.surface, borderLeft: `3px solid ${toast.type === 'success' ? C.success : C.error}`, padding: '14px 20px', display: 'flex', alignItems: 'center', gap: '16px', maxWidth: '340px', zIndex: 999, boxShadow: '0 8px 32px rgba(0,0,0,0.6)' }}>
                        <div>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.15em', color: C.onBg, textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>{toast.type}</span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted }}>{toast.msg}</span>
                        </div>
                        <button onClick={() => setToast(null)} style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', color: C.outline, fontSize: '18px' }}>×</button>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

// ── Inline Pill component ──────────────────────────────────────────────────
const Pill = ({ icon, text, color }) => (
    <span style={{
        display: 'inline-flex', alignItems: 'center', gap: '6px',
        padding: '5px 14px', border: `1px solid ${color || C.border}22`,
        fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
        color: color || C.muted, letterSpacing: '0.06em',
    }}>
        <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>{icon}</span>
        {text}
    </span>
);
