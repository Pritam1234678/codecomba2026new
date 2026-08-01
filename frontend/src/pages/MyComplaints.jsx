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

const PAGE_SIZE = 10;

export default function MyComplaints() {
    const [complaints, setComplaints] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [expanded, setExpanded] = useState(null);

    const fetch = (p) => {
        setLoading(true);
        api.get('/complaints/mine', { params: { page: p, size: PAGE_SIZE } })
            .then(r => {
                setComplaints(r.data.complaints || []);
                setTotal(r.data.total || 0);
                setTotalPages(r.data.totalPages || 0);
                setPage(p);
            })
            .finally(() => setLoading(false));
    };

    useEffect(() => { fetch(0); }, []);

    return (
        <div style={{ padding: '24px', backgroundColor: C.bg, minHeight: '100vh', color: C.onBg }}>
            <h2 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', letterSpacing: '0.1em', textTransform: 'uppercase', color: C.primary, margin: '0 0 20px 0' }}>
                My Complaints
            </h2>

            {loading ? (
                <div style={{ padding: '40px', textAlign: 'center', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px' }}>Loading...</div>
            ) : complaints.length === 0 ? (
                <div style={{ padding: '60px', textAlign: 'center', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
                    No complaints submitted yet.
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {complaints.map(c => (
                        <div key={c.id} style={{
                            backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`,
                            display: 'flex', flexDirection: 'column',
                        }}>
                            <div
                                onClick={() => setExpanded(expanded === c.id ? null : c.id)}
                                style={{
                                    padding: '14px 18px', display: 'flex', alignItems: 'center', gap: '12px',
                                    cursor: 'pointer', transition: 'background-color 0.15s',
                                }}
                                onMouseEnter={e => e.currentTarget.style.backgroundColor = C.surfaceMin}
                                onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}
                            >
                                <span style={{
                                    padding: '2px 8px', fontSize: '10px', fontFamily: "'JetBrains Mono', monospace",
                                    color: c.status === 'RESOLVED' ? C.success : '#facc15',
                                    border: `1px solid ${c.status === 'RESOLVED' ? C.success : '#facc15'}40`,
                                    flexShrink: 0,
                                }}>{c.status}</span>
                                <span style={{
                                    fontSize: '10px', fontFamily: "'JetBrains Mono', monospace",
                                    color: TYPE_COLORS[c.complaintType] || C.outline, flexShrink: 0,
                                }}>{c.complaintType}</span>
                                <span style={{ color: C.primary, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', flexShrink: 0 }}>#{c.problemId}</span>
                                <span style={{ color: C.muted, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                    {c.message}
                                </span>
                                <span style={{ color: C.outline, fontSize: '10px', fontFamily: "'JetBrains Mono', monospace", flexShrink: 0 }}>
                                    {c.createdAt ? new Date(c.createdAt).toLocaleDateString() : ''}
                                </span>
                                <span className="material-symbols-outlined" style={{ fontSize: '18px', color: C.outline, flexShrink: 0 }}>
                                    {expanded === c.id ? 'expand_less' : 'expand_more'}
                                </span>
                            </div>
                            {expanded === c.id && (
                                <div style={{ padding: '16px 18px', borderTop: `1px solid ${C.border}`, display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                    <div style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.onBg, whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>
                                        {c.message}
                                    </div>
                                    {c.adminResponse && (
                                        <div style={{
                                            backgroundColor: C.surfaceMin, padding: '14px', borderLeft: `3px solid ${C.secondary}`,
                                            display: 'flex', flexDirection: 'column', gap: '6px',
                                        }}>
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, textTransform: 'uppercase', letterSpacing: '0.1em' }}>
                                                Admin Response
                                            </span>
                                            <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.secondary, whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>
                                                {c.adminResponse}
                                            </span>
                                        </div>
                                    )}
                                    {c.status === 'PENDING' && !c.adminResponse && (
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, fontStyle: 'italic' }}>
                                            Waiting for admin review...
                                        </span>
                                    )}
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}

            {totalPages > 1 && (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', marginTop: '20px', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px' }}>
                    <button onClick={() => fetch(0)} disabled={page === 0}
                        style={{ padding: '6px 12px', border: `1px solid ${C.border}`, color: page === 0 ? C.border : C.outline, background: 'none', cursor: page === 0 ? 'default' : 'pointer' }}>«</button>
                    <button onClick={() => fetch(page - 1)} disabled={page === 0}
                        style={{ padding: '6px 12px', border: `1px solid ${C.border}`, color: page === 0 ? C.border : C.outline, background: 'none', cursor: page === 0 ? 'default' : 'pointer' }}>‹</button>
                    <span style={{ color: C.outline, padding: '0 12px' }}>{page + 1} / {totalPages}</span>
                    <button onClick={() => fetch(page + 1)} disabled={page >= totalPages - 1}
                        style={{ padding: '6px 12px', border: `1px solid ${C.border}`, color: page >= totalPages - 1 ? C.border : C.outline, background: 'none', cursor: page >= totalPages - 1 ? 'default' : 'pointer' }}>›</button>
                    <button onClick={() => fetch(totalPages - 1)} disabled={page >= totalPages - 1}
                        style={{ padding: '6px 12px', border: `1px solid ${C.border}`, color: page >= totalPages - 1 ? C.border : C.outline, background: 'none', cursor: page >= totalPages - 1 ? 'default' : 'pointer' }}>»</button>
                </div>
            )}
        </div>
    );
}
