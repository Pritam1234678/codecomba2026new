import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { gsap } from 'gsap';
import AdminService from '../services/admin.service';
import AuthService from '../services/auth.service';
import api from '../services/api';

const AdminDashboard = () => {
  const [userStats, setUserStats] = useState({ total: 0, enabled: 0, disabled: 0 });
  const [contestStats, setContestStats] = useState({ total: 0, active: 0, inactive: 0 });
  const [adminProfile, setAdminProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  const headerRef = useRef(null);
  const profileRef = useRef(null);
  const actionsRef = useRef(null);

  useEffect(() => {
    Promise.all([
      AdminService.getUserStats(),
      AdminService.getContestStats(),
      api.get('/user/profile')
    ]).then(([userRes, contestRes, profileRes]) => {
      setUserStats(userRes.data);
      setContestStats(contestRes.data);
      setAdminProfile(profileRes.data);
      setLoading(false);
    }).catch(err => {
      console.error(err);
      const currentUser = AuthService.getCurrentUser();
      setAdminProfile(currentUser);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    if (!loading && headerRef.current) {
      const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });

      tl.from(headerRef.current, {
        opacity: 0,
        y: 30,
        duration: 0.8
      })
        .from(profileRef.current, {
          opacity: 0,
          y: 20,
          duration: 0.6
        }, '-=0.4')
        .from(actionsRef.current, {
          opacity: 0,
          y: 20,
          duration: 0.6
        }, '-=0.2');
    }
  }, [loading]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-400 text-lg">Loading...</div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Header Section */}
      <div ref={headerRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-8 shadow-2xl">
        <div className="flex items-center gap-6">
          <div className="w-16 h-16 bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-sm border border-white/20 rounded-xl flex items-center justify-center shadow-lg overflow-hidden">
            {adminProfile?.photoUrl ? (
              <img
                src={adminProfile.photoUrl}
                alt={adminProfile?.fullName || adminProfile?.username}
                className="w-full h-full object-cover"
              />
            ) : (
              <span className="text-2xl font-bold text-gray-100">
                {adminProfile?.username?.charAt(0).toUpperCase()}
              </span>
            )}
          </div>
          <div className="flex-1">
            <h1 className="text-3xl font-semibold mb-1 bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent">
              Admin Dashboard
            </h1>
            <p className="text-sm text-gray-500">
              Logged in as <span className="text-gray-300 font-medium">{adminProfile?.username}</span>
            </p>
          </div>
          <div className="text-right flex items-center gap-4">
            <Link
              to="/profile/edit"
              className="px-8 py-4 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-semibold rounded-xl shadow-lg shadow-green-500/30 transition-all transform hover:scale-105 flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              Edit Profile
            </Link>
            <div>
              <div className="text-xs font-medium bg-linear-to-r from-white/10 to-red-900 bg-clip-text text-transparent uppercase tracking-wide mb-1">Role</div>
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 bg-gray-400 rounded-full"></span>
                <span className="text-sm bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent font-medium">Administrator</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Profile Card */}
      <div ref={profileRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 shadow-2xl">
        <h2 className="text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-4">Profile Information</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Email</div>
            <div className="text-sm text-gray-300 font-mono">{adminProfile?.email || '—'}</div>
          </div>
          <div>
            <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Full Name</div>
            <div className="text-sm text-gray-300">{adminProfile?.fullName || '—'}</div>
          </div>
          <div>
            <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Roll Number</div>
            <div className="text-sm text-gray-300 font-mono">{adminProfile?.rollNumber || '—'}</div>
          </div>
          <div>
            <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Branch</div>
            <div className="text-sm text-gray-300">{adminProfile?.branch || '—'}</div>
          </div>
        </div>
      </div>

      {/* Statistics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 shadow-xl hover:border-white/30 transition-all">
          <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Total Users</div>
          <div className="text-3xl font-bold text-gray-100">{userStats.total}</div>
        </div>
        <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 shadow-xl hover:border-white/30 transition-all">
          <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Enabled Users</div>
          <div className="text-3xl font-bold text-gray-100">{userStats.enabled}</div>
        </div>
        <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 shadow-xl hover:border-white/30 transition-all">
          <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Total Contests</div>
          <div className="text-3xl font-bold text-gray-100">{contestStats.total}</div>
        </div>
        <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 shadow-xl hover:border-white/30 transition-all">
          <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Active Contests</div>
          <div className="text-3xl font-bold text-gray-100">{contestStats.active}</div>
        </div>
      </div>

      {/* Quick Actions */}
      <div ref={actionsRef} className="bg-linear-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 shadow-2xl">
        <h2 className="text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 ">
          <Link
            to="/admin/users"
            className="bg-white/5 backdrop-blur-sm border border-white/20 hover:border-white/30 rounded-xl p-4 transition-all hover:shadow-lg hover:scale-105 hover:shadow-green-500/30"
          >
            <div className="text-gray-100 font-medium mb-1">Manage Users</div>
            <div className="text-xs text-gray-500">View and manage all users</div>
          </Link>
          <Link
            to="/admin/contests"
            className="bg-white/5 backdrop-blur-sm border border-white/20 hover:border-white/30 rounded-xl p-4 transition-all hover:shadow-lg hover:scale-105 hover:shadow-green-500/30"
          >
            <div className="text-gray-100 font-medium mb-1">Manage Contests</div>
            <div className="text-xs text-gray-500">Create and manage contests</div>
          </Link>
          <Link
            to="/admin/leaderboard"
            className="bg-white/5 backdrop-blur-sm border border-white/20 hover:border-white/30 rounded-xl p-4 transition-all hover:shadow-lg hover:scale-105 hover:shadow-green-500/30"
          >
            <div className="text-gray-100 font-medium mb-1">Leaderboard</div>
            <div className="text-xs text-gray-500">View rankings</div>
          </Link>
          <Link
            to="/admin/platform-details"
            className="bg-white/5 backdrop-blur-sm border border-white/20 hover:border-white/30 rounded-xl p-4 transition-all hover:shadow-lg hover:scale-105 hover:shadow-green-500/30 "
          >
            <div className="text-gray-100 font-medium mb-1">Platform Details</div>
            <div className="text-xs text-gray-500">View platform information</div>
          </Link>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
