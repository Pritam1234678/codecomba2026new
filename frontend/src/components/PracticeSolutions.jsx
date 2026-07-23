import { useState, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import api from '../services/api';
import SkeletonLoader from './SkeletonLoader';

const C = {
    bg: '#131313', surfaceCon: '#201f1f', surfaceLow: '#1c1b1b', surfaceHi: '#2a2a2a',
    surfaceMin: '#0e0e0e', border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176',
    muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1', error: '#ffb4ab',
};

const LANG_MAP = {
    JAVA: { monaco: 'java' }, CPP: { monaco: 'cpp' }, C: { monaco: 'c' },
    PYTHON: { monaco: 'python' }, JAVASCRIPT: { monaco: 'javascript' },
};
const LANGS = ['JAVA', 'CPP', 'PYTHON', 'JAVASCRIPT', 'C'];

export default function PracticeSolutions({ problemId, currentUserId, onClose }) {
    const [solutions, setSolutions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [count, setCount] = useState(0);
    const [showForm, setShowForm] = useState(false);
    const [saving, setSaving] = useState(false);
    const [expandedId, setExpandedId] = useState(null);
    const [activeTab, setActiveTab] = useState('JAVA');
    const [deleteId, setDeleteId] = useState(null);

    const [codes, setCodes] = useState(Object.fromEntries(LANGS.map(l => [l, ''])));
    const [explanation, setExplanation] = useState('');
    const [imageUrl, setImageUrl] = useState('');
    const [editLang, setEditLang] = useState('JAVA');

    const [editingId, setEditingId] = useState(null);
    const [editCodes, setEditCodes] = useState(Object.fromEntries(LANGS.map(l => [l, ''])));
    const [editExplanation, setEditExplanation] = useState('');
    const [editImageUrl, setEditImageUrl] = useState('');
    const [editExpandedLang, setEditExpandedLang] = useState('JAVA');

    useEffect(() => { fetchAll(); }, [problemId]);

    const fetchAll = () => {
        setLoading(true);
        Promise.all([
            api.get(`/practice/solutions/${problemId}`),
            api.get(`/practice/solutions/${problemId}/count`),
        ]).then(([r1, r2]) => {
            setSolutions(r1.data || []);
            setCount(r2.data?.count || 0);
        }).catch(() => {}).finally(() => setLoading(false));
    };

    const handleSave = async () => {
        const map = {};
        for (const [l, c] of Object.entries(codes)) if (c.trim()) map[l] = c;
        if (!Object.keys(map).length) return;
        setSaving(true);
        try {
            await api.post('/practice/solutions', { problemId: parseInt(problemId), codes: map, explanation: explanation.trim() || null, imageUrl: imageUrl.trim() || null });
            setShowForm(false); setCodes(Object.fromEntries(LANGS.map(l => [l, '']))); setExplanation(''); setImageUrl('');
            fetchAll();
        } catch (err) { alert(err.response?.data?.error || 'Failed'); }
        finally { setSaving(false); }
    };

    const handleUpdate = async () => {
        const map = {};
        for (const [l, c] of Object.entries(editCodes)) if (c.trim()) map[l] = c;
        if (!Object.keys(map).length) return;
        setSaving(true);
        try {
            await api.put(`/practice/solutions/${editingId}`, { codes: map, explanation: editExplanation.trim() || null, imageUrl: editImageUrl.trim() || null });
            setEditingId(null); fetchAll();
        } catch (err) { alert(err.response?.data?.error || 'Failed'); }
        finally { setSaving(false); }
    };

    const handleDelete = async () => {
        if (!deleteId) return;
        try { await api.delete(`/practice/solutions/${deleteId}`); setDeleteId(null); fetchAll(); }
        catch (err) { alert(err.response?.data?.error || 'Failed'); setDeleteId(null); }
    };

    const startEdit = (sol) => {
        setEditingId(sol.id);
        const c = sol.codes || {};
        setEditCodes({ JAVA: c.JAVA || '', CPP: c.CPP || '', PYTHON: c.PYTHON || '', JAVASCRIPT: c.JAVASCRIPT || '', C: c.C || '' });
        setEditExplanation(sol.explanation || '');
        setEditImageUrl(sol.imageUrl || '');
        setEditExpandedLang(Object.keys(c).find(k => c[k]) || 'JAVA');
        setExpandedId(sol.id);
    };

    const toggleExpand = (id) => setExpandedId(expandedId === id ? null : id);

    return (
        <div style={{ position: 'fixed', inset: 0, zIndex: 1001, backgroundColor: 'rgba(0,0,0,0.85)', backdropFilter: 'blur(4px)', display: 'flex', alignItems: 'center', justifyContent: 'center' }} onClick={onClose}>
            <div style={{ width: 'min(95vw, 960px)', maxHeight: '88vh', backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, display: 'flex', flexDirection: 'column', borderRadius: '4px', boxShadow: '0 24px 80px rgba(0,0,0,0.6)' }} onClick={e => e.stopPropagation()}>
                {/* Header */}
                <div style={{ height: '52px', flexShrink: 0, borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{ width: '30px', height: '30px', borderRadius: '50%', backgroundColor: `${C.secondary}18`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.secondary }}>lightbulb</span>
                        </div>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '17px', fontWeight: 600, color: C.primary }}>Solutions</span>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>{count} solution{count !== 1 ? 's' : ''}</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <button onClick={() => setShowForm(!showForm)} style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '7px 16px', border: `1px solid ${showForm ? C.border : C.secondary}`, color: showForm ? C.muted : C.bg, backgroundColor: showForm ? 'transparent' : C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>{showForm ? 'arrow_back' : 'add'}</span>
                            {showForm ? 'Back' : 'Add'}
                        </button>
                        <button onClick={onClose} style={{ width: '30px', height: '30px', borderRadius: '50%', border: 'none', backgroundColor: 'transparent', color: C.outline, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>close</span>
                        </button>
                    </div>
                </div>

                <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
                    {showForm ? (
                        <div style={{ overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
                            <div style={{ display: 'flex', gap: '0', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceHi, padding: '0 24px' }}>
                                {LANGS.map(l => (
                                    <button key={l} onClick={() => setEditLang(l)} style={{ padding: '8px 14px', border: 'none', borderBottom: editLang === l ? `2px solid ${C.secondary}` : '2px solid transparent', backgroundColor: 'transparent', color: editLang === l ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', cursor: 'pointer' }}>{l}{codes[l]?.trim() ? ' ✓' : ''}</button>
                                ))}
                            </div>
                            <div style={{ padding: '12px 24px', height: '240px' }}>
                                <Editor height="100%" language={LANG_MAP[editLang]?.monaco || 'java'} value={codes[editLang] || ''} onChange={v => setCodes(p => ({ ...p, [editLang]: v || '' }))} theme="vs-dark" options={{ minimap: { enabled: false }, fontSize: 13, lineNumbers: 'on', scrollBeyondLastLine: false, wordWrap: 'on', padding: { top: 10 } }} loading={<SkeletonLoader compact rows={3} />} />
                            </div>
                            <div style={{ padding: '0 24px 14px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                                <textarea value={explanation} onChange={e => setExplanation(e.target.value)} placeholder="Explain your approach, time complexity..." rows={3} style={{ width: '100%', padding: '10px 14px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '12px', lineHeight: '1.6', resize: 'vertical', outline: 'none', boxSizing: 'border-box' }} />
                                <input type="text" value={imageUrl} onChange={e => setImageUrl(e.target.value)} placeholder="Image URL (optional)" style={{ padding: '8px 14px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', outline: 'none' }} />
                                <button onClick={handleSave} disabled={saving || Object.values(codes).every(c => !c.trim())} style={{ alignSelf: 'flex-end', padding: '8px 28px', border: 'none', borderRadius: '3px', backgroundColor: saving || Object.values(codes).every(c => !c.trim()) ? C.surfaceHi : C.secondary, color: saving || Object.values(codes).every(c => !c.trim()) ? C.outline : C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: saving ? 'not-allowed' : 'pointer' }}>{saving ? 'Saving...' : 'Submit'}</button>
                            </div>
                        </div>
                    ) : loading ? (
                        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><SkeletonLoader compact rows={4} /></div>
                    ) : solutions.length === 0 ? (
                        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '12px', color: C.outline }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '36px', color: C.border }}>lightbulb</span>
                            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '16px', color: C.muted }}>No solutions yet</span>
                            <button onClick={() => setShowForm(true)} style={{ padding: '8px 20px', border: `1px solid ${C.secondary}`, color: C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>Add Solution</button>
                        </div>
                    ) : (
                        <div style={{ overflowY: 'auto' }}>
                            {solutions.map(sol => {
                                const isOwner = sol.userId === currentUserId;
                                const isEditing = editingId === sol.id;
                                const isExpanded = expandedId === sol.id;
                                const codesMap = sol.codes || {};
                                const langKeys = Object.keys(codesMap).filter(l => codesMap[l]?.trim());
                                const displayLang = isEditing ? editExpandedLang : (langKeys[0] || 'JAVA');

                                return (
                                    <div key={sol.id} style={{ borderBottom: `1px solid ${C.border}` }}>
                                        {/* Compact row */}
                                        <div onClick={() => !isEditing && toggleExpand(sol.id)}
                                            style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 24px', cursor: isEditing ? 'default' : 'pointer', backgroundColor: isExpanded ? C.surfaceMin : 'transparent', transition: 'background-color 0.15s', borderLeft: isExpanded ? `3px solid ${C.secondary}` : '3px solid transparent' }}
                                            onMouseEnter={e => { if (!isExpanded) e.currentTarget.style.backgroundColor = '#181716'; }}
                                            onMouseLeave={e => { if (!isExpanded) e.currentTarget.style.backgroundColor = 'transparent'; }}
                                        >
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flex: 1, minWidth: 0 }}>
                                                <div style={{ width: '26px', height: '26px', borderRadius: '50%', backgroundColor: C.surfaceHi, border: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontFamily: "'Geist', sans-serif", fontSize: '10px', fontWeight: 700, color: C.primary }}>
                                                    {(sol.userName || 'A')[0].toUpperCase()}
                                                </div>
                                                <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', fontWeight: 500, color: C.onBg, flexShrink: 0 }}>{sol.userName || 'Anonymous'}</span>
                                                <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                                                    {isEditing ? LANGS.map(l => (
                                                        <button key={l} onClick={e => { e.stopPropagation(); setEditExpandedLang(l); }}
                                                            style={{ padding: '2px 8px', border: 'none', borderBottom: editExpandedLang === l ? `2px solid ${C.secondary}` : '2px solid transparent', backgroundColor: 'transparent', color: editExpandedLang === l ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.08em', cursor: 'pointer' }}>{l}</button>
                                                    )) : langKeys.map(l => (
                                                        <span key={l} style={{ padding: '1px 7px', borderRadius: '2px', fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.1em', color: C.secondary, textTransform: 'uppercase', backgroundColor: `${C.secondary}12`, border: `1px solid ${C.secondary}30` }}>{l}</span>
                                                    ))}
                                                </div>
                                            </div>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexShrink: 0 }}>
                                                {isOwner && !isEditing && (
                                                    <>
                                                        <button onClick={e => { e.stopPropagation(); startEdit(sol); }} style={{ padding: '3px 8px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, borderRadius: '2px', cursor: 'pointer', fontFamily: "'JetBrains Mono', monospace", fontSize: '9px' }} title="Edit"><span className="material-symbols-outlined" style={{ fontSize: '12px' }}>edit</span></button>
                                                        <button onClick={e => { e.stopPropagation(); setDeleteId(sol.id); }} style={{ padding: '3px 8px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, borderRadius: '2px', cursor: 'pointer' }} title="Delete"><span className="material-symbols-outlined" style={{ fontSize: '12px' }}>delete</span></button>
                                                    </>
                                                )}
                                                {isEditing && (
                                                    <>
                                                        <button onClick={e => { e.stopPropagation(); handleUpdate(); }} style={{ padding: '3px 10px', border: 'none', backgroundColor: C.secondary, color: C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.06em', cursor: 'pointer', borderRadius: '2px' }}>Save</button>
                                                        <button onClick={e => { e.stopPropagation(); setEditingId(null); setExpandedId(null); }} style={{ padding: '3px 10px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', cursor: 'pointer', borderRadius: '2px' }}>Cancel</button>
                                                    </>
                                                )}
                                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, minWidth: '70px', textAlign: 'right' }}>
                                                    {new Date(sol.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                                                </span>
                                                <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.outline, transition: 'transform 0.2s', transform: isExpanded ? 'rotate(180deg)' : 'none' }}>
                                                    expand_more
                                                </span>
                                            </div>
                                        </div>

                                        {/* Expanded detail */}
                                        {isExpanded && (
                                            <div style={{ borderTop: `1px solid ${C.border}`, backgroundColor: C.bg }}>
                                                {isEditing ? (
                                                    <div style={{ padding: '10px 24px' }}>
                                                        <div style={{ height: '200px' }}>
                                                            <Editor height="100%" language={LANG_MAP[editExpandedLang]?.monaco || 'java'} value={editCodes[editExpandedLang] || ''} onChange={v => setEditCodes(p => ({ ...p, [editExpandedLang]: v || '' }))} theme="vs-dark" options={{ readOnly: false, minimap: { enabled: false }, fontSize: 12, lineNumbers: 'on', scrollBeyondLastLine: false, wordWrap: 'on', padding: { top: 8, bottom: 8 } }} loading={<div style={{ height: '100%', backgroundColor: '#0a0a0a' }} />} />
                                                        </div>
                                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', paddingTop: '10px' }}>
                                                            <textarea value={editExplanation} onChange={e => setEditExplanation(e.target.value)} placeholder="Explain your approach..." rows={2} style={{ padding: '8px 12px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '11px', lineHeight: '1.5', resize: 'vertical', outline: 'none' }} />
                                                            <input type="text" value={editImageUrl} onChange={e => setEditImageUrl(e.target.value)} placeholder="Image URL (optional)" style={{ padding: '7px 12px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', outline: 'none' }} />
                                                        </div>
                                                    </div>
                                                ) : (
                                                    <div style={{ padding: '10px 0' }}>
                                                        {langKeys.length > 0 && (
                                                            <div style={{ display: 'flex', gap: '0', borderBottom: `1px solid ${C.border}`, padding: '0 24px' }}>
                                                                {langKeys.map(l => (
                                                                    <button key={l} onClick={() => setActiveTab(l)} style={{ padding: '6px 12px', border: 'none', borderBottom: activeTab === l ? `2px solid ${C.secondary}` : '2px solid transparent', backgroundColor: 'transparent', color: activeTab === l ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', cursor: 'pointer' }}>{l}</button>
                                                                ))}
                                                            </div>
                                                        )}
                                                        <div style={{ height: Math.min(280, Math.max(120, (codesMap[activeTab === langKeys[0] && expandedId === sol.id ? activeTab : langKeys[0]] || '').split('\n').length * 22)) }}>
                                                            <Editor height="100%" language={LANG_MAP[activeTab === langKeys[0] && expandedId === sol.id ? activeTab : langKeys[0]]?.monaco || 'java'} value={codesMap[langKeys[0]] || ''} theme="vs-dark" options={{ readOnly: true, minimap: { enabled: false }, fontSize: 12, lineNumbers: 'on', scrollBeyondLastLine: false, wordWrap: 'on', padding: { top: 8, bottom: 8 }, domReadOnly: true, contextmenu: false }} loading={<div style={{ height: '100%', backgroundColor: '#0a0a0a' }} />} />
                                                        </div>
                                                        {(sol.explanation || sol.imageUrl) && (
                                                            <div style={{ padding: '12px 24px', borderTop: `1px solid ${C.border}`, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                                                {sol.explanation && (
                                                                    <div style={{ display: 'flex', gap: '8px' }}>
                                                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', marginTop: '2px', flexShrink: 0 }}>Approach</span>
                                                                        <p style={{ margin: 0, fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.muted, lineHeight: '1.6' }}>{sol.explanation}</p>
                                                                    </div>
                                                                )}
                                                                {sol.imageUrl && <img src={sol.imageUrl} alt="diagram" style={{ maxWidth: '100%', maxHeight: '240px', border: `1px solid ${C.border}`, borderRadius: '2px', objectFit: 'contain' }} />}
                                                            </div>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            </div>

            {/* Delete Confirm */}
            {deleteId && (
                <div style={{ position: 'fixed', inset: 0, zIndex: 2000, backgroundColor: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(2px)', display: 'flex', alignItems: 'center', justifyContent: 'center' }} onClick={() => setDeleteId(null)}>
                    <div style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: '24px 28px', maxWidth: '360px', width: '90%', display: 'flex', flexDirection: 'column', gap: '18px', borderRadius: '6px' }} onClick={e => e.stopPropagation()}>
                        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                            <div style={{ width: '36px', height: '36px', borderRadius: '50%', backgroundColor: `${C.error}15`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}><span className="material-symbols-outlined" style={{ fontSize: '18px', color: C.error }}>delete</span></div>
                            <div><p style={{ margin: '0 0 4px', fontFamily: "'Geist', sans-serif", fontSize: '14px', fontWeight: 600, color: C.onBg }}>Delete Solution</p><p style={{ margin: 0, fontFamily: "'Geist', sans-serif", fontSize: '12px', color: C.outline, lineHeight: '1.4' }}>This action cannot be undone.</p></div>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                            <button onClick={() => setDeleteId(null)} style={{ padding: '7px 18px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>Cancel</button>
                            <button onClick={handleDelete} style={{ padding: '7px 22px', border: 'none', backgroundColor: C.error, color: C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>Delete</button>
                        </div>
                    </div>
                </div>
            )}
            <style>{`.material-symbols-outlined{font-variation-settings:'FILL'0,'wght'300}`}</style>
        </div>
    );
}
