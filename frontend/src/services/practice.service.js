import api from './api';

const listProblems = (page = 0, size = 25) => api.get('/practice/problems', { params: { page, size } });

const getProblem = (id) => api.get(`/practice/problems/${id}`);

const run = (problemId, code, language) =>
    api.post('/practice/run', { problemId: parseInt(problemId), code, language });

const stats = () => api.get('/practice/stats');

const getSubmissions = (problemId) => api.get('/practice/submissions', { params: { problemId: parseInt(problemId) } });

const PracticeService = { listProblems, getProblem, run, stats, getSubmissions };
export default PracticeService;
