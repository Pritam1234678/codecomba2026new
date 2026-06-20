import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import useResponsive from '../hooks/useResponsive';

// ── Theme tokens (matches Practice.jsx) ──────────────────────────────────────
const C = {
    bg: '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceHi: '#2a2a2a',
    border: '#50453b',
    primary: '#f1bc8b',
    secondary: '#e9c176',
    muted: '#d4c4b7',
    outline: '#9d8e83',
    onBg: '#e5e2e1',
    error: '#ffb4ab',
    success: '#4ade80',
};

const DIFF_CFG = {
    EASY: { color: C.success, label: 'EASY' },
    MEDIUM: { color: C.secondary, label: 'MEDIUM' },
    HARD: { color: C.error, label: 'HARD' },
};

const LANG_CFG = {
    JAVA: { color: '#f89820', label: 'JAVA' },
    NODEJS: { color: '#68a063', label: 'NODE.JS' },
    PYTHON: { color: '#3572A5', label: 'PYTHON' },
};

const WebContestList = () => {
    const { isMobile } = useResponsive();
    const navigate = useNavigate();
    const [challenges, setChallenges] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        api.get('/web-contest/list')
            .then(res => setChallenges(res.data))
            .catch(err => {
                console.error(err);
                setError('Failed to load challenges.');
            })
            .finally(() => setLoading(false));
    }, []);

    if (loading) return (
        <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            minHeight: '60vh', color: C.outline,
            fontFamily: "'JetBrains Mono', monospace", fontSize: '13px',
        }}>
            Loading challenges...
        </div>
    );

    return (
        <div style={{
            backgroundColor: C.bg, color: C.onBg,
            fontFamily: "'Geist', sans-serif",
            minHeight: '100vh',
            padding: isMobile ? '24px 16px' : '48px 64px',
        }}>
            {/* ── Hero ── */}
            <motion.section
                initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
                style={{ marginBottom: '3rem', borderBottom: `1px solid ${C.border}`, paddingBottom: '2rem' }}
            >
                <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: '32px', flexWrap: 'wrap' }}>
                    <div>
                        <span style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px', letterSpacing: '0.2em',
                            color: C.secondary, textTransform: 'uppercase',
                        }}>
                            Web Development
                        </span>
                        <h1 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: 'clamp(36px, 5vw, 56px)',
                            fontWeight: 700, lineHeight: 1.1,
                            color: C.primary, margin: '0.5rem 0 0.75rem',
                        }}>
                            Into the Web
                        </h1>
                        <p style={{
                            fontSize: '15px', color: C.muted,
                            lineHeight: 1.6, maxWidth: '560px', margin: 0,
                        }}>
                            Build real backend APIs and web services. Each challenge gives you a project
                            template — complete the implementation and pass all integration tests.
                        </p>
                    </div>

                    {/* Challenge count badge */}
                    <div style={{
                        border: `1px solid ${C.border}`,
                        backgroundColor: C.surfaceLow,
                        padding: '1rem 1.5rem',
                    }}>
                        <div style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '10px', letterSpacing: '0.12em',
                            color: C.outline, textTransform: 'uppercase', marginBottom: '4px',
                        }}>
                            Challenges
                        </div>
                        <div style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '28px', fontWeight: 600,
                            color: C.secondary, lineHeight: 1,
                        }}>
                            {challenges.length}
                        </div>
                    </div>
                </div>
            </motion.section>

            {/* ── Challenge list ── */}
            {error ? (
                <div style={{
                    border: `1px solid ${C.error}`, padding: '2rem',
                    textAlign: 'center', fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '13px', color: C.error,
                }}>
                    {error}
                </div>
            ) : challenges.length === 0 ? (
                <div style={{
                    border: `1px solid ${C.border}`, padding: '4rem',
                    textAlign: 'center', fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '13px', color: C.outline,
                }}>
                    No challenges available yet. Check back soon.
                </div>
            ) : (
                <motion.div
                    initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.4, delay: 0.2 }}
                    style={{
                        border: `1px solid ${C.border}`,
                        backgroundColor: C.surfaceLow,
                        overflow: 'hidden',
                    }}
                >
                    {challenges.map((c, i) => (
                        <ChallengeRow
                            key={c.templateId}
                            challenge={c}
                            index={i}
                            isLast={i === challenges.length - 1}
                            onClick={() => navigate(`/web-contest/${c.problemId}/ide`)}
                        />
                    ))}
                </motion.div>
            )}

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
            `}</style>
        </div>
    );
};

// ── Challenge row ─────────────────────────────────────────────────────────────
const ChallengeRow = ({ challenge, index, isLast, onClick }) => {
    const [hovered, setHovered] = useState(false);
    const diff = DIFF_CFG[challenge.difficulty] || { color: C.outline, label: challenge.difficulty || '—' };
    const lang = LANG_CFG[(challenge.language || '').toUpperCase()] || { color: C.outline, label: challenge.language || '—' };

    return (
        <motion.div
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.05, duration: 0.35 }}
            onClick={onClick}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: '16px',
                padding: '1.25rem 1.5rem',
                borderBottom: isLast ? 'none' : `1px solid ${C.border}`,
                backgroundColor: hovered ? C.surfaceCon : 'transparent',
                cursor: 'pointer',
                transition: 'background-color 0.15s',
                borderLeft: hovered ? `3px solid ${diff.color}` : '3px solid transparent',
            }}
        >
            {/* Index */}
            <span style={{
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '11px', color: C.outline,
                minWidth: '24px', flexShrink: 0,
            }}>
                {String(index + 1).padStart(2, '0')}
            </span>

            {/* Title */}
            <span style={{
                fontFamily: "'Playfair Display', serif",
                fontSize: '18px', fontWeight: 600,
                color: hovered ? C.primary : C.onBg,
                flex: 1,
                transition: 'color 0.15s',
            }}>
                {challenge.title}
            </span>

            {/* Badges */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
                {/* Language badge */}
                <span style={{
                    padding: '3px 10px',
                    border: `1px solid ${lang.color}`,
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '10px', letterSpacing: '0.1em',
                    color: lang.color, textTransform: 'uppercase',
                }}>
                    {lang.label}
                </span>

                {/* Difficulty badge */}
                <span style={{
                    padding: '3px 10px',
                    border: `1px solid ${diff.color}`,
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '10px', letterSpacing: '0.1em',
                    color: diff.color, textTransform: 'uppercase',
                    minWidth: '64px', textAlign: 'center',
                }}>
                    {diff.label}
                </span>

                {/* Arrow */}
                <span className="material-symbols-outlined" style={{
                    fontSize: '20px',
                    color: hovered ? diff.color : C.outline,
                    transition: 'color 0.15s, transform 0.15s',
                    transform: hovered ? 'translateX(4px)' : 'translateX(0)',
                }}>
                    arrow_forward
                </span>
            </div>
        </motion.div>
    );
};

export default WebContestList;
