import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';

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

// ── Shared background layers (same as Login) ──────────────────────────────────
const BgLayers = () => (
    <>
        <div style={{ position: 'fixed', inset: 0, zIndex: 0, backgroundImage: 'url(/bg-login.webp)', backgroundSize: 'cover', backgroundPosition: 'center', backgroundRepeat: 'no-repeat' }} />
        <div style={{ position: 'fixed', inset: 0, zIndex: 1, background: 'rgba(19,19,19,0.72)' }} />
        <div style={{ position: 'fixed', inset: 0, zIndex: 2, backgroundImage: `linear-gradient(to right,${C.border} 1px,transparent 1px),linear-gradient(to bottom,${C.border} 1px,transparent 1px)`, backgroundSize: '64px 64px', opacity: 0.08 }} />
    </>
);

// ── Underline input ───────────────────────────────────────────────────────────
const UInput = ({ name, type = 'text', placeholder, value, onChange, children }) => (
    <div style={{ position: 'relative' }}>
        <input
            name={name}
            type={type}
            placeholder={placeholder}
            value={value}
            onChange={onChange}
            style={{
                width: '100%', backgroundColor: 'transparent',
                border: 'none', borderBottom: `1px solid ${C.border}`,
                color: C.onBg, fontFamily: "'JetBrains Mono', monospace",
                fontSize: '14px', padding: '10px 0',
                outline: 'none', transition: 'border-color 0.2s',
                boxSizing: 'border-box',
                paddingRight: children ? '36px' : '0',
            }}
            onFocus={e => e.target.style.borderBottomColor = C.secondary}
            onBlur={e => e.target.style.borderBottomColor = C.border}
        />
        {children}
    </div>
);

// ── Eye toggle button ─────────────────────────────────────────────────────────
const EyeBtn = ({ show, onClick }) => (
    <button type="button" onClick={onClick} style={{ position: 'absolute', right: 0, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: C.outline, padding: '4px' }}>
        <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            {show
                ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                : <><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></>
            }
        </svg>
    </button>
);

// ── Label ─────────────────────────────────────────────────────────────────────
const Label = ({ children }) => (
    <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: C.muted, textTransform: 'uppercase', display: 'block', marginBottom: '8px' }}>
        {children}
    </label>
);

const ResetPassword = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const token = searchParams.get('token');

    const [formData, setFormData]               = useState({ newPassword: '', confirmPassword: '' });
    const [loading, setLoading]                 = useState(false);
    const [validating, setValidating]           = useState(true);
    const [tokenValid, setTokenValid]           = useState(false);
    const [fullName, setFullName]               = useState('user');
    const [message, setMessage]                 = useState('');
    const [success, setSuccess]                 = useState(false);
    const [showNew, setShowNew]                 = useState(false);
    const [showConfirm, setShowConfirm]         = useState(false);
    const [passwordError, setPasswordError]     = useState('');

    useEffect(() => { validateToken(); }, [token]);

    const validateToken = async () => {
        if (!token) { setMessage('Invalid password reset link.'); setValidating(false); return; }
        try {
            const res = await api.get(`/auth/validate-reset-token/${token}`);
            setTokenValid(true);
            setFullName(res.data.fullName);
        } catch (err) {
            setTokenValid(false);
            setMessage(err.response?.data?.message || 'This password reset link is invalid or has expired.');
        } finally {
            setValidating(false);
        }
    };

    const validatePassword = (p) => {
        if (p.length < 8) return 'At least 8 characters required';
        if (!/[A-Z]/.test(p)) return 'Must contain an uppercase letter';
        if (!/[a-z]/.test(p)) return 'Must contain a lowercase letter';
        if (!/[!@#$%^&*(),.?":{}|<>]/.test(p)) return 'Must contain a special character';
        return '';
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (name === 'newPassword') setPasswordError(validatePassword(value));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage('');
        const err = validatePassword(formData.newPassword);
        if (err) { setMessage(err); return; }
        if (formData.newPassword !== formData.confirmPassword) { setMessage('Passwords do not match.'); return; }
        setLoading(true);
        try {
            const res = await api.post('/auth/reset-password', { token, newPassword: formData.newPassword });
            setSuccess(true);
            setMessage(res.data.message);
            setTimeout(() => navigate('/login'), 3000);
        } catch (err) {
            setSuccess(false);
            setMessage(err.response?.data?.message || 'An error occurred. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    // ── Password strength ─────────────────────────────────────────────────────
    const strength = (() => {
        const p = formData.newPassword;
        if (!p) return 0;
        let s = 0;
        if (p.length >= 8) s++;
        if (/[A-Z]/.test(p) && /[a-z]/.test(p)) s++;
        if (/[!@#$%^&*(),.?":{}|<>]/.test(p)) s++;
        if (p.length >= 12) s++;
        return Math.min(s, 4);
    })();
    const strengthLabel = ['—', 'Weak', 'Moderate', 'Adequate', 'Optimal'][strength];
    const strengthColor = [C.border, C.error, '#e9c176', C.primary, '#a8d5a2'][strength];

    // ── Loading state ─────────────────────────────────────────────────────────
    if (validating) return (
        <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: C.bg, position: 'relative' }}>
            <BgLayers />
            <div style={{ position: 'relative', zIndex: 10, textAlign: 'center' }}>
                <svg style={{ animation: 'spin 1s linear infinite', width: '48px', height: '48px', color: C.primary, margin: '0 auto 16px' }} viewBox="0 0 24 24">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" opacity="0.25" />
                    <path fill="currentColor" opacity="0.75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: C.muted }}>VALIDATING SECURE TOKEN...</p>
            </div>
            <style>{`@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}`}</style>
        </div>
    );

    // ── Invalid token ─────────────────────────────────────────────────────────
    if (!tokenValid) return (
        <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: C.bg, position: 'relative', padding: '24px' }}>
            <BgLayers />
            <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                style={{
                    position: 'relative', zIndex: 10,
                    backgroundColor: 'rgba(32,31,31,0.85)', backdropFilter: 'blur(20px)',
                    border: `1px solid ${C.error}`,
                    padding: '3rem', maxWidth: '480px', width: '100%', textAlign: 'center',
                }}
            >
                <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.error }} />
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
                <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '32px', fontWeight: 600, color: C.onBg, marginBottom: '12px' }}>Invalid Link</h2>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '16px', color: C.muted, marginBottom: '2rem', lineHeight: 1.6 }}>{message}</p>
                <StitchButton as={Link} to="/forgot-password">Request New Link</StitchButton>
            </motion.div>
        </div>
    );

    // ── Main reset form ───────────────────────────────────────────────────────
    return (
        <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: C.bg, position: 'relative', padding: '24px', fontFamily: "'Geist', sans-serif" }}>
            <BgLayers />

            <motion.div
                initial={{ opacity: 0, y: 24 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, ease: 'easeOut' }}
                style={{
                    position: 'relative', zIndex: 10,
                    width: '100%', maxWidth: '720px',
                    backgroundColor: 'rgba(32,31,31,0.85)',
                    backdropFilter: 'blur(20px)',
                    border: `1px solid ${C.border}`,
                    display: 'flex',
                    overflow: 'hidden',
                }}
            >
                {/* ── Left info panel ── */}
                <div style={{
                    width: '35%', flexShrink: 0,
                    backgroundColor: 'rgba(28,27,27,0.9)',
                    borderRight: `1px solid ${C.border}`,
                    padding: '2.5rem 2rem',
                    display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
                }}>
                    <div>
                        <div style={{ fontSize: '32px', marginBottom: '1rem' }}>🔑</div>
                        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.onBg, marginBottom: '12px', lineHeight: 1.2 }}>
                            Architectural Override
                        </h1>
                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.muted, lineHeight: 1.6 }}>
                            Reset your access credentials to regain control of the arena.
                        </p>
                    </div>
                    <div style={{ marginTop: '2rem' }}>
                        <div style={{
                            display: 'inline-flex', alignItems: 'center', gap: '8px',
                            border: `1px solid ${C.secondary}`,
                            padding: '8px 12px',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px', letterSpacing: '0.1em',
                            color: C.secondary,
                        }}>
                            ⏱ TOKEN EXPIRES IN 14:59
                        </div>
                    </div>
                </div>

                {/* ── Right form panel ── */}
                <div style={{ flex: 1, padding: '2.5rem', position: 'relative' }}>
                    {/* Amber top accent */}
                    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.secondary }} />

                    {/* Message */}
                    {message && !success && (
                        <motion.div
                            initial={{ opacity: 0, y: -8 }}
                            animate={{ opacity: 1, y: 0 }}
                            style={{
                                marginBottom: '1.5rem', padding: '10px 14px',
                                border: `1px solid ${C.error}`, borderLeft: `3px solid ${C.error}`,
                                backgroundColor: 'rgba(255,180,171,0.08)',
                                display: 'flex', alignItems: 'center', gap: '8px',
                            }}
                        >
                            <span style={{ color: C.error }}>⚠</span>
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>{message}</p>
                        </motion.div>
                    )}

                    {/* Success state */}
                    {success && (
                        <motion.div
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            style={{ textAlign: 'center', padding: '2rem 0' }}
                        >
                            <div style={{ fontSize: '48px', marginBottom: '16px' }}>✅</div>
                            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', color: C.secondary, marginBottom: '8px' }}>Identity Restored</h2>
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.muted, marginBottom: '1.5rem' }}>
                                Your credentials have been successfully updated.
                            </p>
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline }}>
                                Redirecting to login...
                            </p>
                        </motion.div>
                    )}

                    {/* Form */}
                    {!success && (
                        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>

                            {/* New Password */}
                            <div>
                                <Label>New Credential</Label>
                                <UInput name="newPassword" type={showNew ? 'text' : 'password'} placeholder="Enter new password" value={formData.newPassword} onChange={handleChange}>
                                    <EyeBtn show={showNew} onClick={() => setShowNew(!showNew)} />
                                </UInput>
                                {passwordError && formData.newPassword && (
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.error, marginTop: '4px' }}>{passwordError}</p>
                                )}
                                {!passwordError && formData.newPassword && (
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: '#a8d5a2', marginTop: '4px' }}>✓ Password meets requirements</p>
                                )}
                            </div>

                            {/* Confirm Password */}
                            <div>
                                <Label>Confirm Credential</Label>
                                <UInput name="confirmPassword" type={showConfirm ? 'text' : 'password'} placeholder="Confirm new password" value={formData.confirmPassword} onChange={handleChange}>
                                    <EyeBtn show={showConfirm} onClick={() => setShowConfirm(!showConfirm)} />
                                </UInput>
                                {formData.confirmPassword && formData.newPassword !== formData.confirmPassword && (
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.error, marginTop: '4px' }}>Passwords do not match</p>
                                )}
                                {formData.confirmPassword && formData.newPassword === formData.confirmPassword && (
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: '#a8d5a2', marginTop: '4px' }}>✓ Passwords match</p>
                                )}
                            </div>

                            {/* Strength bar */}
                            {formData.newPassword && (
                                <div>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.muted, textTransform: 'uppercase' }}>Strength</span>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: strengthColor, textTransform: 'uppercase' }}>{strengthLabel}</span>
                                    </div>
                                    <div style={{ display: 'flex', gap: '3px', height: '4px' }}>
                                        {[1, 2, 3, 4].map(i => (
                                            <div key={i} style={{ flex: 1, height: '100%', backgroundColor: i <= strength ? strengthColor : C.surfaceHi, transition: 'background-color 0.3s' }} />
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Actions */}
                            <div style={{ paddingTop: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
                                <Link
                                    to="/login"
                                    style={{
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '12px', letterSpacing: '0.1em',
                                        color: C.muted, textDecoration: 'none',
                                        display: 'flex', alignItems: 'center', gap: '6px',
                                        transition: 'color 0.2s',
                                    }}
                                    onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                                    onMouseLeave={e => e.currentTarget.style.color = C.muted}
                                >
                                    ← ABORT TO LOGIN
                                </Link>
                                <StitchButton type="submit" disabled={loading || !!passwordError || formData.newPassword !== formData.confirmPassword || !formData.newPassword}>
                                    {loading ? (
                                        <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                            <svg style={{ animation: 'spin 1s linear infinite', width: '14px', height: '14px' }} viewBox="0 0 24 24">
                                                <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" opacity="0.25" />
                                                <path fill="currentColor" opacity="0.75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                            </svg>
                                            Initializing...
                                        </span>
                                    ) : 'INITIALIZE OVERRIDE'}
                                </StitchButton>
                            </div>
                        </form>
                    )}
                </div>
            </motion.div>

            <style>{`@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}`}</style>
        </div>
    );
};

// ── Stitch slide-fill button ──────────────────────────────────────────────────
const StitchButton = ({ children, type = 'button', disabled = false, as: Tag, to }) => {
    const [hovered, setHovered] = React.useState(false);
    const style = {
        position: 'relative', overflow: 'hidden',
        backgroundColor: hovered && !disabled ? '#f1bc8b' : 'transparent',
        border: '1px solid #f1bc8b',
        color: hovered && !disabled ? '#131313' : '#f1bc8b',
        padding: '12px 24px',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.4 : 1,
        transition: 'all 0.3s',
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '12px', letterSpacing: '0.1em',
        fontWeight: 500, textTransform: 'uppercase',
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        textDecoration: 'none',
    };
    if (Tag) return <Tag to={to} style={style} onMouseEnter={() => setHovered(true)} onMouseLeave={() => setHovered(false)}>{children}</Tag>;
    return <button type={type} disabled={disabled} style={style} onMouseEnter={() => setHovered(true)} onMouseLeave={() => setHovered(false)}>{children}</button>;
};

export default ResetPassword;
