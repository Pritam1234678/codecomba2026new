import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import axios from 'axios';
import Editor from '@monaco-editor/react';
import ProblemService from '../services/problem.service';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

const C = {
    bg:         '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceHi:  '#2a2a2a',
    surfaceMin: '#0e0e0e',
    border:     '#50453b',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
};

const LANGS = ['JAVA', 'CPP', 'PYTHON', 'JAVASCRIPT', 'C'];
const LANG_LABELS = { JAVA: 'Java', CPP: 'C++', PYTHON: 'Python', JAVASCRIPT: 'JavaScript', C: 'C' };
const LANG_MONACO = { JAVA: 'java', CPP: 'cpp', PYTHON: 'python', JAVASCRIPT: 'javascript', C: 'c' };

export default function EditProblem() {
    const { id }     = useParams();
    const navigate   = useNavigate();
    const [loading,  setLoading]  = useState(true);
    const [saving,   setSaving]   = useState(false);
    const [error,    setError]    = useState('');
    const [dirty,    setDirty]    = useState(false);
    const [toast,    setToast]    = useState(null);
    const [contestId, setContestId] = useState(null);
    const [activeTab, setActiveTab] = useState('JAVA');
    const [editorTab, setEditorTab] = useState('harness'); // harness | template

    const [formData, setFormData] = useState({
        title: '', description: '', inputFormat: '', outputFormat: '',
        constraints: '', timeLimit: 1000, memoryLimit: 256,
        example1: '', example2: '', example3: '', images: '', active: true, level: 'MEDIUM'
    });

    const [snippets, setSnippets] = useState(
        Object.fromEntries(LANGS.map(l => [l, { solutionTemplate: '' }]))
    );

    useEffect(() => {
        const load = async () => {
            try {
                const token = localStorage.getItem('token');
                const h = { Authorization: `Bearer ${token}` };
                const res = await axios.get(`${API_URL}/problems/${id}`, { headers: h });
                const p = res.data;
                setFormData({ title: p.title || '', description: p.description || '', inputFormat: p.inputFormat || '', outputFormat: p.outputFormat || '', constraints: p.constraints || '', timeLimit: p.timeLimit || 1000, memoryLimit: p.memoryLimit || 256, example1: p.example1 || '', example2: p.example2 || '', example3: p.example3 || '', images: p.images || '', active: p.active !== undefined ? p.active : true, level: p.level || 'MEDIUM' });
                if (p.contestId) setContestId(p.contestId);
                try {
                    const sRes = await ProblemService.getSnippetsAdmin(id);
                    const map = Object.fromEntries(LANGS.map(l => [l, { solutionTemplate: '' }]));
                    sRes.data.forEach(s => { map[s.language] = { solutionTemplate: s.solutionTemplate || '' }; });
                    setSnippets(map);
                } catch {}
            } catch { setError('Failed to load problem'); }
            finally { setLoading(false); }
        };
        load();
    }, [id]);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(p => ({ ...p, [name]: type === 'checkbox' ? checked : (type === 'number' ? parseInt(value) : value) }));
        setDirty(true);
    };

    const showToast = (msg, type = 'success') => { setToast({ msg, type }); setTimeout(() => setToast(null), 3000); };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError('');
        try {
            const token = localStorage.getItem('token');
            const data = { ...formData, example1: formData.example1?.trim() || null, example2: formData.example2?.trim() || null, example3: formData.example3?.trim() || null, images: formData.images?.trim() || null };
            await axios.put(`${API_URL}/admin/problems/${id}`, data, { headers: { Authorization: `Bearer ${token}` } });
            await ProblemService.saveAllSnippets(id, LANGS.map(l => ({ language: l, solutionTemplate: snippets[l].solutionTemplate })));
            setDirty(false);
            showToast('Problem saved successfully.');
            setTimeout(() => contestId ? navigate(`/admin/contests/${contestId}/problems`) : navigate('/admin/contests'), 1500);
        } catch (err) { setError(err.response?.data?.message || 'Failed to update problem'); setSaving(false); }
    };

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>Loading...</div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>

            {/* ── Task Header ── */}
            <header style={{ height: '72px', flexShrink: 0, borderBottom: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 32px', backgroundColor: C.bg, zIndex: 10 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                    <button
                        onClick={() => contestId ? navigate(`/admin/contests/${contestId}/problems`) : navigate('/admin/contests')}
                        style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '40px', height: '40px', border: `1px solid transparent`, color: C.outline, background: 'none', cursor: 'pointer', transition: 'all 0.2s' }}
                        onMouseEnter={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.secondary; }}
                        onMouseLeave={e => { e.currentTarget.style.borderColor = 'transparent'; e.currentTarget.style.color = C.outline; }}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>arrow_back</span>
                    </button>
                    <div style={{ width: '1px', height: '32px', backgroundColor: C.border }} />
                    <div>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '2px' }}>Editing Problem</span>
                        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 600, color: C.onBg, maxWidth: '400px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {formData.title || 'Problem'}
                        </h1>
                    </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                    {dirty && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                            <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: C.secondary, display: 'inline-block' }} />
                            Unsaved Changes
                        </div>
                    )}
                    <div style={{ width: '1px', height: '32px', backgroundColor: C.border }} />
                    <button
                        onClick={() => navigate(`/admin/problems/${id}/testcases`)}
                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', transition: 'color 0.2s' }}
                        onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                        onMouseLeave={e => e.currentTarget.style.color = C.outline}
                    >
                        Manage Test Cases
                    </button>
                    <button
                        form="edit-problem-form"
                        type="submit"
                        disabled={saving}
                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', border: `1px solid ${C.primary}`, color: saving ? C.outline : C.primary, backgroundColor: 'transparent', padding: '10px 24px', cursor: saving ? 'not-allowed' : 'pointer', opacity: saving ? 0.5 : 1, transition: 'all 0.2s' }}
                        onMouseEnter={e => { if (!saving) { e.currentTarget.style.backgroundColor = C.primary; e.currentTarget.style.color = C.bg; } }}
                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = saving ? C.outline : C.primary; }}
                    >
                        {saving ? 'Saving...' : 'Save Changes'}
                    </button>
                </div>
            </header>

            {/* ── Split Workspace ── */}
            <main style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>

                {/* ── Left: Form ── */}
                <section style={{ width: '42%', borderRight: `1px solid ${C.border}`, overflowY: 'auto', backgroundColor: C.bg, display: 'flex', flexDirection: 'column' }}>
                    {error && (
                        <div style={{ margin: '1rem 2rem 0', padding: '10px 14px', border: `1px solid ${C.error}`, borderLeft: `3px solid ${C.error}`, backgroundColor: 'rgba(255,180,171,0.06)', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                            {error}
                        </div>
                    )}
                    <form id="edit-problem-form" onSubmit={handleSubmit} style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>

                        {/* Core Metadata */}
                        <div>
                            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', marginBottom: '1.5rem' }}>Core Metadata</div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                                <UField label="Problem Title" name="title" type="text" value={formData.title} onChange={handleChange} required />
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Time Limit (ms)</label>
                                        <input type="number" name="timeLimit" value={formData.timeLimit} onChange={handleChange} min="100" step="100"
                                            style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', padding: '8px 0', outline: 'none', boxSizing: 'border-box' }}
                                            onFocus={e => e.target.style.borderBottomColor = C.secondary} onBlur={e => e.target.style.borderBottomColor = C.border}
                                        />
                                    </div>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Memory Limit (MB)</label>
                                        <input type="number" name="memoryLimit" value={formData.memoryLimit} onChange={handleChange} min="64" step="64"
                                            style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', padding: '8px 0', outline: 'none', boxSizing: 'border-box' }}
                                            onFocus={e => e.target.style.borderBottomColor = C.secondary} onBlur={e => e.target.style.borderBottomColor = C.border}
                                        />
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Statement Editor */}
                        <div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', marginBottom: '1rem' }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>Statement Editor</span>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    {['format_bold', 'format_italic', 'code', 'functions'].map(icon => (
                                        <button key={icon} type="button" style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.outline, transition: 'color 0.2s', padding: '2px' }}
                                            onMouseEnter={e => e.currentTarget.style.color = C.primary} onMouseLeave={e => e.currentTarget.style.color = C.outline}
                                        >
                                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{icon}</span>
                                        </button>
                                    ))}
                                </div>
                            </div>
                            <textarea name="description" value={formData.description} onChange={handleChange} required rows={6}
                                style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '15px', lineHeight: 1.6, padding: '8px 0', outline: 'none', resize: 'vertical', boxSizing: 'border-box' }}
                                onFocus={e => e.target.style.borderBottomColor = C.secondary} onBlur={e => e.target.style.borderBottomColor = C.border}
                            />
                        </div>

                        {/* I/O + Constraints */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            <UField label="Input Format" name="inputFormat" type="textarea" value={formData.inputFormat} onChange={handleChange} />
                            <UField label="Output Format" name="outputFormat" type="textarea" value={formData.outputFormat} onChange={handleChange} />
                            <UField label="Constraints" name="constraints" type="textarea" value={formData.constraints} onChange={handleChange} codeFont />
                        </div>

                        {/* Examples */}
                        <div>
                            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', marginBottom: '1.5rem' }}>Optional Examples</div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                                <UField label="Example 1" name="example1" type="textarea" value={formData.example1} onChange={handleChange} codeFont />
                                <UField label="Example 2" name="example2" type="textarea" value={formData.example2} onChange={handleChange} codeFont />
                                <UField label="Example 3" name="example3" type="textarea" value={formData.example3} onChange={handleChange} codeFont />
                                <UField label="Images (comma-separated URLs)" name="images" type="text" value={formData.images} onChange={handleChange} />
                            </div>
                        </div>

                        {/* Difficulty Level */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>
                                Difficulty Level
                            </label>
                            <div style={{ display: 'flex', gap: '8px' }}>
                                {['EASY', 'MEDIUM', 'HARD'].map(lv => {
                                    const colors = { EASY: { c:'#66bb6a', b:'#66bb6a', bg:'rgba(102,187,106,0.12)' }, MEDIUM: { c:'#e9c176', b:'#e9c176', bg:'rgba(233,193,118,0.12)' }, HARD: { c:'#ffb4ab', b:'#ffb4ab', bg:'rgba(255,180,171,0.12)' } };
                                    const lc = colors[lv];
                                    const sel = formData.level === lv;
                                    return (
                                        <button key={lv} type="button"
                                            onClick={() => { setFormData(p => ({ ...p, level: lv })); setDirty(true); }}
                                            style={{ flex: 1, padding: '10px', border: `1px solid ${sel ? lc.b : C.border}`, backgroundColor: sel ? lc.bg : 'transparent', color: sel ? lc.c : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.12em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s' }}
                                        >{lv}</button>
                                    );
                                })}
                            </div>
                        </div>

                        {/* Active */}
                        <label style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
                            <input type="checkbox" name="active" checked={formData.active} onChange={handleChange} style={{ accentColor: C.primary, width: '16px', height: '16px' }} />
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted, letterSpacing: '0.08em', textTransform: 'uppercase' }}>Active (visible to users)</span>
                        </label>
                    </form>
                </section>

                {/* ── Right: Code Editor ── */}
                <section style={{ flex: 1, display: 'flex', flexDirection: 'column', backgroundColor: C.surfaceMin }}>
                    {/* Editor Tabs */}
                    <div style={{ display: 'flex', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, flexShrink: 0, paddingTop: '8px', paddingLeft: '8px', gap: '4px' }}>
                        {[
                            { key: 'harness', label: 'Validator / Harness' },
                        ].map(t => (
                            <button key={t.key} type="button" onClick={() => setEditorTab(t.key)}
                                style={{ padding: '10px 24px', borderBottom: editorTab === t.key ? `2px solid ${C.primary}` : '2px solid transparent', color: editorTab === t.key ? C.primary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', background: editorTab === t.key ? C.surfaceHi : 'transparent', border: 'none', cursor: 'pointer', transition: 'all 0.2s' }}
                            >{t.label}</button>
                        ))}
                        <div style={{ flex: 1 }} />
                        <button type="button" style={{ padding: '10px 16px', color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', background: 'none', border: 'none', borderLeft: `1px solid ${C.border}`, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>visibility</span>
                            Preview
                        </button>
                    </div>

                    {/* Lang Tabs + Toolbar */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px', borderBottom: `1px solid ${C.border}`, flexShrink: 0, backgroundColor: C.surfaceMin }}>
                        <div style={{ display: 'flex', gap: '1px', backgroundColor: C.border }}>
                            {LANGS.map(l => (
                                <button key={l} type="button" onClick={() => setActiveTab(l)}
                                    style={{ padding: '6px 16px', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', border: 'none', cursor: 'pointer', backgroundColor: activeTab === l ? C.secondary : C.surfaceMin, color: activeTab === l ? C.bg : C.outline, transition: 'all 0.2s' }}
                                >{LANG_LABELS[l]}</button>
                            ))}
                        </div>
                        <div style={{ display: 'flex', gap: '12px' }}>
                            {['settings', 'subject'].map(icon => (
                                <button key={icon} type="button" style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.outline, transition: 'color 0.2s' }}
                                    onMouseEnter={e => e.currentTarget.style.color = C.onBg} onMouseLeave={e => e.currentTarget.style.color = C.outline}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{icon}</span>
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Monaco */}
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                        <div style={{ padding: '6px 16px', backgroundColor: C.surfaceMin, borderBottom: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between', flexShrink: 0 }}>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                {LANG_LABELS[activeTab]} — Solution Harness
                            </span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.border }}>
                                Mark editable zone with USER_CODE_START / USER_CODE_END
                            </span>
                        </div>
                        <div style={{ flex: 1 }}>
                            <Editor
                                height="100%"
                                theme="vs-dark"
                                language={LANG_MONACO[activeTab]}
                                value={snippets[activeTab].solutionTemplate}
                                onChange={v => { setSnippets(p => ({ ...p, [activeTab]: { solutionTemplate: v || '' } })); setDirty(true); }}
                                options={{ fontSize: 13, fontFamily: "'Fira Code', 'Cascadia Code', monospace", fontLigatures: true, minimap: { enabled: false }, scrollBeyondLastLine: false, automaticLayout: true, lineNumbers: 'on', folding: true, bracketPairColorization: { enabled: true }, autoClosingBrackets: 'always', autoClosingQuotes: 'always', tabSize: 4, insertSpaces: true, wordWrap: 'off', padding: { top: 12, bottom: 12 } }}
                            />
                        </div>
                        {/* Status bar */}
                        <div style={{ height: '28px', borderTop: `1px solid ${C.border}`, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 16px', backgroundColor: C.surfaceLow, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                            <span>UTF-8</span>
                            <span>{LANG_LABELS[activeTab]}</span>
                        </div>
                    </div>
                </section>
            </main>

            {/* ── Toast ── */}
            <AnimatePresence>
                {toast && (
                    <motion.div initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 40 }}
                        style={{ position: 'fixed', bottom: '2rem', right: '2rem', backgroundColor: C.surfaceLow, borderLeft: `2px solid ${toast.type === 'success' ? C.secondary : C.error}`, padding: '1rem 1.5rem', zIndex: 100, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: toast.type === 'success' ? C.secondary : C.error, letterSpacing: '0.05em' }}
                    >{toast.msg}</motion.div>
                )}
            </AnimatePresence>

            <style>{`.material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }`}</style>
        </div>
    );
}

const UField = ({ label, name, type, value, onChange, placeholder, required, codeFont }) => {
    const [f, setF] = useState(false);
    const isTA = type === 'textarea';
    const style = { width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${f ? '#e9c176' : '#50453b'}`, color: '#e5e2e1', fontFamily: codeFont ? "'JetBrains Mono', monospace" : "'Geist', sans-serif", fontSize: '14px', padding: '8px 0', outline: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box', resize: isTA ? 'vertical' : undefined };
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: '#9d8e83', textTransform: 'uppercase' }}>{label}</label>
            {isTA ? <textarea name={name} value={value} onChange={onChange} rows={3} style={style} onFocus={() => setF(true)} onBlur={() => setF(false)} /> : <input type={type} name={name} value={value} onChange={onChange} placeholder={placeholder} required={required} style={style} onFocus={() => setF(true)} onBlur={() => setF(false)} />}
        </div>
    );
};
