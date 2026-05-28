import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import api from '../services/api';

const C = {
    bg:'#131313', surfaceCon:'#201f1f', surfaceLow:'#1c1b1b', surfaceHi:'#2a2a2a', surfaceMin:'#0e0e0e',
    border:'#50453b', primary:'#f1bc8b', secondary:'#e9c176', muted:'#d4c4b7', outline:'#9d8e83',
    onBg:'#e5e2e1', error:'#ffb4ab', success:'#66bb6a',
};

const LEVEL_CFG = {
    EASY:   { color:'#66bb6a', border:'#66bb6a', bg:'rgba(102,187,106,0.12)' },
    MEDIUM: { color:'#e9c176', border:'#e9c176', bg:'rgba(233,193,118,0.12)' },
    HARD:   { color:'#ffb4ab', border:'#ffb4ab', bg:'rgba(255,180,171,0.12)' },
};

export default function ManageContestProblems() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [contest,  setContest]  = useState(null);
    const [problems, setProblems] = useState([]);
    const [loading,  setLoading]  = useState(true);
    const [delModal, setDelModal] = useState({ show:false, problemId:null, problemTitle:'' });
    const [toast,    setToast]    = useState(null);
    const [browseModalOpen, setBrowseModalOpen] = useState(false);
    const [browseSearch, setBrowseSearch] = useState('');
    const [browseLevel, setBrowseLevel] = useState('ALL');
    const [available, setAvailable] = useState([]);
    const [browseLoading, setBrowseLoading] = useState(false);
    const [selectedIds, setSelectedIds] = useState(new Set());
    const [browsePage, setBrowsePage] = useState(0);
    const [browseTotalPages, setBrowseTotalPages] = useState(0);
    const [browseTotalElements, setBrowseTotalElements] = useState(0);
    const PAGE_SIZE = 10;

    useEffect(() => { load(); }, [id]);

    // Debounced fetch of available problems while modal is open.
    // Uses a generation counter to ignore stale responses from earlier
    // typed states — without this, a slow first request can overwrite
    // the result of a later, faster one ("list flashes then vanishes").
    useEffect(() => {
        if (!browseModalOpen) return;
        let cancelled = false;
        const handle = setTimeout(async () => {
            setBrowseLoading(true);
            try {
                const res = await api.get(`/admin/contests/${id}/available-problems`, {
                    params: {
                        search: browseSearch || undefined,
                        level: browseLevel === 'ALL' ? undefined : browseLevel,
                        page: browsePage,
                        size: PAGE_SIZE,
                    },
                });
                if (cancelled) return; // ignore stale response
                // Handle both paginated response {content, totalElements, totalPages}
                // and legacy flat array response (defensive — backend now always
                // returns paginated, but cached old shape may still exist briefly).
                const data = res.data;
                if (data && Array.isArray(data.content)) {
                    setAvailable(data.content);
                    setBrowseTotalPages(data.totalPages || 1);
                    setBrowseTotalElements(data.totalElements ?? data.content.length);
                } else if (Array.isArray(data)) {
                    // Legacy flat-array fallback
                    setAvailable(data);
                    setBrowseTotalPages(1);
                    setBrowseTotalElements(data.length);
                } else {
                    setAvailable([]);
                    setBrowseTotalPages(0);
                    setBrowseTotalElements(0);
                }
            } catch (err) {
                if (cancelled) return;
                console.error('Browse load error:', err?.response?.status, err?.response?.data || err.message);
                setAvailable([]);
                setBrowseTotalPages(0);
                setBrowseTotalElements(0);
            } finally {
                if (!cancelled) setBrowseLoading(false);
            }
        }, 300);
        return () => {
            cancelled = true;
            clearTimeout(handle);
        };
    }, [browseModalOpen, browseSearch, browseLevel, browsePage, id]);

    const closeBrowseModal = () => {
        setBrowseModalOpen(false);
        setBrowseSearch('');
        setBrowseLevel('ALL');
        setAvailable([]);
        setSelectedIds(new Set());
        setBrowsePage(0);
        setBrowseTotalPages(0);
        setBrowseTotalElements(0);
    };

    const toggleSelected = (pid) => {
        setSelectedIds(prev => {
            const next = new Set(prev);
            if (next.has(pid)) next.delete(pid);
            else next.add(pid);
            return next;
        });
    };

    const load = async () => {
        try {
            const [cRes, pRes] = await Promise.all([
                api.get(`/contests/${id}`),
                api.get(`/admin/problems/contest/${id}`),
            ]);
            setContest(cRes.data);
            setProblems(pRes.data);
        } catch (err) {
            console.error('Load error:', err);
        }
        finally { setLoading(false); }
    };

    const showToast = (msg, type = 'success') => {
        setToast({ msg, type });
        setTimeout(() => setToast(null), 3000);
    };

    const handleToggleActive = async (problem) => {
        try {
            await api.patch(`/admin/problems/${problem.id}/toggle-active`);
            showToast(problem.active ? 'Problem disabled.' : 'Problem enabled.');
            load();
        } catch { showToast('Failed to update.', 'error'); }
    };

    const confirmDelete = async () => {
        try {
            await api.delete(`/admin/contests/${id}/problems/${delModal.problemId}`);
            setDelModal({ show: false });
            showToast('Problem detached.');
            load();
        } catch { showToast('Failed to detach.', 'error'); setDelModal({ show: false }); }
    };

    if (loading) return (
        <div style={{ display:'flex', alignItems:'center', justifyContent:'center', minHeight:'60vh',
            color: C.outline, fontFamily:"'JetBrains Mono', monospace", fontSize:'13px' }}>
            Loading...
        </div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily:"'Geist', sans-serif", minHeight:'100vh' }}>

            {/* ── Hero Header ── */}
            <header style={{ backgroundColor: C.surfaceLow, borderBottom:`1px solid ${C.border}`, padding:'48px 64px 40px' }}>
                <span style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'11px', letterSpacing:'0.2em', color: C.secondary, textTransform:'uppercase', display:'block', marginBottom:'16px' }}>
                    Contest ID: CC-{String(id).padStart(4,'0')}
                </span>
                <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-end' }}>
                    <div>
                        <h1 style={{ fontFamily:"'Playfair Display', serif", fontSize:'72px', fontWeight:700, lineHeight:1.05, letterSpacing:'-0.02em', color: C.onBg, marginBottom:'12px' }}>
                            Problem<br/>Assembly
                        </h1>
                        <p style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'13px', color: C.outline }}>
                            {contest?.name || '—'}
                        </p>
                    </div>
                    <div style={{ display:'flex', gap:'12px', flexShrink:0 }}>
                        <button onClick={() => navigate(`/admin/contests/${id}/edit`)}
                            style={{ display:'flex', alignItems:'center', gap:'8px', padding:'12px 20px', border:`1px solid ${C.border}`, color: C.muted, backgroundColor: C.surfaceCon, fontFamily:"'JetBrains Mono', monospace", fontSize:'11px', letterSpacing:'0.1em', textTransform:'uppercase', cursor:'pointer', transition:'all 0.2s' }}
                            onMouseEnter={e => { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; }}
                            onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}
                        >
                            ← Edit Contest
                        </button>
                        <button onClick={() => setBrowseModalOpen(true)}
                            style={{ display:'flex', alignItems:'center', gap:'8px', padding:'12px 24px', border:`1px solid ${C.secondary}`, color: C.secondary, backgroundColor: C.surfaceCon, fontFamily:"'JetBrains Mono', monospace", fontSize:'11px', letterSpacing:'0.1em', textTransform:'uppercase', cursor:'pointer', transition:'all 0.2s' }}
                            onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                            onMouseLeave={e => { e.currentTarget.style.backgroundColor = C.surfaceCon; e.currentTarget.style.color = C.secondary; }}
                        >
                            Browse Existing
                        </button>
                        <button onClick={() => navigate(`/admin/contests/${id}/problems/add`)}
                            style={{ display:'flex', alignItems:'center', gap:'8px', padding:'12px 24px', border:`1px solid ${C.secondary}`, color: C.secondary, backgroundColor: C.surfaceCon, fontFamily:"'JetBrains Mono', monospace", fontSize:'11px', letterSpacing:'0.1em', textTransform:'uppercase', cursor:'pointer', transition:'all 0.2s' }}
                            onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                            onMouseLeave={e => { e.currentTarget.style.backgroundColor = C.surfaceCon; e.currentTarget.style.color = C.secondary; }}
                        >
                            + Add Problem
                        </button>
                    </div>
                </div>
            </header>

            {/* ── Stats Strip ── */}
            <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:'1px', backgroundColor: C.border, borderBottom:`1px solid ${C.border}` }}>
                {[
                    { icon:'{}', label:'Total Problems', value: String(problems.length).padStart(2,'0') },
                    { icon:'⚡', label:'Contest Name',   value: contest?.name || '—', large: true },
                    { icon:'⚙', label:'System Health',  value:'OPTIMAL' },
                ].map(({ icon, label, value, large }) => (
                    <div key={label} style={{ backgroundColor: C.surfaceCon, padding:'1.5rem 2rem', display:'flex', alignItems:'center', gap:'1.5rem' }}>
                        <div style={{ width:'48px', height:'48px', borderRadius:'50%', border:`1px solid ${C.border}`, backgroundColor: C.surfaceMin, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                            <span style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'18px', color: C.primary }}>{icon}</span>
                        </div>
                        <div>
                            <p style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'10px', letterSpacing:'0.15em', color: C.outline, textTransform:'uppercase', marginBottom:'4px' }}>{label}</p>
                            <p style={{ fontFamily: large ? "'Geist', sans-serif" : "'Playfair Display', serif", fontSize: large ? '22px' : '32px', fontWeight: large ? 400 : 300, color: C.onBg, lineHeight:1 }}>{value}</p>
                        </div>
                    </div>
                ))}
            </div>

            {/* ── Roster Label ── */}
            <div style={{ padding:'24px 64px 0', borderBottom:`1px solid ${C.border}` }}>
                <span style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'10px', letterSpacing:'0.2em', color: C.outline, textTransform:'uppercase' }}>
                    Current Roster — {problems.length} Problems
                </span>
            </div>

            {/* ── Problems List ── */}
            <div style={{ padding:'24px 64px 48px' }}>
                {problems.length === 0 ? (
                    <div style={{ border:`1px solid ${C.border}`, padding:'4rem', textAlign:'center', fontFamily:"'JetBrains Mono', monospace", fontSize:'13px', color: C.outline, marginTop:'16px' }}>
                        No problems yet. Click "+ Add Problem" to begin.
                    </div>
                ) : (
                    <div style={{ display:'flex', flexDirection:'column', gap:'1px', backgroundColor: C.border, marginTop:'16px' }}>
                        {problems.map((problem, i) => {
                            const lc = LEVEL_CFG[problem.level] || LEVEL_CFG.MEDIUM;
                            return (
                                <motion.div key={problem.id}
                                    initial={{ opacity:0, x:-8 }} animate={{ opacity:1, x:0 }} transition={{ delay: i * 0.04 }}
                                    style={{ backgroundColor: C.surfaceCon, padding:'1.25rem 2rem', display:'flex', alignItems:'center', gap:'1.5rem', opacity: problem.active ? 1 : 0.55 }}
                                >
                                    {/* Letter */}
                                    <span style={{ fontFamily:"'Playfair Display', serif", fontSize:'20px', fontWeight:700, color: C.primary, flexShrink:0, minWidth:'32px' }}>
                                        {String.fromCharCode(65 + i)}.
                                    </span>
                                    {/* Info */}
                                    <div style={{ flex:1 }}>
                                        <div style={{ display:'flex', alignItems:'center', gap:'10px', marginBottom:'6px' }}>
                                            <span style={{ fontFamily:"'Geist', sans-serif", fontSize:'17px', fontWeight:500, color: C.onBg }}>{problem.title}</span>
                                            {/* Active badge */}
                                            <span style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'9px', letterSpacing:'0.12em', color: problem.active ? C.success : C.outline, border:`1px solid ${problem.active ? C.success : C.border}`, backgroundColor: problem.active ? 'rgba(102,187,106,0.1)' : 'transparent', padding:'2px 8px', textTransform:'uppercase' }}>
                                                {problem.active ? 'Active' : 'Disabled'}
                                            </span>
                                            {/* Level badge */}
                                            <span style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'9px', letterSpacing:'0.12em', color: lc.color, border:`1px solid ${lc.border}`, backgroundColor: lc.bg, padding:'2px 8px', textTransform:'uppercase' }}>
                                                {problem.level || 'MEDIUM'}
                                            </span>
                                        </div>
                                        <span style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'11px', color: C.outline }}>
                                            ID: PRB-{String(problem.id).padStart(3,'0')} &nbsp;•&nbsp; {problem.timeLimit}ms &nbsp;•&nbsp; {problem.memoryLimit}MB
                                        </span>
                                    </div>
                                    {/* Actions */}
                                    <div style={{ display:'flex', alignItems:'center', gap:'20px', flexShrink:0 }}>
                                        <ActionBtn icon="edit" label="Edit" color={C.outline} hoverColor={C.secondary} onClick={() => navigate(`/admin/problems/${problem.id}/edit`)} />
                                        <ActionBtn icon={problem.active ? 'block' : 'check_circle'} label={problem.active ? 'Disable' : 'Enable'} color={C.outline} hoverColor={problem.active ? C.error : C.success} onClick={() => handleToggleActive(problem)} />
                                        <ActionBtn icon="close" label="Remove" color={C.outline} hoverColor={C.error} onClick={() => setDelModal({ show:true, problemId:problem.id, problemTitle:problem.title })} />
                                    </div>
                                </motion.div>
                            );
                        })}
                    </div>
                )}
            </div>

            {/* ── Delete Modal ── */}
            <AnimatePresence>
                {delModal.show && (
                    <motion.div initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}}
                        style={{ position:'fixed', inset:0, zIndex:70, display:'flex', alignItems:'center', justifyContent:'center', backgroundColor:'rgba(0,0,0,0.7)', backdropFilter:'blur(8px)' }}
                    >
                        <motion.div initial={{scale:0.95,y:16}} animate={{scale:1,y:0}}
                            style={{ backgroundColor: C.surfaceCon, border:`1px solid ${C.error}`, maxWidth:'420px', width:'100%', padding:'2.5rem', position:'relative' }}
                        >
                            <div style={{ position:'absolute', top:0, left:0, right:0, height:'2px', backgroundColor: C.error }} />
                            <h3 style={{ fontFamily:"'Playfair Display', serif", fontSize:'24px', color: C.error, marginBottom:'1rem' }}>Detach Problem</h3>
                            <p style={{ fontFamily:"'Geist', sans-serif", fontSize:'14px', color: C.muted, lineHeight:1.6, marginBottom:'2rem' }}>
                                Detach <span style={{ fontFamily:"'JetBrains Mono', monospace", color: C.onBg }}>{delModal.problemTitle}</span> from this contest? The problem will remain available in other contests and the standalone pool.
                            </p>
                            <div style={{ display:'flex', justifyContent:'flex-end', gap:'16px' }}>
                                <button onClick={() => setDelModal({show:false})}
                                    style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'11px', letterSpacing:'0.1em', textTransform:'uppercase', color: C.outline, background:'none', border:'none', cursor:'pointer', padding:'10px 20px', transition:'color 0.2s' }}
                                    onMouseEnter={e=>e.currentTarget.style.color=C.secondary} onMouseLeave={e=>e.currentTarget.style.color=C.outline}
                                >Cancel</button>
                                <button onClick={confirmDelete}
                                    style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'11px', letterSpacing:'0.1em', textTransform:'uppercase', border:`1px solid ${C.error}`, color: C.error, backgroundColor:'transparent', padding:'10px 24px', cursor:'pointer', transition:'all 0.2s' }}
                                    onMouseEnter={e=>{e.currentTarget.style.backgroundColor=C.error;e.currentTarget.style.color=C.bg;}}
                                    onMouseLeave={e=>{e.currentTarget.style.backgroundColor='transparent';e.currentTarget.style.color=C.error;}}
                                >Detach</button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* ── Browse Existing Modal ── */}
            <AnimatePresence>
                {browseModalOpen && (
                    <motion.div initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}}
                        style={{ position:'fixed', inset:0, zIndex:70, display:'flex', alignItems:'center', justifyContent:'center', backgroundColor:'rgba(0,0,0,0.7)', backdropFilter:'blur(8px)', padding:'24px' }}
                    >
                        <motion.div initial={{scale:0.95,y:16}} animate={{scale:1,y:0}}
                            style={{ backgroundColor: C.surfaceCon, border:`1px solid ${C.border}`, maxWidth:'860px', width:'100%', maxHeight:'85vh', display:'flex', flexDirection:'column', position:'relative' }}
                        >
                            <div style={{ position:'absolute', top:0, left:0, right:0, height:'2px', backgroundColor: C.secondary }} />

                            {/* Header */}
                            <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'1.75rem 2rem 1rem', borderBottom:`1px solid ${C.border}` }}>
                                <div>
                                    <span style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'10px', letterSpacing:'0.2em', color: C.secondary, textTransform:'uppercase', display:'block', marginBottom:'6px' }}>
                                        Pool Selection
                                    </span>
                                    <h3 style={{ fontFamily:"'Playfair Display', serif", fontSize:'26px', fontWeight:600, color: C.onBg, lineHeight:1.1 }}>Browse Existing Problems</h3>
                                </div>
                                <button onClick={closeBrowseModal}
                                    style={{ background:'none', border:`1px solid ${C.border}`, color: C.outline, cursor:'pointer', width:'36px', height:'36px', display:'flex', alignItems:'center', justifyContent:'center', transition:'all 0.2s' }}
                                    onMouseEnter={e => { e.currentTarget.style.borderColor = C.error; e.currentTarget.style.color = C.error; }}
                                    onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.outline; }}
                                    aria-label="Close"
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize:'18px' }}>close</span>
                                </button>
                            </div>

                            {/* Filter row */}
                            <div style={{ padding:'1rem 2rem', borderBottom:`1px solid ${C.border}`, display:'flex', flexDirection:'column', gap:'12px' }}>
                                <input
                                    type="text"
                                    value={browseSearch}
                                    onChange={e => { setBrowseSearch(e.target.value); setBrowsePage(0); }}
                                    placeholder="Search problems..."
                                    style={{ width:'100%', backgroundColor: C.surfaceMin, border:`1px solid ${C.border}`, color: C.onBg, padding:'10px 14px', fontFamily:"'JetBrains Mono', monospace", fontSize:'12px', letterSpacing:'0.05em', outline:'none' }}
                                />
                                <div style={{ display:'flex', gap:'8px', flexWrap:'wrap' }}>
                                    {['ALL','EASY','MEDIUM','HARD'].map(lvl => {
                                        const active = browseLevel === lvl;
                                        const cfg = LEVEL_CFG[lvl];
                                        const activeColor = cfg ? cfg.color : C.secondary;
                                        const activeBorder = cfg ? cfg.border : C.secondary;
                                        const activeBg = cfg ? cfg.bg : 'rgba(233,193,118,0.12)';
                                        return (
                                            <button key={lvl} onClick={() => { setBrowseLevel(lvl); setBrowsePage(0); }}
                                                style={{
                                                    fontFamily:"'JetBrains Mono', monospace",
                                                    fontSize:'10px',
                                                    letterSpacing:'0.15em',
                                                    textTransform:'uppercase',
                                                    padding:'6px 14px',
                                                    cursor:'pointer',
                                                    transition:'all 0.2s',
                                                    border: active ? `1px solid ${activeBorder}` : `1px solid ${C.border}`,
                                                    color: active ? activeColor : C.outline,
                                                    backgroundColor: active ? activeBg : 'transparent',
                                                }}
                                            >
                                                {lvl}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>

                            {/* Card grid */}
                            <div style={{ flex:1, overflowY:'auto', padding:'1.25rem 1.5rem' }}>
                                {browseLoading ? (
                                    <div style={{ padding:'3rem', textAlign:'center', fontFamily:"'JetBrains Mono', monospace", fontSize:'12px', color: C.outline }}>
                                        Loading...
                                    </div>
                                ) : available.length === 0 ? (
                                    <div style={{ padding:'3rem', textAlign:'center', fontFamily:"'JetBrains Mono', monospace", fontSize:'12px', color: C.outline }}>
                                        No available problems match your filters.
                                    </div>
                                ) : (
                                    <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill, minmax(260px, 1fr))', gap:'14px' }}>
                                        {available.map(p => {
                                            const lc = LEVEL_CFG[p.level] || LEVEL_CFG.MEDIUM;
                                            const checked = selectedIds.has(p.id);
                                            return (
                                                <BrowseProblemCard
                                                    key={p.id}
                                                    problem={p}
                                                    lc={lc}
                                                    checked={checked}
                                                    onToggle={() => toggleSelected(p.id)}
                                                />
                                            );
                                        })}
                                    </div>
                                )}
                            </div>

                            {/* Footer */}
                            <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'1rem 2rem', borderTop:`1px solid ${C.border}`, backgroundColor: C.surfaceLow, flexWrap:'wrap', gap:'8px' }}>
                                {/* Left: selected count + pagination */}
                                <div style={{ display:'flex', alignItems:'center', gap:'16px' }}>
                                    <span style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'11px', color: C.outline, letterSpacing:'0.1em', textTransform:'uppercase' }}>
                                        {selectedIds.size} selected
                                    </span>
                                    {browseTotalPages > 1 && (
                                        <div style={{ display:'flex', alignItems:'center', gap:'6px' }}>
                                            <button
                                                disabled={browsePage === 0}
                                                onClick={() => setBrowsePage(p => Math.max(0, p - 1))}
                                                style={{ background:'none', border:`1px solid ${browsePage === 0 ? C.border : C.outline}`, color: browsePage === 0 ? C.border : C.outline, cursor: browsePage === 0 ? 'not-allowed' : 'pointer', width:'28px', height:'28px', display:'flex', alignItems:'center', justifyContent:'center', transition:'all 0.2s' }}
                                                onMouseEnter={e => { if (browsePage > 0) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                                                onMouseLeave={e => { e.currentTarget.style.borderColor = browsePage === 0 ? C.border : C.outline; e.currentTarget.style.color = browsePage === 0 ? C.border : C.outline; }}
                                            >
                                                <span className="material-symbols-outlined" style={{ fontSize:'14px' }}>chevron_left</span>
                                            </button>
                                            <span style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'10px', color: C.outline, letterSpacing:'0.08em' }}>
                                                {browsePage + 1} / {browseTotalPages}
                                            </span>
                                            <button
                                                disabled={browsePage >= browseTotalPages - 1}
                                                onClick={() => setBrowsePage(p => Math.min(browseTotalPages - 1, p + 1))}
                                                style={{ background:'none', border:`1px solid ${browsePage >= browseTotalPages - 1 ? C.border : C.outline}`, color: browsePage >= browseTotalPages - 1 ? C.border : C.outline, cursor: browsePage >= browseTotalPages - 1 ? 'not-allowed' : 'pointer', width:'28px', height:'28px', display:'flex', alignItems:'center', justifyContent:'center', transition:'all 0.2s' }}
                                                onMouseEnter={e => { if (browsePage < browseTotalPages - 1) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                                                onMouseLeave={e => { e.currentTarget.style.borderColor = browsePage >= browseTotalPages - 1 ? C.border : C.outline; e.currentTarget.style.color = browsePage >= browseTotalPages - 1 ? C.border : C.outline; }}
                                            >
                                                <span className="material-symbols-outlined" style={{ fontSize:'14px' }}>chevron_right</span>
                                            </button>
                                            <span style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'10px', color: C.border, letterSpacing:'0.05em' }}>
                                                {browseTotalElements} total
                                            </span>
                                        </div>
                                    )}
                                </div>
                                <div style={{ display:'flex', gap:'12px' }}>
                                    <button onClick={closeBrowseModal}
                                        style={{ fontFamily:"'JetBrains Mono', monospace", fontSize:'11px', letterSpacing:'0.1em', textTransform:'uppercase', color: C.outline, background:'none', border:'none', cursor:'pointer', padding:'10px 20px', transition:'color 0.2s' }}
                                        onMouseEnter={e=>e.currentTarget.style.color=C.secondary} onMouseLeave={e=>e.currentTarget.style.color=C.outline}
                                    >Cancel</button>
                                    <button
                                        disabled={selectedIds.size === 0}
                                        onClick={async () => {
                                            const ids = Array.from(selectedIds);
                                            const total = ids.length;
                                            if (total === 0) return;
                                            const results = await Promise.allSettled(
                                                ids.map(pid => api.post(`/admin/contests/${id}/problems/${pid}`))
                                            );
                                            const success = results.filter(r => r.status === 'fulfilled').length;
                                            const failed = total - success;
                                            if (failed === 0) {
                                                showToast(`Attached ${success} problem(s).`);
                                            } else if (success === 0) {
                                                showToast('Failed to attach problems.', 'error');
                                            } else {
                                                showToast(`Attached ${success} of ${total}. ${failed} failed.`);
                                            }
                                            if (success > 0) {
                                                closeBrowseModal();
                                                load();
                                            }
                                        }}
                                        style={{
                                            fontFamily:"'JetBrains Mono', monospace",
                                            fontSize:'11px',
                                            letterSpacing:'0.1em',
                                            textTransform:'uppercase',
                                            border: `1px solid ${selectedIds.size === 0 ? C.border : C.secondary}`,
                                            color: selectedIds.size === 0 ? C.outline : C.secondary,
                                            backgroundColor:'transparent',
                                            padding:'10px 24px',
                                            cursor: selectedIds.size === 0 ? 'not-allowed' : 'pointer',
                                            opacity: selectedIds.size === 0 ? 0.5 : 1,
                                            transition:'all 0.2s',
                                        }}
                                        onMouseEnter={e => { if (selectedIds.size > 0) { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; } }}
                                        onMouseLeave={e => { if (selectedIds.size > 0) { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.secondary; } }}
                                    >
                                        Confirm
                                    </button>
                                </div>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* ── Toast ── */}
            <AnimatePresence>
                {toast && (
                    <motion.div initial={{opacity:0,x:40}} animate={{opacity:1,x:0}} exit={{opacity:0,x:40}}
                        style={{ position:'fixed', bottom:'2rem', right:'2rem', backgroundColor: C.surfaceLow, borderLeft:`2px solid ${toast.type==='success'?C.secondary:C.error}`, padding:'1rem 1.5rem', zIndex:100, fontFamily:"'JetBrains Mono', monospace", fontSize:'12px', color: toast.type==='success'?C.secondary:C.error, letterSpacing:'0.05em' }}
                    >{toast.msg}</motion.div>
                )}
            </AnimatePresence>

            <style>{`.material-symbols-outlined{font-variation-settings:'FILL' 0,'wght' 300;}`}</style>
        </div>
    );
}

/* ── Browse Problem Card (Practice-style, with checkbox + Add toggle) ── */
const BrowseProblemCard = ({ problem: p, lc, checked, onToggle }) => {
    const [hovered, setHovered] = useState(false);
    return (
        <div
            onClick={onToggle}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                border: `1px solid ${checked ? lc.color : hovered ? lc.color : C.border}`,
                backgroundColor: checked ? C.surfaceHi : hovered ? C.surfaceCon : C.surfaceLow,
                padding: '1.25rem',
                display: 'flex', flexDirection: 'column', gap: '10px',
                cursor: 'pointer',
                transition: 'all 0.2s',
                position: 'relative',
                overflow: 'hidden',
            }}
        >
            {/* Top accent bar */}
            <div style={{ position:'absolute', top:0, left:0, right:0, height:'2px', backgroundColor: lc.color, opacity: checked || hovered ? 1 : 0.3, transition:'opacity 0.2s' }} />

            {/* Difficulty + checkbox */}
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                <span style={{ padding:'3px 10px', border:`1px solid ${lc.color}`, fontFamily:"'JetBrains Mono', monospace", fontSize:'9px', letterSpacing:'0.12em', color: lc.color, textTransform:'uppercase' }}>
                    {p.level || 'MEDIUM'}
                </span>
                <div style={{
                    width:'20px', height:'20px', border:`2px solid ${checked ? lc.color : C.border}`,
                    backgroundColor: checked ? lc.color : 'transparent',
                    display:'flex', alignItems:'center', justifyContent:'center',
                    transition:'all 0.2s', flexShrink:0,
                }}>
                    {checked && <span className="material-symbols-outlined" style={{ fontSize:'14px', color: C.bg, fontVariationSettings:"'FILL' 1" }}>check</span>}
                </div>
            </div>

            {/* Title */}
            <h4 style={{ fontFamily:"'Playfair Display', serif", fontSize:'16px', fontWeight:600, color: checked || hovered ? C.primary : C.onBg, margin:0, lineHeight:1.3, transition:'color 0.2s' }}>
                {p.title}
            </h4>

            {/* Description preview */}
            <p style={{ fontFamily:"'Geist', sans-serif", fontSize:'12px', color: C.outline, margin:0, lineHeight:1.5, display:'-webkit-box', WebkitLineClamp:2, WebkitBoxOrient:'vertical', overflow:'hidden' }}>
                {p.description}
            </p>

            {/* Meta */}
            <div style={{ display:'flex', gap:'10px', fontFamily:"'JetBrains Mono', monospace", fontSize:'10px', color: C.outline, marginTop:'2px' }}>
                <span>⏱ {p.timeLimit}s</span>
                <span>·</span>
                <span>💾 {p.memoryLimit}MB</span>
                <span style={{ marginLeft:'auto', fontFamily:"'JetBrains Mono', monospace", fontSize:'10px', color: C.outline }}>
                    PRB-{String(p.id).padStart(3,'0')}
                </span>
            </div>

            {/* Add / Added indicator */}
            <div style={{
                marginTop:'2px', padding:'8px 12px',
                border: `1px solid ${checked ? lc.color : C.secondary}`,
                backgroundColor: checked ? lc.color : hovered ? C.secondary : 'transparent',
                color: checked ? C.bg : hovered ? C.bg : C.secondary,
                fontFamily:"'JetBrains Mono', monospace", fontSize:'10px', letterSpacing:'0.12em', textTransform:'uppercase',
                display:'flex', alignItems:'center', justifyContent:'center', gap:'6px',
                transition:'all 0.2s',
            }}>
                <span className="material-symbols-outlined" style={{ fontSize:'13px', fontVariationSettings:`'FILL' ${checked ? 1 : 0}` }}>
                    {checked ? 'check_circle' : 'add_circle'}
                </span>
                {checked ? 'Selected' : 'Select'}
            </div>
        </div>
    );
};

/* ── Action button with icon + label ── */
const ActionBtn = ({ icon, label, color, hoverColor, onClick }) => {
    const [h, setH] = useState(false);
    return (
        <button onClick={onClick} onMouseEnter={() => setH(true)} onMouseLeave={() => setH(false)}
            style={{ display:'flex', alignItems:'center', gap:'4px', background:'none', border:'none', cursor:'pointer', color: h ? hoverColor : color, fontFamily:"'JetBrains Mono', monospace", fontSize:'10px', letterSpacing:'0.1em', textTransform:'uppercase', transition:'color 0.2s', padding:'4px 0' }}
        >
            <span className="material-symbols-outlined" style={{ fontSize:'15px' }}>{icon}</span>
            {label}
        </button>
    );
};
