import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg:        '#131313',
    surfaceLow:'#1c1b1b',
    surfaceCon:'#201f1f',
    border:    '#50453b',
    primary:   '#f1bc8b',
    secondary: '#e9c176',
    muted:     '#d4c4b7',
    outline:   '#9d8e83',
    error:     '#ffb4ab',
    onBg:      '#e5e2e1',
};

const ForgotPassword = () => {
    const { isMobile } = useResponsive();
    const [formData, setFormData] = useState({ username: '', email: '', website: '', captchaAnswer: '' });
    const [loading, setLoading]   = useState(false);
    const [message, setMessage]   = useState('');
    const [success, setSuccess]   = useState(false);
    const [captcha, setCaptcha]   = useState({ token: '', question: '' });

    const refreshCaptcha = async () => {
        try {
            const res = await api.get('/auth/captcha');
            setCaptcha({ token: res.data.token, question: res.data.question });
            setFormData(prev => ({ ...prev, captchaAnswer: '' }));
        } catch (e) {
            setCaptcha({ token: '', question: 'Failed to load CAPTCHA' });
        }
    };
    useEffect(() => { refreshCaptcha(); }, []);

    const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage(''); setLoading(true);
        try {
            const res = await api.post('/auth/forgot-password', {
                username: formData.username,
                email: formData.email,
                website: formData.website,
                captchaToken: captcha.token,
                captchaAnswer: formData.captchaAnswer,
            });
            setSuccess(true);
            setMessage(res.data.message);
        } catch (err) {
            setSuccess(false);
            setMessage(err.response?.data?.message || 'An error occurred. Please try again.');
            refreshCaptcha();
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{
            flex: 1, display: 'flex', flexDirection: 'column',
            backgroundColor: C.bg, color: C.onBg,
            fontFamily: "'Geist', sans-serif",
            position: 'relative',
        }}>
            {/* ── Background image ── */}
            <div style={{ position: 'fixed', inset: 0, zIndex: 0, backgroundImage: 'url(/bg-forgot-password.webp)', backgroundSize: 'cover', backgroundPosition: 'center', backgroundRepeat: 'no-repeat' }} />
            <div style={{ position: 'fixed', inset: 0, zIndex: 1, background: 'rgba(19,19,19,0.75)' }} />
            <div style={{ position: 'fixed', inset: 0, zIndex: 2, backgroundImage: `linear-gradient(to right,${C.border} 1px,transparent 1px),linear-gradient(to bottom,${C.border} 1px,transparent 1px)`, backgroundSize: '64px 64px', opacity: 0.07 }} />

            {/* ── Main ── */}
            <main style={{
                flexGrow: 1, display: 'flex', flexDirection: 'column',
                alignItems: 'center', justifyContent: 'center',
                padding: isMobile ? '24px 16px' : '32px 64px',
                position: 'relative', zIndex: 10,
            }}>
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6, ease: 'easeOut' }}
                    style={{ width: '100%', maxWidth: '672px', display: 'flex', flexDirection: 'column', gap: '3rem' }}
                >
                    {/* Header section */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        <h1 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: isMobile ? '32px' : '48px', fontWeight: 700,
                            lineHeight: 1.1, color: C.primary, margin: 0,
                        }}>
                            Recovery Protocol
                        </h1>
                        <p style={{ fontSize: '18px', color: C.muted, lineHeight: 1.6, margin: 0 }}>
                            Initialize identity verification to restore access to the Arena.
                        </p>
                    </div>

                    {/* Step indicator */}
                    <div style={{
                        display: 'flex', alignItems: 'center', gap: '16px',
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '12px', letterSpacing: '0.1em', color: C.muted,
                    }}>
                        <span style={{ color: C.secondary, borderBottom: `2px solid ${C.secondary}`, paddingBottom: '4px' }}>1. Identify</span>
                        <span style={{ color: C.border }}>›</span>
                        <span style={{ opacity: 0.5 }}>2. Verify</span>
                        <span style={{ color: C.border }}>›</span>
                        <span style={{ opacity: 0.5 }}>3. Reset</span>
                    </div>

                    {/* Form panel */}
                    {!success ? (
                        <div style={{
                            border: `1px solid ${C.border}`,
                            padding: '3rem',
                            position: 'relative',
                            overflow: 'hidden',
                            backgroundColor: 'rgba(28,27,27,0.88)',
                            backdropFilter: 'blur(12px)',
                        }}>
                            {/* Amber top accent */}
                            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.secondary }} />

                            {/* Error */}
                            {message && (
                                <motion.div
                                    initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}
                                    style={{
                                        marginBottom: '2rem', padding: '10px 14px',
                                        border: `1px solid ${C.error}`, borderLeft: `3px solid ${C.error}`,
                                        backgroundColor: 'rgba(255,180,171,0.08)',
                                        display: 'flex', alignItems: 'center', gap: '8px',
                                    }}
                                >
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error, margin: 0 }}>{message}</p>
                                </motion.div>
                            )}

                            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                                {/* Username */}
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: C.primary, textTransform: 'uppercase' }}>
                                        Username
                                    </label>
                                    <div style={{ position: 'relative' }}>
                                        <span style={{ position: 'absolute', left: 0, top: '50%', transform: 'translateY(-50%)', color: C.primary, opacity: 0.5, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px' }}>
                                            &gt;
                                        </span>
                                        <input
                                            name="username" type="text" placeholder="Architect_01"
                                            value={formData.username} onChange={handleChange} required
                                            style={{
                                                width: '100%', backgroundColor: 'transparent',
                                                border: 'none', borderBottom: `1px solid ${C.border}`,
                                                color: C.onBg, fontFamily: "'JetBrains Mono', monospace",
                                                fontSize: '14px', padding: '8px 0 8px 24px',
                                                outline: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box',
                                            }}
                                            onFocus={e => e.target.style.borderBottomColor = C.secondary}
                                            onBlur={e => e.target.style.borderBottomColor = C.border}
                                        />
                                    </div>
                                </div>

                                {/* Email */}
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: C.primary, textTransform: 'uppercase' }}>
                                        Email Address
                                    </label>
                                    <div style={{ position: 'relative' }}>
                                        <span style={{ position: 'absolute', left: 0, top: '50%', transform: 'translateY(-50%)', color: C.primary, opacity: 0.5, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px' }}>
                                            &gt;
                                        </span>
                                        <input
                                            name="email" type="email" placeholder="architect@codecombat.io"
                                            value={formData.email} onChange={handleChange} required
                                            style={{
                                                width: '100%', backgroundColor: 'transparent',
                                                border: 'none', borderBottom: `1px solid ${C.border}`,
                                                color: C.onBg, fontFamily: "'JetBrains Mono', monospace",
                                                fontSize: '14px', padding: '8px 0 8px 24px',
                                                outline: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box',
                                            }}
                                            onFocus={e => e.target.style.borderBottomColor = C.secondary}
                                            onBlur={e => e.target.style.borderBottomColor = C.border}
                                        />
                                    </div>
                                </div>

                                {/* CAPTCHA */}
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: C.primary, textTransform: 'uppercase' }}>
                                        Verification
                                    </label>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', borderBottom: `1px solid ${C.border}`, paddingBottom: '4px' }}>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', color: C.onBg, flex: 1 }}>
                                            {captcha.question || 'Loading...'}
                                        </span>
                                        <input
                                            name="captchaAnswer" type="text" placeholder="?"
                                            value={formData.captchaAnswer} onChange={handleChange} required
                                            style={{
                                                width: '80px', backgroundColor: 'transparent',
                                                border: 'none',
                                                color: C.onBg, fontFamily: "'JetBrains Mono', monospace",
                                                fontSize: '14px', padding: '8px 0', outline: 'none',
                                                textAlign: 'center', boxSizing: 'border-box',
                                            }}
                                        />
                                        <button type="button" onClick={refreshCaptcha}
                                            style={{
                                                background: 'none', border: `1px solid ${C.border}`,
                                                color: C.muted, fontSize: '11px',
                                                fontFamily: "'JetBrains Mono', monospace",
                                                padding: '4px 8px', cursor: 'pointer',
                                            }}>
                                            ↻
                                        </button>
                                    </div>
                                </div>

                                {/* Honeypot — hidden from real users */}
                                <input
                                    type="text" name="website" autoComplete="off" tabIndex={-1}
                                    aria-hidden="true"
                                    value={formData.website}
                                    onChange={handleChange}
                                    style={{
                                        position: 'absolute', left: '-10000px', top: 'auto',
                                        width: '1px', height: '1px', overflow: 'hidden',
                                        opacity: 0, pointerEvents: 'none',
                                    }}
                                />

                                {/* Actions */}
                                <div style={{ paddingTop: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
                                    <Link to="/login" style={{
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '12px', letterSpacing: '0.1em',
                                        color: C.muted, textDecoration: 'none', transition: 'color 0.2s',
                                    }}
                                        onMouseEnter={e => e.target.style.color = C.secondary}
                                        onMouseLeave={e => e.target.style.color = C.muted}
                                    >
                                        &lt; Abort Protocol
                                    </Link>
                                    <SlideButton type="submit" disabled={loading}>
                                        {loading ? 'Sending...' : 'Execute Request'}
                                    </SlideButton>
                                </div>
                            </form>
                        </div>
                    ) : (
                        /* Success state */
                        <motion.div
                            initial={{ opacity: 0 }} animate={{ opacity: 1 }}
                            style={{
                                border: `1px solid ${C.border}`,
                                padding: '3rem',
                                position: 'relative',
                                backgroundColor: C.surfaceLow,
                                display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', gap: '1.5rem',
                            }}
                        >
                            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.secondary }} />
                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '32px', fontWeight: 600, color: C.onBg, margin: 0 }}>
                                Protocol Sent
                            </h3>
                            <p style={{ fontSize: '16px', color: C.muted, lineHeight: 1.6, maxWidth: '400px', margin: 0 }}>
                                {message}
                            </p>
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.secondary, letterSpacing: '0.05em', margin: 0 }}>
                                Check spam/junk if not in inbox.
                            </p>
                            <Link to="/login" style={{
                                marginTop: '1rem',
                                border: `1px solid ${C.border}`,
                                backgroundColor: 'transparent',
                                padding: '12px 32px',
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', letterSpacing: '0.1em',
                                color: C.onBg, textDecoration: 'none',
                                textTransform: 'uppercase', transition: 'all 0.2s',
                            }}
                                onMouseEnter={e => { e.target.style.borderColor = C.secondary; e.target.style.color = C.secondary; }}
                                onMouseLeave={e => { e.target.style.borderColor = C.border; e.target.style.color = C.onBg; }}
                            >
                                Return to Login
                            </Link>
                        </motion.div>
                    )}
                </motion.div>
            </main>


            <style>{`@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}`}</style>
        </div>
    );
};

const SlideButton = ({ children, type = 'button', disabled = false }) => {
    const [hovered, setHovered] = React.useState(false);
    return (
        <button type={type} disabled={disabled}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                border: '1px solid #e9c176',
                backgroundColor: hovered && !disabled ? '#e9c176' : 'transparent',
                color: hovered && !disabled ? '#131313' : '#e9c176',
                padding: '12px 32px', cursor: disabled ? 'not-allowed' : 'pointer',
                opacity: disabled ? 0.4 : 1, transition: 'all 0.3s',
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px', letterSpacing: '0.1em',
                fontWeight: 500, textTransform: 'uppercase',
                display: 'inline-flex', alignItems: 'center', gap: '8px',
            }}
        >
            {children}
        </button>
    );
};

export default ForgotPassword;
