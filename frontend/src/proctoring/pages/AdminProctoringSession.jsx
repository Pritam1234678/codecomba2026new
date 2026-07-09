import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import proctoringApi from '../services/proctoringApi';
import { C } from '../constants';

// ─── AdminProctoringSession ─────────────────────────────────────────────────
//
// Admin-only drill-down for a single proctoring session (Req 15.3,
// 15.4, 15.6, 15.7, 19.4, 22.4). Renders:
//
//   • session metadata (user, contest, started/ended, end_reason,
//     score / band, event count, screenshot count, submission count)
//   • action bar — Force End (with confirmation), Send Warning (modal
//     prompts for free-form message), Rescore (idempotent recompute)
//   • paginated event log (50/page) in chronological order, generic
//     for any registered `event_type` per Req 22.4
//   • screenshot grid — each thumbnail is fetched as an authed Blob
//     via `proctoringApi.adminFetchScreenshotBlob` and rendered as a
//     same-origin object URL so JWT bearer auth flows naturally
//   • submissions list scoped to the session window
//
// For active sessions the page also subscribes to the contest-scoped
// admin SSE channel and hot-applies `RISK_BAND_CHANGED` /
// `SESSION_ENDED` updates without reloading the rest of the page. Live
// event appending is handled by polling the detail endpoint when an
// `EVENT_RECORDED`-style hint arrives — the SSE bus only carries
// session lifecycle frames, so a band change is the cheapest signal we
// have for "something happened, maybe refetch".
//
// The page reuses the Practice palette tokens already in
// `proctoring/constants.js` so it stays visually cohesive with the
// candidate surface.

const EVENTS_PAGE_SIZE = 50;

// ── Visual helpers ──────────────────────────────────────────────────────────

const BAND_COLOR = {
  LOW: C.success,
  MEDIUM: C.warning,
  HIGH: C.error,
};

const formatTimestamp = (raw) => {
  if (!raw) return '—';
  try {
    const d = new Date(raw);
    if (Number.isNaN(d.getTime())) return String(raw);
    return d.toLocaleString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return String(raw);
  }
};

const formatDuration = (startedAt, endedAt) => {
  if (!startedAt) return '—';
  const start = new Date(startedAt).getTime();
  const end = endedAt ? new Date(endedAt).getTime() : Date.now();
  if (Number.isNaN(start) || Number.isNaN(end) || end < start) return '—';
  const totalSeconds = Math.floor((end - start) / 1000);
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  if (h > 0) return `${h}h ${String(m).padStart(2, '0')}m`;
  if (m > 0) return `${m}m ${String(s).padStart(2, '0')}s`;
  return `${s}s`;
};

const safePayload = (raw) => {
  if (!raw) return '';
  try {
    const obj = JSON.parse(raw);
    return JSON.stringify(obj, null, 2);
  } catch {
    return String(raw);
  }
};

// ── Reusable inline components ──────────────────────────────────────────────

const RiskBadge = ({ band }) => {
  const color = BAND_COLOR[band] || C.outline;
  return (
    <span
      style={{
        padding: '4px 12px',
        border: `1px solid ${color}`,
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '10px',
        letterSpacing: '0.14em',
        color,
        textTransform: 'uppercase',
      }}
    >
      {band || 'LOW'}
    </span>
  );
};

const Pill = ({ label, color }) => (
  <span
    style={{
      padding: '3px 10px',
      border: `1px solid ${color || C.border}`,
      fontFamily: "'JetBrains Mono', monospace",
      fontSize: '10px',
      letterSpacing: '0.12em',
      color: color || C.outline,
      textTransform: 'uppercase',
    }}
  >
    {label}
  </span>
);

const SectionHeading = ({ eyebrow, title, right }) => (
  <div
    style={{
      display: 'flex',
      alignItems: 'baseline',
      justifyContent: 'space-between',
      marginBottom: '14px',
      gap: '12px',
      flexWrap: 'wrap',
    }}
  >
    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
      {eyebrow && (
        <span
          style={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '10px',
            letterSpacing: '0.2em',
            color: C.outline,
            textTransform: 'uppercase',
          }}
        >
          {eyebrow}
        </span>
      )}
      <h2
        style={{
          margin: 0,
          fontFamily: "'Playfair Display', serif",
          fontSize: '20px',
          fontWeight: 600,
          color: C.primary,
        }}
      >
        {title}
      </h2>
    </div>
    {right}
  </div>
);

const Card = ({ children, style }) => (
  <section
    style={{
      backgroundColor: C.surfaceLow,
      border: `1px solid ${C.border}`,
      padding: '24px 24px',
      ...style,
    }}
  >
    {children}
  </section>
);

const MetaRow = ({ label, children }) => (
  <div
    style={{
      display: 'grid',
      gridTemplateColumns: '160px 1fr',
      gap: '16px',
      padding: '8px 0',
      borderBottom: `1px solid ${C.border}33`,
    }}
  >
    <span
      style={{
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '10px',
        letterSpacing: '0.14em',
        color: C.outline,
        textTransform: 'uppercase',
      }}
    >
      {label}
    </span>
    <span
      style={{
        fontFamily: "'Geist', sans-serif",
        fontSize: '13px',
        color: C.muted,
        wordBreak: 'break-word',
      }}
    >
      {children}
    </span>
  </div>
);

// ── Action button ───────────────────────────────────────────────────────────

const ActionButton = ({ children, onClick, color, disabled, loading }) => {
  const [hovered, setHovered] = useState(false);
  const finalColor = disabled ? C.border : color || C.secondary;
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled || loading}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        padding: '10px 18px',
        border: `1px solid ${finalColor}`,
        backgroundColor: hovered && !disabled && !loading ? finalColor : 'transparent',
        color: hovered && !disabled && !loading ? C.bg : finalColor,
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: '11px',
        fontWeight: 600,
        letterSpacing: '0.14em',
        textTransform: 'uppercase',
        cursor: disabled || loading ? 'not-allowed' : 'pointer',
        transition: 'all 0.15s',
        opacity: disabled ? 0.5 : 1,
      }}
    >
      {loading ? 'Working…' : children}
    </button>
  );
};

// ── Confirmation modal ──────────────────────────────────────────────────────

const Modal = ({ open, title, children, onClose }) => {
  if (!open) return null;
  return (
    <div
      role="dialog"
      aria-modal="true"
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 1000,
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px',
      }}
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          backgroundColor: C.surfaceCon,
          border: `1px solid ${C.border}`,
          padding: '28px 28px',
          maxWidth: '520px',
          width: '100%',
          display: 'flex',
          flexDirection: 'column',
          gap: '14px',
        }}
      >
        <h3
          style={{
            margin: 0,
            fontFamily: "'Playfair Display', serif",
            fontSize: '22px',
            color: C.primary,
            fontWeight: 600,
          }}
        >
          {title}
        </h3>
        {children}
      </div>
    </div>
  );
};

// ── Screenshot thumbnail (auth-fetched blob URL) ────────────────────────────

const ScreenshotThumb = ({ shot, onOpen }) => {
  const [src, setSrc] = useState(null);
  const [errored, setErrored] = useState(false);
  const urlRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    setErrored(false);
    setSrc(null);
    proctoringApi
      .adminFetchScreenshotBlob(shot.sessionId, shot.id)
      .then((blob) => {
        if (cancelled) return;
        const url = URL.createObjectURL(blob);
        urlRef.current = url;
        setSrc(url);
      })
      .catch(() => {
        if (!cancelled) setErrored(true);
      });
    return () => {
      cancelled = true;
      if (urlRef.current) {
        try {
          URL.revokeObjectURL(urlRef.current);
        } catch {
          /* noop */
        }
        urlRef.current = null;
      }
    };
  }, [shot.sessionId, shot.id]);

  return (
    <button
      type="button"
      onClick={() => src && onOpen && onOpen(src, shot)}
      style={{
        position: 'relative',
        backgroundColor: C.surfaceHi,
        border: `1px solid ${C.border}`,
        padding: 0,
        cursor: src ? 'zoom-in' : 'default',
        overflow: 'hidden',
        aspectRatio: '4 / 3',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: C.outline,
      }}
      title={`Captured ${formatTimestamp(shot.capturedAt)}`}
    >
      {src ? (
        <img
          src={src}
          alt={`Screenshot ${shot.id}`}
          style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
        />
      ) : errored ? (
        <span
          style={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '10px',
            letterSpacing: '0.1em',
            color: C.error,
            textTransform: 'uppercase',
          }}
        >
          Unavailable
        </span>
      ) : (
        <span
          style={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '10px',
            letterSpacing: '0.1em',
            color: C.outline,
            textTransform: 'uppercase',
          }}
        >
          Loading…
        </span>
      )}
      <span
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          padding: '6px 8px',
          backgroundColor: 'rgba(0, 0, 0, 0.55)',
          fontFamily: "'JetBrains Mono', monospace",
          fontSize: '10px',
          color: C.muted,
          letterSpacing: '0.05em',
          textAlign: 'left',
          textTransform: 'none',
        }}
      >
        #{shot.id} · {formatTimestamp(shot.capturedAt)}
      </span>
    </button>
  );
};

// ── Page ────────────────────────────────────────────────────────────────────

export default function AdminProctoringSession() {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [eventPage, setEventPage] = useState(0);
  const [actionLoading, setActionLoading] = useState(null); // 'force-end' | 'warn' | 'rescore'
  const [forceEndOpen, setForceEndOpen] = useState(false);
  const [forceEndReason, setForceEndReason] = useState('');
  const [warnOpen, setWarnOpen] = useState(false);
  const [warnMessage, setWarnMessage] = useState('');
  const [actionMessage, setActionMessage] = useState(null); // { kind: 'success'|'error', text }
  const [previewSrc, setPreviewSrc] = useState(null);

  const sseRef = useRef(null);

  // ── Initial + manual reload ──────────────────────────────────────────────
  const loadDetail = useCallback(async () => {
    setError(null);
    try {
      const res = await proctoringApi.adminSessionDetail(sessionId);
      setDetail(res?.data ?? null);
      return res?.data ?? null;
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to load session';
      setError(msg);
      return null;
    }
  }, [sessionId]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    loadDetail().finally(() => {
      if (!cancelled) setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [loadDetail]);

  // ── Live SSE subscription (active sessions only) ─────────────────────────
  //
  // The admin SSE bus is keyed by `contestId` and only carries
  // `RISK_BAND_CHANGED`, `SESSION_STARTED`, and `SESSION_ENDED` frames
  // (see `ProctoringSseListener`). We subscribe once we know the
  // contestId for this session and the session is still active. Any
  // frame whose `sessionId` matches this drill-down triggers a refetch
  // so the score / band / event log stay current.
  const session = detail?.session;
  const contestId = session?.contestId;
  const isActive = !!(session && !session.endedAt);

  useEffect(() => {
    // Tear down any prior connection before deciding whether to open one.
    if (sseRef.current) {
      try {
        sseRef.current.close();
      } catch {
        /* noop */
      }
      sseRef.current = null;
    }
    if (!isActive || !contestId) return undefined;

    let cancelled = false;
    let es = null;

    (async () => {
      try {
        const ticketRes = await proctoringApi.adminMintSseTicket(contestId);
        const ticket = ticketRes?.data?.ticket;
        if (cancelled || !ticket) return;
        const base = import.meta.env.VITE_API_URL ?? '/api';
        const url = `${base}/admin/proctoring/contests/${contestId}/stream?ticket=${encodeURIComponent(ticket)}`;
        es = new EventSource(url);
        sseRef.current = es;

        const handleSessionFrame = (ev) => {
          try {
            const payload = JSON.parse(ev.data || '{}');
            if (Number(payload?.sessionId) !== Number(sessionId)) return;
            // Refetch for any frame relevant to this session — cheap
            // enough for an admin tool, and keeps us out of trying to
            // patch the events list ourselves (which would race the
            // server-side ordering).
            loadDetail();
          } catch {
            /* ignore malformed payloads */
          }
        };

        es.addEventListener('RISK_BAND_CHANGED', handleSessionFrame);
        es.addEventListener('SESSION_ENDED', handleSessionFrame);
        es.addEventListener('SESSION_STARTED', handleSessionFrame);
        es.onerror = () => {
          // EventSource auto-reconnects on transient drops. A hard
          // close (CLOSED) means the ticket was rejected — don't
          // thrash, just leave the stream down. The user can refresh
          // the page to mint a new ticket.
        };
      } catch {
        /* SSE is best-effort; the page still works without live updates */
      }
    })();

    return () => {
      cancelled = true;
      if (es) {
        try {
          es.close();
        } catch {
          /* noop */
        }
      }
      if (sseRef.current === es) sseRef.current = null;
    };
  }, [isActive, contestId, sessionId, loadDetail]);

  // ── Derived view-model ────────────────────────────────────────────────────
  const events = detail?.events ?? [];
  const screenshots = detail?.screenshots ?? [];
  const submissions = detail?.submissions ?? [];

  const totalEventPages = Math.max(1, Math.ceil(events.length / EVENTS_PAGE_SIZE));
  const clampedEventPage = Math.min(eventPage, totalEventPages - 1);
  const visibleEvents = useMemo(
    () =>
      events.slice(
        clampedEventPage * EVENTS_PAGE_SIZE,
        (clampedEventPage + 1) * EVENTS_PAGE_SIZE
      ),
    [events, clampedEventPage]
  );

  // ── Action handlers ──────────────────────────────────────────────────────
  const flashAction = (kind, text) => {
    setActionMessage({ kind, text });
    window.setTimeout(() => setActionMessage(null), 4000);
  };

  const handleForceEndConfirm = async () => {
    setActionLoading('force-end');
    try {
      await proctoringApi.adminForceEnd(sessionId, forceEndReason);
      setForceEndOpen(false);
      setForceEndReason('');
      flashAction('success', 'Session force-ended.');
      await loadDetail();
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Force end failed';
      flashAction('error', msg);
    } finally {
      setActionLoading(null);
    }
  };

  const handleWarnConfirm = async () => {
    if (!warnMessage.trim()) {
      flashAction('error', 'Warning message cannot be empty.');
      return;
    }
    setActionLoading('warn');
    try {
      await proctoringApi.adminWarn(sessionId, warnMessage.trim());
      setWarnOpen(false);
      setWarnMessage('');
      flashAction('success', 'Warning sent.');
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Warn failed';
      flashAction('error', msg);
    } finally {
      setActionLoading(null);
    }
  };

  const handleRescore = async () => {
    setActionLoading('rescore');
    try {
      await proctoringApi.adminRescore(sessionId);
      flashAction('success', 'Risk score recomputed.');
      await loadDetail();
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Rescore failed';
      flashAction('error', msg);
    } finally {
      setActionLoading(null);
    }
  };

  // ── Render: loading / error early returns ────────────────────────────────
  if (loading) {
    return (
      <div
        style={{
          minHeight: '60vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: C.outline,
          fontFamily: "'JetBrains Mono', monospace",
          fontSize: '13px',
        }}
      >
        Loading session…
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div
        style={{
          padding: '48px 64px',
          color: C.error,
          fontFamily: "'Geist', sans-serif",
          minHeight: '60vh',
        }}
      >
        <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
          {error || 'Session not found.'}
        </p>
        <button
          type="button"
          onClick={() => navigate(-1)}
          style={{
            marginTop: '12px',
            padding: '10px 18px',
            backgroundColor: 'transparent',
            border: `1px solid ${C.border}`,
            color: C.muted,
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '11px',
            letterSpacing: '0.12em',
            textTransform: 'uppercase',
            cursor: 'pointer',
          }}
        >
          ← Back
        </button>
      </div>
    );
  }

  // ── Render ───────────────────────────────────────────────────────────────
  return (
    <div
      style={{
        backgroundColor: C.bg,
        color: C.onBg,
        fontFamily: "'Geist', sans-serif",
        minHeight: '100vh',
        padding: '40px 56px',
      }}
    >
      {/* Hero */}
      <section
        style={{
          marginBottom: '2.5rem',
          borderBottom: `1px solid ${C.border}`,
          paddingBottom: '1.75rem',
          display: 'flex',
          flexDirection: 'column',
          gap: '12px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <span
            style={{
              fontFamily: "'JetBrains Mono', monospace",
              fontSize: '11px',
              letterSpacing: '0.2em',
              color: C.secondary,
              textTransform: 'uppercase',
            }}
          >
            Admin · Proctoring Session
          </span>
          <Pill label={isActive ? 'Active' : 'Ended'} color={isActive ? C.success : C.outline} />
          <RiskBadge band={session.riskBand} />
          {session.flagged && <Pill label="Flagged" color={C.error} />}
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: '14px', flexWrap: 'wrap' }}>
          <h1
            style={{
              margin: 0,
              fontFamily: "'Playfair Display', serif",
              fontSize: 'clamp(28px, 3.6vw, 40px)',
              fontWeight: 700,
              color: C.primary,
              letterSpacing: '-0.01em',
              lineHeight: 1.1,
            }}
          >
            #{session.id} · {session.username || `User ${session.userId}`}
          </h1>
          <span
            style={{
              fontFamily: "'JetBrains Mono', monospace",
              fontSize: '12px',
              color: C.outline,
              letterSpacing: '0.08em',
            }}
          >
            Contest #{session.contestId}
          </span>
        </div>
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
          <Link
            to={contestId ? `/admin/proctoring/contests/${contestId}` : '/admin/dashboard'}
            style={{
              padding: '8px 16px',
              border: `1px solid ${C.border}`,
              backgroundColor: 'transparent',
              color: C.muted,
              fontFamily: "'JetBrains Mono', monospace",
              fontSize: '11px',
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              textDecoration: 'none',
            }}
          >
            ← Dashboard
          </Link>
          <ActionButton
            color={C.error}
            disabled={!isActive}
            loading={actionLoading === 'force-end'}
            onClick={() => setForceEndOpen(true)}
          >
            Force End
          </ActionButton>
          <ActionButton
            color={C.warning}
            disabled={!isActive}
            loading={actionLoading === 'warn'}
            onClick={() => setWarnOpen(true)}
          >
            Send Warning
          </ActionButton>
          <ActionButton
            color={C.secondary}
            loading={actionLoading === 'rescore'}
            onClick={handleRescore}
          >
            Rescore
          </ActionButton>
        </div>
        {actionMessage && (
          <div
            role="status"
            style={{
              padding: '10px 14px',
              border: `1px solid ${actionMessage.kind === 'success' ? C.success : C.error}`,
              backgroundColor: C.surfaceLow,
              color: actionMessage.kind === 'success' ? C.success : C.error,
              fontFamily: "'JetBrains Mono', monospace",
              fontSize: '12px',
              letterSpacing: '0.05em',
            }}
          >
            {actionMessage.text}
          </div>
        )}
      </section>

      {/* Two-column body: metadata + counts on the left, content on the right */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(0, 320px) minmax(0, 1fr)',
          gap: '24px',
          alignItems: 'flex-start',
        }}
      >
        {/* Left rail: session metadata + counts */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <Card>
            <SectionHeading eyebrow="Session" title="Details" />
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              <MetaRow label="User">{session.username || `#${session.userId}`}</MetaRow>
              <MetaRow label="Contest">#{session.contestId}</MetaRow>
              <MetaRow label="Started">{formatTimestamp(session.startedAt)}</MetaRow>
              <MetaRow label="Ended">{formatTimestamp(session.endedAt)}</MetaRow>
              <MetaRow label="Duration">{formatDuration(session.startedAt, session.endedAt)}</MetaRow>
              <MetaRow label="End Reason">{session.endReason || (isActive ? '— (active)' : '—')}</MetaRow>
              <MetaRow label="Risk Score">
                {session.riskScore ?? 0}{' '}
                <span style={{ marginLeft: '8px' }}>
                  <RiskBadge band={session.riskBand} />
                </span>
              </MetaRow>
              <MetaRow label="Flagged">{session.flagged ? 'Yes' : 'No'}</MetaRow>
              <MetaRow label="Consent v.">{session.consentVersion ?? '—'}</MetaRow>
              <MetaRow label="Client IP">{session.clientIp || '—'}</MetaRow>
            </div>
          </Card>

          <Card>
            <SectionHeading eyebrow="Counts" title="Activity" />
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              <MetaRow label="Events">{events.length}</MetaRow>
              <MetaRow label="Screenshots">{screenshots.length}</MetaRow>
              <MetaRow label="Submissions">{submissions.length}</MetaRow>
            </div>
          </Card>
        </div>

        {/* Right column: events, screenshots, submissions */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', minWidth: 0 }}>
          {/* Events */}
          <Card>
            <SectionHeading
              eyebrow="Chronological"
              title="Event Log"
              right={
                <span
                  style={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '11px',
                    color: C.outline,
                    letterSpacing: '0.05em',
                  }}
                >
                  {events.length} total · page {clampedEventPage + 1}/{totalEventPages}
                </span>
              }
            />
            {events.length === 0 ? (
              <div
                style={{
                  padding: '32px',
                  textAlign: 'center',
                  color: C.outline,
                  fontFamily: "'JetBrains Mono', monospace",
                  fontSize: '12px',
                  border: `1px dashed ${C.border}`,
                }}
              >
                No events recorded for this session
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table
                  style={{
                    width: '100%',
                    borderCollapse: 'collapse',
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '11px',
                  }}
                >
                  <thead>
                    <tr style={{ borderBottom: `1px solid ${C.border}`, color: C.outline }}>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>#</th>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Type</th>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Server Time</th>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Client Time</th>
                      <th style={{ textAlign: 'right', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Δ</th>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Replayed</th>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Payload</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibleEvents.map((ev) => (
                      <tr key={ev.id} style={{ borderBottom: `1px solid ${C.border}33` }}>
                        <td style={{ padding: '8px 10px', color: C.outline }}>{ev.id}</td>
                        <td style={{ padding: '8px 10px', color: C.primary }}>{ev.eventType}</td>
                        <td style={{ padding: '8px 10px', color: C.muted }}>{formatTimestamp(ev.serverTimestamp)}</td>
                        <td style={{ padding: '8px 10px', color: C.muted }}>{formatTimestamp(ev.clientTimestamp)}</td>
                        <td
                          style={{
                            padding: '8px 10px',
                            textAlign: 'right',
                            color: (ev.scoreDelta || 0) > 0 ? C.warning : C.outline,
                          }}
                        >
                          {ev.scoreDelta == null ? '—' : ev.scoreDelta}
                        </td>
                        <td style={{ padding: '8px 10px', color: ev.replayed ? C.warning : C.outline }}>
                          {ev.replayed ? 'Yes' : 'No'}
                        </td>
                        <td
                          style={{
                            padding: '8px 10px',
                            color: C.outline,
                            maxWidth: '320px',
                            whiteSpace: 'pre-wrap',
                            wordBreak: 'break-word',
                          }}
                        >
                          {safePayload(ev.payloadJson) || '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {totalEventPages > 1 && (
              <div
                style={{
                  marginTop: '14px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                }}
              >
                <button
                  type="button"
                  onClick={() => setEventPage((p) => Math.max(0, p - 1))}
                  disabled={clampedEventPage === 0}
                  style={{
                    padding: '6px 14px',
                    backgroundColor: 'transparent',
                    border: `1px solid ${clampedEventPage === 0 ? C.border : C.outline}`,
                    color: clampedEventPage === 0 ? C.border : C.outline,
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '11px',
                    letterSpacing: '0.1em',
                    textTransform: 'uppercase',
                    cursor: clampedEventPage === 0 ? 'not-allowed' : 'pointer',
                  }}
                >
                  Prev
                </button>
                <span
                  style={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '11px',
                    color: C.outline,
                    padding: '0 10px',
                  }}
                >
                  {clampedEventPage + 1} / {totalEventPages}
                </span>
                <button
                  type="button"
                  onClick={() => setEventPage((p) => Math.min(totalEventPages - 1, p + 1))}
                  disabled={clampedEventPage >= totalEventPages - 1}
                  style={{
                    padding: '6px 14px',
                    backgroundColor: 'transparent',
                    border: `1px solid ${clampedEventPage >= totalEventPages - 1 ? C.border : C.outline}`,
                    color: clampedEventPage >= totalEventPages - 1 ? C.border : C.outline,
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '11px',
                    letterSpacing: '0.1em',
                    textTransform: 'uppercase',
                    cursor:
                      clampedEventPage >= totalEventPages - 1 ? 'not-allowed' : 'pointer',
                  }}
                >
                  Next
                </button>
              </div>
            )}
          </Card>

          {/* Screenshots */}
          <Card>
            <SectionHeading
              eyebrow="Captures"
              title="Screenshot Gallery"
              right={
                <span
                  style={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '11px',
                    color: C.outline,
                    letterSpacing: '0.05em',
                  }}
                >
                  {screenshots.length} total
                </span>
              }
            />
            {screenshots.length === 0 ? (
              <div
                style={{
                  padding: '32px',
                  textAlign: 'center',
                  color: C.outline,
                  fontFamily: "'JetBrains Mono', monospace",
                  fontSize: '12px',
                  border: `1px dashed ${C.border}`,
                }}
              >
                No screenshots captured for this session
              </div>
            ) : (
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
                  gap: '12px',
                }}
              >
                {screenshots.map((shot) => (
                  <ScreenshotThumb key={shot.id} shot={shot} onOpen={(src) => setPreviewSrc(src)} />
                ))}
              </div>
            )}
          </Card>

          {/* Submissions */}
          <Card>
            <SectionHeading
              eyebrow="In window"
              title="Submissions"
              right={
                <span
                  style={{
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '11px',
                    color: C.outline,
                    letterSpacing: '0.05em',
                  }}
                >
                  {submissions.length} total
                </span>
              }
            />
            {submissions.length === 0 ? (
              <div
                style={{
                  padding: '32px',
                  textAlign: 'center',
                  color: C.outline,
                  fontFamily: "'JetBrains Mono', monospace",
                  fontSize: '12px',
                  border: `1px dashed ${C.border}`,
                }}
              >
                No submissions during this session window
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table
                  style={{
                    width: '100%',
                    borderCollapse: 'collapse',
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '11px',
                  }}
                >
                  <thead>
                    <tr style={{ borderBottom: `1px solid ${C.border}`, color: C.outline }}>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>#</th>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Problem</th>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Lang</th>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Status</th>
                      <th style={{ textAlign: 'right', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Score</th>
                      <th style={{ textAlign: 'right', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Tests</th>
                      <th style={{ textAlign: 'left', padding: '8px 10px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Submitted</th>
                    </tr>
                  </thead>
                  <tbody>
                    {submissions.map((s) => (
                      <tr key={s.id} style={{ borderBottom: `1px solid ${C.border}33` }}>
                        <td style={{ padding: '8px 10px', color: C.outline }}>{s.id}</td>
                        <td style={{ padding: '8px 10px', color: C.primary }}>
                          {s.problemName || `#${s.problemId}`}
                        </td>
                        <td style={{ padding: '8px 10px', color: C.muted }}>{s.language || '—'}</td>
                        <td style={{ padding: '8px 10px', color: C.muted }}>{s.status || '—'}</td>
                        <td style={{ padding: '8px 10px', textAlign: 'right', color: C.muted }}>
                          {s.score ?? '—'}
                        </td>
                        <td style={{ padding: '8px 10px', textAlign: 'right', color: C.muted }}>
                          {s.testCasesPassed ?? 0}/{s.totalTestCases ?? 0}
                        </td>
                        <td style={{ padding: '8px 10px', color: C.outline }}>
                          {formatTimestamp(s.submittedAt)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </div>
      </div>

      {/* Force End modal — confirmation per task contract */}
      <Modal
        open={forceEndOpen}
        title="Force end this session?"
        onClose={() => (actionLoading === 'force-end' ? null : setForceEndOpen(false))}
      >
        <p style={{ margin: 0, color: C.muted, fontSize: '13px', lineHeight: 1.5 }}>
          The candidate will be disconnected immediately. They will be locked
          out of this contest with end reason <strong>ADMIN_FORCED</strong>.
          This cannot be undone.
        </p>
        <label
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '6px',
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '10px',
            letterSpacing: '0.14em',
            color: C.outline,
            textTransform: 'uppercase',
          }}
        >
          Reason (audit trail)
          <textarea
            value={forceEndReason}
            onChange={(e) => setForceEndReason(e.target.value)}
            rows={3}
            placeholder="e.g. Multiple faces detected, copy-paste from external source"
            style={{
              backgroundColor: C.bg,
              border: `1px solid ${C.border}`,
              color: C.onBg,
              padding: '10px 12px',
              fontFamily: "'Geist', sans-serif",
              fontSize: '13px',
              resize: 'vertical',
              outline: 'none',
            }}
          />
        </label>
        <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '4px' }}>
          <ActionButton
            color={C.outline}
            disabled={actionLoading === 'force-end'}
            onClick={() => setForceEndOpen(false)}
          >
            Cancel
          </ActionButton>
          <ActionButton
            color={C.error}
            loading={actionLoading === 'force-end'}
            onClick={handleForceEndConfirm}
          >
            Confirm Force End
          </ActionButton>
        </div>
      </Modal>

      {/* Warn modal */}
      <Modal
        open={warnOpen}
        title="Send a warning"
        onClose={() => (actionLoading === 'warn' ? null : setWarnOpen(false))}
      >
        <p style={{ margin: 0, color: C.muted, fontSize: '13px', lineHeight: 1.5 }}>
          The candidate will see a non-blocking warning toast. The session
          stays active. The message is also recorded in the admin audit log.
        </p>
        <label
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '6px',
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '10px',
            letterSpacing: '0.14em',
            color: C.outline,
            textTransform: 'uppercase',
          }}
        >
          Message
          <textarea
            value={warnMessage}
            onChange={(e) => setWarnMessage(e.target.value)}
            rows={3}
            placeholder="e.g. Please keep your face centered in the camera frame."
            style={{
              backgroundColor: C.bg,
              border: `1px solid ${C.border}`,
              color: C.onBg,
              padding: '10px 12px',
              fontFamily: "'Geist', sans-serif",
              fontSize: '13px',
              resize: 'vertical',
              outline: 'none',
            }}
          />
        </label>
        <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '4px' }}>
          <ActionButton
            color={C.outline}
            disabled={actionLoading === 'warn'}
            onClick={() => setWarnOpen(false)}
          >
            Cancel
          </ActionButton>
          <ActionButton
            color={C.warning}
            loading={actionLoading === 'warn'}
            onClick={handleWarnConfirm}
          >
            Send Warning
          </ActionButton>
        </div>
      </Modal>

      {/* Screenshot preview overlay */}
      {previewSrc && (
        <div
          role="dialog"
          aria-modal="true"
          onClick={() => { URL.revokeObjectURL(previewSrc); setPreviewSrc(null); }}
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 1100,
            backgroundColor: 'rgba(0, 0, 0, 0.85)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '32px',
            cursor: 'zoom-out',
          }}
        >
          <img
            src={previewSrc}
            alt="Screenshot preview"
            style={{ maxWidth: '90vw', maxHeight: '90vh', border: `1px solid ${C.border}` }}
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </div>
  );
}
