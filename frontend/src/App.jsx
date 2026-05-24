import { lazy, Suspense, useEffect } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import AppSidebar from './components/AppSidebar';
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
import AddProblem from './pages/AddProblem';
import ManageTestCases from './pages/ManageTestCases';
import Leaderboard from './pages/Leaderboard';
import ContestLeaderboard from './pages/ContestLeaderboard';
import UserDashboard from './pages/UserDashboard';
import EditProfile from './pages/EditProfile';
import PlatformDetails from './pages/PlatformDetails';
import Support from './pages/Support';
import Home from './pages/Home';
import CoderCompiler from './pages/CoderCompiler';
import Practice from './pages/Practice';
import PracticeSolve from './pages/PracticeSolve';
import api from './services/api';
import AuthService from './services/auth.service';
const NotFound = lazy(() => import('./pages/NotFound'));
const Duel = lazy(() => import('./pages/Duel'));
const DuelArena = lazy(() => import('./pages/DuelArena'));

// Public routes — no sidebar, no auth required
// /compiler is special: shown with sidebar if logged in, with navbar if not
const PUBLIC_PATHS = ['/', '/login', '/register', '/forgot-password', '/reset-password', '/forgot-username'];
const PUBLIC_LOGGED_OUT_PATHS = ['/compiler']; // accessible publicly but shows sidebar when logged in

function App() {
  const location = useLocation();

  useEffect(() => {
    const checkAccountStatus = async () => {
      if (PUBLIC_PATHS.includes(location.pathname)) return;
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user || !user.token) return;
      const lastCheck = sessionStorage.getItem('lastAccountCheck');
      const now = Date.now();
      if (lastCheck && now - parseInt(lastCheck) < 5 * 60 * 1000) return;
      try {
        await api.get('/user/profile');
        sessionStorage.setItem('lastAccountCheck', String(now));
      } catch {}
    };
    checkAccountStatus();
  }, [location.pathname]);

  const currentUser   = AuthService.getCurrentUser();
  const isLoggedIn    = !!currentUser;
  const isPublicRoute = PUBLIC_PATHS.includes(location.pathname);
  const isPublicLoggedOutRoute = PUBLIC_LOGGED_OUT_PATHS.includes(location.pathname);

  // Show sidebar for all logged-in routes (replaces navbar)
  // Show navbar only for public routes (home, login, register, etc.)
  const showSidebar = isLoggedIn && !isPublicRoute;
  const showNavbar  = !isLoggedIn || (isPublicRoute && !isPublicLoggedOutRoute);
  const showFooter  = !showSidebar; // no footer when sidebar is shown

  const routes = (
    <Routes>
      {/* Public */}
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<GuestRoute><Login /></GuestRoute>} />
      <Route path="/register" element={<GuestRoute><Register /></GuestRoute>} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/forgot-username" element={<ForgotUsername />} />
      <Route path="/compiler" element={<CoderCompiler />} />

      {/* Admin Routes */}
      <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />
      <Route path="/admin/dashboard" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
      <Route path="/admin/users" element={<div className="p-8 flex-1"><AdminRoute><AdminUserManagement /></AdminRoute></div>} />
      <Route path="/admin/contests" element={<div className="p-8 flex-1"><AdminRoute><AdminContestManagement /></AdminRoute></div>} />
      <Route path="/admin/contests/create" element={<div className="p-8 flex-1"><AdminRoute><CreateContest /></AdminRoute></div>} />
      <Route path="/admin/contests/:id/edit" element={<div className="p-8 flex-1"><AdminRoute><EditContest /></AdminRoute></div>} />
      <Route path="/admin/contests/:id/problems" element={<div className="p-8 flex-1"><AdminRoute><ManageContestProblems /></AdminRoute></div>} />
      <Route path="/admin/contests/:contestId/problems/add" element={<AdminRoute><AddProblem /></AdminRoute>} />
      <Route path="/admin/problems/:id/edit" element={<div className="p-8 flex-1"><AdminRoute><EditProblem /></AdminRoute></div>} />
      <Route path="/admin/problems/:id/testcases" element={<div className="p-8 flex-1"><AdminRoute><ManageTestCases /></AdminRoute></div>} />
      <Route path="/admin/leaderboard" element={<div className="p-8 flex-1"><AdminRoute><Leaderboard /></AdminRoute></div>} />
      <Route path="/admin/leaderboard/:contestId" element={<div className="p-8 flex-1"><AdminRoute><ContestLeaderboard /></AdminRoute></div>} />
      <Route path="/admin/platform-details" element={<div className="p-8 flex-1"><AdminRoute><PlatformDetails /></AdminRoute></div>} />

      {/* User Routes */}
      <Route path="/dashboard" element={<div className="p-8 flex-1"><UserRoute><UserDashboard /></UserRoute></div>} />
      <Route path="/profile/edit" element={<UserRoute><EditProfile /></UserRoute>} />
      <Route path="/contests" element={<div className="p-8 flex-1"><UserRoute><ContestList /></UserRoute></div>} />
      <Route path="/contests/:id" element={<div className="p-8 flex-1"><UserRoute><ContestDetail /></UserRoute></div>} />
      <Route path="/problems/:id" element={<div className="flex-1 px-14 py-8"><UserRoute><ProblemSolve /></UserRoute></div>} />
      <Route path="/practice" element={<UserRoute><Practice /></UserRoute>} />
      <Route path="/practice/:id" element={<UserRoute><PracticeSolve /></UserRoute>} />
      <Route path="/duel" element={
        <Suspense fallback={<div className="flex items-center justify-center min-h-screen text-gray-400">Loading...</div>}>
          <UserRoute><Duel /></UserRoute>
        </Suspense>
      } />
      <Route path="/duel/:matchId" element={
        <Suspense fallback={<div className="flex items-center justify-center min-h-screen text-gray-400">Loading...</div>}>
          <UserRoute><DuelArena /></UserRoute>
        </Suspense>
      } />
      <Route path="/platform-details" element={<div className="p-8 flex-1"><UserRoute><PlatformDetails /></UserRoute></div>} />
      <Route path="/support" element={<div className="flex-1"><Support /></div>} />

      {/* 404 */}
      <Route path="*" element={
        <Suspense fallback={<div className="flex items-center justify-center min-h-screen text-gray-400">Loading...</div>}>
          <NotFound />
        </Suspense>
      } />
    </Routes>
  );

  // ── Sidebar layout (logged-in users) ──────────────────────────────────────
  if (showSidebar) {
    return (
      <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: '#131313' }}>
        <AppSidebar />
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
          {routes}
          <Footer />
        </div>
      </div>
    );
  }

  // ── Public layout (navbar + footer) ───────────────────────────────────────
  // Compiler is full-height — hide footer there
  const isCompilerPage = location.pathname === '/compiler';
  return (
    <div className="text-on-surface font-sans" style={{ backgroundColor: 'var(--color-background)', color: 'var(--color-on-surface)' }}>
      {showNavbar && <Navbar />}
      {routes}
      {showFooter && !isCompilerPage && <Footer />}
    </div>
  );
}

export default App;
