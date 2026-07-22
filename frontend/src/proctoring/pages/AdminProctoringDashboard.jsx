import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import proctoringApi from '../services/proctoringApi';
import RiskBadge from '../components/RiskBadge';
import { C } from '../constants';
import SkeletonLoader from '../../components/SkeletonLoader';

// ── About this page ─────────────────────────────────────────────────────────
//
// `/admin/proctoring` — single landing surface for admins to monitor all
// currently-live proctoring sessions (Req 15.1, 15.2, 15.5, 15.6).
//
// Live data path:
//   • The backend exposes the live list per-contest via
//     `GET /api/admin/proctoring/contests/{cid}/sessions` (Valkey-backed,
//     served by `ProctoringAdminController#liveSessions`). There is no
//     global "all contests" endpoint, so this page fans out at the edge:
//       - "All Contests" mode → fetch the proctored-contest list, then
//         call `adminLiveList({ contestId })` for each in parallel and
//         merge. Polled every 5 s.
//       - Specific contest selected → call `adminLiveList({ contestId })`
//         once and subscribe to that contest's SSE channel
//         (`/api/admin/proctoring/contests/{cid}/stream?ticket=…`),
//         refetching the list on any `RISK_BAND_CHANGED`,
//         `SESSION_STARTED`, or `SESSION_ENDED` frame.
//
// Auth: page-level `<AdminRoute>` gate in `App.jsx` redirects non-admins.
// Every REST call carries the JWT bearer header via the shared `api`
// axios instance, and the SSE handshake mints a single-use ticket via
// `adminMintSseTicket` (mirrors `useDuelStream`).
//
// Filters:
//   • Contest dropdown — server-side scoping when a single contest is
//     picked; client-side passthrough otherwise.
//   • Band chips (LOW / MEDIUM / HIGH) — client-side, multi-select.
//   • Sort by risk score descending by default.
//
// Click-through: each row navigates to
// `/admin/proctoring/sessions/:sessionId` (drill-down lands in 10.5).

// ── Constants ───────────────────────────────────────────────────────────────

const POLL_INTERVAL_MS = 5_000;
const ALL_CONTESTS = '__ALL__';
const BAND_OPTIONS = ['LOW', 'MEDIUM', 'HIGH'];
const PAGE_SIZE = 12;

const formatRelativeTime = (epochMs) => {
    if (!epochMs || typeof epochMs !== 'number') return '—';
    const delta = Math.max(0, Date.now() - epochMs);
    if (delta < 5_000) return 'just now';
    if (delta < 60_000) return `${Math.floor(delta / 1000)}s ago`;
    if (delta < 3_600_000) return `${Math.floor(delta / 60_000)}m ago`;
    if (delta < 86_400_000) return `${Math.floor(delta / 3_600_000)}h ago`;
    return `${Math.floor(delta / 86_400_000)}d ago`;
};

// Build the SSE URL for a per-contest admin stream — mirrors the
// duel SSE URL builder in `hooks/useDuelStream.js` so the auth pattern
// (single-use ticket in the query string, NOT the JWT) is identical.
const buildStreamUrl = (contestId, ticket) => {
    const base = import.meta.env.VITE_API_URL ?? '/api';
    return `${base}/admin/proctoring/contests/${contestId}/stream?ticket=${encodeURIComponent(ticket)}`;
};

// ── Styles ──────────────────────────────────────────────────────────────────

const styles = {
    root: {
        minHeight: '100vh',
        backgroundColor: C.bg,
        color: C.onBg,
        fontFamily: "'Geist', sans-serif",
        padding: '48px 64px',
    },
    hero: {
        marginBottom: '2.5rem',
        paddingBottom: '1.75rem',
        borderBottom: `1px solid ${C.border}`,
    },
    eyebrow: {
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '11px',
        letterSpacing: '0.2em',
        color: C.secondary,
        textTransform: 'uppercase',
    },
    title: {
        margin: '0.5rem 0 0.75rem',
        fontFamily: "'Playfair Display', serif",
        fontSize: 'clamp(32px, 4vw, 48px)',
        fontWeight: 700,
        color: C.primary,
        lineHeight: 1.1,
    },
    subtitle: {
        margin: 0,
        fontSize: '14px',
        color: C.muted,
    },
    controlsRow: {
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        gap: '14px',
        marginBottom: '1.5rem',
    },
    controlLabel: {
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '10px',
        letterSpacing: '0.15em',
        color: C.outline,
        textTransform: 'uppercase',
    },
    select: {
        padding: '8px 12px',
        backgroundColor: C.surfaceLow,
        color: C.onBg,
        border: `1px solid ${C.border}`,
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '12px',
        letterSpacing: '0.04em',
        cursor: 'pointer',
        minWidth: '220px',
    },
    chip: (active, color) => ({
        padding: '5px 12px',
        backgroundColor: active ? color : 'transparent',
        color: active ? C.bg : color,
        border: `1px solid ${color}`,
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '10px',
        letterSpacing: '0.12em',
        textTransform: 'uppercase',
        cursor: 'pointer',
        fontWeight: active ? 700 : 400,
        transition: 'all 120ms ease',
    }),
    flagToggle: (active) => ({
        padding: '5px 12px',
        backgroundColor: active ? C.error : 'transparent',
        color: active ? C.bg : C.error,
        border: `1px solid ${C.error}`,
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '10px',
        letterSpacing: '0.12em',
        textTransform: 'uppercase',
        cursor: 'pointer',
        fontWeight: active ? 700 : 400,
    }),
    sectionHeader: {
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        marginBottom: '1.25rem',
    },
    livePulse: {
        width: '6px',
        height: '6px',
        borderRadius: '50%',
        backgroundColor: C.success,
        display: 'inline-block',
        animation: 'apdPulse 1.5s infinite',
    },
    sectionLabel: {
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '11px',
        letterSpacing: '0.15em',
        color: C.outline,
        textTransform: 'uppercase',
    },
    countLabel: {
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '11px',
        color: C.outline,
    },
    grid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
        gap: '16px',
    },
    emptyBlock: {
        border: `1px solid ${C.border}`,
        padding: '3rem',
        textAlign: 'center',
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '12px',
        color: C.outline,
        letterSpacing: '0.08em',
    },
    errorBanner: {
        border: `1px solid ${C.error}`,
        backgroundColor: `${C.error}10`,
        padding: '12px 16px',
        marginBottom: '1.5rem',
        color: C.error,
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '12px',
    },
};

// ── Row card ────────────────────────────────────────────────────────────────

const SessionCard = ({ row, contestName, onOpen }) => {
    const [hovered, setHovered] = useState(false);
    const tone = row.riskBand === 'HIGH' ? C.error
        : row.riskBand === 'MEDIUM' ? C.warning
        : C.success;

    return (
        <div
            role="button"
            tabIndex={0}
            onClick={() => onOpen(row.sessionId)}
            onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    onOpen(row.sessionId);
                }
            }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                position: 'relative',
                border: `1px solid ${hovered ? tone : C.border}`,
                backgroundColor: hovered ? C.surfaceCon : C.surfaceLow,
                padding: '1.25rem 1.5rem',
                display: 'flex',
                flexDirection: 'column',
                gap: '12px',
                cursor: 'pointer',
                outline: 'none',
                transition: 'all 0.15s ease',
                overflow: 'hidden',
            }}
        >
            {/* Top accent bar — same anatomy as AdminDuelMonitor cards */}
            <div style={{
                position: 'absolute', top: 0, left: 0, right: 0, height: '2px',
                backgroundColor: tone, opacity: hovered ? 1 : 0.45,
                transition: 'opacity 0.15s ease',
            }} />

            {/* Row 1: contest pill + connection state */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                <span style={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '10px',
                    letterSpacing: '0.12em',
                    color: C.outline,
                    textTransform: 'uppercase',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    maxWidth: '70%',
                }}>
                    {contestName || `Contest #${row.contestId ?? '?'}`}
                </span>
                <span style={{
                    display: 'inline-flex', alignItems: 'center', gap: '6px',
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '10px',
                    letterSpacing: '0.12em',
                    color: row.connected ? C.success : C.outline,
                    textTransform: 'uppercase',
                }}>
                    <span style={{
                        width: '6px', height: '6px', borderRadius: '50%',
                        backgroundColor: row.connected ? C.success : C.outline,
                        display: 'inline-block',
                        animation: row.connected ? 'apdPulse 1.5s infinite' : 'none',
                    }} />
                    {row.connected ? 'Connected' : 'Ended'}
                </span>
            </div>

            {/* Row 2: username + session id */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                <span style={{
                    fontFamily: "'Playfair Display', serif",
                    fontSize: '20px', fontWeight: 600,
                    color: hovered ? C.primary : C.onBg,
                    transition: 'color 0.15s ease',
                }}>
                    {row.username || `User #${row.userId}`}
                </span>
                <span style={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '10px',
                    color: C.outline,
                    letterSpacing: '0.08em',
                }}>
                    Session #{row.sessionId}
                </span>
            </div>

            {/* Row 3: score + band + flagged */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                <RiskBadge band={row.riskBand} score={row.riskScore} />
                {row.flagged && (
                    <span style={{
                        padding: '2px 8px',
                        border: `1px solid ${C.error}`,
                        backgroundColor: `${C.error}15`,
                        color: C.error,
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '9px',
                        letterSpacing: '0.15em',
                        textTransform: 'uppercase',
                    }}>
                        Flagged
                    </span>
                )}
            </div>

            {/* Row 4: last event + open hint */}
            <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                gap: '8px',
                marginTop: '4px',
                paddingTop: '8px',
                borderTop: `1px solid ${C.border}`,
            }}>
                <span style={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '10px',
                    color: C.outline,
                    letterSpacing: '0.08em',
                }}>
                    Last event {formatRelativeTime(row.lastEventAtMs)}
                </span>
                <span style={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '10px',
                    color: hovered ? C.secondary : C.outline,
                    letterSpacing: '0.12em',
                    textTransform: 'uppercase',
                    transition: 'color 0.15s ease',
                }}>
                    Open →
                </span>
            </div>
        </div>
    );
};

// ── Pagination button ────────────────────────────────────────────────────────

function DashPagBtn({ label, onClick, disabled, active }) {
    const [hovered, setHovered] = useState(false);
    return (
        <button
            type="button"
            onClick={onClick}
            disabled={disabled}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                width: '34px', height: '34px',
                border: `1px solid ${active ? C.secondary : hovered && !disabled ? C.primary : C.border}`,
                backgroundColor: active ? C.secondary : hovered && !disabled ? C.surfaceCon : 'transparent',
                color: active ? C.bg : disabled ? C.border : hovered ? C.primary : C.outline,
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px',
                cursor: disabled ? 'default' : 'pointer',
                transition: 'all 0.15s',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
        >
            {label}
        </button>
    );
}

// ── Page ────────────────────────────────────────────────────────────────────

export default function AdminProctoringDashboard() {
    const navigate = useNavigate();

    // Master state: list of proctored contests (id → name) and rows.
    const [contests, setContests] = useState([]); // [{ id, name }]
    const [contestId, setContestId] = useState(ALL_CONTESTS);
    const [rows, setRows] = useState([]); // LiveSessionRow[] (with contestId stamped)
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Filters (client-side)
    const [bandFilter, setBandFilter] = useState(new Set()); // empty == all
    const [flaggedOnly, setFlaggedOnly] = useState(false);
    const [viewMode, setViewMode] = useState('LIVE'); // LIVE | ALL
    const [page, setPage] = useState(1);

    // SSE bookkeeping — only used when a single contest is selected.
    const esRef = useRef(null);
    // Re-tick on every interval / SSE event to refresh "last event" relative
    // strings even when the row data itself hasn't changed.
    const [tick, setTick] = useState(0);
    useEffect(() => {
        const t = setInterval(() => setTick((v) => v + 1), 15_000);
        return () => clearInterval(t);
    }, []);

    // ── Load proctored contests once ────────────────────────────────────
    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const res = await api.get('/admin/contests');
                if (cancelled) return;
                const all = Array.isArray(res?.data) ? res.data : [];
                const proctored = all
                    .filter((c) => c && c.proctored === true)
                    .map((c) => ({ id: c.id, name: c.name || `Contest #${c.id}` }));
                setContests(proctored);
            } catch (err) {
                if (cancelled) return;
                setError(
                    err?.response?.data?.message ||
                    'Failed to load contests. You may not have admin access.'
                );
            }
        })();
        return () => { cancelled = true; };
    }, []);

    // ── Fetch rows for the current scope ────────────────────────────────
    const fetchRows = useCallback(async () => {
        const isAllMode = viewMode === 'ALL';
        try {
            // Specific contest selected: single REST call.
            if (contestId !== ALL_CONTESTS) {
                const cid = Number(contestId);
                const res = await proctoringApi.adminLiveList({
                    contestId: cid,
                    status: isAllMode ? 'ALL' : undefined,
                });
                const list = Array.isArray(res?.data) ? res.data : [];
                setRows(list.map((r) => ({ ...r, contestId: cid })));
                setError(null);
                return;
            }

            // All contests: fan out across proctored contests.
            if (contests.length === 0) {
                setRows([]);
                setError(null);
                return;
            }
            const results = await Promise.allSettled(
                contests.map((c) =>
                    proctoringApi.adminLiveList({
                        contestId: c.id,
                        status: isAllMode ? 'ALL' : undefined,
                    })
                        .then((res) => ({ id: c.id, list: Array.isArray(res?.data) ? res.data : [] }))
                )
            );
            const merged = [];
            for (const r of results) {
                if (r.status !== 'fulfilled') continue;
                for (const row of r.value.list) {
                    merged.push({ ...row, contestId: r.value.id });
                }
            }
            setRows(merged);
            setError(null);
        } catch (err) {
            setError(
                err?.response?.data?.message ||
                'Failed to load sessions. Check your admin access and try again.'
            );
        } finally {
            setLoading(false);
        }
    }, [contestId, contests, viewMode]);

    // ── Polling (always on; SSE supplements when a contest is selected) ─
    useEffect(() => {
        let cancelled = false;
        (async () => {
            await fetchRows();
            if (cancelled) return;
        })();
        const iv = setInterval(() => {
            if (!cancelled) fetchRows();
        }, POLL_INTERVAL_MS);
        return () => {
            cancelled = true;
            clearInterval(iv);
        };
    }, [fetchRows]);

    // ── SSE for a single selected contest ───────────────────────────────
    useEffect(() => {
        // Tear down the previous stream on every dependency change.
        if (esRef.current) {
            esRef.current.close();
            esRef.current = null;
        }
        // SSE is only meaningful in LIVE mode — ended sessions don't change.
        if (contestId === ALL_CONTESTS || viewMode !== 'LIVE') return undefined;
        const cid = Number(contestId);
        let cancelled = false;
        let es = null;

        const open = async () => {
            try {
                const res = await proctoringApi.adminMintSseTicket(cid);
                const ticket = res?.data?.ticket;
                if (!ticket || cancelled) return;
                es = new EventSource(buildStreamUrl(cid, ticket));

                const refetch = () => {
                    if (cancelled) return;
                    setTick((v) => v + 1);
                    fetchRows();
                };
                // Every event the listener bridges (Req 15.1, 15.2, 12.5)
                // is a trigger to refresh the live-list for this contest.
                es.addEventListener('RISK_BAND_CHANGED', refetch);
                es.addEventListener('SESSION_STARTED', refetch);
                es.addEventListener('SESSION_ENDED', refetch);
                es.addEventListener('connected', () => { /* warm-up frame */ });
                es.onerror = () => {
                    // EventSource auto-reconnects on transient errors. We
                    // only act on a hard close (CLOSED) to avoid thrash.
                    if (es && es.readyState === EventSource.CLOSED) {
                        // Polling already provides the fallback path —
                        // surface a soft hint without breaking the page.
                        // eslint-disable-next-line no-console
                        console.warn('AdminProctoringDashboard: SSE closed, falling back to polling');
                    }
                };
                esRef.current = es;
            } catch (err) {
                // SSE failure is non-fatal — polling still drives updates.
                // eslint-disable-next-line no-console
                console.warn('AdminProctoringDashboard: SSE setup failed', err);
            }
        };

        open();
        return () => {
            cancelled = true;
            if (es) es.close();
            esRef.current = null;
        };
    }, [contestId, fetchRows, viewMode]);

    // ── Filtered + sorted view ──────────────────────────────────────────
    const visibleRows = useMemo(() => {
        let out = rows;
        if (bandFilter.size > 0) {
            out = out.filter((r) => bandFilter.has(r.riskBand));
        }
        if (flaggedOnly) {
            out = out.filter((r) => r.flagged === true);
        }
        return [...out].sort((a, b) => {
            // Risk score descending; flagged-first as a tiebreaker.
            const dScore = (b.riskScore ?? 0) - (a.riskScore ?? 0);
            if (dScore !== 0) return dScore;
            if (a.flagged !== b.flagged) return a.flagged ? -1 : 1;
            return (a.sessionId ?? 0) - (b.sessionId ?? 0);
        });
    }, [rows, bandFilter, flaggedOnly]);

    useEffect(() => { setPage(1); }, [bandFilter, flaggedOnly, contestId, viewMode]);

    const contestNameById = useMemo(() => {
        const m = new Map();
        for (const c of contests) m.set(c.id, c.name);
        return m;
    }, [contests]);

    const totalPages = Math.max(1, Math.ceil(visibleRows.length / PAGE_SIZE));
    const pageRows = visibleRows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

    const toggleBand = (band) => {
        setBandFilter((prev) => {
            const next = new Set(prev);
            if (next.has(band)) next.delete(band);
            else next.add(band);
            return next;
        });
    };

    const handleOpen = (sessionId) => {
        navigate(`/admin/proctoring/sessions/${sessionId}`);
    };

    // ── Render ──────────────────────────────────────────────────────────
    return (
        <div style={styles.root}>
            {/* Re-tick consumer — referenced once so eslint accepts the dep
                without flagging a no-op render. */}
            <span style={{ display: 'none' }} aria-hidden="true">{tick}</span>

            <section style={styles.hero}>
                <span style={styles.eyebrow}>Admin Panel</span>
                <h1 style={styles.title}>Proctoring Live View</h1>
                <p style={styles.subtitle}>
                    Live sessions across {contestId === ALL_CONTESTS
                        ? `${contests.length} proctored contest${contests.length === 1 ? '' : 's'}`
                        : 'the selected contest'}.
                    Updates push via SSE when a single contest is selected; otherwise polled every 5 seconds.
                </p>
            </section>

            {error && <div style={styles.errorBanner}>{error}</div>}

            <div style={styles.controlsRow}>
                <span style={styles.controlLabel}>Contest</span>
                <select
                    value={contestId}
                    onChange={(e) => setContestId(e.target.value)}
                    style={styles.select}
                >
                    <option value={ALL_CONTESTS}>All proctored contests</option>
                    {contests.map((c) => (
                        <option key={c.id} value={c.id}>
                            {c.name}
                        </option>
                    ))}
                </select>

                <span style={{ ...styles.controlLabel, marginLeft: '8px' }}>Band</span>
                {BAND_OPTIONS.map((band) => {
                    const color = band === 'HIGH' ? C.error
                        : band === 'MEDIUM' ? C.warning
                        : C.success;
                    return (
                        <button
                            key={band}
                            type="button"
                            onClick={() => toggleBand(band)}
                            style={styles.chip(bandFilter.has(band), color)}
                        >
                            {band.toLowerCase()}
                        </button>
                    );
                })}

                <button
                    type="button"
                    onClick={() => setFlaggedOnly((v) => !v)}
                    style={styles.flagToggle(flaggedOnly)}
                    title="Show only flagged sessions"
                >
                    Flagged Only
                </button>

                <span style={{ ...styles.controlLabel, marginLeft: '8px' }}>View</span>
                <button
                    type="button"
                    onClick={() => setViewMode(viewMode === 'LIVE' ? 'ALL' : 'LIVE')}
                    style={{
                        ...styles.chip(viewMode === 'ALL', C.primary),
                    }}
                    title={viewMode === 'LIVE' ? 'Show all sessions including ended' : 'Show only live sessions'}
                >
                    {viewMode === 'ALL' ? 'ALL' : 'LIVE'}
                </button>
            </div>

            <section>
                <div style={styles.sectionHeader}>
                    {viewMode === 'LIVE' && <span style={styles.livePulse} />}
                    <span style={styles.sectionLabel}>
                        {viewMode === 'LIVE' ? 'Live Sessions' : 'All Sessions'}
                    </span>
                    <span style={styles.countLabel}>
                        ({visibleRows.length}{visibleRows.length !== rows.length ? ` of ${rows.length}` : ''})
                    </span>
                </div>

                {loading ? (
                    <SkeletonLoader compact rows={2} />
                ) : visibleRows.length === 0 ? (
                    <div style={styles.emptyBlock}>
                        {rows.length === 0
                            ? 'No active proctoring sessions right now.'
                            : 'No sessions match the current filters.'}
                    </div>
                ) : (
                    <div style={styles.grid}>
                        {pageRows.map((row) => (
                            <SessionCard
                                key={row.sessionId}
                                row={row}
                                contestName={contestNameById.get(row.contestId)}
                                onOpen={handleOpen}
                            />
                        ))}
                    </div>
                )}
                {totalPages > 1 && (
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginTop: '2rem' }}>
                        <DashPagBtn label="←" onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1} />
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
                                    : <DashPagBtn key={item} label={item} active={item === page} onClick={() => setPage(item)} />
                            )
                        }
                        <DashPagBtn label="→" onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages} />
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, marginLeft: '12px', letterSpacing: '0.1em' }}>
                            {(page - 1) * PAGE_SIZE + 1}–{Math.min(page * PAGE_SIZE, visibleRows.length)} / {visibleRows.length}
                        </span>
                    </div>
                )}
            </section>

            <style>{`
                @keyframes apdPulse { 0%,100%{opacity:1} 50%{opacity:0.3} }
                @media (max-width: 768px) {
                    div[style*="padding: 48px 64px"] { padding: 24px 16px !important; }
                }
            `}</style>
        </div>
    );
}
