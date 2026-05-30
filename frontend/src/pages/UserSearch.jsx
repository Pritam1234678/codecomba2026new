import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';

const C = {
    bg:         '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceHi:  '#2a2a2a',
    border:     '#50453b',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
};

const PAGE_SIZE = 10;

const Avatar = ({ user, size = 44 }) => {
    const initials = (user.fullName || user.username || '?').charAt(0).toUpperCase();
    return user.photoUrl ? (
        <img
            src={user.photoUrl}
            alt={user.username}
            style={{ width: size, height: size, borderRadius: '50%', objectFit: 'cover', border: `1px solid ${C.border}`, flexShrink: 0 }}
        />
    ) : (
        <div style={{
            width: size, height: size, borderRadius: '50%',
            backgroundColor: C.surfaceHi, border: `1px solid ${C.border}`,
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
        }}>
            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: size * 0.4, fontWeight: 600, color: C.primary }}>
                {initials}
            </span>
        </div>
    );
};

const UserCard = ({ user, index, onClick }) => {
    const [hovered, setHovered] = useState(false);
    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.04, duration: 0.3 }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            onClick={() => onClick(user.username)}
            style={{
                display: 'flex', alignItems: 'center', gap: '16px',
                padding: '16px 20px',
                border: `1px solid ${hovered ? C.secondary : C.border}`,
                backgroundColor: hovered ? C.surfaceCon : C.surfaceLow,
                cursor: 'pointer', transition: 'all 0.2s',
                position: 'relative', overflow: 'hidden',
            }}
        >
            {/* Top accent */}
            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.secondary, opacity: hovered ? 0.6 : 0, transition: 'opacity 0.2s' }} />

            <Avatar user={user} size={44} />

            <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: '10px', flexWrap: 'wrap' }}>
                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '17px', fontWeight: 600, color: hovered ? C.primary : C.onBg, transition: 'color 0.2s', whiteSpace: 'nowrap' }}>
                        {user.fullName || user.username}
                    </span>
                    {user.fullName && (
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, letterSpacing: '0.08em' }}>
                            @{user.username}
                        </span>
                    )}
                </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexShrink: 0 }}>
                <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.secondary, fontVariationSettings: "'FILL' 1" }}>stars</span>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.secondary, fontWeight: 600 }}>
                    {user.totalPoints ?? 0}
                </span>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.08em' }}>pts</span>
            </div>

            <span className="material-symbols-outlined" style={{ fontSize: '18px', color: hovered ? C.secondary : C.outline, transition: 'color 0.2s', flexShrink: 0 }}>
                arrow_forward
            </span>
        </motion.div>
    );
};

export default function UserSearch() {
    const navigate = useNavigate();
    const [query,      setQuery]      = useState('');
    const [results,    setResults]    = useState([]);
    const [total,      setTotal]      = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [page,       setPage]       = useState(0);
    const [loading,    setLoading]    = useState(false);
    const [searched,   setSearched]   = useState(false);
    const debounceRef = useRef(null);
    const inputRef    = useRef(null);

    const doSearch = useCallback(async (q, p) => {
        setLoading(true);
        try {
            const res = await api.get('/user/search', { params: { q, page: p, size: PAGE_SIZE } });
            setResults(res.data.content || []);
            setTotal(res.data.totalElements || 0);
            setTotalPages(res.data.totalPages || 0);
            setPage(p);
            setSearched(true);
        } catch {
            setResults([]);
        } finally {
            setLoading(false);
        }
    }, []);

    // Debounced search on query change
    useEffect(() => {
        clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => {
            doSearch(query, 0);
        }, 300);
        return () => clearTimeout(debounceRef.current);
    }, [query, doSearch]);

    // Initial load — show all users
    useEffect(() => {
        doSearch('', 0);
        inputRef.current?.focus();
    }, [doSearch]);

    const handlePageChange = (p) => doSearch(query, p);

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh', padding: '48px 64px' }}>

            {/* Header */}
            <motion.section
                initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                style={{ marginBottom: '3rem', borderBottom: `1px solid ${C.border}`, paddingBottom: '2rem' }}
            >
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase' }}>
                    Community
                </span>
                <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(32px, 4vw, 48px)', fontWeight: 700, lineHeight: 1.1, color: C.primary, margin: '0.5rem 0 0.75rem' }}>
                    Find Players
                </h1>
                <p style={{ fontSize: '14px', color: C.muted, margin: 0 }}>
                    Search by name or username to view player profiles and stats
                </p>
            </motion.section>

            {/* Search bar */}
            <motion.div
                initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4, delay: 0.1 }}
                style={{ marginBottom: '2rem' }}
            >
                <div style={{
                    display: 'flex', alignItems: 'center', gap: '12px',
                    border: `1px solid ${C.border}`, borderBottom: `2px solid ${C.secondary}`,
                    padding: '14px 20px', backgroundColor: C.surfaceLow,
                    maxWidth: '560px',
                }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '20px', color: C.secondary }}>search</span>
                    <input
                        ref={inputRef}
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                        placeholder="SEARCH BY NAME OR USERNAME..."
                        style={{
                            flex: 1, background: 'transparent', border: 'none', outline: 'none',
                            fontFamily: "'JetBrains Mono', monospace", fontSize: '13px',
                            letterSpacing: '0.08em', color: C.onBg,
                        }}
                    />
                    {query && (
                        <button onClick={() => setQuery('')} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '18px', color: C.outline }}>close</span>
                        </button>
                    )}
                </div>
                {searched && (
                    <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, marginTop: '8px', letterSpacing: '0.08em' }}>
                        {loading ? 'Searching...' : `${total} player${total !== 1 ? 's' : ''} found`}
                        {totalPages > 1 && !loading && ` · page ${page + 1}/${totalPages}`}
                    </p>
                )}
            </motion.div>

            {/* Results */}
            {loading && results.length === 0 ? (
                <div style={{ padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline }}>
                    Searching...
                </div>
            ) : results.length === 0 && searched ? (
                <div style={{ border: `1px solid ${C.border}`, padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline, letterSpacing: '0.08em' }}>
                    No players found{query ? ` for "${query}"` : ''}
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {results.map((user, i) => (
                        <UserCard
                            key={user.id}
                            user={user}
                            index={i}
                            onClick={(username) => navigate(`/players/${username}`)}
                        />
                    ))}
                </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
                <div style={{ marginTop: '2rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
                    <button
                        onClick={() => handlePageChange(Math.max(0, page - 1))}
                        disabled={page === 0}
                        style={{ width: '34px', height: '34px', background: 'transparent', border: `1px solid ${page === 0 ? C.border : C.outline}`, color: page === 0 ? C.border : C.outline, cursor: page === 0 ? 'not-allowed' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                        onMouseEnter={e => { if (page > 0) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                        onMouseLeave={e => { e.currentTarget.style.borderColor = page === 0 ? C.border : C.outline; e.currentTarget.style.color = page === 0 ? C.border : C.outline; }}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>chevron_left</span>
                    </button>

                    {Array.from({ length: totalPages }, (_, i) => {
                        const near = Math.abs(i - page) <= 1;
                        const edge = i === 0 || i === totalPages - 1;
                        if (!near && !edge) {
                            if (i === 1 || i === totalPages - 2) return <span key={i} style={{ color: C.outline, fontSize: '12px', fontFamily: "'JetBrains Mono', monospace" }}>…</span>;
                            return null;
                        }
                        const active = i === page;
                        return (
                            <button key={i} onClick={() => handlePageChange(i)} style={{
                                width: '34px', height: '34px',
                                backgroundColor: active ? C.secondary : 'transparent',
                                border: `1px solid ${active ? C.secondary : C.border}`,
                                color: active ? C.bg : C.outline,
                                cursor: 'pointer', fontSize: '12px',
                                fontFamily: "'JetBrains Mono', monospace",
                                fontWeight: active ? 700 : 400, transition: 'all 0.15s',
                            }}
                                onMouseEnter={e => { if (!active) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                                onMouseLeave={e => { if (!active) { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.outline; } }}
                            >{i + 1}</button>
                        );
                    })}

                    <button
                        onClick={() => handlePageChange(Math.min(totalPages - 1, page + 1))}
                        disabled={page >= totalPages - 1}
                        style={{ width: '34px', height: '34px', background: 'transparent', border: `1px solid ${page >= totalPages - 1 ? C.border : C.outline}`, color: page >= totalPages - 1 ? C.border : C.outline, cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                        onMouseEnter={e => { if (page < totalPages - 1) { e.currentTarget.style.borderColor = C.secondary; e.currentTarget.style.color = C.secondary; } }}
                        onMouseLeave={e => { e.currentTarget.style.borderColor = page >= totalPages - 1 ? C.border : C.outline; e.currentTarget.style.color = page >= totalPages - 1 ? C.border : C.outline; }}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>chevron_right</span>
                    </button>
                </div>
            )}

            <style>{`.material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }`}</style>
        </div>
    );
}
