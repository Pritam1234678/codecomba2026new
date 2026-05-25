import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import AuthService from '../services/auth.service';
import useResponsive from '../hooks/useResponsive';

// ── Stitch Design Tokens ──────────────────────────────────────────────────────
const C = {
    bg:        '#131313',
    surface:   '#201f1f',
    surfaceLow:'#1c1b1b',
    surfaceHi: '#2a2a2a',
    border:    '#50453b',
    primary:   '#f1bc8b',
    secondary: '#e9c176',
    muted:     '#d4c4b7',
    outline:   '#9d8e83',
    error:     '#ffb4ab',
    onBg:      '#e5e2e1',
};

// ── Underline input ───────────────────────────────────────────────────────────
const Field = ({ label, name, type = 'text', placeholder, value, onChange, onBlur, error, required, children }) => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
        <label style={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '12px', letterSpacing: '0.1em',
            color: C.muted, textTransform: 'uppercase',
        }}>
            {label}{required && ' *'}
        </label>
        <div style={{ position: 'relative' }}>
            <input
                name={name}
                type={type}
                placeholder={placeholder}
                value={value}
                onChange={onChange}
                onBlur={onBlur}
                required={required}
                style={{
                    width: '100%',
                    backgroundColor: 'transparent',
                    border: 'none',
                    borderBottom: `1px solid ${error ? C.error : C.border}`,
                    color: C.onBg,
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '14px',
                    padding: '10px 0',
                    outline: 'none',
                    transition: 'border-color 0.2s',
                    boxSizing: 'border-box',
                    paddingRight: children ? '36px' : '0',
                }}
                onFocus={e => e.target.style.borderBottomColor = C.secondary}
                onBlur2={e => e.target.style.borderBottomColor = error ? C.error : C.border}
            />
            {children}
        </div>
        {error && (
            <span style={{
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '11px', color: C.error,
            }}>
                {error}
            </span>
        )}
    </div>
);

const Register = () => {
    const { isMobile } = useResponsive();
    const [formData, setFormData] = useState({
        username: '', email: '', password: '', fullName: ''
    });
    const [errors, setErrors]         = useState({});
    const [loading, setLoading]       = useState(false);
    const [message, setMessage]       = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const navigate = useNavigate();

    // ── Validation ────────────────────────────────────────────────────────────
    const validateField = (name, value, currentFormData) => {
        let error = '';
        const data = { ...currentFormData, [name]: value };
        const { username, email } = data;

        switch (name) {
            case 'username':
                if (value && /\s/.test(value)) error = 'Username must not contain spaces';
                break;
            case 'password': {
                const re = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%&*!]).{8,}$/;
                if (value && !re.test(value)) error = 'Password must contain uppercase, lowercase, and a special character';
                else if (value && (value === username || value === email)) error = 'Password cannot be the same as Username or Email';
                break;
            }
            case 'email':
                if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) error = 'Please enter a valid email address';
                break;
            default: break;
        }
        return error;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        setErrors(prev => ({ ...prev, [name]: validateField(name, value, formData) }));
    };

    const handleBlur = (e) => {
        const { name, value } = e.target;
        if (name === 'email') setErrors(prev => ({ ...prev, [name]: validateField(name, value, formData) }));
    };

    const handleRegister = (e) => {
        e.preventDefault();
        setMessage('');
        const formErrors = {};
        Object.keys(formData).forEach(k => {
            const err = validateField(k, formData[k], formData);
            if (err) formErrors[k] = err;
        });
        if (Object.keys(formErrors).length > 0) { setErrors(formErrors); return; }
        setLoading(true);
        AuthService.register(
            formData.username, formData.email, formData.password,
            formData.fullName
        ).then(
            () => { setLoading(false); setMessage('success'); setTimeout(() => navigate('/login'), 2000); },
            (error) => {
                const msg = error.response?.data?.message || error.message || error.toString();
                setLoading(false);
                setMessage(msg);
            }
        );
    };

    const isFormValid = () => {
        const hasErrors = Object.values(errors).some(e => e !== '');
        const hasEmpty  = ['username', 'email', 'password'].some(f => !formData[f]);
        return !hasErrors && !hasEmpty;
    };

    // Password strength
    const pwStrength = () => {
        const p = formData.password;
        if (!p) return 0;
        let s = 0;
        if (p.length >= 8) s++;
        if (/[A-Z]/.test(p) && /[a-z]/.test(p)) s++;
        if (/[@#$%&*!]/.test(p)) s++;
        return s;
    };
    const strength = pwStrength();
    const strengthLabel = ['—', 'Weak', 'Moderate', 'Optimal'][strength];
    const strengthColor = ['#50453b', '#ffb4ab', '#e9c176', '#f1bc8b'][strength];

    return (
        <div style={{
            backgroundColor: C.bg,
            minHeight: '100vh',
            display: 'flex',
            flexDirection: 'column',
            fontFamily: "'Geist', sans-serif",
        }}>
            {/* ── Background grid ── */}
            <div style={{
                position: 'fixed', inset: 0, pointerEvents: 'none', zIndex: 0,
                backgroundImage: `linear-gradient(to right,${C.border} 1px,transparent 1px),linear-gradient(to bottom,${C.border} 1px,transparent 1px)`,
                backgroundSize: '64px 64px', opacity: 0.07,
            }} />

            {/* ── Main content ── */}
            <main style={{
                position: 'relative', zIndex: 1,
                flexGrow: 1,
                display: 'grid',
                gridTemplateColumns: isMobile ? '1fr' : 'repeat(12, 1fr)',
                gap: isMobile ? '24px' : '32px',
                padding: isMobile ? '24px 16px' : '48px 64px',
                maxWidth: '1400px',
                margin: '0 auto',
                width: '100%',
                boxSizing: 'border-box',
            }}>

                {/* ── Left: Form ── */}
                <motion.div
                    initial={{ opacity: 0, y: 24 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6, ease: 'easeOut' }}
                    style={{ gridColumn: isMobile ? '1 / -1' : '1 / span 8', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}
                >
                    <h1 style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: isMobile ? '36px' : '72px', fontWeight: 700,
                        lineHeight: 1.1, letterSpacing: '-0.02em',
                        color: C.onBg, marginBottom: '3rem',
                    }}>
                        Join the Combat.
                    </h1>

                    {/* Message */}
                    {message && message !== 'success' && (
                        <motion.div
                            initial={{ opacity: 0, y: -8 }}
                            animate={{ opacity: 1, y: 0 }}
                            style={{
                                marginBottom: '2rem',
                                padding: '12px 16px',
                                border: `1px solid ${C.error}`,
                                borderLeft: `3px solid ${C.error}`,
                                backgroundColor: 'rgba(255,180,171,0.08)',
                                display: 'flex', alignItems: 'center', gap: '10px',
                            }}
                        >
                            <span style={{ color: C.error }}>⚠</span>
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.error }}>
                                {message}
                            </p>
                        </motion.div>
                    )}
                    {message === 'success' && (
                        <motion.div
                            initial={{ opacity: 0, y: -8 }}
                            animate={{ opacity: 1, y: 0 }}
                            style={{
                                marginBottom: '2rem', padding: '12px 16px',
                                border: `1px solid ${C.secondary}`,
                                borderLeft: `3px solid ${C.secondary}`,
                                backgroundColor: 'rgba(233,193,118,0.08)',
                            }}
                        >
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.secondary }}>
                                ✓ Registration successful! Redirecting to login...
                            </p>
                        </motion.div>
                    )}

                    <form onSubmit={handleRegister} style={{ maxWidth: '680px', display: 'flex', flexDirection: 'column', gap: '3rem' }}>

                        {/* ── Fieldset 1: Identity ── */}
                        <fieldset style={{ border: 'none', padding: 0, margin: 0 }}>
                            <legend style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', letterSpacing: '0.1em',
                                color: C.primary, textTransform: 'uppercase',
                                borderBottom: `1px solid ${C.border}`,
                                width: '100%', paddingBottom: '8px',
                                marginBottom: '1.5rem', display: 'block',
                            }}>
                                Identity Directive
                            </legend>
                            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: '24px', marginBottom: '24px' }}>
                                <Field label="Full Name" name="fullName" placeholder="ENTER NAME"
                                    value={formData.fullName} onChange={handleChange} error={errors.fullName} />
                                <Field label="Username" name="username" placeholder="CHOOSE ALIAS" required
                                    value={formData.username} onChange={handleChange} error={errors.username} />
                            </div>
                            <Field label="Email Address" name="email" type="email" placeholder="COMMUNICATION VECTOR" required
                                value={formData.email} onChange={handleChange} onBlur={handleBlur} error={errors.email} />
                        </fieldset>

                        {/* ── Fieldset 2: Security ── */}
                        <fieldset style={{ border: 'none', padding: 0, margin: 0 }}>
                            <legend style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', letterSpacing: '0.1em',
                                color: C.primary, textTransform: 'uppercase',
                                borderBottom: `1px solid ${C.border}`,
                                width: '100%', paddingBottom: '8px',
                                marginBottom: '1.5rem', display: 'block',
                            }}>
                                Security Clearance
                            </legend>
                            <div style={{ marginBottom: '24px' }}>
                                <Field label="Passcode" name="password" type={showPassword ? 'text' : 'password'}
                                    placeholder="ENCRYPT KEY" required
                                    value={formData.password} onChange={handleChange} error={errors.password}
                                >
                                    <button type="button" onClick={() => setShowPassword(!showPassword)}
                                        style={{ position: 'absolute', right: 0, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: C.outline, padding: '4px' }}>
                                        {showPassword ? (
                                            <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                                            </svg>
                                        ) : (
                                            <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                            </svg>
                                        )}
                                    </button>
                                </Field>
                            </div>
                            {/* Password strength bar */}
                            {formData.password && (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: C.muted, textTransform: 'uppercase' }}>Integrity</span>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: strengthColor, textTransform: 'uppercase' }}>{strengthLabel}</span>
                                    </div>
                                    <div style={{ height: '4px', width: '100%', backgroundColor: C.surfaceHi, display: 'flex', gap: '2px' }}>
                                        {[1, 2, 3].map(i => (
                                            <div key={i} style={{ flex: 1, height: '100%', backgroundColor: i <= strength ? strengthColor : C.surfaceHi, transition: 'background-color 0.3s' }} />
                                        ))}
                                    </div>
                                </div>
                            )}
                        </fieldset>

                        {/* ── Submit ── */}
                        <div style={{
                            paddingTop: '1.5rem',
                            borderTop: `1px solid ${C.border}`,
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '1rem',
                        }}>
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
                                <StitchButton type="submit" disabled={loading || !isFormValid()}>
                                    {loading ? (
                                        <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                            <svg style={{ animation: 'spin 1s linear infinite', width: '14px', height: '14px' }} viewBox="0 0 24 24">
                                                <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" opacity="0.25" />
                                                <path fill="currentColor" opacity="0.75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                            </svg>
                                            Initializing...
                                        </span>
                                    ) : 'Initialize Candidate'}
                                </StitchButton>
                                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted, maxWidth: '280px', lineHeight: 1.5 }}>
                                    Already enlisted?{' '}
                                    <Link to="/login" style={{ color: C.secondary, textDecoration: 'underline', textDecorationColor: C.secondary }}>
                                        Return to Arena
                                    </Link>
                                </p>
                            </div>
                        </div>
                    </form>
                </motion.div>
                <motion.div
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ duration: 0.6, delay: 0.2, ease: 'easeOut' }}
                    style={{
                        gridColumn: isMobile ? '1 / -1' : '9 / span 4',
                        display: isMobile ? 'none' : 'flex',
                        flexDirection: 'column',
                        justifyContent: 'center',
                        alignItems: 'flex-end',
                        position: 'relative',
                    }}
                >
                    <div style={{
                        border: `1px solid ${C.border}`,
                        backgroundColor: C.surfaceLow,
                        padding: '2rem',
                        width: '100%',
                        maxWidth: '320px',
                        position: 'relative',
                    }}>
                        {/* Top amber accent */}
                        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.secondary }} />

                        <h3 style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '12px', letterSpacing: '0.1em',
                            color: C.muted, textTransform: 'uppercase',
                            borderBottom: `1px solid ${C.border}`,
                            paddingBottom: '8px', marginBottom: '2rem',
                        }}>
                            Profile Telemetry
                        </h3>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', fontFamily: "'JetBrains Mono', monospace", fontSize: '14px' }}>
                            <TelemetryRow label="Status">
                                <span style={{ color: C.secondary, display: 'flex', alignItems: 'center', gap: '6px' }}>
                                    <span style={{ width: '6px', height: '6px', backgroundColor: C.secondary, borderRadius: '50%', display: 'inline-block' }} />
                                    {formData.username ? 'Candidate Active' : 'Awaiting Input'}
                                </span>
                            </TelemetryRow>

                            <TelemetryRow label="Alias">
                                <span style={{ color: formData.username ? C.onBg : `${C.onBg}50` }}>
                                    {formData.username || '_'}
                                </span>
                            </TelemetryRow>

                            <TelemetryRow label="Comm-Link">
                                <span style={{ color: formData.email ? C.onBg : `${C.onBg}50`, fontSize: '12px' }}>
                                    {formData.email || '_'}
                                </span>
                            </TelemetryRow>

                            <TelemetryRow label="Clearance Level">
                                <span style={{ color: C.onBg }}>Recruit [Level 0]</span>
                            </TelemetryRow>
                        </div>

                        {/* Terminal decoration */}
                        <div style={{
                            marginTop: '3rem', paddingTop: '1rem',
                            borderTop: `1px solid ${C.border}`,
                            display: 'flex', justifyContent: 'space-between',
                            alignItems: 'center', opacity: 0.5,
                        }}>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', color: C.muted }}>
                                &gt;_
                            </span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>
                                terminal
                            </span>
                        </div>
                    </div>
                </motion.div>
            </main>

            <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
        </div>
    );
};

// ── Telemetry row ─────────────────────────────────────────────────────────────
const TelemetryRow = ({ label, children }) => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
        <span style={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '10px', letterSpacing: '0.1em',
            color: '#9d8e83', textTransform: 'uppercase',
        }}>
            {label}
        </span>
        {children}
    </div>
);

// ── Stitch slide-fill button ──────────────────────────────────────────────────
const StitchButton = ({ children, type = 'button', disabled = false }) => {
    const [hovered, setHovered] = React.useState(false);
    return (
        <button
            type={type}
            disabled={disabled}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                position: 'relative', overflow: 'hidden',
                backgroundColor: hovered && !disabled ? '#e9c176' : 'transparent',
                border: '1px solid #e9c176',
                color: hovered && !disabled ? '#131313' : '#e9c176',
                padding: '12px 32px',
                cursor: disabled ? 'not-allowed' : 'pointer',
                opacity: disabled ? 0.4 : 1,
                transition: 'all 0.3s',
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px', letterSpacing: '0.1em',
                fontWeight: 500, textTransform: 'uppercase',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                minWidth: '200px',
            }}
        >
            {children}
        </button>
    );
};

export default Register;
