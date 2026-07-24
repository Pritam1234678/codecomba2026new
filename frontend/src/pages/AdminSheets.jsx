import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import api from '../services/api';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg: '#131313', surfaceLow: '#1c1b1b', surfaceCon: '#201f1f', surfaceHi: '#2a2a2a', surfaceMin: '#0e0e0e',
    border: 'rgba(241,188,139,0.2)', borderSolid: 'rgba(241,188,139,0.3)',
    primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1',
    success: '#4ade80', error: '#ffb4ab',
};

export default function AdminSheets() {
    const { isMobile } = useResponsive();
    const [sheets, setSheets] = useState([]);
    const [allProblems, setAllProblems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [expandedId, setExpandedId] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form, setForm] = useState({ name: '', company: '', description: '', tags: '', active: true });
    const [sheetProblems, setSheetProblems] = useState({});
    const [searchProblems, setSearchProblems] = useState('');
    const [deleteId, setDeleteId] = useState(null);

    useEffect(() => {
        Promise.all([api.get('/admin/sheets'), api.get('/problems')])
            .then(([sr, pr]) => { setSheets(sr.data || []); setAllProblems(pr.data || []); })
            .catch(() => {}).finally(() => setLoading(false));
    }, []);

    const fetchSheetProblems = async (sheetId) => {
        try {
            const r = await api.get(`/admin/sheets/${sheetId}/problems`);
            setSheetProblems(p => ({ ...p, [sheetId]: (r.data || []).map(sp => sp.problemId) }));
        } catch { }
    };

    const handleExpand = (id) => {
        if (expandedId === id) { setExpandedId(null); return; }
        setExpandedId(id);
        if (!sheetProblems[id]) fetchSheetProblems(id);
    };

    const handleSave = async () => {
        if (!form.name.trim()) return;
        if (editing) {
            await api.put(`/admin/sheets/${editing}`, form);
        } else {
            await api.post('/admin/sheets', form);
        }
        setEditing(null); setShowForm(false);
        setForm({ name: '', company: '', description: '', tags: '', active: true });
        const r = await api.get('/admin/sheets');
        setSheets(r.data || []);
    };

    const handleEdit = (s) => {
        setEditing(s.id); setShowForm(true);
        setForm({ name: s.name || '', company: s.company || '', description: s.description || '', tags: s.tags || '', active: s.active !== false });
    };

    const handleDelete = async () => {
        if (!deleteId) return;
        await api.delete(`/admin/sheets/${deleteId}`);
        setDeleteId(null); setExpandedId(null);
        const r = await api.get('/admin/sheets');
        setSheets(r.data || []);
    };

    const toggleProblem = async (sheetId, problemId, checked) => {
        if (checked) {
            await api.post(`/admin/sheets/${sheetId}/problems`, { problemIds: [problemId] });
        } else {
            await api.delete(`/admin/sheets/${sheetId}/problems/${problemId}`);
        }
        setSheetProblems(p => ({
            ...p,
            [sheetId]: checked
                ? [...(p[sheetId] || []), problemId]
                : (p[sheetId] || []).filter(id => id !== problemId)
        }));
    };

    const selectAll = async (sheetId) => {
        const ids = filteredProblems.map(p => p.id);
        await api.post(`/admin/sheets/${sheetId}/problems`, { problemIds: ids });
        setSheetProblems(p => ({ ...p, [sheetId]: [...new Set([...(p[sheetId] || []), ...ids])] }));
    };

    const clearAll = async (sheetId) => {
        const current = sheetProblems[sheetId] || [];
        for (const pid of current) {
            await api.delete(`/admin/sheets/${sheetId}/problems/${pid}`);
        }
        setSheetProblems(p => ({ ...p, [sheetId]: [] }));
    };

    const filteredProblems = allProblems.filter(p =>
        !searchProblems || p.title?.toLowerCase().includes(searchProblems.toLowerCase())
    );

    const sheetProblemList = (id) => {
        const pids = sheetProblems[id] || [];
        return allProblems.filter(p => pids.includes(p.id));
    };

    if (loading) return (
        <div style={{ backgroundColor: C.bg, minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <motion.div animate={{ rotate: 360 }} transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                style={{ width: '32px', height: '32px', borderRadius: '50%', border: `2px solid ${C.borderSolid}`, borderTopColor: C.secondary }} />
        </div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh', padding: isMobile ? '24px 16px' : '48px 64px' }}>
            <motion.header initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} style={{ marginBottom: '40px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '16px' }}>
                <div>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase', display: 'block', marginBottom: '8px' }}>Admin · Content</span>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: C.primary, margin: 0 }}>Practice Sheets</h1>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, marginTop: '6px', display: 'block' }}>{sheets.length} sheet{sheets.length !== 1 ? 's' : ''}</span>
                </div>
                <button onClick={() => { setEditing(null); setForm({ name: '', company: '', description: '', tags: '', active: true }); setShowForm(true); }}
                    style={{ padding: '12px 24px', border: `1px solid ${C.secondary}`, backgroundColor: C.secondary, color: C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s', display: 'flex', alignItems: 'center', gap: '8px' }}
                    onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.primary; e.currentTarget.style.borderColor = C.primary; }}
                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.borderColor = C.secondary; }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>add</span> New Sheet
                </button>
            </motion.header>

            {/* Sheet list */}
            {sheets.length === 0 ? (
                <div style={{ border: `1px solid ${C.border}`, padding: '4rem', textAlign: 'center', backgroundColor: C.surfaceLow }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '40px', color: C.border, display: 'block', marginBottom: '16px' }}>dataset</span>
                    <p style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', color: C.muted, margin: '0 0 8px' }}>No sheets yet</p>
                    <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.outline, margin: '0 0 20px' }}>Create your first practice sheet to organize problems.</p>
                    <button onClick={() => { setEditing(null); setForm({ name: '', company: '', description: '', tags: '', active: true }); setShowForm(true); }}
                        style={{ padding: '10px 24px', border: `1px solid ${C.secondary}`, color: C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer' }}>Create Sheet</button>
                </div>
            ) : (
                sheets.map((sheet, i) => {
                    const isExpanded = expandedId === sheet.id;
                    const pids = sheetProblems[sheet.id] || [];
                    const problemCount = pids.length || (sheet._problemCount ?? '—');

                    return (
                        <motion.div key={sheet.id} initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.06 }}
                            style={{ marginBottom: '16px', border: `1px solid ${isExpanded ? C.borderSolid : C.border}`, backgroundColor: C.surfaceLow, overflow: 'hidden', transition: 'border-color 0.2s' }}>
                            {/* Header */}
                            <div onClick={() => handleExpand(sheet.id)}
                                style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer', transition: 'background-color 0.15s' }}
                                onMouseEnter={e => e.currentTarget.style.backgroundColor = C.surfaceCon}
                                onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flex: 1, minWidth: 0 }}>
                                    <div style={{ width: '44px', height: '44px', flexShrink: 0, border: `1px solid ${C.borderSolid}`, backgroundColor: `${C.primary}08`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 700, color: C.secondary }}>
                                        {(sheet.company || sheet.name || 'S')[0].toUpperCase()}
                                    </div>
                                    <div style={{ minWidth: 0 }}>
                                        <div style={{ display: 'flex', alignItems: 'baseline', gap: '10px', flexWrap: 'wrap' }}>
                                            <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '16px', fontWeight: 600, color: C.primary }}>{sheet.name}</span>
                                            {sheet.company && <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', color: C.outline }}>{sheet.company}</span>}
                                            {!sheet.active && <span style={{ padding: '2px 8px', border: `1px solid ${C.error}30`, fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.1em', color: C.error, textTransform: 'uppercase' }}>Inactive</span>}
                                        </div>
                                        <div style={{ display: 'flex', gap: '20px', marginTop: '6px' }}>
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>{problemCount} problems</span>
                                            {sheet.tags && <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>{sheet.tags.split(',').length} tags</span>}
                                        </div>
                                    </div>
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexShrink: 0 }}>
                                    <button onClick={e => { e.stopPropagation(); handleEdit(sheet); }}
                                        style={{ padding: '6px 14px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', cursor: 'pointer', transition: 'all 0.15s' }}
                                        onMouseEnter={ev => { ev.currentTarget.style.borderColor = C.secondary; ev.currentTarget.style.color = C.secondary; }}
                                        onMouseLeave={ev => { ev.currentTarget.style.borderColor = C.border; ev.currentTarget.style.color = C.muted; }}>Edit</button>
                                    <button onClick={e => { e.stopPropagation(); setDeleteId(sheet.id); }}
                                        style={{ padding: '6px 14px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', cursor: 'pointer', transition: 'all 0.15s' }}
                                        onMouseEnter={ev => { ev.currentTarget.style.borderColor = C.error; ev.currentTarget.style.color = C.error; }}
                                        onMouseLeave={ev => { ev.currentTarget.style.borderColor = C.border; ev.currentTarget.style.color = C.muted; }}>Delete</button>
                                    <span className="material-symbols-outlined" style={{ fontSize: '20px', color: C.outline, transition: 'transform 0.2s', transform: isExpanded ? 'rotate(180deg)' : 'none', marginLeft: '4px' }}>expand_more</span>
                                </div>
                            </div>

                            {/* Expanded panel */}
                            <AnimatePresence>
                                {isExpanded && (
                                    <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} transition={{ duration: 0.2 }}
                                        style={{ borderTop: `1px solid ${C.border}`, overflow: 'hidden' }}>
                                        <div style={{ padding: '20px 24px' }}>
                                            {/* Toolbar */}
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', gap: '12px', flexWrap: 'wrap' }}>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', borderBottom: `1px solid ${C.border}`, paddingBottom: '6px', flex: 1, maxWidth: '360px' }}>
                                                    <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.outline }}>search</span>
                                                    <input value={searchProblems} onChange={e => setSearchProblems(e.target.value)} placeholder="Filter problems..."
                                                        style={{ background: 'none', border: 'none', outline: 'none', color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', width: '100%' }} />
                                                </div>
                                                <div style={{ display: 'flex', gap: '8px' }}>
                                                    <button onClick={() => selectAll(sheet.id)}
                                                        style={{ padding: '6px 14px', border: `1px solid ${C.secondary}`, backgroundColor: 'transparent', color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.15s' }}
                                                        onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                                                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.secondary; }}>Select All</button>
                                                    <button onClick={() => clearAll(sheet.id)}
                                                        style={{ padding: '6px 14px', border: `1px solid ${C.error}`, backgroundColor: 'transparent', color: C.error, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.15s' }}
                                                        onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.error; e.currentTarget.style.color = C.bg; }}
                                                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.error; }}>Clear All</button>
                                                </div>
                                            </div>

                                            {/* Current problems in sheet */}
                                            {sheetProblemList(sheet.id).length > 0 && (
                                                <div style={{ marginBottom: '16px' }}>
                                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '8px' }}>
                                                        In this sheet ({sheetProblemList(sheet.id).length})
                                                    </span>
                                                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                                                        {sheetProblemList(sheet.id).map(p => (
                                                            <span key={p.id} style={{ padding: '4px 10px', border: `1px solid ${C.borderSolid}`, backgroundColor: C.surfaceCon, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.onBg, borderRadius: '2px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                                                {p.title}
                                                                <button onClick={() => toggleProblem(sheet.id, p.id, false)}
                                                                    style={{ background: 'none', border: 'none', color: C.outline, cursor: 'pointer', padding: '0', lineHeight: 1 }}
                                                                    onMouseEnter={e => e.currentTarget.style.color = C.error}
                                                                    onMouseLeave={e => e.currentTarget.style.color = C.outline}>
                                                                    <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>close</span>
                                                                </button>
                                                            </span>
                                                        ))}
                                                    </div>
                                                </div>
                                            )}

                                            {/* Available problems — card grid */}
                                            <div>
                                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '12px' }}>
                                                    Available problems ({filteredProblems.length})
                                                </span>
                                                {filteredProblems.length === 0 ? (
                                                    <div style={{ padding: '32px', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin }}>No problems match</div>
                                                ) : (
                                                    <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(auto-fill, minmax(280px, 1fr))', gap: '10px' }}>
                                                        {filteredProblems.map((p, idx) => {
                                                            const isInSheet = (sheetProblems[sheet.id] || []).includes(p.id);
                                                            const diffColor = p.level === 'EASY' ? C.success : p.level === 'MEDIUM' ? C.secondary : C.error;
                                                            const randomHeight = 100 + ((p.id * 7) % 60);
                                                            return (
                                                                <motion.div
                                                                    key={p.id}
                                                                    initial={{ opacity: 0, scale: 0.92, y: 12 }}
                                                                    animate={{ opacity: 1, scale: 1, y: 0 }}
                                                                    transition={{ duration: 0.3, delay: idx * 0.02 }}
                                                                    whileHover={{ y: -3, boxShadow: `0 8px 24px rgba(241,188,139,0.08)` }}
                                                                    onClick={() => toggleProblem(sheet.id, p.id, !isInSheet)}
                                                                    style={{
                                                                        cursor: 'pointer',
                                                                        border: isInSheet ? `1px solid ${C.success}40` : `1px solid ${C.border}`,
                                                                        backgroundColor: isInSheet ? `${C.success}05` : C.surfaceMin,
                                                                        padding: '16px',
                                                                        display: 'flex', flexDirection: 'column', gap: '10px',
                                                                        transition: 'all 0.2s',
                                                                        position: 'relative', overflow: 'hidden',
                                                                        minHeight: `${randomHeight}px`,
                                                                    }}>
                                                                    {/* Left accent */}
                                                                    <div style={{ position: 'absolute', left: 0, top: 0, bottom: 0, width: '3px', backgroundColor: isInSheet ? C.success : diffColor, opacity: 0.5 }} />

                                                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px' }}>
                                                                        <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', fontWeight: 500, color: isInSheet ? C.success : C.onBg, lineHeight: 1.4, flex: 1 }}>
                                                                            {p.title}
                                                                        </span>
                                                                        <motion.div
                                                                            animate={{ scale: isInSheet ? [1, 1.2, 1] : 1 }}
                                                                            transition={{ duration: 0.3 }}
                                                                            style={{ flexShrink: 0, marginTop: '2px' }}>
                                                                            {isInSheet ? (
                                                                                <span className="material-symbols-outlined" style={{ fontSize: '18px', color: C.success, fontVariationSettings: "'FILL' 1" }}>check_circle</span>
                                                                            ) : (
                                                                                <span className="material-symbols-outlined" style={{ fontSize: '18px', color: C.border }}>add_circle</span>
                                                                            )}
                                                                        </motion.div>
                                                                    </div>

                                                                    <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginTop: 'auto' }}>
                                                                        <span style={{
                                                                            padding: '2px 8px', borderRadius: '2px',
                                                                            fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.08em',
                                                                            border: `1px solid ${diffColor}30`, color: diffColor, textTransform: 'uppercase',
                                                                        }}>
                                                                            {p.level}
                                                                        </span>
                                                                        {p.topics?.split(',')[0]?.trim() && (
                                                                            <span style={{ padding: '2px 8px', borderRadius: '2px', fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', color: C.outline, border: `1px solid ${C.border}` }}>
                                                                                {p.topics.split(',')[0].trim()}
                                                                            </span>
                                                                        )}
                                                                        {p.topics?.split(',')[1]?.trim() && (
                                                                            <span style={{ padding: '2px 8px', borderRadius: '2px', fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', color: C.outline, border: `1px solid ${C.border}` }}>
                                                                                {p.topics.split(',')[1].trim()}
                                                                            </span>
                                                                        )}
                                                                    </div>
                                                                </motion.div>
                                                            );
                                                        })}
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </motion.div>
                    );
                })
            )}

            {/* Form Modal */}
            <AnimatePresence>
                {showForm && (
                    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        style={{ position: 'fixed', inset: 0, zIndex: 1000, backgroundColor: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                        onClick={() => setShowForm(false)}>
                        <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }}
                            style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.borderSolid}`, padding: '32px', maxWidth: '520px', width: '90%', borderRadius: '4px', boxShadow: '0 24px 64px rgba(0,0,0,0.5)' }}
                            onClick={e => e.stopPropagation()}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                                <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 600, color: C.primary, margin: 0 }}>{editing ? 'Edit Sheet' : 'New Sheet'}</h3>
                                <button onClick={() => setShowForm(false)} style={{ background: 'none', border: 'none', color: C.outline, cursor: 'pointer' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>close</span>
                                </button>
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                                <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="Sheet Name *"
                                    style={{ padding: '12px 14px', border: `1px solid ${C.borderSolid}`, backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '14px', outline: 'none' }} />
                                <input value={form.company} onChange={e => setForm({ ...form, company: e.target.value })} placeholder="Company"
                                    style={{ padding: '12px 14px', border: `1px solid ${C.borderSolid}`, backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '14px', outline: 'none' }} />
                                <textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Description" rows={3}
                                    style={{ padding: '12px 14px', border: `1px solid ${C.borderSolid}`, backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '14px', outline: 'none', resize: 'vertical' }} />
                                <input value={form.tags} onChange={e => setForm({ ...form, tags: e.target.value })} placeholder="Tags (comma-separated)"
                                    style={{ padding: '12px 14px', border: `1px solid ${C.borderSolid}`, backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '14px', outline: 'none' }} />
                                <button onClick={handleSave}
                                    style={{ padding: '12px', border: 'none', backgroundColor: C.secondary, color: C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.12em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.15s', borderRadius: '2px' }}
                                    onMouseEnter={e => e.currentTarget.style.backgroundColor = C.primary}
                                    onMouseLeave={e => e.currentTarget.style.backgroundColor = C.secondary}>
                                    {editing ? 'Update Sheet' : 'Create Sheet'}
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* Delete Confirm */}
            <AnimatePresence>
                {deleteId && (
                    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        style={{ position: 'fixed', inset: 0, zIndex: 2000, backgroundColor: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(2px)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                        onClick={() => setDeleteId(null)}>
                        <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }}
                            style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.borderSolid}`, padding: '28px 32px', maxWidth: '380px', width: '90%', borderRadius: '6px' }}
                            onClick={e => e.stopPropagation()}>
                            <div style={{ display: 'flex', gap: '14px', marginBottom: '20px' }}>
                                <div style={{ width: '40px', height: '40px', borderRadius: '50%', backgroundColor: `${C.error}15`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '20px', color: C.error }}>delete</span>
                                </div>
                                <div>
                                    <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', fontWeight: 600, color: C.onBg, margin: '0 0 4px' }}>Delete Sheet</p>
                                    <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.outline, margin: 0, lineHeight: 1.5 }}>This will remove the sheet and all its problem associations. This cannot be undone.</p>
                                </div>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                                <button onClick={() => setDeleteId(null)}
                                    style={{ padding: '8px 20px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>Cancel</button>
                                <button onClick={handleDelete}
                                    style={{ padding: '8px 24px', border: 'none', backgroundColor: C.error, color: C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>Delete</button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            <style>{`.material-symbols-outlined{font-variation-settings:'FILL'0,'wght'300}::-webkit-scrollbar{width:4px}::-webkit-scrollbar-track{background:transparent}::-webkit-scrollbar-thumb{background:#50453b;border-radius:2px}`}</style>
        </div>
    );
}
