import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import AdminService from '../services/admin.service';
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

const AdminUserManagement = () => {
    const { isMobile } = useResponsive();
    const [users,        setUsers]        = useState([]);
    const [loading,      setLoading]      = useState(true);
    const [error,        setError]        = useState('');
    const [search,       setSearch]       = useState('');
    const [filter,       setFilter]       = useState('ALL'); // ALL | ACTIVE | DISABLED
    const [deleteModal,  setDeleteModal]  = useState({ show: false, userId: null, username: '', email: '' });
    const [confirmInput, setConfirmInput] = useState('');
    const [toast,        setToast]        = useState(null);
    const [page,         setPage]         = useState(1);

    useEffect(() => { loadUsers(); }, []);

    const loadUsers = () => {
        setLoading(true);
        AdminService.getAllUsers()
            .then(res => {
                const regular = res.data.filter(u => !u.roles?.some(r => r.name === 'ROLE_ADMIN'));
                setUsers(regular);
            })
            .catch(() => setError('Failed to load users'))
            .finally(() => setLoading(false));
    };

    const showToast = (msg, type = 'success') => {
        setToast({ msg, type });
        setTimeout(() => setToast(null), 3000);
    };

    const handleToggle = (userId, enabled) => {
        const action = enabled ? AdminService.disableUser(userId) : AdminService.enableUser(userId);
        action
            .then(() => { loadUsers(); showToast(enabled ? 'Operative disabled.' : 'Operative enabled.'); })
            .catch(() => showToast('Failed to update status.', 'error'));
    };

    const openDelete = (user) => {
        setDeleteModal({ show: true, userId: user.id, username: user.username, email: user.email });
        setConfirmInput('');
    };

    const confirmDelete = () => {
        AdminService.deleteUser(deleteModal.userId)
            .then(() => {
                showToast('Operative purged from system.');
                loadUsers();
                setDeleteModal({ show: false, userId: null, username: '', email: '' });
            })
            .catch(err => showToast(err.response?.data?.message || 'Purge failed.', 'error'));
    };

    const filtered = users.filter(u => {
        const matchSearch = u.username?.toLowerCase().includes(search.toLowerCase()) ||
                            u.email?.toLowerCase().includes(search.toLowerCase());
        const matchFilter = filter === 'ALL' || (filter === 'ACTIVE' && u.enabled) || (filter === 'DISABLED' && !u.enabled);
        return matchSearch && matchFilter;
    });

    useEffect(() => { setPage(1); }, [search, filter]);

    const PAGE_SIZE_USERS = 20;
    const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE_USERS));
    const pageUsers = filtered.slice((page - 1) * PAGE_SIZE_USERS, page * PAGE_SIZE_USERS);

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

    const activeCount   = users.filter(u => u.enabled).length;
    const disabledCount = users.filter(u => !u.enabled).length;

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", padding: isMobile ? '24px 16px' : '48px 64px' }}>

            {/* ── Header ── */}
            <motion.header
                initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '3rem', paddingBottom: '2rem', borderBottom: `1px solid ${C.border}` }}
            >
                <div>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: C.onBg, marginBottom: '8px' }}>
                        User Index
                    </h1>
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline, maxWidth: '560px', lineHeight: 1.5 }}>
                        Manage platform access, monitor activity statuses, and perform structural modifications to user accounts within the architectural grid.
                    </p>
                    {/* Stats */}
                    <div style={{ display: 'flex', gap: '0', marginTop: '1.5rem', borderLeft: `1px solid ${C.border}` }}>
                        {[
                            { label: 'Total', value: users.length, color: C.muted },
                            { label: 'Active', value: activeCount, color: C.success },
                            { label: 'Disabled', value: disabledCount, color: C.outline },
                        ].map(({ label, value, color }) => (
                            <div key={label} style={{ padding: '0 20px', borderRight: `1px solid ${C.border}` }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '2px' }}>{label}</span>
                                <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '22px', fontWeight: 300, color }}>{value}</span>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Search + Filter */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', alignItems: 'flex-end' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', width: '280px' }}>
                        <span className="material-symbols-outlined" style={{ color: C.outline, fontSize: '18px' }}>search</span>
                        <input
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            placeholder="SEARCH OPERATIVES..."
                            style={{ background: 'transparent', border: 'none', outline: 'none', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.08em', color: C.onBg, width: '100%' }}
                        />
                        <span style={{ color: C.primary, animation: 'blink 1s infinite', fontFamily: "'JetBrains Mono', monospace" }}>█</span>
                    </div>
                    {/* Filter tabs */}
                    <div style={{ display: 'flex', gap: '0', border: `1px solid ${C.border}` }}>
                        {['ALL', 'ACTIVE', 'DISABLED'].map(f => (
                            <button
                                key={f}
                                onClick={() => setFilter(f)}
                                style={{
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '10px', letterSpacing: '0.12em',
                                    padding: '6px 16px',
                                    background: filter === f ? C.secondary : 'transparent',
                                    color: filter === f ? C.bg : C.outline,
                                    border: 'none', cursor: 'pointer',
                                    borderRight: f !== 'DISABLED' ? `1px solid ${C.border}` : 'none',
                                    transition: 'all 0.2s',
                                }}
                            >
                                {f}
                            </button>
                        ))}
                    </div>
                </div>
            </motion.header>

            {/* ── Table ── */}
            <motion.div
                initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceCon, overflow: 'hidden' }}
            >
                {/* Table header */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 200px 100px 160px', gap: '16px', padding: '14px 24px', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceHi }}>
                    {['Operative', 'Identifier', 'Status', 'Actions'].map((h, i) => (
                        <span key={h} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', textAlign: i >= 2 ? 'center' : 'left' }}>
                            {h}
                        </span>
                    ))}
                </div>

                {filtered.length === 0 ? (
                    <div style={{ padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                        No operatives found
                    </div>
                ) : (
                    pageUsers.map((user, i) => (
                        <UserRow
                            key={user.id}
                            user={user}
                            index={i}
                            onToggle={() => handleToggle(user.id, user.enabled)}
                            onDelete={() => openDelete(user)}
                        />
                    ))
                )}

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 24px', borderTop: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, flexWrap: 'wrap', gap: '12px' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                        {filtered.length === 0 ? '0 entries' : `${(page - 1) * PAGE_SIZE_USERS + 1}–${Math.min(page * PAGE_SIZE_USERS, filtered.length)} of ${filtered.length}`}
                    </span>
                    {totalPages > 1 && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <UserPagBtn label="←" onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1} />
                            {Array.from({ length: totalPages }, (_, i) => i + 1)
                                .filter(n => n === 1 || n === totalPages || Math.abs(n - page) <= 2)
                                .reduce((acc, n, idx, arr) => {
                                    if (idx > 0 && n - arr[idx - 1] > 1) acc.push('…');
                                    acc.push(n);
                                    return acc;
                                }, [])
                                .map((item, idx) =>
                                    item === '…'
                                        ? <span key={`e${idx}`} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, padding: '0 4px' }}>…</span>
                                        : <UserPagBtn key={item} label={item} active={item === page} onClick={() => setPage(item)} />
                                )
                            }
                            <UserPagBtn label="→" onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages} />
                        </div>
                    )}
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
                            {/* Red top bar */}
                            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.error }} />

                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.error, marginBottom: '1rem' }}>
                                Purge Protocol
                            </h3>
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.muted, lineHeight: 1.6, marginBottom: '1.5rem' }}>
                                You are about to permanently erase operative{' '}
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", color: C.onBg }}>{deleteModal.username}</span>{' '}
                                from the system architecture. This action transcends standard reversal methods.
                            </p>

                            {/* Confirm input */}
                            <div style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, padding: '1rem', marginBottom: '1.5rem' }}>
                                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, marginBottom: '8px', letterSpacing: '0.08em' }}>
                                    Confirm identification to proceed:
                                </p>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', borderBottom: `1px solid ${C.error}`, paddingBottom: '6px' }}>
                                    <span style={{ color: C.error, fontFamily: "'JetBrains Mono', monospace" }}>&gt;</span>
                                    <input
                                        value={confirmInput}
                                        onChange={e => setConfirmInput(e.target.value)}
                                        placeholder={deleteModal.username}
                                        style={{ background: 'transparent', border: 'none', outline: 'none', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.onBg, width: '100%' }}
                                    />
                                    <span style={{ color: C.error, animation: 'blink 1s infinite', fontFamily: "'JetBrains Mono', monospace" }}>█</span>
                                </div>
                            </div>

                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '16px' }}>
                                <button
                                    onClick={() => setDeleteModal({ show: false, userId: null, username: '', email: '' })}
                                    style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, background: 'none', border: 'none', cursor: 'pointer', padding: '10px 20px', transition: 'color 0.2s' }}
                                    onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                                    onMouseLeave={e => e.currentTarget.style.color = C.outline}
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={confirmDelete}
                                    disabled={confirmInput !== deleteModal.username}
                                    style={{
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase',
                                        border: `1px solid ${C.error}`, color: C.error,
                                        backgroundColor: 'transparent', padding: '10px 24px',
                                        cursor: confirmInput !== deleteModal.username ? 'not-allowed' : 'pointer',
                                        opacity: confirmInput !== deleteModal.username ? 0.4 : 1,
                                        transition: 'all 0.2s',
                                    }}
                                    onMouseEnter={e => { if (confirmInput === deleteModal.username) { e.currentTarget.style.backgroundColor = C.error; e.currentTarget.style.color = C.bg; } }}
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
                        style={{
                            position: 'fixed', bottom: '2rem', right: '2rem',
                            backgroundColor: C.surfaceLow,
                            borderLeft: `2px solid ${toast.type === 'success' ? C.secondary : C.error}`,
                            padding: '1rem 1.5rem',
                            display: 'flex', alignItems: 'center', gap: '12px',
                            zIndex: 100, boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
                            fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
                            color: toast.type === 'success' ? C.secondary : C.error,
                            letterSpacing: '0.05em',
                        }}
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

/* ── User Row ── */
const UserRow = ({ user, index, onToggle, onDelete }) => {
    const [hovered, setHovered] = useState(false);
    const initials = (user.username || 'U').slice(0, 2).toUpperCase();

    return (
        <motion.div
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.03 }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'grid', gridTemplateColumns: '1fr 200px 100px 160px',
                gap: '16px', padding: '18px 24px',
                borderBottom: `1px solid ${C.border}`,
                borderLeft: `2px solid ${user.enabled ? '#66bb6a' : C.border}`,
                backgroundColor: hovered ? '#201f1f' : 'transparent',
                transition: 'background-color 0.2s',
                position: 'relative',
            }}
        >
            {/* Operative */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div style={{
                    width: '32px', height: '32px', borderRadius: '50%',
                    border: `1px solid ${user.enabled ? '#66bb6a' : C.border}`,
                    backgroundColor: '#0e0e0e',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    flexShrink: 0,
                }}>
                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '12px', fontWeight: 700, color: user.enabled ? '#f1bc8b' : C.outline }}>
                        {initials}
                    </span>
                </div>
                <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: user.enabled ? C.onBg : C.outline }}>
                    {user.username}
                </span>
            </div>

            {/* Email */}
            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, display: 'flex', alignItems: 'center', overflow: 'hidden' }}>
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{user.email}</span>
            </div>

            {/* Status */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: user.enabled ? '#66bb6a' : C.border, flexShrink: 0 }} />
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: user.enabled ? '#66bb6a' : C.outline, textTransform: 'uppercase' }}>
                        {user.enabled ? 'Active' : 'Disabled'}
                    </span>
                </span>
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '16px' }}>
                <button
                    onClick={onToggle}
                    style={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '10px', letterSpacing: '0.1em', textTransform: 'uppercase',
                        color: user.enabled ? C.outline : '#f1bc8b',
                        background: 'none', border: 'none', cursor: 'pointer',
                        textDecoration: 'underline', textDecorationColor: 'transparent',
                        transition: 'all 0.2s',
                    }}
                    onMouseEnter={e => { e.currentTarget.style.color = C.secondary; e.currentTarget.style.textDecorationColor = C.secondary; }}
                    onMouseLeave={e => { e.currentTarget.style.color = user.enabled ? C.outline : '#f1bc8b'; e.currentTarget.style.textDecorationColor = 'transparent'; }}
                >
                    {user.enabled ? 'Disable' : 'Enable'}
                </button>
                <button
                    onClick={onDelete}
                    style={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '10px', letterSpacing: '0.1em', textTransform: 'uppercase',
                        color: C.outline, background: 'none', border: 'none', cursor: 'pointer',
                        textDecoration: 'underline', textDecorationColor: 'transparent',
                        transition: 'all 0.2s',
                    }}
                    onMouseEnter={e => { e.currentTarget.style.color = C.error; e.currentTarget.style.textDecorationColor = C.error; }}
                    onMouseLeave={e => { e.currentTarget.style.color = C.outline; e.currentTarget.style.textDecorationColor = 'transparent'; }}
                >
                    Delete
                </button>
            </div>
        </motion.div>
    );
};

const UserPagBtn = ({ label, onClick, disabled, active }) => {
    const [hovered, setHovered] = useState(false);
    return (
        <button
            onClick={onClick}
            disabled={disabled}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                width: '30px', height: '30px',
                border: `1px solid ${active ? C.secondary : hovered && !disabled ? C.primary : C.border}`,
                backgroundColor: active ? C.secondary : hovered && !disabled ? C.surfaceCon : 'transparent',
                color: active ? C.bg : disabled ? C.border : hovered ? C.primary : C.outline,
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '11px',
                cursor: disabled ? 'default' : 'pointer',
                transition: 'all 0.15s',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
        >
            {label}
        </button>
    );
};

export default AdminUserManagement;
