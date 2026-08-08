import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useParams, useLocation } from 'react-router-dom';
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
        if (cancelled || sseCancelledRef.current) return;
        startPolling();
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

  useEffect(() => {
    if (!submissionId) return;
    const cleanup = connectSSE();
    return () => {
      sseCancelledRef.current = true;
      cleanup?.();
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, [submissionId]);

  const handleVerdict = (verdict) => {
    setStatus(verdict.status);
    setRunning(false);
    setPolling(false);
    if (verdict.resultPreview) setResult(verdict.resultPreview);
    if (verdict.errorMessage) setError(verdict.errorMessage);
  };

  const execute = async (isSubmit) => {
    if (!sql.trim()) return;
    setRunning(true);
    setStatus('queued');
    setResult(null);
    setError(null);
    
    try {
      const res = isSubmit 
        ? await sqlJudgeApi.submit(id, sql)
        : await sqlJudgeApi.run(id, sql);
      setSubmissionId(res.submissionId);
    } catch (e) {
      setRunning(false);
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

  // Solve view
  return (
    <div style={{ display: 'flex', gap: '2rem', padding: '2rem', maxWidth: '1400px', margin: '0 auto', flex: 1 }}>
      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
          <button onClick={() => navigate('/sql-judge')} style={{
            background: 'none', border: `1px solid ${C.border}`, color: C.muted,
            padding: '6px 12px', borderRadius: '4px', cursor: 'pointer', fontSize: '12px',
            fontFamily: "'JetBrains Mono', monospace",
          }}>
            ← All Problems
          </button>
          <h2 style={{ margin: 0, color: C.primary, fontFamily: "'Playfair Display', serif", fontSize: '1.75rem' }}>
            {problem.title}
          </h2>
        </div>

        <div style={{ background: C.panel, border: `1px solid ${C.border}`, borderRadius: '8px', padding: '1.5rem', color: C.muted, fontSize: '14px', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
          {problem.description}
        </div>

        <div style={{ background: C.panel, border: `1px solid ${C.border}`, borderRadius: '8px', padding: '1rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <label style={{ color: C.muted, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            Your SQL Query
          </label>
          <textarea
            value={sql}
            onChange={e => setSql(e.target.value)}
            disabled={running}
            style={{
              background: C.bg, border: `1px solid ${C.border}`, borderRadius: '4px',
              color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px',
              padding: '1rem', minHeight: '200px', resize: 'vertical',
              outline: 'none', width: '100%', boxSizing: 'border-box',
            }}
            placeholder="SELECT * FROM employees WHERE ..."
            spellCheck={false}
          />
          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            <button
              onClick={() => execute(false)}
              disabled={running || !sql.trim()}
              style={{
                background: C.runBtn, color: '#0e0e0e', border: 'none',
                padding: '10px 20px', borderRadius: '4px', cursor: running ? 'not-allowed' : 'pointer',
                fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
                textTransform: 'uppercase', letterSpacing: '0.06em',
                opacity: running || !sql.trim() ? 0.5 : 1,
              }}
            >
              {running ? 'Running...' : 'Run (Test)'}
            </button>
            <button
              onClick={() => execute(true)}
              disabled={running || !sql.trim()}
              style={{
                background: C.submitBtn, color: '#0e0e0e', border: 'none',
                padding: '10px 20px', borderRadius: '4px', cursor: running ? 'not-allowed' : 'pointer',
                fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
                textTransform: 'uppercase', letterSpacing: '0.06em',
                opacity: running || !sql.trim() ? 0.5 : 1,
              }}
            >
              {running ? 'Submitting...' : 'Submit'}
            </button>
            <button
              onClick={() => { setShowHistory(!showHistory); if (!history.length) loadHistory(); }}
              style={{
                background: 'none', border: `1px solid ${C.border}`, color: C.muted,
                padding: '10px 16px', borderRadius: '4px', cursor: 'pointer',
                fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
              }}
            >
              History ({history.length})
            </button>
          </div>

          {status && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
              {getStatusBadge()}
              {sseConnected && <span style={{ color: C.accent, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace" }}>● Live</span>}
              {polling && <span style={{ color: C.secondary, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace" }}>⟳ Polling...</span>}
              {error && <span style={{ color: C.error, fontSize: '12px' }}>{error}</span>}
            </div>
          )}

          {result && (
            <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: `1px solid ${C.border}` }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span style={{ color: C.primary, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', textTransform: 'uppercase' }}>
                  Result ({result.columns.length} cols, {result.rows.length} rows)
                </span>
              </div>
              {formatResult(result)}
            </div>
          )}
        </div>

        {showHistory && history.length > 0 && (
          <div style={{ background: C.panel, border: `1px solid ${C.border}`, borderRadius: '8px', padding: '1rem' }}>
            <h4 style={{ margin: '0 0 1rem', color: C.primary, fontFamily: "'Playfair Display', serif" }}>Recent Submissions</h4>
            <div style={{ display: 'grid', gap: '0.5rem' }}>
              {history.map(s => (
                <div key={s.id} style={{
                  display: 'flex', alignItems: 'center', gap: '1rem',
                  padding: '0.75rem', background: C.bg, border: `1px solid ${C.border}`, borderRadius: '4px',
                }}>
                  <span style={{
                    width: '100px', padding: '2px 8px', borderRadius: '3px',
                    background: s.status === 'ACCEPTED' ? `${C.accent}22` : `${C.error}22`,
                    color: s.status === 'ACCEPTED' ? C.accent : C.error,
                    fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", textAlign: 'center',
                    textTransform: 'uppercase', letterSpacing: '0.06em',
                  }}>
                    {s.status}
                  </span>
                  <span style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", flex: 1 }}>
                    {s.testRun ? 'Run' : 'Submit'} · {s.executionTimeMs}ms
                  </span>
                  <span style={{ color: C.outline, fontSize: '10px' }}>
                    {new Date(s.submittedAt).toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        textarea::-webkit-scrollbar { width: 8px; }
        textarea::-webkit-scrollbar-track { background: ${C.bg}; }
        textarea::-webkit-scrollbar-thumb { background: ${C.border}; border-radius: 4px; }
      `}</style>
    </div>
  );
};

export default SqlJudge;