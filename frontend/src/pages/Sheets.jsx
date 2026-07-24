import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import api from '../services/api';
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

export default function Sheets() {
    const { isMobile } = useResponsive();
    const navigate = useNavigate();
    const [sheets, setSheets] = useState([]);
    const [selected, setSelected] = useState(null);
    const [problems, setProblems] = useState([]);
    const [solved, setSolved] = useState({});
    const [loading, setLoading] = useState(true);
    const [sheetStats, setSheetStats] = useState(null);
    const [spotlight, setSpotlight] = useState({ x: 50, y: 50 });
    const ctaRef = useRef(null);

    useEffect(() => {
        api.get('/sheets').then(r => setSheets(r.data || [])).catch(() => {}).finally(() => setLoading(false));
    }, []);

    useEffect(() => {
        // Check solved from practice submissions
        api.get('/practice/submissions/user').then(r => {
            const map = {};
            (r.data || []).forEach(s => {
                if (s.status === 'AC' && s.problemId) map[s.problemId] = true;
            });
            setSolved(map);
        }).catch(() => {});
    }, []);

    const openSheet = async (sheet) => {
        setSelected(sheet);
        setLoading(true);
        try {
            const r = await api.get(`/sheets/${sheet.id}`);
            const pids = r.data?.problemIds || [];
            const all = await api.get('/problems');
            const filtered = (all.data || [])
                .filter(p => pids.includes(p.id) && p.active)
                .map((p, i) => ({
                    id: p.id,
                    no: i + 1,
                    name: p.title,
                    difficulty: p.level || 'Medium',
                    topic: p.topics || '—',
                }));

            // Compute stats
            const easyCount = filtered.filter(p => p.difficulty === 'EASY').length;
            const mediumCount = filtered.filter(p => p.difficulty === 'MEDIUM').length;
            const hardCount = filtered.filter(p => p.difficulty === 'HARD').length;
            const topicsSet = new Set(filtered.map(p => p.topic?.split(',')[0]?.trim()).filter(Boolean));
            setSheetStats({
                total: filtered.length,
                easy: easyCount,
                medium: mediumCount,
                hard: hardCount,
                topics: topicsSet.size,
                difficulty: easyCount > mediumCount ? 'Easy' : mediumCount > hardCount ? 'Medium' : 'Hard',
            });
            setProblems(filtered);
        } catch (e) {
            setProblems([]);
        }
        setLoading(false);
    };

    const progressPct = problems.length > 0
        ? Math.round((Object.keys(solved).filter(id => problems.find(p => p.id === parseInt(id))).length / problems.length) * 100)
        : 0;
    const solvedInSheet = problems.filter(p => solved[p.id]).length;

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh' }}>
            <div style={{ position: 'fixed', inset: 0, opacity: 0.03, pointerEvents: 'none', zIndex: 0,
                backgroundImage: `repeating-linear-gradient(-45deg, transparent, transparent 40px, ${C.borderSolid} 40px, ${C.borderSolid} 41px)` }} />

            <div style={{ position: 'relative', zIndex: 1, padding: isMobile ? '40px 24px' : '72px 64px' }}>

                <motion.header initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.8 }} style={{ marginBottom: '56px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px' }}>
                        <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: C.secondary, boxShadow: `0 0 12px ${C.secondary}`, animation: 'pulse 2s infinite' }} />
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase' }}>Curated Collections</span>
                    </div>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '36px' : 'clamp(44px, 6vw, 64px)', fontWeight: 700, lineHeight: 1.08, color: C.primary, margin: '0 0 20px' }}>Interview practice, <span style={{ color: C.secondary }}>organized</span>.</h1>
                </motion.header>

                {!selected ? (
                    <motion.div initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, delay: 0.2 }}>
                        {sheets.map(sheet => (
                            <div key={sheet.id} onClick={() => openSheet(sheet)} ref={ctaRef}
                                onMouseMove={e => { if (ctaRef.current) { const r = ctaRef.current.getBoundingClientRect(); setSpotlight({ x: ((e.clientX - r.left) / r.width) * 100, y: ((e.clientY - r.top) / r.height) * 100 }); } }}
                                style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, cursor: 'pointer', overflow: 'hidden', position: 'relative', transition: 'border-color 0.3s, box-shadow 0.3s' }}
                                onMouseEnter={e => { e.currentTarget.style.borderColor = C.borderSolid; e.currentTarget.style.boxShadow = '0 0 60px rgba(241,188,139,0.06)'; }}
                                onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.boxShadow = 'none'; }}>
                                <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none', opacity: 0.06, background: `radial-gradient(500px circle at ${spotlight.x}% ${spotlight.y}%, ${C.primary}, transparent 40%)`, transition: 'background 0.1s' }} />
                                <div style={{ position: 'relative', zIndex: 1, padding: isMobile ? '28px' : '40px 48px', display: 'flex', gap: '32px', alignItems: 'flex-start' }}>
                                    <div style={{ width: '72px', height: '72px', flexShrink: 0, border: `1px solid ${C.borderSolid}`, backgroundColor: `${C.primary}08`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: "'Playfair Display', serif", fontSize: '32px', fontWeight: 700, color: C.secondary }}>
                                        {(sheet.company || sheet.name)[0]}
                                    </div>
                                    <div style={{ flex: 1 }}>
                                        <div style={{ display: 'flex', alignItems: 'baseline', gap: '14px', marginBottom: '10px' }}>
                                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '24px' : '30px', fontWeight: 600, color: C.primary, margin: 0 }}>{sheet.name}</h3>
                                            {sheet.company && <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>{sheet.company}</span>}
                                        </div>
                                        {sheet.description && <p style={{ fontSize: '15px', color: C.muted, lineHeight: 1.7, margin: '0 0 20px', maxWidth: '600px' }}>{sheet.description}</p>}
                                        {sheet.tags && (
                                            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                                                {sheet.tags.split(',').map(t => (
                                                    <span key={t} style={{ padding: '4px 12px', border: `1px solid ${C.borderSolid}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.08em', color: C.secondary, textTransform: 'uppercase' }}>{t.trim()}</span>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                    <motion.div animate={{ x: [0, 6, 0] }} transition={{ duration: 2, repeat: Infinity }} style={{ flexShrink: 0, display: isMobile ? 'none' : 'flex', alignItems: 'center' }}>
                                        <span className="material-symbols-outlined" style={{ fontSize: '28px', color: C.secondary }}>arrow_forward</span>
                                    </motion.div>
                                </div>
                            </div>
                        ))}
                    </motion.div>
                ) : (
                    <AnimatePresence mode="wait">
                        <motion.div key="detail" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} transition={{ duration: 0.3 }}>
                            <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: '28px', flexWrap: 'wrap', gap: '16px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                                    <button onClick={() => setSelected(null)} style={{ padding: '10px 20px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px', transition: 'all 0.2s' }}
                                        onMouseEnter={e => { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; }}
                                        onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}>
                                        <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>arrow_back</span> Back
                                    </button>
                                    <div>
                                                <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.primary, margin: '0 0 4px' }}>{selected.name}</h2>
                                                {sheetStats && (
                                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                                        {selected.company} · {sheetStats.total} problems · {sheetStats.easy}E {sheetStats.medium}M {sheetStats.hard}H · {sheetStats.topics} topics
                                                    </span>
                                                )}
                                    </div>
                                </div>
                                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '6px' }}>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: C.secondary }}>{solvedInSheet} / {problems.length} solved</span>
                                    <div style={{ width: '200px', height: '3px', backgroundColor: C.surfaceHi, borderRadius: '2px', overflow: 'hidden' }}>
                                        <motion.div initial={{ width: 0 }} animate={{ width: `${progressPct}%` }} transition={{ duration: 0.8 }} style={{ height: '100%', backgroundColor: C.success, borderRadius: '2px' }} />
                                    </div>
                                </div>
                            </div>

                            <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}>
                                <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '56px 1fr 80px 80px' : '56px 1fr 140px 100px 100px', gap: '12px', padding: '14px 24px', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceCon }}>
                                    {['#', 'Problem', 'Topic', 'Difficulty', 'Status'].filter((_, i) => !isMobile || i <= 1 || i === 4).map(h => (
                                        <span key={h} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>{h}</span>
                                    ))}
                                </div>

                                {loading ? (
                                    <div style={{ padding: '4rem', textAlign: 'center' }}>
                                        <motion.div animate={{ rotate: 360 }} transition={{ duration: 1, repeat: Infinity, ease: 'linear' }} style={{ width: '28px', height: '28px', margin: '0 auto 16px', borderRadius: '50%', border: `2px solid ${C.border}`, borderTopColor: C.secondary }} />
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>Loading...</span>
                                    </div>
                                ) : (
                                    problems.map((p, i) => {
                                        const isSolved = solved[p.id];
                                        return (
                                            <motion.div key={p.id} initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.02 }}
                                                onClick={() => navigate(`/practice/${p.id}`)}
                                                style={{ display: 'grid', gridTemplateColumns: isMobile ? '56px 1fr 80px 80px' : '56px 1fr 140px 100px 100px', gap: '12px', padding: '16px 24px', borderBottom: i < problems.length - 1 ? `1px solid ${C.border}` : 'none', cursor: 'pointer', transition: 'background-color 0.15s', opacity: isSolved ? 0.55 : 1 }}
                                                onMouseEnter={e => e.currentTarget.style.backgroundColor = C.surfaceHi}
                                                onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}>
                                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>{String(p.no).padStart(2, '0')}</span>
                                                <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.onBg, fontWeight: 500, textDecoration: isSolved ? 'line-through' : 'none' }}>{p.name}</span>
                                                {!isMobile && <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted }}>{p.topic}</span>}
                                                {!isMobile && (
                                                    <span style={{ padding: '2px 10px', border: `1px solid ${p.difficulty === 'EASY' ? 'rgba(74,222,128,0.3)' : p.difficulty === 'MEDIUM' ? 'rgba(233,193,118,0.3)' : 'rgba(255,180,171,0.3)'}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', textAlign: 'center', color: p.difficulty === 'EASY' ? C.success : p.difficulty === 'MEDIUM' ? C.secondary : C.error }}>
                                                        {p.difficulty}
                                                    </span>
                                                )}
                                                <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                                                    {isSolved ? (
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                                            <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.success, fontVariationSettings: "'FILL' 1" }}>check_circle</span>
                                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.06em', color: C.success, textTransform: 'uppercase' }}>Completed</span>
                                                        </div>
                                                    ) : (
                                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline }}>—</span>
                                                    )}
                                                </div>
                                            </motion.div>
                                        );
                                    })
                                )}
                            </motion.div>
                        </motion.div>
                    </AnimatePresence>
                )}
            </div>
            <style>{`.material-symbols-outlined{font-variation-settings:'FILL'0,'wght'300}@keyframes pulse{0%,100%{opacity:1}50%{opacity:0.4}}::-webkit-scrollbar{width:4px}::-webkit-scrollbar-track{background:transparent}::-webkit-scrollbar-thumb{background:#50453b;border-radius:2px}`}</style>
        </div>
    );
}
