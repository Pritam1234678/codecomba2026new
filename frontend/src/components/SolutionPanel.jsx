import { useState, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import api from '../services/api';
import SkeletonLoader from './SkeletonLoader';

const C = { bg: '#131313', surfaceMin: '#0e0e0e', surfaceHi: '#2a2a2a', border: '#50453b', primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1' };
const LANG_MAP = { JAVA: { monaco: 'java' }, CPP: { monaco: 'cpp' }, C: { monaco: 'c' }, PYTHON: { monaco: 'python' }, JAVASCRIPT: { monaco: 'javascript' } };

export default function SolutionPanel({ problemId }) {
    const [solutions, setSolutions] = useState([]);
    const [count, setCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const [selected, setSelected] = useState(null);
    const [activeLang, setActiveLang] = useState('JAVA');

    useEffect(() => { fetchAll(); }, [problemId]);

    const fetchAll = () => {
        setLoading(true);
        Promise.all([api.get(`/practice/solutions/${problemId}`), api.get(`/practice/solutions/${problemId}/count`)])
            .then(([r1, r2]) => { setSolutions(r1.data || []); setCount(r2.data?.count || 0); })
            .catch(() => {}).finally(() => setLoading(false));
    };

    return (
        <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
            {selected ? (
                <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                    <div style={{ padding: '10px 16px', borderBottom: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', gap: '10px', backgroundColor: C.surfaceMin }}>
                        <button onClick={() => setSelected(null)}
                            style={{ background: 'none', border: `1px solid ${C.border}`, color: C.outline, cursor: 'pointer', padding: '4px 12px', borderRadius: '2px', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>arrow_back</span> Back
                        </button>
                        <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', fontWeight: 600, color: C.primary }}>{selected.userName || 'Anonymous'}</span>
                        <div style={{ display: 'flex', gap: '4px' }}>
                            {Object.keys(selected.codes || {}).filter(l => (selected.codes[l] || '').trim()).map(l => (
                                <button key={l} onClick={() => setActiveLang(l)}
                                    style={{ padding: '2px 8px', border: 'none', borderBottom: activeLang === l ? `2px solid ${C.secondary}` : '2px solid transparent', backgroundColor: 'transparent', color: activeLang === l ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.06em', cursor: 'pointer' }}
                                >{l}</button>
                            ))}
                        </div>
                    </div>
                    <div style={{ flex: 1, overflowY: 'auto' }}>
                        {selected.codes && (() => {
                            const code = selected.codes[activeLang] || '';
                            return (
                                <div style={{ height: Math.min(360, Math.max(160, code.split('\n').length * 22)) }}>
                                    <Editor height="100%" language={LANG_MAP[activeLang]?.monaco || 'java'}
                                        value={code} theme="vs-dark"
                                        options={{ readOnly: true, minimap: { enabled: false }, fontSize: 12, lineNumbers: 'on', scrollBeyondLastLine: false, wordWrap: 'on', padding: { top: 8, bottom: 8 }, domReadOnly: true, contextmenu: false }}
                                        loading={<div style={{ height: '100%', backgroundColor: '#0a0a0a' }} />} />
                                </div>
                            );
                        })()}
                        {(selected.explanation || selected.imageUrl) && (
                            <div style={{ padding: '14px 18px', borderTop: `1px solid ${C.border}` }}>
                                {selected.explanation && (
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', marginTop: '3px', flexShrink: 0 }}>Approach</span>
                                        <p style={{ margin: 0, fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.muted, lineHeight: '1.6' }}>{selected.explanation}</p>
                                    </div>
                                )}
                                {selected.imageUrl && <img src={selected.imageUrl} style={{ maxWidth: '100%', maxHeight: '200px', border: `1px solid ${C.border}`, borderRadius: '2px', objectFit: 'contain', marginTop: '10px' }} />}
                            </div>
                        )}
                    </div>
                </div>
            ) : loading ? (
                <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><SkeletonLoader compact rows={3} /></div>
            ) : solutions.length === 0 ? (
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '10px', padding: '2rem' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '32px', color: C.border }}>lightbulb</span>
                    <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.outline }}>No solutions yet</span>
                </div>
            ) : (
                <div>
                    {solutions.map(sol => {
                        const cm = sol.codes || {};
                        const lk = Object.keys(cm).filter(l => cm[l]?.trim());
                        return (
                            <div key={sol.id} onClick={() => { setSelected(sol); setActiveLang(lk[0] || 'JAVA'); }}
                                style={{ padding: '12px 18px', borderBottom: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer', transition: 'background-color 0.15s' }}
                                onMouseEnter={e => e.currentTarget.style.backgroundColor = '#1c1b1b'}
                                onMouseLeave={e => e.currentTarget.style.backgroundColor = 'transparent'}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
                                    <div style={{ width: '24px', height: '24px', borderRadius: '50%', backgroundColor: C.surfaceHi, border: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontFamily: "'Geist', sans-serif", fontSize: '9px', fontWeight: 700, color: C.primary }}>
                                        {(sol.userName || 'A')[0].toUpperCase()}
                                    </div>
                                    <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '12px', color: C.onBg, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{sol.userName || 'Anonymous'}</span>
                                    <div style={{ display: 'flex', gap: '3px', flexWrap: 'wrap' }}>
                                        {lk.map(l => <span key={l} style={{ padding: '1px 5px', borderRadius: '2px', fontFamily: "'JetBrains Mono', monospace", fontSize: '7px', letterSpacing: '0.08em', color: C.secondary, textTransform: 'uppercase', backgroundColor: `${C.secondary}12`, border: `1px solid ${C.secondary}30` }}>{l}</span>)}
                                    </div>
                                </div>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline, flexShrink: 0 }}>
                                    {new Date(sol.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                                </span>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
