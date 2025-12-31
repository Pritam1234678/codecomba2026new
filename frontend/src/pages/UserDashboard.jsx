import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';
import AuthService from '../services/auth.service';

const UserDashboard = () => {
  const [user, setUser] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [stats, setStats] = useState({
    total: 0,
    accepted: 0,
    successRate: 0,
    problemsSolved: 0
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUserProfile();
    fetchSubmissions();
  }, []);



  const fetchUserProfile = async () => {
    try {
      const res = await api.get('/user/profile');
      setUser(res.data);
    } catch (err) {
      console.error('Error fetching user profile:', err);
      const currentUser = AuthService.getCurrentUser();
      setUser(currentUser);
    }
  };

  const fetchSubmissions = async () => {
    try {
      const res = await api.get('/submissions/user');
      setSubmissions(res.data);
      calculateStats(res.data);
      setLoading(false);
    } catch (err) {
      console.error('Error fetching submissions:', err);
      setLoading(false);
    }
  };

  const calculateStats = (submissions) => {
    const total = submissions.length;
    const accepted = submissions.filter(s => s.status === 'AC').length;
    const problemsSolved = new Set(submissions.filter(s => s.status === 'AC').map(s => s.problemId)).size;
    const successRate = total > 0 ? ((accepted / total) * 100).toFixed(1) : 0;
    setStats({ total, accepted, successRate, problemsSolved });
  };

  const getStatusBadge = (status) => {
    const styles = {
      'AC': 'bg-green-500/10 text-green-400 border-green-500/30',
      'WA': 'bg-red-500/10 text-red-400 border-red-500/30',
      'RE': 'bg-orange-500/10 text-orange-400 border-orange-500/30',
      'CE': 'bg-yellow-500/10 text-yellow-400 border-yellow-500/30',
      'JUDGING': 'bg-blue-500/10 text-blue-400 border-blue-500/30'
    };
    return styles[status] || 'bg-gray-500/10 text-gray-400 border-gray-500/30';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-400 text-lg">Loading...</div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 lg:py-8 space-y-4 sm:space-y-6">
      {/* Header Section */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 lg:p-8 shadow-2xl"
      >
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 sm:gap-6">
          <div className="w-12 h-12 sm:w-16 sm:h-16 bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-sm border border-white/20 rounded-xl flex items-center justify-center shadow-lg overflow-hidden shrink-0">
            {user?.photoUrl ? (
              <img
                src={user.photoUrl}
                alt={user?.fullName || user?.username}
                className="w-full h-full object-cover"
              />
            ) : (
              <span className="text-2xl font-bold text-gray-100">
                {user?.fullName?.charAt(0).toUpperCase() || user?.username?.charAt(0).toUpperCase()}
              </span>
            )}
          </div>
          <div className="flex-1">
            <h1 className="text-xl sm:text-2xl lg:text-3xl font-semibold mb-1 bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent">
              {user?.fullName || user?.username}
            </h1>
            <p className="text-xs sm:text-sm text-gray-500">
              Welcome back, <span className="text-gray-300 font-medium">{user?.username}</span>
            </p>
          </div>
          <Link
            to="/profile/edit"
            className="w-full sm:w-auto px-4 sm:px-6 lg:px-8 py-3 sm:py-4 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white text-sm sm:text-base font-semibold rounded-xl shadow-lg shadow-green-500/30 transition-all transform hover:scale-105 flex items-center justify-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            Edit Profile
          </Link>
        </div>
      </motion.div>

      {/* Stats Grid */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.5, delay: 0.2 }}
        className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4"
      >
        <motion.div
          whileHover={{ scale: 1.02, y: -4 }}
          transition={{ duration: 0.2 }}
          className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 hover:border-white/30 transition-all shadow-xl hover:shadow-2xl group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Total Submissions</div>
            <div className="w-10 h-10 rounded-lg bg-blue-500/10 flex items-center justify-center group-hover:bg-blue-500/20 transition-colors">
              <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
          </div>
          <div className="text-2xl sm:text-3xl lg:text-4xl font-bold bg-gradient-to-r from-white to-gray-300 bg-clip-text text-transparent">{stats.total}</div>
        </motion.div>

        <motion.div
          whileHover={{ scale: 1.02, y: -4 }}
          transition={{ duration: 0.2 }}
          className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 hover:border-white/30 transition-all shadow-xl hover:shadow-2xl group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Accepted</div>
            <div className="w-10 h-10 rounded-lg bg-green-500/20 flex items-center justify-center group-hover:bg-green-500/30 transition-colors">
              <svg className="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
          <div className="text-2xl sm:text-3xl lg:text-4xl font-bold text-green-400">{stats.accepted}</div>
        </motion.div>

        <motion.div
          whileHover={{ scale: 1.02, y: -4 }}
          transition={{ duration: 0.2 }}
          className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 hover:border-white/30 transition-all shadow-xl hover:shadow-2xl group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Success Rate</div>
            <div className="w-10 h-10 rounded-lg bg-emerald-500/10 flex items-center justify-center group-hover:bg-emerald-500/20 transition-colors">
              <svg className="w-5 h-5 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            </div>
          </div>
          <div className="text-2xl sm:text-3xl lg:text-4xl font-bold text-emerald-400">{stats.successRate}%</div>
        </motion.div>

        <motion.div
          whileHover={{ scale: 1.02, y: -4 }}
          transition={{ duration: 0.2 }}
          className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 hover:border-white/30 transition-all shadow-xl hover:shadow-2xl group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Problems Solved</div>
            <div className="w-10 h-10 rounded-lg bg-purple-500/10 flex items-center justify-center group-hover:bg-purple-500/20 transition-colors">
              <svg className="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
              </svg>
            </div>
          </div>
          <div className="text-2xl sm:text-3xl lg:text-4xl font-bold bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">{stats.problemsSolved}</div>
        </motion.div>
      </motion.div>

      {/* Profile Details */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.4 }}
        className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 shadow-2xl"
      >
        <h2 className="text-lg sm:text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-3 sm:mb-4">Profile Information</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
          <div>
            <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Email</div>
            <div className="text-sm text-gray-300 font-mono">{user?.email || '—'}</div>
          </div>
          <div>
            <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Full Name</div>
            <div className="text-sm text-gray-300">{user?.fullName || '—'}</div>
          </div>
          <div>
            <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Roll Number</div>
            <div className="text-sm text-gray-300 font-mono">{user?.rollNumber || '—'}</div>
          </div>
          <div>
            <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Branch</div>
            <div className="text-sm text-gray-300">{user?.branch || '—'}</div>
          </div>
        </div>
      </motion.div>

      {/* Recent Submissions */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.6 }}
        className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl overflow-hidden shadow-2xl"
      >
        <div className="p-4 sm:p-6 border-b border-white/10">
          <h2 className="text-lg sm:text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent">Recent Submissions</h2>
        </div>

        {submissions.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-gray-500 mb-4">No submissions yet</p>
            <Link
              to="/contests"
              className="text-green-400 hover:text-green-300 text-sm font-medium transition-colors"
            >
              Start solving problems →
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-[#151515] border-b border-[#2a2a2a]">
                <tr>

                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Language
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Score
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Action
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#2a2a2a]">
                {submissions.slice(0, 10).map((submission, index) => (
                  <motion.tr
                    key={submission.id}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.05, duration: 0.3 }}
                    whileHover={{ x: 3, backgroundColor: '#1f1f1f' }}
                    className="transition-colors"
                  >

                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="text-sm text-gray-400 font-mono">
                        {submission.language}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2.5 py-1 rounded text-xs font-medium border ${getStatusBadge(submission.status)}`}>
                        {submission.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="text-sm font-medium text-gray-300">
                        {submission.score || 0}<span className="text-gray-600">/100</span>
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {new Date(submission.submittedAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Link
                        to={`/problems/${submission.problemId}`}
                        className="text-sm text-green-400 hover:text-green-300 font-medium transition-colors"
                      >
                        View →
                      </Link>
                    </td>
                  </motion.tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </motion.div>
    </div>
  );
};

export default UserDashboard;  