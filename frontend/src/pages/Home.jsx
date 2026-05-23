import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import AuthService from '../services/auth.service';

const C = {
    bg:         '#131313',
    surfaceLow: '#1c1b1b',
    surfaceCon: '#201f1f',
    surfaceMin: '#0e0e0e',
    border:     'rgba(241,188,139,0.3)',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
};

const Home = () => {
    const [currentUser, setCurrentUser] = useState(null);
    const [isAdmin, setIsAdmin]         = useState(false);

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) {
            setCurrentUser(user);
            setIsAdmin(user.roles?.includes('ROLE_ADMIN'));
        }
    }, []);

    const links = {
        primary:  !currentUser ? '/login'   : isAdmin ? '/admin/contests'    : '/contests',
        explore:  !currentUser ? '/login'   : isAdmin ? '/admin/contests'    : '/contests',
        contests: !currentUser ? '/login'   : isAdmin ? '/admin/contests'    : '/contests',
        cta:      !currentUser ? '/register': isAdmin ? '/admin/leaderboard' : '/contests',
    };

    return (
        <div style={{
            backgroundColor: C.bg,
            color: C.onBg,
            fontFamily: "'Geist', sans-serif",
            minHeight: '100vh',
            display: 'flex',
            flexDirection: 'column',
        }}>

            {/* ── Hero ── */}
            <section style={{
                position: 'relative',
                minHeight: '90vh',
                display: 'flex',
                alignItems: 'center',
                marginBottom: '8rem',
                borderBottom: `0.5px solid ${C.border}`,
                overflow: 'hidden',
            }}>
                <div style={{ position: 'absolute', inset: 0, zIndex: 0 }}>
                    <img
                        src="/bg-hero-new.webp"
                        alt=""
                        loading="eager"
                        fetchpriority="high"
                        decoding="async"
                        style={{
                            width: '100%', height: '100%',
                            objectFit: 'cover',
                            opacity: 0.85,
                        }}
                    />
                    <div style={{
                        position: 'absolute', inset: 0,
                        background: 'linear-gradient(to right, rgba(19,19,19,0.95) 30%, rgba(19,19,19,0.6) 60%, rgba(19,19,19,0.2) 100%)',
                    }} />
                </div>

                <div style={{ position: 'relative', zIndex: 10, width: '100%', padding: '0 64px' }}>
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.8, ease: 'easeOut' }}
                        style={{ maxWidth: '66%', display: 'flex', flexDirection: 'column' }}
                    >
                        <h1 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '72px', fontWeight: 700,
                            lineHeight: 1.1, letterSpacing: '-0.02em',
                            color: C.onBg, marginBottom: '2rem',
                        }}>
                            The Arena of <br />
                            <span style={{ color: C.primary, fontStyle: 'italic', fontWeight: 300 }}>
                                Pure Logic.
                            </span>
                        </h1>

                        <p style={{
                            fontFamily: "'Geist', sans-serif",
                            fontSize: '18px', lineHeight: 1.6,
                            color: C.muted, maxWidth: '640px',
                            marginBottom: '3rem',
                            borderLeft: `1px solid rgba(241,188,139,0.3)`,
                            paddingLeft: '2rem',
                            fontWeight: 300, letterSpacing: '0.02em',
                        }}>
                            Enter the cinematic command center for elite software engineers. Where algorithms are forged in deep focus, and code is executed with architectural precision. No gamification. Just profound intellectual combat.
                        </p>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '2rem', flexWrap: 'wrap' }}>
                            <HeroButton to={links.cta} variant="primary">
                                Start Battle →
                            </HeroButton>
                            <HeroButton to={links.explore} variant="ghost">
                                Explore Challenges
                            </HeroButton>
                        </div>
                    </motion.div>
                </div>
            </section>

            {/* ── Stats ── */}
            <motion.section
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6 }}
                style={{
                    borderTop: `0.5px solid ${C.border}`,
                    borderBottom: `0.5px solid ${C.border}`,
                    backgroundColor: C.bg,
                    width: '100%',
                    marginBottom: '12rem',
                    position: 'relative',
                }}
            >
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(4, 1fr)',
                    margin: '0 64px',
                    borderLeft: `0.5px solid ${C.border}`,
                    borderRight: `0.5px solid ${C.border}`,
                }}>
                    {[
                        { value: '50k+',  label: 'Battles Fought' },
                        { value: '1M+',   label: 'Submissions Processed' },
                        { value: '0.4ms', label: 'Avg Execution Latency' },
                        { value: '300',   label: 'Elite Architects' },
                    ].map(({ value, label }, i) => (
                        <StatCard key={label} value={value} label={label} isLast={i === 3} />
                    ))}
                </div>
            </motion.section>

            {/* ── Features ── */}
            <section style={{
                padding: '0 64px',
                display: 'flex',
                flexDirection: 'column',
                gap: '8rem',
                marginBottom: '12rem',
                maxWidth: '1200px',
                width: '100%',
                alignSelf: 'center',
            }}>

                {/* Feature 1: Real-time Verdicts — full-width image, text left */}
                <motion.div
                    initial={{ opacity: 0, y: 30 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6 }}
                    className="feature-card"
                    style={{
                        position: 'relative', height: '600px',
                        border: `0.5px solid ${C.border}`,
                        overflow: 'hidden',
                    }}
                >
                    <img
                        src="/bg-feature-1.webp"
                        loading="lazy"
                        decoding="async"
                        alt=""
                        style={{
                            position: 'absolute', inset: 0,
                            width: '100%', height: '100%',
                            objectFit: 'cover',
                            transition: 'transform 1s ease',
                        }}
                    />
                    <div style={{
                        position: 'absolute', inset: 0,
                        backgroundColor: 'rgba(19,19,19,0.55)',
                        backdropFilter: 'blur(2px)',
                    }} />
                    <div style={{
                        position: 'absolute', top: 0, bottom: 0, left: 0, width: '50%',
                        padding: '6rem',
                        display: 'flex', flexDirection: 'column', justifyContent: 'center',
                        background: 'linear-gradient(to right, rgba(19,19,19,0.92), transparent)',
                    }}>
                        <h3 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '40px', fontWeight: 300,
                            color: C.onBg, marginBottom: '1.5rem',
                        }}>
                            Real-time Verdicts
                        </h3>
                        <p style={{ fontSize: '18px', color: C.muted, lineHeight: 1.6, fontWeight: 300 }}>
                            Experience execution speeds that rival thought. Our distributed judge infrastructure returns execution results, memory profiles, and structural analysis in milliseconds.
                        </p>
                    </div>
                </motion.div>

                {/* Feature 2: Monaco Intelligence — split grid */}
                <motion.div
                    initial={{ opacity: 0, y: 30 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6, delay: 0.1 }}
                    className="feature-card"
                    style={{
                        position: 'relative',
                        border: `0.5px solid ${C.border}`,
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr',
                        overflow: 'hidden',
                    }}
                >
                    {/* Background image */}
                    <img
                        src="/bg-feature-2.webp"
                        loading="lazy"
                        decoding="async"
                        alt=""
                        style={{
                            position: 'absolute', inset: 0,
                            width: '100%', height: '100%',
                            objectFit: 'cover',
                            transition: 'transform 1s ease',
                            zIndex: 0,
                        }}
                    />
                    <div style={{
                        position: 'absolute', inset: 0,
                        backgroundColor: 'rgba(19,19,19,0.75)',
                        zIndex: 1,
                    }} />

                    {/* Left: text */}
                    <div style={{
                        position: 'relative', zIndex: 2,
                        padding: '6rem',
                        display: 'flex', flexDirection: 'column', justifyContent: 'center',
                        borderRight: `0.5px solid ${C.border}`,
                    }}>
                        <h3 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '40px', fontWeight: 300,
                            color: C.onBg, marginBottom: '1.5rem',
                        }}>
                            Monaco Intelligence
                        </h3>
                        <p style={{ fontSize: '18px', color: C.muted, lineHeight: 1.6, fontWeight: 300 }}>
                            A pristine, distraction-free editing canvas powered by the same engine behind VS Code. Deep syntax highlighting, architectural linting, and absolute structural control.
                        </p>
                    </div>

                    {/* Right: code editor panel */}
                    <div style={{
                        position: 'relative', zIndex: 2,
                        backgroundColor: 'rgba(10,10,10,0.85)',
                        display: 'flex', flexDirection: 'column', justifyContent: 'center',
                        padding: '2rem 4rem',
                    }}>
                        {/* Window chrome */}
                        <div style={{
                            position: 'absolute', top: '2rem', left: '2rem', right: '2rem', height: '32px',
                            borderBottom: `0.5px solid ${C.border}`,
                            display: 'flex', alignItems: 'center', padding: '0 16px', gap: '8px', opacity: 0.5,
                        }}>
                            {[0, 1, 2].map(i => (
                                <div key={i} style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: C.outline }} />
                            ))}
                            <span style={{
                                marginLeft: '16px',
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '10px', color: C.muted, letterSpacing: '0.1em',
                            }}>arena.rs</span>
                        </div>
                        {/* Code */}
                        <div style={{
                            marginTop: '3rem',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '14px', lineHeight: 1.5,
                            color: 'rgba(212,196,183,0.8)',
                        }}>
                            <div style={{ paddingLeft: '16px', borderLeft: `1px solid rgba(241,188,139,0.2)` }}>
                                <span style={{ color: 'rgba(241,188,139,0.7)' }}>fn</span>
                                {' evaluate_complexity(n: '}
                                <span style={{ color: 'rgba(233,193,118,0.7)' }}>usize</span>
                                {') -> '}
                                <span style={{ color: 'rgba(233,193,118,0.7)' }}>Result</span>
                                {'<(), Error> {'}<br />
                                &nbsp;&nbsp;<span style={{ color: 'rgba(157,142,131,0.5)', fontStyle: 'italic' }}>// Absolute precision required.</span><br />
                                &nbsp;&nbsp;{'match system.execute() { ... }'}<br />
                                {'}'}{' '}
                                <span style={{ color: C.primary, animation: 'blink 1s infinite' }}>█</span>
                            </div>
                        </div>
                    </div>
                </motion.div>

                {/* Feature 3: Elite Rankings — full-width image, text right */}
                <motion.div
                    initial={{ opacity: 0, y: 30 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6, delay: 0.1 }}
                    className="feature-card"
                    style={{
                        position: 'relative', height: '600px',
                        border: `0.5px solid ${C.border}`,
                        overflow: 'hidden',
                    }}
                >
                    <img
                        src="/bg-feature-3.webp"
                        loading="lazy"
                        decoding="async"
                        alt=""
                        style={{
                            position: 'absolute', inset: 0,
                            width: '100%', height: '100%',
                            objectFit: 'cover',
                            transition: 'transform 1s ease',
                        }}
                    />
                    <div style={{
                        position: 'absolute', inset: 0,
                        backgroundColor: 'rgba(19,19,19,0.55)',
                        backdropFilter: 'blur(2px)',
                    }} />
                    <div style={{
                        position: 'absolute', top: 0, bottom: 0, right: 0, width: '50%',
                        padding: '6rem',
                        display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'flex-end', textAlign: 'right',
                        background: 'linear-gradient(to left, rgba(19,19,19,0.92), transparent)',
                    }}>
                        <h3 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '40px', fontWeight: 300,
                            color: C.onBg, marginBottom: '1.5rem',
                        }}>
                            Elite Rankings
                        </h3>
                        <p style={{ fontSize: '18px', color: C.muted, lineHeight: 1.6, fontWeight: 300, maxWidth: '400px' }}>
                            Climb the hierarchy of architectural mastery. Our Elo-based system strictly evaluates performance, elegance, and logic under pressure.
                        </p>
                    </div>
                </motion.div>
            </section>

            {/* ── Contests Strip ── */}
            <motion.section
                initial={{ opacity: 0 }}
                whileInView={{ opacity: 1 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6 }}
                style={{
                    borderTop: `0.5px solid ${C.border}`,
                    borderBottom: `0.5px solid ${C.border}`,
                    backgroundColor: C.bg,
                    display: 'flex',
                    position: 'relative', zIndex: 10,
                }}
            >
                {[
                    {
                        division: 'Alpha Division', divColor: C.primary,
                        title: 'Graph Theory Invitational',
                        desc: 'Navigating complex topologies with constrained memory parameters.',
                        time: 'T-Minus 14:00:00',
                    },
                    {
                        division: 'Beta Division', divColor: C.secondary,
                        title: 'Dynamic Programming Blitz',
                        desc: 'Optimize overlapping subproblems in a high-speed sprint.',
                        time: 'T-Minus 48:30:00',
                    },
                ].map(({ division, divColor, title, desc, time }) => (
                    <ContestCard
                        key={title}
                        division={division} divColor={divColor}
                        title={title} desc={desc} time={time}
                        to={links.contests}
                    />
                ))}

                {/* View All */}
                <Link
                    to={links.contests}
                    style={{
                        flex: '0 0 33%', padding: '4rem',
                        display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center',
                        borderLeft: `0.5px solid ${C.border}`,
                        backgroundColor: 'rgba(19,19,19,0.5)',
                        textDecoration: 'none', transition: 'background-color 0.5s',
                    }}
                    onMouseEnter={e => e.currentTarget.style.backgroundColor = C.surfaceLow}
                    onMouseLeave={e => e.currentTarget.style.backgroundColor = 'rgba(19,19,19,0.5)'}
                >
                    <span style={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '10px', letterSpacing: '0.2em',
                        color: C.muted, textTransform: 'uppercase',
                        display: 'block', textAlign: 'center',
                        marginBottom: '1.5rem',
                    }}>
                        View All Schedules
                    </span>
                    <span style={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '24px', color: 'rgba(157,142,131,0.5)',
                        letterSpacing: '0.05em',
                    }}>[ → ]</span>
                </Link>
            </motion.section>

            <style>{`
                @keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
                .feature-card:hover img { transform: scale(1.05); }
            `}</style>
        </div>
    );
};

/* ── Hero Button ─────────────────────────────────────────────────────────── */
const HeroButton = ({ children, to, variant }) => {
    const [hovered, setHovered] = useState(false);
    const isPrimary = variant === 'primary';
    return (
        <Link
            to={to}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px', letterSpacing: '0.1em',
                fontWeight: 500, textTransform: 'uppercase',
                textDecoration: 'none',
                padding: '20px 40px',
                display: 'inline-flex', alignItems: 'center', gap: '16px',
                transition: 'all 0.3s',
                ...(isPrimary ? {
                    color: hovered ? '#131313' : '#f1bc8b',
                    border: '1px solid rgba(241,188,139,0.5)',
                    backgroundColor: hovered ? '#f1bc8b' : 'rgba(19,19,19,0.5)',
                    backdropFilter: 'blur(4px)',
                } : {
                    color: hovered ? '#e9c176' : '#d4c4b7',
                    border: 'none', backgroundColor: 'transparent',
                }),
            }}
        >
            {children}
        </Link>
    );
};

/* ── Stat Card ───────────────────────────────────────────────────────────── */
const StatCard = ({ value, label, isLast }) => {
    const [hovered, setHovered] = useState(false);
    return (
        <div
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                padding: '3rem',
                display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', textAlign: 'center',
                borderRight: isLast ? 'none' : '0.5px solid rgba(241,188,139,0.3)',
                backgroundColor: hovered ? 'rgba(28,27,27,0.3)' : 'transparent',
                transition: 'background-color 0.5s',
            }}
        >
            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '48px', fontWeight: 300, color: '#f1bc8b' }}>
                {value}
            </span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: '#d4c4b7', marginTop: '1rem', textTransform: 'uppercase', opacity: 0.7 }}>
                {label}
            </span>
        </div>
    );
};

/* ── Contest Card ────────────────────────────────────────────────────────── */
const ContestCard = ({ division, divColor, title, desc, time, to }) => {
    const [hovered, setHovered] = useState(false);
    return (
        <Link
            to={to}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                flex: 1, padding: '4rem',
                display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
                borderRight: '0.5px solid rgba(241,188,139,0.3)',
                textDecoration: 'none',
                backgroundColor: hovered ? '#1c1b1b' : 'transparent',
                transition: 'background-color 0.5s',
            }}
        >
            <div>
                <span style={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '10px', letterSpacing: '0.2em',
                    color: divColor, display: 'block', marginBottom: '1rem', textTransform: 'uppercase',
                }}>
                    {division}
                </span>
                <h4 style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 300, color: '#e5e2e1', marginBottom: '1rem' }}>
                    {title}
                </h4>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: 'rgba(212,196,183,0.8)', fontWeight: 300, lineHeight: 1.6 }}>
                    {desc}
                </p>
            </div>
            <div style={{
                marginTop: '3rem', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end',
                borderTop: '0.5px solid rgba(241,188,139,0.3)', paddingTop: '1.5rem',
                opacity: hovered ? 1 : 0.6, transition: 'opacity 0.3s',
            }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: '#d4c4b7', letterSpacing: '0.05em' }}>
                    {time}
                </span>
                <span style={{
                    color: divColor, fontSize: '18px',
                    transform: hovered ? 'translateX(8px)' : 'none',
                    transition: 'transform 0.3s',
                    fontFamily: "'JetBrains Mono', monospace",
                }}>→</span>
            </div>
        </Link>
    );
};

export default Home;
