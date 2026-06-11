import api from '../../services/api';

// ── Candidate REST surface ────────────────────────────────────────────────────

// GET /api/proctoring/contests/{cid}/eligibility
const eligibility = (cid) =>
    api.get(`/proctoring/contests/${cid}/eligibility`);

// POST /api/proctoring/contests/{cid}/consent
const consent = (cid, version) =>
    api.post(`/proctoring/contests/${cid}/consent`, { consentVersion: version });

// POST /api/proctoring/contests/{cid}/sessions
const createSession = (cid) =>
    api.post(`/proctoring/contests/${cid}/sessions`);

// POST /api/proctoring/contests/{cid}/sessions/resume
const resumeSession = (cid) =>
    api.post(`/proctoring/contests/${cid}/sessions/resume`);

// POST /api/proctoring/sessions/{sid}/ws-ticket
const mintWsTicket = (sid) =>
    api.post(`/proctoring/sessions/${sid}/ws-ticket`);

// POST /api/proctoring/sessions/{sid}/finish
const finish = (sid) =>
    api.post(`/proctoring/sessions/${sid}/finish`);

// POST /api/proctoring/sessions/{sid}/quit
const quit = (sid) =>
    api.post(`/proctoring/sessions/${sid}/quit`);

// POST /api/proctoring/screenshots (multipart)
//
// Axios removes Content-Type and sets `multipart/form-data; boundary=...`
// automatically when the body is FormData — but ONLY if we don't have
// a default Content-Type in the instance config. We strip the header
// explicitly here so the boundary isn't lost.
const uploadScreenshot = (formData) =>
    api.post('/proctoring/screenshots', formData, {
        headers: { 'Content-Type': undefined },
    });

// ── Admin REST surface ────────────────────────────────────────────────────────
//
// Mirrors `ProctoringAdminController` (Req 15.x, 16.4). Every request
// flows through the shared axios instance so the JWT bearer header is
// attached automatically and 401 / 403 are surfaced in a single place.

// GET /api/admin/proctoring/sessions/{sid}
const adminSessionDetail = (sid) =>
    api.get(`/admin/proctoring/sessions/${sid}`);

// POST /api/admin/proctoring/sessions/{sid}/force-end
const adminForceEnd = (sid, reason = '') =>
    api.post(`/admin/proctoring/sessions/${sid}/force-end`, { reason });

// POST /api/admin/proctoring/sessions/{sid}/warn
const adminWarn = (sid, message) =>
    api.post(`/admin/proctoring/sessions/${sid}/warn`, { message });

// POST /api/admin/proctoring/sessions/{sid}/rescore
const adminRescore = (sid) =>
    api.post(`/admin/proctoring/sessions/${sid}/rescore`);

// GET /api/admin/proctoring/contests/{cid}/sessions?flagged=…
const adminListSessions = (cid, flagged) =>
    api.get(`/admin/proctoring/contests/${cid}/sessions`, {
        params: flagged === undefined ? {} : { flagged },
    });

// Object-form sibling that maps directly to the dashboard contract
// `adminLiveList({ contestId?, flagged? })`. Backend has no global
// "all contests" endpoint, so passing `contestId: undefined` resolves
// to a 400 — callers fanning out across contests must invoke
// `adminListSessions(cid)` per proctored contest themselves.
const adminLiveList = ({ contestId, flagged } = {}) => {
    if (contestId == null) {
        return Promise.reject(new Error('adminLiveList: contestId is required'));
    }
    return adminListSessions(contestId, flagged);
};

// POST /api/admin/proctoring/contests/{cid}/stream/ticket
const adminMintSseTicket = (cid) =>
    api.post(`/admin/proctoring/contests/${cid}/stream/ticket`);

// GET /api/admin/proctoring/sessions/{sid}/screenshots/{shotId}
//
// Fetches the image bytes as a Blob using the shared axios instance so
// the JWT bearer header is attached. Returns the Blob directly — the
// caller is responsible for `URL.createObjectURL` and revoke.
const adminFetchScreenshotBlob = async (sid, shotId) => {
    const res = await api.get(
        `/admin/proctoring/sessions/${sid}/screenshots/${shotId}`,
        { responseType: 'blob' }
    );
    return res.data;
};

const proctoringApi = {
    eligibility,
    consent,
    createSession,
    resumeSession,
    mintWsTicket,
    finish,
    quit,
    uploadScreenshot,
    adminSessionDetail,
    adminForceEnd,
    adminWarn,
    adminRescore,
    adminListSessions,
    adminMintSseTicket,
    adminFetchScreenshotBlob,
    adminLiveList,
};

export default proctoringApi;
