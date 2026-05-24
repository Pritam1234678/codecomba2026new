import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import useDuelStream from '../hooks/useDuelStream';
import {
    getMatch,
    submitDuelCode,
    forfeit,
    heartbeat,
    getMatchSubmissions,
} from '../services/duelService';
import ProblemService from '../services/problem.service';

// ── Design tokens (mirrors AdminDashboard.jsx / ProblemSolve.jsx) ───────────
const C = {
    bg:         '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceHi:  '#2a2a2a',
    surfaceMin: '#0e0e0e',
    border:     '#50453b',
    borderThin: 'rgba(80,69,59,0.6)',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
    success:    '#4ade80',
    warning:    '#facc15',
};

const LANGS = [
    { value: 'JAVA',       label: 'Java 21',     monaco: 'java' },
    { value: 'CPP',        label: 'C++ 20',      monaco: 'cpp' },
    { value: 'C',          label: 'C',           monaco: 'c' },
    { value: 'PYTHON',     label: 'Python 3.11', monaco: 'python' },
    { value: 'JAVASCRIPT', label: 'JavaScript',  monaco: 'javascript' },
];

const STARTER = {
    JAVA:       '// Write your solution here\npublic class Main {\n    public static void main(String[] args) {\n    }\n}\n',
    CPP:        '// Write your solution here\n#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    return 0;\n}\n',
    C:          '/* Write your solution here */\n#include <stdio.h>\nint main(void) {\n    return 0;\n}\n',
    PYTHON:     '# Write your solution here\n',
    JAVASCRIPT: '// Write your solution here\n',
};

// Outcomes the backend may emit on match_finished. Map to a presentation
// tuple keyed off the seat of "self".
function resultForViewer(outcome, winnerUserId, selfUserId) {
    if (!outcome) return null;
    if (outcome === 'DRAW')      return { title: 'Draw.',             tone: C.warning };
    if (outcome === 'ABANDONED') return { title: 'Match Abandoned.',  tone: C.outline };
    if (winnerUserId == null)    return { title: 'Match Ended.',      tone: C.outline };
    if (selfUserId != null && Number(winnerUserId) === Number(selfUserId)) {
        return { title: 'You Won!', tone: C.success };
    }
    return { title: 'You Lost.', tone: C.error };
}

// Small helper so the countdown looks consistent (mm:ss).
function fmtMmSs(totalSeconds) {
    if (totalSeconds == null || totalSeconds < 0) return '--:--';
    const m = Math.floor(totalSeconds / 60);
    const s = totalSeconds % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

// ── Status pill renderer for the opponent panel ──────────────────────────────
function OpponentStatus({ stream, opponentUserId }) {
    // Pick the most informative pill based on the events list (latest verdict
    // wins) and the live disconnected/typing flags.
    const verdict = useMemo(() => {
        if (!stream?.events?.length) return null;
        for (let i = stream.events.length - 1; i >= 0; i--) {
            const e = stream.events[i];
            if (e?.type !== 'progress') continue;
            const d = e.data ?? {};
            if (d.event !== 'verdict') continue;
            // Only opponent verdicts populate the opponent panel.
            if (opponentUserId != null && Number(d.userId) !== Number(opponentUserId)) continue;
            return d;
        }
        return null;
    }, [stream?.events, opponentUserId]);

    const submitted = useMemo(() => {
        if (!stream?.events?.length) return false;
        for (let i = stream.events.length - 1; i >= 0; i--) {
            const e = stream.events[i];
            if (e?.type !== 'progress') continue;
            const d = e.data ?? {};
            if (opponentUserId != null && Number(d.userId) !== Number(opponentUserId)) continue;
            if (d.event === 'verdict')   return false; // verdict supersedes
            if (d.event === 'submitted') return true;
        }
        return false;
    }, [stream?.events, opponentUserId]);

    let label = 'idle';
    let tone  = C.outline;

    if (stream?.opponentDisconnected) {
        label = 'disconnected';
        tone  = C.error;
    } else if (verdict) {
        const passed = verdict.testCasesPassed ?? verdict.passed ?? 0;
        const total  = verdict.totalTestCases  ?? verdict.total  ?? 0;
        label = `verdict: ${verdict.status} ${passed}/${total}`;
        tone  = verdict.status === 'AC' ? C.success : C.error;
    } else if (submitted) {
        label = 'submitted';
        tone  = C.secondary;
    } else if (stream?.opponentTyping) {
        label = 'typing';
        tone  = C.primary;
    }

    return (
        <span style={{
            display: 'inline-flex', alignItems: 'center', gap: '6px',
            padding: '4px 10px',
            border: `1px solid ${tone}`,
            color: tone,
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '11px', letterSpacing: '0.05em', textTransform: 'lowercase',
            backgroundColor: 'rgba(0,0,0,0.25)',
        }}>
            <span style={{
                width: 6, height: 6, borderRadius: '50%',
                backgroundColor: tone, display: 'inline-block',
            }} />
            {label}
        </span>
    );
}

// ── Main page ───────────────────────────────────────────────────────────────
const DuelArena = () => {
    const { matchId } = useParams();
    const navigate    = useNavigate();
    const stream      = useDuelStream(matchId);

    // Self user pulled from existing localStorage key (same pattern as
    // AdminDashboard.jsx / CoderCompiler.jsx).
    const self = useMemo(() => {
        try { return JSON.parse(localStorage.getItem('user') || 'null'); }
        catch { return null; }
    }, []);
    const selfUserId = self?.id ?? null;

    // ── Match details (problem statement, usernames, initial timer) ──────────
    const [match,         setMatch]         = useState(null);
    const [matchLoading,  setMatchLoading]  = useState(true);
    const [participantOk, setParticipantOk] = useState(true);
    const [matchLoadErr,  setMatchLoadErr]  = useState(null);

    useEffect(() => {
        let cancelled = false;
        setMatchLoading(true);
        getMatch(matchId)
            .then((data) => {
                if (cancelled) return;
                setMatch(data);
                setParticipantOk(true);
                setMatchLoadErr(null);
            })
            .catch((err) => {
                if (cancelled) return;
                const status = err?.response?.status;
                const code   = err?.response?.data?.error;
                if (status === 403 || code === 'NOT_A_PARTICIPANT') {
                    setParticipantOk(false);
                } else {
                    setMatchLoadErr(err?.response?.data?.message ?? 'Failed to load match');
                }
            })
            .finally(() => {
                if (!cancelled) setMatchLoading(false);
            });
        return () => { cancelled = true; };
    }, [matchId]);

    // ── Editor state ─────────────────────────────────────────────────────────
    const [language, setLanguage] = useState('JAVA');
    const [code,     setCode]     = useState(STARTER.JAVA);
    const [snippets, setSnippets] = useState({});
    const [submitting, setSubmitting] = useState(false);
    const [submitErr,  setSubmitErr]  = useState(null);
    const [history,    setHistory]    = useState([]); // [{ submissionId, language, ts }]
    const lastHeartbeatRef = useRef(0);

    // ── Load per-language starter snippets for this duel's problem ───────────
    // The platform's judge harness expects ONLY the function body / user code
    // between USER_CODE_START / USER_CODE_END markers — submitting a full
    // `public class Main { ... }` produces a compile error every time. The
    // snippets endpoint returns the starter template per language so the
    // editor seeds with code that the harness can actually splice in.
    useEffect(() => {
        if (!match?.problemId) return;
        let cancelled = false;
        ProblemService.getSnippets(match.problemId)
            .then((res) => {
                if (cancelled) return;
                const map = {};
                (res.data || []).forEach((s) => {
                    if (s?.language && s?.starterCode) {
                        map[s.language] = s.starterCode;
                    }
                });
                setSnippets(map);
                // Seed the editor with the current language's starter if we
                // have one. The user can still edit freely afterwards.
                if (map[language]) {
                    setCode(map[language]);
                }
            })
            .catch((err) => {
                console.warn('Failed to load problem snippets', err);
                // Fall back to the generic STARTER (already in state).
            });
        return () => { cancelled = true; };
    }, [match?.problemId]);  // eslint-disable-line react-hooks/exhaustive-deps

    // ── Load existing duel-scoped submissions for this user on mount ────────
    // The hook only captures verdicts from SSE events delivered while the
    // page is open — so a refresh inside the arena loses every prior verdict.
    // Backend exposes /api/duels/{matchId}/submissions to rebuild the list.
    useEffect(() => {
        if (!matchId) return;
        let cancelled = false;
        getMatchSubmissions(matchId)
            .then((rows) => {
                if (cancelled) return;
                const items = Array.isArray(rows) ? rows : [];
                // Match the in-memory shape the SSE handler produces.
                const decorated = items.map((s) => ({
                    submissionId: s.submissionId,
                    status:       s.status,
                    passed:       s.testCasesPassed ?? 0,
                    total:        s.totalTestCases ?? 0,
                    ts:           s.submittedAt
                        ? new Date(s.submittedAt).getTime()
                        : Date.now(),
                }));
                // Server returns newest-first; we render in append order so
                // reverse here to keep newest-first after our `.slice().reverse()` flip.
                setHistory(decorated.slice().reverse());
            })
            .catch(() => {
                // Non-fatal — empty list is fine, future SSE verdicts will populate.
            });
        return () => { cancelled = true; };
    }, [matchId]);

    const onCodeChange = useCallback((newCode) => {
        const value = newCode ?? '';
        setCode(value);
        const now = Date.now();
        if (now - lastHeartbeatRef.current > 1500) {
            lastHeartbeatRef.current = now;
            heartbeat(matchId, 'typing').catch(() => {});
        }
    }, [matchId]);

    const onLanguageChange = (lang) => {
        setLanguage(lang);
        // Prefer the problem's per-language starter snippet over the generic
        // boilerplate. Falls back to STARTER[lang] only if the snippet
        // endpoint hasn't loaded yet or this language has no snippet.
        const trimmed = (code ?? '').trim();
        const isStarter = Object.values(STARTER).some((s) => s.trim() === trimmed)
            || Object.values(snippets).some((s) => (s ?? '').trim() === trimmed);
        if (!trimmed || isStarter) {
            setCode(snippets[lang] ?? STARTER[lang] ?? '');
        }
    };

    // ── Match countdown ──────────────────────────────────────────────────────
    // Seed remaining seconds from `room_state` (or the initial GET) and tick
    // down once per second until 0. The server is still authoritative for
    // outcome — we never end the match client-side.
    const [remaining, setRemaining] = useState(null);
    useEffect(() => {
        const seed =
            (typeof stream?.remainingSeconds === 'number' ? stream.remainingSeconds : null) ??
            (typeof match?.remainingSeconds  === 'number' ? match.remainingSeconds  : null);
        if (seed != null) setRemaining(seed);
    }, [stream?.remainingSeconds, match?.remainingSeconds]);

    useEffect(() => {
        if (remaining == null) return undefined;
        if (stream?.status === 'FINISHED') return undefined;
        const id = setInterval(() => {
            setRemaining((s) => (s == null ? s : Math.max(0, s - 1)));
        }, 1000);
        return () => clearInterval(id);
    }, [remaining, stream?.status]);

    // ── Capture verdicts that belong to "me" so we can build the duel-scoped
    //    submission history list on the left pane. We pluck them out of the
    //    same events log the hook is already maintaining. ──
    useEffect(() => {
        if (!stream?.events?.length) return;
        // Walk new events since last seen and append verdicts for self.
        const verdicts = stream.events
            .filter((e) => e?.type === 'progress' && e?.data?.event === 'verdict')
            .filter((e) => selfUserId != null && Number(e.data.userId) === Number(selfUserId));
        if (!verdicts.length) return;
        setHistory((prev) => {
            const seen = new Set(prev.map((h) => h.submissionId));
            const next = [...prev];
            for (const v of verdicts) {
                const sid = v.data.submissionId;
                if (sid == null || seen.has(sid)) continue;
                seen.add(sid);
                next.push({
                    submissionId: sid,
                    status:       v.data.status,
                    passed:       v.data.testCasesPassed ?? v.data.passed ?? 0,
                    total:        v.data.totalTestCases  ?? v.data.total  ?? 0,
                    ts:           v.ts,
                });
            }
            return next.slice(-20);
        });
    }, [stream?.events, selfUserId]);

    // ── Submit ───────────────────────────────────────────────────────────────
    const isMatchFinished = stream?.status === 'FINISHED' || match?.status === 'FINISHED';
    const handleSubmit = async () => {
        if (submitting) return;
        if (isMatchFinished) return;
        setSubmitting(true);
        setSubmitErr(null);
        try {
            const resp = await submitDuelCode(matchId, code, language);
            const sid = resp?.submissionId ?? null;
            if (sid != null) {
                setHistory((prev) => [
                    ...prev,
                    { submissionId: sid, status: 'PENDING', passed: 0, total: 0, ts: Date.now() },
                ].slice(-20));
            }
        } catch (err) {
            const status = err?.response?.status;
            const data   = err?.response?.data;
            if (status === 409 && data?.error === 'MATCH_FINISHED') {
                setSubmitErr('Match already finished.');
            } else if (status === 422) {
                setSubmitErr('Unsupported language.');
            } else if (status === 429) {
                setSubmitErr('Slow down — too many submissions.');
            } else {
                setSubmitErr(data?.message ?? 'Submission failed');
            }
        } finally {
            setSubmitting(false);
        }
    };

    // ── Forfeit ──────────────────────────────────────────────────────────────
    const [showForfeitConfirm, setShowForfeitConfirm] = useState(false);
    const [forfeiting,         setForfeiting]         = useState(false);
    const handleForfeit = async () => {
        if (forfeiting) return;
        setForfeiting(true);
        try {
            await forfeit(matchId);
            // Re-read the match so the result modal renders even if the
            // SSE stream missed the match_finished event (e.g. browser
            // backgrounded the tab during the close).
            try {
                const fresh = await getMatch(matchId);
                setMatch(fresh);
            } catch { /* ignore — modal will fire on next SSE tick */ }
        } catch (err) {
            const status = err?.response?.status;
            if (status === 409) {
                // Already finished — re-read so the modal still shows.
                try {
                    const fresh = await getMatch(matchId);
                    setMatch(fresh);
                } catch { /* ignore */ }
            } else {
                console.warn('Forfeit failed', err);
            }
        } finally {
            setForfeiting(false);
            setShowForfeitConfirm(false);
        }
    };

    // ── Resolve "me" vs "opponent" ───────────────────────────────────────────
    // The hook's room_state populates userA/userB once the SSE handshake is
    // complete. Until then we use the initial GET response.
    const userA = stream?.userA ?? (match
        ? { id: match.userAId, username: match.userAUsername }
        : null);
    const userB = stream?.userB ?? (match
        ? { id: match.userBId, username: match.userBUsername }
        : null);

    let me = null, opponent = null;
    if (userA && userB && selfUserId != null) {
        if (Number(userA.id) === Number(selfUserId))      { me = userA; opponent = userB; }
        else if (Number(userB.id) === Number(selfUserId)) { me = userB; opponent = userA; }
    }

    // ── "Not a participant" empty state — do NOT open SSE in this branch. ───
    // The hook is still mounted but it short-circuits on 403 from the
    // ticket endpoint, so no stream is established.
    const notParticipant =
        !participantOk || stream?.error === 'NOT_A_PARTICIPANT';

    // ── Result modal ─────────────────────────────────────────────────────────
    // Shows the modal when EITHER the SSE stream has flipped status to FINISHED
    // OR the initial GET response already returned a FINISHED match (late
    // joiner — the user opened this URL after the match already ended).
    const result = useMemo(() => {
        const finished = stream?.status === 'FINISHED' || match?.status === 'FINISHED';
        if (!finished) return null;
        const outcome = stream?.outcome ?? match?.outcome;
        const winnerUserId = stream?.winnerUserId ?? match?.winnerUserId;
        return resultForViewer(outcome, winnerUserId, selfUserId);
    }, [stream?.status, stream?.outcome, stream?.winnerUserId,
        match?.status, match?.outcome, match?.winnerUserId, selfUserId]);

    // ── Layout / drag (split-pane) ───────────────────────────────────────────
    const [leftWidth, setLeftWidth] = useState(60); // %
    const [isDragging, setIsDragging] = useState(false);
    const dragStartX = useRef(0);
    const dragStartW = useRef(0);
    const onDividerDown = useCallback((e) => {
        e.preventDefault();
        setIsDragging(true);
        dragStartX.current = e.clientX;
        dragStartW.current = leftWidth;
    }, [leftWidth]);
    useEffect(() => {
        if (!isDragging) return undefined;
        const onMove = (e) => {
            const container = document.getElementById('duel-arena-workspace');
            if (!container) return;
            const totalW = container.getBoundingClientRect().width;
            const delta  = e.clientX - dragStartX.current;
            const next   = Math.min(75, Math.max(35, dragStartW.current + (delta / totalW) * 100));
            setLeftWidth(next);
        };
        const onUp = () => setIsDragging(false);
        window.addEventListener('mousemove', onMove);
        window.addEventListener('mouseup',   onUp);
        return () => {
            window.removeEventListener('mousemove', onMove);
            window.removeEventListener('mouseup',   onUp);
        };
    }, [isDragging]);

    // ── Branch: not a participant ────────────────────────────────────────────
    if (notParticipant) {
        return (
            <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                minHeight: '100vh', backgroundColor: C.bg, padding: '2rem',
            }}>
                <div style={{
                    border: `1px solid ${C.border}`, padding: '3rem',
                    maxWidth: '480px', width: '100%', textAlign: 'center',
                    backgroundColor: C.surfaceLow,
                }}>
                    <h1 style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: '28px', color: C.error, marginBottom: '1rem',
                    }}>Not a Participant</h1>
                    <p style={{
                        fontFamily: "'Geist', sans-serif", fontSize: '15px',
                        color: C.outline, marginBottom: '2rem', lineHeight: 1.6,
                    }}>
                        You are not a participant of this match.
                    </p>
                    <button
                        onClick={() => navigate('/duel')}
                        style={{
                            padding: '10px 24px',
                            border: `1px solid ${C.secondary}`,
                            color: C.secondary, backgroundColor: 'transparent',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px', letterSpacing: '0.1em',
                            textTransform: 'uppercase', cursor: 'pointer',
                        }}
                    >
                        Back to Lobby
                    </button>
                </div>
            </div>
        );
    }

    // ── Branch: still loading the initial match details ──────────────────────
    if (matchLoading) {
        return (
            <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                minHeight: '100vh', backgroundColor: C.bg, color: C.outline,
                fontFamily: "'JetBrains Mono', monospace", fontSize: '13px',
            }}>
                Loading Arena...
            </div>
        );
    }

    if (matchLoadErr || !match) {
        return (
            <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                minHeight: '100vh', backgroundColor: C.bg, padding: '2rem',
            }}>
                <div style={{
                    border: `1px solid ${C.border}`, padding: '3rem',
                    maxWidth: '480px', width: '100%', textAlign: 'center',
                    backgroundColor: C.surfaceLow,
                }}>
                    <h1 style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: '24px', color: C.error, marginBottom: '1rem',
                    }}>Match Unavailable</h1>
                    <p style={{
                        fontFamily: "'Geist', sans-serif", fontSize: '14px',
                        color: C.outline, marginBottom: '2rem',
                    }}>
                        {matchLoadErr ?? 'Unknown error'}
                    </p>
                    <button
                        onClick={() => navigate('/duel')}
                        style={{
                            padding: '10px 24px',
                            border: `1px solid ${C.secondary}`,
                            color: C.secondary, backgroundColor: 'transparent',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px', letterSpacing: '0.1em',
                            textTransform: 'uppercase', cursor: 'pointer',
                        }}
                    >
                        Back to Lobby
                    </button>
                </div>
            </div>
        );
    }

    const monacoLang = LANGS.find((l) => l.value === language)?.monaco ?? 'plaintext';
    const problem = match.problem ?? null; // controller may inline; otherwise null

    return (
        <div style={{
            backgroundColor: C.bg, color: C.onBg,
            height: '100vh', display: 'flex', flexDirection: 'column',
            overflow: 'hidden', fontFamily: "'Geist', sans-serif",
            userSelect: isDragging ? 'none' : 'auto',
        }}>
            {/* ── Top header ─────────────────────────────────────────────── */}
            <header style={{
                height: '56px', flexShrink: 0,
                borderBottom: `1px solid ${C.border}`,
                backgroundColor: C.surfaceMin,
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '0 24px', zIndex: 10,
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                    <span style={{
                        fontFamily: "'JetBrains Mono', monospace", fontSize: '11px',
                        color: C.outline, letterSpacing: '0.1em', textTransform: 'uppercase',
                    }}>
                        Duel Arena
                    </span>
                    <div style={{ width: '1px', height: '24px', backgroundColor: C.border }} />
                    <h1 style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: '18px', fontWeight: 600, color: C.primary,
                        margin: 0, maxWidth: '500px', overflow: 'hidden',
                        textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                    }}>
                        {problem?.title ?? `Problem #${match.problemId}`}
                    </h1>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                    <div style={{
                        display: 'flex', alignItems: 'center', gap: '8px',
                        border: `1px solid ${C.border}`, padding: '6px 12px',
                    }}>
                        <span className="material-symbols-outlined"
                            style={{ fontSize: '14px', color: C.outline }}>
                            schedule
                        </span>
                        <span style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '13px',
                            color: remaining != null && remaining < 60 ? C.error : C.secondary,
                            fontWeight: 600,
                        }}>
                            {fmtMmSs(remaining)}
                        </span>
                    </div>
                    <div style={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '11px', color: C.outline,
                        letterSpacing: '0.05em',
                    }}>
                        {stream?.connected ? 'live' : 'connecting…'}
                    </div>
                </div>
            </header>

            {/* ── Workspace (split pane) ─────────────────────────────────── */}
            <div id="duel-arena-workspace" style={{
                flex: 1, display: 'flex', overflow: 'hidden', position: 'relative',
            }}>
                {/* ── Left pane: editor + controls + history ────────────── */}
                <div style={{
                    width: `${leftWidth}%`, display: 'flex', flexDirection: 'column',
                    backgroundColor: C.surfaceLow, overflow: 'hidden',
                }}>
                    {/* Editor toolbar */}
                    <div style={{
                        height: '44px', flexShrink: 0,
                        borderBottom: `1px solid ${C.border}`,
                        backgroundColor: C.surfaceMin,
                        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                        padding: '0 16px',
                    }}>
                        <select
                            value={language}
                            onChange={(e) => onLanguageChange(e.target.value)}
                            style={{
                                backgroundColor: C.surfaceCon, color: C.onBg,
                                border: `1px solid ${C.border}`,
                                padding: '6px 10px',
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', cursor: 'pointer', outline: 'none',
                            }}
                        >
                            {LANGS.map((l) => (
                                <option key={l.value} value={l.value}>{l.label}</option>
                            ))}
                        </select>

                        <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                                onClick={() => setShowForfeitConfirm(true)}
                                disabled={isMatchFinished || forfeiting}
                                style={{
                                    padding: '6px 14px',
                                    border: `1px solid ${C.error}`,
                                    color: C.error, backgroundColor: 'transparent',
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '11px', letterSpacing: '0.1em',
                                    textTransform: 'uppercase',
                                    cursor: isMatchFinished ? 'not-allowed' : 'pointer',
                                    opacity: isMatchFinished ? 0.4 : 1,
                                }}
                            >
                                Forfeit
                            </button>
                            <button
                                onClick={handleSubmit}
                                disabled={submitting || isMatchFinished}
                                style={{
                                    padding: '6px 18px',
                                    border: `1px solid ${C.secondary}`,
                                    color: C.bg, backgroundColor: C.secondary,
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '11px', letterSpacing: '0.1em',
                                    textTransform: 'uppercase',
                                    cursor: (submitting || isMatchFinished)
                                        ? 'not-allowed' : 'pointer',
                                    fontWeight: 600,
                                    opacity: (submitting || isMatchFinished) ? 0.5 : 1,
                                }}
                            >
                                {submitting ? 'Submitting…' : 'Submit'}
                            </button>
                        </div>
                    </div>

                    {/* Editor */}
                    <div style={{ flex: 1, minHeight: 0 }}>
                        <Editor
                            height="100%"
                            language={monacoLang}
                            theme="vs-dark"
                            value={code}
                            onChange={onCodeChange}
                            options={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: 13,
                                minimap: { enabled: false },
                                scrollBeyondLastLine: false,
                                automaticLayout: true,
                                padding: { top: 12, bottom: 12 },
                                wordWrap: 'on',
                                tabSize: 4,
                            }}
                        />
                    </div>

                    {/* Submission error banner */}
                    {submitErr && (
                        <div style={{
                            flexShrink: 0,
                            padding: '8px 16px',
                            borderTop: `1px solid ${C.error}`,
                            backgroundColor: 'rgba(255,180,171,0.08)',
                            color: C.error,
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px',
                        }}>
                            {submitErr}
                        </div>
                    )}

                    {/* Duel-scoped submission history */}
                    <div style={{
                        flexShrink: 0, height: '160px', overflowY: 'auto',
                        borderTop: `1px solid ${C.border}`,
                        backgroundColor: C.surfaceMin,
                    }}>
                        <div style={{
                            padding: '8px 16px',
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '10px', color: C.outline,
                            letterSpacing: '0.1em', textTransform: 'uppercase',
                            borderBottom: `1px solid ${C.borderThin}`,
                        }}>
                            Your submissions
                        </div>
                        {history.length === 0 ? (
                            <div style={{
                                padding: '14px 16px', color: C.outline,
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '11px', fontStyle: 'italic',
                            }}>
                                No submissions yet.
                            </div>
                        ) : (
                            history.slice().reverse().map((h) => {
                                const isAc = h.status === 'AC';
                                const isPending = h.status === 'PENDING' || h.status === 'JUDGING';
                                const tone = isAc ? C.success
                                    : isPending ? C.outline
                                    : C.error;
                                return (
                                    <div key={h.submissionId} style={{
                                        display: 'flex', alignItems: 'center',
                                        justifyContent: 'space-between',
                                        padding: '8px 16px',
                                        borderBottom: `1px solid ${C.borderThin}`,
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '11px',
                                    }}>
                                        <span style={{ color: C.muted }}>#{h.submissionId}</span>
                                        <span style={{ color: tone, fontWeight: 600 }}>
                                            {h.status}
                                            {h.total > 0 && ` ${h.passed}/${h.total}`}
                                        </span>
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>

                {/* ── Drag divider ─────────────────────────────────────── */}
                <div
                    onMouseDown={onDividerDown}
                    style={{
                        width: '4px', flexShrink: 0,
                        backgroundColor: isDragging ? C.secondary : C.border,
                        cursor: 'col-resize',
                        transition: isDragging ? 'none' : 'background-color 0.15s',
                    }}
                />

                {/* ── Right pane: opponent + problem ──────────────────── */}
                <div style={{
                    width: `${100 - leftWidth}%`,
                    display: 'flex', flexDirection: 'column',
                    backgroundColor: C.surfaceLow, overflow: 'hidden',
                }}>
                    {/* Opponent panel */}
                    <div style={{
                        flexShrink: 0, padding: '14px 18px',
                        borderBottom: `1px solid ${C.border}`,
                        backgroundColor: C.surfaceMin,
                        display: 'flex', flexDirection: 'column', gap: '10px',
                    }}>
                        <div style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '10px', color: C.outline,
                            letterSpacing: '0.1em', textTransform: 'uppercase',
                        }}>
                            Opponent
                        </div>
                        <div style={{
                            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                            gap: '12px',
                        }}>
                            <div style={{
                                display: 'flex', alignItems: 'center', gap: '10px',
                            }}>
                                <div style={{
                                    width: 32, height: 32,
                                    borderRadius: '50%',
                                    border: `1px solid ${C.border}`,
                                    backgroundColor: C.surfaceCon,
                                    color: C.primary,
                                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontSize: '12px', fontWeight: 600,
                                }}>
                                    {(opponent?.username || '?').slice(0, 2).toUpperCase()}
                                </div>
                                <div style={{ display: 'flex', flexDirection: 'column' }}>
                                    <span style={{
                                        fontFamily: "'Geist', sans-serif",
                                        fontSize: '14px', color: C.onBg, fontWeight: 600,
                                    }}>
                                        {opponent?.username ?? '—'}
                                    </span>
                                    <span style={{
                                        fontFamily: "'JetBrains Mono', monospace",
                                        fontSize: '10px', color: C.outline,
                                    }}>
                                        {me?.username ? `vs ${me.username}` : ''}
                                    </span>
                                </div>
                            </div>
                            <OpponentStatus
                                stream={stream}
                                opponentUserId={opponent?.id ?? null}
                            />
                        </div>
                    </div>

                    {/* Problem statement */}
                    <div style={{
                        flex: 1, overflowY: 'auto',
                        padding: '20px 22px',
                    }}>
                        <h2 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '20px', color: C.primary,
                            marginTop: 0, marginBottom: '14px',
                        }}>
                            {problem?.title ?? `Problem #${match.problemId}`}
                        </h2>

                        {problem?.description ? (
                            <pre style={{
                                fontFamily: "'Geist', sans-serif",
                                fontSize: '14px', color: C.onBg,
                                whiteSpace: 'pre-wrap', lineHeight: 1.7,
                                margin: 0,
                            }}>
                                {problem.description}
                            </pre>
                        ) : (
                            <p style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', color: C.outline, fontStyle: 'italic',
                            }}>
                                Problem statement is not available in this view.
                            </p>
                        )}

                        {problem?.inputFormat && (
                            <Section title="Input">
                                {problem.inputFormat}
                            </Section>
                        )}
                        {problem?.outputFormat && (
                            <Section title="Output">
                                {problem.outputFormat}
                            </Section>
                        )}
                        {problem?.constraints && (
                            <Section title="Constraints">
                                {problem.constraints}
                            </Section>
                        )}
                        {[problem?.example1, problem?.example2, problem?.example3]
                            .filter(Boolean)
                            .map((ex, i) => (
                                <Section key={i} title={`Example ${i + 1}`} mono>
                                    {ex}
                                </Section>
                            ))}
                    </div>
                </div>
            </div>

            {/* ── Forfeit confirmation modal ────────────────────────────── */}
            {showForfeitConfirm && (
                <Modal>
                    <h2 style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: '22px', color: C.error, margin: '0 0 12px 0',
                    }}>
                        Forfeit match?
                    </h2>
                    <p style={{
                        fontFamily: "'Geist', sans-serif", fontSize: '14px',
                        color: C.muted, lineHeight: 1.6, margin: '0 0 20px 0',
                    }}>
                        Your opponent will be awarded the win. This cannot be undone.
                    </p>
                    <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                        <button
                            onClick={() => setShowForfeitConfirm(false)}
                            disabled={forfeiting}
                            style={{
                                padding: '8px 18px',
                                border: `1px solid ${C.border}`,
                                color: C.outline, backgroundColor: 'transparent',
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '11px', letterSpacing: '0.1em',
                                textTransform: 'uppercase',
                                cursor: forfeiting ? 'not-allowed' : 'pointer',
                            }}
                        >
                            Cancel
                        </button>
                        <button
                            onClick={handleForfeit}
                            disabled={forfeiting}
                            style={{
                                padding: '8px 18px',
                                border: `1px solid ${C.error}`,
                                color: C.bg, backgroundColor: C.error,
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '11px', letterSpacing: '0.1em',
                                textTransform: 'uppercase', fontWeight: 600,
                                cursor: forfeiting ? 'not-allowed' : 'pointer',
                                opacity: forfeiting ? 0.6 : 1,
                            }}
                        >
                            {forfeiting ? 'Forfeiting…' : 'Confirm Forfeit'}
                        </button>
                    </div>
                </Modal>
            )}

            {/* ── Result modal (match_finished) ─────────────────────────── */}
            {result && (
                <Modal>
                    <div style={{ textAlign: 'center' }}>
                        <h2 style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '32px', color: result.tone,
                            margin: '0 0 12px 0',
                        }}>
                            {result.title}
                        </h2>
                        {stream?.winnerUsername && stream?.outcome !== 'DRAW' && (
                            <p style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '12px', color: C.outline,
                                margin: '0 0 8px 0', letterSpacing: '0.05em',
                            }}>
                                winner: {stream.winnerUsername}
                            </p>
                        )}
                        <p style={{
                            fontFamily: "'JetBrains Mono', monospace",
                            fontSize: '11px', color: C.outline,
                            margin: '0 0 24px 0', letterSpacing: '0.05em',
                            textTransform: 'uppercase',
                        }}>
                            {stream?.outcome ?? ''}
                        </p>
                        <button
                            onClick={() => navigate('/duel')}
                            style={{
                                padding: '10px 28px',
                                border: `1px solid ${C.secondary}`,
                                color: C.bg, backgroundColor: C.secondary,
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '11px', letterSpacing: '0.1em',
                                textTransform: 'uppercase', fontWeight: 600,
                                cursor: 'pointer',
                            }}
                        >
                            Return to Lobby
                        </button>
                    </div>
                </Modal>
            )}
        </div>
    );
};

// ── Helpers ─────────────────────────────────────────────────────────────────
const Section = ({ title, mono, children }) => (
    <div style={{ marginTop: '20px' }}>
        <div style={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '10px', color: C.outline,
            letterSpacing: '0.12em', textTransform: 'uppercase',
            marginBottom: '8px',
        }}>
            {title}
        </div>
        {mono ? (
            <pre style={{
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px', color: C.muted,
                backgroundColor: C.surfaceMin,
                border: `1px solid ${C.borderThin}`,
                padding: '10px 14px',
                whiteSpace: 'pre-wrap', overflowX: 'auto',
                margin: 0,
            }}>
                {children}
            </pre>
        ) : (
            <div style={{
                fontFamily: "'Geist', sans-serif", fontSize: '13px',
                color: C.muted, lineHeight: 1.7, whiteSpace: 'pre-wrap',
            }}>
                {children}
            </div>
        )}
    </div>
);

const Modal = ({ children }) => (
    <div style={{
        position: 'fixed', inset: 0, zIndex: 200,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        backgroundColor: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(6px)',
    }}>
        <div style={{
            backgroundColor: C.surfaceLow,
            border: `1px solid ${C.border}`,
            padding: '28px 32px',
            maxWidth: '440px', width: '100%',
            boxShadow: '0 20px 60px rgba(0,0,0,0.5)',
        }}>
            {children}
        </div>
    </div>
);

export default DuelArena;
