import React, { lazy, Suspense } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import AdminRoute from './components/AdminRoute';
import UserRoute from './components/UserRoute';
import GuestRoute from './components/GuestRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import ForgotUsername from './pages/ForgotUsername';
import ContestList from './pages/ContestList';
import ContestDetail from './pages/ContestDetail';
import ProblemSolve from './pages/ProblemSolve';
import AdminDashboard from './pages/AdminDashboard';
import AdminUserManagement from './pages/AdminUserManagement';
import AdminContestManagement from './pages/AdminContestManagement';
import CreateContest from './pages/CreateContest';
import EditContest from './pages/EditContest';
import ManageContestProblems from './pages/ManageContestProblems';
import EditProblem from './pages/EditProblem';
import ManageTestCases from './pages/ManageTestCases';
import Leaderboard from './pages/Leaderboard';
import ContestLeaderboard from './pages/ContestLeaderboard';
import UserDashboard from './pages/UserDashboard';
import EditProfile from './pages/EditProfile';
import PlatformDetails from './pages/PlatformDetails';
import Support from './pages/Support';
import Home from './pages/Home';
// Lazy-load NotFound — it pulls in Three.js (1MB) which is never needed on normal pages
const NotFound = lazy(() => import('./pages/NotFound'));
import api from './services/api';

function App() {
  const location = useLocation();

  // Check account status only on protected pages, not on every route change.
  // The API interceptor already handles 401/403 responses globally.
  // This effect only runs when navigating to a protected page after being idle.
  useEffect(() => {
    const checkAccountStatus = async () => {
      // Skip check for public pages
      if (location.pathname === '/support' ||
        location.pathname === '/login' ||
        location.pathname === '/register' ||
        location.pathname === '/') {
        return;
      }

      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.token) return;

      // Only check once per 5 minutes — not on every single route change
      const lastCheck = sessionStorage.getItem('lastAccountCheck');
      const now = Date.now();
      if (lastCheck && now - parseInt(lastCheck) < 5 * 60 * 1000) {
        return; // Skip — checked recently
      }

      try {
        await api.get('/user/profile');
        sessionStorage.setItem('lastAccountCheck', String(now));
      } catch (error) {
        // Interceptor handles 401/403
      }
    };

    checkAccountStatus();
  }, [location.pathname]);

  return (
    <div className="min-h-screen bg-linear-to-br from-black via-green-950/30 to-black text-gray-100 font-sans selection:bg-code-green selection:text-black flex flex-col">
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<GuestRoute><Login /></GuestRoute>} />
        <Route path="/register" element={<GuestRoute><Register /></GuestRoute>} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/forgot-username" element={<ForgotUsername />} />

        {/* Admin Routes - Protected */}
        <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />
        <Route path="/admin/dashboard" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><AdminDashboard /></AdminRoute></div>} />
        <Route path="/admin/users" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><AdminUserManagement /></AdminRoute></div>} />
        <Route path="/admin/contests" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><AdminContestManagement /></AdminRoute></div>} />
        <Route path="/admin/contests/create" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><CreateContest /></AdminRoute></div>} />
        <Route path="/admin/contests/:id/edit" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><EditContest /></AdminRoute></div>} />
        <Route path="/admin/contests/:id/problems" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><ManageContestProblems /></AdminRoute></div>} />
        <Route path="/admin/problems/:id/edit" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><EditProblem /></AdminRoute></div>} />
        <Route path="/admin/problems/:id/testcases" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><ManageTestCases /></AdminRoute></div>} />
        <Route path="/admin/leaderboard" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><Leaderboard /></AdminRoute></div>} />
        <Route path="/admin/leaderboard/:contestId" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><ContestLeaderboard /></AdminRoute></div>} />
        <Route path="/admin/platform-details" element={<div className="container mx-auto px-4 py-8 flex-1"><AdminRoute><PlatformDetails /></AdminRoute></div>} />



        {/* User Routes */}
        <Route path="/dashboard" element={<div className="container mx-auto px-4 py-8 flex-1"><UserRoute><UserDashboard /></UserRoute></div>} />
        <Route path="/profile/edit" element={<div className="container mx-auto px-4 py-8 flex-1"><UserRoute><EditProfile /></UserRoute></div>} />
        <Route path="/contests" element={<div className="container mx-auto px-4 py-8 flex-1"><UserRoute><ContestList /></UserRoute></div>} />
        <Route path="/contests/:id" element={<div className="container mx-auto px-4 py-8 flex-1"><UserRoute><ContestDetail /></UserRoute></div>} />
        <Route path="/problems/:id" element={<div className="flex-1  px-14 py-8    "><UserRoute><ProblemSolve /></UserRoute></div>} />
        <Route path="/platform-details" element={<div className="container mx-auto px-4 py-8 flex-1"><UserRoute><PlatformDetails /></UserRoute></div>} />
        <Route path="/support" element={<div className="flex-1"><Support /></div>} />

        {/* 404 Not Found - Catch all unknown routes */}
        <Route path="*" element={
          <Suspense fallback={<div className="flex items-center justify-center min-h-screen text-gray-400">Loading...</div>}>
            <NotFound />
          </Suspense>
        } />
      </Routes>
      <Footer />
    </div>
  );
}

export default App;
