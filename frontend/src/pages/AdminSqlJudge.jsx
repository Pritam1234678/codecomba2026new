import { useState, useEffect } from 'react';
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
  warn: '#ffcc00',
  surfaceLow: '#1c1b1b',
  surfaceHi: '#2a2a2a',
};

const EMPTY_FORM = {
  title: '',
  description: '',
  schemaName: '',
  setupSql: '',
  officialSolutionSql: '',
  comparisonMode: 'UNORDERED',
  timeLimitMs: 2000,
  maxResultRows: 500,
};

const AdminSqlJudge = () => {
  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [status, setStatus] = useState(null);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [creating, setCreating] = useState(false);
  const [provisioning, setProvisioning] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const loadAll = () => {
    setLoading(true);
    sqlJudgeApi.adminListProblems()
      .then(res => {
        setProblems(Array.isArray(res) ? res : res?.content || []);
      })
      .catch(() => setError('Failed to load SQL problems'))
      .finally(() => setLoading(false));
  };

  const loadStatus = () => {
    sqlJudgeApi.adminStatus()
      .then(setStatus)
      .catch(() => {});
  };

  useEffect(() => { loadAll(); loadStatus(); }, []);

  const createProblem = async () => {
    if (!form.title.trim() || !form.setupSql.trim() || !form.officialSolutionSql.trim()) {
      setError('Title, setup SQL and official solution SQL are required');
      return;
    }
    setCreating(true);
    setError(null);
    setMessage(null);
    try {
      const created = await sqlJudgeApi.adminCreateProblem({
        ...form,
        comparisonMode: form.comparisonMode,
        timeLimitMs: Number(form.timeLimitMs) || 2000,
        maxResultRows: Number(form.maxResultRows) || 500,
      });
      setMessage(`Problem #${created.id} created — provisioning on all 6 Neon nodes...`);
      setShowCreate(false);
      setForm(EMPTY_FORM);
      loadAll();
      setSelected(created);
      setProvisioning(true);
      try {
        await sqlJudgeApi.adminProvision(created.id);
        setMessage(`Problem #${created.id} fully provisioned on all 6 Neon nodes and enabled.`);
      } catch (e) {
        setError(`Provisioning failed: ${e.response?.data?.message || 'see node status'}`);
      } finally {
        setProvisioning(false);
        loadStatus();
      }
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to create problem');
    } finally {
      setCreating(false);
    }
  };

  const reprovision = async (p) => {
    setProvisioning(true);
    setError(null);
    setMessage(null);
    try {
      await sqlJudgeApi.adminProvision(p.id);
      setMessage(`Problem #${p.id} reprovisioned.`);
      setSelected(p);
      loadAll();
      loadStatus();
    } catch (e) {
      setError(`Provisioning failed: ${e.response?.data?.message || 'unknown error'}`);
    } finally {
      setProvisioning(false);
    }
  };

  const toggleEnabled = async (p) => {
    try {
      await sqlJudgeApi.adminSetEnabled(p.id, !p.enabled);
      loadAll();
      if (selected?.id === p.id) setSelected({ ...selected, enabled: !p.enabled });
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to toggle');
    }
  };

  const inputStyle = {
    background: C.bg, border: `1px solid ${C.border}`, borderRadius: '4px',
    color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
    padding: '8px 10px', outline: 'none', width: '100%', boxSizing: 'border-box',
  };

  const btnStyle = (color) => ({
    background: color, color: '#0e0e0e', border: 'none',
    padding: '8px 16px', borderRadius: '4px', cursor: 'pointer',
    fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
    textTransform: 'uppercase', letterSpacing: '0.05em',
    opacity: creating || provisioning ? 0.5 : 1,
  });

  return (
    <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', gap: '1rem', flexWrap: 'wrap' }}>
        <h1 style={{ color: C.primary, fontFamily: "'Playfair Display', serif", fontSize: '2rem', margin: 0 }}>
          SQL Judge Admin
        </h1>
        <button onClick={() => { setShowCreate(!showCreate); setError(null); }} style={btnStyle(C.secondary)}>
          {showCreate ? 'Cancel' : '+ New Problem'}
        </button>
      </div>

      {message && (
        <div style={{ background: `${C.accent}1a`, border: `1px solid ${C.accent}44`, color: C.accent, padding: '10px 14px', borderRadius: '4px', marginBottom: '1rem', fontSize: '13px' }}>
          {message}
        </div>
      )}
      {error && (
        <div style={{ background: `${C.error}1a`, border: `1px solid ${C.error}44`, color: C.error, padding: '10px 14px', borderRadius: '4px', marginBottom: '1rem', fontSize: '13px' }}>
          {error}
        </div>
      )}

      {/* Node health strip */}
      {status && (
        <div style={{ background: C.panel, border: `1px solid ${C.border}`, borderRadius: '8px', padding: '1rem', marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
            <span style={{ color: C.primary, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
              Cluster Health
            </span>
            <span style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace" }}>
              queue {status.queueDepth} · active {status.activeJobs} / {status.maxInflightQueries} · workers {status.workers}
            </span>
          </div>
          <div style={{ display: 'grid', gap: '0.5rem', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))' }}>
            {Object.entries(status.nodes || {}).map(([id, n]) => {
              const ok = n.healthy;
              return (
                <div key={id} style={{
                  background: C.bg, border: `1px solid ${ok ? C.accent + '55' : C.error + '55'}`,
                  borderRadius: '6px', padding: '10px 12px',
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px' }}>{id}</span>
                    <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: ok ? C.accent : C.error, flexShrink: 0 }} />
                  </div>
                  <div style={{ marginTop: '6px', color: C.muted, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <span>active: {n.activeQueries ?? 0}</span>
                    <span>latency: {n.recentLatencyMs ?? 0}ms</span>
                    <span>failures: {n.consecutiveFailures ?? 0}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Create form */}
      {showCreate && (
        <div style={{ background: C.panel, border: `1px solid ${C.border}`, borderRadius: '8px', padding: '1.5rem', marginBottom: '1.5rem', display: 'grid', gap: '1rem' }}>
          <h3 style={{ margin: 0, color: C.primary, fontFamily: "'Playfair Display', serif", fontSize: '1.25rem' }}>New SQL Problem</h3>
          <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: '1fr 1fr' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
              <label style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase' }}>Title *</label>
              <input style={inputStyle} value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} />
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
              <label style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase' }}>Schema Name</label>
              <input style={inputStyle} value={form.schemaName} onChange={e => setForm({ ...form, schemaName: e.target.value })} placeholder="optional — defaults to q_&lt;id&gt;" />
            </div>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            <label style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase' }}>Description *</label>
            <textarea style={{ ...inputStyle, minHeight: '80px', resize: 'vertical' }} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            <label style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase' }}>Setup SQL * (CREATE TABLE + INSERT — runs under superuser on each Neon node)</label>
            <textarea style={{ ...inputStyle, minHeight: '140px', resize: 'vertical', fontFamily: "'JetBrains Mono', monospace" }} value={form.setupSql} onChange={e => setForm({ ...form, setupSql: e.target.value })} spellCheck={false} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            <label style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase' }}>Official Solution SQL * (used once to compute expected result)</label>
            <textarea style={{ ...inputStyle, minHeight: '100px', resize: 'vertical', fontFamily: "'JetBrains Mono', monospace" }} value={form.officialSolutionSql} onChange={e => setForm({ ...form, officialSolutionSql: e.target.value })} spellCheck={false} />
          </div>
          <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
              <label style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase' }}>Comparison</label>
              <select style={inputStyle} value={form.comparisonMode} onChange={e => setForm({ ...form, comparisonMode: e.target.value })}>
                <option value="UNORDERED">UNORDERED</option>
                <option value="ORDERED">ORDERED</option>
              </select>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
              <label style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase' }}>Time Limit (ms)</label>
              <input type="number" style={inputStyle} value={form.timeLimitMs} onChange={e => setForm({ ...form, timeLimitMs: e.target.value })} />
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
              <label style={{ color: C.muted, fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase' }}>Max Rows</label>
              <input type="number" style={inputStyle} value={form.maxResultRows} onChange={e => setForm({ ...form, maxResultRows: e.target.value })} />
            </div>
          </div>
          <div>
            <button onClick={createProblem} disabled={creating} style={btnStyle(C.secondary)}>
              {creating ? 'Creating + Provisioning...' : 'Create & Provision on 6 Neon Nodes'}
            </button>
          </div>
        </div>
      )}

      {/* Problems table */}
      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}>
          <div style={{ width: '40px', height: '40px', borderRadius: '50%', border: `2px solid ${C.border}`, borderTopColor: C.primary, animation: 'spin 1s linear infinite' }} />
        </div>
      ) : (
        <div style={{ background: C.panel, border: `1px solid ${C.border}`, borderRadius: '8px', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
            <thead>
              <tr style={{ background: C.surfaceHi }}>
                {['ID', 'Title', 'Status', 'Mode', 'Time', 'Provisioned', 'Enabled', 'Actions'].map(h => (
                  <th key={h} style={{ padding: '10px 12px', textAlign: 'left', color: C.primary, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', borderBottom: `1px solid ${C.border}` }}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {problems.map(p => {
                const provStatus = typeof p.provisioningStatus === 'object'
                  ? (p.provisioningStatus?.allProvisioned ? 'PROVISIONED' : 'PARTIAL')
                  : (p.provisioningStatus || 'UNKNOWN');
                const allOk = provStatus === 'PROVISIONED';
                return (
                  <tr key={p.id} style={{ borderBottom: `1px solid ${C.border}` }} onClick={() => setSelected(selected?.id === p.id ? null : p)}>
                    <td style={{ padding: '10px 12px', color: C.muted, fontFamily: "'JetBrains Mono', monospace" }}>{p.id}</td>
                    <td style={{ padding: '10px 12px', color: C.onBg }}>{p.title}</td>
                    <td style={{ padding: '10px 12px' }}>
                      <span style={{
                        padding: '2px 8px', borderRadius: '3px', fontSize: '10px', fontFamily: "'JetBrains Mono', monospace",
                        background: allOk ? `${C.accent}22` : `${C.warn}22`, color: allOk ? C.accent : C.warn, textTransform: 'uppercase',
                      }}>
                        {provStatus}
                      </span>
                    </td>
                    <td style={{ padding: '10px 12px', color: C.muted, fontFamily: "'JetBrains Mono', monospace" }}>{p.comparisonMode}</td>
                    <td style={{ padding: '10px 12px', color: C.muted, fontFamily: "'JetBrains Mono', monospace" }}>{p.timeLimitMs}ms</td>
                    <td style={{ padding: '10px 12px', color: C.muted, fontFamily: "'JetBrains Mono', monospace" }}>
                      {typeof p.provisioningStatus === 'object' ? `${Object.values(p.provisioningStatus.nodeStatuses || {}).filter(n => n.provisioned).length}/6` : '—'}
                    </td>
                    <td style={{ padding: '10px 12px' }}>
                      <span style={{
                        padding: '2px 8px', borderRadius: '3px', fontSize: '10px', fontFamily: "'JetBrains Mono', monospace",
                        background: p.enabled ? `${C.accent}22` : `${C.error}22`, color: p.enabled ? C.accent : C.error, textTransform: 'uppercase',
                      }}>
                        {p.enabled ? 'ON' : 'OFF'}
                      </span>
                    </td>
                    <td style={{ padding: '10px 12px' }}>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button
                          onClick={e => { e.stopPropagation(); reprovision(p); }}
                          disabled={provisioning}
                          style={{ background: 'none', border: `1px solid ${C.border}`, color: C.secondary, padding: '4px 8px', borderRadius: '3px', cursor: 'pointer', fontSize: '10px', fontFamily: "'JetBrains Mono', monospace" }}
                        >
                          Provision
                        </button>
                        <button
                          onClick={e => { e.stopPropagation(); toggleEnabled(p); }}
                          style={{ background: 'none', border: `1px solid ${C.border}`, color: p.enabled ? C.error : C.accent, padding: '4px 8px', borderRadius: '3px', cursor: 'pointer', fontSize: '10px', fontFamily: "'JetBrains Mono', monospace" }}
                        >
                          {p.enabled ? 'Disable' : 'Enable'}
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Selected problem detail */}
      {selected && (
        <div style={{ background: C.panel, border: `1px solid ${C.border}`, borderRadius: '8px', padding: '1.5rem', marginTop: '1.5rem' }}>
          <h3 style={{ margin: '0 0 0.5rem', color: C.primary, fontFamily: "'Playfair Display', serif", fontSize: '1.25rem' }}>
            #{selected.id} — {selected.title}
          </h3>
          <p style={{ color: C.muted, fontSize: '13px', whiteSpace: 'pre-wrap', lineHeight: 1.6, margin: '0 0 1rem' }}>{selected.description}</p>

          {selected.schemaName && (
            <div style={{ marginBottom: '0.75rem' }}>
              <span style={{ color: C.muted, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase' }}>Schema: </span>
              <span style={{ color: C.onBg, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace" }}>{selected.schemaName}</span>
            </div>
          )}

          <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: '1fr 1fr', marginBottom: '1rem' }}>
            <div>
              <div style={{ color: C.muted, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase', marginBottom: '0.4rem' }}>Setup SQL</div>
              <pre style={{ background: C.bg, border: `1px solid ${C.border}`, borderRadius: '4px', padding: '0.75rem', fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", color: C.onMuted, overflow: 'auto', maxHeight: '200px', margin: 0, whiteSpace: 'pre-wrap' }}>{selected.setupSql}</pre>
            </div>
            <div>
              <div style={{ color: C.muted, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", textTransform: 'uppercase', marginBottom: '0.4rem' }}>Official Solution SQL</div>
              <pre style={{ background: C.bg, border: `1px solid ${C.border}`, borderRadius: '4px', padding: '0.75rem', fontSize: '11px', fontFamily: "'JetBrains Mono', monospace", color: C.onMuted, overflow: 'auto', maxHeight: '200px', margin: 0, whiteSpace: 'pre-wrap' }}>{selected.officialSolutionSql}</pre>
            </div>
          </div>

          {typeof selected.provisioningStatus === 'object' && selected.provisioningStatus.nodeStatuses && (
            <div style={{ display: 'grid', gap: '0.5rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
              {Object.entries(selected.provisioningStatus.nodeStatuses).map(([id, ns]) => (
                <div key={id} style={{ background: C.bg, border: `1px solid ${ns.provisioned ? C.accent + '55' : C.error + '55'}`, borderRadius: '6px', padding: '8px 12px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px' }}>{id}</span>
                    <span style={{ fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", color: ns.provisioned ? C.accent : C.error }}>
                      {ns.provisioned ? 'OK' : 'FAIL'}
                    </span>
                  </div>
                  {ns.error && <div style={{ color: C.error, fontSize: '10px', marginTop: '4px', fontFamily: "'JetBrains Mono', monospace" }}>{ns.error.slice(0, 120)}</div>}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
};

export default AdminSqlJudge;
