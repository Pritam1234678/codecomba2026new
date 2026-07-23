import { useState, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import api from '../services/api';
import SkeletonLoader from './SkeletonLoader';

const C = {
    bg: '#131313', surfaceCon: '#201f1f', surfaceLow: '#1c1b1b', surfaceHi: '#2a2a2a',
    surfaceMin: '#0e0e0e', border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176',
    muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1', error: '#ffb4ab', success: '#4ade80',
};

const LANG_MAP = {
    JAVA: { label: 'Java 21', monaco: 'java' },
    CPP: { label: 'C++ 20', monaco: 'cpp' },
    C: { label: 'C', monaco: 'c' },
    PYTHON: { label: 'Python 3.11', monaco: 'python' },
    JAVASCRIPT: { label: 'JavaScript', monaco: 'javascript' },
};

const LANGS = ['JAVA', 'CPP', 'PYTHON', 'JAVASCRIPT', 'C'];

export default function PracticeSolutions({ problemId, currentUserId, onClose }) {
    const [solutions, setSolutions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [count, setCount] = useState(0);
    const [showForm, setShowForm] = useState(false);
    const [saving, setSaving] = useState(false);
    const [deleteId, setDeleteId] = useState(null);

    const [activeLang, setActiveLang] = useState('JAVA');
    const [codes, setCodes] = useState({ JAVA: '', CPP: '', PYTHON: '', JAVASCRIPT: '', C: '' });
    const [explanation, setExplanation] = useState('');
    const [imageUrl, setImageUrl] = useState('');

    const [editingId, setEditingId] = useState(null);
    const [editLang, setEditLang] = useState('JAVA');
    const [editCodes, setEditCodes] = useState({ JAVA: '', CPP: '', PYTHON: '', JAVASCRIPT: '', C: '' });
    const [editExplanation, setEditExplanation] = useState('');
    const [editImageUrl, setEditImageUrl] = useState('');

    const [viewLangId, setViewLangId] = useState(null);
    const [viewLang, setViewLang] = useState(null);
    const switchViewLang = (id, l) => { setViewLangId(id); setViewLang(l); };

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
        for (const [l, c] of Object.entries(codes)) { if (c.trim()) map[l] = c; }
        if (!Object.keys(map).length) return;
        setSaving(true);
        try {
            await api.post('/practice/solutions', {
                problemId: parseInt(problemId),
                codes: map,
                explanation: explanation.trim() || null,
                imageUrl: imageUrl.trim() || null,
            });
            setShowForm(false);
            resetForm();
            fetchAll();
        } catch (err) { alert(err.response?.data?.error || 'Failed'); }
        finally { setSaving(false); }
    };

    const handleUpdate = async () => {
        const map = {};
        for (const [l, c] of Object.entries(editCodes)) { if (c.trim()) map[l] = c; }
        if (!Object.keys(map).length) return;
        setSaving(true);
        try {
            await api.put(`/practice/solutions/${editingId}`, {
                codes: map,
                explanation: editExplanation.trim() || null,
                imageUrl: editImageUrl.trim() || null,
            });
            setEditingId(null);
            fetchAll();
        } catch (err) { alert(err.response?.data?.error || 'Failed'); }
        finally { setSaving(false); }
    };

    const handleDelete = async () => {
        if (!deleteId) return;
        try {
            await api.delete(`/practice/solutions/${deleteId}`);
            setDeleteId(null);
            fetchAll();
        } catch (err) { alert(err.response?.data?.error || 'Failed'); setDeleteId(null); }
    };

    const startEdit = (sol) => {
        setEditingId(sol.id);
        const c = sol.codes || {};
        setEditCodes({ JAVA: c.JAVA || '', CPP: c.CPP || '', PYTHON: c.PYTHON || '', JAVASCRIPT: c.JAVASCRIPT || '', C: c.C || '' });
        setEditExplanation(sol.explanation || '');
        setEditImageUrl(sol.imageUrl || '');
        setEditLang(Object.keys(c).find(k => c[k]) || 'JAVA');
    };

    const resetForm = () => {
        setCodes({ JAVA: '', CPP: '', PYTHON: '', JAVASCRIPT: '', C: '' });
        setExplanation('');
        setImageUrl('');
        setActiveLang('JAVA');
    };

    useEffect(() => { fetchAll(); }, [problemId]);

    return (
        <div style={{
            position: 'fixed', inset: 0, zIndex: 1001,
            backgroundColor: 'rgba(0,0,0,0.85)', backdropFilter: 'blur(4px)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
        }} onClick={onClose}>
            <div style={{
                width: 'min(95vw, 1100px)', maxHeight: '90vh',
                backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`,
                display: 'flex', flexDirection: 'column', borderRadius: '4px',
                boxShadow: '0 24px 80px rgba(0,0,0,0.6)',
            }} onClick={e => e.stopPropagation()}>
                {/* Header */}
                <div style={{
                    height: '54px', flexShrink: 0, borderBottom: `1px solid ${C.border}`,
                    backgroundColor: C.surfaceMin, display: 'flex', alignItems: 'center',
                    justifyContent: 'space-between', padding: '0 24px',
                }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                        <div style={{ width: '32px', height: '32px', borderRadius: '50%', backgroundColor: `${C.secondary}18`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '18px', color: C.secondary }}>lightbulb</span>
                        </div>
                        <div>
                            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '18px', fontWeight: 600, color: C.primary }}>Solutions</span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, marginLeft: '10px' }}>{count} solution{count !== 1 ? 's' : ''}</span>
                        </div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <button onClick={() => setShowForm(!showForm)}
                            style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 18px', border: `1px solid ${showForm ? C.border : C.secondary}`, color: showForm ? C.muted : C.bg, backgroundColor: showForm ? 'transparent' : C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '15px' }}>{showForm ? 'arrow_back' : 'add'}</span>
                            {showForm ? 'Back' : 'Add Solution'}
                        </button>
                        <button onClick={onClose}
                            style={{ width: '32px', height: '32px', borderRadius: '50%', border: 'none', backgroundColor: 'transparent', color: C.outline, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>close</span>
                        </button>
                    </div>
                </div>

                <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
                    {showForm ? (
                        <AddForm
                            activeLang={activeLang} setActiveLang={setActiveLang}
                            codes={codes} setCodes={setCodes}
                            explanation={explanation} setExplanation={setExplanation}
                            imageUrl={imageUrl} setImageUrl={setImageUrl}
                            saving={saving} onSave={handleSave}
                        />
                    ) : loading ? (
                        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <SkeletonLoader compact rows={4} />
                        </div>
                    ) : solutions.length === 0 ? (
                        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '16px', color: C.outline }}>
                            <div style={{ width: '64px', height: '64px', borderRadius: '50%', border: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <span className="material-symbols-outlined" style={{ fontSize: '28px', color: C.border }}>lightbulb</span>
                            </div>
                            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '18px', color: C.muted }}>No solutions yet</span>
                            <button onClick={() => setShowForm(true)}
                                style={{ padding: '10px 24px', border: `1px solid ${C.secondary}`, color: C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>
                                Add Solution
                            </button>
                        </div>
                    ) : (
                        <div style={{ overflowY: 'auto', padding: '16px 24px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            {solutions.map(sol => {
                                const isOwner = sol.userId === currentUserId;
                                const isEditing = editingId === sol.id;
                                const codesMap = sol.codes || {};

                                return (
                                    <div key={sol.id} style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, overflow: 'hidden', borderRadius: '3px' }}>
                                        {/* Card header */}
                                        <div style={{ padding: '12px 18px', borderBottom: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#0e0d0c' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                                <div style={{ width: '28px', height: '28px', borderRadius: '50%', backgroundColor: C.surfaceHi, border: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: "'Geist', sans-serif", fontSize: '11px', fontWeight: 700, color: C.primary }}>
                                                    {(sol.userName || 'A')[0].toUpperCase()}
                                                </div>
                                                <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', fontWeight: 600, color: C.onBg }}>{sol.userName || 'Anonymous'}</span>
                                                {!isEditing && Object.keys(codesMap).filter(l => codesMap[l]?.trim()).map(l => (
                                                    <span key={l} style={{ padding: '2px 6px', borderRadius: '2px', fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.12em', color: C.secondary, textTransform: 'uppercase', backgroundColor: `${C.secondary}12`, border: `1px solid ${C.secondary}30` }}>{l}</span>
                                                ))}
                                            </div>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                                {isEditing && (
                                                    <div style={{ display: 'flex', gap: '2px' }}>
                                                        {LANGS.map(l => (
                                                            <button key={l} onClick={() => setEditLang(l)}
                                                                style={{ padding: '4px 10px', border: 'none', borderBottom: editLang === l ? `2px solid ${C.secondary}` : '2px solid transparent', backgroundColor: 'transparent', color: editLang === l ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', cursor: 'pointer' }}
                                                            >{l}{editCodes[l]?.trim() ? ' ✓' : ''}</button>
                                                        ))}
                                                    </div>
                                                )}
                                                {isOwner && !isEditing && (
                                                    <>
                                                        <button onClick={() => startEdit(sol)}
                                                            style={{ padding: '4px 10px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', cursor: 'pointer', borderRadius: '2px' }}>
                                                            <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>edit</span>
                                                        </button>
                                                        <button onClick={() => setDeleteId(sol.id)}
                                                            style={{ padding: '4px 10px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', cursor: 'pointer', borderRadius: '2px' }}>
                                                            <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>delete</span>
                                                        </button>
                                                    </>
                                                )}
                                                {isEditing && (
                                                    <>
                                                        <button onClick={handleUpdate}
                                                            style={{ padding: '4px 12px', border: `1px solid ${C.secondary}`, backgroundColor: C.secondary, color: C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.06em', cursor: 'pointer', borderRadius: '2px' }}>Save</button>
                                                        <button onClick={() => setEditingId(null)}
                                                            style={{ padding: '4px 12px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', cursor: 'pointer', borderRadius: '2px' }}>Cancel</button>
                                                    </>
                                                )}
                                                {!isEditing && (
                                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>
                                                        {new Date(sol.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}
                                                    </span>
                                                )}
                                            </div>
                                        </div>

                                        {/* Code + explanation */}
                                        {isEditing ? (
                                            <>
                                                <div style={{ height: Math.min(320, Math.max(120, (editCodes[editLang] || '').split('\n').length * 20 + 32)) }}>
                                                    <Editor height="100%" language={LANG_MAP[editLang]?.monaco || 'java'}
                                                        value={editCodes[editLang] || ''}
                                                        onChange={v => setEditCodes(p => ({ ...p, [editLang]: v || '' }))}
                                                        theme="vs-dark"
                                                        options={{ readOnly: false, minimap: { enabled: false }, fontSize: 12, lineNumbers: 'on', scrollBeyondLastLine: false, wordWrap: 'on', padding: { top: 10, bottom: 10 } }}
                                                        loading={<div style={{ height: '100%', backgroundColor: '#0a0a0a' }} />}
                                                    />
                                                </div>
                                                <div style={{ padding: '12px 18px', display: 'flex', flexDirection: 'column', gap: '10px', borderTop: `1px solid ${C.border}` }}>
                                                    <textarea value={editExplanation} onChange={e => setEditExplanation(e.target.value)}
                                                        placeholder="Explain your approach..."
                                                        style={{ minHeight: '70px', padding: '10px 14px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '12px', lineHeight: '1.6', resize: 'vertical', outline: 'none' }} />
                                                    <input type="text" value={editImageUrl} onChange={e => setEditImageUrl(e.target.value)} placeholder="Image URL (optional)"
                                                        style={{ padding: '8px 14px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', outline: 'none' }} />
                                                </div>
                                            </>
                                        ) : (
                                            <>
                                                {Object.keys(codesMap).filter(l => codesMap[l]?.trim()).length > 0 && (
                                                    <div style={{ display: 'flex', gap: '0', borderBottom: `1px solid ${C.border}`, backgroundColor: '#0a0a0a', padding: '0 12px' }}>
                                                        {Object.keys(codesMap).filter(l => codesMap[l]?.trim()).map(l => (
                                                            <button key={l} onClick={() => switchViewLang(sol.id, l)}
                                                                style={{ padding: '6px 14px', border: 'none', borderBottom: viewLangId === sol.id && viewLang === l ? `2px solid ${C.secondary}` : '2px solid transparent', backgroundColor: 'transparent', color: viewLangId === sol.id && viewLang === l ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.06em', cursor: 'pointer', transition: 'all 0.15s' }}
                                                            >{l}</button>
                                                        ))}
                                                    </div>
                                                )}
                                                {(() => {
                                                    const al = viewLangId === sol.id && viewLang ? viewLang : Object.keys(codesMap)[0];
                                                    const c = codesMap[al] || '';
                                                    return (
                                                        <div style={{ height: Math.min(320, Math.max(120, c.split('\n').length * 20 + 32)) }}>
                                                            <Editor height="100%" language={LANG_MAP[al]?.monaco || 'java'}
                                                                value={c} theme="vs-dark"
                                                                options={{ readOnly: true, minimap: { enabled: false }, fontSize: 12, lineNumbers: 'on', scrollBeyondLastLine: false, wordWrap: 'on', padding: { top: 10, bottom: 10 }, domReadOnly: true, contextmenu: false }}
                                                                loading={<div style={{ height: '100%', backgroundColor: '#0a0a0a' }} />}
                                                            />
                                                        </div>
                                                    );
                                                })()}
                                                {(sol.explanation || sol.imageUrl) && (
                                                    <div style={{ padding: '14px 18px', display: 'flex', flexDirection: 'column', gap: '12px', borderTop: `1px solid ${C.border}` }}>
                                                        {sol.explanation && (
                                                            <div style={{ display: 'flex', gap: '10px' }}>
                                                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', marginTop: '3px', flexShrink: 0 }}>Approach</span>
                                                                <p style={{ margin: 0, fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.muted, lineHeight: '1.7' }}>{sol.explanation}</p>
                                                            </div>
                                                        )}
                                                        {sol.imageUrl && (
                                                            <img src={sol.imageUrl} alt="diagram" style={{ maxWidth: '100%', maxHeight: '280px', border: `1px solid ${C.border}`, borderRadius: '2px', objectFit: 'contain' }} />
                                                        )}
                                                    </div>
                                                )}
                                            </>
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
                    <div style={{ backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, padding: '28px 32px', maxWidth: '380px', width: '90%', display: 'flex', flexDirection: 'column', gap: '20px', borderRadius: '6px', boxShadow: '0 24px 64px rgba(0,0,0,0.5)' }} onClick={e => e.stopPropagation()}>
                        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '14px' }}>
                            <div style={{ width: '40px', height: '40px', borderRadius: '50%', backgroundColor: `${C.error}15`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                                <span className="material-symbols-outlined" style={{ fontSize: '20px', color: C.error }}>delete</span>
                            </div>
                            <div>
                                <p style={{ margin: '0 0 6px', fontFamily: "'Geist', sans-serif", fontSize: '15px', fontWeight: 600, color: C.onBg }}>Delete Solution</p>
                                <p style={{ margin: 0, fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.outline, lineHeight: '1.5' }}>Are you sure? This action cannot be undone.</p>
                            </div>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                            <button onClick={() => setDeleteId(null)}
                                style={{ padding: '8px 20px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>Cancel</button>
                            <button onClick={handleDelete}
                                style={{ padding: '8px 24px', border: 'none', backgroundColor: C.error, color: C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', borderRadius: '3px' }}>Delete</button>
                        </div>
                    </div>
                </div>
            )}

            <style>{`.material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }`}</style>
        </div>
    );
}

function AddForm({ activeLang, setActiveLang, codes, setCodes, explanation, setExplanation, imageUrl, setImageUrl, saving, onSave }) {
    return (
        <div style={{ overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
            <div style={{ padding: '16px 24px 0', display: 'flex', gap: '2px', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceHi }}>
                {LANGS.map(l => (
                    <button key={l} onClick={() => setActiveLang(l)}
                        style={{ padding: '8px 16px', border: 'none', borderBottom: activeLang === l ? `2px solid ${C.secondary}` : '2px solid transparent', backgroundColor: 'transparent', color: activeLang === l ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.08em', cursor: 'pointer', transition: 'all 0.15s' }}
                    >{l}{codes[l]?.trim() ? ' ✓' : ''}</button>
                ))}
            </div>
            <div style={{ padding: '16px 24px', height: '280px' }}>
                <Editor height="100%" language={LANG_MAP[activeLang]?.monaco || 'java'}
                    value={codes[activeLang] || ''}
                    onChange={v => setCodes(p => ({ ...p, [activeLang]: v || '' }))}
                    theme="vs-dark"
                    options={{ minimap: { enabled: false }, fontSize: 13, lineNumbers: 'on', scrollBeyondLastLine: false, wordWrap: 'on', padding: { top: 12 } }}
                    loading={<SkeletonLoader compact rows={3} />}
                />
            </div>
            <div style={{ padding: '0 24px 16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <textarea value={explanation} onChange={e => setExplanation(e.target.value)}
                    placeholder="Explain your approach, time complexity, and optimizations..."
                    rows={5}
                    style={{ width: '100%', padding: '12px 14px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', lineHeight: '1.7', resize: 'vertical', outline: 'none', boxSizing: 'border-box' }} />
                <input type="text" value={imageUrl} onChange={e => setImageUrl(e.target.value)} placeholder="Image URL — diagram, whiteboard, etc. (optional)"
                    style={{ padding: '10px 14px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', outline: 'none' }} />
                <button onClick={onSave} disabled={saving || Object.values(codes).every(c => !c.trim())}
                    style={{ alignSelf: 'flex-end', padding: '10px 32px', border: 'none', borderRadius: '3px', backgroundColor: saving || Object.values(codes).every(c => !c.trim()) ? C.surfaceHi : C.secondary, color: saving || Object.values(codes).every(c => !c.trim()) ? C.outline : C.bg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: saving || Object.values(codes).every(c => !c.trim()) ? 'not-allowed' : 'pointer', transition: 'all 0.2s' }}>
                    {saving ? 'Saving...' : 'Submit Solution'}
                </button>
            </div>
        </div>
    );
}
