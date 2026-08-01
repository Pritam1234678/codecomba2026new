import { useState } from 'react';
import api from '../services/api';

const TYPES = [
    'Network Error',
    'Wrong Test Case',
    'Compile Timeout Error',
    'Output Not Showing',
    'GitHub Not Pushing',
    'Contest Submission Issue',
    'Run Times Exceed',
    'Submission Times Exceed',
    'Others',
];

const C = {
    bg: '#131313', surfaceCon: '#201f1f', surfaceLow: '#1c1b1b', surfaceHi: '#2a2a2a',
    surfaceMin: '#0e0e0e', border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176',
    muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1', error: '#ffb4ab', success: '#4ade80',
};

export default function ComplaintModal({ problemId, contestId, onClose }) {
    const [type, setType] = useState('');
    const [msg, setMsg] = useState('');
    const [sending, setSending] = useState(false);
    const [done, setDone] = useState(false);
    const [err, setErr] = useState('');

    const submit = async () => {
        if (!type) { setErr('Please select a complaint type'); return; }
        if (!msg.trim()) { setErr('Please describe the issue'); return; }
        setSending(true); setErr('');
        try {
            await api.post('/complaints', { problemId, contestId, complaintType: type, message: msg });
            setDone(true);
        } catch(e) {
            setErr(e.response?.data?.error || 'Failed to submit');
        } finally { setSending(false); }
    };

    return (
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.7)' }}
            onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
            <div style={{ width: '480px', maxHeight: '90vh', backgroundColor: C.surfaceLow, border: `1px solid ${C.border}` }} onClick={e => e.stopPropagation()}>
                <div style={{ padding: '16px 20px', borderBottom: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.08em', color: C.primary, textTransform: 'uppercase' }}>
                        Report a Problem
                    </span>
                    <button onClick={onClose} style={{ background: 'none', border: 'none', color: C.outline, cursor: 'pointer', fontSize: '18px' }}>✕</button>
                </div>
                {done ? (
                    <div style={{ padding: '32px 20px', textAlign: 'center' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '32px', color: C.success }}>check_circle</span>
                        <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.success, marginTop: '12px' }}>Complaint submitted. We will look into it.</p>
                        <button onClick={onClose} style={{ marginTop: '16px', padding: '8px 24px', border: `1px solid ${C.border}`, color: C.muted, background: 'none', cursor: 'pointer', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px' }}>Close</button>
                    </div>
                ) : (
                    <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
                        <div>
                            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: '6px', display: 'block' }}>Issue Type</label>
                            <select value={type} onChange={e => setType(e.target.value)}
                                style={{ width: '100%', padding: '10px 12px', backgroundColor: C.surfaceMin, border: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', outline: 'none' }}>
                                <option value="" style={{ backgroundColor: C.surfaceLow }}>-- Select --</option>
                                {TYPES.map(t => <option key={t} value={t} style={{ backgroundColor: C.surfaceLow }}>{t}</option>)}
                            </select>
                        </div>
                        <div>
                            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: '6px', display: 'block' }}>Description</label>
                            <textarea value={msg} onChange={e => setMsg(e.target.value)}
                                rows={5} placeholder="Describe the problem in detail..."
                                style={{ width: '100%', padding: '10px 12px', backgroundColor: C.surfaceMin, border: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', outline: 'none', resize: 'vertical', boxSizing: 'border-box' }} />
                        </div>
                        {err && <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.error }}>{err}</span>}
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                            <button onClick={onClose} style={{ padding: '8px 20px', border: `1px solid ${C.border}`, color: C.outline, background: 'none', cursor: 'pointer', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px' }}>Cancel</button>
                            <button onClick={submit} disabled={sending}
                                style={{ padding: '8px 20px', border: `1px solid ${C.primary}`, color: C.primary, background: 'none', cursor: sending ? 'not-allowed' : 'pointer', opacity: sending ? 0.5 : 1, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px' }}>
                                {sending ? 'Sending...' : 'Submit'}
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
