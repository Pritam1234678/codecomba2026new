import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import AuthService from '../services/auth.service';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg: '#131313', surfaceCon: '#201f1f', surfaceLow: '#1c1b1b', surfaceHi: '#2a2a2a',
    border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7',
    outline: '#9d8e83', onBg: '#e5e2e1', error: '#ffb4ab', success: '#4ade80', warning: '#facc15',
};

const EMPTY = {
    displayName: '', bio: '', title: '', location: '', company: '',
    githubUrl: '', linkedinUrl: '', instagramUrl: '', twitterUrl: '', websiteUrl: '',
};

const SOCIALS = [
    { key: 'githubUrl',    label: 'GitHub',    icon: 'code',             placeholder: 'https://github.com/username',  color: '#8b949e' },
    { key: 'linkedinUrl',  label: 'LinkedIn',  icon: 'badge',            placeholder: 'https://linkedin.com/in/username', color: '#0a66c2' },
    { key: 'instagramUrl', label: 'Instagram', icon: 'photo_camera',    placeholder: 'https://instagram.com/username', color: '#E1306C' },
    { key: 'twitterUrl',   label: 'Twitter',   icon: 'raven',           placeholder: 'https://twitter.com/username',  color: '#1DA1F2' },
    { key: 'websiteUrl',   label: 'Website',   icon: 'language',         placeholder: 'https://yourwebsite.com',       color: C.secondary },
];

const StatBlock = ({ label, value, accent, icon }) => (
    <div style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '6px', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: accent || C.secondary, opacity: 0.6 }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span className="material-symbols-outlined" style={{ fontSize: '14px', color: accent || C.secondary }}>{icon}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>{label}</span>
        </div>
        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '30px', fontWeight: 600, color: accent || C.onBg, lineHeight: 1 }}>{value ?? '—'}</span>
    </div>
);

const SocialLink = ({ url, icon, label, color }) => {
    if (!url) return null;
    return (
        <a href={url} target="_blank" rel="noopener noreferrer"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '8px 16px', border: `1px solid ${C.border}`, textDecoration: 'none', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase', transition: 'all 0.25s' }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = color; e.currentTarget.style.color = color; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}
        >
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>{icon}</span>
            {label}
        </a>
    );
};

export default function EditProfile() {
    const { isMobile } = useResponsive();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [toast, setToast] = useState(null);
    const [profile, setProfile] = useState(null);
    const [form, setForm] = useState({ email: '', fullName: '', ...EMPTY });
    const [selectedPhoto, setSelectedPhoto] = useState(null);
    const [photoPreview, setPhotoPreview] = useState(null);
    const [currentUser, setCurrentUser] = useState(null);

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) setCurrentUser(user);
        api.get('/user/profile')
            .then(res => {
                const p = res.data;
                setProfile(p);
                setForm({
                    email: p.email || '',
                    fullName: p.fullName || '',
                    displayName: p.displayName || '',
                    bio: p.bio || '',
                    title: p.title || '',
                    location: p.location || '',
                    company: p.company || '',
                    githubUrl: p.githubUrl || '',
                    linkedinUrl: p.linkedinUrl || '',
                    instagramUrl: p.instagramUrl || '',
                    twitterUrl: p.twitterUrl || '',
                    websiteUrl: p.websiteUrl || '',
                });
            })
            .catch(() => {})
            .finally(() => setLoading(false));
    }, []);

    // Fetch stats separately for the stat blocks
    const [stats, setStats] = useState(null);
    useEffect(() => {
        if (!currentUser) return;
        const username = currentUser.username;
        api.get(`/user/profile/${username}`).then(r => setStats(r.data)).catch(() => {});
    }, [currentUser]);

    const showToast = (type, msg) => {
        setToast({ type, msg });
        setTimeout(() => setToast(null), 3500);
    };

    const handlePhotoChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;
        if (file.size > 5 * 1024 * 1024) { showToast('error', 'File size must be less than 5MB'); return; }
        setSelectedPhoto(file);
        const reader = new FileReader();
        reader.onloadend = () => setPhotoPreview(reader.result);
        reader.readAsDataURL(file);
    };

    const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            if (selectedPhoto) {
                const fd = new FormData();
                fd.append('photo', selectedPhoto);
                await api.post('/user/profile/photo', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
            }
            await api.put('/user/profile', form);
            showToast('success', 'Profile saved.');
            setEditMode(false);
            // Refresh
            const r = await api.get('/user/profile');
            setProfile(r.data);
            if (currentUser) {
                const s = await api.get(`/user/profile/${currentUser.username}`);
                setStats(s.data);
            }
        } catch (err) {
            showToast('error', err.response?.data?.message || 'Failed to save');
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <PageLoader />;

    const displayName = profile?.displayName || profile?.fullName || currentUser?.username || 'Player';
    const initials = displayName.charAt(0).toUpperCase();
    const activeSocials = SOCIALS.filter(s => profile?.[s.key]);
    const hasSocials = activeSocials.length > 0;

    return (
        <div style={{ minHeight: '100vh', backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif" }}>
            <main style={{ maxWidth: '1000px', margin: '0 auto', padding: isMobile ? '24px 16px' : '48px 40px' }}>
                {/* ── Header ── */}
                <motion.header initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                    style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '3rem', paddingBottom: '2rem', borderBottom: `1px solid ${C.border}`, flexWrap: 'wrap', gap: '16px' }}
                >
                    <div>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>Profile</span>
                        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '32px', fontWeight: 600, color: C.onBg, margin: '4px 0 0' }}>
                            {profile?.fullName || displayName}
                        </h1>
                        {profile?.title && <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.muted, marginTop: '4px' }}>{profile.title}{profile?.company ? ` • ${profile.company}` : ''}</p>}
                    </div>
                    <button onClick={() => setEditMode(!editMode)}
                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.12em', textTransform: 'uppercase', border: `1px solid ${editMode ? C.error : C.secondary}`, color: editMode ? C.error : C.secondary, backgroundColor: 'transparent', padding: '8px 20px', cursor: 'pointer', transition: 'all 0.25s' }}
                    >
                        {editMode ? 'View Profile' : 'Edit Profile'}
                    </button>
                </motion.header>

                {!editMode ? (
                    /* ── VIEW MODE ── */
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
                        {/* Top: avatar + quick stats */}
                        <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'auto 1fr', gap: '28px', alignItems: 'start' }}>
                            <div style={{ width: '140px', height: '140px', borderRadius: '50%', border: `1px solid ${C.border}`, overflow: 'hidden', flexShrink: 0 }}>
                                {profile?.photoUrl ? (
                                    <img src={profile.photoUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                ) : (
                                    <div style={{ width: '100%', height: '100%', backgroundColor: C.surfaceCon, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '52px', fontWeight: 700, color: C.primary }}>{initials}</span>
                                    </div>
                                )}
                            </div>
                            <div>
                                <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 600, color: C.primary, margin: '0 0 4px' }}>
                                    {profile?.displayName || profile?.fullName || currentUser?.username}
                                </h2>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, letterSpacing: '0.08em' }}>
                                    @{currentUser?.username}
                                </span>
                                {profile?.bio && (
                                    <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.muted, lineHeight: 1.7, marginTop: '16px', maxWidth: '600px' }}>
                                        {profile.bio}
                                    </p>
                                )}
                                {/* Location & info pills */}
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', marginTop: '14px' }}>
                                    {profile?.location && <Pill icon="location_on" text={profile.location} />}
                                    {profile?.company && <Pill icon="apartment" text={profile.company} />}
                                </div>
                                {/* Social links */}
                                {hasSocials && (
                                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', marginTop: '18px' }}>
                                        {SOCIALS.map(s => <SocialLink key={s.key} url={profile?.[s.key]} {...s} />)}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Stats grid */}
                        <div>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '16px' }}>Statistics</span>
                            <div style={{ display: 'grid', gridTemplateColumns: `repeat(${isMobile ? 2 : 5}, 1fr)`, gap: '16px' }}>
                                <StatBlock label="Problems Solved" value={stats?.problemsSolved ?? '—'} accent={C.success} icon="check_circle" />
                                <StatBlock label="Submissions"    value={stats?.totalSubmissions ?? '—'} accent={C.secondary} icon="send" />
                                <StatBlock label="Accept Rate"    value={stats?.successRate != null ? `${stats.successRate}%` : '—'} accent={C.primary} icon="trending_up" />
                                <StatBlock label="Contests"       value={stats?.contestsJoined ?? '—'} accent={C.warning} icon="emoji_events" />
                                <StatBlock label="Points"         value={profile?.totalPoints ?? stats?.totalPoints ?? 0} accent="#ff8c00" icon="stars" />
                            </div>
                        </div>
                    </div>
                ) : (
                    /* ── EDIT MODE ── */
                    <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
                        <form onSubmit={handleSave}>
                            {/* Avatar */}
                            <div style={{ display: 'flex', alignItems: 'center', gap: '20px', marginBottom: '2.5rem' }}>
                                <div style={{ width: '100px', height: '100px', borderRadius: '50%', border: `1px solid ${C.border}`, overflow: 'hidden', cursor: 'pointer', position: 'relative' }}
                                    onClick={() => document.getElementById('photo-upload').click()}>
                                    {photoPreview || profile?.photoUrl ? (
                                        <img src={photoPreview || profile?.photoUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                    ) : (
                                        <div style={{ width: '100%', height: '100%', backgroundColor: C.surfaceCon, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '40px', fontWeight: 700, color: C.primary }}>{initials}</span>
                                        </div>
                                    )}
                                    <div style={{ position: 'absolute', inset: 0, backgroundColor: 'rgba(19,19,19,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: 0, transition: 'opacity 0.2s' }}
                                        className="avatar-edit-hover">
                                        <span className="material-symbols-outlined" style={{ color: C.primary, fontSize: '28px' }}>upload</span>
                                    </div>
                                </div>
                                <input type="file" id="photo-upload" accept="image/*" onChange={handlePhotoChange} style={{ display: 'none' }} />
                                <div>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted }}>Click to change photo</span>
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, marginTop: '4px' }}>JPEG/PNG • max 5MB</p>
                                </div>
                            </div>

                            {/* Fields grid */}
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px 32px' }}>
                                <FullField label="Display Name"     name="displayName" value={form.displayName} onChange={handleChange} placeholder="How others see you" />
                                <FullField label="Email"            name="email"       value={form.email}       onChange={handleChange} placeholder="you@example.com" />
                                <FullField label="Full Name"        name="fullName"    value={form.fullName}    onChange={handleChange} placeholder="Full legal name" />
                                <FullField label="Title / Role"     name="title"       value={form.title}       onChange={handleChange} placeholder="e.g. Software Engineer" />
                                <FullField label="Location"         name="location"    value={form.location}    onChange={handleChange} placeholder="e.g. Mumbai, India" />
                                <FullField label="Company"          name="company"     value={form.company}     onChange={handleChange} placeholder="Your company or org" />
                                <div style={{ gridColumn: 'span 2' }}>
                                    <TextAreaField label="Bio" name="bio" value={form.bio} onChange={handleChange} placeholder="Tell others a bit about yourself..." />
                                </div>
                                <div style={{ gridColumn: 'span 2', display: 'flex', flexDirection: 'column', gap: '16px', paddingTop: '16px', borderTop: `1px solid ${C.border}` }}>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>Social Links</span>
                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px 32px' }}>
                                        {SOCIALS.map(s => (
                                            <FullField key={s.key} label={s.label} name={s.key} value={form[s.key]} onChange={handleChange} placeholder={s.placeholder} />
                                        ))}
                                    </div>
                                </div>
                            </div>

                            {/* Action buttons */}
                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '20px', marginTop: '2rem', paddingTop: '1.5rem', borderTop: `1px solid ${C.border}` }}>
                                <button type="button" onClick={() => setEditMode(false)}
                                    style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.12em', textTransform: 'uppercase', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', padding: '10px 24px' }}>
                                    Cancel
                                </button>
                                <button type="submit" disabled={saving}
                                    style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.12em', textTransform: 'uppercase', border: `1px solid ${C.secondary}`, color: saving ? C.outline : C.secondary, backgroundColor: 'transparent', padding: '10px 28px', cursor: saving ? 'not-allowed' : 'pointer', opacity: saving ? 0.5 : 1, transition: 'all 0.3s' }}
                                    onMouseEnter={e => { if (!saving) { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; } }}
                                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = saving ? C.outline : C.secondary; }}>
                                    {saving ? 'Saving...' : 'Save Changes'}
                                </button>
                            </div>
                        </form>
                    </motion.div>
                )}
            </main>

            {/* Toast */}
            {toast && (
                <motion.div initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }}
                    style={{ position: 'fixed', bottom: '2rem', right: '2rem', backgroundColor: C.surfaceLow, borderLeft: `3px solid ${toast.type === 'success' ? C.success : C.error}`, padding: '1rem 1.5rem', display: 'flex', alignItems: 'center', gap: '16px', maxWidth: '360px', zIndex: 100, boxShadow: '0 8px 32px rgba(0,0,0,0.5)' }}>
                    <div>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.onBg, textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>{toast.type}</span>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>{toast.msg}</span>
                    </div>
                    <button onClick={() => setToast(null)} style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', color: C.outline, fontSize: '18px' }}>×</button>
                </motion.div>
            )}
            <style>{`.avatar-edit-hover:hover { opacity: 1 !important; }`}</style>
        </div>
    );
}

const Pill = ({ icon, text }) => (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '4px 12px', border: `1px solid ${C.border}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted, letterSpacing: '0.06em' }}>
        <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.outline }}>{icon}</span>
        {text}
    </span>
);

const FullField = ({ label, name, value, onChange, placeholder }) => {
    const [focused, setFocused] = useState(false);
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>{label}</label>
            <input type="text" name={name} value={value} onChange={onChange} placeholder={placeholder}
                onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
                style={{ backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${focused ? C.secondary : C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', padding: '8px 0', outline: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box' }} />
        </div>
    );
};

const TextAreaField = ({ label, name, value, onChange, placeholder }) => {
    const [focused, setFocused] = useState(false);
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>{label}</label>
            <textarea name={name} value={value} onChange={onChange} placeholder={placeholder} rows={4}
                onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
                style={{ backgroundColor: C.surfaceLow, border: `1px solid ${focused ? C.secondary : C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', padding: '12px', outline: 'none', resize: 'vertical', transition: 'border-color 0.2s', borderRadius: '4px' }} />
        </div>
    );
};

const PageLoader = () => (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>Loading...</div>
);
