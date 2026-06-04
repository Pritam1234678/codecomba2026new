import { lazy, Suspense, useEffect } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import AppSidebar from './components/AppSidebar';
import AdminRoute from './components/AdminRoute';
import UserRoute from './components/UserRoute';
import GuestRoute from './components/GuestRoute';
import api from './services/api';
import AuthService from './services/auth.service';

// ── Lazy-loaded routes (code-split per page) ─────────────────────────────────
// Even Home/Login/Register are split out — keeps the App shell tiny so the
// initial bundle is just routing + auth wrappers + sidebar.
const Home           = lazy(() => import('./pages/Home'));
const Login          = lazy(() => import('./pages/Login'));
const Register       = lazy(() => import('./pages/Register'));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword'));
const ResetPassword  = lazy(() => import('./pages/ResetPassword'));
const ForgotUsername = lazy(() => import('./pages/ForgotUsername'));
const Support        = lazy(() => import('./pages/Support'));
const CoderCompiler  = lazy(() => import('./pages/CoderCompiler'));

// User pages
const ContestList    = lazy(() => import('./pages/ContestList'));
const ContestDetail  = lazy(() => import('./pages/ContestDetail'));
const ProblemSolve   = lazy(() => import('./pages/ProblemSolve'));
const UserDashboard  = lazy(() => import('./pages/UserDashboard'));
const EditProfile    = lazy(() => import('./pages/EditProfile'));
const Practice       = lazy(() => import('./pages/Practice'));
const PracticeSolve  = lazy(() => import('./pages/PracticeSolve'));
const PlatformDetails = lazy(() => import('./pages/PlatformDetails'));
const Leaderboard    = lazy(() => import('./pages/Leaderboard'));
const ContestLeaderboard = lazy(() => import('./pages/ContestLeaderboard'));
const UserSearch     = lazy(() => import('./pages/UserSearch'));
const PlayerProfile  = lazy(() => import('./pages/PlayerProfile'));

// Admin pages — heaviest, load only when admin navigates there
const AdminDashboard       = lazy(() => import('./pages/AdminDashboard'));
const AdminUserManagement  = lazy(() => import('./pages/AdminUserManagement'));
const AdminContestManagement = lazy(() => import('./pages/AdminContestManagement'));
const AdminProblemManagement = lazy(() => import('./pages/AdminProblemManagement'));
const AdminDuelMonitor     = lazy(() => import('./pages/AdminDuelMonitor'));
const CreateContest        = lazy(() => import('./pages/CreateContest'));
const EditContest          = lazy(() => import('./pages/EditContest'));
const ManageContestProblems = lazy(() => import('./pages/ManageContestProblems'));
const EditProblem          = lazy(() => import('./pages/EditProblem'));
const AddProblem           = lazy(() => import('./pages/AddProblem'));
const ManageTestCases      = lazy(() => import('./pages/ManageTestCases'));

const NotFound  = lazy(() => import('./pages/NotFound'));
const Duel      = lazy(() => import('./pages/Duel'));
const DuelArena = lazy(() => import('./pages/DuelArena'));

// Proctored contest mode — entry shell, arena placeholder, and terminal
// screen (task 3.4). Real arena, admin dashboard, and session
// drill-down land in tasks 6, 8, 10, 11. Lazy-loaded so the proctoring
// bundle (MediaPipe wasm, IndexedDB helpers) is only fetched when a
// candidate or admin actually navigates into the proctoring surface.
// Terminator screen is the landing for the WebSocket SESSION_TERMINATED
// flow (task 12.3) and the LOCKED_OUT (423) redirect.
const ProctoredContestEntry      = lazy(() => import('./proctoring/pages/ProctoredContestEntry'));
const ProctoredContestArena      = lazy(() => import('./proctoring/pages/ProctoredContestArena'));
const ProctoredContestTerminated = lazy(() => import('./proctoring/pages/ProctoredContestTerminated'));
const AdminProctoringDashboard   = lazy(() => import('./proctoring/pages/AdminProctoringDashboard'));
const AdminProctoringSession     = lazy(() => import('./proctoring/pages/AdminProctoringSession'));

// Loading fallback shown briefly while a chunk loads
const PageFallback = () => (
  <div className="flex items-center justify-center min-h-[60vh]" style={{ color: '#9d8e83', fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>
    Loading...
  </div>
);
const lazyWrap = (el) => <Suspense fallback={<PageFallback />}>{el}</Suspense>;

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
      <Route path="/" element={lazyWrap(<Home />)} />
      <Route path="/login" element={lazyWrap(<GuestRoute><Login /></GuestRoute>)} />
      <Route path="/register" element={lazyWrap(<GuestRoute><Register /></GuestRoute>)} />
      <Route path="/forgot-password" element={lazyWrap(<ForgotPassword />)} />
      <Route path="/reset-password" element={lazyWrap(<ResetPassword />)} />
      <Route path="/forgot-username" element={lazyWrap(<ForgotUsername />)} />
      <Route path="/compiler" element={lazyWrap(<CoderCompiler />)} />

      {/* Admin Routes */}
      <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />
      <Route path="/admin/dashboard" element={lazyWrap(<AdminRoute><AdminDashboard /></AdminRoute>)} />
      <Route path="/admin/users" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><AdminUserManagement /></AdminRoute>)}</div>} />
      <Route path="/admin/contests" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><AdminContestManagement /></AdminRoute>)}</div>} />
      <Route path="/admin/contests/create" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><CreateContest /></AdminRoute>)}</div>} />
      <Route path="/admin/contests/:id/edit" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><EditContest /></AdminRoute>)}</div>} />
      <Route path="/admin/contests/:id/problems" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><ManageContestProblems /></AdminRoute>)}</div>} />
      <Route path="/admin/contests/:contestId/problems/add" element={lazyWrap(<AdminRoute><AddProblem /></AdminRoute>)} />
      <Route path="/admin/problems" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><AdminProblemManagement /></AdminRoute>)}</div>} />
      <Route path="/admin/problems/new" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><AddProblem /></AdminRoute>)}</div>} />
      <Route path="/admin/problems/:id/edit" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><EditProblem /></AdminRoute>)}</div>} />
      <Route path="/admin/problems/:id/testcases" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><ManageTestCases /></AdminRoute>)}</div>} />
      <Route path="/admin/duels" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><AdminDuelMonitor /></AdminRoute>)}</div>} />
      <Route path="/admin/proctoring" element={lazyWrap(<AdminRoute><AdminProctoringDashboard /></AdminRoute>)} />
      <Route path="/admin/leaderboard" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><Leaderboard /></AdminRoute>)}</div>} />
      <Route path="/admin/leaderboard/:contestId" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><ContestLeaderboard /></AdminRoute>)}</div>} />
      <Route path="/admin/platform-details" element={<div className="p-8 flex-1">{lazyWrap(<AdminRoute><PlatformDetails /></AdminRoute>)}</div>} />

      {/* User Routes */}
      <Route path="/dashboard" element={<div className="p-8 flex-1">{lazyWrap(<UserRoute><UserDashboard /></UserRoute>)}</div>} />
      <Route path="/profile/edit" element={lazyWrap(<UserRoute><EditProfile /></UserRoute>)} />
      <Route path="/contests" element={<div className="p-8 flex-1">{lazyWrap(<UserRoute><ContestList /></UserRoute>)}</div>} />
      <Route path="/contests/:id" element={<div className="p-8 flex-1">{lazyWrap(<UserRoute><ContestDetail /></UserRoute>)}</div>} />
      <Route path="/problems/:id" element={<div className="flex-1 px-14 py-8">{lazyWrap(<UserRoute><ProblemSolve /></UserRoute>)}</div>} />
      <Route path="/practice" element={lazyWrap(<UserRoute><Practice /></UserRoute>)} />
      <Route path="/practice/:id" element={lazyWrap(<UserRoute><PracticeSolve /></UserRoute>)} />
      <Route path="/duel" element={lazyWrap(<UserRoute><Duel /></UserRoute>)} />
      <Route path="/duel/:matchId" element={lazyWrap(<UserRoute><DuelArena /></UserRoute>)} />

      {/* Proctored contest routes — task 3.4 entry shell + arena placeholder */}
      <Route path="/contests/:contestId/proctored/entry" element={lazyWrap(<UserRoute><ProctoredContestEntry /></UserRoute>)} />
      <Route path="/contests/:contestId/proctored/arena" element={lazyWrap(<UserRoute><ProctoredContestArena /></UserRoute>)} />
      <Route path="/platform-details" element={<div className="p-8 flex-1">{lazyWrap(<UserRoute><PlatformDetails /></UserRoute>)}</div>} />
      <Route path="/support" element={<div className="flex-1">{lazyWrap(<Support />)}</div>} />
      <Route path="/players" element={<div className="p-8 flex-1">{lazyWrap(<UserRoute><UserSearch /></UserRoute>)}</div>} />
      <Route path="/players/:username" element={<div className="p-8 flex-1">{lazyWrap(<UserRoute><PlayerProfile /></UserRoute>)}</div>} />

      {/* Proctored contest — terminal screen (Req 10.4, 13.9, 24.6) */}
      <Route
        path="/contests/:contestId/proctored/terminated"
        element={lazyWrap(<UserRoute><ProctoredContestTerminated /></UserRoute>)}
      />

      {/* Admin proctoring drill-down (task 10.5, Req 15.3, 15.4, 15.6, 15.7) */}
      <Route
        path="/admin/proctoring/sessions/:sessionId"
        element={lazyWrap(<AdminRoute><AdminProctoringSession /></AdminRoute>)}
      />

      {/* 404 */}
      <Route path="*" element={lazyWrap(<NotFound />)} />
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
