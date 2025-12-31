import api from './api';

const submitCode = (problemId, code, language) => {
  return api.post('/submissions', {
    problemId,
    code,
    language
  });
};

const SubmissionService = {
  submitCode
};

export default SubmissionService;
