import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import api from '../services/api';
import ProblemService from '../services/problem.service';
import SubmissionService from '../services/submission.service';
import AuthService from '../services/auth.service';

// ── Design tokens ─────────────────────────────────────────────────────────────
const C = {
    bg: '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceHi: '#2a2a2a',
    surfaceMin: '#0e0e0e',
    border: '#50453b',
    primary: '#f1bc8b',
    secondary: '#e9c176',
    muted: '#d4c4b7',
    outline: '#9d8e83',
    onBg: '#e5e2e1',
    error: '#ffb4ab',
    success: '#4ade80',
};

const LANG_MAP = {
    JAVA: { label: 'Java 21', monaco: 'java' },
    CPP: { label: 'C++ 20', monaco: 'cpp' },
    C: { label: 'C', monaco: 'c' },
    PYTHON: { label: 'Python 3.11', monaco: 'python' },
    JAVASCRIPT: { label: 'JavaScript', monaco: 'javascript' },
};

const DIFF_CFG = {
    EASY: { color: C.success, label: 'Easy' },
    MEDIUM: { color: C.secondary, label: 'Medium' },
    HARD: { color: C.error, label: 'Hard' },
};

// ── Verdict renderer (pure function → returns JSX) ────────────────────────────
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
    const visibleTCs = testCaseDetails.filter(tc => !tc.hidden);
    const hiddenTCs = testCaseDetails.filter(tc => tc.hidden);
    const hiddenPassed = hiddenTCs.filter(tc => tc.status === 'PASS').length;
    const total = sub.totalTestCases || 0;
    const passed = sub.testCasesPassed || 0;
    const isAC = sub.status === 'AC';
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
                {/* Test cases passed count instead of score */}
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', fontWeight: 600, color: statusColor }}>
                    {passed}/{total} passed
                </span>
            </div>

            {/* Progress bar based on test cases */}
            <div style={{ height: '3px', backgroundColor: C.surfaceHi, borderRadius: '2px' }}>
                <div style={{ height: '100%', width: `${total > 0 ? (passed / total) * 100 : 0}%`, backgroundColor: statusColor, borderRadius: '2px', transition: 'width 0.8s ease' }} />
            </div>

            {/* Execution time */}
            {(sub.timeConsumedMs > 0 || sub.timeConsumed > 0) && (
                <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                    ⏱ Execution time: {sub.timeConsumedMs || sub.timeConsumed}ms
                </div>
            )}

            {/* Visible test cases — show each with status */}
            {visibleTCs.length > 0 && (
                <div style={{ borderTop: `1px solid ${C.border}`, paddingTop: '10px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.1em', textTransform: 'uppercase', marginBottom: '4px' }}>
                        Test Cases
                    </span>
                    {visibleTCs.map((tc, idx) => (
                        <div key={idx} style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', padding: '6px 10px', backgroundColor: tc.status === 'PASS' ? 'rgba(74,222,128,0.05)' : 'rgba(255,180,171,0.05)', border: `1px solid ${tc.status === 'PASS' ? 'rgba(74,222,128,0.15)' : 'rgba(255,180,171,0.15)'}` }}>
                            {/* TC header row */}
                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <span style={{ color: tc.status === 'PASS' ? C.success : C.error, fontWeight: 600, minWidth: '16px' }}>
                                    {tc.status === 'PASS' ? '✓' : '✗'}
                                </span>
                                <span style={{ color: C.muted }}>Test Case {tc.testCase}</span>
                                <span style={{ marginLeft: 'auto', color: tc.status === 'PASS' ? C.success : C.error, fontSize: '11px' }}>
                                    {tc.status === 'PASS' ? 'PASS' : 'FAIL'}
                                </span>
                            </div>
                            {/* Show input/expected/got for failed visible test cases */}
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

            {/* Hidden test cases */}
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

// ── Main Component ────────────────────────────────────────────────────────────
const ProblemSolve = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    // ── Core state ────────────────────────────────────────────────────────────
    const [problem, setProblem] = useState(null);
    const [language, setLanguage] = useState(() => {
        try { return localStorage.getItem(`lang_problem_${id}`) || 'JAVA'; } catch { return 'JAVA'; }
    });
    const [code, setCode] = useState(() => {
        const lang = (() => { try { return localStorage.getItem(`lang_problem_${id}`) || 'JAVA'; } catch { return 'JAVA'; } })();
        try { return localStorage.getItem(`code_problem_${id}_${lang}`) || '// Write your code here\n'; } catch { return '// Write your code here\n'; }
    });
    const [output, setOutput] = useState(null);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [running, setRunning] = useState(false);
    const [hasExistingSubmission, setHasExistingSubmission] = useState(false);
    const [snippets, setSnippets] = useState({});

    // ── Navigation state ──────────────────────────────────────────────────────
    const [allProblems, setAllProblems] = useState([]);
    const [currentIndex, setCurrentIndex] = useState(-1);

    // ── Contest status ────────────────────────────────────────────────────────
    const [contestStatus, setContestStatus] = useState({ active: true, exists: true, contestName: '', endTime: null });
    const [showStatusBanner, setShowStatusBanner] = useState(false);
    const [timeRemaining, setTimeRemaining] = useState('');

    // ── Contest stats (run count + submit count + execution time) ────────────
    const [runCount, setRunCount] = useState(0);
    const [submitCount, setSubmitCount] = useState(0);
    const [lastExecMs, setLastExecMs] = useState(null);

    // ── UI state ──────────────────────────────────────────────────────────────
    const [consoleTab, setConsoleTab] = useState('output'); // 'output'
    const [leftWidth, setLeftWidth] = useState(42);       // % of total width
    const [isDragging, setIsDragging] = useState(false);
    const [consoleHeight, setConsoleHeight] = useState(220);      // px
    const [isDraggingH, setIsDraggingH] = useState(false);

    // ── Persist code + language to localStorage (debounced) ─────────────────
    const saveTimer = useRef(null);
    useEffect(() => {
        try { localStorage.setItem(`lang_problem_${id}`, language); } catch { }
        clearTimeout(saveTimer.current);
        saveTimer.current = setTimeout(() => {
            try { localStorage.setItem(`code_problem_${id}_${language}`, code); } catch { }
        }, 500);
        return () => clearTimeout(saveTimer.current);
    }, [code, language, id]);

    const sseRef = useRef(null);
    // The submission the user is actively waiting on. The SSE stream delivers
    // EVERY verdict for this user (including stale/previous ones), so we only
    // render a verdict that matches the in-flight submission — otherwise a Run
    // could show an old Submit's "Submission saved" footer, and vice-versa.
    const activeSubRef = useRef(null);
    const pollCleanupRef = useRef(null); // stop polling when SSE verdict arrives
    const dragStartX = useRef(0);
    const dragStartW = useRef(0);
    const dragStartY = useRef(0);
    const dragStartH = useRef(0);
    const runningRef = useRef(false);
    const submittingRef = useRef(false);

    // Keep refs in sync with state (SSE verdict may call setRunning/setSubmitting)
    useEffect(() => { runningRef.current = running; }, [running]);
    useEffect(() => { submittingRef.current = submitting; }, [submitting]);

    // ── SSE connection — auto-reconnects on error ──────────────────────────────
    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (!user?.token) return;

        const API_BASE = import.meta.env.VITE_API_URL
            ?? (window.location.hostname === 'localhost' ? 'http://localhost:8080/api' : '/api');

        let cancelled = false;
        let es = null;
        let reconnectTimer = null;

        const connect = () => {
            if (cancelled) return;
            api.post('/submissions/sse-ticket')
                .then(res => {
                    if (cancelled) return;
                    const ticket = res.data?.ticket;
                    if (!ticket) return;

                    const url = `${API_BASE}/submissions/stream?ticket=${encodeURIComponent(ticket)}`;
                    if (es) { try { es.close(); } catch {} }
                    es = new EventSource(url);
                    sseRef.current = es;

                    es.addEventListener('verdict', (e) => {
                        try {
                            const verdict = JSON.parse(e.data);
                            if (activeSubRef.current != null &&
                                verdict.submissionId !== activeSubRef.current) return;
                            activeSubRef.current = null;
                            if (pollCleanupRef.current) { pollCleanupRef.current(); pollCleanupRef.current = null; }
                            setSubmitting(false);
                            setRunning(false);
                            const execMs = verdict.timeConsumedMs || verdict.timeConsumed || null;
                            if (execMs > 0) setLastExecMs(execMs);
                            setOutput(buildVerdictUI(verdict, verdict.testRun === true));
                        } catch (err) {
                            console.error('SSE parse error:', err);
                        }
                    });

                    es.addEventListener('connected', () => {
                        console.log('SSE stream connected for user');
                    });

                    es.onerror = () => {
                        if (es) { try { es.close(); } catch {} }
                        sseRef.current = null;
                        es = null;
                        if (!cancelled) {
                            console.warn('SSE dropped — reconnecting in 3s');
                            reconnectTimer = setTimeout(connect, 3000);
                        }
                    };
                })
                .catch(err => {
                    if (!cancelled) {
                        console.warn('SSE ticket failed — retrying in 5s', err);
                        reconnectTimer = setTimeout(connect, 5000);
                    }
                });
        };

        connect();

        return () => {
            cancelled = true;
            if (reconnectTimer) clearTimeout(reconnectTimer);
            if (es) { try { es.close(); } catch {} }
            sseRef.current = null;
        };
    }, []);

    // ── Load problem + snippets + existing submission ─────────────────────────
    useEffect(() => {
        setLoading(true);
        setHasExistingSubmission(false);
        setOutput(null);
        // Do NOT reset code/language here — lazy initializers already restored
        // them from localStorage on mount. Resetting here would wipe the user's
        // saved draft every time they revisit the same problem in the same session.
        setSnippets({});

        api.get(`/problems/${id}`)
            .then(res => {
                // timeLimit is canonically SECONDS. Legacy rows may hold ms (e.g. 5000);
                // normalize so the displayed "Ns" and the poll cap are sane.
                const p = res.data;
                if (p && typeof p.timeLimit === 'number') {
                    let tl = p.timeLimit > 100 ? p.timeLimit / 1000 : p.timeLimit;
                    p.timeLimit = Math.max(1, Math.min(15, Math.round(tl)));
                }
                setProblem(p);
                setLoading(false);
            })
            .catch(() => setLoading(false));

        ProblemService.getSnippets(id)
            .then(res => {
                const map = {};
                res.data.forEach(s => { map[s.language] = s.starterCode; });
                setSnippets(map);

                SubmissionService.getUserSubmission(id)
                    .then(subRes => {
                        if (subRes.data) {
                            setHasExistingSubmission(true);
                            // Existing submission: restore its code+language but only if the
                            // user hasn't already typed something new (localStorage draft wins).
                            const savedCode = (() => { try { return localStorage.getItem(`code_problem_${id}_${subRes.data.language}`); } catch { return null; } })();
                            const savedLang = (() => { try { return localStorage.getItem(`lang_problem_${id}`); } catch { return null; } })();
                            // If localStorage has a draft for this problem, prefer it; otherwise
                            // fall back to the last-submitted code so the user isn't left blank.
                            if (!savedCode) {
                                setCode(subRes.data.code);
                                setLanguage(subRes.data.language);
                            } else if (savedLang) {
                                setLanguage(savedLang);
                            }
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
                            // No existing submission. Use saved draft if present; else snippet.
                            const savedLang = (() => { try { return localStorage.getItem(`lang_problem_${id}`); } catch { return null; } })();
                            const activeLang = savedLang || 'JAVA';
                            const savedCode = (() => { try { return localStorage.getItem(`code_problem_${id}_${activeLang}`); } catch { return null; } })();
                            const isPlaceholder = (s) => !s || s.trim() === '' || s.trim() === '// Write your code here' || s.trim() === '// Write your code here\n';
                            if (isPlaceholder(savedCode)) {
                                // No draft yet — seed editor with the starter snippet.
                                if (map[activeLang]) { setCode(map[activeLang]); setLanguage(activeLang); }
                                else if (map['JAVA']) { setCode(map['JAVA']); setLanguage('JAVA'); }
                            }
                            // If savedCode is a real draft, lazy initializer already loaded it — do nothing.
                        }
                    })
                    .catch(err => {
                        if (err.response?.status !== 404) console.error(err);
                        // Same logic: prefer saved draft, fall back to snippet.
                        const savedLang = (() => { try { return localStorage.getItem(`lang_problem_${id}`); } catch { return null; } })();
                        const activeLang = savedLang || 'JAVA';
                        const savedCode = (() => { try { return localStorage.getItem(`code_problem_${id}_${activeLang}`); } catch { return null; } })();
                        const isPlaceholder = (s) => !s || s.trim() === '' || s.trim() === '// Write your code here' || s.trim() === '// Write your code here\n';
                        if (isPlaceholder(savedCode)) {
                            if (map[activeLang]) { setCode(map[activeLang]); setLanguage(activeLang); }
                            else if (map['JAVA']) { setCode(map['JAVA']); setLanguage('JAVA'); }
                        }
                    });
            })
            .catch(() => { });
    }, [id]);

    // ── Load problems for prev/next navigation ────────────────────────────────
    // Contest problem → fetch only that contest's problems (ordered by contest).
    // Standalone problem → fetch all problems.
    useEffect(() => {
        if (!problem) return; // wait until problem is loaded to know contestId
        const contestId = problem.contestId;
        const CACHE_KEY = contestId ? `problems_nav_contest_${contestId}` : 'problems_nav_cache';
        const CACHE_TTL = 60000;
        try {
            const cached = JSON.parse(sessionStorage.getItem(CACHE_KEY) || 'null');
            if (cached && Date.now() - cached.ts < CACHE_TTL) {
                setAllProblems(cached.data);
                setCurrentIndex(cached.data.findIndex(p => p.id === parseInt(id)));
                return;
            }
        } catch (ignored) { }

        const url = contestId ? `/problems/contest/${contestId}` : '/problems';
        api.get(url)
            .then(res => {
                setAllProblems(res.data);
                setCurrentIndex(res.data.findIndex(p => p.id === parseInt(id)));
                try {
                    sessionStorage.setItem(CACHE_KEY, JSON.stringify({ data: res.data, ts: Date.now() }));
                } catch (ignored) { }
            })
            .catch(() => { });
    }, [id, problem]);

    // ── Fetch run count (contest problems only) ───────────────────────────────
    useEffect(() => {
        if (!contestStatus.exists) return;
        api.get(`/submissions/run-count/${id}`)
            .then(res => setRunCount(res.data?.runCount ?? 0))
            .catch(() => { });
    }, [id, contestStatus.exists]);

    useEffect(() => {
        if (!contestStatus.exists) return;
        api.get(`/submissions/submit-count/${id}`)
            .then(res => setSubmitCount(res.data?.submitCount ?? 0))
            .catch(() => { });
    }, [id, contestStatus.exists]);

    // ── Contest status polling ────────────────────────────────────────────────
    useEffect(() => {
        const CACHE_KEY = `contest_status_${id}`;
        const CACHE_TTL = 10000; // 10s client cache for initial load

        const check = async (useCache = false) => {
            if (useCache) {
                try {
                    const cached = JSON.parse(sessionStorage.getItem(CACHE_KEY) || 'null');
                    if (cached && Date.now() - cached.ts < CACHE_TTL) {
                        setContestStatus({ ...cached.data, checked: true });
                        if (!cached.data.exists) { setProblem(null); return; }
                        if (!cached.data.active) setShowStatusBanner(true);
                        if (cached.data.problemActive === false) setShowStatusBanner(true);
                        return;
                    }
                } catch (ignored) { }
            }
            try {
                const res = await api.get(`/problems/${id}/contest-status`);
                setContestStatus({ ...res.data, checked: true });
                if (!res.data.exists) { setProblem(null); return; }
                if (!res.data.active) setShowStatusBanner(true);
                if (res.data.problemActive === false) setShowStatusBanner(true);
                try {
                    sessionStorage.setItem(CACHE_KEY, JSON.stringify({ data: res.data, ts: Date.now() }));
                } catch (ignored) { }
            } catch (err) {
                if (err.response?.status === 404) setProblem(null);
            }
        };
        check(true); // use cache on initial load
        const iv = setInterval(() => check(false), 5000); // always fresh on poll
        return () => clearInterval(iv);
    }, [id]);

    // ── Contest countdown ─────────────────────────────────────────────────────
    useEffect(() => {
        if (!contestStatus.endTime) { setTimeRemaining(''); return; }
        const tick = () => {
            const diff = new Date(contestStatus.endTime) - new Date();
            if (diff <= 0) { setTimeRemaining('Ended'); return; }
            const h = Math.floor(diff / 3600000);
            const m = Math.floor((diff % 3600000) / 60000);
            const s = Math.floor((diff % 60000) / 1000);
            setTimeRemaining(h > 0 ? `${h}h ${m}m` : m > 0 ? `${m}m ${s}s` : `${s}s`);
        };
        tick();
        const iv = setInterval(tick, 1000);
        return () => clearInterval(iv);
    }, [contestStatus.endTime]);

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
            const container = document.getElementById('ps-workspace');
            if (!container) return;
            const totalW = container.getBoundingClientRect().width;
            const delta = e.clientX - dragStartX.current;
            const newPct = Math.min(65, Math.max(25, dragStartW.current + (delta / totalW) * 100));
            setLeftWidth(newPct);
        };
        const onUp = () => setIsDragging(false);
        window.addEventListener('mousemove', onMove);
        window.addEventListener('mouseup', onUp);
        return () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp); };
    }, [isDragging]);

    // ── Drag: horizontal console divider ─────────────────────────────────────
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
        try {
            const saved = localStorage.getItem(`code_problem_${id}_${lang}`);
            if (saved) { setCode(saved); return; }
        } catch { }
        if (snippets[lang]) setCode(snippets[lang]);
        else setCode('');
    };

    const guardContest = () => {
        if (!contestStatus.active || !contestStatus.exists || contestStatus.problemActive === false) {
            setShowStatusBanner(true);
            setTimeout(() => navigate('/contests'), 2000);
            return false;
        }
        return true;
    };

    const handleRun = () => {
        if (!guardContest()) return;
        if (runningRef.current) return;
        runningRef.current = true;
        setRunning(true);
        setOutput(
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: '#facc15' }}>
                <span className="material-symbols-outlined" style={{ fontSize: '16px', animation: 'spin 1s linear infinite' }}>sync</span>
                Running tests...
            </div>
        );
        if (contestStatus.exists) setRunCount(c => c + 1);
        SubmissionService.testCode(id, code, language)
            .then(res => {
                const submissionId = res.data?.id;
                if (submissionId) {
                    activeSubRef.current = submissionId;
                    pollVerdict(submissionId, true);
                } else {
                    runningRef.current = false;
                    setRunning(false);
                    setOutput(<span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                        No submission ID returned from backend. Try again.
                    </span>);
                }
            })
            .catch(err => {
                runningRef.current = false;
                setRunning(false);
                const msg = err.response?.data?.message || err.message || 'Backend unreachable';
                setOutput(<span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                    Error: {msg}
                </span>);
            });
    };

    const handleSubmit = () => {
        if (!guardContest()) return;
        if (submittingRef.current) return;
        submittingRef.current = true;
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
                    activeSubRef.current = submissionId;
                    setSubmitCount(prev => prev + 1);
                    pollVerdict(submissionId, false);
                } else {
                    submittingRef.current = false;
                    setSubmitting(false);
                    setOutput(<span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                        No submission ID returned from backend. Try again.
                    </span>);
                }
            })
            .catch(err => {
                submittingRef.current = false;
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

    // ── Polling: walks /submissions/{id}/status until verdict, with proper
    //    spacing between requests so they don't overlap and pile up.
    //    Wall-clock cap scales with the problem's time limit so a 10s problem
    //    can still complete even if SSE drops, and slow links don't see false
    //    "Timeout" before the worker has had a fair chance.
    const pollVerdict = (submissionId, isTestRun) => {
        const baseTimeout = problem?.timeLimit ? Math.ceil(problem.timeLimit) : 5;
        // Worker queue + judge run + buffer. ~120s ceiling so a never-arrived
        // verdict eventually surfaces but isn't reported prematurely.
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
                    if (activeSubRef.current === submissionId) activeSubRef.current = null;
                    setSubmitting(false);
                    setRunning(false);
                    if (sub.timeConsumed > 0) setLastExecMs(sub.timeConsumed);
                    setOutput(buildVerdictUI({ ...sub, testRun: isTestRun }, isTestRun));
                    return;
                }
            } catch (err) {
                consecutiveErrors++;
                console.warn(`Poll attempt ${attempts} failed (${consecutiveErrors} consecutive):`, err.message);
                // 5 consecutive transport errors → backend is genuinely down,
                // surface a clear message instead of silent retries.
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

        // First attempt slightly delayed to give the worker a head start
        pollCleanupRef.current = () => { cancelled = true; };
        setTimeout(tick, intervalMs);

        return () => { cancelled = true; };
    };

    const handlePrev = () => {
        if (!guardContest()) return;
        if (currentIndex > 0) navigate(`/problems/${allProblems[currentIndex - 1].id}`);
    };

    const handleNext = () => {
        if (!guardContest()) return;
        if (currentIndex < allProblems.length - 1) navigate(`/problems/${allProblems[currentIndex + 1].id}`);
    };

    // ── Loading / Error states ────────────────────────────────────────────────
    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', backgroundColor: C.bg, color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
            Loading Arena...
        </div>
    );

    if (!problem) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', backgroundColor: C.bg }}>
            <div style={{ border: `1px solid ${C.border}`, padding: '3rem', maxWidth: '480px', width: '100%', textAlign: 'center', backgroundColor: C.surfaceLow }}>
                <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', color: C.error, marginBottom: '1rem' }}>Problem Not Found</h1>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.outline, marginBottom: '2rem' }}>
                    Problem ID: <span style={{ fontFamily: "'JetBrains Mono', monospace", color: C.error }}>{id}</span>
                </p>
                <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
                    <button onClick={() => navigate('/contests')} style={{ padding: '10px 24px', border: `1px solid ${C.secondary}`, color: C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer' }}>
                        Browse Contests
                    </button>
                    <button onClick={() => navigate('/dashboard')} style={{ padding: '10px 24px', border: `1px solid ${C.border}`, color: C.muted, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer' }}>
                        Dashboard
                    </button>
                </div>
            </div>
        </div>
    );

    const diff = DIFF_CFG[problem.level] || { color: C.outline, label: problem.level || '—' };
    const letter = currentIndex >= 0 ? String.fromCharCode(65 + currentIndex) : '';

    // ── Main render ───────────────────────────────────────────────────────────
    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden', fontFamily: "'Geist', sans-serif", userSelect: isDragging || isDraggingH ? 'none' : 'auto' }}>

            {/* ── Contest Deactivated Banner ── */}
            {showStatusBanner && (
                <div style={{ position: 'fixed', inset: 0, zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(8px)' }}>
                    <div style={{ border: `2px solid ${C.error}`, backgroundColor: C.surfaceLow, padding: '2.5rem', maxWidth: '520px', width: '100%', position: 'relative' }}>
                        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.error }} />
                        <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', color: C.error, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '28px', fontVariationSettings: "'FILL' 1" }}>warning</span>
                            {!contestStatus.exists ? 'Contest Deleted' : contestStatus.problemActive === false ? 'Problem Disabled' : 'Contest Deactivated'}
                        </h2>
                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.muted, lineHeight: 1.6, marginBottom: '2rem' }}>
                            {!contestStatus.exists
                                ? 'This contest has been removed. Submissions are no longer accepted.'
                                : contestStatus.problemActive === false
                                    ? 'This problem has been disabled by the administrator. You cannot submit solutions.'
                                    : `"${contestStatus.contestName}" has been deactivated. Submissions are no longer allowed.`}
                        </p>
                        <div style={{ display: 'flex', gap: '12px' }}>
                            <button onClick={() => navigate('/contests')} style={{ padding: '10px 24px', border: `1px solid ${C.secondary}`, color: C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer' }}>
                                Go to Contests →
                            </button>
                            <button onClick={() => setShowStatusBanner(false)} style={{ padding: '10px 24px', border: `1px solid ${C.border}`, color: C.outline, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer' }}>
                                Dismiss
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* ── Top Header ── */}
            <header style={{ height: '56px', flexShrink: 0, borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px', zIndex: 10 }}>
                {/* Left: back + problem title */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                    <button
                        onClick={() => navigate(problem?.contestId ? `/contests/${problem.contestId}` : '/problems')}
                        style={{ display: 'flex', alignItems: 'center', gap: '6px', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', transition: 'color 0.2s' }}
                        onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                        onMouseLeave={e => e.currentTarget.style.color = C.outline}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                        Exit Arena
                    </button>
                    <div style={{ width: '1px', height: '24px', backgroundColor: C.border }} />
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        {letter && (
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>{letter}.</span>
                        )}
                        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '18px', fontWeight: 600, color: C.primary, maxWidth: '400px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', margin: 0 }}>
                            {problem.title}
                        </h1>
                    </div>
                </div>

                {/* Right: limits + timer + nav */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                    {/* Time + Memory limits */}
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

                    {/* Contest countdown */}
                    {timeRemaining && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.secondary }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>schedule</span>
                            {timeRemaining}
                        </div>
                    )}

                    {/* Prev / Next */}
                    <div style={{ display: 'flex', gap: '4px' }}>
                        <button
                            onClick={handlePrev}
                            disabled={currentIndex <= 0 || allProblems.length === 0}
                            style={{ padding: '6px 8px', border: `1px solid transparent`, color: C.outline, background: 'none', cursor: currentIndex <= 0 ? 'not-allowed' : 'pointer', opacity: currentIndex <= 0 ? 0.3 : 1, transition: 'all 0.2s' }}
                            onMouseEnter={e => { if (currentIndex > 0) { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.secondary; } }}
                            onMouseLeave={e => { e.currentTarget.style.borderColor = 'transparent'; e.currentTarget.style.color = C.outline; }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>chevron_left</span>
                        </button>
                        <button
                            onClick={handleNext}
                            disabled={currentIndex >= allProblems.length - 1 || allProblems.length === 0}
                            style={{ padding: '6px 8px', border: `1px solid transparent`, color: C.outline, background: 'none', cursor: currentIndex >= allProblems.length - 1 ? 'not-allowed' : 'pointer', opacity: currentIndex >= allProblems.length - 1 ? 0.3 : 1, transition: 'all 0.2s' }}
                            onMouseEnter={e => { if (currentIndex < allProblems.length - 1) { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.secondary; } }}
                            onMouseLeave={e => { e.currentTarget.style.borderColor = 'transparent'; e.currentTarget.style.color = C.outline; }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>chevron_right</span>
                        </button>
                    </div>
                </div>
            </header>

            {/* ── Workspace ── */}
            <main id="ps-workspace" style={{ flex: 1, display: 'flex', overflow: 'hidden', cursor: isDragging ? 'col-resize' : 'auto' }}>

                {/* ── Left Pane: Problem Statement ── */}
                <section style={{ width: `${leftWidth}%`, flexShrink: 0, backgroundColor: C.surfaceLow, borderRight: `1px solid ${C.border}`, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
                    <div style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>

                        {/* Difficulty + tags */}
                        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                            <span style={{ padding: '3px 10px', border: `1px solid ${diff.color}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: diff.color, textTransform: 'uppercase' }}>
                                {diff.label}
                            </span>
                            {hasExistingSubmission && (
                                <span style={{ padding: '3px 10px', border: `1px solid ${C.success}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.success, textTransform: 'uppercase' }}>
                                    ✓ Submitted
                                </span>
                            )}
                        </div>

                        {/* Description */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '16px', lineHeight: 1.7, color: C.onBg, margin: 0 }}>
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
                        {/* Language selector */}
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

                        {/* Toolbar actions */}
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
                            <button
                                onClick={() => setConsoleTab('output')}
                                style={{ height: '100%', display: 'flex', alignItems: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', color: consoleTab === 'output' ? C.secondary : C.outline, background: 'none', border: 'none', borderBottom: consoleTab === 'output' ? `2px solid ${C.secondary}` : '2px solid transparent', cursor: 'pointer', paddingTop: '2px' }}
                            >
                                Output Console
                            </button>
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
                        {/* Left: contest stats (run count + last exec time) */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>terminal</span>
                            Console
                            {contestStatus.exists && (
                                <>
                                    <span style={{ color: C.border }}>|</span>
                                    <span title="Times you pressed Run">▶ {runCount}×</span>
                                    {lastExecMs != null && (
                                        <span title="Last execution time">⏱ {lastExecMs}ms</span>
                                    )}
                                </>
                            )}
                        </div>

                        {/* Right: Run + Submit */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <button
                                onClick={handleRun}
                                disabled={running || submitting || (contestStatus.exists && runCount >= 10)}
                                title={contestStatus.exists && runCount >= 10 ? 'Run limit reached (10/10)' : ''}
                                style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 20px', border: 'none', borderBottom: `1px solid transparent`, color: (running || submitting || (contestStatus.exists && runCount >= 10)) ? C.outline : C.onBg, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: (running || submitting || (contestStatus.exists && runCount >= 10)) ? 'not-allowed' : 'pointer', opacity: (running || submitting || (contestStatus.exists && runCount >= 10)) ? 0.4 : 1, transition: 'all 0.2s' }}
                                onMouseEnter={e => { if (!running && !submitting && !(contestStatus.exists && runCount >= 10)) { e.currentTarget.style.borderBottomColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                                onMouseLeave={e => { e.currentTarget.style.borderBottomColor = 'transparent'; e.currentTarget.style.color = (running || submitting || (contestStatus.exists && runCount >= 10)) ? C.outline : C.onBg; }}
                            >
                                <span className="material-symbols-outlined" style={{ fontSize: '16px', fontVariationSettings: "'FILL' 1" }}>play_arrow</span>
                                {running ? 'Running...' : contestStatus.exists ? `Run (${runCount}/10)` : 'Run Code'}
                            </button>

                            <button
                                onClick={handleSubmit}
                                disabled={submitting || running || (contestStatus.exists && submitCount >= 5)}
                                title={contestStatus.exists && submitCount >= 5 ? 'Submit limit reached (5/5)' : ''}
                                style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 24px', border: `1px solid ${C.secondary}`, color: (submitting || running || (contestStatus.exists && submitCount >= 5)) ? C.outline : C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: (submitting || running || (contestStatus.exists && submitCount >= 5)) ? 'not-allowed' : 'pointer', opacity: (submitting || running || (contestStatus.exists && submitCount >= 5)) ? 0.5 : 1, transition: 'all 0.2s' }}
                                onMouseEnter={e => { if (!submitting && !running && !(contestStatus.exists && submitCount >= 5)) { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; } }}
                                onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = (submitting || running || (contestStatus.exists && submitCount >= 5)) ? C.outline : C.secondary; }}
                            >
                                <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>upload</span>
                                {submitting ? 'Submitting...' : contestStatus.exists ? `Submit (${submitCount}/5)` : 'Submit'}
                            </button>
                        </div>
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

export default ProblemSolve;
