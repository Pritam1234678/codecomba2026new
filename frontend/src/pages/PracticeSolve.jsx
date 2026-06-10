import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import PracticeService from '../services/practice.service';
import ProblemService from '../services/problem.service';

// ── Design tokens ─────────────────────────────────────────────────────────────
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
    success:    '#4ade80',
};

const LANG_MAP = {
    JAVA:       { label: 'Java 21',     monaco: 'java' },
    CPP:        { label: 'C++ 20',      monaco: 'cpp' },
    C:          { label: 'C',           monaco: 'c' },
    PYTHON:     { label: 'Python 3.11', monaco: 'python' },
    JAVASCRIPT: { label: 'JavaScript',  monaco: 'javascript' },
};

const DIFF_CFG = {
    EASY:   { color: C.success,   label: 'Easy' },
    MEDIUM: { color: C.secondary, label: 'Medium' },
    HARD:   { color: C.error,     label: 'Hard' },
};

// ── Verdict renderer ──────────────────────────────────────────────────────────
function buildVerdictUI(v) {
    if (v.status === 'ERROR') {
        return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '18px', color: C.error, fontVariationSettings: "'FILL' 1" }}>error</span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', fontWeight: 600, color: C.error }}>Error</span>
                </div>
                <pre style={{ backgroundColor: 'rgba(0,0,0,0.4)', border: `1px solid ${C.error}30`, padding: '12px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: '#fca5a5', whiteSpace: 'pre-wrap', overflowX: 'auto' }}>
                    {v.errorMessage || 'Unknown error'}
                </pre>
            </div>
        );
    }

    if (v.status === 'CE' || v.status === 'RE') {
        const isCE = v.status === 'CE';
        return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '18px', color: isCE ? C.secondary : C.error, fontVariationSettings: "'FILL' 1" }}>
                        {isCE ? 'code_off' : 'error'}
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', fontWeight: 600, color: isCE ? C.secondary : C.error }}>
                        {isCE ? 'Compilation Error' : 'Runtime Error'}
                    </span>
                </div>
                <pre style={{ backgroundColor: 'rgba(0,0,0,0.4)', border: `1px solid ${C.error}30`, padding: '12px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: '#fca5a5', whiteSpace: 'pre-wrap', overflowX: 'auto' }}>
                    {v.errorMessage || 'No error details available'}
                </pre>
            </div>
        );
    }

    if (v.status === 'TLE') {
        return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '18px', color: '#facc15', fontVariationSettings: "'FILL' 1" }}>hourglass_empty</span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', fontWeight: 600, color: '#facc15' }}>Time Limit Exceeded</span>
                </div>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>Execution exceeded the time limit</span>
            </div>
        );
    }

    const tcs = v.testCases || [];
    const visibleTCs = tcs.filter(tc => !tc.hidden);
    const hiddenTCs  = tcs.filter(tc => tc.hidden);
    const hiddenPassed = hiddenTCs.filter(tc => tc.passed).length;
    const isAC = v.status === 'AC';
    const statusColor = isAC ? C.success : C.error;

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {/* Verdict header */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '18px', color: statusColor, fontVariationSettings: "'FILL' 1" }}>
                        {isAC ? 'check_circle' : 'cancel'}
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', fontWeight: 600, color: statusColor }}>
                        {isAC ? 'Accepted' : 'Wrong Answer'}
                    </span>
                </div>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', fontWeight: 600, color: statusColor }}>
                    {v.passed}/{v.total} passed
                </span>
            </div>

            {/* Progress bar */}
            <div style={{ height: '3px', backgroundColor: C.surfaceHi }}>
                <div style={{ height: '100%', width: `${v.total > 0 ? (v.passed / v.total) * 100 : 0}%`, backgroundColor: statusColor, transition: 'width 0.6s ease' }} />
            </div>

            {/* Points celebration on AC */}
            {isAC && v.pointsAwarded > 0 && (
                <div style={{ padding: '10px 14px', backgroundColor: 'rgba(74,222,128,0.08)', border: `1px solid ${C.success}40`, display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '20px', color: C.success, fontVariationSettings: "'FILL' 1" }}>workspace_premium</span>
                    <div>
                        <div style={{ fontFamily: "'Playfair Display', serif", fontSize: '15px', color: C.success, fontWeight: 600 }}>
                            +{v.pointsAwarded} Points Earned
                        </div>
                        <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.06em' }}>
                            FIRST SOLVE — POINTS ADDED TO YOUR PROFILE
                        </div>
                    </div>
                </div>
            )}
            {isAC && v.alreadySolved && (
                <div style={{ padding: '8px 12px', backgroundColor: 'rgba(233,193,118,0.06)', border: `1px solid ${C.secondary}30`, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.secondary }}>
                    ✓ Already solved earlier — no additional points awarded
                </div>
            )}

            {/* Execution time */}
            {v.executionTime > 0 && (
                <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                    ⏱ Execution time: {v.executionTime}ms
                </div>
            )}

            {/* Visible test cases */}
            {visibleTCs.length > 0 && (
                <div style={{ borderTop: `1px solid ${C.border}`, paddingTop: '10px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.1em', textTransform: 'uppercase', marginBottom: '4px' }}>
                        Test Cases
                    </span>
                    {visibleTCs.map((tc, idx) => (
                        <div key={idx} style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', padding: '6px 10px', backgroundColor: tc.passed ? 'rgba(74,222,128,0.05)' : 'rgba(255,180,171,0.05)', border: `1px solid ${tc.passed ? 'rgba(74,222,128,0.15)' : 'rgba(255,180,171,0.15)'}` }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <span style={{ color: tc.passed ? C.success : C.error, fontWeight: 600, minWidth: '16px' }}>
                                    {tc.passed ? '✓' : '✗'}
                                </span>
                                <span style={{ color: C.muted }}>Test Case {tc.testCase}</span>
                                <span style={{ marginLeft: 'auto', color: tc.passed ? C.success : C.error, fontSize: '11px' }}>
                                    {tc.passed ? 'PASS' : 'FAIL'}
                                </span>
                            </div>
                            {!tc.passed && (tc.input || tc.expected || tc.got) && (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', marginLeft: '26px', fontSize: '11px', color: C.outline }}>
                                    {tc.input    && <span><span style={{ color: C.muted   }}>Input:</span> {tc.input}</span>}
                                    {tc.expected && <span><span style={{ color: C.success }}>Expected:</span> {tc.expected}</span>}
                                    {tc.got      && <span><span style={{ color: C.error   }}>Your Output:</span> {tc.got}</span>}
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}

            {/* Hidden test cases */}
            {hiddenTCs.length > 0 && (
                <div style={{ borderTop: `1px solid ${C.border}`, paddingTop: '8px', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, fontStyle: 'italic' }}>
                    🔒 {hiddenTCs.length} hidden — <span style={{ color: C.success }}>{hiddenPassed} passed</span>, <span style={{ color: C.error }}>{hiddenTCs.length - hiddenPassed} failed</span>
                </div>
            )}

            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: '#7ab3e080', borderTop: `1px solid ${C.border}`, paddingTop: '8px' }}>
                💡 Practice mode — independent from contest submissions
            </span>
        </div>
    );
}

// ── Main Component ────────────────────────────────────────────────────────────
const PracticeSolve = () => {
    const { id }   = useParams();
    const navigate = useNavigate();

    const [problem,  setProblem]  = useState(null);
    const [solved,   setSolved]   = useState(false);
    const [pointsAvailable, setPointsAvailable] = useState(0);
    const [snippets, setSnippets] = useState({});
    const [language, setLanguage] = useState(() => {
        try { return localStorage.getItem(`lang_practice_${id}`) || 'JAVA'; } catch { return 'JAVA'; }
    });
    const [code,     setCode]     = useState(() => {
        const lang = (() => { try { return localStorage.getItem(`lang_practice_${id}`) || 'JAVA'; } catch { return 'JAVA'; } })();
        try { return localStorage.getItem(`code_practice_${id}_${lang}`) || '// Write your code here\n'; } catch { return '// Write your code here\n'; }
    });
    const [output,   setOutput]   = useState(null);
    const [loading,  setLoading]  = useState(true);
    const [running,  setRunning]  = useState(false);

    // Layout state
    const [leftWidth, setLeftWidth]       = useState(42);
    const [isDragging, setIsDragging]     = useState(false);
    const [consoleHeight, setConsoleHeight] = useState(220);
    const [isDraggingH, setIsDraggingH]   = useState(false);
    const dragStartX = useRef(0);
    const dragStartW = useRef(0);
    const dragStartY = useRef(0);
    const dragStartH = useRef(0);
    const saveTimer  = useRef(null);

    // ── Persist code + language to localStorage (debounced) ──────────────────
    useEffect(() => {
        try { localStorage.setItem(`lang_practice_${id}`, language); } catch {}
        clearTimeout(saveTimer.current);
        saveTimer.current = setTimeout(() => {
            try { localStorage.setItem(`code_practice_${id}_${language}`, code); } catch {}
        }, 500);
        return () => clearTimeout(saveTimer.current);
    }, [code, language, id]);

    // ── Load problem + snippets ───────────────────────────────────────────────
    useEffect(() => {
        setLoading(true);
        setOutput(null);
        // Do NOT reset code/language — lazy initializers already restored from localStorage.
        setSnippets({});

        PracticeService.getProblem(id)
            .then(res => {
                setProblem(res.data.problem);
                setSolved(res.data.solved);
                setPointsAvailable(res.data.pointsAvailable);
            })
            .catch(err => {
                console.error('Practice problem load failed', err);
                setProblem(null);
            })
            .finally(() => setLoading(false));

        ProblemService.getSnippets(id)
            .then(res => {
                const map = {};
                res.data.forEach(s => { map[s.language] = s.starterCode; });
                setSnippets(map);
                // Only seed with snippet if the user has no REAL draft (ignore placeholder defaults).
                const isPlaceholder = (s) => !s || s.trim() === '' || s.trim() === '// Write your code here' || s.trim() === '// Write your code here\n';
                const savedLang = (() => { try { return localStorage.getItem(`lang_practice_${id}`); } catch { return null; } })();
                const activeLang = savedLang || 'JAVA';
                const savedCode = (() => { try { return localStorage.getItem(`code_practice_${id}_${activeLang}`); } catch { return null; } })();
                if (isPlaceholder(savedCode)) {
                    // No real draft — seed from snippet
                    if (map[activeLang]) { setCode(map[activeLang]); setLanguage(activeLang); }
                    else if (map['JAVA']) { setCode(map['JAVA']); setLanguage('JAVA'); }
                }
                // If savedCode is a real draft, lazy initializer already loaded it — do nothing.
            })
            .catch(() => {});
    }, [id]);

    const handleLanguageChange = (lang) => {
        setLanguage(lang);
        try {
            const saved = localStorage.getItem(`code_practice_${id}_${lang}`);
            if (saved) { setCode(saved); return; }
        } catch {}
        if (snippets[lang]) setCode(snippets[lang]);
        else setCode('');
    };

    const handleRun = () => {
        if (running) return;
        setRunning(true);
        setOutput(
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: '#facc15' }}>
                <span className="material-symbols-outlined" style={{ fontSize: '16px', animation: 'spin 1s linear infinite' }}>sync</span>
                Running tests...
            </div>
        );
        PracticeService.run(id, code, language)
            .then(res => {
                const v = res.data;
                setOutput(buildVerdictUI(v));
                if (v.status === 'AC') {
                    setSolved(true);
                }
            })
            .catch(err => {
                const msg = err.response?.data?.message || err.message || 'Backend unreachable';
                setOutput(<span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                    Error: {msg}
                </span>);
            })
            .finally(() => setRunning(false));
    };

    // ── Drag dividers ─────────────────────────────────────────────────────────
    const onDividerMouseDown = useCallback((e) => {
        e.preventDefault();
        setIsDragging(true);
        dragStartX.current = e.clientX;
        dragStartW.current = leftWidth;
    }, [leftWidth]);

    useEffect(() => {
        if (!isDragging) return;
        const onMove = (e) => {
            const container = document.getElementById('ps-workspace');
            if (!container) return;
            const totalW = container.getBoundingClientRect().width;
            const delta  = e.clientX - dragStartX.current;
            const newPct = Math.min(65, Math.max(25, dragStartW.current + (delta / totalW) * 100));
            setLeftWidth(newPct);
        };
        const onUp = () => setIsDragging(false);
        window.addEventListener('mousemove', onMove);
        window.addEventListener('mouseup', onUp);
        return () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp); };
    }, [isDragging]);

    const onConsoleDividerMouseDown = useCallback((e) => {
        e.preventDefault();
        setIsDraggingH(true);
        dragStartY.current = e.clientY;
        dragStartH.current = consoleHeight;
    }, [consoleHeight]);

    useEffect(() => {
        if (!isDraggingH) return;
        const onMove = (e) => {
            const delta = dragStartY.current - e.clientY;
            setConsoleHeight(Math.min(500, Math.max(80, dragStartH.current + delta)));
        };
        const onUp = () => setIsDraggingH(false);
        window.addEventListener('mousemove', onMove);
        window.addEventListener('mouseup', onUp);
        return () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp); };
    }, [isDraggingH]);

    // ── Loading / Error states ────────────────────────────────────────────────
    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', backgroundColor: C.bg, color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
            Loading problem...
        </div>
    );

    if (!problem) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', backgroundColor: C.bg }}>
            <div style={{ border: `1px solid ${C.border}`, padding: '3rem', maxWidth: '480px', width: '100%', textAlign: 'center', backgroundColor: C.surfaceLow }}>
                <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', color: C.error, marginBottom: '1rem' }}>Problem Not Found</h1>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.outline, marginBottom: '2rem' }}>
                    Problem ID: <span style={{ fontFamily: "'JetBrains Mono', monospace", color: C.error }}>{id}</span>
                </p>
                <button onClick={() => navigate('/practice')} style={{ padding: '10px 24px', border: `1px solid ${C.secondary}`, color: C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer' }}>
                    Back to Practice
                </button>
            </div>
        </div>
    );

    const diff = DIFF_CFG[problem.level] || { color: C.outline, label: problem.level || '—' };

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden', fontFamily: "'Geist', sans-serif", userSelect: isDragging || isDraggingH ? 'none' : 'auto' }}>

            {/* ── Top Header ── */}
            <header style={{ height: '56px', flexShrink: 0, borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px', zIndex: 10 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                    <button
                        onClick={() => navigate('/practice')}
                        style={{ display: 'flex', alignItems: 'center', gap: '6px', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', transition: 'color 0.2s' }}
                        onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                        onMouseLeave={e => e.currentTarget.style.color = C.outline}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                        Back to Practice
                    </button>
                    <div style={{ width: '1px', height: '24px', backgroundColor: C.border }} />
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '18px', fontWeight: 600, color: C.primary, maxWidth: '400px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', margin: 0 }}>
                        {problem.title}
                    </h1>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                    {/* Mode badge */}
                    <span style={{ padding: '4px 10px', border: `1px solid ${C.secondary}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.secondary, textTransform: 'uppercase' }}>
                        Practice Mode
                    </span>

                    {/* Limits */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', border: `1px solid ${C.border}`, padding: '6px 12px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.outline }}>timer</span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>{problem.timeLimit}s</span>
                        </div>
                        <div style={{ width: '1px', height: '12px', backgroundColor: C.border }} />
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.outline }}>memory</span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.muted }}>{problem.memoryLimit}MB</span>
                        </div>
                    </div>

                    {/* Points */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: solved ? C.success : C.secondary }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '14px', fontVariationSettings: "'FILL' 1" }}>
                            {solved ? 'check_circle' : 'workspace_premium'}
                        </span>
                        {solved ? 'Solved' : `+${pointsAvailable} pts`}
                    </div>
                </div>
            </header>

            {/* ── Workspace ── */}
            <main id="ps-workspace" style={{ flex: 1, display: 'flex', overflow: 'hidden', cursor: isDragging ? 'col-resize' : 'auto' }}>

                {/* ── Left Pane: Problem Statement ── */}
                <section style={{ width: `${leftWidth}%`, flexShrink: 0, backgroundColor: C.surfaceLow, borderRight: `1px solid ${C.border}`, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
                    <div style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>

                        {/* Difficulty + status */}
                        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                            <span style={{ padding: '3px 10px', border: `1px solid ${diff.color}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: diff.color, textTransform: 'uppercase' }}>
                                {diff.label}
                            </span>
                            {solved && (
                                <span style={{ padding: '3px 10px', border: `1px solid ${C.success}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.success, textTransform: 'uppercase' }}>
                                    ✓ Solved
                                </span>
                            )}
                        </div>

                        {/* Description */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '16px', lineHeight: 1.7, color: C.onBg, margin: 0, whiteSpace: 'pre-wrap' }}>
                                {problem.description}
                            </p>
                        </div>

                        {/* Input / Output format */}
                        {problem.inputFormat && (
                            <div>
                                <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.primary, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '6px', marginBottom: '10px' }}>
                                    Input Format
                                </h3>
                                <pre style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.muted, whiteSpace: 'pre-wrap', backgroundColor: C.surfaceCon, padding: '12px', border: `1px solid ${C.border}` }}>
                                    {problem.inputFormat}
                                </pre>
                            </div>
                        )}
                        {problem.outputFormat && (
                            <div>
                                <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.primary, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '6px', marginBottom: '10px' }}>
                                    Output Format
                                </h3>
                                <pre style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.muted, whiteSpace: 'pre-wrap', backgroundColor: C.surfaceCon, padding: '12px', border: `1px solid ${C.border}` }}>
                                    {problem.outputFormat}
                                </pre>
                            </div>
                        )}

                        {/* Examples */}
                        {[problem.example1, problem.example2, problem.example3].filter(Boolean).length > 0 && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.primary, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '6px' }}>
                                    Examples
                                </h3>
                                {[problem.example1, problem.example2, problem.example3].filter(Boolean).map((ex, i) => (
                                    <div key={i} style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceCon, padding: '1rem' }}>
                                        <pre style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.muted, whiteSpace: 'pre-wrap', margin: 0 }}>
                                            {ex}
                                        </pre>
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* Constraints */}
                        {problem.constraints && (
                            <div>
                                <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.primary, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '6px', marginBottom: '10px' }}>
                                    Constraints
                                </h3>
                                <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                    {problem.constraints.split('\n').filter(Boolean).map((c, i) => (
                                        <li key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                            <span style={{ width: '4px', height: '4px', borderRadius: '50%', backgroundColor: C.border, flexShrink: 0 }} />
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>{c}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}

                        {/* Images */}
                        {problem.images && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                {problem.images.split(',').map((url, i) => {
                                    const trimmed = url.trim();
                                    const driveMatch = trimmed.match(/\/file\/d\/([^/]+)/);
                                    const imgUrl = driveMatch ? `https://drive.google.com/uc?export=view&id=${driveMatch[1]}` : trimmed;
                                    return (
                                        <img key={i} src={imgUrl} alt={`Example ${i + 1}`}
                                            style={{ width: '100%', border: `1px solid ${C.border}`, objectFit: 'contain' }}
                                            onError={e => { e.target.style.display = 'none'; }}
                                        />
                                    );
                                })}
                            </div>
                        )}
                    </div>
                </section>

                {/* Drag Divider */}
                <div
                    onMouseDown={onDividerMouseDown}
                    style={{ width: '4px', flexShrink: 0, backgroundColor: isDragging ? C.secondary : C.border, cursor: 'col-resize', transition: 'background-color 0.2s' }}
                    onMouseEnter={e => { if (!isDragging) e.currentTarget.style.backgroundColor = C.secondary; }}
                    onMouseLeave={e => { if (!isDragging) e.currentTarget.style.backgroundColor = C.border; }}
                />

                {/* ── Right Pane: Editor + Console ── */}
                <section style={{ flex: 1, display: 'flex', flexDirection: 'column', backgroundColor: C.bg, minWidth: 0, cursor: isDraggingH ? 'row-resize' : 'auto' }}>

                    {/* Editor toolbar */}
                    <div style={{ height: '48px', flexShrink: 0, borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 16px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '18px', color: C.outline }}>code_blocks</span>
                            <select
                                value={language}
                                onChange={e => handleLanguageChange(e.target.value)}
                                style={{ backgroundColor: 'transparent', border: 'none', color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: 'pointer', outline: 'none', appearance: 'none' }}
                            >
                                {Object.entries(LANG_MAP).map(([key, { label }]) => (
                                    <option key={key} value={key} style={{ backgroundColor: C.surfaceLow }}>{label}</option>
                                ))}
                            </select>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <button
                                onClick={() => { if (snippets[language]) setCode(snippets[language]); }}
                                title="Reset to starter code"
                                style={{ padding: '4px', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', transition: 'color 0.2s' }}
                                onMouseEnter={e => e.currentTarget.style.color = C.muted}
                                onMouseLeave={e => e.currentTarget.style.color = C.outline}
                            >
                                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>refresh</span>
                            </button>
                        </div>
                    </div>

                    {/* Monaco Editor */}
                    <div style={{ flex: 1, overflow: 'hidden' }}>
                        <Editor
                            height="100%"
                            theme="vs-dark"
                            language={LANG_MAP[language]?.monaco || 'java'}
                            value={code}
                            onChange={v => setCode(v || '')}
                            options={{
                                fontSize: 14,
                                fontFamily: "'Fira Code', 'Cascadia Code', 'JetBrains Mono', monospace",
                                fontLigatures: true,
                                lineHeight: 22,
                                minimap: { enabled: false },
                                scrollBeyondLastLine: false,
                                automaticLayout: true,
                                folding: true,
                                bracketPairColorization: { enabled: true },
                                matchBrackets: 'always',
                                autoClosingBrackets: 'always',
                                autoClosingQuotes: 'always',
                                autoIndent: 'full',
                                formatOnPaste: true,
                                formatOnType: true,
                                tabSize: 4,
                                insertSpaces: true,
                                lineNumbers: 'on',
                                renderLineHighlight: 'all',
                                scrollbar: { vertical: 'visible', horizontal: 'visible', verticalScrollbarSize: 8, horizontalScrollbarSize: 8 },
                                padding: { top: 16, bottom: 16 },
                                rulers: [80, 120],
                                wordWrap: 'off',
                                stickyScroll: { enabled: true },
                                mouseWheelZoom: true,
                                cursorBlinking: 'smooth',
                                cursorSmoothCaretAnimation: 'on',
                                snippetSuggestions: 'top',
                                glyphMargin: false,
                            }}
                        />
                    </div>

                    {/* Console drag handle */}
                    <div
                        onMouseDown={onConsoleDividerMouseDown}
                        style={{ height: '4px', flexShrink: 0, backgroundColor: isDraggingH ? C.secondary : C.border, cursor: 'row-resize', transition: 'background-color 0.2s' }}
                        onMouseEnter={e => { if (!isDraggingH) e.currentTarget.style.backgroundColor = C.secondary; }}
                        onMouseLeave={e => { if (!isDraggingH) e.currentTarget.style.backgroundColor = C.border; }}
                    />

                    {/* Console panel */}
                    <div style={{ height: `${consoleHeight}px`, flexShrink: 0, backgroundColor: C.surfaceLow, display: 'flex', flexDirection: 'column' }}>
                        {/* Console tabs */}
                        <div style={{ height: '40px', flexShrink: 0, borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, display: 'flex', alignItems: 'center', padding: '0 16px', gap: '24px' }}>
                            <span style={{ height: '100%', display: 'flex', alignItems: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', color: C.secondary, borderBottom: `2px solid ${C.secondary}`, paddingTop: '2px' }}>
                                Output Console
                            </span>
                        </div>

                        {/* Console body */}
                        <div style={{ flex: 1, overflowY: 'auto', padding: '1rem' }}>
                            {output
                                ? output
                                : <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, fontStyle: 'italic' }}>Run your code to see results...</span>
                            }
                        </div>
                    </div>

                    {/* Bottom action bar */}
                    <div style={{ height: '56px', flexShrink: 0, borderTop: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 20px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>terminal</span>
                            Console
                        </div>

                        <button
                            onClick={handleRun}
                            disabled={running}
                            style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '10px 28px', border: `1px solid ${C.secondary}`, color: running ? C.outline : C.bg, backgroundColor: running ? 'transparent' : C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: running ? 'not-allowed' : 'pointer', opacity: running ? 0.5 : 1, transition: 'all 0.2s' }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '16px', fontVariationSettings: "'FILL' 1", animation: running ? 'spin 1s linear infinite' : 'none' }}>
                                {running ? 'sync' : 'play_arrow'}
                            </span>
                            {running ? 'Running...' : 'Run'}
                        </button>
                    </div>
                </section>
            </main>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
                ::-webkit-scrollbar { width: 4px; height: 4px; }
                ::-webkit-scrollbar-track { background: transparent; }
                ::-webkit-scrollbar-thumb { background: #50453b; border-radius: 2px; }
                ::-webkit-scrollbar-thumb:hover { background: #9d8e83; }
            `}</style>
        </div>
    );
};

export default PracticeSolve;
