import api from './api';

const getProblemById = (id) => {
  return api.get(`/problems/${id}`);
};

const getProblemsByContest = (contestId) => {
  return api.get(`/problems/contest/${contestId}`);
};

const createProblem = (contestId, problemData) => {
  return api.post(`/problems/contest/${contestId}`, problemData);
};

// ─── Code Snippet APIs ────────────────────────────────────────────────────────

/**
 * User-facing: returns only the starter code extracted from between
 * USER_CODE_START / USER_CODE_END markers. solutionTemplate is stripped.
 */
const getSnippets = (problemId) => {
  return api.get(`/problems/${problemId}/snippets`);
};

/**
 * User-facing: get starter code for a specific language.
 */
const getSnippet = (problemId, language) => {
  return api.get(`/problems/${problemId}/snippets/${language}`);
};

/**
 * Admin: get full harness (solutionTemplate) for all languages for editing.
 */
const getSnippetsAdmin = (problemId) => {
  return api.get(`/problems/${problemId}/snippets/admin`);
};

/**
 * Admin: save a single snippet (only solutionTemplate is stored).
 */
const saveSnippet = (problemId, snippetData) => {
  return api.post(`/problems/${problemId}/snippets`, snippetData);
};

/**
 * Admin: bulk save snippets for all languages.
 * Each item should have: { language, solutionTemplate }
 */
const saveAllSnippets = (problemId, snippetsArray) => {
  return api.post(`/problems/${problemId}/snippets/bulk`, snippetsArray);
};

const ProblemService = {
  getProblemById,
  getProblemsByContest,
  createProblem,
  getSnippets,
  getSnippet,
  getSnippetsAdmin,
  saveSnippet,
  saveAllSnippets
};

export default ProblemService;
