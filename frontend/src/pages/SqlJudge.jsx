import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useParams, useLocation } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import { sqlJudgeApi } from '../services/api';

const C = {
  bg: '#0a0a0b',
  panel: '#111114',
  panelHi: '#18181c',
  border: '#242429',
  borderHi: '#33333a',
  primary: '#f1bc8b',
  primaryDim: '#8a6d52',
  secondary: '#e9c176',
  gold: '#e2b96f',
  muted: '#78787e',
  onBg: '#e1e1e3',
  onBgDim: '#9d9da3',
  accent: '#7ec49a',
  accentDim: '#3d6b51',
  error: '#f48771',
  errorDim: '#632d24',
  runBtn: '#4a9eff',
  submitBtn: '#f1bc8b',
  surfaceLow: '#0d0d10',
  surfaceHi: '#1a1a1f',
  outline: '#3a3a40',
  schemaBg: '#0f1118',
  schemaBorder: '#1e2233',
  schemaHeader: 'linear-gradient(135deg, #1a1d2e 0%, #181b24 100%)',
  outputBg: '#0d1117',
  outputBorder: '#1a2332',
  hintBg: '#1a1a15',
  hintBorder: '#3a3520',
  cardShadow: '0 1px 3px rgba(0,0,0,0.4)',
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
      <div style={{ overflow: 'auto', borderRadius: '6px', border: `1px solid ${C.outputBorder}` }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px', fontFamily: "'JetBrains Mono', monospace" }}>
          <thead>
            <tr style={{ background: C.outputBg }}>
              {result.columns.map((col, i) => (
                <th key={i} style={{
                  padding: '7px 14px', textAlign: 'left', borderBottom: `1px solid ${C.outputBorder}`,
                  color: '#6e9ecf', fontWeight: 600, fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.04em',
                }}>
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {result.rows.map((row, ri) => (
              <tr key={ri} style={{ background: ri % 2 === 0 ? 'transparent' : `${C.outputBg}` }}>
                {row.map((cell, ci) => (
                  <td key={ci} style={{
                    padding: '6px 14px', borderBottom: `1px solid ${C.border}20`,
                    color: cell === '\u0000NULL' ? C.error : C.onBg, fontWeight: cell === '\u0000NULL' ? 600 : 400,
                    fontSize: '11.5px',
                  }}>
                    {cell === '\u0000NULL' ? 'NULL' : cell}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
        {result.truncated && <p style={{ color: C.muted, marginTop: '6px', padding: '0 14px 8px', fontSize: '10px' }}>Result truncated — showing first rows only</p>}
      </div>
    );
  };

  const getStatusBadge = () => {
    if (!status) return null;
    const config = {
      ACCEPTED: { bg: C.accentDim, color: C.accent, icon: '✓' },
      WRONG_ANSWER: { bg: C.errorDim, color: C.error, icon: '✗' },
      TIME_LIMIT_EXCEEDED: { bg: '#3d3520', color: '#e2b96f', icon: '⏱' },
      RUNTIME_ERROR: { bg: C.errorDim, color: C.error, icon: '⚠' },
      SECURITY_VIOLATION: { bg: '#3d2020', color: '#ff6b6b', icon: '🔒' },
      INTERNAL_ERROR: { bg: '#3d2020', color: '#ff6b6b', icon: '⚡' },
      ERROR: { bg: C.errorDim, color: C.error, icon: '⚠' },
    };
    const c = config[status] || { bg: '#2a2a2a', color: C.muted, icon: '' };
    return (
      <span style={{
        display: 'inline-flex', alignItems: 'center', gap: '5px',
        padding: '3px 10px', borderRadius: '4px',
        background: c.bg, border: `1px solid ${c.color}30`,
        color: c.color, fontSize: '10.5px', fontFamily: "'JetBrains Mono', monospace",
        fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em',
      }}>
        <span style={{ fontSize: '11px' }}>{c.icon}</span>
        {status.replace(/_/g, ' ')}
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

  const [collapsedSections, setCollapsedSections] = useState({});

  const formatDescription = (text) => {
    if (!text) return null;
    const sections = text.split('\n\n').filter(s => s.trim());
    return sections.map((section, i) => {
      const lines = section.split('\n');
      const firstLine = lines[0].trim();

      // ## Section headers — elegant with icon indicator
      if (firstLine.startsWith('## ')) {
        const label = firstLine.slice(3).trim();
        const collapsed = collapsedSections[i];
        const isSchema = label.toLowerCase().includes('table') || label.toLowerCase().includes('schema');
        const isOutput = label.toLowerCase().includes('expected') || label.toLowerCase().includes('output');
        const accentCol = isSchema ? '#6e9ecf' : isOutput ? C.accent : C.gold;
        return (
          <div key={i} style={{ margin: '1.25rem 0 0.35rem', display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}
               onClick={() => setCollapsedSections(prev => ({...prev, [i]: !collapsed}))}>
            <span style={{
              width: '18px', height: '18px', borderRadius: '3px',
              background: `${accentCol}18`, border: `1px solid ${accentCol}40`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '10px', color: accentCol, flexShrink: 0, transition: 'transform 0.2s',
              transform: collapsed ? 'rotate(-90deg)' : 'rotate(0deg)',
            }}>▼</span>
            <h3 style={{ margin: 0, color: accentCol, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace", fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
              {label}
            </h3>
          </div>
        );
      }

      if (collapsedSections[i]) return null;

      // ### Hint / Example — beautiful card with left accent bar
      if (firstLine.startsWith('### ')) {
        return (
          <div key={i} style={{
            margin: '0.75rem 0', padding: '0.85rem 1rem 0.85rem 0.85rem',
            background: C.hintBg, border: `1px solid ${C.hintBorder}`,
            borderRadius: '6px', borderLeft: `3px solid ${C.gold}`,
            display: 'flex', gap: '0.75rem',
          }}>
            <span style={{ color: C.gold, fontSize: '14px', flexShrink: 0, marginTop: 1 }}>💡</span>
            <div>
              <div style={{ color: C.gold, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", marginBottom: '0.4rem', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>
                {firstLine.slice(4).replace(':', '')}
              </div>
              <div style={{ color: C.onBgDim, fontSize: '12.5px', lineHeight: 1.65, fontFamily: "'JetBrains Mono', monospace", whiteSpace: 'pre-wrap' }}>
                {lines.slice(1).join('\n')}
              </div>
            </div>
          </div>
        );
      }

      // Table Schema — gorgeous card with gradient header
      if (section.includes('+---') && section.includes('|')) {
        const headerLine = lines.length > 3 && lines[2].includes('|') ? lines[0].trim() : 'Table Schema';
        const rows = [];
        let colWidths = [];
        for (const line of lines) {
          if (line.includes('+---')) continue;
          if (line.trim().startsWith('|')) {
            const cells = line.split('|').filter(c => c.trim()).map(c => c.trim());
            if (cells.length > 0) rows.push(cells);
          }
        }
        return (
          <div key={i} style={{
            margin: '0.5rem 0 1rem', background: C.schemaBg,
            border: `1px solid ${C.schemaBorder}`, borderRadius: '8px',
            overflow: 'hidden', boxShadow: C.cardShadow,
          }}>
            <div style={{
              padding: '0.5rem 0.85rem',
              background: 'linear-gradient(135deg, #1a1d2e 0%, #151827 100%)',
              borderBottom: `1px solid ${C.schemaBorder}`,
              display: 'flex', alignItems: 'center', gap: '0.5rem',
            }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6e9ecf" strokeWidth="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="3" x2="9" y2="21"/></svg>
              <span style={{ color: '#6e9ecf', fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                {headerLine}
              </span>
            </div>
            <div style={{ padding: '0.5rem 0', overflow: 'auto' }}>
              {rows.length > 0 && (
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '11px', fontFamily: "'JetBrains Mono', monospace" }}>
                  <tbody>
                    {rows.map((row, ri) => (
                      <tr key={ri} style={{ background: ri % 2 === 0 ? 'transparent' : `${C.surfaceHi}40` }}>
                        {row.map((cell, ci) => (
                          <td key={ci} style={{
                            padding: '3px 10px',
                            color: ri === 0 ? '#6e9ecf' : C.onBgDim,
                            fontWeight: ri === 0 ? 600 : 400,
                            borderBottom: ri < rows.length - 1 ? `1px solid ${C.border}20` : 'none',
                          }}>
                            {cell}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        );
      }

      // Plain text paragraph
      return (
        <p key={i} style={{
          margin: '0.35rem 0 0.65rem',
          color: C.onBgDim, fontSize: '13px', lineHeight: 1.72,
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
        display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.55rem 1.2rem',
        background: C.panel, borderBottom: `1px solid ${C.border}`,
        minHeight: '42px',
      }}>
        <button onClick={() => navigate('/sql-judge')} style={{
          background: C.surfaceHi, border: `1px solid ${C.border}`, color: C.muted,
          padding: '3px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '11px',
          fontFamily: "'JetBrains Mono', monospace",
        }}>
          ← Problems
        </button>
        <div style={{ flex: 1 }} />
        {status && getStatusBadge()}
        {sseConnected && <span style={{ color: C.accent, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", fontWeight: 500 }}>● Live</span>}
        {polling && <span style={{ color: C.gold, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace" }}>⟳ Polling</span>}
      </div>

      {/* Main split area */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* LEFT: Description */}
        <div style={{
          width: '40%', minWidth: '340px', borderRight: `1px solid ${C.border}`,
          background: C.surfaceLow, overflow: 'auto', padding: '1.4rem 1.6rem',
        }}>
          <div style={{ marginBottom: '0.25rem' }}>
            <span style={{ color: C.muted, fontSize: '9px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase', letterSpacing: '0.1em' }}>
              Problem #{problem.id}
            </span>
          </div>
          <h2 style={{ margin: '0 0 1rem', color: C.onBg, fontFamily: "'Playfair Display', serif", fontSize: '1.2rem', fontWeight: 400, lineHeight: 1.3 }}>
            {problem.title}
          </h2>
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