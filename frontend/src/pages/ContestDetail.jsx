import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import SubmissionService from '../services/submission.service';
import useResponsive from '../hooks/useResponsive';

// ── Design tokens ─────────────────────────────────────────────────────────────
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
    success:    '#4ade80',
};

// ── Difficulty config ─────────────────────────────────────────────────────────
const DIFF_CFG = {
    EASY:   { color: '#4ade80', label: 'Easy' },
    MEDIUM: { color: C.secondary, label: 'Medium' },
    HARD:   { color: '#fb923c', label: 'Hard' },
};

// ── Submission status config ──────────────────────────────────────────────────
const STATUS_CFG = {
    AC:      { icon: 'check_circle',   color: '#4ade80',  bar: '#4ade80',  label: 'Accepted' },
    WA:      { icon: 'cancel',         color: C.error,    bar: C.error,    label: 'Wrong Answer' },
    TLE:     { icon: 'hourglass_empty',color: '#facc15',  bar: '#facc15',  label: 'TLE' },
    RE:      { icon: 'error',          color: '#fb923c',  bar: '#fb923c',  label: 'Runtime Error' },
    CE:      { icon: 'code_off',       color: '#fb923c',  bar: '#fb923c',  label: 'Compile Error' },
    MLE:     { icon: 'memory',         color: '#a78bfa',  bar: '#a78bfa',  label: 'MLE' },
    PENDING: { icon: 'schedule',       color: C.outline,  bar: C.outline,  label: 'Pending' },
    JUDGING: { icon: 'sync',           color: '#7ab3e0',  bar: '#7ab3e0',  label: 'Judging' },
};
const getStatusCfg = (s) => STATUS_CFG[s] || { icon: 'horizontal_rule', color: C.border, bar: C.border, label: '—' };

// ── Live countdown ────────────────────────────────────────────────────────────
function Countdown({ startTime, endTime }) {
    const [display, setDisplay] = useState('');
    const [progress, setProgress] = useState(0);
    const [phase, setPhase] = useState('');

    useEffect(() => {
        const tick = () => {
            const now   = Date.now();
            const start = new Date(startTime).getTime();
            const end   = new Date(endTime).getTime();

            if (now < start) {
                const diff = start - now;
                const h = Math.floor(diff / 3600000);
                const m = Math.floor((diff % 3600000) / 60000);
                const s = Math.floor((diff % 60000) / 1000);
                setDisplay(`${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`);
                setPhase('Starts In');
                setProgress(0);
            } else if (now < end) {
                const diff = end - now;
                const h = Math.floor(diff / 3600000);
                const m = Math.floor((diff % 3600000) / 60000);
                const s = Math.floor((diff % 60000) / 1000);
                setDisplay(`${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`);
                setPhase('Time Remaining');
                const total = end - start;
                const elapsed = now - start;
                setProgress(Math.min(100, Math.round((elapsed / total) * 100)));
            } else {
                setDisplay('00:00:00');
                setPhase('Contest Ended');
                setProgress(100);
            }
        };
        tick();
        const id = setInterval(tick, 1000);
        return () => clearInterval(id);
    }, [startTime, endTime]);

    return (
        <div style={{ position: 'relative', overflow: 'hidden', border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, padding: '2rem', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
            {/* Amber top accent */}
            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: C.secondary }} />
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>
                {phase}
            </span>
            <div style={{ fontFamily: "'Playfair Display', serif", fontSize: '36px', fontWeight: 600, color: C.primary, letterSpacing: '0.05em', display: 'flex', alignItems: 'baseline', gap: '4px' }}>
                {display.split(':').map((seg, i) => (
                    <span key={i} style={{ display: 'flex', alignItems: 'baseline', gap: '4px' }}>
                        {i > 0 && <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '20px', color: C.outline }}>:</span>}
                        {seg}
                    </span>
                ))}
            </div>
            {/* Progress bar */}
            <div style={{ width: '100%', height: '2px', backgroundColor: C.surfaceHi, marginTop: '12px' }}>
                <div style={{ height: '100%', width: `${progress}%`, backgroundColor: C.secondary, transition: 'width 1s linear' }} />
            </div>
        </div>
    );
}

// ── Main component ────────────────────────────────────────────────────────────
export default function ContestDetail() {
    const { isMobile } = useResponsive();
    const { id }     = useParams();
    const navigate   = useNavigate();
    const [contest,     setContest]     = useState(null);
    const [problems,    setProblems]    = useState([]);
    const [submissions, setSubmissions] = useState({});
    const [loading,     setLoading]     = useState(true);
    const [error,       setError]       = useState(null);
    const [registered,  setRegistered]  = useState(false);
    const [regCount,    setRegCount]    = useState(0);
    const [registering, setRegistering] = useState(false);
    const [proctored,   setProctored]   = useState(false);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const detailRes = await api.get(`/contests/${id}/detail`);
                setContest(detailRes.data.contest);
                setProblems(detailRes.data.problems);
                setRegistered(detailRes.data.registered ?? false);
                setRegCount(detailRes.data.registrationCount ?? 0);
                setProctored(detailRes.data.proctored === true);
                try {
                    const subsRes = await SubmissionService.getUserSubmissions();
                    const map = {};
                    subsRes.data.forEach(sub => {
                        if (sub.problemId && (!map[sub.problemId] || new Date(sub.submittedAt) > new Date(map[sub.problemId].submittedAt))) {
                            map[sub.problemId] = sub;
                        }
                    });
                    setSubmissions(map);
                } catch {}
            } catch (err) {
                setError(err.response?.status === 404 ? 'Contest not found' : 'Failed to load contest');
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [id]);

    const handleRegister = async () => {
        setRegistering(true);
        try {
            const res = await api.post(`/contests/${id}/register`);
            setRegistered(true);
            setRegCount(res.data.registrationCount ?? regCount + 1);
        } catch (err) {
            alert(err.response?.data?.message || 'Registration failed');
        } finally {
            setRegistering(false);
        }
    };

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
            Loading...
        </div>
    );

    if (error || !contest) return (
        <div style={{ maxWidth: '600px', margin: '4rem auto', padding: '0 24px', textAlign: 'center' }}>
            <div style={{ border: `1px solid ${C.border}`, padding: '3rem', backgroundColor: C.surfaceLow }}>
                <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', color: C.error, marginBottom: '1rem' }}>
                    {error || 'Contest not found'}
                </h1>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.outline, marginBottom: '2rem' }}>
                    The contest you are looking for does not exist or has been removed.
                </p>
                <Link to="/contests" style={{ display: 'inline-block', padding: '12px 28px', border: `1px solid ${C.secondary}`, color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', textDecoration: 'none', transition: 'all 0.2s' }}>
                    ← Back to Contests
                </Link>
            </div>
        </div>
    );

    if (!contest.active) return (
        <div style={{ maxWidth: '600px', margin: '4rem auto', padding: '0 24px', textAlign: 'center' }}>
            <div style={{ border: `1px solid ${C.border}`, padding: '3rem', backgroundColor: C.surfaceLow }}>
                <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', color: C.error, marginBottom: '1rem' }}>
                    Contest Not Available
                </h1>
                <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: C.outline, marginBottom: '2rem' }}>
                    This contest has been disabled by the administrator.
                </p>
                <Link to="/contests" style={{ display: 'inline-block', padding: '12px 28px', border: `1px solid ${C.secondary}`, color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', textDecoration: 'none' }}>
                    ← Back to Contests
                </Link>
            </div>
        </div>
    );

    const now   = Date.now();
    const start = new Date(contest.startTime).getTime();
    const end   = new Date(contest.endTime).getTime();
    const isLive     = now >= start && now < end;
    const isUpcoming = now < start;
    const isEnded    = now >= end;

    const statusLabel = isLive ? 'Live' : isUpcoming ? 'Upcoming' : 'Ended';
    const statusColor = isLive ? C.secondary : isUpcoming ? C.primary : C.outline;

    // Contest duration string
    const durationMs = end - start;
    const dh = Math.floor(durationMs / 3600000);
    const dm = Math.floor((durationMs % 3600000) / 60000);
    const durationStr = dh > 0 ? `${dh}h ${dm > 0 ? dm + 'm' : ''}` : `${dm}m`;

    // Timeline milestones
    const midMs   = start + (end - start) / 2;
    const freezeMs = start + (end - start) * 0.8;
    const milestones = [
        { time: start,    label: 'Contest Starts',     done: now >= start },
        { time: midMs,    label: 'Halfway Point',       done: now >= midMs },
        { time: freezeMs, label: 'Scoreboard Freezes',  done: now >= freezeMs },
        { time: end,      label: 'Contest Ends',        done: now >= end },
    ];

    const formatTime = (ts) => {
        const d = new Date(ts);
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", minHeight: '100vh' }}>
            <div style={{ maxWidth: '1440px', margin: '0 auto', padding: isMobile ? '24px 16px' : '48px 64px', display: 'flex', flexDirection: 'column', gap: '48px' }}>

                {/* ── Hero Section ── */}
                <motion.section
                    initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}
                    style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '8fr 4fr', gap: '32px', alignItems: 'end', borderBottom: `1px solid ${C.border}`, paddingBottom: '48px' }}
                >
                    {/* Left: Contest info */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                        {/* Badges */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                            <span style={{ padding: '4px 12px', backgroundColor: `${statusColor}15`, border: `1px solid ${statusColor}30`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: statusColor, textTransform: 'uppercase' }}>
                                {statusLabel}
                            </span>
                            {proctored && (
                                <span style={{ padding: '4px 12px', border: `1px solid ${C.secondary}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.secondary, textTransform: 'uppercase' }}>
                                    Proctored
                                </span>
                            )}
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.outline }}>
                                Duration: {durationStr}
                            </span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.outline }}>
                                {problems.length} Problem{problems.length !== 1 ? 's' : ''}
                            </span>
                        </div>

                        {/* Title */}
                        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: 'clamp(40px, 5vw, 64px)', fontWeight: 700, lineHeight: 1.1, letterSpacing: '-0.02em', color: C.primary, margin: 0 }}>
                            {contest.name}
                        </h1>

                        {/* Description */}
                        {contest.description && (
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '17px', lineHeight: 1.6, color: C.outline, maxWidth: '640px', margin: 0 }}>
                                {contest.description}
                            </p>
                        )}

                        {/* Time range */}
                        <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
                            {[
                                { label: 'Start', value: new Date(contest.startTime).toLocaleString() },
                                { label: 'End',   value: new Date(contest.endTime).toLocaleString() },
                            ].map(({ label, value }) => (
                                <div key={label} style={{ padding: '8px 16px', backgroundColor: C.surfaceLow, border: `1px solid ${C.border}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.outline }}>
                                    <span style={{ color: C.muted }}>{label}: </span>{value}
                                </div>
                            ))}
                        </div>

                        {/* CTA buttons */}
                        <div style={{ display: 'flex', gap: '16px', marginTop: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
                            {/* Register button — shown when not yet registered */}
                            {!registered ? (
                                <button
                                    onClick={handleRegister}
                                    disabled={registering}
                                    style={{
                                        display: 'flex', alignItems: 'center', gap: '8px',
                                        padding: '14px 32px',
                                        border: `1px solid ${C.secondary}`,
                                        color: C.bg, backgroundColor: C.secondary,
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase',
                                        cursor: registering ? 'not-allowed' : 'pointer',
                                        opacity: registering ? 0.7 : 1,
                                        transition: 'all 0.2s',
                                    }}
                                    onMouseEnter={e => { if (!registering) { e.currentTarget.style.backgroundColor = C.primary; e.currentTarget.style.borderColor = C.primary; } }}
                                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.borderColor = C.secondary; }}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '16px', fontVariationSettings: "'FILL' 1" }}>how_to_reg</span>
                                    {registering ? 'Registering...' : 'Register for Contest'}
                                </button>
                            ) : (
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 20px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.secondary, fontVariationSettings: "'FILL' 1" }}>verified</span>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.secondary, textTransform: 'uppercase' }}>
                                        Registered
                                    </span>
                                </div>
                            )}

                            {registered && isLive && problems.length > 0 && (
                                <button
                                    onClick={() => navigate(proctored ? `/contests/${id}/proctored/entry` : `/problems/${problems[0].id}`)}
                                    style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '14px 32px', border: `1px solid ${C.border}`, color: C.primary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s' }}
                                    onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; e.currentTarget.style.borderColor = C.secondary; }}
                                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.primary; e.currentTarget.style.borderColor = C.border; }}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '18px', fontVariationSettings: "'FILL' 1" }}>play_arrow</span>
                                    Start Solving
                                </button>
                            )}
                            {registered && !isLive && (
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 20px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.outline }}>schedule</span>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase' }}>
                                        {isUpcoming ? 'Contest Not Started' : 'Contest Ended'}
                                    </span>
                                </div>
                            )}
                            <button
                                onClick={() => navigate('/contests')}
                                style={{ padding: '14px 24px', border: 'none', borderBottom: `1px solid transparent`, color: C.outline, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s' }}
                                onMouseEnter={e => { e.currentTarget.style.borderBottomColor = C.secondary; e.currentTarget.style.color = C.secondary; }}
                                onMouseLeave={e => { e.currentTarget.style.borderBottomColor = 'transparent'; e.currentTarget.style.color = C.outline; }}
                            >
                                ← All Contests
                            </button>
                        </div>
                    </div>

                    {/* Right: Countdown */}
                    <Countdown startTime={contest.startTime} endTime={contest.endTime} />
                </motion.section>

                {/* ── Main Content Grid ── */}
                <section style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '8fr 4fr', gap: '32px', alignItems: 'start' }}>

                    {/* ── Left: Problem Set ── */}
                    <motion.div
                        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}
                        style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}
                    >
                        <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '28px', fontWeight: 600, color: C.primary, borderBottom: `1px solid ${C.border}`, paddingBottom: '1rem', margin: 0 }}>
                            Problem Set
                        </h2>

                        <div style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, overflow: 'hidden' }}>
                            {/* Table header */}
                            <div style={{ display: 'grid', gridTemplateColumns: '48px 1fr 100px 120px', gap: '16px', padding: '14px 24px', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceHi }}>
                                {['Status', 'Problem', 'Difficulty', 'Tests Passed'].map((h, i) => (
                                    <span key={h} style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', textAlign: i === 3 ? 'right' : 'left' }}>
                                        {h}
                                    </span>
                                ))}
                            </div>

                            {!registered ? (
                                /* Registration gate */
                                <div style={{ padding: '4rem 2rem', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '40px', color: C.border }}>lock</span>
                                    <div>
                                        <p style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', color: C.muted, margin: '0 0 8px' }}>
                                            Register to Access Problems
                                        </p>
                                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.outline, margin: '0 0 20px' }}>
                                            You need to register for this contest before you can view and solve problems.
                                        </p>
                                        <button
                                            onClick={handleRegister}
                                            disabled={registering}
                                            style={{
                                                padding: '12px 28px',
                                                border: `1px solid ${C.secondary}`,
                                                backgroundColor: C.secondary, color: C.bg,
                                                fontFamily: "'JetBrains Mono', monospace",
                                                fontSize: '11px', letterSpacing: '0.12em', textTransform: 'uppercase',
                                                cursor: registering ? 'not-allowed' : 'pointer',
                                                opacity: registering ? 0.7 : 1,
                                                display: 'inline-flex', alignItems: 'center', gap: '6px',
                                            }}
                                        >
                                            <span className="material-symbols-outlined" style={{ fontSize: '14px', fontVariationSettings: "'FILL' 1" }}>how_to_reg</span>
                                            {registering ? 'Registering...' : 'Register Now'}
                                        </button>
                                    </div>
                                </div>
                            ) : isUpcoming ? (
                                /* Pre-start gate */
                                <div style={{ padding: '4rem 2rem', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '40px', color: C.border }}>schedule</span>
                                    <div>
                                        <p style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', color: C.muted, margin: '0 0 8px' }}>
                                            Contest Has Not Started
                                        </p>
                                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.outline, margin: 0 }}>
                                            Problems will be available when the contest begins.
                                        </p>
                                    </div>
                                </div>
                            ) : problems.length === 0 ? (
                                <div style={{ padding: '4rem', textAlign: 'center', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.outline }}>
                                    No problems released yet.
                                </div>
                            ) : (
                                problems.map((problem, index) => (
                                    <ProblemRow
                                        key={problem.id}
                                        problem={problem}
                                        index={index}
                                        submission={submissions[problem.id]}
                                        isLast={index === problems.length - 1}
                                        proctored={proctored}
                                        contestId={id}
                                        registered={registered}
                                    />                                ))
                            )}
                        </div>
                    </motion.div>

                    {/* ── Right: Rules + Timeline ── */}
                    <motion.div
                        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.2 }}
                        style={{ display: 'flex', flexDirection: 'column', gap: '3rem', position: 'sticky', top: '2rem' }}
                    >
                        {/* Registration Panel */}
                        <div style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, overflow: 'hidden' }}>
                            <div style={{ position: 'relative', overflow: 'hidden' }}>
                                <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '2px', backgroundColor: registered ? C.secondary : C.border }} />
                                <div style={{ padding: '16px', borderBottom: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>
                                        Registration
                                    </span>
                                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted }}>
                                        {regCount} registered
                                    </span>
                                </div>
                                <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                    {registered ? (
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                            <span className="material-symbols-outlined" style={{ fontSize: '20px', color: C.secondary, fontVariationSettings: "'FILL' 1" }}>verified</span>
                                            <div>
                                                <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.secondary, letterSpacing: '0.08em' }}>You are registered</div>
                                                <div style={{ fontFamily: "'Geist', sans-serif", fontSize: '12px', color: C.outline, marginTop: '2px' }}>You can submit solutions</div>
                                            </div>
                                        </div>
                                    ) : (
                                        <>
                                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.outline, margin: 0, lineHeight: 1.5 }}>
                                                Register to participate and submit solutions in this contest.
                                            </p>
                                            <button
                                                onClick={handleRegister}
                                                disabled={registering}
                                                style={{
                                                    padding: '10px 16px',
                                                    border: `1px solid ${C.secondary}`,
                                                    backgroundColor: C.secondary,
                                                    color: C.bg,
                                                    fontFamily: "'JetBrains Mono', monospace",
                                                    fontSize: '11px', letterSpacing: '0.12em', textTransform: 'uppercase',
                                                    cursor: registering ? 'not-allowed' : 'pointer',
                                                    opacity: registering ? 0.7 : 1,
                                                    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
                                                    transition: 'all 0.2s',
                                                }}
                                                onMouseEnter={e => { if (!registering) { e.currentTarget.style.backgroundColor = C.primary; e.currentTarget.style.borderColor = C.primary; } }}
                                                onMouseLeave={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.borderColor = C.secondary; }}
                                            >
                                                <span className="material-symbols-outlined" style={{ fontSize: '14px', fontVariationSettings: "'FILL' 1" }}>how_to_reg</span>
                                                {registering ? 'Registering...' : 'Register Now'}
                                            </button>
                                        </>
                                    )}
                                </div>
                            </div>
                        </div>

                        {/* Briefing & Rules */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.secondary, textTransform: 'uppercase', borderBottom: `1px solid rgba(233,193,118,0.3)`, paddingBottom: '8px', margin: 0 }}>
                                Briefing &amp; Rules
                            </h3>
                            <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                {[
                                    { icon: 'terminal', text: 'Standard I/O formatting is strictly enforced. Avoid extra whitespace.' },
                                    { icon: 'gavel',    text: 'Plagiarism results in immediate disqualification.' },
                                    { icon: 'timer',    text: 'Each problem has individual time and memory limits.' },
                                    { icon: 'code',     text: 'Supported languages: Java, C++, Python, JavaScript, C.' },
                                ].map(({ icon, text }) => (
                                    <li key={icon} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                                        <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.secondary, flexShrink: 0, marginTop: '2px' }}>{icon}</span>
                                        <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: C.outline, lineHeight: 1.5 }}>{text}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>

                        {/* Contest Timeline */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            <h3 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.15em', color: C.secondary, textTransform: 'uppercase', borderBottom: `1px solid rgba(233,193,118,0.3)`, paddingBottom: '8px', margin: 0 }}>
                                Contest Timeline
                            </h3>
                            <div style={{ position: 'relative', paddingLeft: '24px', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                                {/* Vertical line */}
                                <div style={{ position: 'absolute', left: '7px', top: '8px', bottom: '8px', width: '1px', backgroundColor: C.border }} />

                                {milestones.map(({ time, label, done }) => (
                                    <div key={label} style={{ position: 'relative', opacity: done ? 1 : 0.5 }}>
                                        {/* Dot */}
                                        <div style={{ position: 'absolute', left: '-21px', top: '4px', width: '8px', height: '8px', borderRadius: '50%', backgroundColor: done ? C.secondary : C.border, boxShadow: done ? `0 0 0 3px ${C.bg}` : 'none', border: done ? 'none' : `1px solid ${C.border}` }} />
                                        <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.1em', color: done ? C.secondary : C.outline, textTransform: 'uppercase', marginBottom: '4px' }}>
                                            {formatTime(time)}
                                        </p>
                                        <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '14px', color: done ? C.onBg : C.outline, margin: 0 }}>
                                            {label}
                                        </p>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Stats */}
                        <div style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, overflow: 'hidden' }}>
                            <div style={{ padding: '12px 16px', borderBottom: `1px solid ${C.border}`, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>
                                Your Progress
                            </div>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1px', backgroundColor: C.border }}>
                                {[
                                    { label: 'Solved',     value: Object.values(submissions).filter(s => s.status === 'AC').length },
                                    { label: 'Attempted',  value: Object.keys(submissions).length },
                                    { label: 'Total',      value: problems.length },
                                    { label: 'Remaining',  value: problems.length - Object.values(submissions).filter(s => s.status === 'AC').length },
                                ].map(({ label, value }) => (
                                    <div key={label} style={{ backgroundColor: C.bg, padding: '1rem', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', color: C.outline, textTransform: 'uppercase' }}>{label}</span>
                                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 300, color: C.onBg }}>{value}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </motion.div>
                </section>
            </div>

            <style>{`.material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }`}</style>
        </div>
    );
}

// ── Problem Row ───────────────────────────────────────────────────────────────
//
// In a proctored contest the row links to the proctored entry shell instead
// of the bare /problems/:id route — that way a registered candidate ALWAYS
// goes through eligibility → consent → preflight → arena before reaching the
// editor (Req 2.x, 3.x). The bare route is still navigable directly (an
// admin clicking through, an unregistered curious user, etc.) but for the
// proctored happy path the gate is enforced at the link layer.
function ProblemRow({ problem, index, submission, isLast, proctored, contestId, registered }) {
    const [hovered, setHovered] = useState(false);
    const sc   = submission ? getStatusCfg(submission.status) : null;
    const diff = DIFF_CFG[problem.level] || { color: C.outline, label: problem.level || '—' };
    const letter = String.fromCharCode(65 + index);

    // Proctored mode: route every problem click through the entry gate so the
    // candidate cannot bypass consent / preflight by deep-linking. Once the
    // proctored arena gets the real problem-solve UI (follow-up task) the
    // arena will route to specific problems via state.problemId.
    const target = proctored && registered
        ? `/contests/${contestId}/proctored/entry`
        : `/problems/${problem.id}`;

    // Test cases display
    const passed = submission?.testCasesPassed != null ? submission.testCasesPassed : null;
    const total  = submission?.totalTestCases  != null ? submission.totalTestCases  : null;

    return (
        <Link
            to={target}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'grid',
                gridTemplateColumns: '48px 1fr 100px 120px',
                gap: '16px',
                padding: '20px 24px',
                borderBottom: isLast ? 'none' : `1px solid ${C.border}`,
                backgroundColor: hovered ? C.surfaceHi : 'transparent',
                textDecoration: 'none',
                transition: 'background-color 0.2s',
                position: 'relative',
            }}
        >
            {/* Left status bar */}
            <div style={{ position: 'absolute', left: 0, top: 0, bottom: 0, width: '3px', backgroundColor: sc ? sc.bar : C.border }} />

            {/* Status icon */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                {sc ? (
                    <span
                        className="material-symbols-outlined"
                        style={{ fontSize: '20px', color: sc.color, fontVariationSettings: "'FILL' 1" }}
                    >
                        {sc.icon}
                    </span>
                ) : (
                    <span className="material-symbols-outlined" style={{ fontSize: '20px', color: C.border }}>
                        horizontal_rule
                    </span>
                )}
            </div>

            {/* Problem name */}
            <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <span style={{ fontFamily: "'Geist', sans-serif", fontSize: '15px', color: hovered ? C.primary : C.onBg, transition: 'color 0.2s' }}>
                    {letter}. {problem.title}
                </span>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, marginTop: '4px' }}>
                    {problem.timeLimit}s · {problem.memoryLimit}MB
                </span>
            </div>

            {/* Difficulty */}
            <div style={{ display: 'flex', alignItems: 'center' }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: diff.color }}>
                    {diff.label}
                </span>
            </div>

            {/* Tests Passed */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end' }}>
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: passed != null && total != null && passed === total ? C.success : C.outline }}>
                    {passed != null && total != null ? `${passed} / ${total}` : '—'}
                </span>
            </div>
        </Link>
    );
}

// ── useState import needed for sub-components ─────────────────────────────────
