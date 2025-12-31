import api from './api';

const getTestCases = (problemId) => {
  return api.get(`/admin/problems/${problemId}/testcases`);
};

const createTestCase = (problemId, testCaseData) => {
  return api.post(`/admin/problems/${problemId}/testcases`, testCaseData);
};

const updateTestCase = (id, testCaseData) => {
  return api.put(`/admin/testcases/${id}`, testCaseData);
};

const deleteTestCase = (id) => {
  return api.delete(`/admin/testcases/${id}`);
};

const TestCaseService = {
  getTestCases,
  createTestCase,
  updateTestCase,
  deleteTestCase,
};

export default TestCaseService;
