import api from './api';

/**
 * Submit code for judging (saved to DB, upserts previous submission).
 */
const submitCode = (problemId, code, language) => {
  return api.post('/submissions', {
    problemId: parseInt(problemId),
    code,
    language
  });
};

/**
 * Test run — judges the code but does NOT save to DB.
 */
const testCode = (problemId, code, language) => {
  return api.post('/submissions/test', {
    problemId: parseInt(problemId),
    code,
    language
  });
};

/**
 * Get the current user's submission for a specific problem.
 * Returns 404 if no submission exists yet.
 */
const getUserSubmission = (problemId) => {
  return api.get(`/submissions/user/${problemId}`);
};

/**
 * Get all submissions by the current user.
 */
const getUserSubmissions = () => {
  return api.get('/submissions/user');
};

const SubmissionService = {
  submitCode,
  testCode,
  getUserSubmission,
  getUserSubmissions
};

export default SubmissionService;
