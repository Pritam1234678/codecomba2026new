import { useState, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import api from '../services/api';
import SkeletonLoader from './SkeletonLoader';

const C = { bg: '#131313', surfaceMin: '#0e0e0e', surfaceHi: '#2a2a2a', surfaceLow: '#1c1b1b', border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1', error: '#ffb4ab' };
const LANG_MAP = { JAVA: { monaco: 'java' }, CPP: { monaco: 'cpp' }, C: { monaco: 'c' }, PYTHON: { monaco: 'python' }, JAVASCRIPT: { monaco: 'javascript' } };
const LANGS = ['JAVA', 'CPP', 'PYTHON', 'JAVASCRIPT', 'C'];

export default function SolutionPanel({ problemId, currentUserId }) {
    const [solutions, setSolutions] = useState([]);
    const [count, setCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const [selected, setSelected] = useState(null);
    const [activeLang, setActiveLang] = useState('JAVA');
    const [editing, setEditing] = useState(false);
    const [editCodes, setEditCodes] = useState({});
    const [editExplanation, setEditExplanation] = useState('');
    const [editImageUrl, setEditImageUrl] = useState('');
    const [saving, setSaving] = useState(false);
    const [deleteId, setDeleteId] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [formLang, setFormLang] = useState('JAVA');
    const [formCodes, setFormCodes] = useState(Object.fromEntries(LANGS.map(l => [l, ''])));
    const [formExplanation, setFormExplanation] = useState('');
    const [formImageUrl, setFormImageUrl] = useState('');

    useEffect(() => { fetchAll(); }, [problemId]);

    const fetchAll = () => {
        setLoading(true);
        Promise.all([api.get(`/practice/solutions/${problemId}`), api.get(`/practice/solutions/${problemId}/count`)])
            .then(([r1, r2]) => { setSolutions(r1.data || []); setCount(r2.data?.count || 0); })
            .catch(() => {}).finally(() => setLoading(false));
    };

    const handleCreate = async () => {
        const map = {};
        for (const [l, c] of Object.entries(formCodes)) if (c.trim()) map[l] = c;
        if (!Object.keys(map).length) return;
        setSaving(true);
        try {
            await api.post('/practice/solutions', { problemId, codes: map, explanation: formExplanation.trim() || null, imageUrl: formImageUrl.trim() || null });
            setShowForm(false);
            setFormCodes(Object.fromEntries(LANGS.map(l => [l, ''])));
            setFormExplanation(''); setFormImageUrl('');
            fetchAll();
        } catch (err) { alert(err.response?.data?.error || 'Failed'); }
        finally { setSaving(false); }
    };

    const startEditFor = (sol) => {
        setSelected(sol);
        const c = sol.codes || {};
        setEditCodes(Object.fromEntries(LANGS.map(l => [l, c[l] || ''])));
        setEditExplanation(sol.explanation || '');
        setEditImageUrl(sol.imageUrl || '');
        setActiveLang(Object.keys(c).find(l => c[l]?.trim()) || 'JAVA');
        setEditing(true);
    };

    const handleUpdate = async () => {
        const map = {};
        for (const [l, c] of Object.entries(editCodes)) if (c.trim()) map[l] = c;
        if (!Object.keys(map).length) return;
        setSaving(true);
        try {
            await api.put(`/practice/solutions/${selected.id}`, { codes: map, explanation: editExplanation.trim() || null, imageUrl: editImageUrl.trim() || null });
            setEditing(false); fetchAll();
        } catch (err) { alert(err.response?.data?.error || 'Failed'); }
        finally { setSaving(false); }
    };

    const handleDelete = async () => {
        if (!deleteId) return;
        try { await api.delete(`/practice/solutions/${deleteId}`); setDeleteId(null); setSelected(null); setEditing(false); fetchAll(); }
        catch (err) { alert(err.response?.data?.error || 'Failed'); setDeleteId(null); }
    };

    return (
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            {/* Add button bar */}
            <div style={{ padding: '8px 14px', borderBottom: `1px solid ${C.border}`, display: 'flex', flexShrink: 0 }}>
                <button onClick={() => { setShowForm(!showForm); setSelected(null); setEditing(false); }}
                    style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '6px 14px', border: `1px solid ${showForm ? C.border : C.secondary}`, color: showForm ? C.muted : C.bg, backgroundColor: showForm ? 'transparent' : C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>{showForm ? 'arrow_back' : 'add'}</span>
                    {showForm ? 'Back' : 'Add Solution'}
                </button>
            </div>

            <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
                {showForm ? (
                    /* Add Solution Form */
                    <div style={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
                        <div style={{ display: 'flex', gap: '0', borderBottom: `1px solid ${C.border}`, padding: '0 14px' }}>
                            {LANGS.map(l => (
                                <button key={l} onClick={() => setFormLang(l)}
                                    style={{ padding: '7px 12px', border: 'none', borderBottom: formLang === l ? `2px solid ${C.secondary}` : '2px solid transparent', backgroundColor: 'transparent', color: formLang === l ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', cursor: 'pointer', transition: 'all 0.15s' }}
                                >{l}{formCodes[l]?.trim() ? ' ✓' : ''}</button>
                            ))}
                        </div>
                        <div style={{ height: '220px' }}>
                            <Editor height="100%" language={LANG_MAP[formLang]?.monaco || 'java'}
                                value={formCodes[formLang] || ''}
                                onChange={v => setFormCodes(p => ({ ...p, [formLang]: v || '' }))}
                                theme="vs-dark"
                                options={{ minimap: { enabled: false }, fontSize: 12, lineNumbers: 'on', scrollBeyondLastLine: false, wordWrap: 'on', padding: { top: 8 } }}
                                loading={<SkeletonLoader compact rows={3} />} />
                        </div>
                        <div style={{ padding: '10px 14px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            <textarea value={formExplanation} onChange={e => setFormExplanation(e.target.value)} placeholder="Explain your approach..." rows={5}
                                style={{ padding: '8px 12px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '11px', lineHeight: '1.5', resize: 'vertical', outline: 'none', minHeight: '100px' }} />
                            <input type="text" value={formImageUrl} onChange={e => setFormImageUrl(e.target.value)} placeholder="Image URL (optional)"
                                style={{ padding: '7px 12px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', outline: 'none' }} />
                            <button onClick={handleCreate} disabled={saving || Object.values(formCodes).every(c => !c.trim())}
                                style={{ alignSelf: 'flex-end', padding: '6px 22px', border: 'none', borderRadius: '3px', backgroundColor: saving ? C.surfaceHi : C.secondary, color: saving ? C.outline : C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: saving ? 'not-allowed' : 'pointer' }}>
                                {saving ? 'Saving...' : 'Submit'}
                            </button>
                        </div>
                    </div>
                ) : selected ? (
                    /* Solution Detail */
                    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                        <div style={{ padding: '8px 14px', borderBottom: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', gap: '8px', backgroundColor: C.surfaceMin, flexWrap: 'wrap' }}>
                            <button onClick={() => { setSelected(null); setEditing(false); }}
                                style={{ background: 'none', border: `1px solid ${C.border}`, color: C.outline, cursor: 'pointer', padding: '4px 10px', borderRadius: '2px', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', display: 'flex', alignItems: 'center', gap: '4px', flexShrink: 0 }}>
                                <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>arrow_back</span> Back
                            </button>
                            <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', fontWeight: 600, color: C.primary }}>{selected.userName || 'Anonymous'}</span>
                            {editing ? (
                                <div style={{ display: 'flex', gap: '2px' }}>
                                    {LANGS.map(l => (
                                        <button key={l} onClick={() => setActiveLang(l)}
                                            style={{ padding: '2px 8px', border: 'none', borderBottom: activeLang === l ? `2px solid ${C.secondary}` : '2px solid transparent', backgroundColor: 'transparent', color: activeLang === l ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', cursor: 'pointer' }}>{l}{editCodes[l]?.trim() ? ' ✓' : ''}</button>
                                    ))}
                                </div>
                            ) : (
                                <div style={{ display: 'flex', gap: '4px' }}>
                                    {Object.keys(selected.codes || {}).filter(l => (selected.codes[l] || '').trim()).map(l => (
                                        <button key={l} onClick={() => setActiveLang(l)}
                                            style={{ padding: '2px 8px', border: 'none', borderBottom: activeLang === l ? `2px solid ${C.secondary}` : '2px solid transparent', backgroundColor: 'transparent', color: activeLang === l ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.06em', cursor: 'pointer' }}>{l}</button>
                                    ))}
                                </div>
                            )}
                            <div style={{ flex: 1 }} />
                            {selected.userId === currentUserId && !editing && (
                                <>
                                    <button onClick={() => startEditFor(selected)}
                                        style={{ padding: '3px 8px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, borderRadius: '2px', cursor: 'pointer' }}>
                                        <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>edit</span>
                                    </button>
                                    <button onClick={() => setDeleteId(selected.id)}
                                        style={{ padding: '3px 8px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, borderRadius: '2px', cursor: 'pointer' }}>
                                        <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>delete</span>
                                    </button>
                                </>
                            )}
                            {editing && (
                                <>
                                    <button onClick={handleUpdate} disabled={saving}
                                        style={{ padding: '3px 10px', border: 'none', backgroundColor: C.secondary, color: C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.06em', cursor: 'pointer', borderRadius: '2px' }}>{saving ? '...' : 'Save'}</button>
                                    <button onClick={() => setEditing(false)}
                                        style={{ padding: '3px 10px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', cursor: 'pointer', borderRadius: '2px' }}>Cancel</button>
                                </>
                            )}
                        </div>
                        <div style={{ flex: 1, overflowY: 'auto' }}>
                            {(() => {
                                const code = editing ? (editCodes[activeLang] || '') : (selected.codes?.[activeLang] || '');
                                return (
                                    <div style={{ height: Math.min(360, Math.max(160, code.split('\n').length * 22)) }}>
                                        <Editor height="100%" language={LANG_MAP[activeLang]?.monaco || 'java'}
                                            value={code}
                                            onChange={editing ? v => setEditCodes(p => ({ ...p, [activeLang]: v || '' })) : undefined}
                                            theme="vs-dark"
                                            options={{ readOnly: !editing, minimap: { enabled: false }, fontSize: 12, lineNumbers: 'on', scrollBeyondLastLine: false, wordWrap: 'on', padding: { top: 8, bottom: 8 }, domReadOnly: !editing, contextmenu: false }}
                                            loading={<div style={{ height: '100%', backgroundColor: '#0a0a0a' }} />} />
                                    </div>
                                );
                            })()}
                            {editing ? (
                                <div style={{ padding: '10px 14px', display: 'flex', flexDirection: 'column', gap: '8px', borderTop: `1px solid ${C.border}` }}>
                                    <textarea value={editExplanation} onChange={e => setEditExplanation(e.target.value)} placeholder="Explain your approach..." rows={5}
                                        style={{ padding: '8px 12px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '11px', lineHeight: '1.5', resize: 'vertical', outline: 'none', minHeight: '100px' }} />
                                    <input type="text" value={editImageUrl} onChange={e => setEditImageUrl(e.target.value)} placeholder="Image URL (optional)"
                                        style={{ padding: '7px 12px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', outline: 'none' }} />
                                </div>
                            ) : (selected.explanation || selected.imageUrl) && (
                                <div style={{ padding: '14px 18px', borderTop: `1px solid ${C.border}` }}>
                                    {selected.explanation && (
                                        <div style={{ display: 'flex', gap: '8px' }}>
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', marginTop: '3px', flexShrink: 0 }}>Approach</span>
                                            <p style={{ margin: 0, fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.muted, lineHeight: '1.6' }}>{selected.explanation}</p>
                                        </div>
                                    )}
                                    {selected.imageUrl && <img src={selected.imageUrl} style={{ maxWidth: '100%', maxHeight: '200px', border: `1px solid ${C.border}`, borderRadius: '2px', objectFit: 'contain', marginTop: '10px' }} />}
                                </div>
                            )}
                        </div>
                    </div>
                ) : loading ? (
                    <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><SkeletonLoader compact rows={3} /></div>
                ) : solutions.length === 0 ? (
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '10px', padding: '2rem' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '32px', color: C.border }}>lightbulb</span>
                        <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.outline }}>No solutions yet</span>
                    </div>
                ) : (
                    /* Solution List */
                    <div>
                        {solutions.map(sol => {
                            const cm = sol.codes || {};
                            const lk = Object.keys(cm).filter(l => cm[l]?.trim());
                            return (
                                <div key={sol.id}
                                    style={{ padding: '12px 18px', borderBottom: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer', transition: 'background-color 0.15s' }}
                                    onMouseEnter={e => e.currentTarget.style.backgroundColor = '#1c1b1b'}
                                    onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}>
                                    <div onClick={() => { setSelected(sol); setActiveLang(lk[0] || 'JAVA'); setEditing(false); }}
                                        style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0, flex: 1 }}>
                                        <div style={{ width: '22px', height: '22px', borderRadius: '50%', backgroundColor: C.surfaceHi, border: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontFamily: "'Geist', sans-serif", fontSize: '9px', fontWeight: 700, color: C.primary }}>
                                            {(sol.userName || 'A')[0].toUpperCase()}
                                        </div>
                                        <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '12px', color: C.onBg, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{sol.userName || 'Anonymous'}</span>
                                        <div style={{ display: 'flex', gap: '3px', flexWrap: 'wrap' }}>
                                            {lk.map(l => <span key={l} style={{ padding: '1px 5px', borderRadius: '2px', fontFamily: "'JetBrains Mono', monospace", fontSize: '7px', letterSpacing: '0.08em', color: C.secondary, textTransform: 'uppercase', backgroundColor: `${C.secondary}12`, border: `1px solid ${C.secondary}30` }}>{l}</span>)}
                                        </div>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
                                        {sol.userId === currentUserId && (
                                            <>
                                                <button onClick={e => { e.stopPropagation(); startEditFor(sol); }}
                                                    style={{ padding: '3px 6px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, borderRadius: '2px', cursor: 'pointer' }} title="Edit">
                                                    <span className="material-symbols-outlined" style={{ fontSize: '11px' }}>edit</span>
                                                </button>
                                                <button onClick={e => { e.stopPropagation(); setDeleteId(sol.id); }}
                                                    style={{ padding: '3px 6px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, borderRadius: '2px', cursor: 'pointer' }} title="Delete">
                                                    <span className="material-symbols-outlined" style={{ fontSize: '11px' }}>delete</span>
                                                </button>
                                            </>
                                        )}
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, flexShrink: 0 }}>
                                            {new Date(sol.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                                        </span>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            {/* Delete Confirm */}
            {deleteId && (
                <div style={{ position: 'fixed', inset: 0, zIndex: 2000, backgroundColor: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(2px)', display: 'flex', alignItems: 'center', justifyContent: 'center' }} onClick={() => setDeleteId(null)}>
                    <div style={{ backgroundColor: C.surfaceMin, border: `1px solid ${C.border}`, padding: '24px 28px', maxWidth: '340px', width: '90%', display: 'flex', flexDirection: 'column', gap: '16px', borderRadius: '6px' }} onClick={e => e.stopPropagation()}>
                        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                            <div style={{ width: '34px', height: '34px', borderRadius: '50%', backgroundColor: `${C.error}15`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}><span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.error }}>delete</span></div>
                            <div><p style={{ margin: '0 0 4px', fontFamily: "'Geist', sans-serif", fontSize: '14px', fontWeight: 600, color: C.onBg }}>Delete Solution</p><p style={{ margin: 0, fontFamily: "'Geist', sans-serif", fontSize: '12px', color: C.outline, lineHeight: '1.4' }}>This cannot be undone.</p></div>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                            <button onClick={() => setDeleteId(null)} style={{ padding: '6px 16px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>Cancel</button>
                            <button onClick={handleDelete} style={{ padding: '6px 20px', border: 'none', backgroundColor: C.error, color: C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>Delete</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
