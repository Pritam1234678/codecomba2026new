import React, { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { gsap } from 'gsap';
import AdminService from '../services/admin.service';

const AdminUserManagement = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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
        setUsers(response.data);
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
        <div className="overflow-x-auto">
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
                    <button
                      onClick={() => handleToggleUser(user.id, user.enabled)}
                      className={`px-4 py-2 rounded font-medium transition-all ${user.enabled
                        ? 'bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 hover:border-red-500/50 text-red-400'
                        : 'bg-green-500/10 hover:bg-green-500/20 border border-green-500/30 hover:border-green-500/50 text-green-400'
                        }`}
                    >
                      {user.enabled ? 'Disable' : 'Enable'}
                    </button>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default AdminUserManagement;
