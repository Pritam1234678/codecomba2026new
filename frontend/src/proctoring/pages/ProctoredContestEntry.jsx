import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api from '../../services/api';
import useResponsive from '../../hooks/useResponsive';
import proctoringApi from '../services/proctoringApi';
import { C, MIN_DESKTOP_WIDTH } from '../constants';
import ConsentDialog from '../components/ConsentDialog';
import PreflightCheck from '../components/PreflightCheck';

// ── Phases ────────────────────────────────────────────────────────────────────
//
// Local state machine for the entry shell. The server-side eligibility
// response decides which phase we land in on first paint:
//   eligibility   → loading → resolves to one of the next phases
//   contest-ended, not-registered, locked, not-proctored → terminal-ish info screens
//   consent       → renders <ConsentDialog />
//   preflight     → renders <PreflightCheck />
//   creating      → POST /sessions in flight
//   ready         → all green, candidate clicks "Enter Contest" (Req 3.5
//                   requires the fullscreen request to come from a user
//                   gesture so we cannot auto-navigate)
//   error         → fatal error path with retry
const PHASE = {
  LOADING:        'loading',
  NOT_REGISTERED: 'not-registered',
  NOT_PROCTORED:  'not-proctored',
  CONTEST_ENDED:  'contest-ended',
  LOCKED:         'locked',
  CONSENT:        'consent',
  PREFLIGHT:      'preflight',
  CREATING:       'creating',
  READY:          'ready',
  RESUME:         'resume',         // active session found — offer Resume button
  RESUME_LIMIT:   'resume-limit',   // resume cap reached — show support message
  ERROR:          'error',
};

// Map server-side `lockReason` to candidate-friendly copy. Mirrors the
// `EndReason` enum on the backend — values come from
// `proctoring_sessions.end_reason`.
const LOCK_COPY = {
  ADMIN_FORCED:
    'A proctor ended your previous session for this contest. You can ' +
    'no longer enter this contest. If you believe this is a mistake, ' +
    'contact support.',
  SELF_QUIT:
    'You quit your previous attempt at this contest. Quitting is final ' +
    'for this contest — you cannot re-enter.',
  HEARTBEAT_TIMEOUT:
    'Your previous session ended because the proctoring connection was ' +
    'lost for too long. Re-entry is not allowed.',
  CONTEST_ENDED:
    'This contest has ended. Re-entry is not possible.',
};

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = {
  root: {
    minHeight: 'calc(100vh - 0px)',
    backgroundColor: C.bg,
    color: C.onBg,
    fontFamily: "'Geist', sans-serif",
  },
  shell: {
    maxWidth: '1080px',
    margin: '0 auto',
    padding: '40px 24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
  },
  hero: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    paddingBottom: '24px',
    borderBottom: `1px solid ${C.border}`,
  },
  heroBadgeRow: {
    display: 'flex',
    gap: '10px',
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  proctoredBadge: {
    padding: '4px 12px',
    border: `1px solid ${C.secondary}`,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '10px',
    letterSpacing: '0.15em',
    color: C.secondary,
    textTransform: 'uppercase',
  },
  registeredBadge: {
    padding: '4px 12px',
    border: `1px solid ${C.border}`,
    backgroundColor: C.surfaceLow,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '10px',
    letterSpacing: '0.15em',
    color: C.muted,
    textTransform: 'uppercase',
  },
  heroTitle: {
    margin: 0,
    fontFamily: "'Playfair Display', serif",
    fontSize: 'clamp(34px, 4vw, 48px)',
    fontWeight: 700,
    color: C.primary,
    letterSpacing: '-0.02em',
    lineHeight: 1.1,
  },
  heroSub: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '15px',
    color: C.outline,
    lineHeight: 1.5,
    maxWidth: '720px',
  },
  card: {
    backgroundColor: C.surfaceLow,
    border: `1px solid ${C.border}`,
    padding: '32px 28px',
    display: 'flex',
    flexDirection: 'column',
    gap: '14px',
  },
  cardEyebrow: {
    margin: 0,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '10px',
    letterSpacing: '0.2em',
    color: C.outline,
    textTransform: 'uppercase',
  },
  cardHeading: {
    margin: 0,
    fontFamily: "'Playfair Display', serif",
    fontSize: '24px',
    fontWeight: 600,
    color: C.primary,
  },
  cardBody: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '14px',
    lineHeight: 1.55,
    color: C.muted,
  },
  primaryButton: (disabled) => ({
    alignSelf: 'flex-start',
    padding: '14px 28px',
    border: `1px solid ${C.secondary}`,
    backgroundColor: disabled ? 'transparent' : C.secondary,
    color: disabled ? C.outline : C.bg,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    fontWeight: 600,
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: disabled ? 'not-allowed' : 'pointer',
  }),
  ghostButton: {
    alignSelf: 'flex-start',
    padding: '12px 24px',
    border: `1px solid ${C.border}`,
    backgroundColor: 'transparent',
    color: C.muted,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: 'pointer',
    textDecoration: 'none',
    display: 'inline-flex',
    alignItems: 'center',
    gap: '8px',
  },
  errorText: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '14px',
    color: C.error,
    lineHeight: 1.5,
  },
  centered: {
    textAlign: 'center',
    padding: '4rem 1rem',
  },
};

// ── Tiny helpers ──────────────────────────────────────────────────────────────

const StatusLine = ({ children }) => (
  <p
    style={{
      margin: 0,
      fontFamily: "'JetBrains Mono', monospace",
      fontSize: '13px',
      color: C.outline,
      letterSpacing: '0.05em',
    }}
  >
    {children}
  </p>
);

// ── Page ──────────────────────────────────────────────────────────────────────

/**
 * Candidate entry shell for a proctored contest.
 *
 * Phase flow per Req 2 / 3 / 13.9:
 *   1. GET /eligibility → decide which screen to render
 *   2. ConsentDialog → POST /consent → advance to preflight
 *   3. PreflightCheck → on success → POST /sessions → advance to ready
 *   4. "Enter Contest" button (user gesture, Req 3.5) → navigate to
 *      /contests/:id/proctored/arena
 *
 * The lockout terminal screen (Req 13.9, 24.6) is shown directly
 * without consent if the eligibility response reports `locked: true`.
 *
 * Viewport-gated to ≥ 1024 px per Req 23.9 — narrower viewports get a
 * "Desktop required" message before any camera permission is requested.
 */
export default function ProctoredContestEntry() {
  const { contestId } = useParams();
  const id = contestId;
  const navigate = useNavigate();
  const { width } = useResponsive();
  const isDesktop = width >= MIN_DESKTOP_WIDTH;

  const [phase, setPhase] = useState(PHASE.LOADING);
  const [error, setError] = useState(null);
  const [contestName, setContestName] = useState('');
  const [eligibility, setEligibility] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [sessionInfo, setSessionInfo] = useState(null);
  // Holds { resumeCount, maxResumes } when an active session is detected so the
  // Resume screen can show "you have N resumes left".
  const [resumeInfo, setResumeInfo] = useState(null);

  // Initial load: eligibility + contest name.
  const loadEligibility = useCallback(async () => {
    setPhase(PHASE.LOADING);
    setError(null);
    try {
      // Fetch the contest in parallel with eligibility so the entry
      // hero can show the contest name even before eligibility lands.
      const [contestRes, eligRes] = await Promise.all([
        api.get(`/contests/${id}`).catch(() => null),
        proctoringApi.eligibility(id),
      ]);
      const contest = contestRes?.data ?? null;
      const elig = eligRes?.data ?? null;
      if (contest?.name) setContestName(contest.name);
      setEligibility(elig);

      if (!elig) {
        setError('Could not check eligibility for this contest.');
        setPhase(PHASE.ERROR);
        return;
      }
      if (!elig.proctored) {
        setPhase(PHASE.NOT_PROCTORED);
        return;
      }
      if (elig.contestEnded) {
        setPhase(PHASE.CONTEST_ENDED);
        return;
      }
      if (!elig.registered) {
        setPhase(PHASE.NOT_REGISTERED);
        return;
      }
      if (elig.locked) {
        setPhase(PHASE.LOCKED);
        return;
      }
      if (!elig.consentAccepted) {
        setPhase(PHASE.CONSENT);
        return;
      }
      setPhase(PHASE.PREFLIGHT);
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load contest entry.');
      setPhase(PHASE.ERROR);
    }
  }, [id]);

  useEffect(() => {
    loadEligibility();
  }, [loadEligibility]);

  // ── Consent submit ──────────────────────────────────────────────
  const handleAcceptConsent = async () => {
    if (!eligibility) return;
    setSubmitting(true);
    setError(null);
    try {
      await proctoringApi.consent(id, eligibility.consentVersion);
      setPhase(PHASE.PREFLIGHT);
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not record consent. Try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeclineConsent = () => {
    navigate(`/contests/${id}`);
  };

  // ── Preflight done → create session ─────────────────────────────
  const handlePreflightComplete = async () => {
    setPhase(PHASE.CREATING);
    setError(null);
    try {
      const res = await proctoringApi.createSession(id);
      setSessionInfo(res?.data ?? null);
      setPhase(PHASE.READY);
    } catch (err) {
      const data = err?.response?.data;
      // Server can report LOCKED_OUT here too (race between eligibility
      // and session create) — fall back to the locked terminal screen.
      if (data?.error === 'LOCKED_OUT') {
        setEligibility((prev) => ({
          ...(prev ?? {}),
          locked: true,
          lockReason: data.endReason || prev?.lockReason || null,
        }));
        setPhase(PHASE.LOCKED);
        return;
      }
      // An active session already exists (candidate refreshed / closed the
      // tab mid-contest). Offer Resume instead of a hard error — unless the
      // resume cap has already been reached, in which case route to support.
      if (data?.error === 'ALREADY_ACTIVE') {
        setResumeInfo({
          resumeCount: data.resumeCount ?? 0,
          maxResumes: data.maxResumes ?? 2,
        });
        setPhase(data.resumeLimitReached ? PHASE.RESUME_LIMIT : PHASE.RESUME);
        return;
      }
      setError(data?.message || 'Could not start the proctored session. Try again.');
      setPhase(PHASE.ERROR);
    }
  };

  // ── Resume an existing active session ───────────────────────────
  const handleResume = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const res = await proctoringApi.resumeSession(id);
      setSessionInfo(res?.data ?? null);
      setPhase(PHASE.READY);
    } catch (err) {
      const data = err?.response?.data;
      if (data?.error === 'RESUME_LIMIT_REACHED') {
        setResumeInfo((prev) => ({
          resumeCount: data.maxResumes ?? prev?.resumeCount ?? 2,
          maxResumes: data.maxResumes ?? prev?.maxResumes ?? 2,
        }));
        setPhase(PHASE.RESUME_LIMIT);
        return;
      }
      if (data?.error === 'LOCKED_OUT') {
        setEligibility((prev) => ({
          ...(prev ?? {}),
          locked: true,
          lockReason: data.endReason || prev?.lockReason || null,
        }));
        setPhase(PHASE.LOCKED);
        return;
      }
      if (data?.error === 'NO_ACTIVE_SESSION') {
        // Session was closed between detection and resume — restart the flow.
        loadEligibility();
        return;
      }
      setError(data?.message || 'Could not resume your session. Try again.');
      setPhase(PHASE.ERROR);
    } finally {
      setSubmitting(false);
    }
  };

  const handlePreflightCancel = () => {
    navigate(`/contests/${id}`);
  };

  // ── Share screen first (before start) ───────────────────────────
  const [screenShared, setScreenShared] = useState(false);
  const handleShareScreen = async () => {
    try {
      const screenStream = await navigator.mediaDevices.getDisplayMedia({
        video: { frameRate: { ideal: 5 } },
        audio: false,
      });
      window.__proctoringScreenStream = screenStream;
      setScreenShared(true);
    } catch {
      // Denied — non-fatal, Arena won't have screen screenshots.
    }
  };

  // ── Final user gesture → fullscreen + arena ──────────────────
  const handleEnterContest = async () => {
    const el = document.documentElement;
    const request = el.requestFullscreen
      || el.webkitRequestFullscreen
      || el.msRequestFullscreen;
    if (request) {
      try { await request.call(el); } catch { /* denied */ }
    }
    navigate(`/contests/${id}/proctored/arena`, {
      state: sessionInfo ? { session: sessionInfo } : undefined,
    });
  };

  // ── Renderers per phase ─────────────────────────────────────────

  if (!isDesktop) {
    return (
      <div style={styles.root}>
        <div style={styles.shell}>
          <div style={{ ...styles.card, ...styles.centered }}>
            <h2 style={styles.cardHeading}>Desktop Required</h2>
            <p style={styles.cardBody}>
              Proctored contests can only be taken on a desktop browser
              with a viewport of at least {MIN_DESKTOP_WIDTH}px wide.
              Please switch to a desktop or laptop and try again.
            </p>
            <Link to={`/contests/${id}`} style={styles.ghostButton}>
              ← Back to Contest
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const Hero = (
    <div style={styles.hero}>
      <div style={styles.heroBadgeRow}>
        <span style={styles.proctoredBadge}>Proctored</span>
        {eligibility?.registered && <span style={styles.registeredBadge}>Registered</span>}
      </div>
      <h1 style={styles.heroTitle}>{contestName || 'Proctored Contest'}</h1>
      <p style={styles.heroSub}>
        Before you start, we will record your consent and verify that
        your camera, browser, and connection are ready for proctoring.
        Nothing is recorded until you consent.
      </p>
    </div>
  );

  let body = null;
  if (phase === PHASE.LOADING) {
    body = (
      <div style={styles.card}>
        <StatusLine>Checking eligibility…</StatusLine>
      </div>
    );
  } else if (phase === PHASE.NOT_PROCTORED) {
    body = (
      <div style={styles.card}>
        <p style={styles.cardEyebrow}>Not a proctored contest</p>
        <h2 style={styles.cardHeading}>Standard Contest</h2>
        <p style={styles.cardBody}>
          This contest is not configured for proctoring. You can solve
          its problems directly from the contest page.
        </p>
        <Link to={`/contests/${id}`} style={styles.ghostButton}>
          ← Back to Contest
        </Link>
      </div>
    );
  } else if (phase === PHASE.NOT_REGISTERED) {
    body = (
      <div style={styles.card}>
        <p style={styles.cardEyebrow}>Registration required</p>
        <h2 style={styles.cardHeading}>You are not registered</h2>
        <p style={styles.cardBody}>
          You need to register for this contest before the proctoring
          flow can begin. Head back to the contest page and click
          Register.
        </p>
        <Link to={`/contests/${id}`} style={styles.ghostButton}>
          ← Register for Contest
        </Link>
      </div>
    );
  } else if (phase === PHASE.LOCKED) {
    const reason = eligibility?.lockReason || null;
    const copy = (reason && LOCK_COPY[reason]) || LOCK_COPY.ADMIN_FORCED;
    body = (
      <div style={styles.card}>
        <p style={styles.cardEyebrow}>Session terminated</p>
        <h2 style={styles.cardHeading}>You cannot re-enter this contest</h2>
        <p style={styles.cardBody}>{copy}</p>
        <Link to="/contests" style={styles.ghostButton}>
          ← All Contests
        </Link>
      </div>
    );
  } else if (phase === PHASE.CONTEST_ENDED) {
    body = (
      <div style={styles.card}>
        <p style={styles.cardEyebrow}>Contest finished</p>
        <h2 style={styles.cardHeading}>This contest has ended</h2>
        <p style={styles.cardBody}>
          The time window for this contest has closed. No further
          entries or submissions are being accepted.
        </p>
        <Link to="/contests" style={styles.ghostButton}>
          ← All Contests
        </Link>
      </div>
    );
  } else if (phase === PHASE.RESUME) {
    const used = resumeInfo?.resumeCount ?? 0;
    const max = resumeInfo?.maxResumes ?? 2;
    const left = Math.max(0, max - used);
    body = (
      <div style={styles.card}>
        <p style={styles.cardEyebrow}>Session interrupted</p>
        <h2 style={styles.cardHeading}>Resume your contest</h2>
        <p style={styles.cardBody}>
          We found your previous proctored session still running — looks
          like the page was refreshed or closed. You can rejoin and pick
          up where you left off.
        </p>
        <p style={{
          margin: 0,
          fontFamily: "'JetBrains Mono', monospace",
          fontSize: '12px',
          color: left > 1 ? C.muted : C.secondary,
          letterSpacing: '0.05em',
        }}>
          {left} resume{left === 1 ? '' : 's'} remaining
        </p>
        <button
          type="button"
          onClick={handleResume}
          disabled={submitting}
          style={styles.primaryButton(submitting)}
        >
          {submitting ? 'Resuming…' : 'Resume Contest'}
        </button>
        <Link to={`/contests/${id}`} style={styles.ghostButton}>
          ← Back to Contest
        </Link>
      </div>
    );
  } else if (phase === PHASE.RESUME_LIMIT) {
    body = (
      <div style={styles.card}>
        <p style={styles.cardEyebrow}>Resume limit reached</p>
        <h2 style={styles.cardHeading}>You can&apos;t resume this contest</h2>
        <p style={styles.cardBody}>
          You have already resumed this proctored contest the maximum
          number of times allowed. For fairness, further re-entry is not
          possible. Sorry for any inconvenience — if you believe this is a
          mistake, please reach out through the support page.
        </p>
        <Link to="/support" style={styles.primaryButton(false)}>
          Go to Support
        </Link>
        <Link to="/contests" style={styles.ghostButton}>
          ← All Contests
        </Link>
      </div>
    );
  } else if (phase === PHASE.CONSENT) {
    body = (
      <ConsentDialog
        contestName={contestName}
        consentVersion={eligibility?.consentVersion ?? 1}
        onAccept={handleAcceptConsent}
        onDecline={handleDeclineConsent}
        submitting={submitting}
      />
    );
  } else if (phase === PHASE.PREFLIGHT) {
    body = (
      <PreflightCheck
        onComplete={handlePreflightComplete}
        onCancel={handlePreflightCancel}
      />
    );
  } else if (phase === PHASE.CREATING) {
    body = (
      <div style={styles.card}>
        <StatusLine>Starting your proctored session…</StatusLine>
      </div>
    );
  } else if (phase === PHASE.READY) {
    body = (
      <div style={styles.card}>
        <p style={styles.cardEyebrow}>Ready</p>
        <h2 style={styles.cardHeading}>Environment looks good</h2>
        <p style={styles.cardBody}>
          Share your screen first so proctoring can capture evidence on
          tab switches. Then click Start to enter fullscreen.
        </p>
        {!screenShared ? (
          <button
            type="button"
            onClick={handleShareScreen}
            style={styles.primaryButton(false)}
          >
            Share Screen
          </button>
        ) : (
          <p style={{
            margin: 0,
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '12px',
            color: C.success,
          }}>
            ✓ Screen shared — ready to start
          </p>
        )}
        <button
          type="button"
          onClick={handleEnterContest}
          style={styles.primaryButton(false)}
        >
          Start Proctored Contest
        </button>
      </div>
    );
  } else if (phase === PHASE.ERROR) {
    body = (
      <div style={styles.card}>
        <p style={styles.cardEyebrow}>Something went wrong</p>
        <h2 style={styles.cardHeading}>We could not start the session</h2>
        <p style={styles.errorText}>
          {error || 'An unexpected error prevented the entry flow from continuing.'}
        </p>
        <button type="button" onClick={loadEligibility} style={styles.primaryButton(false)}>
          Retry
        </button>
        <Link to={`/contests/${id}`} style={styles.ghostButton}>
          ← Back to Contest
        </Link>
      </div>
    );
  }

  return (
    <div style={styles.root}>
      <div style={styles.shell}>
        {Hero}
        {body}
      </div>
    </div>
  );
}
