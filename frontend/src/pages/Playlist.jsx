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
    'array': 'data_array', 'arrays': 'data_array',
    'string': 'text_fields', 'strings': 'text_fields',
    'linked list': 'link', 'linked-list': 'link',
    'stack': 'layers', 'queue': 'queue_music',
    'tree': 'account_tree', 'binary tree': 'account_tree', 'bst': 'account_tree',
    'graph': 'share', 'heap': 'database',
    'dynamic programming': 'function', 'dp': 'function',
    'greedy': 'trending_up', 'sorting': 'sort',
    'binary search': 'search', 'bit manipulation': 'memory',
    'math': 'calculate', 'mathematics': 'calculate',
    'recursion': 'repeat', 'backtracking': 'undo',
    'hash table': 'fingerprint', 'two pointers': 'compare_arrows',
    'sliding window': 'view_carousel', 'bfs': 'travel_explore', 'dfs': 'explore',
    'union find': 'group_work', 'trie': 'schema',
    'divide and conquer': 'call_split', 'simulation': 'play_circle',
    'matrix': 'grid_on', 'design': 'design_services',
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

    useEffect(() => {
        api.get('/playlist/topics').then(r => setTopics(r.data || [])).catch(() => {});
    }, []);

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

    // Card size variants for asymmetrical grid
    const getCardVariant = (i) => {
        const variants = ['normal', 'wide', 'tall', 'normal', 'wide'];
        return variants[i % variants.length];
    };

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
                        {topicSlug ? <><span style={{ color: C.secondary }}>{topicName}</span> problems</> : 'Master every <span style={{ color: C.secondary }}>topic</span>.'}
                    </h1>
                    <p style={{ fontSize: '15px', color: C.outline, maxWidth: '500px', lineHeight: 1.6 }}>
                        {topicSlug ? `${total} problems tagged with "${topicName}". Solve them all to master this topic.` : 'Pick a topic and solve problems organized by algorithmic category. Track your progress topic by topic.'}
                    </p>
                </motion.header>

                {!topicSlug ? (
                    /* Asymmetrical Topic Cards */
                    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.1 }}
                        style={{
                            columnCount: isMobile ? 1 : 3,
                            columnGap: '12px',
                        }}>
                        {topics.map((t, i) => {
                            const variant = getCardVariant(i);
                            const icon = getIcon(t.name);
                            const accentColors = ['#f1bc8b', '#e9c176', '#d4a574', '#c4956a', '#e9c176'];
                            const accent = accentColors[i % accentColors.length];

                            return (
                                <motion.div
                                    key={t.name}
                                    initial={{ opacity: 0, y: 20 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    transition={{ delay: i * 0.06, duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
                                    whileHover={{ y: -4, transition: { duration: 0.2 } }}
                                    onClick={() => navigate(`/playlist/${t.slug}`)}
                                    style={{
                                        breakInside: 'avoid',
                                        marginBottom: '12px',
                                        cursor: 'pointer',
                                        border: `1px solid ${C.border}`,
                                        backgroundColor: C.surfaceLow,
                                        position: 'relative', overflow: 'hidden',
                                        transition: 'border-color 0.3s, box-shadow 0.3s',
                                        padding: variant === 'tall' ? '36px 28px 32px' : variant === 'wide' ? '28px 32px' : '24px',
                                    }}
                                    onMouseEnter={e => {
                                        e.currentTarget.style.borderColor = `${accent}50`;
                                        e.currentTarget.style.boxShadow = `0 8px 40px rgba(241,188,139,0.06)`;
                                    }}
                                    onMouseLeave={e => {
                                        e.currentTarget.style.borderColor = C.border;
                                        e.currentTarget.style.boxShadow = 'none';
                                    }}>
                                    {/* Decorative corner */}
                                    <svg width="60" height="60" style={{ position: 'absolute', top: -10, right: -10, opacity: 0.06, pointerEvents: 'none' }}>
                                        <circle cx="30" cy="30" r="25" fill="none" stroke={accent} strokeWidth="1" />
                                        <circle cx="30" cy="30" r="18" fill="none" stroke={accent} strokeWidth="0.5" />
                                    </svg>

                                    {/* Count badge */}
                                    <div style={{
                                        position: 'absolute', top: '14px', right: '14px',
                                        padding: '3px 10px', borderRadius: '100px',
                                        backgroundColor: `${accent}12`, border: `1px solid ${accent}25`,
                                        fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                                        color: accent, letterSpacing: '0.04em',
                                    }}>
                                        {t.count}
                                    </div>

                                    {/* Icon + Content */}
                                    <div style={{ position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column', gap: variant === 'tall' ? '16px' : '12px' }}>
                                        <div style={{
                                            width: variant === 'tall' ? '48px' : '40px', height: variant === 'tall' ? '48px' : '40px',
                                            borderRadius: variant === 'tall' ? '14px' : '10px',
                                            backgroundColor: `${accent}10`, border: `1px solid ${accent}20`,
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        }}>
                                            <span className="material-symbols-outlined" style={{ fontSize: variant === 'tall' ? '22px' : '18px', color: accent, fontVariationSettings: "'FILL' 0" }}>
                                                {icon}
                                            </span>
                                        </div>

                                        <div>
                                            <h3 style={{
                                                fontFamily: "'Geist', sans-serif", fontSize: variant === 'tall' ? '18px' : '15px',
                                                fontWeight: 600, color: C.primary, margin: '0 0 4px',
                                                textTransform: 'capitalize', letterSpacing: '-0.01em',
                                            }}>
                                                {t.name}
                                            </h3>
                                        </div>

                                        {/* Progress bar */}
                                        {variant === 'tall' && (
                                            <div style={{ height: '2px', backgroundColor: C.border, borderRadius: '2px', overflow: 'hidden' }}>
                                                <motion.div
                                                    initial={{ width: 0 }}
                                                    animate={{ width: `${Math.min(100, (t.count / topics[0]?.count) * 100)}%` }}
                                                    transition={{ duration: 1, delay: 0.5 + i * 0.1 }}
                                                    style={{ height: '100%', backgroundColor: accent, borderRadius: '2px' }}
                                                />
                                            </div>
                                        )}
                                    </div>
                                </motion.div>
                            );
                        })}
                    </motion.div>
                ) : (
                    /* Problem List */
                    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
                        <button onClick={() => navigate('/playlist')}
                            style={{ padding: '10px 20px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px', marginBottom: '24px', transition: 'all 0.2s' }}
                            onMouseEnter={e => { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; }}
                            onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>arrow_back</span> All Topics
                        </button>

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
                                problems.map((p, i) => {
                                    const diffColors = { EASY: '#f1bc8b', MEDIUM: '#e9c176', HARD: '#ffb4ab' };
                                    return (
                                        <motion.div key={p.id} initial={{ opacity: 0, x: -6 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.02 }}
                                            onClick={() => navigate(`/practice/${p.id}`)}
                                            style={{ display: 'grid', gridTemplateColumns: isMobile ? '56px 1fr 100px' : '56px 1fr 140px 100px', gap: '12px', padding: '16px 24px', borderBottom: i < problems.length - 1 ? `1px solid ${C.border}` : 'none', cursor: 'pointer', transition: 'background-color 0.15s' }}
                                            onMouseEnter={e => e.currentTarget.style.backgroundColor = C.surfaceHi}
                                            onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}>
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>{String(page * PAGE_SIZE + i + 1).padStart(2, '0')}</span>
                                            <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.onBg, fontWeight: 500 }}>{p.title}</span>
                                            {!isMobile && <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.muted }}>{p.topics || '—'}</span>}
                                            <span style={{ padding: '2px 10px', border: `1px solid ${diffColors[p.level] || C.border}30`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', textAlign: 'center', color: diffColors[p.level] || C.outline }}>{p.level}</span>
                                        </motion.div>
                                    );
                                })
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
            <style>{`.material-symbols-outlined{font-variation-settings:'FILL'0,'wght'300}@keyframes pulse{0%,100%{opacity:1}50%{opacity:0.4}}::-webkit-scrollbar{width:4px}::-webkit-scrollbar-track{background:transparent}::-webkit-scrollbar-thumb{background:#50453b;border-radius:2px}`}</style>
        </div>
    );
}
