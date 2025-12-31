import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import AdminRoute from './components/AdminRoute';
import UserRoute from './components/UserRoute';
import GuestRoute from './components/GuestRoute';
import Login from './pages/Login';
import Register from './pages/Register';
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
import Home from './pages/Home';
import NotFound from './pages/NotFound';
import React from 'react';

function App() {
  return (
    <div className="min-h-screen bg-linear-to-br from-black via-green-950/30 to-black text-gray-100 font-sans selection:bg-code-green selection:text-black flex flex-col">
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<GuestRoute><Login /></GuestRoute>} />
        <Route path="/register" element={<GuestRoute><Register /></GuestRoute>} />

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

        {/* 404 Not Found - Catch all unknown routes */}
        <Route path="*" element={<NotFound />} />
      </Routes>
      <Footer />
    </div>
  );
}

export default App;
