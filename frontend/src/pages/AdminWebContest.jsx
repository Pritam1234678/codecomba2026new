import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

// ── Design tokens (VS Code dark) ─────────────────────────────────────────────
const C = {
    bg: '#1e1e1e', surface: '#252526', surfaceAlt: '#2d2d2d',
    onBg: '#cccccc', primary: '#007acc', secondary: '#4ec9b0',
    error: '#f48771', success: '#4ec9b0', warning: '#ce9178',
    outline: '#444', muted: '#888',
    accent: '#0e639c',
};

const Spinner = () => (
    <div style={{
        width: 28, height: 28, border: `3px solid ${C.outline}`,
        borderTop: `3px solid ${C.primary}`, borderRadius: '50%',
        animation: 'spin 0.8s linear infinite',
    }} />
);

// ── Main Component ────────────────────────────────────────────────────────────
const AdminWebContest = () => {
    const navigate = useNavigate();

    // Challenge list
    const [challenges, setChallenges] = useState([]);
    const [loadingList, setLoadingList] = useState(true);

    // Active editor session
    const [sessionId, setSessionId] = useState(null);
    const [iframeUrl, setIframeUrl] = useState(null);
    const [activeChallenge, setActiveChallenge] = useState(null); // { templateId, problemId, title, language }
    const [sessionLoading, setSessionLoading] = useState(false);
    const [sessionError, setSessionError] = useState(null);

    // Test results
    const [testing, setTesting] = useState(false);
    const [testResults, setTestResults] = useState(null);

    // New challenge modal
    const [showNewModal, setShowNewModal] = useState(false);
    const [newForm, setNewForm] = useState({ title: '', description: '', level: 'EASY', language: 'JAVA', templatePath: '' });
    const [creating, setCreating] = useState(false);

    // Status bar message
    const [status, setStatus] = useState('Select a challenge to open its IDE, or create a new one.');

    const heartbeatRef = useRef(null);
    const sessionIdRef = useRef(null);
    useEffect(() => { sessionIdRef.current = sessionId; }, [sessionId]);

    // ── Load challenge list ───────────────────────────────────────────────────
    const loadChallenges = useCallback(async () => {
        try {
            const res = await api.get('/web-contest/list');
            setChallenges(res.data);
        } catch {
            setStatus('Failed to load challenges.');
        } finally {
            setLoadingList(false);
        }
    }, []);

    useEffect(() => { loadChallenges(); }, [loadChallenges]);

    // ── Heartbeat ─────────────────────────────────────────────────────────────
    useEffect(() => {
        if (!sessionId || !activeChallenge) return;
        heartbeatRef.current = setInterval(async () => {
            try {
                await api.post('/web-ide/session/heartbeat', {
                    sessionId: sessionIdRef.current,
                    language: activeChallenge.language,
                });
            } catch { /* reaper handles it */ }
        }, 60_000);
        return () => clearInterval(heartbeatRef.current);
    }, [sessionId, activeChallenge]);

    // ── Open challenge in IDE ─────────────────────────────────────────────────
    const openChallenge = useCallback(async (challenge) => {
        // Stop previous session first
        if (sessionId && activeChallenge) {
            try {
                await api.post('/web-ide/session/stop', {
                    sessionId,
                    language: activeChallenge.language,
                });
            } catch { /* ignore */ }
        }
        setIframeUrl(null);
        setSessionId(null);
        setTestResults(null);
        setSessionError(null);
        setActiveChallenge(challenge);
        setSessionLoading(true);
        setStatus(`Starting VS Code for "${challenge.title}"...`);

        try {
            const res = await api.post('/web-ide/session/start', {
                problemId: challenge.problemId,
                language: challenge.language,
            });
            setSessionId(res.data.sessionId);
            setIframeUrl(res.data.url);
            setStatus(`VS Code ready — ${challenge.title} (${challenge.language})`);
        } catch (err) {
            const msg = err.response?.status === 503
                ? 'All execution VMs are busy. Try again in a minute.'
                : err.response?.data?.message || 'Failed to start IDE session.';
            setSessionError(msg);
            setStatus('Error: ' + msg);
        } finally {
            setSessionLoading(false);
        }
    }, [sessionId, activeChallenge]);

    // ── Close/exit current session ────────────────────────────────────────────
    const closeSession = useCallback(async () => {
        if (!sessionId || !activeChallenge) return;
        if (!window.confirm('Close this IDE session? The workspace will be deleted.')) return;
        try {
            await api.post('/web-ide/session/stop', {
                sessionId,
                language: activeChallenge.language,
            });
        } catch { /* ignore */ }
        setIframeUrl(null);
        setSessionId(null);
        setActiveChallenge(null);
        setTestResults(null);
        setStatus('Session closed.');
    }, [sessionId, activeChallenge]);

    // ── Run JUnit tests ───────────────────────────────────────────────────────
    const runTests = useCallback(async () => {
        if (!sessionId || testing) return;
        setTesting(true);
        setTestResults(null);
        setStatus('Running tests...');
        try {
            const res = await api.post('/web-ide/session/test', {
                sessionId,
                language: activeChallenge.language,
                submit: false,
            });
            setTestResults(res.data);
            setStatus(`Tests done — ${res.data.passed ?? 0}/${res.data.total ?? 0} passed`);
        } catch (err) {
            setTestResults({ status: 'ERROR', message: err.response?.data?.message || 'Test failed' });
            setStatus('Test execution error.');
        } finally {
            setTesting(false);
        }
    }, [sessionId, testing, activeChallenge]);

    // ── Create new challenge ──────────────────────────────────────────────────
    const createChallenge = useCallback(async () => {
        if (!newForm.title || !newForm.templatePath) {
            alert('Title and template path are required.');
            return;
        }
        setCreating(true);
        try {
            // 1. Create problem in DB
            const probRes = await api.post('/admin/problems', {
                title: newForm.title,
                description: newForm.description || 'Web coding challenge.',
                level: newForm.level,
                active: true,
                timeLimit: 60.0,
                memoryLimit: 512,
            });
            const problemId = probRes.data.id;

            // 2. Create template entry
            await api.post('/web-contest/admin/templates', {
                problemId,
                language: newForm.language,
                templatePath: newForm.templatePath,
            });

            setShowNewModal(false);
            setNewForm({ title: '', description: '', level: 'EASY', language: 'JAVA', templatePath: '' });
            setStatus(`Challenge "${newForm.title}" created! Click it to open IDE.`);
            await loadChallenges();
        } catch (err) {
            alert('Failed to create challenge: ' + (err.response?.data?.message || err.message));
        } finally {
            setCreating(false);
        }
    }, [newForm, loadChallenges]);

    // ── Render results ────────────────────────────────────────────────────────
    const renderResults = () => {
        if (!testResults) return null;
        if (testResults.status === 'CE') return (
            <div style={{ padding: '4px 16px' }}>
                <span style={{ color: C.error, fontWeight: 600 }}>Compilation Error</span>
                <pre style={styles.preBlock}>{testResults.message || testResults.rawOutput}</pre>
            </div>
        );
        if (testResults.status === 'ERROR') return (
            <div style={{ padding: '4px 16px', color: C.error }}>{testResults.message}</div>
        );
        const cases = testResults.details || [];
        const p = testResults.passed ?? 0;
        const t = testResults.total ?? 0;
        return (
            <div style={{ padding: '4px 16px', display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
                <span style={{ color: C.muted }}>Tests:</span>
                {cases.map((tc, i) => (
                    <span key={i} style={{ color: tc.status === 'PASS' ? C.success : C.error, fontSize: 13 }}>
                        {tc.status === 'PASS' ? '✅' : '❌'} TC{i + 1}
                    </span>
                ))}
                <span style={{ color: C.muted, marginLeft: 8 }}>{p}/{t} passed</span>
                {testResults.score != null && (
                    <span style={{ color: C.secondary, fontWeight: 600 }}>Score: {testResults.score}</span>
                )}
            </div>
        );
    };

    return (
        <div style={styles.page}>
            <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>

            {/* ── Top bar ── */}
            <div style={styles.topBar}>
                <span style={{ color: C.primary, fontWeight: 700, fontSize: 14, marginRight: 12 }}>⚡ Web IDE Admin</span>
                <select
                    value={activeChallenge?.templateId ?? ''}
                    onChange={e => {
                        const c = challenges.find(x => String(x.templateId) === e.target.value);
                        if (c) openChallenge(c);
                    }}
                    style={styles.select}
                >
                    <option value="">— Select Challenge to Edit —</option>
                    {challenges.map(c => (
                        <option key={c.templateId} value={c.templateId}>
                            {c.title} — {c.language} ({c.difficulty})
                        </option>
                    ))}
                </select>
                <button onClick={() => setShowNewModal(true)} style={styles.btnOutline}>+ New Challenge</button>
                <div style={{ flex: 1 }} />
                {sessionId && activeChallenge && (
                    <>
                        <button
                            onClick={runTests}
                            disabled={testing}
                            style={{ ...styles.btnBlue, opacity: testing ? 0.5 : 1 }}
                        >
                            {testing ? 'Running...' : '▶ Run Tests (JUnit)'}
                        </button>
                        <button onClick={closeSession} style={styles.btnRed}>✕ Close IDE</button>
                    </>
                )}
            </div>

            {/* ── Main area ── */}
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative' }}>
                {/* No session active */}
                {!sessionId && !sessionLoading && !sessionError && (
                    <div style={styles.emptyState}>
                        <span style={{ fontSize: 48 }}>🖥️</span>
                        <p style={{ color: C.muted, fontSize: 14 }}>
                            Select a challenge from the dropdown above, or create a new one.<br />
                            Full VS Code (code-server) will open here with Java extension pack.
                        </p>
                        <button onClick={() => setShowNewModal(true)} style={styles.btnBlue}>
                            + Create First Challenge
                        </button>
                    </div>
                )}

                {/* Loading */}
                {sessionLoading && (
                    <div style={styles.emptyState}>
                        <Spinner />
                        <p style={{ color: C.onBg, marginTop: 16 }}>Starting VS Code for "{activeChallenge?.title}"...</p>
                        <p style={{ color: C.muted, fontSize: 12 }}>Cloning template, launching code-server...</p>
                    </div>
                )}

                {/* Error */}
                {sessionError && (
                    <div style={styles.emptyState}>
                        <p style={{ color: C.error }}>{sessionError}</p>
                        <button onClick={() => setSessionError(null)} style={styles.btnOutline}>Retry</button>
                    </div>
                )}

                {/* VS Code iframe */}
                {iframeUrl && !sessionLoading && (
                    <iframe
                        src={iframeUrl}
                        title="Admin VS Code IDE"
                        style={{ flex: 1, width: '100%', border: 'none' }}
                        allow="clipboard-read; clipboard-write"
                    />
                )}
            </div>

            {/* ── Bottom: test results + status bar ── */}
            {testResults && (
                <div style={{ background: C.surface, borderTop: `1px solid ${C.outline}` }}>
                    {renderResults()}
                </div>
            )}
            <div style={styles.statusBar}>
                <span>{status}</span>
                {activeChallenge && (
                    <span style={{ marginLeft: 'auto', opacity: 0.7 }}>
                        {activeChallenge.language} · {activeChallenge.title}
                    </span>
                )}
            </div>

            {/* ── New challenge modal ── */}
            {showNewModal && (
                <div style={styles.overlay}>
                    <div style={styles.modal}>
                        <div style={styles.modalHeader}>
                            <span>New Web Challenge</span>
                            <button onClick={() => setShowNewModal(false)} style={styles.closeBtn}>✕</button>
                        </div>
                        <div style={styles.modalBody}>
                            <label style={styles.label}>Challenge Title *</label>
                            <input value={newForm.title} onChange={e => setNewForm(f => ({ ...f, title: e.target.value }))}
                                placeholder="e.g. Build a User CRUD API" style={styles.input} />

                            <label style={styles.label}>Description</label>
                            <textarea value={newForm.description} onChange={e => setNewForm(f => ({ ...f, description: e.target.value }))}
                                placeholder="What should users implement?" rows={3} style={{ ...styles.input, resize: 'vertical' }} />

                            <div style={{ display: 'flex', gap: 12 }}>
                                <div style={{ flex: 1 }}>
                                    <label style={styles.label}>Difficulty</label>
                                    <select value={newForm.level} onChange={e => setNewForm(f => ({ ...f, level: e.target.value }))} style={styles.input}>
                                        <option>EASY</option><option>MEDIUM</option><option>HARD</option>
                                    </select>
                                </div>
                                <div style={{ flex: 1 }}>
                                    <label style={styles.label}>Language</label>
                                    <select value={newForm.language} onChange={e => setNewForm(f => ({ ...f, language: e.target.value }))} style={styles.input}>
                                        <option>JAVA</option><option>PYTHON</option><option>NODEJS</option>
                                    </select>
                                </div>
                            </div>

                            <label style={styles.label}>Template Directory Path (on execution VM) *</label>
                            <input value={newForm.templatePath} onChange={e => setNewForm(f => ({ ...f, templatePath: e.target.value }))}
                                placeholder="/home/ubuntu/templates/my-spring-challenge" style={styles.input} />
                            <p style={{ color: C.muted, fontSize: 11, margin: '4px 0 0' }}>
                                Absolute path on VM3 (Java) or VM4 (Python) where the project files will live.
                                Leave empty for a blank project at the default location.
                            </p>

                            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
                                <button onClick={() => setShowNewModal(false)} style={styles.btnOutline} disabled={creating}>Cancel</button>
                                <button onClick={createChallenge} style={styles.btnBlue} disabled={creating}>
                                    {creating ? 'Creating...' : 'Create & Open IDE'}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = {
    page: {
        display: 'flex', flexDirection: 'column', height: '100vh',
        background: C.bg, color: C.onBg,
        fontFamily: "'JetBrains Mono', monospace", fontSize: 12, overflow: 'hidden',
    },
    topBar: {
        display: 'flex', alignItems: 'center', gap: 10,
        padding: '6px 12px', background: C.surfaceAlt,
        borderBottom: `1px solid ${C.outline}`, flexShrink: 0,
    },
    select: {
        background: '#3c3c3c', border: `1px solid ${C.outline}`, color: C.onBg,
        padding: '4px 10px', fontFamily: "'JetBrains Mono', monospace",
        fontSize: 12, outline: 'none', cursor: 'pointer', maxWidth: 340,
    },
    emptyState: {
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        justifyContent: 'center', flex: 1, gap: 16, color: C.muted,
        padding: 40, textAlign: 'center',
    },
    statusBar: {
        background: C.primary, color: '#fff',
        padding: '3px 12px', fontSize: 12, display: 'flex', flexShrink: 0,
    },
    btnBlue: {
        background: C.accent, color: '#fff', border: 'none',
        padding: '5px 14px', cursor: 'pointer', fontSize: 12,
        fontFamily: "'JetBrains Mono', monospace",
    },
    btnRed: {
        background: '#a1260d', color: '#fff', border: 'none',
        padding: '5px 14px', cursor: 'pointer', fontSize: 12,
        fontFamily: "'JetBrains Mono', monospace",
    },
    btnOutline: {
        background: 'transparent', color: C.onBg, border: `1px solid ${C.outline}`,
        padding: '4px 12px', cursor: 'pointer', fontSize: 12,
        fontFamily: "'JetBrains Mono', monospace",
    },
    overlay: {
        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
        zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center',
    },
    modal: {
        background: '#252526', border: `1px solid ${C.outline}`,
        minWidth: 460, maxWidth: 560, width: '100%',
    },
    modalHeader: {
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        padding: '12px 16px', borderBottom: `1px solid ${C.outline}`,
        color: C.onBg, fontWeight: 600, fontSize: 13,
    },
    closeBtn: {
        background: 'none', border: 'none', color: C.muted,
        cursor: 'pointer', fontSize: 18, lineHeight: 1,
    },
    modalBody: {
        padding: 20, display: 'flex', flexDirection: 'column', gap: 10,
    },
    label: { color: C.muted, fontSize: 11, marginBottom: 2 },
    input: {
        padding: '7px 10px', background: '#3c3c3c',
        border: `1px solid ${C.outline}`, color: C.onBg,
        fontFamily: "'JetBrains Mono', monospace", fontSize: 12,
        outline: 'none', width: '100%', boxSizing: 'border-box',
    },
    preBlock: {
        background: C.bg, color: C.error, padding: 10, margin: '4px 0',
        fontSize: 11, overflow: 'auto', maxHeight: 100, whiteSpace: 'pre-wrap',
        border: `1px solid ${C.outline}`,
    },
};

export default AdminWebContest;
