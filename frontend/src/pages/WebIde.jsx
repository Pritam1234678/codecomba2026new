import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';

// ── Design tokens ─────────────────────────────────────────────────────────────
const C = {
    bg: '#121212', surface: '#1e1e1e', surfaceAlt: '#252525',
    onBg: '#e0e0e0', primary: '#bb86fc', secondary: '#03dac6',
    error: '#cf6679', success: '#4caf50', warning: '#fb8c00',
    outline: '#444', muted: '#888'
};

// ── Spinner ───────────────────────────────────────────────────────────────────
const Spinner = () => (
    <div style={{
        width: 32, height: 32, border: `3px solid ${C.outline}`,
        borderTop: `3px solid ${C.primary}`, borderRadius: '50%',
        animation: 'spin 0.8s linear infinite'
    }} />
);

// ── Main Component ────────────────────────────────────────────────────────────
const WebIde = () => {
    const { problemId } = useParams();
    const navigate = useNavigate();

    const [sessionId, setSessionId] = useState(null);
    const [iframeUrl, setIframeUrl] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [title, setTitle] = useState('');

    // Test state
    const [testResults, setTestResults] = useState(null);
    const [testing, setTesting] = useState(false);
    const [runCount, setRunCount] = useState(0);
    const [submitCount, setSubmitCount] = useState(0);

    const heartbeatRef = useRef(null);
    const sessionIdRef = useRef(null);
    const language = 'JAVA';

    // Keep ref in sync for heartbeat closure
    useEffect(() => { sessionIdRef.current = sessionId; }, [sessionId]);

    // ── Start session on mount ────────────────────────────────────────────────
    useEffect(() => {
        let cancelled = false;

        const startSession = async () => {
            try {
                const res = await api.post('/web-ide/session/start', { problemId, language });
                if (cancelled) return;
                setSessionId(res.data.sessionId);
                setIframeUrl(res.data.url);
                setTitle(res.data.title || '');
                setLoading(false);
            } catch (err) {
                if (cancelled) return;
                if (err.response?.status === 503) {
                    setError('All execution servers are busy. Try again in a few minutes.');
                } else {
                    setError(err.response?.data?.message || 'Failed to start IDE session.');
                }
                setLoading(false);
            }
        };

        startSession();
        return () => { cancelled = true; };
    }, [problemId]);

    // ── Heartbeat every 60s ───────────────────────────────────────────────────
    useEffect(() => {
        if (!sessionId) return;

        heartbeatRef.current = setInterval(async () => {
            try {
                await api.post('/web-ide/session/heartbeat', {
                    sessionId: sessionIdRef.current, language
                });
            } catch { /* idle reaper will handle it */ }
        }, 60_000);

        return () => {
            if (heartbeatRef.current) clearInterval(heartbeatRef.current);
        };
    }, [sessionId]);

    // ── Run tests ─────────────────────────────────────────────────────────────
    const handleRun = useCallback(async () => {
        if (testing) return;
        setTesting(true);
        setTestResults(null);
        try {
            const res = await api.post('/web-ide/session/test', {
                sessionId, language, submit: false
            });
            setTestResults(res.data);
            setRunCount(c => c + 1);
        } catch (err) {
            setTestResults({
                status: 'ERROR',
                message: err.response?.data?.message || 'Test execution failed.'
            });
        } finally {
            setTesting(false);
        }
    }, [sessionId, testing]);

    // ── Submit ────────────────────────────────────────────────────────────────
    const handleSubmit = useCallback(async () => {
        if (testing) return;
        setTesting(true);
        setTestResults(null);
        try {
            const res = await api.post('/web-ide/session/test', {
                sessionId, language, submit: true
            });
            setTestResults(res.data);
            setSubmitCount(c => c + 1);
        } catch (err) {
            setTestResults({
                status: 'ERROR',
                message: err.response?.data?.message || 'Submission failed.'
            });
        } finally {
            setTesting(false);
        }
    }, [sessionId, testing]);

    // ── Exit session ──────────────────────────────────────────────────────────
    const handleExit = useCallback(async () => {
        if (!window.confirm('Are you sure? This will delete your workspace.')) return;
        try {
            await api.post('/web-ide/session/stop', { sessionId, language });
        } catch { /* navigate regardless */ }
        navigate('/web-contest');
    }, [sessionId, navigate]);

    // ── Render: Loading state ─────────────────────────────────────────────────
    if (loading) {
        return (
            <div style={styles.center}>
                <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
                <Spinner />
                <p style={{ color: C.onBg, marginTop: 16 }}>Starting your IDE...</p>
            </div>
        );
    }

    // ── Render: Error state ───────────────────────────────────────────────────
    if (error) {
        return (
            <div style={styles.center}>
                <p style={{ color: C.error, fontSize: 18 }}>{error}</p>
                <button onClick={() => navigate('/web-contest')} style={styles.btnSecondary}>
                    ← Back to Challenges
                </button>
            </div>
        );
    }

    // ── Render: Results panel ─────────────────────────────────────────────────
    const renderResults = () => {
        if (!testResults) return null;

        // Compilation error
        if (testResults.status === 'CE') {
            return (
                <div style={{ padding: '8px 16px' }}>
                    <span style={{ color: C.error, fontWeight: 600 }}>Compilation Error</span>
                    <pre style={styles.preBlock}>{testResults.message || testResults.error}</pre>
                </div>
            );
        }

        // Generic error
        if (testResults.status === 'ERROR') {
            return (
                <div style={{ padding: '8px 16px' }}>
                    <span style={{ color: C.error }}>{testResults.message}</span>
                </div>
            );
        }

        // Normal results with test cases
        const cases = testResults.testCases || [];
        const passed = cases.filter(tc => tc.passed).length;
        const total = cases.length;

        return (
            <div style={{ padding: '8px 16px', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12 }}>
                <span style={{ color: C.muted, fontWeight: 600, marginRight: 4 }}>RESULTS:</span>
                {cases.map((tc, i) => (
                    <span key={i} style={{ color: tc.passed ? C.success : C.error }}>
                        {tc.passed ? '✅' : '❌'} TC{i + 1}
                    </span>
                ))}
                {total > 0 && (
                    <span style={{ color: C.muted, marginLeft: 8 }}>
                        {passed}/{total} passed
                    </span>
                )}
                {testResults.score != null && (
                    <span style={{ color: C.secondary, fontWeight: 600, marginLeft: 8 }}>
                        Score: {testResults.score}
                    </span>
                )}
            </div>
        );
    };

    // ── Render: Main layout ───────────────────────────────────────────────────
    return (
        <div style={styles.page}>
            <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>

            {/* Header bar */}
            <div style={styles.header}>
                <button onClick={() => navigate('/web-contest')} style={styles.backBtn}>← Back</button>
                <span style={styles.title}>Into the Web: {title || problemId}</span>
                <button onClick={handleExit} style={styles.exitBtn}>Exit Session</button>
            </div>

            {/* VS Code iframe */}
            <iframe
                src={iframeUrl}
                title="VS Code IDE"
                style={{ width: '100%', height: 'calc(100vh - 180px)', border: 'none' }}
                allow="clipboard-read; clipboard-write"
            />

            {/* Bottom panel: results + buttons */}
            <div style={styles.bottomPanel}>
                {renderResults()}
                <div style={styles.btnRow}>
                    <button
                        onClick={handleRun}
                        disabled={testing}
                        style={{ ...styles.btnPrimary, opacity: testing ? 0.5 : 1 }}
                    >
                        {testing ? 'Running...' : `Run Tests (${runCount}/10)`}
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={testing}
                        style={{ ...styles.btnSubmit, opacity: testing ? 0.5 : 1 }}
                    >
                        {testing ? 'Submitting...' : `Submit (${submitCount}/5)`}
                    </button>
                </div>
            </div>
        </div>
    );
};

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = {
    page: {
        display: 'flex', flexDirection: 'column', height: '100vh',
        background: C.bg, color: C.onBg, overflow: 'hidden'
    },
    center: {
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        justifyContent: 'center', height: '100vh', background: C.bg, gap: 12
    },
    header: {
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '10px 16px', background: C.surface, borderBottom: `1px solid ${C.outline}`
    },
    title: {
        color: C.onBg, fontWeight: 600, fontSize: 16
    },
    backBtn: {
        background: 'none', border: 'none', color: C.muted,
        cursor: 'pointer', fontSize: 14, padding: '4px 8px'
    },
    exitBtn: {
        background: C.error, color: '#fff', border: 'none',
        borderRadius: 6, padding: '6px 14px', cursor: 'pointer',
        fontWeight: 500, fontSize: 13
    },
    bottomPanel: {
        background: C.surfaceAlt, borderTop: `1px solid ${C.outline}`,
        padding: '6px 0', minHeight: 60
    },
    btnRow: {
        display: 'flex', gap: 12, padding: '6px 16px', alignItems: 'center'
    },
    btnPrimary: {
        background: C.primary, color: '#000', border: 'none',
        borderRadius: 6, padding: '8px 18px', cursor: 'pointer',
        fontWeight: 600, fontSize: 14
    },
    btnSubmit: {
        background: C.secondary, color: '#000', border: 'none',
        borderRadius: 6, padding: '8px 18px', cursor: 'pointer',
        fontWeight: 600, fontSize: 14
    },
    btnSecondary: {
        background: 'none', border: `1px solid ${C.outline}`, color: C.onBg,
        borderRadius: 6, padding: '8px 16px', cursor: 'pointer', marginTop: 12
    },
    preBlock: {
        background: C.surface, color: C.error, padding: 12, borderRadius: 6,
        marginTop: 8, fontSize: 12, overflow: 'auto', maxHeight: 120,
        whiteSpace: 'pre-wrap', border: `1px solid ${C.outline}`
    }
};

export default WebIde;
