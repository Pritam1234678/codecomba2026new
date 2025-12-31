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

// Code Snippet APIs
const getSnippets = (problemId) => {
  return api.get(`/problems/${problemId}/snippets`);
};

const getSnippet = (problemId, language) => {
  return api.get(`/problems/${problemId}/snippets/${language}`);
};

const saveSnippet = (problemId, snippetData) => {
  return api.post(`/problems/${problemId}/snippets`, snippetData);
};

const saveAllSnippets = (problemId, snippetsArray) => {
  return api.post(`/problems/${problemId}/snippets/bulk`, snippetsArray);
};

const ProblemService = {
  getProblemById,
  getProblemsByContest,
  createProblem,
  getSnippets,
  getSnippet,
  saveSnippet,
  saveAllSnippets
};

export default ProblemService;
