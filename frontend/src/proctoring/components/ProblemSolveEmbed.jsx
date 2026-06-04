import { useState, useEffect, useRef, useCallback } from 'react';
import Editor from '@monaco-editor/react';
import api from '../../services/api';
import ProblemService from '../../services/problem.service';
import SubmissionService from '../../services/submission.service';
import AuthService from '../../services/auth.service';

// ── Design tokens (Practice palette — must match ProblemSolve.jsx) ──────────
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
    JAVA:       { label: 'Java 21',       monaco: 'java' },
    CPP:        { label: 'C++ 20',        monaco: 'cpp' },
    C:          { label: 'C',             monaco: 'c' },
    PYTHON:     { label: 'Python 3.11',   monaco: 'python' },
    JAVASCRIPT: { label: 'JavaScript',    monaco: 'javascript' },
};

const DIFF_CFG = {
    EASY:   { color: C.success,   label: 'Easy' },
    MEDIUM: { color: C.secondary, label: 'Medium' },
    HARD:   { color: C.error,     label: 'Hard' },
};

// ── Verdict renderer (pure function → returns JSX). Mirrors the version
//    in ProblemSolve.jsx so the verdict UI feels identical across surfaces.
function buildVerdictUI(sub, isTestRun) {
    if (sub.status === 'CE' || sub.status === 'RE') {
        const isCE = sub.status === 'CE';
        return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '18px', color: isCE ? C.secondary : C.error, fontVariationSettings: "'FILL' 1" }}>
                        {isCE ? 'code_off' : 'error'}
                    </span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', fontWeight: 600, color: isCE ? C.secondary : C.error }}>
                        {isCE ? 'Compilation Error' : 'Runtime Error'}
                    </span>
                </div>
                <pre style={{ backgroundColor: 'rgba(0,0,0,0.4)', border: `1px solid ${C.error}30`, padding: '12px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: '#fca5a5', whiteSpace: 'pre-wrap', overflowX: 'auto', borderRadius: '2px' }}>
                    {sub.errorMessage || 'No error details available'}
                </pre>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: isTestRun ? '#facc1580' : '#7ab3e080' }}>
                    {isTestRun ? '💡 Test run — not saved' : '✓ Submission saved'}
                </span>
            </div>
        );
    }

    if (sub.status === 'TLE') {
        return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '18px', color: '#facc15', fontVariationSettings: "'FILL' 1" }}>hourglass_empty</span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', fontWeight: 600, color: '#facc15' }}>Time Limit Exceeded</span>
                </div>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>0 test cases passed — execution exceeded time limit</span>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: isTestRun ? '#facc1580' : '#7ab3e080' }}>
                    {isTestRun ? '💡 Test run — not saved' : '✓ Submission saved'}
                </span>
            </div>
        );
    }

    const testCaseDetails = sub.testCaseDetails ? JSON.parse(sub.testCaseDetails) : [];
    const visibleTCs   = testCaseDetails.filter(tc => !tc.hidden);
    const hiddenTCs    = testCaseDetails.filter(tc => tc.hidden);
    const hiddenPassed = hiddenTCs.filter(tc => tc.status === 'PASS').length;
    const total  = sub.totalTestCases || 0;
    const passed = sub.testCasesPassed || 0;
    const isAC   = sub.status === 'AC';
    const statusColor = isAC ? C.success : C.error;

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
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
                    {passed}/{total} passed
                </span>
            </div>

            <div style={{ height: '3px', backgroundColor: C.surfaceHi, borderRadius: '2px' }}>
                <div style={{ height: '100%', width: `${total > 0 ? (passed / total) * 100 : 0}%`, backgroundColor: statusColor, borderRadius: '2px', transition: 'width 0.8s ease' }} />
            </div>

            {(sub.timeConsumedMs > 0 || sub.timeConsumed > 0) && (
                <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                    ⏱ Execution time: {sub.timeConsumedMs || sub.timeConsumed}ms
                </div>
            )}

            {visibleTCs.length > 0 && (
                <div style={{ borderTop: `1px solid ${C.border}`, paddingTop: '10px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.1em', textTransform: 'uppercase', marginBottom: '4px' }}>
                        Test Cases
                    </span>
                    {visibleTCs.map((tc, idx) => (
                        <div key={idx} style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', padding: '6px 10px', backgroundColor: tc.status === 'PASS' ? 'rgba(74,222,128,0.05)' : 'rgba(255,180,171,0.05)', border: `1px solid ${tc.status === 'PASS' ? 'rgba(74,222,128,0.15)' : 'rgba(255,180,171,0.15)'}` }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <span style={{ color: tc.status === 'PASS' ? C.success : C.error, fontWeight: 600, minWidth: '16px' }}>
                                    {tc.status === 'PASS' ? '✓' : '✗'}
                                </span>
                                <span style={{ color: C.muted }}>Test Case {tc.testCase}</span>
                                <span style={{ marginLeft: 'auto', color: tc.status === 'PASS' ? C.success : C.error, fontSize: '11px' }}>
                                    {tc.status === 'PASS' ? 'PASS' : 'FAIL'}
                                </span>
                            </div>
                            {tc.status !== 'PASS' && (tc.input || tc.expected || tc.got) && (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', marginLeft: '26px', fontSize: '11px', color: C.outline }}>
                                    {tc.input && <span><span style={{ color: C.muted }}>Input:</span> {tc.input}</span>}
                                    {tc.expected && <span><span style={{ color: C.success }}>Expected:</span> {tc.expected}</span>}
                                    {tc.got && <span><span style={{ color: C.error }}>Your Output:</span> {tc.got}</span>}
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}

            {hiddenTCs.length > 0 && (
                <div style={{ borderTop: `1px solid ${C.border}`, paddingTop: '8px', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, fontStyle: 'italic' }}>
                    🔒 {hiddenTCs.length} hidden — <span style={{ color: C.success }}>{hiddenPassed} passed</span>, <span style={{ color: C.error }}>{hiddenTCs.length - hiddenPassed} failed</span>
                </div>
            )}

            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: isTestRun ? '#facc1580' : '#7ab3e080', borderTop: `1px solid ${C.border}`, paddingTop: '8px' }}>
                {isTestRun ? '💡 Test run — not saved' : '✓ Submission saved'}
            </span>
        </div>
    );
}

// ── Embedded Problem Solver ─────────────────────────────────────────────────
//
// A trimmed twin of `pages/ProblemSolve.jsx` for use INSIDE the proctored
// contest arena. It takes a `problemId` prop instead of a route param and
// drops the page-level chrome:
//
//   • No top header / back button — the arena owns the contest header.
//   • No prev/next navigation — the arena's left rail handles selection.
//   • No fullscreen contest-deactivated modal — the arena handles lifecycle.
//   • Root is a flex column that fills its container (no `100vh` lock).
//
// Everything inside the workspace (Monaco editor, problem statement,
// run/submit, verdict console) is preserved unchanged so candidates get
// the exact same problem-solving experience they get in practice mode.
//
// `useCopyPasteBlocker` already runs at the arena level. Monaco is
// whitelisted by that hook (`.monaco-editor` selector skip), so editor
// copy/paste keeps working inside this component without any extra wiring.
//
// `onSubmissionComplete(problemId)` — optional callback fired after a
// non-test submission finalises (any terminal verdict). The arena uses
// it to refresh per-problem submission state in the left rail.
//
// Props:
//   problemId               number | string  required
//   onSubmissionComplete?   (problemId) => void
//   proctored?              boolean — when true, disables ALL copy/paste/cut
//                           inside Monaco including Ctrl+C/V/Z and right-click
//
const ProblemSolveEmbed = ({ problemId, onSubmissionComplete, proctored = false }) => {
    const id = problemId;

    // ── Core state ────────────────────────────────────────────────────────────
    const [problem,    setProblem]    = useState(null);
    const [code,       setCode]       = useState('// Write your code here\n');
    const [language,   setLanguage]   = useState('JAVA');
    const [output,     setOutput]     = useState(null);
    const [loading,    setLoading]    = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [running,    setRunning]    = useState(false);
    const [hasExistingSubmission, setHasExistingSubmission] = useState(false);
    const [snippets,   setSnippets]   = useState({});

    // ── UI state ──────────────────────────────────────────────────────────────
    const [consoleTab,    setConsoleTab]    = useState('output');
    const [leftWidth,     setLeftWidth]     = useState(42);       // % of total width
    const [isDragging,    setIsDragging]    = useState(false);
    const [consoleHeight, setConsoleHeight] = useState(220);      // px
    const [isDraggingH,   setIsDraggingH]   = useState(false);

    const sseRef       = useRef(null);
    const dragStartX   = useRef(0);
    const dragStartW   = useRef(0);
    const dragStartY   = useRef(0);
    const dragStartH   = useRef(0);
    const runningRef   = useRef(false);
    const workspaceRef = useRef(null);

    // Stable callback ref so the SSE / poll handlers always see the latest
    // `onSubmissionComplete` without re-subscribing.
    const onSubCompleteRef = useRef(onSubmissionComplete);
    useEffect(() => { onSubCompleteRef.current = onSubmissionComplete; }, [onSubmissionComplete]);

    useEffect(() => { runningRef.current = running; }, [running]);

    // ── SSE connection (verdict push channel) ─────────────────────────────────
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
                        setSubmitting(false);
                        setRunning(false);
                        setOutput(buildVerdictUI(verdict, verdict.testRun === true));
                        if (verdict.testRun !== true && onSubCompleteRef.current) {
                            onSubCompleteRef.current(id);
                        }
                    } catch (err) {
                        // eslint-disable-next-line no-console
                        console.error('SSE parse error:', err);
                    }
                });

                es.onerror = () => { /* SSE auto-reconnect; polling fallback covers gap */ };
            })
            .catch(() => { /* fall back to polling */ });

        return () => {
            cancelled = true;
            if (es) { try { es.close(); } catch { /* ignored */ } }
            sseRef.current = null;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    // ── Load problem + snippets + existing submission ─────────────────────────
    useEffect(() => {
        if (id == null) return;
        let cancelled = false;
        setLoading(true);
        setHasExistingSubmission(false);
        setOutput(null);
        setCode('// Write your code here\n');
        setLanguage('JAVA');
        setSnippets({});

        api.get(`/problems/${id}`)
            .then(res => { if (!cancelled) { setProblem(res.data); setLoading(false); } })
            .catch(() => { if (!cancelled) setLoading(false); });

        ProblemService.getSnippets(id)
            .then(res => {
                if (cancelled) return;
                const map = {};
                res.data.forEach(s => { map[s.language] = s.starterCode; });
                setSnippets(map);

                SubmissionService.getUserSubmission(id)
                    .then(subRes => {
                        if (cancelled) return;
                        if (subRes.data) {
                            setHasExistingSubmission(true);
                            setCode(subRes.data.code);
                            setLanguage(subRes.data.language);
                            setOutput(
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 14px', backgroundColor: 'rgba(74,222,128,0.1)', border: `1px solid rgba(74,222,128,0.3)` }}>
                                        <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.success, fontVariationSettings: "'FILL' 1" }}>check_circle</span>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.success }}>
                                            Already Submitted — Status: {subRes.data.status}
                                        </span>
                                    </div>
                                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: '#facc1580' }}>
                                        ⚠ Resubmitting will replace your previous submission.
                                    </p>
                                </div>
                            );
                        } else {
                            if (map['JAVA']) { setCode(map['JAVA']); setLanguage('JAVA'); }
                        }
                    })
                    .catch(err => {
                        if (cancelled) return;
                        if (err.response?.status !== 404) {
                            // eslint-disable-next-line no-console
                            console.error(err);
                        }
                        if (map['JAVA']) { setCode(map['JAVA']); setLanguage('JAVA'); }
                    });
            })
            .catch(() => { /* ignored */ });

        return () => { cancelled = true; };
    }, [id]);

    // ── Drag: vertical divider ────────────────────────────────────────────────
    const onDividerMouseDown = useCallback((e) => {
        e.preventDefault();
        setIsDragging(true);
        dragStartX.current = e.clientX;
        dragStartW.current = leftWidth;
    }, [leftWidth]);

    useEffect(() => {
        if (!isDragging) return;
        const onMove = (e) => {
            const container = workspaceRef.current;
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

    // ── Drag: horizontal console divider ──────────────────────────────────────
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

    // ── Handlers ──────────────────────────────────────────────────────────────
    const handleLanguageChange = (lang) => {
        setLanguage(lang);
        if (snippets[lang]) setCode(snippets[lang]);
        else setCode('');
    };

    const handleRun = () => {
        setRunning(true);
        setOutput(
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: '#facc15' }}>
                <span className="material-symbols-outlined" style={{ fontSize: '16px', animation: 'spin 1s linear infinite' }}>sync</span>
                Running tests...
            </div>
        );
        SubmissionService.testCode(id, code, language)
            .then(res => {
                const submissionId = res.data?.id;
                if (submissionId) {
                    pollVerdict(submissionId, true);
                } else {
                    setRunning(false);
                    setOutput(<span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                        No submission ID returned from backend. Try again.
                    </span>);
                }
            })
            .catch(err => {
                setRunning(false);
                const msg = err.response?.data?.message || err.message || 'Backend unreachable';
                setOutput(<span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                    Error: {msg}
                </span>);
            });
    };

    const handleSubmit = () => {
        setSubmitting(true);
        setOutput(
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.secondary }}>
                <span className="material-symbols-outlined" style={{ fontSize: '16px', animation: 'spin 1s linear infinite' }}>sync</span>
                Submitting...
            </div>
        );
        SubmissionService.submitCode(id, code, language)
            .then(res => {
                const submissionId = res.data?.id;
                if (submissionId) {
                    pollVerdict(submissionId, false);
                } else {
                    setSubmitting(false);
                    setOutput(<span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                        No submission ID returned from backend. Try again.
                    </span>);
                }
            })
            .catch(err => {
                setSubmitting(false);
                if (err.response?.status === 429) {
                    setOutput(<span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: '#fb923c' }}>⚠ {err.response.data?.message || 'Too many submissions. Please wait.'}</span>);
                } else {
                    const msg = err.response?.data?.message || err.message || 'Backend unreachable';
                    setOutput(<span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                        Submission failed: {msg}
                    </span>);
                }
            });
    };

    // ── Polling fallback (covers SSE drops) ──────────────────────────────────
    const pollVerdict = (submissionId, isTestRun) => {
        const baseTimeout = problem?.timeLimit ? Math.ceil(problem.timeLimit) : 5;
        const wallClockCapMs = Math.min(120_000, Math.max(45_000, baseTimeout * 5_000 + 30_000));
        const intervalMs = 1_000;
        const maxAttempts = Math.ceil(wallClockCapMs / intervalMs);
        let attempts = 0;
        let cancelled = false;
        let consecutiveErrors = 0;

        const tick = async () => {
            if (cancelled) return;
            attempts++;
            try {
                const res = await api.get(`/submissions/${submissionId}/status`);
                const sub = res.data;
                consecutiveErrors = 0;
                const done = sub.status !== 'PENDING' && sub.status !== 'JUDGING';
                if (done) {
                    setSubmitting(false);
                    setRunning(false);
                    setOutput(buildVerdictUI({ ...sub, testRun: isTestRun }, isTestRun));
                    if (!isTestRun && onSubCompleteRef.current) {
                        onSubCompleteRef.current(id);
                    }
                    return;
                }
            } catch (err) {
                consecutiveErrors++;
                // eslint-disable-next-line no-console
                console.warn(`Poll attempt ${attempts} failed (${consecutiveErrors} consecutive):`, err.message);
                if (consecutiveErrors >= 5) {
                    setSubmitting(false);
                    setRunning(false);
                    setOutput(
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                            Lost connection to the judge. Reload the page; your submission is still queued.
                        </span>
                    );
                    return;
                }
            }

            if (attempts >= maxAttempts) {
                setSubmitting(false);
                setRunning(false);
                setOutput(
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                        Still judging — taking longer than expected. Refresh in a moment to see the verdict.
                    </span>
                );
                return;
            }
            setTimeout(tick, intervalMs);
        };

        setTimeout(tick, intervalMs);
        return () => { cancelled = true; };
    };

    // ── Loading / Error states ────────────────────────────────────────────────
    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', backgroundColor: C.bg, color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
            Loading problem…
        </div>
    );

    if (!problem) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', backgroundColor: C.bg, padding: '24px' }}>
            <div style={{ border: `1px solid ${C.border}`, padding: '2rem', maxWidth: '480px', width: '100%', textAlign: 'center', backgroundColor: C.surfaceLow }}>
                <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', color: C.error, margin: 0, marginBottom: '0.75rem' }}>Problem unavailable</h2>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.outline, margin: 0 }}>
                    Problem ID <span style={{ fontFamily: "'JetBrains Mono', monospace", color: C.error }}>{String(id)}</span> could not be loaded.
                </p>
            </div>
        </div>
    );

    const diff = DIFF_CFG[problem.level] || { color: C.outline, label: problem.level || '—' };

    // ── Main render ───────────────────────────────────────────────────────────
    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden', fontFamily: "'Geist', sans-serif", userSelect: isDragging || isDraggingH ? 'none' : 'auto' }}>

            {/* ── Workspace ── */}
            <main ref={workspaceRef} style={{ flex: 1, display: 'flex', overflow: 'hidden', cursor: isDragging ? 'col-resize' : 'auto', minHeight: 0 }}>

                {/* ── Left Pane: Problem Statement ── */}
                <section style={{ width: `${leftWidth}%`, flexShrink: 0, backgroundColor: C.surfaceLow, borderRight: `1px solid ${C.border}`, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
                    <div style={{ padding: '1.5rem 1.75rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>

                        {/* Title + difficulty + tags */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 600, color: C.primary, margin: 0, lineHeight: 1.25 }}>
                                {problem.title}
                            </h2>
                            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
                                <span style={{ padding: '3px 10px', border: `1px solid ${diff.color}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: diff.color, textTransform: 'uppercase' }}>
                                    {diff.label}
                                </span>
                                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', padding: '3px 10px', border: `1px solid ${C.border}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', color: C.outline }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>timer</span>
                                    {problem.timeLimit}s
                                </span>
                                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', padding: '3px 10px', border: `1px solid ${C.border}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', color: C.outline }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '12px' }}>memory</span>
                                    {problem.memoryLimit}MB
                                </span>
                                {hasExistingSubmission && (
                                    <span style={{ padding: '3px 10px', border: `1px solid ${C.success}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.success, textTransform: 'uppercase' }}>
                                        ✓ Submitted
                                    </span>
                                )}
                            </div>
                        </div>

                        {/* Description */}
                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', lineHeight: 1.7, color: C.onBg, margin: 0 }}>
                            {problem.description}
                        </p>

                        {/* Input / Output format */}
                        {problem.inputFormat && (
                            <div>
                                <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.primary, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '6px', marginBottom: '10px' }}>
                                    Input Format
                                </h3>
                                <pre style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.muted, whiteSpace: 'pre-wrap', backgroundColor: C.surfaceCon, padding: '12px', border: `1px solid ${C.border}`, margin: 0 }}>
                                    {problem.inputFormat}
                                </pre>
                            </div>
                        )}
                        {problem.outputFormat && (
                            <div>
                                <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.primary, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '6px', marginBottom: '10px' }}>
                                    Output Format
                                </h3>
                                <pre style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.muted, whiteSpace: 'pre-wrap', backgroundColor: C.surfaceCon, padding: '12px', border: `1px solid ${C.border}`, margin: 0 }}>
                                    {problem.outputFormat}
                                </pre>
                            </div>
                        )}

                        {/* Examples */}
                        {[problem.example1, problem.example2, problem.example3].filter(Boolean).length > 0 && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.primary, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '6px', margin: 0 }}>
                                    Examples
                                </h3>
                                {[problem.example1, problem.example2, problem.example3].filter(Boolean).map((ex, i) => (
                                    <div key={i} style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceCon, padding: '0.75rem' }}>
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

                {/* ── Drag Divider ── */}
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
                                onClick={() => { const lang = language; if (snippets[lang]) setCode(snippets[lang]); }}
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
                    <div style={{ flex: 1, overflow: 'hidden', minHeight: 0 }}>
                        <Editor
                            height="100%"
                            theme="vs-dark"
                            language={LANG_MAP[language]?.monaco || 'java'}
                            value={code}
                            onChange={v => setCode(v || '')}
                            onMount={(editor, monaco) => {
                                if (proctored) {
                                    // Nuke ALL clipboard / undo keybindings at
                                    // the Monaco level so Ctrl+C/V/X/Z do nothing.
                                    const nop = () => {};
                                    const block = [
                                        monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyC,
                                        monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyV,
                                        monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyX,
                                        monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyZ,
                                        monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.KeyZ,
                                        monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyA,
                                        monaco.KeyMod.CtrlCmd | monaco.KeyCode.Insert,
                                        monaco.KeyMod.Shift | monaco.KeyCode.Insert,
                                        monaco.KeyMod.CtrlCmd | monaco.KeyCode.F10,
                                    ];
                                    block.forEach(k => editor.addAction({
                                        id: `proctoring-block-${k}`,
                                        label: 'Blocked',
                                        keybindings: [k],
                                        run: nop,
                                    }));
                                }
                            }}
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
                                suggestOnTriggerCharacters: true,
                                quickSuggestions: { other: true, comments: true, strings: true },
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
                                // Proctored mode: disable ALL clipboard shortcuts
                                // inside the editor. The DOM-level useCopyPasteBlocker
                                // handles the page-level block; these Monaco-level
                                // options suppress the editor's own keybinding
                                // overrides for Ctrl+C / Ctrl+V / Ctrl+X / Ctrl+Z.
                                ...(proctored ? {
                                    contextmenu: false,
                                    quickSuggestions: false,
                                    suggest: { showWords: false, showSnippets: false },
                                    parameterHints: { enabled: false },
                                    wordBasedSuggestions: 'off',
                                    selectionClipboard: false,
                                    copyWithSyntaxHighlighting: false,
                                } : {}),
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
                        <div style={{ height: '40px', flexShrink: 0, borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, display: 'flex', alignItems: 'center', padding: '0 16px', gap: '24px' }}>
                            <button
                                onClick={() => setConsoleTab('output')}
                                style={{ height: '100%', display: 'flex', alignItems: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', color: consoleTab === 'output' ? C.secondary : C.outline, background: 'none', border: 'none', borderBottom: consoleTab === 'output' ? `2px solid ${C.secondary}` : '2px solid transparent', cursor: 'pointer', paddingTop: '2px' }}
                            >
                                Output Console
                            </button>
                        </div>

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

                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <button
                                onClick={handleRun}
                                disabled={running || submitting}
                                style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 20px', border: 'none', borderBottom: `1px solid transparent`, color: running || submitting ? C.outline : C.onBg, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: running || submitting ? 'not-allowed' : 'pointer', opacity: running || submitting ? 0.5 : 1, transition: 'all 0.2s' }}
                                onMouseEnter={e => { if (!running && !submitting) { e.currentTarget.style.borderBottomColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                                onMouseLeave={e => { e.currentTarget.style.borderBottomColor = 'transparent'; e.currentTarget.style.color = running || submitting ? C.outline : C.onBg; }}
                            >
                                <span className="material-symbols-outlined" style={{ fontSize: '16px', fontVariationSettings: "'FILL' 1" }}>play_arrow</span>
                                {running ? 'Running...' : 'Run Code'}
                            </button>

                            <button
                                onClick={handleSubmit}
                                disabled={submitting || running}
                                style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 24px', border: `1px solid ${C.secondary}`, color: submitting || running ? C.outline : C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: submitting || running ? 'not-allowed' : 'pointer', opacity: submitting || running ? 0.5 : 1, transition: 'all 0.2s' }}
                                onMouseEnter={e => { if (!submitting && !running) { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; } }}
                                onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = submitting || running ? C.outline : C.secondary; }}
                            >
                                <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>upload</span>
                                {submitting ? 'Submitting...' : 'Submit'}
                            </button>
                        </div>
                    </div>
                </section>
            </main>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
            `}</style>
        </div>
    );
};

export default ProblemSolveEmbed;
