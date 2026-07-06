/**
 * AchievementPoster — exact pixel-match of the 6 achievement posters.
 * Pure code-rendered (CSS gradients replace background images).
 */
import { useEffect, useRef } from 'react';

const STYLES = {
    bg: '#131313', border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176',
    muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1',

    glass: { background: 'rgba(19,19,19,0.6)', backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)' },
    shimmer: {
        background: 'linear-gradient(90deg, #d4c4b7 0%, #ffffff 50%, #d4c4b7 100%)', backgroundSize: '200% auto',
        color: 'transparent', WebkitBackgroundClip: 'text', backgroundClip: 'text',
        animation: 'achv-shimmer 3s linear infinite',
    },
    badge: (accent) => ({ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '6px', padding: '6px 20px', border: `1px solid ${accent}44`, borderRadius: '100px', fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.2em', color: accent, textTransform: 'uppercase', backgroundColor: 'rgba(19,19,19,0.55)', backdropFilter: 'blur(12px)' }),
    corner: (size, color) => ({ position: 'absolute', width: size, height: size, borderTop: `1px solid ${color}`, borderLeft: `1px solid ${color}`, opacity: 0.5 }),
};

const GDS = ({ opacity = 0.04 }) => {
    const ref = useRef(null);
    useEffect(() => {
        const c = ref.current; if (!c) return;
        const w = c.width = c.offsetWidth; const h = c.height = c.offsetHeight;
        const ctx = c.getContext('2d');
        ctx.fillStyle = `rgba(229,226,225,${opacity})`;
        for (let x = 0; x < w; x += 16) for (let y = 0; y < h; y += 16) ctx.fillRect(x, y, 1, 1);
    }, [opacity]);
    return <canvas ref={ref} style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }} />;
};

const TierLine = ({ label, color }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', opacity: 0.6 }}>
        <div style={{ width: 24, height: 1, backgroundColor: color }} />
        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.15em', color, textTransform: 'uppercase' }}>Tier: {label}</span>
        <div style={{ width: 24, height: 1, backgroundColor: color }} />
    </div>
);

const PlatformBrand = ({ accent, locked }) => (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
        <div style={{ width: 24, height: 1, backgroundColor: STYLES.border, marginBottom: '4px' }} />
        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '18px', fontWeight: 600, fontStyle: 'italic', color: locked ? STYLES.outline : accent + 'AA', letterSpacing: '0.06em' }}>CodeCoder</span>
    </div>
);

const LockedContent = ({ min }) => (
    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: STYLES.outline, textAlign: 'center', opacity: 0.5, margin: 0 }}>
        Earn {min} points to unlock
    </p>
);

// ── HELLO WORLD (50 pts) — Architectural Noir ─────────────────────────────
const HelloWorld = ({ locked, accent = STYLES.primary }) => (
    <>
        <img src="/achievements/achievement_poster_hello_world_50_points_final.webp" alt=""
            style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', opacity: locked ? 0.2 : 0.85, mixBlendMode: 'luminosity' }} />
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, #131313, transparent 40%, #131313cc)' }} />
        <GDS opacity={locked ? 0.015 : 0.05} />
        <div style={{ position: 'absolute', top: 16, left: 16, ...STYLES.corner(16, accent) }} />
        <div style={{ position: 'absolute', top: 16, right: 16, borderTop: `1px solid ${accent}`, borderRight: `1px solid ${accent}`, width: 16, height: 16, opacity: 0.5 }} />
        <div style={{ position: 'absolute', bottom: 16, left: 16, borderBottom: `1px solid ${accent}`, borderLeft: `1px solid ${accent}`, width: 16, height: 16, opacity: 0.5 }} />
        <div style={{ position: 'absolute', bottom: 16, right: 16, borderBottom: `1px solid ${accent}`, borderRight: `1px solid ${accent}`, width: 16, height: 16, opacity: 0.5 }} />

        <div style={{ ...STYLES.glass, padding: '4px 18px', borderRadius: 32, marginTop: 20, display: 'inline-flex', border: `1px solid ${locked ? STYLES.border : accent}33` }}>
            <span className="material-symbols-outlined" style={{ fontSize: 12, color: accent, marginRight: 6, fontVariationSettings: "'FILL' 1" }}>{locked ? 'lock' : 'workspace_premium'}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.2em', color: accent, textTransform: 'uppercase' }}>Achievement Unlocked</span>
        </div>

        <div style={{ position: 'absolute', left: 24, right: 24, top: '12%', bottom: '12%', border: `1px solid ${locked ? STYLES.border : accent}20`, pointerEvents: 'none', zIndex: 0 }} />

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12 }}>
            <TierLine label="Beginner" color={accent} />
            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: 30, fontWeight: 700, color: locked ? STYLES.outline : STYLES.onBg, margin: 0, textShadow: locked ? 'none' : `0 0 16px ${accent}22` }}>
                {locked ? <span style={STYLES.shimmer}>Hello World</span> : 'Hello World'}
            </h2>
            <div style={{ ...STYLES.glass, padding: '5px 20px', border: `1px solid ${locked ? STYLES.border : STYLES.secondary}44` }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 10, letterSpacing: '0.1em', color: locked ? STYLES.outline : STYLES.secondary, textTransform: 'uppercase' }}>50 Points</span>
            </div>
            {locked ? <LockedContent min={50} /> : (
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: STYLES.outline, textAlign: 'center', lineHeight: 1.6, maxWidth: '82%', margin: 0 }}>Every great developer starts with a single line of code. Your journey has officially begun.</p>
            )}
        </div>
        <PlatformBrand accent={accent} locked={locked} />
    </>
);

// ── BUG HUNTER (100 pts) — Cyber Clean ────────────────────────────────────
const BugHunter = ({ locked, accent = '#4ade80' }) => (
    <>
        <img src="/achievements/achievement_poster_bug_hunter_100_points_final.webp" alt=""
            style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', opacity: locked ? 0.2 : 0.8, mixBlendMode: 'screen' }} />
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, #131313, transparent 30%, #131313aa)' }} />
        <div style={{ position: 'absolute', inset: '15% 8%', borderRadius: 8, border: `1px solid ${accent}10`, pointerEvents: 'none' }} />
        <div style={{ ...STYLES.glass, padding: '5px 20px', borderRadius: 32, marginTop: 22, display: 'inline-flex', border: `1px solid ${locked ? STYLES.border : accent}33` }}>
            <span className="material-symbols-outlined" style={{ fontSize: 12, color: accent, marginRight: 6, fontVariationSettings: "'FILL' 1" }}>{locked ? 'lock' : 'bug_report'}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.2em', color: accent, textTransform: 'uppercase' }}>Achievement Unlocked</span>
        </div>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
            <div style={{ width: 32, height: 1, background: `linear-gradient(90deg,transparent,${accent}66,transparent)` }} />
            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: 38, fontWeight: 700, color: locked ? STYLES.outline : STYLES.onBg, margin: 0 }}>Bug Hunter</h2>
            <div style={{ ...STYLES.glass, padding: '6px 22px', border: `1px solid ${locked ? STYLES.border : accent}33`, marginTop: 4 }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 12, letterSpacing: '0.15em', color: accent, textTransform: 'uppercase', fontWeight: 500 }}>100 Points</span>
            </div>
            <div style={{ width: 48, height: 1, background: `linear-gradient(90deg,transparent,${accent}44,transparent)`, opacity: 0.5, marginTop: 4 }} />
            {locked ? <LockedContent min={100} /> : (
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: STYLES.outline, textAlign: 'center', lineHeight: 1.6, maxWidth: '75%', margin: 0 }}>You have conquered challenges and eliminated bugs. Keep sharpening your problem-solving skills.</p>
            )}
            <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 6, opacity: 0.5 }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.1em', color: accent }}>Intermediate Tier</span>
            </div>
        </div>
        <PlatformBrand accent={accent} locked={locked} />
    </>
);

// ── PROBLEM SOLVER (500 pts) — Architectural Shimmer ──────────────────────
const ProblemSolver = ({ locked, accent = '#e9c176' }) => (
    <>
        <img src="/achievements/achievement_poster_problem_solver_500_points_final.webp" alt=""
            style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', opacity: locked ? 0.2 : 0.8, mixBlendMode: 'luminosity' }} />
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to bottom, #131313ee, transparent 35%, #131313ee)' }} />
        <div style={{ position: 'absolute', inset: 12, border: `1px solid ${accent}15`, pointerEvents: 'none' }} />
        <div style={{ ...STYLES.glass, padding: '5px 22px', borderRadius: 32, marginTop: 22, display: 'inline-flex', border: `1px solid ${locked ? STYLES.border : accent}33` }}>
            <span className="material-symbols-outlined" style={{ fontSize: 12, color: accent, marginRight: 6, fontVariationSettings: "'FILL' 1" }}>{locked ? 'lock' : 'workspace_premium'}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.2em', color: accent, textTransform: 'uppercase' }}>Achievement Unlocked</span>
            <span className="material-symbols-outlined" style={{ fontSize: 12, color: accent, marginLeft: 6, fontVariationSettings: "'FILL' 1" }}>{locked ? 'lock' : 'workspace_premium'}</span>
        </div>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
            <TierLine label="Advanced" color={accent} />
            <h2 className={locked ? '' : 'shimmer-text'} style={{ fontFamily: "'Playfair Display', serif", fontSize: 34, fontWeight: 700, color: locked ? STYLES.outline : STYLES.onBg, margin: 0, lineHeight: 1.1, textAlign: 'center', ...(locked ? {} : STYLES.shimmer) }}>Problem<br/>Solver</h2>
            <div style={{ ...STYLES.glass, padding: '10px 30px', border: `1px solid ${locked ? STYLES.border : accent}33`, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                <div style={{ position: 'absolute', top: 0, left: 0, width: 4, height: 4, borderTop: `1px solid ${accent}`, borderLeft: `1px solid ${accent}` }} />
                <div style={{ position: 'absolute', top: 0, right: 0, width: 4, height: 4, borderTop: `1px solid ${accent}`, borderRight: `1px solid ${accent}` }} />
                <div style={{ position: 'absolute', bottom: 0, left: 0, width: 4, height: 4, borderBottom: `1px solid ${accent}`, borderLeft: `1px solid ${accent}` }} />
                <div style={{ position: 'absolute', bottom: 0, right: 0, width: 4, height: 4, borderBottom: `1px solid ${accent}`, borderRight: `1px solid ${accent}` }} />
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 10, letterSpacing: '0.15em', color: accent, textTransform: 'uppercase' }}>500 Points</span>
            </div>
            {locked ? <LockedContent min={500} /> : (
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: STYLES.outline, textAlign: 'center', lineHeight: 1.6, maxWidth: '80%', margin: 0 }}>Your persistence and logical thinking are turning obstacles into opportunities.</p>
            )}
        </div>
        <div style={{ width: 1, height: 40, background: `linear-gradient(to bottom, ${accent}55, transparent)`, marginBottom: 4 }} />
        <PlatformBrand accent={accent} locked={locked} />
    </>
);

// ── ALGORITHM ACE (1000 pts) — Master Command Center ──────────────────────
const AlgorithmAce = ({ locked, accent = '#facc15' }) => (
    <>
        <img src="/achievements/achievement_poster_algorithm_ace_1000_points_final.webp" alt=""
            style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', opacity: locked ? 0.2 : 0.8 }} />
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, #131313, transparent 35%, #131313cc)' }} />
        <div style={{ position: 'absolute', inset: 16, border: `1px solid ${accent}20`, pointerEvents: 'none', zIndex: 0 }} />
        <div style={{ position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)', width: 64, height: 1, backgroundColor: accent, opacity: 0.5 }} />
        <div style={{ position: 'absolute', bottom: 0, left: '50%', transform: 'translateX(-50%)', width: 64, height: 1, backgroundColor: accent, opacity: 0.5 }} />

        <div style={{ ...STYLES.glass, padding: '4px 20px', borderRadius: 32, marginTop: 22, display: 'inline-flex', border: `1px solid ${locked ? STYLES.border : accent}33` }}>
            <span className="material-symbols-outlined" style={{ fontSize: 12, color: accent, marginRight: 6, fontVariationSettings: "'FILL' 1" }}>{locked ? 'lock' : 'workspace_premium'}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.2em', color: accent, textTransform: 'uppercase' }}>Achievement Unlocked</span>
        </div>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12 }}>
            <TierLine label="Master" color={accent} />
            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: 30, fontWeight: 700, color: locked ? STYLES.outline : STYLES.onBg, margin: 0, textShadow: locked ? 'none' : `0 0 20px ${accent}22` }}>Algorithm Ace</h2>
            <div style={{ ...STYLES.glass, padding: '6px 24px', borderTop: `2px solid ${accent}66` }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 11, letterSpacing: '0.12em', fontWeight: 500, color: accent, textTransform: 'uppercase', textShadow: `0 0 12px ${accent}33` }}>1000 Points</span>
            </div>
            {locked ? <LockedContent min={1000} /> : (
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: STYLES.outline, textAlign: 'center', lineHeight: 1.6, maxWidth: '78%', margin: 0, fontStyle: 'italic' }}>"You have demonstrated exceptional analytical thinking and algorithmic mastery."</p>
            )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8, opacity: 0.5 }}>
            <span className="material-symbols-outlined" style={{ fontSize: 14, color: STYLES.outline }}>terminal</span>
            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: 18, fontWeight: 600, fontStyle: 'italic', color: locked ? STYLES.outline : accent + 'AA' }}>CodeCoder</span>
        </div>
    </>
);

// ── CODE ARCHITECT (2000 pts) — Grandmaster Blue ──────────────────────────
const CodeArchitect = ({ locked, accent = '#60a5fa' }) => (
    <>
        <img src="/achievements/achievement_poster_code_architect_2000_points_final.webp" alt=""
            style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', opacity: locked ? 0.2 : 0.8, mixBlendMode: 'screen' }} />
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, #131313 5%, transparent 40%, #131313dd)' }} />
        <GDS opacity={locked ? 0.01 : 0.04} />
        <div style={{ position: 'absolute', inset: 16, border: `1px solid ${accent}18`, borderRadius: 6, pointerEvents: 'none' }} />
        <div style={{ ...STYLES.glass, padding: '5px 22px', borderRadius: 32, marginTop: 22, display: 'inline-flex', border: `1px solid ${locked ? STYLES.border : accent}33`, borderTopWidth: 2, borderTopColor: accent }}>
            <span className="material-symbols-outlined" style={{ fontSize: 12, color: accent, marginRight: 6, fontVariationSettings: "'FILL' 1" }}>{locked ? 'lock' : 'workspace_premium'}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.2em', color: accent, textTransform: 'uppercase' }}>Achievement Unlocked</span>
        </div>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12 }}>
            <div style={{ width: 40, height: 40, borderRadius: '50%', border: `1px solid ${locked ? STYLES.border : accent}`, display: 'flex', alignItems: 'center', justifyContent: 'center', ...STYLES.glass, boxShadow: locked ? 'none' : `0 0 24px ${accent}18` }}>
                <span className="material-symbols-outlined" style={{ fontSize: 20, color: accent, fontVariationSettings: "'FILL' 0" }}>architecture</span>
            </div>
            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: 30, fontWeight: 700, color: locked ? STYLES.outline : STYLES.onBg, margin: 0, lineHeight: 1.1 }}>Code Architect</h2>
            <div style={{ position: 'relative' }}>
                <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: 1, background: `linear-gradient(90deg,transparent,${accent}66,transparent)`, opacity: 0.5 }} />
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.12em', color: accent, textTransform: 'uppercase', padding: '4px 0', display: 'block' }}>Grandmaster • 2000 Points</span>
            </div>
            {locked ? <LockedContent min={2000} /> : (
                <div style={{ ...STYLES.glass, padding: '10px 18px', borderLeft: `2px solid ${accent}`, maxWidth: '80%' }}>
                    <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: STYLES.outline, lineHeight: 1.6, margin: 0, fontStyle: 'italic' }}>"You are building solutions with vision, structure, and technical excellence."</p>
                </div>
            )}
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, opacity: 0.25 }}>
                <div style={{ width: 40, height: 1, backgroundColor: STYLES.muted }} />
                <div style={{ width: 4, height: 4, transform: 'rotate(45deg)', backgroundColor: accent }} />
                <div style={{ width: 40, height: 1, backgroundColor: STYLES.muted }} />
            </div>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', width: '100%', padding: '0 20px' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 8, color: STYLES.outline }}>Platform</span>
                <span style={{ fontFamily: "'Playfair Display', serif", fontSize: 16, fontWeight: 600, fontStyle: 'italic', color: locked ? STYLES.outline : accent + 'AA' }}>CodeCoder</span>
            </div>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 8, color: STYLES.outline, opacity: 0.5 }}>ID_0x7B9A</span>
        </div>
    </>
);

// ── CODING LEGEND (5000 pts) — Legend Red ─────────────────────────────────
const CodingLegend = ({ locked, accent = '#ff6b6b' }) => (
    <>
        <img src="/achievements/achievement_poster_coding_legend_5000_points_final.webp" alt=""
            style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', opacity: locked ? 0.2 : 0.8, mixBlendMode: 'screen' }} />
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, #131313, transparent 30%, #131313cc)' }} />
        <GDS opacity={locked ? 0.01 : 0.03} />
        <div style={{ position: 'absolute', inset: 0, background: `radial-gradient(ellipse at 50% 40%, ${accent}08, transparent 60%)`, pointerEvents: 'none' }} />
        <div style={{ position: 'absolute', top: 12, left: 12, width: 10, height: 10, borderTop: `1px solid ${accent}`, borderLeft: `1px solid ${accent}`, opacity: 0.5 }} />
        <div style={{ position: 'absolute', top: 12, right: 12, width: 10, height: 10, borderTop: `1px solid ${accent}`, borderRight: `1px solid ${accent}`, opacity: 0.5 }} />
        <div style={{ position: 'absolute', bottom: 12, left: 12, width: 10, height: 10, borderBottom: `1px solid ${accent}`, borderLeft: `1px solid ${accent}`, opacity: 0.5 }} />
        <div style={{ position: 'absolute', bottom: 12, right: 12, width: 10, height: 10, borderBottom: `1px solid ${accent}`, borderRight: `1px solid ${accent}`, opacity: 0.5 }} />

        <div style={{ ...STYLES.glass, padding: '4px 20px', borderRadius: 32, marginTop: 22, display: 'inline-flex', border: `1px solid ${locked ? STYLES.border : accent}33` }}>
            <span className="material-symbols-outlined" style={{ fontSize: 12, color: accent, marginRight: 6, fontVariationSettings: "'FILL' 1" }}>{locked ? 'lock' : 'stars'}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.2em', color: accent, textTransform: 'uppercase' }}>Achievement Unlocked</span>
        </div>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
            <div style={{ position: 'absolute', top: '30%', left: '50%', transform: 'translate(-50%,-50%)', width: '60%', height: '10%', background: `radial-gradient(ellipse, ${accent}12, transparent)`, pointerEvents: 'none' }} />
            <div style={{ width: 40, height: 1, background: `linear-gradient(90deg,transparent,${accent}55,transparent)` }} />
            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: 36, fontWeight: 700, color: locked ? STYLES.outline : STYLES.primary, margin: 0, textShadow: locked ? 'none' : `0 0 30px ${accent}22` }}>Coding Legend</h2>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 9, letterSpacing: '0.12em', color: STYLES.outline, textTransform: 'uppercase' }}>Current Tier</span>
                <div style={{ borderBottom: `2px solid ${accent}55`, paddingBottom: 4, boxShadow: locked ? 'none' : `0 0 20px ${accent}15` }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 10, letterSpacing: '0.12em', color: accent, fontWeight: 500, textTransform: 'uppercase' }}>5000 Points</span>
                </div>
            </div>
            <div style={{ width: 40, height: 1, background: `linear-gradient(90deg,transparent,${accent}55,transparent)` }} />
            {locked ? <LockedContent min={5000} /> : (
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: 13, color: STYLES.outline, textAlign: 'center', lineHeight: 1.6, maxWidth: '75%', margin: 0 }}>A milestone reserved for the elite. Your dedication and expertise have earned legendary status.</p>
            )}
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', width: '100%', padding: '0 20px', borderTop: `1px solid ${STYLES.border}`, paddingTop: 12 }}>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 8, color: STYLES.outline, opacity: 0.5 }}>ID / 0X-LGND-5K</span>
            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: 16, fontWeight: 600, fontStyle: 'italic', color: locked ? STYLES.outline : STYLES.primary, opacity: 0.8 }}>CodeCoder</span>
        </div>
    </>
);

// ── EXPORT ─────────────────────────────────────────────────────────────────

const POSTERS = {
    'Hello World':    HelloWorld,
    'Bug Hunter':     BugHunter,
    'Problem Solver': ProblemSolver,
    'Algorithm Ace':  AlgorithmAce,
    'Code Architect': CodeArchitect,
    'Coding Legend':  CodingLegend,
};

export default function AchievementPoster({ tier, unlocked }) {
    const Poster = POSTERS[tier.name];
    const accent = tier.accent;
    const locked = !unlocked;

    return (
        <div style={{
            aspectRatio: '4/5', width: '100%', maxWidth: 540, position: 'relative',
            overflow: 'hidden', backgroundColor: STYLES.bg,
            border: `1px solid ${STYLES.border}`,
            boxShadow: locked ? 'none' : `0 0 30px ${accent}18`,
            opacity: locked ? 0.35 : 1,
            filter: locked ? 'grayscale(1)' : 'none',
            transition: 'all 0.5s ease',
            display: 'flex', flexDirection: 'column', alignItems: 'center',
            fontFamily: "'Geist', sans-serif", color: STYLES.onBg,
        }}>
            {Poster ? <Poster locked={locked} accent={accent} /> : null}
            <style>{`
                @keyframes achv-shimmer{0%{background-position:200% center}100%{background-position:-200% center}}
            `}</style>
        </div>
    );
}

export { STYLES as PosterStyles };
