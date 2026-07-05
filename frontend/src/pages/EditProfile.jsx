import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import AuthService from '../services/auth.service';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg:         '#131313',
    surface:    '#131313',
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

const EditProfile = () => {
    const { isMobile } = useResponsive();
    const navigate  = useNavigate();
    const [loading,       setLoading]       = useState(true);
    const [saving,        setSaving]        = useState(false);
    const [toast,         setToast]         = useState(null);
    const [formData,      setFormData]      = useState({ email: '', fullName: '' });
    const [selectedPhoto, setSelectedPhoto] = useState(null);
    const [photoPreview,  setPhotoPreview]  = useState(null);
    const [currentPhotoUrl, setCurrentPhotoUrl] = useState(null);
    const [currentUser,   setCurrentUser]   = useState(null);
    const [sidebarOpen,   setSidebarOpen]   = useState(false);

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) setCurrentUser(user);
        api.get('/user/profile')
            .then(res => {
                setFormData({
                    email:       res.data.email       || '',
                    fullName:    res.data.fullName     || '',
                });
                setCurrentPhotoUrl(res.data.photoUrl);
            })
            .catch(() => {})
            .finally(() => setLoading(false));
    }, []);

    const handlePhotoChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;
        if (file.size > 5 * 1024 * 1024) { showToast('error', 'File size must be less than 5MB'); return; }
        setSelectedPhoto(file);
        const reader = new FileReader();
        reader.onloadend = () => setPhotoPreview(reader.result);
        reader.readAsDataURL(file);
    };

    const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

    const showToast = (type, msg) => {
        setToast({ type, msg });
        setTimeout(() => setToast(null), 3500);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            if (selectedPhoto) {
                const fd = new FormData();
                fd.append('photo', selectedPhoto);
                await api.post('/user/profile/photo', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
            }
            await api.put('/user/profile', formData);
            showToast('success', 'Identity Updated Successfully.');
            setTimeout(() => navigate('/dashboard'), 1800);
        } catch (err) {
            showToast('error', err.response?.data?.message || err.response?.data || 'Failed to update profile');
            setSaving(false);
        }
    };

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
            Loading...
        </div>
    );

    const displayName = currentUser?.username || formData.fullName || 'Architect';
    const initials    = displayName.charAt(0).toUpperCase();

    return (
        <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif" }}>
            {/* ── Main Content ── */}
            <main style={{ flex: 1, overflowY: 'auto', padding: isMobile ? '24px 16px' : '48px 64px' }}>

                {/* Header */}
                <motion.header
                    initial={{ opacity: 0, y: 16 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5 }}
                    style={{ marginBottom: '3rem', paddingBottom: '2rem', borderBottom: `1px solid ${C.border}` }}
                >
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '32px', fontWeight: 600, color: C.onBg, marginBottom: '8px' }}>
                        Edit Identity
                    </h1>
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline, lineHeight: 1.5, maxWidth: '560px' }}>
                        Modify structural parameters and system representation. All changes are logged to the immutable architecture registry.
                    </p>
                </motion.header>

                {/* Grid */}
                <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 2fr', gap: '32px', alignItems: 'start' }}>

                    {/* Left: Avatar + Stats */}
                    <motion.aside
                        initial={{ opacity: 0, x: -16 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.5, delay: 0.1 }}
                        style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}
                    >
                        {/* Avatar panel */}
                        <div style={{ backgroundColor: C.surface, border: `1px solid ${C.border}`, padding: '2rem', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}
                            className="avatar-panel"
                        >
                            <div
                                style={{ width: '160px', height: '160px', borderRadius: '50%', border: `1px solid ${C.border}`, overflow: 'hidden', marginBottom: '1.5rem', position: 'relative', cursor: 'pointer' }}
                                onClick={() => document.getElementById('photo-upload').click()}
                            >
                                {photoPreview || currentPhotoUrl ? (
                                    <img src={photoPreview || currentPhotoUrl} alt="Avatar" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                ) : (
                                    <div style={{ width: '100%', height: '100%', backgroundColor: C.surfaceCon, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '56px', fontWeight: 700, color: C.primary }}>{initials}</span>
                                    </div>
                                )}
                                <div className="avatar-overlay" style={{ position: 'absolute', inset: 0, backgroundColor: 'rgba(19,19,19,0.6)', backdropFilter: 'blur(4px)', display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: 0, transition: 'opacity 0.3s' }}>
                                    <span style={{ color: C.primary, fontSize: '32px' }}>↑</span>
                                </div>
                            </div>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '6px' }}>Visual Node</span>
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: 'rgba(157,142,131,0.5)', marginBottom: '1.5rem' }}>1024×1024 max.</p>
                            <input type="file" id="photo-upload" accept="image/*" onChange={handlePhotoChange} style={{ display: 'none' }} />
                            <button type="button" onClick={() => document.getElementById('photo-upload').click()}
                                style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.secondary, background: 'none', border: 'none', cursor: 'pointer', textTransform: 'uppercase', textDecoration: 'underline', textDecorationColor: C.border, transition: 'color 0.2s' }}
                                onMouseEnter={e => e.currentTarget.style.color = C.primary}
                                onMouseLeave={e => e.currentTarget.style.color = C.secondary}
                            >
                                Initialize Upload
                            </button>
                        </div>

                        {/* Node Stats */}
                        <div style={{ backgroundColor: C.surface, border: `1px solid ${C.border}`, borderTop: `2px solid ${C.secondary}`, padding: '2rem' }}>
                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 600, color: C.onBg, marginBottom: '1.5rem' }}>Node Statistics</h3>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                {[
                                    { label: 'Username',  value: displayName },
                                    { label: 'Clearance', value: 'Architect' },
                                ].map(({ label, value }) => (
                                    <div key={label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', borderBottom: `1px solid rgba(80,69,59,0.3)`, paddingBottom: '10px' }}>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline }}>{label}</span>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted, letterSpacing: '0.05em' }}>{value}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </motion.aside>

                    {/* Right: Form */}
                    <motion.div
                        initial={{ opacity: 0, x: 16 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.5, delay: 0.15 }}
                        style={{ backgroundColor: C.surface, border: `1px solid ${C.border}`, padding: '3rem', position: 'relative', overflow: 'hidden' }}
                    >
                        <div style={{ position: 'absolute', top: 0, right: 0, width: '256px', height: '256px', background: 'rgba(241,188,139,0.04)', filter: 'blur(80px)', borderRadius: '50%', pointerEvents: 'none' }} />
                        <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 600, color: C.onBg, marginBottom: '2.5rem', paddingBottom: '1rem', borderBottom: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <span style={{ color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '18px' }}>&gt;_</span>
                            Parameter Input
                        </h2>
                        <form onSubmit={handleSubmit}>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '40px 48px' }}>
                                <div style={{ gridColumn: 'span 2' }}>
                                    <TerminalField label="Designation / Full Name" name="fullName" type="text" value={formData.fullName} onChange={handleChange} placeholder="Enter designation" required />
                                </div>
                                <TerminalField label="Comms / Email"          name="email"       type="email" value={formData.email}       onChange={handleChange} placeholder="Enter comms address" required />
                                <div style={{ gridColumn: 'span 2', display: 'flex', justifyContent: 'flex-end', gap: '24px', marginTop: '1rem', paddingTop: '2rem', borderTop: `1px solid rgba(80,69,59,0.5)` }}>
                                    <button type="button" onClick={() => navigate(-1)}
                                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', padding: '12px 24px', transition: 'color 0.2s' }}
                                        onMouseEnter={e => e.currentTarget.style.color = C.onBg}
                                        onMouseLeave={e => e.currentTarget.style.color = C.outline}
                                    >
                                        Cancel
                                    </button>
                                    <button type="submit" disabled={saving}
                                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', border: `1px solid ${C.secondary}`, color: saving ? C.outline : C.secondary, backgroundColor: 'transparent', padding: '12px 32px', cursor: saving ? 'not-allowed' : 'pointer', opacity: saving ? 0.5 : 1, transition: 'all 0.3s' }}
                                        onMouseEnter={e => { if (!saving) { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; } }}
                                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = saving ? C.outline : C.secondary; }}
                                    >
                                        {saving ? 'Saving...' : 'Save Changes'}
                                    </button>
                                </div>
                            </div>
                        </form>
                    </motion.div>
                </div>
            </main>
            {/* Toast */}
            {toast && (
                <motion.div
                    initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }}
                    style={{ position: 'fixed', bottom: '2rem', right: '2rem', backgroundColor: C.surfaceLow, borderLeft: `2px solid ${toast.type === 'success' ? C.secondary : C.error}`, padding: '1rem 1.5rem', display: 'flex', alignItems: 'center', gap: '16px', maxWidth: '360px', zIndex: 100, boxShadow: '0 8px 32px rgba(0,0,0,0.5)' }}
                >
                    <div>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.onBg, textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>System Notification</span>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.muted }}>{toast.msg}</span>
                    </div>
                    <button onClick={() => setToast(null)} style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', color: C.outline, fontSize: '18px' }}>×</button>
                </motion.div>
            )}

            <style>{`
                .avatar-panel:hover .avatar-overlay { opacity: 1 !important; }
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
            `}</style>
        </div>
    );
};

/* ── Terminal-style underline input ── */
const TerminalField = ({ label, name, type, value, onChange, placeholder, required }) => {
    const [focused, setFocused] = useState(false);
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: '#9d8e83', textTransform: 'uppercase' }}>
                {label}
            </label>
            <div style={{ position: 'relative' }}>
                <span style={{ position: 'absolute', left: 0, bottom: '10px', color: '#e9c176', opacity: 0.5, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px' }}>
                    &gt;
                </span>
                <input
                    type={type} name={name} value={value} onChange={onChange}
                    placeholder={placeholder} required={required}
                    onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
                    style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${focused ? '#e9c176' : '#50453b'}`, color: '#e5e2e1', fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', padding: '8px 0 8px 24px', outline: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box' }}
                />
            </div>
        </div>
    );
};

export default EditProfile;
