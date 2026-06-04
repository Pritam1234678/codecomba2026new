import api from './api';

// User Management
const getAllUsers = () => {
  return api.get('/admin/users');
};

const getUserStats = () => {
  return api.get('/admin/users/stats');
};

const enableUser = (id) => {
  return api.put(`/admin/users/${id}/enable`);
};

const disableUser = (id) => {
  return api.put(`/admin/users/${id}/disable`);
};

const deleteUser = (id) => {
  return api.delete(`/admin/users/${id}`);
};

// Contest Management
const getAllContestsAdmin = () => {
  return api.get('/admin/contests');
};

const getContestStats = () => {
  return api.get('/admin/contests/stats');
};

const createContest = (contestData) => {
  return api.post('/admin/contests', contestData);
};

const updateContest = (id, contestData) => {
  return api.put(`/admin/contests/${id}`, contestData);
};

const activateContest = (id) => {
  return api.put(`/admin/contests/${id}/activate`);
};

const deactivateContest = (id) => {
  return api.put(`/admin/contests/${id}/deactivate`);
};

const deleteContest = (id) => {
  return api.delete(`/admin/contests/${id}`);
};

// ── Proctored toggle ─────────────────────────────────────────────────────────
// Mirrors ProctoredContestController (Req 15, 20). Enabling creates a row in
// `proctored_contests` with the default consent_version=1; disabling deletes
// the row, which makes the contest non-proctored on the next eligibility read.
const enableProctored = (id, consentVersion = 1) => {
  return api.post(`/admin/proctoring/contests/${id}`, { consentVersion });
};

const disableProctored = (id) => {
  return api.delete(`/admin/proctoring/contests/${id}`);
};

// Problem Management
const createProblem = (contestId, problemData) => {
  return api.post(`/admin/problems/contest/${contestId}`, problemData);
};

const updateProblem = (id, problemData) => {
  return api.put(`/admin/problems/${id}`, problemData);
};

const deleteProblem = (id) => {
  return api.delete(`/admin/problems/${id}`);
};

const AdminService = {
  getAllUsers,
  getUserStats,
  enableUser,
  disableUser,
  deleteUser,
  getAllContestsAdmin,
  getContestStats,
  createContest,
  updateContest,
  activateContest,
  deactivateContest,
  deleteContest,
  enableProctored,
  disableProctored,
  createProblem,
  updateProblem,
  deleteProblem
};

export default AdminService;
