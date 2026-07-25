import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg: '#131313', surfaceLow: '#1c1b1b', surfaceCon: '#201f1f', surfaceHi: '#2a2a2a',
    border: 'rgba(241,188,139,0.2)', borderSolid: 'rgba(241,188,139,0.3)',
    primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1',
};

const TOPIC_ICONS = {
    'array': 'data_array', 'arrays': 'data_array', 'string': 'text_fields', 'strings': 'text_fields',
    'linked list': 'link', 'linked-list': 'link', 'stack': 'layers', 'queue': 'queue_music',
    'tree': 'account_tree', 'binary tree': 'account_tree', 'bst': 'account_tree',
    'graph': 'share', 'heap': 'database', 'dynamic programming': 'function', 'dp': 'function',
    'greedy': 'trending_up', 'sorting': 'sort', 'binary search': 'search', 'bit manipulation': 'memory',
    'math': 'calculate', 'mathematics': 'calculate', 'recursion': 'repeat', 'backtracking': 'undo',
    'hash table': 'fingerprint', 'two pointers': 'compare_arrows', 'sliding window': 'view_carousel',
    'bfs': 'travel_explore', 'dfs': 'explore', 'union find': 'group_work', 'trie': 'schema',
    'divide and conquer': 'call_split', 'simulation': 'play_circle', 'matrix': 'grid_on', 'design': 'design_services',
};

const ICON_KEYS = Object.keys(TOPIC_ICONS);

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

    useEffect(() => { api.get('/playlist/topics').then(r => setTopics(r.data || [])).catch(() => {}); }, []);
    useEffect(() => { if (topicSlug) setPage(0); }, [topicSlug]);
    useEffect(() => {
        if (!topicSlug) return;
        setLoading(true);
        api.get(`/playlist/${topicSlug}`, { params: { page, size: PAGE_SIZE } })
            .then(r => { setProblems(r.data?.problems || []); setTotal(r.data?.total || 0); })
            .catch(() => {}).finally(() => setLoading(false));
    }, [topicSlug, page]);

    const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
    const topicName = topics.find(t => t.slug === topicSlug)?.name || topicSlug?.replace(/-/g, ' ');

    const getIcon = (name) => {
        const key = name.toLowerCase().replace(/-/g, ' ');
        if (TOPIC_ICONS[key]) return TOPIC_ICONS[key];
        for (const k of ICON_KEYS) if (key.includes(k)) return TOPIC_ICONS[k];
        return 'tag';
    };

    const getCardVariant = (i) => ['normal', 'wide', 'tall', 'normal', 'wide'][i % 5];

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh' }}>
            <div style={{ position: 'fixed', inset: 0, opacity: 0.03, pointerEvents: 'none', zIndex: 0,
                backgroundImage: `repeating-linear-gradient(-45deg, transparent, transparent 40px, ${C.borderSolid} 40px, ${C.borderSolid} 41px)` }} />
            <div style={{ position: 'relative', zIndex: 1, padding: isMobile ? '32px 20px' : '56px 64px' }}>

                <motion.header initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} style={{ marginBottom: '48px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
                        <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: C.secondary, boxShadow: `0 0 12px ${C.secondary}`, animation: 'pulse 2s infinite' }} />
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase' }}>Curated Playlists</span>
                    </div>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '32px' : 'clamp(40px, 5vw, 56px)', fontWeight: 700, color: C.primary, margin: '0 0 12px', lineHeight: 1.1 }}>
                        {topicSlug ? <>{topicName} problems</> : <>Master every <span style={{ color: C.secondary }}>topic</span>.</>}
                    </h1>
                    <p style={{ fontSize: '15px', color: C.outline, maxWidth: '500px', lineHeight: 1.6 }}>
                        {topicSlug ? `${total} problems tagged with "${topicName}". Solve them all to master this topic.` : 'Pick a topic and solve problems organized by algorithmic category. Track your progress topic by topic.'}
                    </p>
                </motion.header>

                {!topicSlug ? (
                    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.1 }}
                        style={{ columnCount: isMobile ? 1 : 3, columnGap: '12px' }}>
                        {topics.map((t, i) => {
                            const variant = getCardVariant(i);
                            const icon = getIcon(t.name);
                            const accent = ['#f1bc8b', '#e9c176', '#d4a574', '#c4956a', '#e9c176'][i % 5];
                            return (
                                <motion.div key={t.name} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
                                    transition={{ delay: i * 0.06, duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
                                    whileHover={{ y: -4 }} onClick={() => navigate(`/playlist/${t.slug}`)}
                                    style={{ breakInside: 'avoid', marginBottom: '12px', cursor: 'pointer', border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, overflow: 'hidden', transition: 'border-color 0.3s, box-shadow 0.3s', padding: variant === 'tall' ? '36px 28px 32px' : variant === 'wide' ? '28px 32px' : '24px', position: 'relative' }}
                                    onMouseEnter={e => { e.currentTarget.style.borderColor = `${accent}50`; e.currentTarget.style.boxShadow = '0 8px 40px rgba(241,188,139,0.06)'; }}
                                    onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.boxShadow = 'none'; }}>
                                    <svg width="60" height="60" style={{ position: 'absolute', top: -10, right: -10, opacity: 0.06, pointerEvents: 'none' }}>
                                        <circle cx="30" cy="30" r="25" fill="none" stroke={accent} strokeWidth="1" />
                                        <circle cx="30" cy="30" r="18" fill="none" stroke={accent} strokeWidth="0.5" />
                                    </svg>
                                    <div style={{ position: 'absolute', top: 14, right: 14, padding: '3px 10px', borderRadius: '100px', backgroundColor: `${accent}12`, border: `1px solid ${accent}25`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: accent, letterSpacing: '0.04em' }}>{t.count}</div>
                                    <div style={{ position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column', gap: variant === 'tall' ? '16px' : '12px' }}>
                                        <div style={{ width: variant === 'tall' ? '48px' : '40px', height: variant === 'tall' ? '48px' : '40px', borderRadius: variant === 'tall' ? '14px' : '10px', backgroundColor: `${accent}10`, border: `1px solid ${accent}20`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                            <span className="material-symbols-outlined" style={{ fontSize: variant === 'tall' ? '22px' : '18px', color: accent, fontVariationSettings: "'FILL' 0" }}>{icon}</span>
                                        </div>
                                        <h3 style={{ fontFamily: "'Geist', sans-serif", fontSize: variant === 'tall' ? '18px' : '15px', fontWeight: 600, color: C.primary, margin: 0, textTransform: 'capitalize', letterSpacing: '-0.01em' }}>{t.name}</h3>
                                        {variant === 'tall' && (
                                            <div style={{ height: '2px', backgroundColor: C.border, borderRadius: '2px', overflow: 'hidden' }}>
                                                <motion.div initial={{ width: 0 }} animate={{ width: `${Math.min(100, (t.count / (topics[0]?.count || 1)) * 100)}%` }} transition={{ duration: 1, delay: 0.5 + i * 0.1 }} style={{ height: '100%', backgroundColor: accent, borderRadius: '2px' }} />
                                            </div>
                                        )}
                                    </div>
                                </motion.div>
                            );
                        })}
                    </motion.div>
                ) : (
                    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
                        <button onClick={() => navigate('/playlist')}
                            style={{ padding: '10px 20px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px', marginBottom: '24px', transition: 'all 0.2s' }}
                            onMouseEnter={e => { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; }}
                            onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>arrow_back</span> All Topics
                        </button>
                        {loading ? (
                            <div style={{ display: 'flex', justifyContent: 'center', padding: '5rem 0' }}>
                                <motion.div animate={{ rotate: 360 }} transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                                    style={{ width: '32px', height: '32px', borderRadius: '50%', border: `2px solid ${C.border}`, borderTopColor: C.secondary }} />
                            </div>
                        ) : problems.length === 0 ? (
                            <div style={{ border: `1px solid ${C.border}`, padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline, backgroundColor: C.surfaceLow }}>No problems found</div>
                        ) : (
                            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(auto-fill, minmax(320px, 1fr))', gap: '12px' }}>
                                {problems.map((p, i) => {
                                    const diffColors = { EASY: '#f1bc8b', MEDIUM: '#e9c176', HARD: '#ffb4ab' };
                                    const dc = diffColors[p.level] || C.outline;
                                    return (
                                        <motion.div key={p.id} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.04 }}
                                            onClick={() => navigate(`/practice/${p.id}`)} whileHover={{ y: -3 }}
                                            style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, cursor: 'pointer', padding: '24px', display: 'flex', flexDirection: 'column', gap: '14px', borderLeft: `3px solid ${dc}40`, transition: 'border-color 0.2s, background-color 0.2s', position: 'relative', overflow: 'hidden' }}
                                            onMouseEnter={e => { e.currentTarget.style.borderColor = C.borderSolid; e.currentTarget.style.borderLeftColor = dc; e.currentTarget.style.backgroundColor = C.surfaceCon; }}
                                            onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.borderLeftColor = `${dc}40`; e.currentTarget.style.backgroundColor = C.surfaceLow; }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '10px' }}>
                                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, letterSpacing: '0.06em' }}>{String(page * PAGE_SIZE + i + 1).padStart(2, '0')}</span>
                                                <span style={{ padding: '3px 12px', borderRadius: '100px', fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.08em', color: dc, textTransform: 'uppercase', border: `1px solid ${dc}30`, backgroundColor: `${dc}08` }}>{p.level}</span>
                                            </div>
                                            <h3 style={{ fontFamily: "'Geist', sans-serif", fontSize: '16px', fontWeight: 600, color: C.primary, margin: 0, lineHeight: 1.3, letterSpacing: '-0.01em' }}>{p.title}</h3>
                                            {p.topics && (
                                                <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                                                    {p.topics.split(',').slice(0, 3).map(t => (
                                                        <span key={t} style={{ padding: '2px 8px', borderRadius: '2px', fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.06em', color: C.outline, border: `1px solid ${C.border}` }}>{t.trim()}</span>
                                                    ))}
                                                </div>
                                            )}
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: 'auto' }}>
                                                <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.outline }}>arrow_forward</span>
                                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, letterSpacing: '0.06em', textTransform: 'uppercase' }}>Solve</span>
                                            </div>
                                        </motion.div>
                                    );
                                })}
                            </div>
                        )}
                        {totalPages > 1 && (
                            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px', marginTop: '24px' }}>
                                <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} style={{ padding: '6px 16px', border: `1px solid ${page === 0 ? C.border : C.borderSolid}`, backgroundColor: 'transparent', color: page === 0 ? C.border : C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', cursor: page === 0 ? 'not-allowed' : 'pointer' }}>← Prev</button>
                                {Array.from({length: totalPages}, (_, i) => i).filter(n => n === 0 || n === totalPages-1 || Math.abs(n - page) <= 2).reduce((acc, n, idx, arr) => { if(idx>0 && n-arr[idx-1]>1) acc.push('…'); acc.push(n); return acc; }, []).map((item, idx) => item === '…' ? (
                                    <span key={`e${idx}`} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>…</span>) : (
                                    <button key={item} onClick={() => setPage(item)} style={{ width: '28px', height: '28px', border: `1px solid ${page === item ? C.secondary : C.border}`, backgroundColor: page === item ? C.secondary : 'transparent', color: page === item ? C.bg : C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', cursor: 'pointer', borderRadius: '2px' }}>{item+1}</button>
                                ))}
                                <button onClick={() => setPage(p => Math.min(totalPages-1, p+1))} disabled={page >= totalPages-1} style={{ padding: '6px 16px', border: `1px solid ${page >= totalPages-1 ? C.border : C.borderSolid}`, backgroundColor: 'transparent', color: page >= totalPages-1 ? C.border : C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', cursor: page >= totalPages-1 ? 'not-allowed' : 'pointer' }}>Next →</button>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, marginLeft: '10px' }}>{page * PAGE_SIZE + 1}–{Math.min((page+1)*PAGE_SIZE, total)} of {total}</span>
                            </div>
                        )}
                    </motion.div>
                )}
            </div>
            <style>{`.material-symbols-outlined{font-variation-settings:'FILL'0,'wght'300}@keyframes pulse{0%,100%{opacity:1}50%{opacity:0.4}}::-webkit-scrollbar{width:4px}::-webkit-scrollbar-track{background:transparent}::-webkit-scrollbar-thumb{background:#50453b;border-radius:2px}`}</style>
        </div>
    );
}
