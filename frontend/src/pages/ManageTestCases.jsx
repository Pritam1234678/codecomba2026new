import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { gsap } from 'gsap';
import TestCaseService from '../services/testcase.service';

export default function ManageTestCases() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [testCases, setTestCases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [deleteModal, setDeleteModal] = useState({ show: false, id: null, index: null });

  const headerRef = useRef(null);

  const [formData, setFormData] = useState({
    input: '',
    expectedOutput: '',
    isHidden: false
  });

  useEffect(() => {
    loadTestCases();
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

  const loadTestCases = async () => {
    try {
      const response = await TestCaseService.getTestCases(id);
      setTestCases(response.data);
      setLoading(false);
    } catch (err) {
      console.error('Failed to load test cases:', err);
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await TestCaseService.updateTestCase(editingId, formData);
      } else {
        await TestCaseService.createTestCase(id, formData);
      }
      setShowForm(false);
      setEditingId(null);
      setFormData({ input: '', expectedOutput: '', isHidden: false });
      loadTestCases();
    } catch (err) {
      console.error('Failed to save test case:', err);
    }
  };

  const handleEdit = (testCase) => {
    setFormData({
      input: testCase.input,
      expectedOutput: testCase.expectedOutput,
      isHidden: testCase.isHidden || testCase.hidden
    });
    setEditingId(testCase.id);
    setShowForm(true);
  };

  const showDeleteConfirmation = (testCaseId, index) => {
    setDeleteModal({ show: true, id: testCaseId, index });
  };

  const confirmDelete = async () => {
    try {
      await TestCaseService.deleteTestCase(deleteModal.id);
      setDeleteModal({ show: false, id: null, index: null });
      loadTestCases();
    } catch (err) {
      console.error('Failed to delete test case:', err);
      setDeleteModal({ show: false, id: null, index: null });
    }
  };

  const cancelDelete = () => {
    setDeleteModal({ show: false, id: null, index: null });
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingId(null);
    setFormData({ input: '', expectedOutput: '', isHidden: false });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-400 text-lg">Loading...</div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 lg:py-8 space-y-4 sm:space-y-6">
      {/* Header */}
      <div ref={headerRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 lg:p-8 shadow-2xl">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl sm:text-2xl lg:text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-1">Manage Test Cases</h1>
            <p className="text-xs sm:text-sm text-gray-500">Add and manage test cases for this problem</p>
          </div>
          <button
            onClick={() => navigate(`/admin/problems/${id}/edit`)}
            className="w-full sm:w-auto px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 rounded-lg transition-all text-gray-300 text-center"
          >
            ← Back to Problem
          </button>
        </div>
      </div>

      {/* Add/Edit Form */}
      {showForm && (
        <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 shadow-xl">
          <h2 className="text-lg sm:text-xl font-semibold text-green-400 mb-4">
            {editingId ? 'Edit Test Case' : 'Add New Test Case'}
          </h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Input</label>
              <textarea
                value={formData.input}
                onChange={(e) => setFormData({ ...formData, input: e.target.value })}
                required
                rows={4}
                className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white font-mono resize-none"
                placeholder="Enter test case input..."
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Expected Output</label>
              <textarea
                value={formData.expectedOutput}
                onChange={(e) => setFormData({ ...formData, expectedOutput: e.target.value })}
                required
                rows={4}
                className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white font-mono resize-none"
                placeholder="Enter expected output..."
              />
            </div>

            <div className="flex items-center">
              <input
                type="checkbox"
                checked={formData.isHidden}
                onChange={(e) => setFormData({ ...formData, isHidden: e.target.checked })}
                className="w-5 h-5 text-green-500 bg-[#2a2a2a] border-[#3a3a3a] rounded focus:ring-green-500/50"
              />
              <label className="ml-3 text-sm font-medium text-gray-300">
                Hidden Test Case (not visible to users)
              </label>
            </div>

            <div className="flex flex-col sm:flex-row gap-3 sm:gap-4 pt-4 border-t border-white/10">
              <button
                type="submit"
                className="flex-1 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-semibold py-3 px-6 rounded-xl shadow-lg shadow-green-500/30 transition-all transform hover:scale-105"
              >
                {editingId ? 'Update Test Case' : 'Add Test Case'}
              </button>
              <button
                type="button"
                onClick={handleCancel}
                className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-red-400 rounded-lg transition-all"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Add Button */}
      {!showForm && (
        <button
          onClick={() => setShowForm(true)}
          className="w-full sm:w-auto px-6 py-3 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-semibold rounded-xl shadow-lg shadow-green-500/30 transition-all transform hover:scale-105"
        >
          + Add New Test Case
        </button>
      )}

      {/* Test Cases List */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold text-gray-200">
          Test Cases ({testCases.length})
        </h2>

        <div className="space-y-4">
          {testCases.length === 0 ? (
            <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-6 sm:p-8 lg:p-12 text-center shadow-xl">
              <p className="text-gray-500">No test cases added yet</p>
            </div>
          ) : (
            testCases.map((testCase, index) => (
              <motion.div
                key={testCase.id}
                whileHover={{ y: -2, scale: 1.01 }}
                transition={{ duration: 0.2, ease: 'easeOut' }}
                className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 hover:border-white/30 transition-all shadow-xl hover:shadow-2xl"
              >
                <div className="flex flex-col sm:flex-row items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-4">
                      <h3 className="text-lg font-semibold text-green-400">Test Case #{index + 1}</h3>
                      {(testCase.isHidden || testCase.hidden) ? (
                        <span className="px-2.5 py-1 bg-red-500/20 text-red-400 text-xs font-medium rounded">
                          Hidden
                        </span>
                      ) : (
                        <span className="px-2.5 py-1 bg-green-500/20 text-green-400 text-xs font-medium rounded">
                          Sample
                        </span>
                      )}
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <h4 className="text-xs font-medium text-gray-500 mb-2">Input:</h4>
                        <pre className="bg-[#2a2a2a] p-3 rounded border border-[#3a3a3a] text-sm text-gray-300 overflow-x-auto font-mono">
                          {testCase.input}
                        </pre>
                      </div>
                      <div>
                        <h4 className="text-xs font-medium text-gray-500 mb-2">Expected Output:</h4>
                        <pre className="bg-[#2a2a2a] p-3 rounded border border-[#3a3a3a] text-sm text-gray-300 overflow-x-auto font-mono">
                          {testCase.expectedOutput}
                        </pre>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-col sm:flex-col gap-2 w-full sm:w-auto sm:ml-4">
                    <button
                      onClick={() => handleEdit(testCase)}
                      className="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-green-400 rounded-lg transition-all font-medium"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => showDeleteConfirmation(testCase.id, index + 1)}
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
              Are you sure you want to delete <span className="font-semibold text-green-400">Test Case #{deleteModal.index}</span>?
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
