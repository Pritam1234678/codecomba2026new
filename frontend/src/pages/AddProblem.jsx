import { useState, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import Editor from '@monaco-editor/react';
import api from '../services/api';
import ProblemService from '../services/problem.service';

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

const emptySnippets = () =>
    Object.fromEntries(LANGS.map(l => [l, { solutionTemplate: '' }]));

export default function AddProblem() {
    const { contestId } = useParams();
    const navigate      = useNavigate();

    const isStandalone = !contestId;
    const backTarget   = isStandalone ? '/admin/problems' : `/admin/contests/${contestId}/problems`;

    const [saving,    setSaving]    = useState(false);
    const [error,     setError]     = useState('');
    const [toast,     setToast]     = useState(null);
    const [activeTab, setActiveTab] = useState('JAVA');

    const [formData, setFormData] = useState({
        title: '', description: '', inputFormat: '', outputFormat: '',
        constraints: '', timeLimit: 1000, memoryLimit: 256,
        example1: '', example2: '', example3: '', images: '',
        active: true, level: 'MEDIUM',
    });

    const [snippets, setSnippets] = useState(emptySnippets());

    // ── AI panel state ────────────────────────────────────────────────────────
    const [aiOpen,    setAiOpen]    = useState(false);
    const [aiQuery,   setAiQuery]   = useState('');
    const [aiModel, setAiModel] = useState('qwen'); // 'kimi' | 'qwen' | 'deepseek'
    const [aiLoading, setAiLoading] = useState(false);
    const [aiError,   setAiError]   = useState('');
    const [aiStatus,  setAiStatus]  = useState('');
    const aiInputRef = useRef(null);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(p => ({
            ...p,
            [name]: type === 'checkbox' ? checked
                  : type === 'number'   ? parseInt(value)
                  : value,
        }));
    };

    const showToast = (msg, type = 'success') => {
        setToast({ msg, type });
        setTimeout(() => setToast(null), 3500);
    };

    // ── AI generation ─────────────────────────────────────────────────────────
    const handleAiGenerate = async () => {
        const q = aiQuery.trim();
        if (!q) { setAiError('Enter a LeetCode problem name or number.'); return; }
        setAiError('');
        setAiLoading(true);
        setAiStatus('Sending request to AI...');
        try {
            setAiStatus('Generating problem statement...');
            const res = await api.post('/admin/problems/ai-generate', { query: q, model: aiModel });
            const { problem: p, snippets: s } = res.data;

            if (!p || !s) {
                setAiError(res.data.warning || res.data.error || 'AI returned unexpected response.');
                return;
            }

            setAiStatus('Filling form fields...');
            setFormData({
                title:        p.title        || '',
                description:  p.description  || '',
                inputFormat:  p.inputFormat  || '',
                outputFormat: p.outputFormat || '',
                constraints:  p.constraints  || '',
                // AI returns seconds (5.0); form stores ms
                timeLimit:    p.timeLimit    ? Math.round(p.timeLimit * 1000) : 5000,
                memoryLimit:  p.memoryLimit  || 256,
                level:        p.level        || 'MEDIUM',
                example1:     p.example1     || '',
                example2:     p.example2     || '',
                example3:     p.example3     || '',
                images:       '',
                active:       true,
            });

            setAiStatus('Loading code harnesses...');
            setSnippets(
                Object.fromEntries(
                    LANGS.map(l => [l, { solutionTemplate: s[l] || '' }])
                )
            );

            setAiOpen(false);
            setAiQuery('');
            setAiStatus('');
            showToast(`AI generated "${p.title}" — review and save.`);
        } catch (err) {
            const msg = err.response?.data?.error || err.response?.data?.message || err.message || 'AI generation failed.';
            setAiError(msg);
        } finally {
            setAiLoading(false);
            if (!aiError) setAiStatus('');
        }
    };

    const openAiPanel = () => {
        setAiOpen(true);
        setAiError('');
        setAiStatus('');
        setTimeout(() => aiInputRef.current?.focus(), 80);
    };

    // ── Form submit ───────────────────────────────────────────────────────────
    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError('');
        try {
            const data = {
                ...formData,
                example1: formData.example1?.trim() || null,
                example2: formData.example2?.trim() || null,
                example3: formData.example3?.trim() || null,
                images:   formData.images?.trim()   || null,
            };

            const pRes = isStandalone
                ? await api.post('/admin/problems', data)
                : await api.post(`/admin/problems/contest/${contestId}`, data);
            const newProblemId = pRes.data.id;

            await ProblemService.saveAllSnippets(
                newProblemId,
                LANGS.map(l => ({ language: l, solutionTemplate: snippets[l].solutionTemplate }))
            );

            showToast('Problem created successfully.');
            setTimeout(() => navigate(
                isStandalone
                    ? `/admin/problems/${newProblemId}/edit`
                    : `/admin/contests/${contestId}/problems`
            ), 1200);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to create problem');
            setSaving(false);
        }
    };

    const hasAiContent = formData.title !== '';

    return (
        <div style={{
            backgroundColor: C.bg,
            color: C.onBg,
            fontFamily: "'Geist', sans-serif",
            height: '100vh',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
        }}>

            {/* ── Header ── */}
            <header style={{
                height: '72px',
                flexShrink: 0,
                borderBottom: `1px solid ${C.border}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0 32px',
                backgroundColor: C.bg,
                zIndex: 10,
            }}>
                {/* Left */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                    <button
                        onClick={() => navigate(backTarget)}
                        style={{
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            width: '40px', height: '40px',
                            border: '1px solid transparent',
                            color: C.outline, background: 'none', cursor: 'pointer',
                            transition: 'all 0.2s',
                        }}
                        onMouseEnter={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.secondary; }}
                        onMouseLeave={e => { e.currentTarget.style.borderColor = 'transparent'; e.currentTarget.style.color = C.outline; }}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>arrow_back</span>
                    </button>

                    <div style={{ width: '1px', height: '32px', backgroundColor: C.border }} />

                    <div>
                        <span style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '10px', letterSpacing: '0.15em',
                            color: C.outline, textTransform: 'uppercase',
                            display: 'block', marginBottom: '2px',
                        }}>
                            {isStandalone
                                ? 'New Standalone Problem'
                                : `Contest CC-${String(contestId).padStart(4, '0')}`}
                        </span>
                        <h1 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '20px', fontWeight: 600,
                            color: C.onBg,
                            display: 'flex', alignItems: 'center', gap: '10px',
                        }}>
                            {formData.title || 'New Problem'}
                            {hasAiContent && (
                                <span style={{
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '9px', letterSpacing: '0.15em',
                                    color: C.secondary, border: `1px solid ${C.secondary}`,
                                    padding: '2px 8px', textTransform: 'uppercase',
                                    backgroundColor: 'rgba(233,193,118,0.1)',
                                }}>
                                    AI Generated
                                </span>
                            )}
                        </h1>
                    </div>
                </div>

                {/* Right */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    {/* AI Generate button */}
                    <button
                        type="button"
                        onClick={openAiPanel}
                        style={{
                            display: 'flex', alignItems: 'center', gap: '8px',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase',
                            border: `1px solid ${C.secondary}`,
                            color: C.secondary,
                            background: 'rgba(233,193,118,0.06)',
                            padding: '10px 18px',
                            cursor: 'pointer',
                            transition: 'all 0.2s',
                        }}
                        onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'rgba(233,193,118,0.06)'; e.currentTarget.style.color = C.secondary; }}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>auto_awesome</span>
                        AI Generate
                    </button>

                    <div style={{ width: '1px', height: '32px', backgroundColor: C.border }} />

                    <button
                        onClick={() => navigate(backTarget)}
                        style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase',
                            color: C.outline, background: 'none', border: 'none',
                            cursor: 'pointer', transition: 'color 0.2s',
                        }}
                        onMouseEnter={e => e.currentTarget.style.color = C.error}
                        onMouseLeave={e => e.currentTarget.style.color = C.outline}
                    >
                        Cancel
                    </button>

                    <div style={{ width: '1px', height: '32px', backgroundColor: C.border }} />

                    <button
                        form="add-problem-form"
                        type="submit"
                        disabled={saving}
                        style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase',
                            border: `1px solid ${C.secondary}`,
                            color: saving ? C.outline : C.secondary,
                            backgroundColor: 'transparent',
                            padding: '10px 24px',
                            cursor: saving ? 'not-allowed' : 'pointer',
                            opacity: saving ? 0.5 : 1,
                            transition: 'all 0.2s',
                        }}
                        onMouseEnter={e => { if (!saving) { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; } }}
                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = saving ? C.outline : C.secondary; }}
                    >
                        {saving ? 'Creating...' : 'Create Problem'}
                    </button>
                </div>
            </header>

            {/* ── Split Workspace ── */}
            <main style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>

                {/* ── Left: Form ── */}
                <section style={{
                    width: '42%',
                    borderRight: `1px solid ${C.border}`,
                    overflowY: 'auto',
                    backgroundColor: C.bg,
                    display: 'flex',
                    flexDirection: 'column',
                }}>
                    {error && (
                        <div style={{
                            margin: '1rem 2rem 0',
                            padding: '10px 14px',
                            border: `1px solid ${C.error}`,
                            borderLeft: `3px solid ${C.error}`,
                            backgroundColor: 'rgba(255,180,171,0.06)',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '12px', color: C.error,
                        }}>
                            {error}
                        </div>
                    )}

                    <form
                        id="add-problem-form"
                        onSubmit={handleSubmit}
                        style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}
                    >
                        {/* ── Core Metadata ── */}
                        <div>
                            <div style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '10px', letterSpacing: '0.2em',
                                color: C.outline, textTransform: 'uppercase',
                                borderBottom: `1px solid ${C.border}`,
                                paddingBottom: '8px', marginBottom: '1.5rem',
                            }}>
                                Core Metadata
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                                <UField
                                    label="Problem Title" name="title" type="text"
                                    value={formData.title} onChange={handleChange} required
                                />
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>
                                            Time Limit (ms)
                                        </label>
                                        <input
                                            type="number" name="timeLimit"
                                            value={formData.timeLimit} onChange={handleChange}
                                            min="100" step="100"
                                            style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', padding: '8px 0', outline: 'none', boxSizing: 'border-box' }}
                                            onFocus={e => e.target.style.borderBottomColor = C.secondary}
                                            onBlur={e => e.target.style.borderBottomColor = C.border}
                                        />
                                    </div>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>
                                            Memory Limit (MB)
                                        </label>
                                        <input
                                            type="number" name="memoryLimit"
                                            value={formData.memoryLimit} onChange={handleChange}
                                            min="64" step="64"
                                            style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', padding: '8px 0', outline: 'none', boxSizing: 'border-box' }}
                                            onFocus={e => e.target.style.borderBottomColor = C.secondary}
                                            onBlur={e => e.target.style.borderBottomColor = C.border}
                                        />
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* ── Statement Editor ── */}
                        <div>
                            <div style={{
                                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', marginBottom: '1rem',
                            }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>
                                    Statement Editor
                                </span>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    {['format_bold', 'format_italic', 'code', 'functions'].map(icon => (
                                        <button key={icon} type="button"
                                            style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.outline, transition: 'color 0.2s', padding: '2px' }}
                                            onMouseEnter={e => e.currentTarget.style.color = C.primary}
                                            onMouseLeave={e => e.currentTarget.style.color = C.outline}
                                        >
                                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{icon}</span>
                                        </button>
                                    ))}
                                </div>
                            </div>
                            <textarea
                                name="description" value={formData.description}
                                onChange={handleChange} required rows={6}
                                style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '15px', lineHeight: 1.6, padding: '8px 0', outline: 'none', resize: 'vertical', boxSizing: 'border-box' }}
                                onFocus={e => e.target.style.borderBottomColor = C.secondary}
                                onBlur={e => e.target.style.borderBottomColor = C.border}
                            />
                        </div>

                        {/* ── I/O + Constraints ── */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            <UField label="Input Format"  name="inputFormat"  type="textarea" value={formData.inputFormat}  onChange={handleChange} />
                            <UField label="Output Format" name="outputFormat" type="textarea" value={formData.outputFormat} onChange={handleChange} />
                            <UField label="Constraints"   name="constraints"  type="textarea" value={formData.constraints}  onChange={handleChange} codeFont />
                        </div>

                        {/* ── Optional Examples ── */}
                        <div>
                            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', marginBottom: '1.5rem' }}>
                                Optional Examples
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                                <UField label="Example 1" name="example1" type="textarea" value={formData.example1} onChange={handleChange} codeFont />
                                <UField label="Example 2" name="example2" type="textarea" value={formData.example2} onChange={handleChange} codeFont />
                                <UField label="Example 3" name="example3" type="textarea" value={formData.example3} onChange={handleChange} codeFont />
                                <UField label="Images (comma-separated URLs)" name="images" type="text" value={formData.images} onChange={handleChange} />
                            </div>
                        </div>

                        {/* ── Difficulty Level ── */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>
                                Difficulty Level
                            </label>
                            <div style={{ display: 'flex', gap: '8px' }}>
                                {['EASY', 'MEDIUM', 'HARD'].map(lv => {
                                    const colors = {
                                        EASY:   { c: '#66bb6a', b: '#66bb6a', bg: 'rgba(102,187,106,0.12)' },
                                        MEDIUM: { c: '#e9c176', b: '#e9c176', bg: 'rgba(233,193,118,0.12)' },
                                        HARD:   { c: '#ffb4ab', b: '#ffb4ab', bg: 'rgba(255,180,171,0.12)' },
                                    };
                                    const lc  = colors[lv];
                                    const sel = formData.level === lv;
                                    return (
                                        <button key={lv} type="button"
                                            onClick={() => setFormData(p => ({ ...p, level: lv }))}
                                            style={{ flex: 1, padding: '10px', border: `1px solid ${sel ? lc.b : C.border}`, backgroundColor: sel ? lc.bg : 'transparent', color: sel ? lc.c : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.12em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s' }}
                                        >
                                            {lv}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>

                        {/* ── Active toggle ── */}
                        <label style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
                            <input
                                type="checkbox" name="active"
                                checked={formData.active} onChange={handleChange}
                                style={{ accentColor: C.primary, width: '16px', height: '16px' }}
                            />
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted, letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                                Active (visible to users)
                            </span>
                        </label>
                    </form>
                </section>

                {/* ── Right: Monaco Editor ── */}
                <section style={{ flex: 1, display: 'flex', flexDirection: 'column', backgroundColor: C.surfaceMin }}>

                    {/* Editor top tab bar */}
                    <div style={{ display: 'flex', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, flexShrink: 0, paddingTop: '8px', paddingLeft: '8px', gap: '4px' }}>
                        <button
                            type="button"
                            style={{ padding: '10px 24px', borderBottom: `2px solid ${C.primary}`, color: C.primary, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', background: C.surfaceHi, border: 'none', cursor: 'pointer' }}
                        >
                            Validator / Harness
                        </button>
                        <div style={{ flex: 1 }} />
                        <div style={{
                            padding: '10px 16px',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase',
                            color: hasAiContent ? C.secondary : C.outline,
                            borderLeft: `1px solid ${C.border}`,
                            display: 'flex', alignItems: 'center', gap: '6px',
                        }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                                {hasAiContent ? 'auto_awesome' : 'add_circle'}
                            </span>
                            {hasAiContent ? 'AI Filled' : 'New Problem'}
                        </div>
                    </div>

                    {/* Language tabs */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px', borderBottom: `1px solid ${C.border}`, flexShrink: 0, backgroundColor: C.surfaceMin }}>
                        <div style={{ display: 'flex', gap: '1px', backgroundColor: C.border }}>
                            {LANGS.map(l => (
                                <button key={l} type="button" onClick={() => setActiveTab(l)}
                                    style={{ padding: '6px 16px', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', border: 'none', cursor: 'pointer', backgroundColor: activeTab === l ? C.secondary : C.surfaceMin, color: activeTab === l ? C.bg : C.outline, transition: 'all 0.2s' }}
                                >
                                    {LANG_LABELS[l]}
                                </button>
                            ))}
                        </div>
                        <div style={{ display: 'flex', gap: '12px' }}>
                            {['settings', 'subject'].map(icon => (
                                <button key={icon} type="button"
                                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.outline, transition: 'color 0.2s' }}
                                    onMouseEnter={e => e.currentTarget.style.color = C.onBg}
                                    onMouseLeave={e => e.currentTarget.style.color = C.outline}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{icon}</span>
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Monaco editor area */}
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
                                onChange={v => setSnippets(p => ({
                                    ...p,
                                    [activeTab]: { solutionTemplate: v || '' },
                                }))}
                                options={{
                                    fontSize: 13,
                                    fontFamily: "'Fira Code', 'Cascadia Code', monospace",
                                    fontLigatures: true,
                                    minimap: { enabled: false },
                                    scrollBeyondLastLine: false,
                                    automaticLayout: true,
                                    lineNumbers: 'on',
                                    folding: true,
                                    bracketPairColorization: { enabled: true },
                                    autoClosingBrackets: 'always',
                                    autoClosingQuotes: 'always',
                                    tabSize: 4,
                                    insertSpaces: true,
                                    wordWrap: 'off',
                                    padding: { top: 12, bottom: 12 },
                                }}
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

            {/* ── AI Generate Modal ── */}
            <AnimatePresence>
                {aiOpen && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        style={{
                            position: 'fixed', inset: 0, zIndex: 80,
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            backgroundColor: 'rgba(0,0,0,0.75)',
                            backdropFilter: 'blur(8px)',
                            padding: '24px',
                        }}
                        onClick={e => { if (e.target === e.currentTarget && !aiLoading) { setAiOpen(false); setAiError(''); } }}
                    >
                        <motion.div
                            initial={{ scale: 0.95, y: 16 }}
                            animate={{ scale: 1, y: 0 }}
                            exit={{ scale: 0.95, y: 16 }}
                            style={{
                                backgroundColor: C.surfaceCon,
                                border: `1px solid ${C.border}`,
                                maxWidth: '560px',
                                width: '100%',
                                position: 'relative',
                                overflow: 'hidden',
                            }}
                        >
                            {/* Top amber accent */}
                            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.secondary }} />

                            {/* Header */}
                            <div style={{ padding: '2rem 2rem 1.5rem', borderBottom: `1px solid ${C.border}` }}>
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                        <span className="material-symbols-outlined" style={{ fontSize: '22px', color: C.secondary }}>auto_awesome</span>
                                        <div>
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>
                                                Kimi K2.6 · NVIDIA NIM
                                            </span>
                                            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 600, color: C.onBg, margin: 0 }}>
                                                AI Problem Generator
                                            </h2>
                                        </div>
                                    </div>
                                    {!aiLoading && (
                                        <button
                                            onClick={() => { setAiOpen(false); setAiError(''); }}
                                            style={{ background: 'none', border: `1px solid ${C.border}`, color: C.outline, cursor: 'pointer', width: '32px', height: '32px', display: 'flex', alignItems: 'center', justifyContent: 'center', transition: 'all 0.2s' }}
                                            onMouseEnter={e => { e.currentTarget.style.borderColor = C.error; e.currentTarget.style.color = C.error; }}
                                            onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.outline; }}
                                        >
                                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>close</span>
                                        </button>
                                    )}
                                </div>
                                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.muted, marginTop: '12px', marginBottom: 0, lineHeight: 1.5 }}>
                                    Enter a LeetCode problem name or number. The AI will generate the full problem statement and all 5 language harnesses with test cases.
                                </p>
                            </div>

                            {/* Body */}
                            <div style={{ padding: '1.5rem 2rem' }}>

                                {/* Model selector */}
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '1.25rem' }}>
                                    <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>AI Model</label>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        {[
                                            { id: 'qwen',     label: 'Qwen3 Coder',     sub: 'qwen · 480B MoE' },
                                            { id: 'kimi',     label: 'Kimi K2.6',        sub: 'moonshotai' },
                                            { id: 'deepseek', label: 'DeepSeek V4 Pro',  sub: 'deepseek-ai · slow' },
                                        ].map(m => {
                                            const active = aiModel === m.id;
                                            return (
                                                <button key={m.id} type="button" disabled={aiLoading} onClick={() => setAiModel(m.id)}
                                                    style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: '2px', padding: '10px 14px', border: `1px solid ${active ? C.secondary : C.border}`, backgroundColor: active ? `${C.secondary}12` : 'transparent', cursor: aiLoading ? 'not-allowed' : 'pointer', transition: 'all 0.15s', textAlign: 'left' }}>
                                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', fontWeight: 600, color: active ? C.secondary : C.muted, letterSpacing: '0.06em' }}>{m.label}</span>
                                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, letterSpacing: '0.08em', textTransform: 'uppercase' }}>{m.sub}</span>
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>

                                {/* Input */}
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '1.25rem' }}>
                                    <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>
                                        Problem Name or Number
                                    </label>
                                    <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                                        <span style={{ position: 'absolute', left: 0, color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', opacity: 0.6 }}>
                                            &gt;
                                        </span>
                                        <input
                                            ref={aiInputRef}
                                            type="text"
                                            value={aiQuery}
                                            onChange={e => setAiQuery(e.target.value)}
                                            onKeyDown={e => { if (e.key === 'Enter' && !aiLoading) handleAiGenerate(); }}
                                            disabled={aiLoading}
                                            placeholder="e.g. Two Sum   or   #42   or   Trapping Rain Water"
                                            style={{
                                                width: '100%', backgroundColor: 'transparent',
                                                border: 'none', borderBottom: `1px solid ${C.border}`,
                                                color: C.onBg, fontFamily: "'JetBrains Mono', monospace",
                                                fontSize: '14px', padding: '8px 0 8px 22px',
                                                outline: 'none', transition: 'border-color 0.2s',
                                                boxSizing: 'border-box',
                                                opacity: aiLoading ? 0.5 : 1,
                                            }}
                                            onFocus={e => e.target.style.borderBottomColor = C.secondary}
                                            onBlur={e => e.target.style.borderBottomColor = C.border}
                                        />
                                    </div>
                                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: '4px' }}>
                                        {['Two Sum', 'Binary Search', 'Merge Intervals', '#42'].map(ex => (
                                            <button
                                                key={ex}
                                                type="button"
                                                disabled={aiLoading}
                                                onClick={() => setAiQuery(ex)}
                                                style={{
                                                    fontFamily: "'JetBrains Mono', monospace",
                                                    fontSize: '9px', letterSpacing: '0.1em', textTransform: 'uppercase',
                                                    border: `1px solid ${C.border}`, color: C.outline,
                                                    backgroundColor: 'transparent', padding: '4px 10px',
                                                    cursor: aiLoading ? 'not-allowed' : 'pointer',
                                                    transition: 'all 0.15s',
                                                }}
                                                onMouseEnter={e => { if (!aiLoading) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                                                onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.outline; }}
                                            >
                                                {ex}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* Loading state */}
                                {aiLoading && (
                                    <motion.div
                                        initial={{ opacity: 0, y: 4 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        style={{
                                            display: 'flex', alignItems: 'center', gap: '12px',
                                            padding: '12px 16px',
                                            border: `1px solid ${C.border}`,
                                            backgroundColor: 'rgba(233,193,118,0.05)',
                                            marginBottom: '1.25rem',
                                        }}
                                    >
                                        <span style={{ animation: 'spin 1s linear infinite', display: 'inline-block', fontSize: '16px', color: C.secondary }}>⟳</span>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.secondary, letterSpacing: '0.08em' }}>
                                            {aiStatus || 'Processing...'}
                                        </span>
                                    </motion.div>
                                )}

                                {/* Error state */}
                                {aiError && !aiLoading && (
                                    <motion.div
                                        initial={{ opacity: 0, y: -4 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        style={{
                                            padding: '10px 14px',
                                            border: `1px solid ${C.error}`,
                                            borderLeft: `3px solid ${C.error}`,
                                            backgroundColor: 'rgba(255,180,171,0.06)',
                                            marginBottom: '1.25rem',
                                            fontFamily: "'JetBrains Mono', monospace",
                                            fontSize: '12px', color: C.error,
                                        }}
                                    >
                                        {aiError}
                                    </motion.div>
                                )}

                                {/* Info note */}
                                {!aiLoading && !aiError && (
                                    <div style={{ display: 'flex', gap: '10px', marginBottom: '1.25rem' }}>
                                        {[
                                            { icon: 'description', text: 'Full problem statement + I/O format' },
                                            { icon: 'code',        text: '5 language harnesses with 10 test cases each' },
                                        ].map(({ icon, text }) => (
                                            <div key={icon} style={{ flex: 1, display: 'flex', alignItems: 'flex-start', gap: '8px', padding: '10px 12px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}>
                                                <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.secondary, flexShrink: 0, marginTop: '1px' }}>{icon}</span>
                                                <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '12px', color: C.outline, lineHeight: 1.4 }}>{text}</span>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            {/* Footer */}
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '16px', padding: '1rem 2rem 1.5rem', borderTop: `1px solid ${C.border}` }}>
                                <button
                                    type="button"
                                    disabled={aiLoading}
                                    onClick={() => { setAiOpen(false); setAiError(''); setAiQuery(''); }}
                                    style={{
                                        fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase',
                                        color: C.outline, background: 'none', border: 'none', cursor: aiLoading ? 'not-allowed' : 'pointer', padding: '10px 20px', transition: 'color 0.2s', opacity: aiLoading ? 0.4 : 1,
                                    }}
                                    onMouseEnter={e => { if (!aiLoading) e.currentTarget.style.color = C.onBg; }}
                                    onMouseLeave={e => { e.currentTarget.style.color = C.outline; }}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    disabled={aiLoading || !aiQuery.trim()}
                                    onClick={handleAiGenerate}
                                    style={{
                                        display: 'flex', alignItems: 'center', gap: '8px',
                                        fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase',
                                        border: `1px solid ${C.secondary}`,
                                        color: (aiLoading || !aiQuery.trim()) ? C.outline : C.secondary,
                                        backgroundColor: 'transparent',
                                        padding: '10px 28px',
                                        cursor: (aiLoading || !aiQuery.trim()) ? 'not-allowed' : 'pointer',
                                        opacity: (aiLoading || !aiQuery.trim()) ? 0.5 : 1,
                                        transition: 'all 0.2s',
                                    }}
                                    onMouseEnter={e => { if (!aiLoading && aiQuery.trim()) { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; } }}
                                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = (aiLoading || !aiQuery.trim()) ? C.outline : C.secondary; }}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '15px' }}>auto_awesome</span>
                                    {aiLoading ? 'Generating...' : 'Generate'}
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* ── Toast ── */}
            <AnimatePresence>
                {toast && (
                    <motion.div
                        initial={{ opacity: 0, x: 40 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: 40 }}
                        style={{ position: 'fixed', bottom: '2rem', right: '2rem', backgroundColor: C.surfaceLow, borderLeft: `2px solid ${toast.type === 'success' ? C.secondary : C.error}`, padding: '1rem 1.5rem', zIndex: 100, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: toast.type === 'success' ? C.secondary : C.error, letterSpacing: '0.05em', maxWidth: '420px' }}
                    >
                        {toast.msg}
                    </motion.div>
                )}
            </AnimatePresence>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
            `}</style>
        </div>
    );
}

// ── Reusable underline field ──────────────────────────────────────────────────
const UField = ({ label, name, type, value, onChange, placeholder, required, codeFont }) => {
    const [f, setF] = useState(false);
    const isTA = type === 'textarea';
    const style = {
        width: '100%', backgroundColor: 'transparent', border: 'none',
        borderBottom: `1px solid ${f ? '#e9c176' : '#50453b'}`,
        color: '#e5e2e1',
        fontFamily: codeFont ? "'JetBrains Mono', monospace" : "'Geist', sans-serif",
        fontSize: '14px', padding: '8px 0', outline: 'none',
        transition: 'border-color 0.2s', boxSizing: 'border-box',
        resize: isTA ? 'vertical' : undefined,
    };
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: '#9d8e83', textTransform: 'uppercase' }}>
                {label}
            </label>
            {isTA
                ? <textarea name={name} value={value} onChange={onChange} rows={3} style={style} onFocus={() => setF(true)} onBlur={() => setF(false)} />
                : <input type={type} name={name} value={value} onChange={onChange} placeholder={placeholder} required={required} style={style} onFocus={() => setF(true)} onBlur={() => setF(false)} />
            }
        </div>
    );
};

