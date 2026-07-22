import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import api from '../services/api';
import AuthService from '../services/auth.service';
import SkeletonLoader from '../components/SkeletonLoader';

// ── Design tokens ─────────────────────────────────────────────────────────────
const C = {
    bg: '#121212', surface: '#1e1e1e', surfaceAlt: '#252525',
    onBg: '#e0e0e0', primary: '#bb86fc', secondary: '#03dac6',
    error: '#cf6679', success: '#4caf50', warning: '#fb8c00',
    outline: '#444', muted: '#888'
};

// ── Language detection from filename ──────────────────────────────────────────
function detectLanguage(filename) {
    if (!filename) return 'plaintext';
    const ext = filename.split('.').pop().toLowerCase();
    const map = {
        java: 'java', py: 'python', js: 'javascript', ts: 'typescript',
        jsx: 'javascript', tsx: 'typescript', json: 'json', xml: 'xml',
        yml: 'yaml', yaml: 'yaml', properties: 'ini', md: 'markdown',
        html: 'html', css: 'css', sql: 'sql', sh: 'shell',
    };
    return map[ext] || 'plaintext';
}

// ── Main Component ────────────────────────────────────────────────────────────
const WebContest = () => {
    const { problemId } = useParams();

    // ── State ─────────────────────────────────────────────────────────────────
    const [description, setDescription] = useState('');
    const [title, setTitle] = useState('');
    const [descOpen, setDescOpen] = useState(true);
    const [editableFiles, setEditableFiles] = useState({});   // { path: code }
    const [readonlyFiles, setReadonlyFiles] = useState({});   // { path: code }
    const [activeFile, setActiveFile] = useState(null);       // path string
    const [testCount, setTestCount] = useState(0);
    const [language, setLanguage] = useState('JAVA');
    const [loading, setLoading] = useState(true);
    const [running, setRunning] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [output, setOutput] = useState(null);               // JSX or null
    const [runCount, setRunCount] = useState(0);
    const [submitCount, setSubmitCount] = useState(0);
    const [score, setScore] = useState(null);

    const sseRef = useRef(null);
    const activeSubRef = useRef(null);
    const saveTimer = useRef(null);

    // ── localStorage persistence (debounced) ──────────────────────────────────
    useEffect(() => {
        clearTimeout(saveTimer.current);
        saveTimer.current = setTimeout(() => {
            try {
                localStorage.setItem(`web_contest_${problemId}_files`, JSON.stringify(editableFiles));
            } catch { /* quota exceeded — ignore */ }
        }, 600);
        return () => clearTimeout(saveTimer.current);
    }, [editableFiles, problemId]);

    // ── Load template on mount ────────────────────────────────────────────────
    useEffect(() => {
        setLoading(true);
        api.get(`/web-contest/problems/${problemId}/template`, { params: { language: 'JAVA' } })
            .then(res => {
                const { editableFiles: ef, readonlyFiles: rf, manifest } = res.data;
                setReadonlyFiles(rf || {});
                setTestCount(manifest?.testCount || 0);
                setLanguage(manifest?.language || 'JAVA');
                setDescription(manifest?.description || '');
                setTitle(manifest?.title || `Problem ${problemId}`);

                // Restore from localStorage if available
                let restored = null;
                try {
                    const saved = localStorage.getItem(`web_contest_${problemId}_files`);
                    if (saved) restored = JSON.parse(saved);
                } catch { /* ignore */ }

                const files = restored && Object.keys(restored).length > 0 ? restored : (ef || {});
                setEditableFiles(files);

                // Set first editable file as active, or first readonly
                const allPaths = [...Object.keys(files), ...Object.keys(rf || {})];
                const editablePaths = Object.keys(files);
                setActiveFile(editablePaths[0] || allPaths[0] || null);
            })
            .catch(err => {
                console.error('Failed to load web contest template', err);
                setOutput(
                    <span style={{ color: C.error, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px' }}>
                        Failed to load problem template. Please refresh.
                    </span>
                );
            })
            .finally(() => setLoading(false));
    }, [problemId]);

    // ── SSE connection for verdicts ───────────────────────────────────────────
    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (!user?.token) return;

        const API_BASE = import.meta.env.VITE_API_URL
            ?? (window.location.hostname === 'localhost' ? 'http://localhost:8080/api' : '/api');

        let es = null;
        let cancelled = false;

        api.post('/submissions/sse-ticket')
            .then(res => {
                if (cancelled) return;
                const ticket = res.data?.ticket;
                if (!ticket) return;

                const url = `${API_BASE}/submissions/stream?ticket=${encodeURIComponent(ticket)}`;
                es = new EventSource(url);
                sseRef.current = es;

                es.addEventListener('verdict', (e) => {
                    try {
                        const verdict = JSON.parse(e.data);
                        if (activeSubRef.current != null &&
                            verdict.submissionId !== activeSubRef.current) return;
                        activeSubRef.current = null;
                        setRunning(false);
                        setSubmitting(false);
                        handleVerdict(verdict);
                    } catch (err) {
                        console.error('SSE parse error:', err);
                    }
                });

                es.addEventListener('connected', () => {
                    console.log('Web contest SSE connected');
                });

                es.onerror = () => {
                    console.warn('SSE connection error — polling fallback active');
                };
            })
            .catch(err => {
                console.warn('SSE ticket failed; using polling fallback', err);
            });

        return () => {
            cancelled = true;
            if (es) { try { es.close(); } catch { /* noop */ } }
            sseRef.current = null;
        };
    }, []);

    // ── Verdict handler ───────────────────────────────────────────────────────
    const handleVerdict = useCallback((verdict) => {
        const passed = verdict.testCasesPassed ?? verdict.passed ?? 0;
        const total = verdict.totalTestCases ?? verdict.total ?? testCount;
        const pct = total > 0 ? Math.round((passed / total) * 100) : 0;
        setScore(pct);

        if (verdict.status === 'CE' || verdict.status === 'RE') {
            setOutput(
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <span style={{ color: C.error, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
                        {verdict.status === 'CE' ? '⚠️ Compilation Error' : '💥 Runtime Error'}
                    </span>
                    <pre style={{ background: 'rgba(0,0,0,0.5)', border: `1px solid ${C.error}40`, padding: '10px', color: '#fca5a5', fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", whiteSpace: 'pre-wrap', overflowX: 'auto', margin: 0 }}>
                        {verdict.errorMessage || 'No details available'}
                    </pre>
                </div>
            );
            return;
        }

        if (verdict.status === 'TLE') {
            setOutput(
                <span style={{ color: C.warning, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
                    ⏱ Time Limit Exceeded
                </span>
            );
            return;
        }

        // Build TC results
        const tcs = verdict.testCases || verdict.testCaseDetails || [];
        const parsedTCs = typeof tcs === 'string' ? JSON.parse(tcs) : tcs;
        setOutput(
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                    {parsedTCs.length > 0 ? parsedTCs.map((tc, i) => {
                        const p = tc.passed ?? (tc.status === 'PASS');
                        return (
                            <span key={i} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: p ? C.success : C.error }}>
                                {p ? '✅' : '❌'} TC{i + 1}: {p ? 'PASS' : 'FAIL'}
                            </span>
                        );
                    }) : (
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.onBg }}>
                            {passed}/{total} test cases passed
                        </span>
                    )}
                </div>
                <div style={{ height: '3px', background: C.outline, borderRadius: '2px', marginTop: '4px' }}>
                    <div style={{ height: '100%', width: `${pct}%`, background: pct === 100 ? C.success : pct > 50 ? C.warning : C.error, borderRadius: '2px', transition: 'width 0.6s ease' }} />
                </div>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: pct === 100 ? C.success : C.onBg }}>
                    {passed}/{total} passed — Score: {pct}
                </span>
            </div>
        );
    }, [testCount]);

    // ── Polling fallback ──────────────────────────────────────────────────────
    const pollVerdict = useCallback((submissionId) => {
        let attempts = 0;
        const maxAttempts = 30;
        const interval = setInterval(() => {
            attempts++;
            if (attempts > maxAttempts) {
                clearInterval(interval);
                setRunning(false);
                setSubmitting(false);
                setOutput(
                    <span style={{ color: C.warning, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px' }}>
                        ⏱ Timed out waiting for results. Check back later.
                    </span>
                );
                return;
            }
            api.get(`/submissions/${submissionId}/status`)
                .then(res => {
                    const s = res.data;
                    if (s.status && s.status !== 'PENDING' && s.status !== 'RUNNING') {
                        clearInterval(interval);
                        if (activeSubRef.current === submissionId) {
                            activeSubRef.current = null;
                            setRunning(false);
                            setSubmitting(false);
                            handleVerdict(s);
                        }
                    }
                })
                .catch(() => { /* ignore polling errors */ });
        }, 2000);
    }, [handleVerdict]);

    // ── Run / Submit handlers ─────────────────────────────────────────────────
    const handleRun = async () => {
        if (running || submitting) return;
        setRunning(true);
        setOutput(null);
        setScore(null);
        try {
            const res = await api.post('/web-contest/run', {
                problemId, editableFiles, language
            });
            const subId = res.data?.id;
            activeSubRef.current = subId;
            setRunCount(c => c + 1);
            pollVerdict(subId);
        } catch (err) {
            setRunning(false);
            setOutput(
                <span style={{ color: C.error, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px' }}>
                    ❌ {err.response?.data?.message || err.message || 'Run failed'}
                </span>
            );
        }
    };

    const handleSubmit = async () => {
        if (running || submitting) return;
        setSubmitting(true);
        setOutput(null);
        setScore(null);
        try {
            const res = await api.post('/web-contest/submit', {
                problemId, editableFiles, language
            });
            const subId = res.data?.id;
            activeSubRef.current = subId;
            setSubmitCount(c => c + 1);
            pollVerdict(subId);
        } catch (err) {
            setSubmitting(false);
            setOutput(
                <span style={{ color: C.error, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px' }}>
                    ❌ {err.response?.data?.message || err.message || 'Submit failed'}
                </span>
            );
        }
    };

    // ── File change handler ───────────────────────────────────────────────────
    const handleEditorChange = (value) => {
        if (!activeFile || readonlyFiles[activeFile] !== undefined) return;
        setEditableFiles(prev => ({ ...prev, [activeFile]: value || '' }));
    };

    // ── Derived values ────────────────────────────────────────────────────────
    const isReadonly = activeFile ? readonlyFiles[activeFile] !== undefined : true;
    const activeContent = activeFile
        ? (editableFiles[activeFile] ?? readonlyFiles[activeFile] ?? '')
        : '';
    const monacoLang = detectLanguage(activeFile);
    const allFiles = [
        ...Object.keys(editableFiles).map(f => ({ path: f, editable: true })),
        ...Object.keys(readonlyFiles).map(f => ({ path: f, editable: false })),
    ];

    // ── Loading state ─────────────────────────────────────────────────────────
    if (loading) {
        return <SkeletonLoader fullScreen />;
    }

    // ── Render ────────────────────────────────────────────────────────────────
    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: C.bg, color: C.onBg, overflow: 'hidden' }}>
            {/* ── Top bar: Problem title + description (collapsible) ─────────── */}
            <div style={{ borderBottom: `1px solid ${C.outline}`, background: C.surface }}>
                <button
                    onClick={() => setDescOpen(!descOpen)}
                    style={{ width: '100%', display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 16px', border: 'none', background: 'none', color: C.onBg, cursor: 'pointer', textAlign: 'left' }}
                >
                    <span style={{ fontSize: '16px', fontWeight: 600 }}>{title}</span>
                    <span style={{ color: C.muted, fontSize: '12px', marginLeft: 'auto' }}>
                        {descOpen ? '▲ Hide' : '▼ Show'} Description
                    </span>
                </button>
                {descOpen && (
                    <div style={{ padding: '0 16px 12px', fontSize: '13px', color: C.muted, lineHeight: 1.5, maxHeight: '150px', overflowY: 'auto', whiteSpace: 'pre-wrap' }}>
                        {description || 'No description available.'}
                    </div>
                )}
            </div>

            {/* ── Main area: file tree + editor ────────────────────────────────── */}
            <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
                {/* ── File tree sidebar ─────────────────────────────────────────── */}
                <div style={{ width: '200px', minWidth: '200px', background: C.surfaceAlt, borderRight: `1px solid ${C.outline}`, overflowY: 'auto', padding: '8px 0' }}>
                    <div style={{ padding: '4px 12px 8px', fontSize: '10px', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.muted }}>
                        Files
                    </div>
                    {allFiles.map(({ path, editable }) => {
                        const filename = path.split('/').pop();
                        const isActive = path === activeFile;
                        return (
                            <button
                                key={path}
                                onClick={() => setActiveFile(path)}
                                title={path}
                                style={{
                                    display: 'flex', alignItems: 'center', gap: '6px',
                                    width: '100%', padding: '6px 12px', border: 'none',
                                    background: isActive ? C.surface : 'transparent',
                                    color: isActive ? C.primary : C.onBg,
                                    cursor: 'pointer', textAlign: 'left',
                                    fontSize: '12px', fontFamily: "'JetBrains Mono', monospace",
                                    borderLeft: isActive ? `2px solid ${C.primary}` : '2px solid transparent',
                                }}
                            >
                                <span style={{ fontSize: '14px' }}>{editable ? '✏️' : '🔒'}</span>
                                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{filename}</span>
                            </button>
                        );
                    })}
                </div>

                {/* ── Monaco editor ────────────────────────────────────────────── */}
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                    {/* Tab bar */}
                    {activeFile && (
                        <div style={{ display: 'flex', alignItems: 'center', padding: '0 12px', height: '32px', background: C.surface, borderBottom: `1px solid ${C.outline}`, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace" }}>
                            <span style={{ color: C.primary }}>{activeFile.split('/').pop()}</span>
                            {isReadonly && <span style={{ marginLeft: '8px', fontSize: '10px', color: C.muted, background: C.outline, padding: '1px 6px', borderRadius: '3px' }}>READ-ONLY</span>}
                            <span style={{ marginLeft: 'auto', fontSize: '10px', color: C.muted }}>{monacoLang}</span>
                        </div>
                    )}
                    <div style={{ flex: 1 }}>
                        <Editor
                            key={activeFile}
                            height="100%"
                            language={monacoLang}
                            value={activeContent}
                            onChange={handleEditorChange}
                            theme="vs-dark"
                            options={{
                                readOnly: isReadonly,
                                minimap: { enabled: false },
                                fontSize: 13,
                                fontFamily: "'JetBrains Mono', monospace",
                                scrollBeyondLastLine: false,
                                padding: { top: 12 },
                                lineNumbers: 'on',
                                renderLineHighlight: 'all',
                                automaticLayout: true,
                            }}
                        />
                    </div>
                </div>
            </div>

            {/* ── Output panel (bottom) ────────────────────────────────────────── */}
            <div style={{ height: '150px', minHeight: '100px', background: C.surface, borderTop: `1px solid ${C.outline}`, display: 'flex', flexDirection: 'column' }}>
                {/* Output content */}
                <div style={{ flex: 1, overflowY: 'auto', padding: '10px 16px' }}>
                    {(running || submitting) && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span style={{ color: C.secondary, fontSize: '14px', animation: 'spin 1s linear infinite' }}>⟳</span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>
                                {running ? 'Running tests...' : 'Submitting...'}
                            </span>
                            <style>{`@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}`}</style>
                        </div>
                    )}
                    {!running && !submitting && output}
                    {!running && !submitting && !output && (
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>
                            Output will appear here after Run or Submit.
                        </span>
                    )}
                </div>

                {/* Action bar */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '8px 16px', borderTop: `1px solid ${C.outline}` }}>
                    <button
                        onClick={handleRun}
                        disabled={running || submitting}
                        style={{
                            padding: '6px 16px', border: `1px solid ${C.secondary}`,
                            background: 'transparent', color: C.secondary,
                            fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
                            fontWeight: 600, cursor: running || submitting ? 'not-allowed' : 'pointer',
                            opacity: running || submitting ? 0.5 : 1,
                            borderRadius: '4px',
                        }}
                    >
                        Run ({runCount}/10)
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={running || submitting}
                        style={{
                            padding: '6px 16px', border: 'none',
                            background: C.primary, color: '#000',
                            fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
                            fontWeight: 600, cursor: running || submitting ? 'not-allowed' : 'pointer',
                            opacity: running || submitting ? 0.5 : 1,
                            borderRadius: '4px',
                        }}
                    >
                        Submit ({submitCount}/5)
                    </button>
                    {score !== null && (
                        <span style={{ marginLeft: 'auto', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', fontWeight: 600, color: score === 100 ? C.success : score >= 50 ? C.warning : C.error }}>
                            Score: {score}
                        </span>
                    )}
                </div>
            </div>
        </div>
    );
};

export default WebContest;
