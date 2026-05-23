import { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import contestService from '../services/contest.service';
import AdminService from '../services/admin.service';

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

export default function EditContest() {
    const { id }     = useParams();
    const navigate   = useNavigate();
    const [loading,  setLoading]  = useState(true);
    const [saving,   setSaving]   = useState(false);
    const [error,    setError]    = useState('');
    const [dirty,    setDirty]    = useState(false);
    const [toast,    setToast]    = useState(null);
    const [formData, setFormData] = useState({ name: '', description: '', startTime: '', endTime: '', active: false });
    const [original, setOriginal] = useState(null);

    useEffect(() => {
        contestService.getContestById(id)
            .then(res => {
                const c   = res.data;
                const fmt = d => d ? new Date(d).toISOString().slice(0, 16) : '';
                const data = { name: c.name || '', description: c.description || '', startTime: fmt(c.startTime), endTime: fmt(c.endTime), active: c.active || false };
                setFormData(data);
                setOriginal(data);
            })
            .catch(() => setError('Failed to load contest'))
            .finally(() => setLoading(false));
    }, [id]);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
        setDirty(true);
    };

    const showToast = (msg, type = 'success') => {
        setToast({ msg, type });
        setTimeout(() => setToast(null), 3000);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError('');
        try {
            await contestService.updateContest(id, formData);
            setDirty(false);
            showToast('Contest updated successfully.');
            setTimeout(() => navigate('/admin/contests'), 1500);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to update contest');
            setSaving(false);
        }
    };

    const handleDeactivate = () => {
        const action = formData.active ? AdminService.deactivateContest(id) : AdminService.activateContest(id);
        action.then(() => {
            setFormData(p => ({ ...p, active: !p.active }));
            setDirty(true);
            showToast(formData.active ? 'Contest deactivated.' : 'Contest activated.');
        }).catch(() => showToast('Failed to update status.', 'error'));
    };

    const calcDuration = () => {
        if (!formData.startTime || !formData.endTime) return '—';
        const diff = new Date(formData.endTime) - new Date(formData.startTime);
        if (diff <= 0) return 'Invalid';
        const h = Math.floor(diff / 3600000);
        const m = Math.floor((diff % 3600000) / 60000);
        return `${String(h).padStart(2,'0')}h : ${String(m).padStart(2,'0')}m : 00s`;
    };

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', letterSpacing: '0.1em' }}>
            Loading...
        </div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh' }}>

            {/* ── Task Header ── */}
            <header style={{ borderBottom: `1px solid ${C.border}`, backgroundColor: C.bg, position: 'sticky', top: 0, zIndex: 50, padding: '0 64px' }}>
                <div style={{ height: '80px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    {/* Left */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                        <button
                            onClick={() => navigate('/admin/contests')}
                            style={{ display: 'flex', alignItems: 'center', gap: '8px', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', transition: 'color 0.2s' }}
                            onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                            onMouseLeave={e => e.currentTarget.style.color = C.outline}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                            Contests
                        </button>
                        <div style={{ width: '1px', height: '32px', backgroundColor: C.border }} />
                        <div>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '2px' }}>Editing Mode</span>
                            <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 600, color: C.primary, maxWidth: '400px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                {formData.name || 'Contest'}
                            </h1>
                        </div>
                    </div>
                    {/* Right */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                        {dirty && (
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', animation: 'pulse 2s infinite' }}>
                                <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: C.secondary, display: 'inline-block' }} />
                                Unsaved Changes
                            </div>
                        )}
                        <button
                            form="edit-contest-form"
                            type="submit"
                            disabled={saving}
                            style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '12px 24px', border: `1px solid ${C.secondary}`, color: saving ? C.outline : C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: saving ? 'not-allowed' : 'pointer', opacity: saving ? 0.5 : 1, transition: 'all 0.2s' }}
                            onMouseEnter={e => { if (!saving) { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; } }}
                            onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = saving ? C.outline : C.secondary; }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>save</span>
                            {saving ? 'Saving...' : 'Save Changes'}
                        </button>
                    </div>
                </div>
            </header>

            {/* ── Main Workspace ── */}
            <main style={{ padding: '48px 64px', display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '32px', alignItems: 'start' }}>

                {/* ── Left: Form ── */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

                    {error && (
                        <div style={{ padding: '12px 16px', border: `1px solid ${C.error}`, borderLeft: `3px solid ${C.error}`, backgroundColor: 'rgba(255,180,171,0.06)', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                            {error}
                        </div>
                    )}

                    <form id="edit-contest-form" onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

                        {/* Panel: Contest Configuration */}
                        <section style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, position: 'relative' }}>
                            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.secondary, opacity: 0.4 }} />
                            <div style={{ padding: '2.5rem', display: 'flex', flexDirection: 'column', gap: '2.5rem' }}>
                                <div>
                                    <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 600, color: C.onBg, marginBottom: '6px' }}>Contest Configuration</h2>
                                    <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.outline }}>Define the primary parameters and public visibility rules.</p>
                                </div>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                                    {/* Title */}
                                    <UnderlineField label="Contest Title" name="name" type="text" value={formData.name} onChange={handleChange} placeholder="Enter contest title..." required />
                                    {/* Description */}
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>
                                            Description &amp; Rules
                                        </label>
                                        {/* Toolbar */}
                                        <div style={{ border: `1px solid ${C.border}`, backgroundColor: C.bg }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', borderBottom: `1px solid ${C.border}`, padding: '8px 12px', backgroundColor: C.surfaceMin }}>
                                                {['format_bold', 'format_italic', 'code', 'link'].map(icon => (
                                                    <button key={icon} type="button" style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.outline, transition: 'color 0.2s', padding: '2px' }}
                                                        onMouseEnter={e => e.currentTarget.style.color = C.primary}
                                                        onMouseLeave={e => e.currentTarget.style.color = C.outline}
                                                    >
                                                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{icon}</span>
                                                    </button>
                                                ))}
                                            </div>
                                            <textarea
                                                name="description"
                                                value={formData.description}
                                                onChange={handleChange}
                                                placeholder="Write contest rules..."
                                                rows={6}
                                                required
                                                style={{ width: '100%', backgroundColor: 'transparent', border: 'none', color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', padding: '1rem', outline: 'none', resize: 'vertical', boxSizing: 'border-box', lineHeight: 1.6 }}
                                            />
                                        </div>
                                    </div>
                                    {/* Visibility */}
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>
                                            Visibility State
                                        </label>
                                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                            {[
                                                { value: true,  label: 'Active',  desc: 'Visible on the main arena listings.' },
                                                { value: false, label: 'Draft',   desc: 'Hidden from public. Accessible via direct link.' },
                                            ].map(opt => (
                                                <label key={String(opt.value)} style={{ border: `1px solid ${formData.active === opt.value ? C.primary : C.border}`, padding: '1rem', cursor: 'pointer', display: 'flex', alignItems: 'flex-start', gap: '12px', backgroundColor: formData.active === opt.value ? 'rgba(241,188,139,0.05)' : 'transparent', transition: 'all 0.2s', position: 'relative' }}>
                                                    <input type="radio" name="active" checked={formData.active === opt.value} onChange={() => { setFormData(p => ({ ...p, active: opt.value })); setDirty(true); }} style={{ display: 'none' }} />
                                                    <div style={{ width: '16px', height: '16px', borderRadius: '50%', border: `1px solid ${formData.active === opt.value ? C.primary : C.border}`, backgroundColor: formData.active === opt.value ? C.primary : 'transparent', flexShrink: 0, marginTop: '2px', transition: 'all 0.2s' }} />
                                                    <div>
                                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.onBg, textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>{opt.label}</span>
                                                        <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.outline }}>{opt.desc}</span>
                                                    </div>
                                                </label>
                                            ))}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </section>

                        {/* Panel: Chronology */}
                        <section style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}>
                            <div style={{ padding: '2.5rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', borderBottom: `1px solid ${C.border}`, paddingBottom: '1rem' }}>
                                    <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 600, color: C.onBg }}>Chronology</h2>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>Timezone: Local</span>
                                </div>
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                            <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.secondary }}>play_circle</span>
                                            Start Time
                                        </label>
                                        <input type="datetime-local" name="startTime" value={formData.startTime} onChange={handleChange} required
                                            style={{ width: '100%', backgroundColor: C.bg, border: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', padding: '12px', outline: 'none', transition: 'border-color 0.2s', colorScheme: 'dark', boxSizing: 'border-box' }}
                                            onFocus={e => e.target.style.borderColor = C.secondary}
                                            onBlur={e => e.target.style.borderColor = C.border}
                                        />
                                    </div>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                            <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.error }}>stop_circle</span>
                                            End Time
                                        </label>
                                        <input type="datetime-local" name="endTime" value={formData.endTime} onChange={handleChange} required min={formData.startTime}
                                            style={{ width: '100%', backgroundColor: C.bg, border: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', padding: '12px', outline: 'none', transition: 'border-color 0.2s', colorScheme: 'dark', boxSizing: 'border-box' }}
                                            onFocus={e => e.target.style.borderColor = C.secondary}
                                            onBlur={e => e.target.style.borderColor = C.border}
                                        />
                                    </div>
                                    {/* Duration */}
                                    <div style={{ gridColumn: 'span 2', display: 'flex', alignItems: 'center', gap: '12px', backgroundColor: C.surfaceHi, padding: '1rem', borderLeft: `2px solid ${C.primary}` }}>
                                        <span className="material-symbols-outlined" style={{ color: C.primary, fontSize: '20px' }}>timer</span>
                                        <div>
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '2px' }}>Calculated Duration</span>
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', color: C.onBg }}>{calcDuration()}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </section>
                    </form>
                </div>

                {/* ── Right: Meta & Actions ── */}
                <aside style={{ display: 'flex', flexDirection: 'column', gap: '16px', position: 'sticky', top: '96px' }}>

                    {/* Manage Problems */}
                    <motion.div
                        whileHover={{ scale: 1.01 }}
                        style={{ border: `1px solid ${C.secondary}`, backgroundColor: 'rgba(96,68,3,0.1)', padding: '1.5rem', cursor: 'pointer', position: 'relative', overflow: 'hidden' }}
                        onClick={() => navigate(`/admin/contests/${id}/problems`)}
                    >
                        <div style={{ position: 'absolute', inset: 0, backgroundImage: 'radial-gradient(#e9c176 1px, transparent 1px)', backgroundSize: '8px 8px', opacity: 0.04, pointerEvents: 'none' }} />
                        <div style={{ position: 'relative', zIndex: 1 }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: `1px solid rgba(233,193,118,0.3)`, paddingBottom: '1rem', marginBottom: '1rem' }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase' }}>Workspace</span>
                                <span className="material-symbols-outlined" style={{ color: C.secondary, fontSize: '20px' }}>arrow_forward</span>
                            </div>
                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 600, color: C.onBg }}>Manage Problems</h3>
                        </div>
                    </motion.div>

                    {/* Entity Metadata */}
                    <div style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, padding: '1.5rem' }}>
                        <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.primary, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '12px', marginBottom: '12px' }}>
                            Entity Metadata
                        </h3>
                        {[
                            { label: 'ID',      value: `CC-${String(id).padStart(4,'0')}` },
                            { label: 'Status',  value: formData.active ? 'Active' : 'Draft' },
                            { label: 'Start',   value: formData.startTime ? new Date(formData.startTime).toLocaleDateString() : '—' },
                            { label: 'End',     value: formData.endTime   ? new Date(formData.endTime).toLocaleDateString()   : '—' },
                        ].map(({ label, value }) => (
                            <div key={label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: `1px solid rgba(80,69,59,0.3)` }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>{label}</span>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>{value}</span>
                            </div>
                        ))}
                    </div>

                    {/* System Actions */}
                    <div style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, padding: '1.5rem' }}>
                        <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '12px', marginBottom: '12px' }}>
                            System Actions
                        </h3>
                        <button
                            type="button"
                            onClick={handleDeactivate}
                            style={{ width: '100%', textAlign: 'left', display: 'flex', alignItems: 'center', gap: '12px', padding: '10px 0', background: 'none', border: 'none', cursor: 'pointer', color: C.muted, fontFamily: "'Geist', sans-serif", fontSize: '14px', transition: 'color 0.2s', borderBottom: `1px solid rgba(80,69,59,0.3)` }}
                            onMouseEnter={e => e.currentTarget.style.color = C.primary}
                            onMouseLeave={e => e.currentTarget.style.color = C.muted}
                        >
                            <span className="material-symbols-outlined" style={{ color: C.outline, fontSize: '18px' }}>pause_circle</span>
                            {formData.active ? 'Deactivate Contest' : 'Activate Contest'}
                        </button>
                        <button
                            type="button"
                            style={{ width: '100%', textAlign: 'left', display: 'flex', alignItems: 'center', gap: '12px', padding: '10px 0', background: 'none', border: 'none', cursor: 'pointer', color: 'rgba(255,180,171,0.7)', fontFamily: "'Geist', sans-serif", fontSize: '14px', transition: 'color 0.2s', marginTop: '4px' }}
                            onMouseEnter={e => e.currentTarget.style.color = C.error}
                            onMouseLeave={e => e.currentTarget.style.color = 'rgba(255,180,171,0.7)'}
                        >
                            <span className="material-symbols-outlined" style={{ color: 'rgba(255,180,171,0.5)', fontSize: '18px' }}>delete_forever</span>
                            Delete Permanently
                        </button>
                    </div>
                </aside>
            </main>

            {/* ── Unsaved Toast ── */}
            <AnimatePresence>
                {dirty && !toast && (
                    <motion.div
                        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 20 }}
                        style={{ position: 'fixed', bottom: '2rem', right: '2rem', border: `1px solid ${C.secondary}`, backgroundColor: C.bg, padding: '1rem 1.5rem', display: 'flex', alignItems: 'center', gap: '12px', zIndex: 100, boxShadow: '0 8px 32px rgba(0,0,0,0.5)' }}
                    >
                        <span className="material-symbols-outlined" style={{ color: C.secondary, fontSize: '18px' }}>warning</span>
                        <div>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.secondary, textTransform: 'uppercase', display: 'block' }}>Draft State</span>
                            <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.muted }}>You have unsaved changes.</span>
                        </div>
                    </motion.div>
                )}
                {toast && (
                    <motion.div
                        initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 40 }}
                        style={{ position: 'fixed', bottom: '2rem', right: '2rem', backgroundColor: C.surfaceLow, borderLeft: `2px solid ${toast.type === 'success' ? C.secondary : C.error}`, padding: '1rem 1.5rem', zIndex: 100, boxShadow: '0 8px 32px rgba(0,0,0,0.5)', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: toast.type === 'success' ? C.secondary : C.error, letterSpacing: '0.05em' }}
                    >
                        {toast.msg}
                    </motion.div>
                )}
            </AnimatePresence>

            <style>{`
                @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                ::-webkit-calendar-picker-indicator { filter: invert(1); opacity: 0.5; cursor: pointer; }
                ::-webkit-calendar-picker-indicator:hover { opacity: 1; }
            `}</style>
        </div>
    );
}

const UnderlineField = ({ label, name, type, value, onChange, placeholder, required }) => {
    const [focused, setFocused] = useState(false);
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: '#9d8e83', textTransform: 'uppercase' }}>{label}</label>
            <input
                type={type} name={name} value={value} onChange={onChange}
                placeholder={placeholder} required={required}
                onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
                style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${focused ? '#e9c176' : '#50453b'}`, color: '#e5e2e1', fontFamily: "'Geist', sans-serif", fontSize: '18px', padding: '8px 0', outline: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box' }}
            />
        </div>
    );
};
