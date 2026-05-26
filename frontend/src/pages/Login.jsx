import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import AuthService from '../services/auth.service';
import BlockedAccountModal from '../components/BlockedAccountModal';
import useResponsive from '../hooks/useResponsive';

// ── Stitch Design Tokens ──────────────────────────────────────────────────────
const C = {
    bg:        '#131313',
    surface:   '#201f1f',
    surfaceHi: '#2a2a2a',
    border:    '#50453b',
    primary:   '#f1bc8b',
    secondary: '#e9c176',
    muted:     '#d4c4b7',
    outline:   '#9d8e83',
    error:     '#ffb4ab',
    onBg:      '#e5e2e1',
};

const Login = () => {
    const { isMobile, isTablet } = useResponsive();
    const [username, setUsername]         = useState('');
    const [password, setPassword]         = useState('');
    const [website, setWebsite]           = useState(''); // honeypot
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading]           = useState(false);
    const [message, setMessage]           = useState('');
    const [showBlockedModal, setShowBlockedModal] = useState(false);
    const navigate = useNavigate();

    const handleLogin = (e) => {
        e.preventDefault();
        setMessage('');
        setLoading(true);

        AuthService.login(username, password, { website }).then(
            (data) => {
                if (data.roles && data.roles.includes('ROLE_ADMIN')) {
                    navigate('/admin/dashboard');
                } else {
                    navigate('/dashboard');
                }
                window.location.reload();
            },
            (error) => {
                const resMessage =
                    (error.response?.data?.message) ||
                    error.message ||
                    error.toString();

                if (resMessage.includes('ACCOUNT_DISABLED')) {
                    setShowBlockedModal(true);
                    setLoading(false);
                    return;
                }
                setLoading(false);
                setMessage(resMessage);
            }
        );
    };

    return (
        <div
            style={{
                backgroundColor: C.bg,
                flex: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontFamily: "'Geist', sans-serif",
                position: 'relative',
                paddingTop: '3rem',
                paddingBottom: '3rem',
            }}
        >
            {/* ── Background image ── */}
            <div style={{
                position: 'fixed', inset: 0, pointerEvents: 'none', zIndex: 0,
                backgroundImage: 'url(/bg-login.webp)',
                backgroundSize: 'cover',
                backgroundPosition: 'center',
                backgroundRepeat: 'no-repeat',
            }} />
            {/* Dark overlay so card stays readable */}
            <div style={{
                position: 'fixed', inset: 0, pointerEvents: 'none', zIndex: 1,
                background: 'rgba(19,19,19,0.72)',
            }} />
            {/* Subtle grid on top */}
            <div style={{
                position: 'fixed', inset: 0, pointerEvents: 'none', zIndex: 2,
                backgroundImage: `
                    linear-gradient(to right, ${C.border} 1px, transparent 1px),
                    linear-gradient(to bottom, ${C.border} 1px, transparent 1px)
                `,
                backgroundSize: '64px 64px',
                opacity: 0.08,
            }} />

            {/* ── Layout grid ── */}
            <div style={{
                position: 'relative', zIndex: 10,
                width: '100%', maxWidth: '1200px',
                padding: isMobile ? '0 16px' : '0 64px',
                display: 'grid',
                gridTemplateColumns: isMobile ? '1fr' : 'repeat(12, 1fr)',
                gap: '32px',
                alignItems: 'start',
            }}>

                {/* ── Security Status Panel (left) ── */}
                {!isMobile && (
                <motion.div
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ duration: 0.6, delay: 0.3 }}
                    style={{
                        gridColumn: '1 / span 3',
                        marginTop: '6rem',
                        borderLeft: `1px solid ${C.border}`,
                        paddingLeft: '1rem',
                        paddingTop: '0.5rem',
                        paddingBottom: '0.5rem',
                        opacity: 0.7,
                    }}
                >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                        <div style={{
                            width: '8px', height: '8px',
                            backgroundColor: C.secondary,
                            borderRadius: '50%',
                            opacity: 0.8,
                        }} />
                        <span style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '12px', letterSpacing: '0.1em',
                            color: C.muted, textTransform: 'uppercase',
                        }}>
                            System Secure
                        </span>
                    </div>
                    <p style={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '14px', color: C.outline, lineHeight: 1.5,
                    }}>
                        Terminal ID: AX-992<br />Encrypted Connection
                    </p>
                </motion.div>
                )}

                {/* ── Main Login Card ── */}
                <motion.div
                    initial={{ opacity: 0, y: 24 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6, ease: 'easeOut' }}
                    style={{
                        gridColumn: isMobile ? '1 / -1' : '4 / span 6',
                        backgroundColor: 'rgba(20,19,19,0.75)',
                        backdropFilter: 'blur(20px)',
                        border: `1px solid ${C.border}`,
                        padding: isMobile ? '2rem 1.5rem' : '3rem',
                        position: 'relative',
                        overflow: 'hidden',
                    }}
                    className="login-card-group"
                >
                    {/* Bronze top accent — animates on hover via CSS */}
                    <TopAccentLine />

                    {/* Header */}
                    <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
                        <span style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '12px', letterSpacing: '0.1em',
                            color: C.secondary, textTransform: 'uppercase',
                            display: 'block', marginBottom: '1rem',
                        }}>
                            Initialization Sequence
                        </span>
                        <h1 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: isMobile ? '32px' : '48px', fontWeight: 700,
                            lineHeight: 1.1, color: C.onBg,
                        }}>
                            Return to the Arena.
                        </h1>
                    </div>

                    {/* Error Message */}
                    {message && (
                        <motion.div
                            initial={{ opacity: 0, y: -8 }}
                            animate={{ opacity: 1, y: 0 }}
                            style={{
                                marginBottom: '1.5rem',
                                padding: '12px 16px',
                                border: `1px solid ${C.error}`,
                                borderLeft: `3px solid ${C.error}`,
                                backgroundColor: 'rgba(255,180,171,0.08)',
                                display: 'flex', alignItems: 'center', gap: '10px',
                            }}
                        >
                            <span style={{ color: C.error, fontSize: '18px' }}>⚠</span>
                            <p style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '13px', color: C.error,
                            }}>
                                {message}
                            </p>
                        </motion.div>
                    )}

                    {/* Form */}
                    <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>

                        {/* Honeypot — hidden from real users */}
                        <input
                            type="text" name="website" autoComplete="off" tabIndex={-1}
                            aria-hidden="true"
                            value={website}
                            onChange={(e) => setWebsite(e.target.value)}
                            style={{
                                position: 'absolute', left: '-10000px', top: 'auto',
                                width: '1px', height: '1px', overflow: 'hidden',
                                opacity: 0, pointerEvents: 'none',
                            }}
                        />

                        {/* Username */}
                        <div>
                            <label style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', letterSpacing: '0.1em',
                                color: C.muted, textTransform: 'uppercase',
                                display: 'block', marginBottom: '8px',
                            }}>
                                Operative Identity
                            </label>
                            <input
                                type="text"
                                id="username"
                                placeholder="Handle or Comm-Link"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                required
                                style={{
                                    width: '100%',
                                    backgroundColor: 'transparent',
                                    border: 'none',
                                    borderBottom: `1px solid ${C.border}`,
                                    color: C.onBg,
                                    fontFamily: "'Geist', sans-serif",
                                    fontSize: '18px',
                                    lineHeight: 1.6,
                                    padding: '12px 0',
                                    outline: 'none',
                                    transition: 'border-color 0.2s',
                                    boxSizing: 'border-box',
                                }}
                                onFocus={e => e.target.style.borderBottomColor = C.secondary}
                                onBlur={e => e.target.style.borderBottomColor = C.border}
                            />
                            <div style={{ marginTop: '6px' }}>
                                <Link
                                    to="/forgot-username"
                                    style={{
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '12px', letterSpacing: '0.08em',
                                        color: C.outline, textDecoration: 'none',
                                        transition: 'color 0.2s',
                                    }}
                                    onMouseEnter={e => { e.target.style.color = C.secondary; e.target.style.textDecoration = 'underline'; }}
                                    onMouseLeave={e => { e.target.style.color = C.outline; e.target.style.textDecoration = 'none'; }}
                                >
                                    Recover Identity
                                </Link>
                            </div>
                        </div>

                        {/* Password */}
                        <div>
                            <label style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', letterSpacing: '0.1em',
                                color: C.muted, textTransform: 'uppercase',
                                display: 'block', marginBottom: '8px',
                            }}>
                                Access Key
                            </label>
                            <div style={{ position: 'relative' }}>
                                <input
                                    type={showPassword ? 'text' : 'password'}
                                    id="password"
                                    placeholder="••••••••••••"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                    style={{
                                        width: '100%',
                                        backgroundColor: 'transparent',
                                        border: 'none',
                                        borderBottom: `1px solid ${C.border}`,
                                        color: C.onBg,
                                        fontFamily: "'Geist', sans-serif",
                                        fontSize: '18px',
                                        lineHeight: 1.6,
                                        padding: '12px 40px 12px 0',
                                        outline: 'none',
                                        transition: 'border-color 0.2s',
                                        boxSizing: 'border-box',
                                    }}
                                    onFocus={e => e.target.style.borderBottomColor = C.secondary}
                                    onBlur={e => e.target.style.borderBottomColor = C.border}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    style={{
                                        position: 'absolute', right: 0, top: '50%',
                                        transform: 'translateY(-50%)',
                                        background: 'none', border: 'none',
                                        cursor: 'pointer', color: C.outline,
                                        padding: '4px',
                                    }}
                                >
                                    {showPassword ? (
                                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                                        </svg>
                                    ) : (
                                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                        </svg>
                                    )}
                                </button>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '6px' }}>
                                <Link
                                    to="/forgot-password"
                                    style={{
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '12px', letterSpacing: '0.08em',
                                        color: C.outline, textDecoration: 'none',
                                        transition: 'color 0.2s',
                                    }}
                                    onMouseEnter={e => { e.target.style.color = C.secondary; e.target.style.textDecoration = 'underline'; }}
                                    onMouseLeave={e => { e.target.style.color = C.outline; e.target.style.textDecoration = 'none'; }}
                                >
                                    Forgot Key?
                                </Link>
                            </div>
                        </div>

                        {/* Submit Button — slide-fill effect */}
                        <div style={{ paddingTop: '2rem' }}>
                            <StitchButton type="submit" disabled={loading}>
                                {loading ? (
                                    <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <svg style={{ animation: 'spin 1s linear infinite', width: '16px', height: '16px' }} viewBox="0 0 24 24">
                                            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" opacity="0.25" />
                                            <path fill="currentColor" opacity="0.75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                        </svg>
                                        Initializing...
                                    </span>
                                ) : (
                                    <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        Initialize Link →
                                    </span>
                                )}
                            </StitchButton>
                        </div>

                        {/* Register link */}
                        <div style={{ textAlign: 'center', paddingTop: '1.5rem' }}>
                            <span style={{
                                fontFamily: "'Geist', sans-serif",
                                fontSize: '16px', color: C.muted,
                            }}>
                                Unregistered asset?{' '}
                            </span>
                            <Link
                                to="/register"
                                style={{
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '12px', letterSpacing: '0.1em',
                                    color: C.secondary,
                                    textDecoration: 'underline',
                                    textDecorationColor: C.secondary,
                                    transition: 'color 0.2s',
                                }}
                                onMouseEnter={e => e.target.style.color = C.primary}
                                onMouseLeave={e => e.target.style.color = C.secondary}
                            >
                                Enlist Here
                            </Link>
                        </div>
                    </form>
                </motion.div>
            </div>

            {/* CSS for spin animation */}
            <style>{`
                @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
                .stitch-btn-fill { transform: translateX(-100%); transition: transform 0.3s ease-in-out; }
                .stitch-btn:hover .stitch-btn-fill { transform: translateX(0); }
                .stitch-btn:hover .stitch-btn-text { color: #131313; }
                .stitch-btn-text { transition: color 0.3s; }
                .login-top-accent { width: 0; transition: width 0.7s ease-out; }
                .login-card-group:hover .login-top-accent { width: 100%; }
            `}</style>

            {showBlockedModal && <BlockedAccountModal />}
        </div>
    );
};

// ── Top accent line component ─────────────────────────────────────────────────
const TopAccentLine = () => (
    <div
        className="login-top-accent"
        style={{
            position: 'absolute', top: 0, left: 0,
            height: '2px',
            backgroundColor: '#e9c176',
        }}
    />
);

// ── Stitch slide-fill button ──────────────────────────────────────────────────
const StitchButton = ({ children, type = 'button', disabled = false }) => (
    <button
        type={type}
        disabled={disabled}
        className="stitch-btn"
        style={{
            width: '100%',
            position: 'relative',
            overflow: 'hidden',
            backgroundColor: 'transparent',
            border: '1px solid #e9c176',
            color: '#e9c176',
            padding: '16px 32px',
            cursor: disabled ? 'not-allowed' : 'pointer',
            opacity: disabled ? 0.5 : 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
        }}
    >
        <div
            className="stitch-btn-fill"
            style={{
                position: 'absolute', inset: 0,
                backgroundColor: '#e9c176',
                zIndex: 0,
            }}
        />
        <span
            className="stitch-btn-text"
            style={{
                position: 'relative', zIndex: 1,
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px', letterSpacing: '0.1em',
                fontWeight: 500, textTransform: 'uppercase',
            }}
        >
            {children}
        </span>
    </button>
);

export default Login;
