import api from './api';

const getAllContests = () => {
  return api.get('/contests');
};

const getContestById = (id) => {
  return api.get(`/contests/${id}`);
};

const createContest = (contestData) => {
  return api.post('/contests', contestData);
};

const updateContest = (id, contestData) => {
  return api.put(`/admin/contests/${id}`, contestData);
};

const ContestService = {
  getAllContests,
  getContestById,
  createContest,
  updateContest
};

export default ContestService;
