import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import api from '../services/api';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg:         '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceHi:  '#2a2a2a',
    surfaceMin: '#0e0e0e',
    border:     '#50453b',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
    success:    '#66bb6a',
};

const levelConfig = {
    EASY:   { color: '#66bb6a', bg: 'rgba(102,187,106,0.15)', border: '#66bb6a' },
    MEDIUM: { color: '#e9c176', bg: 'rgba(233,193,118,0.15)', border: '#e9c176' },
    HARD:   { color: '#ffb4ab', bg: 'rgba(255,180,171,0.15)', border: '#ffb4ab' },
};

const AdminProblemManagement = () => {
    const { isMobile } = useResponsive();
    const navigate = useNavigate();
    const [problems, setProblems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [search, setSearch] = useState('');
    const [levelFilter, setLevelFilter] = useState('ALL');
    const [deleteModal, setDeleteModal] = useState({ show: false, problemId: null, problemTitle: '' });
    const [toast, setToast] = useState(null);
    const [page, setPage] = useState(0);
    const PAGE_SIZE = 9;

    useEffect(() => { loadProblems(); }, []);
    useEffect(() => { setPage(0); }, [search, levelFilter]);

    const loadProblems = () => {
        setLoading(true);
        api.get('/admin/problems')
            .then(res => setProblems(res.data))
            .catch(() => setError('Failed to load problems'))
            .finally(() => setLoading(false));
    };

    const showToast = (msg, type = 'success') => {
        setToast({ msg, type });
        setTimeout(() => setToast(null), 3000);
    };

    const handleToggleActive = (id) => {
        api.patch(`/admin/problems/${id}/toggle-active`)
            .then(() => { loadProblems(); showToast('Problem status toggled.'); })
            .catch(() => showToast('Failed to toggle status.', 'error'));
    };

    const confirmDelete = () => {
        api.delete(`/admin/problems/${deleteModal.problemId}`)
            .then(() => { loadProblems(); showToast('Problem deleted.'); setDeleteModal({ show: false }); })
            .catch(() => showToast('Failed to delete problem.', 'error'));
    };

    const filtered = problems.filter(p => {
        const matchSearch = p.title?.toLowerCase().includes(search.toLowerCase());
        const matchLevel = levelFilter === 'ALL' || p.level === levelFilter;
        return matchSearch && matchLevel;
    });

    const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
    const paginated = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
    const easyCount = problems.filter(p => p.level === 'EASY').length;
    const mediumCount = problems.filter(p => p.level === 'MEDIUM').length;
    const hardCount = problems.filter(p => p.level === 'HARD').length;

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', letterSpacing: '0.1em' }}>Loading...</div>
    );
    if (error) return (
        <div style={{ padding: isMobile ? '24px 16px' : '48px 64px', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.error }}>{error}</div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", padding: isMobile ? '24px 16px' : '48px 64px' }}>

            {/* ── Header ── */}
            <motion.header
                initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '3rem', paddingBottom: '2rem', borderBottom: `1px solid ${C.border}`, gap: '16px' }}
            >
                <div>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: isMobile ? '28px' : '36px', fontWeight: 600, color: C.onBg, marginBottom: '8px' }}>
                        Problem Management
                    </h1>
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                        View, edit, and manage all problems across contests.
                    </p>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap', width: isMobile ? '100%' : 'auto' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', width: isMobile ? '100%' : '260px' }}>
                        <span className="material-symbols-outlined" style={{ color: C.outline, fontSize: '18px' }}>search</span>
                        <input
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            placeholder="SEARCH PROBLEMS..."
                            style={{ background: 'transparent', border: 'none', outline: 'none', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.08em', color: C.onBg, width: '100%' }}
                        />
                    </div>
                    <button
                        onClick={() => navigate('/admin/problems/new')}
                        style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '12px 24px', border: `1px solid ${C.secondary}`, color: C.secondary, backgroundColor: C.surfaceCon, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s', width: isMobile ? '100%' : 'auto', justifyContent: 'center' }}
                        onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = C.surfaceCon; e.currentTarget.style.color = C.secondary; }}
                    >
                        + Create New Problem
                    </button>
                </div>
            </motion.header>

            {/* ── Stats Bento ── */}
            <motion.div
                initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.05 }}
                style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(4, 1fr)', gap: '1px', backgroundColor: C.border, marginBottom: '2rem' }}
            >
                {[
                    { label: 'Total Problems', value: problems.length, accent: true },
                    { label: 'Easy', value: easyCount, accent: false },
                    { label: 'Medium', value: mediumCount, accent: false },
                    { label: 'Hard', value: hardCount, accent: false },
                ].map(({ label, value, accent }) => (
                    <div key={label} style={{ backgroundColor: C.bg, padding: '2rem', borderTop: accent ? `2px solid ${C.secondary}` : 'none' }}>
                        <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginBottom: '1rem' }}>{label}</p>
                        <p style={{ fontFamily: "'Playfair Display', serif", fontSize: '48px', fontWeight: 300, lineHeight: 1, color: C.onBg }}>{value}</p>
                    </div>
                ))}
            </motion.div>

            {/* ── Filter tabs ── */}
            <div style={{ display: 'flex', gap: '0', marginBottom: '2rem', border: `1px solid ${C.border}`, width: 'fit-content' }}>
                {['ALL', 'EASY', 'MEDIUM', 'HARD'].map(f => (
                    <button key={f} onClick={() => setLevelFilter(f)}
                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', padding: '8px 20px', background: levelFilter === f ? C.secondary : 'transparent', color: levelFilter === f ? C.bg : C.outline, border: 'none', cursor: 'pointer', borderRight: f !== 'HARD' ? `1px solid ${C.border}` : 'none', textTransform: 'uppercase', transition: 'all 0.2s' }}
                    >{f}</button>
                ))}
            </div>

            {/* ── Problem Cards Grid ── */}
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}>
                {paginated.length === 0 ? (
                    <div style={{ border: `1px solid ${C.border}`, padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                        No problems found.
                    </div>
                ) : (
                    <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(auto-fill, minmax(320px, 1fr))', gap: '20px' }}>
                        {paginated.map((problem, i) => (
                            <AdminProblemCard key={problem.id} problem={problem} index={i}
                                onToggle={() => handleToggleActive(problem.id)}
                                onDelete={() => setDeleteModal({ show: true, problemId: problem.id, problemTitle: problem.title })}
                                navigate={navigate}
                            />
                        ))}
                    </div>
                )}

                {/* Pagination */}
                {totalPages > 1 && (
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '2rem', padding: '1rem 0', borderTop: `1px solid ${C.border}` }}>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, letterSpacing: '0.08em' }}>
                            Showing {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, filtered.length)} of {filtered.length}
                        </span>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
                                style={{ background: 'none', border: `1px solid ${page === 0 ? C.border : C.outline}`, color: page === 0 ? C.border : C.outline, cursor: page === 0 ? 'not-allowed' : 'pointer', width: '36px', height: '36px', display: 'flex', alignItems: 'center', justifyContent: 'center', transition: 'all 0.2s' }}
                                onMouseEnter={e => { if (page > 0) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                                onMouseLeave={e => { e.currentTarget.style.borderColor = page === 0 ? C.border : C.outline; e.currentTarget.style.color = page === 0 ? C.border : C.outline; }}
                            ><span className="material-symbols-outlined" style={{ fontSize: '18px' }}>chevron_left</span></button>
                            {Array.from({ length: totalPages }, (_, i) => (
                                <button key={i} onClick={() => setPage(i)}
                                    style={{ background: page === i ? C.secondary : 'none', border: `1px solid ${page === i ? C.secondary : C.border}`, color: page === i ? C.bg : C.outline, cursor: 'pointer', width: '36px', height: '36px', fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', transition: 'all 0.2s' }}
                                    onMouseEnter={e => { if (page !== i) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                                    onMouseLeave={e => { if (page !== i) { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.outline; } }}
                                >{i + 1}</button>
                            ))}
                            <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}
                                style={{ background: 'none', border: `1px solid ${page >= totalPages - 1 ? C.border : C.outline}`, color: page >= totalPages - 1 ? C.border : C.outline, cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer', width: '36px', height: '36px', display: 'flex', alignItems: 'center', justifyContent: 'center', transition: 'all 0.2s' }}
                                onMouseEnter={e => { if (page < totalPages - 1) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                                onMouseLeave={e => { e.currentTarget.style.borderColor = page >= totalPages - 1 ? C.border : C.outline; e.currentTarget.style.color = page >= totalPages - 1 ? C.border : C.outline; }}
                            ><span className="material-symbols-outlined" style={{ fontSize: '18px' }}>chevron_right</span></button>
                        </div>
                    </div>
                )}
            </motion.div>

            {/* ── Delete Modal ── */}
            <AnimatePresence>
                {deleteModal.show && (
                    <motion.div
                        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        style={{ position: 'fixed', inset: 0, zIndex: 50, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem', backgroundColor: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(8px)' }}
                    >
                        <motion.div
                            initial={{ opacity: 0, scale: 0.95, y: 16 }}
                            animate={{ opacity: 1, scale: 1, y: 0 }}
                            exit={{ opacity: 0, scale: 0.95 }}
                            style={{ backgroundColor: C.surfaceCon, border: `1px solid ${C.error}`, maxWidth: '440px', width: '100%', padding: '2.5rem', position: 'relative' }}
                        >
                            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.error }} />
                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.error, marginBottom: '1rem' }}>
                                Delete Problem
                            </h3>
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.muted, lineHeight: 1.6, marginBottom: '2rem' }}>
                                You are about to permanently delete{' '}
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", color: C.onBg }}>{deleteModal.problemTitle}</span>.
                                This will also delete all associated test cases and submissions.
                            </p>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '16px' }}>
                                <button
                                    onClick={() => setDeleteModal({ show: false })}
                                    style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', padding: '10px 20px', transition: 'color 0.2s' }}
                                    onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                                    onMouseLeave={e => e.currentTarget.style.color = C.outline}
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={confirmDelete}
                                    style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', border: `1px solid ${C.error}`, color: C.error, backgroundColor: 'transparent', padding: '10px 24px', cursor: 'pointer', transition: 'all 0.2s' }}
                                    onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.error; e.currentTarget.style.color = C.bg; }}
                                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.error; }}
                                >
                                    Execute Delete
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* ── Toast ── */}
            <AnimatePresence>
                {toast && (
                    <motion.div
                        initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 40 }}
                        style={{ position: 'fixed', bottom: '2rem', right: '2rem', backgroundColor: C.surfaceLow, borderLeft: `2px solid ${toast.type === 'success' ? C.secondary : C.error}`, padding: '1rem 1.5rem', zIndex: 100, boxShadow: '0 8px 32px rgba(0,0,0,0.5)', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: toast.type === 'success' ? C.secondary : C.error, letterSpacing: '0.05em' }}
                    >
                        {toast.msg}
                    </motion.div>
                )}
            </AnimatePresence>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
            `}</style>
        </div>
    );
};

/* ── Admin Problem Card (Practice-style with admin actions) ── */
const AdminProblemCard = ({ problem, index, onToggle, onDelete, navigate }) => {
    const [hovered, setHovered] = useState(false);
    const lc = levelConfig[problem.level] || levelConfig.MEDIUM;

    return (
        <motion.div
            initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * 0.04, duration: 0.4 }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{ border: `1px solid ${hovered ? lc.color : C.border}`, backgroundColor: hovered ? C.surfaceCon : C.surfaceLow, padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '12px', transition: 'all 0.2s', position: 'relative', overflow: 'hidden' }}
        >
            {/* Top accent bar */}
            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: lc.color, opacity: hovered ? 1 : 0.3, transition: 'opacity 0.2s' }} />

            {/* Difficulty + active toggle */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ padding: '3px 10px', border: `1px solid ${lc.color}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: lc.color, textTransform: 'uppercase' }}>
                    {problem.level || 'MEDIUM'}
                </span>
                <button onClick={e => { e.stopPropagation(); onToggle(); }}
                    title={problem.active ? 'Active — click to deactivate' : 'Inactive — click to activate'}
                    style={{ width: '36px', height: '20px', borderRadius: '10px', border: `1px solid ${problem.active ? C.success : C.border}`, backgroundColor: problem.active ? 'rgba(102,187,106,0.2)' : 'rgba(42,42,42,0.5)', cursor: 'pointer', position: 'relative', transition: 'all 0.2s', padding: 0, flexShrink: 0 }}
                >
                    <div style={{ width: '14px', height: '14px', borderRadius: '50%', backgroundColor: problem.active ? C.success : C.outline, position: 'absolute', top: '2px', left: problem.active ? '18px' : '2px', transition: 'all 0.2s' }} />
                </button>
            </div>

            {/* Title */}
            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '18px', fontWeight: 600, color: hovered ? C.primary : C.onBg, margin: 0, lineHeight: 1.3, transition: 'color 0.2s' }}>
                {problem.title}
            </h3>

            {/* Description preview */}
            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.outline, margin: 0, lineHeight: 1.5, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                {problem.description || 'No description available.'}
            </p>

            {/* Meta */}
            <div style={{ display: 'flex', gap: '12px', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>
                <span>⏱ {problem.timeLimit}s</span>
                <span>·</span>
                <span>💾 {problem.memoryLimit}MB</span>
                <span style={{ marginLeft: 'auto', color: C.outline }}>
                    {problem.contestId ? `Contest #${problem.contestId}` : 'Standalone'}
                </span>
            </div>

            {/* Action buttons */}
            <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                <button onClick={() => navigate(`/admin/problems/${problem.id}/edit`)}
                    style={{ flex: 1, padding: '9px 12px', border: `1px solid ${C.secondary}`, backgroundColor: 'transparent', color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '5px' }}
                    onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.secondary; }}
                >
                    <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>edit</span>Edit
                </button>
                <button onClick={onDelete}
                    style={{ padding: '9px 12px', border: `1px solid ${C.border}`, backgroundColor: 'transparent', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '5px' }}
                    onMouseEnter={e => { e.currentTarget.style.borderColor = C.error; e.currentTarget.style.color = C.error; }}
                    onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.outline; }}
                >
                    <span className="material-symbols-outlined" style={{ fontSize: '13px' }}>delete</span>
                </button>
            </div>
        </motion.div>
    );
};

export default AdminProblemManagement;
