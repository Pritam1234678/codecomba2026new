import React, { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { gsap } from 'gsap';
import AdminService from '../services/admin.service';

const AdminUserManagement = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleteModal, setDeleteModal] = useState({ show: false, userId: null, username: '' });
  const [notification, setNotification] = useState({ show: false, message: '', type: '' });

  const headerRef = useRef(null);
  const tableRef = useRef(null);

  useEffect(() => {
    loadUsers();
  }, []);

  useEffect(() => {
    if (!loading && headerRef.current && tableRef.current) {
      const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });

      tl.from(headerRef.current, {
        opacity: 0,
        y: 30,
        duration: 0.8
      })
        .from(tableRef.current, {
          opacity: 0,
          y: 20,
          duration: 0.6
        }, '-=0.3');
    }
  }, [loading]);

  const loadUsers = () => {
    setLoading(true);
    AdminService.getAllUsers()
      .then(response => {
        // Filter out admin users, only show regular users
        const regularUsers = response.data.filter(user =>
          !user.roles.some(role => role.name === 'ROLE_ADMIN')
        );
        setUsers(regularUsers);
        setLoading(false);
      })
      .catch(err => {
        setError('Failed to load users');
        setLoading(false);
      });
  };

  const handleToggleUser = (userId, currentStatus) => {
    const action = currentStatus ? AdminService.disableUser(userId) : AdminService.enableUser(userId);

    action.then(() => {
      loadUsers();
    }).catch(err => {
      alert('Failed to update user status');
    });
  };

  const handleDeleteUser = (userId, username) => {
    setDeleteModal({ show: true, userId, username });
  };

  const confirmDelete = () => {
    AdminService.deleteUser(deleteModal.userId)
      .then(() => {
        setNotification({ show: true, message: 'User and all submissions deleted successfully', type: 'success' });
        setTimeout(() => setNotification({ show: false, message: '', type: '' }), 3000);
        loadUsers();
        setDeleteModal({ show: false, userId: null, username: '' });
      })
      .catch(err => {
        setNotification({ show: true, message: err.response?.data?.message || 'Failed to delete user', type: 'error' });
        setTimeout(() => setNotification({ show: false, message: '', type: '' }), 3000);
        setDeleteModal({ show: false, userId: null, username: '' });
      });
  };

  const cancelDelete = () => {
    setDeleteModal({ show: false, userId: null, username: '' });
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
        <h1 className="text-xl sm:text-2xl lg:text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-1">
          User Management
        </h1>
        <p className="text-xs sm:text-sm text-gray-500">Manage user accounts and permissions</p>
      </div>

      {/* Users Table */}
      <div ref={tableRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto max-h-[600px] overflow-y-auto custom-scrollbar">
          <table className="w-full">
            <thead className="bg-white/10 border-b border-white/20">
              <tr>
                <th className="px-3 sm:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Username</th>
                <th className="px-3 sm:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Roll Number</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Branch</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/10">
              {users.map((user, index) => (
                <motion.tr
                  key={user.id}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: index * 0.05, duration: 0.3 }}
                  whileHover={{ x: 3, backgroundColor: '#1f1f1f' }}
                  className="transition-colors"
                >
                  <td className="px-3 sm:px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-200">{user.username}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">{user.email}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400 font-mono">{user.rollNumber || 'N/A'}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">{user.branch || 'N/A'}</td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2.5 py-1 rounded text-xs font-medium ${user.enabled ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                      }`}>
                      {user.enabled ? 'Enabled' : 'Disabled'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleToggleUser(user.id, user.enabled)}
                        className={`px-4 py-2 rounded font-medium transition-all ${user.enabled
                          ? 'bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 hover:border-red-500/50 text-red-400'
                          : 'bg-green-500/10 hover:bg-green-500/20 border border-green-500/30 hover:border-green-500/50 text-green-400'
                          }`}
                      >
                        {user.enabled ? 'Disable' : 'Enable'}
                      </button>
                      <button
                        onClick={() => handleDeleteUser(user.id, user.username)}
                        className="px-4 py-2 rounded font-medium transition-all bg-gray-500/10 hover:bg-gray-500/20 border border-gray-500/30 hover:border-gray-500/50 text-gray-400 hover:text-gray-300"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {deleteModal.show && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.9 }}
            className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl"
          >
            {/* Warning Icon */}
            <div className="flex items-center justify-center w-16 h-16 bg-red-500/10 rounded-full mx-auto mb-4">
              <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>

            {/* Title */}
            <h3 className="text-xl sm:text-2xl font-bold text-red-400 text-center mb-3">
              Delete User
            </h3>

            {/* Message */}
            <p className="text-gray-300 text-center mb-2">
              Are you sure you want to delete user <span className="font-bold text-white">"{deleteModal.username}"</span>?
            </p>
            <p className="text-sm text-gray-400 text-center mb-6">
              This will also delete all their submissions. This action cannot be undone!
            </p>

            {/* Buttons */}
            <div className="flex gap-3">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={cancelDelete}
                className="flex-1 px-4 py-3 bg-gray-700 hover:bg-gray-600 text-white font-medium rounded-xl transition-all"
              >
                Cancel
              </motion.button>
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={confirmDelete}
                className="flex-1 px-4 py-3 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white font-medium rounded-xl shadow-lg shadow-red-500/30 transition-all"
              >
                Delete
              </motion.button>
            </div>
          </motion.div>
        </div>
      )}

      {/* Notification Toast */}
      {
        notification.show && (
          <motion.div
            initial={{ opacity: 0, y: -50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -50 }}
            className={`fixed top-4 right-4 z-50 px-6 py-4 rounded-xl shadow-2xl backdrop-blur-xl border ${notification.type === 'success'
              ? 'bg-green-500/20 border-green-500/50 text-green-400'
              : 'bg-red-500/20 border-red-500/50 text-red-400'
              }`}
          >
            <div className="flex items-center gap-3">
              {notification.type === 'success' ? (
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              ) : (
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              )}
              <p className="font-medium">{notification.message}</p>
            </div>
          </motion.div>
        )
      }
    </div >
  );
};

export default AdminUserManagement;
