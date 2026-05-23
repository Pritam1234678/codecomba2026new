import api from './api';

const listProblems = () => api.get('/practice/problems');

const getProblem = (id) => api.get(`/practice/problems/${id}`);

const run = (problemId, code, language) =>
    api.post('/practice/run', { problemId: parseInt(problemId), code, language });

const stats = () => api.get('/practice/stats');

const PracticeService = { listProblems, getProblem, run, stats };
export default PracticeService;
