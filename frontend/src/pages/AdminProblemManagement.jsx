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

    useEffect(() => { loadProblems(); }, []);

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

    const easyCount = problems.filter(p => p.level === 'EASY').length;
    const mediumCount = problems.filter(p => p.level === 'MEDIUM').length;
    const hardCount = problems.filter(p => p.level === 'HARD').length;

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', letterSpacing: '0.1em' }}>
            Loading...
        </div>
    );

    if (error) return (
        <div style={{ padding: isMobile ? '24px 16px' : '48px 64px', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.error }}>
            {error}
        </div>
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
            <div style={{ display: 'flex', gap: '0', marginBottom: '1rem', border: `1px solid ${C.border}`, width: 'fit-content' }}>
                {['ALL', 'EASY', 'MEDIUM', 'HARD'].map(f => (
                    <button
                        key={f}
                        onClick={() => setLevelFilter(f)}
                        style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '10px', letterSpacing: '0.12em', padding: '6px 16px',
                            background: levelFilter === f ? C.secondary : 'transparent',
                            color: levelFilter === f ? C.bg : C.outline,
                            border: 'none', cursor: 'pointer',
                            borderRight: f !== 'HARD' ? `1px solid ${C.border}` : 'none',
                            transition: 'all 0.2s',
                        }}
                    >
                        {f}
                    </button>
                ))}
            </div>

            {/* ── Problem Table ── */}
            <motion.div
                initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, overflow: 'hidden' }}
            >
                {/* Table header */}
                {!isMobile && (
                    <div style={{ display: 'grid', gridTemplateColumns: '60px 1fr 100px 100px 80px 280px', gap: '16px', padding: '14px 24px', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceHi }}>
                        {['ID', 'Title', 'Level', 'Contest', 'Active', 'Actions'].map((h, i) => (
                            <span key={h} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', textAlign: i >= 4 ? 'center' : 'left' }}>
                                {h}
                            </span>
                        ))}
                    </div>
                )}

                {filtered.length === 0 ? (
                    <div style={{ padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                        No problems found.
                    </div>
                ) : (
                    filtered.map((problem, i) => (
                        <ProblemRow
                            key={problem.id}
                            problem={problem}
                            index={i}
                            isLast={i === filtered.length - 1}
                            isMobile={isMobile}
                            onToggle={() => handleToggleActive(problem.id)}
                            onDelete={() => setDeleteModal({ show: true, problemId: problem.id, problemTitle: problem.title })}
                            navigate={navigate}
                        />
                    ))
                )}

                {/* Footer */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 24px', borderTop: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                        Showing {filtered.length} of {problems.length} problems
                    </span>
                </div>
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

/* ── Problem Row ── */
const ProblemRow = ({ problem, index, isLast, isMobile, onToggle, onDelete, navigate }) => {
    const [hovered, setHovered] = useState(false);
    const lc = levelConfig[problem.level] || levelConfig.MEDIUM;

    if (isMobile) {
        return (
            <motion.div
                initial={{ opacity: 0, x: -8 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.03 }}
                style={{ padding: '16px 20px', borderBottom: isLast ? 'none' : `1px solid ${C.border}` }}
            >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.onBg }}>{problem.title}</span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: lc.color, border: `1px solid ${lc.border}`, backgroundColor: lc.bg, padding: '2px 8px' }}>
                        {problem.level}
                    </span>
                </div>
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>ID: {problem.id}</span>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>Contest: {problem.contestId || '—'}</span>
                    <ToggleSwitch active={problem.active} onToggle={onToggle} />
                </div>
                <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
                    <ActionBtn label="Edit" icon="edit" color={C.outline} hoverColor={C.secondary} onClick={() => navigate(`/admin/problems/${problem.id}/edit`)} />
                    <ActionBtn label="Tests" icon="science" color={C.outline} hoverColor={C.primary} onClick={() => navigate(`/admin/problems/${problem.id}/testcases`)} />
                    <ActionBtn label="Delete" icon="delete" color={C.outline} hoverColor={C.error} onClick={onDelete} />
                </div>
            </motion.div>
        );
    }

    return (
        <motion.div
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.03 }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'grid', gridTemplateColumns: '60px 1fr 100px 100px 80px 280px',
                gap: '16px', padding: '16px 24px',
                borderBottom: isLast ? 'none' : `1px solid ${C.border}`,
                backgroundColor: hovered ? C.surfaceCon : 'transparent',
                transition: 'background-color 0.2s',
                alignItems: 'center',
            }}
        >
            {/* ID */}
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline }}>
                {problem.id}
            </span>

            {/* Title */}
            <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.onBg, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {problem.title}
            </span>

            {/* Level */}
            <div>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: lc.color, border: `1px solid ${lc.border}`, backgroundColor: lc.bg, padding: '3px 10px', textTransform: 'uppercase' }}>
                    {problem.level}
                </span>
            </div>

            {/* Contest */}
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                {problem.contestId || '—'}
            </span>

            {/* Active Toggle */}
            <div style={{ display: 'flex', justifyContent: 'center' }}>
                <ToggleSwitch active={problem.active} onToggle={onToggle} />
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
                <ActionBtn label="Edit" icon="edit" color={C.outline} hoverColor={C.secondary} onClick={() => navigate(`/admin/problems/${problem.id}/edit`)} />
                <span style={{ color: C.border }}>|</span>
                <ActionBtn label="Tests" icon="science" color={C.outline} hoverColor={C.primary} onClick={() => navigate(`/admin/problems/${problem.id}/testcases`)} />
                <span style={{ color: C.border }}>|</span>
                <ActionBtn label="Delete" icon="delete" color={C.outline} hoverColor={C.error} onClick={onDelete} />
            </div>
        </motion.div>
    );
};

/* ── Toggle Switch ── */
const ToggleSwitch = ({ active, onToggle }) => (
    <button
        onClick={onToggle}
        style={{
            width: '36px', height: '20px',
            borderRadius: '10px',
            border: `1px solid ${active ? C.success : C.border}`,
            backgroundColor: active ? 'rgba(102,187,106,0.2)' : 'rgba(42,42,42,0.5)',
            cursor: 'pointer',
            position: 'relative',
            transition: 'all 0.2s',
            padding: 0,
        }}
        title={active ? 'Active — click to deactivate' : 'Inactive — click to activate'}
    >
        <div style={{
            width: '14px', height: '14px',
            borderRadius: '50%',
            backgroundColor: active ? C.success : C.outline,
            position: 'absolute',
            top: '2px',
            left: active ? '18px' : '2px',
            transition: 'all 0.2s',
        }} />
    </button>
);

/* ── Action Button ── */
const ActionBtn = ({ label, icon, color, hoverColor, onClick }) => (
    <button
        onClick={onClick}
        style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', cursor: 'pointer', color, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', transition: 'color 0.2s', padding: '4px 0' }}
        onMouseEnter={e => e.currentTarget.style.color = hoverColor}
        onMouseLeave={e => e.currentTarget.style.color = color}
    >
        <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>{icon}</span>
        {label}
    </button>
);

export default AdminProblemManagement;
