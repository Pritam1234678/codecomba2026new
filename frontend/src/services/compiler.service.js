import api from './api';

/**
 * Public compiler — no auth required.
 * Backend: POST /api/compiler/run
 */
const run = (code, language, stdin = '') => {
    return api.post('/compiler/run', { code, language, stdin });
};

const status = () => {
    return api.get('/compiler/status');
};

const CompilerService = { run, status };
export default CompilerService;
