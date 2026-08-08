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
  const [isTestRun, setIsTestRun] = useState(false);
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
  const [collapsedSections, setCollapsedSections] = useState({});

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
    setIsTestRun(!isSubmit);
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
      const h = await sqlJudgeApi.mySubmissions(20, id);
      setHistory(h);
    } catch (e) {
      console.error('Failed to load history', e);
    }
  };

  const formatResult = (result) => {
    if (!result) return null;
    return (
      <div style={{ overflow: 'auto', borderRadius: '6px', border: '1px solid #2a2518', background: '#0d0d0a' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px', fontFamily: "'JetBrains Mono', monospace" }}>
          <thead>
            <tr style={{ background: '#14120b' }}>
              {result.columns.map((col, i) => (
                <th key={i} style={{
                  padding: '7px 14px', textAlign: 'left', borderBottom: '1px solid #2a2518',
                  color: '#c9a96e', fontWeight: 600, fontSize: '10.5px', textTransform: 'uppercase', letterSpacing: '0.06em',
                }}>
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {result.rows.map((row, ri) => (
              <tr key={ri} style={{ background: ri % 2 === 0 ? 'transparent' : '#0d0d0a' }}>
                {row.map((cell, ci) => (
                  <td key={ci} style={{
                    padding: '6px 14px', borderBottom: '1px solid #2a251820',
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
    const displayStatus = isTestRun && status === 'ACCEPTED' ? 'RUN_OK' : status;
    const displayConfig = isTestRun && status === 'ACCEPTED' 
      ? { bg: '#1c1912', color: '#9d8e83', icon: '▶' }
      : c;
    return (
      <span style={{
        display: 'inline-flex', alignItems: 'center', gap: '5px',
        padding: '3px 10px', borderRadius: '4px',
        background: displayConfig.bg, border: `1px solid ${displayConfig.color}30`,
        color: displayConfig.color, fontSize: '10.5px', fontFamily: "'JetBrains Mono', monospace",
        fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em',
      }}>
        <span style={{ fontSize: '11px' }}>{displayConfig.icon}</span>
        {isTestRun && status === 'ACCEPTED' ? 'Run OK' : status.replace(/_/g, ' ')}
      </span>
    );
  };

  // List view
  if (!id) {
    if (loadingProblems) {
      return (
        <div style={{ display: 'flex', flex: 1, alignItems: 'center', justifyContent: 'center', background: C.bg }}>
          <div style={{ textAlign: 'center' }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '50%', border: '2px solid #2a2518', borderTopColor: '#c9a96e', animation: 'spin 1s linear infinite', margin: '0 auto 1rem' }} />
            <p style={{ color: C.muted, fontSize: '13px', fontFamily: "'JetBrains Mono', monospace" }}>Loading problems…</p>
          </div>
        </div>
      );
    }

    const easy = problems.filter(p => (p.description || '').length < 400).length;
    const medium = problems.filter(p => { const l = (p.description || '').length; return l >= 400 && l < 800; }).length;
    const hard = problems.filter(p => (p.description || '').length >= 800).length;

    return (
      <div style={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'auto', background: C.bg }}>
        {/* Hero header */}
        <div style={{
          background: 'linear-gradient(180deg, #14100a 0%, #0d0c09 60%, #0a0a0b 100%)',
          borderBottom: '1px solid #1f1c14',
          padding: '3rem 2rem 2.5rem',
        }}>
          <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
            <div style={{ display: 'flex', alignItems: 'flex-end', gap: '2rem', marginBottom: '0.5rem' }}>
              <div>
                <span style={{ color: '#c9a96e', fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase', letterSpacing: '0.12em', fontWeight: 600 }}>
                  Database
                </span>
                <h1 style={{ margin: '0.25rem 0 0', color: C.onBg, fontFamily: "'Playfair Display', serif", fontSize: '2.6rem', fontWeight: 400 }}>
                  SQL Judge
                </h1>
                <p style={{ margin: '0.5rem 0 0', color: C.onBgDim, fontSize: '14px', lineHeight: 1.6, maxWidth: '500px', fontFamily: 'system-ui' }}>
                  Practice SQL queries on real datasets across six distributed execution nodes. Write, run, and submit your solutions.
                </p>
              </div>
              <div style={{ flex: 1 }} />
              <div style={{ display: 'flex', gap: '1.5rem', paddingBottom: '0.5rem' }}>
                {[{ label: 'Total', value: problems.length, color: '#c9a96e' },
                  { label: 'Easy', value: easy, color: C.accent },
                  { label: 'Medium', value: medium, color: '#e2b96f' },
                  { label: 'Hard', value: hard, color: C.error },
                ].map(s => (
                  <div key={s.label} style={{ textAlign: 'center' }}>
                    <div style={{ color: s.color, fontSize: '1.8rem', fontFamily: "'Playfair Display', serif", fontWeight: 600, lineHeight: 1 }}>
                      {s.value}
                    </div>
                    <div style={{ color: C.muted, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase', letterSpacing: '0.08em', marginTop: '0.2rem' }}>
                      {s.label}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Problems grid */}
        <div style={{ flex: 1, padding: '2rem', maxWidth: '1100px', margin: '0 auto', width: '100%', boxSizing: 'border-box' }}>
          {problems.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '5rem 0' }}>
              <div style={{ fontSize: '2.5rem', marginBottom: '1rem', opacity: 0.3 }}>🗄️</div>
              <p style={{ color: C.muted, fontSize: '14px', fontFamily: "'JetBrains Mono', monospace" }}>No SQL problems available yet.</p>
              <p style={{ color: C.onBgDim, fontSize: '12px', marginTop: '0.5rem' }}>Problems will appear here once created by an admin.</p>
            </div>
          ) : (
            <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))' }}>
              {problems.map((p, idx) => {
                const d = p.description || '';
                const isHard = d.length >= 800;
                const isMedium = d.length >= 400 && d.length < 800;
                const diff = isHard ? 'Hard' : isMedium ? 'Medium' : 'Easy';
                const diffColor = isHard ? C.error : isMedium ? '#e2b96f' : C.accent;
                const diffBg = isHard ? '#2d1a1a' : isMedium ? '#2a2415' : '#121c14';
                
                return (
                  <Link key={p.id} to={`/sql-judge/${p.id}`} style={{ textDecoration: 'none' }}>
                    <div style={{
                      background: '#0d0d0d', border: '1px solid #1f1c14', borderRadius: '10px',
                      padding: '1.4rem 1.5rem', transition: 'all 0.25s ease',
                      height: '100%', display: 'flex', flexDirection: 'column',
                    }}
                      onMouseEnter={e => {
                        e.currentTarget.style.borderColor = '#c9a96e40';
                        e.currentTarget.style.background = '#11110e';
                        e.currentTarget.style.transform = 'translateY(-2px)';
                        e.currentTarget.style.boxShadow = '0 8px 30px rgba(0,0,0,0.4)';
                      }}
                      onMouseLeave={e => {
                        e.currentTarget.style.borderColor = '#1f1c14';
                        e.currentTarget.style.background = '#0d0d0d';
                        e.currentTarget.style.transform = 'translateY(0)';
                        e.currentTarget.style.boxShadow = 'none';
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.75rem' }}>
                        <span style={{
                          background: diffBg, color: diffColor,
                          padding: '2px 8px', borderRadius: '3px',
                          fontSize: '9px', fontFamily: "'JetBrains Mono', monospace",
                          fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.08em',
                        }}>
                          {diff}
                        </span>
                        <span style={{ color: C.muted, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace" }}>
                          #{p.id}
                        </span>
                      </div>
                      
                      <h3 style={{
                        margin: '0 0 0.6rem', color: C.onBg, fontFamily: "'Playfair Display', serif",
                        fontSize: '1.15rem', fontWeight: 400, lineHeight: 1.35,
                      }}>
                        {p.title}
                      </h3>
                      
                      <p style={{
                        margin: 0, color: C.onBgDim, fontSize: '12.5px', lineHeight: 1.55,
                        flex: 1, overflow: 'hidden', display: '-webkit-box',
                        WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
                      }}>
                        {d.split('\n')[0]?.slice(0, 150) || 'No description'}
                      </p>

                      <div style={{ marginTop: '1rem', display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                        {(d.includes('JOIN') || d.includes('join')) && (
                          <span style={{ color: '#c9a96e', fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", background: '#1f1b1210', border: '1px solid #2a251820', padding: '2px 6px', borderRadius: '3px' }}>JOIN</span>
                        )}
                        {(d.includes('GROUP BY') || d.includes('COUNT') || d.includes('SUM') || d.includes('AVG')) && (
                          <span style={{ color: C.accent, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", background: '#121c1410', border: '1px solid #1a2a1a20', padding: '2px 6px', borderRadius: '3px' }}>Aggregation</span>
                        )}
                        {(d.includes('HAVING')) && (
                          <span style={{ color: '#e2b96f', fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", background: '#2a241510', border: '1px solid #3a352020', padding: '2px 6px', borderRadius: '3px' }}>Filtering</span>
                        )}
                        {(d.includes('LEFT JOIN')) && (
                          <span style={{ color: '#c9a96e', fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", background: '#1f1b1210', border: '1px solid #2a251820', padding: '2px 6px', borderRadius: '3px' }}>Advanced</span>
                        )}
                      </div>
                    </div>
                  </Link>
                );
              })}
            </div>
          )}
        </div>

        <style>{`
          @keyframes spin { to { transform: rotate(360deg); } }
        `}</style>
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
    
    let collapseUntilNextHeader = false;
    
    return sections.map((section, i) => {
      const lines = section.split('\n');
      const firstLine = lines[0].trim();

      if (firstLine.startsWith('## ')) {
        const label = firstLine.slice(3).trim();
        const collapsed = collapsedSections[i];
        const isTable = label.toLowerCase().includes('table') || label.toLowerCase().includes('schema');
        const isOutput = label.toLowerCase().includes('expected') || label.toLowerCase().includes('output');
        const accent = isTable ? '#c9a96e' : isOutput ? C.accent : C.gold;
        const bg = isTable ? '#1f1b12' : isOutput ? '#121c14' : '#1c1912';
        collapseUntilNextHeader = !!collapsed;
        return (
          <div key={i} style={{
            margin: '1.25rem 0 0.35rem', display: 'flex', alignItems: 'center', gap: '0.5rem',
            cursor: 'pointer', userSelect: 'none',
          }} onClick={() => setCollapsedSections(prev => ({...prev, [i]: !prev[i]}))}>
            <span style={{
              width: '18px', height: '18px', borderRadius: '3px',
              background: bg, border: `1px solid ${accent}40`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '10px', color: accent, flexShrink: 0, transition: 'transform 0.2s',
              transform: collapsed ? 'rotate(-90deg)' : 'rotate(0deg)',
            }}>▼</span>
            <h3 style={{ margin: 0, color: accent, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace", fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
              {label}
            </h3>
          </div>
        );
      }

      if (collapseUntilNextHeader) return null;

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
            margin: '0.5rem 0 1rem', background: '#0f100d',
            border: '1px solid #2a2518', borderRadius: '8px',
            overflow: 'hidden', boxShadow: C.cardShadow,
          }}>
            <div style={{
              padding: '0.5rem 0.85rem',
              background: 'linear-gradient(135deg, #1f1b10 0%, #181510 100%)',
              borderBottom: '1px solid #2a2518',
              display: 'flex', alignItems: 'center', gap: '0.5rem',
            }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#c9a96e" strokeWidth="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="3" x2="9" y2="21"/></svg>
              <span style={{ color: '#c9a96e', fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.08em' }}>
                {headerLine}
              </span>
            </div>
            <div style={{ padding: '0.5rem 0', overflow: 'auto' }}>
              {rows.length > 0 && (
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '11px', fontFamily: "'JetBrains Mono', monospace" }}>
                  <tbody>
                    {rows.map((row, ri) => (
                      <tr key={ri} style={{ background: ri % 2 === 0 ? 'transparent' : `${C.surfaceHi}30` }}>
                        {row.map((cell, ci) => (
                          <td key={ci} style={{
                            padding: '3px 10px',
                            color: ri === 0 ? '#c9a96e' : C.onBgDim,
                            fontWeight: ri === 0 ? 600 : 400,
                            borderBottom: ri < rows.length - 1 ? '1px solid #2a251820' : 'none',
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

      // Pipe-separated table without borders — render as output/preview
      if (lines.every(l => l.trim().startsWith('|') || l.trim().startsWith('-') || l.trim() === '')) {
        const pipeRows = [];
        for (const line of lines) {
          if (line.includes('---')) continue;
          if (line.trim().startsWith('|')) {
            const cells = line.split('|').filter(c => c.trim()).map(c => c.trim());
            if (cells.length > 0) pipeRows.push(cells);
          }
        }
        if (pipeRows.length < 2) return null;
        return (
          <div key={i} style={{
            margin: '0.35rem 0 0.75rem', background: '#0d0f0c',
            border: '1px solid #1a2a1a', borderRadius: '6px',
            overflow: 'hidden', boxShadow: C.cardShadow,
          }}>
            <div style={{
              padding: '0.35rem 0.75rem',
              background: 'linear-gradient(135deg, #121c14 0%, #0e160f 100%)',
              borderBottom: '1px solid #1a2a1a',
              display: 'flex', alignItems: 'center', gap: '0.4rem',
            }}>
              <span style={{ color: C.accent, fontSize: '11px' }}>▹</span>
              <span style={{ color: C.accent, fontSize: '9.5px', fontFamily: "'JetBrains Mono', monospace", fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.08em' }}>
                Expected Output
              </span>
            </div>
            <div style={{ padding: '0.4rem 0', overflow: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '11px', fontFamily: "'JetBrains Mono', monospace" }}>
                <tbody>
                  {pipeRows.map((row, ri) => (
                    <tr key={ri} style={{ background: ri % 2 === 0 ? 'transparent' : '#0a0f0a30' }}>
                      {row.map((cell, ci) => (
                        <td key={ci} style={{
                          padding: '3px 9px',
                          color: ri === 0 ? C.accent : C.onBgDim,
                          fontWeight: ri === 0 ? 600 : 400,
                          borderBottom: ri < pipeRows.length - 1 ? '1px solid #1a2a1a20' : 'none',
                          fontSize: '10.5px',
                        }}>
                          {cell === '...' ? <span style={{ color: C.muted, fontStyle: 'italic' }}>...</span> : cell}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
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
    <div className="sql-judge-workspace" style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0, overflow: 'hidden' }}>
      {/* Top bar */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.45rem 1rem',
        background: '#0d0d0c', borderBottom: '1px solid #1f1c14',
        minHeight: '40px', flexShrink: 0,
      }}>
        <button onClick={() => navigate('/sql-judge')} style={{
          background: C.surfaceHi, border: `1px solid ${C.border}`, color: C.muted,
          padding: '3px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '11px',
          fontFamily: "'JetBrains Mono', monospace",
        }}>
          ← Problems
        </button>
        <span style={{ color: C.onBgDim, fontSize: '12px', fontFamily: "'Playfair Display', serif" }}>{problem.title}</span>
        <div style={{ flex: 1 }} />
        {status && getStatusBadge()}
        {sseConnected && <span style={{ color: C.accent, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace" }}>● Live</span>}
        {polling && <span style={{ color: C.gold, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace" }}>⟳ Polling</span>}
      </div>

      {/* Main split — fixed height, independent scroll */}
      <div className="sql-judge-split" style={{ display: 'flex', flex: 1, minHeight: 0, overflow: 'hidden' }}>
        {/* LEFT: Description panel */}
        <div className="sql-judge-description" style={{
          width: '42%', minWidth: '360px', maxWidth: '520px',
          borderRight: '1px solid #1f1c14', background: C.surfaceLow,
          display: 'flex', flexDirection: 'column', overflow: 'hidden',
        }}>
          {/* Single continuous description — independently scrollable */}
          <div style={{ flex: 1, overflow: 'auto', padding: '1.25rem 1.4rem' }}>
            {formatDescription(problem.description)}
          </div>
        </div>

        {/* RIGHT: Editor + Results */}
        <div className="sql-judge-workbench" style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minWidth: 0 }}>
          {/* Editor */}
          <div style={{ flex: '0 0 45%', display: 'flex', flexDirection: 'column', minHeight: '180px', borderBottom: '1px solid #1f1c14' }}>
            <div style={{
              padding: '0.4rem 1rem', background: '#0d0d0b',
              borderBottom: '1px solid #1f1c14', flexShrink: 0,
            }}>
              <span style={{ color: '#c9a96e', fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>
                SQL Editor
              </span>
            </div>
            <div style={{ flex: 1, minHeight: 0 }}>
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

          {/* Results panel — scrollable */}
          <div style={{
            flex: 1, display: 'flex', flexDirection: 'column', overflow: 'auto',
            background: '#0a0a09', minHeight: 0,
          }}>
            {/* Buttons bar */}
            <div style={{
              padding: '0.6rem 1rem', display: 'flex', gap: '0.75rem', alignItems: 'center',
              flexShrink: 0, borderBottom: '1px solid #1f1c14',
            }}>
              <button onClick={() => execute(false)} disabled={running || !sql.trim()} style={{
                background: 'linear-gradient(135deg, #2a2215 0%, #1f1a10 100%)', color: '#c9a96e',
                border: '1px solid #3a2e1a', padding: '7px 14px', borderRadius: '4px',
                cursor: running ? 'not-allowed' : 'pointer', fontWeight: 500,
                fontFamily: "'JetBrains Mono', monospace", fontSize: '10.5px',
                opacity: running || !sql.trim() ? 0.5 : 1,
              }}>
                {running ? 'Running...' : '▶ Run'}
              </button>
              <button onClick={() => execute(true)} disabled={running || !sql.trim()} style={{
                background: C.submitBtn, color: '#0e0e0e', border: 'none',
                padding: '7px 14px', borderRadius: '4px', cursor: running ? 'not-allowed' : 'pointer',
                fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", fontSize: '10.5px',
                opacity: running || !sql.trim() ? 0.5 : 1,
              }}>
                {running ? 'Submitting...' : 'Submit'}
              </button>
              <button onClick={() => { setShowHistory(!showHistory); if (!history.length) loadHistory(); }} style={{
                background: 'none', border: '1px solid #1f1c14', color: C.muted,
                padding: '5px 10px', borderRadius: '4px', cursor: 'pointer',
                fontFamily: "'JetBrains Mono', monospace", fontSize: '10px',
              }}>
                History
              </button>
              <div style={{ flex: 1 }} />
              {error && <span style={{ color: C.error, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace" }}>{error}</span>}
            </div>

            {/* Result area */}
            {result ? (
              <div style={{ flex: 1, overflow: 'auto', padding: '0.75rem 1rem', minHeight: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ color: '#c9a96e', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>
                    {isTestRun ? 'Preview' : 'Output'} · {result.columns.length} cols × {result.rows.length} rows
                  </span>
                </div>
                {formatResult(result)}
              </div>
            ) : (
              <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: C.border, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace" }}>
                Run or submit your query to see results
              </div>
            )}
          </div>
        </div>
      </div>

      {/* History drawer */}
      {showHistory && history.length > 0 && (
        <div style={{
          background: '#0d0d0d', borderTop: '1px solid #1f1c14', padding: '0.75rem 1rem',
          maxHeight: '160px', overflow: 'auto', flexShrink: 0,
        }}>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            {history.slice(0, 12).map(s => (
              <div key={s.id} style={{
                display: 'flex', alignItems: 'center', gap: '0.5rem',
                padding: '0.3rem 0.6rem', background: C.bg, border: '1px solid #1f1c14', borderRadius: '3px',
              }}>
                <span style={{
                  minWidth: '70px', padding: '1px 5px', borderRadius: '2px',
                  background: s.testRun ? '#1c1912' : (s.status === 'ACCEPTED' ? '#121c14' : '#2a1a1a'),
                  color: s.testRun ? '#9d8e83' : (s.status === 'ACCEPTED' ? C.accent : C.error),
                  fontSize: '9px', fontFamily: "'JetBrains Mono', monospace", textAlign: 'center',
                  textTransform: 'uppercase',
                }}>
                  {s.testRun ? 'Test' : s.status.replace('_', ' ')}
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

        .sql-judge-workspace {
          background: #0a0a0b;
        }
        .sql-judge-description,
        .sql-judge-workbench {
          min-height: 0;
        }
        @media (max-width: 900px) {
          .sql-judge-split {
            overflow: auto !important;
            flex-direction: column !important;
          }
          .sql-judge-description {
            width: 100% !important;
            max-width: none !important;
            min-width: 0 !important;
            flex: 0 0 42% !important;
            border-right: 0 !important;
            border-bottom: 1px solid #1f1c14;
          }
          .sql-judge-workbench {
            flex: 1 0 58% !important;
            min-height: 520px !important;
          }
        }
        @media (max-width: 560px) {
          .sql-judge-description {
            flex-basis: 48% !important;
          }
          .sql-judge-workbench {
            flex-basis: 52% !important;
            min-height: 500px !important;
          }
        }
      `}</style>
    </div>
  );
};

export default SqlJudge;
