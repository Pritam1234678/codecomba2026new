import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import api from '../services/api';
import useResponsive from '../hooks/useResponsive';

const C = { bg: '#131313', surfaceLow: '#1c1b1b', surfaceCon: '#201f1f', surfaceHi: '#2a2a2a', border: 'rgba(241,188,139,0.2)', borderSolid: 'rgba(241,188,139,0.3)', primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1', success: '#4ade80', error: '#ffb4ab' };

export default function AdminSheets() {
    const { isMobile } = useResponsive();
    const [sheets, setSheets] = useState([]);
    const [loading, setLoading] = useState(true);
    const [editing, setEditing] = useState(null);
    const [form, setForm] = useState({ name: '', company: '', description: '', tags: '', active: true });
    const [allProblems, setAllProblems] = useState([]);
    const [sheetProblems, setSheetProblems] = useState([]);
    const [selectedSheet, setSelectedSheet] = useState(null);
    const [selectAll, setSelectAll] = useState(false);

    const fetchSheets = () => {
        api.get('/admin/sheets').then(r => setSheets(r.data || [])).catch(() => {}).finally(() => setLoading(false));
    };

    useEffect(() => { fetchSheets(); }, []);
    useEffect(() => { api.get('/problems').then(r => setAllProblems(r.data || [])).catch(() => {}); }, []);

    const fetchSheetProblems = (id) => {
        api.get(`/admin/sheets/${id}/problems`).then(r => {
            setSheetProblems((r.data || []).map(sp => sp.problemId));
        }).catch(() => {});
    };

    const handleSave = async () => {
        if (!form.name.trim()) return;
        if (editing) {
            await api.put(`/admin/sheets/${editing}`, form);
        } else {
            await api.post('/admin/sheets', form);
        }
        setEditing(null);
        setForm({ name: '', company: '', description: '', tags: '', active: true });
        fetchSheets();
    };

    const handleEdit = (s) => {
        setEditing(s.id);
        setForm({ name: s.name || '', company: s.company || '', description: s.description || '', tags: s.tags || '', active: s.active !== false });
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Delete this sheet?')) return;
        await api.delete(`/admin/sheets/${id}`);
        fetchSheets();
    };

    const toggleProblem = async (sheetId, problemId, checked) => {
        if (checked) {
            await api.post(`/admin/sheets/${sheetId}/problems`, { problemIds: [problemId] });
        } else {
            await api.delete(`/admin/sheets/${sheetId}/problems/${problemId}`);
        }
        fetchSheetProblems(sheetId);
    };

    const handleSelectAll = async (sheetId) => {
        const ids = allProblems.map(p => p.id);
        await api.post(`/admin/sheets/${sheetId}/problems`, { problemIds: ids });
        fetchSheetProblems(sheetId);
    };

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh', padding: isMobile ? '24px 16px' : '48px 64px' }}>
            <motion.header initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} style={{ marginBottom: '40px' }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase', display: 'block', marginBottom: '8px' }}>Admin</span>
                <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: C.primary, margin: 0 }}>Practice Sheets</h1>
            </motion.header>

            {/* Form */}
            <div style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, padding: '24px', marginBottom: '32px', display: 'flex', flexDirection: 'column', gap: '16px', maxWidth: '600px' }}>
                <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="Sheet Name" style={{ padding: '10px 14px', border: `1px solid ${C.borderSolid}`, backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '14px', outline: 'none' }} />
                <input value={form.company} onChange={e => setForm({ ...form, company: e.target.value })} placeholder="Company" style={{ padding: '10px 14px', border: `1px solid ${C.borderSolid}`, backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '14px', outline: 'none' }} />
                <textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Description" rows={3} style={{ padding: '10px 14px', border: `1px solid ${C.borderSolid}`, backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '14px', outline: 'none', resize: 'vertical' }} />
                <input value={form.tags} onChange={e => setForm({ ...form, tags: e.target.value })} placeholder="Tags (comma-separated)" style={{ padding: '10px 14px', border: `1px solid ${C.borderSolid}`, backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '14px', outline: 'none' }} />
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                    <button onClick={handleSave} style={{ padding: '10px 28px', border: 'none', backgroundColor: C.secondary, color: C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer' }}>{editing ? 'Update' : 'Create'}</button>
                    {editing && <button onClick={() => { setEditing(null); setForm({ name: '', company: '', description: '', tags: '', active: true }); }} style={{ padding: '10px 20px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', cursor: 'pointer' }}>Cancel</button>}
                </div>
            </div>

            {/* Sheet list */}
            {loading ? <div style={{ color: C.outline }}>Loading...</div> : sheets.map(s => (
                <div key={s.id} style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, marginBottom: '16px', overflow: 'hidden' }}>
                    <div style={{ padding: '16px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer', backgroundColor: selectedSheet === s.id ? C.surfaceCon : 'transparent' }}
                        onClick={() => { setSelectedSheet(selectedSheet === s.id ? null : s.id); if (selectedSheet !== s.id) fetchSheetProblems(s.id); }}>
                        <div>
                            <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '16px', fontWeight: 600, color: C.primary }}>{s.name}</span>
                            {s.company && <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, marginLeft: '12px' }}>{s.company}</span>}
                        </div>
                        <div style={{ display: 'flex', gap: '8px' }}>
                            <button onClick={e => { e.stopPropagation(); handleEdit(s); }} style={{ padding: '6px 14px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', cursor: 'pointer' }}>Edit</button>
                            <button onClick={e => { e.stopPropagation(); handleDelete(s.id); }} style={{ padding: '6px 14px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.error, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', cursor: 'pointer' }}>Delete</button>
                        </div>
                    </div>
                    {selectedSheet === s.id && (
                        <div style={{ padding: '16px 24px', borderTop: `1px solid ${C.border}` }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, textTransform: 'uppercase' }}>Problems</span>
                                <button onClick={() => handleSelectAll(s.id)} style={{ padding: '4px 12px', border: `1px solid ${C.secondary}`, backgroundColor: 'transparent', color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.06em', textTransform: 'uppercase', cursor: 'pointer' }}>Select All</button>
                            </div>
                            <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
                                {allProblems.map(p => (
                                    <div key={p.id} style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '6px 0', borderBottom: `1px solid ${C.border}` }}>
                                        <input type="checkbox" checked={sheetProblems.includes(p.id)} onChange={e => toggleProblem(s.id, p.id, e.target.checked)}
                                            style={{ accentColor: C.secondary }} />
                                        <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.onBg }}>{p.title}</span>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, marginLeft: 'auto' }}>{p.level}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            ))}
        </div>
    );
}
