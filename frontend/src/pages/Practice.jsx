import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import PracticeService from '../services/practice.service';

// ── Theme tokens ──────────────────────────────────────────────────────────────
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
    success:    '#4ade80',
};

const DIFF_CFG = {
    EASY:   { color: C.success,   label: 'Easy' },
    MEDIUM: { color: C.secondary, label: 'Medium' },
    HARD:   { color: C.error,     label: 'Hard' },
};

const Practice = () => {
    const navigate = useNavigate();
    const [problems, setProblems] = useState([]);
    const [stats, setStats]       = useState({ totalPoints: 0, solvedCount: 0 });
    const [loading, setLoading]   = useState(true);
    const [filter, setFilter]     = useState('ALL'); // ALL | UNSOLVED | SOLVED | EASY | MEDIUM | HARD
    const [search, setSearch]     = useState('');

    useEffect(() => {
        Promise.all([PracticeService.listProblems(), PracticeService.stats()])
            .then(([pRes, sRes]) => {
                setProblems(pRes.data);
                setStats(sRes.data);
            })
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    const filtered = problems.filter(p => {
        const matchSearch = p.title.toLowerCase().includes(search.toLowerCase());
        let matchFilter = true;
        if (filter === 'SOLVED')   matchFilter = p.solved;
        else if (filter === 'UNSOLVED') matchFilter = !p.solved;
        else if (['EASY','MEDIUM','HARD'].includes(filter)) matchFilter = p.level === filter;
        return matchSearch && matchFilter;
    });

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
            Loading problems...
        </div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh', padding: '48px 64px' }}>
            {/* ── Hero ── */}
            <motion.section
                initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                style={{ marginBottom: '3rem', borderBottom: `1px solid ${C.border}`, paddingBottom: '2rem' }}
            >
                <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: '32px', flexWrap: 'wrap' }}>
                    <div>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.2em', color: C.secondary, textTransform: 'uppercase' }}>
                            Practice Mode
                        </span>
                        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(36px, 5vw, 56px)', fontWeight: 700, lineHeight: 1.1, color: C.primary, margin: '0.5rem 0 0.75rem' }}>
                            Sharpen Your Skills
                        </h1>
                        <p style={{ fontSize: '15px', color: C.muted, lineHeight: 1.6, maxWidth: '560px', margin: 0 }}>
                            Solve problems at your own pace. Earn points for each problem you solve — independent from contests.
                        </p>
                    </div>

                    {/* Stats panel */}
                    <div style={{ display: 'flex', gap: '0', border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}>
                        <StatCell label="Points" value={stats.totalPoints} accent={C.secondary} />
                        <StatCell label="Solved" value={stats.solvedCount} accent={C.success} divider />
                        <StatCell label="Total" value={problems.length} divider />
                    </div>
                </div>
            </motion.section>

            {/* ── Filters ── */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', gap: '1rem', flexWrap: 'wrap' }}>
                {/* Search */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', borderBottom: `1px solid ${C.border}`, paddingBottom: '6px', minWidth: '280px', flex: '1 1 280px', maxWidth: '400px' }}>
                    <span className="material-symbols-outlined" style={{ color: C.outline, fontSize: '18px' }}>search</span>
                    <input
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        placeholder="SEARCH PROBLEMS..."
                        style={{ background: 'transparent', border: 'none', outline: 'none', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.08em', color: C.onBg, width: '100%' }}
                    />
                </div>

                {/* Filter tabs */}
                <div style={{ display: 'flex', border: `1px solid ${C.border}` }}>
                    {[
                        { key: 'ALL',      label: 'All' },
                        { key: 'UNSOLVED', label: 'Unsolved' },
                        { key: 'SOLVED',   label: 'Solved' },
                        { key: 'EASY',     label: 'Easy' },
                        { key: 'MEDIUM',   label: 'Medium' },
                        { key: 'HARD',     label: 'Hard' },
                    ].map((f, i, arr) => (
                        <button key={f.key} onClick={() => setFilter(f.key)}
                            style={{
                                fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em',
                                padding: '8px 16px',
                                background: filter === f.key ? C.secondary : 'transparent',
                                color: filter === f.key ? C.bg : C.outline,
                                border: 'none', cursor: 'pointer',
                                borderRight: i < arr.length - 1 ? `1px solid ${C.border}` : 'none',
                                textTransform: 'uppercase',
                                transition: 'all 0.2s',
                            }}
                        >
                            {f.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* ── Problem cards grid ── */}
            {filtered.length === 0 ? (
                <div style={{ border: `1px solid ${C.border}`, padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                    No problems found
                </div>
            ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '20px' }}>
                    {filtered.map((p, i) => (
                        <ProblemCard key={p.id} problem={p} index={i} onSolve={() => navigate(`/practice/${p.id}`)} />
                    ))}
                </div>
            )}

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
            `}</style>
        </div>
    );
};

// ── Sub-components ────────────────────────────────────────────────────────────

const StatCell = ({ label, value, accent = '#d4c4b7', divider }) => (
    <div style={{ padding: '1rem 1.5rem', borderLeft: divider ? `1px solid ${C.border}` : 'none', minWidth: '110px' }}>
        <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase', marginBottom: '4px' }}>
            {label}
        </div>
        <div style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: accent, lineHeight: 1 }}>
            {value}
        </div>
    </div>
);

const ProblemCard = ({ problem, index, onSolve }) => {
    const [hovered, setHovered] = useState(false);
    const diff = DIFF_CFG[problem.level] || { color: C.outline, label: problem.level || '—' };

    return (
        <motion.div
            initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * 0.04, duration: 0.4 }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                border: `1px solid ${hovered ? diff.color : C.border}`,
                backgroundColor: hovered ? C.surfaceCon : C.surfaceLow,
                padding: '1.5rem',
                display: 'flex', flexDirection: 'column', gap: '12px',
                transition: 'all 0.2s',
                position: 'relative',
                overflow: 'hidden',
            }}
        >
            {/* Top accent bar */}
            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: diff.color, opacity: hovered ? 1 : 0.3, transition: 'opacity 0.2s' }} />

            {/* Top row: difficulty + solved status */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                <span style={{ padding: '3px 10px', border: `1px solid ${diff.color}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: diff.color, textTransform: 'uppercase' }}>
                    {diff.label}
                </span>
                {problem.solved && (
                    <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: C.success, textTransform: 'uppercase' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '14px', fontVariationSettings: "'FILL' 1" }}>check_circle</span>
                        Solved
                    </span>
                )}
            </div>

            {/* Title */}
            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 600, color: hovered ? C.primary : C.onBg, margin: 0, lineHeight: 1.3, transition: 'color 0.2s' }}>
                {problem.title}
            </h3>

            {/* Description preview */}
            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.outline, margin: 0, lineHeight: 1.5, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                {problem.description}
            </p>

            {/* Meta row */}
            <div style={{ display: 'flex', gap: '12px', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline }}>
                <span>⏱ {problem.timeLimit}s</span>
                <span>·</span>
                <span>💾 {problem.memoryLimit}MB</span>
                <span style={{ marginLeft: 'auto', color: C.secondary }}>+{problem.pointsAvailable} pts</span>
            </div>

            {/* Solve button */}
            <button
                onClick={onSolve}
                style={{
                    marginTop: '4px',
                    padding: '10px 16px',
                    border: `1px solid ${C.secondary}`,
                    backgroundColor: hovered ? C.secondary : 'transparent',
                    color: hovered ? C.bg : C.secondary,
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '11px',
                    letterSpacing: '0.12em',
                    textTransform: 'uppercase',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                }}
            >
                <span className="material-symbols-outlined" style={{ fontSize: '14px', fontVariationSettings: "'FILL' 1" }}>
                    {problem.solved ? 'replay' : 'play_arrow'}
                </span>
                {problem.solved ? 'Practice Again' : 'Solve'}
            </button>
        </motion.div>
    );
};

export default Practice;
