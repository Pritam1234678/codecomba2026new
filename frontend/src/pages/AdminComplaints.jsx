import { useState, useEffect } from 'react';
import api from '../services/api';

const C = {
    bg: '#131313', surfaceCon: '#201f1f', surfaceLow: '#1c1b1b', surfaceHi: '#2a2a2a',
    surfaceMin: '#0e0e0e', border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176',
    muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1', error: '#ffb4ab', success: '#4ade80',
};

const TYPE_COLORS = {
    'Network Error': '#ffb4ab', 'Wrong Test Case': '#facc15', 'Compile Timeout Error': '#facc15',
    'Output Not Showing': '#facc15', 'GitHub Not Pushing': '#7dd3fc', 'Contest Submission Issue': '#c4b5fd',
    'Run Times Exceed': '#facc15', 'Submission Times Exceed': '#facc15', 'Others': '#9d8e83',
};

const PAGE_SIZE = 15;

export default function AdminComplaints() {
    const [complaints, setComplaints] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('ALL');
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [resolveModal, setResolveModal] = useState(null);
    const [response, setResponse] = useState('');

    const fetchPage = (p, f) => {
        setLoading(true);
        const params = { page: p, size: PAGE_SIZE };
        if (f && f !== 'ALL') params.status = f;
        api.get('/complaints', { params })
            .then(r => {
                setComplaints(r.data.complaints || []);
                setTotal(r.data.total || 0);
                setTotalPages(r.data.totalPages || 0);
                setPage(p);
            })
            .finally(() => setLoading(false));
    };

    useEffect(() => { fetchPage(0, filter); }, [filter]);

    const resolve = async () => {
        if (!response.trim()) return;
        try {
            await api.put(`/complaints/${resolveModal.id}/resolve`, { response });
            setResolveModal(null); setResponse('');
            fetchPage(page, filter);
        } catch {}
    };

    const Pagination = () => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px' }}>
            <button onClick={() => fetchPage(0, filter)} disabled={page === 0}
                style={{ padding: '4px 10px', border: `1px solid ${C.border}`, color: page === 0 ? C.border : C.outline, background: 'none', cursor: page === 0 ? 'default' : 'pointer' }}>«</button>
            <button onClick={() => fetchPage(page - 1, filter)} disabled={page === 0}
                style={{ padding: '4px 10px', border: `1px solid ${C.border}`, color: page === 0 ? C.border : C.outline, background: 'none', cursor: page === 0 ? 'default' : 'pointer' }}>‹</button>
            <span style={{ color: C.outline, padding: '0 8px' }}>{page + 1} / {totalPages || 1}</span>
            <button onClick={() => fetchPage(page + 1, filter)} disabled={page >= totalPages - 1}
                style={{ padding: '4px 10px', border: `1px solid ${C.border}`, color: page >= totalPages - 1 ? C.border : C.outline, background: 'none', cursor: page >= totalPages - 1 ? 'default' : 'pointer' }}>›</button>
            <button onClick={() => fetchPage(totalPages - 1, filter)} disabled={page >= totalPages - 1}
                style={{ padding: '4px 10px', border: `1px solid ${C.border}`, color: page >= totalPages - 1 ? C.border : C.outline, background: 'none', cursor: page >= totalPages - 1 ? 'default' : 'pointer' }}>»</button>
            <span style={{ color: C.outline, marginLeft: '8px' }}>{total} total</span>
        </div>
    );

    return (
        <div style={{ padding: '24px', backgroundColor: C.bg, minHeight: '100vh', color: C.onBg }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', letterSpacing: '0.1em', textTransform: 'uppercase', color: C.primary, margin: 0 }}>
                    Problem Complaints
                </h2>
                <div style={{ display: 'flex', gap: '8px' }}>
                    {['ALL', 'PENDING', 'RESOLVED'].map(s => (
                        <button key={s} onClick={() => setFilter(s)}
                            style={{
                                padding: '6px 16px', border: filter === s ? `1px solid ${C.primary}` : `1px solid ${C.border}`,
                                color: filter === s ? C.primary : C.outline, background: 'none',
                                fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em',
                                textTransform: 'uppercase', cursor: 'pointer',
                            }}>
                            {s}
                        </button>
                    ))}
                </div>
            </div>

            <Pagination />

            {loading ? (
                <div style={{ padding: '40px', textAlign: 'center' }}><span style={{ color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px' }}>Loading...</span></div>
            ) : (
                <div style={{ overflowX: 'auto', marginTop: '12px' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px' }}>
                        <thead>
                            <tr style={{ borderBottom: `2px solid ${C.border}` }}>
                                <th style={{ padding: '10px 12px', textAlign: 'left', color: C.outline, fontWeight: 400, textTransform: 'uppercase', fontSize: '10px' }}>#</th>
                                <th style={{ padding: '10px 12px', textAlign: 'left', color: C.outline, fontWeight: 400, textTransform: 'uppercase', fontSize: '10px' }}>User</th>
                                <th style={{ padding: '10px 12px', textAlign: 'left', color: C.outline, fontWeight: 400, textTransform: 'uppercase', fontSize: '10px' }}>Problem</th>
                                <th style={{ padding: '10px 12px', textAlign: 'left', color: C.outline, fontWeight: 400, textTransform: 'uppercase', fontSize: '10px' }}>Type</th>
                                <th style={{ padding: '10px 12px', textAlign: 'left', color: C.outline, fontWeight: 400, textTransform: 'uppercase', fontSize: '10px' }}>Message</th>
                                <th style={{ padding: '10px 12px', textAlign: 'left', color: C.outline, fontWeight: 400, textTransform: 'uppercase', fontSize: '10px' }}>Status</th>
                                <th style={{ padding: '10px 12px', textAlign: 'left', color: C.outline, fontWeight: 400, textTransform: 'uppercase', fontSize: '10px' }}>Date</th>
                                <th style={{ padding: '10px 12px', textAlign: 'center', color: C.outline, fontWeight: 400, textTransform: 'uppercase', fontSize: '10px' }}>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            {complaints.map((c, i) => (
                                <tr key={c.id} style={{ borderBottom: `1px solid ${C.border}15`, backgroundColor: i % 2 === 0 ? 'transparent' : `${C.surfaceMin}40` }}>
                                    <td style={{ padding: '10px 12px', color: C.outline }}>{c.id}</td>
                                    <td style={{ padding: '10px 12px' }}>
                                        <span style={{ color: C.muted }}>{c.username || '—'}</span>
                                        <br/><span style={{ color: C.outline, fontSize: '9px' }}>{c.email || ''}</span>
                                    </td>
                                    <td style={{ padding: '10px 12px', color: C.primary }}>#{c.problemId}{c.contestId && <span style={{ color: C.outline, fontSize: '9px' }}> (contest #{c.contestId})</span>}</td>
                                    <td style={{ padding: '10px 12px' }}>
                                        <span style={{ color: TYPE_COLORS[c.complaintType] || C.outline, fontSize: '10px' }}>{c.complaintType}</span>
                                    </td>
                                    <td style={{ padding: '10px 12px', color: C.onBg, maxWidth: '250px', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{c.message}</td>
                                    <td style={{ padding: '10px 12px' }}>
                                        <span style={{
                                            padding: '2px 8px', fontSize: '10px',
                                            color: c.status === 'RESOLVED' ? C.success : '#facc15',
                                            border: `1px solid ${c.status === 'RESOLVED' ? C.success : '#facc15'}40`,
                                        }}>{c.status}</span>
                                        {c.adminResponse && <div style={{ color: C.outline, fontSize: '9px', marginTop: '4px', maxWidth: '150px', whiteSpace: 'pre-wrap' }} title={c.adminResponse}>{c.adminResponse.substring(0, 60)}{c.adminResponse.length > 60 ? '...' : ''}</div>}
                                    </td>
                                    <td style={{ padding: '10px 12px', color: C.outline, fontSize: '10px' }}>{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '—'}</td>
                                    <td style={{ padding: '10px 12px', textAlign: 'center' }}>
                                        {c.status === 'PENDING' && (
                                            <button onClick={() => setResolveModal(c)}
                                                style={{ padding: '4px 12px', border: `1px solid ${C.success}`, color: C.success, background: 'none', cursor: 'pointer', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px' }}>
                                                Resolve
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            {complaints.length === 0 && (
                                <tr><td colSpan={8} style={{ padding: '32px', textAlign: 'center', color: C.outline }}>No complaints found</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            )}

            <div style={{ marginTop: '12px' }}><Pagination /></div>

            {/* Resolve Modal */}
            {resolveModal && (
                <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.7)' }}
                    onClick={e => { if (e.target === e.currentTarget) setResolveModal(null); }}>
                    <div style={{ width: '480px', backgroundColor: C.surfaceLow, border: `1px solid ${C.border}` }} onClick={e => e.stopPropagation()}>
                        <div style={{ padding: '16px 20px', borderBottom: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.primary, textTransform: 'uppercase' }}>Resolve #{resolveModal.id}</span>
                            <button onClick={() => setResolveModal(null)} style={{ background: 'none', border: 'none', color: C.outline, cursor: 'pointer' }}>✕</button>
                        </div>
                        <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                <b style={{ color: C.muted }}>User:</b> {resolveModal.username}<br/>
                                <b style={{ color: C.muted }}>Type:</b> {resolveModal.complaintType}<br/>
                                <b style={{ color: C.muted }}>Problem:</b> #{resolveModal.problemId}<br/>
                                <b style={{ color: C.muted }}>Message:</b> {resolveModal.message}
                            </div>
                            <textarea value={response} onChange={e => setResponse(e.target.value)}
                                rows={4} placeholder="Admin response..."
                                style={{ width: '100%', padding: '10px 12px', backgroundColor: C.surfaceMin, border: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', outline: 'none', resize: 'vertical', boxSizing: 'border-box' }} />
                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                                <button onClick={() => setResolveModal(null)} style={{ padding: '8px 20px', border: `1px solid ${C.border}`, color: C.outline, background: 'none', cursor: 'pointer', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px' }}>Cancel</button>
                                <button onClick={resolve} style={{ padding: '8px 20px', border: `1px solid ${C.success}`, color: C.success, background: 'none', cursor: 'pointer', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px' }}>Resolve</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
