import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import AuthService from '../services/auth.service';

// ── Stitch Design Tokens ──────────────────────────────────────────────────────
const C = {
    bg:        '#131313',
    border:    '#50453b',
    primary:   '#f1bc8b',
    secondary: '#e9c176',
    muted:     '#d4c4b7',
    surface:   '#201f1f',
    surfaceHi: '#2a2a2a',
};

const Navbar = () => {
    const [currentUser, setCurrentUser] = useState(undefined);
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const [isDesktop, setIsDesktop] = useState(window.innerWidth >= 900);
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) setCurrentUser(user);

        const handleResize = () => setIsDesktop(window.innerWidth >= 900);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    const logOut = () => {
        AuthService.logout();
        setCurrentUser(undefined);
        setMobileMenuOpen(false);
        navigate('/');
    };

    const isActive = (path) => location.pathname === path;
    const isAdmin  = currentUser?.roles?.includes('ROLE_ADMIN');

    const closeMobileMenu = () => setMobileMenuOpen(false);

    return (
        <>
            {/* ── Desktop / Main Nav ── */}
            <header
                style={{
                    backgroundColor: C.bg,
                    borderBottom: `1px solid ${C.border}`,
                    position: 'sticky',
                    top: 0,
                    zIndex: 50,
                    fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                }}
            >
                <div
                    style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        padding: isDesktop ? '0.875rem 64px' : '0.875rem 20px',
                        maxWidth: '100%',
                    }}
                >
                    {/* Logo */}
                    <Link
                        to="/"
                        style={{
                            fontFamily: "'Playfair Display', 'Georgia', serif",
                            fontSize: '22px',
                            fontWeight: 600,
                            color: C.primary,
                            fontStyle: 'italic',
                            textDecoration: 'none',
                            letterSpacing: '-0.01em',
                            flexShrink: 0,
                        }}
                    >
                        Code Coder
                    </Link>

                    {/* Desktop Nav Links */}
                    {isDesktop && (
                        <nav style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
                            {/* Dashboard — shown when logged in */}
                            {currentUser && isAdmin && (
                                <StitchNavLink to="/admin/dashboard" active={isActive('/admin/dashboard')}>
                                    Dashboard
                                </StitchNavLink>
                            )}
                            {currentUser && !isAdmin && (
                                <StitchNavLink to="/dashboard" active={isActive('/dashboard')}>
                                    Dashboard
                                </StitchNavLink>
                            )}

                            {/* Contests */}
                            <StitchNavLink
                                to={isAdmin ? '/admin/contests' : '/contests'}
                                active={isActive('/contests') || isActive('/admin/contests')}
                            >
                                Contests
                            </StitchNavLink>

                            {/* Compiler — public access */}
                            <StitchNavLink to="/compiler" active={isActive('/compiler')}>
                                Compiler
                            </StitchNavLink>

                            {/* Rankings / Leaderboard — admin only */}
                            {isAdmin && (
                                <StitchNavLink to="/admin/leaderboard" active={isActive('/admin/leaderboard')}>
                                    Rankings
                                </StitchNavLink>
                            )}

                            {/* Platform Details — user only */}
                            {currentUser && !isAdmin && (
                                <StitchNavLink to="/platform-details" active={isActive('/platform-details')}>
                                    Platform Details
                                </StitchNavLink>
                            )}

                            {/* Support — always visible */}
                            <StitchNavLink to="/support" active={isActive('/support')}>
                                Support
                            </StitchNavLink>
                        </nav>
                    )}

                    {/* Desktop Auth */}
                    {isDesktop && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                            {currentUser ? (
                                <>
                                    {/* Username badge */}
                                    <span
                                        style={{
                                            fontSize: '12px',
                                            letterSpacing: '0.08em',
                                            color: C.muted,
                                            padding: '6px 14px',
                                            border: `1px solid ${C.border}`,
                                            backgroundColor: C.surface,
                                        }}
                                    >
                                        {currentUser.username}
                                    </span>
                                    {/* Logout */}
                                    <StitchButton onClick={logOut} variant="ghost">
                                        Logout
                                    </StitchButton>
                                </>
                            ) : (
                                <>
                                    <StitchButton as={Link} to="/login" variant="ghost">
                                        Login
                                    </StitchButton>
                                    <StitchButton as={Link} to="/register" variant="outline">
                                        Sign Up
                                    </StitchButton>
                                </>
                            )}
                        </div>
                    )}

                    {/* Mobile Hamburger */}
                    {!isDesktop && (
                        <button
                            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                            style={{
                                background: 'none',
                                border: `1px solid ${C.border}`,
                                padding: '6px 10px',
                                cursor: 'pointer',
                                color: C.muted,
                                display: 'flex',
                                alignItems: 'center',
                            }}
                            aria-label="Toggle menu"
                        >
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                {mobileMenuOpen
                                    ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                    : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                                }
                            </svg>
                        </button>
                    )}
                </div>
            </header>

            {/* ── Mobile Overlay ── */}
            {mobileMenuOpen && (
                <div
                    onClick={closeMobileMenu}
                    style={{
                        position: 'fixed', inset: 0,
                        backgroundColor: 'rgba(0,0,0,0.7)',
                        backdropFilter: 'blur(4px)',
                        zIndex: 40,
                    }}
                />
            )}

            {/* ── Mobile Slide-out ── */}
            <div
                style={{
                    position: 'fixed',
                    top: 0, right: 0,
                    height: '100%',
                    width: '280px',
                    backgroundColor: C.bg,
                    borderLeft: `1px solid ${C.border}`,
                    zIndex: 50,
                    transform: mobileMenuOpen ? 'translateX(0)' : 'translateX(100%)',
                    transition: 'transform 0.3s ease',
                    display: 'flex',
                    flexDirection: 'column',
                    padding: '1.5rem',
                    fontFamily: "'JetBrains Mono', monospace",
                }}
            >
                {/* Close */}
                <button
                    onClick={closeMobileMenu}
                    style={{
                        alignSelf: 'flex-end',
                        background: 'none',
                        border: `1px solid ${C.border}`,
                        padding: '6px 10px',
                        cursor: 'pointer',
                        color: C.muted,
                        marginBottom: '2rem',
                    }}
                >
                    <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </button>

                {/* Mobile Links */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', flex: 1 }}>
                    {currentUser && isAdmin && (
                        <MobileLink to="/admin/dashboard" active={isActive('/admin/dashboard')} onClick={closeMobileMenu}>Dashboard</MobileLink>
                    )}
                    {currentUser && !isAdmin && (
                        <MobileLink to="/dashboard" active={isActive('/dashboard')} onClick={closeMobileMenu}>Dashboard</MobileLink>
                    )}
                    <MobileLink
                        to={isAdmin ? '/admin/contests' : '/contests'}
                        active={isActive('/contests') || isActive('/admin/contests')}
                        onClick={closeMobileMenu}
                    >
                        Contests
                    </MobileLink>
                    <MobileLink to="/compiler" active={isActive('/compiler')} onClick={closeMobileMenu}>
                        Compiler
                    </MobileLink>
                    {isAdmin && (
                        <MobileLink to="/admin/leaderboard" active={isActive('/admin/leaderboard')} onClick={closeMobileMenu}>Rankings</MobileLink>
                    )}
                    {currentUser && !isAdmin && (
                        <MobileLink to="/platform-details" active={isActive('/platform-details')} onClick={closeMobileMenu}>Platform Details</MobileLink>
                    )}
                    <MobileLink to="/support" active={isActive('/support')} onClick={closeMobileMenu}>Support</MobileLink>
                </div>

                {/* Mobile Auth */}
                <div style={{ borderTop: `1px solid ${C.border}`, paddingTop: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    {currentUser ? (
                        <>
                            <div style={{ fontSize: '12px', letterSpacing: '0.08em', color: C.muted, padding: '10px 14px', border: `1px solid ${C.border}`, textAlign: 'center' }}>
                                {currentUser.username}
                            </div>
                            <button
                                onClick={logOut}
                                style={{
                                    fontSize: '12px', letterSpacing: '0.1em', fontWeight: 500,
                                    color: C.muted, border: `1px solid ${C.border}`,
                                    padding: '10px 16px', background: 'none', cursor: 'pointer',
                                    transition: 'all 0.15s', width: '100%',
                                }}
                            >
                                Logout
                            </button>
                        </>
                    ) : (
                        <>
                            <Link to="/login" onClick={closeMobileMenu} style={{ fontSize: '12px', letterSpacing: '0.1em', color: C.muted, border: `1px solid ${C.border}`, padding: '10px 16px', textDecoration: 'none', textAlign: 'center', display: 'block' }}>
                                Login
                            </Link>
                            <Link to="/register" onClick={closeMobileMenu} style={{ fontSize: '12px', letterSpacing: '0.1em', color: '#131313', backgroundColor: C.secondary, border: `1px solid ${C.secondary}`, padding: '10px 16px', textDecoration: 'none', textAlign: 'center', display: 'block', fontWeight: 600 }}>
                                Sign Up
                            </Link>
                        </>
                    )}
                </div>
            </div>
        </>
    );
};

// ── Sub-components ────────────────────────────────────────────────────────────

const StitchNavLink = ({ to, children, active }) => {
    const [hovered, setHovered] = useState(false);
    return (
        <Link
            to={to}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px',
                letterSpacing: '0.1em',
                fontWeight: 500,
                color: active ? '#f1bc8b' : hovered ? '#e9c176' : '#d4c4b7',
                textDecoration: 'none',
                borderBottom: active ? '2px solid #f1bc8b' : '2px solid transparent',
                paddingBottom: '2px',
                transition: 'color 0.2s, border-color 0.2s',
            }}
        >
            {children}
        </Link>
    );
};

const StitchButton = ({ as: Tag = 'button', children, onClick, to, variant = 'ghost' }) => {
    const [hovered, setHovered] = useState(false);

    const base = {
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '12px',
        letterSpacing: '0.1em',
        fontWeight: 500,
        padding: '8px 16px',
        cursor: 'pointer',
        textDecoration: 'none',
        display: 'inline-block',
        transition: 'all 0.15s',
        background: 'none',
        border: 'none',
    };

    const styles = {
        ghost: {
            ...base,
            color: hovered ? '#e9c176' : '#d4c4b7',
            textDecoration: hovered ? 'underline' : 'none',
            textDecorationColor: '#e9c176',
        },
        outline: {
            ...base,
            color: hovered ? '#131313' : '#f1bc8b',
            border: '1px solid #e9c176',
            backgroundColor: hovered ? '#e9c176' : 'transparent',
        },
    };

    const props = {
        style: styles[variant],
        onMouseEnter: () => setHovered(true),
        onMouseLeave: () => setHovered(false),
        ...(Tag === 'button' ? { onClick } : { to }),
    };

    return <Tag {...props}>{children}</Tag>;
};

const MobileLink = ({ to, children, active, onClick }) => {
    return (
        <Link
            to={to}
            onClick={onClick}
            style={{
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px',
                letterSpacing: '0.1em',
                fontWeight: 500,
                color: active ? '#f1bc8b' : '#d4c4b7',
                textDecoration: 'none',
                padding: '10px 14px',
                borderLeft: active ? '2px solid #f1bc8b' : '2px solid transparent',
                backgroundColor: active ? 'rgba(241,188,139,0.06)' : 'transparent',
                display: 'block',
                transition: 'all 0.2s',
            }}
        >
            {children}
        </Link>
    );
};

export default Navbar;
