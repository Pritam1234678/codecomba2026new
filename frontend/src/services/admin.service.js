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
  getAllContestsAdmin,
  getContestStats,
  createContest,
  updateContest,
  activateContest,
  deactivateContest,
  deleteContest,
  createProblem,
  updateProblem,
  deleteProblem
};

export default AdminService;
