import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useParams, useLocation } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import { sqlJudgeApi } from '../services/api';

const C = {
  bg: '#0e0e0e',
  panel: '#161616',
  border: '#2a2a2a',
  primary: '#f1bc8b',
  secondary: '#e9c176',
  muted: '#9d8e83',
  onBg: '#e5e2e1',
  onMuted: '#b8aca0',
  accent: '#8ec07c',
  error: '#ffb4ab',
  runBtn: '#4a9eff',
  submitBtn: '#f1bc8b',
  surfaceLow: '#1c1b1b',
  surfaceHi: '#2a2a2a',
  outline: '#5c5753',
};

const SqlJudge = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams();
  
  // List view state
  const [problems, setProblems] = useState([]);
  const [loadingProblems, setLoadingProblems] = useState(true);
  
  // Detail/Solve view state
  const [problem, setProblem] = useState(null);
  const [loadingProblem, setLoadingProblem] = useState(false);
  const [sql, setSql] = useState('');
  const [mode, setMode] = useState('run'); // 'run' | 'submit'
  const [running, setRunning] = useState(false);
  const [status, setStatus] = useState(null); // 'queued' | 'running' | 'accepted' | 'wrong' | 'error' | 'timeout'
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [submissionId, setSubmissionId] = useState(null);
  const [polling, setPolling] = useState(false);
  const [sseConnected, setSseConnected] = useState(false);
  const eventSourceRef = useRef(null);
  const sseCancelledRef = useRef(false);
  const safetyRef = useRef(null);
  const verdictReceivedRef = useRef(false);
  const executingRef = useRef(false);
  
  // History
  const [history, setHistory] = useState([]);
  const [showHistory, setShowHistory] = useState(false);

  // Load problem list
  useEffect(() => {
    if (!id) {
      sqlJudgeApi.listProblems()
        .then(setProblems)
        .catch(() => setError('Failed to load SQL problems'))
        .finally(() => setLoadingProblems(false));
    }
  }, [id]);

  // Load single problem
  useEffect(() => {
    if (id) {
      setLoadingProblem(true);
      setProblem(null);
      sqlJudgeApi.getProblem(id)
        .then(p => {
          setProblem(p);
          setSql(`-- ${p.title}\nSELECT `);
        })
        .catch(() => {
          setError('Problem not found');
          navigate('/sql-judge');
        })
        .finally(() => setLoadingProblem(false));
    }
  }, [id, navigate]);

  // SSE connection for live verdicts
  const connectSSE = () => {
    const capturedSubmissionId = submissionId;

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
    sseCancelledRef.current = false;

    let cancelled = false;

    sqlJudgeApi.issueSseTicket()
      .then(({ ticket }) => {
        if (cancelled || sseCancelledRef.current) return;
        const es = new EventSource(`/api/sql/stream?ticket=${ticket}`);
        eventSourceRef.current = es;

        es.onopen = () => { if (!sseCancelledRef.current) setSseConnected(true); };
        es.onerror = () => {
          setSseConnected(false);
          es.close();
          eventSourceRef.current = null;
        };

        es.addEventListener('sql_verdict', (e) => {
          const verdict = JSON.parse(e.data);
          if (verdict.submissionId === capturedSubmissionId) {
            handleVerdict(verdict);
            es.close();
            eventSourceRef.current = null;
            setSseConnected(false);
          }
        });
      })
      .catch(() => {
        // Polling runs in useEffect in parallel — nothing to fall back to
      });

    return () => { cancelled = true; };
  };

  const startPolling = () => {
    setPolling(true);
    const poll = async () => {
      try {
        const res = await sqlJudgeApi.submissionStatus(submissionId);
        if (res.status !== 'QUEUED' && res.status !== 'RUNNING') {
          handleVerdict(res);
          setPolling(false);
        }
      } catch (e) {
        setPolling(false);
      }
    };
    const interval = setInterval(poll, 2000);
    return () => clearInterval(interval);
  };

  const pollingRef = useRef(null);

  useEffect(() => {
    if (!submissionId) return;
    
    // Start SSE (fast path)
    const sseCleanup = connectSSE();
    
    // Start polling immediately (reliable fallback) in parallel
    const capturedSid = submissionId;
    setPolling(true);
    const interval = setInterval(async () => {
      try {
        const res = await sqlJudgeApi.submissionStatus(capturedSid);
        if (res.status !== 'QUEUED' && res.status !== 'RUNNING') {
          handleVerdict(res);
        }
      } catch (e) {
        setPolling(false);
      }
    }, 1500);
    pollingRef.current = interval;
    
    return () => {
      sseCancelledRef.current = true;
      sseCleanup?.();
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, [submissionId]);

  const handleVerdict = (verdict) => {
    if (safetyRef.current) { clearTimeout(safetyRef.current); safetyRef.current = null; }
    verdictReceivedRef.current = true;
    executingRef.current = false;
    if (pollingRef.current) { clearInterval(pollingRef.current); pollingRef.current = null; }
    setStatus(verdict.status);
    setRunning(false);
    setPolling(false);
    if (verdict.resultPreview) setResult(verdict.resultPreview);
    if (verdict.errorMessage) setError(verdict.errorMessage);
  };

  const execute = async (isSubmit) => {
    if (!sql.trim() || executingRef.current) return;
    executingRef.current = true;
    setRunning(true);
    setStatus('queued');
    setResult(null);
    setError(null);
    verdictReceivedRef.current = false;
    
    if (safetyRef.current) clearTimeout(safetyRef.current);
    safetyRef.current = setTimeout(() => {
      if (!verdictReceivedRef.current) {
        setRunning(false);
        executingRef.current = false;
        setPolling(false);
        setError('Request timed out — the judge may be busy. Try again.');
      }
      safetyRef.current = null;
    }, 15000);

    try {
      const res = isSubmit 
        ? await sqlJudgeApi.submit(id, sql)
        : await sqlJudgeApi.run(id, sql);
      setSubmissionId(res.submissionId);
    } catch (e) {
      if (safetyRef.current) { clearTimeout(safetyRef.current); safetyRef.current = null; }
      setRunning(false);
      executingRef.current = false;
      setError(e.response?.data?.message || 'Execution failed');
    }
  };

  const loadHistory = async () => {
    try {
      const h = await sqlJudgeApi.mySubmissions(20);
      setHistory(h);
    } catch (e) {
      console.error('Failed to load history', e);
    }
  };

  const formatResult = (result) => {
    if (!result) return null;
    return (
      <div style={{ overflow: 'auto', maxHeight: '400px' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px', fontFamily: "'JetBrains Mono', monospace" }}>
          <thead>
            <tr style={{ background: C.surfaceHi }}>
              {result.columns.map((col, i) => (
                <th key={i} style={{ padding: '8px 12px', textAlign: 'left', borderBottom: `1px solid ${C.border}`, color: C.primary }}>
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {result.rows.map((row, ri) => (
              <tr key={ri} style={{ borderBottom: `1px solid ${C.border}` }}>
                {row.map((cell, ci) => (
                  <td key={ci} style={{ padding: '8px 12px', color: cell === '\u0000NULL' ? C.error : C.onBg }}>
                    {cell === '\u0000NULL' ? 'NULL' : cell}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
        {result.truncated && <p style={{ color: C.muted, marginTop: '8px', fontSize: '12px' }}>Result truncated</p>}
      </div>
    );
  };

  const getStatusBadge = () => {
    if (!status) return null;
    const colors = {
      ACCEPTED: C.accent,
      WRONG_ANSWER: C.error,
      TIME_LIMIT_EXCEEDED: '#ffcc00',
      RUNTIME_ERROR: C.error,
      SECURITY_VIOLATION: '#ff6b6b',
      INTERNAL_ERROR: '#ff6b6b',
    };
    return (
      <span style={{
        display: 'inline-flex', alignItems: 'center', gap: '6px',
        padding: '4px 12px', borderRadius: '4px',
        background: `${colors[status] || C.muted}22`,
        color: colors[status] || C.muted,
        fontSize: '11px', fontFamily: "'JetBrains Mono', monospace",
        textTransform: 'uppercase', letterSpacing: '0.08em',
      }}>
        {status.replace('_', ' ')}
      </span>
    );
  };

  // List view
  if (!id) {
    return (
      <div style={{ padding: '2rem', maxWidth: '1000px', margin: '0 auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <h1 style={{ color: C.primary, fontFamily: "'Playfair Display', serif", fontSize: '2rem', margin: 0 }}>
            SQL Judge
          </h1>
          <span style={{ color: C.muted, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace" }}>
            {loadingProblems ? 'Loading...' : `${problems.length} problems`}
          </span>
        </div>

        {loadingProblems ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}>
            <div style={{ width: '40px', height: '40px', borderRadius: '50%', border: `2px solid ${C.border}`, borderTopColor: C.primary, animation: 'spin 1s linear infinite' }} />
          </div>
        ) : problems.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '4rem', color: C.muted }}>
            <p>No SQL problems available yet.</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))' }}>
            {problems.map(p => (
              <Link
                key={p.id}
                to={`/sql-judge/${p.id}`}
                style={{
                  display: 'block',
                  background: C.panel,
                  border: `1px solid ${C.border}`,
                  borderRadius: '8px',
                  padding: '1.5rem',
                  textDecoration: 'none',
                  color: C.onBg,
                  transition: 'all 0.2s',
                }}
                onMouseEnter={e => { e.currentTarget.style.borderColor = C.primary; e.currentTarget.style.background = C.surfaceHi; }}
                onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.background = C.panel; }}
              >
                <h3 style={{ margin: '0 0 0.5rem', color: C.primary, fontFamily: "'Playfair Display', serif" }}>
                  {p.title}
                </h3>
                <p style={{ margin: 0, color: C.muted, fontSize: '13px', lineHeight: 1.5 }}>
                  {p.description?.slice(0, 200) || 'No description'}
                </p>
              </Link>
            ))}
          </div>
        )}
      </div>
    );
  }

  // Solve view — guard until the problem is loaded
  if (loadingProblem || !problem) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}>
        <div style={{ width: '40px', height: '40px', borderRadius: '50%', border: `2px solid ${C.border}`, borderTopColor: C.primary, animation: 'spin 1s linear infinite' }} />
      </div>
    );
  }

  const formatDescription = (text) => {
    if (!text) return null;
    const sections = text.split('\n\n').filter(s => s.trim());
    return sections.map((section, i) => {
      const lines = section.split('\n');
      const firstLine = lines[0].trim();

      if (firstLine.startsWith('## ')) {
        return (
          <h3 key={i} style={{
            margin: i > 0 ? '1.5rem 0 0.5rem' : '0 0 0.5rem',
            color: C.secondary, fontSize: '14px', fontFamily: "'Playfair Display', serif",
            fontWeight: 400, borderBottom: `1px solid ${C.border}`, paddingBottom: '0.35rem',
          }}>
            {firstLine.slice(3)}
          </h3>
        );
      }

      if (firstLine.startsWith('### ')) {
        return (
          <div key={i} style={{
            margin: '1rem 0', padding: '0.75rem 1rem',
            background: `${C.primary}0d`, border: `1px solid ${C.primary}33`,
            borderRadius: '6px',
          }}>
            <div style={{ color: C.primary, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace", marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              {firstLine.slice(4).replace(':', '')}
            </div>
            <div style={{ color: C.onBg, fontSize: '13px', lineHeight: 1.6, fontFamily: "'JetBrains Mono', monospace", whiteSpace: 'pre-wrap' }}>
              {lines.slice(1).join('\n')}
            </div>
          </div>
        );
      }

      if (section.includes('+---') && section.includes('|')) {
        return (
          <div key={i} style={{
            margin: '1rem 0', background: C.bg, border: `1px solid ${C.border}`,
            borderRadius: '6px', overflow: 'hidden',
          }}>
            <div style={{
              padding: '0.4rem 0.75rem', background: C.surfaceHi, borderBottom: `1px solid ${C.border}`,
              color: C.muted, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase', letterSpacing: '0.06em',
            }}>
              {lines.length > 3 && lines[2].includes('|') ? lines[0].trim() : 'Table Schema'}
            </div>
            <div style={{
              padding: '0.75rem 1rem', overflow: 'auto',
              color: C.onBg, fontSize: '12px', lineHeight: 1.5,
              fontFamily: "'JetBrains Mono', monospace", whiteSpace: 'pre',
            }}>
              {section}
            </div>
          </div>
        );
      }

      return (
        <p key={i} style={{
          margin: i > 0 ? '0.75rem 0 0' : '0 0 0.75rem',
          color: C.onBg, fontSize: '13px', lineHeight: 1.7,
          fontFamily: 'system-ui, -apple-system, sans-serif',
        }}>
          {section.split('\n').map((l, j) => (
            <span key={j}>
              {l}
              {j < section.split('\n').length - 1 && <br />}
            </span>
          ))}
        </p>
      );
    });
  };

  // Solve view
  return (
    <div style={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
      {/* Top bar */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: '1rem', padding: '0.75rem 1.5rem',
        background: C.panel, borderBottom: `1px solid ${C.border}`,
        minHeight: '48px',
      }}>
        <button onClick={() => navigate('/sql-judge')} style={{
          background: 'none', border: `1px solid ${C.border}`, color: C.muted,
          padding: '4px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '11px',
          fontFamily: "'JetBrains Mono', monospace",
        }}>
          ← Back
        </button>
        <span style={{ color: C.muted, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace" }}>#{problem.id}</span>
        <h2 style={{ margin: 0, color: C.primary, fontFamily: "'Playfair Display', serif", fontSize: '1.15rem', fontWeight: 400 }}>
          {problem.title}
        </h2>
        <div style={{ flex: 1 }} />
        {status && getStatusBadge()}
        {sseConnected && <span style={{ color: C.accent, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace" }}>● Live</span>}
        {polling && <span style={{ color: C.secondary, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace" }}>⟳ Polling...</span>}
      </div>

      {/* Main split area */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* LEFT: Description */}
        <div style={{
          width: '38%', minWidth: '320px', borderRight: `1px solid ${C.border}`,
          overflow: 'auto', padding: '1.25rem 1.5rem',
        }}>
          {formatDescription(problem.description)}
        </div>

        {/* RIGHT: Editor + Results */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minWidth: 0 }}>
          {/* Editor area */}
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
            <div style={{
              padding: '0.5rem 1rem', background: C.surfaceLow,
              borderBottom: `1px solid ${C.border}`,
              display: 'flex', alignItems: 'center', gap: '0.5rem',
            }}>
              <span style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                SQL
              </span>
            </div>
            <div style={{ flex: 1, minHeight: '200px' }}>
              <Editor
                height="100%"
                defaultLanguage="sql"
                value={sql}
                onChange={(val) => setSql(val || '')}
                theme="vs-dark"
                options={{
                  fontSize: 13,
                  fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                  minimap: { enabled: false },
                  lineNumbers: 'on',
                  scrollBeyondLastLine: false,
                  wordWrap: 'on',
                  padding: { top: 8 },
                  renderLineHighlight: 'none',
                  overviewRulerLanes: 0,
                  hideCursorInOverviewRuler: true,
                  overviewRulerBorder: false,
                  glyphMargin: false,
                  folding: false,
                  lineDecorationsWidth: 8,
                  lineNumbersMinChars: 3,
                  readOnly: running,
                }}
                loading={<div style={{ color: C.muted, padding: '1rem', fontSize: '12px' }}>Loading editor...</div>}
              />
            </div>
          </div>

          {/* Buttons + Status + Results panel */}
          <div style={{
            borderTop: `1px solid ${C.border}`, background: C.surfaceLow,
            padding: '0.75rem 1rem', display: 'flex', flexDirection: 'column', gap: '0.75rem',
            maxHeight: result ? '45%' : 'auto', overflow: 'auto',
          }}>
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
              <button
                onClick={() => execute(false)}
                disabled={running || !sql.trim()}
                style={{
                  background: C.runBtn, color: '#fff', border: 'none',
                  padding: '8px 16px', borderRadius: '4px', cursor: running ? 'not-allowed' : 'pointer',
                  fontWeight: 500, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
                  opacity: running || !sql.trim() ? 0.5 : 1,
                }}
              >
                {running ? '▶ Running...' : '▶ Run'}
              </button>
              <button
                onClick={() => execute(true)}
                disabled={running || !sql.trim()}
                style={{
                  background: C.submitBtn, color: '#0e0e0e', border: 'none',
                  padding: '8px 16px', borderRadius: '4px', cursor: running ? 'not-allowed' : 'pointer',
                  fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
                  opacity: running || !sql.trim() ? 0.5 : 1,
                }}
              >
                {running ? 'Submitting...' : 'Submit'}
              </button>
              <button
                onClick={() => { setShowHistory(!showHistory); if (!history.length) loadHistory(); }}
                style={{
                  background: 'none', border: `1px solid ${C.border}`, color: C.muted,
                  padding: '6px 12px', borderRadius: '4px', cursor: 'pointer',
                  fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
                }}
              >
                History ({history.length})
              </button>
              <div style={{ flex: 1 }} />
              {error && <span style={{ color: C.error, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace" }}>{error}</span>}
            </div>

            {/* Result table */}
            {result && (
              <div style={{ flex: 1, overflow: 'auto', minHeight: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ color: C.primary, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                    Output ({result.columns.length} cols, {result.rows.length} rows)
                  </span>
                </div>
                {formatResult(result)}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* History drawer */}
      {showHistory && history.length > 0 && (
        <div style={{
          background: C.panel, borderTop: `1px solid ${C.border}`, padding: '1rem',
          maxHeight: '200px', overflow: 'auto',
        }}>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            {history.slice(0, 10).map(s => (
              <div key={s.id} style={{
                display: 'flex', alignItems: 'center', gap: '0.5rem',
                padding: '0.35rem 0.75rem', background: C.bg, border: `1px solid ${C.border}`, borderRadius: '3px',
              }}>
                <span style={{
                  minWidth: '80px', padding: '1px 6px', borderRadius: '2px',
                  background: s.status === 'ACCEPTED' ? `${C.accent}22` : `${C.error}22`,
                  color: s.status === 'ACCEPTED' ? C.accent : C.error,
                  fontSize: '9px', fontFamily: "'JetBrains Mono', monospace", textAlign: 'center',
                  textTransform: 'uppercase',
                }}>
                  {s.status.replace('_', ' ')}
                </span>
                <span style={{ color: C.muted, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace" }}>
                  {s.executionTimeMs}ms
                </span>
                <span style={{ color: C.outline, fontSize: '9px' }}>
                  {new Date(s.submittedAt).toLocaleTimeString()}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
};

export default SqlJudge;