import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import AuthService from '../services/auth.service';

const C = {
    bg:        '#131313',
    border:    'rgba(80,69,59,0.5)',
    primary:   '#f1bc8b',
    secondary: '#e9c176',
    muted:     '#d4c4b7',
    outline:   '#9d8e83',
    surface:   '#201f1f',
};

const Navbar = () => {
    const [currentUser, setCurrentUser]     = useState(undefined);
    const [scrolled, setScrolled]           = useState(false);
    const [mobileOpen, setMobileOpen]       = useState(false);
    const navigate  = useNavigate();
    const location  = useLocation();

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) setCurrentUser(user);
    }, []);

    useEffect(() => {
        const onScroll = () => setScrolled(window.scrollY > 20);
        window.addEventListener('scroll', onScroll, { passive: true });
        return () => window.removeEventListener('scroll', onScroll);
    }, []);

    // Close mobile menu on route change
    useEffect(() => { setMobileOpen(false); }, [location.pathname]);

    const logOut = () => {
        AuthService.logout();
        setCurrentUser(undefined);
        navigate('/');
    };

    const isActive = (path) => location.pathname === path;
    const isAdmin  = currentUser?.roles?.includes('ROLE_ADMIN');

    const navLinks = [
        ...(currentUser && isAdmin  ? [{ label: 'Dashboard', to: '/admin/dashboard' }] : []),
        ...(currentUser && !isAdmin ? [{ label: 'Dashboard', to: '/dashboard'       }] : []),
        { label: 'Contests', to: isAdmin ? '/admin/contests' : '/contests' },
        { label: 'Compiler', to: '/compiler' },
        ...(isAdmin               ? [{ label: 'Rankings', to: '/admin/leaderboard' }] : []),
        ...(currentUser && !isAdmin ? [{ label: 'Practice', to: '/practice' }] : []),
        { label: 'Support', to: '/support' },
    ];

    return (
        <>
            {/* ── Main Header ── */}
            <header style={{
                position: 'fixed', zIndex: 50,
                transition: 'all 0.5s ease',
                top:   scrolled ? '16px' : '0',
                left:  scrolled ? '16px' : '0',
                right: scrolled ? '16px' : '0',
            }}>
                <nav style={{
                    margin: '0 auto',
                    transition: 'all 0.5s ease',
                    ...(scrolled ? {
                        maxWidth: '1200px',
                        backgroundColor: 'rgba(19,19,19,0.85)',
                        backdropFilter: 'blur(20px)',
                        WebkitBackdropFilter: 'blur(20px)',
                        border: `1px solid ${C.border}`,
                        borderRadius: '999px',
                        boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
                    } : {
                        maxWidth: '100%',
                        backgroundColor: 'transparent',
                    }),
                }}>
                    <div style={{
                        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                        padding: scrolled ? '0 24px' : '0 64px',
                        height: scrolled ? '52px' : '76px',
                        transition: 'all 0.5s ease',
                    }}>
                        {/* Logo */}
                        <Link to="/" style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: scrolled ? '18px' : '22px',
                            fontWeight: 600, fontStyle: 'italic',
                            color: C.primary, textDecoration: 'none',
                            letterSpacing: '-0.01em', flexShrink: 0,
                            transition: 'font-size 0.5s ease',
                        }}>
                            Code Coder
                        </Link>

                        {/* Desktop Nav Links */}
                        <nav style={{ display: 'flex', alignItems: 'center', gap: '2.5rem' }}
                            className="hide-mobile">
                            {navLinks.map(l => (
                                <NavLinkItem key={l.to} to={l.to} active={isActive(l.to)}>{l.label}</NavLinkItem>
                            ))}
                        </nav>

                        {/* Desktop Auth */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}
                            className="hide-mobile">
                            {currentUser ? (
                                <>
                                    <span style={{
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: scrolled ? '10px' : '12px',
                                        letterSpacing: '0.08em', color: C.muted,
                                        padding: scrolled ? '4px 12px' : '6px 14px',
                                        border: `1px solid ${C.border}`,
                                        borderRadius: scrolled ? '999px' : '0',
                                        transition: 'all 0.5s ease',
                                    }}>{currentUser.username}</span>
                                    <AuthBtn onClick={logOut}>Logout</AuthBtn>
                                </>
                            ) : (
                                <>
                                    <AuthBtn to="/login" ghost>Sign in</AuthBtn>
                                    <AuthBtn to="/register" filled scrolled={scrolled}>Start competing</AuthBtn>
                                </>
                            )}
                        </div>

                        {/* Mobile Hamburger */}
                        <button onClick={() => setMobileOpen(v => !v)}
                            className="show-mobile"
                            style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.muted, padding: '6px', display: 'none' }}
                            aria-label="Toggle menu">
                            {mobileOpen
                                ? <svg width="22" height="22" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M6 18L18 6M6 6l12 12" /></svg>
                                : <svg width="22" height="22" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 6h16M4 12h16M4 18h16" /></svg>
                            }
                        </button>
                    </div>
                </nav>
            </header>

            {/* ── Mobile Full-Screen Overlay ── */}
            <div style={{
                position: 'fixed', inset: 0, zIndex: 40,
                backgroundColor: C.bg,
                opacity: mobileOpen ? 1 : 0,
                pointerEvents: mobileOpen ? 'auto' : 'none',
                transition: 'opacity 0.5s ease',
                display: 'flex', flexDirection: 'column',
                padding: '0 32px',
                fontFamily: "'Playfair Display', serif",
            }}>
                {/* Mobile close button */}
                <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: '24px' }}>
                    <button onClick={() => setMobileOpen(false)}
                        style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.muted, padding: '8px' }}>
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                {/* Mobile Links — large type like Optimus */}
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: '2rem' }}>
                    {navLinks.map((l, i) => (
                        <Link key={l.to} to={l.to} onClick={() => setMobileOpen(false)}
                            style={{
                                fontSize: '48px', fontWeight: 300, color: isActive(l.to) ? C.primary : C.muted,
                                textDecoration: 'none', lineHeight: 1,
                                opacity: mobileOpen ? 1 : 0,
                                transform: mobileOpen ? 'none' : 'translateY(16px)',
                                transition: `all 0.5s ease ${i * 75}ms`,
                            }}
                            onMouseEnter={e => e.currentTarget.style.color = C.primary}
                            onMouseLeave={e => e.currentTarget.style.color = isActive(l.to) ? C.primary : C.muted}
                        >
                            {l.label}
                        </Link>
                    ))}
                </div>

                {/* Mobile Auth */}
                <div style={{
                    borderTop: `1px solid ${C.border}`, paddingTop: '2rem', paddingBottom: '2.5rem',
                    display: 'flex', gap: '1rem',
                    opacity: mobileOpen ? 1 : 0, transform: mobileOpen ? 'none' : 'translateY(16px)',
                    transition: 'all 0.5s ease 300ms',
                }}>
                    {currentUser ? (
                        <>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted, flex: 1, display: 'flex', alignItems: 'center' }}>{currentUser.username}</span>
                            <button onClick={() => { logOut(); setMobileOpen(false); }}
                                style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.bg, backgroundColor: C.primary, border: 'none', padding: '14px 28px', cursor: 'pointer', flex: 1, borderRadius: '999px' }}>
                                Logout
                            </button>
                        </>
                    ) : (
                        <>
                            <Link to="/login" onClick={() => setMobileOpen(false)}
                                style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.muted, border: `1px solid ${C.border}`, padding: '14px 0', textDecoration: 'none', textAlign: 'center', flex: 1, borderRadius: '999px' }}>
                                Sign in
                            </Link>
                            <Link to="/register" onClick={() => setMobileOpen(false)}
                                style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.bg, backgroundColor: C.primary, padding: '14px 0', textDecoration: 'none', textAlign: 'center', flex: 1, borderRadius: '999px', fontWeight: 600 }}>
                                Start competing
                            </Link>
                        </>
                    )}
                </div>
            </div>

            <style>{`
                @media (max-width: 768px) {
                    .hide-mobile { display: none !important; }
                    .show-mobile { display: flex !important; }
                }
                @media (min-width: 769px) {
                    .show-mobile { display: none !important; }
                    .hide-mobile { display: flex !important; }
                }
            `}</style>
        </>
    );
};

// ── Nav Link with underline slide animation ───────────────────────────────────
const NavLinkItem = ({ to, children, active }) => {
    const [hov, setHov] = useState(false);
    return (
        <Link to={to}
            onMouseEnter={() => setHov(true)}
            onMouseLeave={() => setHov(false)}
            style={{
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px', letterSpacing: '0.1em',
                color: active ? C.primary : hov ? C.muted : 'rgba(212,196,183,0.7)',
                textDecoration: 'none', position: 'relative',
                transition: 'color 0.3s',
                paddingBottom: '2px',
            }}>
            {children}
            <span style={{
                position: 'absolute', bottom: '-2px', left: 0,
                height: '1px', background: active ? C.primary : C.secondary,
                width: active ? '100%' : hov ? '100%' : '0%',
                transition: 'width 0.3s ease',
            }} />
        </Link>
    );
};

// ── Auth Button ───────────────────────────────────────────────────────────────
const AuthBtn = ({ children, to, onClick, ghost, filled, scrolled }) => {
    const [hov, setHov] = useState(false);
    const style = filled ? {
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: scrolled ? '11px' : '12px', letterSpacing: '0.1em',
        color: C.bg, backgroundColor: hov ? C.secondary : C.primary,
        border: 'none', padding: scrolled ? '7px 18px' : '9px 22px',
        borderRadius: '999px', cursor: 'pointer', textDecoration: 'none',
        display: 'inline-block', transition: 'all 0.3s',
        fontWeight: 600,
    } : {
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '12px', letterSpacing: '0.1em',
        color: hov ? C.primary : 'rgba(212,196,183,0.7)',
        background: 'none', border: 'none', cursor: 'pointer',
        textDecoration: 'none', display: 'inline-block',
        transition: 'color 0.3s', padding: '4px 0',
    };

    return to
        ? <Link to={to} style={style} onMouseEnter={() => setHov(true)} onMouseLeave={() => setHov(false)}>{children}</Link>
        : <button style={style} onClick={onClick} onMouseEnter={() => setHov(true)} onMouseLeave={() => setHov(false)}>{children}</button>;
};

export default Navbar;
