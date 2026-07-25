import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg: '#131313', surfaceLow: '#1c1b1b', surfaceCon: '#201f1f', surfaceHi: '#2a2a2a',
    border: 'rgba(241,188,139,0.2)', borderSolid: 'rgba(241,188,139,0.3)',
    primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1',
    success: '#4ade80', error: '#ffb4ab',
};

const DIFF_COLORS = { EASY: '#f1bc8b', MEDIUM: '#e9c176', HARD: '#ffb4ab' };

export default function Playlist() {
    const { isMobile } = useResponsive();
    const navigate = useNavigate();
    const { topic: topicSlug } = useParams();
    const [topics, setTopics] = useState([]);
    const [problems, setProblems] = useState([]);
    const [total, setTotal] = useState(0);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const PAGE_SIZE = 25;

    useEffect(() => {
        api.get('/playlist/topics').then(r => setTopics(r.data || [])).catch(() => {});
    }, []);

    useEffect(() => {
        if (!topicSlug) return;
        setPage(0);
    }, [topicSlug]);
            .then(r => { setProblems(r.data?.problems || []); setTotal(r.data?.total || 0); })
            .catch(() => {})
            .finally(() => setLoading(false));
    }, [topicSlug, page]);

    const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
    const topicName = topics.find(t => t.slug === topicSlug)?.name || topicSlug?.replace(/-/g, ' ');

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh' }}>
            <div style={{ position: 'fixed', inset: 0, opacity: 0.03, pointerEvents: 'none', zIndex: 0,
                backgroundImage: `repeating-linear-gradient(-45deg, transparent, transparent 40px, ${C.borderSolid} 40px, ${C.borderSolid} 41px)` }} />
            <div style={{ position: 'relative', zIndex: 1, padding: isMobile ? '32px 20px' : '56px 64px' }}>

                <motion.header initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} style={{ marginBottom: '48px' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase', display: 'block', marginBottom: '12px' }}>Topic Playlists</span>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '32px' : '48px', fontWeight: 700, color: C.primary, margin: '0 0 12px' }}>
                        {topicSlug ? topicName : 'Explore by Topic'}
                    </h1>
                    {topicSlug && (
                        <button onClick={() => navigate('/playlist')} style={{ padding: '8px 18px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px', marginTop: '8px' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>arrow_back</span> All Topics
                        </button>
                    )}
                </motion.header>

                {!topicSlug ? (
                    /* Topics Grid */
                    <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
                        style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(auto-fill, minmax(220px, 1fr))', gap: '10px' }}>
                        {topics.map((t, i) => (
                            <motion.div key={t.name} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.04 }}
                                onClick={() => navigate(`/playlist/${t.slug}`)}
                                whileHover={{ y: -3 }}
                                style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, padding: '24px', cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: '12px', transition: 'border-color 0.2s, box-shadow 0.2s' }}
                                onMouseEnter={e => { e.currentTarget.style.borderColor = C.borderSolid; e.currentTarget.style.boxShadow = '0 0 30px rgba(241,188,139,0.04)'; }}
                                onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.boxShadow = 'none'; }}>
                                <div style={{ width: '36px', height: '36px', borderRadius: '50%', backgroundColor: `${C.secondary}10`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.secondary }}>tag</span>
                                </div>
                                <div>
                                    <h3 style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', fontWeight: 600, color: C.primary, margin: '0 0 4px', textTransform: 'capitalize' }}>{t.name}</h3>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>{t.count} problem{t.count !== 1 ? 's' : ''}</span>
                                </div>
                            </motion.div>
                        ))}
                    </motion.div>
                ) : (
                    /* Problem List */
                    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
                        <div style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}>
                            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '56px 1fr 100px' : '56px 1fr 140px 100px', gap: '12px', padding: '14px 24px', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceCon }}>
                                {['#', 'Problem', 'Topics', 'Difficulty'].filter((_, i) => !isMobile || i <= 1 || i === 3).map(h => (
                                    <span key={h} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>{h}</span>
                                ))}
                            </div>
                            {loading ? (
                                <div style={{ padding: '4rem', textAlign: 'center' }}>
                                    <motion.div animate={{ rotate: 360 }} transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                                        style={{ width: '28px', height: '28px', margin: '0 auto 14px', borderRadius: '50%', border: `2px solid ${C.border}`, borderTopColor: C.secondary }} />
                                </div>
                            ) : problems.length === 0 ? (
                                <div style={{ padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>No problems found</div>
                            ) : (
                                problems.map((p, i) => (
                                    <motion.div key={p.id} initial={{ opacity: 0, x: -6 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.02 }}
                                        onClick={() => navigate(`/practice/${p.id}`)}
                                        style={{ display: 'grid', gridTemplateColumns: isMobile ? '56px 1fr 100px' : '56px 1fr 140px 100px', gap: '12px', padding: '16px 24px', borderBottom: i < problems.length - 1 ? `1px solid ${C.border}` : 'none', cursor: 'pointer', transition: 'background-color 0.15s' }}
                                        onMouseEnter={e => e.currentTarget.style.backgroundColor = C.surfaceHi}
                                        onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>{String(page * PAGE_SIZE + i + 1).padStart(2, '0')}</span>
                                        <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.onBg, fontWeight: 500 }}>{p.title}</span>
                                        {!isMobile && <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.muted }}>{p.topics || '—'}</span>}
                                        <span style={{ padding: '2px 10px', border: `1px solid ${DIFF_COLORS[p.level] || C.border}30`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', textAlign: 'center', color: DIFF_COLORS[p.level] || C.outline }}>{p.level}</span>
                                    </motion.div>
                                ))
                            )}
                        </div>

                        {totalPages > 1 && (
                            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px', marginTop: '24px' }}>
                                <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                                    style={{ padding: '6px 16px', border: `1px solid ${page === 0 ? C.border : C.borderSolid}`, backgroundColor: 'transparent', color: page === 0 ? C.border : C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', cursor: page === 0 ? 'not-allowed' : 'pointer' }}>← Prev</button>
                                {Array.from({length: totalPages}, (_, i) => i).filter(n => n === 0 || n === totalPages-1 || Math.abs(n - page) <= 2).reduce((acc, n, idx, arr) => { if(idx>0 && n-arr[idx-1]>1) acc.push('…'); acc.push(n); return acc; }, []).map((item, idx) => item === '…' ? (
                                    <span key={`e${idx}`} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>…</span>
                                ) : (
                                    <button key={item} onClick={() => setPage(item)}
                                        style={{ width: '28px', height: '28px', border: `1px solid ${page === item ? C.secondary : C.border}`, backgroundColor: page === item ? C.secondary : 'transparent', color: page === item ? C.bg : C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', cursor: 'pointer', borderRadius: '2px' }}>{item+1}</button>
                                ))}
                                <button onClick={() => setPage(p => Math.min(totalPages-1, p+1))} disabled={page >= totalPages-1}
                                    style={{ padding: '6px 16px', border: `1px solid ${page >= totalPages-1 ? C.border : C.borderSolid}`, backgroundColor: 'transparent', color: page >= totalPages-1 ? C.border : C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', cursor: page >= totalPages-1 ? 'not-allowed' : 'pointer' }}>Next →</button>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, marginLeft: '10px' }}>{page * PAGE_SIZE + 1}–{Math.min((page+1)*PAGE_SIZE, total)} of {total}</span>
                            </div>
                        )}
                    </motion.div>
                )}
            </div>
            <style>{`.material-symbols-outlined{font-variation-settings:'FILL'0,'wght'300}::-webkit-scrollbar{width:4px}::-webkit-scrollbar-track{background:transparent}::-webkit-scrollbar-thumb{background:#50453b;border-radius:2px}`}</style>
        </div>
    );
}
