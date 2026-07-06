/**
 * AchievementCard — pure code-rendered poster (no external images).
 * Points unlock tiers:
 *   50+  → Hello World    (Beginner)
 *   100+ → Bug Hunter     (Intermediate)
 *   500+ → Problem Solver (Advanced)
 *   1000+→ Algorithm Ace  (Master)
 *   2000+→ Code Architect (Grandmaster)
 *   5000+→ Coding Legend  (Legend)
 */
import { useState, useEffect, useRef } from 'react';

const TIERS = [
    { name: 'Hello World',    min: 50,   tier: 'Beginner',     accent: '#f1bc8b', icon: 'terminal',   desc: 'Every great developer starts with a single line of code.' },
    { name: 'Bug Hunter',     min: 100,  tier: 'Intermediate', accent: '#4ade80', icon: 'bug_report', desc: 'You have conquered challenges and eliminated bugs.' },
    { name: 'Problem Solver', min: 500,  tier: 'Advanced',     accent: '#e9c176', icon: 'extension',  desc: 'Your persistence and logical thinking turn obstacles into opportunities.' },
    { name: 'Algorithm Ace',  min: 1000, tier: 'Master',       accent: '#facc15', icon: 'analytics',  desc: 'Exceptional analytical thinking and algorithmic mastery.' },
    { name: 'Code Architect', min: 2000, tier: 'Grandmaster',   accent: '#60a5fa', icon: 'architecture', desc: 'Building solutions with vision, structure, and technical excellence.' },
    { name: 'Coding Legend',  min: 5000, tier: 'Legend',        accent: '#ff6b6b', icon: 'stars', desc: 'A milestone reserved for the elite. Legendary status earned.' },
];

const C = {
    bg: '#131313', surfaceLow: '#1c1b1b', surfaceHi: '#2a2a2a',
    border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176',
    muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1',
};

// ── Pure-SVG decorative elements ──────────────────────────────────────────

const CornerAccent = ({ color, size = 8 }) => (
    <svg width={size} height={size} style={{ position: 'absolute', opacity: 0.6 }}>
        <rect width={size} height={1} fill={color} />
        <rect width={1} height={size} fill={color} />
    </svg>
);

const GridDots = ({ opacity = 0.08 }) => {
    const ref = useRef(null);
    useEffect(() => {
        const c = ref.current;
        if (!c) return;
        const ctx = c.getContext('2d');
        const w = c.width = c.offsetWidth;
        const h = c.height = c.offsetHeight;
        ctx.fillStyle = `rgba(229,226,225,${opacity})`;
        for (let x = 0; x < w; x += 12)
            for (let y = 0; y < h; y += 12)
                ctx.fillRect(x, y, 1, 1);
    }, [opacity]);
    return <canvas ref={ref} style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }} />;
};

const SparkleMark = ({ accent }) => (
    <svg width="60" height="60" viewBox="0 0 60 60" style={{ position: 'absolute', opacity: 0.25, animation: 'achv-sparkle 3s ease-in-out infinite' }}>
        <circle cx="30" cy="30" r="1.5" fill={accent} />
        <line x1="30" y1="2" x2="30" y2="18" stroke={accent} strokeWidth="0.5" />
        <line x1="30" y1="42" x2="30" y2="58" stroke={accent} strokeWidth="0.5" />
        <line x1="2" y1="30" x2="18" y2="30" stroke={accent} strokeWidth="0.5" />
        <line x1="42" y1="30" x2="58" y2="30" stroke={accent} strokeWidth="0.5" />
        <line x1="10" y1="10" x2="21" y2="21" stroke={accent} strokeWidth="0.3" />
        <line x1="39" y1="39" x2="50" y2="50" stroke={accent} strokeWidth="0.3" />
        <line x1="10" y1="50" x2="21" y2="39" stroke={accent} strokeWidth="0.3" />
        <line x1="39" y1="21" x2="50" y2="10" stroke={accent} strokeWidth="0.3" />
    </svg>
);

// ── Tier-specific decoration ───────────────────────────────────────────────

const TierDecor = ({ tier, accent }) => {
    if (tier === 'Beginner') return <SparkleMark accent={accent} />;
    if (tier === 'Legend') return (
        <svg width="80" height="80" viewBox="0 0 80 80" style={{ position: 'absolute', opacity: 0.3, animation: 'achv-pulse 4s ease-in-out infinite' }}>
            <circle cx="40" cy="40" r="30" fill="none" stroke={accent} strokeWidth="0.5" />
            <circle cx="40" cy="40" r="25" fill="none" stroke={accent} strokeWidth="0.3" strokeDasharray="4 4" />
            <circle cx="40" cy="40" r="35" fill="none" stroke={accent} strokeWidth="0.2" />
            <line x1="40" y1="8" x2="40" y2="30" stroke={accent} strokeWidth="1" />
            <line x1="40" y1="50" x2="40" y2="72" stroke={accent} strokeWidth="1" />
            <line x1="8" y1="40" x2="30" y2="40" stroke={accent} strokeWidth="1" />
            <line x1="50" y1="40" x2="72" y2="40" stroke={accent} strokeWidth="1" />
        </svg>
    );
    if (['Master', 'Grandmaster'].includes(tier)) return (
        <svg width="100" height="80" viewBox="0 0 100 80" style={{ position: 'absolute', opacity: 0.2 }}>
            <path d="M50 5 L65 40 L95 40 L70 55 L80 80 L50 65 L20 80 L30 55 L5 40 L35 40 Z" fill="none" stroke={accent} strokeWidth="0.5" />
            <path d="M50 20 L58 42 L80 42 L62 55 L68 73 L50 60 L32 73 L38 55 L20 42 L42 42 Z" fill="none" stroke={accent} strokeWidth="0.3" />
        </svg>
    );
    return <SparkleMark accent={accent} />;
};

const AchievementGlow = ({ accent, tier }) => {
    const opacity = tier === 'Legend' ? '0.15' : tier === 'Grandmaster' ? '0.1' : '0.05';
    return (
        <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)',
            width: '60%', height: '40%', background: `radial-gradient(ellipse, ${accent}${Math.round(parseFloat(opacity)*255).toString(16).padStart(2,'0')}, transparent 70%)`,
            pointerEvents: 'none', animation: 'achv-pulse 4s ease-in-out infinite' }} />
    );
};

// ── Shimmer heading for Advanced+ ──────────────────────────────────────────

const ShimmerHeading = ({ children, enabled }) => {
    if (!enabled) return <>{children}</>;
    return (
        <span style={{
            background: 'linear-gradient(90deg, #d4c4b7 0%, #ffffff 40%, #d4c4b7 80%)',
            backgroundSize: '200% auto', color: 'transparent', WebkitBackgroundClip: 'text', backgroundClip: 'text',
            animation: 'achv-shimmer 3s linear infinite',
        }}>{children}</span>
    );
};

// ── Main Card ──────────────────────────────────────────────────────────────

export default function AchievementCard({ tier, unlocked }) {
    const { name, accent, icon, desc, tier: tierLabel } = tier;
    const isUnlocked = !!unlocked;

    return (
        <div style={{
            aspectRatio: '4 / 5', width: '100%', maxWidth: '360px', position: 'relative',
            overflow: 'hidden', backgroundColor: C.bg, border: `1px solid ${C.border}`,
            boxShadow: isUnlocked ? `0 0 30px ${accent}18` : 'none',
            opacity: isUnlocked ? 1 : 0.3, filter: isUnlocked ? 'none' : 'grayscale(1)',
            transition: 'all 0.5s ease', display: 'flex', flexDirection: 'column',
            fontFamily: "'Geist', sans-serif",
        }}>
            {/* Grid background */}
            <GridDots opacity={isUnlocked ? 0.06 : 0.02} />

            {/* Glow */}
            {isUnlocked && <AchievementGlow accent={accent} tier={tierLabel} />}

            {/* Corner accents */}
            <CornerAccent color={isUnlocked ? accent : C.outline} size={16} />
            <div style={{ position: 'absolute', top: 0, right: 0 }}><CornerAccent color={isUnlocked ? accent : C.outline} size={16} /></div>
            <div style={{ position: 'absolute', bottom: 0, left: 0 }}><CornerAccent color={isUnlocked ? accent : C.outline} size={16} /></div>
            <div style={{ position: 'absolute', bottom: 0, right: 0 }}><CornerAccent color={isUnlocked ? accent : C.outline} size={16} /></div>

            {/* Tier decor background */}
            <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', pointerEvents: 'none' }}>
                <TierDecor tier={tierLabel} accent={accent} />
            </div>

            {/* Top: badge */}
            <div style={{ display: 'flex', justifyContent: 'center', paddingTop: '20px', position: 'relative', zIndex: 1 }}>
                <span style={{
                    display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '6px 16px',
                    border: `1px solid ${isUnlocked ? accent + '44' : C.border}`,
                    borderRadius: '100px', backgroundColor: 'rgba(19,19,19,0.6)', backdropFilter: 'blur(12px)',
                    fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.2em',
                    color: isUnlocked ? accent : C.outline, textTransform: 'uppercase',
                }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '12px', fontVariationSettings: "'FILL' 1" }}>
                        {isUnlocked ? icon : 'lock'}
                    </span>
                    {isUnlocked ? 'Achievement Unlocked' : 'Locked'}
                </span>
            </div>

            {/* Center: content */}
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '0 24px', position: 'relative', zIndex: 1, gap: '12px' }}>
                {/* Tier line */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', opacity: isUnlocked ? 0.6 : 0.3 }}>
                    <div style={{ width: 20, height: 1, backgroundColor: isUnlocked ? accent : C.border }} />
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.15em', color: isUnlocked ? accent : C.outline, textTransform: 'uppercase' }}>
                        Tier: {tierLabel}
                    </span>
                    <div style={{ width: 20, height: 1, backgroundColor: isUnlocked ? accent : C.border }} />
                </div>

                {/* Title */}
                <h2 style={{
                    fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 700, lineHeight: 1.1,
                    textAlign: 'center', margin: 0, color: isUnlocked ? C.onBg : C.outline,
                    textShadow: isUnlocked ? `0 0 20px ${accent}33` : 'none',
                }}>
                    <ShimmerHeading enabled={isUnlocked && ['Advanced', 'Master', 'Grandmaster', 'Legend'].includes(tierLabel)}>
                        {name}
                    </ShimmerHeading>
                </h2>

                {/* Points */}
                <div style={{
                    display: 'inline-flex', flexDirection: 'column', alignItems: 'center', gap: '2px',
                    padding: '6px 20px', border: `1px solid ${isUnlocked ? accent + '33' : C.border}`,
                    backgroundColor: 'rgba(19,19,19,0.5)', backdropFilter: 'blur(8px)',
                }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.12em', color: isUnlocked ? C.muted : C.outline, textTransform: 'uppercase' }}>
                        {tier.min} Points
                    </span>
                </div>

                {/* Description */}
                {isUnlocked && (
                    <p style={{
                        fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.outline,
                        textAlign: 'center', lineHeight: 1.6, maxWidth: '85%', margin: 0,
                    }}>{desc}</p>
                )}

                {!isUnlocked && (
                    <p style={{
                        fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline,
                        textAlign: 'center', lineHeight: 1.5, opacity: 0.5, margin: 0,
                    }}>Earn {tier.min} points to unlock</p>
                )}
            </div>

            {/* Bottom: platform brand */}
            <div style={{ display: 'flex', justifyContent: 'center', paddingBottom: '20px', position: 'relative', zIndex: 1 }}>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '2px' }}>
                    <div style={{ width: 30, height: 1, backgroundColor: C.border, marginBottom: '4px' }} />
                    <span style={{
                        fontFamily: "'Playfair Display', serif", fontSize: '18px', fontWeight: 600, fontStyle: 'italic',
                        color: isUnlocked ? C.primary + 'AA' : C.outline, letterSpacing: '0.06em',
                    }}>CodeCoder</span>
                </div>
            </div>

            <style>{`
                @keyframes achv-sparkle { 0%,100%{opacity:0.2} 50%{opacity:0.4} }
                @keyframes achv-pulse { 0%,100%{opacity:1} 50%{opacity:0.6} }
                @keyframes achv-shimmer { 0%{background-position:200% center} 100%{background-position:-200% center} }
            `}</style>
        </div>
    );
}

export { TIERS };
