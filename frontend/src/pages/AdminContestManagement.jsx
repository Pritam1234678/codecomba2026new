import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { gsap } from 'gsap';
import AdminService from '../services/admin.service';

const AdminContestManagement = () => {
  const [contests, setContests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleteModal, setDeleteModal] = useState({ show: false, contestId: null, contestName: '' });

  const headerRef = useRef(null);

  useEffect(() => {
    loadContests();
  }, []);

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

  const loadContests = () => {
    setLoading(true);
    AdminService.getAllContestsAdmin()
      .then(response => {
        setContests(response.data);
        setLoading(false);
      })
      .catch(err => {
        setError('Failed to load contests');
        setLoading(false);
      });
  };

  const handleToggleContest = (contestId, currentStatus) => {
    const action = currentStatus
      ? AdminService.deactivateContest(contestId)
      : AdminService.activateContest(contestId);

    action.then(() => {
      loadContests();
    }).catch(err => {
      alert('Failed to update contest status');
    });
  };

  const showDeleteConfirmation = (contestId, contestName) => {
    setDeleteModal({ show: true, contestId, contestName });
  };

  const confirmDelete = () => {
    AdminService.deleteContest(deleteModal.contestId)
      .then(() => {
        setDeleteModal({ show: false, contestId: null, contestName: '' });
        loadContests();
      })
      .catch(err => {
        setDeleteModal({ show: false, contestId: null, contestName: '' });
      });
  };

  const cancelDelete = () => {
    setDeleteModal({ show: false, contestId: null, contestName: '' });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-400 text-lg">Loading...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-red-400 text-lg">{error}</div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 lg:py-8 space-y-4 sm:space-y-6">
      {/* Header */}
      <div ref={headerRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 lg:p-8 shadow-2xl">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-xl sm:text-2xl lg:text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-1">
              Contest Management
            </h1>
            <p className="text-xs sm:text-sm text-gray-500">Manage all contests and their settings</p>
          </div>
          <Link
            to="/admin/contests/create"
            className="w-full sm:w-auto bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-semibold px-6 py-3 rounded-xl shadow-lg shadow-green-500/30 transition-all transform hover:scale-105 text-center"
          >
            Create New Contest
          </Link>
        </div>
      </div>

      {/* Contests Grid */}
      <div className="space-y-3 sm:space-y-4">
        {contests.length === 0 ? (
          <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-6 sm:p-8 lg:p-12 text-center shadow-xl">
            <p className="text-gray-500 mb-4">No contests found</p>
            <Link
              to="/admin/contests/create"
              className="text-green-400 hover:text-green-300 text-sm font-medium transition-colors"
            >
              Create your first contest →
            </Link>
          </div>
        ) : (
          contests.map((contest, index) => (
            <motion.div
              key={contest.id}
              whileHover={{ y: -2, scale: 1.01 }}
              transition={{ duration: 0.2, ease: 'easeOut' }}
              className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 hover:border-white/30 transition-all shadow-xl hover:shadow-2xl"
            >
              <div className="flex flex-col sm:flex-row justify-between items-start gap-4 mb-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h2 className="text-lg sm:text-xl lg:text-2xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent">{contest.name}</h2>
                    <span className={`px-2.5 py-1 rounded text-xs font-medium border ${contest.status === 'LIVE' ? 'bg-green-500/10 text-green-400 border-green-500/30' :
                      contest.status === 'UPCOMING' ? 'bg-yellow-500/10 text-yellow-400 border-yellow-500/30' :
                        'bg-gray-500/10 text-gray-400 border-gray-500/30'
                      }`}>
                      {contest.status}
                    </span>
                  </div>
                  <div className="flex flex-col sm:flex-row gap-2 sm:gap-4 text-xs text-gray-500 mb-3">
                    <span>Start: {new Date(contest.startTime).toLocaleString()}</span>
                    <span>End: {new Date(contest.endTime).toLocaleString()}</span>
                  </div>
                  <p className="text-sm text-gray-400">{contest.description}</p>
                </div>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 sm:gap-3 pt-4 border-t border-white/10">
                <Link
                  to={`/admin/contests/${contest.id}/problems`}
                  className="px-3 sm:px-4 py-2 rounded-lg text-xs sm:text-sm font-medium bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-green-400 transition-all text-center"
                >
                  Manage Problems
                </Link>

                <Link
                  to={`/admin/contests/${contest.id}/edit`}
                  className="px-3 sm:px-4 py-2 rounded-lg text-xs sm:text-sm font-medium bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-green-400 transition-all text-center"
                >
                  Edit Contest
                </Link>

                <button
                  onClick={() => handleToggleContest(contest.id, contest.active)}
                  className="px-3 sm:px-4 py-2 rounded-lg text-xs sm:text-sm font-medium bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-green-400 transition-all text-center"
                >
                  {contest.active ? 'Deactivate' : 'Activate'}
                </button>

                <button
                  onClick={() => showDeleteConfirmation(contest.id, contest.name)}
                  className="px-3 sm:px-4 py-2 rounded-lg text-xs sm:text-sm font-medium bg-white/5 hover:bg-red-500/10 border border-white/20 hover:border-red-500/30 text-gray-300 hover:text-red-400 transition-all text-center"
                >
                  Delete
                </button>
              </div>
            </motion.div>
          ))
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {deleteModal.show && (
        <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-50">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-red-500/30 rounded-3xl p-8 max-w-md w-full mx-4 shadow-2xl"
          >
            <h3 className="text-2xl font-semibold text-red-400 mb-4">Confirm Delete</h3>
            <p className="text-gray-300 mb-6">
              Are you sure you want to delete <span className="font-semibold text-green-400">"{deleteModal.contestName}"</span>?
              <br />
              <span className="text-red-400 text-sm mt-2 block">This action cannot be undone and will delete all associated problems and test cases.</span>
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
};

export default AdminContestManagement;

