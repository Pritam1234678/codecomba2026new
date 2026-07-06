import { useEffect, useRef } from 'react';

const C = {
    bg: '#131313', surfaceLow: '#1c1b1b', surfaceHi: '#2a2a2a', border: '#50453b',
    primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1',
};

const DOT_CANVAS = ({ opacity = 0.05, spacing = 20 }) => {
    const ref = useRef(null);
    useEffect(() => {
        const c = ref.current; if (!c) return;
        c.width = c.offsetWidth; c.height = c.offsetHeight;
        const ctx = c.getContext('2d');
        ctx.fillStyle = `rgba(229,226,225,${opacity})`;
        for (let x = 0; x < c.width; x += spacing)
            for (let y = 0; y < c.height; y += spacing)
                ctx.fillRect(x, y, 1, 1);
    }, [opacity, spacing]);
    return <canvas ref={ref} style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }} />;
};

// ── Shared helpers ──────────────────────────────────────────────────────────

const Points = ({ n, accent, locked }) => (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: 38, fontWeight: 700, color: locked ? C.outline : accent, lineHeight: 1 }}>{n}</span>
        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 10, letterSpacing: '0.15em', color: locked ? C.outline : accent, textTransform: 'uppercase' }}>pts</span>
    </div>
);

const LockOverlay = () => (
    <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: `${C.bg}cc`, zIndex: 5, backdropFilter: 'blur(4px)' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
            <span className="material-symbols-outlined" style={{ fontSize: 28, color: C.outline }}>lock</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>Locked</span>
        </div>
    </div>
);

const AccentLine = ({ color, width = 40 }) => (
    <svg width={width} height="2"><line x1="0" y1="1" x2={width} y2="1" stroke={color} strokeWidth="1.5" opacity="0.6" /></svg>
);

// ── 1. HELLO WORLD — small tilted card, dashed outline ──────────────────────
function HelloWorld({ locked, accent = C.primary }) {
    return (
        <div style={{
            position: 'relative', backgroundColor: C.surfaceLow, overflow: 'hidden',
            padding: '28px 24px 24px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
            ...(locked ? {} : { border: `1px solid ${accent}33`, transform: 'rotate(-1.5deg)' }),
            transition: 'transform 0.3s',
        }}>
            {locked && <LockOverlay />}
            <DOT_CANVAS opacity={0.04} spacing={16} />
            <div style={{ position: 'relative', zIndex: 1 }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.2em', color: accent, textTransform: 'uppercase', opacity: 0.7 }}>Tier I</span>
                <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: 26, fontWeight: 700, color: locked ? C.outline : C.onBg, margin: '8px 0 4px', lineHeight: 1.1 }}>
                    Hello<br/>World
                </h3>
                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 10, color: C.outline, margin: 0, lineHeight: 1.5, maxWidth: 180 }}>
                    Every legend starts with a single line.
                </p>
            </div>
            <div style={{ position: 'relative', zIndex: 1, marginTop: 16 }}>
                <Points n={50} accent={accent} locked={locked} />
                <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 8, letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginTop: 4 }}>
                    Beginner
                </div>
            </div>
        </div>
    );
}

// ── 2. BUG HUNTER — diagonal stripes, green accent ──────────────────────────
function BugHunter({ locked, accent = '#4ade80' }) {
    return (
        <div style={{
            position: 'relative', backgroundColor: C.bg, overflow: 'hidden',
            padding: '32px 28px 28px', display: 'flex', flexDirection: 'column', gap: 20,
            ...(locked ? {} : {
                background: `repeating-linear-gradient(-45deg, transparent, transparent 40px, ${accent}06 40px, ${accent}06 41px)`,
                border: `1px solid ${accent}22`,
                transform: 'translateY(-4px)',
            }),
            transition: 'transform 0.3s',
        }}>
            {locked && <LockOverlay />}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', position: 'relative', zIndex: 1 }}>
                <div>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.2em', color: accent, textTransform: 'uppercase' }}>Tier II</span>
                    <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: 28, fontWeight: 700, color: locked ? C.outline : C.onBg, margin: '6px 0 0', lineHeight: 1.15 }}>
                        Bug<br/>Hunter
                    </h3>
                </div>
                <div style={{ width: 40, height: 40, borderRadius: '50%', border: `1.5px solid ${locked ? C.border : accent}44`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: 18, color: locked ? C.outline : accent }}>bug_report</span>
                </div>
            </div>
            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: C.outline, margin: 0, lineHeight: 1.6, position: 'relative', zIndex: 1 }}>
                Conquered challenges and eliminated bugs. Relentless precision.
            </p>
            <div style={{ position: 'relative', zIndex: 1 }}>
                <Points n={100} accent={accent} locked={locked} />
                <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 8, letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginTop: 2 }}>
                    Intermediate
                </div>
            </div>
        </div>
    );
}

// ── 3. PROBLEM SOLVER — overlapping geometric shapes ─────────────────────────
function ProblemSolver({ locked, accent = C.secondary }) {
    return (
        <div style={{
            position: 'relative', backgroundColor: C.surfaceLow, overflow: 'hidden',
            padding: '36px 28px 28px',
            ...(locked ? {} : { transform: 'rotate(0.8deg)', border: `1px solid ${accent}33` }),
            transition: 'transform 0.3s',
        }}>
            {locked && <LockOverlay />}
            {/* decorative triangles */}
            <svg width="80" height="80" style={{ position: 'absolute', top: -10, right: -10, opacity: 0.12, pointerEvents: 'none' }}>
                <polygon points="0,0 80,0 80,80" fill={accent} />
            </svg>
            <svg width="60" height="60" style={{ position: 'absolute', bottom: -5, left: -5, opacity: 0.08, pointerEvents: 'none' }}>
                <polygon points="0,60 60,60 30,0" fill={accent} />
            </svg>
            <div style={{ position: 'relative', zIndex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                    <AccentLine color={accent} width={24} />
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.18em', color: accent, textTransform: 'uppercase' }}>Tier III</span>
                </div>
                <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: 30, fontWeight: 700, color: locked ? C.outline : C.onBg, margin: 0, lineHeight: 1.1 }}>
                    Problem<br/>Solver
                </h3>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: C.outline, margin: '12px 0 0', lineHeight: 1.6, maxWidth: 220 }}>
                    Persistence and logic transform obstacles into stepping stones.
                </p>
            </div>
            <div style={{ position: 'relative', zIndex: 1, marginTop: 20 }}>
                <Points n={500} accent={accent} locked={locked} />
                <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 8, letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginTop: 2 }}>
                    Advanced
                </div>
            </div>
        </div>
    );
}

// ── 4. ALGORITHM ACE — refined gold, centered, elegant ──────────────────────
function AlgorithmAce({ locked, accent = '#facc15' }) {
    return (
        <div style={{
            position: 'relative', backgroundColor: C.surfaceLow, overflow: 'hidden',
            padding: '40px 32px 32px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center',
            ...(locked ? {} : { border: `1px solid ${accent}22`, transform: 'translateY(4px)' }),
            transition: 'transform 0.3s',
        }}>
            {locked && <LockOverlay />}
            <div style={{ position: 'absolute', top: '30%', left: '50%', transform: 'translate(-50%,-50%)', width: '70%', height: '40%', background: `radial-gradient(ellipse, ${accent}0a, transparent)`, pointerEvents: 'none' }} />
            <div style={{ position: 'relative', zIndex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, justifyContent: 'center', marginBottom: 16 }}>
                    <svg width="24" height="2"><rect width="24" height="1" fill={accent} opacity="0.5" /></svg>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.18em', color: accent, textTransform: 'uppercase' }}>Tier IV</span>
                    <svg width="24" height="2"><rect width="24" height="1" fill={accent} opacity="0.5" /></svg>
                </div>
                <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: 32, fontWeight: 700, color: locked ? C.outline : accent, margin: 0, letterSpacing: '-0.01em' }}>
                    Algorithm<br/>Ace
                </h3>
                <div style={{ width: 48, height: 1, backgroundColor: accent, opacity: 0.3, margin: '16px auto' }} />
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: C.outline, margin: 0, lineHeight: 1.6, maxWidth: 240, fontStyle: 'italic' }}>
                    "Exceptional analytical thinking and algorithmic mastery."
                </p>
            </div>
            <div style={{ position: 'relative', zIndex: 1, marginTop: 20 }}>
                <Points n={1000} accent={accent} locked={locked} />
                <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 8, letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginTop: 2 }}>
                    Master
                </div>
            </div>
        </div>
    );
}

// ── 5. CODE ARCHITECT — blueprint grid, blue accent ─────────────────────────
function CodeArchitect({ locked, accent = '#60a5fa' }) {
    return (
        <div style={{
            position: 'relative', backgroundColor: C.bg, overflow: 'hidden',
            padding: '36px 28px 28px',
            ...(locked ? {} : {
                background: `
                    linear-gradient(${accent}08 1px, transparent 1px),
                    linear-gradient(90deg, ${accent}08 1px, transparent 1px)
                `,
                backgroundSize: '32px 32px',
                border: `1px solid ${accent}22`,
                transform: 'rotate(-0.5deg)',
            }),
            transition: 'transform 0.3s',
        }}>
            {locked && <LockOverlay />}
            <div style={{ position: 'relative', zIndex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
                    <div style={{ width: 32, height: 32, border: `1.5px solid ${locked ? C.border : accent}55`, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 2 }}>
                        <span className="material-symbols-outlined" style={{ fontSize: 14, color: locked ? C.outline : accent }}>architecture</span>
                    </div>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.18em', color: accent, textTransform: 'uppercase' }}>Tier V</span>
                </div>
                <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: 28, fontWeight: 700, color: locked ? C.outline : C.onBg, margin: 0, lineHeight: 1.15 }}>
                    Code<br/>Architect
                </h3>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: C.outline, margin: '14px 0 0', lineHeight: 1.6 }}>
                    Building solutions with vision, structure, and technical excellence.
                </p>
            </div>
            <div style={{ position: 'relative', zIndex: 1, marginTop: 20 }}>
                <Points n={2000} accent={accent} locked={locked} />
                <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 8, letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginTop: 2 }}>
                    Grandmaster
                </div>
            </div>
        </div>
    );
}

// ── 6. CODING LEGEND — crimson, angular, dramatic ────────────────────────────
function CodingLegend({ locked, accent = '#ff6b6b' }) {
    return (
        <div style={{
            position: 'relative', backgroundColor: C.surfaceLow, overflow: 'hidden',
            padding: '44px 32px 32px',
            ...(locked ? {} : { border: `1px solid ${accent}22`, boxShadow: `0 0 40px ${accent}0a` }),
            transition: 'box-shadow 0.3s',
        }}>
            {locked && <LockOverlay />}
            {/* Angular corner accents */}
            <svg width="60" height="60" style={{ position: 'absolute', top: 0, right: 0, opacity: locked ? 0.15 : 0.3, pointerEvents: 'none' }}>
                <polyline points="60,0 60,40 20,0" fill="none" stroke={accent} strokeWidth="1" />
            </svg>
            <svg width="60" height="60" style={{ position: 'absolute', bottom: 0, left: 0, opacity: locked ? 0.15 : 0.3, pointerEvents: 'none' }}>
                <polyline points="0,60 0,20 40,60" fill="none" stroke={accent} strokeWidth="1" />
            </svg>
            <div style={{ position: 'absolute', top: '30%', right: '-20%', width: '60%', height: '40%', background: `radial-gradient(ellipse, ${accent}06, transparent 70%)`, pointerEvents: 'none' }} />

            <div style={{ position: 'relative', zIndex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
                    <svg width="30" height="30" viewBox="0 0 30 30">
                        <polygon points="15,2 28,25 2,25" fill="none" stroke={locked ? C.outline : accent} strokeWidth="1" opacity="0.5" />
                        <polygon points="15,8 22,22 8,22" fill="none" stroke={locked ? C.outline : accent} strokeWidth="0.5" opacity="0.3" />
                    </svg>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.2em', color: accent, textTransform: 'uppercase' }}>Tier VI</span>
                </div>
                <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: 34, fontWeight: 700, color: locked ? C.outline : accent, margin: 0, lineHeight: 1.1, textShadow: locked ? 'none' : `0 0 30px ${accent}18` }}>
                    Coding<br/>Legend
                </h3>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: C.outline, margin: '16px 0 0', lineHeight: 1.6, maxWidth: 240 }}>
                    A milestone reserved for the elite. Legendary dedication.
                </p>
            </div>
            <div style={{ position: 'relative', zIndex: 1, marginTop: 24 }}>
                <Points n={5000} accent={accent} locked={locked} />
                <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 8, letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginTop: 2 }}>
                    Legend
                </div>
            </div>
        </div>
    );
}

// ── Export ───────────────────────────────────────────────────────────────────

const CARDS = { 'Hello World': HelloWorld, 'Bug Hunter': BugHunter, 'Problem Solver': ProblemSolver,
    'Algorithm Ace': AlgorithmAce, 'Code Architect': CodeArchitect, 'Coding Legend': CodingLegend };

export default function AchievementPoster({ tier, unlocked }) {
    const Card = CARDS[tier.name];
    return Card ? <Card locked={!unlocked} accent={tier.accent} /> : null;
}

// Re-export TIERS with accents for Socials
export const TIERS = [
    { name: 'Hello World',    min: 50,   accent: '#f1bc8b' },
    { name: 'Bug Hunter',     min: 100,  accent: '#4ade80' },
    { name: 'Problem Solver', min: 500,  accent: '#e9c176' },
    { name: 'Algorithm Ace',  min: 1000, accent: '#facc15' },
    { name: 'Code Architect', min: 2000, accent: '#60a5fa' },
    { name: 'Coding Legend',  min: 5000, accent: '#ff6b6b' },
];
