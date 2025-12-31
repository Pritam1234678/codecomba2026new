import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { gsap } from 'gsap';
import axios from 'axios';
import ProblemService from '../services/problem.service';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

export default function ManageContestProblems() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [contest, setContest] = useState(null);
  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddForm, setShowAddForm] = useState(false);
  const [deleteModal, setDeleteModal] = useState({ show: false, problemId: null, problemTitle: '' });

  const headerRef = useRef(null);

  const [newProblem, setNewProblem] = useState({
    title: '',
    description: '',
    inputFormat: '',
    outputFormat: '',
    constraints: '',
    timeLimit: 1000,
    memoryLimit: 256,
    example1: '',
    example2: '',
    example3: '',
    images: '',
    active: true
  });

  const [snippets, setSnippets] = useState({
    JAVA: { starterCode: '', solutionTemplate: '' },
    CPP: { starterCode: '', solutionTemplate: '' },
    PYTHON: { starterCode: '', solutionTemplate: '' },
    JAVASCRIPT: { starterCode: '', solutionTemplate: '' },
    C: { starterCode: '', solutionTemplate: '' }
  });

  const [activeSnippetTab, setActiveSnippetTab] = useState('JAVA');

  useEffect(() => {
    loadContestAndProblems();
  }, [id]);

  useEffect(() => {
    if (!loading && headerRef.current) {
      gsap.from(headerRef.current, {
        opacity: 0,
        y: 30,
        duration: 0.8,
        ease: 'power3.out'
      });
    }
  }, [loading]);

  const loadContestAndProblems = async () => {
    try {
      const token = localStorage.getItem('token');
      const headers = { Authorization: `Bearer ${token}` };

      const contestRes = await axios.get(`${API_URL}/contests/${id}`, { headers });
      setContest(contestRes.data);

      const problemsRes = await axios.get(`${API_URL}/problems/contest/${id}`, { headers });
      setProblems(problemsRes.data);

      setLoading(false);
    } catch (err) {
      console.error('Failed to load data:', err);
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setNewProblem(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSnippetChange = (language, field, value) => {
    setSnippets(prev => ({
      ...prev,
      [language]: {
        ...prev[language],
        [field]: value
      }
    }));
  };

  const handleAddProblem = async (e) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem('token');
      const headers = { Authorization: `Bearer ${token}` };

      // 1. Create problem first - Convert empty strings to null
      const problemData = {
        ...newProblem,
        example1: newProblem.example1.trim() || null,
        example2: newProblem.example2.trim() || null,
        example3: newProblem.example3.trim() || null,
        images: newProblem.images.trim() || null
      };

      const problemRes = await axios.post(`${API_URL}/admin/problems/contest/${id}`, problemData, { headers });
      const problemId = problemRes.data.id;

      // 2. Save snippets for the new problem
      const snippetsArray = Object.entries(snippets).map(([lang, data]) => ({
        language: lang,
        starterCode: data.starterCode,
        solutionTemplate: data.solutionTemplate
      }));

      await ProblemService.saveAllSnippets(problemId, snippetsArray);

      // 3. Reset form
      setNewProblem({
        title: '',
        description: '',
        inputFormat: '',
        outputFormat: '',
        constraints: '',
        timeLimit: 1000,
        memoryLimit: 256,
        example1: '',
        example2: '',
        example3: '',
        images: '',
        active: true
      });

      setSnippets({
        JAVA: { starterCode: '', solutionTemplate: '' },
        CPP: { starterCode: '', solutionTemplate: '' },
        PYTHON: { starterCode: '', solutionTemplate: '' },
        JAVASCRIPT: { starterCode: '', solutionTemplate: '' },
        C: { starterCode: '', solutionTemplate: '' }
      });

      setShowAddForm(false);
      loadContestAndProblems();
    } catch (err) {
      alert('Failed to add problem: ' + (err.response?.data?.message || err.message));
    }
  };

  const showDeleteConfirmation = (problemId, problemTitle) => {
    setDeleteModal({ show: true, problemId, problemTitle });
  };

  const confirmDelete = async () => {
    try {
      const token = localStorage.getItem('token');
      await axios.delete(`${API_URL}/admin/problems/${deleteModal.problemId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setDeleteModal({ show: false, problemId: null, problemTitle: '' });
      loadContestAndProblems();
    } catch (err) {
      console.error('Delete error:', err);
      setDeleteModal({ show: false, problemId: null, problemTitle: '' });
    }
  };

  const cancelDelete = () => {
    setDeleteModal({ show: false, problemId: null, problemTitle: '' });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-400 text-lg">Loading...</div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div ref={headerRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-8 shadow-2xl">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-1">Manage Problems</h1>
            <p className="text-sm text-gray-500">{contest?.name}</p>
          </div>
          <button
            onClick={() => navigate('/admin/contests')}
            className="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 rounded-lg transition-all text-gray-300"
          >
            ← Back to Contests
          </button>
        </div>
      </div>

      {/* Add Problem Button */}
      <div>
        <button
          onClick={() => setShowAddForm(!showAddForm)}
          className={`px-6 py-3 font-semibold rounded-xl shadow-lg transition-all transform hover:scale-105 ${showAddForm
              ? 'bg-gradient-to-r from-red-500 to-red-600 hover:from-red-600 hover:to-red-700 text-white shadow-red-500/30'
              : 'bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white shadow-green-500/30'
            }`}
        >
          {showAddForm ? '✕ Cancel' : '+ Add New Problem'}
        </button>
      </div>

      {/* Add Problem Form */}
      {showAddForm && (
        <form onSubmit={handleAddProblem} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 space-y-4 shadow-xl">
          <h3 className="text-xl font-semibold text-green-400 mb-4">New Problem</h3>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Title *</label>
            <input
              type="text"
              name="title"
              value={newProblem.title}
              onChange={handleInputChange}
              required
              className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50"
              placeholder="e.g., Two Sum"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Description *</label>
            <textarea
              name="description"
              value={newProblem.description}
              onChange={handleInputChange}
              required
              rows={4}
              className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50 resize-none"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Input Format</label>
              <textarea
                name="inputFormat"
                value={newProblem.inputFormat}
                onChange={handleInputChange}
                rows={2}
                className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50 resize-none"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Output Format</label>
              <textarea
                name="outputFormat"
                value={newProblem.outputFormat}
                onChange={handleInputChange}
                rows={2}
                className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50 resize-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Constraints</label>
            <textarea
              name="constraints"
              value={newProblem.constraints}
              onChange={handleInputChange}
              rows={2}
              className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50 resize-none"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Time Limit (ms)</label>
              <input
                type="number"
                name="timeLimit"
                value={newProblem.timeLimit}
                onChange={handleInputChange}
                className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Memory Limit (MB)</label>
              <input
                type="number"
                name="memoryLimit"
                value={newProblem.memoryLimit}
                onChange={handleInputChange}
                className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50"
              />
            </div>
          </div>

          {/* Optional Examples Section */}
          <div className="border-t border-[#3a3a3a] pt-6 mt-6">
            <h3 className="text-lg font-semibold text-white mb-4">Optional Examples</h3>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Example 1 (Optional)
              </label>
              <textarea
                name="example1"
                value={newProblem.example1}
                onChange={handleInputChange}
                rows={4}
                placeholder="Input: nums = [3,2,1,4,5], k = 4&#10;Output: 3&#10;Explanation: The subarrays that have a median equal to 4 are: [4], [4,5] and [1,4,5]."
                className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50 resize-none font-mono text-sm"
              />
            </div>

            <div className="mt-4">
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Example 2 (Optional)
              </label>
              <textarea
                name="example2"
                value={newProblem.example2}
                onChange={handleInputChange}
                rows={4}
                placeholder="Input: nums = [2,3,1], k = 3&#10;Output: 1&#10;Explanation: [3] is the only subarray that has a median equal to 3."
                className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50 resize-none font-mono text-sm"
              />
            </div>

            <div className="mt-4">
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Example 3 (Optional)
              </label>
              <textarea
                name="example3"
                value={newProblem.example3}
                onChange={handleInputChange}
                rows={4}
                placeholder="Input: nums = [1,2,3], k = 2&#10;Output: 0&#10;Explanation: There are no subarrays with median equal to 2."
                className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50 resize-none font-mono text-sm"
              />
            </div>

            <div className="mt-4">
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Images (Optional)
                <span className="text-xs text-gray-500 ml-2">Comma-separated URLs</span>
              </label>
              <input
                type="text"
                name="images"
                value={newProblem.images}
                onChange={handleInputChange}
                placeholder="https://example.com/image1.png, https://example.com/image2.png"
                className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white focus:ring-2 focus:ring-green-500/50"
              />
            </div>
          </div>

          <div className="flex items-center">
            <input
              type="checkbox"
              name="active"
              checked={newProblem.active}
              onChange={handleInputChange}
              className="w-5 h-5 text-green-500 bg-[#2a2a2a] border-[#3a3a3a] rounded focus:ring-green-500/50"
            />
            <label className="ml-2 text-sm text-gray-300">Active</label>
          </div>

          {/* Code Snippets Section */}
          <div className="mt-6 border-t border-[#3a3a3a] pt-6">
            <h3 className="text-lg font-semibold text-white mb-4">Code Snippets (Required for all languages)</h3>

            {/* Language Tabs */}
            <div className="flex gap-2 mb-4 overflow-x-auto">
              {['JAVA', 'CPP', 'PYTHON', 'JAVASCRIPT', 'C'].map(lang => (
                <button
                  key={lang}
                  type="button"
                  onClick={() => setActiveSnippetTab(lang)}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${activeSnippetTab === lang
                    ? 'bg-green-500 text-white'
                    : 'bg-[#2a2a2a] text-gray-400 hover:bg-[#3a3a3a]'
                    }`}
                >
                  {lang === 'CPP' ? 'C++' : lang === 'JAVASCRIPT' ? 'JavaScript' : lang}
                </button>
              ))}
            </div>

            {/* Snippet Fields for Active Tab */}
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Starter Code (shown to user) *
                </label>
                <textarea
                  value={snippets[activeSnippetTab].starterCode}
                  onChange={(e) => handleSnippetChange(activeSnippetTab, 'starterCode', e.target.value)}
                  rows={8}
                  placeholder="class Solution {\n    // Write your code here\n}"
                  className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white font-mono text-sm focus:ring-2 focus:ring-green-500/50 resize-none"
                />
                <p className="text-xs text-gray-500 mt-1">Function signature that users will see and edit</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Solution Template (for execution) *
                </label>
                <textarea
                  value={snippets[activeSnippetTab].solutionTemplate}
                  onChange={(e) => handleSnippetChange(activeSnippetTab, 'solutionTemplate', e.target.value)}
                  rows={12}
                  placeholder="import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        // Input parsing\n    }\n}\n\n// USER_CODE_PLACEHOLDER"
                  className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white font-mono text-sm focus:ring-2 focus:ring-green-500/50 resize-none"
                />
                <p className="text-xs text-gray-500 mt-1">Complete executable code with USER_CODE_PLACEHOLDER where user code will be inserted</p>
              </div>
            </div>
          </div>

          <button
            type="submit"
            className="w-full bg-green-500/10 hover:bg-green-500/20 border border-green-500/30 hover:border-green-500/50 text-green-400 font-medium py-3 rounded-lg transition-all"
          >
            Add Problem
          </button>
        </form>
      )}

      {/* Problems List */}
      <div className="space-y-4">
        <h3 className="text-xl font-semibold text-gray-200">
          Problems ({problems.length})
        </h3>

        <div className="space-y-4">
          {problems.length === 0 ? (
            <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-12 text-center shadow-xl">
              <p className="text-gray-500">No problems added yet</p>
            </div>
          ) : (
            problems.map((problem, index) => (
              <motion.div
                key={problem.id}
                whileHover={{ y: -2, scale: 1.01 }}
                transition={{ duration: 0.2, ease: 'easeOut' }}
                className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 hover:border-white/30 transition-all shadow-xl hover:shadow-2xl"
              >
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <span className="text-green-400 font-bold text-lg">#{index + 1}</span>
                      <h4 className="text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent">{problem.title}</h4>
                      {problem.active && (
                        <span className="px-2.5 py-1 bg-green-500/20 text-green-400 text-xs rounded">ACTIVE</span>
                      )}
                    </div>
                    <p className="text-gray-400 mb-3">{problem.description}</p>
                    <div className="flex gap-4 text-sm text-gray-500">
                      <span>⏱️ {problem.timeLimit}ms</span>
                      <span>💾 {problem.memoryLimit}MB</span>
                    </div>
                  </div>

                  <div className="flex flex-col gap-2 ml-4">
                    <button
                      onClick={() => navigate(`/admin/problems/${problem.id}/edit`)}
                      className="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-blue-400 rounded-lg transition-all font-medium"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => navigate(`/admin/problems/${problem.id}/testcases`)}
                      className="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-green-400 rounded-lg transition-all font-medium"
                    >
                      Test Cases
                    </button>
                    <button
                      onClick={() => showDeleteConfirmation(problem.id, problem.title)}
                      className="px-4 py-2 bg-white/5 hover:bg-red-500/10 border border-white/20 hover:border-red-500/30 text-gray-300 hover:text-red-400 rounded-lg transition-all font-medium"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </motion.div>
            ))
          )}
        </div>
      </div>

      {/* Delete Modal */}
      {deleteModal.show && (
        <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-50">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-red-500/30 rounded-3xl p-8 max-w-md w-full mx-4 shadow-2xl"
          >
            <h3 className="text-2xl font-semibold text-red-400 mb-4">Confirm Delete</h3>
            <p className="text-gray-300 mb-6">
              Are you sure you want to delete <span className="font-semibold text-green-400">"{deleteModal.problemTitle}"</span>?
              <br />
              <span className="text-red-400 text-sm mt-2 block">This action cannot be undone.</span>
            </p>
            <div className="flex gap-4">
              <button
                onClick={confirmDelete}
                className="flex-1 bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 hover:border-red-500/50 text-red-400 font-medium py-3 px-6 rounded-lg transition-all"
              >
                Delete
              </button>
              <button
                onClick={cancelDelete}
                className="flex-1 bg-[#2a2a2a] hover:bg-[#3a3a3a] border border-[#3a3a3a] hover:border-[#4a4a4a] text-gray-300 font-medium py-3 px-6 rounded-lg transition-all"
              >
                Cancel
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
