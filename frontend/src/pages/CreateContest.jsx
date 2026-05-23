import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import contestService from '../services/contest.service';

const C = {
    bg:         '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceMin: '#0e0e0e',
    border:     '#50453b',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
};

export default function CreateContest() {
    const navigate = useNavigate();
    const [loading,  setLoading]  = useState(false);
    const [error,    setError]    = useState('');
    const [formData, setFormData] = useState({ name: '', description: '', startTime: '', endTime: '', active: true });

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        try {
            await contestService.createContest(formData);
            navigate('/admin/contests');
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to create contest');
            setLoading(false);
        }
    };

    const getPreviewStatus = () => {
        if (!formData.startTime) return 'DRAFT';
        const now   = Date.now();
        const start = new Date(formData.startTime).getTime();
        const end   = formData.endTime ? new Date(formData.endTime).getTime() : 0;
        if (!formData.active) return 'DRAFT';
        if (now < start) return 'UPCOMING';
        if (end && now > end) return 'ENDED';
        return 'LIVE';
    };

    const previewStatus = getPreviewStatus();

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", padding: '48px 64px' }}>

            {/* ── Header ── */}
            <motion.header
                initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '3rem', paddingBottom: '2rem', borderBottom: `1px solid ${C.border}` }}
            >
                <div>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: C.primary, marginBottom: '8px' }}>
                        Create New Contest
                    </h1>
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                        Define the parameters for the next arena engagement.
                    </p>
                </div>
                <div style={{ display: 'flex', gap: '16px' }}>
                    <button
                        onClick={() => navigate('/admin/contests')}
                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', padding: '12px 20px', transition: 'color 0.2s' }}
                        onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                        onMouseLeave={e => e.currentTarget.style.color = C.outline}
                    >
                        Cancel
                    </button>
                    <button
                        form="create-contest-form"
                        type="submit"
                        disabled={loading}
                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', border: `1px solid ${C.secondary}`, color: loading ? C.outline : C.secondary, backgroundColor: 'transparent', padding: '12px 28px', cursor: loading ? 'not-allowed' : 'pointer', opacity: loading ? 0.5 : 1, transition: 'all 0.2s' }}
                        onMouseEnter={e => { if (!loading) { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; } }}
                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = loading ? C.outline : C.secondary; }}
                    >
                        {loading ? 'Creating...' : 'Create Contest'}
                    </button>
                </div>
            </motion.header>

            {/* ── Error ── */}
            {error && (
                <div style={{ padding: '12px 16px', border: `1px solid ${C.error}`, borderLeft: `3px solid ${C.error}`, backgroundColor: 'rgba(255,180,171,0.06)', marginBottom: '2rem', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                    {error}
                </div>
            )}

            {/* ── Asymmetric Grid ── */}
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '32px', alignItems: 'start' }}>

                {/* ── Left: Form ── */}
                <motion.div
                    initial={{ opacity: 0, x: -16 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                    style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}
                >
                    <form id="create-contest-form" onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

                        {/* Panel: Contest Details */}
                        <div style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, borderTop: `2px solid ${C.secondary}`, padding: '2rem' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2rem' }}>
                                <span className="material-symbols-outlined" style={{ color: C.secondary, fontSize: '18px' }}>edit_note</span>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Contest Details</span>
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                                <TerminalField label="Contest Name" name="name" type="text" value={formData.name} onChange={handleChange} placeholder="Enter contest name" required />
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>
                                        Description
                                    </label>
                                    <textarea
                                        name="description"
                                        value={formData.description}
                                        onChange={handleChange}
                                        placeholder="Describe the focus of this contest..."
                                        rows={4}
                                        required
                                        style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '15px', padding: '8px 0', outline: 'none', resize: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box' }}
                                        onFocus={e => e.target.style.borderBottomColor = C.secondary}
                                        onBlur={e => e.target.style.borderBottomColor = C.border}
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Panel: Schedule */}
                        <div style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: '2rem' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2rem' }}>
                                <span className="material-symbols-outlined" style={{ color: C.secondary, fontSize: '18px' }}>schedule</span>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Schedule</span>
                            </div>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
                                <TerminalField label="Start Time" name="startTime" type="datetime-local" value={formData.startTime} onChange={handleChange} required />
                                <TerminalField label="End Time" name="endTime" type="datetime-local" value={formData.endTime} onChange={handleChange} required />
                            </div>
                        </div>

                        {/* Panel: Visibility */}
                        <div style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: '1.5rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div>
                                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginBottom: '4px' }}>Visibility Status</p>
                                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.muted }}>Make this contest visible to the public arena.</p>
                            </div>
                            <label style={{ display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer' }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: formData.active ? C.secondary : C.outline, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                                    {formData.active ? 'Active' : 'Draft'}
                                </span>
                                <div style={{ position: 'relative', width: '44px', height: '24px' }}>
                                    <input type="checkbox" name="active" checked={formData.active} onChange={handleChange} style={{ opacity: 0, width: 0, height: 0, position: 'absolute' }} />
                                    <div
                                        onClick={() => setFormData(p => ({ ...p, active: !p.active }))}
                                        style={{ position: 'absolute', inset: 0, backgroundColor: formData.active ? 'rgba(96,68,3,0.5)' : C.surfaceMin, border: `1px solid ${formData.active ? C.secondary : C.border}`, borderRadius: '12px', cursor: 'pointer', transition: 'all 0.3s' }}
                                    >
                                        <div style={{ position: 'absolute', top: '3px', left: formData.active ? '22px' : '3px', width: '16px', height: '16px', borderRadius: '50%', backgroundColor: formData.active ? C.secondary : C.outline, transition: 'left 0.3s' }} />
                                    </div>
                                </div>
                            </label>
                        </div>
                    </form>
                </motion.div>

                {/* ── Right: Live Preview ── */}
                <motion.div
                    initial={{ opacity: 0, x: 16 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.5, delay: 0.15 }}
                    style={{ position: 'sticky', top: '2rem' }}
                >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Live Preview</span>
                        <span className="material-symbols-outlined" style={{ color: C.outline, fontSize: '16px' }}>visibility</span>
                    </div>

                    {/* Preview card */}
                    <div style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, borderTop: `2px solid ${C.secondary}`, padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.secondary, border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, padding: '3px 10px', textTransform: 'uppercase' }}>
                                {previewStatus}
                            </span>
                            <span className="material-symbols-outlined" style={{ color: C.primary, fontSize: '20px' }}>military_tech</span>
                        </div>

                        <div>
                            <h4 style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 600, color: C.primary, marginBottom: '8px', lineHeight: 1.2 }}>
                                {formData.name || 'Contest Name'}
                            </h4>
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.muted, lineHeight: 1.5 }}>
                                {formData.description || 'Contest description will appear here as you type...'}
                            </p>
                        </div>

                        <div style={{ paddingTop: '1rem', borderTop: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <span className="material-symbols-outlined" style={{ color: C.outline, fontSize: '14px' }}>schedule</span>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                    {formData.startTime ? new Date(formData.startTime).toLocaleDateString() : 'Set start time'}
                                </span>
                            </div>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: formData.active ? C.secondary : C.outline, letterSpacing: '0.08em', textTransform: 'uppercase', opacity: 0.6 }}>
                                Enter Arena
                            </span>
                        </div>
                    </div>

                    {/* Checklist */}
                    <div style={{ marginTop: '1.5rem', border: `1px solid ${C.border}`, padding: '1.5rem', backgroundColor: C.surfaceMin }}>
                        <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginBottom: '1rem' }}>Checklist</p>
                        {[
                            { label: 'Contest name set', done: !!formData.name },
                            { label: 'Description added', done: !!formData.description },
                            { label: 'Start time configured', done: !!formData.startTime },
                            { label: 'End time configured', done: !!formData.endTime },
                            { label: 'Visibility set', done: true },
                        ].map(({ label, done }) => (
                            <div key={label} style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                                <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: done ? C.secondary : C.border, flexShrink: 0 }} />
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: done ? C.muted : C.outline }}>{label}</span>
                            </div>
                        ))}
                    </div>
                </motion.div>
            </div>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                input[type="datetime-local"] { color-scheme: dark; }
            `}</style>
        </div>
    );
}

/* ── Terminal underline field ── */
const TerminalField = ({ label, name, type, value, onChange, placeholder, required }) => {
    const [focused, setFocused] = useState(false);
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: '#9d8e83', textTransform: 'uppercase' }}>
                {label}
            </label>
            <input
                type={type} name={name} value={value} onChange={onChange}
                placeholder={placeholder} required={required}
                onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
                style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${focused ? '#e9c176' : '#50453b'}`, color: '#e5e2e1', fontFamily: type === 'datetime-local' ? "'JetBrains Mono', monospace" : "'Geist', sans-serif", fontSize: '15px', padding: '8px 0', outline: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box', colorScheme: 'dark' }}
            />
        </div>
    );
};
