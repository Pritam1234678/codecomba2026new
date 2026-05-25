import { motion } from 'framer-motion';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg:         '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceMin: '#0e0e0e',
    border:     '#50453b',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    tertiary:   '#f4bb92',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
};

const fade = (delay = 0) => ({
    initial: { opacity: 0, y: 20 },
    animate: { opacity: 1, y: 0 },
    transition: { duration: 0.5, delay },
});

const PlatformDetails = () => {
    const { isMobile } = useResponsive();
    return (
    <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", padding: isMobile ? '24px 16px' : '48px 64px', maxWidth: '1200px', margin: '0 auto' }}>

        {/* ── Hero Manifesto ── */}
        <motion.section {...fade(0)} style={{ marginBottom: '4rem', paddingBottom: '3rem', borderBottom: `1px solid ${C.border}` }}>
            <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '56px', fontWeight: 700, lineHeight: 1.1, letterSpacing: '-0.02em', color: C.primary, marginBottom: '1.5rem' }}>
                The Architectural Arena
            </h1>
            <p style={{ fontSize: '18px', color: C.muted, lineHeight: 1.7, maxWidth: '720px', fontWeight: 300 }}>
                Code Combat is not a game. It is a cinematic command center designed for the quiet intensity of algorithmic warfare. Every execution cycle, every memory allocation is treated as high-stakes literature within our un-decorated, precision-engineered environment. Built from scratch by a single architect — every layer deliberate, every dependency justified.
            </p>
        </motion.section>

        {/* ── Main Grid: Editor + Tech Stack ── */}
        <motion.section {...fade(0.1)} style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '2fr 1fr', gap: '1px', marginBottom: '1px', backgroundColor: C.border }}>
            {/* Monaco Editor Panel */}
            <div style={{ backgroundColor: C.surfaceCon, padding: '2.5rem', display: 'flex', flexDirection: 'column' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1.5rem', paddingBottom: '1rem', borderBottom: `1px solid ${C.border}` }}>
                    <span className="material-symbols-outlined" style={{ color: C.primary, fontSize: '20px' }}>code</span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Monaco Editor Integration</span>
                </div>
                <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.primary, marginBottom: '1rem' }}>
                    The Canvas of Combat
                </h2>
                <p style={{ fontSize: '15px', color: C.muted, lineHeight: 1.7, marginBottom: '2rem', flexGrow: 1 }}>
                    Powered by the same core as VS Code, our Monaco implementation provides semantic highlighting, intelligent autocompletion, and a pristine typographical environment using JetBrains Mono. Distractions are eliminated; only logic remains. Supports C, C++, Java, Python, and JavaScript with full harness injection.
                </p>
                {/* Code block */}
                <div style={{ backgroundColor: C.surfaceMin, border: `1px solid ${C.border}`, padding: '1.5rem', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', lineHeight: 1.6 }}>
                    <div style={{ color: C.primary }}>{'// USER_CODE_START'}</div>
                    <div style={{ color: C.secondary, marginTop: '4px' }}>{'int twoSum(int* nums, int numsSize, int target, int* returnSize) {'}</div>
                    <div style={{ paddingLeft: '24px', color: C.muted }}>{'// write your solution here'}</div>
                    <div style={{ paddingLeft: '24px', color: C.muted }}>{'*returnSize = 0;'}</div>
                    <div style={{ paddingLeft: '24px', color: C.muted }}>{'return NULL;'}</div>
                    <div style={{ color: C.secondary }}>{'}'}</div>
                    <div style={{ color: C.primary, marginTop: '4px' }}>{'// USER_CODE_END'}</div>
                    <div style={{ color: C.primary, animation: 'blink 1s infinite', display: 'inline-block', marginTop: '8px' }}>█</div>
                </div>
            </div>

            {/* Tech Stack Panel */}
            <div style={{ backgroundColor: C.surfaceCon, padding: '2.5rem', borderTop: `2px solid ${C.secondary}` }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1.5rem' }}>
                    <span className="material-symbols-outlined" style={{ color: C.secondary, fontSize: '20px' }}>memory</span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Tech Stack</span>
                </div>
                <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 600, color: C.primary, marginBottom: '1.5rem' }}>
                    Precision Engineered
                </h2>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {[
                        { dot: C.primary,    text: 'React 19 + Vite + TailwindCSS' },
                        { dot: C.secondary,  text: 'Spring Boot 3.5 + Java 21' },
                        { dot: C.tertiary,   text: 'PostgreSQL + Redis (Valkey)' },
                        { dot: C.primary,    text: 'Oracle Cloud VM (ARM64)' },
                        { dot: C.secondary,  text: 'ProcessBuilder Judge Engine' },
                        { dot: C.tertiary,   text: 'JWT + Spring Security' },
                        { dot: C.outline,    text: 'SSE Real-time Verdicts' },
                        { dot: C.primary,    text: 'Framer Motion + GSAP' },
                    ].map(({ dot, text }) => (
                        <div key={text} style={{ display: 'flex', alignItems: 'center', gap: '12px', paddingBottom: '10px', borderBottom: `1px solid rgba(80,69,59,0.3)` }}>
                            <div style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: dot, flexShrink: 0 }} />
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>{text}</span>
                        </div>
                    ))}
                </div>
            </div>
        </motion.section>

        {/* ── Judge Engine Architecture ── */}
        <motion.section {...fade(0.2)} style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: '3rem', marginBottom: '1px', position: 'relative', overflow: 'hidden' }}>
            <div style={{ position: 'absolute', top: 0, right: 0, width: '400px', height: '400px', background: 'radial-gradient(circle at 100% 0%, rgba(233,193,118,0.06) 0%, transparent 60%)', pointerEvents: 'none' }} />
            <div style={{ maxWidth: '640px', marginBottom: '3rem', position: 'relative', zIndex: 1 }}>
                <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '32px', fontWeight: 600, color: C.primary, marginBottom: '1rem' }}>
                    Judge Engine Architecture
                </h2>
                <p style={{ fontSize: '15px', color: C.muted, lineHeight: 1.7 }}>
                    Our execution engine isolates submissions using Java's <code style={{ color: C.secondary, fontFamily: "'JetBrains Mono', monospace" }}>ProcessBuilder</code> with strict resource limits. Verdicts are streamed in real-time via Server-Sent Events (SSE), ensuring a fluid, instantaneous feedback loop without polling latency. The queue is backed by Redis for durability across restarts.
                </p>
            </div>

            {/* Architecture diagram */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0', position: 'relative', zIndex: 1, overflowX: 'auto' }}>
                {[
                    { label: 'CLIENT', sub: 'Monaco Editor', icon: 'terminal', color: C.primary },
                    null, // arrow
                    { label: 'API GATEWAY', sub: 'Spring Boot', icon: 'router', color: C.secondary },
                    null,
                    { label: 'REDIS QUEUE', sub: 'Valkey / Redis', icon: 'queue', color: C.tertiary },
                    null,
                    { label: 'JUDGE WORKERS', sub: 'ProcessBuilder × 4', icon: 'code', color: C.primary },
                ].map((item, i) => {
                    if (!item) return (
                        <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '0 8px', flexShrink: 0 }}>
                            <div style={{ width: '48px', height: '1px', backgroundColor: C.border, position: 'relative' }}>
                                <div style={{ position: 'absolute', top: '-8px', left: '50%', transform: 'translateX(-50%)', fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, whiteSpace: 'nowrap', backgroundColor: C.surfaceLow, padding: '0 4px' }}>
                                    {i === 1 ? 'HTTP' : i === 3 ? 'PUSH' : 'BRPOP'}
                                </div>
                            </div>
                            <span style={{ color: C.outline, fontSize: '16px', marginTop: '4px' }}>→</span>
                        </div>
                    );
                    return (
                        <div key={i} style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceCon, padding: '1.5rem', textAlign: 'center', minWidth: '160px', flexShrink: 0, borderTop: `2px solid ${item.color}` }}>
                            <span className="material-symbols-outlined" style={{ color: item.color, fontSize: '28px', display: 'block', marginBottom: '8px' }}>{item.icon}</span>
                            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.12em', color: item.color, textTransform: 'uppercase', marginBottom: '4px' }}>{item.label}</div>
                            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>{item.sub}</div>
                        </div>
                    );
                })}
                {/* SSE back arrow */}
                <div style={{ marginLeft: '16px', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.secondary, letterSpacing: '0.1em', textTransform: 'uppercase', writingMode: 'vertical-rl', transform: 'rotate(180deg)', opacity: 0.7 }}>
                    ← SSE STREAM BACK TO CLIENT
                </div>
            </div>
        </motion.section>

        {/* ── Infrastructure ── */}
        <motion.section {...fade(0.3)} style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(3, 1fr)', gap: '1px', backgroundColor: C.border, marginBottom: '1px' }}>
            {[
                {
                    icon: 'cloud',
                    label: 'Cloud Infrastructure',
                    title: 'Oracle Cloud VM',
                    color: C.primary,
                    items: [
                        'ARM64 Ampere A1 (4 vCPU, 24GB RAM)',
                        'Always Free Tier — zero cost',
                        'Ubuntu 22.04 LTS',
                        'Nginx reverse proxy',
                        'SSL/TLS via Let\'s Encrypt',
                    ],
                },
                {
                    icon: 'storage',
                    label: 'Data Layer',
                    title: 'PostgreSQL + Redis',
                    color: C.secondary,
                    items: [
                        'PostgreSQL 15 — primary datastore',
                        'HikariCP connection pooling (20 max)',
                        'Redis (Valkey) — submission queue',
                        'Redis — contest & problem cache',
                        'Cache-aside pattern, 30s TTL',
                    ],
                },
                {
                    icon: 'security',
                    label: 'Security Layer',
                    title: 'JWT + Spring Security',
                    color: C.tertiary,
                    items: [
                        'Stateless JWT authentication',
                        'BCrypt password hashing (strength 10)',
                        'Role-based access control (RBAC)',
                        'CORS configured per environment',
                        'Account enable/disable controls',
                    ],
                },
            ].map(({ icon, label, title, color, items }) => (
                <div key={label} style={{ backgroundColor: C.surfaceCon, padding: '2rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem' }}>
                        <span className="material-symbols-outlined" style={{ color, fontSize: '20px' }}>{icon}</span>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>{label}</span>
                    </div>
                    <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 600, color, marginBottom: '1.5rem' }}>{title}</h3>
                    <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {items.map(item => (
                            <li key={item} style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted, lineHeight: 1.5 }}>
                                <span style={{ color, flexShrink: 0, marginTop: '2px' }}>›</span>
                                {item}
                            </li>
                        ))}
                    </ul>
                </div>
            ))}
        </motion.section>

        {/* ── Platform Stats ── */}
        <motion.section {...fade(0.4)} style={{ display: 'grid', gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)', gap: '1px', backgroundColor: C.border, marginBottom: '1px' }}>
            {[
                { label: 'Languages Supported', value: '5', sub: 'C, C++, Java, Python, JS' },
                { label: 'Judge Workers', value: '4', sub: 'Parallel execution threads' },
                { label: 'Execution Timeout', value: '10s', sub: 'Configurable per problem' },
                { label: 'Max Submission', value: '5MB', sub: 'Code + harness combined' },
            ].map(({ label, value, sub }) => (
                <div key={label} style={{ backgroundColor: C.surfaceCon, padding: '2rem', textAlign: 'center' }}>
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginBottom: '12px' }}>{label}</p>
                    <p style={{ fontFamily: "'Playfair Display', serif", fontSize: '40px', fontWeight: 300, color: C.primary, margin: 0 }}>{value}</p>
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, marginTop: '8px' }}>{sub}</p>
                </div>
            ))}
        </motion.section>

        {/* ── Features ── */}
        <motion.section {...fade(0.5)} style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: '2.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '2rem', paddingBottom: '1rem', borderBottom: `1px solid ${C.border}` }}>
                <span className="material-symbols-outlined" style={{ color: C.secondary, fontSize: '20px' }}>verified</span>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Platform Capabilities</span>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(3, 1fr)', gap: '16px' }}>
                {[
                    { icon: 'bolt',           text: 'Real-time SSE verdict streaming — no polling' },
                    { icon: 'code',           text: 'Code harness injection with USER_CODE_START/END markers' },
                    { icon: 'leaderboard',    text: 'Elo-based contest leaderboard with live updates' },
                    { icon: 'military_tech',  text: 'Full contest lifecycle — create, manage, judge' },
                    { icon: 'group',          text: 'Admin user management with account controls' },
                    { icon: 'lock',           text: 'JWT stateless auth with role-based routing' },
                    { icon: 'cached',         text: 'Redis cache-aside for problems, contests, profiles' },
                    { icon: 'email',          text: 'Gmail SMTP — password reset & username recovery' },
                    { icon: 'terminal',       text: 'Monaco editor with multi-language syntax support' },
                ].map(({ icon, text }) => (
                    <div key={text} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', padding: '1rem', border: `1px solid ${C.border}`, backgroundColor: C.surfaceCon }}>
                        <span className="material-symbols-outlined" style={{ color: C.secondary, fontSize: '18px', flexShrink: 0, marginTop: '2px' }}>{icon}</span>
                        <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.muted, lineHeight: 1.5 }}>{text}</span>
                    </div>
                ))}
            </div>
        </motion.section>

        <style>{`
            @keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
            .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
        `}</style>
    </div>
    );
};

export default PlatformDetails;
