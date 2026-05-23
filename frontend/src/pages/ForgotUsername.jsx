import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';

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

const ForgotUsername = () => {
    const [formData, setFormData] = useState({ email: '' });
    const [loading, setLoading]   = useState(false);
    const [message, setMessage]   = useState('');
    const [success, setSuccess]   = useState(false);

    const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage(''); setLoading(true);
        try {
            const res = await api.post('/auth/forgot-username', formData);
            setSuccess(true);
            setMessage(res.data.message);
        } catch (err) {
            setSuccess(false);
            setMessage(err.response?.data?.message || 'An error occurred. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{
            flex: 1, display: 'flex',
            backgroundColor: C.bg, color: C.onBg,
            fontFamily: "'Geist', sans-serif",
        }}>
            {/* ── Left cinematic panel ── */}
            <div style={{
                flex: '0 0 50%',
                backgroundColor: C.surfaceCon,
                borderRight: `1px solid ${C.border}`,
                position: 'relative',
                overflow: 'hidden',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
                {/* Grid overlay */}
                <div style={{
                    position: 'absolute', inset: 0, zIndex: 1,
                    backgroundImage: `linear-gradient(to right,rgba(80,69,59,0.15) 1px,transparent 1px),linear-gradient(to bottom,rgba(80,69,59,0.15) 1px,transparent 1px)`,
                    backgroundSize: '64px 64px',
                }} />
                {/* Radial glow */}
                <div style={{
                    position: 'absolute', inset: 0, zIndex: 2,
                    background: 'radial-gradient(ellipse 60% 60% at 50% 50%, rgba(241,188,139,0.06) 0%, transparent 70%)',
                }} />
                {/* Wordmark image */}
                <img
                    src="/codecombat-wordmark.webp"
                    alt="Code Combat"
                    style={{
                        position: 'absolute', inset: 0,
                        width: '100%', height: '100%',
                        objectFit: 'cover',
                        zIndex: 0,
                    }}
                />
            </div>

            {/* ── Right form panel ── */}
            <div style={{
                flex: 1,
                display: 'flex', flexDirection: 'column', justifyContent: 'center',
                padding: '64px 48px',
                backgroundColor: C.bg,
                position: 'relative', zIndex: 10,
            }}>
                <motion.div
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ duration: 0.6, ease: 'easeOut' }}
                    style={{ width: '100%', maxWidth: '360px', margin: '0 auto' }}
                >
                    {/* Header */}
                    <div style={{ marginBottom: '3rem' }}>
                        <h1 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '32px', fontWeight: 600,
                            color: C.onBg, marginBottom: '1rem', lineHeight: 1.2,
                        }}>
                            Identity Recovery
                        </h1>
                        <p style={{ fontSize: '16px', color: C.muted, lineHeight: 1.5, margin: 0 }}>
                            Provide your registered credentials. The architectural protocol requires a confirmed match to trace your identity.
                        </p>
                    </div>

                    {/* Error */}
                    {message && !success && (
                        <motion.div
                            initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}
                            style={{
                                marginBottom: '1.5rem', padding: '10px 14px',
                                border: `1px solid ${C.error}`, borderLeft: `3px solid ${C.error}`,
                                backgroundColor: 'rgba(255,180,171,0.08)',
                            }}
                        >
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error, margin: 0 }}>{message}</p>
                        </motion.div>
                    )}

                    {/* Success */}
                    {success && (
                        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}
                            style={{ textAlign: 'center', padding: '1rem 0' }}>
                            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', color: C.secondary, marginBottom: '12px' }}>
                                Identity Traced
                            </h2>
                            <p style={{ fontSize: '15px', color: C.muted, lineHeight: 1.6, marginBottom: '1.5rem' }}>{message}</p>
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.secondary, letterSpacing: '0.05em', marginBottom: '2rem' }}>
                                Check spam/junk if not in inbox.
                            </p>
                            <Link to="/login" style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', letterSpacing: '0.1em',
                                color: C.muted, textDecoration: 'none', transition: 'color 0.2s',
                            }}
                                onMouseEnter={e => e.target.style.color = C.secondary}
                                onMouseLeave={e => e.target.style.color = C.muted}
                            >
                                ← Return to Authentication
                            </Link>
                        </motion.div>
                    )}

                    {/* Form */}
                    {!success && (
                        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            {/* Email */}
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                <label style={{
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '12px', letterSpacing: '0.1em',
                                    color: C.onBg, textTransform: 'uppercase', display: 'block',
                                }}>
                                    Email Address
                                </label>
                                <input
                                    name="email" type="email" placeholder="architect@domain.com"
                                    value={formData.email} onChange={handleChange} required
                                    style={{
                                        width: '100%', backgroundColor: 'transparent',
                                        border: `1px solid ${C.border}`,
                                        color: C.onBg, fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '14px', padding: '12px 16px',
                                        outline: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box',
                                    }}
                                    onFocus={e => e.target.style.borderColor = C.primary}
                                    onBlur={e => e.target.style.borderColor = C.border}
                                />
                            </div>

                            {/* Submit */}
                            <SlideButton type="submit" disabled={loading}>
                                {loading ? (
                                    <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <svg style={{ animation: 'spin 1s linear infinite', width: '14px', height: '14px' }} viewBox="0 0 24 24">
                                            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" opacity="0.25" />
                                            <path fill="currentColor" opacity="0.75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                        </svg>
                                        Sending...
                                    </span>
                                ) : 'Initiate Trace'}
                            </SlideButton>
                        </form>
                    )}

                    {/* Footer mechanics */}
                    {!success && (
                        <div style={{ marginTop: '3rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                            {/* Security note */}
                            <div style={{
                                padding: '1rem',
                                border: `1px solid ${C.border}`,
                                backgroundColor: C.surfaceLow,
                            }}>
                                <p style={{
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '13px', color: C.muted, lineHeight: 1.6, margin: 0,
                                }}>
                                    We will never share your identity. Data is encrypted within the arena's core architecture.
                                </p>
                            </div>

                            {/* Back link */}
                            <div style={{ display: 'flex', justifyContent: 'center' }}>
                                <Link to="/login" style={{
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '12px', letterSpacing: '0.1em',
                                    color: C.muted, textDecoration: 'none',
                                    display: 'flex', alignItems: 'center', gap: '8px',
                                    transition: 'color 0.2s',
                                }}
                                    onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                                    onMouseLeave={e => e.currentTarget.style.color = C.muted}
                                >
                                    ← Return to Authentication
                                </Link>
                            </div>
                        </div>
                    )}
                </motion.div>
            </div>

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
                width: '100%',
                border: '1px solid #f1bc8b',
                backgroundColor: hovered && !disabled ? '#f1bc8b' : 'transparent',
                color: hovered && !disabled ? '#131313' : '#f1bc8b',
                padding: '16px', cursor: disabled ? 'not-allowed' : 'pointer',
                opacity: disabled ? 0.4 : 1, transition: 'all 0.3s',
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px', letterSpacing: '0.1em',
                fontWeight: 500, textTransform: 'uppercase',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
            }}
        >
            {children}
        </button>
    );
};

export default ForgotUsername;
