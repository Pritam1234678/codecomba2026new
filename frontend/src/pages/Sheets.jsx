import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg:         '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceHi:  '#2a2a2a',
    surfaceMin: '#0e0e0e',
    border:     '#50453b',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
    success:    '#4ade80',
};

const SHEETS = [
    {
        id: 'deloitte',
        name: 'Deloitte 100',
        company: 'Deloitte',
        problems: 100,
        topics: '50+ Topics',
        description: 'Complete Deloitte coding interview preparation — 100 handpicked problems covering arrays, strings, linked lists, trees, dynamic programming, graph, and more.',
        tags: ['Deloitte', 'Interview', 'FAANG-style'],
    },
];

const MOCK_PROBLEMS = [
    { no: 1, name: 'Contains Duplicate', difficulty: 'Easy', topic: 'Arrays' },
    { no: 2, name: 'Maximum Subarray', difficulty: 'Medium', topic: 'Arrays' },
    { no: 3, name: 'Product of Array Except Self', difficulty: 'Medium', topic: 'Arrays' },
    { no: 4, name: 'Merge Sorted Array', difficulty: 'Easy', topic: 'Arrays' },
    { no: 5, name: 'Missing Number', difficulty: 'Easy', topic: 'Arrays' },
    { no: 6, name: 'Armstrong Number', difficulty: 'Easy', topic: 'Math' },
    { no: 7, name: 'Valid Anagram', difficulty: 'Easy', topic: 'Strings' },
    { no: 8, name: 'Valid Palindrome', difficulty: 'Easy', topic: 'Strings' },
];

export default function Sheets() {
    const { isMobile } = useResponsive();
    const [selected, setSelected] = useState(null);
    const [problems] = useState(MOCK_PROBLEMS);
    const [solved, setSolved] = useState({});

    const toggleSolved = (name) => setSolved(p => ({ ...p, [name]: !p[name] }));
    const solvedCount = Object.values(solved).filter(Boolean).length;

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh' }}>
            <div style={{ maxWidth: '1280px', margin: '0 auto', padding: isMobile ? '32px 20px' : '56px 64px' }}>

                {/* ── Header ── */}
                <motion.header
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6 }}
                    style={{ marginBottom: '48px' }}
                >
                    <span style={{
                        fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                        letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase',
                        display: 'block', marginBottom: '12px',
                    }}>
                        Curated Problem Sets
                    </span>
                    <h1 style={{
                        fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '32px' : '48px',
                        fontWeight: 700, color: C.primary, margin: '0 0 12px',
                    }}>
                        Practice Sheets
                    </h1>
                    <p style={{ fontSize: '15px', color: C.outline, maxWidth: '480px', lineHeight: 1.6 }}>
                        Company-specific problem collections to prepare for your dream interviews.
                        Track progress, mark solved, and stay organized.
                    </p>
                </motion.header>

                {!selected ? (
                    /* ── Sheet Cards ── */
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.5, delay: 0.15 }}
                        style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}
                    >
                        {SHEETS.map((sheet, i) => (
                            <motion.div
                                key={sheet.id}
                                initial={{ opacity: 0, y: 16 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ delay: 0.2 + i * 0.1 }}
                                onClick={() => setSelected(sheet)}
                                style={{
                                    border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow,
                                    cursor: 'pointer', overflow: 'hidden',
                                    transition: 'all 0.3s', borderLeft: `3px solid ${C.secondary}`,
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.backgroundColor = C.surfaceCon;
                                    e.currentTarget.style.borderLeftColor = C.primary;
                                }}
                                onMouseLeave={e => {
                                    e.currentTarget.style.backgroundColor = C.surfaceLow;
                                    e.currentTarget.style.borderLeftColor = C.secondary;
                                }}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', gap: '24px', padding: isMobile ? '24px' : '32px 36px' }}>
                                    {/* Logo */}
                                    <div style={{
                                        width: '64px', height: '64px', flexShrink: 0,
                                        border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin,
                                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        fontFamily: "'Playfair Display', serif", fontSize: '28px',
                                        fontWeight: 700, color: C.secondary,
                                    }}>
                                        {sheet.company[0]}
                                    </div>

                                    {/* Content */}
                                    <div style={{ flex: 1, minWidth: 0 }}>
                                        <div style={{ display: 'flex', alignItems: 'baseline', gap: '12px', marginBottom: '8px', flexWrap: 'wrap' }}>
                                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 600, color: C.primary, margin: 0 }}>
                                                {sheet.name}
                                            </h3>
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, letterSpacing: '0.06em' }}>
                                                {sheet.company}
                                            </span>
                                        </div>
                                        <p style={{ fontSize: '14px', color: C.muted, lineHeight: 1.6, margin: '0 0 14px' }}>
                                            {sheet.description}
                                        </p>
                                        <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
                                            {[
                                                { label: 'PROBLEMS', value: sheet.problems },
                                                { label: 'TOPICS', value: sheet.topics },
                                            ].map(({ label, value }) => (
                                                <div key={label}>
                                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>{label}</span>
                                                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '18px', fontWeight: 300, color: C.onBg }}>{value}</span>
                                                </div>
                                            ))}
                                            {sheet.tags.map(tag => (
                                                <span key={tag} style={{
                                                    padding: '3px 10px', border: `1px solid ${C.border}`,
                                                    fontFamily: "'JetBrains Mono', monospace", fontSize: '9px',
                                                    letterSpacing: '0.08em', color: C.secondary, textTransform: 'uppercase',
                                                }}>
                                                    {tag}
                                                </span>
                                            ))}
                                        </div>
                                    </div>

                                    {/* Arrow */}
                                    <span className="material-symbols-outlined" style={{ fontSize: '24px', color: C.outline, flexShrink: 0 }}>
                                        arrow_forward
                                    </span>
                                </div>
                            </motion.div>
                        ))}

                        {/* Coming soon */}
                        <div style={{ marginTop: '32px' }}>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '16px' }}>
                                Coming Soon
                            </span>
                            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(4, 1fr)', gap: '1px', backgroundColor: C.border }}>
                                {['Google', 'Amazon', 'Microsoft', 'Meta'].map(c => (
                                    <div key={c} style={{ padding: '32px 24px', backgroundColor: C.surfaceLow, opacity: 0.5, textAlign: 'center' }}>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase' }}>
                                            {c}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </motion.div>
                ) : (
                    /* ── Detail View ── */
                    <AnimatePresence mode="wait">
                        <motion.div
                            key="detail"
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0 }}
                            transition={{ duration: 0.3 }}
                        >
                            {/* Back */}
                            <div style={{ display: 'flex', alignItems: 'center', gap: '20px', marginBottom: '32px' }}>
                                <button onClick={() => setSelected(null)}
                                    style={{
                                        padding: '10px 20px', border: `1px solid ${C.border}`,
                                        backgroundColor: 'transparent', color: C.muted,
                                        fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
                                        letterSpacing: '0.08em', textTransform: 'uppercase',
                                        cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px',
                                        transition: 'all 0.2s',
                                    }}
                                    onMouseEnter={e => { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; }}
                                    onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>arrow_back</span> Back
                                </button>
                                <div>
                                    <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '26px', fontWeight: 600, color: C.primary, margin: '0 0 4px' }}>
                                        {selected.name}
                                    </h2>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                        {selected.company} · {selected.problems} problems
                                    </span>
                                </div>
                                <div style={{ marginLeft: 'auto' }}>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: C.secondary, textTransform: 'uppercase' }}>
                                        {solvedCount} / {problems.length} solved
                                    </span>
                                </div>
                            </div>

                            {/* Problem Table */}
                            <motion.div
                                initial={{ opacity: 0, y: 12 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ delay: 0.1 }}
                                style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}
                            >
                                {/* Header */}
                                <div style={{
                                    display: 'grid', gridTemplateColumns: '56px 1fr 120px 100px 80px',
                                    gap: '16px', padding: '14px 24px', borderBottom: `1px solid ${C.border}`,
                                    backgroundColor: C.surfaceHi,
                                }}>
                                    {['#', 'Problem', 'Topic', 'Difficulty', 'Solved'].map(h => (
                                        <span key={h} style={{
                                            fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                                            letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase',
                                        }}>
                                            {h}
                                        </span>
                                    ))}
                                </div>

                                {problems.map((p, i) => (
                                    <motion.div
                                        key={p.no}
                                        initial={{ opacity: 0, x: -6 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        transition={{ delay: i * 0.025 }}
                                        style={{
                                            display: 'grid',
                                            gridTemplateColumns: '56px 1fr 120px 100px 80px',
                                            gap: '16px', padding: '16px 24px',
                                            borderBottom: i < problems.length - 1 ? `1px solid ${C.border}` : 'none',
                                            transition: 'background-color 0.15s',
                                            opacity: solved[p.name] ? 0.6 : 1,
                                        }}
                                        onMouseEnter={e => e.currentTarget.style.backgroundColor = C.surfaceCon}
                                        onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                                    >
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                            {String(p.no).padStart(2, '0')}
                                        </span>
                                        <span style={{
                                            fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.onBg,
                                            textDecoration: solved[p.name] ? 'line-through' : 'none',
                                        }}>
                                            {p.name}
                                        </span>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted, padding: '2px 0' }}>
                                            {p.topic}
                                        </span>
                                        <span style={{
                                            padding: '2px 10px', borderRadius: '2px',
                                            fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                                            letterSpacing: '0.06em', textAlign: 'center',
                                            border: `1px solid ${p.difficulty === 'Easy' ? '#4ade8020' : p.difficulty === 'Medium' ? '#e9c17620' : '#ffb4ab20'}`,
                                            color: p.difficulty === 'Easy' ? C.success : p.difficulty === 'Medium' ? C.secondary : C.error,
                                        }}>
                                            {p.difficulty}
                                        </span>
                                        <div style={{ display: 'flex', justifyContent: 'center' }}>
                                            <motion.button
                                                whileTap={{ scale: 0.85 }}
                                                onClick={() => toggleSolved(p.name)}
                                                style={{
                                                    width: '28px', height: '28px', border: solved[p.name] ? `1px solid ${C.success}` : `1px solid ${C.border}`,
                                                    backgroundColor: solved[p.name] ? `${C.success}10` : 'transparent',
                                                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                    cursor: 'pointer', transition: 'all 0.2s',
                                                }}
                                            >
                                                <span className="material-symbols-outlined" style={{
                                                    fontSize: '15px', color: solved[p.name] ? C.success : C.border,
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
                ::-webkit-scrollbar { width: 4px; }
                ::-webkit-scrollbar-track { background: transparent; }
                ::-webkit-scrollbar-thumb { background: #50453b; border-radius: 2px; }
            `}</style>
        </div>
    );
}
