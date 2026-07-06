import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import api from '../services/api';
import AuthService from '../services/auth.service';
import useResponsive from '../hooks/useResponsive';
import AchievementPoster from '../components/AchievementPoster';

const TIERS = [
    { name: 'Hello World',    min: 50   },
    { name: 'Bug Hunter',     min: 100  },
    { name: 'Problem Solver', min: 500  },
    { name: 'Algorithm Ace',  min: 1000 },
    { name: 'Code Architect', min: 2000 },
    { name: 'Coding Legend',  min: 5000 },
];

const C = {
    bg: '#131313', surfaceCon: '#201f1f', surfaceLow: '#1c1b1b', surfaceHi: '#2a2a2a',
    border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7',
    outline: '#9d8e83', onBg: '#e5e2e1', error: '#ffb4ab', success: '#4ade80', warning: '#facc15',
};

const SOCIAL_LINKS = [
    { key: 'githubUrl', label: 'GitHub', icon: 'code', color: '#8b949e', placeholder: 'https://github.com/username' },
    { key: 'linkedinUrl', label: 'LinkedIn', icon: 'badge', color: '#0a66c2', placeholder: 'https://linkedin.com/in/username' },
    { key: 'instagramUrl', label: 'Instagram', icon: 'photo_camera', color: '#E1306C', placeholder: 'https://instagram.com/username' },
    { key: 'twitterUrl', label: 'Twitter', icon: 'raven', color: '#1DA1F2', placeholder: 'https://twitter.com/username' },
    { key: 'websiteUrl', label: 'Website', icon: 'language', color: C.secondary, placeholder: 'https://yourwebsite.com' },
];

const StatCard = ({ label, value, accent, icon }) => (
    <motion.div initial={{ opacity: 0, y: 12 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ duration: 0.4 }}
        style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: '22px 20px', display: 'flex', flexDirection: 'column', gap: '8px', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '3px', backgroundColor: accent || C.secondary, opacity: 0.7 }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="material-symbols-outlined" style={{ fontSize: '16px', color: accent || C.secondary }}>{icon}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.14em', color: C.outline, textTransform: 'uppercase' }}>{label}</span>
        </div>
        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: accent || C.onBg, lineHeight: 1 }}>{value ?? '—'}</span>
    </motion.div>
);

const EditInput = ({ label, name, value, onChange, placeholder, type = 'text', autoFocus }) => {
    const [focused, setFocused] = useState(false);
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.18em', color: C.outline, textTransform: 'uppercase' }}>{label}</label>
            <input type={type} name={name} value={value || ''} onChange={onChange} placeholder={placeholder}
                autoFocus={autoFocus}
                onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
                style={{ backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${focused ? C.primary : C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', padding: '6px 0', outline: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box' }} />
        </div>
    );
};

const EditTextArea = ({ label, name, value, onChange, placeholder }) => {
    const [focused, setFocused] = useState(false);
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.18em', color: C.outline, textTransform: 'uppercase' }}>{label}</label>
            <textarea name={name} value={value || ''} onChange={onChange} placeholder={placeholder} rows={3}
                onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
                style={{ backgroundColor: C.surfaceLow, border: `1px solid ${focused ? C.primary : C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', padding: '10px', outline: 'none', resize: 'vertical', transition: 'border-color 0.2s', borderRadius: '4px' }} />
        </div>
    );
};

const Pill = ({ icon, text, color }) => (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', padding: '4px 12px', border: `1px solid ${color || C.border}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: color || C.muted, letterSpacing: '0.06em', borderRadius: '2px' }}>
        <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>{icon}</span>
        {text}
    </span>
);

const GhostField = ({ icon, label }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', opacity: 0.3 }}>
        <span className="material-symbols-outlined" style={{ fontSize: '13px', color: C.outline }}>{icon}</span>
        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, fontStyle: 'italic' }}>{label}</span>
    </div>
);

const EMPTY_FORM = { fullName: '', email: '', bio: '', title: '', location: '', company: '', githubUrl: '', linkedinUrl: '', instagramUrl: '', twitterUrl: '', websiteUrl: '' };

export default function Socials() {
    const { isMobile } = useResponsive();
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
                instagramUrl: p.instagramUrl || '', twitterUrl: p.twitterUrl || '',
                websiteUrl: p.websiteUrl || '',
            });
            return api.get(`/user/profile/${p.username}`);
        }).then(res => setStats(res.data)).catch(() => {}).finally(() => setLoading(false));
    };

    useEffect(() => { fetchData(); }, []);

    const showToast = (type, msg) => {
        setToast({ type, msg });
        setTimeout(() => setToast(null), 3500);
    };

    const handlePhotoChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;
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
                const fd = new FormData();
                fd.append('photo', selectedPhoto);
                await api.post('/user/profile/photo', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
            }
            await api.put('/user/profile', form);
            showToast('success', 'Profile updated');
            setEditMode(false);
            fetchData();
        } catch (err) {
            showToast('error', err.response?.data?.message || 'Save failed');
        } finally { setSaving(false); }
    };

    const handleCancel = () => {
        setEditMode(false);
        setSelectedPhoto(null);
        setPhotoPreview(null);
        if (profile) {
            setForm({
                fullName: profile.fullName || '', bio: profile.bio || '',
                title: profile.title || '', location: profile.location || '', company: profile.company || '',
                githubUrl: profile.githubUrl || '', linkedinUrl: profile.linkedinUrl || '',
                instagramUrl: profile.instagramUrl || '', twitterUrl: profile.twitterUrl || '',
                websiteUrl: profile.websiteUrl || '',
            });
        }
    };

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '70vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>Loading...</div>
    );

    const shownName = profile?.fullName || currentUser?.username || 'Player';
    const initials = shownName.charAt(0).toUpperCase();

    return (
        <div style={{ minHeight: '100vh', backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", paddingBottom: '4rem' }}>
            <main style={{ maxWidth: '960px', margin: '0 auto', padding: isMobile ? '28px 18px' : '52px 40px' }}>

                {/* ── Header ── */}
                <motion.header initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}
                    style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '2.5rem', paddingBottom: '1.8rem', borderBottom: `1px solid ${C.border}`, flexWrap: 'wrap', gap: '16px' }}>
                    <div>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.25em', color: C.outline, textTransform: 'uppercase' }}>Social Profile</span>
                        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: C.primary, margin: '8px 0 0' }}>
                            Your Public Identity
                        </h1>
                    </div>
                    {!editMode ? (
                        <button onClick={() => setEditMode(true)}
                            style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.12em', textTransform: 'uppercase', border: `1px solid ${C.secondary}`, color: C.secondary, backgroundColor: 'transparent', padding: '10px 28px', cursor: 'pointer', transition: 'all 0.25s', borderRadius: '2px' }}
                            onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                            onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.secondary; }}>
                            Edit
                        </button>
                    ) : (
                        <div style={{ display: 'flex', gap: '12px' }}>
                            <button onClick={handleCancel}
                                style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', padding: '10px 20px' }}>
                                Cancel
                            </button>
                            <button onClick={handleSave} disabled={saving}
                                style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', border: `1px solid ${C.success}`, color: saving ? C.outline : C.success, backgroundColor: 'transparent', padding: '10px 28px', cursor: saving ? 'not-allowed' : 'pointer', opacity: saving ? 0.5 : 1, borderRadius: '2px', transition: 'all 0.25s' }}
                                onMouseEnter={e => { if (!saving) { e.currentTarget.style.backgroundColor = C.success; e.currentTarget.style.color = C.bg; } }}
                                onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = saving ? C.outline : C.success; }}>
                                {saving ? 'Saving...' : 'Save'}
                            </button>
                        </div>
                    )}
                </motion.header>

                {/* ── Hero Card ── */}
                <motion.section initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                    style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: isMobile ? '24px 20px' : '36px', display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? '24px' : editMode ? '40px' : '36px', alignItems: isMobile ? 'center' : 'flex-start', marginBottom: '2.5rem', position: 'relative', overflow: 'hidden' }}>
                    <div style={{ position: 'absolute', top: -60, right: -60, width: '200px', height: '200px', background: 'radial-gradient(circle, rgba(241,188,139,0.06) 0%, transparent 70%)', borderRadius: '50%', pointerEvents: 'none' }} />

                    {/* Avatar */}
                    <div style={{ flexShrink: 0, position: 'relative', zIndex: 1 }}>
                        <div style={{ width: editMode ? '110px' : '130px', height: editMode ? '110px' : '130px', borderRadius: '50%', border: `2px solid ${C.secondary}`, overflow: 'hidden', cursor: editMode ? 'pointer' : 'default', transition: 'all 0.3s', position: 'relative' }}
                            onClick={() => editMode && document.getElementById('photo-upload').click()}>
                            {photoPreview || profile?.photoUrl ? (
                                <img src={photoPreview || profile?.photoUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                            ) : (
                                <div style={{ width: '100%', height: '100%', backgroundColor: C.surfaceCon, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: editMode ? '44px' : '52px', fontWeight: 700, color: C.primary }}>{initials}</span>
                                </div>
                            )}
                            {editMode && <div style={{ position: 'absolute', inset: 0, backgroundColor: 'rgba(19,19,19,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: 0, transition: 'opacity 0.2s' }} className="av-hover">
                                <span className="material-symbols-outlined" style={{ color: C.primary, fontSize: '28px' }}>photo_camera</span>
                            </div>}
                        </div>
                        {editMode && <input type="file" id="photo-upload" accept="image/*" onChange={handlePhotoChange} style={{ display: 'none' }} />}
                        {editMode && <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, textAlign: 'center', marginTop: '6px' }}>Click avatar to change</p>}
                    </div>

                    {/* Details */}
                    <div style={{ flex: 1, minWidth: 0, position: 'relative', zIndex: 1 }}>
                        <AnimatePresence mode="wait">
                            {!editMode ? (
                                <motion.div key="view" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={{ duration: 0.2 }}>
                                    <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.primary, margin: '0 0 4px' }}>{shownName}</h2>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, letterSpacing: '0.06em', display: 'inline-block', marginBottom: '14px' }}>@{currentUser?.username}</span>
                                    {profile?.bio ? (
                                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.muted, lineHeight: 1.75, margin: '0 0 16px', maxWidth: '600px' }}>{profile.bio}</p>
                                    ) : <GhostField icon="description" label="Bio — not set" />}
                                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                                        {profile?.title && <Pill icon="work" text={profile.title} color={C.primary} />}
                                        {profile?.company && <Pill icon="apartment" text={profile.company} color={C.secondary} />}
                                        {profile?.location && <Pill icon="location_on" text={profile.location} color={C.muted} />}
                                        {!profile?.title && !profile?.company && !profile?.location && <GhostField icon="info" label="Title, company & location — not set" />}
                                    </div>
                                </motion.div>
                            ) : (
                                <motion.div key="edit" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={{ duration: 0.2 }}
                                    style={{ display: 'grid', gap: '14px', gridTemplateColumns: '1fr 1fr' }}>
                                    <EditInput label="Full Name" name="fullName" value={form.fullName} onChange={handleChange} placeholder="Full name" autoFocus />
                                    <EditInput label="Email" name="email" value={form.email} onChange={handleChange} placeholder="you@example.com" type="email" />
                                    <EditInput label="Title" name="title" value={form.title} onChange={handleChange} placeholder="e.g. Software Engineer" />
                                    <EditInput label="Company" name="company" value={form.company} onChange={handleChange} placeholder="Company / org" />
                                    <EditInput label="Location" name="location" value={form.location} onChange={handleChange} placeholder="e.g. Mumbai, India" />
                                    <div style={{ gridColumn: 'span 2' }}>
                                        <EditTextArea label="Bio" name="bio" value={form.bio} onChange={handleChange} placeholder="Tell others about yourself..." />
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </div>
                </motion.section>

                {/* ── Social Links ── */}
                <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.2 }}
                    style={{ marginBottom: '2.5rem' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '16px' }}>Connected Platforms</span>

                    <AnimatePresence mode="wait">
                        {!editMode ? (
                            <motion.div key="view-socials" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} style={{ display: 'flex', flexWrap: 'wrap', gap: '12px' }}>
                                {SOCIAL_LINKS.map(s =>
                                    profile?.[s.key] ? (
                                        <a key={s.key} href={profile[s.key]} target="_blank" rel="noopener noreferrer"
                                            style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '10px 20px', border: `1px solid ${C.border}`, textDecoration: 'none', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', transition: 'all 0.25s', borderRadius: '2px' }}
                                            onMouseEnter={e => { e.currentTarget.style.borderColor = s.color; e.currentTarget.style.color = s.color; e.currentTarget.style.backgroundColor = `${s.color}14`; }}
                                            onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; e.currentTarget.style.backgroundColor = 'transparent'; }}>
                                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{s.icon}</span>
                                            {s.label} <span style={{ opacity: 0.5 }}>↗</span>
                                        </a>
                                    ) : (
                                        <div key={s.key} style={{ padding: '10px 20px', border: `1px dashed ${C.border}`, opacity: 0.25, display: 'flex', alignItems: 'center', gap: '8px', borderRadius: '2px' }}>
                                            <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.outline }}>{s.icon}</span>
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, textTransform: 'uppercase' }}>{s.label}</span>
                                        </div>
                                    )
                                )}
                            </motion.div>
                        ) : (
                            <motion.div key="edit-socials" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                                style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px 24px', backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: '24px' }}>
                                {SOCIAL_LINKS.map(s => (
                                    <EditInput key={s.key} label={s.label} name={s.key} value={form[s.key]} onChange={handleChange} placeholder={s.placeholder} />
                                ))}
                            </motion.div>
                        )}
                    </AnimatePresence>
                </motion.section>

                {/* ── Stats ── */}
                <div>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '16px' }}>Performance Metrics</span>
                    <div style={{ display: 'grid', gridTemplateColumns: `repeat(${isMobile ? 2 : 5}, 1fr)`, gap: '14px' }}>
                        <StatCard label="Solved"    value={stats?.problemsSolved ?? 0} accent={C.success} icon="check_circle" />
                        <StatCard label="Submissions" value={stats?.totalSubmissions ?? 0} accent={C.secondary} icon="send" />
                        <StatCard label="Accept Rate" value={stats?.successRate != null ? `${stats.successRate}%` : '0%'} accent={C.primary} icon="trending_up" />
                        <StatCard label="Contests"  value={stats?.contestsJoined ?? 0} accent={C.warning} icon="emoji_events" />
                        <StatCard label="Points"    value={stats?.totalPoints ?? profile?.totalPoints ?? 0} accent="#ff8c00" icon="stars" />
                    </div>
                </div>

                {/* ── Achievements ── */}
                <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.3 }}
                    style={{ marginTop: '2.5rem' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '16px' }}>Achievements</span>
                    <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: '24px', justifyContent: 'center', maxWidth: isMobile ? '100%' : '1152px', margin: '0 auto' }}>
                        {TIERS.map(t => {
                            const userPoints = stats?.totalPoints ?? profile?.totalPoints ?? 0;
                            const unlocked = userPoints >= t.min;
                            return <AchievementPoster key={t.name} tier={t} unlocked={unlocked} />;
                        })}
                    </div>
                </motion.div>
            </main>

            {/* ── Toast ── */}
            <AnimatePresence>
                {toast && (
                    <motion.div initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 40 }}
                        style={{ position: 'fixed', bottom: '2rem', right: '2rem', backgroundColor: C.surfaceLow, borderLeft: `3px solid ${toast.type === 'success' ? C.success : C.error}`, padding: '1rem 1.5rem', display: 'flex', alignItems: 'center', gap: '16px', maxWidth: '360px', zIndex: 999, boxShadow: '0 8px 32px rgba(0,0,0,0.5)' }}>
                        <div>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.onBg, textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>{toast.type}</span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>{toast.msg}</span>
                        </div>
                        <button onClick={() => setToast(null)} style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', color: C.outline, fontSize: '20px' }}>×</button>
                    </motion.div>
                )}
            </AnimatePresence>
            <style>{`.material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300, 'GRAD' 0; } .av-hover:hover { opacity: 1 !important; }`}</style>
        </div>
    );
}
