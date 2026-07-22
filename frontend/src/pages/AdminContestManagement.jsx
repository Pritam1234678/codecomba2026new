import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import AdminService from '../services/admin.service';
import useResponsive from '../hooks/useResponsive';

import SkeletonLoader from '../components/SkeletonLoader';
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

const getStatus = (contest) => {
    const now   = Date.now();
    const start = new Date(contest.startTime).getTime();
    const end   = new Date(contest.endTime).getTime();
    if (!contest.active) return 'DRAFT';
    if (now < start)     return 'UPCOMING';
    if (now > end)       return 'FINISHED';
    return 'ACTIVE';
};

const statusConfig = {
    ACTIVE:   { color: C.secondary,  bg: 'rgba(96,68,3,0.3)',   border: C.secondary },
    UPCOMING: { color: C.primary,    bg: 'rgba(80,69,59,0.3)',  border: C.primary },
    FINISHED: { color: C.outline,    bg: 'transparent',         border: C.border },
    DRAFT:    { color: C.outline,    bg: 'rgba(42,42,42,0.5)',  border: C.border },
};

const AdminContestManagement = () => {
    const { isMobile } = useResponsive();
    const [contests,     setContests]     = useState([]);
    const [loading,      setLoading]      = useState(true);
    const [error,        setError]        = useState('');
    const [search,       setSearch]       = useState('');
    const [filter,       setFilter]       = useState('ALL');
    const [deleteModal,  setDeleteModal]  = useState({ show: false, contestId: null, contestName: '' });
    const [toast,        setToast]        = useState(null);

    useEffect(() => { loadContests(); }, []);

    const loadContests = () => {
        setLoading(true);
        AdminService.getAllContestsAdmin()
            .then(res => setContests(res.data))
            .catch(() => setError('Failed to load contests'))
            .finally(() => setLoading(false));
    };

    const showToast = (msg, type = 'success') => {
        setToast({ msg, type });
        setTimeout(() => setToast(null), 3000);
    };

    const handleToggle = (contestId, active) => {
        const action = active ? AdminService.deactivateContest(contestId) : AdminService.activateContest(contestId);
        action
            .then(() => { loadContests(); showToast(active ? 'Contest deactivated.' : 'Contest activated.'); })
            .catch(() => showToast('Failed to update contest.', 'error'));
    };

    const handleProctoredToggle = (contestId, isProctored) => {
        const action = isProctored
            ? AdminService.disableProctored(contestId)
            : AdminService.enableProctored(contestId, 1);
        action
            .then(() => { loadContests(); showToast(isProctored ? 'Proctored mode disabled.' : 'Proctored mode enabled.'); })
            .catch((e) => showToast(e?.response?.data?.message || 'Failed to toggle proctored mode.', 'error'));
    };

    const confirmDelete = () => {
        AdminService.deleteContest(deleteModal.contestId)
            .then(() => { loadContests(); showToast('Contest deleted.'); setDeleteModal({ show: false }); })
            .catch(() => showToast('Failed to delete contest.', 'error'));
    };

    const filtered = contests.filter(c => {
        const matchSearch = c.name?.toLowerCase().includes(search.toLowerCase());
        const status = getStatus(c);
        const matchFilter = filter === 'ALL' || filter === status;
        return matchSearch && matchFilter;
    });

    const activeCount   = contests.filter(c => getStatus(c) === 'ACTIVE').length;
    const upcomingCount = contests.filter(c => getStatus(c) === 'UPCOMING').length;
    const finishedCount = contests.filter(c => getStatus(c) === 'FINISHED').length;

    if (loading) return <SkeletonLoader />;

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
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '3rem', paddingBottom: '2rem', borderBottom: `1px solid ${C.border}` }}
            >
                <div>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: C.onBg, marginBottom: '8px' }}>
                        Contest Management
                    </h1>
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                        Create, manage, and monitor all contest operations.
                    </p>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    {/* Search */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', width: '240px' }}>
                        <span className="material-symbols-outlined" style={{ color: C.outline, fontSize: '18px' }}>search</span>
                        <input
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            placeholder="SEARCH CONTESTS..."
                            style={{ background: 'transparent', border: 'none', outline: 'none', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.08em', color: C.onBg, width: '100%' }}
                        />
                    </div>
                    {/* Create button */}
                    <Link
                        to="/admin/contests/create"
                        style={{
                            display: 'flex', alignItems: 'center', gap: '8px',
                            padding: '12px 24px',
                            border: `1px solid ${C.secondary}`,
                            color: C.secondary,
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase',
                            textDecoration: 'none', transition: 'all 0.2s',
                        }}
                        onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.secondary; }}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>add</span>
                        Create Contest
                    </Link>
                </div>
            </motion.header>

            {/* ── Stats Bento ── */}
            <motion.div
                initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.05 }}
                style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1px', backgroundColor: C.border, marginBottom: '2rem' }}
            >
                {[
                    { label: 'Active Contests',  value: activeCount,   accent: true },
                    { label: 'Upcoming',          value: upcomingCount, accent: false },
                    { label: 'Finished',          value: finishedCount, accent: false },
                ].map(({ label, value, accent }) => (
                    <div key={label} style={{ backgroundColor: C.bg, padding: '2rem', borderTop: accent ? `2px solid ${C.secondary}` : 'none' }}>
                        <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', marginBottom: '1rem' }}>{label}</p>
                        <p style={{ fontFamily: "'Playfair Display', serif", fontSize: '48px', fontWeight: 300, lineHeight: 1, color: C.onBg }}>{value}</p>
                    </div>
                ))}
            </motion.div>

            {/* ── Filter tabs ── */}
            <div style={{ display: 'flex', gap: '0', marginBottom: '1rem', border: `1px solid ${C.border}`, width: 'fit-content' }}>
                {['ALL', 'ACTIVE', 'UPCOMING', 'FINISHED', 'DRAFT'].map(f => (
                    <button
                        key={f}
                        onClick={() => setFilter(f)}
                        style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '10px', letterSpacing: '0.12em', padding: '6px 16px',
                            background: filter === f ? C.secondary : 'transparent',
                            color: filter === f ? C.bg : C.outline,
                            border: 'none', cursor: 'pointer',
                            borderRight: f !== 'DRAFT' ? `1px solid ${C.border}` : 'none',
                            transition: 'all 0.2s',
                        }}
                    >
                        {f}
                    </button>
                ))}
            </div>

            {/* ── Contest Table ── */}
            <motion.div
                initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, overflow: 'hidden' }}
            >
                {/* Table header */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 200px 120px 380px', gap: '16px', padding: '14px 24px', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceHi }}>
                    {['Contest Details', 'Timeline', 'Status', 'Actions'].map((h, i) => (
                        <span key={h} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', textAlign: i >= 2 ? 'center' : 'left' }}>
                            {h}
                        </span>
                    ))}
                </div>

                {filtered.length === 0 ? (
                    <div style={{ padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                        No contests found.{' '}
                        <Link to="/admin/contests/create" style={{ color: C.primary, textDecoration: 'underline' }}>Create one →</Link>
                    </div>
                ) : (
                    filtered.map((contest, i) => (
                        <ContestRow
                            key={contest.id}
                            contest={contest}
                            index={i}
                            isLast={i === filtered.length - 1}
                            onToggle={() => handleToggle(contest.id, contest.active)}
                            onProctoredToggle={() => handleProctoredToggle(contest.id, contest.proctored)}
                            onDelete={() => setDeleteModal({ show: true, contestId: contest.id, contestName: contest.name })}
                        />
                    ))
                )}

                {/* Pagination info */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 24px', borderTop: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                        Showing {filtered.length} of {contests.length} contests
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
                                Purge Protocol
                            </h3>
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.muted, lineHeight: 1.6, marginBottom: '2rem' }}>
                                You are about to permanently delete{' '}
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", color: C.onBg }}>{deleteModal.contestName}</span>.
                                This will also delete all associated problems and test cases.
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
                @keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
            `}</style>
        </div>
    );
};

/* ── Contest Row ── */
const ContestRow = ({ contest, index, isLast, onToggle, onProctoredToggle, onDelete }) => {
    const [hovered, setHovered] = useState(false);
    const status = getStatus(contest);
    const sc     = statusConfig[status];

    const now      = Date.now();
    const start    = new Date(contest.startTime).getTime();
    const end      = new Date(contest.endTime).getTime();
    const progress = status === 'ACTIVE'
        ? Math.min(100, Math.round(((now - start) / (end - start)) * 100))
        : status === 'FINISHED' ? 100 : 0;

    const accentColor = status === 'ACTIVE' ? C.secondary : status === 'UPCOMING' ? C.primary : C.border;

    return (
        <motion.div
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.04 }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'grid', gridTemplateColumns: '1fr 200px 120px 380px',
                gap: '16px', padding: '20px 24px',
                borderBottom: isLast ? 'none' : `1px solid ${C.border}`,
                borderLeft: `2px solid ${accentColor}`,
                backgroundColor: hovered ? C.surfaceCon : 'transparent',
                transition: 'background-color 0.2s',
                opacity: status === 'FINISHED' ? 0.65 : 1,
            }}
        >
            {/* Contest Details */}
            <div>
                <h3 style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.onBg, marginBottom: '4px', textDecoration: status === 'FINISHED' ? 'line-through' : 'none', textDecorationColor: C.border }}>
                    {contest.name}
                </h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, margin: 0 }}>
                        ID: CC-{String(contest.id).padStart(4, '0')}
                    </p>
                    {contest.proctored && (
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.12em', color: C.secondary, border: `1px solid ${C.secondary}`, padding: '1px 8px', textTransform: 'uppercase' }}>
                            Proctored
                        </span>
                    )}
                </div>
            </div>

            {/* Timeline */}
            <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: '6px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>
                    <span>{new Date(contest.startTime).toLocaleDateString()}</span>
                    <span>{new Date(contest.endTime).toLocaleDateString()}</span>
                </div>
                <div style={{ height: '3px', backgroundColor: C.surfaceHi, borderRadius: '2px', overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${progress}%`, backgroundColor: status === 'ACTIVE' ? C.secondary : status === 'FINISHED' ? C.border : 'transparent', transition: 'width 0.5s' }} />
                </div>
            </div>

            {/* Status */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: sc.color, border: `1px solid ${sc.border}`, backgroundColor: sc.bg, padding: '3px 10px', textTransform: 'uppercase' }}>
                    {status}
                </span>
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
                <Link
                    to={`/admin/contests/${contest.id}/problems`}
                    style={{ display: 'flex', alignItems: 'center', gap: '4px', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', textDecoration: 'none', transition: 'color 0.2s', padding: '4px 0' }}
                    onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                    onMouseLeave={e => e.currentTarget.style.color = C.outline}
                >
                    <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>list_alt</span>
                    Problems
                </Link>
                <span style={{ color: C.border }}>|</span>
                <Link
                    to={`/admin/contests/${contest.id}/edit`}
                    style={{ display: 'flex', alignItems: 'center', gap: '4px', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', textDecoration: 'none', transition: 'color 0.2s', padding: '4px 0' }}
                    onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                    onMouseLeave={e => e.currentTarget.style.color = C.outline}
                >
                    <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>edit</span>
                    Edit
                </Link>
                <span style={{ color: C.border }}>|</span>
                <button
                    onClick={onToggle}
                    style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', cursor: 'pointer', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', transition: 'color 0.2s', padding: '4px 0' }}
                    onMouseEnter={e => e.currentTarget.style.color = C.primary}
                    onMouseLeave={e => e.currentTarget.style.color = C.outline}
                >
                    <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>{contest.active ? 'pause_circle' : 'play_circle'}</span>
                    {contest.active ? 'Deactivate' : 'Activate'}
                </button>
                <span style={{ color: C.border }}>|</span>
                <button
                    onClick={onProctoredToggle}
                    title={contest.proctored ? 'Disable proctoring for this contest' : 'Enable proctoring for this contest'}
                    style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', cursor: 'pointer', color: contest.proctored ? C.secondary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', transition: 'color 0.2s', padding: '4px 0' }}
                    onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                    onMouseLeave={e => e.currentTarget.style.color = contest.proctored ? C.secondary : C.outline}
                >
                    <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>{contest.proctored ? 'shield' : 'shield_lock'}</span>
                    {contest.proctored ? 'Unproctor' : 'Proctor'}
                </button>
                <span style={{ color: C.border }}>|</span>
                <button
                    onClick={onDelete}
                    style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', cursor: 'pointer', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', transition: 'color 0.2s', padding: '4px 0' }}
                    onMouseEnter={e => e.currentTarget.style.color = C.error}
                    onMouseLeave={e => e.currentTarget.style.color = C.outline}
                >
                    <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>delete</span>
                    Delete
                </button>
            </div>
        </motion.div>
    );
};

export default AdminContestManagement;
