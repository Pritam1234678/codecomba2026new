import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import api from '../services/api';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg: '#0a0a0b',
    surface: '#111113',
    surfaceHi: '#1a1a1d',
    border: '#2a2a2d',
    borderHi: '#3a3a3f',
    primary: '#f0f0f0',
    secondary: '#a78bfa',
    accent: '#7c3aed',
    accentGlow: 'rgba(124,58,237,0.15)',
    muted: '#8b8b90',
    outline: '#5c5c63',
    onBg: '#e4e4e7',
    success: '#22c55e',
    error: '#ef4444',
    warning: '#f59e0b',
};

const GRADIENT = 'linear-gradient(135deg, #7c3aed 0%, #a78bfa 50%, #c4b5fd 100%)';
const SHEET_GRADIENT = 'linear-gradient(145deg, #1a1025 0%, #0f0820 50%, #1a1025 100%)';
const CARD_GRADIENT = 'linear-gradient(160deg, rgba(124,58,237,0.08) 0%, rgba(124,58,237,0.02) 100%)';

export default function Sheets() {
    const { isMobile } = useResponsive();
    const navigate = useNavigate();
    const [sheets] = useState([
        {
            id: 'deloitte',
            name: 'Deloitte 100',
            company: 'Deloitte',
            logo: 'D',
            questions: 100,
            difficulty: 'Mixed',
            topics: '50+ Topics',
            description: 'Complete Deloitte coding interview preparation — 100 handpicked problems covering arrays, strings, linked lists, trees, dynamic programming, and more.',
            tags: ['Deloitte', 'Interview', 'FAANG-style'],
            gradient: 'linear-gradient(135deg, #1a3a2a 0%, #0d2818 50%, #1a3a2a 100%)',
            accent: '#22c55e',
            glow: 'rgba(34,197,94,0.15)',
        },
    ]);

    const [selected, setSelected] = useState(null);
    const [problems, setProblems] = useState([]);
    const [loading, setLoading] = useState(false);
    const [solved, setSolved] = useState({});
    const hoveredCard = useRef(null);

    const openSheet = async (sheet) => {
        setSelected(sheet);
        setLoading(true);
        // For now, static Deloitte problems
        try {
            const r = await api.get('/sheets/deloitte');
            setProblems(r.data || []);
        } catch {
            // Fallback static data
            setProblems([
                { no: 1, name: 'Contains Duplicate', difficulty: 'Easy', topic: 'Arrays' },
                { no: 2, name: 'Maximum Subarray', difficulty: 'Medium', topic: 'Arrays' },
                { no: 3, name: 'Product of Array Except Self', difficulty: 'Medium', topic: 'Arrays' },
                { no: 4, name: 'Merge Sorted Array', difficulty: 'Easy', topic: 'Arrays' },
                { no: 5, name: 'Missing Number', difficulty: 'Easy', topic: 'Arrays' },
            ]);
        }
        setLoading(false);
    };

    const toggleSolved = (name) => {
        setSolved(prev => ({ ...prev, [name]: !prev[name] }));
    };

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh' }}>
            <div style={{ maxWidth: '1440px', margin: '0 auto', padding: isMobile ? '32px 20px' : '56px 64px' }}>

                {/* ── Hero ── */}
                <motion.header
                    initial={{ opacity: 0, y: 40 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
                    style={{ marginBottom: '56px', textAlign: 'center' }}
                >
                    <div style={{
                        display: 'inline-flex', alignItems: 'center', gap: '10px',
                        padding: '6px 16px', borderRadius: '100px',
                        border: `1px solid ${C.border}`, backgroundColor: C.surface,
                        fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
                        letterSpacing: '0.15em', color: C.muted, textTransform: 'uppercase',
                        marginBottom: '24px',
                    }}>
                        <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: C.accent, boxShadow: `0 0 8px ${C.accent}` }} />
                        Curated Problem Sets
                    </div>

                    <h1 style={{
                        fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '36px' : '56px',
                        fontWeight: 700, lineHeight: 1.1, letterSpacing: '-0.02em',
                        background: GRADIENT, WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent', margin: '0 0 20px',
                    }}>
                        Practice Sheets
                    </h1>
                    <p style={{
                        fontSize: '17px', color: C.muted, maxWidth: '520px', margin: '0 auto',
                        lineHeight: 1.6, fontFamily: "'Geist', sans-serif",
                    }}>
                        Company-specific and topic-focused problem collections.
                        Track your progress, mark problems as solved, and ace your interviews.
                    </p>
                </motion.header>

                {!selected ? (
                    /* ── Sheet Cards Grid ── */
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.2 }}
                        style={{
                            display: 'grid',
                            gridTemplateColumns: isMobile ? '1fr' : 'repeat(auto-fill, minmax(380px, 1fr))',
                            gap: '24px',
                        }}
                    >
                        {sheets.map((sheet, i) => (
                            <motion.div
                                key={sheet.id}
                                initial={{ opacity: 0, y: 24 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ duration: 0.5, delay: 0.1 + i * 0.1 }}
                                whileHover={{ scale: 1.01, y: -4 }}
                                onClick={() => openSheet(sheet)}
                                style={{
                                    cursor: 'pointer',
                                    borderRadius: '16px',
                                    overflow: 'hidden',
                                    border: `1px solid ${C.border}`,
                                    backgroundColor: C.surface,
                                    transition: 'all 0.3s',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.borderColor = sheet.accent;
                                    e.currentTarget.style.boxShadow = `0 8px 40px ${sheet.glow}`;
                                }}
                                onMouseLeave={e => {
                                    e.currentTarget.style.borderColor = C.border;
                                    e.currentTarget.style.boxShadow = 'none';
                                }}
                            >
                                {/* Card top accent bar */}
                                <div style={{ height: '4px', background: sheet.gradient }} />

                                <div style={{ padding: '28px 28px 24px' }}>
                                    {/* Company logo */}
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '20px' }}>
                                        <div style={{
                                            width: '48px', height: '48px', borderRadius: '12px',
                                            background: sheet.gradient,
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                            fontFamily: "'Playfair Display', serif", fontSize: '22px',
                                            fontWeight: 700, color: '#fff',
                                        }}>
                                            {sheet.logo}
                                        </div>
                                        <div>
                                            <h3 style={{
                                                fontFamily: "'Geist', sans-serif", fontSize: '18px',
                                                fontWeight: 600, color: C.primary, margin: '0 0 4px',
                                            }}>
                                                {sheet.name}
                                            </h3>
                                            <span style={{
                                                fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
                                                color: C.muted, letterSpacing: '0.08em',
                                            }}>
                                                {sheet.company}
                                            </span>
                                        </div>
                                    </div>

                                    <p style={{
                                        fontSize: '14px', color: C.muted, lineHeight: 1.6,
                                        margin: '0 0 20px',
                                    }}>
                                        {sheet.description}
                                    </p>

                                    {/* Stats row */}
                                    <div style={{
                                        display: 'flex', gap: '0',
                                        borderTop: `1px solid ${C.border}`,
                                        borderBottom: `1px solid ${C.border}`,
                                        padding: '14px 0', marginBottom: '20px',
                                    }}>
                                        {[
                                            { label: 'Problems', value: sheet.questions },
                                            { label: 'Difficulty', value: sheet.difficulty },
                                            { label: 'Topics', value: sheet.topics },
                                        ].map(({ label, value }) => (
                                            <div key={label} style={{ flex: 1, textAlign: 'center' }}>
                                                <div style={{
                                                    fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                                                    letterSpacing: '0.1em', color: C.outline,
                                                    textTransform: 'uppercase', marginBottom: '4px',
                                                }}>
                                                    {label}
                                                </div>
                                                <div style={{
                                                    fontFamily: "'Playfair Display', serif", fontSize: '20px',
                                                    fontWeight: 300, color: C.primary,
                                                }}>
                                                    {value}
                                                </div>
                                            </div>
                                        ))}
                                    </div>

                                    {/* Tags */}
                                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '20px' }}>
                                        {sheet.tags.map(tag => (
                                            <span key={tag} style={{
                                                padding: '4px 12px', borderRadius: '100px',
                                                backgroundColor: `${sheet.accent}10`,
                                                border: `1px solid ${sheet.accent}20`,
                                                fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                                                letterSpacing: '0.06em', color: sheet.accent,
                                            }}>
                                                {tag}
                                            </span>
                                        ))}
                                    </div>

                                    {/* CTA */}
                                    <div style={{
                                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        gap: '8px', padding: '12px',
                                        borderRadius: '10px', backgroundColor: `${sheet.accent}08`,
                                        border: `1px solid ${sheet.accent}15`,
                                        fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
                                        letterSpacing: '0.06em', color: sheet.accent,
                                        transition: 'all 0.2s',
                                    }}>
                                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_forward</span>
                                        Start Practice
                                    </div>
                                </div>
                            </motion.div>
                        ))}
                    </motion.div>
                ) : (
                    /* ── Sheet Detail View ── */
                    <AnimatePresence mode="wait">
                        <motion.div
                            key="detail"
                            initial={{ opacity: 0, y: 24 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -24 }}
                            transition={{ duration: 0.3 }}
                        >
                            {/* Back + Header */}
                            <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '32px' }}>
                                <button onClick={() => setSelected(null)}
                                    style={{
                                        padding: '8px 16px', borderRadius: '8px',
                                        border: `1px solid ${C.border}`, backgroundColor: C.surface,
                                        color: C.muted, fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '11px', cursor: 'pointer', display: 'flex',
                                        alignItems: 'center', gap: '6px', transition: 'all 0.2s',
                                    }}
                                    onMouseEnter={e => { e.currentTarget.style.borderColor = C.borderHi; e.currentTarget.style.color = C.primary; }}
                                    onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>arrow_back</span>
                                    Back
                                </button>
                                <div>
                                    <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.primary, margin: 0 }}>
                                        {selected.name}
                                    </h2>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted }}>
                                        {selected.company} · {selected.questions} problems
                                    </span>
                                </div>
                            </div>

                            {/* Problem Table */}
                            <div style={{
                                borderRadius: '12px', overflow: 'hidden',
                                border: `1px solid ${C.border}`, backgroundColor: C.surface,
                            }}>
                                {/* Table header */}
                                <div style={{
                                    display: 'grid', gridTemplateColumns: '60px 1fr 120px 100px 100px',
                                    gap: '16px', padding: '16px 24px',
                                    borderBottom: `1px solid ${C.border}`,
                                    backgroundColor: C.surfaceHi,
                                }}>
                                    {['#', 'Problem', 'Topic', 'Difficulty', 'Status'].map(h => (
                                        <span key={h} style={{
                                            fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                                            letterSpacing: '0.12em', color: C.outline,
                                            textTransform: 'uppercase',
                                        }}>
                                            {h}
                                        </span>
                                    ))}
                                </div>

                                {loading ? (
                                    <div style={{ padding: '4rem', textAlign: 'center' }}>
                                        <motion.div
                                            animate={{ rotate: 360 }}
                                            transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                                            style={{
                                                width: '32px', height: '32px', margin: '0 auto 16px',
                                                borderRadius: '50%', border: `2px solid ${C.border}`,
                                                borderTopColor: C.accent,
                                            }}
                                        />
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>
                                            Loading problems...
                                        </span>
                                    </div>
                                ) : (
                                    problems.map((p, i) => (
                                        <motion.div
                                            key={p.no || i}
                                            initial={{ opacity: 0, x: -8 }}
                                            animate={{ opacity: 1, x: 0 }}
                                            transition={{ delay: i * 0.02 }}
                                            style={{
                                                display: 'grid',
                                                gridTemplateColumns: '60px 1fr 120px 100px 100px',
                                                gap: '16px', padding: '18px 24px',
                                                borderBottom: i < problems.length - 1 ? `1px solid ${C.border}` : 'none',
                                                transition: 'background-color 0.15s',
                                                cursor: 'pointer',
                                            }}
                                            onMouseEnter={e => e.currentTarget.style.backgroundColor = C.surfaceHi}
                                            onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                                        >
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline }}>
                                                {p.no || i + 1}
                                            </span>
                                            <span style={{
                                                fontFamily: "'Geist', sans-serif", fontSize: '14px',
                                                color: C.primary, fontWeight: 500,
                                            }}>
                                                {p.name}
                                            </span>
                                            <span style={{
                                                fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
                                                color: C.muted, padding: '2px 0',
                                            }}>
                                                {p.topic}
                                            </span>
                                            <span style={{
                                                padding: '3px 10px', borderRadius: '4px',
                                                fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                                                letterSpacing: '0.06em',
                                                backgroundColor: p.difficulty === 'Easy' ? 'rgba(34,197,94,0.1)' :
                                                    p.difficulty === 'Medium' ? 'rgba(245,158,11,0.1)' : 'rgba(239,68,68,0.1)',
                                                color: p.difficulty === 'Easy' ? '#22c55e' :
                                                    p.difficulty === 'Medium' ? '#f59e0b' : '#ef4444',
                                                textAlign: 'center',
                                            }}>
                                                {p.difficulty}
                                            </span>
                                            <div style={{ display: 'flex', justifyContent: 'center' }}>
                                                <motion.button
                                                    whileTap={{ scale: 0.9 }}
                                                    onClick={(e) => { e.stopPropagation(); toggleSolved(p.name); }}
                                                    style={{
                                                        width: '32px', height: '32px', borderRadius: '8px',
                                                        border: solved[p.name] ? `1px solid ${C.success}` : `1px solid ${C.border}`,
                                                        backgroundColor: solved[p.name] ? `${C.success}10` : 'transparent',
                                                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                        cursor: 'pointer', transition: 'all 0.2s',
                                                    }}
                                                >
                                                    <span className="material-symbols-outlined" style={{
                                                        fontSize: '16px', color: solved[p.name] ? C.success : C.outline,
                                                        fontVariationSettings: solved[p.name] ? "'FILL' 1" : "'FILL' 0",
                                                    }}>
                                                        {solved[p.name] ? 'check_circle' : 'circle'}
                                                    </span>
                                                </motion.button>
                                            </div>
                                        </motion.div>
                                    ))
                                )}
                            </div>
                        </motion.div>
                    </AnimatePresence>
                )}

                {/* ── Coming Soon Section ── */}
                {!selected && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ delay: 0.6, duration: 0.5 }}
                        style={{ marginTop: '56px', textAlign: 'center' }}
                    >
                        <span style={{
                            fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                            letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase',
                        }}>
                            More sheets coming soon
                        </span>
                        <div style={{
                            display: 'flex', justifyContent: 'center', gap: '12px', marginTop: '20px',
                        }}>
                            {['Google', 'Amazon', 'Microsoft', 'Meta', 'Apple'].map(company => (
                                <div key={company} style={{
                                    padding: '12px 24px', borderRadius: '10px',
                                    border: `1px dashed ${C.border}`,
                                    backgroundColor: C.surface,
                                    opacity: 0.4,
                                }}>
                                    <span style={{
                                        fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
                                        color: C.muted, letterSpacing: '0.08em',
                                    }}>
                                        {company}
                                    </span>
                                </div>
                            ))}
                        </div>
                    </motion.div>
                )}
            </div>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                ::-webkit-scrollbar { width: 4px; height: 4px; }
                ::-webkit-scrollbar-track { background: transparent; }
                ::-webkit-scrollbar-thumb { background: #2a2a2d; border-radius: 2px; }
            `}</style>
        </div>
    );
}
