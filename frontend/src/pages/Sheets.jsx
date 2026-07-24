import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
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
    error:      '#ffb4ab',
};

const SHEET = {
    id: 'deloitte',
    name: 'Deloitte 100',
    company: 'Deloitte',
    logo: 'D',
    problems: 100,
    description: 'Complete Deloitte coding interview preparation. 100 handpicked problems covering arrays, strings, linked lists, trees, dynamic programming, graphs, and more — curated from real interview experiences.',
    tags: ['Deloitte', 'Interview Prep', 'FAANG-style'],
};

const PROBLEMS = [
    { no: 1, name: 'Contains Duplicate', difficulty: 'Easy', topic: 'Arrays' },
    { no: 2, name: 'Maximum Subarray', difficulty: 'Medium', topic: 'Arrays' },
    { no: 3, name: 'Product of Array Except Self', difficulty: 'Medium', topic: 'Arrays' },
    { no: 4, name: 'Merge Sorted Array', difficulty: 'Easy', topic: 'Arrays' },
    { no: 5, name: 'Missing Number', difficulty: 'Easy', topic: 'Arrays' },
    { no: 6, name: 'Armstrong Number', difficulty: 'Easy', topic: 'Math' },
    { no: 7, name: 'Valid Anagram', difficulty: 'Easy', topic: 'Strings' },
    { no: 8, name: 'Valid Palindrome', difficulty: 'Easy', topic: 'Strings' },
    { no: 9, name: 'Reverse Words in a String', difficulty: 'Medium', topic: 'Strings' },
    { no: 10, name: 'Longest Substring Without Repeating Characters', difficulty: 'Medium', topic: 'Strings' },
];

export default function Sheets() {
    const { isMobile } = useResponsive();
    const [selected, setSelected] = useState(null);
    const [solved, setSolved] = useState({});
    const [spotlight, setSpotlight] = useState({ x: 50, y: 50 });
    const ctaRef = useRef(null);

    const toggleSolved = (name) => setSolved(p => ({ ...p, [name]: !p[name] }));
    const solvedCount = Object.values(solved).filter(Boolean).length;
    const progressPct = PROBLEMS.length > 0 ? Math.round((solvedCount / PROBLEMS.length) * 100) : 0;

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh' }}>
            {/* Background grid */}
            <div style={{ position: 'fixed', inset: 0, opacity: 0.03, pointerEvents: 'none', zIndex: 0,
                backgroundImage: `repeating-linear-gradient(-45deg, transparent, transparent 40px, ${C.borderSolid} 40px, ${C.borderSolid} 41px)` }} />

            <div style={{ position: 'relative', zIndex: 1, padding: isMobile ? '40px 24px' : '72px 64px' }}>

                {/* ── Hero ── */}
                <motion.header
                    initial={{ opacity: 0, y: 30 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
                    style={{ marginBottom: '56px' }}
                >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px' }}>
                        <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: C.secondary, boxShadow: `0 0 12px ${C.secondary}`, animation: 'pulse 2s infinite' }} />
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase' }}>
                            Curated Collections
                        </span>
                    </div>

                    <h1 style={{
                        fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '36px' : 'clamp(44px, 6vw, 64px)',
                        fontWeight: 700, lineHeight: 1.08, letterSpacing: '-0.02em',
                        color: C.primary, margin: '0 0 20px', maxWidth: '700px',
                    }}>
                        Interview practice,{' '}
                        <span style={{ color: C.secondary }}>organized</span>.
                    </h1>

                    <p style={{
                        fontSize: '17px', color: C.outline, maxWidth: '520px', lineHeight: 1.7,
                    }}>
                        Company-specific problem sheets with curated questions, progress tracking,
                        and difficulty labels. Pick a sheet, solve problems, and track your readiness.
                    </p>
                </motion.header>

                {!selected ? (
                    /* ── Sheet Card ── */
                    <motion.div
                        initial={{ opacity: 0, y: 24 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.2 }}
                    >
                        <div
                            onClick={() => setSelected(SHEET)}
                            ref={ctaRef}
                            onMouseMove={e => {
                                if (!ctaRef.current) return;
                                const r = ctaRef.current.getBoundingClientRect();
                                setSpotlight({ x: ((e.clientX - r.left) / r.width) * 100, y: ((e.clientY - r.top) / r.height) * 100 });
                            }}
                            style={{
                                border: `1px solid ${C.border}`,
                                backgroundColor: C.surfaceLow,
                                cursor: 'pointer', overflow: 'hidden', position: 'relative',
                                transition: 'border-color 0.3s, box-shadow 0.3s',
                            }}
                            onMouseEnter={e => {
                                e.currentTarget.style.borderColor = C.borderSolid;
                                e.currentTarget.style.boxShadow = `0 0 60px rgba(241,188,139,0.06)`;
                            }}
                            onMouseLeave={e => {
                                e.currentTarget.style.borderColor = C.border;
                                e.currentTarget.style.boxShadow = 'none';
                            }}
                        >
                            {/* Spotlight glow */}
                            <div style={{
                                position: 'absolute', inset: 0, pointerEvents: 'none', opacity: 0.06,
                                background: `radial-gradient(500px circle at ${spotlight.x}% ${spotlight.y}%, ${C.primary}, transparent 40%)`,
                                transition: 'background 0.1s',
                            }} />

                            <div style={{ position: 'relative', zIndex: 1, padding: isMobile ? '28px' : '40px 48px' }}>
                                <div style={{ display: 'flex', gap: isMobile ? '20px' : '32px', alignItems: 'flex-start' }}>
                                    {/* Logo */}
                                    <div style={{
                                        width: '72px', height: '72px', flexShrink: 0,
                                        border: `1px solid ${C.borderSolid}`, backgroundColor: `${C.primary}08`,
                                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        fontFamily: "'Playfair Display', serif", fontSize: '32px',
                                        fontWeight: 700, color: C.secondary,
                                    }}>
                                        {SHEET.logo}
                                    </div>

                                    <div style={{ flex: 1, minWidth: 0 }}>
                                        <div style={{ display: 'flex', alignItems: 'baseline', gap: '14px', marginBottom: '10px', flexWrap: 'wrap' }}>
                                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '24px' : '30px', fontWeight: 600, color: C.primary, margin: 0 }}>
                                                {SHEET.name}
                                            </h3>
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, letterSpacing: '0.06em' }}>
                                                {SHEET.company}
                                            </span>
                                        </div>

                                        <p style={{ fontSize: '15px', color: C.muted, lineHeight: 1.7, margin: '0 0 22px', maxWidth: '600px' }}>
                                            {SHEET.description}
                                        </p>

                                        {/* Stats + Tags */}
                                        <div style={{ display: 'flex', gap: '32px', flexWrap: 'wrap', alignItems: 'center' }}>
                                            <div>
                                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>Problems</span>
                                                <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 300, color: C.onBg }}>{SHEET.problems}</span>
                                            </div>
                                            <div>
                                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>Difficulty</span>
                                                <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 300, color: C.onBg }}>Mixed</span>
                                            </div>
                                            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                                                {SHEET.tags.map(tag => (
                                                    <span key={tag} style={{
                                                        padding: '4px 12px', border: `1px solid ${C.borderSolid}`,
                                                        fontFamily: "'JetBrains Mono', monospace", fontSize: '9px',
                                                        letterSpacing: '0.08em', color: C.secondary, textTransform: 'uppercase',
                                                    }}>
                                                        {tag}
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                    </div>

                                    {/* Arrow */}
                                    <div style={{ flexShrink: 0, display: isMobile ? 'none' : 'flex', alignItems: 'center' }}>
                                        <motion.div
                                            animate={{ x: [0, 6, 0] }}
                                            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                                        >
                                            <span className="material-symbols-outlined" style={{ fontSize: '28px', color: C.secondary }}>
                                                arrow_forward
                                            </span>
                                        </motion.div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Coming Soon */}
                        <div style={{ marginTop: '48px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px' }}>
                                <span style={{ width: '4px', height: '4px', borderRadius: '50%', backgroundColor: C.outline }} />
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>
                                    Coming Soon
                                </span>
                            </div>
                            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(4, 1fr)', gap: '1px', backgroundColor: C.border }}>
                                {['Google', 'Amazon', 'Microsoft', 'Meta'].map(company => (
                                    <div key={company} style={{
                                        padding: '40px 24px', backgroundColor: C.surfaceLow,
                                        opacity: 0.45, textAlign: 'center',
                                        transition: 'opacity 0.3s',
                                    }}
                                        onMouseEnter={e => e.currentTarget.style.opacity = '0.7'}
                                        onMouseLeave={e => e.currentTarget.style.opacity = '0.45'}
                                    >
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>
                                            {company}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </motion.div>
                ) : (
                    /* ── Detail View ── */
                    <AnimatePresence mode="wait">
                        <motion.div key="detail" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} transition={{ duration: 0.3 }}>
                            {/* Back + Progress */}
                            <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: '28px', flexWrap: 'wrap', gap: '16px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                                    <button onClick={() => setSelected(null)}
                                        style={{
                                            padding: '10px 20px', border: `1px solid ${C.border}`,
                                            backgroundColor: 'transparent', color: C.muted,
                                            fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
                                            letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer',
                                            display: 'flex', alignItems: 'center', gap: '6px', transition: 'all 0.2s',
                                        }}
                                        onMouseEnter={e => { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; }}
                                        onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}
                                    >
                                        <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>arrow_back</span> Back
                                    </button>
                                    <div>
                                        <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.primary, margin: '0 0 4px' }}>
                                            {SHEET.name}
                                        </h2>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                            {SHEET.company} · {PROBLEMS.length} problems
                                        </span>
                                    </div>
                                </div>

                                {/* Progress bar */}
                                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '6px' }}>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: C.secondary }}>
                                        {solvedCount} / {PROBLEMS.length} solved
                                    </span>
                                    <div style={{ width: '200px', height: '3px', backgroundColor: C.surfaceHi, borderRadius: '2px', overflow: 'hidden' }}>
                                        <motion.div
                                            initial={{ width: 0 }}
                                            animate={{ width: `${progressPct}%` }}
                                            transition={{ duration: 0.8, ease: 'easeOut' }}
                                            style={{ height: '100%', backgroundColor: C.success, borderRadius: '2px' }}
                                        />
                                    </div>
                                </div>
                            </div>

                            {/* Problem Table */}
                            <motion.div
                                initial={{ opacity: 0, y: 12 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ delay: 0.1 }}
                                style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}
                            >
                                <div style={{
                                    display: 'grid', gridTemplateColumns: isMobile ? '40px 1fr 80px' : '52px 1fr 130px 100px 80px',
                                    gap: '12px', padding: '14px 24px', borderBottom: `1px solid ${C.border}`,
                                    backgroundColor: C.surfaceCon,
                                }}>
                                    {['#', 'Problem', 'Topic', 'Difficulty', 'Solved'].filter((_, i) => !isMobile || i <= 1).map(h => (
                                        <span key={h} style={{
                                            fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                                            letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase',
                                        }}>
                                            {h}
                                        </span>
                                    ))}
                                </div>

                                {PROBLEMS.map((p, i) => (
                                    <motion.div
                                        key={p.no}
                                        initial={{ opacity: 0, x: -8 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        transition={{ delay: i * 0.03 }}
                                        style={{
                                            display: 'grid',
                                            gridTemplateColumns: isMobile ? '40px 1fr 80px' : '52px 1fr 130px 100px 80px',
                                            gap: '12px', padding: '16px 24px',
                                            borderBottom: i < PROBLEMS.length - 1 ? `1px solid ${C.border}` : 'none',
                                            transition: 'background-color 0.15s',
                                            opacity: solved[p.name] ? 0.5 : 1,
                                        }}
                                        onMouseEnter={e => { if (!solved[p.name]) e.currentTarget.style.backgroundColor = C.surfaceHi; }}
                                        onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                                    >
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                            {String(p.no).padStart(2, '0')}
                                        </span>
                                        <span style={{
                                            fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.onBg, fontWeight: 500,
                                            textDecoration: solved[p.name] ? 'line-through' : 'none',
                                        }}>
                                            {p.name}
                                        </span>
                                        {!isMobile && (
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted }}>
                                                {p.topic}
                                            </span>
                                        )}
                                        {!isMobile && (
                                            <span style={{
                                                padding: '2px 10px', border: `1px solid ${p.difficulty === 'Easy' ? 'rgba(74,222,128,0.3)' : p.difficulty === 'Medium' ? 'rgba(233,193,118,0.3)' : 'rgba(255,180,171,0.3)'}`,
                                                fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', textAlign: 'center',
                                                color: p.difficulty === 'Easy' ? C.success : p.difficulty === 'Medium' ? C.secondary : C.error,
                                            }}>
                                                {p.difficulty}
                                            </span>
                                        )}
                                        <div style={{ display: 'flex', justifyContent: 'center' }}>
                                            <motion.button
                                                whileTap={{ scale: 0.85 }}
                                                onClick={() => toggleSolved(p.name)}
                                                style={{
                                                    width: '28px', height: '28px', borderRadius: '50%',
                                                    border: solved[p.name] ? `1px solid ${C.success}` : `1px solid ${C.borderSolid}`,
                                                    backgroundColor: solved[p.name] ? `${C.success}10` : 'transparent',
                                                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                    cursor: 'pointer', transition: 'all 0.2s',
                                                }}
                                            >
                                                <span className="material-symbols-outlined" style={{
                                                    fontSize: '16px', color: solved[p.name] ? C.success : C.outline,
                                                    fontVariationSettings: solved[p.name] ? "'FILL' 1" : "'FILL' 0",
                                                }}>
                                                    {solved[p.name] ? 'check_circle' : 'radio_button_unchecked'}
                                                </span>
                                            </motion.button>
                                        </div>
                                    </motion.div>
                                ))}
                            </motion.div>
                        </motion.div>
                    </AnimatePresence>
                )}
            </div>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
                ::-webkit-scrollbar { width: 4px; }
                ::-webkit-scrollbar-track { background: transparent; }
                ::-webkit-scrollbar-thumb { background: #50453b; border-radius: 2px; }
            `}</style>
        </div>
    );
}
