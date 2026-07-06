import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import AuthService from '../services/auth.service';
import ContestService from '../services/contest.service';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg:         '#131313',
    surfaceLow: '#1c1b1b',
    surfaceCon: '#201f1f',
    surfaceHi:  '#2a2a2a',
    border:     'rgba(241,188,139,0.2)',
    borderSolid:'rgba(241,188,139,0.3)',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    success:    '#4ade80',
};

// ─── Animated Sphere (canvas ASCII) ─────────────────────────────────────────
function AnimatedSphere() {
    const canvasRef = useRef(null);
    const frameRef  = useRef(0);
    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const chars = 'CCCCCCCCCCCCCCCCCCCCC';
        let time = 0;
        const resize = () => {
            const dpr = window.devicePixelRatio || 1;
            const rect = canvas.getBoundingClientRect();
            canvas.width  = rect.width  * dpr;
            canvas.height = rect.height * dpr;
            ctx.scale(dpr, dpr);
        };
        resize();
        window.addEventListener('resize', resize);
        const render = () => {
            const rect = canvas.getBoundingClientRect();
            ctx.clearRect(0, 0, rect.width, rect.height);
            const cx = rect.width / 2, cy = rect.height / 2;
            const radius = Math.min(rect.width, rect.height) * 0.525;
            ctx.font = '12px monospace';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            const points = [];
            for (let phi = 0; phi < Math.PI * 2; phi += 0.15) {
                for (let theta = 0; theta < Math.PI; theta += 0.15) {
                    const x = Math.sin(theta) * Math.cos(phi + time * 0.5);
                    const y = Math.sin(theta) * Math.sin(phi + time * 0.5);
                    const z = Math.cos(theta);
                    const rotY = time * 0.3;
                    const newX = x * Math.cos(rotY) - z * Math.sin(rotY);
                    const newZ = x * Math.sin(rotY) + z * Math.cos(rotY);
                    const rotX = time * 0.2;
                    const newY = y * Math.cos(rotX) - newZ * Math.sin(rotX);
                    const fZ   = y * Math.sin(rotX) + newZ * Math.cos(rotX);
                    const depth = (fZ + 1) / 2;
                    points.push({ x: cx + newX * radius, y: cy + newY * radius, z: fZ, char: chars[Math.floor(depth * (chars.length - 1))] });
                }
            }
            points.sort((a, b) => a.z - b.z);
            points.forEach(p => {
                const alpha = 0.15 + (p.z + 1) * 0.35;
                ctx.fillStyle = `rgba(241,188,139,${alpha})`;
                ctx.fillText(p.char, p.x, p.y);
            });
            time += 0.02;
            frameRef.current = requestAnimationFrame(render);
        };
        render();
        return () => { window.removeEventListener('resize', resize); cancelAnimationFrame(frameRef.current); };
    }, []);
    return <canvas ref={canvasRef} style={{ display: 'block', width: '100%', height: '100%' }} />;
}

// ─── Animated SVG Visuals ───────────────────────────────────────────────────

function JudgeVisual() {
    return (
        <svg viewBox="0 0 200 160" style={{ width: '100%', height: '100%', color: C.primary }}>
            <rect x="30" y="20" width="140" height="120" rx="3" fill="none" stroke="currentColor" strokeWidth="1.5" opacity="0.4" />
            {[0,1,2,3,4,5].map(i => (
                <rect key={i} x="40" y={35 + i*16} width="120" height="8" rx="2" fill="currentColor" opacity="0.12">
                    <animate attributeName="opacity" values="0.12;0.7;0.12" dur="2s" begin={`${i*0.18}s`} repeatCount="indefinite" />
                    <animate attributeName="width" values="20;120;20" dur="2s" begin={`${i*0.18}s`} repeatCount="indefinite" />
                </rect>
            ))}
            <circle cx="100" cy="152" r="3" fill="currentColor" opacity="0.3">
                <animate attributeName="opacity" values="0.3;1;0.3" dur="1s" repeatCount="indefinite" />
            </circle>
        </svg>
    );
}

function EditorVisual() {
    return (
        <svg viewBox="0 0 200 160" style={{ width: '100%', height: '100%', color: C.secondary }}>
            <circle cx="100" cy="80" r="12" fill="currentColor">
                <animate attributeName="r" values="12;15;12" dur="2s" repeatCount="indefinite" />
            </circle>
            {[0,1,2,3,4,5].map(i => {
                const angle = (i*60)*(Math.PI/180), r=50;
                return (
                    <g key={i}>
                        <line x1="100" y1="80" x2={100+Math.cos(angle)*r} y2={80+Math.sin(angle)*r}
                            stroke="currentColor" strokeWidth="1" opacity="0.3">
                            <animate attributeName="opacity" values="0.3;0.9;0.3" dur="2s" begin={`${i*0.3}s`} repeatCount="indefinite" />
                        </line>
                        <circle cx={100+Math.cos(angle)*r} cy={80+Math.sin(angle)*r} r="6" fill="none" stroke="currentColor" strokeWidth="1.5">
                            <animate attributeName="r" values="6;9;6" dur="2s" begin={`${i*0.3}s`} repeatCount="indefinite" />
                        </circle>
                    </g>
                );
            })}
            <circle cx="100" cy="80" r="30" fill="none" stroke="currentColor" strokeWidth="1" opacity="0">
                <animate attributeName="r" values="20;65" dur="2s" repeatCount="indefinite" />
                <animate attributeName="opacity" values="0.5;0" dur="2s" repeatCount="indefinite" />
            </circle>
        </svg>
    );
}

function RankingVisual() {
    return (
        <svg viewBox="0 0 200 160" style={{ width: '100%', height: '100%', color: C.primary }}>
            {[{x:65,h:80,d:'0.2s'},{x:95,h:110,d:'0s'},{x:125,h:55,d:'0.4s'}].map((b,i) => (
                <g key={i}>
                    <rect x={b.x} y={140-b.h} width="26" height={b.h} rx="2" fill="currentColor" opacity="0.15">
                        <animate attributeName="height" values={`${b.h*0.3};${b.h};${b.h*0.3}`} dur="3s" begin={b.d} repeatCount="indefinite" />
                        <animate attributeName="y" values={`${140-b.h*0.3};${140-b.h};${140-b.h*0.3}`} dur="3s" begin={b.d} repeatCount="indefinite" />
                    </rect>
                    <circle cx={b.x+13} cy={140-b.h-14} r="10" fill="none" stroke="currentColor" strokeWidth="1.5" opacity="0.5" />
                </g>
            ))}
            <line x1="40" y1="140" x2="160" y2="140" stroke="currentColor" strokeWidth="1" opacity="0.3" />
        </svg>
    );
}

function MultiLangVisual() {
    const langs = ['Java','C++','Python','JS','C'];
    return (
        <svg viewBox="0 0 200 160" style={{ width: '100%', height: '100%', color: C.secondary }}>
            {langs.map((l,i) => {
                const angle = (i*72-90)*(Math.PI/180), r=55;
                return (
                    <g key={l}>
                        <rect x={100+Math.cos(angle)*r-18} y={80+Math.sin(angle)*r-10} width="36" height="20" rx="3"
                            fill="none" stroke="currentColor" strokeWidth="1.2" opacity="0.5">
                            <animate attributeName="opacity" values="0.5;1;0.5" dur="2.5s" begin={`${i*0.4}s`} repeatCount="indefinite" />
                        </rect>
                        <text x={100+Math.cos(angle)*r} y={80+Math.sin(angle)*r+4}
                            textAnchor="middle" fontSize="8" fontFamily="monospace" fill="currentColor" opacity="0.8">{l}</text>
                        <line x1="100" y1="80" x2={100+Math.cos(angle)*r*0.6} y2={80+Math.sin(angle)*r*0.6}
                            stroke="currentColor" strokeWidth="0.8" opacity="0.2">
                            <animate attributeName="opacity" values="0.2;0.6;0.2" dur="2.5s" begin={`${i*0.4}s`} repeatCount="indefinite" />
                        </line>
                    </g>
                );
            })}
            <circle cx="100" cy="80" r="12" fill="currentColor" opacity="0.15">
                <animate attributeName="r" values="10;14;10" dur="2s" repeatCount="indefinite" />
            </circle>
        </svg>
    );
}

// ─── Animated Counter ───────────────────────────────────────────────────────
function Counter({ end, suffix='', prefix='' }) {
    const [count, setCount] = useState(0);
    const ref = useRef(null);
    const animated = useRef(false);
    useEffect(() => {
        const obs = new IntersectionObserver(([e]) => {
            if (e.isIntersecting && !animated.current) {
                animated.current = true;
                const start = performance.now();
                const dur = 2000;
                const tick = (now) => {
                    const p = Math.min((now-start)/dur, 1);
                    const eased = 1 - Math.pow(1-p, 3);
                    setCount(Math.floor(eased * end));
                    if (p < 1) requestAnimationFrame(tick);
                };
                requestAnimationFrame(tick);
            }
        }, { threshold: 0.5 });
        if (ref.current) obs.observe(ref.current);
        return () => obs.disconnect();
    }, [end]);
    return <div ref={ref}>{prefix}{count.toLocaleString()}{suffix}</div>;
}

// ─── Hook: IntersectionObserver ────────────────────────────────────────────
function useVisible(threshold=0.15) {
    const [vis, setVis] = useState(false);
    const ref = useRef(null);
    useEffect(() => {
        const obs = new IntersectionObserver(([e]) => { if (e.isIntersecting) setVis(true); }, { threshold });
        if (ref.current) obs.observe(ref.current);
        return () => obs.disconnect();
    }, [threshold]);
    return [ref, vis];
}

// ─── Main Component ─────────────────────────────────────────────────────────
const HERO_WORDS = ['compete', 'solve', 'win', 'code'];

const Home = () => {
    const { isMobile, isTablet } = useResponsive();
    const [currentUser, setCurrentUser] = useState(null);
    const [isAdmin, setIsAdmin]         = useState(false);
    const [latestContests, setLatestContests] = useState([]);
    const [wordIdx, setWordIdx]         = useState(0);
    const [heroVis, setHeroVis]         = useState(false);
    const [liveTime, setLiveTime]       = useState(new Date());
    const [activeStep, setActiveStep]   = useState(0);
    const [activeQuote, setActiveQuote] = useState(0);
    const [quoteAnim, setQuoteAnim]     = useState(false);
    const [spotlight, setSpotlight]     = useState({ x: 50, y: 50 });

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) { setCurrentUser(user); setIsAdmin(user.roles?.includes('ROLE_ADMIN')); }
        ContestService.getAllContests()
            .then(res => {
                const all = Array.isArray(res.data) ? res.data : [];
                setLatestContests([...all].sort((a,b) => b.id - a.id).slice(0,2));
            }).catch(() => {});
        setTimeout(() => setHeroVis(true), 50);
    }, []);

    useEffect(() => {
        const t = setInterval(() => setWordIdx(p => (p+1) % HERO_WORDS.length), 2500);
        return () => clearInterval(t);
    }, []);

    useEffect(() => {
        const t = setInterval(() => setLiveTime(new Date()), 1000);
        return () => clearInterval(t);
    }, []);

    useEffect(() => {
        const t = setInterval(() => setActiveStep(p => (p+1) % 3), 5000);
        return () => clearInterval(t);
    }, []);

    useEffect(() => {
        const t = setInterval(() => {
            setQuoteAnim(true);
            setTimeout(() => { setActiveQuote(p => (p+1) % TESTIMONIALS.length); setQuoteAnim(false); }, 300);
        }, 5000);
        return () => clearInterval(t);
    }, []);

    const links = {
        cta:      !currentUser ? '/register' : isAdmin ? '/admin/contests' : '/contests',
        contests: !currentUser ? '/login'    : isAdmin ? '/admin/contests' : '/contests',
        practice: !currentUser ? '/login'    : '/practice',
        signin:   '/login',
    };

    const px = isMobile ? '20px' : '64px';

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh', overflowX: 'hidden' }}>

            {/* ── HERO ───────────────────────────────────────────────────── */}
            <section style={{ position: 'relative', minHeight: '95vh', display: 'flex', flexDirection: 'column', justifyContent: 'center', overflow: 'hidden', backgroundColor: C.bg }}>
                {/* Animated sphere — right side, like Optimus */}
                {!isMobile && (
                    <div style={{ position: 'absolute', right: '-60px', top: '50%', transform: 'translateY(-50%)', width: '860px', height: '860px', opacity: 0.5, pointerEvents: 'none', zIndex: 1 }}>
                        <AnimatedSphere />
                    </div>
                )}

                {/* Grid lines overlay — Optimus style */}
                <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none', opacity: 0.12, zIndex: 2 }}>
                    {[...Array(7)].map((_,i) => (
                        <div key={`h${i}`} style={{ position: 'absolute', height: '1px', background: C.borderSolid, top: `${14*(i+1)}%`, left: 0, right: 0 }} />
                    ))}
                    {[...Array(10)].map((_,i) => (
                        <div key={`v${i}`} style={{ position: 'absolute', width: '1px', background: C.borderSolid, left: `${10*(i+1)}%`, top: 0, bottom: 0 }} />
                    ))}
                </div>

                <div style={{ position: 'relative', zIndex: 10, padding: `0 ${px}`, maxWidth: '1400px', width: '100%' }}>
                    {/* Eyebrow */}
                    <div style={{ marginBottom: '2rem', opacity: heroVis ? 1 : 0, transform: heroVis ? 'none' : 'translateY(16px)', transition: 'all 0.7s ease' }}>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.25em', color: C.outline, textTransform: 'uppercase', display: 'inline-flex', alignItems: 'center', gap: '12px' }}>
                            <span style={{ width: '32px', height: '1px', background: C.outline }} />
                            The arena for elite engineers
                        </span>
                    </div>

                    {/* Main headline — Optimus giant typography */}
                    <div style={{ marginBottom: '3rem' }}>
                        <h1 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: isMobile ? '3.5rem' : isTablet ? '6rem' : '9rem',
                            fontWeight: 700, lineHeight: 0.92, letterSpacing: '-0.02em',
                            color: C.onBg,
                            opacity: heroVis ? 1 : 0, transform: heroVis ? 'none' : 'translateY(32px)',
                            transition: 'all 1s ease',
                        }}>
                            <span style={{ display: 'block' }}>The arena to</span>
                            <span style={{ display: 'block' }}>
                                <span style={{ position: 'relative', display: 'inline-block', color: C.primary, fontStyle: 'italic', fontWeight: 300 }}>
                                    {HERO_WORDS[wordIdx].split('').map((ch, i) => (
                                        <span key={`${wordIdx}-${i}`} style={{
                                            display: 'inline-block',
                                            animation: 'charIn 0.5s cubic-bezier(0.22,1,0.36,1) forwards',
                                            animationDelay: `${i*55}ms`,
                                            opacity: 0,
                                        }}>{ch}</span>
                                    ))}
                                </span>
                                <span style={{ color: C.onBg }}>.</span>
                            </span>
                        </h1>
                    </div>

                    {/* Sub + CTAs — Optimus 2-col grid */}
                    <div style={{
                        display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr',
                        gap: isMobile ? '2rem' : '6rem', alignItems: 'flex-end',
                        opacity: heroVis ? 1 : 0, transform: heroVis ? 'none' : 'translateY(16px)',
                        transition: 'all 0.8s ease 0.2s',
                    }}>
                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: isMobile ? '16px' : '20px', lineHeight: 1.6, color: C.muted, borderLeft: `1px solid ${C.borderSolid}`, paddingLeft: '1.5rem', fontWeight: 300 }}>
                            Enter the cinematic command center for elite engineers. Where algorithms are forged in deep focus, and code is executed with architectural precision.
                        </p>
                        <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
                            <HeroBtn to={links.cta} primary>Start Battle →</HeroBtn>
                            <HeroBtn to={links.contests}>Explore Contests</HeroBtn>
                        </div>
                    </div>
                </div>

                {/* Stats marquee — bottom, full width — Optimus style */}
                <div style={{
                    position: 'absolute', bottom: '0', left: 0, right: 0, zIndex: 10,
                    opacity: heroVis ? 1 : 0, transition: 'opacity 0.8s ease 0.5s',
                    overflow: 'hidden', width: '100vw',
                }}>
                    <div style={{ display: 'flex', animation: 'marquee 35s linear infinite', whiteSpace: 'nowrap', width: 'max-content' }}>
                        {[...Array(4)].map((_,si) => (
                            <div key={si} style={{ display: 'flex', flexShrink: 0 }}>
                                {[
                                    { value: '50k+',  label: 'battles fought',     tag: 'CODECOMBAT' },
                                    { value: '1M+',   label: 'submissions judged', tag: 'PLATFORM'   },
                                    { value: '0.4ms', label: 'avg execution',       tag: 'JUDGE'      },
                                    { value: '300+',  label: 'elite engineers',     tag: 'COMMUNITY'  },
                                ].map(s => (
                                    <div key={`${si}-${s.tag}`} style={{ display: 'flex', alignItems: 'baseline', gap: '1rem', marginRight: '4rem' }}>
                                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '2rem' : '3rem', fontWeight: 300, color: C.onBg }}>{s.value}</span>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                            {s.label}
                                            <span style={{ display: 'block', letterSpacing: '0.15em', marginTop: '2px' }}>{s.tag}</span>
                                        </span>
                                    </div>
                                ))}
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            {/* ── FEATURES (numbered 01–04) — Optimus list style ─────────── */}
            <FeaturesSection isMobile={isMobile} />

            {/* ── HOW IT WORKS (dark inverted) — Optimus 3-step ──────────── */}
            <HowItWorksSection isMobile={isMobile} activeStep={activeStep} setActiveStep={setActiveStep} C={C} />

            {/* ── METRICS (animated counters) ─────────────────────────────── */}
            <MetricsSection isMobile={isMobile} liveTime={liveTime} C={C} />

            {/* ── CONTEST STRIP (latest 2 from DB) ────────────────────────── */}
            <ContestStrip isMobile={isMobile} latestContests={latestContests} links={links} C={C} />

            {/* ── TESTIMONIALS — Optimus large quote carousel ─────────────── */}
            <TestimonialsSection activeQuote={activeQuote} setActiveQuote={setActiveQuote} quoteAnim={quoteAnim} setQuoteAnim={setQuoteAnim} isMobile={isMobile} C={C} px={px} />

            {/* ── CTA — Optimus bordered box + mouse spotlight ─────────────── */}
            <CtaSection links={links} isMobile={isMobile} spotlight={spotlight} setSpotlight={setSpotlight} C={C} px={px} />

            <style>{`
                @keyframes charIn {
                    0%   { opacity:0; filter:blur(30px); transform:translateY(60%); }
                    100% { opacity:1; filter:blur(0);    transform:translateY(0); }
                }
                @keyframes marquee {
                    0%   { transform: translateX(0); }
                    100% { transform: translateX(-25%); }
                }
                @keyframes progress {
                    from { width: 0%; }
                    to   { width: 100%; }
                }
                @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
            `}</style>
        </div>
    );
};

// ─── Hero Button ────────────────────────────────────────────────────────────
const HeroBtn = ({ children, to, primary }) => {
    const [hov, setHov] = useState(false);
    return (
        <Link to={to}
            onMouseEnter={() => setHov(true)} onMouseLeave={() => setHov(false)}
            style={{
                fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.12em',
                textTransform: 'uppercase', textDecoration: 'none',
                padding: '18px 36px', display: 'inline-flex', alignItems: 'center',
                transition: 'all 0.3s',
                ...(primary ? {
                    border: '1px solid rgba(241,188,139,0.5)',
                    backgroundColor: hov ? '#f1bc8b' : 'rgba(19,19,19,0.5)',
                    color: hov ? '#131313' : '#f1bc8b',
                    backdropFilter: 'blur(4px)',
                } : {
                    border: 'none', backgroundColor: 'transparent',
                    color: hov ? '#e9c176' : '#d4c4b7',
                }),
            }}
        >{children}</Link>
    );
};

// ─── Features Section ────────────────────────────────────────────────────────
const FEATURES = [
    { n: '01', title: 'Real-time Verdicts',       desc: 'Experience execution speeds that rival thought. Our judge infrastructure returns execution results, memory profiles, and structural analysis in milliseconds.', visual: 'judge'  },
    { n: '02', title: 'Monaco Intelligence',       desc: 'A pristine, distraction-free editing canvas powered by the same engine behind VS Code. Deep syntax highlighting and absolute structural control.', visual: 'editor' },
    { n: '03', title: 'Elite Rankings',            desc: 'Climb the hierarchy of architectural mastery. Our Elo-based system strictly evaluates performance, elegance, and logic under pressure.', visual: 'rank'   },
    { n: '04', title: 'Multi-Language Support',    desc: 'Compete in Java, C++, Python, JavaScript, and C. Full harness support for all 5 languages with identical test cases and judge behaviour.', visual: 'multi'  },
];

function FeatCard({ f, index, C }) {
    const [ref, vis] = useVisible(0.2);
    return (
        <div ref={ref} style={{
            opacity: vis ? 1 : 0, transform: vis ? 'none' : 'translateY(48px)',
            transition: `all 0.7s ease ${index*100}ms`,
        }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem', padding: '3rem 0', borderBottom: `1px solid ${C.border}` }}>
                <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, minWidth: '28px', paddingTop: '6px' }}>{f.n}</span>
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '2rem', alignItems: 'center' }}>
                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(24px,4vw,40px)', fontWeight: 300, color: C.onBg, margin: 0 }}>{f.title}</h3>
                            <div style={{ width: '160px', height: '120px', flexShrink: 0 }}>
                                {f.visual === 'judge'  && <JudgeVisual />}
                                {f.visual === 'editor' && <EditorVisual />}
                                {f.visual === 'rank'   && <RankingVisual />}
                                {f.visual === 'multi'  && <MultiLangVisual />}
                            </div>
                        </div>
                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '17px', color: C.muted, lineHeight: 1.7, fontWeight: 300, maxWidth: '560px' }}>{f.desc}</p>
                    </div>
                </div>
            </div>
        </div>
    );
}

function FeaturesSection({ isMobile }) {
    const [ref, vis] = useVisible(0.1);
    return (
        <section style={{ padding: `6rem ${isMobile ? '20px' : '64px'}` }}>
            <div style={{ maxWidth: '1400px', margin: '0 auto' }}>
                <div ref={ref} style={{ marginBottom: '4rem', opacity: vis ? 1 : 0, transform: vis ? 'none' : 'translateY(16px)', transition: 'all 0.7s ease' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'inline-flex', alignItems: 'center', gap: '12px' }}>
                        <span style={{ width: '32px', height: '1px', background: C.outline }} />Capabilities
                    </span>
                    <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(32px,5vw,56px)', fontWeight: 300, color: C.onBg, marginTop: '1.5rem', lineHeight: 1.1 }}>
                        Everything you need.<br /><span style={{ color: C.outline }}>Nothing you don't.</span>
                    </h2>
                </div>
                {FEATURES.map((f,i) => <FeatCard key={f.n} f={f} index={i} C={C} />)}
            </div>
        </section>
    );
}

// ─── How It Works ────────────────────────────────────────────────────────────
const STEPS = [
    { n: 'I',   title: 'Register & Join',    desc: 'Create your account and enter any contest or practice problem. Zero setup — start competing in under 60 seconds.', code: `// CodeCombat Entry\ncontest.join({\n  user: "you",\n  level: "HARD",\n  ready: true\n})` },
    { n: 'II',  title: 'Write Your Solution', desc: 'Use Monaco editor with full syntax highlighting. Choose from Java, C++, Python, JavaScript, or C. Submit when ready.', code: `// Two Sum — O(n)\npublic static int[] solve(\n  int[] nums, int target\n) {\n  // your solution here\n}` },
    { n: 'III', title: 'Instant Verdict',     desc: 'Get results in milliseconds. See which test cases passed, where you failed, and your rank on the live leaderboard.', code: `TC:1:PASS\nTC:2:PASS\nTC:3:PASS:hidden\nTC:4:FAIL:input=[3,3]\n  expected=[0,1]:got=[]` },
];

function HowItWorksSection({ isMobile, activeStep, setActiveStep, C }) {
    const [ref, vis] = useVisible(0.1);
    return (
        <section ref={ref} style={{ position: 'relative', padding: `6rem ${isMobile ? '20px' : '64px'}`, backgroundColor: '#1a1919', color: C.onBg, overflow: 'hidden' }}>
            {/* Diagonal pattern */}
            <div style={{ position: 'absolute', inset: 0, opacity: 0.04, pointerEvents: 'none', backgroundImage: `repeating-linear-gradient(-45deg, transparent, transparent 40px, ${C.borderSolid} 40px, ${C.borderSolid} 41px)` }} />

            <div style={{ position: 'relative', zIndex: 1, maxWidth: '1400px', margin: '0 auto' }}>
                <div style={{ marginBottom: '4rem' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'inline-flex', alignItems: 'center', gap: '12px' }}>
                        <span style={{ width: '32px', height: '1px', background: C.outline }} />Process
                    </span>
                    <h2 style={{
                        fontFamily: "'Playfair Display', serif", fontSize: 'clamp(32px,5vw,56px)', fontWeight: 300, color: C.onBg, marginTop: '1.5rem', lineHeight: 1.1,
                        opacity: vis ? 1 : 0, transform: vis ? 'none' : 'translateY(16px)', transition: 'all 0.7s ease',
                    }}>
                        Three steps.<br /><span style={{ color: C.outline }}>Infinite possibilities.</span>
                    </h2>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: '4rem' }}>
                    {/* Steps */}
                    <div>
                        {STEPS.map((s,i) => (
                            <button key={s.n} onClick={() => setActiveStep(i)} style={{
                                width: '100%', textAlign: 'left', padding: '2rem 0',
                                background: 'none', border: 'none', borderBottom: `1px solid ${C.border}`,
                                cursor: 'pointer', opacity: activeStep === i ? 1 : 0.35, transition: 'opacity 0.4s',
                            }}>
                                <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'flex-start' }}>
                                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', color: C.border, minWidth: '32px' }}>{s.n}</span>
                                    <div>
                                        <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 400, color: C.onBg, marginBottom: '0.5rem' }}>{s.title}</h3>
                                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.muted, lineHeight: 1.6, fontWeight: 300 }}>{s.desc}</p>
                                        {activeStep === i && (
                                            <div style={{ marginTop: '1rem', height: '1px', background: C.border, overflow: 'hidden' }}>
                                                <div style={{ height: '100%', background: C.primary, animation: 'progress 5s linear forwards' }} />
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </button>
                        ))}
                    </div>

                    {/* Code display */}
                    {!isMobile && (
                        <div style={{ alignSelf: 'flex-start', position: 'sticky', top: '2rem' }}>
                            <div style={{ border: `1px solid ${C.border}`, overflow: 'hidden' }}>
                                <div style={{ padding: '1rem 1.5rem', borderBottom: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center', backgroundColor: C.surfaceCon }}>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        {[0,1,2].map(d => <div key={d} style={{ width: '10px', height: '10px', borderRadius: '50%', background: C.border }} />)}
                                    </div>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>solution.java</span>
                                </div>
                                <div style={{ padding: '2rem', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', minHeight: '240px', backgroundColor: C.surfaceLow }}>
                                    <pre style={{ margin: 0, color: C.muted, whiteSpace: 'pre-wrap' }}>
                                        {STEPS[activeStep].code.split('\n').map((line, li) => (
                                            <div key={`${activeStep}-${li}`} style={{ lineHeight: '1.8', animation: 'fadeSlide 0.4s ease forwards', opacity: 0, animationDelay: `${li*80}ms` }}>
                                                <span style={{ color: C.border, userSelect: 'none', marginRight: '1rem', display: 'inline-block', width: '1.5rem', textAlign: 'right' }}>{li+1}</span>
                                                {line}
                                            </div>
                                        ))}
                                    </pre>
                                </div>
                                <div style={{ padding: '0.75rem 1.5rem', borderTop: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', gap: '8px', backgroundColor: C.surfaceCon }}>
                                    <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#4ade80', animation: 'pulse 1.5s infinite' }} />
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>Judge ready</span>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
            <style>{`@keyframes fadeSlide { from{opacity:0;transform:translateX(-8px)} to{opacity:1;transform:none} }`}</style>
        </section>
    );
}

// ─── Metrics Section ─────────────────────────────────────────────────────────
const METRICS = [
    { end: 1283974, suffix: '',    label: 'Submissions judged'   },
    { end: 99,      suffix: '.9%', label: 'Judge uptime'          },
    { end: 12,      suffix: 'ms',  label: 'Avg execution time'    },
    { end: 47,      suffix: '',    label: 'Active problems'        },
];

function MetricsSection({ isMobile, liveTime, C }) {
    const [ref, vis] = useVisible(0.1);
    return (
        <section ref={ref} style={{ padding: `6rem ${isMobile ? '20px' : '64px'}`, borderTop: `1px solid ${C.border}`, borderBottom: `1px solid ${C.border}` }}>
            <div style={{ maxWidth: '1400px', margin: '0 auto' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '2rem', marginBottom: '4rem' }}>
                    <div>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', display: 'inline-flex', alignItems: 'center', gap: '12px' }}>
                            <span style={{ width: '32px', height: '1px', background: C.outline }} />Live metrics
                        </span>
                        <h2 style={{
                            fontFamily: "'Playfair Display', serif", fontSize: 'clamp(28px,4vw,48px)', fontWeight: 300, color: C.onBg, marginTop: '1rem', lineHeight: 1.1,
                            opacity: vis ? 1 : 0, transform: vis ? 'none' : 'translateY(16px)', transition: 'all 0.7s ease',
                        }}>
                            Performance you<br />can measure.
                        </h2>
                    </div>
                    <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: C.success, animation: 'pulse 1.5s infinite' }} />
                        Live · {liveTime.toLocaleTimeString()}
                    </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr 1fr' : 'repeat(4, 1fr)', gap: '1px', background: C.border }}>
                    {METRICS.map((m,i) => (
                        <div key={m.label} style={{
                            background: C.bg, padding: isMobile ? '2rem 1.5rem' : '3rem',
                            opacity: vis ? 1 : 0, transform: vis ? 'none' : 'translateY(32px)', transition: `all 0.7s ease ${i*100}ms`,
                        }}>
                            <div style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(36px,5vw,64px)', fontWeight: 300, color: C.primary, lineHeight: 1 }}>
                                <Counter end={m.end} suffix={m.suffix} />
                            </div>
                            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, marginTop: '1rem', letterSpacing: '0.08em', textTransform: 'uppercase' }}>{m.label}</div>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
}

// ─── Contest Strip ───────────────────────────────────────────────────────────
function ContestStrip({ isMobile, latestContests, links, C }) {
    const placeholders = [{}, {}];
    const items = latestContests.length > 0 ? latestContests : placeholders;
    return (
        <section style={{ borderTop: `1px solid ${C.border}`, borderBottom: `1px solid ${C.border}`, display: 'flex', flexDirection: isMobile ? 'column' : 'row' }}>
            {items.map((c, i) => <ContestCard key={c.id ?? i} c={c} i={i} to={links.contests} C={C} />)}
            <Link to={links.contests} style={{
                flex: '0 0 auto', width: isMobile ? '100%' : '33%',
                padding: '4rem', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center',
                borderLeft: isMobile ? 'none' : `1px solid ${C.border}`, borderTop: isMobile ? `1px solid ${C.border}` : 'none',
                textDecoration: 'none', transition: 'background-color 0.4s',
            }}
                onMouseEnter={e => e.currentTarget.style.backgroundColor = C.surfaceLow}
                onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
            >
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.muted, textTransform: 'uppercase', marginBottom: '1.5rem', textAlign: 'center' }}>View All Schedules</span>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '24px', color: 'rgba(157,142,131,0.4)', letterSpacing: '0.05em' }}>[ → ]</span>
            </Link>
        </section>
    );
}

function ContestCard({ c, i, to, C }) {
    const [hov, setHov] = useState(false);
    const [time, setTime] = useState('');
    const divColor = i === 0 ? C.primary : C.secondary;
    useEffect(() => {
        if (!c.startTime && !c.endTime) return;
        const tick = () => {
            const now = Date.now(), s = new Date(c.startTime).getTime(), e = new Date(c.endTime).getTime();
            let diff, pre;
            if (now < s) { diff = s - now; pre = 'T-Minus '; }
            else if (now < e) { diff = e - now; pre = 'Ends in '; }
            else { setTime('Ended'); return; }
            const h = Math.floor(diff/3600000), m = Math.floor(diff%3600000/60000), sec = Math.floor(diff%60000/1000);
            setTime(`${pre}${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(sec).padStart(2,'0')}`);
        };
        tick(); const t = setInterval(tick, 1000); return () => clearInterval(t);
    }, [c.startTime, c.endTime]);

    return (
        <Link to={to} onMouseEnter={() => setHov(true)} onMouseLeave={() => setHov(false)} style={{
            flex: 1, padding: '4rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
            borderRight: `1px solid ${C.border}`, textDecoration: 'none',
            backgroundColor: hov ? C.surfaceLow : 'transparent', transition: 'background-color 0.4s',
        }}>
            <div>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: divColor, display: 'block', marginBottom: '1rem', textTransform: 'uppercase' }}>
                    {i === 0 ? 'Alpha Division' : 'Beta Division'}
                </span>
                <h4 style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 300, color: C.onBg, marginBottom: '0.75rem' }}>{c.name ?? '—'}</h4>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.muted, lineHeight: 1.6, fontWeight: 300 }}>{c.description ?? ''}</p>
            </div>
            <div style={{ marginTop: '2.5rem', display: 'flex', justifyContent: 'space-between', borderTop: `1px solid ${C.border}`, paddingTop: '1.25rem', opacity: hov ? 1 : 0.5, transition: 'opacity 0.3s' }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>{time}</span>
                <span style={{ color: divColor, fontFamily: "'JetBrains Mono', monospace", transform: hov ? 'translateX(8px)' : 'none', transition: 'transform 0.3s' }}>→</span>
            </div>
        </Link>
    );
}

// ─── Testimonials ────────────────────────────────────────────────────────────
const TESTIMONIALS = [
    { quote: 'CodeCombat transformed how our team thinks about algorithmic challenges. The judge is blazing fast.', author: 'Arjun Sharma', role: 'Senior SWE', company: 'Groww', metric: '3x faster problem solving' },
    { quote: 'The Monaco editor experience is identical to my daily IDE. I can focus entirely on the algorithm.', author: 'Priya Mehta', role: 'Competitive Programmer', company: 'ICPC India', metric: 'Top 100 globally' },
    { quote: 'Real-time verdicts with detailed fail messages. This is how a judge should work.', author: 'Rohit Das', role: 'Backend Engineer', company: 'Razorpay', metric: '99.9% submission accuracy' },
    { quote: 'Five language support with identical harnesses. Finally a platform that treats all languages equally.', author: 'Sneha Patel', role: 'Fullstack Dev', company: 'Cred', metric: '200+ problems solved' },
];

function TestimonialsSection({ activeQuote, setActiveQuote, quoteAnim, setQuoteAnim, isMobile, C, px }) {
    const q = TESTIMONIALS[activeQuote];
    const setPrev = (idx) => { setQuoteAnim(true); setTimeout(() => { setActiveQuote(idx); setQuoteAnim(false); }, 300); };
    return (
        <section style={{ padding: `6rem ${isMobile ? '20px' : '64px'}`, borderTop: `1px solid ${C.border}` }}>
            <div style={{ maxWidth: '1400px', margin: '0 auto' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', marginBottom: '3rem' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>What competitors say</span>
                    <div style={{ flex: 1, height: '1px', background: C.border }} />
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                        {String(activeQuote+1).padStart(2,'0')} / {String(TESTIMONIALS.length).padStart(2,'0')}
                    </span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '8fr 4fr', gap: '4rem' }}>
                    <blockquote style={{ opacity: quoteAnim ? 0 : 1, transform: quoteAnim ? 'translateY(16px)' : 'none', transition: 'all 0.3s ease' }}>
                        <p style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(24px,4vw,48px)', fontWeight: 300, lineHeight: 1.15, color: C.onBg, marginBottom: '2.5rem' }}>"{q.quote}"</p>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                            <div style={{ width: '48px', height: '48px', borderRadius: '50%', border: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                                <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', color: C.primary }}>{q.author[0]}</span>
                            </div>
                            <div>
                                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', fontWeight: 500, color: C.onBg }}>{q.author}</p>
                                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, letterSpacing: '0.05em' }}>{q.role} · {q.company}</p>
                            </div>
                        </div>
                    </blockquote>

                    <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                        <div style={{ border: `1px solid ${C.border}`, padding: '2rem', opacity: quoteAnim ? 0 : 1, transition: 'all 0.3s ease', marginBottom: '2rem' }}>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '1rem' }}>Key Result</span>
                            <p style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 300, color: C.primary }}>{q.metric}</p>
                        </div>
                        <div style={{ display: 'flex', gap: '8px' }}>
                            {TESTIMONIALS.map((_,idx) => (
                                <button key={idx} onClick={() => setPrev(idx)} style={{
                                    height: '4px', border: 'none', cursor: 'pointer',
                                    width: idx === activeQuote ? '32px' : '8px',
                                    background: idx === activeQuote ? C.primary : `rgba(241,188,139,0.2)`,
                                    transition: 'all 0.3s',
                                }} />
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            {/* Company marquee */}
            {/* <div style={{ marginTop: '4rem', paddingTop: '3rem', borderTop: `1px solid ${C.border}` }}>
                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', textAlign: 'center', marginBottom: '2rem' }}>Trusted by engineers from</p>
                <div style={{ overflow: 'hidden' }}>
                    <div style={{ display: 'flex', gap: '4rem', animation: 'marquee 25s linear infinite', whiteSpace: 'nowrap' }}>
                        {[...Array(2)].map((_,si) => (
                            <div key={si} style={{ display: 'flex', gap: '4rem', flexShrink: 0 }}>
                                {['Google','Microsoft','Flipkart','Razorpay','Cred','Groww','Zepto','Meesho'].map(co => (
                                    <span key={`${si}-${co}`} style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', color: `rgba(229,226,225,0.2)`, letterSpacing: '0.05em', transition: 'color 0.3s' }}
                                        onMouseEnter={e => e.currentTarget.style.color = `rgba(229,226,225,0.7)`}
                                        onMouseLeave={e => e.currentTarget.style.color = `rgba(229,226,225,0.2)`}
                                    >{co}</span>
                                ))}
                            </div>
                        ))}
                    </div>
                </div>
            </div> */}
        </section>
    );
}

// ─── CTA Section ─────────────────────────────────────────────────────────────
function CtaSection({ links, isMobile, spotlight, setSpotlight, C, px }) {
    const [ref, vis] = useVisible(0.2);
    const handleMouse = (e) => {
        const r = e.currentTarget.getBoundingClientRect();
        setSpotlight({ x: ((e.clientX - r.left) / r.width) * 100, y: ((e.clientY - r.top) / r.height) * 100 });
    };
    return (
        <section style={{ padding: `6rem ${isMobile ? '20px' : '64px'}` }}>
            <div style={{ maxWidth: '1400px', margin: '0 auto' }}>
                <div ref={ref} onMouseMove={handleMouse} style={{
                    position: 'relative', border: `1px solid ${C.borderSolid}`, overflow: 'hidden',
                    opacity: vis ? 1 : 0, transform: vis ? 'none' : 'translateY(32px)', transition: 'all 1s ease',
                }}>
                    {/* Spotlight */}
                    <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none', opacity: 0.08, background: `radial-gradient(500px circle at ${spotlight.x}% ${spotlight.y}%, ${C.primary}, transparent 40%)`, transition: 'background 0.1s' }} />
                    {/* Corner decorations */}
                    <div style={{ position: 'absolute', top: 0, right: 0, width: '80px', height: '80px', borderBottom: `1px solid ${C.border}`, borderLeft: `1px solid ${C.border}` }} />
                    <div style={{ position: 'absolute', bottom: 0, left: 0, width: '80px', height: '80px', borderTop: `1px solid ${C.border}`, borderRight: `1px solid ${C.border}` }} />

                    <div style={{ position: 'relative', zIndex: 1, padding: isMobile ? '4rem 2rem' : '6rem', display: 'flex', flexDirection: isMobile ? 'column' : 'row', alignItems: 'center', justifyContent: 'space-between', gap: '3rem' }}>
                        <div style={{ flex: 1 }}>
                            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(36px,6vw,72px)', fontWeight: 300, color: C.onBg, lineHeight: 0.95, marginBottom: '1.5rem' }}>
                                Ready to enter<br /><span style={{ color: C.primary, fontStyle: 'italic' }}>the arena?</span>
                            </h2>
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '18px', color: C.muted, marginBottom: '2.5rem', lineHeight: 1.6, fontWeight: 300, maxWidth: '500px' }}>
                                Join hundreds of engineers competing, learning, and mastering algorithms. Start free, compete forever.
                            </p>
                            <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap' }}>
                                <HeroBtn to={links.cta} primary>Start Competing →</HeroBtn>
                                <HeroBtn to={links.practice}>Try Practice Mode</HeroBtn>
                            </div>
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, marginTop: '1.5rem', letterSpacing: '0.08em' }}>No credit card required</p>
                        </div>

                        {!isMobile && (
                            <div style={{ flexShrink: 0, width: '460px', height: '460px', opacity: 0.7, marginRight: '-2rem' }}>
                                <AnimatedSphere />
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </section>
    );
}

export default Home;
