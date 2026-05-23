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

    useEffect(() => { load(); }, [id]);

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
            await api.delete(`/admin/problems/${delModal.problemId}`);
            setDelModal({ show: false });
            showToast('Problem removed.');
            load();
        } catch { showToast('Failed to delete.', 'error'); setDelModal({ show: false }); }
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
                            <h3 style={{ fontFamily:"'Playfair Display', serif", fontSize:'24px', color: C.error, marginBottom:'1rem' }}>Remove Problem</h3>
                            <p style={{ fontFamily:"'Geist', sans-serif", fontSize:'14px', color: C.muted, lineHeight:1.6, marginBottom:'2rem' }}>
                                Permanently remove <span style={{ fontFamily:"'JetBrains Mono', monospace", color: C.onBg }}>{delModal.problemTitle}</span> from this contest?
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
                                >Delete</button>
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
