import api from './api';

const listProblems = () => api.get('/practice/problems');

const getProblem = (id) => api.get(`/practice/problems/${id}`);

const run = (problemId, code, language) =>
    api.post('/practice/run', { problemId: parseInt(problemId), code, language });

const stats = () => api.get('/practice/stats');

const getSubmissions = (problemId) => api.get('/practice/submissions', { params: { problemId: parseInt(problemId) } });

const PracticeService = { listProblems, getProblem, run, stats, getSubmissions };
export default PracticeService;
