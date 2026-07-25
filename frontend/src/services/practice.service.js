import api from './api';

const listProblems = (page = 0, size = 25, filter = null, search = null) => {
    const params = { page, size };
    if (filter) params.filter = filter;
    if (search) params.search = search;
    return api.get('/practice/problems', { params });
};

const getProblem = (id) => api.get(`/practice/problems/${id}`);

const run = (problemId, code, language) =>
    api.post('/practice/run', { problemId: parseInt(problemId), code, language });

const stats = () => api.get('/practice/stats');

const getSubmissions = (problemId) => api.get('/practice/submissions', { params: { problemId: parseInt(problemId) } });

const PracticeService = { listProblems, getProblem, run, stats, getSubmissions };
export default PracticeService;
