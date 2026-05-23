import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthService from '../services/auth.service';

// ── Design tokens (matches project palette) ───────────────────────────────────
const C = {
    bg:         '#131313',
    surface:    '#201f1f',
    surfaceLow: '#1c1b1b',
    border:     '#50453b',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
};

export default function NotFound() {
    const navigate  = useNavigate();
    const cursorRef = useRef(null);
    const [visible, setVisible] = useState(false);

    // Determine where "Go Home" and "Go Dashboard" should point
    const currentUser = AuthService.getCurrentUser();
    const isAdmin     = currentUser?.roles?.includes('ROLE_ADMIN');
    const homeHref    = '/';
    const dashHref    = !currentUser
        ? '/login'
        : isAdmin
        ? '/admin/dashboard'
        : '/dashboard';
    const dashLabel   = !currentUser ? 'Login' : 'Go Dashboard';

    // Blinking cursor
    useEffect(() => {
        const id = setInterval(() => setVisible(v => !v), 530);
        return () => clearInterval(id);
    }, []);

    return (
        <div
            style={{
                backgroundColor: C.bg,
                color: C.onBg,
                minHeight: '100vh',
                display: 'flex',
                flexDirection: 'column',
                fontFamily: "'Geist', sans-serif",
                overflow: 'hidden',
                position: 'relative',
            }}
        >
            {/* ── Background: abstract wireframe sphere image ── */}
            <div
                aria-hidden="true"
                style={{
                    position: 'absolute',
                    inset: 0,
                    zIndex: 0,
                    opacity: 0.18,
                    mixBlendMode: 'screen',
                    backgroundImage: `url('https://lh3.googleusercontent.com/aida-public/AB6AXuD67vQIxIaSs28f3g6QIU4WCNohLzmQ6ZG8Fs6LTQXJlb-wmYUSSW719F_qYwqkbBQSUfMHvJlHj2lzhSePIBWG7Eqmk-WuZm2ncWGMhA5rOHyr1DRdOPOBZpckKEds6gB2xdL4bNq1EtQzzOkj30whb5u8Y_7wjlyjIXH8fyjQVZWGYVTYK3gJU1S04uKZLBs0ljtdW4X34jshhFhyi4hqpI28-b48lcenBo46NtSx2Sg8JSKnL5uSHJB_yMj_wkpKcyquL0UtwO0U')`,
                    backgroundSize: 'cover',
                    backgroundPosition: 'center',
                    backgroundRepeat: 'no-repeat',
                    pointerEvents: 'none',
                }}
            />

            {/* ── Radial vignette overlay ── */}
            <div
                aria-hidden="true"
                style={{
                    position: 'absolute',
                    inset: 0,
                    zIndex: 1,
                    background: 'radial-gradient(circle at center, transparent 0%, #131313 78%)',
                    pointerEvents: 'none',
                }}
            />

            {/* ── Main content ── */}
            <main
                style={{
                    flex: 1,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    position: 'relative',
                    zIndex: 10,
                    padding: '2rem 64px',
                    minHeight: '100vh',
                }}
            >
                <div
                    style={{
                        width: '100%',
                        maxWidth: '900px',
                        display: 'grid',
                        gridTemplateColumns: '5fr 7fr',
                        gap: '32px',
                        alignItems: 'center',
                    }}
                    className="not-found-grid"
                >
                    {/* ── Left: Fragmented code block ── */}
                    <div
                        style={{
                            borderLeft: `1px solid ${C.border}`,
                            paddingLeft: '2rem',
                            opacity: 0.8,
                        }}
                    >
                        <div
                            style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '14px',
                                lineHeight: 1.7,
                                display: 'flex',
                                flexDirection: 'column',
                                gap: '2px',
                            }}
                        >
                            <span style={{ color: C.primary }}>try {'{'}</span>
                            <span style={{ paddingLeft: '1.5rem', color: C.muted }}>
                                route.execute(target);
                            </span>
                            <span style={{ paddingLeft: '1.5rem', color: C.error, opacity: 0.75 }}>
                                {'// Target missing in environment'}
                            </span>
                            <span style={{ color: C.primary }}>{'} catch (Exception e) {'}</span>
                            <span style={{ paddingLeft: '1.5rem', color: C.muted }}>
                                System.out.println(
                            </span>
                            <span
                                style={{
                                    paddingLeft: '3rem',
                                    color: C.outline,
                                }}
                            >
                                "404: Arena Not Found."
                            </span>
                            <span style={{ paddingLeft: '1.5rem', color: C.muted }}>);</span>
                            <span style={{ color: C.primary }}>{'}'}</span>

                            {/* Blinking cursor */}
                            <span
                                style={{
                                    marginTop: '1rem',
                                    color: C.primary,
                                    fontSize: '16px',
                                    opacity: visible ? 1 : 0,
                                    transition: 'opacity 0.1s',
                                    display: 'block',
                                }}
                            >
                                █
                            </span>
                        </div>
                    </div>

                    {/* ── Right: Error messaging ── */}
                    <div
                        style={{
                            borderLeft: `1px solid ${C.border}`,
                            paddingLeft: '2rem',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '2rem',
                        }}
                    >
                        {/* 404 headline */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <h1
                                style={{
                                    fontFamily: "'Playfair Display', serif",
                                    fontSize: 'clamp(64px, 10vw, 96px)',
                                    fontWeight: 700,
                                    lineHeight: 1,
                                    letterSpacing: '-0.02em',
                                    margin: 0,
                                    background: `linear-gradient(135deg, ${C.onBg} 0%, ${C.outline} 100%)`,
                                    WebkitBackgroundClip: 'text',
                                    WebkitTextFillColor: 'transparent',
                                    backgroundClip: 'text',
                                }}
                            >
                                404
                            </h1>

                            <div
                                style={{
                                    borderBottom: `1px solid rgba(233,193,118,0.3)`,
                                    paddingBottom: '1rem',
                                    display: 'inline-block',
                                }}
                            >
                                <h2
                                    style={{
                                        fontFamily: "'Playfair Display', serif",
                                        fontSize: '24px',
                                        fontWeight: 600,
                                        color: C.secondary,
                                        margin: 0,
                                    }}
                                >
                                    Route Decrypted: Not Found
                                </h2>
                            </div>

                            <p
                                style={{
                                    fontFamily: "'Geist', sans-serif",
                                    fontSize: '16px',
                                    lineHeight: 1.6,
                                    color: C.outline,
                                    margin: 0,
                                    maxWidth: '420px',
                                    paddingTop: '0.5rem',
                                }}
                            >
                                The designated coordinates are unmapped. The arena you are
                                attempting to enter does not exist in the current architecture.
                            </p>
                        </div>

                        {/* ── Action buttons ── */}
                        <div
                            style={{
                                display: 'flex',
                                flexDirection: 'row',
                                gap: '16px',
                                flexWrap: 'wrap',
                                paddingTop: '0.5rem',
                            }}
                        >
                            {/* Primary: Go Home */}
                            <HomeButton to={homeHref} />

                            {/* Secondary: Dashboard / Login */}
                            <DashButton to={dashHref} label={dashLabel} />
                        </div>

                        {/* ── Error code footer ── */}
                        <div
                            style={{
                                borderTop: `1px solid ${C.border}`,
                                paddingTop: '1rem',
                                marginTop: '0.5rem',
                            }}
                        >
                            <p
                                style={{
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '11px',
                                    letterSpacing: '0.1em',
                                    color: C.outline,
                                    margin: 0,
                                    opacity: 0.6,
                                }}
                            >
                                ERROR_CODE:{' '}
                                <span style={{ color: C.primary }}>404_ROUTE_NOT_FOUND</span>
                                {' '}·{' '}
                                <span style={{ color: C.outline }}>
                                    {window.location.pathname}
                                </span>
                            </p>
                        </div>
                    </div>
                </div>
            </main>

            {/* ── Responsive styles ── */}
            <style>{`
                @media (max-width: 768px) {
                    .not-found-grid {
                        grid-template-columns: 1fr !important;
                        padding: 2rem 20px !important;
                    }
                    .not-found-grid > div:first-child {
                        order: 2;
                        border-left: none !important;
                        border-top: 1px solid #50453b;
                        padding-left: 0 !important;
                        padding-top: 1.5rem;
                    }
                    .not-found-grid > div:last-child {
                        order: 1;
                        border-left: none !important;
                        padding-left: 0 !important;
                    }
                }
            `}</style>
        </div>
    );
}

// ── Go Home button — bordered amber ──────────────────────────────────────────
const HomeButton = ({ to }) => {
    const [hovered, setHovered] = useState(false);
    return (
        <Link
            to={to}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                border: `1px solid ${C.secondary}`,
                padding: '14px 28px',
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px',
                letterSpacing: '0.1em',
                fontWeight: 500,
                textTransform: 'uppercase',
                textDecoration: 'none',
                color: hovered ? C.bg : C.secondary,
                backgroundColor: hovered ? C.secondary : 'transparent',
                transition: 'all 0.3s ease',
            }}
        >
            <span
                className="material-symbols-outlined"
                style={{ fontSize: '18px', fontVariationSettings: "'FILL' 0" }}
            >
                home
            </span>
            Go Home
        </Link>
    );
};

// ── Dashboard / Login ghost button ───────────────────────────────────────────
const DashButton = ({ to, label }) => {
    const [hovered, setHovered] = useState(false);
    return (
        <Link
            to={to}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                padding: '14px 28px',
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px',
                letterSpacing: '0.1em',
                fontWeight: 500,
                textTransform: 'uppercase',
                textDecoration: 'none',
                color: hovered ? C.secondary : C.outline,
                backgroundColor: 'transparent',
                border: 'none',
                transition: 'color 0.3s ease',
                position: 'relative',
            }}
        >
            <span
                className="material-symbols-outlined"
                style={{
                    fontSize: '18px',
                    fontVariationSettings: "'FILL' 0",
                    opacity: hovered ? 1 : 0.7,
                    transition: 'opacity 0.3s',
                }}
            >
                dashboard
            </span>
            {label}
        </Link>
    );
};
